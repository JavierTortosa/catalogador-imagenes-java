package vista.renderers;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renderizador de celdas para comentarios en las tablas de modo cliente.
 * Muestra un icono de sobre (✉) si hay comentario, con tooltip.
 */
public class CommentCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
        String text = (value != null) ? value.toString() : "";
        if (!text.trim().isEmpty()) {
            label.setText("\u2709");
            label.setToolTipText(text);
        } else {
            label.setText("");
            label.setToolTipText(null);
        }
        label.setHorizontalAlignment(JLabel.CENTER);
        return label;
    } // --- Fin de metodo getTableCellRendererComponent ---

} // --- Fin de clase CommentCellRenderer ---
