package servicios.cliente;

import java.util.List;

import modelo.proyecto.ImageCheckboxOverlay;
import modelo.proyecto.ProjectImage;
import modelo.proyecto.ProjectModel;
import modelo.proyecto.SelectionState;

public class ClientSyncService {

    /**
     * Cierra la sesi&oacute;n de cliente sincronizando el estado del cliente
     * con el proyecto. Itera masterImages con enSeleccionProyecto==true:
     * - Si estadoCliente == SELECTED (o alg&uacute;n checkbox interno SELECTED) → se queda
     * - Si estadoCliente == DISCARDED || UNDEFINED (y ning&uacute;n checkbox SELECTED) → enSeleccionProyecto = false
     * Finaliza marcando sharedWithClient = false.
     */
    public void closeAndSync(ProjectModel project) {
        if (project.getMasterImages() == null) return;

        for (ProjectImage pi : project.getMasterImages().values()) {
            if (!pi.isEnSeleccionProyecto()) continue;

            boolean hasSelectedCheckbox = false;
            List<ImageCheckboxOverlay> cbs = pi.getCheckboxes();
            if (cbs != null) {
                for (ImageCheckboxOverlay cb : cbs) {
                    if (cb.getState() == SelectionState.SELECTED) {
                        hasSelectedCheckbox = true;
                        break;
                    }
                }
            }

            boolean keepSelected = hasSelectedCheckbox || pi.getEstadoCliente() == SelectionState.SELECTED;

            if (!keepSelected) {
                pi.setEnSeleccionProyecto(false);
            }
        }

        project.setSharedWithClient(false);
    } // --- Fin de metodo closeAndSync ---


    /**
     * Representa un conflicto entre el estado del proyecto y el estado del cliente
     * para una imagen, incluyendo la resoluci&oacute;n elegida por el usuario.
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
