package controlador.actions.cliente;

import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;

import controlador.factory.ActionFactory;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.commands.AppActionCommands;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;

public class SelectVisorAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SelectVisorAction.class);

    private static final String[] VIEW_COMMANDS = {
        AppActionCommands.CMD_VISTA_SINGLE,
        AppActionCommands.CMD_VISTA_GRID,
        AppActionCommands.CMD_VISTA_POLAROID
    };

    private final VisorModel model;
    private final ComponentRegistry registry;
    private final ActionFactory actionFactory;

    public SelectVisorAction(String name, ImageIcon icon, VisorModel model,
                              ComponentRegistry registry, ActionFactory actionFactory) {
        super(name, icon);
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null");
        this.actionFactory = Objects.requireNonNull(actionFactory, "ActionFactory no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Visor de imagenes");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_CLIENTE_VIEW_VISOR);
        putValue(Action.SELECTED_KEY, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        logger.debug("[SelectVisorAction] Seleccionando vista Visor.");
        model.setClienteCheckboxVisible(true);
        model.setClienteEditorPanelVisible(false);
        putValue(Action.SELECTED_KEY, true);

        setViewActionsEnabled(true);

        JPanel displayContainer = registry.get("container.displaymodes.cliente.wrapper");
        if (displayContainer != null && displayContainer.getLayout() instanceof CardLayout) {
            CardLayout cl = (CardLayout) displayContainer.getLayout();
            cl.show(displayContainer, "DISPLAY_NORMAL");
            logger.debug("[SelectVisorAction] Mostrando panel visor.");
        }
    }

    private void setViewActionsEnabled(boolean enabled) {
        Map<String, Action> actionMap = actionFactory.getActionMap();
        for (String cmd : VIEW_COMMANDS) {
            Action action = actionMap.get(cmd);
            if (action != null) {
                action.setEnabled(enabled);
            }
        }
    }

}
