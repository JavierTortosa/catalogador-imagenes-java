package controlador.tools;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Busca todas las capas visibles y no bloqueadas cuyos bounds se crucen con
     * el rectángulo dado, en orden de apilado (fondo primero).
     *
     * @param rect rectángulo en coordenadas de canvas
     * @return lista de capas atrapadas (puede estar vacía)
     */
    public List<Layer> findLayersIn(Rectangle rect) {
        List<Layer> result = new ArrayList<>();
        if (rect == null) return result;
        List<Layer> layers = ctx.layerModel().getLayers();
        for (Layer l : layers) {
            if (!l.isVisible() || l.isLocked()) continue;
            Rectangle b = l.getBounds();
            if (b != null && b.intersects(rect)) {
                result.add(l);
            }
        }
        return result;
    } // --- Fin del metodo findLayersIn ---

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
     * Deselecciona todas las capas (activa y selección múltiple) y limpia la
     * barra de opciones.
     */
    public void clearSelection() {
        if (ctx.layerModel() == null) return;
        ctx.layerModel().clearAllSelection();
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
     * Rectángulo unión de los bounds de varias capas (marco global de la
     * selección múltiple). Devuelve {@code null} si la lista está vacía o
     * ninguna capa tiene bounds.
     */
    public Rectangle unionBounds(List<Layer> layers) {
        if (layers == null || layers.isEmpty()) return null;
        Rectangle union = null;
        for (Layer layer : layers) {
            Rectangle b = layer.getBounds();
            if (b == null) continue;
            union = (union == null) ? new Rectangle(b) : union.union(b);
        }
        return union;
    } // --- Fin del metodo unionBounds ---

    /**
     * Inicia un arrastre de tirador del gizmo sobre varias capas a la vez. El
     * gizmo opera sobre la unión de sus bounds y cada capa se re-posiciona y
     * re-escala proporcionalmente respecto al marco inicial.
     *
     * @param handle      tirador arrastrado (MOVE y ROTATE no aplican aquí)
     * @param layers      capas seleccionadas a transformar juntas
     * @param startUnion  unión de los bounds al iniciar el arrastre
     * @return el drag activo, o null si no se puede iniciar
     */
    public MultiGizmoDrag beginMultiGizmo(Handle handle, List<Layer> layers, Rectangle startUnion) {
        if (layers == null || layers.isEmpty() || handle == Handle.NONE || startUnion == null) return null;
        TransformGizmo.Constraints c = new TransformGizmo.Constraints(isKeepAspect(), 0, 10);
        ctx.gizmo().startDrag(handle, startUnion, c);
        return new MultiGizmoDrag(ctx, handle, layers, startUnion);
    } // --- Fin del metodo beginMultiGizmo ---

    /**
     * Inicia un arrastre de rotación no destructiva sobre varias capas: cada
     * capa rota alrededor del centro de la unión de sus bounds (como un grupo).
     *
     * @param layers     capas a rotar juntas
     * @param startUnion unión de los bounds al iniciar el arrastre
     * @param start      punto del ratón en el que se inicia el arrastre
     * @return el drag de rotación activo, o null si no se puede iniciar
     */
    public MultiRotateDrag beginMultiRotate(List<Layer> layers, Rectangle startUnion, Point start) {
        if (layers == null || layers.isEmpty() || startUnion == null || start == null) return null;
        return new MultiRotateDrag(layers, startUnion, new Point(start));
    } // --- Fin del metodo beginMultiRotate ---

    /**
     * Inicia un arrastre para mover la capa.
     *
     * @return el drag activo, o null si la capa no tiene bounds
     */
    public DragMove beginMove(Point p, Layer layer) {
        if (layer == null) return null;
        return beginMove(p, List.of(layer));
    } // --- Fin del metodo beginMove ---

    /**
     * Inicia un arrastre para mover varias capas a la vez (las que estén
     * seleccionadas). Cada capa conserva sus bounds de partida para poder
     * cancelar el arrastre.
     *
     * @param layers capas a mover juntas
     * @param p      punto del ratón en el que se inicia el arrastre
     * @return el drag activo, o null si la lista está vacía
     */
    public DragMove beginMove(Point p, List<Layer> layers) {
        if (layers == null || layers.isEmpty()) return null;
        return new DragMove(layers, new Point(p));
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

        /**
         * Cancela el arrastre restaurando los bounds iniciales.
         *
         * @return los bounds originales restaurados
         */
        public Rectangle cancel() {
            Rectangle restored = ctx.gizmo().drag(0, 0);
            ctx.gizmo().endDrag();
            return restored;
        } // --- Fin del metodo cancel ---

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

        /**
         * Cancela la rotación restaurando el ángulo inicial.
         */
        public void cancel() {
            layer.setRotation(startRotation);
        } // --- Fin del metodo cancel ---

    } // --- Fin de la clase RotateDrag ---

    /**
     * Drag activo de tirador del gizmo sobre varias capas a la vez. El gizmo
     * calcula el nuevo rectángulo de la unión y cada capa se re-coloca y
     * re-escala proporcionalmente (mismas coordenadas relativas dentro del
     * marco). También cubre MOVE: con escala 1:1 equivale a desplazar todas.
     */
    public static final class MultiGizmoDrag {

        private final ToolContext ctx;
        private final List<Layer> layers;
        private final Rectangle startUnion;
        private final Map<String, Rectangle> startBounds;

        private MultiGizmoDrag(ToolContext ctx, Handle handle, List<Layer> layers, Rectangle startUnion) {
            this.ctx = ctx;
            this.layers = layers;
            this.startUnion = new Rectangle(startUnion);
            this.startBounds = new HashMap<>();
            for (Layer layer : layers) {
                if (layer.getBounds() != null) {
                    startBounds.put(layer.getId(), new Rectangle(layer.getBounds()));
                }
            }
        } // --- Fin del constructor MultiGizmoDrag ---

        /**
         * Aplica el desplazamiento acumulado al marco y a todas las capas.
         *
         * @return el nuevo rectángulo de la unión (tras la transformación)
         */
        public Rectangle drag(int dx, int dy) {
            Rectangle newUnion = ctx.gizmo().drag(dx, dy);
            if (newUnion == null) return startUnion;
            for (Layer layer : layers) {
                Rectangle b = startBounds.get(layer.getId());
                if (b == null) continue;
                double relX = startUnion.width > 0 ? (double) (b.x - startUnion.x) / startUnion.width : 0.0;
                double relY = startUnion.height > 0 ? (double) (b.y - startUnion.y) / startUnion.height : 0.0;
                double relW = startUnion.width > 0 ? (double) b.width / startUnion.width : 1.0;
                double relH = startUnion.height > 0 ? (double) b.height / startUnion.height : 1.0;
                int nx = (int) Math.round(newUnion.x + relX * newUnion.width);
                int ny = (int) Math.round(newUnion.y + relY * newUnion.height);
                int nw = Math.max(1, (int) Math.round(relW * newUnion.width));
                int nh = Math.max(1, (int) Math.round(relH * newUnion.height));
                layer.setBounds(new Rectangle(nx, ny, nw, nh));
            }
            return newUnion;
        } // --- Fin del metodo drag ---

        public void end() {
            ctx.gizmo().endDrag();
        } // --- Fin del metodo end ---

        /**
         * Cancela el arrastre restaurando los bounds iniciales de cada capa.
         */
        public void cancel() {
            for (Layer layer : layers) {
                Rectangle b = startBounds.get(layer.getId());
                if (b != null) {
                    layer.setBounds(b);
                }
            }
            ctx.gizmo().endDrag();
        } // --- Fin del metodo cancel ---

    } // --- Fin de la clase MultiGizmoDrag ---

    /**
     * Drag activo de rotación no destructiva de varias capas como grupo: cada
     * capa rota su centro alrededor del centro de la unión y añade el mismo
     * incremento de ángulo a su propia rotación.
     */
    public static final class MultiRotateDrag {

        private final List<Layer> layers;
        private final Point start;
        private final Rectangle startUnion;
        private final Map<String, Rectangle> startBounds;
        private final Map<String, Double> startRotations;

        private MultiRotateDrag(List<Layer> layers, Rectangle startUnion, Point start) {
            this.layers = layers;
            this.startUnion = new Rectangle(startUnion);
            this.start = start;
            this.startBounds = new HashMap<>();
            this.startRotations = new HashMap<>();
            for (Layer layer : layers) {
                if (layer.getBounds() != null) {
                    startBounds.put(layer.getId(), new Rectangle(layer.getBounds()));
                    startRotations.put(layer.getId(), layer.getRotation());
                }
            }
        } // --- Fin del constructor MultiRotateDrag ---

        /**
         * Aplica la rotación de grupo según la posición actual del ratón.
         *
         * @param current   posición actual del ratón
         * @param shiftSnap si true, ajusta el incremento a múltiplos de 15°
         * @return el incremento de ángulo aplicado en grados
         */
        public double drag(Point current, boolean shiftSnap) {
            double cx = startUnion.getCenterX();
            double cy = startUnion.getCenterY();
            double startAngle = Math.toDegrees(Math.atan2(start.y - cy, start.x - cx));
            double currentAngle = Math.toDegrees(Math.atan2(current.y - cy, current.x - cx));
            double delta = currentAngle - startAngle;
            if (shiftSnap) {
                delta = Math.round(delta / 15.0) * 15.0;
            }
            double rad = Math.toRadians(delta);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            for (Layer layer : layers) {
                Rectangle b = startBounds.get(layer.getId());
                Double rot = startRotations.get(layer.getId());
                if (b == null || rot == null) continue;
                double lcx = b.getCenterX();
                double lcy = b.getCenterY();
                double dxc = lcx - cx;
                double dyc = lcy - cy;
                double ncx = cx + dxc * cos - dyc * sin;
                double ncy = cy + dxc * sin + dyc * cos;
                Rectangle nb = new Rectangle(
                        (int) Math.round(ncx - b.width / 2.0),
                        (int) Math.round(ncy - b.height / 2.0),
                        b.width, b.height);
                layer.setBounds(nb);
                layer.setRotation(rot + delta);
            }
            return delta;
        } // --- Fin del metodo drag ---

        public void end() {
            // sin estado interno que liberar
        } // --- Fin del metodo end ---

        /**
         * Cancela la rotación restaurando posición y ángulo iniciales de cada capa.
         */
        public void cancel() {
            for (Layer layer : layers) {
                Rectangle b = startBounds.get(layer.getId());
                Double rot = startRotations.get(layer.getId());
                if (b != null) {
                    layer.setBounds(b);
                }
                if (rot != null) {
                    layer.setRotation(rot);
                }
            }
        } // --- Fin del metodo cancel ---

    } // --- Fin de la clase MultiRotateDrag ---

    /**
     * Drag activo para mover una o varias capas arrastrándolas juntas.
     */
    public static final class DragMove {

        private final List<Layer> layers;
        private final Point start;
        private final Map<String, Rectangle> startBounds;

        private DragMove(List<Layer> layers, Point start) {
            this.layers = layers;
            this.start = start;
            this.startBounds = new HashMap<>();
            for (Layer layer : layers) {
                if (layer.getBounds() != null) {
                    startBounds.put(layer.getId(), new Rectangle(layer.getBounds()));
                }
            }
        } // --- Fin del constructor DragMove ---

        public Point getStart() {
            return start;
        } // --- Fin del metodo getStart ---

        /**
         * Aplica el desplazamiento acumulado a todas las capas y devuelve los
         * nuevos bounds de la última de la lista.
         */
        public Rectangle move(int dx, int dy) {
            Rectangle last = null;
            for (Layer layer : layers) {
                Rectangle b = startBounds.get(layer.getId());
                if (b == null) continue;
                Rectangle nb = new Rectangle(b.x + dx, b.y + dy, b.width, b.height);
                layer.setBounds(nb);
                last = nb;
            }
            return last;
        } // --- Fin del metodo move ---

        /**
         * Cancela el movimiento restaurando los bounds iniciales de cada capa.
         */
        public void cancel() {
            for (Layer layer : layers) {
                Rectangle b = startBounds.get(layer.getId());
                if (b != null) {
                    layer.setBounds(b);
                }
            }
        } // --- Fin del metodo cancel ---

    } // --- Fin de la clase DragMove ---

} // --- Fin de la clase LayerPicker ---
