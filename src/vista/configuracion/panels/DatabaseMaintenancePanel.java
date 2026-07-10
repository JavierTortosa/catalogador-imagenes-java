package vista.configuracion.panels;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;

import controlador.DataController;
import controlador.managers.DataManager;
import controlador.utils.ComponentRegistry;
import modelo.datos.ImagenInfo;
import modelo.datos.Tag;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.configuracion.ConfigurationPanel;

public class DatabaseMaintenancePanel extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;

    private final transient DataManager dataManager;
    private final transient ComponentRegistry registry;
    private final transient ConfigurationManager config;

    private final JTextField txtDatabasePath = new JTextField();
    private final JButton btnBrowseDb = new JButton("...");
    private final JTextArea reindexResult = new JTextArea(2, 40);
    private String initialDatabasePath;
    private boolean dbChanged = false;

    public DatabaseMaintenancePanel(DataManager dataManager, ComponentRegistry registry,
                                     ConfigurationManager config) {
        this.dataManager = dataManager;
        this.registry = registry;
        this.config = config;

        setLayout(new BorderLayout(0, 10));

        // --- Sección superior: Configuración ---
        add(createConfigSection(), BorderLayout.NORTH);

        // --- Sección central: Pestañas de mantenimiento ---
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Imágenes Huérfanas", createOrphansPanel());
        tabbedPane.addTab("Etiquetas Sin Uso", createUnusedTagsPanel());
        tabbedPane.addTab("Ramas Vacías", createEmptyBranchesPanel());
        tabbedPane.addTab("Fusionar Etiquetas", createMergeTagsPanel());
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createConfigSection() {
        JPanel configPanel = new JPanel(new BorderLayout(10, 5));
        configPanel.setBorder(new TitledBorder("Configuraci\u00f3n"));

        // --- Fila superior: campo de ruta + botón ---
        JPanel pathPanel = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        pathPanel.add(new JLabel("Ruta base de datos:"), g);
        g.gridx = 1; g.weightx = 1.0;
        txtDatabasePath.setPreferredSize(new java.awt.Dimension(300, 24));
        pathPanel.add(txtDatabasePath, g);
        g.gridx = 2; g.weightx = 0;
        btnBrowseDb.setPreferredSize(new java.awt.Dimension(40, 24));
        pathPanel.add(btnBrowseDb, g);

        btnBrowseDb.addActionListener(e -> browseFolder(txtDatabasePath, "Seleccionar carpeta de base de datos"));

        g.gridx = 0; g.gridy = 1; g.gridwidth = 3; g.weightx = 1.0;
        JLabel lblDbHint = new JLabel("(vac\u00edo = ubicaci\u00f3n por defecto)");
        lblDbHint.setFont(lblDbHint.getFont().deriveFont(java.awt.Font.ITALIC, 10f));
        pathPanel.add(lblDbHint, g);

        configPanel.add(pathPanel, BorderLayout.CENTER);

        // --- Fila inferior: botón reindex + resultado inline ---
        JPanel reindexPanel = new JPanel(new BorderLayout(10, 0));
        JButton btnReindex = new JButton("Reconstruir \u00edndices");
        btnReindex.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Se reconstruir\u00e1n todos los \u00edndices de la base de datos\n" +
                    "y se compactar\u00e1 el archivo.\n\n" +
                    "\u00bfDeseas continuar?",
                    "Reconstruir \u00edndices", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                reindexResult.setText("Reconstruyendo \u00edndices...");
                String result = dataManager.reindexDatabase();
                reindexResult.setText(result);
            }
        });

        reindexPanel.add(btnReindex, BorderLayout.WEST);

        reindexResult.setEditable(false);
        reindexResult.setLineWrap(true);
        reindexResult.setWrapStyleWord(true);
        reindexResult.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 4));
        reindexPanel.add(reindexResult, BorderLayout.CENTER);

        configPanel.add(reindexPanel, BorderLayout.SOUTH);

        return configPanel;
    }

    private void browseFolder(JTextField target, String title) {
        JFileChooser chooser = new JFileChooser(target.getText().isEmpty() ? "." : target.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(title);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            target.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void notifyDbChanged() {
        dbChanged = true;
        dataManager.invalidateTagCache();

        DataController dc = registry.getBean("dataController");
        if (dc != null) {
            dc.refreshTagStructure();
        }
    }

    public boolean isDbChanged() {
        return dbChanged;
    }

    private JPanel createOrphansPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblInfo = new JLabel("<html>Busca im\u00e1genes registradas en la base de datos cuyos archivos f\u00edsicos ya no existen en el disco duro. Esto puede tardar unos segundos.</html>");
        panel.add(lblInfo, BorderLayout.NORTH);

        JTextArea resultArea = new JTextArea(5, 40);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        JButton btnScan = new JButton("Buscar y Limpiar Hu\u00e9rfanas");
        btnScan.addActionListener(e -> scanAndCleanOrphans(panel, resultArea, progressBar, btnScan));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.add(btnScan);

        JPanel bottomArea = new JPanel(new BorderLayout(0, 4));
        bottomArea.add(progressBar, BorderLayout.NORTH);
        bottomArea.add(btnPanel, BorderLayout.CENTER);
        panel.add(bottomArea, BorderLayout.PAGE_END);

        return panel;
    }

    private void scanAndCleanOrphans(JPanel parent, JTextArea resultArea,
                                      JProgressBar progressBar, JButton btnScan) {
        resultArea.setText("Escaneando im\u00e1genes...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        btnScan.setEnabled(false);

        new SwingWorker<List<Long>, Void>() {
            @Override
            protected List<Long> doInBackground() throws Exception {
                List<ImagenInfo> allImages = dataManager.getImagenDAO().getAllImagenes();
                List<Long> orphans = new ArrayList<>();
                for (ImagenInfo img : allImages) {
                    Path p = Paths.get(img.getRutaCompleta());
                    if (!Files.exists(p)) {
                        orphans.add(img.getId());
                    }
                }
                return orphans;
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                btnScan.setEnabled(true);
                if (isCancelled()) return;
                try {
                    List<Long> orphans = get();
                    if (orphans.isEmpty()) {
                        resultArea.setText("B\u00fasqueda completada. No se encontraron im\u00e1genes hu\u00e9rfanas.");
                        return;
                    }
                    resultArea.setText("B\u00fasqueda completada. Se encontraron " + orphans.size()
                        + " im\u00e1genes hu\u00e9rfanas.");
                    int confirm = JOptionPane.showConfirmDialog(parent,
                            "\u00bfDeseas eliminarlas de la base de datos?",
                            "Limpiar Hu\u00e9rfanas", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        resultArea.setText("Eliminando " + orphans.size() + " im\u00e1genes hu\u00e9rfanas...");
                        int deleted = dataManager.getImagenDAO().deleteImagenes(orphans);
                        resultArea.setText("Operaci\u00f3n completada. Se han eliminado " + deleted
                            + " im\u00e1genes hu\u00e9rfanas.");
                        notifyDbChanged();
                    }
                } catch (Exception ex) {
                    resultArea.setText("Error al buscar im\u00e1genes hu\u00e9rfanas.");
                }
            }
        }.execute();
    }

    private JPanel createUnusedTagsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblInfo = new JLabel("<html>Busca etiquetas que no est\u00e1n asociadas a ninguna imagen y que no tienen etiquetas hijas.</html>");
        panel.add(lblInfo, BorderLayout.NORTH);

        JTextArea resultArea = new JTextArea(5, 40);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        JButton btnScan = new JButton("Buscar Etiquetas Sin Uso");
        btnScan.addActionListener(e -> {
            resultArea.setText("Buscando...");
            progressBar.setVisible(true);
            btnScan.setEnabled(false);

            new SwingWorker<List<Tag>, Void>() {
                @Override
                protected List<Tag> doInBackground() throws Exception {
                    return dataManager.getTagDAO().getUnusedTags();
                }

                @Override
                protected void done() {
                    progressBar.setVisible(false);
                    btnScan.setEnabled(true);
                    try {
                        List<Tag> unused = get();
                        List<Tag> removables = new ArrayList<>();
                        int skipped = 0;
                        for (Tag t : unused) {
                            if (t.isReadOnly()) {
                                skipped++;
                            } else {
                                removables.add(t);
                            }
                        }
                        if (removables.isEmpty()) {
                            String msg = skipped > 0
                                ? "No hay etiquetas de usuario sin uso. (" + skipped + " etiqueta(s) de sistema omitida(s))"
                                : "No hay etiquetas sin uso.";
                            resultArea.setText(msg);
                        } else {
                            String msg = "Se encontraron " + removables.size() + " etiquetas sin uso.";
                            if (skipped > 0) msg += " (" + skipped + " etiqueta(s) de sistema omitida(s))";
                            resultArea.setText(msg);
                            int confirm = JOptionPane.showConfirmDialog(panel,
                                    "\u00bfDeseas eliminarlas?",
                                    "Limpiar Etiquetas", JOptionPane.YES_NO_OPTION);
                            if (confirm == JOptionPane.YES_OPTION) {
                                resultArea.setText("Eliminando...");
                                int count = 0;
                                for (Tag t : removables) {
                                    if (dataManager.getTagDAO().deleteTag(t.getId())) count++;
                                }
                                resultArea.setText("Operaci\u00f3n completada. Se han eliminado " + count + " etiquetas.");
                                notifyDbChanged();
                            }
                        }
                    } catch (Exception ex) {
                        resultArea.setText("Error al buscar etiquetas.");
                    }
                }
            }.execute();
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.add(btnScan);

        JPanel bottomArea = new JPanel(new BorderLayout(0, 4));
        bottomArea.add(progressBar, BorderLayout.NORTH);
        bottomArea.add(btnPanel, BorderLayout.CENTER);
        panel.add(bottomArea, BorderLayout.PAGE_END);

        return panel;
    }

    private JPanel createEmptyBranchesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblInfo = new JLabel("<html>Busca ramas completas de etiquetas que no tienen ninguna imagen asociada (ni la rama ni sus descendientes).</html>");
        panel.add(lblInfo, BorderLayout.NORTH);

        JTextArea resultArea = new JTextArea(8, 40);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        JButton btnScan = new JButton("Buscar Ramas Vac\u00edas");
        btnScan.addActionListener(e -> {
            resultArea.setText("Buscando...");
            progressBar.setVisible(true);
            btnScan.setEnabled(false);

            new SwingWorker<List<Tag>, Void>() {
                @Override
                protected List<Tag> doInBackground() throws Exception {
                    return dataManager.getTagDAO().getEmptyBranches();
                }

                @Override
                protected void done() {
                    progressBar.setVisible(false);
                    btnScan.setEnabled(true);
                    try {
                        List<Tag> emptyBranches = get();
                        List<Tag> removables = new ArrayList<>();
                        int skipped = 0;
                        for (Tag t : emptyBranches) {
                            if (t.isReadOnly()) {
                                skipped++;
                            } else {
                                removables.add(t);
                            }
                        }
                        if (removables.isEmpty()) {
                            String msg = skipped > 0
                                ? "No hay ramas de usuario vac\u00edas. (" + skipped + " rama(s) de sistema omitida(s))"
                                : "No hay ramas vac\u00edas.";
                            resultArea.setText(msg);
                            return;
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("Se encontraron ").append(removables.size()).append(" rama(s) vac\u00eda(s):\n\n");
                        for (Tag t : removables) {
                            String path = dataManager.getTagDAO().getTagFullPath(t.getId()).replace(" > ", ".");
                            sb.append("  \u2022 ").append(path).append("\n");
                        }
                        if (skipped > 0) sb.append("\n(").append(skipped).append(" rama(s) de sistema omitida(s))");
                        resultArea.setText(sb.toString());

                        int confirm = JOptionPane.showConfirmDialog(panel,
                                "\u00bfDeseas eliminar TODAS estas ramas vac\u00edas?",
                                "Eliminar Ramas Vac\u00edas", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (confirm == JOptionPane.YES_OPTION) {
                            resultArea.setText("Eliminando...");
                            int count = 0;
                            for (Tag t : removables) {
                                if (dataManager.getTagDAO().deleteTagBranch(t.getId())) count++;
                            }
                            resultArea.setText("Operaci\u00f3n completada. Se han eliminado " + count + " rama(s) vac\u00eda(s).");
                            notifyDbChanged();
                        }
                    } catch (Exception ex) {
                        resultArea.setText("Error al buscar ramas vac\u00edas.");
                    }
                }
            }.execute();
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.add(btnScan);

        JPanel bottomArea = new JPanel(new BorderLayout(0, 4));
        bottomArea.add(progressBar, BorderLayout.NORTH);
        bottomArea.add(btnPanel, BorderLayout.CENTER);
        panel.add(bottomArea, BorderLayout.PAGE_END);

        return panel;
    }

    private JPanel createMergeTagsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblInfo = new JLabel("<html>Transfiere todas las im\u00e1genes de una etiqueta a otra, y luego elimina la etiqueta original.</html>");
        panel.add(lblInfo, BorderLayout.NORTH);

        JPanel comboPanel = new JPanel(new java.awt.GridLayout(2, 2, 10, 10));
        List<Tag> allTags = dataManager.getAllTags();

        JComboBox<TagComboItem> comboSource = new JComboBox<>();
        JComboBox<TagComboItem> comboTarget = new JComboBox<>();

        for (Tag t : allTags) {
            TagComboItem item = new TagComboItem(t);
            if (!t.isReadOnly()) {
                comboSource.addItem(item);
            }
            comboTarget.addItem(item);
        }

        comboPanel.add(new JLabel("Etiqueta a eliminar (Origen):"));
        comboPanel.add(comboSource);
        comboPanel.add(new JLabel("Etiqueta destino:"));
        comboPanel.add(comboTarget);

        panel.add(comboPanel, BorderLayout.CENTER);

        JTextArea resultArea = new JTextArea(2, 40);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        JPanel bottomArea = new JPanel(new BorderLayout(0, 4));
        bottomArea.add(resultArea, BorderLayout.CENTER);
        bottomArea.add(progressBar, BorderLayout.SOUTH);
        panel.add(bottomArea, BorderLayout.SOUTH);

        JButton btnMerge = new JButton("Fusionar");
        btnMerge.addActionListener(e -> {
            TagComboItem source = (TagComboItem) comboSource.getSelectedItem();
            TagComboItem target = (TagComboItem) comboTarget.getSelectedItem();

            if (source == null || target == null) return;
            if (source.tag.getId() == target.tag.getId()) {
                resultArea.setText("Debes seleccionar dos etiquetas distintas.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(panel,
                    "\u00bfEst\u00e1s seguro de que quieres mover todo de '" + source.tag.getNombre() +
                    "' a '" + target.tag.getNombre() + "'?\nLa etiqueta origen ser\u00e1 eliminada.",
                    "Confirmar Fusi\u00f3n", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                resultArea.setText("Fusionando...");
                progressBar.setVisible(true);
                btnMerge.setEnabled(false);
                TagComboItem sourceCopy = source;

                new SwingWorker<Boolean, Void>() {
                    @Override
                    protected Boolean doInBackground() throws Exception {
                        return dataManager.getTagDAO().mergeTags(sourceCopy.tag.getId(), target.tag.getId());
                    }

                    @Override
                    protected void done() {
                        progressBar.setVisible(false);
                        btnMerge.setEnabled(true);
                        try {
                            boolean ok = get();
                            if (ok) {
                                resultArea.setText("Fusi\u00f3n completada con \u00e9xito.");
                                comboSource.removeItem(sourceCopy);
                                comboTarget.removeItem(sourceCopy);
                                notifyDbChanged();
                            } else {
                                resultArea.setText("Hubo un error al fusionar las etiquetas.");
                            }
                        } catch (Exception ex) {
                            resultArea.setText("Error al fusionar etiquetas.");
                        }
                    }
                }.execute();
            }
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.add(btnMerge);
        panel.add(btnPanel, BorderLayout.NORTH);

        return panel;
    }

    // --- ConfigurationPanel ---

    @Override
    public void load(ConfigurationManager config) {
        initialDatabasePath = config.getString(ConfigKeys.DATABASE_PATH, "");
        txtDatabasePath.setText(initialDatabasePath);
    }

    @Override
    public boolean save(ConfigurationManager config) {
        boolean changed = false;
        String dbVal = txtDatabasePath.getText().trim();
        if (!dbVal.equals(initialDatabasePath)) {
            config.setString(ConfigKeys.DATABASE_PATH, dbVal);
            initialDatabasePath = dbVal;
            changed = true;
        }
        if (dbChanged) changed = true;
        return changed;
    }

    @Override
    public boolean isModified() {
        return dbChanged || !txtDatabasePath.getText().trim().equals(initialDatabasePath);
    }

    @Override
    public String getTitle() {
        return "Base de Datos";
    }

    private static class TagComboItem {
        Tag tag;
        TagComboItem(Tag tag) { this.tag = tag; }
        @Override
        public String toString() { return tag.getNombre(); }
    }

} // --- FIN de la clase DatabaseMaintenancePanel ---
