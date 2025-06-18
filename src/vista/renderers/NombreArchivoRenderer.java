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

//package vista.renderers; // O el paquete donde realmente esté
//
//import java.awt.Component;
//import java.awt.Color; // Importar Color
//import java.util.Objects; // Importar Objects
//
//import javax.swing.DefaultListCellRenderer;
//import javax.swing.JList;
//
//import vista.config.ViewUIConfig; // Importar ViewUIConfig
//
//public class NombreArchivoRenderer extends DefaultListCellRenderer {
//
//    private static final long serialVersionUID = 1L;
//
//    // Variables para guardar los colores del tema
//    private final Color normalBackground;
//    private final Color normalForeground;
//    private final Color selectionBackground;
//    private final Color selectionForeground;
//
//    /**
//     * Constructor que recibe la configuración de la UI para aplicar los colores del tema.
//     * @param uiConfig La configuración de la UI que contiene los colores.
//     */
//    public NombreArchivoRenderer(ViewUIConfig uiConfig) {
//        super(); // Llama al constructor de la superclase
//        Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser null en NombreArchivoRenderer");
//
//        // Guardar los colores relevantes del tema desde uiConfig
//        this.normalBackground = uiConfig.colorFondoSecundario; // Fondo normal de la lista
//        this.normalForeground = uiConfig.colorTextoPrimario;   // Texto normal de la lista
//        this.selectionBackground = uiConfig.colorSeleccionFondo; // Fondo del item seleccionado
//        this.selectionForeground = uiConfig.colorSeleccionTexto; // Texto del item seleccionado
//
//         // Validación básica de colores (opcional pero recomendado)
//         if (normalBackground == null || normalForeground == null || selectionBackground == null || selectionForeground == null) {
//             System.err.println("WARN [NombreArchivoRenderer]: Uno o más colores recibidos de uiConfig son nulos. Se usarán defaults.");
//             // Podrías asignar defaults de Swing aquí si fallan los del tema
//         }
//    }
//
//
//    @Override
//    public Component getListCellRendererComponent(JList<?> list, Object value,
//                                                  int index, boolean isSelected,
//                                                  boolean cellHasFocus) {
//
//        // 1. Llama al método de la superclase para obtener el JLabel configurado por defecto
//        //    Esto maneja el texto, el foco, etc.
//        Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
//
//        // 2. Extraer solo el nombre del archivo si 'value' es una ruta completa (opcional, como antes)
//        String displayValue = value.toString();
//        int lastSeparator = displayValue.lastIndexOf('/'); // O '\\' si usas separadores de Windows internamente
//        if (lastSeparator != -1) {
//            displayValue = displayValue.substring(lastSeparator + 1);
//        }
//        setText(displayValue); // Establecer el texto corto en el JLabel
//
//        // 3. Aplicar los colores del tema guardados
//        if (isSelected) {
//            // Si está seleccionado, usar colores de selección del tema
//            component.setBackground(this.selectionBackground);
//            component.setForeground(this.selectionForeground);
//        } else {
//            // Si no está seleccionado, usar colores normales del tema
//            component.setBackground(this.normalBackground);
//            component.setForeground(this.normalForeground);
//        }
//
//        // FIXME Opcional: Manejar el borde de foco (a veces interfiere con los colores)
//        // if (cellHasFocus) {
//        //     setBorder(UIManager.getBorder("List.focusCellHighlightBorder"));
//        // } else {
//        //     setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1)); // O el borde que prefieras
//        // }
//
//        return component; // Devolver el componente JLabel modificado
//    }
//}
//// --- FIN NombreArchivoRenderer ---