package controlador.actions.zoom;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import controlador.managers.ZoomManager;
import modelo.VisorModel; // Necesario para leer/sincronizar el modo actual
import servicios.zoom.ZoomModeEnum; 

public class AplicarModoZoomAction extends AbstractAction {
    private static final long serialVersionUID = 1L; // Considera generar uno único

    private ZoomManager zoomManager;
    private VisorModel modelRef; // Para leer el modo de zoom actual del modelo
    private VisorController controllerRef; 
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
                                 VisorController controller,
                                 String name, 
                                 ImageIcon icon, 
                                 ZoomModeEnum modo, 
                                 String actionCommandKey) {
        super(name, icon);
        this.zoomManager = Objects.requireNonNull(zoomManager, "ZoomManager no puede ser null");
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null");
        this.modoDeZoomQueRepresentaEstaAction = Objects.requireNonNull(modo, "ZoomModeEnum no puede ser null");
        
        putValue(Action.SHORT_DESCRIPTION, name); // O una descripción más detallada del modo
        putValue(Action.ACTION_COMMAND_KEY, actionCommandKey);

        // Inicializar el estado SELECTED_KEY basado en el modo actual del modelo
        // Esto asegura que si esta Action representa el modo por defecto, se marque al inicio.
        sincronizarEstadoSeleccionConModelo(); 
    }

    
    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("[AplicarModoZoomAction actionPerformed] Aplicando modo: " + modoDeZoomQueRepresentaEstaAction);
        if (this.zoomManager != null && this.controllerRef != null) {
            boolean modoCambiado = this.zoomManager.aplicarModoDeZoom(this.modoDeZoomQueRepresentaEstaAction);
            if (modoCambiado) {
                this.controllerRef.sincronizarEstadoVisualBotonesYRadiosZoom();
            } else {
                // Si el modo no cambió (ej. se pulsó el mismo botón/radio),
                // aún así, por seguridad, sincronizar para asegurar que el SELECTED_KEY de esta action
                // esté correcto si algo lo hubiera desincronizado.
                this.controllerRef.sincronizarEstadoVisualBotonesYRadiosZoom();
            }
        } else {
            System.err.println("ERROR CRÍTICO [AplicarModoZoomAction]: ZoomManager o ControllerRef es null.");
        }
    }
    
    
    /**
     * MODIFICADO: Actualiza el estado de selección (Action.SELECTED_KEY) y
     * DEVUELVE si la acción quedó seleccionada.
     *
     * @return true si esta acción representa el modo de zoom actualmente activo, false en caso contrario.
     */
    public boolean sincronizarEstadoSeleccionConModelo() {
        boolean deberiaEstarSeleccionada = false; // Valor por defecto

        if (modelRef != null) {
            deberiaEstarSeleccionada = (modelRef.getCurrentZoomMode() == this.modoDeZoomQueRepresentaEstaAction);
        }

        // Obtener el estado actual antes de cambiarlo para evitar eventos innecesarios
        boolean estadoActual = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));

        // Solo actualizar si el estado realmente cambia
        if (estadoActual != deberiaEstarSeleccionada) {
            putValue(Action.SELECTED_KEY, deberiaEstarSeleccionada);
        }

        // Devolver el estado que la acción DEBERÍA tener
        return deberiaEstarSeleccionada;

    } // --- FIN del método sincronizarEstadoSeleccionConModelo ---
    

    // (Opcional) Getter para saber qué modo representa esta acción, podría ser útil para depuración.
    public ZoomModeEnum getModoDeZoomQueRepresenta() {
        return modoDeZoomQueRepresentaEstaAction;
    }
} //--- FIN de la clase AplicarModoZoomAction