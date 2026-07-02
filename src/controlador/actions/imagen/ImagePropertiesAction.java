package controlador.actions.imagen;

import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.GeneralController;
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;
import vista.dialogos.ImagePropertiesDialog;

/**
 * Acción para mostrar las propiedades de la imagen seleccionada.
 */
public class ImagePropertiesAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
    private final GeneralController generalController;
    private final VisorModel model;


    /**
     * Constructor para ImagePropertiesAction.
     *
     * @param name              El nombre de la acción.
     * @param icon              El icono de la acción.
     * @param generalController El controlador general.
     * @param model             El modelo del visor.
     */
    public ImagePropertiesAction(String name, ImageIcon icon, GeneralController generalController, VisorModel model) {
        super(name, icon);
        this.generalController = Objects.requireNonNull(generalController);
        this.model = Objects.requireNonNull(model);
        putValue(Action.SHORT_DESCRIPTION, "Mostrar propiedades de la imagen seleccionada");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_IMAGEN_PROPIEDADES);
    } // --- Fin del método ImagePropertiesAction ---


    /**
     * Ejecuta la acción de mostrar el diálogo de propiedades de la imagen.
     *
     * @param e El evento de acción.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        String key = model.getSelectedImageKey();
        if (key == null) return;

        Path ruta = model.getRutaCompleta(key);
        if (ruta == null) return;

        List<String> tags = new ArrayList<>();
        if (model.getCurrentWorkMode() == WorkMode.DATOS
                && generalController.getDataController() != null
                && generalController.getDataController().getDataManager() != null) {
            try {
                var tagList = generalController.getDataController().getDataManager().getTagsForImage(ruta);
                if (tagList != null) {
                    for (var tag : tagList) {
                        tags.add(tag.getNombre());
                    }
                }
            } catch (Exception ignored) {
            }
        }

        java.awt.Frame owner = generalController.getVisorController() != null
                ? generalController.getVisorController().getView()
                : null;

        ImagePropertiesDialog dialog = new ImagePropertiesDialog(owner, model, tags.isEmpty() ? null : tags);
        dialog.setVisible(true);
    } // --- Fin del método actionPerformed ---


    /**
     * Actualiza el estado de habilitación de la acción basándose en si hay una imagen seleccionada.
     *
     * @param model El modelo del visor.
     */
    @Override
    public void updateEnabledState(VisorModel model) {
        String key = model.getSelectedImageKey();
        Path ruta = (key != null) ? model.getRutaCompleta(key) : null;
        setEnabled(ruta != null);
    } // --- Fin del método updateEnabledState ---

} // --- Fin de la clase ImagePropertiesAction ---
