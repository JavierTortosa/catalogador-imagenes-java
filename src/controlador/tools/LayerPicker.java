package controlador.tools;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.function.Predicate;

import modelo.editor.Layer;
import modelo.gizmo.TransformGizmo;
import modelo.gizmo.TransformGizmo.Handle;

/**
 * Servicio compartido de selección y manipulación de capas.
 * <p>
 * Encapsula la lógica que antes estaba duplicada en EditTool, TextTool y
 * TransformTool: hit-test de capas por punto, activación (respetando el
 * checkbox de auto-selección), deselección, arrastre para mover y arrastre de
 * tiradores del gizmo. Se inyecta en {@link ToolContext}.
 */
public class LayerPicker {

    private ToolContext ctx;

    /**
     * @param ctx contexto compartido (se actualiza con {@link #setContext})
     */
    public LayerPicker(ToolContext ctx) {
        this.ctx = ctx;
    } // --- Fin del constructor LayerPicker ---

    /**
     * Reemplaza el contexto (útil si se reconstruyen los modelos).
     */
    public void setContext(ToolContext ctx) {
        this.ctx = ctx;
    } // --- Fin del metodo setContext ---

    /**
     * @return la capa activa del modelo, o null si no hay
     */
    public Layer getActiveLayer() {
        return ctx.layerModel() != null ? ctx.layerModel().getActiveLayer() : null;
    } // --- Fin del metodo getActiveLayer ---

    /**
     * Busca la capa visible y no bloqueada más frontal cuyos bounds contengan
     * el punto.
     */
    public Layer findLayerAt(Point p) {
        return findLayerAt(p, l -> true);
    } // --- Fin del metodo findLayerAt ---

    /**
     * Busca la capa más frontal que cumpla el filtro y contenga el punto.
     *
     * @param p      coordenadas del canvas
     * @param filter filtro de tipo (ej. {@code l -> l instanceof TextLayer})
     */
    public Layer findLayerAt(Point p, Predicate<Layer> filter) {
        List<Layer> layers = ctx.layerModel().getLayers();
        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer l = layers.get(i);
            if (!l.isVisible() || l.isLocked()) continue;
            if (!filter.test(l)) continue;
            if (l.getBounds() != null && l.getBounds().contains(p)) {
                return l;
            }
        }
        return null;
    } // --- Fin del metodo findLayerAt ---

    public boolean isAutoSelect() {
        return ctx.componentBar() != null && ctx.componentBar().isAutoSelect();
    } // --- Fin del metodo isAutoSelect ---

    public boolean isShowGizmo() {
        return ctx.componentBar() != null && ctx.componentBar().isShowGizmo();
    } // --- Fin del metodo isShowGizmo ---

    public boolean isKeepAspect() {
        return ctx.componentBar() != null && ctx.componentBar().isKeepAspect();
    } // --- Fin del metodo isKeepAspect ---

    /**
     * Activa una capa: la marca como activa en el modelo y sincroniza la barra
     * de opciones y el panel de capas con sus valores.
     */
    public void activateLayer(Layer layer) {
        if (layer == null || ctx.layerModel() == null) return;
        ctx.layerModel().setActiveLayer(layer);
        if (ctx.componentBar() != null) {
            ctx.componentBar().updateEditLayerFields(layer);
        }
        ctx.canvasPanel().repaint();
    } // --- Fin del metodo activateLayer ---

    /**
     * Si la auto-selección está activa, activa la capa bajo el cursor.
     */
    public void activateLayerAt(Point p) {
        activateLayerAt(p, l -> true);
    } // --- Fin del metodo activateLayerAt ---

    /**
     * Si la auto-selección está activa, activa la capa bajo el cursor que
     * cumpla el filtro.
     */
    public void activateLayerAt(Point p, Predicate<Layer> filter) {
        if (!isAutoSelect()) return;
        Layer hit = findLayerAt(p, filter);
        if (hit != null) {
            activateLayer(hit);
        }
    } // --- Fin del metodo activateLayerAt ---

    /**
     * Deselecciona la capa activa y limpia la barra de opciones.
     */
    public void clearSelection() {
        if (ctx.layerModel() == null) return;
        ctx.layerModel().setActiveLayer(-1);
        if (ctx.componentBar() != null) {
            ctx.componentBar().updateEditLayerFields(null);
        }
        ctx.canvasPanel().repaint();
    } // --- Fin del metodo clearSelection ---

    /**
     * Hit-test del gizmo sobre los bounds de la capa.
     */
    public Handle gizmoHitTest(Point p, Layer layer) {
        if (layer == null || layer.getBounds() == null) return Handle.NONE;
        return ctx.gizmo().hitTest(p, layer.getBounds());
    } // --- Fin del metodo gizmoHitTest ---

    /**
     * Inicia un arrastre de tirador del gizmo (mover/escalar) sobre la capa.
     * Debe llamarse con un handle distinto de NONE.
     *
     * @return el drag activo, o null si la capa no tiene bounds
     */
    public GizmoDrag beginGizmo(Handle handle, Layer layer) {
        if (layer == null || layer.getBounds() == null || handle == Handle.NONE) return null;
        TransformGizmo.Constraints c = new TransformGizmo.Constraints(isKeepAspect(), 0, 10);
        ctx.gizmo().startDrag(handle, layer.getBounds(), c);
        return new GizmoDrag(ctx, handle);
    } // --- Fin del metodo beginGizmo ---

    /**
     * Inicia un arrastre de rotación no destructiva sobre la capa.
     *
     * @param layer capa a rotar (debe soportar {@link Layer#setRotation})
     * @param start punto del ratón en el que se inicia el arrastre
     * @return el drag de rotación activo, o null si la capa no tiene bounds
     */
    public RotateDrag beginRotate(Layer layer, Point start) {
        if (layer == null || layer.getBounds() == null || start == null) return null;
        return new RotateDrag(layer, new Point(start), layer.getRotation(), layer.getBounds());
    } // --- Fin del metodo beginRotate ---

    /**
     * Inicia un arrastre para mover la capa.
     *
     * @return el drag activo, o null si la capa no tiene bounds
     */
    public DragMove beginMove(Point p, Layer layer) {
        if (layer == null || layer.getBounds() == null) return null;
        return new DragMove(layer, new Point(p), new Rectangle(layer.getBounds()));
    } // --- Fin del metodo beginMove ---

    /**
     * Drag activo de tirador del gizmo. {@link #drag} recibe los deltas
     * acumulativos desde el inicio del arrastre.
     */
    public static final class GizmoDrag {

        private final ToolContext ctx;
        private final Handle handle;

        private GizmoDrag(ToolContext ctx, Handle handle) {
            this.ctx = ctx;
            this.handle = handle;
        } // --- Fin del constructor GizmoDrag ---

        public Handle getHandle() {
            return handle;
        } // --- Fin del metodo getHandle ---

        public Rectangle drag(int dx, int dy) {
            return ctx.gizmo().drag(dx, dy);
        } // --- Fin del metodo drag ---

        public void end() {
            ctx.gizmo().endDrag();
        } // --- Fin del metodo end ---

    } // --- Fin de la clase GizmoDrag ---

    /**
     * Drag activo de rotación no destructiva. Calcula el ángulo a partir de la
     * posición del ratón relativa al centro del bounds de la capa.
     */
    public static final class RotateDrag {

        private final Layer layer;
        private final Point start;
        private final double startRotation;
        private final Rectangle startBounds;

        private RotateDrag(Layer layer, Point start, double startRotation, Rectangle startBounds) {
            this.layer = layer;
            this.start = start;
            this.startRotation = startRotation;
            this.startBounds = startBounds;
        } // --- Fin del constructor RotateDrag ---

        /**
         * Aplica la rotación según la posición actual del ratón.
         *
         * @param current  posición actual del ratón
         * @param shiftSnap si true, ajusta el ángulo a múltiplos de 15°
         * @return el ángulo aplicado en grados
         */
        public double drag(Point current, boolean shiftSnap) {
            double cx = startBounds.getCenterX();
            double cy = startBounds.getCenterY();
            double startAngle = Math.toDegrees(Math.atan2(start.y - cy, start.x - cx));
            double currentAngle = Math.toDegrees(Math.atan2(current.y - cy, current.x - cx));
            double rotation = startRotation + (currentAngle - startAngle);
            if (shiftSnap) {
                rotation = Math.round(rotation / 15.0) * 15.0;
            }
            layer.setRotation(rotation);
            return rotation;
        } // --- Fin del metodo drag ---

        public void end() {
            // sin estado interno que liberar
        } // --- Fin del metodo end ---

    } // --- Fin de la clase RotateDrag ---

    /**
     * Drag activo para mover una capa arrastrándola.
     */
    public static final class DragMove {

        private final Layer layer;
        private final Point start;
        private final Rectangle startBounds;

        private DragMove(Layer layer, Point start, Rectangle startBounds) {
            this.layer = layer;
            this.start = start;
            this.startBounds = startBounds;
        } // --- Fin del constructor DragMove ---

        public Point getStart() {
            return start;
        } // --- Fin del metodo getStart ---

        /**
         * Aplica el desplazamiento acumulado y devuelve los nuevos bounds.
         */
        public Rectangle move(int dx, int dy) {
            Rectangle b = startBounds;
            Rectangle nb = new Rectangle(b.x + dx, b.y + dy, b.width, b.height);
            layer.setBounds(nb);
            return nb;
        } // --- Fin del metodo move ---

    } // --- Fin de la clase DragMove ---

} // --- Fin de la clase LayerPicker ---
