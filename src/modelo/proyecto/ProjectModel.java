package modelo.proyecto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    
    // --- Configuración de Exportación Global ---
    private String exportDestinationFolder;
    
    // --- Estado Compartido con el Cliente ---
    private boolean sharedWithClient;
    private long sharedTimestamp;
    private int sharedIteration;
    private Map<String, String> imageCodes;

    // --- Datos del Cliente ---
    private ClientSelection clientSelection;
    
    
    // --- Constructor ---
    public ProjectModel() {
        this.selectedImages = new LinkedHashMap<>();
        this.discardedImages = new ArrayList<>();
        this.exportConfigs = new LinkedHashMap<>();
        this.creationDate = System.currentTimeMillis();
        this.lastModifiedDate = this.creationDate;
        this.sharedWithClient = false;
        this.sharedTimestamp = 0;
        this.sharedIteration = 0;
        this.imageCodes = new LinkedHashMap<>();
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
    
    public String getExportDestinationFolder() {
        return exportDestinationFolder;
    } // ---FIN de metodo getExportDestinationFolder---

    public void setExportDestinationFolder(String exportDestinationFolder) {
        this.exportDestinationFolder = exportDestinationFolder;
    } // ---FIN de metodo setExportDestinationFolder---
    
    public ClientSelection getClientSelection() {
        if (clientSelection == null) {
            clientSelection = new ClientSelection();
        }
        return clientSelection;
    } // ---FIN de metodo getClientSelection---

    public void setClientSelection(ClientSelection clientSelection) {
        this.clientSelection = clientSelection;
    } // ---FIN de metodo setClientSelection---
    
    public boolean hasClientSelection() {
        return clientSelection != null;
    } // ---FIN de metodo hasClientSelection---
    
    public boolean isSharedWithClient() {
        return sharedWithClient;
    } // ---FIN de metodo isSharedWithClient---

    public void setSharedWithClient(boolean sharedWithClient) {
        this.sharedWithClient = sharedWithClient;
    } // ---FIN de metodo setSharedWithClient---

    public long getSharedTimestamp() {
        return sharedTimestamp;
    } // ---FIN de metodo getSharedTimestamp---

    public void setSharedTimestamp(long sharedTimestamp) {
        this.sharedTimestamp = sharedTimestamp;
    } // ---FIN de metodo setSharedTimestamp---

    public int getSharedIteration() {
        return sharedIteration;
    } // ---FIN de metodo getSharedIteration---

    public void setSharedIteration(int sharedIteration) {
        this.sharedIteration = sharedIteration;
    } // ---FIN de metodo setSharedIteration---

    public Map<String, String> getImageCodes() {
        if (imageCodes == null) {
            imageCodes = new LinkedHashMap<>();
        }
        return imageCodes;
    } // ---FIN de metodo getImageCodes---

    public void setImageCodes(Map<String, String> imageCodes) {
        this.imageCodes = imageCodes;
    } // ---FIN de metodo setImageCodes---

    /**
     * Normaliza una ruta o clave de imagen, asegurando que use barras inclinadas hacia adelante.
     * @param path La ruta o clave a normalizar.
     * @return La ruta normalizada.
     */
    public static String normalizarClaveImagen(String path) {
        if (path == null) return null;
        return path.replace("\\", "/");
    }

    /**
     * Obtiene el código de catálogo asociado a una imagen.
     * @param imageKey La clave de la imagen (normalizada o no).
     * @return El código de catálogo, o null si no existe.
     */
    public String getCodigoImagen(String imageKey) {
        if (imageCodes == null || imageKey == null) {
            return null;
        }
        return imageCodes.get(normalizarClaveImagen(imageKey));
    }

    /**
     * Verifica si una clave corresponde a una ruta de imagen (no es una clave compuesta).
     * @param key La clave a verificar.
     * @return true si es una ruta de imagen, false si es una clave compuesta (ej. C001_cb01).
     */
    public boolean esClaveRutaImagen(String key) {
        return key != null && !key.contains("_cb");
    }

    /**
     * Resuelve la ruta canónica de una imagen a partir de su clave (ya sea la ruta o una clave compuesta).
     * @param key La clave de la imagen.
     * @return La ruta normalizada de la imagen, o null si no se pudo resolver.
     */
    public String resolverClaveImagenCanonica(String key) {
        if (key == null) return null;
        if (esClaveRutaImagen(key)) {
            return normalizarClaveImagen(key);
        }
        // Es una clave compuesta (ej. C001_cb01)
        int sep = key.lastIndexOf('_');
        if (sep > 0) {
            String imgCode = key.substring(0, sep);
            for (Map.Entry<String, String> entry : imageCodes.entrySet()) {
                if (imgCode.equals(entry.getValue())) {
                    return normalizarClaveImagen(entry.getKey());
                }
            }
        }
        return null;
    }
    
    
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
    
    /**
     * Contenedor para las selecciones y notas realizadas por el cliente.
     */
    public static class ClientSelection {
        
        private Map<String, SelectionState> images;
        private Map<String, String> comments;
        private Map<String, CommentOverlay> commentOverlays;
        private Map<String, java.util.List<ImageCheckboxOverlay>> imageCheckboxes;
        private String clientNotes;
        private int iterationNumber;
        private String fechaRespuesta;
        
        public ClientSelection() {
            this.images = new LinkedHashMap<>();
            this.comments = new LinkedHashMap<>();
            this.commentOverlays = new LinkedHashMap<>();
            this.imageCheckboxes = new LinkedHashMap<>();
            this.iterationNumber = 1;
        } // ---FIN de metodo ClientSelection---

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClientSelection that = (ClientSelection) o;
            return iterationNumber == that.iterationNumber &&
                    Objects.equals(images, that.images) &&
                    Objects.equals(comments, that.comments) &&
                    Objects.equals(commentOverlays, that.commentOverlays) &&
                    Objects.equals(imageCheckboxes, that.imageCheckboxes) &&
                    Objects.equals(clientNotes, that.clientNotes) &&
                    Objects.equals(fechaRespuesta, that.fechaRespuesta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(images, comments, commentOverlays, imageCheckboxes, clientNotes, iterationNumber, fechaRespuesta);
        }

        public Map<String, SelectionState> getImages() {
            if (images == null) {
                images = new LinkedHashMap<>();
            }
            return images;
        } // ---FIN de metodo getImages---

        public void setImages(Map<String, SelectionState> images) {
            this.images = images;
        } // ---FIN de metodo setImages---

        public Map<String, String> getComments() {
            if (comments == null) {
                comments = new LinkedHashMap<>();
            }
            return comments;
        } // ---FIN de metodo getComments---

        public void setComments(Map<String, String> comments) {
            this.comments = comments;
        } // ---FIN de metodo setComments---

        public Map<String, CommentOverlay> getCommentOverlays() {
            if (commentOverlays == null) {
                commentOverlays = new LinkedHashMap<>();
            }
            return commentOverlays;
        }

        public void setCommentOverlays(Map<String, CommentOverlay> commentOverlays) {
            this.commentOverlays = commentOverlays;
        }

        public CommentOverlay getOrCreateCommentOverlay(String imageKey) {
            return getCommentOverlays().computeIfAbsent(imageKey, k -> new CommentOverlay());
        }

        public String getClientNotes() {
            return clientNotes;
        } // ---FIN de metodo getClientNotes---

        public void setClientNotes(String clientNotes) {
            this.clientNotes = clientNotes;
        } // ---FIN de metodo setClientNotes---

        public int getIterationNumber() {
            return iterationNumber;
        } // ---FIN de metodo getIterationNumber---

        public void setIterationNumber(int iterationNumber) {
            this.iterationNumber = iterationNumber;
        } // ---FIN de metodo setIterationNumber---

        /** Alias de setIterationNumber para uso desde el importador. */
        public void setIteracionNumero(int n) {
            this.iterationNumber = n;
        } // ---FIN de metodo setIteracionNumero---

        public String getFechaRespuesta() {
            return fechaRespuesta;
        } // ---FIN de metodo getFechaRespuesta---

        public void setFechaRespuesta(String fechaRespuesta) {
            this.fechaRespuesta = fechaRespuesta;
        } // ---FIN de metodo setFechaRespuesta---

        public java.util.List<ImageCheckboxOverlay> getImageCheckboxes(String imageKey) {
            if (imageCheckboxes == null) {
                imageCheckboxes = new LinkedHashMap<>();
            }
            String canonical = ProjectModel.normalizarClaveImagen(imageKey);
            if (canonical == null) {
                return new java.util.ArrayList<>();
            }
            if (!imageCheckboxes.containsKey(canonical)) {
                for (String existingKey : new java.util.ArrayList<>(imageCheckboxes.keySet())) {
                    if (ProjectModel.normalizarClaveImagen(existingKey).equals(canonical)) {
                        imageCheckboxes.put(canonical, imageCheckboxes.remove(existingKey));
                        break;
                    }
                }
            }
            return imageCheckboxes.computeIfAbsent(canonical, k -> new java.util.ArrayList<>());
        } // ---FIN de metodo getImageCheckboxes---

        public Map<String, java.util.List<ImageCheckboxOverlay>> getImageCheckboxesMap() {
            if (imageCheckboxes == null) {
                imageCheckboxes = new LinkedHashMap<>();
            }
            return imageCheckboxes;
        } // ---FIN de metodo getImageCheckboxesMap---

        public void setImageCheckboxesMap(Map<String, java.util.List<ImageCheckboxOverlay>> imageCheckboxes) {
            this.imageCheckboxes = imageCheckboxes;
        } // ---FIN de metodo setImageCheckboxesMap---

    } // ---FIN de clase ClientSelection---
    
} // --- FIN de clase ProjectModel ---