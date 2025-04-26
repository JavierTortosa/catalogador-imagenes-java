package servicios.image;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * Servicio encargado de gestionar la creación, escalado y caché 
 * de las miniaturas (thumbnails) de las imágenes.
 */
public class ThumbnailService {

    // Mapa para almacenar en caché las miniaturas ya generadas (tamaño NORMAL).
    // La clave es la 'claveUnica' (ruta relativa) de la imagen.
    // El valor es el ImageIcon de la miniatura en tamaño NORMAL.
    private final Map<String, ImageIcon> mapaMiniaturasCacheadas; 

    // Calidad de renderizado para el escalado (trade-off entre velocidad y calidad)
    // Opciones comunes: VALUE_INTERPOLATION_NEAREST_NEIGHBOR (rápido, baja calidad),
    // VALUE_INTERPOLATION_BILINEAR (buen balance), VALUE_INTERPOLATION_BICUBIC (lento, alta calidad)
    private final Object HINT_INTERPOLACION = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
    
    // --- Constructor ---
    public ThumbnailService() {
        // Inicializamos el mapa caché al crear el servicio
        this.mapaMiniaturasCacheadas = new HashMap<>();
        System.out.println("[ThumbnailService] Servicio inicializado. Caché de miniaturas creado.");
    }

    /**
     * Obtiene una miniatura para la imagen especificada.
     * Primero busca en el caché. Si no la encuentra, la carga desde disco,
     * la escala al tamaño deseado usando Graphics2D, la guarda en caché 
     * (si es de tamaño normal) y la devuelve.
     *
     * @param rutaArchivo La ruta completa (Path) al archivo de imagen original.
     * @param claveUnica La clave única (generalmente ruta relativa) usada para el caché.
     * @param anchoObjetivo El ancho deseado para la miniatura.
     * @param altoObjetivo El alto deseado para la miniatura. Si es <= 0, se mantiene proporción.
     * @param esTamanoNormal Indica si el tamaño solicitado corresponde al tamaño 'normal'
     *                      que debe ser cacheado. Solo las miniaturas de tamaño normal
     *                      se guardan en el caché persistente.
     * @return Un ImageIcon con la miniatura escalada, o null si ocurre un error grave.
     */
    public ImageIcon obtenerOCrearMiniatura(Path rutaArchivo, String claveUnica, int anchoObjetivo, int altoObjetivo, boolean esTamanoNormal) {
        
        Objects.requireNonNull(rutaArchivo, "La ruta del archivo no puede ser nula.");
        Objects.requireNonNull(claveUnica, "La clave única no puede ser nula.");
        if (anchoObjetivo <= 0) {
             System.err.println("[ThumbnailService] WARN: anchoObjetivo debe ser > 0 para " + claveUnica);
            return null; // O lanzar excepción
        }

        // --- 1. Buscar en caché (SOLO si se pide tamaño normal) ---
        if (esTamanoNormal && mapaMiniaturasCacheadas.containsKey(claveUnica)) {
             ImageIcon miniaturaCacheada = mapaMiniaturasCacheadas.get(claveUnica);
             // Verificar si el tamaño cacheado coincide EXACTAMENTE (por si acaso)
             if (miniaturaCacheada.getIconWidth() == anchoObjetivo && 
                 (altoObjetivo <= 0 || miniaturaCacheada.getIconHeight() == altoObjetivo)) 
             {
                 // System.out.println("[ThumbnailService] Miniatura NORMAL encontrada en caché para: " + claveUnica);
                 return miniaturaCacheada;
             } else {
                  System.out.println("[ThumbnailService] INFO: Miniatura NORMAL en caché tenía tamaño incorrecto para " + claveUnica + ". Se regenerará.");
                  // No retornamos, continuamos para regenerarla con el tamaño correcto.
             }
        }
        
        // --- 2. Si no está en caché (o se pide tamaño seleccionado/diferente) ---
        // System.out.println("[ThumbnailService] Miniatura NO encontrada en caché (o tamaño diferente) para: " + claveUnica + ". Creando...");

        BufferedImage imagenOriginal = null;
        try {
            // Verificar si el archivo existe antes de leer
            if (!Files.exists(rutaArchivo)) {
                System.err.println("[ThumbnailService] ERROR: El archivo no existe: " + rutaArchivo);
                return null;
            }
            
            // Cargar la imagen original desde el disco
            imagenOriginal = ImageIO.read(rutaArchivo.toFile());
            
            if (imagenOriginal == null) {
                System.err.println("[ThumbnailService] ERROR: No se pudo leer la imagen (formato no soportado o archivo corrupto): " + rutaArchivo);
                return null;
            }

            // --- 3. Calcular dimensiones finales (manteniendo proporción si altoObjetivo <= 0) ---
            int anchoFinal = anchoObjetivo;
            int altoFinal = altoObjetivo;

            if (altoFinal <= 0) {
                double ratio = (double) anchoFinal / imagenOriginal.getWidth();
                altoFinal = (int) (imagenOriginal.getHeight() * ratio);
            }
            
            // Asegurar dimensiones mínimas de 1x1
            anchoFinal = Math.max(1, anchoFinal);
            altoFinal = Math.max(1, altoFinal);
            
            // --- 4. Escalar usando Graphics2D ---
            // Crear un BufferedImage vacío del tamaño final
            BufferedImage imagenEscalada = new BufferedImage(anchoFinal, altoFinal, imagenOriginal.getType());
            
            // Obtener el contexto gráfico del BufferedImage escalado
            Graphics2D g2d = imagenEscalada.createGraphics();
            
            try {
                // Aplicar hints de renderizado para calidad
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, HINT_INTERPOLACION);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Dibujar la imagen original DENTRO del BufferedImage escalado
                // Esto realiza el escalado
                g2d.drawImage(imagenOriginal, 0, 0, anchoFinal, altoFinal, null);
                
            } finally {
                // ¡Muy importante liberar los recursos gráficos!
                g2d.dispose();
            }
            
            // --- 5. Crear el ImageIcon a partir del BufferedImage escalado ---
            ImageIcon miniaturaResultante = new ImageIcon(imagenEscalada);
            
            // --- 6. Guardar en caché SI es de tamaño normal ---
            if (esTamanoNormal) {
                 // System.out.println("[ThumbnailService] Guardando miniatura NORMAL en caché para: " + claveUnica);
                 mapaMiniaturasCacheadas.put(claveUnica, miniaturaResultante);
            }

            return miniaturaResultante;

        } catch (IOException e) {
            System.err.println("[ThumbnailService] ERROR de E/S al procesar: " + rutaArchivo + " - " + e.getMessage());
            return null;
        } catch (OutOfMemoryError oom) {
            System.err.println("[ThumbnailService] ERROR: Falta de memoria (OOM) al procesar: " + rutaArchivo);
            // Limpiar caché puede ayudar a recuperarse para futuras operaciones
            limpiarCache(); 
            return null;
        } catch (Exception e) {
            System.err.println("[ThumbnailService] ERROR inesperado al procesar: " + rutaArchivo + " - " + e.getMessage());
            e.printStackTrace(); // Loguear stack trace para depuración
            return null;
        }
    }

    /**
     * Limpia completamente el caché de miniaturas.
     * Puede ser útil en caso de errores de memoria o si se desea forzar
     * la regeneración de todas las miniaturas.
     */
    public void limpiarCache() {
        mapaMiniaturasCacheadas.clear();
        System.out.println("[ThumbnailService] Caché de miniaturas limpiado.");
        // Forzar recolección de basura podría ayudar a liberar memoria más rápido
        // System.gc(); // Usar con precaución
    }

    /**
     * Elimina una entrada específica del caché de miniaturas.
     * @param claveUnica La clave de la miniatura a eliminar.
     */
    public void eliminarDelCache(String claveUnica) {
        if (mapaMiniaturasCacheadas.remove(claveUnica) != null) {
            // System.out.println("[ThumbnailService] Miniatura eliminada del caché: " + claveUnica);
        }
    }
}