package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.GeneralController;
import controlador.commands.AppActionCommands;

public class NuevoProyectoAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final GeneralController generalController;

    
    public NuevoProyectoAction(GeneralController generalController, String name, ImageIcon icon) { 
        super(name, icon);
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Crea un nuevo proyecto vacío");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_NUEVO);
        
    } // --- Fin del método NuevoProyectoAction (constructor) ---
    
    @Override
    public void actionPerformed(ActionEvent e) {
        generalController.handleNewProject();
    } // --- FIN del método actionPerformed ---

} // --- FIN de la clase NuevoProyectoAction ---

