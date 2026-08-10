package modelo.proyecto;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.datos.ArchiveMetadata;

public class ExportItem {
	
	private static final Logger logger = LoggerFactory.getLogger(ExportItem.class);

    private final Path rutaImagen;
    private ImageIcon miniatura;
    private ArchiveMetadata metadata;

    // --- INICIO DE LA MODIFICACIÓN ---
    // El campo `rutaArchivoComprimido` se reemplaza por una lista.
    private List<Path> rutasArchivosAsociados;
    // --- FIN DE LA MODIFICACIÓN ---

    private List<Path> candidatosArchivo;
    private ExportStatus estadoArchivoComprimido;
    private boolean seleccionadoParaExportar = true;
    private boolean tieneConflictoDeNombre = false;
    private String codigoCatalogo;
    private int piezas;
    private int piezasConSoporte;
    private int piezasSinSoporte;
    private String lvl;
    private String pvp;
    private String notas;
    private Boolean lycheeOverride;
    private Boolean chituboxOverride;
    private Double totalSizeOverride;
    private long imageSize = -1; // -1 indica que no ha sido calculado
    private long associatedFilesSize = -1; // -1 indica que no ha sido calculado

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
        this.associatedFilesSize = -1; // Forzar recalcular el tamaño en la próxima petición
    } // ---FIN de metodo [setRutasArchivosAsociados]---

    /**
     * Método de conveniencia para añadir una única ruta a la lista de archivos asociados.
     * @param ruta La ruta a añadir.
     */
    public void addRutaArchivoAsociado(Path ruta) {
        if (ruta != null && !this.rutasArchivosAsociados.contains(ruta)) {
            this.rutasArchivosAsociados.add(ruta);
            this.associatedFilesSize = -1; // Forzar recalcular el tamaño en la próxima petición
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

    /**
     * Calcula (si es necesario) y devuelve el tamaño del archivo de imagen en bytes.
     * @return El tamaño del archivo de imagen, or 0 si no se puede leer.
     */
    public long getImageSize() {
        if (imageSize == -1) {
            if (!java.nio.file.Files.exists(rutaImagen)) {
                logger.debug("Archivo de imagen no encontrado (no se calculará su tamaño): {}", rutaImagen);
                imageSize = 0;
            } else {
                try {
                    imageSize = java.nio.file.Files.size(rutaImagen);
                } catch (java.io.IOException e) {
                    logger.debug("No se pudo leer el tamaño del archivo de imagen: {}", rutaImagen);
                    imageSize = 0;
                }
            }
        }
        return imageSize;
    } // ---FIN de metodo [getImageSize]---

    /**
     * Calcula (si es necesario) y devuelve el tamaño total de todos los archivos asociados en bytes.
     * @return El tamaño total de los archivos asociados, or 0 si no hay o no se pueden leer.
     */
    public long getAssociatedFilesSize() {
        if (associatedFilesSize == -1) {
            long total = 0;
            if (rutasArchivosAsociados != null) {
                for (Path p : rutasArchivosAsociados) {
                    if (p == null || !java.nio.file.Files.exists(p)) {
                        // Archivo movido, borrado o inaccesible: no calcular tamaño
                        logger.debug("Archivo asociado no encontrado (no se calculará su tamaño): {}", p);
                        continue;
                    }
                    try {
                        total += java.nio.file.Files.size(p);
                    } catch (java.io.IOException e) {
                        logger.debug("No se pudo leer el tamaño del archivo asociado: {}", p);
                    }
                }
            }
            associatedFilesSize = total;
        }
        return associatedFilesSize;
    } // ---FIN de metodo [getAssociatedFilesSize]---

    /**
     * Devuelve la suma del tamaño de la imagen y de todos sus archivos asociados.
     * @return El tamaño total del item en bytes.
     */
    public long getTotalSize() {
        return getImageSize() + getAssociatedFilesSize();
    } // ---FIN de metodo [getTotalSize]---
    
    
    public List<Path> getCandidatosArchivo() { return candidatosArchivo; } // ---FIN de metodo [getCandidatosArchivo]---
    public void setCandidatosArchivo(List<Path> candidatosArchivo) { this.candidatosArchivo = candidatosArchivo; } // ---FIN de metodo [setCandidatosArchivo]---
    public ExportStatus getEstadoArchivoComprimido() { return estadoArchivoComprimido; } // ---FIN de metodo [getEstadoArchivoComprimido]---
    public void setEstadoArchivoComprimido(ExportStatus estadoArchivoComprimido) { this.estadoArchivoComprimido = estadoArchivoComprimido; } // ---FIN de metodo [setEstadoArchivoComprimido]---
    public boolean isSeleccionadoParaExportar() { return seleccionadoParaExportar; } // ---FIN de metodo [isSeleccionadoParaExportar]---
    public void setSeleccionadoParaExportar(boolean seleccionadoParaExportar) { this.seleccionadoParaExportar = seleccionadoParaExportar; } // ---FIN de metodo [setSeleccionadoParaExportar]---
    public boolean tieneConflictoDeNombre() { return tieneConflictoDeNombre; } // ---FIN de metodo [tieneConflictoDeNombre]---
    public void setTieneConflictoDeNombre(boolean tieneConflictoDeNombre) { this.tieneConflictoDeNombre = tieneConflictoDeNombre; } // ---FIN de metodo [setTieneConflictoDeNombre]---
    public String getCodigoCatalogo() { return codigoCatalogo; }
    public void setCodigoCatalogo(String codigoCatalogo) { this.codigoCatalogo = codigoCatalogo; }
    public int getPiezas() { return piezas; }
    public void setPiezas(int piezas) { this.piezas = piezas; }
    public int getPiezasConSoporte() { return piezasConSoporte; }
    public void setPiezasConSoporte(int piezasConSoporte) { this.piezasConSoporte = piezasConSoporte; }
    public int getPiezasSinSoporte() { return piezasSinSoporte; }
    public void setPiezasSinSoporte(int piezasSinSoporte) { this.piezasSinSoporte = piezasSinSoporte; }
    public String getLvl() { return lvl; }
    public void setLvl(String lvl) { this.lvl = lvl; }
    public String getPvp() { return pvp; }
    public void setPvp(String pvp) { this.pvp = pvp; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public ArchiveMetadata getMetadata() { return metadata; }
    public void setMetadata(ArchiveMetadata metadata) { this.metadata = metadata; }

    public boolean hasLychee() {
        if (lycheeOverride != null) return lycheeOverride;
        return metadata != null && metadata.hasLychee;
    }
    public void setHasLychee(boolean value) { this.lycheeOverride = value; }

    public boolean hasChitubox() {
        if (chituboxOverride != null) return chituboxOverride;
        return metadata != null && metadata.hasChitubox;
    }
    public void setHasChitubox(boolean value) { this.chituboxOverride = value; }

    public double getTotalSizeMb() {
        if (totalSizeOverride != null) return totalSizeOverride;
        return metadata != null ? metadata.totalSizeMb : 0;
    }
    public void setTotalSizeMb(double value) { this.totalSizeOverride = value; }

    public Boolean getLycheeOverride() { return lycheeOverride; }
    public void setLycheeOverride(Boolean lycheeOverride) { this.lycheeOverride = lycheeOverride; }
    public Boolean getChituboxOverride() { return chituboxOverride; }
    public void setChituboxOverride(Boolean chituboxOverride) { this.chituboxOverride = chituboxOverride; }
    public Double getTotalSizeOverride() { return totalSizeOverride; }
    public void setTotalSizeOverride(Double totalSizeOverride) { this.totalSizeOverride = totalSizeOverride; }

} // --- FIN de clase [ExportItem]---

