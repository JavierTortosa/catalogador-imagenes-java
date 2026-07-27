package controlador.actions.editoravanzado;

import controlador.commands.AppActionCommands;

public class SeleccionMarcoAction extends EditorToolAction {

    private static final long serialVersionUID = 1L;
    private static final String ICON_KEY = "80002-seleccion-marco.png";
    private static final String TOOLTIP = "Selección por marco";


    public SeleccionMarcoAction() {
        super(AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_MARCO, ICON_KEY, TOOLTIP);
    } // --- Fin del constructor SeleccionMarcoAction ---

} // --- Fin de la clase SeleccionMarcoAction ---
