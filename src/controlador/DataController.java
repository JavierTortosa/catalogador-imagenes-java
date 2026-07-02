package controlador;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.DataManager;
import controlador.managers.InfobarStatusManager;
import controlador.managers.interfaces.IProjectManager;
import controlador.utils.ComponentRegistry;
import controlador.utils.TornadoFilterController;
import modelo.ListContext;
import modelo.VisorModel;
import modelo.datos.Disco;
import modelo.datos.Tag;
import servicios.db.TagDAO;
import vista.components.TagIntelliSenseField;
import vista.panels.DriveListPanel;
import vista.panels.ImageDisplayPanel;
import vista.panels.PolaroidDisplayPanel;
import vista.panels.TagManagementPanel;
import vista.tree.TagTreeCellRenderer;
import vista.tree.TagTreeModel;

/**
 * Controlador para la lógica y la interacción del "Modo Datos".
 */
public class DataController {

    private static final Logger logger = LoggerFactory.getLogger(DataController.class);

    private final VisorModel model;
    private final ComponentRegistry registry;
    private final DataManager dataManager;
    private IProjectManager projectManager;
    private InfobarStatusManager statusBarManager;
    private VisorController visorController;
    private boolean isInitialized = false;

    // Copia del orden original de la lista plana (para restaurar al desactivar orden)
    private List<Tag> flatListOriginalOrder = new ArrayList<>();

    // Modelo y renderer persistentes del árbol (no se recrean en cada refresh)
    private TagTreeModel tagTreeModel;
    private TagTreeCellRenderer tagTreeRenderer;

    // Indica si la vista actual de tags es árbol (true) o lista plana (false)
    private boolean tagTreeViewActive = true;

    // Estado de expansión guardado antes de filtrar el árbol (para restaurar al desactivar tornado)
    private java.util.Set<Long> savedExpandedTagIds = new java.util.HashSet<>();

    // Flag para saber si el tornado forzó la vista lista estando en modo árbol
    private boolean tornadoForcedListView = false;

    // Flag para evitar bucles de sincronización entre lista central y grid
    private boolean isSyncingLists = false;

    // Flag para evitar refrescar IntelliSense repetidamente si los datos no han cambiado
    private boolean intelliSenseRefreshed = false;

    // Datos pendientes de sincronización desde VISUALIZADOR (entrante desde AppModeService)
    private Path pendingSyncPath = null;
    private String pendingSyncKey = null;

    /**
     * Constructor del controlador de datos.
     *
     * @param model Modelo de la aplicación
     * @param registry Registro de componentes
     * @param dataManager Gestor de datos
     */
    public DataController(VisorModel model, ComponentRegistry registry, DataManager dataManager) {
        this.model = model;
        this.registry = registry;
        this.dataManager = dataManager;
    } // --- Fin del metodo/clase DataController ---


    /**
     * Devuelve el gestor de datos.
     *
     * @return DataManager gestor de datos
     */
    public DataManager getDataManager() {
        return this.dataManager;
    } // --- Fin del metodo/clase getDataManager ---

    /**
     * Permite inyectar el VisorController principal para acceder a atajos y acciones globales.
     * @param visorController La instancia del visor controller principal.
     */
    public void setVisorController(VisorController visorController) {
        this.visorController = visorController;
    } // --- Fin del metodo/clase setVisorController ---


    /**
     * Permite inyectar el ProjectManager para poder marcar imágenes desde el Modo Datos.
     * @param projectManager La instancia del gestor de proyectos.
     */
    public void setProjectManager(IProjectManager projectManager) {
        this.projectManager = projectManager;
    } // --- Fin del metodo/clase setProjectManager ---


    /**
     * Permite inyectar el StatusBarManager para mostrar mensajes de estado.
     * @param statusBarManager Gestor de la barra de estado
     */
    public void setStatusBarManager(InfobarStatusManager statusBarManager) {
        this.statusBarManager = statusBarManager;
    } // --- Fin del metodo/clase setStatusBarManager ---


    /**
     * Establece el gestor de etiquetas (tagging).
     * @param taggingManager Gestor de etiquetas
     */
    /**
     * Inicializa el controlador. Carga los datos iniciales y configura los listeners.
     * Se llama solo una vez.
     */
    public void initialize() {
        if (isInitialized) return;
        logger.debug("Inicializando DataController por primera vez...");
        
        // Inicializaciones ligeras (solo listeners, sin carga de datos pesada)
        initializeFileNameList();
        initializeTornadoFilter();
        initializeLeftTagTornadoFilter();
        initializeAssignedTagTornadoFilter();
        setupTagManagementCallbacks();
        setupListeners();
        setupTagCRUDButtons();
        setupImageTagCRUDButtons();
        setupMaintenanceDialog();
        initializeMarkButton();
        
        // Tareas pesadas (carga de tags, cómputo de counts, escaneo de discos) se ejecutan
        // en segundo plano para no bloquear la UI al entrar al modo datos
        logger.debug("Iniciando carga diferida de datos en segundo plano...");
        new Thread(() -> {
            try {
                initializeTagTree();
                initializeFlatTagList();
            } catch (Exception ex) {
                logger.error("Error en carga inicial de tags", ex);
            }
            try {
                initializeDriveList();
            } catch (Exception ex) {
                logger.error("Error en carga inicial de unidades", ex);
            }
            
            // Actualizar el renderer del árbol con los ids de discos conectados
            if (tagTreeRenderer != null) {
                tagTreeRenderer.setConnectedDiscoIds(dataManager.getConnectedDiscoIds());
                tagTreeRenderer.refreshCache();
            }
            
            // Refrescar vista si hay selección inicial
            javax.swing.SwingUtilities.invokeLater(() -> {
                JTree tree = registry.get("tree.datamode.alltags");
                if (tree != null && tree.getSelectionPath() == null) {
                    // Si hay tags cargados, seleccionar el primero
                    if (tree.getRowCount() > 0) {
                        tree.setSelectionRow(0);
                    }
                }
                logger.debug("Carga diferida del modo datos completada.");
            });
        }, "DataController-init").start();
        
        isInitialized = true;
    } // --- Fin del metodo/clase initialize ---


    /**
     * Configura el diálogo de mantenimiento de la base de datos.
     */
    private void setupMaintenanceDialog() {
        javax.swing.JButton btn = registry.get("btn.datamode.mantenimiento");
        if (btn != null) {
            btn.addActionListener(e -> {
                java.awt.Frame mainFrame = null;
                java.awt.Component topComp = btn.getTopLevelAncestor();
                if (topComp instanceof java.awt.Frame) mainFrame = (java.awt.Frame) topComp;
                
                vista.dialogos.DatabaseMaintenanceDialog dialog = new vista.dialogos.DatabaseMaintenanceDialog(mainFrame, dataManager);
                dialog.setVisible(true);
                
                if (dialog.isDbChanged()) {
                    afterTagStructureChanged(null);
                }
            });
        }
    } // --- Fin del metodo/clase setupMaintenanceDialog ---
    
    /**
     * Activa el modo datos. Se llama cada vez que el usuario cambia a esta vista.
     * Recarga el árbol de tags para reflejar los últimos cambios en la BD.
     */
    public void activate() {
        logger.debug("Activando el Modo Datos...");
        
        // Guardar la clave seleccionada antes de que activate() la modifique
        String savedImageKey = model != null ? model.getDatosListContext().getSelectedImageKey() : null;
        
        // Recargamos el árbol cada vez que entramos en este modo.
        refreshTagTreeAndSelect(null);
        refreshFlatTagList();
        refreshDriveList();
        refreshAvailableTags();
        if (!intelliSenseRefreshed) {
            refreshIntelliSense();
            intelliSenseRefreshed = true;
        }
        
        // Sincronizar desde TaggingManager si hay datos pendientes de VISUALIZADOR
        JTree allTagsTree = registry.get("tree.datamode.alltags");
        if (allTagsTree != null) {
            if (pendingSyncPath != null) {
                logger.debug("Procesando pendingSyncPath: {}", pendingSyncPath);
                TagDAO tagDAO = dataManager.getTagDAO();
                java.util.List<Tag> imageTags = dataManager.getTagsForImage(pendingSyncPath);
                Tag deepestSystemTag = null;
                int maxDepth = -1;
                for (Tag t : imageTags) {
                    if (t.isReadOnly()) {
                        int depth = 0;
                        Long pid = t.getParentId();
                        while (pid != null) {
                            depth++;
                            java.util.Optional<Tag> parentOpt = tagDAO.findTagById(pid);
                            if (parentOpt.isPresent()) {
                                pid = parentOpt.get().getParentId();
                            } else {
                                break;
                            }
                        }
                        logger.debug("  tag '{}' (id={}): depth={}", t.getNombre(), t.getId(), depth);
                        if (depth > maxDepth) {
                            maxDepth = depth;
                            deepestSystemTag = t;
                        }
                    }
                }
                if (deepestSystemTag != null) {
                    logger.debug("Seleccionando tag del sistema más profundo: id={}, nombre={} (depth={})",
                        deepestSystemTag.getId(), deepestSystemTag.getNombre(), maxDepth);
                    final long tagId = deepestSystemTag.getId();
                    SwingUtilities.invokeLater(() -> selectTagInTree(tagId));
                } else {
                    logger.debug("No se encontró ningún tag del sistema para la imagen: {}", pendingSyncPath);
                }
                if (pendingSyncKey != null) {
                    model.getDatosListContext().setSelectedImageKey(pendingSyncKey);
                }
                pendingSyncPath = null;
                pendingSyncKey = null;
            } else {
                String savedTag = model != null ? model.getDatosListContext().getDatosSelectedTag() : null;
                if (savedTag != null && !"Biblioteca".equals(savedTag)) {
                    selectTagNode(allTagsTree, savedTag);
                }
            }
            if (allTagsTree.getSelectionCount() == 0) {
                allTagsTree.setSelectionRow(0);
            }
        }
        
        TagManagementPanel tagPanel = registry.get("panel.datamode.tagmanagement");
        if (tagPanel != null) {
            tagPanel.clearPanel();
        }
        
        // Restaurar la seleccion del grid DESPUES de que loadAllImages/loadImagesForTag
        // (invocados desde el TreeSelectionListener) hayan reemplazado el modelo.
        SwingUtilities.invokeLater(() -> {
            JList<String> gridList = registry.get("list.datamode.grid");
            if (gridList == null) return;
            String keyRestore = model != null ? model.getDatosListContext().getSelectedImageKey() : null;
            if (keyRestore == null) keyRestore = savedImageKey;
            if (keyRestore != null) {
                for (int i = 0; i < gridList.getModel().getSize(); i++) {
                    if (keyRestore.equals(gridList.getModel().getElementAt(i))) {
                        gridList.setSelectedIndex(i);
                        gridList.ensureIndexIsVisible(i);
                        return;
                    }
                }
            }
            // Sin clave guardada o no encontrada: seleccionar el primer elemento
            if (gridList.getModel().getSize() > 0) {
                gridList.setSelectedIndex(0);
            }
        });
    } // --- Fin del metodo/clase activate ---


    /**
     * Establece la ruta y la clave de la imagen pendiente de sincronización.
     * @param path Ruta de la imagen
     * @param key Clave de la imagen
     */
    public void setPendingSyncFromVisualizador(Path path, String key) {
        this.pendingSyncPath = path;
        this.pendingSyncKey = key;
    } // --- Fin del metodo/clase setPendingSyncFromVisualizador ---


    /**
     * Guarda el contexto actual del modo datos en el modelo.
     */
    public void guardarContexto() {
        if (model == null) return;
        ListContext ctx = model.getDatosListContext();
        if (ctx == null) return;

        JTree allTagsTree = registry.get("tree.datamode.alltags");
        if (allTagsTree != null) {
            Object selectedNode = null;
            TreePath selPath = allTagsTree.getSelectionPath();
            if (selPath != null) {
                selectedNode = selPath.getLastPathComponent();
            }
            if (selectedNode instanceof Tag) {
                ctx.setDatosSelectedTag(((Tag) selectedNode).getNombre());
            } else if ("Biblioteca".equals(selectedNode)) {
                ctx.setDatosSelectedTag("Biblioteca");
            }
        }

        // Guardar selección de imagen (desde grid o lista central)
        String selectedKey = null;
        JList<String> gridList = registry.get("list.datamode.grid");
        if (gridList != null) {
            selectedKey = gridList.getSelectedValue();
        }
        if (selectedKey == null) {
            JList<String> fileNameList = registry.get("list.datamode.filenames");
            if (fileNameList != null) {
                selectedKey = fileNameList.getSelectedValue();
            }
        }
        ctx.setSelectedImageKey(selectedKey);
    } // --- Fin del metodo/clase guardarContexto ---


    /**
     * Selecciona un nodo en el árbol de tags dado un nombre de tag.
     * @param tree Árbol de tags
     * @param tagName Nombre del tag
     */
    private void selectTagNode(JTree tree, String tagName) {
        javax.swing.tree.TreeModel model = tree.getModel();
        if (model == null || model.getRoot() == null) return;
        buscarYSeleccionarNodo(tree, model.getRoot(), tagName);
    } // --- Fin del metodo/clase selectTagNode ---


    /**
     * Busca y selecciona un nodo en el árbol de tags recursivamente.
     * @param tree Árbol de tags
     * @param node Nodo actual
     * @param tagName Nombre del tag
     * @return true si se encontró y seleccionó, false en caso contrario
     */
    private boolean buscarYSeleccionarNodo(JTree tree, Object node, String tagName) {
        if (node instanceof javax.swing.tree.DefaultMutableTreeNode) {
            javax.swing.tree.DefaultMutableTreeNode treeNode = (javax.swing.tree.DefaultMutableTreeNode) node;
            Object userObj = treeNode.getUserObject();
            String nodeName = userObj != null ? userObj.toString() : "";
            if (tagName.equals(nodeName)) {
                javax.swing.tree.TreePath path = new javax.swing.tree.TreePath(treeNode.getPath());
                tree.setSelectionPath(path);
                tree.scrollPathToVisible(path);
                return true;
            }
            for (int i = 0; i < treeNode.getChildCount(); i++) {
                if (buscarYSeleccionarNodo(tree, treeNode.getChildAt(i), tagName)) return true;
            }
        }
        return false;
    } // --- Fin del metodo/clase buscarYSeleccionarNodo ---


    /**
     * Inicializa el modelo del árbol de tags.
     */
    private void initializeTagTree() {
        final JTree allTagsTree = registry.get("tree.datamode.alltags");
        if (allTagsTree == null) {
            logger.error("No se encontró 'tree.datamode.alltags' en el registro.");
            return;
        }

        // Parte computacional intensiva (puede ejecutarse en hilo de fondo)
        TagDAO dao = dataManager.getTagDAO();
        final TagTreeModel model = new TagTreeModel(dao);
        final TagTreeCellRenderer renderer = new TagTreeCellRenderer(dao);

        // Asignar referencias inmediatamente para evitar re-inicialización
        tagTreeModel = model;
        tagTreeRenderer = renderer;

        // Actualización de Swing en el EDT (setModel, setCellRenderer)
        javax.swing.SwingUtilities.invokeLater(() -> {
            allTagsTree.setModel(model);
            allTagsTree.setCellRenderer(renderer);
        });
    } // --- Fin del metodo/clase initializeTagTree ---

    /**
     * Refresca el árbol de tags y selecciona un tag específico.
     * @param selectTagId ID del tag a seleccionar, o null si no se debe seleccionar uno específico.
     */
    private void refreshTagTreeAndSelect(Long selectTagId) {
        JTree allTagsTree = registry.get("tree.datamode.alltags");
        if (allTagsTree == null || tagTreeModel == null) {
            initializeTagTree();
            return;
        }

        java.util.Set<Long> expandedIds = new java.util.HashSet<>();
        long savedSelectionId = -1;
        for (int row = 0; row < allTagsTree.getRowCount(); row++) {
            javax.swing.tree.TreePath path = allTagsTree.getPathForRow(row);
            if (path != null && allTagsTree.isExpanded(row)) {
                Object last = path.getLastPathComponent();
                if (last instanceof Tag) {
                    expandedIds.add(((Tag) last).getId());
                }
            }
        }
        javax.swing.tree.TreePath selPath = allTagsTree.getSelectionPath();
        if (selPath != null) {
            Object last = selPath.getLastPathComponent();
            if (last instanceof Tag) {
                savedSelectionId = ((Tag) last).getId();
            }
        }

        tagTreeModel.clearCache();
        tagTreeModel.fireTreeStructureChanged();

        if (tagTreeRenderer != null) {
            tagTreeRenderer.setConnectedDiscoIds(dataManager.getConnectedDiscoIds());
            tagTreeRenderer.refreshCache();
        }

        long targetId = selectTagId != null ? selectTagId : savedSelectionId;
        final long finalTargetId = targetId;
        final java.util.Set<Long> finalExpandedIds = expandedIds;
        javax.swing.SwingUtilities.invokeLater(() -> {
            for (int row = 0; row < allTagsTree.getRowCount(); row++) {
                javax.swing.tree.TreePath path = allTagsTree.getPathForRow(row);
                if (path != null) {
                    Object last = path.getLastPathComponent();
                    if (last instanceof Tag && finalExpandedIds.contains(((Tag) last).getId())) {
                        allTagsTree.expandRow(row);
                    }
                }
            }
            if (finalTargetId >= 0) {
                selectTagInTree(finalTargetId);
            }
        });
    } // --- Fin del metodo/clase refreshTagTreeAndSelect ---


    /**
     * Devuelve el ID del tag seleccionado en el árbol.
     * @return ID del tag seleccionado, o -1 si no hay selección válida.
     */
    private long getSelectedTagIdFromTree() {
        JTree tree = registry.get("tree.datamode.alltags");
        if (tree == null) return -1;
        javax.swing.tree.TreePath selPath = tree.getSelectionPath();
        if (selPath == null) return -1;
        Object last = selPath.getLastPathComponent();
        if (last instanceof Tag) return ((Tag) last).getId();
        return -1;
    } // --- Fin del metodo/clase getSelectedTagIdFromTree ---


    /**
     * Selecciona un nodo en el árbol de tags dado un ID de tag.
     * @param tagId ID del tag a seleccionar
     */
    private void selectTagInTree(long tagId) {
        JTree tree = registry.get("tree.datamode.alltags");
        if (tree == null || tagTreeModel == null) {
            logger.debug("selectTagInTree({}): tree={}, model={}", tagId, tree, tagTreeModel);
            return;
        }
        javax.swing.tree.TreeModel model = tree.getModel();
        java.util.ArrayList<Object> path = new java.util.ArrayList<>();
        javax.swing.tree.TreePath result = findNodePathInModel(model, model.getRoot(), tagId, path);
        if (result != null) {
            tree.setSelectionPath(result);
            tree.makeVisible(result);
            tree.scrollPathToVisible(result);
            logger.debug("selectTagInTree({}): seleccionado y expandido: {}", tagId, result);
        } else {
            logger.debug("selectTagInTree({}): NO ENCONTRADO en el modelo", tagId);
        }
    } // --- Fin del metodo/clase selectTagInTree ---


    /**
     * Busca la ruta de un nodo en el modelo de árbol dado un ID de tag.
     * @param model Modelo del árbol
     * @param parent Nodo padre
     * @param tagId ID del tag a buscar
     * @param pathAccum Acumulador de ruta
     * @return Ruta del nodo encontrado, o null si no se encuentra.
     */
    private javax.swing.tree.TreePath findNodePathInModel(javax.swing.tree.TreeModel model, Object parent, long tagId, java.util.ArrayList<Object> pathAccum) {
        pathAccum.add(parent);
        if (parent instanceof Tag && ((Tag) parent).getId() == tagId) {
            logger.debug("findNodePathInModel: ENCONTRADO tagId={} en nodo '{}'", tagId, ((Tag) parent).getNombre());
            return new javax.swing.tree.TreePath(pathAccum.toArray());
        }
        int childCount = model.getChildCount(parent);
        for (int i = 0; i < childCount; i++) {
            Object child = model.getChild(parent, i);
            javax.swing.tree.TreePath found = findNodePathInModel(model, child, tagId, pathAccum);
            if (found != null) return found;
        }
        pathAccum.remove(pathAccum.size() - 1);
        return null;
    } // --- Fin del metodo/clase findNodePathInModel ---

    /**
     * Inicializa la lista plana de tags.
     */
    private void initializeFlatTagList() {
        JList<Tag> flatList = registry.get("list.datamode.alltags.flat");
        if (flatList == null) {
            logger.error("No se encontró 'list.datamode.alltags.flat' en el registro.");
            return;
        }

        flatList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            Tag selectedTag = flatList.getSelectedValue();
            if (selectedTag != null) {
                loadImagesForTagName(selectedTag.getNombre());
            }
        });
    } // --- Fin del metodo/clase initializeFlatTagList ---


    /**
     * Refresca la lista plana de tags.
     */
    private void refreshFlatTagList() {
        flatListOriginalOrder.clear();
        flatListOriginalOrder.addAll(dataManager.getAllTags());

        // Poblar el modelo y re-aplicar filtro si el campo unificado tiene texto
        TagIntelliSenseField unifiedField = registry.get("textfield.datamode.tag.intellisense.create");
        String filterText = (unifiedField != null) ? unifiedField.getText() : "";
        if (filterText == null || filterText.trim().isEmpty()) {
            restoreFlatTagList();
        } else {
            filterFlatTagList(filterText.trim());
        }

        JList<Tag> flatList = registry.get("list.datamode.alltags.flat");
        if (flatList != null) {
            flatList.putClientProperty("flatTagDAO", dataManager.getTagDAO());
            flatList.putClientProperty("flatDiscoIds", dataManager.getConnectedDiscoIds());
        }
        logger.debug("Lista plana de tags actualizada con {} elementos.", flatListOriginalOrder.size());
    } // --- Fin del metodo/clase refreshFlatTagList ---

    /**
     * Notificado desde DataBuilder cuando el usuario cambia entre vista árbol y lista.
     * Preserva la selección actual al cambiar de vista.
     * @param isTree true si se cambió a vista árbol, false si a vista lista.
     */
    public void onTagViewSwitched(boolean isTree) {
        logger.debug("Vista de tags cambiada: {} ", isTree ? "árbol" : "lista");
        this.tagTreeViewActive = isTree;
        if (!isTree) {
            // Preservar selección del árbol antes de refrescar la lista
            Tag currentTreeTag = getSelectedTagFromTree();
            refreshFlatTagList();
            if (currentTreeTag != null) {
                JList<Tag> flatList = registry.get("list.datamode.alltags.flat");
                if (flatList != null) {
                    flatList.setSelectedValue(currentTreeTag, true);
                }
            }
        } else {
            // Preservar selección de la lista antes de restaurar
            JList<Tag> flatList = registry.get("list.datamode.alltags.flat");
            Tag currentListTag = (flatList != null) ? flatList.getSelectedValue() : null;
            restoreFlatTagList();
            if (currentListTag != null) {
                selectTagInTree(currentListTag.getId());
            }
        }
    } // --- Fin del metodo/clase onTagViewSwitched ---


    /**
     * Ordena la lista plana de tags. Llamado desde el callback del botón de ordenar.
     * @param estado 0=ASC, 1=DESC, 2=OFF (sin orden)
     */
    public void ordenarListaPlana(int estado) {
        JList<Tag> flatList = registry.get("list.datamode.alltags.flat");
        if (flatList == null) return;
        DefaultListModel<Tag> model = (DefaultListModel<Tag>) flatList.getModel();
        if (model.getSize() == 0) return;

        List<Tag> items = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++) items.add(model.get(i));

        switch (estado) {
            case 0: // ASC
                items.sort((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()));
                break;
            case 1: // DESC
                items.sort((a, b) -> b.getNombre().compareToIgnoreCase(a.getNombre()));
                break;
            default: // OFF - restaurar orden original
                if (!flatListOriginalOrder.isEmpty()) {
                    items = new ArrayList<>(flatListOriginalOrder);
                }
                break;
        }

        model.clear();
        for (Tag t : items) model.addElement(t);
    } // --- Fin del metodo/clase ordenarListaPlana ---

    /**
     * Inicializa la lista de unidades de disco.
     */
    private void initializeDriveList() {
        DriveListPanel drivePanel = registry.get("panel.datamode.drives");
        if (drivePanel == null) {
            logger.error("No se encontró 'panel.datamode.drives' en el registro.");
            return;
        }

        drivePanel.setOnRefresh(this::refreshDriveList);
        refreshDriveList();
    } // --- Fin del metodo/clase initializeDriveList ---


    /**
     * Refresca la lista de unidades de disco.
     */
    private void refreshDriveList() {
        DriveListPanel drivePanel = registry.get("panel.datamode.drives");
        if (drivePanel == null) return;

        dataManager.invalidateConnectedDisksCache();
        dataManager.ensureAllDrivesRegistered();
        List<Disco> registered = dataManager.getAllRegisteredDisks();
        Map<String, Path> connected = dataManager.getConnectedDisks();

        SwingUtilities.invokeLater(() -> {
            drivePanel.updateDrives(registered, connected);
        });
    } // --- Fin del metodo/clase refreshDriveList ---

    /**
     * Carga la imagen seleccionada en el visor.
     */
    private void cargarImagenEnVisor() {
        String selectedKey = model.getSelectedImageKey();
        if (selectedKey == null || visorController == null) return;

        Path ruta = model.getRutaCompleta(selectedKey);
        if (ruta == null || !Files.exists(ruta)) return;

        VisorModel.DisplayMode currentMode = model.getCurrentDisplayMode();

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                return ImageIO.read(ruta.toFile());
            }
            @Override
            protected void done() {
                try {
                    model.setCurrentImage(get());

                    if (currentMode == VisorModel.DisplayMode.POLAROID) {
                        PolaroidDisplayPanel polaroidPanel = registry.get("panel.datamode.display.polaroid");
                        if (polaroidPanel != null) {
                            polaroidPanel.actualizarInformacionDesdeModelo();
                            polaroidPanel.getImagePanel().repaint();
                        }
                    } else {
                        ImageDisplayPanel singlePanel = registry.get("panel.datamode.display");
                        if (singlePanel != null) singlePanel.repaint();
                    }

                    if (visorController.getZoomManager() != null) {
                        visorController.getZoomManager().aplicarModoDeZoom(model.getCurrentZoomMode());
                    }

                    if (visorController.getInfobarImageManager() != null) {
                        visorController.getInfobarImageManager().actualizar();
                    }
                } catch (Exception ex) {
                    logger.error("Error cargando imagen en modo datos", ex);
                }
            }
        }.execute();
    } // --- Fin del metodo/clase cargarImagenEnVisor ---


    public void toggleMarcaImagenesSeleccionadas() {
        if (projectManager == null || registry == null) return;

        JList<String> fileNameList = registry.get("list.datamode.filenames");
        if (fileNameList == null) return;

        List<String> selectedKeys = fileNameList.getSelectedValuesList();
        if (selectedKeys == null || selectedKeys.isEmpty()) return;

        List<Path> paths = new ArrayList<>();
        for (String key : selectedKeys) {
            if (key == null || key.isEmpty()) continue;
            Path p = model.getRutaCompleta(key);
            if (p != null) paths.add(p);
        }
        if (paths.isEmpty()) return;

        boolean todasMarcadas = paths.stream().allMatch(p -> projectManager.estaMarcada(p));

        for (Path path : paths) {
            if (todasMarcadas) {
                projectManager.desmarcarImagen(path);
            } else {
                projectManager.marcarImagen(path);
            }
        }
        projectManager.notificarModificacion();

        String msg = paths.size() + " imagen(es) " +
            (todasMarcadas ? "quitada(s) del" : "a\u00f1adida(s) al") + " proyecto";
        logger.info(msg);
        if (statusBarManager != null) {
            statusBarManager.mostrarMensajeTemporal(msg, 3000);
        }

        actualizarEstadoVisualMarcado();
    } // --- Fin del metodo/clase toggleMarcaImagenesSeleccionadas ---


    public void actualizarEstadoVisualMarcado() {
        if (registry == null || projectManager == null) return;

        String currentKey = model.getSelectedImageKey();
        boolean estaMarcada = false;
        if (currentKey != null) {
            Path ruta = model.getRutaCompleta(currentKey);
            if (ruta != null) {
                estaMarcada = projectManager.estaMarcada(ruta);
            }
        }

        Object singlePanel = registry.get("panel.datamode.display");
        if (singlePanel instanceof ImageDisplayPanel) {
            ((ImageDisplayPanel) singlePanel).setImagenMarcada(estaMarcada);
        }

        Object polaroidImage = registry.get("panel.datamode.display.polaroid.image");
        if (polaroidImage instanceof ImageDisplayPanel) {
            ((ImageDisplayPanel) polaroidImage).setImagenMarcada(estaMarcada);
        }

        Object gridList = registry.get("list.datamode.grid");
        if (gridList instanceof JList) {
            ((JList<?>) gridList).repaint();
        }

        JToggleButton markBtn = registry.get("toggle.datamode.mark");
        if (markBtn != null) {
            markBtn.setSelected(estaMarcada);
        }
    } // --- Fin del metodo/clase actualizarEstadoVisualMarcado ---


    private void initializeMarkButton() {
        JToggleButton markBtn = registry.get("toggle.datamode.mark");
        if (markBtn == null) return;

        markBtn.addActionListener(e -> toggleMarcaImagenesSeleccionadas());

        // Al cambiar de imagen, actualizar estado del botón y marcos visuales
        JList<String> fileNameList = registry.get("list.datamode.filenames");
        if (fileNameList != null) {
            fileNameList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    actualizarEstadoVisualMarcado();
                }
            });
        }

        // Sincronizar estado inicial
        actualizarEstadoVisualMarcado();
    } // --- Fin del metodo/clase initializeMarkButton ---


    /**
     * Inicializa la lista de nombres de archivo.
     */
    private void initializeFileNameList() {
        JList<String> fileNameList = registry.get("list.datamode.filenames");
        if (fileNameList == null) {
            logger.error("No se encontró 'list.datamode.filenames' en el registro.");
            return;
        }

        fileNameList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !isSyncingLists) {
                isSyncingLists = true;
                try {
                    String selectedKey = fileNameList.getSelectedValue();
                    boolean hasSelection = !fileNameList.isSelectionEmpty();
                    JButton btnAssign = registry.get("btn.datamode.tag.assign");
                    if (btnAssign != null) {
                        btnAssign.setEnabled(hasSelection);
                    }
                    if (selectedKey != null) {
                        model.setSelectedImageKey(selectedKey);
                        cargarImagenEnVisor();
                        updateTagPanelSelection();
                        JList<String> gridList = registry.get("list.datamode.grid");
                        if (gridList != null) {
                            gridList.setSelectedIndices(fileNameList.getSelectedIndices());
                        }
                        if (visorController != null) {
                            controlador.managers.DisplayModeManager dmm = visorController.getDisplayModeManager();
                            if (dmm != null) {
                                dmm.sincronizarEstadoBotonesDisplayMode();
                            }
                        }
                    }
                } finally {
                    isSyncingLists = false;
                }
            }
        });

        // Popup contextual sobre la lista de nombres de archivo
        fileNameList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showFileNameListPopup(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showFileNameListPopup(e);
            }
        });
    } // --- Fin del metodo/clase initializeFileNameList ---


    private void showFileNameListPopup(MouseEvent e) {
        JList<String> fileNameList = registry.get("list.datamode.filenames");
        if (fileNameList == null || projectManager == null) return;

        int index = fileNameList.locationToIndex(e.getPoint());
        if (index == -1) return;

        if (!fileNameList.isSelectedIndex(index)) {
            fileNameList.setSelectedIndex(index);
        }

        List<Path> selectedPaths = new ArrayList<>();
        for (String key : fileNameList.getSelectedValuesList()) {
            if (key == null) continue;
            Path p = model.getRutaCompleta(key);
            if (p != null) selectedPaths.add(p);
        }
        if (selectedPaths.isEmpty()) return;

        boolean todasMarcadas = selectedPaths.stream().allMatch(p -> projectManager.estaMarcada(p));

        JPopupMenu popup = new JPopupMenu();
        String menuText = todasMarcadas ? "Quitar del Proyecto" : "A\u00f1adir al Proyecto";
        JMenuItem menuItem = new JMenuItem(menuText);
        menuItem.addActionListener(ae -> {
            for (Path path : selectedPaths) {
                if (todasMarcadas) {
                    projectManager.desmarcarImagen(path);
                } else {
                    projectManager.marcarImagen(path);
                }
            }
            projectManager.notificarModificacion();

            String msg = selectedPaths.size() + " imagen(es) " +
                (todasMarcadas ? "quitada(s) del" : "a\u00f1adida(s) al") + " proyecto";
            logger.info(msg);
            if (statusBarManager != null) {
                statusBarManager.mostrarMensajeTemporal(msg, 3000);
            }

            actualizarEstadoVisualMarcado();
        });
        popup.add(menuItem);

        popup.show(fileNameList, e.getX(), e.getY());
    } // --- Fin del metodo/clase showFileNameListPopup ---

    /**
     * Inicializa el filtro Tornado para la columna central (lista de archivos).
     */
    private void initializeTornadoFilter() {
        JTextField tornadoField = registry.get("textfield.datamode.tornado");
        JToggleButton btnTornado = registry.get("toggle.datamode.tornado");
        if (tornadoField == null || btnTornado == null) return;

        new TornadoFilterController(tornadoField, btnTornado,
            null, // no popup
            () -> applyTornadoFilter(tornadoField.getText()),
            () -> findNextTornadoMatch(tornadoField.getText()),
            () -> refreshVisibleFileList(),
            300
        );
    } // --- Fin del metodo/clase initializeTornadoFilter ---


    /**
     * Inicializa el filtro Tornado para la columna izquierda (tags/ramas).
     * Comportamiento:
     * - Vista árbol con filtro ON: navega al primer tag que coincida (expande+selecciona)
     * - Vista lista con filtro ON: filtra la lista plana mostrando solo coincidencias
     * - Enter con filtro OFF (y popup oculto): busca la siguiente coincidencia
     */
    private void initializeLeftTagTornadoFilter() {
        TagIntelliSenseField createField = registry.get("textfield.datamode.tag.intellisense.create");
        JToggleButton btnTornado = registry.get("toggle.datamode.tag.tornado");
        if (createField == null || btnTornado == null) {
            logger.debug("initializeLeftTagTornadoFilter: componentes no encontrados");
            return;
        }

        createField.setOnEnterNoPopupAction(() -> {
            if (!btnTornado.isSelected() && !createField.getText().isEmpty()) {
                findNextTagMatch(createField.getText());
            }
        });

        // Guardar estado de expansión del árbol al activar el toggle
        btnTornado.addActionListener(e -> {
            if (btnTornado.isSelected() && tagTreeViewActive) {
                saveTreeExpansionState();
            }
        });

        new TornadoFilterController(createField, btnTornado,
            createField::isPopupVisible,
            () -> { // live filter callback (toggle ON)
                String text = createField.getText();
                if (tagTreeViewActive) {
                    if (text.isEmpty()) {
                        // Sin texto: restaurar vista árbol
                        if (tornadoForcedListView) {
                            tornadoForcedListView = false;
                            switchToTreeCard();
                            restoreTreeExpansionState();
                        }
                    } else {
                        // Con texto: cambiar a vista lista y filtrar
                        if (!tornadoForcedListView) {
                            tornadoForcedListView = true;
                            saveTreeExpansionState();
                            switchToListCard();
                        }
                        filterFlatTagList(text);
                    }
                } else {
                    if (text.isEmpty()) {
                        restoreFlatTagList();
                    } else {
                        filterFlatTagList(text);
                    }
                }
            },
            () -> {}, // ENTER ya se maneja via setOnEnterNoPopupAction
            () -> { // restore callback (toggle OFF)
                if (tagTreeViewActive) {
                    if (tornadoForcedListView) {
                        tornadoForcedListView = false;
                        switchToTreeCard();
                        restoreTreeExpansionState();
                    }
                } else {
                    restoreFlatTagList();
                }
            },
            300
        );
    } // --- Fin del metodo/clase initializeLeftTagTornadoFilter ---


    /**
     * Inicializa el filtro Tornado para la columna derecha (tags asignados a imágenes).
     */
    private void initializeAssignedTagTornadoFilter() {
        TagIntelliSenseField assignField = registry.get("textfield.datamode.tag.intellisense");
        JToggleButton btnTornado = registry.get("toggle.datamode.tag.assigned.tornado");
        TagManagementPanel tagPanel = registry.get("panel.datamode.tagmanagement");
        if (assignField == null || btnTornado == null || tagPanel == null) {
            logger.debug("initializeAssignedTagTornadoFilter: componentes no encontrados");
            return;
        }

        assignField.setOnEnterNoPopupAction(() -> {
            if (!btnTornado.isSelected() && !assignField.getText().isEmpty()) {
                boolean found = tagPanel.findNext(assignField.getText());
                if (!found) {
                    statusBarManager.mostrarMensajeTemporal(
                        "No se encontró: \"" + assignField.getText() + "\"", 3000);
                }
            }
        });

        new TornadoFilterController(assignField, btnTornado,
            assignField::isPopupVisible,
            () -> { // live filter
                String text = assignField.getText();
                if (text.isEmpty()) return;
                tagPanel.filter(text);
            },
            () -> {}, // ENTER ya se maneja via setOnEnterNoPopupAction
            () -> { // restore
                tagPanel.clearFilter();
            },
            300
        );
    }


    /**
     * Aplica el filtro Tornado.
     * @param text Texto del filtro
     */
    private void applyTornadoFilter(String text) {
        JList<String> fileNameList = registry.get("list.datamode.filenames");
        if (fileNameList == null) return;
        String lowerText = text.toLowerCase().trim();
        javax.swing.DefaultListModel<String> masterModel = getMasterFileListModel();
        if (masterModel == null) return;
        String selectedValue = fileNameList.getSelectedValue();

        javax.swing.DefaultListModel<String> filteredModel = new javax.swing.DefaultListModel<>();
        for (int i = 0; i < masterModel.getSize(); i++) {
            String item = masterModel.getElementAt(i);
            if (item.toLowerCase().contains(lowerText)) {
                filteredModel.addElement(item);
            }
        }
        fileNameList.setModel(filteredModel);

        // Restaurar selección si aún existe en el modelo filtrado
        if (selectedValue != null && filteredModel.contains(selectedValue)) {
            fileNameList.setSelectedValue(selectedValue, true);
        }
    } // --- Fin del metodo/clase applyTornadoFilter ---


    /**
     * Busca la siguiente coincidencia con el filtro Tornado.
     * @param text Texto del filtro
     */
    private void findNextTornadoMatch(String text) {
        JList<String> fileNameList = registry.get("list.datamode.filenames");
        if (fileNameList == null || text.isEmpty()) return;
        String lowerText = text.toLowerCase().trim();
        javax.swing.ListModel<String> model = fileNameList.getModel();
        int startIndex = fileNameList.getSelectedIndex();
        if (startIndex < 0) startIndex = 0;

        for (int i = startIndex + 1; i < model.getSize(); i++) {
            if (model.getElementAt(i).toLowerCase().contains(lowerText)) {
                fileNameList.setSelectedIndex(i);
                fileNameList.ensureIndexIsVisible(i);
                return;
            }
        }
        // Wrap around
        for (int i = 0; i <= startIndex; i++) {
            if (model.getElementAt(i).toLowerCase().contains(lowerText)) {
                fileNameList.setSelectedIndex(i);
                fileNameList.ensureIndexIsVisible(i);
                return;
            }
        }
    } // --- Fin del metodo/clase findNextTornadoMatch ---

    /**
     * Busca la siguiente coincidencia en la lista plana de tags (o árbol) y la selecciona.
     * @param text Texto a buscar
     */
    private void findNextTagMatch(String text) {
        if (text == null || text.trim().isEmpty()) return;
        String lower = text.toLowerCase().trim();
        int startIdx = findCurrentTagIndexInFlatList();

        for (int i = startIdx + 1; i < flatListOriginalOrder.size(); i++) {
            if (flatListOriginalOrder.get(i).getNombre().toLowerCase().contains(lower)) {
                selectTagMatch(i);
                return;
            }
        }
        // Wrap around
        for (int i = 0; i <= startIdx; i++) {
            if (flatListOriginalOrder.get(i).getNombre().toLowerCase().contains(lower)) {
                selectTagMatch(i);
                return;
            }
        }
        statusBarManager.mostrarMensajeTemporal("No se encontr\u00f3: \"" + text + "\"", 3000);
    } // --- Fin del metodo/clase findNextTagMatch ---

    /**
     * Encuentra el índice del tag actualmente seleccionado en flatListOriginalOrder.
     */
    private int findCurrentTagIndexInFlatList() {
        if (tagTreeViewActive) {
            Tag currentTreeTag = getSelectedTagFromTree();
            if (currentTreeTag != null) {
                for (int i = 0; i < flatListOriginalOrder.size(); i++) {
                    if (flatListOriginalOrder.get(i).getId() == currentTreeTag.getId()) {
                        return i;
                    }
                }
            }
        } else {
            JList<Tag> flatList = registry.get("list.datamode.alltags.flat");
            if (flatList != null) {
                Tag sel = flatList.getSelectedValue();
                if (sel != null) {
                    for (int i = 0; i < flatListOriginalOrder.size(); i++) {
                        if (flatListOriginalOrder.get(i).getId() == sel.getId()) {
                            return i;
                        }
                    }
                }
            }
        }
        return -1;
    } // --- Fin del metodo/clase findCurrentTagIndexInFlatList ---

    /**
     * Selecciona el tag en la vista activa (árbol o lista) según el índice en flatListOriginalOrder.
     */
    private void selectTagMatch(int flatListIndex) {
        if (flatListIndex < 0 || flatListIndex >= flatListOriginalOrder.size()) return;
        Tag tag = flatListOriginalOrder.get(flatListIndex);
        JList<Tag> flatList = registry.get("list.datamode.alltags.flat");

        if (tagTreeViewActive) {
            selectTagInTree(tag.getId());
            // También seleccionar en la lista plana en segundo plano
            if (flatList != null) {
                flatList.setSelectedValue(tag, true);
            }
        } else {
            if (flatList != null) {
                flatList.setSelectedValue(tag, true);
                flatList.ensureIndexIsVisible(flatList.getSelectedIndex());
            }
        }
    } // --- Fin del metodo/clase selectTagMatch ---


    /**
     * Obtiene el modelo de la lista maestra de archivos.
     * @return El modelo de la lista maestra
     */
    private javax.swing.DefaultListModel<String> getMasterFileListModel() {
        return masterFileListModel;
    } // --- Fin del metodo/clase getMasterFileListModel ---


    private javax.swing.DefaultListModel<String> masterFileListModel = new javax.swing.DefaultListModel<>();


    /**
     * Refresca la lista de archivos visible.
     */
    private void refreshVisibleFileList() {
        JList<String> fileNameList = registry.get("list.datamode.filenames");
        if (fileNameList == null) return;
        fileNameList.setModel(masterFileListModel);
    } // --- Fin del metodo/clase refreshVisibleFileList ---

    /**
     * Configura los listeners para la lista de archivos, el grid y el menú contextual.
     */
    /**
     * Configura los listeners para la lista de archivos, el grid y el menú contextual.
     */
    private void setupListeners() {
        JTree allTagsTree = registry.get("tree.datamode.alltags");
        allTagsTree.addTreeSelectionListener(e -> {
            TreePath selectedPath = e.getNewLeadSelectionPath();
            if (selectedPath == null) {
                return;
            }
            
            Object selectedNode = selectedPath.getLastPathComponent();
            
            // Solo cargamos imágenes si el nodo seleccionado es un Tag o es la raíz "Biblioteca"
            if (selectedNode instanceof Tag) {
                Tag selectedTag = (Tag) selectedNode;
                loadImagesForTag(selectedTag);
            } else if ("Biblioteca".equals(selectedNode)) {
                loadAllImages();
            }
        });
        
        // Listener para el grid de imágenes
        JList<String> gridList = registry.get("list.datamode.grid");
        gridList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        gridList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !isSyncingLists) {
                isSyncingLists = true;
                try {
                    String selectedKey = gridList.getSelectedValue();
                    model.setSelectedImageKey(selectedKey);
                    cargarImagenEnVisor();
                    if (visorController != null && visorController.getListCoordinator() != null) {
                        visorController.getListCoordinator().forzarActualizacionEstadoAcciones();
                    }
                    updateTagPanelSelection();
                    JList<String> fileNameList = registry.get("list.datamode.filenames");
                    if (fileNameList != null) {
                        fileNameList.setSelectedIndices(gridList.getSelectedIndices());
                        int selectedIndex = gridList.getSelectedIndex();
                        if (selectedIndex >= 0) {
                            gridList.ensureIndexIsVisible(selectedIndex);
                            fileNameList.ensureIndexIsVisible(selectedIndex);
                        }
                    }
                    if (visorController != null) {
                        controlador.managers.DisplayModeManager dmm = visorController.getDisplayModeManager();
                        if (dmm != null) {
                            dmm.sincronizarEstadoBotonesDisplayMode();
                        }
                    }
                } finally {
                    isSyncingLists = false;
                }
            }
        });
        
        // --- Menú contextual (clic derecho) para marcar imágenes al proyecto ---
        gridList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showGridContextMenu(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showGridContextMenu(e);
            }
        });
    } // --- Fin del metodo/clase setupListeners ---


    /**
     * Muestra un menú contextual en el grid del modo datos con la opción
     * de añadir/quitar la imagen del proyecto.
     * @param e Evento de ratón
     */
    private void showGridContextMenu(MouseEvent e) {
        JList<String> gridList = registry.get("list.datamode.grid");
        if (gridList == null || projectManager == null) return;
        
        int index = gridList.locationToIndex(e.getPoint());
        if (index == -1) return;
        
        // Seleccionar el elemento bajo el ratón si no está ya seleccionado
        if (!gridList.isSelectedIndex(index)) {
            gridList.setSelectedIndex(index);
        }
        
        List<Path> selectedPaths = getSelectedImagePaths();
        if (selectedPaths.isEmpty()) return;
        
        JPopupMenu popup = new JPopupMenu();
        
        // Determinar estado: si TODAS las seleccionadas están marcadas, mostrar "Quitar"
        boolean todasMarcadas = selectedPaths.stream().allMatch(p -> projectManager.estaMarcada(p));
        
        String menuText = todasMarcadas ? "Quitar del Proyecto" : "Añadir al Proyecto";
        JMenuItem menuItem = new JMenuItem(menuText);
        menuItem.addActionListener(actionEvent -> {
            for (Path path : selectedPaths) {
                if (todasMarcadas) {
                    projectManager.desmarcarImagen(path);
                } else {
                    projectManager.marcarImagen(path);
                }
            }
            projectManager.notificarModificacion();
            
            // Mostrar feedback en la barra de estado
            String mensaje = selectedPaths.size() + " imagen(es) " + 
                (todasMarcadas ? "quitada(s) del" : "añadida(s) al") + " proyecto";
            logger.info(mensaje);
            if (statusBarManager != null) {
                statusBarManager.mostrarMensajeTemporal(mensaje, 3000);
            }
        });
        popup.add(menuItem);
        
        popup.addSeparator();

        // 1. Opción "Localizar Archivo"
        if (visorController != null && visorController.getActionMap() != null) {
            javax.swing.Action localizarAction = visorController.getActionMap().get(controlador.commands.AppActionCommands.CMD_IMAGEN_LOCALIZAR);
            if (localizarAction != null) {
                popup.add(new JMenuItem(localizarAction));
            }
        }

        // 2. Opción "Añadir Etiqueta..."
        JMenuItem addTagItem = new JMenuItem("Añadir Etiqueta...");
        addTagItem.addActionListener(actionEvent -> {
            JPanel panel = new JPanel(new java.awt.BorderLayout(5, 5));
            panel.add(new javax.swing.JLabel("Escribe el nombre de la nueva etiqueta para asignar a las imágenes seleccionadas:"), java.awt.BorderLayout.NORTH);
            
            JPanel inputPanel = new JPanel(new java.awt.BorderLayout(5, 0));
            TagIntelliSenseField txtTag = new TagIntelliSenseField();
            txtTag.setColumns(20);
            txtTag.refreshTags(dataManager.getAllTags());
            JButton btnBrowseLocal = new JButton("...");
            btnBrowseLocal.setToolTipText("Buscar etiqueta existente en la biblioteca...");
            
            inputPanel.add(txtTag, java.awt.BorderLayout.CENTER);
            inputPanel.add(btnBrowseLocal, java.awt.BorderLayout.EAST);
            panel.add(inputPanel, java.awt.BorderLayout.CENTER);
            
            btnBrowseLocal.addActionListener(browseEvent -> {
                java.awt.Frame mainFrame = (visorController != null) ? visorController.getView() : null;
                vista.util.IconUtils iconUtils = (visorController != null) ? visorController.getIconUtils() : null;
                vista.dialogos.TagSelectionDialog selectionDialog = new vista.dialogos.TagSelectionDialog(mainFrame, iconUtils);
                String selected = selectionDialog.showDialog();
                if (selected != null) {
                    txtTag.setText(selected);
                }
            });
            
            int option = JOptionPane.showConfirmDialog(
                gridList.getTopLevelAncestor(),
                panel,
                "Añadir Etiqueta a Mano",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            );
            
            if (option == JOptionPane.OK_OPTION) {
                String tagName = txtTag.getText();
                if (tagName != null && !tagName.trim().isEmpty()) {
                    String cleanTagName = tagName.trim();
                    dataManager.addTagToImages(selectedPaths, cleanTagName);
                    updateTagPanelSelection(); // Refrescar vista de tags de la imagen
                    refreshTagTreeAndSelect(null); // Refrescar el árbol de tags
                    refreshAvailableTags(); // Refrescar el combo
                    
                    String mensaje = "Etiqueta '" + cleanTagName + "' añadida a " + selectedPaths.size() + " imagen(es)";
                    logger.info(mensaje);
                    if (statusBarManager != null) {
                        statusBarManager.mostrarMensajeTemporal(mensaje, 3000);
                    }
                }
            }
        });
        popup.add(addTagItem);
        
        // 3. Opción "Vincular Archivo 3D..."
        JMenuItem linkFileItem = new JMenuItem("Vincular Archivo 3D...");
        linkFileItem.addActionListener(actionEvent -> {
            java.awt.Frame mainFrame = (visorController != null) ? visorController.getView() : null;
            vista.dialogos.FileAssociationDialog dialog = new vista.dialogos.FileAssociationDialog(mainFrame);
            dialog.setVisible(true);
            String selectedPath = dialog.getSelectedPath();
            if (selectedPath != null && !selectedPath.isEmpty()) {
                int count = 0;
                for (Path p : selectedPaths) {
                    java.util.Optional<modelo.datos.ImagenInfo> imgOpt = dataManager.getImagenDAO().findImagenByPath(p);
                    if (imgOpt.isPresent()) {
                        boolean ok = dataManager.getImagenDAO().assignResourceToImage(imgOpt.get().getId(), selectedPath);
                        if (ok) count++;
                    }
                }
                String mensaje = "Archivo vinculado a " + count + " imagen(es).";
                logger.info(mensaje);
                if (statusBarManager != null) {
                    statusBarManager.mostrarMensajeTemporal(mensaje, 3000);
                }
            }
        });
        popup.add(linkFileItem);
        
        popup.show(gridList, e.getX(), e.getY());
    } // --- Fin del metodo/clase showGridContextMenu ---

    /**
     * Configura los callbacks para la gestión de tags.
     */
    private void setupTagManagementCallbacks() {
        TagManagementPanel tagPanel = registry.get("panel.datamode.tagmanagement");
        if (tagPanel == null) return;

    } // --- Fin del metodo/clase setupTagManagementCallbacks ---


    /**
     * Obtiene el Tag actualmente seleccionado en el árbol, o null si no hay selección o no es un Tag.
     * @return El tag seleccionado, o null.
     */
    private Tag getSelectedTagFromTree() {
        JTree allTagsTree = registry.get("tree.datamode.alltags");
        if (allTagsTree == null) return null;
        TreePath selPath = allTagsTree.getSelectionPath();
        if (selPath == null) return null;
        Object node = selPath.getLastPathComponent();
        return (node instanceof Tag) ? (Tag) node : null;
    } // --- Fin del metodo/clase getSelectedTagFromTree ---


    /**
     * Recarga los tags disponibles en el campo IntelliSense.
     */
    private void refreshIntelliSense() {
        List<Tag> allTags = dataManager.getAllTags();
        TagIntelliSenseField createField = registry.get("textfield.datamode.tag.intellisense.create");
        if (createField != null) {
            createField.refreshTags(allTags);
        }
        TagIntelliSenseField assignField = registry.get("textfield.datamode.tag.intellisense");
        if (assignField != null) {
            assignField.refreshTags(allTags);
        }
        logger.debug("IntelliSense actualizado con {} tags.", allTags.size());
    } // --- Fin del metodo/clase refreshIntelliSense ---


    /**
     * Resuelve una ruta en notación punto manejando ambigüedad.
     * Si un segmento no se encuentra bajo su padre esperado pero existe
     * en múltiples ramas, muestra un diálogo para que el usuario elija.
     * @param input Entrada en notación punto
     * @param parent Componente padre para el diálogo
     * @return Lista de tags resueltos
     */
    private List<Tag> resolveWithAmbiguityDialog(String input, Component parent) {
        // Primero intentar la resolución normal (que ya maneja 1 match global automáticamente)
        List<Tag> result = dataManager.resolveDotNotation(input);
        if (!result.isEmpty()) return result;

        // Si falló, buscar el segmento ambiguo segmento por segmento
        String[] segments = input.split("\\.");
        Long parentId = null;

        for (int i = 0; i < segments.length; i++) {
            String trimmed = segments[i].trim();
            if (trimmed.isEmpty()) continue;

            java.util.Optional<Tag> existing = dataManager.getTagDAO().findTagByNameAndParent(trimmed, parentId);
            if (existing.isEmpty()) {
                List<Tag> globalMatches = dataManager.getTagDAO().findTagsByNameAll(trimmed);
                if (globalMatches.size() > 1) {
                    // Mostrar diálogo con las rutas completas
                    String[] options = globalMatches.stream()
                        .map(t -> dataManager.getTagDAO().getTagFullPath(t.getId()))
                        .toArray(String[]::new);
                    String selection = (String) JOptionPane.showInputDialog(parent,
                        "El tag '" + trimmed + "' existe en varias ramas.\nSelecciona cuál usar:",
                        "Ambigüedad", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                    if (selection == null) return Collections.emptyList();

                    // Reconstruir la ruta completa con la selección + segmentos restantes
                    for (Tag t : globalMatches) {
                        if (dataManager.getTagDAO().getTagFullPath(t.getId()).equals(selection)) {
                            String basePath = selection.replace(" > ", ".");
                            StringBuilder remainingPath = new StringBuilder();
                            for (int j = i; j < segments.length; j++) {
                                String s = segments[j].trim();
                                if (!s.isEmpty()) {
                                    if (remainingPath.length() > 0) remainingPath.append(".");
                                    remainingPath.append(s);
                                }
                            }
                            String fullPath = basePath + remainingPath.toString().substring(trimmed.length());
                            return dataManager.resolveDotNotation(fullPath);
                        }
                    }
                }
                break;
            }
            parentId = existing.get().getId();
        }
        return dataManager.resolveDotNotation(input);
    } // --- Fin del metodo/clase resolveWithAmbiguityDialog ---

    /**
     * Configura los listeners para los botones CRUD de tags (Crear, Editar, Borrar en el panel izquierdo).
     * El campo IntelliSense unificado controla qué botones se habilitan según el texto escrito:
     * - Si el texto coincide con un tag existente → [E] y [-] habilitados (Create deshabilitado)
     * - Si el texto NO coincide con ningún tag → [+] habilitado (Edit/Delete deshabilitados)
     * - Texto vacío → todos habilitados (comportamiento clásico con diálogos)
     */
    private void setupTagCRUDButtons() {
        JButton btnCreate = registry.get("btn.datamode.tag.create");
        JButton btnEdit   = registry.get("btn.datamode.tag.edit");
        JButton btnDelete = registry.get("btn.datamode.tag.delete");
        TagIntelliSenseField intellSenseField = registry.get("textfield.datamode.tag.intellisense.create");

        // ── DocumentListener del campo unificado ──────────────────────────────
        if (intellSenseField != null) {
            intellSenseField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onTextChanged(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onTextChanged(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onTextChanged(); }
                private void onTextChanged() {
                    updateTagCRUDButtonsByText(intellSenseField.getText());
                }
            });
            updateTagCRUDButtonsByText(intellSenseField.getText());
        }

        // ── CREAR TAG ────────────────────────────────────────────────────────
        if (btnCreate != null) {
            btnCreate.addActionListener(e -> {
                String input = (intellSenseField != null) ? intellSenseField.getText().trim() : "";
                if (input.isEmpty()) {
                    JPanel crearPanel = new JPanel(new java.awt.BorderLayout(5, 5));
                    crearPanel.add(new javax.swing.JLabel(
                        "<html>Nombre del tag (usa <b>.</b> para explorar la jerarquía):</html>"),
                        java.awt.BorderLayout.NORTH);
                    TagIntelliSenseField crearField = new TagIntelliSenseField();
                    crearField.setColumns(25);
                    crearField.refreshTags(dataManager.getAllTags());
                    crearPanel.add(crearField, java.awt.BorderLayout.CENTER);
                    int opt = JOptionPane.showConfirmDialog(btnCreate.getTopLevelAncestor(),
                        crearPanel, "Crear Tag", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);
                    if (opt != JOptionPane.OK_OPTION) return;
                    input = crearField.getText().trim();
                    if (input.isEmpty()) return;
                } else if (findTagByText(input) != null) {
                    JOptionPane.showMessageDialog(btnCreate.getTopLevelAncestor(),
                        "El tag '" + input + "' ya existe.\nUsa [E] para editar o [-] para borrar.",
                        "Tag existente", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                List<Tag> resolved = dataManager.createByDotNotation(input);
                long createdTagId = -1;
                if (!resolved.isEmpty()) {
                    Tag leaf = resolved.get(resolved.size() - 1);
                    createdTagId = leaf.getId();
                    String msg = resolved.size() > 1
                        ? "Jerarquía '" + input + "' creada (" + resolved.size() + " nivel(es))."
                        : "Tag '" + leaf.getNombre() + "' creado/encontrado.";
                    statusBarManager.mostrarMensajeTemporal(msg, 3000);
                } else {
                    statusBarManager.mostrarMensajeTemporal("Error al crear tag.", 3000);
                }

                if (intellSenseField != null) intellSenseField.setText("");
                afterTagStructureChanged(createdTagId >= 0 ? createdTagId : null);
            });
        }

        // ── EDITAR TAG (Renombrar o Mover) ────────────────────────────────────
        if (btnEdit != null) {
            btnEdit.addActionListener(e -> {
                Tag tag = resolveTagFromInputOrTree(intellSenseField);
                if (tag == null) {
                    JOptionPane.showMessageDialog(btnEdit.getTopLevelAncestor(),
                        "Escribe el nombre del tag en el campo de búsqueda o selecciónalo en el árbol.",
                        "Editar Tag", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (tag.isReadOnly()) {
                    JOptionPane.showMessageDialog(btnEdit.getTopLevelAncestor(),
                        "El tag '" + tag.getNombre() + "' es del sistema y no se puede modificar.",
                        "Editar Tag", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                showEditTagDialog(tag, btnEdit);
            });
        }

        // ── BORRAR TAG ────────────────────────────────────────────────────────
        if (btnDelete != null) {
            btnDelete.addActionListener(e -> {
                Tag tag = resolveTagFromInputOrTree(intellSenseField);
                if (tag == null) {
                    JOptionPane.showMessageDialog(btnDelete.getTopLevelAncestor(),
                        "Escribe el nombre del tag en el campo de búsqueda o selecciónalo en el árbol.",
                        "Borrar Tag", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (tag.isReadOnly()) {
                    JOptionPane.showMessageDialog(btnDelete.getTopLevelAncestor(),
                        "El tag '" + tag.getNombre() + "' es del sistema y no se puede borrar.",
                        "Borrar Tag", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int childCount = dataManager.getTagDAO().getDirectChildCount(tag.getId());
                int imageCount = dataManager.getImageCountForTagRecursive(tag);

                if (childCount > 0) {
                    String mensaje = "<html>El tag '<b>" + tag.getNombre() + "</b>' tiene "
                        + childCount + " hijo(s) directo(s).";
                    if (imageCount > 0) {
                        mensaje += "<br>En total, " + imageCount + " imagen(es) están en esta jerarquía.";
                    }
                    mensaje += "<br><br>¿Qué deseas hacer?</html>";

                    Object[] borrarOpciones = {"Borrar solo este tag", "Borrar toda la rama", "Cancelar"};
                    int borrarChoice = JOptionPane.showOptionDialog(
                        btnDelete.getTopLevelAncestor(),
                        new javax.swing.JLabel(mensaje),
                        "Borrar Etiqueta",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null, borrarOpciones, borrarOpciones[2]
                    );

                    if (borrarChoice == 0) {
                        boolean ok = dataManager.getTagDAO().deleteTag(tag.getId());
                        if (ok) {
                            statusBarManager.mostrarMensajeTemporal(
                                "Tag '" + tag.getNombre() + "' eliminado. Sus hijos se han movido al nivel superior.", 4000);
                            afterTagStructureChanged(tag.getParentId());
                        } else {
                            statusBarManager.mostrarMensajeTemporal("Error al eliminar el tag.", 3000);
                        }
                    } else if (borrarChoice == 1) {
                        int confirmFinal = JOptionPane.showConfirmDialog(
                            btnDelete.getTopLevelAncestor(),
                            "<html>¿Confirmas borrar '<b>" + tag.getNombre() + "</b>' y TODA su jerarquía descendiente?<br>"
                            + "Esta acción no se puede deshacer. Se desasociarán " + imageCount + " imagen(es).</html>",
                            "Confirmar borrado de rama",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                        );
                        if (confirmFinal != JOptionPane.YES_OPTION) return;

                        boolean ok = dataManager.deleteTagBranch(tag);
                        if (ok) {
                            statusBarManager.mostrarMensajeTemporal(
                                "Rama '" + tag.getNombre() + "' eliminada completamente.", 4000);
                            afterTagStructureChanged(tag.getParentId());
                        } else {
                            statusBarManager.mostrarMensajeTemporal("Error al eliminar la rama.", 3000);
                        }
                    }

                } else {
                    String msg = "¿Borrar el tag '" + tag.getNombre() + "'?";
                    if (imageCount > 0) {
                        msg += "\n\n" + imageCount + " imagen(es) perderán esta etiqueta (no se borrarán).";
                    }
                    int confirm = JOptionPane.showConfirmDialog(
                        btnDelete.getTopLevelAncestor(),
                        msg, "Confirmar Borrado",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (confirm != JOptionPane.YES_OPTION) return;

                    boolean ok = dataManager.getTagDAO().deleteTag(tag.getId());
                    if (ok) {
                        statusBarManager.mostrarMensajeTemporal(
                            "Tag '" + tag.getNombre() + "' eliminado.", 3000);
                        afterTagStructureChanged(tag.getParentId());
                    } else {
                        statusBarManager.mostrarMensajeTemporal("Error al eliminar el tag.", 3000);
                    }
                }
            });
        }
    } // ---FIN de metodo [setupTagCRUDButtons]---

    /**
     * Acción de limpieza/refresco que se ejecuta tras cualquier cambio en la estructura de tags.
     * @param selectTagId ID del tag a seleccionar tras el refresco, o null para mantener la selección.
     */
    private void afterTagStructureChanged(Long selectTagId) {
        refreshTagTreeAndSelect(selectTagId);
        refreshFlatTagList();
        refreshAvailableTags();
        refreshIntelliSense();
        dataManager.invalidateTagCache();
    } // ---FIN de metodo [afterTagStructureChanged]---

    /**
     * Resuelve una ruta en notación punto buscando SOLO tags existentes (sin crear nuevos).
     * @param dotPath La ruta a resolver (ej. "juegos.estrategia").
     * @return Lista de tags desde la raíz hasta la hoja, o null si algún segmento no existe.
     */
    private List<Tag> resolveExistingPath(String dotPath) {
        if (dotPath == null || dotPath.isBlank()) return Collections.emptyList();
        String[] segments = dotPath.split("\\.");
        Long parentId = null;
        List<Tag> path = new ArrayList<>();

        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.isEmpty()) continue;

            java.util.Optional<Tag> existing = dataManager.getTagDAO().findTagByNameAndParent(trimmed, parentId);
            if (existing.isEmpty()) {
                logger.warn("resolveExistingPath: no se encontró el segmento '{}' bajo padre ID {}", trimmed, parentId);
                return null; // ruta no encontrada
            }
            path.add(existing.get());
            parentId = existing.get().getId();
        }
        return path;
    } // ---FIN de metodo [resolveExistingPath]---

    /**
     * Obtiene el tag a partir del texto del campo IntelliSense unificado, o del árbol si el texto está vacío.
     */
    private Tag resolveTagFromInputOrTree(TagIntelliSenseField field) {
        if (field != null) {
            String text = field.getText().trim();
            if (!text.isEmpty()) {
                Tag found = findTagByText(text);
                if (found != null) return found;
            }
        }
        return getSelectedTagFromTree();
    }

    /**
     * Busca un tag existente cuyo nombre o ruta completa coincida exactamente con el texto dado.
     * @return el Tag encontrado, o null si no hay coincidencia unívoca.
     */
    private Tag findTagByText(String text) {
        if (text == null || text.isBlank()) return null;
        String trimmed = text.trim();

        List<Tag> allTags = dataManager.getAllTags();
        List<Tag> nameMatches = new ArrayList<>();
        for (Tag tag : allTags) {
            if (tag.getNombre().equalsIgnoreCase(trimmed)) {
                nameMatches.add(tag);
            }
        }
        if (nameMatches.size() == 1) return nameMatches.get(0);

        // Intentar resolución como ruta punto (solo si tiene puntos)
        List<Tag> resolvedPath = trimmed.contains(".") ? resolveExistingPath(trimmed) : null;
        if (resolvedPath != null && !resolvedPath.isEmpty()) {
            return resolvedPath.get(resolvedPath.size() - 1);
        }

        // Múltiples coincidencias de nombre: comprobar si el texto es la ruta completa de alguna
        if (nameMatches.size() > 1) {
            for (Tag tag : nameMatches) {
                String fullPath = dataManager.getTagDAO().getTagFullPath(tag.getId());
                String dotPath = fullPath.replace(" > ", ".");
                if (dotPath.equalsIgnoreCase(trimmed)) {
                    return tag;
                }
            }
        }
        return null;
    }

    /**
     * Actualiza el estado de los botones CRUD y filtra la lista plana según el texto del campo unificado.
     * Texto vacío → todos los botones habilitados, lista completa.
     * Texto coincide con tag → [E] y [-] si no es readOnly, [+] deshabilitado, lista completa.
     * Texto sin coincidencia → [+] habilitado, [E] y [-] deshabilitados, lista filtrada.
     */
    private void updateTagCRUDButtonsByText(String text) {
        JButton btnCreate = registry.get("btn.datamode.tag.create");
        JButton btnEdit   = registry.get("btn.datamode.tag.edit");
        JButton btnDelete = registry.get("btn.datamode.tag.delete");
        if (btnCreate == null && btnEdit == null && btnDelete == null) return;

        String trimmed = (text != null) ? text.trim() : "";

        if (trimmed.isEmpty()) {
            if (btnCreate != null) btnCreate.setEnabled(true);
            if (btnEdit   != null) btnEdit.setEnabled(true);
            if (btnDelete != null) btnDelete.setEnabled(true);
            restoreFlatTagList();
            return;
        }

        Tag matchedTag = findTagByText(trimmed);
        if (matchedTag != null) {
            if (btnCreate != null) btnCreate.setEnabled(false);
            boolean canModify = !matchedTag.isReadOnly();
            if (btnEdit   != null) btnEdit.setEnabled(canModify);
            if (btnDelete != null) btnDelete.setEnabled(canModify);
            restoreFlatTagList();
        } else {
            if (btnCreate != null) btnCreate.setEnabled(true);
            if (btnEdit   != null) btnEdit.setEnabled(false);
            if (btnDelete != null) btnDelete.setEnabled(false);
            filterFlatTagList(trimmed);
        }
    }

    /**
     * Guarda el estado de expansión actual del árbol (ids de tags expandidos).
     */
    private void saveTreeExpansionState() {
        savedExpandedTagIds.clear();
        JTree tree = registry.get("tree.datamode.alltags");
        if (tree == null) return;
        for (int row = 0; row < tree.getRowCount(); row++) {
            if (tree.isExpanded(row)) {
                Object last = tree.getPathForRow(row).getLastPathComponent();
                if (last instanceof Tag) {
                    savedExpandedTagIds.add(((Tag) last).getId());
                }
            }
        }
    } // --- Fin del metodo/clase saveTreeExpansionState ---


    /**
     * Restaura el estado de expansión guardado del árbol.
     */
    private void restoreTreeExpansionState() {
        JTree tree = registry.get("tree.datamode.alltags");
        if (tree == null) return;
        for (int row = 0; row < tree.getRowCount(); row++) {
            javax.swing.tree.TreePath path = tree.getPathForRow(row);
            if (path != null) {
                Object last = path.getLastPathComponent();
                if (last instanceof Tag && savedExpandedTagIds.contains(((Tag) last).getId())) {
                    tree.expandRow(row);
                }
            }
        }
    } // --- Fin del metodo/clase restoreTreeExpansionState ---


    /**
     * Filtra la lista plana de tags para mostrar solo aquellos cuyo nombre contiene el texto.
     */
    private void filterFlatTagList(String filterText) {
        JList<Tag> flatList = registry.get("list.datamode.alltags.flat");
        if (flatList == null) return;
        DefaultListModel<Tag> model = (DefaultListModel<Tag>) flatList.getModel();
        model.clear();
        String lower = filterText.toLowerCase();
        for (Tag tag : flatListOriginalOrder) {
            if (tag.getNombre().toLowerCase().contains(lower)) {
                model.addElement(tag);
            }
        }
    }

    /**
     * Cambia la CardLayout del contenedor árbol/lista a mostrar la LISTA.
     */
    private void switchToListCard() {
        JPanel container = registry.get("panel.datamode.treelist.container");
        if (container != null) {
            java.awt.CardLayout cl = (java.awt.CardLayout) container.getLayout();
            cl.show(container, "LIST");
        }
    } // --- Fin del metodo/clase switchToListCard ---


    /**
     * Cambia la CardLayout del contenedor árbol/lista a mostrar el ÁRBOL.
     */
    private void switchToTreeCard() {
        JPanel container = registry.get("panel.datamode.treelist.container");
        if (container != null) {
            java.awt.CardLayout cl = (java.awt.CardLayout) container.getLayout();
            cl.show(container, "TREE");
        }
    } // --- Fin del metodo/clase switchToTreeCard ---

    /**
     * Restaura la lista plana de tags a su estado completo original (sin filtro).
     */
    private void restoreFlatTagList() {
        JList<Tag> flatList = registry.get("list.datamode.alltags.flat");
        if (flatList == null) return;
        DefaultListModel<Tag> model = (DefaultListModel<Tag>) flatList.getModel();
        model.clear();
        for (Tag tag : flatListOriginalOrder) {
            model.addElement(tag);
        }
    }

    /**
     * Recarga la lista de tags disponibles en el JComboBox del TagManagementPanel.
     */
    private void refreshAvailableTags() {
        javax.swing.JComboBox<String> comboNewTag = registry.get("combo.datamode.img.newtag");
        if (comboNewTag != null) {
            List<Tag> allTags = dataManager.getAllTags();
            javax.swing.DefaultComboBoxModel<String> model = new javax.swing.DefaultComboBoxModel<>();
            model.addElement("");
            if (allTags != null) {
                for (Tag tag : allTags) {
                    model.addElement(tag.getNombre());
                }
            }
            comboNewTag.setModel(model);
            comboNewTag.setSelectedItem("");
        }
    } // ---FIN de metodo [refreshAvailableTags]---

    /**
     * Configura los listeners para la asignación de tags a imágenes.
     */
    private void setupImageTagCRUDButtons() {
        JButton btnAssign = registry.get("btn.datamode.tag.assign");
        javax.swing.JTextField intelliSenseField = registry.get("textfield.datamode.tag.intellisense");
        JButton btnRemoveTag = registry.get("btn.datamode.tag.removetag");
        JList<String> gridList = registry.get("list.datamode.grid");
        
        if (gridList != null && btnAssign != null) {
            gridList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    boolean hasSelection = !gridList.isSelectionEmpty();
                    btnAssign.setEnabled(hasSelection);
                }
            });
        }
        
        if (btnAssign != null && intelliSenseField != null) {
            btnAssign.addActionListener(e -> {
                String dotPath = intelliSenseField.getText();
                if (dotPath == null || dotPath.trim().isEmpty()) return;

                String path = dotPath.trim();
                List<Path> selectedPaths = getSelectedImagePaths();
                if (selectedPaths.isEmpty()) return;

                if (intelliSenseField instanceof TagIntelliSenseField isField) {
                    if (!isField.ensurePathResolved()) return;
                } else {
                    // Fallback: usar el método original si no es TagIntelliSenseField
                    List<Tag> resolvedPath = resolveWithAmbiguityDialog(path, btnAssign.getTopLevelAncestor());
                    if (resolvedPath.isEmpty()) {
                        JOptionPane.showMessageDialog(btnAssign.getTopLevelAncestor(),
                            "No se pudo resolver la ruta: " + path);
                        return;
                    }
                }

                dataManager.assignDotNotationTagToImages(selectedPaths, path);

                intelliSenseField.setText("");
                updateTagPanelSelection();
                refreshTagTreeAndSelect(null);
                refreshAvailableTags();
            });
        }
        
        if (btnRemoveTag != null) {
            btnRemoveTag.addActionListener(e -> {
                TagManagementPanel tagPanel = registry.get("panel.datamode.tagmanagement");
                if (tagPanel == null) return;
                Tag selectedTag = tagPanel.getSelectedTag();
                if (selectedTag == null) {
                    JOptionPane.showMessageDialog(btnRemoveTag.getTopLevelAncestor(),
                        "Selecciona un tag en la lista de etiquetas de la imagen para borrarlo.",
                        "Borrar Tag Asignado", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                List<Path> selectedPaths = getSelectedImagePaths();
                if (selectedPaths.isEmpty()) return;

                dataManager.removeTagFromImages(selectedPaths, selectedTag);
                updateTagPanelSelection();
                refreshTagTreeAndSelect(null);
                refreshAvailableTags();
                statusBarManager.mostrarMensajeTemporal(
                    "Tag '" + selectedTag.getNombre() + "' eliminado de la(s) imagen(es) seleccionada(s).", 3000);
            });
        }
        // Botón [E] derecho: editar tag (misma lógica que el izquierdo)
        JButton btnEditRight = registry.get("btn.datamode.tag.edit.right");
        if (btnEditRight != null) {
            btnEditRight.addActionListener(e -> {
                Tag tag = getSelectedTagFromTree();
                if (tag == null) {
                    JOptionPane.showMessageDialog(btnEditRight.getTopLevelAncestor(),
                        "Selecciona un tag en el árbol para editar.", "Editar Tag",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (tag.isReadOnly()) {
                    JOptionPane.showMessageDialog(btnEditRight.getTopLevelAncestor(),
                        "El tag '" + tag.getNombre() + "' es del sistema y no se puede modificar.",
                        "Editar Tag", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                showEditTagDialog(tag, btnEditRight);
            });
        }
    } // ---FIN de metodo [setupImageTagCRUDButtons]---
    
    private void showEditTagDialog(Tag tag, java.awt.Component parent) {
        java.awt.Window parentWindow = javax.swing.SwingUtilities.getWindowAncestor(parent);
        Object[] opciones = {"Renombrar", "Mover", "Cancelar"};
        int choice = JOptionPane.showOptionDialog(
            parentWindow,
            "¿Qué deseas hacer con '" + tag.getNombre() + "'?",
            "Editar Etiqueta",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null, opciones, opciones[0]
        );

        if (choice == 0) {
            String nuevoNombre = (String) JOptionPane.showInputDialog(
                parentWindow,
                "Nuevo nombre para '" + tag.getNombre() + "':",
                "Renombrar Tag", JOptionPane.PLAIN_MESSAGE,
                null, null, tag.getNombre());
            if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) return;

            boolean ok = dataManager.getTagDAO().updateTagName(tag.getId(), nuevoNombre.trim());
            if (ok) {
                statusBarManager.mostrarMensajeTemporal("Tag renombrado a '" + nuevoNombre.trim() + "'.", 3000);
                afterTagStructureChanged(tag.getId());
            } else {
                JOptionPane.showMessageDialog(parentWindow,
                    "No se pudo renombrar. Ya existe un tag con ese nombre en el mismo nivel.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }

        } else if (choice == 1) {
            int impacto = dataManager.getImageCountForTagRecursive(tag);
            java.awt.Frame mainFrame = null;
            if (parentWindow instanceof java.awt.Frame) mainFrame = (java.awt.Frame) parentWindow;

            javax.swing.JPanel movePanel = new javax.swing.JPanel(new java.awt.BorderLayout(5, 8));
            String impactoTxt = impacto > 0
                ? "\n\u26a0 Esta operación afectará a " + impacto + " imagen(es) en la jerarquía."
                : "";
            movePanel.add(new javax.swing.JLabel(
                "<html>Mover '<b>" + tag.getNombre() + "</b>' bajo un nuevo padre.<br>"
                + "Introduce la ruta del nuevo padre en notación punto (ej. juegos.estrategia),<br>"
                + "o deja en blanco para mover a la RAÍZ." + impactoTxt + "</html>"),
                java.awt.BorderLayout.NORTH);

            TagIntelliSenseField moveField = new TagIntelliSenseField();
            moveField.refreshTags(dataManager.getAllTags());
            moveField.setColumns(25);
            moveField.setToolTipText("Ruta del nuevo padre (vacío = raíz)");
            movePanel.add(moveField, java.awt.BorderLayout.CENTER);

            int confirm = JOptionPane.showConfirmDialog(
                parentWindow,
                movePanel, "Mover Etiqueta",
                JOptionPane.OK_CANCEL_OPTION,
                impacto > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.PLAIN_MESSAGE
            );
            if (confirm != JOptionPane.OK_OPTION) return;

            String newParentPath = moveField.getText().trim();
            Tag newParent = null;

            if (!newParentPath.isEmpty()) {
                List<Tag> resolved = resolveExistingPath(newParentPath);
                if (resolved == null || resolved.isEmpty()) {
                    JOptionPane.showMessageDialog(parentWindow,
                        "No se encontró la ruta '" + newParentPath + "' en la biblioteca.\n"
                        + "Solo puedes mover a etiquetas existentes.",
                        "Ruta no encontrada", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                newParent = resolved.get(resolved.size() - 1);
            }

            boolean ok = dataManager.moveTag(tag, newParent);
            if (ok) {
                String destino = (newParent != null) ? "'" + newParent.getNombre() + "'" : "la raíz";
                statusBarManager.mostrarMensajeTemporal("Tag '" + tag.getNombre() + "' movido a " + destino + ".", 4000);
                afterTagStructureChanged(tag.getId());
            } else {
                JOptionPane.showMessageDialog(parentWindow,
                    "No se pudo mover el tag.\nVerifica que:\n"
                    + "  \u2022 El destino no es un descendiente del tag.\n"
                    + "  \u2022 No existe ya un tag con ese nombre en el destino.",
                    "Error al mover", JOptionPane.ERROR_MESSAGE);
            }
        }
    } // ---FIN de metodo [showEditTagDialog]---
    
    private List<Path> getSelectedImagePaths() {
        JList<String> gridList = registry.get("list.datamode.grid");
        if (gridList == null) return new ArrayList<>();
        List<String> selectedKeys = gridList.getSelectedValuesList();
        List<Path> paths = new ArrayList<>();
        Map<String, Path> pathMap = model.getDatosListContext().getRutaCompletaMap();
        
        for (String key : selectedKeys) {
            Path p = pathMap.get(key);
            if (p != null) paths.add(p);
        }
        return paths;
    } // ---FIN de metodo [getSelectedImagePaths]---

    private void updateTagPanelSelection() {
        TagManagementPanel tagPanel = registry.get("panel.datamode.tagmanagement");
        if (tagPanel == null) return;

        List<Path> selectedPaths = getSelectedImagePaths();
        if (selectedPaths.isEmpty()) {
            tagPanel.clearPanel();
            return;
        }

        // Si hay varias imágenes, buscamos los tags comunes
        if (selectedPaths.size() > 1) {
            Set<Tag> commonTags = null;
            for (Path path : selectedPaths) {
                List<Tag> tags = dataManager.getTagsForImage(path);
                if (commonTags == null) {
                    commonTags = new HashSet<>(tags);
                } else {
                    commonTags.retainAll(tags);
                }
            }
            tagPanel.setTags(new ArrayList<>(commonTags != null ? commonTags : new HashSet<>()));
        } else {
            // Una sola imagen
            tagPanel.setTags(dataManager.getTagsForImage(selectedPaths.get(0)));
        }
    } // ---FIN de metodo [updateTagPanelSelection]---
    
    /**
     * Carga las imágenes asociadas a un tag.
     * @param tag Tag del cual cargar imágenes
     */
    private void loadImagesForTag(Tag tag) {
        JList<String> gridList = registry.get("list.datamode.grid");
        if (gridList == null) {
            logger.error("No se encontró 'list.datamode.grid' en el registro.");
            return;
        }

        List<String> imagePaths = dataManager.getImagePathsForTag(tag);
        imagePaths = dataManager.filterConnectedPaths(imagePaths);
        
        DefaultListModel<String> gridListModel = new DefaultListModel<>();
        DefaultListModel<String> fileNameModel = new DefaultListModel<>();
        Map<String, Path> gridPathMap = new java.util.HashMap<>();

        int duplicateKeyCounter = 0;
        for (String pathStr : imagePaths) {
            Path path = Paths.get(pathStr);
            Path fn = path.getFileName();
            String key = (fn != null) ? fn.toString() : path.toString();
            
            while (gridPathMap.containsKey(key)) {
                duplicateKeyCounter++;
                key = ((fn != null) ? fn.toString() : path.toString()) + " (" + duplicateKeyCounter + ")";
            }

            gridListModel.addElement(key);
            fileNameModel.addElement(key);
            gridPathMap.put(key, path);
        }
        
        model.getDatosListContext().getRutaCompletaMap().clear();
        model.getDatosListContext().getRutaCompletaMap().putAll(gridPathMap);
        model.getDatosListContext().setModeloLista(fileNameModel);

        // Actualizar modelo maestro de la lista central
        masterFileListModel = fileNameModel;

        SwingUtilities.invokeLater(() -> {
            gridList.setModel(gridListModel);
            JList<String> fileNameList = registry.get("list.datamode.filenames");
            if (fileNameList != null) {
                JToggleButton btnTornado = registry.get("toggle.datamode.tornado");
                if (btnTornado == null || !btnTornado.isSelected()) {
                    fileNameList.setModel(fileNameModel);
                }
            }
            if (visorController != null && visorController.getListCoordinator() != null) {
                visorController.getListCoordinator().forzarActualizacionEstadoAcciones();
            }
            if (visorController != null && visorController.getInfobarImageManager() != null) {
                visorController.getInfobarImageManager().actualizar();
            }
            logger.debug("Grid y lista central actualizados con {} elementos.", gridListModel.getSize());
        });
    } // --- Fin del metodo/clase loadImagesForTag ---


    /**
     * Carga las imágenes asociadas a un tag por su nombre.
     * @param tagName Nombre del tag
     */
    private void loadImagesForTagName(String tagName) {
        JList<String> gridList = registry.get("list.datamode.grid");
        if (gridList == null) {
            logger.error("No se encontró 'list.datamode.grid' en el registro.");
            return;
        }

        List<String> imagePaths = dataManager.getImagePathsForTagName(tagName);
        imagePaths = dataManager.filterConnectedPaths(imagePaths);

        DefaultListModel<String> gridListModel = new DefaultListModel<>();
        DefaultListModel<String> fileNameModel = new DefaultListModel<>();
        Map<String, Path> gridPathMap = new java.util.HashMap<>();

        int duplicateKeyCounter = 0;
        for (String pathStr : imagePaths) {
            Path path = Paths.get(pathStr);
            Path fn = path.getFileName();
            String key = (fn != null) ? fn.toString() : path.toString();
            
            while (gridPathMap.containsKey(key)) {
                duplicateKeyCounter++;
                key = ((fn != null) ? fn.toString() : path.toString()) + " (" + duplicateKeyCounter + ")";
            }

            gridListModel.addElement(key);
            fileNameModel.addElement(key);
            gridPathMap.put(key, path);
        }

        model.getDatosListContext().getRutaCompletaMap().clear();
        model.getDatosListContext().getRutaCompletaMap().putAll(gridPathMap);
        model.getDatosListContext().setModeloLista(fileNameModel);

        masterFileListModel = fileNameModel;

        SwingUtilities.invokeLater(() -> {
            gridList.setModel(gridListModel);
            JList<String> fileNameList = registry.get("list.datamode.filenames");
            if (fileNameList != null) {
                JToggleButton btnTornado = registry.get("toggle.datamode.tornado");
                if (btnTornado == null || !btnTornado.isSelected()) {
                    fileNameList.setModel(fileNameModel);
                }
            }
            if (visorController != null && visorController.getListCoordinator() != null) {
                visorController.getListCoordinator().forzarActualizacionEstadoAcciones();
            }
            if (visorController != null && visorController.getInfobarImageManager() != null) {
                visorController.getInfobarImageManager().actualizar();
            }
            logger.debug("Grid y lista central actualizados por nombre '{}' con {} elementos.", tagName, gridListModel.getSize());
        });
    } // --- Fin del metodo/clase loadImagesForTagName ---


    /**
     * Carga todas las imágenes.
     */
    private void loadAllImages() {
        JList<String> gridList = registry.get("list.datamode.grid");
        if (gridList == null) {
            logger.error("No se encontró 'list.datamode.grid' en el registro.");
            return;
        }

        List<String> imagePaths = dataManager.getAllImagePaths();
        imagePaths = dataManager.filterConnectedPaths(imagePaths);
        
        DefaultListModel<String> gridListModel = new DefaultListModel<>();
        DefaultListModel<String> fileNameModel = new DefaultListModel<>();
        Map<String, Path> gridPathMap = new java.util.HashMap<>();

        int duplicateKeyCounter = 0;
        for (String pathStr : imagePaths) {
            Path path = Paths.get(pathStr);
            Path fn = path.getFileName();
            String key = (fn != null) ? fn.toString() : path.toString();
            
            while (gridPathMap.containsKey(key)) {
                duplicateKeyCounter++;
                key = ((fn != null) ? fn.toString() : path.toString()) + " (" + duplicateKeyCounter + ")";
            }

            gridListModel.addElement(key);
            fileNameModel.addElement(key);
            gridPathMap.put(key, path);
        }
        
        model.getDatosListContext().getRutaCompletaMap().clear();
        model.getDatosListContext().getRutaCompletaMap().putAll(gridPathMap);
        model.getDatosListContext().setModeloLista(fileNameModel);

        masterFileListModel = fileNameModel;

        SwingUtilities.invokeLater(() -> {
            gridList.setModel(gridListModel);
            JList<String> fileNameList = registry.get("list.datamode.filenames");
            if (fileNameList != null) {
                JToggleButton btnTornado = registry.get("toggle.datamode.tornado");
                if (btnTornado == null || !btnTornado.isSelected()) {
                    fileNameList.setModel(fileNameModel);
                }
            }
            if (visorController != null && visorController.getListCoordinator() != null) {
                visorController.getListCoordinator().forzarActualizacionEstadoAcciones();
            }
            if (visorController != null && visorController.getInfobarImageManager() != null) {
                visorController.getInfobarImageManager().actualizar();
            }
            logger.debug("Grid y lista central actualizados con todas las {} imágenes.", gridListModel.getSize());
        });
    } // --- Fin del metodo/clase loadAllImages ---

    // -------------------------------------------------------------------------
    // Navegación en modo DATOS (opera directamente sobre el grid)
    // -------------------------------------------------------------------------

    /**
     * Reemplaza la selección del grid con un único índice.
     * Usa clearSelection + addSelectionInterval en vez de setSelectedIndex
     * porque el grid usa MULTIPLE_INTERVAL_SELECTION y setSelectedIndex sería aditivo.
     * @param gridList El JList del grid de datos
     * @param index    El índice a seleccionar
     */
    private void seleccionarIndiceGrid(JList<String> gridList, int index) {
        gridList.getSelectionModel().clearSelection();
        gridList.getSelectionModel().addSelectionInterval(index, index);
    } // --- Fin del método seleccionarIndiceGrid ---


    /**
     * Navega a la imagen siguiente dentro del grid del modo DATOS.
     * Respeta la configuración de navegación circular.
     */
    public void navegarSiguiente() {
        JList<String> gridList = registry.get("list.datamode.grid");
        if (gridList == null) return;
        DefaultListModel<String> listModel = (DefaultListModel<String>) gridList.getModel();
        if (listModel == null || listModel.isEmpty()) return;
        int currentIndex = gridList.getSelectedIndex();
        int nextIndex;
        if (model.isNavegacionCircularActivada()) {
            nextIndex = (currentIndex + 1) % listModel.getSize();
        } else {
            nextIndex = Math.min(currentIndex + 1, listModel.getSize() - 1);
        }
        if (nextIndex != currentIndex) {
            seleccionarIndiceGrid(gridList, nextIndex);
        }
    } // --- Fin del método navegarSiguiente ---


    /**
     * Navega a la imagen anterior dentro del grid del modo DATOS.
     * Respeta la configuración de navegación circular.
     */
    public void navegarAnterior() {
        JList<String> gridList = registry.get("list.datamode.grid");
        if (gridList == null) return;
        DefaultListModel<String> listModel = (DefaultListModel<String>) gridList.getModel();
        if (listModel == null || listModel.isEmpty()) return;
        int currentIndex = gridList.getSelectedIndex();
        int prevIndex;
        if (model.isNavegacionCircularActivada()) {
            prevIndex = (currentIndex <= 0) ? listModel.getSize() - 1 : currentIndex - 1;
        } else {
            prevIndex = Math.max(0, currentIndex - 1);
        }
        if (prevIndex != currentIndex) {
            seleccionarIndiceGrid(gridList, prevIndex);
        }
    } // --- Fin del método navegarAnterior ---


    /**
     * Navega a la primera imagen del grid del modo DATOS.
     */
    public void navegarPrimero() {
        JList<String> gridList = registry.get("list.datamode.grid");
        if (gridList == null) return;
        DefaultListModel<String> listModel = (DefaultListModel<String>) gridList.getModel();
        if (listModel == null || listModel.isEmpty()) return;
        if (gridList.getSelectedIndex() != 0) {
            seleccionarIndiceGrid(gridList, 0);
        }
    } // --- Fin del método navegarPrimero ---


    /**
     * Navega a la última imagen del grid del modo DATOS.
     */
    public void navegarUltimo() {
        JList<String> gridList = registry.get("list.datamode.grid");
        if (gridList == null) return;
        DefaultListModel<String> listModel = (DefaultListModel<String>) gridList.getModel();
        if (listModel == null || listModel.isEmpty()) return;
        int lastIndex = listModel.getSize() - 1;
        if (gridList.getSelectedIndex() != lastIndex) {
            seleccionarIndiceGrid(gridList, lastIndex);
        }
    } // --- Fin del método navegarUltimo ---


    /**
     * Navega un bloque (10 imágenes) hacia atrás en el grid del modo DATOS.
     */
    public void navegarBloqueAnterior() {
        JList<String> gridList = registry.get("list.datamode.grid");
        if (gridList == null) return;
        DefaultListModel<String> listModel = (DefaultListModel<String>) gridList.getModel();
        if (listModel == null || listModel.isEmpty()) return;
        int currentIndex = gridList.getSelectedIndex();
        int prevIndex = Math.max(0, currentIndex - 10);
        if (prevIndex != currentIndex) {
            seleccionarIndiceGrid(gridList, prevIndex);
        }
    } // --- Fin del método navegarBloqueAnterior ---


    /**
     * Navega un bloque (10 imágenes) hacia adelante en el grid del modo DATOS.
     */
    public void navegarBloqueSiguiente() {
        JList<String> gridList = registry.get("list.datamode.grid");
        if (gridList == null) return;
        DefaultListModel<String> listModel = (DefaultListModel<String>) gridList.getModel();
        if (listModel == null || listModel.isEmpty()) return;
        int currentIndex = gridList.getSelectedIndex();
        int nextIndex = Math.min(currentIndex + 10, listModel.getSize() - 1);
        if (nextIndex != currentIndex) {
            seleccionarIndiceGrid(gridList, nextIndex);
        }
    } // --- Fin del método navegarBloqueSiguiente ---


} // --- FIN de clase DataController ---