package controlador.managers.interfaces;

import java.util.List;
import controlador.interfaces.ContextSensitiveAction;

/**
 * Interfaz (Contrato) que define las responsabilidades públicas del ListCoordinator.
 */
public interface IListCoordinator {

    // --- Métodos de Navegación Principales ---
    void seleccionarSiguiente();
    void seleccionarAnterior();
    void seleccionarPrimero();
    void seleccionarUltimo();
    void seleccionarBloqueSiguiente();
    void seleccionarBloqueAnterior();
    void seleccionarSiguienteOAnterior(int wheelRotation);
    void navegarAIndice(int index);
    

    /**
     * Punto de entrada principal para establecer una selección por su índice.
     * @param indiceDeseado El índice a seleccionar.
     */
    void seleccionarImagenPorIndice(int indiceDeseado);

    /**
     * Reinicia el estado del coordinador y selecciona un nuevo índice.
     * Ideal para usar después de recargar la lista de imágenes.
     * @param indiceDeseado El nuevo índice a seleccionar.
     */
    void reiniciarYSeleccionarIndice(int indiceDeseado);

    /**
     * Fuerza una reevaluación del estado 'enabled' de las acciones de navegación y contextuales.
     */
    void forzarActualizacionEstadoAcciones();

    // --- Métodos de Estado y Configuración ---
    boolean isSincronizandoUI();
    void setSincronizandoUI(boolean sincronizando);
    void setContextSensitiveActions(List<ContextSensitiveAction> actions);

} // --- Fin de la interfaz IListCoordinator ---


