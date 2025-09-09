package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import controlador.ProjectController;

public class LocateAssociatedFileAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final ProjectController projectController;

    public LocateAssociatedFileAction(ProjectController controller) {
        super("Localizar"); // Nombre por defecto del botón/menú
        this.projectController = Objects.requireNonNull(controller);
    } // ---FIN de metodo [LocateAssociatedFileAction]---

    @Override
    public void actionPerformed(ActionEvent e) {
        projectController.solicitarLocalizarArchivoAsociado();
    } // ---FIN de metodo [actionPerformed]---

} // --- FIN de clase [LocateAssociatedFileAction]---