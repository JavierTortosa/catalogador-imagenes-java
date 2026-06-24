package vista.dialogos;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import servicios.cliente.ClientSyncService.ConflictEntry;

/**
 * Diálogo modal que muestra los conflictos de sincronización entre
 * el proyecto y la selección del cliente. Permite resolver cada conflicto
 * individualmente, usar "Mantener proyecto (todo)" o "Aceptar cliente (todo)".
 */
public class SyncConflictDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final transient List<ConflictEntry> conflicts;
    private final ConflictTableModel tableModel;
    private boolean accepted = false;

    public SyncConflictDialog(JFrame owner, List<ConflictEntry> conflicts) {
        super(owner, "Conflictos de Sincronizaci\u00f3n", true);
        this.conflicts = new ArrayList<>(conflicts);
        this.tableModel = new ConflictTableModel(this.conflicts);

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(700, 500));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JLabel header = new JLabel(
                "Se encontraron " + conflicts.size() + " conflicto(s). Elige qu\u00e9 estado mantener:",
                SwingConstants.CENTER);
        header.setFont(header.getFont().deriveFont(Font.PLAIN, 13f));
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(header, BorderLayout.NORTH);

        JTable table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));

        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        JButton keepProjectBtn = new JButton("Mantener proyecto (todo)");
        keepProjectBtn.addActionListener(e -> {
            for (ConflictEntry ce : conflicts) {
                ce.setResolvedState(ce.getProjectState());
            }
            tableModel.fireTableDataChanged();
        });
        rowPanel.add(keepProjectBtn);

        JButton acceptClientBtn = new JButton("Aceptar cliente (todo)");
        acceptClientBtn.addActionListener(e -> {
            for (ConflictEntry ce : conflicts) {
                ce.setResolvedState(ce.getClientState());
            }
            tableModel.fireTableDataChanged();
        });
        rowPanel.add(acceptClientBtn);

        buttonPanel.add(rowPanel);

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

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPanel, BorderLayout.NORTH);
        southPanel.add(bottomPanel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    } // --- Fin de metodo SyncConflictDialog (constructor) ---


    public boolean isAccepted() {
        return accepted;
    } // --- Fin de metodo isAccepted ---


    public List<ConflictEntry> getResolvedConflicts() {
        return conflicts;
    } // --- Fin de metodo getResolvedConflicts ---


    /**
     * TableModel interno para mostrar los conflictos en una JTable.
     */
    private static class ConflictTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private static final String[] COLUMNS = {"Imagen", "Estado Proyecto", "Estado Cliente"};
        private final List<ConflictEntry> entries;

        ConflictTableModel(List<ConflictEntry> entries) {
            this.entries = entries;
        } // --- Fin de metodo ConflictTableModel (constructor) ---


        @Override
        public int getRowCount() {
            return entries.size();
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
        public Object getValueAt(int row, int col) {
            ConflictEntry e = entries.get(row);
            switch (col) {
                case 0:
                    java.nio.file.Path p = java.nio.file.Paths.get(e.getImageKey());
                    java.nio.file.Path fn = p.getFileName();
                    return fn != null ? fn.toString() : e.getImageKey();
                case 1: return formatState(e.getResolvedState() != null ? e.getResolvedState() : e.getProjectState());
                case 2: return formatState(e.getClientState());
                default: return "";
            }
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
