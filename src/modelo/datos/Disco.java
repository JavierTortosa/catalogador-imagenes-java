package modelo.datos;

/**
 * POJO que representa un disco o volumen de almacenamiento en la base de datos.
 */
public class Disco {

    private long id;
    private String numeroSerie;
    private String nombreEtiqueta;
    private String ultimaRutaConocida; // Ej: "E:\"
    private long cantidadImagenes; // Campo calculado, no persistido directamente en 'discos'

    public Disco() {
    } // ---FIN de constructor [Disco]---

    public Disco(String numeroSerie, String nombreEtiqueta, String ultimaRutaConocida) {
        this.numeroSerie = numeroSerie;
        this.nombreEtiqueta = nombreEtiqueta;
        this.ultimaRutaConocida = ultimaRutaConocida;
    } // ---FIN de constructor parametrizado [Disco]---

    // --- Getters y Setters ---

    public long getId() {
        return id;
    } // ---FIN de metodo [getId]---

    public void setId(long id) {
        this.id = id;
    } // ---FIN de metodo [setId]---

    public String getNumeroSerie() {
        return numeroSerie;
    } // ---FIN de metodo [getNumeroSerie]---

    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    } // ---FIN de metodo [setNumeroSerie]---

    public String getNombreEtiqueta() {
        return nombreEtiqueta;
    } // ---FIN de metodo [getNombreEtiqueta]---

    public void setNombreEtiqueta(String nombreEtiqueta) {
        this.nombreEtiqueta = nombreEtiqueta;
    } // ---FIN de metodo [setNombreEtiqueta]---

    public String getUltimaRutaConocida() {
        return ultimaRutaConocida;
    } // ---FIN de metodo [getUltimaRutaConocida]---

    public void setUltimaRutaConocida(String ultimaRutaConocida) {
        this.ultimaRutaConocida = ultimaRutaConocida;
    } // ---FIN de metodo [setUltimaRutaConocida]---

    public long getCantidadImagenes() {
        return cantidadImagenes;
    } // ---FIN de metodo [getCantidadImagenes]---

    public void setCantidadImagenes(long cantidadImagenes) {
        this.cantidadImagenes = cantidadImagenes;
    } // ---FIN de metodo [setCantidadImagenes]---

    @Override
    public String toString() {
        return "Disco{" +
               "id=" + id +
               ", numeroSerie='" + numeroSerie + '\'' +
               ", etiqueta='" + nombreEtiqueta + '\'' +
               ", ultimaRuta='" + ultimaRutaConocida + '\'' +
               '}';
    } // ---FIN de metodo [toString]---

} // --- FIN de clase Disco ---
