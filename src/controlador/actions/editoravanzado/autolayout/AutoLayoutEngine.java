package controlador.actions.editoravanzado.autolayout;

import modelo.editor.LayerModel;
import vista.panels.render.CanvasPanel;

/**
 * Orquestador del sistema Auto Layout.
 * <p>
 * Construye el {@link AutoLayoutContext} (inyectando la configuración),
 * invoca {@link AutoLayoutAlgorithm#layout(AutoLayoutContext)} sobre el
 * algoritmo solicitado y repinta el lienzo. El historial de Undo/Redo y el
 * resto de la integración con la UI corresponden a la capa que invoca al motor.
 */
public final class AutoLayoutEngine {

    private AutoLayoutEngine() {
    } // --- Fin del constructor AutoLayoutEngine ---


    /**
     * Aplica un algoritmo Auto Layout sobre el modelo de capas dado.
     *
     * @param canvas    panel del lienzo
     * @param layerModel modelo de capas del documento
     * @param algoritmo  algoritmo a ejecutar (nunca {@code null})
     */
    public static void aplicar(CanvasPanel canvas, LayerModel layerModel, AutoLayoutAlgorithm algoritmo) {
        if (canvas == null || layerModel == null || algoritmo == null) {
            return;
        }
        AutoLayoutContext ctx = new AutoLayoutContext(
                canvas, layerModel, canvas.getCanvasModel(), AutoLayoutConfig.get());
        algoritmo.layout(ctx);
        canvas.repaint();
    } // --- Fin del metodo aplicar ---

} // --- Fin de la clase AutoLayoutEngine ---
