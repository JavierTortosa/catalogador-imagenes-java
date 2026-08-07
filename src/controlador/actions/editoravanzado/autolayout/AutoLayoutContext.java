package controlador.actions.editoravanzado.autolayout;

import modelo.editor.CanvasModel;
import modelo.editor.LayerModel;
import vista.panels.render.CanvasPanel;

/**
 * Contexto inmutable que recibe un algoritmo Auto Layout.
 * <p>
 * Agrupa el modelo de capas, el canvas, el modelo del lienzo y la configuración
 * del sistema. Los algoritmos obtienen toda la información que necesitan
 * exclusivamente a través de este objeto (incluida la config mediante
 * {@link #getConfig()}), sin conocer singletons ni estáticos.
 */
public final class AutoLayoutContext {

    private final CanvasPanel canvas;
    private final LayerModel layerModel;
    private final CanvasModel canvasModel;
    private final AutoLayoutConfig config;


    /**
     * Crea un contexto Auto Layout.
     *
     * @param canvas      panel del lienzo
     * @param layerModel  modelo de capas del documento
     * @param canvasModel modelo de dimensiones del lienzo
     * @param config      configuración del sistema Auto Layout
     */
    public AutoLayoutContext(CanvasPanel canvas, LayerModel layerModel,
            CanvasModel canvasModel, AutoLayoutConfig config) {
        this.canvas = canvas;
        this.layerModel = layerModel;
        this.canvasModel = canvasModel;
        this.config = config;
    } // --- Fin del constructor AutoLayoutContext ---


    public CanvasPanel getCanvas() {
        return canvas;
    } // --- Fin del metodo getCanvas ---


    public LayerModel getLayerModel() {
        return layerModel;
    } // --- Fin del metodo getLayerModel ---


    public CanvasModel getCanvasModel() {
        return canvasModel;
    } // --- Fin del metodo getCanvasModel ---


    public AutoLayoutConfig getConfig() {
        return config;
    } // --- Fin del metodo getConfig ---

} // --- Fin de la clase AutoLayoutContext ---
