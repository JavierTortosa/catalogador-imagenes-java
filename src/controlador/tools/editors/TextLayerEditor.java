package controlador.tools.editors;

import controlador.commands.AppActionCommands;
import controlador.tools.CanvasController;
import controlador.tools.TextTool;
import controlador.tools.ToolContext;
import modelo.editor.Layer;
import modelo.editor.TextLayer;

/**
 * Editor de capas de texto.
 * <p>
 * El doble clic sobre una TextLayer (desde EditTool) activa la herramienta de
 * texto y abre la edición inline existente.
 */
public class TextLayerEditor implements LayerEditor {

    private final TextTool textTool;

    /**
     * @param textTool herramienta de texto que se encarga de la edición inline
     */
    public TextLayerEditor(TextTool textTool) {
        this.textTool = textTool;
    } // --- Fin del constructor TextLayerEditor ---

    @Override
    public boolean supports(Layer layer) {
        return layer instanceof TextLayer;
    } // --- Fin del metodo supports ---

    @Override
    public void onDoubleClick(Layer layer, ToolContext ctx) {
        ctx.layerModel().setActiveLayer(layer);
        CanvasController cc = ctx.componentBar().getCanvasController();
        if (cc == null) return;
        cc.setActiveTool(AppActionCommands.CMD_ADVANCED_EDITOR_TEXTO);
        textTool.editExistingLayer((TextLayer) layer);
    } // --- Fin del metodo onDoubleClick ---

} // --- Fin de la clase TextLayerEditor ---
