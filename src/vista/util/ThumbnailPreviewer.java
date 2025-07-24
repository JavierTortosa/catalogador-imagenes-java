package vista.util;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import controlador.managers.ZoomManager;
import modelo.VisorModel;
import servicios.zoom.ZoomModeEnum;
import vista.panels.ImageDisplayPanel;
import vista.theme.ThemeManager;


/**
 * Muestra una ventana emergente con una previsualización de la miniatura
 * sobre la que se hace doble clic en una JList.
 */
public class ThumbnailPreviewer {

    private final JList<String> thumbnailList;
    private final VisorModel mainModel;

    private JWindow previewWindow;
    private ImageDisplayPanel previewPanel;
    private VisorModel previewModel;
    private ZoomManager previewZoomManager;
    private final ThemeManager themeManager;

    private Timer closeTimer;
    // ELIMINADO: private Timer hoverTimer;
    private int lastClickedIndex = -1; // Cambiado de lastHoveredIndex a lastClickedIndex

    // ELIMINADO: private static final int HOVER_DELAY_MS = 500;
    private static final int PREVIEW_WIDTH = 400;
    private static final int PREVIEW_HEIGHT = 400;

    public ThumbnailPreviewer(JList<String> thumbnailList, VisorModel mainModel, ThemeManager themeManager) {
        this.thumbnailList = thumbnailList;
        this.mainModel = mainModel;
        this.themeManager = themeManager;
        
        setupPreviewComponents();
        installListeners();
    } // --- FIN del Constructor ---   

    private void setupPreviewComponents() {
        this.previewModel = new VisorModel();
        this.previewPanel = new ImageDisplayPanel(this.themeManager, previewModel);
        
        // Añadir un borde compuesto
        // 1. Definir los colores y el grosor del borde usando el ThemeManager
        Color borderColor = themeManager.getTemaActual().colorBordeSeleccionActiva(); // Un color de acento del tema
        int borderThickness = 5; // Un borde de 2 píxeles
        int padding = 10; // 5 píxeles de espacio entre el borde y la imagen

        // 2. Crear un borde compuesto
        //    - El LineBorder es el contorno exterior.
        //    - El EmptyBorder es el padding interior.
        this.previewPanel.setBorder(
            javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(borderColor, borderThickness),
                javax.swing.BorderFactory.createEmptyBorder(padding, padding, padding, padding)
            )
        );
        
        
        Window owner = SwingUtilities.getWindowAncestor(thumbnailList);
        this.previewWindow = new JWindow(owner);
        this.previewWindow.setSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        this.previewWindow.getContentPane().add(this.previewPanel);
        this.previewWindow.setFocusableWindowState(false);
        this.previewZoomManager = new ZoomManager();
        this.previewZoomManager.setModel(previewModel);
        this.previewZoomManager.setSpecificPanel(this.previewPanel);
        JLabel internalLabel = this.previewPanel.getInternalLabel();
        internalLabel.addMouseWheelListener(e -> {
            if (previewModel.isZoomHabilitado()) {
                previewZoomManager.aplicarZoomConRueda(e);
            }
        });
        internalLabel.addMouseListener(new MouseAdapter() {
            @Override 
            public void mousePressed(MouseEvent e) { 
                if (previewModel.isZoomHabilitado()) {
                    previewZoomManager.iniciarPaneo(e);
                }
            }
        });
        internalLabel.addMouseMotionListener(new MouseAdapter() {
            @Override 
            public void mouseDragged(MouseEvent e) {
                if (previewModel.isZoomHabilitado()) {
                    previewZoomManager.continuarPaneo(e);
                }
            }
        });
    } // --- FIN del metodo setupPreviewComponents ---

    
    private void installListeners() {
        // 1. ELIMINAMOS EL LISTENER DE MOVIMIENTO (mouseMoved) y el de SALIDA (mouseExited)

        // 2. CREAMOS EL NUEVO TEMPORIZADOR DE CIERRE
        // La lógica interna de este timer es correcta y se mantiene.
        closeTimer = new Timer(250, e -> {
            if (previewWindow == null || !previewWindow.isVisible()) {
                ((Timer)e.getSource()).stop();
                return;
            }
            Point mousePos = java.awt.MouseInfo.getPointerInfo().getLocation();
            Point mouseInListCoords = new Point(mousePos);
            SwingUtilities.convertPointFromScreen(mouseInListCoords, thumbnailList);
            Point mouseInPopupCoords = new Point(mousePos);
            SwingUtilities.convertPointFromScreen(mouseInPopupCoords, previewWindow);
            boolean isMouseOverList = thumbnailList.contains(mouseInListCoords);
            boolean isMouseOverPopup = previewWindow.contains(mouseInPopupCoords);
            if (!isMouseOverList && !isMouseOverPopup) {
                hidePreview();
            }
        });
        closeTimer.setRepeats(true);

        // 3. AÑADIMOS EL NUEVO LISTENER DE DOBLE CLIC
        thumbnailList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Comprobamos si es un doble clic con el botón izquierdo
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int index = thumbnailList.locationToIndex(e.getPoint());
                    if (index != -1) {
                        lastClickedIndex = index;
                        // Llamamos a showPreview pasando el evento para saber dónde posicionar la ventana
                        showPreview(e); 
                    }
                }
            }
        });
    } // --- FIN del metodo installListeners ---

    private void showPreview(MouseEvent eventTrigger) { // Modificado para recibir el evento
        if (lastClickedIndex == -1) return;

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                // Usamos lastClickedIndex en lugar de lastHoveredIndex
                String imageKey = thumbnailList.getModel().getElementAt(lastClickedIndex);
                java.nio.file.Path imagePath = mainModel.getRutaCompleta(imageKey);
                if (imagePath != null && java.nio.file.Files.exists(imagePath)) {
                    return ImageIO.read(imagePath.toFile());
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    BufferedImage image = get();
                    if (image != null) {
                        // Comprobación de seguridad: ¿sigue el ratón sobre el mismo ítem? Opcional pero bueno.
                        Point currentMousePos = thumbnailList.getMousePosition();
                        if (currentMousePos != null && thumbnailList.locationToIndex(currentMousePos) == lastClickedIndex) {
                            
                            previewModel.setCurrentImage(image);
                            // ... (Toda tu lógica de cálculo de zoom se mantiene igual, es perfecta)
                            double factorZoom;
                            int panelW = PREVIEW_WIDTH;
                            int panelH = PREVIEW_HEIGHT;
                            int imgW = image.getWidth();
                            int imgH = image.getHeight();
                            if (imgW <= 0 || imgH <= 0 || panelW <= 0 || panelH <= 0) {
                                factorZoom = 1.0;
                            } else {
                                double imgAspectRatio = (double) imgW / imgH;
                                double panelAspectRatio = (double) panelW / panelH;
                                if (imgAspectRatio > panelAspectRatio) {
                                    factorZoom = (double) panelW / imgW;
                                } else {
                                    factorZoom = (double) panelH / imgH;
                                }
                            }
                            previewModel.setCurrentZoomMode(ZoomModeEnum.SMART_FIT);
                            previewModel.setZoomFactor(factorZoom);
                            previewModel.resetPan();
                            previewModel.setZoomHabilitado(true);
                            previewPanel.repaint();

                            // Obtenemos la posición de la celda para posicionar la ventana
                            Rectangle cellBounds = thumbnailList.getCellBounds(lastClickedIndex, lastClickedIndex);
                            Point location = cellBounds.getLocation();
                            SwingUtilities.convertPointToScreen(location, thumbnailList);
                            
                            previewWindow.setLocation(location.x, location.y - PREVIEW_HEIGHT);
                            previewWindow.setVisible(true);
                            
                            if (!closeTimer.isRunning()) {
                                closeTimer.start();
                            }
                            
                        } else {
                            hidePreview();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    hidePreview();
                }
            }
        }.execute();
    } // --- FIN del metodo showPreview ---
    

    private void hidePreview() {
        // ELIMINADO: hoverTimer.stop();
        if (closeTimer.isRunning()) {
            closeTimer.stop();
        }
        
        lastClickedIndex = -1; // Cambiado de lastHoveredIndex
        if (previewWindow != null && previewWindow.isVisible()) {
            previewWindow.setVisible(false);
        }
    } // --- FIN del metodo hidePreview ---
    
} // --- FIN DE LA CLASE ThumbnailPreviewer ---


