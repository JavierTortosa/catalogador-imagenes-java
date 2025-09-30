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

    // --- ARCHIVOS ASOCIADOS ---

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
        return Objects.hash(associatedFiles, exportEnabled, ignoreCompressed);
    } // ---FIN de metodo hashCode---

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ExportConfig other = (ExportConfig) obj;
        return exportEnabled == other.exportEnabled && 
               ignoreCompressed == other.ignoreCompressed && 
               Objects.equals(associatedFiles, other.associatedFiles);
    } // ---FIN de metodo equals---

} // --- FIN de clase ExportConfig ---