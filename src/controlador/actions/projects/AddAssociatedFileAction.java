package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import controlador.ProjectController;

public class AddAssociatedFileAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final ProjectController projectController;

    public AddAssociatedFileAction(ProjectController controller) {
        super("Añadir..."); // Nombre por defecto del botón
        this.projectController = Objects.requireNonNull(controller);
    } // ---FIN de metodo [AddAssociatedFileAction]---

    @Override
    public void actionPerformed(ActionEvent e) {
        projectController.solicitarAnadirArchivoAsociado();
    } // ---FIN de metodo [actionPerformed]---
    
} // --- FIN de clase [AddAssociatedFileAction]---