package controlador.actions.especiales;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class PlaceholderToggleAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public PlaceholderToggleAction(String name) {
        super(name);
    } // --- Fin del constructor PlaceholderToggleAction ---

    @Override
    public void actionPerformed(ActionEvent e) {
        // 1. Mostrar el mensaje al usuario.
        JOptionPane.showMessageDialog(null, 
            "Funcionalidad para '" + getValue(Action.NAME) + "' aún no implementada.", 
            "En Desarrollo", 
            JOptionPane.INFORMATION_MESSAGE);
            
        // 2. Deseleccionar este mismo botón inmediatamente.
        //    Usamos invokeLater para asegurar que ocurra después de que el ButtonGroup
        //    haya terminado de procesar el clic inicial.
        SwingUtilities.invokeLater(() -> {
            putValue(Action.SELECTED_KEY, false);
        });

    } // --- Fin del método actionPerformed ---

} // --- Fin de la clase PlaceholderToggleAction ---