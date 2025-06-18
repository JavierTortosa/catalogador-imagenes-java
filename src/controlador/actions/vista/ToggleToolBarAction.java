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
    private final String configKeyForState; // Clave completa con .seleccionado
    private final String uiElementIdentifierForController;
    private final VisorController controllerRef;

    public ToggleToolBarAction(String name,
                               ImageIcon icon,
                               ConfigurationManager configManager,
                               VisorController controller,
                               String configKeyBase, // Recibe la clave base
                               String uiElementId,
                               String actionCommandKey) {
        super(name, icon);

        this.configManagerRef = Objects.requireNonNull(configManager);
        this.controllerRef = Objects.requireNonNull(controller);
        Objects.requireNonNull(configKeyBase);
        this.uiElementIdentifierForController = Objects.requireNonNull(uiElementId);

        // Construimos la clave completa aquí
        this.configKeyForState = configKeyBase + ".seleccionado";

        putValue(Action.SHORT_DESCRIPTION, "Mostrar u ocultar la " + name);
        putValue(Action.ACTION_COMMAND_KEY, Objects.requireNonNull(actionCommandKey));

        // Leer el estado inicial usando la clave completa
        boolean initialState = this.configManagerRef.getBoolean(this.configKeyForState, true);
        putValue(Action.SELECTED_KEY, initialState);
    } // --- FIN del Constructor ---

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean nuevoEstadoVisible = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        
        // Guardar el nuevo estado
        this.configManagerRef.setString(this.configKeyForState, String.valueOf(nuevoEstadoVisible));

        // Notificar al controlador
        this.controllerRef.solicitarActualizacionInterfaz(
            this.uiElementIdentifierForController,
            this.configKeyForState,
            nuevoEstadoVisible
        );
    } // --- FIN del método actionPerformed ---
    
} // --- FIN de la clase ToggleToolBarAction ---