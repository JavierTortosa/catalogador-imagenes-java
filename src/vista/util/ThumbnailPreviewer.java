package vista.util;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.ZoomManager;
import modelo.VisorModel;
import servicios.zoom.ZoomModeEnum;
import vista.panels.ImageDisplayPanel;
import vista.theme.ThemeManager;

public class ThumbnailPreviewer {

	private static final Logger logger = LoggerFactory.getLogger(ThumbnailPreviewer.class);
	
    private final JList<String> thumbnailList;
    private final VisorModel mainModel;
    private final ThemeManager themeManager;

    private JWindow previewWindow;
    private ImageDisplayPanel previewPanel;
    private VisorModel previewModel;
    private ZoomManager previewZoomManager;

    private Timer closeTimer;
    private int lastClickedIndex = -1;

    private static final int PREVIEW_WIDTH = 400;
    private static final int PREVIEW_HEIGHT = 400;

    private AWTEventListener clickOutsideListener;

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
        
        Color borderColor = themeManager.getTemaActual().colorBordeSeleccionActiva();
        int borderThickness = 5;
        int padding = 10;

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
            @Override public void mousePressed(MouseEvent e) { 
                if (previewModel.isZoomHabilitado()) {
                    previewZoomManager.iniciarPaneo(e);
                }
            }
        });
        internalLabel.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (previewModel.isZoomHabilitado()) {
                    previewZoomManager.continuarPaneo(e);
                }
            }
        });
    } // --- FIN del metodo setupPreviewComponents ---

    
    private void installListeners() {
        closeTimer = new Timer(250, e -> {
            if (previewWindow == null || !previewWindow.isVisible()) {
                ((Timer)e.getSource()).stop();
                return;
            }
            Point mousePos = java.awt.MouseInfo.getPointerInfo().getLocation();
            
            // <<< CAMBIO: Lógica del Timer corregida para ser más permisiva
            Point mouseInListCoords = new Point(mousePos);
            SwingUtilities.convertPointFromScreen(mouseInListCoords, thumbnailList);

            Point mouseInPopupCoords = new Point(mousePos);
            SwingUtilities.convertPointFromScreen(mouseInPopupCoords, previewWindow);

            // Comprobamos si el ratón está sobre la parte visible de la lista O sobre la ventana emergente
            boolean isMouseOverList = thumbnailList.getVisibleRect().contains(mouseInListCoords);
            boolean isMouseOverPopup = previewWindow.getBounds().contains(mouseInPopupCoords);

            // Solo cerramos si NO está sobre NINGUNO de los dos
            if (!isMouseOverList && !isMouseOverPopup) {
                hidePreview();
            }
        });
        closeTimer.setRepeats(true);

        thumbnailList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int index = thumbnailList.locationToIndex(e.getPoint());
                    if (index != -1) {
                        // Si ya hay una ventana visible para el mismo índice, la cerramos. Si no, la mostramos.
                        if (previewWindow.isVisible() && index == lastClickedIndex) {
                            hidePreview();
                        } else {
                            lastClickedIndex = index;
                            showPreview();
                        }
                    }
                }
            } // --- Fin del método mouseClicked ---
        });

        clickOutsideListener = event -> {
            if (event instanceof MouseEvent && event.getID() == MouseEvent.MOUSE_CLICKED) {
                if (previewWindow != null && previewWindow.isVisible()) {
                    MouseEvent me = (MouseEvent) event;
                    Point clickPoint = me.getLocationOnScreen();
                    
                    SwingUtilities.convertPointFromScreen(clickPoint, previewWindow);
                    
                    if (!previewWindow.contains(clickPoint)) {
                        hidePreview();
                    }
                }
            }
        };
    } // --- FIN del metodo installListeners ---

    private void showPreview() {
        if (lastClickedIndex == -1) return;

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                String imageKey = thumbnailList.getModel().getElementAt(lastClickedIndex);
                java.nio.file.Path imagePath = mainModel.getRutaCompleta(imageKey);
                if (imagePath != null && java.nio.file.Files.exists(imagePath)) {
                    return ImageIO.read(imagePath.toFile());
                }
                return null;
            } // --- Fin del método doInBackground ---

            @Override
            protected void done() {
                try {
                    BufferedImage image = get();
                    if (image != null) {
                        previewModel.setCurrentImage(image);
                        double factorZoom = Math.min(
                            (double) PREVIEW_WIDTH / image.getWidth(), 
                            (double) PREVIEW_HEIGHT / image.getHeight()
                        );
                        previewModel.setCurrentZoomMode(ZoomModeEnum.SMART_FIT);
                        previewModel.setZoomFactor(factorZoom);
                        previewModel.resetPan();
                        previewModel.setZoomHabilitado(true);
                        previewPanel.repaint();

                        Rectangle cellBounds = thumbnailList.getCellBounds(lastClickedIndex, lastClickedIndex);
                        Point cellLocationOnScreen = cellBounds.getLocation();
                        SwingUtilities.convertPointToScreen(cellLocationOnScreen, thumbnailList);
                        Rectangle screenBounds = thumbnailList.getGraphicsConfiguration().getBounds();

                        int finalX = cellLocationOnScreen.x;
                        int finalY;

                        if (cellLocationOnScreen.y - PREVIEW_HEIGHT > screenBounds.y) {
                            finalY = cellLocationOnScreen.y - PREVIEW_HEIGHT;
                        } else {
                            finalY = cellLocationOnScreen.y + cellBounds.height;
                        }
                        
                        if (finalX + PREVIEW_WIDTH > screenBounds.x + screenBounds.width) {
                            finalX = screenBounds.x + screenBounds.width - PREVIEW_WIDTH;
                        }
                        if (finalX < screenBounds.x) {
                            finalX = screenBounds.x;
                        }
                        
                        previewWindow.setLocation(finalX, finalY);
                        previewWindow.setVisible(true);
                        
                        if (!closeTimer.isRunning()) {
                            closeTimer.start();
                        }

                        // <<< CAMBIO: El listener se añade en un invokeLater para evitar la condición de carrera
                        SwingUtilities.invokeLater(() -> {
                            Toolkit.getDefaultToolkit().addAWTEventListener(clickOutsideListener, AWTEvent.MOUSE_EVENT_MASK);
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    hidePreview();
                }
            } // --- Fin del método done ---
        }.execute();
    } // --- FIN del metodo showPreview ---
    
    private void hidePreview() {
        if (closeTimer.isRunning()) {
            closeTimer.stop();
        }
        
        lastClickedIndex = -1;
        if (previewWindow != null && previewWindow.isVisible()) {
            previewWindow.setVisible(false);
        }
        
        // <<< CAMBIO: El listener se elimina directamente
        Toolkit.getDefaultToolkit().removeAWTEventListener(clickOutsideListener);
    } // --- FIN del metodo hidePreview ---
    
} // --- FIN DE LA CLASE ThumbnailPreviewer ---

