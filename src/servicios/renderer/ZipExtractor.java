package servicios.renderer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import modelo.renderer.ImageEntry;
import modelo.renderer.StlEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import servicios.ExternalToolsManager;

/**
 * Extrae archivos comprimidos (ZIP, RAR, 7z) usando 7z.exe.
 * Soporta multivolumen: para extraer se pasa siempre el primer volumen.
 */
public class ZipExtractor {

    private static final Logger logger = LoggerFactory.getLogger(ZipExtractor.class);

    private static final Set<String> EXT_IMAGEN = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".tiff", ".tif");

    /**
     * Extrae el contenido completo de un archivo comprimido a un directorio temporal.
     *
     * @param archivePath ruta del archivo comprimido
     * @return ruta del directorio temporal con los archivos extraídos
     * @throws IOException si ocurre un error durante la extracción
     */
    public static Path extractToTemp(Path archivePath) throws IOException {
        Path tempDir = Files.createTempDirectory("zip2png_");
        extract(archivePath, tempDir);
        return tempDir;
    }

    /**
     * Extrae el contenido de un archivo comprimido a un directorio específico.
     *
     * @param archivePath ruta del archivo comprimido
     * @param outputDir   directorio de salida
     * @throws IOException si ocurre un error durante la extracción
     */
    public static void extract(Path archivePath, Path outputDir) throws IOException {
        String exePath = ExternalToolsManager.get7zPath();
        ProcessBuilder pb = new ProcessBuilder(exePath, "x", archivePath.toString(),
                "-o" + outputDir.toString(), "-y");
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            if (!p.waitFor(120, java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new IOException("Timeout extrayendo " + archivePath);
            }
            if (p.exitValue() != 0) {
                throw new IOException("7z exit code " + p.exitValue() + " extrayendo " + archivePath);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupción extrayendo " + archivePath, e);
        }
    }

    /**
     * Lista el contenido de un archivo comprimido sin extraerlo.
     *
     * @param archivePath ruta del archivo comprimido
     * @return lista de paths internos
     * @throws IOException si ocurre un error
     */
    public static java.util.List<String> listContents(Path archivePath) throws IOException {
        java.util.List<String> contents = new java.util.ArrayList<>();
        String exePath = ExternalToolsManager.get7zPath();
        ProcessBuilder pb = new ProcessBuilder(exePath, "l", "-ba", archivePath.toString());
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            if (!p.waitFor(120, java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new IOException("Timeout listando " + archivePath);
            }
            for (String line : output.split("\\r?\\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    contents.add(trimmed);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupción listando " + archivePath, e);
        }
        return contents;
    }

    /**
     * Lista solo los archivos STL de un comprimido con sus tamaños,
     * usando 7z con formato -slt (etiquetado) para parseo robusto.
     *
     * @param archivePath ruta del archivo comprimido
     * @return lista de StlEntry encontrados
     * @throws IOException si ocurre un error
     */
    public static List<StlEntry> listStlContents(Path archivePath) throws IOException {
        List<StlEntry> entries = new ArrayList<>();
        String exePath = ExternalToolsManager.get7zPath();
        ProcessBuilder pb = new ProcessBuilder(exePath, "l", "-slt", archivePath.toString());
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            if (!p.waitFor(120, java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new IOException("Timeout listando " + archivePath);
            }
            String currentPath = null;
            long currentSize = 0;
            for (String line : output.split("\\r?\\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("Path = ")) {
                    currentPath = trimmed.substring(6).trim();
                } else if (trimmed.startsWith("Size = ")) {
                    try {
                        currentSize = Long.parseLong(trimmed.substring(6).trim());
                    } catch (NumberFormatException e) {
                        currentSize = 0;
                    }
                } else if (trimmed.equals("--") || trimmed.isEmpty()) {
                    if (currentPath != null && currentPath.toLowerCase().endsWith(".stl")) {
                        entries.add(new StlEntry(currentPath, currentSize));
                    }
                    currentPath = null;
                    currentSize = 0;
                }
            }
            // Ultima entrada si no hay separador final
            if (currentPath != null && currentPath.toLowerCase().endsWith(".stl")) {
                entries.add(new StlEntry(currentPath, currentSize));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupción listando " + archivePath, e);
        }
        return entries;
    }

    /**
     * Lista archivos de imagen dentro de un comprimido con sus tamaños,
     * usando 7z con formato -slt (etiquetado) para parseo robusto.
     *
     * @param archivePath ruta del archivo comprimido
     * @return lista de ImageEntry encontrados
     * @throws IOException si ocurre un error
     */
    public static List<ImageEntry> listImageContents(Path archivePath) throws IOException {
        List<ImageEntry> entries = new ArrayList<>();
        String exePath = ExternalToolsManager.get7zPath();
        ProcessBuilder pb = new ProcessBuilder(exePath, "l", "-slt", archivePath.toString());
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            if (!p.waitFor(120, java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new IOException("Timeout listando imágenes en " + archivePath);
            }
            String currentPath = null;
            long currentSize = 0;
            for (String line : output.split("\\r?\\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("Path = ")) {
                    currentPath = trimmed.substring(6).trim();
                } else if (trimmed.startsWith("Size = ")) {
                    try {
                        currentSize = Long.parseLong(trimmed.substring(6).trim());
                    } catch (NumberFormatException e) {
                        currentSize = 0;
                    }
                } else if (trimmed.equals("--") || trimmed.isEmpty()) {
                    if (currentPath != null && esExtensionImagen(currentPath)) {
                        entries.add(new ImageEntry(currentPath, currentSize));
                    }
                    currentPath = null;
                    currentSize = 0;
                }
            }
            if (currentPath != null && esExtensionImagen(currentPath)) {
                entries.add(new ImageEntry(currentPath, currentSize));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupción listando imágenes en " + archivePath, e);
        }
        return entries;
    }

    private static boolean esExtensionImagen(String path) {
        String lower = path.toLowerCase();
        return EXT_IMAGEN.stream().anyMatch(lower::endsWith);
    }

    /**
     * Extrae un único archivo de un comprimido a un directorio de salida,
     * preservando la estructura de subdirectorios interna.
     *
     * @param archivePath  ruta del archivo comprimido
     * @param internalPath ruta interna del archivo a extraer
     * @param outputDir    directorio de salida
     * @throws IOException si ocurre un error
     */
    public static void extractSingleFile(Path archivePath, String internalPath, Path outputDir) throws IOException {
        String exePath = ExternalToolsManager.get7zPath();
        ProcessBuilder pb = new ProcessBuilder(exePath, "x", archivePath.toString(),
                "-o" + outputDir.toString(), internalPath, "-y");
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            if (!p.waitFor(120, java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new IOException("Timeout extrayendo " + internalPath + " de " + archivePath);
            }
            if (p.exitValue() != 0) {
                throw new IOException("7z exit code " + p.exitValue() + " extrayendo " + internalPath);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupción extrayendo " + internalPath, e);
        }
    }

    /**
     * Elimina un directorio temporal recursivamente.
     */
    public static void deleteDir(Path dir) {
        if (dir == null || !Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (Exception e) { logger.warn("No se pudo eliminar archivo temporal en ZipExtractor: {}", p, e); }
                    });
        } catch (Exception e) {
            logger.warn("No se pudo limpiar temporal en ZipExtractor: {}", dir, e);
        }
    }

    /**
     * Comprueba si un archivo es un comprimido válido.
     */
    public static boolean isValidArchive(Path path) {
        try {
            listContents(path);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

} // --- Fin de la clase ZipExtractor ---
