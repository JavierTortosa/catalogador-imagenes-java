package controlador.actions.displaymode; // O el paquete donde la tengas

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.AppInitializer;
import controlador.factory.ActionFactory;
import controlador.managers.DisplayModeManager; // <-- Importa el nuevo manager
import modelo.VisorModel;
import modelo.VisorModel.DisplayMode;

@SuppressWarnings("serial")
public class SwitchDisplayModeAction extends AbstractAction {

	private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);
	
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