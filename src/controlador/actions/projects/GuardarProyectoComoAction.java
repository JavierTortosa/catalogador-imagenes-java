package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import controlador.GeneralController;
import controlador.commands.AppActionCommands;

public class GuardarProyectoComoAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final GeneralController generalController;

    public GuardarProyectoComoAction(GeneralController controller, String name, ImageIcon icon) {
        super(name, icon);
        this.generalController = Objects.requireNonNull(controller, "GeneralController no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Guarda el proyecto actual en un nuevo archivo");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_GUARDAR_COMO); // Asegúrate de añadir CMD_PROYECTO_GUARDAR_COMO
    } // --- Fin del método GuardarProyectoComoAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Proyecto Como...");

        // Establecer el directorio inicial
        Path dirInicial = generalController.getProjectController().getProjectManager().getCarpetaBaseProyectos();
        if (dirInicial != null) {
            fileChooser.setCurrentDirectory(dirInicial.toFile());
        }

        // Filtrar para .prj y sugerir un nombre de archivo
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Archivos de Proyecto (*.prj)", "prj");
        fileChooser.setFileFilter(filter);
        fileChooser.setSelectedFile(new java.io.File("MiProyecto.prj"));

        int result = fileChooser.showSaveDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            Path archivoDestino = fileChooser.getSelectedFile().toPath();

            // Asegurarse de que el archivo tenga la extensión .prj
            if (!archivoDestino.toString().toLowerCase().endsWith(".prj")) {
                archivoDestino = archivoDestino.resolveSibling(archivoDestino.getFileName().toString() + ".prj");
            }

            // Verificar si el archivo ya existe
            if (java.nio.file.Files.exists(archivoDestino)) {
                int overwriteConfirm = JOptionPane.showConfirmDialog(
                    null,
                    "El archivo '" + archivoDestino.getFileName() + "' ya existe.\n¿Deseas sobrescribirlo?",
                    "Confirmar Sobrescribir",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                if (overwriteConfirm != JOptionPane.YES_OPTION) {
                    return; // El usuario canceló la sobrescritura
                }
            }

            generalController.getProjectController().solicitarGuardarProyectoComo(archivoDestino);
        }
    } // --- FIN del método actionPerformed ---

} // --- FIN de la clase GuardarProyectoComoAction ---