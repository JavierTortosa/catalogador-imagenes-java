package vista.configuracion.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.configuracion.ConfigurationPanel;

public class PathsPanel extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;
    private final JTextField txtDatabasePath = new JTextField();
    private final JButton btnBrowseDb = new JButton("...");
    private final JTextField txtThemesPath = new JTextField();
    private final JButton btnBrowseThemes = new JButton("...");

    private String initialDatabasePath;
    private String initialThemesPath;

    /**
     * Constructor: inicializa el layout, los componentes y carga la configuración actual.
     */
    public PathsPanel(ConfigurationManager config) {
        setLayout(new GridBagLayout());
        setBorder(new TitledBorder("Rutas y Archivos"));
        initComponents();
        load(config);
    }


    /**
     * Construye los subpaneles de Base de Datos y Temas Personalizados con sus campos y botones.
     */
    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // --- Base de Datos ---
        JPanel dbPanel = new JPanel(new GridBagLayout());
        dbPanel.setBorder(new TitledBorder("Base de Datos"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        dbPanel.add(new JLabel("Ruta base de datos:"), g);
        g.gridx = 1; g.weightx = 1.0;
        txtDatabasePath.setPreferredSize(new java.awt.Dimension(300, 24));
        dbPanel.add(txtDatabasePath, g);
        g.gridx = 2; g.weightx = 0;
        btnBrowseDb.setPreferredSize(new java.awt.Dimension(40, 24));
        dbPanel.add(btnBrowseDb, g);

        g.gridx = 0; g.gridy = 1; g.gridwidth = 3;
        JLabel lblDbHint = new JLabel("(vacío = ubicación por defecto)");
        lblDbHint.setFont(lblDbHint.getFont().deriveFont(java.awt.Font.ITALIC, 10f));
        dbPanel.add(lblDbHint, g);

        // --- Temas Personalizados ---
        JPanel themesPanel = new JPanel(new GridBagLayout());
        themesPanel.setBorder(new TitledBorder("Temas Personalizados"));

        g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        themesPanel.add(new JLabel("Carpeta de temas:"), g);
        g.gridx = 1; g.weightx = 1.0;
        txtThemesPath.setPreferredSize(new java.awt.Dimension(300, 24));
        themesPanel.add(txtThemesPath, g);
        g.gridx = 2; g.weightx = 0;
        btnBrowseThemes.setPreferredSize(new java.awt.Dimension(40, 24));
        themesPanel.add(btnBrowseThemes, g);

        // --- Browse buttons ---
        btnBrowseDb.addActionListener(e -> browseFolder(txtDatabasePath, "Seleccionar carpeta de base de datos"));
        btnBrowseThemes.addActionListener(e -> browseFolder(txtThemesPath, "Seleccionar carpeta de temas personalizados"));

        // Ensamblaje
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; gbc.weighty = 0;
        add(dbPanel, gbc);
        gbc.gridy = 1; gbc.weighty = 1.0;
        add(themesPanel, gbc);
    } // --- Fin del metodo/clase initComponents ---


    /**
     * Abre un selector de directorios y vuelca la ruta seleccionada en el JTextField indicado.
     */
    private void browseFolder(JTextField target, String title) {
        JFileChooser chooser = new JFileChooser(target.getText().isEmpty() ? "." : target.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(title);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            target.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    } // --- Fin del metodo/clase browseFolder ---


    /**
     * Carga los valores actuales de configuración en los campos del panel.
     */
    @Override
    public void load(ConfigurationManager config) {
        initialDatabasePath = config.getString(ConfigKeys.DATABASE_PATH, "");
        initialThemesPath = config.getString(ConfigKeys.TEMA_CARPETA_PERSONALIZADOS, ".temas_personalizados");

        txtDatabasePath.setText(initialDatabasePath);
        txtThemesPath.setText(initialThemesPath);
    } // --- Fin del metodo/clase load ---


    /**
     * Guarda los cambios de los campos en la configuración si se han modificado.
     *
     * @return true si algún valor cambió
     */
    @Override
    public boolean save(ConfigurationManager config) {
        boolean changed = false;
        String dbVal = txtDatabasePath.getText().trim();
        if (!dbVal.equals(initialDatabasePath)) {
            config.setString(ConfigKeys.DATABASE_PATH, dbVal);
            initialDatabasePath = dbVal;
            changed = true;
        }
        String themesVal = txtThemesPath.getText().trim();
        if (!themesVal.equals(initialThemesPath)) {
            config.setString(ConfigKeys.TEMA_CARPETA_PERSONALIZADOS, themesVal);
            initialThemesPath = themesVal;
            changed = true;
        }
        return changed;
    } // --- Fin del metodo/clase save ---


    /**
     * Indica si algún campo ha sido modificado respecto a los valores iniciales.
     */
    @Override
    public boolean isModified() {
        return !txtDatabasePath.getText().trim().equals(initialDatabasePath)
            || !txtThemesPath.getText().trim().equals(initialThemesPath);
    } // --- Fin del metodo/clase isModified ---


    /**
     * Devuelve el título del panel usado como clave en el CardLayout y en el árbol.
     */
    @Override
    public String getTitle() {
        return "Rutas";
    } // --- Fin del metodo/clase getTitle ---


} // --- Fin del metodo/clase PathsPanel ---
