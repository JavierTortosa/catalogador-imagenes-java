package vista.renderers;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import vista.theme.Tema;
import vista.theme.ThemeManager;

/**
 * Un ListCellRenderer personalizado para la lista de nombres de archivo.
 * Este renderer es "inteligente" en cuanto a los temas: en lugar de almacenar
 * colores fijos, mantiene una referencia al ThemeManager para obtener los
 * colores del tema actual cada vez que se necesita pintar una celda.
 */
public class NombreArchivoRenderer extends DefaultListCellRenderer {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
     * La referencia al gestor de temas (la "venda transparente del pintor").
     * Esta es la única dependencia que necesita el renderer para la apariencia.
     */
    private final ThemeManager themeManager;

    /**
     * Constructor del renderer.
     * @param themeManager La instancia del gestor de temas de la aplicación.
     */
    public NombreArchivoRenderer(ThemeManager themeManager) {
        if (themeManager == null) {
            throw new IllegalArgumentException("ThemeManager no puede ser nulo en NombreArchivoRenderer");
        }
        this.themeManager = themeManager;
    } // --- Fin del constructor NombreArchivoRenderer ---

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
        // 1. Llamar al método de la clase padre para obtener un JLabel pre-configurado.
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        // 2. ¡LA MAGIA! Obtener el tema ACTUAL desde el ThemeManager.
        //    Esto se hace CADA VEZ que se pinta, asegurando que siempre usamos los colores correctos.
        Tema temaActual = this.themeManager.getTemaActual();

        // 3. Establecer el texto de la celda.
        setText(value.toString());

        // 4. Aplicar los colores del tema ACTUAL.
        if (isSelected) {
            // Si la celda está seleccionada, usar los colores de selección.
            setBackground(temaActual.colorSeleccionFondo());
            setForeground(temaActual.colorSeleccionTexto());
        } else {
            // Si no está seleccionada, usar los colores de fondo y texto normales.
            setBackground(temaActual.colorFondoSecundario());
            setForeground(temaActual.colorTextoPrimario());
        }
        
        // (Opcional) Puedes añadir más personalizaciones aquí, como bordes o iconos.
        // setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5)); // Ejemplo de padding

        // 5. Devolver el componente (este mismo JLabel) ya configurado para ser dibujado.
        return this;

    } // --- Fin del método getListCellRendererComponent ---

} // --- Fin de la clase NombreArchivoRenderer ---

