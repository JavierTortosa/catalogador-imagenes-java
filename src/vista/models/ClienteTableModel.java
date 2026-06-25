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

    private static final String[] COLUMNS = {"Estado", "C\u00f3d IMG", "C\u00f3d CB", "Nombre", "Comentario"};

    private final List<String> imageKeys;
    private final List<Boolean> childRows;
    private final ProjectModel project;
    private final boolean mostrarSeleccion;

    public ClienteTableModel(ProjectModel project, boolean mostrarSeleccion) {
        this.project = project;
        this.mostrarSeleccion = mostrarSeleccion;
        this.imageKeys = new ArrayList<>();
        this.childRows = new ArrayList<>();
        refrescar();
    } // --- Fin de metodo ClienteTableModel (constructor) ---


    public void refrescar() {
        imageKeys.clear();
        childRows.clear();
        if (!project.hasClientSelection()) return;
        Map<String, SelectionState> clientImages = project.getClientSelection().getImages();
        for (Map.Entry<String, SelectionState> entry : clientImages.entrySet()) {
            String key = entry.getKey();
            SelectionState state = entry.getValue();

            // Saltar claves compuestas (C001_cb01) - se añaden como hijas de su imagen padre
            if (!project.esClaveRutaImagen(key)) {
                continue;
            }

            boolean estadoOk = mostrarSeleccion
                    ? (state == SelectionState.SELECTED || state == SelectionState.UNDEFINED)
                    : state == SelectionState.DISCARDED;
            if (!estadoOk) continue;

            String rutaCanon = project.resolverClaveImagenCanonica(key);
            var checkboxes = project.getClientSelection().getImageCheckboxes(rutaCanon);
            if (checkboxes != null && !checkboxes.isEmpty()) {
                // Añadir imagen padre
                imageKeys.add(key);
                childRows.add(false);
                // Añadir todos los checkbox como hijos (siempre con su imagen)
                String imgCode = project.getCodigoImagen(rutaCanon);
                for (var cb : checkboxes) {
                    String compositeKey = imgCode + "_" + cb.getCheckboxCode();
                    imageKeys.add(compositeKey);
                    childRows.add(true);
                }
            } else {
                imageKeys.add(key);
                childRows.add(false);
            }
        }
        fireTableDataChanged();
    } // --- Fin de metodo refrescar ---


    public boolean isChildRow(int row) {
        return row >= 0 && row < childRows.size() && childRows.get(row);
    }


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

        FilaCliente fila = resolverFila(key);

        switch (col) {
            case COL_ESTADO: return state;
            case COL_CODIGO_IMG: return fila.imgCode;
            case COL_CODIGO_CB: return fila.checkboxCode;
            case COL_NOMBRE: return fila.nombre;
            case COL_COMENTARIO: return fila.comentario;
            default: return "";
        }
    } // --- Fin de metodo getValueAt ---

    private FilaCliente resolverFila(String key) {
        if (project.esClaveRutaImagen(key)) {
            String ruta = project.resolverClaveImagenCanonica(key);
            return new FilaCliente(
                    project.getCodigoImagen(ruta),
                    "",
                    nombreArchivo(ruta),
                    project.hasClientSelection()
                            ? project.getClientSelection().getComments().getOrDefault(key, "")
                            : "");
        }

        int sep = key.lastIndexOf('_');
        if (sep > 0) {
            String imgCode = key.substring(0, sep);
            String cbCode = key.substring(sep + 1);
            if (!imgCode.isEmpty() && cbCode.startsWith("cb")) {
                String imagePath = buscarRutaPorCodigoImagen(imgCode);
                return new FilaCliente(imgCode, cbCode, nombreArchivo(imagePath),
                        comentarioDeCheckbox(imagePath, cbCode));
            }
        }

        // Claves antiguas mal formadas (_cb01) o solo cb01: buscar en overlays
        String cbCode = key.startsWith("_") ? key.substring(1) : key;
        if (cbCode.startsWith("cb") && project.hasClientSelection()) {
            for (Map.Entry<String, java.util.List<modelo.proyecto.ImageCheckboxOverlay>> entry
                    : project.getClientSelection().getImageCheckboxesMap().entrySet()) {
                for (var cb : entry.getValue()) {
                    if (cbCode.equals(cb.getCheckboxCode())) {
                        String ruta = entry.getKey();
                        return new FilaCliente(project.getCodigoImagen(ruta), cbCode,
                                nombreArchivo(ruta), cb.getComment() != null ? cb.getComment() : "");
                    }
                }
            }
        }

        return new FilaCliente("", "", key, "");
    } // --- Fin de metodo resolverFila ---

    private String nombreArchivo(String ruta) {
        if (ruta == null || ruta.isEmpty()) {
            return "";
        }
        java.nio.file.Path fn = java.nio.file.Paths.get(ruta).getFileName();
        return fn != null ? fn.toString() : "";
    } // --- Fin de metodo nombreArchivo ---

    private String buscarRutaPorCodigoImagen(String imgCode) {
        if (imgCode == null || imgCode.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, String> entry : project.getImageCodes().entrySet()) {
            if (imgCode.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    } // --- Fin de metodo buscarRutaPorCodigoImagen ---

    private String comentarioDeCheckbox(String imagePath, String checkboxCode) {
        if (!project.hasClientSelection() || imagePath == null || checkboxCode == null) {
            return "";
        }
        var checkboxes = project.getClientSelection().getImageCheckboxes(imagePath);
        if (checkboxes == null) {
            return "";
        }
        for (var cb : checkboxes) {
            if (checkboxCode.equals(cb.getCheckboxCode())) {
                return cb.getComment() != null ? cb.getComment() : "";
            }
        }
        return "";
    } // --- Fin de metodo comentarioDeCheckbox ---


    @Override
    public boolean isCellEditable(int row, int col) {
        if (col == COL_ESTADO && isChildRow(row)) {
            return false;
        }
        return col == COL_ESTADO || col == COL_COMENTARIO;
    } // --- Fin de metodo isCellEditable ---


    @Override
    public void setValueAt(Object value, int row, int col) {
        String key = imageKeys.get(row);
        if (col == COL_ESTADO && value instanceof SelectionState newState) {
            project.getClientSelection().getImages().put(key, newState);
            // Si es imagen padre con checkboxes, cascada el estado a todos los hijos
            if (!isChildRow(row)) {
                String rutaCanon = project.resolverClaveImagenCanonica(key);
                var checkboxes = project.getClientSelection().getImageCheckboxes(rutaCanon);
                if (checkboxes != null && !checkboxes.isEmpty()) {
                    String imgCode = project.getCodigoImagen(rutaCanon);
                    for (var cb : checkboxes) {
                        cb.setState(newState);
                        String compKey = imgCode + "_" + cb.getCheckboxCode();
                        project.getClientSelection().getImages().put(compKey, newState);
                    }
                }
            }
            fireTableDataChanged();
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

    private static final class FilaCliente {
        final String imgCode;
        final String checkboxCode;
        final String nombre;
        final String comentario;

        FilaCliente(String imgCode, String checkboxCode, String nombre, String comentario) {
            this.imgCode = imgCode != null ? imgCode : "";
            this.checkboxCode = checkboxCode != null ? checkboxCode : "";
            this.nombre = nombre != null ? nombre : "";
            this.comentario = comentario != null ? comentario : "";
        }
    } // --- Fin de clase FilaCliente ---

} // --- Fin de clase ClienteTableModel ---
