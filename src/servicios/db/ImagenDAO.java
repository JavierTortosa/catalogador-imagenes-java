package servicios.db;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.datos.ImagenInfo;

/**
 * DAO (Data Access Object) para gestionar las operaciones CRUD (Crear, Leer, Actualizar, Borrar)
 * de las entidades ImagenInfo en la base de datos.
 */
public class ImagenDAO {

    private static final Logger logger = LoggerFactory.getLogger(ImagenDAO.class);
    private Connection connection;

    /**
     * Constructor que recibe la conexión a la base de datos.
     */
    public ImagenDAO() {
        this.connection = DatabaseManager.getInstance().getConnection();
    } // ---FIN de constructor [ImagenDAO]---

    /**
     * Añade una nueva imagen a la base de datos.
     * @param imagen La información de la imagen a guardar.
     * @return Un Optional que contiene el ID generado para la imagen si tuvo éxito, o un Optional vacío si falló.
     */
    public Optional<Long> addImagen(ImagenInfo imagen) {
        String sql = "INSERT INTO imagenes(ruta_completa, nombre_archivo, fecha_modificacion, tamano_bytes, disco_id, ruta_relativa, fecha_adicion) VALUES(?,?,?,?,?,?,?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, imagen.getRutaCompleta());
            pstmt.setString(2, imagen.getNombreArchivo());
            pstmt.setLong(3, imagen.getFechaModificacion());
            pstmt.setLong(4, imagen.getTamanoBytes());
            pstmt.setLong(5, imagen.getDiscoId());
            pstmt.setString(6, imagen.getRutaRelativa());
            pstmt.setLong(7, imagen.getFechaAdicion());
            
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long id = generatedKeys.getLong(1);
                        imagen.setId(id); // Actualizamos el objeto original con el ID
                        logger.trace("Imagen añadida a la BD con ID {}: {}", id, imagen.getNombreArchivo());
                        return Optional.of(id);
                    }
                }
            }
        } catch (SQLException e) {
            // El código de error 19 para SQLite es "CONSTRAINT FAILED". Usamos esto para detectar duplicados.
            if (e.getErrorCode() == 19 && e.getMessage().contains("UNIQUE constraint failed: imagenes.ruta_completa")) {
                logger.trace("La imagen ya existe en la BD (ruta duplicada): {}", imagen.getRutaCompleta());
            } else {
                logger.error("Error al añadir imagen a la BD: " + e.getMessage(), e);
            }
        }
        return Optional.empty();
    } // ---FIN de metodo [addImagen]---

    /**
     * Busca una imagen por su ruta completa.
     * @param ruta La ruta completa del archivo.
     * @return Un Optional con el ImagenInfo si se encuentra, o vacío si no.
     */
    public Optional<ImagenInfo> findImagenByPath(Path ruta) {
        String sql = "SELECT * FROM imagenes WHERE ruta_completa = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ruta.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToImagenInfo(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al buscar imagen por ruta: " + e.getMessage(), e);
        }
        return Optional.empty();
    } // ---FIN de metodo [findImagenByPath]---
    
    /**
     * Actualiza la ruta de una imagen, identificado por su ID.
     * Esencial para cuando se mueven archivos.
     * @param id El ID de la imagen en la BD.
     * @param nuevaRuta La nueva ruta completa del archivo.
     * @param nuevoNombre El nuevo nombre del archivo.
     * @return true si la actualización fue exitosa, false en caso contrario.
     */
    public boolean updateImagenPath(long id, Path nuevaRuta, String nuevoNombre) {
        String sql = "UPDATE imagenes SET ruta_completa = ?, nombre_archivo = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nuevaRuta.toString());
            pstmt.setString(2, nuevoNombre);
            pstmt.setLong(3, id);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logger.debug("Ruta actualizada para imagen ID {}: {}", id, nuevaRuta);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar la ruta de la imagen ID " + id, e);
        }
        return false;
    } // ---FIN de metodo [updateImagenPath]---

    /**
     * Borra una imagen de la base de datos por su ID.
     * @param id El ID de la imagen a borrar.
     * @return true si se borró con éxito, false en caso contrario.
     */
    public boolean deleteImagen(long id) {
        String sql = "DELETE FROM imagenes WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logger.debug("Imagen con ID {} eliminada de la BD.", id);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error al eliminar la imagen con ID " + id, e);
        }
        return false;
    } // ---FIN de metodo [deleteImagen]---

    /**
     * Obtiene todas las imágenes que están dentro de una carpeta específica y sus subcarpetas.
     * @param carpetaRaiz La carpeta a escanear.
     * @return Una lista de ImagenInfo.
     */
    public List<ImagenInfo> getImagenesInFolder(Path carpetaRaiz) {
        List<ImagenInfo> imagenes = new ArrayList<>();
        // El operador LIKE con '%' al final buscará todas las rutas que COMIENCEN
        // con la ruta de la carpeta raíz.
        String sql = "SELECT * FROM imagenes WHERE ruta_completa LIKE ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, carpetaRaiz.toString() + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                imagenes.add(mapResultSetToImagenInfo(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al obtener imágenes de la carpeta " + carpetaRaiz, e);
        }
        logger.debug("Encontradas {} imágenes en la BD para la carpeta {}", imagenes.size(), carpetaRaiz);
        return imagenes;
    } // ---FIN de metodo [getImagenesInFolder]---

    /**
     * Comprueba de forma rápida si una carpeta ya ha sido indexada
     * (si existe al menos una imagen en la BD cuya ruta comience por esa carpeta).
     * @param carpetaRaiz La carpeta a comprobar.
     * @return true si hay al menos una imagen indexada, false en caso contrario.
     */
    public boolean isFolderIndexed(Path carpetaRaiz) {
        String sql = "SELECT 1 FROM imagenes WHERE ruta_completa LIKE ? LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, carpetaRaiz.toString() + "%");
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.error("Error al comprobar indexación de la carpeta " + carpetaRaiz, e);
        }
        return false;
    } // ---FIN de metodo [isFolderIndexed]---

    /**
     * Obtiene las rutas completas de todas las imágenes de la base de datos.
     * @return Una lista de strings con las rutas completas.
     */
    public List<String> getAllImagePaths() {
        List<String> paths = new ArrayList<>();
        String sql = "SELECT ruta_completa FROM imagenes ORDER BY nombre_archivo ASC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                paths.add(rs.getString("ruta_completa"));
            }
        } catch (SQLException e) {
            logger.error("Error al obtener todas las rutas de imágenes.", e);
        }
        return paths;
    }

    /**
     * Busca en la base de datos si existe una imagen con el mismo nombre y tamaño
     * pero cuya ruta física en el disco ya no exista (candidato de archivo movido).
     * @param nombreArchivo El nombre de la imagen.
     * @param tamanoBytes El tamaño del archivo en bytes.
     * @return Un Optional con el ImagenInfo del candidato, o vacío si no hay ninguno.
     */
    public Optional<ImagenInfo> findMovedCandidate(String nombreArchivo, long tamanoBytes) {
        String sql = "SELECT * FROM imagenes WHERE nombre_archivo = ? AND tamano_bytes = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nombreArchivo);
            pstmt.setLong(2, tamanoBytes);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ImagenInfo img = mapResultSetToImagenInfo(rs);
                // Si el archivo en la ubicación antigua de la BD ya no existe físicamente,
                // significa que probablemente ha cambiado de ubicación.
                try {
                    if (!java.nio.file.Files.exists(java.nio.file.Path.of(img.getRutaCompleta()))) {
                        return Optional.of(img);
                    }
                } catch (Exception ignore) {}
            }
        } catch (SQLException e) {
            logger.error("Error al buscar candidato de movimiento para: " + nombreArchivo, e);
        }
        return Optional.empty();
    }

    /**
     * Busca la ruta de una imagen registrada en la BD a partir de su nombre de archivo exacto.
     * Útil para auto-relocalización en proyectos.
     * @param nombreArchivo El nombre del archivo (ej. "imagen.jpg").
     * @return Un Optional con la ruta completa como String.
     */
    public Optional<String> findPathByFileName(String nombreArchivo) {
        String sql = "SELECT ruta_completa FROM imagenes WHERE nombre_archivo = ? LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nombreArchivo);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(rs.getString("ruta_completa"));
            }
        } catch (SQLException e) {
            logger.error("Error al buscar ruta por nombre de archivo: " + nombreArchivo, e);
        }
        return Optional.empty();
    }

    /**
     * Actualiza la información de ubicación y metadatos de una imagen que ha cambiado de posición.
     */
    public boolean updateMovedImagen(long id, String nuevaRuta, String nuevoNombre, String nuevaRutaRelativa, long nuevoDiscoId, long nuevaFechaMod) {
        String sql = "UPDATE imagenes SET ruta_completa = ?, nombre_archivo = ?, ruta_relativa = ?, disco_id = ?, fecha_modificacion = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nuevaRuta);
            pstmt.setString(2, nuevoNombre);
            pstmt.setString(3, nuevaRutaRelativa);
            pstmt.setLong(4, nuevoDiscoId);
            pstmt.setLong(5, nuevaFechaMod);
            pstmt.setLong(6, id);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logger.debug("Información de imagen reubicada actualizada para ID {}: {}", id, nuevaRuta);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar la ruta de movimiento para imagen ID " + id, e);
        }
        return false;
    }


    /**
     * Obtiene todas las imágenes de la base de datos como objetos ImagenInfo.
     * @return Una lista de ImagenInfo.
     */
    public List<ImagenInfo> getAllImagenes() {
        List<ImagenInfo> imagenes = new ArrayList<>();
        String sql = "SELECT * FROM imagenes ORDER BY nombre_archivo ASC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                imagenes.add(mapResultSetToImagenInfo(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al obtener todas las imagenes.", e);
        }
        return imagenes;
    }

    /**
     * Borra un lote de imágenes de la base de datos por sus IDs.
     * @param ids Lista de IDs de las imágenes a borrar.
     * @return Número de imágenes eliminadas.
     */
    public int deleteImagenes(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        int count = 0;
        String sql = "DELETE FROM imagenes WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            for (Long id : ids) {
                pstmt.setLong(1, id);
                pstmt.addBatch();
            }
            int[] results = pstmt.executeBatch();
            connection.commit();
            for (int r : results) {
                if (r > 0) count++;
            }
            logger.debug("{} imágenes eliminadas de la BD en lote.", count);
        } catch (SQLException e) {
            logger.error("Error al eliminar lote de imágenes", e);
            try { connection.rollback(); } catch (SQLException ex) {}
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ex) {}
        }
        return count;
    }

    /**
     * Asocia un archivo de recurso (como un .zip o .stl) a una imagen.
     */
    public boolean assignResourceToImage(long imagenId, String archivePath) {
        String sql = "INSERT OR IGNORE INTO resource_associations(imagen_id, archive_path) VALUES(?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, imagenId);
            pstmt.setString(2, archivePath);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Error al asignar recurso {} a imagen {}", archivePath, imagenId, e);
        }
        return false;
    }

    /**
     * Método de ayuda para mapear una fila de un ResultSet a un objeto ImagenInfo.
     * @param rs El ResultSet posicionado en la fila correcta.
     * @return Un objeto ImagenInfo poblado.
     * @throws SQLException Si hay un error al leer del ResultSet.
     */
    private ImagenInfo mapResultSetToImagenInfo(ResultSet rs) throws SQLException {
        ImagenInfo img = new ImagenInfo();
        img.setId(rs.getLong("id"));
        img.setRutaCompleta(rs.getString("ruta_completa"));
        img.setNombreArchivo(rs.getString("nombre_archivo"));
        img.setFechaModificacion(rs.getLong("fecha_modificacion"));
        img.setTamanoBytes(rs.getLong("tamano_bytes"));
        img.setDiscoId(rs.getLong("disco_id"));
        img.setRutaRelativa(rs.getString("ruta_relativa"));
        img.setFechaAdicion(rs.getLong("fecha_adicion"));
        return img;
    } // ---FIN de metodo [mapResultSetToImagenInfo]---

} // --- FIN de clase ImagenDAO ---