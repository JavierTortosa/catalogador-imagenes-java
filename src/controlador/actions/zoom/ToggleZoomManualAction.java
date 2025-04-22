package controlador.actions.zoom;

// --- TEXTO MODIFICADO ---
// Ya no necesitas Image
// import java.awt.Image;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils; // <-- Importar
// --- FIN MODIFICACION ---

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
        ImageIcon icon = iconUtils.getScaledIcon("3008-Zoom_48x48.png", width, height); // <-- 'Z' mayúscula

        // Verifica y asigna el icono
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
        } else {
            System.err.println("  -> ERROR: No se pudo cargar/escalar el icono '3008-Zoom_48x48.png' usando IconUtils.");
            // Opcional: putValue(Action.NAME, "Zoom");
        }
        // --- FIN DE LA PARTE IMPORTANTE ---
    }
    // --- FIN CONSTRUCTOR MODIFICADO ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller == null) {
             System.err.println("Error: Controller es null en ToggleZoomManualAction");
             return;
        }

        // Loguear primero
        controller.logActionInfo(e);

        // Determinar el nuevo estado deseado basándose en la fuente del evento
        boolean newState = false;
        Object source = e.getSource();
        if (source instanceof JCheckBoxMenuItem) {
            newState = ((JCheckBoxMenuItem) source).isSelected();
        } else if (source instanceof JButton) {
            // Si viene del botón, togglea el estado actual del modelo
            newState = !controller.isZoomManualCurrentlyEnabled();
        } else {
             // Otro tipo de componente? Togglear estado actual Action
             Object selectedValue = getValue(Action.SELECTED_KEY);
             newState = !(selectedValue instanceof Boolean && (Boolean)selectedValue);
             System.out.println("WARN [ToggleZoomManualAction]: Evento de fuente desconocida, toggleando estado actual.");
        }

        // Llama al método del controller que maneja la lógica
        controller.setManualZoomEnabled(newState);

        // Actualiza el estado SELECTED de esta propia Action
        // Es importante hacerlo DESPUÉS de llamar a setManualZoomEnabled por si esa
        // lógica también actualiza este valor (aunque en este caso no debería).
        // Asegura que el estado visual (checkbox/botón) se actualice si la lógica
        // del controlador no lo hizo explícitamente para el componente fuente.
        putValue(Action.SELECTED_KEY, newState);
    }
}