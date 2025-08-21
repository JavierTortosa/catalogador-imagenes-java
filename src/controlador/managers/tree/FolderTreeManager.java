package controlador.managers.tree;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.GeneralController;
import modelo.ListContext;
import modelo.VisorModel;

public class FolderTreeManager {

    private static final Logger logger = LoggerFactory.getLogger(FolderTreeManager.class);

    private final VisorModel model;
    private final GeneralController generalController;
    private JTree folderTree;
    private FileSystemTreeModel treeModel;
    private boolean isSyncing = false;

    public FolderTreeManager(VisorModel model, GeneralController generalController) {
        this.model = Objects.requireNonNull(model);
        this.generalController = Objects.requireNonNull(generalController);
    } // --- Fin del método FolderTreeManager (constructor) ---

    public JPanel crearPanelDelArbol() {
        logger.debug("[FolderTreeManager] Creando el panel del árbol de carpetas...");
        
        treeModel = new FileSystemTreeModel();
        folderTree = new JTree(treeModel);
        folderTree.setCellRenderer(new FileTreeCellRenderer());
        
        folderTree.addMouseListener(new TreeMouseListener());
        
        folderTree.addTreeSelectionListener(e -> {
            if (isSyncing) return;
        });
        
        JScrollPane scrollPane = new JScrollPane(folderTree);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    } // --- Fin del método crearPanelDelArbol ---
    
    public void sincronizarArbolConCarpeta(Path carpetaActual) {
        if (carpetaActual == null) {
            folderTree.clearSelection();
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            isSyncing = true;
            try {
                List<Object> pathComponents = new ArrayList<>();
                pathComponents.add(treeModel.getRoot());

                Path rootPath = carpetaActual.getRoot();
                if (rootPath != null) {
                    pathComponents.add(rootPath.toFile());
                }
                
                // Construimos la ruta parte por parte para expandir el árbol
                Path incrementalPath = rootPath;
                for (Path part : carpetaActual.subpath(0, carpetaActual.getNameCount())) {
                    if (incrementalPath != null) {
                        incrementalPath = incrementalPath.resolve(part.toString());
                        pathComponents.add(incrementalPath.toFile());
                    }
                }

                TreePath treePath = new TreePath(pathComponents.toArray());
                folderTree.expandPath(treePath);
                folderTree.setSelectionPath(treePath);
                folderTree.scrollPathToVisible(treePath);
                
            } finally {
                isSyncing = false;
            }
        });
    } // --- Fin del método sincronizarArbolConCarpeta ---
    
    private class TreeMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                handleDoubleClick();
            }
        } // --- Fin del método mouseClicked ---

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        } // --- Fin del método mousePressed ---

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        } // --- Fin del método mouseReleased ---
    } // --- FIN DE LA CLASE TreeMouseListener ---

    private void handleDoubleClick() {
        TreePath selectedPath = folderTree.getSelectionPath();
        if (selectedPath == null) return;
        
        File selectedFile = (File) selectedPath.getLastPathComponent();
        if (selectedFile.isDirectory()) {
            logger.debug("[FolderTreeManager] Doble clic detectado en: " + selectedFile.toPath());
            handleOpenFolderAction();
        }
    } // --- Fin del método handleDoubleClick ---

    private void showContextMenu(MouseEvent e) {
        TreePath path = folderTree.getPathForLocation(e.getX(), e.getY());
        if (path == null) return;

        folderTree.setSelectionPath(path);
        File selectedFile = (File) path.getLastPathComponent();

        if (selectedFile.isDirectory()) {
            JPopupMenu menu = generalController.crearMenuContextualParaArbol();
            menu.show(folderTree, e.getX(), e.getY());
        }
    } // --- Fin del método showContextMenu ---

    
    public void handleOpenFolderAction() {
        TreePath selectedPath = folderTree.getSelectionPath();
        if (selectedPath == null) return;
        File selectedFile = (File) selectedPath.getLastPathComponent();
        
        logger.debug("[FolderTreeManager] Acción: Abrir aquí (Limpiar Historial) en: " + selectedFile.toPath());
        
        model.getCurrentListContext().getHistorialNavegacion().clear();
        
        generalController.solicitarCargaDesdeNuevaRaiz(selectedFile.toPath());
        
        // ***** INICIO DE LA MODIFICACIÓN 2 *****
        // Después de solicitar la recarga de la lista, forzamos el cambio de la
        // interfaz a la pestaña "Lista" para que el usuario vea el resultado de su acción.
        SwingUtilities.invokeLater(() -> {
            javax.swing.JTabbedPane tabbedPane = generalController.getRegistry().get("tabbedpane.izquierdo");
            if (tabbedPane != null) {
                if (tabbedPane.getSelectedIndex() != 0) {
                    tabbedPane.setSelectedIndex(0);
                    logger.debug("  -> Interfaz cambiada programáticamente a la pestaña 'Lista' (índice 0).");
                }
            } else {
                logger.warn("[FolderTreeManager] No se pudo encontrar 'tabbedpane.izquierdo' en el registro para cambiar de pestaña.");
            }
        });
        // ***** FIN DE LA MODIFICACIÓN 2 *****
    } // --- Fin del método handleOpenFolderAction ---
    
    
    public void handleDrillDownFolderAction() {
        TreePath selectedPath = folderTree.getSelectionPath();
        if (selectedPath == null) return;
        File selectedFile = (File) selectedPath.getLastPathComponent();
        
        logger.debug("[FolderTreeManager] Acción: Entrar aquí (Guardar Historial) en: " + selectedFile.toPath());

        Path carpetaActual = model.getCarpetaRaizActual();
        String claveSeleccionada = model.getSelectedImageKey();

        model.getCurrentListContext().getHistorialNavegacion().push(
            new ListContext.NavigationState(carpetaActual, claveSeleccionada)
        );
        
        generalController.solicitarCargaDesdeNuevaRaiz(selectedFile.toPath());
    } // --- Fin del método handleDrillDownFolderAction ---
    
} // --- FIN DE LA CLASE FolderTreeManager ---

