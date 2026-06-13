package vista.configuracion.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.configuracion.ConfigurationPanel;

public class ProjectPanel extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;
	private final JTextField txtBaseFolder = new JTextField(30);
    private final JTextField txtTempFile = new JTextField(20);

    private String initialBaseFolder;
    private String initialTempFile;

    public ProjectPanel(ConfigurationManager config) {
        setLayout(new GridBagLayout());
        setBorder(new TitledBorder("Proyecto"));
        initComponents();
        load(config);
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JPanel projPanel = new JPanel(new GridBagLayout());
        projPanel.setBorder(new TitledBorder("Proyecto"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0;
        projPanel.add(new JLabel("Carpeta base:"), g);
        g.gridx = 1; g.weightx = 1.0;
        projPanel.add(txtBaseFolder, g);
        g.gridx = 2; g.weightx = 0;
        JButton btnBrowse = new JButton("Examinar...");
        btnBrowse.addActionListener(e -> browseFolder());
        projPanel.add(btnBrowse, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        projPanel.add(new JLabel("Archivo temporal:"), g);
        g.gridx = 1; g.weightx = 1.0;
        projPanel.add(txtTempFile, g);

        g.gridy = 2; g.weighty = 1.0;
        projPanel.add(new JPanel(), g);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; gbc.weighty = 1.0;
        add(projPanel, gbc);
    }

    private void browseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        String current = txtBaseFolder.getText().trim();
        if (!current.isEmpty()) {
            File f = new File(current);
            if (f.exists()) chooser.setCurrentDirectory(f);
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtBaseFolder.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    @Override
    public void load(ConfigurationManager config) {
        initialBaseFolder = config.getString(ConfigKeys.PROYECTOS_CARPETA_BASE, "");
        initialTempFile = config.getString(ConfigKeys.PROYECTOS_ARCHIVO_TEMPORAL, "seleccion_actual.prj");

        txtBaseFolder.setText(initialBaseFolder);
        txtTempFile.setText(initialTempFile);
    }

    @Override
    public boolean save(ConfigurationManager config) {
        boolean changed = false;
        if (!txtBaseFolder.getText().trim().equals(initialBaseFolder)) {
            config.setString(ConfigKeys.PROYECTOS_CARPETA_BASE, txtBaseFolder.getText().trim());
            initialBaseFolder = txtBaseFolder.getText().trim();
            changed = true;
        }
        if (!txtTempFile.getText().trim().equals(initialTempFile)) {
            config.setString(ConfigKeys.PROYECTOS_ARCHIVO_TEMPORAL, txtTempFile.getText().trim());
            initialTempFile = txtTempFile.getText().trim();
            changed = true;
        }
        return changed;
    }

    @Override
    public boolean isModified() {
        return !txtBaseFolder.getText().trim().equals(initialBaseFolder)
            || !txtTempFile.getText().trim().equals(initialTempFile);
    }

    @Override
    public String getTitle() {
        return "Proyecto";
    }

}
