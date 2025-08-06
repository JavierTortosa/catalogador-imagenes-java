package controlador.managers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

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
            System.err.println("ERROR [DisplayModeManager]: No se pudo conectar el listener, 'list.grid' no encontrada.");
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
        System.out.println("[DisplayModeManager] Listener de selección conectado al Grid.");
    } // --- Fin del método conectarGridListener ---
    
    
    // =================================================================================
    // === Lógica Principal de Transición ===
    // =================================================================================

    public void switchToDisplayMode(DisplayMode newDisplayMode) {
        if (model.getCurrentDisplayMode() == newDisplayMode) {
            return;
        }
        System.out.println("\n--- [DisplayModeManager] INICIANDO TRANSICIÓN -> " + newDisplayMode + " ---");
        
        this.model.setCurrentDisplayMode(newDisplayMode);
        
        if (configuration != null) {
            System.out.println("  -> Guardando nuevo DisplayMode en configuración: " + newDisplayMode.name());
            configuration.setString(ConfigKeys.COMPORTAMIENTO_DISPLAY_MODE_ULTIMO_USADO, newDisplayMode.name());
        }
        
        String cardName = mapDisplayModeToCardLayoutKey(newDisplayMode);
        this.viewManager.cambiarAVista("container.displaymodes", cardName);
        
        enterDisplayMode(newDisplayMode);
        
        // <<< LLAMADA CRUCIAL PARA APLICAR LA NUEVA LÓGICA DE VISIBILIDAD >>>
        if (toolbarManager != null) {
            toolbarManager.reconstruirContenedorDeToolbars(model.getCurrentWorkMode());
        }
        
        sincronizarEstadoBotonesDisplayMode();
        if (configuration != null) {
            configuration.setString(ConfigKeys.COMPORTAMIENTO_DISPLAY_MODE_ULTIMO_USADO, newDisplayMode.name());
        }
        System.out.println("--- [DisplayModeManager] TRANSICIÓN COMPLETADA a " + newDisplayMode + " ---\n");
    } // --- Fin del método switchToDisplayMode ---

    
    private void enterDisplayMode(DisplayMode modo) {
        JScrollPane thumbnailScrollPane = registry.get("scroll.miniaturas");
        String thumbnailComponentId = "imagenes_en_miniatura";

        switch (modo) {
            case GRID:
                System.out.println("  -> Configurando para MODO GRID...");
                if (thumbnailScrollPane != null && thumbnailScrollPane.isVisible()) {
                    String configKey = ConfigKeys.menuState("vista", "imagenes_en_miniatura");
                    viewManager.setComponentePrincipalVisible(thumbnailComponentId, false, configKey);
                    actualizarEstadoAccionToggle(AppActionCommands.CMD_VISTA_TOGGLE_THUMBNAILS, false);
                }
                poblarYSincronizarGrid();

                break;

            case SINGLE_IMAGE:
                 System.out.println("  -> Configurando para MODO SINGLE_IMAGE...");
                 if (thumbnailScrollPane != null && configuration != null) {
                     String configKey = ConfigKeys.menuState("vista", "imagenes_en_miniatura");
                     boolean shouldBeVisible = configuration.getBoolean(configKey, true);
                     if (!thumbnailScrollPane.isVisible() && shouldBeVisible) {
                         viewManager.setComponentePrincipalVisible(thumbnailComponentId, true, configKey);
                         actualizarEstadoAccionToggle(AppActionCommands.CMD_VISTA_TOGGLE_THUMBNAILS, true);
                     }
                 }
                 
                break;

            case POLAROID:
                System.out.println("  -> Configurando para MODO POLAROID (en desarrollo)...");
                
                break;
        }
    } // --- Fin del método enterDisplayMode ---
    

    public void poblarYSincronizarGrid() {
        GridDisplayPanel gridPanel = registry.get("panel.display.grid");
        JList<String> gridList = registry.get("list.grid");
        if (gridPanel == null || gridList == null) {
            System.err.println("ERROR [DisplayModeManager]: No se encontraron los componentes del grid.");
            return;
        }
        
        ListContext visualizerContext = model.getVisualizadorListContext();
        List<String> imageKeys = Collections.list(visualizerContext.getModeloLista().elements());
        gridPanel.setImageKeys(imageKeys);
        
        // Obtenemos el índice buscando la clave seleccionada en el modelo
        String selectedKey = visualizerContext.getSelectedImageKey();
        int currentIndex = (selectedKey != null) ? visualizerContext.getModeloLista().indexOf(selectedKey) : -1;

        SwingUtilities.invokeLater(() -> {
            if (currentIndex >= 0 && currentIndex < gridList.getModel().getSize()) {
                gridList.setSelectedIndex(currentIndex);
                gridList.ensureIndexIsVisible(currentIndex);
            }
        });
    } // --- Fin del método poblarYSincronizarGrid ---

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
            System.err.println("WARN [DisplayModeManager]: No se puede sincronizar botones de modo, faltan dependencias.");
            return;
        }

        DisplayMode currentMode = model.getCurrentDisplayMode();
        
        List<String> displayModeCommands = List.of(
            AppActionCommands.CMD_VISTA_SINGLE,
            AppActionCommands.CMD_VISTA_GRID,
            AppActionCommands.CMD_VISTA_POLAROID
        );

        System.out.println("[DisplayModeManager] Sincronizando botones de DisplayMode. Activo: " + currentMode);

        for (String command : displayModeCommands) {
            Action action = actionMap.get(command);
            if (action != null) {
                boolean isSelected = command.equals(mapDisplayModeToActionCommand(currentMode));
                
                // 1. Actualiza el estado LÓGICO en la Action
                // Esta línea hace lo mismo que tu `actualizarEstadoAccionToggle`
                action.putValue(Action.SELECTED_KEY, isSelected);
                
                // 2. Llama al "pintor" para actualizar el estado VISUAL del botón
                configAppManager.actualizarAspectoBotonToggle(action, isSelected);
            }
        }
        
    } // --- FIN del metodo sincronizarEstadoBotonesDisplayMode ---
    
    
//    public void sincronizarEstadoBotonesDisplayMode() {
//        if (actionMap == null || model == null) return;
//        DisplayMode currentMode = model.getCurrentDisplayMode();
//        actualizarEstadoAccionToggle(AppActionCommands.CMD_VISTA_SINGLE, currentMode == DisplayMode.SINGLE_IMAGE);
//        actualizarEstadoAccionToggle(AppActionCommands.CMD_VISTA_GRID, currentMode == DisplayMode.GRID);
//        actualizarEstadoAccionToggle(AppActionCommands.CMD_VISTA_POLAROID, currentMode == DisplayMode.POLAROID);
//    } // --- Fin del método sincronizarEstadoBotonesDisplayMode ---
    
    
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
        System.out.println("[DisplayModeManager] Notificación de cambio de tema recibida. Actualizando paneles de modo de visualización...");
        
        SwingUtilities.invokeLater(() -> {
            
            // Obtenemos el panel de la rejilla desde el registro.
            GridDisplayPanel gridPanel = registry.get("panel.display.grid");
            
            if (gridPanel != null) {
                // Llamamos al método que ya existe en GridDisplayPanel
                gridPanel.actualizarColorDeFondoPorTema(this.themeManager);
            } else {
                System.err.println("  WARN [onThemeChanged]: No se encontró 'panel.display.grid' para actualizar.");
            }
            
            // Si en el futuro tienes más paneles de modo de visualización (como Polaroid),
            // podrías añadir su lógica de actualización aquí también.
            
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