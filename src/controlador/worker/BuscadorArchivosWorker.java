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

import vista.dialogos.ProgresoCargaDialog;

public class BuscadorArchivosWorker extends SwingWorker<Map<String, Path>, Integer> {

    private final Path rutaInicio;
    private final int profundidadBusqueda;
    private final Path rutaRaizParaRelativizar;
    private final Predicate<Path> filtroImagen;
    private final ProgresoCargaDialog dialogoProgreso;

    // --- INICIO CÓDIGO MODIFICADO ---
    // Convertir contadorArchivos en un campo de instancia
    private int contadorArchivos = 0;
    // --- FIN CÓDIGO MODIFICADO ---

	public BuscadorArchivosWorker(Path rutaInicio, int profundidadBusqueda, Path rutaRaizParaRelativizar,
			Predicate<Path> filtroImagen, ProgresoCargaDialog dialogoProgreso)
	{

        this.rutaInicio = rutaInicio;
        this.profundidadBusqueda = profundidadBusqueda;
        this.rutaRaizParaRelativizar = rutaRaizParaRelativizar;
        this.filtroImagen = filtroImagen;
        this.dialogoProgreso = dialogoProgreso;
        // No inicializamos contadorArchivos aquí, ya lo hace la declaración del campo
    }

    @Override
    protected Map<String, Path> doInBackground() throws Exception {
        System.out.println("  [Worker BG] Iniciando búsqueda en " + rutaInicio + " con profundidad " + profundidadBusqueda);
        dialogoProgreso.setMensaje("Escaneando: " + rutaInicio.getFileName() + "...");

        Map<String, Path> mapaRutasResultado = new HashMap<>();
        Set<String> archivosAgregados = new HashSet<>();
        // --- INICIO CÓDIGO MODIFICADO ---
        // Ya no declaramos contadorArchivos aquí, usamos el campo de instancia
        // int contadorArchivos = 0; <--- LÍNEA ELIMINADA
        // --- FIN CÓDIGO MODIFICADO ---


        if (isCancelled()) {
            System.out.println("  [Worker BG] Tarea cancelada antes de iniciar Files.walk.");
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
                             System.err.println("  [Worker BG] WARN: No se pudo relativizar " + path + " a " + this.rutaRaizParaRelativizar + ". Usando nombre archivo.");
                             relativePathToRoot = path.getFileName();
                        }
                    } else {
                         System.err.println("  [Worker BG] WARN: rutaRaizParaRelativizar es null al generar clave. Usando nombre archivo.");
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
             System.err.println("  [Worker BG] Error durante Files.walk: " + ioOrSecEx.getMessage());
             String errorType = (ioOrSecEx instanceof IOException) ? "Error al leer directorio" : "Error de permisos";
             throw new RuntimeException(errorType, ioOrSecEx);
        } catch (CancellationException ce) {
            System.out.println("  [Worker BG] Files.walk detenido por cancelación.");
             return null;
        }

        System.out.println("  [Worker BG] Files.walk terminado. Archivos encontrados: " + this.contadorArchivos);
        publish(this.contadorArchivos); // Publicar el conteo final
        return mapaRutasResultado;
    }

    @Override
    protected void process(List<Integer> chunks) {
        if (!chunks.isEmpty()) {
            int ultimoProgreso = chunks.get(chunks.size() - 1);
            dialogoProgreso.actualizarContador(ultimoProgreso);
        }
    }

    @Override
    protected void done() {
        System.out.println("[Worker EDT] Método done() alcanzado.");
        // La lógica REAL de done() está en el PropertyChangeListener
        // que añadimos en VisorController.
    }
}