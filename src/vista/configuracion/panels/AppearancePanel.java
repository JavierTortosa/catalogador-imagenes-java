package vista.configuracion.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.configuracion.ConfigurationPanel;
import vista.dialogos.ThemeCustomizerDialog;
import vista.theme.ThemeManager;
import vista.theme.ThemeManager.ThemeCategory;
import vista.theme.ThemeManager.ThemeInfo;

public class AppearancePanel extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(AppearancePanel.class);

    private final transient ThemeManager themeManager;
    private final transient ConfigurationManager config;
    private final transient java.awt.Frame ownerFrame;

    private final JList<ThemeListEntry> themeList = new JList<>();
    private final JCheckBox chkCheckered = new JCheckBox("Fondo a cuadros");
    private final JButton btnColor1 = new JButton();
    private final JButton btnColor2 = new JButton();
    private final JButton btnColor3 = new JButton();
    private final JButton btnColor4 = new JButton();
    private final JButton btnThemeDefault = new JButton();
    private final JButton btnCustomizeTheme = new JButton("Personalizar Tema...");

    private String initialThemeId;
    private boolean initialCheckered;
    private Color initialColor1;
    private Color initialColor2;
    private Color initialColor3;
    private Color initialColor4;

    private final Color[] pendingColors = new Color[4];

    public AppearancePanel(ConfigurationManager config, ThemeManager themeManager, java.awt.Frame ownerFrame) {
        this.config = config;
        this.themeManager = themeManager;
        this.ownerFrame = ownerFrame;
        setLayout(new BorderLayout());
        setBorder(new TitledBorder("Apariencia"));
        initComponents();
        load(config);
    }

    private void initComponents() {
        // --- Theme selector ---
        JPanel themePanel = new JPanel(new BorderLayout());
        themePanel.setBorder(new TitledBorder("Tema"));

        themeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        themeList.setCellRenderer(new ThemeListRenderer());
        themeList.addListSelectionListener(onThemeSelected());

        JScrollPane themeScroll = new JScrollPane(themeList);
        themePanel.add(themeScroll, BorderLayout.CENTER);
        themePanel.add(btnCustomizeTheme, BorderLayout.SOUTH);

        btnCustomizeTheme.addActionListener(e -> {
            ThemeCustomizerDialog dialog = new ThemeCustomizerDialog(
                (javax.swing.JFrame) ownerFrame, themeManager);
            dialog.setVisible(true);
        });

        // --- Background colors ---
        JPanel bgPanel = new JPanel(new GridBagLayout());
        bgPanel.setBorder(new TitledBorder("Color de Fondo"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0; g.gridwidth = 4;
        bgPanel.add(chkCheckered, g);

        g.gridwidth = 1; g.gridy = 1; g.gridx = 0;
        bgPanel.add(new JLabel("Color 1:"), g);
        g.gridx = 1;
        bgPanel.add(makeColorButton(btnColor1, 0), g);
        g.gridx = 2;
        bgPanel.add(new JLabel("Color 2:"), g);
        g.gridx = 3;
        bgPanel.add(makeColorButton(btnColor2, 1), g);

        g.gridy = 2; g.gridx = 0;
        bgPanel.add(new JLabel("Color 3:"), g);
        g.gridx = 1;
        bgPanel.add(makeColorButton(btnColor3, 2), g);
        g.gridx = 2;
        bgPanel.add(new JLabel("Color 4:"), g);
        g.gridx = 3;
        bgPanel.add(makeColorButton(btnColor4, 3), g);

        g.gridy = 3; g.gridx = 0; g.gridwidth = 4;
        bgPanel.add(new JLabel("Color por defecto del tema:"), g);
        g.gridy = 4;
        btnThemeDefault.setEnabled(false);
        btnThemeDefault.setPreferredSize(new java.awt.Dimension(50, 24));
        bgPanel.add(btnThemeDefault, g);

        // Ensamblaje
        add(themePanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.add(bgPanel);
        rightPanel.add(new JPanel()); // spacer
        add(rightPanel, BorderLayout.EAST);
    }

    private JButton makeColorButton(JButton btn, int index) {
        btn.setPreferredSize(new java.awt.Dimension(50, 24));
        btn.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(this, "Seleccionar Color", btn.getBackground());
            if (chosen != null) {
                btn.setBackground(chosen);
                pendingColors[index] = chosen;
            }
        });
        return btn;
    }

    private ListSelectionListener onThemeSelected() {
        return e -> {
            if (e.getValueIsAdjusting()) return;
            ThemeListEntry entry = themeList.getSelectedValue();
            if (entry != null && themeManager != null) {
                themeManager.setTemaActual(entry.themeId(), true);
            }
        };
    }

    @Override
    public void load(ConfigurationManager config) {
        // Populate theme list
        ThemeListModel model = new ThemeListModel();
        Map<String, ThemeInfo> themes = themeManager.getAvailableThemes();
        ThemeCategory[] categories = {
            ThemeCategory.LIGHT, ThemeCategory.DARK,
            ThemeCategory.GRADIENT, ThemeCategory.CUSTOM_INTERNAL, ThemeCategory.CUSTOM
        };
        for (ThemeCategory cat : categories) {
            boolean firstInCat = true;
            for (Map.Entry<String, ThemeInfo> e : themes.entrySet()) {
                if (e.getValue().category() == cat) {
                    if (firstInCat) {
                        model.addCategory(cat.getDisplayName());
                        firstInCat = false;
                    }
                    model.addTheme(e.getKey(), e.getValue().nombreDisplay());
                }
            }
        }
        themeList.setModel(model);

        initialThemeId = config.getString(ConfigKeys.TEMA_NOMBRE, "");
        selectThemeInList(initialThemeId);

        initialCheckered = config.getBoolean("interfaz.menu.vista.fondo_a_cuadros.seleccionado", false);
        chkCheckered.setSelected(initialCheckered);

        initialColor1 = config.getColor(ConfigKeys.BACKGROUND_CUSTOM_COLOR_1, Color.GRAY);
        initialColor2 = config.getColor(ConfigKeys.BACKGROUND_CUSTOM_COLOR_2, Color.GRAY);
        initialColor3 = config.getColor(ConfigKeys.BACKGROUND_CUSTOM_COLOR_3, Color.GRAY);
        initialColor4 = config.getColor(ConfigKeys.BACKGROUND_CUSTOM_COLOR_4, Color.GRAY);

        btnColor1.setBackground(initialColor1);
        btnColor2.setBackground(initialColor2);
        btnColor3.setBackground(initialColor3);
        btnColor4.setBackground(initialColor4);
        btnThemeDefault.setBackground(new JLabel().getBackground());

        pendingColors[0] = null;
        pendingColors[1] = null;
        pendingColors[2] = null;
        pendingColors[3] = null;
    }

    private void selectThemeInList(String themeId) {
        ThemeListModel model = (ThemeListModel) themeList.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            ThemeListEntry entry = model.getElementAt(i);
            if (entry != null && entry.themeId() != null && entry.themeId().equals(themeId)) {
                themeList.setSelectedIndex(i);
                return;
            }
        }
    }

    @Override
    public boolean save(ConfigurationManager config) {
        boolean changed = false;
        ThemeListEntry selected = themeList.getSelectedValue();
        if (selected != null && selected.themeId() != null && !selected.themeId().equals(initialThemeId)) {
            config.setString(ConfigKeys.TEMA_NOMBRE, selected.themeId());
            initialThemeId = selected.themeId();
            changed = true;
        }
        if (chkCheckered.isSelected() != initialCheckered) {
            config.setString("interfaz.menu.vista.fondo_a_cuadros.seleccionado", String.valueOf(chkCheckered.isSelected()));
            initialCheckered = chkCheckered.isSelected();
            changed = true;
        }
        changed |= saveColor(config, ConfigKeys.BACKGROUND_CUSTOM_COLOR_1, pendingColors[0], initialColor1, btnColor1);
        changed |= saveColor(config, ConfigKeys.BACKGROUND_CUSTOM_COLOR_2, pendingColors[1], initialColor2, btnColor2);
        changed |= saveColor(config, ConfigKeys.BACKGROUND_CUSTOM_COLOR_3, pendingColors[2], initialColor3, btnColor3);
        changed |= saveColor(config, ConfigKeys.BACKGROUND_CUSTOM_COLOR_4, pendingColors[3], initialColor4, btnColor4);
        return changed;
    }

    private boolean saveColor(ConfigurationManager cfg, String key, Color pending, Color initial, JButton btn) {
        Color effective = (pending != null) ? pending : btn.getBackground();
        if (!effective.equals(initial)) {
            cfg.setColor(key, effective);
            return true;
        }
        return false;
    }

    @Override
    public boolean isModified() {
        ThemeListEntry selected = themeList.getSelectedValue();
        boolean themeChanged = selected != null && selected.themeId() != null && !selected.themeId().equals(initialThemeId);
        return themeChanged || chkCheckered.isSelected() != initialCheckered;
    }

    @Override
    public String getTitle() {
        return "Apariencia";
    }

    // --- Theme list model and entries ---

    private record ThemeListEntry(String themeId, String displayName, boolean isCategory) {
        @Override
        public String toString() {
            return isCategory ? displayName : displayName;
        }
    }

    private static class ThemeListModel extends javax.swing.AbstractListModel<ThemeListEntry> {
        private static final long serialVersionUID = 1L;
		private final java.util.List<ThemeListEntry> entries = new java.util.ArrayList<>();

        void addCategory(String name) {
            entries.add(new ThemeListEntry(null, name, true));
        }

        void addTheme(String id, String name) {
            entries.add(new ThemeListEntry(id, name, false));
        }

        @Override
        public int getSize() {
            return entries.size();
        }

        @Override
        public ThemeListEntry getElementAt(int index) {
            return entries.get(index);
        }
    }

    private static class ThemeListRenderer extends javax.swing.DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

		@Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                       int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ThemeListEntry entry) {
                if (entry.isCategory()) {
                    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
                    label.setBorder(BorderFactory.createEmptyBorder(4, 0, 2, 0));
                } else {
                    label.setFont(label.getFont().deriveFont(java.awt.Font.PLAIN));
                    label.setBorder(BorderFactory.createEmptyBorder(1, 16, 1, 0));
                }
            }
            return label;
        }
    }

}
