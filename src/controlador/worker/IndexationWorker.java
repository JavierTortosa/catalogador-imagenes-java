package controlador.worker;

import java.nio.file.Path;
import java.util.List;
import javax.swing.SwingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import servicios.ConfigurationManager;
import servicios.IndexationService;
import vista.dialogos.TaskProgressDialog;

/**
 * SwingWorker para ejecutar el proceso de indexación de archivos en un hilo de fondo,
 * evitando que la UI se congele y mostrando el progreso al usuario.
 */
public class IndexationWorker extends SwingWorker<Void, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(IndexationWorker.class);

    private final List<Path> filesToIndex;
    private final Path rootPath;
    private final TaskProgressDialog progressDialog;
    private final IndexationService indexationService;

    public IndexationWorker(List<Path> filesToIndex, Path rootPath, TaskProgressDialog progressDialog) {
        this.filesToIndex = filesToIndex;
        this.rootPath = rootPath;
        this.progressDialog = progressDialog;
        this.indexationService = new IndexationService(ConfigurationManager.getInstance());
    } // ---FIN de constructor [IndexationWorker]---

    @Override
    protected Void doInBackground() throws Exception {
        logger.info("Iniciando tarea de indexación en segundo plano para {} archivos.", filesToIndex.size());
        progressDialog.setMensaje("Indexando archivos en la base de datos...");
        progressDialog.updateProgress(0, filesToIndex.size(), null);
        
        java.sql.Connection conn = null;
        boolean originalAutoCommit = true;
        
        try {
            conn = servicios.db.DatabaseManager.getInstance().getConnection();
            if (conn != null) {
                originalAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false); // Iniciar transacción
            }
            
            int processedCount = 0;
            for (Path file : filesToIndex) {
                if (isCancelled()) {
                    logger.warn("Tarea de indexación cancelada por el usuario. Realizando rollback...");
                    if (conn != null) {
                        conn.rollback();
                    }
                    break;
                }

                indexationService.indexImageAndTags(file, rootPath);
                processedCount++;
                
                // Realizar commit cada 1000 archivos para maximizar el rendimiento
                if (conn != null && processedCount % 1000 == 0) {
                    conn.commit();
                }
                
                publish(processedCount); // Envía el progreso al EDT
            }
            
            // Confirmar inserciones restantes si no fue cancelado
            if (!isCancelled() && conn != null) {
                conn.commit();
            }
            
        } catch (Exception e) {
            logger.error("Error durante el lote de indexación", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (java.sql.SQLException ex) {
                    logger.error("Error al hacer rollback de la transacción", ex);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (java.sql.SQLException ignore) {}
            }
        }
        
        return null;
    } // ---FIN de metodo [doInBackground]---

    @Override
    protected void process(List<Integer> chunks) {
        // Este método se ejecuta en el EDT
        if (!chunks.isEmpty()) {
            int latestProgress = chunks.get(chunks.size() - 1);
            progressDialog.updateProgress(latestProgress, filesToIndex.size(), null);
            progressDialog.updateStatusText(String.format("Procesado: %d / %d", latestProgress, filesToIndex.size()));
        }
    } // ---FIN de metodo [process]---

    @Override
    protected void done() {
        try {
            get(); // Llama a get() para propagar excepciones del doInBackground
            logger.info("Tarea de indexación finalizada con éxito.");
        } catch (Exception e) {
            logger.error("Error durante la ejecución de la tarea de indexación.", e);
        } finally {
            progressDialog.dispose(); // Cierra el diálogo de progreso en cualquier caso
        }
    } // ---FIN de metodo [done]---

} // --- FIN de clase IndexationWorker ---