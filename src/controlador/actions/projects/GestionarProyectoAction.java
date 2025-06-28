package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.ProjectController;
import controlador.commands.AppActionCommands;
import modelo.VisorModel;

public class GestionarProyectoAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    
    private final ProjectController projectControllerRef; 

    public GestionarProyectoAction(
    		ProjectController controller,
            String name,
            ImageIcon icon) 
    {
        super(name, icon);
        this.projectControllerRef = Objects.requireNonNull(controller, "ProjectController no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Ver/Gestionar la selección de imágenes del proyecto");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_GESTIONAR);
    } // --- Fin del método GestionarProyectoAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (projectControllerRef == null) {
            System.err.println("ERROR CRÍTICO [GestionarProyectoAction]: ProjectController es nulo.");
            return;
        }
        // Delega la lógica al controlador de proyecto.
//        projectControllerRef.mostrarVistaDeProyecto();
        projectControllerRef.getController().cambiarModoDeTrabajo(VisorModel.WorkMode.PROYECTO);
    } // --- Fin del método actionPerformed ---

} // --- FIN de la clase GestionarProyectoAction ---

//package controlador.actions.projects;
//
//import java.awt.event.ActionEvent;
//import java.util.Objects;
//
//import javax.swing.AbstractAction;
//import javax.swing.Action;
//import javax.swing.ImageIcon;
//
//import controlador.ProjectController;
//import controlador.VisorController; // <--- MODIFICADO: Ahora depende del Controller
//import controlador.commands.AppActionCommands;
//// <--- ELIMINADO: Ya no necesita IProjectManager ni IViewManager ---
//
//public class GestionarProyectoAction extends AbstractAction {
//
//    private static final long serialVersionUID = 1L;
//    
//
//    // <--- MODIFICADO: La única dependencia es el VisorController ---
//    private final ProjectController projectControllerRef; 
//
//    public GestionarProyectoAction(
//    		ProjectController  controller, // <--- MODIFICADO: Recibe el Controller
//            String name,
//            ImageIcon icon) 
//    {
//        super(name, icon);
//        this.projectControllerRef  = Objects.requireNonNull(controller, "ProjectController no puede ser null");
//
//        // <--- MODIFICADO: Un tooltip más genérico para "ver/gestionar" ---
//        putValue(Action.SHORT_DESCRIPTION, "Ver/Gestionar la selección de imágenes del proyecto");
//        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_GESTIONAR);
//    } // --- Fin del método GestionarProyectoAction (constructor) ---
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        if (projectControllerRef == null) {
//            System.err.println("ERROR CRÍTICO [GestionarProyectoAction]: ProjectController es nulo.");
//            return;
//        }
//        // Delega la lógica al controlador de proyecto.
//        projectControllerRef.mostrarVistaDeProyecto();
//    } // --- Fin del método actionPerformed ---
//
//
//
//} // --- FIN de la clase GestionarProyectoAction ---