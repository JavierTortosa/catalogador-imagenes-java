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

import controlador.factory.ActionFactory;
import controlador.managers.BackgroundControlManager;
import controlador.managers.CarouselManager;
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
import vista.util.ThumbnailPreviewer;

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
	private BackgroundControlManager backgroundControlManager;
	private CarouselManager carouselManager;
	

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
        
    } // --- fin del método aplicarConfiguracionAlModelo ---

    private boolean inicializarServiciosEsenciales() {
         System.out.println("  [AppInitializer Fase A.3] Inicializando Servicios Esenciales...");
         try {
        	 this.themeManager = new ThemeManager(this.configuration);
             this.themeManager.install();
             
             this.controller.setThemeManager(this.themeManager);
             if (this.themeManager != null && this.controller != null) {
                 this.themeManager.setControllerParaNotificacion(this.controller);
             }
             this.iconUtils = new IconUtils(this.themeManager);
             this.controller.setIconUtils(this.iconUtils);
             this.servicioMiniaturas = new ThumbnailService();
             this.controller.setServicioMiniaturas(this.servicioMiniaturas);
             DefaultListModel<String> modeloParaMiniaturasJList = new DefaultListModel<>();
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
            this.carouselManager = new CarouselManager(listCoordinator, this.controller, registry, this.model);
            
            this.fileOperationsManager = new FileOperationsManager();
            this.projectController = new ProjectController();
            ProjectBuilder projectBuilder = new ProjectBuilder(registry, this.model, this.themeManager, this.generalController);
            
            ViewBuilder viewBuilder = new ViewBuilder(
                registry, 
                this.model, 
                this.themeManager, 
                this.configuration,
                this.iconUtils, 
                this.servicioMiniaturas,
                projectBuilder
            );

            UIDefinitionService uiDefSvc = new UIDefinitionService();
            
            // Creamos el nuevo mapa que guarda más información
            Map<String, ActionFactory.IconInfo> iconMap = new java.util.HashMap<>();
            
            // 2. LLENAMOS el mapa con la información de los iconos.
            uiDefSvc.generateModularToolbarStructure().forEach(toolbarDef -> 
                toolbarDef.botones().forEach(buttonDef -> 
                    iconMap.put(
                        buttonDef.comandoCanonico(), 
                        new ActionFactory.IconInfo(buttonDef.claveIcono(), buttonDef.scopeIconoBase())
                    )
                )
            );

            // 3. Creamos ActionFactory y le PASAMOS la variable 'iconMap' que acabamos de crear.
            this.actionFactory = new ActionFactory(
                 this.model, null, this.zoomManager, this.fileOperationsManager, 
                 this.editionManager, this.listCoordinator, this.iconUtils, this.configuration, 
                 this.projectManagerService, 
                 iconMap, // <-- Esta línea ahora funciona porque 'iconMap' existe.
                 this.viewManager, this.themeManager, 
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
            
            
            this.backgroundControlManager = new BackgroundControlManager(
            	    registry, 
            	    this.themeManager, 
            	    this.viewManager, 
            	    this.configuration, 
            	    this.iconUtils,
            	    // Puedes crear los bordes aquí o en el ViewBuilder y pasarlos
            	    BorderFactory.createLineBorder(Color.GRAY),
            	    BorderFactory.createCompoundBorder(
            	        BorderFactory.createLineBorder(Color.CYAN, 2),
            	        BorderFactory.createLineBorder(Color.BLACK)
            	    )
            	);
            
            
            //======================================================================
            // BLOQUE 2: CABLEADO (INYECCIÓN DE DEPENDENCIAS - ORDEN CORREGIDO)
            //======================================================================
            System.out.println("  -> BLOQUE 2: Cableando dependencias (orden corregido)...");

            // --- PASO 2.1: Inyectar dependencias en ViewManager PRIMERO ---
            // Le damos al ViewManager todo lo que necesita para estar 100% funcional.
            viewManager.setModel(this.model);
            viewManager.setRegistry(registry);
            viewManager.setConfiguration(this.configuration);
            viewManager.setThemeManager(this.themeManager);
            viewManager.setToolbarManager(this.toolbarManager);
            viewManager.setViewBuilder(viewBuilder);
            
            // --- PASO 2.2: Inyectar dependencias en ZoomManager ---
            // Le damos sus dependencias, incluyendo el ViewManager ya completo.
            this.zoomManager.setModel(this.model);
            this.zoomManager.setRegistry(registry);
            this.zoomManager.setConfiguration(this.configuration);
            this.zoomManager.setViewManager(this.viewManager); // <-- Ahora ViewManager está listo

            // --- PASO 2.3: Resto del cableado ---
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
            
            this.controller.setToolbarManager(this.toolbarManager);
            this.controller.setActionFactory(this.actionFactory);
            
            this.controller.setBackgroundControlManager(this.backgroundControlManager);
            this.themeManager.setConfigApplicationManager(this.configAppManager);
            
            viewBuilder.setToolbarManager(this.toolbarManager);
            menuBuilder.setControllerGlobalActionListener(this.controller);
            this.configAppManager.setBackgroundControlManager(this.backgroundControlManager);
            
            
            //======================================================================
            // BLOQUE 3: FASE DE INICIALIZACIÓN SECUENCIADA (ORDEN CORREGIDO)
            //======================================================================
            System.out.println("  -> BLOQUE 3: Ejecutando inicialización secuenciada...");

            // 1. Inicializar las Actions que NO dependen de la 'view'.
            this.actionFactory.initializeCoreActions();
            this.actionMap = this.actionFactory.getActionMap();

            
            // 2. Inyectar el ActionMap (parcial) en los builders/managers que lo necesitan para construir.
            toolbarBuilder.setActionMap(this.actionMap);
            viewBuilder.setActionMap(this.actionMap);

            // 3. Crear el frame principal (VisorView).
            this.view = viewBuilder.createMainFrame();
            
            
            // --- INICIO DEL NUEVO BLOQUE DE CABLEADO DE MODELOS ---
            System.out.println("    -> Cableando modelos de miniaturas a las JLists...");
            JList<String> miniaturasVisor = registry.get("list.miniaturas");
            if (miniaturasVisor != null) {
                // El getModeloMiniaturas() del controller es inteligente, pero aquí sabemos cuál queremos
                miniaturasVisor.setModel(this.controller.getModeloMiniaturasVisualizador()); 
            }
            JList<String> miniaturasCarrusel = registry.get("list.miniaturas.carousel");
            if (miniaturasCarrusel != null) {
                miniaturasCarrusel.setModel(this.controller.getModeloMiniaturasCarrusel());
            }
            // --- FIN DEL NUEVO BLOQUE DE CABLEADO DE MODELOS ---
            
            
            System.out.println("    -> VisorView (JFrame) creado.");

            // 4. Inyectar la 'view' en todos los componentes que la esperaban.
            System.out.println("    -> Inyectando la instancia de 'view' en los componentes...");
            this.actionFactory.setView(this.view);
            this.configAppManager.setView(this.view);
            this.viewManager.setView(this.view);
            this.controller.setView(this.view);
            this.projectController.setView(this.view);

            // 5. Ahora que la 'view' está inyectada, inicializar las Actions restantes.
            this.actionFactory.initializeViewDependentActions();
            
            this.actionFactory.setCarouselManager(carouselManager);
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
            this.statusBarManager.setController(this.controller);
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
            
            Map<String, javax.swing.AbstractButton> botones = toolbarBuilder.getBotonesPorNombre();
            this.controller.setBotonesPorNombre(botones);
            this.viewManager.setBotonesPorNombre(botones);
            
            this.controller.configurarAtajosTecladoGlobales();
            this.configAppManager.aplicarConfiguracionGlobalmente();

            // Pedir a GeneralController que configure sus listeners GLOBALES de ratón.
            this.generalController.configurarListenersDeEntradaGlobal();

            // Registrar a GeneralController como el dispatcher global de TECLADO.
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this.generalController);

            // En una fase posterior, eliminaremos los listeners de teclado/ratón duplicados de VisorController.
            this.controller.configurarListenersVistaInternal(); 
            
            this.controller.configurarMenusContextuales();

            this.projectController.configurarListeners();
            this.generalController.initialize();
            
            // Activacion de la Thumnail Preview
            System.out.println("  [AppInitializer] Instalando el previsualizador de miniaturas...");
            JList<String> miniaturasList = registry.get("list.miniaturas");

            if (miniaturasList != null) {
                // La llamada ya no necesita el ThemeManager
            	new ThumbnailPreviewer(miniaturasList, this.model, this.themeManager);
                System.out.println("    -> Previsualizador instalado en la lista de miniaturas.");
            } else {
                System.err.println("WARN: No se pudo instalar el previsualizador, 'list.miniaturas' no encontrada en el registro.");
            }
            
            this.controller.configurarListenerGlobalDeToolbars();
            
            this.view.addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent e) { controller.shutdownApplication(); }
            });
            
//            this.controller.establecerCarpetaRaizDesdeConfigInternal();
            
            
            // --- Establecer carpeta raíz inicial EXPLÍCITAMENTE en el contexto del Visualizador ---
            System.out.println("  [AppInitializer] Estableciendo carpeta raíz inicial desde config en el Modelo...");
            String folderInitPath = this.configuration.getString(ConfigKeys.INICIO_CARPETA, "");
            if (!folderInitPath.isEmpty()) {
                try {
                    Path candidatePath = Paths.get(folderInitPath);
                    if (Files.isDirectory(candidatePath)) {
                        // Usamos el método especializado del modelo para establecer el estado inicial
                        this.model.setCarpetaRaizInicialParaVisualizador(candidatePath);
                    }
                } catch (Exception e) {
                    System.err.println("WARN: Ruta de carpeta inicial inválida en config: '" + folderInitPath + "'");
                }
            }
            
            
            this.view.setVisible(true);
            
            this.backgroundControlManager.initializeAndLinkControls();
            this.backgroundControlManager.sincronizarSeleccionConEstadoActual();
            System.out.println("    -> BackgroundControlManager inicializado y enlazado a la UI.");
            
            SwingUtilities.invokeLater(() -> {
                String imagenInicialKey = configuration.getString(ConfigKeys.INICIO_IMAGEN, null);
                if (this.model.getCarpetaRaizActual() != null) {
                    this.controller.cargarListaImagenes(imagenInicialKey, () -> {
                        // Una vez que la carga de imágenes ha terminado,
                        // llamamos al método maestro de sincronización en GeneralController.
                        System.out.println("  [AppInitializer Callback] Carga inicial de imágenes completada. Llamando a sincronización maestra de UI...");
                        this.generalController.sincronizarTodaLaUIConElModelo();
                    });
                } else {
                    this.controller.limpiarUI();
                    // También llamamos a la sincronización aquí para que los botones
                    // reflejen un estado "desactivado" correcto.
                    System.out.println("  [AppInitializer] No hay carpeta inicial. Llamando a sincronización maestra de UI...");
                    this.generalController.sincronizarTodaLaUIConElModelo();
                }
            });

            System.out.println("--- [EDT] Inicialización de UI completada. Carga de datos programada. ---");

        } catch (Exception e) {
            manejarErrorFatalInicializacion("[EDT] Error fatal durante la creación de la UI", e);
        }
    } // --- FIN del método crearUIyComponentesDependientesEnEDT ---
    

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
    } // --- FIN del método manejarErrorFatalInicializacion ---

    public void setControllerParaNotificacion(VisorController controller) {
        this.controllerRefParaNotificacion = controller;
    } // --- FIN del método setControllerParaNotificacion ---
    
} // --- FIN de la clase AppInitializer ---

