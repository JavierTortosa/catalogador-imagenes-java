package controlador.factory;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.GeneralController;
import controlador.ProjectController;
import controlador.actions.archivo.DeleteAction;
// --- SECCIÓN 0: IMPORTS DE CLASES ACTION ESPECCÍFICAS ---
import controlador.actions.archivo.OpenFileAction;
import controlador.actions.archivo.RefreshAction;
import controlador.actions.ayuda.ShowHelpAction;
import controlador.actions.carousel.ChangeCarouselSpeedAction;
import controlador.actions.carousel.PauseCarouselAction;
import controlador.actions.carousel.PlayCarouselAction;
import controlador.actions.carousel.StopCarouselAction;
import controlador.actions.carousel.ToggleCarouselShuffleAction;
import controlador.actions.config.SetInfoBarTextFormatAction;
import controlador.actions.config.SetSubfolderReadModeAction;
import controlador.actions.config.ToggleUIElementVisibilityAction;
import controlador.actions.displaymode.SwitchDisplayModeAction;
import controlador.actions.edicion.CropAction;
import controlador.actions.edicion.FlipHorizontalAction;
import controlador.actions.edicion.FlipVerticalAction;
import controlador.actions.edicion.RotateLeftAction;
import controlador.actions.edicion.RotateRightAction;
import controlador.actions.especiales.HiddenButtonsAction;
import controlador.actions.especiales.MenuAction;
import controlador.actions.especiales.PlaceholderToggleAction;
import controlador.actions.filtro.AddFilterAction;
import controlador.actions.filtro.ClearAllFiltersAction;
import controlador.actions.filtro.PersistLiveFilterAction;
import controlador.actions.filtro.RemoveFilterAction;
import controlador.actions.filtro.SetFilterTypeAction;
import controlador.actions.navegacion.EntrarEnSubcarpetaAction;
import controlador.actions.navegacion.FirstImageAction;
import controlador.actions.navegacion.LastImageAction;
import controlador.actions.navegacion.NextImageAction;
import controlador.actions.navegacion.PreviousImageAction;
import controlador.actions.navegacion.SalirDeSubcarpetaAction;
import controlador.actions.navegacion.VolverACarpetaRaizAction;
import controlador.actions.orden.CycleSortAction;
// Importaciones para PanAction y Direction
import controlador.actions.pan.PanAction;
import controlador.actions.projects.AbrirProyectoAction;
import controlador.actions.projects.AddAssociatedFileAction;
import controlador.actions.projects.DeleteAssociatedFileAction;
import controlador.actions.projects.LocateAssociatedFileAction;
import controlador.actions.projects.EliminarProyectoAction;
import controlador.actions.projects.GestionarProyectoAction;
import controlador.actions.projects.GuardarProyectoAction;
import controlador.actions.projects.GuardarProyectoComoAction;
import controlador.actions.projects.MoveToDiscardsAction;
import controlador.actions.projects.NuevoProyectoAction;
import controlador.actions.projects.RelocateImageAction;
import controlador.actions.projects.RemoveFromExportQueueAction;
import controlador.actions.projects.RestoreFromDiscardsAction;
import controlador.actions.projects.ToggleExportDetailsAction;
import controlador.actions.projects.ToggleExportViewAction;
import controlador.actions.projects.ToggleGridStateAction;
import controlador.actions.projects.ToggleIgnoreCompressedAction;
import controlador.actions.projects.ToggleMarkImageAction;
import controlador.actions.tema.ToggleThemeAction;
import controlador.actions.toggle.ToggleLiveFilterAction;
import controlador.actions.toggle.ToggleNavegacionCircularAction;
import controlador.actions.toggle.ToggleProporcionesAction;
import controlador.actions.toggle.ToggleSubfoldersAction;
import controlador.actions.toggle.ToggleSyncAction;
import controlador.actions.tree.DrillDownFolderAction;
import controlador.actions.tree.OpenFolderAction;
import controlador.actions.vista.MostrarDialogoListaAction;
import controlador.actions.vista.SwitchToVisualizadorAction;
import controlador.actions.vista.ToggleAlwaysOnTopAction;
import controlador.actions.vista.ToggleCheckeredBackgroundAction;
import controlador.actions.vista.ToggleFullScreenAction;
import controlador.actions.vista.ToggleMiniatureTextAction;
import controlador.actions.workmode.SwitchWorkModeAction;
import controlador.actions.zoom.AplicarModoZoomAction;
import controlador.actions.zoom.ResetZoomAction;
import controlador.actions.zoom.ToggleZoomManualAction;
import controlador.actions.zoom.ToggleZoomToCursorAction;
import controlador.commands.AppActionCommands;
import controlador.imagen.LocateFileAction;
import controlador.interfaces.ContextSensitiveAction;
import controlador.managers.CarouselManager;
import controlador.managers.DisplayModeManager;
import controlador.managers.FileOperationsManager;
import controlador.managers.filter.FilterCriterion.FilterSource;
import controlador.managers.filter.FilterCriterion.FilterType;
import controlador.managers.interfaces.ICarouselListCoordinator;
import controlador.managers.interfaces.IEditionManager;
import controlador.managers.interfaces.IListCoordinator;
import controlador.managers.interfaces.IProjectManager;
import controlador.managers.interfaces.IViewManager;
import controlador.managers.interfaces.IZoomManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import modelo.VisorModel.DisplayMode;
import modelo.VisorModel.WorkMode;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.zoom.ZoomModeEnum;
import vista.VisorView;
import vista.components.Direction;
import vista.config.MenuItemDefinition;
import vista.config.UIDefinitionService;
import vista.theme.ThemeManager;
import vista.util.IconUtils;


public class ActionFactory {
	
	public record IconInfo(String iconKey, vista.config.IconScope scope) {}
	private static final Logger logger = LoggerFactory.getLogger(ActionFactory.class);
	
    // --- SECCIÓN 1: CAMPOS DE INSTANCIA (DEPENDENCIAS INYECTADAS) ---
    // 1.1. Referencias a componentes principales
    private final VisorModel model;
    private VisorView view;
    private final ConfigurationManager configuration;
    private final IconUtils iconUtils;
    
    
    // 1.2. Referencias a Managers y Coordinadores
    private FileOperationsManager fileOperationsManager;
    private IZoomManager zoomManager;
    private IViewManager viewManager;

    private final GeneralController generalController;
    private final ProjectController projectControllerRef;
    private final ThemeManager themeManager;
    
    private CarouselManager carouselManager;
    private ICarouselListCoordinator carouselListCoordinator;
    private List<ContextSensitiveAction> carouselContextSensitiveActions;
    
    
    // private final EditionManager editionManager; // Descomentar cuando se implemente y se inyecte
    private final IProjectManager projectService; // Servicio de persistencia de proyectos
    private IListCoordinator listCoordinator;
    private IEditionManager editionManager;

    // 1.3. Mapa para obtener claves de icono
    private final Map<String, IconInfo> comandoToIconKeyMap;
    
    // 1.4. Dimensiones de iconos (leídas de config)
    private final int iconoAncho;
    private final int iconoAlto;

    // 1.5. Mapa para almacenar las Actions creadas.
    private final Map<String, Action> actionMap;

    // 1.6. Referencia a la Action genérica para funcionalidades pendientes.
    private Action funcionalidadPendienteAction;

    // 1.7. CAMPO para registrar acciones sensibles al contexto
    private final List<ContextSensitiveAction> contextSensitiveActions;

    // Constante para la cantidad de paneo incremental (se puede mover a una clase de constantes globales si existe)
    private static final int INCREMENTAL_PAN_AMOUNT = 50; 
    
    // 1.8. Campo para registrar los DisplayModes
    private DisplayModeManager displayModeManager;
    
    // 1.9. Campo para el registro de componentes
    private ComponentRegistry registry;
    
    // --- SECCIÓN 2: CONSTRUCTOR ---
    /**
     * Constructor de ActionFactory.
     * Recibe todas las dependencias necesarias para crear las acciones.
     *
     * @param model El modelo principal de la aplicación.
     * @param view La vista principal (JFrame). Puede ser null inicialmente.
     * @param zoomManager El gestor de zoom.
     * @param fileOperationsManager El gestor de operaciones de archivo.
     * @param editionManager El gestor de edición.
     * @param listCoordinator El coordinador de listas.
     * @param iconUtils La utilidad para cargar iconos.
     * @param configuration El gestor de configuración.
     * @param projectManager El gestor de proyectos.
     * @param iconMap Un mapa para registrar las claves de los iconos.
     * @param viewManager El gestor de la vista.
     * @param themeManager El gestor de temas.
     * @param generalController El controlador general de la aplicación.
     * @param projectController El controlador específico del modo proyecto.
     */
    public ActionFactory(
    		VisorModel model, 
            VisorView view,
            IZoomManager zoomManager, 
            FileOperationsManager fileOperationsManager,
            IEditionManager editionManager,
            IListCoordinator listCoordinator,
            IconUtils iconUtils, 
            ConfigurationManager configuration,
            IProjectManager projectService,
            Map<String, IconInfo> comandoToIconInfoMap,
            IViewManager viewManager,
            ThemeManager themeManager,
            
            ComponentRegistry registry,
            GeneralController generalController,
            
            ProjectController projectController
    ){ 
        
        // 2.1. Asignar dependencias principales.
        this.model 						= Objects.requireNonNull(model, "VisorModel no puede ser null en ActionFactory");
        this.view = view; // Se permite que la vista sea null en el constructor.
        this.configuration 				= Objects.requireNonNull(configuration, "ConfigurationManager no puede ser null en ActionFactory");
        this.iconUtils 					= Objects.requireNonNull(iconUtils, "IconUtils no puede ser null en ActionFactory");
        
        this.generalController			= Objects.requireNonNull(generalController, "GeneralController no puede ser null en ActionFactory");
        this.registry 					= Objects.requireNonNull(registry, "Registry no puede ser null en ActionFactory");
        this.themeManager 				= Objects.requireNonNull(themeManager, "ThemeManager no puede ser null en ActionFactory");
        
        this.projectControllerRef       = Objects.requireNonNull(projectController);
        
        // 2.2. Asignar Managers y Coordinadores.
        this.zoomManager = zoomManager;
        this.fileOperationsManager = fileOperationsManager;
        this.viewManager = viewManager;
        this.editionManager	= editionManager;
        this.contextSensitiveActions 	= new ArrayList<>(); 
        
        this.listCoordinator 			= Objects.requireNonNull(listCoordinator, "IListCoordinator no puede ser null en ActionFactory");
        this.projectService 			= Objects.requireNonNull(projectService, "IProjectManager (servicio) no puede ser null");
        
        // 2.3. Asignar mapa de iconos.
        this.comandoToIconKeyMap 		= Objects.requireNonNull(comandoToIconInfoMap, "comandoToIconInfoMap no puede ser null");
        
        // 2.4. Leer dimensiones de iconos desde la configuración.
        this.iconoAncho 				= configuration.getInt("iconos.ancho", 24);
        this.iconoAlto 					= configuration.getInt("iconos.alto", 24);

        // 2.5. Inicializar el mapa interno de acciones.
        this.actionMap 					= new HashMap<>();

    } // --- Fin del método ActionFactory (constructor) ---

    /**
     * Inicializa todas las acciones que NO dependen de la existencia de la 'view'.
     * Este método debe ser llamado antes de que se necesiten las acciones para construir
     * componentes como las barras de herramientas.
     */
    public void initializeCoreActions() {
		logger.info("  [ActionFactory] Inicializando Actions principales (no dependientes de la vista)...");

		if (zoomManager == null || fileOperationsManager == null || editionManager == null
				|| listCoordinator == null || viewManager == null)
		{
			throw new IllegalStateException(
					"No se pueden inicializar las acciones principales porque las dependencias (zoomManager, etc.) no han sido inyectadas.");
		}
		
		createCoreActions();
		
		logger.debug(
				"  [ActionFactory] Actions principales inicializadas y mapeadas. Total: " + this.actionMap.size());
	} // --- Fin del método initializeCoreActions ---

    /**
     * Inicializa todas las acciones que SÍ dependen de la existencia de la 'view'.
     * Este método DEBE ser llamado DESPUÉS de crear la 'view' y de inyectarla en esta fábrica.
     */
    public void initializeViewDependentActions() {
        logger.info("  [ActionFactory] Inicializando Actions dependientes de la vista...");
        if (view == null) {
            throw new IllegalStateException("No se pueden inicializar las acciones dependientes de la vista porque la 'view' es null.");
        }

        createViewDependentActions();

        logger.debug(
				"  [ActionFactory] Actions dependientes de la vista inicializadas. Total de actions: " + this.actionMap.size());
    } // --- Fin del método initializeViewDependentActions ---
    

	/**
     * Crea las acciones que NO dependen de la 'view'.
     */
    private void createCoreActions() {
    	
        // 3.1. Crear la Action genérica para funcionalidades pendientes primero.
        this.funcionalidadPendienteAction = createFuncionalidadPendienteAction();
        actionMap.put(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, this.funcionalidadPendienteAction);
        
        
        actionMap.put(AppActionCommands.CMD_AYUDA_VER_ATAJOS, createVerAtajosAction());
//        actionMap.put(AppActionCommands.CMD_AYUDA_MOSTRAR_GUIA, createShowHelpAction());
        
        // 3.2. Crear y registrar Actions de Zoom
        Action resetZoomAct = createResetZoomAction();
        actionMap.put(AppActionCommands.CMD_ZOOM_RESET, resetZoomAct);
        actionMap.put(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE, createToggleZoomManualAction());
        
        actionMap.put(AppActionCommands.CMD_ZOOM_TIPO_AUTO, 
                createAplicarModoZoomAction(AppActionCommands.CMD_ZOOM_TIPO_AUTO, "Zoom Automático", ZoomModeEnum.DISPLAY_ORIGINAL));
        actionMap.put(AppActionCommands.CMD_ZOOM_TIPO_ANCHO, 
                createAplicarModoZoomAction(AppActionCommands.CMD_ZOOM_TIPO_ANCHO, "Ajustar a Ancho", ZoomModeEnum.FIT_TO_WIDTH));
        actionMap.put(AppActionCommands.CMD_ZOOM_TIPO_ALTO, 
                createAplicarModoZoomAction(AppActionCommands.CMD_ZOOM_TIPO_ALTO, "Ajustar a Alto", ZoomModeEnum.FIT_TO_HEIGHT));
        actionMap.put(AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR, 
                createAplicarModoZoomAction(AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR, "Ajustar a Pantalla", ZoomModeEnum.FIT_TO_SCREEN));
        actionMap.put(AppActionCommands.CMD_ZOOM_TIPO_FIJO, 
                createAplicarModoZoomAction(AppActionCommands.CMD_ZOOM_TIPO_FIJO, "Zoom Fijo", ZoomModeEnum.MAINTAIN_CURRENT_ZOOM));
        actionMap.put(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO, 
                createAplicarModoZoomAction(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO, "Zoom Especificado", ZoomModeEnum.USER_SPECIFIED_PERCENTAGE));
        actionMap.put(AppActionCommands.CMD_ZOOM_TIPO_RELLENAR,
                createAplicarModoZoomAction(AppActionCommands.CMD_ZOOM_TIPO_RELLENAR, "Escalar para Rellenar", ZoomModeEnum.FILL));
        actionMap.put(AppActionCommands.CMD_ZOOM_TOGGLE_TO_CURSOR, createToggleZoomToCursorAction());
        
        // 3.3. Crear y registrar Actions de Navegación
        actionMap.put(AppActionCommands.CMD_NAV_PRIMERA, createFirstImageAction());
        actionMap.put(AppActionCommands.CMD_NAV_ANTERIOR, createPreviousImageAction());
        actionMap.put(AppActionCommands.CMD_NAV_SIGUIENTE, createNextImageAction());
        actionMap.put(AppActionCommands.CMD_NAV_ULTIMA, createLastImageAction());
        actionMap.put(AppActionCommands.CMD_TOGGLE_WRAP_AROUND, createToggleNavegacionCircularAction());
        
        
        // 3.4. Crear y registrar Actions de Edición
        actionMap.put(AppActionCommands.CMD_IMAGEN_ROTAR_IZQ, createRotateLeftAction());
        actionMap.put(AppActionCommands.CMD_IMAGEN_ROTAR_DER, createRotateRightAction());
        actionMap.put(AppActionCommands.CMD_IMAGEN_VOLTEAR_H, createFlipHorizontalAction());
        actionMap.put(AppActionCommands.CMD_IMAGEN_VOLTEAR_V, createFlipVerticalAction());
        actionMap.put(AppActionCommands.CMD_IMAGEN_RECORTAR, createCropAction());
        actionMap.put(AppActionCommands.CMD_IMAGEN_LOCALIZAR, createLocateFileAction());
        
        // 3.5. Crear y registrar Actions de Archivo y relacionadas
        actionMap.put(AppActionCommands.CMD_ARCHIVO_ABRIR, createOpenFileAction());
        actionMap.put(AppActionCommands.CMD_IMAGEN_ELIMINAR, createDeleteAction());
        actionMap.put(AppActionCommands.CMD_ESPECIAL_REFRESCAR, createRefreshAction());

        // --- Actions de Navegación de Carpetas (para la nueva toolbar) ---
        actionMap.put(AppActionCommands.CMD_ORDEN_CARPETA_ANTERIOR, createNavegarCarpetaAnteriorAction());
        actionMap.put(AppActionCommands.CMD_ORDEN_CARPETA_SIGUIENTE, createNavegarCarpetaSiguienteAction());
        actionMap.put(AppActionCommands.CMD_ORDEN_CARPETA_RAIZ, createNavegarCarpetaRaizAction());
        actionMap.put(AppActionCommands.CMD_ORDEN_CICLO, createCycleSortAction());
        
        // --- Actions de Filtros
        actionMap.put(AppActionCommands.CMD_FILTRO_TOGGLE_LIVE_FILTER, createToggleLiveFilterAction());
        actionMap.put(AppActionCommands.CMD_FILTRO_ADD_POSITIVE, createAddFilterAction(FilterType.CONTAINS));
        actionMap.put(AppActionCommands.CMD_FILTRO_ADD_NEGATIVE, createAddFilterAction(FilterType.DOES_NOT_CONTAIN));
        actionMap.put(AppActionCommands.CMD_FILTRO_REMOVE_SELECTED, createRemoveFilterAction());
        actionMap.put(AppActionCommands.CMD_FILTRO_CLEAR_ALL, createClearAllFiltersAction());
        actionMap.put(AppActionCommands.CMD_FILTRO_ACTIVO, createPersistLiveFilterAction());
        
        actionMap.put(AppActionCommands.CMD_FILTRO_SET_TYPE_FILENAME, createSetFilterTypeAction(FilterSource.FILENAME));
        actionMap.put(AppActionCommands.CMD_FILTRO_SET_TYPE_FOLDER, createSetFilterTypeAction(FilterSource.FOLDER_PATH));
        
        // botones que faltan por implemetar la logica
        actionMap.put(AppActionCommands.CMD_FILTRO_UP, getFuncionalidadPendienteAction());
        actionMap.put(AppActionCommands.CMD_FILTRO_DOWN, getFuncionalidadPendienteAction());
        // Por ahora, el de TAG llamará a la acción de "funcionalidad pendiente"
        actionMap.put(AppActionCommands.CMD_FILTRO_SET_TYPE_TAG, new PlaceholderToggleAction("Filtrar por Etiqueta")); //getFuncionalidadPendienteAction());
        
        
        // 3.6. Crear y registrar Actions de Vista (Toggles de UI)
//        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_LOCATION_BAR, createToggleLocationBarAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_INFOBAR_INFERIOR, createToggleStatusBarAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_INFOBAR_SUPERIOR, createToggleInfoBarAction());
        
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_MENU_BAR, createToggleMenuBarAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_TOOL_BAR, createToggleToolBarAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_FILE_LIST, createToggleFileListAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_THUMBNAILS, createToggleThumbnailsAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_CHECKERED_BG, createToggleCheckeredBackgroundAction());
        actionMap.put(AppActionCommands.CMD_VISTA_MOSTRAR_DIALOGO_LISTA, createMostrarDialogoListaAction());
        actionMap.put(AppActionCommands.CMD_VISTA_PANTALLA_COMPLETA, createToggleFullScreenAction());
        
        // 3.7. Crear y registrar Actions de Tema
        logger.debug("  -> Creando acciones para temas dinámicamente...");

        // Le preguntamos a UIDefinitionService por la lista de temas
        List<vista.config.UIDefinitionService.TemaDefinicion> temasDefiniciones = UIDefinitionService.getTemasPrincipales();

        for (vista.config.UIDefinitionService.TemaDefinicion temaDef : temasDefiniciones) {
            // Generamos la clave de comando canónica para esta acción (ej: "cmd.tema.cyan_light")
            String commandKey = "cmd.tema." + temaDef.id();
            
            // Creamos la acción y la guardamos en el mapa.
            // Necesitaremos modificar `createToggleThemeAction` para que acepte el commandKey.
            actionMap.put(commandKey, createToggleThemeAction(temaDef.id(), temaDef.nombreDisplay(), commandKey));
            logger.debug("    -> Action creada para tema: " + temaDef.nombreDisplay() + " con comando: " + commandKey);
        }
        

        // 3.8. Crear y registrar Actions de Toggle Generales
        actionMap.put(AppActionCommands.CMD_TOGGLE_SUBCARPETAS, createToggleSubfoldersAction());
        actionMap.put(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA,createSetSubfolderReadModeAction(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA,"Mostrar Solo Carpeta Actual",false));
        actionMap.put(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS, createSetSubfolderReadModeAction(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS, "Mostrar Imágenes de Subcarpetas", true));
        actionMap.put(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES, createToggleProporcionesAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP, createToggleAlwaysOnTopAction());
        
        actionMap.put(AppActionCommands.CMD_TOGGLE_SYNC_VISOR_CARRUSEL, createToggleSyncAction());
        
        // 3.9. Crear y registrar Actions de Proyecto
        actionMap.put(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA, createToggleMarkImageAction());
        actionMap.put(AppActionCommands.CMD_PROYECTO_GESTIONAR, createGestionarProyectoAction());
        actionMap.put(AppActionCommands.CMD_PROYECTO_NUEVO, createNuevoProyectoAction());
        actionMap.put(AppActionCommands.CMD_PROYECTO_ABRIR, createAbrirProyectoAction());
        actionMap.put(AppActionCommands.CMD_PROYECTO_GUARDAR, createGuardarProyectoAction());
        actionMap.put(AppActionCommands.CMD_PROYECTO_GUARDAR_COMO, createGuardarProyectoComoAction());
        actionMap.put(AppActionCommands.CMD_PROYECTO_ELIMINAR, createEliminarProyectoAction());
        actionMap.put(AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR, createSwitchToVisualizadorAction());
        actionMap.put(AppActionCommands.CMD_PROYECTO_MOVER_A_DESCARTES, createMoveToDiscardsAction());
        actionMap.put(AppActionCommands.CMD_PROYECTO_RESTAURAR_DE_DESCARTES, createRestoreFromDiscardsAction());
        actionMap.put(AppActionCommands.CMD_EXPORT_ASIGNAR_ARCHIVO, createAssignFileAction());
        actionMap.put(AppActionCommands.CMD_EXPORT_ABRIR_UBICACION, createOpenLocationAction());
        actionMap.put(AppActionCommands.CMD_EXPORT_QUITAR_DE_COLA, createRemoveFromQueueAction());
        actionMap.put(AppActionCommands.CMD_EXPORT_IGNORAR_COMPRIMIDO, createToggleIgnoreCompressedAction());
        actionMap.put(AppActionCommands.CMD_PROYECTO_ELIMINAR_PERMANENTEMENTE, createEliminarDeProyectoAction());
        actionMap.put(AppActionCommands.CMD_EXPORT_RELOCALIZAR_IMAGEN, createRelocateImageAction());
        actionMap.put(AppActionCommands.CMD_PROYECTO_LOCALIZAR_ARCHIVO, createLocalizarArchivoProyectoAction());
        actionMap.put(AppActionCommands.CMD_PROYECTO_VACIAR_DESCARTES, createVaciarDescartesAction());
        actionMap.put(AppActionCommands.CMD_INICIAR_EXPORTACION, createIniciarExportacionAction());
        actionMap.put(AppActionCommands.CMD_EXPORT_SELECCIONAR_CARPETA, createSeleccionarCarpetaAction());
        actionMap.put(AppActionCommands.CMD_EXPORT_REFRESH, createRefreshExportQueueAction());
        
        actionMap.put(AppActionCommands.CMD_EXPORT_ASSIGN_PANNEL, createToggleExportViewAction());
        actionMap.put(AppActionCommands.CMD_EXPORT_DETALLES_SELECCION, createToggleExportDetailsAction());
        
        actionMap.put(AppActionCommands.CMD_EXPORT_ADD_ASSOCIATED_FILE, createAddAssociatedFileAction());
        actionMap.put(AppActionCommands.CMD_EXPORT_DEL_ASSOCIATED_FILE, createDeleteAssociatedFileAction());
        actionMap.put(AppActionCommands.CMD_EXPORT_LOCATE_ASSOCIATED_FILE, createLocateAssociatedFileAction());
        
        // --- CAMBIO ---: Actions especiales movidas de vuelta a la primera fase.
        // 3.10. Crear y registrar Actions Especiales
        actionMap.put(AppActionCommands.CMD_ESPECIAL_MENU, createMenuAction());
        actionMap.put(AppActionCommands.CMD_ESPECIAL_BOTONES_OCULTOS, createHiddenButtonsAction());
        
        // ACCIONES PARA CONFIGURAR VISIBILIDAD DE ELEMENTOS
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_VISIBLE, new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Barra de Información Superior", ConfigKeys.INFOBAR_SUP_VISIBLE, "REFRESH_INFO_BAR_SUPERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_VISIBLE));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_NOMBRE_RUTA, new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Nombre/Ruta Archivo (Sup.)", ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_VISIBLE,"REFRESH_INFO_BAR_SUPERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_NOMBRE_RUTA));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_INDICE_TOTAL, new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Índice/Total Imágenes", ConfigKeys.INFOBAR_SUP_INDICE_TOTAL_VISIBLE,"REFRESH_INFO_BAR_SUPERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_INDICE_TOTAL));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_DIMENSIONES, new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Dimensiones Originales", ConfigKeys.INFOBAR_SUP_DIMENSIONES_VISIBLE, "REFRESH_INFO_BAR_SUPERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_DIMENSIONES));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_TAMANO_ARCHIVO, new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Tamaño de Archivo", ConfigKeys.INFOBAR_SUP_TAMANO_ARCHIVO_VISIBLE, "REFRESH_INFO_BAR_SUPERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_TAMANO_ARCHIVO));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_FECHA_ARCHIVO, new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Fecha de Archivo", ConfigKeys.INFOBAR_SUP_FECHA_ARCHIVO_VISIBLE,"REFRESH_INFO_BAR_SUPERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_FECHA_ARCHIVO));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_FORMATO_IMAGEN, new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Formato de Imagen", ConfigKeys.INFOBAR_SUP_FORMATO_IMAGEN_VISIBLE,"REFRESH_INFO_BAR_SUPERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_FORMATO_IMAGEN));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_MODO_ZOOM, new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Modo de Zoom", ConfigKeys.INFOBAR_SUP_MODO_ZOOM_VISIBLE,"REFRESH_INFO_BAR_SUPERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_MODO_ZOOM));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_ZOOM_REAL_PCT, new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar % Zoom Real", ConfigKeys.INFOBAR_SUP_ZOOM_REAL_PCT_VISIBLE,"REFRESH_INFO_BAR_SUPERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_ZOOM_REAL_PCT));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_SOLO_NOMBRE, new SetInfoBarTextFormatAction(this.viewManager, this.configuration, "Solo Nombre de Archivo", ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_FORMATO, "solo_nombre", "REFRESH_INFO_BAR_SUPERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_SOLO_NOMBRE));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_RUTA_COMPLETA, new SetInfoBarTextFormatAction(this.viewManager, this.configuration, "Ruta Completa y Nombre", ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_FORMATO, "ruta_completa", "REFRESH_INFO_BAR_SUPERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_RUTA_COMPLETA));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_VISIBLE, new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Barra de Estado/Control Inferior", ConfigKeys.INFOBAR_INF_VISIBLE, "REFRESH_INFO_BAR_INFERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_VISIBLE));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_NOMBRE_RUTA, new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Nombre/Ruta Archivo (Inf.)", ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_VISIBLE,"REFRESH_INFO_BAR_INFERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_NOMBRE_RUTA));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_ZM, new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Icono Zoom Manual (Inf.)", ConfigKeys.INFOBAR_INF_ICONO_ZM_VISIBLE,"REFRESH_INFO_BAR_INFERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_ZM));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_PROP, new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Icono Proporciones (Inf.)", ConfigKeys.INFOBAR_INF_ICONO_PROP_VISIBLE,"REFRESH_INFO_BAR_INFERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_PROP));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_SUBC, new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Icono Subcarpetas (Inf.)", ConfigKeys.INFOBAR_INF_ICONO_SUBC_VISIBLE,"REFRESH_INFO_BAR_INFERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_SUBC));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_CTRL_ZOOM_PCT, new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Control % Zoom (Inf.)", ConfigKeys.INFOBAR_INF_CTRL_ZOOM_PCT_VISIBLE, "REFRESH_INFO_BAR_INFERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_CTRL_ZOOM_PCT));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_CTRL_MODO_ZOOM, new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Control Modo Zoom (Inf.)", ConfigKeys.INFOBAR_INF_CTRL_MODO_ZOOM_VISIBLE,"REFRESH_INFO_BAR_INFERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_CTRL_MODO_ZOOM));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_MENSAJES_APP, new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Área de Mensajes (Inf.)", ConfigKeys.INFOBAR_INF_MENSAJES_APP_VISIBLE,"REFRESH_INFO_BAR_INFERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_MENSAJES_APP));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_SOLO_NOMBRE, new SetInfoBarTextFormatAction(this.viewManager, this.configuration, "Solo Nombre de Archivo", ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_FORMATO, "solo_nombre", "REFRESH_INFO_BAR_INFERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_SOLO_NOMBRE));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_RUTA_COMPLETA, new SetInfoBarTextFormatAction(this.viewManager, this.configuration, "Ruta Completa y Nombre", ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_FORMATO, "ruta_completa", "REFRESH_INFO_BAR_INFERIOR", AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_RUTA_COMPLETA));
    
        // 3.11. Crear y registrar Actions de Paneo (para el D-Pad)
        actionMap.put(AppActionCommands.CMD_PAN_TOP_EDGE, createPanActionAbsolute(AppActionCommands.CMD_PAN_TOP_EDGE, Direction.UP));
        actionMap.put(AppActionCommands.CMD_PAN_BOTTOM_EDGE, createPanActionAbsolute(AppActionCommands.CMD_PAN_BOTTOM_EDGE, Direction.DOWN));
        actionMap.put(AppActionCommands.CMD_PAN_LEFT_EDGE, createPanActionAbsolute(AppActionCommands.CMD_PAN_LEFT_EDGE, Direction.LEFT));
        actionMap.put(AppActionCommands.CMD_PAN_RIGHT_EDGE, createPanActionAbsolute(AppActionCommands.CMD_PAN_RIGHT_EDGE, Direction.RIGHT));
        
        actionMap.put(AppActionCommands.CMD_PAN_UP_INCREMENTAL, createPanActionIncremental(AppActionCommands.CMD_PAN_UP_INCREMENTAL, Direction.UP));
        actionMap.put(AppActionCommands.CMD_PAN_DOWN_INCREMENTAL, createPanActionIncremental(AppActionCommands.CMD_PAN_DOWN_INCREMENTAL, Direction.DOWN));
        actionMap.put(AppActionCommands.CMD_PAN_LEFT_INCREMENTAL, createPanActionIncremental(AppActionCommands.CMD_PAN_LEFT_INCREMENTAL, Direction.LEFT));
        actionMap.put(AppActionCommands.CMD_PAN_RIGHT_INCREMENTAL, createPanActionIncremental(AppActionCommands.CMD_PAN_RIGHT_INCREMENTAL, Direction.RIGHT));
        
        // 3.12. Crear y registrar Actions de Control de Fondo
        actionMap.put(AppActionCommands.CMD_BACKGROUND_THEME_COLOR, createSetBackgroundColorAction(AppActionCommands.CMD_BACKGROUND_THEME_COLOR, "clear", "Fondo Tema Claro"));
        actionMap.put(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_1, createSetBackgroundColorAction(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_1, "dark", "Fondo Tema Oscuro"));
        actionMap.put(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_2, createSetBackgroundColorAction(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_2, "blue", "Fondo Tema Azul"));
        actionMap.put(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_3, createSetBackgroundColorAction(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_3, "orange", "Fondo Tema Naranja"));
        actionMap.put(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_4, createSetBackgroundColorAction(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_4, "green", "Fondo Tema Verde"));
        
        actionMap.put(AppActionCommands.CMD_BACKGROUND_CHECKERED, createSetCheckeredBackgroundAction("Fondo a Cuadros"));
        actionMap.put(AppActionCommands.CMD_BACKGROUND_CUSTOM_COLOR, createRequestCustomColorAction("Elegir Color..."));
        
        // 3.13. Crear y registrar Actions de Grid
        //--- Actions de Grid (para Proyecto) ---
        actionMap.put(AppActionCommands.CMD_GRID_SET_TEXT, createSetGridTextAction());
        actionMap.put(AppActionCommands.CMD_GRID_REMOVE_TEXT, createRemoveGridTextAction());
        actionMap.put(AppActionCommands.CMD_GRID_SIZE_UP_MINIATURA, createGridSizeUpAction());
        actionMap.put(AppActionCommands.CMD_GRID_SIZE_DOWN_MINIATURA, createGridSizeDownAction());
        actionMap.put(AppActionCommands.CMD_GRID_SHOW_STATE, createToggleGridStateAction());
        
    	// --- Acciones para cambio de DisplayMode (Modos de Visualización de Contenido) ---
        // Estas acciones delegan al GeneralController y son ContextSensitive (para su selección).
        registerAction(AppActionCommands.CMD_VISTA_SINGLE,createSwitchDisplayModeAction(DisplayMode.SINGLE_IMAGE, AppActionCommands.CMD_VISTA_SINGLE, "Vista Imagen Única"));
        registerAction(AppActionCommands.CMD_VISTA_GRID,createSwitchDisplayModeAction(DisplayMode.GRID, AppActionCommands.CMD_VISTA_GRID, "Vista Cuadrícula"));
        registerAction(AppActionCommands.CMD_VISTA_POLAROID,createSwitchDisplayModeAction(DisplayMode.POLAROID, AppActionCommands.CMD_VISTA_POLAROID, "Vista Polaroid"));

        // --- Acciones para cambio de WorkMode (Modos de Trabajo) ---
        // Asegúrate de que CMD_VISTA_CAROUSEL exista en AppActionCommands
        registerAction(AppActionCommands.CMD_VISTA_CAROUSEL, createSwitchWorkModeAction(WorkMode.CARROUSEL, AppActionCommands.CMD_VISTA_CAROUSEL, "Modo Carrusel"));
        registerAction(AppActionCommands.CMD_MODO_DATOS, createSwitchWorkModeAction(WorkMode.DATOS, AppActionCommands.CMD_MODO_DATOS, "Modo Datos"));
        registerAction(AppActionCommands.CMD_MODO_EDICION, createSwitchWorkModeAction(WorkMode.EDICION, AppActionCommands.CMD_MODO_EDICION, "Modo Edición"));

        // --- Acciones para el Árbol de Carpetas ---
        registerAction(AppActionCommands.CMD_TREE_OPEN_FOLDER, createOpenFolderAction());
        registerAction(AppActionCommands.CMD_TREE_DRILL_DOWN_FOLDER, createDrillDownFolderAction());
        
        
        // Acciones para otros WorkModes futuros, si los tienes definidos en UIDefinitionService.

        
    } // --- FIN del metodo createCoreActions ---
    

	/**
     * Crea las acciones que SÍ dependen de la 'view'.
     */
    private void createViewDependentActions() {
        // Crear la Action genérica para funcionalidades pendientes primero, ya que necesita la 'view'.

        // Action que depende explícitamente de la 'view'.
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_MINIATURE_TEXT, createToggleMiniatureTextAction());

        registerAction(AppActionCommands.CMD_AYUDA_MOSTRAR_GUIA, createShowHelpAction());
        
        registerAction(AppActionCommands.CMD_CAROUSEL_PLAY, new PlayCarouselAction("Iniciar Carrusel", this.carouselManager));
        registerAction(AppActionCommands.CMD_CAROUSEL_PAUSE, new PauseCarouselAction("Pausar Carrusel", this.carouselManager));
        registerAction(AppActionCommands.CMD_CAROUSEL_STOP, new StopCarouselAction("Detener Carrusel", this.carouselManager));
        actionMap.put(AppActionCommands.CMD_CAROUSEL_TOGGLE_SHUFFLE,new ToggleCarouselShuffleAction(model));
        
        registerAction(AppActionCommands.CMD_CAROUSEL_REWIND, new AbstractAction("Retroceso Rápido") {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                // No hace nada a propósito. El MouseListener se encarga de la acción.
            }
        });
        registerAction(AppActionCommands.CMD_CAROUSEL_FAST_FORWARD, new AbstractAction("Avance Rápido") {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                // No hace nada a propósito. El MouseListener se encarga de la acción.
            }
        });
        
     // --- Acciones de Control de Velocidad del Carrusel ---
        actionMap.put(
            AppActionCommands.CMD_CAROUSEL_SPEED_INCREASE,
            new ChangeCarouselSpeedAction(model, carouselManager, configuration, ChangeCarouselSpeedAction.SpeedChangeType.INCREASE)
        );
        actionMap.put(
            AppActionCommands.CMD_CAROUSEL_SPEED_DECREASE,
            new ChangeCarouselSpeedAction(model, carouselManager, configuration, ChangeCarouselSpeedAction.SpeedChangeType.DECREASE)
        );
        actionMap.put(
            AppActionCommands.CMD_CAROUSEL_SPEED_RESET,
            new ChangeCarouselSpeedAction(model, carouselManager, configuration, ChangeCarouselSpeedAction.SpeedChangeType.RESET)
        );
        
        Action openThemeCustomizerAction = new controlador.actions.config.OpenThemeCustomizerAction("Personalizar Tema...", this.view, this.themeManager);
        registerAction(AppActionCommands.CMD_CONFIG_CUSTOM_THEME, openThemeCustomizerAction);
        
        // Actions que dependen del 'actionMap' completo para construir menús emergentes.
    } // --- FIN del metodo createViewDependentActions ---
    
    
    private Action createSwitchDisplayModeAction(DisplayMode displayMode, String commandKey, String displayName) {
        // 1. Obtenemos el icono como ya hacías
        ImageIcon icon = getIconForCommand(commandKey);
        
        // 2. Creamos la nueva acción usando el constructor CORREGIDO de SwitchDisplayModeAction.
        //    Ya no necesita el 'generalController'. Necesita 'displayModeManager' y 'model'.
        SwitchDisplayModeAction action = new SwitchDisplayModeAction(
            displayName,            // El nombre de la acción (para tooltips, etc.)
            this,
            displayMode,            // El modo al que cambiará (GRID, SINGLE_IMAGE, etc.)
            this.model              // El modelo, que necesita para sincronizar el estado
        );
        
        // 3. Configuramos las propiedades visuales de la acción, como ya lo harías en otros sitios
        if (icon != null) {
            action.putValue(Action.SMALL_ICON, icon);
        }
        action.putValue(Action.SHORT_DESCRIPTION, displayName); // Tooltip
        
        // 4. Asignamos el comando canónico
        action.putValue(Action.ACTION_COMMAND_KEY, commandKey);
        
        return action;
    } // --- Fin del método createSwitchDisplayModeAction ---
    
    
    
    private Action createSwitchWorkModeAction(WorkMode workMode, String commandKey, String displayName) {
        ImageIcon icon = getIconForCommand(commandKey);
        return new SwitchWorkModeAction(generalController, workMode, displayName, icon, displayName, null, commandKey);
    }
    
    private Action createSwitchToVisualizadorAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR); 
        SwitchToVisualizadorAction action = new SwitchToVisualizadorAction("Modo Visualizador", icon, this.generalController);
        this.contextSensitiveActions.add(action); 
        return action;
    }


    // --- SECCIÓN 4: MÉTODOS PRIVADOS AUXILIARES PARA CREAR ACTIONS ---
    
    

    /**
     * 4.1. Crea la Action genérica para funcionalidades pendientes.
     */
    private Action createFuncionalidadPendienteAction() {
        return new AbstractAction("Funcionalidad Pendiente") {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                String textoComponente = "";
                 if (e.getSource() instanceof AbstractButton) {
                     AbstractButton comp = (AbstractButton) e.getSource();
                     textoComponente = comp.getText();
                     if (textoComponente == null || textoComponente.isEmpty()) textoComponente = comp.getToolTipText();
                     if (textoComponente == null || textoComponente.isEmpty()) textoComponente = (String)getValue(Action.NAME);
                 }
                 if (textoComponente == null || textoComponente.isBlank()) textoComponente = e.getActionCommand();
                 if (textoComponente == null) textoComponente = "(desconocido)";

                 String mensaje = "Funcionalidad para '" + textoComponente + "' aún no implementada.";

                 // --- CAMBIO ---: Usamos null como componente padre para eliminar la dependencia de la vista.
                 JOptionPane.showMessageDialog(null, mensaje, "En Desarrollo", JOptionPane.INFORMATION_MESSAGE);
            }
        };
    } // --- FIN del metodo createFuncionalidadPendienteAction ---

    
    /**
     * 4.2. Helper para obtener el ImageIcon para un comando dado.
     */
    private ImageIcon getIconForCommand(String appCommand) {
        // Buscamos la información completa del icono en el nuevo mapa
        IconInfo info = this.comandoToIconKeyMap.get(appCommand);
        
        if (info != null && info.iconKey() != null && !info.iconKey().isBlank()) {
            // Si el scope es COMMON, llamamos al método para iconos comunes.
            if (info.scope() == vista.config.IconScope.COMMON) {
            	
            	logger.debug("[ActionFactory] cargando icono comun: " + info.iconKey );
                
            	return this.iconUtils.getScaledCommonIcon(info.iconKey(), this.iconoAncho, this.iconoAlto);
            } else {
            // Si no, llamamos al método para iconos tematizados (comportamiento por defecto).
            	
            	logger.debug("[ActionFactory] cargando icono Tematizado: " + info.iconKey );
            	
                return this.iconUtils.getScaledIcon(info.iconKey(), this.iconoAncho, this.iconoAlto);
            }
        }
        return null; 
    } // --- Fin del método getIconForCommand ---

    
    // --- 4.3. Métodos Create para Actions de Zoom ---
    private Action createToggleZoomManualAction() { 
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
        return new ToggleZoomManualAction("Activar Zoom Manual", icon, this.zoomManager, this.generalController.getVisorController(), this.model);
    } // --- Fin del método createToggleZoomManualAction ---
    
    
    private Action createResetZoomAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ZOOM_RESET);
        return new ResetZoomAction("Resetear Zoom", icon, this.zoomManager);
    } // --- Fin del método createResetZoomAction ---
    
    
    private Action createToggleZoomToCursorAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ZOOM_TOGGLE_TO_CURSOR);
        
        // Ahora llamamos al nuevo constructor y le pasamos el 'icon' que hemos cargado.
        ToggleZoomToCursorAction action = new ToggleZoomToCursorAction("Zoom al Cursor", icon, this.generalController.getVisorController());
        
        // El resto se queda igual...
        this.contextSensitiveActions.add(action);
        
        return action;
    } // --- Fin del método createToggleZoomToCursorAction ---
    
    
    private Action createAplicarModoZoomAction(String commandKey, String displayName, ZoomModeEnum modo) {
        ImageIcon icon = getIconForCommand(commandKey);
        
        // Creamos un Runnable (un callback) que llama al método de sincronización del VisorController.
        // El VisorController es inyectado en ActionFactory a través del constructor, así que tenemos acceso.
        Runnable syncCallback = () -> {
        	if (this.zoomManager != null) {
                this.zoomManager.sincronizarEstadoVisualBotonesYRadiosZoom();
            }
        };

        // Pasamos el zoomManager, el modelo y el nuevo callback.
        return new AplicarModoZoomAction(this.zoomManager, this.model, displayName, icon, modo, commandKey, syncCallback);
        
    } // --- Fin del método createAplicarModoZoomAction ---
    
    
    // --- 4.4. Métodos Create para Actions de Navegación ---
    private Action createFirstImageAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_NAV_PRIMERA);
        // Cambiamos la dependencia de listCoordinator a generalController (que implementa IModoController)
        FirstImageAction action = new FirstImageAction(this.generalController, "Primera Imagen", icon);
        this.contextSensitiveActions.add(action); // Registramos la acción para que se actualice su estado
        return action;
    } // --- Fin del método createFirstImageAction ---
    
    
    private Action createPreviousImageAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_NAV_ANTERIOR);
        // Cambiamos la dependencia
        PreviousImageAction action = new PreviousImageAction(this.generalController, "Imagen Anterior", icon);
        this.contextSensitiveActions.add(action); // La registramos
        return action;
    } // --- Fin del método createPreviousImageAction ---
    
    
    private Action createNextImageAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_NAV_SIGUIENTE);
        // Cambiamos la dependencia
        NextImageAction action = new NextImageAction(this.generalController, "Siguiente Imagen", icon);
        this.contextSensitiveActions.add(action); // La registramos
        return action;
    } // --- Fin del método createNextImageAction ---
    
    
    private Action createLastImageAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_NAV_ULTIMA);
        // Cambiamos la dependencia
        LastImageAction action = new LastImageAction(this.generalController, "Última Imagen", icon);
        this.contextSensitiveActions.add(action); // La registramos
        return action;
    } // --- Fin del método createLastImageAction ---
    
    
    private Action createToggleNavegacionCircularAction() {
        return new ToggleNavegacionCircularAction("Alternar Navegación Circular", null, this.configuration, this.model, this.generalController.getVisorController(), ConfigKeys.COMPORTAMIENTO_NAVEGACION_CIRCULAR,AppActionCommands.CMD_TOGGLE_WRAP_AROUND);
    } // --- Fin del método createToggleNavegacionCircularAction ---


    // --- 4.5. Métodos Create para Actions de Edición (usarán EditionManager) ---
    private Action createRotateRightAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_IMAGEN_ROTAR_DER);
        RotateRightAction action = new RotateRightAction(this.editionManager, this.model, "Girar Derecha", icon);
        this.contextSensitiveActions.add(action); 
        return action;
    } // --- Fin del método createRotateRightAction ---
    
    
    private Action createRotateLeftAction() {
    	ImageIcon icon = getIconForCommand(AppActionCommands.CMD_IMAGEN_ROTAR_IZQ);
        RotateLeftAction action = new RotateLeftAction(this.editionManager, this.model, "Girar Izquierda", icon);
        this.contextSensitiveActions.add(action); 
        return action;
    } // --- Fin del método createRotateLeftAction ---
    
    
    private Action createFlipHorizontalAction() {
    	ImageIcon icon = getIconForCommand(AppActionCommands.CMD_IMAGEN_VOLTEAR_H);
    	FlipHorizontalAction action = new FlipHorizontalAction(this.editionManager, this.model, "Voltear Horizontal", icon);
        this.contextSensitiveActions.add(action); 
        return action;
    } // --- Fin del método createFlipHorizontalAction ---
    
    
    private Action createFlipVerticalAction() {
    	ImageIcon icon = getIconForCommand(AppActionCommands.CMD_IMAGEN_VOLTEAR_V);
    	FlipVerticalAction action = new FlipVerticalAction(this.editionManager, this.model, "Voltear Vertical", icon);
        this.contextSensitiveActions.add(action); 
        return action;
    } // --- Fin del método createFlipVerticalAction ---
    
    
    private Action createCropAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_IMAGEN_RECORTAR);
        CropAction action = new CropAction(this.editionManager, this.model, "Recortar", icon);
        
        this.contextSensitiveActions.add(action);
        return action;
    } // --- Fin del método createCropAction ---

    
    // --- 4.6. Métodos Create para Actions de Archivo (usarán FileOperationsManager o lógica interna) ---
    private Action createOpenFileAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ARCHIVO_ABRIR);
        return new OpenFileAction("Abrir Carpeta...", icon, this.fileOperationsManager, this.model, this.generalController);
    } // --- Fin del método createOpenFileAction ---
    
    
    private Action createDeleteAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_IMAGEN_ELIMINAR);
        return new DeleteAction("Eliminar Imagen", icon, this.fileOperationsManager, this.model);
    } // --- Fin del método createDeleteAction ---
    
    
    private Action createRefreshAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ESPECIAL_REFRESCAR); 
        return new RefreshAction("Refrescar", icon, this.generalController);
    } // --- Fin del método createRefreshAction ---
    
    
    private Action createLocateFileAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_IMAGEN_LOCALIZAR);
        LocateFileAction action = new LocateFileAction(this.model, this.generalController.getVisorController(), "Localizar Archivo", icon);
        this.contextSensitiveActions.add(action);
        return action;
    } // --- Fin del método createLocateFileAction ---    

    
    // --- 4.7. Métodos Create para Actions de Vista (Toggles UI) ---
    private Action createToggleMenuBarAction() {
        return new ToggleUIElementVisibilityAction(
            this.viewManager,
            this.configuration,
            "Barra de Menú",
            "interfaz.menu.vista.barra_de_menu.seleccionado", // La clave de config
            "Barra_de_Menu", // El ID que entiende ViewManager
            AppActionCommands.CMD_VISTA_TOGGLE_MENU_BAR
        );
    } // --- Fin del método createToggleMenuBarAction ---

    private Action createToggleToolBarAction() {
        return new ToggleUIElementVisibilityAction( // <-- Usamos la Action genérica
            this.viewManager,
            this.configuration,
            "Barra de Botones",
            "interfaz.menu.vista.barra_de_botones.seleccionado", // La clave de config
            "Barra_de_Botones", // El ID que entiende ViewManager
            AppActionCommands.CMD_VISTA_TOGGLE_TOOL_BAR
        );
    } // --- Fin del método createToggleToolBarAction ---

    private Action createToggleFileListAction() {
        return new ToggleUIElementVisibilityAction( // <-- Usamos la Action genérica
            this.viewManager,
            this.configuration,
            "Lista de Archivos",
            "interfaz.menu.vista.mostrar_ocultar_la_lista_de_archivos.seleccionado",
            "mostrar_ocultar_la_lista_de_archivos",
            AppActionCommands.CMD_VISTA_TOGGLE_FILE_LIST
        );
    } // --- Fin del método createToggleFileListAction ---

    private Action createToggleThumbnailsAction() {
        return new ToggleUIElementVisibilityAction( // <-- Usamos la Action genérica
            this.viewManager,
            this.configuration,
            "Barra de Miniaturas",
            "interfaz.menu.vista.imagenes_en_miniatura.seleccionado",
            "imagenes_en_miniatura",
            AppActionCommands.CMD_VISTA_TOGGLE_THUMBNAILS
        );
    } // --- Fin del método createToggleThumbnailsAction ---
    
    private Action createToggleStatusBarAction() {
    	return new ToggleUIElementVisibilityAction(
    	        this.viewManager,
    	        this.configuration,
    	        "Barra de Estado",
    	        ConfigKeys.INFOBAR_INF_VISIBLE,
    	        "barra_de_estado",
    	        //"REFRESH_INFO_BAR_INFERIOR",
    	        AppActionCommands.CMD_VISTA_TOGGLE_INFOBAR_INFERIOR
    	    );
    } // --- Fin del método createToggleLocationBarAction ---
    
    private Action createToggleInfoBarAction() {
    	return new ToggleUIElementVisibilityAction(
    	        this.viewManager,
    	        this.configuration,
    	        "Barra de Información",
    	        ConfigKeys.INFOBAR_SUP_VISIBLE,
    	        "barra_de_info_imagen", // <-- El ID que pusimos en el switch del ViewManager
    	        AppActionCommands.CMD_VISTA_TOGGLE_INFOBAR_SUPERIOR
    	    );
    } // --- Fin del método createToggleLocationBarAction ---
    
    
    private Action createToggleCheckeredBackgroundAction() {
    		return new ToggleCheckeredBackgroundAction("Fondo a Cuadros", null, this.viewManager, this.configuration, "interfaz.menu.vista.fondo_a_cuadros.seleccionado", AppActionCommands.CMD_VISTA_TOGGLE_CHECKERED_BG);
   	} // --- Fin del método createToggleCheckeredBackgroundAction ---
    
    
    private Action createToggleAlwaysOnTopAction() {
   	    return new ToggleAlwaysOnTopAction("Mantener Ventana Siempre Encima", null, this.viewManager, this.configuration, "interfaz.menu.vista.mantener_ventana_siempre_encima.seleccionado", controlador.commands.AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP);
   	} // --- Fin del método createToggleAlwaysOnTopAction ---
    
    
    private Action createMostrarDialogoListaAction() {
   	    ImageIcon icon = getIconForCommand(AppActionCommands.CMD_VISTA_MOSTRAR_DIALOGO_LISTA);
   	    return new MostrarDialogoListaAction("Mostrar Lista de Imágenes", icon, this.model, this.generalController.getVisorController(), this.viewManager);
   	} // --- Fin del método createMostrarDialogoListaAction ---
    
    
    private Action createToggleFullScreenAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_VISTA_PANTALLA_COMPLETA);
        return new ToggleFullScreenAction("Pantalla Completa", icon, this.generalController);
        
    } // --- Fin del método createToggleFullScreenAction ---
    
    
    private Action createToggleMiniatureTextAction() {
    	return new ToggleMiniatureTextAction("Mostrar Nombres en Miniaturas", null, this.configuration, this.view, ConfigKeys.VISTA_MOSTRAR_NOMBRES_MINIATURAS_STATE, AppActionCommands.CMD_VISTA_TOGGLE_MINIATURE_TEXT);
    } // --- Fin del método createToggleMiniatureTextAction ---
    

    // --- 4.8. Métodos Create para Actions de Tema ---
    private Action createToggleThemeAction(String themeNameInternal, String displayNameForMenu, String commandKey) {
        return new ToggleThemeAction(this.themeManager, this.generalController.getVisorController(), themeNameInternal, displayNameForMenu, commandKey);
    } // --- Fin del método createToggleThemeAction ---
   
    
    // --- 4.9. Métodos Create para Actions de Toggle Generales ---
    private Action createToggleSubfoldersAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
        return new ToggleSubfoldersAction("Alternar Subcarpetas", icon, this.model, this.generalController);
    }// --- Fin del método createToggleSubfoldersAction ---
    
    private Action createSetSubfolderReadModeAction(String commandKey, String displayName, boolean representaModoIncluirSubcarpetas) {
    	ImageIcon icon = null; 
    	return new SetSubfolderReadModeAction(displayName, icon, this.generalController, this.model,representaModoIncluirSubcarpetas, commandKey);
    } // --- Fin del método createSetSubfolderReadModeAction ---
        
    
    private Action createToggleProporcionesAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);
        return new ToggleProporcionesAction("Mantener Proporciones", icon, this.model, this.generalController.getVisorController());
    } // --- Fin del método createToggleProporcionesAction ---
    
    
    private Action createToggleSyncAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_TOGGLE_SYNC_VISOR_CARRUSEL);
        return new ToggleSyncAction("Sincronizar Visor/Carrusel", icon, this.model, this.configuration, this.generalController);
    } // --- Fin del método createToggleSyncAction ---
   
    
    // --- 4.10. Métodos Create para Actions de Proyecto ---
    private Action createToggleMarkImageAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
        // Ahora pasamos la referencia al GeneralController, no al VisorController.
        ToggleMarkImageAction action = new ToggleMarkImageAction(this.generalController, "Marcar/Desmarcar para Proyecto", icon);
        this.contextSensitiveActions.add(action);
        return action;
    } // --- FIN del método createToggleMarkImageAction ---
    
    private Action createLocalizarArchivoProyectoAction() {
        // Esta acción no necesita icono, es para menús contextuales.
        return new AbstractAction("Localizar archivo en el explorador") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (projectControllerRef != null) {
                    projectControllerRef.solicitarLocalizarArchivoSeleccionado();
                }
            }
        };
    } // --- FIN del método createLocalizarArchivoProyectoAction ---

    private Action createVaciarDescartesAction() {
        // Esta acción tampoco necesita icono.
        return new AbstractAction("Vaciar lista de descartes") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (projectControllerRef != null) {
                    projectControllerRef.solicitarVaciarDescartes();
                }
            }
        };
    } // --- FIN del método createVaciarDescartesAction ---

    
    private Action createGestionarProyectoAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_PROYECTO_GESTIONAR);
        return new GestionarProyectoAction(this.generalController, "Ver Proyecto", icon);
    } // --- Fin del método createGestionarProyectoAction ---
    
    
    private Action createNuevoProyectoAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_PROYECTO_NUEVO);
        
        return new NuevoProyectoAction(this.generalController, "Nuevo Proyecto", icon);
    } // ---FIN de metodo createNuevoProyectoAction---

    private Action createAbrirProyectoAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_PROYECTO_ABRIR);
        return new AbrirProyectoAction(this.generalController, "Abrir Proyecto...", icon);
    } // ---FIN de metodo createAbrirProyectoAction---

    private Action createGuardarProyectoAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_PROYECTO_GUARDAR);
        return new GuardarProyectoAction(this.generalController, "Guardar Proyecto", icon);
    } // ---FIN de metodo createGuardarProyectoAction---

    private Action createGuardarProyectoComoAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_PROYECTO_GUARDAR_COMO);
        return new GuardarProyectoComoAction(this.generalController, "Guardar Proyecto Como...", icon);
    } // ---FIN de metodo createGuardarProyectoComoAction---

    private Action createEliminarProyectoAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_PROYECTO_ELIMINAR);
        return new EliminarProyectoAction(this.generalController, "Eliminar Proyecto...", icon);
    } // ---FIN de metodo createEliminarProyectoAction---
    
    
    private Action createMoveToDiscardsAction() {
        // Esta acción no necesita icono ya que es para un menú contextual
        return new MoveToDiscardsAction(this.generalController, this.model);
    } // --- FIN del método createMoveToDiscardsAction ---

    
    private Action createRestoreFromDiscardsAction() {
        // Esta acción no necesita icono
        return new RestoreFromDiscardsAction(this.generalController, this.generalController.getVisorController().getComponentRegistry());
    } // --- FIN del método createRestoreFromDiscardsAction ---
    
    
    private Action createAssignFileAction() {
        return new AbstractAction("Asignar archivo manualmente...") {
        	private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent e) {
                generalController.getProjectController().solicitarAsignacionManual();
            }
        };
    } // --- Fin del método createAssignFileAction ---

    
    private Action createOpenLocationAction() {
        return new AbstractAction("Abrir ubicación de la imagen") {
        	private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent e) {
                generalController.getProjectController().solicitarAbrirUbicacionImagen();
            }
        };
    } // --- Fin del método createOpenLocationAction ---

    
    private Action createRemoveFromQueueAction() {
        // Usamos la nueva clase dedicada en lugar de la anónima.
        return new RemoveFromExportQueueAction(generalController.getProjectController());
    } // --- Fin del método createRemoveFromQueueAction ---
    
    
    private Action createToggleIgnoreCompressedAction() {
        // Ahora usamos nuestra nueva clase dedicada, que es sensible al contexto
        ToggleIgnoreCompressedAction action = new ToggleIgnoreCompressedAction(generalController.getProjectController());
        this.contextSensitiveActions.add(action); // ¡Muy importante registrarla!
        return action;
    } // --- Fin del método createToggleIgnoreCompressedAction ---
    
   

    
    /**
     * Crea la acción para eliminar permanentemente una imagen del proyecto.
     * Esta acción es para el menú contextual de la lista de descartes.
     */
    private Action createEliminarDeProyectoAction() {
        return new AbstractAction("Eliminar permanentemente del proyecto") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                // Llama a un método en ProjectController que crearemos más adelante.
                if (projectControllerRef != null) {
                    projectControllerRef.solicitarEliminacionPermanente();
                }
            }
        };
    } // --- Fin del método createEliminarDeProyectoAction ---
    
    
    /**
     * Crea la acción para relocalizar una imagen no encontrada.
     * Es sensible al contexto.
     */
    private Action createRelocateImageAction() {
        // Esta acción es para el menú contextual, no necesita icono por ahora.
        RelocateImageAction action = new RelocateImageAction(this.generalController.getProjectController());
        this.contextSensitiveActions.add(action); // ¡Importante! Registrarla como sensible al contexto.
        return action;
    } // --- Fin del método createRelocateImageAction ---
    
    
    private Action createIniciarExportacionAction() {
        // Obtenemos un icono (ej. un 'play' o un 'exportar')
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_INICIAR_EXPORTACION);
        // Creamos la acción
        Action action = new AbstractAction("Iniciar Exportación", icon) {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                generalController.getProjectController().solicitarInicioExportacion();
            }
        };
        // Por defecto, esta acción debe estar deshabilitada
        action.setEnabled(false);
        return action;
    } // --- Fin del método createIniciarExportacionAction ---
    
    
    /**
     * Crea la acción para seleccionar la carpeta de destino de la exportación.
     */
    private Action createSeleccionarCarpetaAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_EXPORT_SELECCIONAR_CARPETA);
        return new AbstractAction("" /*"Seleccionar Carpeta..."*/, icon) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (projectControllerRef != null) {
                    projectControllerRef.solicitarSeleccionCarpetaDestino();
                }
            }
        };
    } // --- Fin del método createSeleccionarCarpetaAction ---
    
    private Action createRefreshExportQueueAction() {
        return new controlador.actions.projects.RefreshExportQueueAction(this.projectControllerRef);
    } // ---FIN de metodo [createRefreshExportQueueAction]---
    
    private Action createAddAssociatedFileAction() {
        return new AddAssociatedFileAction(this.projectControllerRef);
    }// --- Fin del método createAddAssociatedFileAction
    
    private Action createDeleteAssociatedFileAction() {
        return new DeleteAssociatedFileAction(this.projectControllerRef);
    } // --- Fin del método createDeleteAssociatedFileAction

    private Action createLocateAssociatedFileAction() {
        return new LocateAssociatedFileAction(this.projectControllerRef);
    } // --- Fin del método createLocateAssociatedFileAction
    
    private Action createToggleExportViewAction() {
    	ImageIcon icon = getIconForCommand(AppActionCommands.CMD_EXPORT_ASSIGN_PANNEL);
        return new ToggleExportViewAction(this.projectControllerRef);
    } // ---FIN de metodo [createToggleExportViewAction]---
    
    private Action createToggleExportDetailsAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_EXPORT_DETALLES_SELECCION);
        ToggleExportDetailsAction action = new ToggleExportDetailsAction(this.projectControllerRef);
        if (icon != null) {
            action.putValue(Action.SMALL_ICON, icon);
        }
        return action;
    } // ---FIN de metodo [createToggleExportDetailsAction]---
    
    
    // --- 4.11. Métodos Create para Actions Especiales ---
     private Action createMenuAction() { 
         ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ESPECIAL_MENU);
         UIDefinitionService uiDefService = new UIDefinitionService();
         List<MenuItemDefinition> fullMenuStructure = uiDefService.generateMenuStructure();
         return new MenuAction("Menú Principal", icon, fullMenuStructure, this.actionMap, this.themeManager, this.configuration, this.generalController.getVisorController());
     } // --- Fin del método createMenuAction ---
     
     
     private Action createHiddenButtonsAction() {
         ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ESPECIAL_BOTONES_OCULTOS);
         return new HiddenButtonsAction("Más Opciones", icon, this.actionMap, this.themeManager, this.configuration, this.generalController.getVisorController() );
     } // --- Fin del método createHiddenButtonsAction ---

     
     // --- 4.12. Métodos Create para Actions de Paneo ---
     private Action createPanActionAbsolute(String command, Direction direction) {
         PanAction action = new PanAction(generalController, command, direction);
         
         ImageIcon icon = getIconForCommand(command);
         if (icon != null) {
             action.putValue(Action.SMALL_ICON, icon);
         }
         this.contextSensitiveActions.add(action);
         return action;
     } // --- Fin del método createPanActionAbsolute ---

     
     private Action createPanActionIncremental(String command, Direction direction) {
         PanAction action = new PanAction(generalController, command, direction, INCREMENTAL_PAN_AMOUNT);
         
         // Obtener el icono correspondiente al comando y asignarlo a la acción.
         // Esto es necesario para que los JMenuItems que usen esta acción muestren un icono.
         ImageIcon icon = getIconForCommand(command);
         if (icon != null) {
             action.putValue(Action.SMALL_ICON, icon);
         }
         this.contextSensitiveActions.add(action);
         return action;
     } // --- Fin del método createPanActionIncremental ---

     
     // --- 4.13. Métodos Create para Actions de Control de Fondo ---
     
     private Action createSetBackgroundColorAction(String command, String themeKey, String name) {
	    Action action = new AbstractAction(name) {
	        private static final long serialVersionUID = 1L;
	        @Override
	        public void actionPerformed(ActionEvent e) {
	            if (viewManager != null && themeManager != null) {
	                java.awt.Color color = themeManager.getFondoSecundarioParaTema(themeKey);
	                viewManager.setSessionBackgroundColor(color);
	            }
	        }
	    };
	    // Asignamos el comando a la acción.
	    action.putValue(Action.ACTION_COMMAND_KEY, command);
	    return action;
	} // --- Fin del método createSetBackgroundColorAction ---
     
     
	private Action createSetCheckeredBackgroundAction(String name){
		Action action = new AbstractAction(name){
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e){

				if (viewManager != null){
					viewManager.setSessionCheckeredBackground();
				}
			}
		};
		action.putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_BACKGROUND_CHECKERED);
		return action;
		
	}// --- Fin del método createSetCheckeredBackgroundAction ---
 
	
	private Action createRequestCustomColorAction(String name) {
	    Action action = new AbstractAction(name) {
	        private static final long serialVersionUID = 1L;
	        @Override
	        public void actionPerformed(ActionEvent e) {
	            if (viewManager != null) {
	                viewManager.requestCustomBackgroundColor();
	            }
	        }
	    };
	    action.putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_BACKGROUND_CUSTOM_COLOR);
	    return action;
	} // --- Fin del método createRequestCustomColorAction ---
	
	
	private Action createNavegarCarpetaAnteriorAction() {
	    ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ORDEN_CARPETA_ANTERIOR);
	    // Instanciamos la nueva clase
	    SalirDeSubcarpetaAction action = new SalirDeSubcarpetaAction(this.generalController, "Subir Carpeta", icon);
	    // La registramos como sensible al contexto
	    this.contextSensitiveActions.add(action);
	    return action;
	} // --- FIN del metodo createNavegarCarpetaAnteriorAction ---

	
	private Action createNavegarCarpetaSiguienteAction() {
	    ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ORDEN_CARPETA_SIGUIENTE);
	    // Instanciamos la nueva clase
	    EntrarEnSubcarpetaAction action = new EntrarEnSubcarpetaAction(this.generalController, "Entrar en Carpeta", icon);
	    // La registramos como sensible al contexto
	    this.contextSensitiveActions.add(action);
	    return action;
	    
	} // --- FIN del metodo createNavegarCarpetaSiguienteAction ---

	
	private Action createNavegarCarpetaRaizAction() {
	    ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ORDEN_CARPETA_RAIZ);
	    
	    // Instanciamos la nueva clase dedicada
	    VolverACarpetaRaizAction action = new VolverACarpetaRaizAction(this.generalController, "Carpeta Raíz", icon);
	    
	    // La registramos como sensible al contexto para que se habilite/deshabilite sola
	    this.contextSensitiveActions.add(action);
	    
	    return action;
	    
	} // --- FIN del metodo createNavegarCarpetaRaizAction ---
	
	
	private Action createToggleLiveFilterAction() {
	    ImageIcon icon = getIconForCommand(AppActionCommands.CMD_FILTRO_TOGGLE_LIVE_FILTER);
	    return new ToggleLiveFilterAction(this.model, this.generalController, "Activar/Desactivar Filtro en Vivo", icon);
	} // --- Fin del método createToggleLiveFilterAction ---
	
	private Action createAddFilterAction(FilterType type) {
        String command = (type == FilterType.CONTAINS) ? AppActionCommands.CMD_FILTRO_ADD_POSITIVE : AppActionCommands.CMD_FILTRO_ADD_NEGATIVE;
        String name = (type == FilterType.CONTAINS) ? "Añadir Filtro Positivo" : "Añadir Filtro Negativo";
        ImageIcon icon = getIconForCommand(command);
        return new AddFilterAction(this.generalController, type, name, icon);
    } // --- Fin del método createAddFilterAction ---

    private Action createRemoveFilterAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_FILTRO_REMOVE_SELECTED);
        return new RemoveFilterAction(this.generalController, "Eliminar Filtro Seleccionado", icon);
    } // --- Fin del método createRemoveFilterAction ---

    private Action createClearAllFiltersAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_FILTRO_CLEAR_ALL);
        return new ClearAllFiltersAction(this.generalController, "Limpiar Todos los Filtros", icon);
    } // --- Fin del método createClearAllFiltersAction ---
	
    private Action createPersistLiveFilterAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_FILTRO_ACTIVO);
        return new PersistLiveFilterAction(this.generalController, "Hacer Filtro Persistente", icon);
    } // --- Fin del método createPersistLiveFilterAction ---
	
    private Action createSetFilterTypeAction(FilterSource source) {
        String command;
        String name;
        switch (source) {
            case FOLDER_PATH:
                command = AppActionCommands.CMD_FILTRO_SET_TYPE_FOLDER;
                name = "Establecer filtro por Carpeta";
                break;
            case FILENAME:
            default:
                command = AppActionCommands.CMD_FILTRO_SET_TYPE_FILENAME;
                name = "Establecer filtro por Nombre de Archivo";
                break;
        }
        ImageIcon icon = getIconForCommand(command);
        return new SetFilterTypeAction(generalController, source, name, icon, command);
    } // --- Fin del método createSetFilterTypeAction ---

    // Helper para obtener la acción pendiente
    public Action getFuncionalidadPendienteAction() {
        return this.funcionalidadPendienteAction;
    } // --- Fin del método getFuncionalidadPendienteAction ---
	private Action createCycleSortAction() {
	    // Obtener el icono inicial para la acción.
	    ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ORDEN_CICLO);
	    
	    // Instanciar la nueva clase, inyectando las dependencias necesarias.
	    CycleSortAction action = new CycleSortAction(
	        "Ordenar Lista",      // Nombre para menús, si se usa
	        icon,                 // Icono inicial
	        this.model,           // Referencia al modelo
	        this.generalController // Referencia al orquestador
	    );
	    
	    // Configurar propiedades adicionales que no están en el constructor de la Action.
	    action.putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ORDEN_CICLO);

	    return action;
	} // --- FIN del metodo createCycleSortAction ---

		
	private Action createOpenFolderAction() {
	    // Pasa el GeneralController en lugar del FolderTreeManager
	    return new OpenFolderAction("Abrir aquí (Limpiar Historial)", this.generalController);
	} // --- Fin del método createOpenFolderAction ---

	private Action createDrillDownFolderAction() {
	    // Pasa el GeneralController en lugar del FolderTreeManager
	    return new DrillDownFolderAction("Entrar en esta carpeta (Guardar Historial)", this.generalController);
	} // --- Fin del método createDrillDownFolderAction ---
	
	
	// Acciones de la toolbar del GRID
	
	private Action createSetGridTextAction() {
        return new AbstractAction("Añadir/Editar Etiqueta") {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (projectControllerRef != null) {
                    projectControllerRef.solicitarEtiquetaParaImagenSeleccionada();
                }
            }
        };
    } // ---FIN de metodo ---

    private Action createRemoveGridTextAction() {
        return new AbstractAction("Borrar Etiqueta") {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (projectControllerRef != null) {
                    projectControllerRef.solicitarBorradoEtiquetaParaImagenSeleccionada();
                }
            }
        };
    } // ---FIN de metodo ---

    private Action createGridSizeUpAction() {
        return new AbstractAction("Aumentar Tamaño Miniatura") {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (projectControllerRef != null) {
                    projectControllerRef.cambiarTamanoGrid(1.2); // Aumenta un 20%
                }
            }
        };
    } // ---FIN de metodo ---

    private Action createGridSizeDownAction() {
        return new AbstractAction("Reducir Tamaño Miniatura") {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (projectControllerRef != null) {
                    projectControllerRef.cambiarTamanoGrid(0.8); // Reduce un 20%
                }
            }
        };
    } // ---FIN de metodo ---
	
    
    private Action createToggleGridStateAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_GRID_SHOW_STATE);
        // Leemos el estado inicial desde la configuración
        boolean initialState = configuration.getBoolean(ConfigKeys.GRID_MOSTRAR_ESTADO_STATE, true);
        model.setGridMuestraEstado(initialState);
        return new ToggleGridStateAction("Mostrar Estado", icon, model, registry);
    } // ---FIN de metodo---
    
    
    private Action createVerAtajosAction() {
        return new AbstractAction("Ver Atajos de Teclado...") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                // La Action ahora solo delega la llamada al VisorController
                if (generalController != null && generalController.getVisorController() != null) {
                    generalController.getVisorController().mostrarDialogoAyudaAtajos();
                }
            }
        };
    } // ---FIN de metodo createVerAtajosAction---
    
    
    private Action createShowHelpAction() {
        // Esta acción depende de la vista, pero la creamos en la fase "Core"
        // y le pasamos la referencia a la vista que ya tenemos.
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_AYUDA_MOSTRAR_GUIA);
        return new ShowHelpAction("Guía de Usuario...", icon, this.view);
    } // ---FIN de metodo createShowHelpAction---
    
	
	// *********************************************** 
    // --- SECCIÓN 5: GETTERS Y SETTERS ---
	// ***********************************************	
	
    public Map<String, Action> getActionMap() {
         return this.actionMap; 
    } // --- Fin del método getActionMap ---
     
    
    public List<ContextSensitiveAction> getContextSensitiveActions() {
        return Collections.unmodifiableList(this.contextSensitiveActions);
    } // --- Fin del método getContextSensitiveActions ---
    
    public Map<String, IconInfo> getComandoToIconKeyMap() {return Collections.unmodifiableMap(this.comandoToIconKeyMap);}
    public int getIconoAncho() { return this.iconoAncho;}
    public int getIconoAlto() { return this.iconoAlto;}
    
    public void registerAction(String commandKey, Action action) {
        if (this.actionMap.containsKey(commandKey)) {
            logger.warn("WARN [ActionFactory]: Reemplazando Action existente para el comando: " + commandKey);
        }
        this.actionMap.put(commandKey, action);
        logger.debug("  [ActionFactory] Action registrada/actualizada para comando: " + commandKey);
    } // --- Fin del método registerAction ---
    
    
    /**
     * Itera sobre todas las acciones creadas en el actionMap y actualiza sus iconos
     * basándose en el tema actualmente activo en el ThemeManager.
     * Esto es crucial para el cambio de tema en caliente, ya que los iconos se
     * almacenan como una propiedad dentro de cada objeto Action.
     */
    public void actualizarIconosDeAcciones() {
        logger.debug("[ActionFactory] Iniciando actualización de iconos para todas las acciones...");
        if (actionMap == null || actionMap.isEmpty()) {
            logger.debug("  -> No hay acciones en el mapa para actualizar. Proceso omitido.");
            return;
        }

        int iconosActualizados = 0;
        // Iteramos sobre todas las entradas del mapa de acciones.
        for (Map.Entry<String, Action> entry : actionMap.entrySet()) {
        	
            String comando = entry.getKey();
            Action action = entry.getValue();

            // Usamos el método que ya tenemos para obtener el ImageIcon para un comando.
            // Este método, getIconForCommand, internamente ya usa IconUtils, que a su vez
            // usa ThemeManager para obtener el tema ACTUAL. Por lo tanto, obtendrá el icono correcto.
            ImageIcon nuevoIcono = getIconForCommand(comando);

            // Solo actualizamos si se encontró un nuevo icono.
            // Algunas acciones pueden no tener icono por diseño.
            
            // LOG DEBUG TEMA: Actualizando icono para 'MODO VISUALIZADOR
            if (comando.equals(AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR)) {
                logger.debug("DEBUG TEMA: Actualizando icono para 'MODO VISUALIZADOR'. Icono obtenido: " + (nuevoIcono != null ? nuevoIcono.toString() : "NULL"));
                IconInfo info = this.comandoToIconKeyMap.get(comando);
                if (info != null) {
                     logger.debug("DEBUG TEMA: ... a partir de la clave de icono: " + info.iconKey());
                }
            }
            // --- FIN DE AÑADIDO ---
            
            
            if (nuevoIcono != null) {
                // Actualizamos la propiedad Action.SMALL_ICON de la Action.
                // Los componentes de Swing (JButton, JMenuItem) que usan esta Action
                // escucharán este cambio de propiedad y actualizarán su icono automáticamente.
                action.putValue(Action.SMALL_ICON, nuevoIcono);
                iconosActualizados++;
            }
        }
        logger.debug("[ActionFactory] Actualización de iconos completada. Se actualizaron " + iconosActualizados + " acciones con un nuevo icono.");
    } // --- Fin del método actualizarIconosDeAcciones ---
    
    
    
    
    public void setView(VisorView view) { this.view = view; }
    public void setZoomManager(IZoomManager zoomManager) { this.zoomManager = zoomManager; }
    public void setViewManager(IViewManager viewManager) {this.viewManager = Objects.requireNonNull(viewManager);}
    public void setEditionManager(IEditionManager editionManager) {this.editionManager = Objects.requireNonNull(editionManager);}
    public void setListCoordinator(IListCoordinator listCoordinator) {this.listCoordinator = Objects.requireNonNull(listCoordinator);}
    public void setCarouselManager(CarouselManager carouselManager) {this.carouselManager = carouselManager;}
    
    public DisplayModeManager getDisplayModeManager(){ return displayModeManager; }    
    public void setDisplayModeManager(DisplayModeManager displayModeManager){ this.displayModeManager = displayModeManager; }

	public CarouselManager getCarouselManager() {return this.carouselManager;}
    public void setCarouselListCoordinator(ICarouselListCoordinator coordinator) {this.carouselListCoordinator = coordinator;}    
    public List<ContextSensitiveAction> getCarouselContextSensitiveActions() {return Collections.unmodifiableList(this.carouselContextSensitiveActions);}
    
    public void setFileOperationsManager(FileOperationsManager fileOperationsManager) {
        this.fileOperationsManager = Objects.requireNonNull(fileOperationsManager, "FileOperationsManager no puede ser null en setFileOperationsManager");
    }
    
}	// --- FIN de la clase ActionFactory ---


