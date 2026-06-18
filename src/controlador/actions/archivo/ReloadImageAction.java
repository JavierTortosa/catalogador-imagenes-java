package controlador.actions.archivo;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Objects;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.GeneralController;
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;

/**
 * Acción para recargar la imagen actual desde el disco.
 */
public class ReloadImageAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
    private final GeneralController generalController;
    private final VisorModel model;


    /**
     * Constructor para ReloadImageAction.
     *
     * @param name              El nombre de la acción.
     * @param icon              El icono de la acción.
     * @param generalController El controlador general.
     * @param model             El modelo del visor.
     */
    public ReloadImageAction(String name, ImageIcon icon, GeneralController generalController, VisorModel model) {
        super(name, icon);
        this.generalController = Objects.requireNonNull(generalController);
        this.model = Objects.requireNonNull(model);
        putValue(Action.SHORT_DESCRIPTION, "Recargar la imagen actual desde el disco");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ARCHIVO_RECARGAR_IMAGEN);
    } // --- Fin del método ReloadImageAction ---


    /**
     * Ejecuta la acción de recargar la imagen actual.
     *
     * @param e El evento de acción.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        Path rutaPath = model.getRutaCompleta(model.getSelectedImageKey());
        if (rutaPath == null) return;

        new Thread(() -> {
            try {
                Path path = rutaPath;
                BufferedImage img = ImageIO.read(path.toFile());
                if (img != null) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        generalController.getVisorController()
                            .actualizarImagenPrincipalPorPath(path, model.getSelectedImageKey());
                    });
                }
            } catch (Exception ex) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    javax.swing.JOptionPane.showMessageDialog(null,
                        "Error al recargar la imagen:\n" + ex.getMessage(),
                        "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    } // --- Fin del método actionPerformed ---


    /**
     * Actualiza el estado de habilitación de la acción basándose en la ruta de la imagen y el modo de trabajo.
     *
     * @param model El modelo del visor.
     */
    @Override
    public void updateEnabledState(VisorModel model) {
        Path rutaPath = model.getRutaCompleta(model.getSelectedImageKey());
        setEnabled(rutaPath != null && model.getCurrentWorkMode() != WorkMode.DATOS);
    } // --- Fin del método updateEnabledState ---

} // --- Fin de la clase ReloadImageAction ---
