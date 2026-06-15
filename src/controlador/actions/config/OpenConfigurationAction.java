package controlador.actions.config;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import controlador.managers.ConfigApplicationManager;
import servicios.ConfigurationManager;
import vista.configuracion.ConfigurationDialog;
import vista.theme.ThemeManager;

public class OpenConfigurationAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final transient ConfigurationManager config;
    private final transient ConfigApplicationManager configAppManager;
    private final transient ThemeManager themeManager;

    public OpenConfigurationAction(String name, ConfigurationManager config,
                                    ConfigApplicationManager configAppManager, ThemeManager themeManager) {
        super(name);
        this.config = config;
        this.configAppManager = configAppManager;
        this.themeManager = themeManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFrame owner = null;
        if (e.getSource() instanceof Component) {
            owner = (JFrame) SwingUtilities.getWindowAncestor((Component) e.getSource());
        }
        ConfigurationDialog dialog = new ConfigurationDialog(owner, config, configAppManager, themeManager);
        dialog.setVisible(true);
    }

}
