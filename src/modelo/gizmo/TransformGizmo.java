package modelo.gizmo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.function.Predicate;

/**
 * Gizmo de transformación reutilizable (mover, escalar, rotar).
 * 
 * Dibuja un marco con 8 tiradores (esquinas + puntos medios) y un asa de
 * rotación sobre el centro superior. Detecta qué tirador se pulsa (hit-test) y
 * calcula las nuevas coordenadas durante el arrastre.
 * 
 * Es independiente de cualquier herramienta; lo usan TransformTool,
 * SelectionTool, TextTool y ShapeTool.
 */
public class TransformGizmo {

    /**
     * Tiradores del gizmo.
     * <ul>
     * <li>NONE — ningún tirador activo</li>
     * <li>MOVE — arrastrar todo el marco</li>
     * <li>NW, N, NE, W, E, SW, S, SE — escalar desde ese punto</li>
     * <li>ROTATE — rotar (asa superior)</li>
     * </ul>
     */
    public enum Handle {
        NONE, MOVE, NW, N, NE, W, E, SW, S, SE, ROTATE
    } // --- Fin del enum Handle ---

    /**
     * Restricciones aplicadas durante el arrastre.
     *
     * @param keepAspect si true, mantiene la relación ancho/alto original
     * @param snapGrid   tamaño de la cuadrícula (0 = desactivado)
     * @param minSize    tamaño mínimo (ancho y alto) en píxeles
     */
    public record Constraints(boolean keepAspect, int snapGrid, int minSize) {
        public Constraints {
            minSize = Math.max(4, minSize);
        }
    } // --- Fin del record Constraints ---

    private static final int HANDLE_SIZE = 8;
    private static final int HANDLE_HALF = HANDLE_SIZE / 2;
    private static final int HIT_RADIUS = 6;
    private static final int ROTATE_OFFSET = 22;

    private static final Color FRAME_COLOR = new Color(0, 120, 215);
    private static final Color HANDLE_FILL = Color.WHITE;
    private static final Color HANDLE_BORDER = new Color(0, 120, 215);
    private static final Color ROTATE_COLOR = new Color(0, 150, 50);
    private static final Color ROTATE_LINE = new Color(0, 150, 50, 180);

    private Handle currentHandle = Handle.NONE;
    private Rectangle dragStartBounds;
    private Constraints constraints = new Constraints(false, 0, 10);
    private double rotation = 0;

    /**
     * Dibuja el gizmo completo sobre el componente.
     *
     * @param g2     contexto gráfico
     * @param bounds rectángulo delimitador del elemento transformado
     */
    public void draw(Graphics2D g2, Rectangle bounds) {
        draw(g2, bounds, null);
    } // --- Fin del metodo draw ---

    /**
     * Dibuja el gizmo filtrando los tiradores según el predicado. Permite que
     * cada herramienta muestre solo los controles aplicables (p.ej. solo el asa
     * de rotación o solo los tiradores de escalado).
     *
     * @param g2     contexto gráfico
     * @param bounds rectángulo delimitador del elemento transformado
     * @param filter predicado que decide qué {@link Handle} se dibuja (si es
     *               null, se dibujan todos como en {@link #draw(Graphics2D, Rectangle)})
     */
    public void draw(Graphics2D g2, Rectangle bounds, Predicate<Handle> filter) {
        if (bounds == null || bounds.isEmpty()) return;

        Graphics2D g = (Graphics2D) g2.create();
        try {
            if (rotation != 0) {
                g.rotate(Math.toRadians(rotation), bounds.getCenterX(), bounds.getCenterY());
            }
            drawFrame(g, bounds);
            if (filter == null || anyResizeHandle(filter)) {
                drawHandles(g, bounds);
            }
            if (filter == null || filter.test(Handle.ROTATE)) {
                drawRotationHandle(g, bounds);
            }
        } finally {
            g.dispose();
        }
    } // --- Fin del metodo draw ---

    /**
     * @return true si el predicado acepta al menos uno de los 8 tiradores de
     *         esquina/punto medio
     */
    private boolean anyResizeHandle(Predicate<Handle> filter) {
        return filter.test(Handle.NW) || filter.test(Handle.N) || filter.test(Handle.NE)
                || filter.test(Handle.W) || filter.test(Handle.E)
                || filter.test(Handle.SW) || filter.test(Handle.S) || filter.test(Handle.SE);
    } // --- Fin del metodo anyResizeHandle ---


    private void drawFrame(Graphics2D g2, Rectangle bounds) {
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(FRAME_COLOR);
        g2.draw(bounds);

        // Pequeñas L en las esquinas para que el marco se vea aunque el fondo sea claro
        int acc = 4;
        g2.draw(new Line2D.Double(bounds.x, bounds.y + acc, bounds.x, bounds.y));
        g2.draw(new Line2D.Double(bounds.x, bounds.y, bounds.x + acc, bounds.y));

        g2.draw(new Line2D.Double(bounds.x + bounds.width - acc, bounds.y, bounds.x + bounds.width, bounds.y));
        g2.draw(new Line2D.Double(bounds.x + bounds.width, bounds.y, bounds.x + bounds.width, bounds.y + acc));

        g2.draw(new Line2D.Double(bounds.x, bounds.y + bounds.height - acc, bounds.x, bounds.y + bounds.height));
        g2.draw(new Line2D.Double(bounds.x, bounds.y + bounds.height, bounds.x + acc, bounds.y + bounds.height));

        g2.draw(new Line2D.Double(bounds.x + bounds.width - acc, bounds.y + bounds.height, bounds.x + bounds.width, bounds.y + bounds.height));
        g2.draw(new Line2D.Double(bounds.x + bounds.width, bounds.y + bounds.height - acc, bounds.x + bounds.width, bounds.y + bounds.height));

        g2.setStroke(new BasicStroke(1f));
    } // --- Fin del metodo drawFrame ---


    /**
     * Dibuja los 8 tiradores cuadrados en esquinas y puntos medios.
     */
    private void drawHandles(Graphics2D g2, Rectangle bounds) {
        int cx = bounds.x + bounds.width / 2;
        int cy = bounds.y + bounds.height / 2;

        Point[] positions = {
                new Point(bounds.x, bounds.y),
                new Point(cx, bounds.y),
                new Point(bounds.x + bounds.width, bounds.y),
                new Point(bounds.x, cy),
                new Point(bounds.x + bounds.width, cy),
                new Point(bounds.x, bounds.y + bounds.height),
                new Point(cx, bounds.y + bounds.height),
                new Point(bounds.x + bounds.width, bounds.y + bounds.height)
        };

        for (Point p : positions) {
            drawHandleAt(g2, p.x, p.y);
        }
    } // --- Fin del metodo drawHandles ---


    private void drawHandleAt(Graphics2D g2, int x, int y) {
        g2.setColor(HANDLE_FILL);
        g2.fillRect(x - HANDLE_HALF, y - HANDLE_HALF, HANDLE_SIZE, HANDLE_SIZE);
        g2.setColor(HANDLE_BORDER);
        g2.drawRect(x - HANDLE_HALF, y - HANDLE_HALF, HANDLE_SIZE, HANDLE_SIZE);
    } // --- Fin del metodo drawHandleAt ---


    private void drawRotationHandle(Graphics2D g2, Rectangle bounds) {
        int cx = bounds.x + bounds.width / 2;
        int topY = bounds.y - ROTATE_OFFSET;

        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(ROTATE_LINE);
        g2.draw(new Line2D.Double(cx, bounds.y, cx, topY + HANDLE_HALF));

        g2.setColor(HANDLE_FILL);
        g2.fill(new Ellipse2D.Double(cx - HANDLE_HALF, topY - HANDLE_HALF, HANDLE_SIZE, HANDLE_SIZE));
        g2.setColor(ROTATE_COLOR);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new Ellipse2D.Double(cx - HANDLE_HALF, topY - HANDLE_HALF, HANDLE_SIZE, HANDLE_SIZE));

        int arcR = 6;
        g2.draw(new Arc2D.Double(cx - arcR, topY - arcR, arcR * 2, arcR * 2, 270, -270, Arc2D.OPEN));

        g2.setStroke(new BasicStroke(1f));
    } // --- Fin del metodo drawRotationHandle ---


    /**
     * Determina qué tirador está en la posición indicada.
     *
     * @param mouse  coordenadas del ratón
     * @param bounds rectángulo actual del elemento
     * @return el Handle en esa posición, o NONE si no hay ninguno
     */
    public Handle hitTest(Point mouse, Rectangle bounds) {
        if (bounds == null || mouse == null) return Handle.NONE;

        // Si el gizmo está rotado, des-rotar el punto del ratón alrededor del
        // centro para comparar en el sistema de coordenadas local del marco.
        int mx = mouse.x;
        int my = mouse.y;
        if (rotation != 0) {
            double cx = bounds.getCenterX();
            double cy = bounds.getCenterY();
            double rad = Math.toRadians(-rotation);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            double dx = mouse.x - cx;
            double dy = mouse.y - cy;
            mx = (int) Math.round(cx + dx * cos - dy * sin);
            my = (int) Math.round(cy + dx * sin + dy * cos);
        }

        int cx = bounds.x + bounds.width / 2;
        int cy = bounds.y + bounds.height / 2;

        int rotY = bounds.y - ROTATE_OFFSET;
        if (dist(mx, my, cx, rotY) <= HIT_RADIUS) return Handle.ROTATE;

        if (dist(mx, my, bounds.x, bounds.y) <= HIT_RADIUS) return Handle.NW;
        if (dist(mx, my, bounds.x + bounds.width, bounds.y) <= HIT_RADIUS) return Handle.NE;
        if (dist(mx, my, bounds.x, bounds.y + bounds.height) <= HIT_RADIUS) return Handle.SW;
        if (dist(mx, my, bounds.x + bounds.width, bounds.y + bounds.height) <= HIT_RADIUS) return Handle.SE;

        if (dist(mx, my, cx, bounds.y) <= HIT_RADIUS) return Handle.N;
        if (dist(mx, my, cx, bounds.y + bounds.height) <= HIT_RADIUS) return Handle.S;
        if (dist(mx, my, bounds.x, cy) <= HIT_RADIUS) return Handle.W;
        if (dist(mx, my, bounds.x + bounds.width, cy) <= HIT_RADIUS) return Handle.E;

        if (bounds.contains(mx, my)) return Handle.MOVE;

        return Handle.NONE;
    } // --- Fin del metodo hitTest ---


    private double dist(int x1, int y1, int x2, int y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    } // --- Fin del metodo dist ---


    /**
     * Inicia un arrastre. Guarda el estado inicial para calcular la
     * transformación durante drag().
     *
     * @param handle      tirador que se arrastra
     * @param bounds      bounds al iniciar el arrastre
     * @param constraints restricciones (opcional, null usa defaults)
     */
    public void startDrag(Handle handle, Rectangle bounds, Constraints constraints) {
        this.currentHandle = handle;
        this.dragStartBounds = new Rectangle(bounds);
        this.constraints = constraints != null ? constraints : new Constraints(false, 0, 10);
    } // --- Fin del metodo startDrag ---


    /**
     * Calcula el nuevo rectángulo tras un desplazamiento del ratón.
     *
     * @param dx desplazamiento horizontal (píxeles)
     * @param dy desplazamiento vertical (píxeles)
     * @return el nuevo Rectangle transformado (no modifica el original)
     */
    public Rectangle drag(int dx, int dy) {
        if (dragStartBounds == null || currentHandle == Handle.NONE) return dragStartBounds;

        // Los deltas del ratón llegan en coordenadas de canvas, pero el marco se
        // dibuja rotado (ver draw/hitTest). Al escalar hay que des-rotar los
        // deltas al sistema local del marco; MOVE se aplica tal cual porque la
        // capa debe seguir al ratón en el canvas.
        if (rotation != 0 && currentHandle != Handle.MOVE) {
            double rad = Math.toRadians(-rotation);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            double rx = dx * cos - dy * sin;
            double ry = dx * sin + dy * cos;
            dx = (int) Math.round(rx);
            dy = (int) Math.round(ry);
        }

        Rectangle r = new Rectangle(dragStartBounds);
        int minS = constraints.minSize();

        switch (currentHandle) {
            case MOVE -> { r.x = snap(r.x + dx); r.y = snap(r.y + dy); }
            case NW -> { r.x = snap(r.x + dx); r.y = snap(r.y + dy); r.width -= dx; r.height -= dy; }
            case N -> { r.y = snap(r.y + dy); r.height -= dy; }
            case NE -> { r.y = snap(r.y + dy); r.width += dx; r.height -= dy; }
            case Handle.W -> { r.x = snap(r.x + dx); r.width -= dx; }
            case Handle.E -> { r.width += dx; }
            case SW -> { r.x = snap(r.x + dx); r.width -= dx; r.height += dy; }
            case S -> { r.height += dy; }
            case SE -> { r.width += dx; r.height += dy; }
            default -> {}
        }

        if (constraints.keepAspect() && esTiradorEsquina(currentHandle)) {
            applyAspectConstraint(r, dx, dy);
        } else {
            if (r.width < minS) {
                r.width = minS;
            }
            if (r.height < minS) {
                r.height = minS;
            }
        }

        return r;
    } // --- Fin del metodo drag ---


    /**
     * Finaliza el arrastre y resetea el estado interno.
     *
     * @return los bounds iniciales guardados (útil si se necesita el punto de
     *         partida)
     */
    public Rectangle endDrag() {
        currentHandle = Handle.NONE;
        return dragStartBounds;
    } // --- Fin del metodo endDrag ---


    /**
     * Ajusta un valor a la cuadrícula si snapGrid está activo.
     */
    private int snap(int value) {
        int grid = constraints.snapGrid();
        if (grid <= 1) return value;
        return Math.round((float) value / grid) * grid;
    } // --- Fin del metodo snap ---


    /**
     * Ajusta las dimensiones manteniendo la relaci\u00F3n de aspecto y ANCLANDO
     * la esquina opuesta al tirador arrastrado, para que la imagen no se
     * desplace mientras se redimensiona. Solo se aplica a los tiradores de
     * esquina (los laterales redimensionan libremente).
     */
    private void applyAspectConstraint(Rectangle r, int dx, int dy) {
        Rectangle s = dragStartBounds;
        if (s == null || s.width <= 0 || s.height <= 0) return;
        double aspect = (double) s.width / s.height;

        // Esquina fija (opuesta al tirador) y posici\u00F3n objetivo del tirador arrastrado.
        int anchorX = 0, anchorY = 0, targetX = 0, targetY = 0;
        switch (currentHandle) {
            case NW -> { anchorX = s.x + s.width; anchorY = s.y + s.height; targetX = s.x + dx; targetY = s.y + dy; }
            case NE -> { anchorX = s.x; anchorY = s.y + s.height; targetX = s.x + s.width + dx; targetY = s.y + dy; }
            case SW -> { anchorX = s.x + s.width; anchorY = s.y; targetX = s.x + dx; targetY = s.y + s.height + dy; }
            case SE -> { anchorX = s.x; anchorY = s.y; targetX = s.x + s.width + dx; targetY = s.y + s.height + dy; }
            default -> { return; }
        }

        int minS = constraints.minSize();
        int newW = Math.max(minS, Math.abs(targetX - anchorX));
        int newH = Math.max(minS, Math.abs(targetY - anchorY));

        if (newW / (double) newH > aspect) {
            newW = Math.max(minS, (int) Math.round(newH * aspect));
        } else {
            newH = Math.max(minS, (int) Math.round(newW / aspect));
        }

        r.x = snap(targetX < anchorX ? anchorX - newW : anchorX);
        r.y = snap(targetY < anchorY ? anchorY - newH : anchorY);
        r.width = newW;
        r.height = newH;
    } // --- Fin del metodo applyAspectConstraint ---


    /**
     * Indica si el tirador es de una esquina (no lateral ni de rotaci\u00F3n).
     */
    private boolean esTiradorEsquina(Handle h) {
        return h == Handle.NW || h == Handle.NE || h == Handle.SW || h == Handle.SE;
    } // --- Fin del metodo esTiradorEsquina ---


    /**
     * Devuelve el cursor adecuado para cada tirador.
     *
     * @param handle tirador activo
     * @return cursor de Swing correspondiente
     */
    public Cursor getCursor(Handle handle) {
        return switch (handle) {
            case Handle h when h == Handle.NONE || h == Handle.MOVE -> Cursor
                    .getPredefinedCursor(Cursor.MOVE_CURSOR);
            case Handle h when h == Handle.NW || h == Handle.SE -> Cursor
                    .getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
            case Handle h when h == Handle.N || h == Handle.S -> Cursor
                    .getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
            case Handle h when h == Handle.NE || h == Handle.SW -> Cursor
                    .getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
            case Handle h when h == Handle.W || h == Handle.E -> Cursor
                    .getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
            case Handle h when h == Handle.ROTATE -> Cursor
                    .getPredefinedCursor(Cursor.HAND_CURSOR);
            default -> Cursor.getDefaultCursor();
        };
    } // --- Fin del metodo getCursor ---


    /**
     * @return el Handle actual (el que se está arrastrando, o NONE)
     */
    public Handle getCurrentHandle() {
        return currentHandle;
    } // --- Fin del metodo getCurrentHandle ---


    /**
     * @param constraints nuevas restricciones (null para resetear a defaults)
     */
    public void setConstraints(Constraints constraints) {
        this.constraints = constraints != null ? constraints : new Constraints(false, 0, 10);
    } // --- Fin del metodo setConstraints ---


    /**
     * @return restricciones actuales
     */
    public Constraints getConstraints() {
        return constraints;
    } // --- Fin del metodo getConstraints ---


    /**
     * @param rotation ángulo de rotación acumulado (no implementado en drag)
     */
    public void setRotation(double rotation) {
        this.rotation = rotation;
    } // --- Fin del metodo setRotation ---


    /**
     * @return ángulo de rotación acumulado
     */
    public double getRotation() {
        return rotation;
    } // --- Fin del metodo getRotation ---

} // --- Fin de la clase TransformGizmo ---
