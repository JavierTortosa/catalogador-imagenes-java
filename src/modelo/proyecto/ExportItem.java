package modelo.proyecto;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportItem {
	
	private static final Logger logger = LoggerFactory.getLogger(ExportItem.class);

    private final Path rutaImagen;
    private ImageIcon miniatura;

    // --- INICIO DE LA MODIFICACIÓN ---
    // El campo `rutaArchivoComprimido` se reemplaza por una lista.
    private List<Path> rutasArchivosAsociados;
    // --- FIN DE LA MODIFICACIÓN ---

    private List<Path> candidatosArchivo;
    private ExportStatus estadoArchivoComprimido;
    private boolean seleccionadoParaExportar = true;

    public ExportItem(Path rutaImagen) {
        this.rutaImagen = rutaImagen;
        this.estadoArchivoComprimido = ExportStatus.PENDIENTE;
        this.candidatosArchivo = new ArrayList<>();
        this.rutasArchivosAsociados = new ArrayList<>(); // <-- Inicializamos la nueva lista
    } // ---FIN de metodo [ExportItem]---

    // --- Getters y Setters ---

    public Path getRutaImagen() { return rutaImagen; } // ---FIN de metodo [getRutaImagen]---
    public ImageIcon getMiniatura() { return miniatura; } // ---FIN de metodo [getMiniatura]---
    public void setMiniatura(ImageIcon miniatura) { this.miniatura = miniatura; } // ---FIN de metodo [setMiniatura]---

    // --- INICIO DE LA MODIFICACIÓN ---
    /**
     * Devuelve la lista de rutas de archivos asociados (ej. zip, rar, stl).
     * @return Una lista de objetos Path.
     */
    public List<Path> getRutasArchivosAsociados() {
        return rutasArchivosAsociados;
    } // ---FIN de metodo [getRutasArchivosAsociados]---

    /**
     * Establece la lista completa de archivos asociados.
     * @param rutas La nueva lista de rutas.
     */
    public void setRutasArchivosAsociados(List<Path> rutas) {
        this.rutasArchivosAsociados = (rutas != null) ? new ArrayList<>(rutas) : new ArrayList<>();
    } // ---FIN de metodo [setRutasArchivosAsociados]---

    /**
     * Método de conveniencia para añadir una única ruta a la lista de archivos asociados.
     * @param ruta La ruta a añadir.
     */
    public void addRutaArchivoAsociado(Path ruta) {
        if (ruta != null && !this.rutasArchivosAsociados.contains(ruta)) {
            this.rutasArchivosAsociados.add(ruta);
        }
    } // ---FIN de metodo [addRutaArchivoAsociado]---
    
    /**
     * Obtiene la ruta del archivo comprimido. Por retrocompatibilidad, devuelve
     * la primera ruta de la lista si no está vacía.
     * @deprecated Usar {@link #getRutasArchivosAsociados()} para obtener la lista completa.
     * @return El primer Path de la lista, o null si está vacía.
     */
    @Deprecated
    public Path getRutaArchivoComprimido() {
        return rutasArchivosAsociados.isEmpty() ? null : rutasArchivosAsociados.get(0);
    } // ---FIN de metodo [getRutaArchivoComprimido]---

    /**
     * Establece una única ruta de archivo comprimido, limpiando las anteriores.
     * @deprecated Usar {@link #setRutasArchivosAsociados(List)} o {@link #addRutaArchivoAsociado(Path)}.
     * @param rutaArchivoComprimido La única ruta a establecer.
     */
    @Deprecated
    public void setRutaArchivoComprimido(Path rutaArchivoComprimido) {
        this.rutasArchivosAsociados.clear();
        if (rutaArchivoComprimido != null) {
            this.rutasArchivosAsociados.add(rutaArchivoComprimido);
        }
    } // ---FIN de metodo [setRutaArchivoComprimido]---
    // --- FIN DE LA MODIFICACIÓN ---

    public List<Path> getCandidatosArchivo() { return candidatosArchivo; } // ---FIN de metodo [getCandidatosArchivo]---
    public void setCandidatosArchivo(List<Path> candidatosArchivo) { this.candidatosArchivo = candidatosArchivo; } // ---FIN de metodo [setCandidatosArchivo]---
    public ExportStatus getEstadoArchivoComprimido() { return estadoArchivoComprimido; } // ---FIN de metodo [getEstadoArchivoComprimido]---
    public void setEstadoArchivoComprimido(ExportStatus estadoArchivoComprimido) { this.estadoArchivoComprimido = estadoArchivoComprimido; } // ---FIN de metodo [setEstadoArchivoComprimido]---
    public boolean isSeleccionadoParaExportar() { return seleccionadoParaExportar; } // ---FIN de metodo [isSeleccionadoParaExportar]---
    public void setSeleccionadoParaExportar(boolean seleccionadoParaExportar) { this.seleccionadoParaExportar = seleccionadoParaExportar; } // ---FIN de metodo [setSeleccionadoParaExportar]---

} // --- FIN de clase [ExportItem]---

