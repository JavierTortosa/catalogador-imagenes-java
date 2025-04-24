package controlador.actions.zoom;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils;

/**
 * Activa el zoom y establece el zoom actual para escalar las siguientes imagenes que se vean
 * y NO tiene en cuenta si "Mantener Proporciones" esta activado o no
 */

public class ZoomFixedAction extends BaseVisorAction
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ZoomFixedAction (VisorController controller, IconUtils iconUtils, int width, int height) 
	{
		super("Fijar Zoom Actual", controller);
	
		// Cargar icono usando IconUtils
        // Asegúrate que el nombre del archivo PNG sea correcto
        ImageIcon icon = iconUtils.getScaledIcon("3006-zoom_fijo_48x48.png", width, height);
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
        } else {
            System.err.println("WARN [ZoomFixedAction]: No se pudo cargar el icono 3006-zoom_fijo_48x48.png");
            putValue(Action.NAME, "<->"); // Texto fallback
        }
	}

	@Override
	public void actionPerformed (ActionEvent e)
	{
		if (controller != null) {
            controller.logActionInfo(e);
            // Llamar a un método en VisorController que implemente la lógica del Zoom 
            // Ejemplo: controller.aplicarZoomAutomatico();
            System.out.println("TODO: Llamar a controller.aplicarZoomFixed() en ZoomFixedAction");
        } else {
            System.err.println("Error: Controller es null en ZoomFixedAction");
        }
	}
}

