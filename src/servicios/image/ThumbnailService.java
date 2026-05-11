package servicios.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.configurations.Antialiasing;
import net.coobird.thumbnailator.resizers.configurations.Rendering;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import utils.ImageUtils; 

public class ThumbnailService {
	
	private static final Logger logger = LoggerFactory.getLogger(ThumbnailService.class);

	private final Cache<String, ImageIcon> mapaMiniaturasCacheadas;
    private final ExecutorService executor; //  Para generación asíncrona
    
    private final Set<Path> invalidImagePaths;
    private ImageIcon brokenImageIcon;
    
    @FunctionalInterface
    public interface ThumbnailListener {
        void onThumbnailCreated(String key);
    }

    public ThumbnailService() {
        ConfigurationManager config = ConfigurationManager.getInstance();
        int tamanoMaximoCache = config.getInt(ConfigKeys.MINIATURAS_CACHE_MAX_SIZE, 200);
        
        this.mapaMiniaturasCacheadas = Caffeine.newBuilder()
                .maximumSize(tamanoMaximoCache)
                .build();
        
        int numThreads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
        this.executor = Executors.newFixedThreadPool(numThreads, (r) -> {
            Thread t = new Thread(r, "ThumbnailGeneratorThread");
            t.setDaemon(true);
            return t;
        });
        
        // --- INICIO DE LA CORRECCIÓN ---
        // Inicializamos el nuevo caché de fallos de forma segura para hilos.
        this.invalidImagePaths = Collections.synchronizedSet(new HashSet<>());
        
        // Cargamos el icono de imagen rota una sola vez.
        try {
            this.brokenImageIcon = new ImageIcon(ImageIO.read(Objects.requireNonNull(
                getClass().getResource("/iconos/comunes/imagen-rota.png"))));
        } catch (Exception e) {
            logger.error("No se pudo cargar el icono de 'imagen-rota.png'. Se usará null como fallback.", e);
            this.brokenImageIcon = null;
        }
        // --- FIN DE LA CORRECCIÓN ---
        
        logger.debug("[ThumbnailService] Servicio inicializado. LruCache (tamaño: {}) y Executor (threads: {}) creados.", tamanoMaximoCache, numThreads);
    } // ---FIN de constructor [ThumbnailService]---
    
    
    /**
     * MÉTODO ORIGINAL SOBRECARGADO (SIN TOCARLO)
     * Para mantener la compatibilidad con el resto de la aplicación (barra de miniaturas).
     */
    
    public ImageIcon obtenerOCrearMiniatura(Path rutaArchivo, String claveUnica, int anchoObjetivo, int altoObjetivo, boolean esTamanoNormal) {
        // Esta es la magia de Caffeine. La operación `get` es atómica.
        // 1. Busca un valor para `claveUnica`.
        // 2. Si lo encuentra, lo devuelve.
        // 3. Si NO lo encuentra, ejecuta la función que le pasamos (la lambda `k -> ...`).
        //    El resultado de esa función se guarda automáticamente en la caché con la `claveUnica` y se devuelve.
        // Esto reemplaza el `if/else`, el `get` y el `put` en una sola línea thread-safe.
        // NOTA: Con este cambio, ahora cacheamos *todas* las miniaturas, no solo las de "tamaño normal".
        // Esto es una simplificación bienvenida y buena para el rendimiento general.
        if (esTamanoNormal) {
            return mapaMiniaturasCacheadas.get(claveUnica, k -> generarYEscalarMiniatura(rutaArchivo, k, anchoObjetivo, altoObjetivo));
        } else {
            // Para tamaños no normales (ej. miniatura seleccionada más grande), no la guardamos en la caché principal
            // para no expulsar miniaturas de tamaño estándar que son más reutilizadas.
            return generarYEscalarMiniatura(rutaArchivo, claveUnica, anchoObjetivo, altoObjetivo);
        }
    } // end of obtenerOCrearMiniatura (original)
    
    
    public ImageIcon obtenerOCrearMiniatura(Path rutaArchivo, String claveUnica, int anchoObjetivo, int altoObjetivo, boolean esTamanoNormal, ThumbnailListener listener) {
        Objects.requireNonNull(rutaArchivo, "La ruta del archivo no puede ser nula.");
        Objects.requireNonNull(claveUnica, "La clave única no puede ser nula.");

        // El método de Caffeine `getIfPresent` busca en la caché y devuelve el valor si existe, o null si no.
        // No bloquea ni intenta generar nada, es perfecto para una comprobación rápida.
        final ImageIcon cachedIcon = mapaMiniaturasCacheadas.getIfPresent(claveUnica);
        if (cachedIcon != null) {
            return cachedIcon;
        }
        
        // Si no está en la caché, lanzamos la tarea asíncrona para generarla.
        if (executor != null && !executor.isShutdown()) {
            executor.submit(() -> {
                ImageIcon generatedIcon = generarYEscalarMiniatura(rutaArchivo, claveUnica, anchoObjetivo, altoObjetivo);
                if (generatedIcon != null) {
                    // Solo la guardamos en la caché si es de tamaño normal.
                    if (esTamanoNormal) {
                        mapaMiniaturasCacheadas.put(claveUnica, generatedIcon);
                    }
                    if (listener != null) {
                        SwingUtilities.invokeLater(() -> listener.onThumbnailCreated(claveUnica));
                    }
                }
            });
        }
        // Devolvemos null inmediatamente, como antes, para no bloquear la UI.
        return null;
    } // end of obtenerOCrearMiniatura (asíncrono)
    

    /**
     * MÉTODO HELPER PRIVADO (extraído de tu método original para reutilizar)
     */
    private ImageIcon generarYEscalarMiniatura(Path rutaArchivo, String claveUnica, int anchoObjetivo, int altoObjetivo) {
        // --- INICIO DE LA LÓGICA DE CACHÉ DE FALLOS ---
        // 1. Comprobar si esta ruta ya está en nuestra lista negra.
        if (invalidImagePaths.contains(rutaArchivo)) {
            // Si ya sabemos que es inválida, devolvemos el icono de imagen rota.
            // Es importante que este icono ya esté escalado a un tamaño razonable para que no desfigure la UI.
            // Por ahora, lo devolvemos tal cual. Si causa problemas de tamaño, lo ajustaremos.
            return brokenImageIcon;
        }
        // --- FIN DE LA LÓGICA DE CACHÉ DE FALLOS ---

        try {
            if (!Files.exists(rutaArchivo)) {
                logger.warn("[ThumbnailService] El archivo no existe: {}", rutaArchivo);
                invalidImagePaths.add(rutaArchivo); // Añadir a la lista negra
                return brokenImageIcon;
            }
            
            BufferedImage imagenOriginal = ImageIO.read(rutaArchivo.toFile());
            
            if (imagenOriginal == null) {
                logger.warn("[ThumbnailService] ImageIO.read devolvió null (archivo inválido o corrupto) para: {}", rutaArchivo);
                invalidImagePaths.add(rutaArchivo); // Añadir a la lista negra.
                return brokenImageIcon;             // Devolver el icono de imagen rota.
            }

            BufferedImage imagenCorregida = ImageUtils.correctImageOrientation(imagenOriginal, rutaArchivo);

            boolean mantenerProporcion = (altoObjetivo <= 0);
            int anchoFinal = Math.max(1, anchoObjetivo);
            int altoFinal = mantenerProporcion ? Integer.MAX_VALUE : Math.max(1, altoObjetivo);

            BufferedImage imagenEscalada = Thumbnails.of(imagenCorregida)
                    .size(anchoFinal, altoFinal)
                    .keepAspectRatio(mantenerProporcion)
                    .rendering(Rendering.QUALITY)
                    .antialiasing(Antialiasing.ON)
                    .asBufferedImage();

            return new ImageIcon(imagenEscalada);

        } catch (IOException e) {
            logger.warn("[ThumbnailService] ERROR DE E/S al procesar: {}. Mensaje: {}", rutaArchivo, e.getMessage());
            invalidImagePaths.add(rutaArchivo);
            return brokenImageIcon;
        } catch (OutOfMemoryError oom) {
            logger.error("[ThumbnailService] ERROR CRÍTICO: OutOfMemoryError al procesar: {}", rutaArchivo);
            invalidImagePaths.add(rutaArchivo);
            limpiarCache();
            return brokenImageIcon;
        } catch (Exception e) {
            logger.error("[ThumbnailService] ERROR INESPERADO al crear miniatura para: {}. Mensaje: {}", rutaArchivo, e.getMessage(), e);
            invalidImagePaths.add(rutaArchivo);
            return brokenImageIcon;
        }
    } // ---FIN de metodo generarYEscalarMiniatura---
    
    
    public void limpiarCache() {
        // `invalidateAll` es el método equivalente en Caffeine a `clear`.
        mapaMiniaturasCacheadas.invalidateAll();
        logger.debug("[ThumbnailService] Caché de miniaturas limpiado.");
    } // end of limpiarCache
    
    
    public void eliminarDelCache(String claveUnica) {
        // `invalidate` es el método equivalente en Caffeine a `remove`.
        mapaMiniaturasCacheadas.invalidate(claveUnica);
    } // end of eliminarDelCache
   
} // end of class ThumbnailService