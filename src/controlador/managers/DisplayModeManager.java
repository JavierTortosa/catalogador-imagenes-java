package controlador.managers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ListCoordinator;
import controlador.commands.AppActionCommands;
import controlador.utils.ComponentRegistry;
import modelo.ListContext;
import modelo.VisorModel;
import modelo.VisorModel.DisplayMode;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.panels.GridDisplayPanel;
import vista.theme.Tema;
import vista.theme.ThemeChangeListener;
import vista.theme.ThemeManager;

/**
 * Gestor especializado en la lógica de transición y configuración 
 * de los diferentes Modos de Visualización (DisplayModes).
 */
public class DisplayModeManager implements ThemeChangeListener{

	private static final Logger logger = LoggerFactory.getLogger(DisplayModeManager.class);
	
    // --- Dependencias (Inyectadas) ---
	private Map<String, Action> actionMap;
    private VisorModel model;
    private ViewManager viewManager;
    private ComponentRegistry registry;
    private ListCoordinator listCoordinator;
    private ConfigurationManager configuration;
    private ThemeManager themeManager;
    private ToolbarManager toolbarManager;
    private ConfigApplicationManager configAppManager;
    private InfobarStatusManager infobarStatusManager;
    
    public DisplayModeManager() {} // --- Fin del Constructor ---


    /**
     * Se debe llamar a este método UNA VEZ después de que todas las dependencias
     * hayan sido inyectadas (por ejemplo, desde AppInitializer).
     * Conecta los listeners necesarios para la interactividad.
     */
    public void initializeListeners() {
        conectarGridListener();
    } // --- Fin del método initializeListeners ---

    
    /**
     * Añade un ListSelectionListener a la JList del grid para que notifique
     * al ListCoordinator cuando el usuario selecciona un elemento.
     */
    private void conectarGridListener() {
        JList<String> gridList = registry.get("list.grid");
        if (gridList == null) {
            logger.error("ERROR [DisplayModeManager]: No se pudo conectar el listener, 'list.grid' no encontrada.");
            return;
        }

        gridList.addListSelectionListener(e -> {
            // 'getValueIsAdjusting' es true mientras el usuario está arrastrando el ratón para seleccionar.
            // Solo nos interesa el evento final.
            if (!e.getValueIsAdjusting()) {
                // Prevenir que el listener actúe si la UI se está sincronizando desde otro sitio
                if (listCoordinator.isSincronizandoUI()) {
                    return;
                }

                int selectedIndex = gridList.getSelectedIndex();
                // Si hay algo seleccionado (no es -1), notificamos al coordinador.
                if (selectedIndex != -1) {
                    listCoordinator.seleccionarImagenPorIndice(selectedIndex);
                }
            }
        });
        logger.debug("[DisplayModeManager] Listener de selección conectado al Grid.");
    } // --- Fin del método conectarGridListener ---
    
    
    // =================================================================================
    // === Lógica Principal de Transición ===
    // =================================================================================

    
    public void switchToDisplayMode(DisplayMode newDisplayMode) {
        // <-- CAMBIO CLAVE 1: La guarda principal -->
        // Si no hay imágenes, no se debe cambiar el modo de visualización.
        // Esto protege la pantalla de bienvenida.
        if (model.getModeloLista() == null || model.getModeloLista().isEmpty()) {
            logger.debug("[DisplayModeManager] Transición a " + newDisplayMode + " OMITIDA. No hay imágenes cargadas.");
            return;
        }
        // <-- FIN DEL CAMBIO CLAVE 1 -->

        if (model.getCurrentDisplayMode() == newDisplayMode) {
            // No hacemos nada si ya estamos en ese modo
        }
        logger.debug("--- [DisplayModeManager] INICIANDO TRANSICIÓN -> " + newDisplayMode + " ---");
        
        this.model.setCurrentDisplayMode(newDisplayMode);
        
        if (configuration != null) {
            configuration.setString(ConfigKeys.COMPORTAMIENTO_DISPLAY_MODE_ULTIMO_USADO, newDisplayMode.name());
        }
        
        String containerKey = (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) 
                            ? "container.displaymodes.proyecto" 
                            : "container.displaymodes";
        
        String cardName = mapDisplayModeToCardLayoutKey(newDisplayMode);
        this.viewManager.cambiarAVista(containerKey, cardName);
        
        enterDisplayMode(newDisplayMode);
        
        if (toolbarManager != null) {
            toolbarManager.reconstruirContenedorDeToolbars(model.getCurrentWorkMode());
        }
        
        sincronizarEstadoBotonesDisplayMode();
        logger.debug("--- [DisplayModeManager] TRANSICIÓN COMPLETADA a " + newDisplayMode + " ---\n");
        
    } // --- Fin del método switchToDisplayMode ---
    
    
    private void enterDisplayMode(DisplayMode modo) {
        JScrollPane thumbnailScrollPane = registry.get("scroll.miniaturas");
        String thumbnailComponentId = "imagenes_en_miniatura";
        String configKey = ConfigKeys.menuState("vista", "imagenes_en_miniatura");

        switch (modo) {
            case GRID:
                logger.debug("  -> Configurando para MODO GRID: Ocultando barra de miniaturas.");
                if (thumbnailScrollPane != null && thumbnailScrollPane.isVisible()) {
                    viewManager.setComponentePrincipalVisible(thumbnailComponentId, false, configKey);
                    actualizarEstadoAccionToggle(AppActionCommands.CMD_VISTA_TOGGLE_THUMBNAILS, false);
                }
                poblarYSincronizarGrid();
                break;

            case SINGLE_IMAGE:
                 logger.debug("  -> Configurando para MODO SINGLE_IMAGE: Mostrando barra de miniaturas.");
                 if (thumbnailScrollPane != null && !thumbnailScrollPane.isVisible()) {
                     viewManager.setComponentePrincipalVisible(thumbnailComponentId, true, configKey);
                     actualizarEstadoAccionToggle(AppActionCommands.CMD_VISTA_TOGGLE_THUMBNAILS, true);
                 }
                break;

            case POLAROID:
                logger.debug("  -> Configurando para MODO POLAROID (en desarrollo)...");
                break;
        }
    } // --- Fin del método enterDisplayMode ---
    
    
    public void poblarYSincronizarGrid() {
        boolean isProjectMode = (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO);
        String gridPanelKey = isProjectMode ? "panel.display.grid.proyecto" : "panel.display.grid";
        String gridListKey = isProjectMode ? "list.grid.proyecto" : "list.grid";

        GridDisplayPanel gridPanel = registry.get(gridPanelKey);
        JList<String> gridList = registry.get(gridListKey);
    	
        if (gridPanel == null || gridList == null || model == null || registry == null) {
            logger.error("ERROR [DisplayModeManager]: Dependencias nulas para poblar grid.");
            return;
        }

        List<String> imageKeys;
        String selectedKey = null;
        int currentIndex = -1;

        if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            logger.debug("[DisplayModeManager] Poblando grid para el MODO PROYECTO.");
            String focoActual = model.getProyectoListContext().getNombreListaActiva();
            
            JList<String> listaFuenteUI = "descartes".equals(focoActual) 
                                        ? registry.get("list.proyecto.descartes") 
                                        : registry.get("list.proyecto.nombres");

            if (listaFuenteUI != null && listaFuenteUI.getModel() instanceof DefaultListModel) {
                DefaultListModel<String> sourceModel = (DefaultListModel<String>) listaFuenteUI.getModel();
                imageKeys = Collections.list(sourceModel.elements());
                
                int selectedIndexInSourceList = listaFuenteUI.getSelectedIndex();
                if (selectedIndexInSourceList != -1) {
                    selectedKey = sourceModel.getElementAt(selectedIndexInSourceList);
                    currentIndex = selectedIndexInSourceList;
                }
            } else {
                imageKeys = Collections.emptyList();
            }
        } else {
            logger.debug("[DisplayModeManager] Poblando grid para el MODO VISUALIZADOR/otro.");
            ListContext currentContext = model.getCurrentListContext();
            if (currentContext != null && currentContext.getModeloLista() != null) {
                DefaultListModel<String> sourceModel = currentContext.getModeloLista();
                imageKeys = Collections.list(sourceModel.elements());
                
                selectedKey = currentContext.getSelectedImageKey();
                if (selectedKey != null) {
                    currentIndex = sourceModel.indexOf(selectedKey);
                }
            } else {
                imageKeys = Collections.emptyList();
            }
        }

        gridPanel.setImageKeys(imageKeys);
        
        final int finalCurrentIndex = currentIndex;
        SwingUtilities.invokeLater(() -> {
            if (finalCurrentIndex >= 0 && finalCurrentIndex < gridList.getModel().getSize()) {
                gridList.setSelectedIndex(finalCurrentIndex);
                gridList.ensureIndexIsVisible(finalCurrentIndex);
            } else {
                gridList.clearSelection();
            }
        });
        
        logger.debug("[DisplayModeManager] Grid poblado con " + imageKeys.size() + " claves. Selección en índice: " + currentIndex);
        
    }// --- Fin del método poblarYSincronizarGrid ---
    
    
    private String mapDisplayModeToCardLayoutKey(DisplayMode displayMode) {
        switch (displayMode) {
            case SINGLE_IMAGE: return "VISTA_SINGLE_IMAGE";
            case GRID: return "VISTA_GRID";
            case POLAROID: return "VISTA_POLAROID";
            default: return "VISTA_SINGLE_IMAGE";
        }
    } // --- Fin del método mapDisplayModeToCardLayoutKey ---
    
    
    public void sincronizarEstadoBotonesDisplayMode() {
        if (actionMap == null || model == null || configAppManager == null) {
            logger.warn("WARN [DisplayModeManager]: No se puede sincronizar botones de modo, faltan dependencias.");
            return;
        }

        // <-- CAMBIO CLAVE 2: La guarda de sincronización -->
        // Si no hay imágenes, no hay nada que sincronizar.
        if (model.getModeloLista() == null || model.getModeloLista().isEmpty()) {
            return;
        }
        // <-- FIN DEL CAMBIO CLAVE 2 -->

        DisplayMode currentMode = model.getCurrentDisplayMode();
        
        List<String> displayModeCommands = List.of(
            AppActionCommands.CMD_VISTA_SINGLE,
            AppActionCommands.CMD_VISTA_GRID,
            AppActionCommands.CMD_VISTA_POLAROID
        );

        logger.debug("[DisplayModeManager] Sincronizando botones de DisplayMode. Activo: " + currentMode);

        for (String command : displayModeCommands) {
            Action action = actionMap.get(command);
            if (action != null) {
                boolean isSelected = command.equals(mapDisplayModeToActionCommand(currentMode));
                
                action.putValue(Action.SELECTED_KEY, isSelected);
                configAppManager.actualizarAspectoBotonToggle(action, isSelected);
            }
        }
        
    } // --- FIN del metodo sincronizarEstadoBotonesDisplayMode ---
    
    
    private String mapDisplayModeToActionCommand(DisplayMode displayMode) {
        if (displayMode == null) return "";
        switch (displayMode) {
            case SINGLE_IMAGE: return AppActionCommands.CMD_VISTA_SINGLE;
            case GRID:         return AppActionCommands.CMD_VISTA_GRID;
            case POLAROID:     return AppActionCommands.CMD_VISTA_POLAROID;
            default:           return "";
        }
    }// --- Fin del método mapDisplayModeToActionCommand ---
    
    
    private void actualizarEstadoAccionToggle(String commandKey, boolean isSelected) {
        Action action = actionMap.get(commandKey);
        if (action != null) {
            action.putValue(Action.SELECTED_KEY, isSelected);
        }
    } // --- Fin del método actualizarEstadoAccionToggle ---
    
    
    @Override
    public void onThemeChanged(Tema nuevoTema) {
        logger.debug("[DisplayModeManager] Notificación de cambio de tema recibida. Actualizando paneles de modo de visualización...");
        
        SwingUtilities.invokeLater(() -> {
            
            GridDisplayPanel gridPanel = registry.get("panel.display.grid");
            
            if (gridPanel != null) {
                gridPanel.actualizarColorDeFondoPorTema(this.themeManager);
            } else {
                logger.warn("  WARN [onThemeChanged]: No se encontró 'panel.display.grid' para actualizar.");
            }
        });
    } // --- FIN del método onThemeChanged ---
    

    // --- Setters para Inyección de Dependencias ---
    public void setModel(VisorModel model) { this.model = model; }
    public void setViewManager(ViewManager viewManager) { this.viewManager = viewManager; }
    public void setRegistry(ComponentRegistry registry) { this.registry = registry; }
    public void setListCoordinator(ListCoordinator listCoordinator) { this.listCoordinator = listCoordinator; }
    public void setActionMap(Map<String, Action> actionMap) { this.actionMap = actionMap; }
    public void setConfiguration(ConfigurationManager configuration) { this.configuration = configuration; }
    public void setInfobarStatusManager(InfobarStatusManager infobarStatusManager) {this.infobarStatusManager = infobarStatusManager;}
    public void setThemeManager(ThemeManager themeManager) {this.themeManager = themeManager;}
    public void setToolbarManager(ToolbarManager tm) { this.toolbarManager = tm; }
    public void setConfigApplicationManager(ConfigApplicationManager cam) { this.configAppManager = cam; }
    
} // --- Fin de la clase DisplayModeManager ---
    
    
