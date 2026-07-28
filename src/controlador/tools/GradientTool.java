package controlador.tools;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.geom.Point2D;

import controlador.commands.AppActionCommands;
import modelo.editor.ImageLayer;

/**
 * Herramienta degradado.
 * <p>
 * Arrastra para definir el vector del degradado. Al soltar, crea una nueva
 * capa con un relleno degradado lineal o radial.
 */
public class GradientTool extends Tool {

    private Point startPoint;
    private Point endPoint;
    private boolean dragging;

    private static final Color COLOR_START = Color.RED;
    private static final Color COLOR_END = Color.BLUE;

    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_DEGRADADO;
    } // --- Fin del metodo getCommandKey ---

    @Override
    public void mousePressed(MouseEvent e) {
        startPoint = e.getPoint();
        endPoint = null;
        dragging = true;
    } // --- Fin del metodo mousePressed ---

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!dragging || startPoint == null) return;
        endPoint = e.getPoint();
    } // --- Fin del metodo mouseDragged ---

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!dragging || startPoint == null || endPoint == null) return;
        dragging = false;

        int cw = ctx.canvasModel() != null ? ctx.canvasModel().getWidth() : 1920;
        int ch = ctx.canvasModel() != null ? ctx.canvasModel().getHeight() : 1080;

        BufferedImage img = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float x1 = startPoint.x;
        float y1 = startPoint.y;
        float x2 = endPoint.x;
        float y2 = endPoint.y;

        // Use linear gradient by default
        g2.setPaint(new GradientPaint(x1, y1, COLOR_START, x2, y2, COLOR_END));
        g2.fillRect(0, 0, cw, ch);
        g2.dispose();

        Rectangle bounds = new Rectangle(0, 0, cw, ch);
        String name = "Degradado " + (ctx.layerModel().size() + 1);
        ImageLayer layer = new ImageLayer(name, img, bounds);
        ctx.layerModel().addLayer(layer);

        startPoint = null;
        endPoint = null;
    } // --- Fin del metodo mouseReleased ---

    @Override
    public void paintOverlay(Graphics2D g2) {
        if (!dragging || startPoint == null || endPoint == null) return;

        g2.setColor(new Color(100, 100, 100, 80));
        g2.setStroke(new java.awt.BasicStroke(2f));
        g2.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);

        // Draw direction arrow
        int dx = endPoint.x - startPoint.x;
        int dy = endPoint.y - startPoint.y;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len > 4) {
            double nx = dx / len;
            double ny = dy / len;
            int ax = endPoint.x - (int) (nx * 8);
            int ay = endPoint.y - (int) (ny * 8);
            g2.fillOval(ax - 2, ay - 2, 4, 4);
        }
    } // --- Fin del metodo paintOverlay ---

} // --- Fin de la clase GradientTool ---
