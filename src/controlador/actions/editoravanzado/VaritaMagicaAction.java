package controlador.actions.editoravanzado;

import controlador.commands.AppActionCommands;

public class VaritaMagicaAction extends EditorToolAction {

    private static final long serialVersionUID = 1L;
    private static final String ICON_KEY = "80004-varita.png";
    private static final String TOOLTIP = "Varita mágica";


    public VaritaMagicaAction() {
        super(AppActionCommands.CMD_ADVANCED_EDITOR_VARITA, ICON_KEY, TOOLTIP);
    } // --- Fin del constructor VaritaMagicaAction ---

} // --- Fin de la clase VaritaMagicaAction ---
