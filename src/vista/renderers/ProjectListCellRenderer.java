package vista.renderers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.UIManager;

/**
 * Un ListCellRenderer personalizado para las listas del modo Proyecto.
 * Este renderer muestra el nombre del archivo y cambia su apariencia si el
 * archivo correspondiente no existe en el disco. Obtiene los colores de error
 * directamente del UIManager para adaptarse a cualquier tema FlatLaf.
 */
public class ProjectListCellRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;
    
    // --- INICIO DE LA MODIFICACIÓN ---
    // Ya no recibimos colores, los obtenemos del UIManager cuando se necesiten.
    // --- FIN DE LA MODIFICACIÓN ---
    
    // Un simple caché para no comprobar la existencia del archivo en cada repintado
    private final Map<String, Boolean> fileExistsCache = new HashMap<>();

    public ProjectListCellRenderer() {
        // Constructor vacío
    } // ---FIN de metodo [ProjectListCellRenderer]---

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        // Llama al método de la superclase para obtener la apariencia por defecto
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof String) {
            String pathString = (String) value;
            
            // Usamos el caché para mejorar el rendimiento
            boolean exists = fileExistsCache.computeIfAbsent(pathString, p -> Files.exists(Paths.get(p)));

            if (exists) {
                // El archivo existe, texto y apariencia normales
                setText(Paths.get(pathString).getFileName().toString());
                setFont(getFont().deriveFont(Font.PLAIN));
                // Los colores de texto y fondo ya los gestiona la superclase
            } else {
                // El archivo NO existe, aplicamos estilo de error
                setText("<html><strike>" + Paths.get(pathString).getFileName().toString() + "</strike></html>");
                setFont(getFont().deriveFont(Font.ITALIC));
                
                // --- INICIO DE LA MODIFICACIÓN ---
                // Obtenemos los colores estándar de FlatLaf para errores
                Color colorTextoError = UIManager.getColor("Component.error.foreground");
                if (colorTextoError == null) colorTextoError = Color.RED; // Fallback por si acaso

                if (isSelected) {
                    // Para la selección, mantenemos el color de texto de error
                    // pero dejamos que el fondo sea el de selección normal para consistencia.
                    // Si quisiéramos un fondo especial, lo tomaríamos de UIManager.
                    setForeground(colorTextoError);
                } else {
                    setForeground(colorTextoError);
                    // El fondo se mantiene el normal para una celda no seleccionada
                }
                // --- FIN DE LA MODIFICACIÓN ---
            }
        }
        
        return this;
    } // ---FIN de metodo [getListCellRendererComponent]---

    /**
     * Limpia el caché de existencia de archivos. Debe ser llamado cuando
     * se carga un nuevo proyecto o se refrescan los datos.
     */
    public void clearCache() {
        fileExistsCache.clear();
    } // ---FIN de metodo [clearCache]---

} // --- FIN de clase [ProjectListCellRenderer]---