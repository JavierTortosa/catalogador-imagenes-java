package controlador.actions.editoravanzado;

import controlador.commands.AppActionCommands;

public class DegradadoAction extends EditorToolAction {

    private static final long serialVersionUID = 1L;
    private static final String ICON_KEY = "80008-degradado.png";
    private static final String TOOLTIP = "Rellenar mediante un degradado";


    public DegradadoAction() {
        super(AppActionCommands.CMD_ADVANCED_EDITOR_DEGRADADO, ICON_KEY, TOOLTIP);
    } // --- Fin del constructor DegradadoAction ---

} // --- Fin de la clase DegradadoAction ---
