package vista.panels.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;

import javax.swing.JPanel;

import controlador.tools.CanvasController;
import controlador.tools.Tool;
import modelo.editor.CanvasModel;
import modelo.editor.Layer;
import modelo.editor.LayerModel;
import modelo.editor.SelectionModel;

public class CanvasPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private LayerModel layerModel;
    private CanvasModel canvasModel;
    private SelectionModel selectionModel;
    private CanvasController canvasController;

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
        addMouseWheelListener(e -> {
            double oldZoom = zoom;
            double factor = e.getWheelRotation() < 0 ? 1.15 : 0.87;
            double newZoom = Math.max(0.1, Math.min(10.0, zoom * factor));

            // Zoom to cursor
            double mpx = e.getX();
            double mpy = e.getY();
            offsetX = mpx - (mpx - offsetX) * (newZoom / zoom);
            offsetY = mpy - (mpy - offsetY) * (newZoom / zoom);

            zoom = newZoom;
            repaint();
        });
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


    public void setCanvasController(CanvasController cc) {
        this.canvasController = cc;
    } // --- Fin del metodo setCanvasController ---

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
            for (Layer layer : layerModel.getLayers()) {
                if (!layer.isVisible() || layer.getOpacity() < 0.01f) continue;
                layer.paint(g2);
            }
        }

        // Resaltar la capa activa (guía visual de dónde se va a pintar/editar)
        if (layerModel != null && layerModel.getActiveLayer() != null) {
            Layer layer = layerModel.getActiveLayer();
            Rectangle b = layer.getBounds();
            if (b != null) {
                Stroke orig = g2.getStroke();
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_BEVEL, 0, new float[]{4f, 4f}, 0));
                g2.setColor(new Color(255, 140, 0, 160));
                if (layer.getRotation() != 0) {
                    Graphics2D rg = (Graphics2D) g2.create();
                    rg.rotate(Math.toRadians(layer.getRotation()), b.getCenterX(), b.getCenterY());
                    rg.draw(b);
                    rg.dispose();
                } else {
                    g2.draw(b);
                }
                g2.setStroke(orig);
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

        // Draw tool overlay (gizmo, selection preview, etc.)
        if (canvasController != null) {
            Tool tool = canvasController.getActiveTool();
            if (tool != null) {
                tool.paintOverlay(g2);
            }
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
