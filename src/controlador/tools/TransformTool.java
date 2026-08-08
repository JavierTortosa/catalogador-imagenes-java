package controlador.tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import modelo.editor.Layer;
import modelo.gizmo.TransformGizmo;
import modelo.gizmo.TransformGizmo.Handle;

/**
 * Herramienta de transformación (mover, escalar, rotar).
 * <p>
 * Lee de la barra el modo (mover/escalar/rotar) y el target (capa/marco de
 * selección). Con target {@code capa} gestiona la selección de capas estilo
 * Photoshop y transforma todas las capas seleccionadas juntas; con target
 * {@code marco}, el gizmo opera sobre los bounds de la {@code SelectionModel}.
 * El gizmo solo dibuja los tiradores aplicables al modo activo.
 * <p>
 * Tres casos según dónde se pulse:
 * <ul>
 *   <li><b>Caso A</b> zona vacía → marco de selección de capas (Shift añade).
 *   <li><b>Caso B</b> capa no seleccionada → clic la selecciona, Ctrl alterna,
 *       Shift añade.
 *   <li><b>Caso C</b> tirador del gizmo o capa ya seleccionada → transformación
 *       (mover/escalar/rotar) sobre todas las capas seleccionadas.
 * </ul>
 */
public class TransformTool extends Tool {

    private TransformGizmo gizmo;
    private Handle activeHandle;
    private Point dragStart;

    // Arrastres delegados en LayerPicker (rotación y transformación no destructiva)
    private LayerPicker.RotateDrag rotateDrag;
    private LayerPicker.MultiRotateDrag multiRotateDrag;
    private LayerPicker.MultiGizmoDrag multiGizmoDrag;
    private LayerPicker.GizmoDrag gizmoDrag;
    private LayerPicker.DragMove move;

    // Capas objetivo capturadas al iniciar el arrastre
    private List<Layer> targets = new ArrayList<>();

    // Marco de selección de capas en zona vacía (Caso A)
    private boolean marqueeSelecting;
    private Point marqueeStart;
    private Rectangle marqueeRect;

    private static final Color MARQUEE_FILL = new Color(0, 120, 215, 40);
    private static final Color MARQUEE_BORDER = new Color(0, 120, 215);
    private static final float MARQUEE_DASH[] = { 4f, 4f };

    @Override
    public String getCommandKey() {
        return controlador.commands.AppActionCommands.CMD_ADVANCED_EDITOR_TRANSFORMAR;
    } // --- Fin del metodo getCommandKey ---

    @Override
    public boolean modifiesContent() {
        return true;
    } // --- Fin del metodo modifiesContent ---

    @Override
    public void onActivate() {
        this.gizmo = ctx.gizmo();
        this.dragStart = null;
        this.activeHandle = null;
        this.targets = new ArrayList<>();
        this.move = null;
        this.marqueeSelecting = false;
        this.marqueeStart = null;
        this.marqueeRect = null;
    } // --- Fin del metodo onActivate ---

    @Override
    public void onDeactivate() {
        activeHandle = null;
        dragStart = null;
        rotateDrag = null;
        multiRotateDrag = null;
        multiGizmoDrag = null;
        gizmoDrag = null;
        move = null;
        targets = new ArrayList<>();
        marqueeSelecting = false;
        marqueeStart = null;
        marqueeRect = null;
    } // --- Fin del metodo onDeactivate ---

    private boolean targetMarco() {
        return "marco".equals(ctx.componentBar().getTransformTarget());
    } // --- Fin del metodo targetMarco ---

    /**
     * Capas objetivo de la transformación: las seleccionadas (visibles y no
     * bloqueadas); si no hay selección, la capa activa. El orden no importa.
     *
     * @return lista de capas a transformar (puede estar vacía)
     */
    private List<Layer> targetLayers() {
        List<Layer> result = new ArrayList<>();
        if (ctx.layerModel() == null) return result;
        for (int idx : ctx.layerModel().getSelectedIndices()) {
            Layer layer = ctx.layerModel().getLayer(idx);
            if (layer != null && layer.isVisible() && !layer.isLocked()) {
                result.add(layer);
            }
        }
        if (result.isEmpty()) {
            Layer active = ctx.layerModel().getActiveLayer();
            if (active != null && active.isVisible() && !active.isLocked()) {
                result.add(active);
            }
        }
        return result;
    } // --- Fin del metodo targetLayers ---

    /**
     * @return los bounds del elemento a transformar (unión de las capas
     *         objetivo o selección), o null si no hay nada transformable
     */
    private Rectangle getTargetBounds() {
        if (targetMarco()) {
            if (ctx.selectionModel() == null || !ctx.selectionModel().isActive()) return null;
            return ctx.selectionModel().getBounds();
        }
        List<Layer> layers = targetLayers();
        if (layers.isEmpty()) return null;
        return ctx.layerPicker().unionBounds(layers);
    } // --- Fin del metodo getTargetBounds ---

    /**
     * Sincroniza la rotación del gizmo: la de la única capa objetivo, o 0 si
     * hay varias (la unión no tiene rotación propia). El marco de selección
     * tampoco tiene rotación.
     */
    private void syncGizmoRotation() {
        double rotation = 0;
        if (!targetMarco()) {
            List<Layer> layers = targetLayers();
            if (layers.size() == 1) {
                rotation = layers.get(0).getRotation();
            }
        }
        gizmo.setRotation(rotation);
    } // --- Fin del metodo syncGizmoRotation ---

    /**
     * Filtra los tiradores del gizmo según el modo seleccionado en la barra. La
     * rotación no aplica sobre un marco de selección.
     */
    private boolean allowedHandle(Handle h) {
        String mode = ctx.componentBar().getTransformMode();
        return switch (mode) {
            case "escalar" -> h != Handle.NONE && h != Handle.MOVE && h != Handle.ROTATE;
            case "rotar" -> h == Handle.ROTATE && !targetMarco();
            default -> h == Handle.MOVE;
        };
    } // --- Fin del metodo allowedHandle ---

    /**
     * @return predicado para que el gizmo dibuje solo los tiradores del modo activo
     */
    private Predicate<Handle> handleFilter() {
        String mode = ctx.componentBar().getTransformMode();
        return switch (mode) {
            case "escalar" -> h -> h != Handle.NONE && h != Handle.MOVE && h != Handle.ROTATE;
            case "rotar" -> h -> h == Handle.ROTATE && !targetMarco();
            default -> h -> false;
        };
    } // --- Fin del metodo handleFilter ---

    private void applyTargetBounds(Rectangle newBounds) {
        if (newBounds == null) return;
        if (targetMarco()) {
            if (ctx.selectionModel() != null) {
                ctx.selectionModel().setBounds(newBounds);
            }
        } else if (targets.size() == 1) {
            targets.get(0).setBounds(newBounds);
        }
    } // --- Fin del metodo applyTargetBounds ---

    @Override
    public void mousePressed(MouseEvent e) {
        if (targetMarco()) {
            mousePressedMarco(e);
            return;
        }

        boolean ctrl = (e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0;
        boolean shift = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;

        // Caso C: clic sobre un tirador del gizmo del marco de las capas seleccionadas
        List<Layer> layers = targetLayers();
        Rectangle bounds = layers.isEmpty() ? null : ctx.layerPicker().unionBounds(layers);
        if (bounds != null) {
            syncGizmoRotation();
            Handle h = gizmo.hitTest(e.getPoint(), bounds);
            if (allowedHandle(h)) {
                beginHandleDrag(h, layers, bounds, e);
                return;
            }
        }

        // Caso B: clic sobre una capa → seleccionar (Ctrl alterna, Shift añade)
        Layer hit = ctx.layerPicker() != null ? ctx.layerPicker().findLayerAt(e.getPoint()) : null;
        if (hit != null) {
            int index = ctx.layerModel().getLayers().indexOf(hit);
            if (index >= 0) {
                if (ctrl) {
                    ctx.layerModel().toggleSelected(index);
                } else if (shift) {
                    ctx.layerModel().addSelection(index);
                } else if (!ctx.layerModel().isSelected(index)) {
                    ctx.layerModel().setSingleSelection(index);
                }
                if (ctx.componentBar() != null) {
                    ctx.componentBar().updateEditLayerFields(ctx.layerModel().getActiveLayer());
                }
                ctx.canvasPanel().repaint();
            }
            return;
        }

        // Caso A: zona vacía → iniciar marco de selección de capas
        marqueeSelecting = true;
        marqueeStart = e.getPoint();
        marqueeRect = new Rectangle(marqueeStart.x, marqueeStart.y, 0, 0);
    } // --- Fin del metodo mousePressed ---

    /**
     * Gestiona el clic con target {@code marco}: el gizmo opera sobre los bounds
     * de la {@code SelectionModel} (mover/escalar; la rotación no aplica).
     */
    private void mousePressedMarco(MouseEvent e) {
        Rectangle bounds = getTargetBounds();
        if (bounds == null) return;

        syncGizmoRotation();
        Handle h = gizmo.hitTest(e.getPoint(), bounds);
        if (!allowedHandle(h)) h = Handle.NONE;
        activeHandle = h;
        dragStart = e.getPoint();

        if (activeHandle == Handle.NONE) return;

        if (activeHandle == Handle.ROTATE) {
            activeHandle = Handle.NONE;
            return;
        }

        gizmo.startDrag(activeHandle, bounds,
                new TransformGizmo.Constraints(ctx.componentBar().isKeepAspect(), 0, 10));
    } // --- Fin del metodo mousePressedMarco ---

    /**
     * Inicia el arrastre de la transformación según el tirador pulsado (Caso C).
     *
     * @param h      tirador activo (ya filtrado por {@link #allowedHandle})
     * @param layers capas seleccionadas a transformar
     * @param bounds marco unión al iniciar el arrastre
     * @param e      evento de pulsación
     */
    private void beginHandleDrag(Handle h, List<Layer> layers, Rectangle bounds, MouseEvent e) {
        activeHandle = h;
        dragStart = e.getPoint();

        if (h == Handle.ROTATE) {
            if (layers.isEmpty()) {
                activeHandle = Handle.NONE;
                return;
            }
            if (layers.size() > 1) {
                multiRotateDrag = ctx.layerPicker().beginMultiRotate(layers, bounds, e.getPoint());
            } else {
                rotateDrag = ctx.layerPicker().beginRotate(layers.get(0), e.getPoint());
            }
            if (rotateDrag == null && multiRotateDrag == null) {
                activeHandle = Handle.NONE;
            }
            return;
        }

        if (h == Handle.MOVE) {
            move = ctx.layerPicker().beginMove(e.getPoint(), layers);
            if (move == null) {
                activeHandle = Handle.NONE;
            }
            return;
        }

        targets = layers;
        if (layers.size() > 1) {
            multiGizmoDrag = ctx.layerPicker().beginMultiGizmo(h, layers, bounds);
            if (multiGizmoDrag == null) activeHandle = Handle.NONE;
        } else {
            gizmoDrag = ctx.layerPicker().beginGizmo(h, layers.get(0));
            if (gizmoDrag == null) activeHandle = Handle.NONE;
        }
    } // --- Fin del metodo beginHandleDrag ---

    @Override
    public void mouseDragged(MouseEvent e) {
        // Caso A: actualizar el marco de selección de capas
        if (marqueeSelecting && marqueeStart != null) {
            int x = Math.min(marqueeStart.x, e.getX());
            int y = Math.min(marqueeStart.y, e.getY());
            int w = Math.abs(e.getX() - marqueeStart.x);
            int h = Math.abs(e.getY() - marqueeStart.y);
            marqueeRect = new Rectangle(x, y, w, h);
            ctx.canvasPanel().repaint();
            return;
        }

        // Mover las capas seleccionadas juntas (Caso C, modo mover)
        if (move != null && dragStart != null) {
            int dx = e.getPoint().x - dragStart.x;
            int dy = e.getPoint().y - dragStart.y;
            move.move(dx, dy);
            ctx.canvasPanel().repaint();
            return;
        }

        if (activeHandle == null || dragStart == null) return;
        int dx = e.getPoint().x - dragStart.x;
        int dy = e.getPoint().y - dragStart.y;

        if (activeHandle == Handle.ROTATE) {
            boolean shift = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
            if (multiRotateDrag != null) {
                multiRotateDrag.drag(e.getPoint(), shift);
            } else if (rotateDrag != null) {
                rotateDrag.drag(e.getPoint(), shift);
            }
            ctx.canvasPanel().repaint();
            return;
        }

        if (multiGizmoDrag != null) {
            multiGizmoDrag.drag(dx, dy);
            ctx.canvasPanel().repaint();
            return;
        }

        if (gizmoDrag != null) {
            Rectangle newBounds = gizmoDrag.drag(dx, dy);
            if (newBounds != null) {
                applyTargetBounds(newBounds);
            }
            ctx.canvasPanel().repaint();
            return;
        }

        Rectangle newBounds = gizmo.drag(dx, dy);
        if (newBounds != null) {
            applyTargetBounds(newBounds);
            ctx.canvasPanel().repaint();
        }
    } // --- Fin del metodo mouseDragged ---

    @Override
    public void mouseReleased(MouseEvent e) {
        // Caso A: cerrar el marco y aplicar la selección de capas atrapadas
        if (marqueeSelecting) {
            marqueeSelecting = false;
            marqueeStart = null;
            Rectangle rect = marqueeRect;
            marqueeRect = null;
            if (ctx.layerPicker() != null && rect != null) {
                List<Layer> atrapadas = ctx.layerPicker().findLayersIn(rect);
                if (!atrapadas.isEmpty()) {
                    List<Integer> indices = new ArrayList<>();
                    for (Layer l : atrapadas) {
                        indices.add(ctx.layerModel().getLayers().indexOf(l));
                    }
                    boolean shift = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
                    if (shift) {
                        Set<Integer> union = new LinkedHashSet<>(ctx.layerModel().getSelectedIndices());
                        union.addAll(indices);
                        ctx.layerModel().setSelectedIndices(union);
                    } else {
                        ctx.layerModel().setSelectedIndices(indices);
                    }
                } else if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == 0) {
                    ctx.layerModel().clearAllSelection();
                }
            }
            ctx.canvasPanel().repaint();
            return;
        }

        if (move != null) {
            move = null;
            activeHandle = null;
            dragStart = null;
            ctx.canvasPanel().repaint();
            return;
        }

        if (activeHandle == null) return;
        if (multiGizmoDrag != null) {
            multiGizmoDrag.end();
        } else if (gizmoDrag != null) {
            gizmoDrag.end();
        } else if (activeHandle != Handle.ROTATE) {
            gizmo.endDrag();
        }
        if (rotateDrag != null) {
            rotateDrag.end();
        }
        if (multiRotateDrag != null) {
            multiRotateDrag.end();
        }
        activeHandle = null;
        dragStart = null;
        rotateDrag = null;
        multiRotateDrag = null;
        gizmoDrag = null;
        multiGizmoDrag = null;
        targets = new ArrayList<>();
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
        if (move != null) {
            move.cancel();
            move = null;
            activeHandle = null;
            dragStart = null;
            ctx.canvasPanel().repaint();
            return true;
        }
        if (activeHandle == null) return false;
        if (activeHandle == Handle.ROTATE) {
            if (multiRotateDrag != null) {
                multiRotateDrag.cancel();
            } else if (rotateDrag != null) {
                rotateDrag.cancel();
            }
        } else {
            if (multiGizmoDrag != null) {
                multiGizmoDrag.cancel();
            } else if (gizmoDrag != null) {
                gizmoDrag.cancel();
            } else {
                Rectangle startBounds = gizmo.endDrag();
                if (startBounds != null) {
                    applyTargetBounds(startBounds);
                }
            }
        }
        activeHandle = null;
        dragStart = null;
        rotateDrag = null;
        multiRotateDrag = null;
        gizmoDrag = null;
        multiGizmoDrag = null;
        move = null;
        targets = new ArrayList<>();
        ctx.canvasPanel().repaint();
        return true;
    } // --- Fin del metodo cancel ---

    @Override
    public void mouseMoved(MouseEvent e) {
        Rectangle bounds = getTargetBounds();
        if (bounds == null) return;

        syncGizmoRotation();
        Handle handle = gizmo.hitTest(e.getPoint(), bounds);
        if (allowedHandle(handle)) {
            ctx.canvasPanel().setCursor(gizmo.getCursor(handle));
        } else {
            ctx.canvasPanel().setCursor(getCursor());
        }
    } // --- Fin del metodo mouseMoved ---

    @Override
    public void paintOverlay(Graphics2D g2) {
        // Caso A: recuadro de selección de capas en curso
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

        Rectangle bounds = getTargetBounds();
        if (bounds == null) return;
        syncGizmoRotation();
        gizmo.draw(g2, bounds, handleFilter());
    } // --- Fin del metodo paintOverlay ---

    @Override
    public Cursor getCursor() {
        Rectangle bounds = getTargetBounds();
        if (bounds == null) return Cursor.getDefaultCursor();
        Point mp = ctx.canvasPanel().getMousePosition();
        if (mp != null) {
            syncGizmoRotation();
            Handle h = gizmo.hitTest(mp, bounds);
            if (allowedHandle(h)) {
                return gizmo.getCursor(h);
            }
        }
        return Cursor.getDefaultCursor();
    } // --- Fin del metodo getCursor ---

} // --- Fin de la clase TransformTool ---
