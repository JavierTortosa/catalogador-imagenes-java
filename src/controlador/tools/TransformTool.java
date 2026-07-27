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
 * Utiliza {@link TransformGizmo} para dibujar los tiradores y detectar
 * interacción del ratón sobre la capa activa.
 */
public class TransformTool extends Tool {

    private TransformGizmo gizmo;
    private Handle activeHandle;
    private Point dragStart;

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
    } // --- Fin del metodo onDeactivate ---

    private ImageLayer getActiveLayer() {
        if (ctx.layerModel() == null) return null;
        int idx = ctx.layerModel().getActiveIndex();
        if (idx < 0) return null;
        return ctx.layerModel().getLayers().get(idx);
    } // --- Fin del metodo getActiveLayer ---

    @Override
    public void mousePressed(MouseEvent e) {
        ImageLayer layer = getActiveLayer();
        if (layer == null || layer.getBounds() == null) return;

        activeHandle = gizmo.hitTest(e.getPoint(), layer.getBounds());
        if (activeHandle != null && activeHandle != Handle.ROTATE) {
            dragStart = e.getPoint();
            gizmo.startDrag(activeHandle, layer.getBounds(), null);
        }
    } // --- Fin del metodo mousePressed ---

    @Override
    public void mouseDragged(MouseEvent e) {
        if (activeHandle == null || dragStart == null) return;
        ImageLayer layer = getActiveLayer();
        if (layer == null) return;

        int dx = e.getPoint().x - dragStart.x;
        int dy = e.getPoint().y - dragStart.y;
        Rectangle newBounds = gizmo.drag(dx, dy);
        if (newBounds != null) {
            layer.setBounds(newBounds);
        }
    } // --- Fin del metodo mouseDragged ---

    @Override
    public void mouseReleased(MouseEvent e) {
        if (activeHandle == null) return;
        gizmo.endDrag();
        activeHandle = null;
        dragStart = null;
    } // --- Fin del metodo mouseReleased ---

    @Override
    public void mouseMoved(MouseEvent e) {
        ImageLayer layer = getActiveLayer();
        if (layer == null || layer.getBounds() == null) return;

        Handle handle = gizmo.hitTest(e.getPoint(), layer.getBounds());
        if (handle != null && handle != Handle.NONE) {
            ctx.canvasPanel().setCursor(gizmo.getCursor(handle));
        } else {
            ctx.canvasPanel().setCursor(getCursor());
        }
    } // --- Fin del metodo mouseMoved ---

    @Override
    public void paintOverlay(Graphics2D g2) {
        ImageLayer layer = getActiveLayer();
        if (layer == null || layer.getBounds() == null) return;

        gizmo.draw(g2, layer.getBounds());
    } // --- Fin del metodo paintOverlay ---

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    } // --- Fin del metodo getCursor ---

} // --- Fin de la clase TransformTool ---
