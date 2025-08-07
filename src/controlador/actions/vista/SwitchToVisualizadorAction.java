package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.AppInitializer;
import controlador.GeneralController;
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;

// NOTA: Asegúrate de que esta clase implementa 'ContextSensitiveAction'
// y que todas las importaciones necesarias están presentes en tu archivo.

public class SwitchToVisualizadorAction extends AbstractAction implements ContextSensitiveAction {

	private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);
	
    private static final long serialVersionUID = 1L;
    
    private final GeneralController generalController;

    public SwitchToVisualizadorAction(String name, ImageIcon icon, GeneralController generalController) {
        super(name, icon);
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Volver a la vista del Visualizador de Imágenes");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR);
        putValue(Action.SELECTED_KEY, false); 
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (generalController == null) {
            logger.error("ERROR CRÍTICO [SwitchToVisualizadorAction]: GeneralController es nulo. Esto no debería ocurrir si las dependencias se inyectan correctamente.");
            return;
        }
        
        logger.debug("[SwitchToVisualizadorAction] Solicitando cambio al modo VISUALIZADOR.");
        generalController.cambiarModoDeTrabajo(VisorModel.WorkMode.VISUALIZADOR);
    }
    
    /**
     * Implementación del método de la interfaz ContextSensitiveAction.
     * Actualiza el estado de selección de esta acción para que refleje
     * si el WorkMode.VISUALIZADOR es el modo de trabajo actual del modelo.
     *
     * @param model El VisorModel que contiene el estado actual de la aplicación.
     */
    @Override 
    public void updateEnabledState(VisorModel model) { 
        if (model == null) {
            logger.warn("WARN [SwitchToVisualizadorAction.updateEnabledState]: VisorModel es nulo. No se puede sincronizar el estado.");
            putValue(Action.SELECTED_KEY, false);
            setEnabled(false);
            return;
        }

        WorkMode currentWorkMode = model.getCurrentWorkMode();
        boolean isSelected = (currentWorkMode == WorkMode.VISUALIZADOR);
        
        // --- INICIO MODIFICACIÓN: Línea reemplazada ---
        // La línea 'if (Boolean.TRUE.equals(getValue(Action.SELECTED_KEY)) != isSelected)'
        // se ha eliminado. Ahora se asigna el valor directamente.
        putValue(Action.SELECTED_KEY, isSelected);
        // --- FIN MODIFICACIÓN ---

        logger.debug("  [SwitchToVisualizadorAction] Sincronizada acción '" + getValue(Action.NAME) + "'. Seleccionado: " + isSelected);
        
        // El botón debe estar habilitado para que los colores de FlatLaf (y nuestro pintado manual) funcionen.
        // Un ButtonGroup gestiona que solo uno esté seleccionado.
        setEnabled(true); // Siempre habilitamos esta acción.
    }
}