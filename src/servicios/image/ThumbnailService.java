package servicios.image;

//import java.awt.Graphics2D;
//import java.awt.RenderingHints;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.configurations.Antialiasing;
import net.coobird.thumbnailator.resizers.configurations.Rendering;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService; 
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities; 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import utils.ImageUtils; 

public class ThumbnailService {
	
	private static final Logger logger = LoggerFactory.getLogger(ThumbnailService.class);

	private final Cache<String, ImageIcon> mapaMiniaturasCacheadas;
    private final ExecutorService executor; //  Para generación asíncrona
    
//    private final Object HINT_INTERPOLACION = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
    
    @FunctionalInterface
    public interface ThumbnailListener {
        void onThumbnailCreated(String key);
    }

    public ThumbnailService() {
        ConfigurationManager config = ConfigurationManager.getInstance();
        int tamanoMaximoCache = config.getInt(ConfigKeys.MINIATURAS_CACHE_MAX_SIZE, 200);
        
        // Usamos el "builder" de Caffeine para construir nuestra caché.
        // Es muy legible y nos permitiría añadir más reglas en el futuro (como expiración por tiempo).
        this.mapaMiniaturasCacheadas = Caffeine.newBuilder()
                .maximumSize(tamanoMaximoCache) // Le decimos el tamaño máximo de elementos
                .build();                       // Y la construimos.
        
        // Inicializar el ExecutorService
        int numThreads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
        this.executor = Executors.newFixedThreadPool(numThreads, (r) -> {
            Thread t = new Thread(r, "ThumbnailGeneratorThread");
            t.setDaemon(true);
            return t;
        });
        
        logger.debug("[ThumbnailService] Servicio inicializado. LruCache (tamaño: {}) y Executor (threads: {}) creados.", tamanoMaximoCache, numThreads);
    } // end of constructor
    
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
        try {
            // 1. Validaciones iniciales (igual que antes)
            if (!Files.exists(rutaArchivo)) {
                logger.error("[ThumbnailService] ERROR: El archivo no existe: {}", rutaArchivo);
                return null;
            }
            
            // Thumbnailator puede leer directamente de un File, lo que es muy eficiente.
            // Primero, aplicamos nuestra corrección de orientación EXIF.
            BufferedImage imagenOriginal = ImageIO.read(rutaArchivo.toFile());
            if (imagenOriginal == null) {
                logger.error("[ThumbnailService] ERROR: ImageIO.read devolvió null para: {}", rutaArchivo);
                return null;
            }
            BufferedImage imagenCorregida = ImageUtils.correctImageOrientation(imagenOriginal, rutaArchivo);

            // 2. Lógica de escalado con Thumbnailator
            boolean mantenerProporcion = (altoObjetivo <= 0);
            int anchoFinal = Math.max(1, anchoObjetivo);
            // Si no mantenemos proporción, el alto es el objetivo, si no, lo calculamos.
            int altoFinal = mantenerProporcion ? Integer.MAX_VALUE : Math.max(1, altoObjetivo);

            // 3. Creación de la miniatura usando la API fluida de Thumbnailator
            BufferedImage imagenEscalada = Thumbnails.of(imagenCorregida)
                    .size(anchoFinal, altoFinal)                // Establece el tamaño máximo
                    .keepAspectRatio(mantenerProporcion)         // Indica si mantener la proporción o no
                    .rendering(Rendering.QUALITY)              // Equivalente a VALUE_RENDER_QUALITY
                    .antialiasing(Antialiasing.ON)             // Equivalente a VALUE_ANTIALIAS_ON
                    .asBufferedImage();                          // Obtiene el resultado como BufferedImage

            return new ImageIcon(imagenEscalada);

        } catch (IOException e) {
            logger.error("[ThumbnailService] ERROR DE E/S al procesar: {}. Mensaje: {}", rutaArchivo, e.getMessage());
            return null;
        } catch (OutOfMemoryError oom) {
            logger.error("[ThumbnailService] ERROR CRÍTICO: OutOfMemoryError al procesar: {}", rutaArchivo);
            limpiarCache();
            return null;
        } catch (Exception e) {
            logger.error("[ThumbnailService] ERROR INESPERADO al crear miniatura para: {}. Mensaje: {}", rutaArchivo, e.getMessage(), e);
            return null;
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
    
} // end of class