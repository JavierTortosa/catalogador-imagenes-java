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

public class RelocateImageAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
    private final ProjectController projectController;

    public RelocateImageAction(ProjectController projectController) {
        super("Relocalizar Imagen..."); // Texto que aparecerá en el menú
        this.projectController = projectController;
        setEnabled(false); // Por defecto, la acción está deshabilitada
    } // --- Fin del método RelocateImageAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        projectController.solicitarRelocalizacionImagen();
    } // --- Fin del método actionPerformed ---

    @Override
    public void updateEnabledState(VisorModel model) {
        // Esta acción solo está habilitada si hay una fila seleccionada en la tabla
        // y el estado de esa fila es IMAGEN_NO_ENCONTRADA.
        boolean enabled = false;
        
        // Obtenemos la tabla de forma segura a través del método que hicimos público
        JTable tabla = projectController.getTablaExportacionDesdeRegistro(); 
        if (tabla != null && tabla.getSelectedRow() != -1) {
            ExportTableModel tableModel = (ExportTableModel) tabla.getModel();
            ExportItem item = tableModel.getItemAt(tabla.getSelectedRow());
            if (item != null && item.getEstadoArchivoComprimido() == ExportStatus.IMAGEN_NO_ENCONTRADA) {
                enabled = true;
            }
        }
        setEnabled(enabled);
    } // --- Fin del método updateEnabledState ---
    
} // --- FIN de la clase RelocateImageAction ---