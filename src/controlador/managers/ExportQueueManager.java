package controlador.managers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        logger.debug("[ExportQueueManager] Reconciliando cola para " + rutasSeleccionadas.size() + " imágenes.");

        // Paso 1: Guardar el estado antiguo en un mapa para acceso rápido.
        // La clave es la ruta de la imagen, el valor es el ExportItem completo con su estado.
        Map<Path, ExportItem> estadoAntiguo = this.colaDeExportacion.stream()
            .collect(Collectors.toMap(ExportItem::getRutaImagen, item -> item, (item1, item2) -> item1));

        // Paso 2: Crear la nueva cola que vamos a construir.
        List<ExportItem> nuevaCola = new ArrayList<>();

        // Paso 3: Iterar sobre la NUEVA lista de imágenes seleccionadas.
        for (Path rutaImagen : rutasSeleccionadas) {
            // Comprobar si esta imagen ya existía en la cola anterior.
            if (estadoAntiguo.containsKey(rutaImagen)) {
                // ¡Sí existía! La recuperamos del mapa y la añadimos a la nueva cola.
                // Esto preserva cualquier asignación manual o estado que tuviera.
                nuevaCola.add(estadoAntiguo.get(rutaImagen));
            } else {
                // No existía. Es una imagen recién marcada.
                // Creamos un nuevo ExportItem, buscamos sus archivos y lo añadimos.
                ExportItem itemNuevo = new ExportItem(rutaImagen);
                if (Files.exists(rutaImagen) && Files.isRegularFile(rutaImagen)) {
                    buscarArchivoComprimidoAsociado(itemNuevo);
                } else {
                    logger.warn("WARN [ExportQueueManager]: La imagen original no se encontró en la ruta: " + rutaImagen);
                    itemNuevo.setEstadoArchivoComprimido(ExportStatus.IMAGEN_NO_ENCONTRADA);
                }
                nuevaCola.add(itemNuevo);
            }
        }

        // Paso 4: Reemplazar la cola antigua por la nueva, ya reconciliada.
        this.colaDeExportacion = nuevaCola;
        logger.debug("[ExportQueueManager] Reconciliación de cola finalizada. Nuevo tamaño: {}", this.colaDeExportacion.size());
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
    
    
    private boolean esExtensionComprimida(String nombreArchivo) {
        String nombreEnMinusculas = nombreArchivo.toLowerCase();
        return nombreEnMinusculas.endsWith(".zip") || 
               nombreEnMinusculas.endsWith(".rar") || 
               nombreEnMinusculas.endsWith(".7z");
    } // --- Fin del método esExtensionComprimida ---

} // --- FIN de la clase ExportQueueManager ---