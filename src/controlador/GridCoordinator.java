package controlador;

import java.awt.Point;
import java.awt.Rectangle;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.MasterListChangeListener; // <<< AÑADIR IMPORT
import modelo.VisorModel;
import servicios.image.ThumbnailService;

/**
 * Gestor especializado en la lógica de "ventana deslizante" para una JList
 * configurada como un grid. Reacciona a los cambios en la lista maestra.
 */
public class GridCoordinator implements MasterListChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(GridCoordinator.class);

    // --- Componentes y Modelos ---
    private DefaultListModel<String> fullMasterModel; // El modelo completo con TODAS las claves
    private DefaultListModel<String> gridViewModel;   // El modelo de la JList del grid (la "ventana")
    private JList<String> gridList;
    private JScrollPane gridScrollPane;
    private VisorModel visorModel; // Para obtener las rutas completas

    // --- Servicios ---
    private ThumbnailService gridThumbnailService; // El servicio de caché DEDICADO para este grid

    // --- Estado Interno de la Ventana ---
    private int firstVisibleIndexInMaster = -1;
    private int lastVisibleIndexInMaster = -1;
    private int currentNumColumns = 0;
    
    // --- Flags de Control ---
    private boolean isCoordinatorEnabled = false;
    private boolean isUpdatingFromScroll = false;
    private boolean isExternalSelection = false;
    
    // --- Parámetros de Precarga ---
    private static final int PRELOAD_ROWS = 3;

    // --- Gestión de Hilos ---
    private ExecutorService precacheExecutor;
    private Future<?> lastPrecacheTask = null;

    /**
     * Constructor del coordinador del grid.
     * @param gridList La JList que actúa como grid.
     * @param gridScrollPane El JScrollPane que contiene la JList.
     * @param visorModel El modelo principal de la aplicación.
     * @param gridThumbnailService El servicio de miniaturas dedicado para este grid.
     */
    public GridCoordinator(JList<String> gridList, JScrollPane gridScrollPane, VisorModel visorModel, ThumbnailService gridThumbnailService) {
        this.gridList = gridList;
        this.gridScrollPane = gridScrollPane;
        this.visorModel = visorModel;
        this.gridThumbnailService = gridThumbnailService;
        
        if (gridList.getModel() instanceof DefaultListModel) {
            this.gridViewModel = (DefaultListModel<String>) gridList.getModel();
        } else {
            throw new IllegalArgumentException("La JList del grid debe usar un DefaultListModel.");
        }

        this.gridScrollPane.getViewport().addChangeListener(new ViewportChangeListener());
        logger.debug("[GridCoordinator] Creado y listener de scroll conectado.");
    } // --- Fin del constructor GridCoordinator ---

    // =========================================================================
    // === MÉTODO DE LA INTERFAZ ===
    // =========================================================================
    
    @Override
    public void onMasterListChanged(DefaultListModel<String> newMasterList, Object source) {
        logger.debug("[GridCoordinator] Notificación de cambio de lista maestra recibida.");
        // Si el coordinador está activo, se resetea con la nueva lista.
        if (isCoordinatorEnabled) {
            // Limpiamos el caché ANTES de conectar a la nueva lista.
            if (this.gridThumbnailService != null) {
                this.gridThumbnailService.limpiarCache();
            }
            this.fullMasterModel = newMasterList;
            // Reseteamos los índices de la ventana
            this.firstVisibleIndexInMaster = -1;
            this.lastVisibleIndexInMaster = -1;
            // Forzamos un refresco completo de la vista.
            updateGridView();
        }
    } // --- Fin del método onMasterListChanged ---
    
    // =========================================================================

    /**
     * Activa o desactiva el coordinador. Cuando se activa, se conecta a una lista maestra.
     * Cuando se desactiva, limpia el grid y deja de escuchar eventos.
     * @param enable true para activar, false para desactivar.
     */
    public void setEnabled(boolean enable) {
        if (this.isCoordinatorEnabled == enable) return; // No hacer nada si ya está en ese estado

        this.isCoordinatorEnabled = enable;
        if (enable) {
            this.precacheExecutor = Executors.newSingleThreadExecutor();
            this.fullMasterModel = visorModel.getModeloLista(); // Coge la lista actual del modelo al activarse
            logger.debug("[GridCoordinator] HABILITADO. Conectado a una lista maestra de " + (fullMasterModel != null ? fullMasterModel.size() : 0) + " elementos.");
            updateGridView();
        } else {
            if (this.precacheExecutor != null) {
                this.precacheExecutor.shutdownNow();
            }
            this.fullMasterModel = null;
            if (this.gridViewModel != null) {
                this.gridViewModel.clear();
            }
            logger.debug("[GridCoordinator] DESHABILITADO. Grid limpiado y ejecutor detenido.");
        }
    } // --- Fin del método setEnabled ---

    // El resto de la clase permanece igual por ahora
    
    private void updateGridView() {
        if (!isCoordinatorEnabled || fullMasterModel == null || fullMasterModel.isEmpty() || gridList.getWidth() == 0) {
            if (gridViewModel != null && !gridViewModel.isEmpty()) gridViewModel.clear();
            return;
        }

        int cellWidth = gridList.getFixedCellWidth();
        currentNumColumns = (cellWidth > 0) ? Math.max(1, gridList.getWidth() / cellWidth) : 1;

        Rectangle viewRect = gridScrollPane.getViewport().getViewRect();
        int firstVisibleListIndex = gridList.locationToIndex(viewRect.getLocation());
        
        if (firstVisibleListIndex == -1) {
            firstVisibleListIndex = 0;
        }

        int currentMasterIndexOffset = (this.firstVisibleIndexInMaster > 0) ? this.firstVisibleIndexInMaster : 0;
        int newFirstMasterIndex = currentMasterIndexOffset + firstVisibleListIndex;

        int firstIndexToLoad = Math.max(0, newFirstMasterIndex - (PRELOAD_ROWS * currentNumColumns));
        
        int visibleRows = (gridScrollPane.getViewport().getHeight() / Math.max(1, gridList.getFixedCellHeight())) + 1;
        int numCellsToLoad = (visibleRows + (PRELOAD_ROWS * 2)) * currentNumColumns;
        int lastIndexToLoad = Math.min(fullMasterModel.getSize() - 1, firstIndexToLoad + numCellsToLoad);
        
        if (firstIndexToLoad == this.firstVisibleIndexInMaster && lastIndexToLoad == this.lastVisibleIndexInMaster) {
            return;
        }
        
        logger.trace("[GridCoordinator] Actualizando ventana. Master Índices: [" + firstIndexToLoad + " - " + lastIndexToLoad + "]");
        
        this.firstVisibleIndexInMaster = firstIndexToLoad;
        this.lastVisibleIndexInMaster = lastIndexToLoad;

        List<String> keysForView = new ArrayList<>();
        List<Path> pathsForCache = new ArrayList<>();
        for (int i = firstIndexToLoad; i <= lastIndexToLoad; i++) {
            String key = fullMasterModel.getElementAt(i);
            keysForView.add(key);
            pathsForCache.add(visorModel.getRutaCompleta(key));
        }

        precacheThumbnailsAsync(pathsForCache);
        
        gridViewModel.clear();
        gridViewModel.addAll(keysForView);

    } // --- Fin del método updateGridView ---
    
    /**
     * Sincroniza la vista del grid para mostrar y seleccionar un índice maestro.
     * Mantiene la lógica original de scroll por píxeles, pero asegura que la selección
     * ocurra DESPUÉS de que la vista se haya actualizado.
     * @param masterIndex El índice en la lista maestra a seleccionar.
     */
    public void syncSelectionFromMaster(int masterIndex) {
        if (!isCoordinatorEnabled || masterIndex < 0 || (fullMasterModel != null && masterIndex >= fullMasterModel.getSize())) {
            if(gridList != null) gridList.clearSelection();
            return;
        }
        
        isExternalSelection = true;

        // 1. Calculamos el número de columnas actual. Mantenemos tu lógica.
        int cellWidth = gridList.getFixedCellWidth();
        currentNumColumns = (cellWidth > 0) ? Math.max(1, gridList.getWidth() / cellWidth) : 1;
        
        // 2. Calculamos la posición Y de destino. Mantenemos tu lógica.
        int cellHeight = gridList.getFixedCellHeight();
        int row = masterIndex / Math.max(1, currentNumColumns);
        int targetY = row * cellHeight;
        
        JViewport viewport = gridScrollPane.getViewport();
        Rectangle viewRect = viewport.getViewRect();
        
        // 3. Comprobamos si es necesario mover la vista. Mantenemos tu lógica.
        if (targetY < viewRect.y || targetY > viewRect.y + viewRect.height - cellHeight) {
            // El índice está fuera de la pantalla. Hay que mover Y recargar.
            viewport.setViewPosition(new Point(0, targetY));
            
            // EL CAMBIO CLAVE:
            // En lugar de un invokeLater ciego, forzamos la actualización de la vista
            // AHORA MISMO, y luego, en un invokeLater, realizamos la selección.
            // Esto asegura que cuando se ejecute la selección, el modelo ya esté actualizado.
            updateGridView(); 
        }

        // 4. Realizamos la selección en el hilo de UI.
        //    Esta parte ahora se ejecutará siempre con el modelo de datos correcto.
        SwingUtilities.invokeLater(() -> {
            int relativeIndex = masterIndex - this.firstVisibleIndexInMaster;
            if (gridViewModel != null && relativeIndex >= 0 && relativeIndex < gridViewModel.getSize()) {
                if (gridList.getSelectedIndex() != relativeIndex) {
                    gridList.setSelectedIndex(relativeIndex);
                }
            }
            // Marcamos la selección externa como finalizada DESPUÉS de la operación.
            isExternalSelection = false;
        });
    } // --- Fin del método syncSelectionFromMaster ---
    
    private void precacheThumbnailsAsync(List<Path> paths) {
        if (gridThumbnailService == null || paths.isEmpty() || precacheExecutor == null || precacheExecutor.isShutdown()) return;

        if (lastPrecacheTask != null && !lastPrecacheTask.isDone()) {
            lastPrecacheTask.cancel(true);
        }

        this.lastPrecacheTask = precacheExecutor.submit(() -> {
            try {
                for (Path p : paths) {
                    if (Thread.currentThread().isInterrupted()) {
                        logger.debug("[GridCoordinator] Tarea de precarga cancelada, deteniendo procesamiento.");
                        return; // Salir del bucle si nos han cancelado.
                    }
                    if (p != null) {
                        String key = p.getFileName().toString();
                        // La llamada que ya tenías para crear la miniatura
                        gridThumbnailService.obtenerOCrearMiniatura(p, key, 128, 128, true);
                    }
                }

                // === INICIO DE LA NUEVA LÓGICA ===
                // Si la tarea ha llegado hasta aquí sin ser cancelada,
                // significa que la ventana actual ha sido cacheada.
                // Forzamos un repintado de la lista en el hilo de UI.
                if (!Thread.currentThread().isInterrupted()) {
                    SwingUtilities.invokeLater(() -> {
                        if (gridList != null) {
                            gridList.repaint();
                            logger.trace("[GridCoordinator] Repintado forzado tras precarga completada.");
                        }
                    });
                }
                // === FIN DE LA NUEVA LÓGICA ===

            } catch (Exception e) {
                // Capturamos cualquier excepción inesperada dentro del hilo
                logger.error("Error en la tarea de precarga de miniaturas", e);
            }
        });
    } // --- Fin del método precacheThumbnailsAsync ---
    
    public int getSelectedMasterIndex() {
        int selectedInView = gridList.getSelectedIndex();
        if (selectedInView == -1) {
            return -1;
        }
        return this.firstVisibleIndexInMaster + selectedInView;
    } // --- Fin del método getSelectedMasterIndex ---

    private class ViewportChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            if (isCoordinatorEnabled && !isExternalSelection) {
                isUpdatingFromScroll = true;
                updateGridView();
                SwingUtilities.invokeLater(() -> isUpdatingFromScroll = false);
            }
        }
    } // --- Fin de la clase interna ViewportChangeListener ---

    public boolean isUpdatingFromScroll() {
        return isUpdatingFromScroll;
    } // --- Fin del método isUpdatingFromScroll ---

    public boolean isExternalSelection() {
        return isExternalSelection;
    } // --- Fin del método isExternalSelection ---

} // --- Fin de la clase GridCoordinator ---
