package controlador.actions.editoravanzado;

import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Collectors;

import modelo.editor.CanvasModel;
import modelo.editor.Layer;
import modelo.editor.LayerModel;
import vista.panels.render.CanvasPanel;

public final class AutoDistributeActions {

    private AutoDistributeActions() {
    } // --- Fin del constructor AutoDistributeActions ---


    public static void distribucionFija(CanvasPanel canvas, LayerModel layerModel, CanvasModel canvasModel) {
        List<Layer> visibles = getVisibleLayers(layerModel);
        if (visibles.isEmpty()) return;

        int cw = canvasModel.getWidth();
        int ch = canvasModel.getHeight();
        int cols = (int) Math.ceil(Math.sqrt(visibles.size()));
        int rows = (int) Math.ceil((double) visibles.size() / cols);
        double cellW = (double) cw / cols;
        double cellH = (double) ch / rows;
        double margin = 0.85;

        for (int i = 0; i < visibles.size(); i++) {
            Layer layer = visibles.get(i);
            int col = i % cols;
            int row = i / cols;
            Rectangle b = layer.getBounds();
            if (b == null) continue;

            double scale = Math.min(cellW * margin / b.width, cellH * margin / b.height);
            int newW = (int) Math.round(b.width * scale);
            int newH = (int) Math.round(b.height * scale);
            int newX = (int) Math.round(col * cellW + (cellW - newW) / 2.0);
            int newY = (int) Math.round(row * cellH + (cellH - newH) / 2.0);

            layer.setBounds(new Rectangle(newX, newY, Math.max(1, newW), Math.max(1, newH)));
        }

        canvas.repaint();
    } // --- Fin del metodo distribucionFija ---


    public static void distribucionPorCapa(CanvasPanel canvas, LayerModel layerModel, CanvasModel canvasModel) {
        List<Layer> visibles = getVisibleLayers(layerModel);
        if (visibles.isEmpty()) return;

        int cw = canvasModel.getWidth();
        int ch = canvasModel.getHeight();
        int cols = (int) Math.ceil(Math.sqrt(visibles.size()));
        int rows = (int) Math.ceil((double) visibles.size() / cols);
        double spacing = 20;
        double usableW = cw - spacing * (cols + 1);
        double usableH = ch - spacing * (rows + 1);
        if (usableW < 1) usableW = 1;
        if (usableH < 1) usableH = 1;

        double totalW = 0;
        double totalH = 0;
        for (int i = 0; i < Math.min(visibles.size(), cols); i++) {
            Rectangle b = visibles.get(i).getBounds();
            if (b != null) totalW += b.width;
        }
        for (int i = 0; i < Math.min(visibles.size(), rows); i++) {
            Rectangle b = visibles.get(i * cols).getBounds();
            if (b != null) totalH += b.height;
        }
        if (totalW < 1) totalW = 1;
        if (totalH < 1) totalH = 1;

        double scaleW = usableW / totalW;
        double scaleH = usableH / totalH;

        double[] colWidths = new double[cols];
        for (int c = 0; c < cols; c++) {
            int idx = c;
            if (idx < visibles.size()) {
                Rectangle b = visibles.get(idx).getBounds();
                double w = b != null ? b.width * scaleW : 1;
                colWidths[c] = w;
            } else {
                colWidths[c] = 1;
            }
        }

        double[] rowHeights = new double[rows];
        for (int r = 0; r < rows; r++) {
            int idx = r * cols;
            if (idx < visibles.size()) {
                Rectangle b = visibles.get(idx).getBounds();
                double h = b != null ? b.height * scaleH : 1;
                rowHeights[r] = h;
            } else {
                rowHeights[r] = 1;
            }
        }

        double[] xOffsets = new double[cols];
        double cx = spacing;
        for (int c = 0; c < cols; c++) {
            xOffsets[c] = cx;
            cx += colWidths[c] + spacing;
        }

        double[] yOffsets = new double[rows];
        double cy = spacing;
        for (int r = 0; r < rows; r++) {
            yOffsets[r] = cy;
            cy += rowHeights[r] + spacing;
        }

        for (int i = 0; i < visibles.size(); i++) {
            Layer layer = visibles.get(i);
            int col = i % cols;
            int row = i / cols;
            Rectangle b = layer.getBounds();
            if (b == null) continue;

            double nw = colWidths[col];
            double nh = rowHeights[row];
            double nx = xOffsets[col] + (colWidths[col] - nw) / 2.0;
            double ny = yOffsets[row] + (rowHeights[row] - nh) / 2.0;

            layer.setBounds(new Rectangle(
                    (int) Math.round(nx),
                    (int) Math.round(ny),
                    Math.max(1, (int) Math.round(nw)),
                    Math.max(1, (int) Math.round(nh))));
        }

        canvas.repaint();
    } // --- Fin del metodo distribucionPorCapa ---


    public static void escalarParaAjustar(CanvasPanel canvas, LayerModel layerModel, CanvasModel canvasModel) {
        List<Layer> visibles = getVisibleLayers(layerModel);
        if (visibles.isEmpty()) return;

        int cw = canvasModel.getWidth();
        int ch = canvasModel.getHeight();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Layer layer : visibles) {
            Rectangle b = layer.getBounds();
            if (b == null) continue;
            minX = Math.min(minX, b.x);
            minY = Math.min(minY, b.y);
            maxX = Math.max(maxX, b.x + b.width);
            maxY = Math.max(maxY, b.y + b.height);
        }

        if (minX == Integer.MAX_VALUE) return;

        int unionW = maxX - minX;
        int unionH = maxY - minY;
        if (unionW < 1 || unionH < 1) return;

        double scale = Math.min((double) cw / unionW, (double) ch / unionH) * 0.9;
        int scaledW = (int) Math.round(unionW * scale);
        int scaledH = (int) Math.round(unionH * scale);
        int offsetX = (cw - scaledW) / 2;
        int offsetY = (ch - scaledH) / 2;

        for (Layer layer : visibles) {
            Rectangle b = layer.getBounds();
            if (b == null) continue;
            double relX = b.x - minX;
            double relY = b.y - minY;
            int newX = (int) Math.round(offsetX + relX * scale);
            int newY = (int) Math.round(offsetY + relY * scale);
            int newW = Math.max(1, (int) Math.round(b.width * scale));
            int newH = Math.max(1, (int) Math.round(b.height * scale));

            layer.setBounds(new Rectangle(newX, newY, newW, newH));
        }

        canvas.repaint();
    } // --- Fin del metodo escalarParaAjustar ---


    private static List<Layer> getVisibleLayers(LayerModel layerModel) {
        if (layerModel == null) return java.util.Collections.emptyList();
        return layerModel.getLayers().stream()
                .filter(Layer::isVisible)
                .collect(Collectors.toList());
    } // --- Fin del metodo getVisibleLayers ---

} // --- Fin de la clase AutoDistributeActions ---
