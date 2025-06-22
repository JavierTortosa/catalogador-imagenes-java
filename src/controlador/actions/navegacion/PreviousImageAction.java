// En src/controlador/actions/navegacion/FirstImageAction.java
package controlador.actions.navegacion;

import java.awt.event.ActionEvent;
import java.util.Objects; // Para Objects.requireNonNull
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.managers.interfaces.IListCoordinator;
import controlador.commands.AppActionCommands;

public class PreviousImageAction extends AbstractAction { // Ya no hereda de BaseVisorAction

    private static final long serialVersionUID = 1L; // Genera uno si es necesario

    private IListCoordinator listCoordinator;

    // Constructor Refactorizado
    public PreviousImageAction(IListCoordinator listCoordinator, String displayName, ImageIcon icon) {
        super(displayName, icon); // Nombre para menú/tooltip, e icono
        this.listCoordinator = Objects.requireNonNull(listCoordinator, "ListCoordinator no puede ser null en PreviousImageAction");

        // Propiedades estándar de la Action
        putValue(Action.SHORT_DESCRIPTION, "Ir a la imagen anterior de la lista"); // Tooltip
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_NAV_ANTERIOR); // Comando canónico

        // El estado 'enabled' de estas actions de navegación
        // será manejado por el ListCoordinator o VisorController cuando la lista cambie
        // o la selección cambie. Podrían empezar deshabilitadas.
        setEnabled(true); 
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("[PreviousImageAction actionPerformed] Comando: " + e.getActionCommand());

        if (this.listCoordinator == null) {
            System.err.println("ERROR CRÍTICO [PreviousImageAction]: ListCoordinator es nulo.");
            return;
        }

        // Delegar la lógica de navegación al ListCoordinator
        this.listCoordinator.seleccionarAnterior(); 
        // No necesitamos llamar a logActionInfo(e) aquí si ya no depende de VisorController
        // El logging de la acción puede hacerse aquí si es necesario, o el ListCoordinator puede loguear.
    }
}
