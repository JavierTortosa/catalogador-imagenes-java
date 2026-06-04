package modelo.proyecto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Representa la configuración de exportación guardada para una única imagen en un proyecto.
 * Esta clase está diseñada para ser parte del ProjectModel y ser serializada a JSON.
 * Contiene toda la información que el usuario configura en el panel de exportación.
 */
public class ExportConfig {

    // --- ESTADO DE EXPORTACIÓN ---

    /**
     * Define si este ítem está seleccionado para ser incluido en la próxima exportación.
     * Corresponde al estado del checkbox en la tabla de exportación.
     * Por defecto, una imagen está seleccionada para exportar.
     */
    private boolean exportEnabled = true;

    /**
     * Define si el usuario ha decidido explícitamente ignorar la búsqueda o asignación
     * de un archivo comprimido para esta imagen.
     * Corresponde al estado "IGNORAR_COMPRIMIDO".
     */
    private boolean ignoreCompressed = false;

    /**
     * El estado del archivo (ej. ASIGNADO_AUTOMATICAMENTE, ASIGNADO_MANUAL, etc.)
     */
    private ExportStatus status;

    // --- ARCHIVOS ASOCIADOS ---

    /**
     * Código de catálogo asignado a esta imagen (ej. "C001", "C002").
     * Permite a los clientes referenciar imágenes fácilmente en el PDF.
     */
    private String codigoCatalogo;

    /** Número de piezas que componen el modelo */
    private int piezas;

    /** Número de piezas con soporte */
    private int piezasConSoporte;

    /** Número de piezas sin soporte */
    private int piezasSinSoporte;

    /** Nivel de dificultad de pintado (ej. "Fácil", "Medio", "Difícil") */
    private String lvl;

    /** Precio de venta al público */
    private String pvp;

    /** Notas u observaciones sobre este item */
    private String notas;

    /** Indica si el archivo comprimido contiene archivos .lys (licify) */
    private Boolean hasLychee;

    /** Indica si el archivo comprimido contiene archivos .ctb (chitubox) */
    private Boolean hasChitubox;

    /** Tamaño total del contenido comprimido en MB */
    private Double totalSizeMb;

    /**
     * La lista de rutas (como String) a los archivos asociados (.zip, .stl, etc.)
     * que el usuario ha asignado a esta imagen. Esta es la lista que se persiste.
     */
    private List<String> associatedFiles;

    
    // --- CONSTRUCTOR ---
    
    public ExportConfig() {
        this.associatedFiles = new ArrayList<>();
    } // --- Fin del constructor ExportConfig ---

    
    // --- GETTERS Y SETTERS ---

    public boolean isExportEnabled() {
        return exportEnabled;
    } // ---FIN de metodo isExportEnabled---

    public void setExportEnabled(boolean exportEnabled) {
        this.exportEnabled = exportEnabled;
    } // ---FIN de metodo setExportEnabled---

    public boolean isIgnoreCompressed() {
        return ignoreCompressed;
    } // ---FIN de metodo isIgnoreCompressed---

    public void setIgnoreCompressed(boolean ignoreCompressed) {
        this.ignoreCompressed = ignoreCompressed;
    } // ---FIN de metodo setIgnoreCompressed---

    public ExportStatus getStatus() {
        return status;
    } // ---FIN de metodo getStatus---

    public void setStatus(ExportStatus status) {
        this.status = status;
    } // ---FIN de metodo setStatus---

    public String getCodigoCatalogo() {
        return codigoCatalogo;
    }

    public void setCodigoCatalogo(String codigoCatalogo) {
        this.codigoCatalogo = codigoCatalogo;
    }

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

    public Boolean getHasLychee() { return hasLychee; }
    public void setHasLychee(Boolean hasLychee) { this.hasLychee = hasLychee; }
    public Boolean getHasChitubox() { return hasChitubox; }
    public void setHasChitubox(Boolean hasChitubox) { this.hasChitubox = hasChitubox; }
    public Double getTotalSizeMb() { return totalSizeMb; }
    public void setTotalSizeMb(Double totalSizeMb) { this.totalSizeMb = totalSizeMb; }

    public List<String> getAssociatedFiles() {
        // Garantiza que nunca devolvemos null, crucial para la deserialización desde JSON antiguos.
        if (this.associatedFiles == null) {
            this.associatedFiles = new ArrayList<>();
        }
        return associatedFiles;
    } // ---FIN de metodo getAssociatedFiles---

    public void setAssociatedFiles(List<String> associatedFiles) {
        this.associatedFiles = associatedFiles;
    } // ---FIN de metodo setAssociatedFiles---
    
    @Override
    public int hashCode() {
        return Objects.hash(associatedFiles, codigoCatalogo, exportEnabled, hasChitubox, hasLychee, ignoreCompressed, lvl, notas, piezas, piezasConSoporte, piezasSinSoporte, pvp, status, totalSizeMb);
    } // ---FIN de metodo hashCode---

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ExportConfig other = (ExportConfig) obj;
        return exportEnabled == other.exportEnabled && 
               ignoreCompressed == other.ignoreCompressed && 
               piezas == other.piezas &&
               piezasConSoporte == other.piezasConSoporte &&
               piezasSinSoporte == other.piezasSinSoporte &&
               status == other.status &&
               Objects.equals(associatedFiles, other.associatedFiles) &&
               Objects.equals(codigoCatalogo, other.codigoCatalogo) &&
               Objects.equals(hasChitubox, other.hasChitubox) &&
               Objects.equals(hasLychee, other.hasLychee) &&
               Objects.equals(lvl, other.lvl) &&
               Objects.equals(notas, other.notas) &&
               Objects.equals(pvp, other.pvp) &&
               Objects.equals(totalSizeMb, other.totalSizeMb);
    } // ---FIN de metodo equals---

} // --- FIN de clase ExportConfig ---
