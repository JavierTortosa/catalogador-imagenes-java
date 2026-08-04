package vista.config;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import controlador.commands.AppActionCommands;
import modelo.VisorModel.WorkMode;
import servicios.ConfigKeys;

public class UIDefinitionService {

    // ========== CONSTANTES DE MODO DE TRABAJO ==========
    private static final WorkMode VISOR = WorkMode.VISUALIZADOR;
    private static final WorkMode PROJECT = WorkMode.PROYECTO;
    private static final WorkMode CLIENT = WorkMode.CLIENTE;
    private static final WorkMode DATA = WorkMode.DATOS;
    private static final WorkMode SLIDER = WorkMode.CARROUSEL;
    private static final WorkMode RENDER = WorkMode.RENDER;
    private static final WorkMode EDITOR = WorkMode.EDITOR;

    // ========== CONSTANTES DE ALINEACIÓN ==========
    private static final ToolbarAlignment tbarLeft = ToolbarAlignment.LEFT;
    private static final ToolbarAlignment tbarCenter = ToolbarAlignment.CENTER;
    private static final ToolbarAlignment tbarRight = ToolbarAlignment.RIGHT;
    private static final ToolbarAlignment tbarEast = ToolbarAlignment.EAST;
    private static final ToolbarAlignment tbarFree = ToolbarAlignment.FREE;

    public UIDefinitionService() {

    }

    public List<MenuItemDefinition> generateMenuStructure() {
        List<MenuItemDefinition> menuBarStructure = new ArrayList<>();

        // --- SECCIÓN 1: MENÚ "ARCHIVO" ---
        List<MenuItemDefinition> archivoSubItems = List.of(
                new MenuItemDefinition(AppActionCommands.CMD_ARCHIVO_ABRIR,
                        MenuItemType.ITEM, "Abrir Archivo...", null),
                new MenuItemDefinition(AppActionCommands.CMD_ARCHIVO_GUARDAR,
                        MenuItemType.ITEM, "Guardar", null),
                new MenuItemDefinition(AppActionCommands.CMD_ARCHIVO_GUARDAR_COMO,
                        MenuItemType.ITEM, "Guardar Como...", null),
                new MenuItemDefinition(null,
                        MenuItemType.SEPARATOR, null, null),
                new MenuItemDefinition(AppActionCommands.CMD_ARCHIVO_ABRIR_CON,
                        MenuItemType.ITEM, "Abrir con...", null),
                new MenuItemDefinition(AppActionCommands.CMD_ARCHIVO_IMPRIMIR,
                        MenuItemType.ITEM, "Imprimir...", null),
                new MenuItemDefinition(null,
                        MenuItemType.SEPARATOR, null, null),
                new MenuItemDefinition(AppActionCommands.CMD_ESPECIAL_REFRESCAR,
                        MenuItemType.ITEM, "Refrescar Lista", null),
                new MenuItemDefinition(AppActionCommands.CMD_ARCHIVO_RECARGAR_IMAGEN,
                        MenuItemType.ITEM, "Recargar Imagen Actual", null),
                new MenuItemDefinition(null,
                        MenuItemType.SEPARATOR, null, null),
                new MenuItemDefinition(AppActionCommands.CMD_ARCHIVO_SALIR,
                        MenuItemType.ITEM, "Salir", null));
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
                new MenuItemDefinition(null,
                        MenuItemType.SEPARATOR, null, null),
                new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_LOCALIZAR,
                        MenuItemType.ITEM, "Abrir Ubicación del Archivo", null));

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
                        MenuItemType.RADIO_GROUP_END, null, null));

        List<MenuItemDefinition> tiposPaneoSubItems = List.of(
                new MenuItemDefinition(AppActionCommands.CMD_PAN_TOP_EDGE,
                        MenuItemType.ITEM, "Ver Parte Superior", null),
                new MenuItemDefinition(AppActionCommands.CMD_PAN_BOTTOM_EDGE,
                        MenuItemType.ITEM, "Ver Parte Inferior", null),
                new MenuItemDefinition(AppActionCommands.CMD_PAN_LEFT_EDGE,
                        MenuItemType.ITEM, "Ver Parte Izquierda", null),
                new MenuItemDefinition(AppActionCommands.CMD_PAN_RIGHT_EDGE,
                        MenuItemType.ITEM, "Ver Parte Derecha", null));

        List<MenuItemDefinition> zoomSubItems = List.of(
                new MenuItemDefinition(AppActionCommands.CMD_ZOOM_PERSONALIZADO,
                        MenuItemType.ITEM, "Establecer Zoom %...", null),
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
                        MenuItemType.SUB_MENU, "Ajuste Visual", tiposZoomSubItems));
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
                new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_ELIMINAR,
                        MenuItemType.ITEM, "Eliminar Imagen", null),
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
                        MenuItemType.ITEM, "Propiedades de la imagen...", null));
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
                        MenuItemType.RADIO_GROUP_END, null, null));

        List<MenuItemDefinition> vistaSubItems = List.of(
                new MenuItemDefinition(null,
                        MenuItemType.SUB_MENU, "Modos de Visualización de Contenido", tiposVistaDisplayModesSubItems),
                new MenuItemDefinition(null,
                        MenuItemType.SEPARATOR, null, null),
                // Pantalla Completa: Ahora es una configuración de vista (CHECKBOX_ITEM) al
                // mismo nivel.
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

                new MenuItemDefinition(null,
                        MenuItemType.SEPARATOR, null, null),
                
                new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_INFOBAR_SUPERIOR,
                        MenuItemType.CHECKBOX_ITEM, "Barra de Estado de Imagen", null),
                new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_INFOBAR_INFERIOR,
                        MenuItemType.CHECKBOX_ITEM, "Barra de Estado de Aplicacion", null),

                new MenuItemDefinition(null,
                        MenuItemType.SEPARATOR, null, null),
                new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_MINIATURE_TEXT,
                        MenuItemType.CHECKBOX_ITEM, "Mostrar Nombres en Miniaturas", null),
                
                new MenuItemDefinition(null,
                        MenuItemType.SEPARATOR, null, null),
                new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_CHECKERED_BG,
                        MenuItemType.CHECKBOX_ITEM, "Fondo a Cuadros", null),
                
                
                new MenuItemDefinition(null,
                        MenuItemType.SEPARATOR, null, null),
                new MenuItemDefinition(AppActionCommands.CMD_VISTA_MOSTRAR_DIALOGO_LISTA,
                        MenuItemType.ITEM, "Mostrar Diálogo Lista de Imágenes...", null));

        /*
         * 
         * 
         * 
         */
        
        menuBarStructure.add(new MenuItemDefinition(null,
                MenuItemType.MAIN_MENU, "Vista", vistaSubItems));
        /*
         * 
         * 
         * 
         */

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
                new MenuItemDefinition(AppActionCommands.CMD_MODO_DATOS,
                        MenuItemType.RADIO_BUTTON_ITEM, "Modo Datos", null),
                new MenuItemDefinition(AppActionCommands.CMD_MODO_CLIENTE,
                        MenuItemType.RADIO_BUTTON_ITEM, "Modo Cliente", null),
                new MenuItemDefinition(AppActionCommands.CMD_VISTA_CAROUSEL,
                        MenuItemType.RADIO_BUTTON_ITEM, "Modo Carrusel", null),
                new MenuItemDefinition(null,
                        MenuItemType.RADIO_GROUP_END, null, null) // Finaliza el grupo de radios
        );
        
        /*
         * 
         * 
         */
        menuBarStructure.add(new MenuItemDefinition(null,
                MenuItemType.MAIN_MENU, "Modo", modoSubItems));

        /*
         * 
         * 
         */
        
        // --- SECCIÓN 6: MENÚ "PROYECTO" ---
        List<MenuItemDefinition> proyectoSubItems = List.of(
                new MenuItemDefinition(AppActionCommands.CMD_PROYECTO_GESTIONAR,
                        MenuItemType.ITEM, "Gestionar Proyecto Actual...", null),

                new MenuItemDefinition(AppActionCommands.CMD_PROYECTO_NUEVO,
                        MenuItemType.ITEM, "Nuevo Proyecto", null),
                new MenuItemDefinition(AppActionCommands.CMD_PROYECTO_ABRIR,
                        MenuItemType.ITEM, "Abrir Proyecto...", null),
                new MenuItemDefinition(AppActionCommands.CMD_PROYECTO_GUARDAR,
                        MenuItemType.ITEM, "Guardar Proyecto", null),
                new MenuItemDefinition(AppActionCommands.CMD_PROYECTO_GUARDAR_COMO,
                        MenuItemType.ITEM, "Guardar Proyecto Como...", null),
                new MenuItemDefinition(null,
                        MenuItemType.SEPARATOR, null, null),
                new MenuItemDefinition(AppActionCommands.CMD_EXPORT_ASSIGN_PANNEL,
                        MenuItemType.ITEM, "Panel de Control de Asignaciones", null),
                new MenuItemDefinition(null,
                        MenuItemType.SEPARATOR, null, null),
                new MenuItemDefinition(AppActionCommands.CMD_PROYECTO_ELIMINAR,
                        MenuItemType.ITEM, "Eliminar Proyecto...", null),
                new MenuItemDefinition(null,
                        MenuItemType.SEPARATOR, null, null),
                new MenuItemDefinition(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA,
                        MenuItemType.CHECKBOX_ITEM, "Marcar para Proyecto", null),
                new MenuItemDefinition(null, // AppActionCommands.CMD_RENDER_ASIGNAR_PREVIEW,
                        MenuItemType.ITEM, "Vista Rapida de Imagenes Seleccionadas", null)

        );
//        menuBarStructure.add(new MenuItemDefinition(null,
//        		MenuItemType.MAIN_MENU, "Vista", vistaSubItems));
//        menuBarStructure.add(new MenuItemDefinition(null,
//        		MenuItemType.MAIN_MENU, "Modo", modoSubItems));
        menuBarStructure.add(new MenuItemDefinition(null,
                MenuItemType.MAIN_MENU, "Proyecto", proyectoSubItems));
        
        // SECCION 6.5: MENU "MODO RENDER" 
        List<MenuItemDefinition> renderSubItems = List.of(
//                new MenuItemDefinition(AppActionCommands.CMD_PROYECTO_GESTIONAR,
//                        MenuItemType.ITEM, "Gestionar Proyecto Actual...", null)
                
                  new MenuItemDefinition(AppActionCommands.CMD_RENDER_ESCANEAR_CARPETA, 
                		MenuItemType.ITEM,"Escanear carpeta", null)
                , new MenuItemDefinition(AppActionCommands.CMD_RENDER_PROCESAR_SELECCIONADOS, 
                		MenuItemType.ITEM,"Procesar seleccionados", null)
                , new MenuItemDefinition(AppActionCommands.CMD_RENDER_PROCESAR_ARCHIVO, 
                		MenuItemType.ITEM,"Procesar archivo", null)
        );
        menuBarStructure.add(new MenuItemDefinition(null,
                MenuItemType.MAIN_MENU, "Render", renderSubItems));
        
        
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
                        MenuItemType.RADIO_GROUP_END, null, null));

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
                        MenuItemType.RADIO_GROUP_END, null, null));

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
                        MenuItemType.SUB_MENU, "Orden Visual", cargaOrdenSubItems));
        configSubItems.add(new MenuItemDefinition(null,
                MenuItemType.SUB_MENU, "Carga de Imágenes", configCargaImgSubItems));
        configSubItems.add(new MenuItemDefinition(null,
                MenuItemType.SEPARATOR, null, null));

        // 7.3. Submenú "General" (Configuraciones de comportamiento)
        List<MenuItemDefinition> configGeneralSubItems = List.of(
                new MenuItemDefinition(AppActionCommands.CMD_TOGGLE_WELCOME,
                        MenuItemType.CHECKBOX_ITEM, "Mostrar Imagen de Bienvenida", null),
                new MenuItemDefinition(AppActionCommands.CMD_TOGGLE_RESTORE_LAST,
                        MenuItemType.CHECKBOX_ITEM, "Abrir Ultima Imagen Vista al Iniciar", null),
                new MenuItemDefinition(null,
                        MenuItemType.SEPARATOR, null, null),
                new MenuItemDefinition(AppActionCommands.CMD_TOGGLE_WRAP_AROUND,
                        MenuItemType.CHECKBOX_ITEM, "Navegación Circular (Wrap Around)", null),
                new MenuItemDefinition(null,
                        MenuItemType.SEPARATOR, null, null),
                new MenuItemDefinition(AppActionCommands.CMD_TOGGLE_NAV_ARROWS,
                        MenuItemType.CHECKBOX_ITEM, "Mostrar Flechas de Navegación en Imagen", null));
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
            for (ToolbarComponentDefinition compDef : barra.componentes()) { // <-- Usamos el nuevo nombre
                                                                             // "componentes()"

                // Si el componente es una instancia de ToolbarButtonDefinition, lo procesamos.
                // Si es un Label o un Separator, simplemente lo ignoramos.
                if (compDef instanceof ToolbarButtonDefinition boton) { // <-- Usamos un "pattern matching for
                                                                        // instanceof"

                    // Generamos la clave BASE del botón.
                    String claveBaseBoton = ConfigKeys.buildKey(
                            "interfaz.boton", // O el prefijo correcto
                            barra.claveBarra(),
                            extraerNombreClave(boton.comandoCanonico()));

                    checkboxesDeBotones.add(
                            new MenuItemDefinition(claveBaseBoton, // <-- El ActionCommand ahora es la clave BASE
                                    MenuItemType.CHECKBOX_ITEM, "  " + boton.textoTooltip(), null));
                }
            }

            // Si la barra no tenía ningún botón (quizás solo labels?), no creamos un
            // submenú vacío
            if (checkboxesDeBotones.isEmpty()) {
                continue; // Pasamos a la siguiente barra
            }

            // Creamos la clave de configuración para la visibilidad de la barra completa
            String claveConfigBarra = ConfigKeys.buildKey("interfaz.herramientas", barra.claveBarra());

            // Creamos el submenú para esta barra
            configHerramientasSubItems.add(
                    new MenuItemDefinition(claveConfigBarra,
                            MenuItemType.CHECKBOX_ITEM_WITH_SUBMENU, "Barra de " + barra.titulo(),
                            checkboxesDeBotones));
        }

        // Añadimos el submenú "Herramientas" completo al menú de Configuración
        
        // ***** OPCION DE HERRAMIENTAS DEL MENU CONFIGURACION ***** --- PENDIENTE DE ELIMINACION ---
//        configSubItems.add(new MenuItemDefinition(null,
//                MenuItemType.SUB_MENU, "Herramientas", configHerramientasSubItems));
//        configSubItems.add(new MenuItemDefinition(null,
//                MenuItemType.SEPARATOR, null, null));

        // --- 7.6. SUBMENÚ: "CONFIGURAR VISIBILIDAD DE BARRAS DE INFORMACIÓN" ---
        List<MenuItemDefinition> configVisibilidadBarrasSubItems = new ArrayList<>();

        // --- 7.6.1. Sub-Submenú para Barra de Información Superior ---
        List<MenuItemDefinition> barraSuperiorVisSubItems = new ArrayList<>();
        barraSuperiorVisSubItems
                .add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_VISIBLE, // Checkbox
                                                                                                          // para
                                                                                                          // visibilidad
                                                                                                          // de toda la
                                                                                                          // barra
                                                                                                          // superior
                        MenuItemType.CHECKBOX_ITEM, "Mostrar Panel de Info.", null));
        barraSuperiorVisSubItems.add(new MenuItemDefinition(null,
                MenuItemType.SEPARATOR, null, null)); // Separador

        // Checkbox para visibilidad de Nombre/Ruta (Superior)
        barraSuperiorVisSubItems.add(new MenuItemDefinition(
                AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_NOMBRE_RUTA,
                MenuItemType.CHECKBOX_ITEM, "Nombre/Ruta Archivo", null));
        // Submenú para el formato de Nombre/Ruta (Superior)
        List<MenuItemDefinition> formatoNombreRutaSuperiorSubItems = List.of(
                new MenuItemDefinition(null,
                        MenuItemType.RADIO_GROUP_START, null, null),
                new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_SOLO_NOMBRE,
                        MenuItemType.RADIO_BUTTON_ITEM, "Solo Nombre de Archivo", null),
                new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_RUTA_COMPLETA,
                        MenuItemType.RADIO_BUTTON_ITEM, "Ruta Completa y Nombre", null),
                new MenuItemDefinition(null,
                        MenuItemType.RADIO_GROUP_END, null, null));
        barraSuperiorVisSubItems.add(new MenuItemDefinition(null,
                MenuItemType.SUB_MENU,
                "   Formato", formatoNombreRutaSuperiorSubItems));
        barraSuperiorVisSubItems.add(new MenuItemDefinition(null,
                MenuItemType.SEPARATOR, null, null)); // Separador

        // Checkboxes para los demás elementos de la barra superior
        barraSuperiorVisSubItems
                .add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_INDICE_TOTAL,
                        MenuItemType.CHECKBOX_ITEM, "Índice/Total Imágenes", null));
        barraSuperiorVisSubItems
                .add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_DIMENSIONES,
                        MenuItemType.CHECKBOX_ITEM, "Dimensiones Originales", null));
        barraSuperiorVisSubItems
                .add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_TAMANO_ARCHIVO,
                        MenuItemType.CHECKBOX_ITEM, "Tamaño de Archivo", null));
        barraSuperiorVisSubItems
                .add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_FECHA_ARCHIVO,
                        MenuItemType.CHECKBOX_ITEM, "Fecha de Archivo", null));
        barraSuperiorVisSubItems
                .add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_FORMATO_IMAGEN,
                        MenuItemType.CHECKBOX_ITEM, "Formato de Imagen", null));
        barraSuperiorVisSubItems
                .add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_MODO_ZOOM,
                        MenuItemType.CHECKBOX_ITEM, "Modo de Zoom", null));
        barraSuperiorVisSubItems
                .add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_SUPERIOR_ZOOM_REAL_PCT,
                        MenuItemType.CHECKBOX_ITEM, "% Zoom Real", null));

        // Añadir el submenú de la barra superior al menú principal de "Configurar
        // Visibilidad Barras"
        configVisibilidadBarrasSubItems.add(new MenuItemDefinition(null,
                MenuItemType.SUB_MENU,
                "Información de Imagen", barraSuperiorVisSubItems));
        configVisibilidadBarrasSubItems.add(new MenuItemDefinition(null,
                MenuItemType.SEPARATOR, null, null));

        // --- 7.6.2. Sub-Submenú para Barra de Estado/Control Inferior ---
        List<MenuItemDefinition> barraInferiorVisSubItems = new ArrayList<>();
        barraInferiorVisSubItems
                .add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_VISIBLE, // Checkbox
                                                                                                          // para
                                                                                                          // visibilidad
                                                                                                          // de toda la
                                                                                                          // barra
                                                                                                          // inferior
                        MenuItemType.CHECKBOX_ITEM, "Mostrar Panel de Control", null));
        barraInferiorVisSubItems.add(new MenuItemDefinition(null,
                MenuItemType.SEPARATOR, null, null)); // Separador

        // Checkbox para visibilidad de Nombre/Ruta (Inferior)
        barraInferiorVisSubItems
                .add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_NOMBRE_RUTA,
                        MenuItemType.CHECKBOX_ITEM, "Nombre/Ruta Archivo", null));
        // Submenú para el formato de Nombre/Ruta (Inferior)
        List<MenuItemDefinition> formatoNombreRutaInferiorSubItems = List.of(
                new MenuItemDefinition(null,
                        MenuItemType.RADIO_GROUP_START, null, null),
                new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_SOLO_NOMBRE,
                        MenuItemType.RADIO_BUTTON_ITEM, "Solo Nombre de Archivo", null),
                new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_RUTA_COMPLETA,
                        MenuItemType.RADIO_BUTTON_ITEM, "Ruta Completa y Nombre", null),
                new MenuItemDefinition(null,
                        MenuItemType.RADIO_GROUP_END, null, null));
        barraInferiorVisSubItems.add(new MenuItemDefinition(null,
                MenuItemType.SUB_MENU, "   Formato", formatoNombreRutaInferiorSubItems));
        barraInferiorVisSubItems.add(new MenuItemDefinition(null,
                MenuItemType.SEPARATOR, null, null)); // Separador

        // Checkboxes para los demás elementos de la barra inferior
        barraInferiorVisSubItems
                .add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_ZM,
                        MenuItemType.CHECKBOX_ITEM, "Icono Zoom Manual", null));
        barraInferiorVisSubItems
                .add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_PROP,
                        MenuItemType.CHECKBOX_ITEM, "Icono Proporciones", null));
        barraInferiorVisSubItems
                .add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_ICONO_SUBC,
                        MenuItemType.CHECKBOX_ITEM, "Icono Subcarpetas", null));
        barraInferiorVisSubItems.add(new MenuItemDefinition(null,
                MenuItemType.SEPARATOR, null, null)); // Separador
        barraInferiorVisSubItems
                .add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_CTRL_ZOOM_PCT,
                        MenuItemType.CHECKBOX_ITEM, "Control % Zoom ", null));
        barraInferiorVisSubItems
                .add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_CTRL_MODO_ZOOM,
                        MenuItemType.CHECKBOX_ITEM, "Control Modo Zoom", null));
        barraInferiorVisSubItems.add(new MenuItemDefinition(null,
                MenuItemType.SEPARATOR, null, null)); // Separador
        barraInferiorVisSubItems
                .add(new MenuItemDefinition(AppActionCommands.CMD_INFOBAR_CONFIG_TOGGLE_INFERIOR_MENSAJES_APP,
                        MenuItemType.CHECKBOX_ITEM, "Area de Mensajes", null));

        // Añadir el submenú de la barra inferior al menú principal de "Configurar
        // Visibilidad Barras"
        configVisibilidadBarrasSubItems.add(new MenuItemDefinition(null, MenuItemType.SUB_MENU,
                "Estado y Control", barraInferiorVisSubItems));

        // 7.6.3. Añadir el submenú "Configurar Visibilidad Barras" al menú
        // "Configuración"
        configSubItems.add(new MenuItemDefinition(null, MenuItemType.SUB_MENU, "Paneles de Datos",
                configVisibilidadBarrasSubItems));
        configSubItems.add(new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null));

        // 7.5. Submenú "Tema" (movido después de visibilidad de barras por orden
        // lógico)

        List<MenuItemDefinition> configTemaSubItems = new ArrayList<>();

        // Simplemente añadimos un marcador de posición. MenuBarBuilder se encargará del
        // resto.
        configTemaSubItems.add(
                new MenuItemDefinition("placeholder.temas", MenuItemType.PLACEHOLDER, "PLACEHOLDER_TEMAS", null));

        
        configSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_CONFIG_AVANZADA, MenuItemType.ITEM,
                "Configuración Avanzada...", null));
        configSubItems.add(new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null));

        // 7.7. Ítems finales del menú "Configuración" (Guardar, Cargar, Versión)
        configSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_ESPECIAL_REFRESCAR, MenuItemType.ITEM,
                "Refrescar UI y Lista", null));
        configSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_CONFIG_GUARDAR, MenuItemType.ITEM,
                "Guardar Configuración Actual", null));
        configSubItems.add(new MenuItemDefinition(AppActionCommands.CMD_CONFIG_CARGAR_INICIAL, MenuItemType.ITEM,
                "Restaurar Configuración Inicial", null));

        // 7.8. Añadir el menú "Configuración" a la barra de menú principal
        menuBarStructure.add(new MenuItemDefinition(null, MenuItemType.MAIN_MENU, "Configuración", configSubItems));

        // --- SECCIÓN 9: MENÚ "AYUDA" ---
        List<MenuItemDefinition> ayudaSubItems = List.of(
                new MenuItemDefinition(AppActionCommands.CMD_AYUDA_MOSTRAR_GUIA,
                        MenuItemType.ITEM, "Guía de Usuario...", null),
                new MenuItemDefinition(null, // <-- AÑADIDO
                        MenuItemType.SEPARATOR, null, null),
                new MenuItemDefinition(AppActionCommands.CMD_AYUDA_VER_ATAJOS,
                        MenuItemType.ITEM, "Ver Atajos de Teclado...", null),
                new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
        		new MenuItemDefinition(AppActionCommands.CMD_CONFIG_MOSTRAR_VERSION, 
        				MenuItemType.ITEM, "Acerca de...", null)
        		);
        
        menuBarStructure.add(new MenuItemDefinition(null,
                MenuItemType.MAIN_MENU, "Ayuda", ayudaSubItems));

        // --- SECCIÓN 9: FIN DE LA DEFINICIÓN DE TODOS LOS MENÚS ---
        
        return menuBarStructure;
        
    }// --- FIN del metodo generateMenuStructure ---

    
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
                new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_PRIMERA, 
                		"1001-primera_48x48.png", "Primera Imagen", "navegacion"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_ANTERIOR, 
                		"1002-anterior_48x48.png", "Imagen Anterior", "navegacion"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_SIGUIENTE, 
                		"1003-siguiente_48x48.png", "Imagen Siguiente", "navegacion"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_ULTIMA, 
                		"1004-ultima_48x48.png", "Última Imagen", "navegacion")
        );

        // --- BARRA DE EDICIÓN ---
        List<ToolbarButtonDefinition> botonesEdicion = List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_ROTAR_IZQ, 
                		"2001-rotar_izquierda_48x48.png", "Rotar Izquierda", "edicion"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_ROTAR_DER, 
                		"2002-rotar_derecha_48x48.png", "Rotar Derecha", "edicion"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_VOLTEAR_H, 
                		"2003-espejo_horizontal_48x48.png", "Voltear Horizontal", "edicion"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_VOLTEAR_V, 
                		"2004-espejo_vertical_48x48.png", "Voltear Vertical", "edicion"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_RECORTAR, 
                		"2005-recortar_48x48.png", "Recortar", "edicion")
        );

        // --- BARRA DE ZOOM ---
        List<ToolbarButtonDefinition> botonesZoom = List.of(
                // INICIO GRUPO DE BOTONES
                new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR,
                        "3005-escalar_para_ajustar_48x48.png", "Escalar para Ajustar", "zoom", ButtonType.TOGGLE),
                new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_AUTO, 
                		"3002-zoom_auto_48x48.png", "Zoom Automático", "zoom", ButtonType.TOGGLE),
                new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_ANCHO, 
                		"3003-ajustar_al_ancho_48x48.png", "Ajustar al Ancho", "zoom", ButtonType.TOGGLE),
                new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_ALTO, 
                		"3004-ajustar_al_alto_48x48.png", "Ajustar al Alto", "zoom", ButtonType.TOGGLE),
                new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_RELLENAR, 
                		"3009-rellenar_48x48.png", "Rellenar Zoom", "zoom", ButtonType.TOGGLE),
                new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_FIJO, 
                		"3006-zoom_fijo_48x48.png", "Zoom Actual Fijo", "zoom", ButtonType.TOGGLE),
                new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO,
                        "3007-zoom_especifico_48x48.png", "Zoom Especificado", "zoom", ButtonType.TOGGLE)
        // FIN GRUPO DE BOTONES

        );

        // --- BARRA DE VISTA ---
        List<ToolbarButtonDefinition> botonesVista = List.of(
                // new ToolbarButtonDefinition(AppActionCommands.CMD_RENDER_ASIGNAR_PREVIEW,
                // "4001-Panel-Galeria_48x48.png", "Panel Galería", "vista")
                // INICIO GRUPO DE BOTONES
                new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_SINGLE, 
                		"4007-imagen_unica_48x48.png", "Vista Imagen Unica", "vista", ButtonType.TOGGLE)
                , new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_POLAROID, 
                		"4006-polaroid48x48.png", "Vista Polaroid", "vista", ButtonType.TOGGLE)
                , new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_GRID, 
                		"4002-grid_48x48.png", "Vista Grid", "vista", ButtonType.TOGGLE)
                // FIN GRUPO DE BOTONES
                , new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_MOSTRAR_DIALOGO_LISTA, 
                		"4004-lista_48x48.png", "Vista Lista", "vista")
        );

        // --- BARRA DE UITILS ---
        List<ToolbarButtonDefinition> botonesUtils = List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_ELIMINAR, 
                		"5002-borrar_48x48.png", "Eliminar Imagen", "control")
                , new ToolbarButtonDefinition(AppActionCommands.CMD_ESPECIAL_REFRESCAR, 
                		"5001-refrescar_48x48.png", "Refrescar", "control")
                , new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_LOCALIZAR,
                        "5003-ubicacion_de_archivo_48x48.png", "Abrir Ubicación", "control")
        );

        // --- BARRA DE ABRIR CARPETA ---
        List<ToolbarButtonDefinition> botonesAbrirCarpeta = List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_ARCHIVO_ABRIR, 
                		"6001-selector_de_carpetas_48x48.png", "Abrir Carpeta", "abrir_carpeta")
        );

        // --- BARRA DE APOYO ---
        List<ToolbarButtonDefinition> botonesApoyo = List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_ESPECIAL_MENU, 
                		"6002-menu_48x48.png", "Menú Principal", "especiales"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_ESPECIAL_BOTONES_OCULTOS,
                        "6003-botones_ocultos_48x48.png", "Mostrar Botones Ocultos", "especiales")
        );

        // --- BARRA DE BOTONES TOGGLE ---
        List<ToolbarButtonDefinition> botonesToggle = List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_TOGGLE_SUBCARPETAS, 
                		"7001-subcarpetas_48x48.png", "Incluir/Excluir Subcarpetas", "toggle", ButtonType.TOGGLE),
                new ToolbarButtonDefinition(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES,
                        "7002-mantener_proporciones_48x48.png", "Mantener Proporciones", "toggle", ButtonType.TOGGLE),
                new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP,
                        "7004-siempre_encima_48x48.png", "Mantener Siempre Encima", "toggle", ButtonType.TOGGLE),
                new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_PANTALLA_COMPLETA,
                        "4003-pantalla_completa_48x48.png", "Modo Pantalla Completa", "toggle", ButtonType.TOGGLE)
        );

        // --- BARRA ACCIONES DE PROYECTOS ---
        List<ToolbarButtonDefinition> botonesProyectoEnVista = List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA, 
                		"7003-marcar_imagen_48x48.png", "Marcar Imagen para Proyecto", "proyecto_vista", ButtonType.TOGGLE),
                new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE, 
                		"3001-zoom_48x48.png", "Activar/Desactivar Zoom Manual", "proyecto_vista", ButtonType.TOGGLE),
                new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_RESET, 
                		"3008-reset_48x48.png", "Resetear Zoom", "proyecto_vista")
		);

        // --- BOTONES DE PROYECTOS ---
        List<ToolbarComponentDefinition> botonesProyectoEnProyecto = List.of(
                 new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_NUEVO, 
                		 "7102-nuevo_proyecto_48x48.png", "Nuevo Proyecto", "proyecto")
                , new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_ABRIR, 
                		"7103-abrir_proyecto_48x48.png", "Abrir Proyecto", "proyecto")
                , new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_GUARDAR, 
                		"7104-guardar_proyecto_48x48.png", "Guardar Proyecto", "proyecto")
                , new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_GUARDAR_COMO, 
                		"7105-guardar_proyecto_como_48x48.png", "Guardar Proyecto Como", "proyecto")
         );       
                
        List<ToolbarComponentDefinition> botonesAccionesProyecto = List.of(
        		  new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA, 
               		 "7101-marcar_imagen_48x48.png", "Cambia la iamgen de Seleccion a Descartes", "proyecto_accion", ButtonType.TOGGLE)
                 
                , new SeparatorDefinition()
                , new SeparatorDefinition()
                , new ToolbarButtonDefinition(AppActionCommands.CMD_EXPORT_ASSIGN_PANNEL, 
                		 "21017 - panel asignaciones .png", "Panel de Control de Asignaciones", "proyecto_accion", ButtonType.TOGGLE)
                
                , new SeparatorDefinition()
                , new SeparatorDefinition()
                , new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_COMPARTIR_CLIENTE, 
                		"21016-Ar-Environment.png", "Compartir al Cliente", "proyecto_accion")
                
//                , new SeparatorDefinition(), new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_ELIMINAR, 
//                		"7106-eliminar_proyecto_48x48.png", "Eliminar Proyecto", "proyecto_accion")// , ButtonType.TOGGLE)
                
        );

        // --- BARRA DE BOTONES modos ---
        List<ToolbarButtonDefinition> botonesModo = List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR,
                        "8001-modo_visualizador_48x48.png", "Modo Visualizador", "modo", ButtonType.TOGGLE)
                , new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_GESTIONAR,
                        "8002-mostrar_favoritos_48x48.png", "Modo Proyecto", "modo", ButtonType.TOGGLE)
                , new ToolbarButtonDefinition(AppActionCommands.CMD_MODO_CLIENTE, 
                		"21016-Ar-Environment.png", "Modo Cliente", "modo", ButtonType.TOGGLE)
                , new ToolbarButtonDefinition(AppActionCommands.CMD_MODO_DATOS, 
                		"8003-datos_48x48.png", "Modo Datos", "modo", ButtonType.TOGGLE)
                , new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_CAROUSEL, 
                		"4005-carrousel_48x48.png", "Vista Carrusel", "modo", ButtonType.TOGGLE)
                , new ToolbarButtonDefinition(AppActionCommands.CMD_MODO_RENDER, 
                		"8005-render mode.png", "Modo Render", "modo", ButtonType.TOGGLE)
                , new ToolbarButtonDefinition(AppActionCommands.CMD_MODO_EDITOR, 
                		"8006-edit-mode.png", "Modo Editor", "modo", ButtonType.TOGGLE)
        );

        List<ToolbarButtonDefinition> botonesModoBottom = List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_CONFIG_AVANZADA, 
                		"7005-settings_48x48.png", "Configuración Avanzada...", "modo_bottom", ButtonType.NORMAL)
                , new ToolbarButtonDefinition(AppActionCommands.CMD_CONFIG_MOSTRAR_VERSION, 
                		"7006-Information-48x48.png", "Ayuda", "modo_bottom", ButtonType.NORMAL)
        );
        
        // botones para los modos de edicion, carrousel, proyectos, gestion de Datos...

        // --- BARRA DE BOTONES carrousel ---
        List<ToolbarButtonDefinition> botonesCarrousel = List.of(

                new ToolbarButtonDefinition(AppActionCommands.CMD_CAROUSEL_REWIND, 
                		"9002-retroceso_rapido_48x48.png", "Retroceso Rapido", "carrousel"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_CAROUSEL_PLAY, 
                		"9004-play_48x48.png", "Play", "carrousel"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_CAROUSEL_PAUSE, 
                		"9005-pausa_48x48.png", "Pausa", "carrousel"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_CAROUSEL_STOP, 
                		"9007-stop_48x48.png", "Stop", "carrousel"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_CAROUSEL_TOGGLE_SHUFFLE, 
                		"9014-shuffle_48x48.png", "Modo Aleatorio", "carrousel", ButtonType.TOGGLE),
                new ToolbarButtonDefinition(AppActionCommands.CMD_CAROUSEL_FAST_FORWARD, 
                		"9009-avance_rapido_48x48.png", "Avance Rapido", "carrousel")
        );

        List<ToolbarComponentDefinition> botonesVelocidadCarrousel = List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_CAROUSEL_SPEED_INCREASE,
                        "9013-minima_velocidad_48x48.png", "Minima Velocidad", "velocidad_carrousel"),
                new LabelDefinition("label.velocidad.carrusel", "3.0s"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_CAROUSEL_SPEED_DECREASE,
                        "9011-maxima_velocidad_48x48.png", "Maxima Velocidad", "velocidad_carrousel"),
                new SeparatorDefinition(), new ToolbarButtonDefinition(AppActionCommands.CMD_CAROUSEL_SPEED_RESET,
                        "9012-velocidad_normal_48x48.png", "Velocidad Normal", "velocidad_carrousel")

        );

        // --- BARRA DE BOTONES DEL MODO CLIENTE ---
        //java.util.
        List<ToolbarComponentDefinition> botonesCliente = new java.util.ArrayList<>();
		        botonesCliente.add(new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_NUEVO, 
		        		"7102-nuevo_proyecto_48x48.png", "Nuevo proyecto", "cliente"));
		        botonesCliente.add(new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_ABRIR, 
		        		"7103-abrir_proyecto_48x48.png", "Abrir proyecto", "cliente"));
		        botonesCliente.add(new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_GUARDAR, 
		        		"7104-guardar_proyecto_48x48.png", "Guardar proyecto", "cliente"));
		        botonesCliente.add(new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_GUARDAR_COMO, 
		        		"7105-guardar_proyecto_como_48x48.png", "Guardar proyecto como...", "cliente")
		);

        // --- BARRA DE BOTONES DE COMPARTIR CON EL CLIENTE ---
        List<ToolbarComponentDefinition> componentesCompartirCliente = List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_CLIENTE_CARGAR_RESPUESTA, 
                		"60201-User-Followers.png", "Importar respuesta del cliente (JSON)", "compartir_cliente")
                , new SeparatorDefinition()
                , new ToolbarButtonDefinition(AppActionCommands.CMD_CLIENTE_EXPORTAR_PDF, 
                		"21013-File-Pdf.png", "Crear PDF", "compartir_cliente")
                , new SeparatorDefinition()
                , new ToolbarButtonDefinition(AppActionCommands.CMD_CLIENTE_EXPORTAR_WEB, 
                		"60202-User-Following.png", "Exportar catálogo web para el cliente", "compartir_cliente")
                , new ToolbarButtonDefinition(AppActionCommands.CMD_CLIENTE_EXPORTAR_HTML, 
                		"60203-User-Share.png", "Exportar HTML único para el cliente", "compartir_cliente")
                
                , new SeparatorDefinition()
                
                , new ToolbarButtonDefinition(AppActionCommands.CMD_CLIENTE_TOGGLE_OCULTAR_DESCARTES, 
                		"60101-Rotate-X-Axis.png", "No exportar descartes", "compartir_cliente", ButtonType.TOGGLE)
		);
        
        // --- BARRA DE BOTONES DEL EDITOR DE CHECKBOXES ---
        List<ToolbarComponentDefinition> botonesCheckboxEditor = List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_CLIENTE_CHECKBOX_ADD, 
                		"60301-Cursor-Area-Selection-1.png", "Añadir checkbox", "editor_checkboxes"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_CLIENTE_CHECKBOX_ADD_LABEL, 
                		"60302-Text-Select-Start.png", "Añadir label", "editor_checkboxes"),
                new SeparatorDefinition(),
                new ToolbarButtonDefinition(AppActionCommands.CMD_CLIENTE_CHECKBOX_FIN_EDICION, 
                		"60303-Check-Square-2.png", "Finalizar edición de checkboxes", "editor_checkboxes")
        );

        // --- BARRA DE BOTONES VISOR/EDITOR ---
        List<ToolbarComponentDefinition> botonesVisorEditor = List.of(
        		new ToolbarButtonDefinition(AppActionCommands.CMD_CLIENTE_VIEW_VISOR, 
        				"60102-Assistant-Square.png", "Visor de imagenes", "visor_editor", ButtonType.TOGGLE),
        		new SeparatorDefinition(),
                new ToolbarButtonDefinition(AppActionCommands.CMD_CLIENTE_TOGGLE_EDITOR_PANEL, 
                		"60206-Word-Wrap-Around-Bounding-Box.png", "Editor de checkboxes", "visor_editor", ButtonType.TOGGLE),
                new SeparatorDefinition(),
                new ToolbarButtonDefinition(AppActionCommands.CMD_CLIENTE_EDITAR, 
                		"60204-User-Edit-Pencil.png", "Editar selección", "visor_editor", ButtonType.TOGGLE)
        );

        // --- BARRA DE BOTONES Sync ---
        List<ToolbarButtonDefinition> botonesSincronizacion = List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_TOGGLE_SYNC_VISOR_CARRUSEL, 
                		"10001-sync_on_48x48.png", "Sincronizar Visor y Carrusel", "sincronizacion", ButtonType.TOGGLE)
                
                //FUNCION PENDIENTE. NO ESTA CLARO SI SIGUE SIENDO UTIL
//                ,new ToolbarButtonDefinition(AppActionCommands.CMD_TOGGLE_SYNC_COLECCION_CARPETA,
//                        "10005-sync_file_collection_48x48.png", "Sincronizar Coleccion y Carpeta", "sincronizacion", ButtonType.TOGGLE)
                
                
        // ,new
        // ToolbarButtonDefinition(AppActionCommands.CMD_COLECCION_AÑADIR_CARPETA_ACTUAL,
        // "10006-add_actual_folder_48x48.png", "Añadir Carpeta Actual a la Coleccion",
        // "sincronizacion")
        );

        // --- BARRA DE GESTIÓN DE DATOS (NUEVA) ---
        List<ToolbarButtonDefinition> botonesGestionDatos = List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_COLECCION_AÑADIR_CARPETA_ACTUAL,
                        "10005-sync_file_collection_48x48.png", "Añadir Carpeta a la Colección", "gestion_datos")
        );

        // --- BARRA DE BOTONES orden ---
        List<ToolbarComponentDefinition> botonesOrdenLista = List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_ORDEN_CARPETA_RAIZ, 
                		"30001-carpeta_raiz.png", "Carpeta Raiz", "orden_lista"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_ORDEN_CARPETA_ANTERIOR, 
                		"30002-subir_carpeta.png", "Subir Subcarpeta", "orden_lista"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_ORDEN_CARPETA_SIGUIENTE, 
                		"30003-bajar_carpeta.png", "Entrar en Subcarpeta", "orden_lista"),
                
                new SeparatorDefinition(),
                
                new ToolbarButtonDefinition(AppActionCommands.CMD_ORDEN_CICLO, 
                		"30004-orden_ascendente.png", "Orden de Archivos", "orden_lista"),
                
                new SeparatorDefinition(), 
                
                new TextFieldDefinition("textfield.filtro.orden", "", 30),
                
                new ToolbarButtonDefinition(AppActionCommands.CMD_FILTRO_TOGGLE_LIVE_FILTER, 
                		"40001-filter_48x48.png", "Activar/Desactivar Filtro en Vivo", "orden_lista", ButtonType.TOGGLE),
                new ToolbarButtonDefinition(AppActionCommands.CMD_FILTRO_ACTIVO, 
                		"40011-cambio_de_filtro_48x48.png", "Añadir Filtro Positivo (+)", "orden_lista", ButtonType.NORMAL)
        );

        // --- BARRA DE BOTONES filtros ---
        // filtrado por extension (bmp, gif, png...), filtros por tags, filtros por
        // letra inicial...

        List<ToolbarComponentDefinition> componentesFiltro = List.of(
                // El JComboBox para elegir la fuente (Nombre/Carpeta) lo añadiremos más tarde.
                // Por ahora, solo el campo de texto y los botones.

                // new ToolbarButtonDefinition(AppActionCommands.CMD_FILTRO_ACTIVO,
                // "40011-cambio_de_filtro_48x48.png","Cambia el filtro
                // activo","barra_filtros",ButtonType.TOGGLE)
                // new TextFieldDefinition("textfield.filtro.texto", "",30)
                new ToolbarButtonDefinition(AppActionCommands.CMD_FILTRO_ADD_POSITIVE,
                        "40002-filter_positive_48x48.png", "Añadir Filtro Positivo (+)", "barra_filtros", ButtonType.NORMAL),
                // ,new ToolbarButtonDefinition(AppActionCommands.CMD_FILTRO_ADD_NEGATIVE,
                // "40003-filter_negative_48x48.png","Añadir Filtro Negativo
                // (-)","barra_filtros",ButtonType.NORMAL),
                
                new ToolbarButtonDefinition(AppActionCommands.CMD_FILTRO_REMOVE_SELECTED,
                        "40006-filter_delete_48x48.png", "Quitar Filtro Seleccionado", "barra_filtros", ButtonType.NORMAL),

                new SeparatorDefinition(),

                // ,new ToolbarButtonDefinition(AppActionCommands.CMD_FILTRO_SET_TYPE_FILENAME,
                // "40007-filter_file_48x48.png", "Filtro tipo
                // archivo","barra_filtros",ButtonType.TOGGLE)
                // ,new ToolbarButtonDefinition(AppActionCommands.CMD_FILTRO_SET_TYPE_FOLDER,
                // "40008-filter_folder_48x48.png", "Filtro tipo
                // carpeta","barra_filtros",ButtonType.TOGGLE)
                // ,new ToolbarButtonDefinition(AppActionCommands.CMD_FILTRO_SET_TYPE_TAG,
                // "40009-filter_tag_48x48.png", "Filtro tipo
                // etiqueta","barra_filtros",ButtonType.TOGGLE)

                new SeparatorDefinition(),

                // new ButtonGroupDefinition.ButtonGroupDefinition("start")
                // ,new ButtonGroupDefinition("start"),
                
                new ToolbarButtonDefinition(AppActionCommands.CMD_FILTRO_UP, 
                		"40004-filter_up_48x48.png", "Subir 1 nivel", "barra_filtros", ButtonType.NORMAL),
                new ToolbarButtonDefinition(AppActionCommands.CMD_FILTRO_DOWN, 
                		"40005-filter_down_48x48.png", "Bajar 1 nivel", "barra_filtros", ButtonType.NORMAL),
                // ,new ButtonGroupDefinition("end")

                new SeparatorDefinition(),

                new ToolbarButtonDefinition(AppActionCommands.CMD_FILTRO_CLEAR_ALL, 
                		"40010-filter_clear_48x48.png", "Limpiar Todos los Filtros", "barra_filtros", ButtonType.NORMAL)

        );

        // --- BARRA DE ZOOM PARA MINIATURAS (HORIZONTAL) ---
        List<ToolbarComponentDefinition> botones_zoom_miniaturas = List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_GRID_SIZE_UP_MINIATURA,
                        "50003-agrandar_miniatura_48x48.png", "Agrandar Miniaturas", "barra_zoom_miniaturas",
                        ButtonType.NORMAL),
                new ToolbarButtonDefinition(AppActionCommands.CMD_GRID_SIZE_DOWN_MINIATURA,
                        "50004-reducir_miniatura_48x48.png", "Reducir Miniaturas", "barra_zoom_miniaturas",
                        ButtonType.NORMAL)
        );

        // --- BARRA DE BOTONES Grid Control de proyecto---
        List<ToolbarComponentDefinition> botones_grid_proyecto = List.of(
//                new ToolbarButtonDefinition(AppActionCommands.CMD_EXPORT_ASSIGN_PANNEL, 
//                		"21005-iniciar_exportación.png", "Panel de Control de Asignaciones", "barra_grid_proyecto", ButtonType.TOGGLE),
//                
//                new SeparatorDefinition(),
                
                new ToolbarButtonDefinition(AppActionCommands.CMD_GRID_SHOW_STATE, 
                		"50005-mostrar_estado_48x48.png", "Mostrar Estado", "barra_grid_proyecto", ButtonType.TOGGLE),
                
                new SeparatorDefinition(),
                
                new ToolbarButtonDefinition(AppActionCommands.CMD_GRID_SET_TEXT, 
                		"50001-add_texto_48x48.png", "Añadir/Modificar Etiqueta", "barra_grid_proyecto", ButtonType.NORMAL),
                new ToolbarButtonDefinition(AppActionCommands.CMD_GRID_REMOVE_TEXT, 
                		"50002-subst_texto_48x48.png", "Borrar Etiqueta", "barra_grid_proyecto", ButtonType.NORMAL)
        );

        // --- BARRA DE CONTROL DE IMAGEN INFERIOR (CONSOLIDADA: D-Pad, Colores,
        // Cuadros, Paleta) ---
        // -- ICONOS INTERNOS DEL DPAD
        // 0: UP, 1: DOWN, 2: LEFT, 3: RIGHT
        List<HotspotDefinition> dpadPaneoHotspots = List.of(
                new HotspotDefinition(AppActionCommands.CMD_PAN_TOP_EDGE, "d-pad_up_48x48.png", "Panear Arriba",
                        IconScope.COMMON),
                new HotspotDefinition(AppActionCommands.CMD_PAN_BOTTOM_EDGE, "d-pad_down_48x48.png", "Panear Abajo",
                        IconScope.COMMON),
                new HotspotDefinition(AppActionCommands.CMD_PAN_LEFT_EDGE, "d-pad_left_48x48.png", "Panear Izquierda",
                        IconScope.COMMON),
                new HotspotDefinition(AppActionCommands.CMD_PAN_RIGHT_EDGE, "d-pad_right_48x48.png", "Panear Derecha",
                        IconScope.COMMON)
        );

        List<ToolbarButtonDefinition> botonesControlesImagenInferior = List.of(
                // 1. D-Pad (Definición actualizada)
                new ToolbarButtonDefinition(
                        "dpad.paneo", // Comando canónico/clave base para el componente D-Pad
                        "d-pad_none_48x48.png", // Icono base (cuando no hay hover)
                        "d-pad_all_48x48.png", // Icono secundario/presionado
                        IconScope.COMMON, // El icono base es común
                        "Control de Paneo", // Tooltip general
                        "controles_imagen_inferior", // Categoría
                        dpadPaneoHotspots // La lista de hotspots definida arriba
                ),

                new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_RESET, 
                		"3008-reset_48x48.png", "Resetear Zoom", "controles_imagen_inferior"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TOGGLE_TO_CURSOR,
                        "20001-zoom_al_cursor_48x48.png", "Activar/Desactivar Zoom al Cursor", "controles_imagen_inferior", ButtonType.TOGGLE),
                // 2. Botones de color de fondo

                // 2. Botones de color de fondo (¡con ActionCommands mejorados!)
                new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_THEME_COLOR, "stopw.png", 
                		IconScope.COMMON, "Fondo del Tema Actual", "controles_imagen_inferior",
                        /* ButtonType.NORMAL), // */ButtonType.TRANSPARENT),
                new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_1, "stopw.png",
                        IconScope.COMMON, "Ranura de color 1", "controles_imagen_inferior",
                        /* ButtonType.NORMAL), // */ButtonType.TRANSPARENT),
                new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_2, "stopw.png",
                        IconScope.COMMON, "Ranura de color 2", "controles_imagen_inferior",
                        /* ButtonType.NORMAL), // */ButtonType.TRANSPARENT),
                new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_3, "stopw.png",
                        IconScope.COMMON, "Ranura de color 3", "controles_imagen_inferior",
                        /* ButtonType.NORMAL), // */ButtonType.TRANSPARENT),
                new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_4, "stopw.png",
                        IconScope.COMMON, "Ranura de color 4", "controles_imagen_inferior",
                        /* ButtonType.NORMAL), // */ButtonType.TRANSPARENT),
                new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_CHECKERED, "stopw.png", 
                		IconScope.COMMON, "Fondo a Cuadros", "controles_imagen_inferior",
                        /* ButtpmType.NORMAL), // */ButtonType.TRANSPARENT),
                new ToolbarButtonDefinition(AppActionCommands.CMD_BACKGROUND_CUSTOM_COLOR, "paint-palette--streamline-core.png", 
                		IconScope.COMMON, "Seleccionar Color Personalizado...", "controles_imagen_inferior", ButtonType.TRANSPARENT)
        );

        // Toolbar de Exportar
        List<ToolbarComponentDefinition> componentesExportacion = List.of(

                new ToolbarButtonDefinition(AppActionCommands.CMD_EXPORT_DETALLES_SELECCION,
                        "21014-Merge.png", "Detalles de Archivos", "acciones_exportacion", ButtonType.TOGGLE)
                // new SeparatorDefinition(),

                // Botones de acciones sobre la tabla
                , new ToolbarButtonDefinition(AppActionCommands.CMD_EXPORT_ASIGNAR_ARCHIVO, 
                		"21002-asignar_archivo.png", "Asignar archivo manualmente", "acciones_exportacion")
                , new ToolbarButtonDefinition(AppActionCommands.CMD_EXPORT_IGNORAR_COMPRIMIDO,
                        "21003-ignorar_comprimido.png", "Ignorar archivo comprimido", "acciones_exportacion")
                , new ToolbarButtonDefinition(AppActionCommands.CMD_EXPORT_RELOCALIZAR_IMAGEN,
                        "21004-relocalizar_imagen.png", "Relocalizar Imagen", "acciones_exportacion")
                , new ToolbarButtonDefinition(AppActionCommands.CMD_EXPORT_REFRESH, 
                		"21010-estado_exportación.png", "Repasar ", "acciones_exportacion")
                , new ToolbarButtonDefinition(AppActionCommands.CMD_EXPORT_QUITAR_DE_COLA, 
                		"21001-quitar_de_cola.png", "Quitar de la cola", "acciones_exportacion")

                , new SeparatorDefinition()

                // Botón final para iniciar la exportación
//                , new ToolbarButtonDefinition(AppActionCommands.CMD_INICIAR_EXPORTACION, 
//                		"21005-iniciar_exportación.png", "Iniciar Exportación", "acciones_exportacion")
                
                , new SeparatorDefinition()
                
                , new ToolbarButtonDefinition(AppActionCommands.CMD_DETALLES_PDF_SELECCION,
                		"21006-items_exportar_48x48.png", "Detalles del PDF", "acciones_exportacion",ButtonType.TOGGLE)
                , new ToolbarButtonDefinition(AppActionCommands.CMD_EXPORTAR_PDF, 
                		"21013-File-Pdf.png", "Crear PDF", "acciones_exportacion")
                
//                , new SeparatorDefinition()
//                
//                , new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_COMPARTIR_CLIENTE, 
//                		"21016-Ar-Environment.png", "Compartir al Cliente", "acciones_exportacion")
                
                , new SeparatorDefinition()
                
                , new ToolbarButtonDefinition(AppActionCommands.CMD_CLIENTE_EXPORTAR_WEB, 
                		"60202-User-Following.png", "Exportar catálogo web para el cliente", "acciones_exportacion")
                , new ToolbarButtonDefinition(AppActionCommands.CMD_CLIENTE_EXPORTAR_HTML, 
                		"60203-User-Share.png", "Exportar HTML único para el cliente", "acciones_exportacion")
                
                , new SeparatorDefinition()
                , new SeparatorDefinition()
                
//                ,new ToolbarButtonDefinition(AppActionCommands.CMD_EXPORT_ASSIGN_PANNEL, 
//                		"21005-iniciar_exportación.png", "Panel de Control de Asignaciones", "barra_grid_proyecto", ButtonType.TOGGLE)
                , new ToolbarButtonDefinition(AppActionCommands.CMD_INICIAR_EXPORTACION, 
                		"21005-iniciar_exportación.png", "Iniciar Exportación", "acciones_exportacion")
                
                , new SeparatorDefinition()
                , new SeparatorDefinition(), new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_ELIMINAR, 
                		"7106-eliminar_proyecto_48x48.png", "Eliminar Proyecto", "acciones_exportacion")// , ButtonType.TOGGLE)
                
		);

        List<ToolbarComponentDefinition> componentesDetallesExportacion = List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_EXPORT_ADD_ASSOCIATED_FILE,
                        "21007-add_items_exportar_48x48.png", "Añadir Archivos", "acciones_det_exportacion"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_EXPORT_DEL_ASSOCIATED_FILE,
                        "21008-del_item_exportar.png", "Borrar Archivos", "acciones_det_exportacion"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_EXPORT_LOCATE_ASSOCIATED_FILE,
                        "21009-locate_item_exportar.png", "Localizar Archivos", "acciones_det_exportacion")
        );

        // Toolbar de Statusbar
        List<ToolbarComponentDefinition> componentesBarraEstado = List.of(
                  new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE, 
                		"3001-zoom_48x48.png", "Activar/Desactivar Zoom Manual", "barra_estado", ButtonType.TOGGLE)
                , new ToolbarButtonDefinition(AppActionCommands.CMD_TOGGLE_SUBCARPETAS, 
                		"7001-subcarpetas_48x48.png", "Incluir/Excluir Subcarpetas", "barra_estado", ButtonType.TOGGLE)
                , new ToolbarButtonDefinition(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES,
                        "7002-mantener_proporciones_48x48.png", "Mantener Proporciones", "barra_estado", ButtonType.TOGGLE) 
                , new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP,
                        "7004-siempre_encima_48x48.png", "Mantener Siempre Encima", "barra_estado", ButtonType.TOGGLE)
                
                , new SeparatorDefinition()
                
                , new LabelDefinition("label.control.zoomPorcentaje", "Z: 100%")
                
                // new ToolbarButtonDefinition("cmd.control.modoZoom", "imagen.png", "Cambiar
                // Modo de Zoom", "barra_estado") // Usamos un comando simple, no de
                // AppActionCommands
                
                , new ToolbarButtonDefinition("cmd.control.modoZoom", 
                		"3005-escalar_para_ajustar_48x48.png", "Cambiar Modo de Zoom", "barra_estado", ButtonType.STATUS_BAR_BUTTON)

        );
        
        // 
        List<ToolbarComponentDefinition> componentesToolbarRender = List.of(
        		
        		new ToolbarButtonDefinition(AppActionCommands.CMD_RENDER_ESCANEAR_CARPETA,
                        "70001 - escanear carpeta.png", "Escanear carpeta", "toolbarRender")
        		
        		, new SeparatorDefinition()
        		
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_RENDER_PROCESAR_SELECCIONADOS,
                        "70002 - procesar seleccionados.png", "Procesar seleccionados", "toolbarRender")
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_RENDER_PROCESAR_ARCHIVO,
                        "70003 - procesar archivo.png", "Procesar archivo", "toolbarRender")
        		
        		, new SeparatorDefinition()
        		
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_RENDER_COPIAR_ARCHIVOS,
                        "70004 - copiar a origen.png", "Copiar archivos", "toolbarRender")
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_RENDER_DESCARGAR_ARCHIVOS,
                        "7008 - descarga.png", "Guardar Prevew en Disco", "toolbarRender")
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_RENDER_ABRIR_TEMP,
                        "70006 - abrir temp.png", "Abrir carpeta temporal", "toolbarRender")
        		
        		, new SeparatorDefinition()
        		
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_RENDER_ASIGNAR_PREVIEW,
                        "70007 - establece_imagen.png", "Asigna preview al archivo", "toolbarRender")
        		
        		, new SeparatorDefinition()
        		
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_RENDER_CLEAR_PREVIEW,
                        "70008-clear-preview.png", "Limpia el Panel de Preview", "toolbarRender")
        		
        		
        );
        
        List<ToolbarComponentDefinition> componentesToolbarRenderCenter = List.of(
        		new ToolbarButtonDefinition(AppActionCommands.CMD_RENDER_ESCANEAR_CARPETA,
                        "70001 - escanear carpeta.png", "Escanear carpeta", "toolbarRenderCenter")
        		
        		, new SeparatorDefinition()
        		
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_RENDER_PROCESAR_SELECCIONADOS,
                        "70002 - procesar seleccionados.png", "Procesar seleccionados", "toolbarRenderCenter")
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_RENDER_PROCESAR_ARCHIVO,
                        "70003 - procesar archivo.png", "Procesar archivo", "toolbarRenderCenter")
        		
        		, new SeparatorDefinition()
        		
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE /*CMD_RENDER_PROCESAR_HUERFANOS*/,
                        "70009-scanner.png", "Escanear Archivos Sin Representacion", "toolbarRenderCenter")
        		
        		
		);
        
        List<ToolbarComponentDefinition> componentesPreviewRenderView = List.of(
        		
        		new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_3DGRID,
                        "70101-3d_grid.png", "Grid Archivos Sin Imagen", "previewrenderview", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_2DGRID,
                        "70102-2d_grid.png", "Grid Archivos Con Imagen", "previewrenderview", ButtonType.TOGGLE)

        		, new SeparatorDefinition()
        		
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_ADVANCE_EDIT,
        				"70105-edition-pro.png", "Edicion Avanzada", "previewrenderview", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_COLLAGE,
                        "70103-multimage.png", "Panel Edicion Multi-Imagen", "previewrenderview", ButtonType.TOGGLE)
        		
        		, new SeparatorDefinition()
        		
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_GALLERY,
                        "70104-gallery-view.png", "Ver galeria del archivo comprimido", "previewrenderview", ButtonType.TOGGLE)
		);
        
        List<ToolbarComponentDefinition> toolbarEditorAvanzado = List.of(
        		  new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_EDICION,
                        "80000-edicion.png", "Edicion", "tbeditoravanzado", ButtonType.TOGGLE)
        		  
        		, new SeparatorDefinition()
        		
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_TRANSFORMAR,
                        "80001-transform.png", "Transformar", "tbeditoravanzado", ButtonType.TOGGLE)
        		
        		, new SeparatorDefinition()
        		
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_MARCO,
        				"80002-seleccion-marco.png", "Selección por marco", "tbeditoravanzado", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_CAPA,
                        "80003-seleccion-layer.png", "Selección por capa", "tbeditoravanzado", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_VARITA,
                        "80004-varita.png", "Varita mágica", "tbeditoravanzado", ButtonType.TOGGLE)
        		
        		, new SeparatorDefinition()
        		
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_RECORTAR,
                        "80005.-recortar.png", "Recortar", "tbeditoravanzado", ButtonType.TOGGLE)
        		
        		, new SeparatorDefinition()
        		
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_CUENTAGOTAS,
                        "80006-cuentagotas.png", "Capturar color bajo el cursor", "tbeditoravanzado", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_BOTE_PINTURA,
                        "80007-bote-pintura.png", "Rellenar con un color sólido", "tbeditoravanzado", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_DEGRADADO,
                        "80008-degradado.png", "Rellena con un degradado", "tbeditoravanzado", ButtonType.TOGGLE)
        		
        		, new SeparatorDefinition()
        		
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_TEXTO,
                        "80009-text.png", "Crear capa de texto", "tbeditoravanzado", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_FORMAS,
                        "80010-formas.png", "Crear formas básicas", "tbeditoravanzado", ButtonType.TOGGLE)
        		
        		, new SeparatorDefinition()
        		
         		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_ZOOM,
                        "80011-zoom.png", "Acercar y alejar", "tbeditoravanzado", ButtonType.TOGGLE)
         		
         		, new SeparatorDefinition()
         		
         		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_PANTALLA_COMPLETA,
                        "80012-pantalla_completa_48x48.png", "Expandir el editor ocultando el resto de la interfaz", "tbeditoravanzado", ButtonType.TOGGLE)
        );
        
        List<ToolbarComponentDefinition> componentbarEditorAvanzadoColor = List.of(
         		  new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_RESET_COLORS,
                        "80013-reset-colors.png", "Restablecer colores por defecto", "editoravanzadocolor", ButtonType.TOGGLE)
         		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_INVERT_COLORS,
                        "80014-invert-colors.png", "Intercambiar colores frontal/fondo", "editoravanzadocolor", ButtonType.TOGGLE)
        );
        
        List<ToolbarComponentDefinition> componentbarEditorAvanzadoTransform = List.of(
        		  new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_MOVE,
                        "80101-move.png", "Mover", "editoravanzadotransform", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_SCALE,
                        "80102-escalar.png", "Escalar", "editoravanzadotransform", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_ROTATE,
                        "80103-rotate.png", "Rotar", "editoravanzadotransform", ButtonType.TOGGLE)
        );
        
        List<ToolbarComponentDefinition> componentbarEditorAvanzadoLayerSelection = List.of(
         		  new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_PAGE,
                        "80120-target-page.png", "Tamaño del Lienzo", "editoravanzadolayerselection", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_LAYER,
                        "80104-transform-layer.png", "Transformar una capa", "editoravanzadolayerselection", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_MARCO,
                        "80105-transform.png", "Transformar un marco de selección", "editoravanzadolayerselection", ButtonType.TOGGLE)
        );
        
        
        List<ToolbarComponentDefinition> componentbarEditorAvanzadoZoomType = List.of(
        		    new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_ZOOM_CENTER,
                        "80112-zoom_al_centro_48x48.png", "Zoom al centro de la imagen", "editoravanzadozoomtype", ButtonType.TOGGLE)
        		  , new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_ZOOM_CURSOR,
                          "80111-zoom_al_cursor_48x48.png", "Zoom hacia el puntero del raton", "editoravanzadozoomtype", ButtonType.TOGGLE)
        );
        
        
        List<ToolbarComponentDefinition> componentbarEditorAvanzadoCrop = List.of(
        		  new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_CROP_SELECTION,
                        "80502-recortar-eliminar.png", "Recorta eliminando seleccion", "editoravanzadocrop", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_CROP_LAYER,
                        "80503-recortar-nueva-capa.png", "Recorta Creando una Nueva Capa", "editoravanzadocrop", ButtonType.TOGGLE)
        );
        
        List<ToolbarComponentDefinition> componentbarEditorAvanzadoTexto = List.of(
        		
        		//este boton solo es un icono que acompaña al jcombobox con la lista de fuentes
        		  new ToolbarButtonDefinition(AppActionCommands.CMD_EDITOR_TEXTO_FUENTE,
        				"80900-search-font.png", "", "editoravanzadotexto", ButtonType.TOGGLE)
        		  
        		//aqui va el jcombobox con la lista de fuentes  
        		  
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_EDITOR_TEXTO_NEGRITA,
                        "80901-bold-text.png", "Negrita", "editoravanzadotexto", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_EDITOR_TEXTO_CURSIVA,
                        "80902-italic-text.png", "Cursiva", "editoravanzadotexto", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_EDITOR_TEXTO_SUBRAYADO,
                        "80903-underline-text.png", "Subrayado", "editoravanzadotexto", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_EDITOR_TEXTO_TACHADO,
                        "80904-tachado.png", "Tachado", "editoravanzadotexto", ButtonType.TOGGLE)
        		
        		
        		// este boton en realidad solo es el icono que acompaña al jcombobox con la lista de tamaños de fuente
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_EDITOR_TEXTO_TAMANO,
                        "80905-font-size.png", "", "editoravanzadotexto", ButtonType.TOGGLE)
        		
        		// aqui va un JComboBox con los tamaños de la fuente 
        		
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_LEFT,
                        "80906-align-left.png", "Alinear a la Izquierda", "editoravanzadotexto", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_CENTER,
                        "80907-align-center.png", "Alinear al Centro", "editoravanzadotexto", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_RIGHT,
                        "80908-align-right.png", "Alinear a la Derecha", "editoravanzadotexto", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_JUSTIFY,
                        "80909-justified.png", "Justificado", "editoravanzadotexto", ButtonType.TOGGLE)
        		
        		// estos 2 botones son el mismo con 2 iconos
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_EDITOR_TEXTO_FLOW_ROWS,
                        "80910-text-flow-rows.png", "Texto en Linea", "editoravanzadotexto", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_EDITOR_TEXTO_FLOW_COLUMNS,
                        "80911-text-flow-columns.png", "Texto en Columna", "editoravanzadotexto", ButtonType.TOGGLE)
        		
        		// estos 2 botones son el mismo con 2 iconos        		
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_EDITOR_TEXTO_HORIZONTAL,
                        "80912-horizontal-text.png", "Texto Horizontal", "editoravanzadotexto", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_EDITOR_TEXTO_VERTICAL,
                        "80913-vertical-text.png", "Texto Vertical", "editoravanzadotexto", ButtonType.TOGGLE)
        );		
        
        
        List<ToolbarComponentDefinition> componentbarEditorAvanzadoConfirmacion = List.of(
        		  new ToolbarButtonDefinition(AppActionCommands.CMD_HOME_ACEPTAR,
                        "82001-aceptar.png", "Acepta el Proceso", "editoravanzadoconfirmacion", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_HOME_CANCELAR,
                        "82002-cancelar.png", "Cancela el Proceso", "editoravanzadoconfirmacion", ButtonType.TOGGLE)
        );
        
        
        List<ToolbarComponentDefinition> componentbarEditorAvanzadoFormas = List.of(
        		
        		  new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_SHAPE_RECT,
                        "81000-shape-rect.png", "Rectangulo", "editoravanzadoformas", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_SHAPE_LINE,
                        "81002-shape-line.png", "Linea", "editoravanzadoformas", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_SHAPE_ELLIPSE,
                        "81001-shape-ellipse.png", "Elipse", "editoravanzadoformas", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_SHAPE_TRIANGLE,
                        "81003-shape-triangle.png", "Triangulo", "editoravanzadoformas", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_SHAPE_POLYGON,
                        "81004-shape-polygon.png", "Poligono", "editoravanzadoformas", ButtonType.TOGGLE)
        		
        		// estos 3 botones no tienen icono porque son especiales
        			// label "Relleno" + cuadrado con el color del relleno que al pulsarlo abre el selector de color 
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_FILL_COLOR,
                        "", "Relleno", "editoravanzadoformas", ButtonType.TOGGLE)
        			// label "Borde" + cuadrado con el color del relleno que al pulsarlo abre el selector de color
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_STROKE_COLOR,
                        "", "Borde", "editoravanzadoformas", ButtonType.TOGGLE)
        			// label "Grosor" + spin para los pixeles de grosor
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_ADVANCED_EDITOR_STROKE_WIDTH,
                        "", "Grosor de Borde", "editoravanzadoformas", ButtonType.TOGGLE)
        );
        
        
        List<ToolbarComponentDefinition> componentbarEditorAvanzadoHome = List.of(
        		new ToolbarButtonDefinition(AppActionCommands.CMD_HOME_CASA,
                        "90000-home.png", "Principal", "editoravanzadohome", ButtonType.TOGGLE)
		);
        
        
        List<ToolbarComponentDefinition> componentbarEditorAvanzadoHomeTools = List.of(
        		  new ToolbarButtonDefinition(AppActionCommands.CMD_HOME_LIENZO,
                        "90001-lienzo.png", "Lienzo", "editoravanzadohometools", ButtonType.TOGGLE)
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_HOME_CONTROL_IMAGEN,
                        "21016-Ar-Environment.png", "Imagen", "editoravanzadohometools", ButtonType.TOGGLE)
		);
        	
        
        List<ToolbarComponentDefinition> componentbarEditorAvanzadoHomeCanvas = List.of(        		
        		
        		  new ToolbarButtonDefinition(AppActionCommands.CMD_HOME_LIENZO_NUEVO,
                        "90002-nuevo-lienzo.png", "Nuevo Lienzo", "editoravanzadohomecanvas")
         		, new ToolbarButtonDefinition(AppActionCommands.CMD_HOME_LIENZO_MEDIDAS,
                        "80120-target-page.png", "Cambio de Medidas del Lienzo", "editoravanzadohomecanvas")
         		, new ToolbarButtonDefinition(AppActionCommands.CMD_HOME_LIENZO_AJUSTAR_CONTENIDO,
                        "80121-ajustar-lienzo.png", "Ajustar lienzo al contenido", "editoravanzadohomecanvas")
		);
        		
        		
        List<ToolbarComponentDefinition> componentbarEditorAvanzadoHomeImage = List.of(        		
        		  new ToolbarButtonDefinition(AppActionCommands.CMD_HOME_IMPORTAR,
                        "90101-importar-imagen.png", "Importar", "editoravanzadohomeimage")
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_HOME_EXPORTAR,
                        "90102-exportar-imagen.png", "Exportar", "editoravanzadohomeimage")
        		, new ToolbarButtonDefinition(AppActionCommands.CMD_HOME_EXPORTAR_PREVIEW,
                        "90103-export-to-preview.png", "Enviar a Preview", "editoravanzadohomeimage")
		);
        
        
        
        
        
        
        List<ToolbarComponentDefinition> componentesLayerOrderPreviewRender = getComponentesLayerOrderPreviewRender();
        List<ToolbarComponentDefinition> componentesLayerLoadPreviewRender = getComponentesLayerLoadPreviewRender();
        
        // =====================================================================
        // DEFINICIÓN DE TOOLBARS
        // =====================================================================
        // Cada toolbar se declara como variable local, agrupada por alineación.
        // Rangos de orden:
        //   LEFT   10-199  | CENTER 200-399 | RIGHT 400-599
        //   EAST   600-799 | FREE   1000+
        // =====================================================================

        // ==================== LEFT ====================

        final ToolbarDefinition tbAbrirCarpeta = new ToolbarDefinition(
                "abrir_carpeta", "Abrir Carpeta", 10,
                EnumSet.of(VISOR, SLIDER/*, RENDER*/),
                List.copyOf(botonesAbrirCarpeta), tbarLeft, Set.of(tbarEast));

        final ToolbarDefinition tbNavegacion = new ToolbarDefinition(
        		"navegacion", "Navegación", 20,
        		EnumSet.of(VISOR, PROJECT, SLIDER, CLIENT),
        		List.copyOf(botonesNavegacion), tbarLeft);
        
        final ToolbarDefinition tbProyecto = new ToolbarDefinition(
                "proyecto", "Proyecto", 30,
                EnumSet.of(PROJECT),
                List.copyOf(botonesProyectoEnProyecto), tbarLeft);

        final ToolbarDefinition tbCliente = new ToolbarDefinition(
                "cliente", "Cliente", 40,
                EnumSet.of(CLIENT),
                List.copyOf(botonesCliente), tbarLeft);

        // ==================== CENTER ====================
        final ToolbarDefinition tbEdicion = new ToolbarDefinition(
                "edicion", "Edición", 200,
                EnumSet.of(VISOR),
                List.copyOf(botonesEdicion), tbarCenter);

        final ToolbarDefinition tbZoom = new ToolbarDefinition(
                "zoom", "Zoom", 210,
                EnumSet.of(VISOR, DATA, PROJECT, CLIENT),
                List.copyOf(botonesZoom), tbarCenter);

        final ToolbarDefinition tbVista = new ToolbarDefinition(
                "vista", "Vista", 220,
                EnumSet.of(VISOR, PROJECT, DATA, CLIENT),
                List.copyOf(botonesVista), tbarCenter);

        final ToolbarDefinition tbZoomMiniaturas = new ToolbarDefinition(
                "barra_zoom_miniaturas", "Zoom de Miniaturas", 230,
                EnumSet.of(VISOR, DATA, PROJECT, CLIENT),
                botones_zoom_miniaturas, tbarCenter);

        final ToolbarDefinition tbCarrousel = new ToolbarDefinition(
                "carrousel", "Carrousel", 240,
                EnumSet.of(SLIDER),
                List.copyOf(botonesCarrousel), tbarCenter);

        final ToolbarDefinition tbVelocidadCarrousel = new ToolbarDefinition(
                "velocidad_carrousel", "Velocidad", 250,
                EnumSet.of(SLIDER),
                botonesVelocidadCarrousel, tbarCenter);

        final ToolbarDefinition tbGestionDatos = new ToolbarDefinition(
                "gestion_datos", "Gestión de Colección", 260,
                EnumSet.of(DATA),
                List.copyOf(botonesGestionDatos), tbarCenter);

        final ToolbarDefinition tbEditorCheckboxes = new ToolbarDefinition(
                "editor_checkboxes", "Editor de Checkboxes", 270,
                EnumSet.noneOf(WorkMode.class),
                List.copyOf(botonesCheckboxEditor), tbarCenter);
        
        final ToolbarDefinition  tbToolbarRenderCenter= new ToolbarDefinition(
                "toolbarRenderCenter", "Controles", 265,
                EnumSet.of(RENDER),
                List.copyOf(componentesToolbarRenderCenter), tbarCenter);

        // ==================== RIGHT ====================
        final ToolbarDefinition tbControl = new ToolbarDefinition(
                "control", "Utilidades", 400,
                EnumSet.of(VISOR, PROJECT, CLIENT),
                List.copyOf(botonesUtils), tbarRight);

        final ToolbarDefinition tbToggle = new ToolbarDefinition(
                "toggle", "Toggles", 410,
                EnumSet.of(VISOR, SLIDER),
                List.copyOf(botonesToggle), tbarRight);

        final ToolbarDefinition tbEspeciales = new ToolbarDefinition(
                "especiales", "Apoyo", 420,
                EnumSet.of(VISOR, PROJECT, DATA, CLIENT, SLIDER, RENDER),
                List.copyOf(botonesApoyo), tbarRight);

        // ==================== EAST ====================
        final ToolbarDefinition tbProyectoVista = new ToolbarDefinition(
                "proyecto_vista", "Proyecto (Vista)", 600,
                EnumSet.of(VISOR, DATA, SLIDER),
                List.copyOf(botonesProyectoEnVista), tbarEast);

        final ToolbarDefinition tbSincronizacion = new ToolbarDefinition(
                "sincronizacion", "Sincronización", 610,
                EnumSet.of(VISOR, SLIDER),
                List.copyOf(botonesSincronizacion), tbarEast);

        final ToolbarDefinition tbProyectoAccion = new ToolbarDefinition(
                "proyecto_accion", "Acciones", 620,
                EnumSet.of(PROJECT),
                List.copyOf(botonesAccionesProyecto), tbarEast);

        final ToolbarDefinition tbCompartirCliente = new ToolbarDefinition(
                "compartir_cliente", "Compartir con el Cliente", 630,
                EnumSet.of(CLIENT),
                List.copyOf(componentesCompartirCliente), tbarEast);

        final ToolbarDefinition tbVisorEditor = new ToolbarDefinition(
                "visor_editor", "Visor/Editor", 640,
                EnumSet.of(CLIENT),
                List.copyOf(botonesVisorEditor), tbarEast);

        final ToolbarDefinition tbClienteCerrar = new ToolbarDefinition(
                "cliente_cerrar", "Cerrar Cliente", 650,
                EnumSet.of(CLIENT),
                List.of(new ToolbarButtonDefinition(AppActionCommands.CMD_CLIENTE_CERRAR_SINCRONIZAR,
                        "60205-User-Check-Validate.png",
                        "Cerrar y sincronizar con el cliente", "cliente_cerrar")),
                tbarEast);

        final ToolbarDefinition tbAccionesExportacion = new ToolbarDefinition(
                "acciones_exportacion", "Acciones de Exportación", 660,
                EnumSet.of(PROJECT),
                componentesExportacion, tbarEast);
        
        final ToolbarDefinition tbToolbarRender = new ToolbarDefinition(
		        "toolbarRender", "Acciones de Renderizado", 670,
		        EnumSet.of(RENDER),
		        componentesToolbarRender, tbarEast);

        final ToolbarDefinition tbPreviewRenderView = new ToolbarDefinition(
                "previewrenderview", "Vista Preview Render", 680,
                EnumSet.of(RENDER),
                componentesPreviewRenderView, tbarCenter);

        final ToolbarDefinition tbLayerOrderPreview = new ToolbarDefinition(
                "layerorderpreview", "Orden de Capas", 681,
                EnumSet.of(RENDER),
                componentesLayerOrderPreviewRender, tbarEast);

        final ToolbarDefinition tbLayerLoadPreview = new ToolbarDefinition(
                "layerloadpreview", "Carga de Capas", 682,
                EnumSet.of(RENDER),
                componentesLayerLoadPreviewRender, tbarEast);

        // ==================== FREE ====================
        final ToolbarDefinition tbTextfieldDestino = new ToolbarDefinition(
                "textfield_destino", "Carpeta Destino", 1000,
                EnumSet.of(PROJECT),
                List.of(new TextFieldDefinition("textfield.export.destino",
                        "Seleccione una carpeta de destino...", 30)),
                tbarFree);

        final ToolbarDefinition tbAccionesDetExportacion = new ToolbarDefinition(
                "acciones_det_exportacion", "Detalles de Exportación", 1010,
                EnumSet.of(PROJECT),
                componentesDetallesExportacion, tbarFree);

        final ToolbarDefinition tbControlesImagenInferior = new ToolbarDefinition(
                "controles_imagen_inferior", "Controles de Imagen", 1020,
                EnumSet.of(VISOR, CLIENT),
                List.copyOf(botonesControlesImagenInferior), tbarFree);

        final ToolbarDefinition tbBarraEstadoControles = new ToolbarDefinition(
                "barra_estado_controles", "Controles de Estado", 1030,
                EnumSet.allOf(WorkMode.class),
                componentesBarraEstado, tbarFree);

        final ToolbarDefinition tbBotonesOrdenLista = new ToolbarDefinition(
                "botonesOrdenLista", "Orden de Lista", 1040,
                EnumSet.allOf(WorkMode.class),
                botonesOrdenLista, tbarFree);

        final ToolbarDefinition tbBarraFiltros = new ToolbarDefinition(
                "barra_filtros", "Herramientas de Filtro", 1050,
                EnumSet.allOf(WorkMode.class),
                componentesFiltro, tbarFree);

        final ToolbarDefinition tbBarraGridProyecto = new ToolbarDefinition(
                "barra_grid_proyecto", "Controles de Proyecto de Grid", 1060,
                EnumSet.of(PROJECT),
                botones_grid_proyecto, tbarFree);

        final ToolbarDefinition tbModo = new ToolbarDefinition(
                "modo", "Modo", 1070,
                EnumSet.of(VISOR, PROJECT, DATA, SLIDER, CLIENT, RENDER, EDITOR),
                List.copyOf(botonesModo), tbarFree);

        // --- BARRA DE DOCUMENTO DEL EDITOR ---
        final List<ToolbarButtonDefinition> botonesDocumentoEditor = List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_EDITOR_NUEVO,
                        "7102-nuevo_proyecto_48x48.png", "Nuevo documento", "editor"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_EDITOR_ABRIR,
                        "7103-abrir_proyecto_48x48.png", "Abrir documento", "editor"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_EDITOR_GUARDAR,
                        "7104-guardar_proyecto_48x48.png", "Guardar documento", "editor"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_EDITOR_GUARDAR_COMO,
                        "7105-guardar_proyecto_como_48x48.png", "Guardar como", "editor"));

        final ToolbarDefinition tbBarraEditor = new ToolbarDefinition(
                "barra_editor", "Documento del Editor", 1075,
                EnumSet.of(EDITOR),
                List.copyOf(botonesDocumentoEditor), tbarLeft);

        final ToolbarDefinition tbModoBottom = new ToolbarDefinition(
                "modo_bottom", "Modo Bottom", 1080,
                EnumSet.of(VISOR, PROJECT, DATA, SLIDER, CLIENT, RENDER, EDITOR),
                List.copyOf(botonesModoBottom), tbarFree);

        final ToolbarDefinition tbEditorAvanzado = new ToolbarDefinition(
                "editoravanzado", "Editor Avanzado", 1090,
                EnumSet.of(RENDER),
                toolbarEditorAvanzado, tbarFree);

        // ==================== SUB-TOOLBARS EDITOR AVANZADO (Parte B) ====================
        final ToolbarDefinition tbEditorAvanzadoTransform = new ToolbarDefinition(
                "editoravanzadotransform", "Transformar", 1091,
                EnumSet.of(RENDER),
                componentbarEditorAvanzadoTransform, tbarFree);

        final ToolbarDefinition tbEditorAvanzadoLayerSelection = new ToolbarDefinition(
                "editoravanzadolayerselection", "Seleccion de Capa", 1092,
                EnumSet.of(RENDER),
                componentbarEditorAvanzadoLayerSelection, tbarFree);

        final ToolbarDefinition tbEditorAvanzadoZoomType = new ToolbarDefinition(
                "editoravanzadozoomtype", "Tipo de Zoom", 1093,
                EnumSet.of(RENDER),
                componentbarEditorAvanzadoZoomType, tbarFree);

        final ToolbarDefinition tbEditorAvanzadoCrop = new ToolbarDefinition(
                "editoravanzadocrop", "Recortar", 1094,
                EnumSet.of(RENDER),
                componentbarEditorAvanzadoCrop, tbarFree);

        final ToolbarDefinition tbEditorAvanzadoTexto = new ToolbarDefinition(
                "editoravanzadotexto", "Texto", 1095,
                EnumSet.of(RENDER),
                componentbarEditorAvanzadoTexto, tbarFree);

        final ToolbarDefinition tbEditorAvanzadoFormas = new ToolbarDefinition(
                "editoravanzadoformas", "Formas", 1096,
                EnumSet.of(RENDER),
                componentbarEditorAvanzadoFormas, tbarFree);

        final ToolbarDefinition tbEditorAvanzadoColor = new ToolbarDefinition(
                "editoravanzadocolor", "Colores", 1102,
                EnumSet.of(RENDER),
                componentbarEditorAvanzadoColor, tbarFree);

        final ToolbarDefinition tbEditorAvanzadoConfirmacion = new ToolbarDefinition(
                "editoravanzadoconfirmacion", "Confirmacion", 1097,
                EnumSet.of(RENDER),
                componentbarEditorAvanzadoConfirmacion, tbarFree);

        final ToolbarDefinition tbHome = new ToolbarDefinition(
                "editoravanzadohome", "Home", 1098,
                EnumSet.of(RENDER),
                componentbarEditorAvanzadoHome, tbarFree);

        final ToolbarDefinition tbHomeTools = new ToolbarDefinition(
                "editoravanzadohometools", "Home Tools", 1099,
                EnumSet.of(RENDER),
                componentbarEditorAvanzadoHomeTools, tbarFree);

        final ToolbarDefinition tbHomeCanvas = new ToolbarDefinition(
                "editoravanzadohomecanvas", "Home Canvas", 1100,
                EnumSet.of(RENDER),
                componentbarEditorAvanzadoHomeCanvas, tbarFree);

        final ToolbarDefinition tbHomeImage = new ToolbarDefinition(
                "editoravanzadohomeimage", "Home Image", 1101,
                EnumSet.of(RENDER),
                componentbarEditorAvanzadoHomeImage, tbarFree);

        return List.of(
                // LEFT
                tbNavegacion,
                tbAbrirCarpeta,
                tbProyecto,
                tbCliente,
                tbBarraEditor,

                // CENTER
                tbEdicion,
                tbZoom,
                tbVista,
                tbZoomMiniaturas,
                tbCarrousel,
                tbVelocidadCarrousel,
                tbGestionDatos,
                tbEditorCheckboxes,
                tbPreviewRenderView,
                tbToolbarRenderCenter,

                // RIGHT
                tbControl,
                tbToggle,
                tbEspeciales,

                // EAST
                tbProyectoVista,
                tbSincronizacion,
                tbProyectoAccion,
                tbCompartirCliente,
                tbVisorEditor,
                tbClienteCerrar,
                tbAccionesExportacion,
                tbToolbarRender,
                tbLayerOrderPreview,
                tbLayerLoadPreview,

                // FREE
                tbTextfieldDestino,
                tbAccionesDetExportacion,
                tbControlesImagenInferior,
                tbBarraEstadoControles,
                tbBotonesOrdenLista,
                tbBarraFiltros,
                tbBarraGridProyecto,
                tbModo,
                tbModoBottom,
                tbEditorAvanzado,
                tbEditorAvanzadoTransform,
                tbEditorAvanzadoLayerSelection,
                tbEditorAvanzadoZoomType,
                tbEditorAvanzadoCrop,
                tbEditorAvanzadoTexto,
                tbEditorAvanzadoFormas,
                tbEditorAvanzadoColor,
                tbEditorAvanzadoConfirmacion,
                tbHome,
                tbHomeTools,
                tbHomeCanvas,
                tbHomeImage);

     }// --- FIN DEL METODO generateModularToolbarStructure ---

    // -----------------------------------------------------------------------
    // Getters públicos para listas de componentes de capa (AdvanceEditPanel)
    // -----------------------------------------------------------------------

    public List<ToolbarComponentDefinition> getComponentesLayerAlign() {
        return List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_BORDE_INFERIOR,
                        "70401-align-borde-inferior.png", "Alinear Bordes Inferiores", "layeralign"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_CENTRO_VERTICAL,
                        "70402-align-centro-vertical.png", "Alinear Centros Verticales", "layeralign"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_BORDE_SUPERIOR,
                        "70403-align-borde-superior.png", "Alinear Bordes Superiores", "layeralign"),
                new SeparatorDefinition(),
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_BORDE_IZQUIERDO,
                        "70404-align-borde-izquierdo.png", "Alinear Bordes Izquierdos", "layeralign"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_CENTRO_HORIZONTAL,
                        "70405-align-centro-horizontal.png", "Alinear Centros Horizontales", "layeralign"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_BORDE_DERECHO,
                        "70406-align-borde-derecho.png", "Alinear Bordes Derechos", "layeralign"));
    } // --- Fin del metodo getComponentesLayerAlign ---

    public List<ToolbarComponentDefinition> getComponentesLayerDistribute() {
        return List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_BOTTOM_BORDER,
                        "70501-distribute-bottom-border.png", "Distribuir Bordes Superiores", "layerDistribute"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_CENTER_VERTICAL,
                        "70502-distribute-center-vertical.png", "Distribuir Centros Verticales", "layerDistribute"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_TOP_BORDER,
                        "70503-distribute-top-border.png", "Distribuir Bordes Inferiores", "layerDistribute"),
                new SeparatorDefinition(),
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_LEFT_BORDER,
                        "70504-distribute-left-border.png", "Distribuir Bordes Izquierdos", "layerDistribute"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_CENTER_HORIZONTAL,
                        "70505-distribute-center-horizontal.png", "Distribuir Centros Horizontales", "layerDistribute"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_RIGHT_BORDER,
                        "70506-distribute-right-border.png", "Distribuir Bordes Derechos", "layerDistribute"));
    } // --- Fin del metodo getComponentesLayerDistribute ---

    public List<ToolbarComponentDefinition> getComponentesLayerDistributeSpace() {
        return List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_HORIZONTAL_SPACE,
                        "70601-distribute-horizontal-space.png", "Distribuye Espacios Horizontales", "layerdistributespace"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_VERTICAL_SPACE,
                        "70602-distribute-vertical-space.png", "Distribuye Espacios Verticales", "layerdistributespace"));
    } // --- Fin del metodo getComponentesLayerDistributeSpace ---

    public List<ToolbarComponentDefinition> getComponentesLayerAutoDistribute() {
        return List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_AUTO_DISTRIBUTE_FIXED,
                        "70701-auto-distribute-fixed.png", "Distribucion Fija por Posicion", "layerautodistribute"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_AUTO_DISTRIBUTE_LAYER,
                        "70702-auto-distribute-layer.png", "Distribucion por Tamaño de la Capa", "layerautodistribute"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_AUTO_DISTRIBUTE_CANVAS,
                        "70703-reducir-ajustar.png", "Escalado de las Capas para Ajustar", "layerautodistribute")
	);

    } // --- Fin del metodo getComponentesLayerAutoDistribute ---

    public List<ToolbarComponentDefinition> getComponentesLayerOrderPreviewRender() {
        return List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_BRING_TO_FRONT,
                        "70201-bring-to-front.png", "Traer al frente", "layerorderpreview"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_BRING_FORWARD,
                        "70202-bring-forward.png", "Subir un Nivel", "layerorderpreview"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_SEND_BACKWARD,
                        "70203-send-backward.png", "Bajar un Nivel", "layerorderpreview"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_SEND_TO_BACK,
                        "70204-send-to-back.png", "Enviar al Fondo", "layerorderpreview"));
    } // --- Fin del metodo getComponentesLayerOrderPreviewRender ---

    public List<ToolbarComponentDefinition> getComponentesLayerLoadPreviewRender() {
        return List.of(
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_CLEAN_AND_ADD,
                        "70301-clean-and-add-image.png", "Borra Preview y Muestra Imagen", "previewRender"),
                new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_ADD_IMAGE,
                        "70302-add-image-to-group.png", "Añade Capa Con Una Nueva Imagen", "previewRender"),
        
                new SeparatorDefinition(),
                new SeparatorDefinition(),
                
            new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_ADD_LAYER,
                    	"70304-nueva-capa.png", "Añade Capa Nueva", "previewRender"),
            new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_COMBINE_LAYER,
            			"70306-combine-layer.png", "Combina Capas", "previewRender"),
            new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_DUPLICATE_LAYER,
                    	"70305-duplicate-layer.png", "Duplicar Capa", "previewRender"),
            new ToolbarButtonDefinition(AppActionCommands.CMD_PREVIEW_RENDER_DELETE_LAYER,
                    	"70303-delete-layer.png","Borra Capa Actual", "previewRender"));
        
    } // --- Fin del metodo getComponentesLayerLoadPreviewRender ---

    public ToolbarDefinition getToolbarDefinition(String clave) {
        return generateModularToolbarStructure().stream()
                .filter(td -> td.claveBarra().equals(clave))
                .findFirst().orElse(null);
    } // --- Fin del metodo getToolbarDefinition ---

    private String extraerNombreClave(String comandoCanonico) {
        if (comandoCanonico == null)
            return "desconocido";

        // Elimina prefijos comunes como "cmd." o "toggle."
        String resultado = comandoCanonico.startsWith("cmd.") ? comandoCanonico.substring(4) : comandoCanonico;
        resultado = resultado.startsWith("toggle.") ? resultado.substring(7) : resultado;

        // Reemplaza puntos por guiones bajos para un nombre de clave válido
        resultado = resultado.replace('.', '_');

        return resultado;
    }

    /**
     * Devuelve una lista con las claves de todas las barras de herramientas
     * definidas.
     * 
     * @return Una lista de Strings (ej. ["navegacion", "edicion", "zoom", ...]).
     */
    public List<String> getToolbarKeys() {
        return generateModularToolbarStructure().stream()
                .map(ToolbarDefinition::claveBarra)
                .collect(Collectors.toList());
    }

    /**
     * Método público y estático para que cualquiera pueda obtener la lista de
     * temas.
     * 
     * @return La lista de definiciones de los temas principales.
     */
    public static List<TemaDefinicion> getTemasPrincipales() {
        return TEMAS_PRINCIPALES;
    } // --- Fin del método getTemasPrincipales ---

    // *************************************************************************************************************************************************
    // *************************************************************************************************************
    // CLASE RECORD DE DEFINICION DE TEMAS
    // *************************************************************************************************************************************************

    /**
     * Record simple para almacenar la definición de un tema para la UI.
     * Contiene el ID interno (para la lógica) y el nombre para mostrar (para el
     * menú).
     */
    public record TemaDefinicion(String id, String nombreDisplay) {
    }

    /**
     * Define la lista curada de los temas principales de la aplicación,
     * ordenados por claridad y con el tema personalizado al final.
     * Esta es la ÚNICA fuente de verdad sobre qué temas debe manejar la UI.
     */
    private static final List<TemaDefinicion> TEMAS_PRINCIPALES = List.of(
            // --- TEMAS CLAROS ---
            new TemaDefinicion("arc_light", "Arc"),
            new TemaDefinicion("arc_orange_light", "Arc Orange"),
            new TemaDefinicion("cyan_light", "Cyan Light"),
            new TemaDefinicion("github_light", "GitHub"),
            new TemaDefinicion("light_owl_light", "Light Owl"),
            new TemaDefinicion("material_lighter", "Material Lighter"),
            new TemaDefinicion("solarized_light", "Solarized Light"),

            // --- TEMAS OSCUROS ---
            new TemaDefinicion("arc_dark", "Arc Dark"),
            new TemaDefinicion("arc_dark_orange", "Arc Dark Orange"),
            new TemaDefinicion("carbon_dark", "Carbon"),
            new TemaDefinicion("cobalt_2_dark", "Cobalt 2"),
            new TemaDefinicion("dark_purple", "Dark Purple"),
            new TemaDefinicion("dracula_dark", "Dracula"),
            new TemaDefinicion("github_dark", "GitHub Dark"),
            new TemaDefinicion("gruvbox_dark_hard", "Gruvbox Dark Hard"),
            new TemaDefinicion("gruvbox_dark_medium", "Gruvbox Dark Medium"),
            new TemaDefinicion("gruvbox_dark_soft", "Gruvbox Dark Soft"),
            new TemaDefinicion("high_contrast", "High Contrast"),
            new TemaDefinicion("material_darker", "Material Darker"),
            new TemaDefinicion("material_deep_ocean", "Material Deep Ocean"),
            new TemaDefinicion("material_oceanic", "Material Oceanic"),
            new TemaDefinicion("monocai_dark", "Monocai"),
            new TemaDefinicion("monokai_pro_dark", "Monokai Pro"),
            new TemaDefinicion("nord_dark", "Nord"),
            new TemaDefinicion("one_dark", "One Dark"),
            new TemaDefinicion("solarized_dark", "Solarized Dark"),
            new TemaDefinicion("spacegray_dark", "Spacegray"),
            new TemaDefinicion("vuesion_dark", "Vuesion"),

            // Temas oscuros con degradados
            new TemaDefinicion("gradianto_dark_fuchsia", "Gradianto Dark Fuchsia"),
            new TemaDefinicion("gradianto_deep_ocean", "Gradianto Deep Ocean"),
            new TemaDefinicion("gradianto_midnight_blue", "Gradianto Midnight Blue"),
            new TemaDefinicion("gradianto_nature_green", "Gradianto Nature Green"),

            // --- TEMA PERSONALIZADO ---
            new TemaDefinicion("purpura_misterioso", "Púrpura Misterioso"),
            new TemaDefinicion("gradianto_azul_medianoche", "Azul Medianoche"),
            new TemaDefinicion("carbon_orange", "Carbon Orange"),
            new TemaDefinicion("obsidian_orange", "Obsidian Orange"));

    // --- Fin de la Definición de Temas ---

} // ---FIN de la clase UIDefinitionService



