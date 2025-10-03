package controlador.actions.zoom;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.VisorController;
import controlador.interfaces.ContextSensitiveAction; // <-- 1. Importar la interfaz
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;

// 2. Añadir 'implements ContextSensitiveAction'
public class ToggleZoomToCursorAction extends AbstractAction implements ContextSensitiveAction {

	private static final Logger logger = LoggerFactory.getLogger(ToggleZoomToCursorAction.class);
	
    private static final long serialVersionUID = 1L;
    private final VisorModel model;
    private final ConfigurationManager config;
    private final VisorController controller;

    public ToggleZoomToCursorAction(String name, Icon icon, VisorController controller) {
        super(name, icon);
        this.controller = controller;
        this.model = controller.getModel();
        this.config = controller.getConfigurationManager();
        
        // --- INICIO DE LA MODIFICACIÓN (Constructor) ---
        // El estado inicial de la Action debe reflejar lo que está en la configuración
        // y lo que ya se habrá puesto en el modelo durante la inicialización.
        // Se llama a sincronizarEstadoConModelo() para que el valor de SELECTED_KEY sea correcto.
        sincronizarEstadoConModelo(); // Esta llamada usa el estado actual del modelo/config.
        // --- FIN DE LA MODIFICACIÓN (Constructor) ---
        
        // La acción empieza deshabilitada hasta que haya una imagen
        setEnabled(false);
    } // --- Fin del método ToggleZoomToCursorAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        // --- INICIO DE LA MODIFICACIÓN (actionPerformed) ---
        // Obtener el estado actual directamente del modelo, que es la fuente de verdad.
        boolean estadoActualDelModelo = model.isZoomToCursorEnabled();
        boolean nuevoEstado = !estadoActualDelModelo;
        
        // Actualizar el modelo y la configuración con el nuevo estado.
        model.setZoomToCursorEnabled(nuevoEstado);
        config.setString(ConfigKeys.COMPORTAMIENTO_ZOOM_AL_CURSOR_ACTIVADO, String.valueOf(nuevoEstado));
        
        // Sincronizar el estado visual de esta acción (SELECTED_KEY).
        sincronizarEstadoConModelo();
        
        if (this.controller != null && this.controller.getZoomManager() != null) {
            // Notificar al ZoomManager que el estado ha cambiado para que sincronice la UI global.
        	this.controller.getZoomManager().sincronizarEstadoVisualBotonesYRadiosZoom();
        }
        
        logger.debug("[ToggleZoomToCursorAction] Estado de 'Zoom al Cursor' cambiado a: " + nuevoEstado);
    } // --- Fin del método actionPerformed ---

    // 3. Implementar el método requerido por la interfaz
    @Override
    public void updateEnabledState(VisorModel model) {
        // La acción "Zoom al Cursor" solo debe estar habilitada si
        // hay una imagen cargada actualmente en el modelo.
        if (model != null) {
            boolean isEnabled = model.getCurrentImage() != null;
            setEnabled(isEnabled);
        } else {
            setEnabled(false);
        }
    } // --- Fin del método updateEnabledState ---

    /**
     * Sincroniza la propiedad `SELECTED_KEY` de esta Action con el estado actual
     * de `zoomToCursorEnabled` en el VisorModel. Este método es crucial para que
     * los componentes de UI (como JToggleButton o JCheckBoxMenuItem) vinculados
     * a esta Action reflejen siempre el estado correcto del modelo.
     */
    public void sincronizarEstadoConModelo() {
        if (model == null) return;
        // La propiedad SELECTED_KEY de la Action debe ser igual al estado del modelo.
        putValue(Action.SELECTED_KEY, model.isZoomToCursorEnabled());
    } // --- Fin del método sincronizarEstadoConModelo ---

} // --- FIN DE LA CLASE ToggleZoomToCursorAction ---
