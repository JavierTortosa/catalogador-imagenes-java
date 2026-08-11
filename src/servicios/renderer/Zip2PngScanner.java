package servicios.renderer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RenderCandidate other)) return false;
            return path.equals(other.path);
        }

        @Override
        public int hashCode() {
            return path.hashCode();
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
     * Escanea una carpeta buscando candidatos sin representación externa.
     * <p>
     * El parámetro {@code includeSubfolders} controla la profundidad:
     * <ul>
     *   <li>{@code false} → {@code Files.walk(folderPath, 1)} (solo nivel inmediato).</li>
     *   <li>{@code true} → {@code Files.walk(folderPath, Integer.MAX_VALUE)} (recursivo).</li>
     * </ul>
     * <p>
     * {@code folderCallback} solo notifica los directorios visitados (útil para
     * progreso); NUNCA interviene en la agrupación de candidatos, que siempre se
     * deriva de {@code RenderCandidate.path.getParent()}.
     *
     * @param folderPath        carpeta a escanear
     * @param includeSubfolders true para recorrido recursivo completo
     * @param candidateCallback recibe cada candidato encontrado (puede ser null)
     * @param folderCallback    recibe cada directorio visitado (puede ser null)
     * @param entryCallback     recibe CADA entrada visitada (directorio o archivo), útil para progreso real (puede ser null)
     * @return lista completa de candidatos
     */
    public List<RenderCandidate> scanFolder(Path folderPath, boolean includeSubfolders,
            Consumer<RenderCandidate> candidateCallback, Consumer<Path> folderCallback,
            Consumer<Path> entryCallback) {
        if (!Files.isDirectory(folderPath)) {
            return Collections.emptyList();
        }

        int depth = includeSubfolders ? Integer.MAX_VALUE : 1;
        List<Path> allFiles = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(folderPath, depth)) {
            for (Iterator<Path> it = walk.iterator(); it.hasNext();) {
                Path p = it.next();
                if (entryCallback != null) entryCallback.accept(p);
                if (Files.isDirectory(p)) {
                    if (folderCallback != null) folderCallback.accept(p);
                } else if (Files.isRegularFile(p)) {
                    allFiles.add(p);
                }
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }

        List<RenderCandidate> candidates = new ArrayList<>();
        Set<String> imagenesEncontradas = new HashSet<>();

        // Clave compuesta "carpetaRelativa/nombreBase" (normalizada a /). Así
        // A/modelo.zip no desaparece por existir B/modelo.png.
        for (Path file : allFiles) {
            String name = file.getFileName().toString().toLowerCase();
            if (esExtensionImagen(name)) {
                imagenesEncontradas.add(claveRepresentacion(folderPath, file));
            }
        }

        Map<String, List<Path>> archives = new HashMap<>();
        List<Path> standalone3D = new ArrayList<>();

        for (Path file : allFiles) {
            String name = file.getFileName().toString().toLowerCase();
            if (esExtensionArchivo(name)) {
                archives.computeIfAbsent(claveGrupo(file), k -> new ArrayList<>()).add(file);
            } else if (esExtension3D(name)) {
                standalone3D.add(file);
            }
        }

        for (List<Path> group : archives.values()) {
            Path first = group.get(0);
            if (imagenesEncontradas.contains(claveRepresentacion(folderPath, first))) continue;

            long totalSize = group.stream().mapToLong(p -> p.toFile().length()).sum();
            boolean excede = totalSize > limiteBytes;
            RenderCandidate c = new RenderCandidate(first, nombreBase(first.getFileName().toString().toLowerCase()),
                    totalSize, true, excede, group);
            candidates.add(c);
            if (candidateCallback != null) candidateCallback.accept(c);
        }

        for (Path file : standalone3D) {
            if (imagenesEncontradas.contains(claveRepresentacion(folderPath, file))) continue;

            long size = file.toFile().length();
            boolean excede = size > limiteBytes;
            RenderCandidate c = new RenderCandidate(file, nombreBase(file.getFileName().toString().toLowerCase()),
                    size, false, excede, List.of(file));
            candidates.add(c);
            if (candidateCallback != null) candidateCallback.accept(c);
        }

        return candidates;
    }

    /**
     * Escanea recursivamente (incluye subcarpetas) notificando candidatos.
     *
     * @param folderPath carpeta a escanear
     * @param callback   recibe cada candidato encontrado (puede ser null)
     * @return lista completa de candidatos
     */
    public List<RenderCandidate> scanFolder(Path folderPath, Consumer<RenderCandidate> callback) {
        return scanFolder(folderPath, true, callback, null, null);
    }

    public List<RenderCandidate> scanFolder(Path folderPath) {
        return scanFolder(folderPath, true, null, null, null);
    }

    /**
     * Clave de representación: "carpetaRelativa/nombreBase" normalizada a '/'.
     * Se calcula IGUAL para imágenes y para archivos 3D/comprimidos, de modo que
     * el matching sea correcto incluso con varias subcarpetas en el mismo escaneo.
     */
    private String claveRepresentacion(Path root, Path file) {
        String base = nombreBase(file.getFileName().toString().toLowerCase());
        Path parent = file.getParent();
        if (parent == null || parent.equals(root)) {
            return "/" + base;
        }
        String rel = root.relativize(parent).toString().replace('\\', '/');
        return "/" + rel + "/" + base;
    }

    /**
     * Escanea imágenes dentro de archivos comprimidos para todos los candidatos.
     * Pobla el campo {@code imagenesInternas} de cada candidato comprimido.
     *
     * @param candidates lista de candidatos a procesar
     */
    public static void detectarImagenesEnArchivos(List<RenderCandidate> candidates) {
        detectarImagenesEnArchivos(candidates, null);
    }

    /**
     * Escanea imágenes dentro de archivos comprimidos para todos los candidatos.
     * Pobla el campo {@code imagenesInternas} de cada candidato comprimido y
     * notifica progreso por cada archivo comprimido procesado.
     *
     * @param candidates lista de candidatos a procesar
     * @param onArchivo  recibe {@code (procesados, total)} por cada comprimido con 7z (puede ser null)
     */
    public static void detectarImagenesEnArchivos(List<RenderCandidate> candidates,
            BiConsumer<Integer, Integer> onArchivo) {
        long total = candidates.stream().filter(c -> c.esComprimido).count();
        long procesados = 0;
        for (RenderCandidate c : candidates) {
            if (!c.esComprimido) continue;
            try {
                c.imagenesInternas = ZipExtractor.listImageContents(c.path);
            } catch (Exception e) {
                // Si falla, dejamos lista vacía
            }
            procesados++;
            if (onArchivo != null) onArchivo.accept((int) procesados, (int) total);
        }
    }

    public static boolean esExtensionArchivo(String name) {
        return name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z");
    }

    public static boolean esExtensionImagen(String name) {
        return EXT_IMAGEN.stream().anyMatch(name::endsWith);
    }

    /**
     * Clave de grupo multivolumen. Incorpora la carpeta del archivo para evitar
     * que volúmenes homónimos de carpetas distintas se fusionen en un solo grupo.
     */
    private String claveGrupo(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        String base;
        if (name.matches(".+\\.part\\d+\\.(rar|7z)$")
                || name.matches(".+\\.7z\\.\\d{3,}")
                || name.matches(".+\\.r\\d+")
                || name.matches(".+\\.z\\d{2}")) {
            int dot = name.indexOf('.');
            base = (dot == -1) ? name : name.substring(0, dot);
        } else {
            base = nombreBase(name);
        }
        Path parent = file.getParent();
        String carpeta = (parent == null) ? "" : parent.toString().replace('\\', '/');
        return carpeta + "/" + base;
    }

    private String nombreBase(String name) {
        String s = name.toLowerCase();
        // Quitar extensiones de archivo comprimido conocidas, iterativamente
        while (true) {
            String ext = null;
            if (s.endsWith(".rar")) ext = ".rar";
            else if (s.endsWith(".zip")) ext = ".zip";
            else if (s.endsWith(".7z")) ext = ".7z";
            else if (s.endsWith(".tar")) ext = ".tar";
            else if (s.endsWith(".gz")) ext = ".gz";
            else if (s.endsWith(".bz2")) ext = ".bz2";
            else if (s.endsWith(".xz")) ext = ".xz";
            else if (s.endsWith(".zst")) ext = ".zst";
            else if (s.endsWith(".lz")) ext = ".lz";
            else if (s.endsWith(".lz4")) ext = ".lz4";
            else if (s.endsWith(".001")) ext = ".001";
            else if (s.endsWith(".stl")) ext = ".stl";
            else if (s.endsWith(".obj")) ext = ".obj";
            else if (s.endsWith(".3mf")) ext = ".3mf";
            if (ext == null) break;
            String sinExt = s.substring(0, s.length() - ext.length());
            if (sinExt.isEmpty()) break;
            s = sinExt;
        }
        // Quitar sufijos de multivolumen (.partN, .rNN, .zNN, .7z.NNN)
        s = s.replaceAll("\\.part\\d+$", "");
        s = s.replaceAll("\\.r\\d+$", "");
        s = s.replaceAll("\\.z\\d{2}$", "");
        s = s.replaceAll("\\.7z\\.\\d{3,}$", "");
        // Quitar cualquier extensión de imagen restante
        for (String imgExt : new String[]{".png", ".jpg", ".jpeg", ".tif", ".tiff", ".bmp", ".webp", ".gif"}) {
            if (s.endsWith(imgExt)) {
                s = s.substring(0, s.length() - imgExt.length());
                break;
            }
        }
        // Windows no permite que una ruta termine en espacio ni en punto
        // (ej: "modelo .zip" → base "modelo "). Se recorta para poder usar la
        // base como nombre de archivo de salida (".png") sin InvalidPathException.
        s = s.trim();
        while (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.isEmpty()) {
            s = "archivo";
        }
        return s;
    }

    private boolean esExtension3D(String name) {
        return name.endsWith(".stl") || name.endsWith(".obj") || name.endsWith(".3mf");
    }

} // --- Fin de la clase Zip2PngScanner ---
