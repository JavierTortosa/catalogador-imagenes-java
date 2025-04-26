// --- INICIO CÓDIGO A VERIFICAR/COMPLETAR en controlador/actions/toggle/ToggleProporcionesAction1.java ---
package controlador.actions.toggle;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem; // Puede ser útil si necesitas distinguir la fuente

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils;

public class ToggleProporcionesAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L;
    // Clave de configuración para el estado seleccionado del menú
    private static final String CONFIG_KEY_SELECTED = "interfaz.menu.zoom.Mantener_Proporciones.seleccionado";
    // Clave de configuración para el estado activado (enabled) - Opcional si siempre está activo
    // private static final String CONFIG_KEY_ENABLED = "interfaz.menu.zoom.Mantener_Proporciones.activado";

    public ToggleProporcionesAction (VisorController controller, IconUtils iconUtils, int width, int height) {
        super("Mantener Proporciones", controller); // Nombre base
        putValue(Action.SHORT_DESCRIPTION, "Alternar mantener relación de aspecto al escalar/zoomear");

        ImageIcon icon = iconUtils.getScaledIcon("7002-Mantener_Proporciones_48x48.png", width, height);
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
        } else { /* Mensaje error o fallback */ }

        // --- LEER ESTADO INICIAL ---
        if (controller != null && controller.getConfigurationManager() != null) {
            // Leer estado SELECCIONADO
            boolean initialStateSelected = controller.getConfigurationManager().getBoolean(CONFIG_KEY_SELECTED, true); // Default a TRUE
            putValue(Action.SELECTED_KEY, initialStateSelected);
            System.out.println("[ToggleProporcionesAction1] Estado inicial SELECTED leído (" + CONFIG_KEY_SELECTED + "): " + initialStateSelected);

            // Leer estado HABILITADO (opcional, si puede deshabilitarse)
            // boolean initialStateEnabled = controller.getConfigurationManager().getBoolean(CONFIG_KEY_ENABLED, true);
            // setEnabled(initialStateEnabled);
            // System.out.println("[ToggleProporcionesAction1] Estado inicial ENABLED leído (" + CONFIG_KEY_ENABLED + "): " + initialStateEnabled);

        } else {
            System.err.println("ERROR [ToggleProporcionesAction1]: Controller o ConfigManager nulos al inicializar.");
            putValue(Action.SELECTED_KEY, true); // Default fallback a TRUE
            // setEnabled(true); // Habilitado por defecto si hay error
        }
        // --------------------------
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller == null) { /* Error */ return; }
        controller.logActionInfo(e); // Loguear

        // Determinar el estado deseado. Como es un toggle simple (checkbox o botón),
        // el nuevo estado deseado es simplemente el opuesto al actual de la Action.
        boolean estadoActualAction = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        boolean estadoDeseado = !estadoActualAction;

        System.out.println("[ToggleProporcionesAction1] Solicitando cambio de estado a: " + estadoDeseado);
        controller.setMantenerProporcionesAndUpdateConfig(estadoDeseado);
    }
}
// --- FIN CÓDIGO A VERIFICAR/COMPLETAR en controlador/actions/toggle/ToggleProporcionesAction1.java ---