package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import servicios.ConfigurationManager;

public class ToggleToolBarAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private final ConfigurationManager configManagerRef;
    private final String configKeyBase; // Antes: configKeyForState
    private final String uiElementIdentifierForController;
    private final VisorController controllerRef;

    /**
     * Constructor MODIFICADO para una Action que alterna la visibilidad de la barra de herramientas principal.
     * AHORA espera la clave de configuración BASE y añade el sufijo ".seleccionado" internamente.
     *
     * @param name           El texto que se mostrará en el JCheckBoxMenuItem (ej. "Barra de Botones").
     * @param icon           El icono para la Action (puede ser null).
     * @param configManager  El gestor de configuración.
     * @param controller     La referencia al controlador principal.
     * @param configKeyBase  La clave BASE en ConfigurationManager (ej. "interfaz.menu.vista.barra_de_botones").
     * @param uiElementId    El identificador único para la UI (ej. "Barra_de_Botones").
     * @param actionCommandKey El comando canónico asociado a esta Action.
     */
    public ToggleToolBarAction(String name,
                               ImageIcon icon,
                               ConfigurationManager configManager,
                               VisorController controller,
                               String configKeyBase, // <<< CAMBIO: Nombre del parámetro
                               String uiElementId,
                               String actionCommandKey) {
        super(name, icon);

        this.configManagerRef = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null");
        this.configKeyBase = Objects.requireNonNull(configKeyBase, "configKeyBase no puede ser null");
        this.uiElementIdentifierForController = Objects.requireNonNull(uiElementId, "uiElementId no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Mostrar u ocultar la " + name);
        putValue(Action.ACTION_COMMAND_KEY, Objects.requireNonNull(actionCommandKey, "actionCommandKey no puede ser null"));

        // --- CAMBIO: Construir la clave completa para leer el estado inicial ---
        // La clave para un checkbox de menú es "...seleccionado"
        String fullStateKey = this.configKeyBase + ".seleccionado";
        boolean initialState = this.configManagerRef.getBoolean(fullStateKey, true);
        putValue(Action.SELECTED_KEY, initialState);
        
        System.out.println("[ToggleToolBarAction CSTR] '" + name + "' (clave base: " + this.configKeyBase +
                           ", UI ID: " + this.uiElementIdentifierForController + ") inicializada. Leyendo de '" + fullStateKey + "'. SELECTED_KEY: " + initialState);
    } // --- FIN del Constructor ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (configManagerRef == null || controllerRef == null) {
            System.err.println("ERROR CRÍTICO [ToggleToolBarAction]: ConfigManager o ControllerRef nulos.");
            return;
        }

        boolean nuevoEstadoVisible = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        
        // --- CAMBIO: Construir la clave completa para guardar el nuevo estado ---
        String fullStateKey = this.configKeyBase + ".seleccionado";
        this.configManagerRef.setString(fullStateKey, String.valueOf(nuevoEstadoVisible));
        
        System.out.println("[ToggleToolBarAction actionPerformed] para UI ID: " + this.uiElementIdentifierForController +
                           ", nuevo estado de visibilidad: " + nuevoEstadoVisible);
        System.out.println("  -> [ToggleToolBarAction] Estado guardado en config: " + fullStateKey + " = " + nuevoEstadoVisible);

        // Notificar al VisorController para que actualice la UI
        this.controllerRef.solicitarActualizacionInterfaz(
            this.uiElementIdentifierForController, // "Barra_de_Botones"
            fullStateKey,                          // La clave completa que se modificó
            nuevoEstadoVisible
        );
    } // --- FIN del método actionPerformed ---
    
} // --- FIN de la clase ToggleToolBarAction ---