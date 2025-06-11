package controlador.actions.config;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;

import controlador.VisorController;
import servicios.ConfigurationManager;

public class ToggleUIElementVisibilityAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    private final VisorController controller;
    private final ConfigurationManager configManager;
    // --- CAMBIO 1: Renombrar el campo para mayor claridad ---
    private final String configKeyBase; // Antes: configKeyForVisibilityState
    private final String uiElementIdentifier;

    /**
     * Constructor MODIFICADO para una Action que alterna la visibilidad de un elemento de UI.
     * AHORA espera la clave de configuración BASE y añade el sufijo ".visible" internamente.
     *
     * @param controller        Referencia al VisorController para notificar actualizaciones de UI.
     * @param configManager     El gestor de configuración para leer/guardar el estado.
     * @param name              El texto que se mostrará en el JCheckBoxMenuItem.
     * @param configKeyBase     La clave BASE en ConfigurationManager (ej. "interfaz.herramientas.edicion").
     * @param uiElementId       Un identificador único para este elemento de UI (ej. "edicion").
     * @param actionCommandKey  El comando canónico asociado a esta Action.
     */
    public ToggleUIElementVisibilityAction(	VisorController controller, 
    										ConfigurationManager configManager,
    										String name, 
    										String configKeyBase, // <<< CAMBIO 2: Nombre del parámetro
    										String uiElementId, 
    										String actionCommandKey) {
    	
        super(name);
        this.controller = controller;
        this.configManager = configManager;
        // --- CAMBIO 1: Asignar al nuevo campo ---
        this.configKeyBase = configKeyBase;
        this.uiElementIdentifier = uiElementId;

        putValue(Action.ACTION_COMMAND_KEY, actionCommandKey);
        putValue(Action.SHORT_DESCRIPTION, "Mostrar/Ocultar " + name);

        // --- CAMBIO 2: Construir la clave completa para leer el estado inicial ---
        String fullVisibilityKey = this.configKeyBase + ".visible";
        boolean isCurrentlyVisible = configManager.getBoolean(fullVisibilityKey, true); // true como default
        putValue(Action.SELECTED_KEY, isCurrentlyVisible);

        System.out.println("[ToggleUIElementVisibilityAction CSTR] '" + name + "' (clave base: " + configKeyBase +
                           ", UI ID: " + uiElementId + ") inicializada. Leyendo de '" + fullVisibilityKey + "'. SELECTED_KEY: " + isCurrentlyVisible);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean nuevoEstadoVisible = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));

        // --- CAMBIO 3: Construir la clave completa para guardar el nuevo estado ---
        String fullVisibilityKey = this.configKeyBase + ".visible";
        configManager.setString(fullVisibilityKey, String.valueOf(nuevoEstadoVisible));

        System.out.println("[ToggleUIElementVisibilityAction] '" + getValue(Action.NAME) +
                           "' toggled. Nueva visibilidad: " + nuevoEstadoVisible +
                           ". Clave config actualizada: " + fullVisibilityKey);

        // Notificar al VisorController
        if (controller != null) {
        	// Se pasa el uiIdentifier (ej. "edicion") y la clave COMPLETA que se modificó
        	controller.solicitarActualizacionInterfaz(uiElementIdentifier, fullVisibilityKey, nuevoEstadoVisible);
        } else {
            System.err.println("ERROR [ToggleUIElementVisibilityAction]: VisorController es null.");
        }
    }
} // --- FIN de la clase ToggleUIElementVisibilityAction ---