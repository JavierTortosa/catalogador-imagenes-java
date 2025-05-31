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

public class ToggleLocationBarAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private ConfigurationManager configManagerRef;
    private String configKeyForState; // Esta será KEY_INTERFAZ_INFOBAR_INFERIOR_NOMBRE_RUTA_VISIBLE
    private String uiElementIdentifierForController; // Este será "REFRESH_INFO_BAR_INFERIOR"
    private VisorController controllerRef;

    // CONSTRUCTOR MODIFICADO
    public ToggleLocationBarAction(String name, // "Linea de Ubicacion del Archivo"
                               ImageIcon icon,
                               ConfigurationManager configManager,
                               VisorController controller,
                               String configKeyForSelectedState, // Debería ser KEY_INTERFAZ_INFOBAR_INFERIOR_NOMBRE_RUTA_VISIBLE
                               String uiElementId, // Debería ser "REFRESH_INFO_BAR_INFERIOR"
                               String actionCommandKey) { // CMD_VISTA_TOGGLE_LOCATION_BAR
        super(name, icon);

        this.configManagerRef = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null");
        this.configKeyForState = Objects.requireNonNull(configKeyForSelectedState, "configKeyForState no puede ser null");
        this.uiElementIdentifierForController = Objects.requireNonNull(uiElementId, "uiElementId no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Mostrar u ocultar la Línea de Ubicación del Archivo (en barra inferior)");
        putValue(Action.ACTION_COMMAND_KEY, Objects.requireNonNull(actionCommandKey, "actionCommandKey no puede ser null"));

        // Leer el estado inicial desde la clave de configuración correcta
        boolean initialState = this.configManagerRef.getBoolean(this.configKeyForState, true);
        putValue(Action.SELECTED_KEY, initialState);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (configManagerRef == null || controllerRef == null) {
            System.err.println("ERROR CRÍTICO [ToggleLocationBarAction]: ConfigManager o ControllerRef nulos.");
            return;
        }

        boolean nuevoEstadoVisible = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        System.out.println("[ToggleLocationBarAction actionPerformed] para UI ID: " + uiElementIdentifierForController +
                           " (afectando configKey: " + configKeyForState + ")" +
                           ", nuevo estado de visibilidad: " + nuevoEstadoVisible);

        // 1. Guardar el nuevo estado en ConfigurationManager
        //    La configKeyForState ya debería ser la correcta para el JLabel de nombre/ruta de la barra inferior.
        this.configManagerRef.setString(this.configKeyForState, String.valueOf(nuevoEstadoVisible));
        System.out.println("  -> [ToggleLocationBarAction] Estado guardado en config: " + this.configKeyForState + " = " + nuevoEstadoVisible);

        // 2. Notificar al VisorController para que actualice la UI (la barra inferior)
        this.controllerRef.solicitarActualizacionInterfaz(
            this.uiElementIdentifierForController, // Será "REFRESH_INFO_BAR_INFERIOR"
            this.configKeyForState,
            nuevoEstadoVisible
        );
    }
}