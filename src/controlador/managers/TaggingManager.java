package controlador.managers;

import java.nio.file.Path;

/**
 * Gestor centralizado que recuerda la última imagen activa en el modo VISUALIZADOR,
 * permitiendo que otros modos (especialmente DATOS) puedan recuperar contexto
 * al cambiar de modo de trabajo.
 */
public class TaggingManager {

    private Path lastActiveImagePath;
    private String lastActiveImageKey;

    /**
     * Registra la imagen que se está visualizando actualmente.
     * @param path Ruta absoluta del archivo de imagen.
     * @param key Clave identificadora de la imagen en el contexto actual.
     */
    public void setLastActiveImage(Path path, String key) {
        this.lastActiveImagePath = path;
        this.lastActiveImageKey = key;
    } // ---FIN de metodo [setLastActiveImage]---

    /**
     * @return La ruta absoluta de la última imagen activa, o null si no hay ninguna.
     */
    public Path getLastActiveImagePath() {
        return lastActiveImagePath;
    } // ---FIN de metodo [getLastActiveImagePath]---

    /**
     * @return La clave de la última imagen activa, o null si no hay ninguna.
     */
    public String getLastActiveImageKey() {
        return lastActiveImageKey;
    } // ---FIN de metodo [getLastActiveImageKey]---

    /**
     * Limpia el registro de la última imagen activa.
     */
    public void clearLastActiveImage() {
        this.lastActiveImagePath = null;
        this.lastActiveImageKey = null;
    } // ---FIN de metodo [clearLastActiveImage]---

} // --- FIN de clase TaggingManager ---
