package controlador.actions.zoom;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils;

/**
 * Activa el zoom para que ajuste al maximo tamaño permitido tanto en alto como en ancho  
 * y tiene en cuenta si "Mantener Proporciones" esta activado o no
 */

public class ZoomFitAction extends BaseVisorAction
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ZoomFitAction (VisorController controller, IconUtils iconUtils, int width, int height) 
	{
		super("Escalar Para Ajustar", controller);
	
		// Cargar icono usando IconUtils
        // Asegúrate que el nombre del archivo PNG sea correcto
        ImageIcon icon = iconUtils.getScaledIcon("3005-escalar_para_ajustar_48x48.png", width, height);
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
        } else {
            System.err.println("WARN [ZoomFitAction]: No se pudo cargar el icono 3005-escalar_para_ajustar_48x48.png");
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
            System.out.println("TODO: Llamar a controller.aplicarZoomFit() en ZoomFitAction");
        } else {
            System.err.println("Error: Controller es null en ZoomFitAction");
        }
	}
}

