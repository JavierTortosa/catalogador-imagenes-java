package controlador.actions.editoravanzado.autolayout.equal;

import java.awt.Rectangle;
import java.util.List;

import controlador.actions.editoravanzado.autolayout.AutoLayoutAlgorithm;
import controlador.actions.editoravanzado.autolayout.AutoLayoutContext;
import controlador.actions.editoravanzado.autolayout.AutoLayoutSupport;
import modelo.editor.Layer;

/**
 * Igualar Altura: ajusta la altura de cada capa objetivo a la de la capa
 * activa (o a la de la primera seleccionada si no hay capa activa),
 * recalculando la anchura para mantener la proporción.
 * <p>
 * No modifica la posición, la rotación ni la opacidad de las capas.
 */
public final class EqualHeightLayout implements AutoLayoutAlgorithm {

    public void layout(AutoLayoutContext ctx) {
        List<Layer> targets = AutoLayoutSupport.getTargets(ctx);
        if (targets.isEmpty()) {
            return;
        }
        AutoLayoutSupport.apartarNoSeleccionadas(ctx);

        double refH = alturaReferencia(ctx, targets);
        if (refH <= 0) {
            return;
        }
        for (Layer layer : targets) {
            Rectangle b = layer.getBounds();
            if (b == null || b.height <= 0) {
                continue;
            }
            int newW = Math.max(1, (int) Math.round(b.width * (refH / b.height)));
            int newH = (int) Math.round(refH);
            layer.setBounds(new Rectangle(b.x, b.y, newW, newH));
        }
    } // --- Fin del metodo layout ---


    /**
     * Altura de referencia: la de la capa activa, o la de la primera seleccionada.
     */
    private double alturaReferencia(AutoLayoutContext ctx, List<Layer> targets) {
        Layer activa = ctx.getLayerModel().getActiveLayer();
        if (activa != null && activa.getBounds() != null) {
            return activa.getBounds().height;
        }
        Rectangle primero = targets.get(0).getBounds();
        return primero != null ? primero.height : 0;
    } // --- Fin del metodo alturaReferencia ---

} // --- Fin de la clase EqualHeightLayout ---
