package controlador.tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import controlador.commands.AppActionCommands;
import modelo.editor.CanvasModel;
import modelo.editor.Layer;
import modelo.editor.LayerModel;
import modelo.editor.smartguides.GuideLine;
import modelo.editor.smartguides.SmartGuidesConfig;
import modelo.editor.smartguides.SmartGuidesEngine;
import modelo.editor.smartguides.SnapResult;
import modelo.gizmo.TransformGizmo;
import vista.panels.render.EditorComponentBar;

/**
 * Herramienta de seleccion y manipulacion universal (Move Tool).
 * <ul>
 *   <li>1 clic sobre capa -> selección simple (si la auto-selección está activa)
 *   <li>Ctrl+clic -> añade/quita la capa de la selección múltiple
 *   <li>Shift+clic -> selecciona el rango desde la capa activa
 *   <li>arrastre en vacío -> marco que selecciona las capas atrapadas (Shift añade)
 *   <li>1 clic simple fuera -> deselecciona
 *   <li>arrastre -> mueve las capas seleccionadas (Alt = duplicar y mover la copia)
 *   <li>tiradores opcionales con {@link TransformGizmo}
 *   <li>flechas del teclado -> mueven las capas seleccionadas 1 px (Shift = 10 px)
 * </ul>
 * <p>
 * La selección, el movimiento y el gizmo están delegados en
 * {@link LayerPicker}; el doble clic (atajo) delega en el editor del tipo de
 * capa vía {@link controlador.tools.editors.LayerEditorRegistry}.
 */
public class EditTool extends Tool {

    private boolean dragging;
    private boolean gizmoDragging;
    private Point dragStart;
    private LayerPicker.MultiGizmoDrag gizmoDrag;
    private LayerPicker.MultiRotateDrag rotateDrag;
    private LayerPicker.DragMove move;
    private SmartGuidesEngine smartGuides;
    private List<GuideLine> smartGuideLines = new ArrayList<>();
    private boolean smartGuidesOverride;

    // Marco de selección de capas en zona vacía
    private boolean marqueeSelecting;
    private Point marqueeStart;
    private Rectangle marqueeRect;

    private static final Color MARQUEE_FILL = new Color(0, 120, 215, 40);
    private static final Color MARQUEE_BORDER = new Color(0, 120, 215);
    private static final float MARQUEE_DASH[] = { 4f, 4f };

    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_EDICION;
    } // --- Fin del metodo getCommandKey ---

    @Override
    public boolean modifiesContent() {
        return true;
    } // --- Fin del metodo modifiesContent ---

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
            var history = bar().getEditorHistory();
            if (history != null) {
                history.record("Propiedades de capa",
                        () -> layer.setBounds(new Rectangle(x, y, w, h)));
            } else {
                layer.setBounds(new Rectangle(x, y, w, h));
            }
            ctx.canvasPanel().repaint();
        } catch (Exception ignored) {
        }
    } // --- Fin del metodo syncLayerFromSpinners ---


    private void syncLayerFromAngleSpinner() {
        Layer layer = model().getActiveLayer();
        if (layer == null || layer.isLocked()) return;
        try {
            double angle = ((Number) bar().getSpinnerAngle().getValue()).doubleValue();
            var history = bar().getEditorHistory();
            if (history != null) {
                history.record("Rotación de capa", () -> layer.setRotation(angle));
            } else {
                layer.setRotation(angle);
            }
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
        marqueeSelecting = false;
        marqueeStart = null;
        marqueeRect = null;
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
        smartGuides = null;
        smartGuideLines = new ArrayList<>();
        smartGuidesOverride = false;

        boolean ctrl = (e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0;
        boolean shift = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
        boolean alt = (e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0;

        // 1. Tiradores del gizmo (MOVE se excluye: lo maneja el paso 5)
        if (isShowGizmo()) {
            List<Layer> targets = gizmoTargets();
            Rectangle gb = gizmoBounds(targets);
            if (gb != null) {
                ctx.gizmo().setRotation(targets.size() > 1 ? 0 : targets.get(0).getRotation());
                TransformGizmo.Handle h = ctx.gizmo().hitTest(p, gb);
                if (h == TransformGizmo.Handle.ROTATE) {
                    rotateDrag = picker().beginMultiRotate(targets, gb, p);
                    if (rotateDrag != null) {
                        gizmoDragging = true;
                        dragStart = p;
                        return;
                    }
                } else if (h != TransformGizmo.Handle.NONE && h != TransformGizmo.Handle.MOVE) {
                    gizmoDrag = picker().beginMultiGizmo(h, targets, gb);
                    gizmoDragging = true;
                    dragStart = p;
                    return;
                }
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

        // 3. Selecci\u00F3n estilo Photoshop sobre el canvas:
        //    - clic normal           -> selecci\u00F3n simple (o se mantiene el
        //                               conjunto si la capa ya estaba seleccionada)
        //    - Ctrl+clic             -> a\u00F1ade/quita de la selecci\u00F3n m\u00FAltiple
        //    - Shift+clic            -> rango desde la capa activa
        if (isAutoSelect()) {
            Layer hit = picker().findLayerAt(p);
            if (hit != null) {
                int index = model().getLayers().indexOf(hit);
                if (ctrl) {
                    model().toggleSelected(index);
                } else if (shift) {
                    model().selectRange(index);
                } else if (model().isSelected(index)) {
                    model().setActiveLayer(index);
                } else {
                    model().setSingleSelection(index);
                }
                active = model().getActiveLayer();
                bar().updateEditLayerFields(active);
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
            beginSmartGuides(List.of(copy));
            return;
        }

        // 5. Iniciar arrastre para mover las capas seleccionadas, o si se pulsa
        //    en vacío (fuera de toda capa, tanto si la auto-selección está
        //    activa como si no) iniciar un marco de selección de capas
        active = model().getActiveLayer();
        List<Layer> targets = gizmoTargets();
        Rectangle gb = gizmoBounds(targets);
        if (gb != null && gb.contains(p)) {
            dragging = true;
            dragStart = p;
            move = picker().beginMove(p, targets);
            beginSmartGuides(targets);
        } else if (!ctrl && picker().findLayerAt(p) == null) {
            marqueeSelecting = true;
            marqueeStart = p;
            marqueeRect = new Rectangle(p.x, p.y, 0, 0);
        }
    } // --- Fin del metodo mousePressed ---


    @Override
    public void mouseDragged(MouseEvent e) {
        // Marco de selección de capas: actualizar el rectángulo en curso
        if (marqueeSelecting && marqueeStart != null) {
            int x = Math.min(marqueeStart.x, e.getX());
            int y = Math.min(marqueeStart.y, e.getY());
            int w = Math.abs(e.getX() - marqueeStart.x);
            int h = Math.abs(e.getY() - marqueeStart.y);
            marqueeRect = new Rectangle(x, y, w, h);
            ctx.canvasPanel().repaint();
            return;
        }

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
            gizmoDrag.drag(dx, dy);
            bar().updateEditLayerFields(model().getActiveLayer());
            ctx.canvasPanel().repaint();
            return;
        }

        if (dragging && move != null && dragStart != null) {
            int dx = e.getX() - dragStart.x;
            int dy = e.getY() - dragStart.y;
            SnapResult snap = updateSmartGuides(dx, dy, e);
            Rectangle newBounds = move.move(snap.dx(), snap.dy());
            if (newBounds != null) {
                bar().updateEditLayerFields(active);
                ctx.canvasPanel().repaint();
            }
        }
    } // --- Fin del metodo mouseDragged ---


    @Override
    public void mouseReleased(MouseEvent e) {
        // Marco de selección de capas: cerrar y aplicar las capas atrapadas
        if (marqueeSelecting) {
            marqueeSelecting = false;
            marqueeStart = null;
            Rectangle rect = marqueeRect;
            marqueeRect = null;
            if (rect != null) {
                List<Layer> atrapadas = picker().findLayersIn(rect);
                if (!atrapadas.isEmpty()) {
                    List<Integer> indices = new ArrayList<>();
                    for (Layer l : atrapadas) {
                        indices.add(model().getLayers().indexOf(l));
                    }
                    boolean shift = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
                    if (shift) {
                        Set<Integer> union = new LinkedHashSet<>(model().getSelectedIndices());
                        union.addAll(indices);
                        model().setSelectedIndices(union);
                    } else {
                        model().setSelectedIndices(indices);
                    }
                    bar().updateEditLayerFields(model().getActiveLayer());
                } else if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == 0) {
                    picker().clearSelection();
                }
            }
            ctx.canvasPanel().repaint();
            return;
        }

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
        endSmartGuides();

        ctx.canvasPanel().repaint();
    } // --- Fin del metodo mouseReleased ---


    @Override
    public boolean cancel() {
        if (marqueeSelecting) {
            marqueeSelecting = false;
            marqueeStart = null;
            marqueeRect = null;
            ctx.canvasPanel().repaint();
            return true;
        }
        boolean hadDrag = gizmoDragging || dragging;
        if (gizmoDragging) {
            if (gizmoDrag != null) {
                gizmoDrag.cancel();
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
        endSmartGuides();
        if (hadDrag) {
            bar().updateEditLayerFields(model().getActiveLayer());
            ctx.canvasPanel().repaint();
        }
        return hadDrag;
    } // --- Fin del metodo cancel ---


    // ======================== TECLADO ========================


    /**
     * Capas seleccionadas, visibles y no bloqueadas, que se mueven juntas en un
     * arrastre o con las flechas. Si no hay selección, devuelve la capa activa.
     *
     * @return lista de capas a mover (nunca vacía si hay capa activa utilizable)
     */
    private List<Layer> movableSelectedLayers() {
        List<Layer> result = new ArrayList<>();
        for (int idx : model().getSelectedIndices()) {
            Layer layer = model().getLayer(idx);
            if (layer != null && layer.isVisible() && !layer.isLocked()) {
                result.add(layer);
            }
        }
        if (result.isEmpty()) {
            Layer active = model().getActiveLayer();
            if (active != null && active.isVisible() && !active.isLocked()) {
                result.add(active);
            }
        }
        return result;
    } // --- Fin del metodo movableSelectedLayers ---


    /**
     * Capas objetivo del gizmo: las mismas que se moverían juntas
     * ({@link #movableSelectedLayers()}).
     *
     * @return lista de capas sobre las que dibuja y opera el gizmo
     */
    private List<Layer> gizmoTargets() {
        return movableSelectedLayers();
    } // --- Fin del metodo gizmoTargets ---


    /**
     * Rectángulo sobre el que dibuja y opera el gizmo. Con selección múltiple
     * devuelve la unión de las capas objetivo (marco global estilo Photoshop);
     * con una sola capa, sus propios bounds.
     *
     * @param targets capas objetivo del gizmo
     * @return el rectángulo del marco, o null si no hay ninguna usable
     */
    private Rectangle gizmoBounds(List<Layer> targets) {
        if (targets == null || targets.isEmpty()) return null;
        if (targets.size() == 1) {
            Layer l = targets.get(0);
            return l.getBounds() != null ? l.getBounds() : null;
        }
        Rectangle union = null;
        for (Layer l : targets) {
            Rectangle b = l.getBounds();
            if (b == null) continue;
            union = (union == null) ? new Rectangle(b) : union.union(b);
        }
        return union;
    } // --- Fin del metodo gizmoBounds ---


    @Override
    public void keyPressed(KeyEvent e) {
        List<Layer> layers = movableSelectedLayers();
        if (layers.isEmpty()) return;

        int step = e.isShiftDown() ? 10 : 1;
        int dx = 0, dy = 0;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT -> dx = -step;
            case KeyEvent.VK_RIGHT -> dx = step;
            case KeyEvent.VK_UP -> dy = -step;
            case KeyEvent.VK_DOWN -> dy = step;
            default -> { return; }
        }

        var history = bar().getEditorHistory();
        if (history != null) {
            List<Layer> finalLayers = layers;
            int fdx = dx, fdy = dy;
            history.record("Mover capa", () -> {
                for (Layer layer : finalLayers) {
                    Rectangle b = layer.getBounds();
                    layer.setBounds(new Rectangle(b.x + fdx, b.y + fdy, b.width, b.height));
                }
            });
        } else {
            for (Layer layer : layers) {
                Rectangle b = layer.getBounds();
                layer.setBounds(new Rectangle(b.x + dx, b.y + dy, b.width, b.height));
            }
        }
        bar().updateEditLayerFields(model().getActiveLayer());
        ctx.canvasPanel().repaint();
    } // --- Fin del metodo keyPressed ---


    // ======================== SMART GUIDES ========================


    /**
     * Inicia la sesión de Smart Guides cacheando el lienzo, las capas estáticas
     * y los bounds de partida de las capas que se van a arrastrar.
     *
     * @param targets capas que se moverán en este arrastre
     */
    private void beginSmartGuides(List<Layer> targets) {
        smartGuides = null;
        smartGuideLines = new ArrayList<>();
        smartGuidesOverride = false;
        SmartGuidesConfig cfg = SmartGuidesConfig.get();
        if (!cfg.isShowGuides() || targets == null || targets.isEmpty()) return;

        CanvasModel canvas = ctx.canvasModel();
        if (canvas == null) return;

        List<Rectangle> moving = new ArrayList<>();
        List<Rectangle> statics = new ArrayList<>();
        for (Layer layer : model().getLayers()) {
            Rectangle b = layer.getBounds();
            if (b == null) continue;
            if (targets.contains(layer)) {
                moving.add(new Rectangle(b));
            } else if (layer.isVisible() && !layer.isLocked()) {
                statics.add(new Rectangle(b));
            }
        }
        if (moving.isEmpty()) return;

        smartGuides = new SmartGuidesEngine();
        smartGuides.begin(new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight()), statics, moving);
    } // --- Fin del metodo beginSmartGuides ---


    /**
     * Calcula el desplazamiento corregido aplicando el snap. Mantener Ctrl
     * durante el arrastre anula temporalmente el ajuste y las guías.
     *
     * @param dx desplazamiento X crudo acumulado
     * @param dy desplazamiento Y crudo acumulado
     * @param e evento de ratón actual
     * @return desplazamientos corregidos y guías activas
     */
    private SnapResult updateSmartGuides(int dx, int dy, MouseEvent e) {
        SmartGuidesConfig cfg = SmartGuidesConfig.get();
        smartGuidesOverride = (e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0;
        if (!cfg.isShowGuides() || smartGuidesOverride || smartGuides == null) {
            smartGuideLines = List.of();
            return new SnapResult(dx, dy, List.of());
        }
        SnapResult result = smartGuides.update(dx, dy, cfg);
        smartGuideLines = result.guides();
        return result;
    } // --- Fin del metodo updateSmartGuides ---


    /**
     * Finaliza la sesión de Smart Guides y limpia las guías temporales.
     */
    private void endSmartGuides() {
        if (smartGuides != null) {
            smartGuides.end();
            smartGuides = null;
        }
        smartGuideLines = new ArrayList<>();
        smartGuidesOverride = false;
    } // --- Fin del metodo endSmartGuides ---


    /**
     * Dibuja las guías de ajuste activas a línea completa del lienzo.
     *
     * @param g2 gráficos del overlay en coordenadas de canvas
     */
    private void drawSmartGuides(Graphics2D g2) {
        if (smartGuidesOverride || smartGuideLines == null || smartGuideLines.isEmpty()) return;
        SmartGuidesConfig cfg = SmartGuidesConfig.get();
        if (!cfg.isShowGuides()) return;
        g2.setColor(cfg.getGuideColor());
        g2.setStroke(new BasicStroke(cfg.getGuideStrokeWidth()));
        for (GuideLine line : smartGuideLines) {
            if (line.vertical()) {
                g2.drawLine(line.pos(), line.start(), line.pos(), line.end());
            } else {
                g2.drawLine(line.start(), line.pos(), line.end(), line.pos());
            }
        }
    } // --- Fin del metodo drawSmartGuides ---


    // ======================== OVERLAY ========================


    @Override
    public void paintOverlay(Graphics2D g2) {
        // Marco de selección de capas en curso
        if (marqueeSelecting && marqueeRect != null) {
            g2.setColor(MARQUEE_FILL);
            g2.fill(marqueeRect);
            Stroke original = g2.getStroke();
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL, 0, MARQUEE_DASH, 0));
            g2.setColor(MARQUEE_BORDER);
            g2.draw(marqueeRect);
            g2.setStroke(original);
            return;
        }
        drawSmartGuides(g2);
        if (!isShowGizmo()) return;
        List<Layer> targets = gizmoTargets();
        Rectangle gb = gizmoBounds(targets);
        if (gb == null) return;
        ctx.gizmo().setRotation(targets.size() > 1 ? 0 : targets.get(0).getRotation());
        ctx.gizmo().draw(g2, gb);
    } // --- Fin del metodo paintOverlay ---


    // ======================== CURSOR ========================


    @Override
    public Cursor getCursor() {
        List<Layer> targets = gizmoTargets();
        Rectangle gb = gizmoBounds(targets);
        if (gb == null) return Cursor.getDefaultCursor();
        Point mp = ctx.canvasPanel().getMousePosition();
        if (mp != null) {
            if (isShowGizmo()) {
                ctx.gizmo().setRotation(targets.size() > 1 ? 0 : targets.get(0).getRotation());
                TransformGizmo.Handle h = ctx.gizmo().hitTest(mp, gb);
                if (h != TransformGizmo.Handle.NONE) {
                    return ctx.gizmo().getCursor(h);
                }
            }
            if (gb.contains(mp)) {
                return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
            }
        }
        return Cursor.getDefaultCursor();
    } // --- Fin del metodo getCursor ---

} // --- Fin de la clase EditTool ---
