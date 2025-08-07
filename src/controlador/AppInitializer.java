package controlador;

import java.awt.Color;
import java.awt.KeyboardFocusManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
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
import controlador.managers.InfobarImageManager;
import controlador.managers.InfobarStatusManager;
import controlador.managers.ToolbarManager;
import controlador.managers.ViewManager;
import controlador.managers.ZoomManager;
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

    private ProjectController projectController;
    private VisorModel model;
    private ConfigurationManager configuration;
    private IconUtils iconUtils;
    private ThumbnailService servicioMiniaturas;
    private ProjectManager projectManagerService;
    private VisorController controllerRefParaNotificacion; 
    private GeneralController generalController;
    private ConfigApplicationManager configAppManager;
    private VisorView view;
    private ListCoordinator listCoordinator;
    private ZoomManager zoomManager;
    private EditionManager editionManager; 
    private FileOperationsManager fileOperationsManager;
    private InfobarImageManager infobarImageManager; 
    private InfobarStatusManager statusBarManager;   
    private ToolbarManager toolbarManager;
    private ThemeManager themeManager;
    private ActionFactory actionFactory;
    private Map<String, Action> actionMap;
    private final VisorController controller;
	private ViewManager viewManager;
	private BackgroundControlManager backgroundControlManager;
	private CarouselManager carouselManager;
	private DisplayModeManager displayModeManager;
	

    public AppInitializer(VisorController controller) {
        this.controller = Objects.requireNonNull(controller, "VisorController no puede ser null en AppInitializer");
    } // --- fin del método AppInitializer (constructor) ---

    public boolean initialize() {
    	logger.info("--- Iniciando Proceso de Inicialización Global ---");

        try {
            if (!inicializarComponentesBase()) return false;
            aplicarConfiguracionAlModelo();
            if (!inicializarServiciosEsenciales()) return false;
            if (!inicializarServicioDeProyectos()) return false;

            logger.debug("  [AppInitializer] Programando creación de UI y componentes dependientes en EDT...");
            SwingUtilities.invokeLater(this::crearUIyComponentesDependientesEnEDT);

            logger.debug("--- Inicialización base (fuera de EDT) completada. ---");
            		
            return true;
        } catch (Exception e) {
            manejarErrorFatalInicializacion("Error fatal durante la inicialización base (fuera del EDT)", e);
            logger.error("Error fatal durante la inicialización base (fuera del EDT)", e);
            return false;
        }
    } // --- Fin del método initialize ---

    private boolean inicializarComponentesBase() {
        logger.info("  [AppInitializer Fase A.1] Inicializando Componentes Base (Modelo, Configuración)...");
        
        try {
            this.model = new VisorModel();
            this.controller.setModel(this.model);
            this.configuration = ConfigurationManager.getInstance();
            this.controller.setConfigurationManager(this.configuration);
            return true;
        } catch (Exception e) {
             manejarErrorFatalInicializacion("Error inesperado inicializando componentes base", e);
             logger.error("Error inesperado inicializando componentes base", e);
             
             return false;
        }
    } // --- fin del método inicializarComponentesBase ---

    private void aplicarConfiguracionAlModelo() {
        logger.info("  [AppInitializer Fase A.2] Aplicando Configuración a los contextos del Modelo...");
        
        if (this.configuration == null || this.model == null) return;
        
        boolean mantenerProp = configuration.getBoolean("interfaz.menu.zoom.mantener_proporciones.seleccionado", true);
        boolean incluirSubcarpetas = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_CARGAR_SUBCARPETAS, true);
        boolean soloCarpeta = !incluirSubcarpetas;
        boolean navCircular = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_NAVEGACION_CIRCULAR, false);
        boolean zoomManualInicial = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_ZOOM_MANUAL_INICIAL, true);
        boolean zoomAlCursor = configuration.getBoolean("comportamiento.zoom.al_cursor.activado", false);
        int saltoBloque = configuration.getInt(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE, 10);
        
        ZoomModeEnum modoZoomInicial;
        String ultimoModoStr = configuration.getString(ConfigKeys.COMPORTAMIENTO_ZOOM_ULTIMO_MODO, "FIT_TO_SCREEN").toUpperCase();
        try {
            modoZoomInicial = ZoomModeEnum.valueOf(ultimoModoStr);
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
        this.model.setMiniaturasAntes(configuration.getInt(ConfigKeys.MINIATURAS_CANTIDAD_ANTES, 8));
        
        boolean pantallaCompleta = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_PANTALLA_COMPLETA, false);
        this.model.setModoPantallaCompletaActivado(pantallaCompleta);
        
        int carouselDelay = configuration.getInt(ConfigKeys.CAROUSEL_DELAY_MS, 3000);
        this.model.setCarouselDelay(carouselDelay);
        
        
        // --- INICIO DE LÓGICA DE SINCRONIZACIÓN ---
        boolean syncActivado = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_SYNC_VISOR_CARRUSEL, false);
        this.model.setSyncVisualizadorCarrusel(syncActivado);

        String ultimaCarpetaStr = configuration.getString(ConfigKeys.CARRUSEL_ESTADO_ULTIMA_CARPETA, "");
        if (!ultimaCarpetaStr.isEmpty()) {
            try {
                this.model.setUltimaCarpetaCarrusel(Paths.get(ultimaCarpetaStr));
            } catch (java.nio.file.InvalidPathException e) {
                logger.error("WARN: Ruta de la última carpeta del carrusel inválida en config: " + ultimaCarpetaStr);            }
        }
        String ultimaImagenKey = configuration.getString(ConfigKeys.CARRUSEL_ESTADO_ULTIMA_IMAGEN, "");
        this.model.setUltimaImagenKeyCarrusel(ultimaImagenKey);
        
        
        logger.debug("  [Initializer] Estado de Sincronización cargado: " + syncActivado);
        
        // --- FIN DE LÓGICA DE SINCRONIZACIÓN ---
        
    } // --- fin del método aplicarConfiguracionAlModelo ---

    private boolean inicializarServiciosEsenciales() {
         
        logger.info ("  [AppInitializer Fase A.3] Inicializando Servicios Esenciales...");
         
         try {
        	 this.themeManager = new ThemeManager(this.configuration);
             this.themeManager.install();
             this.controller.setThemeManager(this.themeManager);
             this.iconUtils = new IconUtils(this.themeManager);
             this.controller.setIconUtils(this.iconUtils);
             this.servicioMiniaturas = new ThumbnailService();
             this.controller.setServicioMiniaturas(this.servicioMiniaturas);
             
             DefaultListModel<String> modeloParaMiniaturasJList = new DefaultListModel<>();
             
             int numThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
             this.controller.setExecutorService(Executors.newFixedThreadPool(numThreads));
             return true;
         } catch (Exception e) {
        	 
//             manejarErrorFatalInicializacion("Error inicializando servicios esenciales", e);
             logger.error("Error inicializando servicios esenciales", e);
             
             return false;
         }
    } // --- Fin del método inicializarServiciosEsenciales ---
    
    private boolean inicializarServicioDeProyectos() {
    	
        logger.info("  [AppInitializer Fase A.4] Creando instancia de ProjectManager...");
        
        try {
            this.projectManagerService = new ProjectManager();
            return true;
        } catch (Exception e) {
        	
            manejarErrorFatalInicializacion("Error creando la instancia de ProjectManager (servicio)", e);
            logger.error("Error creando la instancia de ProjectManager (servicio)", e);
            
            return false;
        }
    } // --- Fin del método inicializarServicioDeProyectos ---
    
    
    private void crearUIyComponentesDependientesEnEDT() {
        try {
            logger.info("--- [AppInitializer Fase B - EDT] Iniciando creación de UI y componentes dependientes ---");

            //======================================================================
            // FASE B.1: CREACIÓN DE TODAS LAS INSTANCIAS (SIN DEPENDENCIAS)
            // En esta fase, simplemente creamos los objetos. No los conectamos entre sí.
            //======================================================================
            
            logger.debug("  -> Fase B.1: Creando todas las instancias...");
            
            ComponentRegistry registry = new ComponentRegistry();
            this.generalController = new GeneralController();
            this.zoomManager = new ZoomManager();
            this.editionManager = new EditionManager();
            this.viewManager = new ViewManager();
            this.listCoordinator = new ListCoordinator();
            this.displayModeManager = new DisplayModeManager();
            this.fileOperationsManager = new FileOperationsManager();
            this.projectController = new ProjectController();
            ProjectListCoordinator projectListCoordinator = new ProjectListCoordinator(this.model, this.controller, registry);
            this.carouselManager = new CarouselManager(listCoordinator, this.controller, registry, this.model, this.iconUtils);
            ProjectBuilder projectBuilder = new ProjectBuilder(registry, this.model, this.themeManager, this.generalController);
            ViewBuilder viewBuilder = new ViewBuilder(
                registry, this.model, this.themeManager, this.configuration,
                this.iconUtils, this.servicioMiniaturas, projectBuilder
            );
            UIDefinitionService uiDefSvc = new UIDefinitionService();
            Map<String, ActionFactory.IconInfo> iconMap = new java.util.HashMap<>();
            uiDefSvc.generateModularToolbarStructure().forEach(toolbarDef -> {
                for (ToolbarComponentDefinition compDef : toolbarDef.componentes()) {
                    if (compDef instanceof ToolbarButtonDefinition buttonDef) {
                        iconMap.put(buttonDef.comandoCanonico(), new ActionFactory.IconInfo(buttonDef.claveIcono(), buttonDef.scopeIconoBase()));
                    }
                }
            });
            this.actionFactory = new ActionFactory(
                 this.model, null, this.zoomManager, this.fileOperationsManager, 
                 this.editionManager, this.listCoordinator, this.iconUtils, this.configuration, 
                 this.projectManagerService, iconMap, this.viewManager, this.themeManager, 
                 this.generalController, this.projectController
            );
            this.configAppManager = new ConfigApplicationManager(this.model, this.configuration, this.themeManager, registry);
            this.infobarImageManager = new InfobarImageManager(this.model, registry, this.configuration);
            MenuBarBuilder menuBuilder = new MenuBarBuilder(this.controller, this.configuration, this.viewManager, registry);
            ToolbarBuilder toolbarBuilder = new ToolbarBuilder(
                    this.themeManager, this.iconUtils, this.controller,
                    configuration.getInt("iconos.ancho", 24),
                    configuration.getInt("iconos.alto", 24),
                    registry
            );
            this.toolbarManager = new ToolbarManager(registry, this.configuration, toolbarBuilder, uiDefSvc, this.model);
            this.backgroundControlManager = new BackgroundControlManager(
                    registry, this.themeManager, this.viewManager, this.configuration, this.iconUtils,
                    BorderFactory.createLineBorder(Color.GRAY),
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.CYAN, 2),
                        BorderFactory.createLineBorder(Color.BLACK)
                    )
            );
            
            //======================================================================
            // FASE B.2: CABLEADO DE DEPENDENCIAS (SETS)
            // Aquí, conectamos los objetos entre sí usando sus métodos `set...`.
            //======================================================================
            
            logger.debug("  -> Fase B.2: Cableando dependencias (sets)...");
            
            // Suscripción a eventos de cambio de tema
            this.themeManager.addThemeChangeListener(this.toolbarManager); 
            this.themeManager.addThemeChangeListener(this.controller);
            this.themeManager.addThemeChangeListener(this.viewManager);
            this.themeManager.addThemeChangeListener(this.displayModeManager);
            this.themeManager.addThemeChangeListener(this.infobarImageManager);
            this.themeManager.addThemeChangeListener(projectBuilder);
            this.themeManager.addThemeChangeListener(this.backgroundControlManager);
            
            // Inyección de dependencias en Managers y Controladores principales
            viewManager.setModel(this.model);
            viewManager.setRegistry(registry);
            viewManager.setConfiguration(this.configuration);
            viewManager.setThemeManager(this.themeManager);
            viewManager.setToolbarManager(this.toolbarManager);
            viewManager.setViewBuilder(viewBuilder);
            
            zoomManager.setModel(this.model);
            zoomManager.setRegistry(registry);
            zoomManager.setConfiguration(this.configuration);
            zoomManager.setViewManager(this.viewManager);

            this.controller.setViewManager(this.viewManager);
            this.controller.setComponentRegistry(registry);
            
            if (this.projectManagerService != null) {
                this.projectManagerService.setConfigManager(this.configuration);
                this.controller.setProjectManager(this.projectManagerService);
            }
            this.editionManager.setModel(this.model);
            this.editionManager.setController(this.controller);
            this.editionManager.setZoomManager(this.zoomManager);
            
            this.listCoordinator.setModel(this.model);
            this.listCoordinator.setController(this.controller);
            this.listCoordinator.setRegistry(registry);
            
            this.projectController.setProjectManager(this.projectManagerService);
            this.projectController.setViewManager(this.viewManager);
            this.projectController.setRegistry(registry);
            this.projectController.setZoomManager(this.zoomManager); 
            this.projectController.setModel(this.model); 
            this.projectController.setController(this.controller);
            this.projectController.setListCoordinator(this.listCoordinator);
            this.projectController.setProjectListCoordinator(projectListCoordinator);
            
            this.generalController.setModel(this.model);
            this.generalController.setViewManager(this.viewManager);
            this.generalController.setVisorController(this.controller);
            this.generalController.setProjectController(this.projectController);
            this.generalController.setConfigApplicationManager(this.configAppManager);
            this.generalController.setRegistry(registry);
            this.generalController.setToolbarManager(this.toolbarManager);
            this.generalController.setDisplayModeManager(this.displayModeManager);
            this.generalController.setConfiguration(this.configuration);
            
            java.util.function.Consumer<java.nio.file.Path> onFolderSelectedCallback = (p) -> this.controller.cargarListaImagenes(null, null);
            this.fileOperationsManager.setModel(this.model);
            this.fileOperationsManager.setController(this.controller);
            this.fileOperationsManager.setConfiguration(this.configuration);
            this.fileOperationsManager.setOnNuevaCarpetaSeleccionadaCallback(onFolderSelectedCallback);

            // Inyecciones finales en el controlador principal y otros componentes
            this.controller.setToolbarManager(this.toolbarManager);
            this.controller.setActionFactory(this.actionFactory);
            this.controller.setBackgroundControlManager(this.backgroundControlManager);
            this.themeManager.setConfigApplicationManager(this.configAppManager);
            viewBuilder.setToolbarManager(this.toolbarManager);
            menuBuilder.setControllerGlobalActionListener(this.controller);
            this.configAppManager.setBackgroundControlManager(this.backgroundControlManager);
            this.toolbarManager.setBackgroundControlManager(this.backgroundControlManager);

            //======================================================================
            // FASE B.3: INICIALIZACIÓN SECUENCIADA Y CONSTRUCCIÓN DE LA UI
            // Este es el paso más delicado. Creamos la UI a mitad de camino para
            // resolver el "problema del huevo y la gallina" con las dependencias.
            //======================================================================
            
            logger.debug("  -> Fase B.3: Inicialización secuenciada y construcción de UI...");
            
            
            // Paso 3.1: Inicializar las Actions que NO dependen de la 'view'.
            this.actionFactory.initializeCoreActions();
            this.actionMap = this.actionFactory.getActionMap();
            
            // Paso 3.2: Inyectar el ActionMap en los builders.
            toolbarBuilder.setActionMap(this.actionMap);
            viewBuilder.setActionMap(this.actionMap);

            // Paso 3.3: Crear el frame principal (VisorView). ¡ESTE ES EL PASO CLAVE!
            this.view = viewBuilder.createMainFrame();
            
            logger.debug("    -> VisorView (JFrame) creado.");

            // Paso 3.4: Inyectar la 'view' en todos los componentes que la necesitaban.
            logger.debug("    -> Inyectando la instancia de 'view' en los componentes...");
            
            this.actionFactory.setView(this.view);
            this.configAppManager.setView(this.view);
            this.viewManager.setView(this.view);
            this.controller.setView(this.view);
            this.projectController.setView(this.view);

            // Paso 3.5: Inicializar las Actions que SÍ dependen de la 'view'.
            this.actionFactory.initializeViewDependentActions();
            this.actionFactory.setCarouselManager(carouselManager);

            // Paso 3.6: Cableado final del DisplayModeManager (AHORA que la UI existe).
            this.displayModeManager.setModel(this.model);
            this.displayModeManager.setViewManager(this.viewManager);
            this.displayModeManager.setRegistry(registry);
            this.displayModeManager.setListCoordinator(this.listCoordinator);
            this.displayModeManager.setActionMap(this.actionMap);
            this.displayModeManager.setConfiguration(this.configuration);
            this.displayModeManager.setThemeManager(this.themeManager);
            this.displayModeManager.setToolbarManager(this.toolbarManager);
            this.displayModeManager.setConfigApplicationManager(this.configAppManager);
            this.viewManager.setDisplayModeManager(this.displayModeManager); 
            this.actionFactory.setDisplayModeManager(this.displayModeManager);
            this.controller.setDisplayModeManager(this.displayModeManager);
            this.displayModeManager.initializeListeners();
            
            logger.debug("    -> DisplayModeManager configurado y conectado.");
            logger.debug("    -> DisplayModeManager configurado y conectado.");

            // Paso 3.7: Resto del cableado y configuración final.
            if (this.projectManagerService != null) { this.projectManagerService.initialize(); }
            this.configAppManager.setActionMap(this.actionMap);
            this.viewManager.setActionMap(this.actionMap);
            this.controller.setActionMap(this.actionMap);
            this.projectController.setActionMap(this.actionMap);
            this.generalController.setActionMap(this.actionMap);
            this.statusBarManager = new InfobarStatusManager(this.model, registry, this.themeManager, this.configuration, this.projectManagerService, this.actionMap, this.iconUtils);
            this.statusBarManager.setController(this.controller);
            this.controller.setZoomManager(this.zoomManager);
            this.controller.setListCoordinator(this.listCoordinator);
            this.controller.setConfigApplicationManager(configAppManager);
            this.controller.setInfobarImageManager(infobarImageManager);
            this.controller.setStatusBarManager(statusBarManager);
            this.zoomManager.setStatusBarManager(this.statusBarManager);
            this.zoomManager.setListCoordinator(this.listCoordinator);
            this.listCoordinator.setContextSensitiveActions(this.actionFactory.getContextSensitiveActions());
            this.generalController.setStatusBarManager(this.statusBarManager);
            projectListCoordinator.setContextSensitiveActions(this.actionFactory.getContextSensitiveActions());
            this.displayModeManager.setInfobarStatusManager(this.statusBarManager);
            
            //======================================================================
            // FASE B.4: ENSAMBLAJE FINAL, VISIBILIDAD Y CARGA DE DATOS
            //======================================================================
            logger.debug("  -> Fase B.4: Ensamblaje final, visibilidad y carga de datos...");

            // Construir y registrar Menú y Toolbars
            this.view.setJMenuBar(menuBuilder.buildMenuBar(uiDefSvc.generateMenuStructure(), this.actionMap));
            this.controller.setMenuItemsPorNombre(menuBuilder.getMenuItemsMap());
            menuBuilder.getMenuItemsMap().forEach(registry::register);
            this.toolbarManager.reconstruirContenedorDeToolbars(this.model.getCurrentWorkMode());
            this.controller.setBotonesPorNombre(toolbarBuilder.getBotonesPorNombre());
            this.viewManager.setBotonesPorNombre(toolbarBuilder.getBotonesPorNombre());
            
            // Cablear los modelos de las JList
            JList<String> miniaturasVisor = registry.get("list.miniaturas");
            if (miniaturasVisor != null) { miniaturasVisor.setModel(this.controller.getModeloMiniaturasVisualizador()); }
            JList<String> miniaturasCarrusel = registry.get("list.miniaturas.carousel");
            if (miniaturasCarrusel != null) { miniaturasCarrusel.setModel(this.controller.getModeloMiniaturasCarrusel()); }

            // Configurar listeners globales y específicos
            this.controller.configurarAtajosTecladoGlobales();
            this.generalController.configurarListenersDeEntradaGlobal();
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this.generalController);
            this.controller.configurarListenersVistaInternal(); 
            this.controller.configurarMenusContextuales();
            this.projectController.configurarListeners();
            this.generalController.initialize();
            
            // Instalar previsualizador de miniaturas
            JList<String> miniaturasList = registry.get("list.miniaturas");
            if (miniaturasList != null) {
                new ThumbnailPreviewer(miniaturasList, this.model, this.themeManager);
            } else {
            	
                logger.warn("WARN: No se pudo instalar ThumbnailPreviewer, 'list.miniaturas' no encontrada.");
            }
            
            // Configurar el listener de cierre de la ventana
            this.view.addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent e) { controller.shutdownApplication(); }
            });
            
            // Determinar si hay una carpeta inicial para cargar
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
            
            // Hacer visible la ventana. A partir de aquí, la aplicación está "viva" para el usuario.
            this.view.setVisible(true);

            // Aplicar configuraciones y sincronizar estados que dependen de una UI visible
            this.configAppManager.aplicarConfiguracionGlobalmente();
            
            logger.debug("  [AppInitializer] Ejecutando sincronización maestra INICIAL...");
            
            this.generalController.sincronizarTodaLaUIConElModelo();
            this.backgroundControlManager.initializeAndLinkControls();
            this.backgroundControlManager.sincronizarSeleccionConEstadoActual();
            
            logger.debug("    -> BackgroundControlManager inicializado y enlazado a la UI.");
            
            // Cargar datos iniciales en un hilo separado para no bloquear la UI
            SwingUtilities.invokeLater(() -> {
                String imagenInicialKey = configuration.getString(ConfigKeys.INICIO_IMAGEN, null);
                
                // Creamos un callback que se ejecutará DESPUÉS de que la carga de imágenes termine.
                Runnable accionPostCarga = () -> {
                	
                    logger.debug("  [Callback Post-Carga] Carga inicial completada. Aplicando estado final de UI...");
                    
                    // >>> LA SOLUCIÓN <<<
                    // Este es el último paso. Después de que las imágenes se han cargado y la selección inicial
                    // se ha procesado (lo que podría haber cambiado la vista incorrectamente), forzamos
                    // a la UI a mostrar el modo de visualización correcto que leímos de la configuración.
                    if (this.displayModeManager != null) {
                    	
                        logger.debug("    -> [Callback] Sincronizando DisplayMode final a: " + this.model.getCurrentDisplayMode());
                        
                        this.displayModeManager.switchToDisplayMode(VisorModel.DisplayMode.SINGLE_IMAGE);
                    }
                    
                    // Refrescar las listas visuales
                    
                    logger.debug("    -> [Callback] Forzando refresco de las listas visuales...");
                    
                    if (registry != null) {
                        JList<String> listaNombres = registry.get("list.nombresArchivo");
                        if (listaNombres != null) { listaNombres.repaint(); }
                        if (this.displayModeManager != null) { this.displayModeManager.poblarYSincronizarGrid(); }
                    }
                };
                
                // Decidir si cargar imágenes o simplemente limpiar y sincronizar
                if (this.model.getCarpetaRaizActual() != null) {
                    this.controller.cargarListaImagenes(imagenInicialKey, accionPostCarga);
                } else {
                    this.controller.limpiarUI();
                    
                    logger.debug("  [AppInitializer] No hay carpeta inicial. Sincronizando UI maestra...");
                    
                    this.generalController.sincronizarTodaLaUIConElModelo();
                    // Incluso si no hay imágenes, establece el modo de visualización correcto.
                    this.displayModeManager.switchToDisplayMode(this.model.getCurrentDisplayMode());
                }
            });
            
            logger.info("--- [AppInitializer Fase B - EDT] Inicialización de UI completada. La aplicación está lista. ---");

        } catch (Exception e) {
            manejarErrorFatalInicializacion("[EDT] Error fatal durante la creación de la UI", e);
        }
    } // --- FIN del método crearUIyComponentesDependientesEnEDT ---
    
    
    private void manejarErrorFatalInicializacion(String message, Throwable cause) {
        logger.error("### ERROR FATAL DE INICIALIZACIÓN ###");
        logger.error("Mensaje: " + message);
        
        if (cause != null) {
            logger.error("Causa: " + cause.toString());
            
            cause.printStackTrace(System.err);
        }
        logger.error("#####################################");

        final String finalMessage = message;
        final Throwable finalCause = cause;
        
        if (SwingUtilities.isEventDispatchThread()) {
             JOptionPane.showMessageDialog(null, finalMessage + (finalCause != null ? "\n\nError Detallado: " + finalCause.getMessage() : ""), "Error Fatal de Inicialización", JOptionPane.ERROR_MESSAGE);
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> {
                     JOptionPane.showMessageDialog(null, finalMessage + (finalCause != null ? "\n\nError Detallado: " + finalCause.getMessage() : ""), "Error Fatal de Inicialización", JOptionPane.ERROR_MESSAGE);
                });
            } catch (Exception swingEx) {
                 
                logger.error ("Error adicional al intentar mostrar el diálogo de error en EDT: " + swingEx.getMessage());
                
                swingEx.printStackTrace(System.err);
            }
        }
        
        logger.error("Terminando la aplicación debido a un error fatal de inicialización.");
        
        System.exit(1); 
    } // --- FIN del método manejarErrorFatalInicializacion ---

    public void setControllerParaNotificacion(VisorController controller) {
        this.controllerRefParaNotificacion = controller;
    } // --- FIN del método setControllerParaNotificacion ---
    
} // --- FIN de la clase AppInitializer ---

