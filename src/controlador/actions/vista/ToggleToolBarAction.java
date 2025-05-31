package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController; // <<<< NECESARIO
import servicios.ConfigurationManager;
// Ya no necesitamos ViewManager aquí si el controller hace el despacho
// import controlador.managers.ViewManager;

public class ToggleToolBarAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    // private ViewManager viewManagerRef; // Ya no se usa aquí
    private ConfigurationManager configManagerRef;
    private String configKeyForState;
    private String uiElementIdentifierForController; // Este es el ID que el controller usará
    private VisorController controllerRef; // <<<< AÑADIR REFERENCIA AL CONTROLLER

    // CONSTRUCTOR MODIFICADO
    public ToggleToolBarAction(String name,
                               ImageIcon icon,
                               // ViewManager viewManager, // Ya no se pasa directamente
                               ConfigurationManager configManager,
                               VisorController controller, // <<<< AÑADIR PARÁMETRO CONTROLLER
                               String configKeyForSelectedState,
                               String uiElementId, // Renombrado para claridad
                               String actionCommandKey) {
        super(name, icon);

        this.configManagerRef = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null"); // <<<< ASIGNAR
        this.configKeyForState = Objects.requireNonNull(configKeyForSelectedState, "configKeyForState no puede ser null");
        this.uiElementIdentifierForController = Objects.requireNonNull(uiElementId, "uiElementId no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Mostrar u ocultar la " + name); // "la Barra de Botones"
        putValue(Action.ACTION_COMMAND_KEY, Objects.requireNonNull(actionCommandKey, "actionCommandKey no puede ser null"));

        boolean initialState = this.configManagerRef.getBoolean(this.configKeyForState, true);
        putValue(Action.SELECTED_KEY, initialState);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (configManagerRef == null || controllerRef == null) {
            System.err.println("ERROR CRÍTICO [ToggleToolBarAction]: ConfigManager o ControllerRef nulos.");
            return;
        }

        boolean nuevoEstadoVisible = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        System.out.println("[ToggleToolBarAction actionPerformed] para UI ID: " + uiElementIdentifierForController +
                           ", nuevo estado de visibilidad: " + nuevoEstadoVisible);

        // 1. Guardar el nuevo estado en ConfigurationManager
        this.configManagerRef.setString(this.configKeyForState, String.valueOf(nuevoEstadoVisible));
        System.out.println("  -> [ToggleToolBarAction] Estado guardado en config: " + this.configKeyForState + " = " + nuevoEstadoVisible);

        // 2. Notificar al VisorController para que actualice la UI
        this.controllerRef.solicitarActualizacionInterfaz(
            this.uiElementIdentifierForController, // Será "Barra_de_Botones"
            this.configKeyForState,
            nuevoEstadoVisible
        );
    }
}