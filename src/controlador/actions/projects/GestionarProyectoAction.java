// Contenido para GestionarProyectoAction.java
package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.GeneralController;
import controlador.commands.AppActionCommands;

public class GestionarProyectoAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final GeneralController generalController; 

    public GestionarProyectoAction(GeneralController generalController, String name, ImageIcon icon) {
        super(name, icon);
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Ver/Gestionar la selección de imágenes del proyecto");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_GESTIONAR);
    } // --- Fin del constructor ---

//    @Override
//    public void actionPerformed(ActionEvent e) {
//    	
//    	
//        if (generalController == null) { return; }
//        generalController.solicitarEntrarEnModoProyecto();
    
    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("\n\nDEBUG: Se ha pulsado GestionarProyectoAction!"); // <-- AÑADE ESTO
        if (generalController == null) {
            System.err.println("ERROR CRÍTICO [GestionarProyectoAction]: GeneralController es nulo.");
            return;
        }
        
        System.out.println("DEBUG: Llamando a generalController.solicitarEntrarEnModoProyecto()..."); // <-- AÑADE ESTO
        generalController.solicitarEntrarEnModoProyecto();
    } // --- Fin del método actionPerformed ---

} // --- Fin de la clase GestionarProyectoAction ---