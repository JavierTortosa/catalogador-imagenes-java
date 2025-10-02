package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.GeneralController;
import controlador.commands.AppActionCommands;

public class GestionarProyectoAction extends AbstractAction {

	private static final Logger logger = LoggerFactory.getLogger(GestionarProyectoAction.class);
	
    private static final long serialVersionUID = 1L;
    private final GeneralController generalController; 

    public GestionarProyectoAction(GeneralController generalController, String name, ImageIcon icon) {
        super(name, icon);
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Ver/Gestionar la selección de imágenes del proyecto");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_GESTIONAR);
    } // --- Fin del constructor ---

    
    @Override
    public void actionPerformed(ActionEvent e) {
        logger.info ("\n\nDEBUG: Se ha pulsado GestionarProyectoAction!"); // <-- AÑADE ESTO
        if (generalController == null) {
            System.err.println("ERROR CRÍTICO [GestionarProyectoAction]: GeneralController es nulo.");
            return;
        }
        
        logger.info ("DEBUG: Llamando a generalController.solicitarEntrarEnModoProyecto()..."); // <-- AÑADE ESTO
        generalController.solicitarEntrarEnModoProyecto();
    } // --- Fin del método actionPerformed ---

} // --- Fin de la clase GestionarProyectoAction ---