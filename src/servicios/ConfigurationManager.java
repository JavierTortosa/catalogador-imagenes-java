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

import vista.config.ToolbarButtonDefinition;
import vista.config.ToolbarComponentDefinition;
import vista.config.UIDefinitionService;

public class ConfigurationManager
{

	// variables de la clase
	public static final String CONFIG_FILE_PATH = "config.cfg";

	private Map<String, String> config;

	private static final Map<String, String> DEFAULT_GROUP_COMMENTS;
	public static final Map<String, String> DEFAULT_CONFIG;
	
	// --- CONSTANTES INICIALES
	public static final String KEY_INICIO_CARPETA = "inicio.carpeta";
	
	// --- CONSTANTES PARA ESTADO VENTANA ---
	
    // --- CONSTANTES PARA PROYECTOS
	
    // Para el futuro:

    // --- 	CONSTANTES PARA COMPORTAMIENTO ---
    
    
	// --- CONSTANTES PARA VISIBILIDAD Y FORMATO DE BARRAS DE INFORMACIÓN ---

    // -- Visibilidad de Barras Completas --

    // -- Elementos Barra Superior --

    // --- SECCIÓN: BARRAS DE INFORMACIÓN (INFERIOR) ---
    
    
    
	static
	{
		// Usar un método estático para inicializar el mapa de defaults
		DEFAULT_CONFIG = Collections.unmodifiableMap(createDefaultConfigMap());
		System.out.println("Mapa de configuración por defecto inicializado con " + DEFAULT_CONFIG.size() + " claves.");
		DEFAULT_GROUP_COMMENTS = Collections.unmodifiableMap(createDefaultGroupCommentsMap());
		System.out.println("Mapa DEFAULT_GROUP_COMMENTS inicializado con " + DEFAULT_GROUP_COMMENTS.size() + " entradas.");
		
	}

	// Lista de prefijos que definen SECCIONES PRINCIPALES (las que usan =====)
	private static final List<String> KNOWN_SECTION_PREFIXES = List.of(
	        "comportamiento",
	        "inicio",
	        "miniaturas",
	        "interfaz",
	        "proyecto"// Ahora "interfaz" es la sección principal
	);
	
	//--------------------------------------------------------------------------------------------------------------
	//FIXME comentar este bloque para eliminar el singleton de configurationManager
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
	//FIXME recordar volver public el constructor y modificar el bloque de "singleton de configurationmanager"
	//--------------------------------------------------------------------------------------------------------------
	
	private ConfigurationManager() throws IOException
	{

		config = cargarConfiguracion();
		
		//LOG ConfigurationManager inicializado.
		System.out.println("ConfigurationManager inicializado.");
		System.out.println("!!! DEBUG: Mapa 'config' INICIALIZADO. HashCode: " + System.identityHashCode(config));
		
	}

	
	// Método para cargar toda la configuración
	public Map<String, String> cargarConfiguracion() throws IOException {
	    File configFile = new File(CONFIG_FILE_PATH);
	    Map<String, String> loadedConfig;

	    // --- FASE 1: LEER EL ARCHIVO O CREARLO SI NO EXISTE ---
	    if (!configFile.exists()) {
	        System.out.println("Config no encontrado. Creando por defecto...");
	        try {
	            // Intenta crear el archivo de configuración por defecto
	            crearConfigPorDefecto(configFile);
	            // Luego, lee el archivo que acabamos de crear
	            loadedConfig = leerArchivoConfigExistente(configFile);
	        } catch (IOException e) {
	            // Si falla la creación o la lectura posterior, es un error crítico.
	            // Usamos el mapa de defaults puro como último recurso.
	            System.err.println("¡FALLO CRÍTICO al crear/leer config! Usando defaults internos en memoria.");
	            loadedConfig = new HashMap<>(DEFAULT_CONFIG); 
	        }
	    } else {
	        // Si el archivo ya existe, intentamos leerlo.
	        try {
	            loadedConfig = leerArchivoConfigExistente(configFile);
	        } catch (IOException e) {
	            // Si falla la lectura de un archivo existente, es un error crítico.
	            // Usamos el mapa de defaults puro como último recurso.
	            System.err.println("¡FALLO CRÍTICO al leer config existente! Usando defaults internos en memoria. Error: " + e.getMessage());
	            loadedConfig = new HashMap<>(DEFAULT_CONFIG);
	        }
	    }

	 // FIXME --- ¡NUEVO BLOQUE DE DEBUG! ---
	    System.out.println("--- DEBUG PRE-SINCRONIZACIÓN ---");
	    System.out.println("Tamaño de DEFAULT_CONFIG: " + DEFAULT_CONFIG.size());
	    System.out.println("Tamaño de loadedConfig (del archivo): " + loadedConfig.size());
	    System.out.println("DEFAULT_CONFIG contiene 'salto_bloque'?: " + DEFAULT_CONFIG.containsKey(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE));
	    System.out.println("loadedConfig contiene 'salto_bloque'?: " + loadedConfig.containsKey(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE));
	    System.out.println("---------------------------------");
	 // --- FIN NUEVO BLOQUE DE DEBUG ---
	    
	    
	    // --- FASE 2: SINCRONIZAR LA CONFIGURACIÓN CARGADA CON LOS VALORES POR DEFECTO ---
	    //    Este bloque asegura que cualquier clave nueva definida en DEFAULT_CONFIG
	    //    que no esté en el archivo .cfg se añada al mapa en memoria.
	    System.out.println("Sincronizando configuración cargada con mapa de defaults...");
	    int clavesAñadidas = 0;
	    
	    // Iteramos sobre cada entrada del mapa de configuración por defecto.
	    for (Map.Entry<String, String> defaultEntry : DEFAULT_CONFIG.entrySet()) {
	        
	        // El método putIfAbsent(clave, valor) hace lo siguiente:
	        // 1. Comprueba si 'loadedConfig' contiene 'defaultEntry.getKey()'.
	        // 2. Si NO la contiene, añade la clave y su valor a 'loadedConfig' y devuelve null.
	        // 3. Si SÍ la contiene, no hace NADA y devuelve el valor que ya existía.
	        if (loadedConfig.putIfAbsent(defaultEntry.getKey(), defaultEntry.getValue()) == null) {
	            // Entramos aquí solo si la clave no existía y fue añadida.
	            System.out.println("  -> Clave por defecto faltante añadida a la config en memoria: '" + defaultEntry.getKey() + "'");
	            clavesAñadidas++;
	        }
	    }

	    if (clavesAñadidas > 0) {
	        System.out.println("Se añadieron " + clavesAñadidas + " claves por defecto que no estaban en el archivo config.cfg.");
	    } else {
	        System.out.println("La configuración cargada ya contenía todas las claves por defecto. No se añadieron nuevas.");
	    }
	    // -------------------------------------------------------------------------------------

	    // Devolvemos el mapa 'loadedConfig', que ahora está completo y sincronizado.
	    return loadedConfig;
	}
	

	private Map<String, String> leerArchivoConfigExistente (File configFile) throws IOException
    {
        Map<String, String> loadedConfig = new HashMap<>();
        System.out.println("Leyendo configuración desde: " + configFile.getAbsolutePath());

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

//                        // --- LOG log DETALLADO leerArchivoConfigExistente ---
//                        System.out.println("  Línea " + lineNumber 
//                        		+ ": [" + lineaOriginal + "] -> Clave: '" + clave + "', Valor: '" + valor + "'");
//                        // ---------------------

                        if (!clave.isEmpty())
                        {
                            loadedConfig.put(clave, valor);
                        } else { System.err.println("WARN: Clave vacía en línea " + lineNumber); }
                    } else { System.err.println("WARN: Línea mal formada en " + lineNumber + ": " + lineaOriginal); }
                } else {
                	
                	// LOG Loguear líneas de comentario/vacías si quieres ver todo
                     // Opcional: Loguear líneas de comentario/vacías si quieres ver todo
                     // System.out.println("  Línea " + lineNumber + ": Ignorada (comentario/vacía)");
                	
                }
            }
        } catch (IOException e) { /* ... */ throw e; } // Asegúrate que el throw esté fuera del try-catch interno si lo hubiera

        System.out.println("Configuración leída. Total claves: " + loadedConfig.size());
        return loadedConfig;
    }
	
	
	public void guardarConfiguracion (Map<String, String> configAGuardar) throws IOException
	{

		System.out.println("!!! DEBUG: Guardando mapa. HashCode del mapa recibido: " + System.identityHashCode(configAGuardar));
		
		// LOG DETALLADO: Inicio y verificación del valor a guardar ---
        System.out.println("\n[ConfigurationManager guardarConfiguracion] === INICIO GUARDADO ===");
        System.out.println("[ConfigurationManager guardarConfiguracion] Intentando guardar en: " + CONFIG_FILE_PATH);
        String temaEnMapaAGuardar = configAGuardar.get(ConfigKeys.TEMA_NOMBRE);
        System.out.println("[ConfigurationManager guardarConfiguracion] Valor de '" + ConfigKeys.TEMA_NOMBRE + "' que se intentará guardar: '" + temaEnMapaAGuardar + "'");
        if (temaEnMapaAGuardar == null) {
             System.err.println("  --> ¡¡ADVERTENCIA!! La clave '" + ConfigKeys.TEMA_NOMBRE + "' NO está en el mapa que se va a guardar.");
        }
        
        
     // --- AÑADE ESTE BLOQUE DE DEBUG AQUÍ MISMO ---
        System.out.println("--- DEBUG: Contenido del mapa a guardar ---");
        boolean encontrado = false;
        for (String clave : configAGuardar.keySet()) {
            if (clave.equals(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE)) {
                System.out.println("  -> ¡ENCONTRADO! " + clave + " = " + configAGuardar.get(clave));
                encontrado = true;
            }
        }
        if (!encontrado) {
            System.err.println("  -> ¡ERROR DE DEBUG! La clave '" + ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE + "' NO se encontró en el mapa a guardar.");
        }
        System.out.println("------------------------------------------");
        // --- FIN DEL BLOQUE DE DEBUG ---
        //------------------------------------
		
		File configFile = new File(CONFIG_FILE_PATH);
		File tempFile = new File(CONFIG_FILE_PATH + ".tmp"); // Archivo temporal

		// Crear una copia del mapa para poder eliminar claves mientras se procesan
		Map<String, String> configPendiente = new HashMap<>(configAGuardar);

		//LOG Guardando configuración (preservando estructura) en
		System.out.println("Guardando configuración (preservando estructura) en " + CONFIG_FILE_PATH);
		System.out.println("Claves a guardar/actualizar: " + configPendiente.size());

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
				System.out.println("Añadiendo " + configPendiente.size() + " claves nuevas:");

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
					System.out.println("  Nuevo: " + newKey + " = " + newValue);
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
				System.err.println("Advertencia: No se pudo borrar el archivo de configuración original: "
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
			System.out.println("Archivo de configuración actualizado exitosamente.");

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
	    System.out.println("Creando config por defecto (estructura jerárquica v2.1)...");
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
	     System.out.println("Archivo de configuración por defecto creado en: " + configFile.getAbsolutePath());
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
	    defaults.put(ConfigKeys.INICIO_CARPETA, inicioCarpetaDefault + "\\resources");
	    defaults.put(ConfigKeys.INICIO_IMAGEN, "");

	    defaults.put(ConfigKeys.PROYECTOS_CARPETA_BASE, inicioCarpetaDefault + "\\.proyectos");
	    defaults.put(ConfigKeys.PROYECTOS_ARCHIVO_TEMPORAL, "seleccion_actual.txt");
	    
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
	    defaults.put(ConfigKeys.WINDOW_MAXIMIZED, "false");

	    defaults.put(ConfigKeys.TEMA_NOMBRE, "clear");
	    defaults.put("iconos.ancho", "24");
	    defaults.put("iconos.alto", "24");
	    
	    defaults.put("ui.splitpane.main.dividerLocation", "0.25");
	    
	    defaults.put(ConfigKeys.CAROUSEL_DELAY_MS, "3000"); // 3 segundos por defecto
        defaults.put(ConfigKeys.COMPORTAMIENTO_SYNC_VISOR_CARRUSEL, "false"); // Sync desactivado por defecto
        defaults.put(ConfigKeys.CARRUSEL_ESTADO_ULTIMA_CARPETA, ""); // Sin carpeta guardada por defecto
        defaults.put(ConfigKeys.CARRUSEL_ESTADO_ULTIMA_IMAGEN, ""); // Sin imagen guardada por defecto

	    // --- 2. CONFIGURACIÓN DE MINIATURAS ---
	    defaults.put(ConfigKeys.MINIATURAS_CANTIDAD_ANTES, "8");
	    defaults.put(ConfigKeys.MINIATURAS_CANTIDAD_DESPUES, "15");
	    defaults.put(ConfigKeys.MINIATURAS_TAMANO_SEL_ANCHO, "60");
	    defaults.put(ConfigKeys.MINIATURAS_TAMANO_SEL_ALTO, "60");
	    defaults.put(ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, "40");
	    defaults.put(ConfigKeys.MINIATURAS_TAMANO_NORM_ALTO, "40");
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

		
		
//		//Inicio
//		String inicioCarpetaDefault = "D:\\Programacion\\Eclipse\\Workspace 2024-12R\\VisorImagenes";
//		String inicioImagenDefault = "";//"pandoras pedestals sagas.png";
//		
////			defaults.put("inicio.carpeta", inicioCarpetaDefault+ "\\resources");
//			defaults.put(ConfigKeys.INICIO_CARPETA, inicioCarpetaDefault + "\\resources");
//			defaults.put("inicio.imagen", inicioImagenDefault);
//			
//			// --- INICIO ---
//		    defaults.put(ConfigKeys.INICIO_CARPETA, inicioCarpetaDefault + "\\resources");
//		    defaults.put(ConfigKeys.INICIO_IMAGEN, "");
//
//		    // --- PROYECTOS ---
//		    defaults.put(ConfigKeys.PROYECTOS_CARPETA_BASE, inicioCarpetaDefault + "\\.proyectos");
//		    defaults.put(ConfigKeys.PROYECTOS_ARCHIVO_TEMPORAL, "seleccion_actual.txt");
//
//		    // --- COMPORTAMIENTO ---
//		    defaults.put(ConfigKeys.COMPORTAMIENTO_ZOOM_MANUAL_INICIAL, "true");
//		    defaults.put(ConfigKeys.COMPORTAMIENTO_ZOOM_MODO_INICIAL, "FIT_TO_SCREEN");
//		    defaults.put(ConfigKeys.COMPORTAMIENTO_ZOOM_ULTIMO_MODO, "FIT_TO_SCREEN");
//		    defaults.put(ConfigKeys.COMPORTAMIENTO_ZOOM_PORCENTAJE_PERSONALIZADO, "100.0");
//		    defaults.put(ConfigKeys.COMPORTAMIENTO_NAVEGACION_CIRCULAR, "false");
//		    defaults.put(ConfigKeys.COMPORTAMIENTO_CARGAR_SUBCARPETAS, "true");
//		    defaults.put(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE, "10");
//
//		    // --- VENTANA ---
//		    defaults.put(ConfigKeys.WINDOW_X, "0");
//		    defaults.put(ConfigKeys.WINDOW_Y, "0");
//		    defaults.put(ConfigKeys.WINDOW_WIDTH, "1550");
//		    defaults.put(ConfigKeys.WINDOW_HEIGHT, "500");
//		    defaults.put(ConfigKeys.WINDOW_MAXIMIZED, "false");
//
//		    // --- TEMA E ICONOS ---
//		    defaults.put(ConfigKeys.TEMA_NOMBRE, "clear");
//		    defaults.put("iconos.alto", "24");
//		    defaults.put("iconos.ancho", "24");
//
//		    // --- BARRAS DE INFO ---
//		    defaults.put(ConfigKeys.INFOBAR_SUP_VISIBLE, "true");
//		    defaults.put(ConfigKeys.INFOBAR_INF_VISIBLE, "true");
//		    // ... (todas las demás claves de INFOBAR que ya tenías)
//		    defaults.put(ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_VISIBLE, "true");
//		    defaults.put(ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_FORMATO, "solo_nombre");
//		    defaults.put(ConfigKeys.INFOBAR_SUP_INDICE_TOTAL_VISIBLE, "true");
//		    // ... etc.
//
//		    // --- MINIATURAS ---
//		    defaults.put(ConfigKeys.MINIATURAS_CANTIDAD_ANTES, "8");
//		    defaults.put(ConfigKeys.MINIATURAS_CANTIDAD_DESPUES, "8");
//		    defaults.put(ConfigKeys.MINIATURAS_TAMANO_SEL_ANCHO, "60");
//		    // ... etc.
//
//		    // --- ESTADOS DE MENÚS Y BOTONES ---
//		    // ¡ESTA ES LA PARTE IMPORTANTE!
//		    // Usamos UIDefinitionService para generar dinámicamente los defaults, asegurando consistencia.
//		    UIDefinitionService uiDefs = new UIDefinitionService();
//
//		    // 1. Defaults para la visibilidad de las BARRAS DE HERRAMIENTAS
//		    for (String toolbarKey : uiDefs.getToolbarKeys()) {
//		        // Genera la clave COMPLETA, ej: "interfaz.herramientas.edicion.visible"
//		        defaults.put(ConfigKeys.toolbarVisible(toolbarKey), "true");
//		    }
//
//		    // 2. Defaults para la visibilidad de los BOTONES INDIVIDUALES
//		    uiDefs.generateModularToolbarStructure().forEach(toolbarDef -> {
//		        toolbarDef.botones().forEach(buttonDef -> {
//		            String buttonKeyBase = ConfigKeys.buildKey(
//		                "interfaz.boton", 
//		                toolbarDef.claveBarra(), 
//		                ConfigKeys.normalizePart(buttonDef.comandoCanonico())
//		            );
//		            defaults.put(buttonKeyBase + ".visible", "true");
//		            defaults.put(buttonKeyBase + ".activado", "true"); // O el valor que corresponda
//		        });
//		    });
//
//		    // 3. Defaults para el estado de los CHECKBOXES en el menú
//		    //    Esto es más complejo, pero podemos basarnos en lo anterior.
//		    //    Por simplicidad, podemos omitir esto por ahora, ya que la `Action`
//		    //    lo inicializará basándose en el estado del botón.
//
//		    // ... (Aquí podrías añadir manualmente los defaults de otros menús si lo necesitas)
//		    defaults.put("interfaz.menu.vista.barra_de_menu.seleccionado", "true");
//		    defaults.put("interfaz.menu.vista.fondo_a_cuadros.seleccionado", "false");
//		    
//		    
////		//carpeta inicial de proyectos
////			defaults.put(ConfigKeys.PROYECTOS_CARPETA_BASE, inicioCarpetaDefault + "\\.proyectos");
////			defaults.put(ConfigKeys.PROYECTOS_ARCHIVO_TEMPORAL, "seleccion_actual.txt");
////			// defaults.put(KEY_PROYECTOS_ULTIMO_ABIERTO, ""); // Vacío por defecto
////			// defaults.put(KEY_PROYECTOS_INICIO_ACCION, "nuevo_temporal");
////		
////		// Comportamiento
////			defaults.put(ConfigKeys.COMPORTAMIENTO_ZOOM_MANUAL_INICIAL, "true");
////			defaults.put(ConfigKeys.COMPORTAMIENTO_ZOOM_MODO_INICIAL, "FIT_TO_SCREEN");
////			defaults.put(ConfigKeys.COMPORTAMIENTO_ZOOM_ULTIMO_MODO, "FIT_TO_SCREEN");
////			defaults.put(ConfigKeys.COMPORTAMIENTO_ZOOM_PORCENTAJE_PERSONALIZADO, "100.0");
////			defaults.put(ConfigKeys.COMPORTAMIENTO_NAVEGACION_CIRCULAR, "false");
////			defaults.put(ConfigKeys.COMPORTAMIENTO_CARGAR_SUBCARPETAS, "true");
////			
////			defaults.put("interfaz.menu.configuracion.comportamiento_general.mostrar_flechas_de_navegacion_en_imagen.seleccionado", "false");
////
////		// Ventana de la aplicacion
////	        // Usar -1 para indicar que no hay posición/tamaño guardado (usar defaults del sistema/pack)
////	        defaults.put(ConfigKeys.WINDOW_X, "0");
////	        defaults.put(ConfigKeys.WINDOW_Y, "0");
////	        defaults.put(ConfigKeys.WINDOW_WIDTH, "1550");  // O un tamaño default razonable si prefieres
////	        defaults.put(ConfigKeys.WINDOW_HEIGHT, "500"); // Ej: defaults.put(KEY_WINDOW_WIDTH, "1200");
////	        defaults.put(ConfigKeys.WINDOW_MAXIMIZED, "false"); // Por defecto no empieza maximizada
////		
////	     // --- Visibilidad y Formato Barras de Información ---
////	        defaults.put(ConfigKeys.INFOBAR_SUP_VISIBLE, "true");
////	        defaults.put(ConfigKeys.INFOBAR_INF_VISIBLE, "true");
////
////	        defaults.put(ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_VISIBLE, "true");
////	        defaults.put(ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_FORMATO, "solo_nombre");
////	        defaults.put(ConfigKeys.INFOBAR_SUP_INDICE_TOTAL_VISIBLE, "true");
////	        defaults.put(ConfigKeys.INFOBAR_SUP_DIMENSIONES_VISIBLE, "true");
////	        defaults.put(ConfigKeys.INFOBAR_SUP_TAMANO_ARCHIVO_VISIBLE, "true");
////	        defaults.put(ConfigKeys.INFOBAR_SUP_FECHA_ARCHIVO_VISIBLE, "true");
////	        defaults.put(ConfigKeys.INFOBAR_SUP_FORMATO_IMAGEN_VISIBLE, "false"); // Desactivado por defecto
////	        defaults.put(ConfigKeys.INFOBAR_SUP_MODO_ZOOM_VISIBLE, "true");
////	        defaults.put(ConfigKeys.INFOBAR_SUP_ZOOM_REAL_PCT_VISIBLE, "true");
////
////	        defaults.put(ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_VISIBLE, "true");
////	        defaults.put(ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_FORMATO, "ruta_completa");
////	        defaults.put(ConfigKeys.INFOBAR_INF_ICONO_ZM_VISIBLE, "true");
////	        defaults.put(ConfigKeys.INFOBAR_INF_ICONO_PROP_VISIBLE, "true");
////	        defaults.put(ConfigKeys.INFOBAR_INF_ICONO_SUBC_VISIBLE, "true");
////	        defaults.put(ConfigKeys.INFOBAR_INF_CTRL_ZOOM_PCT_VISIBLE, "true");
////	        defaults.put(ConfigKeys.INFOBAR_INF_CTRL_MODO_ZOOM_VISIBLE, "true");
////	        defaults.put(ConfigKeys.INFOBAR_INF_MENSAJES_APP_VISIBLE, "true");
////	        
////		//===== Personalizacion =====
////		//Tema
////			defaults.put(ConfigKeys.TEMA_NOMBRE, "clear");
////		
////		//TODO Iconos
////			defaults.put("iconos.alto" , "24");
////			defaults.put("iconos.ancho" , "24");
////		
////		//Tamaño del salto con pgup y pgdown
//////			defaults.put("navegacion.tamano_salto_bloque", "10");
////			defaults.put(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE , "10");
////			
////    	//===== Barra de Miniaturas =====
////    	//Cantidad de miniaturas antes/después de la seleccionada
////			defaults.put(ConfigKeys.MINIATURAS_CANTIDAD_ANTES, "8");
////			defaults.put(ConfigKeys.MINIATURAS_CANTIDAD_DESPUES, "8");
////
////    	//Tamaño (píxeles) para miniaturas seleccionadas
////			defaults.put(ConfigKeys.MINIATURAS_TAMANO_SEL_ANCHO, "60");
////			defaults.put(ConfigKeys.MINIATURAS_TAMANO_SEL_ALTO, "60");
////
////    	//Tamaño (píxeles) para miniaturas normales
////			defaults.put(ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, "40");
////			defaults.put(ConfigKeys.MINIATURAS_TAMANO_NORM_ALTO, "40");
////
////		//Mostrar Texto de las miniaturas on off
////			
////    	//===== Estados Interfaz Usuario =====
////		
////    	//--- Botones ---
////    	//# Formato: interfaz.boton.<ActionCommand>.{activado|visible} = {true|false}\n\n");
////
////			defaults.put("interfaz.boton.control.especial_refrescar.activado", "true");
////			defaults.put("interfaz.boton.control.especial_refrescar.visible", "true");
////			defaults.put("interfaz.boton.control.imagen_eliminar.activado", "true");
////			defaults.put("interfaz.boton.control.imagen_eliminar.visible", "true");
////			defaults.put("interfaz.boton.control.imagen_localizar.activado", "true");
////			defaults.put("interfaz.boton.control.imagen_localizar.visible", "true");
////			
////			defaults.put("interfaz.boton.edicion.imagen_recortar.activado", "true");
////			defaults.put("interfaz.boton.edicion.imagen_recortar.visible", "true");
////			defaults.put("interfaz.boton.edicion.imagen_rotar_der.activado", "true");
////			defaults.put("interfaz.boton.edicion.imagen_rotar_der.visible", "true");
////			defaults.put("interfaz.boton.edicion.imagen_rotar_izq.activado", "true");
////			defaults.put("interfaz.boton.edicion.imagen_rotar_izq.visible", "true");
////			defaults.put("interfaz.boton.edicion.imagen_voltear_h.activado", "true");
////			defaults.put("interfaz.boton.edicion.imagen_voltear_h.visible", "true");
////			defaults.put("interfaz.boton.edicion.imagen_voltear_v.activado", "true");
////			defaults.put("interfaz.boton.edicion.imagen_voltear_v.visible", "true");
////			
////			defaults.put("interfaz.boton.especiales.archivo_abrir.activado", "true");
////			defaults.put("interfaz.boton.especiales.archivo_abrir.visible", "true");
////			defaults.put("interfaz.boton.especiales.especial_botones_ocultos.activado", "true");
////			defaults.put("interfaz.boton.especiales.especial_botones_ocultos.visible", "true");
////			defaults.put("interfaz.boton.especiales.especial_menu.activado", "true");
////			defaults.put("interfaz.boton.especiales.especial_menu.visible", "true");
////			
////			defaults.put("interfaz.boton.movimiento.nav_anterior.activado", "false");
////			defaults.put("interfaz.boton.movimiento.nav_anterior.visible", "true");
////			defaults.put("interfaz.boton.movimiento.nav_primera.activado", "false");
////			defaults.put("interfaz.boton.movimiento.nav_primera.visible", "true");
////			defaults.put("interfaz.boton.movimiento.nav_siguiente.activado", "true");
////			defaults.put("interfaz.boton.movimiento.nav_siguiente.visible", "true");
////			defaults.put("interfaz.boton.movimiento.nav_ultima.activado", "true");
////			defaults.put("interfaz.boton.movimiento.nav_ultima.visible", "true");
////			
////			defaults.put("interfaz.boton.proyecto.proyecto_gestionar.activado", "true");
////			defaults.put("interfaz.boton.proyecto.proyecto_gestionar.visible", "true");
////			defaults.put("interfaz.boton.proyecto.proyecto_toggle_marca.activado", "true");
////			defaults.put("interfaz.boton.proyecto.proyecto_toggle_marca.visible", "true");
////			
////			defaults.put("interfaz.boton.toggle.mantener_proporciones.activado", "true");
////			defaults.put("interfaz.boton.toggle.mantener_proporciones.visible", "true");
////			defaults.put("interfaz.boton.toggle.subcarpetas.activado", "true");
////			defaults.put("interfaz.boton.toggle.subcarpetas.visible", "true");
////			
////			defaults.put("interfaz.boton.vista.todo_funcionalidad_pendiente.activado", "true");
////			defaults.put("interfaz.boton.vista.todo_funcionalidad_pendiente.visible", "true");
////			defaults.put("interfaz.boton.vista.vista_mostrar_dialogo_lista.activado", "true");
////			defaults.put("interfaz.boton.vista.vista_mostrar_dialogo_lista.visible", "true");
////			
////			defaults.put("interfaz.boton.zoom.zoom_manual_toggle.activado", "true");
////			defaults.put("interfaz.boton.zoom.zoom_manual_toggle.visible", "true");
////			defaults.put("interfaz.boton.zoom.zoom_reset.activado", "true");
////			defaults.put("interfaz.boton.zoom.zoom_reset.visible", "true");
////			defaults.put("interfaz.boton.zoom.zoom_tipo_ajustar.activado", "true");
////			defaults.put("interfaz.boton.zoom.zoom_tipo_ajustar.visible", "true");
////			defaults.put("interfaz.boton.zoom.zoom_tipo_alto.activado", "true");
////			defaults.put("interfaz.boton.zoom.zoom_tipo_alto.visible", "true");
////			defaults.put("interfaz.boton.zoom.zoom_tipo_ancho.activado", "true");
////			defaults.put("interfaz.boton.zoom.zoom_tipo_ancho.visible", "true");
////			defaults.put("interfaz.boton.zoom.zoom_tipo_auto.activado", "true");
////			defaults.put("interfaz.boton.zoom.zoom_tipo_auto.visible", "true");
////			defaults.put("interfaz.boton.zoom.zoom_tipo_especificado.activado", "true");
////			defaults.put("interfaz.boton.zoom.zoom_tipo_especificado.visible", "true");
////			defaults.put("interfaz.boton.zoom.zoom_tipo_fijo.activado", "true");
////			defaults.put("interfaz.boton.zoom.zoom_tipo_fijo.visible", "true");
////			
////			
////			defaults.put("interfaz.herramientas.apoyo.visible", "true");
////			defaults.put("interfaz.herramientas.edicion.visible", "true");
////			defaults.put("interfaz.herramientas.navegacion.visible", "true");
////			defaults.put("interfaz.herramientas.proyecto.visible", "true");
////			defaults.put("interfaz.herramientas.toggle.visible", "true");
////			defaults.put("interfaz.herramientas.utils.visible", "true");
////			defaults.put("interfaz.herramientas.vistas.visible", "true");
////			defaults.put("interfaz.herramientas.zoom.visible", "true");
////			
////			
////    	//--- Menú ---\n");
////    	//Formato: interfaz.menu.<ActionCommand>.{activado|visible|seleccionado} = {true|false}\n");
////    	//Nota: .seleccionado solo aplica a CheckBox y RadioButton\n\n");
////
////			defaults.put("interfaz.menu.configuracion.carga_de_imagenes.mostrar_imagenes_de_subcarpetas.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.carga_de_imagenes.mostrar_solo_carpeta_actual.seleccionado", "false");
////			defaults.put("interfaz.menu.configuracion.carga_de_imagenes.orden_visual.ascendente.seleccionado", "false");
////			defaults.put("interfaz.menu.configuracion.carga_de_imagenes.orden_visual.descendente.seleccionado", "false");
////			defaults.put("interfaz.menu.configuracion.carga_de_imagenes.orden_visual.extension.seleccionado", "false");
////			defaults.put("interfaz.menu.configuracion.carga_de_imagenes.orden_visual.fecha_de_creacion.seleccionado", "false");
////			defaults.put("interfaz.menu.configuracion.carga_de_imagenes.orden_visual.nombre_por_defecto.seleccionado", "false");
////			defaults.put("interfaz.menu.configuracion.carga_de_imagenes.orden_visual.sin_ordenar.seleccionado", "false");
////			defaults.put("interfaz.menu.configuracion.carga_de_imagenes.orden_visual.tamao_de_archivo.seleccionado", "false");
////			
////			defaults.put("interfaz.menu.configuracion.comportamiento_general.abrir_ultima_imagen_vista_al_iniciar.seleccionado", "false");
////			defaults.put("interfaz.menu.configuracion.comportamiento_general.mostrar_imagen_de_bienvenida.seleccionado", "false");
////			defaults.put("interfaz.menu.configuracion.comportamiento_general.navegacion_circular_wrap_around.seleccionado", "false");
////			
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_apoyo.abrir_carpeta.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_apoyo.menu_principal.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_apoyo.mostrar_botones_ocultos.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_edicion.recortar.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_edicion.rotar_derecha.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_edicion.rotar_izquierda.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_edicion.voltear_horizontal.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_edicion.voltear_vertical.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_navegacion.imagen_anterior.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_navegacion.imagen_siguiente.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_navegacion.ltima_imagen.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_navegacion.primera_imagen.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_proyecto.marcar_imagen_para_proyecto.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_proyecto.mostrar_ocultar_favoritos.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_toggles.incluir_excluir_subcarpetas.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_toggles.mantener_proporciones.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_utilidades.abrir_ubicacion.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_utilidades.eliminar_imagen.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_utilidades.refrescar.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_vistas.panel_galeria.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_vistas.pantalla_completa.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_vistas.vista_carrusel.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_vistas.vista_grid.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_vistas.vista_lista.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_zoom.activar_desactivar_zoom_manual.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_zoom.ajustar_al_alto.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_zoom.ajustar_al_ancho.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_zoom.escalar_para_ajustar.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_zoom.resetear_zoom.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_zoom.zoom_actual_fijo.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_zoom.zoom_automatico.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.herramientas.barra_de_zoom.zoom_especificado.seleccionado", "true");
////			
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.estado_y_control.area_de_mensajes.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.estado_y_control.control_modo_zoom.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.estado_y_control.control_porc_zoom.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.estado_y_control.formato.ruta_completa_y_nombre.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.estado_y_control.formato.solo_nombre_de_archivo.seleccionado", "false");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.estado_y_control.icono_proporciones.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.estado_y_control.icono_subcarpetas.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.estado_y_control.icono_zoom_manual.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.estado_y_control.mostrar_panel_de_control.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.estado_y_control.nombre_ruta_archivo.seleccionado", "true");
////			
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.informacion_de_imagen.dimensiones_originales.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.informacion_de_imagen.fecha_de_archivo.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.informacion_de_imagen.formato.ruta_completa_y_nombre.seleccionado", "false");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.informacion_de_imagen.formato.solo_nombre_de_archivo.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.informacion_de_imagen.formato_de_imagen.seleccionado", "false");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.informacion_de_imagen.modo_de_zoom.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.informacion_de_imagen.mostrar_panel_de_info.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.informacion_de_imagen.ndice_total_imagenes.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.informacion_de_imagen.nombre_ruta_archivo.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.informacion_de_imagen.porc_zoom_real.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.paneles_de_datos.informacion_de_imagen.tamao_de_archivo.seleccionado", "true");
////			
////			defaults.put("interfaz.menu.configuracion.tema.tema_blue.seleccionado", "false");
////			defaults.put("interfaz.menu.configuracion.tema.tema_clear.seleccionado", "true");
////			defaults.put("interfaz.menu.configuracion.tema.tema_dark.seleccionado", "false");
////			defaults.put("interfaz.menu.configuracion.tema.tema_green.seleccionado", "false");
////			defaults.put("interfaz.menu.configuracion.tema.tema_orange.seleccionado", "false");
////			
////			defaults.put("interfaz.menu.proyecto.marcar_para_proyecto.seleccionado", "false");
////			
////			defaults.put("interfaz.menu.vista.barra_de_botones.seleccionado", "true");
////			defaults.put("interfaz.menu.vista.barra_de_menu.seleccionado", "true");
////			defaults.put("interfaz.menu.vista.barra_de_miniaturas.seleccionado", "true");
////			defaults.put("interfaz.menu.vista.fondo_a_cuadros.seleccionado", "false");
////			defaults.put("interfaz.menu.vista.imagenes_en_miniatura.seleccionado", "true");
////			defaults.put("interfaz.menu.vista.lista_de_archivos.seleccionado", "true");
////			defaults.put("interfaz.menu.vista.mantener_ventana_siempre_encima.seleccionado", "false");
////			defaults.put("interfaz.menu.vista.mostrar_nombres_en_miniaturas.seleccionado", "true");
////			defaults.put("interfaz.menu.vista.mostrar_ocultar_la_lista_de_archivos.seleccionado", "true");
////			
////			defaults.put("interfaz.menu.zoom.activar_zoom_manual.seleccionado", "true");
////			defaults.put("interfaz.menu.zoom.ajuste_visual.ajustar_a_alto.seleccionado", "false");
////			defaults.put("interfaz.menu.zoom.ajuste_visual.ajustar_a_ancho.seleccionado", "false");
////			defaults.put("interfaz.menu.zoom.ajuste_visual.ajustar_a_pantalla.seleccionado", "true");
////			defaults.put("interfaz.menu.zoom.ajuste_visual.mantener_zoom_actual.seleccionado", "false");
////			defaults.put("interfaz.menu.zoom.ajuste_visual.tamao_original_100porc.seleccionado", "false");
////			defaults.put("interfaz.menu.zoom.ajuste_visual.zoom_personalizado_porc.seleccionado", "false");
////			defaults.put("interfaz.menu.zoom.mantener_proporciones.seleccionado", "true");
////			
////			
////		//# --- Tema Claro (Basado en Swing Defaults) ---
////		defaults.put("colores.claro.fondo.principal",  		"238, 238, 238");
////		defaults.put("colores.claro.fondo.secundario",  	"255, 255, 255");
////		defaults.put("colores.claro.texto.primario",  		"0, 0, 0      ");
////		defaults.put("colores.claro.texto.secundario",  	"80, 80, 80   ");
////		defaults.put("colores.claro.borde.color",  			"184, 207, 229");
////		defaults.put("colores.claro.borde.titulo",  		"0, 0, 0      ");
////		defaults.put("colores.claro.seleccion.fondo",  		"57, 105, 138 ");
////		defaults.put("colores.claro.seleccion.texto",  		"255, 255, 255");
////		defaults.put("colores.claro.boton.fondo",  			"238, 238, 238");
////		defaults.put("colores.claro.boton.texto",  			"0, 0, 0      ");
////		defaults.put("colores.claro.boton.fondoActivado",  	"84, 144, 164 ");
////		defaults.put("colores.claro.boton.fondoAnimacion",  "173, 216, 230");
//        
//		return defaults;
//	} // FIN del metodo createDefaultConfigMap ---

	
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
			System.err.println(
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
            System.out.println("  [ConfigManager] Propiedad eliminada de la configuración en memoria: " + key);
        } else {
            System.err.println(
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
            System.err.println("WARN [ConfigurationManager]: Intento de establecer un tema nulo o vacío. No se guardará.");
            return;
        }
    
        // Podrías añadir una validación aquí para asegurar que nuevoTema es uno de los soportados
        // List<String> temasValidos = List.of("clear", "dark", "blue", "green", "orange");
        // if (!temasValidos.contains(nuevoTema.toLowerCase())) {
        //     System.err.println("WARN [ConfigurationManager]: Tema '" + nuevoTema + "' no reconocido. No se guardará.");
        //     return;
        // }

        // Actualiza el valor en el mapa 'config' en memoria
        setString(ConfigKeys.TEMA_NOMBRE, nuevoTema.trim()); // setString ya imprime un log

        //LOG [ConfigurationManager setTemaActualYGuardar] Valor de: 
        String valorEnMemoria = config.get(ConfigKeys.TEMA_NOMBRE);
        System.out.println("[ConfigurationManager setTemaActualYGuardar] Valor de '" + ConfigKeys.TEMA_NOMBRE + "' en memoria ANTES de guardar: '" + valorEnMemoria + "'");
        if (!nuevoTema.trim().equals(valorEnMemoria)) {
             System.err.println("  --> ¡¡ADVERTENCIA!! El valor en memoria no coincide con el solicitado después de setString.");
        }
        //-----------------------
        
        // Guarda el mapa 'config' completo (que ahora incluye el nuevo tema) en el archivo
        try {
            guardarConfiguracion(this.config); // Usa el método existente para guardar
            System.out.println("[ConfigurationManager]: Tema cambiado a '" + nuevoTema + "' y configuración guardada en " + CONFIG_FILE_PATH);
            // Necesitarás lógica adicional en tu UI (posiblemente reiniciar o recargar elementos)
        } catch (IOException e) {
            System.err.println("ERROR CRÍTICO [ConfigurationManager]: No se pudo guardar la configuración después de cambiar el tema a '" + nuevoTema + "'. Error: " + e.getMessage());
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
            System.out.println("WARN: Clave '" + key + "' no encontrada. Usando default '" + defaultValue + "' y añadiendo a config en memoria.");
            this.setString(key, String.valueOf(defaultValue)); // Autocorrección en memoria
            return defaultValue;
        }

        try {
            return Integer.parseInt(valueStr.trim());
        } catch (NumberFormatException e) {
            System.err.println("WARN: Valor inválido para la clave '" + key + "': '" + valueStr + "'. Usando default '" + defaultValue + "' y corrigiendo en memoria.");
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
            System.out.println("WARN: Clave '" + key + "' no encontrada. Usando default '" + defaultValue + "' y añadiendo a config en memoria.");
            this.setString(key, String.valueOf(defaultValue)); // Autocorrección en memoria
            return defaultValue;
        }

        String trimmedValue = valueStr.trim().toLowerCase();
        if (trimmedValue.equals("true")) {
            return true;
        } else if (trimmedValue.equals("false")) {
            return false;
        } else {
            System.err.println("WARN: Valor inválido para la clave '" + key + "': '" + valueStr + "'. Debe ser 'true' o 'false'. Usando default '" + defaultValue + "' y corrigiendo en memoria.");
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
            System.out.println("WARN: Clave '" + key + "' no encontrada. Usando default '" + defaultValue + "' y añadiendo a config en memoria.");
            this.setString(key, String.valueOf(defaultValue)); // Autocorrección en memoria
            return defaultValue;
        }

        try {
            // Reemplaza la coma por un punto para asegurar compatibilidad con Double.parseDouble
            return Double.parseDouble(valueStr.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            System.err.println("WARN: Valor inválido para la clave '" + key + "': '" + valueStr + "'. Usando default '" + defaultValue + "' y corrigiendo en memoria.");
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
        System.out.println("[ConfigurationManager] Reseteando configuración en memoria a los valores por defecto...");

        // Paso 1: Crear el nuevo mapa en una variable local para que no haya ambigüedad.
        Map<String, String> nuevoMapaDefaults = new HashMap<>(ConfigurationManager.DEFAULT_CONFIG);
        
        // Paso 2: Asignarlo al campo de la instancia.
        this.config = nuevoMapaDefaults;
        
        // Paso 3: Imprimir el tamaño.
        System.out.println("  -> Configuración en memoria reseteada. Total claves: " + this.config.size());
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
            System.out.println("  [ConfigManager] Propiedad eliminada (en memoria): " + key);
        }
    }
    
} //Fin configurationManager

