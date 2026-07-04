package vista.renderers;

import java.awt.Color;
import java.awt.Component;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import modelo.proyecto.ImageCheckboxOverlay;
import modelo.proyecto.Mensaje;
import modelo.proyecto.ProjectImage;
import vista.models.ClienteTableModel;
import modelo.proyecto.CommentThread;

public class CommentCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    // Colores del indicador de estado unificados
    private static final Color COLOR_NUEVO    = new Color(220, 50, 50);    // Rojo (Estado 3)
    private static final Color COLOR_LEIDO    = new Color(60, 190, 60);    // Verde (Estado 1)
    private static final Color COLOR_ESPERA   = new Color(50, 130, 220);   // Azul (Estado 2)
    private static final Color COLOR_SIN_MSGS = new Color(160, 160, 160);  // Gris (Estado 0)
    private static final Color IND_COLLAPSED  = new Color(70, 130, 180);   // Azul (hijos colapsados)

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        label.setHorizontalAlignment(JLabel.CENTER);
        label.setText("");

        if (!(table.getModel() instanceof ClienteTableModel ctm)) {
            return label;
        }

        int modelRow = table.convertRowIndexToModel(row);
        boolean isParent = ctm.isParentRow(modelRow);

        label.setOpaque(true);
        if (!isSelected) {
            label.setBackground(Color.BLACK);
        }

        // Recuperamos el CommentThread real desde el modelo para leer sus estados
        CommentThread realThread = null;
        if (isParent) {
            ProjectImage pi = ctm.getProjectImage(modelRow);
            if (pi != null) realThread = pi.getCommentThreadAccess(); // Usamos el método que accede al objeto
        } else {
            ImageCheckboxOverlay cb = ctm.getCheckbox(modelRow);
            if (cb != null) realThread = cb.getCommentThreadAccess(); // Usamos el método que accede al objeto
        }

        if (realThread != null && !realThread.isEmpty()) {
            label.setText("\u2709");
            Mensaje ultimoMsg = realThread.getLast();
            if (ultimoMsg != null) {
                label.setToolTipText(ultimoMsg.de().toUpperCase() + ": " + ultimoMsg.texto());
            }

            if (!isSelected) {
                int estado = realThread.getEstadoPr();
                Color color;
                switch (estado) {
                    case 1:
                        color = COLOR_LEIDO; // Verde (Leído no contestado)
                        break;
                    case 2:
                        color = COLOR_ESPERA; // Azul (Contestado)
                        break;
                    case 3:
                        color = COLOR_NUEVO; // Rojo (Nuevo no leído)
                        break;
                    default:
                        color = COLOR_SIN_MSGS; // Gris (Por seguridad)
                        break;
                }
                label.setForeground(color);
                label.setBorder(crearBorde(color));
            }
        } else {
            label.setText(isParent ? "" : "\u2709");
            label.setToolTipText(null);
            if (!isSelected) {
                label.setForeground(COLOR_SIN_MSGS);
                label.setBorder(crearBorde(COLOR_SIN_MSGS));
            }
        }

        if (isParent) {
            int msgCount = ctm.getCheckboxMessageCount(modelRow);
            if (msgCount > 0 && ctm.getCollapseFilter(modelRow) != ClienteTableModel.CollapseFilter.NONE) {
                String textoActual = label.getText();
                label.setText((textoActual.isEmpty() ? "" : textoActual + " | ") + "\uD83D\uDCDD " + msgCount);

                if (!isSelected && COLOR_SIN_MSGS.equals(label.getForeground())) {
                    label.setForeground(IND_COLLAPSED);
                    label.setBorder(crearBorde(IND_COLLAPSED));
                }
            }
        }

        return label;
    }

    private static Border crearBorde(Color color) {
        return BorderFactory.createMatteBorder(0, 4, 0, 0, color);
    }

} // --- Fin de clase CommentCellRenderer ---

