package controlador.actions.filtro;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import controlador.GeneralController;

/**
 * Acción para limpiar todos los filtros activos.
 */
public class ClearAllFiltersAction extends AbstractAction {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final GeneralController generalController;

    public ClearAllFiltersAction(GeneralController generalController, String name, ImageIcon icon) {
        super(name, icon);
        this.generalController = generalController;
    } // --- Fin del constructor ClearAllFiltersAction ---

    @Override
    public void actionPerformed(ActionEvent e) {
        generalController.solicitarLimpiarTodosLosFiltros();
    } // --- Fin del método actionPerformed ---

} // --- Fin de la clase ClearAllFiltersAction ---