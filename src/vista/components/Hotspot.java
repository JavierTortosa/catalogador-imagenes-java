package vista.components;

import java.awt.Image;
import java.awt.Rectangle;
import javax.swing.Action;

/**
 * Representa un "punto caliente" o zona interactiva dentro de un componente de imagen.
 * Cada hotspot tiene una clave única, una región definida, una imagen para mostrar
 * cuando el ratón está sobre él, y una acción asociada para ejecutar al ser clicado.
 *
 * @param key        Un identificador único para este hotspot (ej. "up", "on").
 * @param bounds     La región rectangular dentro del componente donde este hotspot es activo.
 * @param hoverImage La imagen que se dibujará sobre la imagen base cuando el ratón
 *                   esté sobre este hotspot. Puede ser null si no hay efecto visual especial.
 * @param action     La acción de Swing asociada a este hotspot, que se ejecutará al hacer clic.
 */
public record Hotspot(
    String key,             // Identificador único del hotspot (ej. "up", "down", "on")
    Rectangle bounds,       // Las coordenadas y dimensiones de la zona activa dentro del componente
    Image hoverImage,       // La imagen a dibujar cuando el ratón está sobre este hotspot
    Action action           // La Action de Swing asociada a este hotspot
) {
    // Los records generan automáticamente constructor, getters, equals(), hashCode() y toString().
    // Puedes añadir validaciones si son necesarias, por ejemplo:
    public Hotspot {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("La clave del Hotspot no puede ser nula o vacía.");
        }
        if (bounds == null) {
            throw new IllegalArgumentException("Los límites (bounds) del Hotspot no pueden ser nulos.");
        }
        // La imagen de hover y la acción pueden ser null, dependiendo del caso de uso.
    }
} // --- FIN de la clase record Hotspot ---