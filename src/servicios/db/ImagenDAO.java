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