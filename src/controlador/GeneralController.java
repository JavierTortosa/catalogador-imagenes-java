package controlador;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.actions.filtro.SetFilterTypeAction;
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import controlador.interfaces.IModoController;
import controlador.managers.ConfigApplicationManager;
import controlador.managers.DisplayModeManager;
import controlador.managers.FilterManager;
import controlador.managers.FolderNavigationManager;
import controlador.managers.ImageListManager;
import controlador.managers.InfobarStatusManager;
import controlador.managers.MenuPopupManager;
import controlador.managers.ToolbarManager;
import controlador.managers.ViewManager;
import controlador.managers.filter.FilterCriterion;
import controlador.managers.filter.FilterCriterion.FilterSource;
import controlador.managers.filter.FilterCriterion.FilterType;
import controlador.managers.interfaces.IProjectManager;
import controlador.managers.tree.FolderTreeManager;
import controlador.services.AppModeService;
import controlador.services.FilterService;
import controlador.services.NavigationService;
import controlador.services.ProjectLifecycleService;
import controlador.services.SearchSortService;
import controlador.services.ZoomPanService;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.components.Direction;

/**
 * Controlador de aplicación de alto nivel.
 * Orquesta la interacción entre los controladores de modo (VisorController,
 * ProjectController)
 * y gestiona el estado global de la aplicación, como el modo de trabajo actual
 * y la
 * habilitación/deshabilitación de la UI correspondiente.
 */
public class GeneralController
        implements IModoController, modelo.MasterListChangeListener, servicios.ProjectStateListener {

    private static final Logger logger = LoggerFactory.getLogger(GeneralController.class);

    // --- Dependencias Clave ---
    private VisorModel model;
    private VisorController visorController;
    private ProjectController projectController;
    private ViewManager viewManager;
    private InfobarStatusManager statusBarManager;
    private ConfigApplicationManager configAppManager;
    private ToolbarManager toolbarManager;
    private ComponentRegistry registry;
    private DisplayModeManager displayModeManager;
    private ConfigurationManager configuration;
    private FolderNavigationManager folderNavManager;
    private FolderTreeManager folderTreeManager;
    private FilterManager filterManager;
    private ImageListManager imageListManager;
    private DataController dataController;
    private AppModeService appModeService;
    private MenuPopupManager menuPopupManager;
    private ProjectLifecycleService projectLifecycleService;
    private SearchSortService searchSortService;
    private FilterService filterService; 
    private NavigationService navigationService;
    private ZoomPanService zoomPanService;
    
    private Map<controlador.managers.filter.FilterCriterion.SourceType, javax.swing.Icon> typeIconsMap;
    private Map<String, Action> actionMap;

    private volatile boolean isChangingSubfolderMode = false;

    private javax.swing.Timer filterDebounceTimer;

    /**
     * Constructor de GeneralController.
     * Las dependencias se inyectarán a través de setters después de la creación.
     */
    public GeneralController() {
        // Constructor vacío. La inicialización se delega al método initialize.
    } // --- Fin del método GeneralController (constructor) ---

    public void initialize() {

        logger.debug("[GeneralController] Inicializado.");

        sincronizarEstadoBotonesDeModo();

        SwingUtilities.invokeLater(() -> {
            javax.swing.JTextField searchField = registry.get("textfield.filtro.orden");
            JList<String> fileList = registry.get("list.nombresArchivo");

            if (searchField == null || fileList == null) {
                logger.error(
                        "[GeneralController] ¡ERROR CRÍTICO! Faltan JTextField o JList para inicializar la búsqueda/filtro.");
                return;
            }

            searchField.addActionListener(e -> {
                if (!model.isLiveFilterActive()) {
                    buscarSiguienteCoincidencia();
                }
            });

            // --- INICIO: LÓGICA DE DEBOUNCING PARA FILTRO EN VIVO ---
            // 1. Creamos el Timer. Se disparará 300ms después de la última pulsación de
            // tecla.
            filterDebounceTimer = new javax.swing.Timer(300, (e) -> {
                // Esto se ejecuta cuando el usuario ha dejado de teclear.
                // Llamamos al método de filtrado real.
                filterManager.actualizarFiltro();
            });
            filterDebounceTimer.setRepeats(false); // Importante: solo se ejecuta una vez por ráfaga de eventos.

            // 2. Modificamos el DocumentListener para que REINICIE el Timer en lugar de
            // filtrar.

            searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {

                private void handleTextChange() {
                    if (model.isLiveFilterActive()) {
                        filterDebounceTimer.restart(); // Cada pulsación de tecla reinicia el temporizador.
                    }

                    sincronizarEstadoControlesTornado();
                }

                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    handleTextChange();
                }

                public void removeUpdate(javax.swing.event.DocumentEvent e) {
                    handleTextChange();
                }

                public void changedUpdate(javax.swing.event.DocumentEvent e) {
                    handleTextChange();
                }
            });

            // --- FIN: LÓGICA DE DEBOUNCING ---

            searchSortService.configurePlaceholderText(searchField);

            // Tooltip informativo del campo de búsqueda
            searchField.setToolTipText("<html><b>Busqueda rapida (Tornado):</b><br>"
                    + "• Con filtro tornado <b>APAGADO</b>: pulsa Enter para buscar la cadena desde la seleccion actual<br>"
                    + "• Con filtro tornado <b>ENCENDIDO</b>: filtra en vivo los nombres que contienen el texto<br>"
                    + "• El boton <b>'+'</b> (a la derecha) convierte el texto en filtros permanentes<br>"
                    + "• Usa <b>','</b> para filtrar por varios textos a la vez (ej: \"desktop, phone\")</html>");

            // --- HINT en statusbar al enfocar el campo de búsqueda ---
            searchField.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    if (statusBarManager != null) {
                        statusBarManager.mostrarAyuda("Enter: busca texto desde seleccion | Tornado ON: filtra en vivo | + : anade filtros permanentes");
                    }
                }
                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    if (statusBarManager != null) {
                        statusBarManager.mostrarAyuda(null);
                    }
                }
            });

            sincronizarEstadoControlesTornado();

            fileList.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting())
                    return;

                @SuppressWarnings("unchecked")
                JList<String> sourceList = (JList<String>) e.getSource();
                int selectedIndexInView = sourceList.getSelectedIndex();
                if (selectedIndexInView == -1)
                    return;

                if (model.isLiveFilterActive()) {
                    String selectedValue = sourceList.getSelectedValue();
                    DefaultListModel<String> masterModel = model.getCurrentListContext().getModeloLista();
                    int realIndexInMaster = masterModel.indexOf(selectedValue);

                    if (realIndexInMaster != -1
                            && visorController.getListCoordinator().getOfficialSelectedIndex() != realIndexInMaster) {
                        visorController.getListCoordinator().seleccionarImagenPorIndice(realIndexInMaster);
                    }
                } else {
                    if (visorController.getListCoordinator().getOfficialSelectedIndex() != selectedIndexInView) {
                        visorController.getListCoordinator().seleccionarImagenPorIndice(selectedIndexInView);
                    }
                }
            });

            logger.debug("[GeneralController] Listeners de búsqueda/filtro configurados correctamente.");

            if (projectController != null && projectController.getProjectManager() != null) {
                projectController.getProjectManager().addProjectStateListener(this);
                logger.debug("[GeneralController] Registrado como oyente de estado del proyecto.");
            }
        });
    } // --- Fin del método initialize ---

    // --- Setters para Inyección de Dependencias ---

    public void setModel(VisorModel model) {
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en GeneralController");
    } // --- Fin del método setModel ---

    public void setVisorController(VisorController visorController) {
        this.visorController = Objects.requireNonNull(visorController,
                "VisorController no puede ser null en GeneralController");
    } // --- Fin del método setVisorController ---

    public void setProjectController(ProjectController projectController) {
        this.projectController = Objects.requireNonNull(projectController,
                "ProjectController no puede ser null en GeneralController");
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

    public DataController getDataController() {
        return this.dataController;
    } // --- Fin del método getDataController ---

    public ProjectController getProjectController() {
        return this.projectController;
    } // --- Fin del método getProjectController ---
    
    

    public void setStatusBarManager(InfobarStatusManager statusBarManager) {
        this.statusBarManager = Objects.requireNonNull(statusBarManager,
                "InfobarStatusManager no puede ser null en GeneralController");
    } // --- Fin del método setStatusBarManager ---

    public void setConfigApplicationManager(ConfigApplicationManager configAppManager) {
        this.configAppManager = Objects.requireNonNull(configAppManager,
                "ConfigApplicationManager no puede ser null en GeneralController");
    } // --- Fin del método setConfigApplicationManager ---

    public void setToolbarManager(ToolbarManager toolbarManager) {
        this.toolbarManager = Objects.requireNonNull(toolbarManager,
                "ToolbarManager no puede ser null en GeneralController");
    } // --- Fin del método setToolbarManager ---

    public void setRegistry(ComponentRegistry registry) { // <-- NUEVO SETTER
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null en GeneralController");
    } // --- Fin del método setRegistry ---

    public FilterManager getFilterManager() {
        return this.filterManager;
    } // --- Fin del método getFilterManager ---

    // ******************************************************************************************
    // Fin Setters
    
    /**
     * Delega la solicitud de actualizar el título de la ventana principal al
     * AppModeService.
     * Este método se llama después de operaciones que pueden cambiar el contexto,
     * como cargar un nuevo proyecto.
     */
    public void actualizarTituloVentana() {
        if (appModeService != null) {
            appModeService.actualizarTituloPrincipal();
        } else {
            logger.error("[GeneralController] AppModeService es nulo. No se pudo actualizar el título de la ventana.");
        }
    } // ---FIN de metodo actualizarTituloVentana---

    public void handleNewProject() {
        projectLifecycleService.handleNewProject();
    }

    public void handleOpenProject() {
        projectLifecycleService.handleOpenProject();
    }

    public void handleSaveProject() {
        projectLifecycleService.handleSaveProject();
    }

    public void handleSaveProjectAs() {
        projectLifecycleService.handleSaveProjectAs();
    }

    public void handleDeleteProject() {
        projectLifecycleService.handleDeleteProject();
    }

    public void handleApplicationShutdown() {
        projectLifecycleService.handleApplicationShutdown();
    }

    /**
     * Orquesta el guardado explícito del archivo de configuración.
     * Esto NO guarda el proyecto, solo las preferencias de la aplicación.
     */
    public void handleSaveConfiguration() {
        if (configuration == null) {
            logger.error("ERROR [handleSaveConfiguration]: ConfigurationManager es nulo.");
            return;
        }

        logger.debug("  -> Guardando estado de la ventana y configuración a petición del usuario...");

        // Guardamos el estado de la ventana por si ha cambiado
        if (visorController != null) {
            visorController.guardarEstadoVentanaEnConfig();
        }

        try {
            configuration.guardarConfiguracion(configuration.getConfig());
            logger.info("Configuración guardada exitosamente en el archivo.");
            if (statusBarManager != null) {
                statusBarManager.mostrarMensajeTemporal("Configuración guardada.", 2000);
            }
        } catch (java.io.IOException e) {
            logger.error("### ERROR AL GUARDAR CONFIGURACIÓN MANUALMENTE: " + e.getMessage());
            if (statusBarManager != null) {
                statusBarManager.mostrarMensajeTemporal("Error al guardar configuración.", 3000);
            }
        }
    } // ---FIN de metodo handleSaveConfiguration---

    /**
     * Orquesta la transición entre los diferentes modos de trabajo de la
     * aplicación.
     * Es el punto de entrada central para cambiar de vista. Contiene la lógica
     * de sincronización y confirmación para el "Carrusel Megapower".
     * 
     * @param modoDestino El modo al que se desea cambiar (VISUALIZADOR o PROYECTO).
     */
    public void cambiarModoDeTrabajo(VisorModel.WorkMode modoDestino) {
        WorkMode modoActual = this.model.getCurrentWorkMode();
        if (modoActual == modoDestino) {
            logger.trace("[GeneralController] Intento de cambiar al modo que ya está activo: {}. No se hace nada.",
                    modoDestino);
            return;
        }

        // --- PRE-VALIDACIÓN ESPECIAL PARA MODO PROYECTO ---
        if (modoDestino == WorkMode.PROYECTO) {
            boolean hayImagenesEnProyecto = !visorController.getProjectManager().getImagenesMarcadas().isEmpty();

            if (!hayImagenesEnProyecto) {
                // El proyecto está vacío. Llamamos al helper que maneja la selección de
                // archivo.
                // Si el helper devuelve 'false', significa que el usuario canceló,
                // por lo tanto, debemos abortar la transición.
                if (!appModeService.manejarAperturaDeProyectoVacio()) {
                    sincronizarEstadoBotonesDeModo(); // Revertir el estado visual del botón
                    return; // Abortar la transición
                }
                // Si el helper devuelve 'true', significa que un proyecto fue cargado
                // exitosamente.
                // El flujo de este método continuará para completar la transición al modo
                // proyecto.
            }
        }
        // --- FIN DE LA PRE-VALIDACIÓN ---

        logger.debug("--- [GeneralController] INICIANDO TRANSICIÓN DE MODO: {} -> {} ---", modoActual, modoDestino);

        // --- LÓGICA DE SEGURIDAD Y CONFIRMACIÓN PARA SINCRONIZACIÓN (Se mantiene
        // igual) ---
        boolean esTransicionSincronizable = (modoActual == WorkMode.VISUALIZADOR && modoDestino == WorkMode.CARROUSEL)
                ||
                (modoActual == WorkMode.CARROUSEL && modoDestino == WorkMode.VISUALIZADOR);

        if (esTransicionSincronizable && model.isSyncVisualizadorCarrusel()) {
            String titulo = "Confirmar Transición Sincronizada";
            String mensaje = modoDestino == WorkMode.CARROUSEL
                    ? "<html>El modo <b>Sincronización</b> está activo.<br>Se cargará el estado del Visualizador en el Carrusel.<br><br>¿Continuar?</html>"
                    : "<html>El modo <b>Sincronización</b> está activo.<br>La posición actual del Carrusel se transferirá al Visualizador.<br><br>¿Continuar?</html>";

            int respuesta = javax.swing.JOptionPane.showConfirmDialog(null, mensaje, titulo,
                    javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.INFORMATION_MESSAGE);
            if (respuesta != javax.swing.JOptionPane.YES_OPTION) {
                logger.debug("--- [GeneralController] TRANSICIÓN CANCELADA por el usuario. ---");
                sincronizarEstadoBotonesDeModo();
                return;
            }
        }
        // --- FIN LÓGICA DE SEGURIDAD ---

        salirModo(modoActual);
        this.model.setCurrentWorkMode(modoDestino);
        entrarModo(modoDestino);
        actualizarTituloVentana();

        logger.debug("--- [GeneralController] TRANSICIÓN DE MODO COMPLETADA a {} ---\n", modoDestino);
    } // --- Fin del método cambiarModoDeTrabajo ---

    /**
     * Comprueba si existe una sesión de recuperación pendiente y gestiona la decisión del usuario.
     * Es invocado cuando el usuario intenta realizar una acción que requiere el uso de proyectos.
     * 
     * @return {@code true} si se puede proceder con la acción original, 
     *         {@code false} si el usuario canceló la operación de recuperación.
     */
    private boolean verificarYGestionarRecuperacion() {
        IProjectManager pm = projectController.getProjectManager();
        if (pm == null || !pm.hasPendingRecovery()) {
            return true; // No hay nada que recuperar, flujo normal.
        }

        logger.info("[GeneralController] Detectada sesión de recuperación pendiente. Solicitando decisión al usuario...");

        // Limpiamos la clave de configuración para que no vuelva a saltar en el futuro
        configuration.setString(servicios.ConfigKeys.PROYECTO_RECUPERACION_PENDIENTE, "");
        try {
            configuration.guardarConfiguracion(configuration.getConfig());
        } catch (java.io.IOException e) {
            logger.error("Error al guardar la configuración tras limpiar la clave de recuperación.", e);
        }

        String[] opciones = {"Restaurar Sesión", "Empezar de Cero", "Cancelar"};
        int seleccion = JOptionPane.showOptionDialog(
                null,
                "<html>Se ha detectado una sesión anterior con cambios sin guardar.<br>" +
                "¿Deseas <b>restaurar</b> el trabajo de la sesión anterior o empezar un <b>proyecto nuevo</b>?</html>",
                "Recuperación de Proyecto",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]);

        if (seleccion == 0) { // Restaurar Sesión
            try {
                pm.cargarDesdeRecuperacion(pm.getArchivoRecuperacionPath());
                logger.info("  -> Sesión de recuperación restaurada con éxito.");
                // Si restauramos con éxito, ya no hay recuperación pendiente (se borra al cargar o guardar)
                pm.eliminarSesionDeRecuperacion(); 
                return true;
            } catch (Exception e) {
                logger.error("Error al restaurar la sesión de recuperación.", e);
                JOptionPane.showMessageDialog(null, "No se pudo restaurar la sesión anterior.", "Error de Recuperación", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (seleccion == 1) { // Empezar de Cero
            logger.info("  -> El usuario decidió descartar la recuperación y empezar de cero.");
            pm.eliminarSesionDeRecuperacion();
            pm.nuevoProyecto(); // Aseguramos estado limpio
            return true;
        } else {
            // Cancelar (seleccion == 2 o cerrar el diálogo)
            logger.debug("  -> El usuario canceló el diálogo de recuperación.");
            return false;
        }
    } // --- Fin del método verificarYGestionarRecuperacion ---

    private void salirModo(WorkMode modo) {
    	appModeService.salirModo(modo);
    }

    private void entrarModo(WorkMode modoAlQueSeEntra) {
    	appModeService.entrarModo(modoAlQueSeEntra);
    }
    
    private void actualizarEstadoUiParaModo(WorkMode modoActual) {
        appModeService.actualizarUiModo(modoActual, this.actionMap);
    }

    @Override
    public void onProjectStateChanged(boolean hasUnsavedChanges) {
        // Este método es llamado por ProjectManager cuando el estado de "sucio" cambia.
        // Su única responsabilidad es actualizar el título de la ventana.
        logger.debug(
                "[GeneralController] Notificación recibida: el estado del proyecto ha cambiado. Actualizando título.");
        actualizarTituloVentana();
    } // ---FIN de metodo onProjectStateChanged---

    /**
     * Orquesta la transición para entrar o salir del modo de pantalla completa.
     * Este método es llamado por la ToggleFullScreenAction y delega la manipulación
     * directa del JFrame al ViewManager, manteniendo la lógica de decisión
     * centralizada.
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

    // ***********************************************************************************************************************
    // INICIO SINCRONIZACION

    
    public void sincronizarEstadoBotonesDeModo() {
        appModeService.sincronizarBotonesModo(this.actionMap);
    }

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
        sincronizarEstadoBotonesDeModo();

        // 2. Sincronizar los botones de MODO DE VISUALIZACIÓN (DisplayMode).
        displayModeManager.sincronizarEstadoBotonesDisplayMode();

        // 3. Delegar el resto de la sincronización específica del modo al
        // VisorController.
        visorController.sincronizarComponentesDeModoVisualizador();

        // 4. Sincronizar los controles de subcarpetas de forma centralizada.
        sincronizarControlesDeSubcarpetas();

        // ***** INICIO DE LA MODIFICACIÓN *****
        // 5. Sincronizar el botón de ordenación.
        sincronizarBotonDeOrdenacion();
        // ***** FIN DE LA MODIFICACIÓN *****
                                                                                               
        // 6. Asegurar que los paneles básicos (Lista, Miniaturas) sean visibles si corresponde.
        if (viewManager != null) {                                                             
            viewManager.asegurarVisibilidadPanelesBase();                                      
        }                                                                                      
                                                                                               
        actualizarTituloVentana();                                                             
        
        // Actualizar la barra de estado inferior
        if (statusBarManager != null) {
            statusBarManager.actualizar();
        }
        
        logger.debug("--- [GeneralController] SINCRONIZACIÓN MAESTRA DE UI COMPLETADA ---");
                                                                                               
    } // --- FIN del metodo sincronizarTodaLaUIConElModelo ---

    /**
     * Recorre todas las actions del actionMap y, si son de tipo
     * SetFilterTypeAction,
     * les ordena que se sincronicen con el estado actual del controlador.
     */
    public void sincronizarAccionesDeTipoFiltro() {
        if (actionMap == null)
            return;

        for (Action action : actionMap.values()) {
            if (action instanceof SetFilterTypeAction) {
                ((SetFilterTypeAction) action).sincronizarEstadoConControlador();
            }
        }
    } // --- Fin del método sincronizarAccionesDeTipoFiltro ---

    // **************************************************************************************************************************
    // FIN SINCRONIZACION

    /**
     * Panea la imagen al borde especificado del panel de visualización.
     * Esta es la lógica de paneo ABSOLUTO.
     * 
     * @param direction La dirección (UP, DOWN, LEFT, RIGHT) a la que panear la
     *                  imagen.
     */
    public void panImageToEdge(Direction direction) {
        zoomPanService.panToEdge(direction);
    }

    public void panImageIncrementally(Direction direction, int amount) {
        zoomPanService.panIncrementally(direction, amount);
    }

    /**
     * Actúa como un router para la acción de marcar/desmarcar una imagen.
     * Delega la solicitud al controlador del modo de trabajo activo.
     */
    public void solicitudAlternarMarcaImagenActual() {
        logger.debug("[GeneralController] Recibida solicitud para alternar marca. Modo actual: "
                + model.getCurrentWorkMode());
        
        // --- NUEVA LÓGICA DE SEGURIDAD: Recuperación de sesión ---
        // Si no estamos en modo proyecto y hay una recuperación pendiente, preguntamos ANTES de marcar.
        if (!model.isEnModoProyecto()) {
            if (!verificarYGestionarRecuperacion()) {
                logger.debug("  -> Acción de marcar cancelada por el usuario en el diálogo de recuperación.");
                return;
            }
        }
        // --------------------------------------------------------

        if (model.isEnModoProyecto()) {
            projectController.solicitudAlternarMarcaImagen();
        } else {
            visorController.solicitudAlternarMarcaDeImagenActual();
        }
    } // --- Fin del método solicitudAlternarMarcaImagenActual ---

    public void solicitarEntrarEnModoProyecto() {
        logger.debug("[GeneralController] Solicitud para entrar en modo proyecto.");
        
        // --- NUEVA LÓGICA DE SEGURIDAD: Recuperación de sesión ---
        if (!model.isEnModoProyecto()) {
            if (!verificarYGestionarRecuperacion()) {
                logger.debug("  -> Cambio a modo proyecto cancelado por el usuario en el diálogo de recuperación.");
                // Sincronizar el estado de los botones (por si venimos de un ToggleButton)
                sincronizarEstadoBotonesDeModo();
                return;
            }
        }
        // --------------------------------------------------------

        cambiarModoDeTrabajo(VisorModel.WorkMode.PROYECTO);
    } // --- Fin del método solicitarEntrarEnModoProyecto ---

    // **************************************************************************************
    // IMPLEMENTACION INTERFAZ IModoController

    @Override
    public void aumentarTamanoMiniaturas() {
        logger.debug("[GeneralController] Delegando 'aumentarTamanoMiniaturas' al controlador del modo: {}",
                model.getCurrentWorkMode());
        if (model.isEnModoProyecto()) {
            projectController.aumentarTamanoMiniaturas();
        } else {
            // Sirve tanto para VISUALIZADOR como para CARROUSEL
            visorController.aumentarTamanoMiniaturas();
        }
    } // ---FIN de metodo aumentarTamanoMiniaturas---

    @Override
    public void reducirTamanoMiniaturas() {
        logger.debug("[GeneralController] Delegando 'reducirTamanoMiniaturas' al controlador del modo: {}",
                model.getCurrentWorkMode());
        if (model.isEnModoProyecto()) {
            projectController.reducirTamanoMiniaturas();
        } else {
            // Sirve tanto para VISUALIZADOR como para CARROUSEL
            visorController.reducirTamanoMiniaturas();
        }
    } // ---FIN de metodo reducirTamanoMiniaturas---

    /**
     * Delega una solicitud de refresco al controlador del modo de trabajo activo.
     * AHORA ES INTELIGENTE: Si estamos en el visualizador, inicia una
     * sincronización con la BD.
     */
    public void solicitarRefrescoDelModoActivo() {
        logger.debug("[GeneralController] Enrutando solicitud de refresco para el modo: " + model.getCurrentWorkMode());

        // --- INICIO DE LA MODIFICACIÓN ---
        if (model.getCurrentWorkMode() == WorkMode.VISUALIZADOR) {
            // Si estamos en el modo visualizador, "Refrescar" significa "Sincronizar con
            // Disco".
            logger.info("Refresco solicitado en Modo Visualizador. Iniciando sincronización con la BD...");
            
            // --- NUEVO: Asegurar registro de discos antes de sincronizar ---
            if (this.dataController != null && this.dataController.getDataManager() != null) {
                this.dataController.getDataManager().ensureAllDrivesRegistered();
            }

            if (imageListManager != null) {
                imageListManager.sincronizarCarpetaConBD();
            } else {
                logger.error("ImageListManager es nulo. No se puede iniciar la sincronización.");
            }
        } else if (model.isEnModoProyecto()) {
            // Para el modo proyecto, el refresco sigue siendo la lógica original.
            projectController.solicitarRefresco();
        } else {
            // Para otros modos como Carrusel, por ahora mantenemos la lógica antigua de
            // refresco.
            visorController.ejecutarRefrescoCompleto();
        }
        // --- FIN DE LA MODIFICACIÓN ---

    } // ---FIN del metodo solicitarRefrescoDelModoActivo ---

    public void solicitarAumentoTamanoMiniaturas() {
        logger.debug("[GeneralController] Enrutando solicitud para aumentar tamaño de miniaturas.");
        // Llama al método de la interfaz IModoController.
        // El método de abajo se encargará de delegar al controlador correcto.
        aumentarTamanoMiniaturas();
    } // ---FIN de metodo solicitarAumentoTamanoMiniaturas---

    public void solicitarReduccionTamanoMiniaturas() {
        logger.debug("[GeneralController] Enrutando solicitud para reducir tamaño de miniaturas.");
        // Llama al método de la interfaz IModoController.
        reducirTamanoMiniaturas();
    } // ---FIN de metodo solicitarReduccionTamanoMiniaturas---

    // *************************************************************************************************************************
    // *************************************************************************
    // IMPLEMENTACIÓN DE LA INTERFAZ IModoController
    // *************************************************************************************************************************

    // --- Implementación de IModoController (delegando al controlador de modo
    // activo) ---
    // NOTA: La lógica interna de estos métodos seguirá residiendo en
    // VisorController y ProjectController
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

    @Override // ESTO ES UNA IMPLEMENTACIÓN DE LA INTERFAZ IModoController
    public void aplicarZoomConRueda(MouseWheelEvent e) {
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR) {
            visorController.aplicarZoomConRueda(e);
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.aplicarZoomConRueda(e);
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.CARROUSEL) {
            visorController.aplicarZoomConRueda(e);
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.DATOS) {
            visorController.aplicarZoomConRueda(e);
        }

        // log [GeneralController] Delegando aplicarZoomConRueda
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
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.CARROUSEL) {
            visorController.aplicarPan(deltaX, deltaY);
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.DATOS) {
            visorController.aplicarPan(deltaX, deltaY);
        }

        // log [GeneralController] Delegando aplicarPan
        logger.debug("[GeneralController] Delegando aplicarPan a " + model.getCurrentWorkMode());

    }// --- FIN del metodo aplicarPan ---

    @Override
    public void iniciarPaneo(MouseEvent e) {
        // La lógica de guardar las coordenadas ahora vive en GlobalInputManager.
        // Este método solo delega la notificación al controlador de modo activo.
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR) {
            visorController.iniciarPaneo(e);
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.iniciarPaneo(e);
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.CARROUSEL) {
            visorController.iniciarPaneo(e);
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.DATOS) {
            visorController.iniciarPaneo(e);
        }

        logger.debug("[GeneralController] Delegando notificación de iniciarPaneo a {}", model.getCurrentWorkMode());
    } // --- FIN del metodo iniciarPaneo ---

    /**
     * Notifica a todas las acciones sensibles al contexto para que actualicen su
     * estado 'enabled'.
     * Este es el método central para llamar después de un cambio de estado global,
     * como activar/desactivar la sincronización.
     */
    public void notificarAccionesSensiblesAlContexto() { // weno
        logger.debug("[GeneralController] Notificando a todas las acciones sensibles al contexto...");
        if (actionMap == null || model == null)
            return;

        // Itera por todas las acciones del mapa
        for (Action action : actionMap.values()) {
            // Comprueba si la acción implementa nuestra interfaz
            if (action instanceof ContextSensitiveAction) {
                // Si es así, la "castea" de forma segura y llama a su método de actualización
                ((ContextSensitiveAction) action).updateEnabledState(model);
            }
        }

        // Adicionalmente, forzamos la sincronización del botón de sync para asegurar su
        // estado visual.
        Action syncAction = actionMap.get(AppActionCommands.CMD_TOGGLE_SYNC_VISOR_CARRUSEL);
        if (syncAction != null && configAppManager != null) {
            // Le pedimos al ConfigAppManager que aplique el estilo visual correcto al botón
            // de Sync
            configAppManager.actualizarAspectoBotonToggle(syncAction, model.isSyncVisualizadorCarrusel());
        }

        // Aseguramos que el borde también se actualice en cualquier notificación
        // general.
        actualizarBordeDeSincronizacion(model.isSyncVisualizadorCarrusel());

        logger.debug("[GeneralController] Notificación completada.");
    } // --- Fin del método notificarAccionesSensiblesAlContexto ---

    /**
     * Orquesta el cambio de modo de carga de subcarpetas para el modo Visualizador.
     * Este método se encarga de la lógica de alto nivel, incluyendo la
     * sincronización final.
     * 
     * @param nuevoEstadoIncluirSubcarpetas El estado deseado: true para cargar
     *                                      subcarpetas, false para no hacerlo.
     */
    public void solicitarCambioModoCargaSubcarpetas(boolean nuevoEstadoIncluirSubcarpetas) {
        logger.debug("[GeneralController] Solicitud para cambiar modo de carga de subcarpetas a: "
                + nuevoEstadoIncluirSubcarpetas);

        // --- INICIO DE LA MODIFICACIÓN (LA GUARDA DE SEGURIDAD) ---
        // Comprobamos si el modelo YA está en el estado que se nos pide.
        // Si es así, no hay nada que hacer más que asegurar que la UI esté
        // sincronizada.
        boolean estadoActualIncluyeSubcarpetas = !model.isMostrarSoloCarpetaActual();
        if (estadoActualIncluyeSubcarpetas == nuevoEstadoIncluirSubcarpetas) {
            logger.debug(
                    "  -> El modelo ya está en el estado deseado. Sincronizando UI por si acaso y deteniendo proceso.");
            sincronizarControlesDeSubcarpetas(); // Aseguramos que los botones reflejen el estado correcto.
            return; // Detenemos la ejecución para romper el bucle.
        }
        // --- FIN DE LA MODIFICACIÓN ---

        // 1. Validar que estemos en un modo compatible para esta operación.
        if (model.getCurrentWorkMode() != VisorModel.WorkMode.VISUALIZADOR
                && model.getCurrentWorkMode() != VisorModel.WorkMode.CARROUSEL) {
            logger.warn("  -> Operación cancelada: El modo actual (" + model.getCurrentWorkMode()
                    + ") no soporta esta acción.");
            sincronizarControlesDeSubcarpetas(); // Revertimos visualmente por si acaso.
            return;
        }

        // 2. Validar dependencias.
        if (visorController == null || model == null || configuration == null || displayModeManager == null) {
            logger.error(
                    "  ERROR [GeneralController]: Dependencias críticas (visorController, model, config, displayModeManager) nulas. Abortando.");
            return;
        }

        // 3. Guardar la clave de la imagen actual ANTES de cualquier cambio.
        final String claveAntesDelCambio = model.getSelectedImageKey();
        logger.debug("  -> Clave de imagen a intentar mantener: " + claveAntesDelCambio);

        // 4. Actualizar el estado en el Modelo y la Configuración.
        model.setMostrarSoloCarpetaActual(!nuevoEstadoIncluirSubcarpetas);
        configuration.setString(ConfigKeys.COMPORTAMIENTO_CARGAR_SUBCARPETAS,
                String.valueOf(nuevoEstadoIncluirSubcarpetas));

        // 5. Definir la acción de sincronización que se ejecutará DESPUÉS de la carga.
        Runnable accionPostCarga = () -> {
            logger.debug("  [Callback Post-Carga] Tarea de carga finalizada. Ejecutando sincronización maestra...");

            // a) Sincronizar toda la UI (botones, menús, estados, etc.).
            this.sincronizarTodaLaUIConElModelo();

            // b) Repoblar el Grid con la nueva lista.
            if (displayModeManager != null) {
                displayModeManager.poblarGridConModelo(model.getModeloLista());
                displayModeManager.sincronizarSeleccionGrid();
            }

            logger.debug("  [Callback Post-Carga] Sincronización finalizada.");
        };

        // 6. Delegar la tarea de carga de bajo nivel al VisorController.
        logger.debug("  -> Delegando a VisorController la tarea de recargar la lista de imágenes...");
        this.imageListManager.cargarListaImagenes(claveAntesDelCambio, accionPostCarga);

    } // --- FIN del metodo solicitarCambioModoCargaSubcarpetas ---

    /**
     * MÉTODO DE SINCRONIZACIÓN CENTRALIZADO.
     * Lee el estado actual del modelo y actualiza el estado 'selected' y la
     * apariencia
     * de TODOS los controles relacionados con la carga de subcarpetas (el botón
     * toggle y los dos radio-botones del menú).
     * Esta es la ÚNICA fuente de verdad para la sincronización de estos
     * componentes.
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

        logger.debug("  -> Sincronizados controles de subcarpetas. Estado actual (incluir): "
                + estadoActualIncluyeSubcarpetas);
    } // --- Fin del método sincronizarControlesDeSubcarpetas ---

    /**
     * Punto de entrada principal para cargar una nueva carpeta sin una preselección
     * específica.
     * Delega a la versión más completa del método pasando null como clave a
     * seleccionar.
     * 
     * @param nuevaCarpeta La nueva carpeta raíz a visualizar.
     */
    public void solicitarCargaDesdeNuevaRaiz(Path nuevaCarpeta) {

        solicitarCargaDesdeNuevaRaiz(nuevaCarpeta, null);
    } // --- Fin del método solicitarCargaDesdeNuevaRaiz (simple) ---

    public void solicitarCargaDesdeNuevaRaiz(Path nuevaCarpeta, String claveASeleccionar) {
        logger.debug("--->>> [GeneralController] Solicitud para cargar desde nueva raíz: " + nuevaCarpeta);

        // --- INICIO DE LA CORRECCIÓN ---
        // 1. Delegar el reseteo de CUALQUIER tipo de filtro (persistente o en vivo) al
        // FilterManager.
        if (filterManager.isFilterActive()) {
            filterManager.resetPersistentFilterState();
        }
        if (model.isLiveFilterActive()) {
            onLiveFilterStateChanged(false);
        }
        // --- FIN DE LA CORRECCIÓN ---

        if (nuevaCarpeta == null || !Files.isDirectory(nuevaCarpeta)) {
            return;
        }
        if (model == null || visorController == null || displayModeManager == null) {
            return;
        }

        // 1. Establecer la carpeta raíz en el modelo primero.
        // Esto es necesario porque el refresco -> sincronizarCarpetaConBD
        // usa model.getCarpetaRaizActual() para saber qué escanear.
        model.setCarpetaRaizActual(nuevaCarpeta);
        model.setCarpetaRaizInicialParaVisualizador(nuevaCarpeta);

        // 2. Sincronizar el árbol de carpetas.
        if (this.folderTreeManager != null) {
            this.folderTreeManager.sincronizarArbolConCarpeta(nuevaCarpeta);
        }

        // 3. --- LÓGICA UNIFICADA DE CARGA Y SINCRONIZACIÓN ---
        // Al seleccionar una nueva raíz, queremos que el comportamiento sea 
        // idéntico al botón "Actualizar": Escanear disco y sincronizar BD.
        if (model.getCurrentWorkMode() == WorkMode.VISUALIZADOR || model.getCurrentWorkMode() == WorkMode.CARROUSEL) {
             logger.info("  -> Navegación en modo Visor/Carrusel. Disparando sincronización con disco...");
             this.sincronizarTodaLaUIConElModelo();
             solicitarRefrescoDelModoActivo();
             return;
        }

        // Si estamos en otros modos (ej: Proyecto), mantenemos la carga normal desde la BD.
        Runnable accionPostCarga = () -> {
            logger.debug("  [Callback Post-Carga de Nueva Raíz] Tarea de carga finalizada.");

            this.sincronizarTodaLaUIConElModelo();

            if (displayModeManager != null) {
                displayModeManager.poblarGridConModelo(model.getModeloLista());
                displayModeManager.sincronizarSeleccionGrid();
            }
        };

        this.imageListManager.cargarListaImagenes(claveASeleccionar, accionPostCarga);

    } // --- Fin del método solicitarCargaDesdeNuevaRaiz (con preselección) ---

    // **********************************************************************************
    // FIN IMPLEMENTACION INTERFAZ IModoController

    // ***************************************************************************************************************
    // INICIO GETTERS

    public ToolbarManager getToolbarManager() {
        return this.toolbarManager;
    }

    public VisorModel getModel() {
        return this.model;
    }

    public void setDisplayModeManager(DisplayModeManager displayModeManager) {
        this.displayModeManager = Objects.requireNonNull(displayModeManager, "DisplayModeManager no puede ser nulo");
    }

    public void setConfiguration(ConfigurationManager configuration) {
        this.configuration = Objects.requireNonNull(configuration, "ConfigurationManager no puede ser nulo");
    }

    // ******************************************************************************************************************
    // FIN GETTERS

    /**
     * MÉTODO ORQUESTADOR CENTRAL PARA ALTERNAR EL MODO DE CARGA DE SUBCARPETAS.
     * Invierte el estado actual del modelo y luego inicia el proceso de recarga.
     * Utiliza un flag de bloqueo para evitar ejecuciones concurrentes.
     */
    public void solicitarToggleModoCargaSubcarpetas() {
        // Si ya hay una operación en curso, la ignoramos.
        if (isChangingSubfolderMode) {
            logger.warn(
                    "  [GeneralController] ADVERTENCIA: Se ha ignorado una solicitud de toggle de subcarpetas porque ya hay una en progreso.");
            return;
        }

        try {
            isChangingSubfolderMode = true; // --- BLOQUEAMOS ---
            logger.debug("[GeneralController] Solicitud para ALTERNAR modo de carga de subcarpetas.");

            // 1. Invertir el estado actual del modelo. Esta es la lógica central.
            boolean nuevoEstadoSoloCarpeta = !model.isMostrarSoloCarpetaActual();
            model.setMostrarSoloCarpetaActual(nuevoEstadoSoloCarpeta);

            // 2. Actualizar la configuración para que se guarde.
            configuration.setString(ConfigKeys.COMPORTAMIENTO_CARGAR_SUBCARPETAS,
                    String.valueOf(!nuevoEstadoSoloCarpeta));
            logger.debug("  -> Estado del modelo cambiado a: isMostrarSoloCarpetaActual=" + nuevoEstadoSoloCarpeta);

            // 3. El resto de la lógica es la que ya teníamos...
            final String claveAntesDelCambio = model.getSelectedImageKey();
            logger.debug("  -> Clave de imagen a intentar mantener: " + claveAntesDelCambio);

            Runnable accionPostCarga = () -> {
                try {
                    logger.debug(
                            "  [Callback Post-Carga] Tarea de carga finalizada. Ejecutando sincronización maestra...");
                    this.sincronizarTodaLaUIConElModelo();

                    if (displayModeManager != null) {
                        displayModeManager.poblarGridConModelo(model.getModeloLista());
                        displayModeManager.sincronizarSeleccionGrid();
                    }

                    logger.debug("  [Callback Post-Carga] Sincronización finalizada.");
                } finally {
                    isChangingSubfolderMode = false; // --- DESBLOQUEAMOS ---
                }
            };

            if (model.getCurrentWorkMode() == WorkMode.VISUALIZADOR || model.getCurrentWorkMode() == WorkMode.CARROUSEL) {
                logger.info("  -> Alternancia de subcarpetas en modo Visor/Carrusel. Iniciando sincronización con disco...");
                this.sincronizarTodaLaUIConElModelo();
                boolean syncIniciada = this.imageListManager.sincronizarCarpetaConBD();
                if (!syncIniciada) {
                    this.imageListManager.cargarListaImagenes(claveAntesDelCambio, accionPostCarga);
                } else {
                    isChangingSubfolderMode = false;
                }
            } else {
                this.imageListManager.cargarListaImagenes(claveAntesDelCambio, accionPostCarga);
            }

        } catch (Exception e) {
            logger.error("ERROR INESPERADO en solicitarToggleModoCargaSubcarpetas: " + e.getMessage());
            e.printStackTrace();
            isChangingSubfolderMode = false; // --- DESBLOQUEAMOS EN CASO DE ERROR ---
        }
    } // --- FIN del metodo solicitarToggleModoCargaSubcarpetas ---

    public void resortFileListAndSyncButton() {
        searchSortService.resortFileListAndSyncButton();
    }

    public void sincronizarBotonDeOrdenacion() {
        searchSortService.sincronizarBotonDeOrdenacion();
    }

    public void buscarSiguienteCoincidencia() {
        searchSortService.buscarSiguienteCoincidencia();
    }

    /**
     * Es llamado por la ToggleLiveFilterAction. Delega el cambio de estado
     * al FilterManager y luego sincroniza la UI de los controles relacionados.
     * 
     * @param isSelected El nuevo estado del modo filtro.
     */
    public void onLiveFilterStateChanged(boolean isSelected) {
        searchSortService.onLiveFilterStateChanged(isSelected);
    }

    /**
     * Es llamado por la AddFilterAction. Orquesta la adición de un nuevo filtro.
     * 
     * @param source La fuente del filtro (FILENAME o FOLDER_PATH).
     * @param type   El tipo de filtro (CONTAINS o DOES_NOT_CONTAIN).
     */
    public void solicitarAnadirFiltro(FilterSource source, FilterType type) {
        filterService.añadirFiltro(filterManager.getFiltroActivoSource(), type);
    }

    public void solicitarAnadirFiltroSilencioso(String texto, FilterSource source, FilterType type) {
        filterService.añadirFiltroSilencioso(texto, source, type);
    }

    public void solicitarEliminarFiltroSeleccionado() {
        filterService.eliminarFiltroSeleccionado();
    }

    public void solicitarLimpiarTodosLosFiltros() {
        filterService.limpiarTodosLosFiltros();
    }

    /**
     * NUEVO MÉTODO HELPER.
     * Cumple la "Regla del Reset Total": si el filtro rápido ("Tornado") está
     * activo, lo desactiva y limpia su JTextField asociado.
     */
    public void limpiarEstadoFiltroRapidoSiActivo() {
        searchSortService.limpiarEstadoFiltroRapidoSiActivo();
    }

    /**
     * Cambia el tipo de filtro que se usará al añadir un nuevo criterio.
     * Es llamado por las Actions de los JToggleButtons de tipo de filtro.
     * 
     * @param nuevoSource El nuevo FilterSource a establecer como activo.
     */
    public void solicitarCambioTipoFiltro(FilterSource nuevoSource) {
        filterService.cambiarTipoFiltro(nuevoSource);
    }

    /**
     * Orquesta la conversión del filtro rápido (Tornado) en un filtro persistente.
     * Este método es llamado por la Action del botón "hacer persistente".
     * AÑADE el filtro del Tornado a los filtros persistentes existentes.
     */
    public void solicitarPersistenciaDeFiltroRapido() {
        searchSortService.solicitarPersistenciaDeFiltroRapido();
    }

    private void sincronizarEstadoControlesTornado() {
        searchSortService.sincronizarEstadoControlesTornado();
    }

    public void handleFilterListClick(java.awt.event.MouseEvent e,
            controlador.managers.filter.FilterCriterion criterion) {
        JList<controlador.managers.filter.FilterCriterion> filterList = registry.get("list.filtrosActivos");
        if (filterList == null || criterion == null)
            return;

        int index = filterList.locationToIndex(e.getPoint());
        if (index == -1)
            return;

        Component rendererComponent = filterList.getCellRenderer().getListCellRendererComponent(filterList, criterion,
                index, true, true);
        if (!(rendererComponent instanceof vista.renderers.FilterCriterionCellRenderer))
            return;
        vista.renderers.FilterCriterionCellRenderer renderer = (vista.renderers.FilterCriterionCellRenderer) rendererComponent;

        java.awt.Rectangle cellBounds = filterList.getCellBounds(index, index);
        int clickX = e.getX() - cellBounds.x;
        int clickY = e.getY() - cellBounds.y;

        // --- Lógica de clic SIMPLIFICADA ---

        if (renderer.getDeleteLabel().getBounds().contains(clickX, clickY)) {
            // Clic en BORRAR
            int confirm = javax.swing.JOptionPane.showConfirmDialog(viewManager.getView(),
                    "¿Deseas eliminar este filtro?", "Confirmar Eliminación", javax.swing.JOptionPane.YES_NO_OPTION);
            if (confirm == javax.swing.JOptionPane.YES_OPTION) {
                filterManager.removeFilter(criterion);
                filterManager.gestionarFiltroPersistente();
            }
        } else if (renderer.getLogicLabel().getBounds().contains(clickX, clickY)) {
            // Clic en LÓGICA
            criterion.setLogic(criterion.getLogic() == controlador.managers.filter.FilterCriterion.Logic.ADD
                    ? controlador.managers.filter.FilterCriterion.Logic.NOT
                    : controlador.managers.filter.FilterCriterion.Logic.ADD);
            filterList.repaint();
            filterManager.gestionarFiltroPersistente();
        }
        // No hay más interacciones en la fila. Clicar en el tipo o el valor ya no hace
        // nada.

    } // ---FIN de metodo handleFilterListClick---

    public void solicitarAnadirFiltro() {
        limpiarEstadoFiltroRapidoSiActivo();

        // 1. Crear una instancia de nuestro nuevo diálogo.
        vista.dialogos.FilterDialog dialog = new vista.dialogos.FilterDialog(
                (JFrame) viewManager.getView(),
                this.typeIconsMap);

        // 2. Mostrar el diálogo y esperar a que el usuario lo cierre.
        FilterCriterion newCriterion = dialog.showDialog();

        // 3. Si el usuario pulsó "Aceptar" (el resultado no es null)...
        if (newCriterion != null) {
            String valor = newCriterion.getValue();
            if (valor != null && !valor.isBlank()) {
                String[] terms = valor.split(",");
                boolean added = false;
                for (String term : terms) {
                    String trimmed = term.trim();
                    if (!trimmed.isEmpty()) {
                        FilterCriterion c = new FilterCriterion(trimmed, newCriterion.getSource(), newCriterion.getType());
                        c.setSourceType(newCriterion.getSourceType());
                        c.setLogic(newCriterion.getLogic());
                        filterManager.addFilter(c);
                        added = true;
                    }
                }
                if (added) {
                    filterManager.gestionarFiltroPersistente();
                }
            }
        }
    } // ---FIN de metodo solicitarAnadirFiltro---

    public JPopupMenu crearMenuContextualParaArbol() {
        return menuPopupManager.crearMenuContextualParaArbol();
    }

    public void solicitarAbrirCarpetaDesdeArbol() {
        navigationService.abrirCarpetaDesdeArbol();
    }

    public void solicitarEntrarEnCarpetaDesdeArbol() {
        navigationService.entrarEnCarpetaDesdeArbol();
    }

    public void solicitarNavegarCarpetaAnterior() {
        navigationService.navegarCarpetaAnterior();
    }

    public void solicitarNavegarCarpetaSiguiente() {
        navigationService.navegarCarpetaSiguiente();
    }

    public void solicitarNavegarCarpetaRaiz() {
        navigationService.navegarRaiz();
    }

    public void solicitarSalirDeSubcarpeta() {
        navigationService.salirDeSubcarpeta();
    }

    public void setFolderNavigationManager(FolderNavigationManager folderNavManager) {
        this.folderNavManager = Objects.requireNonNull(folderNavManager);
    }

    /**
     * Comanda a la VisorView para que actualice su borde visual de sincronización.
     * Este método actúa como un puente seguro entre las acciones y la vista.
     * 
     * @param activado El nuevo estado de sincronización.
     */
    public void actualizarBordeDeSincronizacion(boolean activado) {
        if (visorController != null && visorController.getView() != null) {
            visorController.getView().actualizarBordeDeSincronizacion(activado);
        }
    } // --- Fin del método actualizarBordeDeSincronizacion ---

    public void setFolderTreeManager(FolderTreeManager folderTreeManager) {
        this.folderTreeManager = Objects.requireNonNull(folderTreeManager);
    } // --- FIN del metodo setFolderTreeManager ---

    public void setFilterManager(FilterManager filterManager) {
        this.filterManager = Objects.requireNonNull(filterManager,
                "FilterManager no puede ser null en GeneralController");
    } // --- Fin del método setFilterManager ---

    public ComponentRegistry getRegistry() {
        return this.registry;
    }

    public FilterSource getFiltroActivoSource() {
        return filterManager.getFiltroActivoSource();
    } // --- Fin del método getFiltroActivoSource ---

    @Override
    public void solicitarRefresco() {
        // Esta implementación del método de la interfaz simplemente llama
        // a nuestro método "router" más descriptivo.
        solicitarRefrescoDelModoActivo();
    }// FIN del metodo solicitarRefresco ---

    /**
     * Implementación de la interfaz MasterListChangeListener.
     * Este método es el "cartero" central. Se ejecuta cada vez que VisorModel
     * notifica un cambio en su lista maestra. Su única responsabilidad es
     * tomar esa nueva lista y entregarla al grid del modo de trabajo activo.
     * 
     * @param newMasterList El nuevo modelo de lista que se debe mostrar.
     * @param source        El objeto que originó el cambio, para evitar bucles.
     */
    @Override
    public void onMasterListChanged(DefaultListModel<String> newMasterList, Object source) {
        if (registry == null || model == null) {
            logger.warn("WARN [onMasterListChanged]: Registry o Model nulos. No se puede actualizar el grid.");
            return;
        }

        JList<String> gridTarget;
        WorkMode currentMode = model.getCurrentWorkMode();

        if (currentMode == WorkMode.PROYECTO) {
            gridTarget = registry.get("list.grid.proyecto");
        } else {
            // --- CORRECCIÓN: Usamos la clave correcta "list.grid" ---
            gridTarget = registry.get("list.grid");
        }

        if (gridTarget != null) {
            SwingUtilities.invokeLater(() -> {
                gridTarget.setModel(newMasterList);
                logger.debug("[MasterListChangeListener] Grid para modo {} actualizado con {} elementos.", currentMode,
                        newMasterList.getSize());
            });
        } else {
            logger.error("ERROR [onMasterListChanged]: No se encontró el JList del grid para el modo {}.", currentMode);
        }
    } // --- Fin del método onMasterListChanged ---

    public void setImageListManager(ImageListManager imageListManager) {
        this.imageListManager = imageListManager;
    }

    public void setTypeIconsMap(
            Map<controlador.managers.filter.FilterCriterion.SourceType, javax.swing.Icon> typeIconsMap) {
        this.typeIconsMap = typeIconsMap;
    }

    public void setDataController(DataController dataController) {
        this.dataController = dataController;
    }

    public void setFilterService(FilterService filterService) {
        this.filterService = filterService;
    }

    public void setNavigationService(NavigationService navigationService) {
        this.navigationService = navigationService;
    }
    
    public void setProjectLifecycleService(ProjectLifecycleService s) { 
    	this.projectLifecycleService = s; 
    }
    
    public void setSearchSortService(SearchSortService s) { 
    	this.searchSortService = s; 
    }
    
    public void setZoomPanService(ZoomPanService s) { 
    	this.zoomPanService = s; 
    }
    
    public void setAppModeService(AppModeService s) { 
    	this.appModeService = s; 
    }
    
    public void setMenuPopupManager(MenuPopupManager menuPopupManager) {
        this.menuPopupManager = menuPopupManager;
    }
} // --- Fin de la clase GeneralController ---
