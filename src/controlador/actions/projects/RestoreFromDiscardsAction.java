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

public class RestoreFromDiscardsAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
    private final GeneralController generalController;
    private final ComponentRegistry registry;

    public RestoreFromDiscardsAction(GeneralController controller, ComponentRegistry registry) {
        super("Restaurar a Selección");
        this.generalController = Objects.requireNonNull(controller, "GeneralController no puede ser null");
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null");
        putValue(Action.SHORT_DESCRIPTION,
                "Devuelve las imágenes seleccionadas de descartes a la selección (Ctrl+clic / Mayús+clic para varias)");
    } // --- Fin del método RestoreFromDiscardsAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (generalController.getProjectController() != null) {
            generalController.getProjectController().restaurarDesdeDescartes();
        }
    } // --- Fin del método actionPerformed ---

    @Override
    public void updateEnabledState(VisorModel modelo) {
        if (modelo == null || !modelo.isEnModoProyecto()) {
            setEnabled(false);
            return;
        }
        JList<String> listaDescartes = registry.get("list.proyecto.descartes");
        boolean haySeleccion = listaDescartes != null && listaDescartes.getSelectedIndices().length > 0;
        setEnabled(haySeleccion);
    } // --- Fin del método updateEnabledState ---

    @Override
    public void setEnabled(boolean newValue) {
        JList<String> listaDescartes = registry.get("list.proyecto.descartes");
        boolean isEnabled = listaDescartes != null && listaDescartes.getSelectedIndices().length > 0;
        super.setEnabled(isEnabled);
    } // --- Fin del método setEnabled ---

} // --- FIN de la clase RestoreFromDiscardsAction ---
