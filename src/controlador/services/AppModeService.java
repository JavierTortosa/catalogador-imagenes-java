package controlador.services;

import java.awt.BorderLayout;
import java.awt.Component;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ClientController;
import controlador.DataController;
import controlador.ProjectController;
import controlador.RenderController;
import controlador.VisorController;
import controlador.commands.AppActionCommands;
import controlador.managers.CarouselManager;
import controlador.managers.ConfigApplicationManager;
import controlador.managers.DisplayModeManager;
import controlador.managers.InfobarImageManager;
import controlador.managers.InfobarStatusManager;
import controlador.managers.ToolbarManager;
import controlador.managers.ViewManager;
import controlador.managers.interfaces.IProjectManager;
import controlador.utils.ComponentRegistry;
import modelo.ListContext;
import modelo.VisorModel;
import modelo.VisorModel.DisplayMode;
import modelo.VisorModel.WorkMode;
import servicios.ConfigurationManager;

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
    private InfobarImageManager infobarImageManager;

    private VisorController visorController;
    private ProjectController projectController;
    private DataController dataController;
    private ClientController clientController;
    private RenderController renderController;
    private ConfigurationManager configuration;
    private ComponentRegistry registry;
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
    } // --- FIN de metodo AppModeService (constructor) ---


    public void setVisorController(VisorController visorController) {
        this.visorController = visorController;
    } // --- FIN de metodo setVisorController ---


    public void setProjectController(ProjectController projectController) {
        this.projectController = projectController;
    } // --- FIN de metodo setProjectController ---


    public void setDataController(DataController dataController) {
        this.dataController = dataController;
    } // --- FIN de metodo setDataController ---

    public void setClientController(ClientController clientController) {
        this.clientController = clientController;
    } // --- FIN de metodo setClientController ---


    public void setRenderController(RenderController renderController) {
        this.renderController = renderController;
    } // --- FIN de metodo setRenderController ---


    public void setInfobarImageManager(InfobarImageManager infobarImageManager) {
        this.infobarImageManager = infobarImageManager;
    } // --- FIN de metodo setInfobarImageManager ---


    public void setConfiguration(ConfigurationManager configuration) {
        this.configuration = configuration;
    } // --- FIN de metodo setConfiguration ---


    public void setRegistry(ComponentRegistry registry) {
        this.registry = registry;
    } // --- FIN de metodo setRegistry ---


    public void setActionMap(Map<String, Action> actionMap) {
        this.actionMap = actionMap;
    } // --- FIN de metodo setActionMap ---


    public void setTaggingManager(controlador.managers.TaggingManager taggingManager) {
        this.taggingManager = taggingManager;
    } // --- FIN de metodo setTaggingManager ---


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
            case RENDER -> "VISTA_RENDER";
            case EDITOR -> "VISTA_RENDER";
        };
        viewManager.cambiarAVista("container.workmodes", vistaName);
        logger.debug("[AppModeService] Vista cambiada a: {}", vistaName);
    } // --- FIN de metodo cambiarVistaPrincipal ---


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
    } // --- FIN de metodo actualizarUiParaModo ---

    /**
     * Lógica de sincronización de subcarpetas.
     */
    public void gestionarCargaSubcarpetas(boolean incluir, Runnable postCarga) {
        model.setMostrarSoloCarpetaActual(!incluir);
        if (postCarga != null) postCarga.run();
    } // --- FIN de metodo gestionarCargaSubcarpetas ---


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
    } // --- FIN de metodo actualizarTituloPrincipal ---


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
            case RENDER        -> AppActionCommands.CMD_MODO_RENDER;
            case EDITOR        -> AppActionCommands.CMD_MODO_EDITOR;
        };

        List<String> comandosDeModo = List.of(
                AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR,
                AppActionCommands.CMD_PROYECTO_GESTIONAR,
                AppActionCommands.CMD_MODO_DATOS,
                AppActionCommands.CMD_MODO_CLIENTE,
                AppActionCommands.CMD_VISTA_CAROUSEL,
                AppActionCommands.CMD_MODO_RENDER,
                AppActionCommands.CMD_MODO_EDITOR);

        for (String comando : comandosDeModo) {
            Action action = actionMap.get(comando);
            if (action != null) {
                boolean isSelected = comando.equals(comandoModoActivo);
                action.putValue(Action.SELECTED_KEY, isSelected);
                configAppManager.actualizarAspectoBotonToggle(action, isSelected);
            }
        }
        logger.debug("Botones de modo sincronizados. Activo: {}", comandoModoActivo);
    } // --- FIN de metodo sincronizarBotonesModo ---


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
    } // --- FIN de metodo cambiarModoDeTrabajo ---


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
            // Re-marcar como guardado para que lastSavedProjectState refleje las nuevas
            // instancias de ProjectImage creadas por sincronizarModeloConUI.
            if (projectController.getProjectManager() != null) {
                projectController.getProjectManager().markProjectAsSaved();
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

        // --- LÓGICA AL SALIR DEL MODO CLIENTE ---
        if (modoQueSeAbandona == WorkMode.CLIENTE && clientController != null) {
            clientController.guardarEstado();
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

        // --- LÓGICA AL SALIR DEL MODO EDITOR ---
        if (modoQueSeAbandona == WorkMode.EDITOR && renderController != null) {
            renderController.desactivarModoEditor();

            // Si hay un documento sin nombre con cambios sin guardar, se persiste
            // en el archivo temporal (editor_temporal.edoc) para no perderlo.
            servicios.editor.EditorDocumentManager edm = renderController.getEditorDocumentManager();
            if (edm != null && edm.hayCambiosSinGuardar() && edm.getArchivoActivo() == null) {
                logger.info("Saliendo del Modo Editor con documento temporal sin guardar. Guardando en archivo temporal...");
                edm.guardarAArchivo();
            }
            logger.debug("    -> Modo Editor: Fullscreen del editor desactivado.");
        }


    } // --- FIN de metodo salirModo ---

    public void entrarModo(WorkMode modoAlQueSeEntra) {
    	logger.debug("  [AppModeService] Entrando en modo: " + modoAlQueSeEntra);

        // --- REPARENTING: reposicionar el contenedor de visualización compartido ---
        reparentSharedDisplayContainer(modoAlQueSeEntra);

        if (displayModeManager != null) {
            ListContext contextoDestino = model.getCurrentListContext();
            
            // El modo DATOS requiere trabajar en vista GRID.
            if (modoAlQueSeEntra == WorkMode.DATOS) {
                contextoDestino.setDisplayMode(DisplayMode.GRID);
            }
            
            // El modo CLIENTE siempre debe arrancar en SINGLE_IMAGE para que la toolbar de zoom sea visible
            if (modoAlQueSeEntra == WorkMode.CLIENTE) {
                contextoDestino.setDisplayMode(DisplayMode.SINGLE_IMAGE);
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
                case RENDER:
                    viewManager.cambiarAVista("container.workmodes", "VISTA_RENDER");
                    if (infobarImageManager != null) {
                        infobarImageManager.limpiar();
                    }
                    break;
                case EDITOR:
                    viewManager.cambiarAVista("container.workmodes", "VISTA_RENDER");
                    if (renderController != null) {
                        renderController.activarModoEditor();
                    }
                    if (infobarImageManager != null) {
                        infobarImageManager.limpiar();
                    }
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
                        break;
                    case CARROUSEL:
                        ListContext contextoCarrusel = model.getCarouselListContext();
                        if (model.isSyncVisualizadorCarrusel())
                            contextoCarrusel.clonarDesde(model.getVisualizadorListContext());
                        else if (contextoCarrusel.getModeloLista() == null
                                || contextoCarrusel.getModeloLista().isEmpty())
                            contextoCarrusel.clonarDesde(model.getVisualizadorListContext());
                        visorController.restaurarUiCarrusel();
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
                        if (projectController != null) {
                            projectController.actualizarEstadoExportacionUI();
                        }
                        // Re-marcar como guardado para limpiar cualquier suciedad
                        // introducida por la activación de la vista cliente
                        // (ej. cambios en masterImages, creación de nuevas instancias).
                        if (projectController != null && projectController.getProjectManager() != null) {
                            projectController.getProjectManager().markProjectAsSaved();
                        }
                        break;
                    case RENDER:
                        break;
                    case EDITOR:
                        if (renderController != null) {
                            renderController.restaurarUiModoEditor();
                        }
                        break;
                }
                actualizarUiModo(modoAlQueSeEntra, actionMap);
                if (toolbarManager != null)
                    toolbarManager.reconstruirContenedorDeToolbars(modoAlQueSeEntra);
                if (modoAlQueSeEntra == WorkMode.RENDER && renderController != null) {
                    renderController.restaurarVistaRender();
                }
                if (modoAlQueSeEntra == WorkMode.CARROUSEL
                        && visorController.getActionFactory().getCarouselManager() != null) {
                    visorController.getActionFactory().getCarouselManager().onCarouselModeChanged(true);
                    visorController.getActionFactory().getCarouselManager().findAndWireUpFastMoveButtons();
                    visorController.getActionFactory().getCarouselManager().findAndWireUpSpeedButtons();
                    visorController.getActionFactory().getCarouselManager().wireUpEventListeners();
                }
                sincronizarBotonesModo(actionMap);
                logger.debug("    -> [EDT-2] Restauración de UI para " + modoAlQueSeEntra + " completada.");
            });
        });
    } // --- FIN de metodo entrarModo ---

    /**
     * Reparenta el contenedor de visualización compartido (container.displaymodes)
     * al placeholder del modo de trabajo activo. Esto permite que los paneles
     * ImageDisplayPanel, GridDisplayPanel y PolaroidDisplayPanel sean instancias
     * únicas reutilizadas en todos los WorkModes.
     */
    private void reparentSharedDisplayContainer(WorkMode modoActivo) {
        if (modoActivo == WorkMode.DATOS || modoActivo == WorkMode.CARROUSEL) return;

        JPanel sharedContainer = registry.get("container.displaymodes");
        if (sharedContainer == null) {
            logger.warn("  -> [Reparenting] No se encontró 'container.displaymodes' compartido.");
            return;
        }

        String placeholderKey = switch (modoActivo) {
            case VISUALIZADOR -> "placeholder.display.visualizador";
            case PROYECTO -> "placeholder.display.proyecto";
            case CLIENTE -> "placeholder.display.cliente";
            default -> null;
        };

        if (placeholderKey == null) return;

        JPanel placeholder = registry.get(placeholderKey);
        if (placeholder == null) {
            logger.warn("  -> [Reparenting] No se encontró placeholder '{}'.", placeholderKey);
            return;
        }

        if (sharedContainer.getParent() == placeholder) return;

        logger.debug("  -> [Reparenting] Moviendo contenedor compartido a '{}'", placeholderKey);
        placeholder.add(sharedContainer, BorderLayout.CENTER);
        placeholder.revalidate();
        placeholder.repaint();
    } // --- FIN de metodo reparentSharedDisplayContainer ---


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
    } // --- FIN de metodo toggleModoCargaSubcarpetas ---
    
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
    } // --- FIN de metodo manejarAperturaDeProyectoVacio---


} // --- FIN de clase AppModeService ---