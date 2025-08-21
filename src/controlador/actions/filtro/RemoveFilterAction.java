package controlador.actions.filtro;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import controlador.GeneralController;

/**
 * Acción para eliminar el filtro seleccionado de la lista de filtros activos.
 */
public class RemoveFilterAction extends AbstractAction {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final GeneralController generalController;

    public RemoveFilterAction(GeneralController generalController, String name, ImageIcon icon) {
        super(name, icon);
        this.generalController = generalController;
    } // --- Fin del constructor RemoveFilterAction ---

    @Override
    public void actionPerformed(ActionEvent e) {
        generalController.solicitarEliminarFiltroSeleccionado();
    } // --- Fin del método actionPerformed ---

} // --- Fin de la clase RemoveFilterAction ---