package controlador.actions.editoravanzado.autolayout.grid;

import java.awt.Rectangle;
import java.util.List;

import controlador.actions.editoravanzado.autolayout.AutoLayoutAlgorithm;
import controlador.actions.editoravanzado.autolayout.AutoLayoutContext;
import controlador.actions.editoravanzado.autolayout.AutoLayoutSupport;
import modelo.editor.Layer;

/**
 * Compactar por Columnas: versión vertical de {@link RowsLayout}.
 * <p>
 * Reparte las capas de forma equilibrada entre columnas (nunca una columna casi
 * vacía) y aplica una escala uniforme de optimización si la composición no cabe.
 */
public final class ColumnsLayout implements AutoLayoutAlgorithm {

    public void layout(AutoLayoutContext ctx) {
        List<Layer> targets = AutoLayoutSupport.getTargets(ctx);
        if (targets.isEmpty()) {
            return;
        }
        AutoLayoutSupport.apartarNoSeleccionadas(ctx);

        int cw = ctx.getCanvasModel().getWidth();
        int ch = ctx.getCanvasModel().getHeight();
        int margin = ctx.getConfig().getMargin();
        int spacing = ctx.getConfig().getSpacing();
        int n = targets.size();
        int rows = (int) Math.ceil(Math.sqrt(n));
        int cols = (int) Math.ceil((double) n / rows);
        int base = n / cols;
        int extra = n % cols;
        int[] perCol = new int[cols];
        for (int c = 0; c < cols; c++) {
            perCol[c] = base + (c < extra ? 1 : 0);
        }

        double[] colWidths = new double[cols];
        int idx = 0;
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < perCol[c]; r++) {
                Rectangle b = targets.get(idx).getBounds();
                if (b != null) {
                    colWidths[c] = Math.max(colWidths[c], b.width);
                }
                idx++;
            }
        }

        double[][] posX = new double[n][1];
        double[][] posY = new double[n][1];
        double curX = margin;
        double maxColH = 0;
        idx = 0;
        for (int c = 0; c < cols; c++) {
            double colH = 0;
            for (int r = 0; r < perCol[c]; r++) {
                Rectangle b = targets.get(idx).getBounds();
                double w = (b != null) ? b.width : 1;
                double h = (b != null) ? b.height : 1;
                colH += h + (r > 0 ? spacing : 0);
                posX[idx][0] = curX + (colWidths[c] - w) / 2.0;
                posY[idx][0] = margin + (colH - h);
                idx++;
            }
            maxColH = Math.max(maxColH, colH);
            curX += colWidths[c] + spacing;
        }
        double packedW = curX - spacing + margin;

        double availW = Math.max(1, cw - 2.0 * margin);
        double availH = Math.max(1, ch - 2.0 * margin);
        double scale = Math.min(1.0, Math.min(availW / packedW, availH / maxColH));

        for (int i = 0; i < n; i++) {
            Layer layer = targets.get(i);
            Rectangle b = layer.getBounds();
            if (b == null) {
                continue;
            }
            int newW = Math.max(1, (int) Math.round(b.width * scale));
            int newH = Math.max(1, (int) Math.round(b.height * scale));
            int newX = margin + (int) Math.round((posX[i][0] - margin) * scale);
            int newY = margin + (int) Math.round((posY[i][0] - margin) * scale);
            layer.setBounds(new Rectangle(newX, newY, newW, newH));
        }
    } // --- Fin del metodo layout ---

} // --- Fin de la clase ColumnsLayout ---
