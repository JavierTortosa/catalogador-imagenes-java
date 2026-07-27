package controlador.actions.editoravanzado;

import controlador.commands.AppActionCommands;

public class FormasAction extends EditorToolAction {

    private static final long serialVersionUID = 1L;
    private static final String ICON_KEY = "80010-formas.png";
    private static final String TOOLTIP = "Crear formas geométricas básicas";


    public FormasAction() {
        super(AppActionCommands.CMD_ADVANCED_EDITOR_FORMAS, ICON_KEY, TOOLTIP);
    } // --- Fin del constructor FormasAction ---

} // --- Fin de la clase FormasAction ---
