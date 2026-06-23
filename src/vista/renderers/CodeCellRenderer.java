package vista.renderers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renderizador de celdas para códigos de catálogo en las tablas
 * de modo cliente. Muestra el texto en azul negrita centrado.
 */
public class CodeCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    private final Color codeColor = new Color(70, 130, 180);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        if (!isSelected) {
            label.setForeground(codeColor);
        }
        label.setHorizontalAlignment(JLabel.CENTER);
        return label;
    } // --- Fin de metodo getTableCellRendererComponent ---

} // --- Fin de clase CodeCellRenderer ---
