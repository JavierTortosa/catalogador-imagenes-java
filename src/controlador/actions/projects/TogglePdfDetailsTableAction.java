package controlador.actions.projects;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ProjectController;
import vista.panels.export.ExportPanel;

public class TogglePdfDetailsTableAction extends AbstractAction {

    private static final Logger logger = LoggerFactory.getLogger(TogglePdfDetailsTableAction.class);
    private static final long serialVersionUID = 1L;
    private ProjectController projectController;

    public TogglePdfDetailsTableAction(ProjectController projectController) {
        this.projectController = projectController;
        putValue(Action.NAME, "Mostrar/Ocultar Tabla de Detalles PDF");
        putValue(Action.SHORT_DESCRIPTION, "Muestra u oculta la tabla con los campos del catálogo PDF.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        logger.debug("Acción TogglePdfDetailsTableAction ejecutada.");

        ExportPanel exportPanel = projectController.getRegistry()
                .get("panel.proyecto.exportacion.completo");

        if (exportPanel != null) {
            exportPanel.togglePdfDetailsTableVisibility();

            if (e.getSource() instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) e.getSource();
                putValue(Action.SELECTED_KEY, button.isSelected());
            }
        } else {
            logger.error("No se pudo encontrar 'panel.proyecto.exportacion.completo' en el ComponentRegistry.");
        }
    }

}
