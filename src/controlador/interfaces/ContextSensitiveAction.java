package controlador.interfaces; // Paquete para interfaces

import modelo.VisorModel;

public interface ContextSensitiveAction {
    /**
     * Actualiza el estado 'enabled' de la acción basándose en el
     * estado actual del modelo proporcionado.
     * Este método es llamado por un coordinador (como ListCoordinator)
     * cuando el contexto de la aplicación (ej. la imagen seleccionada) cambia.
     *
     * @param model El modelo actual de la aplicación, que la acción puede
     *              consultar para determinar si debe estar habilitada.
     */
    void updateEnabledState(VisorModel model);
}