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
     * FASE A.4: Inicializa el servicio de gestión de proyectos (ProjectManager).
     *           Se asume que solo depende de ConfigurationManager por ahora.
     *           Inyecta la dependencia en VisorController.
     * @return true si éxito, false si falla.
     */
    private boolean inicializarServicioDeProyectos() {
        System.out.println("  [AppInitializer Fase A.4] Inicializando Servicio de Proyectos...");
        try {
            this.projectManagerService = new ProjectManager(this.configuration);
            this.controller.setProjectManager(this.projectManagerService);
            System.out.println("    -> ProjectManager (servicio) creado e inyectado.");
            return true;
        } catch (Exception e) {
            manejarErrorFatalInicializacion("Error inicializando ProjectManager (servicio)", e);
            return false;
        }
    }



    // --- MÉTODO PARA INICIALIZACIÓN DENTRO DEL EDT ---


 // En AppInitializer.java

    private void crearUIyComponentesDependientesEnEDT() {
        try {
            System.out.println("--- [AppInitializer Fase B - EDT] Iniciando creación de UI (Orden Lógico Estable)...");

            // --- FASE 1: Crear componentes base y managers SIN dependencias de UI complejas ---
            ComponentRegistry registry = new ComponentRegistry();
            this.controller.setComponentRegistry(registry);

            this.zoomManager = new ZoomManager(this.model, registry, this.configuration);
            this.editionManager = new EditionManager(this.model, this.controller, this.zoomManager);

            // --- FASE 2: Crear ViewManager (dependencia para Builders y Actions) ---
            // Lo creamos ahora, pasándole null para la vista, que se la inyectaremos después.
            this.viewManager = new ViewManager(null, this.configuration, registry);
            
            // --- FASE 3: Crear los Builders ---
            ProjectBuilder projectBuilder = new ProjectBuilder(registry, this.model);
            ViewBuilder viewBuilder = new ViewBuilder(
                registry, this.model, this.themeManager, this.configuration,
                this.controller, this.iconUtils, this.servicioMiniaturas,
                projectBuilder
            );
            // Inyectamos el ViewManager en el ViewBuilder usando el setter
            viewBuilder.setViewManager(this.viewManager);

            // --- FASE 4: Construir la Vista y los componentes que dependen de ella ---
            this.view = viewBuilder.createMainFrame();
            
            // Inyectar la vista en los managers que la necesitan
            this.viewManager.setView(this.view);
            this.listCoordinator = new ListCoordinator(this.model, this.view, this.controller, registry);
            
            // --- FASE 5: Crear la Fábrica de Acciones (ahora que TODOS los managers existen) ---
            UIDefinitionService uiDefSvc = new UIDefinitionService();
            Map<String, String> iconMap = new java.util.HashMap<>();
            uiDefSvc.generateModularToolbarStructure().forEach(td -> td.botones().forEach(bd -> iconMap.put(bd.comandoCanonico(), bd.claveIcono())));
            java.util.function.Consumer<java.nio.file.Path> cb = (p) -> this.controller.cargarListaImagenes(null, null);
            this.fileOperationsManager = new FileOperationsManager(this.model, this.controller, this.configuration, cb);

            this.actionFactory = new ActionFactory(
                 this.model, this.view, this.zoomManager, this.fileOperationsManager, 
                 this.editionManager, this.listCoordinator, this.iconUtils, this.configuration, 
                 this.projectManagerService, iconMap, this.viewManager, this.themeManager, this.controller
            );
            this.actionFactory.initializeActions();
            this.actionMap = this.actionFactory.getActionMap();
            
            // --- FASE 6: Cableado Final y Arranque ---
            // Inyectar todas las dependencias finales en el controlador y otros managers
            this.controller.setView(this.view);
            this.controller.setZoomManager(this.zoomManager);
            this.controller.setListCoordinator(this.listCoordinator);
            this.controller.setViewManager(this.viewManager);
            this.controller.setActionMap(this.actionMap);
            
            this.viewManager.setActionMap(this.actionMap);
            this.listCoordinator.setContextSensitiveActions(this.actionFactory.getContextSensitiveActions());
            this.zoomManager.setListCoordinator(this.listCoordinator);
            
            // Crear y cablear los managers de UI
            this.configAppManager = new ConfigApplicationManager(this.model, this.view, this.configuration, this.actionMap, this.themeManager, registry);
            this.infobarImageManager = new InfobarImageManager(this.model, registry, this.configuration);
            this.statusBarManager = new InfobarStatusManager(this.model, registry, this.themeManager, this.configuration, this.projectManagerService, this.actionMap, this.iconUtils);
            this.controller.setConfigApplicationManager(configAppManager);
            this.controller.setInfobarImageManager(infobarImageManager);
            this.controller.setStatusBarManager(statusBarManager);
            this.zoomManager.setStatusBarManager(statusBarManager);

            // Poblar la UI
            MenuBarBuilder menuBuilder = new MenuBarBuilder(this.controller, this.configuration, this.viewManager);
            menuBuilder.setControllerGlobalActionListener(this.controller);
            this.view.setJMenuBar(menuBuilder.buildMenuBar(uiDefSvc.generateMenuStructure(), this.actionMap));
            this.controller.setMenuItemsPorNombre(menuBuilder.getMenuItemsMap());
            menuBuilder.getMenuItemsMap().forEach(registry::register);

            ToolbarBuilder toolbarBuilder = new ToolbarBuilder(this.actionMap, this.themeManager, this.iconUtils, this.controller, configuration.getInt("iconos.ancho", 24), configuration.getInt("iconos.alto", 24));
            ToolbarManager toolbarManager = new ToolbarManager(this.configuration, toolbarBuilder, uiDefSvc, registry);
            toolbarManager.inicializarBarrasDeHerramientas();
            Map<String, javax.swing.JButton> botones = toolbarBuilder.getBotonesPorNombre();
            this.controller.setBotonesPorNombre(botones);
            this.viewManager.setBotonesPorNombre(botones);

            // Arranque
            this.configAppManager.aplicarConfiguracionGlobalmente();
            this.controller.configurarListenersVistaInternal();
            java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this.controller);
            this.view.addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent e) { controller.shutdownApplication(); }
            });
            
            this.controller.establecerCarpetaRaizDesdeConfigInternal();
            String imagenInicialKey = configuration.getString(ConfigKeys.INICIO_IMAGEN, null);
            
            this.view.setVisible(true);
            
            if (this.model.getCarpetaRaizActual() != null) {
                this.controller.cargarListaImagenes(imagenInicialKey, null);
            } else {
                this.controller.limpiarUI();
            }
            
            System.out.println("--- [EDT] Inicialización de UI completada. ---");

        } catch (Exception e) {
            manejarErrorFatalInicializacion("[EDT] Error fatal durante la creación de la UI", e);
            // Imprime el stack trace aquí mismo para verlo directamente en la consola de errores
            e.printStackTrace();
        }
    }

    
    

    

    
    
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