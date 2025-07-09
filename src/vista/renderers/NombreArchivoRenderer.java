package vista.renderers;

import java.awt.Color; // <<< NUEVA IMPORTACIÓN
import java.awt.Component;
import java.nio.file.Paths; // <<< NUEVA IMPORTACIÓN
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import vista.theme.Tema;
import vista.theme.ThemeManager;

/**
 * Un ListCellRenderer personalizado para la lista de nombres de archivo.
 * Este renderer es "inteligente" en cuanto a los temas: en lugar de almacenar
 * colores fijos, mantiene una referencia al ThemeManager para obtener los
 * colores del tema actual cada vez que se necesita pintar una celda.
 * MODIFICADO: Ahora puede mostrar un color de selección diferente para la lista de descartes.
 */
public class NombreArchivoRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;
    private final ThemeManager themeManager;
    private final boolean isForDiscardsList;      // <<< NUEVO CAMPO
    private final Color discardSelectionColor;    // <<< NUEVO CAMPO

    /**
     * Constructor principal y ahora único.
     * @param themeManager El gestor de temas para obtener los colores.
     * @param isForDiscardsList true si este renderer es para la lista de descartes.
     */
    public NombreArchivoRenderer(ThemeManager themeManager, boolean isForDiscardsList) { // <<< CONSTRUCTOR MODIFICADO
        if (themeManager == null) {
            throw new IllegalArgumentException("ThemeManager no puede ser nulo en NombreArchivoRenderer");
        }
        this.themeManager = themeManager;
        this.isForDiscardsList = isForDiscardsList; // <<< NUEVA LÍNEA
        
        // Definir el color de selección para la lista de descartes.
        // Puedes ajustar este color como prefieras.
        this.discardSelectionColor = new Color(255, 220, 220); // Un rojo/rosa pálido <<< NUEVA LÍNEA
    } // --- Fin del método NombreArchivoRenderer (constructor) ---

    /**
     * Constructor de conveniencia para mantener la compatibilidad hacia atrás.
     * Asume que no es para la lista de descartes.
     * @param themeManager El gestor de temas.
     */
    public NombreArchivoRenderer(ThemeManager themeManager) {
        this(themeManager, false); // Llama al constructor principal con 'false' <<< NUEVA LÍNEA
    } // --- Fin del método NombreArchivoRenderer (constructor de conveniencia) ---

    /**
     * Este método es llamado por la JList para cada celda que necesita ser dibujada.
     */
    @Override
    public Component getListCellRendererComponent(
            JList<?> list, 
            Object value, 
            int index, 
            boolean isSelected, 
            boolean cellHasFocus) 
    {
        // 1. Llamar al método de la clase padre
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        // 2. Obtener el tema ACTUAL
        Tema temaActual = this.themeManager.getTemaActual();

        // 3. Establecer el texto
        if (value != null) {
            try {
                setText(Paths.get(value.toString()).getFileName().toString());
            } catch (Exception e) {
                setText(value.toString());
            }
        } else {
            setText("");
        }

        // 4. Aplicar los colores del tema ACTUAL
        if (isSelected) {
            // Si la celda está seleccionada, usar los colores de selección del tema.
            setBackground(temaActual.colorSeleccionFondo());
            setForeground(temaActual.colorSeleccionTexto());
        } else {
            // Si la celda NO está seleccionada...
            if (!list.isEnabled()) {
                // <<< INICIO DE LA CORRECCIÓN >>>
                // Si la lista está "desactivada", usamos colores que simulan inactividad.
                setBackground(temaActual.colorFondoPrincipal()); // Un fondo ligeramente diferente.
                setForeground(temaActual.colorTextoSecundario()); // Un texto menos prominente.
                // <<< FIN DE LA CORRECCIÓN >>>
            } else {
                // Si la lista está activa, usamos los colores normales.
                setBackground(temaActual.colorFondoSecundario());
                setForeground(temaActual.colorTextoPrimario());
            }
        }
        
        return this;
    } // --- Fin del método getListCellRendererComponent ---

    
} // --- Fin de la clase NombreArchivoRenderer ---


