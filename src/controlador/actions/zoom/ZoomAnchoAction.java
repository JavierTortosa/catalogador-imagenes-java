package controlador.actions.zoom;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils;

/**
 * Activa el zoom para que ajuste al maximo tamaño permitido tanto en ancho 
 * y tiene en cuenta si "Mantener Proporciones" esta activado o no
 */

public class ZoomAnchoAction extends BaseVisorAction
{
	/**
	 * Activa el zoom para visualizar la imagen ajustada al ancho maximo
	 * y tiene en cuenta si "Mantener Proporciones" esta activado o no
	 */
	private static final long serialVersionUID = 1L;

	public ZoomAnchoAction (VisorController controller, IconUtils iconUtils, int width, int height) 
	{
		super("Zoom a lo Ancho", controller);
	
		// Cargar icono usando IconUtils
        // Asegúrate que el nombre del archivo PNG sea correcto
        ImageIcon icon = iconUtils.getScaledIcon("3003-ajustar_al_ancho_48x48.png", width, height);
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
        } else {
            System.err.println("WARN [ZoomAnchoAction]: No se pudo cargar el icono 3003-ajustar_al_ancho_48x48.png");
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
            System.out.println("TODO: Llamar a controller.aplicarZoomAncho() en ZoomAnchoAction");
        } else {
            System.err.println("Error: Controller es null en ZoomAnchoAction");
        }
	}
}

