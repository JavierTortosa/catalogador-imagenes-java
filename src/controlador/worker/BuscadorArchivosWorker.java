package controlador.worker; 

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vista.dialogos.TaskProgressDialog;

public class BuscadorArchivosWorker extends SwingWorker<Map<String, Path>, Integer> {

	private static final Logger logger = LoggerFactory.getLogger(BuscadorArchivosWorker.class);
	
    private final Path rutaInicio;
    private final int profundidadBusqueda;
    private final Path rutaRaizParaRelativizar;
    private final Predicate<Path> filtroImagen;
    private final TaskProgressDialog dialogoProgreso;
    

    // Convertir contadorArchivos en un campo de instancia
    private int contadorArchivos = 0;

	public BuscadorArchivosWorker(Path rutaInicio, int profundidadBusqueda, Path rutaRaizParaRelativizar,
			Predicate<Path> filtroImagen, TaskProgressDialog dialogoProgreso)
	{

        this.rutaInicio = rutaInicio;
        this.profundidadBusqueda = profundidadBusqueda;
        this.rutaRaizParaRelativizar = rutaRaizParaRelativizar;
        this.filtroImagen = filtroImagen;
        this.dialogoProgreso = dialogoProgreso;
        // No inicializamos contadorArchivos aquí, ya lo hace la declaración del campo
    } // -- FIN del constructor -- 

    @Override
    protected Map<String, Path> doInBackground() throws Exception {
        logger.debug("  [Worker BG] Iniciando búsqueda en " + rutaInicio + " con profundidad " + profundidadBusqueda);
        dialogoProgreso.setMensaje("Escaneando: " + rutaInicio.getFileName() + "...");

        Map<String, Path> mapaRutasResultado = new HashMap<>();
        Set<String> archivosAgregados = new HashSet<>();
        // --- INICIO CÓDIGO MODIFICADO ---
        // Ya no declaramos contadorArchivos aquí, usamos el campo de instancia
        // int contadorArchivos = 0; <--- LÍNEA ELIMINADA
        // --- FIN CÓDIGO MODIFICADO ---


        if (isCancelled()) {
            logger.debug("  [Worker BG] Tarea cancelada antes de iniciar Files.walk.");
            return null;
        }

        try (Stream<Path> stream = Files.walk(rutaInicio, profundidadBusqueda)) {
            stream
                .filter(Files::isRegularFile)
                .filter(filtroImagen)
                .forEach(path -> {
                    if (isCancelled()) {
                        throw new CancellationException("Tarea cancelada por el usuario durante el escaneo.");
                    }

                    Path relativePathToRoot;
                    if (this.rutaRaizParaRelativizar != null) {
                        try {
                            relativePathToRoot = this.rutaRaizParaRelativizar.relativize(path);
                        } catch (IllegalArgumentException e) {
                             logger.warn("  [Worker BG] WARN: No se pudo relativizar " + path + " a " + this.rutaRaizParaRelativizar + ". Usando nombre archivo.");
                             relativePathToRoot = path.getFileName();
                        }
                    } else {
                         logger.warn("  [Worker BG] WARN: rutaRaizParaRelativizar es null al generar clave. Usando nombre archivo.");
                         relativePathToRoot = path.getFileName();
                    }
                    String uniqueKey = relativePathToRoot.toString().replace("\\", "/");

                    if (archivosAgregados.add(uniqueKey)) {
                        mapaRutasResultado.put(uniqueKey, path);

                        // --- INICIO CÓDIGO MODIFICADO ---
                        // Ahora usamos el campo de instancia, esto es válido dentro de la lambda
                        this.contadorArchivos++; // O simplemente contadorArchivos++
                        publish(this.contadorArchivos); // O simplemente publish(contadorArchivos)
                        // --- FIN CÓDIGO MODIFICADO ---
                    }
                }); // Fin forEach

        } catch (IOException | SecurityException ioOrSecEx) {
             logger.error("  [Worker BG] Error durante Files.walk: " + ioOrSecEx.getMessage());
             String errorType = (ioOrSecEx instanceof IOException) ? "Error al leer directorio" : "Error de permisos";
             throw new RuntimeException(errorType, ioOrSecEx);
        } catch (CancellationException ce) {
            logger.debug("  [Worker BG] Files.walk detenido por cancelación.");
            
             return null;
        }

        logger.debug("  [Worker BG] Files.walk terminado. Archivos encontrados: " + this.contadorArchivos);
        publish(this.contadorArchivos); // Publicar el conteo final
        return mapaRutasResultado;
    } // -- FIN del metodo doInBackground --

    @Override
    protected void process(List<Integer> chunks) {
        if (!chunks.isEmpty()) {
            int ultimoProgreso = chunks.get(chunks.size() - 1);
            // Usamos el nuevo método para actualizar la etiqueta de estado
            dialogoProgreso.updateStatusText("Archivos encontrados: " + ultimoProgreso);
        }
    } // -- FIN del metodo process --
    

    @Override
    protected void done() {
        logger.debug("[Worker EDT] Método done() alcanzado.");
        // La lógica REAL de done() está en el PropertyChangeListener
        // que añadimos en VisorController.
        
    } // -- FIN del metodo done -- 
    
} // --- FIN de la clase BuscadorArchivosWorker ---