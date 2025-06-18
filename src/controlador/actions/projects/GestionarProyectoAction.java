package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController; // Importar VisorController
import controlador.commands.AppActionCommands;
import servicios.ProjectManager;
// No se necesita importar VisorView

public class GestionarProyectoAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private final ProjectManager projectManagerServiceRef;
    private final VisorController controllerRef; // Referencia al controlador

    public GestionarProyectoAction(
            ProjectManager projectManager,
            VisorController controller, // Recibe el controlador
            String name,
            ImageIcon icon) {
        super(name, icon);
        this.projectManagerServiceRef = Objects.requireNonNull(projectManager, "ProjectManager no puede ser null");
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Gestionar la selección de imágenes del proyecto actual");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_GESTIONAR);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (projectManagerServiceRef == null || controllerRef == null) {
            System.err.println("ERROR CRÍTICO [GestionarProyectoAction]: ProjectManager o VisorController nulos.");
            return;
        }

        // Llamar al método del ProjectManager, pasándole el frame obtenido del controlador
        projectManagerServiceRef.gestionarSeleccionProyecto(controllerRef.getView());
    }
} // --- FIN de la clase GestionarProyectoAction ---