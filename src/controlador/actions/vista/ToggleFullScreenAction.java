package controlador.actions.vista; // O el paquete que prefieras para acciones de vista

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import java.util.Objects;
import controlador.GeneralController;
import controlador.commands.AppActionCommands;

/**
 * Acción para alternar el modo de pantalla completa de la aplicación.
 * Esta acción delega la lógica al GeneralController para orquestar el cambio.
 */
public class ToggleFullScreenAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final GeneralController generalController;

    /**
     * Constructor de la acción para alternar la pantalla completa.
     *
     * @param name             El nombre de la acción (para menús, tooltips).
     * @param icon             El icono para la acción (puede ser null).
     * @param generalController El controlador general al que se delegará la solicitud.
     */
    public ToggleFullScreenAction(String name, ImageIcon icon, GeneralController generalController) {
        super(name, icon);
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null en ToggleFullScreenAction.");
        
        // Configurar propiedades estándar de la Action
        putValue(Action.SHORT_DESCRIPTION, "Activar/Desactivar el modo de pantalla completa (F11)");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_VISTA_PANTALLA_COMPLETA);
        
        // Esta acción puede comportarse como un toggle, así que inicializamos su estado
        putValue(Action.SELECTED_KEY, false);
    } // --- Fin del constructor de ToggleFullScreenAction ---

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("[ToggleFullScreenAction] Acción disparada. Delegando a GeneralController...");
        // Llama al método centralizado en GeneralController para manejar la lógica
        generalController.solicitarToggleFullScreen();
    } // --- Fin del método actionPerformed ---
    
} // --- Fin de la clase ToggleFullScreenAction ---