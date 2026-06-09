package servicios.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.datos.Tag;

/**
 * DAO para gestionar las operaciones CRUD de las entidades Tag
 * y su relación con las imágenes.
 */
public class TagDAO {
    
    private static final Logger logger = LoggerFactory.getLogger(TagDAO.class);
    private Connection connection;

    public TagDAO() {
        this.connection = DatabaseManager.getInstance().getConnection();
    } // ---FIN de constructor [TagDAO]---

    /**
     * Añade un nuevo tag a la base de datos sin un padre especificado (será un tag raíz).
     * @param nombreTag El nombre del tag a crear.
     * @return Un Optional con el objeto Tag.
     */
    public Optional<Tag> addTag(String nombreTag) {
        return addTag(nombreTag, null);
    } // ---FIN de metodo [addTag]---

    /**
     * Añade un nuevo tag a la base de datos si no existe uno con el mismo nombre.
     * El nombre del tag se normaliza a minúsculas antes de la inserción.
     * @param nombreTag El nombre del tag a crear.
     * @param parentId El ID del tag padre (puede ser null para un tag raíz).
     * @return Un Optional con el objeto Tag (con su ID) si se creó o ya existía. Vacío si hubo un error.
     */
    public Optional<Tag> addTag(String nombreTag, Long parentId) {
        return addTag(nombreTag, parentId, 0);
    } // ---FIN de metodo [addTag]---

    /**
     * Añade un nuevo tag a la base de datos si no existe uno con el mismo nombre,
     * permitiendo especificar si es de solo lectura (automático).
     * @param nombreTag El nombre del tag a crear.
     * @param parentId El ID del tag padre (puede ser null para un tag raíz).
     * @param readOnly El indicador de solo lectura (0 = usuario, 1 = automático).
     * @return Un Optional con el objeto Tag (con su ID) si se creó o ya existía. Vacío si hubo un error.
     */
    public Optional<Tag> addTag(String nombreTag, Long parentId, int readOnly) {
        if (nombreTag == null || nombreTag.isBlank()) {
            return Optional.empty();
        }
        
        String nombreNormalizado = nombreTag.trim().toLowerCase();
        
        Optional<Tag> tagExistente = findTagByNameAndParent(nombreNormalizado, parentId);
        if (tagExistente.isPresent()) {
            return tagExistente;
        }

        String sql = "INSERT INTO tags(nombre, parent_id, read_only) VALUES(?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, nombreNormalizado);
            if (parentId == null) {
                pstmt.setNull(2, java.sql.Types.INTEGER);
            } else {
                pstmt.setLong(2, parentId);
            }
            pstmt.setInt(3, readOnly);
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long id = generatedKeys.getLong(1);
                        logger.trace("Tag '{}' añadido a la BD con ID {} y parent_id {}", nombreNormalizado, id, parentId);
                        return Optional.of(new Tag(id, nombreNormalizado, parentId, readOnly));
                    }
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 19) { // UNIQUE constraint failed
                logger.trace("Intento de añadir tag duplicado, pero la búsqueda previa falló. Se recupera el existente.");
                return findTagByNameAndParent(nombreNormalizado, parentId);
            }
            logger.error("Error al añadir tag a la BD: " + e.getMessage(), e);
        }
        return Optional.empty();
    } // ---FIN de metodo [addTag]---

    /**
     * Busca un tag por su nombre y su padre (insensible a mayúsculas/minúsculas).
     * @param nombre El nombre del tag a buscar.
     * @param parentId El ID del tag padre (puede ser null).
     * @return Un Optional con el Tag si se encuentra.
     */
    public Optional<Tag> findTagByNameAndParent(String nombre, Long parentId) {
        String sql = (parentId == null) ? "SELECT * FROM tags WHERE nombre = ? AND parent_id IS NULL" 
                                        : "SELECT * FROM tags WHERE nombre = ? AND parent_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nombre.trim().toLowerCase());
            if (parentId != null) {
                pstmt.setLong(2, parentId);
            }
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToTag(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al buscar tag por nombre y padre: " + e.getMessage(), e);
        }
        return Optional.empty();
    } // ---FIN de metodo [findTagByNameAndParent]---

    /**
     * Busca el primer tag que coincida por nombre.
     * NOTA: Dado que ahora se permiten nombres duplicados si tienen distinto padre,
     * este método puede devolver cualquier tag con ese nombre.
     * Úselo con precaución.
     * @param nombre El nombre del tag a buscar.
     * @return Un Optional con el Tag si se encuentra.
     */
    public Optional<Tag> findTagByName(String nombre) {
        String sql = "SELECT * FROM tags WHERE nombre = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nombre.trim().toLowerCase());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToTag(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al buscar tag por nombre: " + e.getMessage(), e);
        }
        return Optional.empty();
    } // ---FIN de metodo [findTagByName]---
    
    /**
     * Busca TODOS los tags que coincidan exactamente con el nombre dado.
     * A diferencia de {@link #findTagByName(String)}, este método devuelve
     * todas las ocurrencias del nombre en la jerarquía (porque ahora se permiten
     * nombres duplicados en distintas ramas).
     * @param nombre El nombre del tag a buscar.
     * @return Una lista con todos los tags que tienen ese nombre.
     */
    public List<Tag> findTagsByNameAll(String nombre) {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT * FROM tags WHERE nombre = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nombre.trim().toLowerCase());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tags.add(mapResultSetToTag(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al buscar todos los tags por nombre: " + e.getMessage(), e);
        }
        return tags;
    } // ---FIN de metodo [findTagsByNameAll]---
    
    /**
     * Busca un tag por su ID.
     * @param id El ID del tag.
     * @return Un Optional con el Tag si se encuentra.
     */
    public Optional<Tag> findTagById(long id) {
        String sql = "SELECT * FROM tags WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToTag(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al buscar tag por ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    } // ---FIN de metodo [findTagById]---
    
    /**
     * Obtiene la ruta completa de un tag como cadena (ej. "juegos > blood bowl").
     * Camina hacia arriba por la jerarquía de padres hasta la raíz.
     * @param tagId El ID del tag.
     * @return La ruta completa en formato "padre > hijo", o el nombre del tag si es raíz.
     */
    public String getTagFullPath(long tagId) {
        StringBuilder sb = new StringBuilder();
        Long currentId = tagId;
        while (currentId != null) {
            Optional<Tag> tagOpt = findTagById(currentId);
            if (tagOpt.isEmpty()) break;
            Tag tag = tagOpt.get();
            if (sb.length() > 0) {
                sb.insert(0, " > ");
            }
            sb.insert(0, tag.getNombre());
            currentId = tag.getParentId();
        }
        return sb.toString();
    } // ---FIN de metodo [getTagFullPath]---

    /**
     * Obtiene todos los tags de la base de datos.
     * @return Una lista de todos los tags.
     */
    public List<Tag> getAllTags() {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT * FROM tags ORDER BY nombre ASC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tags.add(mapResultSetToTag(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al obtener todos los tags.", e);
        }
        return tags;
    } // ---FIN de metodo [getAllTags]---

    /**
     * Obtiene solo los tags que son raíz (no tienen padre).
     * @return Una lista de tags raíz.
     */
    public List<Tag> getRootTags() {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT * FROM tags WHERE parent_id IS NULL ORDER BY nombre ASC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tags.add(mapResultSetToTag(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al obtener los tags raíz.", e);
        }
        return tags;
    } // ---FIN de metodo [getRootTags]---

    /**
     * Obtiene los tags hijos directos de un tag padre específico.
     * @param parentId El ID del tag padre.
     * @return Una lista de tags hijos.
     */
    public List<Tag> getChildTags(long parentId) {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT * FROM tags WHERE parent_id = ? ORDER BY nombre ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, parentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tags.add(mapResultSetToTag(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al obtener los tags hijos para el padre ID " + parentId, e);
        }
        return tags;
    } // ---FIN de metodo [getChildTags]---

    /**
     * Asocia uno o más tags a una o más imágenes en una sola transacción SQL.
     * @param imagenIds Lista de IDs de las imágenes.
     * @param tagIds Lista de IDs de los tags a asignar.
     * @return true si la operación se realizó con éxito.
     */
    public boolean assignTagsToImages(List<Long> imagenIds, List<Long> tagIds) {
        if (imagenIds == null || imagenIds.isEmpty() || tagIds == null || tagIds.isEmpty()) return true;
        
        String sql = "INSERT OR IGNORE INTO imagen_tags(imagen_id, tag_id) VALUES(?,?)";
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                for (Long imagenId : imagenIds) {
                    for (Long tagId : tagIds) {
                        pstmt.setLong(1, imagenId);
                        pstmt.setLong(2, tagId);
                        pstmt.addBatch();
                    }
                }
                pstmt.executeBatch();
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                logger.error("Error al asignar múltiples tags a múltiples imágenes (rollback)", e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Error manejando transacción de asignación masiva de tags", e);
        }
        return false;
    } // ---FIN de metodo [assignTagsToImages]---

    /**
     * Asocia un tag a una imagen en la tabla intermedia.
     * @param imagenId El ID de la imagen.
     * @param tagId El ID del tag.
     * @return true si la asociación se creó con éxito.
     */
    public boolean assignTagToImage(long imagenId, long tagId) {
        String sql = "INSERT OR IGNORE INTO imagen_tags(imagen_id, tag_id) VALUES(?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, imagenId);
            pstmt.setLong(2, tagId);
            pstmt.executeUpdate();
            return true; // "INSERT OR IGNORE" no lanza error por duplicados, así que asumimos éxito.
        } catch (SQLException e) {
            logger.error("Error al asignar tag {} a imagen {}", tagId, imagenId, e);
        }
        return false;
    } // ---FIN de metodo [assignTagToImage]---

    /**
     * Elimina la asociación entre un tag y una imagen.
     * @param imagenId El ID de la imagen.
     * @param tagId El ID del tag.
     * @return true si la desasociación fue exitosa.
     */
    public boolean removeTagFromImage(long imagenId, long tagId) {
        String sql = "DELETE FROM imagen_tags WHERE imagen_id = ? AND tag_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, imagenId);
            pstmt.setLong(2, tagId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Error al quitar tag {} de imagen {}", tagId, imagenId, e);
        }
        return false;
    } // ---FIN de metodo [removeTagFromImage]---
    
    /**
     * Obtiene todos los tags asociados a una imagen específica.
     * @param imagenId El ID de la imagen.
     * @return Una lista de los tags de esa imagen.
     */
    public List<Tag> getTagsForImage(long imagenId) {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT t.* FROM tags t " +
                     "JOIN imagen_tags it ON t.id = it.tag_id " +
                     "WHERE it.imagen_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, imagenId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tags.add(mapResultSetToTag(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al obtener tags para la imagen ID " + imagenId, e);
        }
        return tags;
    } // ---FIN de metodo [getTagsForImage]---
    
    /**
     * Mapea una fila de un ResultSet a un objeto Tag.
     */
    private Tag mapResultSetToTag(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String nombre = rs.getString("nombre");
        long parentId = rs.getLong("parent_id");
        
        // Si getLong devuelve 0 y el valor en la BD era NULL, wasNull() será true.
        Long parentIdObject = rs.wasNull() ? null : parentId;
        int readOnly = rs.getInt("read_only");
        
        return new Tag(id, nombre, parentIdObject, readOnly);
    } // ---FIN de metodo [mapResultSetToTag]---
    
    
    /**
     * Busca todas las rutas completas de imágenes que están asociadas a CUALQUIERA
     * de los nombres de tags proporcionados.
     * 
     * @param tagNames Una lista de nombres de tags a buscar.
     * @return Una lista de Strings con las rutas completas de las imágenes encontradas.
     */
    public List<String> findImagePathsByTagNames(List<String> tagNames) {
        List<String> imagePaths = new ArrayList<>();
        if (tagNames == null || tagNames.isEmpty()) {
            return imagePaths;
        }

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < tagNames.size(); i++) {
            placeholders.append("?");
            if (i < tagNames.size() - 1) {
                placeholders.append(",");
            }
        }

        String sql = "SELECT DISTINCT i.ruta_completa " +
                     "FROM imagenes i " +
                     "JOIN imagen_tags it ON i.id = it.imagen_id " +
                     "JOIN tags t ON it.tag_id = t.id " +
                     "WHERE t.nombre IN (" + placeholders.toString() + ")";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < tagNames.size(); i++) {
                pstmt.setString(i + 1, tagNames.get(i).trim().toLowerCase());
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                imagePaths.add(rs.getString("ruta_completa"));
            }
        } catch (SQLException e) {
            logger.error("Error al buscar imágenes por nombres de tags.", e);
        }
        
        logger.debug("Búsqueda por tags {} devolvió {} resultados.", tagNames, imagePaths.size());
        return imagePaths;
    } // ---FIN de metodo [findImagePathsByTagNames]---

    public List<String> findImagePathsByTagIds(List<Long> tagIds) {
        List<String> imagePaths = new ArrayList<>();
        if (tagIds == null || tagIds.isEmpty()) {
            return imagePaths;
        }

        String placeholders = tagIds.stream().map(id -> "?").collect(Collectors.joining(","));

        String sql = "SELECT DISTINCT i.ruta_completa " +
                     "FROM imagenes i " +
                     "JOIN imagen_tags it ON i.id = it.imagen_id " +
                     "WHERE it.tag_id IN (" + placeholders + ")";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < tagIds.size(); i++) {
                pstmt.setLong(i + 1, tagIds.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                imagePaths.add(rs.getString("ruta_completa"));
            }
        } catch (SQLException e) {
            logger.error("Error al buscar imágenes por IDs de tags.", e);
        }

        logger.debug("Búsqueda por IDs de tags devolvió {} resultados.", imagePaths.size());
        return imagePaths;
    } // ---FIN de metodo [findImagePathsByTagIds]---

    /**
     * Obtiene una lista de todos los tags descendientes (hijos, nietos, etc.) de un tag padre,
     * utilizando una consulta CTE recursiva en la base de datos para máxima eficiencia.
     * @param parentTagId El ID del tag del que se quieren encontrar los descendientes.
     * @return Una lista con todos los tags descendientes.
     */
    public List<Tag> getAllDescendantTags(long parentTagId) {
        List<Tag> descendants = new ArrayList<>();
        String sql = "WITH RECURSIVE CteTags AS (" +
                     "  SELECT * FROM tags WHERE parent_id = ? " +
                     "  UNION ALL " +
                     "  SELECT t.* FROM tags t " +
                     "  JOIN CteTags ct ON t.parent_id = ct.id" +
                     ") " +
                     "SELECT * FROM CteTags";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, parentTagId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                descendants.add(mapResultSetToTag(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al obtener tags descendientes recursivamente para ID " + parentTagId, e);
        }
        return descendants;
    } // ---FIN de metodo [getAllDescendantTags]---
    /**
     * Cuenta cuántas imágenes tienen asignado directamente un tag.
     * @param tagId ID del tag.
     * @return El número de imágenes.
     */
    public int getImageCountForTag(long tagId) {
        String sql = "SELECT COUNT(*) FROM imagen_tags WHERE tag_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, tagId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error al contar imágenes para el tag ID " + tagId, e);
        }
        return 0;
    } // ---FIN de metodo [getImageCountForTag]---

    /**
     * Cuenta cuántas imágenes en discos conectados tienen asignado directamente un tag.
     * @param tagId ID del tag.
     * @param connectedDiscoIds Lista de IDs de los discos conectados actualmente.
     * @return El número de imágenes disponibles para este tag directo.
     */
    public int getAvailableImageCountForTag(long tagId, List<Long> connectedDiscoIds) {
        if (connectedDiscoIds == null || connectedDiscoIds.isEmpty()) return 0;

        String discoPlaceholders = connectedDiscoIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT COUNT(DISTINCT it.imagen_id) FROM imagen_tags it " +
                     "JOIN imagenes i ON it.imagen_id = i.id " +
                     "WHERE it.tag_id = ? AND i.disco_id IN (" + discoPlaceholders + ")";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int idx = 1;
            pstmt.setLong(idx++, tagId);
            for (Long did : connectedDiscoIds) pstmt.setLong(idx++, did);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Error al contar imágenes disponibles para el tag ID " + tagId, e);
        }
        return 0;
    } // ---FIN de metodo [getAvailableImageCountForTag]---

    /**
     * Cuenta cuántas imágenes tienen asignado un tag o cualquiera de sus descendientes.
     * @param tagId ID del tag raíz de la búsqueda.
     * @return El número total de imágenes únicas.
     */
    public int getImageCountForTagRecursive(long tagId) {
        // Obtenemos todos los descendientes (incluyendo el actual)
        List<Tag> descendants = getAllDescendantTags(tagId);
        List<Long> ids = new ArrayList<>();
        ids.add(tagId);
        for (Tag t : descendants) ids.add(t.getId());

        // Construimos los placeholders para el IN
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT COUNT(DISTINCT imagen_id) FROM imagen_tags WHERE tag_id IN (" + placeholders + ")";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) {
                pstmt.setLong(i + 1, ids.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error al contar imágenes recurrentes para el tag ID " + tagId, e);
        }
        return 0;
    } // ---FIN de metodo [getImageCountForTagRecursive]---

    /**
     * Cuenta cuántas imágenes tienen asignado un tag o cualquiera de sus descendientes,
     * PERO filtrando sólo aquellas imágenes cuyo disco_id esté en la lista de discos conectados.
     * @param tagId ID del tag raíz de la búsqueda.
     * @param connectedDiscoIds Lista de IDs de los discos conectados actualmente.
     * @return El número de imágenes disponibles.
     */
    public int getAvailableImageCountForTagRecursive(long tagId, List<Long> connectedDiscoIds) {
        if (connectedDiscoIds == null || connectedDiscoIds.isEmpty()) return 0;
        
        List<Tag> descendants = getAllDescendantTags(tagId);
        List<Long> tagIds = new ArrayList<>();
        tagIds.add(tagId);
        for (Tag t : descendants) tagIds.add(t.getId());

        String tagPlaceholders = tagIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String discoPlaceholders = connectedDiscoIds.stream().map(id -> "?").collect(Collectors.joining(","));
        
        String sql = "SELECT COUNT(DISTINCT it.imagen_id) FROM imagen_tags it " +
                     "JOIN imagenes i ON it.imagen_id = i.id " +
                     "WHERE it.tag_id IN (" + tagPlaceholders + ") AND i.disco_id IN (" + discoPlaceholders + ")";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int paramIndex = 1;
            for (Long id : tagIds) {
                pstmt.setLong(paramIndex++, id);
            }
            for (Long id : connectedDiscoIds) {
                pstmt.setLong(paramIndex++, id);
            }
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error al contar imágenes disponibles recurrentes para el tag ID " + tagId, e);
        }
        return 0;
    } // ---FIN de metodo [getAvailableImageCountForTagRecursive]---

    /**
     * Elimina un tag de la base de datos, junto con todas sus asociaciones
     * en la tabla imagen_tags. Los tags hijos se reasignan al padre del tag eliminado.
     * @param tagId El ID del tag a eliminar.
     * @return true si se eliminó con éxito.
     */
    public boolean deleteTag(long tagId) {
        try {
            // 1. Obtener el parent_id del tag a eliminar para reasignar hijos
            Tag tagToDelete = null;
            String sqlGet = "SELECT * FROM tags WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sqlGet)) {
                pstmt.setLong(1, tagId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    tagToDelete = mapResultSetToTag(rs);
                }
            }
            if (tagToDelete == null) {
                logger.warn("No se encontró el tag con ID {} para eliminar.", tagId);
                return false;
            }

            // 2. Reasignar los hijos directos al padre del tag eliminado
            String sqlUpdateChildren = "UPDATE tags SET parent_id = ? WHERE parent_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sqlUpdateChildren)) {
                if (tagToDelete.getParentId() == null) {
                    pstmt.setNull(1, java.sql.Types.INTEGER);
                } else {
                    pstmt.setLong(1, tagToDelete.getParentId());
                }
                pstmt.setLong(2, tagId);
                pstmt.executeUpdate();
            }

            // 3. Eliminar las asociaciones con imágenes
            String sqlDeleteAssocs = "DELETE FROM imagen_tags WHERE tag_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sqlDeleteAssocs)) {
                pstmt.setLong(1, tagId);
                pstmt.executeUpdate();
            }

            // 4. Eliminar el tag
            String sqlDeleteTag = "DELETE FROM tags WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sqlDeleteTag)) {
                pstmt.setLong(1, tagId);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    logger.info("Tag con ID {} eliminado correctamente.", tagId);
                    return true;
                }
            }
        } catch (SQLException e) {
            logger.error("Error al eliminar el tag con ID " + tagId, e);
        }
        return false;
    } // ---FIN de metodo [deleteTag]---

    /**
     * Actualiza el nombre de un tag existente.
     * @param tagId El ID del tag a renombrar.
     * @param nuevoNombre El nuevo nombre para el tag.
     * @return true si se actualizó con éxito.
     */
    public boolean updateTagName(long tagId, String nuevoNombre) {
        if (nuevoNombre == null || nuevoNombre.isBlank()) {
            return false;
        }
        String nombreNormalizado = nuevoNombre.trim().toLowerCase();
        
        // Obtener el tag actual para conocer su parentId
        Tag tagToDelete = null;
        String sqlGet = "SELECT * FROM tags WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sqlGet)) {
            pstmt.setLong(1, tagId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                tagToDelete = mapResultSetToTag(rs);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener tag antes de renombrar", e);
            return false;
        }
        
        if (tagToDelete == null) return false;

        // Comprobar si ya existe un tag con ese nombre en el mismo nivel (mismo padre)
        Optional<Tag> existing = findTagByNameAndParent(nombreNormalizado, tagToDelete.getParentId());
        if (existing.isPresent() && existing.get().getId() != tagId) {
            logger.warn("Ya existe un tag con el nombre '{}' en el mismo nivel. No se puede renombrar.", nombreNormalizado);
            return false;
        }
        
        String sql = "UPDATE tags SET nombre = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nombreNormalizado);
            pstmt.setLong(2, tagId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Tag con ID {} renombrado a '{}'.", tagId, nombreNormalizado);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error al renombrar el tag con ID " + tagId, e);
        }
        return false;
    } // ---FIN de metodo [updateTagName]---

    /**
     * Elimina todas las asociaciones de tags para una imagen específica.
     * @param imagenId ID de la imagen.
     * @return true si la eliminación tuvo éxito.
     */
    public boolean clearTagsForImage(long imagenId) {
        String sql = "DELETE FROM imagen_tags WHERE imagen_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, imagenId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Error al limpiar tags para la imagen ID " + imagenId, e);
        }
        return false;
    }

    /**
     * Mueve un tag (cambia su parent_id) a un nuevo padre.
     * Realiza las siguientes validaciones antes de ejecutar:
     * <ul>
     *   <li>El tag no puede ser de tipo read_only.</li>
     *   <li>El nuevo padre no puede ser un descendiente del tag (anti-bucle).</li>
     *   <li>No puede existir ya un tag con el mismo nombre en el nuevo nivel.</li>
     * </ul>
     * @param tagId       El ID del tag a mover.
     * @param newParentId El nuevo ID padre (null = raíz).
     * @return true si la operación fue exitosa.
     */
    public boolean moveTag(long tagId, Long newParentId) {
        // 1. Obtener el tag actual
        Tag tagToMove = null;
        try (PreparedStatement pstmt = connection.prepareStatement("SELECT * FROM tags WHERE id = ?")) {
            pstmt.setLong(1, tagId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) tagToMove = mapResultSetToTag(rs);
        } catch (SQLException e) {
            logger.error("moveTag: error obteniendo tag a mover", e);
            return false;
        }
        if (tagToMove == null) {
            logger.warn("moveTag: no se encontró el tag con ID {}", tagId);
            return false;
        }
        // 2. No mover tags del sistema
        if (tagToMove.isReadOnly()) {
            logger.warn("moveTag: el tag '{}' es read_only y no se puede mover.", tagToMove.getNombre());
            return false;
        }
        // 3. Anti-bucle: el nuevo padre NO puede ser un descendiente del tag
        if (newParentId != null) {
            List<Tag> descendants = getAllDescendantTags(tagId);
            boolean isDescendant = descendants.stream().anyMatch(d -> d.getId() == newParentId);
            if (isDescendant || newParentId == tagId) {
                logger.warn("moveTag: el nuevo padre (ID {}) es descendiente del tag (ID {}). Operación bloqueada.", newParentId, tagId);
                return false;
            }
        }
        // 4. UNIQUE constraint: no puede haber otro tag con el mismo nombre en el nuevo nivel
        Optional<Tag> existing = findTagByNameAndParent(tagToMove.getNombre(), newParentId);
        if (existing.isPresent() && existing.get().getId() != tagId) {
            logger.warn("moveTag: ya existe un tag con el nombre '{}' en el nivel destino.", tagToMove.getNombre());
            return false;
        }
        // 5. Ejecutar el movimiento
        String sql = "UPDATE tags SET parent_id = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            if (newParentId == null) {
                pstmt.setNull(1, java.sql.Types.INTEGER);
            } else {
                pstmt.setLong(1, newParentId);
            }
            pstmt.setLong(2, tagId);
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                logger.info("Tag '{}' (ID {}) movido al padre ID {}.", tagToMove.getNombre(), tagId, newParentId);
                return true;
            }
        } catch (SQLException e) {
            logger.error("moveTag: error al actualizar parent_id para tag ID " + tagId, e);
        }
        return false;
    } // ---FIN de metodo [moveTag]---

    /**
     * Elimina un tag y TODA su rama descendiente de forma recursiva,
     * así como todas las asociaciones de imagen_tags relacionadas.
     * Utiliza una CTE recursiva para obtener todos los IDs de la rama.
     * @param tagId El ID raíz de la rama a eliminar.
     * @return true si la operación se completó con éxito.
     */
    public boolean deleteTagBranch(long tagId) {
        // Obtener todos los IDs de la rama (el propio tag + todos sus descendientes)
        List<Long> branchIds = new ArrayList<>();
        branchIds.add(tagId);
        List<Tag> descendants = getAllDescendantTags(tagId);
        for (Tag d : descendants) branchIds.add(d.getId());

        String placeholders = branchIds.stream().map(id -> "?").collect(Collectors.joining(","));

        try {
            connection.setAutoCommit(false);
            try {
                // 1. Eliminar asociaciones imagen_tags de toda la rama
                String sqlDeleteAssocs = "DELETE FROM imagen_tags WHERE tag_id IN (" + placeholders + ")";
                try (PreparedStatement pstmt = connection.prepareStatement(sqlDeleteAssocs)) {
                    for (int i = 0; i < branchIds.size(); i++) pstmt.setLong(i + 1, branchIds.get(i));
                    pstmt.executeUpdate();
                }
                // 2. Eliminar todos los tags de la rama
                String sqlDeleteTags = "DELETE FROM tags WHERE id IN (" + placeholders + ")";
                try (PreparedStatement pstmt = connection.prepareStatement(sqlDeleteTags)) {
                    for (int i = 0; i < branchIds.size(); i++) pstmt.setLong(i + 1, branchIds.get(i));
                    int affected = pstmt.executeUpdate();
                    connection.commit();
                    logger.info("Rama del tag ID {} eliminada: {} tag(s) borrado(s).", tagId, affected);
                    return affected > 0;
                }
            } catch (SQLException e) {
                connection.rollback();
                logger.error("deleteTagBranch: error durante la eliminación de la rama (rollback)", e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("deleteTagBranch: error manejando transacción", e);
        }
        return false;
    } // ---FIN de metodo [deleteTagBranch]---

    /**
     * Cuenta cuántos tags hijos directos tiene un tag.
     * @param tagId ID del tag padre.
     * @return Número de hijos directos.
     */
    public int getDirectChildCount(long tagId) {
        String sql = "SELECT COUNT(*) FROM tags WHERE parent_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, tagId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("getDirectChildCount: error para tag ID " + tagId, e);
        }
        return 0;
    } // ---FIN de metodo [getDirectChildCount]---

    /**
     * Obtiene los tags que no están asociados a ninguna imagen y no tienen hijos.
     */
    public List<Tag> getUnusedTags() {
        List<Tag> unusedTags = new ArrayList<>();
        String sql = "SELECT t.* FROM tags t " +
                     "LEFT JOIN imagen_tags it ON t.id = it.tag_id " +
                     "LEFT JOIN tags children ON t.id = children.parent_id " +
                     "WHERE it.tag_id IS NULL AND children.id IS NULL";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                unusedTags.add(mapResultSetToTag(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al obtener tags sin uso.", e);
        }
        return unusedTags;
    } // ---FIN de metodo [getUnusedTags]---

    /**
     * Fusiona dos tags: mueve las asociaciones de sourceId a targetId y elimina sourceId.
     */
    public boolean mergeTags(long sourceId, long targetId) {
        if (sourceId == targetId) return false;
        try {
            connection.setAutoCommit(false);
            
            // 1. Mover asociaciones ignorando duplicados (INSERT OR IGNORE)
            String sqlMoveAssoc = "INSERT OR IGNORE INTO imagen_tags (imagen_id, tag_id) " +
                                  "SELECT imagen_id, ? FROM imagen_tags WHERE tag_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sqlMoveAssoc)) {
                pstmt.setLong(1, targetId);
                pstmt.setLong(2, sourceId);
                pstmt.executeUpdate();
            }
            
            // 2. Eliminar el tag original (sus asociaciones y el tag en sí)
            boolean ok = deleteTagBranch(sourceId);
            
            if (ok) {
                connection.commit();
                logger.info("Tag {} fusionado dentro del Tag {}.", sourceId, targetId);
                return true;
            } else {
                connection.rollback();
                return false;
            }
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) {}
            logger.error("Error al fusionar tags", e);
            return false;
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ex) {}
        }
    } // ---FIN de metodo [mergeTags]---

    /**
     * Calcula los conteos de imágenes (total y disponibles) para TODOS los tags
     * usando 2 consultas SQL masivas en lugar de N×2 consultas individuales.
     * @param connectedDiscoIds Lista de IDs de discos conectados (puede ser null o vacío).
     * @return Mapa de tag_id → int[]{available, total}
     */
    public Map<Long, int[]> computeAllTagCountsBulk(List<Long> connectedDiscoIds) {
        Map<Long, int[]> result = new HashMap<>();

        // Consulta 1: Total de imágenes por tag (incluyendo descendientes)
        String totalSql = "WITH RECURSIVE tag_tree AS ("
            + "SELECT id AS ancestor_id, id AS descendant_id FROM tags "
            + "UNION ALL "
            + "SELECT tt.ancestor_id, t.id FROM tags t "
            + "JOIN tag_tree tt ON t.parent_id = tt.descendant_id"
            + ") SELECT tt.ancestor_id, COUNT(DISTINCT it.imagen_id) "
            + "FROM tag_tree tt "
            + "LEFT JOIN imagen_tags it ON it.tag_id = tt.descendant_id "
            + "GROUP BY tt.ancestor_id";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(totalSql)) {
            while (rs.next()) {
                long tagId = rs.getLong(1);
                int total = rs.getInt(2);
                result.put(tagId, new int[]{0, total});
            }
        } catch (SQLException e) {
            logger.error("Error en computeAllTagCountsBulk (total): " + e.getMessage(), e);
            return result;
        }

        if (connectedDiscoIds == null || connectedDiscoIds.isEmpty()) {
            // Sin filtro de discos: available = total
            for (Map.Entry<Long, int[]> entry : result.entrySet()) {
                entry.getValue()[0] = entry.getValue()[1];
            }
            return result;
        }

        // Consulta 2: Imágenes disponibles (solo discos conectados) por tag
        String discoPlaceholders = connectedDiscoIds.stream()
            .map(id -> "?").collect(Collectors.joining(","));

        String availSql = "WITH RECURSIVE tag_tree AS ("
            + "SELECT id AS ancestor_id, id AS descendant_id FROM tags "
            + "UNION ALL "
            + "SELECT tt.ancestor_id, t.id FROM tags t "
            + "JOIN tag_tree tt ON t.parent_id = tt.descendant_id"
            + ") SELECT tt.ancestor_id, COUNT(DISTINCT it.imagen_id) "
            + "FROM tag_tree tt "
            + "JOIN imagen_tags it ON it.tag_id = tt.descendant_id "
            + "JOIN imagenes i ON i.id = it.imagen_id "
            + "WHERE i.disco_id IN (" + discoPlaceholders + ") "
            + "GROUP BY tt.ancestor_id";

        try (PreparedStatement pstmt = connection.prepareStatement(availSql)) {
            for (int i = 0; i < connectedDiscoIds.size(); i++) {
                pstmt.setLong(i + 1, connectedDiscoIds.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                long tagId = rs.getLong(1);
                int available = rs.getInt(2);
                int[] counts = result.get(tagId);
                if (counts != null) {
                    counts[0] = available;
                }
            }
        } catch (SQLException e) {
            logger.error("Error en computeAllTagCountsBulk (available): " + e.getMessage(), e);
        }

        logger.debug("computeAllTagCountsBulk: {} tags procesados en 2 consultas SQL.", result.size());
        return result;
    } // ---FIN de metodo [computeAllTagCountsBulk]---

} // --- FIN de clase TagDAO ---