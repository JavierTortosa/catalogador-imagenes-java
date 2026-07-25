package servicios.renderer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import servicios.ConfigKeys;
import servicios.ConfigurationManager;

/**
 * Servicio encargado exclusivamente de la creación, gestión de rutas y limpieza
 * segura de directorios temporales utilizados por el Modo RENDER (Zip2Png).
 */
public class RenderTempFileManager {

    private static final Logger logger = LoggerFactory.getLogger(RenderTempFileManager.class);

    private final Path outputDir;
    private final Path imagesDir;

    public RenderTempFileManager(ConfigurationManager config) {
        String temp = config.getString(ConfigKeys.ZIP2PNG_CARPETA_TEMP,
                System.getProperty("java.io.tmpdir") + File.separator + "visor_zip2png");
        this.outputDir = Path.of(temp);
        this.imagesDir = outputDir.resolve("imagenes");
        inicializarDirectorios();
    } // --- Fin del constructor RenderTempFileManager ---

    private void inicializarDirectorios() {
        try {
            Files.createDirectories(outputDir);
            Files.createDirectories(imagesDir);
            logger.info("[RenderTempFileManager] Directorio temporal inicializado en: {}", outputDir);
        } catch (IOException e) {
            logger.error("[RenderTempFileManager] No se pudo inicializar el directorio temporal: {}", outputDir, e);
        }
    } // --- Fin del metodo inicializarDirectorios ---

    public Path getOutputDir() {
        return outputDir;
    } // --- Fin del metodo getOutputDir ---

    public Path getImagesDir() {
        return imagesDir;
    } // --- Fin del metodo getImagesDir ---

    /**
     * Elimina el contenido del directorio de salida temporal de forma segura,
     * registrando advertencias si algún archivo se encuentra bloqueado.
     */
    public void limpiarOutputDir() {
        if (!Files.exists(outputDir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(outputDir)) {
            walk.filter(p -> !p.equals(outputDir))
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ex) {
                        logger.warn("[RenderTempFileManager] No se pudo eliminar el archivo temporal: {}", p, ex);
                    }
                });
            logger.info("[RenderTempFileManager] Directorio temporal limpiado correctamente.");
        } catch (IOException e) {
            logger.error("[RenderTempFileManager] Error al limpiar el directorio temporal: {}", outputDir, e);
        }
    } // --- Fin del metodo limpiarOutputDir ---

    /**
     * Elimina un directorio temporal especifico recursivamente.
     */
    public static void deleteDir(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception e) {
                        logger.warn("[RenderTempFileManager] No se pudo eliminar archivo temporal en deleteDir: {}", p, e);
                    }
                });
        } catch (Exception e) {
            logger.warn("[RenderTempFileManager] No se pudo limpiar el directorio temporal: {}", dir, e);
        }
    } // --- Fin del metodo deleteDir ---

} // --- Fin de la clase RenderTempFileManager ---
