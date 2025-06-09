package vista.builders;

import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenu;

@SuppressWarnings ("serial")
public class JCheckBoxMenu extends JMenu {
    private final JCheckBox checkbox;

    public JCheckBoxMenu(Action action) {
        // PASO 1: Llamar al constructor de JMenu SIN la acción, solo con el texto.
        // Esto evita que llame a nuestro setAction sobreescrito prematuramente.
        super(action.getValue(Action.NAME).toString());
        
        // PASO 2: Crear el JCheckBox AHORA, ANTES de hacer cualquier otra cosa.
        this.checkbox = new JCheckBox();
        this.checkbox.setOpaque(false);
        this.checkbox.setFocusable(false);
        this.checkbox.setText("Mostrar " + action.getValue(Action.NAME).toString());
        
        // PASO 3: Configurar el layout y añadir los componentes.
        setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        
        
        add(this.checkbox);
        add(new JLabel("Botones Visibles"));
        addSeparator();
        // Ya no añadimos una JLabel, porque el texto ya lo tiene el propio JMenu gracias a super(...)

        // PASO 4: AHORA sí, asignamos la Action completa.
        // En este punto, 'this.checkbox' ya existe y no será null.
        setAction(action); 
    }

    private void actualizarEstadoSeleccion() {
        Object selectedValue = getAction().getValue(Action.SELECTED_KEY);
        checkbox.setSelected(Boolean.TRUE.equals(selectedValue));
    }

    // Sobreescribimos setAction para asegurar que todo se reconfigure si cambia la acción
    @Override
    public void setAction(Action a) {
        // No es la implementación más robusta, pero para nuestro caso funciona.
        // Una implementación completa requeriría quitar listeners antiguos, etc.
        super.setAction(a);
        if (a != null) {
             actualizarEstadoSeleccion();
        }
    }
}