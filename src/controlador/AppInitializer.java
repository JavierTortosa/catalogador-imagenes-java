package controlador;

import java.awt.Color;
import java.awt.KeyboardFocusManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.factory.ActionFactory;
import controlador.managers.BackgroundControlManager;
import controlador.managers.CarouselManager;
import controlador.managers.ConfigApplicationManager;
import controlador.managers.DisplayModeManager;
import controlador.managers.EditionManager;
import controlador.managers.FileOperationsManager;
import controlador.managers.FilterManager;
import controlador.managers.FolderNavigationManager;
import controlador.managers.InfobarImageManager;
import controlador.managers.InfobarStatusManager;
import controlador.managers.ToolbarManager;
import controlador.managers.ViewManager;
import controlador.managers.ZoomManager;
import controlador.managers.tree.FolderTreeManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.ProjectManager;
import servicios.image.ThumbnailService;
import servicios.zoom.ZoomModeEnum;
import vista.VisorView;
import vista.builders.MenuBarBuilder;
import vista.builders.ProjectBuilder;
import vista.builders.ToolbarBuilder;
import vista.builders.ViewBuilder;
import vista.config.ToolbarButtonDefinition;
import vista.config.ToolbarComponentDefinition;
import vista.config.UIDefinitionService;
import vista.theme.ThemeManager;
import vista.util.IconUtils;
import vista.util.ThumbnailPreviewer;

public class AppInitializer {
	
	private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);

    // --- Campos de la clase (Dependencias a gestionar) ---
    private final VisorController controller;
    
    private VisorModel model;
    private ConfigurationManager configuration;
    private ThemeManager themeManager;
    private IconUtils iconUtils;
    private ThumbnailService thumbnailServiceGlobal;
    private ThumbnailService gridThumbnailService;
    private ProjectManager projectManagerService;
    private ComponentRegistry registry;
    
    // Controladores y Coordinadores
    private ProjectController projectController;
    private GeneralController generalController;
    private ListCoordinator listCoordinator;
    private ProjectListCoordinator projectListCoordinator;
    
    // Managers
    private ConfigApplicationManager configAppManager;
    private ZoomManager zoomManager;
    private EditionManager editionManager;
    private FileOperationsManager fileOperationsManager;
    private InfobarImageManager infobarImageManager;
    private InfobarStatusManager statusBarManager;
    private ToolbarManager toolbarManager;
    private ViewManager viewManager;
    private BackgroundControlManager backgroundControlManager;
    private CarouselManager carouselManager;
    private DisplayModeManager displayModeManager;
    private FolderNavigationManager folderNavManager;
    private FolderTreeManager folderTreeManager;
    private FilterManager filterManager;
    
    // UI y Factorías
    private ActionFactory actionFactory;
    private Map<String, Action> actionMap;
    private VisorView view;
    private ProjectBuilder projectBuilder;
    private ViewBuilder viewBuilder;
    private ToolbarBuilder toolbarBuilder;
    private MenuBarBuilder menuBuilder;
    
    private vista.builders.PopupMenuBuilder popupMenuBuilder;
    
    private List<ThumbnailPreviewer> activePreviewers;
    private VisorController controllerRefParaNotificacion;

    public AppInitializer(VisorController controller) {
        this.controller = Objects.requireNonNull(controller, "VisorController no puede ser null en AppInitializer");
        this.activePreviewers = new ArrayList<>();
    } // ---FIN de metodo AppInitializer (constructor)---

    /**
     * Orquesta el proceso de arranque de la aplicación en tres fases claras:
     * 1. Instanciación: Crea todos los objetos.
     * 2. Cableado: Conecta las dependencias entre los objetos creados.
     * 3. Inicialización: Construye la UI y arranca la lógica de la aplicación.
     */
    public boolean initialize() {
    	logger.info("--- Iniciando Proceso de Inicialización Global ---");
        try {
            instantiateComponents();
            wireDependencies();
            initializeApplication();
            return true;
        } catch (Exception e) {
            manejarErrorFatalInicializacion("Error fatal durante el proceso de arranque", e);
            logger.error("Error fatal durante el proceso de arranque", e);
            return false;
        }
    } // ---FIN de metodo initialize---

    /**
     * FASE 1: INSTANCIACIÓN.
     * Crea todas las instancias de los componentes principales (Modelos, Servicios, Managers, Controladores, Builders).
     * En esta fase, los objetos se crean pero no se conectan entre sí (excepto por dependencias de constructor).
     */
    private void instantiateComponents() {
        logger.info("--- [Fase 1/3] Instanciando todos los componentes...");

        // Componentes base y servicios
        this.model = new VisorModel();
        this.configuration = ConfigurationManager.getInstance();
        this.themeManager = new ThemeManager(this.configuration);
        
        // <<< CORRECCIÓN: Instalamos el tema aquí mismo, para que esté disponible
        // para los constructores de otros componentes que lo necesiten.
        this.themeManager.install();
        
        this.iconUtils = new IconUtils(this.themeManager);
        this.thumbnailServiceGlobal = new ThumbnailService();
        this.gridThumbnailService = new ThumbnailService();
        this.projectManagerService = new ProjectManager();
        this.registry = new ComponentRegistry();

        // Controladores, Coordinadores y Managers
        this.generalController = new GeneralController();
        this.filterManager = new FilterManager(this.model);
        this.folderNavManager = new FolderNavigationManager(this.model, this.generalController);
        this.folderTreeManager = new FolderTreeManager(this.model, this.generalController);
        this.zoomManager = new ZoomManager();
        this.editionManager = new EditionManager();
        this.viewManager = new ViewManager();
        this.listCoordinator = new ListCoordinator();
        this.displayModeManager = new DisplayModeManager();
        this.fileOperationsManager = new FileOperationsManager();
        this.projectController = new ProjectController();
        this.projectListCoordinator = new ProjectListCoordinator(this.model, this.controller, this.registry);
        this.carouselManager = new CarouselManager(listCoordinator, this.controller, this.registry, this.model, this.iconUtils);
        
        // UI Builders y Servicios de UI
        UIDefinitionService uiDefSvc = new UIDefinitionService();
        
        this.popupMenuBuilder = new vista.builders.PopupMenuBuilder(this.themeManager, this.configuration, this.controller);
        
        this.toolbarBuilder = new ToolbarBuilder(this.themeManager, this.iconUtils, this.controller, configuration.getInt("iconos.ancho", 24), configuration.getInt("iconos.alto", 24), this.registry);
        this.toolbarManager = new ToolbarManager(this.registry, this.configuration, this.toolbarBuilder, uiDefSvc, this.model);
        this.projectBuilder = new ProjectBuilder(this.registry, this.model, this.themeManager, this.generalController, this.toolbarManager, this.projectController); // <-- USAR EL CAMPO
        this.viewBuilder = new ViewBuilder(this.registry, this.model, this.themeManager, this.configuration, this.iconUtils, this.thumbnailServiceGlobal, this.gridThumbnailService, this.projectBuilder); // <-- PASAR EL CAMPO
        ProjectBuilder projectBuilder = new ProjectBuilder(this.registry, this.model, this.themeManager, this.generalController, this.toolbarManager, this.projectController);
        this.viewBuilder = new ViewBuilder(this.registry, this.model, this.themeManager, this.configuration, this.iconUtils, this.thumbnailServiceGlobal, this.gridThumbnailService, projectBuilder);
        this.menuBuilder = new MenuBarBuilder(this.controller, this.configuration, this.viewManager, this.registry, this.themeManager);
        
        // Managers dependientes de UI
        this.configAppManager = new ConfigApplicationManager(this.model, this.configuration, this.themeManager, this.registry);
        this.infobarImageManager = new InfobarImageManager(this.model, this.registry, this.configuration);
        this.backgroundControlManager = new BackgroundControlManager(registry, this.themeManager, this.viewManager, this.configuration, this.iconUtils, BorderFactory.createLineBorder(Color.GRAY), BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.CYAN, 2), BorderFactory.createLineBorder(Color.BLACK)));

        // Factoría de Acciones (ActionFactory)
        Map<String, ActionFactory.IconInfo> iconMap = new java.util.HashMap<>();
        uiDefSvc.generateModularToolbarStructure().forEach(toolbarDef -> {
            for (ToolbarComponentDefinition compDef : toolbarDef.componentes()) {
                if (compDef instanceof ToolbarButtonDefinition buttonDef) {
                    iconMap.put(buttonDef.comandoCanonico(), new ActionFactory.IconInfo(buttonDef.claveIcono(), buttonDef.scopeIconoBase()));
                }
            }
        });
        this.actionFactory = new ActionFactory(this.model, null, this.zoomManager, this.fileOperationsManager, this.editionManager, this.listCoordinator, this.iconUtils, this.configuration, this.projectManagerService, iconMap, this.viewManager, this.themeManager, this.registry, this.generalController, this.projectController);
        
        logger.debug(" -> Instanciación de componentes completada.");
    } // ---FIN de metodo instantiateComponents---

    /**
     * FASE 2: CABLEADO DE DEPENDENCIAS.
     * Conecta los objetos instanciados en la Fase 1 usando sus métodos `set...` y `add...Listener`.
     * Todas las dependencias que NO requieren una instancia de la UI (JFrame) se resuelven aquí.
     */
    private void wireDependencies() {
        logger.info("--- [Fase 2/3] Cableando dependencias entre componentes...");

        // Inyecciones al controlador principal (`VisorController`)
        this.controller.setModel(this.model);
        this.controller.setConfigurationManager(this.configuration);
        this.controller.setThemeManager(this.themeManager);
        this.controller.setIconUtils(this.iconUtils);
        this.controller.setServicioMiniaturas(this.thumbnailServiceGlobal);
        this.controller.setExecutorService(Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2)));
        this.controller.setViewManager(this.viewManager);
        this.controller.setComponentRegistry(this.registry);
        this.controller.setProjectManager(this.projectManagerService);
        this.controller.setToolbarManager(this.toolbarManager);
        this.controller.setActionFactory(this.actionFactory);
        this.controller.setBackgroundControlManager(this.backgroundControlManager);
        this.controller.setGeneralController(this.generalController);
        this.controller.setDisplayModeManager(this.displayModeManager);
        this.controller.setZoomManager(this.zoomManager);
        this.controller.setListCoordinator(this.listCoordinator);
        this.controller.setConfigApplicationManager(this.configAppManager);
        this.controller.setInfobarImageManager(this.infobarImageManager);
        
        // Suscripciones a eventos de cambio de tema
        this.themeManager.addThemeChangeListener(this.toolbarManager);
        this.themeManager.addThemeChangeListener(this.controller);
        this.themeManager.addThemeChangeListener(this.viewManager);
        this.themeManager.addThemeChangeListener(this.displayModeManager);
        this.themeManager.addThemeChangeListener(this.projectBuilder);
        this.themeManager.addThemeChangeListener(this.backgroundControlManager);
        this.themeManager.addThemeChangeListener(this.infobarImageManager);
        
        // Inyección de dependencias en Managers y Controladores
        viewManager.setModel(this.model);
        viewManager.setRegistry(this.registry);
        viewManager.setConfiguration(this.configuration);
        viewManager.setThemeManager(this.themeManager);
        viewManager.setToolbarManager(this.toolbarManager);
        viewManager.setViewBuilder(this.viewBuilder);
        viewManager.setDisplayModeManager(this.displayModeManager);
        viewBuilder.setFolderTreeManager(this.folderTreeManager);
        viewBuilder.setToolbarManager(this.toolbarManager);
        viewBuilder.setProjectBuilder(this.projectBuilder);
        

        zoomManager.setModel(this.model);
        zoomManager.setRegistry(this.registry);
        zoomManager.setConfiguration(this.configuration);
        zoomManager.setViewManager(this.viewManager);
        zoomManager.setListCoordinator(this.listCoordinator);

        if (this.projectManagerService != null) {
            this.projectManagerService.setConfigManager(this.configuration);
        }
        editionManager.setModel(this.model);
        editionManager.setController(this.controller);
        editionManager.setZoomManager(this.zoomManager);

        listCoordinator.setModel(this.model);
        listCoordinator.setController(this.controller);
        listCoordinator.setRegistry(this.registry);
        listCoordinator.setContextSensitiveActions(this.actionFactory.getContextSensitiveActions());

        projectController.setProjectManager(this.projectManagerService);
        projectController.setViewManager(this.viewManager);
        projectController.setRegistry(this.registry);
        projectController.setZoomManager(this.zoomManager);
        projectController.setModel(this.model);
        projectController.setController(this.controller);
        projectController.setListCoordinator(this.listCoordinator);
        
        projectController.setProjectListCoordinator(this.projectListCoordinator);
        projectController.setDisplayModeManager(this.displayModeManager);

        displayModeManager.setListCoordinator(this.listCoordinator);
        displayModeManager.setProjectListCoordinator(this.projectListCoordinator);
        projectListCoordinator.setContextSensitiveActions(this.actionFactory.getContextSensitiveActions());
        projectListCoordinator.setProjectController(this.projectController);

        generalController.setModel(this.model);
        generalController.setViewManager(this.viewManager);
        generalController.setVisorController(this.controller);
        generalController.setProjectController(this.projectController);
        generalController.setConfigApplicationManager(this.configAppManager);
        generalController.setRegistry(this.registry);
        generalController.setToolbarManager(this.toolbarManager);
        generalController.setDisplayModeManager(this.displayModeManager);
        generalController.setConfiguration(this.configuration);
        generalController.setFilterManager(this.filterManager);
        generalController.setFolderNavigationManager(this.folderNavManager);
        generalController.setFolderTreeManager(this.folderTreeManager);

        java.util.function.Consumer<java.nio.file.Path> onFolderSelectedCallback = (p) -> this.generalController.solicitarCargaDesdeNuevaRaiz(p);
        fileOperationsManager.setModel(this.model);
        fileOperationsManager.setController(this.controller);
        fileOperationsManager.setConfiguration(this.configuration);
        fileOperationsManager.setOnNuevaCarpetaSeleccionadaCallback(onFolderSelectedCallback);
        
        themeManager.setConfigApplicationManager(this.configAppManager);
        menuBuilder.setControllerGlobalActionListener(this.controller);
        configAppManager.setBackgroundControlManager(this.backgroundControlManager);
        toolbarManager.setBackgroundControlManager(this.backgroundControlManager);
        actionFactory.setCarouselManager(carouselManager);
        
        toolbarManager.setProjectController(this.projectController);
        
        actionFactory.setDisplayModeManager(this.displayModeManager);
        this.model.addMasterListChangeListener(this.generalController);
        
        logger.debug(" -> Cableado de dependencias completado.");
    } // ---FIN de metodo wireDependencies---

    /**
     * FASE 3: INICIALIZACIÓN.
     * Ejecuta las tareas que arrancan la aplicación. Esto se hace en el Event Dispatch Thread (EDT)
     * porque implica la creación y manipulación de componentes Swing.
     * Incluye la creación de la ventana principal, el cableado de dependencias que la necesitaban,
     * la construcción de la UI, la configuración de listeners y la carga de datos iniciales.
     */
    private void initializeApplication() {
        logger.info("--- [Fase 3/3] Inicializando la aplicación en el EDT...");
        SwingUtilities.invokeLater(() -> {
            try {
                // 3.1: Aplicar configuración al modelo y al tema
                aplicarConfiguracionAlModelo();

                // 3.2: Inicializar acciones y crear el mapa de acciones
                this.actionFactory.initializeCoreActions();
                this.actionMap = this.actionFactory.getActionMap();
                this.toolbarBuilder.setActionMap(this.actionMap);
                this.viewBuilder.setActionMap(this.actionMap);
                this.configAppManager.setActionMap(this.actionMap);
                this.viewManager.setActionMap(this.actionMap);
                this.controller.setActionMap(this.actionMap);
                this.projectController.setActionMap(this.actionMap);
                this.generalController.setActionMap(this.actionMap);

                // 3.3: ¡Paso clave! Crear la ventana principal (JFrame)
                logger.debug("    -> Creando VisorView (JFrame)...");
                this.view = viewBuilder.createMainFrame();

                // 3.4: Inyectar la 'view' en los componentes que la necesitaban
                logger.debug("    -> Inyectando la instancia de 'view' en los componentes...");
                this.actionFactory.setView(this.view);
                this.configAppManager.setView(this.view);
                this.viewManager.setView(this.view);
                this.controller.setView(this.view);
                this.projectController.setView(this.view);
                
                // 3.5: Inicializar acciones que dependen de la vista
                this.actionFactory.initializeViewDependentActions();
                
                // 3.6: Cableado final de componentes que dependen de la UI
                this.statusBarManager = new InfobarStatusManager(this.model, this.registry, this.themeManager, this.configuration, this.projectManagerService, this.actionMap, this.iconUtils);
                this.themeManager.addThemeChangeListener(this.statusBarManager);
                this.controller.setStatusBarManager(this.statusBarManager);
                this.zoomManager.setStatusBarManager(this.statusBarManager);
                this.generalController.setStatusBarManager(this.statusBarManager);
                this.displayModeManager.setInfobarStatusManager(this.statusBarManager);
                this.statusBarManager.setController(this.controller);
                
                this.displayModeManager.setModel(this.model);
                this.displayModeManager.setViewManager(this.viewManager);
                this.displayModeManager.setRegistry(this.registry);
                this.displayModeManager.setActionMap(this.actionMap);
                this.displayModeManager.setConfiguration(this.configuration);
                this.displayModeManager.setThemeManager(this.themeManager);
                this.displayModeManager.setToolbarManager(this.toolbarManager);
                this.displayModeManager.setConfigApplicationManager(this.configAppManager);
                this.displayModeManager.setGridThumbnailService(this.gridThumbnailService);
                this.displayModeManager.initializeListeners();
                
                this.listCoordinator.addMasterSelectionChangeListener(this.displayModeManager);
                this.projectListCoordinator.addMasterSelectionChangeListener(this.displayModeManager);
                
                // 3.7: Ensamblaje final de la UI
                logger.debug("    -> Ensamblando UI: Menús, Toolbars, Listeners...");
                this.view.setJMenuBar(this.menuBuilder.buildMenuBar(new UIDefinitionService().generateMenuStructure(), this.actionMap));
                this.controller.setMenuItemsPorNombre(this.menuBuilder.getMenuItemsMap());
                this.menuBuilder.getMenuItemsMap().forEach(this.registry::register);
                this.toolbarManager.reconstruirContenedorDeToolbars(this.model.getCurrentWorkMode());
                this.controller.setBotonesPorNombre(this.toolbarBuilder.getBotonesPorNombre());
                this.viewManager.setBotonesPorNombre(this.toolbarBuilder.getBotonesPorNombre());

                JList<String> miniaturasVisor = registry.get("list.miniaturas");
                if (miniaturasVisor != null) miniaturasVisor.setModel(this.controller.getModeloMiniaturasVisualizador());
                JList<String> miniaturasCarrusel = registry.get("list.miniaturas.carousel");
                if (miniaturasCarrusel != null) miniaturasCarrusel.setModel(this.controller.getModeloMiniaturasCarrusel());

                // 3.8: Configurar listeners y estado inicial
                if (this.projectManagerService != null) this.projectManagerService.initialize();
                
                this.controller.configurarAtajosTecladoGlobales();
                this.generalController.configurarListenersDeEntradaGlobal();
                KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this.generalController);
                KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", this.generalController);
                this.controller.configurarListenersVistaInternal();
                this.controller.configurarMenusContextuales();
                this.projectController.configurarListeners();
                this.generalController.initialize();
                
                instalarPreviewers();
                configurarCierreVentana();

                // 3.9: Hacer visible la UI y cargar datos
                String folderInitPath = this.configuration.getString(ConfigKeys.INICIO_CARPETA, "");
                if (!folderInitPath.isEmpty()) {
                    try {
                        Path candidatePath = Paths.get(folderInitPath);
                        if (Files.isDirectory(candidatePath)) {
                            this.model.setCarpetaRaizInicialParaVisualizador(candidatePath);
                        }
                    } catch (Exception e) {
                        logger.warn("WARN: Ruta de carpeta inicial inválida en config: '" + folderInitPath + "'");
                    }
                }

                this.view.setVisible(true);
                logger.debug("    -> Ventana principal visible. La aplicación está 'viva'.");
                
                sincronizarVisibilidadInicialUI();
                
                this.configAppManager.aplicarConfiguracionGlobalmente();
                this.controller.sincronizarColoresDePanelesPorTema();
                this.generalController.sincronizarTodaLaUIConElModelo();
                this.backgroundControlManager.initializeAndLinkControls();
                this.backgroundControlManager.sincronizarSeleccionConEstadoActual();
                if (this.viewManager != null) this.viewManager.initializeFocusBorders();
                
                cargarDatosIniciales();

                logger.info("--- [Fase 3/3] Inicialización de UI completada. La aplicación está lista. ---");

            } catch (Exception e) {
                manejarErrorFatalInicializacion("[EDT] Error fatal durante la creación de la UI", e);
            }
        });
    } // ---FIN de metodo initializeApplication---

    
    // --- MÉTODOS AYUDANTES REFACTORIZADOS ---
    
    private void aplicarConfiguracionAlModelo() {
        logger.debug("  -> Aplicando Configuración a los contextos del Modelo...");
        if (this.configuration == null || this.model == null) return;
        
        boolean mantenerProp = configuration.getBoolean("interfaz.menu.zoom.mantener_proporciones.seleccionado", true);
        boolean incluirSubcarpetas = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_CARGAR_SUBCARPETAS, true);
        boolean soloCarpeta = !incluirSubcarpetas;
        boolean navCircular = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_NAVEGACION_CIRCULAR, false);
        boolean zoomManualInicial = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_ZOOM_MANUAL_INICIAL, true);
        boolean zoomAlCursor = configuration.getBoolean("comportamiento.zoom.al_cursor.activado", false);
        int saltoBloque = configuration.getInt(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE, 10);
        
        ZoomModeEnum modoZoomInicial;
        try {
            modoZoomInicial = ZoomModeEnum.valueOf(configuration.getString(ConfigKeys.COMPORTAMIENTO_ZOOM_ULTIMO_MODO, "FIT_TO_SCREEN").toUpperCase());
        } catch (IllegalArgumentException e) {
            modoZoomInicial = ZoomModeEnum.FIT_TO_SCREEN;
        }
        
        this.model.initializeContexts(mantenerProp, soloCarpeta, modoZoomInicial, zoomManualInicial, navCircular, zoomAlCursor);
        this.model.setMiniaturasAntes(configuration.getInt(ConfigKeys.MINIATURAS_CANTIDAD_ANTES, 8));
        this.model.setMiniaturasDespues(configuration.getInt(ConfigKeys.MINIATURAS_CANTIDAD_DESPUES, 8));
        this.model.setMiniaturaSelAncho(configuration.getInt(ConfigKeys.MINIATURAS_TAMANO_SEL_ANCHO, 60));
        this.model.setMiniaturaSelAlto(configuration.getInt(ConfigKeys.MINIATURAS_TAMANO_SEL_ALTO, 60));
        this.model.setMiniaturaNormAncho(configuration.getInt(ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, 40));
        this.model.setMiniaturaNormAlto(configuration.getInt(ConfigKeys.MINIATURAS_TAMANO_NORM_ALTO, 40));
        this.model.setSaltoDeBloque(saltoBloque);
        this.model.setModoPantallaCompletaActivado(configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_PANTALLA_COMPLETA, false));
        this.model.setCarouselDelay(configuration.getInt(ConfigKeys.CAROUSEL_DELAY_MS, 3000));
        
        boolean syncActivado = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_SYNC_VISOR_CARRUSEL, false);
        this.model.setSyncVisualizadorCarrusel(syncActivado);
        String ultimaCarpetaStr = configuration.getString(ConfigKeys.CARRUSEL_ESTADO_ULTIMA_CARPETA, "");
        if (!ultimaCarpetaStr.isEmpty()) {
            try {
                this.model.setUltimaCarpetaCarrusel(Paths.get(ultimaCarpetaStr));
            } catch (java.nio.file.InvalidPathException e) {
                logger.error("WARN: Ruta de la última carpeta del carrusel inválida en config: " + ultimaCarpetaStr);
            }
        }
        this.model.setUltimaImagenKeyCarrusel(configuration.getString(ConfigKeys.CARRUSEL_ESTADO_ULTIMA_IMAGEN, ""));
    } // ---FIN de metodo aplicarConfiguracionAlModelo---

    private void instalarPreviewers() {
        JList<String> miniaturasList = registry.get("list.miniaturas");
        if (miniaturasList != null) {
            this.activePreviewers.add(new ThumbnailPreviewer(miniaturasList, this.model, this.themeManager, this.viewManager, registry));
            logger.debug("  -> Previsualizador de doble clic instalado en 'list.miniaturas'.");
        } else {
            logger.warn("WARN: No se pudo instalar ThumbnailPreviewer, 'list.miniaturas' no encontrada.");
        }

        JList<String> gridList = registry.get("list.grid");
        if (gridList != null) {
            this.activePreviewers.add(new ThumbnailPreviewer(gridList, this.model, this.themeManager, this.viewManager, registry));
            logger.debug("  -> Previsualizador de doble clic instalado en 'list.grid'.");
        } else {
            logger.warn("WARN: No se pudo instalar ThumbnailPreviewer, 'list.grid' no encontrada.");
        }

        JList<String> projectGridList = registry.get("list.grid.proyecto");
        if (projectGridList != null) {
            this.activePreviewers.add(new ThumbnailPreviewer(projectGridList, this.model, this.themeManager, this.viewManager, registry));
            logger.debug("  -> Previsualizador de doble clic instalado en 'list.grid.proyecto'.");
        } else {
            logger.warn("WARN: No se pudo instalar ThumbnailPreviewer, 'list.grid.proyecto' no encontrada.");
        }
    } // ---FIN de metodo instalarPreviewers---
    
    private void configurarCierreVentana() {
        this.view.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                controller.shutdownApplication();
            }
        });
    } // ---FIN de metodo configurarCierreVentana---

    private void sincronizarVisibilidadInicialUI() {
        logger.debug("  -> Sincronizando visibilidad inicial de paneles...");
        for (Action action : this.actionMap.values()) {
            if (action instanceof controlador.actions.config.ToggleUIElementVisibilityAction) {
                boolean isVisible = Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY));
                if (!isVisible) {
                    String actionCommand = (String) action.getValue(Action.ACTION_COMMAND_KEY);
                    action.actionPerformed(new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, actionCommand));
                }
            }
        }
    } // ---FIN de metodo sincronizarVisibilidadInicialUI---
    
    private void cargarDatosIniciales() {
        String imagenInicialKey = configuration.getString(ConfigKeys.INICIO_IMAGEN, null);
        if (this.model.getCarpetaRaizActual() != null) {
            this.controller.cargarListaImagenes(imagenInicialKey, null);
        } else {
            logger.debug("  -> No hay carpeta inicial válida. Se mostrará el estado de bienvenida.");
            this.controller.limpiarUI();
            this.generalController.sincronizarTodaLaUIConElModelo();
            if (this.controller.getStatusBarManager() != null) {
                this.controller.getStatusBarManager().mostrarMensaje("Abre una carpeta para empezar (Archivo -> Abrir Carpeta)");
            }
        }
    } // ---FIN de metodo cargarDatosIniciales---
    
    private void manejarErrorFatalInicializacion(String message, Throwable cause) {
        logger.error("### ERROR FATAL DE INICIALIZACIÓN ###");
        logger.error("Mensaje: " + message, cause);
        
        final String finalMessage = message + (cause != null ? "\n\nError Detallado: " + cause.getMessage() : "");
        
        Runnable showErrorDialog = () -> JOptionPane.showMessageDialog(null, finalMessage, "Error Fatal de Inicialización", JOptionPane.ERROR_MESSAGE);

        if (SwingUtilities.isEventDispatchThread()) {
            showErrorDialog.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(showErrorDialog);
            } catch (Exception swingEx) {
                logger.error("Error adicional al intentar mostrar el diálogo de error en EDT.", swingEx);
            }
        }
        
        logger.error("Terminando la aplicación debido a un error fatal de inicialización.");
        System.exit(1); 
    } // ---FIN de metodo manejarErrorFatalInicializacion---

    public void setControllerParaNotificacion(VisorController controller) {
        this.controllerRefParaNotificacion = controller;
    } // ---FIN de metodo setControllerParaNotificacion---
    
} // ---FIN de la clase AppInitializer---

