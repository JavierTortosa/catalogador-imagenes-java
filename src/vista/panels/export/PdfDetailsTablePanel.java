package vista.panels.export;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.proyecto.ExportItem;


public class PdfDetailsTablePanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(PdfDetailsTablePanel.class);
    private static final long serialVersionUID = 1L;

    private JTable detailsTable;
    private PdfDetailsTableModel tableModel;
    private List<ExportItem> currentItems = new ArrayList<>();

    // Listener para avisar al controlador cuando se edita un dato
    private Runnable onDataChangedListener;

    // Constructor: establece el borde y lanza la construcción de componentes
    public PdfDetailsTablePanel() {
        super(new BorderLayout(5, 5));
        TitledBorder border = BorderFactory.createTitledBorder("Campos del Catálogo PDF");
        setBorder(border);
        initComponents();
    } // --- Fin del metodo: PdfDetailsTablePanel ---


    // Inicializa la tabla con sus columnas y la añade al panel
    private void initComponents() {
        tableModel = new PdfDetailsTableModel();
        detailsTable = new JTable(tableModel);
        detailsTable.setFillsViewportHeight(true);
        detailsTable.setShowGrid(true);
        detailsTable.putClientProperty("JTable.autoStartsEdit", true);

        detailsTable.getTableHeader().setReorderingAllowed(false);

        detailsTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        detailsTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        detailsTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        detailsTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        detailsTable.getColumnModel().getColumn(4).setPreferredWidth(70);
        detailsTable.getColumnModel().getColumn(5).setPreferredWidth(200);

        add(new JScrollPane(detailsTable), BorderLayout.CENTER);
    } // --- Fin del metodo: initComponents ---


    // Permite inyectar el listener desde ExportPanel para notificar cambios
    public void setOnDataChangedListener(Runnable listener) {
        this.onDataChangedListener = listener;
    } // --- Fin del metodo: setOnDataChangedListener ---


    // Carga la lista de items (solo los marcados para exportar) en la tabla
    public void setItems(List<ExportItem> items) {
        if (items != null) {
            this.currentItems = items.stream()
                                     .filter(ExportItem::isSeleccionadoParaExportar)
                                     .collect(Collectors.toList());
        } else {
            this.currentItems.clear();
        }
        tableModel.fireTableDataChanged();
    } // --- Fin del metodo: setItems ---


    // Refresca los datos visibles en la tabla sin recargar la lista
    public void refreshData() {
        tableModel.fireTableDataChanged();
    } // --- Fin del metodo: refreshData ---


    // Modelo interno de la tabla con columnas: Imagen, Código, Piezas, LVL, PVP, Notas
    private class PdfDetailsTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;
        private final String[] columnNames = {"Imagen", "Código", "Piezas", "LVL", "PVP", "Notas"};

        @Override
        public int getRowCount() {
            return currentItems.size();
        } // --- Fin del metodo: getRowCount ---


        @Override
        public int getColumnCount() {
            return columnNames.length;
        } // --- Fin del metodo: getColumnCount ---


        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        } // --- Fin del metodo: getColumnName ---


        // Retorna Integer para Piezas (columna 2) y String para el resto, para alineación y edición correctas
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 2) {
                return Integer.class;
            }
            return String.class;
        } // --- Fin del metodo: getColumnClass ---


        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= currentItems.size()) return null;
            ExportItem item = currentItems.get(rowIndex);
            switch (columnIndex) {
                case 0: return item.getRutaImagen().getFileName().toString();
                case 1: return item.getCodigoCatalogo() != null ? item.getCodigoCatalogo() : "";
                case 2: return item.getPiezas() > 0 ? item.getPiezas() : 0;
                case 3: return item.getLvl() != null ? item.getLvl() : "";
                case 4: return item.getPvp() != null ? item.getPvp() : "";
                case 5: return item.getNotas() != null ? item.getNotas() : "";
                default: return null;
            }
        } // --- Fin del metodo: getValueAt ---


        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex >= 1;
        } // --- Fin del metodo: isCellEditable ---


        // Aplica el valor editado al item y notifica al listener de cambios
        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= currentItems.size()) return;
            ExportItem item = currentItems.get(rowIndex);

            boolean changed = false;

            switch (columnIndex) {
                case 1:
                    item.setCodigoCatalogo(aValue != null ? aValue.toString() : "");
                    changed = true;
                    break;
                case 2:
                    try {
                        item.setPiezas(aValue != null ? Integer.parseInt(aValue.toString()) : 0);
                        changed = true;
                    } catch (NumberFormatException e) { /* ignore */ }
                    break;
                case 3:
                    item.setLvl(aValue != null ? aValue.toString() : "");
                    changed = true;
                    break;
                case 4:
                    item.setPvp(aValue != null ? aValue.toString() : "");
                    changed = true;
                    break;
                case 5:
                    item.setNotas(aValue != null ? aValue.toString() : "");
                    changed = true;
                    break;
            }

            if (changed) {
                fireTableCellUpdated(rowIndex, columnIndex);
                if (onDataChangedListener != null) {
                    onDataChangedListener.run();
                }
            }
        } // --- Fin del metodo: setValueAt ---

    } // --- Fin de la Clase PdfDetailsTableModel ---

} // --- Fin de la Clase PdfDetailsTablePanel ---
