package controlador.actions.projects;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JTable;

import controlador.ProjectController;
import controlador.interfaces.ContextSensitiveAction; // <-- AÑADIR IMPORT
import modelo.VisorModel; // <-- AÑADIR IMPORT
import modelo.proyecto.ExportItem;
import modelo.proyecto.ExportStatus;
import vista.panels.export.ExportTableModel;

public class RelocateImageAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
    private final ProjectController projectController;

    public RelocateImageAction(ProjectController controller) {
        super("Relocalizar Imagen...");
        this.projectController = controller;
    } // ---FIN de metodo [RelocateImageAction]---

    @Override
    public void actionPerformed(ActionEvent e) {
        projectController.solicitarRelocalizacionImagen();
    } // ---FIN de metodo [actionPerformed]---

    @Override
    public void updateEnabledState(VisorModel model) {
        JTable tablaExportacion = projectController.getTablaExportacionDesdeRegistro();
        boolean shouldBeEnabled = false;
        if (tablaExportacion != null && tablaExportacion.getSelectedRow() != -1) {
            ExportTableModel tableModel = (ExportTableModel) tablaExportacion.getModel();
            int modelRow = tablaExportacion.convertRowIndexToModel(tablaExportacion.getSelectedRow());
            ExportItem selectedItem = tableModel.getItemAt(modelRow);
            if (selectedItem != null) {
                // Habilitar solo si la imagen original no se encontró
                shouldBeEnabled = (selectedItem.getEstadoArchivoComprimido() == ExportStatus.IMAGEN_NO_ENCONTRADA);
            }
        }
        setEnabled(shouldBeEnabled);
    } // ---FIN de metodo [updateEnabledState]---

} // --- FIN de clase [RelocateImageAction]---