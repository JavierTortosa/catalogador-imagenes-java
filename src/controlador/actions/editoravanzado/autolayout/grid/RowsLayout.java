package controlador.actions.editoravanzado.autolayout.grid;

import java.awt.Rectangle;
import java.util.List;

import controlador.actions.editoravanzado.autolayout.AutoLayoutAlgorithm;
import controlador.actions.editoravanzado.autolayout.AutoLayoutContext;
import controlador.actions.editoravanzado.autolayout.AutoLayoutSupport;
import modelo.editor.Layer;

/**
 * Compactar por Filas: dispone las capas de izquierda a derecha en filas.
 * <p>
 * El reparto del número de capas entre filas es equilibrado (nunca una fila
 * casi vacía): con 12 capas se obtiene 4/4/4, no 10/2. Si la composición no
 * cabe en el lienzo, se aplica una escala uniforme de optimización.
 */
public final class RowsLayout implements AutoLayoutAlgorithm {

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
        int cols = (int) Math.ceil(Math.sqrt(n));
        int rows = (int) Math.ceil((double) n / cols);
        int base = n / rows;
        int extra = n % rows;
        int[] perRow = new int[rows];
        for (int r = 0; r < rows; r++) {
            perRow[r] = base + (r < extra ? 1 : 0);
        }

        double[] rowHeights = new double[rows];
        int idx = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < perRow[r]; c++) {
                Rectangle b = targets.get(idx).getBounds();
                if (b != null) {
                    rowHeights[r] = Math.max(rowHeights[r], b.height);
                }
                idx++;
            }
        }

        double[][] posX = new double[n][1];
        double[][] posY = new double[n][1];
        double curY = margin;
        double maxRowW = 0;
        idx = 0;
        for (int r = 0; r < rows; r++) {
            double rowW = 0;
            for (int c = 0; c < perRow[r]; c++) {
                Rectangle b = targets.get(idx).getBounds();
                double w = (b != null) ? b.width : 1;
                double h = (b != null) ? b.height : 1;
                rowW += w + (c > 0 ? spacing : 0);
                posX[idx][0] = margin + (rowW - w);
                posY[idx][0] = curY + (rowHeights[r] - h) / 2.0;
                idx++;
            }
            maxRowW = Math.max(maxRowW, rowW);
            curY += rowHeights[r] + spacing;
        }
        double packedH = curY - spacing + margin;

        double availW = Math.max(1, cw - 2.0 * margin);
        double availH = Math.max(1, ch - 2.0 * margin);
        double scale = Math.min(1.0, Math.min(availW / maxRowW, availH / packedH));

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

} // --- Fin de la clase RowsLayout ---
