package servicios;

/**
 * Interfaz para oyentes que necesitan ser notificados sobre cambios en el estado
 * de guardado de un proyecto.
 */
@FunctionalInterface
public interface ProjectStateListener {
    
    /**
     * Se invoca cuando el estado de "cambios sin guardar" del proyecto cambia.
     *
     * @param hasUnsavedChanges true si el proyecto ahora tiene cambios pendientes,
     *                          false si acaba de ser guardado o reseteado.
     */
    void onProjectStateChanged(boolean hasUnsavedChanges);
    
} // --- FIN de la interfaz ProjectStateListener ---