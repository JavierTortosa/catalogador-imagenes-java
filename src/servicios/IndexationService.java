package servicios;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.datos.ImagenInfo;
import modelo.datos.Tag;
import servicios.db.ImagenDAO;
import servicios.db.TagDAO;
import servicios.db.DiscoDAO;
import modelo.datos.Disco;

/**
 * Servicio encargado de la lógica de indexación de imágenes en la base de datos.
 * Procesa un archivo, lo añade a la BD y le asigna tags basados en su ruta.
 */
public class IndexationService {

    private static final Logger logger = LoggerFactory.getLogger(IndexationService.class);

    private final ImagenDAO imagenDAO;
    private final TagDAO tagDAO;
    private final DiscoDAO discoDAO;
    private final VolumeService volumeService;

    /**
     * Constructor que inicializa los DAO necesarios para la operación.
     */
    public IndexationService() {
        this.imagenDAO = new ImagenDAO();
        this.tagDAO = new TagDAO();
        this.discoDAO = new DiscoDAO();
        this.volumeService = new VolumeService();
    } // ---FIN de constructor [IndexationService]---

    /**
     * Procesa e indexa una única imagen.
     * Si la imagen no existe en la BD, la añade y le asigna tags derivados
     * de la estructura de carpetas jerárquica que la contienen.
     * 
     * @param imagePath La ruta completa del archivo de imagen a procesar.
     * @param rootPath La carpeta raíz del escaneo, usada para determinar qué carpetas son tags.
     */
    public void indexImageAndTags(Path imagePath, Path rootPath) {
        try {
            // 0. Identificar el Disco
            Optional<String> serialOpt = volumeService.getVolumeSerialNumber(imagePath);
            Long discoId = null;
            if (serialOpt.isPresent()) {
                String serial = serialOpt.get();
                Optional<Disco> discoOpt = discoDAO.findByNumeroSerie(serial);
                if (discoOpt.isPresent()) {
                    discoId = discoOpt.get().getId();
                } else {
                    // Si el disco es nuevo, lo registramos
                    Disco nuevoDisco = new Disco(serial, volumeService.getVolumeName(imagePath), imagePath.getRoot().toString());
                    discoId = discoDAO.addDisco(nuevoDisco).orElse(null);
                }
            }

            // 1. Crear el objeto ImagenInfo a partir del archivo
            BasicFileAttributes attrs = Files.readAttributes(imagePath, BasicFileAttributes.class);
            ImagenInfo imagenInfo = new ImagenInfo();
            imagenInfo.setRutaCompleta(imagePath.toString());
            Path fileNamePath = imagePath.getFileName();
            imagenInfo.setNombreArchivo(fileNamePath != null ? fileNamePath.toString() : imagePath.toString());
            imagenInfo.setFechaModificacion(attrs.lastModifiedTime().toMillis());
            imagenInfo.setTamanoBytes(attrs.size());
            imagenInfo.setDiscoId(discoId != null ? discoId : 0);
            imagenInfo.setRutaRelativa(rootPath.relativize(imagePath).toString());
            imagenInfo.setFechaAdicion(System.currentTimeMillis());

            // 2. Intentar añadir la imagen a la base de datos.
            Optional<Long> newImageIdOpt = imagenDAO.addImagen(imagenInfo);

            if (newImageIdOpt.isPresent()) {
                long newImageId = newImageIdOpt.get();
                logger.debug("Nueva imagen para indexar jerárquicamente: {} (ID: {})", imagePath.getFileName(), newImageId);

                // 3. Si la imagen es nueva, proceder con el etiquetado jerárquico.
                Path relativePath = rootPath.relativize(imagePath);
                Path parentPath = relativePath.getParent();

                if (parentPath != null) {
                    Long currentParentId = null; // Empezamos sin padre (tags raíz)
                    Tag lastCreatedTag = null;   // El último tag creado en la jerarquía

                    // Iteramos por cada parte de la ruta (cada nombre de carpeta)
                    for (Path folderNamePath : parentPath) {
                        String tagName = folderNamePath.toString();

                        // Añadimos el tag a la BD, especificando su padre
                        Optional<Tag> tagOpt = tagDAO.addTag(tagName, currentParentId);

                        if (tagOpt.isPresent()) {
                            Tag tag = tagOpt.get();
                            // El ID de este tag será el padre del siguiente en la jerarquía
                            currentParentId = tag.getId();
                            lastCreatedTag = tag;
                            logger.trace("  -> Tag en jerarquía procesado: '{}' (ID: {}, ParentID: {})", 
                                         tag.getNombre(), tag.getId(), tag.getParentId());
                        }
                    }

                    // 4. ASIGNAR SOLO EL ÚLTIMO TAG A LA IMAGEN.
                    // La imagen pertenece a la categoría más específica (la última carpeta).
                    if (lastCreatedTag != null) {
                        tagDAO.assignTagToImage(newImageId, lastCreatedTag.getId());
                        logger.debug("  -> Etiqueta final asignada a la imagen: '{}' (TagID: {})", 
                                     lastCreatedTag.getNombre(), lastCreatedTag.getId());
                    }
                }
            } else {
                 logger.trace("La imagen ya existe en la base de datos, omitiendo etiquetado: {}", imagePath.getFileName());
            }

        } catch (IOException e) {
            logger.error("Error de E/S al procesar el archivo para indexación: {}", imagePath, e);
        } catch (Exception e) {
            logger.error("Error inesperado durante la indexación de: {}", imagePath, e);
        }
    } // ---FIN de metodo [indexImageAndTags]---
    
    /**
     * Indexa una lista de imágenes en un único lote, utilizando una transacción
     * para maximizar el rendimiento.
     * 
     * @param imagePaths Lista de rutas de imágenes.
     * @param rootPath Ruta raíz para el etiquetado.
     */
    public void indexImagesBatch(java.util.List<Path> imagePaths, Path rootPath) {
        java.sql.Connection conn = null;
        boolean originalAutoCommit = true;
        
        try {
            conn = servicios.db.DatabaseManager.getInstance().getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false); // Iniciar transacción
            
            logger.info("Iniciando indexación por lote de {} imágenes...", imagePaths.size());
            long startTime = System.currentTimeMillis();
            
            for (Path path : imagePaths) {
                indexImageAndTags(path, rootPath);
            }
            
            conn.commit(); // Confirmar cambios
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Indexación por lote completada en {} ms.", duration);
            
        } catch (java.sql.SQLException e) {
            logger.error("Error SQL durante la indexación por lote. Intentando rollback...", e);
            if (conn != null) {
                try { conn.rollback(); } catch (java.sql.SQLException ex) { logger.error("Error al hacer rollback", ex); }
            }
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(originalAutoCommit); } catch (java.sql.SQLException ignore) {}
            }
        }
    } // ---FIN de metodo [indexImagesBatch]---

} // --- FIN de clase IndexationService ---