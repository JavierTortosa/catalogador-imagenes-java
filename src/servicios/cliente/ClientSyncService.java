package servicios.cliente;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import modelo.proyecto.ProjectModel;
import modelo.proyecto.SelectionState;

/**
 * Servicio de sincronización entre la selección del cliente y el proyecto.
 * Proporciona merge, cómputo de conflictos, resolución, update rápido y cierre.
 */
public class ClientSyncService {

    public SyncReport mergeClientResponse(ProjectModel project, Path respuestaJson) {
        SyncReport report = new SyncReport();
        if (!project.hasClientSelection()) return report;

        Map<String, SelectionState> clientImages = project.getClientSelection().getImages();
        Map<String, String> projectSelected = project.getSelectedImages();
        List<String> projectDiscarded = project.getDiscardedImages();

        for (Map.Entry<String, SelectionState> entry : clientImages.entrySet()) {
            String imageKey = entry.getKey();
            SelectionState clientState = entry.getValue();

            boolean inProjectSelected = projectSelected.containsKey(imageKey);
            boolean inProjectDiscarded = projectDiscarded.contains(imageKey);

            SelectionState projectState = SelectionState.DISCARDED;
            if (inProjectSelected) {
                projectState = SelectionState.SELECTED;
            } else if (inProjectDiscarded) {
                projectState = SelectionState.DISCARDED;
            }

            if (clientState == SelectionState.UNDEFINED) {
                report.incrementUnchanged();
            } else if (clientState != projectState) {
                if (clientState == SelectionState.SELECTED) {
                    report.getAddedToSelection().add(imageKey);
                } else {
                    report.getMovedToDiscard().add(imageKey);
                }
            } else {
                report.incrementUnchanged();
            }
        }
        return report;
    } // --- Fin de metodo mergeClientResponse ---


    /**
     * Genera la lista de conflictos comparando el estado del proyecto con el
     * estado de la selección del cliente. Útil para mostrar en
     * el diálogo de conflicto antes de aplicar.
     */
    public List<ConflictEntry> computeConflicts(ProjectModel project) {
        List<ConflictEntry> conflicts = new ArrayList<>();
        if (!project.hasClientSelection()) return conflicts;

        Map<String, SelectionState> clientImages = project.getClientSelection().getImages();
        Map<String, String> projectSelected = project.getSelectedImages();
        List<String> projectDiscarded = project.getDiscardedImages();

        for (Map.Entry<String, SelectionState> entry : clientImages.entrySet()) {
            String imageKey = entry.getKey();
            SelectionState clientState = entry.getValue();

            SelectionState projectState = SelectionState.DISCARDED;
            if (projectSelected.containsKey(imageKey)) {
                projectState = SelectionState.SELECTED;
            } else if (projectDiscarded.contains(imageKey)) {
                projectState = SelectionState.DISCARDED;
            }

            if (clientState != SelectionState.UNDEFINED && clientState != projectState) {
                conflicts.add(new ConflictEntry(imageKey, projectState, clientState));
            }
        }
        return conflicts;
    } // --- Fin de metodo computeConflicts ---


    /**
     * Resuelve los conflictos aplicando la decisión del usuario.
     * @param project Proyecto actual.
     * @param resolved Lista de entradas ya resueltas (projectState actualizado según la decisión).
     */
    public void applyResolvedConflicts(ProjectModel project, List<ConflictEntry> resolved) {
        Map<String, String> projectSelected = project.getSelectedImages();
        List<String> projectDiscarded = project.getDiscardedImages();

        for (ConflictEntry entry : resolved) {
            String key = entry.imageKey;
            SelectionState finalState = entry.resolvedState != null ? entry.resolvedState : entry.projectState;

            projectSelected.remove(key);
            projectDiscarded.remove(key);

            if (finalState == SelectionState.SELECTED) {
                projectSelected.put(key, "");
            } else if (finalState == SelectionState.DISCARDED) {
                if (!projectDiscarded.contains(key)) {
                    projectDiscarded.add(key);
                }
            }
        }
    } // --- Fin de metodo applyResolvedConflicts ---


    /**
     * Sincronización rápida: aplica el estado del cliente al proyecto
     * sin diálogo de conflictos.
     */
    public void update(ProjectModel project) {
        if (!project.hasClientSelection()) return;

        Map<String, SelectionState> clientImages = project.getClientSelection().getImages();
        Map<String, String> projectSelected = project.getSelectedImages();
        List<String> projectDiscarded = project.getDiscardedImages();

        for (Map.Entry<String, SelectionState> entry : clientImages.entrySet()) {
            String imageKey = entry.getKey();
            SelectionState state = entry.getValue();

            projectSelected.remove(imageKey);
            projectDiscarded.remove(imageKey);

            if (state == SelectionState.SELECTED) {
                projectSelected.put(imageKey, "");
            } else if (state == SelectionState.DISCARDED) {
                if (!projectDiscarded.contains(imageKey)) {
                    projectDiscarded.add(imageKey);
                }
            }
        }
    } // --- Fin de metodo update ---


    public void closeAndSync(ProjectModel project, Path prjclPath) {
        if (!project.hasClientSelection()) return;

        Map<String, SelectionState> clientImages = project.getClientSelection().getImages();
        Map<String, String> projectSelected = project.getSelectedImages();
        List<String> projectDiscarded = project.getDiscardedImages();

        for (Map.Entry<String, SelectionState> entry : clientImages.entrySet()) {
            String imageKey = entry.getKey();
            SelectionState clientState = entry.getValue();

            if (clientState == SelectionState.SELECTED) {
                if (!projectSelected.containsKey(imageKey)) {
                    projectSelected.put(imageKey, null);
                    projectDiscarded.remove(imageKey);
                }
            } else if (clientState == SelectionState.DISCARDED) {
                if (!projectDiscarded.contains(imageKey)) {
                    projectDiscarded.add(imageKey);
                    projectSelected.remove(imageKey);
                }
            }
        }

        project.setClientSelection(null);
    } // --- Fin de metodo closeAndSync ---


    /**
     * Informe de resultados de una operación de sincronización.
     */
    public static class SyncReport {
        private List<String> conflictedItems = new ArrayList<>();
        private List<String> addedToSelection = new ArrayList<>();
        private List<String> movedToDiscard = new ArrayList<>();
        private int unchangedCount = 0;

        public List<String> getConflictedItems() {
            return conflictedItems;
        } // --- Fin de metodo getConflictedItems ---

        public List<String> getAddedToSelection() {
            return addedToSelection;
        } // --- Fin de metodo getAddedToSelection ---

        public List<String> getMovedToDiscard() {
            return movedToDiscard;
        } // --- Fin de metodo getMovedToDiscard ---

        public int getUnchangedCount() {
            return unchangedCount;
        } // --- Fin de metodo getUnchangedCount ---

        public void incrementUnchanged() {
            unchangedCount++;
        } // --- Fin de metodo incrementUnchanged ---

    } // --- Fin de clase SyncReport ---


    /**
     * Representa un conflicto entre el estado del proyecto y el estado del cliente
     * para una imagen, incluyendo la resolución elegida por el usuario.
     */
    public static class ConflictEntry {
        private final String imageKey;
        private final SelectionState projectState;
        private final SelectionState clientState;
        private SelectionState resolvedState;

        public ConflictEntry(String imageKey, SelectionState projectState, SelectionState clientState) {
            this.imageKey = imageKey;
            this.projectState = projectState;
            this.clientState = clientState;
        } // --- Fin de metodo ConflictEntry (constructor) ---

        public String getImageKey() {
            return imageKey;
        } // --- Fin de metodo getImageKey ---

        public SelectionState getProjectState() {
            return projectState;
        } // --- Fin de metodo getProjectState ---

        public SelectionState getClientState() {
            return clientState;
        } // --- Fin de metodo getClientState ---

        public SelectionState getResolvedState() {
            return resolvedState;
        } // --- Fin de metodo getResolvedState ---

        public void setResolvedState(SelectionState resolvedState) {
            this.resolvedState = resolvedState;
        } // --- Fin de metodo setResolvedState ---

    } // --- Fin de clase ConflictEntry ---

} // --- Fin de clase ClientSyncService ---
