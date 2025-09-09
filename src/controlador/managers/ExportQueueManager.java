package controlador.managers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.proyecto.ExportItem;
import modelo.proyecto.ExportStatus;

public class ExportQueueManager {

	private static final Logger logger = LoggerFactory.getLogger(ExportQueueManager.class);
	
    private List<ExportItem> colaDeExportacion;

    public ExportQueueManager() {
        this.colaDeExportacion = new ArrayList<>();
    } // --- Fin del método ExportQueueManager (constructor) ---

    public List<ExportItem> getColaDeExportacion() {
        return this.colaDeExportacion;
    } // --- Fin del método getColaDeExportacion ---

    public void limpiarCola() {
        this.colaDeExportacion.clear();
    } // --- Fin del método limpiarCola ---
    
    /**
     * Prepara la cola de exportación basándose en la selección actual del proyecto.
     * Limpia la cola anterior y la repuebla.
     * MODIFICADO: Ahora comprueba la existencia de la imagen antes de procesarla.
     * @param rutasSeleccionadas La lista de rutas absolutas de las imágenes seleccionadas.
     */
    public void prepararColaDesdeSeleccion(List<Path> rutasSeleccionadas) {
        logger.debug("[ExportQueueManager] Preparando cola para " + rutasSeleccionadas.size() + " imágenes.");
        limpiarCola();

        for (Path rutaImagen : rutasSeleccionadas) {
            ExportItem item = new ExportItem(rutaImagen);

            // --- LÓGICA DE VERIFICACIÓN DE EXISTENCIA ---
            if (Files.exists(rutaImagen) && Files.isRegularFile(rutaImagen)) {
                // Si la imagen existe, procedemos a buscar su archivo comprimido asociado.
                buscarArchivoComprimidoAsociado(item);
            } else {
                // Si la imagen NO existe, marcamos el item con el nuevo estado y no buscamos nada más.
                logger.warn("WARN [ExportQueueManager]: La imagen original no se encontró en la ruta: " + rutaImagen);
                item.setEstadoArchivoComprimido(ExportStatus.IMAGEN_NO_ENCONTRADA);
            }
            // --- FIN DE LA LÓGICA DE VERIFICACIÓN ---

            this.colaDeExportacion.add(item);
        }
        logger.debug("[ExportQueueManager] Preparación de cola finalizada.");
    } // --- Fin del método prepararColaDesdeSeleccion ---

    
    /**
     * Lógica mejorada para buscar archivos asociados a una imagen, ahora con soporte para multipart.
     * Busca archivos que compartan el mismo nombre base y tengan una extensión comprimida.
     * @param item El ExportItem que se está procesando.
     */
    public void buscarArchivoComprimidoAsociado(ExportItem item) {
        Path rutaImagen = item.getRutaImagen();
        Path directorio = rutaImagen.getParent();
        if (directorio == null || !Files.isDirectory(directorio)) {
            item.setEstadoArchivoComprimido(ExportStatus.NO_ENCONTRADO);
            return;
        }

        // Usamos el nuevo método para obtener un nombre base más limpio.
        String nombreBaseImagen = obtenerNombreBase(rutaImagen.getFileName().toString());

        try (Stream<Path> stream = Files.list(directorio)) {
            List<Path> candidatos = stream
                .filter(path -> Files.isRegularFile(path) && !path.equals(rutaImagen))
                .filter(path -> {
                    String nombreCandidato = path.getFileName().toString();
                    // La condición clave: el nombre del candidato debe EMPEZAR con el nombre base de la imagen
                    // y tener una extensión comprimida. Esto captura "nombre.zip", "nombre.part1.rar", etc.
                    return nombreCandidato.toLowerCase().startsWith(nombreBaseImagen.toLowerCase()) && 
                           esExtensionComprimida(nombreCandidato);
                })
                .collect(Collectors.toList());

            // Ordenamos la lista para que los "part1, part2" queden en orden.
            candidatos.sort(Path::compareTo);

            if (!candidatos.isEmpty()) {
                // Si encontramos uno o más, los añadimos todos.
                item.setRutasArchivosAsociados(candidatos);
                item.setEstadoArchivoComprimido(ExportStatus.ENCONTRADO_OK);
                logger.debug(" -> Encontrados {} archivos asociados para {}", candidatos.size(), nombreBaseImagen);
            } else {
                // Si la lista está vacía, no se encontró nada.
                item.setEstadoArchivoComprimido(ExportStatus.NO_ENCONTRADO);
            }

        } catch (IOException e) {
            logger.error("Error buscando archivo asociado para " + rutaImagen + ": " + e.getMessage());
            item.setEstadoArchivoComprimido(ExportStatus.NO_ENCONTRADO);
        }
    } // ---FIN de metodo [buscarArchivoComprimidoAsociado]---
    
    /**
     * Obtiene el nombre base de un archivo, manejando posibles extensiones dobles
     * como ".part1.rar". Devuelve el nombre hasta el primer punto.
     * @param nombreArchivo El nombre completo del archivo.
     * @return El nombre base.
     */
    private String obtenerNombreBase(String nombreArchivo) {
        // Buscamos el primer punto en el nombre del archivo.
        int puntoIndex = nombreArchivo.indexOf('.');
        // Si no hay punto, devolvemos el nombre completo.
        // Si hay un punto, devolvemos todo lo que está antes.
        return (puntoIndex == -1) ? nombreArchivo : nombreArchivo.substring(0, puntoIndex);
    } // ---FIN de metodo [obtenerNombreBase]---
    
    
//    /**
//     * Lógica simple para buscar un archivo comprimido asociado a una imagen.
//     * @param item El ExportItem que se está procesando.
//     */
//    public void buscarArchivoComprimidoAsociado(ExportItem item) {
//        Path rutaImagen = item.getRutaImagen();
//        Path directorio = rutaImagen.getParent();
//        if (directorio == null || !Files.isDirectory(directorio)) {
//            item.setEstadoArchivoComprimido(ExportStatus.NO_ENCONTRADO);
//            return;
//        }
//
//        String nombreBase = obtenerNombreBase(rutaImagen.getFileName().toString());
//
//        try (Stream<Path> stream = Files.list(directorio)) {
//            List<Path> candidatos = stream
//                .filter(p -> Files.isRegularFile(p) && !p.equals(rutaImagen))
//                .filter(p -> {
//                    String nombreArchivoCandidato = p.getFileName().toString();
//                    String nombreBaseArchivoCandidato = obtenerNombreBase(nombreArchivoCandidato);
//                    // Comprobamos si el nombre base del candidato es igual al de la imagen
//                    // y si la extensión es de un tipo comprimido conocido.
//                    return nombreBase.equalsIgnoreCase(nombreBaseArchivoCandidato) && esExtensionComprimida(nombreArchivoCandidato);
//                })
//                .collect(Collectors.toList());
//
//            if (candidatos.size() == 1) {
//                item.setRutaArchivoComprimido(candidatos.get(0));
//                item.setEstadoArchivoComprimido(ExportStatus.ENCONTRADO_OK);
//            } else if (candidatos.size() > 1) {
//                item.setCandidatosArchivo(candidatos);
//                item.setEstadoArchivoComprimido(ExportStatus.MULTIPLES_CANDIDATOS);
//            } else {
//                item.setEstadoArchivoComprimido(ExportStatus.NO_ENCONTRADO);
//            }
//        } catch (IOException e) {
//            logger.error("Error buscando archivo asociado para " + rutaImagen + ": " + e.getMessage());
//            item.setEstadoArchivoComprimido(ExportStatus.NO_ENCONTRADO);
//        }
//    } // --- Fin del método buscarArchivoComprimidoAsociado ---
    
    
//    private String obtenerNombreBase(String nombreArchivo) {
//        int puntoIndex = nombreArchivo.lastIndexOf('.');
//        return (puntoIndex == -1) ? nombreArchivo : nombreArchivo.substring(0, puntoIndex);
//    } // --- Fin del método obtenerNombreBase ---
    
    private boolean esExtensionComprimida(String nombreArchivo) {
        String nombreEnMinusculas = nombreArchivo.toLowerCase();
        return nombreEnMinusculas.endsWith(".zip") || 
               nombreEnMinusculas.endsWith(".rar") || 
               nombreEnMinusculas.endsWith(".7z");
    } // --- Fin del método esExtensionComprimida ---

} // --- FIN de la clase ExportQueueManager ---