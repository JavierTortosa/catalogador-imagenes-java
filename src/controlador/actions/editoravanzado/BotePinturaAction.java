package controlador.actions.editoravanzado;

import controlador.commands.AppActionCommands;

public class BotePinturaAction extends EditorToolAction {

    private static final long serialVersionUID = 1L;
    private static final String ICON_KEY = "80007-bote-pintura.png";
    private static final String TOOLTIP = "Rellenar zonas con un color sólido";


    public BotePinturaAction() {
        super(AppActionCommands.CMD_ADVANCED_EDITOR_BOTE_PINTURA, ICON_KEY, TOOLTIP);
    } // --- Fin del constructor BotePinturaAction ---

} // --- Fin de la clase BotePinturaAction ---
