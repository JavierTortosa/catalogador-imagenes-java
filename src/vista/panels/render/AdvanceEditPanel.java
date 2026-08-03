package vista.panels.render;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import controlador.actions.editoravanzado.AutoDistributeActions;
import controlador.actions.editoravanzado.EditorToolAction;
import controlador.commands.AppActionCommands;
import controlador.tools.CanvasController;
import controlador.tools.TextTool;
import controlador.tools.Tool;
import controlador.utils.EditorHotkeys;
import modelo.editor.CanvasModel;
import modelo.editor.ImageLayer;
import modelo.editor.LayerModel;
import modelo.editor.TextLayer;
import vista.config.SeparatorDefinition;
import vista.config.ToolbarButtonDefinition;
import vista.config.ToolbarComponentDefinition;
import vista.config.ToolbarDefinition;
import vista.config.UIDefinitionService;
import vista.components.ThemedToggleButton;
import vista.renderers.ToolBadgeIcon;
import vista.theme.Tema;
import vista.theme.ThemeChangeListener;
import vista.theme.ThemeManager;
import vista.util.IconUtils;

public class AdvanceEditPanel extends JPanel implements ThemeChangeListener {

    private static final long serialVersionUID = 1L;

    // --- Ancho adaptativo de la columna de herramientas ---
    // Cuando se colapsa el preview, el panel crece en ancho: la mitad del espacio
    // extra va a la columna (para que las tarjetas de capa no queden recortadas
    // por los scrolls) y la otra mitad al canvas, con tope para no desperdiciar lienzo.
    private static final int TOOLS_MIN_WIDTH = 200;
    private static final int TOOLS_MAX_WIDTH = 320;
    private int lastSplitWidth = -1;
    private int toolsWidth = TOOLS_MIN_WIDTH;

	private final CanvasPanel canvasPanel;
    private final JPanel advanceEditToolsPanel;
    private final JPanel toolbarContainer;
    private final JPanel mainContent;
    private final JSplitPane advanceEditSplit;
    private final EditorComponentBar componentBar;
    private final JLabel toolsTitle;
    private boolean advanceEditToolsVisible;
    private boolean advanceEditActive;
    private boolean homeActive;
    private LayerCardPanel layerCardPanel;
    private modelo.editor.LayerModel editorLayerModel;
    private IconUtils iconUtils;
    private UIDefinitionService uiDefinitionService;
    private ThemeManager themeManager;
    private int iconWidth = 24;
    private int iconHeight = 24;
    private boolean syncingTools;
    private javax.swing.JToggleButton homeButton;
    private CanvasController canvasController;

    // --- Retención de controles de texto del panel derecho ---
    private JComboBox<String> rightFontCombo;
    private JComboBox<Integer> rightSizeCombo;
    private JToggleButton rightBoldBtn;
    private JToggleButton rightItalicBtn;
    private JToggleButton rightUnderlineBtn;
    private JToggleButton rightStrikethroughBtn;
    private JToggleButton rightAlignLeftBtn;
    private JToggleButton rightAlignCenterBtn;
    private JToggleButton rightAlignRightBtn;
    private JToggleButton rightJustifiedBtn;
    private JToggleButton rightFlowRowsBtn;
    private JToggleButton rightFlowColumnsBtn;
    private JToggleButton rightHorizontalBtn;
    private JToggleButton rightVerticalBtn;

    // Colores temáticos (inicializados desde UIManager con fallbacks oscuros)
    private Color bgMain = clr("TabbedPane.contentAreaColor", 40, 40, 45);
    private Color bgTools = clr("Panel.background", 50, 50, 55);
    private Color bgToolbar = clr("Panel.background", 45, 45, 50);
    private Color bgHeader = clr("TabbedPane.contentAreaColor", 48, 48, 53);
    private Color fgToolbar = clr("Label.foreground", 240, 240, 245);
    private Color borderColor = clr("Component.borderColor", 60, 60, 65);
    private Color fgSectionTitle = clr("Label.disabledForeground", 180, 180, 190);

    // Colores frontal/fondo (estilo Photoshop) de la barra izquierda
    private Color colorFrontal = Color.WHITE;
    private Color colorFondo = Color.BLACK;
    private JComponent colorBoxesComponent;

    private static Color clr(String key, int r, int g, int b) {
        Color c = javax.swing.UIManager.getColor(key);
        return c != null ? c : new Color(r, g, b);
    }

    public AdvanceEditPanel() {
        setLayout(new BorderLayout());
        setBackground(bgMain);

        componentBar = new EditorComponentBar();
        componentBar.getToolCombo().addActionListener(e -> {
            if (syncingTools) return;
            syncingTools = true;
            try {
                EditorComponentBar.ToolItem item = (EditorComponentBar.ToolItem) componentBar.getToolCombo().getSelectedItem();
                if (item != null) {
                    setActiveTool(item.commandKey());
                }
            } finally {
                syncingTools = false;
            }
        });

        // Fila superior: casa + componentBar
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(bgToolbar);
        topRow.add(createHomePanel(), BorderLayout.WEST);
        topRow.add(componentBar, BorderLayout.CENTER);
        add(topRow, BorderLayout.NORTH);

        mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(bgMain);

        toolbarContainer = new JPanel();
        toolbarContainer.setLayout(new BoxLayout(toolbarContainer, BoxLayout.Y_AXIS));
        toolbarContainer.setBackground(bgToolbar);
        toolbarContainer.setPreferredSize(new Dimension(36, 0));
        toolbarContainer.setMinimumSize(new Dimension(36, 0));
        mainContent.add(toolbarContainer, BorderLayout.WEST);

        advanceEditToolsVisible = true;

        advanceEditToolsPanel = new JPanel(new BorderLayout());
        advanceEditToolsPanel.setBackground(bgTools);
        advanceEditToolsPanel.setPreferredSize(new Dimension(160, 0));
        advanceEditToolsPanel.setMinimumSize(new Dimension(0, 0));

        JPanel toolsHeader = new JPanel(new BorderLayout());
        toolsHeader.setBackground(bgToolbar);
        toolsHeader.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
        toolsTitle = new JLabel("Herramientas");
        toolsTitle.setForeground(fgToolbar);
        toolsHeader.add(toolsTitle, BorderLayout.WEST);
        advanceEditToolsPanel.add(toolsHeader, BorderLayout.NORTH);

        JPanel toolsContent = new JPanel(new BorderLayout());
        toolsContent.setBackground(bgTools);
        advanceEditToolsPanel.add(toolsContent, BorderLayout.CENTER);

        advanceEditSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        advanceEditSplit.setBackground(bgMain);
        advanceEditSplit.setBorder(null);
        advanceEditSplit.setResizeWeight(1.0);
        advanceEditSplit.setDividerSize(6);
        advanceEditSplit.setOneTouchExpandable(true);
        advanceEditSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            JSplitPane sp = (JSplitPane) evt.getSource();
            int loc = (Integer) evt.getNewValue();
            int maxLoc = sp.getWidth() - sp.getDividerSize() - sp.getRightComponent().getMinimumSize().width;
            advanceEditToolsVisible = loc < maxLoc - 10;
            // Sincronizar el estado del ancho adaptativo con la realidad del divider
            if (sp.getWidth() > 0) {
                toolsWidth = Math.max(TOOLS_MIN_WIDTH, sp.getWidth() - loc);
                lastSplitWidth = sp.getWidth();
            }
        });

        // Al crecer el panel (p. ej. colapsar el preview), repartir el espacio extra:
        // mitad al canvas, mitad a la columna de herramientas (con tope)
        advanceEditSplit.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustToolsWidth();
            }
        });

        canvasPanel = new CanvasPanel();
        canvasPanel.setBackground(bgMain);
        // El cuentagotas publica "foregroundColor" en el canvas → actualiza el frontal de la barra
        canvasPanel.addPropertyChangeListener("foregroundColor", evt -> {
            if (evt.getNewValue() instanceof Color c) {
                setColorFrontal(c);
            }
        });
        advanceEditSplit.setLeftComponent(canvasPanel);
        advanceEditSplit.setRightComponent(advanceEditToolsPanel);
        advanceEditSplit.setDividerLocation(Integer.MAX_VALUE);

        mainContent.add(advanceEditSplit, BorderLayout.CENTER);
        add(mainContent, BorderLayout.CENTER);

        installEscapeBinding();
    } // --- Fin del constructor AdvanceEditPanel ---


    /**
     * Reparte el espacio extra de ancho del panel entre el canvas y la columna
     * de herramientas: la mitad para cada uno, con tope en la columna para no
     * quitar demasiado lienzo. Solo actúa mientras las herramientas estén visibles.
     */
    private void adjustToolsWidth() {
        if (!advanceEditToolsVisible) return;
        int w = advanceEditSplit.getWidth();
        if (w <= 0) return;
        if (lastSplitWidth < 0) {
            lastSplitWidth = w;
            return;
        }
        int delta = w - lastSplitWidth;
        lastSplitWidth = w;
        if (delta == 0) return;
        toolsWidth = Math.max(TOOLS_MIN_WIDTH,
                Math.min(TOOLS_MAX_WIDTH, toolsWidth + delta / 2));
        int actual = w - advanceEditSplit.getDividerLocation();
        if (Math.abs(actual - toolsWidth) > 6) {
            advanceEditSplit.setDividerLocation(w - toolsWidth);
        }
    } // --- Fin del metodo adjustToolsWidth ---


    private void installEscapeBinding() {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "editor.escape");
        getActionMap().put("editor.escape", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                handleEscapeLocal();
            }
        });
    } // --- Fin del metodo installEscapeBinding ---


    private void handleEscapeLocal() {
        if (canvasController != null) {
            canvasController.handleEscape();
        }
    } // --- Fin del metodo handleEscapeLocal ---


    private JPanel createHomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(bgToolbar);
        panel.setPreferredSize(new Dimension(36, 48));
        panel.setMinimumSize(new Dimension(36, 48));
        panel.setMaximumSize(new Dimension(36, 48));

        homeButton = new javax.swing.JToggleButton();
        homeButton.setPreferredSize(new Dimension(36, 48));
        homeButton.setMinimumSize(new Dimension(36, 48));
        homeButton.setMaximumSize(new Dimension(36, 48));
        homeButton.setBackground(bgToolbar);
        homeButton.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, borderColor));
        homeButton.setFocusPainted(false);
        homeButton.setToolTipText("Men\u00FA principal");
        if (iconUtils != null) {
            var icon = iconUtils.getScaledIcon("90000-home.png", 28, 28);
            if (icon != null) homeButton.setIcon(icon);
        }
        homeButton.addActionListener(e -> toggleHomeMode());
        panel.add(homeButton, BorderLayout.CENTER);

        return panel;
    } // --- Fin del metodo createHomePanel ---


    private void toggleHomeMode() {
        homeActive = homeButton.isSelected();
        componentBar.setHomeMode(homeActive);
        if (homeActive) {
            // Deseleccionar todas las herramientas
            for (java.awt.Component c : toolbarContainer.getComponents()) {
                if (c instanceof JPanel) {
                    for (java.awt.Component child : ((JPanel) c).getComponents()) {
                        if (child instanceof javax.swing.JToggleButton tb) {
                            tb.setSelected(false);
                        }
                    }
                }
            }
        } else {
            // Restaurar la herramienta activa
            String activeCmd = getActiveToolCommand();
            if (activeCmd == null) {
                componentBar.selectToolByCommand(AppActionCommands.CMD_ADVANCED_EDITOR_EDICION);
            }
        }
    } // --- Fin del metodo toggleHomeMode ---


    private String getActiveToolCommand() {
        for (java.awt.Component c : toolbarContainer.getComponents()) {
            if (c instanceof JPanel) {
                for (java.awt.Component child : ((JPanel) c).getComponents()) {
                    if (child instanceof javax.swing.JToggleButton tb && tb.isSelected()) {
                        Object cmd = tb.getAction().getValue(Action.ACTION_COMMAND_KEY);
                        if (cmd instanceof String) return (String) cmd;
                    }
                }
            }
        }
        return null;
    } // --- Fin del metodo getActiveToolCommand ---


    private void applyTheme(Tema tema) {
        bgMain = tema.colorFondoSecundario();
        bgTools = tema.colorFondoPrincipal();
        bgToolbar = tema.colorFondoPrincipal();
        bgHeader = tema.colorFondoPrincipal();
        fgToolbar = tema.colorTextoPrimario();
        borderColor = tema.colorBorde();
        fgSectionTitle = tema.colorTextoSecundario();

        setBackground(bgMain);
        componentBar.updateTheme(bgToolbar, fgToolbar, borderColor);
        toolbarContainer.setBackground(bgToolbar);
        mainContent.setBackground(bgMain);
        advanceEditSplit.setBackground(bgMain);
        advanceEditSplit.setDividerSize(6);
        advanceEditToolsPanel.setBackground(bgTools);

        for (java.awt.Component c : advanceEditToolsPanel.getComponents()) {
            if (c instanceof JPanel h && ((BorderLayout) advanceEditToolsPanel.getLayout()).getLayoutComponent(BorderLayout.NORTH) == h) {
                h.setBackground(bgToolbar);
                for (java.awt.Component child : ((JPanel) h).getComponents()) {
                    if (child instanceof JLabel) child.setForeground(fgToolbar);
                }
            }
        }

        canvasPanel.setBackground(bgMain);
        toolsTitle.setForeground(fgToolbar);

        if (layerCardPanel != null) {
            layerCardPanel.updateTheme(bgTools, borderColor, fgToolbar,
                    tema.colorSeleccionFondo());
        }

        rebuildTools();
    } // --- Fin del metodo applyTheme ---


    @Override
    public void onThemeChanged(Tema tema) {
        SwingUtilities.invokeLater(() -> applyTheme(tema));
    } // --- Fin del metodo onThemeChanged ---


    public void setThemeManager(ThemeManager themeManager) {
        this.themeManager = themeManager;
        if (themeManager != null) {
            themeManager.addThemeChangeListener(this);
            applyTheme(themeManager.getTemaActual());
        }
    } // --- Fin del metodo setThemeManager ---


    public void setActive(boolean active) {
        this.advanceEditActive = active;
        if (active) {
            advanceEditToolsVisible = true;
            javax.swing.SwingUtilities.invokeLater(() -> {
                JSplitPane sp = getSplit();
                if (sp != null && sp.isShowing()) {
                    int w = sp.getWidth();
                    if (w > TOOLS_MIN_WIDTH + 20) {
                        sp.setDividerLocation(w - TOOLS_MIN_WIDTH);
                    }
                }
            });
        }
    } // --- Fin del metodo setActive ---


    public boolean isActive() {
        return advanceEditActive;
    } // --- Fin del metodo isActive ---


    public CanvasPanel getCanvas() {
        return canvasPanel;
    } // --- Fin del metodo getCanvas ---

    public void setLayerModel(modelo.editor.LayerModel layerModel) {
        this.editorLayerModel = layerModel;
        canvasPanel.setLayerModel(layerModel);
        if (layerCardPanel != null) {
            layerCardPanel.setLayerModel(layerModel);
        }
        // Refresco automático ante cualquier cambio en el modelo de capas
        layerModel.setChangeListener(() -> {
            if (layerCardPanel != null) {
                layerCardPanel.rebuild();
            }
            if (canvasPanel != null) {
                canvasPanel.repaint();
            }
        });
    } // --- Fin del metodo setLayerModel ---

    public void setCanvasModel(CanvasModel canvasModel) {
        canvasPanel.setCanvasModel(canvasModel);
    } // --- Fin del metodo setCanvasModel ---


    public JPanel getToolsPanel() {
        return advanceEditToolsPanel;
    } // --- Fin del metodo getToolsPanel ---


    public JPanel getToolsContent() {
        return (JPanel) ((BorderLayout) advanceEditToolsPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
    } // --- Fin del metodo getToolsContent ---


    public JPanel getToolbarContainer() {
        return toolbarContainer;
    } // --- Fin del metodo getToolbarContainer ---


    public EditorComponentBar getComponentBar() {
        return componentBar;
    } // --- Fin del metodo getComponentBar ---


    public void addToolbarGroup(String groupTitle, javax.swing.JButton... buttons) {
        if (buttons.length == 0) return;
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setBackground(bgToolbar);
        for (javax.swing.JButton btn : buttons) {
            btn.setAlignmentX(javax.swing.JComponent.CENTER_ALIGNMENT);
            btn.setPreferredSize(new Dimension(28, 28));
            btn.setMaximumSize(new Dimension(28, 28));
            btn.setMinimumSize(new Dimension(28, 28));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            group.add(btn);
        }
        toolbarContainer.add(group);
        toolbarContainer.add(new JSeparator(javax.swing.SwingConstants.HORIZONTAL));
    } // --- Fin del metodo addToolbarGroup ---


    public JSplitPane getSplit() {
        for (java.awt.Component c : getComponents()) {
            if (c instanceof JSplitPane) return (JSplitPane) c;
        }
        JPanel main = (JPanel) ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (main != null) {
            for (java.awt.Component c : main.getComponents()) {
                if (c instanceof JSplitPane) return (JSplitPane) c;
            }
        }
        return null;
    } // --- Fin del metodo getSplit ---


    public JPanel getToolControlsPanel() {
        return componentBar.getToolControlsPanel();
    } // --- Fin del metodo getToolControlsPanel ---


    public void setActiveTool(String commandKey) {
        if (homeActive) {
            homeActive = false;
            homeButton.setSelected(false);
            componentBar.setHomeMode(false);
        }
        componentBar.selectToolByCommand(commandKey);
        selectToolButton(commandKey);
        if (canvasController != null) {
            canvasController.setActiveTool(commandKey);
        }
    } // --- Fin del metodo setActiveTool ---


    public void setCanvasController(CanvasController cc) {
        this.canvasController = cc;
        componentBar.setCanvasController(cc);
        componentBar.setOnTextChange(this::syncToolsFromBar);
        componentBar.setOnShapeChange(this::syncToolsFromBar);
        canvasController.setFullscreenToggle(this::toggleFullscreen);
        canvasController.setFullscreenEscapeHandler(() -> {
            RenderPanel rp = (RenderPanel) javax.swing.SwingUtilities
                    .getAncestorOfClass(RenderPanel.class, this);
            if (rp != null && rp.isFullscreen()) {
                rp.toggleEditorFullscreen();
                return true;
            }
            return false;
        });
        layerCardPanel.setOnDoubleClick(textLayer -> {
            if (canvasController != null) {
                TextTool tt = (TextTool) canvasController.getTool(AppActionCommands.CMD_ADVANCED_EDITOR_TEXTO);
                if (tt != null) {
                    canvasController.setActiveTool(AppActionCommands.CMD_ADVANCED_EDITOR_TEXTO);
                    tt.editExistingLayer(textLayer);
                }
            }
        });
    } // --- Fin del metodo setCanvasController ---


    private void selectToolButton(String commandKey) {
        for (java.awt.Component c : toolbarContainer.getComponents()) {
            if (c instanceof JPanel) {
                for (java.awt.Component child : ((JPanel) c).getComponents()) {
                    if (child instanceof JToggleButton) {
                        Action a = ((JToggleButton) child).getAction();
                        if (a != null && commandKey.equals(a.getValue(Action.ACTION_COMMAND_KEY))) {
                            ((JToggleButton) child).setSelected(true);
                            return;
                        }
                    }
                }
            }
        }
    } // --- Fin del metodo selectToolButton ---


    // -----------------------------------------------------------------------
    // Construcción del contenido del panel de herramientas (lado derecho)
    // -----------------------------------------------------------------------
    private void buildDefaultTools() {
        JPanel tc = getToolsContent();
        tc.setLayout(new BorderLayout());

        JScrollPane scroll = new JScrollPane();
        scroll.setBorder(null);
        scroll.getViewport().setBackground(bgTools);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(bgTools);
        container.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        if (iconUtils != null && uiDefinitionService != null) {
            container.add(createSectionFromDefs("Alinear",
                    uiDefinitionService.getComponentesLayerAlign(), 3));
            container.add(Box.createVerticalStrut(2));
            container.add(createSectionFromDefs("Distribuir",
                    uiDefinitionService.getComponentesLayerDistribute(), 3));
            container.add(Box.createVerticalStrut(2));
            container.add(createSectionFromDefs("Espacio",
                    uiDefinitionService.getComponentesLayerDistributeSpace(), 2));
            container.add(Box.createVerticalStrut(2));
            container.add(createSectionFromDefs("Auto distribuir",
                    uiDefinitionService.getComponentesLayerAutoDistribute(), 2));
        }

        // --- Texto (tabs: alineamiento + fuentes) ---
        JTabbedPane textTabs = new JTabbedPane();
        textTabs.setBackground(bgToolbar);
        textTabs.setForeground(fgToolbar);
        textTabs.setFont(textTabs.getFont().deriveFont(10f));
        textTabs.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        textTabs.setPreferredSize(new Dimension(160, 140));
        textTabs.setMinimumSize(new Dimension(160, 80));

        // TAB ALINEAMIENTO
        JPanel alineacionPanel = new JPanel();
        alineacionPanel.setLayout(new BoxLayout(alineacionPanel, BoxLayout.Y_AXIS));
        alineacionPanel.setBackground(bgTools);

        JPanel alignRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
        alignRow.setBackground(bgTools);
        JPanel flowRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
        flowRow.setBackground(bgTools);
        JPanel orientRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
        orientRow.setBackground(bgTools);

        ButtonGroup alignGroup = new ButtonGroup();
        ButtonGroup flowGroup = new ButtonGroup();
        ButtonGroup orientGroup = new ButtonGroup();

        if (uiDefinitionService != null && iconUtils != null) {
            List<ToolbarComponentDefinition> textoDefs = uiDefinitionService.getToolbarDefinition("editoravanzadotexto").componentes();
            for (ToolbarComponentDefinition comp : textoDefs) {
                if (comp instanceof ToolbarButtonDefinition btnDef) {
                    String cmd = btnDef.comandoCanonico();
                    if (AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_LEFT.equals(cmd)) {
                        rightAlignLeftBtn = createRightTextButton(btnDef, alignGroup,
                                b -> rightSetAlignment(SwingConstants.LEFT));
                        alignRow.add(rightAlignLeftBtn);
                    } else if (AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_CENTER.equals(cmd)) {
                        rightAlignCenterBtn = createRightTextButton(btnDef, alignGroup,
                                b -> rightSetAlignment(SwingConstants.CENTER));
                        alignRow.add(rightAlignCenterBtn);
                    } else if (AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_RIGHT.equals(cmd)) {
                        rightAlignRightBtn = createRightTextButton(btnDef, alignGroup,
                                b -> rightSetAlignment(SwingConstants.RIGHT));
                        alignRow.add(rightAlignRightBtn);
                    } else if (AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_JUSTIFY.equals(cmd)) {
                        rightJustifiedBtn = createRightTextButton(btnDef, alignGroup,
                                b -> rightSetAlignment(modelo.editor.TextLayer.ALIGN_JUSTIFY));
                        alignRow.add(rightJustifiedBtn);
                    } else if (AppActionCommands.CMD_EDITOR_TEXTO_FLOW_ROWS.equals(cmd)) {
                        rightFlowRowsBtn = createRightTextButton(btnDef, flowGroup,
                                b -> rightSetFlowColumns(false));
                        flowRow.add(rightFlowRowsBtn);
                    } else if (AppActionCommands.CMD_EDITOR_TEXTO_FLOW_COLUMNS.equals(cmd)) {
                        rightFlowColumnsBtn = createRightTextButton(btnDef, flowGroup,
                                b -> rightSetFlowColumns(true));
                        flowRow.add(rightFlowColumnsBtn);
                    } else if (AppActionCommands.CMD_EDITOR_TEXTO_HORIZONTAL.equals(cmd)) {
                        rightHorizontalBtn = createRightTextButton(btnDef, orientGroup,
                                b -> rightSetVertical(false));
                        orientRow.add(rightHorizontalBtn);
                    } else if (AppActionCommands.CMD_EDITOR_TEXTO_VERTICAL.equals(cmd)) {
                        rightVerticalBtn = createRightTextButton(btnDef, orientGroup,
                                b -> rightSetVertical(true));
                        orientRow.add(rightVerticalBtn);
                    }
                }
            }
        }
        alineacionPanel.add(alignRow);
        alineacionPanel.add(flowRow);
        alineacionPanel.add(orientRow);
        textTabs.addTab("Alineamiento", alineacionPanel);

        // TAB FUENTES
        JPanel fuentesPanel = new JPanel();
        fuentesPanel.setLayout(new BoxLayout(fuentesPanel, BoxLayout.Y_AXIS));
        fuentesPanel.setBackground(bgTools);

        JPanel fontRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
        fontRow.setBackground(bgTools);
        JPanel styleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
        styleRow.setBackground(bgTools);
        JPanel sizeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
        sizeRow.setBackground(bgTools);

        ButtonGroup styleGroup = new ButtonGroup();

        if (uiDefinitionService != null && iconUtils != null) {
            List<ToolbarComponentDefinition> textoDefs = uiDefinitionService.getToolbarDefinition("editoravanzadotexto").componentes();
            for (ToolbarComponentDefinition comp : textoDefs) {
                if (comp instanceof ToolbarButtonDefinition btnDef) {
                    String cmd = btnDef.comandoCanonico();
                    String icon = btnDef.claveIcono();
                    if (AppActionCommands.CMD_EDITOR_TEXTO_FUENTE.equals(cmd)) {
                        if (iconUtils != null) {
                            fontRow.add(new JLabel(iconUtils.getScaledIcon(icon, 16, 16)));
                        }
                        String[] fonts = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
                        rightFontCombo = new JComboBox<>(fonts);
                        rightFontCombo.setPreferredSize(new Dimension(130, 22));
                        fontRow.add(rightFontCombo);
                        rightFontCombo.addActionListener(e -> {
                            if (syncingTools) return;
                            String sel = (String) rightFontCombo.getSelectedItem();
                            if (sel != null) rightSetFontFamily(sel);
                        });
                    } else if (AppActionCommands.CMD_EDITOR_TEXTO_TAMANO.equals(cmd)) {
                        if (iconUtils != null) {
                            sizeRow.add(new JLabel(iconUtils.getScaledIcon(icon, 16, 16)));
                        }
                        Integer[] sizes = {8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 36, 48, 72};
                        rightSizeCombo = new JComboBox<>(sizes);
                        rightSizeCombo.setSelectedItem(12);
                        rightSizeCombo.setPreferredSize(new Dimension(55, 22));
                        sizeRow.add(rightSizeCombo);
                        rightSizeCombo.addActionListener(e -> {
                            if (syncingTools) return;
                            Integer sel = (Integer) rightSizeCombo.getSelectedItem();
                            if (sel != null) rightSetFontSize(sel);
                        });
                    } else if (AppActionCommands.CMD_EDITOR_TEXTO_NEGRITA.equals(cmd)) {
                        rightBoldBtn = createRightTextButton(btnDef, styleGroup,
                                b -> rightSetBold(b.isSelected()));
                        styleRow.add(rightBoldBtn);
                    } else if (AppActionCommands.CMD_EDITOR_TEXTO_CURSIVA.equals(cmd)) {
                        rightItalicBtn = createRightTextButton(btnDef, styleGroup,
                                b -> rightSetItalic(b.isSelected()));
                        styleRow.add(rightItalicBtn);
                    } else if (AppActionCommands.CMD_EDITOR_TEXTO_SUBRAYADO.equals(cmd)) {
                        rightUnderlineBtn = createRightTextButton(btnDef, styleGroup,
                                b -> rightSetUnderline(b.isSelected()));
                        styleRow.add(rightUnderlineBtn);
                    } else if (AppActionCommands.CMD_EDITOR_TEXTO_TACHADO.equals(cmd)) {
                        rightStrikethroughBtn = createRightTextButton(btnDef, styleGroup,
                                b -> rightSetStrikethrough(b.isSelected()));
                        styleRow.add(rightStrikethroughBtn);
                    }
                }
            }
        }
        fuentesPanel.add(fontRow);
        fuentesPanel.add(styleRow);
        fuentesPanel.add(sizeRow);
        textTabs.addTab("Fuentes", fuentesPanel);

        container.add(createSection("Texto", textTabs, false));

        // --- Capas (tabs: capas + orden) ---
        JTabbedPane capasTabs = new JTabbedPane();
        capasTabs.setBackground(bgToolbar);
        capasTabs.setForeground(fgToolbar);
        capasTabs.setFont(capasTabs.getFont().deriveFont(10f));
        capasTabs.setPreferredSize(new Dimension(160, 150));
        capasTabs.setMinimumSize(new Dimension(160, 100));

        layerCardPanel = new LayerCardPanel();
        layerCardPanel.updateTheme(bgTools, borderColor, fgSectionTitle, new Color(65, 90, 120));

        JPanel capasTabPanel = new JPanel(new BorderLayout());
        capasTabPanel.setBackground(bgTools);
        capasTabPanel.add(layerCardPanel, BorderLayout.CENTER);

        JPanel opacityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        opacityPanel.setBackground(bgTools);
        JLabel opLbl = new JLabel("Opacidad:");
        opLbl.setForeground(fgToolbar);
        opLbl.setFont(opLbl.getFont().deriveFont(10f));
        opacityPanel.add(opLbl);
        javax.swing.JSlider opSlider = new javax.swing.JSlider(0, 100, 100);
        opSlider.setBackground(bgTools);
        opSlider.setPreferredSize(new Dimension(80, 20));
        JLabel opVal = new JLabel("100%");
        opVal.setForeground(fgToolbar);
        opVal.setFont(opVal.getFont().deriveFont(10f));
        opSlider.addChangeListener(e -> {
            int v = opSlider.getValue();
            opVal.setText(v + "%");
            if (editorLayerModel != null) {
                var active = editorLayerModel.getActiveLayer();
                if (active != null) {
                    active.setOpacity(v / 100f);
                    canvasPanel.repaint();
                }
            }
        });
        opacityPanel.add(opSlider);
        opacityPanel.add(opVal);
        capasTabPanel.add(opacityPanel, BorderLayout.SOUTH);

        capasTabs.addTab("Capas", capasTabPanel);

        // Pestaña "Orden": botones de orden
        JPanel ordenPanel = new JPanel(new BorderLayout());
        ordenPanel.setBackground(bgTools);
        ordenPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        if (iconUtils != null && uiDefinitionService != null) {
            JPanel orderGrid = new JPanel(new GridLayout(2, 2, 5, 5));
            orderGrid.setBackground(bgTools);
            for (ToolbarComponentDefinition def : uiDefinitionService.getComponentesLayerOrderPreviewRender()) {
                if (def instanceof ToolbarButtonDefinition btnDef) {
                    orderGrid.add(wrapInCell(createLayerActionButton(btnDef)));
                }
            }
            ordenPanel.add(orderGrid, BorderLayout.NORTH);
        }
        capasTabs.addTab("Orden", ordenPanel);

        JPanel capasContent = new JPanel(new BorderLayout());
        capasContent.setBackground(bgTools);
        capasContent.add(capasTabs, BorderLayout.CENTER);

        if (iconUtils != null && uiDefinitionService != null) {
            JPanel layerToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
            layerToolbar.setBackground(bgTools);
            for (ToolbarComponentDefinition def : uiDefinitionService.getComponentesLayerLoadPreviewRender()) {
                if (def instanceof ToolbarButtonDefinition btnDef) {
                    String cmd = btnDef.comandoCanonico();
                    if (AppActionCommands.CMD_PREVIEW_RENDER_ADD_LAYER.equals(cmd)) {
                        layerToolbar.add(createAddLayerButton(btnDef));
                    } else if (AppActionCommands.CMD_PREVIEW_RENDER_DUPLICATE_LAYER.equals(cmd)
                            || AppActionCommands.CMD_PREVIEW_RENDER_DELETE_LAYER.equals(cmd)) {
                        layerToolbar.add(createLayerActionButton(btnDef));
                    } else {
                        layerToolbar.add(createButtonFromDef(btnDef));
                    }
                }
            }
            capasContent.add(layerToolbar, BorderLayout.SOUTH);
        }

        container.add(Box.createVerticalStrut(2));
        container.add(createSection("Capas", capasContent, true));

        scroll.setViewportView(container);
        tc.add(scroll, BorderLayout.CENTER);
    } // --- Fin del metodo buildDefaultTools ---


    private JPanel createSection(String title, JComponent content) {
        return createSection(title, content, false);
    } // --- Fin del metodo createSection ---


    private JPanel createSection(String title, JComponent content, boolean stretch) {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(bgTools);

        section.setAlignmentX(LEFT_ALIGNMENT);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        header.setBackground(bgHeader);
        header.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, borderColor));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel arrowLbl = new JLabel("\u25BC");
        arrowLbl.setForeground(fgSectionTitle);
        arrowLbl.setFont(new Font("SansSerif", Font.PLAIN, 9));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setForeground(fgSectionTitle);
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 11f));

        header.add(arrowLbl);
        header.add(titleLbl);
        section.add(header, BorderLayout.NORTH);

        section.add(content, BorderLayout.CENTER);

        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean visible = !content.isVisible();
                content.setVisible(visible);
                arrowLbl.setText(visible ? "\u25BC" : "\u25B6");
                if (visible) {
                    section.invalidate();
                    if (stretch) {
                        section.setMaximumSize(null);
                    } else {
                        int prefH = section.getPreferredSize().height;
                        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, prefH));
                    }
                } else {
                    int h = header.getPreferredSize().height;
                    section.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
                }
                SwingUtilities.invokeLater(() -> {
                    Container p = section.getParent();
                    if (p != null) p.revalidate();
                });
            }
        });

        return section;
    } // --- Fin del metodo createSection (stretch) ---


    private JPanel createSectionFromDefs(String title, List<ToolbarComponentDefinition> defs, int columns) {
        JPanel grid = new JPanel(new GridLayout(0, columns, 5, 5));
        grid.setBackground(bgTools);
        grid.setBorder(BorderFactory.createEmptyBorder(2, 8, 6, 8));
        for (ToolbarComponentDefinition def : defs) {
            if (def instanceof ToolbarButtonDefinition btnDef) {
                grid.add(wrapInCell(createButtonFromDef(btnDef)));
            }
        }
        return createSection(title, grid, false);
    } // --- Fin del metodo createSectionFromDefs ---


    private JButton createButtonFromDef(ToolbarButtonDefinition def) {
        String cmd = def.comandoCanonico();
        Action action;
        if (AppActionCommands.CMD_PREVIEW_RENDER_AUTO_DISTRIBUTE_FIXED.equals(cmd)) {
            action = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    AutoDistributeActions.distribucionFija(canvasPanel, editorLayerModel, canvasPanel.getCanvasModel());
                }
            };
        } else if (AppActionCommands.CMD_PREVIEW_RENDER_AUTO_DISTRIBUTE_LAYER.equals(cmd)) {
            action = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    AutoDistributeActions.distribucionPorCapa(canvasPanel, editorLayerModel, canvasPanel.getCanvasModel());
                }
            };
        } else if (AppActionCommands.CMD_PREVIEW_RENDER_AUTO_DISTRIBUTE_CANVAS.equals(cmd)) {
            action = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    AutoDistributeActions.escalarParaAjustar(canvasPanel, editorLayerModel, canvasPanel.getCanvasModel());
                }
            };
        } else {
            action = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Stub — sin implementacion todavia
                }
            };
        }
        action.putValue(Action.ACTION_COMMAND_KEY, cmd);
        action.putValue(Action.SHORT_DESCRIPTION, def.textoTooltip());
        if (iconUtils != null) {
            javax.swing.ImageIcon icon = iconUtils.getScaledIcon(def.claveIcono(), iconWidth, iconHeight);
            if (icon != null) {
                action.putValue(Action.SMALL_ICON, icon);
            }
        }

        JButton btn = new JButton(action);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(iconWidth, iconHeight));
        btn.setMinimumSize(new Dimension(iconWidth, iconHeight));
        btn.setMaximumSize(new Dimension(iconWidth, iconHeight));
        btn.setBackground(bgTools);
        btn.setBorder(BorderFactory.createEmptyBorder());

        return btn;
    } // --- Fin del metodo createButtonFromDef ---


    private JButton createAddLayerButton(ToolbarButtonDefinition def) {
        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (editorLayerModel == null) return;
                int w = canvasPanel.getCanvasModel() != null ? canvasPanel.getCanvasModel().getWidth() : 1920;
                int h = canvasPanel.getCanvasModel() != null ? canvasPanel.getCanvasModel().getHeight() : 1080;
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                String name = "Capa " + (editorLayerModel.size() + 1);
                ImageLayer layer = new ImageLayer(name, img, new Rectangle(0, 0, w, h));
                editorLayerModel.addLayer(layer);
                if (layerCardPanel != null) {
                    layerCardPanel.rebuild();
                }
            }
        };
        action.putValue(Action.ACTION_COMMAND_KEY, def.comandoCanonico());
        action.putValue(Action.SHORT_DESCRIPTION, def.textoTooltip());
        if (iconUtils != null) {
            javax.swing.ImageIcon icon = iconUtils.getScaledIcon(def.claveIcono(), iconWidth, iconHeight);
            if (icon != null) {
                action.putValue(Action.SMALL_ICON, icon);
            }
        }

        JButton btn = new JButton(action);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(iconWidth, iconHeight));
        btn.setMinimumSize(new Dimension(iconWidth, iconHeight));
        btn.setMaximumSize(new Dimension(iconWidth, iconHeight));
        btn.setBackground(bgTools);
        btn.setBorder(BorderFactory.createEmptyBorder());

        return btn;
    } // --- Fin del metodo createAddLayerButton ---


    private JButton createLayerActionButton(ToolbarButtonDefinition def) {
        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (editorLayerModel == null || editorLayerModel.size() == 0) return;
                String cmd = def.comandoCanonico();
                int idx = editorLayerModel.getActiveIndex();
                if (idx < 0) return;

                switch (cmd) {
                    case AppActionCommands.CMD_PREVIEW_RENDER_DELETE_LAYER:
                        editorLayerModel.removeLayer(idx);
                        break;
                    case AppActionCommands.CMD_PREVIEW_RENDER_DUPLICATE_LAYER:
                        editorLayerModel.duplicateLayer(idx);
                        break;
                    case AppActionCommands.CMD_PREVIEW_RENDER_BRING_TO_FRONT:
                        editorLayerModel.moveLayer(idx, editorLayerModel.size() - 1);
                        break;
                    case AppActionCommands.CMD_PREVIEW_RENDER_BRING_FORWARD:
                        if (idx < editorLayerModel.size() - 1) {
                            editorLayerModel.moveLayer(idx, idx + 1);
                        }
                        break;
                    case AppActionCommands.CMD_PREVIEW_RENDER_SEND_BACKWARD:
                        if (idx > 0) {
                            editorLayerModel.moveLayer(idx, idx - 1);
                        }
                        break;
                    case AppActionCommands.CMD_PREVIEW_RENDER_SEND_TO_BACK:
                        editorLayerModel.moveLayer(idx, 0);
                        break;
                }
                if (layerCardPanel != null) {
                    layerCardPanel.rebuild();
                }
                canvasPanel.repaint();
            }
        };
        action.putValue(Action.ACTION_COMMAND_KEY, def.comandoCanonico());
        action.putValue(Action.SHORT_DESCRIPTION, def.textoTooltip());
        if (iconUtils != null) {
            javax.swing.ImageIcon icon = iconUtils.getScaledIcon(def.claveIcono(), iconWidth, iconHeight);
            if (icon != null) {
                action.putValue(Action.SMALL_ICON, icon);
            }
        }

        JButton btn = new JButton(action);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(iconWidth, iconHeight));
        btn.setMinimumSize(new Dimension(iconWidth, iconHeight));
        btn.setMaximumSize(new Dimension(iconWidth, iconHeight));
        btn.setBackground(bgTools);
        btn.setBorder(BorderFactory.createEmptyBorder());

        return btn;
    } // --- Fin del metodo createLayerActionButton ---


    private JToggleButton createTextToggle(ToolbarButtonDefinition btnDef) {
        Action action = new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                // Stub — sin implementacion todavia
            }
        };
        action.putValue(Action.ACTION_COMMAND_KEY, btnDef.comandoCanonico());
        action.putValue(Action.SHORT_DESCRIPTION, btnDef.textoTooltip());
        if (iconUtils != null) {
            var icon = iconUtils.getScaledIcon(btnDef.claveIcono(), iconWidth, iconHeight);
            if (icon != null) {
                action.putValue(Action.SMALL_ICON, icon);
            }
        }
        JToggleButton btn = new JToggleButton(action);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setMinimumSize(new Dimension(24, 24));
        btn.setMaximumSize(new Dimension(24, 24));
        btn.setBackground(bgTools);
        btn.setBorder(BorderFactory.createEmptyBorder());
        return btn;
    } // --- Fin del metodo createTextToggle ---


    private JToggleButton createRightTextButton(ToolbarButtonDefinition btnDef, ButtonGroup group,
            Consumer<JToggleButton> onClick) {
        JToggleButton btn = createTextToggle(btnDef);
        if (group != null) group.add(btn);
        btn.addActionListener(e -> {
            if (syncingTools) return;
            onClick.accept(btn);
        });
        return btn;
    } // --- Fin del metodo createRightTextButton ---


    // --- Aplicaciones de propiedades desde el panel derecho (reutilizan componentBar) ---

    private void rightSetFontFamily(String fam) {
        if (componentBar == null) return;
        componentBar.setTextFontFamily(fam);
        componentBar.applyTextProperty(tl -> {
            Font curr = tl.getFont();
            tl.setFont(new Font(fam, curr.getStyle(), curr.getSize()));
        });
    } // --- Fin del metodo rightSetFontFamily ---


    private void rightSetFontSize(int sz) {
        if (componentBar == null) return;
        componentBar.setTextFontSize(sz);
        componentBar.applyTextProperty(tl -> {
            Font curr = tl.getFont();
            tl.setFont(curr.deriveFont((float) sz));
        });
    } // --- Fin del metodo rightSetFontSize ---


    private void rightSetBold(boolean b) {
        if (componentBar == null) return;
        componentBar.setTextBold(b);
        componentBar.applyTextProperty(tl -> {
            Font curr = tl.getFont();
            int style = b ? (curr.getStyle() | Font.BOLD) : (curr.getStyle() & ~Font.BOLD);
            tl.setFont(curr.deriveFont(style));
        });
    } // --- Fin del metodo rightSetBold ---


    private void rightSetItalic(boolean i) {
        if (componentBar == null) return;
        componentBar.setTextItalic(i);
        componentBar.applyTextProperty(tl -> {
            Font curr = tl.getFont();
            int style = i ? (curr.getStyle() | Font.ITALIC) : (curr.getStyle() & ~Font.ITALIC);
            tl.setFont(curr.deriveFont(style));
        });
    } // --- Fin del metodo rightSetItalic ---


    private void rightSetUnderline(boolean u) {
        if (componentBar == null) return;
        componentBar.setTextUnderline(u);
        componentBar.applyTextProperty(tl -> tl.setUnderline(u));
    } // --- Fin del metodo rightSetUnderline ---


    private void rightSetStrikethrough(boolean s) {
        if (componentBar == null) return;
        componentBar.setTextStrikethrough(s);
        componentBar.applyTextProperty(tl -> tl.setStrikethrough(s));
    } // --- Fin del metodo rightSetStrikethrough ---


    private void rightSetAlignment(int align) {
        if (componentBar == null) return;
        componentBar.setTextAlignment(align);
        if (align == modelo.editor.TextLayer.ALIGN_JUSTIFY) {
            componentBar.applyTextPropertyFixedBox(tl -> tl.setAlignment(align), 1.5f, 200);
        } else {
            componentBar.applyTextProperty(tl -> tl.setAlignment(align));
        }
    } // --- Fin del metodo rightSetAlignment ---


    private void rightSetFlowColumns(boolean fc) {
        if (componentBar == null) return;
        componentBar.setTextFlowColumns(fc);
        if (fc) {
            componentBar.applyTextPropertyFixedBox(tl -> tl.setFlowColumns(true), 2f, 240);
        } else {
            componentBar.applyTextProperty(tl -> tl.setFlowColumns(false));
        }
    } // --- Fin del metodo rightSetFlowColumns ---


    private void rightSetVertical(boolean v) {
        if (componentBar == null) return;
        componentBar.setTextVertical(v);
        componentBar.applyTextProperty(tl -> tl.setVertical(v));
    } // --- Fin del metodo rightSetVertical ---


    private void syncToolsFromBar() {
        if (componentBar == null || syncingTools) return;
        syncingTools = true;
        try {
            String ff = componentBar.getTextFontFamily();
            if (ff != null && rightFontCombo != null && !ff.equals(rightFontCombo.getSelectedItem())) {
                rightFontCombo.setSelectedItem(ff);
            }
            int sz = componentBar.getTextFontSize();
            if (sz > 0 && rightSizeCombo != null && rightSizeCombo.getSelectedItem() != null
                    && !Integer.valueOf(sz).equals(rightSizeCombo.getSelectedItem())) {
                rightSizeCombo.setSelectedItem(sz);
            }
            Boolean b = componentBar.isTextBold();
            if (b != null && rightBoldBtn != null) rightBoldBtn.setSelected(b);
            Boolean i = componentBar.isTextItalic();
            if (i != null && rightItalicBtn != null) rightItalicBtn.setSelected(i);
            Boolean u = componentBar.isTextUnderline();
            if (u != null && rightUnderlineBtn != null) rightUnderlineBtn.setSelected(u);
            Boolean s = componentBar.isTextStrikethrough();
            if (s != null && rightStrikethroughBtn != null) rightStrikethroughBtn.setSelected(s);
            int a = componentBar.getTextAlignment();
            if (a >= 0) {
                if (rightAlignLeftBtn != null) rightAlignLeftBtn.setSelected(a == SwingConstants.LEFT);
                if (rightAlignCenterBtn != null) rightAlignCenterBtn.setSelected(a == SwingConstants.CENTER);
                if (rightAlignRightBtn != null) rightAlignRightBtn.setSelected(a == SwingConstants.RIGHT);
                if (rightJustifiedBtn != null) rightJustifiedBtn.setSelected(a == modelo.editor.TextLayer.ALIGN_JUSTIFY);
            }
            Boolean v = componentBar.isTextVertical();
            if (v != null) {
                if (rightHorizontalBtn != null) rightHorizontalBtn.setSelected(!v);
                if (rightVerticalBtn != null) rightVerticalBtn.setSelected(v);
            }
            Boolean fc = componentBar.isTextFlowColumns();
            if (fc != null) {
                if (rightFlowRowsBtn != null) rightFlowRowsBtn.setSelected(!fc);
                if (rightFlowColumnsBtn != null) rightFlowColumnsBtn.setSelected(fc);
            }
        } finally {
            syncingTools = false;
        }
    } // --- Fin del metodo syncToolsFromBar ---


    private JPanel wrapInCell(JComponent comp) {
        JPanel cell = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        cell.setBackground(bgTools);
        cell.add(comp);
        return cell;
    } // --- Fin del metodo wrapInCell ---


    public void setIconUtils(IconUtils iconUtils) {
        this.iconUtils = iconUtils;
        componentBar.setIconUtils(iconUtils);
        if (homeButton != null && iconUtils != null) {
            var icon = iconUtils.getScaledIcon("90000-home.png", 28, 28);
            if (icon != null) homeButton.setIcon(icon);
        }
        rebuildIfReady();
    } // --- Fin del metodo setIconUtils ---


    public void setUiDefinitionService(UIDefinitionService service) {
        this.uiDefinitionService = service;
        componentBar.setUiDefinitionService(service);
        rebuildIfReady();
    } // --- Fin del metodo setUiDefinitionService ---


    public void setIconSize(int width, int height) {
        if (width > 0) this.iconWidth = width;
        if (height > 0) this.iconHeight = height;
        rebuildIfReady();
    } // --- Fin del metodo setIconSize ---


    private void buildLeftToolbar() {
        toolbarContainer.removeAll();

        if (iconUtils == null || uiDefinitionService == null) return;

        ToolbarDefinition tbDef = uiDefinitionService.getToolbarDefinition("editoravanzado");
        if (tbDef == null) return;

        List<ToolbarComponentDefinition> comps = tbDef.componentes();

        int separatorCount = 0;
        for (ToolbarComponentDefinition comp : comps) {
            if (comp instanceof SeparatorDefinition) separatorCount++;
        }

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(bgToolbar);
        toolbarContainer.add(mainPanel);

        JPanel bottomPanel = null;
        ButtonGroup mainGroup = new ButtonGroup();
        int sepIndex = 0;

        for (ToolbarComponentDefinition comp : comps) {
            if (comp instanceof SeparatorDefinition) {
                sepIndex++;
                if (sepIndex == separatorCount) {
                    toolbarContainer.add(new JSeparator(JSeparator.HORIZONTAL));
                    bottomPanel = new JPanel();
                    bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
                    bottomPanel.setBackground(bgToolbar);
                    toolbarContainer.add(bottomPanel);
                    buildColorControls(bottomPanel);
                }
            } else if (comp instanceof ToolbarButtonDefinition btnDef) {
                String cmdKey = btnDef.comandoCanonico();
                Action action = EditorToolAction.createForCommand(cmdKey);
                if (action == null) {
                    if (AppActionCommands.CMD_ADVANCED_EDITOR_PANTALLA_COMPLETA.equals(cmdKey)) {
                        action = new AbstractAction() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                toggleFullscreen();
                            }
                        };
                    } else {
                        action = new AbstractAction() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                // Zoom — no cambian herramienta
                            }
                        };
                    }
                    action.putValue(Action.ACTION_COMMAND_KEY, cmdKey);
                    action.putValue(Action.SHORT_DESCRIPTION, btnDef.textoTooltip());
                }
                if (iconUtils != null) {
                    javax.swing.ImageIcon icon = iconUtils.getScaledIcon(btnDef.claveIcono(), 20, 20);
                    if (icon != null) {
                        Character hotkey = EditorHotkeys.hotkeyFor(cmdKey);
                        if (hotkey != null) {
                            action.putValue(Action.SMALL_ICON, new ToolBadgeIcon(icon, hotkey));
                        } else {
                            action.putValue(Action.SMALL_ICON, icon);
                        }
                    }
                }

                boolean isFullscreen = AppActionCommands.CMD_ADVANCED_EDITOR_PANTALLA_COMPLETA.equals(cmdKey);
                ThemedToggleButton btn = new ThemedToggleButton(themeManager, action);
                btn.putClientProperty("JButton.buttonType", "regular");
                if (!isFullscreen) {
                    btn.addItemListener(e -> {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            if (syncingTools) return;
                            syncingTools = true;
                            try {
                                Action a = btn.getAction();
                                Object cmd = a != null ? a.getValue(Action.ACTION_COMMAND_KEY) : null;
                                if (cmd instanceof String) {
                                    setActiveTool((String) cmd);
                                }
                            } finally {
                                syncingTools = false;
                            }
                        }
                    });
                }
                btn.setAlignmentX(JComponent.CENTER_ALIGNMENT);
                btn.setPreferredSize(new Dimension(28, 28));
                btn.setMaximumSize(new Dimension(28, 28));
                btn.setMinimumSize(new Dimension(28, 28));
                btn.setFocusPainted(false);
                btn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

                if (separatorCount > 0 && sepIndex == separatorCount) {
                    bottomPanel.add(btn);
                } else {
                    mainGroup.add(btn);
                    mainPanel.add(btn);
                }
            }
        }

        toolbarContainer.revalidate();
        toolbarContainer.repaint();

        // Sincronizar el desplegable con el boton seleccionado
        var combo = componentBar.getToolCombo();
        var sel = combo.getSelectedItem();
        if (sel instanceof EditorComponentBar.ToolItem ti) {
            selectToolButton(ti.commandKey());
        }
    } // --- Fin del metodo buildLeftToolbar ---


    private void buildColorControls(JPanel bottomPanel) {
        if (uiDefinitionService == null) return;

        ToolbarDefinition tbDef = uiDefinitionService.getToolbarDefinition("editoravanzadocolor");
        if (tbDef == null) return;

        // Glue: empuja el bloque hacia abajo (anclado al fondo del panel)
        bottomPanel.add(Box.createVerticalGlue());

        // Fila con reset + invert, uno al lado del otro
        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 2));
        buttonsRow.setBackground(bgToolbar);
        buttonsRow.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        for (ToolbarComponentDefinition comp : tbDef.componentes()) {
            if (!(comp instanceof ToolbarButtonDefinition btnDef)) continue;
            String cmdKey = btnDef.comandoCanonico();
            JButton btn = new JButton();
            btn.putClientProperty("JButton.buttonType", "regular");
            btn.setToolTipText(btnDef.textoTooltip());
            btn.setPreferredSize(new Dimension(16, 16));
            btn.setMaximumSize(new Dimension(16, 16));
            btn.setMinimumSize(new Dimension(16, 16));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            if (iconUtils != null) {
                javax.swing.ImageIcon icon = iconUtils.getScaledIcon(btnDef.claveIcono(), 12, 12);
                if (icon != null) btn.setIcon(icon);
            }
            btn.addActionListener(e -> {
                if (AppActionCommands.CMD_ADVANCED_EDITOR_RESET_COLORS.equals(cmdKey)) {
                    setColorFrontal(Color.WHITE);
                    setColorFondo(Color.BLACK);
                } else if (AppActionCommands.CMD_ADVANCED_EDITOR_INVERT_COLORS.equals(cmdKey)) {
                    Color tmp = colorFrontal;
                    setColorFrontal(colorFondo);
                    setColorFondo(tmp);
                }
            });
            buttonsRow.add(btn);
        }
        buttonsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttonsRow.getPreferredSize().height));
        bottomPanel.add(buttonsRow);

        // Bloque de los dos cuadrados superpuestos (frontal delante, fondo detrás)
        JComponent boxes = new JComponent() {
            private static final long serialVersionUID = 1L;
            private final Rectangle frontalRect = new Rectangle(2, 2, 16, 16);
            private final Rectangle fondoRect = new Rectangle(8, 8, 20, 20);

            {
                setPreferredSize(new Dimension(30, 30));
                setMaximumSize(new Dimension(30, 30));
                setMinimumSize(new Dimension(30, 30));
                setAlignmentX(JComponent.CENTER_ALIGNMENT);
                setToolTipText("Clic: color frontal · Clic derecho: color de fondo");
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        boolean frontal = frontalRect.contains(e.getPoint());
                        boolean fondo = fondoRect.contains(e.getPoint());
                        if (!frontal && !fondo) return;
                        if (frontal && e.getButton() == MouseEvent.BUTTON3) frontal = false;
                        if (!frontal && !fondo) return;
                        Color current = frontal ? colorFrontal : colorFondo;
                        Color chosen = JColorChooser.showDialog(AdvanceEditPanel.this,
                                frontal ? "Color frontal" : "Color de fondo", current);
                        if (chosen == null) return;
                        if (frontal) {
                            setColorFrontal(chosen);
                        } else {
                            setColorFondo(chosen);
                        }
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fondo (cuadrado grande detrás, abajo-derecha)
                g2.setColor(colorFondo);
                g2.fillRect(fondoRect.x, fondoRect.y, fondoRect.width, fondoRect.height);
                g2.setColor(new Color(200, 200, 200, 180));
                g2.drawRect(fondoRect.x, fondoRect.y, fondoRect.width, fondoRect.height);

                // Frontal (cuadrado pequeño delante, arriba-izquierda, tapa la esquina)
                g2.setColor(colorFrontal);
                g2.fillRect(frontalRect.x, frontalRect.y, frontalRect.width, frontalRect.height);
                g2.setColor(new Color(200, 200, 200, 220));
                g2.drawRect(frontalRect.x, frontalRect.y, frontalRect.width, frontalRect.height);

                g2.dispose();
            }
        };
        colorBoxesComponent = boxes;
        bottomPanel.add(boxes);
    } // --- Fin del metodo buildColorControls ---


    private void rebuildIfReady() {
        if (iconUtils == null || uiDefinitionService == null || themeManager == null) return;
        JPanel tc = getToolsContent();
        tc.removeAll();
        buildDefaultTools();
        buildLeftToolbar();
        tc.revalidate();
        tc.repaint();
    } // --- Fin del metodo rebuildIfReady ---


    private void rebuildTools() {
        JPanel tc = getToolsContent();
        tc.removeAll();
        buildDefaultTools();
        buildLeftToolbar();
        tc.revalidate();
        tc.repaint();
    } // --- Fin del metodo rebuildTools ---


    private void toggleFullscreen() {
        RenderPanel rp = (RenderPanel) javax.swing.SwingUtilities.getAncestorOfClass(RenderPanel.class, this);
        if (rp != null) {
            rp.toggleEditorFullscreen();
        }
    } // --- Fin del metodo toggleFullscreen ---


    public Color getColorFrontal() {
        return colorFrontal;
    } // --- Fin del metodo getColorFrontal ---


    public Color getColorFondo() {
        return colorFondo;
    } // --- Fin del metodo getColorFondo ---


    /**
     * Establece el color frontal y lo propaga a las herramientas que pintan
     * con el primer plano (bote, relleno de formas, texto y degradado inicial).
     */
    public void setColorFrontal(Color c) {
        if (c == null) return;
        colorFrontal = c;
        if (colorBoxesComponent != null) colorBoxesComponent.repaint();
        componentBar.setPaintBucketColor(c);
        componentBar.setShapeFillColor(c);
        componentBar.setTextColor(c);
        componentBar.setGradientStartColor(c);
    } // --- Fin del metodo setColorFrontal ---


    /**
     * Establece el color de fondo y lo propaga a las herramientas que lo usan
     * (actualmente el color final del degradado).
     */
    public void setColorFondo(Color c) {
        if (c == null) return;
        colorFondo = c;
        if (colorBoxesComponent != null) colorBoxesComponent.repaint();
        componentBar.setGradientEndColor(c);
    } // --- Fin del metodo setColorFondo ---

} // --- Fin de la clase AdvanceEditPanel ---
