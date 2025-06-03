package controlador;

import java.awt.Color;
import java.awt.KeyboardFocusManager;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap; // Para el mapa de comando -> claveIcono
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;  // Para el panel de la toolbar
import javax.swing.SwingUtilities;
import javax.swing.Timer; // Para el listener de redimensionamiento

import controlador.actions.especiales.MenuAction;
import controlador.commands.AppActionCommands;
import controlador.factory.ActionFactory; // La fábrica de Actions
import controlador.interfaces.ContextSensitiveAction;
import controlador.managers.EditionManager;
import controlador.managers.FileOperationsManager; // Futuro
import controlador.managers.InfoBarManager;
import controlador.managers.ViewManager;
// Managers
import controlador.managers.ZoomManager;
import modelo.VisorModel;
import servicios.ConfigurationManager;
import servicios.ProjectManager; // Asumiendo que este es tu servicio de proyectos
import servicios.image.ThumbnailService;
import servicios.zoom.ZoomModeEnum;
import vista.VisorView;
import vista.builders.MenuBarBuilder;
import vista.builders.ToolbarBuilder;
import vista.config.MenuItemDefinition;
import vista.config.ToolbarButtonDefinition;
import vista.config.UIDefinitionService;
import vista.config.ViewUIConfig;       // Si aún la usas o la refactorizas
import vista.theme.Tema;
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
    private ThemeManager themeManager;
    private IconUtils iconUtils;
    private ThumbnailService servicioMiniaturas;
    private ProjectManager projectManagerService; // Servicio de persistencia de proyectos
    private VisorController controllerRefParaNotificacion; 
    
    // 1.2. Componentes de UI y Coordinadores (creados DENTRO del EDT)
    private VisorView view;
    private ListCoordinator listCoordinator;
    
    // 1.3. Managers (creados DENTRO del EDT, después de la Vista si dependen de ella)
    private ZoomManager zoomManager;
    private EditionManager editionManager; 
    private FileOperationsManager fileOperationsManager;
    private InfoBarManager infoBarManager;
    private boolean zoomManualEstabaActivoAntesDeError = false;

    // private ViewUIManager viewUIManager;
    // private ProjectActionsManager projectActionsManager;

//    private List<ContextSensitiveAction> sensitiveActionsList;
    
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
            this.configuration = new ConfigurationManager(); // Puede lanzar IOException
            this.controller.setConfigurationManager(this.configuration); // Inyectar en VisorController
            System.out.println("    -> ConfigurationManager creado, cargado e inyectado.");

            return true; // Éxito de la fase
        } catch (IOException e) {
            manejarErrorFatalInicializacion("Error fatal al inicializar ConfigurationManager", e);
            return false; 
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
        String initialZoomModeStr = this.configuration.getString(ConfigurationManager.KEY_COMPORTAMIENTO_DISPLAY_ZOOM_INITIAL_MODE, "FIT_TO_SCREEN").toUpperCase();
        try {
            ZoomModeEnum initialMode = ZoomModeEnum.valueOf(initialZoomModeStr);
            this.model.setCurrentZoomMode(initialMode);
            System.out.println("    -> Modo de zoom inicial del modelo (comportamiento.display.zoom.initial_mode) establecido desde config a: " + initialMode);
        } catch (IllegalArgumentException e) {
            System.err.println("WARN [AppInitializer]: Valor inválido para '" + 
                               ConfigurationManager.KEY_COMPORTAMIENTO_DISPLAY_ZOOM_INITIAL_MODE + 
                               "' en config: '" + initialZoomModeStr + 
                               "'. Usando FIT_TO_SCREEN por defecto.");
            this.model.setCurrentZoomMode(ZoomModeEnum.FIT_TO_SCREEN);
        }
        
        // Aplicar Estado Inicial de Tipo de Zoom
        String ultimoModoZoomStr = this.configuration.getString(
       	    ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_ULTIMO_MODO_SELECCIONADO, "FIT_TO_SCREEN").toUpperCase();
        	ZoomModeEnum modoAEstablecer;
        	try {
        	    modoAEstablecer = ZoomModeEnum.valueOf(ultimoModoZoomStr);
        	} catch (IllegalArgumentException e) {
        	    System.err.println("WARN [AppInitializer]: Valor inválido para '" + 
        	                       ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_ULTIMO_MODO_SELECCIONADO +
        	                       "' en config: '" + ultimoModoZoomStr + 
        	                       "'. Usando FIT_TO_SCREEN por defecto.");
        	    modoAEstablecer = ZoomModeEnum.FIT_TO_SCREEN;
        	}
        	this.model.setCurrentZoomMode(modoAEstablecer);
        	System.out.println("    -> Modelo: Modo de zoom actual (desde ultimo_modo_seleccionado) establecido a: " + modoAEstablecer);
        
        try {
            // Aplicar configuración de miniaturas al modelo
            this.model.setMiniaturasAntes(configuration.getInt("miniaturas.cantidad.antes", VisorController.DEFAULT_MINIATURAS_ANTES_FALLBACK));
            this.model.setMiniaturasDespues(configuration.getInt("miniaturas.cantidad.despues", VisorController.DEFAULT_MINIATURAS_DESPUES_FALLBACK));
            this.model.setMiniaturaSelAncho(configuration.getInt("miniaturas.tamano.seleccionada.ancho", 60));
            this.model.setMiniaturaSelAlto(configuration.getInt("miniaturas.tamano.seleccionada.alto", 60));
            this.model.setMiniaturaNormAncho(configuration.getInt("miniaturas.tamano.normal.ancho", 40));
            this.model.setMiniaturaNormAlto(configuration.getInt("miniaturas.tamano.normal.alto", 40));

            // Aplicar configuración de comportamiento al modelo
            boolean cargarSubcarpetas = configuration.getBoolean("comportamiento.carpeta.cargarSubcarpetas", true);
            boolean zoomManualInicialActivo = this.configuration.getBoolean(
            		ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_MANUAL_INICIAL_ACTIVO, true);// Valor por defecto si la clave no existe en el archivo (aunque debería estar en DEFAULT_CONFIG)
            this.model.setZoomHabilitado(zoomManualInicialActivo);
            System.out.println("    -> Modelo: Zoom manual inicial activo (desde config 'comportamiento...'): " + zoomManualInicialActivo);
            this.model.setMostrarSoloCarpetaActual(!cargarSubcarpetas); 
            boolean mantenerProp = configuration.getBoolean("interfaz.menu.zoom.mantener_proporciones.seleccionado", true); // Clave de config directa
            this.model.setMantenerProporcion(mantenerProp);
            boolean navCircular = configuration.getBoolean("comportamiento.navegacion.circular", false);
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
    /**
     * FASE B (EDT): Crea la VisorView, los Managers que dependen de la vista,
     *               ListCoordinator, ActionFactory, y finalmente construye
     *               la UI completa (menús, toolbars) y configura los listeners.
     *               Este método DEBE ejecutarse en el Event Dispatch Thread.
     */
    private void crearUIyComponentesDependientesEnEDT() {
        System.out.println("  [AppInitializer Fase B - EDT] Iniciando creación de UI y componentes dependientes...");
        
        try {
            // --- B.1. PREPARACIÓN DE VALORES INICIALES (Ej. altura panel miniaturas) ---
            int selThumbH = (this.model != null) ? this.model.getMiniaturaSelAlto() : 60; // Default si el modelo no está o no tiene el valor
            int calculatedMiniaturePanelHeightForController = Math.max(80, selThumbH + 40); // Mínimo 80px o basado en miniatura seleccionada
            this.controller.setCalculatedMiniaturePanelHeight(calculatedMiniaturePanelHeightForController);
            // Dimensiones de iconos por defecto (pueden ser de config)
            int anchoIconoCfg = (this.configuration != null) ? this.configuration.getInt("iconos.ancho", 24) : 24;
            int altoIconoCfg = (this.configuration != null) ? this.configuration.getInt("iconos.alto", 24) : 24;
            Tema temaActualParaUiConfig = (this.themeManager != null) ? this.themeManager.getTemaActual() : null; // Manejar null si themeManager falla

            if (temaActualParaUiConfig == null) { // Fallback si no hay tema
                System.err.println("WARN [AppInitializer B.1]: themeManager o temaActual es null. Usando colores de fallback para ViewUIConfig.");
                // Crear un tema de fallback o usar colores directos
                temaActualParaUiConfig = new Tema("fallback", "Fallback", "black", /* Colores de fallback */
                    new Color(240, 240, 240), new Color(220, 220, 220), /*...*/ Color.BLACK, Color.DARK_GRAY, 
                    Color.GRAY, Color.DARK_GRAY, new Color(50,100,150), Color.WHITE, 
                    new Color(200,200,200), Color.BLACK, new Color(180,180,180), new Color(150,200,250), Color.ORANGE
                );
            }

            // --- B.1.bis. CREAR ViewUIConfig INICIAL (SIN ActionMap) ---
            //      Esta instancia se usará para construir VisorView y ActionFactory (que necesita IconUtils de uiConfig).
            //      Pasamos 'null' para el actionMap porque aún no se ha creado completamente.
            ViewUIConfig uiConfigInicialParaVistaYFactory = new ViewUIConfig(
                temaActualParaUiConfig.colorFondoPrincipal(), temaActualParaUiConfig.colorBotonFondoActivado(), temaActualParaUiConfig.colorBotonFondoAnimacion(),
                anchoIconoCfg, altoIconoCfg,
                null, // <<--- ActionMap es null aquí
                this.iconUtils, // IconUtils ya está inicializado
                temaActualParaUiConfig.colorFondoPrincipal(), temaActualParaUiConfig.colorFondoSecundario(), temaActualParaUiConfig.colorTextoPrimario(),
                temaActualParaUiConfig.colorTextoSecundario(), temaActualParaUiConfig.colorBorde(), temaActualParaUiConfig.colorBordeTitulo(),
                temaActualParaUiConfig.colorSeleccionFondo(), temaActualParaUiConfig.colorSeleccionTexto(), temaActualParaUiConfig.colorBotonFondo(),
                temaActualParaUiConfig.colorBotonTexto(), temaActualParaUiConfig.colorBotonFondoActivado(), temaActualParaUiConfig.colorBotonFondoAnimacion(),
                temaActualParaUiConfig.colorBordeSeleccionActiva(), this.configuration
            );
            System.out.println("    B.1.bis. [EDT] ViewUIConfig INICIAL (sin actionMap) creado.");

            // --- B.2. CREAR VisorView CON UIConfig INICIAL ---
            //      VisorView necesita un uiConfig para su construcción inicial (colores, IconUtils).
            this.view = new VisorView(
                calculatedMiniaturePanelHeightForController,
                uiConfigInicialParaVistaYFactory, // Pasa el uiConfig sin el actionMap completo
                this.model, 
                this.servicioMiniaturas,
                new JMenuBar(),       // Placeholder para JMenuBar
                new JPanel(),         // Placeholder para Toolbar Panel
                Collections.emptyMap(), // Placeholder para menuItems
                Collections.emptyMap()  // Placeholder para botones
            );
            this.controller.setView(this.view); // Inyectar la Vista en el Controlador
            System.out.println("    B.2. [EDT] VisorView instanciada con uiConfigInicial e inyectada en Controller.");

            // --- B.3. CREAR MANAGERS QUE DEPENDEN DE LA VISTA Y OTRAS DEPENDENCIAS ---
            this.zoomManager = new ZoomManager(this.model, this.view, this.configuration);
            this.controller.setZoomManager(this.zoomManager);
            System.out.println("    B.3.1. [EDT] ZoomManager creado e inyectado.");

            this.viewManager = new ViewManager(this.view, this.configuration);
            // this.controller.setViewManager(this.viewManager); // Si el controller lo necesita
            System.out.println("    B.3.2. [EDT] ViewManager creado.");

            Consumer<Path> recargarDesdeNuevaCarpetaCallback = (nuevaCarpeta) -> {
                if (this.controller != null) {
                    // Podrías querer actualizar model.setCarpetaRaizActual(nuevaCarpeta) aquí también
                    // antes de llamar a cargarListaImagenes.
                    this.controller.cargarListaImagenes(null); // Carga la nueva carpeta, seleccionando la primera imagen
                }
            };
            this.fileOperationsManager = new FileOperationsManager(this.model, this.view, this.configuration, recargarDesdeNuevaCarpetaCallback);
            // this.controller.setFileOperationsManager(this.fileOperationsManager); // Si el controller lo necesita
            System.out.println("    B.3.3. [EDT] FileOperationsManager creado.");
            
            if (this.editionManager == null) {
            	this.editionManager = new EditionManager(this.model, this.view, this.zoomManager);
            	System.out.println("    B.3.4. [EDT] EditionManager creado.");
            }
            
            // --- B.4. CREAR ListCoordinator ---
            this.listCoordinator = new ListCoordinator(this.model, this.view, this.controller);
            this.controller.setListCoordinator(this.listCoordinator);
            System.out.println("    B.4. [EDT] ListCoordinator creado e inyectado.");
            
            // --- B.5. CREAR ActionFactory ---
            //      ActionFactory necesita varias dependencias, incluyendo uiConfigInicialParaVistaYFactory (para IconUtils).
            UIDefinitionService uiDefSvcForIcons = new UIDefinitionService(); // Para la estructura de botones/iconos
            List<ToolbarButtonDefinition> toolbarDefsForIcons = uiDefSvcForIcons.generateToolbarStructure();
            Map<String, String> comandoToIconKeyMap = new HashMap<>();
            for (ToolbarButtonDefinition def : toolbarDefsForIcons) {
                if (def.comandoCanonico() != null && def.claveIcono() != null) {
                    comandoToIconKeyMap.put(def.comandoCanonico(), def.claveIcono());
                }
            }
            this.actionFactory = new ActionFactory(
                this.model, this.view, this.zoomManager, this.fileOperationsManager,
                this.editionManager, this.listCoordinator, this.iconUtils, this.configuration,
                this.projectManagerService, comandoToIconKeyMap, this.viewManager,
                this.themeManager, uiConfigInicialParaVistaYFactory, this.controller
            );
            
            // --- B.6. OBTENER EL MAPA DE ACCIONES BASE (MUTABLE) DESDE ActionFactory ---
            //      Este mapa NO contendrá MenuAction si la eliminaste de ActionFactory.initializeAllActions().
            //      Asegúrate que ActionFactory.getActionMap() devuelve el mapa mutable interno.
            this.actionMap = this.actionFactory.getActionMap(); 
            System.out.println("    B.6. [EDT] ActionFactory creada. ActionMap base obtenido (Tamaño: " + this.actionMap.size() + ").");

            // --- B.7. CREAR ViewUIConfig COMPLETO ---
            //      Ahora creamos el ViewUIConfig que SÍ tendrá una referencia al actionMap (aún sin MenuAction).
            //      Este uiConfig se usará para MenuAction y los Builders.
            ViewUIConfig uiConfigParaBuildersYMenuAction = new ViewUIConfig(
                temaActualParaUiConfig.colorFondoPrincipal(), temaActualParaUiConfig.colorBotonFondoActivado(), temaActualParaUiConfig.colorBotonFondoAnimacion(),
                anchoIconoCfg, altoIconoCfg,
                this.actionMap, // Pasa la referencia al actionMap actual (con ~65 actions)
                this.iconUtils,
                temaActualParaUiConfig.colorFondoPrincipal(), temaActualParaUiConfig.colorFondoSecundario(), temaActualParaUiConfig.colorTextoPrimario(),
                temaActualParaUiConfig.colorTextoSecundario(), temaActualParaUiConfig.colorBorde(), temaActualParaUiConfig.colorBordeTitulo(),
                temaActualParaUiConfig.colorSeleccionFondo(), temaActualParaUiConfig.colorSeleccionTexto(), temaActualParaUiConfig.colorBotonFondo(),
                temaActualParaUiConfig.colorBotonTexto(), temaActualParaUiConfig.colorBotonFondoActivado(), temaActualParaUiConfig.colorBotonFondoAnimacion(),
                temaActualParaUiConfig.colorBordeSeleccionActiva(), this.configuration
            );
            System.out.println("    B.7. [EDT] ViewUIConfig para Builders y MenuAction creado (usando actionMap de " + this.actionMap.size() + " actions).");

            // --- B.7.bis. CREAR Y AÑADIR MenuAction AL this.actionMap ---
            System.out.println("    B.7.bis. [EDT] Creando MenuAction y añadiéndola al this.actionMap...");
            if (this.actionMap != null && uiConfigParaBuildersYMenuAction != null && this.iconUtils != null && 
                this.view != null && this.actionFactory != null) {

                UIDefinitionService uiDefSvc = new UIDefinitionService(); // Puedes reutilizar uiDefSvcForIcons si prefieres
                List<MenuItemDefinition> menuStructure = uiDefSvc.generateMenuStructure();
                
                String menuActionIconKey = null;
                Map<String, String> cmdToIconMap = this.actionFactory.getComandoToIconKeyMap();

                if (cmdToIconMap != null) {
                     menuActionIconKey = cmdToIconMap.get(AppActionCommands.CMD_ESPECIAL_MENU);
                }
                if (menuActionIconKey == null) { 
                    menuActionIconKey = "6002-Menu_48x48.png"; // Fallback
                    System.out.println("      WARN [AppInitializer B.7.bis]: Clave de icono para MenuAction no encontrada. Usando fallback: " + menuActionIconKey);
                }

                ImageIcon menuIcon = this.iconUtils.getScaledIcon(
                    menuActionIconKey,
                    this.actionFactory.getIconoAncho(), 
                    this.actionFactory.getIconoAlto()   
                );
                if (menuIcon == null) { System.err.println("WARN [AppInitializer B.7.bis]: No se pudo cargar/escalar el icono para MenuAction: " + menuActionIconKey); }

                
                MenuAction menuActionFinal = new MenuAction(
                	    "Menú Principal",    
                	    menuIcon,            
                	    menuStructure,       
                	    this.actionMap,
                	    uiConfigParaBuildersYMenuAction,        // ViewUIConfig
                	    this.configuration,                   // <--- AÑADIR ConfigurationManager
                	    this.controller                       // <--- AÑADIR VisorController (como ActionListener)
                	);
                
                // Añadir la MenuAction al mapa mutable.
                this.actionMap.put(AppActionCommands.CMD_ESPECIAL_MENU, menuActionFinal);
                System.out.println("      -> MenuAction final añadida a this.actionMap. Tamaño actual del actionMap: " + this.actionMap.size()); // Debería ser 66

            } else { /* ... log de error de dependencias nulas para MenuAction ... */ 
                System.err.println("ERROR CRÍTICO [AppInitializer B.7.bis]: No se pudo crear MenuAction por dependencias nulas.");
            }
            // --- FIN B.7.bis ---

            // --- B.7.ter. INYECTAR EL ACTIONMAP COMPLETO y UICONFIG COMPLETO ---
            //      Ahora this.actionMap tiene todas las actions, incluida MenuAction.
            //      Y uiConfigParaBuildersYMenuAction referencia este actionMap completo.
            this.controller.setActionMap(this.actionMap); // Inyectar actionMap COMPLETO en Controller
            this.controller.setUiConfigForView(uiConfigParaBuildersYMenuAction); // Inyectar uiConfig COMPLETO en Controller
            this.view.setUiConfig(uiConfigParaBuildersYMenuAction); // Actualizar uiConfig en Vista con el que tiene el actionMap completo
            System.out.println("    B.7.ter. [EDT] ActionMap FINAL (con MenuAction) y ViewUIConfig correspondiente inyectados en Controller y View.");

            // --- B.8. INYECTAR LISTA DE ContextSensitiveActions EN ListCoordinator ---
            if (this.actionFactory != null) { // actionFactory debería existir
                List<ContextSensitiveAction> sensitiveActions = this.actionFactory.getContextSensitiveActions();
                if (this.listCoordinator != null && sensitiveActions != null) {
                    this.listCoordinator.setContextSensitiveActions(sensitiveActions);
                    System.out.println("    B.8. [EDT] Lista de ContextSensitiveActions inyectada en ListCoordinator.");
                } else { /* ... log error ... */ }
            } else { /* ... log error ... */ }
            
            // --- B.9. CREAR InfoBarManager ---
            //      Asegúrate que InfoBarManager usa el uiConfigParaBuildersYMenuAction si necesita el actionMap.
            this.infoBarManager = new InfoBarManager(this.model, this.view, uiConfigParaBuildersYMenuAction, comandoToIconKeyMap, this.projectManagerService);
            this.controller.setInfoBarManager(this.infoBarManager);
            System.out.println("    B.9. [EDT] InfoBarManager creado.");
            
            // --- B.10. INYECCIÓN CRUZADA ZoomManager <-> InfoBarManager ---
            if (this.zoomManager != null && this.infoBarManager != null) {
                this.zoomManager.setInfoBarManager(this.infoBarManager);
                System.out.println("    B.10. [EDT] InfoBarManager inyectado en ZoomManager.");
            } else { /* ... log error ... */ }
            
            // Sincronizar estado inicial de radios de formato de InfoBar
            if (this.controller != null) {
                this.controller.sincronizarEstadoVisualInicialDeRadiosDeFormato();
                System.out.println("    B.10.1. [EDT] Estado inicial de radios de formato InfoBar sincronizado.");
            }
            
            // --- B.11. CONSTRUIR MENÚ Y TOOLBAR REALES USANDO BUILDERS ---
            //      Los builders ahora usarán this.actionMap (completo) y uiConfigParaBuildersYMenuAction.
            List<MenuItemDefinition> menuStructureForBuilder = uiDefSvcForIcons.generateMenuStructure(); // Reutilizar o regenerar
            List<ToolbarButtonDefinition> toolbarStructureForBuilder = toolbarDefsForIcons; 

            MenuBarBuilder menuBuilder = new MenuBarBuilder();
            if (this.controller != null) {
                menuBuilder.setControllerGlobalActionListener(this.controller);
            } else { /* ... log error ... */ }
            JMenuBar finalMenuBar = menuBuilder.buildMenuBar(menuStructureForBuilder, this.actionMap); // USA EL ACTIONMAP COMPLETO
            this.view.setActualJMenuBar(finalMenuBar);
            this.view.setActualMenuItemsMap(menuBuilder.getMenuItemsMap());

            ToolbarBuilder toolbarBuilder = new ToolbarBuilder(
                 this.actionMap, // USA EL ACTIONMAP COMPLETO
                 uiConfigParaBuildersYMenuAction.colorBotonFondo, uiConfigParaBuildersYMenuAction.colorBotonTexto,
                 uiConfigParaBuildersYMenuAction.colorBotonActivado, uiConfigParaBuildersYMenuAction.colorBotonAnimacion,
                 uiConfigParaBuildersYMenuAction.iconoAncho, uiConfigParaBuildersYMenuAction.iconoAlto,
                 uiConfigParaBuildersYMenuAction.iconUtils, 
                 this.controller
            );
            JPanel finalToolbarPanel = toolbarBuilder.buildToolbar(toolbarStructureForBuilder);
            this.view.setActualToolbarPanel(finalToolbarPanel);
            this.view.setActualBotonesMap(toolbarBuilder.getBotonesPorNombre());
            System.out.println("    B.11. [EDT] Menú y Toolbar reales construidos (usando actionMap completo) y asignados a VisorView.");

            // --- B.11.bis. VERIFICACIÓN DEL BOTÓN DE MENÚ EN TOOLBAR (OPCIONAL LOG) ---
            //      ToolbarBuilder ya debería haber asignado la menuActionFinal correcta al botón
            //      porque la encontró en el actionMap que se le pasó.
            JButton botonMenuToolbarCheck = this.view.getBotonesPorNombre().get("interfaz.boton.especiales.Menu_48x48");
            if (botonMenuToolbarCheck != null && botonMenuToolbarCheck.getAction() == this.actionMap.get(AppActionCommands.CMD_ESPECIAL_MENU) ) {
                System.out.println("      VERIFICACIÓN [AppInitializer]: Botón de toolbar 'Menu_48x48' tiene la MenuAction correcta asignada por ToolbarBuilder.");
            } else if (botonMenuToolbarCheck != null) {
                 System.err.println("      ERROR VERIFICACIÓN [AppInitializer]: Botón de toolbar 'Menu_48x48' NO tiene la MenuAction correcta. Action actual: " + botonMenuToolbarCheck.getAction());
            } else {
                 System.err.println("      ERROR VERIFICACIÓN [AppInitializer]: Botón 'interfaz.boton.especiales.Menu_48x48' no encontrado después de construir toolbar.");
            }

            // --- B.12. FINALIZAR CONFIGURACIÓN DEL CONTROLADOR Y VISTA ---
            this.controller.assignModeloMiniaturasToViewInternal();
            this.controller.establecerCarpetaRaizDesdeConfigInternal();
            this.controller.aplicarConfigAlaVistaInternal();
            this.controller.sincronizarEstadoVisualBotonesYRadiosZoom(); // Sincroniza también el último modo de zoom a config
            this.controller.cargarEstadoInicialInternal();
            this.controller.configurarListenersVistaInternal();
            this.controller.interceptarAccionesTecladoListas();
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this.controller);
            configurarListenerRedimensionVentanaEDT();
            this.controller.sincronizarUIFinalInternal();
            this.controller.configurarShutdownHookInternal();

            // Sincronización inicial de la visibilidad del botón "Menú Especial" en toolbar
            Action toggleMenuActionInstance = this.actionMap.get(AppActionCommands.CMD_VISTA_TOGGLE_MENU_BAR);
            if (toggleMenuActionInstance != null && this.view != null && this.view.getBotonesPorNombre() != null) {
                
            	boolean menuBarVisibleInicialmente = Boolean.TRUE.equals(toggleMenuActionInstance.getValue(Action.SELECTED_KEY));
            	
            	
            	if (this.configuration != null && this.view != null) {
            	    // Obtener la clave de config que ToggleMenuBarAction usa para guardar su estado
            	    String menuBarSelectedConfigKey = "interfaz.menu.vista.barra_de_menu.seleccionado"; // Asegúrate que esta es la clave correcta
            	    boolean shouldMenuBarBeVisible = this.configuration.getBoolean(menuBarSelectedConfigKey, true); // true es el default si no se encuentra
            	    
            	    
//            	    System.out.println("  [AppInitializer] Leyendo directamente de config para JMenuBar: '" + menuBarSelectedConfigKey + "' -> " + shouldMenuBarBeVisible);
            	    this.view.setJMenuBarVisible(shouldMenuBarBeVisible);
//            	    System.out.println("  [AppInitializer] Visibilidad inicial de JMenuBar establecida a: " + shouldMenuBarBeVisible);

            	    // Sincronizar Action.SELECTED_KEY (IMPORTANTE si otros componentes dependen de él después)
            	    Action toggleMenuBarAction = (this.actionMap != null) ? this.actionMap.get(AppActionCommands.CMD_VISTA_TOGGLE_MENU_BAR) : null;
            	    if (toggleMenuBarAction != null) {
            	        // Forzar el SELECTED_KEY de la Action a que coincida con lo que realmente se aplicó a la vista
            	        if (!Objects.equals(toggleMenuBarAction.getValue(Action.SELECTED_KEY), shouldMenuBarBeVisible)) {
            	            toggleMenuBarAction.putValue(Action.SELECTED_KEY, shouldMenuBarBeVisible);
            	            System.out.println("    -> [AppInitializer] Action.SELECTED_KEY para ToggleMenuBarAction sincronizado a: " + shouldMenuBarBeVisible);
            	        }
            	    }
            	}
            	
            	
            	
            	JButton botonMenuEspecialToolbar = this.view.getBotonesPorNombre().get("interfaz.boton.especiales.Menu_48x48");
                if (botonMenuEspecialToolbar != null) {
                    botonMenuEspecialToolbar.setVisible(!menuBarVisibleInicialmente); // Se muestra si la JMenuBar está oculta
                    System.out.println("    B.12.1. [EDT] Visibilidad inicial del botón Menu_48x48 (toolbar) establecida a: " + !menuBarVisibleInicialmente);
                }
            }
            
            // Forzar actualización inicial del estado de todas las acciones contextuales
            if (this.listCoordinator != null) {
                this.listCoordinator.forzarActualizacionEstadoAcciones();
            }

            System.out.println("  [AppInitializer Fase B - EDT] Inicialización de UI y componentes dependientes completada.");
            System.out.println("--- AppInitializer: INICIALIZACIÓN GLOBAL COMPLETADA ---");
            
            this.view.setVisible(true); // Hacer visible la ventana principal al final de todo
            
        } catch (Exception e) {
            manejarErrorFatalInicializacion("[EDT] Error fatal durante creación de UI o componentes dependientes", e);
        }
    } // --- FIN del método crearUIyComponentesDependientesEnEDT ---
    
    
    /**
     * (NUEVO MÉTODO PRIVADO en AppInitializer, ejecutado en EDT)
     * Configura un ComponentListener en la ventana principal (VisorView)
     * para detectar cambios de tamaño y recalcular dinámicamente la cantidad
     * de miniaturas visibles en la barra de miniaturas.
     * Utiliza un Timer para "debouncing".
     */
    private void configurarListenerRedimensionVentanaEDT() {
        // Validar que las dependencias (this.view, this.controller, this.listCoordinator) estén listas
        if (this.view != null && this.controller != null && this.listCoordinator != null) {
            System.out.println("    -> [EDT AppInitializer] Configurando ComponentListener para miniaturas dinámicas...");

            this.view.getFrame().addComponentListener(new java.awt.event.ComponentAdapter() {
                private Timer resizeTimer; // Timer para debouncing

                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    if (resizeTimer != null && resizeTimer.isRunning()) {
                        resizeTimer.restart(); // Reiniciar el timer si ya está corriendo
                    } else {
                        // Crear un nuevo timer si no existe o si el anterior ya se disparó
                        resizeTimer = new Timer(WINDOW_RESIZE_UPDATE_DELAY_MS, ae -> {
                            System.out.println("      [WindowResized Timer - AppInitializer] Ejecutando recálculo de miniaturas...");
                            
                            // Obtener el índice actualmente seleccionado por el ListCoordinator
                            // Es importante usar el índice "oficial" del ListCoordinator
                            int indiceActualOficial = listCoordinator.getIndiceOficialSeleccionado();
                            
                            // Validar que el modelo y el ListCoordinator estén listos
                            // y que haya una selección válida (índice != -1)
                            if (model != null && indiceActualOficial != -1) {
                                // Llamar al método del VisorController que actualiza las miniaturas,
                                // pasándole el índice oficial.
                                controller.actualizarModeloYVistaMiniaturas(indiceActualOficial);
                            } else {
                                System.out.println("      [WindowResized Timer - AppInitializer] Modelo no listo o sin selección oficial, no se actualizan miniaturas.");
                            }
                        });
                        resizeTimer.setRepeats(false); // El timer solo se dispara una vez
                        resizeTimer.start(); // Iniciar el timer
                    }
                }
            });
            System.out.println("    -> [EDT AppInitializer] ComponentListener para miniaturas dinámicas añadido a VisorView.");
        } else {
            System.err.println("ERROR [AppInitializer EDT - configurarListenerRedimensionVentana]: No se pudo añadir ComponentListener (vista, controller o listCoordinator nulos).");
        }
    } // --- FIN configurarListenerRedimensionVentanaEDT ---


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