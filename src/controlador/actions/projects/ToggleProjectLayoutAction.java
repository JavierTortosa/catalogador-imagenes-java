package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;

import controlador.ProjectController;
import controlador.commands.AppActionCommands;

/**
 * Acción que alterna el layout del panel de proyecto entre
 * el modo de asignación (ASSIGNMENT) y el modo normal (DEFAULT).
 */
public class ToggleProjectLayoutAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final ProjectController projectController;

    /**
     * Constructor de la acción.
     * @param controller Controlador del proyecto que contiene el método toggleProjectLayout.
     */
    public ToggleProjectLayoutAction(ProjectController controller) {
        super();
        this.projectController = Objects.requireNonNull(controller);
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_TOGGLE_LAYOUT);
    } // --- Fin del constructor ToggleProjectLayoutAction ---


    @Override
    public void actionPerformed(ActionEvent e) {
        projectController.toggleProjectLayout();
    } // --- Fin del método actionPerformed ---


} // --- Fin de la clase ToggleProjectLayoutAction ---
