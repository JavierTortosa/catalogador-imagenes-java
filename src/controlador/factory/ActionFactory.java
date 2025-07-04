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

import controlador.GeneralController;
import controlador.ProjectController;
import controlador.actions.archivo.DeleteAction;
// --- SECCIÓN 0: IMPORTS DE CLASES ACTION ESPECCÍFICAS ---
// (Asegúrate de que estas clases Action tengan los constructores correctos)
import controlador.actions.archivo.OpenFileAction;
import controlador.actions.archivo.RefreshAction;
import controlador.actions.config.SetInfoBarTextFormatAction;
import controlador.actions.config.SetSubfolderReadModeAction;
import controlador.actions.config.ToggleUIElementVisibilityAction;
import controlador.actions.edicion.CropAction;
import controlador.actions.edicion.FlipHorizontalAction;
import controlador.actions.edicion.FlipVerticalAction;
import controlador.actions.edicion.RotateLeftAction;
import controlador.actions.edicion.RotateRightAction;
import controlador.actions.especiales.HiddenButtonsAction;
import controlador.actions.especiales.MenuAction;
import controlador.actions.navegacion.FirstImageAction;
import controlador.actions.navegacion.LastImageAction;
import controlador.actions.navegacion.NextImageAction;
import controlador.actions.navegacion.PreviousImageAction;
// Importaciones para PanAction y Direction
import controlador.actions.pan.PanAction;
import controlador.actions.projects.GestionarProyectoAction;
import controlador.actions.projects.ToggleMarkImageAction;
import controlador.actions.tema.ToggleThemeAction;
import controlador.actions.toggle.ToggleNavegacionCircularAction;
import controlador.actions.toggle.ToggleProporcionesAction;
import controlador.actions.toggle.ToggleSubfoldersAction;
import controlador.actions.vista.MostrarDialogoListaAction;
import controlador.actions.vista.SwitchToVisualizadorAction;
import controlador.actions.vista.ToggleAlwaysOnTopAction;
import controlador.actions.vista.ToggleCheckeredBackgroundAction;
import controlador.actions.vista.ToggleFileListAction;
import controlador.actions.vista.ToggleMenuBarAction;
import controlador.actions.vista.ToggleMiniatureTextAction;
import controlador.actions.vista.ToggleThumbnailsAction;
import controlador.actions.vista.ToggleToolBarAction;
import controlador.actions.zoom.AplicarModoZoomAction;
import controlador.actions.zoom.ResetZoomAction;
import controlador.actions.zoom.ToggleZoomManualAction;
import controlador.actions.zoom.ToggleZoomToCursorAction;
import controlador.commands.AppActionCommands;
import controlador.imagen.LocateFileAction;
import controlador.interfaces.ContextSensitiveAction;
import controlador.managers.FileOperationsManager;
import controlador.managers.interfaces.IEditionManager;
import controlador.managers.interfaces.IListCoordinator;
import controlador.managers.interfaces.IProjectManager;
import controlador.managers.interfaces.IViewManager;
import controlador.managers.interfaces.IZoomManager;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.zoom.ZoomModeEnum;
import vista.VisorView;
import vista.components.Direction;
import vista.config.MenuItemDefinition;
import vista.config.UIDefinitionService;
//import vista.config.ViewUIConfig;
import vista.theme.ThemeManager;
import vista.util.IconUtils;


public class ActionFactory {

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

//        private final VisorController controllerRef;
    private final GeneralController generalController;
    
    private final ProjectController projectControllerRef;
    private final ThemeManager themeManager;
    
    // private final EditionManager editionManager; // Descomentar cuando se implemente y se inyecte
    private final IProjectManager projectService; // Servicio de persistencia de proyectos
    private IListCoordinator listCoordinator;
    private IEditionManager editionManager;

    // 1.3. Mapa para obtener claves de icono
    private final Map<String, String> comandoToIconKeyMap; 

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
            Map<String, String> comandoToIconKeyMap,
            IViewManager viewManager,
            ThemeManager themeManager,
            
            GeneralController generalController,
//            VisorController controller,
            
            ProjectController projectController
    ){ 
        
        // 2.1. Asignar dependencias principales.
        this.model 						= Objects.requireNonNull(model, "VisorModel no puede ser null en ActionFactory");
        this.view = view; // Se permite que la vista sea null en el constructor.
        this.configuration 				= Objects.requireNonNull(configuration, "ConfigurationManager no puede ser null en ActionFactory");
        this.iconUtils 					= Objects.requireNonNull(iconUtils, "IconUtils no puede ser null en ActionFactory");
        
        this.generalController			= Objects.requireNonNull(generalController, "GeneralController no puede ser null en ActionFactory");
        
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
        this.comandoToIconKeyMap 		= Objects.requireNonNull(comandoToIconKeyMap, "comandoToIconKeyMap no puede ser null");
        
        // 2.4. Leer dimensiones de iconos desde la configuración.
        this.iconoAncho 				= configuration.getInt("iconos.ancho", 24);
        this.iconoAlto 					= configuration.getInt("iconos.alto", 24);

        // 2.5. Inicializar el mapa interno de acciones.
        this.actionMap 					= new HashMap<>();

    } // --- Fin del método ActionFactory (constructor) ---

    // --- CAMBIO ---
    // El método initializeActions se renombra a initializeCoreActions y se ajusta.
    /**
     * Inicializa todas las acciones que NO dependen de la existencia de la 'view'.
     * Este método debe ser llamado antes de que se necesiten las acciones para construir
     * componentes como las barras de herramientas.
     */
    public void initializeCoreActions() {
		System.out.println("  [ActionFactory] Inicializando Actions principales (no dependientes de la vista)...");

		if (zoomManager == null || fileOperationsManager == null || editionManager == null
				|| listCoordinator == null || viewManager == null)
		{
			throw new IllegalStateException(
					"No se pueden inicializar las acciones principales porque las dependencias (zoomManager, etc.) no han sido inyectadas.");
		}
		
		createCoreActions();
		
		System.out.println(
				"  [ActionFactory] Actions principales inicializadas y mapeadas. Total: " + this.actionMap.size());
	} // --- Fin del método initializeCoreActions ---

    // --- CAMBIO ---
    // Se añade un nuevo método para las acciones que dependen de la vista.
    /**
     * Inicializa todas las acciones que SÍ dependen de la existencia de la 'view'.
     * Este método DEBE ser llamado DESPUÉS de crear la 'view' y de inyectarla en esta fábrica.
     */
    public void initializeViewDependentActions() {
        System.out.println("  [ActionFactory] Inicializando Actions dependientes de la vista...");
        if (view == null) {
            throw new IllegalStateException("No se pueden inicializar las acciones dependientes de la vista porque la 'view' es null.");
        }

        createViewDependentActions();

        System.out.println(
				"  [ActionFactory] Actions dependientes de la vista inicializadas. Total de actions: " + this.actionMap.size());
    } // --- Fin del método initializeViewDependentActions ---
    

	/**
     * Crea las acciones que NO dependen de la 'view'.
     */
private void createCoreActions() {
    	
        // 3.1. Crear la Action genérica para funcionalidades pendientes primero.
        this.funcionalidadPendienteAction = createFuncionalidadPendienteAction();
        actionMap.put(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, this.funcionalidadPendienteAction);

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

        // 3.6. Crear y registrar Actions de Vista (Toggles de UI)
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_MENU_BAR, createToggleMenuBarAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_TOOL_BAR, createToggleToolBarAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_FILE_LIST, createToggleFileListAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_THUMBNAILS, createToggleThumbnailsAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_LOCATION_BAR, createToggleLocationBarAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_CHECKERED_BG, createToggleCheckeredBackgroundAction());
        actionMap.put(AppActionCommands.CMD_VISTA_MOSTRAR_DIALOGO_LISTA, createMostrarDialogoListaAction());
        
        // 3.7. Crear y registrar Actions de Tema
        actionMap.put(AppActionCommands.CMD_TEMA_CLEAR, createToggleThemeAction("clear", "Tema Clear"));
        actionMap.put(AppActionCommands.CMD_TEMA_DARK, createToggleThemeAction("dark", "Tema Dark"));
        actionMap.put(AppActionCommands.CMD_TEMA_BLUE, createToggleThemeAction("blue", "Tema Blue"));
        actionMap.put(AppActionCommands.CMD_TEMA_GREEN, createToggleThemeAction("green", "Tema Green"));
        actionMap.put(AppActionCommands.CMD_TEMA_ORANGE, createToggleThemeAction("orange", "Tema Orange"));

        // 3.8. Crear y registrar Actions de Toggle Generales
        actionMap.put(AppActionCommands.CMD_TOGGLE_SUBCARPETAS, createToggleSubfoldersAction());
        actionMap.put(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA,createSetSubfolderReadModeAction(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA,"Mostrar Solo Carpeta Actual",false));
        actionMap.put(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS, createSetSubfolderReadModeAction(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS, "Mostrar Imágenes de Subcarpetas", true));
        actionMap.put(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES, createToggleProporcionesAction());

        // 3.9. Crear y registrar Actions de Proyecto
        actionMap.put(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA, createToggleMarkImageAction());
        actionMap.put(AppActionCommands.CMD_PROYECTO_GESTIONAR, createGestionarProyectoAction());
        actionMap.put(AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR, createSwitchToVisualizadorAction());
        
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
        actionMap.put(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_1, createSetBackgroundColorAction("clear", "Fondo Tema Claro"));
        actionMap.put(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_2, createSetBackgroundColorAction("dark", "Fondo Tema Oscuro"));
        actionMap.put(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_3, createSetBackgroundColorAction("blue", "Fondo Tema Azul"));
        actionMap.put(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_4, createSetBackgroundColorAction("orange", "Fondo Tema Naranja"));
        actionMap.put(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_5, createSetBackgroundColorAction("green", "Fondo Tema Verde"));
        actionMap.put(AppActionCommands.CMD_BACKGROUND_CHECKERED, createSetCheckeredBackgroundAction("Fondo a Cuadros"));
        actionMap.put(AppActionCommands.CMD_BACKGROUND_CUSTOM_COLOR, createRequestCustomColorAction("Elegir Color..."));
    } // --- FIN del metodo createCoreActions ---
    

	/**
     * Crea las acciones que SÍ dependen de la 'view'.
     */
    private void createViewDependentActions() {
        // Crear la Action genérica para funcionalidades pendientes primero, ya que necesita la 'view'.
//        this.funcionalidadPendienteAction = createFuncionalidadPendienteAction();
//        actionMap.put(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, this.funcionalidadPendienteAction);

        // Action que depende explícitamente de la 'view'.
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP, createToggleAlwaysOnTopAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_MINIATURE_TEXT, createToggleMiniatureTextAction());

        // Actions que dependen del 'actionMap' completo para construir menús emergentes.
//        actionMap.put(AppActionCommands.CMD_ESPECIAL_MENU, createMenuAction());
//        actionMap.put(AppActionCommands.CMD_ESPECIAL_BOTONES_OCULTOS, createHiddenButtonsAction());
    } // --- FIN del metodo createViewDependentActions ---


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
        String claveIcono = this.comandoToIconKeyMap.get(appCommand);
        if (claveIcono != null && !claveIcono.isBlank()) {
        	return this.iconUtils.getScaledIcon(claveIcono, this.iconoAncho, this.iconoAlto);
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
        // Creamos la acción. El nombre "Zoom al Cursor" se usará para el tooltip del botón
        // y el texto del item de menú. Le pasamos el VisorController.
        ToggleZoomToCursorAction action = new ToggleZoomToCursorAction("Zoom al Cursor", this.generalController.getVisorController());
        
        // --- ¡IMPORTANTE! ---
        // Como este es un botón que debe reflejar un estado (ON/OFF), lo registramos
        // como sensible al contexto para futuras actualizaciones.
        this.contextSensitiveActions.add(action);
        
        return action;
    } // --- Fin del método createToggleZoomToCursorAction ---
    
    private Action createAplicarModoZoomAction(String commandKey, String displayName, ZoomModeEnum modo) {
        ImageIcon icon = getIconForCommand(commandKey); 
        return new AplicarModoZoomAction(this.zoomManager, this.model, this.generalController.getVisorController(), displayName, icon, modo, commandKey);
    } // --- Fin del método createAplicarModoZoomAction ---
    
    // --- 4.4. Métodos Create para Actions de Navegación ---
    private Action createFirstImageAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_NAV_PRIMERA);
        return new FirstImageAction(this.listCoordinator, "Primera Imagen", icon);
    } // --- Fin del método createFirstImageAction ---
    
    private Action createPreviousImageAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_NAV_ANTERIOR);
        return new PreviousImageAction(this.listCoordinator, "Imagen Anterior", icon);
    } // --- Fin del método createPreviousImageAction ---
    
    private Action createNextImageAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_NAV_SIGUIENTE);
        return new NextImageAction(this.listCoordinator, "Siguiente Imagen", icon);
    } // --- Fin del método createNextImageAction ---
    
    private Action createLastImageAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_NAV_ULTIMA);
        return new LastImageAction(this.listCoordinator, "Última Imagen", icon);
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
        return new OpenFileAction("Abrir Carpeta...", icon, this.fileOperationsManager);
    } // --- Fin del método createOpenFileAction ---
    
    private Action createDeleteAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_IMAGEN_ELIMINAR);
        return new DeleteAction("Eliminar Imagen", icon, this.fileOperationsManager, this.model);
    } // --- Fin del método createDeleteAction ---
    
    private Action createRefreshAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ESPECIAL_REFRESCAR); 
        return new RefreshAction("Refrescar", icon, this.generalController.getVisorController());
    } // --- Fin del método createRefreshAction ---
    
    private Action createLocateFileAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_IMAGEN_LOCALIZAR);
        LocateFileAction action = new LocateFileAction(this.model, this.generalController.getVisorController(), "Localizar Archivo", icon);
        this.contextSensitiveActions.add(action);
        return action;
    } // --- Fin del método createLocateFileAction ---    

    // --- 4.7. Métodos Create para Actions de Vista (Toggles UI) ---
     
    private Action createToggleMenuBarAction() {
    	return new ToggleMenuBarAction("Barra de Menú", null, this.configuration, this.viewManager, "interfaz.menu.vista.barra_de_menu.seleccionado", "Barra_de_Menu", AppActionCommands.CMD_VISTA_TOGGLE_MENU_BAR);
    } // --- Fin del método createToggleMenuBarAction ---
    
    private Action createToggleToolBarAction() {
        return new ToggleToolBarAction("Barra de Botones", null, this.configuration, this.viewManager, "interfaz.menu.vista.barra_de_botones", "Barra_de_Botones", AppActionCommands.CMD_VISTA_TOGGLE_TOOL_BAR);
    } // --- Fin del método createToggleToolBarAction ---
      
    private Action createToggleFileListAction() {
        return new ToggleFileListAction("Lista de Archivos", null, this.configuration, this.viewManager, "interfaz.menu.vista.mostrar_ocultar_la_lista_de_archivos.seleccionado", "mostrar_ocultar_la_lista_de_archivos", AppActionCommands.CMD_VISTA_TOGGLE_FILE_LIST);
    } // --- Fin del método createToggleFileListAction ---
    
    private Action createToggleThumbnailsAction() {
    	return new ToggleThumbnailsAction( "Barra de Miniaturas", null, this.configuration, this.viewManager, "interfaz.menu.vista.imagenes_en_miniatura.seleccionado", "imagenes_en_miniatura", AppActionCommands.CMD_VISTA_TOGGLE_THUMBNAILS);
   	} // --- Fin del método createToggleThumbnailsAction ---
    
    private Action createToggleLocationBarAction() {
    	return new ToggleUIElementVisibilityAction(
    	        this.viewManager,
    	        this.configuration,
    	        "Barra de Estado",
    	        ConfigKeys.INFOBAR_INF_VISIBLE,
    	        "REFRESH_INFO_BAR_INFERIOR",
    	        AppActionCommands.CMD_VISTA_TOGGLE_LOCATION_BAR
    	    );
    } // --- Fin del método createToggleLocationBarAction ---
    
    private Action createToggleCheckeredBackgroundAction() {
    		return new ToggleCheckeredBackgroundAction("Fondo a Cuadros", null, this.viewManager, this.configuration, "interfaz.menu.vista.fondo_a_cuadros.seleccionado", AppActionCommands.CMD_VISTA_TOGGLE_CHECKERED_BG);
   	} // --- Fin del método createToggleCheckeredBackgroundAction ---
    
    private Action createToggleAlwaysOnTopAction() {
   	    return new ToggleAlwaysOnTopAction("Mantener Ventana Siempre Encima", null, this.view, this.configuration, "interfaz.menu.vista.mantener_ventana_siempre_encima.seleccionado", controlador.commands.AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP);
   	} // --- Fin del método createToggleAlwaysOnTopAction ---
    
    private Action createMostrarDialogoListaAction() {
   	    ImageIcon icon = getIconForCommand(AppActionCommands.CMD_VISTA_MOSTRAR_DIALOGO_LISTA);
   	    return new MostrarDialogoListaAction("Mostrar Lista de Imágenes", icon, this.model, this.generalController.getVisorController());
   	} // --- Fin del método createMostrarDialogoListaAction ---
    
    private Action createToggleMiniatureTextAction() {
    	return new ToggleMiniatureTextAction("Mostrar Nombres en Miniaturas", null, this.configuration, this.view, ConfigKeys.VISTA_MOSTRAR_NOMBRES_MINIATURAS_STATE, AppActionCommands.CMD_VISTA_TOGGLE_MINIATURE_TEXT);
    } // --- Fin del método createToggleMiniatureTextAction ---
    
    private Action createSwitchToVisualizadorAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR); 
        return new SwitchToVisualizadorAction("Visualizador", icon, this.generalController);
    } // --- Fin del método createSwitchToVisualizadorAction ---
   	 	

    // --- 4.8. Métodos Create para Actions de Tema ---
    private Action createToggleThemeAction(String themeNameInternal, String displayNameForMenu) {
        String commandKey;
        switch (themeNameInternal.toLowerCase()) {
            case "clear":  commandKey = AppActionCommands.CMD_TEMA_CLEAR;  break;
            case "dark":   commandKey = AppActionCommands.CMD_TEMA_DARK;   break;
            case "blue":   commandKey = AppActionCommands.CMD_TEMA_BLUE;   break;
            case "green":  commandKey = AppActionCommands.CMD_TEMA_GREEN;  break;
            case "orange": commandKey = AppActionCommands.CMD_TEMA_ORANGE; break;
            default:
                commandKey = AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE;
        }
        return new ToggleThemeAction(this.themeManager, this.generalController.getVisorController(), themeNameInternal, displayNameForMenu, commandKey);
    } // --- Fin del método createToggleThemeAction ---
   
    // --- 4.9. Métodos Create para Actions de Toggle Generales ---
    private Action createToggleSubfoldersAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
        return new ToggleSubfoldersAction("Alternar Subcarpetas", icon, this.configuration, this.model, this.generalController.getVisorController());
    } // --- Fin del método createToggleSubfoldersAction ---
    
    private Action createSetSubfolderReadModeAction(String commandKey, String displayName, boolean representaModoIncluirSubcarpetas) {
    	ImageIcon icon = null; 
    	return new SetSubfolderReadModeAction(displayName, icon, this.generalController.getVisorController(), this.model,representaModoIncluirSubcarpetas, commandKey);
    } // --- Fin del método createSetSubfolderReadModeAction ---
        
    private Action createToggleProporcionesAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);
        return new ToggleProporcionesAction("Mantener Proporciones", icon, this.model, this.generalController.getVisorController());
    } // --- Fin del método createToggleProporcionesAction ---
   
    // --- 4.10. Métodos Create para Actions de Proyecto ---
    private Action createToggleMarkImageAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
        ToggleMarkImageAction action = new ToggleMarkImageAction(this.generalController.getVisorController(), "Marcar/Desmarcar para Proyecto", icon);
        this.contextSensitiveActions.add(action);
        return action;
    } // --- FIN del método createToggleMarkImageAction ---
    
    private Action createGestionarProyectoAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_PROYECTO_GESTIONAR);
        return new GestionarProyectoAction(this.generalController, "Ver Proyecto", icon);
    } // --- Fin del método createGestionarProyectoAction ---
    
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
         
         // --- INICIO DE LA MODIFICACIÓN ---
         // Obtener el icono correspondiente al comando y asignarlo a la acción.
         // Esto es necesario para que los JMenuItems que usen esta acción muestren un icono.
         ImageIcon icon = getIconForCommand(command);
         if (icon != null) {
             action.putValue(Action.SMALL_ICON, icon);
         }
         // --- FIN DE LA MODIFICACIÓN ---
         
         this.contextSensitiveActions.add(action);
         return action;
     } // --- Fin del método createPanActionIncremental ---

     // --- 4.13. Métodos Create para Actions de Control de Fondo ---
     private Action createSetBackgroundColorAction(String themeKey, String name) {
         return new AbstractAction(name) {
             private static final long serialVersionUID = 1L;
             @Override
             public void actionPerformed(ActionEvent e) {
                 if (viewManager != null && themeManager != null) {
                     java.awt.Color color = themeManager.getFondoSecundarioParaTema(themeKey);
                     viewManager.setSessionBackgroundColor(color);
                 }
             }
         };
     } // --- Fin del método createSetBackgroundColorAction ---
     
     private Action createSetCheckeredBackgroundAction(String name) {
         return new AbstractAction(name) {
             private static final long serialVersionUID = 1L;
             @Override
             public void actionPerformed(ActionEvent e) {
                 if (viewManager != null) {
                     viewManager.setSessionCheckeredBackground();
                 }
             }
         };
     } // --- Fin del método createSetCheckeredBackgroundAction ---
     
     private Action createRequestCustomColorAction(String name) {
         return new AbstractAction(name) {
             private static final long serialVersionUID = 1L;
             @Override
             public void actionPerformed(ActionEvent e) {
                 if (viewManager != null) {
                     viewManager.requestCustomBackgroundColor();
                 }
             }
         };
     } // --- Fin del método createRequestCustomColorAction ---
     
    // --- SECCIÓN 5: GETTERS Y SETTERS ---
    public Map<String, Action> getActionMap() {
         return this.actionMap; 
    } // --- Fin del método getActionMap ---
     
    public List<ContextSensitiveAction> getContextSensitiveActions() {
        return Collections.unmodifiableList(this.contextSensitiveActions);
    } // --- Fin del método getContextSensitiveActions ---
    
    public Map<String, String> getComandoToIconKeyMap() {return Collections.unmodifiableMap(this.comandoToIconKeyMap);}
    public int getIconoAncho() { return this.iconoAncho;}
    public int getIconoAlto() { return this.iconoAlto;}
    
    public void registerAction(String commandKey, Action action) {
        if (this.actionMap.containsKey(commandKey)) {
            System.out.println("WARN [ActionFactory]: Reemplazando Action existente para el comando: " + commandKey);
        }
        this.actionMap.put(commandKey, action);
        System.out.println("  [ActionFactory] Action registrada/actualizada para comando: " + commandKey);
    } // --- Fin del método registerAction ---
    
    
    /**
     * Itera sobre todas las acciones creadas en el actionMap y actualiza sus iconos
     * basándose en el tema actualmente activo en el ThemeManager.
     * Esto es crucial para el cambio de tema en caliente, ya que los iconos se
     * almacenan como una propiedad dentro de cada objeto Action.
     */
    public void actualizarIconosDeAcciones() {
        System.out.println("[ActionFactory] Iniciando actualización de iconos para todas las acciones...");
        if (actionMap == null || actionMap.isEmpty()) {
            System.out.println("  -> No hay acciones en el mapa para actualizar. Proceso omitido.");
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
            if (nuevoIcono != null) {
                // Actualizamos la propiedad Action.SMALL_ICON de la Action.
                // Los componentes de Swing (JButton, JMenuItem) que usan esta Action
                // escucharán este cambio de propiedad y actualizarán su icono automáticamente.
                action.putValue(Action.SMALL_ICON, nuevoIcono);
                iconosActualizados++;
            }
        }
        System.out.println("[ActionFactory] Actualización de iconos completada. Se actualizaron " + iconosActualizados + " acciones con un nuevo icono.");
    } // --- Fin del método actualizarIconosDeAcciones ---
    
    
    public void setView(VisorView view) { this.view = view; }
    public void setZoomManager(IZoomManager zoomManager) { this.zoomManager = zoomManager; }
    public void setViewManager(IViewManager viewManager) {this.viewManager = Objects.requireNonNull(viewManager);}
    public void setEditionManager(IEditionManager editionManager) {this.editionManager = Objects.requireNonNull(editionManager);}
    public void setListCoordinator(IListCoordinator listCoordinator) {this.listCoordinator = Objects.requireNonNull(listCoordinator);}
    public void setFileOperationsManager(FileOperationsManager fileOperationsManager) {
        this.fileOperationsManager = Objects.requireNonNull(fileOperationsManager, "FileOperationsManager no puede ser null en setFileOperationsManager");
    } // --- Fin del método setFileOperationsManager ---
    
}	// --- FIN de la clase ActionFactory ---


