package vista.renderers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import modelo.proyecto.SelectionState;

/**
 * Renderizador de celdas para el estado del cliente (SELECTED/DISCARDED/UNDEFINED).
 * Muestra ✓ (verde), ✗ (rojo) o — (gris) según el SelectionState.
 */
public class TristateCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    private static final Color GREEN = new Color(34, 139, 34);
    private static final Color RED = new Color(200, 50, 50);
    private static final Color GRAY = Color.GRAY;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));
        label.setHorizontalAlignment(JLabel.CENTER);
        if (value instanceof SelectionState state) {
            switch (state) {
                case SELECTED -> {
                    label.setText("\u2713");
                    label.setForeground(isSelected ? label.getForeground() : GREEN);
                    label.setToolTipText("Seleccionado");
                }
                case DISCARDED -> {
                    label.setText("\u2717");
                    label.setForeground(isSelected ? label.getForeground() : RED);
                    label.setToolTipText("Descartado");
                }
                default -> {
                    label.setText("\u2014");
                    label.setForeground(isSelected ? label.getForeground() : GRAY);
                    label.setToolTipText("Sin definir");
                }
            }
        } else {
            label.setText("");
            label.setToolTipText(null);
        }
        return label;
    } // --- Fin de metodo getTableCellRendererComponent ---

} // --- Fin de clase TristateCellRenderer ---
