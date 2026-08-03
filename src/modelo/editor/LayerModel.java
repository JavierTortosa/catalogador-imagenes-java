package modelo.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class LayerModel {

    private final List<Layer> layers;
    private int activeIndex;
    private Runnable changeListener;


    public LayerModel() {
        this.layers = new ArrayList<>();
        this.activeIndex = -1;
    } // --- Fin del constructor LayerModel ---


    /**
     * Registra un callback que se invoca tras cada modificaci\u00F3n del modelo
     * (alta, baja, orden, selecci\u00F3n). La vista debe suscribirse aqu\u00ED.
     */
    public void setChangeListener(Runnable listener) {
        this.changeListener = listener;
    } // --- Fin del metodo setChangeListener ---


    private void fireChanged() {
        if (changeListener != null) {
            changeListener.run();
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
        fireChanged();
        return copy;
    } // --- Fin del metodo duplicateLayer ---

    public void moveLayer(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= layers.size()) return;
        if (toIndex < 0 || toIndex >= layers.size()) return;
        if (fromIndex == toIndex) return;
        Layer layer = layers.remove(fromIndex);
        layers.add(toIndex, layer);
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
        activeIndex = -1;
        fireChanged();
    } // --- Fin del metodo clear ---

} // --- Fin de la clase LayerModel ---
