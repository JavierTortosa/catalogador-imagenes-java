package controlador.actions.projects;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ProjectController;
import vista.panels.export.ExportPanel;

public class ToggleExportDetailsAction extends AbstractAction {

    private static final Logger logger = LoggerFactory.getLogger(ToggleExportDetailsAction.class);
    private static final long serialVersionUID = 1L;
    private ProjectController projectController;

    public ToggleExportDetailsAction(ProjectController projectController) {
        this.projectController = projectController;
        putValue(Action.NAME, "Mostrar/Ocultar Detalles de Archivos");
        putValue(Action.SHORT_DESCRIPTION, "Muestra u oculta el panel inferior con los detalles de los archivos asociados.");
    } // ---FIN de metodo [ToggleExportDetailsAction]---

    @Override
    public void actionPerformed(ActionEvent e) {
        logger.debug("Acción ToggleExportDetailsAction ejecutada.");
        
        // Obtenemos el panel de exportación a través del registro de componentes
        ExportPanel exportPanel = projectController.getRegistry()
                .get("panel.proyecto.exportacion.completo");

        if (exportPanel != null) {
            // Llamamos al método que creamos en el Paso 1
            exportPanel.toggleDetailsPanelVisibility();

            // Sincronizamos el estado de selección del botón (si es un JToggleButton)
            if (e.getSource() instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) e.getSource();
                putValue(Action.SELECTED_KEY, button.isSelected());
            }
        } else {
            logger.error("No se pudo encontrar 'panel.proyecto.exportacion.completo' en el ComponentRegistry.");
        }
    } // ---FIN de metodo [actionPerformed]---

} // --- FIN de clase [ToggleExportDetailsAction]---