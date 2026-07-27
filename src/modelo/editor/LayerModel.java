package modelo.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class LayerModel {

    private final List<ImageLayer> layers;
    private int activeIndex;

    public LayerModel() {
        this.layers = new ArrayList<>();
        this.activeIndex = -1;
    } // --- Fin del constructor LayerModel ---

    public void addLayer(ImageLayer layer) {
        layers.add(Objects.requireNonNull(layer));
        if (activeIndex < 0) {
            activeIndex = 0;
        }
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
    } // --- Fin del metodo removeLayer ---

    public void removeLayer(ImageLayer layer) {
        int idx = layers.indexOf(layer);
        if (idx >= 0) {
            removeLayer(idx);
        }
    } // --- Fin del metodo removeLayer ---

    public ImageLayer duplicateLayer(int index) {
        if (index < 0 || index >= layers.size()) return null;
        ImageLayer original = layers.get(index);
        ImageLayer copy = original.copy();
        copy.setName(original.getName() + " (copia)");
        layers.add(index + 1, copy);
        return copy;
    } // --- Fin del metodo duplicateLayer ---

    public void moveLayer(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= layers.size()) return;
        if (toIndex < 0 || toIndex >= layers.size()) return;
        if (fromIndex == toIndex) return;
        ImageLayer layer = layers.remove(fromIndex);
        layers.add(toIndex, layer);
        if (activeIndex == fromIndex) {
            activeIndex = toIndex;
        }
    } // --- Fin del metodo moveLayer ---

    public ImageLayer getActiveLayer() {
        if (activeIndex < 0 || activeIndex >= layers.size()) return null;
        return layers.get(activeIndex);
    } // --- Fin del metodo getActiveLayer ---

    public void setActiveLayer(int index) {
        if (index >= -1 && index < layers.size()) {
            this.activeIndex = index;
        }
    } // --- Fin del metodo setActiveLayer ---

    public void setActiveLayer(ImageLayer layer) {
        int idx = layers.indexOf(layer);
        if (idx >= 0) {
            this.activeIndex = idx;
        }
    } // --- Fin del metodo setActiveLayer ---

    public int getActiveIndex() {
        return activeIndex;
    } // --- Fin del metodo getActiveIndex ---

    public List<ImageLayer> getLayers() {
        return Collections.unmodifiableList(layers);
    } // --- Fin del metodo getLayers ---

    public int size() {
        return layers.size();
    } // --- Fin del metodo size ---

    public ImageLayer getLayer(int index) {
        if (index < 0 || index >= layers.size()) return null;
        return layers.get(index);
    } // --- Fin del metodo getLayer ---

    public void clear() {
        layers.clear();
        activeIndex = -1;
    } // --- Fin del metodo clear ---

} // --- Fin de la clase LayerModel ---
