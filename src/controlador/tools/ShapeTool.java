package controlador.tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import controlador.commands.AppActionCommands;
import modelo.editor.ImageLayer;

/**
 * Herramienta de formas.
 * <p>
 * Arrastra para dibujar formas geométricas (rectángulo, elipse, línea,
 * triángulo, polígono). Cada forma se crea como una nueva capa.
 */
public class ShapeTool extends Tool {

    private Point startPoint;
    private Rectangle currentRect;
    private boolean dragging;

    private static final Color FILL_COLOR = new Color(200, 200, 200, 180);
    private static final Color STROKE_COLOR = Color.BLACK;
    private static final int STROKE_WIDTH = 2;
    private static final float DASH[] = { 4f, 4f };

    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_FORMAS;
    } // --- Fin del metodo getCommandKey ---

    @Override
    public void mousePressed(MouseEvent e) {
        startPoint = e.getPoint();
        currentRect = new Rectangle(startPoint.x, startPoint.y, 0, 0);
        dragging = true;
    } // --- Fin del metodo mousePressed ---

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!dragging || startPoint == null) return;
        int x = Math.min(startPoint.x, e.getX());
        int y = Math.min(startPoint.y, e.getY());
        int w = Math.abs(e.getX() - startPoint.x);
        int h = Math.abs(e.getY() - startPoint.y);
        currentRect = new Rectangle(x, y, Math.max(w, 4), Math.max(h, 4));
    } // --- Fin del metodo mouseDragged ---

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!dragging || currentRect == null) return;
        dragging = false;

        if (currentRect.width < 4 || currentRect.height < 4) {
            startPoint = null;
            currentRect = null;
            return;
        }

        int iw = currentRect.width;
        int ih = currentRect.height;
        BufferedImage img = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(FILL_COLOR);
        Stroke orig = g2.getStroke();
        g2.setStroke(new BasicStroke(STROKE_WIDTH));

        int cx = iw / 2;
        int cy = ih / 2;
        int r = Math.min(cx, cy);

        // Draw shape (default: rectangle)
        g2.fillRect(0, 0, iw, ih);
        g2.setColor(STROKE_COLOR);
        g2.drawRect(0, 0, iw, ih);

        g2.setStroke(orig);
        g2.dispose();

        Rectangle bounds = new Rectangle(currentRect.x, currentRect.y, iw, ih);
        String name = "Forma " + (ctx.layerModel().size() + 1);
        ImageLayer layer = new ImageLayer(name, img, bounds);
        ctx.layerModel().addLayer(layer);

        startPoint = null;
        currentRect = null;
    } // --- Fin del metodo mouseReleased ---

    @Override
    public void paintOverlay(Graphics2D g2) {
        if (!dragging || currentRect == null) return;

        Stroke original = g2.getStroke();
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 0, DASH, 0));
        g2.setColor(new Color(0, 180, 80));
        g2.draw(currentRect);
        g2.setStroke(original);
    } // --- Fin del metodo paintOverlay ---

} // --- Fin de la clase ShapeTool ---
