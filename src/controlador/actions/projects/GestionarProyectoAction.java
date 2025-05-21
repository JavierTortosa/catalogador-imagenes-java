package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.commands.AppActionCommands;
import servicios.ProjectManager; // El servicio
import vista.VisorView;         // Para obtener el frame padre del diálogo

public class GestionarProyectoAction extends AbstractAction { // Ya no hereda de BaseVisorAction

    private static final long serialVersionUID = 1L;

    private ProjectManager projectManagerServiceRef;
    private VisorView viewRef; // Para el JOptionPane

    // Constructor REFACTORIZADO
    public GestionarProyectoAction(
            ProjectManager projectManager,
            VisorView view,
            String name,
            ImageIcon icon) {
        super(name, icon);
        this.projectManagerServiceRef = Objects.requireNonNull(projectManager, "ProjectManager no puede ser null en GestionarProyectoAction");
        this.viewRef = Objects.requireNonNull(view, "VisorView no puede ser null en GestionarProyectoAction");

        putValue(Action.SHORT_DESCRIPTION, "Gestionar la selección de imágenes del proyecto actual");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_GESTIONAR);
        
        // Esta acción generalmente está siempre habilitada.
        // setEnabled(true); // Por defecto las actions están habilitadas
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (projectManagerServiceRef == null || viewRef == null) {
            System.err.println("ERROR CRÍTICO [GestionarProyectoAction]: ProjectManager o VisorView nulos.");
            return;
        }
        // System.out.println("[GestionarProyectoAction actionPerformed] Comando: " + e.getActionCommand());

        // Llamar al método del ProjectManager, pasándole el frame de la vista como padre
        projectManagerServiceRef.gestionarSeleccionProyecto(viewRef.getFrame());
    }
}