package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.GeneralController;
import controlador.commands.AppActionCommands;
import controlador.managers.interfaces.IProjectManager;
import modelo.VisorModel;

public class EliminarProyectoAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(EliminarProyectoAction.class);
    
    private final GeneralController generalController;

    public EliminarProyectoAction(GeneralController controller, String name, ImageIcon icon) {
        super(name, icon);
        this.generalController = Objects.requireNonNull(controller, "GeneralController no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Elimina un archivo de proyecto del disco");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_ELIMINAR); // Añadir a AppActionCommands
    } // --- Fin del método EliminarProyectoAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar Proyecto a Eliminar");
        
        IProjectManager pm = generalController.getProjectController().getProjectManager();
        Path dirInicial = pm.getCarpetaBaseProyectos();
        if (dirInicial != null) {
            fileChooser.setCurrentDirectory(dirInicial.toFile());
        }

        FileNameExtensionFilter filter = new FileNameExtensionFilter("Archivos de Proyecto (*.prj)", "prj");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showDialog(null, "Eliminar Seleccionado");

        if (result == JFileChooser.APPROVE_OPTION) {
            Path archivoAEliminar = fileChooser.getSelectedFile().toPath();
            
            // Doble confirmación
            int confirm = JOptionPane.showConfirmDialog(
                null,
                "¿Estás ABSOLUTAMENTE SEGURO de que quieres eliminar el proyecto '" + archivoAEliminar.getFileName() + "'?\n" +
                "Esta acción es irreversible y borrará el archivo del disco.",
                "Confirmar Eliminación Permanente",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    Files.delete(archivoAEliminar);
                    logger.info("Proyecto eliminado exitosamente: {}", archivoAEliminar);
                    JOptionPane.showMessageDialog(null, "El proyecto ha sido eliminado.", "Eliminación Completada", JOptionPane.INFORMATION_MESSAGE);

                    // Comprobar si el proyecto eliminado era el activo
                    Path proyectoActivo = pm.getArchivoProyectoActivo();
                    if (Objects.equals(proyectoActivo, archivoAEliminar)) {
                        logger.info("El proyecto eliminado era el activo. Volviendo al modo Visualizador con un proyecto temporal.");
                        // En lugar de llamar a solicitarNuevoProyecto (que ahora cambia de modo),
                        // simplemente le decimos al manager que cree el nuevo estado temporal...
                        pm.nuevoProyecto(); 
                        // ...y luego le pedimos al GeneralController que nos devuelva al modo Visualizador.
                        generalController.cambiarModoDeTrabajo(VisorModel.WorkMode.VISUALIZADOR);
                    }
                    
                } catch (IOException ex) {
                    logger.error("Error al intentar eliminar el archivo de proyecto: " + archivoAEliminar, ex);
                    JOptionPane.showMessageDialog(null, "No se pudo eliminar el archivo del proyecto.\nError: " + ex.getMessage(), "Error de Eliminación", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    } // --- FIN del método actionPerformed ---

} // --- FIN de la clase EliminarProyectoAction ---