package controlador.tools;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Deque;

import controlador.commands.AppActionCommands;
import modelo.editor.ImageLayer;

/**
 * Herramienta bote de pintura.
 * <p>
 * Rellena píxeles de color similar con un color sólido, usando BFS con
 * tolerancia sobre la imagen de la capa activa. Lee color, tolerancia y modo
 * contiguo desde la barra de opciones (Parte B).
 */
public class PaintBucketTool extends Tool {

    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_BOTE_PINTURA;
    } // --- Fin del metodo getCommandKey ---

    @Override
    public void mousePressed(MouseEvent e) {
        ImageLayer layer = getActiveLayer();
        if (layer == null || layer.isLocked()) return;
        BufferedImage img = layer.getImage();
        if (img == null) return;

        int mx = e.getX() - layer.getBounds().x;
        int my = e.getY() - layer.getBounds().y;
        if (mx < 0 || my < 0 || mx >= img.getWidth() || my >= img.getHeight()) return;

        int tolerance = ctx.componentBar().getPaintBucketTolerance();
        boolean contiguous = ctx.componentBar().isPaintBucketContiguous();
        Color fillColor = ctx.componentBar().getPaintBucketColor();
        int fillRgb = (fillColor.getRGB() & 0x00FFFFFF) | (0xFF000000);
        int targetRgb = img.getRGB(mx, my) & 0x00FFFFFF;

        // Same color → nothing to do
        if ((fillRgb & 0x00FFFFFF) == targetRgb) return;

        int w = img.getWidth();
        int h = img.getHeight();

        if (contiguous) {
            floodFill(img, w, h, mx, my, targetRgb, tolerance, fillRgb);
        } else {
            fillAllMatching(img, w, h, targetRgb, tolerance, fillRgb);
        }

        // Respetar el checkbox de auto-selección de capa de la barra
        if (ctx.componentBar().isAutoSelect()) {
            ctx.layerModel().setActiveLayer(layer);
            ctx.canvasPanel().repaint();
        }
    } // --- Fin del metodo mousePressed ---

    private ImageLayer getActiveLayer() {
        if (ctx.layerModel() == null) return null;
        return ctx.layerModel().getActiveLayer() instanceof ImageLayer il ? il : null;
    } // --- Fin del metodo getActiveLayer ---

    private void floodFill(BufferedImage img, int w, int h,
            int sx, int sy, int targetRgb, int tolerance, int fillRgb) {
        BitSet visited = new BitSet(w * h);
        Deque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(sx, sy));
        visited.set(sy * w + sx);

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            img.setRGB(p.x, p.y, fillRgb);

            checkNeighbor(img, w, h, p.x - 1, p.y, targetRgb, tolerance, visited, queue);
            checkNeighbor(img, w, h, p.x + 1, p.y, targetRgb, tolerance, visited, queue);
            checkNeighbor(img, w, h, p.x, p.y - 1, targetRgb, tolerance, visited, queue);
            checkNeighbor(img, w, h, p.x, p.y + 1, targetRgb, tolerance, visited, queue);
        }
    } // --- Fin del metodo floodFill ---

    private void fillAllMatching(BufferedImage img, int w, int h,
            int targetRgb, int tolerance, int fillRgb) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = img.getRGB(x, y) & 0x00FFFFFF;
                if (colorDistance(pixel, targetRgb) <= tolerance) {
                    img.setRGB(x, y, fillRgb);
                }
            }
        }
    } // --- Fin del metodo fillAllMatching ---

    private void checkNeighbor(BufferedImage img, int w, int h,
            int x, int y, int targetRgb, int tolerance,
            BitSet visited, Deque<Point> queue) {
        if (x < 0 || x >= w || y < 0 || y >= h) return;
        int idx = y * w + x;
        if (visited.get(idx)) return;
        visited.set(idx);

        int pixel = img.getRGB(x, y) & 0x00FFFFFF;
        if (colorDistance(pixel, targetRgb) <= tolerance) {
            queue.add(new Point(x, y));
        }
    } // --- Fin del metodo checkNeighbor ---

    private static int colorDistance(int a, int b) {
        int dr = Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF));
        int dg = Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF));
        int db = Math.abs((a & 0xFF) - (b & 0xFF));
        return dr + dg + db;
    } // --- Fin del metodo colorDistance ---

} // --- Fin de la clase PaintBucketTool ---
