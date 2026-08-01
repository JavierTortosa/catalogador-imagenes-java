package controlador.tools.editors;

import controlador.tools.ToolContext;
import modelo.editor.Layer;

/**
 * Editor específico de un tipo de capa.
 * <p>
 * Cada implementación sabe si atiende a un tipo de capa ({@link #supports}) y
 * cómo abrir su edición al hacer doble clic sobre ella. Es el equivalente, para
 * la edición, de lo que {@code Layer.paint} es para el pintado: el orquestador
 * (EditTool) consulta el registro y delega, sin usar if-cadenas por tipo.
 */
public interface LayerEditor {

    /**
     * @param layer capa candidata
     * @return true si este editor atiende a ese tipo de capa
     */
    boolean supports(Layer layer);

    /**
     * Abre la edición del contenido de la capa (texto inline, panel de formas,
     * etc.). El caller ya ha activado la capa si corresponde.
     *
     * @param layer capa sobre la que se hace doble clic
     * @param ctx   contexto compartido
     */
    void onDoubleClick(Layer layer, ToolContext ctx);

} // --- Fin de la interfaz LayerEditor ---
