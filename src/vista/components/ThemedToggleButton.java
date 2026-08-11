package vista.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Action;
import javax.swing.JToggleButton;
import javax.swing.UIManager;

import vista.theme.ThemeManager;

/**
 * JToggleButton personalizado que dibuja un marco visible alrededor del botón
 * cuando está seleccionado. El marco usa colorBordeActivo (Component.accentColor),
 * el mismo color que FlatLaf usa para el borde de foco.
 */
public class ThemedToggleButton extends JToggleButton {

    private static final long serialVersionUID = 3L;
    private static final int BORDER_THICKNESS = 3;

    private ThemeManager themeManager;

    /**
     * Crea un ThemedToggleButton asociado a una acción.
     * @param themeManager Gestor de temas para obtener colores
     * @param action       Acción asociada al botón
     */
    public ThemedToggleButton(ThemeManager themeManager, Action action) {
        this(action);
        this.themeManager = themeManager;
    } // --- Fin del constructor ThemedToggleButton (ThemeManager, Action) ---


    /**
     * Crea un ThemedToggleButton sin ThemeManager. Útil cuando el gestor de temas
     * se asigna posteriormente con {@link #setThemeManager(ThemeManager)}. Sin
     * tema, el marco usa como fallback el color de acento de FlatLaf.
     *
     * @param action Acción asociada al botón
     */
    public ThemedToggleButton(Action action) {
        super(action);
        // El LAF no pintará el fondo; nosotros solo pintamos el marco en paintComponent.
        setContentAreaFilled(false);
    } // --- Fin del constructor ThemedToggleButton (Action) ---


    /**
     * Asigna el gestor de temas para obtener el color del marco. Puede invocarse
     * tras la construcción si el ThemeManager aún no estaba disponible.
     *
     * @param themeManager gestor de temas
     */
    public void setThemeManager(ThemeManager themeManager) {
        this.themeManager = themeManager;
    } // --- Fin del metodo setThemeManager ---

    // ─────────────────────────────────────────────────────────────────
    //  Pintado personalizado
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (isSelected()) {
            Color borde = colorBordeActivo();
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setStroke(new BasicStroke(BORDER_THICKNESS));
                g2.setColor(borde);
                g2.drawRect(BORDER_THICKNESS / 2, BORDER_THICKNESS / 2,
                        getWidth() - BORDER_THICKNESS, getHeight() - BORDER_THICKNESS);
            } finally {
                g2.dispose();
            }
        }
    } // --- Fin del metodo paintComponent ---


    /**
     * Devuelve el color del marco: el colorBordeActivo del tema activo, o como
     * fallback el color de acento de FlatLaf si no hay tema disponible.
     *
     * @return color del marco
     */
    private Color colorBordeActivo() {
        if (themeManager != null && themeManager.getTemaActual() != null) {
            return themeManager.getTemaActual().colorBordeActivo();
        }
        Color accent = UIManager.getColor("Component.accentColor");
        return accent != null ? accent : new Color(0, 120, 215);
    } // --- Fin del metodo colorBordeActivo ---

} // --- Fin de la clase ThemedToggleButton ---