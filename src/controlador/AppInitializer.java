package controlador;

import java.awt.KeyboardFocusManager;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import controlador.factory.ActionFactory;
import controlador.managers.ConfigApplicationManager;
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
import vista.config.UIDefinitionService;
import vista.theme.ThemeManager;
import vista.util.IconUtils;

public class AppInitializer {

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

    public AppInitializer(VisorController controller) {
        this.controller = Objects.requireNonNull(controller, "VisorController no puede ser null en AppInitializer");
    } // --- fin del método AppInitializer (constructor) ---

    public boolean initialize() {
        System.out.println("--- AppInitializer: Iniciando Proceso de Inicialización Global ---");

        try {
            if (!inicializarComponentesBase()) return false;
            aplicarConfiguracionAlModelo();
            if (!inicializarServiciosEsenciales()) return false;
            if (!inicializarServicioDeProyectos()) return false;

            System.out.println("  [AppInitializer] Programando creación de UI y componentes dependientes en EDT...");
            SwingUtilities.invokeLater(this::crearUIyComponentesDependientesEnEDT);

            System.out.println("--- AppInitializer: Inicialización base (fuera de EDT) completada. ---");
            
            return true;
        } catch (Exception e) {
            manejarErrorFatalInicializacion("Error fatal durante la inicialización base (fuera del EDT)", e);
            return false;
        }
    } // --- Fin del método initialize ---

    private boolean inicializarComponentesBase() {
        System.out.println("  [AppInitializer Fase A.1] Inicializando Componentes Base (Modelo, Configuración)...");
        try {
            this.model = new VisorModel();
            this.controller.setModel(this.model);
            this.configuration = ConfigurationManager.getInstance();
            this.controller.setConfigurationManager(this.configuration);
            return true;
        } catch (Exception e) {
             manejarErrorFatalInicializacion("Error inesperado inicializando componentes base", e);
             return false;
        }
    } // --- fin del método inicializarComponentesBase ---

    private void aplicarConfiguracionAlModelo() {
        System.out.println("  [AppInitializer Fase A.2] Aplicando Configuración a los contextos del Modelo...");
        if (this.configuration == null || this.model == null) return;
        
        boolean mantenerProp = configuration.getBoolean("interfaz.menu.zoom.mantener_proporciones.seleccionado", true);
        boolean incluirSubcarpetas = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_CARGAR_SUBCARPETAS, true);
        boolean soloCarpeta = !incluirSubcarpetas;
        boolean navCircular = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_NAVEGACION_CIRCULAR, false);
        boolean zoomManualInicial = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_ZOOM_MANUAL_INICIAL, true);
        boolean zoomAlCursor = configuration.getBoolean("comportamiento.zoom.al_cursor.activado", false);
        
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
    } // --- fin del método aplicarConfiguracionAlModelo ---

    private boolean inicializarServiciosEsenciales() {
         System.out.println("  [AppInitializer Fase A.3] Inicializando Servicios Esenciales...");
         try {
             this.themeManager = new ThemeManager(this.configuration);
             this.controller.setThemeManager(this.themeManager);
             if (this.themeManager != null && this.controller != null) {
                 this.themeManager.setControllerParaNotificacion(this.controller);
             }
             this.iconUtils = new IconUtils(this.themeManager);
             this.controller.setIconUtils(this.iconUtils);
             this.servicioMiniaturas = new ThumbnailService();
             this.controller.setServicioMiniaturas(this.servicioMiniaturas);
             DefaultListModel<String> modeloParaMiniaturasJList = new DefaultListModel<>();
             this.controller.setModeloMiniaturas(modeloParaMiniaturasJList); 
             int numThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
             this.controller.setExecutorService(Executors.newFixedThreadPool(numThreads));
             return true;
         } catch (Exception e) {
             manejarErrorFatalInicializacion("Error inicializando servicios esenciales", e);
             return false;
         }
    } // --- Fin del método inicializarServiciosEsenciales ---
    
    private boolean inicializarServicioDeProyectos() {
        System.out.println("  [AppInitializer Fase A.4] Creando instancia de ProjectManager...");
        try {
            this.projectManagerService = new ProjectManager();
            return true;
        } catch (Exception e) {
            manejarErrorFatalInicializacion("Error creando la instancia de ProjectManager (servicio)", e);
            return false;
        }
    } // --- Fin del método inicializarServicioDeProyectos ---
    
    
    private void crearUIyComponentesDependientesEnEDT() {
        try {
            System.out.println("--- [AppInitializer Fase B - EDT] Reorganizando la inicialización en bloques... ---");

            //======================================================================
            // BLOQUE 1: CREACIÓN DE TODAS LAS INSTANCIAS
            //======================================================================
            System.out.println("  -> BLOQUE 1: Creando todas las instancias...");
            
            ComponentRegistry registry = new ComponentRegistry();
            this.generalController = new GeneralController();
            this.zoomManager = new ZoomManager();
            this.editionManager = new EditionManager();
            this.viewManager = new ViewManager();
            this.listCoordinator = new ListCoordinator();
            this.fileOperationsManager = new FileOperationsManager();
            this.projectController = new ProjectController();
            ProjectBuilder projectBuilder = new ProjectBuilder(registry, this.model, this.themeManager);
            DefaultListModel<String> modeloMiniaturas = this.controller.getModeloMiniaturas();
            
            ViewBuilder viewBuilder = new ViewBuilder(
                registry, this.model, this.themeManager, this.configuration,
                this.controller, this.iconUtils, this.servicioMiniaturas,
                projectBuilder,
                modeloMiniaturas
            );

            UIDefinitionService uiDefSvc = new UIDefinitionService();
            Map<String, String> iconMap = new java.util.HashMap<>();
            uiDefSvc.generateModularToolbarStructure().forEach(td -> td.botones().forEach(bd -> iconMap.put(bd.comandoCanonico(), bd.claveIcono())));
            
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
                    this.themeManager,
                    this.iconUtils,
                    this.controller,
                    configuration.getInt("iconos.ancho", 24),
                    configuration.getInt("iconos.alto", 24),
                    registry
            );
            
            this.toolbarManager = new ToolbarManager(registry, this.configuration, toolbarBuilder, uiDefSvc, this.model);
            
            //======================================================================
            // BLOQUE 2: CABLEADO (INYECCIÓN DE DEPENDENCIAS)
            //======================================================================
            System.out.println("  -> BLOQUE 2: Cableando dependencias...");

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
            this.projectController.setProjectManager(this.projectManagerService);
            this.projectController.setViewManager(this.viewManager);
            this.projectController.setRegistry(registry);
            this.projectController.setZoomManager(this.zoomManager); 
            this.projectController.setModel(this.model); 
            this.projectController.setController(this.controller);
            
            this.projectController.setListCoordinator(this.listCoordinator);
            
            this.controller.setProjectController(this.projectController);
            this.generalController.setModel(this.model);
            this.generalController.setViewManager(this.viewManager);
            this.generalController.setVisorController(this.controller);
            this.generalController.setProjectController(this.projectController);
            this.generalController.setConfigApplicationManager(this.configAppManager);
            this.generalController.setRegistry(registry);
            this.generalController.setToolbarManager(this.toolbarManager);
            java.util.function.Consumer<java.nio.file.Path> onFolderSelectedCallback = (p) -> this.controller.cargarListaImagenes(null, null);
            this.fileOperationsManager.setModel(this.model);
            this.fileOperationsManager.setController(this.controller);
            this.fileOperationsManager.setConfiguration(this.configuration);
            this.fileOperationsManager.setOnNuevaCarpetaSeleccionadaCallback(onFolderSelectedCallback);
            
            this.controller.setComponentRegistry(registry);
            this.controller.setToolbarManager(this.toolbarManager);
            this.controller.setActionFactory(this.actionFactory);
            
            viewBuilder.setToolbarManager(this.toolbarManager);
            viewBuilder.setViewManager(this.viewManager);
            menuBuilder.setControllerGlobalActionListener(this.controller);
            
            //======================================================================
            // BLOQUE 3: FASE DE INICIALIZACIÓN SECUENCIADA (ORDEN CORREGIDO)
            //======================================================================
            System.out.println("  -> BLOQUE 3: Ejecutando inicialización secuenciada...");

            // 1. Inicializar las Actions que NO dependen de la 'view'.
            this.actionFactory.initializeCoreActions();
            this.actionMap = this.actionFactory.getActionMap();

            // 2. Inyectar el ActionMap (parcial) en los builders/managers que lo necesitan para construir.
            toolbarBuilder.setActionMap(this.actionMap);

            // 3. Crear el frame principal (VisorView).
            this.view = viewBuilder.createMainFrame();
            System.out.println("    -> VisorView (JFrame) creado.");

            // 4. Inyectar la 'view' en todos los componentes que la esperaban.
            System.out.println("    -> Inyectando la instancia de 'view' en los componentes...");
            this.actionFactory.setView(this.view);
            this.configAppManager.setView(this.view);
            this.viewManager.setView(this.view);
            this.listCoordinator.setView(this.view);
            this.controller.setView(this.view);
            this.projectController.setView(this.view);

            // 5. Ahora que la 'view' está inyectada, inicializar las Actions restantes.
            this.actionFactory.initializeViewDependentActions();
            
            // 6. El resto del cableado y configuración final.
            if (this.projectManagerService != null) {
                this.projectManagerService.initialize();
            }

            this.configAppManager.setActionMap(this.actionMap);
            this.viewManager.setActionMap(this.actionMap);
            this.controller.setActionMap(this.actionMap);
            this.projectController.setActionMap(this.actionMap);
            this.generalController.setActionMap(this.actionMap);
            
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
            this.generalController.setStatusBarManager(this.statusBarManager);

            this.view.setJMenuBar(menuBuilder.buildMenuBar(uiDefSvc.generateMenuStructure(), this.actionMap));
            this.controller.setMenuItemsPorNombre(menuBuilder.getMenuItemsMap());
            menuBuilder.getMenuItemsMap().forEach(registry::register);
            
            this.toolbarManager.reconstruirContenedorDeToolbars(this.model.getCurrentWorkMode());
            
            Map<String, javax.swing.JButton> botones = toolbarBuilder.getBotonesPorNombre();
            this.controller.setBotonesPorNombre(botones);
            this.viewManager.setBotonesPorNombre(botones);
            
            this.controller.configurarAtajosTecladoGlobales();
            this.configAppManager.aplicarConfiguracionGlobalmente();

            // ------------------ INICIO DE LAS MODIFICACIONES CLAVE ------------------
            
            // AÑADIDO: Pedir a GeneralController que configure sus listeners GLOBALES de ratón.
            // Esto es SEGURO ahora porque la UI está construida y registrada en el registry.
            this.generalController.configurarListenersDeEntradaGlobal();

            // AÑADIDO: Registrar a GeneralController como el dispatcher global de TECLADO.
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this.generalController);

            // MANTENIDO: La siguiente línea se mantiene por ahora para no romper nada.
            // En una fase posterior, eliminaremos los listeners de teclado/ratón duplicados de VisorController.
            this.controller.configurarListenersVistaInternal(); 

            // ELIMINADO: La siguiente línea ahora es redundante.
            // Se elimina porque GeneralController ya está registrado como el dispatcher.
            // java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this.controller);
            
            // ------------------ FIN DE LAS MODIFICACIONES CLAVE ------------------

            this.projectController.configurarListeners();
            this.generalController.initialize();
            
            this.controller.configurarListenerGlobalDeToolbars();
            
            this.view.addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent e) { controller.shutdownApplication(); }
            });
            
            this.controller.configurarListenerDePrimerRenderizado();
            this.controller.establecerCarpetaRaizDesdeConfigInternal();
            
            this.view.setVisible(true);
            
            System.out.println("  [AppInitializer] Forzando sincronización visual inicial de botones toggle...");
            if (this.configAppManager != null) {
                this.configAppManager.sincronizarAparienciaTodosLosToggles();
            }
            
            SwingUtilities.invokeLater(() -> {
                String imagenInicialKey = configuration.getString(ConfigKeys.INICIO_IMAGEN, null);
                if (this.model.getCarpetaRaizActual() != null) {
                    this.controller.cargarListaImagenes(imagenInicialKey, null);
                } else {
                    this.controller.limpiarUI();
                }
            });

            System.out.println("--- [EDT] Inicialización de UI completada. Carga de datos programada. ---");

        } catch (Exception e) {
            manejarErrorFatalInicializacion("[EDT] Error fatal durante la creación de la UI", e);
        }
    } // --- Fin del método crearUIyComponentesDependientesEnEDT ---
    


    
    private void manejarErrorFatalInicializacion(String message, Throwable cause) {
        System.err.println("### ERROR FATAL DE INICIALIZACIÓN ###");
        System.err.println("Mensaje: " + message);
        if (cause != null) {
            System.err.println("Causa: " + cause.toString());
            cause.printStackTrace(System.err);
        }
        System.err.println("#####################################");

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
                 System.err.println("Error adicional al intentar mostrar el diálogo de error en EDT: " + swingEx.getMessage());
                 swingEx.printStackTrace(System.err);
            }
        }
        System.err.println("Terminando la aplicación debido a un error fatal de inicialización.");
        System.exit(1); 
    } // --- fin del método manejarErrorFatalInicializacion ---

    public void setControllerParaNotificacion(VisorController controller) {
        this.controllerRefParaNotificacion = controller;
    } // --- fin del método setControllerParaNotificacion ---
    
} // --- FIN de la clase AppInitializer ---

