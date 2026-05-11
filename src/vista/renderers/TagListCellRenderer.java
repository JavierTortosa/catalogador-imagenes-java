package vista.renderers;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import modelo.datos.Tag;

/**
 * Un ListCellRenderer para mostrar correctamente objetos de tipo Tag en una JList.
 * Muestra el nombre del tag como el texto de la celda.
 */
public class TagListCellRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        // Llama al método de la clase padre para obtener la apariencia por defecto (colores de selección, etc.)
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        // Si el valor es una instancia de Tag, usamos su nombre como texto.
        if (value instanceof Tag) {
            Tag tag = (Tag) value;
            setText(tag.getNombre());
        }
        
        return this;
    } // ---FIN de metodo [getListCellRendererComponent]---

} // --- FIN de clase TagListCellRenderer ---