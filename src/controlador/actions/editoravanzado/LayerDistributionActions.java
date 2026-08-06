package controlador.actions.editoravanzado;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import modelo.editor.CanvasModel;
import modelo.editor.Layer;
import modelo.editor.LayerModel;
import vista.panels.render.CanvasPanel;

/**
 * Operaciones de alineación, distribución y espaciado de capas del editor.
 * <p>
 * Comparten la semántica decidida para los botones del panel Herramientas y
 * los combos de la barra superior:
 * <ul>
 * <li><strong>Alinear:</strong> respecto al bounding box de las capas
 * seleccionadas (unión); con una sola capa, respecto al lienzo.</li>
 * <li><strong>Distribuir:</strong> entre los bordes del bounding box de la
 * selección, repartiendo el rango a intervalos iguales.</li>
 * <li><strong>Espaciar:</strong> hueco uniforme entre capas adyacentes, sin
 * mover la primera ni la última.</li>
 * </ul>
 */
public final class LayerDistributionActions {

    // --- Modos de alineación horizontal ---

    public static final int ALIGN_NONE = -1;
    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_HCENTER = 1;
    public static final int ALIGN_RIGHT = 2;

    // --- Modos de alineación vertical ---

    public static final int ALIGN_TOP = 0;
    public static final int ALIGN_VCENTER = 1;
    public static final int ALIGN_BOTTOM = 2;

    // --- Modos de distribución ---

    public static final int DIST_TOP_BORDER = 0;
    public static final int DIST_CENTER_VERTICAL = 1;
    public static final int DIST_BOTTOM_BORDER = 2;
    public static final int DIST_LEFT_BORDER = 3;
    public static final int DIST_CENTER_HORIZONTAL = 4;
    public static final int DIST_RIGHT_BORDER = 5;

    private LayerDistributionActions() {
    } // --- Fin del constructor LayerDistributionActions ---


    /**
     * Alinea las capas objetivo según la combinación de modos horizontal y
     * vertical. Los modos {@link #ALIGN_NONE} dejan la coordenada intacta.
     *
     * @param canvas     lienzo del editor (para repintar)
     * @param layerModel modelo de capas
     * @param canvasModel modelo del lienzo
     * @param hMode      modo horizontal (ALIGN_LEFT / ALIGN_HCENTER / ALIGN_RIGHT / ALIGN_NONE)
     * @param vMode      modo vertical (ALIGN_TOP / ALIGN_VCENTER / ALIGN_BOTTOM / ALIGN_NONE)
     */
    public static void alinear(CanvasPanel canvas, LayerModel layerModel, CanvasModel canvasModel, int hMode, int vMode) {
        List<Layer> targets = getAlignTargets(layerModel);
        if (targets.isEmpty()) return;

        Rectangle ref = computeReference(targets, layerModel, canvasModel);

        for (Layer layer : targets) {
            Rectangle b = layer.getBounds();
            if (b == null) continue;
            int nx = b.x;
            int ny = b.y;
            if (hMode == ALIGN_LEFT) {
                nx = ref.x;
            } else if (hMode == ALIGN_HCENTER) {
                nx = ref.x + (ref.width - b.width) / 2;
            } else if (hMode == ALIGN_RIGHT) {
                nx = ref.x + ref.width - b.width;
            }
            if (vMode == ALIGN_TOP) {
                ny = ref.y;
            } else if (vMode == ALIGN_VCENTER) {
                ny = ref.y + (ref.height - b.height) / 2;
            } else if (vMode == ALIGN_BOTTOM) {
                ny = ref.y + ref.height - b.height;
            }
            layer.setBounds(new Rectangle(nx, ny, b.width, b.height));
        }

        canvas.repaint();
    } // --- Fin del metodo alinear ---


    /**
     * Distribuye las capas objetivo a intervalos iguales. La primera y la
     * última quedan fijas en sus posiciones (sus valores son los extremos del
     * rango) y las intermedias se reparten a intervalos iguales entre ellos.
     * <p>
     * El rango se calcula sobre la propia magnitud repartida (borde, centro o
     * borde opuesto según el modo), no sobre el bounding box de la selección:
     * así la operación es idempotente y no dispersa las capas al repetirla.
     *
     * @param canvas     lienzo del editor (para repintar)
     * @param layerModel modelo de capas
     * @param mode       uno de los modos DIST_*
     */
    public static void distribuir(CanvasPanel canvas, LayerModel layerModel, int mode) {
        List<Layer> targets = getDistributionTargets(layerModel);
        if (targets.size() < 2) return;

        targets.sort((a, b) -> Integer.compare(valueOf(a.getBounds(), mode), valueOf(b.getBounds(), mode)));

        int n = targets.size();
        double refStart = valueOf(targets.get(0).getBounds(), mode);
        double refEnd = valueOf(targets.get(n - 1).getBounds(), mode);
        double step = (refEnd - refStart) / (n - 1);

        for (int i = 0; i < n; i++) {
            Layer layer = targets.get(i);
            Rectangle b = layer.getBounds();
            double pos = refStart + step * i;
            int nx = b.x;
            int ny = b.y;
            if (mode <= DIST_BOTTOM_BORDER) {
                ny = positionY(b, mode, pos);
            } else {
                nx = positionX(b, mode, pos);
            }
            layer.setBounds(new Rectangle(nx, ny, b.width, b.height));
        }

        canvas.repaint();
    } // --- Fin del metodo distribuir ---


    /**
     * Iguala el hueco entre capas consecutivas sin mover la primera ni la
     * última (estilo Photoshop "Distribute Spacing").
     *
     * @param canvas     lienzo del editor (para repintar)
     * @param layerModel modelo de capas
     * @param horizontal {@code true} hueco horizontal (eje X), {@code false} vertical (eje Y)
     */
    public static void espaciar(CanvasPanel canvas, LayerModel layerModel, boolean horizontal) {
        List<Layer> targets = getDistributionTargets(layerModel);
        if (targets.size() < 2) return;

        if (horizontal) {
            targets.sort((a, b) -> Integer.compare(a.getBounds().x, b.getBounds().x));
        } else {
            targets.sort((a, b) -> Integer.compare(a.getBounds().y, b.getBounds().y));
        }

        Layer first = targets.get(0);
        Layer last = targets.get(targets.size() - 1);

        int start = horizontal ? first.getBounds().x : first.getBounds().y;
        int end = horizontal ? last.getBounds().x + last.getBounds().width
                : last.getBounds().y + last.getBounds().height;
        int total = targets.stream().mapToInt(l -> horizontal ? l.getBounds().width : l.getBounds().height).sum();
        int gaps = targets.size() - 1;
        double gap = Math.max(0, (double) (end - start - total) / gaps);

        double cursor = start;
        for (Layer layer : targets) {
            Rectangle b = layer.getBounds();
            int pos = (int) Math.round(cursor);
            if (horizontal) {
                layer.setBounds(new Rectangle(pos, b.y, b.width, b.height));
            } else {
                layer.setBounds(new Rectangle(b.x, pos, b.width, b.height));
            }
            cursor += (horizontal ? b.width : b.height) + gap;
        }

        canvas.repaint();
    } // --- Fin del metodo espaciar ---


    /**
     * Capas objetivo de alineación: la selección si hay varias; si no, solo la
     * capa activa (o la única seleccionada).
     */
    private static List<Layer> getAlignTargets(LayerModel layerModel) {
        if (layerModel == null) return Collections.emptyList();

        List<Layer> selected = getSelectedVisibleUnlocked(layerModel);
        if (selected.size() > 1) return selected;

        Layer single = layerModel.getActiveLayer();
        if (single == null && !selected.isEmpty()) {
            single = selected.get(0);
        }
        if (single == null || !single.isVisible() || single.isLocked()) {
            return Collections.emptyList();
        }
        return List.of(single);
    } // --- Fin del metodo getAlignTargets ---


    /**
     * Capas objetivo de distribución/espaciado: la selección si hay dos o más;
     * si no, todas las visibles y no bloqueadas.
     */
    private static List<Layer> getDistributionTargets(LayerModel layerModel) {
        if (layerModel == null) return Collections.emptyList();

        List<Layer> selected = getSelectedVisibleUnlocked(layerModel);
        if (selected.size() >= 2) return selected;

        return layerModel.getLayers().stream()
                .filter(Layer::isVisible)
                .filter(layer -> !layer.isLocked())
                .collect(Collectors.toList());
    } // --- Fin del metodo getDistributionTargets ---


    /**
     * Número de capas efectivas para una operación de distribución: la selección
     * si hay dos o más seleccionadas; si no, todas las visibles y no bloqueadas.
     * Sirve para habilitar/deshabilitar los botones (con menos de 3 no hay nada
     * que repartir).
     *
     * @param layerModel modelo de capas activo
     * @return número de capas objetivo (0 si el modelo es {@code null})
     */
    public static int countDistributionTargets(LayerModel layerModel) {
        if (layerModel == null) return 0;

        List<Layer> selected = getSelectedVisibleUnlocked(layerModel);
        if (selected.size() >= 2) return selected.size();

        return (int) layerModel.getLayers().stream()
                .filter(Layer::isVisible)
                .filter(layer -> !layer.isLocked())
                .count();
    } // --- Fin del metodo countDistributionTargets ---


    private static List<Layer> getSelectedVisibleUnlocked(LayerModel layerModel) {
        List<Layer> result = new ArrayList<>();
        for (int idx : layerModel.getSelectedIndices()) {
            Layer layer = layerModel.getLayer(idx);
            if (layer != null && layer.isVisible() && !layer.isLocked()) {
                result.add(layer);
            }
        }
        return result;
    } // --- Fin del metodo getSelectedVisibleUnlocked ---


    /**
     * Rectángulo de referencia: unión de las capas objetivo si hay selección
     * múltiple; si no, el lienzo completo.
     */
    private static Rectangle computeReference(List<Layer> targets, LayerModel layerModel, CanvasModel canvasModel) {
        if (layerModel.getSelectedCount() > 1) {
            Rectangle ref = null;
            for (Layer layer : targets) {
                Rectangle b = layer.getBounds();
                if (b == null) continue;
                ref = (ref == null) ? new Rectangle(b) : ref.union(b);
            }
            if (ref != null) return ref;
        }
        return new Rectangle(0, 0, canvasModel.getWidth(), canvasModel.getHeight());
    } // --- Fin del metodo computeReference ---


    private static int valueOf(Rectangle b, int mode) {
        return switch (mode) {
            case DIST_TOP_BORDER -> b.y;
            case DIST_CENTER_VERTICAL -> b.y + b.height / 2;
            case DIST_BOTTOM_BORDER -> b.y + b.height;
            case DIST_LEFT_BORDER -> b.x;
            case DIST_CENTER_HORIZONTAL -> b.x + b.width / 2;
            default -> b.x + b.width;
        };
    } // --- Fin del metodo valueOf ---


    private static int positionY(Rectangle b, int mode, double pos) {
        return switch (mode) {
            case DIST_TOP_BORDER -> (int) Math.round(pos);
            case DIST_CENTER_VERTICAL -> (int) Math.round(pos - b.height / 2.0);
            default -> (int) Math.round(pos - b.height);
        };
    } // --- Fin del metodo positionY ---


    private static int positionX(Rectangle b, int mode, double pos) {
        return switch (mode) {
            case DIST_LEFT_BORDER -> (int) Math.round(pos);
            case DIST_CENTER_HORIZONTAL -> (int) Math.round(pos - b.width / 2.0);
            default -> (int) Math.round(pos - b.width);
        };
    } // --- Fin del metodo positionX ---

} // --- Fin de la clase LayerDistributionActions ---
