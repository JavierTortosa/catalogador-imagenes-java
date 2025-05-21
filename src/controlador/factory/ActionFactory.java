// En src/controlador/factory/ActionFactory.java
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
import controlador.ListCoordinator;
import controlador.VisorController;
import controlador.actions.archivo.DeleteAction;
// --- SECCIÓN 0: IMPORTS DE CLASES ACTION ESPECCÍFICAS ---
// (Asegúrate de que estas clases Action tengan los constructores correctos)
import controlador.actions.archivo.OpenFileAction;
import controlador.actions.archivo.RefreshAction;
import controlador.actions.config.SetSubfolderReadModeAction;
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
import controlador.actions.toggle.ToggleProporcionesAction;
import controlador.actions.toggle.ToggleSubfoldersAction;
import controlador.actions.vista.MostrarDialogoListaAction;
import controlador.actions.vista.ToggleAlwaysOnTopAction;
import controlador.actions.vista.ToggleCheckeredBackgroundAction;
import controlador.actions.vista.ToggleFileListAction;
import controlador.actions.vista.ToggleLocationBarAction;
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
import controlador.managers.EditionManager;
import controlador.managers.FileOperationsManager; // Futuro
import controlador.managers.ViewManager;
import controlador.managers.ZoomManager;
// import controlador.managers.ViewUIManager; // Futuro
// import controlador.managers.ProjectActionsManager; // Futuro
import modelo.VisorModel;
import servicios.ConfigurationManager;
import servicios.ProjectManager; // El servicio de persistencia de proyectos
import servicios.zoom.ZoomModeEnum;
import vista.VisorView;
import vista.config.MenuItemDefinition;
import vista.config.UIDefinitionService;
import vista.config.ViewUIConfig;
import vista.theme.ThemeManager;
import vista.util.IconUtils;


public class ActionFactory {

    // --- SECCIÓN 1: CAMPOS DE INSTANCIA (DEPENDENCIAS INYECTADAS) ---
    // 1.1. Referencias a componentes principales
    private final VisorModel model;
    private final VisorView view;
    private final ConfigurationManager configuration;
    private final IconUtils iconUtils;
    
    // 1.2. Referencias a Managers y Coordinadores
    private final ZoomManager zoomManager;
    private final FileOperationsManager fileOperationsManager;
    private final ViewManager viewManager;
    private final VisorController controllerRef;
    private final ThemeManager themeManager;
    
    // private final EditionManager editionManager; // Descomentar cuando se implemente y se inyecte
    private final ListCoordinator listCoordinator;
    private final ProjectManager projectService; // Servicio de persistencia de proyectos
    private final EditionManager editionManager;

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

    
    private final ViewUIConfig uiConfig;
    
    // --- SECCIÓN 2: CONSTRUCTOR ---
    /**
     * Constructor de ActionFactory.
     * Recibe todas las dependencias necesarias para crear las Actions.
     * Llama a initializeAllActions para poblar el actionMap.
     */
    public ActionFactory(
            VisorModel model, 
            VisorView view,
            ZoomManager zoomManager, 
            FileOperationsManager fileOperationsManager,
            EditionManager editionManager, // Descomentar cuando esté
            ListCoordinator listCoordinator,
            IconUtils iconUtils, 
            ConfigurationManager configuration,
            ProjectManager projectService,
            Map<String, String> comandoToIconKeyMap,
    		ViewManager viewManager,
    		ThemeManager themeManager,
    		ViewUIConfig uiConfig,
            VisorController controller
    ){ 
        
        // 2.1. Asignar dependencias principales.
        this.model 						= Objects.requireNonNull(model, "VisorModel no puede ser null en ActionFactory");
        this.view 						= Objects.requireNonNull(view, "VisorView no puede ser null en ActionFactory");
        this.configuration 				= Objects.requireNonNull(configuration, "ConfigurationManager no puede ser null en ActionFactory");
        this.iconUtils 					= Objects.requireNonNull(iconUtils, "IconUtils no puede ser null en ActionFactory");
        this.controllerRef 				= Objects.requireNonNull(controller, "VisorController (controllerRef) no puede ser null en ActionFactory");
        this.themeManager 				= Objects.requireNonNull(themeManager, "ThemeManager no puede ser null en ActionFactory");
        
        // 2.2. Asignar Managers y Coordinadores.
        this.zoomManager 				= Objects.requireNonNull(zoomManager, "ZoomManager no puede ser null en ActionFactory");
        this.fileOperationsManager 		= Objects.requireNonNull(fileOperationsManager, "FileManager no puede ser null");
        this.viewManager 				= Objects.requireNonNull(viewManager, "ViewManager no puede ser null");
        this.editionManager				= Objects.requireNonNull(editionManager, "ViewManager no puede ser null");
        this.contextSensitiveActions 	= new ArrayList<>(); 
        
        // this.editionManager = Objects.requireNonNull(editionManager, "EditionManager no puede ser null");
        this.listCoordinator 			= Objects.requireNonNull(listCoordinator, "ListCoordinator no puede ser null en ActionFactory");
        this.projectService 			= Objects.requireNonNull(projectService, "ProjectManager (servicio) no puede ser null");
        
        // 2.3. Asignar mapa de iconos.
        this.comandoToIconKeyMap 		= Objects.requireNonNull(comandoToIconKeyMap, "comandoToIconKeyMap no puede ser null");
        
        this.uiConfig = Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser null en ActionFactory");

        // 2.4. Leer dimensiones de iconos desde la configuración.
        this.iconoAncho 				= configuration.getInt("iconos.ancho", 24);
        this.iconoAlto 					= configuration.getInt("iconos.alto", 24); // Usar el mismo valor o -1 si quieres proporción basada en ancho

        // 2.5. Inicializar el mapa interno de acciones.
        this.actionMap 					= new HashMap<>();

        // 2.6. Crear e inicializar todas las Actions.
        System.out.println("  [ActionFactory] Inicializando todas las Actions...");
        initializeAllActions();
        System.out.println("  [ActionFactory] Todas las Actions inicializadas y mapeadas. Total: " + this.actionMap.size());
    }


    // --- SECCIÓN 3: MÉTODO PRINCIPAL DE INICIALIZACIÓN DE ACTIONS ---
    /**
     * Crea instancias de todas las Actions de la aplicación y las registra en el actionMap.
     * Se llama desde el constructor de ActionFactory.
     * El orden puede ser importante si hay dependencias entre Actions (ej. ToggleZoomManual necesita ResetZoom).
     */
    private void initializeAllActions() {
        // 3.1. Crear la Action genérica para funcionalidades pendientes primero.
        this.funcionalidadPendienteAction = createFuncionalidadPendienteAction();
        actionMap.put(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, this.funcionalidadPendienteAction);

        // 3.2. Crear y registrar Actions de Zoom
        Action resetZoomAct = createResetZoomAction();
        actionMap.put(AppActionCommands.CMD_ZOOM_RESET, resetZoomAct);
        actionMap.put(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE, createToggleZoomManualAction(resetZoomAct)); // Pasa la dependencia
        
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
        
        
//        actionMap.put(AppActionCommands.CMD_ZOOM_TIPO_AUTO, createZoomAutoAction());
//        actionMap.put(AppActionCommands.CMD_ZOOM_TIPO_ANCHO, createZoomAnchoAction());
//        actionMap.put(AppActionCommands.CMD_ZOOM_TIPO_ALTO, createZoomAltoAction());
//        actionMap.put(AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR, createZoomFitAction());
//        actionMap.put(AppActionCommands.CMD_ZOOM_TIPO_FIJO, createZoomFixedAction());
//        actionMap.put(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO, createZoomFijadoAction());
        // Las Actions de zoom ya inicializan su SELECTED_KEY en sus constructores leyendo del modelo/config.

        // 3.3. Crear y registrar Actions de Navegación
        actionMap.put(AppActionCommands.CMD_NAV_PRIMERA, createFirstImageAction());
        actionMap.put(AppActionCommands.CMD_NAV_ANTERIOR, createPreviousImageAction());
        actionMap.put(AppActionCommands.CMD_NAV_SIGUIENTE, createNextImageAction());
        actionMap.put(AppActionCommands.CMD_NAV_ULTIMA, createLastImageAction());
        
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
        actionMap.put(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA, createSetSubfolderReadModeAction(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA,"Mostrar Solo Carpeta Actual",false)); // false significa NO incluir subcarpetas
        actionMap.put(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS, createSetSubfolderReadModeAction(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS,"Mostrar Imágenes de Subcarpetas",true));

        actionMap.put(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES, createToggleProporcionesAction());

        // 3.9. Crear y registrar Actions de Proyecto
        actionMap.put(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA, createToggleMarkImageAction());
        actionMap.put(AppActionCommands.CMD_PROYECTO_GESTIONAR, createGestionarProyectoAction());
        
        // 3.10. Crear y registrar Actions Especiales
        actionMap.put(AppActionCommands.CMD_ESPECIAL_MENU, createMenuAction());
        actionMap.put(AppActionCommands.CMD_ESPECIAL_BOTONES_OCULTOS, createHiddenButtonsAction());
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
                 JOptionPane.showMessageDialog(view.getFrame(), mensaje, "En Desarrollo", JOptionPane.INFORMATION_MESSAGE);
            }
        };
    }

    /**
     * 4.2. Helper para obtener el ImageIcon para un comando dado.
     */
    private ImageIcon getIconForCommand(String appCommand) {
        String claveIcono = this.comandoToIconKeyMap.get(appCommand);
        if (claveIcono != null && !claveIcono.isBlank()) {
//            return this.iconUtils.getIcon(claveIcono, this.iconoAncho, this.iconoAlto);
        	return this.iconUtils.getScaledIcon(claveIcono, this.iconoAncho, this.iconoAlto);
        }
        // System.err.println("WARN [ActionFactory]: No se encontró clave de icono en comandoToIconKeyMap para el comando: " + appCommand + ". No se asignará icono a la Action.");
        return null; // Devolver null si no hay icono definido para este comando
    }

    // --- 4.3. Métodos Create para Actions de Zoom ---
    private Action createToggleZoomManualAction(Action resetZoomActionDependency) {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
        return new ToggleZoomManualAction("Activar Zoom Manual", icon, this.zoomManager, resetZoomActionDependency, this.view, this.model);
    }
    private Action createResetZoomAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ZOOM_RESET);
        return new ResetZoomAction("Resetear Zoom", icon, this.zoomManager, this.view);
    }
    
    private Action createAplicarModoZoomAction(String commandKey, String displayName, ZoomModeEnum modo) {
        ImageIcon icon = getIconForCommand(commandKey); 
        // Pasamos this.model a AplicarModoZoomAction
        return new AplicarModoZoomAction(this.zoomManager, this.model, displayName, icon, modo, commandKey);
    }
    
    // --- 4.4. Métodos Create para Actions de Navegación ---
    // (Asumiendo que los constructores de estas Actions aceptan ListCoordinator, nombre, icono)
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
        CropAction action = new CropAction(
            // this.editionManager, // Se pasará cuando la funcionalidad esté lista
            this.model, this.view, "Recortar", icon);
        this.contextSensitiveActions.add(action); // Registrarla
        return action;
    }

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
        return new RefreshAction("Refrescar Lista", icon, this.model);
    }
    private Action createLocateFileAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_IMAGEN_LOCALIZAR);
        LocateFileAction action = new LocateFileAction(this.model, this.view, "Localizar Archivo", icon);
        this.contextSensitiveActions.add(action); // <<< REGISTRAR LA ACCIÓN
        return action;
    }    

    // --- 4.7. Métodos Create para Actions de Vista (Toggles UI) ---
    // (Estas actions suelen tomar VisorView y ConfigurationManager)
     
    private Action createToggleMenuBarAction() {
        String commandKey = AppActionCommands.CMD_VISTA_TOGGLE_MENU_BAR;
        return new ToggleMenuBarAction("Barra de Menú", null, this.viewManager, this.configuration, this.view, "interfaz.menu.vista.barra_de_menu.seleccionado", "Barra_de_Menu", commandKey);
    }
    private Action createToggleToolBarAction() {
    	return new ToggleToolBarAction("Barra de Botones", null, this.viewManager, this.configuration, "interfaz.menu.vista.barra_de_botones.seleccionado", "Barra_de_Botones", AppActionCommands.CMD_VISTA_TOGGLE_TOOL_BAR);
    }
    private Action createToggleFileListAction() {
    	return new ToggleFileListAction("Mostrar/Ocultar la Lista de Archivos", null, this.viewManager, this.configuration, "interfaz.menu.vista.mostrar_ocultar_la_lista_de_archivos.seleccionado", "mostrar_ocultar_la_lista_de_archivos", AppActionCommands.CMD_VISTA_TOGGLE_FILE_LIST);
    }
    private Action createToggleThumbnailsAction() {
   	    return new ToggleThumbnailsAction("Imagenes en Miniatura", null, this.viewManager, this.configuration, "interfaz.menu.vista.imagenes_en_miniatura.seleccionado", "imagenes_en_miniatura", AppActionCommands.CMD_VISTA_TOGGLE_THUMBNAILS);
   	}
    private Action createToggleLocationBarAction() {
   	    return new ToggleLocationBarAction("Linea de Ubicacion del Archivo", null, this.viewManager, this.configuration, "interfaz.menu.vista.linea_de_ubicacion_del_archivo.seleccionado", "linea_de_ubicacion_del_archivo", AppActionCommands.CMD_VISTA_TOGGLE_LOCATION_BAR); 
   	}
    private Action createToggleCheckeredBackgroundAction() {
   	    return new ToggleCheckeredBackgroundAction("Fondo a Cuadros", null, this.view, this.configuration, "interfaz.menu.vista.fondo_a_cuadros.seleccionado", AppActionCommands.CMD_VISTA_TOGGLE_CHECKERED_BG);
   	}
    private Action createToggleAlwaysOnTopAction() {
   	    return new ToggleAlwaysOnTopAction("Mantener Ventana Siempre Encima", null, this.view, this.configuration, "interfaz.menu.vista.mantener_ventana_siempre_encima.seleccionado", controlador.commands.AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP);
   	}
    private Action createMostrarDialogoListaAction() {
   	    ImageIcon icon = getIconForCommand(AppActionCommands.CMD_VISTA_MOSTRAR_DIALOGO_LISTA);
   	    return new MostrarDialogoListaAction("Mostrar Lista de Imágenes", icon, this.view, this.model,this.controllerRef);
   	}
    private Action createToggleMiniatureTextAction() {
   	 	return new ToggleMiniatureTextAction("Mostrar Nombres en Miniaturas", null, this.configuration, this.view,"miniaturas.ui.mostrar_nombres",AppActionCommands.CMD_VISTA_TOGGLE_MINIATURE_TEXT);
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
        return new ToggleThemeAction(this.themeManager, this.view, themeNameInternal, displayNameForMenu, commandKey);
    }
   
    // --- 4.9. Métodos Create para Actions de Toggle Generales ---
    private Action createToggleSubfoldersAction() {
   	ImageIcon icon = getIconForCommand(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
    	return new ToggleSubfoldersAction("Alternar Subcarpetas", icon, this.configuration, this.model, this.controllerRef);
    }
    private Action createSetSubfolderReadModeAction(String commandKey, String displayName, boolean shoulIncludeSubfolders) {
   	    // Los JRadioButtonMenuItems generalmente no usan iconos de la Action
    ImageIcon icon = null; 
    return new SetSubfolderReadModeAction(displayName, icon, this.controllerRef, shoulIncludeSubfolders, commandKey);
    }
    private Action createToggleProporcionesAction() {
    ImageIcon icon = getIconForCommand(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);
    return new ToggleProporcionesAction("Mantener Proporciones", icon, this.model, this.controllerRef);
    }
   
    // --- 4.10. Métodos Create para Actions de Proyecto ---
    private Action createToggleMarkImageAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
        // Ahora este constructor coincide con el refactorizado
        ToggleMarkImageAction action = new ToggleMarkImageAction(this.projectService, this.model, this.view, "Marcar/Desmarcar para Proyecto",icon);
        this.contextSensitiveActions.add(action); // REGISTRAR como sensible al contexto
        return action;
    }
    private Action createGestionarProyectoAction() {
        ImageIcon icon = getIconForCommand(AppActionCommands.CMD_PROYECTO_GESTIONAR);
        return new GestionarProyectoAction(this.projectService, this.view, "Gestionar Proyectos", icon);
    }
    
    // --- 4.11. Métodos Create para Actions Especiales ---
//	private Action createMenuAction() { // Ya no necesita toggleMenuBarActionInstance
//	    ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ESPECIAL_MENU);
//	    
//	    UIDefinitionService uiDefService = new UIDefinitionService();
//	    List<MenuItemDefinition> fullMenuStructure = uiDefService.generateMenuStructure();
//	    return new MenuAction("Menú Principal", icon, fullMenuStructure, this.actionMap, this.uiConfig);
//	}
     private Action createMenuAction() { // Ya no necesita toggleMenuBarActionInstance como parámetro directo
         ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ESPECIAL_MENU);
         
         UIDefinitionService uiDefService = new UIDefinitionService();
         List<MenuItemDefinition> fullMenuStructure = uiDefService.generateMenuStructure();

         return new MenuAction("Menú Principal", icon, fullMenuStructure, this.actionMap, this.uiConfig);
     }
     private Action createHiddenButtonsAction() {
         ImageIcon icon = getIconForCommand(AppActionCommands.CMD_ESPECIAL_BOTONES_OCULTOS);
         return new HiddenButtonsAction("Más Opciones", icon, this.actionMap, this.uiConfig );
     }

    // --- SECCIÓN 5: GETTER PARA EL ACTIONMAP ---
    /**
     * Devuelve el mapa de acciones construido.
     * @return El mapa donde la clave es el String del comando (de AppActionCommands)
     *         y el valor es la instancia de Action correspondiente.
     */
    public Map<String, Action> getActionMap() {
        return Collections.unmodifiableMap(this.actionMap); // Devolver copia inmutable
    }
    
    public List<ContextSensitiveAction> getContextSensitiveActions() {
        return Collections.unmodifiableList(this.contextSensitiveActions);
    }
}