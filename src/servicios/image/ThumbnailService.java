package servicios.image;

import java.awt.Graphics2D;
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
    
    
 // En servicios.image.ThumbnailService.java

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

        // --- 0. VALIDACIÓN DE PARÁMETROS DE ENTRADA ---
        Objects.requireNonNull(rutaArchivo, "La ruta del archivo no puede ser nula para obtenerOCrearMiniatura.");
        Objects.requireNonNull(claveUnica, "La clave única no puede ser nula para obtenerOCrearMiniatura.");

        if (anchoObjetivo <= 0) {
            System.err.println("[ThumbnailService] ERROR: anchoObjetivo debe ser > 0. Recibido: " + anchoObjetivo + " para clave: " + claveUnica);
            return null; // Un ancho no positivo no es válido para crear una imagen.
        }
        // altoObjetivo <= 0 es válido e indica que se debe mantener la proporción.

        // --- 1. BUSCAR EN CACHÉ (SOLO SI SE SOLICITA EL TAMAÑO NORMAL) ---
        // Se cachean solo las miniaturas de "tamaño normal" para evitar llenar el caché
        // con múltiples tamaños de la misma imagen (ej. una miniatura seleccionada más grande).
        if (esTamanoNormal && mapaMiniaturasCacheadas.containsKey(claveUnica)) {
            ImageIcon miniaturaCacheada = mapaMiniaturasCacheadas.get(claveUnica);
            // Verificación adicional: que el tamaño cacheado coincida con el solicitado.
            // Esto es por si la configuración de tamaño de miniaturas normales hubiera cambiado.
            if (miniaturaCacheada.getIconWidth() == anchoObjetivo &&
                (altoObjetivo <= 0 || miniaturaCacheada.getIconHeight() == altoObjetivo)) { // Si altoObjetivo es >0, debe coincidir
                // System.out.println("[ThumbnailService] Miniatura NORMAL encontrada y con tamaño correcto en caché para: " + claveUnica);
                return miniaturaCacheada;
            } else {
                System.out.println("[ThumbnailService] INFO: Miniatura NORMAL en caché para '" + claveUnica +
                                   "' tenía un tamaño diferente al solicitado (Cache: " +
                                   miniaturaCacheada.getIconWidth() + "x" + miniaturaCacheada.getIconHeight() +
                                   ", Solicitado: " + anchoObjetivo + "x" + (altoObjetivo <=0 ? "auto" : altoObjetivo) +
                                   "). Se regenerará.");
                // No retornamos, continuamos para regenerarla y actualizar el caché.
            }
        }

        // --- 2. CARGAR IMAGEN ORIGINAL DESDE DISCO (SI NO ESTÁ EN CACHÉ O ES TAMAÑO DIFERENTE) ---
        // System.out.println("[ThumbnailService] Miniatura no encontrada en caché (o tamaño diferente) para: " + claveUnica + ". Creando desde disco...");

        BufferedImage imagenOriginal = null;
        try {
            // 2.1. Verificar existencia del archivo
            if (!Files.exists(rutaArchivo)) {
                System.err.println("[ThumbnailService] ERROR: El archivo de imagen original no existe en la ruta: " + rutaArchivo);
                return null; // No se puede proceder si el archivo no existe.
            }

            // 2.2. Leer la imagen desde el archivo
            imagenOriginal = ImageIO.read(rutaArchivo.toFile());

            // 2.3. Verificar si la lectura fue exitosa
            if (imagenOriginal == null) {
                System.err.println("[ThumbnailService] ERROR: ImageIO.read devolvió null (formato no soportado, archivo corrupto o no es una imagen) para: " + rutaArchivo);
                return null; // No se pudo leer la imagen.
            }

            // --- 3. CALCULAR DIMENSIONES FINALES PARA LA MINIATURA ---
            int anchoFinalMiniatura = anchoObjetivo;
            int altoFinalMiniatura;

            if (altoObjetivo <= 0) { // Si altoObjetivo no es positivo, calcular para mantener proporción
                if (imagenOriginal.getWidth() == 0) { // Evitar división por cero
                    System.err.println("[ThumbnailService] WARN: La imagen original tiene ancho 0. Usando alto original o 1. Para: " + rutaArchivo);
                    altoFinalMiniatura = Math.max(1, imagenOriginal.getHeight() > 0 ? imagenOriginal.getHeight() : 1);
                } else {
                    double ratio = (double) anchoFinalMiniatura / imagenOriginal.getWidth();
                    altoFinalMiniatura = (int) (imagenOriginal.getHeight() * ratio);
                }
            } else { // Si altoObjetivo es positivo, usarlo directamente
                altoFinalMiniatura = altoObjetivo;
            }

            // Asegurar que las dimensiones finales no sean menores que 1x1
            anchoFinalMiniatura = Math.max(1, anchoFinalMiniatura);
            altoFinalMiniatura = Math.max(1, altoFinalMiniatura);

            // --- 4. ESCALAR LA IMAGEN USANDO Graphics2D PARA MEJOR CALIDAD ---

            // 4.1. Crear un BufferedImage vacío para la miniatura escalada.
            //      **Se usa TYPE_INT_ARGB explícitamente para evitar el error "Unknown image type 0"**
            //      y para asegurar el manejo correcto de la transparencia (común en PNGs).
            int tipoImagenParaMiniatura = BufferedImage.TYPE_INT_ARGB;
            BufferedImage imagenEscalada = new BufferedImage(anchoFinalMiniatura, altoFinalMiniatura, tipoImagenParaMiniatura);

            // 4.2. Obtener el contexto Graphics2D del BufferedImage destino.
            Graphics2D g2d = imagenEscalada.createGraphics();

            try {
                // 4.3. Aplicar hints de renderizado para mejorar la calidad del escalado.
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, HINT_INTERPOLACION); // Bilinear es un buen compromiso
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY); // Para transparencia

                // 4.4. Dibujar la imagen original escalada sobre el BufferedImage destino.
                g2d.drawImage(imagenOriginal, 0, 0, anchoFinalMiniatura, altoFinalMiniatura, null);

            } finally {
                // 4.5. ¡Muy importante! Liberar los recursos del contexto gráfico.
                g2d.dispose();
            }

            // --- 5. CREAR EL ImageIcon A PARTIR DEL BufferedImage ESCALADO ---
            ImageIcon miniaturaResultante = new ImageIcon(imagenEscalada);

            // --- 6. GUARDAR EN CACHÉ SI CORRESPONDE AL TAMAÑO NORMAL ---
            if (esTamanoNormal) {
                // System.out.println("[ThumbnailService] Miniatura NORMAL generada y guardada en caché para: " + claveUnica);
                mapaMiniaturasCacheadas.put(claveUnica, miniaturaResultante);
            } else {
                // System.out.println("[ThumbnailService] Miniatura de tamaño específico (no normal) generada para: " + claveUnica + ". No se guarda en caché principal.");
            }

            return miniaturaResultante; // Devolver la miniatura creada

        } catch (IOException e) {
            System.err.println("[ThumbnailService] ERROR DE E/S al procesar la imagen: " + rutaArchivo + ". Mensaje: " + e.getMessage());
            // No es necesario e.printStackTrace() aquí para errores de IO comunes, el mensaje suele ser suficiente.
            return null;
        } catch (OutOfMemoryError oom) {
            System.err.println("[ThumbnailService] ERROR CRÍTICO: Falta de memoria (OutOfMemoryError) al procesar la imagen: " + rutaArchivo);
            // Limpiar el caché puede ayudar a la aplicación a recuperarse para futuras operaciones.
            limpiarCache();
            // System.gc(); // Considerar con mucho cuidado, puede tener implicaciones de rendimiento.
            return null;
        } catch (IllegalArgumentException iae) {
            // Esto podría capturar otros IllegalArgumentException, no solo el de BufferedImage.
            System.err.println("[ThumbnailService] ERROR DE ARGUMENTO ILEGAL al procesar: " + rutaArchivo + ". Mensaje: " + iae.getMessage());
            iae.printStackTrace(); // Útil para ver si es el "Unknown image type" u otro.
            return null;
        }
        catch (Exception e) {
            // Captura genérica para cualquier otro error inesperado durante el proceso.
            System.err.println("[ThumbnailService] ERROR INESPERADO al crear miniatura para: " + rutaArchivo + ". Mensaje: " + e.getMessage());
            e.printStackTrace(); // Importante para diagnosticar errores no previstos.
            return null;
        }
    }
    

//    /**
//     * Obtiene una miniatura para la imagen especificada.
//     * Primero busca en el caché. Si no la encuentra, la carga desde disco,
//     * la escala al tamaño deseado usando Graphics2D, la guarda en caché 
//     * (si es de tamaño normal) y la devuelve.
//     *
//     * @param rutaArchivo La ruta completa (Path) al archivo de imagen original.
//     * @param claveUnica La clave única (generalmente ruta relativa) usada para el caché.
//     * @param anchoObjetivo El ancho deseado para la miniatura.
//     * @param altoObjetivo El alto deseado para la miniatura. Si es <= 0, se mantiene proporción.
//     * @param esTamanoNormal Indica si el tamaño solicitado corresponde al tamaño 'normal'
//     *                      que debe ser cacheado. Solo las miniaturas de tamaño normal
//     *                      se guardan en el caché persistente.
//     * @return Un ImageIcon con la miniatura escalada, o null si ocurre un error grave.
//     */
//    public ImageIcon obtenerOCrearMiniatura(Path rutaArchivo, String claveUnica, int anchoObjetivo, int altoObjetivo, boolean esTamanoNormal) {
//        
//        Objects.requireNonNull(rutaArchivo, "La ruta del archivo no puede ser nula.");
//        Objects.requireNonNull(claveUnica, "La clave única no puede ser nula.");
//        if (anchoObjetivo <= 0) {
//             System.err.println("[ThumbnailService] WARN: anchoObjetivo debe ser > 0 para " + claveUnica);
//            return null; // O lanzar excepción
//        }
//
//        // --- 1. Buscar en caché (SOLO si se pide tamaño normal) ---
//        if (esTamanoNormal && mapaMiniaturasCacheadas.containsKey(claveUnica)) {
//             ImageIcon miniaturaCacheada = mapaMiniaturasCacheadas.get(claveUnica);
//             // Verificar si el tamaño cacheado coincide EXACTAMENTE (por si acaso)
//             if (miniaturaCacheada.getIconWidth() == anchoObjetivo && 
//                 (altoObjetivo <= 0 || miniaturaCacheada.getIconHeight() == altoObjetivo)) 
//             {
//                 // System.out.println("[ThumbnailService] Miniatura NORMAL encontrada en caché para: " + claveUnica);
//                 return miniaturaCacheada;
//             } else {
//                  System.out.println("[ThumbnailService] INFO: Miniatura NORMAL en caché tenía tamaño incorrecto para " + claveUnica + ". Se regenerará.");
//                  // No retornamos, continuamos para regenerarla con el tamaño correcto.
//             }
//        }
//        
//        // --- 2. Si no está en caché (o se pide tamaño seleccionado/diferente) ---
//        // System.out.println("[ThumbnailService] Miniatura NO encontrada en caché (o tamaño diferente) para: " + claveUnica + ". Creando...");
//
//        BufferedImage imagenOriginal = null;
//        try {
//            // Verificar si el archivo existe antes de leer
//            if (!Files.exists(rutaArchivo)) {
//                System.err.println("[ThumbnailService] ERROR: El archivo no existe: " + rutaArchivo);
//                return null;
//            }
//            
//            // Cargar la imagen original desde el disco
//            imagenOriginal = ImageIO.read(rutaArchivo.toFile());
//            
//            if (imagenOriginal == null) {
//                System.err.println("[ThumbnailService] ERROR: No se pudo leer la imagen (formato no soportado o archivo corrupto): " + rutaArchivo);
//                return null;
//            }
//
//            // --- 3. Calcular dimensiones finales (manteniendo proporción si altoObjetivo <= 0) ---
//            int anchoFinal = anchoObjetivo;
//            int altoFinal = altoObjetivo;
//
//            if (altoFinal <= 0) {
//                double ratio = (double) anchoFinal / imagenOriginal.getWidth();
//                altoFinal = (int) (imagenOriginal.getHeight() * ratio);
//            }
//            
//            // Asegurar dimensiones mínimas de 1x1
//            anchoFinal = Math.max(1, anchoFinal);
//            altoFinal = Math.max(1, altoFinal);
//            
//            // --- 4. Escalar usando Graphics2D ---
//            // Crear un BufferedImage vacío del tamaño final
//            BufferedImage imagenEscalada = new BufferedImage(anchoFinal, altoFinal, imagenOriginal.getType());
//            
//            // Obtener el contexto gráfico del BufferedImage escalado
//            Graphics2D g2d = imagenEscalada.createGraphics();
//            
//            try {
//                // Aplicar hints de renderizado para calidad
//                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, HINT_INTERPOLACION);
//                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//                
//                // Dibujar la imagen original DENTRO del BufferedImage escalado
//                // Esto realiza el escalado
//                g2d.drawImage(imagenOriginal, 0, 0, anchoFinal, altoFinal, null);
//                
//            } finally {
//                // ¡Muy importante liberar los recursos gráficos!
//                g2d.dispose();
//            }
//            
//            // --- 5. Crear el ImageIcon a partir del BufferedImage escalado ---
//            ImageIcon miniaturaResultante = new ImageIcon(imagenEscalada);
//            
//            // --- 6. Guardar en caché SI es de tamaño normal ---
//            if (esTamanoNormal) {
//                 // System.out.println("[ThumbnailService] Guardando miniatura NORMAL en caché para: " + claveUnica);
//                 mapaMiniaturasCacheadas.put(claveUnica, miniaturaResultante);
//            }
//
//            return miniaturaResultante;
//
//        } catch (IOException e) {
//            System.err.println("[ThumbnailService] ERROR de E/S al procesar: " + rutaArchivo + " - " + e.getMessage());
//            return null;
//        } catch (OutOfMemoryError oom) {
//            System.err.println("[ThumbnailService] ERROR: Falta de memoria (OOM) al procesar: " + rutaArchivo);
//            // Limpiar caché puede ayudar a recuperarse para futuras operaciones
//            limpiarCache(); 
//            return null;
//        } catch (Exception e) {
//            System.err.println("[ThumbnailService] ERROR inesperado al procesar: " + rutaArchivo + " - " + e.getMessage());
//            e.printStackTrace(); // Loguear stack trace para depuración
//            return null;
//        }
//    }

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