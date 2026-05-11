package controlador.managers;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import modelo.datos.ImagenInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.datos.Tag;
import servicios.db.ImagenDAO;
import servicios.db.TagDAO;
import modelo.datos.Disco;
import servicios.db.DiscoDAO;
import servicios.VolumeService;
import java.util.Map;
import java.util.HashMap;
import java.io.File;

/**
 * Manager para gestionar la lógica de negocio de alto nivel relacionada
 * con el acceso a los datos de la base de datos para el "Modo Datos".
 */
public class DataManager {

    private static final Logger logger = LoggerFactory.getLogger(DataManager.class);

    private final TagDAO tagDAO;
    private final ImagenDAO imagenDAO;

    private final DiscoDAO discoDAO;
    private final VolumeService volumeService;

    public DataManager() {
        this.tagDAO = new TagDAO();
        this.imagenDAO = new ImagenDAO();
        this.discoDAO = new DiscoDAO();
        this.volumeService = new VolumeService();
    } // ---FIN de constructor [DataManager]---


    /**
     * Obtiene una lista de todos los tags existentes en la base de datos,
     * ordenados alfabéticamente.
     * @return Una lista de objetos Tag.
     */
    public List<Tag> getAllTags() {
        logger.debug("Solicitando todos los tags de la base de datos...");
        List<Tag> tags = tagDAO.getAllTags();
        logger.info("Se encontraron {} tags en total.", tags.size());
        return tags;
    } // ---FIN de metodo [getAllTags]---

    /**
     * Obtiene una lista de todas las rutas de imágenes asociadas a un tag específico
     * Y a TODOS sus tags descendientes (hijos, nietos, etc.).
     * @param tag El tag por el cual filtrar jerárquicamente.
     * @return Una lista de Strings con las rutas completas de las imágenes.
     */
    public List<String> getImagePathsForTag(Tag tag) {
        if (tag == null) {
            logger.warn("Se solicitó buscar imágenes para un tag nulo. Devolviendo lista vacía.");
            return Collections.emptyList();
        }
        
        logger.debug("Solicitando rutas de imágenes para el tag jerárquico: '{}' (ID: {})", tag.getNombre(), tag.getId());

        // 1. Crear una lista que contendrá el tag padre y todos sus descendientes.
        List<Tag> tagsToSearch = new ArrayList<>();
        tagsToSearch.add(tag); // Añadir el propio tag seleccionado.
        
        // 2. Pedir al DAO todos los descendientes y añadirlos a la lista.
        List<Tag> descendants = tagDAO.getAllDescendantTags(tag.getId());
        tagsToSearch.addAll(descendants);
        
        // 3. Convertir la lista de objetos Tag a una lista de sus nombres.
        List<String> tagNamesToSearch = tagsToSearch.stream()
                                                    .map(Tag::getNombre)
                                                    .collect(java.util.stream.Collectors.toList());

        logger.info("Búsqueda jerárquica para '{}' incluye {} tags en total: {}", tag.getNombre(), tagNamesToSearch.size(), tagNamesToSearch);
        
        // 4. Usar el método DAO existente, que ya sabe buscar por una lista de nombres.
        List<String> paths = tagDAO.findImagePathsByTagNames(tagNamesToSearch);
        logger.info("Se encontraron {} imágenes para la jerarquía del tag '{}'.", paths.size(), tag.getNombre());
        return paths;
    } // ---FIN de metodo [getImagePathsForTag]---
    /**
     * Obtiene los tags asociados a una imagen dada su ruta.
     * @param imagePath La ruta de la imagen.
     * @return Una lista de tags.
     */
    public List<Tag> getTagsForImage(Path imagePath) {
        if (imagePath == null) return Collections.emptyList();
        
        Optional<ImagenInfo> imgOpt = imagenDAO.findImagenByPath(imagePath);
        if (imgOpt.isPresent()) {
            return tagDAO.getTagsForImage(imgOpt.get().getId());
        }
        return Collections.emptyList();
    } // ---FIN de metodo [getTagsForImage]---

    /**
     * Añade un tag (por nombre) a una lista de imágenes.
     * Si el tag no existe, lo crea.
     * @param imagePaths Lista de rutas de imágenes.
     * @param tagName Nombre del tag.
     */
    public void addTagToImages(List<Path> imagePaths, String tagName) {
        if (imagePaths == null || imagePaths.isEmpty() || tagName == null || tagName.isBlank()) return;
        
        // 1. Asegurar que el tag existe
        Tag tag = tagDAO.addTag(tagName).orElse(null);
        if (tag == null) return;
        
        logger.info("Añadiendo etiqueta '{}' a {} imágenes.", tagName, imagePaths.size());
        
        for (Path path : imagePaths) {
            Optional<ImagenInfo> imgOpt = imagenDAO.findImagenByPath(path);
            if (imgOpt.isPresent()) {
                tagDAO.assignTagToImage(imgOpt.get().getId(), tag.getId());
            } else {
                logger.warn("No se pudo añadir tag a la imagen porque no está indexada: {}", path);
            }
        }
    } // ---FIN de metodo [addTagToImages]---

    /**
     * Elimina un tag específico de una lista de imágenes.
     * @param imagePaths Lista de rutas de imágenes.
     * @param tag El tag a eliminar.
     */
    public void removeTagFromImages(List<Path> imagePaths, Tag tag) {
        if (imagePaths == null || imagePaths.isEmpty() || tag == null) return;
        
        logger.info("Quitando etiqueta '{}' de {} imágenes.", tag.getNombre(), imagePaths.size());
        
        for (Path path : imagePaths) {
            Optional<ImagenInfo> imgOpt = imagenDAO.findImagenByPath(path);
            if (imgOpt.isPresent()) {
                tagDAO.removeTagFromImage(imgOpt.get().getId(), tag.getId());
            }
        }
    } // ---FIN de metodo [removeTagFromImages]---

    /**
     * Obtiene todos los discos registrados en la base de datos, incluyendo
     * el conteo de imágenes asociadas.
     * @return Lista de discos.
     */
    public List<Disco> getAllRegisteredDisks() {
        List<Disco> discos = discoDAO.getAllDiscos();
        java.util.Map<Long, Long> counts = discoDAO.getImageCountsByDisk();
        
        for (Disco d : discos) {
            d.setCantidadImagenes(counts.getOrDefault(d.getId(), 0L));
        }
        return discos;
    } // ---FIN de metodo [getAllRegisteredDisks]---

    /**
     * Identifica qué discos del sistema están conectados actualmente y mapea
     * sus números de serie a sus rutas raíz (ej: "F:\\", "D:\\").
     * @return Un mapa de NumeroSerie -> Path de la raíz.
     */
    public Map<String, Path> getConnectedDisks() {
        Map<String, Path> connected = new HashMap<>();
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                Path rootPath = root.toPath();
                volumeService.getVolumeSerialNumber(rootPath).ifPresent(serial -> {
                    connected.put(serial, rootPath);
                });
            }
        }
        return connected;
    } // ---FIN de metodo [getConnectedDisks]---

    /**
     * Asegura que todos los discos conectados actualmente estén registrados en la base de datos.
     * Si un disco es nuevo, lo añade automáticamente.
     */
    public void ensureAllDrivesRegistered() {
        Map<String, Path> connectedMap = getConnectedDisks();
        List<Disco> registered = getAllRegisteredDisks();
        
        java.util.Set<String> registeredSerials = registered.stream()
                .map(Disco::getNumeroSerie)
                .collect(Collectors.toSet());
        
        for (Map.Entry<String, Path> entry : connectedMap.entrySet()) {
            String serial = entry.getKey();
            Path root = entry.getValue();
            
            if (!registeredSerials.contains(serial)) {
                logger.info("Detectado nuevo disco conectado: {}. Registrando en la BD...", root);
                Disco nuevoDisco = new Disco(
                    serial, 
                    volumeService.getVolumeName(root), 
                    root.toString()
                );
                discoDAO.addDisco(nuevoDisco).ifPresent(id -> {
                    logger.debug("Disco {} registrado con ID {}", root, id);
                });
            } else {
                // Si ya existe, nos aseguramos de actualizar la última letra conocida
                // por si ha cambiado (ej: de F: a G:).
                registered.stream()
                    .filter(d -> d.getNumeroSerie().equals(serial))
                    .findFirst()
                    .ifPresent(d -> {
                        if (!root.toString().equalsIgnoreCase(d.getUltimaRutaConocida())) {
                            discoDAO.updateUltimaRuta(d.getId(), root.toString());
                        }
                    });
            }
        }
    } // ---FIN de metodo [ensureAllDrivesRegistered]---

} // --- FIN de clase DataManager ---
