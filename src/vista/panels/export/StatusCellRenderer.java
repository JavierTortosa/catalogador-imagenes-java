package vista.panels.export;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import modelo.proyecto.ExportItem;
import modelo.proyecto.ExportStatus;
import vista.util.IconUtils;

public class StatusCellRenderer implements TableCellRenderer {

    private IconUtils iconUtils;

    private static final Color COLOR_DESACTIVADO = new Color(220, 220, 220);
    private static final Color COLOR_TEXTO_DESACTIVADO = new Color(128, 128, 128);

    public StatusCellRenderer(IconUtils iconUtils) {
        this.iconUtils = iconUtils;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        if (!(value instanceof ExportItem)) {
            JLabel fallback = new JLabel(value != null ? value.toString() : "");
            fallback.setOpaque(true);
            if (isSelected) {
                fallback.setBackground(table.getSelectionBackground());
                fallback.setForeground(table.getSelectionForeground());
            } else {
                fallback.setBackground(table.getBackground());
                fallback.setForeground(table.getForeground());
            }
            return fallback;
        }

        ExportItem item = (ExportItem) value;
        ExportStatus status = item.tieneConflictoDeNombre() ? ExportStatus.NOMBRE_DUPLICADO : item.getEstadoArchivoComprimido();

        JLabel iconLabel = new JLabel();
        ImageIcon icon = iconUtils.getCommonIcon(status.getIconName());
        iconLabel.setIcon(iconUtils.scaleImageIcon(icon, 16, 16));

        JLabel textLabel = new JLabel(status.getDisplay(), SwingConstants.CENTER);

        JPanel panel = new JPanel(new BorderLayout(4, 0));
        panel.add(iconLabel, BorderLayout.WEST);
        panel.add(textLabel, BorderLayout.CENTER);

        if (isSelected) {
            panel.setBackground(table.getSelectionBackground());
            textLabel.setForeground(table.getSelectionForeground());
        } else if (!item.isSeleccionadoParaExportar()) {
            panel.setBackground(COLOR_DESACTIVADO);
            textLabel.setForeground(COLOR_TEXTO_DESACTIVADO);
        } else {
            Color statusColor = status.getColor();
            if (statusColor != null) {
                panel.setBackground(statusColor);
            } else {
                panel.setBackground(table.getBackground());
            }

            if (status == ExportStatus.IMAGEN_NO_ENCONTRADA ||
                    status == ExportStatus.ERROR_COPIA ||
                    status == ExportStatus.NOMBRE_DUPLICADO ||
                    status == ExportStatus.ASIGNADO_DUPLICADO ||
                    status == ExportStatus.ENCONTRADO_OK ||
                    status == ExportStatus.ASIGNADO_MANUAL ||
                    status == ExportStatus.COPIADO_OK)
            {
                textLabel.setForeground(Color.WHITE);
            } else {
                textLabel.setForeground(Color.BLACK);
            }
        }

        panel.setToolTipText(status.getTooltip());
        return panel;
    }

}
