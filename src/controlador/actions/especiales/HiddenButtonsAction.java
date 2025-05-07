package controlador.actions.especiales;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils;

@SuppressWarnings ("serial")
public class HiddenButtonsAction extends BaseVisorAction {

	public HiddenButtonsAction(VisorController controller, IconUtils iconUtils, int width, int height) {

		// TODO Metodo HiddenButtonsAction pendiente de implementar
	
		// Texto para menú (si lo añades) o tooltip
		super("Botones Ocultos", controller);
		putValue(Action.SHORT_DESCRIPTION, "Muestra los botones ocultos");

		// Cargar icono usando IconUtils
		// Asegúrate que el nombre del archivo PNG sea correcto
		ImageIcon icon = iconUtils.getScaledIcon("6003-Botones_Ocultos_48x48.png", width, height);
		if (icon != null) {
			putValue(Action.SMALL_ICON, icon);
		} else {
			System.err.println("WARN [HiddenButtonsAction]: No se pudo cargar el icono 6003-Botones_Ocultos_48x48.png");
			putValue(Action.NAME, "<->"); // Texto fallback
		}
	}	
	
	 @Override
	 public void actionPerformed(ActionEvent e) {
	     // Loguear
	     if (controller != null) {
	         controller.logActionInfo(e);
	     } else {
	          System.err.println("Error: Controller es null en HiddenButtonsAction");
	          return;
	     }

	     // Llamar al método del controlador que realiza la acción
	     //controller.aplicarVolteoHorizontal(); 
	     System.out.println("HiddenButtonsAction pendiente de llamar al método del controlador que realiza la acción");
	 }
} // --- FIN HiddenButtonsAction
