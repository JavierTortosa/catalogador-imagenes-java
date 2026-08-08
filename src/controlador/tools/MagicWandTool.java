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
 * Herramienta varita mágica.
 * <p>
 * Selecciona píxeles de color similar partiendo del punto pulsado, usando BFS
 * con tolerancia. El resultado se representa como un rectángulo delimitador
 * sobre el {@code SelectionModel}. Lee tolerancia y modo contiguo desde la
 * barra de opciones (Parte B).
 * <p>
 * Operación estilo Photoshop: la varita actúa sobre la capa bajo el cursor
 * (activándola si la auto-selección está marcada); si no hay capa bajo el
 * cursor usa la capa activa. Las coordenadas del ratón se mapean a los píxeles
 * de la imagen considerando la escala entre el rectángulo (bounds) y el tamaño
 * real de la imagen.
 */
public class MagicWandTool extends Tool {

    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_VARITA;
    } // --- Fin del metodo getCommandKey ---

    @Override
    public void mousePressed(MouseEvent e) {
        ImageLayer layer = findTargetLayer(e.getPoint());
        if (layer == null || layer.isLocked()) return;
        BufferedImage img = layer.getImage();
        Rectangle b = layer.getBounds();
        if (img == null || b == null) return;

        int imgW = img.getWidth();
        int imgH = img.getHeight();
        int mx = (int) ((e.getX() - b.x) * imgW / b.width);
        int my = (int) ((e.getY() - b.y) * imgH / b.height);
        if (mx < 0 || my < 0 || mx >= imgW || my >= imgH) return;

        int tolerance = ctx.componentBar().getWandTolerance();
        boolean contiguous = ctx.componentBar().isWandContiguous();
        int targetRgb = img.getRGB(mx, my) & 0x00FFFFFF;

        int minX = mx, maxX = mx, minY = my, maxY = my;

        if (contiguous) {
            BitSet visited = new BitSet(imgW * imgH);
            Deque<Integer> queue = new ArrayDeque<>();
            int startIdx = my * imgW + mx;
            queue.add(startIdx);
            visited.set(startIdx);

            while (!queue.isEmpty()) {
                int idx = queue.poll();
                int px = idx % imgW;
                int py = idx / imgW;

                if (px < minX) minX = px;
                if (px > maxX) maxX = px;
                if (py < minY) minY = py;
                if (py > maxY) maxY = py;

                checkNeighbor(img, imgW, imgH, px - 1, py, targetRgb, tolerance, visited, queue);
                checkNeighbor(img, imgW, imgH, px + 1, py, targetRgb, tolerance, visited, queue);
                checkNeighbor(img, imgW, imgH, px, py - 1, targetRgb, tolerance, visited, queue);
                checkNeighbor(img, imgW, imgH, px, py + 1, targetRgb, tolerance, visited, queue);
            }
        } else {
            for (int y = 0; y < imgH; y++) {
                for (int x = 0; x < imgW; x++) {
                    int pixel = img.getRGB(x, y) & 0x00FFFFFF;
                    if (colorDistance(pixel, targetRgb) <= tolerance) {
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }
        }

        // Convertir el rectángulo de imagen a coordenadas de canvas (bounds)
        int fx = b.x + (int) ((long) minX * b.width / imgW);
        int fy = b.y + (int) ((long) minY * b.height / imgH);
        int fw = Math.max(1, (int) ((long) (maxX - minX + 1) * b.width / imgW));
        int fh = Math.max(1, (int) ((long) (maxY - minY + 1) * b.height / imgH));

        if (ctx.selectionModel() != null) {
            ctx.selectionModel().setBounds(new Rectangle(fx, fy, fw, fh));
            ctx.selectionModel().setFeather(ctx.componentBar().getFeatherAmount());
        }
        ctx.canvasPanel().repaint();
    } // --- Fin del metodo mousePressed ---

    /**
     * Capa sobre la que actúa la varita: la capa más frontal bajo el cursor
     * (se activa si la auto-selección está marcada); si no hay ninguna, la capa
     * activa.
     *
     * @param p coordenadas de canvas del punto pulsado
     * @return la capa de imagen a usar, o null si no hay ninguna
     */
    private ImageLayer findTargetLayer(Point p) {
        if (ctx.layerPicker() != null) {
            Layer hit = ctx.layerPicker().findLayerAt(p);
            if (hit instanceof ImageLayer il && il.getImage() != null) {
                if (ctx.componentBar().isAutoSelect()) {
                    ctx.layerModel().setActiveLayer(il);
                }
                return il;
            }
        }
        return getActiveLayer();
    } // --- Fin del metodo findTargetLayer ---

    private ImageLayer getActiveLayer() {
        if (ctx.layerModel() == null) return null;
        return ctx.layerModel().getActiveLayer() instanceof ImageLayer il ? il : null;
    } // --- Fin del metodo getActiveLayer ---

    private void checkNeighbor(BufferedImage img, int w, int h,
            int x, int y, int targetRgb, int tolerance,
            BitSet visited, Deque<Integer> queue) {
        if (x < 0 || x >= w || y < 0 || y >= h) return;
        int idx = y * w + x;
        if (visited.get(idx)) return;
        visited.set(idx);

        int rgb = img.getRGB(x, y);
        if (isTransparent(rgb)) return;
        if (colorDistance(rgb & 0x00FFFFFF, targetRgb) <= tolerance) {
            queue.add(idx);
        }
    } // --- Fin del metodo checkNeighbor ---

    /**
     * @return {@code true} si el píxel es totalmente transparente
     */
    private static boolean isTransparent(int rgb) {
        return ((rgb >>> 24) & 0xFF) == 0;
    } // --- Fin del metodo isTransparent ---

    /**
     * Distancia de color normalizada a 0-255. Con tres canales (RGB) el máximo
     * es 765, por lo que se divide entre 3 para que la tolerancia de la barra
     * responda en la escala 0-255.
     *
     * @param a píxel en formato RGB
     * @param b píxel en formato RGB
     * @return distancia de color entre 0 y 255
     */
    private static int colorDistance(int a, int b) {
        int dr = Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF));
        int dg = Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF));
        int db = Math.abs((a & 0xFF) - (b & 0xFF));
        return (dr + dg + db) / 3;
    } // --- Fin del metodo colorDistance ---

} // --- Fin de la clase MagicWandTool ---
