package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.GeneralController;
import controlador.commands.AppActionCommands;

public class GuardarProyectoAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final GeneralController generalController;

    public GuardarProyectoAction(GeneralController controller, String name, ImageIcon icon) {
        super(name, icon);
        this.generalController = Objects.requireNonNull(controller, "GeneralController no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Guarda el proyecto actual");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_GUARDAR); // Asegúrate de añadir CMD_PROYECTO_GUARDAR
    } // --- Fin del método GuardarProyectoAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        generalController.handleSaveProject();
    } // --- FIN del método actionPerformed ---

} // --- FIN de la clase GuardarProyectoAction ---