package vista.config;

/**
 * Especifica el ámbito de un icono, determinando en qué carpeta buscarlo.
 */
public enum IconScope {
    /**
     * El icono depende del tema actual.
     * Se buscará primero en /iconos/[tema_actual]/ y si no, en /iconos/comunes/.
     */
    THEMED,

    /**
     * El icono es universal y no depende del tema.
     * Se buscará únicamente en /iconos/comunes/.
     */
    COMMON
    
} // --- FIN de la clase enum IconScope ---