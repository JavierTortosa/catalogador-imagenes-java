package controlador.tools;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Deque;

import controlador.commands.AppActionCommands;
import modelo.editor.ImageLayer;
import modelo.editor.Layer;

/**
 * Herramienta bote de pintura.
 * <p>
 * Rellena píxeles de color similar con el color seleccionado, usando BFS con
 * tolerancia sobre la imagen de la capa activa. Lee color, tolerancia y modo
 * contiguo desde la barra de opciones (Parte B).
 * <p>
 * Resolución de capa estilo Photoshop: si la auto-selección está activada o no
 * hay capa activa, se busca la capa visible más frontal bajo el cursor y se
 * activa; si no hay ninguna capa en ese punto, la operación se cancela. Las
 * coordenadas del ratón se mapean a los píxeles de la imagen considerando la
 * escala entre el rectángulo (bounds) y el tamaño real de la imagen.
 */
public class PaintBucketTool extends Tool {

    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_BOTE_PINTURA;
    } // --- Fin del metodo getCommandKey ---

    @Override
    public boolean modifiesContent() {
        return true;
    } // --- Fin del metodo modifiesContent ---

    @Override
    public void mousePressed(MouseEvent e) {
        ImageLayer layer = findTargetLayer(e.getPoint());
        if (layer == null || layer.isLocked()) return;
        BufferedImage img = layer.getImage();
        Rectangle b = layer.getBounds();
        if (img == null || b == null) return;

        int mx = (int) ((e.getX() - b.x) * img.getWidth() / b.width);
        int my = (int) ((e.getY() - b.y) * img.getHeight() / b.height);
        if (mx < 0 || my < 0 || mx >= img.getWidth() || my >= img.getHeight()) return;

        int tolerance = ctx.componentBar().getPaintBucketTolerance();
        boolean contiguous = ctx.componentBar().isPaintBucketContiguous();
        int fillRgb = ctx.componentBar().getPaintBucketColor().getRGB();
        int targetRgb = img.getRGB(mx, my);

        // Mismo color (incluyendo alfa) → nada que hacer
        if (fillRgb == targetRgb) return;

        int w = img.getWidth();
        int h = img.getHeight();

        // Si hay una selección de píxeles activa, el relleno se limita a ella
        BitSet limitMask = selectionLimitInImage(b, w, h);
        if (limitMask != null && !limitMask.get(my * w + mx)) return;

        if (contiguous) {
            floodFill(img, w, h, mx, my, targetRgb, tolerance, fillRgb, limitMask);
        } else {
            fillAllMatching(img, w, h, targetRgb, tolerance, fillRgb, limitMask);
        }

        ctx.canvasPanel().repaint();
    } // --- Fin del metodo mousePressed ---

    /**
     * Convierte la {@code SelectionModel} (coordenadas de canvas) a una
     * <strong>máscara de píxeles</strong> de la capa, considerando la escala
     * entre el bounds y el tamaño real de la imagen. Cada bit a 1 indica un
     * píxel de imagen dentro de la selección (forma irregular incluida).
     *
     * @param b bounds de la capa en el canvas
     * @param w ancho de la imagen
     * @param h alto de la imagen
     * @return máscara de imagen que limita la operación, o null si no hay
     *         selección activa o la selección no cruza la capa
     */
    private BitSet selectionLimitInImage(Rectangle b, int w, int h) {
        if (ctx.selectionModel() == null || !ctx.selectionModel().isActive()) return null;
        Rectangle sel = ctx.selectionModel().getBounds();

        int loX = Math.max(0, (int) ((long) (sel.x - b.x) * w / b.width));
        int loY = Math.max(0, (int) ((long) (sel.y - b.y) * h / b.height));
        int hiX = Math.min(w - 1, (int) ((long) (sel.x + sel.width - b.x) * w / b.width));
        int hiY = Math.min(h - 1, (int) ((long) (sel.y + sel.height - b.y) * h / b.height));

        if (hiX < loX || hiY < loY) return null;

        BitSet mask = new BitSet(w * h);
        for (int y = loY; y <= hiY; y++) {
            for (int x = loX; x <= hiX; x++) {
                int cx = b.x + (int) ((long) x * b.width / w);
                int cy = b.y + (int) ((long) y * b.height / h);
                if (ctx.selectionModel().contains(cx, cy)) {
                    mask.set(y * w + x);
                }
            }
        }
        return mask;
    } // --- Fin del metodo selectionLimitInImage ---

    /**
     * Capa sobre la que actúa el bote de pintura. Si la auto-selección está
     * activada o no hay capa activa, busca la capa visible más frontal bajo el
     * cursor y la activa. Si no hay ninguna capa en ese punto, devuelve null.
     *
     * @param p coordenadas de canvas del punto pulsado
     * @return la capa de imagen a usar, o null si no hay ninguna
     */
    private ImageLayer findTargetLayer(Point p) {
        boolean autoSelect = ctx.componentBar() != null && ctx.componentBar().isAutoSelect();
        boolean sinActiva = getActiveLayer() == null;
        if (autoSelect || sinActiva) {
            if (ctx.layerPicker() != null) {
                Layer hit = ctx.layerPicker().findLayerAt(p);
                if (hit instanceof ImageLayer il && il.getImage() != null) {
                    ctx.layerPicker().activateLayer(il);
                    return il;
                }
            }
            return null;
        }
        return getActiveLayer();
    } // --- Fin del metodo findTargetLayer ---

    private ImageLayer getActiveLayer() {
        if (ctx.layerModel() == null) return null;
        return ctx.layerModel().getActiveLayer() instanceof ImageLayer il ? il : null;
    } // --- Fin del metodo getActiveLayer ---

    private void floodFill(BufferedImage img, int w, int h,
            int sx, int sy, int targetRgb, int tolerance, int fillRgb,
            BitSet limitMask) {
        BitSet visited = new BitSet(w * h);
        Deque<Integer> queue = new ArrayDeque<>();
        int startIdx = sy * w + sx;
        queue.add(startIdx);
        visited.set(startIdx);

        while (!queue.isEmpty()) {
            int idx = queue.poll();
            int px = idx % w;
            int py = idx / w;
            img.setRGB(px, py, fillRgb);

            checkNeighbor(img, w, h, px - 1, py, targetRgb, tolerance, visited, queue, limitMask);
            checkNeighbor(img, w, h, px + 1, py, targetRgb, tolerance, visited, queue, limitMask);
            checkNeighbor(img, w, h, px, py - 1, targetRgb, tolerance, visited, queue, limitMask);
            checkNeighbor(img, w, h, px, py + 1, targetRgb, tolerance, visited, queue, limitMask);
        }
    } // --- Fin del metodo floodFill ---

    private void fillAllMatching(BufferedImage img, int w, int h,
            int targetRgb, int tolerance, int fillRgb, BitSet limitMask) {
        int x0 = 0, y0 = 0, x1 = w - 1, y1 = h - 1;
        if (limitMask != null) {
            int first = limitMask.nextSetBit(0);
            int last = limitMask.previousSetBit(w * h - 1);
            if (first < 0) return;
            x0 = first % w;
            y0 = first / w;
            x1 = last % w;
            y1 = last / w;
        }
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (limitMask != null && !limitMask.get(y * w + x)) continue;
                if (colorDistance(img.getRGB(x, y), targetRgb) <= tolerance) {
                    img.setRGB(x, y, fillRgb);
                }
            }
        }
    } // --- Fin del metodo fillAllMatching ---

    private void checkNeighbor(BufferedImage img, int w, int h,
            int x, int y, int targetRgb, int tolerance,
            BitSet visited, Deque<Integer> queue, BitSet limitMask) {
        if (x < 0 || x >= w || y < 0 || y >= h) return;
        int idx = y * w + x;
        if (limitMask != null && !limitMask.get(idx)) return;
        if (visited.get(idx)) return;
        visited.set(idx);

        if (colorDistance(img.getRGB(x, y), targetRgb) <= tolerance) {
            queue.add(idx);
        }
    } // --- Fin del metodo checkNeighbor ---

    /**
     * Distancia de color normalizada a 0-255. Incluye el canal alfa para no
     * tratar la transparencia como color negro/blanco puro. Con los cuatro
     * canales el máximo es 1020, por lo que se divide entre 4.
     *
     * @param a píxel en formato ARGB
     * @param b píxel en formato ARGB
     * @return distancia de color entre 0 y 255
     */
    private static int colorDistance(int a, int b) {
        int dr = Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF));
        int dg = Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF));
        int db = Math.abs((a & 0xFF) - (b & 0xFF));
        int da = Math.abs(((a >> 24) & 0xFF) - ((b >> 24) & 0xFF));
        return (dr + dg + db + da) / 4;
    } // --- Fin del metodo colorDistance ---

} // --- Fin de la clase PaintBucketTool ---
