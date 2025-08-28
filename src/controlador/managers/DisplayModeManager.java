package controlador.managers;

import java.awt.CardLayout;
import java.util.Map;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ProjectController; // <<< IMPORT NECESARIO
import controlador.ProjectListCoordinator; // <<< IMPORT NECESARIO
import controlador.managers.interfaces.IListCoordinator;
import controlador.utils.ComponentRegistry;
import modelo.MasterListChangeListener;
import modelo.MasterSelectionChangeListener;
import modelo.VisorModel;
import modelo.VisorModel.DisplayMode;
import modelo.VisorModel.WorkMode; // <<< IMPORT NECESARIO
import servicios.ConfigurationManager;
import servicios.image.ThumbnailService;
import vista.theme.Tema;
import vista.theme.ThemeChangeListener;
import vista.theme.ThemeManager;

public class DisplayModeManager implements ThemeChangeListener, MasterListChangeListener, MasterSelectionChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(DisplayModeManager.class);
    
    // =========================================================================
    // === CAMPOS ORIGINALES RESTAURADOS (SIN SIMPLIFICAR) ===
    // =========================================================================
    private VisorModel model;
    private ViewManager viewManager;
    private ComponentRegistry registry;
    private IListCoordinator listCoordinator;
    private Map<String, Action> actionMap;
    private ConfigurationManager configuration;
    private ThemeManager themeManager;
    private ToolbarManager toolbarManager;
    private ConfigApplicationManager configApplicationManager;
    private ThumbnailService gridThumbnailService;
    private InfobarStatusManager infobarStatusManager;
    private ProjectController projectController; // Restaurado
    private ProjectListCoordinator projectListCoordinator; // Añadido para el grid de proyecto
    // =========================================================================

    // --- Estado ---
    private boolean isSyncingFromManager = false; // Flag para evitar bucles de eventos
    
    public DisplayModeManager() {
        logger.info("Iniciando DisplayModeManager");
    } // end of constructor

    public void initializeListeners() {
        // --- Listener para el grid del MODO VISUALIZADOR ---
        JList<String> gridListVisualizador = registry.get("list.grid");
        if (gridListVisualizador != null) {
            gridListVisualizador.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && !isSyncingFromManager) {
                    int selectedIndex = gridListVisualizador.getSelectedIndex();
                    if (selectedIndex != -1 && listCoordinator != null && selectedIndex != listCoordinator.getOfficialSelectedIndex()) {
                        logger.debug("[DisplayModeManager] Selección del usuario en Grid-Visualizador. Índice: {}. Notificando a ListCoordinator.", selectedIndex);
                        listCoordinator.seleccionarImagenPorIndice(selectedIndex);
                    }
                }
            });
            logger.debug(" -> Listener de selección añadido a 'list.grid' (Visualizador).");
        }

        // --- Listener para el grid del MODO PROYECTO ---
        JList<String> gridListProyecto = registry.get("list.grid.proyecto");
        if (gridListProyecto != null) {
            gridListProyecto.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && !isSyncingFromManager) {
                    int selectedIndex = gridListProyecto.getSelectedIndex();
                    if (selectedIndex != -1 && projectListCoordinator != null && selectedIndex != projectListCoordinator.getOfficialSelectedIndex()) {
                        logger.debug("[DisplayModeManager] Selección del usuario en Grid-Proyecto. Índice: {}. Notificando a ProjectListCoordinator.", selectedIndex);
                        projectListCoordinator.seleccionarImagenPorIndice(selectedIndex);
                    }
                }
            });
            logger.debug(" -> Listener de selección añadido a 'list.grid.proyecto'.");
        }

        // Registrar este manager como oyente de cambios en la lista maestra
        if (model != null) {
            model.addMasterListChangeListener(this);
        }
    } // end of initializeListeners

    public void switchToDisplayMode(DisplayMode newMode) {
        if (model.getCurrentDisplayMode() == newMode) {
            return;
        }
        
        logger.info("Cambiando a DisplayMode: {}", newMode);
        model.setCurrentDisplayMode(newMode);
        
        String containerKey = (model.getCurrentWorkMode() == WorkMode.PROYECTO) 
                            ? "container.displaymodes.proyecto" 
                            : "container.displaymodes";
                            
        logger.debug("  -> Actuando sobre el contenedor CardLayout: {}", containerKey);

        JPanel container = registry.get(containerKey);
        if (container == null) {
            logger.error("ERROR CRÍTICO: No se encontró el contenedor CardLayout con la clave '{}'", containerKey);
            return;
        }
        
        CardLayout cardLayout = (CardLayout) container.getLayout();
        JScrollPane thumbnailBar = registry.get("scroll.miniaturas");

        switch (newMode) {
            case SINGLE_IMAGE:
                cardLayout.show(container, "VISTA_SINGLE_IMAGE");
                if (thumbnailBar != null) thumbnailBar.setVisible(true);
                if (infobarStatusManager != null) infobarStatusManager.mostrarMensaje("Modo: Vista Individual");
                break;
            case GRID:
                if (model.getCurrentWorkMode() == WorkMode.PROYECTO && projectController != null) {
                    projectController.actualizarModeloPrincipalConListaDeProyectoActiva();
                } else {
                    poblarGridConModelo(model.getModeloLista());
                    sincronizarSeleccionGrid();
                }
                cardLayout.show(container, "VISTA_GRID");
                if (thumbnailBar != null) thumbnailBar.setVisible(false);
                if (infobarStatusManager != null) infobarStatusManager.mostrarMensaje("Modo: Parrilla de Miniaturas");
                break;
            case POLAROID:
                cardLayout.show(container, "VISTA_POLAROID");
                if (thumbnailBar != null) thumbnailBar.setVisible(false);
                if (infobarStatusManager != null) infobarStatusManager.mostrarMensaje("Modo: Polaroid (En desarrollo)");
                break;
        }

        sincronizarBotonesDeModo();
        container.revalidate();
        container.repaint();
    } // end of switchToDisplayMode
    
    public void poblarGridConModelo(DefaultListModel<String> modelToShow) {
        if (modelToShow == null) {
            modelToShow = new DefaultListModel<>();
        }
        
        JList<String> gridList = getActiveGridList();
        
        if (gridList == null) {
            logger.error("No se puede poblar la parrilla, no se encontró una JList de grid activa en el registro para el modo {}.", model.getCurrentWorkMode());
            return;
        }
        
        gridList.setModel(modelToShow);
        logger.debug("Parrilla del modo {} poblada con un modelo de {} elementos.", model.getCurrentWorkMode(), modelToShow.getSize());
    }

    public void sincronizarSeleccionGrid() {
        isSyncingFromManager = true;
        SwingUtilities.invokeLater(() -> {
            try {
                JList<String> gridList = getActiveGridList();
                if (gridList == null) return;
                
                int masterIndex = -1;
                if (model.getCurrentWorkMode() == WorkMode.PROYECTO && projectListCoordinator != null) {
                    masterIndex = projectListCoordinator.getOfficialSelectedIndex();
                } else if (listCoordinator != null) {
                    masterIndex = listCoordinator.getOfficialSelectedIndex();
                }
                
                if (masterIndex >= 0 && masterIndex < gridList.getModel().getSize()) {
                    if (gridList.getSelectedIndex() != masterIndex) {
                        gridList.setSelectedIndex(masterIndex);
                    }
                    gridList.ensureIndexIsVisible(masterIndex);
                } else {
                    gridList.clearSelection();
                }
            } finally {
                isSyncingFromManager = false;
            }
        });
    } // end of sincronizarSeleccionGrid

    private void sincronizarBotonesDeModo() {
        if (actionMap == null) return;
        for (Action action : actionMap.values()) {
            if (action instanceof controlador.actions.displaymode.SwitchDisplayModeAction) {
                ((controlador.actions.displaymode.SwitchDisplayModeAction) action).updateSelectedState(model);
            }
        }
    } // end of sincronizarBotonesDeModo
    
    public void sincronizarEstadoBotonesDisplayMode() {
        sincronizarBotonesDeModo();
    } // end of sincronizarEstadoBotonesDisplayMode

    @Override
    public void onThemeChanged(Tema nuevoTema) {
        JList<String> gridListVis = registry.get("list.grid");
        if (gridListVis != null) {
            gridListVis.setBackground(nuevoTema.colorFondoSecundario());
            gridListVis.repaint();
        }
        JList<String> gridListProy = registry.get("list.grid.proyecto");
        if (gridListProy != null) {
            gridListProy.setBackground(nuevoTema.colorFondoSecundario());
            gridListProy.repaint();
        }
    } // end of onThemeChanged
    
    @Override
    public void onMasterListChanged(DefaultListModel<String> newMasterList, Object source) {
        if (model.getCurrentDisplayMode() == DisplayMode.GRID) {
            logger.debug("[DisplayModeManager] Notificado de cambio en lista maestra. Repoblando la parrilla...");
            poblarGridConModelo(newMasterList);
            sincronizarSeleccionGrid();
        }
    } // end of onMasterListChanged
    
    @Override
    public void onMasterSelectionChanged(int newMasterIndex, Object source) {
        if (source != this && model.getCurrentDisplayMode() == DisplayMode.GRID) {
            logger.debug("[DisplayModeManager] Notificado de cambio de selección maestra (desde {}). Sincronizando grid...", source != null ? source.getClass().getSimpleName() : "null");
            sincronizarSeleccionGrid();
        }
    } // end of onMasterSelectionChanged
    
    private JList<String> getActiveGridList() {
        if (model == null || registry == null) return null;
        return (model.getCurrentWorkMode() == WorkMode.PROYECTO)
             ? registry.get("list.grid.proyecto")
             : registry.get("list.grid");
    } // end of getActiveGridList

    // --- Setters para Inyección de Dependencias (RESTAURADOS Y COMPLETOS) ---
    public void setModel(VisorModel model) { this.model = model; }
    public void setViewManager(ViewManager viewManager) { this.viewManager = viewManager; }
    public void setRegistry(ComponentRegistry registry) { this.registry = registry; }
    public void setListCoordinator(IListCoordinator listCoordinator) { this.listCoordinator = listCoordinator; }
    public void setActionMap(Map<String, Action> actionMap) { this.actionMap = actionMap; }
    public void setConfiguration(ConfigurationManager configuration) { this.configuration = configuration; }
    public void setThemeManager(ThemeManager themeManager) { this.themeManager = themeManager; }
    public void setToolbarManager(ToolbarManager toolbarManager) { this.toolbarManager = toolbarManager; }
    public void setConfigApplicationManager(ConfigApplicationManager configApplicationManager) { this.configApplicationManager = configApplicationManager; }
    public void setGridThumbnailService(ThumbnailService gridThumbnailService) { this.gridThumbnailService = gridThumbnailService; }
    public void setInfobarStatusManager(InfobarStatusManager infobarStatusManager) { this.infobarStatusManager = infobarStatusManager; }
    public void setProjectController(ProjectController projectController) { this.projectController = projectController; }
    public void setProjectListCoordinator(ProjectListCoordinator projectListCoordinator) { this.projectListCoordinator = projectListCoordinator; }

} // end of class