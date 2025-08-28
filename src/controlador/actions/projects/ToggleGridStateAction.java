package controlador.actions.projects;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JList;

import controlador.utils.ComponentRegistry;
import modelo.VisorModel;

/**
 * Acción para alternar la visibilidad de los colores de estado en el grid del modo proyecto.
 */
public class ToggleGridStateAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private VisorModel model;
    private ComponentRegistry registry;

    public ToggleGridStateAction(String name, ImageIcon icon, VisorModel model, ComponentRegistry registry) {
        super(name, icon);
        this.model = model;
        this.registry = registry;
        
        // Sincronizar el estado inicial del botón (seleccionado/no seleccionado) con el modelo.
        this.putValue(Action.SELECTED_KEY, model.isGridMuestraEstado());
    } // ---FIN de metodo---

    @Override
    public void actionPerformed(ActionEvent e) {
        // Invertir el estado en el modelo.
        boolean nuevoEstado = !model.isGridMuestraEstado();
        model.setGridMuestraEstado(nuevoEstado);
        
        // Actualizar el estado visual del botón de toggle.
        putValue(Action.SELECTED_KEY, nuevoEstado);

        // Forzar un repintado del grid del proyecto para que los cambios se apliquen.
        JList<?> gridList = registry.get("list.grid.proyecto");
        if (gridList != null) {
            gridList.repaint();
        }
    } // ---FIN de metodo---

} // --- FIN de clase ToggleGridStateAction---