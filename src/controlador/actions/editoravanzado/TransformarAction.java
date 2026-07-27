package controlador.actions.editoravanzado;

import controlador.commands.AppActionCommands;

public class TransformarAction extends EditorToolAction {

    private static final long serialVersionUID = 1L;
    private static final String ICON_KEY = "80001-transform.png";
    private static final String TOOLTIP = "Transformar";


    public TransformarAction() {
        super(AppActionCommands.CMD_ADVANCED_EDITOR_TRANSFORMAR, ICON_KEY, TOOLTIP);
    } // --- Fin del constructor TransformarAction ---

} // --- Fin de la clase TransformarAction ---
