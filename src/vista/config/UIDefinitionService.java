package vista.config;

import java.util.List;
import java.util.ArrayList; // Usaremos ArrayList para construir las listas internas

import controlador.commands.AppActionCommands; // ¡Importa tus comandos!

public class UIDefinitionService {

	/**
	 * 
	 * Usa MenuItemType.MAIN_MENU para los menús principales (- Archivo).
	 * Usa MenuItemType.SUB_MENU para los submenús (--< Edicion).
	 * Usa MenuItemType.ITEM para ítems normales (--- Guardar).
	 * Usa MenuItemType.CHECKBOX_ITEM para los que empiezan con ---*.
	 * Usa MenuItemType.RADIO_BUTTON_ITEM para los que empiezan con ---..
	 * Usa MenuItemType.SEPARATOR donde tenías _.
	 * Usa MenuItemType.RADIO_GROUP_START donde tenías --{.
	 * Usa MenuItemType.RADIO_GROUP_END donde tenías --}.
	 * En el primer argumento (comandoOClave), pon la constante AppActionCommands correspondiente si la acción es funcional. 
	 * Si es un elemento de configuración de UI (como los checkboxes de "Visualizar Botón X"), pon la clave larga del componente que controla. 
	 * Si no tiene acción (separador, menú contenedor), pon null.
	 * Presta atención a la anidación usando el cuarto argumento subItems.
	 * 
	 * @return
	 */
    public List<MenuItemDefinition> generateMenuStructure() {
        List<MenuItemDefinition> menuBarStructure = new ArrayList<>();

        // --- Menú Archivo ---
        List<MenuItemDefinition> archivoSubItems = List.of(
            new MenuItemDefinition(AppActionCommands.CMD_ARCHIVO_ABRIR, MenuItemType.ITEM, "Abrir Archivo", null),
            new MenuItemDefinition(AppActionCommands.CMD_ARCHIVO_ABRIR_NUEVA_VENTANA, MenuItemType.ITEM, "Abrir en ventana nueva", null),
            new MenuItemDefinition(AppActionCommands.CMD_ARCHIVO_GUARDAR, MenuItemType.ITEM, "Guardar", null),
            new MenuItemDefinition(AppActionCommands.CMD_ARCHIVO_GUARDAR_COMO, MenuItemType.ITEM, "Guardar Como", null),
            new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
            // TODO: Añadir comando para "Abrir Con..." si tienes Action
            new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, MenuItemType.ITEM, "Abrir Con...", null), // Comando null por ahora
            // TODO: Añadir comando para "Editar Imagen" si tienes Action
            new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, MenuItemType.ITEM, "Editar Imagen", null), // Comando null por ahora
            new MenuItemDefinition(AppActionCommands.CMD_ARCHIVO_IMPRIMIR, MenuItemType.ITEM, "Imprimir", null),
            // TODO: Añadir comando para "Compartir" si tienes Action
            new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, MenuItemType.ITEM, "Compartir", null), // Comando null por ahora
            new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_ESPECIAL_REFRESCAR, MenuItemType.ITEM, "Refrescar Imagen", null), // Reutiliza Refrescar?
            new MenuItemDefinition(AppActionCommands.CMD_ESPECIAL_REFRESCAR, MenuItemType.ITEM, "Volver a Cargar", null), // O necesita otro comando?
            // TODO: Añadir comando para "Recargar Lista" si tienes Action
            new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, MenuItemType.ITEM, "Recargar Lista de Imagenes", null), // Comando null por ahora
            // TODO: Añadir comando para "Unload Imagen" si tienes Action
            new MenuItemDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, MenuItemType.ITEM, "Unload Imagen", null) // Comando null por ahora
        );
        menuBarStructure.add(new MenuItemDefinition(null, MenuItemType.MAIN_MENU, "Archivo", archivoSubItems));

        // --- Menú Navegación ---
        List<MenuItemDefinition> navSubItems = List.of(
            new MenuItemDefinition(AppActionCommands.CMD_NAV_PRIMERA, MenuItemType.ITEM, "Primera Imagen", null),
            new MenuItemDefinition(AppActionCommands.CMD_NAV_ANTERIOR, MenuItemType.ITEM, "Imagen Anterior", null), // Ojo: Aterior -> Anterior
            new MenuItemDefinition(AppActionCommands.CMD_NAV_SIGUIENTE, MenuItemType.ITEM, "Imagen Siguiente", null),
            new MenuItemDefinition(AppActionCommands.CMD_NAV_ULTIMA, MenuItemType.ITEM, "Ultima Imagen", null),
            new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_NAV_IR_A, MenuItemType.ITEM, "Ir a...", null),
            new MenuItemDefinition(AppActionCommands.CMD_NAV_BUSCAR, MenuItemType.ITEM, "Buscar...", null),
            new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_NAV_FOTOGRAMA_ANTERIOR, MenuItemType.ITEM, "Anterior Fotograma", null),
            new MenuItemDefinition(AppActionCommands.CMD_NAV_FOTOGRAMA_SIGUIENTE, MenuItemType.ITEM, "Siguiente Fotograma", null),
            new MenuItemDefinition(AppActionCommands.CMD_NAV_FOTOGRAMA_PRIMERO, MenuItemType.ITEM, "Primer Fotograma", null),
            new MenuItemDefinition(AppActionCommands.CMD_NAV_FOTOGRAMA_ULTIMO, MenuItemType.ITEM, "Ultimo Fotograma", null)
        );
        menuBarStructure.add(new MenuItemDefinition(null, MenuItemType.MAIN_MENU, "Navegacion", navSubItems));

        // --- Menú Zoom ---
        // Submenú Tipos de Zoom
        List<MenuItemDefinition> tiposZoomSubItems = List.of(
            new MenuItemDefinition(null, MenuItemType.RADIO_GROUP_START, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR, MenuItemType.RADIO_BUTTON_ITEM, "Escalar Para Ajustar", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_TIPO_AUTO, MenuItemType.RADIO_BUTTON_ITEM, "Zoom Automatico", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_TIPO_ANCHO, MenuItemType.RADIO_BUTTON_ITEM, "Zoom a lo Ancho", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_TIPO_ALTO, MenuItemType.RADIO_BUTTON_ITEM, "Zoom a lo Alto", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_TIPO_FIJO, MenuItemType.RADIO_BUTTON_ITEM, "Zoom Actual Fijo", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO, MenuItemType.RADIO_BUTTON_ITEM, "Zoom Especificado", null),
            //new MenuItemDefinition(AppActionCommands.CMD_ZOOM_TIPO_RELLENAR, MenuItemType.RADIO_BUTTON_ITEM, "Escalar Para Rellenar", null),
            new MenuItemDefinition(null, MenuItemType.RADIO_GROUP_END, null, null)
        );
        
        // Menú Zoom principal
        List<MenuItemDefinition> zoomSubItems = List.of(
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_ACERCAR, MenuItemType.ITEM, "Acercar", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_ALEJAR, MenuItemType.ITEM, "Alejar", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_PERSONALIZADO, MenuItemType.ITEM, "Zoom Personalizado %", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_TAMAÑO_REAL, MenuItemType.ITEM, "Zoom Tamaño Real", null),
            new MenuItemDefinition(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES, MenuItemType.CHECKBOX_ITEM, "Mantener Proporciones", null),
            new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE, MenuItemType.CHECKBOX_ITEM, "Activar Zoom Manual", null),
            new MenuItemDefinition(AppActionCommands.CMD_ZOOM_RESET, MenuItemType.ITEM, "Resetear Zoom", null),
            new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(null, MenuItemType.SUB_MENU, "Tipos de Zoom", tiposZoomSubItems) // Submenú anidado
        );
        menuBarStructure.add(new MenuItemDefinition(null, MenuItemType.MAIN_MENU, "Zoom", zoomSubItems));

        // --- Menú Imagen ---
        // Carga y Orden - Criterio
        List<MenuItemDefinition> ordenCriterioSubItems = List.of(
            new MenuItemDefinition(null, MenuItemType.RADIO_GROUP_START, null, null),
            // TODO: Definir comandos para estos radios (si no usan Action directa)
            new MenuItemDefinition("cmd.orden.criterio.nombre", MenuItemType.RADIO_BUTTON_ITEM, "Nombre por Defecto", null),
            new MenuItemDefinition("cmd.orden.criterio.tamano", MenuItemType.RADIO_BUTTON_ITEM, "Tamaño de Archivo", null),
            new MenuItemDefinition("cmd.orden.criterio.fecha", MenuItemType.RADIO_BUTTON_ITEM, "Fecha de Creacion", null),
            new MenuItemDefinition("cmd.orden.criterio.extension", MenuItemType.RADIO_BUTTON_ITEM, "Extension", null),
            new MenuItemDefinition(null, MenuItemType.RADIO_GROUP_END, null, null)
        );
         // Carga y Orden - Dirección
         List<MenuItemDefinition> ordenDireccionSubItems = List.of(
             new MenuItemDefinition(null, MenuItemType.RADIO_GROUP_START, null, null),
             // TODO: Definir comandos para estos radios
             new MenuItemDefinition("cmd.orden.direccion.ninguno", MenuItemType.RADIO_BUTTON_ITEM, "Sin Ordenar", null), // ¿Necesario si ya hay criterio?
             new MenuItemDefinition("cmd.orden.direccion.asc", MenuItemType.RADIO_BUTTON_ITEM, "Ascendente", null),
             new MenuItemDefinition("cmd.orden.direccion.desc", MenuItemType.RADIO_BUTTON_ITEM, "Descendente", null),
             new MenuItemDefinition(null, MenuItemType.RADIO_GROUP_END, null, null)
         );
         // Carga y Orden - Principal
         List<MenuItemDefinition> cargaOrdenSubItems = new ArrayList<>(); // Usamos ArrayList para añadir
         cargaOrdenSubItems.addAll(ordenCriterioSubItems);
         cargaOrdenSubItems.add(new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null));
         cargaOrdenSubItems.addAll(ordenDireccionSubItems);

         // Edición
         List<MenuItemDefinition> edicionSubItems = List.of(
             new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_ROTAR_IZQ, MenuItemType.ITEM, "Girar Izquierda", null),
             new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_ROTAR_DER, MenuItemType.ITEM, "Girar Derecha", null),
             new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_VOLTEAR_H, MenuItemType.ITEM, "Voltear Horizontal", null),
             new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_VOLTEAR_V, MenuItemType.ITEM, "Voltear Vertical", null)
         );
         // Menú Imagen principal
         List<MenuItemDefinition> imagenSubItems = List.of(
             new MenuItemDefinition(null, MenuItemType.SUB_MENU, "Carga y Orden", cargaOrdenSubItems),
             new MenuItemDefinition(null, MenuItemType.SUB_MENU, "Edicion", edicionSubItems),
             new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
             new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_RENOMBRAR, MenuItemType.ITEM, "Cambiar Nombre de la Imagen", null),
             new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_MOVER_PAPELERA, MenuItemType.ITEM, "Mover a la Papelera", null),
             new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_ELIMINAR, MenuItemType.ITEM, "Eliminar Permanentemente", null),
             new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
             new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_FONDO_ESCRITORIO, MenuItemType.ITEM, "Establecer Como Fondo de Escritorio", null),
             new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_FONDO_BLOQUEO, MenuItemType.ITEM, "Establecer Como Imagen de Bloqueo", null),
             new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
             new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_LOCALIZAR, MenuItemType.ITEM, "Abrir Ubicacion del Archivo", null),
             new MenuItemDefinition(AppActionCommands.CMD_IMAGEN_PROPIEDADES, MenuItemType.ITEM, "Propiedades de la imagen", null)
         );
        menuBarStructure.add(new MenuItemDefinition(null, MenuItemType.MAIN_MENU, "Imagen", imagenSubItems));

        // --- Menú Vista ---
         List<MenuItemDefinition> vistaSubItems = List.of(
             new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_MENU_BAR, MenuItemType.CHECKBOX_ITEM, "Barra de Menu", null),
             new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_TOOL_BAR, MenuItemType.CHECKBOX_ITEM, "Barra de Botones", null),
             new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_FILE_LIST, MenuItemType.CHECKBOX_ITEM, "Mostrar/Ocultar la Lista de Archivos", null),
             new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_THUMBNAILS, MenuItemType.CHECKBOX_ITEM, "Imagenes en Miniatura", null),
             new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_LOCATION_BAR, MenuItemType.CHECKBOX_ITEM, "Linea de Ubicacion del Archivo", null),
             new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
             new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_CHECKERED_BG, MenuItemType.CHECKBOX_ITEM, "Fondo a Cuadros", null),
             new MenuItemDefinition(AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP, MenuItemType.CHECKBOX_ITEM, "Mantener Ventana Siempre Encima", null),
             new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null), // Separador si lo ves necesario 
             new MenuItemDefinition(AppActionCommands.	CMD_VISTA_TOGGLE_MINIATURE_TEXT, MenuItemType.CHECKBOX_ITEM, "Mostrar Nombres en Miniaturas", null),
             new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
             new MenuItemDefinition(AppActionCommands.CMD_VISTA_MOSTRAR_DIALOGO_LISTA, MenuItemType.ITEM, "Mostrar Dialogo Lista de Imagenes", null)
             

        );
        menuBarStructure.add(new MenuItemDefinition(null, MenuItemType.MAIN_MENU, "Vista", vistaSubItems));

        // --- Nuevo Menú Proyecto ---
        List<MenuItemDefinition> proyectoSubItems = List.of(
            new MenuItemDefinition(AppActionCommands.CMD_PROYECTO_GESTIONAR, MenuItemType.ITEM, "Gestionar Selección Actual...", null),
            new MenuItemDefinition(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA, MenuItemType.CHECKBOX_ITEM, "Marcar para Proyecto", null)
            // Aquí irían "Nuevo Proyecto", "Abrir Proyecto", "Guardar Como..." en Iteración 2
        );
        menuBarStructure.add(new MenuItemDefinition(null, MenuItemType.MAIN_MENU, "Proyecto", proyectoSubItems));
        
        // --- Menú Configuración ---
        // Carga de Imagenes
        List<MenuItemDefinition> cargaImgSubItems = List.of(
            new MenuItemDefinition(null, MenuItemType.RADIO_GROUP_START, null, null),
            // TODO: Decidir si usar Actions o comandos fallback para estos radios
//            new MenuItemDefinition("cmd.config.carga.solo_carpeta", MenuItemType.RADIO_BUTTON_ITEM, "Mostrar Solo Carpeta Actual", null),
//            new MenuItemDefinition("cmd.config.carga.subcarpetas", MenuItemType.RADIO_BUTTON_ITEM, "Mostrar Imagenes de Subcarpetas", null),
            new MenuItemDefinition(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA, MenuItemType.RADIO_BUTTON_ITEM, "Mostrar Solo Carpeta Actual", null),
            new MenuItemDefinition(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS, MenuItemType.RADIO_BUTTON_ITEM, "Mostrar Imagenes de Subcarpetas", null),
            new MenuItemDefinition(null, MenuItemType.RADIO_GROUP_END, null, null),
            new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
            // TODO: Añadir comando o Action para Miniaturas en Barra
            new MenuItemDefinition(null, MenuItemType.ITEM, "Miniaturas en la Barra de Imagenes", null) // ¿Debería ser Checkbox?
        );
        // General
        List<MenuItemDefinition> generalSubItems = List.of(
            // TODO: Añadir comandos o Actions
            new MenuItemDefinition(null, MenuItemType.CHECKBOX_ITEM, "Mostrar Imagen de Bienvenida", null),
            new MenuItemDefinition(null, MenuItemType.CHECKBOX_ITEM, "Abrir Ultima Imagen Vista", null),
            new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_TOGGLE_WRAP_AROUND, MenuItemType.CHECKBOX_ITEM, "Volver a la Primera Imagen al Llegar al final de la Lista", null),
            // TODO: Añadir comando o Action para mostrar flechas
            new MenuItemDefinition(null, MenuItemType.CHECKBOX_ITEM, "Mostrar Flechas de Navegacion", null)
        );
        // Visualizar Botones (¡Usando clave de botón como comando!)
        List<MenuItemDefinition> visBtnSubItems = List.of(
                // Edición
                new MenuItemDefinition("interfaz.boton.edicion.Rotar_Izquierda_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Rotar Izquierda", null),
                new MenuItemDefinition("interfaz.boton.edicion.Rotar_Derecha_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Rotar Derecha", null),
                new MenuItemDefinition("interfaz.boton.edicion.Espejo_Horizontal_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Espejo Horizontal", null),
                new MenuItemDefinition("interfaz.boton.edicion.Espejo_Vertical_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Espejo Vertical", null),
                new MenuItemDefinition("interfaz.boton.edicion.Recortar_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Recortar", null),
                new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),

                // Zoom
                new MenuItemDefinition("interfaz.boton.zoom.Zoom_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Zoom", null),
                new MenuItemDefinition("interfaz.boton.zoom.Zoom_Auto_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Zoom Automatico", null),
                new MenuItemDefinition("interfaz.boton.zoom.Ajustar_al_Ancho_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Ajustar al Ancho", null),
                new MenuItemDefinition("interfaz.boton.zoom.Ajustar_al_Alto_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Ajustar al Alto", null),
                new MenuItemDefinition("interfaz.boton.zoom.Escalar_Para_Ajustar_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Escalar para Ajustar", null),
                new MenuItemDefinition("interfaz.boton.zoom.Zoom_Fijo_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Zoom Fijo", null),
                new MenuItemDefinition("interfaz.boton.zoom.Reset_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Reset Zoom", null), // Nota: tu config tenía "botn_reset_zoom", asegúrate que el texto sea "Botón Reset Zoom" para generar "botón_reset_zoom" o ajusta.
                new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),

                // Vista
                new MenuItemDefinition("interfaz.boton.vista.Panel-Galeria_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Panel-Galeria", null),
                new MenuItemDefinition("interfaz.boton.vista.Grid_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Grid", null),
                new MenuItemDefinition("interfaz.boton.vista.Pantalla_Completa_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Pantalla Completa", null),
                new MenuItemDefinition("interfaz.boton.vista.Lista_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Lista", null),
                new MenuItemDefinition("interfaz.boton.vista.Carrousel_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Carrousel", null),
                new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),

                // Control y Toggles (algunos de estos podrían ser los mismos botones)
                new MenuItemDefinition("interfaz.boton.control.Refrescar_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Refrescar", null),
                new MenuItemDefinition("interfaz.boton.toggle.Subcarpetas_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Subcarpetas", null), // Tu config tenía "botn_subcarpetas"
                new MenuItemDefinition("interfaz.boton.control.lista_de_favoritos_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Lista de Favoritos", null),
                new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),

                // Especiales
                new MenuItemDefinition("interfaz.boton.control.Borrar_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Borrar", null), // El botón de borrar de la toolbar
                new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
                new MenuItemDefinition("interfaz.boton.especiales.Menu_48x48", MenuItemType.CHECKBOX_ITEM, "Botón Menu", null),
                new MenuItemDefinition("interfaz.boton.especiales.Botones_Ocultos_48x48", MenuItemType.CHECKBOX_ITEM, "Mostrar Boton de Botones Ocultos", null)
            );
        // Barra de Información
        List<MenuItemDefinition> barraInfoSubItems = List.of(
            new MenuItemDefinition(null, MenuItemType.RADIO_GROUP_START, null, null),
             // TODO: Añadir comandos o Actions
            new MenuItemDefinition(null, MenuItemType.RADIO_BUTTON_ITEM, "Nombre del Archivo", null),
            new MenuItemDefinition(null, MenuItemType.RADIO_BUTTON_ITEM, "Ruta y Nombre del Archivo", null),
            new MenuItemDefinition(null, MenuItemType.RADIO_GROUP_END, null, null),
            new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(null, MenuItemType.CHECKBOX_ITEM, "Numero de Imagenes en la Carpeta Actual", null),
            new MenuItemDefinition(null, MenuItemType.CHECKBOX_ITEM, "% de Zoom actual", null),
            new MenuItemDefinition(null, MenuItemType.CHECKBOX_ITEM, "Tamaño del Archivo", null),
            new MenuItemDefinition(null, MenuItemType.CHECKBOX_ITEM, "Fecha y Hora de la Imagen", null)
        );
        // Tema
        List<MenuItemDefinition> temaSubItems = List.of(
            new MenuItemDefinition(null, MenuItemType.RADIO_GROUP_START, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_TEMA_CLEAR, MenuItemType.RADIO_BUTTON_ITEM, "Tema Clear", null),
            new MenuItemDefinition(AppActionCommands.CMD_TEMA_DARK, MenuItemType.RADIO_BUTTON_ITEM, "Tema Dark", null),
            new MenuItemDefinition(AppActionCommands.CMD_TEMA_BLUE, MenuItemType.RADIO_BUTTON_ITEM, "Tema Blue", null),
            new MenuItemDefinition(AppActionCommands.CMD_TEMA_ORANGE, MenuItemType.RADIO_BUTTON_ITEM, "Tema Orange", null),
            new MenuItemDefinition(AppActionCommands.CMD_TEMA_GREEN, MenuItemType.RADIO_BUTTON_ITEM, "Tema Green", null),
            new MenuItemDefinition(null, MenuItemType.RADIO_GROUP_END, null, null)
        );
        // Menú Configuración principal
        List<MenuItemDefinition> configSubItems = List.of(
            new MenuItemDefinition(null, MenuItemType.SUB_MENU, "Carga de Imagenes", cargaImgSubItems),
            new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(null, MenuItemType.SUB_MENU, "General", generalSubItems),
            new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(null, MenuItemType.SUB_MENU, "Visualizar Botones", visBtnSubItems),
            new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(null, MenuItemType.SUB_MENU, "Barra de Informacion", barraInfoSubItems), // Corregido nombre var
            new MenuItemDefinition(null, MenuItemType.SUB_MENU, "Tema", temaSubItems),
            new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_CONFIG_GUARDAR, MenuItemType.ITEM, "Guardar Configuracion Actual", null),
            new MenuItemDefinition(AppActionCommands.CMD_CONFIG_CARGAR_INICIAL, MenuItemType.ITEM, "Cargar Configuracion Inicial", null),
            new MenuItemDefinition(null, MenuItemType.SEPARATOR, null, null),
            new MenuItemDefinition(AppActionCommands.CMD_CONFIG_MOSTRAR_VERSION, MenuItemType.ITEM, "Version", null)
        );
        menuBarStructure.add(new MenuItemDefinition(null, MenuItemType.MAIN_MENU, "Configuracion", configSubItems));


        
        // --- FIN de Definición de Menús ---
        return menuBarStructure;
    }

    // --- Método para la Toolbar ---
    public List<ToolbarButtonDefinition> generateToolbarStructure() {
        List<ToolbarButtonDefinition> toolbarStructure = new ArrayList<>();

        // Grupo Movimiento
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_PRIMERA, "1001-Primera_48x48.png", "Primera Imagen", "movimiento"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_ANTERIOR, "1002-Anterior_48x48.png", "Imagen Anterior", "movimiento"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_SIGUIENTE, "1003-Siguiente_48x48.png", "Imagen Siguiente", "movimiento"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_NAV_ULTIMA, "1004-Ultima_48x48.png", "Última Imagen", "movimiento"));

        // Grupo Edición
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_ROTAR_IZQ, "2001-Rotar_Izquierda_48x48.png", "Rotar Izquierda", "edicion"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_ROTAR_DER, "2002-Rotar_Derecha_48x48.png", "Rotar Derecha", "edicion"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_VOLTEAR_H, "2003-Espejo_Horizontal_48x48.png", "Voltear Horizontal", "edicion"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_VOLTEAR_V, "2004-Espejo_Vertical_48x48.png", "Voltear Vertical", "edicion"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_RECORTAR, "2005-Recortar_48x48.png", "Recortar", "edicion"));

        // Grupo Zoom
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR, "3005-Escalar_Para_Ajustar_48x48.png", "Escalar para Ajustar", "zoom"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_AUTO, "3002-Zoom_Auto_48x48.png", "Zoom Automático", "zoom"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_ANCHO, "3003-Ajustar_al_Ancho_48x48.png", "Ajustar al Ancho", "zoom"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_ALTO, "3004-Ajustar_al_Alto_48x48.png", "Ajustar al Alto", "zoom"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_FIJO, "3006-Zoom_Fijo_48x48.png", "Zoom Actual Fijo", "zoom"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO, "3007-zoom_especifico_48x48.png", "Zoom Especificado", "zoom")); // Ojo: nombre icono
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE, "3001-Zoom_48x48.png", "Activar/Desactivar Zoom Manual", "zoom"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_ZOOM_RESET, "3008-Reset_48x48.png", "Resetear Zoom", "zoom"));

        // Grupo Vista
        // TODO: Añadir comandos para estos botones si tienen Actions
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, "4001-Panel-Galeria_48x48.png", "Panel Galería", "vista"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, "4002-Grid_48x48.png", "Vista Grid", "vista"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, "4003-Pantalla_Completa_48x48.png", "Pantalla Completa", "vista"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_VISTA_MOSTRAR_DIALOGO_LISTA, "4004-Lista_48x48.png", "Vista Lista", "vista"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, "4005-Carrousel_48x48.png", "Vista Carrusel", "vista"));

        // Grupo Control
        
        // TODO: Añadir comando para Favoritos
//        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_FAVORITO_MOSTRAR_LISTA, "5003-marcar_imagen_48x48.png", "Marcar Imagen para Favoritos", "control"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_ELIMINAR, "5004-Borrar_48x48.png", "Eliminar Imagen", "control"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_ESPECIAL_REFRESCAR, "5001-Refrescar_48x48.png", "Refrescar", "control"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_IMAGEN_LOCALIZAR, "7004-Ubicacion_de_Archivo_48x48.png", "Abrir Ubicación", "control")); // Reubicado?

        // Grupo Especiales
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_ARCHIVO_ABRIR, "6001-Selector_de_Carpetas_48x48.png", "Abrir Carpeta", "especiales"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_ESPECIAL_MENU, "6002-Menu_48x48.png", "Menú Principal", "especiales"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_ESPECIAL_BOTONES_OCULTOS, "6003-Botones_Ocultos_48x48.png", "Mostrar Botones Ocultos", "especiales"));

        // Grupo Proyecto
        // TODO: Añadir comando para Mostrar Favoritos (toggle)
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA, "5003-marcar_imagen_48x48.png", "Marcar Imagen para Proyecto", "proyecto"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_GESTIONAR, "7003-Mostrar_Favoritos_48x48.png", "Mostrar/Ocultar Favoritos", "proyecto"));
//        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE, "7003-Mostrar_Favoritos_48x48.png", "Mostrar/Ocultar Favoritos", "toggle"));

        // Grupo Toggle
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_TOGGLE_SUBCARPETAS, "7001-Subcarpetas_48x48.png", "Incluir/Excluir Subcarpetas", "toggle"));
        toolbarStructure.add(new ToolbarButtonDefinition(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES, "7002-Mantener_Proporciones_48x48.png", "Mantener Proporciones", "toggle"));

        
        
        
        return toolbarStructure;
    }
} // ---FIN de la clase UIDefinitionService