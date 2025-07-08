package vista.panels.export;

import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import java.util.EventObject;
import javax.swing.AbstractCellEditor;
import javax.swing.JFileChooser;
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
        // Solo actuamos si el estado es de un tipo que requiere intervención
        if (value == ExportStatus.NO_ENCONTRADO || value == ExportStatus.MULTIPLES_CANDIDATOS) {
            
            // Usamos invokeLater para que el FileChooser no bloquee el repintado de la tabla
            SwingUtilities.invokeLater(() -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Localizar Archivo Comprimido para " + table.getValueAt(row, 0));
                
                int result = fileChooser.showOpenDialog(table);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    Path selectedPath = selectedFile.toPath();
                    
                    // Obtenemos el ExportItem de la tabla para modificarlo
                    ExportTableModel model = (ExportTableModel) table.getModel();
                    // Necesitaremos un método en el TableModel para obtener el item
                    ExportItem item = model.getItemAt(row);
                    
                    if (item != null) {
                        item.setRutaArchivoComprimido(selectedPath);
                        item.setEstadoArchivoComprimido(ExportStatus.ASIGNADO_MANUAL);
                        
                        // Llamamos al callback para notificar al controlador que algo ha cambiado
                        if (onFileSelectedCallback != null) {
                            onFileSelectedCallback.accept(item);
                        }
                    }
                }
                // Importante: después de la interacción, le decimos al editor que pare.
                fireEditingStopped();
            });
        }
        
        // No devolvemos un componente de edición visible, la acción ocurre en el diálogo
        return null; 
    } // --- Fin del método getTableCellEditorComponent ---

} // --- FIN de la clase ExportStatusEditor ---