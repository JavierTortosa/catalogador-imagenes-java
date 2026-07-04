package controlador.actions.filtro;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import controlador.GeneralController;
import controlador.managers.filter.FilterCriterion.FilterType;

/**
 * Acción para añadir un nuevo criterio de filtro al FilterManager.
 */
public class AddFilterAction extends AbstractAction {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final GeneralController generalController;

    public AddFilterAction(GeneralController generalController, FilterType filterType, String name, ImageIcon icon) {
        super(name, icon);
        this.generalController = generalController;
    } // --- Fin del constructor AddFilterAction ---

    @Override
    public void actionPerformed(ActionEvent e) {
        generalController.solicitarAnadirFiltro();
        
    } // --- Fin del método actionPerformed ---

} // --- Fin de la clase AddFilterAction ---