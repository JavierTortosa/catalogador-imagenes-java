package controlador.services.proyecto;

import java.util.Collections;
import java.util.Set;

public class ExportStatusReport
{
    private final Set<String> nombresImagenEnConflicto;
    private final Set<String> nombresArchivoEnConflicto;

    public ExportStatusReport(Set<String> nombresImagenEnConflicto, Set<String> nombresArchivoEnConflicto)
    {
        this.nombresImagenEnConflicto = nombresImagenEnConflicto != null ? nombresImagenEnConflicto : Collections.emptySet();
        this.nombresArchivoEnConflicto = nombresArchivoEnConflicto != null ? nombresArchivoEnConflicto : Collections.emptySet();
    }

    public Set<String> getNombresImagenEnConflicto() { return nombresImagenEnConflicto; }
    public Set<String> getNombresArchivoEnConflicto() { return nombresArchivoEnConflicto; }
    public boolean hayConflictos() { return !nombresImagenEnConflicto.isEmpty() || !nombresArchivoEnConflicto.isEmpty(); }
}
