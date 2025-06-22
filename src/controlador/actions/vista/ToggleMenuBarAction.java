package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

//import controlador.managers.ViewManager;
import controlador.managers.interfaces.IViewManager;
import servicios.ConfigurationManager;
// No se necesita importar VisorView

public class ToggleMenuBarAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private final ConfigurationManager configManagerRef;
    private final String configKeyForState;
    private final String componentIdForController; 
    private final IViewManager viewManager;  

    public ToggleMenuBarAction(String name,
                               ImageIcon icon,
                               ConfigurationManager configManager,
                               IViewManager viewManager,
                               String configKeyForSelectedState,
                               String componentIdentifier,
                               String actionCommandKey) {
        super(name, icon);

        this.configManagerRef = Objects.requireNonNull(configManager);
        this.viewManager = Objects.requireNonNull(viewManager);
        this.configKeyForState = Objects.requireNonNull(configKeyForSelectedState);
        this.componentIdForController = Objects.requireNonNull(componentIdentifier);

        putValue(Action.SHORT_DESCRIPTION, "Mostrar u ocultar la " + name);
        putValue(Action.ACTION_COMMAND_KEY, Objects.requireNonNull(actionCommandKey));

        boolean initialState = this.configManagerRef.getBoolean(this.configKeyForState, true);
        putValue(Action.SELECTED_KEY, initialState);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (configManagerRef == null || viewManager == null) {
            System.err.println("ERROR CRÍTICO [ToggleMenuBarAction]: ConfigManager o viewManager nulos.");
            return;
        }

        // El estado seleccionado del JCheckBoxMenuItem se actualiza automáticamente.
        // Lo leemos para saber cuál es el nuevo estado deseado.
        boolean nuevaVisibilidadComponente = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        
        System.out.println("[ToggleMenuBarAction actionPerformed] para: " + componentIdForController +
                           ", nuevo estado de visibilidad: " + nuevaVisibilidadComponente);

        // 1. Guardar el nuevo estado en la configuración.
        this.configManagerRef.setString(this.configKeyForState, String.valueOf(nuevaVisibilidadComponente));
        
        // 2. Notificar al VisorController para que actualice la UI.
        this.viewManager.solicitarActualizacionUI(
            this.componentIdForController,
            this.configKeyForState,
            nuevaVisibilidadComponente
        );
    }
} // --- FIN de la clase ToggleMenuBarAction ---