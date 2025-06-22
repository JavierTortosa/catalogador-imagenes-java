package controlador.managers.interfaces;

/**
 * Interfaz (Contrato) que define las responsabilidades del EditionManager.
 * Define las operaciones de edición de imágenes que se pueden realizar.
 */
public interface IEditionManager {

    /**
     * Aplica un volteo horizontal a la imagen actual.
     */
    void aplicarVolteoHorizontal();

    /**
     * Aplica un volteo vertical a la imagen actual.
     */
    void aplicarVolteoVertical();

    /**
     * Aplica una rotación de 90 grados a la izquierda a la imagen actual.
     */
    void aplicarRotarIzquierda();

    /**
     * Aplica una rotación de 90 grados a la derecha a la imagen actual.
     */
    void aplicarRotarDerecha();

    // --- INICIO DE LA MODIFICACIÓN ---
    /**
     * Inicia la operación de recorte de la imagen actual.
     * (Funcionalidad pendiente de implementación).
     */
    void aplicarRecorte();
    // --- FIN DE LA MODIFICACIÓN ---

} // --- FIN de la interfaz IEditionManager ---