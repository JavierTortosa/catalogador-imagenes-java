package modelo.editor;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Interfaz base para todos los tipos de capa del editor.
 * <p>
 * Cada implementación sabe pintarse a sí misma ({@link #paint(Graphics2D)})
 * y generar su propia miniatura ({@link #renderThumbnail(int)}).
 */
public interface Layer {

    String getId();

    String getName();
    void setName(String name);

    Rectangle getBounds();
    void setBounds(Rectangle bounds);

    boolean isVisible();
    void setVisible(boolean visible);

    boolean isLocked();
    void setLocked(boolean locked);

    float getOpacity();
    void setOpacity(float opacity);

    /**
     * Renderiza esta capa sobre el contexto gráfico proporcionado.
     * El caller debe haber aplicado ya el zoom/pan si corresponde.
     */
    void paint(Graphics2D g2);

    /**
     * Genera una miniatura cuadrada de esta capa para la tarjeta del panel de capas.
     */
    BufferedImage renderThumbnail(int size);

    /**
     * Crea una copia independiente de esta capa.
     */
    Layer copy();

} // --- Fin de la interfaz Layer ---
