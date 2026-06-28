package vista.models;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import modelo.proyecto.ImageCheckboxOverlay;
import modelo.proyecto.ProjectImage;
import modelo.proyecto.ProjectModel;
import modelo.proyecto.SelectionState;

/**
 * TableModel para las tablas de selecci&oacute;n/descartes del cliente.
 * Fuente: project.masterImages.values().
 * Filtro selecci&oacute;n cliente:
 *   (pi.estadoCliente == SELECTED || pi.estadoCliente == UNDEFINED) && pi.enSeleccionProyecto == true
 * Filtro descartes cliente:
 *   pi.estadoCliente == DISCARDED && pi.enSeleccionProyecto == true
 * Las filas hijas se resuelven desde pi.checkboxes.
 * Columnas: Estado, C&oacute;d IMG, C&oacute;d CB, Precio, Comentario.
 */
public class ClienteTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    public static final int COL_ESTADO = 0;
    public static final int COL_CODIGO_IMG = 1;
    public static final int COL_CODIGO_CB = 2;
    public static final int COL_PRECIO = 3;
    public static final int COL_COMENTARIO = 4;

    private static final String[] COLUMNS = {"Estado", "C\u00f3d IMG", "C\u00f3d CB", "Precio", "Comentario"};

    private final List<ProjectImage> parentImages;
    private final List<Integer> checkboxIndices; // -1 para filas padre, >=0 para hijas
    private final ProjectModel project;
    private final boolean mostrarSeleccion;


    public ClienteTableModel(ProjectModel project, boolean mostrarSeleccion) {
        this.project = project;
        this.mostrarSeleccion = mostrarSeleccion;
        this.parentImages = new ArrayList<>();
        this.checkboxIndices = new ArrayList<>();
        refrescar();
    } // --- Fin de metodo ClienteTableModel (constructor) ---


    public void refrescar() {
        parentImages.clear();
        checkboxIndices.clear();
        if (project.getMasterImages() == null) {
            fireTableDataChanged();
            return;
        }
        for (ProjectImage pi : project.getMasterImages().values()) {
            if (!pi.isEnSeleccionProyecto()) continue;

            boolean estadoOk = mostrarSeleccion
                    ? (pi.getEstadoCliente() == SelectionState.SELECTED
                    || pi.getEstadoCliente() == SelectionState.UNDEFINED)
                    : pi.getEstadoCliente() == SelectionState.DISCARDED;
            if (!estadoOk) continue;

            // Fila padre
            parentImages.add(pi);
            checkboxIndices.add(-1);

            // Filas hijas: cada checkbox interno
            List<ImageCheckboxOverlay> cbs = pi.getCheckboxes();
            if (cbs != null) {
                for (int i = 0; i < cbs.size(); i++) {
                    parentImages.add(pi);
                    checkboxIndices.add(i);
                }
            }
        }
        fireTableDataChanged();
    } // --- Fin de metodo refrescar ---


    /**
     * @return la ruta de la imagen a la que pertenece la fila, o null si el &iacute;ndice es inv&aacute;lido.
     */
    public String getImageKey(int row) {
        if (row < 0 || row >= parentImages.size()) return null;
        return parentImages.get(row).getRutaImagen();
    } // --- Fin de metodo getImageKey ---


    /**
     * @return true si la fila corresponde a un checkbox interno (hija).
     */
    public boolean isChildRow(int row) {
        return row >= 0 && row < checkboxIndices.size() && checkboxIndices.get(row) >= 0;
    } // --- Fin de metodo isChildRow ---


    /**
     * @return el c&oacute;digo del checkbox interno en la fila hija, o null si es fila padre.
     */
    public String getCheckboxCode(int row) {
        if (!isChildRow(row)) return null;
        ImageCheckboxOverlay cb = getCheckbox(row);
        return cb != null ? cb.getCheckboxCode() : null;
    } // --- Fin de metodo getCheckboxCode ---


    public ImageCheckboxOverlay getCheckbox(int row) {
        int idx = checkboxIndices.get(row);
        if (idx < 0) return null;
        ProjectImage pi = parentImages.get(row);
        List<ImageCheckboxOverlay> cbs = pi.getCheckboxes();
        if (cbs != null && idx < cbs.size()) {
            return cbs.get(idx);
        }
        return null;
    } // --- Fin de metodo getCheckbox ---


    @Override
    public int getRowCount() {
        return parentImages.size();
    } // --- Fin de metodo getRowCount ---


    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    } // --- Fin de metodo getColumnCount ---


    @Override
    public String getColumnName(int col) {
        return COLUMNS[col];
    } // --- Fin de metodo getColumnName ---


    @Override
    public Object getValueAt(int row, int col) {
        ProjectImage pi = parentImages.get(row);
        switch (col) {
            case COL_ESTADO: {
                if (isChildRow(row)) {
                    ImageCheckboxOverlay cb = getCheckbox(row);
                    return cb != null ? cb.getState() : SelectionState.UNDEFINED;
                }
                return pi.getEstadoCliente() != null ? pi.getEstadoCliente() : SelectionState.UNDEFINED;
            }
            case COL_CODIGO_IMG: {
                String code = pi.getCodigoCatalogo();
                return code != null ? code : "";
            }
            case COL_CODIGO_CB: {
                if (isChildRow(row)) {
                    ImageCheckboxOverlay cb = getCheckbox(row);
                    return cb != null ? cb.getCheckboxCode() : "";
                }
                return "";
            }
            case COL_PRECIO: {
                if (isChildRow(row)) {
                    ImageCheckboxOverlay cb = getCheckbox(row);
                    return cb != null ? cb.getPrice() : 0.0;
                }
                return 0.0;
            }
            case COL_COMENTARIO: {
                if (isChildRow(row)) {
                    ImageCheckboxOverlay cb = getCheckbox(row);
                    String cmt = cb != null ? cb.getComment() : null;
                    return cmt != null ? cmt : "";
                }
                String cmt = pi.getComment();
                return cmt != null ? cmt : "";
            }
            default: return "";
        }
    } // --- Fin de metodo getValueAt ---


    @Override
    public boolean isCellEditable(int row, int col) {
        if (col == COL_ESTADO) return true;
        // COL_COMENTARIO se edita via MsgPopupDialog (doble-click)
        return false;
    } // --- Fin de metodo isCellEditable ---


    @Override
    public void setValueAt(Object value, int row, int col) {
        ProjectImage pi = parentImages.get(row);
        if (col == COL_ESTADO && value instanceof SelectionState newState) {
            if (isChildRow(row)) {
                ImageCheckboxOverlay cb = getCheckbox(row);
                if (cb != null) cb.setState(newState);
            } else {
                pi.setEstadoCliente(newState);
                // Cascada a hijos: el estado del padre se propaga a todos los checkboxes
                List<ImageCheckboxOverlay> cbs = pi.getCheckboxes();
                if (cbs != null) {
                    for (ImageCheckboxOverlay cb : cbs) {
                        cb.setState(newState);
                    }
                }
            }
            fireTableDataChanged();
        } else if (col == COL_COMENTARIO && value instanceof String str) {
            setComentario(row, str);
        }
    } // --- Fin de metodo setValueAt ---


    /**
     * @return el ProjectImage al que pertenece la fila, o null si el &iacute;ndice es inv&aacute;lido.
     */
    public ProjectImage getProjectImage(int row) {
        if (row < 0 || row >= parentImages.size()) return null;
        return parentImages.get(row);
    } // --- Fin de metodo getProjectImage ---


    /**
     * Establece el comentario de la fila (padre o hija).
     */
    public void setComentario(int row, String comment) {
        if (row < 0 || row >= parentImages.size()) return;
        if (comment == null) comment = "";
        if (isChildRow(row)) {
            ImageCheckboxOverlay cb = getCheckbox(row);
            if (cb != null) cb.setComment(comment);
        } else {
            parentImages.get(row).setComment(comment);
        }
        fireTableCellUpdated(row, COL_COMENTARIO);
    } // --- Fin de metodo setComentario ---


    public SelectionState getEstado(int row) {
        if (row < 0 || row >= parentImages.size()) return SelectionState.UNDEFINED;
        if (isChildRow(row)) {
            ImageCheckboxOverlay cb = getCheckbox(row);
            return cb != null ? cb.getState() : SelectionState.UNDEFINED;
        }
        return parentImages.get(row).getEstadoCliente();
    } // --- Fin de metodo getEstado ---


    public void setEstado(int row, SelectionState state) {
        if (row < 0 || row >= parentImages.size()) return;
        if (isChildRow(row)) {
            ImageCheckboxOverlay cb = getCheckbox(row);
            if (cb != null) cb.setState(state);
        } else {
            parentImages.get(row).setEstadoCliente(state);
        }
        fireTableDataChanged();
    } // --- Fin de metodo setEstado ---

} // --- Fin de clase ClienteTableModel ---
