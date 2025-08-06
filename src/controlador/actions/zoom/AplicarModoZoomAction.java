package controlador.actions.zoom;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.managers.interfaces.IZoomManager;
import modelo.VisorModel;
import servicios.zoom.ZoomModeEnum; 

public class AplicarModoZoomAction extends AbstractAction {
    private static final long serialVersionUID = 1L;
    
    private final IZoomManager zoomManager;
    private final VisorModel modelRef;
    private final ZoomModeEnum modoDeZoomQueRepresentaEstaAction;

    // El constructor se mantiene EXACTAMENTE IGUAL, con el Runnable
    public AplicarModoZoomAction(IZoomManager zoomManager, VisorModel model, String name, ImageIcon icon, ZoomModeEnum modo, String actionCommandKey, Runnable uiSyncCallback) {
        super(name, icon);
        this.zoomManager = Objects.requireNonNull(zoomManager);
        this.modelRef = Objects.requireNonNull(model);
        this.modoDeZoomQueRepresentaEstaAction = Objects.requireNonNull(modo);
        putValue(Action.SHORT_DESCRIPTION, name);
        putValue(Action.ACTION_COMMAND_KEY, actionCommandKey);
        putValue("uiSyncCallback", uiSyncCallback); // Guardamos el callback
        sincronizarEstadoSeleccionConModelo(); 
    }
    
    // >>>>> ESTE ES EL ÚNICO CAMBIO REAL EN LA CLASE <<<<<
    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("[AplicarModoZoomAction] Acción disparada. Delegando a ZoomManager.setZoomMode para modo: " + this.modoDeZoomQueRepresentaEstaAction);
        
        // Obtenemos el callback que guardamos
        Runnable callback = (Runnable) getValue("uiSyncCallback");
        
        // Llamamos al NUEVO método del ZoomManager
        zoomManager.setZoomMode(this.modoDeZoomQueRepresentaEstaAction, callback);
    }
    
    // El resto de los métodos se mantienen como estaban
    public void sincronizarEstadoSeleccionConModelo() {
        if (modelRef == null) return;
        boolean deberiaEstarSeleccionada = (modelRef.getCurrentZoomMode() == this.modoDeZoomQueRepresentaEstaAction);
        putValue(Action.SELECTED_KEY, deberiaEstarSeleccionada);
    }
    
    public ZoomModeEnum getModoAsociado() {
        return modoDeZoomQueRepresentaEstaAction;
    }
}

//package controlador.actions.zoom;
//
//import java.awt.event.ActionEvent;
//import java.util.Objects;
//import javax.swing.AbstractAction;
//import javax.swing.Action;
//import javax.swing.ImageIcon;
//import controlador.managers.interfaces.IZoomManager;
//import modelo.VisorModel;
//import servicios.zoom.ZoomModeEnum; 
//
//public class AplicarModoZoomAction extends AbstractAction {
//    private static final long serialVersionUID = 1L;
//    
//    // Se eliminan las dependencias a VisorController y se mantiene la del ZoomManager.
//    private IZoomManager zoomManager;
//    private VisorModel modelRef;
//    private ZoomModeEnum modoDeZoomQueRepresentaEstaAction;
//
//    // El constructor ya no necesita VisorController.
//    // En su lugar, se le pasa un Runnable (callback) para la sincronización de la UI.
//    public AplicarModoZoomAction(IZoomManager zoomManager, VisorModel model, String name, ImageIcon icon, ZoomModeEnum modo, String actionCommandKey, Runnable uiSyncCallback) {
//        super(name, icon);
//        this.zoomManager = Objects.requireNonNull(zoomManager);
//        this.modelRef = Objects.requireNonNull(model);
//        this.modoDeZoomQueRepresentaEstaAction = Objects.requireNonNull(modo);
//        putValue(Action.SHORT_DESCRIPTION, name);
//        putValue(Action.ACTION_COMMAND_KEY, actionCommandKey);
//
//        // Se le añade el callback como un valor en la propia acción.
//        // Esto evita tener un campo extra para el Runnable.
//        putValue("uiSyncCallback", uiSyncCallback);
//
//        sincronizarEstadoSeleccionConModelo(); 
//    }
//    
//    
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        System.out.println("[AplicarModoZoomAction] Acción disparada para modo: " + this.modoDeZoomQueRepresentaEstaAction);
//
//        // Evita ejecutar si el modo ya está activo, excepto para el modo de "bloqueo"
//        // que necesita capturar el zoom actual.
//        if (modelRef.getCurrentZoomMode() == this.modoDeZoomQueRepresentaEstaAction && 
//            this.modoDeZoomQueRepresentaEstaAction != ZoomModeEnum.USER_SPECIFIED_PERCENTAGE) {
//            
//            System.out.println("  -> El modo ya está activo. No se hace nada.");
//            return;
//        }
//
//        // --- INICIO DE LA NUEVA LÓGICA DE REGLAS ---
//
//        switch (this.modoDeZoomQueRepresentaEstaAction) {
//            
//            case MAINTAIN_CURRENT_ZOOM: // Modo "Capturador"
//                // Regla: Sincroniza zoomCustomPercentage con el zoomFactor actual.
//                double currentZoomFactor = modelRef.getZoomFactor();
//                modelRef.setZoomCustomPercentage(currentZoomFactor * 100.0);
//                
//                // Llama al zoomManager para que aplique el modo. El zoomManager usará
//                // el zoomFactor que ya está en el modelo.
//                zoomManager.aplicarModoDeZoom(this.modoDeZoomQueRepresentaEstaAction, (Runnable) getValue("uiSyncCallback"));
//                break;
//
//            case USER_SPECIFIED_PERCENTAGE: // Modo "Bloqueador"
//                // Regla: El valor de zoomCustomPercentage manda.
//                // Primero, nos aseguramos de que el modelo esté en el modo correcto.
//                modelRef.setCurrentZoomMode(this.modoDeZoomQueRepresentaEstaAction);
//                
//                // Ahora, le decimos al modelo que su zoom REAL (zoomFactor) debe ser
//                // el que está guardado en el zoom PERSONALIZADO (zoomCustomPercentage).
//                double customPercentage = modelRef.getZoomCustomPercentage();
//                modelRef.setZoomFactor(customPercentage / 100.0);
//                
//                // Finalmente, refrescamos la vista y la UI.
//                zoomManager.refrescarVistaSincrono(); // Repinta la imagen con el nuevo factor.
//                Runnable callback = (Runnable) getValue("uiSyncCallback");
//                if (callback != null) {
//                    callback.run(); // Ejecuta la sincronización de botones, labels, etc.
//                }
//                break;
//
//            default: // Para todos los modos automáticos (FIT_TO_SCREEN, etc.)
//                // Regla: No tocan zoomCustomPercentage. Simplemente aplican su modo.
//                zoomManager.aplicarModoDeZoom(this.modoDeZoomQueRepresentaEstaAction, (Runnable) getValue("uiSyncCallback"));
//                break;
//        }
//    } // --- FIN del metodo actionPerformed ---
//    
//    
////    @Override
////    public void actionPerformed(ActionEvent e) {
////        // --- LÓGICA DE CONTROL AÑADIDA ---
////        // La acción solo se ejecuta si el modo que representa NO es ya el modo activo.
////        // Esto evita que al hacer clic en un botón ya seleccionado se produzcan efectos no deseados.
////        if (modelRef.getCurrentZoomMode() != this.modoDeZoomQueRepresentaEstaAction) {
////            Runnable callback = (Runnable) getValue("uiSyncCallback");
////            zoomManager.aplicarModoDeZoom(this.modoDeZoomQueRepresentaEstaAction, callback);
////        } else {
////            System.out.println("[AplicarModoZoomAction] El modo '" + this.modoDeZoomQueRepresentaEstaAction + "' ya está activo. No se hace nada.");
////        }
////    }
//    
//    public void sincronizarEstadoSeleccionConModelo() {
//        if (modelRef == null) return;
//        boolean deberiaEstarSeleccionada = (modelRef.getCurrentZoomMode() == this.modoDeZoomQueRepresentaEstaAction);
//        putValue(Action.SELECTED_KEY, deberiaEstarSeleccionada);
//    }
//    
//    public ZoomModeEnum getModoAsociado() {
//        return modoDeZoomQueRepresentaEstaAction;
//    }
//    
//} // --- FIN de la clase AplicarModoZoomAction ---
//
