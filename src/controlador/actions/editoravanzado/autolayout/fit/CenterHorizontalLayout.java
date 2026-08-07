package controlador.actions.editoravanzado.autolayout.fit;

import java.awt.Rectangle;
import java.util.List;

import controlador.actions.editoravanzado.autolayout.AutoLayoutAlgorithm;
import controlador.actions.editoravanzado.autolayout.AutoLayoutContext;
import controlador.actions.editoravanzado.autolayout.AutoLayoutSupport;
import modelo.editor.Layer;

/**
 * Centrar Composición Horizontalmente: traslada la composición (bbox de las
 * capas objetivo) para centrarla en el eje horizontal del lienzo, sin
 * redimensionar ni rotar ninguna capa.
 */
public final class CenterHorizontalLayout implements AutoLayoutAlgorithm {

    public void layout(AutoLayoutContext ctx) {
        List<Layer> targets = AutoLayoutSupport.getTargets(ctx);
        if (targets.isEmpty()) {
            return;
        }
        AutoLayoutSupport.apartarNoSeleccionadas(ctx);

        Rectangle bbox = AutoLayoutSupport.bboxDe(targets);
        if (bbox == null) {
            return;
        }
        int cw = ctx.getCanvasModel().getWidth();
        int dx = (cw - bbox.width) / 2 - bbox.x;
        for (Layer layer : targets) {
            Rectangle b = layer.getBounds();
            if (b == null) {
                continue;
            }
            layer.setBounds(new Rectangle(b.x + dx, b.y, b.width, b.height));
        }
    } // --- Fin del metodo layout ---

} // --- Fin de la clase CenterHorizontalLayout ---
