package modelo.editor;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class LayerModel {

    private final List<Layer> layers;
    private final Set<Integer> selectedIndices;
    private int activeIndex;
    private final List<Runnable> changeListeners = new ArrayList<>();


    public LayerModel() {
        this.layers = new ArrayList<>();
        this.selectedIndices = new LinkedHashSet<>();
        this.activeIndex = -1;
    } // --- Fin del constructor LayerModel ---


    /**
     * Registra un callback que se invoca tras cada modificaci\u00F3n del modelo
     * (alta, baja, orden, selecci\u00F3n). La vista debe suscribirse aqu\u00ED.
     */
    public void setChangeListener(Runnable listener) {
        this.changeListeners.clear();
        if (listener != null) {
            this.changeListeners.add(listener);
        }
    } // --- Fin del metodo setChangeListener ---


    /**
     * A\u00F1ade un callback adicional de cambio sin reemplazar los existentes.
     * \u00DAtil para que el {@code EditorDocumentManager} marque el documento
     * como sucio adem\u00E1s de las suscripciones de la vista.
     */
    public void addChangeListener(Runnable listener) {
        if (listener != null && !this.changeListeners.contains(listener)) {
            this.changeListeners.add(listener);
        }
    } // --- Fin del metodo addChangeListener ---


    /**
     * Elimina un callback de cambio previamente registrado.
     *
     * @param listener el callback a eliminar
     */
    public void removeChangeListener(Runnable listener) {
        if (listener != null) {
            this.changeListeners.remove(listener);
        }
    } // --- Fin del metodo removeChangeListener ---


    private void fireChanged() {
        for (Runnable listener : this.changeListeners) {
            if (listener != null) {
                listener.run();
            }
        }
    } // --- Fin del metodo fireChanged ---


    /**
     * Notifica un cambio de propiedad de una capa (visibilidad, bloqueo, nombre)
     * para que la vista reconstruya las tarjetas y repinte el canvas.
     */
    public void refresh() {
        fireChanged();
    } // --- Fin del metodo refresh ---


    public void addLayer(Layer layer) {
        layers.add(Objects.requireNonNull(layer));
        if (activeIndex < 0) {
            activeIndex = 0;
        }
        fireChanged();
    } // --- Fin del metodo addLayer ---

    public void removeLayer(int index) {
        if (index < 0 || index >= layers.size()) return;
        layers.remove(index);
        adjustIndicesAfterRemove(index);
        if (activeIndex >= layers.size()) {
            activeIndex = layers.size() - 1;
        }
        if (layers.isEmpty()) {
            activeIndex = -1;
        }
        fireChanged();
    } // --- Fin del metodo removeLayer ---

    public void removeLayer(Layer layer) {
        int idx = layers.indexOf(layer);
        if (idx >= 0) {
            removeLayer(idx);
        }
    } // --- Fin del metodo removeLayer ---

    public Layer duplicateLayer(int index) {
        if (index < 0 || index >= layers.size()) return null;
        Layer original = layers.get(index);
        Layer copy = original.copy();
        copy.setName(original.getName() + " (copia)");
        layers.add(index + 1, copy);
        shiftIndicesAfterInsert(index + 1);
        fireChanged();
        return copy;
    } // --- Fin del metodo duplicateLayer ---

    public void moveLayer(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= layers.size()) return;
        if (toIndex < 0 || toIndex >= layers.size()) return;
        if (fromIndex == toIndex) return;
        Layer layer = layers.remove(fromIndex);
        layers.add(toIndex, layer);
        shiftSelectionOnMove(fromIndex, toIndex);
        if (activeIndex == fromIndex) {
            activeIndex = toIndex;
        }
        fireChanged();
    } // --- Fin del metodo moveLayer ---

    public Layer getActiveLayer() {
        if (activeIndex < 0 || activeIndex >= layers.size()) return null;
        return layers.get(activeIndex);
    } // --- Fin del metodo getActiveLayer ---

    public void setActiveLayer(int index) {
        if (index >= -1 && index < layers.size()) {
            this.activeIndex = index;
            fireChanged();
        }
    } // --- Fin del metodo setActiveLayer ---

    public void setActiveLayer(Layer layer) {
        int idx = layers.indexOf(layer);
        if (idx >= 0) {
            this.activeIndex = idx;
            fireChanged();
        }
    } // --- Fin del metodo setActiveLayer ---

    public int getActiveIndex() {
        return activeIndex;
    } // --- Fin del metodo getActiveIndex ---

    public List<Layer> getLayers() {
        return Collections.unmodifiableList(layers);
    } // --- Fin del metodo getLayers ---

    public int size() {
        return layers.size();
    } // --- Fin del metodo size ---

    public Layer getLayer(int index) {
        if (index < 0 || index >= layers.size()) return null;
        return layers.get(index);
    } // --- Fin del metodo getLayer ---

    public void clear() {
        layers.clear();
        selectedIndices.clear();
        activeIndex = -1;
        fireChanged();
    } // --- Fin del metodo clear ---


    /**
     * Reemplaza por completo el contenido del modelo (capas, selección y capa
     * activa) con el estado dado. Lo usa únicamente el historial de undo/redo
     * para restaurar un snapshot. Dispara {@code fireChanged()} una sola vez.
     *
     * @param nuevasCapas capas en el orden de apilado (z) que se adoptan tal cual
     * @param seleccion   índices seleccionados (puede ser {@code null})
     * @param activa      índice de la capa activa
     */
    public void restaurarEstado(List<Layer> nuevasCapas, Set<Integer> seleccion, int activa) {
        layers.clear();
        if (nuevasCapas != null) {
            layers.addAll(nuevasCapas);
        }
        selectedIndices.clear();
        if (seleccion != null) {
            selectedIndices.addAll(seleccion);
        }
        this.activeIndex = activa;
        fireChanged();
    } // --- Fin del metodo restaurarEstado ---


    // ==================== MULTISELECCIÓN ====================


    /**
     * Deja solo la capa indicada como seleccionada y activa.
     */
    public void setSingleSelection(int index) {
        if (index < 0 || index >= layers.size()) return;
        selectedIndices.clear();
        selectedIndices.add(index);
        activeIndex = index;
        fireChanged();
    } // --- Fin del metodo setSingleSelection ---


    /**
     * Añade o quita la capa de la selección múltiple y la deja como activa.
     */
    public void toggleSelected(int index) {
        if (index < 0 || index >= layers.size()) return;
        if (!selectedIndices.remove(index)) {
            selectedIndices.add(index);
        }
        activeIndex = index;
        fireChanged();
    } // --- Fin del metodo toggleSelected ---


    /**
     * Añade la capa a la selección múltiple sin quitar las ya seleccionadas y
     * la deja como activa. Es el comportamiento de Shift+clic de Photoshop.
     *
     * @param index índice de la capa a añadir
     */
    public void addSelection(int index) {
        if (index < 0 || index >= layers.size()) return;
        selectedIndices.add(index);
        activeIndex = index;
        fireChanged();
    } // --- Fin del metodo addSelection ---


    /**
     * Añade la capa a la selección múltiple sin quitar las ya seleccionadas y
     * la deja como activa.
     *
     * @param layer capa a añadir
     */
    public void addSelection(Layer layer) {
        if (layer == null) return;
        int idx = layers.indexOf(layer);
        if (idx >= 0) {
            addSelection(idx);
        }
    } // --- Fin del metodo addSelection ---


    /**
     * Reemplaza la selección múltiple por el conjunto de índices dado (como el
     * marco de selección de la herramienta de transformación). La capa activa
     * pasa a ser la seleccionada de mayor índice (la superior del panel). Si el
     * conjunto es null o vacío, se deja sin selección.
     *
     * @param indices índices a seleccionar
     */
    public void setSelectedIndices(Collection<Integer> indices) {
        selectedIndices.clear();
        if (indices != null) {
            for (int idx : indices) {
                if (idx >= 0 && idx < layers.size()) {
                    selectedIndices.add(idx);
                }
            }
        }
        activeIndex = selectedIndices.isEmpty()
                ? -1
                : Collections.max(selectedIndices);
        fireChanged();
    } // --- Fin del metodo setSelectedIndices ---


    /**
     * Selecciona el rango contiguo entre la capa activa (ancla) y la indicada,
     * como el Shift+clic de Photoshop. La capa pulsada pasa a ser la activa.
     */
    public void selectRange(int index) {
        if (index < 0 || index >= layers.size()) return;
        int anchor = (activeIndex >= 0) ? activeIndex : index;
        int from = Math.min(anchor, index);
        int to = Math.max(anchor, index);
        selectedIndices.clear();
        for (int i = from; i <= to; i++) {
            selectedIndices.add(i);
        }
        activeIndex = index;
        fireChanged();
    } // --- Fin del metodo selectRange ---


    public void clearSelection() {
        if (selectedIndices.isEmpty()) return;
        selectedIndices.clear();
        fireChanged();
    } // --- Fin del metodo clearSelection ---


    /**
     * Invierte la selección: quedan seleccionadas todas las capas que no lo
     * estaban y viceversa. Si la capa activa deja de estar seleccionada, pasa a
     * ser activa la capa seleccionada de mayor índice (la superior del panel).
     */
    public void invertSelection() {
        if (layers.isEmpty()) return;
        Set<Integer> prev = new LinkedHashSet<>(selectedIndices);
        selectedIndices.clear();
        for (int i = 0; i < layers.size(); i++) {
            if (!prev.contains(i)) {
                selectedIndices.add(i);
            }
        }
        if (!selectedIndices.contains(activeIndex)) {
            activeIndex = selectedIndices.isEmpty()
                    ? -1
                    : Collections.max(selectedIndices);
        }
        fireChanged();
    } // --- Fin del metodo invertSelection ---


    /**
     * Deselecciona todo: limpia la selección múltiple y deja sin capa activa,
     * disparando {@code fireChanged()} una sola vez. Es la operación completa
     * de "deseleccionar todas las capas" (a diferencia de {@link #clearSelection}
     * y {@link #setActiveLayer(int)} que solo limpian una de las dos cosas).
     */
    public void clearAllSelection() {
        boolean huboCambios = !selectedIndices.isEmpty() || activeIndex >= 0;
        selectedIndices.clear();
        activeIndex = -1;
        if (huboCambios) {
            fireChanged();
        }
    } // --- Fin del metodo clearAllSelection ---


    public boolean isSelected(int index) {
        return selectedIndices.contains(index);
    } // --- Fin del metodo isSelected ---


    public int getSelectedCount() {
        return selectedIndices.size();
    } // --- Fin del metodo getSelectedCount ---


    /**
     * Devuelve los índices seleccionados en orden ascendente (fondo primero).
     */
    public List<Integer> getSelectedIndices() {
        List<Integer> result = new ArrayList<>(new TreeSet<>(selectedIndices));
        return result;
    } // --- Fin del metodo getSelectedIndices ---


    /**
     * Ajusta los índices de selección tras eliminar la capa en {@code removed}.
     */
    private void adjustIndicesAfterRemove(int removed) {
        List<Integer> toRemove = new ArrayList<>();
        for (int idx : selectedIndices) {
            if (idx == removed) {
                toRemove.add(idx);
            }
        }
        for (int idx : toRemove) {
            selectedIndices.remove(idx);
        }
        Set<Integer> adjusted = new LinkedHashSet<>();
        for (int idx : selectedIndices) {
            adjusted.add(idx > removed ? idx - 1 : idx);
        }
        selectedIndices.clear();
        selectedIndices.addAll(adjusted);
    } // --- Fin del metodo adjustIndicesAfterRemove ---


    /**
     * Ajusta los índices de selección tras insertar una capa en {@code inserted}.
     */
    private void shiftIndicesAfterInsert(int inserted) {
        Set<Integer> adjusted = new LinkedHashSet<>();
        for (int idx : selectedIndices) {
            adjusted.add(idx >= inserted ? idx + 1 : idx);
        }
        selectedIndices.clear();
        selectedIndices.addAll(adjusted);
    } // --- Fin del metodo shiftIndicesAfterInsert ---


    /**
     * Recalcula los índices de selección tras mover la capa de {@code from} a {@code to}.
     */
    private void shiftSelectionOnMove(int from, int to) {
        if (selectedIndices.isEmpty()) return;
        Set<Integer> adjusted = new LinkedHashSet<>();
        for (int idx : selectedIndices) {
            int newIdx;
            if (idx == from) {
                newIdx = to;
            } else if (from < to) {
                newIdx = (idx > from && idx <= to) ? idx - 1 : idx;
            } else {
                newIdx = (idx >= to && idx < from) ? idx + 1 : idx;
            }
            adjusted.add(newIdx);
        }
        selectedIndices.clear();
        selectedIndices.addAll(adjusted);
    } // --- Fin del metodo shiftSelectionOnMove ---


    // ==================== COMBINAR CAPAS ====================


    /**
     * Fusiona las capas seleccionadas que son visibles y no están bloqueadas en
     * una única capa de imagen, con el área de la unión de sus bounds.
     * <p>
     * Las seleccionadas ocultas se descartan (como Photoshop). Las bloqueadas
     * quedan intactas. El resultado se coloca en la posición de la selección
     * más alta y adopta el nombre de la capa superior fusionada. Devuelve la
     * nueva capa, o {@code null} si no hay al menos dos capas fusionables.
     */
    public Layer mergeSelected() {
        List<Integer> selected = getSelectedIndices();
        if (selected.size() < 2) return null;

        List<Integer> toMerge = new ArrayList<>();
        List<Integer> toDiscard = new ArrayList<>();
        for (int idx : selected) {
            Layer l = layers.get(idx);
            if (!l.isVisible()) {
                toDiscard.add(idx);
            } else if (l.isLocked()) {
                // Bloqueada: no se fusiona ni se descarta, permanece
            } else {
                toMerge.add(idx);
            }
        }
        if (toMerge.size() < 2) return null;

        Rectangle union = null;
        for (int idx : toMerge) {
            Rectangle b = layers.get(idx).getBounds();
            if (b == null) return null;
            union = (union == null) ? new Rectangle(b) : union.union(b);
        }
        if (union == null || union.width <= 0 || union.height <= 0) return null;

        BufferedImage merged = new BufferedImage(union.width, union.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = merged.createGraphics();
        try {
            g.translate(-union.x, -union.y);
            for (int idx : toMerge) {
                layers.get(idx).paint(g);
            }
        } finally {
            g.dispose();
        }

        Layer top = layers.get(toMerge.get(toMerge.size() - 1));
        ImageLayer result = new ImageLayer(top.getName(), merged, union);
        result.setVisible(true);
        result.setLocked(false);
        result.setOpacity(1.0f);

        // El resultado se coloca donde estaba la capa superior de la selección,
        // preservando el orden relativo de las no fusionadas.
        int topIndex = toMerge.get(toMerge.size() - 1);
        Set<Integer> removed = new TreeSet<>();
        removed.addAll(toMerge);
        removed.addAll(toDiscard);
        List<Layer> newList = new ArrayList<>(layers.size());
        for (int i = 0; i < layers.size(); i++) {
            if (i == topIndex) {
                newList.add(result);
            }
            if (!removed.contains(i)) {
                newList.add(layers.get(i));
            }
        }
        layers.clear();
        layers.addAll(newList);

        selectedIndices.clear();
        activeIndex = layers.indexOf(result);
        fireChanged();
        return result;
    } // --- Fin del metodo mergeSelected ---

} // --- Fin de la clase LayerModel ---
