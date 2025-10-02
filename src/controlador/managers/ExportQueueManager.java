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
     * Limpia la cola anterior y la repuebla con una lógica de prioridad:
     * 1. Preserva el estado en memoria de los items que ya estaban en la cola.
     * 2. Para items nuevos, carga las asociaciones guardadas en el archivo de proyecto.
     * 3. Si no hay asociaciones guardadas, escanea el disco en busca de archivos.
     *
     * @param rutasSeleccionadas La lista de rutas de las imágenes seleccionadas.
     * @param associatedFilesFromProject El mapa de archivos asociados cargado del ProjectModel.
     */
    public void prepararColaDesdeSeleccion(List<Path> rutasSeleccionadas, Map<String, modelo.proyecto.ExportConfig> exportConfigsFromProject) {
        logger.debug("[ExportQueueManager] Reconciliando cola para " + rutasSeleccionadas.size() + " imágenes.");

        Map<Path, ExportItem> estadoAntiguo = this.colaDeExportacion.stream()
            .collect(Collectors.toMap(ExportItem::getRutaImagen, item -> item, (item1, item2) -> item1));

        List<ExportItem> nuevaCola = new ArrayList<>();

        for (Path rutaImagen : rutasSeleccionadas) {
            if (estadoAntiguo.containsKey(rutaImagen)) {
                // Si el item ya estaba en memoria (ej. el usuario ya interactuó con él), lo preservamos.
                nuevaCola.add(estadoAntiguo.get(rutaImagen));
            } else {
                // Si es un item nuevo (ej. al cargar el proyecto), lo construimos desde cero.
                ExportItem itemNuevo = new ExportItem(rutaImagen);
                String claveImagen = rutaImagen.toString().replace("\\", "/");

                // --- INICIO DE LA LÓGICA DE CARGA MEJORADA ---
                modelo.proyecto.ExportConfig config = (exportConfigsFromProject != null) ? exportConfigsFromProject.get(claveImagen) : null;

                if (config != null) {
                    // --- Prioridad 1: Hay configuración guardada para esta imagen ---
                    itemNuevo.setSeleccionadoParaExportar(config.isExportEnabled());
                    
                    List<String> rutasGuardadasStr = config.getAssociatedFiles();
                    if (rutasGuardadasStr != null && !rutasGuardadasStr.isEmpty()) {
                        List<Path> pathsGuardados = rutasGuardadasStr.stream()
                                                                     .map(java.nio.file.Paths::get)
                                                                     .collect(Collectors.toList());
                        itemNuevo.setRutasArchivosAsociados(pathsGuardados);
                        itemNuevo.setEstadoArchivoComprimido(ExportStatus.ASIGNADO_MANUAL);
                    } else if (config.isIgnoreCompressed()) {
                        itemNuevo.setEstadoArchivoComprimido(ExportStatus.IGNORAR_COMPRIMIDO);
                    } else {
                        // Hay config, pero sin archivos y sin ignorar. Se busca en disco.
                        buscarArchivoComprimidoAsociado(itemNuevo);
                    }
                     logger.debug(" -> Configuración de exportación cargada desde el proyecto para {}", rutaImagen.getFileName());

                } else {
                    // --- Prioridad 2: No hay configuración guardada, escanear el disco ---
                    if (Files.exists(rutaImagen) && Files.isRegularFile(rutaImagen)) {
                        buscarArchivoComprimidoAsociado(itemNuevo);
                    } else {
                        logger.warn("WARN [ExportQueueManager]: La imagen original no se encontró en la ruta: " + rutaImagen);
                        itemNuevo.setEstadoArchivoComprimido(ExportStatus.IMAGEN_NO_ENCONTRADA);
                    }
                }
                // --- FIN DE LA LÓGICA DE CARGA MEJORADA ---
                
                nuevaCola.add(itemNuevo);
            }
        }

        this.colaDeExportacion = nuevaCola;
        logger.debug("[ExportQueueManager] Reconciliación de cola finalizada. Nuevo tamaño: {}", this.colaDeExportacion.size());
        
        detectarColisionesDeNombres();
        
        
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
    
    
    /**
     * Detecta colisiones de nombres de archivo dentro de la cola de exportación.
     * Itera sobre todos los archivos que se van a exportar (imágenes y asociados)
     * y marca los ExportItems correspondientes si encuentra nombres duplicados.
     */
    private void detectarColisionesDeNombres() {
        logger.debug("[ExportQueueManager] Iniciando detección de colisiones de nombres...");
        // Reseteamos el estado de conflicto de todos los items primero.
        for (ExportItem item : colaDeExportacion) {
            item.setTieneConflictoDeNombre(false);
        }

        // Mapa para rastrear nombres: Clave = nombre de archivo (lowercase), Valor = lista de items que lo contienen.
        java.util.Map<String, java.util.List<ExportItem>> nameTracker = new java.util.HashMap<>();

        // 1. Poblar el mapa con todos los archivos que se exportarán.
        for (ExportItem item : colaDeExportacion) {
            // Añadir la imagen principal
            String imageName = item.getRutaImagen().getFileName().toString().toLowerCase();
            nameTracker.computeIfAbsent(imageName, k -> new java.util.ArrayList<>()).add(item);

            // Añadir todos los archivos asociados
            if (item.getRutasArchivosAsociados() != null) {
                for (java.nio.file.Path asociado : item.getRutasArchivosAsociados()) {
                    String asociadoName = asociado.getFileName().toString().toLowerCase();
                    nameTracker.computeIfAbsent(asociadoName, k -> new java.util.ArrayList<>()).add(item);
                }
            }
        }

        // 2. Identificar los conflictos y marcar los items.
        int conflictosEncontrados = 0;
        for (java.util.Map.Entry<String, java.util.List<ExportItem>> entry : nameTracker.entrySet()) {
            if (entry.getValue().size() > 1) {
                // ¡Conflicto detectado para el nombre de archivo!
                String nombreEnConflicto = entry.getKey();
                logger.warn("  -> Conflicto detectado para el nombre: '{}'", nombreEnConflicto);
                conflictosEncontrados++;
                
                // Marcamos TODOS los items involucrados en este conflicto.
                for (ExportItem itemEnConflicto : entry.getValue()) {
                    itemEnConflicto.setTieneConflictoDeNombre(true);
                }
            }
        }
        
        if (conflictosEncontrados > 0) {
            logger.info("[ExportQueueManager] Detección finalizada. Se encontraron {} conflictos de nombre.", conflictosEncontrados);
        } else {
            logger.debug("[ExportQueueManager] Detección finalizada. No se encontraron conflictos de nombre.");
        }
    } // ---FIN de metodo [detectarColisionesDeNombres]---

} // --- FIN de la clase ExportQueueManager ---