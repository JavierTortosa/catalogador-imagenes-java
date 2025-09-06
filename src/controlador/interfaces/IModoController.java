package controlador.interfaces;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Define el contrato para los controladores de modo (como VisorController, ProjectController).
 * Estos controladores gestionan la interacción del usuario dentro de un modo de trabajo específico.
 * El GeneralController delegará las acciones a la implementación activa de esta interfaz.
 */
public interface IModoController {

    /**
     * Navega a la siguiente imagen en la lista del modo actual.
     */
    void navegarSiguiente();

    /**
     * Navega a la imagen anterior en la lista del modo actual.
     */
    void navegarAnterior();

    /**
     * Navega a la primera imagen de la lista del modo actual.
     */
    void navegarPrimero();

    /**
     * Navega a la última imagen de la lista del modo actual.
     */
    void navegarUltimo();

    /**
     * Navega a la página o bloque anterior de imágenes.
     */
    void navegarBloqueAnterior();

    /**
     * Navega a la página o bloque siguiente de imágenes.
     */
    void navegarBloqueSiguiente();

    /**
     * Procesa un evento de la rueda del ratón para aplicar zoom.
     * @param e El MouseWheelEvent original.
     */
    void aplicarZoomConRueda(MouseWheelEvent e);

    /**
     * Procesa una acción de paneo incremental.
     * @param deltaX El desplazamiento horizontal.
     * @param deltaY El desplazamiento vertical.
     */
    void aplicarPan(int deltaX, int deltaY);

    /**
     * Registra el punto de inicio de una operación de paneo con el ratón.
     * @param e El MouseEvent original (normalmente mousePressed).
     */
    void iniciarPaneo(MouseEvent e);

    /**
     * Continúa una operación de paneo mientras el ratón se arrastra.
     * @param e El MouseEvent original (normalmente mouseDragged).
     */
    void continuarPaneo(MouseEvent e);
    
    /**
     * Solicita al controlador de modo que ejecute su lógica de refresco principal.
     */
    void solicitarRefresco();
    
} // --- FIN de la interfaz IModoController ---