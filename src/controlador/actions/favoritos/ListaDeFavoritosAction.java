package controlador.actions.favoritos;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils;

@SuppressWarnings ("serial")
public class ListaDeFavoritosAction extends BaseVisorAction {

	public ListaDeFavoritosAction(VisorController controller, IconUtils iconUtils, int width, int height) {

		// TODO Metodo ListaDeFavoritosAction pendiente de implementar
	
		// Texto para menú (si lo añades) o tooltip
		super("Imagenes Favoritas", controller);
		putValue(Action.SHORT_DESCRIPTION, "Muestra las imagenes favoritas");

		// Cargar icono usando IconUtils
		// Asegúrate que el nombre del archivo PNG sea correcto
		ImageIcon icon = iconUtils.getScaledIcon("5003-marcar_imagen_48x48.png", width, height);
		if (icon != null) {
			putValue(Action.SMALL_ICON, icon);
		} else {
			System.err.println("WARN [ListaDeFavoritosAction]: No se pudo cargar el icono 5003-marcar_imagen_48x48.png");
			putValue(Action.NAME, "<->"); // Texto fallback
		}
	}	
	
	 @Override
	 public void actionPerformed(ActionEvent e) {
	     // Loguear
	     if (controller != null) {
	         controller.logActionInfo(e);
	     } else {
	          System.err.println("Error: Controller es null en ListaDeFavoritosAction");
	          return;
	     }

	     // Llamar al método del controlador que realiza la acción
	     //controller.aplicarVolteoHorizontal(); 
	     System.out.println("ListaDeFavoritosAction pendiente de llamar al método del controlador que realiza la acción");
	 }
} // --- FIN ListaDeFavoritosAction
