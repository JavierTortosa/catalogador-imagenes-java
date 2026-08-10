package vista.panels.render;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;

import servicios.renderer.Zip2PngScanner.RenderCandidate;

/**
 * Renderer para la lista de candidatos a renderizar. Pinta un checkbox al
 * inicio de cada fila que indica si el candidato está marcado para procesar.
 */
public class RenderListCellRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;

    private final Font baseFont;
    private final JCheckBox check = new JCheckBox();
    private final JLabel label = new JLabel();
    private final JPanel panel = new JPanel(new BorderLayout(6, 0));

    /** Proveedor del estado de marcado de un candidato (o null para no mostrar checkbox). */
    private java.util.function.Predicate<RenderCandidate> marcadoProvider;

    public RenderListCellRenderer() {
        baseFont = getFont().deriveFont(Font.PLAIN, 12f);
        check.setOpaque(false);
        check.setFocusable(false);
        check.setHorizontalAlignment(SwingConstants.CENTER);
        label.setOpaque(true);
        panel.setOpaque(false);
        panel.add(check, BorderLayout.WEST);
        panel.add(label, BorderLayout.CENTER);
    }

    /**
     * Fija el proveedor que determina si un candidato está marcado para procesar.
     *
     * @param marcadoProvider predicado consultado por cada celda, o null para
     *                        ocultar el checkbox
     */
    public void setMarcadoProvider(java.util.function.Predicate<RenderCandidate> marcadoProvider) {
        this.marcadoProvider = marcadoProvider;
    } // --- Fin del metodo setMarcadoProvider ---

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        JLabel base = (JLabel) super.getListCellRendererComponent(
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

            Color bg = isSelected ? new Color(60, 60, 80) : new Color(40, 40, 45);
            label.setBackground(bg);
            label.setForeground(isSelected ? Color.WHITE : label.getForeground());
            label.setBorder(BorderFactory.createEmptyBorder(3, 2, 3, 6));

            if (marcadoProvider != null) {
                check.setSelected(marcadoProvider.test(c));
                panel.setVisible(true);
                panel.setToolTipText(base.getToolTipText());
                return panel;
            }
        }

        return base;
    }

    private String formatearTamano(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

} // --- Fin de la clase RenderListCellRenderer ---
