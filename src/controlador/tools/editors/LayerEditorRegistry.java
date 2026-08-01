package controlador.tools.editors;

import java.util.ArrayList;
import java.util.List;

import controlador.tools.ToolContext;
import modelo.editor.Layer;

/**
 * Registro de editores de capa por tipo.
 * <p>
 * Consulta en orden el primer {@link LayerEditor} que soporte la capa dada.
 * Los adapters se registran junto con las herramientas en el arranque del
 * editor.
 */
public class LayerEditorRegistry {

    private final List<LayerEditor> editors = new ArrayList<>();

    /**
     * Registra un editor de capa.
     */
    public void register(LayerEditor editor) {
        if (editor != null) {
            editors.add(editor);
        }
    } // --- Fin del metodo register ---

    /**
     * @return el primer editor que soporta la capa, o null si ninguno
     */
    public LayerEditor find(Layer layer) {
        if (layer == null) return null;
        for (LayerEditor e : editors) {
            if (e.supports(layer)) return e;
        }
        return null;
    } // --- Fin del metodo find ---

    /**
     * Delega el doble clic al editor del tipo de la capa.
     *
     * @return true si algún editor atendió el doble clic
     */
    public boolean handleDoubleClick(Layer layer, ToolContext ctx) {
        LayerEditor e = find(layer);
        if (e == null) return false;
        e.onDoubleClick(layer, ctx);
        return true;
    } // --- Fin del metodo handleDoubleClick ---

} // --- Fin de la clase LayerEditorRegistry ---
