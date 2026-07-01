package controlador.actions.cliente;

import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import controlador.commands.AppActionCommands;
import controlador.factory.ActionFactory;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import vista.panels.CheckboxEditorPanel;
import vista.panels.ImageDisplayPanel;

public class SelectEditorAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SelectEditorAction.class);

    private static final String[] VIEW_COMMANDS = {
        AppActionCommands.CMD_VISTA_SINGLE,
        AppActionCommands.CMD_VISTA_GRID,
        AppActionCommands.CMD_VISTA_POLAROID
    };

    private static final String[] ZOOM_COMMANDS = {
        AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR,
        AppActionCommands.CMD_ZOOM_TIPO_AUTO,
        AppActionCommands.CMD_ZOOM_TIPO_ANCHO,
        AppActionCommands.CMD_ZOOM_TIPO_ALTO,
        AppActionCommands.CMD_ZOOM_TIPO_RELLENAR,
        AppActionCommands.CMD_ZOOM_TIPO_FIJO,
        AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO,
    };

    private final VisorModel model;
    private final ComponentRegistry registry;
    private final ActionFactory actionFactory;

    public SelectEditorAction(String name, ImageIcon icon, VisorModel model,
                              ComponentRegistry registry, ActionFactory actionFactory) {
        super(name, icon);
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null");
        this.actionFactory = Objects.requireNonNull(actionFactory, "ActionFactory no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Editor de checkboxes");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_CLIENTE_TOGGLE_EDITOR_PANEL);
        putValue(Action.SELECTED_KEY, false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        logger.debug("[SelectEditorAction] Seleccionando vista Editor.");
        model.setClienteEditorPanelVisible(true);
        model.setClienteCheckboxVisible(false);
        putValue(Action.SELECTED_KEY, true);

        setZoomActionsEnabled(false);
        setViewActionsEnabled(false);

        JPanel displayContainer = registry.get("container.displaymodes.cliente.wrapper");
        if (displayContainer != null && displayContainer.getLayout() instanceof CardLayout) {
            CardLayout cl = (CardLayout) displayContainer.getLayout();
            cl.show(displayContainer, "DISPLAY_EDITOR");
            logger.debug("[SelectEditorAction] Mostrando panel editor.");
        }

        CheckboxEditorPanel editorPanel = registry.get("panel.cliente.editor");
        if (editorPanel != null) {
            ImageDisplayPanel imagePanel = editorPanel.getImagePanel();
            imagePanel.resetEditorZoom();
            imagePanel.setEditorZoomFactor(model.getZoomFactor());
            imagePanel.setEditorOffset(model.getImageOffsetX(), model.getImageOffsetY());
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

    private void setZoomActionsEnabled(boolean enabled) {
        Map<String, Action> actionMap = actionFactory.getActionMap();
        for (String cmd : ZOOM_COMMANDS) {
            Action action = actionMap.get(cmd);
            if (action != null) {
                action.setEnabled(enabled);
            }
        }
    }

}
