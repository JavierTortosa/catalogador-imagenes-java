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

// --- SECCIÓN 0.1: IMPORTS DE COMPONENTES DEL SISTEMA ---

import controlador.VisorController;
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
    private final VisorController controllerRef;
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

    
    // --- SECCIÓN 2: CONSTRUCTOR ---
    /**
     * Constructor de ActionFactory.
     * Recibe todas las dependencias necesarias para crear las Actions.
     * Llama a initializeAllActions para poblar el actionMap.
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
            VisorController controller
    ){ 
        
        // 2.1. Asignar dependencias principales.
        this.model 						= Objects.requireNonNull(model, "VisorModel no puede ser null en ActionFactory");
        this.view =view;//						= Objects.requireNonNull(view, "VisorView no puede ser null en ActionFactory");
        this.configuration 				= Objects.requireNonNull(configuration, "ConfigurationManager no puede ser null en ActionFactory");
        this.iconUtils 					= Objects.requireNonNull(iconUtils, "IconUtils no puede ser null en ActionFactory");
        this.controllerRef 				= Objects.requireNonNull(controller, "VisorController (controllerRef) no puede ser null en ActionFactory");
        this.themeManager 				= Objects.requireNonNull(themeManager, "ThemeManager no puede ser null en ActionFactory");
        
        // 2.2. Asignar Managers y Coordinadores.
        this.zoomManager = zoomManager;//				= Objects.requireNonNull(zoomManager, "ZoomManager no puede ser null en ActionFactory");
        this.fileOperationsManager = fileOperationsManager;// 		= Objects.requireNonNull(fileOperationsManager, "FileManager no puede ser null");
        this.viewManager = viewManager;	//			= Objects.requireNonNull(viewManager, "ViewManager no puede ser null");
        this.editionManager	= editionManager;//			= Objects.requireNonNull(editionManager, "ViewManager no puede ser null");
        this.contextSensitiveActions 	= new ArrayList<>(); 
        
        // this.editionManager = Objects.requireNonNull(editionManager, "EditionManager no puede ser null");
        this.listCoordinator 			= Objects.requireNonNull(listCoordinator, "IListCoordinator no puede ser null en ActionFactory");
        this.projectService 			= Objects.requireNonNull(projectService, "IProjectManager (servicio) no puede ser null");
        
        // 2.3. Asignar mapa de iconos.
        this.comandoToIconKeyMap 		= Objects.requireNonNull(comandoToIconKeyMap, "comandoToIconKeyMap no puede ser null");
        

        // 2.4. Leer dimensiones de iconos desde la configuración.
        this.iconoAncho 				= configuration.getInt("iconos.ancho", 24);
        this.iconoAlto 					= configuration.getInt("iconos.alto", 24); // Usar el mismo valor o -1 si quieres proporción basada en ancho

        // 2.5. Inicializar el mapa interno de acciones.
        this.actionMap 					= new HashMap<>();

        // 2.6. Crear e inicializar todas las Actions.
        System.out.println("  [ActionFactory] Inicializando todas las Actions...");
        System.out.println("  [ActionFactory] Todas las Actions inicializadas y mapeadas. Total: " + this.actionMap.size());
    }


    public void initializeActions() {
        System.out.println("  [ActionFactory] Inicializando todas las Actions...");
        if (view == null || zoomManager == null || fileOperationsManager == null ||
                editionManager == null || listCoordinator == null || viewManager == null) {
            throw new IllegalStateException("No se pueden inicializar las acciones porque las dependencias tardías (view, zoomManager, etc.) no han sido inyectadas.");
        }
        initializeAllActions(); // Llama al método privado que ya tienes
        System.out.println("  [ActionFactory] Todas las Actions inicializadas y mapeadas. Total: " + this.actionMap.size());
    }
    
    
    // --- SECCIÓN 3: MÉTODO PRINCIPAL DE INICIALIZACIÓN DE ACTIONS ---
    /**
     * Crea instancias de todas las Actions de la aplicación y las registra en el actionMap.
     * Se llama desde el constructor de ActionFactory.
     * El orden puede ser importante si hay dependencias entre Actions (ej. ToggleZoomManual necesita ResetZoom).
     */
    private void initializeAllActions() {
    	
    	boolean estadoConfigActualIncluyeSubcarpetas = this.configuration.getBoolean("comportamiento.carpeta.cargarSubcarpetas",true);
    	
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
        
        // 3.3. Crear y registrar Actions de Navegación
        actionMap.put(AppActionCommands.CMD_NAV_PRIMERA, createFirstImageAction());
        actionMap.put(AppActionCommands.CMD_NAV_ANTERIOR, createPreviousImageAction());
        actionMap.put(AppActionCommands.CMD_NAV_SIGUIENTE, createNextImageAction());
        actionMap.put(AppActionCommands.CMD_NAV_ULTIMA, createLastImageAction());
        actionMap.put(AppActionCommands.CMD_TOGGLE_WRAP_AROUND, createToggleNavegacionCircularAction());
        
        // 3.4. Crear y registrar Actions de Edición (usando funcionalidadPendiente por ahora)
        // Cuando EditionManager esté listo, se crearán las actions reales aquí.
        actionMap.put(AppActionCommands.CMD_IMAGEN_ROTAR_IZQ, createRotateLeftAction());
        actionMap.put(AppActionCommands.CMD_IMAGEN_ROTAR_DER, createRotateRightAction());
        actionMap.put(AppActionCommands.CMD_IMAGEN_VOLTEAR_H, createFlipHorizontalAction());
        actionMap.put(AppActionCommands.CMD_IMAGEN_VOLTEAR_V, createFlipVerticalAction());
        actionMap.put(AppActionCommands.CMD_IMAGEN_RECORTAR, createCropAction());
        actionMap.put(AppActionCommands.CMD_IMAGEN_LOCALIZAR, createLocateFileAction());
        
        // 3.5. Crear y registrar Actions de Archivo y relacionadas
        actionMap.put(AppActionCommands.CMD_ARCHIVO_ABRIR, createOpenFileAction()); // Requiere FileOperationsManager o lógica interna
        actionMap.put(AppActionCommands.CMD_IMAGEN_ELIMINAR, createDeleteAction());   // Requiere FileOperationsManager
        actionMap.put(AppActionCommands.CMD_ESPECIAL_REFRESCAR, createRefreshAction()); // Puede necesitar recargar lista

        // 3.6. Crear y registrar Actions de Vista (Toggles de UI)
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_MENU_BAR, createToggleMenuBarAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_TOOL_BAR, createToggleToolBarAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_FILE_LIST, createToggleFileListAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_THUMBNAILS, createToggleThumbnailsAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_LOCATION_BAR, createToggleLocationBarAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_CHECKERED_BG, createToggleCheckeredBackgroundAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP, createToggleAlwaysOnTopAction());
        actionMap.put(AppActionCommands.CMD_VISTA_MOSTRAR_DIALOGO_LISTA, createMostrarDialogoListaAction());
        actionMap.put(AppActionCommands.CMD_VISTA_TOGGLE_MINIATURE_TEXT, createToggleMiniatureTextAction());
        
        

        // 3.7. Crear y registrar Actions de Tema
        actionMap.put(AppActionCommands.CMD_TEMA_CLEAR, createToggleThemeAction("clear", "Tema Clear"));
        actionMap.put(AppActionCommands.CMD_TEMA_DARK, createToggleThemeAction("dark", "Tema Dark"));
        actionMap.put(AppActionCommands.CMD_TEMA_BLUE, createToggleThemeAction("blue", "Tema Blue"));
        actionMap.put(AppActionCommands.CMD_TEMA_GREEN, createToggleThemeAction("green", "Tema Green"));
        actionMap.put(AppActionCommands.CMD_TEMA_ORANGE, createToggleThemeAction("orange", "Tema Orange"));
        // El estado SELECTED de estas actions lo maneja ToggleThemeAction internamente.

        // 3.8. Crear y registrar Actions de Toggle Generales
        
        
        actionMap.put(AppActionCommands.CMD_TOGGLE_SUBCARPETAS, createToggleSubfoldersAction());
        
        boolean estadoModeloActualIncluyeSubcarpetas = !this.model.isMostrarSoloCarpetaActual();
        actionMap.put(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA,createSetSubfolderReadModeAction(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA,"Mostrar Solo Carpeta Actual",false));//, estadoModeloActualIncluyeSubcarpetas));
        actionMap.put(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS, createSetSubfolderReadModeAction(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS, "Mostrar Imágenes de Subcarpetas", true));//, estadoModeloActualIncluyeSubcarpetas));
        actionMap.put(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES, createToggleProporcionesAction());

        // 3.9. Crear y registrar Actions de Proyecto
        actionMap.put(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA, createToggleMarkImageAction());
        actionMap.put(AppActionCommands.CMD_PROYECTO_GESTIONAR, createGestionarProyectoAction());
        actionMap.put(AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR, createSwitchToVisualizadorAction());
        
        // 3.10. Crear y registrar Actions Especiales
        actionMap.put(AppActionCommands.CMD_ESPECIAL_MENU, createMenuAction());
        actionMap.put(AppActionCommands.CMD_ESPECIAL_BOTONES_OCULTOS, createHiddenButtonsAction());
        
     // BARRA DE INFORMACION SUPERIOR
        
        // --- ACCIONES PARA CONFIGURAR VISIBILIDAD DE ELEMENTOS EN LA BARRA DE INFORMACIÓN SUPERIOR ---

        // Visibilidad del PANEL COMPLETO de la Barra Superior
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_VISIBLE,
            new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Barra de Información Superior",
            		ConfigKeys.INFOBAR_SUP_VISIBLE, "REFRESH_INFO_BAR_SUPERIOR", 
                AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_VISIBLE));

        // Visibilidad del componente Nombre/Ruta en la Barra Superior
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_NOMBRE_RUTA,
            new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Nombre/Ruta Archivo (Sup.)",
            		ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_VISIBLE,"REFRESH_INFO_BAR_SUPERIOR", 
                AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_NOMBRE_RUTA
            ));

        // Visibilidad del componente Índice/Total en la Barra Superior
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_INDICE_TOTAL,
            new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Índice/Total Imágenes",
            		ConfigKeys.INFOBAR_SUP_INDICE_TOTAL_VISIBLE,"REFRESH_INFO_BAR_SUPERIOR",
                AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_INDICE_TOTAL
            ));

        // Visibilidad del componente Dimensiones en la Barra Superior
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_DIMENSIONES,
            new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Dimensiones Originales",
            		ConfigKeys.INFOBAR_SUP_DIMENSIONES_VISIBLE, "REFRESH_INFO_BAR_SUPERIOR",
                AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_DIMENSIONES
            ));

        // Visibilidad del componente Tamaño de Archivo en la Barra Superior
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_TAMANO_ARCHIVO,
            new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Tamaño de Archivo",
            		ConfigKeys.INFOBAR_SUP_TAMANO_ARCHIVO_VISIBLE, "REFRESH_INFO_BAR_SUPERIOR",
                AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_TAMANO_ARCHIVO
            ));

        // Visibilidad del componente Fecha de Archivo en la Barra Superior
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_FECHA_ARCHIVO,
            new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Fecha de Archivo",
            		ConfigKeys.INFOBAR_SUP_FECHA_ARCHIVO_VISIBLE,"REFRESH_INFO_BAR_SUPERIOR",
                AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_FECHA_ARCHIVO
            ));

        // Visibilidad del componente Formato de Imagen en la Barra Superior
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_FORMATO_IMAGEN,
            new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Formato de Imagen",
            		ConfigKeys.INFOBAR_SUP_FORMATO_IMAGEN_VISIBLE,"REFRESH_INFO_BAR_SUPERIOR",
                AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_FORMATO_IMAGEN
            ));

        // Visibilidad del componente Modo de Zoom en la Barra Superior
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_MODO_ZOOM,
            new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Modo de Zoom",
            		ConfigKeys.INFOBAR_SUP_MODO_ZOOM_VISIBLE,"REFRESH_INFO_BAR_SUPERIOR",
                AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_MODO_ZOOM
            ));

        // Visibilidad del componente % Zoom Real en la Barra Superior
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_ZOOM_REAL_PCT,
            new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar % Zoom Real",
            		ConfigKeys.INFOBAR_SUP_ZOOM_REAL_PCT_VISIBLE,"REFRESH_INFO_BAR_SUPERIOR",
                AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_ZOOM_REAL_PCT
            ));

     // --- ACCIONES PARA CONFIGURAR FORMATO DE TEXTO EN BARRA SUPERIOR (RADIO BUTTONS) ---
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_SOLO_NOMBRE,
            new SetInfoBarTextFormatAction(
                this.viewManager, this.configuration, "Solo Nombre de Archivo", // Texto del JRadioButtonMenuItem
                ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_FORMATO, // Clave donde se guarda "solo_nombre" o "ruta_completa"
                "solo_nombre", // Valor que esta Action establece
                "REFRESH_INFO_BAR_SUPERIOR", // Zona a refrescar
                AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_SOLO_NOMBRE // Comando de esta Action
            ));
        actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_RUTA_COMPLETA,
            new SetInfoBarTextFormatAction(
                this.viewManager, this.configuration, "Ruta Completa y Nombre",
                ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_FORMATO,
                "ruta_completa",
                "REFRESH_INFO_BAR_SUPERIOR",
                AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_RUTA_COMPLETA
            ));

     // --- BARRA DE INFORMACIÓN INFERIOR ---

     // Visibilidad del PANEL COMPLETO de la Barra Inferior
     actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_VISIBLE,
         new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Barra de Estado/Control Inferior",
             ConfigKeys.INFOBAR_INF_VISIBLE, "REFRESH_INFO_BAR_INFERIOR", 
             AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_VISIBLE
         ));

     // Visibilidad del componente Nombre/Ruta en la Barra Inferior
     actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_NOMBRE_RUTA,
         new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Nombre/Ruta Archivo (Inf.)",
        		 ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_VISIBLE,"REFRESH_INFO_BAR_INFERIOR",
             AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_NOMBRE_RUTA
         ));

     // Visibilidad del Icono Zoom Manual en la Barra Inferior
     actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_ZM,
         new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Icono Zoom Manual (Inf.)",
        		 ConfigKeys.INFOBAR_INF_ICONO_ZM_VISIBLE,"REFRESH_INFO_BAR_INFERIOR",
             AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_ZM
         ));

     // Visibilidad del Icono Proporciones en la Barra Inferior
     actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_PROP,
         new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Icono Proporciones (Inf.)",
        		 ConfigKeys.INFOBAR_INF_ICONO_PROP_VISIBLE,"REFRESH_INFO_BAR_INFERIOR",
             AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_PROP
         ));

     // Visibilidad del Icono Subcarpetas en la Barra Inferior
     actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_SUBC,
         new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Icono Subcarpetas (Inf.)",
        		 ConfigKeys.INFOBAR_INF_ICONO_SUBC_VISIBLE,"REFRESH_INFO_BAR_INFERIOR",
             AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_SUBC
         ));

     // Visibilidad del Control % Zoom en la Barra Inferior
     actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_CTRL_ZOOM_PCT,
         new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Control % Zoom (Inf.)",
        		 ConfigKeys.INFOBAR_INF_CTRL_ZOOM_PCT_VISIBLE, "REFRESH_INFO_BAR_INFERIOR",
             AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_CTRL_ZOOM_PCT
         ));

     // Visibilidad del Control Modo Zoom en la Barra Inferior
     actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_CTRL_MODO_ZOOM,
         new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Control Modo Zoom (Inf.)",
        		 ConfigKeys.INFOBAR_INF_CTRL_MODO_ZOOM_VISIBLE,"REFRESH_INFO_BAR_INFERIOR",
             AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_CTRL_MODO_ZOOM
         ));

     // Visibilidad de Mensajes Aplicación en la Barra Inferior
     actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_MENSAJES_APP,
         new ToggleUIElementVisibilityAction(this.viewManager, this.configuration, "Mostrar Área de Mensajes (Inf.)",
        		 ConfigKeys.INFOBAR_INF_MENSAJES_APP_VISIBLE,"REFRESH_INFO_BAR_INFERIOR",
             AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_MENSAJES_APP
         ));

  // --- ACCIONES PARA CONFIGURAR FORMATO DE TEXTO EN BARRA INFERIOR (RADIO BUTTONS) ---
     actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_SOLO_NOMBRE,
         new SetInfoBarTextFormatAction(
             this.viewManager, this.configuration, "Solo Nombre de Archivo",
             ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_FORMATO,
             "solo_nombre",
             "REFRESH_INFO_BAR_INFERIOR",
             AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_SOLO_NOMBRE
         ));
     actionMap.put(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_RUTA_COMPLETA,
         new SetInfoBarTextFormatAction(
             this.viewManager, this.configuration, "Ruta Completa y Nombre",
             ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_FORMATO,
             "ruta_completa",
             "REFRESH_INFO_BAR_INFERIOR",
             AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_RUTA_COMPLETA
         ));

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

                 // Usamos la variable 'view' directamente como el componente padre.
                 JOptionPane.showMessageDialog(view, mensaje, "En Desarrollo", JOptionPane.INFORMATION_MESSAGE);
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
        // System.err.println("WARN [ActionFactory]: No se encontró clave de icono en comandoToIconKeyMap para el comando: " + appCommand + ". No se asignará icono a la Action.");
        return null; // Devolver null si no hay icono definido para este comando
    }

    // --- 4.3. Métodos Create para Actions de Zoom ---
    private Action createToggleZoomManualAction() { 
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
        return new ToggleZoomManualAction("Activar Zoom Manual", icon, this.zoomManager, this.controllerRef, this.model);
    }
    private Action createResetZoomAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ZOOM_RESET);
        return new ResetZoomAction("Resetear Zoom", icon, this.zoomManager);
    }
    private Action createAplicarModoZoomAction(String commandKey, String displayName, ZoomModeEnum modo) {
        ImageIcon icon = getIconForCommand(commandKey); 
        // Pasamos this.model a AplicarModoZoomAction
        return new AplicarModoZoomAction(this.zoomManager, this.model, this.controllerRef, displayName, icon, modo, commandKey);
    }
    
    // --- 4.4. Métodos Create para Actions de Navegación ---
    private Action createFirstImageAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_NAV_PRIMERA);
        return new FirstImageAction(this.listCoordinator, "Primera Imagen", icon);
    }
    private Action createPreviousImageAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_NAV_ANTERIOR);
        return new PreviousImageAction(this.listCoordinator, "Imagen Anterior", icon);
    }
    private Action createNextImageAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_NAV_SIGUIENTE);
        return new NextImageAction(this.listCoordinator, "Siguiente Imagen", icon);
    }
    private Action createLastImageAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_NAV_ULTIMA);
        return new LastImageAction(this.listCoordinator, "Última Imagen", icon);
    }
    private Action createToggleNavegacionCircularAction() {
        return new ToggleNavegacionCircularAction("Alternar Navegación Circular", null, this.configuration, this.model, this.controllerRef, ConfigKeys.COMPORTAMIENTO_NAVEGACION_CIRCULAR,AppActionCommands.CMD_TOGGLE_WRAP_AROUND);
    }


    // --- 4.5. Métodos Create para Actions de Edición (usarán EditionManager) ---
    // Ejemplo (cuando EditionManager esté listo):
    // private Action createRotateLeftAction() {
    //     ImageIcon icon = getIconForCommand(AppActionCommands.CMD_IMAGEN_ROTAR_IZQ);
    //     return new RotateLeftAction(this.editionManager, "Girar Izquierda", icon); 
    // }
    // Por ahora, se usa funcionalidadPendienteAction.
    private Action createRotateRightAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_IMAGEN_ROTAR_DER);
        RotateRightAction action = new RotateRightAction(this.editionManager, this.model, "Girar Derecha", icon);
        this.contextSensitiveActions.add(action); 
        return action;
    }
    private Action createRotateLeftAction() {
    	ImageIcon icon = getIconForCommand(AppActionCommands.CMD_IMAGEN_ROTAR_IZQ);
        RotateLeftAction action = new RotateLeftAction(this.editionManager, this.model, "Girar Izquierda", icon);
        this.contextSensitiveActions.add(action); 
        return action;
    }
    private Action createFlipHorizontalAction() {
    	ImageIcon icon = getIconForCommand(AppActionCommands.CMD_IMAGEN_VOLTEAR_H);
    	FlipHorizontalAction action = new FlipHorizontalAction(this.editionManager, this.model, "Voltear Horizontal", icon);
        this.contextSensitiveActions.add(action); 
        return action;
    }
    private Action createFlipVerticalAction() {
    	ImageIcon icon = getIconForCommand(AppActionCommands.CMD_IMAGEN_VOLTEAR_V);
    	FlipVerticalAction action = new FlipVerticalAction(this.editionManager, this.model, "Voltear Vertical", icon);
        this.contextSensitiveActions.add(action); 
        return action;
    }
    private Action createCropAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_IMAGEN_RECORTAR);
        CropAction action = new CropAction(this.editionManager, this.model, "Recortar", icon);
        
        this.contextSensitiveActions.add(action); // Registrarla
        return action;
    } // --- Fin del método createCropAction ---

    // --- 4.6. Métodos Create para Actions de Archivo (usarán FileOperationsManager o lógica interna) ---
    private Action createOpenFileAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ARCHIVO_ABRIR);
        return new OpenFileAction("Abrir Carpeta...", icon, this.fileOperationsManager);
    }
    private Action createDeleteAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_IMAGEN_ELIMINAR);
        return new DeleteAction("Eliminar Imagen", icon, this.fileOperationsManager, this.model);
    }
    private Action createRefreshAction() {
        // 1. Obtener el icono usando el comando canónico
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ESPECIAL_REFRESCAR); 
        return new RefreshAction("Refrescar", icon, this.controllerRef);
    }
    private Action createLocateFileAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_IMAGEN_LOCALIZAR);
        LocateFileAction action = new LocateFileAction(this.model, this.controllerRef, "Localizar Archivo", icon);
        this.contextSensitiveActions.add(action); // <<< REGISTRAR LA ACCIÓN
        return action;
    }    

    // --- 4.7. Métodos Create para Actions de Vista (Toggles UI) ---
    // (Estas actions suelen tomar VisorView y ConfigurationManager)
     
    private Action createToggleMenuBarAction() {
    	return new ToggleMenuBarAction("Barra de Menú", null, this.configuration, this.viewManager, "interfaz.menu.vista.barra_de_menu.seleccionado", "Barra_de_Menu", AppActionCommands.CMD_VISTA_TOGGLE_MENU_BAR);
    }
    private Action createToggleToolBarAction() {
        return new ToggleToolBarAction("Barra de Botones", null, this.configuration, this.viewManager, "interfaz.menu.vista.barra_de_botones", "Barra_de_Botones", AppActionCommands.CMD_VISTA_TOGGLE_TOOL_BAR);
    }  
    private Action createToggleFileListAction() {
        return new ToggleFileListAction("Lista de Archivos", null, this.configuration, this.viewManager, "interfaz.menu.vista.mostrar_ocultar_la_lista_de_archivos.seleccionado", "mostrar_ocultar_la_lista_de_archivos", AppActionCommands.CMD_VISTA_TOGGLE_FILE_LIST);
    }
    private Action createToggleThumbnailsAction() {
   	    //return new ToggleThumbnailsAction("Imagenes en Miniatura", null, this.viewManager, this.configuration, "interfaz.menu.vista.imagenes_en_miniatura.seleccionado", "imagenes_en_miniatura", AppActionCommands.CMD_VISTA_TOGGLE_THUMBNAILS);
    	return new ToggleThumbnailsAction( "Barra de Miniaturas", null, this.configuration, this.viewManager, "interfaz.menu.vista.imagenes_en_miniatura.seleccionado", "imagenes_en_miniatura", AppActionCommands.CMD_VISTA_TOGGLE_THUMBNAILS);
   	}
    private Action createToggleLocationBarAction() {
//        return new ToggleLocationBarAction("Linea de Ubicacion del Archivo", null, this.configuration, this.controllerRef, ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_VISIBLE, "REFRESH_INFO_BAR_INFERIOR", AppActionCommands.CMD_VISTA_TOGGLE_LOCATION_BAR);
    	return new ToggleUIElementVisibilityAction( // <<< Usamos la Action genérica
    	        this.viewManager,
    	        this.configuration,
    	        "Barra de Estado", // <-- Texto más apropiado para el menú
    	        ConfigKeys.INFOBAR_INF_VISIBLE, // <<< Clave que controla TODA la barra
    	        "REFRESH_INFO_BAR_INFERIOR", // <<< Identificador para el refresco
    	        AppActionCommands.CMD_VISTA_TOGGLE_LOCATION_BAR // Comando
    	    );
    }
    private Action createToggleCheckeredBackgroundAction() {
//   	    return new ToggleCheckeredBackgroundAction("Fondo a Cuadros", null, this.view, this.configuration, "interfaz.menu.vista.fondo_a_cuadros.seleccionado", AppActionCommands.CMD_VISTA_TOGGLE_CHECKERED_BG);
    		return new ToggleCheckeredBackgroundAction("Fondo a Cuadros", null, this.viewManager, this.configuration, "interfaz.menu.vista.fondo_a_cuadros.seleccionado", AppActionCommands.CMD_VISTA_TOGGLE_CHECKERED_BG);
   	}
    private Action createToggleAlwaysOnTopAction() {
   	    return new ToggleAlwaysOnTopAction("Mantener Ventana Siempre Encima", null, this.view, this.configuration, "interfaz.menu.vista.mantener_ventana_siempre_encima.seleccionado", controlador.commands.AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP);
   	}
    private Action createMostrarDialogoListaAction() {
   	    ImageIcon icon = getIconForCommand(AppActionCommands.CMD_VISTA_MOSTRAR_DIALOGO_LISTA);
   	    return new MostrarDialogoListaAction("Mostrar Lista de Imágenes", icon, this.model, this.controllerRef);
   	}
    private Action createToggleMiniatureTextAction() {
    	return new ToggleMiniatureTextAction("Mostrar Nombres en Miniaturas", null, this.configuration, this.view, ConfigKeys.VISTA_MOSTRAR_NOMBRES_MINIATURAS_STATE, AppActionCommands.CMD_VISTA_TOGGLE_MINIATURE_TEXT);
    }
    private Action createSwitchToVisualizadorAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR); 
        return new SwitchToVisualizadorAction("Visualizador", icon, this.viewManager);
    }
   	 	

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
                System.err.println("WARN [ActionFactory]: Nombre de tema interno no reconocido para ActionCommandKey: " + themeNameInternal);
                commandKey = AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE;
        }
        return new ToggleThemeAction(this.themeManager, this.controllerRef, themeNameInternal, displayNameForMenu, commandKey);
    }
   
    // --- 4.9. Métodos Create para Actions de Toggle Generales ---
    private Action createToggleSubfoldersAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
        ToggleSubfoldersAction action = new ToggleSubfoldersAction("Alternar Subcarpetas", icon, this.configuration, this.model, this.controllerRef);
        System.out.println("ActionFactory: Creando ToggleSubfoldersAction@" + Integer.toHexString(System.identityHashCode(action))); // Log de la instancia creada
        return action;
    }
    private Action createSetSubfolderReadModeAction(String commandKey, String displayName, boolean representaModoIncluirSubcarpetas) {
    	ImageIcon icon = null; 
    	return new SetSubfolderReadModeAction(displayName, icon, this.controllerRef, this.model,representaModoIncluirSubcarpetas, commandKey);
    }    
    private Action createToggleProporcionesAction() {
    ImageIcon icon = getIconForCommand(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);
    return new ToggleProporcionesAction("Mantener Proporciones", icon, this.model, this.controllerRef);
    }
   
    // --- 4.10. Métodos Create para Actions de Proyecto ---
    private Action createToggleMarkImageAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
        ToggleMarkImageAction action = new ToggleMarkImageAction(this.controllerRef, "Marcar/Desmarcar para Proyecto", icon);
        this.contextSensitiveActions.add(action);
        return action;
    } // --- FIN del método createToggleMarkImageAction ---
    private Action createGestionarProyectoAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_PROYECTO_GESTIONAR);
        return new GestionarProyectoAction(this.projectService, this.viewManager, "Gestionar Proyectos", icon);
    }
    
    // --- 4.11. Métodos Create para Actions Especiales ---
     private Action createMenuAction() { // Ya no necesita toggleMenuBarActionInstance como parámetro directo
         ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ESPECIAL_MENU);
         UIDefinitionService uiDefService = new UIDefinitionService();
         List<MenuItemDefinition> fullMenuStructure = uiDefService.generateMenuStructure();
         return new MenuAction("Menú Principal", icon, fullMenuStructure, this.actionMap, this.themeManager, this.configuration, this.controllerRef);
     }
     private Action createHiddenButtonsAction() {
         ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ESPECIAL_BOTONES_OCULTOS);
         return new HiddenButtonsAction("Más Opciones", icon, this.actionMap, this.themeManager, this.configuration, this.controllerRef );
     }

    // --- SECCIÓN 5: GETTER PARA EL ACTIONMAP ---
    /**
     * Devuelve el mapa de acciones construido.
     * @return El mapa donde la clave es el String del comando (de AppActionCommands)
     *         y el valor es la instancia de Action correspondiente.
     */
     public Map<String, Action> getActionMap() {
         return this.actionMap; 
     }
    public List<ContextSensitiveAction> getContextSensitiveActions() {
        return Collections.unmodifiableList(this.contextSensitiveActions);
    }
    
    // Getters para las dimensiones de los iconos
    public Map<String, String> getComandoToIconKeyMap() {return Collections.unmodifiableMap(this.comandoToIconKeyMap);}
    public int getIconoAncho() { return this.iconoAncho;}
    public int getIconoAlto() { return this.iconoAlto;}
    
    public void registerAction(String commandKey, Action action) {
        if (this.actionMap.containsKey(commandKey)) {
            System.out.println("WARN [ActionFactory]: Reemplazando Action existente para el comando: " + commandKey);
        }
        this.actionMap.put(commandKey, action); // Modifica el mapa interno mutable
        System.out.println("  [ActionFactory] Action registrada/actualizada para comando: " + commandKey);
    }
    
    
    public void setView(VisorView view) { this.view = view; }
    public void setZoomManager(IZoomManager zoomManager) { this.zoomManager = zoomManager; }
    public void setViewManager(IViewManager viewManager) {this.viewManager = Objects.requireNonNull(viewManager);}
    public void setEditionManager(IEditionManager editionManager) {this.editionManager = Objects.requireNonNull(editionManager);}
    public void setListCoordinator(IListCoordinator listCoordinator) {this.listCoordinator = Objects.requireNonNull(listCoordinator);}
    public void setFileOperationsManager(FileOperationsManager fileOperationsManager) {
        this.fileOperationsManager = Objects.requireNonNull(fileOperationsManager, "FileOperationsManager no puede ser null en setFileOperationsManager");
    }
    
}	// --- FIN de la clase ActionFactory