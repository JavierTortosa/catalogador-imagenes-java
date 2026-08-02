package controlador.tools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.event.MouseEvent;

import controlador.commands.AppActionCommands;

/**
 * Herramienta de selección por marco.
 * <p>
 * Permite crear un área de selección rectangular arrastrando el ratón sobre el
 * canvas. Al soltar, la selección se fija en el {@code SelectionModel}.
 */
public class MarqueeSelectionTool extends Tool {

    private Point startPoint;
    private Rectangle currentRect;
    private boolean dragging;

    private static final Color FILL_COLOR = new Color(0, 120, 215, 40);
    private static final Color BORDER_COLOR = new Color(0, 120, 215);
    private static final float DASH[] = { 4f, 4f };

    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_MARCO;
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

        if (ctx.canvasModel() != null) {
            int cw = ctx.canvasModel().getWidth();
            int ch = ctx.canvasModel().getHeight();
            x = Math.max(0, Math.min(x, cw - 1));
            y = Math.max(0, Math.min(y, ch - 1));
            if (x + w > cw) w = cw - x;
            if (y + h > ch) h = ch - y;
        }

        currentRect = new Rectangle(x, y, w, h);
    } // --- Fin del metodo mouseDragged ---

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!dragging) return;

        if (currentRect != null && currentRect.width > 2 && currentRect.height > 2) {
            if (ctx.selectionModel() != null) {
                ctx.selectionModel().setBounds(currentRect);
                ctx.selectionModel().setFeather(ctx.componentBar().getFeatherAmount());
            }
        } else {
            if (ctx.selectionModel() != null) {
                ctx.selectionModel().clear();
            }
        }

        dragging = false;
        startPoint = null;
        currentRect = null;
    } // --- Fin del metodo mouseReleased ---


    @Override
    public boolean cancel() {
        if (!dragging) return false;
        dragging = false;
        startPoint = null;
        currentRect = null;
        ctx.canvasPanel().repaint();
        return true;
    } // --- Fin del metodo cancel ---

    @Override
    public void paintOverlay(Graphics2D g2) {
        if (!dragging || currentRect == null) return;

        // Fill
        g2.setColor(FILL_COLOR);
        g2.fill(currentRect);

        // Dashed border
        Stroke original = g2.getStroke();
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 0, DASH, 0));
        g2.setColor(BORDER_COLOR);
        g2.draw(currentRect);
        g2.setStroke(original);
    } // --- Fin del metodo paintOverlay ---

} // --- Fin de la clase MarqueeSelectionTool ---
