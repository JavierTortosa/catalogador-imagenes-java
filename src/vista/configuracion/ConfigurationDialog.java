package vista.configuracion;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.ConfigApplicationManager;
import servicios.ConfigurationManager;
import vista.theme.ThemeManager;

/**
 * Diálogo para la configuración avanzada de la aplicación, que organiza los paneles de opciones
 * mediante un árbol de navegación y un diseño de tarjetas (CardLayout).
 */
public class ConfigurationDialog extends JDialog {

    private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(ConfigurationDialog.class);

    private final transient ConfigurationManager config;
    private final transient ConfigApplicationManager configAppManager;

    private JTree tree;
    private final CardLayout cardLayout;
    private final JPanel cardsPanel;
    private DefaultMutableTreeNode rootNode;

    /**
     * Constructor del diálogo de configuración.
     *
     * @param owner Ventana propietaria
     * @param config Gestor de configuración
     * @param configAppManager Gestor de aplicación de configuración
     * @param themeManager Gestor de temas
     */
    public ConfigurationDialog(JFrame owner, ConfigurationManager config,
                                ConfigApplicationManager configAppManager,
                                ThemeManager themeManager) {
        super(owner, "Configuración Avanzada", true);
        this.config = config;
        this.configAppManager = configAppManager;

        this.cardLayout = new CardLayout();
        this.cardsPanel = new JPanel(cardLayout);

        initComponents(owner, themeManager);
        setSize(850, 600);
        setLocationRelativeTo(owner);
    } // --- Fin del metodo/clase ConfigurationDialog ---


    /**
     * Inicializa los componentes de la interfaz de usuario.
     *
     * @param owner Ventana propietaria
     * @param themeManager Gestor de temas
     */
    private void initComponents(JFrame owner, ThemeManager themeManager) {
        setLayout(new BorderLayout());

        // Tree on the left
        this.rootNode = buildTreeNodes(owner, themeManager);
        this.tree = createTree(rootNode);

        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setPreferredSize(new Dimension(200, 0));
        treeScroll.setMinimumSize(new Dimension(150, 0));

        // Panels on the right
        registerPanels(cardsPanel, owner, themeManager);

        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, cardsPanel);
        splitPane.setDividerLocation(220);
        splitPane.setResizeWeight(0.0);
        add(splitPane, BorderLayout.CENTER);

        // Bottom buttons
        add(createButtonPanel(), BorderLayout.SOUTH);

        // Expand all + select first leaf
        expandAll(tree);
        selectFirstLeaf();
    } // --- Fin del metodo/clase initComponents ---


    /**
     * Construye los nodos del árbol de navegación.
     *
     * @param owner Ventana propietaria
     * @param themeManager Gestor de temas
     * @return Nodo raíz del árbol
     */
    private DefaultMutableTreeNode buildTreeNodes(JFrame owner, ThemeManager themeManager) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new CategoryNode("Configuración", null));
        addCategoryNode(root, "General");
        addCategoryNode(root, "Rutas");
        addCategoryNode(root, "Navegación");
        addCategoryNode(root, "Zoom y Visualización");
        addCategoryNode(root, "Miniaturas");
        addCategoryNode(root, "Apariencia");
        addCategoryNode(root, "Barras de Herramientas");
        addCategoryNode(root, "Paneles de Información");
        addCategoryNode(root, "Grid");
        addCategoryNode(root, "Proyecto");
        return root;
    } // --- Fin del metodo/clase buildTreeNodes ---


    /**
     * Añade un nodo de categoría al árbol.
     *
     * @param parent Nodo padre
     * @param name Nombre de la categoría
     */
    private void addCategoryNode(DefaultMutableTreeNode parent, String name) {
        parent.add(new DefaultMutableTreeNode(new CategoryNode(name, null)));
    } // --- Fin del metodo/clase addCategoryNode ---


    /**
     * Crea y configura el JTree de navegación.
     *
     * @param root Nodo raíz del árbol
     * @return JTree configurado
     */
    private JTree createTree(DefaultMutableTreeNode root) {
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        JTree newTree = new JTree(treeModel);
        newTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        newTree.setRootVisible(false);
        newTree.setShowsRootHandles(true);

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        newTree.setCellRenderer(renderer);

        newTree.addTreeSelectionListener(this::onTreeSelection);
        return newTree;
    } // --- Fin del metodo/clase createTree ---


    /**
     * Maneja la selección de nodos en el árbol de navegación.
     *
     * @param e Evento de selección de árbol
     */
    private void onTreeSelection(TreeSelectionEvent e) {
        TreePath path = e.getNewLeadSelectionPath();
        if (path == null) return;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (node.getUserObject() instanceof CategoryNode catNode) {
            String name = catNode.getDisplayName();
            for (int i = 0; i < cardsPanel.getComponentCount(); i++) {
                java.awt.Component comp = cardsPanel.getComponent(i);
                if (comp.getName() != null && comp.getName().equals(name)) {
                    cardLayout.show(cardsPanel, name);
                    return;
                }
            }
        }
    } // --- Fin del metodo/clase onTreeSelection ---


    /**
     * Registra todos los paneles de configuración en el CardLayout.
     *
     * @param cards Panel de tarjetas
     * @param owner Ventana propietaria
     * @param themeManager Gestor de temas
     */
    private void registerPanels(JPanel cards, JFrame owner, ThemeManager themeManager) {
        registerPanel(cards, new vista.configuracion.panels.GeneralPanel(config));
        registerPanel(cards, new vista.configuracion.panels.PathsPanel(config));
        registerPanel(cards, new vista.configuracion.panels.NavigationPanel(config));
        registerPanel(cards, new vista.configuracion.panels.ZoomPanel(config));
        registerPanel(cards, new vista.configuracion.panels.ThumbnailPanel(config));
        registerPanel(cards, new vista.configuracion.panels.AppearancePanel(config, themeManager, owner));
        registerPanel(cards, new vista.configuracion.panels.ToolbarVisibilityPanel(config));
        registerPanel(cards, new vista.configuracion.panels.InfobarPanel(config));
        registerPanel(cards, new vista.configuracion.panels.GridPanel(config));
        registerPanel(cards, new vista.configuracion.panels.ProjectPanel(config));
    } // --- Fin del metodo/clase registerPanels ---


    /**
     * Registra un panel de configuración específico.
     *
     * @param cards Panel de tarjetas
     * @param panel Panel de configuración
     */
    private void registerPanel(JPanel cards, ConfigurationPanel panel) {
        panel.load(config);
        JPanel p = (JPanel) panel;
        p.setName(panel.getTitle());
        cards.add(p, panel.getTitle());
    } // --- Fin del metodo/clase registerPanel ---


    /**
     * Crea el panel de botones inferiores.
     *
     * @return Panel con botones de acción
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnOk = new JButton("Aceptar");
        btnOk.addActionListener(e -> onAccept());

        JButton btnApply = new JButton("Aplicar");
        btnApply.addActionListener(e -> onApply());

        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> dispose());

        panel.add(btnOk);
        panel.add(btnApply);
        panel.add(btnCancel);
        return panel;
    } // --- Fin del metodo/clase createButtonPanel ---


    /**
     * Maneja la acción de aceptar, guardando la configuración y cerrando el diálogo.
     */
    private void onAccept() {
        if (saveAll()) {
            try {
                config.guardarConfiguracion(config.getConfigMap());
                if (configAppManager != null) {
                    configAppManager.aplicarConfiguracionGlobalmente();
                }
            } catch (java.io.IOException ex) {
                logger.error("Error al guardar la configuración", ex);
            }
        }
        dispose();
    } // --- Fin del metodo/clase onAccept ---


    /**
     * Maneja la acción de aplicar, guardando la configuración sin cerrar el diálogo.
     */
    private void onApply() {
        if (saveAll()) {
            try {
                config.guardarConfiguracion(config.getConfigMap());
                if (configAppManager != null) {
                    configAppManager.aplicarConfiguracionGlobalmente();
                }
            } catch (java.io.IOException ex) {
                logger.error("Error al guardar la configuración", ex);
            }
        }
    } // --- Fin del metodo/clase onApply ---


    /**
     * Guarda la configuración de todos los paneles registrados.
     *
     * @return true si hubo cambios, false en caso contrario
     */
    private boolean saveAll() {
        boolean changed = false;
        for (int i = 0; i < cardsPanel.getComponentCount(); i++) {
            java.awt.Component comp = cardsPanel.getComponent(i);
            if (comp instanceof ConfigurationPanel cp) {
                if (cp.save(config)) {
                    changed = true;
                }
            }
        }
        return changed;
    } // --- Fin del metodo/clase saveAll ---


    /**
     * Expande todos los nodos del árbol.
     *
     * @param tree El árbol a expandir
     */
    private void expandAll(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    } // --- Fin del metodo/clase expandAll ---


    /**
     * Selecciona la primera hoja del árbol de navegación.
     */
    private void selectFirstLeaf() {
        DefaultMutableTreeNode firstLeaf = findFirstLeaf(rootNode);
        if (firstLeaf != null) {
            TreePath path = new TreePath(firstLeaf.getPath());
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
        }
    } // --- Fin del metodo/clase selectFirstLeaf ---


    /**
     * Busca recursivamente la primera hoja en un nodo del árbol.
     *
     * @param node Nodo actual
     * @return La primera hoja encontrada, o null
     */
    private DefaultMutableTreeNode findFirstLeaf(DefaultMutableTreeNode node) {
        if (node.isLeaf() && node.getUserObject() instanceof CategoryNode) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            DefaultMutableTreeNode found = findFirstLeaf(child);
            if (found != null) return found;
        }
        return null;
    } // --- Fin del metodo/clase findFirstLeaf ---


} // --- Fin del metodo/clase ConfigurationDialog ---
