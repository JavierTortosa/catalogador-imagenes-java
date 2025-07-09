package modelo.proyecto;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;

public class ExportItem {

    private final Path rutaImagen; // Ruta absoluta de la imagen original
    private ImageIcon miniatura;   // Miniatura para mostrar en la tabla

    private Path rutaArchivoComprimido; // Ruta al .zip/.rar asociado
    private List<Path> candidatosArchivo; // Para el caso de múltiples candidatos

    private ExportStatus estadoArchivoComprimido;
    
    private boolean seleccionadoParaExportar = true;

    public ExportItem(Path rutaImagen) {
        this.rutaImagen = rutaImagen;
        this.estadoArchivoComprimido = ExportStatus.PENDIENTE;
        this.candidatosArchivo = new ArrayList<>();
    } // --- Fin del método ExportItem (constructor) ---

    // --- Getters y Setters ---

    public Path getRutaImagen() {
        return rutaImagen;
    } // --- Fin del método getRutaImagen ---

    public ImageIcon getMiniatura() {
        return miniatura;
    } // --- Fin del método getMiniatura ---

    public void setMiniatura(ImageIcon miniatura) {
        this.miniatura = miniatura;
    } // --- Fin del método setMiniatura ---

    public Path getRutaArchivoComprimido() {
        return rutaArchivoComprimido;
    } // --- Fin del método getRutaArchivoComprimido ---

    public void setRutaArchivoComprimido(Path rutaArchivoComprimido) {
        this.rutaArchivoComprimido = rutaArchivoComprimido;
    } // --- Fin del método setRutaArchivoComprimido ---

    public List<Path> getCandidatosArchivo() {
        return candidatosArchivo;
    } // --- Fin del método getCandidatosArchivo ---

    public void setCandidatosArchivo(List<Path> candidatosArchivo) {
        this.candidatosArchivo = candidatosArchivo;
    } // --- Fin del método setCandidatosArchivo ---

    public ExportStatus getEstadoArchivoComprimido() {
        return estadoArchivoComprimido;
    } // --- Fin del método getEstadoArchivoComprimido ---

    public void setEstadoArchivoComprimido(ExportStatus estadoArchivoComprimido) {
        this.estadoArchivoComprimido = estadoArchivoComprimido;
    } // --- Fin del método setEstadoArchivoComprimido ---
    
    public boolean isSeleccionadoParaExportar() {
        return seleccionadoParaExportar;
    } // --- Fin del método isSeleccionadoParaExportar ---

    public void setSeleccionadoParaExportar(boolean seleccionadoParaExportar) {
        this.seleccionadoParaExportar = seleccionadoParaExportar;
    } // --- Fin del método setSeleccionadoParaExportar ---

} // --- FIN de la clase ExportItem ---