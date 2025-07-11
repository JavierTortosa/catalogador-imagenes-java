package vista.config;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import controlador.commands.AppActionCommands;
import modelo.VisorModel.WorkMode;
import servicios.ConfigKeys;

public class UIDefinitionService {

    public List<MenuItemDefinition> generateMenuStructure() {
        List<MenuItemDefinition> menuBarStructure = new ArrayList<>();

        // --- SECCIÓN 1: MENÚ "ARCHIVO" ---
        List<MenuItemDefinition> archivoSubItems = List.of(
            new MenuItemDefinition(AppActionCommands.CMD_ARCHIVO_ABRIR, 
            		MenuItemType.ITEM, "Abrir Archivo...", null),
            new MenuItemDefinition(AppActionCommands.CMD_ARCHIVO_ABRIR_NUEVA_VENTANA, 
            		MenuItemType.ITEM, "Abrir en ventana nueva", null),
            new MenuItemDefinition(AppActionCommands.CMD_ARCHIVO_GUARDAR, 
            		MenuItemType.ITEM, "Guardar", null), // Placeholder
            new MenuItemDefinition(AppActionCommands.CMD_ARCHIVO_GUARDAR_COMO, 
            		MenuItemType.ITEM, "Guardar Como...", null), // Placeholder
            new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 
            		MenuItemType.ITEM, "Abrir Con...", null),
            new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 
            		MenuItemType.ITEM, "Editar Imagen", null),
            new MenuItemDefinition(AppActionCommands.CMD_ARCHIVO_IMPRIMIR, 
            		MenuItemType.ITEM, "Imprimir...", null), // Placeholder
            new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 
            		MenuItemType.ITEM, "Compartir", null),
            new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_ESPECIAL_REFRESCAR, 
            		MenuItemType.ITEM, "Refrescar Lista", null),
            new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 
            		MenuItemType.ITEM, "Recargar Imagen Actual", null),
            new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 
            		MenuItemType.ITEM, "Descargar Imagen", null),
            new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_ARCHIVO_SALIR, 
            		MenuItemType.ITEM, "Salir", null)
        );
        menuBarStructure.add(new MenuItemDefinition(null, 
        		MenuItemType.MAIN_MENU, "Archivo", archivoSubItems));

        // --- SECCIÓN 2: MENÚ "NAVEGACIÓN" ---
        List<MenuItemDefinition> navSubItems = List.of(
            new MenuItemDefinition(AppActionCommands.CMD_NAV_PRIMERA, 
            		MenuItemType.ITEM, "Primera Imagen", null),
            new MenuItemDefinition(AppActionCommands.CMD_NAV_ANTERIOR, 
            		MenuItemType.ITEM, "Imagen Anterior", null),
            new MenuItemDefinition(AppActionCommands.CMD_NAV_SIGUIENTE, 
            		MenuItemType.ITEM, "Siguiente Imagen", null),
            new MenuItemDefinition(AppActionCommands.CMD_NAV_ULTIMA, 
            		MenuItemType.ITEM, "Ultima Imagen", null),
            new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_NAV_IR_A, 
            		MenuItemType.ITEM, "Ir a...", null),
            new MenuItemDefinition(AppActionCommands.CMD_NAV_BUSCAR, 
            		MenuItemType.ITEM, "Buscar...", null),
            new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_NAV_FOTOGRAMA_ANTERIOR, 
            		MenuItemType.ITEM, "Anterior Fotograma (GIF)", null),
            new MenuItemDefinition(AppActionCommands.CMD_NAV_FOTOGRAMA_SIGUIENTE, 
            		MenuItemType.ITEM, "Siguiente Fotograma (GIF)", null),
            new MenuItemDefinition(AppActionCommands.CMD_NAV_FOTOGRAMA_PRIMERO, 
            		MenuItemType.ITEM, "Primer Fotograma (GIF)", null),
            new MenuItemDefinition(AppActionCommands.CMD_NAV_FOTOGRAMA_ULTIMO, 
            		MenuItemType.ITEM, "Último Fotograma (GIF)", null)
        );
        menuBarStructure.add(new MenuItemDefinition(null, 
        		MenuItemType.MAIN_MENU, "Navegación", navSubItems));

        // --- SECCIÓN 3: MENÚ "ZOOM" ---
        List<MenuItemDefinition> tiposZoomSubItems = List.of(
            new MenuItemDefinition(null, 
            		MenuItemType.RADIO_GROUP_START, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR, 
            		MenuItemType.RADIO_BUTTON_ITEM, "Ajustar a Pantalla", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_TIPO_AUTO, 
            		MenuItemType.RADIO_BUTTON_ITEM, "Tamaño Original (100%)", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_TIPO_ANCHO, 
            		MenuItemType.RADIO_BUTTON_ITEM, "Ajustar a Ancho", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_TIPO_ALTO, 
            		MenuItemType.RADIO_BUTTON_ITEM, "Ajustar a Alto", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO, 
            		MenuItemType.RADIO_BUTTON_ITEM, "Zoom Personalizado %", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_TIPO_FIJO, 
            		MenuItemType.RADIO_BUTTON_ITEM, "Mantener Zoom Actual", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_TIPO_RELLENAR, 
            		MenuItemType.RADIO_BUTTON_ITEM, "Escalar Para Rellenar", null), // Futuro
            new MenuItemDefinition(null, 
            		MenuItemType.RADIO_GROUP_END, null, null)
        );
        
	        List<MenuItemDefinition> tiposPaneoSubItems = List.of(
	    			new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 
	                		MenuItemType.ITEM, "Ver Parte Superior", null),
	    			new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 
	                		MenuItemType.ITEM, "Ver Parte Inferior", null),
	    			new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 
	                		MenuItemType.ITEM, "Ver Parte Izquierda", null),
	    			new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 
	                		MenuItemType.ITEM, "Ver Parte Derecha", null)
			);
        
        List<MenuItemDefinition> zoomSubItems = List.of(
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_ACERCAR, 
            		MenuItemType.ITEM, "Acercar", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_ALEJAR, 
            		MenuItemType.ITEM, "Alejar", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_PERSONALIZADO, 
            		MenuItemType.ITEM, "Establecer Zoom %...", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_TAMAÑO_REAL, 
            		MenuItemType.ITEM, "Zoom Tamaño Real (100%)", null),
            new MenuItemDefinition(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES, 
            		MenuItemType.CHECKBOX_ITEM, "Mantener Proporciones", null),
            new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE, 
            		MenuItemType.CHECKBOX_ITEM, "Activar Zoom Manual", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_TOGGLE_TO_CURSOR, 
                    MenuItemType.CHECKBOX_ITEM, "Zoom al Cursor", null),
            new MenuItemDefinition(null, 
            		MenuItemType.SUB_MENU, "Paneo", tiposPaneoSubItems),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_RESET, 
            		MenuItemType.ITEM, "Resetear Zoom", null),
            new MenuItemDefinition(null,
            		MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(null, 
            		MenuItemType.SUB_MENU, "Ajuste Visual", tiposZoomSubItems)
        );
        menuBarStructure.add(new MenuItemDefinition(null, 
        		MenuItemType.MAIN_MENU, "Zoom", zoomSubItems));

        
        // --- SECCIÓN 4: MENÚ "IMAGEN" ---
        List<MenuItemDefinition> edicionSubItems = List.of(
            new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_ROTAR_IZQ, 
            		MenuItemType.ITEM, "Girar Izquierda", null),
            new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_ROTAR_DER, 
            		MenuItemType.ITEM, "Girar Derecha", null),
            new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_VOLTEAR_H, 
            		MenuItemType.ITEM, "Voltear Horizontal", null),
            new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_VOLTEAR_V, 
            		MenuItemType.ITEM, "Voltear Vertical", null),
            new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_RECORTAR, 
            		MenuItemType.ITEM, "Recortar", null) // Placeholder
        );
        	
        
        
        List<MenuItemDefinition> imagenSubItems = List.of(
        	new MenuItemDefinition(null, 
        			MenuItemType.SUB_MENU, "Edición", edicionSubItems),
            new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_RENOMBRAR, 
            		MenuItemType.ITEM, "Cambiar Nombre de Imagen...", null),
            new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_MOVER_PAPELERA, 
            		MenuItemType.ITEM, "Mover a Papelera", null),
            new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_ELIMINAR, 
            		MenuItemType.ITEM, "Eliminar Permanentemente...", null),
            new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_FONDO_ESCRITORIO, 
            		MenuItemType.ITEM, "Establecer Como Fondo de Escritorio", null),
            new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_FONDO_BLOQUEO, 
            		MenuItemType.ITEM, "Establecer Como Imagen de Bloqueo", null),
            new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_LOCALIZAR, 
            		MenuItemType.ITEM, "Abrir Ubicación del Archivo", null),
            new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_PROPIEDADES, 
            		MenuItemType.ITEM, "Propiedades de la imagen...", null)
        );
        menuBarStructure.add(new MenuItemDefinition(null, 
        		MenuItemType.MAIN_MENU, "Imagen", imagenSubItems));

        // --- SECCIÓN 5: MENÚ "VISTA" ---
        List<MenuItemDefinition> vistaSubItems = List.of(
            new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_MENU_BAR, 
            		MenuItemType.CHECKBOX_ITEM, "Barra de Menú", null),
            new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_TOOL_BAR, 
            		MenuItemType.CHECKBOX_ITEM, "Barra de Botones", null),
            new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_FILE_LIST, 
            		MenuItemType.CHECKBOX_ITEM, "Lista de Archivos", null),
            new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_THUMBNAILS, 
            		MenuItemType.CHECKBOX_ITEM, "Barra de Miniaturas", null),
            // new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_LOCATION_BAR, MenuItemType.CHECKBOX_ITEM, "Barra de Ubicación", null), // Integrado en Barra Inferior
            new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_LOCATION_BAR, 
            		MenuItemType.CHECKBOX_ITEM, "Barra de Estado", null),
            new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_CHECKERED_BG, 
            		MenuItemType.CHECKBOX_ITEM, "Fondo a Cuadros", null),
            new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP, 
            		MenuItemType.CHECKBOX_ITEM, "Mantener Ventana Siempre Encima", null),
            new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_MINIATURE_TEXT, 
            		MenuItemType.CHECKBOX_ITEM, "Mostrar Nombres en Miniaturas", null),
            new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_VISTA_MOSTRAR_DIALOGO_LISTA, 
            		MenuItemType.ITEM, "Mostrar Diálogo Lista de Imágenes...", null)
        );
        menuBarStructure.add(new MenuItemDefinition(null, 
        		MenuItemType.MAIN_MENU, "Vista", vistaSubItems));

        
        // --- NUEVA SECCIÓN: MENÚ "MODO" ---
        List<MenuItemDefinition> modoSubItems = List.of(
            new MenuItemDefinition(null, 
            		MenuItemType.RADIO_GROUP_START, null, null), // Inicia el grupo de radios
            new MenuItemDefinition(AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR, 
            		MenuItemType.RADIO_BUTTON_ITEM, "Modo Visualizador", null),
            new MenuItemDefinition(AppActionCommands.CMD_PROYECTO_GESTIONAR, 
            		MenuItemType.RADIO_BUTTON_ITEM, "Modo Proyecto", null),
            new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null), // Separador visual
            new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 
            		MenuItemType.RADIO_BUTTON_ITEM, "Modo Datos", null),
            new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 
            		MenuItemType.RADIO_BUTTON_ITEM, "Modo Edición", null),
            new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 
            		MenuItemType.RADIO_BUTTON_ITEM, "Modo Carrusel", null),
            new MenuItemDefinition(null, 
            		MenuItemType.RADIO_GROUP_END, null, null) // Finaliza el grupo de radios
        );
        menuBarStructure.add(new MenuItemDefinition(null, 
        		MenuItemType.MAIN_MENU, "Modo", modoSubItems));
        
        
        // --- SECCIÓN 6: MENÚ "PROYECTO" ---
        List<MenuItemDefinition> proyectoSubItems = List.of(
            new MenuItemDefinition(AppActionCommands.CMD_PROYECTO_GESTIONAR, 
            		MenuItemType.ITEM, "Gestionar Proyecto Actual...", null),
            new MenuItemDefinition(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA, 
            		MenuItemType.CHECKBOX_ITEM, "Marcar para Proyecto", null),
            new MenuItemDefinition(null,//AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE,
            		MenuItemType.ITEM, "Vista Rapida de Imagenes Seleccionadas", null)
            
        );
        menuBarStructure.add(new MenuItemDefinition(null, 
        		MenuItemType.MAIN_MENU, "Proyecto", proyectoSubItems));

        // --- SECCIÓN 7: MENÚ "CONFIGURACIÓN" ---
        List<MenuItemDefinition> configSubItems = new ArrayList<>();

        // 7.2. Submenú "Carga de Imágenes"
        List<MenuItemDefinition> ordenCriterioSubItems = List.of(
            	new MenuItemDefinition(null, 
            			MenuItemType.RADIO_GROUP_START, null, null),
                new MenuItemDefinition("cmd.orden.criterio.nombre", 
                		MenuItemType.RADIO_BUTTON_ITEM, "Nombre por Defecto", null),
                new MenuItemDefinition("cmd.orden.criterio.tamano", 
                		MenuItemType.RADIO_BUTTON_ITEM, "Tamaño de Archivo", null),
                new MenuItemDefinition("cmd.orden.criterio.fecha", 
                		MenuItemType.RADIO_BUTTON_ITEM, "Fecha de Creacion", null),
                new MenuItemDefinition("cmd.orden.criterio.extension", 
                		MenuItemType.RADIO_BUTTON_ITEM, "Extension", null),
                new MenuItemDefinition(null, 
                		MenuItemType.RADIO_GROUP_END, null, null)
                );
                
        List<MenuItemDefinition> ordenDireccionSubItems = List.of(
            new MenuItemDefinition(null, 
            		MenuItemType.RADIO_GROUP_START, null, null),
	        new MenuItemDefinition("cmd.orden.direccion.ninguno", 
	        		MenuItemType.RADIO_BUTTON_ITEM, "Sin Ordenar", null), // ¿Necesario si ya hay criterio?
	        new MenuItemDefinition("cmd.orden.direccion.asc", 
	        		MenuItemType.RADIO_BUTTON_ITEM, "Ascendente", null),
	        new MenuItemDefinition("cmd.orden.direccion.desc", 
	        		MenuItemType.RADIO_BUTTON_ITEM, "Descendente", null),
	        new MenuItemDefinition(null, 
	        		MenuItemType.RADIO_GROUP_END, null, null)
	        );
    	        
        List<MenuItemDefinition> cargaOrdenSubItems = new ArrayList<>(); // Usamos ArrayList para añadir
	        cargaOrdenSubItems.addAll(ordenCriterioSubItems);
	        cargaOrdenSubItems.add(new MenuItemDefinition(null, 
	        		MenuItemType.SEPARATOR, null, null));
	        cargaOrdenSubItems.addAll(ordenDireccionSubItems);
        
        List<MenuItemDefinition> configCargaImgSubItems = List.of(
            new MenuItemDefinition(null, 
            		MenuItemType.RADIO_GROUP_START, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA, 
            		MenuItemType.RADIO_BUTTON_ITEM, "Mostrar Solo Carpeta Actual", null),
            new MenuItemDefinition(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS, 
            		MenuItemType.RADIO_BUTTON_ITEM, "Mostrar Imágenes de Subcarpetas", null),
            new MenuItemDefinition(null, 
            		MenuItemType.RADIO_GROUP_END, null, null),
            new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(null, 
            		MenuItemType.SUB_MENU, "Orden Visual", cargaOrdenSubItems)
        );
        configSubItems.add(new MenuItemDefinition(null, 
        		MenuItemType.SUB_MENU, "Carga de Imágenes", configCargaImgSubItems));
        configSubItems.add(new MenuItemDefinition(null, 
        		MenuItemType.SEPARATOR, null, null));

        // 7.3. Submenú "General" (Configuraciones de comportamiento)
        List<MenuItemDefinition> configGeneralSubItems = List.of(
            new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 
            		MenuItemType.CHECKBOX_ITEM, "Mostrar Imagen de Bienvenida", null),
            new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 
            		MenuItemType.CHECKBOX_ITEM, "Abrir Ultima Imagen Vista al Iniciar", null),
            new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_TOGGLE_WRAP_AROUND, 
            		MenuItemType.CHECKBOX_ITEM, "Navegación Circular (Wrap Around)", null),
            new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 
            		MenuItemType.CHECKBOX_ITEM, "Mostrar Flechas de Navegación en Imagen", null)
        );
        configSubItems.add(new MenuItemDefinition(null, 
        		MenuItemType.SUB_MENU, "Comportamiento General", configGeneralSubItems));
        configSubItems.add(new MenuItemDefinition(null, 
        		MenuItemType.SEPARATOR, null, null));
       
     // 7.4. Submenú "Visualizar Botones en Toolbar"
        // Menu inteligente para configurar los botones visibles de las toolbars
        List<MenuItemDefinition> configHerramientasSubItems = new ArrayList<>();

	    // Obtenemos la estructura modular que ya definiste
	    List<ToolbarDefinition> todasLasBarras = generateModularToolbarStructure();
	
	    // Iteramos sobre cada barra para crear su sección en el menú
	    for (ToolbarDefinition barra : todasLasBarras) {
	
	        // Creamos la lista de checkboxes para los botones de ESTA barra
	        List<MenuItemDefinition> checkboxesDeBotones = new ArrayList<>();
	        for (ToolbarButtonDefinition boton : barra.botones()) {
	            // Generamos la clave BASE del botón.
	            String claveBaseBoton = ConfigKeys.buildKey(
	                "interfaz.boton", // O el prefijo correcto
	                barra.claveBarra(),
	                extraerNombreClave(boton.comandoCanonico())
	            );
	            
	            checkboxesDeBotones.add(
	                new MenuItemDefinition(claveBaseBoton, // <-- El ActionCommand ahora es la clave BASE
	                    MenuItemType.CHECKBOX_ITEM, "  " + boton.textoTooltip(),null)
	            );
	        }
	        
	        // Creamos la clave de configuración para la visibilidad de la barra completa
	        String claveConfigBarra = ConfigKeys.buildKey("interfaz.herramientas", barra.claveBarra());
	
	        // Creamos el submenú para esta barra
	        configHerramientasSubItems.add(
	            new MenuItemDefinition(claveConfigBarra, 
	            		MenuItemType.CHECKBOX_ITEM_WITH_SUBMENU, "Barra de " + barra.titulo(), checkboxesDeBotones)
	        );
	    }
	
	     // Añadimos el submenú "Herramientas" completo al menú de Configuración
	    configSubItems.add(new MenuItemDefinition(null, 
	    		MenuItemType.SUB_MENU, "Herramientas", configHerramientasSubItems));
	    configSubItems.add(new MenuItemDefinition(null, 
	    		MenuItemType.SEPARATOR, null, null));
        
	    
        // --- 7.6. SUBMENÚ: "CONFIGURAR VISIBILIDAD DE BARRAS DE INFORMACIÓN" ---
        List<MenuItemDefinition> configVisibilidadBarrasSubItems = new ArrayList<>();

            // --- 7.6.1. Sub-Submenú para Barra de Información Superior ---
            List<MenuItemDefinition> barraSuperiorVisSubItems = new ArrayList<>();
            barraSuperiorVisSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_VISIBLE,// Checkbox para visibilidad de toda la barra superior
                	MenuItemType.CHECKBOX_ITEM, "Mostrar Panel de Info.", null
            ));
            barraSuperiorVisSubItems.add(new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null)); // Separador

            // Checkbox para visibilidad de Nombre/Ruta (Superior)
            barraSuperiorVisSubItems.add(new MenuItemDefinition(
                AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_NOMBRE_RUTA,
                	MenuItemType.CHECKBOX_ITEM, "Nombre/Ruta Archivo", null
            ));
            // Submenú para el formato de Nombre/Ruta (Superior)
            List<MenuItemDefinition> formatoNombreRutaSuperiorSubItems = List.of(
                new MenuItemDefinition(null, 
                		MenuItemType.RADIO_GROUP_START, null, null),
                new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_SOLO_NOMBRE,
                		MenuItemType.RADIO_BUTTON_ITEM, "Solo Nombre de Archivo", null),
                new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_RUTA_COMPLETA,
                		MenuItemType.RADIO_BUTTON_ITEM, "Ruta Completa y Nombre", null),
                new MenuItemDefinition(null, 
                		MenuItemType.RADIO_GROUP_END, null, null)
            );
            barraSuperiorVisSubItems.add(new MenuItemDefinition(null, 
            		MenuItemType.SUB_MENU,
                "   Formato", formatoNombreRutaSuperiorSubItems
            ));
            barraSuperiorVisSubItems.add(new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null)); // Separador

            // Checkboxes para los demás elementos de la barra superior
            barraSuperiorVisSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_INDICE_TOTAL,
            		MenuItemType.CHECKBOX_ITEM, "Índice/Total Imágenes", null));
            barraSuperiorVisSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_DIMENSIONES,
            		MenuItemType.CHECKBOX_ITEM, "Dimensiones Originales", null));
            barraSuperiorVisSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_TAMANO_ARCHIVO,
            		MenuItemType.CHECKBOX_ITEM, "Tamaño de Archivo", null));
            barraSuperiorVisSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_FECHA_ARCHIVO,
            		MenuItemType.CHECKBOX_ITEM, "Fecha de Archivo", null));
            barraSuperiorVisSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_FORMATO_IMAGEN,
            		MenuItemType.CHECKBOX_ITEM, "Formato de Imagen", null));
            barraSuperiorVisSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_MODO_ZOOM,
            		MenuItemType.CHECKBOX_ITEM, "Modo de Zoom", null));
            barraSuperiorVisSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_ZOOM_REAL_PCT,
            		MenuItemType.CHECKBOX_ITEM, "% Zoom Real", null));

            // Añadir el submenú de la barra superior al menú principal de "Configurar Visibilidad Barras"
            configVisibilidadBarrasSubItems.add(new MenuItemDefinition(null, 
            		MenuItemType.SUB_MENU,
                "Información de Imagen", barraSuperiorVisSubItems));
            configVisibilidadBarrasSubItems.add(new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null));


            // --- 7.6.2. Sub-Submenú para Barra de Estado/Control Inferior ---
            List<MenuItemDefinition> barraInferiorVisSubItems = new ArrayList<>();
            barraInferiorVisSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_VISIBLE,// Checkbox para visibilidad de toda la barra inferior
                	MenuItemType.CHECKBOX_ITEM, "Mostrar Panel de Control", null
            ));
            barraInferiorVisSubItems.add(new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null)); // Separador

            // Checkbox para visibilidad de Nombre/Ruta (Inferior)
            barraInferiorVisSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_NOMBRE_RUTA,
                	MenuItemType.CHECKBOX_ITEM, "Nombre/Ruta Archivo", null
            ));
            // Submenú para el formato de Nombre/Ruta (Inferior)
            List<MenuItemDefinition> formatoNombreRutaInferiorSubItems = List.of(
                new MenuItemDefinition(null, 
                		MenuItemType.RADIO_GROUP_START, null, null),
                new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_SOLO_NOMBRE,
                		MenuItemType.RADIO_BUTTON_ITEM, "Solo Nombre de Archivo", null),
                new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_RUTA_COMPLETA,
                		MenuItemType.RADIO_BUTTON_ITEM, "Ruta Completa y Nombre", null),
                new MenuItemDefinition(null, 
                		MenuItemType.RADIO_GROUP_END, null, null)
            );
            barraInferiorVisSubItems.add(new MenuItemDefinition(null, 
            		MenuItemType.SUB_MENU,"   Formato", formatoNombreRutaInferiorSubItems
            ));
            barraInferiorVisSubItems.add(new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null)); // Separador

            // Checkboxes para los demás elementos de la barra inferior
            barraInferiorVisSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_ZM,
            		MenuItemType.CHECKBOX_ITEM, "Icono Zoom Manual", null));
            barraInferiorVisSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_PROP,
            		MenuItemType.CHECKBOX_ITEM, "Icono Proporciones", null));
            barraInferiorVisSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_SUBC,
            		MenuItemType.CHECKBOX_ITEM, "Icono Subcarpetas", null));
            barraInferiorVisSubItems.add(new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null)); // Separador
            barraInferiorVisSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_CTRL_ZOOM_PCT,
            		MenuItemType.CHECKBOX_ITEM, "Control % Zoom ", null));
            barraInferiorVisSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_CTRL_MODO_ZOOM,
            		MenuItemType.CHECKBOX_ITEM, "Control Modo Zoom", null));
            barraInferiorVisSubItems.add(new MenuItemDefinition(null, 
            		MenuItemType.SEPARATOR, null, null)); // Separador
            barraInferiorVisSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_MENSAJES_APP,
            		MenuItemType.CHECKBOX_ITEM, "Area de Mensajes", null));

            // Añadir el submenú de la barra inferior al menú principal de "Configurar Visibilidad Barras"
            configVisibilidadBarrasSubItems.add(new MenuItemDefinition(null, MenuItemType.SUB_MENU,
                "Estado y Control", barraInferiorVisSubItems));

        // 7.6.3. Añadir el submenú "Configurar Visibilidad Barras" al menú "Configuración"
        configSubItems.add(new MenuItemDefinition(null, MenuItemType.SUB_MENU,"Paneles de Datos", configVisibilidadBarrasSubItems));
        // --- FIN DE LA SECCIÓN PARA CONFIGURAR VISIBILIDAD DE BARRAS ---
        configSubItems.add(new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null));


        // 7.5. Submenú "Tema" (movido después de visibilidad de barras por orden lógico)
        List<MenuItemDefinition> configTemaSubItems = List.of(
            new MenuItemDefinition(null, MenuItemType.RADIO_GROUP_START, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_TEMA_CLEAR, MenuItemType.RADIO_BUTTON_ITEM, 	"Tema Clear", null),
            new MenuItemDefinition(AppActionCommands.CMD_TEMA_DARK, MenuItemType.RADIO_BUTTON_ITEM, 	"Tema Dark", null),
            new MenuItemDefinition(AppActionCommands.CMD_TEMA_BLUE, MenuItemType.RADIO_BUTTON_ITEM, 	"Tema Blue", null),
            new MenuItemDefinition(AppActionCommands.CMD_TEMA_ORANGE, MenuItemType.RADIO_BUTTON_ITEM, 	"Tema Orange", null),
            new MenuItemDefinition(AppActionCommands.CMD_TEMA_GREEN, MenuItemType.RADIO_BUTTON_ITEM, 	"Tema Green", null),
            new MenuItemDefinition(null, MenuItemType.RADIO_GROUP_END, null, null)
        );
        configSubItems.add(new MenuItemDefinition(null, MenuItemType.SUB_MENU, "Tema", configTemaSubItems));
        configSubItems.add(new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null));


        // 7.7. Ítems finales del menú "Configuración" (Guardar, Cargar, Versión)
        configSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_ESPECIAL_REFRESCAR, MenuItemType.ITEM, "Refrescar UI y Lista", null));
        configSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_CONFIG_GUARDAR, MenuItemType.ITEM, "Guardar Configuración Actual", null));
        configSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_CONFIG_CARGAR_INICIAL, MenuItemType.ITEM, "Restaurar Configuración Inicial", null));
        configSubItems.add(new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null));
        configSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_CONFIG_MOSTRAR_VERSION, MenuItemType.ITEM, "Acerca de...", null));

        // 7.8. Añadir el menú "Configuración" a la barra de menú principal
        menuBarStructure.add(new MenuItemDefinition(null, MenuItemType.MAIN_MENU, "Configuración", configSubItems));

        // --- SECCIÓN 8: FIN DE LA DEFINICIÓN DE TODOS LOS MENÚS ---
        return menuBarStructure;
    }// --- FIN del metodo generateMenuStructure ---


    // --- Método para definir la estructura de barras de herramientas modulares ---
    /**
     * Define la estructura completa de todas las barras de herramientas modulares.
     * Cada `ToolbarDefinition` representa una barra de herramientas temática que
     * contiene una lista de botones.
     *
     * @return Una lista de objetos ToolbarDefinition.
     */
    public List<ToolbarDefinition> generateModularToolbarStructure() {

    	// --- BARRA DE NAVEGACIÓN ---
		List<ToolbarButtonDefinition> botonesNavegacion = List.of(	
             new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_PRIMERA, 				"1001-Primera_48x48.png", "Primera Imagen", "movimiento")
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_ANTERIOR, 				"1002-Anterior_48x48.png", "Imagen Anterior", "movimiento")
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_SIGUIENTE, 				"1003-Siguiente_48x48.png", "Imagen Siguiente", "movimiento")
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_ULTIMA, 					"1004-Ultima_48x48.png", "Última Imagen", "movimiento")
        );
		
		
		// --- BARRA DE EDICIÓN ---
		List<ToolbarButtonDefinition> botonesEdicion = List.of(
             new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_ROTAR_IZQ,			"2001-Rotar_Izquierda_48x48.png", "Rotar Izquierda", "edicion")
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_ROTAR_DER, 			"2002-Rotar_Derecha_48x48.png", "Rotar Derecha", "edicion")
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_VOLTEAR_H, 			"2003-Espejo_Horizontal_48x48.png", "Voltear Horizontal", "edicion")
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_VOLTEAR_V, 			"2004-Espejo_Vertical_48x48.png", "Voltear Vertical", "edicion")
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_RECORTAR, 			"2005-Recortar_48x48.png", "Recortar", "edicion")
        );

		
		// --- BARRA DE ZOOM ---
		List<ToolbarButtonDefinition> botonesZoom = List.of(
            new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR, 			"3005-Escalar_Para_Ajustar_48x48.png", "Escalar para Ajustar", "zoom")
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_AUTO, 				"3002-Zoom_Auto_48x48.png", "Zoom Automático", "zoom")
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_ANCHO, 				"3003-Ajustar_al_Ancho_48x48.png", "Ajustar al Ancho", "zoom")
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_ALTO, 				"3004-Ajustar_al_Alto_48x48.png", "Ajustar al Alto", "zoom")
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_RELLENAR, 			"3009-rellenar_48x48.png", "Rellenar Zoom", "zoom")
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_FIJO, 				"3006-Zoom_Fijo_48x48.png", "Zoom Actual Fijo", "zoom")
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO, 		"3007-zoom_especifico_48x48.png", "Zoom Especificado", "zoom")
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE, 			"3001-Zoom_48x48.png", "Activar/Desactivar Zoom Manual", "zoom")//, ButtonType.TOGGLE)
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_RESET, 					"3008-Reset_48x48.png", "Resetear Zoom", "zoom")
        );             
		 
		
		// --- BARRA DE VISTA ---
		List<ToolbarButtonDefinition> botonesVista = List.of( 
		    new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 		"4001-Panel-Galeria_48x48.png", "Panel Galería", "vista")
		   ,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 		"4002-Grid_48x48.png", "Vista Grid", "vista")
		   ,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 		"4006-Polaroid48x48.png", "Vista Polaroid", "vista")
		   ,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 		"4003-Pantalla_Completa_48x48.png", "Pantalla Completa", "vista")
//		   ,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 		"4005-Carrousel_48x48.png", "Vista Carrusel", "vista")
		   ,new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_MOSTRAR_DIALOGO_LISTA, 	"4004-Lista_48x48.png", "Vista Lista", "vista")
		);
            
		
		// --- BARRA DE UITILS ---
		List<ToolbarButtonDefinition> botonesUtils = List.of( 
			new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_ELIMINAR, 				"5002-borrar_48x48.png", "Eliminar Imagen", "control")
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ESPECIAL_REFRESCAR, 			"5001-Refrescar_48x48.png", "Refrescar", "control")
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_LOCALIZAR, 			"5003-Ubicacion_de_Archivo_48x48.png", "Abrir Ubicación", "control")
		);
             
		
		// --- BARRA DE APOYO ---
		List<ToolbarButtonDefinition> botonesApoyo = List.of( 
            new ToolbarButtonDefinition(AppActionCommands.CMD_ARCHIVO_ABRIR, 				"6001-selector_de_carpetas_48x48.png", "Abrir Carpeta", "especiales")
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ESPECIAL_MENU, 				"6002-menu_48x48.png", "Menú Principal", "especiales")
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ESPECIAL_BOTONES_OCULTOS, 	"6003-Botones_Ocultos_48x48.png", "Mostrar Botones Ocultos", "especiales")
        );
		
		
		// --- BARRA DE BOTONES TOGGLE ---
		List<ToolbarButtonDefinition> botonesToggle = List.of(
             new ToolbarButtonDefinition(AppActionCommands.CMD_TOGGLE_SUBCARPETAS, 			"7001-Subcarpetas_48x48.png", "Incluir/Excluir Subcarpetas", "toggle")//, ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES,"7002-Mantener_Proporciones_48x48.png", "Mantener Proporciones", "toggle")//, ButtonType.TOGGLE)
        );
		
             
		// --- BARRA DE PROYECTOS ---
		List<ToolbarButtonDefinition> botonesProyectoEnVista = List.of(
             new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA, 		"7003-marcar_imagen_48x48.png", "Marcar Imagen para Proyecto", "proyectoVista")
        );
		
		List<ToolbarButtonDefinition> botonesProyectoEnProyecto = List.of(
			 new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA, 		"7101-marcar_imagen_48x48.png", "Cambia la iamgen de Seleccion a Descartes", "proyecto")
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 	"7102-nuevo_proyecto_48x48.png", "Nuevo Proyecto", "proyecto")//, ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 	"7103-abrir_proyecto_48x48.png", "Abrir Proyecto", "proyecto")//, ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 	"7104-guardar_proyecto_48x48.png", "Guardar Proyecto", "proyecto")//, ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 	"7105-guardar_proyecto_como_48x48.png", "Guardar Proyecto Como", "proyecto")//, ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 	"7106-eliminar_proyecto_48x48.png", "Eliminar Proyecto", "proyecto")//, ButtonType.TOGGLE)
		);    
            
		
		// --- BARRA DE BOTONES modos ---
		List<ToolbarButtonDefinition> botonesModo = List.of(
			 new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR,"8001-modod_visualizador_48x48.png", "Modo Visualizador", "modo")//, ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_GESTIONAR, 			"8002-Mostrar_Favoritos_48x48.png", "Mostrar/Ocultar Favoritos", "modo")//, ButtonType.TOGGLE)2
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 	"8003-datos_48x48.png", "Modo Datos", "modo")//, ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 	"4005-Carrousel_48x48.png", "Vista Carrusel", "modo")
            
	    );
		// botones para los modos de edicion, carrousel, proyectos, gestion de Datos...
		
		
		// --- BARRA DE BOTONES carrousel ---
		List<ToolbarButtonDefinition> botonesCarrousel = List.of(
			 new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_FOTOGRAMA_PRIMERO,		"9001-primer_fotograma_48x48.png", "Primer Fotograma", "carrousel")
			,new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_FOTOGRAMA_ANTERIOR,		"9003-fotograma_anterior_48x48.png", "Fotograma Anterior", "carrousel")
			,new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_FOTOGRAMA_SIGUIENTE,	"9008-fotograma_siguiente_48x48.png", "Fotograma Siguiente", "carrousel")
			,new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_FOTOGRAMA_ULTIMO,		"9010-ultimo_fotograma_48x48.png", "Ultimo Fotograma", "carrousel")
			,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE,		"9002-retroceso_rapido_48x48.png", "Retroceso Rapido", "carrousel")
			,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE,		"9004-play_48x48.png", "Play", "carrousel")
			,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE,		"9005-pausa_48x48.png", "Pausa", "carrousel")
//			,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE,		"9006-play-pausa_48x48.png", "Play-Pausa", "carrousel")
			,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE,		"9007-stop_48x48.png", "Stop", "carrousel")
			,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE,		"9009-avance_rapido_48x48.png", "Avance Rapido", "carrousel")
			,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE,		"9011-maxima_velocidad_48x48.png", "Maxima Velocidad", "carrousel")
			,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE,		"9012-velocidad_normal_48x48.png", "Velocidad Normal", "carrousel")
			,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE,		"9013-minima_velocidad_48x48.png", "Minima Velocidad", "carrousel")
		);

		
		// --- BARRA DE BOTONES orden ---
		// boton de on/off, acendente/descendente, nombre, tamaño, fecha, tags...
		
		// --- BARRA DE BOTONES filtros ---
		// filtrado por extension (bmp, gif, png...), filtros por tags, filtros por letra inicial...

		
		
            
		// --- BARRA DE CONTROL DE IMAGEN INFERIOR (CONSOLIDADA: D-Pad, Colores, Cuadros, Paleta) ---
		// -- ICONOS INTERNOS DEL DPAD
		// 0: UP, 1: DOWN, 2: LEFT, 3: RIGHT
		List<HotspotDefinition> dpadPaneoHotspots = List.of(
             new HotspotDefinition(AppActionCommands.CMD_PAN_TOP_EDGE, 						"D-Pad_up_48x48.png", "Panear Arriba", IconScope.COMMON)
            ,new HotspotDefinition(AppActionCommands.CMD_PAN_BOTTOM_EDGE, 					"D-Pad_down_48x48.png", "Panear Abajo", IconScope.COMMON)
            ,new HotspotDefinition(AppActionCommands.CMD_PAN_LEFT_EDGE, 					"D-Pad_Left_48x48.png", "Panear Izquierda", IconScope.COMMON)
            ,new HotspotDefinition(AppActionCommands.CMD_PAN_RIGHT_EDGE, 					"D-Pad_right_48x48.png", "Panear Derecha", IconScope.COMMON)
        );

        List<ToolbarButtonDefinition> botonesControlesImagenInferior = List.of(
            // 1. D-Pad (Definición actualizada)
            new ToolbarButtonDefinition(
                "dpad.paneo", // Comando canónico/clave base para el componente D-Pad
                "D-Pad_none_48x48.png", // Icono base (cuando no hay hover)
                IconScope.COMMON, // El icono base es común
                "Control de Paneo", // Tooltip general
                "controles_imagen_inferior", // Categoría
                dpadPaneoHotspots // La lista de hotspots definida arriba
            ),
        		
            new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TOGGLE_TO_CURSOR, 	"20001-zoom_al_cursor_48x48.png", "Activar/Desactivar Zoom al Cursor", "controles_imagen_inferior"),//, ButtonType.TOGGLE),
            
            // 2. Botones de color de fondo
            new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_1, 	"color_Button_48x48.png", IconScope.COMMON, "Fondo Tema Claro", 	"controles_imagen_inferior", 	ButtonType.TRANSPARENT),
            new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_2, 	"color_Button_48x48.png", IconScope.COMMON, "Fondo Tema Oscuro", "controles_imagen_inferior", 	ButtonType.TRANSPARENT),
            new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_3, 	"color_Button_48x48.png", IconScope.COMMON, "Fondo Tema Azul", 	"controles_imagen_inferior", 	ButtonType.TRANSPARENT),
            new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_4, 	"color_Button_48x48.png", IconScope.COMMON, "Fondo Tema Naranja","controles_imagen_inferior", 	ButtonType.TRANSPARENT),
            new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_5, 	"color_Button_48x48.png", IconScope.COMMON, "Fondo Tema Verde", 	"controles_imagen_inferior", 	ButtonType.TRANSPARENT),
            
            // 3. Botón de fondo a cuadros
            new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_CHECKERED, 	"color_Button_48x48.png", IconScope.COMMON, "Fondo a Cuadros", 		"controles_imagen_inferior", 	ButtonType.TRANSPARENT),
            
            // 4. Botón de selección de color personalizado
            new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_CUSTOM_COLOR, 	"Paint-Palette--Streamline-Core.png", IconScope.COMMON, "Seleccionar Color Personalizado...", "controles_imagen_inferior", ButtonType.TRANSPARENT)
        );


        return List.of(
            // Grupo Izquierda
            new ToolbarDefinition("navegacion", "Navegación", 	10, EnumSet.of(WorkMode.VISUALIZADOR, WorkMode.PROYECTO), botonesNavegacion, ToolbarAlignment.LEFT),

            // Grupo Centro
            new ToolbarDefinition("edicion", 	"Edición", 		20, EnumSet.of(WorkMode.VISUALIZADOR), botonesEdicion, ToolbarAlignment.CENTER),
            new ToolbarDefinition("zoom", 		"Zoom", 		30, EnumSet.of(WorkMode.VISUALIZADOR, WorkMode.PROYECTO), botonesZoom, ToolbarAlignment.CENTER),
//            new ToolbarDefinition("vistas",	    "Vistas", 		40, EnumSet.of(WorkMode.VISUALIZADOR, WorkMode.DATOS), botonesVista, ToolbarAlignment.CENTER),
            new ToolbarDefinition("carrousel",	"Carrousel", 	110, EnumSet.of(WorkMode.VISUALIZADOR, WorkMode.PROYECTO), botonesCarrousel, ToolbarAlignment.CENTER),
            
            // Grupo Derecha
            new ToolbarDefinition("utils",		"Utilidades", 	50, EnumSet.of(WorkMode.VISUALIZADOR, WorkMode.PROYECTO), botonesUtils, ToolbarAlignment.RIGHT),
            new ToolbarDefinition("toggle",	    "Toggles", 		60, EnumSet.of(WorkMode.VISUALIZADOR), botonesToggle, ToolbarAlignment.RIGHT),
            
            new ToolbarDefinition("proyectoVista","Proyecto", 	70, EnumSet.of(WorkMode.VISUALIZADOR, WorkMode.DATOS), botonesProyectoEnVista, ToolbarAlignment.RIGHT),
            new ToolbarDefinition("proyecto",	"Proyecto", 	90, EnumSet.of(WorkMode.PROYECTO), botonesProyectoEnProyecto, ToolbarAlignment.RIGHT),
            
            new ToolbarDefinition("modo",		"Modo",			80, EnumSet.of(WorkMode.VISUALIZADOR, WorkMode.PROYECTO, WorkMode.DATOS), botonesModo, ToolbarAlignment.RIGHT),
            
            new ToolbarDefinition("apoyo", 	    "Apoyo", 		100, EnumSet.of(WorkMode.VISUALIZADOR, WorkMode.PROYECTO, WorkMode.DATOS), botonesApoyo, ToolbarAlignment.RIGHT),
            
            // Barra especial que no se añade al contenedor principal. Su alineamiento no importa.
            new ToolbarDefinition("controles_imagen_inferior", "Controles de Imagen", 95, EnumSet.of(WorkMode.VISUALIZADOR), botonesControlesImagenInferior, ToolbarAlignment.RIGHT)
            
    	);
        
        
//            //new ToolbarDefinition("orden","Orden", botonesOrden)
//            //new ToolbarDefinition("filtros","Filtros", botonesFiltros)
//    	);
    }
    
    
    private String extraerNombreClave(String comandoCanonico) {
        if (comandoCanonico == null) return "desconocido";
        
        // Elimina prefijos comunes como "cmd." o "toggle."
        String resultado = comandoCanonico.startsWith("cmd.") ? comandoCanonico.substring(4) : comandoCanonico;
        resultado = resultado.startsWith("toggle.") ? resultado.substring(7) : resultado;

        // Reemplaza puntos por guiones bajos para un nombre de clave válido
        resultado = resultado.replace('.', '_');
        
        return resultado;
    }
    
    
    /**
     * Devuelve una lista con las claves de todas las barras de herramientas definidas.
     * @return Una lista de Strings (ej. ["navegacion", "edicion", "zoom", ...]).
     */
    public List<String> getToolbarKeys() {
        return generateModularToolbarStructure().stream()
                                                .map(ToolbarDefinition::claveBarra)
                                                .collect(Collectors.toList());
    }
    
    
} // ---FIN de la clase UIDefinitionService







/*
**********************************************************************************************************************
*********************************************************************************** MEGACOMENTARIO PARAA CARGA Y ORDEN
**********************************************************************************************************************

¡Excelente decisión! La disciplina ahora te ahorrará dolores de cabeza después.

Aquí tienes un prompt detallado que resume lo que hemos discutido sobre la refactorización de la funcionalidad de "Carga y Orden", moviendo la configuración por defecto al menú "Configuración" y añadiendo una barra de herramientas para la ordenación temporal. Puedes usar esto como tu hoja de ruta cuando retomes esta parte.

---

**Prompt para la Refactorización de la Funcionalidad de "Carga y Orden"**

**Objetivo General:**
Separar la configuración de la ordenación *por defecto* (persistente) de la capacidad de aplicar una ordenación *temporal* a la vista actual. Mejorar la usabilidad proporcionando acceso rápido a la ordenación temporal mediante una nueva sección en la barra de herramientas y simplificando los menús.

**Fases y Tareas Detalladas:**

**FASE 1: Definición de Comandos y Estados en el Modelo**

1.  **`controlador.commands.AppActionCommands.java`:**
    *   **Definir/Verificar Comandos para Ordenación por Defecto (Configuración):**
        *   `CMD_CONFIG_ORDEN_DEFAULT_CRITERIO_NOMBRE` (ej: "cmd.config.orden.default.crit.nombre")
        *   `CMD_CONFIG_ORDEN_DEFAULT_CRITERIO_TAMANO`
        *   `CMD_CONFIG_ORDEN_DEFAULT_CRITERIO_FECHA`
        *   `CMD_CONFIG_ORDEN_DEFAULT_CRITERIO_EXTENSION`
        *   `CMD_CONFIG_ORDEN_DEFAULT_DIRECCION_ASC`
        *   `CMD_CONFIG_ORDEN_DEFAULT_DIRECCION_DESC`
        *   `CMD_CONFIG_ORDEN_DEFAULT_DIRECCION_NINGUNO` (si se mantiene "Sin Ordenar" como opción default)
    *   **Definir/Verificar Comandos para Ordenación Temporal (Toolbar/Acción Rápida):**
        *   `CMD_ORDEN_TEMP_SET_CRITERIO_NOMBRE` (ej: "cmd.orden.temp.crit.nombre")
        *   `CMD_ORDEN_TEMP_SET_CRITERIO_TAMANO`
        *   `CMD_ORDEN_TEMP_SET_CRITERIO_FECHA`
        *   `CMD_ORDEN_TEMP_SET_CRITERIO_EXTENSION`
        *   `CMD_ORDEN_TEMP_TOGGLE_DIRECCION` (para un botón que cicle Asc/Desc) O:
        *   `CMD_ORDEN_TEMP_SET_DIRECCION_ASC`
        *   `CMD_ORDEN_TEMP_SET_DIRECCION_DESC`
        *   `CMD_ORDEN_TEMP_SET_DIRECCION_NINGUNO` (si se mantiene "Sin Ordenar" como opción temporal)

2.  **`modelo.VisorModel.java`:**
    *   **Añadir Enums para Criterio y Dirección (si no existen):**
        ```java
        public enum CriterioOrdenacion { NOMBRE, TAMANO, FECHA, EXTENSION, NINGUNO }
        public enum DireccionOrdenacion { ASCENDENTE, DESCENDENTE, NINGUNO } // 'NINGUNO' para dirección puede ser implícito si el criterio es NINGUNO
        ```
    *   **Campos para Configuración de Ordenación por Defecto:**
        *   `private CriterioOrdenacion criterioOrdenacionDefault = CriterioOrdenacion.NOMBRE;`
        *   `private DireccionOrdenacion direccionOrdenacionDefault = DireccionOrdenacion.ASCENDENTE;`
        *   Getters y Setters (los setters serán llamados por las Actions de Configuración).
        *   Estos campos se inicializarán desde `ConfigurationManager` al arrancar la aplicación.
    *   **Campos para Ordenación Temporal de la Vista Actual:**
        *   `private CriterioOrdenacion criterioOrdenacionTemporal;`
        *   `private DireccionOrdenacion direccionOrdenacionTemporal;`
        *   Getters y Setters (los setters serán llamados por las Actions de la Toolbar).
        *   Al cargar una nueva carpeta, estos campos temporales se resetean a los valores `...Default` o a un estado "sin ordenación temporal activa".

**FASE 2: Definición de la Interfaz de Usuario (`UIDefinitionService.java`)**

1.  **Menú "Imagen":**
    *   **Eliminar** el submenú "Carga y Orden" y todos sus ítems.

2.  **Menú "Configuración" -> Submenú "Carga y Ordenación" (o similar):**
    *   Este submenú ahora contendrá las opciones para establecer los **defaults persistentes**.
    *   **Sub-Submenú "Criterio de Ordenación Predeterminado":**
        *   Grupo de `JRadioButtonMenuItem`s vinculados a `CMD_CONFIG_ORDEN_DEFAULT_CRITERIO_...`.
        *   Textos: "Nombre", "Tamaño", "Fecha", "Extensión".
    *   **Sub-Submenú "Dirección de Ordenación Predeterminada":**
        *   Grupo de `JRadioButtonMenuItem`s vinculados a `CMD_CONFIG_ORDEN_DEFAULT_DIRECCION_...`.
        *   Textos: "Ascendente", "Descendente", "Sin Ordenar" (opcional).

3.  **Barra de Herramientas (Nueva Sección "Ordenación"):**
    *   **Botón/Control para Criterio de Ordenación Temporal:**
        *   **Opción A (Botón con `JPopupMenu`):**
            *   `ToolbarButtonDefinition` para un `JButton` principal (ej. `CMD_ORDEN_TEMP_MOSTRAR_CRITERIOS`, texto "Ordenar Por:", icono genérico de orden).
            *   El `JPopupMenu` se construiría dinámicamente en `ToolbarBuilder` o se asociaría en `VisorController`, conteniendo ítems para "Nombre", "Tamaño", etc., cada uno vinculado a su `CMD_ORDEN_TEMP_SET_CRITERIO_...`.
        *   **Opción B (`JComboBox`):**
            *   No se define directamente como `ToolbarButtonDefinition`. `ToolbarBuilder` crearía un `JComboBox` y lo poblaría. Se necesitaría un `ActionListener` o `ItemListener` para él.
    *   **Botón/Control para Dirección de Ordenación Temporal:**
        *   **Opción A (Botón Cíclico `JButton`):**
            *   `ToolbarButtonDefinition` para un `JButton` (ej. `CMD_ORDEN_TEMP_TOGGLE_DIRECCION`). Icono y texto cambiarán según el estado (A-Z, Z-A, ---).
        *   **Opción B (Grupo de `JToggleButton`):**
            *   Dos o tres `ToolbarButtonDefinition` para `JToggleButton`s (ej. `CMD_ORDEN_TEMP_SET_DIRECCION_ASC`, `CMD_ORDEN_TEMP_SET_DIRECCION_DESC`), agrupados visualmente y en un `ButtonGroup`.

**FASE 3: Implementación de Actions (`ActionFactory.java`)**

1.  **Actions para Configuración de Orden por Defecto:**
    *   Clase: `SetOrdenDefaultAction` (o nombres más específicos como `SetCriterioOrdenDefaultAction`, `SetDireccionOrdenDefaultAction`).
    *   Constructor: Recibe `ConfigurationManager`, el criterio/dirección que representa, y la clave de config a modificar.
    *   `actionPerformed()`: Llama a `configuration.setString("clave.default.criterio", "valor_criterio")` y `configuration.setString("clave.default.direccion", "valor_direccion")`. Actualiza el estado `.seleccionado` de los radios en su grupo.
    *   Estas `Action`s se asignarán a los `JRadioButtonMenuItem` del menú "Configuración".

2.  **Actions para Ordenación Temporal (Toolbar):**
    *   **Para Criterio:**
        *   Si es `JPopupMenu`: Cada `JMenuItem` del popup necesita una `Action` (ej. `AplicarCriterioOrdenTemporalAction`) que tome el criterio como parámetro.
        *   `actionPerformed()`: Llama a `model.setCriterioOrdenacionTemporal(nuevoCriterio)`, actualiza el texto/icono del botón principal de la toolbar, y dispara la reordenación (`controller.solicitarReordenacionVistaActual()`).
    *   **Para Dirección:**
        *   Si es botón cíclico: Una `Action` (`ToggleDireccionOrdenTemporalAction`).
            *   `actionPerformed()`: Ciclac `model.getDireccionOrdenacionTemporal()`, actualiza el icono/texto del botón, y dispara la reordenación.
        *   Si son `JToggleButton`s: `Action`s individuales (`SetDireccionAscTemporalAction`, `SetDireccionDescTemporalAction`).
            *   `actionPerformed()`: Establece `model.setDireccionOrdenacionTemporal(...)`, actualiza `Action.SELECTED_KEY`, y dispara reordenación.

**FASE 4: Lógica en Controladores y Servicios**

1.  **`VisorController.java`:**
    *   **`cargarListaImagenes(String claveImagenAMantener)`:**
        *   Al inicio, obtener `criterioActivo` y `direccionActiva`:
            *   Si `model.getCriterioOrdenacionTemporal()` no es "ninguno" o `null`, usar los valores temporales.
            *   Si no, usar `model.getCriterioOrdenacionDefault()` y `model.getDireccionOrdenacionDefault()`.
        *   Usar `criterioActivo` y `direccionActiva` para ordenar `clavesOrdenadas` (o el `mapaResultado`) ANTES de crear el `DefaultListModel` para `listaNombres`. Esto requerirá un `Comparator` personalizado.
    *   **`solicitarReordenacionVistaActual()` (Nuevo método):**
        *   Llamado por las `Action`s de ordenación temporal.
        *   Obtiene la `claveImagenAMantener` del `model.getSelectedImageKey()`.
        *   Llama a `cargarListaImagenes(claveImagenAMantener)` para forzar una recarga y reordenación con los nuevos criterios temporales.
        *   Actualiza la UI de la toolbar de ordenación para reflejar el estado actual.

2.  **`ConfigurationManager.java`:**
    *   **`DEFAULT_CONFIG`:** Añadir las nuevas claves para la ordenación por defecto:
        *   `comportamiento.orden.default.criterio = NOMBRE` (o el que prefieras)
        *   `comportamiento.orden.default.direccion = ASCENDENTE`
        *   Claves `.seleccionado` para los `JRadioButtonMenuItem` del menú "Configuración" que controlan estos defaults.
    *   **`createDefaultGroupCommentsMap()`:** Añadir comentarios para estas nuevas secciones/claves.
    *   **`guardarConfiguracionActual()` en `VisorController`:** Debe asegurar que los valores de `model.getCriterioOrdenacionDefault()` y `model.getDireccionOrdenacionDefault()` se escriban en `estadoFinalAGuardar` con las claves correctas (esto podría hacerse obteniéndolos de las `Action`s de configuración si estas actualizan `this.configuration`, o directamente del modelo si las `Action`s solo actualizan el modelo y el `config.cfg`).

3.  **`MenuBarBuilder.java` y `ToolbarBuilder.java`:**
    *   Adaptarlos para construir los nuevos ítems de menú y los componentes de la toolbar de ordenación, asignando las `Action`s correctas.
    *   `ToolbarBuilder` podría necesitar lógica especial para el botón de criterio con `JPopupMenu` o para los `JToggleButton` agrupados.

4.  **`AppInitializer.java`:**
    *   En `aplicarConfiguracionAlModelo()`: Leer `comportamiento.orden.default.criterio` y `comportamiento.orden.default.direccion` de `configuration` y establecerlos en `model.setCriterioOrdenacionDefault()` y `model.setDireccionOrdenacionDefault()`.
    *   En `aplicarConfigAlaVistaInternal()`: Asegurar que los `JRadioButtonMenuItem` del menú "Configuración" para la ordenación por defecto reflejen el estado cargado.

**FASE 5: Sincronización Visual**

*   Cuando se cambia un criterio o dirección de ordenación temporal (desde la toolbar), los botones de la toolbar deben actualizar su apariencia.
*   Cuando se cambia un criterio o dirección de ordenación por defecto (desde el menú Configuración), los `JRadioButtonMenuItem` de ese menú deben actualizar su estado seleccionado. (Las `Action`s deberían manejar esto con `Action.SELECTED_KEY` y `ButtonGroup`).

---

Este prompt es bastante exhaustivo, pero cubre los cambios necesarios en las diferentes capas de tu aplicación para implementar la funcionalidad de ordenación de forma clara y separada para la configuración persistente y la manipulación temporal.

Cuando estés listo, puedes ir abordando cada fase. ¡No dudes en preguntar si algo no está claro o si necesitas ayuda con una parte específica!

  
*/