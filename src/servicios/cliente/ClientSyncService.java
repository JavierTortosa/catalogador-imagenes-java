package servicios.cliente;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import modelo.proyecto.ProjectModel;
import modelo.proyecto.SelectionState;

/**
 * Servicio encargado de la sincronización y resolución de conflictos 
 * entre el estado del proyecto actual y el estado propuesto por el cliente.
 */
public class ClientSyncService {

    /**
     * Compara el proyecto actual con las respuestas del cliente en JSON (simulado por ahora usando el objeto ClientSelection cargado).
     * @param project El modelo del proyecto actual.
     * @param respuestaJson Ruta al JSON con la respuesta (para futuras iteraciones de carga directa, si procede).
     * @return El reporte de sincronización.
     */
    public SyncReport mergeClientResponse(ProjectModel project, Path respuestaJson) {
        SyncReport report = new SyncReport();
        
        if (!project.hasClientSelection()) {
            return report;
        }

        Map<String, SelectionState> clientImages = project.getClientSelection().getImages();
        Map<String, String> projectSelected = project.getSelectedImages();
        List<String> projectDiscarded = project.getDiscardedImages();

        for (Map.Entry<String, SelectionState> entry : clientImages.entrySet()) {
            String imageKey = entry.getKey();
            SelectionState clientState = entry.getValue();

            boolean inProjectSelected = projectSelected.containsKey(imageKey);
            boolean inProjectDiscarded = projectDiscarded.contains(imageKey);

            // Determinar estado actual en el proyecto
            SelectionState projectState = SelectionState.UNDEFINED;
            if (inProjectSelected) {
                projectState = SelectionState.SELECTED;
            } else if (inProjectDiscarded) {
                projectState = SelectionState.DISCARDED;
            }

            // Comparar y añadir al reporte
            if (clientState != projectState) {
                if (clientState == SelectionState.SELECTED) {
                    report.getAddedToSelection().add(imageKey);
                } else if (clientState == SelectionState.DISCARDED) {
                    report.getMovedToDiscard().add(imageKey);
                } else if (clientState == SelectionState.UNDEFINED) {
                    report.getConflictedItems().add(imageKey); // o "sin resolver"
                }
            } else {
                report.incrementUnchanged();
            }
        }
        return report;
    } // --- FIN del metodo mergeClientResponse ---


    /**
     * Aplica el merge definitivo al .prj.
     */
    public void closeAndSync(ProjectModel project, Path prjclPath) {
        if (!project.hasClientSelection()) {
            return;
        }

        Map<String, SelectionState> clientImages = project.getClientSelection().getImages();
        Map<String, String> projectSelected = project.getSelectedImages();
        List<String> projectDiscarded = project.getDiscardedImages();

        for (Map.Entry<String, SelectionState> entry : clientImages.entrySet()) {
            String imageKey = entry.getKey();
            SelectionState clientState = entry.getValue();

            if (clientState == SelectionState.SELECTED) {
                if (!projectSelected.containsKey(imageKey)) {
                    projectSelected.put(imageKey, null); // Añadimos a selección
                    projectDiscarded.remove(imageKey);
                }
            } else if (clientState == SelectionState.DISCARDED) {
                if (!projectDiscarded.contains(imageKey)) {
                    projectDiscarded.add(imageKey);
                    projectSelected.remove(imageKey);
                }
            }
        }
        
        // Limpiamos la selección de cliente tras sincronizar
        project.setClientSelection(null);
        
    } // --- FIN del metodo closeAndSync ---


    /**
     * Clase anidada para resumir los conflictos y cambios.
     */
    public static class SyncReport {
        private List<String> conflictedItems = new ArrayList<>();
        private List<String> addedToSelection = new ArrayList<>();
        private List<String> movedToDiscard = new ArrayList<>();
        private int unchangedCount = 0;

        /** Retorna la lista de elementos en conflicto. */
        public List<String> getConflictedItems() { return conflictedItems; } // --- Fin del metodo getConflictedItems ---

        /** Retorna la lista de elementos añadidos a selección. */
        public List<String> getAddedToSelection() { return addedToSelection; } // --- Fin del metodo getAddedToSelection ---

        /** Retorna la lista de elementos movidos a descartes. */
        public List<String> getMovedToDiscard() { return movedToDiscard; } // --- Fin del metodo getMovedToDiscard ---

        /** Retorna el número de elementos sin cambios. */
        public int getUnchangedCount() { return unchangedCount; } // --- Fin del metodo getUnchangedCount ---

        /** Incrementa el contador de elementos sin cambios. */
        public void incrementUnchanged() { unchangedCount++; } // --- Fin del metodo incrementUnchanged ---

    } // --- FIN de clase SyncReport ---


} // --- FIN de clase ClientSyncService ---
