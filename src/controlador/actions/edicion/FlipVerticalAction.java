package controlador.actions.edicion;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils;

public class FlipVerticalAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L;

    public FlipVerticalAction(VisorController controller, IconUtils iconUtils, int width, int height) {
        super("Voltear Vertical", controller); // Texto para menú
        putValue(Action.SHORT_DESCRIPTION, "Voltear la imagen verticalmente");

        // Cargar icono
        ImageIcon icon = iconUtils.getScaledIcon("2004-Espejo_Vertical_48x48.png", width, height);
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
        } else {
            System.err.println("WARN [FlipVerticalAction]: No se pudo cargar el icono 2004-Espejo_Vertical_48x48.png");
            // putValue(Action.NAME, "VFlip"); // Fallback
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller != null) {
            controller.logActionInfo(e);
            controller.aplicarVolteoVertical(); // Llama al método correcto del controller
        } else {
             System.err.println("Error: Controller es null en FlipVerticalAction");
        }
    }
}
