package controlador;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import controlador.managers.InfobarStatusManager;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.DataManager;
import controlador.managers.interfaces.IProjectManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import modelo.datos.Tag;
import servicios.db.TagDAO;
import vista.panels.GridDisplayPanel;
import vista.panels.TagManagementPanel;
import vista.tree.TagTreeModel;
import vista.tree.TagTreeCellRenderer;
import vista.panels.DriveListPanel;
import modelo.datos.Disco;
import java.util.Collections;

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
        
        initializeTagTree();
        initializeDriveList();
        setupTagManagementCallbacks();
        setupListeners();
        setupTagCRUDButtons();
        
        isInitialized = true;
    } // ---FIN de metodo [initialize]---
    
    /**
     * Activa el modo datos. Se llama cada vez que el usuario cambia a esta vista.
     * Recarga el árbol de tags para reflejar los últimos cambios en la BD.
     */
    public void activate() {
        logger.debug("Activando el Modo Datos...");
        
        // ¡LA SOLUCIÓN! Recargamos el árbol cada vez que entramos en este modo.
        initializeTagTree();
        refreshDriveList();
        refreshAvailableTags(); // Recargar la lista de tags del combo
        
        // Seleccionamos la raíz ("Biblioteca") por defecto para marcarla e indexar todas las imágenes en el grid
        JTree allTagsTree = registry.get("tree.datamode.alltags");
        if (allTagsTree != null) {
            allTagsTree.setSelectionRow(0);
        }
        
        TagManagementPanel tagPanel = registry.get("panel.datamode.tagmanagement");
        if (tagPanel != null) {
            tagPanel.clearPanel();
        }
    } // ---FIN de metodo [activate]---

    private void initializeTagTree() {
        JTree allTagsTree = registry.get("tree.datamode.alltags");
        if (allTagsTree == null) {
            logger.error("No se encontró 'tree.datamode.alltags' en el registro.");
            return;
        }

        // Creamos una instancia de nuestro nuevo modelo y se la asignamos al árbol.
        TagDAO dao = new TagDAO();
        TagTreeModel treeModel = new TagTreeModel(dao);
        allTagsTree.setModel(treeModel);
        
        // Asignamos el renderizador personalizado para ver los conteos.
        allTagsTree.setCellRenderer(new TagTreeCellRenderer(dao));
        
        logger.debug("JTree de etiquetas inicializado con TagTreeModel y TagTreeCellRenderer.");
    } // ---FIN de metodo [initializeTagTree]---

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

        dataManager.ensureAllDrivesRegistered(); // Escaneo proactivo al refrescar
        List<Disco> registered = dataManager.getAllRegisteredDisks();
        Map<String, Path> connected = dataManager.getConnectedDisks();

        SwingUtilities.invokeLater(() -> {
            drivePanel.updateDrives(registered, connected);
        });
    } // ---FIN de metodo [refreshDriveList]---

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
            if (!e.getValueIsAdjusting()) {
                String selectedKey = gridList.getSelectedValue();
                model.setSelectedImageKey(selectedKey);
                updateTagPanelSelection();
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
            javax.swing.JTextField txtTag = new javax.swing.JTextField();
            txtTag.setColumns(20);
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
                    initializeTagTree(); // Refrescar el árbol de tags
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
        
        popup.show(gridList, e.getX(), e.getY());
    } // ---FIN de metodo [showGridContextMenu]---

    private void setupTagManagementCallbacks() {
        TagManagementPanel tagPanel = registry.get("panel.datamode.tagmanagement");
        if (tagPanel == null) return;

        tagPanel.setOnAddTag(tagName -> {
            List<Path> selectedPaths = getSelectedImagePaths();
            if (!selectedPaths.isEmpty()) {
                dataManager.addTagToImages(selectedPaths, tagName);
                updateTagPanelSelection(); // Refrescar vista de tags de la imagen
                initializeTagTree(); // Refrescar el árbol de tags
                refreshAvailableTags(); // Refrescar el combo
            }
        });

        tagPanel.setOnRemoveTag(tag -> {
            List<Path> selectedPaths = getSelectedImagePaths();
            if (!selectedPaths.isEmpty()) {
                dataManager.removeTagFromImages(selectedPaths, tag);
                updateTagPanelSelection(); // Refrescar vista
                initializeTagTree(); // Refrescar conteos en el árbol
                refreshAvailableTags(); // Refrescar el combo
            }
        });

        tagPanel.setOnBrowseTags(() -> {
            java.awt.Frame mainFrame = (visorController != null) ? visorController.getView() : null;
            vista.util.IconUtils iconUtils = (visorController != null) ? visorController.getIconUtils() : null;
            vista.dialogos.TagSelectionDialog dialog = new vista.dialogos.TagSelectionDialog(mainFrame, iconUtils);
            String selected = dialog.showDialog();
            if (selected != null) {
                tagPanel.getComboNewTag().setSelectedItem(selected);
            }
        });
    } // ---FIN de metodo [setupTagManagementCallbacks]---

    /**
     * Configura los listeners para los botones CRUD de tags (Nuevo, Renombrar, Borrar).
     */
    private void setupTagCRUDButtons() {
        JButton btnNew = registry.get("btn.datamode.tag.new");
        JButton btnRename = registry.get("btn.datamode.tag.rename");
        JButton btnDelete = registry.get("btn.datamode.tag.delete");
        JTree allTagsTree = registry.get("tree.datamode.alltags");
        
        if (btnNew != null) {
            btnNew.addActionListener(e -> {
                // Determinar el padre: si hay un tag seleccionado en el árbol, será hijo de ese.
                Long parentId = null;
                if (allTagsTree != null) {
                    TreePath selectedPath = allTagsTree.getSelectionPath();
                    if (selectedPath != null && selectedPath.getLastPathComponent() instanceof Tag) {
                        Tag selectedTag = (Tag) selectedPath.getLastPathComponent();
                        parentId = selectedTag.getId();
                    }
                }
                
                String nombre = JOptionPane.showInputDialog(
                    btnNew.getTopLevelAncestor(),
                    parentId != null ? "Nombre del nuevo sub-tag:" : "Nombre del nuevo tag:",
                    "Crear Etiqueta",
                    JOptionPane.PLAIN_MESSAGE
                );
                
                if (nombre != null && !nombre.trim().isEmpty()) {
                    TagDAO dao = new TagDAO();
                    dao.addTag(nombre.trim(), parentId);
                    initializeTagTree();
                    refreshAvailableTags();
                    logger.info("Nuevo tag '{}' creado con parent_id={}", nombre.trim(), parentId);
                }
            });
        }
        
        if (btnRename != null) {
            btnRename.addActionListener(e -> {
                if (allTagsTree == null) return;
                TreePath selectedPath = allTagsTree.getSelectionPath();
                if (selectedPath == null || !(selectedPath.getLastPathComponent() instanceof Tag)) {
                    JOptionPane.showMessageDialog(btnRename.getTopLevelAncestor(),
                        "Selecciona primero un tag en el árbol para renombrarlo.",
                        "Sin selección", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                Tag selectedTag = (Tag) selectedPath.getLastPathComponent();
                String nuevoNombre = (String) JOptionPane.showInputDialog(
                    btnRename.getTopLevelAncestor(),
                    "Nuevo nombre para '" + selectedTag.getNombre() + "':",
                    "Renombrar Etiqueta",
                    JOptionPane.PLAIN_MESSAGE,
                    null, null, selectedTag.getNombre()
                );
                
                if (nuevoNombre != null && !nuevoNombre.trim().isEmpty()) {
                    TagDAO dao = new TagDAO();
                    boolean success = dao.updateTagName(selectedTag.getId(), nuevoNombre.trim());
                    if (success) {
                        initializeTagTree();
                        refreshAvailableTags();
                        updateTagPanelSelection();
                    } else {
                        JOptionPane.showMessageDialog(btnRename.getTopLevelAncestor(),
                            "No se pudo renombrar. Puede que ya exista un tag con ese nombre.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        }
        
        if (btnDelete != null) {
            btnDelete.addActionListener(e -> {
                if (allTagsTree == null) return;
                TreePath selectedPath = allTagsTree.getSelectionPath();
                if (selectedPath == null || !(selectedPath.getLastPathComponent() instanceof Tag)) {
                    JOptionPane.showMessageDialog(btnDelete.getTopLevelAncestor(),
                        "Selecciona primero un tag en el árbol para borrarlo.",
                        "Sin selección", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                Tag selectedTag = (Tag) selectedPath.getLastPathComponent();
                int confirm = JOptionPane.showConfirmDialog(
                    btnDelete.getTopLevelAncestor(),
                    "¿Eliminar la etiqueta '" + selectedTag.getNombre() + "'?\n" +
                    "Se eliminarán también todas las asociaciones con imágenes.\n" +
                    "Los sub-tags se reasignarán al padre.",
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                
                if (confirm == JOptionPane.YES_OPTION) {
                    TagDAO dao = new TagDAO();
                    dao.deleteTag(selectedTag.getId());
                    initializeTagTree();
                    refreshAvailableTags();
                    updateTagPanelSelection();
                    
                    // Limpiar el grid si se estaban mostrando imágenes del tag borrado
                    GridDisplayPanel gridPanel = registry.get("panel.datamode.grid");
                    if (gridPanel != null) {
                        gridPanel.getGridList().setModel(new DefaultListModel<>());
                    }
                    logger.info("Tag '{}' eliminado.", selectedTag.getNombre());
                }
            });
        }
    } // ---FIN de metodo [setupTagCRUDButtons]---

    /**
     * Recarga la lista de tags disponibles en el JComboBox del TagManagementPanel.
     */
    private void refreshAvailableTags() {
        TagManagementPanel tagPanel = registry.get("panel.datamode.tagmanagement");
        if (tagPanel != null) {
            List<Tag> allTags = dataManager.getAllTags();
            tagPanel.setAvailableTags(allTags);
        }
    } // ---FIN de metodo [refreshAvailableTags]---

    private List<Path> getSelectedImagePaths() {
        JList<String> gridList = registry.get("list.datamode.grid");
        List<String> selectedKeys = gridList.getSelectedValuesList();
        List<Path> paths = new ArrayList<>();
        Map<String, Path> pathMap = model.getRutaCompletaMap();
        
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
        // Obtenemos la JList del grid del Modo Datos usando su clave ÚNICA.
        JList<String> gridList = registry.get("list.datamode.grid");
        if (gridList == null) {
            logger.error("No se encontró 'list.datamode.grid' en el registro.");
            return;
        }

        // Obtenemos las rutas de las imágenes para el tag.
        List<String> imagePaths = dataManager.getImagePathsForTag(tag);
        
        // Preparamos el nuevo modelo de lista y el mapa de rutas.
        DefaultListModel<String> gridListModel = new DefaultListModel<>();
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
            gridPathMap.put(key, path);
        }
        
        // Le decimos al VisorModel global cuál es el mapa de rutas que el renderer del grid debe usar.
        model.getRutaCompletaMap().clear();
        model.getRutaCompletaMap().putAll(gridPathMap);

        // Actualizamos el modelo de la JList directamente.
        SwingUtilities.invokeLater(() -> {
            gridList.setModel(gridListModel);
            logger.debug("Grid del Modo Datos actualizado con {} elementos.", gridListModel.getSize());
        });
    } // ---FIN de metodo loadImagesForTag---

    private void loadAllImages() {
        JList<String> gridList = registry.get("list.datamode.grid");
        if (gridList == null) {
            logger.error("No se encontró 'list.datamode.grid' en el registro.");
            return;
        }

        List<String> imagePaths = dataManager.getAllImagePaths();
        
        DefaultListModel<String> gridListModel = new DefaultListModel<>();
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
            gridPathMap.put(key, path);
        }
        
        model.getRutaCompletaMap().clear();
        model.getRutaCompletaMap().putAll(gridPathMap);

        // Actualizamos el modelo de la JList directamente.
        SwingUtilities.invokeLater(() -> {
            gridList.setModel(gridListModel);
            logger.debug("Grid del Modo Datos actualizado con todas las {} imágenes.", gridListModel.getSize());
        });
    } // ---FIN de metodo loadAllImages---

} // --- FIN de clase DataController ---