package vista.panels.render;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import controlador.commands.AppActionCommands;
import vista.util.IconUtils;

public class AdvanceEditPanel extends JPanel {

    private final JPanel canvasPanel;
    private final JPanel advanceEditToolsPanel;
    private final JPanel toolbarContainer;
    private final JPanel statusBar;
    private boolean advanceEditToolsVisible;
    private boolean advanceEditActive;
    private int iconWidth = 24;
    private int iconHeight = 24;
    private IconUtils iconUtils;

    public AdvanceEditPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(40, 40, 45));

        // --- Status bar superior ---
        statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        statusBar.setBackground(new Color(55, 55, 60));
        statusBar.setPreferredSize(new Dimension(0, 26));
        JLabel statusLabel = new JLabel("Editor avanzado");
        statusLabel.setForeground(Color.WHITE);
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.NORTH);

        // --- Contenedor principal (toolbar izquierda + split central) ---
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(new Color(40, 40, 45));

        // --- Toolbar izquierda (toolbox) ---
        toolbarContainer = new JPanel();
        toolbarContainer.setLayout(new BoxLayout(toolbarContainer, BoxLayout.Y_AXIS));
        toolbarContainer.setBackground(new Color(45, 45, 50));
        toolbarContainer.setPreferredSize(new Dimension(36, 0));
        toolbarContainer.setMinimumSize(new Dimension(36, 0));
        mainContent.add(toolbarContainer, BorderLayout.WEST);

        // --- Panel de herramientas derecha ---
        advanceEditToolsVisible = true;

        advanceEditToolsPanel = new JPanel(new BorderLayout());
        advanceEditToolsPanel.setBackground(new Color(50, 50, 55));
        advanceEditToolsPanel.setPreferredSize(new Dimension(160, 0));
        advanceEditToolsPanel.setMinimumSize(new Dimension(0, 0));

        JPanel toolsHeader = new JPanel(new BorderLayout());
        toolsHeader.setBackground(new Color(55, 55, 60));
        toolsHeader.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
        JLabel toolsTitle = new JLabel("Herramientas");
        toolsTitle.setForeground(Color.WHITE);
        toolsHeader.add(toolsTitle, BorderLayout.WEST);
        advanceEditToolsPanel.add(toolsHeader, BorderLayout.NORTH);

        JPanel toolsContent = new JPanel(new BorderLayout());
        toolsContent.setBackground(new Color(50, 50, 55));
        advanceEditToolsPanel.add(toolsContent, BorderLayout.CENTER);

        // --- Split central: canvas | tools ---
        JSplitPane advanceEditSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        advanceEditSplit.setBackground(new Color(40, 40, 45));
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
        canvasPanel.setBackground(new Color(40, 40, 45));
        advanceEditSplit.setLeftComponent(canvasPanel);
        advanceEditSplit.setRightComponent(advanceEditToolsPanel);
        advanceEditSplit.setDividerLocation(Integer.MAX_VALUE);

        mainContent.add(advanceEditSplit, BorderLayout.CENTER);
        add(mainContent, BorderLayout.CENTER);

        buildDefaultTools();
    } // --- Fin del constructor AdvanceEditPanel ---


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
        group.setBackground(new Color(45, 45, 50));
        for (javax.swing.JButton btn : buttons) {
            btn.setAlignmentX(javax.swing.JComponent.CENTER_ALIGNMENT);
            btn.setPreferredSize(new Dimension(28, 28));
            btn.setMaximumSize(new Dimension(28, 28));
            btn.setMinimumSize(new Dimension(28, 28));
            btn.setBackground(new Color(55, 55, 60));
            btn.setForeground(Color.WHITE);
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

    private static final Color BG_TOOLS = new Color(50, 50, 55);
    private static final Color FG_SECTION_TITLE = new Color(180, 180, 190);

    private void buildDefaultTools() {
        JPanel tc = getToolsContent();
        tc.setLayout(new BorderLayout());

        JScrollPane scroll = new JScrollPane();
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_TOOLS);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(BG_TOOLS);
        container.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        container.add(createSection("Alinear", new String[][] {
            {AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_BORDE_INFERIOR, "70401-align-borde-inferior.png", "Alinear borde inferior"},
            {AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_CENTRO_VERTICAL, "70402-align-centro-vertical.png", "Alinear centro vertical"},
            {AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_BORDE_SUPERIOR, "70403-align-borde-superior.png", "Alinear borde superior"},
            {AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_BORDE_IZQUIERDO, "70404-align-borde-izquierdo.png", "Alinear borde izquierdo"},
            {AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_CENTRO_HORIZONTAL, "70405-align-centro-horizontal.png", "Alinear centro horizontal"},
            {AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_BORDE_DERECHO, "70406-align-borde-derecho.png", "Alinear borde derecho"},
        }, 3));

        container.add(Box.createVerticalStrut(2));
        container.add(createSection("Distribuir", new String[][] {
            {AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_BOTTOM_BORDER, "70501-distribute-bottom-border.png", "Distribuir bordes inferiores"},
            {AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_CENTER_VERTICAL, "70502-distribute-center-vertical.png", "Distribuir centros verticales"},
            {AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_TOP_BORDER, "70503-distribute-top-border.png", "Distribuir bordes superiores"},
            {AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_LEFT_BORDER, "70504-distribute-left-border.png", "Distribuir bordes izquierdos"},
            {AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_CENTER_HORIZONTAL, "70505-distribute-center-horizontal.png", "Distribuir centros horizontales"},
            {AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_RIGHT_BORDER, "70506-distribute-right-border.png", "Distribuir bordes derechos"},
        }, 3));

        container.add(Box.createVerticalStrut(2));
        container.add(createSection("Espacio", new String[][] {
            {AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_HORIZONTAL_SPACE, "70601-distribute-horizontal-space.png", "Distribuir espacio horizontal"},
            {AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_VERTICAL_SPACE, "70602-distribute-vertical-space.png", "Distribuir espacio vertical"},
        }, 2));

        container.add(Box.createVerticalStrut(2));
        container.add(createSection("Auto distribuir", new String[][] {
            {AppActionCommands.CMD_PREVIEW_RENDER_AUTO_DISTRIBUTE_FIXED, "70701-auto-distribute-fixed.png", "Distribución fija por posición"},
            {AppActionCommands.CMD_PREVIEW_RENDER_AUTO_DISTRIBUTE_LAYER, "70701-auto-distribute-layer.png", "Distribución por tamaño de capa"},
        }, 2));

        container.add(createSection("Auto distribuir", new String[][] {
            {AppActionCommands.CMD_PREVIEW_RENDER_AUTO_DISTRIBUTE_FIXED, "70701-auto-distribute-fixed.png", "Distribución fija por posición"},
            {AppActionCommands.CMD_PREVIEW_RENDER_AUTO_DISTRIBUTE_LAYER, "70701-auto-distribute-layer.png", "Distribución por tamaño de capa"},
        }, 2));

        // --- Texto (tabs: alineamiento + fuentes) ---
        JTabbedPane textTabs = new JTabbedPane();
        textTabs.setBackground(new Color(55, 55, 60));
        textTabs.setForeground(FG_SECTION_TITLE);
        textTabs.setFont(textTabs.getFont().deriveFont(10f));
        textTabs.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        textTabs.setPreferredSize(new Dimension(160, 140));
        textTabs.setMinimumSize(new Dimension(160, 80));
        textTabs.addTab("Alineamiento", new JPanel());
        textTabs.addTab("Fuentes", new JPanel());
        container.add(createSection("Texto", textTabs, false));

        // --- Capas (tabs: capas + orden) ---
        JTabbedPane capasTabs = new JTabbedPane();
        capasTabs.setBackground(new Color(55, 55, 60));
        capasTabs.setForeground(FG_SECTION_TITLE);
        capasTabs.setFont(capasTabs.getFont().deriveFont(10f));
        capasTabs.setPreferredSize(new Dimension(160, 150));
        capasTabs.setMinimumSize(new Dimension(160, 100));

        // Pestaña "Capas": tabla con visibilidad + nombre
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
        layerTable.setBackground(new Color(50, 50, 55));
        layerTable.setForeground(FG_SECTION_TITLE);
        layerTable.setSelectionBackground(new Color(65, 65, 75));
        layerTable.setSelectionForeground(FG_SECTION_TITLE);
        layerTable.getColumnModel().getColumn(0).setMaxWidth(28);
        layerTable.getColumnModel().getColumn(0).setMinWidth(28);
        JScrollPane capasScroll = new JScrollPane(layerTable);
        capasScroll.setBorder(null);
        capasScroll.getViewport().setBackground(new Color(50, 50, 55));
        capasTabs.addTab("Capas", capasScroll);

        // Pestaña "Orden": botones de orden
        JPanel ordenPanel = new JPanel(new BorderLayout());
        ordenPanel.setBackground(BG_TOOLS);
        ordenPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JPanel orderGrid = new JPanel(new GridLayout(2, 2, 5, 5));
        orderGrid.setBackground(BG_TOOLS);
        orderGrid.add(createToolButton(AppActionCommands.CMD_PREVIEW_RENDER_BRING_TO_FRONT,
                "70201-bring-to-front.png", "Traer al frente"));
        orderGrid.add(createToolButton(AppActionCommands.CMD_PREVIEW_RENDER_BRING_FORWARD,
                "70202-bring-forward.png", "Adelantar"));
        orderGrid.add(createToolButton(AppActionCommands.CMD_PREVIEW_RENDER_SEND_BACKWARD,
                "70203-send-backward.png", "Retroceder"));
        orderGrid.add(createToolButton(AppActionCommands.CMD_PREVIEW_RENDER_SEND_TO_BACK,
                "70204-send-to-back.png", "Enviar al fondo"));
        ordenPanel.add(orderGrid, BorderLayout.NORTH);
        capasTabs.addTab("Orden", ordenPanel);

        // Contenedor capas: tabs + barra de herramientas inferior
        JPanel capasContent = new JPanel(new BorderLayout());
        capasContent.setBackground(BG_TOOLS);
        capasContent.add(capasTabs, BorderLayout.CENTER);

        JPanel layerToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        layerToolbar.setBackground(BG_TOOLS);
        layerToolbar.add(createToolButton(AppActionCommands.CMD_PREVIEW_RENDER_CLEAN_AND_ADD,
                "70301-clean-and-add-image.png", "Limpiar y añadir imagen"));
        layerToolbar.add(createToolButton(AppActionCommands.CMD_PREVIEW_RENDER_DELETE_LAYER,
                "70303-delete-layer.png", "Eliminar capa"));
        capasContent.add(layerToolbar, BorderLayout.SOUTH);

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
        section.setBackground(BG_TOOLS);

        // --- Header clicable (flecha + título) ---
        section.setAlignmentX(LEFT_ALIGNMENT);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        header.setBackground(new Color(48, 48, 53));
        header.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(60, 60, 65)));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel arrowLbl = new JLabel("\u25BC"); // ▼ expandido por defecto
        arrowLbl.setForeground(FG_SECTION_TITLE);
        arrowLbl.setFont(new Font("SansSerif", Font.PLAIN, 9));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setForeground(FG_SECTION_TITLE);
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 11f));

        header.add(arrowLbl);
        header.add(titleLbl);
        section.add(header, BorderLayout.NORTH);

        // --- Contenido directamente en CENTER (llena todo el ancho) ---
        section.add(content, BorderLayout.CENTER);

        // --- Toggle colapso al hacer clic en el header ---
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean visible = !content.isVisible();
                content.setVisible(visible);
                arrowLbl.setText(visible ? "\u25BC" : "\u25B6"); // ▼ / ▶
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


    private JPanel createSection(String title, String[][] buttons, int columns) {
        JPanel grid = new JPanel(new GridLayout(0, columns, 5, 5));
        grid.setBackground(BG_TOOLS);
        grid.setBorder(BorderFactory.createEmptyBorder(2, 8, 6, 8));
        for (String[] btnData : buttons) {
            JButton btn = createToolButton(btnData[0], btnData[1], btnData[2]);
            JPanel cell = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            cell.setBackground(BG_TOOLS);
            cell.add(btn);
            grid.add(cell);
        }
        return createSection(title, grid, false);
    } // --- Fin del metodo createSection (grid) ---


    private JButton createToolButton(String command, String iconName, String tooltip) {
        Action stubAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Stub — sin implementación todavía
            }
        };
        stubAction.putValue(Action.ACTION_COMMAND_KEY, command);
        stubAction.putValue(Action.SHORT_DESCRIPTION, tooltip);

        ImageIcon icon = loadIcon(iconName);
        if (icon != null) {
            stubAction.putValue(Action.SMALL_ICON, icon);
        }

        JButton btn = new JButton(stubAction);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(iconWidth, iconHeight));
        btn.setMinimumSize(new Dimension(iconWidth, iconHeight));
        btn.setMaximumSize(new Dimension(iconWidth, iconHeight));
        btn.setBackground(new Color(50, 50, 55));
        btn.setBorder(BorderFactory.createEmptyBorder());

        return btn;
    } // --- Fin del metodo createToolButton ---


    private ImageIcon loadIcon(String name) {
        if (iconUtils != null) {
            return iconUtils.getScaledIcon(name, iconWidth, iconHeight);
        }
        try {
            java.net.URL url = getClass().getResource("/iconos/black/" + name);
            if (url != null) {
                ImageIcon original = new ImageIcon(url);
                Image scaled = original.getImage().getScaledInstance(iconWidth, iconHeight, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Exception e) {
            // ignorar
        }
        return null;
    } // --- Fin del metodo loadIcon ---


    public void setIconUtils(IconUtils iconUtils) {
        this.iconUtils = iconUtils;
        JPanel tc = getToolsContent();
        if (tc.getComponentCount() > 0) {
            rebuildTools();
        }
    } // --- Fin del metodo setIconUtils ---


    public void setIconSize(int width, int height) {
        if (width > 0) this.iconWidth = width;
        if (height > 0) this.iconHeight = height;
        rebuildTools();
    } // --- Fin del metodo setIconSize ---


    private void rebuildTools() {
        JPanel tc = getToolsContent();
        tc.removeAll();
        buildDefaultTools();
        tc.revalidate();
        tc.repaint();
    } // --- Fin del metodo rebuildTools ---

} // --- Fin de la clase AdvanceEditPanel ---
