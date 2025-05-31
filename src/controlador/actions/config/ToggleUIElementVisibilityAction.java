package controlador.actions.config;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem; // Para obtener el estado si es necesario (aunque Action.SELECTED_KEY es preferible)
import servicios.ConfigurationManager;
import controlador.VisorController;

public class ToggleUIElementVisibilityAction extends AbstractAction {
    private static final long serialVersionUID = 1L; // Genera uno si lo deseas

    private final VisorController controller;
    private final ConfigurationManager configManager;
    private final String configKeyForVisibilityState; // La clave en ConfigurationManager para el estado booleano de este toggle
    private final String uiElementIdentifier; // Un identificador para que el controller sepa qué actualizar en la vista

    /**
     * Constructor para una Action que alterna la visibilidad de un elemento de UI
     * y guarda su estado en la configuración.
     *
     * @param controller        Referencia al VisorController para notificar actualizaciones de UI.
     * @param configManager     El gestor de configuración para leer/guardar el estado.
     * @param name              El texto que se mostrará en el JCheckBoxMenuItem.
     * @param configKey         La clave única en ConfigurationManager para el estado de visibilidad de este elemento.
     * @param uiElementId       Un identificador único para este elemento de UI, usado por el controller para saber qué actualizar en la vista.
     * @param actionCommandKey  El comando canónico (de AppActionCommands) asociado a esta Action.
     */
    public ToggleUIElementVisibilityAction(VisorController controller, ConfigurationManager configManager,
                                           String name, String configKey, String uiElementId, String actionCommandKey) {
        super(name); // Establece el nombre de la Action (usado por JMenuItem)
        this.controller = controller;
        this.configManager = configManager;
        this.configKeyForVisibilityState = configKey;
        this.uiElementIdentifier = uiElementId;

        // Establecer el ActionCommandKey (útil si alguna vez necesitas identificar la Action por su comando)
        putValue(Action.ACTION_COMMAND_KEY, actionCommandKey);

        // Establecer el tooltip (opcional, pero buena práctica)
        putValue(Action.SHORT_DESCRIPTION, "Mostrar/Ocultar " + name);

        // Inicializar el estado de SELECCIÓN (Action.SELECTED_KEY) de esta Action
        // leyendo el valor actual de la configuración.
        // JCheckBoxMenuItem se vinculará automáticamente a este estado.
        boolean isCurrentlyVisible = configManager.getBoolean(this.configKeyForVisibilityState, true); // true como default si no se encuentra
        putValue(Action.SELECTED_KEY, isCurrentlyVisible);

        System.out.println("[ToggleUIElementVisibilityAction CSTR] '" + name + "' (clave config: " + configKey +
                           ", UI ID: " + uiElementId + ") inicializada. SELECTED_KEY: " + isCurrentlyVisible);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Cuando un JCheckBoxMenuItem vinculado a esta Action es clickeado,
        // Swing actualiza automáticamente la propiedad Action.SELECTED_KEY ANTES de llamar a actionPerformed.
        boolean nuevoEstadoVisible = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));

        // Guardar el nuevo estado en ConfigurationManager
        configManager.setString(configKeyForVisibilityState, String.valueOf(nuevoEstadoVisible));

        System.out.println("[ToggleUIElementVisibilityAction] '" + getValue(Action.NAME) +
                           "' toggled. Nueva visibilidad: " + nuevoEstadoVisible +
                           ". Clave config actualizada: " + configKeyForVisibilityState);

        // Notificar al VisorController para que actualice la UI
        if (controller != null) {
            // Pasamos el identificador del elemento UI y el nuevo estado de visibilidad.
            // El controller luego le dirá a la VisorView qué hacer.
//            controller.solicitarActualizacionVisibilidadUI(uiElementIdentifier, configKeyForVisibilityState, nuevoEstadoVisible);
        	controller.solicitarActualizacionInterfaz(uiElementIdentifier, configKeyForVisibilityState, nuevoEstadoVisible);
        } else {
            System.err.println("ERROR [ToggleUIElementVisibilityAction]: VisorController es null. No se puede notificar actualización de UI.");
        }
    }
} //--- FIN de la clase ToggleUIElementVisibilityAction