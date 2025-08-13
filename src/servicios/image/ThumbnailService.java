package servicios.image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.cache.LruCache; 

/**
 * Servicio encargado de gestionar la creación, escalado y caché 
 * de las miniaturas (thumbnails) de las imágenes.
 */
public class ThumbnailService {
	
	private static final Logger logger = LoggerFactory.getLogger(ThumbnailService.class);

    private final Map<String, ImageIcon> mapaMiniaturasCacheadas; 

    private final Object HINT_INTERPOLACION = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
    
    // <--- MODIFICADO: El constructor ahora depende de ConfigurationManager ---
    /**
     * Constructor del servicio de miniaturas.
     * Inicializa un caché de tamaño limitado (LruCache) leyendo el tamaño máximo
     * desde el gestor de configuración.
     */
    public ThumbnailService() {
        // Obtenemos la instancia del gestor de configuración
        ConfigurationManager config = ConfigurationManager.getInstance();
        
        // Leemos el tamaño máximo para el caché desde la configuración, con un valor por defecto seguro.
        int tamanoMaximoCache = config.getInt(ConfigKeys.MINIATURAS_CACHE_MAX_SIZE, 200);
        
        // Inicializamos el mapa caché como una instancia de LruCache.
        this.mapaMiniaturasCacheadas = new LruCache<>(tamanoMaximoCache);
        
        logger.debug("[ThumbnailService] Servicio inicializado. LruCache de miniaturas creado con un tamaño máximo de " + tamanoMaximoCache + " entradas.");
    } // --- Fin del método ThumbnailService (constructor) ---
    
    /**
     * Obtiene una miniatura para la imagen especificada.
     * Primero busca en el caché. Si no la encuentra, la carga desde disco,
     * la escala al tamaño deseado usando Graphics2D, la guarda en caché
     * (si es de tamaño normal) y la devuelve.
     *
     * @param rutaArchivo La ruta completa (Path) al archivo de imagen original.
     * @param claveUnica La clave única (generalmente ruta relativa) usada para el caché.
     * @param anchoObjetivo El ancho deseado para la miniatura. Debe ser > 0.
     * @param altoObjetivo El alto deseado para la miniatura. Si es <= 0, se calcula
     *                     para mantener la proporción basada en el anchoObjetivo.
     * @param esTamanoNormal Indica si el tamaño solicitado corresponde al tamaño 'normal'
     *                       que debe ser cacheado. Solo las miniaturas de tamaño normal
     *                       se guardan en el caché persistente del servicio.
     * @return Un ImageIcon con la miniatura escalada, o null si ocurre un error grave
     *         (archivo no encontrado, formato no soportado, error de memoria, etc.).
     */
    public ImageIcon obtenerOCrearMiniatura(Path rutaArchivo, String claveUnica, int anchoObjetivo, int altoObjetivo, boolean esTamanoNormal) {
        // --- El resto del método no necesita cambios ---
        
        Objects.requireNonNull(rutaArchivo, "La ruta del archivo no puede ser nula para obtenerOCrearMiniatura.");
        Objects.requireNonNull(claveUnica, "La clave única no puede ser nula para obtenerOCrearMiniatura.");

        if (anchoObjetivo <= 0) {
            logger.error("[ThumbnailService] ERROR: anchoObjetivo debe ser > 0. Recibido: " + anchoObjetivo + " para clave: " + claveUnica);
            return null;
        }

        if (esTamanoNormal && mapaMiniaturasCacheadas.containsKey(claveUnica)) {
            ImageIcon miniaturaCacheada = mapaMiniaturasCacheadas.get(claveUnica);
            if (miniaturaCacheada.getIconWidth() == anchoObjetivo &&
                (altoObjetivo <= 0 || miniaturaCacheada.getIconHeight() == altoObjetivo)) {
                return miniaturaCacheada;
            } else {
                logger.debug("[ThumbnailService] INFO: Miniatura NORMAL en caché para '" + claveUnica +
                                   "' tenía un tamaño diferente al solicitado. Se regenerará.");
            }
        }

        BufferedImage imagenOriginal = null;
        try {
            if (!Files.exists(rutaArchivo)) {
                logger.error("[ThumbnailService] ERROR: El archivo de imagen original no existe en la ruta: " + rutaArchivo);
                return null;
            }

            imagenOriginal = ImageIO.read(rutaArchivo.toFile());

            if (imagenOriginal == null) {
                logger.error("[ThumbnailService] ERROR: ImageIO.read devolvió null (formato no soportado, archivo corrupto o no es una imagen) para: " + rutaArchivo);
                return null;
            }

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

            ImageIcon miniaturaResultante = new ImageIcon(imagenEscalada);

            if (esTamanoNormal) {
                mapaMiniaturasCacheadas.put(claveUnica, miniaturaResultante);
            }

            return miniaturaResultante;

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
    } // --- Fin del método obtenerOCrearMiniatura ---

    public void limpiarCache() {
        mapaMiniaturasCacheadas.clear();
        logger.debug("[ThumbnailService] Caché de miniaturas limpiado.");
    } // --- Fin del método limpiarCache ---

    public void eliminarDelCache(String claveUnica) {
        if (mapaMiniaturasCacheadas.remove(claveUnica) != null) {
            // logger.debug("[ThumbnailService] Miniatura eliminada del caché: " + claveUnica);
        }
    } // --- Fin del método eliminarDelCache ---
    
} // --- FIN DE LA CLASE ThumbnailService ---
