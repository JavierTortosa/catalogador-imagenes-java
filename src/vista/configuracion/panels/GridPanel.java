package vista.configuracion.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.configuracion.ConfigurationPanel;

public class GridPanel extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;
	private final JSpinner spnThumbWidth = new JSpinner(new SpinnerNumberModel(150, 20, 500, 10));
    private final JSpinner spnThumbHeight = new JSpinner(new SpinnerNumberModel(150, 20, 500, 10));
    private final JCheckBox chkShowNames = new JCheckBox("Mostrar nombres de archivo");
    private final JCheckBox chkShowState = new JCheckBox("Mostrar estado (marcado/descartado)");

    private int initialWidth;
    private int initialHeight;
    private boolean initialShowNames;
    private boolean initialShowState;

    public GridPanel(ConfigurationManager config) {
        setLayout(new GridBagLayout());
        setBorder(new TitledBorder("Grid"));
        initComponents();
        load(config);
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JPanel gridPanel = new JPanel(new GridBagLayout());
        gridPanel.setBorder(new TitledBorder("Grid"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0;
        gridPanel.add(new JLabel("Ancho thumbnail:"), g);
        g.gridx = 1;
        spnThumbWidth.setPreferredSize(new java.awt.Dimension(80, 24));
        gridPanel.add(spnThumbWidth, g);

        g.gridx = 2;
        gridPanel.add(new JLabel("Alto thumbnail:"), g);
        g.gridx = 3;
        spnThumbHeight.setPreferredSize(new java.awt.Dimension(80, 24));
        gridPanel.add(spnThumbHeight, g);

        g.gridx = 0; g.gridy = 1; g.gridwidth = 4;
        gridPanel.add(chkShowNames, g);
        g.gridy = 2;
        gridPanel.add(chkShowState, g);

        g.gridx = 0; g.gridy = 3; g.weighty = 1.0;
        gridPanel.add(new JPanel(), g);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; gbc.weighty = 1.0;
        add(gridPanel, gbc);
    }

    @Override
    public void load(ConfigurationManager config) {
        initialWidth = config.getInt(ConfigKeys.GRID_THUMBNAIL_WIDTH, 150);
        initialHeight = config.getInt(ConfigKeys.GRID_THUMBNAIL_HEIGHT, 150);
        initialShowNames = config.getBoolean(ConfigKeys.GRID_MOSTRAR_NOMBRES_STATE, true);
        initialShowState = config.getBoolean(ConfigKeys.GRID_MOSTRAR_ESTADO_STATE, true);

        spnThumbWidth.setValue(initialWidth);
        spnThumbHeight.setValue(initialHeight);
        chkShowNames.setSelected(initialShowNames);
        chkShowState.setSelected(initialShowState);
    }

    @Override
    public boolean save(ConfigurationManager config) {
        boolean changed = false;
        int w = (Integer) spnThumbWidth.getValue();
        if (w != initialWidth) {
            config.setString(ConfigKeys.GRID_THUMBNAIL_WIDTH, String.valueOf(w));
            initialWidth = w; changed = true;
        }
        int h = (Integer) spnThumbHeight.getValue();
        if (h != initialHeight) {
            config.setString(ConfigKeys.GRID_THUMBNAIL_HEIGHT, String.valueOf(h));
            initialHeight = h; changed = true;
        }
        if (chkShowNames.isSelected() != initialShowNames) {
            config.setString(ConfigKeys.GRID_MOSTRAR_NOMBRES_STATE, String.valueOf(chkShowNames.isSelected()));
            initialShowNames = chkShowNames.isSelected(); changed = true;
        }
        if (chkShowState.isSelected() != initialShowState) {
            config.setString(ConfigKeys.GRID_MOSTRAR_ESTADO_STATE, String.valueOf(chkShowState.isSelected()));
            initialShowState = chkShowState.isSelected(); changed = true;
        }
        return changed;
    }

    @Override
    public boolean isModified() {
        return (Integer) spnThumbWidth.getValue() != initialWidth
            || (Integer) spnThumbHeight.getValue() != initialHeight
            || chkShowNames.isSelected() != initialShowNames
            || chkShowState.isSelected() != initialShowState;
    }

    @Override
    public String getTitle() {
        return "Grid";
    }

}
