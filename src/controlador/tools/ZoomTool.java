package controlador.tools;

import java.awt.Cursor;
import java.awt.event.MouseEvent;

import controlador.commands.AppActionCommands;

/**
 * Herramienta zoom.
 * <p>
 * El zoom se maneja principalmente con la rueda del ratón (integrado en
 * {@code CanvasPanel}). Esta herramienta permite cambiar el modo de zoom
 * (al cursor / al centro) y proporciona un clic para zoom adicional.
 */
public class ZoomTool extends Tool {

    private boolean zoomToCursor;

    @Override
    public String getCommandKey() {
        return controlador.commands.AppActionCommands.CMD_ADVANCED_EDITOR_EDICION;
    } // --- Fin del metodo getCommandKey ---

    @Override
    public void mousePressed(MouseEvent e) {
        // Left click zooms in, right click zooms out
        if (e.getButton() == MouseEvent.BUTTON1) {
            zoomAroundPoint(e.getX(), e.getY(), 1.3);
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            zoomAroundPoint(e.getX(), e.getY(), 0.77);
        }
    } // --- Fin del metodo mousePressed ---

    private void zoomAroundPoint(int mx, int my, double factor) {
        var cp = ctx.canvasPanel();
        double oldZoom = cp.getZoom();
        double newZoom = Math.max(0.1, Math.min(10.0, oldZoom * factor));
        double ox = cp.getOffsetX();
        double oy = cp.getOffsetY();
        double newOx = mx - (mx - ox) * (newZoom / oldZoom);
        double newOy = my - (my - oy) * (newZoom / oldZoom);
        cp.setZoom(newZoom);
        cp.setPan(newOx, newOy);
    } // --- Fin del metodo zoomAroundPoint ---

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    } // --- Fin del metodo getCursor ---

} // --- Fin de la clase ZoomTool ---
