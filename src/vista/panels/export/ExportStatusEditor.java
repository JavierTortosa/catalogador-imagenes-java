package vista.panels.export;

import java.awt.Component;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;

import modelo.proyecto.ExportItem;
import modelo.proyecto.ExportStatus;

public class ExportStatusEditor extends AbstractCellEditor implements TableCellEditor {

    private static final long serialVersionUID = 1L;
    private final java.util.function.Consumer<ExportItem> onFileSelectedCallback;

    public ExportStatusEditor(java.util.function.Consumer<ExportItem> callback) {
        this.onFileSelectedCallback = callback;
    } // --- Fin del método ExportStatusEditor (constructor) ---

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        // Solo permitimos la edición si es un evento de ratón (un clic)
        return anEvent instanceof java.awt.event.MouseEvent;
    } // --- Fin del método isCellEditable ---

    @Override
    public Object getCellEditorValue() {
        // No necesitamos devolver un nuevo valor, la tabla se refrescará
        return null;
    } // --- Fin del método getCellEditorValue ---

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (value == ExportStatus.NO_ENCONTRADO || value == ExportStatus.MULTIPLES_CANDIDATOS) {
            SwingUtilities.invokeLater(() -> {
                // Ya no muestra el FileChooser, solo llama al callback.
                if (onFileSelectedCallback != null) {
                    // El callback debe obtener el item de la tabla y llamar al controlador
                    ExportTableModel model = (ExportTableModel) table.getModel();
                    ExportItem item = model.getItemAt(row);
                    if (item != null) {
                        onFileSelectedCallback.accept(item);
                    }
                }
                fireEditingStopped();
            });
        }
        return null; 
    } // --- Fin del método getTableCellEditorComponent ---

} // --- FIN de la clase ExportStatusEditor ---