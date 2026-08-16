package servicios;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import modelo.proyecto.ExportConfig;

/**
 * Servicio encargado de validar la integridad de las imágenes
 * y sus archivos asociados para asegurar que son exportables o
 * compartibles con el cliente.
 */
public class ValidationService {

    /**
     * Valida si una imagen y su configuración son aptas para exportar o compartir con el cliente.
     * Esta validación asume que los archivos existen en disco y que si se ignoran los comprimidos
     * o se asignan manualmente, es válido.
     * 
     * @param imagePath La ruta de la imagen original
     * @param config La configuración de exportación de la imagen
     * @return true si es válido, false en caso contrario
     */
    public boolean isValidForExport(Path imagePath, ExportConfig config) {
        if (imagePath == null || !Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
            return false;
        }

        if (config == null) {
            // Si no hay configuración, la única forma de que sea válido es si existe
            // el archivo comprimido asociado automáticamente en disco.
            return hasAssociatedCompressedFile(imagePath);
        }

        if (!config.isExportEnabled()) {
            return false;
        }

        if (config.isIgnoreCompressed()) {
            return true; // Es válido porque el usuario decidió ignorar el comprimido
        }

        List<String> associatedFiles = config.getAssociatedFiles();
        if (associatedFiles != null && !associatedFiles.isEmpty()) {
            // Validar que los archivos asignados realmente existan
            for (String associatedPathStr : associatedFiles) {
                Path associatedPath = java.nio.file.Paths.get(associatedPathStr);
                if (!Files.exists(associatedPath) || !Files.isRegularFile(associatedPath)) {
                    return false;
                }
            }
            return true;
        }

        // Si no se asignó manualmente y no se ignoró, comprobamos si en el disco hay uno que coincida
        return hasAssociatedCompressedFile(imagePath);
    } // --- FIN del metodo isValidForExport ---


    /**
     * Comprueba si existe un archivo comprimido asociado (mismo nombre base) en el directorio de la imagen.
     */
    private boolean hasAssociatedCompressedFile(Path imagePath) {
        Path directorio = imagePath.getParent();
        if (directorio == null || !Files.isDirectory(directorio)) {
            return false;
        }

        Path fnImg = imagePath.getFileName();
        String sImg = (fnImg != null) ? fnImg.toString() : imagePath.toString();
        String nombreBaseImagen = obtenerNombreBase(sImg);

        try (Stream<Path> stream = Files.list(directorio)) {
            long matches = stream
                .filter(path -> Files.isRegularFile(path) && !path.equals(imagePath))
                .filter(path -> {
                    Path fn = path.getFileName();
                    String nombreCandidato = (fn != null) ? fn.toString() : path.toString();
                    return nombreCandidato.toLowerCase().startsWith(nombreBaseImagen.toLowerCase()) && 
                           esExtensionAsociada(nombreCandidato);
                })
                .count();
                
            // Consideramos válido solo si encontramos al menos un archivo (si hay múltiples
            // podría considerarse advertencia, pero a nivel de integridad de disco existe).
            return matches == 1;

        } catch (Exception e) {
            return false;
        }
    } // --- FIN del metodo hasAssociatedCompressedFile ---


    /**
     * Extrae el nombre base de un archivo (sin extensión).
     */
    private String obtenerNombreBase(String nombreArchivo) {
        int puntoIndex = nombreArchivo.indexOf('.');
        return (puntoIndex == -1) ? nombreArchivo : nombreArchivo.substring(0, puntoIndex);
    } // --- FIN del metodo obtenerNombreBase ---


    /**
     * Comprueba si la extensión del archivo corresponde a un formato asociado
     * (comprimidos ZIP/RAR/7Z o 3D directos STL/OBJ/3MF).
     */
    private boolean esExtensionAsociada(String nombreArchivo) {
        String nombreEnMinusculas = nombreArchivo.toLowerCase();
        return nombreEnMinusculas.endsWith(".zip") || 
               nombreEnMinusculas.endsWith(".rar") || 
               nombreEnMinusculas.endsWith(".7z") ||
               nombreEnMinusculas.endsWith(".stl") ||
               nombreEnMinusculas.endsWith(".obj") ||
               nombreEnMinusculas.endsWith(".3mf");
    } // --- FIN del metodo esExtensionAsociada ---


} // --- FIN de clase ValidationService ---
