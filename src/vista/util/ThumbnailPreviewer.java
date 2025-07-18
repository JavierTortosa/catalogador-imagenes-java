package vista.util;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
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

public class ThumbnailPreviewer {

    private final JList<String> thumbnailList;
    private final VisorModel mainModel;

//    private JPopupMenu previewPopup;
    private JWindow previewWindow;
    private ImageDisplayPanel previewPanel;
    private VisorModel previewModel;
    private ZoomManager previewZoomManager;

    private Timer closeTimer;
    private Timer hoverTimer;
    private int lastHoveredIndex = -1;

    private static final int HOVER_DELAY_MS = 500;
    private static final int PREVIEW_WIDTH = 400;
    private static final int PREVIEW_HEIGHT = 400;

    public ThumbnailPreviewer(JList<String> thumbnailList, VisorModel mainModel) {
        this.thumbnailList = thumbnailList;
        this.mainModel = mainModel;
        
        setupPreviewComponents();
        installListeners();
    }

    private void setupPreviewComponents() {
        // Crea el modelo y el panel de previsualización aislados
        this.previewModel = new VisorModel();
        this.previewPanel = new ImageDisplayPanel(null, previewModel);
        
        // Crea una ventana emergente (JWindow) sin decoración
        Window owner = SwingUtilities.getWindowAncestor(thumbnailList);
        this.previewWindow = new JWindow(owner);
        this.previewWindow.setSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        this.previewWindow.getContentPane().add(this.previewPanel);
        this.previewWindow.setFocusableWindowState(false);

        // Crea un ZoomManager dedicado para este panel
        this.previewZoomManager = new ZoomManager();
        this.previewZoomManager.setModel(previewModel);

        // ¡VÍNCULO CLAVE! Le decimos al ZoomManager que opere SOBRE el panel del popup
        this.previewZoomManager.setSpecificPanel(this.previewPanel);

        // ¡PUNTO CLAVE! Obtenemos el JLabel interno del panel, que es el que recibe los eventos
        JLabel internalLabel = this.previewPanel.getInternalLabel();

        // Añadimos los listeners de ratón AL LABEL, no al panel
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

        // Configuración de los temporizadores para mostrar/ocultar el popup
        this.hoverTimer = new Timer(HOVER_DELAY_MS, e -> showPreview());
        this.hoverTimer.setRepeats(false);
    } // --- Fin del método setupPreviewComponents ---

    private void installListeners() {
        // 1. EL TEMPORIZADOR DE APARICIÓN (HOVER) - SIN CAMBIOS
        hoverTimer = new Timer(HOVER_DELAY_MS, e -> showPreview());
        hoverTimer.setRepeats(false);

        // 2. EL NUEVO TEMPORIZADOR DE CIERRE
        // Este timer se ejecutará cada 250ms para comprobar si debe cerrar el popup.
        closeTimer = new Timer(250, e -> {
            // Si el popup no está visible, no hay nada que hacer.
            if (previewWindow == null || !previewWindow.isVisible()) {
                ((Timer)e.getSource()).stop(); // Detenemos el timer si no hay nada que vigilar
                return;
            }

            // Obtiene la posición actual del ratón en la pantalla
            Point mousePos = java.awt.MouseInfo.getPointerInfo().getLocation();

            // Convierte la posición del ratón a las coordenadas de la lista y del popup
            Point mouseInListCoords = new Point(mousePos);
            SwingUtilities.convertPointFromScreen(mouseInListCoords, thumbnailList);

            Point mouseInPopupCoords = new Point(mousePos);
            SwingUtilities.convertPointFromScreen(mouseInPopupCoords, previewWindow);

            // Comprueba si el ratón está DENTRO de los límites de la lista O del popup
            boolean isMouseOverList = thumbnailList.contains(mouseInListCoords);
            boolean isMouseOverPopup = previewWindow.contains(mouseInPopupCoords);

            // Si el ratón NO está sobre NINGUNO de los dos, cierra el popup
            if (!isMouseOverList && !isMouseOverPopup) {
                hidePreview();
            }
        });
        closeTimer.setRepeats(true); // Queremos que se ejecute continuamente mientras el popup esté visible

        // 3. LISTENER DE MOVIMIENTO - MUY SIMPLIFICADO
        // Su única responsabilidad es iniciar el temporizador de aparición.
        thumbnailList.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = thumbnailList.locationToIndex(e.getPoint());
                if (index != -1 && index != lastHoveredIndex) {
                    lastHoveredIndex = index;
                    hoverTimer.restart(); // Inicia la cuenta atrás para mostrar el popup
                }
            }
        });

        // 4. LISTENER DE SALIDA - MUY SIMPLIFICADO
        // Su única responsabilidad es detener el temporizador de aparición.
        thumbnailList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoverTimer.stop();
            }
        });

    } // --- Fin del nuevo método installListeners ---
    
    
    private void showPreview() {
        if (lastHoveredIndex == -1) return;

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                String imageKey = thumbnailList.getModel().getElementAt(lastHoveredIndex);
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
                        Point mousePos = thumbnailList.getMousePosition();
                        if (mousePos != null && thumbnailList.locationToIndex(mousePos) == lastHoveredIndex) {
                            
                            previewModel.setCurrentImage(image);
                            
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
                            previewModel.setZoomHabilitado(true); // Habilitamos el paneo/zoom
                            
                            previewPanel.repaint();

                            Rectangle cellBounds = thumbnailList.getCellBounds(lastHoveredIndex, lastHoveredIndex);
                            Point location = cellBounds.getLocation();
                            SwingUtilities.convertPointToScreen(location, thumbnailList);
                            
                            // Posicionar la ventana emergente encima de la miniatura
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
    }

    private void hidePreview() {
        // --- MODIFICACIÓN CLAVE ---
        // Al ocultar el popup, también detenemos AMBOS temporizadores.
        hoverTimer.stop();
        if (closeTimer.isRunning()) {
            closeTimer.stop();
        }
        // --- FIN DE LA MODIFICACIÓN ---

        lastHoveredIndex = -1;
        if (previewWindow != null && previewWindow.isVisible()) {
            previewWindow.setVisible(false);
        }
    } // --- Fin del método hidePreview ---

}