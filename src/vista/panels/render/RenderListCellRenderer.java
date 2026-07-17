package vista.panels.render;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import servicios.renderer.Zip2PngScanner.RenderCandidate;

/**
 * Renderer para la lista de candidatos a renderizar.
 */
public class RenderListCellRenderer extends DefaultListCellRenderer {

    private final Font baseFont;

    public RenderListCellRenderer() {
        baseFont = getFont().deriveFont(Font.PLAIN, 12f);
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);

        if (value instanceof RenderCandidate) {
            RenderCandidate c = (RenderCandidate) value;
            String sizeStr = formatearTamano(c.tamanoBytes);
            String tipo = c.esComprimido ? "[ZIP]" : "[3D]";
            StringBuilder sb = new StringBuilder();
            sb.append(tipo).append(" ").append(c.nombreBase).append("  (").append(sizeStr).append(")");
            if (c.tieneImagenesDentro()) {
                sb.append(" [").append(c.imagenesInternas.size()).append(" img]");
            }
            label.setText(sb.toString());
            label.setFont(baseFont);

            if (c.excedeLimite) {
                label.setForeground(Color.RED);
                label.setText("\u26A0 " + label.getText());
            } else if (!isSelected) {
                label.setForeground(Color.WHITE);
            }

            if (isSelected) {
                label.setBackground(new Color(60, 60, 80));
                label.setForeground(Color.WHITE);
            } else {
                label.setBackground(new Color(40, 40, 45));
            }
            label.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        }

        return label;
    }

    private String formatearTamano(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

} // --- Fin de la clase RenderListCellRenderer ---
