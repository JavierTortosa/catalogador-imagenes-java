package vista.panels.export;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import modelo.proyecto.ExportItem;


public class PdfDetailsTablePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTable detailsTable;
    private PdfDetailsTableModel tableModel;
    private List<ExportItem> currentItems = new ArrayList<>();

    // Listener para avisar al controlador cuando se edita un dato
    private Runnable onDataChangedListener;

    // Listener para notificar cuando se selecciona una fila en la tabla de detalles
    private Consumer<ExportItem> onSelectionChangedListener;

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
        detailsTable.setAutoCreateRowSorter(true);

        detailsTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        detailsTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        detailsTable.getColumnModel().getColumn(2).setPreferredWidth(85);
        detailsTable.getColumnModel().getColumn(3).setPreferredWidth(85);
        detailsTable.getColumnModel().getColumn(4).setPreferredWidth(70);
        detailsTable.getColumnModel().getColumn(5).setPreferredWidth(55);
        detailsTable.getColumnModel().getColumn(6).setPreferredWidth(65);
        detailsTable.getColumnModel().getColumn(7).setPreferredWidth(85);
        detailsTable.getColumnModel().getColumn(8).setPreferredWidth(50);
        detailsTable.getColumnModel().getColumn(9).setPreferredWidth(50);
        detailsTable.getColumnModel().getColumn(10).setPreferredWidth(150);

        detailsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int viewRow = detailsTable.getSelectedRow();
                int modelRow = viewRow != -1 ? detailsTable.convertRowIndexToModel(viewRow) : -1;
                ExportItem selectedItem = (modelRow != -1 && modelRow < currentItems.size())
                        ? currentItems.get(modelRow) : null;
                if (onSelectionChangedListener != null) {
                    onSelectionChangedListener.accept(selectedItem);
                }
            }
        });

        add(new JScrollPane(detailsTable), BorderLayout.CENTER);
    } // --- Fin del metodo: initComponents ---


    // Permite inyectar el listener desde ExportPanel para notificar cambios
    public void setOnDataChangedListener(Runnable listener) {
        this.onDataChangedListener = listener;
    } // --- Fin del metodo: setOnDataChangedListener ---

    // Establece el callback que se invoca al seleccionar una fila en la tabla de detalles
    public void setOnSelectionChangedListener(Consumer<ExportItem> listener) {
        this.onSelectionChangedListener = listener;
    } // --- Fin del metodo: setOnSelectionChangedListener ---


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


    // Selecciona la fila cuya ruta de imagen coincida con la clave indicada
    public void selectRowByPath(String imagePath) {
        if (imagePath == null || imagePath.isEmpty() || currentItems.isEmpty()) {
            detailsTable.clearSelection();
            return;
        }
        String normalizedTarget = imagePath.replace("\\", "/");
        for (int i = 0; i < currentItems.size(); i++) {
            ExportItem item = currentItems.get(i);
            if (item.getRutaImagen() != null) {
                String itemPath = item.getRutaImagen().toString().replace("\\", "/");
                if (itemPath.equals(normalizedTarget)) {
                    int viewRow = detailsTable.convertRowIndexToView(i);
                    if (viewRow != -1) {
                        detailsTable.setRowSelectionInterval(viewRow, viewRow);
                        detailsTable.scrollRectToVisible(detailsTable.getCellRect(viewRow, 0, true));
                    }
                    return;
                }
            }
        }
        detailsTable.clearSelection();
    } // --- Fin del metodo: selectRowByPath ---

    // Refresca los datos visibles en la tabla sin recargar la lista
    public void refreshData() {
        tableModel.fireTableDataChanged();
    } // --- Fin del metodo: refreshData ---

    // Detiene cualquier edición en curso en la tabla para que los valores pendientes se confirmen
    public void stopEditing() {
        if (detailsTable != null && detailsTable.isEditing()) {
            detailsTable.getCellEditor().stopCellEditing();
        }
    } // --- Fin del metodo: stopEditing ---


    // Modelo interno de la tabla con columnas: Imagen, Código, Archivos C/S, S/S, Totales, Lychee, Chitubox, Tamaño Total, LVL, PVP, Notas
    private class PdfDetailsTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;
        private final String[] columnNames = {"Imagen", "Código", "Piezas C/S", "Piezas S/S", "Piezas", "Lychee", "Chitubox", "Tamaño Total", "LVL", "PVP", "Notas"};

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


        // Retorna Integer para columnas numéricas, Boolean para checkboxes y String para el resto
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 2 || columnIndex == 3 || columnIndex == 4) {
                return Integer.class;
            }
            if (columnIndex == 5 || columnIndex == 6) {
                return Boolean.class;
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
                case 2: return item.getPiezasConSoporte();
                case 3: return item.getPiezasSinSoporte();
                case 4: return item.getPiezas() > 0 ? item.getPiezas() : 0;
                case 5: return item.hasLychee();
                case 6: return item.hasChitubox();
                case 7: return String.format("%.1f MB", item.getTotalSizeMb());
                case 8: return item.getLvl() != null ? item.getLvl() : "";
                case 9: return item.getPvp() != null ? item.getPvp() : "";
                case 10: return item.getNotas() != null ? item.getNotas() : "";
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
                        item.setPiezasConSoporte(aValue != null ? Integer.parseInt(aValue.toString()) : 0);
                        changed = true;
                    } catch (NumberFormatException e) { /* ignore */ }
                    break;
                case 3:
                    try {
                        item.setPiezasSinSoporte(aValue != null ? Integer.parseInt(aValue.toString()) : 0);
                        changed = true;
                    } catch (NumberFormatException e) { /* ignore */ }
                    break;
                case 4:
                    try {
                        item.setPiezas(aValue != null ? Integer.parseInt(aValue.toString()) : 0);
                        changed = true;
                    } catch (NumberFormatException e) { /* ignore */ }
                    break;
                case 5:
                    if (aValue instanceof Boolean) {
                        item.setHasLychee((Boolean) aValue);
                        changed = true;
                    }
                    break;
                case 6:
                    if (aValue instanceof Boolean) {
                        item.setHasChitubox((Boolean) aValue);
                        changed = true;
                    }
                    break;
                case 7:
                    try {
                        item.setTotalSizeMb(Double.parseDouble(aValue.toString().replace(" MB", "").trim()));
                        changed = true;
                    } catch (NumberFormatException e) { /* ignore */ }
                    break;
                case 8:
                    item.setLvl(aValue != null ? aValue.toString() : "");
                    changed = true;
                    break;
                case 9:
                    item.setPvp(aValue != null ? aValue.toString() : "");
                    changed = true;
                    break;
                case 10:
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
