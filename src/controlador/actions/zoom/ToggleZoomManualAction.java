package controlador.actions.zoom;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.AppInitializer;
import controlador.VisorController;
import controlador.managers.interfaces.IZoomManager;
import modelo.VisorModel;

public class ToggleZoomManualAction extends AbstractAction {
	
	private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);
	
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
    } // --- FIN del metodo constructor ---
    
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // El patrón ahora es idéntico al de ToggleSubfoldersAction.
        // La Action ya no gestiona su propio estado.

        logger.debug("[ToggleZoomManualAction] Solicitando al controlador que alterne el modo de paneo...");
        
        // Simplemente llamamos a un método en el VisorController que se encargará de TODO.
        // Este método es el que ya tienes: solicitarTogglePaneo()
        // (O un nombre similar si lo cambiaste).
        visorController.solicitarTogglePaneo(); 
        
        // ¡¡ELIMINAMOS TODO LO DEMÁS!!
        // La lógica de leer el modelo, calcular el nuevo estado, actualizar el modelo
        // y actualizar el SELECTED_KEY se traslada al método del controlador.
    } // --- FIN del metodo actionPerformed ---
    
} // --- FIN de la clase ToggleZoomManualAction ---