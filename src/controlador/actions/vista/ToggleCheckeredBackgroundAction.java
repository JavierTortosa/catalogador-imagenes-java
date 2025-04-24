// --- Archivo: controlador/actions/vista/ToggleCheckeredBackgroundAction.java ---
package controlador.actions.vista;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import controlador.VisorController;
import controlador.actions.BaseVisorAction;

public class ToggleCheckeredBackgroundAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L;
    private static final String CONFIG_KEY = "interfaz.menu.vista.Fondo_a_Cuadros.seleccionado";

    public ToggleCheckeredBackgroundAction(VisorController controller) {
        super("Fondo a Cuadros", controller);
        putValue(Action.SHORT_DESCRIPTION, "Mostrar/ocultar patrón a cuadros para transparencias");

        // Estado inicial
        if (controller != null && controller.getConfigurationManager() != null) {
            boolean initialState = controller.getConfigurationManager().getBoolean(CONFIG_KEY, false); // Default a false (no visible)
            putValue(Action.SELECTED_KEY, initialState);
             System.out.println("[ToggleCheckeredBG] Estado inicial leído (" + CONFIG_KEY + "): " + initialState);
        } else { /*...*/ putValue(Action.SELECTED_KEY, false); }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller == null) { /*...*/ return; }
        controller.logActionInfo(e);

        boolean newState = false;
        if (e.getSource() instanceof JCheckBoxMenuItem) {
            newState = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        } else { /* ... toggle ... */
             Object selectedValue = getValue(Action.SELECTED_KEY);
             newState = !(selectedValue instanceof Boolean && (Boolean)selectedValue);
        }

        // Llamar al método del controlador pasando el IDENTIFICADOR correcto
        controller.setComponenteVisibleAndUpdateConfig("Fondo_a_Cuadros", newState);

        // Actualizar estado de la Action
        putValue(Action.SELECTED_KEY, newState);
    }
}