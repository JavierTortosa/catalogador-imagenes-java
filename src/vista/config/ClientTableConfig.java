package vista.config;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import vista.models.ClienteTableModel;
import vista.models.ProyectoClienteTableModel;
import vista.renderers.CodeCellRenderer;
import vista.renderers.CommentCellRenderer;
import vista.renderers.TristateCellEditor;
import vista.renderers.TristateCellRenderer;

public final class ClientTableConfig {

    private ClientTableConfig() {}


    /**
     * Configura una tabla de proyecto (selecci&oacute;n o descartes).
     */
    public static void configureProjectTable(JTable table) {
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);

        table.getColumnModel().getColumn(ProyectoClienteTableModel.COL_CODIGO)
                .setCellRenderer(new CodeCellRenderer());
        table.getColumnModel().getColumn(ProyectoClienteTableModel.COL_CODIGO).setMaxWidth(40);

        table.getColumnModel().getColumn(ProyectoClienteTableModel.COL_NOMBRE).setMaxWidth(230);
    }


    /**
     * Configura una tabla de cliente (selecci&oacute;n o descartes).
     */
    public static void configureClientTable(JTable table) {
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(false);

        // Indicador visual de ordenaci&oacute;n en el header (▲/▼)
        table.getTableHeader().setDefaultRenderer(new SortHeaderRenderer());

        // Ordenaci&oacute;n por grupos mediante click en cabecera (solo una vez)
        if (table.getTableHeader().getClientProperty("opencode.sort.listener") == null) {
            table.getTableHeader().putClientProperty("opencode.sort.listener", Boolean.TRUE);
            table.getTableHeader().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (table.getModel() instanceof ClienteTableModel ctm) {
                        int viewCol = table.columnAtPoint(e.getPoint());
                        if (viewCol >= 0) {
                            ctm.toggleSort(table.convertColumnIndexToModel(viewCol));
                        }
                    }
                }
            });
        }

        // ---- COL_COLLAPSE ----
        TableColumn collapseCol = table.getColumnModel().getColumn(ClienteTableModel.COL_COLLAPSE);
        collapseCol.setMaxWidth(35);
        collapseCol.setResizable(false);
        collapseCol.setCellRenderer(new TableCellRenderer() {
            private final Color collapseColor = new Color(0, 120, 60);
            private final JLabel label = new JLabel("", JLabel.CENTER);
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                ClienteTableModel m = (ClienteTableModel) t.getModel();
                int modelRow = t.convertRowIndexToModel(row);
                if (m.isParentRow(modelRow)) {
                    label.setText(value != null ? value.toString() : "");
                    label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
                    label.setForeground(isSelected ? label.getForeground() : collapseColor);
                    label.setBorder(null);
                } else {
                    label.setText("");
                    label.setBorder(null);
                }
                label.setOpaque(true);
                label.setBackground(isSelected ? t.getSelectionBackground() : t.getBackground());
                return label;
            }
        });

        // ---- COL_ESTADO ----
        table.getColumnModel().getColumn(ClienteTableModel.COL_ESTADO)
                .setCellRenderer(new TristateCellRenderer());
        table.getColumnModel().getColumn(ClienteTableModel.COL_ESTADO)
                .setCellEditor(new TristateCellEditor());
        table.getColumnModel().getColumn(ClienteTableModel.COL_ESTADO).setMaxWidth(40);

        // ---- COL_CODIGO_IMG ----
        table.getColumnModel().getColumn(ClienteTableModel.COL_CODIGO_IMG)
                .setCellRenderer(new CodeCellRenderer());
        table.getColumnModel().getColumn(ClienteTableModel.COL_CODIGO_IMG).setMaxWidth(50);

        // ---- COL_CODIGO_CB ----
        table.getColumnModel().getColumn(ClienteTableModel.COL_CODIGO_CB)
                .setCellRenderer(new CodeCellRenderer());
        table.getColumnModel().getColumn(ClienteTableModel.COL_CODIGO_CB).setMaxWidth(50);

        // ---- COL_PRECIO ----
        TableColumn precioCol = table.getColumnModel().getColumn(ClienteTableModel.COL_PRECIO);
        precioCol.setCellRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        t, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(JLabel.RIGHT);
                if (value instanceof Number n) {
                    label.setText(n.doubleValue() == 0.0 ? "" : String.format("%.2f", n.doubleValue()));// 270->30-50-50-80-50
                }
                return label;
            }
        });
        JTextField precioEditor = new JTextField();
        precioEditor.setHorizontalAlignment(JTextField.RIGHT);
        precioCol.setCellEditor(new DefaultCellEditor(precioEditor));
        precioCol.setMaxWidth(80);

        // ---- COL_COMENTARIO ----
        table.getColumnModel().getColumn(ClienteTableModel.COL_COMENTARIO)
                .setCellRenderer(new CommentCellRenderer());
        table.getColumnModel().getColumn(ClienteTableModel.COL_COMENTARIO).setMaxWidth(50);
    }


    /**
     * Renderer para el header de la tabla que muestra ▲ (ascendente) o ▼ (descendente)
     * en la columna activa de ordenaci&oacute;n.
     */
    private static final class SortHeaderRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setBorder(BorderFactory.createEtchedBorder());

            if (table.getModel() instanceof ClienteTableModel ctm) {
                int sortCol = ctm.getSortColumn();
                if (sortCol == column) {
                    if (ctm.isSortAscending()) {
                        label.setText(label.getText() + " \u25B2");
                    } else {
                        label.setText(label.getText() + " \u25BC");
                    }
                }
            }
            return label;
        }

    }

} // --- Fin de clase ClientTableConfig ---
