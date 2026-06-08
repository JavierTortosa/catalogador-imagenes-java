package vista.builders;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.tree.TreeSelectionModel;

import controlador.DataController;
import controlador.commands.AppActionCommands;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigurationManager;
import servicios.image.ThumbnailService;
import vista.config.MenuItemDefinition;
import vista.config.MenuItemType;
import vista.panels.DriveListPanel;
import vista.panels.GridDisplayPanel;
import vista.panels.ImageDisplayPanel;
import vista.panels.PolaroidDisplayPanel;
import vista.panels.TagManagementPanel;
import vista.components.TagIntelliSenseField;
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

    /**
     * Construye y ensambla el panel principal para el Modo Datos.
     * @return El JPanel completamente configurado para ser añadido al CardLayout principal.
     */
    public JPanel buildDataModePanel() {
        JPanel dataModePanel = new JPanel(new BorderLayout());
        
        // --- 1. Panel Izquierdo (CON JTREE, BARRA Y BOTONES CRUD) ---
        JPanel leftPanel = new JPanel(new BorderLayout());
        TitledBorder allTagsBorder = BorderFactory.createTitledBorder("Biblioteca de Etiquetas");
        leftPanel.setBorder(allTagsBorder);
        
        JToolBar tagToolbar = new JToolBar();
        tagToolbar.setFloatable(false);
        
        // Botones vista (lista/árbol)
        JButton btnVistaLista = new JButton(iconUtils.getScaledIcon("30100-Vector.png", 24, 24));
        btnVistaLista.setToolTipText("Vista por lista");
        btnVistaLista.addActionListener(actionMap.get(AppActionCommands.CMD_DATOS_TAGS_VISTA_LISTA));
        
        JButton btnVistaArbol = new JButton(iconUtils.getScaledIcon("30101-Hierarchy.png", 24, 24));
        btnVistaArbol.setToolTipText("Vista por árbol");
        btnVistaArbol.addActionListener(actionMap.get(AppActionCommands.CMD_DATOS_TAGS_VISTA_ARBOL));
        
        tagToolbar.add(btnVistaLista);
        tagToolbar.add(btnVistaArbol);
        tagToolbar.addSeparator();
        
        // Botón orden
        tagToolbar.add(new JButton(actionMap.get(AppActionCommands.CMD_DATOS_TAGS_ORDENAR)));
        tagToolbar.addSeparator();
        
        // Textbox filtro
        JTextField filterField = new JTextField(10);
        filterField.setToolTipText("Filtrar etiquetas...");
        tagToolbar.add(filterField);
        registry.register("textfield.datamode.tags.filter", filterField);
        tagToolbar.addSeparator();
        
        // Botón Mantenimiento BD
        JButton btnMantenimiento = new JButton(iconUtils.getScaledIcon("7005-settings_48x48.png", 24, 24)); // Usamos un icono genérico por ahora
        if (btnMantenimiento.getIcon() == null) btnMantenimiento.setText("Mantenimiento");
        btnMantenimiento.setToolTipText("Mantenimiento de Base de Datos");
        if (actionMap != null && actionMap.containsKey(AppActionCommands.CMD_DATOS_MANTENIMIENTO_BD)) {
            btnMantenimiento.addActionListener(actionMap.get(AppActionCommands.CMD_DATOS_MANTENIMIENTO_BD));
        } else {
            // Fallback si no está en el actionMap
            btnMantenimiento.setActionCommand(AppActionCommands.CMD_DATOS_MANTENIMIENTO_BD);
            registry.register("btn.datamode.mantenimiento", btnMantenimiento);
        }
        tagToolbar.add(btnMantenimiento);
        tagToolbar.addSeparator();
        
        // --- Árbol ---
        JTree allTagsTree = new JTree();
        allTagsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        
        // --- Menú Contextual para el Árbol ---
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
                    java.util.List<Long> discoIds = (java.util.List<Long>) list.getClientProperty("flatDiscoIds");
                    int total = (tagDAO != null) ? tagDAO.getImageCountForTag(tag.getId()) : 0;
                    int avail = (tagDAO != null && discoIds != null) ? tagDAO.getAvailableImageCountForTag(tag.getId(), discoIds) : 0;
                    label.setText(tag.getNombre() + " (" + avail + "/" + total + ")");
                    if (!isSelected) {
                        if (tag.isReadOnly()) {
                            if (avail == 0 && total > 0) label.setForeground(Color.GRAY);
                        } else {
                            Color accent = UIManager.getColor("Component.accentColor");
                            if (accent == null) accent = new Color(50, 150, 255);
                            if (avail == 0 && total > 0) accent = accent.darker();
                            label.setForeground(accent);
                        }
                    }
                }
                return label;
            }
        });
        // Registramos la lista plana para que DataController pueda usarla y poblarla
        registry.register("list.datamode.alltags.flat", flatTagList);
        
        JScrollPane flatListScrollPane = new JScrollPane(flatTagList);
        
        // Contenedor con CardLayout para alternar entre árbol y lista
        JPanel treeListContainer = new JPanel(new CardLayout());
        treeListContainer.add(allTagsScrollPane, "TREE");
        treeListContainer.add(flatListScrollPane, "LIST");
        registry.register("panel.datamode.treelist.container", treeListContainer);
        
        // Toggle de vista botones
        btnVistaLista.addActionListener(e -> {
            CardLayout cl = (CardLayout) treeListContainer.getLayout();
            cl.show(treeListContainer, "LIST");
            if (dataController != null) dataController.onTagViewSwitched(false);
        });
        btnVistaArbol.addActionListener(e -> {
            CardLayout cl = (CardLayout) treeListContainer.getLayout();
            cl.show(treeListContainer, "TREE");
            if (dataController != null) dataController.onTagViewSwitched(true);
        });
        
        // Contenedor del árbol/botones
        JPanel treeWithButtonsPanel = new JPanel(new BorderLayout());
        treeWithButtonsPanel.add(tagToolbar, BorderLayout.NORTH);
        treeWithButtonsPanel.add(treeListContainer, BorderLayout.CENTER);
        
        // --- 1.2. Panel de Discos ---
        DriveListPanel driveListPanel = new DriveListPanel();
        driveListPanel.setPreferredSize(new Dimension(100, 200));
        
        JSplitPane leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeWithButtonsPanel, driveListPanel);
        leftSplitPane.setResizeWeight(0.6);
        leftSplitPane.setBorder(null);
        
        leftPanel.add(leftSplitPane, BorderLayout.CENTER);
        
        // --- 2. Panel Derecho ---

        // --- Contenedor DisplayModes ---
        JPanel displayModesContainer = new JPanel(new java.awt.CardLayout());
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

        // ¡LA SOLUCIÓN A LA COLISIÓN!
        // Creamos un ComponentRegistry falso y temporal que se "tragará" los registros del constructor.
        ComponentRegistry fakeRegistry = new ComponentRegistry();
        ThumbnailPreviewer gridPreviewer = new ThumbnailPreviewer(null, model, themeManager, null, fakeRegistry);
        
        // Le pasamos el registry falso para que no contamine el nuestro.
        GridDisplayPanel gridDisplayPanel = new GridDisplayPanel(model, gridThumbnailService, themeManager, iconUtils, gridPreviewer, fakeRegistry);
        
        TitledBorder gridBorder = BorderFactory.createTitledBorder("Imágenes con la etiqueta seleccionada");
        displayModesContainer.setBorder(gridBorder);

        displayModesContainer.add(singleImageViewPanel, "VISTA_SINGLE_IMAGE");
        displayModesContainer.add(gridDisplayPanel, "VISTA_GRID");
        displayModesContainer.add(polaroidViewPanel, "VISTA_POLAROID");

        // --- TagManagementPanel con su propia estructura ---
        TagManagementPanel tagManagementPanel = new TagManagementPanel();
        tagManagementPanel.setPreferredSize(new Dimension(100, 150));
        
        // Área A: Gestión de Taxonomía (CRUD)
        JToolBar taxonomyToolbar = new JToolBar();
        taxonomyToolbar.setFloatable(false);
        
        JButton btnCreateTag = new JButton("Crear", iconUtils.getScaledIcon("30102-Add-Square.png", 24, 24));
        btnCreateTag.setToolTipText("Crear nueva etiqueta (x.y.z)");
        registry.register("btn.datamode.tag.create", btnCreateTag);
        taxonomyToolbar.add(btnCreateTag);
        
        JButton btnEditTag = new JButton("Modificar", iconUtils.getScaledIcon("30104-Pencil-Square.png", 24, 24));
        btnEditTag.setToolTipText("Modificar etiqueta seleccionada");
        registry.register("btn.datamode.tag.edit", btnEditTag);
        taxonomyToolbar.add(btnEditTag);
        
        JButton btnDeleteTag = new JButton("Borrar", iconUtils.getScaledIcon("30103-Subtract-Square.png", 24, 24));
        btnDeleteTag.setToolTipText("Borrar etiqueta seleccionada");
        registry.register("btn.datamode.tag.delete", btnDeleteTag);
        taxonomyToolbar.add(btnDeleteTag);
        
        // Área B: Asignación de Etiquetas a Imágenes
        JPanel assignmentPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        
        javax.swing.JCheckBox chkHerencia = new javax.swing.JCheckBox("Herencia Jerárquica", true);
        chkHerencia.setToolTipText("Asignar la etiqueta y todas sus etiquetas padre superiores.");
        registry.register("checkbox.datamode.tag.herencia", chkHerencia);
        assignmentPanel.add(chkHerencia);
        
        TagIntelliSenseField intelliSenseField = new TagIntelliSenseField();
        intelliSenseField.setColumns(15);
        intelliSenseField.setToolTipText("<html>Escribe . para explorar jerarquia, o pon el nombre directamente.<br>Tab = autocompletar &middot; Enter = confirmar</html>");
        registry.register("textfield.datamode.tag.intellisense", intelliSenseField);
        assignmentPanel.add(intelliSenseField);
        
        JButton btnAssignTag = new JButton("Asignar Tag");
        btnAssignTag.setToolTipText("Asignar etiqueta a las imágenes seleccionadas");
        btnAssignTag.setEnabled(false); // Por defecto deshabilitado hasta seleccionar imágenes
        registry.register("btn.datamode.tag.assign", btnAssignTag);
        assignmentPanel.add(btnAssignTag);
        
        JPanel tagMgmtPanelContainer = new JPanel(new BorderLayout());
        
        // Contenedor para las dos áreas (Arriba la toolbar CRUD, luego la asignación)
        JPanel topToolbars = new JPanel(new java.awt.GridLayout(2, 1));
        topToolbars.add(taxonomyToolbar);
        topToolbars.add(assignmentPanel);
        
        tagMgmtPanelContainer.add(topToolbars, BorderLayout.NORTH);
        tagMgmtPanelContainer.add(tagManagementPanel, BorderLayout.CENTER);
        
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, displayModesContainer, tagMgmtPanelContainer);
        rightSplitPane.setResizeWeight(0.8);
        rightSplitPane.setBorder(null);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightSplitPane);
        mainSplitPane.setResizeWeight(0.2);
        mainSplitPane.setBorder(null);

        dataModePanel.add(mainSplitPane, BorderLayout.CENTER);
        
        // --- 3. REGISTRO MANUAL Y CORRECTO ---
        // Ahora nosotros registramos los componentes con las claves correctas y sin colisiones.
        registry.register("panel.workmode.datos", dataModePanel);
        // ¡CAMBIO CRÍTICO! Registramos el JTree con una nueva clave.
        registry.register("tree.datamode.alltags", allTagsTree);
        registry.register("panel.datamode.grid", gridDisplayPanel);
        registry.register("list.datamode.grid", gridDisplayPanel.getGridList(), "WHEEL_NAVIGABLE"); // <- Clave única
        registry.register("panel.datamode.tagmanagement", tagManagementPanel);
        registry.register("panel.datamode.drives", driveListPanel);
        registry.register("list.datamode.drives", driveListPanel.getDriveList());
        
        return dataModePanel;
    } // ---FIN de metodo buildDataModePanel---
    
    
} // --- FIN de clase DataBuilder ---
