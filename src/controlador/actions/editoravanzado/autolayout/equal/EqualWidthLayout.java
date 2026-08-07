package controlador.actions.editoravanzado.autolayout.equal;

import java.awt.Rectangle;
import java.util.List;

import controlador.actions.editoravanzado.autolayout.AutoLayoutAlgorithm;
import controlador.actions.editoravanzado.autolayout.AutoLayoutContext;
import controlador.actions.editoravanzado.autolayout.AutoLayoutSupport;
import modelo.editor.Layer;

/**
 * Igualar Anchura: ajusta la anchura de cada capa objetivo a la de la capa
 * activa (o a la de la primera seleccionada si no hay capa activa),
 * recalculando la altura para mantener la proporción.
 * <p>
 * No modifica la posición, la rotación ni la opacidad de las capas.
 */
public final class EqualWidthLayout implements AutoLayoutAlgorithm {

    public void layout(AutoLayoutContext ctx) {
        List<Layer> targets = AutoLayoutSupport.getTargets(ctx);
        if (targets.isEmpty()) {
            return;
        }
        AutoLayoutSupport.apartarNoSeleccionadas(ctx);

        double refW = anchuraReferencia(ctx, targets);
        if (refW <= 0) {
            return;
        }
        for (Layer layer : targets) {
            Rectangle b = layer.getBounds();
            if (b == null || b.width <= 0) {
                continue;
            }
            int newH = Math.max(1, (int) Math.round(b.height * (refW / b.width)));
            int newW = (int) Math.round(refW);
            layer.setBounds(new Rectangle(b.x, b.y, newW, newH));
        }
    } // --- Fin del metodo layout ---


    /**
     * Anchura de referencia: la de la capa activa, o la de la primera seleccionada.
     */
    private double anchuraReferencia(AutoLayoutContext ctx, List<Layer> targets) {
        Layer activa = ctx.getLayerModel().getActiveLayer();
        if (activa != null && activa.getBounds() != null) {
            return activa.getBounds().width;
        }
        Rectangle primero = targets.get(0).getBounds();
        return primero != null ? primero.width : 0;
    } // --- Fin del metodo anchuraReferencia ---

} // --- Fin de la clase EqualWidthLayout ---
