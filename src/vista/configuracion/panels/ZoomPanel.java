package vista.configuracion.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.zoom.ZoomModeEnum;
import vista.configuracion.ConfigurationPanel;

public class ZoomPanel extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;
	private final JComboBox<String> cmbInitialMode = new JComboBox<>();
    private final JCheckBox chkManualZoom = new JCheckBox("Activar zoom manual al inicio");
    private final JCheckBox chkZoomToCursor = new JCheckBox("Zoom al cursor");
    private final JCheckBox chkMaintainProportions = new JCheckBox("Mantener proporciones");
    private final JSpinner spnCustomPercent = new JSpinner(new SpinnerNumberModel(100.0, 10.0, 1000.0, 5.0));

    private String initialMode;
    private boolean initialManualZoom;
    private boolean initialZoomToCursor;
    private boolean initialMaintainProportions;
    private double initialCustomPercent;

    public ZoomPanel(ConfigurationManager config) {
        setLayout(new GridBagLayout());
        setBorder(new TitledBorder("Zoom y Visualización"));
        initComponents();
        load(config);
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // --- Zoom General ---
        JPanel zoomPanel = new JPanel(new GridBagLayout());
        zoomPanel.setBorder(new TitledBorder("Zoom"));

        for (ZoomModeEnum mode : ZoomModeEnum.values()) {
            cmbInitialMode.addItem(mode.getNombreLegible());
        }

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0;
        zoomPanel.add(new JLabel("Modo inicial:"), g);
        g.gridx = 1; g.weightx = 1.0;
        zoomPanel.add(cmbInitialMode, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0; g.gridwidth = 2;
        zoomPanel.add(chkManualZoom, g);
        g.gridy = 2;
        zoomPanel.add(chkZoomToCursor, g);
        g.gridy = 3;
        zoomPanel.add(chkMaintainProportions, g);

        g.gridx = 0; g.gridy = 4; g.gridwidth = 1; g.weightx = 0;
        zoomPanel.add(new JLabel("Zoom personalizado %:"), g);
        g.gridx = 1; g.weightx = 0;
        spnCustomPercent.setPreferredSize(new java.awt.Dimension(100, 24));
        zoomPanel.add(spnCustomPercent, g);

        // Ensamblaje
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; gbc.weighty = 1.0;
        add(zoomPanel, gbc);
    }

    @Override
    public void load(ConfigurationManager config) {
        initialMode = config.getString(ConfigKeys.COMPORTAMIENTO_ZOOM_MODO_INICIAL, "FIT_TO_SCREEN");
        initialManualZoom = config.getBoolean(ConfigKeys.COMPORTAMIENTO_ZOOM_MANUAL_INICIAL, true);
        initialZoomToCursor = config.getBoolean(ConfigKeys.COMPORTAMIENTO_ZOOM_AL_CURSOR_ACTIVADO, true);
        initialMaintainProportions = config.getBoolean("interfaz.menu.zoom.mantener_proporciones.seleccionado", true);
        initialCustomPercent = config.getDouble(ConfigKeys.COMPORTAMIENTO_ZOOM_PORCENTAJE_PERSONALIZADO, 100.0);

        // Seleccionar modo inicial en combo
        for (int i = 0; i < cmbInitialMode.getItemCount(); i++) {
            String item = cmbInitialMode.getItemAt(i);
            if (item != null) {
                try {
                    ZoomModeEnum modeEnum = ZoomModeEnum.valueOf(initialMode);
                    if (modeEnum.getNombreLegible().equals(item)) {
                        cmbInitialMode.setSelectedIndex(i);
                        break;
                    }
                } catch (IllegalArgumentException e) {
                    cmbInitialMode.setSelectedIndex(0);
                }
            }
        }

        chkManualZoom.setSelected(initialManualZoom);
        chkZoomToCursor.setSelected(initialZoomToCursor);
        chkMaintainProportions.setSelected(initialMaintainProportions);
        spnCustomPercent.setValue(initialCustomPercent);
    }

    @Override
    public boolean save(ConfigurationManager config) {
        boolean changed = false;
        String selectedMode = getSelectedModeKey();
        if (!selectedMode.equals(initialMode)) {
            config.setString(ConfigKeys.COMPORTAMIENTO_ZOOM_MODO_INICIAL, selectedMode);
            initialMode = selectedMode;
            changed = true;
        }
        if (chkManualZoom.isSelected() != initialManualZoom) {
            config.setString(ConfigKeys.COMPORTAMIENTO_ZOOM_MANUAL_INICIAL, String.valueOf(chkManualZoom.isSelected()));
            initialManualZoom = chkManualZoom.isSelected();
            changed = true;
        }
        if (chkZoomToCursor.isSelected() != initialZoomToCursor) {
            config.setString(ConfigKeys.COMPORTAMIENTO_ZOOM_AL_CURSOR_ACTIVADO, String.valueOf(chkZoomToCursor.isSelected()));
            initialZoomToCursor = chkZoomToCursor.isSelected();
            changed = true;
        }
        if (chkMaintainProportions.isSelected() != initialMaintainProportions) {
            config.setString("interfaz.menu.zoom.mantener_proporciones.seleccionado", String.valueOf(chkMaintainProportions.isSelected()));
            initialMaintainProportions = chkMaintainProportions.isSelected();
            changed = true;
        }
        double percentVal = (Double) spnCustomPercent.getValue();
        if (Math.abs(percentVal - initialCustomPercent) > 0.01) {
            config.setString(ConfigKeys.COMPORTAMIENTO_ZOOM_PORCENTAJE_PERSONALIZADO, String.valueOf(percentVal));
            initialCustomPercent = percentVal;
            changed = true;
        }
        return changed;
    }

    private String getSelectedModeKey() {
        String selected = (String) cmbInitialMode.getSelectedItem();
        for (ZoomModeEnum mode : ZoomModeEnum.values()) {
            if (mode.getNombreLegible().equals(selected)) {
                return mode.name();
            }
        }
        return "FIT_TO_SCREEN";
    }

    @Override
    public boolean isModified() {
        return !getSelectedModeKey().equals(initialMode)
            || chkManualZoom.isSelected() != initialManualZoom
            || chkZoomToCursor.isSelected() != initialZoomToCursor
            || chkMaintainProportions.isSelected() != initialMaintainProportions
            || Math.abs((Double) spnCustomPercent.getValue() - initialCustomPercent) > 0.01;
    }

    @Override
    public String getTitle() {
        return "Zoom y Visualización";
    }

}
