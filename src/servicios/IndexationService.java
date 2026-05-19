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
            // Comprobamos primero si el archivo ya está en la base de datos con su ruta actual exacta
            Optional<ImagenInfo> existingImgOpt = imagenDAO.findImagenByPath(imagePath);
            
            if (existingImgOpt.isPresent()) {
                // ESCENARIO 3: Si encuentra el archivo en su ruta actual, no hace nada. Todo está correcto.
                logger.trace("La imagen ya existe y está correcta en la base de datos: {}", imagePath.getFileName());
                return;
            }

            // Si llegamos aquí, el archivo no está en la base de datos bajo su ruta actual.
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

            // 1. Obtener metadatos físicos
            BasicFileAttributes attrs = Files.readAttributes(imagePath, BasicFileAttributes.class);
            Path fileNamePath = imagePath.getFileName();
            String nombreArchivo = fileNamePath != null ? fileNamePath.toString() : imagePath.toString();
            long tamanoBytes = attrs.size();
            long fechaModificacion = attrs.lastModifiedTime().toMillis();
            long discoIdValue = discoId != null ? discoId : 0;
            String rutaRelativa = rootPath.relativize(imagePath).toString();

            // ESCENARIO 1: Comprobar si el archivo ha cambiado de ubicación (mismo nombre y tamaño, pero ruta vieja ya no existe en disco)
            Optional<ImagenInfo> movedCandidateOpt = imagenDAO.findMovedCandidate(nombreArchivo, tamanoBytes);
            long imageIdToTag;
            
            if (movedCandidateOpt.isPresent()) {
                ImagenInfo candidate = movedCandidateOpt.get();
                imageIdToTag = candidate.getId();
                logger.info("El archivo '{}' ha cambiado de ubicación. Se actualiza de '{}' a '{}'", 
                            nombreArchivo, candidate.getRutaCompleta(), imagePath);
                
                // Actualizamos la ruta y los metadatos en la base de datos
                imagenDAO.updateMovedImagen(imageIdToTag, imagePath.toString(), nombreArchivo, rutaRelativa, discoIdValue, fechaModificacion);
                
                // Limpiamos los tags antiguos de esta imagen ya que ha cambiado de directorio
                tagDAO.clearTagsForImage(imageIdToTag);
            } else {
                // ESCENARIO 2: Si no ha cambiado de ubicación (es una imagen nueva), la añadimos normalmente
                ImagenInfo imagenInfo = new ImagenInfo();
                imagenInfo.setRutaCompleta(imagePath.toString());
                imagenInfo.setNombreArchivo(nombreArchivo);
                imagenInfo.setFechaModificacion(fechaModificacion);
                imagenInfo.setTamanoBytes(tamanoBytes);
                imagenInfo.setDiscoId(discoIdValue);
                imagenInfo.setRutaRelativa(rutaRelativa);
                imagenInfo.setFechaAdicion(System.currentTimeMillis());

                Optional<Long> newImageIdOpt = imagenDAO.addImagen(imagenInfo);
                if (newImageIdOpt.isPresent()) {
                    imageIdToTag = newImageIdOpt.get();
                    logger.debug("Nueva imagen indexada: {} (ID: {})", nombreArchivo, imageIdToTag);
                } else {
                    logger.error("No se pudo añadir la nueva imagen a la base de datos: {}", nombreArchivo);
                    return;
                }
            }

            // Para ESCENARIO 1 y ESCENARIO 2, asignamos/re-asignamos los tags correspondientes de la nueva ruta
            Path parentPath = imagePath.toAbsolutePath().normalize().getParent();

            if (parentPath != null) {
                Long currentParentId = null; // Empezamos sin padre (tags raíz)

                // Iteramos por cada parte de la ruta absoluta (cada nombre de carpeta)
                for (int i = 0; i < parentPath.getNameCount(); i++) {
                    String tagName = parentPath.getName(i).toString().trim();

                    // Omitir la carpeta principal que no aporta información selectiva
                    if ("ARCHIVOS 3D".equalsIgnoreCase(tagName)) {
                        continue;
                    }

                    // Añadimos el tag a la BD, especificando su padre
                    Optional<Tag> tagOpt = tagDAO.addTag(tagName, currentParentId);

                    if (tagOpt.isPresent()) {
                        Tag tag = tagOpt.get();
                        // El ID de este tag será el padre del siguiente en la jerarquía
                        currentParentId = tag.getId();
                        
                        // ASIGNAR ESTE TAG A LA IMAGEN (cada carpeta de la ruta es un tag en sí mismo)
                        tagDAO.assignTagToImage(imageIdToTag, tag.getId());
                        
                        logger.trace("  -> Tag en jerarquía absoluta asignado a la imagen: '{}' (ID: {}, ParentID: {})", 
                                     tag.getNombre(), tag.getId(), tag.getParentId());
                    }
                }
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