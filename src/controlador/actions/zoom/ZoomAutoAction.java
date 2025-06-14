package controlador.actions.zoom;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils;

/**
 * Activa el zoom para que la imagen se vea tal como la original  
 * y NO tiene en cuenta si "Mantener Proporciones" esta activado o no
 */

public class ZoomAutoAction extends BaseVisorAction
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ZoomAutoAction (VisorController controller, IconUtils iconUtils, int width, int height) 
	{
		super("Zoom Automático", controller);
	
		// Cargar icono usando IconUtils
        // Asegúrate que el nombre del archivo PNG sea correcto
        ImageIcon icon = iconUtils.getScaledIcon("3002-Zoom_Auto_48x48.png", width, height);
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
        } else {
            System.err.println("WARN [FlipHorizontalAction]: No se pudo cargar el icono 2003-Espejo_Horizontal_48x48.png");
            putValue(Action.NAME, "<->"); // Texto fallback
        }
	}

	@Override
	public void actionPerformed (ActionEvent e)
	{
		if (controller != null) {
            controller.logActionInfo(e);
            // Llamar a un método en VisorController que implemente la lógica del Zoom Automático
            // Ejemplo: controller.aplicarZoomAutomatico();
            System.out.println("TODO: Llamar a controller.aplicarZoomAutomatico() en ZoomAutoAction");
        } else {
            System.err.println("Error: Controller es null en ZoomAutoAction");
        }
	}
}

