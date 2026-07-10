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
    private final JTextField txtThemesPath = new JTextField();
    private final JButton btnBrowseThemes = new JButton("...");

    private String initialThemesPath;

    public PathsPanel(ConfigurationManager config) {
        setLayout(new GridBagLayout());
        setBorder(new TitledBorder("Rutas y Archivos"));
        initComponents();
        load(config);
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // --- Temas Personalizados ---
        JPanel themesPanel = new JPanel(new GridBagLayout());
        themesPanel.setBorder(new TitledBorder("Temas Personalizados"));

        GridBagConstraints g = new GridBagConstraints();
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

        btnBrowseThemes.addActionListener(e -> browseFolder(txtThemesPath, "Seleccionar carpeta de temas personalizados"));

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; gbc.weighty = 1.0;
        add(themesPanel, gbc);
    }

    private void browseFolder(JTextField target, String title) {
        JFileChooser chooser = new JFileChooser(target.getText().isEmpty() ? "." : target.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(title);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            target.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    @Override
    public void load(ConfigurationManager config) {
        initialThemesPath = config.getString(ConfigKeys.TEMA_CARPETA_PERSONALIZADOS, ".temas_personalizados");
        txtThemesPath.setText(initialThemesPath);
    }

    @Override
    public boolean save(ConfigurationManager config) {
        boolean changed = false;
        String themesVal = txtThemesPath.getText().trim();
        if (!themesVal.equals(initialThemesPath)) {
            config.setString(ConfigKeys.TEMA_CARPETA_PERSONALIZADOS, themesVal);
            initialThemesPath = themesVal;
            changed = true;
        }
        return changed;
    }

    @Override
    public boolean isModified() {
        return !txtThemesPath.getText().trim().equals(initialThemesPath);
    }

    @Override
    public String getTitle() {
        return "Rutas";
    }

} // --- Fin del metodo/clase PathsPanel ---
