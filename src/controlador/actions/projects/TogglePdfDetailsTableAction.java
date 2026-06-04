package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ProjectController;
import modelo.proyecto.ExportItem;
import vista.panels.export.ExportPanel;
import vista.panels.export.ExportTableModel;

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

        if (exportPanel == null) {
            logger.error("No se pudo encontrar 'panel.proyecto.exportacion.completo' en el ComponentRegistry.");
            return;
        }

        // Confirmar cualquier edición pendiente antes de cambiar el estado
        exportPanel.getPdfDetailsTablePanel().stopEditing();
        exportPanel.stopExportTableEditing();

        if (exportPanel.isPdfDetailsTableVisible()) {
            // Ya visible: simplemente ocultar
            exportPanel.togglePdfDetailsTableVisibility();
        } else {
            // Oculto: analizar archivos primero, luego mostrar
            ExportTableModel modelTable = (ExportTableModel) exportPanel.getTablaExportacion().getModel();
            List<ExportItem> seleccionados = modelTable.getCola().stream()
                    .filter(ExportItem::isSeleccionadoParaExportar)
                    .collect(Collectors.toList());

            if (seleccionados.isEmpty()) {
                // Sin elementos, mostrar el panel vacío directamente
                exportPanel.togglePdfDetailsTableVisibility();
            } else {
                projectController.asegurarAnalisisTecnico(seleccionados,
                        () -> SwingUtilities.invokeLater(
                                () -> exportPanel.togglePdfDetailsTableVisibility()));
            }
        }

        if (e.getSource() instanceof AbstractButton) {
            AbstractButton button = (AbstractButton) e.getSource();
            putValue(Action.SELECTED_KEY, button.isSelected());
        }
    }

}
