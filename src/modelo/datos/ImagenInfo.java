package modelo.datos;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * POJO (Plain Old Java Object) que representa la información de una imagen
 * almacenada en la base de datos.
 */
public class ImagenInfo {

    private long id;
    private String rutaCompleta;
    private String nombreArchivo;
    private long fechaModificacion;
    private long tamanoBytes;
    private long discoId;
    private String rutaRelativa;
    private long fechaAdicion;

    // Constructor por defecto
    public ImagenInfo() {
    } // ---FIN de constructor [ImagenInfo]---

    // --- Getters y Setters ---

    public long getId() {
        return id;
    } // ---FIN de metodo [getId]---

    public void setId(long id) {
        this.id = id;
    } // ---FIN de metodo [setId]---

    public String getRutaCompleta() {
        return rutaCompleta;
    } // ---FIN de metodo [getRutaCompleta]---
    
    public Path getRutaCompletaAsPath() {
        return (rutaCompleta != null) ? Paths.get(rutaCompleta) : null;
    } // ---FIN de metodo [getRutaCompletaAsPath]---

    public void setRutaCompleta(String rutaCompleta) {
        this.rutaCompleta = rutaCompleta;
    } // ---FIN de metodo [setRutaCompleta]---
    
    public void setRutaCompletaFromPath(Path path) {
        this.rutaCompleta = (path != null) ? path.toString() : null;
    } // ---FIN de metodo [setRutaCompletaFromPath]---

    public String getNombreArchivo() {
        return nombreArchivo;
    } // ---FIN de metodo [getNombreArchivo]---

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    } // ---FIN de metodo [setNombreArchivo]---

    public long getFechaModificacion() {
        return fechaModificacion;
    } // ---FIN de metodo [getFechaModificacion]---

    public void setFechaModificacion(long fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    } // ---FIN de metodo [setFechaModificacion]---

    public long getTamanoBytes() {
        return tamanoBytes;
    } // ---FIN de metodo [getTamanoBytes]---

    public void setTamanoBytes(long tamanoBytes) {
        this.tamanoBytes = tamanoBytes;
    } // ---FIN de metodo [setTamanoBytes]---

    public long getDiscoId() {
        return discoId;
    } // ---FIN de metodo [getDiscoId]---

    public void setDiscoId(long discoId) {
        this.discoId = discoId;
    } // ---FIN de metodo [setDiscoId]---

    public String getRutaRelativa() {
        return rutaRelativa;
    } // ---FIN de metodo [getRutaRelativa]---

    public void setRutaRelativa(String rutaRelativa) {
        this.rutaRelativa = rutaRelativa;
    } // ---FIN de metodo [setRutaRelativa]---

    public long getFechaAdicion() {
        return fechaAdicion;
    } // ---FIN de metodo [getFechaAdicion]---

    public void setFechaAdicion(long fechaAdicion) {
        this.fechaAdicion = fechaAdicion;
    } // ---FIN de metodo [setFechaAdicion]---

    // --- Métodos de utilidad (equals, hashCode, toString) ---

    @Override
    public String toString() {
        return "ImagenInfo{" +
               "id=" + id +
               ", nombreArchivo='" + nombreArchivo + '\'' +
               ", rutaCompleta='" + rutaCompleta + '\'' +
               '}';
    } // ---FIN de metodo [toString]---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImagenInfo that = (ImagenInfo) o;
        return id == that.id &&
               Objects.equals(rutaCompleta, that.rutaCompleta);
    } // ---FIN de metodo [equals]---

    @Override
    public int hashCode() {
        return Objects.hash(id, rutaCompleta);
    } // ---FIN de metodo [hashCode]---

} // --- FIN de clase ImagenInfo ---