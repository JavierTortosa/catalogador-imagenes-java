package controlador.actions.zoom;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.commands.AppActionCommands;
import controlador.managers.ZoomManager;
import servicios.zoom.ZoomModeEnum;

public class ResetZoomAction extends AbstractAction {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ZoomManager zoomManager;

    public ResetZoomAction(String name, ImageIcon icon, ZoomManager zoomManager) {
        super(name, icon);
        this.zoomManager = Objects.requireNonNull(zoomManager);
        putValue(Action.SHORT_DESCRIPTION, "Restablecer la vista a 'Ajustar a Pantalla'");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ZOOM_RESET);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        zoomManager.aplicarModoDeZoom(ZoomModeEnum.FIT_TO_SCREEN);
    }
}

//package controlador.actions.zoom;
//
//import java.awt.event.ActionEvent;
//import java.util.Objects;
//import javax.swing.AbstractAction;
//import javax.swing.Action;
//import javax.swing.ImageIcon;
//import controlador.commands.AppActionCommands;
//import controlador.managers.ZoomManager;
//import servicios.zoom.ZoomModeEnum; // <<< IMPORTA ESTO
//
//public class ResetZoomAction extends AbstractAction {
//
//    private static final long serialVersionUID = 1L;
//
//    private ZoomManager zoomManager;
//
//    // Constructor Refactorizado (ya no necesita la VisorView)
//    public ResetZoomAction(String name, 
//                           ImageIcon icon, 
//                           ZoomManager zoomManager) {
//        super(name, icon);
//
//        this.zoomManager = Objects.requireNonNull(zoomManager, "ZoomManager no puede ser null en ResetZoomAction");
//
//        putValue(Action.SHORT_DESCRIPTION, "Restablecer la vista de la imagen a 'Ajustar a Pantalla'");
//        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ZOOM_RESET);
//        
//        // Esta acción se habilita/deshabilita desde VisorController,
//        // así que la dejamos habilitada por defecto y que el controlador decida.
//        setEnabled(true); 
//    }
//    // --- FIN del constructor ---
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        System.out.println("[ResetZoomAction actionPerformed] Comando: " + e.getActionCommand());
//
//        if (this.zoomManager == null) {
//            System.err.println("ERROR CRÍTICO [ResetZoomAction]: ZoomManager es nulo. No se puede ejecutar la acción.");
//            return;
//        }
//
//        // --- INICIO DE LA CORRECCIÓN ---
//        // Delegamos la lógica al ZoomManager, diciéndole que aplique el modo por defecto.
//        // El ZoomManager se encargará de resetear el pan, actualizar el modelo y refrescar la vista.
//        this.zoomManager.aplicarModoDeZoom(ZoomModeEnum.FIT_TO_SCREEN);
//        // --- FIN DE LA CORRECCIÓN ---
//        
//        System.out.println("[ResetZoomAction actionPerformed] Lógica de reseteo (volver a FIT_TO_SCREEN) delegada a ZoomManager.");
//    }
//    // --- FIN del metodo actionPerformed ---
//    
//}
// --- FIN de la clase ResetZoomAction ---