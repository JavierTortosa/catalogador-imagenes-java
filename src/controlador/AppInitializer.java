package controlador;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;

import javax.swing.Action;
import javax.swing.DefaultListModel;
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
    
    private ProjectController projectController;

    // 1.1. Modelo y Servicios Base (creados fuera del EDT)
    private VisorModel model;
    private ConfigurationManager configuration;
    private IconUtils iconUtils;
    private ThumbnailService servicioMiniaturas;
    private ProjectManager projectManagerService; // Servicio de persistencia de proyectos
    private VisorController controllerRefParaNotificacion; 
    private GeneralController generalController;
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
     * FASE A.2: Lee valores de la configuración y los usa para inicializar
     *           el estado por defecto de TODOS los contextos en el VisorModel.
     */
    private void aplicarConfiguracionAlModelo() {
        System.out.println("  [AppInitializer Fase A.2] Aplicando Configuración a los contextos del Modelo...");
        if (this.configuration == null || this.model == null) {
            System.err.println("ERROR: Configuración o Modelo nulos. Saltando fase.");
            return;
        }
        
        // --- 1. Leer TODOS los valores de configuración necesarios ---
        
        // Configuración de comportamiento
        boolean mantenerProp = configuration.getBoolean("interfaz.menu.zoom.mantener_proporciones.seleccionado", true);
        boolean incluirSubcarpetas = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_CARGAR_SUBCARPETAS, true);
        boolean soloCarpeta = !incluirSubcarpetas;
        boolean navCircular = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_NAVEGACION_CIRCULAR, false);
        boolean zoomManualInicial = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_ZOOM_MANUAL_INICIAL, true);
        
        // Configuración del modo de zoom inicial (usando el último modo visto como prioridad)
        ZoomModeEnum modoZoomInicial;
        String ultimoModoStr = configuration.getString(ConfigKeys.COMPORTAMIENTO_ZOOM_ULTIMO_MODO, "FIT_TO_SCREEN").toUpperCase();
        try {
            modoZoomInicial = ZoomModeEnum.valueOf(ultimoModoStr);
        } catch (IllegalArgumentException e) {
            System.err.println("WARN [AppInitializer]: Valor de ultimo_modo_zoom inválido. Usando FIT_TO_SCREEN.");
            modoZoomInicial = ZoomModeEnum.FIT_TO_SCREEN;
        }
        
        // --- 2. Llamar al método centralizado del modelo para inicializar los contextos ---
        this.model.initializeContexts(mantenerProp, soloCarpeta, modoZoomInicial, zoomManualInicial, navCircular);
        System.out.println("    -> Contextos del modelo (Visualizador, Proyecto, etc.) inicializados.");

        // --- 3. Aplicar configuración que NO es contextual (como las dimensiones de las miniaturas) ---
        try {
            this.model.setMiniaturasAntes(configuration.getInt(ConfigKeys.MINIATURAS_CANTIDAD_ANTES, 8));
            this.model.setMiniaturasDespues(configuration.getInt(ConfigKeys.MINIATURAS_CANTIDAD_DESPUES, 8));
            this.model.setMiniaturaSelAncho(configuration.getInt(ConfigKeys.MINIATURAS_TAMANO_SEL_ANCHO, 60));
            this.model.setMiniaturaSelAlto(configuration.getInt(ConfigKeys.MINIATURAS_TAMANO_SEL_ALTO, 60));
            this.model.setMiniaturaNormAncho(configuration.getInt(ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, 40));
            this.model.setMiniaturaNormAlto(configuration.getInt(ConfigKeys.MINIATURAS_TAMANO_NORM_ALTO, 40));
            System.out.println("    -> Configuración de miniaturas (no contextual) aplicada al modelo.");
        } catch (Exception e) {
            System.err.println("ERROR [AppInitializer.aplicarConfigAlModelo] aplicando config de miniaturas: " + e.getMessage());
        }
        
        System.out.println("  -> Fin de la aplicación de configuración al modelo.");
    }

    
    /**
     * FASE A.3: Inicializa servicios esenciales que no dependen directamente de la UI:
     *           ThemeManager, IconUtils, ExecutorService, ThumbnailService y el modelo de miniaturas.
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
                 System.err.println("ERROR CRÍTICO en AppInitializer: ThemeManager o this.controller nulos al intentar inyección cruzada.");
             }
             
             // A.3.2. IconUtils (depende de ThemeManager)
             this.iconUtils = new IconUtils(this.themeManager);
             this.controller.setIconUtils(this.iconUtils);
             System.out.println("    -> IconUtils creado e inyectado.");

             // A.3.3. ThumbnailService (servicio para generar/cachear miniaturas)
             this.servicioMiniaturas = new ThumbnailService();
             this.controller.setServicioMiniaturas(this.servicioMiniaturas);
             System.out.println("    -> ThumbnailService creado e inyectado.");
             
             // --- INICIO DE LA LÓGICA CORREGIDA ---
             // A.3.4. Creación del Modelo de Datos para la JList de Miniaturas.
             //        Se crea aquí, en el AppInitializer, como único punto de creación.
             DefaultListModel<String> modeloParaMiniaturasJList = new DefaultListModel<>();
             
             // Log de depuración para poder rastrear esta instancia específica.
             System.out.println("    -> Modelo para JList de Miniaturas creado. HashCode: " + System.identityHashCode(modeloParaMiniaturasJList));
             
             // A.3.5. Inyección del modelo en el VisorController.
             this.controller.setModeloMiniaturas(modeloParaMiniaturasJList); 
             // --- FIN DE LA LÓGICA CORREGIDA ---

             // A.3.6. ExecutorService (para tareas en segundo plano)
             int numThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
             this.controller.setExecutorService(Executors.newFixedThreadPool(numThreads));
             System.out.println("    -> ExecutorService (FixedThreadPool con " + numThreads + " hilos) creado e inyectado.");

             return true; // Éxito de la fase
         } catch (Exception e) {
             manejarErrorFatalInicializacion("Error inicializando servicios esenciales", e);
             return false;
         }
    } // --- Fin del método inicializarServiciosEsenciales ---
    
    
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

    
    private void crearUIyComponentesDependientesEnEDT() {
        try {
            System.out.println("--- [AppInitializer Fase B - EDT] Reorganizando la inicialización en bloques... ---");

            //======================================================================
            // BLOQUE 1: CREACIÓN DE TODAS LAS INSTANCIAS (SIN CABLEAR)
            //======================================================================
            System.out.println("  -> BLOQUE 1: Creando todas las instancias...");
            
            ComponentRegistry registry = new ComponentRegistry();
            
            // --- CAMBIO: Crear el GeneralController ---
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
            
            // --- CAMBIO: La ActionFactory ahora recibe el GeneralController ---
            // Le pasamos el generalController en lugar del visorController para la gestión de modos.
            this.actionFactory = new ActionFactory(
                 this.model, null, this.zoomManager, this.fileOperationsManager, 
                 this.editionManager, this.listCoordinator, this.iconUtils, this.configuration, 
                 this.projectManagerService, iconMap, this.viewManager, this.themeManager, 
                 this.generalController, // <-- SE INYECTA EL GENERALCONTROLLER
                 this.projectController
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

            this.projectController.setProjectManager(this.projectManagerService);
            this.projectController.setViewManager(this.viewManager);
            this.projectController.setRegistry(registry);
            this.projectController.setZoomManager(this.zoomManager); 
            this.projectController.setModel(this.model); 
            this.projectController.setController(this.controller);
            this.controller.setProjectController(this.projectController);

            // --- CAMBIO: Cableado del GeneralController ---
            this.generalController.setModel(this.model);
            this.generalController.setViewManager(this.viewManager);
            this.generalController.setVisorController(this.controller);
            this.generalController.setProjectController(this.projectController);
            this.generalController.setConfigApplicationManager(this.configAppManager);
            
            // El actionMap se inyectará más adelante, después de crearlo.

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
            this.projectController.setView(this.view);
            
            uiDefSvc.generateModularToolbarStructure().forEach(td -> td.botones().forEach(bd -> iconMap.put(bd.comandoCanonico(), bd.claveIcono())));
            this.actionFactory.initializeActions();
            this.actionMap = this.actionFactory.getActionMap();

            this.configAppManager.setActionMap(this.actionMap);
            this.viewManager.setActionMap(this.actionMap);
            this.controller.setActionMap(this.actionMap);
            this.projectController.setActionMap(this.actionMap);
            
            // --- CAMBIO: Inyectar el ActionMap en el GeneralController ---
            this.generalController.setActionMap(this.actionMap);
            
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
            
            this.generalController.setStatusBarManager(this.statusBarManager);

            this.view.setJMenuBar(menuBuilder.buildMenuBar(uiDefSvc.generateMenuStructure(), this.actionMap));
            this.controller.setMenuItemsPorNombre(menuBuilder.getMenuItemsMap());
            menuBuilder.getMenuItemsMap().forEach(registry::register);
            
            toolbarManager.inicializarBarrasDeHerramientas();
            Map<String, javax.swing.JButton> botones = toolbarBuilder.getBotonesPorNombre();
            this.controller.setBotonesPorNombre(botones);
            this.viewManager.setBotonesPorNombre(botones);
            
            this.controller.configurarAtajosTecladoGlobales();
            this.configAppManager.aplicarConfiguracionGlobalmente();
            this.controller.configurarListenersVistaInternal();
            this.projectController.configurarListeners();
            
            // --- CAMBIO: Llamar al initialize del GeneralController ---
            this.generalController.initialize();
            
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