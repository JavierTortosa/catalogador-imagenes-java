package controlador.actions.filtro;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.GeneralController;
import controlador.managers.filter.FilterCriterion;
import controlador.managers.filter.FilterCriterion.FilterSource;
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
    private final FilterType filterType; // POSITIVE o NEGATIVE

    public AddFilterAction(GeneralController generalController, FilterType filterType, String name, ImageIcon icon) {
        super(name, icon);
        this.generalController = generalController;
        this.filterType = filterType;
    } // --- Fin del constructor AddFilterAction ---

    @Override
    public void actionPerformed(ActionEvent e) {
        // En el futuro, leeremos la FilterSource (nombre/carpeta) de un JComboBox.
        // Por ahora, asumimos que todos los filtros son sobre el nombre del archivo.
        FilterSource filterSource = FilterSource.FILENAME;
        
        generalController.solicitarAnadirFiltro(filterSource, filterType);
    } // --- Fin del método actionPerformed ---

} // --- Fin de la clase AddFilterAction ---