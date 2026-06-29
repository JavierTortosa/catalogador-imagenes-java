package vista.models;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import modelo.proyecto.ProjectImage;
import modelo.proyecto.ProjectModel;

/**
 * TableModel para las tablas de selecci&oacute;n/descartes del proyecto
 * en modo cliente. Fuente: project.masterImages.values().
 * Filtro selecci&oacute;n: pi.isEnSeleccionProyecto() == true
 * Filtro descartes: pi.isEnSeleccionProyecto() == false
 * Columnas: C&oacute;digo, Nombre.
 */
public class ProyectoClienteTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    public static final int COL_CODIGO = 0;
    public static final int COL_NOMBRE = 1;

    private static final String[] COLUMNS = {"Cod", "Nombre"};

    private final List<ProjectImage> images;
    private final ProjectModel project;
    private final boolean mostrarSeleccion;


    public ProyectoClienteTableModel(ProjectModel project, boolean mostrarSeleccion) {
        this.project = project;
        this.mostrarSeleccion = mostrarSeleccion;
        this.images = new ArrayList<>();
        refrescar();
    } // --- Fin de metodo ProyectoClienteTableModel (constructor) ---


    public void refrescar() {
        images.clear();
        if (project.getMasterImages() == null) {
            fireTableDataChanged();
            return;
        }
        for (ProjectImage pi : project.getMasterImages().values()) {
            if (mostrarSeleccion == pi.isEnSeleccionProyecto()) {
                images.add(pi);
            }
        }
        fireTableDataChanged();
    } // --- Fin de metodo refrescar ---


    @Override
    public int getRowCount() {
        return images.size();
    } // --- Fin de metodo getRowCount ---


    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    } // --- Fin de metodo getColumnCount ---


    @Override
    public String getColumnName(int col) {
        return COLUMNS[col];
    } // --- Fin de metodo getColumnName ---


    /**
     * @return la ruta de la imagen en la fila dada, o null si el &iacute;ndice es inv&aacute;lido.
     */
    public String getImageKey(int row) {
        if (row < 0 || row >= images.size()) return null;
        return images.get(row).getRutaImagen();
    } // --- Fin de metodo getImageKey ---


    /**
     * @return el ProjectImage en la fila dada, o null si el &iacute;ndice es inv&aacute;lido.
     */
    public ProjectImage getProjectImage(int row) {
        if (row < 0 || row >= images.size()) return null;
        return images.get(row);
    } // --- Fin de metodo getProjectImage ---


    @Override
    public Object getValueAt(int row, int col) {
        ProjectImage pi = images.get(row);
        switch (col) {
            case COL_CODIGO: {
                String code = pi.getCodigoCatalogo();
                return code != null ? code : "";
            }
            case COL_NOMBRE: {
                String ruta = pi.getRutaImagen();
                if (ruta == null || ruta.isEmpty()) return "";
                java.nio.file.Path p = java.nio.file.Paths.get(ruta);
                java.nio.file.Path fn = p.getFileName();
                return fn != null ? fn.toString() : ruta;
            }
            default: return "";
        }
    } // --- Fin de metodo getValueAt ---


    public boolean isSeleccion() {
        return mostrarSeleccion;
    } // --- Fin de metodo isSeleccion ---

} // --- Fin de clase ProyectoClienteTableModel ---
