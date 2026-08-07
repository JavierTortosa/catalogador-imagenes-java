package controlador.actions.editoravanzado.autolayout;

import java.awt.Rectangle;
import java.util.List;

import modelo.editor.Layer;

/**
 * Espiral: coloca las capas a lo largo de una espiral de Arquímedes que parte
 * del centro del lienzo, con separación angular uniforme ({@code spacing}) y
 * radio creciente hasta encajar en el lienzo. Determinista.
 */
public final class SpiralLayout implements AutoLayoutAlgorithm {

    public void layout(AutoLayoutContext ctx) {
        List<Layer> targets = AutoLayoutSupport.getTargets(ctx);
        if (targets.isEmpty()) {
            return;
        }
        AutoLayoutSupport.apartarNoSeleccionadas(ctx);

        int cw = ctx.getCanvasModel().getWidth();
        int ch = ctx.getCanvasModel().getHeight();
        int spacing = ctx.getConfig().getSpacing();
        double usableW = Math.max(1, cw - 2.0 * spacing);
        double usableH = Math.max(1, ch - 2.0 * spacing);
        double maxR = Math.min(usableW, usableH) / 2.0;
        double cx = cw / 2.0;
        double cy = ch / 2.0;
        int n = targets.size();

        double layersPerTurn = Math.max(1.0, maxR / Math.max(1, spacing));
        for (int i = 0; i < n; i++) {
            Layer layer = targets.get(i);
            Rectangle b = layer.getBounds();
            if (b == null) {
                continue;
            }
            double theta = i * (2.0 * Math.PI / layersPerTurn);
            double radius = (n > 1) ? maxR * i / (n - 1) : maxR / 2.0;
            int newX = (int) Math.round(cx + radius * Math.cos(theta) - b.width / 2.0);
            int newY = (int) Math.round(cy + radius * Math.sin(theta) - b.height / 2.0);
            layer.setBounds(new Rectangle(newX, newY, b.width, b.height));
        }
    } // --- Fin del metodo layout ---

} // --- Fin de la clase SpiralLayout ---
