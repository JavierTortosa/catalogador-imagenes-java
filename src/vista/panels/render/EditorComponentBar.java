package vista.panels.render;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import controlador.commands.AppActionCommands;
import vista.config.SeparatorDefinition;
import vista.config.ToolbarButtonDefinition;
import vista.config.ToolbarComponentDefinition;
import vista.config.ToolbarDefinition;
import vista.config.UIDefinitionService;
import vista.util.IconUtils;

public class EditorComponentBar extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JComboBox<ToolItem> toolCombo;
    private final DefaultComboBoxModel<ToolItem> comboModel;
    private final JSpinner spinnerX;
    private final JSpinner spinnerY;
    private final JSpinner spinnerW;
    private final JSpinner spinnerH;
    private final JLabel labelX;
    private final JLabel labelY;
    private final JLabel labelW;
    private final JLabel labelH;
    private final JPanel toolControlsPanel;
    private final CardLayout cardLayout;
    private final Map<String, JPanel> toolOptionsMap;
    private final JPanel row1Right;

    private IconUtils iconUtils;
    private UIDefinitionService uiDefinitionService;
    private Color fgStatus = Color.WHITE;


    public record ToolItem(String commandKey, String iconKey, String tooltip) {

        @Override
        public String toString() {
            return tooltip;
        }

    } // --- Fin del record ToolItem ---


    public EditorComponentBar() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(55, 55, 60));

        toolOptionsMap = new HashMap<>();

        // ===== FILA 1: izquierda (combo + spinners) + derecha (layer selection) =====
        JPanel row1 = new JPanel(new BorderLayout(0, 0));
        row1.setBackground(getBackground());

        JPanel leftPart = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        leftPart.setBackground(getBackground());

        leftPart.add(Box.createHorizontalStrut(8));

        comboModel = new DefaultComboBoxModel<>();
        toolCombo = new JComboBox<>(comboModel);
        toolCombo.setPreferredSize(new Dimension(180, 22));
        toolCombo.setMinimumSize(new Dimension(180, 22));
        toolCombo.setMaximumSize(new Dimension(180, 22));
        toolCombo.setRenderer(new ToolListRenderer());
        toolCombo.addActionListener(e -> {
            ToolItem selected = (ToolItem) toolCombo.getSelectedItem();
            if (selected != null) {
                showOptionsFor(selected.commandKey());
            }
        });
        leftPart.add(toolCombo);

        leftPart.add(Box.createHorizontalStrut(4));

        labelX = createDimLabel("X:");
        leftPart.add(labelX);
        spinnerX = createDimsSpinner();
        leftPart.add(spinnerX);

        leftPart.add(Box.createHorizontalStrut(2));

        labelY = createDimLabel("Y:");
        leftPart.add(labelY);
        spinnerY = createDimsSpinner();
        leftPart.add(spinnerY);

        leftPart.add(Box.createHorizontalStrut(2));

        labelW = createDimLabel("W:");
        leftPart.add(labelW);
        spinnerW = createDimsSpinner();
        leftPart.add(spinnerW);

        leftPart.add(Box.createHorizontalStrut(2));

        labelH = createDimLabel("H:");
        leftPart.add(labelH);
        spinnerH = createDimsSpinner();
        leftPart.add(spinnerH);

        row1Right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        row1Right.setBackground(getBackground());

        row1.add(leftPart, BorderLayout.CENTER);
        row1.add(row1Right, BorderLayout.EAST);

        // ===== FILA 2: tool controls (Parte B) =====
        cardLayout = new CardLayout();
        toolControlsPanel = new JPanel(cardLayout);
        toolControlsPanel.setBackground(getBackground());
        // Margen izquierdo para que no empiece encima del panel de botones
        toolControlsPanel.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 0));
        // Altura mínima para que la fila 2 no colapse
        toolControlsPanel.setPreferredSize(new Dimension(0, 24));

        add(row1);
        add(toolControlsPanel);
    } // --- Fin del constructor EditorComponentBar ---


    public void setIconUtils(IconUtils iconUtils) {
        this.iconUtils = iconUtils;
        repaint();
    } // --- Fin del metodo setIconUtils ---


    public JPanel getToolControlsPanel() {
        return toolControlsPanel;
    } // --- Fin del metodo getToolControlsPanel ---


    public JComboBox<ToolItem> getToolCombo() {
        return toolCombo;
    } // --- Fin del metodo getToolCombo ---


    public void setUiDefinitionService(UIDefinitionService service) {
        this.uiDefinitionService = service;
        reloadTools();
    } // --- Fin del metodo setUiDefinitionService ---


    public void reloadTools() {
        comboModel.removeAllElements();
        if (uiDefinitionService == null) return;

        ToolbarDefinition tbDef = uiDefinitionService.getToolbarDefinition("editoravanzado");
        if (tbDef == null) return;

        for (ToolbarComponentDefinition comp : tbDef.componentes()) {
            if (comp instanceof ToolbarButtonDefinition btnDef) {
                if (AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE.equals(btnDef.comandoCanonico())) continue;
                comboModel.addElement(new ToolItem(
                        btnDef.comandoCanonico(),
                        btnDef.claveIcono(),
                        btnDef.textoTooltip()));
            }
        }

        buildRow1RightSide();
        buildAllToolPanels();

        if (comboModel.getSize() > 0) {
            toolCombo.setSelectedIndex(0);
        }
    } // --- Fin del metodo reloadTools ---


    public void selectToolByCommand(String commandKey) {
        for (int i = 0; i < comboModel.getSize(); i++) {
            ToolItem item = comboModel.getElementAt(i);
            if (item.commandKey().equals(commandKey)) {
                toolCombo.setSelectedIndex(i);
                return;
            }
        }
    } // --- Fin del metodo selectToolByCommand ---


    private void buildRow1RightSide() {
        row1Right.removeAll();
        if (uiDefinitionService == null) return;
        Color bg = getBackground();
        ButtonGroup group = new ButtonGroup();
        addButtonsFromDef(row1Right, "editoravanzadolayerselection", bg, group);
        row1Right.revalidate();
        row1Right.repaint();
    } // --- Fin del metodo buildRow1RightSide ---


    public void buildAllToolPanels() {
        toolControlsPanel.removeAll();
        toolOptionsMap.clear();
        Color bg = getBackground();

        for (int i = 0; i < comboModel.getSize(); i++) {
            ToolItem item = comboModel.getElementAt(i);
            JPanel panel = buildPanelForTool(item.commandKey(), bg);
            toolControlsPanel.add(panel, item.commandKey());
            toolOptionsMap.put(item.commandKey(), panel);
        }

        toolControlsPanel.revalidate();
        toolControlsPanel.repaint();
    } // --- Fin del metodo buildAllToolPanels ---


    public void showOptionsFor(String commandKey) {
        if (toolOptionsMap.containsKey(commandKey)) {
            cardLayout.show(toolControlsPanel, commandKey);
        }
    } // --- Fin del metodo showOptionsFor ---


    private JPanel buildPanelForTool(String commandKey, Color bg) {
        if (AppActionCommands.CMD_ADVANCED_EDITOR_EDICION.equals(commandKey))
            return newEmptyPanel(bg);
        if (AppActionCommands.CMD_ADVANCED_EDITOR_TRANSFORMAR.equals(commandKey))
            return buildTransformPanel(bg);
        if (AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_MARCO.equals(commandKey) ||
            AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_CAPA.equals(commandKey))
            return buildFeatherPanel(bg);
        if (AppActionCommands.CMD_ADVANCED_EDITOR_VARITA.equals(commandKey))
            return buildWandPanel(bg);
        if (AppActionCommands.CMD_ADVANCED_EDITOR_RECORTAR.equals(commandKey))
            return buildCropPanel(bg);
        if (AppActionCommands.CMD_ADVANCED_EDITOR_CUENTAGOTAS.equals(commandKey))
            return buildEyedropperPanel(bg);
        if (AppActionCommands.CMD_ADVANCED_EDITOR_BOTE_PINTURA.equals(commandKey))
            return buildPaintBucketPanel(bg);
        if (AppActionCommands.CMD_ADVANCED_EDITOR_DEGRADADO.equals(commandKey))
            return buildGradientPanel(bg);
        if (AppActionCommands.CMD_ADVANCED_EDITOR_TEXTO.equals(commandKey))
            return buildTextPanel(bg);
        if (AppActionCommands.CMD_ADVANCED_EDITOR_FORMAS.equals(commandKey))
            return buildShapesPanel(bg);
        return newEmptyPanel(bg);
    } // --- Fin del metodo buildPanelForTool ---


    // ===================== PANELES PARTE B =====================


    private JPanel buildTransformPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        ButtonGroup group = new ButtonGroup();
        addButtonsFromDef(p, "editoravanzadotransform", bg, group);
        p.add(createSubSeparator(bg));
        addButtonsFromDef(p, "editoravanzadolayerselection", bg, group);

        JCheckBox keepAspect = new JCheckBox("Mantener proporción");
        keepAspect.setBackground(bg);
        keepAspect.setForeground(fgStatus);
        p.add(Box.createHorizontalStrut(4));
        p.add(keepAspect);

        return p;
    } // --- Fin del metodo buildTransformPanel ---


    private JPanel buildFeatherPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        JLabel lb = new JLabel("Desvanecer:");
        lb.setForeground(fgStatus);
        p.add(lb);
        JSlider slider = new JSlider(0, 50, 0);
        slider.setBackground(bg);
        slider.setPreferredSize(new Dimension(80, 20));
        p.add(slider);
        JLabel val = new JLabel("0");
        val.setForeground(fgStatus);
        p.add(val);
        slider.addChangeListener(e -> val.setText(String.valueOf(slider.getValue())));
        return p;
    } // --- Fin del metodo buildFeatherPanel ---


    private JPanel buildWandPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        JLabel lb = new JLabel("Tolerancia:");
        lb.setForeground(fgStatus);
        p.add(lb);
        JSpinner tol = new JSpinner(new SpinnerNumberModel(32, 0, 255, 1));
        tol.setPreferredSize(new Dimension(50, 20));
        p.add(tol);
        JCheckBox contiguo = new JCheckBox("Contiguo");
        contiguo.setBackground(bg);
        contiguo.setForeground(fgStatus);
        contiguo.setSelected(true);
        p.add(Box.createHorizontalStrut(4));
        p.add(contiguo);
        return p;
    } // --- Fin del metodo buildWandPanel ---


    private JPanel buildCropPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        ButtonGroup group = new ButtonGroup();
        addButtonsFromDef(p, "editoravanzadocrop", bg, group);
        JCheckBox mantener = new JCheckBox("Mantener original");
        mantener.setBackground(bg);
        mantener.setForeground(fgStatus);
        p.add(Box.createHorizontalStrut(4));
        p.add(mantener);
        return p;
    } // --- Fin del metodo buildCropPanel ---


    private JPanel buildEyedropperPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        JLabel lb = new JLabel("Tamaño de muestra:");
        lb.setForeground(fgStatus);
        p.add(lb);
        ButtonGroup group = new ButtonGroup();
        for (String s : new String[]{"1×1", "3×3", "5×5"}) {
            JRadioButton rb = new JRadioButton(s);
            rb.setBackground(bg);
            rb.setForeground(fgStatus);
            group.add(rb);
            p.add(rb);
        }
        group.getElements().nextElement().setSelected(true);
        return p;
    } // --- Fin del metodo buildEyedropperPanel ---


    private JPanel buildPaintBucketPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        // Color swatch button
        p.add(createColorSwatch(new Color(255, 0, 0), "Color de relleno", bg));
        p.add(Box.createHorizontalStrut(6));
        // Tolerancia
        JLabel lb = new JLabel("Tolerancia:");
        lb.setForeground(fgStatus);
        p.add(lb);
        JSpinner tol = new JSpinner(new SpinnerNumberModel(32, 0, 255, 1));
        tol.setPreferredSize(new Dimension(50, 20));
        p.add(tol);
        p.add(Box.createHorizontalStrut(4));
        JCheckBox contiguo = new JCheckBox("Contiguo");
        contiguo.setBackground(bg);
        contiguo.setForeground(fgStatus);
        contiguo.setSelected(true);
        p.add(contiguo);
        return p;
    } // --- Fin del metodo buildPaintBucketPanel ---


    private JPanel buildGradientPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);

        JButton colorStart = createColorSwatch(Color.RED, "Color inicial", bg);
        JButton colorEnd = createColorSwatch(Color.BLUE, "Color final", bg);
        JComboBox<String> typeCombo = new JComboBox<>(
                new String[]{"Lineal", "Radial", "Angular", "Reflejado", "Diamante"});
        typeCombo.setPreferredSize(new Dimension(90, 22));

        // Preview que lee colores y tipo en tiempo real
        JPanel preview = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                int w = getWidth(), h = getHeight();
                if (w > 0 && h > 0) {
                    Color c1 = colorStart.getBackground();
                    Color c2 = colorEnd.getBackground();
                    String tipo = (String) typeCombo.getSelectedItem();
                    if ("Radial".equals(tipo)) {
                        float r = Math.min(w, h) / 2f;
                        g2.setPaint(new java.awt.RadialGradientPaint(
                                w / 2f, h / 2f, r,
                                new float[]{0f, 1f},
                                new java.awt.Color[]{c1, c2}));
                    } else if ("Lineal".equals(tipo)) {
                        g2.setPaint(new java.awt.GradientPaint(0, 0, c1, w, h, c2));
                    } else if ("Angular".equals(tipo)) {
                        g2.setPaint(new java.awt.GradientPaint(0, h / 2f, c1, w, h / 2f, c2));
                    } else if ("Reflejado".equals(tipo)) {
                        int mid = h / 2;
                        g2.setPaint(new java.awt.GradientPaint(0, 0, c1, 0, mid, c2));
                        g2.fillRect(0, 0, w, mid);
                        g2.setPaint(new java.awt.GradientPaint(0, mid, c2, 0, h, c1));
                        g2.fillRect(0, mid, w, h - mid);
                        g2.dispose();
                        return;
                    } else if ("Diamante".equals(tipo)) {
                        float r = Math.max(w, h) * 0.7f;
                        g2.setPaint(new java.awt.RadialGradientPaint(
                                w / 2f, h / 2f, r,
                                new float[]{0f, 0.5f, 1f},
                                new java.awt.Color[]{c1, c2, c1}));
                    }
                    g2.fillRect(0, 0, w, h);
                }
                g2.dispose();
            }
        };
        preview.setPreferredSize(new Dimension(28, 20));
        preview.setBackground(bg);
        preview.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 80)));

        // Forzar repintado del preview al cambiar colores o tipo
        colorStart.addActionListener(e -> preview.repaint());
        colorEnd.addActionListener(e -> preview.repaint());
        typeCombo.addActionListener(e -> preview.repaint());

        p.add(preview);
        p.add(Box.createHorizontalStrut(2));
        p.add(colorStart);
        p.add(Box.createHorizontalStrut(2));
        p.add(colorEnd);

        p.add(Box.createHorizontalStrut(4));
        p.add(typeCombo);

        p.add(Box.createHorizontalStrut(4));
        JLabel lb = new JLabel("Opacidad:");
        lb.setForeground(fgStatus);
        p.add(lb);
        JSlider op = new JSlider(0, 100, 100);
        op.setBackground(bg);
        op.setPreferredSize(new Dimension(50, 20));
        p.add(op);

        return p;
    } // --- Fin del metodo buildGradientPanel ---


    private JPanel buildTextPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        ButtonGroup group = new ButtonGroup();
        ToolbarDefinition def = getSubDef("editoravanzadotexto");
        if (def != null) {
            for (ToolbarComponentDefinition comp : def.componentes()) {
                if (comp instanceof ToolbarButtonDefinition btnDef) {
                    String iconKey = btnDef.claveIcono();
                    if ("80900-search-font.png".equals(iconKey)) {
                        JLabel iconLb = createIconLabel(btnDef, bg);
                        p.add(iconLb);
                        // JComboBox con fuentes del sistema
                        String[] fonts = java.awt.GraphicsEnvironment
                                .getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
                        JComboBox<String> fontCombo = new JComboBox<>(fonts);
                        fontCombo.setPreferredSize(new Dimension(140, 22));
                        p.add(fontCombo);
                    } else if ("80905-font-size.png".equals(iconKey)) {
                        JLabel iconLb = createIconLabel(btnDef, bg);
                        p.add(iconLb);
                        // JComboBox con tamaños de fuente
                        Integer[] sizes = {8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 36, 48, 72};
                        JComboBox<Integer> sizeCombo = new JComboBox<>(sizes);
                        sizeCombo.setSelectedItem(12);
                        sizeCombo.setPreferredSize(new Dimension(55, 22));
                        p.add(sizeCombo);
                    } else {
                        p.add(createSubToolButton(btnDef, bg, group));
                    }
                } else if (comp instanceof SeparatorDefinition) {
                    p.add(createSubSeparator(bg));
                }
            }
        }

        // Color picker al final
        p.add(createSubSeparator(bg));
        p.add(createColorSwatch(Color.BLACK, "Color del texto", bg));

        return p;
    } // --- Fin del metodo buildTextPanel ---


    private JPanel buildShapesPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        ButtonGroup group = new ButtonGroup();
        ToolbarDefinition def = getSubDef("editoravanzadoformas");
        if (def != null) {
            int idx = 0;
            int total = def.componentes().size();
            for (ToolbarComponentDefinition comp : def.componentes()) {
                idx++;
                if (comp instanceof ToolbarButtonDefinition btnDef) {
                    if (idx > total - 3) {
                        // Últimas 3: relleno, borde, grosor → componentes custom
                        String text = btnDef.textoTooltip();
                        if ("Relleno".equals(text)) {
                            JLabel lb = new JLabel("Relleno:");
                            lb.setForeground(fgStatus);
                            p.add(lb);
                            p.add(createColorSwatch(new Color(200, 200, 200), "Color de relleno", bg));
                        } else if ("Borde".equals(text)) {
                            p.add(Box.createHorizontalStrut(4));
                            JLabel lb = new JLabel("Borde:");
                            lb.setForeground(fgStatus);
                            p.add(lb);
                            p.add(createColorSwatch(Color.BLACK, "Color del borde", bg));
                        } else if ("Grosor de Borde".equals(text)) {
                            p.add(Box.createHorizontalStrut(4));
                            JLabel lb = new JLabel("Grosor:");
                            lb.setForeground(fgStatus);
                            p.add(lb);
                            JSpinner sp = new JSpinner(new SpinnerNumberModel(1, 0, 50, 1));
                            sp.setPreferredSize(new Dimension(50, 20));
                            p.add(sp);
                        }
                    } else {
                        p.add(createSubToolButton(btnDef, bg, group));
                    }
                }
            }
        }
        return p;
    } // --- Fin del metodo buildShapesPanel ---


    // ===================== HELPERS =====================


    private void addButtonsFromDef(JPanel panel, String toolbarKey, Color bg, ButtonGroup group) {
        ToolbarDefinition def = getSubDef(toolbarKey);
        if (def == null) return;
        for (ToolbarComponentDefinition comp : def.componentes()) {
            if (comp instanceof ToolbarButtonDefinition btnDef) {
                panel.add(createSubToolButton(btnDef, bg, group));
            } else if (comp instanceof SeparatorDefinition) {
                panel.add(createSubSeparator(bg));
            }
        }
    } // --- Fin del metodo addButtonsFromDef ---


    private ToolbarDefinition getSubDef(String key) {
        return uiDefinitionService != null ? uiDefinitionService.getToolbarDefinition(key) : null;
    } // --- Fin del metodo getSubDef ---


    private JPanel newEmptyPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        p.setBackground(bg);
        return p;
    } // --- Fin del metodo newEmptyPanel ---


    private JButton createColorSwatch(Color initial, String tooltip, Color bg) {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(20, 20));
        btn.setBackground(initial);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createLineBorder(new Color(120, 120, 120)));
        btn.setToolTipText(tooltip);
        btn.addActionListener(e -> {
            Color nuevo = JColorChooser.showDialog(btn, tooltip, btn.getBackground());
            if (nuevo != null) {
                btn.setBackground(nuevo);
            }
        });
        return btn;
    } // --- Fin del metodo createColorSwatch ---


    private JLabel createIconLabel(ToolbarButtonDefinition btnDef, Color bg) {
        JLabel lb = new JLabel();
        lb.setBackground(bg);
        if (iconUtils != null) {
            var icon = iconUtils.getScaledIcon(btnDef.claveIcono(), 16, 16);
            if (icon != null) lb.setIcon(icon);
        }
        lb.setToolTipText(btnDef.textoTooltip());
        return lb;
    } // --- Fin del metodo createIconLabel ---


    private JToggleButton createSubToolButton(ToolbarButtonDefinition btnDef, Color bg, ButtonGroup group) {
        Action action = new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                // Sub-herramienta — sin implementacion todavia
            }
        };
        action.putValue(Action.ACTION_COMMAND_KEY, btnDef.comandoCanonico());
        action.putValue(Action.SHORT_DESCRIPTION, btnDef.textoTooltip());
        if (iconUtils != null) {
            var icon = iconUtils.getScaledIcon(btnDef.claveIcono(), 18, 18);
            if (icon != null) {
                action.putValue(Action.SMALL_ICON, icon);
            }
        }

        JToggleButton btn = new JToggleButton(action);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setMinimumSize(new Dimension(24, 24));
        btn.setMaximumSize(new Dimension(24, 24));
        btn.setBackground(bg);
        btn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        group.add(btn);
        return btn;
    } // --- Fin del metodo createSubToolButton ---


    private Component createSubSeparator(Color bg) {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(2, 20));
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        wrap.setBackground(bg);
        wrap.add(sep);
        return wrap;
    } // --- Fin del metodo createSubSeparator ---


    public JSpinner getSpinnerX() { return spinnerX; }
    public JSpinner getSpinnerY() { return spinnerY; }
    public JSpinner getSpinnerW() { return spinnerW; }
    public JSpinner getSpinnerH() { return spinnerH; }


    private JLabel createDimLabel(String text) {
        JLabel lb = new JLabel(text);
        lb.setForeground(fgStatus);
        lb.setFont(lb.getFont().deriveFont(10f));
        return lb;
    } // --- Fin del metodo createDimLabel ---


    private JSpinner createDimsSpinner() {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(0, 0, 99999, 1));
        sp.setPreferredSize(new Dimension(48, 20));
        sp.setMaximumSize(new Dimension(48, 20));
        sp.setMinimumSize(new Dimension(48, 20));
        sp.setEnabled(false);
        var editor = sp.getEditor();
        if (editor instanceof JSpinner.DefaultEditor de) {
            de.getTextField().setColumns(4);
            de.getTextField().setHorizontalAlignment(JLabel.RIGHT);
        }
        return sp;
    } // --- Fin del metodo createDimsSpinner ---


    public void updateTheme(Color bg, Color fg, Color border) {
        fgStatus = fg;
        setBackground(bg);
        setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, border));
        updateChildColors(bg, fg, this);
        toolControlsPanel.setBackground(bg);
        row1Right.setBackground(bg);
        toolCombo.setBackground(bg);
        labelX.setForeground(fg);
        labelY.setForeground(fg);
        labelW.setForeground(fg);
        labelH.setForeground(fg);

        ToolItem sel = (ToolItem) toolCombo.getSelectedItem();
        buildRow1RightSide();
        buildAllToolPanels();
        if (sel != null) {
            showOptionsFor(sel.commandKey());
        }
    } // --- Fin del metodo updateTheme ---


    private void updateChildColors(Color bg, Color fg, Container parent) {
        parent.setBackground(bg);
        for (Component c : parent.getComponents()) {
            if (c instanceof JLabel) {
                c.setForeground(fg);
            }
            if (c instanceof Container && !(c instanceof Box.Filler) && c != toolCombo) {
                updateChildColors(bg, fg, (Container) c);
            }
        }
    } // --- Fin del metodo updateChildColors ---


    // -----------------------------------------------------------------------
    // Renderer personalizado para mostrar icono + texto en el desplegable
    // -----------------------------------------------------------------------


    private class ToolListRenderer extends JLabel implements ListCellRenderer<ToolItem> {

        private static final long serialVersionUID = 1L;


        ToolListRenderer() {
            setOpaque(true);
            setFont(getFont().deriveFont(11f));
        } // --- Fin del constructor ToolListRenderer ---


        @Override
        public Component getListCellRendererComponent(JList<? extends ToolItem> list,
                ToolItem value, int index, boolean isSelected, boolean cellHasFocus) {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(fgStatus);
            }
            if (value != null) {
                setText(value.tooltip());
                setToolTipText(value.tooltip());
                if (iconUtils != null) {
                    var icon = iconUtils.getScaledIcon(value.iconKey(), 16, 16);
                    setIcon(icon);
                } else {
                    setIcon(null);
                }
            } else {
                setText("");
                setIcon(null);
                setToolTipText(null);
            }
            return this;
        } // --- Fin del metodo getListCellRendererComponent ---

    } // --- Fin de la clase ToolListRenderer ---

} // --- Fin de la clase EditorComponentBar ---
