package controlador.tools;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

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

    // Arrastre de rotación no destructiva (delegado en LayerPicker)
    private LayerPicker.RotateDrag rotateDrag;

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
        rotateDrag = null;
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
     * Sincroniza la rotación del gizmo con la de la capa activa (el marco de
     * selección no tiene rotación propia, se deja a 0).
     */
    private void syncGizmoRotation() {
        double rotation = 0;
        if (!targetMarco()) {
            ImageLayer layer = getActiveLayer();
            if (layer != null) {
                rotation = layer.getRotation();
            }
        }
        gizmo.setRotation(rotation);
    } // --- Fin del metodo syncGizmoRotation ---

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

        syncGizmoRotation();
        Handle h = gizmo.hitTest(e.getPoint(), bounds);
        if (!allowedHandle(h)) h = Handle.NONE;
        activeHandle = h;
        dragStart = e.getPoint();

        if (activeHandle == Handle.NONE) return;

        if (activeHandle == Handle.ROTATE) {
            ImageLayer layer = getActiveLayer();
            if (layer == null) {
                activeHandle = Handle.NONE;
                return;
            }
            rotateDrag = ctx.layerPicker().beginRotate(layer, e.getPoint());
            if (rotateDrag == null) {
                activeHandle = Handle.NONE;
            }
            return;
        }

        gizmo.startDrag(activeHandle, bounds,
                new TransformGizmo.Constraints(ctx.componentBar().isKeepAspect(), 0, 10));
    } // --- Fin del metodo mousePressed ---

    @Override
    public void mouseDragged(MouseEvent e) {
        if (activeHandle == Handle.ROTATE) {
            if (rotateDrag != null) {
                boolean shift = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
                rotateDrag.drag(e.getPoint(), shift);
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
        rotateDrag = null;
    } // --- Fin del metodo mouseReleased ---

    @Override
    public void mouseMoved(MouseEvent e) {
        Rectangle bounds = getTargetBounds();
        if (bounds == null) return;

        syncGizmoRotation();
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
        syncGizmoRotation();
        gizmo.draw(g2, bounds);
    } // --- Fin del metodo paintOverlay ---

    @Override
    public Cursor getCursor() {
        Rectangle bounds = getTargetBounds();
        if (bounds == null) return Cursor.getDefaultCursor();
        Point mp = ctx.canvasPanel().getMousePosition();
        if (mp != null) {
            syncGizmoRotation();
            Handle h = gizmo.hitTest(mp, bounds);
            if (allowedHandle(h)) {
                return gizmo.getCursor(h);
            }
        }
        return Cursor.getDefaultCursor();
    } // --- Fin del metodo getCursor ---

} // --- Fin de la clase TransformTool ---
