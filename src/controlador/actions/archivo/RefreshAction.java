package controlador.actions.archivo;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import java.util.Objects;

import controlador.GeneralController; 
import controlador.commands.AppActionCommands;

public class RefreshAction extends AbstractAction {

    private static final long serialVersionUID = 2L;
    private final GeneralController generalController; 

    public RefreshAction(String name, ImageIcon icon, GeneralController controller) { 
        super(name, icon);
        this.generalController = Objects.requireNonNull(controller, "GeneralController no puede ser null en RefreshAction");
        putValue(Action.SHORT_DESCRIPTION, "Recarga la lista de archivos y refresca la interfaz de usuario");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ESPECIAL_REFRESCAR); 
    } // FIN del metodo RefreshAction

    @Override
    public void actionPerformed(ActionEvent e) {
        // La Action solo notifica al GeneralController para que él decida qué hacer.
        generalController.solicitarRefrescoDelModoActivo();
    } // FIN del metodo actionPerformed
    
} // FIN de la clase RefreshAction
