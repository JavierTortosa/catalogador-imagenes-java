package vista.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Action;
import javax.swing.JToggleButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.AppInitializer;
import vista.theme.ThemeManager;

public class ThemedToggleButton extends JToggleButton {

	private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final ThemeManager themeManager;

    public ThemedToggleButton(ThemeManager themeManager, Action action) {
        super(action);
        this.themeManager = themeManager;
        // Nos aseguramos de que el Look and Feel no intente pintar el fondo por nosotros.
        // Nosotros tomamos el control.
        setContentAreaFilled(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Solo pintamos nuestro fondo personalizado si el botón está seleccionado.
        if (isSelected()) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                // Obtenemos el color que queremos de nuestro tema.
                Color selectedColor = themeManager.getTemaActual().colorBotonFondoActivado();
                
                // Pintamos un rectángulo del color deseado sobre toda el área del botón.
                g2.setColor(selectedColor);
                g2.fillRect(0, 0, getWidth(), getHeight());

            } finally {
                g2.dispose();
            }
        }
        
        // MUY IMPORTANTE: Después de pintar (o no) nuestro fondo,
        // le decimos al Look and Feel que continúe con su pintado normal.
        // Esto dibujará el icono, el borde, el texto, etc., ENCIMA de nuestro fondo.
        super.paintComponent(g);
    }
}