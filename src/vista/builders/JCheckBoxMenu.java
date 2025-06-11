package vista.builders;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

/**
 * Un JMenu que se comporta como un JCheckBoxMenuItem.
 * Contiene un JCheckBoxMenuItem como primer elemento para controlar su estado
 * y actuar como la "cabeza" visible del menú.
 */
@SuppressWarnings("serial")
public class JCheckBoxMenu extends JMenu {

    private final JCheckBoxMenuItem headerCheckBox;

    public JCheckBoxMenu(Action action) {
        // 1. El JMenu principal toma el texto de la acción.
        super((String) action.getValue(Action.NAME));

        // 2. Creamos un JCheckBoxMenuItem que actuará como la cabecera visible
        //    y el controlador del estado.
        this.headerCheckBox = new JCheckBoxMenuItem(action);
        
        // Sincronizamos el estado inicial del checkbox con el de la acción.
        boolean isSelected = Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY));
        this.headerCheckBox.setState(isSelected);
        
        // Asignamos la misma acción al JCheckBoxMenuItem.
        // Al hacer clic, se ejecutará el actionPerformed de la Action.
        this.headerCheckBox.setAction(action);
        
        // 3. Añadimos la cabecera al menú y un separador.
        this.add(this.headerCheckBox);
        this.addSeparator();
    }
    
    // Método para obtener la cabecera, por si es necesario.
    public JCheckBoxMenuItem getHeaderCheckBox() {
        return headerCheckBox;
    }
}// --- FIN de la clase JCheckBoxMenu ---