package controlador.commands;


/**
 * Define los comandos de acción canónicos utilizados en toda la aplicación.
 * Estos Strings constantes se usan como claves en el ActionMap y como
 * valor para Action.ACTION_COMMAND_KEY.
 */
public interface AppActionCommands {

 // --- Generico pendiente de implementar ---
	public static final String CMD_RENDER_ASIGNAR_PREVIEW              = "cmd.render.asignar_preview";
	public static final String CMD_FUNCIONALIDAD_PENDIENTE             = "cmd.todo.funcionalidad_pendiente";

 // --- Ayuda ---
	public static final String CMD_AYUDA_VER_ATAJOS 				= "cmd.ayuda.ver_atajos";
	public static final String CMD_AYUDA_MOSTRAR_GUIA 				= "cmd.ayuda.mostrar_guia";
	
 // --- Archivo ---
    public static final String CMD_ARCHIVO_ABRIR 					= "cmd.archivo.abrir";
    public static final String CMD_ARCHIVO_ABRIR_NUEVA_VENTANA 		= "cmd.archivo.abrir_nueva_ventana"; // (Asumiendo Action futura)
    public static final String CMD_ARCHIVO_GUARDAR 					= "cmd.archivo.guardar"; // (Asumiendo Action futura)
    public static final String CMD_ARCHIVO_GUARDAR_COMO 			= "cmd.archivo.guardar_como"; // (Asumiendo Action futura)
    public static final String CMD_ARCHIVO_IMPRIMIR 				= "cmd.archivo.imprimir";
    public static final String CMD_ARCHIVO_ABRIR_CON 				= "cmd.archivo.abrir_con";
    public static final String CMD_ARCHIVO_RECARGAR_IMAGEN			= "cmd.archivo.recargar_imagen";
    public static final String CMD_ARCHIVO_SALIR 					= "cmd.archivo.salir";
    

 // --- Navegación ---
    public static final String CMD_NAV_PRIMERA 						= "cmd.nav.primera";
    public static final String CMD_NAV_ANTERIOR 					= "cmd.nav.anterior";
    public static final String CMD_NAV_SIGUIENTE					= "cmd.nav.siguiente";
    public static final String CMD_NAV_ULTIMA 						= "cmd.nav.ultima";
    public static final String CMD_NAV_IR_A 						= "cmd.nav.ir_a"; 
    public static final String CMD_NAV_BUSCAR 						= "cmd.nav.buscar"; 
    
    
 // --- Árbol de Carpetas ---
    public static final String CMD_ORDEN_CICLO 						= "cmd.orden.ciclo";
    public static final String CMD_TREE_OPEN_FOLDER                 = "cmd.tree.open_folder";
    public static final String CMD_TREE_DRILL_DOWN_FOLDER           = "cmd.tree.drill_down_folder";
    
 // --- Árbol de Tags (Modo Datos) ---
    public static final String CMD_DATOS_TAGS_VISTA_LISTA		= "cmd.datos.tags.vista_lista";
    public static final String CMD_DATOS_TAGS_VISTA_ARBOL		= "cmd.datos.tags.vista_arbol";
    public static final String CMD_DATOS_TAGS_ORDENAR			= "cmd.datos.tags.ordenar";
    public static final String CMD_DATOS_TAGS_FILTRAR			= "cmd.datos.tags.filtrar";
    public static final String CMD_DATOS_TAG_NUEVO				= "cmd.datos.tag.nuevo";
    public static final String CMD_DATOS_TAG_RENOMBRAR			= "cmd.datos.tag.renombrar";
    public static final String CMD_DATOS_TAG_BORRAR				= "cmd.datos.tag.borrar";
    public static final String CMD_DATOS_TAG_TOGGLE_ASIGNADO	= "cmd.datos.tag.toggle_asignado";
    public static final String CMD_DATOS_MANTENIMIENTO_BD		= "cmd.datos.mantenimiento.bd";
    public static final String CMD_DATOS_ORDEN_CICLO				= "cmd.datos.orden.ciclo";
    
    
 // --- Filtros
    public static final String CMD_FILTRO_ACTIVO					= "cmd.filtro.activo";
    public static final String CMD_FILTRO_TOGGLE_LIVE_FILTER		= "cmd.filtro.toggleLiveFilter";
    public static final String CMD_FILTRO_ADD_POSITIVE 				= "cmd.filtro.addPositive";
    public static final String CMD_FILTRO_ADD_NEGATIVE 				= "cmd.filtro.addNegative";
    public static final String CMD_FILTRO_REMOVE_SELECTED 			= "cmd.filtro.removeSelected";
    public static final String CMD_FILTRO_CLEAR_ALL 				= "cmd.filtro.clearAll";
    
    public static final String CMD_FILTRO_UP						= "cmd.filtro.up";
    public static final String CMD_FILTRO_DOWN						= "cmd.filtro.down";
    public static final String CMD_FILTRO_SET_TYPE_FILENAME 		= "cmd.filtro.setType.filename";
    public static final String CMD_FILTRO_SET_TYPE_FOLDER 			= "cmd.filtro.setType.folder";
    public static final String CMD_FILTRO_SET_TYPE_TAG 				= "cmd.filtro.setType.tag";
    
    
 // --- Grid
    
    public static final String CMD_GRID_SET_TEXT					= "cmd.grid.set_text";
    public static final String CMD_GRID_REMOVE_TEXT					= "cmd.grid.remove_text";
    public static final String CMD_GRID_SIZE_UP_MINIATURA			= "cmd.grid.size_up_miniatura";
    public static final String CMD_GRID_SIZE_DOWN_MINIATURA			= "cmd.grid.size_down_miniatura";
    public static final String CMD_GRID_SHOW_STATE					= "cmd.grid.mostrar_estado";
    
 // --- Carrousel
    
 // Comandos para navegación por fotogramas (si son Actions separadas)
    
//    public static final String CMD_CAROUSEL_NAV_PRIMERA 			= "cmd.carousel.nav.primera";
//    public static final String CMD_CAROUSEL_NAV_ANTERIOR 			= "cmd.carousel.nav.anterior";
//    public static final String CMD_CAROUSEL_NAV_SIGUIENTE 		= "cmd.carousel.nav.siguiente";
//    public static final String CMD_CAROUSEL_NAV_ULTIMA 			= "cmd.carousel.nav.ultima";
//    public static final String CMD_CAROUSEL_FASTER				= "cmd.carousel.faster";
//    public static final String CMD_CAROUSEL_SLOWER 				= "cmd.carousel.slower";
    public static final String CMD_CAROUSEL_PLAY 					= "cmd.carousel.play";
    public static final String CMD_CAROUSEL_PAUSE 					= "cmd.carousel.pause";
    public static final String CMD_CAROUSEL_STOP 					= "cmd.carousel.stop";
    public static final String CMD_CAROUSEL_REWIND 					= "cmd.carousel.rewind"; // Retroceso rápido
    public static final String CMD_CAROUSEL_FAST_FORWARD 			= "cmd.carousel.fast_forward"; // Avance rápido
    
    public static final String CMD_CAROUSEL_SPEED_INCREASE  		= "cmd.carousel.speed.increase";
    public static final String CMD_CAROUSEL_SPEED_DECREASE  		= "cmd.carousel.speed.decrease";
    public static final String CMD_CAROUSEL_SPEED_RESET     		= "cmd.carousel.speed.reset";
    public static final String CMD_CAROUSEL_SET_SPEED_PREFIX 		= "cmd.carousel.speed.set_";
    public static final String CMD_CAROUSEL_TOGGLE_SHUFFLE 			= "cmd.carousel.toggle.shuffle";
    
    
    //Comandos para la barra de orden
    public static final String CMD_ORDEN_CARPETA_RAIZ				= "cmd.orden.carpeta_raiz";
    public static final String CMD_ORDEN_CARPETA_ANTERIOR			= "cmd.orden.carpeta_anterior";
    public static final String CMD_ORDEN_CARPETA_SIGUIENTE			= "cmd.orden.carpeta_siguiente";
    public static final String CMD_ORDEN_ORDEN_ASCENDENTE			= "cmd.orden.ascendente";
    public static final String CMD_ORDEN_ORDEN_DESCENDENTE			= "cmd.orden.descentende";

 // --- Syncro ---
//    public static final String CMD_SYNC_TOGGLE					= "cmd.sync.toggle";
//    public static final String CMD_SYNC_SAFE						= "cmd.sync.safe";
    public static final String CMD_TOGGLE_SYNC_VISOR_CARRUSEL		= "cmd.toggle.sync_visor_carrusel";
    public static final String CMD_TOGGLE_SYNC_COLECCION_CARPETA	= "cmd.toggle.sync_coleccion_carpeta";
    public static final String CMD_COLECCION_AÑADIR_CARPETA_ACTUAL 	= "cmd.coleccion.añadir_carpeta_actual";
    
 // --- Zoom ---
    public static final String CMD_ZOOM_ACERCAR 					= "cmd.zoom.acercar"; 					// (Asumiendo Action futura)
    public static final String CMD_ZOOM_ALEJAR 						= "cmd.zoom.alejar";   					// (Asumiendo Action futura)
    public static final String CMD_ZOOM_PERSONALIZADO 				= "cmd.zoom.personalizado"; 			// (Asumiendo Action futura)
    public static final String CMD_ZOOM_TAMAÑO_REAL 				= "cmd.zoom.tamano_real"; 				// (Asumiendo Action futura)
    public static final String CMD_ZOOM_MANUAL_TOGGLE 				= "cmd.zoom.manual.toggle"; 			// Para ToggleZoomManualAction
    public static final String CMD_ZOOM_RESET 						= "cmd.zoom.reset";         			// Para ResetZoomAction
    public static final String CMD_ZOOM_TOGGLE_TO_CURSOR 			= "cmd.zoom.toggle_to_cursor";			// para zoom al cursor
    public static final String CMD_ZOOM_TIPO_SMART_FIT 				= "cmd.zoom.tipo.smart_fit";
    
    // Tipos de Zoom (cada uno es una Action)
    public static final String CMD_ZOOM_TIPO_AUTO 					= "cmd.zoom.tipo.auto";       			// Para ZoomAutoAction
    public static final String CMD_ZOOM_TIPO_ANCHO 					= "cmd.zoom.tipo.ancho";      			// Para ZoomAnchoAction
    public static final String CMD_ZOOM_TIPO_ALTO 					= "cmd.zoom.tipo.alto";       			// Para ZoomAltoAction
    public static final String CMD_ZOOM_TIPO_AJUSTAR 				= "cmd.zoom.tipo.ajustar";   			// Para ZoomFitAction
    public static final String CMD_ZOOM_TIPO_FIJO 					= "cmd.zoom.tipo.fijo";       			// Para ZoomFixedAction
    public static final String CMD_ZOOM_TIPO_ESPECIFICADO 			= "cmd.zoom.tipo.especificado"; 		// Para ZoomFijadoAction (o el nombre que le des)
    public static final String CMD_ZOOM_TIPO_RELLENAR 				= "cmd.zoom.tipo.rellenar"; 			// (Asumiendo Action futura)

 // --- Imagen/Edición ---
    public static final String CMD_IMAGEN_ROTAR_IZQ 				= "cmd.imagen.rotar.izq";    			// Para RotateLeftAction
    public static final String CMD_IMAGEN_ROTAR_DER 				= "cmd.imagen.rotar.der";    			// Para RotateRightAction
    public static final String CMD_IMAGEN_VOLTEAR_H 				= "cmd.imagen.voltear.h";  				// Para FlipHorizontalAction
    public static final String CMD_IMAGEN_VOLTEAR_V 				= "cmd.imagen.voltear.v";  				// Para FlipVerticalAction
    public static final String CMD_IMAGEN_RECORTAR 					= "cmd.imagen.recortar";    			// Para CropAction
    public static final String CMD_IMAGEN_LOCALIZAR 				= "cmd.imagen.localizar";   			// Para LocateFileAction
    public static final String CMD_IMAGEN_RENOMBRAR 				= "cmd.imagen.renombrar"; 				// (Asumiendo Action futura)
    public static final String CMD_IMAGEN_MOVER_PAPELERA 			= "cmd.imagen.mover_papelera"; 			// (Asumiendo Action futura)
    public static final String CMD_IMAGEN_ELIMINAR 					= "cmd.imagen.eliminar";   				// Para DeleteAction
    public static final String CMD_IMAGEN_FONDO_ESCRITORIO 			= "cmd.imagen.fondo_escritorio"; 		// (Asumiendo Action futura)
    public static final String CMD_IMAGEN_FONDO_BLOQUEO 			= "cmd.imagen.fondo_bloqueo"; 			// (Asumiendo Action futura)
    public static final String CMD_IMAGEN_PROPIEDADES 				= "cmd.imagen.propiedades"; 			// (Asumiendo Action futura)

    
 // --- Gestor de proyectos ---
    public static final String CMD_PROYECTO_TOGGLE_MARCA 			= "cmd.proyecto.toggle_marca"; 			// Para Marcar/Desmarcar imagen actual
    public static final String CMD_PROYECTO_GESTIONAR 				= "cmd.proyecto.gestionar";     		// Para abrir el diálogo/funcionalidad de gestión de proyectos (el JOptionPane por ahora)
    public static final String CMD_PROYECTO_TOGGLE_VISTA 			= "cmd.proyecto.toggle_vista_seleccion";//  botón/menú para alternar vista
    public static final String CMD_PROYECTO_NUEVO          			= "cmd.proyecto.nuevo";
    public static final String CMD_PROYECTO_ABRIR          			= "cmd.proyecto.abrir";
    public static final String CMD_PROYECTO_GUARDAR        			= "cmd.proyecto.guardar";
    public static final String CMD_PROYECTO_GUARDAR_COMO   			= "cmd.proyecto.guardar_como";
    public static final String CMD_PROYECTO_ELIMINAR   				= "cmd.proyecto.borrar";
    
    
    // Gestor de proyectos - TOOLBAR EXPORTAR
	    public static final String CMD_EXPORT_ASIGNAR_ARCHIVO 		= "cmd.export.asignar_archivo";
	    public static final String CMD_EXPORT_ABRIR_UBICACION 		= "cmd.export.abrir_ubicacion";
	    public static final String CMD_EXPORT_QUITAR_DE_COLA 		= "cmd.export.quitar_de_cola";
	    public static final String CMD_EXPORT_IGNORAR_COMPRIMIDO 	= "cmd.export.ignorar_comprimido";
	    public static final String CMD_EXPORT_RELOCALIZAR_IMAGEN 	= "cmd.export.relocalizar_imagen";
	    public static final String CMD_EXPORT_LIMPIAR_NO_ENCONTRADOS = "cmd.export.limpiar_no_encontrados";
	    public static final String CMD_EXPORT_REFRESH				= "cmd.export.refresh";
	    public static final String CMD_INICIAR_EXPORTACION 			= "cmd.export.iniciar"; 
	    public static final String CMD_EXPORTAR_PDF		 			= "cmd.export.pdf";
	    public static final String CMD_EXPORT_SELECCIONAR_CARPETA 	= "cmd.export.seleccionar.carpeta";
	    public static final String CMD_EXPORT_ASSIGN_PANNEL			= "cmd.export.assign_pannel";
    public static final String CMD_PROYECTO_TOGGLE_LAYOUT      = "cmd.proyecto.toggle_layout";
	    
	    public static final String CMD_EXPORT_DETALLES_SELECCION	= "cmd.export.detalles_seleccion";
	    public static final String CMD_DETALLES_PDF_SELECCION		= "cmd.export.detalles_pdf_seleccion";
	    
	    public static final String CMD_EXPORT_ADD_ASSOCIATED_FILE	= "cmd.export.add_associated_file";
	    public static final String CMD_EXPORT_DEL_ASSOCIATED_FILE	= "cmd.export.del_associated_file";
	    public static final String CMD_EXPORT_LOCATE_ASSOCIATED_FILE= "cmd.export.locate_associated_file";
	    
	 // Gestor de proyectos - POPUP MENU
	    public static final String CMD_PROYECTO_MOVER_A_DESCARTES 	= "cmd.proyecto.mover_a_descartes";
	    public static final String CMD_CLIENTE_TOGGLE_OCULTAR_DESCARTES = "cmd.cliente.toggle_ocultar_descartes";
	    public static final String CMD_PROYECTO_LOCALIZAR_ARCHIVO 	= "cmd.proyecto.localizar_archivo";
	    public static final String CMD_PROYECTO_VACIAR_DESCARTES 	= "cmd.proyecto.vaciar_descartes";
	    public static final String CMD_PROYECTO_RESTAURAR_DE_DESCARTES = "cmd.proyecto.restaurar_de_descartes";
	    public static final String CMD_PROYECTO_ELIMINAR_PERMANENTEMENTE = "cmd.proyecto.eliminar";
	    public static final String CMD_PROYECTO_ANADIR_ARCHIVOS 	= "cmd.proyecto.anadir_archivos";
	    
	    
 // --- Vista (Toggles de UI) ---
	    public static final String CMD_VISTA_SINGLE					= "cmd.vista.single";				// Para Vista de 1 imagen
	    public static final String CMD_VISTA_GRID 					= "cmd.vista.grid";					// Para Vista de Grid
	    public static final String CMD_VISTA_POLAROID 				= "cmd.vista.polaroid";				// Para Vista de Polaroid
	    public static final String CMD_VISTA_PANTALLA_COMPLETA 		= "cmd.vista.pantalla_completa";	// Para Vista de Pantalla Completa

    public static final String CMD_VISTA_TOGGLE_LOCATION_BAR 		= "cmd.vista.toggle.location_bar";  	// Para ToggleLocationBarAction
	public static final String CMD_VISTA_TOGGLE_INFOBAR_INFERIOR   	= "cmd.vista.toggle.infobar.inferior";
	public static final String CMD_VISTA_TOGGLE_INFOBAR_SUPERIOR   	= "cmd.vista.toggle.infobar.superior";
	
    public static final String CMD_VISTA_TOGGLE_MENU_BAR 			= "cmd.vista.toggle.menu_bar";      	// Para ToggleMenuBarAction
    public static final String CMD_VISTA_TOGGLE_TOOL_BAR 			= "cmd.vista.toggle.tool_bar";      	// Para ToggleToolBarAction
    public static final String CMD_VISTA_TOGGLE_FILE_LIST			= "cmd.vista.toggle.file_list";     	// Para ToggleFileListAction
    public static final String CMD_VISTA_TOGGLE_THUMBNAILS 			= "cmd.vista.toggle.thumbnails";   		// Para ToggleThumbnailsAction
    public static final String CMD_VISTA_TOGGLE_CHECKERED_BG 		= "cmd.vista.toggle.checkered_bg";  	// Para ToggleCheckeredBackgroundAction
    public static final String CMD_VISTA_TOGGLE_ALWAYS_ON_TOP		= "cmd.vista.toggle.always_on_top";		// Para ToggleAlwaysOnTopAction
    public static final String CMD_VISTA_MOSTRAR_DIALOGO_LISTA 		= "cmd.vista.mostrar_dialogo_lista";	// Para el menú sin Action directa

    public static final String CMD_VISTA_TOGGLE_MINIATURE_TEXT 		= "cmd.vista.toggle_miniature_text"; 	//Muestra el texto del nombre de las miniaturas
    public static final String CMD_VISTA_SWITCH_TO_VISUALIZADOR 	= "cmd.vista.switch_to_visualizador";
    public static final String CMD_MODO_DATOS 						= "cmd.modo.datos";
    public static final String CMD_PROYECTO_COMPARTIR_CLIENTE		= "cmd.proyecto.compartir_cliente";
    public static final String CMD_MODO_CLIENTE 					= "cmd.modo.cliente";
    public static final String CMD_VISTA_CAROUSEL 					= "cmd.vista.carousel";
    public static final String CMD_MODO_RENDER                      = "cmd.modo.render";
    public static final String CMD_MODO_EDITOR                      = "cmd.modo.editor";
    public static final String CMD_RENDER_ESCANEAR_CARPETA          = "cmd.render.escanear_carpeta";
    public static final String CMD_RENDER_PROCESAR_SELECCIONADOS    = "cmd.render.procesar_seleccionados";
    public static final String CMD_RENDER_PROCESAR_ARCHIVO          = "cmd.render.procesar_archivo";
    public static final String CMD_RENDER_COPIAR_ARCHIVOS           = "cmd.render.copiar_archivos";
    public static final String CMD_RENDER_DESCARGAR_ARCHIVOS        = "cmd.render.descargar_archivos";
    public static final String CMD_RENDER_ABRIR_TEMP                = "cmd.render.abrir_temp";
    public static final String CMD_RENDER_CLEAR_PREVIEW             = "cmd.render.clear_preview";

    public static final String CMD_PREVIEW_RENDER_3DGRID            = "cmd.preview.render.3dgrid";
    public static final String CMD_PREVIEW_RENDER_2DGRID            = "cmd.preview.render.2dgrid";
    public static final String CMD_PREVIEW_RENDER_COLLAGE           = "cmd.preview.render.collage";
    public static final String CMD_PREVIEW_RENDER_ADVANCE_EDIT      = "cmd.preview.render.advance_edit";
    public static final String CMD_EDITOR_NUEVO                      = "cmd.editor.nuevo";
    public static final String CMD_EDITOR_ABRIR                      = "cmd.editor.abrir";
    public static final String CMD_EDITOR_GUARDAR                    = "cmd.editor.guardar";
    public static final String CMD_EDITOR_GUARDAR_COMO               = "cmd.editor.guardar_como";
    public static final String CMD_PREVIEW_RENDER_GALLERY           = "cmd.preview.render.gallery";

    public static final String CMD_PREVIEW_RENDER_BRING_TO_FRONT    = "cmd.preview.render.bring_to_front";
    public static final String CMD_PREVIEW_RENDER_BRING_FORWARD     = "cmd.preview.render.bring_forward";
    public static final String CMD_PREVIEW_RENDER_SEND_BACKWARD     = "cmd.preview.render.send_backward";
    public static final String CMD_PREVIEW_RENDER_SEND_TO_BACK      = "cmd.preview.render.send_to_back";

    public static final String CMD_PREVIEW_RENDER_CLEAN_AND_ADD     = "cmd.preview.render.clean_and_add";
    public static final String CMD_PREVIEW_RENDER_ADD_IMAGE         = "cmd.preview.render.add_image";
    public static final String CMD_PREVIEW_RENDER_ADD_LAYER         = "cmd.preview.render.add_layer";
    public static final String CMD_PREVIEW_RENDER_DUPLICATE_LAYER   = "cmd.preview.render.duplicate_layer";
    public static final String CMD_PREVIEW_RENDER_COMBINE_LAYER     = "cmd.preview.render.combine_layer";
    public static final String CMD_PREVIEW_RENDER_DELETE_LAYER      = "cmd.preview.render.delete_layer";

    // --- Alinear capa ---
    public static final String CMD_PREVIEW_RENDER_ALIGN_BORDE_INFERIOR   = "cmd.preview.render.align.borde_inferior";
    public static final String CMD_PREVIEW_RENDER_ALIGN_CENTRO_VERTICAL  = "cmd.preview.render.align.centro_vertical";
    public static final String CMD_PREVIEW_RENDER_ALIGN_BORDE_SUPERIOR   = "cmd.preview.render.align.borde_superior";
    public static final String CMD_PREVIEW_RENDER_ALIGN_BORDE_IZQUIERDO  = "cmd.preview.render.align.borde_izquierdo";
    public static final String CMD_PREVIEW_RENDER_ALIGN_CENTRO_HORIZONTAL = "cmd.preview.render.align.centro_horizontal";
    public static final String CMD_PREVIEW_RENDER_ALIGN_BORDE_DERECHO    = "cmd.preview.render.align.borde_derecho";

    // --- Distribuir capas ---
    public static final String CMD_PREVIEW_RENDER_DISTRIBUTE_BOTTOM_BORDER   = "cmd.preview.render.distribute.bottom_border";
    public static final String CMD_PREVIEW_RENDER_DISTRIBUTE_CENTER_VERTICAL = "cmd.preview.render.distribute.center_vertical";
    public static final String CMD_PREVIEW_RENDER_DISTRIBUTE_TOP_BORDER      = "cmd.preview.render.distribute.top_border";
    public static final String CMD_PREVIEW_RENDER_DISTRIBUTE_LEFT_BORDER     = "cmd.preview.render.distribute.left_border";
    public static final String CMD_PREVIEW_RENDER_DISTRIBUTE_CENTER_HORIZONTAL = "cmd.preview.render.distribute.center_horizontal";
    public static final String CMD_PREVIEW_RENDER_DISTRIBUTE_RIGHT_BORDER    = "cmd.preview.render.distribute.right_border";

    // --- Distribuir espacios ---
    public static final String CMD_PREVIEW_RENDER_DISTRIBUTE_HORIZONTAL_SPACE = "cmd.preview.render.distribute.horizontal_space";
    public static final String CMD_PREVIEW_RENDER_DISTRIBUTE_VERTICAL_SPACE   = "cmd.preview.render.distribute.vertical_space";

    // --- Auto-distribuir ---
    public static final String CMD_PREVIEW_RENDER_AUTO_DISTRIBUTE_FIXED  = "cmd.preview.render.auto_distribute.fixed";
    public static final String CMD_PREVIEW_RENDER_AUTO_DISTRIBUTE_LAYER  = "cmd.preview.render.auto_distribute.layer";
    public static final String CMD_PREVIEW_RENDER_AUTO_DISTRIBUTE_CANVAS = "cmd.preview.render.auto_distribute.canvas";

    // --- Editor Avanzado: herramientas principales ---
    public static final String CMD_ADVANCED_EDITOR_EDICION             = "cmd.advanced.editor.edicion";
    public static final String CMD_ADVANCED_EDITOR_TRANSFORMAR         = "cmd.advanced.editor.transformar";
    public static final String CMD_ADVANCED_EDITOR_SELECCION_MARCO    = "cmd.advanced.editor.seleccion.marco";
    public static final String CMD_ADVANCED_EDITOR_SELECCION_CAPA     = "cmd.advanced.editor.seleccion.capa";
    public static final String CMD_ADVANCED_EDITOR_VARITA             = "cmd.advanced.editor.varita";
    public static final String CMD_ADVANCED_EDITOR_RECORTAR           = "cmd.advanced.editor.recortar";
    public static final String CMD_ADVANCED_EDITOR_CUENTAGOTAS        = "cmd.advanced.editor.cuentagotas";
    public static final String CMD_ADVANCED_EDITOR_BOTE_PINTURA       = "cmd.advanced.editor.bote_pintura";
    public static final String CMD_ADVANCED_EDITOR_DEGRADADO          = "cmd.advanced.editor.degradado";
    public static final String CMD_ADVANCED_EDITOR_TEXTO              = "cmd.advanced.editor.texto";
    public static final String CMD_ADVANCED_EDITOR_FORMAS             = "cmd.advanced.editor.formas";
    public static final String CMD_ADVANCED_EDITOR_ZOOM                = "cmd.advanced.editor.zoom";
    public static final String CMD_ADVANCED_EDITOR_PANTALLA_COMPLETA = "cmd.advanced.editor.pantalla_completa";
    public static final String CMD_ADVANCED_EDITOR_RESET_COLORS        = "cmd.advanced.editor.reset_colors";
    public static final String CMD_ADVANCED_EDITOR_INVERT_COLORS       = "cmd.advanced.editor.invert_colors";
    public static final String CMD_ADVANCED_EDITOR_PAGE              = "cmd.advanced.editor.page";

    // --- Editor Avanzado: sub-herramientas de transformación ---
    public static final String CMD_ADVANCED_EDITOR_MOVE              = "cmd.advanced.editor.transform.mover";
    public static final String CMD_ADVANCED_EDITOR_SCALE             = "cmd.advanced.editor.transform.escalar";
    public static final String CMD_ADVANCED_EDITOR_ROTATE            = "cmd.advanced.editor.transform.rotar";
    public static final String CMD_ADVANCED_EDITOR_LAYER             = "cmd.advanced.editor.transform.capa";
    public static final String CMD_ADVANCED_EDITOR_MARCO             = "cmd.advanced.editor.transform.marco";

    // --- Editor Avanzado: sub-herramientas de texto ---
    public static final String CMD_EDITOR_TEXTO_NEGRITA              = "cmd.editor.texto.negrita";
    public static final String CMD_EDITOR_TEXTO_CURSIVA              = "cmd.editor.texto.cursiva";
    public static final String CMD_EDITOR_TEXTO_SUBRAYADO            = "cmd.editor.texto.subrayado";
    public static final String CMD_EDITOR_TEXTO_TACHADO              = "cmd.editor.texto.tachado";
    public static final String CMD_EDITOR_TEXTO_ALIGN_LEFT           = "cmd.editor.texto.align.left";
    public static final String CMD_EDITOR_TEXTO_ALIGN_CENTER         = "cmd.editor.texto.align.center";
    public static final String CMD_EDITOR_TEXTO_ALIGN_RIGHT          = "cmd.editor.texto.align.right";
    public static final String CMD_EDITOR_TEXTO_ALIGN_JUSTIFY        = "cmd.editor.texto.align.justify";
    public static final String CMD_EDITOR_TEXTO_FLOW_ROWS            = "cmd.editor.texto.flow.rows";
    public static final String CMD_EDITOR_TEXTO_FLOW_COLUMNS         = "cmd.editor.texto.flow.columns";
    public static final String CMD_EDITOR_TEXTO_HORIZONTAL           = "cmd.editor.texto.horizontal";
    public static final String CMD_EDITOR_TEXTO_VERTICAL             = "cmd.editor.texto.vertical";
    public static final String CMD_EDITOR_TEXTO_FUENTE               = "cmd.editor.texto.fuente";
    public static final String CMD_EDITOR_TEXTO_TAMANO               = "cmd.editor.texto.tamano";

    // --- Editor Avanzado: tipos de zoom ---
    public static final String CMD_ADVANCED_EDITOR_ZOOM_CENTER       = "cmd.advanced.editor.zoom.centro";
    public static final String CMD_ADVANCED_EDITOR_ZOOM_CURSOR       = "cmd.advanced.editor.zoom.cursor";

    // --- Editor Avanzado: modos de recorte ---
    public static final String CMD_ADVANCED_EDITOR_CROP_SELECTION    = "cmd.advanced.editor.crop.seleccion";
    public static final String CMD_ADVANCED_EDITOR_CROP_LAYER        = "cmd.advanced.editor.crop.capa";

    // --- Editor Avanzado: tipos de forma y sus propiedades ---
    public static final String CMD_ADVANCED_EDITOR_SHAPE_RECT        = "cmd.advanced.editor.shape.rect";
    public static final String CMD_ADVANCED_EDITOR_SHAPE_ELLIPSE     = "cmd.advanced.editor.shape.ellipse";
    public static final String CMD_ADVANCED_EDITOR_SHAPE_LINE        = "cmd.advanced.editor.shape.line";
    public static final String CMD_ADVANCED_EDITOR_SHAPE_TRIANGLE    = "cmd.advanced.editor.shape.triangle";
    public static final String CMD_ADVANCED_EDITOR_SHAPE_POLYGON     = "cmd.advanced.editor.shape.polygon";
    public static final String CMD_ADVANCED_EDITOR_FILL_COLOR        = "cmd.advanced.editor.shape.fill_color";
    public static final String CMD_ADVANCED_EDITOR_STROKE_COLOR      = "cmd.advanced.editor.shape.stroke_color";
    public static final String CMD_ADVANCED_EDITOR_STROKE_WIDTH      = "cmd.advanced.editor.shape.stroke_width";

    // --- D-Pad ---
    public static final String CMD_PREVIEW_RENDER_DPAD_TOP_LEFT      = "cmd.preview.render.dpad.top_left";
    public static final String CMD_PREVIEW_RENDER_DPAD_TOP_CENTER    = "cmd.preview.render.dpad.top_center";
    public static final String CMD_PREVIEW_RENDER_DPAD_TOP_RIGHT     = "cmd.preview.render.dpad.top_right";
    public static final String CMD_PREVIEW_RENDER_DPAD_CENTER_LEFT   = "cmd.preview.render.dpad.center_left";
    public static final String CMD_PREVIEW_RENDER_DPAD_CENTER        = "cmd.preview.render.dpad.center";
    public static final String CMD_PREVIEW_RENDER_DPAD_CENTER_RIGHT  = "cmd.preview.render.dpad.center_right";
    public static final String CMD_PREVIEW_RENDER_DPAD_BOTTOM_LEFT   = "cmd.preview.render.dpad.bottom_left";
    public static final String CMD_PREVIEW_RENDER_DPAD_BOTTOM_CENTER = "cmd.preview.render.dpad.bottom_center";
    public static final String CMD_PREVIEW_RENDER_DPAD_BOTTOM_RIGHT  = "cmd.preview.render.dpad.bottom_right";

    public static final String CMD_CONFIG_CUSTOM_THEME				= "cmd.config.custom_theme";
    public static final String CMD_CONFIG_AVANZADA					= "cmd.config.avanzada";

    public static final String CMD_CLIENTE_ABRIR_PRJCL 				= "cmd.cliente.abrir_prjcl";
    public static final String CMD_CLIENTE_UPDATE 					= "cmd.cliente.update";
    public static final String CMD_CLIENTE_EXPORTAR_WEB 			= "cmd.cliente.exportar_web";
    public static final String CMD_CLIENTE_EXPORTAR_HTML 			= "cmd.cliente.exportar_html";
    public static final String CMD_CLIENTE_CARGAR_RESPUESTA 		= "cmd.cliente.cargar_respuesta";
    public static final String CMD_CLIENTE_CERRAR_SINCRONIZAR 		= "cmd.cliente.cerrar_sincronizar";
    public static final String CMD_CLIENTE_CHECKBOX_ADD 			= "cmd.cliente.checkbox.add";
    public static final String CMD_CLIENTE_CHECKBOX_ADD_LABEL 		= "cmd.cliente.checkbox.add_label";
    public static final String CMD_CLIENTE_CHECKBOX_FIN_EDICION 	= "cmd.cliente.checkbox.fin_edicion";
    
    public static final String CMD_CLIENTE_VIEW_VISOR 			    = "cmd.cliente.view_visor";
    public static final String CMD_CLIENTE_TOGGLE_EDITOR_PANEL 		= "cmd.cliente.toggle_editor_panel";
    public static final String CMD_CLIENTE_EDITAR 					= "cmd.cliente.editar";
    public static final String CMD_CLIENTE_EXPORTAR_PDF 			= "cmd.cliente.exportar_pdf";
    
    // --- Comportamiento/Toggles Generales ---
    public static final String CMD_TOGGLE_SUBCARPETAS 				= "cmd.toggle.subcarpetas";             // Para ToggleSubfoldersAction
    public static final String CMD_TOGGLE_MANTENER_PROPORCIONES 	= "cmd.toggle.mantener_proporciones"; 	// Para ToggleProporcionesAction
    public static final String CMD_TOGGLE_WRAP_AROUND 				= "cmd.toggle.wrap_around";           	// Para 'Volver al inicio al llegar al final'
    public static final String CMD_TOGGLE_WELCOME 				    = "cmd.toggle.welcome";               	// Mostrar imagen de bienvenida
    public static final String CMD_TOGGLE_RESTORE_LAST 			    = "cmd.toggle.restore_last_image";    	// Abrir última imagen vista al iniciar
    public static final String CMD_TOGGLE_NAV_ARROWS 			    = "cmd.toggle.nav_arrows";            	// Mostrar flechas de navegación en imagen
    
    // --- Tema ---
    public static final String CMD_TEMA_PREFIX 						= "cmd.tema.";
    
    
//    public static final String CMD_TEMA_CLEAR 						= "cmd.tema.clear";     				// Para ToggleThemeAction("clear",...)
//    public static final String CMD_TEMA_DARK 						= "cmd.tema.dark";      				// Para ToggleThemeAction("dark",...)
//    public static final String CMD_TEMA_BLUE 						= "cmd.tema.blue";      				// etc.
//    public static final String CMD_TEMA_CYBER_BLUE 					= "cmd.tema.cyber_blue";
//    public static final String CMD_TEMA_GREEN						= "cmd.tema.green";
//    public static final String CMD_TEMA_ORANGE 						= "cmd.tema.orange";

    // --- Configuración ---
    public static final String CMD_CONFIG_GUARDAR 					= "cmd.config.guardar";         		// Para menú "Guardar Configuración Actual"
    public static final String CMD_CONFIG_CARGAR_INICIAL 			= "cmd.config.cargar_inicial"; 			// Para menú "Cargar Configuración Inicial"
    public static final String CMD_CONFIG_MOSTRAR_VERSION 			= "cmd.config.mostrar_version";	 		// Para menú "Version"
    public static final String CMD_CONFIG_CARGA_SOLO_CARPETA 		= "cmd.config.carga.solo_carpeta"; 		// Mostrar Solo Carpeta Actual
    public static final String CMD_CONFIG_CARGA_CON_SUBCARPETAS 	= "cmd.config.carga.con_subcarpetas";	// Mostrar Imagenes de Subcarpetas
    
    public static final String DISPLAY_MODE 						= "display.mode"; 						// Para guardar el modo actual (SINGLE_IMAGE, GRID, POLAROID)
    public static final String MINIATURAS_TAMANO_GRID_ANCHO 		= "miniaturas.tamano.grid.ancho"; 		// Para el ancho de miniaturas en Grid
    public static final String MINIATURAS_TAMANO_GRID_ALTO 			= "miniaturas.tamano.grid.alto";		// Para el ancho de miniaturas en Grid
    
    // --- Especiales / Otros ---
    public static final String CMD_ESPECIAL_REFRESCAR 				= "cmd.especial.refrescar";      		// Para RefreshAction (si es necesaria además de recargar lista)
    public static final String CMD_ESPECIAL_REFRESCAR_UI			= "cmd.especial.refrescar_ui";			// Para Refrescar la UI
    public static final String CMD_ESPECIAL_MENU 					= "cmd.especial.menu";             		// Para MenuAction (si hace algo específico)
    public static final String CMD_ESPECIAL_BOTONES_OCULTOS 		= "cmd.especial.botones_ocultos"; 		// Para HiddenButtonsAction

    // --- Favoritos (Futuro) ---
    public static final String CMD_FAVORITO_TOGGLE 					= "cmd.favorito.toggle";       			// Para botón/menú Añadir/Quitar Favorito
    public static final String CMD_FAVORITO_MOSTRAR_LISTA 			= "cmd.favorito.mostrar"; 				// Para botón/menú Ver lista de Favoritos

    // --- Comandos para Configurar Visibilidad y Formato de Barras de Información ---

    // -- Checkboxes para visibilidad de barras completas --
    public static final String CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_VISIBLE = "cmd.infobar.config.toggle.superior.visible";
    public static final String CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_VISIBLE = "cmd.infobar.config.toggle.inferior.visible";

    // -- Checkboxes para visibilidad de elementos en Barra Superior --
    public static final String CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_NOMBRE_RUTA 		= "cmd.infobar.config.toggle.superior.nombre_ruta";
    public static final String CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_INDICE_TOTAL 		= "cmd.infobar.config.toggle.superior.indice_total";
    public static final String CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_DIMENSIONES 		= "cmd.infobar.config.toggle.superior.dimensiones";
    public static final String CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_TAMANO_ARCHIVO 	= "cmd.infobar.config.toggle.superior.tamano_archivo";
    public static final String CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_FECHA_ARCHIVO 	= "cmd.infobar.config.toggle.superior.fecha_archivo";
    public static final String CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_FORMATO_IMAGEN 	= "cmd.infobar.config.toggle.superior.formato_imagen"; // Para futuro
    public static final String CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_MODO_ZOOM 		= "cmd.infobar.config.toggle.superior.modo_zoom";
    public static final String CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_ZOOM_REAL_PCT 	= "cmd.infobar.config.toggle.superior.zoom_real_pct";

    // -- RadioButtons para formato de Nombre/Ruta en Barra Superior --
    public static final String CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_SOLO_NOMBRE 		= "cmd.infobar.config.formato.superior.nombre_ruta.solo_nombre";
    public static final String CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_RUTA_COMPLETA 	= "cmd.infobar.config.formato.superior.nombre_ruta.ruta_completa";

    // -- Checkboxes para visibilidad de elementos en Barra Inferior --
    public static final String CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_NOMBRE_RUTA 	= "cmd.infobar.config.toggle.inferior.nombre_ruta";
    public static final String CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_ZM 		= "cmd.infobar.config.toggle.inferior.icono_zm";
    public static final String CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_PROP 	= "cmd.infobar.config.toggle.inferior.icono_prop";
    public static final String CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_SUBC 	= "cmd.infobar.config.toggle.inferior.icono_subc";
    public static final String CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_CTRL_ZOOM_PCT = "cmd.infobar.config.toggle.inferior.ctrl_zoom_pct";
    public static final String CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_MENSAJES_APP 	= "cmd.infobar.config.toggle.inferior.mensajes_app";
    public static final String CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_CTRL_MODO_ZOOM = "cmd.infobar.config.toggle.inferior.ctrl_modo_zoom";

    // -- RadioButtons para formato de Nombre/Ruta en Barra Inferior --
    public static final String CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_SOLO_NOMBRE = "cmd.infobar.config.formato.inferior.nombre_ruta.solo_nombre";
    public static final String CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_RUTA_COMPLETA = "cmd.infobar.config.formato.inferior.nombre_ruta.ruta_completa";
    
    
    
    
    
    public static final String CMD_REFRESH_TOOLBARS = "cmd.refresh.toolbars";
    
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
    public static final String CMD_PAN_TOP_EDGE 	= "cmd.pan.top_edge";
    public static final String CMD_PAN_BOTTOM_EDGE 	= "cmd.pan.bottom_edge";
    public static final String CMD_PAN_LEFT_EDGE 	= "cmd.pan.left_edge";
    public static final String CMD_PAN_RIGHT_EDGE 	= "cmd.pan.right_edge";

    // Paneo Incremental (pequeños pasos)
    public static final String CMD_PAN_UP_INCREMENTAL 	= "cmd.pan.up_incremental";
    public static final String CMD_PAN_DOWN_INCREMENTAL = "cmd.pan.down_incremental";
    public static final String CMD_PAN_LEFT_INCREMENTAL = "cmd.pan.left_incremental";
    public static final String CMD_PAN_RIGHT_INCREMENTAL= "cmd.pan.right_incremental";

    // --- Comandos para Control de Fondo de Imagen (NUEVOS) ---
    public static final String CMD_BACKGROUND_THEME_COLOR  	= "cmd.background.color.theme_default";
    public static final String CMD_BACKGROUND_COLOR_SLOT_1 	= "cmd.background.color.slot_1";
    public static final String CMD_BACKGROUND_COLOR_SLOT_2 	= "cmd.background.color.slot_2";
    public static final String CMD_BACKGROUND_COLOR_SLOT_3 	= "cmd.background.color.slot_3";
    public static final String CMD_BACKGROUND_COLOR_SLOT_4 	= "cmd.background.color.slot_4";
    public static final String CMD_BACKGROUND_CHECKERED     = "cmd.background.checkered";
    public static final String CMD_BACKGROUND_CUSTOM_COLOR  = "cmd.background.custom_color";
    
    // --- Home Mode (Editor Avanzado) ---
    public static final String CMD_HOME_CASA               = "cmd.home.casa";
    public static final String CMD_HOME_LIENZO             = "cmd.home.lienzo";
    public static final String CMD_HOME_CONTROL_IMAGEN     = "cmd.home.control_imagen";
    public static final String CMD_HOME_LIENZO_NUEVO       = "cmd.home.lienzo.nuevo";
    public static final String CMD_HOME_LIENZO_MEDIDAS     = "cmd.home.lienzo.medidas";
    public static final String CMD_HOME_LIENZO_AJUSTAR_CONTENIDO = "cmd.home.lienzo.ajustar_contenido";
    public static final String CMD_HOME_ACEPTAR            = "cmd.home.aceptar";
    public static final String CMD_HOME_CANCELAR           = "cmd.home.cancelar";
    public static final String CMD_HOME_IMPORTAR           = "cmd.home.importar";
    public static final String CMD_HOME_EXPORTAR           = "cmd.home.exportar";
    public static final String CMD_HOME_EXPORTAR_PREVIEW   = "cmd.home.exportar_preview";
    
} // --- FIN DE LA CLASE AppActionCommand ---