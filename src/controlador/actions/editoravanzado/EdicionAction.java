package controlador.actions.editoravanzado;

import controlador.commands.AppActionCommands;

public class EdicionAction extends EditorToolAction {

    private static final long serialVersionUID = 1L;
    private static final String ICON_KEY = "80000-edicion.png";
    private static final String TOOLTIP = "Edicion";


    public EdicionAction() {
        super(AppActionCommands.CMD_ADVANCED_EDITOR_EDICION, ICON_KEY, TOOLTIP);
    } // --- Fin del constructor EdicionAction ---

} // --- Fin de la clase EdicionAction ---
