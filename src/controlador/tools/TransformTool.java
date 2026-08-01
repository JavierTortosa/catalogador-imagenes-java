package controlador.tools;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import modelo.editor.ImageLayer;
import modelo.gizmo.TransformGizmo;
import modelo.gizmo.TransformGizmo.Handle;

/**
 * Herramienta de transformación (mover, escalar, rotar).
 * <p>
 * Lee de la barra el modo (mover/escalar/rotar) y el target (capa/marco de
 * selección). Con target {@code marco}, el gizmo opera sobre los bounds de la
 * {@code SelectionModel} en lugar de sobre la capa activa.
 */
public class TransformTool extends Tool {

    private TransformGizmo gizmo;
    private Handle activeHandle;
    private Point dragStart;

    // Estado de rotación: se re-renderiza desde la imagen original para no
    // acumular error de redondeo en cada arrastre.
    private Rectangle rotateStartBounds;
    private BufferedImage rotateStartImage;
    private double rotateStartAngle;

    @Override
    public String getCommandKey() {
        return controlador.commands.AppActionCommands.CMD_ADVANCED_EDITOR_TRANSFORMAR;
    } // --- Fin del metodo getCommandKey ---

    @Override
    public void onActivate() {
        this.gizmo = ctx.gizmo();
        this.dragStart = null;
        this.activeHandle = null;
    } // --- Fin del metodo onActivate ---

    @Override
    public void onDeactivate() {
        activeHandle = null;
        dragStart = null;
        rotateStartImage = null;
        rotateStartBounds = null;
    } // --- Fin del metodo onDeactivate ---

    private ImageLayer getActiveLayer() {
        if (ctx.layerModel() == null) return null;
        return ctx.layerModel().getActiveLayer() instanceof ImageLayer il ? il : null;
    } // --- Fin del metodo getActiveLayer ---

    private boolean targetMarco() {
        return "marco".equals(ctx.componentBar().getTransformTarget());
    } // --- Fin del metodo targetMarco ---

    /**
     * @return los bounds del elemento a transformar (capa activa o selección),
     *         o null si no hay nada transformable
     */
    private Rectangle getTargetBounds() {
        if (targetMarco()) {
            if (ctx.selectionModel() == null || !ctx.selectionModel().isActive()) return null;
            return ctx.selectionModel().getBounds();
        }
        ImageLayer layer = getActiveLayer();
        return layer != null ? layer.getBounds() : null;
    } // --- Fin del metodo getTargetBounds ---

    /**
     * Filtra los tiradores del gizmo según el modo seleccionado en la barra.
     */
    private boolean allowedHandle(Handle h) {
        String mode = ctx.componentBar().getTransformMode();
        return switch (mode) {
            case "escalar" -> h != Handle.NONE && h != Handle.MOVE && h != Handle.ROTATE;
            case "rotar" -> h == Handle.ROTATE;
            default -> h == Handle.MOVE;
        };
    } // --- Fin del metodo allowedHandle ---

    private void applyTargetBounds(Rectangle newBounds) {
        if (newBounds == null) return;
        if (targetMarco()) {
            if (ctx.selectionModel() != null) {
                ctx.selectionModel().setBounds(newBounds);
            }
        } else {
            ImageLayer layer = getActiveLayer();
            if (layer != null) {
                layer.setBounds(newBounds);
            }
        }
    } // --- Fin del metodo applyTargetBounds ---

    @Override
    public void mousePressed(MouseEvent e) {
        Rectangle bounds = getTargetBounds();
        if (bounds == null) return;

        Handle h = gizmo.hitTest(e.getPoint(), bounds);
        if (!allowedHandle(h)) h = Handle.NONE;
        activeHandle = h;
        dragStart = e.getPoint();

        if (activeHandle == Handle.NONE) return;

        if (activeHandle == Handle.ROTATE) {
            ImageLayer layer = getActiveLayer();
            if (layer == null || layer.getImage() == null) {
                activeHandle = Handle.NONE;
                return;
            }
            rotateStartBounds = new Rectangle(bounds);
            rotateStartImage = copyImage(layer.getImage());
            rotateStartAngle = angleOf(e.getPoint(), bounds);
            return;
        }

        gizmo.startDrag(activeHandle, bounds,
                new TransformGizmo.Constraints(ctx.componentBar().isKeepAspect(), 0, 10));
    } // --- Fin del metodo mousePressed ---

    @Override
    public void mouseDragged(MouseEvent e) {
        if (activeHandle == Handle.ROTATE) {
            Rectangle base = rotateStartBounds;
            if (base == null) return;
            double delta = angleOf(e.getPoint(), base) - rotateStartAngle;
            ImageLayer layer = getActiveLayer();
            if (layer != null) {
                rotateLayer(layer, rotateStartImage, base, delta);
                ctx.canvasPanel().repaint();
            }
            return;
        }

        if (activeHandle == null || dragStart == null) return;
        int dx = e.getPoint().x - dragStart.x;
        int dy = e.getPoint().y - dragStart.y;
        Rectangle newBounds = gizmo.drag(dx, dy);
        if (newBounds != null) {
            applyTargetBounds(newBounds);
            ctx.canvasPanel().repaint();
        }
    } // --- Fin del metodo mouseDragged ---

    @Override
    public void mouseReleased(MouseEvent e) {
        if (activeHandle == null) return;
        if (activeHandle != Handle.ROTATE) {
            gizmo.endDrag();
        }
        activeHandle = null;
        dragStart = null;
        rotateStartImage = null;
        rotateStartBounds = null;
    } // --- Fin del metodo mouseReleased ---

    @Override
    public void mouseMoved(MouseEvent e) {
        Rectangle bounds = getTargetBounds();
        if (bounds == null) return;

        Handle handle = gizmo.hitTest(e.getPoint(), bounds);
        if (allowedHandle(handle)) {
            ctx.canvasPanel().setCursor(gizmo.getCursor(handle));
        } else {
            ctx.canvasPanel().setCursor(getCursor());
        }
    } // --- Fin del metodo mouseMoved ---

    @Override
    public void paintOverlay(Graphics2D g2) {
        Rectangle bounds = getTargetBounds();
        if (bounds == null) return;
        gizmo.draw(g2, bounds);
    } // --- Fin del metodo paintOverlay ---

    @Override
    public Cursor getCursor() {
        Rectangle bounds = getTargetBounds();
        if (bounds == null) return Cursor.getDefaultCursor();
        Point mp = ctx.canvasPanel().getMousePosition();
        if (mp != null) {
            Handle h = gizmo.hitTest(mp, bounds);
            if (allowedHandle(h)) {
                return gizmo.getCursor(h);
            }
        }
        return Cursor.getDefaultCursor();
    } // --- Fin del metodo getCursor ---

    private static BufferedImage copyImage(BufferedImage src) {
        if (src == null) return null;
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    } // --- Fin del metodo copyImage ---

    /**
     * Rota la imagen original alrededor de su centro y reajusta bounds
     * manteniendo el centro del marco base.
     */
    private static void rotateLayer(ImageLayer layer, BufferedImage src,
            Rectangle base, double angleDeg) {
        if (layer == null || src == null) return;

        double rad = Math.toRadians(angleDeg);
        int w = src.getWidth();
        int h = src.getHeight();
        double cos = Math.abs(Math.cos(rad));
        double sin = Math.abs(Math.sin(rad));
        int nw = Math.max(1, (int) Math.ceil(w * cos + h * sin));
        int nh = Math.max(1, (int) Math.ceil(w * sin + h * cos));

        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        AffineTransform tx = new AffineTransform();
        tx.translate((nw - w) / 2.0, (nh - h) / 2.0);
        tx.rotate(rad, w / 2.0, h / 2.0);
        g2.setTransform(tx);
        g2.drawImage(src, 0, 0, null);
        g2.dispose();

        int cx = base.x + base.width / 2;
        int cy = base.y + base.height / 2;
        layer.setImage(out);
        layer.setBounds(new Rectangle(cx - nw / 2, cy - nh / 2, nw, nh));
    } // --- Fin del metodo rotateLayer ---

    private static double angleOf(Point p, Rectangle b) {
        double cx = b.getCenterX();
        double cy = b.getCenterY();
        return Math.toDegrees(Math.atan2(p.y - cy, p.x - cx));
    } // --- Fin del metodo angleOf ---

} // --- Fin de la clase TransformTool ---
