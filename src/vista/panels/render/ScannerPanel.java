package vista.panels.render;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import servicios.renderer.ScannerFolderResult;

/**
 * Vista del Scanner de Huérfanos: permite elegir una carpeta, escanear archivos
 * 3D/comprimidos sin representación y revisar las carpetas con trabajo pendiente
 * (pendientes, con preview, % huérfanos y tamaño) antes de enviarlas al pipeline
 * de render.
 */
public class ScannerPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextField carpetaField;
    private final JCheckBox chkSubcarpetas;
    private final JButton btnEscanear;
    private final JButton btnProcesarSeleccion;
    private final JLabel lblInfo;
    private final ScannerTableModel tableModel;
    private final JTable table;
    private final JButton btnSeleccionarTodo;
    private final List<ScannerFolderResult> resultados = new ArrayList<>();

    private Runnable onEscanear;
    private Runnable onProcesarSeleccion;

    private static final Color COLOR_TABLE_BG = UIManager.getColor("Table.background") != null
            ? UIManager.getColor("Table.background") : new Color(35, 35, 40);
    private static final Color COLOR_TABLE_FG = UIManager.getColor("Table.foreground") != null
            ? UIManager.getColor("Table.foreground") : new Color(230, 230, 235);

    public ScannerPanel() {
        super(new BorderLayout(8, 8));
        setBackground(COLOR_TABLE_BG);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        carpetaField = new JTextField(28);
        chkSubcarpetas = new JCheckBox("Incluir subcarpetas");
        btnEscanear = new JButton("Escanear");
        btnProcesarSeleccion = new JButton("Abrir selección");
        btnSeleccionarTodo = new JButton("Seleccionar todo");
        btnSeleccionarTodo.setToolTipText("Marcar o desmarcar todas las carpetas");
        lblInfo = new JLabel(" ");

        tableModel = new ScannerTableModel();
        table = new JTable(tableModel);
        table.setRowSorter(new ScannerRowSorter(tableModel));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.setBackground(COLOR_TABLE_BG);
        table.setForeground(COLOR_TABLE_FG);
        table.getTableHeader().setReorderingAllowed(false);
        configureRenderers();
        tableModel.addTableModelListener(e -> actualizarEstadoBotonSeleccion());

        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBackground(COLOR_TABLE_BG);
        JLabel lblCarpeta = new JLabel("Carpeta inicial:");
        lblCarpeta.setForeground(COLOR_TABLE_FG);

        JButton btnExaminar = new JButton("…");
        btnExaminar.setToolTipText("Seleccionar carpeta de escaneo");
        btnExaminar.addActionListener(e -> seleccionarCarpeta());

        chkSubcarpetas.setBackground(COLOR_TABLE_BG);
        chkSubcarpetas.setForeground(COLOR_TABLE_FG);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 3, 3, 3);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        gc.gridx = 0;
        gc.gridy = 0;
        topPanel.add(lblCarpeta, gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(carpetaField, gc);

        gc.gridx = 2;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        topPanel.add(btnExaminar, gc);

        gc.gridx = 3;
        topPanel.add(chkSubcarpetas, gc);

        gc.gridx = 4;
        gc.insets = new Insets(3, 12, 3, 3);
        topPanel.add(btnEscanear, gc);

        JPanel bottomPanel = new JPanel(new BorderLayout(8, 0));
        bottomPanel.setBackground(COLOR_TABLE_BG);
        lblInfo.setForeground(COLOR_TABLE_FG);
        lblInfo.setFont(lblInfo.getFont().deriveFont(Font.PLAIN));
        bottomPanel.add(lblInfo, BorderLayout.CENTER);

        JPanel botonesSur = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        botonesSur.setBackground(COLOR_TABLE_BG);
        botonesSur.add(btnSeleccionarTodo);
        botonesSur.add(btnProcesarSeleccion);
        bottomPanel.add(botonesSur, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder(
                "Carpetas con trabajo pendiente"));

        add(topPanel, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        btnEscanear.addActionListener(e -> {
            if (onEscanear != null) onEscanear.run();
        });
        btnProcesarSeleccion.addActionListener(e -> {
            if (onProcesarSeleccion != null) onProcesarSeleccion.run();
        });
        btnSeleccionarTodo.addActionListener(e -> aplicarSeleccionTotal(!todosMarcados()));

        actualizarEstadoEscaneo();
        carpetaField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                actualizarEstadoEscaneo();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                actualizarEstadoEscaneo();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                actualizarEstadoEscaneo();
            }
        });
    } // --- Fin del metodo ScannerPanel (constructor) ---


    /**
     * Asigna el callback que se invoca al pulsar "Escanear".
     *
     * @param onEscanear acción a ejecutar
     */
    public void setOnEscanear(Runnable onEscanear) {
        this.onEscanear = onEscanear;
    } // --- Fin del metodo setOnEscanear ---


    /**
     * Asigna el callback que se invoca al pulsar "Abrir selección".
     *
     * @param onProcesarSeleccion acción a ejecutar
     */
    public void setOnProcesarSeleccion(Runnable onProcesarSeleccion) {
        this.onProcesarSeleccion = onProcesarSeleccion;
    } // --- Fin del metodo setOnProcesarSeleccion ---


    /**
     * Devuelve la carpeta inicial del campo de texto, o null si está vacía o no existe.
     *
     * @return carpeta de escaneo o null
     */
    public Path getCarpetaInicial() {
        String txt = carpetaField.getText();
        if (txt == null || txt.isBlank()) return null;
        Path p = Path.of(txt.trim());
        return Files.isDirectory(p) ? p : null;
    } // --- Fin del metodo getCarpetaInicial ---


    /**
     * Rellena el campo de carpeta inicial con una ruta dada.
     *
     * @param folder carpeta a mostrar
     */
    public void setCarpetaInicial(Path folder) {
        carpetaField.setText(folder != null ? folder.toString() : "");
        actualizarEstadoEscaneo();
    } // --- Fin del metodo setCarpetaInicial ---


    /**
     * Estado del checkbox "Incluir subcarpetas".
     *
     * @return true si el escaneo debe ser recursivo
     */
    public boolean isIncludeSubfolders() {
        return chkSubcarpetas.isSelected();
    } // --- Fin del metodo isIncludeSubfolders ---


    /**
     * Fija el estado del checkbox "Incluir subcarpetas".
     *
     * @param include true para escaneo recursivo
     */
    public void setIncludeSubfolders(boolean include) {
        chkSubcarpetas.setSelected(include);
    } // --- Fin del metodo setIncludeSubfolders ---


    /**
     * Reconstruye la tabla a partir del resultado de un escaneo. Las filas se
     * crean marcadas por defecto.
     *
     * @param foldersResult lista de resultados agrupados por carpeta
     */
    public void setResultados(List<ScannerFolderResult> foldersResult) {
        resultados.clear();
        resultados.addAll(foldersResult);

        tableModel.setRowCount(0);
        for (ScannerFolderResult r : foldersResult) {
            tableModel.addRow(new Object[] {
                Boolean.TRUE,
                carpetaCorta(r.folder()),
                r.folder().toString(),
                r.pendientes(),
                r.conPreview(),
                r.porcentajeHuerfanos(),
                r.tamanoPendientesBytes()
            });
        }
        ordenarPorHuerfanosDesc();
        actualizarInfo();
        table.repaint();
    } // --- Fin del metodo setResultados ---


    /**
     * Resultados del último escaneo (lista paralela al modelo de la tabla).
     *
     * @return lista inmutable de resultados
     */
    public List<ScannerFolderResult> getResultados() {
        return List.copyOf(resultados);
    } // --- Fin del metodo getResultados ---


    /**
     * Resultados de las filas marcadas. Convierte siempre el índice de vista al
     * de modelo ({@code convertRowIndexToModel}) para que el orden de la tabla
     * no altere qué carpeta se procesa.
     *
     * @return resultados seleccionados
     */
    public List<ScannerFolderResult> getSelectedResults() {
        List<ScannerFolderResult> selected = new ArrayList<>();
        for (int vRow = 0; vRow < table.getRowCount(); vRow++) {
            if (!Boolean.TRUE.equals(table.getValueAt(vRow, 0))) continue;
            int mRow = table.convertRowIndexToModel(vRow);
            if (mRow >= 0 && mRow < resultados.size()) {
                selected.add(resultados.get(mRow));
            }
        }
        return selected;
    } // --- Fin del metodo getSelectedResults ---


    /**
     * Número de resultados que tiene marcada su casilla de selección.
     *
     * @return cantidad de filas seleccionadas
     */
    public int getSelectedCount() {
        return getSelectedResults().size();
    } // --- Fin del metodo getSelectedCount ---


    /**
     * Añade un listener de cambios del checkbox "Incluir subcarpetas".
     *
     * @param listener listener a registrar
     */
    public void addSubcarpetasChangeListener(ChangeListener listener) {
        chkSubcarpetas.addChangeListener(listener);
    } // --- Fin del metodo addSubcarpetasChangeListener ---


    /**
     * Añade un listener de cambios de selección en la tabla (para actualizar el
     * contador de filas marcadas).
     *
     * @param listener listener a registrar
     */
    public void addTableSelectionListener(TableModelListener listener) {
        tableModel.addTableModelListener(listener);
    } // --- Fin del metodo addTableSelectionListener ---


    /**
     * Actualiza la línea de resumen inferior (carpetas, pendientes y marcadas).
     */
    public void refrescarResumen() {
        actualizarInfo();
    } // --- Fin del metodo refrescarResumen ---


    /**
     * Aplica el estado de selección a todas las filas de la tabla (marcar o
     * desmarcar todas).
     *
     * @param seleccionar true para marcar todas, false para desmarcarlas
     */
    private void aplicarSeleccionTotal(boolean seleccionar) {
        for (int mRow = 0; mRow < tableModel.getRowCount(); mRow++) {
            tableModel.setValueAt(Boolean.valueOf(seleccionar), mRow, 0);
        }
        table.repaint();
        actualizarEstadoBotonSeleccion();
        actualizarInfo();
    } // --- Fin del metodo aplicarSeleccionTotal ---


    /**
     * Indica si todas las filas de la tabla están marcadas.
     *
     * @return true si todas las filas están marcadas (y hay alguna)
     */
    private boolean todosMarcados() {
        int total = table.getRowCount();
        if (total == 0) return false;
        for (int vRow = 0; vRow < total; vRow++) {
            if (!Boolean.TRUE.equals(table.getValueAt(vRow, 0))) return false;
        }
        return true;
    } // --- Fin del metodo todosMarcados ---


    /**
     * Actualiza el texto y estado del botón "Seleccionar todo": muestra
     * "Seleccionar todo" cuando no todas las filas están marcadas y
     * "Deseleccionar todo" cuando todas lo están.
     */
    private void actualizarEstadoBotonSeleccion() {
        int total = table.getRowCount();
        if (total == 0) {
            btnSeleccionarTodo.setEnabled(false);
            btnSeleccionarTodo.setText("Seleccionar todo");
            return;
        }
        btnSeleccionarTodo.setEnabled(true);
        boolean todos = todosMarcados();
        btnSeleccionarTodo.setText(todos ? "Deseleccionar todo" : "Seleccionar todo");
    } // --- Fin del metodo actualizarEstadoBotonSeleccion ---


    private void seleccionarCarpeta() {
        Path inicial = getCarpetaInicial();
        JFileChooser chooser = new JFileChooser(inicial != null ? inicial.toFile() : null);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Seleccionar carpeta de escaneo");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            carpetaField.setText(chooser.getSelectedFile().getAbsolutePath());
            actualizarEstadoEscaneo();
        }
    } // --- Fin del metodo seleccionarCarpeta ---


    /**
     * Habilita o deshabilita el botón "Escanear" según haya una carpeta válida
     * en el campo de texto. Sin carpeta inicial no tiene sentido escanear.
     */
    private void actualizarEstadoEscaneo() {
        btnEscanear.setEnabled(getCarpetaInicial() != null);
    } // --- Fin del metodo actualizarEstadoEscaneo ---


    private void configureRenderers() {
        table.getColumnModel().getColumn(0).setMaxWidth(40);

        DefaultTableCellRenderer porc = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void setValue(Object value) {
                if (value instanceof Number n) {
                    setText(String.format(java.util.Locale.ROOT, "%.0f%%", n.doubleValue() * 100.0));
                } else {
                    super.setValue(value);
                }
            }
        };
        porc.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(5).setCellRenderer(porc);

        DefaultTableCellRenderer tam = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void setValue(Object value) {
                if (value instanceof Number n) {
                    setText(formatearBytes(n.longValue()));
                } else {
                    super.setValue(value);
                }
            }
        };
        tam.setHorizontalAlignment(JLabel.RIGHT);
        table.getColumnModel().getColumn(6).setCellRenderer(tam);

        DefaultTableCellRenderer numCenter = new DefaultTableCellRenderer();
        numCenter.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(3).setCellRenderer(numCenter);
        table.getColumnModel().getColumn(4).setCellRenderer(numCenter);
    } // --- Fin del metodo configureRenderers ---


    private void ordenarPorHuerfanosDesc() {
        RowSorter<?> sorter = table.getRowSorter();
        if (sorter == null) return;
        sorter.setSortKeys(List.of(new RowSorter.SortKey(5, SortOrder.DESCENDING)));
    } // --- Fin del metodo ordenarPorHuerfanosDesc ---


    private void actualizarInfo() {
        int total = resultados.size();
        if (total == 0) {
            lblInfo.setText("Sin resultados. Pulsa Escanear para buscar carpetas con trabajo pendiente.");
            return;
        }
        int marcadas = getSelectedCount();
        long pendientes = resultados.stream().mapToLong(ScannerFolderResult::pendientes).sum();
        lblInfo.setText(total + " carpeta(s) | " + pendientes + " pendiente(s) | " + marcadas + " marcada(s)");
    } // --- Fin del metodo actualizarInfo ---


    private String carpetaCorta(Path folder) {
        if (folder == null) return "";
        Path name = folder.getFileName();
        if (name == null) return folder.toString();
        Path parent = folder.getParent();
        if (parent != null && parent.getFileName() != null) {
            return parent.getFileName() + "/" + name;
        }
        return name.toString();
    } // --- Fin del metodo carpetaCorta ---


    private String formatearBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(java.util.Locale.ROOT, "%.1f KB", kb).replace('.', ',');
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(java.util.Locale.ROOT, "%.1f MB", mb).replace('.', ',');
        double gb = mb / 1024.0;
        return String.format(java.util.Locale.ROOT, "%.1f GB", gb).replace('.', ',');
    } // --- Fin del metodo formatearBytes ---


    /**
     * Modelo de tabla tipado para que el TableRowSorter ordene numéricamente
     * (no lexicográficamente). Solo la columna de selección es editable.
     */
    private static class ScannerTableModel extends DefaultTableModel {

        private static final long serialVersionUID = 1L;

        private static final String[] COLUMNAS = {
            "✓", "Carpeta", "Path", "Pendientes", "Con preview", "% Huérfanos", "Tamaño"
        };
        private static final Class<?>[] TIPOS = {
            Boolean.class, String.class, String.class, Integer.class, Integer.class, Double.class, Long.class
        };

        ScannerTableModel() {
            super(COLUMNAS, 0);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return TIPOS[columnIndex];
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0;
        }
    } // --- Fin de la clase ScannerTableModel ---


    /**
     * Sorter con el ciclo de ordenación invertido respecto al estándar de
     * Swing: el primer clic ordena descendente, el segundo ascendente y el
     * tercero vuelve al orden sin clasificar.
     */
    private static class ScannerRowSorter extends TableRowSorter<DefaultTableModel> {

        private static final long serialVersionUID = 1L;

        ScannerRowSorter(DefaultTableModel model) {
            super(model);
        }

        @Override
        public void toggleSortOrder(int column) {
            if (!isSortable(column)) return;
            List<? extends SortKey> keys = getSortKeys();
            if (keys.isEmpty() || keys.get(0).getColumn() != column) {
                setSortKeys(List.of(new SortKey(column, SortOrder.DESCENDING)));
            } else if (keys.get(0).getSortOrder() == SortOrder.DESCENDING) {
                setSortKeys(List.of(new SortKey(column, SortOrder.ASCENDING)));
            } else {
                setSortKeys(List.of());
            }
        }
    } // --- Fin de la clase ScannerRowSorter ---

} // --- Fin de la clase ScannerPanel ---
