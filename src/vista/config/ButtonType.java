package vista.config;

/**
 * Define el tipo de botón a crear en una barra de herramientas,
 * permitiendo diferenciar entre botones estándar y botones de tipo toggle
 * que pueden mantener un estado de presionado/seleccionado.
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
     * Un componente de control direccional personalizado (D-Pad).
     * Se creará como una instancia de vista.components.DPadComponent.
     * Representará múltiples zonas clicables que ejecutan acciones distintas.
     */
    DPAD,
    
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
    CHECKERED_OVERLAY_ICON_BUTTON
} // --- FIN del enum ButtonType ---
