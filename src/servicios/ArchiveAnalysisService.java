package servicios;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import modelo.datos.ArchiveMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchiveAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(ArchiveAnalysisService.class);

    public ArchiveMetadata analyze(Path archivePath) {
        return analyze(archivePath, null);
    }

    public ArchiveMetadata analyze(Path archivePath, java.util.function.Consumer<String> progressReporter) {
        ArchiveMetadata meta = new ArchiveMetadata();
        meta.archivePath = archivePath.toString();
        meta.analysisDate = System.currentTimeMillis();

        if (progressReporter != null) progressReporter.accept("Listando contenido: " + archivePath.getFileName());
        analyzRecursive(archivePath, meta, progressReporter);

        meta.isMultipart = meta.stlCount > 1;
        return meta;
    }

    private void analyzRecursive(Path archivePath, ArchiveMetadata meta, java.util.function.Consumer<String> progressReporter) {
        List<String> internalPaths = new ArrayList<>();
        long totalBytes = 0;

        String exePath = get7zExePath();
        ProcessBuilder pb = new ProcessBuilder(exePath, "l", "-slt", archivePath.toString());
        Process p = null;
        try {
            p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Path = ")) {
                        String internalPath = line.substring(7).toLowerCase();
                        if (archivePath.toString().toLowerCase().endsWith(internalPath)) continue;
                        internalPaths.add(internalPath);
                    }
                    if (line.startsWith("Size = ")) {
                        try { totalBytes += Long.parseLong(line.substring(7).trim()); } catch (Exception e) {}
                    }
                }
            }
            if (!p.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)) {
                logger.warn("Timeout leyendo metadata de {}. Matando proceso.", archivePath);
                p.destroyForcibly();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupción leyendo metadata de {}. Matando proceso.", archivePath);
            Thread.currentThread().interrupt();
            return;
        } catch (Exception e) {
            logger.error("Error ejecutando 7z para: " + archivePath, e);
            return;
        } finally {
            if (p != null && p.isAlive()) {
                p.destroyForcibly();
            }
        }

        meta.totalSizeMb = totalBytes / (1024.0 * 1024.0);

        // Procesar cada archivo interno
        for (String internalPath : internalPaths) {
            if (isModelFile(internalPath)) {
                meta.stlCount++;
                if (isSupportedPath(internalPath)) meta.supportedStlCount++;
                else meta.unsupportedStlCount++;
            }
            if (internalPath.endsWith(".lys")) meta.hasLychee = true;
            if (internalPath.endsWith(".ctb")) meta.hasChitubox = true;

            if (isNestedArchive(internalPath)) {
                // Extraer el archivo anidado a temporal y analizarlo recursivamente
                Path tempDir = null;
                try {
                    if (progressReporter != null) {
                        String name = internalPath;
                        int lastSlash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
                        if (lastSlash >= 0) name = name.substring(lastSlash + 1);
                        progressReporter.accept("Extrayendo comprimido anidado: " + name);
                    }
                    tempDir = Files.createTempDirectory("visor_archive_");
                    Path nestedFile = extractToTemp(archivePath, internalPath, tempDir);
                    if (nestedFile != null && Files.exists(nestedFile)) {
                        analyzRecursive(nestedFile, meta, progressReporter);
                    }
                } catch (Exception e) {
                    logger.warn("No se pudo analizar archivo anidado: {} dentro de {}", internalPath, archivePath);
                } finally {
                    if (tempDir != null) deleteDir(tempDir);
                }
            }
        }
    }

    private Path extractToTemp(Path archivePath, String internalPath, Path tempDir) {
        Process p = null;
        try {
            String exePath = get7zExePath();
            ProcessBuilder pb = new ProcessBuilder(exePath, "x", archivePath.toString(),
                    "-o" + tempDir.toString(), internalPath, "-y");
            p = pb.start();
            
            // Esperar con un timeout razonable (ej. 2 minutos) para evitar bloqueos infinitos
            if (!p.waitFor(120, java.util.concurrent.TimeUnit.SECONDS)) {
                logger.warn("Timeout esperando la extracción de {}. Forzando cierre.", internalPath);
                p.destroyForcibly();
                return null;
            }
            
            // Devolver la ruta al archivo extraído
            Path nested = tempDir.resolve(internalPath);
            return nested;
        } catch (InterruptedException e) {
            logger.warn("Interrupción durante la extracción de {}. Matando proceso.", internalPath);
            if (p != null) p.destroyForcibly();
            Thread.currentThread().interrupt(); // Restaurar el estado de interrupción
            return null;
        } catch (Exception e) {
            logger.error("Error extrayendo {} de {}: {}", internalPath, archivePath, e.getMessage());
            if (p != null) p.destroyForcibly();
            return null;
        }
    }

    private void deleteDir(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception e) {} });
        } catch (Exception e) {
            logger.warn("No se pudo limpiar temporal: {}", dir);
        }
    }

    private String get7zExePath() {
        return ExternalToolsManager.get7zPath();
    }

    private boolean isModelFile(String p) {
        return p.endsWith(".stl") || p.endsWith(".obj") || p.endsWith(".3mf");
    }

    private boolean isSupportedPath(String p) {
        if (p.contains("unsupported") || p.contains("without") || p.contains("raw") || p.contains("no_support")) return false;
        return p.contains("supported") || p.contains("presupp");
    }

    private boolean isNestedArchive(String p) {
        return p.endsWith(".zip") || p.endsWith(".rar") || p.endsWith(".7z");
    }
}