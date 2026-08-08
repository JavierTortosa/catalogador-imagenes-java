package vista.panels.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;

import javax.swing.JPanel;
import javax.swing.Timer;

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
    private SelectionModel selectionModel = new SelectionModel();
    private CanvasController canvasController;

    // Checkerboard colors for transparency
    private static final Color CHECK_LIGHT = new Color(0xCC, 0xCC, 0xCC);
    private static final Color CHECK_DARK  = new Color(0x99, 0x99, 0x99);
    private static final int CHECK_SIZE = 12;

    private double zoom = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;

    // Hormigueo de la selección (marching ants)
    private static final float[] DASH_PATTERN = new float[]{4f, 4f};
    private static final float DASH_LEN = 4f;
    private Timer antsTimer;
    private int antsPhase;

    public CanvasPanel() {
        setOpaque(true);
        setBackground(new Color(0x33, 0x33, 0x33));
        antsTimer = new Timer(120, e -> {
            antsPhase++;
            repaint();
        });
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
    public void removeNotify() {
        if (antsTimer != null) {
            antsTimer.stop();
        }
        super.removeNotify();
    } // --- Fin del metodo removeNotify ---

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Sincronizar el hormigueo: activo solo mientras haya selección
        boolean haySeleccion = selectionModel != null && selectionModel.isActive();
        if (haySeleccion && !antsTimer.isRunning()) {
            antsTimer.start();
        } else if (!haySeleccion && antsTimer.isRunning()) {
            antsTimer.stop();
        }

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
            if (selectionModel.hasMask()) {
                drawMaskSelectionOverlay(g2, selectionModel);
            } else {
                Rectangle sel = selectionModel.getBounds();
                g2.setColor(new Color(0, 120, 215, 60));
                g2.fill(sel);
                Stroke origSel = g2.getStroke();
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_BEVEL, 0, DASH_PATTERN, antsPhase));
                g2.setColor(Color.BLACK);
                g2.draw(sel);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_BEVEL, 0, DASH_PATTERN, antsPhase + DASH_LEN));
                g2.setColor(Color.WHITE);
                g2.draw(sel);
                g2.setStroke(origSel);
            }
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

    /**
     * Dibuja la selección con forma irregular (máscara de píxeles): relleno
     * semitransparente de la zona real más un contorno punteado que sigue su
     * borde.
     *
     * @param g2 gráficos del overlay en coordenadas de canvas
     * @param sm selección con máscara activa
     */
    private void drawMaskSelectionOverlay(Graphics2D g2, SelectionModel sm) {
        Rectangle sel = sm.getBounds();
        java.util.BitSet mask = sm.getMask();
        int w = sel.width;
        int h = sel.height;
        if (mask == null || w <= 0 || h <= 0) return;

        // Relleno: pintar píxel a píxel los bits seleccionados (bordes también)
        g2.setColor(new Color(0, 120, 215, 60));
        for (int i = mask.nextSetBit(0); i >= 0; i = mask.nextSetBit(i + 1)) {
            if (i >= w * h) break;
            int px = sel.x + (i % w);
            int py = sel.y + (i / w);
            g2.fillRect(px, py, 1, 1);
        }

        // Contorno: píxeles seleccionados con algún vecino no seleccionado.
        // Se dibujan como dashes marchando (hormigueo): trazo de DASH_LEN px con
        // hueco de DASH_LEN px, alternando blanco y negro por segmento, según la
        // fase que avanza con el timer.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                if (!mask.get(idx)) continue;
                boolean edge = x == 0 || !mask.get(idx - 1)
                        || x == w - 1 || !mask.get(idx + 1)
                        || y == 0 || !mask.get(idx - w)
                        || y == h - 1 || !mask.get(idx + w);
                if (!edge) continue;
                int ciclo = (int) (DASH_LEN * 2);
                int t = x + y + antsPhase;
                int seg = t % ciclo;
                if (seg >= (int) DASH_LEN) continue;
                boolean claro = ((t / ciclo) & 1) == 1;
                g2.setColor(claro ? Color.WHITE : Color.BLACK);
                g2.fillRect(sel.x + x, sel.y + y, 1, 1);
            }
        }
    } // --- Fin del metodo drawMaskSelectionOverlay ---

} // --- Fin de la clase CanvasPanel ---
