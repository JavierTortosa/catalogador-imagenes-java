package vista.config;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.AppInitializer;
import controlador.commands.AppActionCommands;
import modelo.VisorModel.WorkMode;
import servicios.ConfigKeys;

public class UIDefinitionService {
	
	private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);
	

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
            		MenuItemType.ITEM, "Buscar...", null)
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
        // Submenú para MODOS DE VISUALIZACIÓN DE CONTENIDO (DisplayMode)
        List<MenuItemDefinition> tiposVistaDisplayModesSubItems = List.of(
	            new MenuItemDefinition(null, 
	            	MenuItemType.RADIO_GROUP_START, null, null),
	            new MenuItemDefinition(AppActionCommands.CMD_VISTA_SINGLE, 
	            	MenuItemType.RADIO_BUTTON_ITEM, "Vista Imagen Única", null),
	            new MenuItemDefinition(AppActionCommands.CMD_VISTA_GRID,   
	            	MenuItemType.RADIO_BUTTON_ITEM, "Vista Cuadrícula (Grid)", null),
	            new MenuItemDefinition(AppActionCommands.CMD_VISTA_POLAROID, 
	            	MenuItemType.RADIO_BUTTON_ITEM, "Vista Polaroid", null),
	            new MenuItemDefinition(null, 
	            	MenuItemType.RADIO_GROUP_END, null, null)
        );
        
        List<MenuItemDefinition> vistaSubItems = List.of(
            new MenuItemDefinition(null, 
            	MenuItemType.SUB_MENU, "Modos de Visualización de Contenido", tiposVistaDisplayModesSubItems),
            new MenuItemDefinition(null, 
            	MenuItemType.SEPARATOR, null, null),
            // Pantalla Completa: Ahora es una configuración de vista (CHECKBOX_ITEM) al mismo nivel.
            new MenuItemDefinition(AppActionCommands.CMD_VISTA_PANTALLA_COMPLETA, 
            	MenuItemType.CHECKBOX_ITEM, "Modo Pantalla Completa", null),
            new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP, 
            	MenuItemType.CHECKBOX_ITEM, "Mantener Ventana Siempre Encima", null),
            new MenuItemDefinition(null, 
        		MenuItemType.SEPARATOR, null, null), 
            new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_MENU_BAR, 
        		MenuItemType.CHECKBOX_ITEM, "Barra de Menú", null),
            new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_TOOL_BAR, 
        		MenuItemType.CHECKBOX_ITEM, "Barra de Botones", null),
            new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_FILE_LIST, 
        		MenuItemType.CHECKBOX_ITEM, "Lista de Archivos", null),
            new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_THUMBNAILS, 
        		MenuItemType.CHECKBOX_ITEM, "Barra de Miniaturas", null),
            new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_LOCATION_BAR, 
        		MenuItemType.CHECKBOX_ITEM, "Barra de Estado", null),
            new MenuItemDefinition(null, 
        		MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_CHECKERED_BG, 
        		MenuItemType.CHECKBOX_ITEM, "Fondo a Cuadros", null),
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
	        
	        // Iteramos sobre la lista de COMPONENTES de la barra
	        for (ToolbarComponentDefinition compDef : barra.componentes()) { // <-- Usamos el nuevo nombre "componentes()"
	        
	            // Si el componente es una instancia de ToolbarButtonDefinition, lo procesamos.
	            // Si es un Label o un Separator, simplemente lo ignoramos.
	            if (compDef instanceof ToolbarButtonDefinition boton) { // <-- Usamos un "pattern matching for instanceof"
	                
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
	        }
	        
	        // Si la barra no tenía ningún botón (quizás solo labels?), no creamos un submenú vacío
	        if (checkboxesDeBotones.isEmpty()) {
	            continue; // Pasamos a la siguiente barra
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
             new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_PRIMERA, 				"1001-primera_48x48.png", "Primera Imagen", "navegacion")
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_ANTERIOR, 				"1002-anterior_48x48.png", "Imagen Anterior", "navegacion")
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_SIGUIENTE, 				"1003-siguiente_48x48.png", "Imagen Siguiente", "navegacion")
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_ULTIMA, 					"1004-ultima_48x48.png", "Última Imagen", "navegacion")
        );
		
		
		// --- BARRA DE EDICIÓN ---
		List<ToolbarButtonDefinition> botonesEdicion = List.of(
             new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_ROTAR_IZQ,			"2001-rotar_izquierda_48x48.png", "Rotar Izquierda", "edicion")
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_ROTAR_DER, 			"2002-rotar_derecha_48x48.png", "Rotar Derecha", "edicion")
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_VOLTEAR_H, 			"2003-espejo_horizontal_48x48.png", "Voltear Horizontal", "edicion")
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_VOLTEAR_V, 			"2004-espejo_vertical_48x48.png", "Voltear Vertical", "edicion")
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_RECORTAR, 			"2005-recortar_48x48.png", "Recortar", "edicion")
        );

		
		// --- BARRA DE ZOOM ---
		List<ToolbarButtonDefinition> botonesZoom = List.of(
				//INICIO GRUPO DE BOTONES
            new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR, 			"3005-escalar_para_ajustar_48x48.png", "Escalar para Ajustar", "zoom", ButtonType.TOGGLE)
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_AUTO, 				"3002-zoom_auto_48x48.png", "Zoom Automático", "zoom", ButtonType.TOGGLE)
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_ANCHO, 				"3003-ajustar_al_ancho_48x48.png", "Ajustar al Ancho", "zoom", ButtonType.TOGGLE)
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_ALTO, 				"3004-ajustar_al_alto_48x48.png", "Ajustar al Alto", "zoom", ButtonType.TOGGLE)
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_RELLENAR, 			"3009-rellenar_48x48.png", "Rellenar Zoom", "zoom", ButtonType.TOGGLE)
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_FIJO, 				"3006-zoom_fijo_48x48.png", "Zoom Actual Fijo", "zoom", ButtonType.TOGGLE)
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO, 		"3007-zoom_especifico_48x48.png", "Zoom Especificado", "zoom", ButtonType.TOGGLE)
           		//FIN GRUPO DE BOTONES
           
        );             
		 
		
		// --- BARRA DE VISTA ---
		List<ToolbarButtonDefinition> botonesVista = List.of( 
//		    new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 		"4001-Panel-Galeria_48x48.png", "Panel Galería", "vista")
				//INICIO GRUPO DE BOTONES
			new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_SINGLE, 				"4007-imagen_unica_48x48.png", "Vista Imagen Unica", "vista", ButtonType.TOGGLE)
		   ,new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_POLAROID, 				"4006-polaroid48x48.png", "Vista Polaroid", "vista", ButtonType.TOGGLE)
		   ,new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_GRID, 					"4002-grid_48x48.png", "Vista Grid", "vista", ButtonType.TOGGLE)
		   		//FIN GRUPO DE BOTONES
		   ,new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_MOSTRAR_DIALOGO_LISTA, 	"4004-lista_48x48.png", "Vista Lista", "vista")
		);
            
		
		// --- BARRA DE UITILS ---
		List<ToolbarButtonDefinition> botonesUtils = List.of( 
			new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_ELIMINAR, 				"5002-borrar_48x48.png", "Eliminar Imagen", "control")
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ESPECIAL_REFRESCAR, 			"5001-refrescar_48x48.png", "Refrescar", "control")
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_LOCALIZAR, 			"5003-ubicacion_de_archivo_48x48.png", "Abrir Ubicación", "control")
		);
             
		
		// --- BARRA DE APOYO ---
		List<ToolbarButtonDefinition> botonesApoyo = List.of( 
            new ToolbarButtonDefinition(AppActionCommands.CMD_ARCHIVO_ABRIR, 				"6001-selector_de_carpetas_48x48.png", "Abrir Carpeta", "especiales")
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ESPECIAL_MENU, 				"6002-menu_48x48.png", "Menú Principal", "especiales")
           ,new ToolbarButtonDefinition(AppActionCommands.CMD_ESPECIAL_BOTONES_OCULTOS, 	"6003-botones_ocultos_48x48.png", "Mostrar Botones Ocultos", "especiales")
        );
		
		
		// --- BARRA DE BOTONES TOGGLE ---
		List<ToolbarButtonDefinition> botonesToggle = List.of(
             new ToolbarButtonDefinition(AppActionCommands.CMD_TOGGLE_SUBCARPETAS, 			"7001-subcarpetas_48x48.png", "Incluir/Excluir Subcarpetas", "toggle", ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES,"7002-mantener_proporciones_48x48.png", "Mantener Proporciones", "toggle", ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP,	"7004-siempre_encima_48x48.png", "Mantener Siempre Encima", "toggle", ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_PANTALLA_COMPLETA,     "4003-pantalla_completa_48x48.png", "Modo Pantalla Completa", "toggle", ButtonType.TOGGLE)
        );
		
             
		// --- BARRA DE PROYECTOS ---
		List<ToolbarButtonDefinition> botonesProyectoEnVista = List.of(
             new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA, 		"7003-marcar_imagen_48x48.png", "Marcar Imagen para Proyecto", "proyecto_vista", ButtonType.TOGGLE)
             ,new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE, 		"3001-zoom_48x48.png", "Activar/Desactivar Zoom Manual", "proyecto_vista", ButtonType.TOGGLE)
             ,new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_RESET, 				"3008-reset_48x48.png", "Resetear Zoom", "proyecto_vista")
        );
		
		List<ToolbarButtonDefinition> botonesProyectoEnProyecto = List.of(
			 new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA, 		"7101-marcar_imagen_48x48.png", "Cambia la iamgen de Seleccion a Descartes", "proyecto", ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 	"7102-nuevo_proyecto_48x48.png", "Nuevo Proyecto", "proyecto")//, ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 	"7103-abrir_proyecto_48x48.png", "Abrir Proyecto", "proyecto")//, ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 	"7104-guardar_proyecto_48x48.png", "Guardar Proyecto", "proyecto")//, ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 	"7105-guardar_proyecto_como_48x48.png", "Guardar Proyecto Como", "proyecto")//, ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, 	"7106-eliminar_proyecto_48x48.png", "Eliminar Proyecto", "proyecto")//, ButtonType.TOGGLE)
		);    
            
		
		// --- BARRA DE BOTONES modos ---
		List<ToolbarButtonDefinition> botonesModo = List.of(
				//INICIO GRUPO DE BOTONES
			 new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR,"8001-modo_visualizador_48x48.png", 	"Modo Visualizador", "modo", ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_GESTIONAR, 			"8002-mostrar_favoritos_48x48.png", 	"Modo Proyecto", "modo", ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_MODO_DATOS, 					"8003-datos_48x48.png", 				"Modo Datos", "modo", ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_MODO_EDICION,			 	"8004-edicion_48x48.png", 				"Modo Edicion", "modo", ButtonType.TOGGLE)
            ,new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_CAROUSEL, 				"4005-carrousel_48x48.png", 			"Vista Carrusel", "modo", ButtonType.TOGGLE)
            
            	//FIN GRUPO DE BOTONES
            
	    );
		// botones para los modos de edicion, carrousel, proyectos, gestion de Datos...
		
		
		// --- BARRA DE BOTONES carrousel ---
		List<ToolbarButtonDefinition> botonesCarrousel = List.of(
				
			 new ToolbarButtonDefinition(AppActionCommands.CMD_CAROUSEL_REWIND,				"9002-retroceso_rapido_48x48.png", "Retroceso Rapido", "carrousel")
			,new ToolbarButtonDefinition(AppActionCommands.CMD_CAROUSEL_PLAY,				"9004-play_48x48.png", "Play", "carrousel")
			,new ToolbarButtonDefinition(AppActionCommands.CMD_CAROUSEL_PAUSE,				"9005-pausa_48x48.png", "Pausa", "carrousel")
			,new ToolbarButtonDefinition(AppActionCommands.CMD_CAROUSEL_STOP,				"9007-stop_48x48.png", "Stop", "carrousel")
			,new ToolbarButtonDefinition(AppActionCommands.CMD_CAROUSEL_TOGGLE_SHUFFLE, 	"9014-shuffle_48x48.png", "Modo Aleatorio", "carrousel", ButtonType.TOGGLE)
			,new ToolbarButtonDefinition(AppActionCommands.CMD_CAROUSEL_FAST_FORWARD,		"9009-avance_rapido_48x48.png", "Avance Rapido", "carrousel")
    	);

		List<ToolbarComponentDefinition> botonesVelocidadCarrousel = List.of(
			 new ToolbarButtonDefinition(AppActionCommands.CMD_CAROUSEL_SPEED_INCREASE,		"9013-minima_velocidad_48x48.png", "Minima Velocidad", "velocidad_carrousel")
			,new LabelDefinition("label.velocidad.carrusel", "3.0s")
			,new ToolbarButtonDefinition(AppActionCommands.CMD_CAROUSEL_SPEED_DECREASE,		"9011-maxima_velocidad_48x48.png", "Maxima Velocidad", "velocidad_carrousel")
			,new SeparatorDefinition()
			,new ToolbarButtonDefinition(AppActionCommands.CMD_CAROUSEL_SPEED_RESET,		"9012-velocidad_normal_48x48.png", "Velocidad Normal", "velocidad_carrousel")
			
		);

		
		// --- BARRA DE BOTONES Sync ---
//		List<ToolbarComponentDefinition> botonesSync = List.of(
//			 new ToolbarButtonDefinition(AppActionCommands.CMD_SYNC_TOGGLE,					"10001-sync_on_48x48.png", "Sync Datos", "botones_syncro", ButtonType.TOGGLE)
//			,new ToolbarButtonDefinition(AppActionCommands.CMD_SYNC_SAFE,					"10002-shield_48x48.png", "Deshacer Sync", "botones_syncro")
//		);
		
		List<ToolbarButtonDefinition> botonesSincronizacion = List.of(
	            new ToolbarButtonDefinition(AppActionCommands.CMD_TOGGLE_SYNC_VISOR_CARRUSEL, "10001-sync_on_48x48.png", "Sincronizar Visor y Carrusel", "sincronizacion", ButtonType.TOGGLE)
	        );
		
		
		// --- BARRA DE BOTONES orden ---
		// boton de on/off, acendente/descendente, nombre, tamaño, fecha, tags...
		
		// --- BARRA DE BOTONES filtros ---
		// filtrado por extension (bmp, gif, png...), filtros por tags, filtros por letra inicial...

		
		
            
		// --- BARRA DE CONTROL DE IMAGEN INFERIOR (CONSOLIDADA: D-Pad, Colores, Cuadros, Paleta) ---
		// -- ICONOS INTERNOS DEL DPAD
		// 0: UP, 1: DOWN, 2: LEFT, 3: RIGHT
		List<HotspotDefinition> dpadPaneoHotspots = List.of(
             new HotspotDefinition(AppActionCommands.CMD_PAN_TOP_EDGE, 						"d-pad_up_48x48.png", "Panear Arriba", IconScope.COMMON)
            ,new HotspotDefinition(AppActionCommands.CMD_PAN_BOTTOM_EDGE, 					"d-pad_down_48x48.png", "Panear Abajo", IconScope.COMMON)
            ,new HotspotDefinition(AppActionCommands.CMD_PAN_LEFT_EDGE, 					"d-pad_left_48x48.png", "Panear Izquierda", IconScope.COMMON)
            ,new HotspotDefinition(AppActionCommands.CMD_PAN_RIGHT_EDGE, 					"d-pad_right_48x48.png", "Panear Derecha", IconScope.COMMON)
        );

        List<ToolbarButtonDefinition> botonesControlesImagenInferior = List.of(
            // 1. D-Pad (Definición actualizada)
            new ToolbarButtonDefinition(
                "dpad.paneo", // Comando canónico/clave base para el componente D-Pad
                "d-pad_none_48x48.png", // Icono base (cuando no hay hover)
                IconScope.COMMON, // El icono base es común
                "Control de Paneo", // Tooltip general
                "controles_imagen_inferior", // Categoría
                dpadPaneoHotspots // La lista de hotspots definida arriba
            ),
        		
            new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TOGGLE_TO_CURSOR, 		"20001-zoom_al_cursor_48x48.png", /*IconScope.COMMON,*/ "Activar/Desactivar Zoom al Cursor", "controles_imagen_inferior", ButtonType.TOGGLE),
            
            // 2. Botones de color de fondo
            
         // 2. Botones de color de fondo (¡con ActionCommands mejorados!)
            new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_THEME_COLOR,   	"stopw.png", IconScope.COMMON, "Fondo del Tema Actual",   "controles_imagen_inferior", /*ButtonType.NORMAL), //*/ButtonType.TRANSPARENT),
            new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_1,  	"stopw.png", IconScope.COMMON, "Ranura de color 1",       "controles_imagen_inferior", /*ButtonType.NORMAL), //*/ButtonType.TRANSPARENT),
            new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_2,  	"stopw.png", IconScope.COMMON, "Ranura de color 2",       "controles_imagen_inferior", /*ButtonType.NORMAL), //*/ButtonType.TRANSPARENT),
            new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_3,  	"stopw.png", IconScope.COMMON, "Ranura de color 3",       "controles_imagen_inferior", /*ButtonType.NORMAL), //*/ButtonType.TRANSPARENT),
            new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_4,  	"stopw.png", IconScope.COMMON, "Ranura de color 4",      	"controles_imagen_inferior", /*ButtonType.NORMAL), //*/ButtonType.TRANSPARENT),
            new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_CHECKERED,     	"stopw.png", IconScope.COMMON, "Fondo a Cuadros",			"controles_imagen_inferior", /*ButtpmType.NORMAL), //*/ButtonType.TRANSPARENT),
            new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_CUSTOM_COLOR,  	"paint-palette--streamline-core.png", IconScope.COMMON, "Seleccionar Color Personalizado...", 	"controles_imagen_inferior", ButtonType.TRANSPARENT)
        );

        
        List<ToolbarButtonDefinition> botonesExportacion = List.of(	
       		new ToolbarButtonDefinition(AppActionCommands.CMD_EXPORT_SELECCIONAR_CARPETA, 	"6001-selector_de_carpetas_48x48.png", "Seleccionar Carpeta de Destino", "acciones_exportacion"		),	//,ButtonType.STATUS_BAR_BUTTON*/),
            new ToolbarButtonDefinition(AppActionCommands.CMD_EXPORT_QUITAR_DE_COLA, 		"21001-quitar_de_cola.png", "Quitar de la cola", "acciones_exportacion" 							),	//,ButtonType.STATUS_BAR_BUTTON*/),
            new ToolbarButtonDefinition(AppActionCommands.CMD_EXPORT_ASIGNAR_ARCHIVO,		"21002-asignar_archivo.png","Asignar archivo manualmente", "acciones_exportacion" 					),	//,ButtonType.STATUS_BAR_BUTTON*/),
            new ToolbarButtonDefinition(AppActionCommands.CMD_EXPORT_IGNORAR_COMPRIMIDO, 	"21003-ignorar_comprimido.png", "Ignorar archivo comprimido", "acciones_exportacion" 				),	//,ButtonType.STATUS_BAR_BUTTON*/),
            new ToolbarButtonDefinition(AppActionCommands.CMD_EXPORT_RELOCALIZAR_IMAGEN, 	"21004-relocalizar_imagen.png", "Relocalizar Imagen", "acciones_exportacion" 						),	//,ButtonType.STATUS_BAR_BUTTON*/),
            new ToolbarButtonDefinition(AppActionCommands.CMD_INICIAR_EXPORTACION, 			"21005-iniciar_exportación.png","Iniciar Exportación", "acciones_exportacion" 						)	//,ButtonType.STATUS_BAR_BUTTON*/)
        );
        
        
        List<ToolbarComponentDefinition> componentesBarraEstado = List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE, "3001-zoom_48x48.png", "Activar/Desactivar Zoom Manual", "barra_estado", ButtonType.TOGGLE),
                new ToolbarButtonDefinition(AppActionCommands.CMD_TOGGLE_SUBCARPETAS, "7001-subcarpetas_48x48.png", "Incluir/Excluir Subcarpetas", "barra_estado", ButtonType.TOGGLE),
                new ToolbarButtonDefinition(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES,"7002-mantener_proporciones_48x48.png", "Mantener Proporciones", "barra_estado", ButtonType.TOGGLE),
                new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP, "7004-siempre_encima_48x48.png", "Mantener Siempre Encima", "barra_estado", ButtonType.TOGGLE),
                new SeparatorDefinition(),
                new LabelDefinition("label.control.zoomPorcentaje", "Z: 100%"),
//                new ToolbarButtonDefinition("cmd.control.modoZoom", "imagen.png", "Cambiar Modo de Zoom", "barra_estado") // Usamos un comando simple, no de AppActionCommands
                new ToolbarButtonDefinition("cmd.control.modoZoom", "3005-escalar_para_ajustar_48x48.png", "Cambiar Modo de Zoom", "barra_estado", /*ButtonType.NORMAL)//*/ButtonType.STATUS_BAR_BUTTON)
                
		);

        
        return List.of(
        	    // Grupo Izquierda
        	    new ToolbarDefinition("navegacion", "Navegación", 			 10, EnumSet.of(WorkMode.VISUALIZADOR, WorkMode.PROYECTO, WorkMode.CARROUSEL), List.copyOf(botonesNavegacion), ToolbarAlignment.LEFT),

        	    // Grupo Centro
        	    new ToolbarDefinition("edicion", "Edición", 				 20, EnumSet.of(WorkMode.VISUALIZADOR), List.copyOf(botonesEdicion), ToolbarAlignment.CENTER),
        	    new ToolbarDefinition("zoom", "Zoom", 						 30, EnumSet.of(WorkMode.VISUALIZADOR, WorkMode.PROYECTO), List.copyOf(botonesZoom), ToolbarAlignment.CENTER),
        	    new ToolbarDefinition("vista", "Vista", 					 40, EnumSet.of(WorkMode.VISUALIZADOR, WorkMode.PROYECTO, WorkMode.DATOS), List.copyOf(botonesVista), ToolbarAlignment.CENTER),
        	    
        	    // Grupo Derecha
        	    new ToolbarDefinition("control", "Utilidades", 				 50, EnumSet.of(WorkMode.VISUALIZADOR, WorkMode.PROYECTO), List.copyOf(botonesUtils), ToolbarAlignment.RIGHT),
        	    new ToolbarDefinition("proyecto", "Acciones de Proyecto", 	 90, EnumSet.of(WorkMode.PROYECTO), List.copyOf(botonesProyectoEnProyecto), ToolbarAlignment.RIGHT),
        	    new ToolbarDefinition("proyecto_vista", "Proyecto (Vista)",  60, EnumSet.of(WorkMode.VISUALIZADOR, WorkMode.DATOS, WorkMode.CARROUSEL), List.copyOf(botonesProyectoEnVista), ToolbarAlignment.RIGHT),
        	    
        	    new ToolbarDefinition("toggle", "Toggles", 					 70, EnumSet.of(WorkMode.VISUALIZADOR, WorkMode.CARROUSEL), List.copyOf(botonesToggle), ToolbarAlignment.RIGHT),
        	    
        	    new ToolbarDefinition("sincronizacion", "Sincronización",    75, EnumSet.of(WorkMode.VISUALIZADOR, WorkMode.CARROUSEL), List.copyOf(botonesSincronizacion), ToolbarAlignment.RIGHT),
//        	    new ToolbarDefinition("botones_syncro", "Sincronizacion", 	 75, EnumSet.of(WorkMode.VISUALIZADOR, WorkMode.CARROUSEL), List.copyOf(botonesSync), ToolbarAlignment.RIGHT),
        	    
        	    new ToolbarDefinition("modo", "Modo", 						 80, EnumSet.of(WorkMode.VISUALIZADOR, WorkMode.PROYECTO, WorkMode.DATOS, WorkMode.CARROUSEL, WorkMode.EDICION), List.copyOf(botonesModo), ToolbarAlignment.RIGHT),
        	    new ToolbarDefinition("especiales", "Apoyo", 				100, EnumSet.of(WorkMode.VISUALIZADOR, WorkMode.PROYECTO, WorkMode.DATOS, WorkMode.EDICION, WorkMode.CARROUSEL), List.copyOf(botonesApoyo), ToolbarAlignment.RIGHT),
        	    
        	    // Toolbars específicas del modo Carrusel
        	    new ToolbarDefinition("carrousel", "Carrousel", 			110, EnumSet.of(WorkMode.CARROUSEL), List.copyOf(botonesCarrousel), ToolbarAlignment.CENTER),
        	    new ToolbarDefinition("velocidad_carrousel", "Velocidad", 	120, EnumSet.of(WorkMode.CARROUSEL), botonesVelocidadCarrousel, ToolbarAlignment.CENTER),
        	    
        	    // Barras especiales
        	    new ToolbarDefinition("acciones_exportacion", "Acciones de Exportación", 	500, EnumSet.of(WorkMode.PROYECTO), List.copyOf(botonesExportacion), ToolbarAlignment.FREE),
        	    new ToolbarDefinition("controles_imagen_inferior", "Controles de Imagen", 	510, EnumSet.of(WorkMode.VISUALIZADOR), List.copyOf(botonesControlesImagenInferior), ToolbarAlignment.FREE),
        	    new ToolbarDefinition("barra_estado_controles", "Controles de Estado", 		600, EnumSet.allOf(WorkMode.class), componentesBarraEstado, ToolbarAlignment.FREE)
        	);
        
        
    }// --- FIN DEL METODO generateModularToolbarStructure --- 
    
    
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

