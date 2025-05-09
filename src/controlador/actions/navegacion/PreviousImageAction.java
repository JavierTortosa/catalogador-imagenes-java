package controlador.actions.navegacion;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.VisorController;
import controlador.actions.BaseVisorAction; // Asumiendo que existe y tiene 'controller'
import controlador.commands.AppActionCommands; // Importar comandos
import vista.util.IconUtils;

public class PreviousImageAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L;
    // IconUtils podría estar en BaseVisorAction, si no, añadir campo y pasarlo

    public PreviousImageAction(VisorController controller, IconUtils iconUtils, int iconoAncho, int iconoAlto) {
        super("Imagen Anterior", controller); // Pasa controller a la superclase

        putValue(Action.SHORT_DESCRIPTION, "Ir a la imagen anterior");

        ImageIcon icon = iconUtils.getScaledIcon("1002-Anterior_48x48.png", iconoAncho, iconoAlto);
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
        }
        // ¡Asignar Comando Canónico!
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_NAV_ANTERIOR);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller == null) return; // Chequeo por si acaso

        controller.logActionInfo(e); // <-- Logging centralizado

        System.out.println("Acción: Imagen Anterior -> Llamando controller.navegarAnteriorViaCoordinador()");
        controller.navegarAnteriorViaCoordinador(); // <-- Delegar en Controller
    }
} //--- FIN PreviousImageAction

