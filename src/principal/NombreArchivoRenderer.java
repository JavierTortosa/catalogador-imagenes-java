package principal;

import javax.swing.*;
import java.awt.*;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NombreArchivoRenderer extends DefaultListCellRenderer {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
                                                  int index, boolean isSelected,
                                                  boolean cellHasFocus) {
        // Llama al método de la superclase para obtener el JLabel configurado por defecto
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        // El 'value' es la ruta relativa (String) que almacenamos en el modelo
        if (value instanceof String) {
            String rutaRelativa = (String) value;
            try {
                // Obtenemos el nombre del archivo de la ruta
                Path path = Paths.get(rutaRelativa);
                label.setText(path.getFileName().toString()); // ¡Mostrar solo el nombre!
            } catch (InvalidPathException e) {
                // En caso de error, mostrar la ruta original
                label.setText(rutaRelativa);
                System.err.println("Error al parsear ruta en Renderer: " + rutaRelativa);
            }
        }
        return label;
    }
}


