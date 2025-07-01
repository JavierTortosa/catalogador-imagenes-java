package vista.components;

/**
 * Define las direcciones cardinales y un estado "NONE" para el DPadComponent.
 * Utilizado para identificar qué zona del D-Pad está activa o qué dirección
 * de paneo se está solicitando.
 */
public enum Direction {
    NONE,   // Indica que no hay una dirección activa o seleccionada.
    UP,     // Dirección hacia arriba.
    DOWN,   // Dirección hacia abajo.
    LEFT,   // Dirección hacia la izquierda.
    RIGHT   // Dirección hacia la derecha.
} // --- FIN de la clase enum Direction ---