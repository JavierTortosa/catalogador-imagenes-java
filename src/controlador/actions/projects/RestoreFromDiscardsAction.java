package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JList;

import controlador.GeneralController;
import controlador.utils.ComponentRegistry;

public class RestoreFromDiscardsAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final GeneralController generalController;
    private final ComponentRegistry registry;

    public RestoreFromDiscardsAction(GeneralController controller, ComponentRegistry registry) {
        super("Restaurar a Selección");
        this.generalController = Objects.requireNonNull(controller, "GeneralController no puede ser null");
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Mueve la imagen de vuelta a la lista de selección actual");
    } // --- Fin del método RestoreFromDiscardsAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (generalController.getProjectController() != null) {
            generalController.getProjectController().restaurarDesdeDescartes();
        }
    } // --- Fin del método actionPerformed ---

    @Override
    public void setEnabled(boolean newValue) {
        // Habilitar la acción solo si hay una imagen seleccionada en la lista de descartes
        JList<String> listaDescartes = registry.get("list.proyecto.descartes");
        boolean isEnabled = listaDescartes != null && listaDescartes.getSelectedIndex() != -1;
        super.setEnabled(isEnabled);
    } // --- Fin del método setEnabled ---

} // --- FIN de la clase RestoreFromDiscardsAction ---