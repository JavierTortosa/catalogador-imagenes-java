package controlador;

import controlador.managers.interfaces.IListCoordinator;
import modelo.MasterSelectionChangeListener;

/**
 * Clase base abstracta para implementaciones de IListCoordinator.
 * Proporciona implementaciones por defecto o vacías para métodos que no
 * son relevantes para todos los coordinadores, como el sistema de listeners
 * de selección maestra, que solo es usado por el ListCoordinator principal.
 */
public abstract class AbstractListCoordinator implements IListCoordinator {

    // Proporcionamos una implementación por defecto (vacía) para el nuevo método.
    // Las clases hijas que no necesiten notificar cambios de selección (como ProjectListCoordinator)
    // no tendrán que hacer nada. ListCoordinator sobreescribirá este comportamiento.
    @Override
    public void addMasterSelectionChangeListener(MasterSelectionChangeListener listener) {
        // Implementación vacía por defecto.
    } // end of method

    // También podemos mover aquí otros métodos que podrían tener implementaciones por defecto.
    // Por ahora, solo necesitamos este.

} // end of class