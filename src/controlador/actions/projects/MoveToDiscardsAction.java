package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JList;

import controlador.GeneralController;
import controlador.interfaces.ContextSensitiveAction;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;

public class MoveToDiscardsAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
    private final GeneralController generalController;
    private final ComponentRegistry registry;

    public MoveToDiscardsAction(GeneralController controller, ComponentRegistry registry) {
        super("Mover a Descartes");
        this.generalController = Objects.requireNonNull(controller, "GeneralController no puede ser null");
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null");
        putValue(Action.SHORT_DESCRIPTION,
                "Mueve las imágenes seleccionadas a la lista de descartes (Ctrl+clic / Mayús+clic para varias)");
    } // --- Fin del método MoveToDiscardsAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (generalController.getProjectController() != null) {
            generalController.getProjectController().moverSeleccionActualADescartes();
        }
    } // --- Fin del método actionPerformed ---

    @Override
    public void updateEnabledState(VisorModel modelo) {
        if (modelo == null || !modelo.isEnModoProyecto()) {
            setEnabled(false);
            return;
        }
        JList<String> listaSeleccion = registry.get("list.proyecto.nombres");
        boolean haySeleccion = listaSeleccion != null && listaSeleccion.getSelectedIndices().length > 0;
        setEnabled(haySeleccion);
    } // --- Fin del método updateEnabledState ---

    @Override
    public void setEnabled(boolean newValue) {
        JList<String> listaSeleccion = registry.get("list.proyecto.nombres");
        boolean isEnabled = listaSeleccion != null && listaSeleccion.getSelectedIndices().length > 0;
        super.setEnabled(isEnabled);
    } // --- Fin del método setEnabled ---

} // --- FIN de la clase MoveToDiscardsAction ---
