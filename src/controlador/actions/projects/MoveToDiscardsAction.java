package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;

import controlador.GeneralController;
import modelo.VisorModel;

public class MoveToDiscardsAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final GeneralController generalController;
    private final VisorModel model;

    public MoveToDiscardsAction(GeneralController controller, VisorModel model) {
        super("Mover a Descartes");
        this.generalController = Objects.requireNonNull(controller, "GeneralController no puede ser null");
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Mueve la imagen seleccionada a la lista de descartes");
    } // --- Fin del método MoveToDiscardsAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (generalController.getProjectController() != null) {
            generalController.getProjectController().moverSeleccionActualADescartes();
        }
    } // --- Fin del método actionPerformed ---

    @Override
    public void setEnabled(boolean newValue) {
        // Habilitar la acción solo si hay una imagen seleccionada en el modelo del proyecto
        boolean isEnabled = model.getSelectedImageKey() != null && !model.getSelectedImageKey().isEmpty();
        super.setEnabled(isEnabled);
    } // --- Fin del método setEnabled ---

} // --- FIN de la clase MoveToDiscardsAction ---