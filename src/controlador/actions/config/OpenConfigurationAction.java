package controlador.actions.config;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import controlador.managers.ConfigApplicationManager;
import servicios.ConfigurationManager;
import vista.configuracion.ConfigurationDialog;
import vista.theme.ThemeManager;

public class OpenConfigurationAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
	private final JFrame owner;
    private final transient ConfigurationManager config;
    private final transient ConfigApplicationManager configAppManager;
    private final transient ThemeManager themeManager;

    public OpenConfigurationAction(String name, JFrame owner, ConfigurationManager config,
                                    ConfigApplicationManager configAppManager, ThemeManager themeManager) {
        super(name);
        this.owner = owner;
        this.config = config;
        this.configAppManager = configAppManager;
        this.themeManager = themeManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ConfigurationDialog dialog = new ConfigurationDialog(owner, config, configAppManager, themeManager);
        dialog.setVisible(true);
    }

}
