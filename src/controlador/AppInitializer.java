package controlador; // O controlador.initialization

import java.awt.KeyboardFocusManager;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors; // Para crear ExecutorService

import javax.swing.Action; // Para Map<String, Action>
import javax.swing.DefaultListModel; // Para inicializar modelo miniaturas
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

// Importa todas las clases necesarias de otros paquetes
import modelo.VisorModel;
import servicios.ConfigurationManager;
import servicios.image.ThumbnailService;
import vista.VisorView;
import vista.config.ViewUIConfig;
import vista.theme.Tema;
import vista.theme.ThemeManager;
import vista.util.IconUtils;

/**
 * Orquesta el proceso de inicialización completo de la aplicación Visor de Imágenes.
 * Separa la lógica de arranque del constructor principal de VisorController.
 */
public class AppInitializer {

    // --- Referencias a Componentes Creados/Configurados ---
    private ConfigurationManager configuration;
    private VisorModel model;
    private ThemeManager themeManager;
    private IconUtils iconUtils;
    private ThumbnailService servicioMiniaturas;
    private DefaultListModel<String> modeloMiniaturas; // Modelo local para miniaturas
    private Map<String, Action> actionMap; // Mapa parcial y luego completo
    private ViewUIConfig uiConfig; // Configuración para la vista
    private VisorView view;
    private ListCoordinator listCoordinator;

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
    public boolean initialize() {
        System.out.println("--- AppInitializer: Iniciando Inicialización ---");

        try {
            // --- FASE 1: Componentes Base (Modelo, Configuración) ---
            if (!inicializarComponentesBase()) return false;

            // --- FASE 2: Aplicar Configuración Leída al Modelo ---
            aplicarConfiguracionAlModelo();

            // --- FASE 3: Servicios Esenciales (Tema, Iconos, Hilos, Miniaturas) ---
            if (!inicializarServiciosEsenciales()) return false;

            // --- FASE 4: Actions (Parcial) y Configuración UI ---
            if (!inicializarActionsParcialesYConfigUI()) return false;

            // --- FASE 5: Creación de UI y Finalización (en EDT) ---
            // Se usa invokeLater para no bloquear el hilo principal si la creación de UI tarda.
            // El VisorController quedará listo para usarse DESPUÉS de que este invokeLater termine.
            SwingUtilities.invokeLater(this::crearUICompletarInicializacion);

            System.out.println("--- AppInitializer: Inicialización base completada. UI se creará en EDT. ---");
            return true; // Indica éxito de la inicialización base (UI pendiente)

        } catch (Exception e) {
            // Captura errores inesperados durante las fases iniciales (fuera del EDT)
            manejarErrorFatalInicializacion("Error fatal durante inicialización base", e);
            return false; // Indica fallo
        }
    }

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
             controller.setActionMap(actionMap); // Inyectar mapa parcial en controller
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

         // 5.1. Obtener dependencias necesarias desde el VisorController
         //      (ya fueron inyectadas en fases anteriores)
         ViewUIConfig configParaVista = controller.getUiConfigForView();
         int panelHeight = controller.getCalculatedMiniaturePanelHeight();
         ThumbnailService thumbs = controller.getServicioMiniaturas();
         IconUtils icons = controller.getIconUtils(); // Necesario para actions de nav
         int anchoIcono = configuration.getInt("iconos.ancho", 24); // Releer o pasar desde Fase 4
         int altoIcono = configuration.getInt("iconos.alto", 24);

         // 5.2. Re-validar dependencias críticas dentro del EDT
         if (model == null || thumbs == null || configParaVista == null || icons == null || controller == null) {
              manejarErrorFatalInicializacion("[EDT] Dependencias nulas antes de crear Vista/Coordinator", null);
              return; // Salir si falla
         }

         // 5.3. Crear la instancia de VisorView
         try {
             view = new VisorView(panelHeight, configParaVista, model, thumbs);
             controller.setView(view); // Inyectar la vista en el controller
             
//             LOG [EDT] CONTENIDO REAL del mapa menuItemsPorNombre:
//             -> KEY: 	'interfaz.menu.imagen.Abrir_Ubicacion_del_Archivo' -> ITEM: 'Abrir Ubicacion del Archivo'
//             			'interfaz.menu.archivo.Abrir_Ubicacion_del_Archivo'
             
             System.out.println("    -> [EDT] CONTENIDO REAL del mapa menuItemsPorNombre:");
             if (view != null && view.getMenuItemsPorNombre() != null) {
                 view.getMenuItemsPorNombre().forEach((key, item) -> {
                     // Imprimir clave y el texto del item para identificarlo fácil
                     String itemText = (item != null) ? item.getText() : "NULL_ITEM";
                     System.out.println("      -> KEY: '" + key + "' -> ITEM: '" + itemText + "'");
                 });
             } else {
                  System.out.println("      -> Vista o mapa de menús es null.");
             }
             System.out.println("    -> [EDT] FIN CONTENIDO MAPA");
//             FIN DEL Log
             
             System.out.println("    -> [EDT] VisorView instanciado.");
         } catch (Exception e) {
              manejarErrorFatalInicializacion("[EDT] Error creando VisorView", e);
              return;
         }

         // 5.4. Crear la instancia de ListCoordinator
         try {
             listCoordinator = new ListCoordinator(model, view, controller);
             controller.setListCoordinator(listCoordinator); // Inyectar en el controller
             System.out.println("    -> [EDT] ListCoordinator instanciado.");
         } catch (Exception e) {
              manejarErrorFatalInicializacion("[EDT] Error creando ListCoordinator", e);
              return;
         }

         // 5.5. Crear Actions de Navegación y actualizar mapa
         try {
             controller.createNavigationActionsAndUpdateMap(anchoIcono, altoIcono); // Llama a método refactorizado
             System.out.println("    -> [EDT] Actions Navegación creadas y mapa actualizado.");
         } catch (Exception e) {
              System.err.println("[EDT] ERROR creando Actions de Navegación: " + e.getMessage());
              e.printStackTrace(); // Continuar pero loguear error
         }

         // 5.6. Asignar modelo de miniaturas a la JList correspondiente en la vista
         try {
            controller.assignModeloMiniaturasToViewInternal(); // Llama a método refactorizado
            System.out.println("    -> [EDT] Modelo Miniaturas asignado a la Vista.");
         } catch (Exception e) {
              System.err.println("[EDT] ERROR asignando modelo de miniaturas: " + e.getMessage());
              e.printStackTrace();
         }

         // 5.7. Establecer la carpeta raíz inicial desde la configuración
         try {
             controller.establecerCarpetaRaizDesdeConfigInternal(); // Llama a método refactorizado
             System.out.println("    -> [EDT] Carpeta raíz inicial establecida.");
         } catch (Exception e) {
              System.err.println("[EDT] ERROR estableciendo carpeta raíz: " + e.getMessage());
              e.printStackTrace();
         }

         // 5.8. Aplicar configuración inicial a los componentes de la VISTA
         try {
             controller.aplicarConfigAlaVistaInternal(); // Llama a método refactorizado
             System.out.println("    -> [EDT] Configuración aplicada a los componentes de la Vista.");
         } catch (Exception e) {
              System.err.println("[EDT] ERROR aplicando configuración a la Vista: " + e.getMessage());
              e.printStackTrace();
         }

         // 5.9. Cargar el estado inicial (lista de imágenes y selección inicial)
         try {
             controller.cargarEstadoInicialInternal(); // Llama a método refactorizado
             System.out.println("    -> [EDT] Carga del estado inicial iniciada.");
         } catch (Exception e) {
              System.err.println("[EDT] ERROR iniciando carga de estado inicial: " + e.getMessage());
              e.printStackTrace();
         }

         // 5.10. Configurar los listeners de la vista
         try {
             controller.configurarListenersVistaInternal(); // Llama a método refactorizado
             System.out.println("    -> [EDT] Listeners de la Vista configurados.");
         } catch (Exception e) {
              System.err.println("[EDT] ERROR configurando listeners: " + e.getMessage());
              e.printStackTrace();
         }
         
         // 5.10.bis. Interceptar acciones de teclado de las listas
         try {
             controller.interceptarAccionesTecladoListas(); // Configura los bindings restantes
             System.out.println("    -> [EDT] Acciones de teclado de listas interceptadas.");
         } catch (Exception e) {
              System.err.println("[EDT] ERROR interceptando acciones teclado listas: " + e.getMessage());
              e.printStackTrace();
         }

         // 5.10.ter. Registrar el KeyEventDispatcher (DEBE ir DESPUÉS de que la UI esté creada)
         try {
             KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(controller);
             System.out.println("    -> [EDT] KeyEventDispatcher registrado.");
         } catch (Exception e) {
              System.err.println("[EDT] ERROR registrando KeyEventDispatcher: " + e.getMessage());
              e.printStackTrace();
         }

         // 5.11. Configurar las Actions en los componentes de la UI (botones, menús)
         try {
             controller.configurarComponentActionsInternal(); // Llama a método refactorizado
             System.out.println("    -> [EDT] Actions asignadas a Componentes UI.");
         } catch (Exception e) {
              System.err.println("[EDT] ERROR configurando Actions en componentes: " + e.getMessage());
              e.printStackTrace();
         }

         // 5.12. Sincronizar el estado visual final (ej. fondo a cuadros)
         try {
             controller.sincronizarUIFinalInternal(); // Llama a método refactorizado
             System.out.println("    -> [EDT] UI Final sincronizada.");
         } catch (Exception e) {
              System.err.println("[EDT] ERROR sincronizando UI final: " + e.getMessage());
              e.printStackTrace();
         }

         // 5.13. Configurar el Hook de Cierre
         try {
             controller.configurarShutdownHookInternal(); // Llama a método refactorizado
             System.out.println("    -> [EDT] Shutdown Hook configurado.");
         } catch (Exception e) {
              System.err.println("[EDT] ERROR configurando Shutdown Hook: " + e.getMessage());
              e.printStackTrace();
         }

         // 5.14. Log final
         System.out.println("  [Initializer Fase 5 - EDT] Inicialización UI completada.");
         System.out.println("--- AppInitializer: INICIALIZACIÓN COMPLETA ---");
         // A partir de aquí, la aplicación está lista y la ventana principal (view) es visible.
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