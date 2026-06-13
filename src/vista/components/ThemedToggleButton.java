package vista.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Action;
import javax.swing.JToggleButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vista.theme.ThemeManager;

/**
 * JToggleButton personalizado que pinta un fondo y un marco visibles cuando está seleccionado.
 * El marco usa el color de borde activo del tema (colorBordeActivo =
 * Component.accentColor), el mismo que FlatLaf usa para el foco.
 */
public class ThemedToggleButton extends JToggleButton {

    private static final Logger logger = LoggerFactory.getLogger(ThemedToggleButton.class);
    private static final long serialVersionUID = 2L;
    private static final int BORDER_THICKNESS = 3;

    private final ThemeManager themeManager;

    /**
     * Crea un ThemedToggleButton asociado a una acción.
     * @param themeManager Gestor de temas para obtener colores
     * @param action       Acción asociada al botón
     */
    public ThemedToggleButton(ThemeManager themeManager, Action action) {
        super(action);
        this.themeManager = themeManager;
        // El LAF no pintará el fondo; nosotros lo hacemos en paintComponent.
        setContentAreaFilled(false);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Pintado personalizado
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        if (isSelected()) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(themeManager.getTemaActual().colorBotonFondoActivado());
                g2.fillRect(0, 0, getWidth(), getHeight());
            } finally {
                g2.dispose();
            }
        }
        super.paintComponent(g);
    }

    @Override
    protected void paintBorder(Graphics g) {
        if (isSelected()) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setStroke(new BasicStroke(BORDER_THICKNESS));
                g2.setColor(themeManager.getTemaActual().colorBordeActivo());
                g2.drawRect(BORDER_THICKNESS / 2, BORDER_THICKNESS / 2,
                        getWidth() - BORDER_THICKNESS, getHeight() - BORDER_THICKNESS);
            } finally {
                g2.dispose();
            }
        } else {
            super.paintBorder(g);
        }
    }

} // --- Fin de la clase ThemedToggleButton ---