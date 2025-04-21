package controlador.actions.zoom;

import java.awt.Image;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import controlador.VisorController;
import controlador.actions.BaseVisorAction;

public class ToggleZoomManualAction extends BaseVisorAction {

    public ToggleZoomManualAction(VisorController controller) {
        super("Activar Zoom Manual", controller); // Texto para menú/checkbox
        putValue(Action.SHORT_DESCRIPTION, "Activar/desactivar zoom y desplazamiento manual");
        // La propiedad SELECTED_KEY manejará el estado del check
        // El estado inicial se leerá de config y se pondrá en initializeActions

        try {
            java.net.URL iconUrl = getClass().getResource("/iconos/08-Zoom_48x48.png");
            if (iconUrl != null) {
                ImageIcon icon = new ImageIcon(iconUrl);
                Image scaledImg = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                putValue(Action.SMALL_ICON, new ImageIcon(scaledImg));
            } else { System.err.println("WARN [ToggleZoomManualAction]: Icono no encontrado."); }
        } catch (Exception e) { System.err.println("ERROR cargando icono para ToggleZoomManualAction: " + e); }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller == null) return;

        // Determinar el nuevo estado deseado basándose en la fuente del evento
        boolean newState = false;
        Object source = e.getSource();
        if (source instanceof JCheckBoxMenuItem) {
            newState = ((JCheckBoxMenuItem) source).isSelected();
        } else if (source instanceof JButton) {
            // Si viene del botón, togglea el estado actual del modelo
            newState = !controller.isZoomManualCurrentlyEnabled(); // Necesitarás un getter en Controller para el estado del modelo
        } else {
             // Otro tipo de componente? Tomar estado actual de la Action?
             Object selectedValue = getValue(Action.SELECTED_KEY);
             newState = !(selectedValue instanceof Boolean && (Boolean)selectedValue); // Toggle estado actual Action
             System.out.println("WARN [ToggleZoomManualAction]: Evento de fuente desconocida, toggleando estado actual.");
        }


        // Llama al método del controller que maneja la lógica y actualiza otras Actions/UI
        controller.setManualZoomEnabled(newState);

        // Actualiza el estado SELECTED de esta propia Action para mantener sincronizados
        // los checkboxes/togglebuttons que la usen.
        putValue(Action.SELECTED_KEY, newState);
    }
}