package controlador.actions.editoravanzado.autolayout;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import modelo.editor.Layer;
import modelo.editor.LayerModel;

/**
 * Utilidades compartidas por los algoritmos Auto Layout.
 * <p>
 * Todos los helpers trabajan únicamente con el {@link AutoLayoutContext}, de
 * modo que los algoritmos no necesitan conocer el modelo directamente.
 */
public final class AutoLayoutSupport {

    private AutoLayoutSupport() {
    } // --- Fin del constructor AutoLayoutSupport ---


    /**
     * Capas objetivo de una operación Auto Layout: seleccionadas, visibles y no
     * bloqueadas, en orden Z (fondo primero). Sin selección devuelve lista vacía.
     *
     * @param ctx contexto de la operación
     * @return capas objetivo en orden Z
     */
    public static List<Layer> getTargets(AutoLayoutContext ctx) {
        LayerModel layerModel = ctx.getLayerModel();
        List<Layer> targets = new ArrayList<>();
        if (layerModel == null) {
            return targets;
        }
        for (int idx : layerModel.getSelectedIndices()) {
            Layer layer = layerModel.getLayer(idx);
            if (layer != null && layer.isVisible() && !layer.isLocked()) {
                targets.add(layer);
            }
        }
        return targets;
    } // --- Fin del metodo getTargets ---


    /**
     * Aplica el comportamiento configurado a las capas visibles no seleccionadas.
     * <p>
     * Con {@link AutoLayoutConfig.NoSeleccionadasMode#SACAR_FUERA} se trasladan
     * fuera del canvas (x = ancho del lienzo + margen, misma y, sin cambiar su
     * tamaño). Con {@code IGNORAR} no se tocan.
     *
     * @param ctx contexto de la operación
     */
    public static void apartarNoSeleccionadas(AutoLayoutContext ctx) {
        if (ctx.getConfig().getNoSeleccionadas() == AutoLayoutConfig.NoSeleccionadasMode.IGNORAR) {
            return;
        }
        LayerModel layerModel = ctx.getLayerModel();
        if (layerModel == null) {
            return;
        }
        int canvasW = ctx.getCanvasModel().getWidth();
        int margin = ctx.getConfig().getMargin();
        List<Layer> layers = layerModel.getLayers();
        for (int i = 0; i < layers.size(); i++) {
            if (layerModel.isSelected(i)) {
                continue;
            }
            Layer layer = layers.get(i);
            if (!layer.isVisible() || layer.isLocked()) {
                continue;
            }
            Rectangle b = layer.getBounds();
            if (b == null) {
                continue;
            }
            layer.setBounds(new Rectangle(canvasW + margin, b.y, b.width, b.height));
        }
    } // --- Fin del metodo apartarNoSeleccionadas ---


    /**
     * Rectángulo unión (bbox) de los bounds de las capas indicadas.
     *
     * @param targets capas a unir
     * @return bbox de la composición, o {@code null} si no hay bounds válidos
     */
    public static Rectangle bboxDe(List<Layer> targets) {
        Rectangle union = null;
        for (Layer layer : targets) {
            Rectangle b = layer.getBounds();
            if (b == null) {
                continue;
            }
            union = (union == null) ? new Rectangle(b) : union.union(b);
        }
        return union;
    } // --- Fin del metodo bboxDe ---

} // --- Fin de la clase AutoLayoutSupport ---
