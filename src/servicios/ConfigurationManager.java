package servicios;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vista.config.ToolbarButtonDefinition;
import vista.config.ToolbarComponentDefinition;
import vista.config.UIDefinitionService;

public class ConfigurationManager
{
	private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

	// variables de la clase
	public static final String CONFIG_FILE_PATH = "config.cfg";

	private Map<String, String> config;

	private static final Map<String, String> DEFAULT_GROUP_COMMENTS;
	public static final Map<String, String> DEFAULT_CONFIG;
	
	public static final String KEY_INICIO_CARPETA = "inicio.carpeta";
	
	static
	{
		// Usar un método estático para inicializar el mapa de defaults
		DEFAULT_CONFIG = Collections.unmodifiableMap(createDefaultConfigMap());
		logger.debug("Mapa de configuración por defecto inicializado con " + DEFAULT_CONFIG.size() + " claves.");
		DEFAULT_GROUP_COMMENTS = Collections.unmodifiableMap(createDefaultGroupCommentsMap());
		logger.debug("Mapa DEFAULT_GROUP_COMMENTS inicializado con " + DEFAULT_GROUP_COMMENTS.size() + " entradas.");
		
	}

	// Lista de prefijos que definen SECCIONES PRINCIPALES (las que usan =====)
	private static final List<String> KNOWN_SECTION_PREFIXES = List.of(
	        "comportamiento",
	        "inicio",
	        "miniaturas",
	        "interfaz",
	        "proyecto"// Ahora "interfaz" es la sección principal
	);
	
	private static ConfigurationManager instance = null;
	public static synchronized ConfigurationManager getInstance() {
	    if (instance == null) {
	        try {
	            instance = new ConfigurationManager();
	        } catch (IOException e) {
	            throw new RuntimeException("Fallo al inicializar la instancia de ConfigurationManager", e);
	        }
	    }
	    return instance;
	}
	
	private ConfigurationManager() throws IOException
	{

		config = cargarConfiguracion();
		
		logger.debug("[ConfigurationManager] Mapa 'config' INICIALIZADO. HashCode: " + System.identityHashCode(config));
		
	}
	
	// Método para cargar toda la configuración
	public Map<String, String> cargarConfiguracion() throws IOException {
	    File configFile = new File(CONFIG_FILE_PATH);
	    Map<String, String> loadedConfig;

	    // --- FASE 1: LEER EL ARCHIVO O CREARLO SI NO EXISTE ---
	    if (!configFile.exists()) {
	        logger.debug("Config no encontrado. Creando por defecto...");
	        try {
	            // Intenta crear el archivo de configuración por defecto
	            crearConfigPorDefecto(configFile);
	            // Luego, lee el archivo que acabamos de crear
	            loadedConfig = leerArchivoConfigExistente(configFile);
	        } catch (IOException e) {
	            // Si falla la creación o la lectura posterior, es un error crítico.
	            // Usamos el mapa de defaults puro como último recurso.
	            logger.error("¡FALLO CRÍTICO al crear/leer config! Usando defaults internos en memoria.");
	            loadedConfig = new HashMap<>(DEFAULT_CONFIG); 
	        }
	    } else {
	        // Si el archivo ya existe, intentamos leerlo.
	        try {
	            loadedConfig = leerArchivoConfigExistente(configFile);
	        } catch (IOException e) {
	            // Si falla la lectura de un archivo existente, es un error crítico.
	            // Usamos el mapa de defaults puro como último recurso.
	            logger.error("¡FALLO CRÍTICO al leer config existente! Usando defaults internos en memoria. Error: " + e.getMessage());
	            loadedConfig = new HashMap<>(DEFAULT_CONFIG);
	        }
	    }

	 // log --- ¡NUEVO BLOQUE DE DEBUG! ---
	    logger.info("--- DEBUG PRE-SINCRONIZACIÓN ---");
	    logger.debug("Tamaño de DEFAULT_CONFIG: " + DEFAULT_CONFIG.size());
	    logger.debug("Tamaño de loadedConfig (del archivo): " + loadedConfig.size());
	    logger.debug("DEFAULT_CONFIG contiene 'salto_bloque'?: " + DEFAULT_CONFIG.containsKey(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE));
	    logger.debug("loadedConfig contiene 'salto_bloque'?: " + loadedConfig.containsKey(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE));
	    logger.debug("---------------------------------");
	 // --- FIN NUEVO BLOQUE DE DEBUG ---
	    
	    
	    // --- FASE 2: SINCRONIZAR LA CONFIGURACIÓN CARGADA CON LOS VALORES POR DEFECTO ---
	    //    Este bloque asegura que cualquier clave nueva definida en DEFAULT_CONFIG
	    //    que no esté en el archivo .cfg se añada al mapa en memoria.
	    logger.info("Sincronizando configuración cargada con mapa de defaults...");
	    int clavesAñadidas = 0;
	    
	    // Iteramos sobre cada entrada del mapa de configuración por defecto.
	    for (Map.Entry<String, String> defaultEntry : DEFAULT_CONFIG.entrySet()) {
	        
	        // El método putIfAbsent(clave, valor) hace lo siguiente:
	        // 1. Comprueba si 'loadedConfig' contiene 'defaultEntry.getKey()'.
	        // 2. Si NO la contiene, añade la clave y su valor a 'loadedConfig' y devuelve null.
	        // 3. Si SÍ la contiene, no hace NADA y devuelve el valor que ya existía.
	        if (loadedConfig.putIfAbsent(defaultEntry.getKey(), defaultEntry.getValue()) == null) {
	            // Entramos aquí solo si la clave no existía y fue añadida.
	            logger.debug("  -> Clave por defecto faltante añadida a la config en memoria: '" + defaultEntry.getKey() + "'");
	            clavesAñadidas++;
	        }
	    }

	    if (clavesAñadidas > 0) {
	        logger.debug("Se añadieron " + clavesAñadidas + " claves por defecto que no estaban en el archivo config.cfg.");
	    } else {
	        logger.debug("La configuración cargada ya contenía todas las claves por defecto. No se añadieron nuevas.");
	    }
	    // -------------------------------------------------------------------------------------

	    // Devolvemos el mapa 'loadedConfig', que ahora está completo y sincronizado.
	    return loadedConfig;
	}
	

	private Map<String, String> leerArchivoConfigExistente (File configFile) throws IOException
    {
        Map<String, String> loadedConfig = new HashMap<>();
        logger.info("Leyendo configuración desde: " + configFile.getAbsolutePath());

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile)))
        {
            String linea;
            int lineNumber = 0; // Añadir para depuración
            while ((linea = reader.readLine()) != null)
            {
                lineNumber++;
                String lineaOriginal = linea; // Guardar original para log
                linea = linea.trim();

                if (!linea.isEmpty() && !linea.startsWith("#"))
                {
                    String[] partes = linea.split("=", 2);

                    if (partes.length == 2)
                    {
                        String clave = partes[0].trim();
                        String valor = partes[1].trim();

                        logger.debug("  Línea " + lineNumber 
                        		+ ": [" + lineaOriginal + "] -> Clave: '" + clave + "', Valor: '" + valor + "'");
                        // ---------------------

                        if (!clave.isEmpty())
                        {
                            loadedConfig.put(clave, valor);
                        } else { logger.warn("WARN: Clave vacía en línea " + lineNumber); }
                    } else { logger.warn("WARN: Línea mal formada en " + lineNumber + ": " + lineaOriginal); }
                } else {
                	
                     // Opcional: Loguear líneas de comentario/vacías si quieres ver todo
                     logger.debug("  Línea " + lineNumber + ": Ignorada (comentario/vacía)");
                	
                }
            }
        } catch (IOException e) { /* ... */ throw e; } // Asegúrate que el throw esté fuera del try-catch interno si lo hubiera

        logger.info("Configuración leída. Total claves: " + loadedConfig.size());
        return loadedConfig;
    }
	
	
	public void guardarConfiguracion (Map<String, String> configAGuardar) throws IOException
	{

		logger.debug("!!! DEBUG: Guardando mapa. HashCode del mapa recibido: " + System.identityHashCode(configAGuardar));
		
		// log DETALLADO: Inicio y verificación del valor a guardar ---
        logger.debug("[ConfigurationManager guardarConfiguracion] === INICIO GUARDADO ===");
        logger.debug("[ConfigurationManager guardarConfiguracion] Intentando guardar en: " + CONFIG_FILE_PATH);
        String temaEnMapaAGuardar = configAGuardar.get(ConfigKeys.TEMA_NOMBRE);
        logger.debug("[ConfigurationManager guardarConfiguracion] Valor de '" + ConfigKeys.TEMA_NOMBRE + "' que se intentará guardar: '" + temaEnMapaAGuardar + "'");
        if (temaEnMapaAGuardar == null) {
             logger.warn("  --> ¡¡ADVERTENCIA!! La clave '" + ConfigKeys.TEMA_NOMBRE + "' NO está en el mapa que se va a guardar.");
        }
        
        
        // --- BLOQUE DE DEBUG ---
        logger.debug("--- DEBUG: Contenido del mapa a guardar ---");
        boolean encontrado = false;
        for (String clave : configAGuardar.keySet()) {
            if (clave.equals(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE)) {
                logger.debug("  -> ¡ENCONTRADO! " + clave + " = " + configAGuardar.get(clave));
                encontrado = true;
            }
        }
        if (!encontrado) {
            logger.error("  -> ¡ERROR DE DEBUG! La clave '" + ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE + "' NO se encontró en el mapa a guardar.");
        }
        logger.debug("------------------------------------------");
        
        // --- FIN DEL BLOQUE DE DEBUG ---
        //------------------------------------
		
		File configFile = new File(CONFIG_FILE_PATH);
		File tempFile = new File(CONFIG_FILE_PATH + ".tmp"); // Archivo temporal

		// Crear una copia del mapa para poder eliminar claves mientras se procesan
		Map<String, String> configPendiente = new HashMap<>(configAGuardar);

		logger.debug("Guardando configuración (preservando estructura) en " + CONFIG_FILE_PATH);
		logger.debug("Claves a guardar/actualizar: " + configPendiente.size());

		try (BufferedReader reader = new BufferedReader(new FileReader(configFile));
				BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile)))
		{

			String currentLine;
			boolean firstWrite = true; // Para evitar línea en blanco al inicio si no hay cabecera

			while ((currentLine = reader.readLine()) != null)
			{
				String trimmedLine = currentLine.trim();
				boolean esComentarioOVacia = trimmedLine.isEmpty() || trimmedLine.startsWith("#");

				if (esComentarioOVacia)
				{
					// Mantener comentarios y líneas vacías
					writer.write(currentLine + "\n");
					firstWrite = false; // Ya escribimos algo
				} else
				{
					// Es una línea de datos (potencialmente clave=valor)
					String[] parts = trimmedLine.split("=", 2);

					if (parts.length == 2)
					{
						String key = parts[0].trim();

						if (configPendiente.containsKey(key))
						{
							// Clave encontrada en el mapa a guardar: actualizar valor
							String newValue = configPendiente.get(key);
							writer.write(key + " = " + newValue + "\n");
							configPendiente.remove(key); // Marcar como procesada
							
						} else
						{
							writer.write(currentLine + "\n"); // Opción: mantenerla
						}
					} else
					{
						// Línea de datos mal formada, mantenerla como está
						writer.write(currentLine + "\n");
					}
					firstWrite = false;
				}
			}

			// Escribir claves nuevas (las que quedaron en configPendiente) al final
			if (!configPendiente.isEmpty())
			{
				logger.debug("Añadiendo " + configPendiente.size() + " claves nuevas:");

				if (!firstWrite)
				{ // Evitar doble línea en blanco si el archivo original estaba vacío
					writer.write("\n"); // Separador antes de nuevas claves
				}
				writer.write("# ===== Nuevas Configuraciones Añadidas =====\n");

				// Ordenar las nuevas claves para consistencia
				List<String> nuevasClavesOrdenadas = new ArrayList<>(configPendiente.keySet());
				java.util.Collections.sort(nuevasClavesOrdenadas);

				for (String newKey : nuevasClavesOrdenadas)
				{
					String newValue = configPendiente.get(newKey);
					writer.write(newKey + " = " + newValue + "\n");
					logger.debug("  Nuevo: " + newKey + " = " + newValue);
				}
			}

		} catch (IOException e)
		{

			// Limpiar archivo temporal si falla la escritura/lectura
			if (tempFile.exists())
			{
				tempFile.delete();
			}
			throw new IOException("Error durante la actualización del archivo de configuración: " + e.getMessage(), e);
		}

		// Reemplazar el archivo original con el temporal de forma segura
		try
		{
			// Primero, intentar borrar el original (puede fallar si está bloqueado)
			if (configFile.exists() && !configFile.delete())
			{
				logger.warn("Advertencia: No se pudo borrar el archivo de configuración original: "
						+ configFile.getAbsolutePath());
				// Podrías intentar renombrar el original como backup aquí
			}

			// Renombrar el temporal al nombre original
			if (!tempFile.renameTo(configFile))
			{
				// Si falla el renombrado, puede ser un problema de permisos o bloqueo
				// Podrías intentar copiar el contenido si el renombrado falla
				throw new IOException("No se pudo renombrar el archivo temporal a " + configFile.getName());
			}
			logger.debug("Archivo de configuración actualizado exitosamente.");

		} catch (IOException | SecurityException e)
		{
			// Limpiar temporal si falla el reemplazo
			if (tempFile.exists())
				tempFile.delete();
			throw new IOException(
					"Error al reemplazar el archivo de configuración con la versión actualizada: " + e.getMessage(), e);
		}

	}

	// --- Crear Archivo de Configuración por Defecto ---

	// En ConfigurationManager

	private void crearConfigPorDefecto(File configFile) throws IOException {
	    logger.debug("Creando config por defecto (estructura jerárquica v2.1)...");
	    try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
	        writer.write("# Archivo de Configuración VisorV2 (Generado por defecto)\n");
	        writer.write("# Use '#' al inicio de línea para comentarios.\n\n");

	        // --- Preparación ---
	        List<String> sortedKeys = new ArrayList<>(DEFAULT_CONFIG.keySet());
	        Collections.sort(sortedKeys);

	        List<String> sortedCommentPrefixes = new ArrayList<>(DEFAULT_GROUP_COMMENTS.keySet());
	        sortedCommentPrefixes.sort((s1, s2) -> Integer.compare(s2.length(), s1.length())); // Más largo primero

	        // --- Iteración y Escritura ---
	        String lastWrittenCommentPrefix = null; // El prefijo exacto del último comentario escrito
	        boolean firstEntryOverall = true;

	        for (String key : sortedKeys) {
	            String value = DEFAULT_CONFIG.get(key);
	            if (value == null) continue;

	            // --- Encontrar el MEJOR prefijo de comentario para ESTA clave ---
	            String bestMatchingPrefixForKey = null;
	            for (String prefix : sortedCommentPrefixes) {
	                if (key.startsWith(prefix)) {
	                    bestMatchingPrefixForKey = prefix; // Encontramos el más largo/específico
	                    break;
	                }
	            }

	            // --- Lógica de Escritura de Comentarios ---
	            // Escribir un comentario SI el mejor prefijo para esta clave
	            // es DIFERENTE del último prefijo de comentario que escribimos.
	            if (!Objects.equals(bestMatchingPrefixForKey, lastWrittenCommentPrefix)) {

	                 // Determinar si necesitamos un salto de línea extra (cambio de sección mayor?)
	                 boolean isMajorSectionChange = false;
	                 if (lastWrittenCommentPrefix != null && bestMatchingPrefixForKey != null) {
	                     // Es un cambio mayor si el nuevo prefijo no empieza con el anterior
	                     // O si alguno pertenece a KNOWN_SECTION_PREFIXES y el otro no o es diferente
	                     String currentTopSection = findTopSection(bestMatchingPrefixForKey);
	                     String lastTopSection = findTopSection(lastWrittenCommentPrefix);
	                     if (!Objects.equals(currentTopSection, lastTopSection)) {
	                         isMajorSectionChange = true;
	                     }
	                 } else if (bestMatchingPrefixForKey != null) {
	                     // Si antes no había prefijo y ahora sí, podría ser inicio de sección
	                     if (KNOWN_SECTION_PREFIXES.contains(bestMatchingPrefixForKey)) {
	                         isMajorSectionChange = true;
	                     }
	                 }

	                 // Escribir salto de línea antes del comentario si no es el primero general
	                 // Y si es un cambio de sección mayor O si simplemente cambió el prefijo
	                 if (!firstEntryOverall) {
	                     // Poner doble salto si es cambio de sección mayor, simple si es subgrupo
	                     writer.write(isMajorSectionChange ? "\n\n" : "\n");
	                 }

	                // Escribir el comentario si encontramos un prefijo para esta clave
	                if (bestMatchingPrefixForKey != null) {
	                    String comment = DEFAULT_GROUP_COMMENTS.get(bestMatchingPrefixForKey);
	                    if (comment != null) {
	                        writer.write(comment + "\n");
	                    } else {
	                        // Caso raro: el prefijo existe pero no tiene comentario?
	                        writer.write("# Prefijo encontrado: " + bestMatchingPrefixForKey + " (Sin Comentario)\n");
	                    }
	                } else {
	                    // Si una clave no coincide con NINGÚN prefijo, no escribimos comentario para ella
	                    // Podríamos añadir un comentario genérico aquí si quisiéramos agrupar los "sin grupo"
	                    // writer.write("# == Otras Configuraciones ==\n"); // <-- Cuidado, se repetiría
	                }

	                // Actualizar el último prefijo escrito
	                lastWrittenCommentPrefix = bestMatchingPrefixForKey;
	            }

	            // --- Escribir Clave-Valor ---
	            writer.write(key + " = " + value + "\n");
	            firstEntryOverall = false;

	        } // Fin del bucle for

	        writer.write("\n#----------------------- FIN CONFIGURACION ------------------------\n");

	    } catch (IOException e) {
	        // ... manejo de error ...
	    }
	     logger.debug("Archivo de configuración por defecto creado en: " + configFile.getAbsolutePath());
	}

	
	// Helper para encontrar la sección principal de un prefijo
	private String findTopSection(String prefix) {
	     if (prefix == null) return null;
	     for(String sectionPrefix : KNOWN_SECTION_PREFIXES) {
	         if (prefix.startsWith(sectionPrefix)) {
	             return sectionPrefix;
	         }
	     }
	     return null; // O devolver "otros"
	}
	
	
	// Método estático privado para crear el mapa de defaults
	private static Map<String, String> createDefaultConfigMap ()
	{
		// ===== Inicio =====
		Map<String, String> defaults = new HashMap<>();
		
		// --- 1. CONFIGURACIÓN BÁSICA Y DE COMPORTAMIENTO ---
	    String inicioCarpetaDefault = System.getProperty("user.dir"); // Directorio actual del proyecto
	    
	    defaults.put(ConfigKeys.INICIO_CARPETA, inicioCarpetaDefault + "\\resources" + "quitar esto");
	    defaults.put(ConfigKeys.INICIO_IMAGEN, "");

	    defaults.put(ConfigKeys.PROYECTOS_CARPETA_BASE, inicioCarpetaDefault + "\\.proyectos");
	    defaults.put(ConfigKeys.PROYECTOS_ARCHIVO_TEMPORAL, "seleccion_actual.prj");
//	    defaults.put(ConfigKeys.PROYECTOS_ARCHIVO_RECUPERACION, "");
//	    defaults.put(ConfigKeys.PROYECTOS_ULTIMO_PROYECTO_ABIERTO, "");
	    
	    defaults.put(ConfigKeys.COMPORTAMIENTO_ZOOM_MANUAL_INICIAL, "true");
	    defaults.put(ConfigKeys.COMPORTAMIENTO_ZOOM_MODO_INICIAL, "FIT_TO_SCREEN");
	    defaults.put(ConfigKeys.COMPORTAMIENTO_ZOOM_ULTIMO_MODO, "FIT_TO_SCREEN");
	    defaults.put(ConfigKeys.COMPORTAMIENTO_ZOOM_PORCENTAJE_PERSONALIZADO, "100.0");
	    
	    
	    defaults.put(ConfigKeys.COMPORTAMIENTO_NAVEGACION_CIRCULAR, "false");
	    defaults.put(ConfigKeys.COMPORTAMIENTO_CARGAR_SUBCARPETAS, "true");
	    defaults.put(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE, "10");
	    defaults.put(ConfigKeys.COMPORTAMIENTO_PANTALLA_COMPLETA, "false");
	    defaults.put(ConfigKeys.COMPORTAMIENTO_ZOOM_AL_CURSOR_ACTIVADO, "true");

//	    defaults.put(ConfigKeys.WINDOW_X, "-1");
//	    defaults.put(ConfigKeys.WINDOW_Y, "-1");
	    defaults.put(ConfigKeys.WINDOW_X, "-6");
	    defaults.put(ConfigKeys.WINDOW_Y, "3");
	    defaults.put(ConfigKeys.WINDOW_WIDTH, "1544");
	    defaults.put(ConfigKeys.WINDOW_HEIGHT, "500");
//	    defaults.put(ConfigKeys.WINDOW_WIDTH, "1280");
//	    defaults.put(ConfigKeys.WINDOW_HEIGHT, "800");
	    
//	    defaults.put(ConfigKeys.WINDOW_MAXIMIZED, "false");// INICIO DE LA APLICACION TAMAÑO VENTANA
	    defaults.put(ConfigKeys.WINDOW_MAXIMIZED, "true"); // INICIO DE LA APLICACION MAXIMIZADA

	    defaults.put(ConfigKeys.TEMA_NOMBRE, "purpura_misterioso");// clear"); TEMA DE INICIO POR DEFECTO
	    defaults.put("iconos.ancho", "18");
	    defaults.put("iconos.alto", "18");
	    
	    defaults.put("ui.splitpane.main.dividerLocation", "0.25");
	    
	    defaults.put(ConfigKeys.CAROUSEL_DELAY_MS, "3000"); // 3 segundos por defecto
        defaults.put(ConfigKeys.COMPORTAMIENTO_SYNC_VISOR_CARRUSEL, "false"); // Sync desactivado por defecto
        defaults.put(ConfigKeys.CARRUSEL_ESTADO_ULTIMA_CARPETA, ""); // Sin carpeta guardada por defecto
        defaults.put(ConfigKeys.CARRUSEL_ESTADO_ULTIMA_IMAGEN, ""); // Sin imagen guardada por defecto

	    // --- 2. CONFIGURACIÓN DE MINIATURAS ---
	    defaults.put(ConfigKeys.MINIATURAS_CANTIDAD_ANTES, "9");
	    defaults.put(ConfigKeys.MINIATURAS_CANTIDAD_DESPUES, "9");
	    defaults.put(ConfigKeys.MINIATURAS_TAMANO_SEL_ANCHO, "80");
	    defaults.put(ConfigKeys.MINIATURAS_TAMANO_SEL_ALTO, "80");
	    defaults.put(ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, "70");
	    defaults.put(ConfigKeys.MINIATURAS_TAMANO_NORM_ALTO, "70");
	    defaults.put(ConfigKeys.MINIATURAS_CACHE_MAX_SIZE, "200");
	    
	    // --- 3. ESTADOS DE MENÚS (CHECKBOXES Y RADIOS) ---
	    defaults.put(ConfigKeys.VISTA_MOSTRAR_NOMBRES_MINIATURAS_STATE, "true");
	    defaults.put("interfaz.menu.vista.barra_de_menu.seleccionado", "true");
	    defaults.put("interfaz.menu.vista.barra_de_botones.seleccionado", "true");
	    defaults.put("interfaz.menu.vista.mostrar_ocultar_la_lista_de_archivos.seleccionado", "true");
	    defaults.put("interfaz.menu.vista.imagenes_en_miniatura.seleccionado", "true");
	    defaults.put("interfaz.menu.vista.mantener_ventana_siempre_encima.seleccionado", "false");
	    defaults.put("interfaz.menu.vista.fondo_a_cuadros.seleccionado", "false");
	    defaults.put("interfaz.menu.zoom.mantener_proporciones.seleccionado", "true");

	    // --- 4. VISIBILIDAD DE BARRAS DE INFORMACIÓN ---
	    defaults.put(ConfigKeys.INFOBAR_SUP_VISIBLE, "true");
	    defaults.put(ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_VISIBLE, "true");
	    defaults.put(ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_FORMATO, "solo_nombre");
	    defaults.put(ConfigKeys.INFOBAR_SUP_INDICE_TOTAL_VISIBLE, "true");
	    defaults.put(ConfigKeys.INFOBAR_SUP_DIMENSIONES_VISIBLE, "true");
	    defaults.put(ConfigKeys.INFOBAR_SUP_TAMANO_ARCHIVO_VISIBLE, "true");
	    defaults.put(ConfigKeys.INFOBAR_SUP_FECHA_ARCHIVO_VISIBLE, "true");
	    defaults.put(ConfigKeys.INFOBAR_SUP_FORMATO_IMAGEN_VISIBLE, "true");
	    defaults.put(ConfigKeys.INFOBAR_SUP_MODO_ZOOM_VISIBLE, "true");
	    defaults.put(ConfigKeys.INFOBAR_SUP_ZOOM_REAL_PCT_VISIBLE, "true");

	    defaults.put(ConfigKeys.INFOBAR_INF_VISIBLE, "true");
	    defaults.put(ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_VISIBLE, "true");
	    defaults.put(ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_FORMATO, "ruta_completa");
	    defaults.put(ConfigKeys.INFOBAR_INF_ICONO_ZM_VISIBLE, "true");
	    defaults.put(ConfigKeys.INFOBAR_INF_ICONO_PROP_VISIBLE, "true");
	    defaults.put(ConfigKeys.INFOBAR_INF_ICONO_SUBC_VISIBLE, "true");
	    defaults.put(ConfigKeys.INFOBAR_INF_CTRL_ZOOM_PCT_VISIBLE, "true");
	    defaults.put(ConfigKeys.INFOBAR_INF_CTRL_MODO_ZOOM_VISIBLE, "true");
	    defaults.put(ConfigKeys.INFOBAR_INF_MENSAJES_APP_VISIBLE, "true");

	    defaults.put(ConfigKeys.GRID_MOSTRAR_NOMBRES_STATE, "true");
	    defaults.put(ConfigKeys.GRID_MOSTRAR_ESTADO_STATE, "true");
	    defaults.put(ConfigKeys.GRID_THUMBNAIL_WIDTH, "120");
	    defaults.put(ConfigKeys.GRID_THUMBNAIL_HEIGHT, "120");
	    
	    defaults.put("interfaz.menu.vista.barra_de_botones.seleccionado.visible", "true");
	    defaults.put("interfaz.herramientas.botonesOrdenLista.visible", "true");

	    defaults.put("proyectos.estado.recuperacion_pendiente", "true");
	    
	    defaults.put("interfaz.menu.vista.barra_de_menu.seleccionado.visible", "true");
	    defaults.put("interfaz.menu.vista.imagenes_en_miniatura.seleccionado.visible", "true");
	    defaults.put("interfaz.menu.vista.mostrar_ocultar_la_lista_de_archivos.seleccionado.visible", "true");
	    
	    defaults.put("interfaz.boton.barra_estado.control_modoZoom.visible", "true");
	    defaults.put("interfaz.boton.barra_estado.mantener_proporciones.visible", "true");
	    defaults.put("interfaz.boton.barra_estado.subcarpetas.visible", "true");
	    defaults.put("interfaz.boton.barra_estado.vista_toggle_always_on_top.visible", "true");
	    defaults.put("interfaz.boton.barra_estado.zoom_manual_toggle.visible", "true");
	    
	    defaults.put("interfaz.boton.botonesOrdenLista.filtro_activo.visible", "true");
	    defaults.put("interfaz.boton.botonesOrdenLista.filtro_toggleLiveFilter.visible", "true");
	    defaults.put("interfaz.boton.botonesOrdenLista.orden_carpeta_anterior.visible", "true");
	    defaults.put("interfaz.boton.botonesOrdenLista.orden_carpeta_raiz.visible", "true");
	    defaults.put("interfaz.boton.botonesOrdenLista.orden_carpeta_siguiente.visible", "true");
	    defaults.put("interfaz.boton.botonesOrdenLista.orden_ciclo.visible", "true");
	    
	    defaults.put("interfaz.boton.orden_lista.filtro_activo.visible", "true");
	    defaults.put("interfaz.boton.orden_lista.filtro_toggleLiveFilter.visible", "true");
	    defaults.put("interfaz.boton.orden_lista.orden_carpeta_anterior.visible", "true");
	    defaults.put("interfaz.boton.orden_lista.orden_carpeta_raiz.visible", "true");
	    defaults.put("interfaz.boton.orden_lista.orden_carpeta_siguiente.visible", "true");
	    defaults.put("interfaz.boton.orden_lista.orden_ciclo.visible", "true");
	    // --- 5. VISIBILIDAD DINÁMICA DE TOOLBARS Y BOTONES ---
	    UIDefinitionService uiDefs = new UIDefinitionService();
	    
	    // Visibilidad de barras de herramientas completas
	    uiDefs.generateModularToolbarStructure().forEach(toolbarDef -> {
	        defaults.put(ConfigKeys.toolbarVisible(toolbarDef.claveBarra()), "true");
	    });
	    
	    // Visibilidad de botones individuales
	    uiDefs.generateModularToolbarStructure().forEach(toolbarDef -> {
	        // Usamos el mismo patrón de bucle for seguro
	        for (ToolbarComponentDefinition compDef : toolbarDef.componentes()) {
	            // Filtramos para procesar solo los botones
	            if (compDef instanceof ToolbarButtonDefinition buttonDef) {
	                String buttonVisibilityKey = ConfigKeys.toolbarButtonVisible(
	                    toolbarDef.claveBarra(), 
	                    buttonDef.comandoCanonico()
	                );
	                defaults.put(buttonVisibilityKey, "true");
	            }
	        }
	    });
	    
	    return defaults;
	}


	private static Map<String, String> createDefaultGroupCommentsMap () {
        Map<String, String> comments = new HashMap<>();

        // --- Secciones Principales ---
        comments.put("inicio",         	"# ===== Inicio =====");
        comments.put("miniaturas",     	"# ===== Barra de Imagenes en Miniatura =====");
        comments.put("interfaz",       	"# ===== Interfaz =====");

        // --- Ventana de la aplicacion
        comments.put("window", 			"# ===== Estado de la Ventana de la aplicacion=====");
        comments.put("comportamiento", 	"# ===== Comportamiento =====");
        comments.put("carrusel", 	    "# === Comportamiento del Carrusel ===");
       // --------------------------------------------
        
        // --- Personalizacion
//        comments.put("colores", 	   	"# ===== Colores UI =====");
        comments.put("iconos",         	"# ===== Configuración General Iconos ====="); // Para 'iconos.alto', 'iconos.ancho'
        comments.put("tema",			"# ===== Tema Visual=====");
        comments.put("proyectos",		"# ===== Gestión de Proyectos/Selecciones =====");
        
//        comments.put("tema.nombre", 	"# Nombre del tema (dark, clear, blue, orange, green");
//        comments.put("interfaz.menu.configuracion.tema", "# ===== Tema Visual=====");
        
        
        // --- Subgrupos Nivel 1 (Dentro de interfaz) ---
        comments.put("interfaz.boton", 	"# == Botones =="); // Subgrupo para TODOS los botones
        comments.put("interfaz.menu",  	"# == Menús ==");  // Subgrupo para TODOS los menús
        comments.put("interfaz.infobar",             "# ===== Configuración General de Barras de Información =====");
        comments.put("interfaz.infobar.superior",    "# === Barra de Información Superior ===");
        comments.put("interfaz.infobar.inferior",    "# === Barra de Estado/Control Inferior ===");
        
        comments.put("comportamiento.display", "# === Comportamiento de Visualización ===");
        comments.put("comportamiento.zoom", "# === Comportamiento del Zoom ===");

        // --- Subgrupos Nivel 2 (Dentro de interfaz.boton) ---
        comments.put("interfaz.boton.movimiento", "# === Botones de Movimiento ==="); // Nota: Uso "===" para diferenciar nivel
        comments.put("interfaz.boton.edicion",    "# === Botones de Edición ===");
        comments.put("interfaz.boton.zoom",       "# === Botones de Zoom ===");
        comments.put("interfaz.boton.vista",       "# === Botones de Visualizacion ===");
        comments.put("interfaz.boton.control",       "# === Botones de Control ===");
        comments.put("interfaz.boton.especiales",       "# === Botones Especiales ===");

        // --- Subgrupos Nivel 2 (Dentro de interfaz.menu) ---
        comments.put("interfaz.menu.archivo",     "# === Menú Archivo ===");
        comments.put("interfaz.menu.navegacion",  "# === Menú Navegación ===");
        comments.put("interfaz.menu.zoom",  "# === Menú Zoom ===");
        comments.put("interfaz.menu.imagen",  "# === Menú Imagen ===");
        comments.put("interfaz.menu.vista",  "# === Menú Vista ===");
        comments.put("interfaz.menu.configuracion",  "# === Menú Configuracion ===");
        
        // --- Subgrupos Nivel 2 (Dentro de interfaz.infobar) ---
        comments.put("interfaz.infobar.superior.nombre_ruta", "# --- Visibilidad y Formato para Nombre/Ruta (Superior) ---");
        comments.put("interfaz.infobar.inferior.nombre_ruta", "# --- Visibilidad y Formato para Nombre/Ruta (Inferior) ---");
        // ... etc para otros grupos de menús

        // --- Subgrupos Nivel 1 (Dentro de miniaturas) ---
        comments.put("miniaturas.cantidad",          	"# == Cantidad de miniaturas Antes/Después ==");
        comments.put("miniaturas.tamano.normal",     	"# == Tamaño Normal ==");
        comments.put("miniaturas.tamano.seleccionada", 	"# == Tamaño Seleccionada ==");
        comments.put("miniaturas.ui", 					"# == Configuración de Miniaturas en la UI ==");

        // --- Paneles de datos (infobars)
        comments.put("interfaz.menu.configuracion.paneles_de_datos", "# == Barras de Status o Paneles de Datos ==");
        comments.put("interfaz.menu.configuracion.paneles_de_datos.estado_y_control", "# == Panel de Estado ==");
        comments.put("interfaz.menu.configuracion.paneles_de_datos.informacion_de_imagen", "# == Panel de Informacion de Imagen ==");
        
        // ... Añadir más según sea necesario ...
        return comments;
    }
	

// *******************************************************************************************************************************
// GETTERS Y SETTERS
// *******************************************************************************************************************************



	// Devuelve una COPIA del mapa para proteger el estado interno
	public Map<String, String> getConfigMap (){return new HashMap<>(config);}
	public Map<String, String> getConfig ()	{return config;}

	// Método para actualizar una clave en memoria (importante usar synchronized si hubiera concurrencia)
	public synchronized void setString (String key, String value) {
		
		if (config != null && key != null && value != null){
			config.put(key, value);
		} else {
			logger.warn(
					"[Config Mgr] Intento de actualizar config con null (key=" + key + ", value=" + value + ")");
		}
	} // --- Fin del metodo setString ---

	/**
     * Elimina una propiedad del mapa de configuración en memoria.
     * Al guardar, esta clave ya no existirá en el archivo config.cfg.
     * Este método es 'synchronized' para mantener la consistencia con setString.
     *
     * @param key La clave de la propiedad a eliminar.
     */
    public synchronized void remove(String key) {
        if (config != null && key != null) {
            // Llama al método remove() del mapa 'config' de la instancia.
            config.remove(key); 
            logger.debug("  [ConfigManager] Propiedad eliminada de la configuración en memoria: " + key);
        } else {
            logger.warn(
                    "[Config Mgr] Intento de eliminar propiedad con key nula o config nulo.");
        }
    } // --- FIN del metodo remove ---
	
	// Método específico para inicio.carpeta por conveniencia
	public void setInicioCarpeta (String path){setString("inicio.carpeta", path != null ? path : "");}

	// Útil para el Controller si necesita saber el default sin leer la config	cargada
	public static String getDefault (String key){return DEFAULT_CONFIG.get(key);}
	public static String getDefault (String key, String fallback){return DEFAULT_CONFIG.getOrDefault(key, fallback);}

	
	// Zoom Personalizado
	public double getZoomPersonalizadoPorcentaje() {return getDouble(ConfigKeys.COMPORTAMIENTO_ZOOM_PORCENTAJE_PERSONALIZADO, 100.0);}
	
	public void setZoomPersonalizadoPorcentaje(double porcentaje) {
		
		String valorFormateado;
		
	    if (porcentaje == (long) porcentaje) { // Comprueba si es un entero
	        valorFormateado = String.format("%.0f", porcentaje); // Sin decimales
	    } else {
	        valorFormateado = String.format("%.2f", porcentaje).replace(',', '.'); // Un decimal, asegurar punto
	    }
	    
		//setString(KEY_COMPORTAMIENTO_ZOOM_PERSONALIZADO_PORCENTAJE, String.valueOf(porcentaje));
		setString(ConfigKeys.COMPORTAMIENTO_ZOOM_PORCENTAJE_PERSONALIZADO, valorFormateado);
		
	}
	
	
	/**
     * Obtiene el nombre del tema actual definido en la configuración (clave 'tema').
     * Si la clave 'tema' no está definida en el archivo cargado,
     * devuelve el valor por defecto definido en DEFAULT_CONFIG ("clear").
     * Si DEFAULT_CONFIG tampoco tiene 'tema', devuelve "clear" como último recurso.
     *
     * @return El nombre del tema actual (ej. "clear", "dark", "blue", "green", "orange").
     */
    public String getTemaActual() {return getString(ConfigKeys.TEMA_NOMBRE, "clear");}
    
    
    /**
     * Determina el nombre de la carpeta de iconos correspondiente al tema actual.
     * Mapea los nombres de tema ("clear", "dark", "blue", etc.) a los nombres
     * de las carpetas físicas de iconos ("black", "white", "blue", etc.).
     *
     * @return El nombre de la subcarpeta de iconos a usar (ej. "black", "white").
     */
    public String getCarpetaIconosTemaActual() {
        String tema = getTemaActual(); // Obtiene el tema ("clear", "dark", etc.)

        // Convertimos a minúsculas para hacer la comparación insensible a mayúsculas/minúsculas
        switch (tema.toLowerCase()) {
            case "dark":
                return "white"; // Tema dark usa iconos blancos
            case "blue":
                return "blue";  // Tema blue usa iconos azules
            case "green":
                return "green"; // Tema green usa iconos verdes
            case "orange":
                return "orange";// Tema orange usa iconos naranjas
            case "clear":
                 // Incluye el caso por defecto si el tema no es reconocido o es "clear"
            default:
                return "black"; // Tema clear (o desconocido) usa iconos negros
        }
    }
    
    
    /**
     * Establece un nuevo tema en la configuración en memoria y luego
     * guarda toda la configuración actual en el archivo 'config.cfg'.
     *
     * @param nuevoTema El nombre del nuevo tema a establecer (ej. "dark", "clear").
     *                  Debe coincidir con las opciones válidas ("clear", "dark", "blue", etc.).
     */
    public void setTemaActualYGuardar(String nuevoTema) {
        if (nuevoTema == null || nuevoTema.trim().isEmpty()) {
            logger.warn("WARN [ConfigurationManager]: Intento de establecer un tema nulo o vacío. No se guardará.");
            return;
        }
    
        // Podrías añadir una validación aquí para asegurar que nuevoTema es uno de los soportados
        // List<String> temasValidos = List.of("clear", "dark", "blue", "green", "orange");
        // if (!temasValidos.contains(nuevoTema.toLowerCase())) {
        //     logger.warn("WARN [ConfigurationManager]: Tema '" + nuevoTema + "' no reconocido. No se guardará.");
        //     return;
        // }

        // Actualiza el valor en el mapa 'config' en memoria
        setString(ConfigKeys.TEMA_NOMBRE, nuevoTema.trim()); // setString ya imprime un log

        //log [ConfigurationManager setTemaActualYGuardar] Valor de: 
        String valorEnMemoria = config.get(ConfigKeys.TEMA_NOMBRE);
        logger.debug("[ConfigurationManager setTemaActualYGuardar] Valor de '" + ConfigKeys.TEMA_NOMBRE + "' en memoria ANTES de guardar: '" + valorEnMemoria + "'");
        if (!nuevoTema.trim().equals(valorEnMemoria)) {
             logger.warn("  --> ¡¡ADVERTENCIA!! El valor en memoria no coincide con el solicitado después de setString.");
        }
        //-----------------------
        
        // Guarda el mapa 'config' completo (que ahora incluye el nuevo tema) en el archivo
        try {
            guardarConfiguracion(this.config); // Usa el método existente para guardar
            logger.debug("[ConfigurationManager]: Tema cambiado a '" + nuevoTema + "' y configuración guardada en " + CONFIG_FILE_PATH);
            // Necesitarás lógica adicional en tu UI (posiblemente reiniciar o recargar elementos)
        } catch (IOException e) {
            logger.error("ERROR CRÍTICO [ConfigurationManager]: No se pudo guardar la configuración después de cambiar el tema a '" + nuevoTema + "'. Error: " + e.getMessage());
            // Considera notificar al usuario o revertir el cambio en 'config' si el guardado falla.
        }
    }
    
    
// *******************************************************************************************************************************
// 													GETTERS DE CONFIGURACION 
// *******************************************************************************************************************************

    /**
     * Obtiene un valor String de la configuración.
     * Si la clave no existe, devuelve el valor por defecto proporcionado.
     * Este método es de SOLO LECTURA y no modifica la configuración en memoria.
     *
     * @param key La clave de configuración.
     * @param defaultValue El valor a devolver si la clave no se encuentra.
     * @return El valor de la configuración o el valor por defecto.
     */
    public String getString(String key, String defaultValue) {
        // Usa getOrDefault, que es la forma más limpia y segura.
        return config.getOrDefault(key, defaultValue);
    }

    /**
     * Obtiene un valor entero de la configuración.
     * Valida que la clave exista y que su valor sea un número entero válido.
     * Si no existe o es inválido, utiliza el valor por defecto, lo guarda/corrige en la
     * configuración en memoria para que se persista al cerrar, y lo devuelve.
     *
     * @param key La clave de configuración.
     * @param defaultValue El valor a usar y guardar si la clave no se encuentra o es inválida.
     * @return El valor entero de la configuración o el valor por defecto.
     */
    public int getInt(String key, int defaultValue) {
        String valueStr = config.get(key);
        if (valueStr == null) {
            logger.warn("WARN: Clave '" + key + "' no encontrada. Usando default '" + defaultValue + "' y añadiendo a config en memoria.");
            this.setString(key, String.valueOf(defaultValue)); // Autocorrección en memoria
            return defaultValue;
        }

        try {
            return Integer.parseInt(valueStr.trim());
        } catch (NumberFormatException e) {
            logger.warn("WARN: Valor inválido para la clave '" + key + "': '" + valueStr + "'. Usando default '" + defaultValue + "' y corrigiendo en memoria.");
            this.setString(key, String.valueOf(defaultValue)); // Autocorrección en memoria
            return defaultValue;
        }
    }

    /**
     * Obtiene un valor booleano de la configuración.
     * Valida que la clave exista y que su valor sea "true" o "false" (ignorando mayúsculas/minúsculas).
     * Si no existe o es inválido, utiliza el valor por defecto, lo guarda/corrige en la
     * configuración en memoria para que se persista al cerrar, y lo devuelve.
     *
     * @param key La clave de configuración.
     * @param defaultValue El valor a usar y guardar si la clave no se encuentra o es inválida.
     * @return El valor booleano de la configuración o el valor por defecto.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String valueStr = config.get(key);
        if (valueStr == null) {
            logger.warn("WARN: Clave '" + key + "' no encontrada. Usando default '" + defaultValue + "' y añadiendo a config en memoria.");
            this.setString(key, String.valueOf(defaultValue)); // Autocorrección en memoria
            return defaultValue;
        }

        String trimmedValue = valueStr.trim().toLowerCase();
        if (trimmedValue.equals("true")) {
            return true;
        } else if (trimmedValue.equals("false")) {
            return false;
        } else {
            logger.warn("WARN: Valor inválido para la clave '" + key + "': '" + valueStr + "'. Debe ser 'true' o 'false'. Usando default '" + defaultValue + "' y corrigiendo en memoria.");
            this.setString(key, String.valueOf(defaultValue)); // Autocorrección en memoria
            return defaultValue;
        }
    }

    /**
     * Obtiene un valor double de la configuración.
     * Valida que la clave exista y que su valor sea un número double válido (acepta '.' y ',' como separador decimal).
     * Si no existe o es inválido, utiliza el valor por defecto, lo guarda/corrige en la
     * configuración en memoria para que se persista al cerrar, y lo devuelve.
     *
     * @param key La clave de configuración.
     * @param defaultValue El valor a usar y guardar si la clave no se encuentra o es inválida.
     * @return El valor double de la configuración o el valor por defecto.
     */
    public double getDouble(String key, double defaultValue) {
        String valueStr = config.get(key);
        if (valueStr == null) {
            logger.warn("WARN: Clave '" + key + "' no encontrada. Usando default '" + defaultValue + "' y añadiendo a config en memoria.");
            this.setString(key, String.valueOf(defaultValue)); // Autocorrección en memoria
            return defaultValue;
        }

        try {
            // Reemplaza la coma por un punto para asegurar compatibilidad con Double.parseDouble
            return Double.parseDouble(valueStr.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            logger.warn("WARN: Valor inválido para la clave '" + key + "': '" + valueStr + "'. Usando default '" + defaultValue + "' y corrigiendo en memoria.");
            this.setString(key, String.valueOf(defaultValue)); // Autocorrección en memoria
            return defaultValue;
        }
    }
    
    /**
     * Descarta la configuración actual en memoria y la reemplaza con una
     * copia fresca del mapa de configuración por defecto (DEFAULT_CONFIG).
     * Esto es útil para la funcionalidad de "Restaurar Defaults".
     */
    public void resetToDefaults() {
        logger.debug("[ConfigurationManager] Reseteando configuración en memoria a los valores por defecto...");

        // Paso 1: Crear el nuevo mapa en una variable local para que no haya ambigüedad.
        Map<String, String> nuevoMapaDefaults = new HashMap<>(ConfigurationManager.DEFAULT_CONFIG);
        
        // Paso 2: Asignarlo al campo de la instancia.
        this.config = nuevoMapaDefaults;
        
        // Paso 3: Imprimir el tamaño.
        logger.debug("  -> Configuración en memoria reseteada. Total claves: " + this.config.size());
    } // --- FIN del método resetToDefaults ---
    
    
    public Color getColor(String key, Color defaultColor) {
        // Este método AHORA usa nuestro getString seguro y de solo lectura.
        String rgbString = getString(key, null);
        
        if (rgbString == null || rgbString.trim().isEmpty()) {
            return defaultColor;
        }
        
        String[] components = rgbString.split(",");
        if (components.length == 3) {
            try {
                int r = Integer.parseInt(components[0].trim());
                int g = Integer.parseInt(components[1].trim());
                int b = Integer.parseInt(components[2].trim());
                return new Color(r, g, b);
            } catch (NumberFormatException e) {
                return defaultColor;
            }
        }
        return defaultColor;
    } // --- FIN DEL METODO getColor ---
    

    /**
     * Guarda un color en la configuración en formato "R,G,B".
     * @param key La clave de configuración.
     * @param color El objeto Color a guardar.
     */
    public void setColor(String key, Color color) {
        if (color == null) {
            remove(key);
            return;
        }
        String rgbString = color.getRed() + "," + color.getGreen() + "," + color.getBlue();
        setString(key, rgbString);
    }

    
    /**
     * Elimina una propiedad del mapa de configuración actual.
     * Este cambio no se guardará en el archivo .cfg hasta que se llame a saveConfig().
     *
     * @param key La clave de la propiedad a eliminar.
     */
    public void removeProperty(String key) {
        if (key != null && this.config != null) {
            // Llama al método remove() del mapa 'config' de la instancia.
            this.config.remove(key); 
            logger.debug("  [ConfigManager] Propiedad eliminada (en memoria): " + key);
        }
    }
    
} //Fin configurationManager

