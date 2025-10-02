package controlador;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.actions.tema.ToggleThemeAction;
import controlador.actions.zoom.AplicarModoZoomAction;
import controlador.commands.AppActionCommands;
import controlador.factory.ActionFactory;
import controlador.interfaces.IModoController;
import controlador.managers.CarouselManager;
import controlador.managers.ConfigApplicationManager;
import controlador.managers.DisplayModeManager;
import controlador.managers.FilterManager;
import controlador.managers.InfobarImageManager;
import controlador.managers.InfobarStatusManager;
import controlador.managers.ToolbarManager;
import controlador.managers.ViewManager;
import controlador.managers.interfaces.IListCoordinator;
import controlador.managers.interfaces.IViewManager;
import controlador.managers.interfaces.IZoomManager;
import controlador.utils.ComponentRegistry;
import controlador.worker.BuscadorArchivosWorker;

// --- Imports de Modelo, Servicios y Vista ---
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.ProjectManager;
import servicios.image.ThumbnailService;
import servicios.zoom.ZoomModeEnum;
import vista.VisorView;
import vista.config.ViewUIConfig;
import vista.dialogos.TaskProgressDialog;
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
public class VisorController implements IModoController, ActionListener, ClipboardOwner, ThemeChangeListener  {
	
	private static final Logger logger = LoggerFactory.getLogger(VisorController.class);

    // --- 1. Referencias a Componentes del Sistema ---

	public VisorModel model;						// El modelo de datos principal de la aplicación
    public VisorView view;							// Clase principal de la Interfaz Grafica
    private ViewManager viewManager;
    private ConfigurationManager configuration;		// Gestor del archivo de configuracion
    private IconUtils iconUtils;					// utilidad para cargar y gestionar iconos de la aplicación
    private ThemeManager themeManager;				// Gestor de tema visual de la interfaz
    private ThumbnailService servicioMiniaturas;	// Servicio para gestionar las miniaturas
    private IListCoordinator listCoordinator;		// El coordinador para la selección y navegación en las listas
    private ProjectManager projectManager;			// Gestor de proyectos (imagenes favoritas)
    private IZoomManager zoomManager;				// Responsable de los metodos de zoom
    private ComponentRegistry registry;
    private InfobarImageManager infobarImageManager; 
    private InfobarStatusManager statusBarManager;
    private ProjectController projectController;
    private ToolbarManager toolbarManager;
    private ActionFactory actionFactory;
    private controlador.managers.DisplayModeManager displayModeManager;
    private GeneralController generalController;
    
    // --- Comunicación con AppInitializer ---
    private ViewUIConfig uiConfigForView;			// Necesario para el renderer (para colores y config de thumbWidth/Height)
    private int calculatedMiniaturePanelHeight;		//

    private ExecutorService executorService;		 
    
    // --- 2. Estado Interno del Controlador ---
    private Future<?> cargaImagenesFuture;
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
    public VisorController() {
        logger.debug("--- Iniciando VisorController (Constructor Simple) ---");
        AppInitializer initializer = new AppInitializer(this); // Pasa 'this'
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
                        actualizarTituloVentana();
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
     * Carga o recarga la lista de imágenes desde disco para una carpeta específica,
     * utilizando un SwingWorker para no bloquear el EDT. Muestra un diálogo de
     * progreso durante la carga. Una vez cargada la lista: 
     * - Actualiza el modelo principal de datos (`VisorModel`). 
     * - Actualiza las JList en la vista (`VisorView`). 
     * - Inicia el precalentamiento ASÍNCRONO y DIRIGIDO del caché de miniaturas. 
     * - Selecciona una imagen específica (si se proporciona `claveImagenAMantener`) 
     *   o la primera imagen de la lista. 
     * - Ejecuta un callback opcional al finalizar con éxito.
     *
     * @param claveImagenAMantener La clave única (ruta relativa) de la imagen que
     *                             se intentará seleccionar después de que la lista
     *                             se cargue. Si es `null`, se seleccionará la
     *                             primera imagen (índice 0).
     * @param alFinalizarConExito Un objeto Runnable cuya lógica se ejecutará en el EDT
     *                            después de que la carga y el procesamiento de la lista
     *                            hayan finalizado con éxito. Puede ser `null`.
     */
    public void cargarListaImagenes(String claveImagenAMantener, Runnable alFinalizarConExito) {
        logger.debug("-->>> INICIO cargarListaImagenes | Mantener Clave: " + claveImagenAMantener);

        if (configuration == null || model == null || executorService == null || executorService.isShutdown() || view == null) {
            logger.error("ERROR [cargarListaImagenes]: Dependencias nulas o Executor apagado.");
            if (view != null) SwingUtilities.invokeLater(this::limpiarUI);
            return;
        }

        if (cargaImagenesFuture != null && !cargaImagenesFuture.isDone()) {
            logger.debug("  -> Cancelando tarea de carga de lista anterior...");
            cargaImagenesFuture.cancel(true);
        }

        final boolean mostrarSoloCarpeta = model.isMostrarSoloCarpetaActual();
        int depth = mostrarSoloCarpeta ? 1 : Integer.MAX_VALUE;
        Path pathDeInicioWalk = model.getCarpetaRaizActual();

        if (pathDeInicioWalk == null || !Files.isDirectory(pathDeInicioWalk)) {
            logger.warn("[cargarListaImagenes] No se puede cargar: Carpeta de inicio inválida o nula: " + pathDeInicioWalk);
            limpiarUI();
            if (statusBarManager != null) {
                statusBarManager.mostrarMensaje("No hay una carpeta válida seleccionada. Usa 'Archivo -> Abrir Carpeta'.");
            }
            return;
        }
        
        if (this.servicioMiniaturas != null) {
            this.servicioMiniaturas.limpiarCache();
        }
        
        final TaskProgressDialog dialogo = new TaskProgressDialog(view, "Cargando Imágenes", "Escaneando carpeta de imágenes...");
        final BuscadorArchivosWorker worker = new BuscadorArchivosWorker(
            pathDeInicioWalk,
            depth,
            pathDeInicioWalk,
            this::esArchivoImagenSoportado,
            dialogo
        );
        dialogo.setWorkerAsociado(worker);
        this.cargaImagenesFuture = worker;

        worker.addPropertyChangeListener(evt -> {
            if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE.equals(evt.getNewValue())) {
                
                if (worker.isCancelled()) {
                    logger.debug("    -> Tarea CANCELADA por el usuario.");
                    dialogo.setFinalMessageAndClose("Carga cancelada.", false, 1500);
                    if (statusBarManager != null) {
                        statusBarManager.mostrarMensaje("Carga cancelada por el usuario.");
                    }
                    return;
                }

                try {
                    Map<String, Path> mapaResultado = worker.get();

                    if (mapaResultado == null || mapaResultado.isEmpty()) {
                        logger.info("    -> La búsqueda no encontró imágenes soportadas. Entrando en estado de bienvenida final.");
                        dialogo.setFinalMessageAndClose("La carpeta no contiene imágenes.", false, 2000);
                        
                        this.limpiarUI(); 
                        
                        if (listCoordinator != null) {
                            listCoordinator.forzarActualizacionEstadoAcciones();
                        }
                        
                        if (statusBarManager != null) {
                            statusBarManager.mostrarMensaje("La carpeta no contiene imágenes. Abre otra para empezar.");
                        }
                        
                        return; 
                    }

                    dialogo.closeDialog();
                    
                    if (statusBarManager != null) statusBarManager.limpiarMensaje();
                    
                    logger.debug("    -> Restaurando visibilidad de paneles según la configuración del usuario.");
                    if (registry != null && actionMap != null) {
                        
                        // --- INICIO DE LA MODIFICACIÓN FINAL ---
                        Action fileListAction = actionMap.get(AppActionCommands.CMD_VISTA_TOGGLE_FILE_LIST);
                        if (fileListAction != null) {
                            boolean shouldBeVisible = Boolean.TRUE.equals(fileListAction.getValue(Action.SELECTED_KEY));
                            JPanel panelIzquierdo = registry.get("panel.izquierdo.contenedorPrincipal");
                            if (panelIzquierdo != null) {
                                panelIzquierdo.setVisible(shouldBeVisible);
                                // ¡LA LÍNEA QUE FALTABA!
                                if (shouldBeVisible) {
                                    JSplitPane splitPane = registry.get("splitpane.main");
                                    if (splitPane != null) {
                                        splitPane.setDividerLocation(0.25);
                                    }
                                }
                            }
                        }

                        Action thumbnailsAction = actionMap.get(AppActionCommands.CMD_VISTA_TOGGLE_THUMBNAILS);
                        if (thumbnailsAction != null) {
                            boolean shouldBeVisible = Boolean.TRUE.equals(thumbnailsAction.getValue(Action.SELECTED_KEY));
                            JScrollPane scrollMiniaturas = registry.get("scroll.miniaturas");
                            if (scrollMiniaturas != null) {
                                scrollMiniaturas.setVisible(shouldBeVisible);
                            }
                        }
                        // --- FIN DE LA MODIFICACIÓN FINAL ---
                    }

                    List<String> clavesOrdenadas = new ArrayList<>(mapaResultado.keySet());
                    java.util.Collections.sort(clavesOrdenadas);

                    DefaultListModel<String> nuevoModeloListaPrincipal = new DefaultListModel<>();
                    nuevoModeloListaPrincipal.addAll(new java.util.Vector<>(clavesOrdenadas));
                    
                    model.setMasterListAndNotify(nuevoModeloListaPrincipal, mapaResultado, this); // linea adaptada

                    if (view != null) {
                        view.setListaImagenesModel(model.getModeloLista());
                        view.setTituloPanelIzquierdo("Archivos: " + model.getModeloLista().getSize());
                    }
                    
                    int indiceCalculado = -1;
                    if (claveImagenAMantener != null && !claveImagenAMantener.isEmpty()) {
                        indiceCalculado = model.getModeloLista().indexOf(claveImagenAMantener);
                    }
                    if (indiceCalculado == -1 && !model.getModeloLista().isEmpty()) {
                        indiceCalculado = 0;
                    }

                    if (listCoordinator != null && indiceCalculado != -1) {
                        listCoordinator.reiniciarYSeleccionarIndice(indiceCalculado);
                    }

                    
                    
                    
                    if (alFinalizarConExito != null) {
                        alFinalizarConExito.run();
                    }

                } catch (Exception e) {
                    logger.error("    -> ERROR durante la ejecución del worker: " + e.getMessage(), e);
                    dialogo.setFinalMessageAndClose("Error durante la carga.", true, 2500);
                    limpiarUI();
                    if (statusBarManager != null) {
                        statusBarManager.mostrarMensaje("Error al leer la carpeta. Consulta los logs para más detalles.");
                    }
                } finally {
                    if (cargaImagenesFuture == worker) {
                        cargaImagenesFuture = null;
                    }
                }
            }
        });

        worker.execute();
        SwingUtilities.invokeLater(() -> {
            if (dialogo != null) {
                dialogo.setVisible(true);
            }
        });
        
    } // --- fin del metodo cargarListaImagenes ---
    

    /**
     * Carga una nueva "lista maestra" en el modelo a partir de un resultado de filtro precalculado.
     * Este método actualiza el modelo de datos y luego reinicia el ListCoordinator.
     * NO actualiza la JList de la vista directamente; esa es responsabilidad del GeneralController.
     *
     * @param resultadoFiltro Un objeto FilterResult que contiene el nuevo modelo de lista y el mapa de rutas.
     * @param alFinalizarConExito Un Runnable opcional para ejecutar al final.
     */
    public void cargarListaDesdeFiltro(FilterManager.FilterResult resultadoFiltro, Runnable alFinalizarConExito) {
        logger.debug("-->>> INICIO VisorController.cargarListaDesdeFiltro | Tamaño: {}", resultadoFiltro.model().getSize());

        if (model == null || listCoordinator == null) {
            logger.error("ERROR [cargarListaDesdeFiltro]: Dependencias críticas (model, listCoordinator) nulas.");
            return;
        }

        // 1. Obtener los datos del resultado del filtro.
        DefaultListModel<String> modeloFiltrado = resultadoFiltro.model();
        Map<String, Path> mapaFiltrado = resultadoFiltro.pathMap();

        // 2. Actualizar el modelo central. Esto cambia la "Lista Maestra" en el ListContext ACTIVO.
        model.actualizarListaCompleta(modeloFiltrado, mapaFiltrado);

        // --- LÍNEA ELIMINADA ---
        // view.setListaImagenesModel(model.getModeloLista());  <-- ESTO SE QUITA
        // view.setTituloPanelIzquierdo(...);                    <-- ESTO TAMBIÉN

        // 3. Determinar el índice a seleccionar (el primero de la lista filtrada).
        int indiceASeleccionar = modeloFiltrado.isEmpty() ? -1 : 0;

        // 4. Reiniciar el coordinador con la nueva lista y la selección inicial.
        //    Esto disparará la actualización de la imagen principal y las miniaturas.
        listCoordinator.reiniciarYSeleccionarIndice(indiceASeleccionar);

        // 5. Ejecutar el callback si existe.
        if (alFinalizarConExito != null) {
            // Se ejecuta en el hilo actual, si necesita ser en EDT, quien llama es responsable.
            alFinalizarConExito.run();
        }
        
        logger.debug("-->>> FIN VisorController.cargarListaDesdeFiltro. Modelo actualizado.");
    
    } // --- Fin del método cargarListaDesdeFiltro ---
    
    
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
     * Actualiza el título de la ventana principal de la aplicación.
     * El título incluirá el nombre del proyecto activo si estamos en modo Proyecto,
     * o el nombre de la aplicación si estamos en otro modo.
     */
    public void actualizarTituloVentana() {
        if (view == null || model == null || projectManager == null) {
            return;
        }
        
        String tituloBase = "ModelTag - Your visual STL manager";
        String tituloFinal;
        String prefijoDirty = projectManager.hayCambiosSinGuardar() ? "*" : "";

        // Obtiene el nombre del proyecto desde el ProjectManager. Esta es la fuente principal.
        String nombreProyecto = projectManager.getNombreProyectoActivo();
        
        // Comprobación de seguridad: Si el PM dice "Proyecto Temporal", pero el modelo
        // tiene una ruta con nombre (porque acabamos de guardar), usamos la del modelo.
        // Esto cierra la brecha de sincronización después de "Guardar Como".
        if ("Proyecto Temporal".equals(nombreProyecto) && model.getRutaProyectoActivoConNombre() != null) {
            nombreProyecto = model.getRutaProyectoActivoConNombre().getFileName().toString();
        }

        // Ahora construimos el título con el nombre correcto.
        if (!"Proyecto Temporal".equals(nombreProyecto) || !projectManager.getImagenesMarcadas().isEmpty()) {
            tituloFinal = prefijoDirty + tituloBase + " - [Proyecto: " + nombreProyecto + "]";
        } else {
            // Modo visualizador puro, sin proyecto activo ni imágenes marcadas.
            tituloFinal = tituloBase;
        }
        
        view.setTitle(tituloFinal);
        logger.debug("Título de la ventana actualizado a: {}", tituloFinal);
        
    } // ---FIN de metodo actualizarTituloVentana---


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
        cargarListaImagenes(claveASeleccionar, accionPostCarga);
        
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
    
    
    public void limpiarUI() {
    	
        logger.debug("[Controller] Limpiando UI y Modelo a estado de bienvenida...");

        if (listCoordinator != null) {
            listCoordinator.setSincronizandoUI(true); // Bloquea listeners para evitar eventos en cascada
        }

        try {
            // --- 1. LIMPIEZA DEL MODELO Y CACHÉ ---
            if (model != null) {
                model.setCurrentImage(null);
                model.setSelectedImageKey(null);
                model.resetZoomState();
                // Limpiamos los modelos de las listas de forma segura
                if (this.modeloMiniaturasVisualizador != null) this.modeloMiniaturasVisualizador.clear();
                if (this.modeloMiniaturasCarrusel != null) this.modeloMiniaturasCarrusel.clear();
                logger.debug("  -> Estado de imagen, selección y modelos de lista en 'model' limpiados.");
            }

            if (servicioMiniaturas != null) {
                servicioMiniaturas.limpiarCache();
                logger.debug("  -> Caché de miniaturas limpiado.");
            }
            
            // --- 2. ACTUALIZACIÓN VISUAL DE LA PANTALLA DE BIENVENIDA ---
            if (view != null && iconUtils != null && viewManager != null) {
                view.limpiarImagenMostrada(); // Limpia la imagen anterior si la hubiera
                
                ImageDisplayPanel displayPanel = viewManager.getActiveDisplayPanel();
                if (displayPanel != null) {
                    ImageIcon welcomeIcon = iconUtils.getWelcomeImage("modeltag-bienvenida-apaisado.png");
                    if (welcomeIcon != null) {
                        // Prepara y muestra la imagen de bienvenida
                        BufferedImage welcomeImage = new BufferedImage(
                            welcomeIcon.getIconWidth(),
                            welcomeIcon.getIconHeight(),
                            BufferedImage.TYPE_INT_ARGB);
                        java.awt.Graphics2D g2d = welcomeImage.createGraphics();
                        welcomeIcon.paintIcon(null, g2d, 0, 0);
                        g2d.dispose();
                        displayPanel.setWelcomeImage(welcomeImage);
                        displayPanel.showWelcomeMessage();
                    } else {
                        displayPanel.limpiar(); // Fallback si no hay imagen de bienvenida
                    }
                }
            }
            
            // --- 3. OCULTAMIENTO DE PANELES POR ESTADO (LA LÓGICA QUE DISCUTIMOS) ---
            // Se ocultan los paneles que no tienen sentido sin una lista de imágenes.
            // Esto se hace directamente sobre los componentes, sin afectar la configuración del usuario.
            if (registry != null) {
                 logger.debug("  -> Ocultando paneles de lista y miniaturas por estado de bienvenida.");
                
                 // Ocultar panel de lista de archivos
                 JPanel panelIzquierdo = registry.get("panel.izquierdo.contenedorPrincipal");
                 if (panelIzquierdo != null) panelIzquierdo.setVisible(false);

                 // Ocultar panel de miniaturas del visualizador
                 JScrollPane scrollMiniaturasVisor = registry.get("scroll.miniaturas");
                 if (scrollMiniaturasVisor != null) scrollMiniaturasVisor.setVisible(false);
                 
                 // Ocultar panel de miniaturas del carrusel (por si acaso)
                 JScrollPane scrollMiniaturasCarousel = registry.get("scroll.miniaturas.carousel");
                 if (scrollMiniaturasCarousel != null) scrollMiniaturasCarousel.setVisible(false);
            }

            // --- 4. PREVENCIÓN DEL NULLPOINTEREXCEPTION ---
            // Se actualizan las barras de información para que muestren su estado "vacío".
            // Esto evita que intenten acceder a datos nulos del modelo.
            if (infobarImageManager != null) {
                infobarImageManager.actualizar();
            }
            if (statusBarManager != null) {
                statusBarManager.actualizar();
            }
            
            // --- 5. ACTUALIZACIÓN FINAL DE ACCIONES ---
            // Se actualiza el estado de los botones (deshabilitar "Siguiente", "Anterior", etc.)
            if (listCoordinator != null) {
                listCoordinator.forzarActualizacionEstadoAcciones();
            }

            if (view != null) {
                logger.debug("  -> Forzando revalidate() y repaint() de la ventana principal para imponer el estado de bienvenida.");
                view.revalidate();
                view.repaint();
            }
            
        } finally {
            if (listCoordinator != null) {
                SwingUtilities.invokeLater(() -> listCoordinator.setSincronizandoUI(false)); // Libera listeners
            }
        }
        
        logger.debug("[Controller] Limpieza de UI y Modelo completada.");
        
    } // Fin del metodo limpiarUI ---
    
    
// *********************************************************************************************************** FIN DE UTILIDAD  
// ***************************************************************************************************************************    

// ***************************************************************************************************************************     
// ******************************************************************************************************************** LOGICA
     
     
     /**
      * Verifica si un archivo, dado por su Path, tiene una extensión
      * correspondiente a los formatos de imagen que la aplicación soporta actualmente.
      * La comparación de extensiones ignora mayúsculas/minúsculas.
      *
      * Formatos soportados actualmente: JPG, JPEG, PNG, GIF, BMP.
      *
      * @param path El objeto Path que representa la ruta del archivo a verificar.
      *             No debe ser null.
      * @return true si el archivo tiene una extensión de imagen soportada,
      *         false si no la tiene, si el path es null, o si no tiene nombre de archivo.
      */
     private boolean esArchivoImagenSoportado(Path path) {
         // 1. Validación de entrada: Asegurar que el Path no sea null
         if (path == null) {
             // No imprimir error aquí, es normal que se llame con null a veces
             return false;
         }

         // 2. Obtener el nombre del archivo del Path
         Path nombreArchivoPath = path.getFileName();
         if (nombreArchivoPath == null) {
             // Path representa un directorio raíz o algo sin nombre de archivo
             return false;
         }
         String nombreArchivo = nombreArchivoPath.toString();

         // 3. Evitar procesar archivos ocultos o carpetas (defensivo)
         try {
              if (!Files.isRegularFile(path) || Files.isHidden(path)) {
                   return false;
              }
         } catch (IOException e) {
              // Error al acceder a atributos del archivo, tratar como no soportado
               logger.warn("WARN [esArchivoImagenSoportado]: Error al comprobar atributos de " + path + ": " + e.getMessage());
               return false;
         } catch (SecurityException se) {
              // No tenemos permisos para leer atributos
               logger.warn("WARN [esArchivoImagenSoportado]: Sin permisos para comprobar atributos de " + path);
               return false;
         }


         // 4. Encontrar la posición del último punto (separador de extensión)
         int lastDotIndex = nombreArchivo.lastIndexOf('.');
         if (lastDotIndex <= 0 || lastDotIndex == nombreArchivo.length() - 1) {
             // No hay punto, empieza con punto (oculto en Unix), o termina con punto (sin extensión)
             return false;
         }

         // 5. Extraer la extensión y convertir a minúsculas
         String extension = nombreArchivo.substring(lastDotIndex + 1).toLowerCase();

         // 6. Comprobar si la extensión está en la lista de soportadas
         //    Usar un switch es legible para pocas extensiones
         switch (extension) {
             case "jpg":
             case "jpeg":
             case "png":
             case "gif":
             case "bmp":
             case "tiff":
             case "psd":
             case "webp":
             case "tga":
             case "pcx":
            	 
                 return true; // Es una extensión soportada
             default:
                 return false; // No es una extensión soportada
         }

         /* Alternativa con List.of y contains (un poco más flexible si tienes muchas):
            List<String> extensionesSoportadas = List.of("jpg", "jpeg", "png", "gif", "bmp");
            return extensionesSoportadas.contains(extension);
         */

     } // --- FIN esArchivoImagenSoportado ---

 
     /**
      * Lanza tareas en segundo plano usando el ExecutorService para generar y cachear
      * las miniaturas de tamaño normal para la lista de rutas de imágenes proporcionada.
      * Esto ayuda a que el MiniaturaListCellRenderer encuentre las miniaturas ya listas
      * en el caché la mayoría de las veces, mejorando la fluidez del scroll.
      *
      * @param rutas La lista de objetos Path correspondientes a todas las imágenes
      *              cargadas actualmente en el modelo principal.
      */
     public void precalentarCacheMiniaturasAsync(List<Path> rutas) {
         // 1. Validar dependencias y entrada
         if (servicioMiniaturas == null) {
              logger.error("ERROR [Precalentar Cache]: ThumbnailService es nulo.");
              return;
         }
         if (executorService == null || executorService.isShutdown()) {
              logger.error("ERROR [Precalentar Cache]: ExecutorService no está disponible o está apagado.");
              return;
         }
         if (rutas == null || rutas.isEmpty()) {
             logger.debug("[Precalentar Cache]: Lista de rutas vacía o nula. No hay nada que precalentar.");
             return;
         }
         if (model == null) { // Necesitamos el modelo para obtener las dimensiones normales
              logger.error("ERROR [Precalentar Cache]: Modelo es nulo.");
              return;
         }

         logger.debug("[Controller] Iniciando pre-calentamiento de caché para " + rutas.size() + " miniaturas...");

         // 2. Obtener Dimensiones Normales del Modelo
         //    Usamos las dimensiones configuradas para las miniaturas "no seleccionadas"
         final int anchoNormal = model.getMiniaturaNormAncho();
         final int altoNormal = model.getMiniaturaNormAlto();

         // Verificar que las dimensiones sean válidas
         if (anchoNormal <= 0) {
             logger.error("ERROR [Precalentar Cache]: Ancho normal de miniatura inválido (" + anchoNormal + "). Abortando.");
             return;
         }
         // Nota: altoNormal puede ser <= 0 si se quiere mantener proporción basada en anchoNormal

         // 3. Enviar una Tarea al Executor por cada Imagen
         //    Cada tarea generará (si no existe ya) y cacheará una miniatura.
         int tareasLanzadas = 0;
         for (Path ruta : rutas) {
             // Saltar si la ruta es nula (aunque no debería pasar si la carga fue correcta)
             if (ruta == null) continue;

             // Enviar la tarea al ExecutorService
             executorService.submit(() -> { // Inicio lambda tarea individual
                 try {
                	 
                     // 3.1. Generar Clave Única para el Caché
                     //      (Debe ser consistente con cómo se genera en otros lugares)
                     Path relativePath = null;
                     Path carpetaRaizDelModelo = this.model.getCarpetaRaizActual(); // <<< OBTENER DEL MODELO
                     
                     if (carpetaRaizDelModelo != null) {      // <<< CAMBIO AQUÍ
                    	 
                          try {
                        	  
                              // Intentar relativizar respecto a la carpeta raíz actual
                        	  relativePath = carpetaRaizDelModelo.relativize(ruta);   // <<< CAMBIO AQUÍ
                              
                          } catch (IllegalArgumentException e) {
                               // Si no se puede relativizar (ej. están en unidades diferentes), usar nombre archivo
                               // logger.warn("WARN [Precalentar Cache BG]: No se pudo relativizar " + ruta + ". Usando nombre.");
                               relativePath = ruta.getFileName();
                               
                          } catch (Exception e) {
                               // Otro error inesperado al relativizar
                               logger.error("ERROR [Precalentar Cache BG]: Relativizando " + ruta + ": " + e.getMessage());
                               relativePath = ruta.getFileName(); // Fallback
                          }
                          
                     } else {
                          // Si no hay carpeta raíz definida, usar solo el nombre del archivo
                          // logger.warn("WARN [Precalentar Cache BG]: Carpeta raíz actual es null. Usando nombre archivo.");
                          relativePath = ruta.getFileName();
                     }

                     // Asegurar que relativePath no sea null y obtener clave
                     if (relativePath == null) {
                          logger.error("ERROR [Precalentar Cache BG]: No se pudo obtener ruta relativa para " + ruta);
                          return; // Salir de esta tarea lambda específica
                     }
                     String claveUnica = relativePath.toString().replace("\\", "/");


                     // 3.2. Llamar al Servicio para Obtener/Crear y Cachear
                     //      Pasamos 'true' para 'esTamanoNormal' para que se guarde en caché.
                     servicioMiniaturas.obtenerOCrearMiniatura(
                    		 ruta, claveUnica, anchoNormal, altoNormal, true // <- true para indicar que es tamaño normal
                     );

                 } catch (Exception e) {
                     // Captura cualquier error inesperado dentro de la tarea submit
                     logger.error("ERROR INESPERADO [Precalentar Cache BG] Procesando " + ruta + ": " + e.getMessage());
                     e.printStackTrace();
                 }
             }); // Fin lambda tarea individual
             tareasLanzadas++;
         } // Fin bucle for

         // 4. Log Final (Informa que las tareas fueron enviadas)
         logger.debug("[Controller] " + tareasLanzadas + " tareas de pre-calentamiento de caché lanzadas al ExecutorService.");

         // 5. Repintado Inicial Opcional
         if (view != null && registry.get("list.miniaturas") != null) {
             SwingUtilities.invokeLater(() -> {
                 if (view != null && registry.get("list.miniaturas") != null) { // Doble chequeo
                      logger.debug("  -> Solicitando repintado inicial de listaMiniaturas.");
                      registry.get("list.miniaturas").repaint();
                 }
             });
         }

     } // --- FIN precalentarCacheMiniaturasAsync ---
     
     
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
            sincronizarEstadoVisualBotonesYRadiosZoom();
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
        sincronizarEstadoVisualBotonesYRadiosZoom();
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
     }
     
     
     /**
      * Recorre un componente contenedor y todos sus hijos (y los hijos de sus hijos, etc.)
      * para aplicarles un color de texto (foreground) específico.
      * Este método es crucial para asegurar que todos los elementos dentro de paneles
      * con colores de fondo personalizados (como la barra de estado) hereden el color
      * de texto correcto para mantener la legibilidad.
      *
      * @param container El componente raíz desde el que empezar a aplicar colores.
      * @param color El color de texto a aplicar.
      */
     private void actualizarColoresDeTextoRecursivamente(java.awt.Container container, Color color) {
         // Itera sobre todos los componentes directos del contenedor.
         for (java.awt.Component component : container.getComponents()) {
             // Aplica el color de texto al componente actual.
             component.setForeground(color);
             
             // Si el componente es a su vez un contenedor (como un JToolBar o un JPanel anidado),
             // se llama a este mismo método de forma recursiva para que actualice a sus hijos.
             if (component instanceof java.awt.Container) {
                 actualizarColoresDeTextoRecursivamente((java.awt.Container) component, color);
             }
         }
     } // --- fin del método actualizarColoresDeTextoRecursivamente ---     
     
     
     /**
      * Muestra un diálogo modal que contiene una lista de los archivos de imagen
      * actualmente cargados en el modelo principal. Permite al usuario ver la lista
      * completa y, opcionalmente, copiarla al portapapeles, mostrando nombres de archivo
      * relativos o rutas completas.
      */
      public void mostrarDialogoListaImagenes() {
          // 1. Validar dependencias (Vista y Modelo necesarios)
          if (view == null || model == null) {
              logger.error("ERROR [mostrarDialogoListaImagenes]: Vista o Modelo nulos. No se puede mostrar el diálogo.");
              // Podríamos mostrar un JOptionPane de error aquí si fuera crítico
              return;
          }
          logger.debug("[Controller] Abriendo diálogo de lista de imágenes...");

          // 2. Crear el JDialog
          //    - Lo hacemos modal (true) para que bloquee la ventana principal mientras está abierto.
          //    - Usamos view.getFrame() como padre para que se centre correctamente.
          final JDialog dialogoLista = new JDialog(view, "Lista de Imágenes Cargadas", true);
          dialogoLista.setSize(600, 400); // Tamaño inicial razonable
          dialogoLista.setLocationRelativeTo(view); // Centrar sobre la ventana principal
          dialogoLista.setLayout(new BorderLayout(5, 5)); // Layout principal del diálogo

          // 3. Crear componentes internos del diálogo
          
          // 3.1. Modelo para la JList del diálogo (será llenado dinámicamente)
          final DefaultListModel<String> modeloListaDialogo = new DefaultListModel<>();
          
          // 3.2. JList que usará el modelo anterior
          JList<String> listaImagenesDialogo = new JList<>(modeloListaDialogo);
          
          // 3.3. ScrollPane para la JList (indispensable si la lista es larga)
          JScrollPane scrollPaneListaDialogo = new JScrollPane(listaImagenesDialogo);
          
          // 3.4. CheckBox para alternar entre nombres relativos y rutas completas
          final JCheckBox checkBoxMostrarRutas = new JCheckBox("Mostrar Rutas Completas");
          
          // 3.5. Botón para copiar la lista visible al portapapeles
          JButton botonCopiarLista = new JButton("Copiar Lista");

          // 4. Configurar Panel Superior (Botón Copiar y CheckBox)
          JPanel panelSuperiorDialog = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Alineación izquierda
          panelSuperiorDialog.add(botonCopiarLista);
          panelSuperiorDialog.add(checkBoxMostrarRutas);

          // 5. Añadir Componentes al Layout del Diálogo
          dialogoLista.add(panelSuperiorDialog, BorderLayout.NORTH);  // Panel superior arriba
          dialogoLista.add(scrollPaneListaDialogo, BorderLayout.CENTER); // Lista (en scroll) en el centro

          // 6. Añadir ActionListeners a los controles interactivos
          
          // 6.1. Listener para el CheckBox (actualiza la lista cuando cambia su estado)
          checkBoxMostrarRutas.addActionListener(e -> {
              // Llama al método helper para refrescar el contenido de la lista del diálogo
              // pasándole el modelo del diálogo y el estado actual del checkbox.
              actualizarListaEnDialogo(modeloListaDialogo, checkBoxMostrarRutas.isSelected());
          });

          // 6.2. Listener para el Botón Copiar
          botonCopiarLista.addActionListener(e -> {
          
        	  // Llama al método helper que copia el contenido del modelo del diálogo
              copiarListaAlPortapapeles(modeloListaDialogo);
          });

          // 7. Cargar el contenido inicial de la lista en el diálogo
          //    Se llama una vez antes de mostrar el diálogo, usando el estado inicial del checkbox (desmarcado).
          logger.debug("  -> Actualizando contenido inicial del diálogo...");
          actualizarListaEnDialogo(modeloListaDialogo, checkBoxMostrarRutas.isSelected());

          // 8. Hacer visible el diálogo
          //    Como es modal, la ejecución se detendrá aquí hasta que el usuario cierre el diálogo.
          logger.debug("  -> Mostrando diálogo...");
          dialogoLista.setVisible(true);

          // 9. Código después de cerrar el diálogo (si es necesario)
          //    Aquí podríamos hacer algo una vez el diálogo se cierra, pero usualmente no es necesario.
          logger.debug("[Controller] Diálogo de lista de imágenes cerrado.");

      } // --- FIN mostrarDialogoListaImagenes ---
      
    
      /**
       * Actualiza el contenido del DefaultListModel proporcionado (que pertenece
       * al diálogo de la lista de imágenes) basándose en el modelo principal
       * de la aplicación (model.getModeloLista()) y el mapa de rutas completas
       * (model.getRutaCompletaMap()).
       *
       * Llena el modelo del diálogo con las claves relativas o las rutas absolutas
       * de los archivos, según el valor del parámetro 'mostrarRutas'.
       *
       * @param modeloDialogo El DefaultListModel del JList que se encuentra en el diálogo.
       *                      Este método modificará su contenido (lo limpia y lo vuelve a llenar).
       * @param mostrarRutas  boolean que indica qué formato mostrar:
       *                      - true: Muestra la ruta completa (absoluta) de cada archivo.
       *                      - false: Muestra la clave única (ruta relativa) de cada archivo.
       */
      private void actualizarListaEnDialogo(DefaultListModel<String> modeloDialogo, boolean mostrarRutas) {
          // 1. Validación de entradas
          if (modeloDialogo == null) {
              logger.error("ERROR [actualizarListaEnDialogo]: El modelo del diálogo es null.");
              return;
          }
          if (model == null || model.getModeloLista() == null || model.getRutaCompletaMap() == null) {
              logger.error("ERROR [actualizarListaEnDialogo]: El modelo principal o sus componentes internos son null.");
              modeloDialogo.clear(); // Limpiar el diálogo si no hay datos fuente
              modeloDialogo.addElement("Error: No se pudo acceder a los datos de la lista principal.");
              return;
          }

          // 2. Referencias al modelo principal y al mapa de rutas
          DefaultListModel<String> modeloPrincipal = model.getModeloLista();
          Map<String, Path> mapaRutas = model.getRutaCompletaMap();

          // 3. Log informativo
          logger.debug("  [Dialogo Lista] Actualizando contenido. Mostrar Rutas: " + mostrarRutas + ". Elementos en modelo principal: " + modeloPrincipal.getSize());

          // 4. Limpiar el modelo del diálogo antes de llenarlo
          modeloDialogo.clear();

          // 5. Iterar sobre el modelo principal y añadir elementos al modelo del diálogo
          if (modeloPrincipal.isEmpty()) {
              modeloDialogo.addElement("(La lista principal está vacía)");
          } else {
              for (int i = 0; i < modeloPrincipal.getSize(); i++) {
                  // 5.1. Obtener la clave del modelo principal
                  String claveArchivo = modeloPrincipal.getElementAt(i);
                  if (claveArchivo == null) { // Seguridad extra
                      claveArchivo = "(Clave nula en índice " + i + ")";
                  }

                  // 5.2. Determinar qué texto añadir al diálogo
                  String textoAAgregar = claveArchivo; // Por defecto, la clave

                  if (mostrarRutas) {
                      // Si se deben mostrar rutas completas, obtenerla del mapa
                      Path rutaCompleta = mapaRutas.get(claveArchivo);
                      if (rutaCompleta != null) {
                          // Usar la ruta completa si se encontró
                          textoAAgregar = rutaCompleta.toString();
                      } else {
                          // Si no se encontró la ruta (inconsistencia en datos), indicarlo
                          logger.warn("WARN [Dialogo Lista]: No se encontró ruta para la clave: " + claveArchivo);
                          textoAAgregar = claveArchivo + " (¡Ruta no encontrada!)";
                      }
                  }
                  // Si mostrarRutas es false, textoAAgregar simplemente mantiene la claveArchivo.

                  // 5.3. Añadir el texto determinado al modelo del diálogo
                  modeloDialogo.addElement(textoAAgregar);
                  
              } // Fin del bucle for
          } // Fin else (modeloPrincipal no está vacío)

          // 6. Log final (opcional)
           logger.debug("  [Dialogo Lista] Contenido actualizado. Elementos añadidos al diálogo: " + modeloDialogo.getSize());

          // Nota: No necesitamos repintar la JList del diálogo aquí.
          // El DefaultListModel notifica automáticamente a la JList asociada
          // sobre los cambios (clear y addElement disparan ListDataEvents).

      } // --- FIN actualizarListaEnDialogo ---
      
      
	
      /**
	   * Copia el contenido actual de un DefaultListModel (que se asume contiene
	   * Strings, una por línea) al portapapeles del sistema.
	   * Cada elemento del modelo se añade como una línea separada en el texto copiado.
	   *
	   * @param listModel El DefaultListModel<String> cuyo contenido se copiará.
	   *                  Típicamente, este será el modelo de la JList del diálogo
	   *                  (modeloListaDialogo).
	   */
      public void copiarListaAlPortapapeles(DefaultListModel<String> listModel) {
      // 1. Validación de entrada
      if (listModel == null) {
          logger.error("ERROR [copiarListaAlPortapapeles]: El listModel proporcionado es null.");
          // Opcional: Mostrar mensaje al usuario si la vista está disponible
          
          if (view != null) {
        	  JOptionPane.showMessageDialog(
        	            view, // Usar 'view' directamente como el componente padre
        	            "Error interno al intentar copiar la lista.",
        	            "Error al Copiar",
        	            JOptionPane.WARNING_MESSAGE
        	            );
          }
          
          return;
      }

      // 2. Construir el String a copiar
      StringBuilder sb = new StringBuilder();
      int numeroElementos = listModel.getSize();

      logger.debug("[Portapapeles] Preparando para copiar " + numeroElementos + " elementos...");

      // Iterar sobre todos los elementos del modelo
      for (int i = 0; i < numeroElementos; i++) {
          String elemento = listModel.getElementAt(i);
          if (elemento != null) { // Añadir solo si no es null
              sb.append(elemento); // Añadir el texto del elemento
              
              // Añadir un salto de línea después de cada elemento, excepto el último
              if (i < numeroElementos - 1) {
                  sb.append("\n"); // Usar salto de línea estándar del sistema
                  // Alternativa: sb.append(System.lineSeparator());
              }
          }
      }

      // 3. Crear el objeto Transferable (StringSelection)
      //    StringSelection es una implementación de Transferable para texto plano.
      String textoCompleto = sb.toString();
      StringSelection stringSelection = new StringSelection(textoCompleto);

      // 4. Obtener el Portapapeles del Sistema
      Clipboard clipboard = null;
      try {
          clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      } catch (Exception e) {
           logger.error("ERROR [copiarListaAlPortapapeles]: No se pudo acceder al portapapeles del sistema: " + e.getMessage());
            if (view != null) {
               JOptionPane.showMessageDialog(view,
                                             "Error al acceder al portapapeles del sistema.",
                                             "Error al Copiar", 
                                             JOptionPane.ERROR_MESSAGE);
            }
           return; // Salir si no podemos obtener el clipboard
      }


      // 5. Establecer el contenido en el Portapapeles
      try {
          // El segundo argumento 'this' indica que nuestra clase VisorController
          // actuará como "dueño" temporal del contenido (implementa ClipboardOwner).
          clipboard.setContents(stringSelection, this);
          logger.debug("[Portapapeles] Lista copiada exitosamente (" + numeroElementos + " líneas).");
          
          if (statusBarManager != null) {
              statusBarManager.mostrarMensajeTemporal("Lista copiada al portapapeles (" + numeroElementos + " ítems)", 3000); // Muestra por 3 segundos
          }
          
          // Opcional: Mostrar mensaje de éxito
           if (view != null) {
               // Podríamos usar un mensaje no modal o una etiqueta temporal
               // JOptionPane.showMessageDialog(view.getFrame(), "Lista copiada al portapapeles.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
           }
      } catch (IllegalStateException ise) {
          // Puede ocurrir si el clipboard no está disponible o está siendo usado
           logger.error("ERROR [copiarListaAlPortapapeles]: No se pudo establecer el contenido en el portapapeles: " + ise.getMessage());
            if (view != null) {
               JOptionPane.showMessageDialog(view,
                                             "No se pudo copiar la lista al portapapeles.\n" +
                                             "Puede que otra aplicación lo esté usando.",
                                             "Error al Copiar", JOptionPane.WARNING_MESSAGE);
            }
      } catch (Exception e) {
           // Capturar otros errores inesperados
           logger.error("ERROR INESPERADO [copiarListaAlPortapapeles]: " + e.getMessage());
           e.printStackTrace();
            if (view != null) {
               JOptionPane.showMessageDialog(view,
                                             "Ocurrió un error inesperado al copiar la lista.",
                                             "Error al Copiar", JOptionPane.ERROR_MESSAGE);
            }
      }

  } // --- FIN copiarListaAlPortapapeles ---


	/**
	 * Método requerido por la interfaz ClipboardOwner. Se llama cuando otra
	 * aplicación toma posesión del contenido del portapapeles que esta aplicación
	 * había puesto previamente.
	 * 
	 * En la mayoría de los casos, especialmente cuando solo copiamos texto simple,
	 * no necesitamos realizar ninguna acción específica cuando perdemos la
	 * posesión. Dejamos el método implementado pero vacío.
	 *
	 * @param clipboard El portapapeles que perdió la posesión.
	 * @param contents  El contenido Transferable que estaba en el portapapeles.
	 */
	@Override
	public void lostOwnership (Clipboard clipboard, Transferable contents)
	{
		// 1. Log (Opcional, útil para depuración o entender el flujo)
		// logger.debug("[Clipboard] Se perdió la propiedad del contenido del
		// portapapeles.");

		// 2. Lógica Adicional (Normalmente no necesaria para copia de texto simple)
		// - Si estuvieras manejando recursos más complejos o datos que necesitan
		// liberarse cuando ya no están en el portapapeles, podrías hacerlo aquí.
		// - Para StringSelection, no hay nada que liberar.

		// -> Método intencionalmente vacío en este caso. <-

	} // --- FIN lostOwnership ---       
       

    /**
     * Manejador central de eventos para componentes que NO utilizan directamente
     * el sistema de Actions de Swing (p.ej., JMenuItems a los que se les añadió
     * 'this' como ActionListener en addFallbackListeners por MenuBarBuilder)
     * o para acciones muy específicas que no justificaban una clase Action separada.
     *
     * Prioriza el manejo de los checkboxes para la visibilidad de los botones de la toolbar.
     * Si no es uno de esos, pasa a un switch para otros comandos fallback.
     *
     * @param e El ActionEvent generado por el componente Swing.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // 1. --- LOGG INICIAL DETALLADO ---
        //     Ayuda a depurar qué componente y comando dispararon el evento.
        logActionInfo(e);

        // 2. --- OBTENER INFORMACIÓN DEL EVENTO ---
        Object source = e.getSource();         // El JMenuItem que fue clickeado.
        String command = e.getActionCommand(); // El ActionCommand configurado para ese JMenuItem.

        // 2.1. Validar que el comando no sea nulo.
        if (command == null) {
            logger.warn("WARN [VisorController.actionPerformed]: ActionCommand es null para la fuente: " +
                               (source != null ? source.getClass().getSimpleName() : "null") +
                               ". No se puede procesar.");
            return;
        }
        
        // 4. --- MANEJO DE OTROS COMANDOS (FALLBACK GENERAL o para ítems de menú que usan VisorController como listener directo) ---
        //    Si el código llega aquí, el evento NO FUE de un checkbox de "Visualizar Botones Toolbar".
        //    El 'command' aquí será el AppActionCommands.CMD_... si el JMenuItem fue configurado
        //    con un comando de ese tipo pero SIN una Action directa del actionMap.
        logger.debug("[VC.actionPerformed General Switch] Procesando comando fallback/directo: '" + command + "'");

        // Manejo genérico para todos los comandos de cambio de tema.
        if (command.startsWith(AppActionCommands.CMD_TEMA_PREFIX)) {
            // Extraemos el ID del tema del comando.
            // Ej: "cmd.tema.custom_deepoceanmejorado" -> "custom_deepoceanmejorado"
            String themeId = command.substring(AppActionCommands.CMD_TEMA_PREFIX.length());
            
            logger.debug("    -> Acción: Cambiar Tema. ID extraído: '{}'", themeId);
            
            // Llamamos al ThemeManager para que aplique el tema.
            // El 'true' es para que se repinte la UI y se notifique a los listeners.
            if (this.themeManager != null) {
                this.themeManager.setTemaActual(themeId, true);
            } else {
                logger.error("ERROR: ThemeManager es nulo. No se puede cambiar el tema.");
            }
            // No necesitamos un 'break' porque este if está fuera del switch.
            return; // Hemos manejado la acción, podemos salir del método.
        }
        
        switch (command) {
            // 4.1. --- Configuración ---
	        case AppActionCommands.CMD_CONFIG_GUARDAR:
	            logger.debug("    -> Acción: Guardar Configuración Actual");
	            // ¡OJO! Este método ahora solo guarda la configuración, no el estado del proyecto.
	            if (generalController != null) {
	                // Creamos un nuevo método en GeneralController para esto
	                generalController.handleSaveConfiguration(); 
	            }
	            break;
            case AppActionCommands.CMD_CONFIG_CARGAR_INICIAL:
            	
            	logger.debug("    -> Acción: Restaurar Configuración por Defecto");
                // <<< CAMBIO CLAVE >>>
                if (this.configAppManager != null) {
                    this.configAppManager.restaurarConfiguracionPredeterminada();
                    // La notificación al usuario ya puede estar dentro del método del manager.
                } else {
                    logger.error("ERROR: ConfigApplicationManager es nulo. No se puede restaurar la configuración.");
                }
                break;
            	
            // 4.2. --- Zoom ---
            case AppActionCommands.CMD_ZOOM_PERSONALIZADO: // Para "Establecer Zoom %..." del menú
                logger.debug("    -> Acción: Establecer Zoom % desde Menú");
                handleSetCustomZoomFromMenu();
                break;

            // 4.3. --- Carga de Carpetas/Subcarpetas (Radios del Menú) ---
            //     Estas Actions (SetSubfolderReadModeAction) son responsables de su propia lógica.
            //     Es muy raro que este 'case' se active si las Actions están bien asignadas.
            //     Esto actuaría como un fallback si, por alguna razón, el ActionListener
            //     del JRadioButtonMenuItem fuera 'this' (VisorController) en lugar de su Action.
            case AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA:
                logger.debug("    -> Acción (Fallback Radio): Mostrar Solo Carpeta Actual");
                if (actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA) != null) {
                    actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA).actionPerformed(
                        new ActionEvent(source, ActionEvent.ACTION_PERFORMED, command)
                    );
                }
                break;
            case AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS:
                logger.debug("    -> Acción (Fallback Radio): Mostrar Imágenes de Subcarpetas");
                if (actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS) != null) {
                    actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS).actionPerformed(
                        new ActionEvent(source, ActionEvent.ACTION_PERFORMED, command)
                    );
                }
                break;

            // 4.4. --- Comandos de Imagen (Placeholders) ---
            case AppActionCommands.CMD_IMAGEN_RENOMBRAR:
                logger.debug("    TODO: Implementar Cambiar Nombre Imagen");
                break;
            case AppActionCommands.CMD_IMAGEN_MOVER_PAPELERA:
                logger.debug("    TODO: Implementar Mover a Papelera");
                break;
            case AppActionCommands.CMD_IMAGEN_FONDO_ESCRITORIO:
                logger.debug("    TODO: Implementar Fondo Escritorio");
                break;
            case AppActionCommands.CMD_IMAGEN_FONDO_BLOQUEO:
                logger.debug("    TODO: Implementar Imagen Bloqueo");
                break;
            case AppActionCommands.CMD_IMAGEN_PROPIEDADES:
                logger.debug("    TODO: Implementar Propiedades Imagen");
                break;
            // 4.5. --- Ayuda ---
            case AppActionCommands.CMD_CONFIG_MOSTRAR_VERSION:
                logger.debug("    -> Acción: Mostrar Versión");
                mostrarVersion();
                break;
            case AppActionCommands.CMD_ESPECIAL_REFRESCAR_UI:
            	logger.debug("    -> Acción: Refrescar UI (Delegando a ViewManager)");
                if (this.viewManager != null) {
                    this.viewManager.ejecutarRefrescoCompletoUI();
                } else {
                    logger.error("ERROR: ViewManager es nulo. No se puede refrescar la UI.");
                }
                break;
                
            // 4.6. --- Default Case ---
            default:
                // Si un JMenuItem (que no sea un JMenu contenedor) tiene este VisorController
                // como ActionListener y su ActionCommand no coincide con ninguno de los 'case' anteriores.
                if (source instanceof JMenuItem && !(source instanceof JMenu)) {
                    logger.warn("  WARN [VisorController.actionPerformed]: Comando fallback no manejado explícitamente: '" + command +
                                       "' originado por: " + source.getClass().getSimpleName() +
                                       " con texto: '" + ((JMenuItem)source).getText() + "'");
                }
                break;
        } // Fin del switch general
        
    } // --- FIN actionPerformed ---
	




    /**
     * REFACTORIZADO: Método para manejar la lógica cuando se selecciona "Zoom Personalizado
     * %..." desde el menú principal. Ahora delega al ZoomManager.
     */
    private void handleSetCustomZoomFromMenu() {
        if (this.view == null) {
            logger.error("ERROR [handleSetCustomZoomFromMenu]: Vista nula.");
            return;
        }

        String input = JOptionPane.showInputDialog(
            this.view,
            "Introduce el porcentaje de zoom deseado (ej: 150):",
            "Establecer Zoom Personalizado",
            JOptionPane.PLAIN_MESSAGE
        );

        if (input != null && !input.trim().isEmpty()) {
            try {
                double percentValue = Double.parseDouble(input.replace('%', ' ').trim());
                if (percentValue >= 1 && percentValue <= 5000) {
                    // Llamamos directamente a nuestro método orquestador.
                    solicitarZoomPersonalizado(percentValue);
                } else {
                    JOptionPane.showMessageDialog(this.view, "Porcentaje inválido.", "Error de Entrada", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this.view, "Entrada inválida.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            }
        }
    } // --- FIN del metodo handleSetCustomZoomFromMenu ---
    
    
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
    
    
    /**
     * Muestra un diálogo simple (JOptionPane) con información básica
     * sobre la versión de la aplicación.
     * El número de versión y el autor están actualmente hardcodeados,
     * pero podrían leerse de un archivo de propiedades o de metadatos del MANIFEST.
     */
    private void mostrarVersion() {
        // 1. Definir la información a mostrar
        //    TODO: Considerar leer estos valores de un archivo externo o MANIFEST.MF
        String nombreApp = "Visor de Imágenes V2";
        String version = "1.1.0-MVC-SyncLists"; // Ejemplo de número de versión
        String autor = "(c) 2024 Javier Tortosa"; // ¡Tu nombre aquí!
        String mensaje = nombreApp + "\nVersión: " + version + "\n" + autor;
        String tituloDialogo = "Acerca de " + nombreApp;

        // 2. Log (Opcional)
        logger.debug("[Controller] Mostrando diálogo de versión...");

        // 3. Mostrar el JOptionPane
        //    - Usamos view.getFrame() como componente padre para centrar el diálogo.
        //    - message: El texto a mostrar.
        //    - title: El título de la ventana del diálogo.
        //    - messageType: El icono a mostrar (INFORMATION_MESSAGE es un icono 'i').
        JOptionPane.showMessageDialog(
            (view != null ? view : null), // Componente padre (o null si view no existe)
            mensaje,                                 // Mensaje a mostrar
            tituloDialogo,                           // Título de la ventana
            JOptionPane.INFORMATION_MESSAGE          // Tipo de icono
        );

        // 4. Log final (Opcional)
        // logger.debug("  -> Diálogo de versión cerrado.");

    } // --- FIN mostrarVersion ---
    
       
    /**
     * Imprime en la consola información detallada sobre un ActionEvent recibido.
     * Útil para depurar y entender qué componente/acción generó un evento.
     * Intenta obtener la clase de la fuente, el comando de acción, la clave larga
     * de configuración asociada (si se encuentra) y el nombre del icono (si es un botón).
     *
     * @param e El ActionEvent a analizar.
     */
    public void logActionInfo(ActionEvent e) {
        if (e == null) {
            logger.debug("--- Acción Detectada (Evento Nulo) ---");
            return;
        }

        Object source = e.getSource();
        String commandFromEvent = e.getActionCommand(); // Comando del evento
        String sourceClass = (source != null) ? source.getClass().getSimpleName() : "null";
        String sourceId = (source != null) ? " (ID: " + System.identityHashCode(source) + ")" : "";

        // Información adicional a obtener
        String longConfigKey = findLongKeyForComponent(source);
        String assignedActionClass = "NINGUNA";
        String canonicalCommand = "(No aplicable o no encontrado)";

        if (source instanceof AbstractButton) {
            AbstractButton comp = (AbstractButton) source;
            Action assignedAction = comp.getAction(); // Obtener Action asignada

            if (assignedAction != null) {
                assignedActionClass = assignedAction.getClass().getName();
                Object cmdValue = assignedAction.getValue(Action.ACTION_COMMAND_KEY);
                if (cmdValue instanceof String) {
                    canonicalCommand = (String) cmdValue;
                } else {
                    canonicalCommand = "(Action sin ACTION_COMMAND_KEY)";
                }
            } else {
                 // Si no hay Action, el comando canónico "esperado" sería el ActionCommand del componente
                 canonicalCommand = commandFromEvent;
            }
        } else {
             // Si no es un AbstractButton, el comando canónico podría ser el del evento
             canonicalCommand = commandFromEvent;
        }

        // Imprimir log formateado
        logger.info("--- DEBUG: Acción Detectada ---");
        logger.debug("  > Fuente        : " + sourceClass + sourceId);
        logger.debug("  > Event Command : " + (commandFromEvent != null ? "'" + commandFromEvent + "'" : "null"));
        logger.debug("  > Config Key    : " + (longConfigKey != null ? "'" + longConfigKey + "'" : "(No encontrada)"));
        logger.debug("  > Comando Canon.: " + (canonicalCommand != null ? "'" + canonicalCommand + "'" : "(null)"));
        logger.debug("  > Action Class  : " + assignedActionClass);
        logger.debug("------------------------------");
    }// ---FIN logActionInfo
    

    /**
     * Busca en los mapas de botones y menús de la vista para encontrar la
     * clave de configuración larga asociada a un componente Swing dado.
     * @param source El componente (JButton, JMenuItem, etc.).
     * @return La clave larga de configuración (ej. "interfaz.boton.movimiento.Siguiente_48x48")
     *         o null si no se encuentra o la vista/mapas no están inicializados.
     */
    public String findLongKeyForComponent(Object source) {
        // Validar dependencias
        if (view == null || !(source instanceof Component)) {
             // logger.warn("WARN [findLongKey]: Vista nula o fuente no es Componente."); // Log opcional
            return null;
        }
        Component comp = (Component) source;

        // Buscar en botones
        if (this.botonesPorNombre != null) {
            for (Map.Entry<String, AbstractButton> entry : this.botonesPorNombre.entrySet()) {
                if (entry.getValue() == comp) {
                    return entry.getKey();
                }
            }
        }

        // Buscar en menús
        if (this.menuItemsPorNombre != null) {
            for (Map.Entry<String, JMenuItem> entry : this.menuItemsPorNombre.entrySet()) {
                 if (entry.getValue() == comp) {
                     return entry.getKey();
                 }
            }
        }

        // Si no se encontró en ninguno de los mapas
        // logger.debug("INFO [findLongKey]: No se encontró clave larga para: " + source.getClass().getSimpleName()); // Log opcional
        return null;
    }

     /**
     * Intenta inferir el nombre del archivo de icono asociado a un JButton.
     * Es una heurística simple basada en la clave larga del componente.
     * @param source El componente fuente (se espera que sea un JButton).
     * @return El nombre inferido del archivo PNG del icono (ej. "Siguiente_48x48.png")
     *         o null si no es un JButton, no tiene icono, o no se puede inferir.
     */
    public String findIconNameForComponent(Object source) {
         if (source instanceof JButton) {
             JButton button = (JButton) source;
             Icon icon = button.getIcon(); // Obtener el icono actual del botón

             // Proceder solo si el icono es un ImageIcon (no otros tipos de Icon)
             if (icon instanceof ImageIcon) {
                 // Intentar obtener la clave larga para inferir el nombre
                 String longKey = findLongKeyForComponent(source);
                  if (longKey != null && longKey.startsWith("interfaz.boton.")) {
                      // Separar la clave por puntos
                     String[] parts = longKey.split("\\.");
                     // Si tenemos suficientes partes (interfaz.boton.categoria.nombreBoton)
                     if (parts.length >= 4) {
                          // La última parte debería ser el nombre base del icono
                          return parts[parts.length - 1] + ".png"; // Asumir extensión .png
                     }
                 }
                 // Si no se pudo inferir desde la clave, devolver un mensaje genérico
                  // return "(Icono: " + ((ImageIcon) icon).getDescription() + ")"; // Opcional: usar descripción si la tiene
                 return "(Icono presente, nombre no inferido)";
             }
             // Si no tiene icono ImageIcon
             // else { return "(Sin ImageIcon)"; }
         }
         // Si no es un JButton
         return null;
    }// fin findIconNameForComponent
    

     
     
//   FIXME (Opcionalmente, podría estar en una clase de Utilidades si se usa en más sitios)

  /**
   * Convierte una cadena de texto que representa un color en formato "R, G, B"
   * (donde R, G, B son números enteros entre 0 y 255) en un objeto java.awt.Color.
   *
   * Ignora espacios alrededor de los números y las comas.
   * Valida que los componentes numéricos estén en el rango [0, 255].
   *
   * @param rgbString La cadena de texto a parsear (ej. "238, 238, 238", " 0, 0,0 ").
   *                  Si es null, vacía o tiene un formato incorrecto, se devolverá
   *                  un color por defecto (gris claro).
   * @return El objeto Color correspondiente a la cadena RGB, o Color.LIGHT_GRAY
   *         si la cadena no se pudo parsear correctamente.
   */
  private Color parseColor(String rgbString) {
      // 1. Manejar entrada nula o vacía
      if (rgbString == null || rgbString.trim().isEmpty()) {
          logger.warn("WARN [parseColor]: Cadena RGB nula o vacía. Usando color por defecto (Gris Claro).");
          return Color.LIGHT_GRAY; // Color por defecto seguro
      }

      // 2. Separar la cadena por las comas
      String[] components = rgbString.split(",");

      // 3. Validar que tengamos exactamente 3 componentes
      if (components.length == 3) {
          try {
              // 3.1. Parsear cada componente a entero, quitando espacios (trim)
              int r = Integer.parseInt(components[0].trim());
              int g = Integer.parseInt(components[1].trim());
              int b = Integer.parseInt(components[2].trim());

              // 3.2. Validar el rango [0, 255] para cada componente
              //      Usamos Math.max/min para asegurar que el valor quede dentro del rango.
              r = Math.max(0, Math.min(255, r));
              g = Math.max(0, Math.min(255, g));
              b = Math.max(0, Math.min(255, b));

              // 3.3. Crear y devolver el objeto Color
              return new Color(r, g, b);

          } catch (NumberFormatException e) {
              // Error si alguno de los componentes no es un número entero válido
              logger.warn("WARN [parseColor]: Formato numérico inválido en '" + rgbString + "'. Usando color por defecto (Gris Claro). Error: " + e.getMessage());
              return Color.LIGHT_GRAY; // Devolver color por defecto
          } catch (Exception e) {
               // Capturar otros posibles errores inesperados durante el parseo
               logger.error("ERROR INESPERADO [parseColor] parseando '" + rgbString + "': " + e.getMessage());
               e.printStackTrace();
               return Color.LIGHT_GRAY; // Devolver color por defecto
          }
      	} else {
          // Error si no se encontraron exactamente 3 componentes después de split(',')
           logger.warn("WARN [parseColor]: Formato de color debe ser R,G,B. Recibido: '" + rgbString + "'. Usando color por defecto (Gris Claro).");
           return Color.LIGHT_GRAY; // Devolver color por defecto
      	}
  	} // --- FIN parseColor ---
  
  
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
	        cargarListaImagenes(imagenInicialKey, null);
	    } else {
	        SwingUtilities.invokeLater(this::limpiarUI);
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
	        sincronizarColoresDePanelesPorTema(nuevoTema);

	        // 4. Sincronizar el estado lógico (botones seleccionados, etc.)
	        logger.debug("  [EDT] Sincronizando estado de controles...");
	        sincronizarEstadoVisualBotonesYRadiosZoom();
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
	
	
	
	public void sincronizarColoresDePanelesPorTema(Tema temaAFlejar) {
	    if (registry == null || temaAFlejar == null) {
	        logger.warn("WARN [sincronizarColoresDePanelesPorTema]: Dependencias nulas (registry o tema nulo).");
	        return;
	    }
	    
	    logger.debug("  [Sync Colors] Sincronizando colores de paneles para el tema: {}", temaAFlejar.nombreDisplay());

	    // Obtenemos los colores específicos para las barras de estado desde el objeto Tema.
	    Color backgroundColor = temaAFlejar.colorBarraEstadoFondo();
	    Color foregroundColor = temaAFlejar.colorBarraEstadoTexto();

	    // LÓGICA DE FALLBACK: Si el tema actual NO define estos colores específicos, 
	    // usamos los colores genéricos del LookAndFeel como alternativa segura.
	    if (backgroundColor == null) {
	        backgroundColor = UIManager.getColor("Panel.background"); 
	        logger.debug("    -> Usando color de fondo de fallback (UIManager) para la barra de estado.");
	    }
	    if (foregroundColor == null) {
	        foregroundColor = UIManager.getColor("Label.foreground");
	        logger.debug("    -> Usando color de texto de fallback (UIManager) para la barra de estado.");
	    }

	    // --- APLICACIÓN A LOS PANELES DE STATUSBAR ---

	    // 1. Panel de estado inferior (StatusBar de la aplicación)
	    JPanel panelEstadoInferior = registry.get("panel.estado.inferior");
	    if (panelEstadoInferior != null) {
	        panelEstadoInferior.setBackground(backgroundColor);
	        // Usamos el método recursivo para asegurar que todos los JLabels y otros componentes
	        // dentro de este panel (incluso si están anidados) reciban el color de texto correcto.
	        actualizarColoresDeTextoRecursivamente(panelEstadoInferior, foregroundColor);
	        logger.debug("    -> Colores personalizados aplicados a 'panel.estado.inferior'.");
	    }
	    
	    // 2. Panel de información superior (StatusBar de la imagen)
	    JPanel panelInfoSuperior = registry.get("panel.info.superior");
	    if (panelInfoSuperior != null) {
	        panelInfoSuperior.setBackground(backgroundColor);
	        // Hacemos lo mismo para la barra superior.
	        actualizarColoresDeTextoRecursivamente(panelInfoSuperior, foregroundColor);
	        logger.debug("    -> Colores personalizados aplicados a 'panel.info.superior'.");
	    }

	} // --- Fin del método sincronizarColoresDePanelesPorTema ---
	
	
	/**
	 * Sincroniza los colores de los paneles usando el tema actualmente activo en el ThemeManager.
	 * Este es un método de conveniencia para la inicialización y otros refrescos generales
	 * que no tienen un 'nuevoTema' a mano.
	 */
	public void sincronizarColoresDePanelesPorTema() {
	    if (themeManager != null) {
	        // Llama a la versión detallada pasando el tema que ya está activo.
	        sincronizarColoresDePanelesPorTema(themeManager.getTemaActual());
	    } else {
	        logger.error("ERROR [sincronizarColoresDePanelesPorTema]: ThemeManager es nulo. No se puede sincronizar colores.");
	    }
	} // ---FIN de metodo [sincronizarColoresDePanelesPorTema]---
	
	
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
    
    public Map<String, Action> getActionMap() {return this.actionMap;}
    public int getCalculatedMiniaturePanelHeight() { return calculatedMiniaturePanelHeight; }
    public Map<String, AbstractButton> getBotonesPorNombre() {return this.botonesPorNombre;}
    public ExecutorService getExecutorService() {return this.executorService;}
    public IViewManager getViewManager() {return this.viewManager;}
    public IListCoordinator getListCoordinator() {return this.listCoordinator;}
    
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
 	        sincronizarEstadoVisualBotonesYRadiosZoom(); 
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
    public void setProjectController		(ProjectController projectController) {this.projectController = Objects.requireNonNull(projectController);}
    public void setConfigApplicationManager	(ConfigApplicationManager manager) { this.configAppManager = manager; }
    public void setDisplayModeManager		(controlador.managers.DisplayModeManager displayModeManager) {this.displayModeManager = displayModeManager;}
    public void setGeneralController		(GeneralController generalController) {this.generalController = generalController;}
    
    
    
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
        sincronizarEstadoVisualBotonesYRadiosZoom();

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
    
    
    /**
     * Sincroniza explícitamente el estado visual y lógico de TODOS los botones y radios 
     * de la UI que controlan el zoom (modos, paneo manual, zoom al cursor, reset),
     * basándose en el estado actual del VisorModel.
     * Este es el método maestro para mantener la UI de zoom coherente.
     */
    public void sincronizarEstadoVisualBotonesYRadiosZoom() {
        // 1. Validar que las dependencias críticas no sean nulas.
        if (this.actionMap == null || this.model == null || this.configAppManager == null) {
            logger.warn("WARN [sincronizarEstadoVisualBotonesYRadiosZoom]: Dependencias críticas (actionMap, model, configAppManager) nulas. Abortando sincronización.");
            return;
        }
        
        // 2. Leer el estado "de verdad" desde el modelo una sola vez.
        final ZoomModeEnum modoActivoDelModelo = model.getCurrentZoomMode();
        final boolean permisoManualActivoDelModelo = model.isZoomHabilitado();
        final boolean zoomAlCursorActivoDelModelo = model.isZoomToCursorEnabled();

        logger.debug("[VisorController] Sincronizando UI de Zoom: Paneo=" + permisoManualActivoDelModelo + ", Modo=" + modoActivoDelModelo + ", ZoomAlCursor=" + zoomAlCursorActivoDelModelo);

        // --- 3. SINCRONIZAR EL BOTÓN DE PANEO MANUAL (ToggleZoomManualAction) ---
        Action zoomManualAction = actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
        if (zoomManualAction != null) {
            // a) Sincronizar el estado lógico (SELECTED_KEY) de la Action.
            zoomManualAction.putValue(Action.SELECTED_KEY, permisoManualActivoDelModelo);
            
            // b) Llamar al ConfigApplicationManager para que aplique el aspecto visual correcto al botón.
            configAppManager.actualizarAspectoBotonToggle(zoomManualAction, permisoManualActivoDelModelo);
        }

        // --- 4. SINCRONIZAR LOS BOTONES DE MODO DE ZOOM (AplicarModoZoomAction) ---
        // Se itera sobre todas las acciones y se filtran las que son de tipo AplicarModoZoomAction.
        for (Action action : actionMap.values()) {
            if (action instanceof controlador.actions.zoom.AplicarModoZoomAction) {
                AplicarModoZoomAction zoomModeAction = (AplicarModoZoomAction) action;
                
                // a) Determinar si esta acción representa el modo actualmente activo en el modelo.
                boolean estaAccionDebeEstarSeleccionada = (zoomModeAction.getModoAsociado() == modoActivoDelModelo);
                
                // b) Sincronizar el estado lógico (SELECTED_KEY) de la Action.
                zoomModeAction.putValue(Action.SELECTED_KEY, estaAccionDebeEstarSeleccionada);
                
                // c) Llamar al ConfigApplicationManager para que aplique el aspecto visual.
                configAppManager.actualizarAspectoBotonToggle(zoomModeAction, estaAccionDebeEstarSeleccionada);
            }
        }
        
        // --- 5. SINCRONIZAR EL BOTÓN DE ZOOM AL CURSOR (ToggleZoomToCursorAction) ---
        Action zoomCursorAction = actionMap.get(AppActionCommands.CMD_ZOOM_TOGGLE_TO_CURSOR);
        if (zoomCursorAction != null) {
            // a) Sincronizar el estado lógico de la Action.
            zoomCursorAction.putValue(Action.SELECTED_KEY, zoomAlCursorActivoDelModelo);

            // b) Llamar al ConfigApplicationManager. Aunque este botón no esté en la toolbar principal,
            //    el método encontrará el componente asociado (en el menú, por ejemplo) y lo actualizará si es un JCheckBoxMenuItem.
            //    Si en el futuro lo pones como un JToggleButton en otro sitio, esto ya funcionará.
            configAppManager.actualizarAspectoBotonToggle(zoomCursorAction, zoomAlCursorActivoDelModelo);
        }

        // --- 6. SINCRONIZAR EL BOTÓN DE RESET (ResetZoomAction) ---
        // Este no es un botón de tipo "toggle", solo se habilita o deshabilita.
        Action resetAction = actionMap.get(AppActionCommands.CMD_ZOOM_RESET);
        if (resetAction != null) {
            // Su estado 'enabled' depende de si el paneo manual está activo.
            resetAction.setEnabled(permisoManualActivoDelModelo);
        }
        
        // --- 7. ACTUALIZAR LAS BARRAS DE INFORMACIÓN (Opcional, pero buena práctica) ---
        // Esto asegura que cualquier texto que muestre el modo de zoom se actualice.
        if (infobarImageManager != null) {
            infobarImageManager.actualizar();
        }
        if (statusBarManager != null) {
            statusBarManager.actualizar();
        }
        
        logger.debug("[VisorController] Sincronización completa de la UI de Zoom finalizada.");

    } // --- FIN del método sincronizarEstadoVisualBotonesYRadiosZoom ---
    
    
    /**
     * Sincroniza ÚNICAMENTE los componentes que dependen del estado de paneo,
     * como el botón de Reset.
     */
    public void sincronizarEstadoBotonReset() {
        if (actionMap == null || model == null) {
            return;
        }
        
        boolean permisoManualActivo = model.isZoomHabilitado();
        
        Action resetAction = actionMap.get(AppActionCommands.CMD_ZOOM_RESET);
        if (resetAction != null) {
            // Habilita o deshabilita la Action de Reset basándose en si el paneo está activo.
            resetAction.setEnabled(permisoManualActivo);
            logger.debug("[VisorController] Botón Reset " + (permisoManualActivo ? "habilitado." : "deshabilitado."));
        }
    }// --- FIN del metodo sincronizarEstadoBotonReset ---
    

    /**
     * Gestiona la lógica cuando el usuario establece un nuevo zoom desde la UI
     * (barra de estado o menú). Activa el modo correcto y aplica el zoom.
     * @param nuevoPorcentaje El nuevo porcentaje de zoom a aplicar.
     */
    public void solicitarZoomPersonalizado(double nuevoPorcentaje) {
        logger.debug("--- [VisorController] INICIO solicitarZoomPersonalizado (Lógica Centralizada): " + nuevoPorcentaje + "% ---");
        if (model == null || zoomManager == null || configuration == null || actionMap == null) {
            logger.error("  -> ERROR: Dependencias nulas. Abortando.");
            return;
        }

        // --- INICIO DE LA MODIFICACIÓN ---

        // 1. LÓGICA DE NEGOCIO: ACTUALIZAR EL MODELO Y LA CONFIGURACIÓN
        //    Guardamos el "deseo" del usuario en nuestro nuevo campo del modelo.
        model.setZoomCustomPercentage(nuevoPorcentaje);
        
        //    También actualizamos la configuración para que el cambio se guarde al cerrar.
        configuration.setZoomPersonalizadoPorcentaje(nuevoPorcentaje);
        logger.debug("  -> Modelo y Configuración actualizados con el nuevo porcentaje: " + nuevoPorcentaje + "%");

        // 2. ACTIVAR EL MODO DE ZOOM FIJADO
        //    Buscamos la Action correspondiente al modo "Bloqueador" y la ejecutamos.
        //    Esto asegura que se aplique toda la lógica definida en AplicarModoZoomAction,
        //    incluida la sincronización de la UI.
        Action zoomFijadoAction = actionMap.get(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO);
        if (zoomFijadoAction != null) {
            logger.debug("  -> Disparando Action para aplicar el modo USER_SPECIFIED_PERCENTAGE...");
            // Creamos un ActionEvent simple para disparar la acción.
            zoomFijadoAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO));
        } else {
            logger.error("  -> ERROR: No se encontró la Action para CMD_ZOOM_TIPO_ESPECIFICADO.");
        }

        // --- FIN DE LA MODIFICACIÓN ---
        
        // (El código antiguo que tenías aquí ya no es necesario porque la Action se encarga de todo)
        
        logger.debug("--- [VisorController] FIN solicitarZoomPersonalizado ---\n");
        
    } // --- FIN del metodo solicitarZoomPersonalizado ---
    
    
    public void notificarCambioEstadoZoomManual() {
        logger.debug("[VisorController] Notificado cambio de estado de zoom manual. Actualizando barras...");
        
     // << --- ACTUALIZAR BARRAS AL FINAL DE LA LIMPIEZA --- >>  
        if (infobarImageManager != null) {
            infobarImageManager.actualizar();
        }
        if (statusBarManager != null) {
            statusBarManager.actualizar();
        }
    }
    
    
    /**
     * REFACTORIZADO: Configura un listener que se dispara UNA SOLA VEZ, cuando la
     * ventana principal es mostrada y tiene dimensiones válidas por primera vez.
     * Su único propósito es corregir el zoom inicial.
     */
    void configurarListenerRedimensionVentana() {
        if (view == null) {
            logger.error("ERROR [Controller - configurarListenerRedimensionamiento]: Vista nula.");
            return;
        }
        
        logger.debug("    [Controller] Configurando ComponentListener para el primer arranque...");

        view.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
                
                // Solo actuar si el panel tiene un tamaño válido y hay una imagen cargada.
                if (displayPanel != null && displayPanel.getWidth() > 0 && model != null && model.getCurrentImage() != null) {
                    
                    logger.debug("--- [Listener de Ventana] Primer redimensionado válido detectado. Re-aplicando modo de zoom inicial. ---");
                    
                    if (zoomManager != null) {
                        // Llama al método que ya tienes, que usará las dimensiones ahora correctas del panel.
                        zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
                    }
                    
                    // ¡Importante! Eliminar el listener después de que se haya ejecutado una vez.
                    view.removeComponentListener(this);
                    logger.debug("--- [Listener de Ventana] Tarea completada. Listener eliminado. ---");
                }
            }
        });
    } // --- FIN del metodo configurarListenerRedimensionVentana ---
    
    
    public void sincronizarUiControlesZoom(Action action, boolean isSelected) {
        if (configAppManager != null) {
            // Delega la actualización visual al manager correspondiente.
            configAppManager.actualizarEstadoControlesZoom(isSelected, isSelected);
            configAppManager.actualizarAspectoBotonToggle(action, isSelected);
        } else {
            logger.warn("WARN [sincronizarUiControlesZoom]: configAppManager es nulo.");
        }
    }

    
    
    
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

     
// ************************************************************************FIN CLASE ANIDADA DE CONTROL DE MINIATURAS VISIBLES
// ***************************************************************************************************************************    

     
} // --- FIN CLASE VisorController ---
