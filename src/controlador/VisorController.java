package controlador;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.actions.tema.ToggleThemeAction;
import controlador.commands.AppActionCommands;
import controlador.factory.ActionFactory;
import controlador.interfaces.IModoController;
import controlador.managers.CarouselManager;
import controlador.managers.ConfigApplicationManager;
import controlador.managers.DisplayModeManager;
import controlador.managers.ImageListManager;
import controlador.managers.InfobarImageManager;
import controlador.managers.InfobarStatusManager;
import controlador.managers.ToolbarManager;
import controlador.managers.ViewManager;
import controlador.managers.interfaces.IListCoordinator;
import controlador.managers.interfaces.IViewManager;
import controlador.managers.interfaces.IZoomManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.ProjectManager;
import servicios.image.ThumbnailService;
import vista.VisorView;
import vista.config.ViewUIConfig;
import vista.panels.GridDisplayPanel;
import vista.panels.ImageDisplayPanel;
import vista.renderers.MiniaturaListCellRenderer;
import vista.theme.Tema;
import vista.theme.ThemeChangeListener;
import vista.theme.ThemeManager;
import vista.util.IconUtils;


/**
 * Controlador principal para el Visor de Imágenes (Versión con 2 JList sincronizadas).
 * Orquesta la interacción entre Modelo y Vista, maneja acciones y lógica de negocio.
 */
public class VisorController implements IModoController, ThemeChangeListener {
	
	private static final Logger logger = LoggerFactory.getLogger(VisorController.class);

    // --- 1. Referencias a Componentes del Sistema ---

	public VisorModel model;						// El modelo de datos principal de la aplicación
    public VisorView view;							// Clase principal de la Interfaz Grafica
    
    private ViewManager viewManager;
    private ConfigurationManager configuration;		// Gestor del archivo de configuracion
    private IconUtils iconUtils;					// utilidad para cargar y gestionar iconos de la aplicación
    private ThemeManager themeManager;				// Gestor de tema visual de la interfaz
    private ThumbnailService servicioMiniaturas;	// Servicio para gestionar las miniaturas
    private ProjectManager projectManager;			// Gestor de proyectos (imagenes favoritas)
    private ComponentRegistry registry;
    private InfobarImageManager infobarImageManager; 
    private InfobarStatusManager statusBarManager;
    private ToolbarManager toolbarManager;
    private ActionFactory actionFactory;
    private GeneralController generalController;
    private ImageListManager imageListManager;
    
    private IListCoordinator listCoordinator;		// El coordinador para la selección y navegación en las listas
    private IZoomManager zoomManager;				// Responsable de los metodos de zoom

    private controlador.managers.DisplayModeManager displayModeManager;
    
    // --- Comunicación con AppInitializer ---
    private ViewUIConfig uiConfigForView;			// Necesario para el renderer (para colores y config de thumbWidth/Height)
    private int calculatedMiniaturePanelHeight;		
    private String version;
    private ExecutorService executorService;		 
    
    // --- 2. Estado Interno del Controlador ---
    private Future<?> cargaImagenPrincipalFuture;
    
    private DefaultListModel<String> modeloMiniaturasVisualizador;
    private DefaultListModel<String> modeloMiniaturasCarrusel;
    
    private Map<String, Action> actionMap;

    // Constantes de seguridad de imagenes antes y despues de la seleccionada
    public static final int DEFAULT_MINIATURAS_ANTES_FALLBACK = 8;
    public static final int DEFAULT_MINIATURAS_DESPUES_FALLBACK = 8;
    
    
    private ConfigApplicationManager configAppManager;
    
    private Map<String, AbstractButton> botonesPorNombre;
    
    private Map<String, JMenuItem> menuItemsPorNombre;
    
    private volatile boolean isRebuildingToolbars = false;	// Ayuda al cierre de las toolbar Flotantes
    
    // --- Atributos para Menús Contextuales ---
    private JPopupMenu popupMenuImagenPrincipal;
    private JPopupMenu popupMenuListaNombres;
    private JPopupMenu popupMenuListaMiniaturas;
    private JPopupMenu popupMenuGrid;
    private MouseListener popupListenerImagenPrincipal;
    private MouseListener popupListenerListaNombres;
    private MouseListener popupListenerListaMiniaturas;
    private MouseListener popupListenerGrid;
    
    /**
     * Constructor principal (AHORA SIMPLIFICADO).
     * Delega toda la inicialización a AppInitializer.
     */
    public VisorController(String version) {
        logger.debug("--- Iniciando VisorController (Constructor Simple) ---");
        AppInitializer initializer = new AppInitializer(this, version); // Pasa 'this'
        boolean success = initializer.initialize(); // Llama al método orquestador

        // Manejar fallo de inicialización si ocurre
        if (!success) {
             // AppInitializer ya debería haber mostrado un error y salido,
             // pero podemos añadir un log extra aquí si queremos.
             logger.warn("VisorController: La inicialización falló (ver logs de AppInitializer).");
             // Podríamos lanzar una excepción aquí o simplemente no continuar.
             // Si AppInitializer llama a System.exit(), este código no se alcanzará.
        } else {
             logger.debug("--- VisorController: Inicialización delegada completada con éxito ---");
        }
        
        this.modeloMiniaturasVisualizador = new DefaultListModel<>();
        this.modeloMiniaturasCarrusel = new DefaultListModel<>();
        
    } // --- FIN CONSTRUCTOR ---
    
    
// ----------------------------------- Métodos de Inicialización Interna -----------------------------------------------

// ************************************************************************************************************* ACTIONS
    
    
	/**
	 * Carga la carpeta y la imagen iniciales definidas en la configuración.
	 * Si no hay configuración válida, limpia la interfaz.
	 * Se llama desde AppInitializer (en el EDT) después de aplicar la config inicial a la vista.
	 * Llama a `cargarListaImagenes` para iniciar la carga en segundo plano.
	 */
    void cargarEstadoInicialInternal() {
        logger.debug("  [Load Initial State Internal] Cargando estado inicial...");

        // --- SECCIÓN 1: LÓGICA DE RECUPERACIÓN DE PROYECTO ---
        String ultimoProyecto = configuration.getString(ConfigKeys.PROYECTOS_ULTIMO_PROYECTO_ABIERTO, "");
        boolean debeCargarProyecto = false;
        Path rutaProyectoACargar = null;

        if (!ultimoProyecto.isEmpty()) {
            String nombreProyectoParaDialogo = "el proyecto temporal";
            if (!"TEMPORAL".equalsIgnoreCase(ultimoProyecto)) {
                try {
                    // Comprobamos si el archivo del proyecto con nombre realmente existe.
                    Path proyectoPath = Paths.get(ultimoProyecto);
                    if (Files.exists(proyectoPath)) {
                        nombreProyectoParaDialogo = "'" + proyectoPath.getFileName().toString() + "'";
                        rutaProyectoACargar = proyectoPath;
                    } else {
                        logger.warn("El último proyecto guardado ({}) ya no existe. Se ignorará.", ultimoProyecto);
                        ultimoProyecto = ""; // Reseteamos para que no se intente cargar.
                    }
                } catch (Exception e) {
                    logger.error("La ruta del último proyecto guardado es inválida: {}. Se ignorará.", ultimoProyecto, e);
                    ultimoProyecto = ""; // Reseteamos.
                }
            }
            
            // Solo mostramos el diálogo si, después de las comprobaciones, todavía tenemos un proyecto válido.
            if (!ultimoProyecto.isEmpty()) {
                int respuesta = JOptionPane.showConfirmDialog(
                    null, 
                    "Se ha encontrado " + nombreProyectoParaDialogo + " de la sesión anterior.\n¿Deseas continuar trabajando en él?",
                    "Recuperar Proyecto Anterior",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
        
                if (respuesta == JOptionPane.YES_OPTION) {
                    debeCargarProyecto = true;
                } else {
                    // El usuario dijo NO, así que limpiamos el proyecto temporal para empezar de cero.
                    projectManager.nuevoProyecto();
                }
            }
        }

        // --- SECCIÓN 2: EJECUCIÓN DE LA CARGA ---
        if (debeCargarProyecto) {
            try {
                // Determina qué archivo cargar: el proyecto con nombre o el de recuperación temporal.
                Path archivoARecuperar;
                if (rutaProyectoACargar != null) {
                    archivoARecuperar = rutaProyectoACargar;
                } else {
                    // Si no había un proyecto con nombre, significa que debemos recuperar el temporal.
                    archivoARecuperar = projectManager.getRutaArchivoRecuperacion();
                }

                // Usamos el nuevo método específico para recuperar, que NO resetea el flag de cambios.
                projectManager.cargarDesdeRecuperacion(archivoARecuperar);

                // Si el proyecto que cargamos tiene contenido, cambiamos al modo proyecto.
                if (!projectManager.getImagenesMarcadas().isEmpty() || !projectManager.getImagenesDescartadas().isEmpty()) {

                	SwingUtilities.invokeLater(() -> {
        
                        // Actualizamos el título inmediatamente para reflejar el proyecto cargado.
                        this.generalController.actualizarTituloVentana();
                        // Cambiamos al modo proyecto para empezar a trabajar.
                        generalController.cambiarModoDeTrabajo(VisorModel.WorkMode.PROYECTO);
                    });
                } else {
                     // Si el proyecto estaba vacío (o era el temporal y estaba vacío), cargamos el visor normal.
                     cargarVisorNormal();
                }
            } catch (Exception e) {
                logger.error("Error al intentar cargar el proyecto anterior: {}", e.getMessage());
                JOptionPane.showMessageDialog(null, "No se pudo cargar el proyecto anterior.", "Error de Carga", JOptionPane.ERROR_MESSAGE);
                cargarVisorNormal(); // Fallback a la carga normal del visor.
            }
        } else {
            // Carga normal del visor (si no había proyecto o el usuario dijo NO).
            cargarVisorNormal();
        }
    } // ---FIN de metodo cargarEstadoInicialInternal---
    
    
	/**
     * Configura un AWTEventListener global para detectar cuándo una JToolBar
     * se cierra mientras está flotando. Dispara una recreación y reconstrucción
     * completa de todas las barras de herramientas para asegurar un estado limpio.
     */
	void configurarListenerGlobalDeToolbars() {
        // --- MÉTODO INTENCIONALMENTE VACÍO ---
        // La lógica se manejará con un PropertyChangeListener en ToolbarBuilder.
        logger.debug("  [Controller] configurarListenerGlobalDeToolbars: Lógica deshabilitada en favor de PropertyChangeListener.");
    } // --- Fin del método configurarListenerGlobalDeToolbars ---

	
	/**
	 * Configura los listeners de selección para las JLists del MODO VISUALIZADOR y MODO CARRUSEL.
	 * Este método ahora es responsable de "cablear" la interacción del usuario en ambos modos.
	 */
	void configurarListenersVistaInternal() {
	    if (view == null || listCoordinator == null || model == null || registry == null) {
	        logger.warn("WARN [configurarListenersVistaInternal]: Dependencias críticas nulas. Abortando.");
	        return;
	    }
	    logger.debug("[VisorController Internal] Configurando listeners para listas de VISUALIZADOR y CARRUSEL...");

	    // =========================================================================
        // === INICIO DE LA IMPLEMENTACIÓN DE TU LÓGICA ===
        // =========================================================================
	    
	    // --- 1. LISTENER INTELIGENTE PARA LA LISTA DE NOMBRES ---
	    JList<String> listaNombres = registry.get("list.nombresArchivo");
	    if (listaNombres != null) {
	        // Limpiamos listeners antiguos para evitar duplicados
	        for (javax.swing.event.ListSelectionListener lsl : listaNombres.getListSelectionListeners()) {
	            listaNombres.removeListSelectionListener(lsl);
	        }
	        
	        listaNombres.addListSelectionListener(e -> {
	            if (!e.getValueIsAdjusting()) {
                    // 1. Obtenemos el índice MAESTRO de la acción del usuario en ESTA vista.
                    //    Como esta lista usa el modelo maestro directamente, el índice es el correcto.
	                int selectedMasterIndex = listaNombres.getSelectedIndex();
                    if (selectedMasterIndex == -1) return;

                    // 2. Comprobamos el estado actual del MODELO MAESTRO.
                    int officialMasterIndex = listCoordinator.getOfficialSelectedIndex();

                    // 3. El "no hacemos nada": si la acción del usuario coincide con el estado
                    //    actual del modelo, es un evento de rebote. Lo ignoramos.
                    if (selectedMasterIndex == officialMasterIndex) {
                        return;
                    }

                    // 4. Si llegamos aquí, es una acción genuina. Ordenamos al controlador
                    //    maestro que actualice el estado de toda la aplicación.
	                listCoordinator.seleccionarImagenPorIndice(selectedMasterIndex);
	            }
	        });
	        logger.debug("  -> Listener INTELIGENTE añadido a 'list.nombresArchivo'");
	    }
	    
        // =========================================================================
        // === FIN DE LA IMPLEMENTACIÓN DE TU LÓGICA ===
        // =========================================================================

	    // --- 2. CREACIÓN DE UN LISTENER DE MINIATURAS REUTILIZABLE E INTELIGENTE ---
	    javax.swing.event.ListSelectionListener thumbnailListener = e -> {
	        if (e.getValueIsAdjusting() || listCoordinator.isSincronizandoUI()) {
	            return;
	        }

	        // a) Obtenemos la JList que originó el evento. Puede ser la del visor o la del carrusel.
	        @SuppressWarnings("unchecked")
	        JList<String> sourceList = (JList<String>) e.getSource();
	        String claveSeleccionada = sourceList.getSelectedValue();
	        
	        if (claveSeleccionada == null) return;

	        // b) TRADUCCIÓN: Buscamos el índice REAL de esa clave en el modelo de datos del CONTEXTO ACTUAL.
	        //    model.getCurrentListContext() devolverá el del visualizador o el del carrusel según corresponda.
	        int indiceEnListaPrincipal = model.getCurrentListContext().getModeloLista().indexOf(claveSeleccionada);
	        
	        // c) Si se encuentra, se lo comunicamos al coordinador.
	        if (indiceEnListaPrincipal != -1) {
	            listCoordinator.seleccionarImagenPorIndice(indiceEnListaPrincipal);
	        }
	    };

	    // --- 3. APLICAR EL LISTENER REUTILIZABLE A AMBAS JLISTS DE MINIATURAS ---
	    
	    // a) Aplicar al JList de miniaturas del VISUALIZADOR
	    JList<String> listaMiniaturasVisor = registry.get("list.miniaturas");
	    if (listaMiniaturasVisor != null) {
	        for (javax.swing.event.ListSelectionListener lsl : listaMiniaturasVisor.getListSelectionListeners()) {
	            listaMiniaturasVisor.removeListSelectionListener(lsl);
	        }
	        listaMiniaturasVisor.addListSelectionListener(thumbnailListener);
	        logger.debug("  -> Listener de miniaturas añadido a 'list.miniaturas' (VISOR)");
	    }

	    // b) Aplicar al JList de miniaturas del CARRUSEL
	    JList<String> listaMiniaturasCarrusel = registry.get("list.miniaturas.carousel");
	    if (listaMiniaturasCarrusel != null) {
	        for (javax.swing.event.ListSelectionListener lsl : listaMiniaturasCarrusel.getListSelectionListeners()) {
	            listaMiniaturasCarrusel.removeListSelectionListener(lsl);
	        }
	        listaMiniaturasCarrusel.addListSelectionListener(thumbnailListener);
	        logger.debug("  -> Listener de miniaturas añadido a 'list.miniaturas.carousel' (CARRUSEL)");
	    }

	    configurarListenersDeRedimension();
	    
	    logger.debug("[VisorController Internal] Listeners de listas configurados.");

	} // --- Fin del método configurarListenersVistaInternal ---
	
    
    /**
     * Revalida y repinta el panel que contiene las barras de herramientas.
     * Es útil después de mostrar u ocultar una barra de herramientas individual o un botón.
     */
    public void revalidateToolbarContainer() {
        if (registry == null) {
            logger.error("ERROR [revalidateToolbarContainer]: ComponentRegistry es nulo.");
            return;
        }
        
        // Obtenemos el contenedor de las barras de herramientas desde el registro.
        // ViewBuilder debe haberlo registrado con esta clave.
        JPanel toolbarContainer = registry.get("container.toolbars");
        
        if (toolbarContainer != null) {
            // Revalidate recalcula el layout, repaint lo redibuja.
            toolbarContainer.revalidate();
            toolbarContainer.repaint();
        } else {
            logger.warn("WARN [revalidateToolbarContainer]: 'container.toolbars' no encontrado en el registro.");
        }
    } // --- FIN del metodo revalidateToolbarContainer ---
    
    
    /**
     * Orquesta de forma segura la reconstrucción del contenedor de barras de herramientas.
     * Utiliza un flag para evitar que múltiples eventos de cierre disparen reconstrucciones
     * en cascada y causen un bucle infinito.
     */
    public void solicitarReconstruccionDeToolbars() {
        if (isRebuildingToolbars) return; // Si ya está en proceso, no hace nada.

        try {
            isRebuildingToolbars = true; // Bloquea
            
            // 1. Desactivamos los listeners ANTES de manipular el contenedor.
            desactivarListenersDeToolbars();
            
            // 2. Llamamos a la reconstrucción.
            if (toolbarManager != null) {
                toolbarManager.reconstruirContenedorDeToolbars(model.getCurrentWorkMode());
            }

        } finally {
            // 3. Reactivamos los listeners DESPUÉS de que la UI se ha estabilizado.
            //    Lo hacemos en un invokeLater para asegurar que se ejecuta al final de la cola de eventos.
            javax.swing.SwingUtilities.invokeLater(() -> {
                reactivarListenersDeToolbars();
                isRebuildingToolbars = false; // Desbloquea
            });
        }
    } // --- Fin del método solicitarReconstruccionDeToolbars ---
    
    
    /**
     * Itera sobre todas las barras gestionadas y les quita su AncestorListener.
     */
    private void desactivarListenersDeToolbars() {
        logger.debug("  -> Desactivando AncestorListeners...");
        if (toolbarManager == null) return;
        for (javax.swing.JToolBar tb : toolbarManager.getManagedToolbars().values()) {
            Object listenerObj = tb.getClientProperty("JM_ANCESTOR_LISTENER");
            if (listenerObj instanceof javax.swing.event.AncestorListener) {
                tb.removeAncestorListener((javax.swing.event.AncestorListener) listenerObj);
            }
        }
    }

    /**
     * Itera sobre todas las barras gestionadas y les vuelve a añadir su AncestorListener.
     */
    private void reactivarListenersDeToolbars() {
        logger.debug("  -> Reactivando AncestorListeners...");
        if (toolbarManager == null) return;
        for (javax.swing.JToolBar tb : toolbarManager.getManagedToolbars().values()) {
            Object listenerObj = tb.getClientProperty("JM_ANCESTOR_LISTENER");
            if (listenerObj instanceof javax.swing.event.AncestorListener) {
                tb.addAncestorListener((javax.swing.event.AncestorListener) listenerObj);
            }
        }
    }
    
    
// *********************************************************************************************** configurarShutdownHookInternal
    

    /**
     * Orquesta el proceso de apagado limpio de la aplicación.
     * Es llamado cuando el usuario cierra la ventana principal.
     */
    public void shutdownApplication() {
        logger.info("--- [VisorController] Solicitud de cierre de aplicación recibida, delegando a GeneralController ---");
        if (generalController != null) {
            generalController.handleApplicationShutdown();
        } else {
            // Fallback muy básico si GeneralController no está disponible
            logger.error("GeneralController es nulo. Realizando cierre de emergencia.");
            System.exit(0);
        }
    } // --- FIN del metodo shutdownApplication ---
    
    
    /**
     * Guarda el estado actual de la ventana (posición, tamaño, etc.) en el
     * ConfigurationManager. Este método es llamado por el GeneralController durante
     * el proceso de cierre.
     */
    public void guardarEstadoVentanaEnConfig() {
        if (view == null || configuration == null) {
            logger.warn("WARN [guardarEstadoVentanaEnConfig]: Vista o Configuración nulas. No se puede guardar estado.");
            return;
        }
        
        try {
            boolean isMaximized = (view.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
            configuration.setString(ConfigKeys.WINDOW_MAXIMIZED, String.valueOf(isMaximized));
            
            java.awt.Rectangle normalBounds = view.getLastNormalBounds(); 
            if (normalBounds != null) {
                configuration.setString(ConfigKeys.WINDOW_X, String.valueOf(normalBounds.x));
                configuration.setString(ConfigKeys.WINDOW_Y, String.valueOf(normalBounds.y));
                configuration.setString(ConfigKeys.WINDOW_WIDTH, String.valueOf(normalBounds.width));
                configuration.setString(ConfigKeys.WINDOW_HEIGHT, String.valueOf(normalBounds.height));
            }
        } catch (Exception e) {
            logger.error("ERROR al guardar el estado de la ventana: " + e.getMessage(), e);
        }
    } // ---FIN de metodo guardarEstadoVentanaEnConfig---


    /**
     * Apaga el ExecutorService de forma ordenada. Este método es llamado por el
     * GeneralController durante el proceso de cierre.
     */
    public void apagarExecutorServiceOrdenadamente() {
        if (executorService != null && !executorService.isShutdown()) {
           executorService.shutdown();
           try {
               if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                   logger.warn("ExecutorService no terminó a tiempo. Forzando cierre...");
                   executorService.shutdownNow();
               }
           } catch (InterruptedException ie) {
               logger.warn("Interrumpido mientras se esperaba el cierre del ExecutorService.");
               executorService.shutdownNow();
               Thread.currentThread().interrupt();
           }
        }
    } // ---FIN de metodo apagarExecutorServiceOrdenadamente---

    
// ******************************************************************************************* FIN configurarShutdownHookInternal    
    
    
 // ******************************************************************************************************************* CARGA      

    
    /**
     * Configura listeners en los paneles de visualización de imágenes para que
     * se reajuste el zoom automáticamente cuando el tamaño del panel cambia.
     */
    private void configurarListenersDeRedimension() {
        if (registry == null || zoomManager == null || model == null) {
            logger.error("ERROR [configurarListenersDeRedimension]: Dependencias nulas (registry, zoomManager o model).");
            return;
        }

        // Crear una instancia del listener para reutilizarla
        java.awt.event.ComponentListener resizeListener = new java.awt.event.ComponentAdapter() {
            // Usamos un Timer para evitar una avalancha de repaints mientras se arrastra la ventana.
            // Solo se ejecutará el último evento de redimensionado tras un breve lapso de inactividad.
            private javax.swing.Timer repaintTimer = new javax.swing.Timer(100, e -> {
                logger.debug(" -> [Resize Timer] Disparando re-aplicación de zoom.");
                // Aplicamos el modo de zoom del CONTEXTO DE ZOOM ACTUAL.
                // El modelo ya sabe cuál es el correcto (visualizador, proyecto, etc.).
                zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
            });

            {
                repaintTimer.setRepeats(false); // El timer se ejecuta solo una vez por ráfaga de eventos.
            }

            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                // Cada vez que el componente se redimensiona, reiniciamos el timer.
                // Si el usuario está arrastrando el borde de la ventana, esto reiniciará
                // el timer constantemente, y solo cuando suelte y pasen 100ms, se ejecutará.
                repaintTimer.restart();
            }
        };

        // Aplicar el listener a todos los paneles de visualización relevantes
        ImageDisplayPanel panelVisor = registry.get("panel.display.imagen");
        if (panelVisor != null) {
            panelVisor.addComponentListener(resizeListener);
            logger.debug("  -> Listener de redimensionado añadido a 'panel.display.imagen'.");
        }

        ImageDisplayPanel panelProyecto = registry.get("panel.proyecto.display");
        if (panelProyecto != null) {
            panelProyecto.addComponentListener(resizeListener);
            logger.debug("  -> Listener de redimensionado añadido a 'panel.proyecto.display'.");
        }

        ImageDisplayPanel panelCarrusel = registry.get("panel.display.carousel");
        if (panelCarrusel != null) {
            panelCarrusel.addComponentListener(resizeListener);
            logger.debug("  -> Listener de redimensionado añadido a 'panel.display.carousel'.");
        }

    } // --- Fin del método configurarListenersDeRedimension ---
    
    
// ************************************************************************************************************* FIN DE CARGA
    
// *************************************************************************************************************** POPUP MENU  
    
    /**
     * Configura todos los menús contextuales de la aplicación.
     * Este método se llama durante la inicialización de la UI.
     */
    public void configurarMenusContextuales() {
        logger.debug("  [VisorController] Configurando Menús Contextuales...");
        if (actionMap == null || actionMap.isEmpty()) {
            logger.warn("WARN [configurarMenusContextuales]: ActionMap es nulo o vacío. No se pueden crear menús.");
            return;
        }

        // Crear los JPopupMenu para cada área.
        popupMenuImagenPrincipal = crearMenuContextualStandard();
        popupMenuListaNombres = crearMenuContextualStandard();
        popupMenuListaMiniaturas = crearMenuContextualStandard();
        popupMenuGrid = crearMenuContextualStandard();

        // Obtener los componentes de la UI donde se activarán los menús desde el registro.
        JLabel labelImagenPrincipal = registry.get("label.imagenPrincipal");
        JList<String> listaNombres = registry.get("list.nombresArchivo");
        JList<String> listaMiniaturas = registry.get("list.miniaturas");
        JList<String> gridList = registry.get("list.grid");
        
        // --- INICIO CORRECCIÓN: Instanciar PopupListener y guardar la referencia ---
        // Para labelImagenPrincipal
        if (labelImagenPrincipal != null) {
            // Eliminar el listener previamente si existe para evitar duplicados.
            if (popupListenerImagenPrincipal != null) {
                labelImagenPrincipal.removeMouseListener(popupListenerImagenPrincipal);
            }
            // Crear una nueva instancia y guardarla.
            popupListenerImagenPrincipal = new PopupListener(popupMenuImagenPrincipal);
            labelImagenPrincipal.addMouseListener(popupListenerImagenPrincipal);
            logger.debug("    -> Menú contextual añadido a label.imagenPrincipal.");
        } else {
            logger.warn("WARN [configurarMenusContextuales]: 'label.imagenPrincipal' no encontrado en el registro.");
        }

        // Para listaNombres
        if (listaNombres != null) {
            // Eliminar el listener previamente si existe.
            if (popupListenerListaNombres != null) {
                listaNombres.removeMouseListener(popupListenerListaNombres);
            }
            // Crear una nueva instancia y guardarla.
            popupListenerListaNombres = new PopupListener(popupMenuListaNombres);
            listaNombres.addMouseListener(popupListenerListaNombres);
            logger.debug("    -> Menú contextual añadido a list.nombresArchivo.");
        } else {
            logger.warn("WARN [configurarMenusContextuales]: 'list.nombresArchivo' no encontrado en el registro.");
        }

        // Para listaMiniaturas
        if (listaMiniaturas != null) {
            // Eliminar el listener previamente si existe.
            if (popupListenerListaMiniaturas != null) {
                listaMiniaturas.removeMouseListener(popupListenerListaMiniaturas);
            }
            // Crear una nueva instancia y guardarla.
            popupListenerListaMiniaturas = new PopupListener(popupMenuListaMiniaturas);
            listaMiniaturas.addMouseListener(popupListenerListaMiniaturas);
            logger.debug("    -> Menú contextual añadido a list.miniaturas.");
        } else {
            logger.warn("WARN [configurarMenusContextuales]: 'list.miniaturas' no encontrado en el registro.");
        }

        
        // --- INICIO DEL NUEVO BLOQUE PARA EL GRID ---
        
        if (gridList != null) {
            // Eliminar listeners antiguos para evitar duplicados
            if (popupListenerGrid != null) {
                gridList.removeMouseListener(popupListenerGrid);
            }
            
            // Creamos un listener especial para la JList del grid
            popupListenerGrid = new MouseAdapter() {
                public void mousePressed(MouseEvent e) { maybeShowPopup(e); }
                public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }

                private void maybeShowPopup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        // Paso CRUCIAL: Identificar la celda bajo el cursor
                        int index = gridList.locationToIndex(e.getPoint());
                        
                        // Si el cursor está sobre una celda válida y no está ya seleccionada, la seleccionamos.
                        // Esto asegura que la Action (ej. "Marcar Imagen") actúe sobre la imagen correcta.
                        if (index != -1 && gridList.getSelectedIndex() != index) {
                            gridList.setSelectedIndex(index);
                        }
                        
                        // Ahora que la selección es correcta, mostramos el menú.
                        popupMenuGrid.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            };
            
            gridList.addMouseListener(popupListenerGrid);
            logger.debug("    -> Menú contextual añadido a list.grid.");
        } else {
            logger.warn("WARN [configurarMenusContextuales]: 'list.grid' no encontrado en el registro.");
        }
        // --- FIN DEL NUEVO BLOQUE PARA EL GRID ---
        
        logger.debug("  [VisorController] Menús Contextuales configurados.");
    } // --- FIN del metodo configurarMenusContextuales ---

    
    /**
     * Crea y devuelve un JPopupMenu con un conjunto estándar de acciones
     * para el modo VISUALIZADOR.
     * @return Un JPopupMenu pre-poblado.
     */
    private JPopupMenu crearMenuContextualStandard() {
        JPopupMenu menu = new JPopupMenu();
        if (actionMap == null) return menu; // Seguridad

        // 1. Acciones de Proyecto/Marcado
        // ¡CORRECCIÓN! Usamos la Action directamente, que ya sabe cómo manejar el estado.
        Action marcarAction = actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
        if (marcarAction != null) {
            JCheckBoxMenuItem marcarItem = new JCheckBoxMenuItem(marcarAction);
            menu.add(marcarItem);
        }
        
        menu.addSeparator();

        // 2. Acciones de Archivo/Ubicación
        menu.add(new JMenuItem(actionMap.get(AppActionCommands.CMD_IMAGEN_LOCALIZAR)));
        
        menu.addSeparator();

        // 3. Acciones de Zoom/Vista (como JCheckBoxMenuItem para reflejar estado)
        menu.add(new JCheckBoxMenuItem(actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE)));
        menu.add(new JMenuItem(actionMap.get(AppActionCommands.CMD_ZOOM_RESET))); // Reset no es un toggle
        menu.addSeparator();
        menu.add(new JCheckBoxMenuItem(actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES)));
        menu.add(new JCheckBoxMenuItem(actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS)));

        return menu;
    }// Fin del metodo crearMenuContextualStandard
    

    /**
     * Clase interna para manejar la lógica de mostrar el JPopupMenu
     * en respuesta a un clic de ratón (especialmente clic derecho).
     */
    private static class PopupListener extends MouseAdapter {
        private final JPopupMenu popupMenu;

        public PopupListener(JPopupMenu popupMenu) {
            this.popupMenu = popupMenu;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
    // --- FIN AÑADIDO: Métodos para Menús Contextuales ---
    
// *********************************************************************************************************** FIN POPUP MENU  

    
// *************************************************************************************************************** NAVEGACION
    
	
    void configurarFocusListenerMenu() {
        if (view == null) return;
        JMenuBar menuBar = view.getJMenuBar();
        if (menuBar != null) {
            menuBar.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    logger.debug("--- [FocusListener] JMenuBar GANÓ el foco (forzado). ---");
                    if (menuBar.getMenuCount() > 0) menuBar.getMenu(0).setSelected(true);
                    if (statusBarManager != null) statusBarManager.mostrarMensajeTemporal("Navegación por menú activada (pulsa Alt o Esc para salir)", 4000);
                }
                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    logger.debug("--- [FocusListener] JMenuBar PERDIÓ el foco. ---");
                    if (menuBar.getMenuCount() > 0) {
                        if (menuBar.getMenu(0).isSelected()) menuBar.getMenu(0).setSelected(false);
                    }
                    if (statusBarManager != null) statusBarManager.limpiarMensaje();
                }
            });
        }
    }
    
    
    /**
     * Configura los bindings de teclado personalizados para las JList, enfocándose
     * principalmente en las flechas direccionales. Las teclas HOME, END, PAGE_UP, PAGE_DOWN
     * serán manejadas globalmente por el KeyEventDispatcher cuando el foco esté
     * en el área de miniaturas.
     */
//    @SuppressWarnings("serial")
    /*package-private*/ void interceptarAccionesTecladoListas() {
        if (view == null || listCoordinator == null || registry == null) {
            logger.warn("WARN [interceptarAccionesTecladoListas]: Dependencias nulas.");
            return;
        }
        logger.debug("  -> Configurando bindings de teclado para JLists...");

        // --- Acciones Reutilizables ---
        Action selectPreviousAction = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (listCoordinator != null) listCoordinator.seleccionarAnterior(); }
        };
        Action selectNextAction = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (listCoordinator != null) listCoordinator.seleccionarSiguiente(); }
        };

        // --- Aplicar SOLO a listaNombres ---
        // La navegación en la lista de nombres es simple y no entra en conflicto con un JScrollPane.
        JList<String> listaNombres = registry.get("list.nombresArchivo");
        if (listaNombres != null) {
            InputMap inputMap = listaNombres.getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap actionMap = listaNombres.getActionMap();
            
            final String ACT_PREV = "coordSelectPrevious";
            final String ACT_NEXT = "coordSelectNext";
            
            // Forzamos que IZQ/DER también naveguen
            inputMap.put(KeyStroke.getKeyStroke("LEFT"), ACT_PREV);
            inputMap.put(KeyStroke.getKeyStroke("RIGHT"), ACT_NEXT);

            actionMap.put(ACT_PREV, selectPreviousAction);
            actionMap.put(ACT_NEXT, selectNextAction);
            
            
            // =========================================================================
            String actionName = "selectNextMatch";
            for (int i = 0; i <= 9; i++) {
                KeyStroke numberKeyStroke = KeyStroke.getKeyStroke(String.valueOf(i).charAt(0));
                inputMap.put(numberKeyStroke, actionName);
            }
            logger.debug("    -> Re-vinculada la acción '{}' para las teclas numéricas 0-9 en la JList.", actionName);
            // =========================================================================
            
            
            // UP y DOWN ya funcionan por defecto para cambiar la selección, lo que dispara nuestro
            // ListSelectionListener, así que no necesitamos sobreescribirlos aquí.
            // HOME, END, PAGE_UP/DOWN también tienen comportamiento por defecto que es aceptable para esta lista.
        }

        // NO APLICAMOS NINGÚN BINDING a la lista de miniaturas.
        // Toda la navegación por teclado para esa área será manejada por el KeyEventDispatcher
        // para evitar conflictos con el JScrollPane y asegurar que se use el modelo principal.
        
        logger.debug("  -> Bindings de teclado para JLists configurados.");
    } // --- FIN del metodo interceptarAccionesTecladoListas ---

    
// ********************************************************************************************************* FIN DE NAVEGACION    
// ***************************************************************************************************************************    

// ***************************************************************************************************************************    
// ****************************************************************************************************************** UTILIDAD

    
    /**
     * Orquesta el refresco de la vista principal después de una operación de edición.
     * Este método es llamado por el EditionManager después de modificar la imagen en el modelo.
     *
     * @param resetearZoom Si es true, el zoom y el paneo se resetearán a su estado por defecto,
     *                     lo cual es necesario si la edición cambió las dimensiones de la imagen (ej. rotación).
     */
    public void solicitarRefrescoDeVistaPorEdicion(boolean resetearZoom) {
        logger.debug("[VisorController] Solicitud de refresco por edición recibida. Resetear zoom: " + resetearZoom);
        
        // Validar que las dependencias necesarias existen.
        if (model == null || zoomManager == null) {
            logger.error("ERROR [solicitarRefrescoDeVistaPorEdicion]: Modelo o ZoomManager nulos.");
            return;
        }

        // Si la edición cambió las dimensiones (como una rotación), es crucial
        // resetear el estado del zoom en el modelo ANTES de redibujar.
        if (resetearZoom) {
            logger.debug("  -> Reseteando estado de zoom en el modelo...");
            model.resetZoomState();
        }

        // Llamamos al método del ZoomManager para que aplique el modo de zoom actual.
        // Este método ya se encarga de calcular el reescalado, actualizar la vista y
        // sincronizar los botones/radios relacionados con el zoom.
        // Es la forma más robusta y centralizada de refrescar la imagen.
        logger.debug("  -> Delegando a ZoomManager para aplicar el modo de zoom actual y refrescar la vista...");
        zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());

        // Forzamos una actualización de las barras de información, ya que la imagen
        // (y posiblemente sus propiedades como dimensiones) ha cambiado.
        if (infobarImageManager != null) {
            infobarImageManager.actualizar();
        }
        if (statusBarManager != null) {
            statusBarManager.actualizar();
        }
        
        logger.debug("[VisorController] Refresco por edición completado.");
    } // --- Fin del método solicitarRefrescoDeVistaPorEdicion ---
    
    
    /**
     * Ejecuta un refresco completo del modo VISUALIZADOR.
     * Vuelve a leer el contenido de la carpeta raíz actual desde el disco y, si el filtro
     * Tornado estaba activo, lo reaplica automáticamente.
     */
    public void ejecutarRefrescoCompleto() {
        logger.debug("--- [VisorController] Ejecutando Refresco Completo del Modo Visualizador (Versión Inteligente) ---");

        if (model == null || generalController == null || registry == null) {
            logger.error("ERROR [ejecutarRefrescoCompleto]: Dependencias nulas.");
            return;
        }

        final Path carpetaActual = model.getCarpetaRaizActual();
        if (carpetaActual == null) {
            logger.warn("No hay carpeta raíz seleccionada. No se puede refrescar.");
            return;
        }

        // 1. Guardar el estado que queremos restaurar
        final String claveASeleccionar = model.getSelectedImageKey();
        
        String textoFiltroAReaplicar = null;
        if (model.isLiveFilterActive()) {
            javax.swing.JTextField searchField = registry.get("textfield.filtro.orden");
            if (searchField != null && !searchField.getText().isBlank() && !searchField.getText().equals("Texto a buscar...")) {
                textoFiltroAReaplicar = searchField.getText();
                
                // 2. ORDENAR AL GENERALCONTROLLER QUE LIMPIE SU ESTADO DE FILTRO INTERNO
                //    Esta es la clave para que luego se pueda desactivar.
                logger.debug("  -> Filtro Tornado activo. Limpiando estado en GeneralController antes de recargar.");
                generalController.limpiarEstadoFiltroRapidoSiActivo();
            }
        }
        
        final String finalTextoFiltro = textoFiltroAReaplicar;

        // 3. Definir el callback para REAPLICAR el filtro después de la carga
        Runnable accionPostCarga = () -> {
            logger.debug("    -> [Callback post-refresco] Sincronizando UI...");
            generalController.sincronizarTodaLaUIConElModelo();
            
            if (finalTextoFiltro != null) {
                logger.debug("    -> Reaplicando filtro Tornado con texto: '{}'", finalTextoFiltro);
                javax.swing.JTextField searchField = registry.get("textfield.filtro.orden");
                if (searchField != null) {
                    searchField.setText(finalTextoFiltro);
                    // Vuelve a activar el filtro sobre la nueva lista de datos
                    generalController.onLiveFilterStateChanged(true);
                }
            }
        };

        // 4. Iniciar la recarga de la lista, pasando nuestro callback especial.
        logger.debug("  -> Recargando lista de imágenes para la carpeta: {}", carpetaActual);
        imageListManager.cargarListaImagenes(claveASeleccionar, accionPostCarga);
        
        logger.debug("--- [VisorController] Refresco Inteligente encolado/ejecutado. ---");
    } // --- Fin del método ejecutarRefrescoCompleto ---
    

    /**
     * Inicia el proceso de carga y visualización de la imagen principal.
     * Es el método central que se llama cada vez que cambia la selección de imagen.
     * Orquesta la carga en segundo plano y la actualización de la UI.
     *
     * @param indiceSeleccionado El índice de la imagen a mostrar en el modelo principal.
     */
    public void actualizarImagenPrincipal(int indiceSeleccionado) {
        // --- 1. VALIDACIÓN DE DEPENDENCIAS ---
        if (view == null || model == null || executorService == null || executorService.isShutdown() || registry == null) {
            logger.warn("WARN [actualizarImagenPrincipal]: Dependencias no listas.");
            return;
        }

        // --- 2. OBTENER EL PANEL DE VISUALIZACIÓN ACTIVO ---
        ImageDisplayPanel displayPanel = (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO)
                                       ? registry.get("panel.proyecto.display")
                                       : registry.get("panel.display.imagen");
        if (displayPanel == null) {
            logger.error("ERROR CRÍTICO: No se encontró el panel de display activo.");
            return;
        }
        
        // --- 3. MANEJO DEL CASO SIN SELECCIÓN ---
        if (indiceSeleccionado == -1) {
            model.setCurrentImage(null);
            model.setSelectedImageKey(null);
            displayPanel.limpiar();
            if (listCoordinator != null) {
                listCoordinator.forzarActualizacionEstadoAcciones();
            }
            return;
        }
        
        // --- 4. PREPARACIÓN PARA LA CARGA ---
        final String archivoSeleccionadoKey = model.getSelectedImageKey(); // La clave se hace final aquí
        logger.debug("--> [actualizarImagenPrincipal] Iniciando carga para clave: '" + archivoSeleccionadoKey + "'");

        if (cargaImagenPrincipalFuture != null && !cargaImagenPrincipalFuture.isDone()) {
            cargaImagenPrincipalFuture.cancel(true);
        }
        final Path rutaCompleta = model.getRutaCompleta(archivoSeleccionadoKey); // La ruta también se hace final aquí
        if (rutaCompleta == null) {
            displayPanel.mostrarError("Ruta no encontrada para:\n" + archivoSeleccionadoKey, null);
            return;
        }
        displayPanel.mostrarCargando("Cargando: " + rutaCompleta.getFileName() + "...");
        
        // --- 5. EJECUTAR LA CARGA EN SEGUNDO PLANO ---
        cargaImagenPrincipalFuture = executorService.submit(() -> {
            BufferedImage imagenCargadaDesdeDisco = null;
            try {
                // Comprobación temprana de interrupción
                if (Thread.currentThread().isInterrupted()) {
                    logger.trace("Tarea de carga para '{}' cancelada antes de empezar la lectura.", rutaCompleta.getFileName());
                    return;
                }

                if (!Files.exists(rutaCompleta)) throw new IOException("El archivo no existe: " + rutaCompleta);
                imagenCargadaDesdeDisco = ImageIO.read(rutaCompleta.toFile());
                if (imagenCargadaDesdeDisco == null) throw new IOException("Formato no soportado o archivo inválido.");

            } catch (Exception ex) {
                // --- INICIO DE LA MODIFICACIÓN CLAVE ---
                // Si el hilo ha sido interrumpido, no es un error real, es una cancelación.
                if (Thread.currentThread().isInterrupted()) {
                    logger.debug("La carga de la imagen '{}' fue interrumpida, lo cual es normal durante la navegación rápida.", rutaCompleta.getFileName());
                } else {
                    // Si no fue interrumpido, es un error genuino de lectura.
                    logger.error("Error al cargar la imagen '{}': {}", rutaCompleta.getFileName(), ex.getMessage());
                }
                // En ambos casos (cancelación o error real), la imagen cargada será null.
                imagenCargadaDesdeDisco = null; 
                // --- FIN DE LA MODIFICACIÓN CLAVE ---
            }
            
            // Si después de la lectura, el hilo fue interrumpido, salimos.
            if (Thread.currentThread().isInterrupted()) {
                 logger.trace("Tarea de carga para '{}' cancelada después de la lectura.", rutaCompleta.getFileName());
                 return;
            }

            final BufferedImage imagenCorregida = utils.ImageUtils.correctImageOrientation(imagenCargadaDesdeDisco, rutaCompleta);
            
            final BufferedImage finalImagenCargada = imagenCorregida;

            SwingUtilities.invokeLater(() -> {
                // ... el resto del método SwingUtilities.invokeLater se queda exactamente igual ...
                // Doble chequeo: solo actualizar si la imagen que hemos cargado sigue siendo la seleccionada.
                if (!Objects.equals(archivoSeleccionadoKey, model.getSelectedImageKey())) {
                    logger.debug("  [actualizarImagenPrincipal EDT] Carga de '" + archivoSeleccionadoKey + "' descartada. La selección ha cambiado.");
                    return;
                }
                
                if (view == null || model == null || zoomManager == null || registry == null || projectManager == null) return;
                
                if (finalImagenCargada != null) {
                    // --- Caso de Éxito ---
                    model.setCurrentImage(finalImagenCargada);
                    displayPanel.limpiar();

                    if (zoomManager != null) {
                        logger.debug("  [actualizarImagenPrincipal] Aplicando modo de zoom actual...");
                        zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
                    }

                    boolean estaMarcada = projectManager.estaMarcada(rutaCompleta);
                    actualizarEstadoVisualBotonMarcarYBarraEstado(estaMarcada, rutaCompleta);

                } else { 
                    // --- Caso de Error de Carga ---
                    model.setCurrentImage(null);
                    if (iconUtils != null) {
                         ImageIcon errorIcon = iconUtils.getScaledCommonIcon("imagen-rota.png", 128, 128);
                         displayPanel.mostrarError("Error al cargar: \n" + rutaCompleta.getFileName().toString(), errorIcon);
                    }
                    actualizarEstadoVisualBotonMarcarYBarraEstado(false, null);
                }

                if (infobarImageManager != null) infobarImageManager.actualizar();
                if (statusBarManager != null) statusBarManager.actualizar();
                if (listCoordinator != null) listCoordinator.forzarActualizacionEstadoAcciones();
            });
        });
        
    } // --- Fin del método actualizarImagenPrincipal ---
    
    
    /**
     * Carga y muestra una imagen principal directamente por su Path absoluto,
     * sin depender de que esté en el modelo de lista actual. Esto es útil
     * para previsualizaciones rápidas de archivos no indexados (ej., desde exportación).
     *
     * @param rutaCompleta La Path absoluta de la imagen a cargar.
     * @param claveImagen (Opcional) La clave (String) asociada a esta ruta si existe, para logs o referencias.
     */
    public void actualizarImagenPrincipalPorPath(Path rutaCompleta, String claveImagen) {
        logger.debug("--> [actualizarImagenPrincipalPorPath] Iniciando carga directa para: '" + (claveImagen != null ? claveImagen : rutaCompleta.getFileName()) + "'");

        // 1. VALIDACIÓN DE DEPENDENCIAS
        if (view == null || model == null || executorService == null || executorService.isShutdown() || registry == null) {
            logger.warn("WARN [actualizarImagenPrincipalPorPath]: Dependencias no listas (vista, modelo, executor o registry nulos).");
            return;
        }

        // 2. OBTENER EL PANEL DE VISUALIZACIÓN ACTIVO
        ImageDisplayPanel displayPanel = (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO)
                                       ? registry.get("panel.proyecto.display")
                                       : registry.get("panel.display.imagen");
        
        if (displayPanel == null) {
            logger.error("ERROR CRÍTICO: No se encontró el panel de display activo en el registro.");
            return;
        }
        
        // 3. MANEJO DE RUTA NULA
        if (rutaCompleta == null) {
            logger.debug("--> [actualizarImagenPrincipalPorPath] Limpiando panel de imagen por ruta nula.");
            model.setCurrentImage(null);
            // No cambiamos selectedImageKey aquí, ya que esta carga es independiente de la selección de lista.
            displayPanel.limpiar();
            return;
        }

        // 4. CANCELAR CARGAS ANTERIORES
        if (cargaImagenPrincipalFuture != null && !cargaImagenPrincipalFuture.isDone()) {
            cargaImagenPrincipalFuture.cancel(true);
        }

        // 5. MOSTRAR INDICADOR DE CARGA
        displayPanel.mostrarCargando("Cargando: " + rutaCompleta.getFileName() + "...");
        
        // 6. EJECUTAR LA CARGA EN SEGUNDO PLANO
        cargaImagenPrincipalFuture = executorService.submit(() -> {
            BufferedImage imagenCargadaDesdeDisco = null;
            try {
                if (Thread.currentThread().isInterrupted()) {
                    logger.trace("Tarea de carga por Path para '{}' cancelada antes de empezar.", rutaCompleta.getFileName());
                    return;
                }

                if (!Files.exists(rutaCompleta)) throw new IOException("El archivo no existe: " + rutaCompleta);
                imagenCargadaDesdeDisco = ImageIO.read(rutaCompleta.toFile());
                if (imagenCargadaDesdeDisco == null) throw new IOException("Formato no soportado o archivo inválido.");

            } catch (Exception ex) {
                if (Thread.currentThread().isInterrupted()) {
                    logger.debug("La carga por Path de la imagen '{}' fue interrumpida.", rutaCompleta.getFileName());
                } else {
                    logger.error("Error al cargar la imagen por Path '{}': {}", rutaCompleta.getFileName(), ex.getMessage());
                }
                imagenCargadaDesdeDisco = null;
            }
            
            if (Thread.currentThread().isInterrupted()) {
                logger.trace("Tarea de carga por Path para '{}' cancelada después de la lectura.", rutaCompleta.getFileName());
                return;
            }

            final BufferedImage imagenCorregida = utils.ImageUtils.correctImageOrientation(imagenCargadaDesdeDisco, rutaCompleta);
            
            final BufferedImage finalImagenCargada = imagenCorregida;
            
            final Path finalPath = rutaCompleta;

            SwingUtilities.invokeLater(() -> {
                // El resto del método SwingUtilities.invokeLater se queda exactamente igual.
                if (finalImagenCargada != null) {
                    model.setCurrentImage(finalImagenCargada);
                    displayPanel.limpiar();
                    if (zoomManager != null) {
                         zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
                    } else {
                         displayPanel.repaint();
                    }
                } else { 
                    model.setCurrentImage(null);
                    if (iconUtils != null) {
                         ImageIcon errorIcon = iconUtils.getScaledCommonIcon("imagen-rota.png", 128, 128);
                         displayPanel.mostrarError("Error al cargar: \n" + finalPath.getFileName().toString(), errorIcon);
                    }
                }
                if (infobarImageManager != null) infobarImageManager.actualizar();
                if (statusBarManager != null) statusBarManager.actualizar();
            });
        });
        
    }// --- Fin del nuevo método actualizarImagenPrincipalPorPath ---
    
    
// *********************************************************************************************************** FIN DE UTILIDAD  
// ***************************************************************************************************************************    

// ***************************************************************************************************************************     
// ******************************************************************************************************************** LOGICA
     
     
     /**
      * Solicita que la JList de miniaturas se repinte.
      * Esto hará que MiniaturaListCellRenderer se ejecute para las celdas visibles
      * y pueda leer el nuevo estado de configuración para mostrar/ocultar nombres.
      */
     public void solicitarRefrescoRenderersMiniaturas() {
         if (view != null && registry.get("list.miniaturas") != null) {
             logger.debug("  [Controller] Solicitando repintado de listaMiniaturas.");
             registry.get("list.miniaturas").repaint();

             // Si ocultar/mostrar nombres cambia la ALTURA de las celdas,
             // podrías necesitar más que un simple repaint().
             // Por ahora, asumamos que la altura de la celda es fija y solo cambia
             // la visibilidad del JLabel del nombre.
             // Si la altura cambia, necesitarías:
             // 1. Que MiniaturaListCellRenderer devuelva una nueva PreferredSize.
             // 2. Invalidar el layout de la JList:
             //    registry.get("list.miniaturas").revalidate();
             //    registry.get("list.miniaturas").repaint();
             // 3. Posiblemente recalcular el número de miniaturas visibles si la altura de celda cambió.
             //    Esto haría que el `ComponentListener` de redimensionamiento sea más complejo
             //    o que necesites llamar a actualizarModeloYVistaMiniaturas aquí también.
             // ¡POR AHORA, MANTENGAMOSLO SIMPLE CON SOLO REPAINT!
         }
     } // --- FIN metodo solicitarRefrescoRenderersMiniaturas
     
     
     /**
      * Orquesta el refresco de la vista principal después de que una selección de imagen
      * ha sido actualizada en el modelo (ya sea en modo Visualizador o Proyecto).
      * Este método centraliza la lógica de refresco de la imagen y el zoom.
      */
     public void solicitarRefrescoDeVistaPorSeleccion() {
         logger.debug("[VisorController] Solicitud de refresco por selección recibida.");
         
         if (model == null || zoomManager == null) {
             logger.error("ERROR [solicitarRefrescoDeVistaPorSeleccion]: Modelo o ZoomManager nulos.");
             return;
         }

         // Simplemente le pedimos al ZoomManager que aplique el modo de zoom actual.
         // Esto se encargará de:
         // 1. Leer la imagen actual del modelo.
         // 2. Calcular el factor de zoom correcto para esa imagen y el modo.
         // 3. Actualizar la vista (forzando un repaint del ImageDisplayPanel).
         // 4. Sincronizar todos los controles de la UI relacionados con el zoom.
         logger.debug("  -> Delegando a ZoomManager para aplicar el modo de zoom actual y refrescar la vista...");
         zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());

         // También actualizamos las barras de info, ya que la imagen ha cambiado.
         if (infobarImageManager != null) {
             infobarImageManager.actualizar();
         }
         if (statusBarManager != null) {
             statusBarManager.actualizar();
         }

         logger.debug("[VisorController] Refresco por selección completado.");
     } // --- Fin del método solicitarRefrescoDeVistaPorSeleccion ---


// ************************************************************************************************************* FIN DE LOGICA     
// ***************************************************************************************************************************
      
// ***************************************************************************************************************************
// ********************************************************************************************************************** ZOOM     

     private void refrescarManualmenteLaVistaPrincipal() {
    	 
         if (this.zoomManager != null) {
        	 
             // La forma más segura de forzar un refresco es volver a aplicar el modo de zoom actual.
             if (this.model != null) {
                 this.zoomManager.aplicarModoDeZoom(this.model.getCurrentZoomMode());
             }
         } else {
             logger.error("ERROR [VisorController.refrescarManualmente]: ZoomManager es nulo.");
         }
 	}
    // --- FIN del metodo refrescarManualmenteLaVistaPrincipal ---

    
    /**
     * Configura los atajos de teclado globales para la aplicación.
     * Estos atajos funcionarán sin importar qué componente tenga el foco.
     */
     void configurarAtajosTecladoGlobales() {
         
	 	if (view == null || actionMap == null) {
	        logger.warn("WARN [configurarAtajosTecladoGlobales]: Vista o ActionMap nulos.");
	        return;
	    }
	    logger.debug("  [Controller] Configurando atajos de teclado globales...");

	    javax.swing.JRootPane rootPane = view.getRootPane();
	    // Usamos WHEN_IN_FOCUSED_WINDOW para que los atajos funcionen siempre que la ventana esté activa
	    javax.swing.InputMap inputMap = rootPane.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
	    javax.swing.ActionMap actionMapGlobal = rootPane.getActionMap();
	    
	    // Obtenemos el modificador de atajos estándar del sistema (Cmd en Mac, Ctrl en Win/Linux)
	    final int shortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

	    // --- Atajos Globales de Aplicación (código existente) ---
	    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), AppActionCommands.CMD_ESPECIAL_REFRESCAR);
	    actionMapGlobal.put(AppActionCommands.CMD_ESPECIAL_REFRESCAR, actionMap.get(AppActionCommands.CMD_ESPECIAL_REFRESCAR));
	    
	    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), AppActionCommands.CMD_VISTA_PANTALLA_COMPLETA);
	    actionMapGlobal.put(AppActionCommands.CMD_VISTA_PANTALLA_COMPLETA, actionMap.get(AppActionCommands.CMD_VISTA_PANTALLA_COMPLETA));
	    
	    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, shortcutKeyMask), AppActionCommands.CMD_IMAGEN_LOCALIZAR);
	    actionMapGlobal.put(AppActionCommands.CMD_IMAGEN_LOCALIZAR, actionMap.get(AppActionCommands.CMD_IMAGEN_LOCALIZAR));
	    

	    // --- INICIO DE LA NUEVA SECCIÓN: Atajos del Modo Proyecto ---
	    
	    // CTRL+S -> Guardar Proyecto
	    KeyStroke ctrlS = KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKeyMask);
	    inputMap.put(ctrlS, AppActionCommands.CMD_PROYECTO_GUARDAR);
	    actionMapGlobal.put(AppActionCommands.CMD_PROYECTO_GUARDAR, actionMap.get(AppActionCommands.CMD_PROYECTO_GUARDAR));
	    logger.debug("  -> Atajo registrado: Ctrl+S -> " + AppActionCommands.CMD_PROYECTO_GUARDAR);

	    // CTRL+SHIFT+S -> Guardar Proyecto Como
	    KeyStroke ctrlShiftS = KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKeyMask | KeyEvent.SHIFT_DOWN_MASK);
	    inputMap.put(ctrlShiftS, AppActionCommands.CMD_PROYECTO_GUARDAR_COMO);
	    actionMapGlobal.put(AppActionCommands.CMD_PROYECTO_GUARDAR_COMO, actionMap.get(AppActionCommands.CMD_PROYECTO_GUARDAR_COMO));
	    logger.debug("  -> Atajo registrado: Ctrl+Shift+S -> " + AppActionCommands.CMD_PROYECTO_GUARDAR_COMO);

	    // CTRL+N -> Nuevo Proyecto
	    KeyStroke ctrlN = KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutKeyMask);
	    inputMap.put(ctrlN, AppActionCommands.CMD_PROYECTO_NUEVO);
	    actionMapGlobal.put(AppActionCommands.CMD_PROYECTO_NUEVO, actionMap.get(AppActionCommands.CMD_PROYECTO_NUEVO));
	    logger.debug("  -> Atajo registrado: Ctrl+N -> " + AppActionCommands.CMD_PROYECTO_NUEVO);

	    // CTRL+O -> Abrir Proyecto
	    KeyStroke ctrlO = KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutKeyMask);
	    inputMap.put(ctrlO, AppActionCommands.CMD_PROYECTO_ABRIR);
	    actionMapGlobal.put(AppActionCommands.CMD_PROYECTO_ABRIR, actionMap.get(AppActionCommands.CMD_PROYECTO_ABRIR));
	    logger.debug("  -> Atajo registrado: Ctrl+O -> " + AppActionCommands.CMD_PROYECTO_ABRIR);
	    
	    // --- FIN DE LA NUEVA SECCIÓN ---

	    logger.debug("  -> Atajos de teclado globales configurados.");
	} // --- Fin del método configurarAtajosTecladoGlobales ---


// *************************************************************************************************************** FIN DE ZOOM     
// ***************************************************************************************************************************

// ***************************************************************************************************************************
// ******************************************************************************************************************* ARCHIVO     
     

     
     
     
	
// ************************************************************************************************************ FIN DE ARCHIVO
// ***************************************************************************************************************************

// ***************************************************************************************************************************
// *********************************************************************************************************** IModoController	
	

 // --- INICIO DE LA IMPLEMENTACIÓN DE IModoController ---

    /**
     * Navega a la siguiente imagen. Delega la lógica al ListCoordinator.
     * Es llamado por GeneralController.
     */
    public void navegarSiguiente() {
        if (listCoordinator != null) {
            listCoordinator.seleccionarSiguiente();
        }
    } // --- Fin del método navegarSiguiente ---

    /**
     * Navega a la imagen anterior. Delega la lógica al ListCoordinator.
     * Es llamado por GeneralController.
     */
    public void navegarAnterior() {
        if (listCoordinator != null) {
            listCoordinator.seleccionarAnterior();
        }
    } // --- Fin del método navegarAnterior ---

    /**
     * Navega a la primera imagen. Delega la lógica al ListCoordinator.
     * Es llamado por GeneralController.
     */
    public void navegarPrimero() {
        if (listCoordinator != null) {
            listCoordinator.seleccionarPrimero();
        }
    } // --- Fin del método navegarPrimero ---

    /**
     * Navega a la última imagen. Delega la lógica al ListCoordinator.
     * Es llamado por GeneralController.
     */
    public void navegarUltimo() {
        if (listCoordinator != null) {
            listCoordinator.seleccionarUltimo();
        }
    } // --- Fin del método navegarUltimo ---

    /**
     * Navega al bloque anterior de imágenes. Delega la lógica al ListCoordinator.
     * Es llamado por GeneralController.
     */
    public void navegarBloqueAnterior() {
        if (listCoordinator != null) {
            listCoordinator.seleccionarBloqueAnterior();
        }
    } // --- Fin del método navegarBloqueAnterior ---

    /**
     * Navega al bloque siguiente de imágenes. Delega la lógica al ListCoordinator.
     * Es llamado por GeneralController.
     */
    public void navegarBloqueSiguiente() {
        if (listCoordinator != null) {
            listCoordinator.seleccionarBloqueSiguiente();
        }
    } // --- Fin del método navegarBloqueSiguiente ---

    /**
     * Aplica zoom con la rueda del ratón. Delega la lógica al ZoomManager.
     * Es llamado por GeneralController.
     * @param e El evento de la rueda del ratón.
     */
    public void aplicarZoomConRueda(java.awt.event.MouseWheelEvent e) {
        if (zoomManager != null) {
            zoomManager.aplicarZoomConRueda(e);
            zoomManager.sincronizarEstadoVisualBotonesYRadiosZoom();
        }
    } // --- Fin del método aplicarZoomConRueda ---

    /**
     * Aplica paneo a la imagen. Delega la lógica al ZoomManager.
     * Es llamado por GeneralController.
     * @param deltaX Desplazamiento horizontal.
     * @param deltaY Desplazamiento vertical.
     */
    public void aplicarPan(int deltaX, int deltaY) {
        if (zoomManager != null) {
            zoomManager.aplicarPan(deltaX, deltaY);
        }
    } // --- Fin del método aplicarPan ---

    /**
     * Inicia una operación de paneo. Delega la lógica al ZoomManager.
     * Es llamado por GeneralController.
     * @param e El evento del ratón.
     */
    public void iniciarPaneo(java.awt.event.MouseEvent e) {
        if (zoomManager != null && model.isZoomHabilitado()) {
            zoomManager.iniciarPaneo(e);
        }
    } // --- Fin del método iniciarPaneo ---

    /**
     * Continúa una operación de paneo. Delega la lógica al ZoomManager.
     * Es llamado por GeneralController.
     * @param e El evento del ratón.
     */
    public void continuarPaneo(java.awt.event.MouseEvent e) {
        if (zoomManager != null && model.isZoomHabilitado()) {
            zoomManager.continuarPaneo(e);
        }
    } // --- Fin del método continuarPaneo ---

    
    @Override
    public void solicitarRefresco() {
        logger.debug("[VisorController] Solicitud de refresco recibida. Llamando a ejecutarRefrescoCompleto...");
        ejecutarRefrescoCompleto();
    }
    
    @Override
    public void aumentarTamanoMiniaturas() {
        final int STEP = 10;
        final int MAX_SIZE = 300;

        if (model.getCurrentDisplayMode() == VisorModel.DisplayMode.GRID) {
            int currentWidth = configuration.getInt(ConfigKeys.GRID_THUMBNAIL_WIDTH, 120);
            int newWidth = Math.min(currentWidth + STEP, MAX_SIZE);
            configuration.setString(ConfigKeys.GRID_THUMBNAIL_WIDTH, String.valueOf(newWidth));
            configuration.setString(ConfigKeys.GRID_THUMBNAIL_HEIGHT, String.valueOf(newWidth));
            GridDisplayPanel gridVisor = registry.get("panel.display.grid");
            if (gridVisor != null) gridVisor.setGridCellSize(newWidth, newWidth);
        } else {
            // --- CIRUGÍA PARA LA BARRA DE MINIATURAS ---
            // 1. Calcular y guardar el nuevo tamaño en la configuración
            int currentNormWidth = configuration.getInt(ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, 70);
            int newNormWidth = Math.min(currentNormWidth + STEP, MAX_SIZE);
            configuration.setString(ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, String.valueOf(newNormWidth));
            configuration.setString(ConfigKeys.MINIATURAS_TAMANO_NORM_ALTO, String.valueOf(newNormWidth));

            // 2. Limpiar la caché de imágenes viejas
            if (servicioMiniaturas != null) {
                servicioMiniaturas.limpiarCache();
            }

            // 3. (Paso crucial) Llamar al nuevo método en la VISTA para que actualice TODO el layout
            if (view != null) {
                view.actualizarLayoutBarraMiniaturas();
            }
            
            // 4. (Paso final, ahora sí en el orden correcto)
            //    Actualizar el contenido del viewport de miniaturas. Esto se hace al final,
            //    cuando la UI ya tiene sus nuevas dimensiones.
            if (listCoordinator instanceof ListCoordinator) {
                ((ListCoordinator) listCoordinator).forzarActualizacionDeTiraDeMiniaturas();
            }
        }
    } // ---FIN de metodo aumentarTamanoMiniaturas---
    

    @Override
    public void reducirTamanoMiniaturas() {
        final int STEP = 10;
        final int MIN_SIZE = 40;

        if (model.getCurrentDisplayMode() == VisorModel.DisplayMode.GRID) {
            int currentWidth = configuration.getInt(ConfigKeys.GRID_THUMBNAIL_WIDTH, 120);
            int newWidth = Math.max(currentWidth - STEP, MIN_SIZE);
            configuration.setString(ConfigKeys.GRID_THUMBNAIL_WIDTH, String.valueOf(newWidth));
            configuration.setString(ConfigKeys.GRID_THUMBNAIL_HEIGHT, String.valueOf(newWidth));
            GridDisplayPanel gridVisor = registry.get("panel.display.grid");
            if (gridVisor != null) gridVisor.setGridCellSize(newWidth, newWidth);
        } else {
            // --- CIRUGÍA PARA LA BARRA DE MINIATURAS ---
            // 1. Calcular y guardar el nuevo tamaño en la configuración
            int currentNormWidth = configuration.getInt(ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, 70);
            int newNormWidth = Math.max(currentNormWidth - STEP, MIN_SIZE);
            configuration.setString(ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, String.valueOf(newNormWidth));
            configuration.setString(ConfigKeys.MINIATURAS_TAMANO_NORM_ALTO, String.valueOf(newNormWidth));

            // 2. Limpiar la caché de imágenes viejas
            if (servicioMiniaturas != null) {
                servicioMiniaturas.limpiarCache();
            }

            // 3. (Paso crucial) Llamar al nuevo método en la VISTA para que actualice TODO el layout
            if (view != null) {
                view.actualizarLayoutBarraMiniaturas();
            }
            
            // 4. (Paso final, ahora sí en el orden correcto)
            //    Actualizar el contenido del viewport de miniaturas.
            if (listCoordinator instanceof ListCoordinator) {
                ((ListCoordinator) listCoordinator).forzarActualizacionDeTiraDeMiniaturas();
            }
        }
        
    } // ---FIN de metodo reducirTamanoMiniaturas---
    
     
// **************************************************************************************************** FIN DE IModoController     
// ***************************************************************************************************************************

    
    /**
     * Método "cerebro" que maneja la lógica de alternar el modo de paneo.
     * Es llamado por la Action correspondiente.
     */
    public void solicitarTogglePaneo() {
        // 1. Leer el estado actual del modelo (la fuente de verdad).
        boolean estadoActual = model.isZoomHabilitado();
        
        // 2. Calcular el nuevo estado.
        boolean nuevoEstado = !estadoActual;

        logger.debug("[VisorController] Solicitud para alternar paneo. Nuevo estado: " + nuevoEstado);

        // 3. Actualizar el modelo a través del ZoomManager.
        zoomManager.setPermisoManual(nuevoEstado);
        
        Action zoomManualAction = actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
        if (zoomManualAction != null) {
            zoomManualAction.putValue(Action.SELECTED_KEY, nuevoEstado);
        }
        
        // 4. Llamar al método de sincronización maestro para que actualice TODA la UI.
        zoomManager.sincronizarEstadoVisualBotonesYRadiosZoom();
    } // --- FIN del metodo solicitarTogglePaneo ---
     

     /**
      * Actualiza el estado lógico y visual para mantener (o no) las proporciones
      * de la imagen al reescalarla para ajustarse a la vista.
      * Guarda el nuevo estado en la configuración ('interfaz.menu.zoom.Mantener_Proporciones.seleccionado'),
      * actualiza la Action 'toggleProporcionesAction', actualiza el estado en el Modelo,
      * sincroniza la apariencia del botón toggle asociado y finalmente repinta la imagen principal
      * para que refleje el nuevo modo de escalado.
      *
      * @param mantener True si se deben mantener las proporciones originales de la imagen,
      *                 false si se debe estirar/encoger para rellenar el área de visualización.
      */
     public void setMantenerProporcionesAndUpdateConfig(boolean mantener) {
         // 1. Log inicio y validación de dependencias
         logger.debug("\n[Controller setMantenerProporciones] INICIO. Estado deseado (mantener): " + mantener);
         Action toggleProporcionesAction = (this.actionMap != null) ? this.actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES) : null;
         if (model == null || configuration == null || toggleProporcionesAction == null || view == null) {
             logger.error("  -> ERROR: Dependencias nulas (Modelo, Config, Action Proporciones o Vista). Abortando.");
             return;
         }

         // 2. Comprobar si el cambio es realmente necesario
         //    Comparamos el estado deseado con el estado actual de la Action.
         boolean estadoActualAction = Boolean.TRUE.equals(toggleProporcionesAction.getValue(Action.SELECTED_KEY));
         logger.debug("  -> Estado Lógico Actual (Action.SELECTED_KEY): " + estadoActualAction);
         if (mantener == estadoActualAction) {
             logger.debug("  -> Estado deseado ya es el actual. No se realizan cambios.");
             // Opcional: asegurar que la UI del botón esté sincronizada por si acaso
             // actualizarAspectoBotonToggle(toggleProporcionesAction, estadoActualAction);
             logger.debug("[Controller setMantenerProporciones] FIN (Sin cambios necesarios).");
             return;
         }

         logger.debug("  -> Aplicando cambio a estado (mantener proporciones): " + mantener);

         // 3. Actualizar el estado lógico de la Action ('SELECTED_KEY')
         //    Esto permite que los componentes asociados (JCheckBoxMenuItem) se actualicen.
         logger.debug("    1. Actualizando Action.SELECTED_KEY...");
         toggleProporcionesAction.putValue(Action.SELECTED_KEY, mantener);
         // logger.debug("       -> Action.SELECTED_KEY AHORA ES: " + Boolean.TRUE.equals(toggleProporcionesAction.getValue(Action.SELECTED_KEY)));

         // 4. Actualizar el estado en el Modelo
         logger.debug("    2. Actualizando Modelo...");
         model.setMantenerProporcion(mantener); // Llama al setter específico en el modelo
         // El modelo imprime su propio log al cambiar

         // 5. Actualizar la Configuración en Memoria
         logger.debug("    3. Actualizando Configuración en Memoria...");
         String configKey = "interfaz.menu.zoom.Mantener_Proporciones.seleccionado";
         configuration.setString(configKey, String.valueOf(mantener));
         // logger.debug("       -> Config '" + configKey + "' AHORA ES: " + configuration.getString(configKey));
         // Se guardará al archivo en el ShutdownHook

         // 6. Sincronizar la Interfaz de Usuario (Botón Toggle)
         //    El JCheckBoxMenuItem se actualiza automáticamente por la Action.
         logger.debug("    4. Sincronizando UI (Botón)...");
         if (this.configAppManager != null) {
             this.configAppManager.actualizarAspectoBotonToggle(toggleProporcionesAction, mantener);
         } else {
             logger.warn("WARN [setMantenerProporcionesAndUpdateConfig]: configAppManager es nulo, no se puede actualizar el botón toggle.");
         }

         // 7. Repintar la Imagen Principal para aplicar el nuevo modo de escalado
         //    Llamamos a reescalarImagenParaAjustar que ahora usará el nuevo valor
         //    de model.isMantenerProporcion() y luego actualizamos la vista.
         logger.debug("    5. Programando repintado de imagen principal en EDT...");
         SwingUtilities.invokeLater(() -> {
             // Verificar que la vista y el modelo sigan disponibles
             if (view == null || model == null) {
                 logger.error("ERROR [EDT Repintar Proporciones]: Vista o Modelo nulos.");
                 return;
             }
             logger.debug("      -> [EDT] Llamando a ZoomManager para refrescar con nueva proporción...");
             
             if (this.zoomManager != null) {

            	 this.zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
                 
             } else {
                 logger.error("ERROR [setMantenerProporciones EDT]: ZoomManager es null.");
                 refrescarManualmenteLaVistaPrincipal(); // Fallback
             }
         });

         logger.debug("[Controller setMantenerProporciones] FIN (Cambio aplicado y repintado programado).");
     } // --- FIN setMantenerProporcionesAndUpdateConfig ---
     

    /**
     * Cambia el tema visual actual de la aplicación.
     * 
     * Pasos:
     * 1. Valida las dependencias (ThemeManager) y el nombre del nuevo tema.
     * 2. Llama a themeManager.setTemaActual() para:
     *    a) Cambiar el objeto Tema actual en memoria.
     *    b) Actualizar la clave 'tema.nombre' en el ConfigurationManager en memoria.
     * 3. Si el tema realmente cambió (setTemaActual devuelve true):
     *    a) Itera sobre la lista 'themeActions' (que contiene las Actions de los radios del menú de tema).
     *    b) Llama a actualizarEstadoSeleccion() en cada ToggleThemeAction para que
     *       el radio button correspondiente al nuevo tema quede marcado como seleccionado.
     *    c) Muestra un JOptionPane informando al usuario que el cambio de tema
     *       requiere reiniciar la aplicación para que los cambios visuales (colores, etc.)
     *       tengan efecto completo, ya que muchos colores se aplican durante la inicialización
     *       de la UI.
     *
     * Nota: Este método NO guarda la configuración en el archivo inmediatamente. El guardado
     * ocurrirá a través del ShutdownHook al cerrar la aplicación.
     *
     * @param nuevoTema El nombre interno del nuevo tema a aplicar (ej. "dark", "clear", "blue").
     */
     public void cambiarTemaYNotificar(String nuevoTema) {
         if (themeManager == null) {
             logger.error("ERROR [cambiarTemaYNotificar]: ThemeManager es nulo.");
             return;
         }
         if (nuevoTema == null || nuevoTema.trim().isEmpty()) {
             logger.error("ERROR [cambiarTemaYNotificar]: El nombre del nuevo tema no puede ser nulo o vacío.");
             return;
         }
         String temaLimpio = nuevoTema.trim().toLowerCase();
         logger.debug("[VisorController] Solicitud para cambiar tema a: " + temaLimpio);

         // Delegar al ThemeManager.
         // ThemeManager internamente:
         // 1. Cambia su temaActual.
         // 2. Actualiza ConfigurationManager.
         // 3. Llama a this.sincronizarEstadoDeTodasLasToggleThemeActions() (donde 'this' es VisorController).
         // 4. Muestra el JOptionPane.
         themeManager.setTemaActual(temaLimpio, true); 

         logger.debug("[VisorController] Fin cambiarTemaYNotificar.");
         
     }// FIN del metodo cambiarTemaYNotificar
       

     /**
     * Sincroniza explícitamente el estado visual de los JCheckBoxMenuItems que controlan
     * la visibilidad de los botones de la toolbar.
     * Se asegura de que su estado "seleccionado" coincida con la configuración de visibilidad
     * del botón que controlan. Se debe llamar después de que toda la UI haya sido construida.
     */
    public void sincronizarEstadoVisualCheckboxesDeBotones() {
        logger.debug("[VisorController] Sincronizando estado visual de Checkboxes de visibilidad de botones...");
        
        // Validaciones para evitar NullPointerException
        if (this.menuItemsPorNombre == null || configuration == null) {
            logger.warn("  WARN: No se puede sincronizar, el mapa de menús o la configuración son nulos.");
            return;
        }

        Map<String, JMenuItem> menuItems = this.menuItemsPorNombre;
        
        // Iteramos sobre todos los items de menú que hemos creado y mapeado.
        for (Map.Entry<String, JMenuItem> entry : menuItems.entrySet()) {
            JMenuItem item = entry.getValue();
            
            // Nos interesan solo los que son JCheckBoxMenuItem y cuya Action es del tipo correcto.
            if (item instanceof JCheckBoxMenuItem && item.getAction() instanceof controlador.actions.config.ToggleToolbarButtonVisibilityAction) {
                JCheckBoxMenuItem checkbox = (JCheckBoxMenuItem) item;
                controlador.actions.config.ToggleToolbarButtonVisibilityAction action = (controlador.actions.config.ToggleToolbarButtonVisibilityAction) item.getAction();
                
                // Usamos el nuevo getter para obtener la clave de visibilidad del botón.
                String buttonVisibilityKey = action.getButtonVisibilityKey();

                if (buttonVisibilityKey != null) {
                    // Leemos el estado REAL que debería tener el checkbox.
                    boolean estadoCorrecto = configuration.getBoolean(buttonVisibilityKey, true);
                    
                    // Si el estado visual actual del checkbox no coincide, lo forzamos.
                    if (checkbox.isSelected() != estadoCorrecto) {
                        logger.debug("  -> CORRIGIENDO estado para '" + checkbox.getText().trim() + "'. Debería ser: " + estadoCorrecto + " (Estaba: " + checkbox.isSelected() + ")");
                        checkbox.setSelected(estadoCorrecto);
                    }
                }
            }
        }
        logger.debug("[VisorController] Sincronización de checkboxes de visibilidad finalizada.");
    } // --- FIN del método sincronizarEstadoVisualCheckboxesDeBotones ---
    

    private void cargarVisorNormal() {
	    String folderInit = configuration.getString("inicio.carpeta", "");
	    Path folderPath = null;
	    boolean carpetaValida = false;

	    if (!folderInit.isEmpty()) {
	        try {
	            folderPath = Paths.get(folderInit);
	            if (Files.isDirectory(folderPath)) {
	                carpetaValida = true;
	                this.model.setCarpetaRaizActual(folderPath);
	            }
	        } catch (Exception e) {
	            logger.warn("Ruta de carpeta inicial inválida: {}", folderInit);
	        }
	    }

	    if (carpetaValida) {
	        String imagenInicialKey = configuration.getString("inicio.imagen", null);
	        imageListManager.cargarListaImagenes(imagenInicialKey, null);
	    } else {
	        SwingUtilities.invokeLater(viewManager::limpiarUI);
	    }
	} // ---FIN de metodo cargarVisorNormal---
  
  
	/**
	 * Calcula dinámicamente el número de miniaturas a mostrar antes y después de la
	 * miniatura central, basándose en el ancho disponible del viewport del
	 * JScrollPane de miniaturas y el ancho de cada celda de miniatura. Respeta los
	 * máximos configurados por el usuario.
	 *
	 * @return Un objeto RangoMiniaturasCalculado con los valores 'antes' y
	 *         'despues'.
	 */
	public RangoMiniaturasCalculado calcularNumMiniaturasDinamicas(){
		// --- 1. OBTENER LÍMITES SUPERIORES DE CONFIGURACIÓN/MODELO (sin cambios) ---
		int cfgMiniaturasAntes, cfgMiniaturasDespues;

		if (model != null){
			cfgMiniaturasAntes = model.getMiniaturasAntes();
			cfgMiniaturasDespues = model.getMiniaturasDespues();
		} else if (configuration != null){
			
			cfgMiniaturasAntes = configuration.getInt("miniaturas.cantidad.antes", DEFAULT_MINIATURAS_ANTES_FALLBACK);
			cfgMiniaturasDespues = configuration.getInt("miniaturas.cantidad.despues",
					DEFAULT_MINIATURAS_DESPUES_FALLBACK);
			logger.warn("  [CalcularMiniaturas] WARN: Modelo nulo, usando valores de config/fallback.");
		} else{
			
			cfgMiniaturasAntes = DEFAULT_MINIATURAS_ANTES_FALLBACK;
			cfgMiniaturasDespues = DEFAULT_MINIATURAS_DESPUES_FALLBACK;
			logger.error("  [CalcularMiniaturas] ERROR: Modelo y Config nulos, usando fallbacks.");
		}

		// --- 2. OBTENER COMPONENTES DE LA VISTA DESDE EL REGISTRO ---
		JScrollPane scrollPane = registry.get("scroll.miniaturas");
		JList<String> listaMin = registry.get("list.miniaturas");

		// --- 3. VALIDAR DISPONIBILIDAD DE COMPONENTES ---
		if (scrollPane == null || listaMin == null){
			
			logger.warn(
					"  [CalcularMiniaturas] WARN: ScrollPane o JList de miniaturas nulos en registro. Devolviendo máximos configurados.");
			return new RangoMiniaturasCalculado(cfgMiniaturasAntes, cfgMiniaturasDespues);
		}

		// --- 4. OBTENER DIMENSIONES ACTUALES DE LA UI ---
		int viewportWidth = scrollPane.getViewport().getWidth();
		int cellWidth = listaMin.getFixedCellWidth();

		// Log de depuración
		// logger.debug(" [CalcularMiniaturas DEBUG] ViewportWidth: " + ...);

		// --- 5. LÓGICA DE FALLBACK MEJORADA ---
		if (viewportWidth <= 0 || cellWidth <= 0 || !scrollPane.isShowing())
		{
			logger.warn(
					"  [CalcularMiniaturas] WARN: Viewport/Cell inválido o ScrollPane no visible. Usando MÁXIMOS configurados como fallback.");
			return new RangoMiniaturasCalculado(cfgMiniaturasAntes, cfgMiniaturasDespues);
		}

		// --- 6. CÁLCULO Y DISTRIBUCIÓN (sin cambios) ---
		int totalMiniaturasQueCaben = viewportWidth / cellWidth;
		int numAntesCalculado;
		int numDespuesCalculado;
		int maxTotalConfigurado = cfgMiniaturasAntes + 1 + cfgMiniaturasDespues;

		if (totalMiniaturasQueCaben >= maxTotalConfigurado){
			numAntesCalculado = cfgMiniaturasAntes;
			numDespuesCalculado = cfgMiniaturasDespues;
			
		} else if (totalMiniaturasQueCaben <= 1){
			numAntesCalculado = 0;
			numDespuesCalculado = 0;
		} else{
			int miniaturasLateralesDisponibles = totalMiniaturasQueCaben - 1;
			double ratioAntesOriginal = (cfgMiniaturasAntes + cfgMiniaturasDespues > 0)
					? (double) cfgMiniaturasAntes / (cfgMiniaturasAntes + cfgMiniaturasDespues)
					: 0.5;
			numAntesCalculado = (int) Math.round(miniaturasLateralesDisponibles * ratioAntesOriginal);
			numDespuesCalculado = miniaturasLateralesDisponibles - numAntesCalculado;
			numAntesCalculado = Math.min(numAntesCalculado, cfgMiniaturasAntes);
			numDespuesCalculado = Math.min(numDespuesCalculado, cfgMiniaturasDespues);
		}

		// --- 7. DEVOLVER EL RESULTADO CALCULADO ---
		logger.debug("  [CalcularMiniaturas] Rango dinámico calculado -> Antes: " + numAntesCalculado
				+ ", Despues: " + numDespuesCalculado);
		return new RangoMiniaturasCalculado(numAntesCalculado, numDespuesCalculado);
	}// --- FIN del metodo calcularNumMiniaturasDinamicas ---
     
     
// ***************************************************************************** FIN METODOS DE MOVIMIENTO CON LISTCOORDINATOR
// ***************************************************************************************************************************

// ****************************************************************************************************** GESTION DE PROYECTOS
// ***************************************************************************************************************************	  
	  
	 
	/**
	 * Actualiza el estado visual de los componentes relacionados con la marca de
	 * proyecto. Este método se asegura de que el estado de la Action, el botón de
	 * la toolbar y la barra de estado reflejen si la imagen actual está marcada o
	 * no.
	 *
	 * @param estaMarcada         true si la imagen está marcada, false en caso
	 *                            contrario.
	 * @param rutaParaBarraEstado La ruta de la imagen, para mostrar en la barra de
	 *                            estado (puede ser null).
	 */
	public void actualizarEstadoVisualBotonMarcarYBarraEstado(boolean estaMarcada, Path rutaParaBarraEstado)
	{
		if (actionMap == null)
			return;

		// --- 1. Sincronizar la Action ---
		Action toggleMarkImageAction = actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);

		if (toggleMarkImageAction != null)
		{
			toggleMarkImageAction.putValue(Action.SELECTED_KEY, estaMarcada);

			// --- 2. Lógica de pintado correcta y directa ---
			if (configAppManager != null)
			{
				configAppManager.actualizarAspectoBotonToggle(toggleMarkImageAction, estaMarcada);
			}
		}

		// --- 3. Actualizar la Barra de Estado ---
		if (statusBarManager != null)
		{
			statusBarManager.actualizar();
		}

		logger.debug("  [Controller] Estado visual de 'Marcar' actualizado. Marcada: " + estaMarcada);
	} // --- Fin del método actualizarEstadoVisualBotonMarcarYBarraEstado ---
     
	
	public void solicitudAlternarMarcaDeImagenActual() {
	    logger.debug("[Controller] Solicitud para alternar marca de imagen actual...");
	    if (model == null || projectManager == null || view == null) { return; }
	    
	    String claveActual = model.getSelectedImageKey();
	    if (claveActual == null || claveActual.isEmpty()) { return; }

	    Path rutaAbsoluta = model.getRutaCompleta(claveActual);
	    if (rutaAbsoluta == null) { return; }

	    // 1. Modificar el modelo en memoria
	    boolean estaAhoraMarcada = projectManager.alternarMarcaImagen(rutaAbsoluta);
	    
	    // 2. Notificar que ha habido un cambio. Esto hará que aparezca el *.
	    projectManager.notificarModificacion();

	    // 3. Actualizar la UI (botón y barra de estado)
	    actualizarEstadoVisualBotonMarcarYBarraEstado(estaAhoraMarcada, rutaAbsoluta);
	} // --- Fin del método solicitudAlternarMarcaDeImagenActual ---
     
	
	/**
     * Restaura toda la UI específica del modo VISUALIZADOR, leyendo el estado
     * desde el contexto correspondiente en el VisorModel.
     */
	public void restaurarUiVisualizador() {
        logger.debug("    -> Restaurando UI para el modo VISUALIZADOR...");
        
        JList<String> listaNombres = registry.get("list.nombresArchivo");
        DefaultListModel<String> modeloVisualizador = model.getModeloLista(); // Este es el ListModel del visualizadorListContext

        // Deshabilitar temporalmente el ListSelectionListener para evitar que se dispare
        // cuando JList.setModel() limpia internamente la selección.
        // Capturar los listeners existentes para poder re-añadirlos.
        javax.swing.event.ListSelectionListener[] listeners = null;
        if (listaNombres != null) {
            listeners = listaNombres.getListSelectionListeners();
            for (javax.swing.event.ListSelectionListener l : listeners) {
                listaNombres.removeListSelectionListener(l);
            }
        }
        
        if (listaNombres != null) {
            listaNombres.setModel(modeloVisualizador); // Asignar el modelo de la lista del visualizador
            logger.debug("      -> Modelo de lista del visualizador restaurado en la JList. Tamaño: " + modeloVisualizador.getSize());
        }

        // Re-añadir los listeners después de setModel()
        if (listaNombres != null && listeners != null) {
            for (javax.swing.event.ListSelectionListener l : listeners) {
                listaNombres.addListSelectionListener(l);
            }
        }

        JPanel panelIzquierdo = registry.get("panel.izquierdo.listaArchivos");
        if(panelIzquierdo != null && panelIzquierdo.getBorder() instanceof javax.swing.border.TitledBorder) {
            ((javax.swing.border.TitledBorder)panelIzquierdo.getBorder()).setTitle("Archivos: " + modeloVisualizador.getSize());
            panelIzquierdo.repaint();
        }
        
        String claveGuardada = model.getSelectedImageKey();
        logger.debug("### DEBUG: Restaurando selección. Clave guardada en contexto VISUALIZADOR: '" + claveGuardada + "'");
        int indiceARestaurar = (claveGuardada != null) ? modeloVisualizador.indexOf(claveGuardada) : -1;
        
        // La lógica de fallback a 0 es correcta si la clave no se encuentra o no hay nada seleccionado.
        if (indiceARestaurar == -1 && !modeloVisualizador.isEmpty()) {
            indiceARestaurar = 0;
        }

        // Llamar al coordinador para establecer la selección y disparar la carga de la imagen.
        // Aquí NO necesitamos limpiar la selección de la JList de nombres de nuevo,
        // ya lo hicimos indirectamente con setModel(), y el ListCoordinator.reiniciarYSeleccionarIndice
        // ya tiene lógica para limpiar y luego seleccionar.
        if (listCoordinator != null) {
            listCoordinator.reiniciarYSeleccionarIndice(indiceARestaurar);
        } else {
             logger.error("ERROR: ListCoordinator es null al restaurar UI del visualizador.");
        }
        
    } // --- Fin del método restaurarUiVisualizador ---
	
	
	/**
     * Restaura la UI específica del modo CARRUSEL.
     * Este método asume que el model.carouselListContext ya ha sido
     * cargado (ya sea clonado o restaurado). Su trabajo es reflejar
     * ese estado en la UI.
     */
    public void restaurarUiCarrusel() {
        logger.debug("    -> Restaurando UI para el modo CARRUSEL...");
        
        // 1. Obtener la clave de la imagen que debe estar seleccionada desde el contexto del carrusel.
        String claveGuardada = model.getSelectedImageKey(); // getCurrentListContext() devolverá el del carrusel.
        logger.debug("      -> Clave guardada en contexto CARRUSEL: '" + claveGuardada + "'");
        
        // 2. Encontrar el índice de esa clave en el modelo de lista del carrusel.
        int indiceARestaurar = (claveGuardada != null) ? model.getModeloLista().indexOf(claveGuardada) : -1;
        
        // 3. Lógica de fallback: si no hay clave o no se encuentra, seleccionar la primera imagen.
        if (indiceARestaurar == -1 && !model.getModeloLista().isEmpty()) {
            indiceARestaurar = 0;
        }

        // 4. Delegar al ListCoordinator para que haga la selección.
        //    Esto cargará la imagen principal, actualizará las miniaturas, etc.
        if (listCoordinator != null) {
            // Usamos reiniciarYSeleccionarIndice para asegurar un estado limpio.
            listCoordinator.reiniciarYSeleccionarIndice(indiceARestaurar);
        } else {
             logger.error("ERROR: ListCoordinator es nulo al restaurar UI del carrusel.");
        }
        
    } // --- Fin del método restaurarUiCarrusel ---
    
    
    /**
     * Crea y muestra un menú emergente para seleccionar la velocidad del carrusel.
     * Este método es llamado por el MouseListener del JLabel de velocidad.
     *
     * @param invoker El componente (el JLabel) sobre el cual se mostrará el menú.
     */
    public void showCarouselSpeedMenu(java.awt.Component invoker) {
        JPopupMenu speedMenu = new JPopupMenu();

        java.util.Map<String, Integer> speedOptions = new java.util.LinkedHashMap<>();
        speedOptions.put("Muy Rápido (1.0s)", 1000);
        speedOptions.put("Rápido (2.0s)", 2000);
        speedOptions.put("Normal (5.0s)", 5000);
        speedOptions.put("Lento (10.0s)", 10000);
        speedOptions.put("Muy Lento (20.0s)", 20000);

        CarouselManager carouselManager = getActionFactory().getCarouselManager();

        for (java.util.Map.Entry<String, Integer> entry : speedOptions.entrySet()) {
            String text = entry.getKey();
            int delayMs = entry.getValue();
            Action setSpeedAction = new controlador.actions.carousel.SetCarouselSpeedAction(
                getModel(), carouselManager, text, delayMs);
            speedMenu.add(new javax.swing.JMenuItem(setSpeedAction));
        }

        speedMenu.addSeparator(); // Un separador para distinguir la opción especial

        Action setReverseSpeedAction = new controlador.actions.carousel.SetCarouselSpeedAction(
            getModel(),
            carouselManager,
            "Velocidad Inversa (-5.0s)",
            -5000 // <-- Le pasamos un valor negativo
        );
        speedMenu.add(new javax.swing.JMenuItem(setReverseSpeedAction));

        speedMenu.show(invoker, 0, -speedMenu.getPreferredSize().height);
    } // --- Fin del método showCarouselSpeedMenu ---
    
	
// ************************************************************************************************** FIN GESTION DE PROYECTOS
// ***************************************************************************************************************************
	  
	  
// ********************************************************************************************************** METODOS DE AYUDA
// ***************************************************************************************************************************	
	
	public void mostrarDialogoAyudaAtajos() {

	    // --- PANEL PRINCIPAL Y LAYOUT ---
	    JPanel panelPrincipal = new JPanel(new GridBagLayout());
	    panelPrincipal.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
	    GridBagConstraints gbc = new GridBagConstraints();

	    // --- TÍTULO PRINCIPAL ---
	    JLabel titulo = new JLabel("Atajos de Teclado");
	    titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));
	    titulo.putClientProperty("FlatLaf.style", "font: bold $h2.font"); // Estilo para FlatLaf
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.gridwidth = 2; // Ocupa las dos columnas
	    gbc.anchor = GridBagConstraints.CENTER;
	    gbc.insets = new Insets(0, 0, 20, 0);
	    panelPrincipal.add(titulo, gbc);

	    // --- COLUMNA 1 ---
	    gbc.gridwidth = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.NORTHWEST; // Alinear todo arriba a la izquierda
	    gbc.insets = new Insets(0, 0, 10, 20); // Espaciado: arriba, izq, abajo, derecha (espacio entre columnas)
	    
	    // Contenedor para la columna 1
	    JPanel columna1 = new JPanel();
	    columna1.setLayout(new BoxLayout(columna1, BoxLayout.Y_AXIS));
	    
	    // --- Sección Navegación General ---
	    columna1.add(createHelpSection("Navegación General", new String[][]{
	        {"Flechas / Rueda Ratón", "Imagen Siguiente/Anterior"},
	        {"Inicio / Fin", "Primera / Última Imagen"},
	        {"Av Pág / Re Pág", "Saltar bloque de imágenes"}
	    }));
	    columna1.add(Box.createVerticalStrut(15)); // Espacio entre secciones

	    // --- Sección Gestión ---
	    columna1.add(createHelpSection("Gestión y Archivos", new String[][]{
	        {"Barra Espaciadora", "Marcar / Desmarcar imagen"},
	        {"Ctrl + L", "Localizar archivo en el explorador"}
	    }));
	    columna1.add(Box.createVerticalStrut(15));
	    
	    // --- Sección Acciones de Proyecto ---
	    columna1.add(createHelpSection("Acciones de Proyecto", new String[][]{
	        {"Ctrl + S", "Guardar Proyecto Actual"},
	        {"Ctrl + Shift + S", "Guardar Proyecto Como..."},
	        {"Ctrl + N", "Nuevo Proyecto"},
	        {"Ctrl + O", "Abrir Proyecto..."}
	    }));

	    panelPrincipal.add(columna1, gbc);

	    // --- COLUMNA 2 ---
	    gbc.gridx = 1;
	    gbc.insets = new Insets(0, 20, 10, 0); // Espaciado: arriba, izq, abajo, derecha
	    
	    JPanel columna2 = new JPanel();
	    columna2.setLayout(new BoxLayout(columna2, BoxLayout.Y_AXIS));

	    // --- Sección Vista Global ---
	    columna2.add(createHelpSection("Ventana y Vista Global", new String[][]{
	        {"F5", "Refrescar vista actual"},
	        {"F11", "Modo Pantalla Completa"},
	        {"Alt", "Activar Foco en Menú"},
	        {"ESC", "Salir de Pantalla Completa"}
	    }));
	    columna2.add(Box.createVerticalStrut(15));

	    // --- Sección Zoom ---
	    columna2.add(createHelpSection("Zoom (Numérico y Paneo)", new String[][]{
	        {"1 / Rueda Ratón", "Ajustar / Zoom (manual)"},
	        {"2", "Tamaño Original (100%)"},
	        {"3", "Ajustar a Ancho"},
	        {"4", "Ajustar a Alto"},
	        {"8", "Activar/Desactivar Paneo"},
	        {"9", "Resetear Zoom"}
	    }));
	    columna2.add(Box.createVerticalStrut(15));

	    // --- Sección Grid ---
	    columna2.add(createHelpSection("Atajos del Grid", new String[][]{
	        {"Ctrl + T", "Añadir / Editar Etiqueta"},
	        {"Ctrl + Supr", "Borrar Etiqueta"},
	        {"Ctrl + Más (+)", "Aumentar Tamaño"},
	        {"Ctrl + Menos (-)", "Reducir Tamaño"}
	    }));

	    panelPrincipal.add(columna2, gbc);

	    // --- MOSTRAR EL DIÁLOGO ---
	    JOptionPane.showMessageDialog(
	        view,
	        panelPrincipal,
	        "Ayuda: Atajos de Teclado",
	        JOptionPane.INFORMATION_MESSAGE
	    );
	} // ---FIN de metodo [mostrarDialogoAyudaAtajos]---
	
	/**
	 * Método de ayuda para crear una sección de la ayuda (título + lista de atajos).
	 * @param title El título de la sección.
	 * @param shortcuts Un array de arrays de String, donde cada subarray es [tecla, descripción].
	 * @return Un JComponent que contiene la sección formateada.
	 */
	private JComponent createHelpSection(String title, String[][] shortcuts) {
	    JPanel sectionPanel = new JPanel();
	    sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.Y_AXIS));
	    sectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

	    JLabel titleLabel = new JLabel(title);
	    titleLabel.putClientProperty("FlatLaf.style", "font: bold $h3.font");
	    titleLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")));
	    titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    
	    JPanel shortcutsPanel = new JPanel(new GridBagLayout());
	    shortcutsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    GridBagConstraints gbc = new GridBagConstraints();
	    gbc.anchor = GridBagConstraints.WEST;
	    gbc.insets = new Insets(2, 0, 2, 5);
	    
	    for (int i = 0; i < shortcuts.length; i++) {
	        gbc.gridy = i;
	        
	        gbc.gridx = 0;
	        gbc.weightx = 0;
	        JLabel keyLabel = new JLabel("<html><b>" + shortcuts[i][0] + ":</b></html>");
	        shortcutsPanel.add(keyLabel, gbc);
	        
	        gbc.gridx = 1;
	        gbc.weightx = 1.0;
	        shortcutsPanel.add(new JLabel(shortcuts[i][1]), gbc);
	    }
	    
	    sectionPanel.add(titleLabel);
	    sectionPanel.add(Box.createVerticalStrut(5));
	    sectionPanel.add(shortcutsPanel);
	    
	    return sectionPanel;
	} // ---FIN de metodo [createHelpSection]---
	
	
// ****************************************************************************************************** FIN METODOS DE AYUDA
// ***************************************************************************************************************************
	
	
// ********************************************************************************************************* GETTERS Y SETTERS
// ***************************************************************************************************************************
	
	
	@Override
	public void onThemeChanged(Tema nuevoTema) {
	    logger.debug("\n--- [VisorController] ORQUESTANDO REFRESCO COMPLETO DE UI POR CAMBIO DE TEMA ---");

	    if (nuevoTema == null) {
	        logger.error("Se recibió un tema nulo en onThemeChanged. Abortando refresco.");
	        return;
	    }

	    // FASE 0: PREPARACIÓN Y LIMPIEZA ATÓMICA (FUERA DEL EDT)
	    if (this.actionFactory != null) {
	        this.actionFactory.actualizarIconosDeAcciones();
	    }
	    if (this.registry != null) {
	        this.registry.unregisterToolbarComponents();
	    }

	    // FASE 1: Reconstrucción y Sincronización en el Hilo de UI (EDT)
	    SwingUtilities.invokeLater(() -> {
	        logger.debug("  [EDT] Iniciando reconstrucción y sincronización para el tema '{}'...", nuevoTema.nombreDisplay());

	        // 1. Reconstruir las Toolbars.
	        if (this.toolbarManager != null && this.model != null) {
	            this.toolbarManager.reconstruirContenedorDeToolbars(this.model.getCurrentWorkMode());
	        }

	        if (this.viewManager != null) {
	            this.viewManager.reconstruirPanelesEspecialesTrasTema();
	        }

	        // 2. Volver a llamar a updateComponentTreeUI para asegurar que los nuevos componentes
	        //    y los existentes se actualicen al LookAndFeel base.
	        if (this.view != null) {
	            SwingUtilities.updateComponentTreeUI(this.view);
	        }

	        // 3. AHORA, DESPUÉS del reseteo de Swing, aplicamos nuestros colores personalizados.
	        //    Esto sobreescribe los colores por defecto de la UI solo donde queremos.
	        this.viewManager.sincronizarColoresDePanelesPorTema(nuevoTema);

	        // 4. Sincronizar el estado lógico (botones seleccionados, etc.)
	        logger.debug("  [EDT] Sincronizando estado de controles...");
	        zoomManager.sincronizarEstadoVisualBotonesYRadiosZoom();
	        sincronizarComponentesDeModoVisualizador();
	        sincronizarEstadoDeTodasLasToggleThemeActions();

	        if (this.statusBarManager != null) {
	            this.statusBarManager.actualizar();
	        }

	        // 5. Forzar un revalidate/repaint final.
	        if (this.view != null) {
	            this.view.revalidate();
	            this.view.repaint();
	        }

	        logger.debug("--- [VisorController] REFRESCO DE UI COMPLETADO ---\n");
	    });

	} // --- Fin del método onThemeChanged ---
	
    /**
     * Devuelve el número actual de elementos (imágenes) en el modelo de la lista principal.
     * Es un método seguro que comprueba la existencia del modelo y su lista interna.
     *
     * @return El tamaño (número de elementos) de la lista de imágenes,
     *         o 0 si el modelo o la lista no están inicializados o están vacíos.
     */
    public int getTamanioListaImagenes() {
        // 1. Verificar que el modelo principal ('model') no sea null
        if (model != null) {
            // 2. Obtener el DefaultListModel interno del modelo principal
            DefaultListModel<String> modeloLista = model.getModeloLista();
            // 3. Verificar que el DefaultListModel obtenido no sea null
            if (modeloLista != null) {
                // 4. Devolver el tamaño del modelo de lista
                return modeloLista.getSize();
            } else {
                // Log si el modelo interno es null (inesperado si el modelo principal no es null)
                logger.warn("WARN [getTamanioListaImagenes]: El modelo interno (modeloLista) es null.");
                return 0;
            }
        } else {
            // Log si el modelo principal es null
            logger.warn("WARN [getTamanioListaImagenes]: El modelo principal (model) es null.");
            return 0; // Devuelve 0 si el modelo principal no está listo
        }
    } // --- FIN getTamanioListaImagenes ---	
    
    public int getCalculatedMiniaturePanelHeight() { return calculatedMiniaturePanelHeight; }
    
    public Map<String, Action> getActionMap() {return this.actionMap;}
    public Map<String, AbstractButton> getBotonesPorNombre() {return this.botonesPorNombre;}
    public ExecutorService getExecutorService() {return this.executorService;}
    public IViewManager getViewManager() {return this.viewManager;}
    public IListCoordinator getListCoordinator() {return this.listCoordinator;}
    public IZoomManager getZoomManager() {return this.zoomManager;}
    
    public ViewUIConfig getUiConfigForView() { return uiConfigForView; }
	public ThumbnailService getServicioMiniaturas() { return servicioMiniaturas; }
	public IconUtils getIconUtils() { return iconUtils; } 
    public ComponentRegistry getComponentRegistry() {return this.registry;}
    public ToolbarManager getToolbarManager() {return this.toolbarManager;}
    public VisorModel getModel() { return model; }
    public VisorView getView() { return view; }
    public ConfigurationManager getConfigurationManager() { return configuration; }
    public ConfigApplicationManager getConfigApplicationManager() { return this.configAppManager; }
    public ProjectManager getProjectManager() {return this.projectManager;}
    public ActionFactory getActionFactory() {return this.actionFactory;}
    public ThemeManager getThemeManager() {return this.themeManager;}
    public DefaultListModel<String> getModeloMiniaturasVisualizador() {return this.modeloMiniaturasVisualizador;}
    public DefaultListModel<String> getModeloMiniaturasCarrusel() {return this.modeloMiniaturasCarrusel;}
    public DisplayModeManager getDisplayModeManager() {return this.displayModeManager;}
    public InfobarStatusManager getStatusBarManager() {return this.statusBarManager;}
    public GeneralController getGeneralController() {return this.generalController;}
    public ImageListManager getImageListManager() { return this.imageListManager; }
    public InfobarImageManager getInfobarImageManager() { return this.infobarImageManager; }
    public String getVersion() {return this.version;}
    
    /**
     * Devuelve el modelo de lista de miniaturas correcto según el modo de trabajo actual.
     * @return El DefaultListModel para el modo Visualizador o Carrusel.
     */
    public DefaultListModel<String> getModeloMiniaturas() {
        if (model != null && model.getCurrentWorkMode() == WorkMode.CARROUSEL) {
            return this.modeloMiniaturasCarrusel;
        }
        // Por defecto, o si el modo es Visualizador, devuelve el del visualizador.
        return this.modeloMiniaturasVisualizador;
    } // --- Fin del método getModeloMiniaturas ---
    
    /**
     * Establece si se deben mostrar los nombres de archivo debajo de las miniaturas
     * y refresca el renderer para que el cambio visual sea inmediato.
     *
     * @param mostrar El nuevo estado deseado: true para mostrar nombres, false para ocultarlos.
     */
    public void setMostrarNombresMiniaturas(boolean mostrar) {
        logger.debug("[VisorController] Solicitud para cambiar 'Mostrar Nombres en Miniaturas' a: " + mostrar);

        // --- 1. VALIDACIÓN DE DEPENDENCIAS ESENCIALES ---
        if (configuration == null || view == null || registry.get("list.miniaturas") == null || this.model == null ||
            this.servicioMiniaturas == null || this.themeManager == null || this.iconUtils == null) {
            logger.error("ERROR CRÍTICO [setMostrarNombresMiniaturas]: Faltan dependencias esenciales (config, view, model, etc.). Operación cancelada.");
            return;
        }

        // --- 2. ACTUALIZAR LA CONFIGURACIÓN PERSISTENTE ---
        configuration.setString(ConfigKeys.VISTA_MOSTRAR_NOMBRES_MINIATURAS_STATE, String.valueOf(mostrar));
        logger.debug("  -> Configuración '" + ConfigKeys.VISTA_MOSTRAR_NOMBRES_MINIATURAS_STATE + "' actualizada en memoria a: " + mostrar);

        // --- 3. RECREAR Y APLICAR EL RENDERER DE MINIATURAS EN LA VISTA ---
        logger.debug("  -> Preparando para recrear y asignar nuevo MiniaturaListCellRenderer...");

        // 3.1. Obtener solo las dimensiones (los colores ya no son necesarios aquí).
        int thumbWidth = configuration.getInt("miniaturas.tamano.normal.ancho", 40);
        int thumbHeight = configuration.getInt("miniaturas.tamano.normal.alto", 40);

        // 3.2. Crear la nueva instancia del renderer usando el CONSTRUCTOR MODERNO.
        MiniaturaListCellRenderer newRenderer = new MiniaturaListCellRenderer(
            this.servicioMiniaturas,
            this.model,
            this.themeManager,         // <--- Le pasamos el ThemeManager
            this.iconUtils,            // <--- Le pasamos el IconUtils
            thumbWidth,
            thumbHeight,
            mostrar                    // <--- Le pasamos el flag de comportamiento
        );
        logger.debug("    -> Nueva instancia de MiniaturaListCellRenderer creada con el constructor moderno.");

        // 3.3. Asignar el nuevo renderer a la JList (sin cambios en esta parte).
        final MiniaturaListCellRenderer finalRenderer = newRenderer;
        SwingUtilities.invokeLater(() -> {
            JList<String> listaMin = registry.get("list.miniaturas");
            listaMin.setCellRenderer(finalRenderer);
            listaMin.setFixedCellHeight(finalRenderer.getAlturaCalculadaDeCelda());
            listaMin.setFixedCellWidth(finalRenderer.getAnchoCalculadaDeCelda());
            listaMin.revalidate();
            listaMin.repaint();
            logger.debug("      [EDT] Nuevo renderer asignado y lista de miniaturas actualizada.");

            // ... (el resto de tu lógica de actualización del listCoordinator se mantiene)
        });

        logger.debug("[VisorController] setMostrarNombresMiniaturas completado.");
    }// --- FIN del metodo setMostrarNombresMiniaturas ---
    
    /**
     * Método centralizado para cambiar el estado de "Navegación Circular".
     * Actualiza el modelo, la configuración, y sincroniza la UI (incluyendo el
     * JCheckBoxMenuItem asociado y los botones de navegación).
     * Este método es llamado por ToggleNavegacionCircularAction.
     * @param nuevoEstadoCircular El nuevo estado deseado para la navegación circular.
     */
    public void setNavegacionCircularLogicaYUi(boolean nuevoEstadoCircular) {
        logger.debug("[VisorController setNavegacionCircularLogicaYUi] Nuevo estado deseado: " + nuevoEstadoCircular);

        if (model == null || configuration == null || actionMap == null || getListCoordinator() == null) {
            logger.error("  ERROR [VisorController setNavegacionCircularLogicaYUi]: Dependencias nulas. Abortando.");
            return;
        }

        // 1. Actualizar el VisorModel
        model.setNavegacionCircularActivada(nuevoEstadoCircular);
        // El modelo ya imprime su log: "[VisorModel] Navegación Circular cambiada a: ..."

        // 2. Actualizar ConfigurationManager (en memoria)
        String configKey = "comportamiento.navegacion.circular"; // La clave que usa la Action
        configuration.setString(configKey, String.valueOf(nuevoEstadoCircular));
        logger.debug("  -> Configuración '" + configKey + "' actualizada en memoria a: " + nuevoEstadoCircular);

        // 3. Sincronizar la UI:
        //    a) El JCheckBoxMenuItem asociado a la Action
        Action toggleAction = actionMap.get(AppActionCommands.CMD_TOGGLE_WRAP_AROUND);
        if (toggleAction != null) {
            // Comprobar si el estado de la Action ya es el correcto, para evitar ciclos si algo más lo cambió.
            // Aunque en este flujo, nosotros somos la fuente del cambio.
            if (!Boolean.valueOf(nuevoEstadoCircular).equals(toggleAction.getValue(Action.SELECTED_KEY))) {
                toggleAction.putValue(Action.SELECTED_KEY, nuevoEstadoCircular);
                logger.debug("    -> Action.SELECTED_KEY para CMD_TOGGLE_WRAP_AROUND actualizado a: " + nuevoEstadoCircular);
            }
        } else {
            logger.warn("WARN [VisorController setNavegacionCircularLogicaYUi]: No se encontró Action para CMD_TOGGLE_WRAP_AROUND.");
        }

        //    b) El estado de los botones de navegación
        getListCoordinator().forzarActualizacionEstadoAcciones();
        logger.debug("    -> Forzada actualización del estado de botones de navegación.");

        logger.debug("[VisorController setNavegacionCircularLogicaYUi] Proceso completado.");
        
    }// --- FIN del metodo setNavegacionCircularLogicaYUi ---
     
     
    /**
     * Método centralizado para cambiar el estado de "Mantener Proporciones".
     * Actualiza el modelo, la configuración, refresca la imagen y sincroniza la UI.
     * Este método es llamado por ToggleProporcionesAction.
     * @param nuevoEstadoMantener El nuevo estado deseado para mantener proporciones.
     */
    public void setMantenerProporcionesLogicaYUi(boolean nuevoEstadoMantener) { 
 	    logger.debug("[VisorController setMantenerProporcionesLogicaYUi] Nuevo estado deseado: " + nuevoEstadoMantener);
 	
 	    if (model == null || configuration == null || zoomManager == null || actionMap == null || view == null) {
 	        logger.error("  ERROR [VisorController setMantenerProporcionesLogicaYUi]: Dependencias nulas. Abortando.");
 	        return;
 	    }
 	
 	    // 1. Actualizar VisorModel
 	    model.setMantenerProporcion(nuevoEstadoMantener); 
 	
 	    // 2. Actualizar ConfigurationManager (en memoria)
 	    String configKey = "interfaz.menu.zoom.mantener_proporciones.seleccionado";
 	    configuration.setString(configKey, String.valueOf(nuevoEstadoMantener));
 	    logger.debug("  -> Configuración '" + configKey + "' actualizada en memoria a: " + nuevoEstadoMantener);
 	
        // 3. Sincronizar la lógica de la Action y la apariencia del botón
 	    Action toggleAction = actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);
        if (toggleAction != null) {
            toggleAction.putValue(Action.SELECTED_KEY, nuevoEstadoMantener);
            
            if (configAppManager != null) {
                configAppManager.actualizarAspectoBotonToggle(toggleAction, nuevoEstadoMantener);
            }
        }
 	
 	    // 4. Refrescar la imagen principal en la vista
 	    logger.debug("  -> Solicitando a ZoomManager que refresque la imagen principal...");
 	     
 	    if (this.zoomManager != null && this.model != null && this.model.getCurrentZoomMode() != null) {
 	        this.zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
 	        zoomManager.sincronizarEstadoVisualBotonesYRadiosZoom(); 
 	    } else if (this.zoomManager != null) { 
 	    	this.zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
 	    } else {
 	        logger.error("ERROR [setMantenerProporcionesLogicaYUi]: ZoomManager es null al intentar refrescar.");
 	    }
 	     
 	     if (infobarImageManager != null) {
 	    	 infobarImageManager.actualizar();
 	     }
 	     if (statusBarManager != null) {
 	    	 statusBarManager.actualizar();
 	     }	     
 	     
 	     logger.debug("[VisorController setMantenerProporcionesLogicaYUi] Proceso completado.");
 	} //--- FIN del metodo setMantenerProporcionesLogicaYUi ---
    
 	
 	public void setCalculatedMiniaturePanelHeight(int calculatedMiniaturePanelHeight) { this.calculatedMiniaturePanelHeight = calculatedMiniaturePanelHeight; }
 	public void setVersion					(String version) { this.version = version;} 
 	public void setModel					(VisorModel model) { this.model = model; }
	public void setConfigurationManager		(ConfigurationManager configuration) { this.configuration = configuration; }
	public void setThemeManager				(ThemeManager themeManager) { this.themeManager = themeManager; }
	public void setIconUtils				(IconUtils iconUtils) { this.iconUtils = iconUtils; }
	public void setServicioMiniaturas		(ThumbnailService servicioMiniaturas) { this.servicioMiniaturas = servicioMiniaturas; }
	public void setExecutorService			(ExecutorService executorService) { this.executorService = executorService; }
	public void setActionMap				(Map<String, Action> actionMap) { this.actionMap = actionMap; }
	public void setUiConfigForView			(ViewUIConfig uiConfigForView) { this.uiConfigForView = uiConfigForView; }
	public void setView						(VisorView view) { this.view = view; }
	public void setListCoordinator			(IListCoordinator listCoordinator) { this.listCoordinator = listCoordinator; }
 	public void setProjectManager			(ProjectManager projectManager) {this.projectManager = projectManager;}
    public void setActionFactory			(ActionFactory actionFactory) {this.actionFactory = actionFactory;}
    public void setZoomManager				(IZoomManager zoomManager) {this.zoomManager = zoomManager;}	
    public void setInfobarImageManager		(InfobarImageManager manager) { this.infobarImageManager = manager; }
    public void setStatusBarManager			(InfobarStatusManager manager) { this.statusBarManager = manager; }
    public void setComponentRegistry		(ComponentRegistry registry) {this.registry = registry;}
    public void setBotonesPorNombre			(Map<String, AbstractButton> botones) {this.botonesPorNombre = (botones != null) ? botones : new HashMap<>();}
    public void setMenuItemsPorNombre		(Map<String, JMenuItem> menuItems) {this.menuItemsPorNombre = (menuItems != null) ? menuItems : new HashMap<>();}
    public void setToolbarManager			(ToolbarManager toolbarManager) {this.toolbarManager = toolbarManager;}
    public void setViewManager				(ViewManager viewManager) {this.viewManager = viewManager;}
    public void setConfigApplicationManager	(ConfigApplicationManager manager) { this.configAppManager = manager; }
    public void setDisplayModeManager		(controlador.managers.DisplayModeManager displayModeManager) {this.displayModeManager = displayModeManager;}
    public void setGeneralController		(GeneralController generalController) {this.generalController = generalController;}
    public void setImageListManager			(ImageListManager imageListManager) { this.imageListManager = imageListManager; }
    
// ***************************************************************************************************** FIN GETTERS Y SETTERS
// ***************************************************************************************************************************    

// ***************************************************************************************************************************
// ************************************************************************************************* METODOS DE SINCRONIZACION

    
    /**
     * Sincroniza todos los componentes de la UI que son específicos del modo VISUALIZADOR.
     * Este método es llamado por el GeneralController.
     */
    public void sincronizarComponentesDeModoVisualizador() {
        logger.debug("  [VisorController] Sincronizando componentes específicos del modo Visualizador...");

        // 1. Sincronizar todos los controles de Zoom
        zoomManager.sincronizarEstadoVisualBotonesYRadiosZoom();

        // 2. Sincronizar el toggle de Mantener Proporciones
        Action proporcionesAction = actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);
        if (proporcionesAction != null) {
            proporcionesAction.putValue(Action.SELECTED_KEY, model.isMantenerProporcion());
        }

        // 4. Sincronizar el botón de Marcar para Proyecto
        Action marcarAction = actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
        if (marcarAction != null) {
            boolean isMarked = false;
            if (model.getSelectedImageKey() != null && projectManager != null) {
                Path imagePath = model.getRutaCompleta(model.getSelectedImageKey());
                if (imagePath != null) {
                    isMarked = projectManager.estaMarcada(imagePath);
                }
            }
            marcarAction.putValue(Action.SELECTED_KEY, isMarked);
        }
        
    }// --- FIN DEL METODO sincronizarComponentesDeModoVisualizador ---
    
    
    public void sincronizarEstadoDeTodasLasToggleThemeActions() {
        logger.debug("[VisorController] Sincronizando estado de todas las ToggleThemeAction...");
        if (this.actionMap == null || this.themeManager == null) return;

        for (Action action : this.actionMap.values()) {
            if (action instanceof ToggleThemeAction) {
                ((ToggleThemeAction) action).sincronizarEstadoSeleccionConManager();
            }
        }
    } // FIN del metodo sincronizarEstadoDeTodasLasToggleThemeActions
    
    
// ********************************************************************************************* FIN METODOS DE SINCRONIZACION
// ***************************************************************************************************************************    
    
    
// *************************************************************************** CLASE ANIDADA DE CONTROL DE MINIATURAS VISIBLES
// ***************************************************************************************************************************    
     
     
     public static class RangoMiniaturasCalculado { // Puede ser public o package-private
         public final int antes;
         public final int despues;

         public RangoMiniaturasCalculado(int antes, int despues) {
             this.antes = antes;
             this.despues = despues;
         }
     }

     
// *********************************************************************** FIN CLASE ANIDADA DE CONTROL DE MINIATURAS VISIBLES
// ***************************************************************************************************************************    

     
} // --- FIN CLASE VisorController ---
