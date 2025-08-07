package vista.panels.export;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.AppInitializer;

public class HeaderMouseListener extends MouseAdapter {

	private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);
    private final JTable table;

    public HeaderMouseListener(JTable table) {
        this.table = table;
    } // --- Fin del método HeaderMouseListener (constructor) ---

    @Override
    public void mouseClicked(MouseEvent e) {
        TableColumnModel columnModel = table.getColumnModel();
        int viewColumn = columnModel.getColumnIndexAtX(e.getX());
        int modelColumn = table.convertColumnIndexToModel(viewColumn);

        // Solo actuar si el clic es en la primera columna (la de los checkboxes)
        if (modelColumn == 0) {
            // Determinar el nuevo estado. Si no todas están seleccionadas, seleccionamos todas.
            // Si ya todas estaban seleccionadas, las deseleccionamos.
            boolean shouldSelectAll = !areAllRowsSelected();
            
            // Obtenemos el TableModel para llamar a setValueAt
            ExportTableModel model = (ExportTableModel) table.getModel();
            
            // Iteramos por todas las filas y cambiamos su estado.
            for (int i = 0; i < model.getRowCount(); i++) {
                // Llamamos a setValueAt, que se encarga de actualizar el modelo
                // y notificar al controlador para que actualice la UI.
                model.setValueAt(shouldSelectAll, i, 0);
            }
            
            // Forzar un repintado de la cabecera para que el checkbox refleje el nuevo estado.
            table.getTableHeader().repaint();
        }
    } // --- Fin del método mouseClicked ---
    
    /**
     * Comprueba si todas las filas de la tabla están actualmente seleccionadas.
     */
    private boolean areAllRowsSelected() {
        ExportTableModel model = (ExportTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            // Obtenemos el valor booleano directamente del modelo.
            if (!(Boolean) model.getValueAt(i, 0)) {
                return false;
            }
        }
        return model.getRowCount() > 0;
    } // --- Fin del método areAllRowsSelected ---
    
} // --- FIN de la clase HeaderMouseListener ---