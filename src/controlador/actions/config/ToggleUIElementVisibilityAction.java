package controlador.actions.config;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import controlador.interfaces.InitializableVisibility;
import controlador.managers.interfaces.IViewManager;
import servicios.ConfigurationManager;

public class ToggleUIElementVisibilityAction extends AbstractAction implements InitializableVisibility {
    private static final long serialVersionUID = 1L;

//    private final VisorController controller;
    private final IViewManager viewManager;
    private final ConfigurationManager configManager;
    
    // --- CAMBIO 1: Renombrar el campo para mayor claridad ---
    private final String configKeyBase; // Antes: configKeyForVisibilityState
    private final String uiElementIdentifier;

    /**
     * Constructor MODIFICADO y ROBUSTO para una Action que alterna la visibilidad de un elemento de UI.
     * AHORA, maneja correctamente tanto claves base como claves que ya incluyen el sufijo ".visible".
     *
     * @param controller        Referencia al VisorController para notificar actualizaciones de UI.
     * @param configManager     El gestor de configuración para leer/guardar el estado.
     * @param name              El texto que se mostrará en el JCheckBoxMenuItem.
     * @param configKey         La clave de configuración para la visibilidad. Puede ser la clave base 
     *                          (ej. "interfaz.herramientas.edicion") o la clave completa 
     *                          (ej. "interfaz.herramientas.edicion.visible").
     * @param uiElementId       Un identificador único para este elemento de UI (ej. "edicion").
     * @param actionCommandKey  El comando canónico asociado a esta Action.
     */
    public ToggleUIElementVisibilityAction( IViewManager viewManager, 
                                            ConfigurationManager configManager,
                                            String name, 
                                            String configKey, // <-- Renombrado para más claridad
                                            String uiElementId, 
                                            String actionCommandKey) {
        
        super(name);
        this.viewManager = viewManager;
        this.configManager = configManager;
        this.uiElementIdentifier = uiElementId;

        // --- LÓGICA DE CORRECCIÓN ---
        // Determinar la clave de visibilidad completa y la clave base.
        if (configKey != null && configKey.endsWith(".visible")) {
            // Si la clave ya termina en .visible, la usamos tal cual.
            this.configKeyBase = configKey; 
        } else {
            // Si no, asumimos que es una clave base y le añadimos el sufijo.
            this.configKeyBase = (configKey != null) ? configKey + ".visible" : null;
        }
        
        // Validar que no acabemos con una clave nula
        if (this.configKeyBase == null) {
            System.err.println("ERROR CRÍTICO [ToggleUIElementVisibilityAction CSTR] para '" + name + "': La clave de configuración es nula.");
            // Podríamos deshabilitar la acción o lanzar una excepción aquí.
            // Por ahora, continuará, pero probablemente fallará más adelante.
        }
        // --- FIN LÓGICA DE CORRECCIÓN ---

        putValue(Action.ACTION_COMMAND_KEY, actionCommandKey);
        putValue(Action.SHORT_DESCRIPTION, "Mostrar/Ocultar " + name);

        // Usar la clave base (que ahora sabemos que es la correcta) para leer el estado inicial.
        boolean isCurrentlyVisible = configManager.getBoolean(this.configKeyBase, true); // true como default
        putValue(Action.SELECTED_KEY, isCurrentlyVisible);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean nuevoEstadoVisible = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));

        // Usar la clave base (que ya es la clave de visibilidad completa) para guardar.
        configManager.setString(this.configKeyBase, String.valueOf(nuevoEstadoVisible));

        System.out.println("[ToggleUIElementVisibilityAction] '" + getValue(Action.NAME) +
                           "' toggled. Nueva visibilidad: " + nuevoEstadoVisible +
                           ". Clave config actualizada: " + this.configKeyBase);

        // Notificar al VisorController
        if (viewManager != null) {
        	viewManager.solicitarActualizacionUI(uiElementIdentifier, this.configKeyBase, nuevoEstadoVisible);
        } else {
            System.err.println("ERROR [ToggleUIElementVisibilityAction]: VisorController es null.");
        }
    } 
}// --- FIN de la clase ToggleUIElementVisibilityAction ---