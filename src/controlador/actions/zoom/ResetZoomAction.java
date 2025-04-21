package controlador.actions.zoom;

import java.awt.Image;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.VisorController;
import controlador.actions.BaseVisorAction;

public class ResetZoomAction extends BaseVisorAction {

    public ResetZoomAction(VisorController controller) {
        super("Resetear Zoom", controller); // Texto para menú
        putValue(Action.SHORT_DESCRIPTION, "Volver al zoom 100% sin desplazamiento");
        // Empieza deshabilitado, se habilitará junto con el zoom manual
        setEnabled(false);

        try {
            java.net.URL iconUrl = getClass().getResource("/iconos/14-Reset_48x48.png");
            if (iconUrl != null) {
                ImageIcon icon = new ImageIcon(iconUrl);
                Image scaledImg = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                putValue(Action.SMALL_ICON, new ImageIcon(scaledImg));
            } else { System.err.println("WARN [ResetZoomAction]: Icono no encontrado."); }
        } catch (Exception e) { System.err.println("ERROR cargando icono para ResetZoomAction: " + e); }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller != null) {
            // Llama al método público del controller que hace el reset
            controller.aplicarResetZoomAction();
        }
    }
}