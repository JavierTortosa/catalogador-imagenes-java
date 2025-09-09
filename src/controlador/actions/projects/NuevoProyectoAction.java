package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import controlador.GeneralController;
import controlador.ProjectController; 
import controlador.commands.AppActionCommands;

public class NuevoProyectoAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final GeneralController generalController;

    private final ProjectController projectController; 
    
    public NuevoProyectoAction(GeneralController generalController, ProjectController projectController, String name, ImageIcon icon) { 
        super(name, icon);
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null");
        this.projectController = Objects.requireNonNull(projectController, "ProjectController no puede ser null"); 
        putValue(Action.SHORT_DESCRIPTION, "Crea un nuevo proyecto vacío");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_NUEVO); // Asegúrate de añadir CMD_PROYECTO_NUEVO a AppActionCommands
    } // --- Fin del método NuevoProyectoAction (constructor) ---
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // Opcional: Preguntar al usuario si quiere guardar los cambios del proyecto actual si los hubiera.
        // Por ahora, lo hacemos directamente para simplificar.
        
        int confirm = JOptionPane.showConfirmDialog(
            null, // Usamos null para que el diálogo aparezca centrado
            "¿Estás seguro de que quieres crear un nuevo proyecto?\nCualquier cambio no guardado en el proyecto actual se perderá.",
            "Confirmar Nuevo Proyecto",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
        	
            // Antes de llamar a solicitarNuevoProyecto, creamos una nueva instancia del project.
            // Como projectManager crea una copia de seguridad de cada proyecto al salir, al volver a entrar se recuperará esa copia.
            projectController.solicitarNuevoProyecto();
        }
    } // --- FIN del método actionPerformed ---

} // --- FIN de la clase NuevoProyectoAction ---

