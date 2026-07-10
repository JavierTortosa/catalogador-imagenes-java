package controlador.actions.config;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import controlador.managers.ConfigApplicationManager;
import controlador.managers.DataManager;
import controlador.utils.ComponentRegistry;
import servicios.ConfigurationManager;
import vista.configuracion.ConfigurationDialog;
import vista.theme.ThemeManager;

public class OpenConfigurationAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final transient ConfigurationManager config;
    private final transient ConfigApplicationManager configAppManager;
    private final transient ThemeManager themeManager;
    private final transient DataManager dataManager;
    private final transient ComponentRegistry registry;

    public OpenConfigurationAction(String name, ConfigurationManager config,
                                    ConfigApplicationManager configAppManager, ThemeManager themeManager,
                                    DataManager dataManager, ComponentRegistry registry) {
        super(name);
        this.config = config;
        this.configAppManager = configAppManager;
        this.themeManager = themeManager;
        this.dataManager = dataManager;
        this.registry = registry;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFrame owner = null;
        if (e.getSource() instanceof Component) {
            owner = (JFrame) SwingUtilities.getWindowAncestor((Component) e.getSource());
        }
        ConfigurationDialog dialog = new ConfigurationDialog(owner, config, configAppManager, themeManager,
                dataManager, registry);
        dialog.setVisible(true);
    }

}
