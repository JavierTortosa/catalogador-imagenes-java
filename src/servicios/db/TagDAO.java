package servicios.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
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
        if (nombreTag == null || nombreTag.isBlank()) {
            return Optional.empty();
        }
        
        String nombreNormalizado = nombreTag.trim().toLowerCase();
        
        Optional<Tag> tagExistente = findTagByName(nombreNormalizado);
        if (tagExistente.isPresent()) {
            return tagExistente;
        }

        String sql = "INSERT INTO tags(nombre, parent_id) VALUES(?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, nombreNormalizado);
            if (parentId == null) {
                pstmt.setNull(2, java.sql.Types.INTEGER);
            } else {
                pstmt.setLong(2, parentId);
            }
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long id = generatedKeys.getLong(1);
                        logger.trace("Tag '{}' añadido a la BD con ID {} y parent_id {}", nombreNormalizado, id, parentId);
                        return Optional.of(new Tag(id, nombreNormalizado, parentId));
                    }
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 19) { // UNIQUE constraint failed
                logger.trace("Intento de añadir tag duplicado, pero la búsqueda previa falló. Se recupera el existente.");
                return findTagByName(nombreNormalizado);
            }
            logger.error("Error al añadir tag a la BD: " + e.getMessage(), e);
        }
        return Optional.empty();
    } // ---FIN de metodo [addTag]---

    /**
     * Busca un tag por su nombre (insensible a mayúsculas/minúsculas).
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
        
        return new Tag(id, nombre, parentIdObject);
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
    
    
    /**
     * Obtiene una lista de todos los tags descendientes (hijos, nietos, etc.) de un tag padre.
     * @param parentTagId El ID del tag del que se quieren encontrar los descendientes.
     * @return Una lista con todos los tags descendientes.
     */
    public List<Tag> getAllDescendantTags(long parentTagId) {
        List<Tag> descendants = new ArrayList<>();
        findDescendantsRecursive(parentTagId, descendants);
        return descendants;
    } // ---FIN de metodo [getAllDescendantTags]---

    /**
     * Método auxiliar recursivo para encontrar todos los descendientes.
     * @param parentId El ID del padre actual.
     * @param accumulator La lista donde se acumulan los resultados.
     */
    private void findDescendantsRecursive(long parentId, List<Tag> accumulator) {
        List<Tag> directChildren = getChildTags(parentId);
        for (Tag child : directChildren) {
            accumulator.add(child);
            // Llamada recursiva para encontrar los hijos de este hijo.
            findDescendantsRecursive(child.getId(), accumulator);
        }
    } // ---FIN de metodo [findDescendantsRecursive]---
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
        
        // Comprobar si ya existe un tag con ese nombre
        Optional<Tag> existing = findTagByName(nombreNormalizado);
        if (existing.isPresent() && existing.get().getId() != tagId) {
            logger.warn("Ya existe un tag con el nombre '{}'. No se puede renombrar.", nombreNormalizado);
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

} // --- FIN de clase TagDAO ---