package vista.panels.export;

import java.nio.file.Path; 
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import modelo.proyecto.ExportItem;

public class ExportTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private final java.util.function.Consumer<javax.swing.event.TableModelEvent> onDataChangedCallback;
    private List<ExportItem> cola;
    
    private final String[] nombresColumnas = {"", "Imagen", "Estado", "Archivos Asignados"};

    public ExportTableModel(java.util.function.Consumer<javax.swing.event.TableModelEvent> callback) {
        this.cola = new ArrayList<>();
        this.onDataChangedCallback = callback;
    } // ---FIN de metodo [ExportTableModel]---

    public void setCola(List<ExportItem> nuevaCola) {
        this.cola = new ArrayList<>(nuevaCola);
        fireTableDataChanged();
    } // ---FIN de metodo [setCola]---

    public ExportItem getItemAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < cola.size()) {
            return cola.get(rowIndex);
        }
        return null;
    } // ---FIN de metodo [getItemAt]---

    @Override
    public int getRowCount() {
        return cola.size();
    } // ---FIN de metodo [getRowCount]---

    @Override
    public int getColumnCount() {
        return nombresColumnas.length;
    } // ---FIN de metodo [getColumnCount]---
    
    @Override
    public String getColumnName(int column) {
        return nombresColumnas[column];
    } // ---FIN de metodo [getColumnName]---

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= cola.size()) {
            return null;
        }
        ExportItem item = cola.get(rowIndex);
        
        switch (columnIndex) {
            case 0: return item.isSeleccionadoParaExportar();
            case 1: return item.getRutaImagen().getFileName().toString();
            case 2: return item; // Para el renderer de estado
            case 3: return item.getRutasArchivosAsociados(); // Devolvemos la lista de Paths
            default: return null;
        }
    } // ---FIN de metodo [getValueAt]---

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0: return Boolean.class;
            case 1: return String.class;
            case 2: return ExportItem.class;
            case 3: return List.class; // La columna contiene una lista
            default: return Object.class;
        }
    } // ---FIN de metodo [getColumnClass]---

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    } // ---FIN de metodo [isCellEditable]---

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 0 && aValue instanceof Boolean) {
            cola.get(rowIndex).setSeleccionadoParaExportar((Boolean) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
            if (onDataChangedCallback != null) {
                onDataChangedCallback.accept(new javax.swing.event.TableModelEvent(this, rowIndex, rowIndex, columnIndex));
            }
        }
    } // ---FIN de metodo [setValueAt]---
    
    /**
     * Busca el índice de la fila que corresponde a una ruta de imagen específica.
     * La comparación se hace usando la representación normalizada del Path.
     * @param imagePathKey La ruta de la imagen a buscar, como un String.
     * @return El índice de la fila (0-based) si se encuentra, o -1 si no.
     */
    public int findRowIndexByPath(String imagePathKey) {
        if (imagePathKey == null || imagePathKey.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < cola.size(); i++) {
            ExportItem item = cola.get(i);
            if (item != null && item.getRutaImagen() != null) {
                // Comparamos la representación de String normalizada para evitar problemas de SO
                String currentItemPath = item.getRutaImagen().toString().replace("\\", "/");
                if (currentItemPath.equals(imagePathKey)) {
                    return i;
                }
            }
        }
        return -1; // No encontrado
    } // ---FIN de metodo [findRowIndexByPath]---

    public List<ExportItem> getCola() {
        return this.cola;
    } // ---FIN de metodo [getCola]---
    
    public void clear() {
        this.cola.clear();
        fireTableDataChanged();
    } // ---FIN de metodo [clear]---

} // --- FIN de clase [ExportTableModel]---