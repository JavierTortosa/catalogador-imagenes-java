package controlador.actions.orden; 

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import controlador.GeneralController;
import modelo.VisorModel;

public class CycleSortAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    
    private final VisorModel model;
    private final GeneralController generalController;

    /**
     * Constructor de la acción para ciclar el modo de ordenación.
     * 
     * @param displayName El nombre/texto para la acción (ej. "Ordenar Lista").
     * @param icon El icono inicial para la acción.
     * @param model El modelo principal de la aplicación.
     * @param generalController El controlador general que orquestará la reordenación.
     */
    public CycleSortAction(String displayName, ImageIcon icon, VisorModel model, GeneralController generalController) {
        super(displayName, icon);
        
        if (model == null || generalController == null) {
            throw new IllegalArgumentException("El modelo y el controlador general no pueden ser nulos");
        }
        
        this.model = model;
        this.generalController = generalController;
        
        // Establecer el tooltip inicial
        putValue(SHORT_DESCRIPTION, "Orden: Por defecto");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // 1. Obtener el estado de ordenación actual desde el modelo.
        VisorModel.SortDirection currentState = model.getSortDirection();
        
        // 2. Determinar cuál será el siguiente estado en el ciclo.
        VisorModel.SortDirection nextState;
        switch (currentState) {
            case NONE:
                nextState = VisorModel.SortDirection.ASCENDING;
                break;
            case ASCENDING:
                nextState = VisorModel.SortDirection.DESCENDING;
                break;
            case DESCENDING:
            default: // Si es DESCENDING o un estado inesperado, vuelve a NONE.
                nextState = VisorModel.SortDirection.NONE;
                break;
        }
        
        // 3. Actualizar el estado en la fuente de la verdad (el modelo).
        model.setSortDirection(nextState);
        
        // 4. Delegar la tarea compleja de reordenar la lista y actualizar
        //    la UI del botón al GeneralController.
        generalController.resortFileListAndSyncButton();
    }
}