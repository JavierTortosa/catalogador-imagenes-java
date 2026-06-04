package servicios.db;

import java.sql.*;
import modelo.datos.ArchiveMetadata;

public class ArchiveMetadataDAO {
    public void guardar(ArchiveMetadata meta) throws SQLException {
        String sql = "INSERT OR REPLACE INTO archives_metadata VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, meta.archivePath);
            ps.setInt(2, meta.stlCount);
            ps.setInt(3, meta.supportedStlCount);
            ps.setInt(4, meta.unsupportedStlCount);
            ps.setInt(5, meta.isMultipart ? 1 : 0);
            ps.setInt(6, meta.hasLychee ? 1 : 0);
            ps.setInt(7, meta.hasChitubox ? 1 : 0);
            ps.setDouble(8, meta.totalSizeMb);
            ps.setLong(9, meta.analysisDate);
            ps.executeUpdate();
        }
    }

    public ArchiveMetadata buscarPorRuta(String ruta) throws SQLException {
        String sql = "SELECT * FROM archives_metadata WHERE archive_path = ?";
        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, ruta);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ArchiveMetadata m = new ArchiveMetadata();
                m.archivePath = rs.getString(1);
                m.stlCount = rs.getInt(2);
                m.supportedStlCount = rs.getInt(3);
                m.unsupportedStlCount = rs.getInt(4);
                m.isMultipart = rs.getInt(5) == 1;
                m.hasLychee = rs.getInt(6) == 1;
                m.hasChitubox = rs.getInt(7) == 1;
                m.totalSizeMb = rs.getDouble(8);
                return m;
            }
        }
        return null;
    }
}