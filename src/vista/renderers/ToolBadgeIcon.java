package vista.renderers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

/**
 * Icono decorador que superpone la letra del atajo de teclado en la esquina
 * inferior derecha del icono base.
 * <p>
 * Se usa en la barra izquierda del editor avanzado para que el usuario recuerde
 * la hotkey de cada herramienta sin abrir la ayuda.
 */
public class ToolBadgeIcon implements Icon {

    private final Icon base;
    private final char letter;

    /**
     * @param base   icono original de la herramienta
     * @param letter letra del atajo (mayúscula)
     */
    public ToolBadgeIcon(Icon base, char letter) {
        this.base = base;
        this.letter = letter;
    } // --- Fin del constructor ToolBadgeIcon ---


    @Override
    public int getIconWidth() {
        return base.getIconWidth();
    } // --- Fin del metodo getIconWidth ---


    @Override
    public int getIconHeight() {
        return base.getIconHeight();
    } // --- Fin del metodo getIconHeight ---


    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        base.paintIcon(c, g, x, y);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = base.getIconWidth();
            int h = base.getIconHeight();
            int badgeSize = Math.max(8, (int) Math.round(w * 0.45));
            int bx = x + w - badgeSize;
            int by = y + h - badgeSize;

            g2.setColor(new Color(30, 30, 30, 210));
            g2.fillRoundRect(bx, by, badgeSize, badgeSize, 3, 3);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(bx, by, badgeSize - 1, badgeSize - 1, 3, 3);

            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, badgeSize - 2));
            FontMetrics fm = g2.getFontMetrics();
            String s = String.valueOf(letter);
            int tx = bx + (badgeSize - fm.stringWidth(s)) / 2;
            int ty = by + (badgeSize - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(s, tx, ty);
        } finally {
            g2.dispose();
        }
    } // --- Fin del metodo paintIcon ---

} // --- Fin de la clase ToolBadgeIcon ---
