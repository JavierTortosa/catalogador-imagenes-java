package controlador.tools;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.SwingUtilities;

import controlador.commands.AppActionCommands;
import modelo.editor.Layer;
import modelo.editor.LayerModel;
import modelo.gizmo.TransformGizmo;
import vista.panels.render.EditorComponentBar;

/**
 * Herramienta de navegacion y manipulacion universal.
 * <ul>
 *   <li>1 clic sobre capa -> la activa
 *   <li>1 clic fuera -> deselecciona
 *   <li>arrastre -> mueve la capa seleccionada
 *   <li>tiradores opcionales con {@link TransformGizmo}
 * </ul>
 */
public class EditTool extends Tool {

    private boolean dragging;
    private boolean gizmoDragging;
    private TransformGizmo.Handle activeHandle;
    private Point dragStart;
    private Rectangle dragStartBounds;
    private TransformGizmo.Constraints gizmoConstraints;

    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_EDICION;
    } // --- Fin del metodo getCommandKey ---


    private EditorComponentBar bar() {
        return ctx.componentBar();
    }


    private LayerModel model() {
        return ctx.layerModel();
    }


    private boolean isAutoSelect() {
        return bar().isAutoSelect();
    }


    private boolean isShowGizmo() {
        return bar().isShowGizmo();
    }


    private boolean isKeepAspect() {
        return bar().isKeepAspect();
    }


    /**
     * Busca la capa mas frontal (ultimo indice) cuyos bounds contengan el punto dado.
     */
    private Layer findLayerAt(Point p) {
        List<Layer> layers = model().getLayers();
        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer layer = layers.get(i);
            if (!layer.isVisible() || layer.isLocked()) continue;
            if (layer.getBounds() != null && layer.getBounds().contains(p)) {
                return layer;
            }
        }
        return null;
    } // --- Fin del metodo findLayerAt ---


    private void syncSpinnersFromLayer(Layer layer) {
        if (layer == null) return;
        Rectangle b = layer.getBounds();
        if (b == null) return;
        bar().updateDimensionSpinners(b.x, b.y, b.width, b.height);
    } // --- Fin del metodo syncSpinnersFromLayer ---


    private void syncLayerFromSpinners() {
        Layer layer = model().getActiveLayer();
        if (layer == null) return;
        try {
            int x = ((Number) bar().getSpinnerX().getValue()).intValue();
            int y = ((Number) bar().getSpinnerY().getValue()).intValue();
            int w = ((Number) bar().getSpinnerW().getValue()).intValue();
            int h = ((Number) bar().getSpinnerH().getValue()).intValue();
            layer.setBounds(new Rectangle(x, y, w, h));
            ctx.canvasPanel().repaint();
        } catch (Exception ignored) {
        }
    } // --- Fin del metodo syncLayerFromSpinners ---


    private void enableSpinners(boolean enabled) {
        bar().getSpinnerX().setEnabled(enabled);
        bar().getSpinnerY().setEnabled(enabled);
        bar().getSpinnerW().setEnabled(enabled);
        bar().getSpinnerH().setEnabled(enabled);
    } // --- Fin del metodo enableSpinners ---


    // ======================== ACTIVACION / DESACTIVACION ========================


    @Override
    public void onActivate() {
        Layer active = model().getActiveLayer();
        if (active != null) {
            enableSpinners(true);
            syncSpinnersFromLayer(active);
        } else {
            enableSpinners(false);
            bar().updateDimensionSpinners(0, 0, 0, 0);
        }
        bar().setEditToolSpinnerListener(vals -> {
            if (vals.length == 4) {
                syncLayerFromSpinners();
            }
        });
    } // --- Fin del metodo onActivate ---


    @Override
    public void onDeactivate() {
        enableSpinners(false);
    } // --- Fin del metodo onDeactivate ---


    // ======================== EVENTOS DE RATON ========================


    @Override
    public void mousePressed(MouseEvent e) {
        Point p = e.getPoint();
        Layer active = model().getActiveLayer();
        gizmoDragging = false;
        dragging = false;
        activeHandle = null;

        // 1. Comprobar tiradores del gizmo
        if (isShowGizmo() && active != null && active.getBounds() != null) {
            activeHandle = ctx.gizmo().hitTest(p, active.getBounds());
            if (activeHandle != TransformGizmo.Handle.NONE && activeHandle != TransformGizmo.Handle.ROTATE) {
                gizmoDragging = true;
                gizmoConstraints = new TransformGizmo.Constraints(isKeepAspect(), 0, 10);
                ctx.gizmo().startDrag(activeHandle, active.getBounds(), gizmoConstraints);
                return;
            }
        }

        // 2. Auto-select: buscar capa bajo el cursor
        Layer hit = isAutoSelect() ? findLayerAt(p) : null;
        if (hit != null) {
            model().setActiveLayer(hit);
            active = hit;
            syncSpinnersFromLayer(active);
            ctx.canvasPanel().repaint();
        }

        // 3. Si hay capa activa, iniciar arrastre para mover
        active = model().getActiveLayer();
        if (active != null && active.getBounds() != null && active.getBounds().contains(p)) {
            dragging = true;
            dragStart = p;
            dragStartBounds = new Rectangle(active.getBounds());
        } else if (hit == null) {
            // clic fuera de toda capa -> deseleccionar
            model().setActiveLayer(-1);
            bar().updateDimensionSpinners(0, 0, 0, 0);
            ctx.canvasPanel().repaint();
        }
    } // --- Fin del metodo mousePressed ---


    @Override
    public void mouseDragged(MouseEvent e) {
        Point p = e.getPoint();
        Layer active = model().getActiveLayer();

        if (gizmoDragging && active != null && dragStart != null) {
            int dx = p.x - dragStart.x;
            int dy = p.y - dragStart.y;
            Rectangle newBounds = ctx.gizmo().drag(dx, dy);
            if (newBounds != null) {
                active.setBounds(newBounds);
                syncSpinnersFromLayer(active);
                ctx.canvasPanel().repaint();
            }
            return;
        }

        if (dragging && active != null && dragStart != null) {
            int dx = p.x - dragStart.x;
            int dy = p.y - dragStart.y;
            Rectangle b = dragStartBounds;
            if (b != null) {
                active.setBounds(new Rectangle(b.x + dx, b.y + dy, b.width, b.height));
                syncSpinnersFromLayer(active);
                ctx.canvasPanel().repaint();
            }
        }
    } // --- Fin del metodo mouseDragged ---


    @Override
    public void mouseReleased(MouseEvent e) {
        if (gizmoDragging) {
            ctx.gizmo().endDrag();
            gizmoDragging = false;
        }
        dragging = false;
        activeHandle = null;
        dragStart = null;
        dragStartBounds = null;
        ctx.canvasPanel().repaint();
    } // --- Fin del metodo mouseReleased ---


    // ======================== OVERLAY ========================


    @Override
    public void paintOverlay(Graphics2D g2) {
        if (!isShowGizmo()) return;
        Layer active = model().getActiveLayer();
        if (active == null || active.getBounds() == null) return;
        ctx.gizmo().draw(g2, active.getBounds());
    } // --- Fin del metodo paintOverlay ---


    // ======================== CURSOR ========================


    @Override
    public Cursor getCursor() {
        Layer active = model().getActiveLayer();
        if (active == null) return Cursor.getDefaultCursor();
        Point mp = ctx.canvasPanel().getMousePosition();
        if (mp != null && active.getBounds() != null && active.getBounds().contains(mp)) {
            return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        }
        return Cursor.getDefaultCursor();
    } // --- Fin del metodo getCursor ---

} // --- Fin de la clase EditTool ---
