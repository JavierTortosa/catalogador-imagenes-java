// En src/controlador/actions/zoom/ResetZoomAction.java
package controlador.actions.zoom;

import java.awt.event.ActionEvent;
import java.util.Objects; // Para Objects.requireNonNull
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.commands.AppActionCommands;
import controlador.managers.ZoomManager;
import vista.VisorView; // Si necesita interactuar directamente con la vista para algo específico

public class ResetZoomAction extends AbstractAction {

    private static final long serialVersionUID = 1L; // Buena práctica añadir SUID

    private ZoomManager zoomManager;
    private VisorView viewRef; // Opcional: Solo si la Action necesita interactuar directamente con la View
                              // más allá de lo que hace ZoomManager. Para ResetZoom, podría no ser necesario.

    // Constructor Refactorizado
    public ResetZoomAction(String name, 
                           ImageIcon icon, 
                           ZoomManager zoomManager, 
                           VisorView view) { // 'view' podría ser opcional aquí
        super(name, icon); // El icono se pasa al constructor de AbstractAction
                           // El nombre es para el menú/tooltip si no hay icono o si se sobreescribe

        this.zoomManager = Objects.requireNonNull(zoomManager, "ZoomManager no puede ser null en ResetZoomAction");
        this.viewRef = view; // Guardar la referencia a la vista, puede ser null si no se usa

        // Propiedades estándar de la Action
        putValue(Action.SHORT_DESCRIPTION, "Restablecer el zoom y la posición de la imagen al 100%");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ZOOM_RESET);
        
        // El estado 'enabled' de esta Action es manejado por ToggleZoomManualAction.
        // Por defecto, podría empezar deshabilitada.
        setEnabled(false); 
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // 1. Log de la acción (opcional pero útil)
        System.out.println("[ResetZoomAction actionPerformed] Comando: " + e.getActionCommand());

        // 2. Validar que ZoomManager esté disponible (aunque el constructor ya lo hizo)
        if (this.zoomManager == null) {
            System.err.println("ERROR CRÍTICO [ResetZoomAction]: ZoomManager es nulo. No se puede ejecutar la acción.");
            return;
        }

        // 3. Delegar la lógica al ZoomManager
        //    ZoomManager se encargará de actualizar el modelo y refrescar la vista.
        this.zoomManager.resetearZoomYPanYRefrescarVista();

        // 4. (Opcional) Si esta Action debe disparar una animación específica en un botón
        //    de la toolbar, necesitaría una forma de acceder a la vista y llamar a un método
        //    como 'aplicarAnimacionBotonPorComando'.
        if (this.viewRef != null) {
            // Asumiendo que VisorView tiene un método para aplicar animación basado en el comando de la Action
            // this.viewRef.aplicarAnimacionBotonPorComando(AppActionCommands.CMD_ZOOM_RESET);
            // O si usas la clave larga de config del botón (menos ideal para la Action saber esto):
            // this.viewRef.aplicarAnimacionBotonPorClaveLarga("interfaz.boton.zoom.Reset_48x48");
        }
        
        System.out.println("[ResetZoomAction actionPerformed] Lógica de reseteo delegada a ZoomManager.");
    }
} // FIN de la clase ResetZoomAction