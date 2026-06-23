package vista.renderers;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renderizador de celdas para la columna de Acciones en las tablas
 * de proyecto en modo cliente. Muestra ↑✎ (selección) o ↓✎ (descartes).
 */
public class ActionCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    private final boolean esSeleccion;

    public ActionCellRenderer(boolean esSeleccion) {
        this.esSeleccion = esSeleccion;
    } // --- Fin de metodo ActionCellRenderer (constructor) ---


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 14f));
        label.setHorizontalAlignment(JLabel.CENTER);
        if (esSeleccion) {
            label.setText("\u2191 \u270E");
            label.setToolTipText("Mover a descartes / Editar c\u00f3digo");
        } else {
            label.setText("\u2193 \u270E");
            label.setToolTipText("Restaurar a selecci\u00f3n / Editar c\u00f3digo");
        }
        return label;
    } // --- Fin de metodo getTableCellRendererComponent ---

} // --- Fin de clase ActionCellRenderer ---
