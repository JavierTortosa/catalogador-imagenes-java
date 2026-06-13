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

public class NavigationPanel extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;
	private final JCheckBox chkCircular = new JCheckBox("Navegación circular (wrap around)");
    private final JSpinner spnBlockSize = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
    private final JSpinner spnCarouselDelay = new JSpinner(new SpinnerNumberModel(3000, 500, 30000, 100));
    private final JCheckBox chkSyncVisorCarousel = new JCheckBox("Sincronizar visor con carrusel");

    private boolean initialCircular;
    private int initialBlockSize;
    private int initialCarouselDelay;
    private boolean initialSyncVisorCarousel;

    public NavigationPanel(ConfigurationManager config) {
        setLayout(new GridBagLayout());
        setBorder(new TitledBorder("Navegación"));
        initComponents();
        load(config);
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // --- Navegación ---
        JPanel navPanel = new JPanel(new GridBagLayout());
        navPanel.setBorder(new TitledBorder("Navegación"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        navPanel.add(chkCircular, g);

        g.gridx = 0; g.gridy = 1; g.gridwidth = 1; g.weightx = 0;
        navPanel.add(new JLabel("Tamaño salto bloque:"), g);
        g.gridx = 1; g.weightx = 0;
        spnBlockSize.setPreferredSize(new java.awt.Dimension(80, 24));
        navPanel.add(spnBlockSize, g);

        // --- Carrusel ---
        JPanel carPanel = new JPanel(new GridBagLayout());
        carPanel.setBorder(new TitledBorder("Carrusel"));

        g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0;
        carPanel.add(new JLabel("Delay (ms):"), g);
        g.gridx = 1;
        spnCarouselDelay.setPreferredSize(new java.awt.Dimension(100, 24));
        carPanel.add(spnCarouselDelay, g);

        g.gridx = 0; g.gridy = 1; g.gridwidth = 2;
        carPanel.add(chkSyncVisorCarousel, g);

        // Ensamblaje
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; gbc.weighty = 0;
        add(navPanel, gbc);
        gbc.gridy = 1; gbc.weighty = 1.0;
        add(carPanel, gbc);
    }

    @Override
    public void load(ConfigurationManager config) {
        initialCircular = config.getBoolean(ConfigKeys.COMPORTAMIENTO_NAVEGACION_CIRCULAR, false);
        initialBlockSize = config.getInt(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE, 10);
        initialCarouselDelay = config.getInt(ConfigKeys.CAROUSEL_DELAY_MS, 3000);
        initialSyncVisorCarousel = config.getBoolean(ConfigKeys.COMPORTAMIENTO_SYNC_VISOR_CARRUSEL, false);

        chkCircular.setSelected(initialCircular);
        spnBlockSize.setValue(initialBlockSize);
        spnCarouselDelay.setValue(initialCarouselDelay);
        chkSyncVisorCarousel.setSelected(initialSyncVisorCarousel);
    }

    @Override
    public boolean save(ConfigurationManager config) {
        boolean changed = false;
        if (chkCircular.isSelected() != initialCircular) {
            config.setString(ConfigKeys.COMPORTAMIENTO_NAVEGACION_CIRCULAR, String.valueOf(chkCircular.isSelected()));
            initialCircular = chkCircular.isSelected();
            changed = true;
        }
        int blockVal = (Integer) spnBlockSize.getValue();
        if (blockVal != initialBlockSize) {
            config.setString(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE, String.valueOf(blockVal));
            initialBlockSize = blockVal;
            changed = true;
        }
        int delayVal = (Integer) spnCarouselDelay.getValue();
        if (delayVal != initialCarouselDelay) {
            config.setString(ConfigKeys.CAROUSEL_DELAY_MS, String.valueOf(delayVal));
            initialCarouselDelay = delayVal;
            changed = true;
        }
        if (chkSyncVisorCarousel.isSelected() != initialSyncVisorCarousel) {
            config.setString(ConfigKeys.COMPORTAMIENTO_SYNC_VISOR_CARRUSEL, String.valueOf(chkSyncVisorCarousel.isSelected()));
            initialSyncVisorCarousel = chkSyncVisorCarousel.isSelected();
            changed = true;
        }
        return changed;
    }

    @Override
    public boolean isModified() {
        return chkCircular.isSelected() != initialCircular
            || (Integer) spnBlockSize.getValue() != initialBlockSize
            || (Integer) spnCarouselDelay.getValue() != initialCarouselDelay
            || chkSyncVisorCarousel.isSelected() != initialSyncVisorCarousel;
    }

    @Override
    public String getTitle() {
        return "Navegación";
    }

}
