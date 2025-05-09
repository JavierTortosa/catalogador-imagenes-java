// --- Archivo Completo: principal/NombreArchivoRenderer.java ---
package vista.renderers; // O el paquete donde realmente esté

import java.awt.Component;
import java.awt.Color; // Importar Color
import java.util.Objects; // Importar Objects

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import vista.config.ViewUIConfig; // Importar ViewUIConfig

public class NombreArchivoRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;

    // Variables para guardar los colores del tema
    private final Color normalBackground;
    private final Color normalForeground;
    private final Color selectionBackground;
    private final Color selectionForeground;

    /**
     * Constructor que recibe la configuración de la UI para aplicar los colores del tema.
     * @param uiConfig La configuración de la UI que contiene los colores.
     */
    public NombreArchivoRenderer(ViewUIConfig uiConfig) {
        super(); // Llama al constructor de la superclase
        Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser null en NombreArchivoRenderer");

        // Guardar los colores relevantes del tema desde uiConfig
        this.normalBackground = uiConfig.colorFondoSecundario; // Fondo normal de la lista
        this.normalForeground = uiConfig.colorTextoPrimario;   // Texto normal de la lista
        this.selectionBackground = uiConfig.colorSeleccionFondo; // Fondo del item seleccionado
        this.selectionForeground = uiConfig.colorSeleccionTexto; // Texto del item seleccionado

         // Validación básica de colores (opcional pero recomendado)
         if (normalBackground == null || normalForeground == null || selectionBackground == null || selectionForeground == null) {
             System.err.println("WARN [NombreArchivoRenderer]: Uno o más colores recibidos de uiConfig son nulos. Se usarán defaults.");
             // Podrías asignar defaults de Swing aquí si fallan los del tema
         }
    }


    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
                                                  int index, boolean isSelected,
                                                  boolean cellHasFocus) {

        // 1. Llama al método de la superclase para obtener el JLabel configurado por defecto
        //    Esto maneja el texto, el foco, etc.
        Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        // 2. Extraer solo el nombre del archivo si 'value' es una ruta completa (opcional, como antes)
        String displayValue = value.toString();
        int lastSeparator = displayValue.lastIndexOf('/'); // O '\\' si usas separadores de Windows internamente
        if (lastSeparator != -1) {
            displayValue = displayValue.substring(lastSeparator + 1);
        }
        setText(displayValue); // Establecer el texto corto en el JLabel

        // 3. Aplicar los colores del tema guardados
        if (isSelected) {
            // Si está seleccionado, usar colores de selección del tema
            component.setBackground(this.selectionBackground);
            component.setForeground(this.selectionForeground);
        } else {
            // Si no está seleccionado, usar colores normales del tema
            component.setBackground(this.normalBackground);
            component.setForeground(this.normalForeground);
        }

        // FIXME Opcional: Manejar el borde de foco (a veces interfiere con los colores)
        // if (cellHasFocus) {
        //     setBorder(UIManager.getBorder("List.focusCellHighlightBorder"));
        // } else {
        //     setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1)); // O el borde que prefieras
        // }

        return component; // Devolver el componente JLabel modificado
    }
}
// --- FIN NombreArchivoRenderer ---