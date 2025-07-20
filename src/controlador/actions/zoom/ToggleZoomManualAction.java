package controlador.actions.zoom;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import controlador.VisorController;
import controlador.managers.interfaces.IZoomManager;
import modelo.VisorModel;

public class ToggleZoomManualAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    private final IZoomManager zoomManager;
    private final VisorModel model;
    private final VisorController visorController;

    public ToggleZoomManualAction(String name, Icon icon, IZoomManager zoomManager, VisorController visorController, VisorModel model) {
        super(name, icon);
        this.zoomManager = Objects.requireNonNull(zoomManager);
        this.model = Objects.requireNonNull(model);
        this.visorController = Objects.requireNonNull(visorController);
        
        putValue(Action.ACTION_COMMAND_KEY, controlador.commands.AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
        
        // El estado inicial de la Action se basa en el estado inicial del modelo.
        putValue(Action.SELECTED_KEY, model.isZoomHabilitado());
    }
    
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // El patrón ahora es idéntico al de ToggleSubfoldersAction.
        // La Action ya no gestiona su propio estado.

        System.out.println("[ToggleZoomManualAction] Solicitando al controlador que alterne el modo de paneo...");
        
        // Simplemente llamamos a un método en el VisorController que se encargará de TODO.
        // Este método es el que ya tienes: solicitarTogglePaneo()
        // (O un nombre similar si lo cambiaste).
        visorController.solicitarTogglePaneo(); 
        
        // ¡¡ELIMINAMOS TODO LO DEMÁS!!
        // La lógica de leer el modelo, calcular el nuevo estado, actualizar el modelo
        // y actualizar el SELECTED_KEY se traslada al método del controlador.
    }
    
    
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        
//        // 1. LEEMOS el estado actual del modelo.
//        boolean estadoActual = model.isZoomHabilitado();
//        
//        // 2. CALCULAMOS el nuevo estado.
//        boolean nuevoEstado = !estadoActual;
//        
//        System.out.println("[ToggleZoomManualAction] Alternando Paneo. Nuevo estado: " + nuevoEstado);
//
//        // 3. APLICAMOS el cambio de estado al modelo.
//        zoomManager.setPermisoManual(nuevoEstado);
//        
//        // 4. ACTUALIZAMOS NUESTRO PROPIO ESTADO.
//        //    Esta es la línea que le dice al JToggleButton: "¡Márcate!" o "¡Desmárcate!".
//        putValue(Action.SELECTED_KEY, nuevoEstado);
//
//        // 5. NOTIFICAMOS AL CONTROLADOR, pero solo para que sincronice a OTROS
//        //    componentes que dependen de este estado (como el botón Reset).
//        visorController.sincronizarEstadoBotonReset();
//    }
}