package controlador.actions.displaymode; 

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.factory.ActionFactory;
import controlador.managers.DisplayModeManager; 
import modelo.VisorModel;
import modelo.VisorModel.DisplayMode;

@SuppressWarnings("serial")
public class SwitchDisplayModeAction extends AbstractAction {

	private static final Logger logger = LoggerFactory.getLogger(SwitchDisplayModeAction.class);
	
	private final ActionFactory actionFactory;
    private final DisplayMode targetDisplayMode;
    private final VisorModel model;

    public SwitchDisplayModeAction(String name, ActionFactory factory, DisplayMode targetMode, VisorModel model) {
    	super(name);
        // this.displayModeManager = Objects.requireNonNull(manager, "DisplayModeManager no puede ser nulo"); // <--- Comenta o elimina
        this.actionFactory = Objects.requireNonNull(factory, "ActionFactory no puede ser nula"); // <--- Añade esto
        this.targetDisplayMode = targetMode;
        this.model = model;
    } // --- Fin del Constructor ---

    @Override
    public void actionPerformed(ActionEvent e) {
        DisplayModeManager manager = actionFactory.getDisplayModeManager(); // <--- Obtén el manager JUSTO AHORA
        if (manager == null) {
            logger.error("ERROR [SwitchDisplayModeAction]: DisplayModeManager es nulo en la ActionFactory. La acción no se puede ejecutar.");
            return;
        }
        logger.debug("[SwitchDisplayModeAction] Acción disparada para cambiar a: " + targetDisplayMode);
        manager.switchToDisplayMode(targetDisplayMode);
    } // --- Fin del método actionPerformed ---

    
    /**
     * Sincroniza el estado de selección y habilitación de esta acción (y por tanto, del botón asociado)
     * con el estado actual del modelo. Este método es llamado por DisplayModeManager
     * para asegurar que solo el botón del modo activo aparezca seleccionado y que
     * los botones se habiliten/deshabiliten correctamente según el contexto.
     * 
     * @param model El modelo de datos principal que contiene el estado actual.
     */
    public void updateSelectedState(VisorModel model) {
        if (model == null) {
            return;
        }
        
        // 1. Sincronizar estado de selección (si el modo es el actual)
        boolean isSelected = (model.getCurrentDisplayMode() == this.targetDisplayMode);
        putValue(SELECTED_KEY, isSelected);

        // 2. Sincronizar estado de habilitación (ENABLED)
        // En modo DATOS, los modos SINGLE_IMAGE y POLAROID solo se habilitan si hay una imagen seleccionada.
        boolean isEnabled = true;
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.DATOS) {
            if (this.targetDisplayMode == DisplayMode.SINGLE_IMAGE || this.targetDisplayMode == DisplayMode.POLAROID) {
                isEnabled = (model.getSelectedImageKey() != null);
            }
        }
        setEnabled(isEnabled);
    } // --- Fin del método updateSelectedState ---
    
    
    /**
     * Sincroniza el estado de selección de esta acción con el estado actual del modelo.
     * Este método es llamado por un manager (como DisplayModeManager o GeneralController)
     * para asegurar que los botones de la UI reflejen el estado correcto.
     * @param currentMode El DisplayMode actualmente activo en el modelo.
     */
    public void sincronizarEstadoSeleccionConModelo(DisplayMode currentMode) {
        boolean isSelected = (model.getCurrentDisplayMode() == this.targetDisplayMode);
        putValue(SELECTED_KEY, isSelected);
    } // --- Fin del método sincronizarEstadoSeleccionConModelo ---

} // --- Fin de la clase SwitchDisplayModeAction ---