package controlador.actions.archivo;

import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;

/**
 * Acción para abrir la imagen actual con un programa externo mediante un selector de archivos.
 */
public class AbrirConAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
    private final VisorModel model;


    /**
     * Constructor para AbrirConAction.
     *
     * @param name  El nombre de la acción.
     * @param icon  El icono de la acción.
     * @param model El modelo del visor.
     */
    public AbrirConAction(String name, ImageIcon icon, VisorModel model) {
        super(name, icon);
        this.model = Objects.requireNonNull(model);
        putValue(Action.SHORT_DESCRIPTION, "Abrir la imagen actual con un programa externo");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ARCHIVO_ABRIR_CON);
    } // --- Fin del método AbrirConAction ---


    /**
     * Ejecuta la acción de abrir la imagen con el programa seleccionado.
     *
     * @param e El evento de acción.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        Path rutaPath = model.getRutaCompleta(model.getSelectedImageKey());
        if (rutaPath == null) return;

        File imageFile = rutaPath.toFile();
        if (!imageFile.exists()) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar programa para abrir la imagen");
        chooser.setFileFilter(new FileNameExtensionFilter("Ejecutables (*.exe)", "exe"));

        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File program = chooser.getSelectedFile();
            try {
                Runtime.getRuntime().exec(new String[] { program.getAbsolutePath(), imageFile.getAbsolutePath() });
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(null,
                    "Error al ejecutar el programa:\n" + ex.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    } // --- Fin del método actionPerformed ---


    /**
     * Actualiza el estado de habilitación de la acción basándose en la selección y el modo de trabajo.
     *
     * @param model El modelo del visor.
     */
    @Override
    public void updateEnabledState(VisorModel model) {
        Path rutaPath = model.getRutaCompleta(model.getSelectedImageKey());
        setEnabled(rutaPath != null && model.getCurrentWorkMode() != WorkMode.DATOS);
    } // --- Fin del método updateEnabledState ---

} // --- Fin de la clase AbrirConAction ---
