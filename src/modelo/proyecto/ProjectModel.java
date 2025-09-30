package modelo.proyecto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Representa la estructura completa de un proyecto.
 * Esta clase está diseñada para ser serializada y deserializada a formato JSON,
 * sirviendo como el modelo de datos para la persistencia del proyecto.
 */
public class ProjectModel {

    // --- Metadatos del Proyecto ---
    private String projectName;
    private String projectDescription;
    private long creationDate;
    private long lastModifiedDate;

    // --- Datos de Imágenes ---
    // Mantiene el modelo actual: un mapa de la ruta de la imagen (como String) a su etiqueta opcional.
    private Map<String, String> selectedImages;

    // Los descartes no tienen etiquetas, por lo que una simple lista de rutas (como String) es suficiente.
    private List<String> discardedImages;

    // --- Datos para la Exportación (Persistencia de trabajo) ---
    // Mapea una ruta de imagen a la lista de archivos asociados (ej. .stl, .zip)
    // que el usuario ha asignado manualmente en el panel de exportación.
    private Map<String, ExportConfig> exportConfigs;
    
    // --- Metadatos de Recuperación ---
    // Guarda la ruta del archivo de proyecto original cuando este modelo se guarda
    // como un archivo de recuperación temporal. Es nulo en un guardado normal.
    private String originalProjectPath;
    
    
    // --- Constructor ---
    public ProjectModel() {
        this.selectedImages = new LinkedHashMap<>();
        this.discardedImages = new ArrayList<>();
        this.exportConfigs = new LinkedHashMap<>();
        this.creationDate = System.currentTimeMillis();
        this.lastModifiedDate = this.creationDate;
    } // --- Fin del método ProjectModel (constructor) ---

    // --- Getters y Setters ---

    public String getProjectName() {
        return projectName;
    } // --- FIN de metodo getProjectName ---

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    } // --- FIN de metodo setProjectName ---

    public String getProjectDescription() {
        return projectDescription;
    } // --- FIN de metodo getProjectDescription ---

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    } // --- FIN de metodo setProjectDescription ---

    public long getCreationDate() {
        return creationDate;
    } // --- FIN de metodo getCreationDate ---

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    } // --- FIN de metodo setCreationDate ---

    public long getLastModifiedDate() {
        return lastModifiedDate;
    } // --- FIN de metodo getLastModifiedDate ---

    public void setLastModifiedDate(long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    } // --- FIN de metodo setLastModifiedDate ---

    public Map<String, String> getSelectedImages() {
        // Asegurarse de que nunca sea nulo para evitar NullPointerExceptions
        if (selectedImages == null) {
            selectedImages = new LinkedHashMap<>();
        }
        return selectedImages;
    } // --- FIN de metodo getSelectedImages ---

    public void setSelectedImages(Map<String, String> selectedImages) {
        this.selectedImages = selectedImages;
    } // --- FIN de metodo setSelectedImages ---

    public List<String> getDiscardedImages() {
        // Asegurarse de que nunca sea nulo
        if (discardedImages == null) {
            discardedImages = new ArrayList<>();
        }
        return discardedImages;
    } // --- FIN de metodo getDiscardedImages ---

    public void setDiscardedImages(List<String> discardedImages) {
        this.discardedImages = discardedImages;
    } // --- FIN de metodo setDiscardedImages ---

    public Map<String, ExportConfig> getExportConfigs() {
        // Asegurarse de que nunca sea nulo
        if (exportConfigs == null) {
            exportConfigs = new LinkedHashMap<>();
        }
        return exportConfigs;
    } // --- FIN de metodo getExportConfigs ---

    public void setExportConfigs(Map<String, ExportConfig> exportConfigs) {
        this.exportConfigs = exportConfigs;
    } // --- FIN de metodo setExportConfigs ---
    
    public String getOriginalProjectPath() {
        return originalProjectPath;
    } // ---FIN de metodo getOriginalProjectPath---

    public void setOriginalProjectPath(String originalProjectPath) {
        this.originalProjectPath = originalProjectPath;
    } // ---FIN de metodo setOriginalProjectPath---
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Contenido de ProjectModel ---\n");
        sb.append("  Nombre: ").append(projectName).append("\n");
        sb.append("  Fecha Modificación: ").append(lastModifiedDate).append("\n");
        sb.append("  Imágenes Seleccionadas (").append(getSelectedImages().size()).append("):\n");
        if (getSelectedImages().isEmpty()) {
            sb.append("    (Vacío)\n");
        } else {
            for (String key : getSelectedImages().keySet()) {
                sb.append("    - ").append(key).append("\n");
            }
        }
        sb.append("  Imágenes Descartadas (").append(getDiscardedImages().size()).append("):\n");
        if (getDiscardedImages().isEmpty()) {
            sb.append("    (Vacío)\n");
        } else {
            for (String key : getDiscardedImages()) {
                sb.append("    - ").append(key).append("\n");
            }
        }
        sb.append("---------------------------------");
        return sb.toString();
    } // ---FIN de metodo toString---
    
} // --- FIN de clase ProjectModel ---