package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;

import controlador.ProjectController;
import controlador.commands.AppActionCommands;

public class ToggleExportViewAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final ProjectController projectController;

    public ToggleExportViewAction(ProjectController controller) {
        super();
        this.projectController = Objects.requireNonNull(controller);
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_EXPORT_ASSIGN_PANNEL);
    } // ---FIN de metodo [ToggleExportViewAction]---

    @Override
    public void actionPerformed(ActionEvent e) {
        // Le decimos al ProjectController que alterne la visibilidad del panel de exportación.
        projectController.toggleExportView();
    } // ---FIN de metodo [actionPerformed]---
} // --- FIN de clase [ToggleExportViewAction]---