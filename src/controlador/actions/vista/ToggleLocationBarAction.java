package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon; 
import servicios.ConfigurationManager;
import vista.VisorView; // No se usa directamente aquí si ViewManager lo maneja
import controlador.managers.ViewManager;


public class ToggleLocationBarAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private ViewManager viewManagerRef;
    private ConfigurationManager configManagerRef; 
    private String configKeyForState;       
    private String componentIdForViewManager; 

    // CONSTRUCTOR ACTUALIZADO PARA ACEPTAR 7 ARGUMENTOS
    public ToggleLocationBarAction(String name, 
                               ImageIcon icon, 
                               ViewManager viewManager,
                               ConfigurationManager configManager, 
                               String configKeyForSelectedState,
                               String componentIdentifier,
                               String actionCommandKey) { // <--- AÑADIDO actionCommandKey
        super(name, icon); 
        
        this.viewManagerRef = Objects.requireNonNull(viewManager, "ViewManager no puede ser null");
        this.configManagerRef = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
        this.configKeyForState = Objects.requireNonNull(configKeyForSelectedState, "configKeyForSelectedState no puede ser null");
        this.componentIdForViewManager = Objects.requireNonNull(componentIdentifier, "componentIdentifier no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Mostrar u ocultar la " + name);
        putValue(Action.ACTION_COMMAND_KEY, Objects.requireNonNull(actionCommandKey, "actionCommandKey no puede ser null")); // Usar el parámetro

        boolean initialState = this.configManagerRef.getBoolean(this.configKeyForState, true);
        putValue(Action.SELECTED_KEY, initialState);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (viewManagerRef == null || configManagerRef == null) { // configManagerRef no se usa aquí, pero está bien el chequeo
            System.err.println("ERROR CRÍTICO [ToggleLocationBarAction]: ViewManager es nulo.");
            return;
        }

        boolean nuevoEstadoVisible = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        System.out.println("[ToggleLocationBarAction actionPerformed] para: " + componentIdForViewManager + ", nuevo estado: " + nuevoEstadoVisible);

        this.viewManagerRef.setComponentePrincipalVisible(
            this.componentIdForViewManager, 
            nuevoEstadoVisible, 
            this.configKeyForState
        );
    }
}

