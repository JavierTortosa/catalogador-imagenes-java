package controlador.managers.interfaces;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import controlador.ListCoordinator;
import controlador.managers.InfobarStatusManager;
import servicios.zoom.ZoomModeEnum;

/**
 * Interfaz (Contrato) que define las responsabilidades del ZoomManager.
 * Define el "qué" puede hacer el gestor de zoom y paneo, abstrayendo
 * el "cómo" lo hace.
 */
public interface IZoomManager {

    /**
     * Activa o desactiva el permiso para realizar zoom y paneo manual.
     * @param activar true para permitirlo, false para deshabilitarlo.
     */
    void setPermisoManual(boolean activar);

    /**
     * Aplica un modo de zoom predefinido a la imagen.
     * @param modo El modo de zoom a aplicar (ej. FIT_TO_SCREEN).
     */
    void aplicarModoDeZoom(ZoomModeEnum modo);
    
    /**
     * Maneja la interacción de la rueda del ratón, decidiendo si hacer zoom o paneo.
     * @param e El evento de la rueda del ratón.
     */
    void manejarRuedaInteracciona(MouseWheelEvent e);

    /**
     * Realiza una operación de zoom simple basada en la dirección de la rueda del ratón.
     * @param e El evento de la rueda del ratón.
     */
    void aplicarZoomConRueda(MouseWheelEvent e);

    /**
     * Registra el punto inicial para una operación de paneo por arrastre.
     * @param e El evento del ratón al presionar el botón.
     */
    void iniciarPaneo(MouseEvent e);

    /**
     * Continúa una operación de paneo por arrastre, moviendo la imagen.
     * @param e El evento del ratón mientras se arrastra.
     */
    void continuarPaneo(MouseEvent e);

    /**
     * Aplica un desplazamiento (pan) a la imagen.
     * @param deltaX El desplazamiento en el eje X.
     * @param deltaY El desplazamiento en el eje Y.
     */
    void aplicarPan(int deltaX, int deltaY);

    /**
     * Refresca la vista de la imagen principal para que refleje el estado
     * actual del modelo (zoom, pan, imagen).
     */
    void refrescarVistaSincrono();

    // --- MÉTODOS DE INYECCIÓN (SETTERS) ---

    /**
     * Inyecta el gestor de la barra de estado.
     * @param manager La instancia de InfobarStatusManager.
     */
    void setStatusBarManager(InfobarStatusManager manager);

    /**
     * Inyecta el coordinador de las listas.
     * @param listCoordinator La instancia de ListCoordinator.
     */
    void setListCoordinator(ListCoordinator listCoordinator);
    
    /**
     * Aplica un modo de zoom predefinido a la imagen y ejecuta un callback al finalizar.
     * @param modo El modo de zoom a aplicar.
     * @param onComplete Un Runnable que se ejecutará después de que el zoom se haya aplicado. Puede ser null.
     */
    void aplicarModoDeZoom(ZoomModeEnum modo, Runnable onComplete);
    
    /**
     * Inyecta el gestor de la vista.
     * @param viewManager La instancia de IViewManager.
     */
    void setViewManager(IViewManager viewManager);

} // --- FIN de la interfaz IZoomManager ---