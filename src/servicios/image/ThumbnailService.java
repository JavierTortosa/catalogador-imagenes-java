package servicios.image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService; // <<< AÑADIR IMPORT
import java.util.concurrent.Executors;     // <<< AÑADIR IMPORT

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities; // <<< AÑADIR IMPORT

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.cache.LruCache;
import utils.ImageUtils; 

public class ThumbnailService {
	
	private static final Logger logger = LoggerFactory.getLogger(ThumbnailService.class);

    private final Map<String, ImageIcon> mapaMiniaturasCacheadas; 
    private final ExecutorService executor; // <<< AÑADIDO: Para generación asíncrona

    private final Object HINT_INTERPOLACION = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
    
    // =========================================================================
    // === INICIO DE MODIFICACIÓN: AÑADIR INTERFAZ DE CALLBACK ===
    // =========================================================================
    @FunctionalInterface
    public interface ThumbnailListener {
        void onThumbnailCreated(String key);
    }
    // =========================================================================
    // === FIN DE MODIFICACIÓN ===
    // =========================================================================

    public ThumbnailService() {
        ConfigurationManager config = ConfigurationManager.getInstance();
        int tamanoMaximoCache = config.getInt(ConfigKeys.MINIATURAS_CACHE_MAX_SIZE, 200);
        this.mapaMiniaturasCacheadas = new LruCache<>(tamanoMaximoCache);
        
        // <<< AÑADIDO: Inicializar el ExecutorService
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
        // Llama a la nueva versión asíncrona sin un listener.
        // Como este método debe devolver un ImageIcon, tenemos que hacer la generación síncrona aquí.
        // Esto mantiene el comportamiento original para quien lo llame.
        if (mapaMiniaturasCacheadas.containsKey(claveUnica)) {
            return mapaMiniaturasCacheadas.get(claveUnica);
        }
        ImageIcon generatedIcon = generarYEscalarMiniatura(rutaArchivo, claveUnica, anchoObjetivo, altoObjetivo);
        if (generatedIcon != null && esTamanoNormal) {
            mapaMiniaturasCacheadas.put(claveUnica, generatedIcon);
        }
        return generatedIcon;
    } // end of obtenerOCrearMiniatura (original)

    // =========================================================================
    // === INICIO DE MODIFICACIÓN: NUEVO MÉTODO ASÍNCRONO CON LISTENER ===
    // =========================================================================
    public ImageIcon obtenerOCrearMiniatura(Path rutaArchivo, String claveUnica, int anchoObjetivo, int altoObjetivo, boolean esTamanoNormal, ThumbnailListener listener) {
        Objects.requireNonNull(rutaArchivo, "La ruta del archivo no puede ser nula.");
        Objects.requireNonNull(claveUnica, "La clave única no puede ser nula.");

        if (mapaMiniaturasCacheadas.containsKey(claveUnica)) {
            return mapaMiniaturasCacheadas.get(claveUnica);
        }
        
        if (executor != null && !executor.isShutdown()) {
            executor.submit(() -> {
                ImageIcon generatedIcon = generarYEscalarMiniatura(rutaArchivo, claveUnica, anchoObjetivo, altoObjetivo);
                if (generatedIcon != null) {
                    if (esTamanoNormal) {
                        mapaMiniaturasCacheadas.put(claveUnica, generatedIcon);
                    }
                    if (listener != null) {
                        SwingUtilities.invokeLater(() -> listener.onThumbnailCreated(claveUnica));
                    }
                }
            });
        }
        return null;
    } // end of obtenerOCrearMiniatura (asíncrono)
    // =========================================================================
    // === FIN DE MODIFICACIÓN ===
    // =========================================================================

    /**
     * MÉTODO HELPER PRIVADO (extraído de tu método original para reutilizar)
     */
    private ImageIcon generarYEscalarMiniatura(Path rutaArchivo, String claveUnica, int anchoObjetivo, int altoObjetivo) {
         try {
            if (!Files.exists(rutaArchivo)) {
                logger.error("[ThumbnailService] ERROR: El archivo de imagen original no existe en la ruta: " + rutaArchivo);
                return null;
            }
            BufferedImage imagenOriginal = ImageIO.read(rutaArchivo.toFile());
            if (imagenOriginal == null) {
                logger.error("[ThumbnailService] ERROR: ImageIO.read devolvió null (formato no soportado, archivo corrupto o no es una imagen) para: " + rutaArchivo);
                return null;
            }
            
            // La variable 'imagenOriginal' ahora contendrá la imagen ya rotada correctamente.
            imagenOriginal = ImageUtils.correctImageOrientation(imagenOriginal, rutaArchivo);
            
            
            int anchoFinalMiniatura = anchoObjetivo;
            int altoFinalMiniatura;
            if (altoObjetivo <= 0) {
                if (imagenOriginal.getWidth() == 0) {
                    logger.warn("[ThumbnailService] WARN: La imagen original tiene ancho 0. Usando alto original o 1. Para: " + rutaArchivo);
                    altoFinalMiniatura = Math.max(1, imagenOriginal.getHeight() > 0 ? imagenOriginal.getHeight() : 1);
                } else {
                    double ratio = (double) anchoFinalMiniatura / imagenOriginal.getWidth();
                    altoFinalMiniatura = (int) (imagenOriginal.getHeight() * ratio);
                }
            } else {
                altoFinalMiniatura = altoObjetivo;
            }
            anchoFinalMiniatura = Math.max(1, anchoFinalMiniatura);
            altoFinalMiniatura = Math.max(1, altoFinalMiniatura);
            int tipoImagenParaMiniatura = BufferedImage.TYPE_INT_ARGB;
            BufferedImage imagenEscalada = new BufferedImage(anchoFinalMiniatura, altoFinalMiniatura, tipoImagenParaMiniatura);
            Graphics2D g2d = imagenEscalada.createGraphics();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, HINT_INTERPOLACION);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                g2d.drawImage(imagenOriginal, 0, 0, anchoFinalMiniatura, altoFinalMiniatura, null);
            } finally {
                g2d.dispose();
            }
            return new ImageIcon(imagenEscalada);
        } catch (IOException e) {
            logger.error("[ThumbnailService] ERROR DE E/S al procesar la imagen: " + rutaArchivo + ". Mensaje: " + e.getMessage());
            return null;
        } catch (OutOfMemoryError oom) {
            logger.error("[ThumbnailService] ERROR CRÍTICO: Falta de memoria (OutOfMemoryError) al procesar la imagen: " + rutaArchivo);
            limpiarCache();
            return null;
        } catch (IllegalArgumentException iae) {
            logger.error("[ThumbnailService] ERROR DE ARGUMENTO ILEGAL al procesar: " + rutaArchivo + ". Mensaje: " + iae.getMessage());
            iae.printStackTrace();
            return null;
        }
        catch (Exception e) {
            logger.error("[ThumbnailService] ERROR INESPERADO al crear miniatura para: " + rutaArchivo + ". Mensaje: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    } // end of generarYEscalarMiniatura
    
    
    public void limpiarCache() {
        mapaMiniaturasCacheadas.clear();
        logger.debug("[ThumbnailService] Caché de miniaturas limpiado.");
    } // end of limpiarCache
    
    public void eliminarDelCache(String claveUnica) {
        if (mapaMiniaturasCacheadas.remove(claveUnica) != null) {}
    } // end of eliminarDelCache
    
} // end of class