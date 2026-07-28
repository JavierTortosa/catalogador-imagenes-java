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
 * tolerancia sobre la imagen de la capa activa.
 */
public class PaintBucketTool extends Tool {

    private static final int DEFAULT_TOLERANCE = 32;
    private static final Color DEFAULT_COLOR = Color.RED;

    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_BOTE_PINTURA;
    } // --- Fin del metodo getCommandKey ---

    @Override
    public void mousePressed(MouseEvent e) {
        ImageLayer layer = getActiveLayer();
        if (layer == null) return;
        BufferedImage img = layer.getImage();
        if (img == null) return;

        int mx = e.getX() - layer.getBounds().x;
        int my = e.getY() - layer.getBounds().y;
        if (mx < 0 || my < 0 || mx >= img.getWidth() || my >= img.getHeight()) return;

        int tolerance = DEFAULT_TOLERANCE;
        Color fillColor = DEFAULT_COLOR;
        int fillRgb = (fillColor.getRGB() & 0x00FFFFFF) | (0xFF000000);
        int targetRgb = img.getRGB(mx, my) & 0x00FFFFFF;

        // Same color → nothing to do
        if ((fillRgb & 0x00FFFFFF) == targetRgb) return;

        int w = img.getWidth();
        int h = img.getHeight();
        BitSet visited = new BitSet(w * h);
        Deque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(mx, my));
        visited.set(my * w + mx);

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            img.setRGB(p.x, p.y, fillRgb);

            checkNeighbor(img, w, h, p.x - 1, p.y, targetRgb, tolerance, visited, queue);
            checkNeighbor(img, w, h, p.x + 1, p.y, targetRgb, tolerance, visited, queue);
            checkNeighbor(img, w, h, p.x, p.y - 1, targetRgb, tolerance, visited, queue);
            checkNeighbor(img, w, h, p.x, p.y + 1, targetRgb, tolerance, visited, queue);
        }
    } // --- Fin del metodo mousePressed ---

    private ImageLayer getActiveLayer() {
        if (ctx.layerModel() == null) return null;
        return ctx.layerModel().getActiveLayer() instanceof ImageLayer il ? il : null;
    } // --- Fin del metodo getActiveLayer ---

    private void checkNeighbor(BufferedImage img, int w, int h,
            int x, int y, int targetRgb, int tolerance,
            BitSet visited, Deque<Point> queue) {
        if (x < 0 || x >= w || y < 0 || y >= h) return;
        int idx = y * w + x;
        if (visited.get(idx)) return;
        visited.set(idx);

        int pixel = img.getRGB(x, y) & 0x00FFFFFF;
        int dr = Math.abs(((pixel >> 16) & 0xFF) - ((targetRgb >> 16) & 0xFF));
        int dg = Math.abs(((pixel >> 8) & 0xFF) - ((targetRgb >> 8) & 0xFF));
        int db = Math.abs((pixel & 0xFF) - (targetRgb & 0xFF));
        if (dr + dg + db <= tolerance) {
            queue.add(new Point(x, y));
        }
    } // --- Fin del metodo checkNeighbor ---

} // --- Fin de la clase PaintBucketTool ---
