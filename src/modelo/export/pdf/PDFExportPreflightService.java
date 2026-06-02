package modelo.export.pdf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PDFExportPreflightService {

    public static class PreflightResult {
        public final boolean isSuccess;
        public final List<String> warnings;

        public PreflightResult(boolean isSuccess, List<String> warnings) {
            this.isSuccess = isSuccess;
            this.warnings = warnings;
        }
    }

    public PreflightResult checkPreflight(List<Path> filesToExport) {
        List<String> warnings = new ArrayList<>();

        // 1. Verificar librerías de compresión (No bloqueante)
        try {
            Class.forName("org.tukaani.xz.FilterOptions");
        } catch (ClassNotFoundException e) {
            warnings.add("⚠️ Falta librería XZ (no se podrán leer archivos 7z).");
        }

        try {
            Class.forName("com.github.junrar.Archive");
        } catch (ClassNotFoundException e) {
            warnings.add("⚠️ Falta librería Junrar (no se podrán leer archivos RAR).");
        }

        // 2. Verificar accesibilidad de archivos (Bloqueante)
        boolean archivosAccesibles = true;
        for (Path path : filesToExport) {
            if (!Files.exists(path)) {
                warnings.add("❌ Archivo no encontrado: " + path.getFileName());
                archivosAccesibles = false;
            } else if (!Files.isReadable(path)) {
                warnings.add("❌ Archivo no legible: " + path.getFileName());
                archivosAccesibles = false;
            }
        }

        return new PreflightResult(archivosAccesibles, warnings);
    }
}
