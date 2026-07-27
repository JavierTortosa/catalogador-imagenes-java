package controlador.actions.editoravanzado;

import controlador.commands.AppActionCommands;

public class RecortarAction extends EditorToolAction {

    private static final long serialVersionUID = 1L;
    private static final String ICON_KEY = "80005.-recortar.png";
    private static final String TOOLTIP = "Recortar";


    public RecortarAction() {
        super(AppActionCommands.CMD_ADVANCED_EDITOR_RECORTAR, ICON_KEY, TOOLTIP);
    } // --- Fin del constructor RecortarAction ---

} // --- Fin de la clase RecortarAction ---
