package controlador.actions.navegacion;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.GeneralController;
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;

public class SalirDeSubcarpetaAction extends AbstractAction implements ContextSensitiveAction {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final GeneralController generalController;

    public SalirDeSubcarpetaAction(GeneralController generalController, String text, ImageIcon icon) {
        super(text, icon);
        this.generalController = generalController;
        putValue(Action.SHORT_DESCRIPTION, "Salir de la subcarpeta actual");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ORDEN_CARPETA_ANTERIOR);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (generalController != null) {
            generalController.solicitarSalirDeSubcarpeta();
        }
    }

    @Override
    public void updateEnabledState(VisorModel model) {
        // La acción está habilitada solo si el historial de navegación no está vacío.
        boolean isEnabled = !model.getCurrentListContext().getHistorialNavegacion().isEmpty();
        setEnabled(isEnabled);
    }
}