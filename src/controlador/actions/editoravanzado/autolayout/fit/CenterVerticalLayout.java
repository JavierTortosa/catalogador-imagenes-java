package controlador.actions.editoravanzado.autolayout.fit;

import java.awt.Rectangle;
import java.util.List;

import controlador.actions.editoravanzado.autolayout.AutoLayoutAlgorithm;
import controlador.actions.editoravanzado.autolayout.AutoLayoutContext;
import controlador.actions.editoravanzado.autolayout.AutoLayoutSupport;
import modelo.editor.Layer;

/**
 * Centrar Composición Verticalmente: traslada la composición (bbox de las capas
 * objetivo) para centrarla en el eje vertical del lienzo, sin redimensionar ni
 * rotar ninguna capa.
 */
public final class CenterVerticalLayout implements AutoLayoutAlgorithm {

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
        int ch = ctx.getCanvasModel().getHeight();
        int dy = (ch - bbox.height) / 2 - bbox.y;
        for (Layer layer : targets) {
            Rectangle b = layer.getBounds();
            if (b == null) {
                continue;
            }
            layer.setBounds(new Rectangle(b.x, b.y + dy, b.width, b.height));
        }
    } // --- Fin del metodo layout ---

} // --- Fin de la clase CenterVerticalLayout ---
