// --- INICIO CÓDIGO A MODIFICAR en ToggleSubfoldersAction.java ---
package controlador.actions.toggle;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.ImageIcon;
// Quitar import de JRadioButtonMenuItem si ya no se usa
// import javax.swing.JRadioButtonMenuItem;

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils;

public class ToggleSubfoldersAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L;
    private static final String CONFIG_KEY = "comportamiento.carpeta.cargarSubcarpetas";

    public ToggleSubfoldersAction(VisorController controller, IconUtils iconUtils, int width, int height) {
        // El nombre aquí ya no importa tanto para los radios, pero sí para el botón si no tiene icono/tooltip
        super("Alternar Subcarpetas", controller); // Cambiado para claridad
        putValue(Action.SHORT_DESCRIPTION, "Alternar mostrar solo carpeta actual o incluir subcarpetas");

        ImageIcon icon = iconUtils.getScaledIcon("7001-Subcarpetas_48x48.png", width, height);
        if (icon != null) { putValue(Action.SMALL_ICON, icon); }
        else { /*...*/ }

        // Estado Inicial (Leer de config) - SIN CAMBIOS
        if (controller != null && controller.getConfigurationManager() != null) {
            boolean initialState = controller.getConfigurationManager().getBoolean(CONFIG_KEY, true);
            putValue(Action.SELECTED_KEY, initialState);
            System.out.println("[ToggleSubfoldersAction] Estado inicial leído (" + CONFIG_KEY + "): " + initialState);
        } else { /*...*/ }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller == null) { /*...*/ return; }
        controller.logActionInfo(e); // Sigue siendo útil

        // SIMPLIFICADO: Asumimos que si esta Action se dispara, es porque se quiere
        // alternar el estado actual. Los radios ahora tienen sus propios listeners.
        boolean estadoActualAction = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        boolean estadoDeseado = !estadoActualAction; // Simplemente alternar

        // Llamar al controlador para que haga todo
        System.out.println("[ToggleSubfoldersAction] Solicitando cambio de estado (desde botón) a: " + estadoDeseado);
        controller.setMostrarSubcarpetasAndUpdateConfig(estadoDeseado);
    }
}
// --- FIN CÓDIGO A MODIFICAR en ToggleSubfoldersAction.java ---