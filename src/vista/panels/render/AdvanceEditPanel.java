package vista.panels.render;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import controlador.commands.AppActionCommands;
import vista.config.SeparatorDefinition;
import vista.config.ToolbarButtonDefinition;
import vista.config.ToolbarComponentDefinition;
import vista.config.ToolbarDefinition;
import vista.config.UIDefinitionService;
import vista.theme.Tema;
import vista.theme.ThemeChangeListener;
import vista.theme.ThemeManager;
import vista.util.IconUtils;

public class AdvanceEditPanel extends JPanel implements ThemeChangeListener {

    private final JPanel canvasPanel;
    private final JPanel advanceEditToolsPanel;
    private final JPanel toolbarContainer;
    private final JPanel mainContent;
    private final JSplitPane advanceEditSplit;
    private final JPanel statusBar;
    private final JLabel statusLabel;
    private final JLabel toolsTitle;
    private boolean advanceEditToolsVisible;
    private boolean advanceEditActive;
    private IconUtils iconUtils;
    private UIDefinitionService uiDefinitionService;
    private int iconWidth = 24;
    private int iconHeight = 24;

    // Colores temáticos (inicializados con defaults oscuros)
    private Color bgMain = new Color(40, 40, 45);
    private Color bgTools = new Color(50, 50, 55);
    private Color bgToolbar = new Color(45, 45, 50);
    private Color bgHeader = new Color(48, 48, 53);
    private Color bgStatus = new Color(55, 55, 60);
    private Color fgStatus = Color.WHITE;
    private Color borderColor = new Color(60, 60, 65);
    private Color fgSectionTitle = new Color(180, 180, 190);

    public AdvanceEditPanel() {
        setLayout(new BorderLayout());
        setBackground(bgMain);

        statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        statusBar.setBackground(bgStatus);
        statusBar.setPreferredSize(new Dimension(0, 26));
        statusLabel = new JLabel("Editor avanzado");
        statusLabel.setForeground(fgStatus);
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.NORTH);

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
        toolsHeader.setBackground(bgStatus);
        toolsHeader.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
        toolsTitle = new JLabel("Herramientas");
        toolsTitle.setForeground(fgStatus);
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
        });

        canvasPanel = new JPanel(new BorderLayout());
        canvasPanel.setBackground(bgMain);
        advanceEditSplit.setLeftComponent(canvasPanel);
        advanceEditSplit.setRightComponent(advanceEditToolsPanel);
        advanceEditSplit.setDividerLocation(Integer.MAX_VALUE);

        mainContent.add(advanceEditSplit, BorderLayout.CENTER);
        add(mainContent, BorderLayout.CENTER);
    } // --- Fin del constructor AdvanceEditPanel ---


    @Override
    public void onThemeChanged(Tema tema) {
        SwingUtilities.invokeLater(() -> {
            bgMain = tema.colorFondoSecundario();
            bgTools = tema.colorFondoPrincipal();
            bgToolbar = tema.colorFondoPrincipal();
            bgHeader = tema.colorFondoPrincipal();
            bgStatus = tema.colorBarraEstadoFondo();
            fgStatus = tema.colorBarraEstadoTexto();
            borderColor = tema.colorBorde();
            fgSectionTitle = tema.colorTextoSecundario();

            setBackground(bgMain);
            statusBar.setBackground(bgStatus);
            statusLabel.setForeground(fgStatus);
            toolbarContainer.setBackground(bgToolbar);
            mainContent.setBackground(bgMain);
            advanceEditSplit.setBackground(bgMain);
            advanceEditSplit.setDividerSize(6);
            advanceEditToolsPanel.setBackground(bgTools);

            for (java.awt.Component c : advanceEditToolsPanel.getComponents()) {
                if (c instanceof JPanel h && ((BorderLayout) advanceEditToolsPanel.getLayout()).getLayoutComponent(BorderLayout.NORTH) == h) {
                    h.setBackground(bgStatus);
                    for (java.awt.Component child : ((JPanel) h).getComponents()) {
                        if (child instanceof JLabel) child.setForeground(fgStatus);
                    }
                }
            }

            canvasPanel.setBackground(bgMain);
            toolsTitle.setForeground(fgStatus);

            rebuildTools();
        });
    } // --- Fin del metodo onThemeChanged ---


    public void setThemeManager(ThemeManager themeManager) {
        if (themeManager != null) {
            themeManager.addThemeChangeListener(this);
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
                    if (w > 200) {
                        sp.setDividerLocation(w - 180);
                    }
                }
            });
        }
    } // --- Fin del metodo setActive ---


    public boolean isActive() {
        return advanceEditActive;
    } // --- Fin del metodo isActive ---


    public JPanel getCanvas() {
        return canvasPanel;
    } // --- Fin del metodo getCanvas ---


    public JPanel getToolsPanel() {
        return advanceEditToolsPanel;
    } // --- Fin del metodo getToolsPanel ---


    public JPanel getToolsContent() {
        return (JPanel) ((BorderLayout) advanceEditToolsPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
    } // --- Fin del metodo getToolsContent ---


    public JPanel getToolbarContainer() {
        return toolbarContainer;
    } // --- Fin del metodo getToolbarContainer ---


    public JPanel getStatusBar() {
        return statusBar;
    } // --- Fin del metodo getStatusBar ---


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
            btn.setBackground(bgStatus);
            btn.setForeground(fgStatus);
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
        textTabs.setBackground(bgStatus);
        textTabs.setForeground(fgSectionTitle);
        textTabs.setFont(textTabs.getFont().deriveFont(10f));
        textTabs.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        textTabs.setPreferredSize(new Dimension(160, 140));
        textTabs.setMinimumSize(new Dimension(160, 80));
        textTabs.addTab("Alineamiento", new JPanel());
        textTabs.addTab("Fuentes", new JPanel());
        container.add(createSection("Texto", textTabs, false));

        // --- Capas (tabs: capas + orden) ---
        JTabbedPane capasTabs = new JTabbedPane();
        capasTabs.setBackground(bgStatus);
        capasTabs.setForeground(fgSectionTitle);
        capasTabs.setFont(capasTabs.getFont().deriveFont(10f));
        capasTabs.setPreferredSize(new Dimension(160, 150));
        capasTabs.setMinimumSize(new Dimension(160, 100));

        DefaultTableModel layerModel = new DefaultTableModel(new String[]{"Visible", "Nombre"}, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 0 ? Boolean.class : String.class;
            }
            @Override
            public boolean isCellEditable(int row, int col) {
                return true;
            }
        };
        JTable layerTable = new JTable(layerModel);
        layerTable.setTableHeader(null);
        layerTable.setShowGrid(false);
        layerTable.setRowHeight(22);
        layerTable.setBackground(bgTools);
        layerTable.setForeground(fgSectionTitle);
        layerTable.setSelectionBackground(new Color(65, 65, 75));
        layerTable.setSelectionForeground(fgSectionTitle);
        layerTable.getColumnModel().getColumn(0).setMaxWidth(28);
        layerTable.getColumnModel().getColumn(0).setMinWidth(28);
        JScrollPane capasScroll = new JScrollPane(layerTable);
        capasScroll.setBorder(null);
        capasScroll.getViewport().setBackground(bgTools);
        capasTabs.addTab("Capas", capasScroll);

        // Pestaña "Orden": botones de orden
        JPanel ordenPanel = new JPanel(new BorderLayout());
        ordenPanel.setBackground(bgTools);
        ordenPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        if (iconUtils != null && uiDefinitionService != null) {
            JPanel orderGrid = new JPanel(new GridLayout(2, 2, 5, 5));
            orderGrid.setBackground(bgTools);
            for (ToolbarComponentDefinition def : uiDefinitionService.getComponentesLayerOrderPreviewRender()) {
                if (def instanceof ToolbarButtonDefinition btnDef) {
                    orderGrid.add(wrapInCell(createButtonFromDef(btnDef)));
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
                    layerToolbar.add(createButtonFromDef(btnDef));
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
        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Stub — sin implementacion todavia
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
    } // --- Fin del metodo createButtonFromDef ---


    private JPanel wrapInCell(JComponent comp) {
        JPanel cell = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        cell.setBackground(bgTools);
        cell.add(comp);
        return cell;
    } // --- Fin del metodo wrapInCell ---


    public void setIconUtils(IconUtils iconUtils) {
        this.iconUtils = iconUtils;
        rebuildIfReady();
    } // --- Fin del metodo setIconUtils ---


    public void setUiDefinitionService(UIDefinitionService service) {
        this.uiDefinitionService = service;
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
                    toolbarContainer.add(Box.createVerticalGlue());
                    toolbarContainer.add(new JSeparator(JSeparator.HORIZONTAL));
                    bottomPanel = new JPanel();
                    bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
                    bottomPanel.setBackground(bgToolbar);
                    toolbarContainer.add(bottomPanel);
                }
            } else if (comp instanceof ToolbarButtonDefinition btnDef) {
                Action action = new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // Stub — sin implementacion todavia
                    }
                };
                action.putValue(Action.ACTION_COMMAND_KEY, btnDef.comandoCanonico());
                action.putValue(Action.SHORT_DESCRIPTION, btnDef.textoTooltip());
                if (iconUtils != null) {
                    javax.swing.ImageIcon icon = iconUtils.getScaledIcon(btnDef.claveIcono(), 20, 20);
                    if (icon != null) {
                        action.putValue(Action.SMALL_ICON, icon);
                    }
                }

                JToggleButton btn = new JToggleButton(action);
                btn.setAlignmentX(JComponent.CENTER_ALIGNMENT);
                btn.setPreferredSize(new Dimension(28, 28));
                btn.setMaximumSize(new Dimension(28, 28));
                btn.setMinimumSize(new Dimension(28, 28));
                btn.setFocusPainted(false);
                btn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
                btn.setBackground(bgStatus);
                btn.setForeground(fgStatus);

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
    } // --- Fin del metodo buildLeftToolbar ---


    private void rebuildIfReady() {
        if (iconUtils == null || uiDefinitionService == null) return;
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

} // --- Fin de la clase AdvanceEditPanel ---
