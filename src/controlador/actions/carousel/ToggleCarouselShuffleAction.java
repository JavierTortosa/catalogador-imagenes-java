package controlador.actions.carousel;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;

public class ToggleCarouselShuffleAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
    private final VisorModel model;

    public ToggleCarouselShuffleAction(VisorModel model) {
        this.model = model;
        putValue(Action.SELECTED_KEY, model.isCarouselShuffleEnabled());
        updateEnabledState(model); // Llama para establecer el estado inicial
    } // --- Fin del constructor ---

    @Override
    public void actionPerformed(ActionEvent e) {
        // Añadimos una guarda de seguridad: no hacer nada si está deshabilitado.
        if (!isEnabled()) {
            return;
        }
        
        boolean newState = !model.isCarouselShuffleEnabled();
        model.setCarouselShuffleEnabled(newState);
        putValue(Action.SELECTED_KEY, newState);
    } // --- Fin del método actionPerformed ---

    @Override
    public void updateEnabledState(VisorModel currentModel) {
        // La acción de "aleatorio" se deshabilita si la sincronización está activa.
        // Un carrusel no puede ser un "espejo" y "aleatorio" al mismo tiempo.
        boolean isSyncActive = currentModel.isSyncVisualizadorCarrusel();
        setEnabled(!isSyncActive);
        
        // Adicionalmente, si se desactiva por la sincronización, nos aseguramos
        // de que el modo aleatorio en el modelo también se desactive.
        if (isSyncActive && currentModel.isCarouselShuffleEnabled()) {
            currentModel.setCarouselShuffleEnabled(false);
            putValue(Action.SELECTED_KEY, false);
        }
    } // --- Fin del método updateEnabledState ---
    
} // --- FIN de la clase ToggleCarouselShuffleAction ---