package servicios;

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

public class ConfigurationManager
{

	// variables de la clase
	private static final String CONFIG_FILE_PATH = "config.cfg";

//	private static final Map<String, String> DEFAULT_COMMENTS;

	private Map<String, String> config;

	private static final Map<String, String> DEFAULT_GROUP_COMMENTS;
	private static final Map<String, String> DEFAULT_CONFIG;
	
	public static final String KEY_TEMA_NOMBRE = "tema.nombre";
	
	// --- CONSTANTES PARA ESTADO VENTANA ---
    public static final String KEY_WINDOW_X = "window.x";
    public static final String KEY_WINDOW_Y = "window.y";
    public static final String KEY_WINDOW_WIDTH = "window.width";
    public static final String KEY_WINDOW_HEIGHT = "window.height";
    public static final String KEY_WINDOW_MAXIMIZED = "window.maximized";
	
	
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
	        "interfaz" // Ahora "interfaz" es la sección principal
	);
	
	public ConfigurationManager() throws IOException
	{

		config = cargarConfiguracion();
		
		//LOG ConfigurationManager inicializado.
		//System.out.println("ConfigurationManager inicializado.");

	}

	// Método para cargar toda la configuración
	public Map<String, String> cargarConfiguracion () throws IOException
	{

		File configFile = new File(CONFIG_FILE_PATH);

		if (!configFile.exists())
		{
			System.out.println("Config no encontrado. Creando por defecto...");

			try
			{
				crearConfigPorDefecto(configFile);
				// Leemos el archivo recién creado
				return leerArchivoConfigExistente(configFile);
			} catch (IOException e)
			{
				System.err.println("¡FALLO CRÍTICO al crear/leer config! Usando defaults internos.");
				// Devolver una COPIA del mapa de defaults como último recurso
				return new HashMap<>(DEFAULT_CONFIG);
				// O podrías lanzar una excepción si la config es obligatoria:
				// throw new IOException("No se pudo crear ni leer config, y los defaults
				// internos podrían no ser suficientes.", e);
			}
		} else
		{
			// Si existe, intentar leerlo
			try
			{
				return leerArchivoConfigExistente(configFile);
			} catch (IOException e)
			{
				System.err.println(
						"¡FALLO CRÍTICO al leer config existente! Usando defaults internos. Error: " + e.getMessage());
				// Devolver una COPIA del mapa de defaults como último recurso
				return new HashMap<>(DEFAULT_CONFIG);
				// O lanzar excepción
				// throw new IOException("Error al leer config existente, usando defaults
				// internos.", e);
			}
		}
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

		// LOG DETALLADO: Inicio y verificación del valor a guardar ---
        System.out.println("\n[ConfigurationManager guardarConfiguracion] === INICIO GUARDADO ===");
        System.out.println("[ConfigurationManager guardarConfiguracion] Intentando guardar en: " + CONFIG_FILE_PATH);
        String temaEnMapaAGuardar = configAGuardar.get(KEY_TEMA_NOMBRE);
        System.out.println("[ConfigurationManager guardarConfiguracion] Valor de '" + KEY_TEMA_NOMBRE + "' que se intentará guardar: '" + temaEnMapaAGuardar + "'");
        if (temaEnMapaAGuardar == null) {
             System.err.println("  --> ¡¡ADVERTENCIA!! La clave '" + KEY_TEMA_NOMBRE + "' NO está en el mapa que se va a guardar.");
        }
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
							
							//SYSO LOG actualizacion config
							//System.out.println("  Actualizado: " + key + " = " + newValue);
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

		String inicioCarpetaDefault = "D:\\Programacion\\Eclipse\\Workspace 2024-12R\\VisorImagenes\\resources";
		String inicioImagenDefault = "";//"pandoras pedestals sagas.png";

		//Inicio
			defaults.put("inicio.carpeta", inicioCarpetaDefault);
			defaults.put("inicio.imagen", inicioImagenDefault);
		
		// Comportamiento
			defaults.put("comportamiento.carpeta.cargarSubcarpetas", "true");
			defaults.put("comportamiento.carga.conRutas", "false");
			
		// Ventana de la aplicacion
	        // Usar -1 para indicar que no hay posición/tamaño guardado (usar defaults del sistema/pack)
	        defaults.put(KEY_WINDOW_X, "-1");
	        defaults.put(KEY_WINDOW_Y, "-1");
	        defaults.put(KEY_WINDOW_WIDTH, "-1");  // O un tamaño default razonable si prefieres
	        defaults.put(KEY_WINDOW_HEIGHT, "-1"); // Ej: defaults.put(KEY_WINDOW_WIDTH, "1200");
	        defaults.put(KEY_WINDOW_MAXIMIZED, "false"); // Por defecto no empieza maximizada
		
		//===== Personalizacion =====
		//Tema
			defaults.put(KEY_TEMA_NOMBRE, "clear");
		
		//TODO Iconos
			defaults.put("iconos.alto" , "24");
			defaults.put("iconos.ancho" , "24");
		
    	//===== Barra de Miniaturas =====
    	//Cantidad de miniaturas antes/después de la seleccionada
			defaults.put("miniaturas.cantidad.antes", "8");
			defaults.put("miniaturas.cantidad.despues", "8");

    	//Tamaño (píxeles) para miniaturas seleccionadas
			defaults.put("miniaturas.tamano.seleccionada.ancho", "60");
			defaults.put("miniaturas.tamano.seleccionada.alto", "60");

    	//Tamaño (píxeles) para miniaturas normales
			defaults.put("miniaturas.tamano.normal.ancho", "40");
			defaults.put("miniaturas.tamano.normal.alto", "40");

    	//===== Estados Interfaz Usuario =====
		
    	//--- Botones ---
    	//# Formato: interfaz.boton.<ActionCommand>.{activado|visible} = {true|false}\n\n");

		//toggle
			defaults.put("interfaz.boton.toggle.Mantener_Proporciones_48x48.activado", "true");
			defaults.put("interfaz.boton.toggle.Mantener_Proporciones_48x48.visible", "true");
			defaults.put("interfaz.boton.toggle.Mostrar_Favoritos_48x48.activado", "true");
			defaults.put("interfaz.boton.toggle.Mostrar_Favoritos_48x48.visible", "true");
			defaults.put("interfaz.boton.toggle.Subcarpetas_48x48.activado", "true");
			defaults.put("interfaz.boton.toggle.Subcarpetas_48x48.visible", "true");
			
			
    	//-- Navegación --
			defaults.put("interfaz.boton.movimiento.Primera_48x48.activado", "true");
			defaults.put("interfaz.boton.movimiento.Primera_48x48.visible", "true");
			defaults.put("interfaz.boton.movimiento.Anterior_48x48.activado", "true");
			defaults.put("interfaz.boton.movimiento.Anterior_48x48.visible", "true");
			defaults.put("interfaz.boton.movimiento.Siguiente_48x48.activado", "true");
			defaults.put("interfaz.boton.movimiento.Siguiente_48x48.visible", "true");
			defaults.put("interfaz.boton.movimiento.Ultima_48x48.activado", "true");
			defaults.put("interfaz.boton.movimiento.Ultima_48x48.visible", "true");
		
    	//-- Edición --
			defaults.put("interfaz.boton.edicion.Rotar_Izquierda_48x48.activado", "true");
			defaults.put("interfaz.boton.edicion.Rotar_Izquierda_48x48.visible", "true");
			defaults.put("interfaz.boton.edicion.Rotar_Derecha_48x48.activado", "true");
			defaults.put("interfaz.boton.edicion.Rotar_Derecha_48x48.visible", "true");
			defaults.put("interfaz.boton.edicion.Espejo_Horizontal_48x48.activado", "true");
			defaults.put("interfaz.boton.edicion.Espejo_Horizontal_48x48.visible", "true");
			defaults.put("interfaz.boton.edicion.Espejo_Vertical_48x48.activado", "true");
			defaults.put("interfaz.boton.edicion.Espejo_Vertical_48x48.visible", "true");
			defaults.put("interfaz.boton.edicion.Recortar_48x48.activado", "true");
			defaults.put("interfaz.boton.edicion.Recortar_48x48.visible", "true");

    	//-- Zoom --
			defaults.put("interfaz.boton.zoom.Zoom_48x48.activado", "true");	
			defaults.put("interfaz.boton.zoom.Zoom_48x48.visible", "true");
			defaults.put("interfaz.boton.zoom.Reset_48x48.activado", "false");	
			defaults.put("interfaz.boton.zoom.Reset_48x48.visible", "true");	
			defaults.put("interfaz.boton.zoom.Zoom_Auto_48x48.activado", "true");
			defaults.put("interfaz.boton.zoom.Zoom_Auto_48x48.visible", "true");
			defaults.put("interfaz.boton.zoom.Ajustar_al_Ancho_48x48.activado", "true");
			defaults.put("interfaz.boton.zoom.Ajustar_al_Ancho_48x48.visible", "true");
			defaults.put("interfaz.boton.zoom.Ajustar_al_Alto_48x48.activado", "true");
			defaults.put("interfaz.boton.zoom.Ajustar_al_Alto_48x48.visible", "true");
			defaults.put("interfaz.boton.zoom.Escalar_Para_Ajustar_48x48.activado", "true");
			defaults.put("interfaz.boton.zoom.Escalar_Para_Ajustar_48x48.visible", "true");
			defaults.put("interfaz.boton.zoom.Zoom_Fijo_48x48.activado", "true");
			defaults.put("interfaz.boton.zoom.Zoom_Fijo_48x48.visible", "true");
			
			defaults.put("interfaz.boton.zoom.zoom_especifico_48x48.activado", "true");
			defaults.put("interfaz.boton.zoom.zoom_especifico_48x48.visible", "true");

    	//-- Vista --
			defaults.put("interfaz.boton.vista.Panel-Galeria_48x48.activado", "true");
			defaults.put("interfaz.boton.vista.Panel-Galeria_48x48.visible", "true");
			defaults.put("interfaz.boton.vista.Grid_48x48.activado", "true");
			defaults.put("interfaz.boton.vista.Grid_48x48.visible", "true");
			defaults.put("interfaz.boton.vista.Pantalla_Completa_48x48.activado", "true");
			defaults.put("interfaz.boton.vista.Pantalla_Completa_48x48.visible", "true");
			defaults.put("interfaz.boton.vista.Lista_48x48.activado", "true");
			defaults.put("interfaz.boton.vista.Lista_48x48.visible", "true");
			defaults.put("interfaz.boton.vista.Carrousel_48x48.activado", "true");
			defaults.put("interfaz.boton.vista.Carrousel_48x48.visible", "true");

    	//-- Control --
			defaults.put("interfaz.boton.control.Refrescar_48x48.activado", "true");
			defaults.put("interfaz.boton.control.Refrescar_48x48.visible", "true");
			defaults.put("interfaz.boton.control.lista_de_favoritos_48x48.activado", "true");
			defaults.put("interfaz.boton.control.lista_de_favoritos_48x48.visible", "true");
			defaults.put("interfaz.boton.control.Borrar_48x48.activado", "true");
			defaults.put("interfaz.boton.control.Borrar_48x48.visible", "true");
			
    	//-- Especiales --
			defaults.put("interfaz.boton.especiales.Selector_de_Carpetas_48x48.activado", "true");
			defaults.put("interfaz.boton.especiales.Selector_de_Carpetas_48x48.visible", "true");
			defaults.put("interfaz.boton.especiales.Menu_48x48.activado", "true");
			defaults.put("interfaz.boton.especiales.Menu_48x48.visible", "true");
			defaults.put("interfaz.boton.especiales.Botones_Ocultos_48x48.activado", "true");
			defaults.put("interfaz.boton.especiales.Botones_Ocultos_48x48.visible", "true");

			defaults.put("interfaz.boton.control.Ubicacion_de_Archivo_48x48.activado", "true");
			defaults.put("interfaz.boton.control.Ubicacion_de_Archivo_48x48.visible", "true");
		
    	//--- Menú ---\n");
    	//Formato: interfaz.menu.<ActionCommand>.{activado|visible|seleccionado} = {true|false}\n");
    	//Nota: .seleccionado solo aplica a CheckBox y RadioButton\n\n");

    	//-- Archivo --
		defaults.put("interfaz.menu.archivo.Archivo.activado", "true");
		defaults.put("interfaz.menu.archivo.Archivo.visible", "true");

		defaults.put("interfaz.menu.archivo.Abrir_Archivo.activado", "true");
		defaults.put("interfaz.menu.archivo.Abrir_Archivo.visible", "true");
		defaults.put("interfaz.menu.archivo.Abrir_Con.activado", "true");
		defaults.put("interfaz.menu.archivo.Abrir_Con.visible", "true");
		defaults.put("interfaz.menu.archivo.Abrir_en_ventana_nueva.activado", "true");
		defaults.put("interfaz.menu.archivo.Abrir_en_ventana_nueva.visible", "true");
		defaults.put("interfaz.menu.archivo.Guardar.activado", "true");
		defaults.put("interfaz.menu.archivo.Guardar.visible", "true");
		defaults.put("interfaz.menu.archivo.Guardar_Como.activado", "true");
		defaults.put("interfaz.menu.archivo.Guardar_Como.visible", "true");
		defaults.put("interfaz.menu.archivo.Editar_Imagen.activado", "true");
		defaults.put("interfaz.menu.archivo.Editar_Imagen.visible", "true");
		defaults.put("interfaz.menu.archivo.Imprimir.activado", "true");
		defaults.put("interfaz.menu.archivo.Imprimir.visible", "true");
		defaults.put("interfaz.menu.archivo.Compartir.activado", "true");
		defaults.put("interfaz.menu.archivo.Compartir.visible", "true");
		defaults.put("interfaz.menu.archivo.Refrescar_Imagen.activado", "true");
		defaults.put("interfaz.menu.archivo.Refrescar_Imagen.visible", "true");
		defaults.put("interfaz.menu.archivo.Volver_a_Cargar.activado", "true");
		defaults.put("interfaz.menu.archivo.Volver_a_Cargar.visible", "true");
		defaults.put("interfaz.menu.archivo.Recargar_Lista_de_Imagenes.activado", "true");
		defaults.put("interfaz.menu.archivo.Recargar_Lista_de_Imagenes.visible", "true");
		defaults.put("interfaz.menu.archivo.Unload_Imagen.activado", "true");
		defaults.put("interfaz.menu.archivo.Unload_Imagen.visible", "true");
		defaults.put("interfaz.menu.archivo.Abrir_Ubicacion_del_Archivo.activado", "true");
		defaults.put("interfaz.menu.archivo.Abrir_Ubicacion_del_Archivo.visible", "true");
				
		//-- Navegacion --
		defaults.put("interfaz.menu.navegacion.Navegacion.activado", "true");
		defaults.put("interfaz.menu.navegacion.Navegacion.visible", "true");

		defaults.put("interfaz.menu.navegacion.Imagen_Aterior.activado", "true");
		defaults.put("interfaz.menu.navegacion.Imagen_Aterior.visible", "true");
		defaults.put("interfaz.menu.navegacion.Imagen_Siguiente.activado", "true");
		defaults.put("interfaz.menu.navegacion.Imagen_Siguiente.visible", "true");
		defaults.put("interfaz.menu.navegacion.Ir_a.activado", "true");
		defaults.put("interfaz.menu.navegacion.Ir_a.visible", "true");
		defaults.put("interfaz.menu.navegacion.Primera_Imagen.activado", "true");
		defaults.put("interfaz.menu.navegacion.Primera_Imagen.visible", "true");
		defaults.put("interfaz.menu.navegacion.Ultima_Imagen.activado", "true");
		defaults.put("interfaz.menu.navegacion.Ultima_Imagen.visible", "true");
		defaults.put("interfaz.menu.navegacion.Anterior_Fotograma.activado", "true");
		defaults.put("interfaz.menu.navegacion.Anterior_Fotograma.visible", "true");
		defaults.put("interfaz.menu.navegacion.Siguiente_Fotograma.activado", "true");
		defaults.put("interfaz.menu.navegacion.Siguiente_Fotograma.visible", "true");
		defaults.put("interfaz.menu.navegacion.Primer_Fotograma.activado", "true");
		defaults.put("interfaz.menu.navegacion.Primer_Fotograma.visible", "true");
		defaults.put("interfaz.menu.navegacion.Ultimo_Fotograma.activado", "true");
		defaults.put("interfaz.menu.navegacion.Ultimo_Fotograma.visible", "true");

		//-- Zoom --
		defaults.put("interfaz.menu.zoom.zoom.Zoom.activado", "true");
		defaults.put("interfaz.menu.zoom.Zoom.visible", "true");
		defaults.put("interfaz.menu.zoom.Acercar.activado", "true");
		defaults.put("interfaz.menu.zoom.Acercar.visible", "true");
		defaults.put("interfaz.menu.zoom.Alejar.activado", "true");
		defaults.put("interfaz.menu.zoom.Alejar.visible", "true");
		defaults.put("interfaz.menu.zoom.Zoom_Personalizado_%.activado", "true");
		defaults.put("interfaz.menu.zoom.Zoom_Personalizado_%.visible", "true");
		defaults.put("interfaz.menu.zoom.Zoom_Tamaño_Real.activado", "true");
		defaults.put("interfaz.menu.zoom.Zoom_Tamaño_Real.visible", "true");
		defaults.put("interfaz.menu.zoom.Mantener_Proporciones.activado", "true");
		defaults.put("interfaz.menu.zoom.Mantener_Proporciones.visible", "true");
		defaults.put("interfaz.menu.zoom.Mantener_Proporciones.seleccionado", "true");
		defaults.put("interfaz.menu.zoom.Activar_Zoom_Manual.activado", "true");
		defaults.put("interfaz.menu.zoom.Activar_Zoom_Manual.visible", "true");
		defaults.put("interfaz.menu.zoom.Activar_Zoom_Manual.seleccionado", "false");// = false # Zoom desactivado
		defaults.put("interfaz.menu.zoom.Resetear_Zoom.activado", "false");// # Ligado a zoom\n");
		defaults.put("interfaz.menu.zoom.Resetear_Zoom.visible", "true");

		defaults.put("interfaz.menu.zoom.Tipos_de_Zoom.activado", "true");
		defaults.put("interfaz.menu.zoom.Tipos_de_Zoom.visible", "true");

		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Zoom_Automatico.activado", "true");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Zoom_Automatico.seleccionado", "false");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Zoom_Automatico.visible", "true");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Zoom_a_lo_Ancho.activado", "true");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Zoom_a_lo_Ancho.seleccionado", "true");// # Selección por defecto ejemplo\n");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Zoom_a_lo_Ancho.visible", "true");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Zoom_a_lo_Alto.activado", "true");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Zoom_a_lo_Alto.seleccionado", "true");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Zoom_a_lo_Alto.visible", "true");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Escalar_Para_Ajustar.activado", "true");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Escalar_Para_Ajustar.seleccionado", "false");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Escalar_Para_Ajustar.visible", "true");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Zoom_Actual_Fijo.activado", "true");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Zoom_Actual_Fijo.seleccionado", "false");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Zoom_Actual_Fijo.visible", "true");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Zoom_Especificado.activado", "true");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Zoom_Especificado.seleccionado", "false");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Zoom_Especificado.visible", "true");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Escalar_Para_Rellenar.activado", "true");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Escalar_Para_Rellenar.seleccionado", "false");
		defaults.put("interfaz.menu.zoom.tipos_de_zoom.Escalar_Para_Rellenar.visible", "true");

		//Imagen
		defaults.put("interfaz.menu.imagen.Imagen.activado", "true");
		defaults.put("interfaz.menu.imagen.Imagen.visible", "true");
		defaults.put("interfaz.menu.imagen.Carga_y_Orden.activado", "true");
		defaults.put("interfaz.menu.imagen.Carga_y_Orden.visible", "true");
    		//-- Carga y Orden     
		defaults.put("interfaz.menu.imagen.carga_y_orden.Nombre_por_Defecto.activado", "true");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Nombre_por_Defecto.seleccionado", "true");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Nombre_por_Defecto.visible", "true");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Tamaño_de_Archivo.activado", "true");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Tamaño_de_Archivo.seleccionado", "false");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Tamaño_de_Archivo.visible", "true");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Fecha_de_Creacion.activado", "true");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Fecha_de_Creacion.seleccionado", "false");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Fecha_de_Creacion.visible", "true");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Extension.activado", "true");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Extension.seleccionado", "false");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Extension.visible", "true");
	    		//--Orden -> Tipos (Radio)
		defaults.put("interfaz.menu.imagen.carga_y_orden.Sin_Ordenar.activado", "true");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Sin_Ordenar.seleccionado", "true");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Sin_Ordenar.visible", "true");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Ascendente.activado", "true");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Ascendente.seleccionado", "false");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Ascendente.visible", "true");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Descendente.activado", "true");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Descendente.seleccionado", "false");
		defaults.put("interfaz.menu.imagen.carga_y_orden.Descendente.visible", "true");
			// --Edicion           
		defaults.put("interfaz.menu.imagen.Edicion.activado", "true");
		defaults.put("interfaz.menu.imagen.Edicion.visible", "true");
		defaults.put("interfaz.menu.imagen.edicion.Girar_Izquierda.activado", "true");
		defaults.put("interfaz.menu.imagen.edicion.Girar_Izquierda.visible", "true");
		defaults.put("interfaz.menu.imagen.edicion.Girar_Derecha.activado", "true");
		defaults.put("interfaz.menu.imagen.edicion.Girar_Derecha.visible", "true");
		defaults.put("interfaz.menu.imagen.edicion.Voltear_Horizontal.activado", "true");
		defaults.put("interfaz.menu.imagen.edicion.Voltear_Horizontal.visible", "true");
		defaults.put("interfaz.menu.imagen.edicion.Voltear_Vertical.activado", "true");
		defaults.put("interfaz.menu.imagen.edicion.Voltear_Vertical.visible", "true");
                                   
		defaults.put("interfaz.menu.imagen.Cambiar_Nombre_de_la_Imagen.activado", "true");
		defaults.put("interfaz.menu.imagen.Cambiar_Nombre_de_la_Imagen.visible", "true");
		defaults.put("interfaz.menu.imagen.Mover_a_la_Papelera.activado", "true");
		defaults.put("interfaz.menu.imagen.Mover_a_la_Papelera.visible", "true");
		defaults.put("interfaz.menu.imagen.Eliminar_Permanentemente.activado", "true");
		defaults.put("interfaz.menu.imagen.Eliminar_Permanentemente.visible", "true");
		defaults.put("interfaz.menu.imagen.Establecer_Como_Fondo_de_Escritorio.activado", "true");
		defaults.put("interfaz.menu.imagen.Establecer_Como_Fondo_de_Escritorio.visible", "true");
		defaults.put("interfaz.menu.imagen.Establecer_Como_Imagen_de_Bloqueo.activado", "true");
		defaults.put("interfaz.menu.imagen.Establecer_Como_Imagen_de_Bloqueo.visible", "true");
		defaults.put("interfaz.menu.imagen.Abrir_Ubicacion_del_Archivo.activado", "true");
		defaults.put("interfaz.menu.imagen.Abrir_Ubicacion_del_Archivo.visible", "true");
		defaults.put("interfaz.menu.imagen.Propiedades_de_la_imagen.activado", "true");
		defaults.put("interfaz.menu.imagen.Propiedades_de_la_imagen.visible", "true");

		//Vista
		defaults.put("interfaz.menu.vista.Vista.activado", "true");
		defaults.put("interfaz.menu.vista.Vista.visible", "true");
                                   
		defaults.put("interfaz.menu.vista.Barra_de_Menu.activado", "true");
		defaults.put("interfaz.menu.vista.Barra_de_Menu.seleccionado", "true");
		defaults.put("interfaz.menu.vista.Barra_de_Menu.visible", "true");
		defaults.put("interfaz.menu.vista.Barra_de_Botones.activado", "true");
		defaults.put("interfaz.menu.vista.Barra_de_Botones.seleccionado", "true");
		defaults.put("interfaz.menu.vista.Barra_de_Botones.visible", "true");
		defaults.put("interfaz.menu.vista.Mostrar/Ocultar_la_Lista_de_Archivos.activado", "true");
		defaults.put("interfaz.menu.vista.Mostrar/Ocultar_la_Lista_de_Archivos.seleccionado", "true");
		defaults.put("interfaz.menu.vista.Mostrar/Ocultar_la_Lista_de_Archivos.visible", "true");
		defaults.put("interfaz.menu.vista.Imagenes_en_Miniatura.activado", "true");
		defaults.put("interfaz.menu.vista.Imagenes_en_Miniatura.seleccionado", "true");
		defaults.put("interfaz.menu.vista.Imagenes_en_Miniatura.visible", "true");
		defaults.put("interfaz.menu.vista.Linea_de_Ubicacion_del_Archivo.activado", "true");
		defaults.put("interfaz.menu.vista.Linea_de_Ubicacion_del_Archivo.seleccionado", "true");
		defaults.put("interfaz.menu.vista.Linea_de_Ubicacion_del_Archivo.visible", "true");
		defaults.put("interfaz.menu.vista.Fondo_a_Cuadros.activado", "true");
		defaults.put("interfaz.menu.vista.Fondo_a_Cuadros.seleccionado", "false");
		defaults.put("interfaz.menu.vista.Fondo_a_Cuadros.visible", "true");
		defaults.put("interfaz.menu.vista.Mantener_Ventana_Siempre_Encima.activado", "true");
		defaults.put("interfaz.menu.vista.Mantener_Ventana_Siempre_Encima.seleccionado", "false");
		defaults.put("interfaz.menu.vista.Mantener_Ventana_Siempre_Encima.visible", "true");
		defaults.put("interfaz.menu.vista.Mostrar_Dialogo_Lista_de_Imagenes.activado", "true");
		defaults.put("interfaz.menu.vista.Mostrar_Dialogo_Lista_de_Imagenes.visible", "true");

    	//Configuracion
		defaults.put("interfaz.menu.configuracion.Configuracion.activado", "true");
		defaults.put("interfaz.menu.configuracion.Configuracion.visible", "true");
	    	//Configuración -> Carga de Imagenes
		defaults.put("interfaz.menu.configuracion.Carga_de_Imagenes.activado", "true");
		defaults.put("interfaz.menu.configuracion.Carga_de_Imagenes.visible", "true");
			//Configuración -> Carga de Imagenes (Radio)
		defaults.put("interfaz.menu.configuracion.carga_de_imagenes.Mostrar_Solo_Carpeta_Actual.activado", "true");
		defaults.put("interfaz.menu.configuracion.carga_de_imagenes.Mostrar_Solo_Carpeta_Actual.seleccionado", "false");// # Corresponde a
		defaults.put("interfaz.menu.configuracion.carga_de_imagenes.Mostrar_Solo_Carpeta_Actual.visible", "true");
		defaults.put("interfaz.menu.configuracion.carga_de_imagenes.Mostrar_Imagenes_de_Subcarpetas.activado", "true");
		defaults.put("interfaz.menu.configuracion.carga_de_imagenes.Mostrar_Imagenes_de_Subcarpetas.seleccionado", "true");// # Corresponde a
		defaults.put("interfaz.menu.configuracion.carga_de_imagenes.Mostrar_Imagenes_de_Subcarpetas.visible", "true");
		
		defaults.put("interfaz.menu.configuracion.carga_de_imagenes.Miniaturas_en_la_Barra_de_Imagenes.activado", "true");
		defaults.put("interfaz.menu.configuracion.carga_de_imagenes.Miniaturas_en_la_Barra_de_Imagenes.visible", "true");
			//Configuracion -> General                  
		defaults.put("interfaz.menu.configuracion.General.activado", "true");
		defaults.put("interfaz.menu.configuracion.General.visible", "true");
		
		defaults.put("interfaz.menu.configuracion.general.Mostrar_Imagen_de_Bienvenida.activado", "true");
		defaults.put("interfaz.menu.configuracion.general.Mostrar_Imagen_de_Bienvenida.seleccionado", "false");
		defaults.put("interfaz.menu.configuracion.general.Mostrar_Imagen_de_Bienvenida.visible", "true");
		defaults.put("interfaz.menu.configuracion.general.Abrir_Ultima_Imagen_Vista.activado", "true");
		defaults.put("interfaz.menu.configuracion.general.Abrir_Ultima_Imagen_Vista.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.general.Abrir_Ultima_Imagen_Vista.visible", "true");
                                   
		defaults.put("interfaz.menu.configuracion.general.Volver_a_la_Primera_Imagen_al_Llegar_al_final_de_la_Lista.activado", "true");
		defaults.put("interfaz.menu.configuracion.general.Volver_a_la_Primera_Imagen_al_Llegar_al_final_de_la_Lista.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.general.Volver_a_la_Primera_Imagen_al_Llegar_al_final_de_la_Lista.visible", "true");
		defaults.put("interfaz.menu.configuracion.general.Mostrar_Flechas_de_Navegacion.activado", "true");
		defaults.put("interfaz.menu.configuracion.general.Mostrar_Flechas_de_Navegacion.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.general.Mostrar_Flechas_de_Navegacion.visible", "true");
			// Configuracion -> Visualizar Botones
		defaults.put("interfaz.menu.configuracion.Visualizar_Botones.activado", "true");
		defaults.put("interfaz.menu.configuracion.Visualizar_Botones.visible", "true");
		
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Ajustar_al_Alto.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Ajustar_al_Alto.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Ajustar_al_Alto.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Ajustar_al_Ancho.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Ajustar_al_Ancho.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Ajustar_al_Ancho.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Borrar.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Borrar.seleccionado", "false");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Borrar.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Carrousel.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Carrousel.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Carrousel.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Escalar_para_Ajustar.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Escalar_para_Ajustar.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Escalar_para_Ajustar.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Espejo_Horizontal.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Espejo_Horizontal.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Espejo_Horizontal.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Espejo_Vertical.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Espejo_Vertical.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Espejo_Vertical.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Grid.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Grid.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Grid.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Lista.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Lista.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Lista.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Lista_de_Favoritos.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Lista_de_Favoritos.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Lista_de_Favoritos.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Menu.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Menu.seleccionado", "false");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Menu.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Panel-Galeria.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Panel-Galeria.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Panel-Galeria.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Pantalla_Completa.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Pantalla_Completa.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Pantalla_Completa.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Refrescar.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Refrescar.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Refrescar.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Reset_Zoom.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Reset_Zoom.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Reset_Zoom.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Recortar.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Recortar.seleccionado", "false");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Recortar.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Rotar_Derecha.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Rotar_Derecha.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Rotar_Derecha.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Rotar_Izquierda.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Rotar_Izquierda.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Rotar_Izquierda.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Subcarpetas.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Subcarpetas.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Subcarpetas.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Zoom.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Zoom.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Zoom.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Zoom_Automatico.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Zoom_Automatico.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Zoom_Automatico.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Zoom_Fijo.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Zoom_Fijo.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Botón_Zoom_Fijo.visible", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Mostrar_Boton_de_Botones_Ocultos.activado", "true");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Mostrar_Boton_de_Botones_Ocultos.seleccionado", "false");
		defaults.put("interfaz.menu.configuracion.visualizar_botones.Mostrar_Boton_de_Botones_Ocultos.visible", "true");
			//Configuracion -> Barra de Informacion
		defaults.put("interfaz.menu.configuracion.Barra_de_Informacion.activado", "true");
		defaults.put("interfaz.menu.configuracion.Barra_de_Informacion.visible", "true");
				//Configuracion -> Barra de Informacion (Radio)
		defaults.put("interfaz.menu.configuracion.barra_de_informacion.Nombre_del_Archivo.activado", "true");
		defaults.put("interfaz.menu.configuracion.barra_de_informacion.Nombre_del_Archivo.seleccionado", "true");
		defaults.put("interfaz.menu.configuracion.barra_de_informacion.Nombre_del_Archivo.visible", "true");
		defaults.put("interfaz.menu.configuracion.barra_de_informacion.Ruta_y_Nombre_del_Archivo.activado", "true");
		defaults.put("interfaz.menu.configuracion.barra_de_informacion.Ruta_y_Nombre_del_Archivo.seleccionado", "false");
		defaults.put("interfaz.menu.configuracion.barra_de_informacion.Ruta_y_Nombre_del_Archivo.visible", "true");

		defaults.put("interfaz.menu.configuracion.barra_de_informacion.Numero_de_Imagenes_en_la_Carpeta_Actual.activado", "true");
		defaults.put("interfaz.menu.configuracion.barra_de_informacion.Numero_de_Imagenes_en_la_Carpeta_Actual.seleccionado", "false");
		defaults.put("interfaz.menu.configuracion.barra_de_informacion.Numero_de_Imagenes_en_la_Carpeta_Actual.visible", "true");
		defaults.put("interfaz.menu.configuracion.barra_de_informacion.%_de_Zoom_actual.activado", "true");
		defaults.put("interfaz.menu.configuracion.barra_de_informacion.%_de_Zoom_actual.seleccionado", "false");
		defaults.put("interfaz.menu.configuracion.barra_de_informacion.%_de_Zoom_actual.visible", "true");
		defaults.put("interfaz.menu.configuracion.barra_de_informacion.Tamaño_del_Archivo.activado", "true");
		defaults.put("interfaz.menu.configuracion.barra_de_informacion.Tamaño_del_Archivo.seleccionado", "false");
		defaults.put("interfaz.menu.configuracion.barra_de_informacion.Tamaño_del_Archivo.visible", "true");
		defaults.put("interfaz.menu.configuracion.barra_de_informacion.Fecha_y_Hora_de_la_Imagen.activado", "true");
		defaults.put("interfaz.menu.configuracion.barra_de_informacion.Fecha_y_Hora_de_la_Imagen.seleccionado", "false");
		defaults.put("interfaz.menu.configuracion.barra_de_informacion.Fecha_y_Hora_de_la_Imagen.visible", "true");

		//Tema
		defaults.put("interfaz.menu.configuracion.tema.Tema_Clear.activado", 		"true");
		defaults.put("interfaz.menu.configuracion.tema.Tema_Clear.seleccionado", 	"true");
		defaults.put("interfaz.menu.configuracion.tema.Tema_Clear.visible", 		"true");
		defaults.put("interfaz.menu.configuracion.tema.Tema_Dark.activado", 		"true");
		defaults.put("interfaz.menu.configuracion.tema.Tema_Dark.seleccionado", 	"false");
		defaults.put("interfaz.menu.configuracion.tema.Tema_Dark.visible", 			"true");
		defaults.put("interfaz.menu.configuracion.tema.Tema_Blue.activado", 		"true");
		defaults.put("interfaz.menu.configuracion.tema.Tema_Blue.seleccionado", 	"false");
		defaults.put("interfaz.menu.configuracion.tema.Tema_Blue.visible", 			"true");
		defaults.put("interfaz.menu.configuracion.tema.Tema_Orange.activado", 		"true");
		defaults.put("interfaz.menu.configuracion.tema.Tema_Orange.seleccionado", 	"false");
		defaults.put("interfaz.menu.configuracion.tema.Tema_Orange.visible", 		"true");
		defaults.put("interfaz.menu.configuracion.tema.Tema_Green.activado", 		"true");
		defaults.put("interfaz.menu.configuracion.tema.Tema_Green.seleccionado", 	"false");
		defaults.put("interfaz.menu.configuracion.tema.Tema_Green.visible", 		"true");
		
		defaults.put("interfaz.menu.configuracion.Guardar_Configuracion_Actual.activado", 	"true");
		defaults.put("interfaz.menu.configuracion.Guardar_Configuracion_Actual.visible", 	"true");
		defaults.put("interfaz.menu.configuracion.Cargar_Configuracion_Inicial.activado", 	"true");
		defaults.put("interfaz.menu.configuracion.Cargar_Configuracion_Inicial.visible", 	"true");
		
		defaults.put("interfaz.menu.configuracion.Version.activado", "true");
		defaults.put("interfaz.menu.configuracion.Version.visible", "true");

		

		
		
		//Colores
		
		//# --- Tema Claro (Basado en Swing Defaults) ---
		defaults.put("colores.claro.fondo.principal",  		"238, 238, 238");
		defaults.put("colores.claro.fondo.secundario",  	"255, 255, 255");
		defaults.put("colores.claro.texto.primario",  		"0, 0, 0      ");
		defaults.put("colores.claro.texto.secundario",  	"80, 80, 80   ");
		defaults.put("colores.claro.borde.color",  			"184, 207, 229");
		defaults.put("colores.claro.borde.titulo",  		"0, 0, 0      ");
		defaults.put("colores.claro.seleccion.fondo",  		"57, 105, 138 ");
		defaults.put("colores.claro.seleccion.texto",  		"255, 255, 255");
		defaults.put("colores.claro.boton.fondo",  			"238, 238, 238");
		defaults.put("colores.claro.boton.texto",  			"0, 0, 0      ");
		defaults.put("colores.claro.boton.fondoActivado",  	"84, 144, 164 ");
		defaults.put("colores.claro.boton.fondoAnimacion",  "173, 216, 230");
        
		
		
		
		
		return defaults;
	}

	
	private static Map<String, String> createDefaultGroupCommentsMap () {
        Map<String, String> comments = new HashMap<>();

        // --- Secciones Principales ---
        comments.put("comportamiento", 	"# ===== Comportamiento =====");
        comments.put("inicio",         	"# ===== Inicio =====");
        comments.put("miniaturas",     	"# ===== Barra de Imagenes en Miniatura =====");
        comments.put("interfaz",       	"# ===== Interfaz =====");

        // --- Ventana de la aplicacion
        comments.put("window", "# ===== Estado de la Ventana de la aplicacion=====");
       // --------------------------------------------
        
        // --- Personalizacion
//        comments.put("colores", 	   	"# ===== Colores UI =====");
        comments.put("iconos",         	"# ===== Configuración General Iconos ====="); // Para 'iconos.alto', 'iconos.ancho'
        comments.put("tema",			"# ===== Tema Visual=====");
        
//        comments.put("tema.nombre", 	"# Nombre del tema (dark, clear, blue, orange, green");
//        comments.put("interfaz.menu.configuracion.tema", "# ===== Tema Visual=====");
        
        
        // --- Subgrupos Nivel 1 (Dentro de interfaz) ---
        comments.put("interfaz.boton", 	"# == Botones =="); // Subgrupo para TODOS los botones
        comments.put("interfaz.menu",  	"# == Menús ==");  // Subgrupo para TODOS los menús

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
        
        // ... etc para otros grupos de menús

        // --- Subgrupos Nivel 1 (Dentro de miniaturas) ---
        comments.put("miniaturas.cantidad",          "# == Cantidad Antes/Después ==");
        comments.put("miniaturas.tamano.normal",     "# == Tamaño Normal ==");
        comments.put("miniaturas.tamano.seleccionada", "# == Tamaño Seleccionada ==");

        // ... Añadir más según sea necesario ...
        
        return comments;
    }
	

	// ************************************************************************************
	// GETTERS Y SETTERS
	// ************************************************************************************

	// Devuelve el valor cargado, o el default de DEFAULT_CONFIG si no existe
	public String getString (String key, String defaultValue)
	{

		return config.getOrDefault(key, defaultValue);
	}

	// Sobrecarga para mantener compatibilidad si se quiere pasar un default
	// específico
	public String getString (String key)// , String defaultValue)
	{

		return config.getOrDefault(key, DEFAULT_CONFIG.getOrDefault(key, ""));

	}

	// Devuelve el valor cargado, o el default de DEFAULT_CONFIG si no existe
	public int getInt (String key)
	{

		String defaultValueStr = DEFAULT_CONFIG.getOrDefault(key, "0"); // Default de String es "0"
		String valueStr = config.getOrDefault(key, defaultValueStr);

		try
		{
			return Integer.parseInt(valueStr);
		} catch (NumberFormatException e)
		{
			System.err.println("WARN: Error parseando int para '" + key + "', valor: '" + valueStr
					+ "'. Usando default int: " + Integer.parseInt(defaultValueStr));

			// Intentar parsear el default string, o usar 0 si falla
			try
			{
				return Integer.parseInt(defaultValueStr);
			} catch (NumberFormatException ne)
			{
				return 0;
			}
		}

	}

	// sobrecarga de metodo
	public int getInt (String key, int defaultValue)
	{

		String value = config.get(key);

		if (value == null)
		{
			return defaultValue;
		}

		try
		{
			return Integer.parseInt(value);
		} catch (NumberFormatException e)
		{
			System.err.println("Advertencia: Error al parsear entero para la clave '" + key + "', valor: '" + value
					+ "'. Usando valor por defecto: " + defaultValue);
			return defaultValue;
		}

	}

	// Devuelve el valor cargado, o el default de DEFAULT_CONFIG si no existe
	public boolean getBoolean (String key)
	{

		String defaultValueStr = DEFAULT_CONFIG.getOrDefault(key, "false"); // Default de String es "false"
		String valueStr = config.getOrDefault(key, defaultValueStr);
		// Boolean.parseBoolean considera "true" (ignorando caso) como true, el resto
		// false.
		return Boolean.parseBoolean(valueStr);

	}

	// sobrecarga de metodo
	public boolean getBoolean (String key, boolean defaultValue)
	{

		String value = config.get(key);

		if (value == null)
		{
			return defaultValue;
		}
		// "true" (ignorando mayúsculas) es true, cualquier otra cosa es false.
		return Boolean.parseBoolean(value);

	}

	// Devuelve una COPIA del mapa para proteger el estado interno
	public Map<String, String> getConfigMap ()
	{

		return new HashMap<>(config);

	}

	public Map<String, String> getConfig ()
	{

		return config;

	}

	// Método para actualizar una clave en memoria (importante usar synchronized si
	// hubiera concurrencia)
	public synchronized void setString (String key, String value)
	{

		if (config != null && key != null && value != null)
		{
			
			//LOG [Config Mgr setString] Actualizando clave
			String oldValue = config.get(key); // Obtener valor anterior
            System.out.println("[Config Mgr setString] Actualizando clave: '" + key + "' | Valor Anterior: '" + oldValue + "' | Valor Nuevo: '" + value + "'");
			
			config.put(key, value);
			System.out.println("[Config Mgr] Valor en memoria actualizado: " + key + " = " + value); // Log
		} else
		{
			System.err.println(
					"[Config Mgr] Intento de actualizar config con null (key=" + key + ", value=" + value + ")");
		}

	}

	// Método específico para inicio.carpeta por conveniencia
	public void setInicioCarpeta (String path)
	{

		setString("inicio.carpeta", path != null ? path : ""); // Asegurar no guardar null

	}

	// Útil para el Controller si necesita saber el default sin leer la config
	// cargada
	public static String getDefault (String key)
	{

		return DEFAULT_CONFIG.get(key); // Devuelve null si la clave no está en los defaults

	}

	public static String getDefault (String key, String fallback)
	{

		return DEFAULT_CONFIG.getOrDefault(key, fallback);

	}

	
	/**
     * Obtiene el nombre del tema actual definido en la configuración (clave 'tema').
     * Si la clave 'tema' no está definida en el archivo cargado,
     * devuelve el valor por defecto definido en DEFAULT_CONFIG ("clear").
     * Si DEFAULT_CONFIG tampoco tiene 'tema', devuelve "clear" como último recurso.
     *
     * @return El nombre del tema actual (ej. "clear", "dark", "blue", "green", "orange").
     */
    public String getTemaActual() {
        // Usamos getString que ya maneja la lógica de buscar en 'config' y luego en 'DEFAULT_CONFIG'
        // El segundo argumento de getString es el fallback final si la clave no existe en NINGUNO
        return getString(KEY_TEMA_NOMBRE, "clear");
    }
    
    
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
        setString(KEY_TEMA_NOMBRE, nuevoTema.trim()); // setString ya imprime un log

        //LOG [ConfigurationManager setTemaActualYGuardar] Valor de: 
        String valorEnMemoria = config.get(KEY_TEMA_NOMBRE);
        System.out.println("[ConfigurationManager setTemaActualYGuardar] Valor de '" + KEY_TEMA_NOMBRE + "' en memoria ANTES de guardar: '" + valorEnMemoria + "'");
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
        
} //Fin configurationManager

