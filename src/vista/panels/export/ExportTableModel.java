package vista.panels.export;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import modelo.proyecto.ExportItem;
import modelo.proyecto.ExportStatus;

public class ExportTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private final java.util.function.Consumer<javax.swing.event.TableModelEvent> onDataChangedCallback;

    private List<ExportItem> cola;
    // --- INICIO DE LA MODIFICACIÓN ---
    private final String[] nombresColumnas = {"Exportar", "Imagen", "Archivo Comprimido", "Estado"};
    // --- FIN DE LA MODIFICACIÓN ---

    public ExportTableModel(java.util.function.Consumer<javax.swing.event.TableModelEvent> callback) {
        this.cola = new ArrayList<>();
        this.onDataChangedCallback = callback;
    } // --- Fin del método ExportTableModel (constructor) ---

    public void setCola(List<ExportItem> nuevaCola) {
        this.cola = new ArrayList<>(nuevaCola);
        fireTableDataChanged();
    } // --- Fin del método setCola ---

    public ExportItem getItemAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < cola.size()) {
            return cola.get(rowIndex);
        }
        return null;
    } // --- Fin del método getItemAt ---

    @Override
    public int getRowCount() {
        return cola.size();
    } // --- Fin del método getRowCount ---

    @Override
    public int getColumnCount() {
        return nombresColumnas.length;
    } // --- Fin del método getColumnCount ---
    
    @Override
    public String getColumnName(int column) {
        return nombresColumnas[column];
    } // --- Fin del método getColumnName ---

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= cola.size()) {
            return null;
        }
        ExportItem item = cola.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return item.isSeleccionadoParaExportar();
            case 1:
                return item.getRutaImagen().getFileName().toString();
            case 2:
                Path rutaComprimido = item.getRutaArchivoComprimido();
                return (rutaComprimido != null) ? rutaComprimido.getFileName().toString() : "---";
            case 3:
                return item.getEstadoArchivoComprimido();
            default:
                return null;
        }
    } // --- Fin del método getValueAt ---

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return Boolean.class; // La primera columna es de checkboxes
            case 3:
                return ExportStatus.class; // La última es de estados
            default:
                return String.class;
        }
    } // --- Fin del método getColumnClass ---

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Hacemos editable la columna del checkbox y la del estado
        return columnIndex == 0 || columnIndex == 3;
    } // --- Fin del método isCellEditable ---

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= cola.size()) {
            return;
        }
        ExportItem item = cola.get(rowIndex);
        if (columnIndex == 0 && aValue instanceof Boolean) {
            item.setSeleccionadoParaExportar((Boolean) aValue);
            // Notificamos a la tabla que la fila ha cambiado (para repintar)
            fireTableCellUpdated(rowIndex, columnIndex);
            
            // --- INICIO DE LA MODIFICACIÓN ---
            // Y ahora, llamamos al callback para notificar al controlador que re-evalúe la UI.
            if (onDataChangedCallback != null) {
                // Le pasamos el evento para que el listener sepa qué cambió, aunque no lo usemos ahora.
                onDataChangedCallback.accept(new javax.swing.event.TableModelEvent(this, rowIndex, rowIndex, columnIndex));
            }
            // --- FIN DE LA MODIFICACIÓN ---
        }
    } // --- Fin del método setValueAt ---

} // --- FIN de la clase ExportTableModel ---