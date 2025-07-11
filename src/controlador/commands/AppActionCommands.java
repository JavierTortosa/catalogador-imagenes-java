package controlador.commands;


/**
 * Define los comandos de acción canónicos utilizados en toda la aplicación.
 * Estos Strings constantes se usan como claves en el ActionMap y como
 * valor para Action.ACTION_COMMAND_KEY.
 */
public interface AppActionCommands {

 // --- Generico pendiente de implementar ---
	String CMD_FUNCIONALIDAD_PENDIENTE 		= "cmd.todo.funcionalidad_pendiente";

	
 // --- Archivo ---
    String CMD_ARCHIVO_ABRIR 				= "cmd.archivo.abrir";
    String CMD_ARCHIVO_ABRIR_NUEVA_VENTANA 	= "cmd.archivo.abrir_nueva_ventana"; // (Asumiendo Action futura)
    String CMD_ARCHIVO_GUARDAR 				= "cmd.archivo.guardar"; // (Asumiendo Action futura)
    String CMD_ARCHIVO_GUARDAR_COMO 		= "cmd.archivo.guardar_como"; // (Asumiendo Action futura)
    String CMD_ARCHIVO_IMPRIMIR 			= "cmd.archivo.imprimir"; // (Asumiendo Action futura)
    String CMD_ARCHIVO_SALIR 				= "cmd.archivo.salir"; // (Necesario si tienes item "Salir")
    

 // --- Navegación ---
    String CMD_NAV_PRIMERA 					= "cmd.nav.primera";
    String CMD_NAV_ANTERIOR 				= "cmd.nav.anterior";
    String CMD_NAV_SIGUIENTE				= "cmd.nav.siguiente";
    String CMD_NAV_ULTIMA 					= "cmd.nav.ultima";
    String CMD_NAV_IR_A 					= "cmd.nav.ir_a"; 
    String CMD_NAV_BUSCAR 					= "cmd.nav.buscar"; 
    
    
 // Comandos para navegación por fotogramas (si son Actions separadas)
    String CMD_NAV_FOTOGRAMA_ANTERIOR 		= "cmd.nav.fotograma.anterior";
    String CMD_NAV_FOTOGRAMA_SIGUIENTE 		= "cmd.nav.fotograma.siguiente";
    String CMD_NAV_FOTOGRAMA_PRIMERO 		= "cmd.nav.fotograma.primero";
    String CMD_NAV_FOTOGRAMA_ULTIMO 		= "cmd.nav.fotograma.ultimo";


 // --- Zoom ---
    String CMD_ZOOM_ACERCAR 				= "cmd.zoom.acercar"; 					// (Asumiendo Action futura)
    String CMD_ZOOM_ALEJAR 					= "cmd.zoom.alejar";   					// (Asumiendo Action futura)
    String CMD_ZOOM_PERSONALIZADO 			= "cmd.zoom.personalizado"; 			// (Asumiendo Action futura)
    String CMD_ZOOM_TAMAÑO_REAL 			= "cmd.zoom.tamano_real"; 				// (Asumiendo Action futura)
    String CMD_ZOOM_MANUAL_TOGGLE 			= "cmd.zoom.manual.toggle"; 			// Para ToggleZoomManualAction
    String CMD_ZOOM_RESET 					= "cmd.zoom.reset";         			// Para ResetZoomAction
    String CMD_ZOOM_TOGGLE_TO_CURSOR 		= "cmd.zoom.toggle_to_cursor";			// para zoom al cursor
    
    // Tipos de Zoom (cada uno es una Action)
	    String CMD_ZOOM_TIPO_AUTO 				= "cmd.zoom.tipo.auto";       			// Para ZoomAutoAction
	    String CMD_ZOOM_TIPO_ANCHO 				= "cmd.zoom.tipo.ancho";      			// Para ZoomAnchoAction
	    String CMD_ZOOM_TIPO_ALTO 				= "cmd.zoom.tipo.alto";       			// Para ZoomAltoAction
	    String CMD_ZOOM_TIPO_AJUSTAR 			= "cmd.zoom.tipo.ajustar";   			// Para ZoomFitAction
	    String CMD_ZOOM_TIPO_FIJO 				= "cmd.zoom.tipo.fijo";       			// Para ZoomFixedAction
	    String CMD_ZOOM_TIPO_ESPECIFICADO 		= "cmd.zoom.tipo.especificado"; 		// Para ZoomFijadoAction (o el nombre que le des)
	    String CMD_ZOOM_TIPO_RELLENAR 			= "cmd.zoom.tipo.rellenar"; 			// (Asumiendo Action futura)

 // --- Imagen/Edición ---
    String CMD_IMAGEN_ROTAR_IZQ 			= "cmd.imagen.rotar.izq";    			// Para RotateLeftAction
    String CMD_IMAGEN_ROTAR_DER 			= "cmd.imagen.rotar.der";    			// Para RotateRightAction
    String CMD_IMAGEN_VOLTEAR_H 			= "cmd.imagen.voltear.h";  				// Para FlipHorizontalAction
    String CMD_IMAGEN_VOLTEAR_V 			= "cmd.imagen.voltear.v";  				// Para FlipVerticalAction
    String CMD_IMAGEN_RECORTAR 				= "cmd.imagen.recortar";    			// Para CropAction
    String CMD_IMAGEN_LOCALIZAR 			= "cmd.imagen.localizar";   			// Para LocateFileAction
    String CMD_IMAGEN_RENOMBRAR 			= "cmd.imagen.renombrar"; 				// (Asumiendo Action futura)
    String CMD_IMAGEN_MOVER_PAPELERA 		= "cmd.imagen.mover_papelera"; 			// (Asumiendo Action futura)
    String CMD_IMAGEN_ELIMINAR 				= "cmd.imagen.eliminar";   				// Para DeleteAction
    String CMD_IMAGEN_FONDO_ESCRITORIO 		= "cmd.imagen.fondo_escritorio"; 		// (Asumiendo Action futura)
    String CMD_IMAGEN_FONDO_BLOQUEO 		= "cmd.imagen.fondo_bloqueo"; 			// (Asumiendo Action futura)
    String CMD_IMAGEN_PROPIEDADES 			= "cmd.imagen.propiedades"; 			// (Asumiendo Action futura)

    
 // --- Gestor de proyectos ---
    String CMD_PROYECTO_TOGGLE_MARCA 		= "cmd.proyecto.toggle_marca"; 			// Para Marcar/Desmarcar imagen actual
    String CMD_PROYECTO_GESTIONAR 			= "cmd.proyecto.gestionar";     		// Para abrir el diálogo/funcionalidad de gestión de proyectos (el JOptionPane por ahora)
    String CMD_PROYECTO_TOGGLE_VISTA 		= "cmd.proyecto.toggle_vista_seleccion";//  botón/menú para alternar vista
    
    // Gestor de proyectos - POPUP MENU
	    String CMD_PROYECTO_MOVER_A_DESCARTES 	= "cmd.proyecto.mover_a_descartes";
	    String CMD_EXPORT_ASIGNAR_ARCHIVO 		= "cmd.export.asignar_archivo";
	    String CMD_EXPORT_ABRIR_UBICACION 		= "cmd.export.abrir_ubicacion";
	    String CMD_EXPORT_QUITAR_DE_COLA 		= "cmd.export.quitar_de_cola";
	    String CMD_EXPORT_IGNORAR_COMPRIMIDO 	= "cmd.export.ignorar_comprimido";
	    String CMD_PROYECTO_RESTAURAR_DE_DESCARTES = "cmd.proyecto.restaurar_de_descartes";
	    String CMD_PROYECTO_ELIMINAR_PERMANENTEMENTE = "cmd.proyecto.eliminar";
    

 // --- Vista (Toggles de UI) ---
    String CMD_VISTA_TOGGLE_MENU_BAR 		= "cmd.vista.toggle.menu_bar";      	// Para ToggleMenuBarAction
    String CMD_VISTA_TOGGLE_TOOL_BAR 		= "cmd.vista.toggle.tool_bar";      	// Para ToggleToolBarAction
    String CMD_VISTA_TOGGLE_FILE_LIST		= "cmd.vista.toggle.file_list";     	// Para ToggleFileListAction
    String CMD_VISTA_TOGGLE_THUMBNAILS 		= "cmd.vista.toggle.thumbnails";   		// Para ToggleThumbnailsAction
    String CMD_VISTA_TOGGLE_LOCATION_BAR 	= "cmd.vista.toggle.location_bar";  	// Para ToggleLocationBarAction
    String CMD_VISTA_TOGGLE_CHECKERED_BG 	= "cmd.vista.toggle.checkered_bg";  	// Para ToggleCheckeredBackgroundAction
    String CMD_VISTA_TOGGLE_ALWAYS_ON_TOP	= "cmd.vista.toggle.always_on_top";		// Para ToggleAlwaysOnTopAction
    String CMD_VISTA_MOSTRAR_DIALOGO_LISTA 	= "cmd.vista.mostrar_dialogo_lista";	// Para el menú sin Action directa

    String CMD_VISTA_TOGGLE_MINIATURE_TEXT 	= "cmd.vista.toggle_miniature_text"; 	//Muestra el texto del nombre de las miniaturas
    String CMD_VISTA_SWITCH_TO_VISUALIZADOR = "cmd.vista.switch_to_visualizador";
    
    
    // --- Comportamiento/Toggles Generales ---
    String CMD_TOGGLE_SUBCARPETAS 			= "cmd.toggle.subcarpetas";             // Para ToggleSubfoldersAction
    String CMD_TOGGLE_MANTENER_PROPORCIONES = "cmd.toggle.mantener_proporciones"; 	// Para ToggleProporcionesAction
    String CMD_TOGGLE_WRAP_AROUND 			= "cmd.toggle.wrap_around";           	// (Futuro?) Para 'Volver al inicio al llegar al final'
    
    // --- Tema ---
    String CMD_TEMA_CLEAR 					= "cmd.tema.clear";     // Para ToggleThemeAction("clear",...)
    String CMD_TEMA_DARK 					= "cmd.tema.dark";      // Para ToggleThemeAction("dark",...)
    String CMD_TEMA_BLUE 					= "cmd.tema.blue";      // etc.
    String CMD_TEMA_GREEN					= "cmd.tema.green";
    String CMD_TEMA_ORANGE 					= "cmd.tema.orange";

    // --- Configuración ---
    String CMD_CONFIG_GUARDAR 				= "cmd.config.guardar";         // Para menú "Guardar Configuración Actual"
    String CMD_CONFIG_CARGAR_INICIAL 		= "cmd.config.cargar_inicial"; // Para menú "Cargar Configuración Inicial"
    String CMD_CONFIG_MOSTRAR_VERSION 		= "cmd.config.mostrar_version"; // Para menú "Version"
    String CMD_CONFIG_CARGA_SOLO_CARPETA 	= "cmd.config.carga.solo_carpeta"; //Mostrar Solo Carpeta Actual
    String CMD_CONFIG_CARGA_CON_SUBCARPETAS = "cmd.config.carga.con_subcarpetas";//Mostrar Imagenes de Subcarpetas

    // --- Especiales / Otros ---
    String CMD_ESPECIAL_REFRESCAR 			= "cmd.especial.refrescar";      // Para RefreshAction (si es necesaria además de recargar lista)
    String CMD_ESPECIAL_REFRESCAR_UI		= "cmd.especial.refrescar_ui";	//Para Refrescar la UI
    String CMD_ESPECIAL_MENU 				= "cmd.especial.menu";             // Para MenuAction (si hace algo específico)
    String CMD_ESPECIAL_BOTONES_OCULTOS 	= "cmd.especial.botones_ocultos"; // Para HiddenButtonsAction

    // --- Favoritos (Futuro) ---
    String CMD_FAVORITO_TOGGLE 				= "cmd.favorito.toggle";       // Para botón/menú Añadir/Quitar Favorito
    String CMD_FAVORITO_MOSTRAR_LISTA 		= "cmd.favorito.mostrar"; // Para botón/menú Ver lista de Favoritos

    // --- Comandos para Configurar Visibilidad y Formato de Barras de Información ---

    // -- Checkboxes para visibilidad de barras completas --
    String CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_VISIBLE = "cmd.infobar.config.toggle.superior.visible";
    String CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_VISIBLE = "cmd.infobar.config.toggle.inferior.visible";

    // -- Checkboxes para visibilidad de elementos en Barra Superior --
    String CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_NOMBRE_RUTA = "cmd.infobar.config.toggle.superior.nombre_ruta";
    String CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_INDICE_TOTAL = "cmd.infobar.config.toggle.superior.indice_total";
    String CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_DIMENSIONES = "cmd.infobar.config.toggle.superior.dimensiones";
    String CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_TAMANO_ARCHIVO = "cmd.infobar.config.toggle.superior.tamano_archivo";
    String CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_FECHA_ARCHIVO = "cmd.infobar.config.toggle.superior.fecha_archivo";
    String CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_FORMATO_IMAGEN = "cmd.infobar.config.toggle.superior.formato_imagen"; // Para futuro
    String CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_MODO_ZOOM = "cmd.infobar.config.toggle.superior.modo_zoom";
    String CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_ZOOM_REAL_PCT = "cmd.infobar.config.toggle.superior.zoom_real_pct";

    // -- RadioButtons para formato de Nombre/Ruta en Barra Superior --
    String CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_SOLO_NOMBRE = "cmd.infobar.config.formato.superior.nombre_ruta.solo_nombre";
    String CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_RUTA_COMPLETA = "cmd.infobar.config.formato.superior.nombre_ruta.ruta_completa";

    // -- Checkboxes para visibilidad de elementos en Barra Inferior --
    String CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_NOMBRE_RUTA = "cmd.infobar.config.toggle.inferior.nombre_ruta";
    String CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_ZM = "cmd.infobar.config.toggle.inferior.icono_zm";
    String CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_PROP = "cmd.infobar.config.toggle.inferior.icono_prop";
    String CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_SUBC = "cmd.infobar.config.toggle.inferior.icono_subc";
    String CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_CTRL_ZOOM_PCT = "cmd.infobar.config.toggle.inferior.ctrl_zoom_pct";
    String CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_CTRL_MODO_ZOOM = "cmd.infobar.config.toggle.inferior.ctrl_modo_zoom";
    String CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_MENSAJES_APP = "cmd.infobar.config.toggle.inferior.mensajes_app";

    // -- RadioButtons para formato de Nombre/Ruta en Barra Inferior --
    String CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_SOLO_NOMBRE = "cmd.infobar.config.formato.inferior.nombre_ruta.solo_nombre";
    String CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_RUTA_COMPLETA = "cmd.infobar.config.formato.inferior.nombre_ruta.ruta_completa";
    
    
    //public static final 
    String CMD_REFRESH_TOOLBARS = "cmd.refresh.toolbars";
    
    // --- Comandos para IDENTIFICAR elementos de UI (usados como ActionCommand en Fallback) ---
    // Estos NO tendrán una Action asociada en el actionMap principal.
    // Usaremos las claves largas de config directamente como identificadores.
    // Ejemplo: La clave del botón como comando para el checkbox que lo controla.
    // (No definimos constantes aquí, usaremos las strings largas directamente)

    // --- Orden Lista (si se usa lógica sin Action directa para radios) ---
    // String CMD_ORDEN_CRITERIO_NOMBRE = "cmd.orden.criterio.nombre";
    // String CMD_ORDEN_CRITERIO_TAMANO = "cmd.orden.criterio.tamano";
    // String CMD_ORDEN_DIRECCION_ASC = "cmd.orden.direccion.asc";
    // String CMD_ORDEN_DIRECCION_DESC = "cmd.orden.direccion.desc";
    
    // --- Comandos de Paneo del D-Pad ---
    // Paneo Absoluto (al borde del panel)
    String CMD_PAN_TOP_EDGE 	= "cmd.pan.top_edge";
    String CMD_PAN_BOTTOM_EDGE 	= "cmd.pan.bottom_edge";
    String CMD_PAN_LEFT_EDGE 	= "cmd.pan.left_edge";
    String CMD_PAN_RIGHT_EDGE 	= "cmd.pan.right_edge";

    // Paneo Incremental (pequeños pasos)
    String CMD_PAN_UP_INCREMENTAL 	= "cmd.pan.up_incremental";
    String CMD_PAN_DOWN_INCREMENTAL = "cmd.pan.down_incremental";
    String CMD_PAN_LEFT_INCREMENTAL = "cmd.pan.left_incremental";
    String CMD_PAN_RIGHT_INCREMENTAL= "cmd.pan.right_incremental";

    // --- Comandos para Control de Fondo de Imagen (NUEVOS) ---
    String CMD_BACKGROUND_COLOR_SLOT_1  = "cmd.background.color.slot_1";
    String CMD_BACKGROUND_COLOR_SLOT_2  = "cmd.background.color.slot_2";
    String CMD_BACKGROUND_COLOR_SLOT_3  = "cmd.background.color.slot_3";
    String CMD_BACKGROUND_COLOR_SLOT_4  = "cmd.background.color.slot_4";
    String CMD_BACKGROUND_COLOR_SLOT_5  = "cmd.background.color.slot_5";
    String CMD_BACKGROUND_CHECKERED     = "cmd.background.checkered";
    String CMD_BACKGROUND_CUSTOM_COLOR  = "cmd.background.custom_color";
    
    

}