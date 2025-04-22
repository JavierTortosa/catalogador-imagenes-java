package controlador.actions.navegacion;

import java.awt.Image;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.VisorController;
import controlador.actions.BaseVisorAction;

public class NextImageAction extends BaseVisorAction 
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NextImageAction(VisorController controller, int anchoIcono, int altoIcono) 
	{
        super("Siguiente", controller);
        putValue(Action.SHORT_DESCRIPTION, "Ir a la siguiente imagen");

        String iconos = "/iconos";
        String tema = "/black";
        String nombreIcono = "/1003-Siguiente_48x48.png";
        String iconPath = iconos + tema + nombreIcono;
        
        try {
            java.net.URL iconUrl = getClass().getResource(iconPath);//"/iconos/1003-Siguiente_48x48.png");
            if (iconUrl != null) {
                ImageIcon icon = new ImageIcon(iconUrl);
                // --- MODIFICADO: Usar ancho y alto recibidos ---
                int altoFinal = (altoIcono <= 0) ? -1 : altoIcono; // Usar -1 si alto<=0
                Image scaledImg = icon.getImage().getScaledInstance(anchoIcono, altoFinal, Image.SCALE_SMOOTH);
                putValue(Action.SMALL_ICON, new ImageIcon(scaledImg));
                // ---------------------------------------------
            } else { System.err.println("WARN [NextImageAction]: Icono no encontrado."); }
        } catch (Exception e) { System.err.println("ERROR cargando icono para NextImageAction: " + e); }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller != null) {
            controller.navegarImagen(1); // Llama al método público del controller
        }
    }
}