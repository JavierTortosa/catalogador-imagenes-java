package servicios.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import servicios.ConfigKeys;
import servicios.ConfigurationManager;

/**
 * Gestor Singleton para la conexión con la base de datos SQLite.
 * Se encarga de inicializar la conexión y crear la estructura de tablas.
 * AHORA ES CONFIGURABLE.
 */
public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    // --- Instancia Singleton ---
    private static volatile DatabaseManager instance;
    
    // --- Conexión a la BD ---
    private Connection connection;
    
    private static String dbUrl; // Se inicializará una sola vez

    
    /**
     * Constructor privado. Se llama desde initialize().
     */
    private DatabaseManager() {
        try {
            // La URL ya ha sido calculada por el método initialize()
            logger.info("Conectando a la base de datos en: {}", dbUrl);
            connection = DriverManager.getConnection(dbUrl);
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA journal_mode = WAL;");
                stmt.execute("PRAGMA synchronous = NORMAL;");
                stmt.execute("PRAGMA temp_store = MEMORY;");
                stmt.execute("PRAGMA cache_size = -64000;"); // 64MB Cache de páginas
            }
            
            logger.info("Conexión a la base de datos establecida con éxito.");
        } catch (SQLException e) {
            logger.error("ERROR CRÍTICO: No se pudo conectar a la base de datos SQLite.", e);
        }
    } // ---FIN de constructor [DatabaseManager]---
    
    
    /**
     * Comprueba si la base de datos necesita actualizaciones de esquema y las aplica.
     * Por ejemplo, añadir nuevas columnas a tablas existentes.
     */
    private void upgradeSchema() {
        // --- MIGRACIÓN 1: Añadir parent_id a la tabla de tags ---
        String sqlAlterTableTags = "ALTER TABLE tags ADD COLUMN parent_id INTEGER REFERENCES tags(id);";
        
        try (Statement stmt = connection.createStatement()) {
            logger.debug("Intentando aplicar migración de esquema: añadir 'parent_id' a la tabla 'tags'...");
            stmt.execute(sqlAlterTableTags);
            logger.info("Migración de esquema aplicada con éxito: columna 'parent_id' añadida a 'tags'.");
        } catch (SQLException e) {
            // Un error "duplicate column name" es esperado si la migración ya se aplicó.
            if (e.getMessage().contains("duplicate column name")) {
                logger.debug("La columna 'parent_id' ya existe en 'tags'. No se necesita migración.");
            } else {
                logger.error("Error al intentar actualizar el esquema de la tabla 'tags'.", e);
            }
        }

        // --- MIGRACIÓN 2: Añadir índices para optimización (FASE 2) ---
        String[] indexQueries = {
            "CREATE INDEX IF NOT EXISTS idx_imagenes_nombre_archivo ON imagenes(nombre_archivo);",
            "CREATE INDEX IF NOT EXISTS idx_imagenes_fecha ON imagenes(fecha_modificacion);",
            "CREATE INDEX IF NOT EXISTS idx_imagenes_tamano ON imagenes(tamano_bytes);",
            "CREATE INDEX IF NOT EXISTS idx_tags_parent_id ON tags(parent_id);",
            "CREATE INDEX IF NOT EXISTS idx_imagen_tags_tag_id ON imagen_tags(tag_id);"
        };

        try (Statement stmt = connection.createStatement()) {
            logger.debug("Verificando/Creando índices de optimización...");
            for (String sql : indexQueries) {
                stmt.execute(sql);
            }
            logger.info("Índices de optimización de la Fase 2 verificados/creados con éxito.");
        } catch (SQLException e) {
            logger.error("Error al crear los índices de optimización.", e);
        }

        // --- MIGRACIÓN 3: Refactorización de 'imagenes' para Robustez Técnica (FASE 3) ---
        // Añadimos disco_id, ruta_relativa y fecha_adicion si no existen.
        try (Statement stmt = connection.createStatement()) {
            logger.debug("Verificando si se necesitan columnas de robustez técnica en 'imagenes'...");
            
            // Usamos un bloque try-catch por columna para simplificar si ya existen parcialmente
            try { stmt.execute("ALTER TABLE imagenes ADD COLUMN disco_id INTEGER REFERENCES discos(id);"); } catch (SQLException ignore) {}
            try { stmt.execute("ALTER TABLE imagenes ADD COLUMN ruta_relativa TEXT;"); } catch (SQLException ignore) {}
            try { stmt.execute("ALTER TABLE imagenes ADD COLUMN fecha_adicion INTEGER;"); } catch (SQLException ignore) {}
            
            logger.info("Migración de robustez técnica verificada/aplicada en 'imagenes'.");
        } catch (SQLException e) {
            logger.error("Error al aplicar migración de robustez técnica.", e);
        }

        // --- MIGRACIÓN 4: Añadir read_only a la tabla de tags ---
        try (Statement stmt = connection.createStatement()) {
            logger.debug("Verificando si se necesita la columna 'read_only' en 'tags'...");
            try { 
                stmt.execute("ALTER TABLE tags ADD COLUMN read_only INTEGER DEFAULT 0;"); 
                logger.info("Migración aplicada: columna 'read_only' añadida a 'tags'.");
            } catch (SQLException e) {
                if (e.getMessage().contains("duplicate column name")) {
                    logger.debug("La columna 'read_only' ya existe en 'tags'.");
                } else {
                    throw e;
                }
            }
        } catch (SQLException e) {
            logger.error("Error al aplicar migración de 'read_only' en 'tags'.", e);
        }

        // --- MIGRACIÓN 5: Refactorización de UNIQUE constraint en 'tags' ---
        try (Statement stmt = connection.createStatement()) {
            // Check if the old unique constraint on 'nombre' exists by trying to add a duplicate name with different parent
            // A simpler way: just recreate the table if it doesn't have UNIQUE(parent_id, nombre). 
            // We can determine this by checking the sql used to create the table from sqlite_master.
            ResultSet rs = stmt.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='tags'");
            if (rs.next()) {
                String sql = rs.getString("sql");
                if (sql != null && sql.contains("nombre TEXT NOT NULL UNIQUE")) {
                    logger.info("Iniciando migración 5: Refactorizando tabla 'tags' para UNIQUE(parent_id, nombre)...");
                    stmt.execute("PRAGMA foreign_keys = OFF;");
                    stmt.execute("BEGIN TRANSACTION;");
                    
                    stmt.execute("CREATE TABLE tags_new (" +
                                 "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                 "  nombre TEXT NOT NULL," +
                                 "  parent_id INTEGER REFERENCES tags_new(id)," +
                                 "  read_only INTEGER DEFAULT 0," +
                                 "  UNIQUE(parent_id, nombre)" +
                                 ");");
                    
                    stmt.execute("INSERT INTO tags_new (id, nombre, parent_id, read_only) SELECT id, nombre, parent_id, read_only FROM tags;");
                    stmt.execute("DROP TABLE tags;");
                    stmt.execute("ALTER TABLE tags_new RENAME TO tags;");
                    
                    stmt.execute("COMMIT;");
                    stmt.execute("PRAGMA foreign_keys = ON;");
                    
                    // Recreate indices since table was dropped
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_tags_parent_id ON tags(parent_id);");
                    
                    logger.info("Migración 5 aplicada con éxito.");
                }
            }
        } catch (SQLException e) {
            logger.error("Error al aplicar migración 5 (UNIQUE constraint en 'tags').", e);
            try (Statement rollbackStmt = connection.createStatement()) {
                rollbackStmt.execute("ROLLBACK;");
                rollbackStmt.execute("PRAGMA foreign_keys = ON;");
            } catch (SQLException ex) {
                logger.error("Error al hacer rollback de migración 5.", ex);
            }
        }
    } // ---FIN de metodo [upgradeSchema]---
    

    /**
     * MÉTODO DE INICIALIZACIÓN CLAVE. Debe ser llamado ANTES de getInstance().
     * Determina la ruta de la BD a partir de la configuración.
     * @param config El gestor de configuración de la aplicación.
     */
    public static void initialize(ConfigurationManager config) {
        if (dbUrl != null) {
            logger.warn("El DatabaseManager ya ha sido inicializado. Se ignora la llamada.");
            return;
        }

        String customDbPath = config.getString(ConfigKeys.DATABASE_PATH, "");
        
        if (customDbPath != null && !customDbPath.isBlank()) {
            // El usuario ha especificado una ruta. La usamos.
            // Esto permite el MODO PORTABLE.
            logger.info("Se ha encontrado una ruta personalizada para la base de datos: {}", customDbPath);
            File dbFile = new File(customDbPath);
            
            // Asegurarnos de que el directorio padre exista
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                logger.info("Creando directorio para la base de datos personalizada: {}", parentDir.getAbsolutePath());
                parentDir.mkdirs();
            }
            dbUrl = "jdbc:sqlite:" + customDbPath;

        } else {
            // Comportamiento por defecto: usar la carpeta del usuario.
            logger.info("No se encontró ruta personalizada. Usando ubicación por defecto en el directorio del usuario.");
            String dbFolderPath = System.getProperty("user.home") + File.separator + ".VisorImagenes";
            String dbFileName = "visor_collection.db";
            
            File dbFolder = new File(dbFolderPath);
            if (!dbFolder.exists()) {
                logger.info("Creando directorio por defecto para la base de datos: {}", dbFolderPath);
                dbFolder.mkdirs();
            }
            dbUrl = "jdbc:sqlite:" + dbFolderPath + File.separator + dbFileName;
        }
    } // ---FIN de metodo [initialize]---

    /**
     * Devuelve la instancia única del DatabaseManager (thread-safe).
     * IMPORTANTE: El método 'initialize(config)' DEBE ser llamado antes que este.
     * @return La instancia Singleton de DatabaseManager.
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    if (dbUrl == null) {
                        throw new IllegalStateException("DatabaseManager no ha sido inicializado. Llama a DatabaseManager.initialize(config) primero.");
                    }
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    } // ---FIN de metodo [getInstance]---


    public Connection getConnection() {
        return connection;
        
    } // ---FIN de metodo [getConnection]---
    
    
    public void initializeDatabase() {
        if (connection == null) {
            logger.error("No se puede inicializar la base de datos, la conexión es nula.");
            return;
        }

        String sqlCreateTableDiscos = "CREATE TABLE IF NOT EXISTS discos (" +
                                      "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                      "  numero_serie TEXT NOT NULL UNIQUE," +
                                      "  nombre_etiqueta TEXT," +
                                      "  ultima_ruta_conocida TEXT" +
                                      ");";

        String sqlCreateTableImagenes = "CREATE TABLE IF NOT EXISTS imagenes (" +
                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "  ruta_completa TEXT NOT NULL UNIQUE," +
                                        "  nombre_archivo TEXT NOT NULL," +
                                        "  fecha_modificacion INTEGER NOT NULL," +
                                        "  tamano_bytes INTEGER," +
                                        "  disco_id INTEGER REFERENCES discos(id)," +
                                        "  ruta_relativa TEXT," +
                                        "  fecha_adicion INTEGER" +
                                        ");";

        String sqlCreateTableTags = "CREATE TABLE IF NOT EXISTS tags (" +
                                    "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "  nombre TEXT NOT NULL," +
                                    "  parent_id INTEGER REFERENCES tags(id)," + 
                                    "  read_only INTEGER DEFAULT 0," +
                                    "  UNIQUE(parent_id, nombre)" +
                                    ");";

        String sqlCreateTableImagenTags = "CREATE TABLE IF NOT EXISTS imagen_tags (" +
                                          "  imagen_id INTEGER NOT NULL," +
                                          "  tag_id INTEGER NOT NULL," +
                                          "  FOREIGN KEY(imagen_id) REFERENCES imagenes(id) ON DELETE CASCADE," +
                                          "  FOREIGN KEY(tag_id) REFERENCES tags(id) ON DELETE CASCADE," +
                                          "  PRIMARY KEY (imagen_id, tag_id)" +
                                          ");";
        
        try (Statement stmt = connection.createStatement()) {
            logger.debug("Ejecutando sentencias de creación de tablas...");
            stmt.execute(sqlCreateTableDiscos); // Añadimos discos primero por las FK
            stmt.execute(sqlCreateTableImagenes);
            stmt.execute(sqlCreateTableTags);
            stmt.execute(sqlCreateTableImagenTags);
            logger.info("Estructura de la base de datos verificada/creada con éxito.");
        } catch (SQLException e) {
            logger.error("Error al crear las tablas de la base de datos.", e);
        }
        
        String sqlCreateTableArchivesMetadata = "CREATE TABLE IF NOT EXISTS archives_metadata (" +
                "  archive_path TEXT PRIMARY KEY," + // Ruta completa del rar/zip
                "  stl_count INTEGER," +
                "  supported_stl_count INTEGER," +
                "  unsupported_stl_count INTEGER," +
                "  is_multipart INTEGER," + // 0 o 1
                "  has_lychee INTEGER," +    // 0 o 1
                "  has_chitubox INTEGER," +  // 0 o 1
                "  total_size_mb REAL," +
                "  analysis_date INTEGER" +
                ");";

        try (Statement stmt = connection.createStatement()) {
        	stmt.execute(sqlCreateTableArchivesMetadata);
        	logger.info("Tabla archives_metadata verificada/creada.");
        } catch (SQLException e) {
        	logger.error("Error al crear tabla archives_metadata", e);
        }
        
        // Llamamos al método de actualización para asegurar la compatibilidad hacia atrás
        upgradeSchema();
        
    } // ---FIN de metodo [initializeDatabase]---
    
    
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Conexión a la base de datos cerrada.");
            }
        } catch (SQLException e) {
            logger.error("Error al cerrar la conexión de la base de datos.", e);
        }
        
    } // ---FIN de metodo [closeConnection]---

    
} // --- FIN de clase DatabaseManager ---