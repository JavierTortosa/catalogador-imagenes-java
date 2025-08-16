package controlador.actions.navegacion;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.GeneralController;
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;

public class VolverACarpetaRaizAction extends AbstractAction implements ContextSensitiveAction {

    private final GeneralController generalController;

    public VolverACarpetaRaizAction(GeneralController generalController, String text, ImageIcon icon) {
        super(text, icon);
        this.generalController = generalController;
        putValue(Action.SHORT_DESCRIPTION, "Volver a la carpeta raíz de la sesión");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ORDEN_CARPETA_RAIZ);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (generalController != null) {
            generalController.solicitarNavegarCarpetaRaiz();
        }
    }

    @Override
    public void updateEnabledState(VisorModel model) {
        // Esta acción, al igual que "Salir", solo debe estar habilitada
        // si hemos entrado en al menos una subcarpeta (el historial no está vacío).
        boolean isEnabled = !model.getCurrentListContext().getHistorialNavegacion().isEmpty();
        setEnabled(isEnabled);
    }
}