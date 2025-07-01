// Contenido de la clase vista.components.icons.ColorOverlayIcon.java

package vista.components.icons;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.util.Objects;

import javax.swing.Icon; // Importar la interfaz Icon

/**
 * Una implementación de la interfaz Icon que dibuja una imagen base
 * y opcionalmente le aplica una superposición de color o un patrón de cuadros.
 * Útil para crear iconos dinámicos (tintados) a partir de una imagen base (ej. un círculo).
 */
public class ColorOverlayIcon implements Icon {

    private final Image baseImage; // La imagen original (ej. el círculo, un emoticono)
    private final Color overlayColor; // El color para superponer (puede ser null)
    private final boolean isCheckered; // Si se debe dibujar un patrón de cuadros en lugar de un color
    private final int width; // Ancho del icono
    private final int height; // Alto del icono

    // Constructor para color sólido
    /**
     * Crea un ColorOverlayIcon que dibuja una imagen base y le aplica una superposición de color.
     *
     * @param baseImage La imagen base que se usará como forma.
     * @param overlayColor El color que se superpondrá a la imagen base.
     * @param width El ancho deseado para el icono.
     * @param height El alto deseado para el icono.
     * @throws NullPointerException si baseImage o overlayColor son nulos.
     */
    public ColorOverlayIcon(Image baseImage, Color overlayColor, int width, int height) {
        this.baseImage = Objects.requireNonNull(baseImage, "La imagen base no puede ser nula.");
        this.overlayColor = Objects.requireNonNull(overlayColor, "El color de superposición no puede ser nulo.");
        this.isCheckered = false;
        this.width = width > 0 ? width : baseImage.getWidth(null);
        this.height = height > 0 ? height : baseImage.getHeight(null);
        // Asegurarse de que las dimensiones sean válidas, usando las de la imagen si no se especifican.
        if (this.width <= 0 || this.height <= 0) {
            throw new IllegalArgumentException("Las dimensiones del icono deben ser positivas.");
        }
    } // --- Fin del constructor ColorOverlayIcon (para color sólido) ---

    // Constructor para patrón de cuadros
    /**
     * Crea un ColorOverlayIcon que dibuja una imagen base y le aplica un patrón de cuadros.
     *
     * @param baseImage La imagen base que se usará como forma.
     * @param width El ancho deseado para el icono.
     * @param height El alto deseado para el icono.
     * @throws NullPointerException si baseImage es nula.
     */
    public ColorOverlayIcon(Image baseImage, int width, int height) {
        this.baseImage = Objects.requireNonNull(baseImage, "La imagen base no puede ser nula.");
        this.overlayColor = null; // No hay color para superponer
        this.isCheckered = true;
        this.width = width > 0 ? width : baseImage.getWidth(null);
        this.height = height > 0 ? height : baseImage.getHeight(null);
        // Asegurarse de que las dimensiones sean válidas
        if (this.width <= 0 || this.height <= 0) {
            throw new IllegalArgumentException("Las dimensiones del icono deben ser positivas.");
        }
    } // --- Fin del constructor ColorOverlayIcon (para patrón de cuadros) ---

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create(); // Crear una copia del contexto gráfico

        try {
            // Habilitar anti-aliasing para un mejor renderizado del icono
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // 1. Dibujar la imagen base (con su alfa original)
            // Se dibuja a las dimensiones finales (width, height)
            g2d.drawImage(baseImage, x, y, width, height, c);

            // 2. Aplicar la superposición (color o patrón) usando AlphaComposite para respetar la forma de la imagen base
            // Esto asegura que el color/patrón solo se aplique a las partes opacas de la imagen base.
            g2d.setComposite(AlphaComposite.SrcAtop); // Dibuja solo donde ya hay píxeles opacos de la fuente.

            if (isCheckered) {
                // Dibujar patrón de cuadros
                int cellSize = Math.max(2, Math.min(width, height) / 4); // Tamaño de celda dinámico
                Color color1 = Color.WHITE;
                Color color2 = Color.LIGHT_GRAY;

                for (int row = 0; row < height; row += cellSize) {
                    for (int col = 0; col < width; col += cellSize) {
                        g2d.setColor((((row / cellSize) % 2) == ((col / cellSize) % 2)) ? color1 : color2);
                        g2d.fillRect(x + col, y + row, cellSize, cellSize);
                    }
                }
            } else if (overlayColor != null) {
                // Dibujar color sólido
                g2d.setColor(overlayColor);
                g2d.fillRect(x, y, width, height); // Cubre toda el área del icono
            }

        } finally {
            g2d.dispose(); // Liberar recursos
        }
    } // --- Fin del método paintIcon ---

    @Override
    public int getIconWidth() {
        return width;
    } // --- Fin del método getIconWidth ---

    @Override
    public int getIconHeight() {
        return height;
    } // --- Fin del método getIconHeight ---

} // --- FIN de la clase ColorOverlayIcon ---