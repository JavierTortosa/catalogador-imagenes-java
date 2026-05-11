package servicios;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio para gestionar la identificación de volúmenes físicos (discos).
 * Utiliza el número de serie del volumen (VSN) para diferenciar discos aunque cambie su letra.
 */
public class VolumeService {

    private static final Logger logger = LoggerFactory.getLogger(VolumeService.class);

    /**
     * Obtiene el número de serie del volumen para una ruta dada.
     * En Windows, esto suele ser una cadena hexadecimal de 8 caracteres.
     * 
     * @param path La ruta de la cual obtener el número de serie.
     * @return Un Optional con el número de serie, o vacío si no se pudo obtener.
     */
    public Optional<String> getVolumeSerialNumber(Path path) {
        try {
            FileStore store = Files.getFileStore(path);
            
            // En Windows, el atributo "volume:vsn" devuelve el número de serie del volumen.
            // Es un Integer. Lo convertimos a una representación hexadecimal limpia.
            Object vsn = store.getAttribute("volume:vsn");
            if (vsn instanceof Integer) {
                String serial = Integer.toHexString((Integer) vsn).toUpperCase();
                // Aseguramos que tenga 8 caracteres rellenando con ceros si es necesario
                while (serial.length() < 8) {
                    serial = "0" + serial;
                }
                logger.debug("Número de serie obtenido para {}: {}", path, serial);
                return Optional.of(serial);
            }
        } catch (IOException e) {
            logger.error("Error al obtener el FileStore para la ruta: {}", path, e);
        } catch (UnsupportedOperationException e) {
            logger.warn("El sistema de archivos no soporta la obtención del VSN: {}", path);
        } catch (Exception e) {
            logger.error("Error inesperado al obtener el VSN para: {}", path, e);
        }
        return Optional.empty();
    } // ---FIN de metodo [getVolumeSerialNumber]---

    /**
     * Obtiene el nombre/etiqueta del volumen.
     * 
     * @param path La ruta del volumen.
     * @return El nombre del volumen (ej: "Windows", "DATOS"), o "Desconocido".
     */
    public String getVolumeName(Path path) {
        try {
            FileStore store = Files.getFileStore(path);
            String name = store.name();
            return (name == null || name.isBlank()) ? "Sin Etiqueta" : name;
        } catch (IOException e) {
            return "Error al leer nombre";
        }
    } // ---FIN de metodo [getVolumeName]---

} // --- FIN de clase VolumeService ---
