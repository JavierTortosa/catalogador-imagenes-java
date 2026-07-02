package vista.configuracion.panels;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import servicios.ConfigurationManager;
import vista.config.ToolbarButtonDefinition;
import vista.config.ToolbarDefinition;
import vista.config.UIDefinitionService;
import vista.configuracion.ConfigurationPanel;

public class ToolbarVisibilityPanel extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;
	private final transient UIDefinitionService uiDefService = new UIDefinitionService();

    private final List<ToolbarCheckGroup> toolbarGroups = new ArrayList<>();

    public ToolbarVisibilityPanel(ConfigurationManager config) {
        setLayout(new BorderLayout());
        setBorder(new TitledBorder("Barras de Herramientas"));
        initComponents();
    }

    private void initComponents() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        List<ToolbarDefinition> toolbars = uiDefService.generateModularToolbarStructure();
        for (ToolbarDefinition tbDef : toolbars) {
            ToolbarCheckGroup group = new ToolbarCheckGroup(tbDef);
            toolbarGroups.add(group);
            contentPanel.add(group.getPanel());
        }

        JScrollPane scroll = new JScrollPane(contentPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    @Override
    public void load(ConfigurationManager config) {
        for (ToolbarCheckGroup group : toolbarGroups) {
            group.load(config);
        }
    }

    @Override
    public boolean save(ConfigurationManager config) {
        boolean changed = false;
        for (ToolbarCheckGroup group : toolbarGroups) {
            if (group.save(config)) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean isModified() {
        for (ToolbarCheckGroup group : toolbarGroups) {
            if (group.isModified()) return true;
        }
        return false;
    }

    @Override
    public String getTitle() {
        return "Barras de Herramientas";
    }

    // --- Internal class for a toolbar + its buttons ---

    private static class ToolbarCheckGroup {
        private final String toolbarId;
        private final JCheckBox chkToolbar;
        private final List<JCheckBox> chkButtons = new ArrayList<>();
        private final List<String> buttonKeys = new ArrayList<>();

        private boolean initialToolbarVisible;
        private final List<Boolean> initialButtonVisible = new ArrayList<>();

        ToolbarCheckGroup(ToolbarDefinition tbDef) {
            this.toolbarId = tbDef.claveBarra();
            this.chkToolbar = new JCheckBox(tbDef.titulo() + "  [" + tbDef.claveBarra() + "]");

            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(""),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)
            ));

            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(1, 4, 1, 4);
            g.fill = GridBagConstraints.HORIZONTAL;
            g.anchor = GridBagConstraints.WEST;
            g.weightx = 1.0;

            g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
            panel.add(chkToolbar, g);

            int row = 1;
            for (var comp : tbDef.componentes()) {
                if (comp instanceof ToolbarButtonDefinition btnDef) {
                    String btnKey = btnDef.comandoCanonico();
                    String label = btnDef.textoTooltip() != null ? btnDef.textoTooltip() : btnKey;
                    JCheckBox chk = new JCheckBox(label);

                    String cleanedKey = btnKey
                        .replace("cmd.", "")
                        .replace("toggle.", "")
                        .replace(".", "_");
                    String fullKey = "interfaz.boton." + toolbarId + "." + cleanedKey + ".visible";

                    buttonKeys.add(fullKey);
                    chkButtons.add(chk);

                    g.gridx = 0; g.gridy = row; g.gridwidth = 2;
                    g.insets = new Insets(1, 20, 1, 4);
                    panel.add(chk, g);
                    row++;
                }
            }

            this.panel = panel;
        }

        private final JPanel panel;

        JPanel getPanel() {
            return panel;
        }

        void load(ConfigurationManager config) {
            String toolbarKey = "interfaz.herramientas." + toolbarId + ".visible";
            initialToolbarVisible = config.getBoolean(toolbarKey, true);
            chkToolbar.setSelected(initialToolbarVisible);

            initialButtonVisible.clear();
            for (int i = 0; i < buttonKeys.size(); i++) {
                boolean vis = config.getBoolean(buttonKeys.get(i), true);
                initialButtonVisible.add(vis);
                chkButtons.get(i).setSelected(vis);
            }
        }

        boolean save(ConfigurationManager config) {
            boolean changed = false;
            String toolbarKey = "interfaz.herramientas." + toolbarId + ".visible";
            if (chkToolbar.isSelected() != initialToolbarVisible) {
                config.setString(toolbarKey, String.valueOf(chkToolbar.isSelected()));
                initialToolbarVisible = chkToolbar.isSelected();
                changed = true;
            }
            for (int i = 0; i < buttonKeys.size(); i++) {
                if (chkButtons.get(i).isSelected() != initialButtonVisible.get(i)) {
                    config.setString(buttonKeys.get(i), String.valueOf(chkButtons.get(i).isSelected()));
                    initialButtonVisible.set(i, chkButtons.get(i).isSelected());
                    changed = true;
                }
            }
            return changed;
        }

        boolean isModified() {
            if (chkToolbar.isSelected() != initialToolbarVisible) return true;
            for (int i = 0; i < chkButtons.size(); i++) {
                if (chkButtons.get(i).isSelected() != initialButtonVisible.get(i)) return true;
            }
            return false;
        }
    }

}
