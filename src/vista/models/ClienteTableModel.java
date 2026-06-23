package vista.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import modelo.proyecto.ProjectModel;
import modelo.proyecto.SelectionState;

/**
 * TableModel para las tablas de selección/descartes del cliente.
 * Muestra Estado, Código img, Código cb, Nombre y Comentario.
 */
public class ClienteTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    public static final int COL_ESTADO = 0;
    public static final int COL_CODIGO_IMG = 1;
    public static final int COL_CODIGO_CB = 2;
    public static final int COL_NOMBRE = 3;
    public static final int COL_COMENTARIO = 4;

    private static final String[] COLUMNS = {"Estado", "C\u00f3digo img", "C\u00f3digo cb", "Nombre", "Comentario"};

    private final List<String> imageKeys;
    private final ProjectModel project;
    private final boolean mostrarSeleccion;

    public ClienteTableModel(ProjectModel project, boolean mostrarSeleccion) {
        this.project = project;
        this.mostrarSeleccion = mostrarSeleccion;
        this.imageKeys = new ArrayList<>();
        refrescar();
    } // --- Fin de metodo ClienteTableModel (constructor) ---


    public void refrescar() {
        imageKeys.clear();
        if (!project.hasClientSelection()) return;
        Map<String, SelectionState> clientImages = project.getClientSelection().getImages();
        for (Map.Entry<String, SelectionState> entry : clientImages.entrySet()) {
            boolean estadoOk = mostrarSeleccion
                    ? entry.getValue() == SelectionState.SELECTED
                    : entry.getValue() == SelectionState.DISCARDED;
            if (estadoOk) {
                imageKeys.add(entry.getKey());
            }
        }
        fireTableDataChanged();
    } // --- Fin de metodo refrescar ---


    @Override
    public int getRowCount() {
        return imageKeys.size();
    } // --- Fin de metodo getRowCount ---


    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    } // --- Fin de metodo getColumnCount ---


    @Override
    public String getColumnName(int col) {
        return COLUMNS[col];
    } // --- Fin de metodo getColumnName ---


    public String getImageKey(int row) {
        return imageKeys.get(row);
    } // --- Fin de metodo getImageKey ---


    @Override
    public Object getValueAt(int row, int col) {
        String key = imageKeys.get(row);
        SelectionState state = project.hasClientSelection()
                ? project.getClientSelection().getImages().getOrDefault(key, SelectionState.UNDEFINED)
                : SelectionState.UNDEFINED;
        String imgCode = project.getImageCodes().getOrDefault(key, "");
        String nombre = java.nio.file.Paths.get(key).getFileName().toString();
        String comentario = project.hasClientSelection()
                ? project.getClientSelection().getComments().getOrDefault(key, "")
                : "";
        String checkboxCode = "";
        if (project.hasClientSelection()) {
            var checkboxes = project.getClientSelection().getImageCheckboxes(key);
            if (!checkboxes.isEmpty()) {
                checkboxCode = checkboxes.get(0).getCheckboxCode();
            }
        }
        switch (col) {
            case COL_ESTADO: return state;
            case COL_CODIGO_IMG: return imgCode;
            case COL_CODIGO_CB: return checkboxCode;
            case COL_NOMBRE: return nombre;
            case COL_COMENTARIO: return comentario;
            default: return "";
        }
    } // --- Fin de metodo getValueAt ---


    @Override
    public boolean isCellEditable(int row, int col) {
        return col == COL_ESTADO || col == COL_COMENTARIO;
    } // --- Fin de metodo isCellEditable ---


    @Override
    public void setValueAt(Object value, int row, int col) {
        String key = imageKeys.get(row);
        if (col == COL_ESTADO && value instanceof SelectionState) {
            project.getClientSelection().getImages().put(key, (SelectionState) value);
            fireTableCellUpdated(row, col);
        } else if (col == COL_COMENTARIO && value instanceof String) {
            project.getClientSelection().getComments().put(key, (String) value);
            fireTableCellUpdated(row, col);
        }
    } // --- Fin de metodo setValueAt ---


    public SelectionState getEstado(int row) {
        String key = imageKeys.get(row);
        return project.hasClientSelection()
                ? project.getClientSelection().getImages().getOrDefault(key, SelectionState.UNDEFINED)
                : SelectionState.UNDEFINED;
    } // --- Fin de metodo getEstado ---


    public void setEstado(int row, SelectionState state) {
        String key = imageKeys.get(row);
        project.getClientSelection().getImages().put(key, state);
        fireTableCellUpdated(row, COL_ESTADO);
    } // --- Fin de metodo setEstado ---

} // --- Fin de clase ClienteTableModel ---
