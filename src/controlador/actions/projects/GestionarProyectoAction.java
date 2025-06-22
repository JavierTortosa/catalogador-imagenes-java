package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.commands.AppActionCommands;
import controlador.managers.interfaces.IProjectManager;
import controlador.managers.interfaces.IViewManager;

public class GestionarProyectoAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private final IProjectManager projectManager;
    private final IViewManager viewManager; 

    public GestionarProyectoAction(
            IProjectManager projectManager,
            IViewManager viewManager,
            String name,
            ImageIcon icon) 
    {
        super(name, icon);
        this.projectManager = Objects.requireNonNull(projectManager, "IProjectManager no puede ser null");
        this.viewManager = Objects.requireNonNull(viewManager, "IViewManager no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Gestionar la selección de imágenes del proyecto actual");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_GESTIONAR);
    } // --- Fin del método GestionarProyectoAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (projectManager == null || viewManager == null) {
            System.err.println("ERROR CRÍTICO [GestionarProyectoAction]: Dependencias nulas.");
            return;
        }
        viewManager.cambiarAVista("VISTA_PROYECTOS");
    } // --- Fin del método actionPerformed ---

} // --- FIN de la clase GestionarProyectoAction ---