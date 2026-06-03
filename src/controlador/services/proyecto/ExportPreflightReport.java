package controlador.services.proyecto;

import java.util.Collections;
import java.util.List;

public class ExportPreflightReport
{
    private final boolean destinoValido;
    private final String mensajeError;
    private final List<String> archivosEnConflicto;
    private final int totalItemsSeleccionados;

    public ExportPreflightReport(boolean destinoValido, String mensajeError, List<String> archivosEnConflicto, int totalItemsSeleccionados)
    {
        this.destinoValido = destinoValido;
        this.mensajeError = mensajeError;
        this.archivosEnConflicto = archivosEnConflicto != null ? archivosEnConflicto : Collections.emptyList();
        this.totalItemsSeleccionados = totalItemsSeleccionados;
    }

    public boolean isDestinoValido() { return destinoValido; }
    public String getMensajeError() { return mensajeError; }
    public List<String> getArchivosEnConflicto() { return archivosEnConflicto; }
    public int getTotalItemsSeleccionados() { return totalItemsSeleccionados; }
    public boolean hayConflictos() { return !archivosEnConflicto.isEmpty(); }
}
