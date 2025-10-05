package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import controlador.GeneralController;
import controlador.commands.AppActionCommands;

public class EliminarProyectoAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
//    private static final Logger logger = LoggerFactory.getLogger(EliminarProyectoAction.class);
    
    private final GeneralController generalController;

    public EliminarProyectoAction(GeneralController controller, String name, ImageIcon icon) {
        super(name, icon);
        this.generalController = Objects.requireNonNull(controller, "GeneralController no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Elimina un archivo de proyecto del disco");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_ELIMINAR); // Añadir a AppActionCommands
    } // --- Fin del método EliminarProyectoAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        generalController.handleDeleteProject();
        
    } // --- FIN del método actionPerformed ---

} // --- FIN de la clase EliminarProyectoAction ---