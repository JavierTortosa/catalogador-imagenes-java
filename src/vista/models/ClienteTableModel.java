package vista.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import modelo.proyecto.ImageCheckboxOverlay;
import modelo.proyecto.Mensaje;
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

    public static final int COL_COLLAPSE = 0;
    public static final int COL_ESTADO = 1;
    public static final int COL_CODIGO_IMG = 2;
    public static final int COL_CODIGO_CB = 3;
    public static final int COL_PRECIO = 4;
    public static final int COL_COMENTARIO = 5;

    private static final String[] COLUMNS = {"", "Est", "C. IMG", "C. CB", "PVP", "Msg"};

    public enum CollapseFilter { NONE, SMART, FULL }

    private final List<ProjectImage> parentImages;
    private final List<Integer> checkboxIndices; // -1 para filas padre, >=0 para hijas
    private final ProjectModel project;
    private final boolean mostrarSeleccion;
    private boolean editingActive;
    private Runnable modificationListener;
    private final Map<String, CollapseFilter> collapseFilters = new HashMap<>();
    private int sortColumn = -1;
    private boolean sortAscending = true;


    public int getSortColumn() {
        return sortColumn;
    }


    public boolean isSortAscending() {
        return sortAscending;
    }


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

            // Filas hijas segun filtro de colapso
            List<ImageCheckboxOverlay> cbs = pi.getCheckboxes();
            if (cbs == null || cbs.isEmpty()) continue;

            CollapseFilter filter = collapseFilters.getOrDefault(pi.getRutaImagen(), CollapseFilter.NONE);

            switch (filter) {
                case FULL:
                    // no se añaden hijos
                    break;
                case SMART:
                    for (int i = 0; i < cbs.size(); i++) {
                        ImageCheckboxOverlay cb = cbs.get(i);
                        // Mostrar solo si NO es DISCARDED/UNDEFINED sin comentario
                        if (cb.getState() != SelectionState.SELECTED && cb.getComment().isEmpty()) continue;
                        parentImages.add(pi);
                        checkboxIndices.add(i);
                    }
                    break;
                case NONE:
                default:
                    for (int i = 0; i < cbs.size(); i++) {
                        parentImages.add(pi);
                        checkboxIndices.add(i);
                    }
                    break;
            }
        }
        if (sortColumn >= 0) {
            sortGroups(sortColumn, sortAscending);
        }
        fireTableDataChanged();
    } // --- Fin de metodo refrescar ---


    public void toggleSort(int column) {
        if (column == sortColumn) {
            if (sortAscending) {
                sortAscending = false;
            } else {
                sortColumn = -1; // reinicia a orden natural
            }
        } else {
            sortColumn = column;
            sortAscending = true;
        }
        refrescar();
    } // --- Fin de metodo toggleSort ---


    /**
     * Ordena los grupos (padre + hijos) seg&uacute;n el valor de la columna en la fila padre.
     */
    private void sortGroups(int column, boolean ascending) {
        // 1. Identificar grupos de filas consecutivas
        List<int[]> groups = new ArrayList<>();
        int i = 0;
        while (i < parentImages.size()) {
            int start = i;
            i++; // padre
            while (i < parentImages.size() && checkboxIndices.get(i) >= 0) {
                i++; // hijos
            }
            groups.add(new int[]{start, i});
        }

        // 2. Ordenar grupos por el valor de la fila padre
        groups.sort((g1, g2) -> {
            int cmp;
            if (column == COL_COMENTARIO) {
                cmp = Integer.compare(getMessageWeight(g1[0]), getMessageWeight(g2[0]));
            } else {
                Object v1 = getValueAt(g1[0], column);
                Object v2 = getValueAt(g2[0], column);
                cmp = compareSortValues(v1, v2);
            }
            return ascending ? cmp : -cmp;
        });

        // 3. Reconstruir listas en el nuevo orden
        List<ProjectImage> newParents = new ArrayList<>(parentImages.size());
        List<Integer> newIndices = new ArrayList<>(checkboxIndices.size());
        for (int[] g : groups) {
            for (int r = g[0]; r < g[1]; r++) {
                newParents.add(parentImages.get(r));
                newIndices.add(checkboxIndices.get(r));
            }
        }
        parentImages.clear();
        parentImages.addAll(newParents);
        checkboxIndices.clear();
        checkboxIndices.addAll(newIndices);
    } // --- Fin de metodo sortGroups ---


    /**
     * Compara dos valores de celda para ordenaci&oacute;n.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compareSortValues(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        if (a instanceof Comparable ca && b instanceof Comparable) {
            return ca.compareTo(b);
        }
        // SelectionState tiene toString() descriptivo; comparar por ordinal
        if (a instanceof SelectionState sa && b instanceof SelectionState sb) {
            return Integer.compare(sa.ordinal(), sb.ordinal());
        }
        return a.toString().compareTo(b.toString());
    } // --- Fin de metodo compareSortValues ---


    /**
     * Peso del mensaje para ordenaci&oacute;n: 3=rojo(cliente), 2=azul(espera), 1=verde(le&iacute;do), 0=gris(sin msgs).
     */
    private int getMessageWeight(int row) {
        ProjectImage pi = parentImages.get(row);
        if (pi == null) return 0;
        int maxW = threadWeight(pi.getCommentThread());
        List<ImageCheckboxOverlay> cbs = pi.getCheckboxes();
        if (cbs != null) {
            for (ImageCheckboxOverlay cb : cbs) {
                maxW = Math.max(maxW, threadWeight(cb.getCommentThread()));
            }
        }
        return maxW;
    } // --- Fin de metodo getMessageWeight ---


    private int threadWeight(List<Mensaje> thread) {
        if (thread == null || thread.isEmpty()) return 0;
        Mensaje last = thread.get(thread.size() - 1);
        if ("cliente".equalsIgnoreCase(last.de())) return 3;
        for (Mensaje m : thread) {
            if ("cliente".equalsIgnoreCase(m.de())) return 1;
        }
        return 2;
    } // --- Fin de metodo threadWeight ---


    /**
     * @return true si el cliente ha respondido al menos una vez en el hilo de la fila padre.
     */
    public boolean hasClientReplied(int row) {
        ProjectImage pi = parentImages.get(row);
        if (pi == null) return false;
        List<Mensaje> thread = pi.getCommentThread();
        if (thread != null) {
            for (Mensaje m : thread) {
                if ("cliente".equalsIgnoreCase(m.de())) return true;
            }
        }
        return false;
    } // --- Fin de metodo hasClientReplied ---


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


    /**
     * @return true si la fila corresponde a una imagen (padre), no a un checkbox interno.
     */
    public boolean isParentRow(int row) {
        return row >= 0 && row < checkboxIndices.size() && checkboxIndices.get(row) < 0;
    } // --- Fin de metodo isParentRow ---


    public CollapseFilter getCollapseFilter(int row) {
        if (!isParentRow(row)) return CollapseFilter.NONE;
        return collapseFilters.getOrDefault(parentImages.get(row).getRutaImagen(), CollapseFilter.NONE);
    } // --- Fin de metodo getCollapseFilter ---


    /**
     * Cicla NONE -> SMART -> FULL -> NONE para la fila padre indicada.
     */
    public void cycleCollapseFilter(int row) {
        if (!isParentRow(row)) return;
        String key = parentImages.get(row).getRutaImagen();
        CollapseFilter current = collapseFilters.getOrDefault(key, CollapseFilter.NONE);
        CollapseFilter next = switch (current) {
            case NONE -> CollapseFilter.SMART;
            case SMART -> CollapseFilter.FULL;
            case FULL -> CollapseFilter.NONE;
        };
        collapseFilters.put(key, next);
        refrescar();
    } // --- Fin de metodo cycleCollapseFilter ---


    /**
     * @return n&uacute;mero total de checkboxes internos de la imagen en la fila.
     */
    public int getCheckboxCount(int row) {
        ProjectImage pi = parentImages.get(row);
        List<ImageCheckboxOverlay> cbs = pi.getCheckboxes();
        return cbs != null ? cbs.size() : 0;
    } // --- Fin de metodo getCheckboxCount ---


    /**
     * @return n&uacute;mero de checkboxes visibles en modo SMART para la imagen en la fila.
     */
    public int getSmartVisibleCount(int row) {
        ProjectImage pi = parentImages.get(row);
        List<ImageCheckboxOverlay> cbs = pi.getCheckboxes();
        if (cbs == null) return 0;
        int count = 0;
        for (ImageCheckboxOverlay cb : cbs) {
            if (cb.getState() != SelectionState.SELECTED && cb.getComment().isEmpty()) continue;
            count++;
        }
        return count;
    } // --- Fin de metodo getSmartVisibleCount ---


    public void setEditingActive(boolean editingActive) {
        this.editingActive = editingActive;
    } // --- Fin de metodo setEditingActive ---


    public boolean isEditingActive() {
        return editingActive;
    } // --- Fin de metodo isEditingActive ---


    public void setModificationListener(Runnable listener) {
        this.modificationListener = listener;
    } // --- Fin de metodo setModificationListener ---


    private void fireModification() {
        if (modificationListener != null) {
            modificationListener.run();
        }
    } // --- Fin de metodo fireModification ---


    /**
     * @return n&uacute;mero de checkboxes internos que tienen comentario o hilo de mensajes.
     */
    public int getCheckboxMessageCount(int row) {
        ProjectImage pi = parentImages.get(row);
        List<ImageCheckboxOverlay> cbs = pi.getCheckboxes();
        if (cbs == null) return 0;
        int count = 0;
        for (ImageCheckboxOverlay cb : cbs) {
            if (!cb.getComment().isEmpty()) count++;
        }
        return count;
    } // --- Fin de metodo getCheckboxMessageCount ---


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
        
            case COL_COLLAPSE: {
                if (isChildRow(row)) return "";
                
                int total = getCheckboxCount(row);
                
                // Si no hay elementos hijos, no mostramos flecha ni contador
                if (total <= 0) {
                    return "";
                }
                
                String key = pi.getRutaImagen();
                
                CollapseFilter filter = collapseFilters.getOrDefault(key, CollapseFilter.NONE);
//                int total = getCheckboxCount(row);
                return switch (filter) {
                    case NONE -> "\u25BC " + total;										// ▼ 16 (todos visibles)
                    case SMART -> "\u25B6 " + getSmartVisibleCount(row) + "/" + total;	// ▶ 3/16 (filtrados)
                    case FULL -> "\u25B6";												// ▶ (ninguno visible)
                };
            }
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
                return pi.getPrice();
            }
            case COL_COMENTARIO: {
                if (isChildRow(row)) {
                    ImageCheckboxOverlay cb = getCheckbox(row);
                    String cmt = cb != null ? cb.getComment() : null;
                    return cmt != null ? cmt : "";
                }
                String cmt = pi.getComment();
                String base = cmt != null ? cmt : "";
                CollapseFilter filter = collapseFilters.getOrDefault(pi.getRutaImagen(), CollapseFilter.NONE);
                if (filter != CollapseFilter.NONE) {
                    int msgCount = getCheckboxMessageCount(row);
                    if (msgCount > 0) {
                        if (!base.isEmpty()) base += " | ";
                        base += "\uD83D\uDCDD " + msgCount;
                    }
                }
                return base;
            }
            default: return "";
        }
    } // --- Fin de metodo getValueAt ---


    @Override
    public boolean isCellEditable(int row, int col) {
        if (!editingActive) return false;
        if (col == COL_ESTADO) return true;
        if (col == COL_PRECIO) return true;
        return false;
    } // --- Fin de metodo isCellEditable ---


    @Override
    public void setValueAt(Object value, int row, int col) {
        ProjectImage pi = parentImages.get(row);
        if (col == COL_ESTADO && value instanceof SelectionState newState) {
            if (isChildRow(row)) {
                ImageCheckboxOverlay cb = getCheckbox(row);
                if (cb != null) {
                    cb.setState(newState);
                    project.derivarEstadoImagen(pi.getRutaImagen());
                }
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
            fireModification();
        } else if (col == COL_PRECIO) {
            try {
                double precio = Double.parseDouble(value.toString().trim().replace(",", "."));
                if (isChildRow(row)) {
                    ImageCheckboxOverlay cb = getCheckbox(row);
                    if (cb != null) cb.setPrice(precio);
                } else {
                    pi.setPrice(precio);
                    List<ImageCheckboxOverlay> cbs = pi.getCheckboxes();
                    if (cbs != null) {
                        for (ImageCheckboxOverlay cb : cbs) {
                            cb.setPrice(precio);
                        }
                    }
                }
                fireTableDataChanged();
                fireModification();
            } catch (NumberFormatException e) {
                // ignorar entrada no numérica
            }
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
            if (cb != null) {
                cb.setState(state);
                project.derivarEstadoImagen(parentImages.get(row).getRutaImagen());
            }
        } else {
            parentImages.get(row).setEstadoCliente(state);
        }
        fireTableDataChanged();
        fireModification();
    } // --- Fin de metodo setEstado ---

} // --- Fin de clase ClienteTableModel ---
