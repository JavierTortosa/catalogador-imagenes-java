package controlador.actions.archivo;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.GeneralController;
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;
import servicios.image.ImageFileUtils;

/**
 * Acción para guardar los cambios actuales.
 */
public class SaveAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
    private final GeneralController generalController;
    private final VisorModel model;


    /**
     * Constructor para SaveAction.
     *
     * @param name              El nombre de la acción.
     * @param icon              El icono de la acción.
     * @param generalController El controlador general.
     * @param model             El modelo del visor.
     */
    public SaveAction(String name, ImageIcon icon, GeneralController generalController, VisorModel model) {
        super(name, icon);
        this.generalController = Objects.requireNonNull(generalController);
        this.model = Objects.requireNonNull(model);
        putValue(Action.SHORT_DESCRIPTION, "Guardar los cambios actuales");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ARCHIVO_GUARDAR);
    } // --- Fin del método SaveAction ---


    /**
     * Ejecuta la acción de guardar los cambios actuales.
     *
     * @param e El evento de acción.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (model.getCurrentWorkMode() == WorkMode.PROYECTO) {
            generalController.handleSaveProject();
        } else if (model.getCurrentImage() != null) {
            if (model.isImagenModificada()) {
                ImageFileUtils.guardarImagenComo(model.getCurrentImage(), null);
                model.setImagenModificada(false);
            } else {
                javax.swing.JOptionPane.showMessageDialog(null,
                    "No hay cambios que guardar en la imagen actual.",
                    "Guardar", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            }
        }
    } // --- Fin del método actionPerformed ---


    /**
     * Actualiza el estado de habilitación de la acción basándose en el modo de trabajo y si hay una imagen cargada.
     *
     * @param model El modelo del visor.
     */
    @Override
    public void updateEnabledState(VisorModel model) {
        boolean projectMode = model.getCurrentWorkMode() == WorkMode.PROYECTO;
        boolean hasImage = model.getCurrentImage() != null;
        boolean dataMode = model.getCurrentWorkMode() == WorkMode.DATOS;
        setEnabled(!dataMode && (projectMode || hasImage));
    } // --- Fin del método updateEnabledState ---

} // --- Fin de la clase SaveAction ---
