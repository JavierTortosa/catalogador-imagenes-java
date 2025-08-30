package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import controlador.GeneralController;
import controlador.commands.AppActionCommands;

public class AbrirProyectoAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final GeneralController generalController;

    public AbrirProyectoAction(GeneralController controller, String name, ImageIcon icon) {
        super(name, icon);
        this.generalController = Objects.requireNonNull(controller, "GeneralController no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Abre un proyecto existente desde un archivo");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_ABRIR); // Asegúrate de añadir CMD_PROYECTO_ABRIR
    } // --- Fin del método AbrirProyectoAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Abrir Proyecto");
        
        // Establecer el directorio inicial en la carpeta de proyectos
        Path dirInicial = generalController.getProjectController().getProjectManager().getCarpetaBaseProyectos();
        if (dirInicial != null) {
            fileChooser.setCurrentDirectory(dirInicial.toFile());
        }

        // Filtrar para mostrar solo archivos .prj
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Archivos de Proyecto (*.prj)", "prj");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            Path archivoSeleccionado = fileChooser.getSelectedFile().toPath();
            generalController.getProjectController().solicitarAbrirProyecto(archivoSeleccionado);
        }
    } // --- FIN del método actionPerformed ---

} // --- FIN de la clase AbrirProyectoAction ---