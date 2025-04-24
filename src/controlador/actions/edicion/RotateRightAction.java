package controlador.actions.edicion;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils;

public class RotateRightAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L;

    public RotateRightAction(VisorController controller, IconUtils iconUtils, int width, int height) {
        super("Girar Derecha", controller); // Texto para menú
        putValue(Action.SHORT_DESCRIPTION, "Girar la imagen 90 grados a la derecha");

        // Cargar icono
        ImageIcon icon = iconUtils.getScaledIcon("2002-Rotar_Derecha_48x48.png", width, height);
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
        } else {
            System.err.println("WARN [RotateRightAction]: No se pudo cargar el icono 2002-Rotar_Derecha_48x48.png");
            // putValue(Action.NAME, "RotR"); // Fallback
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller != null) {
            controller.logActionInfo(e);
            controller.aplicarRotarDerecha(); // Llama al método correcto del controller
        } else {
             System.err.println("Error: Controller es null en RotateRightAction");
        }
    }
}