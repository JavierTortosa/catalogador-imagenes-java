package vista.configuracion.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.configuracion.ConfigurationPanel;

public class ThumbnailPanel extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;
	private final JSpinner spnNormWidth = new JSpinner(new SpinnerNumberModel(40, 10, 300, 5));
    private final JSpinner spnNormHeight = new JSpinner(new SpinnerNumberModel(40, 10, 300, 5));
    private final JSpinner spnSelWidth = new JSpinner(new SpinnerNumberModel(60, 10, 300, 5));
    private final JSpinner spnSelHeight = new JSpinner(new SpinnerNumberModel(60, 10, 300, 5));
    private final JSpinner spnCountBefore = new JSpinner(new SpinnerNumberModel(9, 1, 50, 1));
    private final JSpinner spnCountAfter = new JSpinner(new SpinnerNumberModel(9, 1, 50, 1));
    private final JSpinner spnCacheSize = new JSpinner(new SpinnerNumberModel(200, 10, 1000, 10));

    private int initialNormWidth;
    private int initialNormHeight;
    private int initialSelWidth;
    private int initialSelHeight;
    private int initialCountBefore;
    private int initialCountAfter;
    private int initialCacheSize;

    public ThumbnailPanel(ConfigurationManager config) {
        setLayout(new GridBagLayout());
        setBorder(new TitledBorder("Miniaturas"));
        initComponents();
        load(config);
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // --- Tamaños ---
        JPanel sizePanel = new JPanel(new GridBagLayout());
        sizePanel.setBorder(new TitledBorder("Tamaños"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0;
        sizePanel.add(new JLabel("Normal ancho:"), g);
        g.gridx = 1;
        spnNormWidth.setPreferredSize(new java.awt.Dimension(80, 24));
        sizePanel.add(spnNormWidth, g);
        g.gridx = 2;
        sizePanel.add(new JLabel("Normal alto:"), g);
        g.gridx = 3;
        spnNormHeight.setPreferredSize(new java.awt.Dimension(80, 24));
        sizePanel.add(spnNormHeight, g);

        g.gridx = 0; g.gridy = 1;
        sizePanel.add(new JLabel("Seleccionada ancho:"), g);
        g.gridx = 1;
        spnSelWidth.setPreferredSize(new java.awt.Dimension(80, 24));
        sizePanel.add(spnSelWidth, g);
        g.gridx = 2;
        sizePanel.add(new JLabel("Seleccionada alto:"), g);
        g.gridx = 3;
        spnSelHeight.setPreferredSize(new java.awt.Dimension(80, 24));
        sizePanel.add(spnSelHeight, g);

        // --- Cantidad ---
        JPanel countPanel = new JPanel(new GridBagLayout());
        countPanel.setBorder(new TitledBorder("Cantidad"));

        g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0;
        countPanel.add(new JLabel("Miniaturas antes:"), g);
        g.gridx = 1;
        spnCountBefore.setPreferredSize(new java.awt.Dimension(80, 24));
        countPanel.add(spnCountBefore, g);
        g.gridx = 2;
        countPanel.add(new JLabel("Miniaturas después:"), g);
        g.gridx = 3;
        spnCountAfter.setPreferredSize(new java.awt.Dimension(80, 24));
        countPanel.add(spnCountAfter, g);

        // --- Caché ---
        JPanel cachePanel = new JPanel(new GridBagLayout());
        cachePanel.setBorder(new TitledBorder("Caché"));

        g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0;
        cachePanel.add(new JLabel("Tamaño máximo de caché:"), g);
        g.gridx = 1;
        spnCacheSize.setPreferredSize(new java.awt.Dimension(80, 24));
        cachePanel.add(spnCacheSize, g);

        // Ensamblaje
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; gbc.weighty = 0;
        add(sizePanel, gbc);
        gbc.gridy = 1;
        add(countPanel, gbc);
        gbc.gridy = 2; gbc.weighty = 1.0;
        add(cachePanel, gbc);
    }

    @Override
    public void load(ConfigurationManager config) {
        initialNormWidth = config.getInt(ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, 40);
        initialNormHeight = config.getInt(ConfigKeys.MINIATURAS_TAMANO_NORM_ALTO, 40);
        initialSelWidth = config.getInt(ConfigKeys.MINIATURAS_TAMANO_SEL_ANCHO, 60);
        initialSelHeight = config.getInt(ConfigKeys.MINIATURAS_TAMANO_SEL_ALTO, 60);
        initialCountBefore = config.getInt(ConfigKeys.MINIATURAS_CANTIDAD_ANTES, 9);
        initialCountAfter = config.getInt(ConfigKeys.MINIATURAS_CANTIDAD_DESPUES, 9);
        initialCacheSize = config.getInt(ConfigKeys.MINIATURAS_CACHE_MAX_SIZE, 200);

        spnNormWidth.setValue(initialNormWidth);
        spnNormHeight.setValue(initialNormHeight);
        spnSelWidth.setValue(initialSelWidth);
        spnSelHeight.setValue(initialSelHeight);
        spnCountBefore.setValue(initialCountBefore);
        spnCountAfter.setValue(initialCountAfter);
        spnCacheSize.setValue(initialCacheSize);
    }

    @Override
    public boolean save(ConfigurationManager config) {
        boolean changed = false;
        int nw = (Integer) spnNormWidth.getValue();
        if (nw != initialNormWidth) {
            config.setString(ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, String.valueOf(nw));
            initialNormWidth = nw; changed = true;
        }
        int nh = (Integer) spnNormHeight.getValue();
        if (nh != initialNormHeight) {
            config.setString(ConfigKeys.MINIATURAS_TAMANO_NORM_ALTO, String.valueOf(nh));
            initialNormHeight = nh; changed = true;
        }
        int sw = (Integer) spnSelWidth.getValue();
        if (sw != initialSelWidth) {
            config.setString(ConfigKeys.MINIATURAS_TAMANO_SEL_ANCHO, String.valueOf(sw));
            initialSelWidth = sw; changed = true;
        }
        int sh = (Integer) spnSelHeight.getValue();
        if (sh != initialSelHeight) {
            config.setString(ConfigKeys.MINIATURAS_TAMANO_SEL_ALTO, String.valueOf(sh));
            initialSelHeight = sh; changed = true;
        }
        int cb = (Integer) spnCountBefore.getValue();
        if (cb != initialCountBefore) {
            config.setString(ConfigKeys.MINIATURAS_CANTIDAD_ANTES, String.valueOf(cb));
            initialCountBefore = cb; changed = true;
        }
        int ca = (Integer) spnCountAfter.getValue();
        if (ca != initialCountAfter) {
            config.setString(ConfigKeys.MINIATURAS_CANTIDAD_DESPUES, String.valueOf(ca));
            initialCountAfter = ca; changed = true;
        }
        int cs = (Integer) spnCacheSize.getValue();
        if (cs != initialCacheSize) {
            config.setString(ConfigKeys.MINIATURAS_CACHE_MAX_SIZE, String.valueOf(cs));
            initialCacheSize = cs; changed = true;
        }
        return changed;
    }

    @Override
    public boolean isModified() {
        return (Integer) spnNormWidth.getValue() != initialNormWidth
            || (Integer) spnNormHeight.getValue() != initialNormHeight
            || (Integer) spnSelWidth.getValue() != initialSelWidth
            || (Integer) spnSelHeight.getValue() != initialSelHeight
            || (Integer) spnCountBefore.getValue() != initialCountBefore
            || (Integer) spnCountAfter.getValue() != initialCountAfter
            || (Integer) spnCacheSize.getValue() != initialCacheSize;
    }

    @Override
    public String getTitle() {
        return "Miniaturas";
    }

}
