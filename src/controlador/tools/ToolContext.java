package controlador.tools;

import modelo.editor.CanvasModel;
import modelo.editor.LayerModel;
import modelo.editor.SelectionModel;
import modelo.gizmo.TransformGizmo;
import vista.panels.render.CanvasPanel;
import vista.panels.render.EditorComponentBar;

/**
 * Contexto compartido inyectado en cada Tool.
 * <p>
 * Agrupa las referencias al modelo, la vista y la barra de opciones para que
 * las herramientas puedan acceder a todo sin acoplarse a componentes concretos.
 * También expone los servicios compartidos {@link LayerPicker} y
 * {@link controlador.tools.editors.LayerEditorRegistry}.
 */
public record ToolContext(
        CanvasModel canvasModel,
        LayerModel layerModel,
        SelectionModel selectionModel,
        TransformGizmo gizmo,
        CanvasPanel canvasPanel,
        EditorComponentBar componentBar,
        LayerPicker layerPicker,
        controlador.tools.editors.LayerEditorRegistry layerEditorRegistry) {

    /**
     * @return true si todos los modelos esenciales están inicializados
     */
    public boolean isValid() {
        return canvasModel != null && layerModel != null && selectionModel != null;
    } // --- Fin del metodo isValid ---

} // --- Fin de la clase ToolContext ---
