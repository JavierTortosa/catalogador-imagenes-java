package controlador;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import controlador.factory.ActionFactory; // La fábrica de Actions
import controlador.managers.ConfigApplicationManager;
import controlador.managers.EditionManager;
import controlador.managers.FileOperationsManager; // Futuro
import controlador.managers.InfobarImageManager;
import controlador.managers.InfobarStatusManager;
import controlador.managers.ToolbarManager;
import controlador.managers.ViewManager;
// Managers
import controlador.managers.ZoomManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.ProjectManager; // Asumiendo que este es tu servicio de proyectos
import servicios.image.ThumbnailService;
import servicios.zoom.ZoomModeEnum;
import vista.VisorView;
import vista.builders.MenuBarBuilder;
import vista.builders.ProjectBuilder;
import vista.builders.ToolbarBuilder;
import vista.builders.ViewBuilder;
import vista.config.UIDefinitionService;
import vista.theme.ThemeManager;
import vista.util.IconUtils;

public class AppInitializer {

    // --- SECCIÓN 0: CONSTANTES ---
    private static final int WINDOW_RESIZE_UPDATE_DELAY_MS = 250;

    // --- SECCIÓN 1: REFERENCIAS A COMPONENTES PRINCIPALES DE LA APLICACIÓN ---
    // Estos campos almacenarán las instancias creadas durante la inicialización.

    // 1.1. Modelo y Servicios Base (creados fuera del EDT)
    private VisorModel model;
    private ConfigurationManager configuration;
    private IconUtils iconUtils;
    private ThumbnailService servicioMiniaturas;
    private ProjectManager projectManagerService; // Servicio de persistencia de proyectos
    private VisorController controllerRefParaNotificacion; 
    private ConfigApplicationManager configAppManager;
    
    // 1.2. Componentes de UI y Coordinadores (creados DENTRO del EDT)
    private VisorView view;
    private ListCoordinator listCoordinator;
    
    // 1.3. Managers (creados DENTRO del EDT, después de la Vista si dependen de ella)
    private ZoomManager zoomManager;
    private EditionManager editionManager; 
    private FileOperationsManager fileOperationsManager;
    private InfobarImageManager infobarImageManager; 
    private InfobarStatusManager statusBarManager;   
    private boolean zoomManualEstabaActivoAntesDeError = false;
    private ToolbarManager toolbarManager;

    private ThemeManager themeManager;
//    private ThumbnailService thumbnailService;

    
    // 1.4. Fábrica de Acciones y Mapa de Acciones (creados DENTRO del EDT)
    private ActionFactory actionFactory;
    private Map<String, Action> actionMap;

    // 1.5. Referencia al Controlador Principal
    //      Este es el VisorController al que este AppInitializer está ayudando a inicializar.
    private final VisorController controller;

	private ViewManager viewManager;

    /**
     * Constructor de AppInitializer.
     * @param controller La instancia de VisorController que será inicializada.
     */
    public AppInitializer(VisorController controller) {
        this.controller = Objects.requireNonNull(controller, "VisorController no puede ser null en AppInitializer");
    }

    /**
     * Orquesta el proceso de inicialización completo de la aplicación.
     * Separa la lógica de arranque en fases.
     * @return true si la inicialización base (pre-UI) fue exitosa, false si ocurrió un error fatal.
     *         La UI y los componentes dependientes se inicializan de forma asíncrona en el EDT.
     */
    public boolean initialize() {
        System.out.println("--- AppInitializer: Iniciando Proceso de Inicialización Global ---");

        try {
            // --- FASE A: INICIALIZACIÓN FUERA DEL EVENT DISPATCH THREAD (EDT) ---
            //          Componentes que no requieren que la UI exista aún.

            // A.1. Inicializar Componentes Base (Modelo, Configuración)
            if (!inicializarComponentesBase()) return false;

            // A.2. Aplicar Configuración Leída al Modelo
            aplicarConfiguracionAlModelo();

            // A.3. Inicializar Servicios Esenciales (Tema, Iconos, Hilos, Servicio de Miniaturas)
            if (!inicializarServiciosEsenciales()) return false;
            
            // A.4. Inicializar Servicio de Proyectos (si no depende de la UI)
            //      Si ProjectManager solo necesita ConfigurationManager, puede ir aquí.
            //      Si necesita Model o View, se mueve al EDT.
            //      Por ahora lo ponemos aquí asumiendo que solo necesita 'configuration'.
            if (!inicializarServicioDeProyectos()) return false;


            // --- FASE B: INICIALIZACIÓN DENTRO DEL EVENT DISPATCH THREAD (EDT) ---
            //          Componentes de UI y aquellos que dependen de la UI.
            //          Se usa invokeLater para asegurar que se ejecuten en el EDT.
            System.out.println("  [AppInitializer] Programando creación de UI y componentes dependientes en EDT...");
            SwingUtilities.invokeLater(this::crearUIyComponentesDependientesEnEDT);

            System.out.println("--- AppInitializer: Inicialización base (fuera de EDT) completada. ---");
            
            return true; // Indica éxito de la inicialización base.

        } catch (Exception e) {
            // Captura errores inesperados durante las fases iniciales (fuera del EDT).
            manejarErrorFatalInicializacion("Error fatal durante la inicialización base (fuera del EDT)", e);
            return false; // Indica fallo.
        }
        
        
    } // --- FIN del método initialize ---


    // --- MÉTODOS DE INICIALIZACIÓN POR FASE (FUERA DEL EDT) ---

    /**
     * FASE A.1: Inicializa los componentes fundamentales: VisorModel y ConfigurationManager.
     *           Inyecta estas dependencias en el VisorController.
     * @return true si la inicialización de esta fase fue exitosa, false si falló.
     */
    private boolean inicializarComponentesBase() {
        System.out.println("  [AppInitializer Fase A.1] Inicializando Componentes Base (Modelo, Configuración)...");
        try {
            // A.1.1. Crear el Modelo de Datos Principal
            this.model = new VisorModel();
            this.controller.setModel(this.model); // Inyectar en VisorController
            System.out.println("    -> VisorModel creado e inyectado.");

            // A.1.2. Crear y Cargar el Gestor de Configuración
            
        // ---------------------------------------------------------------------------------------- singleton de configurationmanager
            this.configuration = ConfigurationManager.getInstance();
        // ----------------------------------------------------------------------------------------
            
            this.controller.setConfigurationManager(this.configuration); // Inyectar en VisorController
            System.out.println("    -> ConfigurationManager creado, cargado e inyectado.");

            return true; // Éxito de la fase
            
        } catch (Exception e) {
             manejarErrorFatalInicializacion("Error inesperado inicializando componentes base", e);
             return false;
        }
    }

    /**
     * FASE A.2: Lee valores de la configuración (cargada en Fase A.1) y los establece
     *           en el VisorModel.
     *           Esto asegura que el modelo tenga los datos correctos antes de que
     *           cualquier otro componente intente usarlos.
     */
    private void aplicarConfiguracionAlModelo() {
        System.out.println("  [AppInitializer Fase A.2] Aplicando Configuración al Modelo...");
        if (this.configuration == null || this.model == null) { // Validar dependencias de esta fase
            System.err.println("ERROR [AppInitializer.aplicarConfigAlModelo]: Configuración o Modelo nulos. Saltando fase.");
            // Considerar esto un error fatal si estas dependencias son cruciales.
            return;
        }
        
        // Aplicar Estado Inicial de Zoom Manual
        String initialZoomModeStr = this.configuration.getString(ConfigKeys.COMPORTAMIENTO_ZOOM_MODO_INICIAL, "FIT_TO_SCREEN").toUpperCase();
        try {
            ZoomModeEnum initialMode = ZoomModeEnum.valueOf(initialZoomModeStr);
            this.model.setCurrentZoomMode(initialMode);
            System.out.println("    -> Modo de zoom inicial del modelo (comportamiento.display.zoom.initial_mode) establecido desde config a: " + initialMode);
        } catch (IllegalArgumentException e) {
            System.err.println("WARN [AppInitializer]: Valor inválido para '" + 
            		ConfigKeys.COMPORTAMIENTO_ZOOM_MODO_INICIAL + 
                               "' en config: '" + initialZoomModeStr + 
                               "'. Usando FIT_TO_SCREEN por defecto.");
            this.model.setCurrentZoomMode(ZoomModeEnum.FIT_TO_SCREEN);
        }
        
        // Aplicar Estado Inicial de Tipo de Zoom
        String ultimoModoZoomStr = this.configuration.getString(
        		ConfigKeys.COMPORTAMIENTO_ZOOM_ULTIMO_MODO, "FIT_TO_SCREEN").toUpperCase();
        	ZoomModeEnum modoAEstablecer;
        	try {
        	    modoAEstablecer = ZoomModeEnum.valueOf(ultimoModoZoomStr);
        	} catch (IllegalArgumentException e) {
        	    System.err.println("WARN [AppInitializer]: Valor inválido para '" + 
        	    		ConfigKeys.COMPORTAMIENTO_ZOOM_ULTIMO_MODO +
        	                       "' en config: '" + ultimoModoZoomStr + 
        	                       "'. Usando FIT_TO_SCREEN por defecto.");
        	    modoAEstablecer = ZoomModeEnum.FIT_TO_SCREEN;
        	}
        	this.model.setCurrentZoomMode(modoAEstablecer);
        	System.out.println("    -> Modelo: Modo de zoom actual (desde ultimo_modo_seleccionado) establecido a: " + modoAEstablecer);
        
        try {
            // Aplicar configuración de miniaturas al modelo
            this.model.setMiniaturasAntes(configuration.getInt		(ConfigKeys.MINIATURAS_CANTIDAD_ANTES			, VisorController.DEFAULT_MINIATURAS_ANTES_FALLBACK));
            this.model.setMiniaturasDespues(configuration.getInt	(ConfigKeys.MINIATURAS_CANTIDAD_DESPUES			, VisorController.DEFAULT_MINIATURAS_DESPUES_FALLBACK));
            this.model.setMiniaturaSelAncho(configuration.getInt	(ConfigKeys.MINIATURAS_TAMANO_SEL_ANCHO	, 60));
            this.model.setMiniaturaSelAlto(configuration.getInt		(ConfigKeys.MINIATURAS_TAMANO_SEL_ALTO	, 60));
            this.model.setMiniaturaNormAncho(configuration.getInt	(ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO		, 40));
            this.model.setMiniaturaNormAlto(configuration.getInt	(ConfigKeys.MINIATURAS_TAMANO_NORM_ALTO		, 40));

            // Aplicar configuración de comportamiento al modelo
            boolean cargarSubcarpetas = configuration.getBoolean("comportamiento.carpeta.cargarSubcarpetas", true);
            boolean zoomManualInicialActivo = this.configuration.getBoolean(
            		ConfigKeys.COMPORTAMIENTO_ZOOM_MANUAL_INICIAL, true);// Valor por defecto si la clave no existe en el archivo (aunque debería estar en DEFAULT_CONFIG)
            this.model.setZoomHabilitado(zoomManualInicialActivo);
            System.out.println("    -> Modelo: Zoom manual inicial activo (desde config 'comportamiento...'): " + zoomManualInicialActivo);
            this.model.setMostrarSoloCarpetaActual(!cargarSubcarpetas); 
            boolean mantenerProp = configuration.getBoolean("interfaz.menu.zoom.mantener_proporciones.seleccionado", true); // Clave de config directa
            this.model.setMantenerProporcion(mantenerProp);
            boolean navCircular = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_NAVEGACION_CIRCULAR, false);
            this.model.setNavegacionCircularActivada(navCircular);
            
            //LOG Navegacion Circular
            System.out.println("    -> Modelo: Navegación circular (desde config 'comportamiento.navegacion.circular'): " + navCircular);
            System.out.println("    -> Configuración aplicada al Modelo (Miniaturas, Comportamiento).");
            
        } catch (Exception e) {
            System.err.println("ERROR [AppInitializer.aplicarConfigAlModelo]: " + e.getMessage());
            e.printStackTrace(); // Continuar si es posible, pero loguear el error.
        }
    }

    /**
     * FASE A.3: Inicializa servicios esenciales que no dependen directamente de la UI:
     *           ThemeManager, IconUtils, ExecutorService, y ThumbnailService.
     *           Inyecta estas dependencias en el VisorController.
     * @return true si la inicialización de esta fase fue exitosa, false si falló.
     */
    private boolean inicializarServiciosEsenciales() {
         System.out.println("  [AppInitializer Fase A.3] Inicializando Servicios Esenciales...");
         try {
             // A.3.1. ThemeManager (depende de ConfigurationManager)
             this.themeManager = new ThemeManager(this.configuration);
             this.controller.setThemeManager(this.themeManager);
             System.out.println("    -> ThemeManager creado e inyectado.");

             
             if (this.themeManager != null && this.controller != null) {
                 this.themeManager.setControllerParaNotificacion(this.controller);
             } else {
                 // Esto sería un error de inicialización muy grave
                 System.err.println("ERROR CRÍTICO en AppInitializer: ThemeManager o this.controller nulos al intentar inyección cruzada.");
             }
             
             
             // A.3.2. IconUtils (depende de ThemeManager)
             this.iconUtils = new IconUtils(this.themeManager);
             this.controller.setIconUtils(this.iconUtils);
             System.out.println("    -> IconUtils creado e inyectado.");

             // A.3.3. ThumbnailService (servicio para generar/cachear miniaturas)
             this.servicioMiniaturas = new ThumbnailService();
             this.controller.setServicioMiniaturas(this.servicioMiniaturas);
             // El DefaultListModel para las miniaturas se crea en el controller o aquí y se pasa.
             // VisorController ya tiene 'private DefaultListModel<String> modeloMiniaturas;'
             // Aquí lo instanciamos si no lo hace el controller, y se lo pasamos.
             // Por coherencia, AppInitializer lo crea y lo pasa.
             DefaultListModel<String> modeloParaMiniaturasJList = new DefaultListModel<>();
             this.controller.setModeloMiniaturas(modeloParaMiniaturasJList); // Inyectar el modelo en el controller
             System.out.println("    -> ThumbnailService y Modelo para JList de Miniaturas creados e inyectados.");

             // A.3.4. ExecutorService (para tareas en segundo plano)
             // Usar un pool de hilos de tamaño fijo, adaptable al número de procesadores.
             int numThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2); // Mínimo 2, o mitad de cores
             this.controller.setExecutorService(Executors.newFixedThreadPool(numThreads));
             System.out.println("    -> ExecutorService (FixedThreadPool con " + numThreads + " hilos) creado e inyectado.");

             return true; // Éxito de la fase
         } catch (Exception e) {
             manejarErrorFatalInicializacion("Error inicializando servicios esenciales", e);
             return false;
         }
    }

    /**
     * FASE A.4: Prepara el servicio de gestión de proyectos (ProjectManager).
     * Ahora solo crea la instancia. El cableado y la inicialización se harán más tarde.
     * @return true si la creación fue exitosa, false si falló.
     */
    private boolean inicializarServicioDeProyectos() {
        System.out.println("  [AppInitializer Fase A.4] Creando instancia de ProjectManager...");
        try {
            // Simplemente creamos la instancia. No se cablea ni se inicializa aquí.
            this.projectManagerService = new ProjectManager();
            
            // La inyección en el controlador también se hará más tarde para mantener
            // la consistencia, pero podríamos hacerlo aquí si quisiéramos.
            // Por ahora, solo confirmamos la creación.
            System.out.println("    -> Instancia de ProjectManager (servicio) creada.");
            return true;
        } catch (Exception e) {
            manejarErrorFatalInicializacion("Error creando la instancia de ProjectManager (servicio)", e);
            return false;
        }
    } // --- Fin del método inicializarServicioDeProyectos ---



    // --- MÉTODO PARA INICIALIZACIÓN DENTRO DEL EDT ---

 // Dentro de la clase controlador.AppInitializer

    private void crearUIyComponentesDependientesEnEDT() {
        try {
            System.out.println("--- [AppInitializer Fase B - EDT] Reorganizando la inicialización en bloques... ---");

            //======================================================================
            // BLOQUE 1: CREACIÓN DE TODAS LAS INSTANCIAS (SIN CABLEAR)
            //======================================================================
            System.out.println("  -> BLOQUE 1: Creando todas las instancias...");
            
            ComponentRegistry registry = new ComponentRegistry();
            this.zoomManager = new ZoomManager();
            this.editionManager = new EditionManager();
            this.viewManager = new ViewManager();
            this.listCoordinator = new ListCoordinator();
            this.fileOperationsManager = new FileOperationsManager();
            
            ProjectBuilder projectBuilder = new ProjectBuilder(registry, this.model);
            ViewBuilder viewBuilder = new ViewBuilder(
                registry, this.model, this.themeManager, this.configuration,
                this.controller, this.iconUtils, this.servicioMiniaturas,
                projectBuilder
            );

            UIDefinitionService uiDefSvc = new UIDefinitionService();
            Map<String, String> iconMap = new java.util.HashMap<>();
            
            this.actionFactory = new ActionFactory(
                 this.model, null, this.zoomManager, this.fileOperationsManager, 
                 this.editionManager, this.listCoordinator, this.iconUtils, this.configuration, 
                 this.projectManagerService, iconMap, this.viewManager, this.themeManager, this.controller
            );
            
            this.configAppManager = new ConfigApplicationManager(this.model, this.configuration, this.themeManager, registry);
            this.infobarImageManager = new InfobarImageManager(this.model, registry, this.configuration);

            MenuBarBuilder menuBuilder = new MenuBarBuilder(this.controller, this.configuration, this.viewManager);
            ToolbarBuilder toolbarBuilder = new ToolbarBuilder(this.themeManager, this.iconUtils, this.controller, configuration.getInt("iconos.ancho", 24), configuration.getInt("iconos.alto", 24));
            ToolbarManager toolbarManager = new ToolbarManager(this.configuration, toolbarBuilder, uiDefSvc, registry);
            
            //======================================================================
            // BLOQUE 2: CABLEADO (INYECCIÓN DE DEPENDENCIAS PRE-UI)
            //======================================================================
            System.out.println("  -> BLOQUE 2: Cableando dependencias pre-UI...");

            this.controller.setComponentRegistry(registry);

            if (this.projectManagerService != null) {
                this.projectManagerService.setConfigManager(this.configuration);
                this.controller.setProjectManager(this.projectManagerService);
            }

            this.zoomManager.setModel(this.model);
            this.zoomManager.setRegistry(registry);
            this.zoomManager.setConfiguration(this.configuration);

            this.editionManager.setModel(this.model);
            this.editionManager.setController(this.controller);
            this.editionManager.setZoomManager(this.zoomManager);

            this.viewManager.setConfiguration(this.configuration);
            this.viewManager.setRegistry(registry);
            this.viewManager.setThemeManager(this.themeManager);

            this.listCoordinator.setModel(this.model);
            this.listCoordinator.setController(this.controller);
            this.listCoordinator.setRegistry(registry);

            java.util.function.Consumer<java.nio.file.Path> onFolderSelectedCallback = (p) -> this.controller.cargarListaImagenes(null, null);
            this.fileOperationsManager.setModel(this.model);
            this.fileOperationsManager.setController(this.controller);
            this.fileOperationsManager.setConfiguration(this.configuration);
            this.fileOperationsManager.setOnNuevaCarpetaSeleccionadaCallback(onFolderSelectedCallback);
            
            viewBuilder.setViewManager(this.viewManager);
            menuBuilder.setControllerGlobalActionListener(this.controller);
            
            //======================================================================
            // BLOQUE 3: EJECUCIÓN Y ARRANQUE (POST-CABLEADO)
            //======================================================================
            System.out.println("  -> BLOQUE 3: Ejecutando lógica de arranque...");
            
            if (this.projectManagerService != null) {
                this.projectManagerService.initialize();
            }

            this.view = viewBuilder.createMainFrame();

            this.actionFactory.setView(this.view);
            this.configAppManager.setView(this.view);
            this.viewManager.setView(this.view);
            this.listCoordinator.setView(this.view);
            this.controller.setView(this.view);

            uiDefSvc.generateModularToolbarStructure().forEach(td -> td.botones().forEach(bd -> iconMap.put(bd.comandoCanonico(), bd.claveIcono())));
            this.actionFactory.initializeActions();
            this.actionMap = this.actionFactory.getActionMap();

            this.configAppManager.setActionMap(this.actionMap);
            this.viewManager.setActionMap(this.actionMap);
            this.controller.setActionMap(this.actionMap);
            toolbarBuilder.setActionMap(this.actionMap);
            
            this.statusBarManager = new InfobarStatusManager(this.model, registry, this.themeManager, this.configuration, this.projectManagerService, this.actionMap, this.iconUtils);
            this.controller.setZoomManager(this.zoomManager);
            this.controller.setListCoordinator(this.listCoordinator);
            this.controller.setViewManager(this.viewManager);
            this.controller.setConfigApplicationManager(configAppManager);
            this.controller.setInfobarImageManager(infobarImageManager);
            this.controller.setStatusBarManager(statusBarManager);
            this.zoomManager.setStatusBarManager(this.statusBarManager);
            this.zoomManager.setListCoordinator(this.listCoordinator);
            this.listCoordinator.setContextSensitiveActions(this.actionFactory.getContextSensitiveActions());

            this.view.setJMenuBar(menuBuilder.buildMenuBar(uiDefSvc.generateMenuStructure(), this.actionMap));
            this.controller.setMenuItemsPorNombre(menuBuilder.getMenuItemsMap());
            menuBuilder.getMenuItemsMap().forEach(registry::register);
            
            toolbarManager.inicializarBarrasDeHerramientas();
            Map<String, javax.swing.JButton> botones = toolbarBuilder.getBotonesPorNombre();
            this.controller.setBotonesPorNombre(botones);
            this.viewManager.setBotonesPorNombre(botones);
            
            // --- INICIO DE LA MODIFICACIÓN ---
            // Se añade la llamada para configurar los atajos de teclado globales.
            // Este es el lugar correcto, después de que la vista y el actionMap han sido
            // creados e inyectados en el controlador.
            this.controller.configurarAtajosTecladoGlobales();
            // --- FIN DE LA MODIFICACIÓN ---

            this.configAppManager.aplicarConfiguracionGlobalmente();
            this.controller.configurarListenersVistaInternal();
            java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this.controller);
            this.view.addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent e) { controller.shutdownApplication(); }
            });
            
            this.controller.configurarListenerDePrimerRenderizado();
            this.controller.establecerCarpetaRaizDesdeConfigInternal();
            
            this.view.setVisible(true);
            
            SwingUtilities.invokeLater(() -> {
                String imagenInicialKey = configuration.getString(ConfigKeys.INICIO_IMAGEN, null);
                if (this.model.getCarpetaRaizActual() != null) {
                    System.out.println("  -> [EDT Post-Visibilidad] Lanzando carga de imágenes...");
                    this.controller.cargarListaImagenes(imagenInicialKey, null);
                } else {
                    this.controller.limpiarUI();
                }
            });

            System.out.println("--- [EDT] Inicialización de UI completada. Carga de datos programada. ---");

        } catch (Exception e) {
            manejarErrorFatalInicializacion("[EDT] Error fatal durante la creación de la UI", e);
            e.printStackTrace();
        }
    } // --- Fin del método crearUIyComponentesDependientesEnEDT ---
    

//	// Este es tu método reorganizado, con la mejora de rendimiento al final.
//	// La lógica de creación y cableado es la tuya, que ya es correcta.
//	
//	private void crearUIyComponentesDependientesEnEDT() {
//	    try {
//	        System.out.println("--- [AppInitializer Fase B - EDT] Reorganizando la inicialización en bloques... ---");
//	
//	        //======================================================================
//	        // BLOQUE 1: CREACIÓN DE TODAS LAS INSTANCIAS (SIN CABLEAR)
//	        //======================================================================
//	        System.out.println("  -> BLOQUE 1: Creando todas las instancias...");
//	        
//	        ComponentRegistry registry = new ComponentRegistry();
//	        this.zoomManager = new ZoomManager();
//	        this.editionManager = new EditionManager();
//	        this.viewManager = new ViewManager();
//	        this.listCoordinator = new ListCoordinator();
//	        this.fileOperationsManager = new FileOperationsManager();
//	        
//	        ProjectBuilder projectBuilder = new ProjectBuilder(registry, this.model);
//	        ViewBuilder viewBuilder = new ViewBuilder(
//	            registry, this.model, this.themeManager, this.configuration,
//	            this.controller, this.iconUtils, this.servicioMiniaturas,
//	            projectBuilder
//	        );
//	
//	        UIDefinitionService uiDefSvc = new UIDefinitionService();
//	        Map<String, String> iconMap = new java.util.HashMap<>();
//	        
//	        // Se crea ActionFactory con dependencias que ya existen, pasando null para la 'view' que aún no se ha creado.
//	        this.actionFactory = new ActionFactory(
//	             this.model, null, this.zoomManager, this.fileOperationsManager, 
//	             this.editionManager, this.listCoordinator, this.iconUtils, this.configuration, 
//	             this.projectManagerService, iconMap, this.viewManager, this.themeManager, this.controller
//	        );
//	        
//	        this.configAppManager = new ConfigApplicationManager(this.model, this.configuration, this.themeManager, registry);
//	        this.infobarImageManager = new InfobarImageManager(this.model, registry, this.configuration);
//	
//	        MenuBarBuilder menuBuilder = new MenuBarBuilder(this.controller, this.configuration, this.viewManager);
//	        ToolbarBuilder toolbarBuilder = new ToolbarBuilder(this.themeManager, this.iconUtils, this.controller, configuration.getInt("iconos.ancho", 24), configuration.getInt("iconos.alto", 24));
//	        ToolbarManager toolbarManager = new ToolbarManager(this.configuration, toolbarBuilder, uiDefSvc, registry);
//	        
//	        //======================================================================
//	        // BLOQUE 2: CABLEADO (INYECCIÓN DE DEPENDENCIAS PRE-UI)
//	        //======================================================================
//	        System.out.println("  -> BLOQUE 2: Cableando dependencias pre-UI...");
//	
//	        this.controller.setComponentRegistry(registry);
//	
//	        if (this.projectManagerService != null) {
//	            this.projectManagerService.setConfigManager(this.configuration);
//	            this.controller.setProjectManager(this.projectManagerService);
//	        }
//	
//	        this.zoomManager.setModel(this.model);
//	        this.zoomManager.setRegistry(registry);
//	        this.zoomManager.setConfiguration(this.configuration);
//	
//	        this.editionManager.setModel(this.model);
//	        this.editionManager.setController(this.controller);
//	        this.editionManager.setZoomManager(this.zoomManager);
//	
//	        this.viewManager.setConfiguration(this.configuration);
//	        this.viewManager.setRegistry(registry);
//	        this.viewManager.setThemeManager(this.themeManager);
//	
//	        this.listCoordinator.setModel(this.model);
//	        this.listCoordinator.setController(this.controller);
//	        this.listCoordinator.setRegistry(registry);
//	
//	        java.util.function.Consumer<java.nio.file.Path> onFolderSelectedCallback = (p) -> this.controller.cargarListaImagenes(null, null);
//	        this.fileOperationsManager.setModel(this.model);
//	        this.fileOperationsManager.setController(this.controller);
//	        this.fileOperationsManager.setConfiguration(this.configuration);
//	        this.fileOperationsManager.setOnNuevaCarpetaSeleccionadaCallback(onFolderSelectedCallback);
//	        
//	        viewBuilder.setViewManager(this.viewManager);
//	        menuBuilder.setControllerGlobalActionListener(this.controller);
//	        
//	        //======================================================================
//	        // BLOQUE 3: EJECUCIÓN Y ARRANQUE (POST-CABLEADO)
//	        //======================================================================
//	        System.out.println("  -> BLOQUE 3: Ejecutando lógica de arranque...");
//	        
//	        // 3.1. Inicializar servicios que necesitaban dependencias
//	        if (this.projectManagerService != null) {
//	            this.projectManagerService.initialize();
//	        }
//	
//	        // 3.2. Construir la UI principal. A partir de aquí, 'this.view' existe.
//	        this.view = viewBuilder.createMainFrame();
//	
//	        // 3.3. CABLEADO FINAL: Inyectar las dependencias que necesitaban la 'view'.
//	        this.actionFactory.setView(this.view); // <-- ¡Aquí se resuelve la dependencia!
//	        this.configAppManager.setView(this.view);
//	        this.viewManager.setView(this.view);
//	        this.listCoordinator.setView(this.view);
//	        this.controller.setView(this.view);
//	
//	        // 3.4. Ahora que ActionFactory está completa, podemos inicializar las acciones y obtener el actionMap.
//	        uiDefSvc.generateModularToolbarStructure().forEach(td -> td.botones().forEach(bd -> iconMap.put(bd.comandoCanonico(), bd.claveIcono())));
//	        this.actionFactory.initializeActions(); // <-- Ahora es seguro llamar a esto
//	        this.actionMap = this.actionFactory.getActionMap();
//	
//	        // 3.5. CABLEADO FINAL: Inyectar el 'actionMap' a los componentes que lo necesitan.
//	        this.configAppManager.setActionMap(this.actionMap);
//	        this.viewManager.setActionMap(this.actionMap);
//	        this.controller.setActionMap(this.actionMap);
//	        toolbarBuilder.setActionMap(this.actionMap);
//	        
//	        // 3.6. CABLEADO FINAL: Resto de interconexiones entre managers.
//	        this.statusBarManager = new InfobarStatusManager(this.model, registry, this.themeManager, this.configuration, this.projectManagerService, this.actionMap, this.iconUtils);
//	        this.controller.setZoomManager(this.zoomManager);
//	        this.controller.setListCoordinator(this.listCoordinator);
//	        this.controller.setViewManager(this.viewManager);
//	        this.controller.setConfigApplicationManager(configAppManager);
//	        this.controller.setInfobarImageManager(infobarImageManager);
//	        this.controller.setStatusBarManager(statusBarManager);
//	        this.zoomManager.setStatusBarManager(this.statusBarManager);
//	        this.zoomManager.setListCoordinator(this.listCoordinator);
//	        this.listCoordinator.setContextSensitiveActions(this.actionFactory.getContextSensitiveActions());
//	
//	        // 3.7. Poblar la UI con los datos/componentes finales
//	        this.view.setJMenuBar(menuBuilder.buildMenuBar(uiDefSvc.generateMenuStructure(), this.actionMap));
//	        this.controller.setMenuItemsPorNombre(menuBuilder.getMenuItemsMap());
//	        menuBuilder.getMenuItemsMap().forEach(registry::register);
//	        
//	        toolbarManager.inicializarBarrasDeHerramientas();
//	        Map<String, javax.swing.JButton> botones = toolbarBuilder.getBotonesPorNombre();
//	        this.controller.setBotonesPorNombre(botones);
//	        this.viewManager.setBotonesPorNombre(botones);
//	        
//	        // 3.8. Configuración final de listeners
//	        this.configAppManager.aplicarConfiguracionGlobalmente();
//	        this.controller.configurarListenersVistaInternal();
//	        java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this.controller);
//	        this.view.addWindowListener(new java.awt.event.WindowAdapter() {
//	            public void windowClosing(java.awt.event.WindowEvent e) { controller.shutdownApplication(); }
//	        });
//	        
//	        this.controller.configurarListenerDePrimerRenderizado();
//	        this.controller.establecerCarpetaRaizDesdeConfigInternal();
//	        
//	        // --- INICIO DE LA MEJORA DE RENDIMIENTO ---
//	
//	        // 3.9. HACER VISIBLE LA VENTANA. En este punto la UI está vacía pero responde.
//	        // Esta es una operación rápida para el EDT.
//	        this.view.setVisible(true);
//	        
//	        // 3.10. PROGRAMAR LA CARGA PESADA EN UN EVENTO POSTERIOR DEL EDT.
//	        //       Usamos invokeLater para darle tiempo al EDT a procesar el setVisible(true)
//	        //       y pintar la ventana antes de empezar a buscar archivos.
//	        SwingUtilities.invokeLater(() -> {
//	            String imagenInicialKey = configuration.getString(ConfigKeys.INICIO_IMAGEN, null);
//	            if (this.model.getCarpetaRaizActual() != null) {
//	                System.out.println("  -> [EDT Post-Visibilidad] Lanzando carga de imágenes...");
//	                this.controller.cargarListaImagenes(imagenInicialKey, null);
//	            } else {
//	                // Si no hay carpeta, simplemente limpiamos la UI (acción rápida).
//	                this.controller.limpiarUI();
//	            }
//	        });
//	
//	        // --- FIN DE LA MEJORA ---
//	        
//	        System.out.println("--- [EDT] Inicialización de UI completada. Carga de datos programada. ---");
//	
//	      } catch (Exception e) {
//	          manejarErrorFatalInicializacion("[EDT] Error fatal durante la creación de la UI", e);
//	          e.printStackTrace();
//	      }
//	  } // --- Fin del método crearUIyComponentesDependientesEnEDT ---


    
    
    



    
    

    

    
    
//***************************************************************************************************************    
// 							--- MÉTODOS HELPER PARA CADA FASE DE INICIALIZACIÓN ---
//***************************************************************************************************************	
    
    


    
	
    
    
//***************************************************************************************************************    
//						--- FINAL DE LOS MÉTODOS HELPER PARA CADA FASE DE INICIALIZACIÓN ---
//***************************************************************************************************************    
    
	
    

    


    /**
     * Manejador de errores fatales ocurridos durante la inicialización.
     * Muestra un mensaje al usuario y termina la aplicación.
     * @param message Mensaje descriptivo del error.
     * @param cause La excepción original (puede ser null).
     */
    private void manejarErrorFatalInicializacion(String message, Throwable cause) {
        System.err.println("### ERROR FATAL DE INICIALIZACIÓN ###");
        System.err.println("Mensaje: " + message);
        if (cause != null) {
            System.err.println("Causa: " + cause.toString());
            cause.printStackTrace(System.err); // Imprimir stack trace completo a System.err
        }
        System.err.println("#####################################");

        // Asegurarse de mostrar el mensaje en el EDT, ya que puede ser llamado desde cualquier hilo
        final String finalMessage = message; // Variable final para la lambda
        final Throwable finalCause = cause;  // Variable final para la lambda
        
        if (SwingUtilities.isEventDispatchThread()) {
            // Si ya estamos en el EDT, mostrar directamente
             JOptionPane.showMessageDialog(null, // Sin padre si la UI no está lista
                 finalMessage + (finalCause != null ? "\n\nError Detallado: " + finalCause.getMessage() : ""),
                 "Error Fatal de Inicialización",
                 JOptionPane.ERROR_MESSAGE);
        } else {
            // Si no estamos en el EDT, programar para que se muestre en el EDT
            try {
                SwingUtilities.invokeAndWait(() -> { // invokeAndWait para asegurar que se muestre antes de salir
                     JOptionPane.showMessageDialog(null,
                         finalMessage + (finalCause != null ? "\n\nError Detallado: " + finalCause.getMessage() : ""),
                         "Error Fatal de Inicialización",
                         JOptionPane.ERROR_MESSAGE);
                });
            } catch (Exception swingEx) {
                 System.err.println("Error adicional al intentar mostrar el diálogo de error en EDT: " + swingEx.getMessage());
                 swingEx.printStackTrace(System.err);
            }
        }

        // Terminar la aplicación
        System.err.println("Terminando la aplicación debido a un error fatal de inicialización.");
        System.exit(1); 
    } // --- FIN manejarErrorFatalInicializacion ---

    
    public void setControllerParaNotificacion(VisorController controller) {
        this.controllerRefParaNotificacion = controller;
    }
    
    
} // --- FIN CLASE AppInitializer ---