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
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import servicios.cliente.ClientSyncService;
import controlador.commands.AppActionCommands;
import controlador.factory.ActionFactory;
import controlador.managers.BackgroundControlManager;
import controlador.managers.CarouselManager;
import controlador.managers.ConfigApplicationManager;
import controlador.managers.DataManager;
import controlador.managers.DisplayModeManager;
import controlador.managers.EditionManager;
import controlador.managers.FileOperationsManager;
import controlador.managers.FilterManager;
import controlador.managers.FolderNavigationManager;
import controlador.managers.GlobalInputManager;
import controlador.managers.ImageListManager;
import controlador.managers.InfobarImageManager;
import controlador.managers.InfobarStatusManager;
import controlador.managers.KeyboardShortcutManager;
import controlador.managers.MenuPopupManager;
import controlador.managers.ToolbarManager;
import controlador.managers.ViewManager;
import controlador.managers.ZoomManager;
import controlador.managers.filter.FilterCriterion;
import controlador.managers.tree.FolderTreeManager;
import controlador.services.AppModeService;
import controlador.services.FilterService;
import controlador.services.NavigationService;
import controlador.services.ProjectLifecycleService;
import controlador.services.SearchSortService;
import controlador.services.ZoomPanService;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.ProjectManager;
import servicios.db.DatabaseManager;
import servicios.image.ThumbnailService;
import servicios.zoom.ZoomModeEnum;
import vista.VisorView;
import vista.builders.ClientBuilder;
import vista.builders.DataBuilder;
import vista.builders.MenuBarBuilder;
import vista.builders.ProjectBuilder;
import vista.builders.ToolbarBuilder;
import vista.builders.ViewBuilder;
import vista.config.IconScope;
import vista.config.ToolbarButtonDefinition;
import vista.config.ToolbarComponentDefinition;
import vista.config.UIDefinitionService;
import vista.theme.ThemeManager;
import vista.util.IconUtils;
import vista.util.ThumbnailPreviewer;

/**
 * Orquestador principal del arranque de la aplicación.
 * Gestiona la instanciación, cableado e inicialización de todos los
 * componentes (Modelo, Vista, Controladores, Servicios, Managers y Builders)
 * en tres fases secuenciales.
 */
public class AppInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);

    // --- Campos de la clase (Dependencias a gestionar) ---

    private VisorModel model;
    private ConfigurationManager configuration;
    private ThemeManager themeManager;
    private IconUtils iconUtils;
    private ThumbnailService thumbnailServiceGlobal;
    private ThumbnailService gridThumbnailService;
    private ProjectManager projectManagerService;
    private ComponentRegistry registry;
    private DataManager dataManager;

    // Controladores
    private final VisorController controller;

    private ProjectController projectController;
    private ClientController clientController;
    private ClientSyncService clientSyncService;
    private DataController dataController;
    private GeneralController generalController;

    // Builders
    private ProjectBuilder projectBuilder;
    private ClientBuilder clientBuilder;
    private DataBuilder dataBuilder;
    private ViewBuilder viewBuilder;
    private ToolbarBuilder toolbarBuilder;
    private MenuBarBuilder menuBuilder;

    // Coordinadores
    private ListCoordinator listCoordinator;
    private ProjectListCoordinator projectListCoordinator;

    // Services
    private FilterService filterService; // PENDIENTE DE REFACTORIZAR
    private NavigationService navigationService; // PENDIENTE DE REFACTORIZAR
    private ProjectLifecycleService projectLifecycleService; // PENDIENTE DE REFACTORIZAR
    private SearchSortService searchSortService; // PENDIENTE DE REFACTORIZAR
    private ZoomPanService zoomPanService; // PENDIENTE DE REFACTORIZAR
    private AppModeService appModeService;

    // Managers
    private ConfigApplicationManager configAppManager;
    private ZoomManager zoomManager;
    private EditionManager editionManager;
    private FileOperationsManager fileOperationsManager;
    private InfobarImageManager infobarImageManager;
    private InfobarStatusManager statusBarManager;
    private ToolbarManager toolbarManager;
    private MenuPopupManager menuPopupManager;
    private KeyboardShortcutManager keyboardShortcutManager;
    private ViewManager viewManager;
    private BackgroundControlManager backgroundControlManager;
    private CarouselManager carouselManager;
    private DisplayModeManager displayModeManager;
    private FolderNavigationManager folderNavManager;
    private FolderTreeManager folderTreeManager;
    private FilterManager filterManager;
    private GlobalInputManager globalInputManager;
    private vista.theme.ThemeApplier themeApplier;

    // UI y Factorías
    private ActionFactory actionFactory;
    private VisorView view;

    private Map<String, Action> actionMap;
    private List<ThumbnailPreviewer> activePreviewers;

    private final String appVersion;

    /**
     * Constructor que inicializa el controlador principal y la versión de la aplicación.
     *
     * @param controller Controlador principal de la aplicación
     * @param version Versión de la aplicación
     */
    public AppInitializer(VisorController controller, String version) {
        this.controller = Objects.requireNonNull(controller, "VisorController no puede ser null en AppInitializer");
        this.activePreviewers = new ArrayList<>();
        this.appVersion = version;
    } // --- FIN de metodo/clase AppInitializer ---

    
    /**
     * Orquesta el proceso de arranque de la aplicación en tres fases claras:
     * 1. Instanciación: Crea todos los objetos.
     * 2. Cableado: Conecta las dependencias entre los objetos creados.
     * 3. Inicialización: Construye la UI y arranca la lógica de la aplicación.
     *
     * @return true si la inicialización fue exitosa, false en caso contrario
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
    } // --- FIN de metodo/clase initialize ---

    
    /**
     * FASE 1: INSTANCIACIÓN.
     * Crea todas las instancias de los componentes principales (Modelos, Servicios,
     * Managers, Controladores, Builders).
     * En esta fase, los objetos se crean pero no se conectan entre sí (excepto por
     * dependencias de constructor).
     */
    private void instantiateComponents() {
        logger.info("--- [Fase 1/3] Instanciando todos los componentes...");

        // Aplicar la version de la aplicacion
        controller.setVersion(this.appVersion);

        // Componentes base y servicios
        this.model = new VisorModel();
        this.configuration = ConfigurationManager.getInstance();

        // Inicialización de la Base de Datos (AHORA CONFIGURABLE) ---
        logger.info("Inicializando la capa de persistencia de datos...");
        try {
            // Primero, pasamos la configuración para que decida la ruta de la BD.
            DatabaseManager.initialize(this.configuration);
            // Luego, obtenemos la instancia y creamos las tablas.
            DatabaseManager.getInstance().initializeDatabase();
        } catch (Exception e) {
            // Un error aquí es crítico. No se puede continuar.
            manejarErrorFatalInicializacion("No se pudo inicializar la base de datos.", e);
        }
        

        this.themeManager = new ThemeManager(this.configuration);
        this.themeManager.install();
        this.iconUtils = new IconUtils(this.themeManager);
        this.thumbnailServiceGlobal = new ThumbnailService();
        this.gridThumbnailService = new ThumbnailService();
        this.projectManagerService = new ProjectManager();
        this.registry = new ComponentRegistry();

        // Controladores, Coordinadores y Managers (en orden de dependencia)
        this.generalController = new GeneralController();
        this.globalInputManager = new GlobalInputManager();
        this.projectController = new ProjectController(); // Crear la ÚNICA instancia aquí
        this.clientController = new ClientController();
        this.clientSyncService = new ClientSyncService();
        
        this.clientController.setGeneralController(this.generalController);
        this.clientController.setClientSyncService(this.clientSyncService);
        this.clientController.setProjectManager(this.projectManagerService);
        this.clientController.setComponentRegistry(this.registry);
        this.clientController.setVisorController(this.controller);
        
        this.projectController.setGeneralController(this.generalController);

        this.filterManager = new FilterManager(this.model);
        this.folderNavManager = new FolderNavigationManager(this.model, this.generalController);
        this.folderTreeManager = new FolderTreeManager(this.model, this.generalController);
        this.zoomManager = new ZoomManager();
        this.editionManager = new EditionManager();
        this.viewManager = new ViewManager();
        this.listCoordinator = new ListCoordinator();
        this.displayModeManager = new DisplayModeManager();
        this.fileOperationsManager = new FileOperationsManager();
        this.projectListCoordinator = new ProjectListCoordinator(this.model, this.controller, this.registry);
        this.carouselManager = new CarouselManager(listCoordinator, this.controller, this.registry, this.model,
                this.iconUtils);

        // --- Instanciación de componentes del Modo Datos ---
        this.dataManager = new DataManager();
        this.dataController = new DataController(this.model, this.registry, this.dataManager);
        this.dataBuilder = new DataBuilder(this.registry, this.model, this.themeManager, this.iconUtils,
                this.gridThumbnailService, this.configuration);

        // UI Builders y Servicios de UI
        UIDefinitionService uiDefSvc = new UIDefinitionService();

        logger.info("Inicializando Botones");
        this.toolbarBuilder = new ToolbarBuilder(this.themeManager, this.iconUtils, this.controller,
                configuration.getInt(ConfigKeys.ICONOS_ANCHO, 24), configuration.getInt(ConfigKeys.ICONOS_ALTO, 24), this.registry);
        this.toolbarManager = new ToolbarManager(this.registry, this.configuration, this.toolbarBuilder, uiDefSvc,
                this.model);

        logger.info("Inicializando Controllador de Proyectos");
        // Usar la ÚNICA instancia de projectController
                this.projectBuilder = new ProjectBuilder(this.registry, this.model, this.themeManager, this.generalController,
                        this.toolbarManager, this.projectController);
                this.projectBuilder.setDataManager(this.dataManager);

        logger.info("Inicializando ClientBuilder");
        this.clientBuilder = new ClientBuilder(this.registry, this.model, this.themeManager, this.generalController,
                this.toolbarManager, this.clientController, this.projectManagerService);

        logger.info("Inicializando Construccion del Visor");
        // Usar el projectBuilder del campo de la clase (this.projectBuilder) para el
        // ViewBuilder
        this.viewBuilder = new ViewBuilder(this.registry, this.model, this.themeManager, this.configuration,
                this.iconUtils, this.thumbnailServiceGlobal, this.gridThumbnailService, this.projectManagerService,
                this.projectBuilder, this.clientBuilder);

        this.menuBuilder = new MenuBarBuilder(this.controller, this.configuration, this.viewManager, this.registry,
                this.themeManager);

        logger.info("Cargando configuracion inicial");
        // Managers dependientes de UI
        this.configAppManager = new ConfigApplicationManager(this.model, this.configuration, this.themeManager,
                this.registry);

        this.infobarImageManager = new InfobarImageManager(this.model, this.registry, this.configuration);
        this.backgroundControlManager = new BackgroundControlManager(registry, this.themeManager, this.viewManager,
                this.configuration, this.iconUtils, BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.CYAN, 2),
                        BorderFactory.createLineBorder(Color.BLACK)));

        logger.info("Inicializando Acciones");
        // Factoría de Acciones (ActionFactory)
        Map<String, ActionFactory.IconInfo> iconMap = new java.util.HashMap<>();
        uiDefSvc.generateModularToolbarStructure().forEach(toolbarDef -> {
            for (ToolbarComponentDefinition compDef : toolbarDef.componentes()) {
                if (compDef instanceof ToolbarButtonDefinition buttonDef) {
                    iconMap.put(buttonDef.comandoCanonico(),
                            new ActionFactory.IconInfo(buttonDef.claveIcono(), buttonDef.scopeIconoBase()));
                }
            }
        });

        // Registrar iconos huérfanos que no están en toolbars pero se usan en la UI
        iconMap.put(AppActionCommands.CMD_EXPORT_SELECCIONAR_CARPETA,
                new ActionFactory.IconInfo("6001-selector_de_carpetas_48x48.png", IconScope.THEMED));
        iconMap.put(AppActionCommands.CMD_AYUDA_MOSTRAR_GUIA,
                new ActionFactory.IconInfo("6002-menu_48x48.png", IconScope.THEMED));

        // Pasar la ÚNICA instancia de projectController a la ActionFactory
        this.actionFactory = new ActionFactory(this.model, null, this.zoomManager, this.fileOperationsManager,
                this.editionManager, this.listCoordinator, this.iconUtils, this.configuration,
                this.projectManagerService, iconMap, this.viewManager, this.themeManager, this.registry,
                this.generalController, this.projectController);

        
        // PENDIENTE DE REFACTORIZACION
        
        // Instanciamos el servicio de navegacion
        this.navigationService = new NavigationService(this.folderNavManager, this.folderTreeManager);
        
        // Instanciamos el servicio de ciclo de vida de proyectos
        this.projectLifecycleService = new ProjectLifecycleService(this.projectController, this.generalController,
                this.controller, this.model, this.configuration);
        
        // Instanciamos el servicio de búsqueda, tornado y ordenación
        this.searchSortService = new SearchSortService(this.model, this.filterManager, this.registry,
                this.configAppManager, this.configuration);
        
        // Instanciamos el paneo de imagen
        this.zoomPanService = new ZoomPanService(this.model, this.viewManager);
        
        // Instanciamos la orquestacion de mosdos y sincronizacion
        this.appModeService = new AppModeService(this.model, this.viewManager, this.toolbarManager, this.displayModeManager, this.configAppManager, this.statusBarManager);
        
        logger.debug(" -> Instanciación de componentes completada.");
    } // --- FIN de metodo/clase instantiateComponents ---
    

    /**
     * FASE 2: CABLEADO DE DEPENDENCIAS.
     * Conecta los objetos instanciados en la Fase 1 usando sus métodos `set...` y
     * `add...Listener`.
     * Todas las dependencias que NO requieren una instancia de la UI (JFrame) se
     * resuelven aquí.
     */
    private void wireDependencies() {
        logger.info("--- [Fase 2/3] Cableando dependencias entre componentes...");

        // Inyecciones al controlador principal (`VisorController`)
        this.controller.setModel(this.model);
        this.controller.setConfigurationManager(this.configuration);
        this.controller.setThemeManager(this.themeManager);
        this.controller.setIconUtils(this.iconUtils);
        this.controller.setServicioMiniaturas(this.thumbnailServiceGlobal);
        this.controller.setExecutorService(
                Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2)));
        this.controller.setViewManager(this.viewManager);
        this.controller.setComponentRegistry(this.registry);
        this.controller.setProjectManager(this.projectManagerService);
        this.controller.setToolbarManager(this.toolbarManager);
        this.controller.setActionFactory(this.actionFactory);
        this.controller.setGeneralController(this.generalController);
        this.controller.setDisplayModeManager(this.displayModeManager);
        this.controller.setZoomManager(this.zoomManager);
        this.controller.setListCoordinator(this.listCoordinator);
        this.controller.setConfigApplicationManager(this.configAppManager);
        this.controller.setInfobarImageManager(this.infobarImageManager);

        // Suscripciones a eventos de cambio de tema
        this.themeApplier = new vista.theme.ThemeApplier(this.registry);
        this.themeManager.addThemeChangeListener(this.themeApplier);
        this.themeManager.addThemeChangeListener(this.toolbarManager);
        this.themeManager.addThemeChangeListener(this.controller);
        this.themeManager.addThemeChangeListener(this.viewManager);
        this.themeManager.addThemeChangeListener(this.displayModeManager);
        this.themeManager.addThemeChangeListener(this.projectBuilder);
        this.themeManager.addThemeChangeListener(this.backgroundControlManager);
        this.themeManager.addThemeChangeListener(this.infobarImageManager);
        this.themeManager.addThemeChangeListener(this.globalInputManager);

        // Inyección de dependencias en Managers y Controladores
        viewManager.setModel(this.model);
        viewManager.setRegistry(this.registry);
        viewManager.setConfiguration(this.configuration);
        viewManager.setThemeManager(this.themeManager);
        viewManager.setToolbarManager(this.toolbarManager);
        viewManager.setDisplayModeManager(this.displayModeManager);
        viewBuilder.setFolderTreeManager(this.folderTreeManager);
        viewBuilder.setToolbarManager(this.toolbarManager);
        viewBuilder.setProjectBuilder(this.projectBuilder);
        viewBuilder.setClientBuilder(this.clientBuilder);

        zoomManager.setModel(this.model);
        zoomManager.setRegistry(this.registry);
        zoomManager.setConfiguration(this.configuration);
        zoomManager.setViewManager(this.viewManager);
        zoomManager.setInfobarImageManager(this.infobarImageManager);

        zoomManager.setConfigApplicationManager(this.configAppManager);

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
        projectController.setRegistry(this.registry);
        projectController.setDataManager(this.dataManager);
        projectController.setZoomManager(this.zoomManager);
        projectController.setModel(this.model);

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
        generalController.setFolderTreeManager(this.folderTreeManager);
        
        //PENDIENTE DE REFACTORIZACION
        
        // 1. Instanciamos el servicio (en la fase 1: instantiateComponents)
        this.filterService = new FilterService(this.filterManager, this.registry, this.model);

        // 2. Inyectamos el servicio (en la fase 2: wireDependencies)
        this.generalController.setFilterService(this.filterService);
        
        // 3. Inyectamos navigationService
        this.generalController.setNavigationService(this.navigationService);
        
        // 4. Inyectamos projectLifecycleService ("Guardar", "Abrir" y "Nuevo" proyecto) 
        this.generalController.setProjectLifecycleService(this.projectLifecycleService);
        
        // 5. Inyectamos el Filtro Tornado
        this.generalController.setSearchSortService(this.searchSortService);
        
        // 6. Inyectamos el paneo de imagenes
        this.generalController.setZoomPanService(this.zoomPanService);
        
        // 7. Inyectamos el gestor de modos y sincronizacion
        this.generalController.setAppModeService(this.appModeService);
        this.generalController.setMenuPopupManager(this.menuPopupManager);
        this.generalController.setClientController(this.clientController);

        // 7.1 Inyectamos dependencias adicionales en AppModeService
        this.appModeService.setVisorController(this.controller);
        this.appModeService.setProjectController(this.projectController);
        this.appModeService.setDataController(this.dataController);
        this.appModeService.setClientController(this.clientController);
        this.appModeService.setConfiguration(this.configuration);
        this.appModeService.setRegistry(this.registry);

        // --- TaggingManager (contexto compartido entre modos) ---
        controlador.managers.TaggingManager taggingManager = new controlador.managers.TaggingManager();
        this.controller.setTaggingManager(taggingManager);
        this.appModeService.setTaggingManager(taggingManager);
        
        // --- Cableado de componentes del Modo Datos ---
        this.viewBuilder.setDataBuilder(this.dataBuilder);
        this.generalController.setDataController(this.dataController);
        this.dataController.setProjectManager(this.projectManagerService);
        this.dataController.setVisorController(this.controller);

        // Inyectar dependencias en FilterManager para que pueda operar de forma
        // autónoma
        this.filterManager.setVisorController(this.controller);
        this.filterManager.setRegistry(this.registry);

        java.util.function.Consumer<java.nio.file.Path> onFolderSelectedCallback = (p) -> this.generalController
                .solicitarCargaDesdeNuevaRaiz(p);
        java.util.function.Consumer<java.nio.file.Path> onFileDeletedCallback = (p) -> {
            if (this.projectManagerService != null) {
                this.projectManagerService.eliminarDeProyecto(p);
            }
            if (this.model.getCarpetaRaizActual() != null) {
                this.generalController.solicitarCargaDesdeNuevaRaiz(this.model.getCarpetaRaizActual());
            }
        };
        fileOperationsManager.setModel(this.model);
        fileOperationsManager.setController(this.controller);
        fileOperationsManager.setConfiguration(this.configuration);
        fileOperationsManager.setOnNuevaCarpetaSeleccionadaCallback(onFolderSelectedCallback);
        fileOperationsManager.setOnArchivoBorradoCallback(onFileDeletedCallback);

        themeManager.setConfigApplicationManager(this.configAppManager);
        configAppManager.setBackgroundControlManager(this.backgroundControlManager);
        configAppManager.setActionFactory(this.actionFactory);
        toolbarManager.setBackgroundControlManager(this.backgroundControlManager);
        toolbarManager.setViewManager(this.viewManager);
        toolbarManager.setIconUtils(this.iconUtils);
        actionFactory.setCarouselManager(carouselManager);
        actionFactory.setConfigAppManager(this.configAppManager);
        actionFactory.setToolbarManager(this.toolbarManager);

        actionFactory.setDisplayModeManager(this.displayModeManager);
        this.model.addMasterListChangeListener(this.generalController);

        if (this.viewBuilder != null) {
            int iconSize = 16;
            Map<FilterCriterion.Logic, Icon> logicIcons = new java.util.EnumMap<>(FilterCriterion.Logic.class);
            logicIcons.put(FilterCriterion.Logic.ADD,
                    this.iconUtils.getScaledCommonIcon("tab-add_48x48.png", iconSize, iconSize));
            logicIcons.put(FilterCriterion.Logic.NOT,
                    this.iconUtils.getScaledCommonIcon("tab-substr_48x48.png", iconSize, iconSize));

            Map<FilterCriterion.SourceType, Icon> typeIcons = new java.util.EnumMap<>(FilterCriterion.SourceType.class);
            typeIcons.put(FilterCriterion.SourceType.TEXT,
                    this.iconUtils.getScaledCommonIcon("tag_texto_48x48.png", iconSize, iconSize));
            typeIcons.put(FilterCriterion.SourceType.FOLDER,
                    this.iconUtils.getScaledCommonIcon("tag_carpeta_48x48.png", iconSize, iconSize));
            typeIcons.put(FilterCriterion.SourceType.TAG,
                    this.iconUtils.getScaledCommonIcon("tag_tags_48x48.png", iconSize, iconSize));

            Icon deleteIcon = this.iconUtils.getScaledCommonIcon("status-remove-bold.png", iconSize, iconSize);

            // Pasamos los mapas al ViewBuilder para que pueda construir el renderer.
            this.viewBuilder.setFilterRendererIcons(logicIcons, typeIcons, deleteIcon);

            // Pasamos el mapa de tipos al GeneralController para que pueda construir el
            // menú contextual.
            if (this.generalController != null) {
                this.generalController.setTypeIconsMap(typeIcons);
            }
        }

        logger.debug(" -> Cableado de dependencias completado.");

    } // --- FIN de metodo/clase wireDependencies ---

    
    /**
     * FASE 3: INICIALIZACIÓN.
     * Ejecuta las tareas que arrancan la aplicación. Esto se hace en el Event
     * Dispatch Thread (EDT)
     * porque implica la creación y manipulación de componentes Swing.
     * Incluye la creación de la ventana principal, el cableado de dependencias que
     * la necesitaban,
     * la construcción de la UI, la configuración de listeners y la carga de datos
     * iniciales.
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

                // --- Inyección de dependencias en el GlobalInputManager (AHORA SÍ) ---
                this.globalInputManager.setModel(this.model);
                this.globalInputManager.setRegistry(this.registry);
                this.globalInputManager.setActionMap(this.actionMap); // actionMap ya está creado
                this.globalInputManager.setModoController(this.generalController);
                this.globalInputManager.setVisorController(this.controller);
                this.globalInputManager.setProjectController(this.projectController);

                this.zoomManager.setActionMap(this.actionMap);
                this.toolbarBuilder.setActionMap(this.actionMap);
                this.appModeService.setActionMap(this.actionMap);
                this.viewBuilder.setActionMap(this.actionMap);
                this.dataBuilder.setActionMap(this.actionMap);
                this.dataBuilder.setDataController(this.dataController);
                this.dataBuilder.setProjectManager(this.projectManagerService);
                this.configAppManager.setActionMap(this.actionMap);
                this.viewManager.setActionMap(this.actionMap);
                this.controller.setActionMap(this.actionMap);
                this.projectController.setActionMap(this.actionMap);
                this.generalController.setActionMap(this.actionMap);
                this.searchSortService.setActionMap(this.actionMap);
                this.searchSortService.setVisorController(this.controller);
                this.menuPopupManager = new MenuPopupManager(this.actionMap, this.registry,
                        this.generalController, this.actionFactory, this.model, this.dataManager);

                // 3.3: ¡Paso clave! Crear la ventana principal (JFrame)
                logger.debug("    -> Creando VisorView (JFrame)...");
                this.view = viewBuilder.createMainFrame();
                this.zoomManager.setView(this.view);

                // 3.4: Inyectar la 'view' en los componentes que la necesitaban
                logger.debug("    -> Inyectando la instancia de 'view' en los componentes...");
                this.actionFactory.setView(this.view);
                this.viewManager.setView(this.view);
                this.controller.setView(this.view);
                this.view.setController(this.controller);
                this.projectController.setView(this.view);
                this.keyboardShortcutManager = new KeyboardShortcutManager(this.view, this.actionMap,
                        this.controller.getListCoordinator(), this.registry, this.statusBarManager);

                // 3.5: Inicializar acciones que dependen de la vista
                this.actionFactory.initializeViewDependentActions();

                // 3.6: Cableado final de componentes que dependen de la UI
                this.statusBarManager = new InfobarStatusManager(this.model, this.registry, this.themeManager,
                        this.configuration, this.projectManagerService, this.actionMap, this.iconUtils);
                this.themeManager.addThemeChangeListener(this.statusBarManager);
                this.controller.setStatusBarManager(this.statusBarManager);
                this.zoomManager.setStatusBarManager(this.statusBarManager);
                this.generalController.setStatusBarManager(this.statusBarManager);
                this.filterManager.setStatusBarManager(this.statusBarManager);
                this.dataController.setStatusBarManager(this.statusBarManager);
                this.displayModeManager.setInfobarStatusManager(this.statusBarManager);
                this.statusBarManager.setMenuPopupManager(this.menuPopupManager);
                this.searchSortService.setStatusBarManager(this.statusBarManager);

                this.displayModeManager.setModel(this.model);
                this.displayModeManager.setRegistry(this.registry);
                this.displayModeManager.setActionMap(this.actionMap);
                this.displayModeManager.setZoomManager(this.zoomManager);
                this.displayModeManager.setToolbarManager(this.toolbarManager);
                this.displayModeManager.initializeListeners();

                this.listCoordinator.addMasterSelectionChangeListener(this.displayModeManager);
                this.projectListCoordinator.addMasterSelectionChangeListener(this.displayModeManager);

                ImageListManager imageListManager = new ImageListManager(this.controller);
                imageListManager.setFilterManager(this.filterManager);
                this.controller.setImageListManager(imageListManager);
                this.generalController.setImageListManager(imageListManager);
                this.actionFactory.setImageListManager(imageListManager);
                this.actionFactory.setClientController(this.clientController);
                this.clientController.setActionFactory(this.actionFactory);
                this.actionFactory.initializeLateActions();

                // 3.7: Ensamblaje final de la UI
                logger.debug("    -> Ensamblando UI: Menús, Toolbars, Listeners...");
                this.view.setJMenuBar(this.menuBuilder.buildMenuBar(new UIDefinitionService().generateMenuStructure(),
                        this.actionMap));
                this.menuBuilder.getMenuItemsMap().forEach(this.registry::register);
                this.toolbarManager.reconstruirContenedorDeToolbars(this.model.getCurrentWorkMode());
                this.controller.setBotonesPorNombre(this.toolbarBuilder.getBotonesPorNombre());

                JList<String> miniaturasVisor = registry.get("list.miniaturas");
                if (miniaturasVisor != null)
                    miniaturasVisor.setModel(this.controller.getModeloMiniaturasVisualizador());
                JList<String> miniaturasCarrusel = registry.get("list.miniaturas.carousel");
                if (miniaturasCarrusel != null)
                    miniaturasCarrusel.setModel(this.controller.getModeloMiniaturasCarrusel());

                if (this.listCoordinator != null) {
                    this.listCoordinator.configurarSyncMultiSeleccion();
                }

                // 3.8: Configurar listeners y estado inicial
                if (this.projectManagerService != null)
                    this.projectManagerService.initialize();

                this.keyboardShortcutManager.configurarAtajosTecladoGlobales();
                this.globalInputManager.configurarListeners();

                // --- INICIO: AÑADIR LISTENER INTERACTIVO A LA LISTA DE FILTROS ---
                JList<controlador.managers.filter.FilterCriterion> filterList = registry.get("list.filtrosActivos");
                if (filterList != null) {
                    filterList.addMouseListener(new java.awt.event.MouseAdapter() {

                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            int index = filterList.locationToIndex(e.getPoint());
                            if (index == -1) {
                                return;
                            }

                            // Aseguramos que la fila clickeada sea la seleccionada
                            if (filterList.getSelectedIndex() != index) {
                                filterList.setSelectedIndex(index);
                            }

                            controlador.managers.filter.FilterCriterion criterion = filterList.getModel()
                                    .getElementAt(index);

                            // Delegamos toda la lógica de qué hacer con el clic al GeneralController
                            if (generalController != null) {
                                generalController.handleFilterListClick(e, criterion);
                            }

                        } // ---FIN de metodo mouseClicked---

                    });
                    logger.debug("    -> Listener interactivo añadido a 'list.filtrosActivos'.");

                } else {
                    logger.warn(
                            "WARN [AppInitializer]: No se pudo encontrar 'list.filtrosActivos' en el registro para añadir el listener interactivo.");
                }
                // --- FIN: AÑADIR LISTENER INTERACTIVO A LA LISTA DE FILTROS ---

                KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this.globalInputManager);
                KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner",
                        this.globalInputManager);
                this.controller.configurarListenersVistaInternal();
                this.menuPopupManager.configurarMenusContextuales();
                this.projectController.configurarListeners();

                this.globalInputManager.initialize();
                this.generalController.initialize();
                this.dataController.initialize();
                this.actionFactory.setSortCallback(this.dataController::ordenarListaPlana);
                this.actionFactory.setTagNuevoCallback(this.dataController::treePopupCrearTag);
                this.actionFactory.setTagRenombrarCallback(this.dataController::treePopupRenombrarTag);
                this.actionFactory.setTagBorrarCallback(this.dataController::treePopupBorrarTag);
                instalarPreviewers();
                configurarCierreVentana();

                // 3.9: Hacer visible la UI y cargar datos
                boolean restaurarUltimaImagen = this.configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_RESTAURAR_ULTIMA_IMAGEN, true);
                if (restaurarUltimaImagen) {
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
                }

				this.view.setVisible(true);
				this.zoomManager.configurarListenerRedimensionVentana();
				new javax.swing.Timer(1000, e -> this.toolbarManager.aplicarOverflow()).start();

				logger.debug("    -> Ventana principal visible. La aplicación está 'viva'.");

                sincronizarVisibilidadInicialUI();

                this.configAppManager.aplicarConfiguracionGlobalmente();
                this.viewManager.sincronizarColoresDePanelesPorTema();
                this.generalController.sincronizarTodaLaUIConElModelo();
                this.backgroundControlManager.initializeAndLinkControls();
                this.backgroundControlManager.sincronizarSeleccionConEstadoActual();
                if (this.viewManager != null)
                    this.viewManager.initializeFocusBorders();

                // 1. Prepara la acción que se ejecutará DESPUÉS de la carga inicial.
                Runnable accionPostCarga = null; // Desactivado el auto-restore al inicio

                // 2. Llama a la carga de datos iniciales, pasándole la acción de recuperación
                // como callback.
                cargarDatosIniciales(accionPostCarga);

                logger.info("--- [Fase 3/3] Inicialización de UI completada. La aplicación está lista. ---");

            } catch (Exception e) {
                manejarErrorFatalInicializacion("[EDT] Error fatal durante la creación de la UI", e);
            }
        });
    } // --- FIN de metodo/clase initializeApplication ---

    
    private void aplicarConfiguracionAlModelo() {
        logger.debug("  -> Aplicando Configuración a los contextos del Modelo...");
        if (this.configuration == null || this.model == null)
            return;

        boolean mantenerProp = configuration.getBoolean("interfaz.menu.zoom.mantener_proporciones.seleccionado", true);
        boolean incluirSubcarpetas = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_CARGAR_SUBCARPETAS, true);
        boolean soloCarpeta = !incluirSubcarpetas;
        boolean navCircular = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_NAVEGACION_CIRCULAR, false);
        boolean zoomManualInicial = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_ZOOM_MANUAL_INICIAL, true);
        boolean zoomAlCursor = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_ZOOM_AL_CURSOR_ACTIVADO, false);
        int saltoBloque = configuration.getInt(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE, 10);

        ZoomModeEnum modoZoomInicial;
        try {
            modoZoomInicial = ZoomModeEnum.valueOf(
                    configuration.getString(ConfigKeys.COMPORTAMIENTO_ZOOM_ULTIMO_MODO, "FIT_TO_SCREEN").toUpperCase());
        } catch (IllegalArgumentException e) {
            modoZoomInicial = ZoomModeEnum.FIT_TO_SCREEN;
        }

        this.model.initializeContexts(mantenerProp, soloCarpeta, modoZoomInicial, zoomManualInicial, navCircular,
                zoomAlCursor);
        this.model.setMiniaturasAntes(configuration.getInt(ConfigKeys.MINIATURAS_CANTIDAD_ANTES, 8));
        this.model.setMiniaturasDespues(configuration.getInt(ConfigKeys.MINIATURAS_CANTIDAD_DESPUES, 8));
        this.model.setMiniaturaSelAncho(configuration.getInt(ConfigKeys.MINIATURAS_TAMANO_SEL_ANCHO, 60));
        this.model.setMiniaturaSelAlto(configuration.getInt(ConfigKeys.MINIATURAS_TAMANO_SEL_ALTO, 60));
        this.model.setMiniaturaNormAncho(configuration.getInt(ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, 40));
        this.model.setMiniaturaNormAlto(configuration.getInt(ConfigKeys.MINIATURAS_TAMANO_NORM_ALTO, 40));
        this.model.setSaltoDeBloque(saltoBloque);
        this.model.setModoPantallaCompletaActivado(
                configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_PANTALLA_COMPLETA, false));
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
    } // --- FIN de metodo/clase aplicarConfiguracionAlModelo ---

    private void instalarPreviewers() {
        JList<String> miniaturasList = registry.get("list.miniaturas");
        if (miniaturasList != null) {
            this.activePreviewers.add(
                    new ThumbnailPreviewer(miniaturasList, this.model, this.themeManager, this.viewManager, registry));
            logger.debug("  -> Previsualizador de doble clic instalado en 'list.miniaturas'.");
        } else {
            logger.warn("WARN: No se pudo instalar ThumbnailPreviewer, 'list.miniaturas' no encontrada.");
        }

        JList<String> gridList = registry.get("list.grid");
        if (gridList != null) {
            // El preview del grid lo maneja GridCellRenderer internamente
            logger.debug("  -> Previsualizador de doble clic integrado en GridCellRenderer.");
        } else {
            logger.warn("WARN: No se pudo instalar ThumbnailPreviewer, 'list.grid' no encontrada.");
        }

        JList<String> projectGridList = registry.get("list.grid.proyecto");
        if (projectGridList != null) {
            this.activePreviewers.add(
                    new ThumbnailPreviewer(projectGridList, this.model, this.themeManager, this.viewManager, registry));
            logger.debug("  -> Previsualizador de doble clic instalado en 'list.grid.proyecto'.");
        } else {
            logger.warn("WARN: No se pudo instalar ThumbnailPreviewer, 'list.grid.proyecto' no encontrada.");
        }

        // --- Previewer para el grid del Modo Datos ---
        JList<String> dataGridList = registry.get("list.datamode.grid");
        if (dataGridList != null) {
            this.activePreviewers.add(
                    new ThumbnailPreviewer(dataGridList, this.model, this.themeManager, this.viewManager, registry));
            logger.debug("  -> Previsualizador de doble clic instalado en 'list.datamode.grid'.");
        } else {
            logger.warn("WARN: No se pudo instalar ThumbnailPreviewer, 'list.datamode.grid' no encontrada.");
        }
    } // --- FIN de metodo/clase instalarPreviewers ---

    private void configurarCierreVentana() {
        // --- INICIO DE LA MODIFICACIÓN ---
        // Establecemos la operación de cierre por defecto a NO HACER NADA.
        // Esto nos da el control total sobre el proceso de cierre.
        this.view.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        // --- FIN DE LA MODIFICACIÓN ---

        this.view.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                // Delegamos toda la lógica de cierre al método orquestador del controlador.
                controller.shutdownApplication();
            }
        });
    } // --- FIN de metodo/clase configurarCierreVentana ---

    private void sincronizarVisibilidadInicialUI() {
        logger.debug("  -> Sincronizando visibilidad inicial de paneles...");
        for (Action action : this.actionMap.values()) {
            if (action instanceof controlador.actions.config.ToggleUIElementVisibilityAction) {
                boolean isVisible = Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY));
                if (!isVisible) {
                    String actionCommand = (String) action.getValue(Action.ACTION_COMMAND_KEY);
                    action.actionPerformed(new java.awt.event.ActionEvent(this,
                            java.awt.event.ActionEvent.ACTION_PERFORMED, actionCommand));
                }
            }
        }
        // Sincronizar botón menú: si el menú está visible, el botón debe ocultarse
        Action menuBarAction = this.actionMap.get(AppActionCommands.CMD_VISTA_TOGGLE_MENU_BAR);
        if (menuBarAction instanceof controlador.actions.config.ToggleUIElementVisibilityAction) {
            boolean menuVisible = Boolean.TRUE.equals(menuBarAction.getValue(Action.SELECTED_KEY));
            this.viewManager.setBotonMenuEspecialVisible(!menuVisible);
        }

        // El botón de desbordamiento siempre empieza oculto
        this.viewManager.setSpecialButtonVisibleByCommand(
                AppActionCommands.CMD_ESPECIAL_BOTONES_OCULTOS, false, true);
    } // --- FIN de metodo/clase sincronizarVisibilidadInicialUI ---

    private void cargarDatosIniciales(Runnable onComplete) {
        boolean restaurarUltimaImagen = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_RESTAURAR_ULTIMA_IMAGEN, true);
        String imagenInicialKey = restaurarUltimaImagen
                ? configuration.getString(ConfigKeys.INICIO_IMAGEN, null) : null;

        if (this.model.getCarpetaRaizActual() != null) {
            boolean mostrarBienvenida = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_MOSTRAR_BIENVENIDA, true);
            if (mostrarBienvenida) {
                // Splash de bienvenida: mostrar la imagen de bienvenida de inmediato
                this.viewManager.limpiarUI();
                this.viewManager.restaurarVisibilidadPaneles();
                this.generalController.sincronizarTodaLaUIConElModelo();
                // Después del splash, cargar los datos
                javax.swing.Timer splashTimer = new javax.swing.Timer(2000, e -> {
                    // limpiarUI dejó el flag de sincronización activado (lo limpia asíncronamente)
                    this.controller.getListCoordinator().setSincronizandoUI(false);
                    this.controller.getImageListManager().cargarListaImagenes(imagenInicialKey, () -> {
                        if (onComplete != null) onComplete.run();
                    });
                });
                splashTimer.setRepeats(false);
                splashTimer.start();
            } else {
                this.controller.getImageListManager().cargarListaImagenes(imagenInicialKey, onComplete);
            }
        } else {
            logger.debug("  -> No hay carpeta inicial válida. Se mostrará el estado de bienvenida.");
            this.viewManager.limpiarUI();
            this.generalController.sincronizarTodaLaUIConElModelo();
            if (this.controller.getStatusBarManager() != null) {
                this.controller.getStatusBarManager()
                        .mostrarMensaje("Abre una carpeta para empezar (Archivo -> Abrir Carpeta)");
            }
            if (onComplete != null) {
                onComplete.run();
            }
        }
    } // --- FIN de metodo/clase cargarDatosIniciales ---

    /**
     * Comprueba si existe una sesión de recuperación pendiente y, de ser así,
     * pregunta al usuario si desea restaurarla. Esta lógica se ejecuta
     * después de que la UI principal se ha inicializado.
     * CRUCIAL: Limpia la clave de recuperación de la configuración después de
     * leerla
     * para evitar que vuelva a preguntar en el siguiente inicio.
     */
    private void comprobarYRestaurarSesion() {
        logger.debug("--- [AppInitializer] Comprobando si hay una sesión para restaurar... ---");

        // Obtenemos la ruta DEL ARCHIVO DE RECUPERACIÓN guardada en el config.
        String rutaRecuperacionStr = this.configuration.getString(ConfigKeys.PROYECTO_RECUPERACION_PENDIENTE, null); // Usaremos
                                                                                                                     // una
                                                                                                                     // nueva
                                                                                                                     // clave

        // Limpiamos la clave INMEDIATAMENTE después de leerla.
        if (rutaRecuperacionStr != null) {
            this.configuration.setString(ConfigKeys.PROYECTO_RECUPERACION_PENDIENTE, "");
            try {
                this.configuration.guardarConfiguracion(this.configuration.getConfig());
            } catch (java.io.IOException e) {
                logger.error("Error al limpiar la clave de recuperación en la configuración.", e);
            }
        }

        if (rutaRecuperacionStr != null && !rutaRecuperacionStr.isBlank()) {
            Path rutaRecuperacion = Paths.get(rutaRecuperacionStr);

            if (Files.exists(rutaRecuperacion)) {
                logger.info("  -> Se ha detectado un archivo de sesión de recuperación pendiente en: {}",
                        rutaRecuperacion);

                int respuesta = JOptionPane.showConfirmDialog(
                        null,
                        "La sesión anterior no se guardó correctamente.\n¿Deseas restaurar el trabajo no guardado?",
                        "Restaurar Sesión Anterior",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                if (respuesta == JOptionPane.YES_OPTION) {
                    logger.debug("    -> Usuario ha elegido restaurar la sesión.");
                    try {
                        this.projectManagerService.cargarDesdeRecuperacion(rutaRecuperacion);
                        this.generalController.cambiarModoDeTrabajo(VisorModel.WorkMode.PROYECTO);
                        this.generalController.actualizarTituloVentana();
                        logger.info("  -> Sesión restaurada con éxito.");
                    } catch (Exception e) {
                        logger.error("Error al intentar restaurar la sesión.", e);
                        JOptionPane.showMessageDialog(null, "No se pudo restaurar la sesión anterior.",
                                "Error de Recuperación", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    logger.debug("    -> Usuario eligió no restaurar. Borrando archivo de recuperación.");
                    try {
                        Files.deleteIfExists(rutaRecuperacion);
                    } catch (java.io.IOException e) {
                        logger.warn("No se pudo eliminar el archivo de recuperación: {}", e.getMessage());
                    }
                    this.projectManagerService.nuevoProyecto();
                }
            } else {
                logger.warn("  -> Archivo de recuperación no encontrado en la ruta guardada: {}. Inicio normal.",
                        rutaRecuperacionStr);
            }
        } else {
            logger.debug("  -> No se encontró clave de recuperación. Inicio normal.");
        }
    } // --- FIN de metodo/clase comprobarYRestaurarSesion ---

    /**
     * Maneja errores fatales durante la inicialización de la aplicación, mostrando un diálogo de error al usuario
     * y terminando la ejecución si es necesario.
     *
     * @param message Mensaje de error a mostrar
     * @param cause Excepción que causó el error
     */
    private void manejarErrorFatalInicializacion(String message, Throwable cause) {
        logger.error("### ERROR FATAL DE INICIALIZACIÓN ###");
        logger.error("Mensaje: " + message, cause);

        final String finalMessage = message + (cause != null ? "\n\nError Detallado: " + cause.getMessage() : "");

        Runnable showErrorDialog = () -> JOptionPane.showMessageDialog(null, finalMessage,
                "Error Fatal de Inicialización", JOptionPane.ERROR_MESSAGE);

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
    } // --- FIN de metodo/clase manejarErrorFatalInicializacion ---

} // --- FIN de metodo/clase AppInitializer ---
