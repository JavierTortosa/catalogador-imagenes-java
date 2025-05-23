package controlador.actions.zoom;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.commands.AppActionCommands;
import controlador.managers.ZoomManager;
import modelo.VisorModel;
import vista.VisorView;

public class ToggleZoomManualAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    // --- SECCIÓN 1: CAMPOS DE INSTANCIA (DEPENDENCIAS) ---
    private ZoomManager zoomManager;
    private Action resetZoomActionRef; // Referencia a la Action de Reset para habilitarla/deshabilitarla
    private VisorView viewRef;         // Para actualizar controles específicos de la UI (ej. aspecto del botón)
    private VisorModel modelRef;       // Para leer el estado inicial y actual del zoom manual

    // --- SECCIÓN 2: CONSTRUCTOR REFACTORIZADO ---
    public ToggleZoomManualAction(String name,
                                  ImageIcon icon,
                                  ZoomManager zoomManager,
                                  Action resetZoomActionRef, // La instancia de ResetZoomAction
                                  VisorView view,
                                  VisorModel model) {
        super(name, icon); // El icono se pasa al constructor de AbstractAction

        // 2.1. Asignar dependencias inyectadas.
        this.zoomManager = Objects.requireNonNull(zoomManager, "ZoomManager no puede ser null en ToggleZoomManualAction");
        this.resetZoomActionRef = Objects.requireNonNull(resetZoomActionRef, "ResetZoomActionRef no puede ser null en ToggleZoomManualAction");
        this.viewRef = Objects.requireNonNull(view, "VisorView no puede ser null en ToggleZoomManualAction");
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null en ToggleZoomManualAction");

        // 2.2. Establecer propiedades estándar de la Action.
        putValue(Action.SHORT_DESCRIPTION, "Activar o desactivar el zoom y desplazamiento manual de la imagen");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);

        // 2.3. Establecer el estado inicial de selección (SELECTED_KEY).
        //      Esto es crucial para que JCheckBoxMenuItem y JToggleButton reflejen el estado correcto.
        //      El estado se lee del modelo, que a su vez pudo haberlo leído de la configuración.
        if (this.modelRef != null) { // Doble chequeo aunque el constructor lo requiere no nulo
            boolean zoomHabilitadoInicialmente = this.modelRef.isZoomHabilitado();
            putValue(Action.SELECTED_KEY, zoomHabilitadoInicialmente);
            // También sincronizar el estado enabled de ResetZoomAction al inicio
            this.resetZoomActionRef.setEnabled(zoomHabilitadoInicialmente);
        } else {
            putValue(Action.SELECTED_KEY, Boolean.FALSE); // Fallback seguro
            this.resetZoomActionRef.setEnabled(false);    // Fallback seguro
        }
    }

    // --- SECCIÓN 3: MÉTODO actionPerformed ---
    @Override
    public void actionPerformed(ActionEvent e) {
        // 3.1. Log de inicio (opcional).
        System.out.println("[ToggleZoomManualAction actionPerformed] Comando: " + e.getActionCommand());

        // 3.2. Validar que las dependencias clave existan (aunque el constructor ya lo hizo).
        if (zoomManager == null || modelRef == null || resetZoomActionRef == null || viewRef == null) {
             System.err.println("ERROR CRÍTICO [ToggleZoomManualAction]: Alguna dependencia es nula. No se puede ejecutar.");
             return;
        }

        // 3.3. Determinar el nuevo estado de activación deseado.
        //      Se alterna el estado actual leído desde el modelo (fuente de verdad).
        boolean estadoActualModelo = modelRef.isZoomHabilitado();
        boolean nuevoEstadoActivacion = !estadoActualModelo;

        System.out.println("  -> Estado actual del zoom manual en modelo: " + estadoActualModelo + ". Intentando cambiar a: " + nuevoEstadoActivacion);

        // 3.4. Llamar al ZoomManager para que aplique el cambio de estado en el modelo.
        //      ZoomManager.activarODesactivarZoomManual actualiza el modelo y devuelve true si hubo cambio.
        boolean cambioRealizadoEnModelo = zoomManager.activarODesactivarZoomManual(nuevoEstadoActivacion);

        // 3.5. Si el estado en el modelo realmente cambió, actualizar la UI y otras Actions.
        if (cambioRealizadoEnModelo) {
            System.out.println("  -> El estado del zoom manual CAMBIÓ en el modelo. Actualizando UI y Actions...");

            // 3.5.1. Actualizar la propiedad SELECTED_KEY de ESTA Action.
            //          Esto es crucial para que JCheckBoxMenuItem y JToggleButton asociados se actualicen.
            putValue(Action.SELECTED_KEY, nuevoEstadoActivacion);

            // 3.5.2. Habilitar o deshabilitar la Action de reseteo de zoom.
            resetZoomActionRef.setEnabled(nuevoEstadoActivacion);

            // 3.5.3. Actualizar la apariencia visual de los controles de zoom en la Vista.
            //          (ej. cambiar el color de fondo del botón de zoom manual si la vista lo hace).
            //          El método en VisorView se encarga de los detalles visuales específicos.
            viewRef.actualizarEstadoControlesZoom(nuevoEstadoActivacion, nuevoEstadoActivacion); // El segundo param es para el botón de reset
            
            // 3.5.4. Solicitar al ZoomManager que refresque la imagen principal en la vista.
            //          Esto es necesario porque activarODesactivarZoomManual en ZoomManager
            //          probablemente llamó a model.resetZoomState(), lo que cambia el factor y el paneo.
            zoomManager.refrescarVistaPrincipalConEstadoActualDelModelo();

        } else {
            // 3.6. Si el estado en el modelo NO cambió (porque ya era el deseado),
            //      igualmente es buena idea asegurarse de que el SELECTED_KEY de esta Action
            //      y la UI visual estén sincronizados con el estado del modelo.
            //      Esto puede ocurrir si el evento se dispara programáticamente cuando el estado ya es el correcto.
            System.out.println("  -> El estado del zoom manual NO cambió en el modelo (ya era el correcto). Resincronizando UI/Action por si acaso...");
            putValue(Action.SELECTED_KEY, modelRef.isZoomHabilitado()); // Sincronizar con el estado real del modelo
            resetZoomActionRef.setEnabled(modelRef.isZoomHabilitado());
            viewRef.actualizarEstadoControlesZoom(modelRef.isZoomHabilitado(), modelRef.isZoomHabilitado());
            // No es necesario refrescar la imagen principal si no hubo cambio de estado de zoom.
        }
        
        // 3.7. Log final del estado de la Action.
        System.out.println("  [ToggleZoomManualAction] Fin actionPerformed. Estado Action.SELECTED_KEY final: " + getValue(Action.SELECTED_KEY));
    }
} // FIN de la clase ToggleZoomManualAction