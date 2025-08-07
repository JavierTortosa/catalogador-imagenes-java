package controlador;

import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import controlador.interfaces.IModoController;
import controlador.managers.CarouselManager;
import controlador.managers.ConfigApplicationManager;
import controlador.managers.DisplayModeManager;
import controlador.managers.InfobarStatusManager;
import controlador.managers.ToolbarManager; // <-- Importación necesaria
import controlador.managers.ViewManager;
import controlador.utils.ComponentRegistry; // <-- NUEVO: Importación para ComponentRegistry
import modelo.ListContext;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode; // <-- Importación necesaria
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.components.Direction; // <-- NUEVO: Importación para Direction
import vista.panels.ImageDisplayPanel; // <-- NUEVO: Importación para ImageDisplayPanel

/**
 * Controlador de aplicación de alto nivel.
 * Orquesta la interacción entre los controladores de modo (VisorController, ProjectController)
 * y gestiona el estado global de la aplicación, como el modo de trabajo actual y la
 * habilitación/deshabilitación de la UI correspondiente.
 */
public class GeneralController implements IModoController, KeyEventDispatcher{

	private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);
	
    // --- Dependencias Clave ---
    private VisorModel model;
    private VisorController visorController;
    private ProjectController projectController;
    private ViewManager viewManager;
    private Map<String, Action> actionMap;
    private InfobarStatusManager statusBarManager;
    private ConfigApplicationManager configAppManager;
    private ToolbarManager toolbarManager;
    private ComponentRegistry registry; // <-- NUEVO: Referencia al ComponentRegistry
    
    private int lastMouseX, lastMouseY;
    
    private DisplayModeManager displayModeManager; 
    private ConfigurationManager configuration;

    
    private volatile boolean isChangingSubfolderMode = false;
    
    
    /**
     * Constructor de GeneralController.
     * Las dependencias se inyectarán a través de setters después de la creación.
     */
    public GeneralController() {
        // Constructor vacío. La inicialización se delega al método initialize.
    } // --- Fin del método GeneralController (constructor) ---

    /**
     * Inicializa el controlador después de que todas las dependencias hayan sido inyectadas.
     * Este método se usa para configurar el estado inicial de la UI.
     */
    public void initialize() {
        logger.debug("[GeneralController] Inicializado.");
        // Se asegura de que al arrancar, el botón del modo inicial (Visualizador) aparezca correctamente seleccionado.
        sincronizarEstadoBotonesDeModo();
    } // --- Fin del método initialize ---
    
    // --- Setters para Inyección de Dependencias ---

    public void setModel(VisorModel model) {
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en GeneralController");
    } // --- Fin del método setModel ---

    public void setVisorController(VisorController visorController) {
        this.visorController = Objects.requireNonNull(visorController, "VisorController no puede ser null en GeneralController");
    } // --- Fin del método setVisorController ---

    public void setProjectController(ProjectController projectController) {
        this.projectController = Objects.requireNonNull(projectController, "ProjectController no puede ser null en GeneralController");
    } // --- Fin del método setProjectController ---
    
    public void setViewManager(ViewManager viewManager) {
        this.viewManager = Objects.requireNonNull(viewManager, "ViewManager no puede ser null en GeneralController");
    } // --- Fin del método setViewManager ---
    
    public void setActionMap(Map<String, Action> actionMap) {
        this.actionMap = Objects.requireNonNull(actionMap, "ActionMap no puede ser null en GeneralController");
    } // --- Fin del método setActionMap ---
    
    public VisorController getVisorController() {
        return this.visorController;
    } // --- Fin del método getVisorController ---

    public ProjectController getProjectController() {
        return this.projectController;
    } // --- Fin del método getProjectController ---
    
    public void setStatusBarManager(InfobarStatusManager statusBarManager) {
        this.statusBarManager = Objects.requireNonNull(statusBarManager, "InfobarStatusManager no puede ser null en GeneralController");
    } // --- Fin del método setStatusBarManager ---
    
    public void setConfigApplicationManager(ConfigApplicationManager configAppManager) {
        this.configAppManager = Objects.requireNonNull(configAppManager, "ConfigApplicationManager no puede ser null en GeneralController");
    } // --- Fin del método setConfigApplicationManager ---
    
    public void setToolbarManager(ToolbarManager toolbarManager) {
        this.toolbarManager = Objects.requireNonNull(toolbarManager, "ToolbarManager no puede ser null en GeneralController");
    } // --- Fin del método setToolbarManager ---

    public void setRegistry(ComponentRegistry registry) { // <-- NUEVO SETTER
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null en GeneralController");
    } // --- Fin del método setRegistry ---

//****************************************************************************************** Fin Setters
    
    /**
     * Orquesta la transición entre los diferentes modos de trabajo de la aplicación.
     * Es el punto de entrada central para cambiar de vista. Contiene la lógica
     * de sincronización y confirmación para el "Carrusel Megapower".
     * @param modoDestino El modo al que se desea cambiar (VISUALIZADOR o PROYECTO).
     */
    public void cambiarModoDeTrabajo(VisorModel.WorkMode modoDestino) {
        WorkMode modoActual = this.model.getCurrentWorkMode();
        if (modoActual == modoDestino) {
            logger.debug("[GeneralController] Intento de cambiar al modo que ya está activo: " + modoDestino + ". No se hace nada.");
            return;
        }

        logger.debug("--- [GeneralController] INICIANDO TRANSICIÓN DE MODO: " + modoActual + " -> " + modoDestino + " ---");

        // --- LÓGICA DE SEGURIDAD Y CONFIRMACIÓN PARA SINCRONIZACIÓN ---
        boolean esTransicionSincronizable = (modoActual == WorkMode.VISUALIZADOR && modoDestino == WorkMode.CARROUSEL) ||
                                           (modoActual == WorkMode.CARROUSEL && modoDestino == WorkMode.VISUALIZADOR);

        if (esTransicionSincronizable && model.isSyncVisualizadorCarrusel()) {
            String titulo = "Confirmar Transición Sincronizada";
            String mensaje = modoDestino == WorkMode.CARROUSEL
                ? "<html>El modo <b>Sincronización</b> está activo.<br>Se cargará el estado del Visualizador en el Carrusel.<br><br>¿Continuar?</html>"
                : "<html>El modo <b>Sincronización</b> está activo.<br>La posición actual del Carrusel se transferirá al Visualizador.<br><br>¿Continuar?</html>";
            
            int respuesta = javax.swing.JOptionPane.showConfirmDialog(null, mensaje, titulo, javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.INFORMATION_MESSAGE);
            if (respuesta != javax.swing.JOptionPane.YES_OPTION) {
                logger.debug("--- [GeneralController] TRANSICIÓN CANCELADA por el usuario. ---");
                sincronizarEstadoBotonesDeModo(); // Revertir visualmente el botón
                return;
            }
        }
        // --- FIN LÓGICA DE SEGURIDAD ---

        if (modoDestino == VisorModel.WorkMode.PROYECTO) {
            if (!this.projectController.prepararDatosProyecto()) {
                sincronizarEstadoBotonesDeModo();
                logger.debug("--- [GeneralController] TRANSICIÓN CANCELADA: El modo proyecto no está listo. ---");
                return;
            }
        }
        
        salirModo(modoActual);
        this.model.setCurrentWorkMode(modoDestino);
        entrarModo(modoDestino);

        logger.debug("--- [GeneralController] TRANSICIÓN DE MODO COMPLETADA a " + modoDestino + " ---\n");
    } // --- Fin del método cambiarModoDeTrabajo ---

    
    /**
     * Realiza las tareas de "limpieza" o guardado de estado de un modo antes de abandonarlo.
     * Contiene lógica clave para el modo Carrusel independiente.
     * @param modoQueSeAbandona El modo que estamos dejando.
     */
	private void salirModo(VisorModel.WorkMode modoQueSeAbandona) {
        logger.debug("  [GeneralController] Saliendo del modo: " + modoQueSeAbandona);
        
        // Si salimos del modo Carrusel y la sincronización está DESACTIVADA,
        // guardamos su estado actual en el modelo para que pueda ser persistido al cerrar la app.
        if (modoQueSeAbandona == WorkMode.CARROUSEL && !model.isSyncVisualizadorCarrusel()) {
            ListContext carruselCtx = model.getCarouselListContext();
            model.setUltimaCarpetaCarrusel(carruselCtx.getCarpetaRaizContexto());
            model.setUltimaImagenKeyCarrusel(carruselCtx.getSelectedImageKey());
            logger.debug("    -> Modo Carrusel Independiente: Guardando estado en el modelo.");
        }
        
        // Si salimos del modo carrusel, notificamos a su manager
        if (modoQueSeAbandona == WorkMode.CARROUSEL) {
            CarouselManager carouselManager = visorController.getActionFactory().getCarouselManager();
            if (carouselManager != null) {
                carouselManager.onCarouselModeChanged(false); // Notificar salida
            }
        }
    } // --- Fin del método salirModo ---

	
	/**
	 * Realiza las tareas de "configuración" y "restauración" de la UI para un modo
	 * en el que estamos entrando. Contiene la lógica de clonado condicional.
	 * @param modoAlQueSeEntra El nuevo modo activo.
	 */
	private void entrarModo(WorkMode modoAlQueSeEntra) {
	    logger.debug("  [GeneralController] Entrando en modo: " + modoAlQueSeEntra);
	    
        // ----> INICIO DE LA CORRECCIÓN DE SINTAXIS <----
	    SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // --- PRIMER BLOQUE DENTRO DEL EDT ---
                logger.debug("    -> [EDT-1] Cambiando tarjeta del CardLayout a: " + modoAlQueSeEntra);
	        
                switch (modoAlQueSeEntra) {
                    case VISUALIZADOR:
                        if (model.isSyncVisualizadorCarrusel()) {
                            logger.debug("      -> Sincronización ON: Clonando contexto Carrusel -> Visualizador.");
                            model.getVisualizadorListContext().clonarDesde(model.getCarouselListContext());
                        }
                        viewManager.cambiarAVista("container.workmodes", "VISTA_VISUALIZADOR");
                        break;
                    case PROYECTO: viewManager.cambiarAVista("container.workmodes", "VISTA_PROYECTOS"); break;
                    case DATOS: viewManager.cambiarAVista("container.workmodes", "VISTA_DATOS"); break;
                    case EDICION: viewManager.cambiarAVista("container.workmodes", "VISTA_EDICION"); break;
                    case CARROUSEL: viewManager.cambiarAVista("container.workmodes", "VISTA_CARROUSEL_WORKMODE"); break;
                }

                JPanel workModesContainer = registry.get("container.workmodes");
                if (workModesContainer != null) {
                    workModesContainer.revalidate();
                    workModesContainer.repaint();
                }

                // --- SEGUNDO INVOKELATER ANIDADO ---
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        logger.debug("    -> [EDT-2] Restaurando y sincronizando UI para: " + modoAlQueSeEntra);

                        switch (modoAlQueSeEntra) {
                            case VISUALIZADOR:
                                visorController.restaurarUiVisualizador();
                                break;
                            case PROYECTO:
                                projectController.activarVistaProyecto();
                                break;
                            case CARROUSEL:
                                ListContext contextoCarrusel = model.getCarouselListContext();
                                if (model.isSyncVisualizadorCarrusel()) {
                                    logger.debug("      -> Sincronización ON: Clonando contexto Visualizador -> Carrusel.");
                                    contextoCarrusel.clonarDesde(model.getVisualizadorListContext());
                                } else if (contextoCarrusel.getModeloLista() == null || contextoCarrusel.getModeloLista().isEmpty()) {
                                    logger.debug("      -> Sincronización OFF y Carrusel vacío: Clonando desde Visualizador (primera vez).");
                                    contextoCarrusel.clonarDesde(model.getVisualizadorListContext());
                                }
                                visorController.restaurarUiCarrusel();
                                
                                CarouselManager carouselManager = visorController.getActionFactory().getCarouselManager();
                                if (carouselManager != null) {
                                    carouselManager.onCarouselModeChanged(true);
                                }
                                break;
                            case DATOS: case EDICION: break;
                        }

                        actualizarEstadoUiParaModo(modoAlQueSeEntra);
                        
                        if (toolbarManager != null) {
                            toolbarManager.reconstruirContenedorDeToolbars(modoAlQueSeEntra);
                        }
                        
                        if (modoAlQueSeEntra == WorkMode.CARROUSEL) {
                            CarouselManager carouselManager = visorController.getActionFactory().getCarouselManager();
                            if (carouselManager != null) {
                                logger.debug("    -> [EDT-2] Conectando listeners de la UI del Carrusel...");
                                carouselManager.findAndWireUpFastMoveButtons();
                                carouselManager.wireUpEventListeners();
                            }
                        }
                        
                        sincronizarEstadoBotonesDeModo();
                        
                        logger.debug("    -> [EDT-2] Restauración de UI para " + modoAlQueSeEntra + " completada.");
                    }
                }); // --- Fin del segundo Runnable ---
            }
        });
	    
	} // --- Fin del método entrarModo ---
	
	
	/**
     * MÉTODO MAESTRO DE SINCRONIZACIÓN DE UI.
     * Habilita/deshabilita y selecciona/deselecciona componentes de la UI (acciones, botones)
     * basándose en el modo de trabajo actual y el estado del Modelo.
     * 
     * @param modoActual El modo que se acaba de activar.
     */
    private void actualizarEstadoUiParaModo(WorkMode modoActual) {
        logger.debug("  [GeneralController] Actualizando estado de la UI para el modo: " + modoActual);

        // --- 1. LÓGICA DE HABILITACIÓN/DESHABILITACIÓN (Enabled/Disabled) ---
        boolean subcarpetasHabilitado = (modoActual == WorkMode.VISUALIZADOR || modoActual == WorkMode.CARROUSEL);

	     // 2. Obtenemos todas las acciones relacionadas con esta funcionalidad.
	     Action subfolderAction = this.actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
	     Action soloCarpetaAction = this.actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA);
	     Action conSubcarpetasAction = this.actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS);
	
	     // 3. Aplicamos la misma regla a TODAS las acciones.
	     if (subfolderAction != null) {
	         subfolderAction.setEnabled(subcarpetasHabilitado);
	     }
	     if (soloCarpetaAction != null) {
	         soloCarpetaAction.setEnabled(subcarpetasHabilitado);
	     }
	     if (conSubcarpetasAction != null) {
	         conSubcarpetasAction.setEnabled(subcarpetasHabilitado);
	     }
	     
        // --- 2. LÓGICA DE SELECCIÓN (Selected/Deselected) para Toggles ---
        
        if (configAppManager != null) {
            // Sincronizar el toggle de subcarpetas
            if (subfolderAction != null) {
                // ---> INICIO DE LA CORRECCIÓN CLAVE <---
                // Leemos el estado del contexto de lista ACTUALMENTE ACTIVO en el modelo.
                // model.isMostrarSoloCarpetaActual() ya es inteligente y devuelve el del contexto correcto.
            	
            	//LOG [DEBUG-SYNC] Modo:
            	logger.debug("  [DEBUG-SYNC] Modo: " + modoActual + ", Valor de isMostrarSoloCarpetaActual() en modelo: " + model.isMostrarSoloCarpetaActual());
            	
                boolean estadoModeloSubcarpetas = !model.isMostrarSoloCarpetaActual(); 
                // ---> FIN DE LA CORRECCIÓN CLAVE <---
                
                subfolderAction.putValue(Action.SELECTED_KEY, estadoModeloSubcarpetas);
                configAppManager.actualizarAspectoBotonToggle(subfolderAction, estadoModeloSubcarpetas);
            }
            
            // Sincronizar el toggle de proporciones
            Action proporcionesAction = actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);
            if (proporcionesAction != null) {
                // De forma similar, model.isMantenerProporcion() leerá del contexto de zoom correcto.
                boolean estadoModeloProporciones = model.isMantenerProporcion();
                proporcionesAction.putValue(Action.SELECTED_KEY, estadoModeloProporciones);
                configAppManager.actualizarAspectoBotonToggle(proporcionesAction, estadoModeloProporciones);
            }
        }
        
        // --- 3. ACTUALIZACIÓN DE OTROS COMPONENTES ---
        
        if (this.statusBarManager != null) {
            this.statusBarManager.actualizar();
        }
        
        logger.debug("  [GeneralController] Estado de la UI actualizado.");
    } // --- Fin del método actualizarEstadoUiParaModo ---
    
    
    /**
     * Orquesta la transición para entrar o salir del modo de pantalla completa.
     * Este método es llamado por la ToggleFullScreenAction y delega la manipulación
     * directa del JFrame al ViewManager, manteniendo la lógica de decisión centralizada.
     */
    public void solicitarToggleFullScreen() {
        logger.debug("[GeneralController] Solicitud para alternar pantalla completa.");

        if (viewManager == null || model == null) {
            logger.error("ERROR [solicitarToggleFullScreen]: ViewManager o Model son nulos.");
            return;
        }

        // 1. Determinar el nuevo estado invirtiendo el estado actual del MODELO.
        boolean nuevoEstado = !model.isModoPantallaCompletaActivado();

        // 2. Actualizar el MODELO con el nuevo estado.
        model.setModoPantallaCompletaActivado(nuevoEstado);

        // 3. Comandar al ViewManager para que aplique el cambio visual.
        viewManager.setFullScreen(nuevoEstado);
        
        // 4. Sincronizar la Action para que refleje el nuevo estado del MODELO.
        if (actionMap != null) {
            Action fullScreenAction = actionMap.get(AppActionCommands.CMD_VISTA_PANTALLA_COMPLETA);
            if (fullScreenAction != null) {
                fullScreenAction.putValue(Action.SELECTED_KEY, nuevoEstado);
            }
        }
    } // --- Fin del método solicitarToggleFullScreen ---
    
// *********************************************************************************************************************** INICIO SINCRONIZACION    
    
    /**
	 * Sincroniza el estado LÓGICO Y VISUAL de los botones de modo de trabajo.
	 * Asegura que solo el botón del modo activo esté seleccionado y que se aplique
     * el estilo visual personalizado.
	 */
	public void sincronizarEstadoBotonesDeModo() {
	    if (this.actionMap == null || this.model == null || this.configAppManager == null) {
	        logger.warn("WARN [GeneralController.sincronizarEstadoBotonesDeModo]: Dependencias nulas.");
	        return;
	    }

        // --- INICIO DE LA CORRECCIÓN ---
        
        // 1. Obtener el WorkMode actual del modelo.
        WorkMode modoActivo = this.model.getCurrentWorkMode();
        String comandoModoActivo;

        // 2. Mapear el WorkMode actual a su comando de acción correspondiente.
        switch (modoActivo) {
            case VISUALIZADOR:
                comandoModoActivo = AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR;
                break;
            case PROYECTO:
                comandoModoActivo = AppActionCommands.CMD_PROYECTO_GESTIONAR;
                break;
            case DATOS:
                comandoModoActivo = AppActionCommands.CMD_MODO_DATOS;
                break;
            case EDICION:
                comandoModoActivo = AppActionCommands.CMD_MODO_EDICION;
                break;
            case CARROUSEL:
                comandoModoActivo = AppActionCommands.CMD_VISTA_CAROUSEL;
                break;
            default:
                // Fallback por si acaso
                comandoModoActivo = AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR;
                break;
        }

	    // 3. Crear una lista de TODOS los comandos de los botones de modo.
	    List<String> comandosDeModo = List.of(
	        AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR,
	        AppActionCommands.CMD_PROYECTO_GESTIONAR,
	        AppActionCommands.CMD_MODO_DATOS,
	        AppActionCommands.CMD_MODO_EDICION,
	        AppActionCommands.CMD_VISTA_CAROUSEL
	    );

        // --- FIN DE LA CORRECCIÓN ---

        // El resto del método se queda igual, ya que la lógica de iteración es correcta.
	    for (String comando : comandosDeModo) {
	        Action action = this.actionMap.get(comando);
	        if (action != null) {
                // a) Actualizar el estado lógico de la Action
	            boolean isSelected = comando.equals(comandoModoActivo);
	            action.putValue(Action.SELECTED_KEY, isSelected);

                // b) Actualizar el estado visual del botón asociado
                this.configAppManager.actualizarAspectoBotonToggle(action, isSelected);
	        }
	    }
	    logger.debug("[GeneralController] Sincronizados botones de modo. Activo: " + comandoModoActivo);
	} // --- Fin del método sincronizarEstadoBotonesDeModo ---
	
	
	/**
     * Orquesta una sincronización completa del estado lógico de todas las Actions
     * y la apariencia de la UI basándose en el estado actual del modelo.
     * Este es el método que se debe llamar al arrancar la aplicación para asegurar
     * que la vista inicial sea coherente.
     */
    public void sincronizarTodaLaUIConElModelo() {
        logger.info("--- [GeneralController] Iniciando sincronización maestra de ui ---");

        if (model == null || actionMap == null || visorController == null) {
            logger.error("  -> ERROR: Modelo, ActionMap o VisorController nulos. Abortando sincronización.");
            return;
        }

        // 1. Sincronizar los botones de MODO DE TRABAJO.
        //    Esto asegura que el botón del modo actual (Visualizador/Proyecto) esté seleccionado.
        sincronizarEstadoBotonesDeModo();
        
        // 2. Sincronizar los botones de MODO DE VISUALIZACIÓN (DisplayMode).
            displayModeManager.sincronizarEstadoBotonesDisplayMode();

        // 3. Delegar el resto de la sincronización específica del modo al VisorController.
        //    IMPORTANTE: Debemos quitar la sincronización de subcarpetas de allí para evitar redundancia.
        visorController.sincronizarComponentesDeModoVisualizador();

        // 2. Sincronizar los controles de subcarpetas de forma centralizada.
        sincronizarControlesDeSubcarpetas();
        
        
        // 4. Delegar la sincronización específica del modo PROYECTO a su controlador (cuando sea necesario).
        // projectController.sincronizarComponentesDeModoProyecto(); // <- Futura implementación

        logger.debug("--- [GeneralController] SINCRONIZACIÓN MAESTRA DE UI COMPLETADA ---");
        
    } // --- FIN del metodo sincronizarTodaLaUIConElModelo ---
	
	
// ************************************************************************************************************************** FIN SINCRONIZACION 


    /**
     * Panea la imagen al borde especificado del panel de visualización.
     * Esta es la lógica de paneo ABSOLUTO.
     * @param direction La dirección (UP, DOWN, LEFT, RIGHT) a la que panear la imagen.
     */
    public void panImageToEdge(Direction direction) {
        logger.debug("[GeneralController] Solicitud de paneo ABSOLUTO a: " + direction);
        if (model == null || model.getCurrentImage() == null || registry == null) {
            logger.error("ERROR [GeneralController.panImageToEdge]: Dependencias nulas o sin imagen actual.");
            return;
        }

        ImageDisplayPanel displayPanel = viewManager.getActiveDisplayPanel();

        if (displayPanel == null || displayPanel.getWidth() <= 0 || displayPanel.getHeight() <= 0) {
            logger.error("ERROR [GeneralController.panImageToEdge]: ImageDisplayPanel no encontrado o sin dimensiones válidas.");
            return;
        }

        BufferedImage currentImage = model.getCurrentImage();
        double zoomFactor = model.getZoomFactor();

        int imageScaledWidth = (int) (currentImage.getWidth() * zoomFactor);
        int imageScaledHeight = (int) (currentImage.getHeight() * zoomFactor);
        
        int panelWidth = displayPanel.getWidth();
        int panelHeight = displayPanel.getHeight();

        // Calcular el punto inicial del centrado (si la imagen estuviera centrada sin paneo)
        // La imagen se dibuja desde (xBase + offsetX, yBase + offsetY)
        double xBaseCentered = (double) (panelWidth - imageScaledWidth) / 2;
        double yBaseCentered = (double) (panelHeight - imageScaledHeight) / 2;

        int newOffsetX = model.getImageOffsetX(); // Partimos del offset actual del modelo
        int newOffsetY = model.getImageOffsetY();

        // Si la imagen es más pequeña que el panel en esa dimensión, el paneo al borde no tiene sentido.
        // En ese caso, la imagen ya está "en el borde" (y centrada) o no se puede mover más allá del centro.
        // Aquí solo calculamos si la imagen es MÁS GRANDE que el panel en esa dimensión,
        // de lo contrario, los offsets serán 0 (centrado) por defecto si se aplican los límites.

        switch (direction) {
            case UP:
                if (imageScaledHeight > panelHeight) {
                    newOffsetY = (int) -yBaseCentered; // Borde superior: el inicio de la imagen debe estar al inicio del panel
                } else { // Imagen más pequeña que el panel en vertical, centramos o dejamos en 0.
                    newOffsetY = 0; // O mantener el offset actual si ya está centrada.
                }
                break;
            case DOWN:
                if (imageScaledHeight > panelHeight) {
                    // Borde inferior: el final de la imagen (yBase + offsetY + imageScaledHeight) debe coincidir con el final del panel (panelHeight)
                    newOffsetY = (int) (panelHeight - imageScaledHeight - yBaseCentered);
                } else {
                    newOffsetY = 0;
                }
                break;
            case LEFT:
                if (imageScaledWidth > panelWidth) {
                    newOffsetX = (int) -xBaseCentered; // Borde izquierdo: el inicio de la imagen debe estar al inicio del panel
                } else {
                    newOffsetX = 0;
                }
                break;
            case RIGHT:
                if (imageScaledWidth > panelWidth) {
                    // Borde derecho: el final de la imagen (xBase + offsetX + imageScaledWidth) debe coincidir con el final del panel (panelWidth)
                    newOffsetX = (int) (panelWidth - imageScaledWidth - xBaseCentered);
                } else {
                    newOffsetX = 0;
                }
                break;
            case NONE:
                // No hacer nada
                return;
        }

        // Si la imagen escalada es más pequeña que el panel en una dimensión,
        // aseguramos que el offset no mueva la imagen fuera de un estado "centrado" en esa dimensión.
        // Esto es para que si paneas a la izquierda, y la imagen es más pequeña que el panel en X,
        // no se pegue al borde sino que se quede centrada.
        if (imageScaledWidth <= panelWidth) {
            newOffsetX = 0; // Si la imagen cabe, el offset es 0 (centrado)
        }
        if (imageScaledHeight <= panelHeight) {
            newOffsetY = 0; // Si la imagen cabe, el offset es 0 (centrado)
        }
        
        // Actualizar el modelo con los nuevos offsets
        model.setImageOffsetX(newOffsetX);
        model.setImageOffsetY(newOffsetY);

        // Solicitar el repintado del panel para que muestre la imagen en la nueva posición
        displayPanel.repaint();
        logger.debug("[GeneralController] Paneo absoluto a " + direction + " aplicado. Offset: (" + newOffsetX + ", " + newOffsetY + ")");
    } // --- Fin del método panImageToEdge ---


    /**
     * Panea la imagen de forma incremental en la dirección especificada por una cantidad fija.
     * Esta es la lógica de paneo INCREMENTAL.
     * @param direction La dirección (UP, DOWN, LEFT, RIGHT) del paneo incremental.
     * @param amount La cantidad de píxeles a mover en cada paso.
     */
    public void panImageIncrementally(Direction direction, int amount) {
        logger.debug("[GeneralController] Solicitud de paneo INCREMENTAL (" + amount + "px) a: " + direction);
        if (model == null || model.getCurrentImage() == null || registry == null) {
            logger.error("ERROR [GeneralController.panImageIncrementally]: Dependencias nulas o sin imagen actual.");
            return;
        }

        ImageDisplayPanel displayPanel = viewManager.getActiveDisplayPanel();
        
        // TODO: Igual que arriba, si hay múltiples paneles de display, obtener el correcto.
        if (displayPanel == null || displayPanel.getWidth() <= 0 || displayPanel.getHeight() <= 0) {
            logger.error("ERROR [GeneralController.panImageIncrementally]: ImageDisplayPanel no encontrado o sin dimensiones válidas.");
            return;
        }

        BufferedImage currentImage = model.getCurrentImage();
        double zoomFactor = model.getZoomFactor();

        int imageScaledWidth = (int) (currentImage.getWidth() * zoomFactor);
        int imageScaledHeight = (int) (currentImage.getHeight() * zoomFactor);

        int panelWidth = displayPanel.getWidth();
        int panelHeight = displayPanel.getHeight();

        int currentOffsetX = model.getImageOffsetX();
        int currentOffsetY = model.getImageOffsetY();

        int newOffsetX = currentOffsetX;
        int newOffsetY = currentOffsetY;

        // Calcular el nuevo offset incremental, y luego aplicar límites para que no se salga de la imagen.
        switch (direction) {
            case UP:    newOffsetY = currentOffsetY - amount; break;
            case DOWN:  newOffsetY = currentOffsetY + amount; break;
            case LEFT:  newOffsetX = currentOffsetX - amount; break;
            case RIGHT: newOffsetX = currentOffsetX + amount; break;
            case NONE: return;
        }
        
        // Lógica para limitar el paneo dentro de los límites de la imagen/panel
        // La imagen se dibuja a partir de (xBase + offsetX, yBase + offsetY)
        // xBase/yBase son los offsets para centrar la imagen si no hay paneo.
        double xBaseCentered = (double) (panelWidth - imageScaledWidth) / 2;
        double yBaseCentered = (double) (panelHeight - imageScaledHeight) / 2;

        // Calcular los límites máximos y mínimos para los offsets
        // (xBase + offsetX) debe estar entre (panelWidth - imageScaledWidth) y 0
        // Para que el borde izquierdo de la imagen no vaya más allá del borde izquierdo del panel (0),
        // y el borde derecho de la imagen no vaya más allá del borde derecho del panel (panelWidth).
        
        // minXOffset: (panelWidth - imageScaledWidth) - xBaseCentered
        // maxXOffset: 0 - xBaseCentered
        
        // Límites de paneo (el borde de la imagen no debe ir más allá del borde del panel)
        // minOffset: Lo más a la izquierda/arriba que puede ir el *inicio* de la imagen (relativo al panel)
        // maxOffset: Lo más a la derecha/abajo que puede ir el *inicio* de la imagen (relativo al panel)

        // Si la imagen es más grande que el panel:
        if (imageScaledWidth > panelWidth) {
            int minPossibleX = panelWidth - imageScaledWidth - (int)xBaseCentered;
            int maxPossibleX = (int)-xBaseCentered;
            newOffsetX = Math.max(minPossibleX, Math.min(newOffsetX, maxPossibleX));
        } else {
            newOffsetX = (int)-xBaseCentered; // Si la imagen es más pequeña, siempre se centra (offset es negativo de xBase)
            // Opcional: si la imagen es más pequeña, se podría forzar newOffsetX = 0 (si se prefiere pegar al centro visual)
        }
        
        if (imageScaledHeight > panelHeight) {
            int minPossibleY = panelHeight - imageScaledHeight - (int)yBaseCentered;
            int maxPossibleY = (int)-yBaseCentered;
            newOffsetY = Math.max(minPossibleY, Math.min(newOffsetY, maxPossibleY));
        } else {
            newOffsetY = (int)-yBaseCentered; // Si la imagen es más pequeña, siempre se centra
            // Opcional: si la imagen es más pequeña, se podría forzar newOffsetY = 0
        }

        // Actualizar el modelo con los nuevos offsets
        model.setImageOffsetX(newOffsetX);
        model.setImageOffsetY(newOffsetY);

        // Solicitar el repintado del panel
        displayPanel.repaint();
        logger.debug("[GeneralController] Paneo incremental aplicado. Offset: (" + newOffsetX + ", " + newOffsetY + ")");
    } // --- Fin del método panImageIncrementally ---
    
    /**
     * Actúa como un router para la acción de marcar/desmarcar una imagen.
     * Delega la solicitud al controlador del modo de trabajo activo.
     */
    public void solicitudAlternarMarcaImagenActual() {
        logger.debug("[GeneralController] Recibida solicitud para alternar marca. Modo actual: " + model.getCurrentWorkMode());
        if (model.isEnModoProyecto()) {
            projectController.solicitudAlternarMarcaImagen();
        } else {
            visorController.solicitudAlternarMarcaDeImagenActual();
        }
    } // --- Fin del método solicitudAlternarMarcaImagenActual ---
    
    
//  ************************************************************************************** IMPLEMENTACION INTERFAZ IModoController
    
    
 // REEMPLAZA ESTE MÉTODO COMPLETO EN GeneralController.java

    /**
     * Configura los listeners globales de teclado y ratón para la aplicación,
     * utilizando un sistema de etiquetado para desacoplar el controlador de la vista.
     * Añade un MouseWheelListener universal a todos los componentes etiquetados
     * como "WHEEL_NAVIGABLE" en el ComponentRegistry.
     */
    public void configurarListenersDeEntradaGlobal() {
        logger.debug("[GeneralController] Configurando listeners de entrada globales para todos los modos...");

        // --- Definición del Master Mouse Wheel Listener (Lógica Centralizada) ---
        java.awt.event.MouseWheelListener masterWheelListener = e -> {
            // Obtenemos las referencias a TODOS los componentes relevantes una sola vez, al principio del evento.
            JLabel etiquetaImagenVisualizador = registry.get("label.imagenPrincipal");
            JLabel etiquetaImagenProyecto = registry.get("label.proyecto.imagen");
            JLabel etiquetaImagenCarrusel = registry.get("label.carousel.imagen");
            JTable tablaExportacion = registry.get("tabla.exportacion");

            // Detección de la ubicación del cursor
            boolean sobreLaImagen = (e.getComponent() == etiquetaImagenVisualizador) ||
                                    (e.getComponent() == etiquetaImagenProyecto) ||
                                    (e.getComponent() == etiquetaImagenCarrusel);
            
            boolean sobreTablaExportacion = (tablaExportacion != null && SwingUtilities.isDescendingFrom(e.getComponent(), tablaExportacion));
            
            // --- LÓGICA DE PRIORIDADES ---

            // PRIORIDAD 1: Navegación especial por bloque con Ctrl+Alt
            if (e.isControlDown() && e.isAltDown()) {
                if (e.getWheelRotation() < 0) this.navegarBloqueAnterior();
                else this.navegarBloqueSiguiente();
                e.consume();
                return;
            }

            // PRIORIDAD 2: Si estamos sobre la IMAGEN
            if (sobreLaImagen) {
                // Y el MODO ZOOM MANUAL está ACTIVO...
                if (model.isZoomHabilitado()) {
                    // ...la rueda hace zoom o paneo rápido.
                    if (e.isShiftDown()) {
                        this.aplicarPan(-e.getWheelRotation() * 30, 0);
                    } else if (e.isControlDown()) {
                        this.aplicarPan(0, e.getWheelRotation() * 30);
                    } else {
                        this.aplicarZoomConRueda(e);
                    }
                } else {
                    // Si el zoom manual está DESACTIVADO, la rueda navega por la lista.
                    this.navegarSiguienteOAnterior(e.getWheelRotation());
                }
                e.consume();
                return;
            }
            
            // PRIORIDAD 3: Si estamos sobre la tabla de exportación en modo Proyecto
            if (sobreTablaExportacion && model.getCurrentWorkMode() == WorkMode.PROYECTO) {
                projectController.navegarTablaExportacionConRueda(e);
                e.consume();
                return;
            }

            // PRIORIDAD 4 (Por defecto): Navegación normal para otras listas (Nombres, Miniaturas)
            this.navegarSiguienteOAnterior(e.getWheelRotation());
            e.consume();
        };

        // --- Añadir el Master Wheel Listener a todos los componentes etiquetados ---
        List<Component> componentesConRueda = registry.getComponentsByTag("WHEEL_NAVIGABLE");
        logger.debug("[GeneralController] Encontrados " + componentesConRueda.size() + " componentes etiquetados como 'WHEEL_NAVIGABLE'.");
        for (Component c : componentesConRueda) {
            // Limpiamos listeners antiguos para evitar duplicados
            for (java.awt.event.MouseWheelListener mwl : c.getMouseWheelListeners()) {
                c.removeMouseWheelListener(mwl);
            }
            c.addMouseWheelListener(masterWheelListener);
        }

        // --- Listeners de clic y arrastre para paneo ---
        MouseAdapter paneoMouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent ev) {
                GeneralController.this.iniciarPaneo(ev);
            }
        };
        MouseMotionAdapter paneoMouseMotionAdapter = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent ev) {
                GeneralController.this.continuarPaneo(ev);
            }
        };
        
        // Obtenemos los componentes y aplicamos los listeners de paneo
        Component etiquetaVisor = registry.get("label.imagenPrincipal");
        Component etiquetaProyecto = registry.get("label.proyecto.imagen");
        Component etiquetaCarrusel = registry.get("label.carousel.imagen");

        if (etiquetaVisor != null) {
            etiquetaVisor.addMouseListener(paneoMouseAdapter);
            etiquetaVisor.addMouseMotionListener(paneoMouseMotionAdapter);
        }
        if (etiquetaProyecto != null) {
            etiquetaProyecto.addMouseListener(paneoMouseAdapter);
            etiquetaProyecto.addMouseMotionListener(paneoMouseMotionAdapter);
        }
        if (etiquetaCarrusel != null) {
            etiquetaCarrusel.addMouseListener(paneoMouseAdapter);
            etiquetaCarrusel.addMouseMotionListener(paneoMouseMotionAdapter);
        }

        logger.debug("[GeneralController] Listeners de entrada globales configurados.");
        
    } // --- Fin del método configurarListenersDeEntradaGlobal ---
    
    
    /**
     * Implementación del método de la interfaz KeyEventDispatcher.
     * Intercepta eventos de teclado globales, movida desde VisorController.
     * @param e El KeyEvent a procesar.
     * @return true si el evento fue consumido, false para continuar.
     */
    @Override // ESTO ES UNA IMPLEMENTACIÓN DE LA INTERFAZ KeyEventDispatcher
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getID() != KeyEvent.KEY_PRESSED) {
            return false;
        }

        // --- MANEJO ESPECIAL Y SEGURO DE LA TECLA ALT (movido de VisorController) ---
        if (e.getKeyCode() == KeyEvent.VK_ALT) {
            // Necesita acceso a la vista, se obtiene a través de visorController.getView()
            if (visorController != null && visorController.getView() != null && visorController.getView().getJMenuBar() != null) {
                JMenuBar menuBar = visorController.getView().getJMenuBar();
                if (menuBar.isSelected()) { // Comprueba si algún menú ya está abierto (seleccionado)
                    menuBar.getSelectionModel().clearSelection(); // Cierra la selección actual
                    logger.debug("--- [GeneralController Dispatcher] ALT: Menú ya activo. Cerrando selección.");
                } else {
                    if (menuBar.getMenuCount() > 0) { // Si no hay ningún menú activo, activamos el primero.
                        JMenu primerMenu = menuBar.getMenu(0);
                        if (primerMenu != null) {
                        	
                        	//LOG [GeneralController Dispatcher] ALT: Simulando clic en el menú 'Archivo'
                            //logger.debug("--- [GeneralController Dispatcher] ALT: Simulando clic en el menú 'Archivo'...");
                        	
                            primerMenu.doClick(); // Simula un clic del ratón, abriendo el menú.
                        }
                    }
                }
                e.consume(); // Consumimos el evento ALT
                return true;
            }
        }
        
        // Continuación de la lógica de navegación por teclado (movido de VisorController)
        if (model == null || registry == null) { // Dependencias mínimas
            return false;
        }

        java.awt.Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner == null) {
            return false;
        }

        JScrollPane scrollMiniaturas = registry.get("scroll.miniaturas");
        JList<String> listaNombres = registry.get("list.nombresArchivo");

        boolean focoEnAreaMiniaturas = (scrollMiniaturas != null && SwingUtilities.isDescendingFrom(focusOwner, scrollMiniaturas));
        boolean focoEnListaNombres = (listaNombres != null && SwingUtilities.isDescendingFrom(focusOwner, listaNombres));

        if (focoEnAreaMiniaturas || focoEnListaNombres) {
            boolean consumed = false;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP: case KeyEvent.VK_LEFT:
                    this.navegarAnterior(); // Delega a los métodos IModoController de GeneralController
                    consumed = true;
                    break;
                case KeyEvent.VK_DOWN: case KeyEvent.VK_RIGHT:
                    this.navegarSiguiente(); // Delega a los métodos IModoController de GeneralController
                    consumed = true;
                    break;
                case KeyEvent.VK_HOME:
                    this.navegarPrimero(); // Delega a los métodos IModoController de GeneralController
                    consumed = true;
                    break;
                case KeyEvent.VK_END:
                    this.navegarUltimo(); // Delega a los métodos IModoController de GeneralController
                    consumed = true;
                    break;
                case KeyEvent.VK_PAGE_UP:
                    this.navegarBloqueAnterior(); // Delega a los métodos IModoController de GeneralController
                    consumed = true;
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    this.navegarBloqueSiguiente(); // Delega a los métodos IModoController de GeneralController
                    consumed = true;
                    break;
            }
            if (consumed) {
                e.consume();
                return true;
            }
        }
        
        return false;
    }// --- Fin del método dispatchKeyEvent ---

// *************************************************************************************************************************
// *************************************************************************   IMPLEMENTACIÓN DE LA INTERFAZ IModoController
// *************************************************************************************************************************
    
    // --- Implementación de IModoController (delegando al controlador de modo activo) ---
    // NOTA: La lógica interna de estos métodos seguirá residiendo en VisorController y ProjectController
    // tal como están ahora. GeneralController solo actúa como un router.

    @Override
    public void navegarSiguiente() {
        logger.debug("[GeneralController] Delegando navegarSiguiente para modo: " + model.getCurrentWorkMode());
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.navegarSiguiente();
        } else {
            // Sirve tanto para VISUALIZADOR como para CARROUSEL
            visorController.navegarSiguiente();
        }
    } // --- FIN del metodo navegarSiguiente ---

    
    @Override
    public void navegarAnterior() {
        logger.debug("[GeneralController] Delegando navegarAnterior para modo: " + model.getCurrentWorkMode());
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.navegarAnterior();
        } else {
            visorController.navegarAnterior();
        }
    } // --- FIN del metodo navegarAnterior ---

    
    @Override
    public void navegarPrimero() {
        logger.debug("[GeneralController] Delegando navegarPrimero para modo: " + model.getCurrentWorkMode());
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.navegarPrimero();
        } else {
            visorController.navegarPrimero();
        }
    } // --- FIN del metodo navegarPrimero ---
    

    @Override
    public void navegarUltimo() {
        logger.debug("[GeneralController] Delegando navegarUltimo para modo: " + model.getCurrentWorkMode());
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.navegarUltimo();
        } else {
            visorController.navegarUltimo();
        }
    } // --- FIN del metodo navegarUltimo ---
    

    @Override
    public void navegarBloqueAnterior() {
        logger.debug("[GeneralController] Delegando navegarBloqueAnterior para modo: " + model.getCurrentWorkMode());
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.navegarBloqueAnterior();
        } else {
            visorController.navegarBloqueAnterior();
        }
    } // --- FIN del metodo navegarBloqueAnterior ---
    

    @Override
    public void navegarBloqueSiguiente() {
        logger.debug("[GeneralController] Delegando navegarBloqueSiguiente para modo: " + model.getCurrentWorkMode());
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.navegarBloqueSiguiente();
        } else {
            visorController.navegarBloqueSiguiente();
        }
    } // --- FIN del metodo navegarBloqueSiguiente ---
    

    /**
     * Helper para la navegación con rueda del ratón en listas (si no hay modificadores).
     * Mueve el selector de la lista en lugar del scroll.
     * Lógica movida de VisorController.
     */
    private void navegarSiguienteOAnterior(int wheelRotation) { // MÉTODO AÑADIDO (HELPER PRIVADO)
        if (wheelRotation < 0) { // Rueda hacia arriba
            navegarAnterior();
        } else { // Rueda hacia abajo
            navegarSiguiente();
        }
    
    }// --- FIN del metodo navegarSiguienteOAnterior ---
    

    @Override // ESTO ES UNA IMPLEMENTACIÓN DE LA INTERFAZ IModoController
    public void aplicarZoomConRueda(MouseWheelEvent e) {
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR) {
            visorController.aplicarZoomConRueda(e);
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.aplicarZoomConRueda(e);
        }else if (model.getCurrentWorkMode() == VisorModel.WorkMode.CARROUSEL) {
        	visorController.aplicarZoomConRueda(e);
        }
        
        //LOG [GeneralController] Delegando aplicarZoomConRueda
        logger.debug("[GeneralController] Delegando aplicarZoomConRueda a " + model.getCurrentWorkMode());
    
    }// --- FIN del metodo aplicarZoomConRueda ---
    

    @Override // ESTO ES UNA IMPLEMENTACIÓN DE LA INTERFAZ IModoController
    public void aplicarPan(int deltaX, int deltaY) {
        // La lógica de cálculo del pan (cuánto se mueve) reside en ZoomManager.
        // Aquí solo delegamos la acción de pan al controlador de modo activo.
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR) {
            visorController.aplicarPan(deltaX, deltaY);
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.aplicarPan(deltaX, deltaY);
        }else if (model.getCurrentWorkMode() == VisorModel.WorkMode.CARROUSEL) {
        	visorController.aplicarPan(deltaX, deltaY);
        }
        
        //LOG [GeneralController] Delegando aplicarPan
        logger.debug("[GeneralController] Delegando aplicarPan a " + model.getCurrentWorkMode());
    
    }// --- FIN del metodo aplicarPan ---
    

    @Override // ESTO ES UNA IMPLEMENTACIÓN DE LA INTERFAZ IModoController
    public void iniciarPaneo(MouseEvent e) {
        // En GeneralController, almacenamos lastMouseX/Y para el cálculo de delta en continuarPaneo.
        // Luego delegamos al controlador de modo, quien llamará al ZoomManager para iniciar el paneo.
        this.lastMouseX = e.getX(); // MODIFICACIÓN CLAVE: GeneralController mantiene el estado del ratón para el paneo
        this.lastMouseY = e.getY(); // MODIFICACIÓN CLAVE

        if (model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR) {
            visorController.iniciarPaneo(e);
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.iniciarPaneo(e);
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.CARROUSEL) {
        	visorController.iniciarPaneo(e);
        }
        
        //LOG [GeneralController] Delegando iniciarPaneo
        logger.debug("[GeneralController] Delegando iniciarPaneo a " + model.getCurrentWorkMode());
    
    }// --- FIN del metodo iniciarPaneo ---

    
    @Override // ESTO ES UNA IMPLEMENTACIÓN DE LA INTERFAZ IModoController
    public void continuarPaneo(MouseEvent e) {
        // Calculamos el delta de movimiento y actualizamos lastMouseX/Y aquí.
        int deltaX = e.getX() - this.lastMouseX; // MODIFICACIÓN CLAVE
        int deltaY = e.getY() - this.lastMouseY; // MODIFICACIÓN CLAVE
        this.lastMouseX = e.getX();
        this.lastMouseY = e.getY();

        // Luego delegamos la acción de aplicar el pan con los deltas calculados.
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR) {
            visorController.aplicarPan(deltaX, deltaY);
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.aplicarPan(deltaX, deltaY);
        }else if (model.getCurrentWorkMode() == VisorModel.WorkMode.CARROUSEL) {
        	visorController.aplicarPan(deltaX, deltaY);
        }
        
        
        //LOG [GeneralController] Delegando continuarPaneo
        logger.debug("[GeneralController] Delegando continuarPaneo a " + model.getCurrentWorkMode());
    
    }// --- FIN del metodo continuarPaneo ---
    
    
    private boolean isComponentTagged(Component component, String tag) {
        // Itera hacia arriba en la jerarquía de componentes
        for (Component c = component; c != null; c = c.getParent()) {
            // Comprueba si el ComponentRegistry tiene alguna etiqueta para este componente
            // (Necesitaríamos un método en ComponentRegistry para buscar por componente,
            // pero por ahora, vamos a simplificar asumiendo que el componente principal está etiquetado)
            
            // La forma más simple es comprobar si el componente es uno de los que etiquetamos.
            if (c == registry.get("label.imagenPrincipal") ||
                c == registry.get("label.proyecto.imagen") ||
                c == registry.get("label.carousel.imagen")) {
                return true;
            }
        }
        return false;
    } // --- Fin del método isComponentTagged ---
    
    
    /**
     * Notifica a todas las acciones sensibles al contexto para que actualicen su estado 'enabled'.
     * Este es el método central para llamar después de un cambio de estado global, como activar/desactivar la sincronización.
     */
    public void notificarAccionesSensiblesAlContexto() { //weno
        logger.debug("[GeneralController] Notificando a todas las acciones sensibles al contexto...");
        if (actionMap == null || model == null) return;

        // Itera por todas las acciones del mapa
        for (Action action : actionMap.values()) {
            // Comprueba si la acción implementa nuestra interfaz
            if (action instanceof ContextSensitiveAction) {
                // Si es así, la "castea" de forma segura y llama a su método de actualización
                ((ContextSensitiveAction) action).updateEnabledState(model);
            }
        }
        
        // Adicionalmente, forzamos la sincronización del botón de sync para asegurar su estado visual.
        Action syncAction = actionMap.get(AppActionCommands.CMD_TOGGLE_SYNC_VISOR_CARRUSEL);
        if (syncAction != null && configAppManager != null) {
            // Le pedimos al ConfigAppManager que aplique el estilo visual correcto al botón de Sync
            configAppManager.actualizarAspectoBotonToggle(syncAction, model.isSyncVisualizadorCarrusel());
        }
        
        logger.debug("[GeneralController] Notificación completada.");
    } // --- Fin del método notificarAccionesSensiblesAlContexto ---
    
    
    /**
     * Orquesta el cambio de modo de carga de subcarpetas para el modo Visualizador.
     * Este método se encarga de la lógica de alto nivel, incluyendo la sincronización final.
     * 
     * @param nuevoEstadoIncluirSubcarpetas El estado deseado: true para cargar subcarpetas, false para no hacerlo.
     */
    public void solicitarCambioModoCargaSubcarpetas(boolean nuevoEstadoIncluirSubcarpetas) {
        logger.debug("[GeneralController] Solicitud para cambiar modo de carga de subcarpetas a: " + nuevoEstadoIncluirSubcarpetas);

        // --- INICIO DE LA MODIFICACIÓN (LA GUARDA DE SEGURIDAD) ---
        // Comprobamos si el modelo YA está en el estado que se nos pide.
        // Si es así, no hay nada que hacer más que asegurar que la UI esté sincronizada.
        boolean estadoActualIncluyeSubcarpetas = !model.isMostrarSoloCarpetaActual();
        if (estadoActualIncluyeSubcarpetas == nuevoEstadoIncluirSubcarpetas) {
            logger.debug("  -> El modelo ya está en el estado deseado. Sincronizando UI por si acaso y deteniendo proceso.");
            sincronizarControlesDeSubcarpetas(); // Aseguramos que los botones reflejen el estado correcto.
            return; // Detenemos la ejecución para romper el bucle.
        }
        // --- FIN DE LA MODIFICACIÓN ---

        // 1. Validar que estemos en un modo compatible para esta operación.
        if (model.getCurrentWorkMode() != VisorModel.WorkMode.VISUALIZADOR && model.getCurrentWorkMode() != VisorModel.WorkMode.CARROUSEL) {
            logger.warn("  -> Operación cancelada: El modo actual (" + model.getCurrentWorkMode() + ") no soporta esta acción.");
            sincronizarControlesDeSubcarpetas(); // Revertimos visualmente por si acaso.
            return;
        }

        // 2. Validar dependencias.
        if (visorController == null || model == null || configuration == null || displayModeManager == null) {
            logger.error("  ERROR [GeneralController]: Dependencias críticas (visorController, model, config, displayModeManager) nulas. Abortando.");
            return;
        }

        // 3. Guardar la clave de la imagen actual ANTES de cualquier cambio.
        final String claveAntesDelCambio = model.getSelectedImageKey();
        logger.debug("  -> Clave de imagen a intentar mantener: " + claveAntesDelCambio);

        // 4. Actualizar el estado en el Modelo y la Configuración.
        model.setMostrarSoloCarpetaActual(!nuevoEstadoIncluirSubcarpetas);
        configuration.setString(ConfigKeys.COMPORTAMIENTO_CARGAR_SUBCARPETAS, String.valueOf(nuevoEstadoIncluirSubcarpetas));

        // 5. Definir la acción de sincronización que se ejecutará DESPUÉS de la carga.
        Runnable accionPostCarga = () -> {
            logger.debug("  [Callback Post-Carga] Tarea de carga finalizada. Ejecutando sincronización maestra...");
            
            // a) Sincronizar toda la UI (botones, menús, estados, etc.).
            this.sincronizarTodaLaUIConElModelo();
            
            // b) Repoblar el Grid con la nueva lista.
            displayModeManager.poblarYSincronizarGrid();
            
            logger.debug("  [Callback Post-Carga] Sincronización finalizada.");
        };

        // 6. Delegar la tarea de carga de bajo nivel al VisorController.
        logger.debug("  -> Delegando a VisorController la tarea de recargar la lista de imágenes...");
        visorController.cargarListaImagenes(claveAntesDelCambio, accionPostCarga);
        
    } // --- FIN del metodo solicitarCambioModoCargaSubcarpetas ---
    
    
    /**
     * MÉTODO DE SINCRONIZACIÓN CENTRALIZADO.
     * Lee el estado actual del modelo y actualiza el estado 'selected' y la apariencia
     * de TODOS los controles relacionados con la carga de subcarpetas (el botón toggle y los dos radio-botones del menú).
     * Esta es la ÚNICA fuente de verdad para la sincronización de estos componentes.
     */
    private void sincronizarControlesDeSubcarpetas() {
        if (model == null || actionMap == null || configAppManager == null) {
            logger.warn("WARN [sincronizarControlesDeSubcarpetas]: Dependencias nulas. No se puede sincronizar.");
            return;
        }

        // 1. Leer el estado "de verdad" desde el modelo UNA SOLA VEZ.
        boolean estadoActualIncluyeSubcarpetas = !model.isMostrarSoloCarpetaActual();

        // 2. Obtener las tres Actions relacionadas.
        Action toggleAction = actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
        Action radioIncluirAction = actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS);
        Action radioSoloAction = actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA);

        // 3. Sincronizar el botón Toggle principal.
        if (toggleAction != null) {
            toggleAction.putValue(Action.SELECTED_KEY, estadoActualIncluyeSubcarpetas);
            configAppManager.actualizarAspectoBotonToggle(toggleAction, estadoActualIncluyeSubcarpetas);
        }
        
        // 4. Sincronizar el radio-botón "Incluir Subcarpetas".
        if (radioIncluirAction != null) {
            radioIncluirAction.putValue(Action.SELECTED_KEY, estadoActualIncluyeSubcarpetas);
        }

        // 5. Sincronizar el radio-botón "Solo Carpeta Actual". Su estado es el inverso.
        if (radioSoloAction != null) {
            radioSoloAction.putValue(Action.SELECTED_KEY, !estadoActualIncluyeSubcarpetas);
        }
        
        logger.debug("  -> Sincronizados controles de subcarpetas. Estado actual (incluir): " + estadoActualIncluyeSubcarpetas);
    } // --- Fin del método sincronizarControlesDeSubcarpetas ---
    
    
//  ********************************************************************************** FIN IMPLEMENTACION INTERFAZ IModoController
    
//  *************************************************************************************************************** INICIO GETTERS    
    
    public ToolbarManager getToolbarManager() {return this.toolbarManager;}
    public VisorModel getModel() { return this.model;}
    public void setDisplayModeManager(DisplayModeManager displayModeManager) {this.displayModeManager = Objects.requireNonNull(displayModeManager, "DisplayModeManager no puede ser nulo");}
    public void setConfiguration(ConfigurationManager configuration) {this.configuration = Objects.requireNonNull(configuration, "ConfigurationManager no puede ser nulo");}
    

//  ****************************************************************************************************************** FIN GETTERS
    
    
    /**
     * MÉTODO ORQUESTADOR CENTRAL PARA ALTERNAR EL MODO DE CARGA DE SUBCARPETAS.
     * Invierte el estado actual del modelo y luego inicia el proceso de recarga.
     * Utiliza un flag de bloqueo para evitar ejecuciones concurrentes.
     */
    public void solicitarToggleModoCargaSubcarpetas() {
        // Si ya hay una operación en curso, la ignoramos.
        if (isChangingSubfolderMode) {
            logger.warn("  [GeneralController] ADVERTENCIA: Se ha ignorado una solicitud de toggle de subcarpetas porque ya hay una en progreso.");
            return;
        }

        try {
            isChangingSubfolderMode = true; // --- BLOQUEAMOS ---
            logger.debug("[GeneralController] Solicitud para ALTERNAR modo de carga de subcarpetas.");
            
            // 1. Invertir el estado actual del modelo. Esta es la lógica central.
            boolean nuevoEstadoSoloCarpeta = !model.isMostrarSoloCarpetaActual();
            model.setMostrarSoloCarpetaActual(nuevoEstadoSoloCarpeta);
            
            // 2. Actualizar la configuración para que se guarde.
            configuration.setString(ConfigKeys.COMPORTAMIENTO_CARGAR_SUBCARPETAS, String.valueOf(!nuevoEstadoSoloCarpeta));
            logger.debug("  -> Estado del modelo cambiado a: isMostrarSoloCarpetaActual=" + nuevoEstadoSoloCarpeta);

            // 3. El resto de la lógica es la que ya teníamos...
            final String claveAntesDelCambio = model.getSelectedImageKey();
            logger.debug("  -> Clave de imagen a intentar mantener: " + claveAntesDelCambio);

            Runnable accionPostCarga = () -> {
                try {
                    logger.debug("  [Callback Post-Carga] Tarea de carga finalizada. Ejecutando sincronización maestra...");
                    this.sincronizarTodaLaUIConElModelo();
                    displayModeManager.poblarYSincronizarGrid();
                    logger.debug("  [Callback Post-Carga] Sincronización finalizada.");
                } finally {
                    isChangingSubfolderMode = false; // --- DESBLOQUEAMOS ---
                }
            };

            visorController.cargarListaImagenes(claveAntesDelCambio, accionPostCarga);

        } catch (Exception e) {
            logger.error("ERROR INESPERADO en solicitarToggleModoCargaSubcarpetas: " + e.getMessage());
            e.printStackTrace();
            isChangingSubfolderMode = false; // --- DESBLOQUEAMOS EN CASO DE ERROR ---
        }
    } // --- FIN del metodo solicitarToggleModoCargaSubcarpetas ---
    
    
} // --- Fin de la clase GeneralController ---


