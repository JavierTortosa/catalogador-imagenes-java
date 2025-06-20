package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController; // Importar VisorController
import controlador.commands.AppActionCommands;
import controlador.managers.ViewManager;
import servicios.ProjectManager;
// No se necesita importar VisorView

public class GestionarProyectoAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private final ProjectManager projectManagerServiceRef;
    private final ViewManager viewManager; 

    public GestionarProyectoAction(
            ProjectManager projectManager,
            ViewManager viewManager, // Recibe el controlador
            String name,
            ImageIcon icon) 
    {
    	
        super(name, icon);
        this.projectManagerServiceRef = Objects.requireNonNull(projectManager, "ProjectManager no puede ser null");
        this.viewManager = Objects.requireNonNull(viewManager, "viewManager no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Gestionar la selección de imágenes del proyecto actual");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_GESTIONAR);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (projectManagerServiceRef == null || viewManager == null) {
            System.err.println("ERROR CRÍTICO [GestionarProyectoAction]: ProjectManager o VisorController nulos.");
            return;
        }

        // Llamar al método del ProjectManager, pasándole el frame obtenido del controlador
        viewManager.cambiarAVista("VISTA_PROYECTOS");
    }
} // --- FIN de la clase GestionarProyectoAction ---