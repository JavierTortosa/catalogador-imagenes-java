package controlador.actions.zoom;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils;

/**
 * Activa el zoom para visualizar la imagen ajustada al alto maximo
 * y tiene en cuenta si "Mantener Proporciones" esta activado o no
 */

public class ZoomAltoAction extends BaseVisorAction
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ZoomAltoAction (VisorController controller, IconUtils iconUtils, int width, int height) 
	{
		super("Zoom a lo Alto", controller);
	
		// Cargar icono usando IconUtils
        // Asegúrate que el nombre del archivo PNG sea correcto
        ImageIcon icon = iconUtils.getScaledIcon("3004-ajustar_al_alto_48x48.png", width, height);
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
        } else {
            System.err.println("WARN [ZoomAltoAction]: No se pudo cargar el icono 3004-ajustar_al_alto_48x48.png");
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
            System.out.println("TODO: Llamar a controller.aplicarZoomAlto() en ZoomAltoAction");
        } else {
            System.err.println("Error: Controller es null en ZoomAltoAction");
        }
	}
}

