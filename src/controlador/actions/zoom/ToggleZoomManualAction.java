package controlador.actions.zoom;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils; // <-- Importar

public class ToggleZoomManualAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L;
    // Opcional: private IconUtils iconUtils;

    // --- TEXTO MODIFICADO: Constructor CORRECTO ---
    public ToggleZoomManualAction(VisorController controller, IconUtils iconUtils, int width, int height) {
        // Llama al constructor de la superclase
        super("Activar Zoom Manual", controller);

        // Establece descripción
        putValue(Action.SHORT_DESCRIPTION, "Activar/desactivar zoom y desplazamiento manual");
        // El estado inicial SELECTED_KEY se pone en initializeActions

        // --- ¡LA PARTE IMPORTANTE! Usa IconUtils ---
        // Llama a getScaledIcon con el nombre del icono CORRECTO (con Z mayúscula)
        // y los tamaños recibidos
        ImageIcon icon = iconUtils.getScaledIcon("3001-Zoom_48x48.png", width, height); // <-- 'Z' mayúscula

        // Verifica y asigna el icono
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
        } else {
            System.err.println("  -> ERROR: No se pudo cargar/escalar el icono '3008-Zoom_48x48.png' usando IconUtils.");
            // Opcional: putValue(Action.NAME, "Zoom");
        }
        // --- FIN DE LA PARTE IMPORTANTE ---
    } // --- FIN CONSTRUCTOR ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller == null) { // Buena práctica chequear controller
//            controller.logActionInfo(e);
//        } else {
             System.err.println("Error: Controller es null en ToggleZoomManualAction");
             return;
        }

        // Loguear primero
        controller.logActionInfo(e);

        // Determinar el nuevo estado deseado basándose en la fuente del evento
        boolean newState;// = false;
        Object source = e.getSource();
        
        if (source instanceof JCheckBoxMenuItem) {
            newState = ((JCheckBoxMenuItem) source).isSelected();
            System.out.println("  [ToggleZoomManualAction] Evento desde JCheckBoxMenuItem. Nuevo estado (isSelected): " + newState);
        } else if (source instanceof JButton) {
            // Si viene del botón, togglea el estado actual del modelo
            newState = !controller.isZoomManualCurrentlyEnabled();
            System.out.println("  [ToggleZoomManualAction] Evento desde JButton. Estado actual del modelo: "
                    + controller.isZoomManualCurrentlyEnabled() + ". Nuevo estado deseado: " + newState);
        } else {
             // Otro tipo de componente? Togglear estado actual Action
             Object selectedValue = getValue(Action.SELECTED_KEY);
             boolean currentState = (selectedValue instanceof Boolean && (Boolean)selectedValue);
             newState = !currentState;
             //newState = !(selectedValue instanceof Boolean && (Boolean)selectedValue);
             System.out.println("WARN [ToggleZoomManualAction]: Evento de fuente desconocida ("
                     + (source != null ? source.getClass().getName() : "null")
                     + "). Toggleando estado actual de la Action. Nuevo estado: " + newState);
        }

        // --- 3. Llamar al Método del Controller ---
        // Llama al método del controller que maneja la lógica
        controller.setManualZoomEnabled(newState);

        
        // --- 4. Actualizar Estado SELECTED_KEY de Esta Action ---
        // Esto asegura que los componentes Swing vinculados (checkbox y botón toggle)
        // reflejen visualmente el 'newState' que acabamos de procesar.
        // setManualZoomEnabled en el controller también podría actualizar el SELECTED_KEY
        // de esta Action (lo cual es bueno para centralizar), pero hacerlo aquí
        // también como una "sincronización final" es seguro.
        putValue(Action.SELECTED_KEY, newState);
        System.out.println("  [ToggleZoomManualAction] Estado Action.SELECTED_KEY actualizado a: " + getValue(Action.SELECTED_KEY));
    }
    
}