package controlador.actions.editoravanzado;

import controlador.commands.AppActionCommands;

public class TextoAction extends EditorToolAction {

    private static final long serialVersionUID = 1L;
    private static final String ICON_KEY = "80009-text.png";
    private static final String TOOLTIP = "Crear una nueva capa de texto";


    public TextoAction() {
        super(AppActionCommands.CMD_ADVANCED_EDITOR_TEXTO, ICON_KEY, TOOLTIP);
    } // --- Fin del constructor TextoAction ---

} // --- Fin de la clase TextoAction ---
