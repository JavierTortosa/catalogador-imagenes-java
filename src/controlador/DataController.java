package controlador;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.DataManager;
import controlador.managers.InfobarStatusManager;
import controlador.managers.interfaces.IProjectManager;
import controlador.utils.ComponentRegistry;
import modelo.ListContext;
import modelo.VisorModel;
import modelo.datos.Disco;
import modelo.datos.Tag;
import servicios.db.TagDAO;
import vista.components.TagIntelliSenseField;
import vista.panels.DriveListPanel;
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

    // Flag para evitar bucles de sincronización entre lista central y grid
    private boolean isSyncingLists = false;

    // Flag para evitar refrescar IntelliSense repetidamente si los datos no han cambiado
    private boolean intelliSenseRefreshed = false;

    public DataController(VisorModel model, ComponentRegistry registry, DataManager dataManager) {
        this.model = model;
        this.registry = registry;
        this.dataManager = dataManager;
    } // ---FIN de constructor [DataController]---

    public DataManager getDataManager() {
        return this.dataManager;
    } // ---FIN de metodo [getDataManager]---

    /**
     * Permite inyectar el VisorController principal para acceder a atajos y acciones globales.
     * @param visorController La instancia del visor controller principal.
     */
    public void setVisorController(VisorController visorController) {
        this.visorController = visorController;
    } // ---FIN de metodo [setVisorController]---

    /**
     * Permite inyectar el ProjectManager para poder marcar imágenes desde el Modo Datos.
     * @param projectManager La instancia del gestor de proyectos.
     */
    public void setProjectManager(IProjectManager projectManager) {
        this.projectManager = projectManager;
    } // ---FIN de metodo [setProjectManager]---

    /**
     * Permite inyectar el StatusBarManager para mostrar mensajes de estado.
     */
    public void setStatusBarManager(InfobarStatusManager statusBarManager) {
        this.statusBarManager = statusBarManager;
    } // ---FIN de metodo [setStatusBarManager]---

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
        setupTagManagementCallbacks();
        setupListeners();
        setupTagCRUDButtons();
        setupImageTagCRUDButtons();
        setupMaintenanceDialog();
        
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
    } // ---FIN de metodo [initialize]---
    
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
    }
    
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
        
        // Restaurar la etiqueta guardada o seleccionar "Biblioteca" por defecto
        JTree allTagsTree = registry.get("tree.datamode.alltags");
        if (allTagsTree != null) {
            String savedTag = model != null ? model.getDatosListContext().getDatosSelectedTag() : null;
            if (savedTag != null && !"Biblioteca".equals(savedTag)) {
                selectTagNode(allTagsTree, savedTag);
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
    } // ---FIN de metodo [activate]---

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
    } // ---FIN de metodo [guardarContexto]---

    private void selectTagNode(JTree tree, String tagName) {
        javax.swing.tree.TreeModel model = tree.getModel();
        if (model == null || model.getRoot() == null) return;
        buscarYSeleccionarNodo(tree, model.getRoot(), tagName);
    }

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
    }

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
    } // ---FIN de metodo [initializeTagTree]---

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
    } // ---FIN de metodo [refreshTagTreeAndSelect]---

    private long getSelectedTagIdFromTree() {
        JTree tree = registry.get("tree.datamode.alltags");
        if (tree == null) return -1;
        javax.swing.tree.TreePath selPath = tree.getSelectionPath();
        if (selPath == null) return -1;
        Object last = selPath.getLastPathComponent();
        if (last instanceof Tag) return ((Tag) last).getId();
        return -1;
    } // ---FIN de metodo [getSelectedTagIdFromTree]---

    private void selectTagInTree(long tagId) {
        JTree tree = registry.get("tree.datamode.alltags");
        if (tree == null || tagTreeModel == null) return;
        javax.swing.tree.TreePath path = findTreePathForTagId(tagId);
        if (path != null) {
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
        }
    } // ---FIN de metodo [selectTagInTree]---

    private javax.swing.tree.TreePath findTreePathForTagId(long tagId) {
        JTree tree = registry.get("tree.datamode.alltags");
        if (tree == null || tagTreeModel == null) return null;
        for (int row = 0; row < tree.getRowCount(); row++) {
            javax.swing.tree.TreePath path = tree.getPathForRow(row);
            if (path != null) {
                Object last = path.getLastPathComponent();
                if (last instanceof Tag && ((Tag) last).getId() == tagId) {
                    return path;
                }
            }
        }
        return null;
    } // ---FIN de metodo [findTreePathForTagId]---

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
    } // ---FIN de metodo [initializeFlatTagList]---

    @SuppressWarnings("unchecked")
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
    } // ---FIN de metodo [refreshFlatTagList]---

    /**
     * Notificado desde DataBuilder cuando el usuario cambia entre vista árbol y lista.
     * @param isTree true si se cambió a vista árbol, false si a vista lista.
     */
    public void onTagViewSwitched(boolean isTree) {
        logger.debug("Vista de tags cambiada: {} ", isTree ? "árbol" : "lista");
        if (!isTree) {
            refreshFlatTagList();
        }
    } // ---FIN de metodo [onTagViewSwitched]---

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
    } // ---FIN de metodo [ordenarListaPlana]---

    private void initializeDriveList() {
        DriveListPanel drivePanel = registry.get("panel.datamode.drives");
        if (drivePanel == null) {
            logger.error("No se encontró 'panel.datamode.drives' en el registro.");
            return;
        }

        drivePanel.setOnRefresh(this::refreshDriveList);
        refreshDriveList();
    } // ---FIN de metodo [initializeDriveList]---

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
    } // ---FIN de metodo [refreshDriveList]---

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
                        updateTagPanelSelection();
                        JList<String> gridList = registry.get("list.datamode.grid");
                        if (gridList != null) {
                            gridList.setSelectedValue(selectedKey, true);
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
    } // ---FIN de metodo [initializeFileNameList]---

    private void initializeTornadoFilter() {
        JTextField tornadoField = registry.get("textfield.datamode.tornado");
        JToggleButton btnTornado = registry.get("toggle.datamode.tornado");
        JList<String> fileNameList = registry.get("list.datamode.filenames");
        if (tornadoField == null || btnTornado == null || fileNameList == null) return;

        btnTornado.addActionListener(e -> {
            if (btnTornado.isSelected()) {
                String text = tornadoField.getText();
                if (!text.isEmpty()) {
                    applyTornadoFilter(text);
                }
            } else {
                refreshVisibleFileList();
            }
        });

        tornadoField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void handleTextChange() {
                if (btnTornado.isSelected()) {
                    applyTornadoFilter(tornadoField.getText());
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { handleTextChange(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { handleTextChange(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { handleTextChange(); }
        });

        tornadoField.addActionListener(e -> {
            if (!btnTornado.isSelected() && !tornadoField.getText().isEmpty()) {
                findNextTornadoMatch(tornadoField.getText());
            }
        });
    } // ---FIN de metodo [initializeTornadoFilter]---

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
    } // ---FIN de metodo [applyTornadoFilter]---

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
    } // ---FIN de metodo [findNextTornadoMatch]---

    private javax.swing.DefaultListModel<String> getMasterFileListModel() {
        return masterFileListModel;
    }

    private javax.swing.DefaultListModel<String> masterFileListModel = new javax.swing.DefaultListModel<>();

    private void refreshVisibleFileList() {
        JList<String> fileNameList = registry.get("list.datamode.filenames");
        if (fileNameList == null) return;
        fileNameList.setModel(masterFileListModel);
    } // ---FIN de metodo [refreshVisibleFileList]---

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
                    updateTagPanelSelection();
                    JList<String> fileNameList = registry.get("list.datamode.filenames");
                    if (fileNameList != null && selectedKey != null) {
                        fileNameList.setSelectedValue(selectedKey, true);
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
    } // ---FIN de metodo [setupListeners]---

    /**
     * Muestra un menú contextual en el grid del modo datos con la opción
     * de añadir/quitar la imagen del proyecto.
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
    } // ---FIN de metodo [showGridContextMenu]---

    private void setupTagManagementCallbacks() {
        TagManagementPanel tagPanel = registry.get("panel.datamode.tagmanagement");
        if (tagPanel == null) return;

        tagPanel.setOnRemoveTag(tag -> {
            List<Path> selectedPaths = getSelectedImagePaths();
            if (!selectedPaths.isEmpty()) {
                dataManager.removeTagFromImages(selectedPaths, tag);
                updateTagPanelSelection(); // Refrescar vista
                refreshTagTreeAndSelect(null); // Refrescar conteos en el árbol
                refreshAvailableTags(); // Refrescar el combo
            }
        });
    } // ---FIN de metodo [setupTagManagementCallbacks]---

    /**
     * Obtiene el Tag actualmente seleccionado en el árbol, o null si no hay selección o no es un Tag.
     */
    private Tag getSelectedTagFromTree() {
        JTree allTagsTree = registry.get("tree.datamode.alltags");
        if (allTagsTree == null) return null;
        TreePath selPath = allTagsTree.getSelectionPath();
        if (selPath == null) return null;
        Object node = selPath.getLastPathComponent();
        return (node instanceof Tag) ? (Tag) node : null;
    } // ---FIN de metodo [getSelectedTagFromTree]---

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
    } // ---FIN de metodo [refreshIntelliSense]---

    /**
     * Resuelve una ruta en notación punto manejando ambigüedad.
     * Si un segmento no se encuentra bajo su padre esperado pero existe
     * en múltiples ramas, muestra un diálogo para que el usuario elija.
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
    } // ---FIN de metodo [resolveWithAmbiguityDialog]---

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

        // Intentar resolución como ruta punto
        List<Tag> resolvedPath = resolveExistingPath(trimmed);
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
        JToggleButton btnHerencia = registry.get("toggle.datamode.tag.herencia");
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

                List<Tag> resolvedPath = resolveWithAmbiguityDialog(dotPath.trim(), btnAssign.getTopLevelAncestor());
                if (resolvedPath.isEmpty()) {
                    JOptionPane.showMessageDialog(btnAssign.getTopLevelAncestor(),
                        "No se pudo resolver la ruta: " + dotPath);
                    return;
                }

                List<Path> selectedPaths = getSelectedImagePaths();
                if (selectedPaths.isEmpty()) return;

                Tag leafTag = resolvedPath.get(resolvedPath.size() - 1);

                if (btnHerencia != null && btnHerencia.isSelected()) {
                    for (Tag tag : resolvedPath) {
                        dataManager.addTagToImages(selectedPaths, tag.getNombre());
                    }
                } else {
                    dataManager.addTagToImages(selectedPaths, leafTag.getNombre());
                }

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
            logger.debug("Grid y lista central actualizados con {} elementos.", gridListModel.getSize());
        });
    } // ---FIN de metodo loadImagesForTag---

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
            logger.debug("Grid y lista central actualizados por nombre '{}' con {} elementos.", tagName, gridListModel.getSize());
        });
    } // ---FIN de metodo loadImagesForTagName---

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
            logger.debug("Grid y lista central actualizados con todas las {} imágenes.", gridListModel.getSize());
        });
    } // ---FIN de metodo loadAllImages---

} // --- FIN de clase DataController ---