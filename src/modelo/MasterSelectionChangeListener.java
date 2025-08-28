package modelo;

public interface MasterSelectionChangeListener {
    
    /**
     * Se invoca cuando el índice de selección maestro ha cambiado.
     * @param newMasterIndex El nuevo índice seleccionado en la lista maestra.
     * @param source El objeto que originó el cambio.
     */
    void onMasterSelectionChanged(int newMasterIndex, Object source);
    
} // end of interface