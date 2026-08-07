package modelo.editor.smartguides;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor de Smart Guides: matemática pura de ajuste de capas durante el arrastre.
 * <p>
 * Es una sesión creada en el {@code mousePressed} del arrastre de mover y
 * destruida en el {@code mouseReleased}. En {@link #begin} se cachean los bounds
 * de las capas estáticas y del lienzo para no recalcularlos en cada evento.
 * <p>
 * No accede a singletons ni a la UI: la configuración se inyecta en
 * {@link #update}. Mantiene estado de histéresis (guías "retenidas") para que,
 * una vez producido el ajuste, la guía no parpadee mientras el cursor se aleja
 * dentro de la distancia de retención.
 */
public final class SmartGuidesEngine {

    /**
     * Resultado de la corrección sobre un eje (X o Y).
     *
     * @param delta corrección a sumar al desplazamiento crudo
     * @param held posición absoluta de la guía retenida, o null si no hay ajuste
     */
    private record AxisSnap(int delta, Integer held) {
    }

    private Rectangle canvasRect;
    private List<Rectangle> staticBounds = new ArrayList<>();
    private List<Rectangle> movingStartBounds = new ArrayList<>();
    private Integer heldX;
    private Integer heldY;

    /**
     * Inicializa la sesión de ajuste cacheando el rectángulo del lienzo, los
     * bounds de las capas estáticas y los bounds de partida de las capas en
     * movimiento.
     *
     * @param canvasRect rectángulo del lienzo (0,0,w,h)
     * @param staticBounds bounds de las capas estáticas candidatas a alinear
     * @param movingBounds bounds de partida de las capas que se arrastran
     */
    public void begin(Rectangle canvasRect, List<Rectangle> staticBounds, List<Rectangle> movingBounds) {
        this.canvasRect = canvasRect;
        this.staticBounds = new ArrayList<>(staticBounds);
        this.movingStartBounds = new ArrayList<>(movingBounds);
        this.heldX = null;
        this.heldY = null;
    } // --- Fin del metodo begin ---


    /**
     * Calcula el desplazamiento corregido y las guías activas para el desplazamiento
     * crudo {@code (dx, dy)} acumulado desde el punto de pulsación.
     *
     * @param dx desplazamiento X crudo acumulado
     * @param dy desplazamiento Y crudo acumulado
     * @param cfg configuración activa de Smart Guides
     * @return resultado con los desplazamientos corregidos y las guías a dibujar
     */
    public SnapResult update(int dx, int dy, SmartGuidesConfig cfg) {
        if (!cfg.isShowGuides() || canvasRect == null || movingStartBounds.isEmpty()) {
            heldX = null;
            heldY = null;
            return new SnapResult(dx, dy, List.of());
        }

        Rectangle union = unionAt(dx, dy);
        if (union == null) {
            return new SnapResult(dx, dy, List.of());
        }

        List<GuideLine> guides = new ArrayList<>();

        AxisSnap sx = snapAxis(edgesX(union), candidatesX(cfg), heldX, cfg);
        int correctedDx = dx + sx.delta();
        heldX = sx.held();
        if (heldX != null) {
            guides.add(new GuideLine(true, heldX, 0, canvasRect.height));
        }

        AxisSnap sy = snapAxis(edgesY(union), candidatesY(cfg), heldY, cfg);
        int correctedDy = dy + sy.delta();
        heldY = sy.held();
        if (heldY != null) {
            guides.add(new GuideLine(false, heldY, 0, canvasRect.width));
        }

        return new SnapResult(correctedDx, correctedDy, guides);
    } // --- Fin del metodo update ---


    /**
     * Libera el estado de histéresis al terminar el arrastre.
     */
    public void end() {
        heldX = null;
        heldY = null;
        canvasRect = null;
        staticBounds.clear();
        movingStartBounds.clear();
    } // --- Fin del metodo end ---


    /**
     * Calcula la corrección de ajuste sobre un eje aplicando snap y histéresis.
     *
     * @param draggedEdges bordes del rectángulo en movimiento en el eje
     * @param candidates posiciones candidatas (capas estáticas y lienzo)
     * @param held guía retenida del evento anterior, o null
     * @param cfg configuración activa
     * @return corrección y guía resultante
     */
    private AxisSnap snapAxis(List<Integer> draggedEdges, List<Integer> candidates, Integer held,
            SmartGuidesConfig cfg) {
        boolean enabled = cfg.isSnapToCanvas() || cfg.isSnapToLayers();
        if (!enabled || draggedEdges.isEmpty()) {
            return new AxisSnap(0, null);
        }

        int bestDiff = Integer.MAX_VALUE;
        int bestCandidate = 0;
        int bestEdge = 0;
        for (int edge : draggedEdges) {
            for (int cand : candidates) {
                int diff = Math.abs(cand - edge);
                if (diff <= cfg.getSnapDistance() && diff < bestDiff) {
                    bestDiff = diff;
                    bestCandidate = cand;
                    bestEdge = edge;
                }
            }
        }

        if (held != null) {
            int minHeldDiff = Integer.MAX_VALUE;
            int heldEdge = draggedEdges.get(0);
            for (int edge : draggedEdges) {
                int diff = Math.abs(held - edge);
                if (diff < minHeldDiff) {
                    minHeldDiff = diff;
                    heldEdge = edge;
                }
            }
            if (minHeldDiff <= cfg.getStickyDistance()) {
                return new AxisSnap(held - heldEdge, held);
            }
        }

        if (bestDiff <= cfg.getSnapDistance()) {
            return new AxisSnap(bestCandidate - bestEdge, bestCandidate);
        }
        return new AxisSnap(0, null);
    } // --- Fin del metodo snapAxis ---


    /**
     * Posiciones X candidatas: bordes y centros de capas estáticas y del lienzo.
     *
     * @param cfg configuración activa
     * @return lista de posiciones candidatas en el eje X
     */
    private List<Integer> candidatesX(SmartGuidesConfig cfg) {
        List<Integer> result = new ArrayList<>();
        if (cfg.isSnapToLayers()) {
            for (Rectangle b : staticBounds) {
                result.add(b.x);
                result.add(b.x + b.width / 2);
                result.add(b.x + b.width);
            }
        }
        if (cfg.isSnapToCanvas()) {
            result.add(0);
            result.add(canvasRect.width / 2);
            result.add(canvasRect.width);
        }
        return result;
    } // --- Fin del metodo candidatesX ---


    /**
     * Posiciones Y candidatas: bordes y centros de capas estáticas y del lienzo.
     *
     * @param cfg configuración activa
     * @return lista de posiciones candidatas en el eje Y
     */
    private List<Integer> candidatesY(SmartGuidesConfig cfg) {
        List<Integer> result = new ArrayList<>();
        if (cfg.isSnapToLayers()) {
            for (Rectangle b : staticBounds) {
                result.add(b.y);
                result.add(b.y + b.height / 2);
                result.add(b.y + b.height);
            }
        }
        if (cfg.isSnapToCanvas()) {
            result.add(0);
            result.add(canvasRect.height / 2);
            result.add(canvasRect.height);
        }
        return result;
    } // --- Fin del metodo candidatesY ---


    /**
     * Bordes X (izquierda, centro, derecha) de un rectángulo.
     *
     * @param r rectángulo
     * @return lista de posiciones
     */
    private static List<Integer> edgesX(Rectangle r) {
        return List.of(r.x, r.x + r.width / 2, r.x + r.width);
    } // --- Fin del metodo edgesX ---


    /**
     * Bordes Y (superior, centro, inferior) de un rectángulo.
     *
     * @param r rectángulo
     * @return lista de posiciones
     */
    private static List<Integer> edgesY(Rectangle r) {
        return List.of(r.y, r.y + r.height / 2, r.y + r.height);
    } // --- Fin del metodo edgesY ---


    /**
     * Calcula la unión de las capas en movimiento tras aplicar el desplazamiento
     * crudo acumulado.
     *
     * @param dx desplazamiento X crudo
     * @param dy desplazamiento Y crudo
     * @return rectángulo unión, o null si no hay capas
     */
    private Rectangle unionAt(int dx, int dy) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Rectangle b : movingStartBounds) {
            minX = Math.min(minX, b.x + dx);
            minY = Math.min(minY, b.y + dy);
            maxX = Math.max(maxX, b.x + b.width + dx);
            maxY = Math.max(maxY, b.y + b.height + dy);
        }
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    } // --- Fin del metodo unionAt ---

} // --- Fin de la clase SmartGuidesEngine ---
