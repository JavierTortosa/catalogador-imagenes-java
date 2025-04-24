package controlador.actions.edicion;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils;

public class RotateLeftAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L;

    public RotateLeftAction(VisorController controller, IconUtils iconUtils, int width, int height) {
        super("Girar Izquierda", controller); // Texto para menú
        putValue(Action.SHORT_DESCRIPTION, "Girar la imagen 90 grados a la izquierda");

        // Cargar icono
        ImageIcon icon = iconUtils.getScaledIcon("2001-Rotar_Izquierda_48x48.png", width, height);
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
        } else {
            System.err.println("WARN [RotateLeftAction]: No se pudo cargar el icono 2001-Rotar_Izquierda_48x48.png");
            // putValue(Action.NAME, "RotL"); // Fallback
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller != null) {
            controller.logActionInfo(e);
            controller.aplicarRotarIzquierda(); // Llama al método correcto del controller
        } else {
             System.err.println("Error: Controller es null en RotateLeftAction");
        }
    }
}