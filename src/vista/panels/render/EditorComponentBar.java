package vista.panels.render;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
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
import javax.swing.UIManager;

import controlador.commands.AppActionCommands;
import controlador.tools.CanvasController;
import controlador.tools.TextTool;
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
    private final JPanel leftPartHome;
    private final java.util.List<Component> leftPartToolComponents;

    private IconUtils iconUtils;
    private UIDefinitionService uiDefinitionService;
    private Color fgStatus = Color.WHITE;

    // Home mode state
    private boolean homeActive;
    private boolean homeCanvasSubMode; // true=medidas, false=nuevo
    private final JPanel leftPart;

    // Widgets compartidos del home lienzo (para leer valores desde handleHomeAction)
    private JSpinner homeWSpinner;
    private JSpinner homeHSpinner;
    private JButton homeColorSwatch;
    private JPanel homeLienzoPanel;
    private JPanel homeImagePanel;

    // Mapa de constructores de paneles (Strategy) — elimina el if-chain de buildPanelForTool
    @FunctionalInterface
    private interface PanelBuilder {
        JPanel build(Color bg);
    }

    private final Map<String, PanelBuilder> toolPanelBuilders = new HashMap<>();

    // Referencia al CanvasController (para conectar controles de texto)
    private CanvasController canvasController;

    // --- Retención de componentes del panel de texto ---
    private JComboBox<String> textFontCombo;
    private JComboBox<Integer> textSizeCombo;
    private JButton textColorBtn;
    private JToggleButton textBoldBtn;
    private JToggleButton textItalicBtn;
    private JToggleButton textUnderlineBtn;
    private JToggleButton textStrikethroughBtn;
    private JToggleButton textAlignLeftBtn;
    private JToggleButton textAlignCenterBtn;
    private JToggleButton textAlignRightBtn;
    private JToggleButton textVerticalBtn;

    {
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_EDICION, this::newEmptyPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_TRANSFORMAR, this::buildTransformPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_MARCO, this::buildFeatherPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_CAPA, this::buildFeatherPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_VARITA, this::buildWandPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_RECORTAR, this::buildCropPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_CUENTAGOTAS, this::buildEyedropperPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_BOTE_PINTURA, this::buildPaintBucketPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_DEGRADADO, this::buildGradientPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_TEXTO, this::buildTextPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_FORMAS, this::buildShapesPanel);
    }


    public record ToolItem(String commandKey, String iconKey, String tooltip) {

        @Override
        public String toString() {
            return tooltip;
        }

    } // --- Fin del record ToolItem ---


    private static Color clr(String key, int r, int g, int b) {
        Color c = UIManager.getColor(key);
        return c != null ? c : new Color(r, g, b);
    }

    public EditorComponentBar() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(clr("Visor.statusBarBackground", 55, 55, 60));

        toolOptionsMap = new HashMap<>();

        // ===== FILA 1: izquierda (combo + spinners) + derecha (layer selection) =====
        JPanel row1 = new JPanel(new BorderLayout(0, 0));
        row1.setBackground(getBackground());

        leftPart = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        leftPart.setBackground(getBackground());

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
        leftPart.add(Box.createHorizontalStrut(8));
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

        // Guardar componentes del modo herramienta
        leftPartToolComponents = new java.util.ArrayList<>();
        for (Component c : leftPart.getComponents()) {
            leftPartToolComponents.add(c);
        }

        // Panel home para la fila 1 (Lienzo / Control de la Imagen)
        leftPartHome = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        leftPartHome.setBackground(getBackground());
        leftPartHome.setVisible(false);
        buildHomeRow1();
        leftPart.add(leftPartHome);

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


    public void setCanvasController(CanvasController cc) {
        this.canvasController = cc;
    } // --- Fin del metodo setCanvasController ---


    public void reloadTools() {
        comboModel.removeAllElements();
        if (uiDefinitionService == null) return;

        ToolbarDefinition tbDef = uiDefinitionService.getToolbarDefinition("editoravanzado");
        if (tbDef == null) return;

        for (ToolbarComponentDefinition comp : tbDef.componentes()) {
            if (comp instanceof ToolbarButtonDefinition btnDef) {
            	
                if (AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE.equals(btnDef.comandoCanonico())) continue;
//            	System.out.println(btnDef.comandoCanonico());
                
            	comboModel.addElement(new ToolItem(
                        btnDef.comandoCanonico(),
                        btnDef.claveIcono(),
                        btnDef.textoTooltip()));
            }
        }

        buildRow1RightSide();
        buildHomeRow1();
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
        ToolbarDefinition def = uiDefinitionService.getToolbarDefinition("editoravanzadolayerselection");
        if (def != null) {
            for (ToolbarComponentDefinition comp : def.componentes()) {
                if (comp instanceof ToolbarButtonDefinition btnDef) {
                    String cmd = btnDef.comandoCanonico();
                    if (AppActionCommands.CMD_ADVANCED_EDITOR_PAGE.equals(cmd)) {
                        row1Right.add(createCanvasResizeButton(btnDef, bg));
                    } else {
                        row1Right.add(createSubToolButton(btnDef, bg, group));
                    }
                }
            }
        }
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

        buildHomePanels();

        toolControlsPanel.revalidate();
        toolControlsPanel.repaint();
    } // --- Fin del metodo buildAllToolPanels ---


    public void showOptionsFor(String commandKey) {
        if (toolOptionsMap.containsKey(commandKey)) {
            cardLayout.show(toolControlsPanel, commandKey);
        }
    } // --- Fin del metodo showOptionsFor ---


    private JPanel buildPanelForTool(String commandKey, Color bg) {
        PanelBuilder builder = toolPanelBuilders.get(commandKey);
        return builder != null ? builder.build(bg) : newEmptyPanel(bg);
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
        preview.setBorder(BorderFactory.createLineBorder(swatchBorderColor()));

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
                    String cmd = btnDef.comandoCanonico();
                    String iconKey = btnDef.claveIcono();

                    // Icono + combo de fuentes
                    if ("80900-search-font.png".equals(iconKey)) {
                        JLabel iconLb = createIconLabel(btnDef, bg);
                        p.add(iconLb);
                        String[] fonts = java.awt.GraphicsEnvironment
                                .getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
                        textFontCombo = new JComboBox<>(fonts);
                        textFontCombo.setPreferredSize(new Dimension(140, 22));
                        textFontCombo.addActionListener(e -> {
                            if (canvasController != null
                                    && canvasController.getActiveTool() instanceof TextTool tt) {
                                String sel = (String) textFontCombo.getSelectedItem();
                                if (sel != null) tt.setFontFamily(sel);
                            }
                        });
                        p.add(textFontCombo);

                    // Icono + combo de tamaños
                    } else if ("80905-font-size.png".equals(iconKey)) {
                        JLabel iconLb = createIconLabel(btnDef, bg);
                        p.add(iconLb);
                        Integer[] sizes = {8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 36, 48, 72};
                        textSizeCombo = new JComboBox<>(sizes);
                        textSizeCombo.setSelectedItem(24);
                        textSizeCombo.setPreferredSize(new Dimension(55, 22));
                        textSizeCombo.addActionListener(e -> {
                            if (canvasController != null
                                    && canvasController.getActiveTool() instanceof TextTool tt) {
                                Integer sel = (Integer) textSizeCombo.getSelectedItem();
                                if (sel != null) tt.setFontSize(sel);
                            }
                        });
                        p.add(textSizeCombo);

                    // Botones con command key real
                    } else if (!AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE.equals(cmd)) {
                        String tooltip = btnDef.textoTooltip();
                        JToggleButton tb = createTextToggleButton(cmd, iconKey, tooltip, bg, group);
                        p.add(tb);

                    } else {
                        // Pendiente — botón stub (justified, flow, horizontal)
                        p.add(createSubToolButton(btnDef, bg, new ButtonGroup()));
                    }

                } else if (comp instanceof SeparatorDefinition) {
                    p.add(createSubSeparator(bg));
                }
            }
        }

        // Color picker (el listener integra JColorChooser + setTextColor
        // para evitar el orden inverso de AbstractButton.fireActionPerformed)
        p.add(createSubSeparator(bg));
        textColorBtn = createColorSwatch(Color.BLACK, "Color del texto", bg, nuevo -> {
            if (canvasController != null
                    && canvasController.getActiveTool() instanceof TextTool tt) {
                tt.setTextColor(nuevo);
            }
        });
        p.add(textColorBtn);

        return p;
    } // --- Fin del metodo buildTextPanel ---


    private JToggleButton createTextToggleButton(String cmd, String iconKey,
            String tooltip, Color bg, ButtonGroup group) {
        JToggleButton btn = new JToggleButton();
        btn.setActionCommand(cmd);
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setMinimumSize(new Dimension(24, 24));
        btn.setMaximumSize(new Dimension(24, 24));
        btn.setBackground(bg);
        btn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        if (iconUtils != null) {
            var icon = iconUtils.getScaledIcon(iconKey, 18, 18);
            if (icon != null) btn.setIcon(icon);
        }
        group.add(btn);

        // Guardar referencia según command key
        switch (cmd) {
            case AppActionCommands.CMD_EDITOR_TEXTO_NEGRITA -> {
                textBoldBtn = btn;
                btn.addActionListener(e -> {
                    if (canvasController != null
                            && canvasController.getActiveTool() instanceof TextTool tt) {
                        tt.setBold(btn.isSelected());
                    }
                });
            }
            case AppActionCommands.CMD_EDITOR_TEXTO_CURSIVA -> {
                textItalicBtn = btn;
                btn.addActionListener(e -> {
                    if (canvasController != null
                            && canvasController.getActiveTool() instanceof TextTool tt) {
                        tt.setItalic(btn.isSelected());
                    }
                });
            }
            case AppActionCommands.CMD_EDITOR_TEXTO_SUBRAYADO -> {
                textUnderlineBtn = btn;
                // Subrayado no implementado en TextLayer — acción stub
            }
            case AppActionCommands.CMD_EDITOR_TEXTO_TACHADO -> {
                textStrikethroughBtn = btn;
                // Tachado no implementado — acción stub
            }
            case AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_LEFT -> {
                textAlignLeftBtn = btn;
                btn.addActionListener(e -> {
                    if (canvasController != null
                            && canvasController.getActiveTool() instanceof TextTool tt) {
                        tt.setAlignment(javax.swing.SwingConstants.LEFT);
                    }
                });
            }
            case AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_CENTER -> {
                textAlignCenterBtn = btn;
                btn.addActionListener(e -> {
                    if (canvasController != null
                            && canvasController.getActiveTool() instanceof TextTool tt) {
                        tt.setAlignment(javax.swing.SwingConstants.CENTER);
                    }
                });
            }
            case AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_RIGHT -> {
                textAlignRightBtn = btn;
                btn.addActionListener(e -> {
                    if (canvasController != null
                            && canvasController.getActiveTool() instanceof TextTool tt) {
                        tt.setAlignment(javax.swing.SwingConstants.RIGHT);
                    }
                });
            }
            case AppActionCommands.CMD_EDITOR_TEXTO_VERTICAL -> {
                textVerticalBtn = btn;
                btn.addActionListener(e -> {
                    if (canvasController != null
                            && canvasController.getActiveTool() instanceof TextTool tt) {
                        tt.setVertical(btn.isSelected());
                    }
                });
            }
        }

        return btn;
    } // --- Fin del metodo createTextToggleButton ---


    // ==================== Sincronización bidireccional texto ====================


    public String getTextFontFamily() {
        return textFontCombo != null ? (String) textFontCombo.getSelectedItem() : null;
    } // --- Fin del metodo getTextFontFamily ---


    public void setTextFontFamily(String ff) {
        if (textFontCombo != null) textFontCombo.setSelectedItem(ff);
    } // --- Fin del metodo setTextFontFamily ---


    public int getTextFontSize() {
        if (textSizeCombo != null && textSizeCombo.getSelectedItem() != null) {
            return (Integer) textSizeCombo.getSelectedItem();
        }
        return 0;
    } // --- Fin del metodo getTextFontSize ---


    public void setTextFontSize(int sz) {
        if (textSizeCombo != null) textSizeCombo.setSelectedItem(sz);
    } // --- Fin del metodo setTextFontSize ---


    public Boolean isTextBold() {
        return textBoldBtn != null ? textBoldBtn.isSelected() : null;
    } // --- Fin del metodo isTextBold ---


    public void setTextBold(boolean b) {
        if (textBoldBtn != null) textBoldBtn.setSelected(b);
    } // --- Fin del metodo setTextBold ---


    public Boolean isTextItalic() {
        return textItalicBtn != null ? textItalicBtn.isSelected() : null;
    } // --- Fin del metodo isTextItalic ---


    public void setTextItalic(boolean i) {
        if (textItalicBtn != null) textItalicBtn.setSelected(i);
    } // --- Fin del metodo setTextItalic ---


    public Color getTextColor() {
        return textColorBtn != null ? textColorBtn.getBackground() : null;
    } // --- Fin del metodo getTextColor ---


    public void setTextColor(Color c) {
        if (textColorBtn != null) textColorBtn.setBackground(c);
    } // --- Fin del metodo setTextColor ---


    public int getTextAlignment() {
        if (textAlignLeftBtn != null && textAlignLeftBtn.isSelected())  return javax.swing.SwingConstants.LEFT;
        if (textAlignCenterBtn != null && textAlignCenterBtn.isSelected()) return javax.swing.SwingConstants.CENTER;
        if (textAlignRightBtn != null && textAlignRightBtn.isSelected()) return javax.swing.SwingConstants.RIGHT;
        return -1;
    } // --- Fin del metodo getTextAlignment ---


    public void setTextAlignment(int align) {
        if (textAlignLeftBtn != null)   textAlignLeftBtn.setSelected(align == javax.swing.SwingConstants.LEFT);
        if (textAlignCenterBtn != null) textAlignCenterBtn.setSelected(align == javax.swing.SwingConstants.CENTER);
        if (textAlignRightBtn != null)  textAlignRightBtn.setSelected(align == javax.swing.SwingConstants.RIGHT);
    } // --- Fin del metodo setTextAlignment ---


    public Boolean isTextVertical() {
        return textVerticalBtn != null ? textVerticalBtn.isSelected() : null;
    } // --- Fin del metodo isTextVertical ---


    public void setTextVertical(boolean v) {
        if (textVerticalBtn != null) textVerticalBtn.setSelected(v);
    } // --- Fin del metodo setTextVertical ---


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


    private static Color swatchBorderColor() {
        Color c = UIManager.getColor("Component.borderColor");
        return c != null ? c : new Color(120, 120, 120);
    }


    private JButton createColorSwatch(Color initial, String tooltip, Color bg) {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(20, 20));
        btn.setBackground(initial);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createLineBorder(swatchBorderColor()));
        btn.setToolTipText(tooltip);
        btn.addActionListener(e -> {
            Color nuevo = JColorChooser.showDialog(btn, tooltip, btn.getBackground());
            if (nuevo != null) {
                btn.setBackground(nuevo);
            }
        });
        return btn;
    } // --- Fin del metodo createColorSwatch ---


    /**
     * Crea un boton de color que ademas ejecuta un callback con el color elegido,
     * evitando el orden inverso de AbstractButton.fireActionPerformed.
     */
    private JButton createColorSwatch(Color initial, String tooltip, Color bg,
                                       Consumer<Color> onColorSelected) {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(20, 20));
        btn.setBackground(initial);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createLineBorder(swatchBorderColor()));
        btn.setToolTipText(tooltip);
        btn.addActionListener(e -> {
            Color nuevo = JColorChooser.showDialog(btn, tooltip, btn.getBackground());
            if (nuevo != null) {
                btn.setBackground(nuevo);
                if (onColorSelected != null) {
                    onColorSelected.accept(nuevo);
                }
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


    private JButton createCanvasResizeButton(ToolbarButtonDefinition btnDef, Color bg) {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setMinimumSize(new Dimension(24, 24));
        btn.setMaximumSize(new Dimension(24, 24));
        btn.setBackground(bg);
        btn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        btn.setFocusPainted(false);
        btn.setToolTipText(btnDef.textoTooltip());
        if (iconUtils != null) {
            var icon = iconUtils.getScaledIcon(btnDef.claveIcono(), 18, 18);
            if (icon != null) btn.setIcon(icon);
        }
        btn.addActionListener(e -> showCanvasResizeDialog());
        return btn;
    } // --- Fin del metodo createCanvasResizeButton ---


    private void showCanvasResizeDialog() {
        // Find the advance edit panel ancestor
        java.awt.Container parent = getParent();
        while (parent != null && !(parent instanceof vista.panels.render.AdvanceEditPanel)) {
            parent = parent.getParent();
        }
        if (!(parent instanceof vista.panels.render.AdvanceEditPanel aep)) return;

        var cm = aep.getCanvas().getCanvasModel();
        if (cm == null) return;

        javax.swing.JSpinner wSpinner = new javax.swing.JSpinner(
                new javax.swing.SpinnerNumberModel(cm.getWidth(), 1, 99999, 1));
        javax.swing.JSpinner hSpinner = new javax.swing.JSpinner(
                new javax.swing.SpinnerNumberModel(cm.getHeight(), 1, 99999, 1));
        wSpinner.setPreferredSize(new java.awt.Dimension(80, 22));
        hSpinner.setPreferredSize(new java.awt.Dimension(80, 22));

        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridBagLayout());
        var gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(4, 4, 4, 4);
        gbc.anchor = java.awt.GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new javax.swing.JLabel("Anchura:"), gbc);
        gbc.gridx = 1;
        panel.add(wSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new javax.swing.JLabel("Altura:"), gbc);
        gbc.gridx = 1;
        panel.add(hSpinner, gbc);

        int result = javax.swing.JOptionPane.showConfirmDialog(this, panel,
                "Tama\u00F1o del Lienzo", javax.swing.JOptionPane.OK_CANCEL_OPTION,
                javax.swing.JOptionPane.PLAIN_MESSAGE);

        if (result == javax.swing.JOptionPane.OK_OPTION) {
            int w = (Integer) wSpinner.getValue();
            int h = (Integer) hSpinner.getValue();
            cm.setSize(w, h);
            // Also resize existing layers to fit
            var lm = aep.getCanvas().getLayerModel();
            if (lm != null) {
                for (var layer : lm.getLayers()) {
                    var b = layer.getBounds();
                    if (b != null) {
                        layer.setBounds(new java.awt.Rectangle(
                                Math.min(b.x, w - 1), Math.min(b.y, h - 1),
                                Math.min(b.width, w), Math.min(b.height, h)));
                    }
                }
            }
            aep.getCanvas().repaint();
        }
    } // --- Fin del metodo showCanvasResizeDialog ---


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


    // ===================== HOME MODE =====================


    public void setHomeMode(boolean active) {
        this.homeActive = active;

        for (Component c : leftPartToolComponents) {
            c.setVisible(!active);
        }
        leftPartHome.setVisible(active);

        if (active) {
            // Deseleccionar todos los toggles del home
            for (Component c : leftPartHome.getComponents()) {
                if (c instanceof JToggleButton tb) {
                    tb.setSelected(false);
                }
            }
            cardLayout.show(toolControlsPanel, "home_empty");
        } else {
            ToolItem sel = (ToolItem) toolCombo.getSelectedItem();
            if (sel != null) {
                showOptionsFor(sel.commandKey());
            }
        }

        revalidate();
        repaint();
    } // --- Fin del metodo setHomeMode ---


    private void buildHomeRow1() {
        leftPartHome.removeAll();
        Color bg = getBackground();
        leftPartHome.add(Box.createHorizontalStrut(8));
        addHomeToggleButtonsFromDef(leftPartHome, "editoravanzadohometools", bg);
    } // --- Fin del metodo buildHomeRow1 ---


    private void buildHomePanels() {
        Color bg = getBackground();
        homeLienzoPanel = buildHomeLienzoPanel(bg);
        homeImagePanel = buildHomeImagePanel(bg);
        toolControlsPanel.add(homeLienzoPanel, "home_lienzo_medidas");
        toolOptionsMap.put("home_lienzo_medidas", homeLienzoPanel);
        toolControlsPanel.add(homeImagePanel, "home_imagen");
        toolOptionsMap.put("home_imagen", homeImagePanel);
        // Panel vacío para cuando ningún toggle está seleccionado
        toolControlsPanel.add(newEmptyPanel(bg), "home_empty");
    } // --- Fin del metodo buildHomePanels ---


    private JPanel buildHomeLienzoPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);

        p.add(Box.createHorizontalStrut(6));

        // Combo medidas standard
        String[] presets = {"Personalizado", "A3 (3508\u00D74961)", "A4 (2480\u00D73508)", "A5 (1748\u00D72480)",
                "Full HD (1920\u00D71080)", "HD (1280\u00D7720)", "Facebook (1200\u00D7630)",
                "Instagram (1080\u00D71080)", "YouTube (1280\u00D7720)"};
        int[][] presetValues = {{0, 0}, {3508, 4961}, {2480, 3508}, {1748, 2480},
                {1920, 1080}, {1280, 720}, {1200, 630},
                {1080, 1080}, {1280, 720}};
        JComboBox<String> presetCombo = new JComboBox<>(presets);
        presetCombo.setPreferredSize(new Dimension(140, 22));
        p.add(presetCombo);

        p.add(Box.createHorizontalStrut(4));

        // Spinner ancho + unidad
        JLabel lbW = new JLabel("Ancho:");
        lbW.setForeground(fgStatus);
        p.add(lbW);
        homeWSpinner = new JSpinner(new SpinnerNumberModel(1920, 1, 99999, 1));
        homeWSpinner.setPreferredSize(new Dimension(65, 20));
        p.add(homeWSpinner);

        JToggleButton unitBtn = new JToggleButton("px");
        unitBtn.setFont(unitBtn.getFont().deriveFont(9f));
        unitBtn.setPreferredSize(new Dimension(30, 20));
        unitBtn.setFocusPainted(false);
        p.add(unitBtn);

        p.add(Box.createHorizontalStrut(4));

        // Spinner alto + unidad
        JLabel lbH = new JLabel("Alto:");
        lbH.setForeground(fgStatus);
        p.add(lbH);
        homeHSpinner = new JSpinner(new SpinnerNumberModel(1080, 1, 99999, 1));
        homeHSpinner.setPreferredSize(new Dimension(65, 20));
        p.add(homeHSpinner);

        JToggleButton unitBtnH = new JToggleButton("px");
        unitBtnH.setFont(unitBtnH.getFont().deriveFont(9f));
        unitBtnH.setPreferredSize(new Dimension(30, 20));
        unitBtnH.setFocusPainted(false);
        p.add(unitBtnH);

        p.add(Box.createHorizontalStrut(4));

        // Color de fondo
        homeColorSwatch = new JButton();
        homeColorSwatch.setPreferredSize(new Dimension(20, 20));
        homeColorSwatch.setBackground(clr("Panel.background", 200, 200, 200));
        homeColorSwatch.setOpaque(true);
        homeColorSwatch.setBorder(BorderFactory.createLineBorder(clr("Component.borderColor", 120, 120, 120)));
        homeColorSwatch.setToolTipText("Color de fondo");
        homeColorSwatch.addActionListener(e -> {
            Color nuevo = JColorChooser.showDialog(homeColorSwatch, "Color de fondo", homeColorSwatch.getBackground());
            if (nuevo != null) homeColorSwatch.setBackground(nuevo);
        });
        p.add(homeColorSwatch);

        p.add(Box.createHorizontalStrut(8));

        // Aceptar + Cancelar
        addHomeButtonsFromDef(p, "editoravanzadoconfirmacion", bg, null);

        // Preset listener
        presetCombo.addActionListener(e -> {
            int idx = presetCombo.getSelectedIndex();
            if (idx > 0 && idx < presetValues.length) {
                homeWSpinner.setValue(presetValues[idx][0]);
                homeHSpinner.setValue(presetValues[idx][1]);
            }
        });

        // Unidad toggle listener
        final boolean[] isPx = {true};
        ActionListener unitListener = ev -> {
            isPx[0] = !isPx[0];
            String text = isPx[0] ? "px" : "mm";
            unitBtn.setText(text);
            unitBtnH.setText(text);
        };
        unitBtn.addActionListener(unitListener);
        unitBtnH.addActionListener(unitListener);

        return p;
    } // --- Fin del metodo buildHomeLienzoPanel ---


    private JPanel buildHomeImagePanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        addHomeButtonsFromDef(p, "editoravanzadohomeimage", bg, null);
        return p;
    } // --- Fin del metodo buildHomeImagePanel ---


    // ===================== HOME HELPERS =====================


    private void addHomeButtonsFromDef(JPanel panel, String toolbarKey, Color bg, ButtonGroup group) {
        ToolbarDefinition def = getSubDef(toolbarKey);
        if (def == null) return;
        for (ToolbarComponentDefinition comp : def.componentes()) {
            if (comp instanceof ToolbarButtonDefinition btnDef) {
                panel.add(createHomeButton(btnDef, bg, group));
            } else if (comp instanceof SeparatorDefinition) {
                panel.add(createSubSeparator(bg));
            }
        }
    } // --- Fin del metodo addHomeButtonsFromDef ---


    // ===================== HOME BUTTON CREATION =====================


    /** Crea botones toggle independientes (sin ButtonGroup) para home */
    private void addHomeToggleButtonsFromDef(JPanel panel, String toolbarKey, Color bg) {
        ToolbarDefinition def = getSubDef(toolbarKey);
        if (def == null) return;
        for (ToolbarComponentDefinition comp : def.componentes()) {
            if (comp instanceof ToolbarButtonDefinition btnDef) {
                panel.add(createHomeToggleButton(btnDef, bg));
            } else if (comp instanceof SeparatorDefinition) {
                panel.add(createSubSeparator(bg));
            }
        }
    } // --- Fin del metodo addHomeToggleButtonsFromDef ---


    private JToggleButton createHomeToggleButton(ToolbarButtonDefinition btnDef, Color bg) {
        String cmd = btnDef.comandoCanonico();
        JToggleButton btn = new JToggleButton();
        btn.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleHomeToggleAction(cmd, btn.isSelected());
            }
        });
        btn.getAction().putValue(Action.ACTION_COMMAND_KEY, cmd);
        btn.getAction().putValue(Action.SHORT_DESCRIPTION, btnDef.textoTooltip());
        if (iconUtils != null) {
            var icon = iconUtils.getScaledIcon(btnDef.claveIcono(), 16, 16);
            if (icon != null) btn.getAction().putValue(Action.SMALL_ICON, icon);
        }
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(24, 22));
        btn.setBackground(bg);
        btn.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        return btn;
    } // --- Fin del metodo createHomeToggleButton ---


    /** Crea botones de acción (JButton) para home */
    private AbstractButton createHomeButton(ToolbarButtonDefinition btnDef, Color bg, ButtonGroup group) {
        String cmd = btnDef.comandoCanonico();
        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleHomeAction(cmd);
            }
        };
        action.putValue(Action.ACTION_COMMAND_KEY, cmd);
        action.putValue(Action.SHORT_DESCRIPTION, btnDef.textoTooltip());
        if (iconUtils != null) {
            var icon = iconUtils.getScaledIcon(btnDef.claveIcono(), 16, 16);
            if (icon != null) action.putValue(Action.SMALL_ICON, icon);
        }

        AbstractButton btn;
        if (group != null) {
            JToggleButton tb = new JToggleButton(action);
            group.add(tb);
            btn = tb;
        } else {
            btn = new JButton(action);
        }
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(24, 22));
        btn.setBackground(bg);
        btn.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        return btn;
    } // --- Fin del metodo createHomeButton ---


    // ===================== HOME ACTION HANDLERS =====================


    private void handleHomeToggleAction(String cmd, boolean selected) {
        if (!selected) {
            cardLayout.show(toolControlsPanel, "home_empty");
            return;
        }
        // Al seleccionar uno, deseleccionar el otro
        for (Component c : leftPartHome.getComponents()) {
            if (c instanceof JToggleButton tb && tb != getToggleButtonFor(cmd)) {
                tb.setSelected(false);
            }
        }
        switch (cmd) {
            case AppActionCommands.CMD_HOME_LIENZO -> {
                if (toolOptionsMap.containsKey("home_lienzo_medidas")) {
                    cardLayout.show(toolControlsPanel, "home_lienzo_medidas");
                }
            }
            case AppActionCommands.CMD_HOME_CONTROL_IMAGEN -> {
                if (toolOptionsMap.containsKey("home_imagen")) {
                    cardLayout.show(toolControlsPanel, "home_imagen");
                }
            }
            default -> {}
        }
    } // --- Fin del metodo handleHomeToggleAction ---


    private AbstractButton getToggleButtonFor(String cmd) {
        for (Component c : leftPartHome.getComponents()) {
            if (c instanceof AbstractButton ab && cmd.equals(ab.getActionCommand())) {
                return ab;
            }
        }
        return null;
    } // --- Fin del metodo getToggleButtonFor ---


    private void handleHomeAction(String cmd) {
        if (cmd == null) return;

        AdvanceEditPanel aep = findAdvanceEditPanel();

        switch (cmd) {
            case AppActionCommands.CMD_HOME_LIENZO -> {
                if (toolOptionsMap.containsKey("home_lienzo_medidas")) {
                    cardLayout.show(toolControlsPanel, "home_lienzo_medidas");
                }
            }
            case AppActionCommands.CMD_HOME_CONTROL_IMAGEN -> {
                if (toolOptionsMap.containsKey("home_imagen")) {
                    cardLayout.show(toolControlsPanel, "home_imagen");
                }
            }
            case AppActionCommands.CMD_HOME_LIENZO_MEDIDAS -> {
                // Cambio de medidas: los controles ya están visibles en el panel
                if (toolOptionsMap.containsKey("home_lienzo_medidas")) {
                    cardLayout.show(toolControlsPanel, "home_lienzo_medidas");
                }
            }
            case AppActionCommands.CMD_HOME_LIENZO_NUEVO -> {
                int w = homeWSpinner != null ? (Integer) homeWSpinner.getValue() : 1920;
                int h = homeHSpinner != null ? (Integer) homeHSpinner.getValue() : 1080;
                Color bc = homeColorSwatch != null ? homeColorSwatch.getBackground() : new Color(200, 200, 200);
                crearNuevoLienzo(w, h, bc);
            }
            case AppActionCommands.CMD_HOME_ACEPTAR -> {
                int w = homeWSpinner != null ? (Integer) homeWSpinner.getValue() : 1920;
                int h = homeHSpinner != null ? (Integer) homeHSpinner.getValue() : 1080;
                Color bc = homeColorSwatch != null ? homeColorSwatch.getBackground() : new Color(200, 200, 200);
                aplicarMedidasLienzo(w, h, bc);
            }
            case AppActionCommands.CMD_HOME_CANCELAR -> cerrarHome();
            case AppActionCommands.CMD_HOME_IMPORTAR -> importarImagen();
            case AppActionCommands.CMD_HOME_EXPORTAR -> exportarImagen();
            case AppActionCommands.CMD_HOME_EXPORTAR_PREVIEW -> exportarAPreview();
            default -> {
                if (aep != null) aep.setActiveTool(cmd);
            }
        }
    } // --- Fin del metodo handleHomeAction ---


    // ===================== HOME ACTIONS =====================


    private AdvanceEditPanel findAdvanceEditPanel() {
        Container parent = getParent();
        while (parent != null && !(parent instanceof AdvanceEditPanel)) {
            parent = parent.getParent();
        }
        return (AdvanceEditPanel) parent;
    } // --- Fin del metodo findAdvanceEditPanel ---


    private void crearNuevoLienzo(int w, int h, Color bgColor) {
        AdvanceEditPanel aep = findAdvanceEditPanel();
        if (aep == null) return;

        var cm = aep.getCanvas().getCanvasModel();
        if (cm == null) return;

        // Limpiar capas existentes
        var lm = aep.getCanvas().getLayerModel();
        if (lm != null) {
            lm.clear();
        }

        cm.setSize(w, h);
        cm.setBackgroundColor(bgColor);
        cm.setTransparent(false);
        aep.getCanvas().repaint();

        cerrarHome();
    } // --- Fin del metodo crearNuevoLienzo ---


    private void aplicarMedidasLienzo(int w, int h, Color bgColor) {
        AdvanceEditPanel aep = findAdvanceEditPanel();
        if (aep == null) return;

        var cm = aep.getCanvas().getCanvasModel();
        if (cm == null) return;

        cm.setSize(w, h);
        cm.setBackgroundColor(bgColor);
        aep.getCanvas().repaint();

        cerrarHome();
    } // --- Fin del metodo aplicarMedidasLienzo ---


    private void cerrarHome() {
        AdvanceEditPanel aep = findAdvanceEditPanel();
        if (aep != null) {
            // Esto deselecciona la casa y vuelve al modo herramienta
            aep.setActiveTool(AppActionCommands.CMD_ADVANCED_EDITOR_EDICION);
        }
    } // --- Fin del metodo cerrarHome ---


    private void importarImagen() {
        AdvanceEditPanel aep = findAdvanceEditPanel();
        if (aep == null) return;

        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Imágenes (PNG, JPG, GIF, BMP)", "png", "jpg", "jpeg", "gif", "bmp"));
        if (fc.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;

        java.io.File file = fc.getSelectedFile();
        if (file == null) return;

        try {
            BufferedImage img = javax.imageio.ImageIO.read(file);
            if (img == null) return;

            var cm = aep.getCanvas().getCanvasModel();
            var lm = aep.getCanvas().getLayerModel();
            if (cm == null || lm == null) return;

            if (lm.size() == 0) {
                // Lienzo vacío → redimensionar canvas al tamaño de la imagen
                cm.setSize(img.getWidth(), img.getHeight());
            }

            modelo.editor.ImageLayer layer = new modelo.editor.ImageLayer(
                    file.getName(), img,
                    new java.awt.Rectangle(0, 0, img.getWidth(), img.getHeight()));
            lm.addLayer(layer);
            aep.getCanvas().repaint();
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al importar la imagen: " + ex.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }

        cerrarHome();
    } // --- Fin del metodo importarImagen ---


    private void exportarImagen() {
        AdvanceEditPanel aep = findAdvanceEditPanel();
        if (aep == null) return;

        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "PNG (*.png)", "png"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "JPEG (*.jpg)", "jpg", "jpeg"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "GIF (*.gif)", "gif"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "BMP (*.bmp)", "bmp"));
        if (fc.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;

        java.io.File file = fc.getSelectedFile();
        if (file == null) return;

        String name = file.getName().toLowerCase();
        String format;
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            format = "jpg";
        } else if (name.endsWith(".gif")) {
            format = "gif";
        } else if (name.endsWith(".bmp")) {
            format = "bmp";
        } else {
            format = "png";
            if (!name.endsWith(".png")) {
                file = new java.io.File(file.getAbsolutePath() + ".png");
            }
        }

        try {
            BufferedImage out = composeBufferedImage();
            if (out != null) javax.imageio.ImageIO.write(out, format, file);
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al exportar: " + ex.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }

        cerrarHome();
    } // --- Fin del metodo exportarImagen ---


    private void exportarAPreview() {
        BufferedImage out = composeBufferedImage();
        if (out == null) return;

        AdvanceEditPanel aep = findAdvanceEditPanel();
        Container parent = aep.getParent();
        while (parent != null && !(parent instanceof RenderPanel)) {
            parent = parent.getParent();
        }
        if (parent instanceof RenderPanel rp) {
            rp.set2DImage(out);
            rp.show2DView();
        }

        cerrarHome();
    } // --- Fin del metodo exportarAPreview ---


    private BufferedImage composeBufferedImage() {
        AdvanceEditPanel aep = findAdvanceEditPanel();
        if (aep == null) return null;

        var cm = aep.getCanvas().getCanvasModel();
        var lm = aep.getCanvas().getLayerModel();
        if (cm == null) return null;

        int w = cm.getWidth();
        int h = cm.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = out.createGraphics();
        if (!cm.isTransparent()) {
            g2.setColor(cm.getBackgroundColor());
            g2.fillRect(0, 0, w, h);
        }
        if (lm != null) {
            for (var layer : lm.getLayers()) {
                if (layer.isVisible() && layer.getOpacity() >= 0.01f) {
                    layer.paint(g2);
                }
            }
        }
        g2.dispose();
        return out;
    } // --- Fin del metodo composeBufferedImage ---


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
