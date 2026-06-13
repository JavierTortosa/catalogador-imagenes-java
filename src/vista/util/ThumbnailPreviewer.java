package vista.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.ZoomManager;
import controlador.managers.interfaces.IViewManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigurationManager;
import servicios.zoom.ZoomModeEnum;
import utils.ImageUtils;
import vista.panels.ImageDisplayPanel;
import vista.theme.ThemeManager;

public class ThumbnailPreviewer {

	private static final Logger logger = LoggerFactory.getLogger(ThumbnailPreviewer.class);
	
    private final JList<String> targetList;
    private final VisorModel mainModel;
    private final ThemeManager themeManager;
    private final IViewManager viewManager;
    private final ComponentRegistry registry;

    // Campos del diálogo: se anulan en windowClosed para recrear limpio en el siguiente doble-click
    private JDialog previewDialog;
    private ImageDisplayPanel previewPanel;
    private VisorModel previewModel;
    private ZoomManager previewZoomManager;
    
    // Componente que tenía el foco antes de abrir el diálogo, para restaurarlo al cerrar
    private Component focusOwnerBeforeDialog;

    private static final int PREVIEW_WIDTH = 500;
    private static final int PREVIEW_HEIGHT = 500;

    public ThumbnailPreviewer(JList<String> targetList, VisorModel mainModel, ThemeManager themeManager, IViewManager viewManager, ComponentRegistry registry) {
        this.targetList = targetList;
        this.mainModel = mainModel;
        this.themeManager = themeManager;
        this.viewManager = viewManager;
        this.registry = registry;
        
        if (this.targetList != null) {
            installListeners();
        }
    } // --- FIN del Constructor ---   

    private void installListeners() {
        targetList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int index = targetList.locationToIndex(e.getPoint());
                    if (index != -1) {
                        showPreviewForIndex(targetList, index);
                    }
                }
            }
        });
    } // --- FIN del metodo installListeners ---

    private void createPreviewDialog(JList<String> listContext) {
        Window owner = SwingUtilities.getWindowAncestor(listContext);
        
        previewDialog = new JDialog(owner, "Previsualización", JDialog.ModalityType.MODELESS);
        previewDialog.getContentPane().setLayout(new BorderLayout());

        previewModel = new VisorModel();
        previewPanel = new ImageDisplayPanel(this.themeManager, previewModel);
        previewPanel.setPreferredSize(new java.awt.Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));

        Color borderColor = themeManager.getTemaActual().colorBordeSeleccionActiva();
        previewPanel.setBorder(javax.swing.BorderFactory.createLineBorder(borderColor, 3));
        
        previewDialog.getContentPane().add(this.previewPanel, BorderLayout.CENTER);
        
        previewZoomManager = new ZoomManager();
        previewZoomManager.setModel(previewModel);
        previewZoomManager.setSpecificPanel(this.previewPanel);
        previewZoomManager.setViewManager(this.viewManager);
        previewZoomManager.setRegistry(this.registry);
        previewZoomManager.setConfiguration(ConfigurationManager.getInstance());
        JLabel internalLabel = this.previewPanel.getInternalLabel();
        internalLabel.addMouseWheelListener(e -> { if (previewModel.isZoomHabilitado()) { previewZoomManager.aplicarZoomConRueda(e); } });
        internalLabel.addMouseListener(new MouseAdapter() { @Override public void mousePressed(MouseEvent e) { if (previewModel.isZoomHabilitado()) { previewZoomManager.iniciarPaneo(e); } } });
        internalLabel.addMouseMotionListener(new MouseAdapter() { @Override public void mouseDragged(MouseEvent e) { if (previewModel.isZoomHabilitado()) { previewZoomManager.continuarPaneo(e); } } });
        
        // Cerrar con ESC
        JRootPane rootPane = previewDialog.getRootPane();
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "CLOSE_DIALOG");
        rootPane.getActionMap().put("CLOSE_DIALOG", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                previewDialog.dispose();
            }
        });
        
        // Al cerrar el diálogo: restaurar el foco y resetear todas las referencias
        // para que la próxima vez se cree un nuevo diálogo limpio.
        previewDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                restoreFocusAndReset(listContext);
            }
            
            @Override
            public void windowDeactivated(WindowEvent e) {
                // Cuando la ventana principal recupera el foco (el usuario clickó fuera),
                // cerramos el diálogo de previsualización automáticamente.
                if (previewDialog != null && previewDialog.isVisible()) {
                    Window newActive = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
                    if (newActive != null && newActive != previewDialog) {
                        previewDialog.dispose();
                    }
                }
            }
        });
    } // --- FIN del metodo createPreviewDialog ---
    
    /**
     * Restaura el foco al componente que lo tenía antes de abrir el diálogo
     * y limpia todas las referencias del diálogo para permitir recreación limpia.
     */
    private void restoreFocusAndReset(JList<String> fallbackList) {
        Component componenteAReactivar = focusOwnerBeforeDialog;
        focusOwnerBeforeDialog = null;
        
        // Resetear referencias ANTES de pedir foco para evitar estados inconsistentes
        previewDialog = null;
        previewPanel = null;
        previewModel = null;
        previewZoomManager = null;
        
        // Restaurar el foco en el siguiente ciclo del EDT, una vez que el diálogo
        // esté completamente cerrado y el sistema de ventanas haya actualizado su estado.
        SwingUtilities.invokeLater(() -> {
            if (componenteAReactivar != null && componenteAReactivar.isShowing()) {
                componenteAReactivar.requestFocusInWindow();
                logger.debug("Foco restaurado a: {}", componenteAReactivar.getClass().getSimpleName());
            } else if (fallbackList != null && fallbackList.isShowing()) {
                fallbackList.requestFocusInWindow();
                logger.debug("Foco restaurado (fallback) a la lista.");
            }
        });
    } // --- FIN del metodo restoreFocusAndReset ---
    

    // Calcula el zoom inicial para ajustar la imagen al panel.
    private double calculateSmartFitZoom() {
        BufferedImage img = previewModel.getCurrentImage();
        if (img == null || previewPanel.getWidth() <= 0 || previewPanel.getHeight() <= 0) {
            return 1.0;
        }
        double widthRatio = (double) previewPanel.getWidth() / img.getWidth();
        double heightRatio = (double) previewPanel.getHeight() / img.getHeight();
        return Math.min(widthRatio, heightRatio);
    } // ---FIN de metodo ---

    public void showPreviewForIndexPublic(JList<String> list, int index) {
        showPreviewForIndex(list, index);
    }
    
    private void showPreviewForIndex(JList<String> list, int index) {
        if (index == -1) return;

        // Si ya hay un diálogo abierto, cerrarlo (toggle)
        if (previewDialog != null && previewDialog.isVisible()) {
            previewDialog.dispose();
            return;
        }
        
        // Capturar el foco AHORA (en el EDT, sincronamente), antes de que el SwingWorker lo mueva
        focusOwnerBeforeDialog = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        logger.debug("Foco capturado antes de abrir diálogo: {}",
            focusOwnerBeforeDialog != null ? focusOwnerBeforeDialog.getClass().getSimpleName() : "null");
        
        // Crear el diálogo (siempre nuevo, ya que las referencias se anulan al cerrar)
        createPreviewDialog(list);

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                String imageKey = list.getModel().getElementAt(index);
                java.nio.file.Path imagePath = mainModel.getRutaCompleta(imageKey);
                
                if (imagePath != null) {
                    final String title = "Previsualización: " + imagePath.getFileName();
                    SwingUtilities.invokeLater(() -> {
                        if (previewDialog != null) previewDialog.setTitle(title);
                    });
                }
                
                if (imagePath != null && java.nio.file.Files.exists(imagePath)) {
                	// 1. Cargamos la imagen original del disco.
                    BufferedImage imagenOriginal = ImageIO.read(imagePath.toFile());
                    // 2. Aplicamos la corrección de orientación EXIF.
                    BufferedImage imagenCorregida = ImageUtils.correctImageOrientation(imagenOriginal, imagePath);
                    // 3. Devolvemos la imagen YA CORREGIDA.
                    return imagenCorregida;
                }
                return null;
            }

            @Override
            protected void done() {
                // Si el diálogo fue cerrado/cancelado mientras cargaba, no hacer nada
                if (previewDialog == null) return;
                
                try {
                    BufferedImage image = get();
                    if (image != null) {
                        previewModel.setCurrentImage(image);
                        previewModel.setZoomHabilitado(true);
                        previewDialog.pack();
                        
                        double initialZoomFactor = calculateSmartFitZoom();
                        previewModel.setZoomFactor(initialZoomFactor);
                        previewModel.setCurrentZoomMode(ZoomModeEnum.SMART_FIT);
                        previewModel.resetPan();
                        
                        previewDialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(list));
                        previewDialog.setVisible(true);

                    } else {
                        previewPanel.mostrarError("No se pudo cargar la imagen", null);
                        previewDialog.pack();
                        previewDialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(list));
                        previewDialog.setVisible(true);
                    }
                } catch (Exception e) {
                    logger.error("Error al mostrar la previsualización", e);
                }
            }
        }.execute();
    } // --- FIN del metodo showPreviewForIndex ---
    
} // --- FIN DE LA CLASE ---