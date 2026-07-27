package controlador.actions.editoravanzado;

import controlador.commands.AppActionCommands;

public class SeleccionCapaAction extends EditorToolAction {

    private static final long serialVersionUID = 1L;
    private static final String ICON_KEY = "80003-seleccion-layer.png";
    private static final String TOOLTIP = "Selección por capa";


    public SeleccionCapaAction() {
        super(AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_CAPA, ICON_KEY, TOOLTIP);
    } // --- Fin del constructor SeleccionCapaAction ---

} // --- Fin de la clase SeleccionCapaAction ---
