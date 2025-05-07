// --- Archivo Completo: controlador/actions/edicion/FlipHorizontalAction.java ---
package controlador.actions.edicion; // Asegúrate que el paquete sea correcto

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils;

public class FlipHorizontalAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor para la acción de volteo horizontal.
     */
    public FlipHorizontalAction(VisorController controller, IconUtils iconUtils, int width, int height) {
        // Texto para menú (si lo añades) o tooltip
        super("Voltear Horizontal", controller);
        putValue(Action.SHORT_DESCRIPTION, "Voltear la imagen horizontalmente (efecto espejo)");

        // Cargar icono usando IconUtils
        // Asegúrate que el nombre del archivo PNG sea correcto
        ImageIcon icon = iconUtils.getScaledIcon("2003-Espejo_Horizontal_48x48.png", width, height);
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
        } else {
            System.err.println("WARN [FlipHorizontalAction]: No se pudo cargar el icono 2003-Espejo_Horizontal_48x48.png");
            putValue(Action.NAME, "<->"); // Texto fallback
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Loguear
        if (controller != null) {
            controller.logActionInfo(e);
        } else {
             System.err.println("Error: Controller es null en FlipHorizontalAction");
             return;
        }

        // Llamar al método del controlador que realiza la acción
        controller.aplicarVolteoHorizontal(); // Llama al método que ya tenías en el controller
    }
}// --- FIN FlipHorizontalAction ---