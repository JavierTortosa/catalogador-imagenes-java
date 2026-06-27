package vista.renderers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.UIManager;

import modelo.proyecto.ProjectImage;
import modelo.proyecto.ProjectModel;
import modelo.proyecto.SelectionState;
import servicios.ProjectManager;

/**
 * ListCellRenderer personalizado para las listas del cliente.
 * Muestra el nombre de la imagen y su estado (SELECTED, DISCARDED).
 */
public class ClientListCellRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;
    
    private final ProjectManager projectManager;
    private final Map<String, Boolean> fileExistsCache = new HashMap<>();

    /**
     * Constructor que inyecta el ProjectManager para consultar el estado del cliente.
     */
    public ClientListCellRenderer(ProjectManager projectManager) {
        this.projectManager = projectManager;
    } // --- Fin del constructor ClientListCellRenderer ---

    /**
     * Devuelve el componente renderizado para cada celda de la lista del cliente,
     * mostrando nombre de archivo, estado (SELECTED/DISCARDED) y comentarios.
     */
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof String) {
            String pathString = (String) value;
            boolean exists = fileExistsCache.computeIfAbsent(pathString, p -> Files.exists(Paths.get(p)));

            Path p = Paths.get(pathString);
            Path fn = p.getFileName();
            String fileName = (fn != null) ? fn.toString() : p.toString();

            ProjectModel project = projectManager.getCurrentProject();
            SelectionState state = SelectionState.DISCARDED;
            boolean hasComment = false;

            if (project != null) {
                String canonical = ProjectModel.normalizarClaveImagen(pathString);
                ProjectImage pi = project.getMasterImages().get(canonical);
                if (pi != null) {
                    state = pi.getEstadoCliente();
                    String comment = pi.getComment();
                    hasComment = comment != null && !comment.trim().isEmpty();
                }
            }

            // Construir representación visual del estado
            String prefix = "";
            Color stateColor = UIManager.getColor("Label.foreground");

            if (state == SelectionState.SELECTED) {
                prefix = "[✓] ";
                stateColor = new Color(34, 139, 34); // Verde
            } else if (state == SelectionState.DISCARDED) {
                prefix = "[✗] ";
                stateColor = UIManager.getColor("Component.error.foreground");
                if (stateColor == null) stateColor = Color.RED;
            } else {
                prefix = "[?] ";
                stateColor = Color.GRAY;
            }

            String suffix = hasComment ? " 💬" : "";

            if (exists) {
                setText(prefix + fileName + suffix);
                setFont(getFont().deriveFont(Font.PLAIN));
                if (!isSelected) {
                    setForeground(stateColor);
                }
            } else {
                setText("<html><strike>" + prefix + fileName + suffix + "</strike></html>");
                setFont(getFont().deriveFont(Font.ITALIC));
                Color errorColor = UIManager.getColor("Component.error.foreground");
                if (errorColor == null) errorColor = Color.RED;
                setForeground(errorColor);
            }
        }
        
        return this;
    } // --- Fin del metodo getListCellRendererComponent ---

    /**
     * Limpia la caché de existencia de archivos para forzar una re-verificación.
     */
    public void clearCache() {
        fileExistsCache.clear();
    } // --- Fin del metodo clearCache ---

} // --- Fin de clase ClientListCellRenderer ---
