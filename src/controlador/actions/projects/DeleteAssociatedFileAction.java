package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import controlador.ProjectController;

public class DeleteAssociatedFileAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final ProjectController projectController;

    public DeleteAssociatedFileAction(ProjectController controller) {
        super("Quitar"); // Nombre por defecto del botón/menú
        this.projectController = Objects.requireNonNull(controller);
    } // ---FIN de metodo [DeleteAssociatedFileAction]---

    @Override
    public void actionPerformed(ActionEvent e) {
        projectController.solicitarQuitarArchivoAsociado();
    } // ---FIN de metodo [actionPerformed]---

} // --- FIN de clase [DeleteAssociatedFileAction]---