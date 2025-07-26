package controlador.managers.interfaces;

import java.util.List;
import controlador.interfaces.ContextSensitiveAction;

public interface ICarouselListCoordinator {

    void seleccionarSiguiente();
    void seleccionarAnterior();
    void seleccionarPrimero();
    void seleccionarUltimo();

    /**
     * Fuerza la actualización del estado 'enabled' de todas las acciones
     * sensibles al contexto que gestiona este coordinador.
     */
    void forceUpdateActionStates();

    /**
     * Inyecta la lista de acciones sensibles al contexto que este
     * coordinador debe gestionar.
     * @param actions La lista de acciones.
     */
    void setContextSensitiveActions(List<ContextSensitiveAction> actions);
    
} // --- FIN de la interfaz ICarouselListCoordinator ---