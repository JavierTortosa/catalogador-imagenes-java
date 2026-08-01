package controlador.tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;

import controlador.commands.AppActionCommands;
import modelo.editor.ImageLayer;

/**
 * Herramienta de selección por capa.
 * <p>
 * Crea una selección rectangular limitada a los bounds de la capa activa. El
 * arrastre fuera de la capa no genera selección.
 */
public class LayerSelectionTool extends Tool {

    private Point startPoint;
    private Rectangle currentRect;
    private boolean dragging;

    private static final Color FILL_COLOR = new Color(0, 180, 80, 40);
    private static final Color BORDER_COLOR = new Color(0, 180, 80);
    private static final float DASH[] = { 4f, 4f };

    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_CAPA;
    } // --- Fin del metodo getCommandKey ---

    private ImageLayer getActiveLayer() {
        if (ctx.layerModel() == null) return null;
        return ctx.layerModel().getActiveLayer() instanceof ImageLayer il ? il : null;
    } // --- Fin del metodo getActiveLayer ---

    @Override
    public void mousePressed(MouseEvent e) {
        ImageLayer layer = getActiveLayer();
        if (layer == null || layer.getBounds() == null) return;
        if (!layer.getBounds().contains(e.getPoint())) return;

        startPoint = e.getPoint();
        currentRect = new Rectangle(startPoint.x, startPoint.y, 0, 0);
        dragging = true;
    } // --- Fin del metodo mousePressed ---

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!dragging || startPoint == null) return;
        ImageLayer layer = getActiveLayer();
        if (layer == null || layer.getBounds() == null) return;

        Rectangle lb = layer.getBounds();
        int x = Math.max(lb.x, Math.min(startPoint.x, e.getX()));
        int y = Math.max(lb.y, Math.min(startPoint.y, e.getY()));
        int ex = Math.min(lb.x + lb.width, Math.max(startPoint.x, e.getX()));
        int ey = Math.min(lb.y + lb.height, Math.max(startPoint.y, e.getY()));
        int w = ex - x;
        int h = ey - y;

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
    public void paintOverlay(Graphics2D g2) {
        if (!dragging || currentRect == null) return;

        g2.setColor(FILL_COLOR);
        g2.fill(currentRect);

        Stroke original = g2.getStroke();
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 0, DASH, 0));
        g2.setColor(BORDER_COLOR);
        g2.draw(currentRect);
        g2.setStroke(original);
    } // --- Fin del metodo paintOverlay ---

} // --- Fin de la clase LayerSelectionTool ---
