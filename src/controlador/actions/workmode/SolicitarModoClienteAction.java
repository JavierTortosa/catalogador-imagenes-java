package controlador.actions.workmode;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.GeneralController;
import controlador.commands.AppActionCommands;

public class SolicitarModoClienteAction extends AbstractAction {

    private static final Logger logger = LoggerFactory.getLogger(SolicitarModoClienteAction.class);

    private static final long serialVersionUID = 1L;
    private final GeneralController generalController;

    public SolicitarModoClienteAction(GeneralController generalController, String name, ImageIcon icon) {
        super(name, icon);
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Entrar en modo cliente");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_MODO_CLIENTE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        logger.debug("[SolicitarModoClienteAction] Acción disparada para entrar en modo cliente.");
        if (generalController == null) {
            System.err.println("ERROR CRÍTICO [SolicitarModoClienteAction]: GeneralController es nulo.");
            return;
        }
        generalController.solicitarEntrarEnModoCliente();
    }

} // --- Fin de la clase SolicitarModoClienteAction ---
