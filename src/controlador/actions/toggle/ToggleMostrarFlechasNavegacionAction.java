package controlador.actions.toggle;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.managers.interfaces.IViewManager;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.panels.ImageDisplayPanel;

public class ToggleMostrarFlechasNavegacionAction extends AbstractAction {
    private static final long serialVersionUID = 1L;
    private final ConfigurationManager configManagerRef;
    private final IViewManager viewManagerRef;
    private final String configKey;

    public ToggleMostrarFlechasNavegacionAction(String name, ImageIcon icon,
                                                  ConfigurationManager configManager,
                                                  IViewManager viewManager,
                                                  String actionCommandKey) {
        super(name, icon);
        this.configManagerRef = configManager;
        this.viewManagerRef = viewManager;
        this.configKey = ConfigKeys.COMPORTAMIENTO_MOSTRAR_FLECHAS;

        putValue(Action.ACTION_COMMAND_KEY, actionCommandKey);
        putValue(Action.SHORT_DESCRIPTION, "Muestra/oculta las flechas de navegación sobre la imagen");

        boolean initialState = this.configManagerRef.getBoolean(this.configKey, true);
        putValue(Action.SELECTED_KEY, initialState);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean newState = (Boolean) getValue(Action.SELECTED_KEY);
        configManagerRef.setString(configKey, String.valueOf(newState));
        if (viewManagerRef != null) {
            ImageDisplayPanel display = viewManagerRef.getActiveDisplayPanel();
            if (display != null) {
                display.setNavigationArrowsVisible(newState);
            }
        }
    }
}
