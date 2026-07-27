package controlador.actions.editoravanzado;

import controlador.commands.AppActionCommands;

public class CuentagotasAction extends EditorToolAction {

    private static final long serialVersionUID = 1L;
    private static final String ICON_KEY = "80006-cuentagotas.png";
    private static final String TOOLTIP = "Capturar el color situado bajo el cursor";


    public CuentagotasAction() {
        super(AppActionCommands.CMD_ADVANCED_EDITOR_CUENTAGOTAS, ICON_KEY, TOOLTIP);
    } // --- Fin del constructor CuentagotasAction ---

} // --- Fin de la clase CuentagotasAction ---
