package controlador.actions.editoravanzado.autolayout.grid;

import java.awt.Rectangle;
import java.util.List;

import controlador.actions.editoravanzado.autolayout.AutoLayoutAlgorithm;
import controlador.actions.editoravanzado.autolayout.AutoLayoutContext;
import controlador.actions.editoravanzado.autolayout.AutoLayoutSupport;
import modelo.editor.CanvasModel;
import modelo.editor.Layer;

/**
 * Grid Conservando Tamaño: cuadrícula que respeta el tamaño natural de las
 * capas. La celda de cada capa se calcula con la propia capa (máximo de la
 * columna/fila). El canvas solo crece hacia la derecha y hacia abajo; el origen
 * nunca se desplaza.
 */
public final class GridKeepSizeLayout implements AutoLayoutAlgorithm {

    public void layout(AutoLayoutContext ctx) {
        List<Layer> targets = AutoLayoutSupport.getTargets(ctx);
        if (targets.isEmpty()) {
            return;
        }
        AutoLayoutSupport.apartarNoSeleccionadas(ctx);

        CanvasModel canvasModel = ctx.getCanvasModel();
        int spacing = ctx.getConfig().getSpacing();
        int margin = ctx.getConfig().getMargin();
        int n = targets.size();
        int cols = (int) Math.ceil(Math.sqrt(n));
        int rows = (int) Math.ceil((double) n / cols);

        double[] colWidths = new double[cols];
        double[] rowHeights = new double[rows];
        for (int i = 0; i < n; i++) {
            Rectangle b = targets.get(i).getBounds();
            if (b == null) {
                continue;
            }
            int col = i % cols;
            int row = i / cols;
            colWidths[col] = Math.max(colWidths[col], b.width);
            rowHeights[row] = Math.max(rowHeights[row], b.height);
        }

        double[] xOffsets = new double[cols];
        double cx = margin;
        for (int c = 0; c < cols; c++) {
            xOffsets[c] = cx;
            cx += colWidths[c] + spacing;
        }
        double[] yOffsets = new double[rows];
        double cy = margin;
        for (int r = 0; r < rows; r++) {
            yOffsets[r] = cy;
            cy += rowHeights[r] + spacing;
        }

        double gridW = cx - spacing + margin;
        double gridH = cy - spacing + margin;
        int newCanvasW = Math.max(canvasModel.getWidth(), (int) Math.ceil(gridW));
        int newCanvasH = Math.max(canvasModel.getHeight(), (int) Math.ceil(gridH));
        canvasModel.setSize(newCanvasW, newCanvasH);

        for (int i = 0; i < n; i++) {
            Layer layer = targets.get(i);
            Rectangle b = layer.getBounds();
            if (b == null) {
                continue;
            }
            int col = i % cols;
            int row = i / cols;
            int newX = (int) Math.round(xOffsets[col] + (colWidths[col] - b.width) / 2.0);
            int newY = (int) Math.round(yOffsets[row] + (rowHeights[row] - b.height) / 2.0);
            layer.setBounds(new Rectangle(newX, newY, b.width, b.height));
        }
    } // --- Fin del metodo layout ---

} // --- Fin de la clase GridKeepSizeLayout ---
