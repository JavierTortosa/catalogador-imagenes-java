package vista.builders;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.tree.TreeSelectionModel;

import controlador.DataController;
import controlador.commands.AppActionCommands;
import controlador.managers.interfaces.IProjectManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.image.ThumbnailService;
import vista.components.TagIntelliSenseField;
import vista.config.MenuItemDefinition;
import vista.config.MenuItemType;
import vista.panels.DriveListPanel;
import vista.panels.GridDisplayPanel;
import vista.panels.ImageDisplayPanel;
import vista.panels.PolaroidDisplayPanel;
import vista.panels.TagManagementPanel;
import vista.theme.ThemeManager;
import vista.util.IconUtils;
import vista.util.ThumbnailPreviewer;


/**
 * Constructor de la interfaz de usuario para el "Modo Datos".
 * Se encarga de ensamblar los paneles y componentes que conforman esta vista.
 */
public class DataBuilder {

    private final ComponentRegistry registry;
    private final VisorModel model;
    private final ThemeManager themeManager;
    private final IconUtils iconUtils;
    private final ThumbnailService gridThumbnailService;
    private final ConfigurationManager configManager;
    private Map<String, javax.swing.Action> actionMap;
    private DataController dataController;
    private IProjectManager projectManager;

    public DataBuilder(ComponentRegistry registry, VisorModel model, ThemeManager themeManager, IconUtils iconUtils, ThumbnailService gridThumbnailService, ConfigurationManager configManager) {
        this.registry = registry;
        this.model = model;
        this.themeManager = themeManager;
        this.iconUtils = iconUtils;
        this.gridThumbnailService = gridThumbnailService;
        this.configManager = configManager;
    } // ---FIN de constructor [DataBuilder]---
    
    public void setActionMap(Map<String, javax.swing.Action> actionMap) {
        this.actionMap = actionMap;
    }

    public void setDataController(DataController dataController) {
        this.dataController = dataController;
    }

    public void setProjectManager(IProjectManager projectManager) {
        this.projectManager = projectManager;
    }

    /**
     * Construye y ensambla el panel principal para el Modo Datos.
     * Layout de 3 columnas: [TAGS/RAMAS] | [IMÁGENES + UNIDADES] | [VISOR + ASIGNACIÓN]
     * @return El JPanel completamente configurado para ser añadido al CardLayout principal.
     */
    public JPanel buildDataModePanel() {
        JPanel dataModePanel = new JPanel(new BorderLayout());
        
        // ═══════════════════════════════════════════════════════════════
        // 1. COLUMNA IZQUIERDA - TAGS/RAMAS
        // ═══════════════════════════════════════════════════════════════
        JPanel leftPanel = new JPanel(new BorderLayout());
        TitledBorder allTagsBorder = BorderFactory.createTitledBorder("Biblioteca de Etiquetas");
        leftPanel.setBorder(allTagsBorder);
        
        // --- Toolbar izquierda: vista, orden, filtro, mantenimiento, CRUD, IntelliSense ---
        JToolBar tagToolbar = new JToolBar();
        tagToolbar.setFloatable(false);
        
        // Botón único de cambio de vista (árbol ↔ lista) con 2 iconos
        javax.swing.Icon iconTree = iconUtils.getScaledIcon("30101-Hierarchy.png", 24, 24);
        javax.swing.Icon iconList = iconUtils.getScaledIcon("30100-Vector.png", 24, 24);
        JToggleButton btnViewToggle = new JToggleButton(iconTree);
        btnViewToggle.setSelectedIcon(iconList);
        btnViewToggle.setToolTipText("Vista por \u00e1rbol");
        registry.register("btn.datamode.tag.view.toggle", btnViewToggle);
        tagToolbar.add(btnViewToggle);
        tagToolbar.addSeparator();
        
        // Orden
        tagToolbar.add(new JButton(actionMap.get(AppActionCommands.CMD_DATOS_TAGS_ORDENAR)));
        tagToolbar.addSeparator();
        
        // Mantenimiento BD
        JButton btnMantenimiento = new JButton(iconUtils.getScaledIcon("7005-settings_48x48.png", 24, 24));
        if (btnMantenimiento.getIcon() == null) btnMantenimiento.setText("Mantenimiento");
        btnMantenimiento.setToolTipText("Mantenimiento de Base de Datos");
        if (actionMap != null && actionMap.containsKey(AppActionCommands.CMD_DATOS_MANTENIMIENTO_BD)) {
            btnMantenimiento.addActionListener(actionMap.get(AppActionCommands.CMD_DATOS_MANTENIMIENTO_BD));
        } else {
            btnMantenimiento.setActionCommand(AppActionCommands.CMD_DATOS_MANTENIMIENTO_BD);
            registry.register("btn.datamode.mantenimiento", btnMantenimiento);
        }
        tagToolbar.add(btnMantenimiento);
        tagToolbar.addSeparator();
        
        // --- Botones CRUD (movidos de la toolbar derecha) ---
        JButton btnCreateTag = new JButton(iconUtils.getScaledIcon("30102-Add-Square.png", 24, 24));
        btnCreateTag.setToolTipText("Crear nueva etiqueta (x.y.z)");
        registry.register("btn.datamode.tag.create", btnCreateTag);
        tagToolbar.add(btnCreateTag);
        
        JButton btnEditTag = new JButton(iconUtils.getScaledIcon("30104-Pencil-Square.png", 24, 24));
        btnEditTag.setToolTipText("Modificar etiqueta seleccionada");
        registry.register("btn.datamode.tag.edit", btnEditTag);
        tagToolbar.add(btnEditTag);
        
        JButton btnDeleteTag = new JButton(iconUtils.getScaledIcon("30103-Subtract-Square.png", 24, 24));
        btnDeleteTag.setToolTipText("Borrar etiqueta seleccionada");
        registry.register("btn.datamode.tag.delete", btnDeleteTag);
        tagToolbar.add(btnDeleteTag);
        
        tagToolbar.addSeparator();
        
        // --- Árbol de tags (modelo vacío; se puebla en DataController.initializeTagTree) ---
        JTree allTagsTree = new JTree(new javax.swing.tree.DefaultTreeModel(
            new javax.swing.tree.DefaultMutableTreeNode("Cargando...")));
        allTagsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        
        PopupMenuBuilder popupBuilder = new PopupMenuBuilder(themeManager, configManager);
        List<MenuItemDefinition> treeMenuDefs = new ArrayList<>();
        treeMenuDefs.add(new MenuItemDefinition(AppActionCommands.CMD_DATOS_TAG_NUEVO, MenuItemType.ITEM, "Nuevo "));
        treeMenuDefs.add(new MenuItemDefinition(AppActionCommands.CMD_DATOS_TAG_RENOMBRAR, MenuItemType.ITEM, "Renombrar"));
        treeMenuDefs.add(new MenuItemDefinition(AppActionCommands.CMD_DATOS_TAG_BORRAR, MenuItemType.ITEM, "Borrar"));
        
        JPopupMenu treePopup = popupBuilder.buildPopupMenuWithNestedMenus(treeMenuDefs, actionMap);
        
        allTagsTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showMenu(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showMenu(e);
            }
            private void showMenu(MouseEvent e) {
                int row = allTagsTree.getRowForLocation(e.getX(), e.getY());
                if (row != -1) {
                    allTagsTree.setSelectionRow(row);
                    javax.swing.tree.TreePath path = allTagsTree.getPathForRow(row);
                    if (path != null) {
                        Object lastComponent = path.getLastPathComponent();
                        if (lastComponent instanceof javax.swing.tree.DefaultMutableTreeNode) {
                            javax.swing.tree.DefaultMutableTreeNode node = (javax.swing.tree.DefaultMutableTreeNode) lastComponent;
                            if (node.getUserObject() instanceof modelo.datos.Tag) {
                                modelo.datos.Tag tag = (modelo.datos.Tag) node.getUserObject();
                                boolean isReadOnly = tag.isReadOnly();
                                for (java.awt.Component comp : treePopup.getComponents()) {
                                    if (comp instanceof javax.swing.JMenuItem) {
                                        javax.swing.JMenuItem item = (javax.swing.JMenuItem) comp;
                                        String text = item.getText();
                                        if (text != null && (text.contains("Renombrar") || text.contains("Borrar"))) {
                                            item.setEnabled(!isReadOnly);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                treePopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        
        JScrollPane allTagsScrollPane = new JScrollPane(allTagsTree);
        allTagsScrollPane.getVerticalScrollBar().setUnitIncrement(24);
        
        // --- Vista en Lista Plana ---
        DefaultListModel<modelo.datos.Tag> flatTagListModel = new DefaultListModel<>();
        JList<modelo.datos.Tag> flatTagList = new JList<>(flatTagListModel);
        flatTagList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        flatTagList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof modelo.datos.Tag) {
                    modelo.datos.Tag tag = (modelo.datos.Tag) value;
                    servicios.db.TagDAO tagDAO = (servicios.db.TagDAO) list.getClientProperty("flatTagDAO");
                    
                    @SuppressWarnings("unchecked")
					java.util.List<Long> discoIds = (java.util.List<Long>) list.getClientProperty("flatDiscoIds");
                    
                    int total = (tagDAO != null) ? tagDAO.getImageCountForTag(tag.getId()) : 0;
                    int avail = (tagDAO != null && discoIds != null) ? tagDAO.getAvailableImageCountForTag(tag.getId(), discoIds) : 0;
                    label.setText(tag.getNombre() + " (" + avail + "/" + total + ")");
                    if (!isSelected) {
                        if (tag.isReadOnly()) {
                            label.setFont(label.getFont().deriveFont(Font.PLAIN));
                            Color accent = UIManager.getColor("Component.accentColor");
                            if (accent == null) accent = new Color(50, 150, 255);
                            if (avail == 0 && total > 0) accent = accent.darker();
                            label.setForeground(accent);
                        } else {
                            label.setFont(label.getFont().deriveFont(Font.ITALIC));
                            label.setForeground(UIManager.getColor("Label.foreground"));
                        }
                    }
                }
                return label;
            }
        });
        registry.register("list.datamode.alltags.flat", flatTagList);
        
        JScrollPane flatListScrollPane = new JScrollPane(flatTagList);
        
        // CardLayout para alternar entre árbol y lista
        JPanel treeListContainer = new JPanel(new CardLayout());
        treeListContainer.add(allTagsScrollPane, "TREE");
        treeListContainer.add(flatListScrollPane, "LIST");
        registry.register("panel.datamode.treelist.container", treeListContainer);
        
        btnViewToggle.addActionListener(e -> {
            boolean showingTree = !btnViewToggle.isSelected();
            btnViewToggle.setToolTipText(showingTree ? "Vista por lista" : "Vista por \u00e1rbol");
            CardLayout cl = (CardLayout) treeListContainer.getLayout();
            if (showingTree) {
                cl.show(treeListContainer, "TREE");
            } else {
                cl.show(treeListContainer, "LIST");
            }
            if (dataController != null) dataController.onTagViewSwitched(showingTree);
        });
        
        // IntelliSense unificado: se coloca debajo del toolbar, centrado al ancho de los botones
        TagIntelliSenseField unifiedIntelliSense = new TagIntelliSenseField();
        unifiedIntelliSense.setToolTipText("<html><b>Filtro + comando de etiquetas</b><br>" +
            "• Escribe texto para filtrar la lista de etiquetas<br>" +
            "• Escribe <b>.</b> para explorar/navegar la jerarquía<br>" +
            "• Si el texto coincide con un tag: pulsa [E] o [-] para editar/borrar<br>" +
            "• Si el texto NO coincide: pulsa [+] para crear una nueva etiqueta<br>" +
            "• Tab = autocompletar &middot; Enter = confirmar</html>");
        registry.register("textfield.datamode.tag.intellisense.create", unifiedIntelliSense);

        // Header vertical: toolbar + IntelliSense centrado
        JPanel leftHeader = new JPanel();
        leftHeader.setLayout(new BoxLayout(leftHeader, BoxLayout.Y_AXIS));
        leftHeader.setBorder(BorderFactory.createTitledBorder("Gestión de Etiquetas"));
        leftHeader.add(tagToolbar);

        int toolbarWidth = Math.max(tagToolbar.getPreferredSize().width, 200);
        JPanel intelliSenseRow = new JPanel(new BorderLayout(2, 0));
        // Tornado toggle para la columna izquierda (a la izquierda del IntelliSense)
        JToggleButton btnTornadoIzda = new JToggleButton(iconUtils.getScaledIcon("40001-filter_48x48.png", 20, 20));
        btnTornadoIzda.setToolTipText("Activar/desactivar filtro en vivo en el panel de etiquetas");
        btnTornadoIzda.setSelected(false);
        registry.register("toggle.datamode.tag.tornado", btnTornadoIzda);
        intelliSenseRow.add(btnTornadoIzda, BorderLayout.WEST);
        intelliSenseRow.add(unifiedIntelliSense, BorderLayout.CENTER);
        leftHeader.add(intelliSenseRow);

        // Árbol + header
        JPanel treeWithButtonsPanel = new JPanel(new BorderLayout());
        treeWithButtonsPanel.add(leftHeader, BorderLayout.NORTH);
        treeWithButtonsPanel.add(treeListContainer, BorderLayout.CENTER);

        // Anclar el ancho de la columna izquierda
        leftPanel.setPreferredSize(new Dimension(toolbarWidth + 30, 400));
        leftPanel.add(treeWithButtonsPanel, BorderLayout.CENTER);
        
        // ═══════════════════════════════════════════════════════════════
        // 2. COLUMNA CENTRAL - IMÁGENES + UNIDADES
        // ═══════════════════════════════════════════════════════════════
        JPanel centerPanel = new JPanel(new BorderLayout());
        
        // --- Barra Tornado ---
        JPanel tornadoPanel = new JPanel(new BorderLayout());
        tornadoPanel.setBorder(BorderFactory.createTitledBorder("Filtro de Archivos"));
        JToolBar tornadobar = new JToolBar();
        tornadobar.setFloatable(false);
        
        JToggleButton btnTornado = new JToggleButton(iconUtils.getScaledIcon("40001-filter_48x48.png", 24, 24));
        btnTornado.setToolTipText("Activar/desactivar filtro en vivo");
        btnTornado.setSelected(false);
        registry.register("toggle.datamode.tornado", btnTornado);
        tornadobar.add(btnTornado);

        tornadobar.addSeparator();

        javax.swing.Icon markIcon = iconUtils.getScaledIcon("7003-marcar_imagen_48x48.png", 24, 24);
        javax.swing.Icon markIconSelected = iconUtils.getScaledIcon("7101-marcar_imagen_48x48.png", 24, 24);
        JToggleButton btnDataMark = new javax.swing.JToggleButton(markIcon);
        btnDataMark.setSelectedIcon(markIconSelected);
        btnDataMark.setToolTipText("Marcar/desmarcar im\u00e1genes seleccionadas para el proyecto");
        btnDataMark.setSelected(false);
        registry.register("toggle.datamode.mark", btnDataMark);
        tornadobar.add(btnDataMark);
        
        JTextField tornadoField = new JTextField(20);
        tornadoField.setToolTipText("<html><b>Búsqueda rápida (Tornado):</b><br>" +
            "• Con filtro tornado <b>APAGADO</b>: pulsa Enter para buscar la cadena desde la selección actual<br>" +
            "• Con filtro tornado <b>ENCENDIDO</b>: filtra en vivo los nombres que contienen el texto</html>");
        registry.register("textfield.datamode.tornado", tornadoField);
        tornadobar.add(tornadoField);
        
        tornadoPanel.add(tornadobar, BorderLayout.CENTER);
        centerPanel.add(tornadoPanel, BorderLayout.NORTH);
        
        // --- Lista de nombres de imágenes ---
        DefaultListModel<String> fileNameModel = new DefaultListModel<>();
        JList<String> fileNameList = new JList<>(fileNameModel);
        fileNameList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane fileNameScroll = new JScrollPane(fileNameList);
        TitledBorder imgBorder = BorderFactory.createTitledBorder("Imágenes");
        fileNameScroll.setBorder(imgBorder);
        registry.register("list.datamode.filenames", fileNameList, "WHEEL_NAVIGABLE");
        
        // --- Unidades (movido de columna izquierda) ---
        DriveListPanel driveListPanel = new DriveListPanel();
        driveListPanel.setPreferredSize(new Dimension(100, 200));
        
        JSplitPane centerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, fileNameScroll, driveListPanel);
        centerSplitPane.setResizeWeight(0.7);
        centerSplitPane.setBorder(null);
        
        centerPanel.add(centerSplitPane, BorderLayout.CENTER);
        
        // ═══════════════════════════════════════════════════════════════
        // 3. COLUMNA DERECHA - VISOR + ASIGNACIÓN TAGS
        // ═══════════════════════════════════════════════════════════════
        JPanel rightPanel = new JPanel(new BorderLayout());
        
        // --- Contenedor DisplayModes ---
        JPanel displayModesContainer = new JPanel(new CardLayout());
        registry.register("container.displaymodes.datos", displayModesContainer);
        
        // Visor Single
        ImageDisplayPanel singleImageViewPanel = new ImageDisplayPanel(themeManager, model);
        singleImageViewPanel.setFocusable(true);
        registry.register("panel.datamode.display", singleImageViewPanel);
        registry.register("label.datamode.imagen", singleImageViewPanel.getInternalLabel(), "WHEEL_NAVIGABLE");

        PolaroidDisplayPanel polaroidViewPanel = new PolaroidDisplayPanel(themeManager, model);
        ImageDisplayPanel polaroidImagePanel = polaroidViewPanel.getImagePanel();
        polaroidImagePanel.setFocusable(true);
        registry.register("panel.datamode.display.polaroid", polaroidViewPanel);
        registry.register("panel.datamode.display.polaroid.image", polaroidImagePanel);
        registry.register("label.datamode.polaroid.imagen", polaroidViewPanel.getInternalLabel(), "WHEEL_NAVIGABLE");

        // Flechas de navegacion en los paneles de datos
        if (actionMap != null && iconUtils != null) {
            javax.swing.Action prevAction = actionMap.get(AppActionCommands.CMD_NAV_ANTERIOR);
            javax.swing.Action nextAction = actionMap.get(AppActionCommands.CMD_NAV_SIGUIENTE);
            javax.swing.Icon prevIcon = iconUtils.getScaledIcon("1002-anterior_48x48.png", 48, 48);
            javax.swing.Icon nextIcon = iconUtils.getScaledIcon("1003-siguiente_48x48.png", 48, 48);
            singleImageViewPanel.setNavigationActions(prevAction, nextAction, prevIcon, nextIcon);
            polaroidViewPanel.setNavigationActions(prevAction, nextAction, prevIcon, nextIcon);
            boolean arrowsVisible = this.configManager.getBoolean(ConfigKeys.COMPORTAMIENTO_MOSTRAR_FLECHAS, true);
            singleImageViewPanel.setNavigationArrowsVisible(arrowsVisible);
            polaroidViewPanel.setNavigationArrowsVisible(arrowsVisible);
        }

        ComponentRegistry fakeRegistry = new ComponentRegistry();
        ThumbnailPreviewer gridPreviewer = new ThumbnailPreviewer(null, model, themeManager, null, fakeRegistry);
        GridDisplayPanel gridDisplayPanel = new GridDisplayPanel(model, gridThumbnailService, themeManager, iconUtils, gridPreviewer, projectManager, null, fakeRegistry);
        
        TitledBorder gridBorder = BorderFactory.createTitledBorder("Imágenes con la etiqueta seleccionada");
        displayModesContainer.setBorder(gridBorder);

        displayModesContainer.add(singleImageViewPanel, "VISTA_SINGLE_IMAGE");
        displayModesContainer.add(gridDisplayPanel, "VISTA_GRID");
        displayModesContainer.add(polaroidViewPanel, "VISTA_POLAROID");

        // --- TagManagementPanel ---
        TagManagementPanel tagManagementPanel = new TagManagementPanel();
        tagManagementPanel.setPreferredSize(new Dimension(100, 150));
        
        // --- Toolbar de asignación (lado derecho, simplificada) ---
        JPanel assignPanel = new JPanel(new BorderLayout());
        assignPanel.setBorder(BorderFactory.createTitledBorder("Asignación de Etiquetas"));
        JToolBar assignToolbar = new JToolBar();
        assignToolbar.setFloatable(false);
        
        // Tornado toggle para la columna derecha (filtro de tags asignados) - antes del IntelliSense
        JToggleButton btnTornadoAsig = new JToggleButton(iconUtils.getScaledIcon("40001-filter_48x48.png", 20, 20));
        btnTornadoAsig.setToolTipText("Activar/desactivar filtro en vivo en los tags asignados");
        btnTornadoAsig.setSelected(false);
        registry.register("toggle.datamode.tag.assigned.tornado", btnTornadoAsig);
        assignToolbar.add(btnTornadoAsig);
        
        // IntelliSense para asignar tags (mantiene clave antigua para compatibilidad)
        TagIntelliSenseField assignIntelliSense = new TagIntelliSenseField();
        assignIntelliSense.setColumns(15);
        assignIntelliSense.setToolTipText("<html>Escribe . para explorar jerarquía, o pon el nombre directamente.<br>Tab = autocompletar &middot; Enter = confirmar</html>");
        registry.register("textfield.datamode.tag.intellisense", assignIntelliSense);
        assignToolbar.add(assignIntelliSense);
        
        // [+] = Asignar (crea y asigna)
        JButton btnAssignTag = new JButton(iconUtils.getScaledIcon("30102-Add-Square.png", 24, 24));
        btnAssignTag.setToolTipText("Crear y asignar etiqueta a las imágenes seleccionadas");
        btnAssignTag.setEnabled(false);
        registry.register("btn.datamode.tag.assign", btnAssignTag);
        assignToolbar.add(btnAssignTag);
        
        // [E] = Editar tag
        JButton btnAssignEdit = new JButton(iconUtils.getScaledIcon("30104-Pencil-Square.png", 24, 24));
        btnAssignEdit.setToolTipText("Modificar etiqueta seleccionada");
        registry.register("btn.datamode.tag.edit.right", btnAssignEdit);
        assignToolbar.add(btnAssignEdit);
        
        // [-] = Quitar tag asignado
        JButton btnRemoveAssignedTag = new JButton(iconUtils.getScaledIcon("30107-Delete-Square.png", 24, 24));
        btnRemoveAssignedTag.setToolTipText("Borrar etiqueta asignada de la imagen seleccionada");
        registry.register("btn.datamode.tag.removetag", btnRemoveAssignedTag);
        assignToolbar.add(btnRemoveAssignedTag);
        
        // [V] = Herencia toggle
        JToggleButton btnHerencia = new JToggleButton(iconUtils.getScaledIcon("30106-Check-Square-2.png", 24, 24));
        btnHerencia.setSelectedIcon(iconUtils.getScaledIcon("30105-Layout-Square.png", 24, 24));
        btnHerencia.setToolTipText("Herencia jerárquica: asignar automáticamente tags padre al asignar un tag hijo");
        btnHerencia.setSelected(true);
        registry.register("toggle.datamode.tag.herencia", btnHerencia);
        assignToolbar.add(btnHerencia);

        assignPanel.add(assignToolbar, BorderLayout.CENTER);
        
        JPanel tagMgmtPanelContainer = new JPanel(new BorderLayout());
        tagMgmtPanelContainer.add(assignPanel, BorderLayout.NORTH);
        tagMgmtPanelContainer.add(tagManagementPanel, BorderLayout.CENTER);
        
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, displayModesContainer, tagMgmtPanelContainer);
        rightSplitPane.setResizeWeight(0.7);
        rightSplitPane.setBorder(null);
        
        rightPanel.add(rightSplitPane, BorderLayout.CENTER);
        
        // ═══════════════════════════════════════════════════════════════
        // 4. ENSAMBLADO FINAL - 3 COLUMNAS
        // ═══════════════════════════════════════════════════════════════
        // Split interno: centro | derecha
        JSplitPane innerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerPanel, rightPanel);
        innerSplit.setResizeWeight(0.35);
        innerSplit.setBorder(null);
        
        // Split externo: izquierda | innerSplit
        JSplitPane outerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, innerSplit);
        outerSplitPane.setResizeWeight(0);
        outerSplitPane.setBorder(null);
        
        dataModePanel.add(outerSplitPane, BorderLayout.CENTER);
        
        // ═══════════════════════════════════════════════════════════════
        // 5. REGISTRO DE COMPONENTES
        // ═══════════════════════════════════════════════════════════════
        registry.register("panel.workmode.datos", dataModePanel);
        registry.register("tree.datamode.alltags", allTagsTree);
        registry.register("panel.datamode.grid", gridDisplayPanel);
        registry.register("list.datamode.grid", gridDisplayPanel.getGridList(), "WHEEL_NAVIGABLE");
        registry.register("panel.datamode.tagmanagement", tagManagementPanel);
        registry.register("panel.datamode.drives", driveListPanel);
        registry.register("list.datamode.drives", driveListPanel.getDriveList());
        
        return dataModePanel;
    } // ---FIN de metodo buildDataModePanel---
    
    
} // --- FIN de clase DataBuilder ---
