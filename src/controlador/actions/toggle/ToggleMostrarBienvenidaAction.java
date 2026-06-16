package controlador.actions.toggle;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;

public class ToggleMostrarBienvenidaAction extends AbstractAction {
    private static final long serialVersionUID = 1L;
    private final ConfigurationManager configManagerRef;
    private final String configKey;

    public ToggleMostrarBienvenidaAction(String name, ImageIcon icon,
                                          ConfigurationManager configManager,
                                          String actionCommandKey) {
        super(name, icon);
        this.configManagerRef = configManager;
        this.configKey = ConfigKeys.COMPORTAMIENTO_MOSTRAR_BIENVENIDA;

        putValue(Action.ACTION_COMMAND_KEY, actionCommandKey);
        putValue(Action.SHORT_DESCRIPTION, "Activa/Desactiva la imagen de bienvenida al iniciar");

        boolean initialState = this.configManagerRef.getBoolean(this.configKey, true);
        putValue(Action.SELECTED_KEY, initialState);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean newState = (Boolean) getValue(Action.SELECTED_KEY);
        configManagerRef.setString(configKey, String.valueOf(newState));
    }
}
