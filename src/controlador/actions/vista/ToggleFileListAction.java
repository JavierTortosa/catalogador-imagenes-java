package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController; // <<<< NECESARIO
import controlador.managers.ViewManager;
import servicios.ConfigurationManager;
// Ya no necesitamos ViewManager aquí si el controller hace el despacho
// import controlador.managers.ViewManager;

public class ToggleFileListAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    // private ViewManager viewManagerRef; // Ya no se usa aquí
    private ConfigurationManager configManagerRef;
    private String configKeyForState;
    private String uiElementIdentifierForController; // Este es el ID que el controller usará
    private ViewManager viewManager; // <<<< AÑADIR REFERENCIA AL CONTROLLER

    // CONSTRUCTOR MODIFICADO
    public ToggleFileListAction(String name,
                               ImageIcon icon,
                               // ViewManager viewManager, // Ya no se pasa directamente
                               ConfigurationManager configManager,
                               ViewManager viewManager, // <<<< AÑADIR PARÁMETRO CONTROLLER
                               String configKeyForSelectedState,
                               String uiElementId, // El identificador que usará el controller
                               String actionCommandKey) {
        super(name, icon);

        this.configManagerRef = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
        this.viewManager = Objects.requireNonNull(viewManager, "VisorController no puede ser null"); // <<<< ASIGNAR
        this.configKeyForState = Objects.requireNonNull(configKeyForSelectedState, "configKeyForState no puede ser null");
        this.uiElementIdentifierForController = Objects.requireNonNull(uiElementId, "uiElementId no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Mostrar u ocultar la " + name); // "la Lista de Archivos"
        putValue(Action.ACTION_COMMAND_KEY, Objects.requireNonNull(actionCommandKey, "actionCommandKey no puede ser null"));

        boolean initialState = this.configManagerRef.getBoolean(this.configKeyForState, true);
        putValue(Action.SELECTED_KEY, initialState);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (configManagerRef == null || viewManager == null) {
            System.err.println("ERROR CRÍTICO [ToggleFileListAction]: ConfigManager o ControllerRef nulos.");
            return;
        }

        boolean nuevoEstadoVisible = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        System.out.println("[ToggleFileListAction actionPerformed] para UI ID: " + uiElementIdentifierForController +
                           ", nuevo estado de visibilidad: " + nuevoEstadoVisible);

        // 1. Guardar el nuevo estado en ConfigurationManager
        this.configManagerRef.setString(this.configKeyForState, String.valueOf(nuevoEstadoVisible));
        System.out.println("  -> [ToggleFileListAction] Estado guardado en config: " + this.configKeyForState + " = " + nuevoEstadoVisible);

        // 2. Notificar al VisorController para que actualice la UI
        this.viewManager.solicitarActualizacionUI(
            this.uiElementIdentifierForController, // Será "mostrar_ocultar_la_lista_de_archivos"
            this.configKeyForState,
            nuevoEstadoVisible
        );
    }
}