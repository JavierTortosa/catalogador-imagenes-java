package vista.configuracion.panels;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

import controlador.actions.editoravanzado.autolayout.AutoLayoutConfig;
import modelo.editor.smartguides.SmartGuidesConfig;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.configuracion.ConfigurationPanel;

/**
 * Panel de configuración del editor: parámetros persistentes de Smart Guides y
 * del sistema Auto Layout. Al guardar, escribe las claves de configuración y
 * sincroniza los singletons de ejecución de ambos sistemas.
 */
public class EditorPanel extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;

    private final JCheckBox chkShowGuides = new JCheckBox("Activar Smart Guides");
    private final JCheckBox chkSnapCanvas = new JCheckBox("Ajustar al lienzo");
    private final JCheckBox chkSnapLayers = new JCheckBox("Ajustar a otras capas");
    private final JSpinner spnSnapDistance = new JSpinner(new SpinnerNumberModel(6, 0, 50, 1));
    private final JSpinner spnStickyDistance = new JSpinner(new SpinnerNumberModel(3, 0, 20, 1));
    private final JButton btnGuideColor = new JButton();
    private final JSpinner spnStrokeWidth = new JSpinner(new SpinnerNumberModel(1.0, 0.25, 5.0, 0.25));

    private final JSpinner spnMargin = new JSpinner(new SpinnerNumberModel(24, 0, 400, 1));
    private final JSpinner spnSpacing = new JSpinner(new SpinnerNumberModel(20, 0, 400, 1));
    private final JSpinner spnFitMargin = new JSpinner(new SpinnerNumberModel(24, 0, 400, 1));
    private final JSlider slHeroScale = new JSlider(20, 80, 50);
    private final JSlider slMosaicVariance = new JSlider(0, 50, 15);
    private final JComboBox<String> cbPackMode = new JComboBox<>(
            new String[] { "Estantes (altura)", "Skyline", "Guillotina" });
    private final JRadioButton rbNoSelFuera = new JRadioButton("No seleccionadas: fuera");
    private final JRadioButton rbNoSelIgnorar = new JRadioButton("No seleccionadas: ignorar");

    private boolean initialShowGuides;
    private boolean initialSnapCanvas;
    private boolean initialSnapLayers;
    private int initialSnapDistance;
    private int initialStickyDistance;
    private Color initialGuideColor;
    private float initialStrokeWidth;
    private int initialMargin;
    private int initialSpacing;
    private int initialFitMargin;
    private double initialHeroScale;
    private double initialMosaicVariance;
    private int initialPackMode;
    private int initialNoSeleccionadas;

    /**
     * Construye el panel de configuración del editor.
     *
     * @param config gestor de configuración con las claves persistentes
     */
    public EditorPanel(ConfigurationManager config) {
        setLayout(new GridBagLayout());
        initComponents();
        load(config);
    } // --- Fin del constructor EditorPanel ---


    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;

        JPanel guidesPanel = buildGuidesPanel();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 0.0;
        add(guidesPanel, gbc);

        JPanel autoLayoutPanel = buildAutoLayoutPanel();
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        add(autoLayoutPanel, gbc);
    } // --- Fin del metodo initComponents ---


    /**
     * Construye la subsección de Smart Guides.
     *
     * @return panel con los controles de Smart Guides
     */
    private JPanel buildGuidesPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Smart Guides"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 3;
        panel.add(chkShowGuides, g);

        g.gridy = 1;
        panel.add(chkSnapCanvas, g);

        g.gridy = 2;
        panel.add(chkSnapLayers, g);

        g.gridwidth = 1;
        g.gridy = 3;
        g.gridx = 0;
        panel.add(new JLabel("Distancia de ajuste (px):"), g);
        g.gridx = 1;
        spnSnapDistance.setPreferredSize(new java.awt.Dimension(70, 24));
        panel.add(spnSnapDistance, g);

        g.gridy = 4;
        g.gridx = 0;
        panel.add(new JLabel("Distancia de retención (px):"), g);
        g.gridx = 1;
        spnStickyDistance.setPreferredSize(new java.awt.Dimension(70, 24));
        panel.add(spnStickyDistance, g);

        g.gridy = 5;
        g.gridx = 0;
        panel.add(new JLabel("Color de guías:"), g);
        g.gridx = 1;
        btnGuideColor.setPreferredSize(new java.awt.Dimension(70, 24));
        btnGuideColor.addActionListener(e -> chooseGuideColor());
        panel.add(btnGuideColor, g);

        g.gridy = 6;
        g.gridx = 0;
        panel.add(new JLabel("Grosor de línea:"), g);
        g.gridx = 1;
        spnStrokeWidth.setPreferredSize(new java.awt.Dimension(70, 24));
        panel.add(spnStrokeWidth, g);

        g.gridy = 7;
        g.gridx = 0;
        g.gridwidth = 3;
        g.weighty = 1.0;
        panel.add(new JPanel(), g);

        return panel;
    } // --- Fin del metodo buildGuidesPanel ---


    /**
     * Construye la subsección de parámetros del sistema Auto Layout.
     *
     * @return panel con los controles de Auto Layout
     */
    private JPanel buildAutoLayoutPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Auto Layout"));

        ButtonGroup noSelGroup = new ButtonGroup();
        noSelGroup.add(rbNoSelFuera);
        noSelGroup.add(rbNoSelIgnorar);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        panel.add(rbNoSelFuera, g);
        g.gridy = 1;
        panel.add(rbNoSelIgnorar, g);

        g.gridwidth = 1;
        addSpinnerRow(panel, g, 2, "Espaciado:", spnSpacing);
        addSpinnerRow(panel, g, 3, "Margen:", spnMargin);
        addSpinnerRow(panel, g, 4, "Margen ajustar:", spnFitMargin);

        g.gridy = 5;
        g.gridx = 0;
        panel.add(new JLabel("Escala hero:"), g);
        g.gridx = 1;
        slHeroScale.setPreferredSize(new java.awt.Dimension(120, 18));
        panel.add(slHeroScale, g);

        g.gridy = 6;
        g.gridx = 0;
        panel.add(new JLabel("Var. mosaico:"), g);
        g.gridx = 1;
        slMosaicVariance.setPreferredSize(new java.awt.Dimension(120, 18));
        panel.add(slMosaicVariance, g);

        g.gridy = 7;
        g.gridx = 0;
        panel.add(new JLabel("Empaquetado:"), g);
        g.gridx = 1;
        cbPackMode.setPreferredSize(new java.awt.Dimension(150, 24));
        panel.add(cbPackMode, g);

        return panel;
    } // --- Fin del metodo buildAutoLayoutPanel ---


    /**
     * Añade una fila etiqueta + spinner al panel de Auto Layout.
     *
     * @param panel panel destino
     * @param g constraints de la cuadrícula
     * @param row fila
     * @param label texto de la etiqueta
     * @param spinner spinner asociado
     */
    private void addSpinnerRow(JPanel panel, GridBagConstraints g, int row, String label, JSpinner spinner) {
        g.gridy = row;
        g.gridx = 0;
        panel.add(new JLabel(label), g);
        g.gridx = 1;
        spinner.setPreferredSize(new java.awt.Dimension(70, 24));
        panel.add(spinner, g);
    } // --- Fin del metodo addSpinnerRow ---


    /**
     * Abre el selector de color para el color de las guías.
     */
    private void chooseGuideColor() {
        Color chosen = JColorChooser.showDialog(this, "Color de Smart Guides", getGuideColor());
        if (chosen != null) {
            setGuideColor(chosen);
        }
    } // --- Fin del metodo chooseGuideColor ---


    private Color getGuideColor() {
        return btnGuideColor.getBackground();
    } // --- Fin del metodo getGuideColor ---


    private void setGuideColor(Color color) {
        btnGuideColor.setBackground(color);
    } // --- Fin del metodo setGuideColor ---


    @Override
    public void load(ConfigurationManager config) {
        SmartGuidesConfig guides = SmartGuidesConfig.get();
        AutoLayoutConfig autoLayout = AutoLayoutConfig.get();

        initialShowGuides = config.getBoolean(ConfigKeys.EDITOR_SMART_GUIDES_SHOW, true);
        initialSnapCanvas = config.getBoolean(ConfigKeys.EDITOR_SMART_GUIDES_SNAP_CANVAS, true);
        initialSnapLayers = config.getBoolean(ConfigKeys.EDITOR_SMART_GUIDES_SNAP_LAYERS, true);
        initialSnapDistance = config.getInt(ConfigKeys.EDITOR_SMART_GUIDES_SNAP_DISTANCE, 6);
        initialStickyDistance = config.getInt(ConfigKeys.EDITOR_SMART_GUIDES_STICKY_DISTANCE, 3);
        initialGuideColor = config.getColor(ConfigKeys.EDITOR_SMART_GUIDES_COLOR, new Color(0xFF00FF));
        initialStrokeWidth = (float) config.getDouble(ConfigKeys.EDITOR_SMART_GUIDES_STROKE_WIDTH, 1.0);

        chkShowGuides.setSelected(initialShowGuides);
        chkSnapCanvas.setSelected(initialSnapCanvas);
        chkSnapLayers.setSelected(initialSnapLayers);
        spnSnapDistance.setValue(initialSnapDistance);
        spnStickyDistance.setValue(initialStickyDistance);
        setGuideColor(initialGuideColor);
        spnStrokeWidth.setValue((double) initialStrokeWidth);

        initialMargin = config.getInt(ConfigKeys.EDITOR_AUTOLAYOUT_MARGIN, 24);
        initialSpacing = config.getInt(ConfigKeys.EDITOR_AUTOLAYOUT_SPACING, 20);
        initialFitMargin = config.getInt(ConfigKeys.EDITOR_AUTOLAYOUT_FIT_MARGIN, 24);
        initialHeroScale = config.getDouble(ConfigKeys.EDITOR_AUTOLAYOUT_HERO_SCALE, 0.5);
        initialMosaicVariance = config.getDouble(ConfigKeys.EDITOR_AUTOLAYOUT_MOSAIC_VARIANCE, 0.15);
        initialPackMode = packModeIndex(config.getString(ConfigKeys.EDITOR_AUTOLAYOUT_PACK_MODE, "SHELF"));
        initialNoSeleccionadas = noSeleccionadasIndex(
                config.getString(ConfigKeys.EDITOR_AUTOLAYOUT_NO_SELECCIONADAS, "SACAR_FUERA"));

        spnMargin.setValue(initialMargin);
        spnSpacing.setValue(initialSpacing);
        spnFitMargin.setValue(initialFitMargin);
        slHeroScale.setValue((int) Math.round(initialHeroScale * 100));
        slMosaicVariance.setValue((int) Math.round(initialMosaicVariance * 100));
        cbPackMode.setSelectedIndex(initialPackMode);
        rbNoSelFuera.setSelected(initialNoSeleccionadas == 0);
        rbNoSelIgnorar.setSelected(initialNoSeleccionadas == 1);

        syncRuntime(guides, autoLayout);
    } // --- Fin del metodo load ---


    @Override
    public boolean save(ConfigurationManager config) {
        boolean changed = false;
        SmartGuidesConfig guides = SmartGuidesConfig.get();
        AutoLayoutConfig autoLayout = AutoLayoutConfig.get();

        if (chkShowGuides.isSelected() != initialShowGuides) {
            config.setString(ConfigKeys.EDITOR_SMART_GUIDES_SHOW, String.valueOf(chkShowGuides.isSelected()));
            initialShowGuides = chkShowGuides.isSelected();
            changed = true;
        }
        if (chkSnapCanvas.isSelected() != initialSnapCanvas) {
            config.setString(ConfigKeys.EDITOR_SMART_GUIDES_SNAP_CANVAS, String.valueOf(chkSnapCanvas.isSelected()));
            initialSnapCanvas = chkSnapCanvas.isSelected();
            changed = true;
        }
        if (chkSnapLayers.isSelected() != initialSnapLayers) {
            config.setString(ConfigKeys.EDITOR_SMART_GUIDES_SNAP_LAYERS, String.valueOf(chkSnapLayers.isSelected()));
            initialSnapLayers = chkSnapLayers.isSelected();
            changed = true;
        }
        int snapDistance = (Integer) spnSnapDistance.getValue();
        if (snapDistance != initialSnapDistance) {
            config.setString(ConfigKeys.EDITOR_SMART_GUIDES_SNAP_DISTANCE, String.valueOf(snapDistance));
            initialSnapDistance = snapDistance;
            changed = true;
        }
        int stickyDistance = (Integer) spnStickyDistance.getValue();
        if (stickyDistance != initialStickyDistance) {
            config.setString(ConfigKeys.EDITOR_SMART_GUIDES_STICKY_DISTANCE, String.valueOf(stickyDistance));
            initialStickyDistance = stickyDistance;
            changed = true;
        }
        Color guideColor = getGuideColor();
        if (!guideColor.equals(initialGuideColor)) {
            config.setColor(ConfigKeys.EDITOR_SMART_GUIDES_COLOR, guideColor);
            initialGuideColor = guideColor;
            changed = true;
        }
        float strokeWidth = ((Number) spnStrokeWidth.getValue()).floatValue();
        if (strokeWidth != initialStrokeWidth) {
            config.setString(ConfigKeys.EDITOR_SMART_GUIDES_STROKE_WIDTH, String.valueOf(strokeWidth));
            initialStrokeWidth = strokeWidth;
            changed = true;
        }

        int margin = (Integer) spnMargin.getValue();
        if (margin != initialMargin) {
            config.setString(ConfigKeys.EDITOR_AUTOLAYOUT_MARGIN, String.valueOf(margin));
            initialMargin = margin;
            changed = true;
        }
        int spacing = (Integer) spnSpacing.getValue();
        if (spacing != initialSpacing) {
            config.setString(ConfigKeys.EDITOR_AUTOLAYOUT_SPACING, String.valueOf(spacing));
            initialSpacing = spacing;
            changed = true;
        }
        int fitMargin = (Integer) spnFitMargin.getValue();
        if (fitMargin != initialFitMargin) {
            config.setString(ConfigKeys.EDITOR_AUTOLAYOUT_FIT_MARGIN, String.valueOf(fitMargin));
            initialFitMargin = fitMargin;
            changed = true;
        }
        double heroScale = slHeroScale.getValue() / 100.0;
        if (heroScale != initialHeroScale) {
            config.setString(ConfigKeys.EDITOR_AUTOLAYOUT_HERO_SCALE, String.valueOf(heroScale));
            initialHeroScale = heroScale;
            changed = true;
        }
        double mosaicVariance = slMosaicVariance.getValue() / 100.0;
        if (mosaicVariance != initialMosaicVariance) {
            config.setString(ConfigKeys.EDITOR_AUTOLAYOUT_MOSAIC_VARIANCE, String.valueOf(mosaicVariance));
            initialMosaicVariance = mosaicVariance;
            changed = true;
        }
        int packMode = cbPackMode.getSelectedIndex();
        if (packMode != initialPackMode) {
            config.setString(ConfigKeys.EDITOR_AUTOLAYOUT_PACK_MODE,
                    AutoLayoutConfig.PackMode.values()[packMode].name());
            initialPackMode = packMode;
            changed = true;
        }
        int noSeleccionadas = rbNoSelIgnorar.isSelected() ? 1 : 0;
        if (noSeleccionadas != initialNoSeleccionadas) {
            config.setString(ConfigKeys.EDITOR_AUTOLAYOUT_NO_SELECCIONADAS,
                    noSeleccionadas == 1
                            ? AutoLayoutConfig.NoSeleccionadasMode.IGNORAR.name()
                            : AutoLayoutConfig.NoSeleccionadasMode.SACAR_FUERA.name());
            initialNoSeleccionadas = noSeleccionadas;
            changed = true;
        }

        syncRuntime(guides, autoLayout);
        return changed;
    } // --- Fin del metodo save ---


    @Override
    public boolean isModified() {
        return chkShowGuides.isSelected() != initialShowGuides
                || chkSnapCanvas.isSelected() != initialSnapCanvas
                || chkSnapLayers.isSelected() != initialSnapLayers
                || (Integer) spnSnapDistance.getValue() != initialSnapDistance
                || (Integer) spnStickyDistance.getValue() != initialStickyDistance
                || !getGuideColor().equals(initialGuideColor)
                || ((Number) spnStrokeWidth.getValue()).floatValue() != initialStrokeWidth
                || (Integer) spnMargin.getValue() != initialMargin
                || (Integer) spnSpacing.getValue() != initialSpacing
                || (Integer) spnFitMargin.getValue() != initialFitMargin
                || slHeroScale.getValue() / 100.0 != initialHeroScale
                || slMosaicVariance.getValue() / 100.0 != initialMosaicVariance
                || cbPackMode.getSelectedIndex() != initialPackMode
                || (rbNoSelIgnorar.isSelected() ? 1 : 0) != initialNoSeleccionadas;
    } // --- Fin del metodo isModified ---


    @Override
    public String getTitle() {
        return "Editor";
    } // --- Fin del metodo getTitle ---


    /**
     * Sincroniza los singletons de ejecución de Smart Guides y Auto Layout con
     * los valores actuales del panel.
     *
     * @param guides configuración runtime de Smart Guides
     * @param autoLayout configuración runtime de Auto Layout
     */
    private void syncRuntime(SmartGuidesConfig guides, AutoLayoutConfig autoLayout) {
        guides.setShowGuides(chkShowGuides.isSelected());
        guides.setSnapToCanvas(chkSnapCanvas.isSelected());
        guides.setSnapToLayers(chkSnapLayers.isSelected());
        guides.setSnapDistance((Integer) spnSnapDistance.getValue());
        guides.setStickyDistance((Integer) spnStickyDistance.getValue());
        guides.setGuideColor(getGuideColor());
        guides.setGuideStrokeWidth(((Number) spnStrokeWidth.getValue()).floatValue());

        autoLayout.setMargin((Integer) spnMargin.getValue());
        autoLayout.setSpacing((Integer) spnSpacing.getValue());
        autoLayout.setFitMargin((Integer) spnFitMargin.getValue());
        autoLayout.setHeroScale(slHeroScale.getValue() / 100.0);
        autoLayout.setMosaicVariance(slMosaicVariance.getValue() / 100.0);
        autoLayout.setPackMode(AutoLayoutConfig.PackMode.values()[cbPackMode.getSelectedIndex()]);
        autoLayout.setNoSeleccionadas(rbNoSelIgnorar.isSelected()
                ? AutoLayoutConfig.NoSeleccionadasMode.IGNORAR
                : AutoLayoutConfig.NoSeleccionadasMode.SACAR_FUERA);
    } // --- Fin del metodo syncRuntime ---


    /**
     * Convierte el nombre persistido de modo de empaquetado a índice del combo.
     *
     * @param name nombre del enum
     * @return índice correspondiente (0 por defecto)
     */
    private static int packModeIndex(String name) {
        try {
            return AutoLayoutConfig.PackMode.valueOf(name).ordinal();
        } catch (IllegalArgumentException ex) {
            return 0;
        }
    } // --- Fin del metodo packModeIndex ---


    /**
     * Convierte el nombre persistido de comportamiento de no seleccionadas a
     * índice de selección (0 = fuera, 1 = ignorar).
     *
     * @param name nombre del enum
     * @return índice correspondiente (0 por defecto)
     */
    private static int noSeleccionadasIndex(String name) {
        try {
            return AutoLayoutConfig.NoSeleccionadasMode.valueOf(name) == AutoLayoutConfig.NoSeleccionadasMode.IGNORAR
                    ? 1
                    : 0;
        } catch (IllegalArgumentException ex) {
            return 0;
        }
    } // --- Fin del metodo noSeleccionadasIndex ---

} // --- Fin de la clase EditorPanel ---
