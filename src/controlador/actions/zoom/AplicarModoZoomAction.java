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
    
    // Se eliminan las dependencias a VisorController y se mantiene la del ZoomManager.
    private IZoomManager zoomManager;
    private VisorModel modelRef;
    private ZoomModeEnum modoDeZoomQueRepresentaEstaAction;

    // El constructor ya no necesita VisorController.
    // En su lugar, se le pasa un Runnable (callback) para la sincronización de la UI.
    public AplicarModoZoomAction(IZoomManager zoomManager, VisorModel model, String name, ImageIcon icon, ZoomModeEnum modo, String actionCommandKey, Runnable uiSyncCallback) {
        super(name, icon);
        this.zoomManager = Objects.requireNonNull(zoomManager);
        this.modelRef = Objects.requireNonNull(model);
        this.modoDeZoomQueRepresentaEstaAction = Objects.requireNonNull(modo);
        putValue(Action.SHORT_DESCRIPTION, name);
        putValue(Action.ACTION_COMMAND_KEY, actionCommandKey);

        // Se le añade el callback como un valor en la propia acción.
        // Esto evita tener un campo extra para el Runnable.
        putValue("uiSyncCallback", uiSyncCallback);

        sincronizarEstadoSeleccionConModelo(); 
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // --- LÓGICA DE CONTROL AÑADIDA ---
        // La acción solo se ejecuta si el modo que representa NO es ya el modo activo.
        // Esto evita que al hacer clic en un botón ya seleccionado se produzcan efectos no deseados.
        if (modelRef.getCurrentZoomMode() != this.modoDeZoomQueRepresentaEstaAction) {
            Runnable callback = (Runnable) getValue("uiSyncCallback");
            zoomManager.aplicarModoDeZoom(this.modoDeZoomQueRepresentaEstaAction, callback);
        } else {
            System.out.println("[AplicarModoZoomAction] El modo '" + this.modoDeZoomQueRepresentaEstaAction + "' ya está activo. No se hace nada.");
        }
    }
    
    public void sincronizarEstadoSeleccionConModelo() {
        if (modelRef == null) return;
        boolean deberiaEstarSeleccionada = (modelRef.getCurrentZoomMode() == this.modoDeZoomQueRepresentaEstaAction);
        putValue(Action.SELECTED_KEY, deberiaEstarSeleccionada);
    }
    
    public ZoomModeEnum getModoAsociado() {
        return modoDeZoomQueRepresentaEstaAction;
    }
    
} // --- FIN de la clase AplicarModoZoomAction ---

