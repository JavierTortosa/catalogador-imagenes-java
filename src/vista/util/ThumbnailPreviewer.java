package vista.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.AWTEventListener;
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
import javax.swing.JFrame;
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

    private JDialog previewDialog;
    private ImageDisplayPanel previewPanel;
    private VisorModel previewModel;
    private ZoomManager previewZoomManager; // <-- Lo mantenemos para el paneo/zoom interactivo
    private AWTEventListener clickOutsideListener;

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

    private void createPreviewDialogIfNeeded(JList<String> listContext) {
        if (previewDialog == null) {
            Window owner = SwingUtilities.getWindowAncestor(listContext);
            
            // --- ESTRATEGIA FINAL: MODELESS + GLASS PANE ---
            // 1. El diálogo DEBE ser MODELESS.
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
            
            // CERRAR CON LA TECLA ESC (sin cambios)
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
            
            // 2. Listener para limpiar el Glass Pane cuando el diálogo se cierre
            previewDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    if (owner instanceof JFrame) {
                        Component glassPane = ((JFrame) owner).getGlassPane();
                        // Limpiamos sus listeners y lo hacemos invisible
                        glassPane.setVisible(false);
                        for (MouseAdapter listener : glassPane.getListeners(MouseAdapter.class)) {
                             glassPane.removeMouseListener(listener);
                        }
                        logger.debug("Glass Pane limpiado y ocultado.");
                    }
                }
            });
        }
    } // --- FIN del metodo createPreviewDialogIfNeeded ---
    

    // Este es el método que hace el trabajo del ajuste inicial,
    // sin llamar al ZoomManager. Es una lógica matemática simple y segura.
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

        createPreviewDialogIfNeeded(list);

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                String imageKey = list.getModel().getElementAt(index);
                java.nio.file.Path imagePath = mainModel.getRutaCompleta(imageKey);
                
                if (imagePath != null) {
                    SwingUtilities.invokeLater(() -> previewDialog.setTitle("Previsualización: " + imagePath.getFileName()));
                }
                
                if (imagePath != null && java.nio.file.Files.exists(imagePath)) {
                	
                	// 1. Cargamos la imagen original del disco.
                    BufferedImage imagenOriginal = ImageIO.read(imagePath.toFile());
                    
                    // 2. Aplicamos la corrección de orientación EXIF.
                    BufferedImage imagenCorregida = ImageUtils.correctImageOrientation(imagenOriginal, imagePath);
                    
                    // 3. Devolvemos la imagen YA CORREGIDA.
                    return imagenCorregida;
                	
//                    return ImageIO.read(imagePath.toFile());
                    
                }
                return null;
            }

            @Override
            protected void done() {
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
                        
                        // --- INICIO DE LA LÓGICA DEL GLASS PANE ---
                        // 3. Activar el Glass Pane ANTES de mostrar el diálogo
                        Window owner = previewDialog.getOwner();
                        if (owner instanceof JFrame) {
                            Component glassPane = ((JFrame) owner).getGlassPane();
                            
                            // Añadimos un listener que cerrará el diálogo al hacer clic
                            MouseAdapter glassPaneListener = new MouseAdapter() {
                                @Override
                                public void mousePressed(MouseEvent e) {
                                    previewDialog.dispose();
                                }
                            };
                            glassPane.addMouseListener(glassPaneListener);
                            
                            glassPane.setVisible(true);
                            logger.debug("Glass Pane activado con listener.");
                        }
                        // --- FIN DE LA LÓGICA DEL GLASS PANE ---
                        
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