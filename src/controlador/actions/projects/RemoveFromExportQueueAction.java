package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import controlador.ProjectController;

public class RemoveFromExportQueueAction extends AbstractAction {

    private static final Logger logger = LoggerFactory.getLogger(RemoveFromExportQueueAction.class);
    private static final long serialVersionUID = 1L;
    private final ProjectController projectController;

    public RemoveFromExportQueueAction(ProjectController controller) {
        super("Mover a descartes"); // El tooltip ahora es más preciso
        this.projectController = Objects.requireNonNull(controller);
    } // ---FIN de metodo [RemoveFromExportQueueAction]---

    @Override
    public void actionPerformed(ActionEvent e) {
        logger.debug("Acción 'Mover a descartes' ejecutada desde la tabla de exportación.");
        // La acción ya no contiene lógica. Simplemente delega la solicitud
        // al controlador, que es el único que sabe cómo orquestar la operación.
        projectController.solicitarMoverSeleccionadoAdescartes();
    } // ---FIN de metodo [actionPerformed]---

} // --- FIN de clase [RemoveFromExportQueueAction]---