package controlador.actions.projects;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import controlador.ProjectController;

public class RefreshExportQueueAction extends AbstractAction {
    
    private static final long serialVersionUID = 1L;
    private ProjectController projectController;

    public RefreshExportQueueAction(ProjectController projectController) {
        this.projectController = projectController;
    } // ---FIN de metodo [RefreshExportQueueAction]---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (projectController != null) {
            projectController.solicitarPreparacionColaExportacion();
        }
    } // ---FIN de metodo [actionPerformed]---
    
} // --- FIN de clase [RefreshExportQueueAction]---