package controlador.actions.projects;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JTable;

import controlador.ProjectController;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;
import modelo.proyecto.ExportItem;
import modelo.proyecto.ExportStatus;
import vista.panels.export.ExportTableModel;

public class ToggleIgnoreCompressedAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
    private final ProjectController projectController;

    public ToggleIgnoreCompressedAction(ProjectController controller) {
        super("Ignorar archivo comprimido");
        this.projectController = controller;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        projectController.solicitarAlternarIgnorarComprimido();
    }

    @Override
    public void updateEnabledState(VisorModel model) {
        JTable tablaExportacion = projectController.getTablaExportacionDesdeRegistro();
        boolean shouldBeEnabled = false;
        if (tablaExportacion != null && tablaExportacion.getSelectedRow() != -1) {
            ExportTableModel tableModel = (ExportTableModel) tablaExportacion.getModel();
            ExportItem selectedItem = tableModel.getItemAt(tablaExportacion.getSelectedRow());
            if (selectedItem != null) {
                ExportStatus status = selectedItem.getEstadoArchivoComprimido();
                // Habilitar si el estado es NO_ENCONTRADO o si YA ESTÁ IGNORADO (para poder revertirlo)
                shouldBeEnabled = (status == ExportStatus.NO_ENCONTRADO || status == ExportStatus.IGNORAR_COMPRIMIDO);
            }
        }
        setEnabled(shouldBeEnabled);
    }
} // --- FIN de clase [ToggleIgnoreCompressedAction]---