package servicios.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.datos.Disco;

/**
 * DAO para la gestión de la tabla 'discos'.
 */
public class DiscoDAO {

    private static final Logger logger = LoggerFactory.getLogger(DiscoDAO.class);
    private Connection connection;

    public DiscoDAO() {
        this.connection = DatabaseManager.getInstance().getConnection();
    } // ---FIN de constructor [DiscoDAO]---

    /**
     * Busca un disco por su número de serie de volumen.
     * @param numeroSerie El número de serie hexadecimal.
     * @return El disco si existe, o vacío si no.
     */
    public Optional<Disco> findByNumeroSerie(String numeroSerie) {
        String sql = "SELECT * FROM discos WHERE numero_serie = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, numeroSerie);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToDisco(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al buscar disco por número de serie: {}", numeroSerie, e);
        }
        return Optional.empty();
    } // ---FIN de metodo [findByNumeroSerie]---

    /**
     * Añade un nuevo disco a la base de datos.
     * @param disco La información del disco.
     * @return El ID generado o vacío si falló.
     */
    public Optional<Long> addDisco(Disco disco) {
        String sql = "INSERT INTO discos(numero_serie, nombre_etiqueta, ultima_ruta_conocida) VALUES(?,?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, disco.getNumeroSerie());
            pstmt.setString(2, disco.getNombreEtiqueta());
            pstmt.setString(3, disco.getUltimaRutaConocida());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long id = generatedKeys.getLong(1);
                        disco.setId(id);
                        return Optional.of(id);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error al añadir disco a la BD: {}", disco.getNumeroSerie(), e);
        }
        return Optional.empty();
    } // ---FIN de metodo [addDisco]---

    /**
     * Actualiza la última letra de unidad conocida para un disco.
     * @param id El ID del disco.
     * @param ruta La nueva letra de unidad o ruta raíz.
     */
    public void updateUltimaRuta(long id, String ruta) {
        String sql = "UPDATE discos SET ultima_ruta_conocida = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ruta);
            pstmt.setLong(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al actualizar última ruta para disco ID: {}", id, e);
        }
    } // ---FIN de metodo [updateUltimaRuta]---

    /**
     * Obtiene todos los discos registrados en la base de datos.
     * @return Una lista de todos los discos.
     */
    public java.util.List<Disco> getAllDiscos() {
        java.util.List<Disco> discos = new java.util.ArrayList<>();
        String sql = "SELECT * FROM discos ORDER BY nombre_etiqueta ASC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                discos.add(mapResultSetToDisco(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al obtener todos los discos de la BD.", e);
        }
        return discos;
    } // ---FIN de metodo [getAllDiscos]---

    /**
     * Obtiene un mapa con el conteo de imágenes por cada ID de disco.
     * @return Mapa de DiscoID -> CantidadImagenes.
     */
    public java.util.Map<Long, Long> getImageCountsByDisk() {
        java.util.Map<Long, Long> counts = new java.util.HashMap<>();
        String sql = "SELECT disco_id, COUNT(*) as total FROM imagenes GROUP BY disco_id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                counts.put(rs.getLong("disco_id"), rs.getLong("total"));
            }
        } catch (SQLException e) {
            logger.error("Error al obtener conteo de imágenes por disco.", e);
        }
        return counts;
    } // ---FIN de metodo [getImageCountsByDisk]---

    private Disco mapResultSetToDisco(ResultSet rs) throws SQLException {
        Disco disco = new Disco();
        disco.setId(rs.getLong("id"));
        disco.setNumeroSerie(rs.getString("numero_serie"));
        disco.setNombreEtiqueta(rs.getString("nombre_etiqueta"));
        disco.setUltimaRutaConocida(rs.getString("ultima_ruta_conocida"));
        return disco;
    } // ---FIN de metodo [mapResultSetToDisco]---

} // --- FIN de clase DiscoDAO ---
