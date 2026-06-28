package vista.renderers;

import java.awt.Color;
import java.awt.Component;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import modelo.proyecto.ImageCheckboxOverlay;
import modelo.proyecto.Mensaje;
import vista.models.ClienteTableModel;

/**
 * Renderizador de celdas para comentarios en las tablas de modo cliente.
 * Muestra un icono de sobre (✉) si hay comentario, con tooltip.
 * Dorado si el �ltimo mensaje es del cliente, gris si es del fot�grafo.
 */
public class CommentCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    private static final Color GOLD = new Color(200, 160, 0);
    private static final Color GRAY = new Color(160, 160, 160);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        String text = (value != null) ? value.toString() : "";
        label.setHorizontalAlignment(JLabel.CENTER);

        if (!text.trim().isEmpty()) {
            label.setText("\u2709");
            label.setToolTipText(text);

            // Comprobar si el �ltimo mensaje es del cliente → dorado
            boolean esCliente = false;
            if (table.getModel() instanceof ClienteTableModel ctm) {
                int modelRow = table.convertRowIndexToModel(row);
                if (ctm.isChildRow(modelRow)) {
                    ImageCheckboxOverlay cb = ctm.getCheckbox(modelRow);
                    if (cb != null) {
                        List<Mensaje> thread = cb.getCommentThread();
                        if (!thread.isEmpty() && "cliente".equals(thread.get(thread.size() - 1).de())) {
                            esCliente = true;
                        }
                    }
                }
            }
            label.setForeground(esCliente && !isSelected ? GOLD : GRAY);
        } else {
            label.setText("");
            label.setToolTipText(null);
            label.setForeground(null);
        }
        return label;
    } // --- Fin de metodo getTableCellRendererComponent ---

} // --- Fin de clase CommentCellRenderer ---
