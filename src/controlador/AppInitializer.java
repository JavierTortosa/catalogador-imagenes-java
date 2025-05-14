package controlador; // O controlador.initialization

import java.awt.KeyboardFocusManager;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors; // Para crear ExecutorService

import javax.swing.Action; // Para Map<String, Action>
import javax.swing.DefaultListModel; // Para inicializar modelo miniaturas
import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

// Importa todas las clases necesarias de otros paquetes
import modelo.VisorModel;
import servicios.ConfigurationManager;
import servicios.ProjectManager;
import servicios.image.ThumbnailService;
import vista.VisorView;
import vista.builders.MenuBarBuilder;
import vista.builders.ToolbarBuilder;
import vista.config.MenuItemDefinition;
import vista.config.ToolbarButtonDefinition;
import vista.config.ViewUIConfig;
import vista.theme.Tema;
import vista.theme.ThemeManager;
import vista.util.IconUtils; 

/**
 * Orquesta el proceso de inicialización completo de la aplicación Visor de Imágenes.
 * Separa la lógica de arranque del constructor principal de VisorController.
 */
public class AppInitializer {

    // --- Constante para delay de la redimension de la ventana de la aplicacion
	private static final int WINDOW_RESIZE_UPDATE_DELAY_MS = 250;
    
	// --- Referencias a Componentes Creados/Configurados ---
	
    private ConfigurationManager configuration;
    private VisorModel model;
    private ThemeManager themeManager;
    private IconUtils iconUtils;
    private ThumbnailService servicioMiniaturas;
    private DefaultListModel<String> modeloMiniaturas; // Modelo local para miniaturas
    private ViewUIConfig uiConfig; // Configuración para la vista
    private VisorView view;
    private ListCoordinator listCoordinator;
    private ProjectManager projectManagerService;
    
    private Map<String, Action> actionMap; // Mapa parcial y luego completo
    private List<MenuItemDefinition> menuStructure;
    private List<ToolbarButtonDefinition> toolbarStructure;

    // --- Referencia al Controlador Principal ---
    private final VisorController controller; // El controller que usa este inicializador
    
    
    
    /**
     * Constructor de AppInitializer.
     * @param controller La instancia de VisorController que será inicializada.
     */
    public AppInitializer(VisorController controller) {
        this.controller = Objects.requireNonNull(controller, "VisorController no puede ser null en AppInitializer");
    }

    
    /**
     * Ejecuta todas las fases de inicialización de la aplicación.
     * @return true si la inicialización fue exitosa, false si ocurrió un error fatal.
     */
 // En AppInitializer.java
    public boolean initialize() {
        System.out.println("--- AppInitializer: Iniciando Inicialización ---");

        try {
            // --- FASE 1: Componentes Base (Modelo, Configuración) ---
            if (!inicializarComponentesBase()) return false;

            // --- FASE 2: Aplicar Configuración Leída al Modelo ---
            aplicarConfiguracionAlModelo();

            // --- FASE 3: Servicios Esenciales (Tema, Iconos, Hilos, Miniaturas) ---
            if (!inicializarServiciosEsenciales()) return false;

            // --- FASE 4A: Inicializar Actions PARCIALES y UIConfig ---
            //    (Actions que NO dependen de ListCoordinator ni de la Vista)
            if (!inicializarActionsParcialesYConfigUI()) return false;
            //    En este punto, controller.actionMap tiene las actions parciales.
            //    Copiamos este mapa parcial a this.actionMap de AppInitializer.
            this.actionMap = controller.getActionMap(); // Obtiene el mapa PARCIAL del controller
            System.out.println("    -> ActionMap PARCIAL copiado a AppInitializer (Tamaño: " + (this.actionMap != null ? this.actionMap.size() : "null") + ")");


            // --- FASE 4B: Generar Definiciones de UI ---
            //    (Esto se hace ANTES de crear la UI en EDT, y puede usar AppActionCommands
            //     que ya están en el actionMap parcial si es necesario, o simplemente
            //     AppActionCommands que se conectarán luego).
            System.out.println("  [Initializer] Generando definiciones de UI...");
            vista.config.UIDefinitionService uiDefinitionService = new vista.config.UIDefinitionService();
            this.menuStructure = uiDefinitionService.generateMenuStructure();
            this.toolbarStructure = uiDefinitionService.generateToolbarStructure();
            System.out.println("    -> Definiciones de UI generadas (Menú: "
                    + (this.menuStructure != null ? this.menuStructure.size() : "null") + " top-level, Toolbar: "
                    + (this.toolbarStructure != null ? this.toolbarStructure.size() : "null") + " botones)");

            // --- FASE 5: Gestor de Proyectos
            controller.setProjectManager(new ProjectManager(this.configuration));
            
            // --- FASE 5: Creación de UI y Finalización (en EDT) ---
            SwingUtilities.invokeLater(this::crearUICompletarInicializacion);

            System.out.println("--- AppInitializer: Inicialización base completada. UI se creará en EDT. ---");
            return true;

        } catch (Exception e) {
            manejarErrorFatalInicializacion("Error fatal durante inicialización base", e);
            return false;
        }
    }    
    
//    public boolean initialize() {
//        System.out.println("--- AppInitializer: Iniciando Inicialización ---");
//
//        try {
//            // --- FASE 1: Componentes Base (Modelo, Configuración) ---
//            if (!inicializarComponentesBase()) return false;
//
//            // --- FASE 2: Aplicar Configuración Leída al Modelo ---
//            aplicarConfiguracionAlModelo();
//
//            // --- FASE 3: Servicios Esenciales (Tema, Iconos, Hilos, Miniaturas) ---
//            if (!inicializarServiciosEsenciales()) return false;
//
//            // --- FASE 4: Actions (Parcial) y Configuración UI ---
//            if (!inicializarActionsParcialesYConfigUI()) return false;
//
//            // --- FASE 5: Creación de UI y Finalización (en EDT) ---
//            // Se usa invokeLater para no bloquear el hilo principal si la creación de UI tarda.
//            // El VisorController quedará listo para usarse DESPUÉS de que este invokeLater termine.
//            
//           	// 5.1. Generar Definiciones de UI
//            System.out.println("  [Initializer Fase 2] Generando definiciones de UI...");
//            UIDefinitionService uiDefinitionService = new vista.config.UIDefinitionService();
//
//            // 5.2. Guardamos las estructuras en campos de AppInitializer para usarlas en crearUICompletarInicializacion
//            this.menuStructure = uiDefinitionService.generateMenuStructure();
//            this.toolbarStructure = uiDefinitionService.generateToolbarStructure();
//            
//            System.out.println("    -> Definiciones de UI generadas (Menú: "
//                    + (this.menuStructure != null ? this.menuStructure.size() : "null") + " top-level, Toolbar: "
//                    + (this.toolbarStructure != null ? this.toolbarStructure.size() : "null") + " botones)");
//            
//            //5.3. 
//            SwingUtilities.invokeLater(this::crearUICompletarInicializacion);
//
//            System.out.println("--- AppInitializer: Inicialización base completada. UI se creará en EDT. ---");
//            return true; // Indica éxito de la inicialización base (UI pendiente)
//
//        } catch (Exception e) {
//            // Captura errores inesperados durante las fases iniciales (fuera del EDT)
//            manejarErrorFatalInicializacion("Error fatal durante inicialización base", e);
//            return false; // Indica fallo
//        }
//    } // --- FIN metodo initialize

    
    // --- Métodos Helper Privados para Cada Fase ---

    
    /**
     * Fase 1: Inicializa los componentes fundamentales (Modelo y Configuración).
     * Inyecta estas dependencias en el VisorController.
     * @return true si éxito, false si falla.
     */
    private boolean inicializarComponentesBase() {
        System.out.println("  [Initializer Fase 1] Inicializando Componentes Base...");
        try {
            // 1.1. Crear el Modelo
            model = new VisorModel();
            controller.setModel(model); // Inyectar en Controller
            System.out.println("    -> Modelo OK.");

            // 1.2. Crear y cargar ConfigurationManager
            configuration = new ConfigurationManager();
            controller.setConfigurationManager(configuration); // Inyectar en Controller
            System.out.println("    -> ConfigurationManager OK.");

            return true; // Éxito
        } catch (IOException e) {
            manejarErrorFatalInicializacion("Error inicializando ConfigurationManager", e);
            return false; // Fallo
        } catch (Exception e) {
             manejarErrorFatalInicializacion("Error inesperado inicializando componentes base", e);
             return false; // Fallo
        }
    }

    /**
     * Fase 2: Lee valores de la configuración y los establece en el Modelo.
     * Esto asegura que el modelo tenga los datos correctos ANTES de crear la Vista.
     */
    private void aplicarConfiguracionAlModelo() {
        System.out.println("  [Initializer Fase 2] Aplicando Config al Modelo...");
        if (configuration == null || model == null) {
            System.err.println("ERROR [aplicarConfigAlModelo]: Configuración o Modelo nulos. Saltando fase.");
            return;
        }
        try {
            // 2.1. Leer y aplicar configuración de miniaturas
            model.setMiniaturasAntes(configuration.getInt("miniaturas.cantidad.antes", 7));
            model.setMiniaturasDespues(configuration.getInt("miniaturas.cantidad.despues", 7));
            model.setMiniaturaSelAncho(configuration.getInt("miniaturas.tamano.seleccionada.ancho", 60));
            model.setMiniaturaSelAlto(configuration.getInt("miniaturas.tamano.seleccionada.alto", 60));
            model.setMiniaturaNormAncho(configuration.getInt("miniaturas.tamano.normal.ancho", 40));
            model.setMiniaturaNormAlto(configuration.getInt("miniaturas.tamano.normal.alto", 40));

            // 2.2. Leer y aplicar configuración de comportamiento
            boolean cargarSubcarpetas = configuration.getBoolean("comportamiento.carpeta.cargarSubcarpetas", true);
            model.setMostrarSoloCarpetaActual(!cargarSubcarpetas); // Ojo a la lógica inversa
            boolean mantenerProp = configuration.getBoolean("interfaz.menu.zoom.Mantener_Proporciones.seleccionado", true);
            model.setMantenerProporcion(mantenerProp);

            // 2.3. Leer y aplicar otras configuraciones relevantes para el modelo si las hubiera
            // ...

            System.out.println("    -> Config Modelo OK (Antes=" + model.getMiniaturasAntes() + ", Despues=" + model.getMiniaturasDespues() + ")");
        } catch (Exception e) {
            // Loguear error pero intentar continuar si es posible
            System.err.println("ERROR aplicando config al Modelo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Fase 3: Inicializa servicios como ThemeManager, IconUtils, ThumbnailService y ExecutorService.
     * Inyecta estas dependencias en el VisorController.
     * @return true si éxito, false si falla.
     */
    private boolean inicializarServiciosEsenciales() {
         System.out.println("  [Initializer Fase 3] Inicializando Servicios Esenciales...");
         try {
             // 3.1. Theme Manager (depende de configuration)
             themeManager = new ThemeManager(configuration);
             controller.setThemeManager(themeManager);
             System.out.println("    -> ThemeManager OK.");

             // 3.2. Icon Utils (depende de themeManager)
             iconUtils = new IconUtils(themeManager);
             controller.setIconUtils(iconUtils);
             System.out.println("    -> IconUtils OK.");

             // 3.3. Thumbnail Service
             servicioMiniaturas = new ThumbnailService();
             controller.setServicioMiniaturas(servicioMiniaturas);
             // Crear el modelo para la lista deslizante (instancia del controller)
             this.modeloMiniaturas = new DefaultListModel<>();
             controller.setModeloMiniaturas(this.modeloMiniaturas); // Inyectar modelo
             System.out.println("    -> ThumbnailService y ModeloMiniaturas OK.");

             // 3.4. Executor Service
             controller.setExecutorService(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
             System.out.println("    -> ExecutorService OK.");

             return true; // Éxito
         } catch (Exception e) {
             manejarErrorFatalInicializacion("Error inicializando servicios esenciales", e);
             return false; // Fallo
         }
    }

    /**
     * Fase 4: Inicializa las Actions (excepto navegación), crea el mapa de acciones
     * y prepara el objeto ViewUIConfig.
     * Inyecta dependencias en el VisorController.
     * @return true si éxito, false si falla.
     */
    private boolean inicializarActionsParcialesYConfigUI() {
        System.out.println("  [Initializer Fase 4] Inicializando Actions Parciales y UIConfig...");
         try {
             // 4.1. Obtener dimensiones de iconos desde config
             int anchoIcono = configuration.getInt("iconos.ancho", 24);
             int altoIcono = configuration.getInt("iconos.alto", 24);

             // 4.2. Llamar al método del controller para inicializar actions (sin navegación)
             //      Este método ahora podría ser package-private o public en VisorController.
             controller.initializeActionsInternal(anchoIcono, altoIcono);
             System.out.println("    -> Actions parciales inicializadas.");

             // 4.3. Llamar al método del controller para crear el mapa de acciones (parcial)
             //      Este método ahora podría ser package-private o public en VisorController.
             actionMap = controller.createActionMapInternal(); // Obtiene el mapa creado por el controller
             controller.setActionMap(this.actionMap); // Inyectar mapa parcial en controller
             System.out.println("    -> Mapa de Actions parcial creado.");

             // 4.4. Calcular altura del panel de miniaturas (leyendo del modelo ahora poblado)
             int selThumbH = model.getMiniaturaSelAlto(); // Leer del modelo
             final int calculatedMiniaturePanelHeight = Math.max(80, selThumbH + 40);
             controller.setCalculatedMiniaturePanelHeight(calculatedMiniaturePanelHeight); // Guardar en controller
             System.out.println("    -> Altura Panel Miniaturas calculada: " + calculatedMiniaturePanelHeight);

             // 4.5. Crear el objeto ViewUIConfig
             Tema tema = themeManager.getTemaActual();
             uiConfig = new ViewUIConfig(
                tema.colorFondoPrincipal(), tema.colorBotonFondoActivado(), tema.colorBotonFondoAnimacion(), // Params obsoletos?
                anchoIcono,
                altoIcono,
                actionMap, // Pasar mapa parcial
                iconUtils,
                tema.colorFondoPrincipal(),
                tema.colorFondoSecundario(),
                tema.colorTextoPrimario(),
                tema.colorTextoSecundario(),
                tema.colorBorde(),
                tema.colorBordeTitulo(),
                tema.colorSeleccionFondo(),
                tema.colorSeleccionTexto(),
                tema.colorBotonFondo(),
                tema.colorBotonTexto(),
                tema.colorBotonFondoActivado(), // Sobrescribe param obsoleto 2
                tema.colorBotonFondoAnimacion(), // Sobrescribe param obsoleto 3
                configuration // Pasar ConfigurationManager
             );
             controller.setUiConfigForView(uiConfig); // Guardar en controller para pasar a la vista
             System.out.println("    -> ViewUIConfig creada.");

             return true; // Éxito
         } catch (Exception e) {
              manejarErrorFatalInicializacion("Error inicializando Actions/UIConfig", e);
              return false; // Fallo
         }
    }

    /**
     * Fase 5: Se ejecuta en el EDT. Crea la VisorView, el ListCoordinator,
     * las Actions de navegación restantes, y finaliza la configuración de la UI
     * y los listeners.
     */
    private void crearUICompletarInicializacion() {
    	
        System.out.println("  [Initializer Fase 5 - EDT] Creando UI y finalizando...");

        // --- 0. Project Manager --- 
        try {
            ProjectManager pm = new ProjectManager(this.configuration);
            controller.setProjectManager(pm); // Inyectar en el controller
            System.out.println("    -> [EDT] ProjectManager (Placeholder) instanciado e inyectado.");
        } catch (Exception e) {
            manejarErrorFatalInicializacion("[EDT] Error creando ProjectManager Placeholder", e);
            return;
        }
        
        // --- 1. Obtener Dependencias Iniciales ---
        // (Estas se obtienen de los campos de AppInitializer o del Controller,
        //  y ya fueron configuradas en las fases 1-4 del método initialize())
        ViewUIConfig configParaVista = controller.getUiConfigForView();
        int panelHeight = controller.getCalculatedMiniaturePanelHeight();
        ThumbnailService thumbs = controller.getServicioMiniaturas();
        IconUtils icons = controller.getIconUtils();
        // El campo 'this.model' (de AppInitializer) ya está listo.
        // El campo 'this.actionMap' (de AppInitializer) tiene el mapa PARCIAL en este punto.
        // El campo 'this.menuStructure' y 'this.toolbarStructure' ya están generados.

        // --- 2. Validar Dependencias Críticas ---
        if (this.model == null || thumbs == null || configParaVista == null || icons == null || controller == null || this.actionMap == null) {
             manejarErrorFatalInicializacion("[EDT] Dependencias críticas nulas antes de proceder.", null);
             return;
        }

        // --- 3. Crear VisorView (Inicialmente con Menú/Toolbar temporales/vacíos y mapas vacíos) ---
        //     Esto es necesario para poder pasar la referencia 'view' al ListCoordinator.
        System.out.println("    -> [EDT] Instanciando VisorView (con componentes UI temporales)...");
        JMenuBar initialMenuBar = new JMenuBar(); // Menú temporal vacío
        JPanel initialToolbarPanel = new JPanel(); // Toolbar temporal vacía
        Map<String, JMenuItem> initialMenuItemsMap = Collections.emptyMap();
        Map<String, JButton> initialBotonesMap = Collections.emptyMap();
        try {
             view = new VisorView(
                 panelHeight, configParaVista, this.model, thumbs,
                 initialMenuBar, initialToolbarPanel, initialMenuItemsMap, initialBotonesMap
             );
             controller.setView(view); // Inyectar la vista en el controller
             System.out.println("    -> [EDT] VisorView instanciado.");
         } catch (Exception e) {
              manejarErrorFatalInicializacion("[EDT] Error creando VisorView", e);
              return;
         }

        // --- 4. Crear ListCoordinator (AHORA que 'view' existe y ha sido asignada al controller) ---
        try {
            listCoordinator = new ListCoordinator(this.model, view, controller);
            controller.setListCoordinator(listCoordinator);
            // Ya no es necesario listCoordinator.setView(view); si el constructor lo maneja
            System.out.println("    -> [EDT] ListCoordinator instanciado.");
        } catch (Exception e) {
            manejarErrorFatalInicializacion("[EDT] Error creando ListCoordinator", e);
            return;
        }

        // --- 5. Completar el ActionMap con Actions de Navegación ---
        //     (Esto actualiza el actionMap DENTRO del controller)
        System.out.println("    -> [EDT] Completando ActionMap con acciones de navegación...");
        try {
            int anchoIconoNav = configParaVista.iconoAncho;
            int altoIconoNav = configParaVista.iconoAlto;
            controller.createNavigationActionsAndUpdateMap(anchoIconoNav, altoIconoNav); // Usa ListCoordinator
            // Actualizamos la referencia en AppInitializer para tener el mapa COMPLETO
            this.actionMap = controller.getActionMap();
            System.out.println("    -> [EDT] ActionMap en AppInitializer ahora es COMPLETO (Tamaño: " + this.actionMap.size() + ")");
        } catch (Exception e) {
             System.err.println("[EDT] ERROR creando/completando Actions de Navegación: " + e.getMessage());
             e.printStackTrace();
        }

        // --- 6. Construcción REAL de Menú y Toolbar con Builders Refactorizados ---
        //     (Usan this.actionMap que ahora está COMPLETO, y this.menuStructure/toolbarStructure)
        System.out.println("    -> [EDT] Construyendo Menú y Toolbar REALES (métodos refactorizados)...");
        JMenuBar finalMenuBar = null;
        JPanel finalToolbarPanel = null;
        Map<String, JMenuItem> finalMenuItemsMap = Collections.emptyMap();
        Map<String, JButton> finalBotonesMap = Collections.emptyMap();

        if (this.menuStructure == null || this.toolbarStructure == null || this.actionMap == null) {
            manejarErrorFatalInicializacion("[EDT] Estructuras de UI o ActionMap COMPLETO no disponibles para builders.", null);
            return;
        }
        
        try {
             MenuBarBuilder menuBuilder = new MenuBarBuilder();
             finalMenuBar = menuBuilder.buildMenuBar(this.menuStructure, this.actionMap);
             finalMenuItemsMap = menuBuilder.getMenuItemsMap();
             System.out.println("      -> Menú REAL construido (refactorizado).");
        } catch (Exception e) {
             System.err.println("      -> ERROR [EDT] creando Menú REAL: " + e.getMessage()); e.printStackTrace();
             finalMenuBar = new JMenuBar(); finalMenuItemsMap = Collections.emptyMap(); // Fallback
        }

        try {
             ToolbarBuilder toolbarBuilder = new ToolbarBuilder(
                 this.actionMap, configParaVista.colorBotonFondo, configParaVista.colorBotonTexto,
                 configParaVista.colorBotonActivado, configParaVista.colorBotonAnimacion,
                 configParaVista.iconoAncho, configParaVista.iconoAlto,
                 configParaVista.iconUtils, this.controller
             );
             finalToolbarPanel = toolbarBuilder.buildToolbar(this.toolbarStructure, this.actionMap);
             finalBotonesMap = toolbarBuilder.getBotonesPorNombre();
             System.out.println("      -> Toolbar REAL construida (refactorizado).");
        } catch (Exception e) {
              System.err.println("      -> ERROR [EDT] creando Toolbar REAL: " + e.getMessage()); e.printStackTrace();
              finalToolbarPanel = new JPanel(); finalBotonesMap = Collections.emptyMap(); // Fallback
        }
        System.out.println("    -> [EDT] Fin construcción Menú/Toolbar REALES.");

        // --- 7. ACTUALIZAR la UI de VisorView con el Menú y Toolbar REALES ---
        if (view != null) {
            view.setActualJMenuBar(finalMenuBar); 			
            view.setActualToolbarPanel(finalToolbarPanel); 
            view.setActualMenuItemsMap(finalMenuItemsMap); 
            view.setActualBotonesMap(finalBotonesMap);     
            System.out.println("    -> [EDT] Menú y Toolbar reales asignados a VisorView.");
        }

        // --- 8. Resto de la Inicialización (Llamadas a métodos del Controller) ---
        try { controller.assignModeloMiniaturasToViewInternal(); } catch (Exception e) { System.err.println("[EDT] ERROR assignModeloMiniaturas: " + e.getMessage()); e.printStackTrace(); }
        try { controller.establecerCarpetaRaizDesdeConfigInternal(); } catch (Exception e) { System.err.println("[EDT] ERROR establecerCarpetaRaiz: " + e.getMessage()); e.printStackTrace(); }
        try { controller.configurarComponentActionsInternal(); } catch (Exception e) { System.err.println("[EDT] ERROR configurarComponentActions: " + e.getMessage()); e.printStackTrace(); }
        try { controller.aplicarConfigAlaVistaInternal(); } catch (Exception e) { System.err.println("[EDT] ERROR aplicarConfigAlaVista: " + e.getMessage()); e.printStackTrace(); }
        try { controller.cargarEstadoInicialInternal(); } catch (Exception e) { System.err.println("[EDT] ERROR cargarEstadoInicial: " + e.getMessage()); e.printStackTrace(); }
        try { controller.configurarListenersVistaInternal(); } catch (Exception e) { System.err.println("[EDT] ERROR configurarListenersVista: " + e.getMessage()); e.printStackTrace(); }
        try { controller.interceptarAccionesTecladoListas(); } catch (Exception e) { System.err.println("[EDT] ERROR interceptarAccionesTecladoListas: " + e.getMessage()); e.printStackTrace(); }
        try { KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(controller); } catch (Exception e) { System.err.println("[EDT] ERROR registrando KeyEventDispatcher: " + e.getMessage()); e.printStackTrace(); }
        
        // --- 9. CONFIGURAR LISTENER DE REDIMENSIONAMIENTO DE VENTANA ---
        //     Se hace después de que la UI básica y los listeners principales estén listos,
        //     pero antes de la sincronización final de UI y el hook de cierre.
        try {
            configurarListenerRedimensionVentana(); 
        } catch (Exception e) {
            System.err.println("[EDT] ERROR configurando listener de redimensionamiento: " + e.getMessage());
            e.printStackTrace();
            // No necesariamente fatal, la app podría funcionar sin miniaturas dinámicas.
        }
        
        // --- 10. Sincronización Final y Hook de Cierre 
        try { controller.sincronizarUIFinalInternal(); } catch (Exception e) { System.err.println("[EDT] ERROR sincronizarUIFinal: " + e.getMessage()); e.printStackTrace(); }
        try { controller.configurarShutdownHookInternal(); } catch (Exception e) { System.err.println("[EDT] ERROR configurarShutdownHook: " + e.getMessage()); e.printStackTrace(); }

        System.out.println("  [Initializer Fase 5 - EDT] Inicialización UI completada.");
        System.out.println("--- AppInitializer: INICIALIZACIÓN COMPLETA ---");

    } //--- FIn metodo crearUICompletarInicializacion
    
    
    /**
     * (NUEVO MÉTODO PRIVADO)
     * Configura un ComponentListener en la ventana principal (VisorView)
     * para detectar cambios de tamaño y recalcular dinámicamente la cantidad
     * de miniaturas visibles en la barra de miniaturas.
     * Utiliza un Timer para "debouncing" y evitar recálculos excesivos.
     */
    private void configurarListenerRedimensionVentana() {
        if (view != null && controller != null && listCoordinator != null) { // Asegúrate que todos existan
            System.out.println("    -> [EDT - AppInitializer] Configurando ComponentListener para miniaturas dinámicas...");

            view.getFrame().addComponentListener(new java.awt.event.ComponentAdapter() {
                private Timer resizeTimer;

                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    if (resizeTimer != null && resizeTimer.isRunning()) {
                        resizeTimer.restart();
                    } else {
                        resizeTimer = new Timer(WINDOW_RESIZE_UPDATE_DELAY_MS, ae -> {
                            System.out.println("      [WindowResized Timer - AppInitializer] Ejecutando recálculo de miniaturas...");

                            // Validar que el modelo y el listCoordinator estén listos
                            // y que haya una selección válida (índice != -1)
                            if (controller.getModel() != null && listCoordinator != null && listCoordinator.getIndiceOficialSeleccionado() != -1) {
                                int indiceActual = listCoordinator.getIndiceOficialSeleccionado(); 
                                controller.actualizarModeloYVistaMiniaturas(indiceActual);
                            } else {
                                System.out.println("      [WindowResized Timer - AppInitializer] Modelo no listo, ListCoordinator no listo o sin selección, no se actualizan miniaturas.");
                            }
                        });
                        resizeTimer.setRepeats(false);
                        resizeTimer.start();
                    }
                }
            });
            System.out.println("    -> [EDT - AppInitializer] ComponentListener para miniaturas dinámicas añadido a VisorView.");
        } else {
            System.err.println("ERROR [AppInit EDT - configurarListenerRedimensionVentana]: No se pudo añadir ComponentListener (vista, controller o listCoordinator nulos).");
        }
    }
    
    
    /**
     * Manejador de errores fatales ocurridos durante la inicialización.
     * Muestra un mensaje al usuario y termina la aplicación.
     * @param message Mensaje descriptivo del error.
     * @param cause La excepción original (puede ser null).
     */
    private void manejarErrorFatalInicializacion(String message, Throwable cause) {
        System.err.println("FATAL INITIALIZER: " + message);
        if (cause != null) {
            cause.printStackTrace(); // Imprimir stack trace para depuración
        }
        // Asegurarse de mostrar el mensaje en el EDT
        final String finalMessage = message;
        final Throwable finalCause = cause;
        SwingUtilities.invokeLater(() -> {
             JOptionPane.showMessageDialog(null,
                 finalMessage + (finalCause != null ? "\n\nError: " + finalCause.getMessage() : ""),
                 "Error Fatal de Inicialización",
                 JOptionPane.ERROR_MESSAGE);
        });

        // Terminar la aplicación de forma abrupta si la inicialización falla
        // Considera alternativas si quieres intentar una recuperación parcial.
        System.err.println("Terminando aplicación debido a error fatal de inicialización.");
        System.exit(1);

        // O lanzar una RuntimeException si prefieres que el hilo principal la capture
        // throw new RuntimeException("Fallo en la inicialización: " + message, cause);
    }
    

}