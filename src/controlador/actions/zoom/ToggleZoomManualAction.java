package controlador.actions.zoom;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.VisorController;
import controlador.commands.AppActionCommands;
import controlador.managers.interfaces.IZoomManager;
import modelo.VisorModel;

public class ToggleZoomManualAction extends AbstractAction {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final IZoomManager zoomManager;
    private final VisorModel modelRef;
    private final VisorController controllerRef; // Se mantiene para notificar

    public ToggleZoomManualAction(String name, ImageIcon icon, IZoomManager zoomManager, VisorController controller, VisorModel model) {
        super(name, icon);
        this.zoomManager = Objects.requireNonNull(zoomManager);
        this.modelRef = Objects.requireNonNull(model);
        this.controllerRef = Objects.requireNonNull(controller); // Se mantiene la referencia
        putValue(Action.SHORT_DESCRIPTION, "Activar/desactivar la interacción manual (zoom/paneo).");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
        sincronizarEstadoConModelo();
    } // --- Fin del método ToggleZoomManualAction (constructor) ---

    // <--- MODIFICADO: Se elimina la responsabilidad de controlar otras acciones ---
    @Override
    public void actionPerformed(ActionEvent e) {
        // 1. Determinar el nuevo estado deseado.
        boolean nuevoEstado = !modelRef.isZoomHabilitado();
        
        // 2. Ejecutar la lógica de negocio a través del manager.
        zoomManager.setPermisoManual(nuevoEstado);
        
        // 3. Sincronizar el estado visual de esta acción (SELECTED_KEY).
        sincronizarEstadoConModelo();
        
        // 4. Notificar al controlador que el estado del zoom ha cambiado,
        //    para que él se encargue de la sincronización global.
        controllerRef.sincronizarEstadoVisualBotonesYRadiosZoom();
    } // --- Fin del método actionPerformed ---
    
    public void sincronizarEstadoConModelo() {
        if (modelRef == null) return;
        // Sincroniza la propiedad 'selected' de esta acción con el estado del modelo.
        putValue(Action.SELECTED_KEY, modelRef.isZoomHabilitado());
    } // --- Fin del método sincronizarEstadoConModelo ---
    
} // --- FIN de la clase ToggleZoomManualAction ---




//package controlador.actions.zoom;
//
//import java.awt.event.ActionEvent;
//import java.util.Objects;
//import javax.swing.AbstractAction;
//import javax.swing.Action;
//import javax.swing.ImageIcon;
//import controlador.VisorController;
//import controlador.commands.AppActionCommands;
//import controlador.managers.interfaces.IZoomManager;
//import modelo.VisorModel;
//
//public class ToggleZoomManualAction extends AbstractAction {
//    /**
//	 * 
//	 */
//	private static final long serialVersionUID = 1L;
//	private final IZoomManager zoomManager;
//    private final VisorController controllerRef;
//    private final VisorModel modelRef;
//
//    public ToggleZoomManualAction(String name, ImageIcon icon, IZoomManager zoomManager, VisorController controller, VisorModel model) {
//        super(name, icon);
//        this.zoomManager = Objects.requireNonNull(zoomManager);
//        this.modelRef = Objects.requireNonNull(model);
//        this.controllerRef = Objects.requireNonNull(controller);
//        putValue(Action.SHORT_DESCRIPTION, "Activar/desactivar la interacción manual (zoom/paneo).");
//        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
//        sincronizarEstadoConModelo();
//    }
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        boolean nuevoEstado = !modelRef.isZoomHabilitado();
//        zoomManager.setPermisoManual(nuevoEstado);
//        sincronizarEstadoConModelo();
//        controllerRef.sincronizarEstadoVisualBotonesYRadiosZoom();
//    }
//    
//    public void sincronizarEstadoConModelo() {
//        if (modelRef == null) return;
//        putValue(Action.SELECTED_KEY, modelRef.isZoomHabilitado());
//    }
//}
//// --- FIN de la clase ToggleZoomManualAction ---