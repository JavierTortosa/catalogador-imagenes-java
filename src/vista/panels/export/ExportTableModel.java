package vista.panels.export;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import modelo.proyecto.ExportItem;
import modelo.proyecto.ExportStatus;

public class ExportTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private List<ExportItem> cola;
    private final String[] nombresColumnas = {"Imagen", "Archivo Comprimido", "Estado"};

    public ExportTableModel() {
        this.cola = new ArrayList<>();
    } // --- Fin del método ExportTableModel (constructor) ---

    public void setCola(List<ExportItem> nuevaCola) {
        this.cola = new ArrayList<>(nuevaCola); // Crear una copia para evitar modificaciones externas
        fireTableDataChanged(); // Notificar a la tabla que los datos han cambiado por completo
    } // --- Fin del método setCola ---

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
                return item.getRutaImagen().getFileName().toString();
            case 1:
                Path rutaComprimido = item.getRutaArchivoComprimido();
                return (rutaComprimido != null) ? rutaComprimido.getFileName().toString() : "---";
            case 2:
                return item.getEstadoArchivoComprimido();
            default:
                return null;
        }
    } // --- Fin del método getValueAt ---

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 2) {
            return ExportStatus.class;
        }
        return String.class;
    } // --- Fin del método getColumnClass ---
    
    
    public ExportItem getItemAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < cola.size()) {
            return cola.get(rowIndex);
        }
        return null;
    } // --- Fin del método getItemAt ---

} // --- FIN de la clase ExportTableModel ---