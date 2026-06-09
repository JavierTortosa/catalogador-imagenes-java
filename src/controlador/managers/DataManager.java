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

    private List<Tag> allTagsCache = null;
    private boolean allTagsCacheValid = false;

    public DataManager() {
        this.tagDAO = new TagDAO();
        this.imagenDAO = new ImagenDAO();
        this.discoDAO = new DiscoDAO();
        this.volumeService = new VolumeService();
    } // ---FIN de constructor [DataManager]---


    /**
     * Obtiene la instancia de TagDAO para operaciones directas de CRUD.
     * @return La instancia de TagDAO.
     */
    public TagDAO getTagDAO() {
        return tagDAO;
    } // ---FIN de metodo [getTagDAO]---

    /**
     * Obtiene la instancia de ImagenDAO para operaciones directas de CRUD.
     * @return La instancia de ImagenDAO.
     */
    public ImagenDAO getImagenDAO() {
        return imagenDAO;
    } // ---FIN de metodo [getImagenDAO]---

    /**
     * Obtiene una lista de todos los tags existentes en la base de datos,
     * ordenados alfabéticamente.
     * @return Una lista de objetos Tag.
     */
    public List<Tag> getAllTags() {
        if (!allTagsCacheValid) {
            logger.debug("getAllTags: consultando BD...");
            allTagsCache = tagDAO.getAllTags();
            allTagsCacheValid = true;
            logger.info("Se encontraron {} tags en total.", allTagsCache.size());
        }
        return allTagsCache;
    } // ---FIN de metodo [getAllTags]---

    public void invalidateTagCache() {
        allTagsCacheValid = false;
        allTagsCache = null;
        logger.debug("Caché de tags invalidada.");
    } // ---FIN de metodo [invalidateTagCache]---

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

        List<Long> tagIdsToSearch = new ArrayList<>();
        tagIdsToSearch.add(tag.getId());
        tagIdsToSearch.addAll(
            tagDAO.getAllDescendantTags(tag.getId()).stream()
                  .map(Tag::getId)
                  .collect(Collectors.toList())
        );

        logger.info("Búsqueda jerárquica para '{}' incluye {} tags (IDs).", tag.getNombre(), tagIdsToSearch.size());

        List<String> paths = tagDAO.findImagePathsByTagIds(tagIdsToSearch);
        logger.info("Se encontraron {} imágenes para la jerarquía del tag '{}'.", paths.size(), tag.getNombre());
        return paths;
    } // ---FIN de metodo [getImagePathsForTag]---
    /**
     * Obtiene las rutas de imágenes asociadas a TODOS los tags que tengan el nombre dado,
     * incluyendo los descendientes de cada uno.
     * útil para la "vista lista" donde se busca por nombre de tag sin importar su ubicación.
     * @param tagName El nombre del tag a buscar (ej. "armas").
     * @return Lista de rutas completas de las imágenes encontradas.
     */
    public List<String> getImagePathsForTagName(String tagName) {
        if (tagName == null || tagName.isBlank()) {
            logger.warn("getImagePathsForTagName: nombre de tag vacío.");
            return Collections.emptyList();
        }

        // 1. Encontrar TODOS los tags con ese nombre en cualquier rama.
        List<Tag> allWithName = tagDAO.findTagsByNameAll(tagName.trim().toLowerCase());
        if (allWithName.isEmpty()) {
            logger.debug("No se encontró ningún tag con el nombre '{}'.", tagName);
            return Collections.emptyList();
        }

        // 2. Para cada tag encontrado, incluir también sus descendientes (por ID).
        List<Long> tagIdsToSearch = new ArrayList<>();
        for (Tag tag : allWithName) {
            tagIdsToSearch.add(tag.getId());
            tagDAO.getAllDescendantTags(tag.getId())
                  .forEach(d -> tagIdsToSearch.add(d.getId()));
        }

        logger.info("Búsqueda por nombre '{}' encontró {} tags raíz, {} IDs totales a buscar.",
                tagName, allWithName.size(), tagIdsToSearch.size());

        // 3. Buscar imágenes por todos esos IDs.
        List<String> paths = tagDAO.findImagePathsByTagIds(tagIdsToSearch);
        logger.info("Se encontraron {} imágenes para el nombre de tag '{}'.", paths.size(), tagName);
        return paths;
    } // ---FIN de metodo [getImagePathsForTagName]---

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

    /**
     * Obtiene las rutas completas de todas las imágenes registradas en la base de datos.
     * @return Una lista de strings con las rutas.
     */
    public List<String> getAllImagePaths() {
        return imagenDAO.getAllImagePaths();
    }

    /**
     * Obtiene una lista con los IDs internos de los discos que están actualmente conectados.
     */
    public List<Long> getConnectedDiscoIds() {
        Map<String, Path> connectedMap = getConnectedDisks();
        java.util.Set<String> connectedSerials = connectedMap.keySet();
        return getAllRegisteredDisks().stream()
                .filter(d -> connectedSerials.contains(d.getNumeroSerie()))
                .map(Disco::getId)
                .collect(Collectors.toList());
    }

    /**
     * Resuelve una ruta con notación de puntos (ej. "juegos.blood.bowl").
     * Para cada segmento, busca el tag existente o lo crea como tag de usuario.
     * @param dotPath La ruta con notación de puntos.
     * @return Lista de Tags desde la raíz hasta la hoja, o lista vacía si hay error.
     */
    public List<Tag> createByDotNotation(String dotPath) {
        if (dotPath == null || dotPath.isBlank()) return Collections.emptyList();

        String[] segments = dotPath.split("\\.");
        Long parentId = null;
        List<Tag> path = new ArrayList<>();

        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.isEmpty()) continue;

            Optional<Tag> existing = tagDAO.findTagByNameAndParent(trimmed, parentId);
            Tag current;
            if (existing.isPresent()) {
                current = existing.get();
            } else {
                current = tagDAO.addTag(trimmed, parentId, 0).orElse(null);
            }

            if (current == null) {
                logger.error("No se pudo crear el segmento '{}' de la ruta '{}'", trimmed, dotPath);
                return Collections.emptyList();
            }
            path.add(current);
            parentId = current.getId();
        }

        invalidateTagCache();

        logger.debug("Ruta '{}' creada: {} tags", dotPath, path.size());
        return path;
    } // ---FIN de metodo [createByDotNotation]---

    public List<Tag> resolveDotNotation(String dotPath) {
        if (dotPath == null || dotPath.isBlank()) return Collections.emptyList();

        String[] segments = dotPath.split("\\.");
        Long parentId = null;
        List<Tag> path = new ArrayList<>();

        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.isEmpty()) continue;

            Optional<Tag> existing = tagDAO.findTagByNameAndParent(trimmed, parentId);
            Tag current;
            if (existing.isPresent()) {
                current = existing.get();
            } else {
                // No exact match under expected parent - check globally
                List<Tag> globalMatches = tagDAO.findTagsByNameAll(trimmed);
                if (globalMatches.isEmpty()) {
                    // Doesn't exist anywhere - create new
                    current = tagDAO.addTag(trimmed, parentId, 0).orElse(null);
                } else if (globalMatches.size() == 1) {
                    // Single global match - adopt it (the tag exists, just not under this parent)
                    current = globalMatches.get(0);
                } else {
                    // Multiple global matches - caller must handle disambiguation
                    logger.warn("Ambigüedad: el tag '{}' existe en {} ramas diferentes. Usa la ruta completa para desambiguar.",
                            trimmed, globalMatches.size());
                    return Collections.emptyList();
                }
            }

            if (current == null) {
                logger.error("No se pudo resolver el segmento '{}' de la ruta '{}'", trimmed, dotPath);
                return Collections.emptyList();
            }
            path.add(current);
            parentId = current.getId();
        }

        invalidateTagCache(); // Marcar caché como sucia (se recargará en el próximo getAllTags)

        logger.debug("Ruta '{}' resuelta a {} tags: {}", dotPath, path.size(),
                path.stream().map(Tag::getNombre).collect(Collectors.joining(" > ")));
        return path;
    } // ---FIN de metodo [resolveDotNotation]---

    /**
     * Filtra una lista de rutas de imágenes para dejar sólo aquellas
     * cuya raíz coincida con alguna de las unidades conectadas.
     */
    public List<String> filterConnectedPaths(List<String> paths) {
        if (paths == null || paths.isEmpty()) return Collections.emptyList();
        
        Map<String, Path> connectedMap = getConnectedDisks();
        List<String> connectedRoots = connectedMap.values().stream()
                .map(Path::toString)
                .map(String::toUpperCase)
                .collect(Collectors.toList());
                
        return paths.stream().filter(pathStr -> {
            String upperPath = pathStr.toUpperCase();
            for (String root : connectedRoots) {
                if (upperPath.startsWith(root)) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
    }

    /**
     * Busca la ruta de una imagen a partir de su nombre de archivo.
     * @param nombreArchivo El nombre de la imagen.
     * @return Un Optional con el path como String si existe, o vacío.
     */
    public Optional<String> findPathByFileName(String nombreArchivo) {
        return imagenDAO.findPathByFileName(nombreArchivo);
    }

    /**
     * Mueve un tag a un nuevo padre, con validación completa (anti-bucle,
     * unicidad en destino, protección de tags de sistema).
     * @param tag       El tag a mover.
     * @param newParent El nuevo tag padre (null = mover a la raíz).
     * @return true si el movimiento fue exitoso.
     */
    public boolean moveTag(Tag tag, Tag newParent) {
        if (tag == null) return false;
        Long newParentId = (newParent != null) ? newParent.getId() : null;
        logger.info("Moviendo tag '{}' (ID {}) al padre '{}' (ID {}).",
                tag.getNombre(), tag.getId(),
                (newParent != null ? newParent.getNombre() : "RAÍZ"),
                newParentId);
        boolean ok = tagDAO.moveTag(tag.getId(), newParentId);
        if (ok) invalidateTagCache();
        return ok;
    } // ---FIN de metodo [moveTag]---

    /**
     * Cuenta el número de imágenes afectadas por un tag y toda su jerarquía descendiente.
     * @param tag El tag raíz de la búsqueda.
     * @return El número de imágenes únicas afectadas.
     */
    public int getImageCountForTagRecursive(Tag tag) {
        if (tag == null) return 0;
        return tagDAO.getImageCountForTagRecursive(tag.getId());
    } // ---FIN de metodo [getImageCountForTagRecursive]---

    /**
     * Elimina un tag y TODA su rama de descendientes junto con sus asociaciones de imágenes.
     * @param tag El tag raíz de la rama a eliminar.
     * @return true si la eliminación fue exitosa.
     */
    public boolean deleteTagBranch(Tag tag) {
        if (tag == null) return false;
        logger.info("Eliminando rama completa del tag '{}' (ID {}).", tag.getNombre(), tag.getId());
        boolean ok = tagDAO.deleteTagBranch(tag.getId());
        if (ok) invalidateTagCache();
        return ok;
    } // ---FIN de metodo [deleteTagBranch]---

    /**
     * Construye la ruta completa de un tag en notación punto (ej. "fantasía.armas").
     * Camina hacia arriba por la jerarquía de padres hasta llegar a la raíz.
     * @param tag El tag del que se quiere obtener la ruta completa.
     * @return La ruta completa como String, o el nombre del tag si es raíz.
     */
    public String buildFullPath(Tag tag) {
        if (tag == null) return "";
        Map<Long, Tag> tagById = new HashMap<>();
        for (Tag t : getAllTags()) {
            tagById.put(t.getId(), t);
        }

        StringBuilder sb = new StringBuilder(tag.getNombre());
        Long parentId = tag.getParentId();
        int safetyCounter = 0;
        while (parentId != null && safetyCounter < 50) {
            Tag parent = tagById.get(parentId);
            if (parent == null) break;
            sb.insert(0, parent.getNombre() + ".");
            parentId = parent.getParentId();
            safetyCounter++;
        }
        return sb.toString();
    } // ---FIN de metodo [buildFullPath]---

} // --- FIN de clase DataManager ---
