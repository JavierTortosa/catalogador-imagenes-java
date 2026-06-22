package modelo.proyecto;

/**
 * Estado de selección de una imagen por parte del cliente.
 */
public enum SelectionState {
    UNDEFINED, 
    SELECTED, 
    DISCARDED;

    public TristateState toTristate() {
        return switch(this) {
            case SELECTED -> TristateState.SELECTED;
            case DISCARDED -> TristateState.DESELECTED;
            case UNDEFINED -> TristateState.INDETERMINATE;
        };
    }

} // --- FIN de enum SelectionState ---
