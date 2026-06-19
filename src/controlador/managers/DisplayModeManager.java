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

import controlador.ProjectController;
import controlador.ProjectListCoordinator;
import controlador.managers.interfaces.IListCoordinator;
import controlador.managers.interfaces.IZoomManager;
import controlador.utils.ComponentRegistry;
import modelo.MasterListChangeListener;
import modelo.MasterSelectionChangeListener;
import modelo.VisorModel;
import modelo.VisorModel.DisplayMode;
import modelo.VisorModel.WorkMode; // <<< IMPORT NECESARIO
import vista.theme.Tema;
import vista.theme.ThemeChangeListener;
import vista.panels.PolaroidDisplayPanel;

public class DisplayModeManager implements ThemeChangeListener, MasterListChangeListener, MasterSelectionChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(DisplayModeManager.class);
    
    // =========================================================================
    // === CAMPOS ORIGINALES RESTAURADOS (SIN SIMPLIFICAR) ===
    // =========================================================================
    private VisorModel model;
    private ComponentRegistry registry;
    private IListCoordinator listCoordinator;
    private Map<String, Action> actionMap;
    
    private InfobarStatusManager infobarStatusManager;
    private ProjectController projectController; 
    private ProjectListCoordinator projectListCoordinator; 
    private IZoomManager zoomManager;
    private controlador.managers.ToolbarManager toolbarManager;


    
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
                    if (gridListVisualizador.getSelectedIndices().length > 1) return;
                    int selectedIndex = gridListVisualizador.getLeadSelectionIndex();
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
                    if (gridListProyecto.getSelectedIndices().length > 1) return;
                    int selectedIndex = gridListProyecto.getLeadSelectionIndex();
                    if (selectedIndex != -1 && projectListCoordinator != null && selectedIndex != projectListCoordinator.getOfficialSelectedIndex()) {
                        logger.debug("[DisplayModeManager] Selección del usuario en Grid-Proyecto. Índice: {}. Notificando a ProjectListCoordinator.", selectedIndex);
                        projectListCoordinator.seleccionarImagenPorIndice(selectedIndex);
                    }
                }
            });
            logger.debug(" -> Listener de selección añadido a 'list.grid.proyecto' (Proyecto).");
        }

        // Registrar este manager como oyente de cambios en la lista maestra
        if (model != null) {
            model.addMasterListChangeListener(this);
        }
    } // end of initializeListeners

    public void switchToDisplayMode(DisplayMode newMode) {
        // Eliminamos el early return (if current == newMode return) para garantizar 
        // que el CardLayout se actualice correctamente al cambiar de WorkMode,
        // ya que cada WorkMode tiene su propio contenedor físico.
        
        logger.info("Cambiando a DisplayMode: {}", newMode);
        model.setCurrentDisplayMode(newMode);
        
        if (model.getCurrentWorkMode() == WorkMode.CLIENTE) {
            logger.info("El modo {} no soporta cambios de CardLayout de DisplayMode. Ignorando.", model.getCurrentWorkMode());
            sincronizarBotonesDeModo();
            return;
        }
        
        String containerKey = (model.getCurrentWorkMode() == WorkMode.PROYECTO) 
                            ? "container.displaymodes.proyecto" 
                            : (model.getCurrentWorkMode() == WorkMode.DATOS)
                            ? "container.displaymodes.datos"
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
                if (model.getCurrentWorkMode() == WorkMode.DATOS && model.getSelectedImageKey() != null) {
                    java.nio.file.Path ruta = model.getRutaCompleta(model.getSelectedImageKey());
                    if (ruta != null && java.nio.file.Files.exists(ruta)) {
                        javax.swing.SwingWorker<java.awt.image.BufferedImage, Void> worker = new javax.swing.SwingWorker<>() {
                            @Override
                            protected java.awt.image.BufferedImage doInBackground() throws Exception {
                                return javax.imageio.ImageIO.read(ruta.toFile());
                            }
                            @Override
                            protected void done() {
                                try {
                                    model.setCurrentImage(get());
                                    vista.panels.ImageDisplayPanel singlePanel = registry.get("panel.datamode.display");
                                    if (singlePanel != null) singlePanel.repaint();
                                    if (zoomManager != null) {
                                        zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
                                    }
                                } catch (Exception ex) {
                                    logger.error("Error cargando imagen single al cambiar de modo", ex);
                                }
                            }
                        };
                        worker.execute();
                    }
                }
                if (thumbnailBar != null) thumbnailBar.setVisible(true);
                if (infobarStatusManager != null) infobarStatusManager.mostrarMensaje("Modo: Vista Individual");
                break;
            case GRID:
                if (model.getCurrentWorkMode() == WorkMode.PROYECTO && projectController != null) {
                    projectController.actualizarModeloPrincipalConListaDeProyectoActiva();
                } else if (model.getCurrentWorkMode() == WorkMode.DATOS) {
                    sincronizarSeleccionGrid();
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
                
                if (model.getCurrentWorkMode() == WorkMode.DATOS && model.getSelectedImageKey() != null) {
                    java.nio.file.Path ruta = model.getRutaCompleta(model.getSelectedImageKey());
                    if (ruta != null && java.nio.file.Files.exists(ruta)) {
                        javax.swing.SwingWorker<java.awt.image.BufferedImage, Void> worker = new javax.swing.SwingWorker<>() {
                            @Override
                            protected java.awt.image.BufferedImage doInBackground() throws Exception {
                                return javax.imageio.ImageIO.read(ruta.toFile());
                            }
                            @Override
                            protected void done() {
                                try {
                                    model.setCurrentImage(get());
                                    actualizarPanelPolaroidActivo();
                                    vista.panels.PolaroidDisplayPanel polaroidPanel = registry.get("panel.datamode.display.polaroid");
                                    if (polaroidPanel != null) {
                                        polaroidPanel.getImagePanel().repaint();
                                        if (zoomManager != null) {
                                            zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
                                        }
                                    }
                                } catch (Exception ex) {
                                    logger.error("Error cargando imagen polaroid al cambiar de modo", ex);
                                }
                            }
                        };
                        worker.execute();
                    }
                }
                
                if (model.getCurrentImage() != null && zoomManager != null) {
                    zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
                }
                
                actualizarPanelPolaroidActivo();
                if (infobarStatusManager != null) infobarStatusManager.mostrarMensaje("Modo: Vista Polaroid");
                break;
        }

        sincronizarBotonesDeModo();
        if (toolbarManager != null) {
            toolbarManager.sincronizarEstadoBotonesToolbar(actionMap, model);
        }
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
                } else if (model.getCurrentWorkMode() == WorkMode.DATOS) {
                    String key = model.getSelectedImageKey();
                    if (key != null) {
                        for (int i = 0; i < gridList.getModel().getSize(); i++) {
                            if (key.equals(gridList.getModel().getElementAt(i))) {
                                masterIndex = i;
                                break;
                            }
                        }
                    }
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
        // El ProjectController es responsable de poblar el grid del proyecto.
        if (model != null && model.getCurrentWorkMode() == WorkMode.PROYECTO) {
            return;
        }

        sincronizarBotonesDeModo();

        DisplayMode currentDisplay = model.getCurrentDisplayMode();
        
        if (currentDisplay == DisplayMode.GRID) {
            logger.debug("[DisplayModeManager] Notificado de cambio en lista maestra. Repoblando la parrilla...");
            poblarGridConModelo(newMasterList);
            sincronizarSeleccionGrid();
        } else if (currentDisplay == DisplayMode.POLAROID) {
            // La lista ha cambiado, refrescamos el panel de info por si el idx/total ha cambiado
            actualizarPanelPolaroidActivo();
        }
    } // end of onMasterListChanged
    
    @Override
    public void onMasterSelectionChanged(int newMasterIndex, Object source) {
        // Si estamos en modo Proyecto, el ProjectController manda.
        if (model != null && model.getCurrentWorkMode() == WorkMode.PROYECTO) {
            sincronizarSeleccionGrid();
            if (model.getCurrentDisplayMode() == DisplayMode.POLAROID) {
                actualizarPanelPolaroidActivo();
            }
            return; 
        }

        // Siempre que la selección cambie, refrescamos el estado de los botones de modo
        // para habilitar/deshabilitar SINGLE_IMAGE/POLAROID en modo DATOS.
        sincronizarBotonesDeModo();

        DisplayMode currentDisplay = model.getCurrentDisplayMode();
        
        if (currentDisplay == DisplayMode.GRID) {
            if (source != this) {
                logger.debug("[DisplayModeManager] Notificado de cambio de selección maestra (desde {}). Sincronizando grid...", source != null ? source.getClass().getSimpleName() : "null");
                sincronizarSeleccionGrid();
            }
        } else if (currentDisplay == DisplayMode.POLAROID) {
            // La selección cambió estando en Polaroid (ej: navegando con teclado)
            // → refrescamos el panel de metadatos e imagen
            logger.debug("[DisplayModeManager] Selección cambiada en modo POLAROID. Actualizando panel.");
            actualizarPanelPolaroidActivo();
        }
    } // end of onMasterSelectionChanged
    
    private JList<String> getActiveGridList() {
        if (model == null || registry == null) return null;
        if (model.getCurrentWorkMode() == WorkMode.PROYECTO) return registry.get("list.grid.proyecto");
        if (model.getCurrentWorkMode() == WorkMode.DATOS) return registry.get("list.datamode.grid");
        return registry.get("list.grid");
    } // end of getActiveGridList

    private void actualizarPanelPolaroidActivo() {
        if (model == null || registry == null) return;

        String panelKey;
        switch (model.getCurrentWorkMode()) {
            case PROYECTO:
                panelKey = "panel.proyecto.display.polaroid";
                break;
            case DATOS:
                panelKey = "panel.datamode.display.polaroid";
                break;
            default:
                panelKey = "panel.display.polaroid";
                break;
        }

        PolaroidDisplayPanel polaroidPanel = registry.get(panelKey);
        if (polaroidPanel != null) {
            polaroidPanel.actualizarInformacionDesdeModelo();
        }
    } // end of actualizarPanelPolaroidActivo

    // --- Setters para Inyección de Dependencias (RESTAURADOS Y COMPLETOS) ---
    public void setModel(VisorModel model) { this.model = model; }
    public void setRegistry(ComponentRegistry registry) { this.registry = registry; }
    public void setListCoordinator(IListCoordinator listCoordinator) { this.listCoordinator = listCoordinator; }
    public void setActionMap(Map<String, Action> actionMap) { this.actionMap = actionMap; }
    public void setInfobarStatusManager(InfobarStatusManager infobarStatusManager) { this.infobarStatusManager = infobarStatusManager; }
    public void setProjectController(ProjectController projectController) { this.projectController = projectController; }
    public void setProjectListCoordinator(ProjectListCoordinator projectListCoordinator) { this.projectListCoordinator = projectListCoordinator; }
    public void setZoomManager(IZoomManager zoomManager) { this.zoomManager = zoomManager; }
    public void setToolbarManager(controlador.managers.ToolbarManager toolbarManager) { this.toolbarManager = toolbarManager; }


} // end of class DisplayModeManager

