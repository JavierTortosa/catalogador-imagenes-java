package controlador.actions.zoom;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.interfaces.IZoomManager;
import modelo.VisorModel;
import servicios.zoom.ZoomModeEnum; 

public class AplicarModoZoomAction extends AbstractAction {
	
	private static final Logger logger = LoggerFactory.getLogger(AplicarModoZoomAction.class);
	
    private static final long serialVersionUID = 1L;
    
    private final IZoomManager zoomManager;
    private final VisorModel modelRef;
    private final ZoomModeEnum modoDeZoomQueRepresentaEstaAction;

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
    
    @Override
    public void actionPerformed(ActionEvent e) {
        logger.debug("[AplicarModoZoomAction] Acción disparada. Delegando a ZoomManager.setZoomMode para modo: " + this.modoDeZoomQueRepresentaEstaAction);
        
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
    
} // --- FIN de la clase AplicarModoZoomAction ---

