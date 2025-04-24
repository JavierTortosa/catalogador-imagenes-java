// --- Archivo: controlador/actions/vista/ToggleMenuBarAction.java ---
package controlador.actions.vista; // O el paquete donde la tengas

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem; // Importar

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
// No suele necesitar IconUtils para menú
// import vista.util.IconUtils;

public class ToggleToolBarAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L;
    // Clave de configuración para el estado de este toggle
    private static final String CONFIG_KEY = "interfaz.menu.vista.Barra_de_Menu.seleccionado";

    public ToggleToolBarAction(VisorController controller) {
        // --- TEXTO NUEVO ---
        super("ToggleToolBarAction Barra de Menu", controller); // Texto del menú
        putValue(Action.SHORT_DESCRIPTION, "Mostrar u ocultar la ToggleToolBarActionBarra de Menú");

        // --- Estado Inicial Seleccionado ---
        if (controller != null && controller.getConfigurationManager() != null) {
            boolean initialState = controller.getConfigurationManager().getBoolean(CONFIG_KEY, true); // Default a true (visible)
            putValue(Action.SELECTED_KEY, initialState);
            System.out.println("[ToggleToolBarAction] Estado inicial leído (" + CONFIG_KEY + "): " + initialState);
        } else {
             System.err.println("WARN [ToggleToolBarAction]: Controller o ConfigMgr nulos en constructor. Estado inicial no establecido.");
             putValue(Action.SELECTED_KEY, true); // Asumir visible por defecto si hay error
        }
        // --- FIN TEXTO NUEVO ---
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // --- TEXTO NUEVO ---
        if (controller == null) { /*...*/ return; }
        controller.logActionInfo(e);

        // Determinar el nuevo estado deseado
        boolean newState = false;
        Object source = e.getSource();
        if (source instanceof JCheckBoxMenuItem) {
            // El estado deseado es el estado actual del checkbox después del clic
            newState = ((JCheckBoxMenuItem) source).isSelected();
        } else {
             // Si se llama de otra forma (ej. atajo teclado), invertir estado actual de la Action
             Object selectedValue = getValue(Action.SELECTED_KEY);
             newState = !(selectedValue instanceof Boolean && (Boolean)selectedValue);
             System.out.println("WARN [ToggleToolBarAction]: Evento no es de JCheckBoxMenuItem, toggleando estado: " + newState);
        }

        // Llamar al método del controlador para aplicar el cambio y actualizar config
        controller.setComponenteVisibleAndUpdateConfig("ToggleToolBarActionBarra_de_Menu", newState);

        // Actualizar el estado de esta Action para sincronizar
        putValue(Action.SELECTED_KEY, newState);
        // --- FIN TEXTO NUEVO ---
    }
}