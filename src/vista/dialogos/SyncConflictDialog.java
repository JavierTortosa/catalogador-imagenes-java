package vista.dialogos;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import modelo.proyecto.SelectionState;

/**
 * Di&aacute;logo modal que muestra los conflictos de sincronizaci&oacute;n entre
 * el proyecto y la selecci&oacute;n del cliente.
 * Columnas: C&oacute;digo, Nombre, Estado Proyecto, Estado Cliente, Resoluci&oacute;n (JComboBox por fila).
 * getResolvedConflicts() devuelve Map&lt;String, Boolean&gt; (imageKey &rarr; nuevo enSeleccionProyecto).
 */
public class SyncConflictDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final transient List<FilaConflicto> filas;
    private final ConflictTableModel tableModel;
    private boolean accepted = false;

    public SyncConflictDialog(JFrame owner, List<FilaConflicto> filas) {
        super(owner, "Conflictos de Sincronizaci\u00f3n", true);
        this.filas = new ArrayList<>(filas);
        this.tableModel = new ConflictTableModel(this.filas);

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(750, 500));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JLabel header = new JLabel(
                "Se encontraron " + filas.size()
                + " conflicto(s). Elige la resoluci\u00f3n para cada imagen:",
                SwingConstants.CENTER);
        header.setFont(header.getFont().deriveFont(Font.PLAIN, 13f));
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(header, BorderLayout.NORTH);

        JTable table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(240);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        // JComboBox en columna Resoluci�n
        JComboBox<String> comboResolucion = new JComboBox<>(
                new String[] {"Respetar proyecto", "Aceptar cliente"});
        table.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(comboResolucion));

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        JButton cancelBtn = new JButton("Cancelar");
        cancelBtn.addActionListener(e -> dispose());
        bottomPanel.add(cancelBtn);

        JButton applyBtn = new JButton("Aplicar");
        applyBtn.addActionListener(e -> {
            accepted = true;
            dispose();
        });
        bottomPanel.add(applyBtn);

        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    } // --- Fin de metodo SyncConflictDialog (constructor) ---


    public boolean isAccepted() {
        return accepted;
    } // --- Fin de metodo isAccepted ---


    /**
     * @return Mapa imageKey &rarr; nuevo valor de enSeleccionProyecto.
     *         true = aceptar cliente (incluir en selecci&oacute;n),
     *         false = respetar proyecto (dejar como descarte).
     */
    public Map<String, Boolean> getResolvedConflicts() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (FilaConflicto f : filas) {
            result.put(f.imageKey, f.aceptarCliente);
        }
        return result;
    } // --- Fin de metodo getResolvedConflicts ---


    /**
     * Datos de una fila de conflicto.
     */
    public static class FilaConflicto {
        public final String imageKey;
        public final String codigo;
        public final String nombre;
        public final SelectionState projectState;
        public final SelectionState clientState;
        public boolean aceptarCliente; // false = respetar proyecto (default)

        public FilaConflicto(String imageKey, String codigo, String nombre,
                             SelectionState projectState, SelectionState clientState) {
            this.imageKey = imageKey;
            this.codigo = codigo;
            this.nombre = nombre;
            this.projectState = projectState;
            this.clientState = clientState;
            this.aceptarCliente = false; // default: respetar proyecto
        } // --- Fin de metodo FilaConflicto (constructor) ---

    } // --- Fin de clase FilaConflicto ---


    /**
     * TableModel interno para mostrar los conflictos en una JTable.
     */
    private static class ConflictTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private static final String[] COLUMNS = {
                "C\u00f3digo", "Nombre", "Estado Proyecto", "Estado Cliente", "Resoluci\u00f3n"};
        private final List<FilaConflicto> rows;

        ConflictTableModel(List<FilaConflicto> rows) {
            this.rows = rows;
        } // --- Fin de metodo ConflictTableModel (constructor) ---


        @Override
        public int getRowCount() {
            return rows.size();
        } // --- Fin de metodo getRowCount ---


        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        } // --- Fin de metodo getColumnCount ---


        @Override
        public String getColumnName(int col) {
            return COLUMNS[col];
        } // --- Fin de metodo getColumnName ---


        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 4;
        } // --- Fin de metodo isCellEditable ---


        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 4) {
                rows.get(row).aceptarCliente = "Aceptar cliente".equals(value);
                fireTableCellUpdated(row, col);
            }
        } // --- Fin de metodo setValueAt ---


        @Override
        public Object getValueAt(int row, int col) {
            FilaConflicto f = rows.get(row);
            return switch (col) {
                case 0 -> f.codigo;
                case 1 -> f.nombre;
                case 2 -> formatState(f.projectState);
                case 3 -> formatState(f.clientState);
                case 4 -> f.aceptarCliente ? "Aceptar cliente" : "Respetar proyecto";
                default -> "";
            };
        } // --- Fin de metodo getValueAt ---


        private String formatState(SelectionState s) {
            if (s == null) return "\u2014";
            return switch (s) {
                case SELECTED   -> "Seleccionado (\u2713)";
                case DISCARDED  -> "Descartado (\u2717)";
                case UNDEFINED  -> "Sin definir (\u25CB)";
            };
        } // --- Fin de metodo formatState ---

    } // --- Fin de clase ConflictTableModel ---

} // --- Fin de clase SyncConflictDialog ---
