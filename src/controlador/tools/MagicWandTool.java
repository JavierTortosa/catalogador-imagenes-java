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

/**
 * Herramienta varita mágica.
 * <p>
 * Selecciona píxeles de color similar partiendo del punto pulsado, usando BFS
 * con tolerancia. El resultado se representa como un rectángulo delimitador
 * sobre el {@code SelectionModel}.
 */
public class MagicWandTool extends Tool {

    private static final int DEFAULT_TOLERANCE = 32;

    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_VARITA;
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
        int targetRgb = img.getRGB(mx, my) & 0x00FFFFFF;

        int w = img.getWidth();
        int h = img.getHeight();
        BitSet visited = new BitSet(w * h);
        Deque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(mx, my));
        visited.set(my * w + mx);

        int minX = mx, maxX = mx, minY = my, maxY = my;

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            int px = p.x;
            int py = p.y;

            if (px < minX) minX = px;
            if (px > maxX) maxX = px;
            if (py < minY) minY = py;
            if (py > maxY) maxY = py;

            checkNeighbor(img, w, h, px - 1, py, targetRgb, tolerance, visited, queue);
            checkNeighbor(img, w, h, px + 1, py, targetRgb, tolerance, visited, queue);
            checkNeighbor(img, w, h, px, py - 1, targetRgb, tolerance, visited, queue);
            checkNeighbor(img, w, h, px, py + 1, targetRgb, tolerance, visited, queue);
        }

        int fx = minX + layer.getBounds().x;
        int fy = minY + layer.getBounds().y;
        Rectangle sel = new Rectangle(fx, fy, maxX - minX + 1, maxY - minY + 1);

        if (ctx.selectionModel() != null) {
            ctx.selectionModel().setBounds(sel);
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

} // --- Fin de la clase MagicWandTool ---
