package controlador.actions.zoom;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.managers.ZoomManager;
import modelo.VisorModel; // Necesario para leer/sincronizar el modo actual
import servicios.zoom.ZoomModeEnum; 

public class AplicarModoZoomAction extends AbstractAction {
    private static final long serialVersionUID = 1L; // Considera generar uno único

    private ZoomManager zoomManager;
    private VisorModel modelRef; // Para leer el modo de zoom actual del modelo
    private ZoomModeEnum modoDeZoomQueRepresentaEstaAction; // El modo específico que esta instancia aplicará

    /**
     * Constructor para una acción que aplica un modo de zoom específico.
     * @param zoomManager El gestor de lógica de zoom.
     * @param model El modelo de la aplicación (para saber el modo de zoom actual).
     * @param name El nombre/texto de la Action (para menús, tooltips).
     * @param icon El ImageIcon para esta Action.
     * @param modo El ZoomModeEnum que esta instancia de Action aplicará.
     * @param actionCommandKey El comando canónico de AppActionCommands para esta acción.
     */
    public AplicarModoZoomAction(ZoomManager zoomManager, 
                                 VisorModel model,
                                 String name, 
                                 ImageIcon icon, 
                                 ZoomModeEnum modo, 
                                 String actionCommandKey) {
        super(name, icon);
        this.zoomManager = Objects.requireNonNull(zoomManager, "ZoomManager no puede ser null");
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.modoDeZoomQueRepresentaEstaAction = Objects.requireNonNull(modo, "ZoomModeEnum no puede ser null");
        
        putValue(Action.SHORT_DESCRIPTION, name); // O una descripción más detallada del modo
        putValue(Action.ACTION_COMMAND_KEY, actionCommandKey);

        // Inicializar el estado SELECTED_KEY basado en el modo actual del modelo
        // Esto asegura que si esta Action representa el modo por defecto, se marque al inicio.
        sincronizarEstadoSeleccionConModelo(); 
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("[AplicarModoZoomAction actionPerformed] Aplicando modo: " + modoDeZoomQueRepresentaEstaAction + ", Comando: " + e.getActionCommand());
        if (this.zoomManager != null) {
            // ZoomManager se encarga de actualizar el modelo (incluyendo model.setCurrentZoomMode())
            // y de refrescar la vista. Después de eso, se debe llamar a la sincronización de todas
            // las AplicarModoZoomAction (lo cual se hará desde VisorController vía ZoomManager).
            this.zoomManager.aplicarModoDeZoom(this.modoDeZoomQueRepresentaEstaAction);
            
            // No actualizamos SELECTED_KEY aquí directamente, esperamos la notificación centralizada.
        } else {
            System.err.println("ERROR CRÍTICO [AplicarModoZoomAction]: ZoomManager es null.");
        }
    }
    
    /**
     * Actualiza el estado de selección (Action.SELECTED_KEY) de esta Action
     * basándose en si el modo de zoom que representa coincide con el modo
     * actualmente activo en el VisorModel.
     * Este método debe ser llamado externamente cuando el modo de zoom global cambia.
     */
    public void sincronizarEstadoSeleccionConModelo() {
        if (modelRef != null) {
            boolean deberiaEstarSeleccionada = (modelRef.getCurrentZoomMode() == this.modoDeZoomQueRepresentaEstaAction);
            
            // Solo actualizar y disparar evento si el estado realmente cambia
            if (!Objects.equals(getValue(Action.SELECTED_KEY), deberiaEstarSeleccionada)) {
                putValue(Action.SELECTED_KEY, deberiaEstarSeleccionada);
                // System.out.println("  [AplicarModoZoomAction: " + modoDeZoomQueRepresentaEstaAction + "] SELECTED_KEY actualizado a: " + deberiaEstarSeleccionada);
            }
        } else {
            // Si el modelo es null, por seguridad, deseleccionar.
            if (!Objects.equals(getValue(Action.SELECTED_KEY), Boolean.FALSE)) {
                 putValue(Action.SELECTED_KEY, Boolean.FALSE);
            }
        }
    }

    // (Opcional) Getter para saber qué modo representa esta acción, podría ser útil para depuración.
    public ZoomModeEnum getModoDeZoomQueRepresenta() {
        return modoDeZoomQueRepresentaEstaAction;
    }
} //--- FIN de la clase AplicarModoZoomAction