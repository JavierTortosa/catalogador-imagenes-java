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


//package controlador.managers.interfaces;
//
//import java.awt.event.ActionEvent;
//import java.util.List;
//
//import controlador.interfaces.ContextSensitiveAction;
//
///**
// * Interfaz (Contrato) que define las responsabilidades del ListCoordinator.
// * Define el "qué" puede hacer el coordinador de listas, abstrayendo el "cómo" lo hace.
// */
//public interface IListCoordinator {
//
//    void seleccionarImagenPorIndice(int indiceDeseado);
//    void seleccionarAnterior();
//    void seleccionarPrimero();
//    void seleccionarUltimo();
//    void seleccionarSiguiente();
//    void seleccionarBloqueAnterior();
//    void seleccionarBloqueSiguiente();
//    void asegurarVisibilidadAmbasListasSiVisibles(int indice);
//    void seleccionarIndiceYActualizarUICompleta(int indice);
//    void reiniciarYSeleccionarIndice(int indiceDeseado);
//    void seleccionarSiguienteOAnterior(int wheelRotation);
//    void actualizarModeloYVistaMiniaturas(int indiceSeleccionadoPrincipal);
//    boolean isSincronizandoUI();
//    void navegarAIndice(int index);
//    void logActionInfo(ActionEvent e);
//    int getIndiceOficialSeleccionado();
//    void setContextSensitiveActions(List<ContextSensitiveAction> actions);
//    void forzarActualizacionEstadoAcciones();
//    void setSincronizandoUI(boolean sincronizando);
//    
//
//} // --- FIN de la interfaz IListCoordinator ---