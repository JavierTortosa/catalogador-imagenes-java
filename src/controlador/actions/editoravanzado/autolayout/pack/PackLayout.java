package controlador.actions.editoravanzado.autolayout.pack;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import controlador.actions.editoravanzado.autolayout.AutoLayoutAlgorithm;
import controlador.actions.editoravanzado.autolayout.AutoLayoutConfig;
import controlador.actions.editoravanzado.autolayout.AutoLayoutContext;
import controlador.actions.editoravanzado.autolayout.AutoLayoutSupport;
import modelo.editor.Layer;

/**
 * Compactar: empaqueta las capas minimizando los espacios vacíos mediante un
 * algoritmo determinista seleccionado por {@link AutoLayoutConfig.PackMode}.
 * <p>
 * Los modos ordenan las capas de forma fija (SHELF por altura, SKYLINE y
 * GUILLOTINE por área) y las colocan sin solaparse. Si el resultado no cabe en
 * el lienzo, se aplica una escala uniforme y la composición se centra.
 */
public final class PackLayout implements AutoLayoutAlgorithm {

    // Rectángulo libre usado por el modo guillotina.
    private static final class FreeRect {
        final double x;
        final double y;
        final double w;
        final double h;

        FreeRect(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    // Pieza a empaquetar: índice de capa y dimensiones naturales.
    private static final class Item {
        final int index;
        final double w;
        final double h;

        Item(int index, double w, double h) {
            this.index = index;
            this.w = w;
            this.h = h;
        }
    }

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
        double usableW = Math.max(1, cw - 2.0 * margin);
        double usableH = Math.max(1, ch - 2.0 * margin);

        List<Item> items = new ArrayList<>();
        for (int i = 0; i < targets.size(); i++) {
            Rectangle b = targets.get(i).getBounds();
            if (b != null) {
                items.add(new Item(i, b.width, b.height));
            }
        }
        if (items.isEmpty()) {
            return;
        }

        double[] x = new double[items.size()];
        double[] y = new double[items.size()];
        switch (ctx.getConfig().getPackMode()) {
            case SKYLINE -> packSkyline(items, usableW, spacing, x, y);
            case GUILLOTINE -> packGuillotine(items, usableW, usableH, spacing, x, y);
            default -> packShelf(items, usableW, spacing, x, y);
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            minX = Math.min(minX, x[i]);
            minY = Math.min(minY, y[i]);
            maxX = Math.max(maxX, x[i] + it.w);
            maxY = Math.max(maxY, y[i] + it.h);
        }
        double packedW = Math.max(1, maxX - minX);
        double packedH = Math.max(1, maxY - minY);
        double scale = Math.min(1.0, Math.min(usableW / packedW, usableH / packedH));
        double scaledW = packedW * scale;
        double scaledH = packedH * scale;
        double offsetX = margin + (usableW - scaledW) / 2.0;
        double offsetY = margin + (usableH - scaledH) / 2.0;

        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            Layer layer = targets.get(it.index);
            int newW = Math.max(1, (int) Math.round(it.w * scale));
            int newH = Math.max(1, (int) Math.round(it.h * scale));
            int newX = (int) Math.round(offsetX + (x[i] - minX) * scale);
            int newY = (int) Math.round(offsetY + (y[i] - minY) * scale);
            layer.setBounds(new Rectangle(newX, newY, newW, newH));
        }
    } // --- Fin del metodo layout ---


    /**
     * Estantes: ordena por altura descendente y coloca en filas.
     */
    private void packShelf(List<Item> items, double usableW, double spacing, double[] x, double[] y) {
        List<Item> orden = new ArrayList<>(items);
        orden.sort(Comparator.comparingDouble((Item it) -> -it.h).thenComparingInt(it -> it.index));
        double curX = 0;
        double curY = 0;
        double rowH = 0;
        for (Item it : orden) {
            if (curX > 0 && curX + it.w > usableW) {
                curX = 0;
                curY += rowH + spacing;
                rowH = 0;
            }
            x[it.index] = curX;
            y[it.index] = curY;
            rowH = Math.max(rowH, it.h);
            curX += it.w + spacing;
        }
    } // --- Fin del metodo packShelf ---


    /**
     * Skyline: ordena por área descendente y coloca cada pieza en la posición
     * de menor altura de la silueta, actualizando el perfil resultante.
     */
    private void packSkyline(List<Item> items, double usableW, double spacing, double[] x, double[] y) {
        List<Item> orden = new ArrayList<>(items);
        orden.sort(Comparator.comparingDouble((Item it) -> -(it.w * it.h)).thenComparingInt(it -> it.index));
        int W = Math.max(1, (int) Math.round(usableW));
        int[] sky = new int[W];
        for (Item it : orden) {
            int w = Math.min((int) Math.round(it.w), W);
            int h = (int) Math.round(it.h);
            int bestX = 0;
            int bestY = Integer.MAX_VALUE;
            for (int px = 0; px <= W - w; px++) {
                int maxH = 0;
                for (int k = 0; k < w; k++) {
                    maxH = Math.max(maxH, sky[px + k]);
                }
                if (maxH < bestY) {
                    bestY = maxH;
                    bestX = px;
                }
            }
            x[it.index] = bestX;
            y[it.index] = bestY;
            for (int k = 0; k < w; k++) {
                sky[bestX + k] = bestY + h;
            }
        }
    } // --- Fin del metodo packSkyline ---


    /**
     * Guillotina: divide el espacio libre en rectángulos y coloca cada pieza en
     * el rectángulo de menor área que la contiene, partiéndolo en dos.
     */
    private void packGuillotine(List<Item> items, double usableW, double usableH,
            double spacing, double[] x, double[] y) {
        List<Item> orden = new ArrayList<>(items);
        orden.sort(Comparator.comparingDouble((Item it) -> -(it.w * it.h)).thenComparingInt(it -> it.index));
        List<FreeRect> free = new ArrayList<>();
        free.add(new FreeRect(0, 0, usableW, usableH));
        double maxY = 0;
        for (Item it : orden) {
            FreeRect best = null;
            for (FreeRect fr : free) {
                if (fr.w >= it.w && fr.h >= it.h
                        && (best == null || fr.w * fr.h < best.w * best.h)) {
                    best = fr;
                }
            }
            if (best != null) {
                free.remove(best);
                double rightW = best.w - it.w;
                double topH = best.h - it.h;
                if (rightW > 0) {
                    free.add(new FreeRect(best.x + it.w, best.y, rightW, best.h));
                }
                if (topH > 0) {
                    free.add(new FreeRect(best.x, best.y + it.h, it.w, topH));
                }
                x[it.index] = best.x;
                y[it.index] = best.y;
                maxY = Math.max(maxY, best.y + it.h);
            } else {
                x[it.index] = 0;
                y[it.index] = maxY + spacing;
                maxY += it.h + spacing;
            }
        }
    } // --- Fin del metodo packGuillotine ---

} // --- Fin de la clase PackLayout ---
