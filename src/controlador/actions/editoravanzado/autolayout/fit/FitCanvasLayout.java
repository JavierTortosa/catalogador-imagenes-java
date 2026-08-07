package controlador.actions.editoravanzado.autolayout.fit;

import java.awt.Rectangle;
import java.util.List;

import controlador.actions.editoravanzado.autolayout.AutoLayoutAlgorithm;
import controlador.actions.editoravanzado.autolayout.AutoLayoutContext;
import controlador.actions.editoravanzado.autolayout.AutoLayoutSupport;
import modelo.editor.Layer;

/**
 * Ajustar al Canvas: escala la composición de forma uniforme para que su bbox
 * quepa en el lienzo con el margen de ajuste configurado y la centra.
 * <p>
 * Nunca modifica las posiciones relativas de las capas entre sí.
 */
public final class FitCanvasLayout implements AutoLayoutAlgorithm {

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
        int ch = ctx.getCanvasModel().getHeight();
        int fitMargin = ctx.getConfig().getFitMargin();
        double usableW = Math.max(1, cw - 2.0 * fitMargin);
        double usableH = Math.max(1, ch - 2.0 * fitMargin);
        double scale = Math.min(usableW / bbox.width, usableH / bbox.height);
        int scaledW = (int) Math.round(bbox.width * scale);
        int scaledH = (int) Math.round(bbox.height * scale);
        int offsetX = (cw - scaledW) / 2;
        int offsetY = (ch - scaledH) / 2;

        for (Layer layer : targets) {
            Rectangle b = layer.getBounds();
            if (b == null) {
                continue;
            }
            int newX = (int) Math.round(offsetX + (b.x - bbox.x) * scale);
            int newY = (int) Math.round(offsetY + (b.y - bbox.y) * scale);
            int newW = Math.max(1, (int) Math.round(b.width * scale));
            int newH = Math.max(1, (int) Math.round(b.height * scale));
            layer.setBounds(new Rectangle(newX, newY, newW, newH));
        }
    } // --- Fin del metodo layout ---

} // --- Fin de la clase FitCanvasLayout ---
