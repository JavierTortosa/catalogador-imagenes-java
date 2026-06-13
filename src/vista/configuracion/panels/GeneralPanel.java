package vista.configuracion.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.configuracion.ConfigurationPanel;

public class GeneralPanel extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;
	private final JTextField txtStartupFolder = new JTextField(30);
    private final JTextField txtStartupImage = new JTextField(20);

    private final DefaultListModel<String> excludeModel = new DefaultListModel<>();
    private final JList<String> listExcludeFolders = new JList<>(excludeModel);
    private final JTextField txtNewExclude = new JTextField(20);

    private final DefaultListModel<String> omitModel = new DefaultListModel<>();
    private final JList<String> listOmitDirs = new JList<>(omitModel);
    private final JTextField txtNewOmit = new JTextField(20);

    private final JCheckBox chkSubfolders = new JCheckBox("Cargar imágenes de subcarpetas");

    private String initialStartupFolder;
    private String initialStartupImage;
    private List<String> initialExcludeFolders;
    private List<String> initialOmitDirs;
    private boolean initialSubfolders;

    public GeneralPanel(ConfigurationManager config) {
        setLayout(new GridBagLayout());
        setBorder(new TitledBorder("General"));
        initComponents();
        load(config);
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;

        // --- Inicio ---
        JPanel inicioPanel = new JPanel(new GridBagLayout());
        inicioPanel.setBorder(new TitledBorder("Inicio"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0;
        inicioPanel.add(new JLabel("Carpeta inicial:"), g);
        g.gridx = 1; g.weightx = 1.0;
        inicioPanel.add(txtStartupFolder, g);
        g.gridx = 2; g.weightx = 0;
        JButton btnBrowseFolder = new JButton("Examinar...");
        btnBrowseFolder.addActionListener(e -> browseFolder());
        inicioPanel.add(btnBrowseFolder, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        inicioPanel.add(new JLabel("Imagen inicial:"), g);
        g.gridx = 1; g.weightx = 1.0;
        inicioPanel.add(txtStartupImage, g);

        // --- Indexación ---
        JPanel indexPanel = new JPanel(new GridBagLayout());
        indexPanel.setBorder(new TitledBorder("Indexación"));

        g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.fill = GridBagConstraints.BOTH;
        g.anchor = GridBagConstraints.WEST;

        // Excluir carpetas
        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        indexPanel.add(new JLabel("Excluir carpetas:"), g);
        g.gridx = 1; g.weightx = 1.0; g.weighty = 1.0;
        JScrollPane scrollExclude = new JScrollPane(listExcludeFolders);
        scrollExclude.setPreferredSize(new java.awt.Dimension(200, 80));
        listExcludeFolders.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        indexPanel.add(scrollExclude, g);

        g.gridx = 2; g.weightx = 0; g.weighty = 0; g.fill = GridBagConstraints.NONE;
        JPanel btnExcludePanel = new JPanel(new GridBagLayout());
        GridBagConstraints bg = new GridBagConstraints();
        bg.insets = new Insets(1, 2, 1, 2);
        bg.gridx = 0; bg.gridy = 0; bg.fill = GridBagConstraints.HORIZONTAL;
        JButton btnAddExclude = new JButton("Añadir");
        btnAddExclude.addActionListener(e -> addExcludeItem());
        btnExcludePanel.add(btnAddExclude, bg);
        bg.gridy = 1;
        JButton btnRemoveExclude = new JButton("Quitar");
        btnRemoveExclude.addActionListener(e -> removeSelected(listExcludeFolders, excludeModel));
        btnExcludePanel.add(btnRemoveExclude, bg);

        g.anchor = GridBagConstraints.NORTH; g.gridx = 2; g.gridy = 0; g.weighty = 1.0;
        indexPanel.add(btnExcludePanel, g);

        // New exclude item text field
        g.gridx = 1; g.gridy = 1; g.weightx = 1.0; g.weighty = 0; g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        g.gridwidth = 2;
        indexPanel.add(txtNewExclude, g);
        txtNewExclude.addActionListener(e -> addExcludeItem());

        // Omitir directorios
        g.gridx = 0; g.gridy = 2; g.weightx = 0; g.weighty = 0; g.gridwidth = 1;
        indexPanel.add(new JLabel("Omitir directorios:"), g);
        g.gridx = 1; g.weightx = 1.0; g.weighty = 1.0;
        JScrollPane scrollOmit = new JScrollPane(listOmitDirs);
        scrollOmit.setPreferredSize(new java.awt.Dimension(200, 80));
        listOmitDirs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        indexPanel.add(scrollOmit, g);

        g.gridx = 2; g.weightx = 0; g.weighty = 0; g.fill = GridBagConstraints.NONE;
        g.anchor = GridBagConstraints.NORTH;
        JPanel btnOmitPanel = new JPanel(new GridBagLayout());
        bg = new GridBagConstraints();
        bg.insets = new Insets(1, 2, 1, 2);
        bg.gridx = 0; bg.gridy = 0; bg.fill = GridBagConstraints.HORIZONTAL;
        JButton btnAddOmit = new JButton("Añadir");
        btnAddOmit.addActionListener(e -> addOmitItem());
        btnOmitPanel.add(btnAddOmit, bg);
        bg.gridy = 1;
        JButton btnRemoveOmit = new JButton("Quitar");
        btnRemoveOmit.addActionListener(e -> removeSelected(listOmitDirs, omitModel));
        btnOmitPanel.add(btnRemoveOmit, bg);
        g.gridx = 2; g.gridy = 2; g.weighty = 1.0;
        indexPanel.add(btnOmitPanel, g);

        // New omit item text field
        g.gridx = 1; g.gridy = 3; g.weightx = 1.0; g.weighty = 0; g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        g.gridwidth = 2;
        indexPanel.add(txtNewOmit, g);
        txtNewOmit.addActionListener(e -> addOmitItem());

        // --- Carga ---
        JPanel cargaPanel = new JPanel(new GridBagLayout());
        cargaPanel.setBorder(new TitledBorder("Carga de Imágenes"));

        g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = 0;
        cargaPanel.add(chkSubfolders, g);

        // Ensamblaje
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; gbc.weighty = 0;
        add(inicioPanel, gbc);
        gbc.gridy = 1; gbc.weighty = 1.0;
        add(indexPanel, gbc);
        gbc.gridy = 2; gbc.weighty = 0;
        add(cargaPanel, gbc);
    }

    private void addExcludeItem() {
        String text = txtNewExclude.getText().trim();
        if (!text.isEmpty() && !excludeModel.contains(text)) {
            excludeModel.addElement(text);
            txtNewExclude.setText("");
        }
    }

    private void addOmitItem() {
        String text = txtNewOmit.getText().trim();
        if (!text.isEmpty() && !omitModel.contains(text)) {
            omitModel.addElement(text);
            txtNewOmit.setText("");
        }
    }

    private void removeSelected(JList<String> list, DefaultListModel<String> model) {
        int idx = list.getSelectedIndex();
        if (idx != -1) {
            model.remove(idx);
        }
    }

    private void browseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        String current = txtStartupFolder.getText().trim();
        if (!current.isEmpty()) {
            File f = new File(current);
            if (f.exists()) chooser.setCurrentDirectory(f);
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtStartupFolder.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private List<String> parseCommaList(String value) {
        if (value == null || value.isBlank()) return new ArrayList<>();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String joinList(List<String> list) {
        return String.join(", ", list);
    }

    @Override
    public void load(ConfigurationManager config) {
        initialStartupFolder = config.getString(ConfigKeys.INICIO_CARPETA, "");
        initialStartupImage = config.getString(ConfigKeys.INICIO_IMAGEN, "");
        initialExcludeFolders = parseCommaList(config.getString(ConfigKeys.INDEXACION_EXCLUIR_CARPETAS, ""));
        initialOmitDirs = parseCommaList(config.getString(ConfigKeys.INDEXACION_OMITIR_DIRECTORIOS, ""));
        initialSubfolders = config.getBoolean(ConfigKeys.COMPORTAMIENTO_CARGAR_SUBCARPETAS, true);

        txtStartupFolder.setText(initialStartupFolder);
        txtStartupImage.setText(initialStartupImage);

        excludeModel.clear();
        initialExcludeFolders.forEach(excludeModel::addElement);

        omitModel.clear();
        initialOmitDirs.forEach(omitModel::addElement);

        chkSubfolders.setSelected(initialSubfolders);
    }

    @Override
    public boolean save(ConfigurationManager config) {
        boolean changed = false;
        if (!txtStartupFolder.getText().trim().equals(initialStartupFolder)) {
            config.setString(ConfigKeys.INICIO_CARPETA, txtStartupFolder.getText().trim());
            initialStartupFolder = txtStartupFolder.getText().trim();
            changed = true;
        }
        if (!txtStartupImage.getText().trim().equals(initialStartupImage)) {
            config.setString(ConfigKeys.INICIO_IMAGEN, txtStartupImage.getText().trim());
            initialStartupImage = txtStartupImage.getText().trim();
            changed = true;
        }
        List<String> currentExclude = modelToList(excludeModel);
        if (!currentExclude.equals(initialExcludeFolders)) {
            config.setString(ConfigKeys.INDEXACION_EXCLUIR_CARPETAS, joinList(currentExclude));
            initialExcludeFolders = currentExclude;
            changed = true;
        }
        List<String> currentOmit = modelToList(omitModel);
        if (!currentOmit.equals(initialOmitDirs)) {
            config.setString(ConfigKeys.INDEXACION_OMITIR_DIRECTORIOS, joinList(currentOmit));
            initialOmitDirs = currentOmit;
            changed = true;
        }
        if (chkSubfolders.isSelected() != initialSubfolders) {
            config.setString(ConfigKeys.COMPORTAMIENTO_CARGAR_SUBCARPETAS, String.valueOf(chkSubfolders.isSelected()));
            initialSubfolders = chkSubfolders.isSelected();
            changed = true;
        }
        return changed;
    }

    private List<String> modelToList(DefaultListModel<String> model) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++) {
            result.add(model.getElementAt(i));
        }
        return result;
    }

    @Override
    public boolean isModified() {
        return !txtStartupFolder.getText().trim().equals(initialStartupFolder)
            || !txtStartupImage.getText().trim().equals(initialStartupImage)
            || !modelToList(excludeModel).equals(initialExcludeFolders)
            || !modelToList(omitModel).equals(initialOmitDirs)
            || chkSubfolders.isSelected() != initialSubfolders;
    }

    @Override
    public String getTitle() {
        return "General";
    }

}
