package vista.renderers;

import java.awt.Component;
import java.nio.file.Paths;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import modelo.VisorModel; 
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
        // Llamar a super se encarga de los colores de fondo y selección estándar
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        // Solo nos preocupamos de establecer el texto del archivo
        if (value != null) {
            try {
                setText(Paths.get(value.toString()).getFileName().toString());
            } catch (Exception e) {
                setText(value.toString());
            }
        } else {
            setText("");
        }
        
        return this;
    } // --- Fin del método getListCellRendererComponent ---

} // --- Fin de la clase NombreArchivoRenderer ---

