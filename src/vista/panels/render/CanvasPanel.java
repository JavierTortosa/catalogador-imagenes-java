package vista.panels.render;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import modelo.editor.CanvasModel;
import modelo.editor.ImageLayer;
import modelo.editor.LayerModel;
import modelo.editor.SelectionModel;

public class CanvasPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private LayerModel layerModel;
    private CanvasModel canvasModel;
    private SelectionModel selectionModel;

    // Checkerboard colors for transparency
    private static final Color CHECK_LIGHT = new Color(0xCC, 0xCC, 0xCC);
    private static final Color CHECK_DARK  = new Color(0x99, 0x99, 0x99);
    private static final int CHECK_SIZE = 12;

    private double zoom = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;

    public CanvasPanel() {
        setOpaque(true);
        setBackground(new Color(0x33, 0x33, 0x33));
    } // --- Fin del constructor CanvasPanel ---

    public void setLayerModel(LayerModel layerModel) {
        this.layerModel = layerModel;
        repaint();
    } // --- Fin del metodo setLayerModel ---

    public void setCanvasModel(CanvasModel canvasModel) {
        this.canvasModel = canvasModel;
        repaint();
    } // --- Fin del metodo setCanvasModel ---

    public void setSelectionModel(SelectionModel selectionModel) {
        this.selectionModel = selectionModel;
        repaint();
    } // --- Fin del metodo setSelectionModel ---

    public LayerModel getLayerModel() {
        return layerModel;
    } // --- Fin del metodo getLayerModel ---

    public CanvasModel getCanvasModel() {
        return canvasModel;
    } // --- Fin del metodo getCanvasModel ---

    public SelectionModel getSelectionModel() {
        return selectionModel;
    } // --- Fin del metodo getSelectionModel ---

    public void setZoom(double zoom) {
        this.zoom = Math.max(0.1, zoom);
        repaint();
    } // --- Fin del metodo setZoom ---

    public double getZoom() {
        return zoom;
    } // --- Fin del metodo getZoom ---

    public double getOffsetX() {
        return offsetX;
    } // --- Fin del metodo getOffsetX ---

    public double getOffsetY() {
        return offsetY;
    } // --- Fin del metodo getOffsetY ---

    public void setPan(double offsetX, double offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        repaint();
    } // --- Fin del metodo setPan ---

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int panelW = getWidth();
        int panelH = getHeight();

        // Fill panel background (dark area outside canvas)
        g2.setColor(getBackground());
        g2.fillRect(0, 0, panelW, panelH);

        if (canvasModel == null) {
            g2.dispose();
            return;
        }

        int canvasW = canvasModel.getWidth();
        int canvasH = canvasModel.getHeight();
        if (canvasW <= 0 || canvasH <= 0) {
            g2.dispose();
            return;
        }

        // Apply zoom and pan
        g2.translate(offsetX, offsetY);
        g2.scale(zoom, zoom);

        // Draw transparency checkerboard if canvas is transparent
        if (canvasModel.isTransparent() || canvasModel.getBackgroundColor() == null) {
            drawCheckerboard(g2, canvasW, canvasH);
        } else {
            g2.setColor(canvasModel.getBackgroundColor());
            g2.fillRect(0, 0, canvasW, canvasH);
        }

        // Draw layers bottom-to-top
        if (layerModel != null) {
            for (ImageLayer layer : layerModel.getLayers()) {
                if (!layer.isVisible() || layer.getImage() == null) continue;

                Rectangle bounds = layer.getBounds();
                if (bounds == null) continue;

                float opacity = layer.getOpacity();
                if (opacity < 0.01f) continue;

                if (opacity < 1.0f) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
                }

                g2.drawImage(layer.getImage(), bounds.x, bounds.y, bounds.width, bounds.height, null);

                if (opacity < 1.0f) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                }
            }
        }

        // Draw selection overlay
        if (selectionModel != null && selectionModel.isActive()) {
            Rectangle sel = selectionModel.getBounds();
            g2.setColor(new Color(0, 120, 215, 60));
            g2.fill(sel);
            g2.setColor(new Color(0, 120, 215));
            g2.draw(sel);
        }

        g2.dispose();
    } // --- Fin del metodo paintComponent ---

    private void drawCheckerboard(Graphics2D g2, int w, int h) {
        for (int y = 0; y < h; y += CHECK_SIZE) {
            for (int x = 0; x < w; x += CHECK_SIZE) {
                boolean light = ((x / CHECK_SIZE) + (y / CHECK_SIZE)) % 2 == 0;
                g2.setColor(light ? CHECK_LIGHT : CHECK_DARK);
                int tw = Math.min(CHECK_SIZE, w - x);
                int th = Math.min(CHECK_SIZE, h - y);
                g2.fillRect(x, y, tw, th);
            }
        }
    } // --- Fin del metodo drawCheckerboard ---

} // --- Fin de la clase CanvasPanel ---
