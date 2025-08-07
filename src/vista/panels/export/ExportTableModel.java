package vista.panels.export;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import modelo.proyecto.ExportItem;

public class ExportTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private final java.util.function.Consumer<javax.swing.event.TableModelEvent> onDataChangedCallback;

    private List<ExportItem> cola;
    // --- INICIO DE CAMBIO (1/4) ---
    // Modificamos los nombres de las columnas
    private final String[] nombresColumnas = {"", "Imagen", "Estado"};
    // --- FIN DE CAMBIO (1/4) ---

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
        
        // --- INICIO DE CAMBIO (2/4) ---
        // Adaptamos los valores devueltos a la nueva estructura de columnas
        switch (columnIndex) {
            case 0: // Columna del Checkbox
                return item.isSeleccionadoParaExportar();
            case 1: // Columna del Nombre de la Imagen
                return item.getRutaImagen().getFileName().toString();
            case 2: // Columna de Estado (ahora devuelve el propio item)
                // Devolvemos el objeto ExportItem completo. El renderer se encargará
                // de extraer el icono, el texto y el color de fondo.
                return item; 
            default:
                return null;
        }
        // --- FIN DE CAMBIO (2/4) ---
    } // --- Fin del método getValueAt ---

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        // --- INICIO DE CAMBIO (3/4) ---
        // Definimos los tipos de datos para cada columna
        switch (columnIndex) {
            case 0: // Columna del Checkbox
                return Boolean.class;
            case 1: // Columna del Nombre de la Imagen
                return String.class;
            case 2: // Columna de Estado
                // Le decimos a la tabla que esta columna contiene objetos ExportItem.
                // Esto permite al renderer recibir el objeto completo.
                return ExportItem.class;
            default:
                return Object.class;
        }
        // --- FIN DE CAMBIO (3/4) ---
    } // --- Fin del método getColumnClass ---

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Solo la columna del checkbox es editable directamente en la tabla.
        return columnIndex == 0;
    } // --- Fin del método isCellEditable ---

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= cola.size()) {
            return;
        }
        // --- INICIO DE CAMBIO (4/4) ---
        // La lógica se mantiene, solo se aplica a la columna 0
        if (columnIndex == 0 && aValue instanceof Boolean) {
            ExportItem item = cola.get(rowIndex);
            item.setSeleccionadoParaExportar((Boolean) aValue);
            
            fireTableCellUpdated(rowIndex, columnIndex);
            
            if (onDataChangedCallback != null) {
                onDataChangedCallback.accept(new javax.swing.event.TableModelEvent(this, rowIndex, rowIndex, columnIndex));
            }
        }
        // --- FIN DE CAMBIO (4/4) ---
    } // --- Fin del método setValueAt ---

} // --- FIN de la clase ExportTableModel ---