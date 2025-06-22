package controlador.managers.interfaces;

import java.awt.event.ActionEvent;
import java.util.List;

import controlador.interfaces.ContextSensitiveAction;

/**
 * Interfaz (Contrato) que define las responsabilidades del ListCoordinator.
 * Define el "qué" puede hacer el coordinador de listas, abstrayendo el "cómo" lo hace.
 */
public interface IListCoordinator {

    void seleccionarImagenPorIndice(int indiceDeseado);
    void seleccionarAnterior();
    void seleccionarPrimero();
    void seleccionarUltimo();
    void seleccionarSiguiente();
    void seleccionarBloqueAnterior();
    void seleccionarBloqueSiguiente();
    void asegurarVisibilidadAmbasListasSiVisibles(int indice);
    void seleccionarIndiceYActualizarUICompleta(int indice);
    void reiniciarYSeleccionarIndice(int indiceDeseado);
    void seleccionarSiguienteOAnterior(int wheelRotation);
    void actualizarModeloYVistaMiniaturas(int indiceSeleccionadoPrincipal);
    boolean isSincronizandoUI();
    void navegarAIndice(int index);
    void logActionInfo(ActionEvent e);
    int getIndiceOficialSeleccionado();
    void setContextSensitiveActions(List<ContextSensitiveAction> actions);
    void forzarActualizacionEstadoAcciones();
    void setSincronizandoUI(boolean sincronizando);
    

} // --- FIN de la interfaz IListCoordinator ---