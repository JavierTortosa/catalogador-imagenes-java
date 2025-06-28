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
    TOGGLE
} // --- FIN del enum ButtonType ---
