package vista.components;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Action;
import javax.swing.JToggleButton;

import vista.theme.ThemeManager;

/**
 * JToggleButton personalizado que dibuja un marco visible alrededor del botón
 * cuando está seleccionado. El marco usa colorBordeActivo (Component.accentColor),
 * el mismo color que FlatLaf usa para el borde de foco.
 */
public class ThemedToggleButton extends JToggleButton {

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
        // El LAF no pintará el fondo; nosotros solo pintamos el marco en paintComponent.
        setContentAreaFilled(false);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Pintado personalizado
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

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
        }
    }

} // --- Fin de la clase ThemedToggleButton ---