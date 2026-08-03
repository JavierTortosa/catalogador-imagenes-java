package controlador.tools;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import controlador.commands.AppActionCommands;
import modelo.editor.Layer;
import modelo.editor.LayerModel;
import modelo.gizmo.TransformGizmo;
import vista.panels.render.EditorComponentBar;

/**
 * Herramienta de seleccion y manipulacion universal (Move Tool).
 * <ul>
 *   <li>1 clic sobre capa -> la activa (si la auto-selecci\u00F3n est\u00E1 activa)
 *   <li>1 clic fuera -> deselecciona
 *   <li>arrastre -> mueve la capa seleccionada (Alt = duplicar y mover la copia)
 *   <li>tiradores opcionales con {@link TransformGizmo}
 *   <li>Ctrl+clic -> activa temporalmente la capa bajo el cursor y restaura al soltar
 *   <li>flechas del teclado -> mueven la capa activa 1 px (Shift = 10 px)
 * </ul>
 * <p>
 * La selecci\u00F3n, el movimiento y el gizmo est\u00E1n delegados en
 * {@link LayerPicker}; el doble clic (atajo) delega en el editor del tipo de
 * capa v\u00EDa {@link controlador.tools.editors.LayerEditorRegistry}.
 */
public class EditTool extends Tool {

    private boolean dragging;
    private boolean gizmoDragging;
    private Point dragStart;
    private LayerPicker.GizmoDrag gizmoDrag;
    private LayerPicker.RotateDrag rotateDrag;
    private LayerPicker.DragMove move;
    private Layer ctrlRestoreLayer;

    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_EDICION;
    } // --- Fin del metodo getCommandKey ---


    private LayerPicker picker() {
        return ctx.layerPicker();
    } // --- Fin del metodo picker ---


    private LayerModel model() {
        return ctx.layerModel();
    } // --- Fin del metodo model ---


    private EditorComponentBar bar() {
        return ctx.componentBar();
    } // --- Fin del metodo bar ---


    private boolean isAutoSelect() {
        return picker().isAutoSelect();
    } // --- Fin del metodo isAutoSelect ---


    private boolean isShowGizmo() {
        return picker().isShowGizmo();
    } // --- Fin del metodo isShowGizmo ---


    private void syncLayerFromSpinners() {
        Layer layer = model().getActiveLayer();
        if (layer == null || layer.isLocked()) return;
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


    private void syncLayerFromAngleSpinner() {
        Layer layer = model().getActiveLayer();
        if (layer == null || layer.isLocked()) return;
        try {
            double angle = ((Number) bar().getSpinnerAngle().getValue()).doubleValue();
            layer.setRotation(angle);
            ctx.canvasPanel().repaint();
        } catch (Exception ignored) {
        }
    } // --- Fin del metodo syncLayerFromAngleSpinner ---


    private void enableSpinners(boolean enabled) {
        bar().getSpinnerX().setEnabled(enabled);
        bar().getSpinnerY().setEnabled(enabled);
        bar().getSpinnerW().setEnabled(enabled);
        bar().getSpinnerH().setEnabled(enabled);
        bar().getSpinnerAngle().setEnabled(enabled);
    } // --- Fin del metodo enableSpinners ---


    // ======================== ACTIVACION / DESACTIVACION ========================


    @Override
    public void onActivate() {
        Layer active = model().getActiveLayer();
        if (active != null) {
            enableSpinners(true);
            bar().updateEditLayerFields(active);
        } else {
            enableSpinners(false);
            bar().updateEditLayerFields(null);
        }
        bar().setEditToolSpinnerListener(vals -> {
            if (vals.length == 4) {
                syncLayerFromSpinners();
            }
        });
        bar().setAngleSpinnerListener(angle -> syncLayerFromAngleSpinner());
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
        gizmoDrag = null;
        rotateDrag = null;
        move = null;

        boolean ctrl = (e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0;
        boolean alt = (e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0;

        // 1. Tiradores del gizmo (MOVE se excluye: lo maneja el paso 5)
        if (isShowGizmo() && active != null && active.getBounds() != null) {
            ctx.gizmo().setRotation(active.getRotation());
            TransformGizmo.Handle h = picker().gizmoHitTest(p, active);
            if (h == TransformGizmo.Handle.ROTATE) {
                rotateDrag = picker().beginRotate(active, p);
                if (rotateDrag != null) {
                    gizmoDragging = true;
                    dragStart = p;
                    return;
                }
            } else if (h != TransformGizmo.Handle.NONE && h != TransformGizmo.Handle.MOVE) {
                gizmoDrag = picker().beginGizmo(h, active);
                gizmoDragging = true;
                dragStart = p;
                return;
            }
        }

        // 2. Doble clic (atajo): editar contenido seg\u00FAn el tipo de capa
        if (e.getClickCount() == 2) {
            Layer hit2 = isAutoSelect() ? picker().findLayerAt(p) : active;
            if (hit2 != null && hit2.getBounds() != null && hit2.getBounds().contains(p)) {
                if (ctx.layerEditorRegistry().handleDoubleClick(hit2, ctx)) {
                    return;
                }
            }
        }

        // 3. Ctrl+clic: activar temporalmente la capa bajo el cursor
        if (ctrl) {
            Layer hitC = picker().findLayerAt(p);
            if (hitC != null) {
                ctrlRestoreLayer = active;
                picker().activateLayer(hitC);
                active = hitC;
            } else {
                ctrlRestoreLayer = null;
            }
        } else {
            ctrlRestoreLayer = null;
            // Auto-select: activar la capa bajo el cursor
            if (isAutoSelect()) {
                Layer hit = picker().findLayerAt(p);
                if (hit != null) {
                    picker().activateLayer(hit);
                    active = hit;
                }
            }
        }

        // 4. Alt+arrastre: duplicar la capa activa y mover la copia
        if (alt && active != null && active.getBounds() != null
                && active.getBounds().contains(p)) {
            int idx = model().getActiveIndex();
            model().duplicateLayer(idx);
            model().setActiveLayer(idx + 1);
            Layer copy = model().getActiveLayer();
            picker().activateLayer(copy);
            dragging = true;
            dragStart = p;
            move = picker().beginMove(p, copy);
            return;
        }

        // 5. Iniciar arrastre para mover o deseleccionar si se pulsa en vac\u00EDo
        active = model().getActiveLayer();
        if (active != null && active.getBounds() != null
                && active.getBounds().contains(p)
                && !active.isLocked()) {
            dragging = true;
            dragStart = p;
            move = picker().beginMove(p, active);
        } else if (isAutoSelect() && !ctrl) {
            picker().clearSelection();
        }
    } // --- Fin del metodo mousePressed ---


    @Override
    public void mouseDragged(MouseEvent e) {
        Layer active = model().getActiveLayer();
        if (active != null && active.isLocked()) return;

        if (gizmoDragging && rotateDrag != null && dragStart != null) {
            boolean shift = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
            rotateDrag.drag(e.getPoint(), shift);
            bar().updateEditLayerFields(active);
            ctx.canvasPanel().repaint();
            return;
        }

        if (gizmoDragging && gizmoDrag != null && dragStart != null) {
            int dx = e.getX() - dragStart.x;
            int dy = e.getY() - dragStart.y;
            Rectangle newBounds = gizmoDrag.drag(dx, dy);
            if (newBounds != null && active != null) {
                active.setBounds(newBounds);
                bar().updateEditLayerFields(active);
                ctx.canvasPanel().repaint();
            }
            return;
        }

        if (dragging && move != null && dragStart != null) {
            int dx = e.getX() - dragStart.x;
            int dy = e.getY() - dragStart.y;
            Rectangle newBounds = move.move(dx, dy);
            if (newBounds != null) {
                bar().updateEditLayerFields(active);
                ctx.canvasPanel().repaint();
            }
        }
    } // --- Fin del metodo mouseDragged ---


    @Override
    public void mouseReleased(MouseEvent e) {
        if (gizmoDragging && gizmoDrag != null) {
            gizmoDrag.end();
        }
        if (gizmoDragging && rotateDrag != null) {
            rotateDrag.end();
        }
        gizmoDragging = false;
        gizmoDrag = null;
        rotateDrag = null;
        dragging = false;
        move = null;
        dragStart = null;

        // Restaurar la capa activa previa tras un Ctrl+clic temporal
        if (ctrlRestoreLayer != null) {
            picker().activateLayer(ctrlRestoreLayer);
            ctrlRestoreLayer = null;
        }
        ctx.canvasPanel().repaint();
    } // --- Fin del metodo mouseReleased ---


    @Override
    public boolean cancel() {
        boolean hadDrag = gizmoDragging || dragging;
        if (gizmoDragging) {
            Layer active = model().getActiveLayer();
            if (gizmoDrag != null) {
                Rectangle restored = gizmoDrag.cancel();
                if (restored != null && active != null) {
                    active.setBounds(restored);
                }
                gizmoDrag = null;
            }
            if (rotateDrag != null) {
                rotateDrag.cancel();
                rotateDrag = null;
            }
            gizmoDragging = false;
        }
        if (dragging) {
            if (move != null) {
                move.cancel();
                move = null;
            }
            dragging = false;
        }
        dragStart = null;
        if (ctrlRestoreLayer != null) {
            picker().activateLayer(ctrlRestoreLayer);
            ctrlRestoreLayer = null;
        }
        if (hadDrag) {
            bar().updateEditLayerFields(model().getActiveLayer());
            ctx.canvasPanel().repaint();
        }
        return hadDrag;
    } // --- Fin del metodo cancel ---


    // ======================== TECLADO ========================


    @Override
    public void keyPressed(KeyEvent e) {
        Layer active = model().getActiveLayer();
        if (active == null || active.getBounds() == null) return;
        if (active.isLocked()) return;

        int step = e.isShiftDown() ? 10 : 1;
        int dx = 0, dy = 0;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT -> dx = -step;
            case KeyEvent.VK_RIGHT -> dx = step;
            case KeyEvent.VK_UP -> dy = -step;
            case KeyEvent.VK_DOWN -> dy = step;
            default -> { return; }
        }

        Rectangle b = active.getBounds();
        active.setBounds(new Rectangle(b.x + dx, b.y + dy, b.width, b.height));
        bar().updateEditLayerFields(active);
        ctx.canvasPanel().repaint();
    } // --- Fin del metodo keyPressed ---


    // ======================== OVERLAY ========================


    @Override
    public void paintOverlay(Graphics2D g2) {
        if (!isShowGizmo()) return;
        Layer active = model().getActiveLayer();
        if (active == null || active.getBounds() == null) return;
        ctx.gizmo().setRotation(active.getRotation());
        ctx.gizmo().draw(g2, active.getBounds());
    } // --- Fin del metodo paintOverlay ---


    // ======================== CURSOR ========================


    @Override
    public Cursor getCursor() {
        Layer active = model().getActiveLayer();
        if (active == null || active.getBounds() == null) return Cursor.getDefaultCursor();
        Point mp = ctx.canvasPanel().getMousePosition();
        if (mp != null) {
            if (isShowGizmo()) {
                ctx.gizmo().setRotation(active.getRotation());
                TransformGizmo.Handle h = ctx.gizmo().hitTest(mp, active.getBounds());
                if (h != TransformGizmo.Handle.NONE) {
                    return ctx.gizmo().getCursor(h);
                }
            }
            if (active.getBounds().contains(mp)) {
                return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
            }
        }
        return Cursor.getDefaultCursor();
    } // --- Fin del metodo getCursor ---

} // --- Fin de la clase EditTool ---
