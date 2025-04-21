// EN: controlador.actions.PreviousImageAction.java
package controlador.actions.navegacion;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.net.URL; // Importar URL explícitamente

import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import controlador.actions.BaseVisorAction;

public class PreviousImageAction extends BaseVisorAction {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PreviousImageAction(VisorController controller, int anchoIcono, int altoIcono) {
        super("Anterior", controller);
        putValue(Action.SHORT_DESCRIPTION, "Ir a la imagen anterior");
        System.out.println("[PreviousImageAction] Constructor iniciado.");

        // --- DECLARAR url ANTES del try ---
        URL iconUrl = null; // Inicializar a null
        String iconPath = "/iconos/01-Anterior_48x48.png"; // Guardar ruta para logs

        try {
            System.out.println("  -> Intentando cargar icono desde: " + iconPath);
            // --- ASIGNAR valor dentro del try ---
            iconUrl = getClass().getResource(iconPath);

            if (iconUrl != null) {
                System.out.println("  -> URL del icono encontrada: " + iconUrl);
                // Crear ImageIcon DESPUÉS de confirmar que url no es null
                ImageIcon icon = new ImageIcon(iconUrl);
                if (icon.getImageLoadStatus() == java.awt.MediaTracker.COMPLETE) {
                    System.out.println("  -> ImageIcon cargado correctamente (Estado: COMPLETO).");
                    int altoFinal = (altoIcono <= 0) ? -1 : altoIcono;
                    Image scaledImg = icon.getImage().getScaledInstance(anchoIcono, altoFinal, Image.SCALE_SMOOTH);
                    putValue(Action.SMALL_ICON, new ImageIcon(scaledImg));
                    System.out.println("  -> Icono escalado y asignado a SMALL_ICON.");
                } else {
                    System.err.println("  -> ERROR: ImageIcon NO cargado correctamente (Estado: " + icon.getImageLoadStatus() + ")");
                }
            } else {
                 // Ahora iconUrl es null aquí fuera del if, pero la variable existe
                 System.err.println("  -> ERROR: Icono no encontrado en la ruta: " + iconPath);
            }
        } catch (Exception e) {
            // iconUrl puede o no ser null aquí, pero la variable existe
            System.err.println("  -> ERROR EXCEPCIÓN cargando icono para PreviousImageAction: " + e.getMessage());
            e.printStackTrace();
        }
        // iconUrl existe aquí fuera (puede ser null o tener valor)
        System.out.println("[PreviousImageAction] Constructor finalizado.");
    }

//    @Override
//    public void actionPerformed(ActionEvent e) {
//        if (controller != null) {
//            controller.navegarImagen(-1);
//        }
//    }
	
	 @Override
	    public void actionPerformed(ActionEvent e) {
	        // --- LLAMAR AL logActionInfo ANTES DE LA LÓGICA ---
	        if (controller != null) {
	            // Llama al método público (o hazlo protected en BaseVisorAction/VisorController)
	            controller.logActionInfo(e); // Necesita que logActionInfo sea accesible
	        }
	        // -----------------------------------------

	        // Lógica específica de la acción
	        if (controller != null) {
	            controller.navegarImagen(-1);
	        } else {
	             System.err.println("Error: Controller es null en PreviousImageAction");
	        }
	    }
}