package servicios;

/**
 * Contiene todas las claves de configuración (strings) usadas en la aplicación.
 * Esta clase no debe ser instanciada. Sirve como un diccionario centralizado
 * para evitar errores de tipeo y facilitar la refactorización.
 */
public final class ConfigKeys {

    // Constructor privado para evitar que alguien cree una instancia de esta clase de utilidad.
    private ConfigKeys() {}
    
    
 // --- PREFIJOS BASE ---
    private static final String MENU 			= "interfaz.menu";
    private static final String TOOLBAR 		= "interfaz.herramientas";
    private static final String BUTTON_PREFIX	= "interfaz.boton";
    private static final String INFOBAR 		= "interfaz.infobar";
    private static final String MINIATURES 		= "interfaz.miniaturas";
//    private static final String 		= "";
    
    // ... puedes añadir más prefijos aquí si los necesitas

    // --- GENERADORES DINÁMICOS ---
    public static String menu(String... parts) {return buildKey(MENU, parts);}
    public static String menuState(String... parts) {return buildKey(MENU, parts) + ".seleccionado";}
    public static String toolbarVisible(String toolbarKey) {return buildKey(TOOLBAR, normalizePart(toolbarKey)) + ".visible";}
    public static String buttonVisible(String toolbarKey, String buttonKey) { return buildKey("interfaz.boton", toolbarKey, buttonKey, "visible");}
    public static String toolbarButtonVisible(String toolbarKey, String buttonKey) {
        return buildKey(BUTTON_PREFIX, normalizePart(toolbarKey), normalizePart(buttonKey)) + ".visible";
    }
    
    
    // --- CONSTANTES PARA CLAVES ÚNICAS (las que no se pueden generar) ---
	    public static final String INICIO_CARPETA = "inicio.carpeta";
	    public static final String INICIO_IMAGEN = "inicio.imagen";
    
    public static final String VISTA_MOSTRAR_NOMBRES_MINIATURAS_STATE = menuState("vista", "mostrar_nombres_en_miniaturas");
    // ... aquí irían el resto de tus constantes de ConfigKeys que no siguen un patrón generable

    // --- MÉTODOS AYUDANTES ---

    public static String buildKey(String prefix, String... parts) {
        StringBuilder sb = new StringBuilder(prefix);
        for (String part : parts) {
            sb.append(".").append(part);
        }
        return sb.toString();
    }
    public static String normalizePart(String text) {
        // 1. Manejar caso de entrada nula o vacía.
        if (text == null || text.isBlank()) {
            // System.out.println("  [generateKeyPart] Texto nulo/vacío, devolviendo 'unknown_key_part'"); // Log opcional
            return "unknown_key_part"; // Devolver un placeholder identificable.
        }
        
        String normalizedText = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        normalizedText = normalizedText.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        
        String cleanedText = normalizedText.toLowerCase();

        // 2. Limpiar y normalizar la cadena:
        cleanedText = text.trim()                // Quitar espacios al inicio y al final.
                .replace(" ", "_")       // Reemplazar espacios internos por guiones bajos.
                .replace("/", "_")       // Reemplazar barras por guiones bajos.
                .replace("\\", "_")      // Reemplazar contrabarras.
                .replace("*", "")        // Eliminar asteriscos (usados para indicar tipo en definición antigua).
                .replace(".", "")        // Eliminar puntos (para evitar confusión en claves jerárquicas).
                .replace("%", "porc")    // Reemplazar '%' por "porc" para evitar problemas.
                .replace(":", "")        // Eliminar dos puntos.
                .replace("?", "")        // Eliminar signos de interrogación.
                .replace("á", "a")		// quita la tilde de la á
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                // Eliminar cualquier carácter que NO sea letra (a-z, A-Z), número (0-9) o guion bajo (_).
                // Esto asegura que la clave solo contenga caracteres seguros.
                .replaceAll("[^a-zA-Z0-9_]", "");

        // 3. Convertir la cadena resultante a minúsculas para consistencia.
        cleanedText = cleanedText.toLowerCase();

        // 4. Asegurar que la clave no quede vacía después de la limpieza.
        //    Si todos los caracteres fueron eliminados, generar una clave fallback.
        if (cleanedText.isEmpty()) {
            // Generar algo basado en el hash del texto original (poco legible pero único).
            // System.out.println("  [generateKeyPart] Texto original '" + text + "' resultó en clave vacía. Usando hash."); // Log opcional
            return "emptykey_" + Math.abs(text.hashCode()); // Usar Math.abs para evitar signo negativo.
        }

        return cleanedText;
    } // --- FIN del método generateKeyPart ---
    
    
    // --- SECCIÓN: ESTADO DE LA VENTANA ---
    public static final String WINDOW_X = "window.x";
    public static final String WINDOW_Y = "window.y";
    public static final String WINDOW_WIDTH = "window.width";
    public static final String WINDOW_HEIGHT = "window.height";
    public static final String WINDOW_MAXIMIZED = "window.maximized";

    // --- SECCIÓN: INICIO Y PROYECTOS ---
    public static final String PROYECTOS_CARPETA_BASE = "proyectos.carpeta_base";
    public static final String PROYECTOS_ARCHIVO_TEMPORAL = "proyectos.archivo_temporal_nombre";
    
    public static final String PROYECTOS_LISTA_ACTIVA = "proyectos.estado.lista_activa";
    public static final String PROYECTOS_ULTIMA_SELECCION_KEY = "proyectos.estado.ultima_seleccion_key";
    public static final String PROYECTOS_ULTIMA_DESCARTES_KEY = "proyectos.estado.ultima_descartes_key";
    
    // Para el futuro:
    // public static final String KEY_PROYECTOS_ULTIMO_ABIERTO = "proyectos.ultimo_abierto_ruta_completa";
    // public static final String KEY_PROYECTOS_INICIO_ACCION = "proyectos.inicio.accion"; // ej. "cargar_ultimo", "nuevo_temporal"
    
    // --- SECCIÓN: TEMA ---
    public static final String TEMA_NOMBRE = "tema.nombre";

    // --- SECCIÓN: COMPORTAMIENTO ---
    public static final String COMPORTAMIENTO_ZOOM_MODO_INICIAL = "comportamiento.display.zoom.initial_mode";
    public static final String COMPORTAMIENTO_ZOOM_MANUAL_INICIAL = "comportamiento.zoom.manual_inicial_activo";
    public static final String COMPORTAMIENTO_ZOOM_ULTIMO_MODO = "comportamiento.zoom.ultimo_modo_seleccionado";
    public static final String COMPORTAMIENTO_ZOOM_PORCENTAJE_PERSONALIZADO = "comportamiento.zoom.personalizado.porcentaje";
    public static final String COMPORTAMIENTO_NAVEGACION_CIRCULAR = "comportamiento.navegacion.circular";
    public static final String COMPORTAMIENTO_CARGAR_SUBCARPETAS = "comportamiento.carpeta.cargarSubcarpetas";
    public static final String COMPORTAMIENTO_ZOOM_AL_CURSOR_ACTIVADO = "comportamiento.zoom.al_cursor.activado";
    
 // --- SECCIÓN: BARRAS DE INFORMACIÓN (SUPERIOR) ---
    public static final String INFOBAR_SUP_VISIBLE 				= "interfaz.infobar.superior.visible";
    
    // -- Contenido Barra Superior
    public static final String INFOBAR_SUP_NOMBRE_RUTA_VISIBLE = "interfaz.infobar.superior.nombre_ruta.visible";
    public static final String INFOBAR_SUP_NOMBRE_RUTA_FORMATO = "interfaz.infobar.superior.nombre_ruta.formato"; // Valores: "solo_nombre", "ruta_completa"
    public static final String INFOBAR_SUP_INDICE_TOTAL_VISIBLE = "interfaz.infobar.superior.indice_total.visible";
    public static final String INFOBAR_SUP_DIMENSIONES_VISIBLE = "interfaz.infobar.superior.dimensiones.visible";
    public static final String INFOBAR_SUP_TAMANO_ARCHIVO_VISIBLE = "interfaz.infobar.superior.tamano_archivo.visible";
    public static final String INFOBAR_SUP_FECHA_ARCHIVO_VISIBLE = "interfaz.infobar.superior.fecha_archivo.visible";
    public static final String INFOBAR_SUP_FORMATO_IMAGEN_VISIBLE = "interfaz.infobar.superior.formato_imagen.visible";
    public static final String INFOBAR_SUP_MODO_ZOOM_VISIBLE = "interfaz.infobar.superior.modo_zoom.visible";
    public static final String INFOBAR_SUP_ZOOM_REAL_PCT_VISIBLE = "interfaz.infobar.superior.zoom_real_pct.visible";
    // ... (añade aquí el resto de claves de infobar superior)


 // --- SECCIÓN: BARRAS DE INFORMACIÓN (INFERIOR) ---
    public static final String INFOBAR_INF_VISIBLE = "interfaz.infobar.inferior.visible";
    
    // -- Elementos Barra Inferior --
    public static final String INFOBAR_INF_NOMBRE_RUTA_VISIBLE = "interfaz.infobar.inferior.nombre_ruta.visible";
    public static final String INFOBAR_INF_NOMBRE_RUTA_FORMATO = "interfaz.infobar.inferior.nombre_ruta.formato"; // Valores: "solo_nombre", "ruta_completa"
    public static final String INFOBAR_INF_ICONO_ZM_VISIBLE = "interfaz.infobar.inferior.icono_zm.visible";
    public static final String INFOBAR_INF_ICONO_PROP_VISIBLE = "interfaz.infobar.inferior.icono_prop.visible";
    public static final String INFOBAR_INF_ICONO_SUBC_VISIBLE = "interfaz.infobar.inferior.icono_subc.visible";
    public static final String INFOBAR_INF_CTRL_ZOOM_PCT_VISIBLE = "interfaz.infobar.inferior.ctrl_zoom_pct.visible";
    public static final String INFOBAR_INF_CTRL_MODO_ZOOM_VISIBLE = "interfaz.infobar.inferior.ctrl_modo_zoom.visible";
    public static final String INFOBAR_INF_MENSAJES_APP_VISIBLE = "interfaz.infobar.inferior.mensajes_app.visible";
    // ... (añade aquí el resto de claves de infobar inferior)

    
    // --- SECCIÓN: MINIATURAS ---
    public static final String MINIATURAS_CANTIDAD_ANTES = "miniaturas.cantidad.antes";
    public static final String MINIATURAS_CANTIDAD_DESPUES = "miniaturas.cantidad.despues";
    public static final String MINIATURAS_TAMANO_SEL_ANCHO = "miniaturas.tamano.seleccionada.ancho";
    public static final String MINIATURAS_TAMANO_SEL_ALTO = "miniaturas.tamano.seleccionada.alto";
    public static final String MINIATURAS_TAMANO_NORM_ANCHO = "miniaturas.tamano.normal.ancho";
    public static final String MINIATURAS_TAMANO_NORM_ALTO = "miniaturas.tamano.normal.alto";
    public static final String MINIATURAS_CACHE_MAX_SIZE = "miniaturas.cache.tamano.maximo";
//    public static final String MINIATURAS_MOSTRAR_NOMBRES = "miniaturas.ui.mostrar_nombres";

    
    // =================================================================================
    // === NUEVAS CLAVES PARA LA REFACTORIZACIÓN DE BARRAS DE HERRAMIENTAS (Toolbars) ===
    // =================================================================================

    // --- Barra de Navegación ---
    public static final String HERRAMIENTAS_NAVEGACION_VISIBLE = "interfaz.herramientas.navegacion.visible";
    public static final String HERRAMIENTAS_NAVEGACION_BOTON_PRIMERA_VISIBLE = "interfaz.herramientas.navegacion.boton.primera.visible";
    public static final String HERRAMIENTAS_NAVEGACION_BOTON_ANTERIOR_VISIBLE = "interfaz.herramientas.navegacion.boton.anterior.visible";
    public static final String HERRAMIENTAS_NAVEGACION_BOTON_SIGUIENTE_VISIBLE = "interfaz.herramientas.navegacion.boton.siguiente.visible";
    public static final String HERRAMIENTAS_NAVEGACION_BOTON_ULTIMA_VISIBLE = "interfaz.herramientas.navegacion.boton.ultima.visible";

    // --- Barra de Edición ---
    public static final String HERRAMIENTAS_EDICION_VISIBLE = "interfaz.herramientas.edicion.visible";
    public static final String HERRAMIENTAS_EDICION_BOTON_ROTAR_IZQ_VISIBLE = "interfaz.herramientas.edicion.boton.rotar_izq.visible";
    public static final String HERRAMIENTAS_EDICION_BOTON_ROTAR_DER_VISIBLE = "interfaz.herramientas.edicion.boton.rotar_der.visible";
    public static final String HERRAMIENTAS_EDICION_BOTON_VOLTEAR_H_VISIBLE = "interfaz.herramientas.edicion.boton.voltear_h.visible";
    public static final String HERRAMIENTAS_EDICION_BOTON_VOLTEAR_V_VISIBLE = "interfaz.herramientas.edicion.boton.voltear_v.visible";
    public static final String HERRAMIENTAS_EDICION_BOTON_RECORTAR_VISIBLE = "interfaz.herramientas.edicion.boton.recortar.visible";

    // --- Barra de Zoom ---
    public static final String HERRAMIENTAS_ZOOM_VISIBLE = "interfaz.herramientas.zoom.visible";
    public static final String HERRAMIENTAS_ZOOM_BOTON_AJUSTAR_PANTALLA_VISIBLE = "interfaz.herramientas.zoom.boton.ajustar_pantalla.visible";
    public static final String HERRAMIENTAS_ZOOM_BOTON_TAMANO_ORIGINAL_VISIBLE = "interfaz.herramientas.zoom.boton.tamano_original.visible";
    public static final String HERRAMIENTAS_ZOOM_BOTON_ZOOM_MANUAL_VISIBLE = "interfaz.herramientas.zoom.boton.zoom_manual.visible";
    public static final String HERRAMIENTAS_ZOOM_BOTON_RESET_VISIBLE = "interfaz.herramientas.zoom.boton.reset.visible";

    // ... (Añade aquí las claves para el resto de barras: vista, proyecto, etc.) ...
    
    
    //FIXME pendientes de implementar
    public static final String COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE ="comportamiento.navegacion.tamano_salto_bloque"; //nuevo
    
}