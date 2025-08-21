package controlador.actions.toggle;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.GeneralController;
import controlador.commands.AppActionCommands;
import modelo.VisorModel;

/**
 * Acción para activar o desactivar el modo de filtro en vivo.
 * Esta acción maneja su propio estado de 'seleccionado'.
 */
public class ToggleLiveFilterAction extends AbstractAction {

    private final VisorModel model;
    private final GeneralController generalController;

    public ToggleLiveFilterAction(VisorModel model, GeneralController generalController, String name, ImageIcon icon) {
        super(name, icon); // Llama al constructor de AbstractAction
        this.model = model;
        this.generalController = generalController;
        
        // Establece el comando canónico
        putValue(ACTION_COMMAND_KEY, AppActionCommands.CMD_FILTRO_TOGGLE_LIVE_FILTER);
        
        // Sincroniza el estado inicial del botón con el modelo la primera vez
        putValue(SELECTED_KEY, model.isLiveFilterActive());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // 1. Invertir el estado actual
        boolean newState = !model.isLiveFilterActive();
        
        // 2. Actualizar el modelo (la fuente de la verdad)
        model.setLiveFilterActive(newState);
        
        // 3. Actualizar el estado visual de ESTA ACCIÓN (el botón se actualizará solo)
        putValue(SELECTED_KEY, newState);
        
        // 4. Notificar directamente al GeneralController para que actúe
        //    (esto es lo que hacíamos con el listener, pero ahora es una llamada directa)
        if (generalController != null) {
            generalController.onLiveFilterStateChanged(newState);
        }
    }
}