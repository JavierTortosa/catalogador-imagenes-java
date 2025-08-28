package vista.renderers;

import java.awt.Component;
import java.nio.file.Paths;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import modelo.VisorModel;
import vista.theme.Tema;
import vista.theme.ThemeManager;

/**
 * Un ListCellRenderer personalizado para las listas de nombres de archivo del modo proyecto.
 * Es sensible al tema y al "foco lógico" del modo proyecto, permitiendo que la lista
 * inactiva se muestre con un aspecto "apagado" sin deshabilitarla.
 */
public class NombreArchivoRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;
    private final ThemeManager themeManager;
    private final boolean isForDiscardsList;
    
    private static final javax.swing.border.Border noFocusBorder = new javax.swing.border.EmptyBorder(1, 1, 1, 1);
    
    private final VisorModel model; // Referencia al modelo para consultar el foco lógico

    /**
     * Constructor principal.
     * @param themeManager El gestor de temas para obtener los colores.
     * @param model El modelo principal de la aplicación.
     * @param isForDiscardsList true si este renderer es para la lista de descartes.
     */
    public NombreArchivoRenderer(ThemeManager themeManager, VisorModel model, boolean isForDiscardsList) {
        if (themeManager == null || model == null) {
            throw new IllegalArgumentException("ThemeManager y VisorModel no pueden ser nulos en NombreArchivoRenderer");
        }
        this.themeManager = themeManager;
        this.model = model; 
        this.isForDiscardsList = isForDiscardsList;
        setOpaque(true); 
        
    } // --- Fin del método NombreArchivoRenderer (constructor) ---

    
    /**
     * Constructor de conveniencia para la lista de "Selección Actual".
     * @param themeManager El gestor de temas.
     * @param model El modelo principal.
     */
    public NombreArchivoRenderer(ThemeManager themeManager, VisorModel model) {
        this(themeManager, model, false);
        setOpaque(true); 
    } // --- Fin del método NombreArchivoRenderer (constructor de conveniencia) ---

    
    @Override
    public Component getListCellRendererComponent(
            JList<?> list, 
            Object value, 
            int index, 
            boolean isSelected, 
            boolean cellHasFocus) 
    {
        // 1. Establecer el texto de la celda (tu lógica original)
        if (value != null) {
            try {
                setText(Paths.get(value.toString()).getFileName().toString());
            } catch (Exception e) {
                setText(value.toString());
            }
        } else {
            setText("");
        }
        
        // 2. Obtener el tema actual desde tu ThemeManager
        Tema temaActual = this.themeManager.getTemaActual();

        // 3. Aplicar colores basándose en si la celda está seleccionada o no
        if (isSelected) {
            // --- CELDA SELECCIONADA ---
            // Usa los colores de selección de tu tema. ¡Esto arreglará el selector invisible!
        	
            setBackground(temaActual.colorSeleccionFondo());
            setForeground(temaActual.colorSeleccionTexto());
        } else {
            // --- CELDA NO SELECCIONADA ---
            // Usa los colores normales de tu tema
            setBackground(temaActual.colorFondoPrincipal());
            setForeground(temaActual.colorTextoPrimario());
        }

        // 4. Gestionar el borde del foco para accesibilidad
        if (cellHasFocus) {
            // Usa el borde estándar de Swing para indicar el foco
            setBorder(javax.swing.UIManager.getBorder("List.focusCellHighlightBorder"));
        } else {
            // Usa el borde vacío que ya tenías definido para celdas sin foco
            setBorder(noFocusBorder); 
        }
        
        return this;
    
    } // --- Fin del método getListCellRendererComponent ---

} // --- Fin de la clase NombreArchivoRenderer ---

