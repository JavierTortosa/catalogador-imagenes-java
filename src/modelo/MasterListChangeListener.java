package modelo;

import javax.swing.DefaultListModel;

/**
 * Interfaz para los componentes que necesitan ser notificados
 * cuando la lista de datos maestra en VisorModel cambia fundamentalmente
 * (ej. al cargar una nueva carpeta o aplicar un filtro).
 */
public interface MasterListChangeListener {
    
    /**
     * Se invoca cuando la lista maestra ha sido reemplazada por una nueva.
     * @param newMasterList el nuevo DefaultListModel que es ahora la fuente de la verdad.
     * @param source El objeto que originó el cambio (útil para depuración).
     */
    void onMasterListChanged(DefaultListModel<String> newMasterList, Object source);

} // --- Fin de la interfaz MasterListChangeListener ---