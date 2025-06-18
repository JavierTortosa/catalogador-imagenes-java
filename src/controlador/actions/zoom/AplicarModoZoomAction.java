package controlador.actions.zoom;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.VisorController;
import controlador.managers.ZoomManager;
import modelo.VisorModel;
import servicios.zoom.ZoomModeEnum; 

public class AplicarModoZoomAction extends AbstractAction {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ZoomManager zoomManager;
    private VisorModel modelRef;
    private VisorController controllerRef; 
    private ZoomModeEnum modoDeZoomQueRepresentaEstaAction;

    public AplicarModoZoomAction(ZoomManager zoomManager, VisorModel model, VisorController controller, String name, ImageIcon icon, ZoomModeEnum modo, String actionCommandKey) {
        super(name, icon);
        this.zoomManager = Objects.requireNonNull(zoomManager);
        this.modelRef = Objects.requireNonNull(model);
        this.controllerRef = Objects.requireNonNull(controller);
        this.modoDeZoomQueRepresentaEstaAction = Objects.requireNonNull(modo);
        putValue(Action.SHORT_DESCRIPTION, name);
        putValue(Action.ACTION_COMMAND_KEY, actionCommandKey);
        sincronizarEstadoSeleccionConModelo(); 
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        zoomManager.aplicarModoDeZoom(this.modoDeZoomQueRepresentaEstaAction);
        controllerRef.sincronizarEstadoVisualBotonesYRadiosZoom();
    }
    
    public void sincronizarEstadoSeleccionConModelo() {
        if (modelRef == null) return;
        boolean deberiaEstarSeleccionada = (modelRef.getCurrentZoomMode() == this.modoDeZoomQueRepresentaEstaAction);
        putValue(Action.SELECTED_KEY, deberiaEstarSeleccionada);
    }
    
    public ZoomModeEnum getModoAsociado() {
        return modoDeZoomQueRepresentaEstaAction;
    }
}

