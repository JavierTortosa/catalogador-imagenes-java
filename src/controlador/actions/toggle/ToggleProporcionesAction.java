package controlador.actions.toggle;

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

public class ToggleProporcionesAction extends BaseVisorAction
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ToggleProporcionesAction (VisorController controller, IconUtils iconUtils, int width, int height) 
	{
		super("Mantener Proporciones", controller);
	
		// Cargar icono usando IconUtils
        // Asegúrate que el nombre del archivo PNG sea correcto
        ImageIcon icon = iconUtils.getScaledIcon("7002-Mantener_proporciones_48x48.png", width, height);
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
        } else {
            System.err.println("WARN [ProporcionesAction]: No se pudo cargar el icono 7002-Mantener_proporciones_48x48.png");
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
            System.out.println("TODO: Llamar a controller.aplicarZoomAlto() en ProporcionesAction");
        } else {
            System.err.println("Error: Controller es null en ProporcionesAction");
        }
	}
}

