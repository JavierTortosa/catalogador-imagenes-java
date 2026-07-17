package servicios.renderer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import modelo.renderer.ImageEntry;

/**
 * Escanea una carpeta buscando archivos 3D/archivo (STL, OBJ, 3MF, ZIP, RAR, 7z)
 * que NO tengan una imagen asociada (mismo nombre base, misma carpeta).
 * <p>
 * Rápido: no abre comprimidos, solo compara nombres de archivo.
 * Agrupa automáticamente archivos multivolumen (.partN.rar, .7z.NNN).
 */
public class Zip2PngScanner {

    private static final Set<String> EXT_BUSCADAS = Set.of(
            ".stl", ".obj", ".3mf",
            ".zip", ".rar", ".7z");

    private static final Set<String> EXT_IMAGEN = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");

    /**
     * Candidato a renderizar: archivo 3D o comprimido sin imagen asociada.
     */
    public static class RenderCandidate {
        public final Path path;
        public final String nombreBase;
        public final long tamanoBytes;
        public final boolean esComprimido;
        public final boolean excedeLimite;
        public final List<Path> volumenesAgrupados;
        public List<ImageEntry> imagenesInternas = List.of();

        public RenderCandidate(Path path, String nombreBase, long tamanoBytes,
                boolean esComprimido, boolean excedeLimite, List<Path> volumenesAgrupados) {
            this.path = path;
            this.nombreBase = nombreBase;
            this.tamanoBytes = tamanoBytes;
            this.esComprimido = esComprimido;
            this.excedeLimite = excedeLimite;
            this.volumenesAgrupados = volumenesAgrupados;
        }

        public boolean tieneImagenesDentro() {
            return esComprimido && !imagenesInternas.isEmpty();
        }
    }

    private final long limiteBytes;

    public Zip2PngScanner() {
        this(512L * 1024 * 1024);
    }

    public Zip2PngScanner(long limiteBytes) {
        this.limiteBytes = limiteBytes;
    }

    /**
     * Escanea una carpeta y notifica cada candidato encontrado via callback.
     * No bloquea: el callback se invoca desde el hilo de escaneo.
     *
     * @param folderPath carpeta a escanear
     * @param callback   recibe cada candidato encontrado (puede ser null)
     * @return lista completa de candidatos
     */
    public List<RenderCandidate> scanFolder(Path folderPath, Consumer<RenderCandidate> callback) {
        if (!Files.isDirectory(folderPath)) {
            return Collections.emptyList();
        }

        List<Path> allFiles;
        try (Stream<Path> walk = Files.walk(folderPath, 2)) {
            allFiles = walk.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            return Collections.emptyList();
        }

        List<RenderCandidate> candidates = new ArrayList<>();
        Set<String> imagenesEncontradas = new HashSet<>();

        for (Path file : allFiles) {
            String name = file.getFileName().toString().toLowerCase();
            if (esExtensionImagen(name)) {
                imagenesEncontradas.add(nombreBase(name));
            }
        }

        Map<String, List<Path>> archives = new HashMap<>();
        List<Path> standalone3D = new ArrayList<>();

        for (Path file : allFiles) {
            String name = file.getFileName().toString().toLowerCase();
            if (esExtensionArchivo(name)) {
                String groupKey = claveGrupo(name);
                archives.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(file);
            } else if (esExtension3D(name)) {
                standalone3D.add(file);
            }
        }

        for (List<Path> group : archives.values()) {
            Path first = group.get(0);
            String name = first.getFileName().toString().toLowerCase();
            String base = nombreBase(name);
            if (imagenesEncontradas.contains(base)) continue;

            long totalSize = group.stream().mapToLong(p -> p.toFile().length()).sum();
            boolean excede = totalSize > limiteBytes;
            RenderCandidate c = new RenderCandidate(first, base, totalSize, true, excede, group);
            candidates.add(c);
            if (callback != null) callback.accept(c);
        }

        for (Path file : standalone3D) {
            String name = file.getFileName().toString().toLowerCase();
            String base = nombreBase(name);
            if (imagenesEncontradas.contains(base)) continue;

            long size = file.toFile().length();
            boolean excede = size > limiteBytes;
            RenderCandidate c = new RenderCandidate(file, base, size, false, excede, List.of(file));
            candidates.add(c);
            if (callback != null) callback.accept(c);
        }

        return candidates;
    }

    public List<RenderCandidate> scanFolder(Path folderPath) {
        return scanFolder(folderPath, null);
    }

    /**
     * Escanea imágenes dentro de archivos comprimidos para todos los candidatos.
     * Pobla el campo {@code imagenesInternas} de cada candidato comprimido.
     *
     * @param candidates lista de candidatos a procesar
     */
    public static void detectarImagenesEnArchivos(List<RenderCandidate> candidates) {
        for (RenderCandidate c : candidates) {
            if (!c.esComprimido) continue;
            try {
                c.imagenesInternas = ZipExtractor.listImageContents(c.path);
            } catch (Exception e) {
                // Si falla, dejamos lista vacía
            }
        }
    }

    public static boolean esExtensionArchivo(String name) {
        return name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z");
    }

    public static boolean esExtensionImagen(String name) {
        return EXT_IMAGEN.stream().anyMatch(name::endsWith);
    }

    private String claveGrupo(String name) {
        String lower = name.toLowerCase();
        if (lower.matches(".+\\.part\\d+\\.(rar|7z)$")
                || lower.matches(".+\\.7z\\.\\d{3,}")
                || lower.matches(".+\\.r\\d+")
                || lower.matches(".+\\.z\\d{2}")) {
            int dot = name.indexOf('.');
            return (dot == -1) ? name : name.substring(0, dot);
        }
        return nombreBase(name);
    }

    private String nombreBase(String name) {
        int dot = name.indexOf('.');
        return (dot == -1) ? name : name.substring(0, dot);
    }

    private boolean esExtension3D(String name) {
        return name.endsWith(".stl") || name.endsWith(".obj") || name.endsWith(".3mf");
    }

} // --- Fin de la clase Zip2PngScanner ---
