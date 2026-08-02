package controlador.actions.editoravanzado;

import controlador.commands.AppActionCommands;

public class ZoomAction extends EditorToolAction {

    private static final long serialVersionUID = 1L;
    private static final String ICON_KEY = "80011-zoom.png";
    private static final String TOOLTIP = "Acercar y alejar";


    public ZoomAction() {
        super(AppActionCommands.CMD_ADVANCED_EDITOR_ZOOM, ICON_KEY, TOOLTIP);
    } // --- Fin del constructor ZoomAction ---

} // --- Fin de la clase ZoomAction ---
