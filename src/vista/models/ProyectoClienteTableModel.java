package vista.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import modelo.proyecto.ProjectModel;

/**
 * TableModel para las tablas de selección/descartes del proyecto
 * en modo cliente. Muestra Cod, Nombre y Acciones.
 */
public class ProyectoClienteTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    public static final int COL_CODIGO = 0;
    public static final int COL_NOMBRE = 1;
    public static final int COL_ACCIONES = 2;

    private static final String[] COLUMNS = {"Cod", "Nombre", "Acciones"};

    private final List<String> imageKeys;
    private final ProjectModel project;
    private final boolean mostrarSeleccion; // true=selección, false=descartes

    public ProyectoClienteTableModel(ProjectModel project, boolean mostrarSeleccion) {
        this.project = project;
        this.mostrarSeleccion = mostrarSeleccion;
        this.imageKeys = new ArrayList<>();
        refrescar();
    } // --- Fin de metodo ProyectoClienteTableModel (constructor) ---


    public void refrescar() {
        imageKeys.clear();
        if (!project.isSharedWithClient()) {
            fireTableDataChanged();
            return;
        }
        if (mostrarSeleccion) {
            imageKeys.addAll(project.getSelectedImages().keySet());
        } else {
            imageKeys.addAll(project.getDiscardedImages());
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
        Map<String, String> codes = project.getImageCodes();
        String code = codes != null ? codes.getOrDefault(key, "") : "";
        switch (col) {
            case COL_CODIGO: return code;
            case COL_NOMBRE: {
                java.nio.file.Path p = java.nio.file.Paths.get(key);
                java.nio.file.Path fn = p.getFileName();
                return fn != null ? fn.toString() : key;
            }
            case COL_ACCIONES: return key;
            default: return "";
        }
    } // --- Fin de metodo getValueAt ---


    public boolean isSeleccion() {
        return mostrarSeleccion;
    } // --- Fin de metodo isSeleccion ---

} // --- Fin de clase ProyectoClienteTableModel ---
