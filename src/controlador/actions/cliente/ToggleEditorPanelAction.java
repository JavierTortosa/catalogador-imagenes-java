package controlador.actions.cliente;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.commands.AppActionCommands;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;

public class ToggleEditorPanelAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ToggleEditorPanelAction.class);

    private final VisorModel model;
    private final ComponentRegistry registry;

    public ToggleEditorPanelAction(String name, ImageIcon icon, VisorModel model,
                                   ComponentRegistry registry) {
        super(name, icon);
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Mostrar/ocultar el panel de edición de checkboxes");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_CLIENTE_TOGGLE_EDITOR_PANEL);
        putValue(Action.SELECTED_KEY, model.isClienteEditorPanelVisible());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean nuevoEstado = !model.isClienteEditorPanelVisible();
        logger.debug("[ToggleEditorPanelAction] Cambiando estado a: {}", nuevoEstado);
        model.setClienteEditorPanelVisible(nuevoEstado);
        putValue(Action.SELECTED_KEY, nuevoEstado);
    }

}
