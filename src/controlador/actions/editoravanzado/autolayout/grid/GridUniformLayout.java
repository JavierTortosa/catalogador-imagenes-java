package controlador.actions.editoravanzado.autolayout.grid;

import java.awt.Rectangle;
import java.util.List;

import controlador.actions.editoravanzado.autolayout.AutoLayoutAlgorithm;
import controlador.actions.editoravanzado.autolayout.AutoLayoutContext;
import controlador.actions.editoravanzado.autolayout.AutoLayoutSupport;
import modelo.editor.Layer;

/**
 * Grid Uniforme: dispone las capas en una cuadrícula de celdas iguales,
 * escalando cada capa (manteniendo la proporción) para encajar en su celda.
 */
public final class GridUniformLayout implements AutoLayoutAlgorithm {

    public void layout(AutoLayoutContext ctx) {
        List<Layer> targets = AutoLayoutSupport.getTargets(ctx);
        if (targets.isEmpty()) {
            return;
        }
        AutoLayoutSupport.apartarNoSeleccionadas(ctx);

        int cw = ctx.getCanvasModel().getWidth();
        int ch = ctx.getCanvasModel().getHeight();
        int margin = ctx.getConfig().getMargin();
        int n = targets.size();
        int cols = (int) Math.ceil(Math.sqrt(n));
        int rows = (int) Math.ceil((double) n / cols);
        double usableW = Math.max(1, cw - 2.0 * margin);
        double usableH = Math.max(1, ch - 2.0 * margin);
        double cellW = usableW / cols;
        double cellH = usableH / rows;

        for (int i = 0; i < n; i++) {
            Layer layer = targets.get(i);
            Rectangle b = layer.getBounds();
            if (b == null) {
                continue;
            }
            int col = i % cols;
            int row = i / cols;
            double scale = Math.min(cellW / b.width, cellH / b.height);
            int newW = Math.max(1, (int) Math.round(b.width * scale));
            int newH = Math.max(1, (int) Math.round(b.height * scale));
            int newX = margin + (int) Math.round(col * cellW + (cellW - newW) / 2.0);
            int newY = margin + (int) Math.round(row * cellH + (cellH - newH) / 2.0);
            layer.setBounds(new Rectangle(newX, newY, newW, newH));
        }
    } // --- Fin del metodo layout ---

} // --- Fin de la clase GridUniformLayout ---
