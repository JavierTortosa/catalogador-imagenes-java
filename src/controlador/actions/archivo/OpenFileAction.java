package controlador.actions.archivo;

import java.awt.Image;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils;

public class OpenFileAction extends BaseVisorAction {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public OpenFileAction(VisorController controller, IconUtils iconUtils, int width, int height) {
        super("Abrir Archivo...", controller);
        putValue(Action.SHORT_DESCRIPTION, "Abrir una nueva carpeta de imágenes");

        try {
            // Icono para el botón "Selector_de_Carpetas"
            java.net.URL iconUrl = getClass().getResource("/iconos/6024-Selector_de_Carpetas_48x48.png");
            if (iconUrl != null) {
                ImageIcon icon = new ImageIcon(iconUrl);
                Image scaledImg = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                putValue(Action.SMALL_ICON, new ImageIcon(scaledImg));
            } else { System.err.println("WARN [OpenFileAction]: Icono no encontrado."); }
        } catch (Exception e) { System.err.println("ERROR cargando icono para OpenFileAction: " + e); }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller != null) {
            controller.abrirSelectorDeCarpeta();
        }
    }
}