package vista.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.Border;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Un Border personalizado que dibuja una simple línea de subrayado debajo de un componente.
 */
public class UnderlineBorder implements Border {

	private static final Logger logger = LoggerFactory.getLogger(UnderlineBorder.class);
	
    private final int thickness;
    private final Color color;

    /**
     * Crea un borde de subrayado.
     * @param color El color de la línea.
     * @param thickness El grosor de la línea en píxeles.
     */
    public UnderlineBorder(Color color, int thickness) {
        this.color = color;
        this.thickness = thickness;
    } // --- FIN del constructor --- 

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        // Guardamos el color original del Graphics para restaurarlo después.
        Color oldColor = g.getColor();
        
        // Establecemos el color de nuestro subrayado.
        g.setColor(this.color);
        
        // Dibujamos un rectángulo relleno en la parte inferior del componente.
        // Esto crea la línea del grosor especificado.
        g.fillRect(x, y + height - thickness, width, thickness);
        
        // Restauramos el color original.
        g.setColor(oldColor);
    } // --- FIN del metodo paintBorder ---

    @Override
    public Insets getBorderInsets(Component c) {
        // Este método es CRUCIAL. Le dice al componente que reserve espacio
        // en la parte inferior para que nuestro subrayado no se pinte encima del icono.
        return new Insets(0, 0, this.thickness, 0);
    } // --- FIN del metodo getBorderInsets ---

    @Override
    public boolean isBorderOpaque() {
        // Nuestro borde no es opaco, ya que solo es una línea.
        return false;
    } // --- FIN del metodo isBorderOpaque ---
    
}// --- FIN DE LA CLASE UnderlineBorder ---