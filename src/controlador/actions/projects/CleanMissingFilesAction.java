package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import controlador.ProjectController;

public class CleanMissingFilesAction extends AbstractAction {

    private static final Logger logger = LoggerFactory.getLogger(CleanMissingFilesAction.class);
    private static final long serialVersionUID = 1L;
    private final ProjectController projectController;

    public CleanMissingFilesAction(ProjectController controller) {
        super("Limpiar archivos no encontrados");
        this.projectController = Objects.requireNonNull(controller);
    } // ---FIN de constructor [CleanMissingFilesAction]---

    @Override
    public void actionPerformed(ActionEvent e) {
        logger.debug("Acción 'Limpiar archivos no encontrados' ejecutada.");
        projectController.solicitarLimpiarImagenesNoEncontradas();
    } // ---FIN de metodo [actionPerformed]---

} // --- FIN de clase [CleanMissingFilesAction]---
