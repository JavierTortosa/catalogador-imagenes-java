package vista.builders;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.border.TitledBorder;
import javax.swing.tree.TreeSelectionModel;

import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.image.ThumbnailService;
import vista.panels.GridDisplayPanel;
import vista.panels.TagManagementPanel;
import vista.panels.DriveListPanel;
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

    public DataBuilder(ComponentRegistry registry, VisorModel model, ThemeManager themeManager, IconUtils iconUtils, ThumbnailService gridThumbnailService) {
        this.registry = registry;
        this.model = model;
        this.themeManager = themeManager;
        this.iconUtils = iconUtils;
        this.gridThumbnailService = gridThumbnailService;
    } // ---FIN de constructor [DataBuilder]---

    /**
     * Construye y ensambla el panel principal para el Modo Datos.
     * @return El JPanel completamente configurado para ser añadido al CardLayout principal.
     */
    public JPanel buildDataModePanel() {
        JPanel dataModePanel = new JPanel(new BorderLayout());
        
        // --- 1. Panel Izquierdo (CON JTREE Y BOTONES CRUD) ---
        JPanel leftPanel = new JPanel(new BorderLayout());
        TitledBorder allTagsBorder = BorderFactory.createTitledBorder("Biblioteca de Etiquetas");
        leftPanel.setBorder(allTagsBorder);
        
        JTree allTagsTree = new JTree();
        allTagsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        // El TreeModel y el Renderer se asignarán desde el DataController
        
        JScrollPane allTagsScrollPane = new JScrollPane(allTagsTree);
        
        // --- 1.1. Botones CRUD para Tags ---
        JPanel tagButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 2));
        
        JButton btnNewTag = new JButton("Nuevo");
        btnNewTag.setToolTipText("Crear una nueva etiqueta");
        
        JButton btnRenameTag = new JButton("Renombrar");
        btnRenameTag.setToolTipText("Renombrar la etiqueta seleccionada");
        
        JButton btnDeleteTag = new JButton("Borrar");
        btnDeleteTag.setToolTipText("Borrar la etiqueta seleccionada");
        
        tagButtonsPanel.add(btnNewTag);
        tagButtonsPanel.add(btnRenameTag);
        tagButtonsPanel.add(btnDeleteTag);
        
        // Contenedor del árbol + botones
        JPanel treeWithButtonsPanel = new JPanel(new BorderLayout());
        treeWithButtonsPanel.add(allTagsScrollPane, BorderLayout.CENTER);
        treeWithButtonsPanel.add(tagButtonsPanel, BorderLayout.SOUTH);
        
        // --- 1.2. Panel de Discos ---
        DriveListPanel driveListPanel = new DriveListPanel();
        driveListPanel.setPreferredSize(new Dimension(100, 200));
        
        JSplitPane leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeWithButtonsPanel, driveListPanel);
        leftSplitPane.setResizeWeight(0.6);
        leftSplitPane.setBorder(null);
        
        leftPanel.add(leftSplitPane, BorderLayout.CENTER);
        
        // --- 2. Panel Derecho ---

        // ¡LA SOLUCIÓN A LA COLISIÓN!
        // Creamos un ComponentRegistry falso y temporal que se "tragará" los registros del constructor.
        ComponentRegistry fakeRegistry = new ComponentRegistry();
        ThumbnailPreviewer gridPreviewer = new ThumbnailPreviewer(null, model, themeManager, null, fakeRegistry);
        
        // Le pasamos el registry falso para que no contamine el nuestro.
        GridDisplayPanel gridDisplayPanel = new GridDisplayPanel(model, gridThumbnailService, themeManager, iconUtils, gridPreviewer, fakeRegistry);
        
        TitledBorder gridBorder = BorderFactory.createTitledBorder("Imágenes con la etiqueta seleccionada");
        gridDisplayPanel.setBorder(gridBorder);

        TagManagementPanel tagManagementPanel = new TagManagementPanel();
        tagManagementPanel.setPreferredSize(new Dimension(100, 150));
        
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, gridDisplayPanel, tagManagementPanel);
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
        registry.register("list.datamode.grid", gridDisplayPanel.getGridList()); // <- Clave única
        registry.register("panel.datamode.tagmanagement", tagManagementPanel);
        registry.register("panel.datamode.drives", driveListPanel);
        registry.register("list.datamode.drives", driveListPanel.getDriveList());
        
        // Registrar botones CRUD de tags
        registry.register("btn.datamode.tag.new", btnNewTag);
        registry.register("btn.datamode.tag.rename", btnRenameTag);
        registry.register("btn.datamode.tag.delete", btnDeleteTag);
        
        return dataModePanel;
    } // ---FIN de metodo buildDataModePanel---

} // --- FIN de clase DataBuilder ---
