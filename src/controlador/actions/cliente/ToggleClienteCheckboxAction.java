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

import controlador.commands.AppActionCommands;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;

public class ToggleClienteCheckboxAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ToggleClienteCheckboxAction.class);

    private final VisorModel model;
    private final ComponentRegistry registry;

    public ToggleClienteCheckboxAction(String name, ImageIcon icon, VisorModel model,
                                       ComponentRegistry registry) {
        super(name, icon);
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Mostrar/ocultar checkboxes en las imágenes del cliente");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_CLIENTE_TOGGLE_CHECKBOX);
        putValue(Action.SELECTED_KEY, model.isClienteCheckboxVisible());
    } // --- FIN de constructor ToggleClienteCheckboxAction ---

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean nuevoEstado = !model.isClienteCheckboxVisible();
        logger.debug("[ToggleClienteCheckboxAction] Cambiando estado a: {}", nuevoEstado);
        model.setClienteCheckboxVisible(nuevoEstado);
        putValue(Action.SELECTED_KEY, nuevoEstado);

        JPanel displayContainer = registry.get("container.displaymodes.cliente.wrapper");
        if (displayContainer != null && displayContainer.getLayout() instanceof CardLayout) {
            CardLayout cl = (CardLayout) displayContainer.getLayout();
            if (nuevoEstado) {
                cl.show(displayContainer, "DISPLAY_REVIEW");
                logger.debug("[ToggleClienteCheckboxAction] Mostrando panel de revisión de checkboxes.");
            } else {
                cl.show(displayContainer, "DISPLAY_NORMAL");
                logger.debug("[ToggleClienteCheckboxAction] Mostrando panel normal de cliente.");
            }
        }
    } // --- FIN de metodo actionPerformed ---

} // --- FIN de clase ToggleClienteCheckboxAction ---
