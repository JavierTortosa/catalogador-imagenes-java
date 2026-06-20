package controlador.services;

import controlador.ClientController;
import controlador.DataController;
import controlador.ProjectController;
import controlador.VisorController;
import controlador.commands.AppActionCommands;
import controlador.managers.CarouselManager;
import controlador.managers.ConfigApplicationManager;
import controlador.managers.DisplayModeManager;
import controlador.managers.ImageListManager;
import controlador.managers.InfobarStatusManager;
import controlador.managers.ToolbarManager;
import controlador.managers.ViewManager;
import controlador.managers.interfaces.IProjectManager;
import controlador.utils.ComponentRegistry;
import modelo.ListContext;
import modelo.VisorModel;
import modelo.VisorModel.DisplayMode;
import modelo.VisorModel.WorkMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import servicios.ConfigurationManager;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.Component;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Servicio maestro que orquesta los cambios de modo de trabajo 
 * y la sincronización de los componentes de la interfaz.
 */
public class AppModeService {
    private static final Logger logger = LoggerFactory.getLogger(AppModeService.class);

    private final VisorModel model;
    private final ViewManager viewManager;
    private final ToolbarManager toolbarManager;
    private final DisplayModeManager displayModeManager;
    private final ConfigApplicationManager configAppManager;
    private final InfobarStatusManager statusBarManager;

    private VisorController visorController;
    private ProjectController projectController;
    private DataController dataController;
    private ClientController clientController;
    private ConfigurationManager configuration;
    private ComponentRegistry registry;
    private ImageListManager imageListManager;
    private Map<String, Action> actionMap;
    private controlador.managers.TaggingManager taggingManager;

    private volatile boolean isChangingSubfolderMode = false;

    public AppModeService(VisorModel model, ViewManager viewManager, ToolbarManager toolbarManager, 
                          DisplayModeManager displayModeManager, ConfigApplicationManager configAppManager, 
                          InfobarStatusManager statusBarManager) {
        this.model = model;
        this.viewManager = viewManager;
        this.toolbarManager = toolbarManager;
        this.displayModeManager = displayModeManager;
        this.configAppManager = configAppManager;
        this.statusBarManager = statusBarManager;
    } // --- Fin del método AppModeService (constructor) ---


    public void setVisorController(VisorController visorController) {
        this.visorController = visorController;
    } // --- Fin del método setVisorController ---


    public void setProjectController(ProjectController projectController) {
        this.projectController = projectController;
    } // --- Fin del método setProjectController ---


    public void setDataController(DataController dataController) {
        this.dataController = dataController;
    } // --- Fin del método setDataController ---

    public void setClientController(ClientController clientController) {
        this.clientController = clientController;
    } // --- Fin del método setClientController ---


    public void setConfiguration(ConfigurationManager configuration) {
        this.configuration = configuration;
    } // --- Fin del método setConfiguration ---


    public void setRegistry(ComponentRegistry registry) {
        this.registry = registry;
    } // --- Fin del método setRegistry ---


    public void setImageListManager(ImageListManager imageListManager) {
        this.imageListManager = imageListManager;
    } // --- Fin del método setImageListManager ---


    public void setActionMap(Map<String, Action> actionMap) {
        this.actionMap = actionMap;
    } // --- Fin del método setActionMap ---


    public void setTaggingManager(controlador.managers.TaggingManager taggingManager) {
        this.taggingManager = taggingManager;
    } // --- Fin del método setTaggingManager ---


    /**
     * Cambia la tarjeta visible en el CardLayout principal.
     */
    public void cambiarVistaPrincipal(WorkMode modo) {
        String vistaName = switch (modo) {
            case VISUALIZADOR -> "VISTA_VISUALIZADOR";
            case PROYECTO -> "VISTA_PROYECTOS";
            case DATOS -> "VISTA_DATOS";
            case CLIENTE -> "VISTA_CLIENTE";
            case CARROUSEL -> "VISTA_CARROUSEL_WORKMODE";
        };
        viewManager.cambiarAVista("container.workmodes", vistaName);
        logger.debug("[AppModeService] Vista cambiada a: {}", vistaName);
    } // --- Fin del método cambiarVistaPrincipal ---


    /**
     * MÉTODO MAESTRO DE SINCRONIZACIÓN DE UI.
     * Habilita/deshabilita y selecciona/deselecciona componentes de la UI
     * (acciones, botones)
     * basándose en el modo de trabajo actual y el estado del Modelo.
     * 
     * @param modoActual El modo que se acaba de activar.
     */
    public void actualizarUiModo(WorkMode modoActual, Map<String, Action> actionMap) {
        logger.debug("  [GeneralController] Actualizando estado de la UI para el modo: " + modoActual);
        
        if (actionMap == null) return;
        
        // --- 1. LÓGICA DE HABILITACIÓN/DESHABILITACIÓN (Enabled/Disabled) ---
        boolean subcarpetasHabilitado = (modoActual == WorkMode.VISUALIZADOR || modoActual == WorkMode.CARROUSEL);
        boolean abrirCarpetaHabilitado = (modoActual != WorkMode.PROYECTO && modoActual != WorkMode.DATOS);
        
        // 2. Obtenemos todas las acciones relacionadas con esta funcionalidad.
        Action subfolderAction = actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
        Action soloCarpetaAction = actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA);
        Action conSubcarpetasAction = actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS);
        Action abrirCarpetaAction = actionMap.get(AppActionCommands.CMD_ARCHIVO_ABRIR);
        
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
        if (abrirCarpetaAction != null) {
            abrirCarpetaAction.setEnabled(abrirCarpetaHabilitado);
        }
        
        // --- 2. LÓGICA DE SELECCIÓN (Selected/Deselected) para Toggles ---
        
        if (configAppManager != null) {
            // Sincronizar el toggle de subcarpetas
            if (subfolderAction != null) {
                // Leemos el estado del contexto de lista ACTUALMENTE ACTIVO en el modelo.
                // model.isMostrarSoloCarpetaActual() ya es inteligente y devuelve el del
                // contexto correcto.
                
                // log [DEBUG-SYNC] Modo:
                logger.debug("  [DEBUG-SYNC] Modo: " + modoActual
                        + ", Valor de isMostrarSoloCarpetaActual() en modelo: " + model.isMostrarSoloCarpetaActual());

                boolean estadoModeloSubcarpetas = !model.isMostrarSoloCarpetaActual();

                subfolderAction.putValue(Action.SELECTED_KEY, estadoModeloSubcarpetas);
                configAppManager.actualizarAspectoBotonToggle(subfolderAction, estadoModeloSubcarpetas);
            }
            
            // Sincronizar el toggle de proporciones
            Action proporcionesAction = actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);
            if (proporcionesAction != null) {
                // De forma similar, model.isMantenerProporcion() leerá del contexto de zoom
                // correcto.
                boolean estadoModeloProporciones = model.isMantenerProporcion();
                proporcionesAction.putValue(Action.SELECTED_KEY, estadoModeloProporciones);
                configAppManager.actualizarAspectoBotonToggle(proporcionesAction, estadoModeloProporciones);
            }
        }
        
        // --- 3. ACTUALIZACIÓN DE OTROS COMPONENTES ---

        if (this.statusBarManager != null) {
            this.statusBarManager.actualizar();
        }

        // Sincronizar botones de la toolbar con el nuevo modo
        if (this.toolbarManager != null) {
            this.toolbarManager.sincronizarEstadoBotonesToolbar(actionMap, this.model);
        }
        
        logger.debug("  [GeneralController] Estado de la UI actualizado.");
    } // --- Fin del método actualizarUiParaModo ---

    /**
     * Lógica de sincronización de subcarpetas.
     */
    public void gestionarCargaSubcarpetas(boolean incluir, Runnable postCarga) {
        model.setMostrarSoloCarpetaActual(!incluir);
        if (postCarga != null) postCarga.run();
    } // --- Fin del método gestionarCargaSubcarpetas ---


    /**
     * Solicita al ViewManager que actualice el título de la ventana principal.
     * Este método se encarga de la lógica de presentación del título.
     */
    public void actualizarTituloPrincipal() {
        // Aquí podemos omitir la validación de 'visorController != null' que estaba en GeneralController
        // porque el ViewManager es una dependencia directa de este servicio
        // y asumimos que está correctamente inicializado.
        viewManager.actualizarTituloVentana();
        logger.debug("[AppModeService] Título principal de la ventana actualizado.");
    } // --- Fin del método actualizarTituloPrincipal ---


    /**
     * Sincroniza el estado LÓGICO Y VISUAL de los botones de modo de trabajo.
     * Asegura que solo el botón del modo activo esté seleccionado y que se aplique
     * el estilo visual personalizado.
     */
    public void sincronizarBotonesModo(Map<String, Action> actionMap) {
        if (actionMap == null || model == null || configAppManager == null) {
            logger.warn("No se puede sincronizar botones: dependencias nulas.");
            return;
        }

        WorkMode modoActivo = this.model.getCurrentWorkMode();
        String comandoModoActivo = switch (modoActivo) {
            case VISUALIZADOR -> AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR;
            case PROYECTO      -> AppActionCommands.CMD_PROYECTO_GESTIONAR;
            case DATOS         -> AppActionCommands.CMD_MODO_DATOS;
            case CLIENTE       -> AppActionCommands.CMD_MODO_CLIENTE;
            case CARROUSEL     -> AppActionCommands.CMD_VISTA_CAROUSEL;
        };

        List<String> comandosDeModo = List.of(
                AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR,
                AppActionCommands.CMD_PROYECTO_GESTIONAR,
                AppActionCommands.CMD_MODO_DATOS,
                AppActionCommands.CMD_MODO_CLIENTE,
                AppActionCommands.CMD_VISTA_CAROUSEL);

        for (String comando : comandosDeModo) {
            Action action = actionMap.get(comando);
            if (action != null) {
                boolean isSelected = comando.equals(comandoModoActivo);
                action.putValue(Action.SELECTED_KEY, isSelected);
                configAppManager.actualizarAspectoBotonToggle(action, isSelected);
            }
        }
        logger.debug("Botones de modo sincronizados. Activo: {}", comandoModoActivo);
    } // --- Fin del método sincronizarBotonesModo ---


    public boolean cambiarModoDeTrabajo(WorkMode modoDestino, WorkMode modoActual) {
        if (modoActual == modoDestino) return false;

        // --- LÓGICA DE VALIDACIÓN ---
        if (modoDestino == WorkMode.PROYECTO) {
            boolean hayImagenesEnProyecto = !visorController.getProjectManager().getImagenesMarcadas().isEmpty();
            if (!hayImagenesEnProyecto) {
                // Aquí delegamos la UI al manejarAperturaDeProyectoVacio (que también moveremos)
                if (!manejarAperturaDeProyectoVacio()) {
                    return false; // Abortar
                }
            }
        }

        // --- LÓGICA DE CONFIRMACIÓN PARA TRANSICIONES SINCRONIZADAS ---
        boolean esTransicionSincronizable = (modoActual == WorkMode.VISUALIZADOR && modoDestino == WorkMode.CARROUSEL)
                || (modoActual == WorkMode.CARROUSEL && modoDestino == WorkMode.VISUALIZADOR);

        if (esTransicionSincronizable && model.isSyncVisualizadorCarrusel()) {
            String titulo = "Confirmar Transición Sincronizada";
            String mensaje = modoDestino == WorkMode.CARROUSEL
                    ? "<html>El modo <b>Sincronización</b> está activo.<br>Se cargará el estado del Visualizador en el Carrusel.<br><br>¿Continuar?</html>"
                    : "<html>El modo <b>Sincronización</b> está activo.<br>La posición actual del Carrusel se transferirá al Visualizador.<br><br>¿Continuar?</html>";

            int respuesta = javax.swing.JOptionPane.showConfirmDialog(null, mensaje, titulo,
                    javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.INFORMATION_MESSAGE);
            if (respuesta != javax.swing.JOptionPane.YES_OPTION) {
                logger.debug("--- [AppModeService] TRANSICIÓN CANCELADA por el usuario. ---");
                return false;
            }
        }

        // --- EJECUCIÓN ---
        salirModo(modoActual);
        model.setCurrentWorkMode(modoDestino);
        entrarModo(modoDestino);
        actualizarTituloPrincipal(); // Llamamos al método que ya creamos antes
        
        return true; // Éxito
    } // --- Fin del método cambiarModoDeTrabajo ---


	/**
	* Realiza las tareas de "limpieza" o guardado de estado de un modo antes de
	* abandonarlo.
	* 
	* @param modoQueSeAbandona El modo que estamos dejando.
	*/
    public void salirModo(WorkMode modoQueSeAbandona) {
    	logger.debug("  [AppModeService] Saliendo del modo: " + modoQueSeAbandona);

        // --- LÓGICA DE GUARDADO AL SALIR DEL MODO PROYECTO ---
        if (modoQueSeAbandona == WorkMode.PROYECTO) {
            if (projectController != null) {
                // Sincronizamos el estado de la UI (listas, descripción, etc.) al modelo en
                // memoria.
                // ESTO NO GUARDA EN DISCO, solo asegura que el ProjectModel esté actualizado.
                projectController.sincronizarModeloConUI();

                // Guardamos el estado del panel de exportación.
                model.setProjectExportPanelVisible(projectController.isExportPanelVisible());
                logger.debug("    -> Modo Proyecto: Estado de UI sincronizado con el modelo en memoria.");
            }
        }

        // --- LÓGICA DE GUARDADO AL SALIR DEL MODO VISUALIZADOR ---
        if (modoQueSeAbandona == WorkMode.VISUALIZADOR) {
            // Si salimos del modo visualizador Y hay cambios pendientes (el usuario marcó
            // algo),
            // guardamos el estado en el archivo TEMPORAL. Esto preserva el "proyecto sin
            // nombre".
            if (visorController != null && visorController.getProjectManager() != null
                    && visorController.getProjectManager().hayCambiosSinGuardar()) {
                // Solo guardamos si no tenemos un proyecto con nombre. Si lo tenemos, los
                // cambios se quedan en memoria
                // esperando un guardado explícito.
                if (visorController.getProjectManager().getArchivoProyectoActivo() == null) {
                    logger.info(
                            "Saliendo del modo VISUALIZADOR con cambios en proyecto temporal. Guardando en archivo temporal...");
                    visorController.getProjectManager().guardarAArchivo(); // Esto guardará en "seleccion_temporal.prj"
                }
            }
        }

        // --- LÓGICA DE GUARDADO AL SALIR DEL MODO DATOS ---
        if (modoQueSeAbandona == WorkMode.DATOS) {
            if (dataController != null) {
                dataController.guardarContexto();
                logger.debug("    -> Modo Datos: Contexto (árbol y grid) guardado en el modelo.");
            }
        }

        // --- LÓGICA DEL CARRUSEL (se mantiene igual) ---
        if (modoQueSeAbandona == WorkMode.CARROUSEL && !model.isSyncVisualizadorCarrusel()) {
            ListContext carruselCtx = model.getCarouselListContext();
            model.setUltimaCarpetaCarrusel(carruselCtx.getCarpetaRaizContexto());
            model.setUltimaImagenKeyCarrusel(carruselCtx.getSelectedImageKey());
            logger.debug("    -> Modo Carrusel Independiente: Guardando estado en el modelo.");
        }

        if (modoQueSeAbandona == WorkMode.CARROUSEL) {
            CarouselManager carouselManager = visorController.getActionFactory().getCarouselManager();
            if (carouselManager != null) {
                carouselManager.onCarouselModeChanged(false); // Notificar salida
            }
        }

        
        
    } // --- Fin del método salirModo ---

    public void entrarModo(WorkMode modoAlQueSeEntra) {
    	logger.debug("  [AppModeService] Entrando en modo: " + modoAlQueSeEntra);
        if (displayModeManager != null) {
            ListContext contextoDestino = model.getCurrentListContext();
            
            // El modo DATOS requiere trabajar en vista GRID.
            if (modoAlQueSeEntra == WorkMode.DATOS) {
                contextoDestino.setDisplayMode(DisplayMode.GRID);
            }
            
            DisplayMode modoGuardado = contextoDestino.getDisplayMode();
            displayModeManager.switchToDisplayMode(modoGuardado);
        }

        SwingUtilities.invokeLater(() -> {
            logger.debug("    -> [EDT-1] Cambiando tarjeta del CardLayout a: " + modoAlQueSeEntra);

            switch (modoAlQueSeEntra) {
                case VISUALIZADOR:
                    boolean miniaturasVisibles = configuration
                            .getBoolean("interfaz.menu.vista.imagenes_en_miniatura.seleccionado", true);
                    if (registry.get("scroll.miniaturas") != null) {
                        registry.get("scroll.miniaturas").setVisible(miniaturasVisibles);
                    }
                    if (model.isSyncVisualizadorCarrusel()) {
                        model.getVisualizadorListContext().clonarDesde(model.getCarouselListContext());
                    }
                    viewManager.cambiarAVista("container.workmodes", "VISTA_VISUALIZADOR");
                    break;
                case PROYECTO:
                    viewManager.cambiarAVista("container.workmodes", "VISTA_PROYECTOS");
                    break;
                case DATOS:
                    viewManager.cambiarAVista("container.workmodes", "VISTA_DATOS");
                    break;
                case CLIENTE:
                    viewManager.cambiarAVista("container.workmodes", "VISTA_CLIENTE");
                    break;
                case CARROUSEL:
                    viewManager.cambiarAVista("container.workmodes", "VISTA_CARROUSEL_WORKMODE");
                    break;
            }

            JPanel workModesContainer = registry.get("container.workmodes");
            if (workModesContainer != null) {
                workModesContainer.revalidate();
                workModesContainer.repaint();
            }

            SwingUtilities.invokeLater(() -> {
                logger.debug("    -> [EDT-2] Restaurando y sincronizando UI para: " + modoAlQueSeEntra);
                switch (modoAlQueSeEntra) {
                    case VISUALIZADOR:
                        visorController.restaurarUiVisualizador();
                        break;
                    case PROYECTO:
                        projectController.activarVistaProyecto();
                        projectController.configurarContextMenuTablaExportacion();
                        if (model.isProjectExportPanelVisible()) {
                            projectController.setExportPanelVisible(true);
                            projectController.solicitarPreparacionColaExportacion();
                            projectController.sincronizarSeleccionEnTablaExportacion();
                        }
                        break;
                    case CARROUSEL:
                        ListContext contextoCarrusel = model.getCarouselListContext();
                        if (model.isSyncVisualizadorCarrusel())
                            contextoCarrusel.clonarDesde(model.getVisualizadorListContext());
                        else if (contextoCarrusel.getModeloLista() == null
                                || contextoCarrusel.getModeloLista().isEmpty())
                            contextoCarrusel.clonarDesde(model.getVisualizadorListContext());
                        visorController.restaurarUiCarrusel();
                        if (visorController.getActionFactory().getCarouselManager() != null)
                            visorController.getActionFactory().getCarouselManager().onCarouselModeChanged(true);
                        break;
                    case DATOS:
                        if (dataController != null) {
                            Path visPath = taggingManager != null ? taggingManager.getLastActiveImagePath() : null;
                            String visKey = taggingManager != null ? taggingManager.getLastActiveImageKey() : null;
                            logger.debug("TaggingManager >>> path={}, key={}", visPath, visKey);
                            dataController.setPendingSyncFromVisualizador(visPath, visKey);
                            dataController.activate();
                        } else {
                            logger.error("DataController es nulo. No se puede activar el Modo Datos.");
                        }
                        break;
                    case CLIENTE:
                        if (clientController != null) {
                            clientController.activarVistaCliente();
                        }
                        break;
                }
                actualizarUiModo(modoAlQueSeEntra, actionMap);
                if (toolbarManager != null)
                    toolbarManager.reconstruirContenedorDeToolbars(modoAlQueSeEntra);
                if (modoAlQueSeEntra == WorkMode.CARROUSEL
                        && visorController.getActionFactory().getCarouselManager() != null) {
                    visorController.getActionFactory().getCarouselManager().findAndWireUpFastMoveButtons();
                    visorController.getActionFactory().getCarouselManager().findAndWireUpSpeedButtons();
                    visorController.getActionFactory().getCarouselManager().wireUpEventListeners();
                }
                sincronizarBotonesModo(actionMap);
                logger.debug("    -> [EDT-2] Restauración de UI para " + modoAlQueSeEntra + " completada.");
            });
        });
    } // --- Fin del método entrarModo ---
    
    public void toggleModoCargaSubcarpetas() {
        // Si el servicio es el que orquesta, el flag de bloqueo debe vivir aquí dentro.
        // Esto evita que el GeneralController tenga variables de estado "sucias".
        if (isChangingSubfolderMode) return; 
        
        isChangingSubfolderMode = true;
        try {
            boolean nuevoEstado = !model.isMostrarSoloCarpetaActual();
            model.setMostrarSoloCarpetaActual(nuevoEstado);
            // ... (resto de lógica de carga) ...
        } finally {
            isChangingSubfolderMode = false;
        }
    } // --- Fin del método toggleModoCargaSubcarpetas ---
    
    /**
     * Método helper que gestiona el flujo cuando se intenta entrar en modo proyecto
     * sin un proyecto cargado. Muestra un diálogo para abrir un archivo.
     * 
     * @return {@code true} si un proyecto fue seleccionado y cargado exitosamente,
     *         {@code false} si el usuario canceló la operación.
     */
    public boolean manejarAperturaDeProyectoVacio() {
        logger.debug("[GeneralController] Manejando apertura de proyecto vacío...");

        // Obtenemos una referencia a la ventana principal para centrar el diálogo
        Component parent = (visorController != null && visorController.getView() != null) ? visorController.getView()
                : null;

        // Mostramos el JFileChooser
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        fileChooser.setDialogTitle("Abrir Proyecto");
        fileChooser.setCurrentDirectory(projectController.getProjectManager().getCarpetaBaseProyectos().toFile());
        javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter(
                "Archivos de Proyecto (*.prj)", "prj");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(parent);

        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
            // El usuario seleccionó un archivo.
            Path selectedFile = fileChooser.getSelectedFile().toPath();
            logger.debug(" -> Usuario seleccionó el archivo: {}", selectedFile);

            // Delegamos la carga de datos al ProjectController.
            projectController.solicitarAbrirProyecto(selectedFile);

            // Verificamos si la carga fue exitosa (ahora hay imágenes en el proyecto)
            IProjectManager pm = visorController.getProjectManager();
            if (!pm.getImagenesMarcadas().isEmpty() || !pm.getImagenesDescartadas().isEmpty()) {

                logger.debug(" -> Proyecto cargado exitosamente. Se procederá con el cambio de modo.");
                return true; // Éxito
            } else {
                logger.warn(" -> El proyecto seleccionado ({}) está vacío o no se pudo cargar.",
                        selectedFile.getFileName());
                // Opcional: Mostrar un mensaje al usuario
                javax.swing.JOptionPane.showMessageDialog(parent, "El proyecto seleccionado está vacío o no es válido.",
                        "Proyecto Vacío", javax.swing.JOptionPane.WARNING_MESSAGE);
                return false; // Fracaso
            }
        } else {
            // El usuario canceló el diálogo.
            logger.debug(" -> El usuario canceló la apertura del proyecto.");
            return false; // Cancelado
        }
    } // ---FIN de metodo manejarAperturaDeProyectoVacio---


} // --- FIN de clase AppModeService ---