package servicios.renderer;

import java.nio.file.Path;
import java.util.List;

import servicios.renderer.Zip2PngScanner.RenderCandidate;

/**
 * Resultado de un escaneo agrupado por carpeta: conserva TODOS los candidatos
 * encontrados en esa carpeta (pendientes y con preview interna). La tabla del
 * Scanner solo proyecta este resultado; el orden de presentación no altera los
 * datos aquí contenidos.
 *
 * @param folder     carpeta real de los candidatos (parent del archivo 3D/comprimido)
 * @param candidates candidatos encontrados en {@code folder}
 */
public record ScannerFolderResult(Path folder, List<RenderCandidate> candidates) {

    /**
     * Número de candidatos sin representación (ni externa ni interna).
     *
     * @return cantidad de candidatos renderizables
     */
    public int pendientes() {
        int count = 0;
        for (RenderCandidate c : candidates) {
            if (!c.tieneImagenesDentro()) count++;
        }
        return count;
    }

    /**
     * Número de candidatos con imagen interna dentro del comprimido.
     *
     * @return cantidad de candidatos con preview interna
     */
    public int conPreview() {
        int count = 0;
        for (RenderCandidate c : candidates) {
            if (c.tieneImagenesDentro()) count++;
        }
        return count;
    }

    /**
     * Tamaño total en bytes de los candidatos PENDIENTES (los que irán al
     * pipeline de render).
     *
     * @return suma de bytes de los candidatos sin representación
     */
    public long tamanoPendientesBytes() {
        long sum = 0;
        for (RenderCandidate c : candidates) {
            if (!c.tieneImagenesDentro()) sum += c.tamanoBytes;
        }
        return sum;
    }

    /**
     * Porcentaje de huérfanos sobre los CANDIDATOS del escaneo (no sobre todos
     * los archivos de la carpeta): {@code pendientes / totalCandidatos}.
     *
     * @return 0.0 a 1.0
     */
    public double porcentajeHuerfanos() {
        int total = pendientes() + conPreview();
        if (total == 0) return 0.0;
        return (double) pendientes() / total;
    }
} // --- Fin de la clase ScannerFolderResult ---
