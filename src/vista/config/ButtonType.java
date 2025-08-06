package vista.config;

/**
 * Define el tipo de botón a crear en una barra de herramientas,
 * permitiendo diferenciar entre botones estándar y componentes más complejos.
 */
public enum ButtonType {
    /** 
     * Un botón estándar que realiza una acción puntual.
     * Se creará como una instancia de javax.swing.JButton.
     */
    NORMAL,
    
    /** 
     * Un botón que representa un estado (presionado/no presionado) y que
     * a menudo forma parte de un grupo de selección exclusiva.
     * Se creará como una instancia de javax.swing.JToggleButton.
     */
    TOGGLE,
    
    /**
     * Un componente de control direccional personalizado (D-Pad) con un layout en forma de cruz.
     * Se creará como una instancia de vista.components.DPadComponent usando la fábrica createCrossLayout.
     */
    DPAD_CRUZ,
    
    /**
     * Un componente de control direccional personalizado (D-Pad) con un layout en forma de cuadrícula (grid).
     * Se creará como una instancia de vista.components.DPadComponent usando la fábrica createGridLayout.
     */
    DPAD_GRID,

    /**
     * Un botón estándar que tendrá un icono base al que se le aplicará un color
     * específico del tema. Se usará para los selectores de color de fondo.
     * Se creará como una instancia de javax.swing.JButton y su icono será
     * una instancia de vista.components.icons.ColorOverlayIcon.
     */
    COLOR_OVERLAY_ICON_BUTTON,

    /**
     * Un botón estándar que tendrá un icono base al que se le aplicará un patrón
     * de cuadros. Se usará para el selector de fondo a cuadros.
     * Se creará como una instancia de javax.swing.JButton y su icono será
     * una instancia de vista.components.icons.ColorOverlayIcon.
     */
    CHECKERED_OVERLAY_ICON_BUTTON,
    
    
    /**
     * Un botón transparente que tendrá un icono base al que se le aplicará un icono
     * Ese icono debe ser blanco y transparente
     * Posteriormente se aplicara un color que solo afectara a las partes blancas
     */
    TRANSPARENT,
    
    
    STATUS_BAR_BUTTON 
    
    
    
    
} // --- FIN del enum ButtonType ---