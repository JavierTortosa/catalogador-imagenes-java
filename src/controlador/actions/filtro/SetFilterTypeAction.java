package controlador.actions.filtro;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.GeneralController;
import controlador.managers.filter.FilterCriterion.FilterSource;

public class SetFilterTypeAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final GeneralController generalController;
    private final FilterSource filterSource;

    public SetFilterTypeAction(GeneralController controller, FilterSource source, String name, ImageIcon icon, String commandKey) {
        super(name, icon);
        this.generalController = controller;
        this.filterSource = source;
        putValue(Action.ACTION_COMMAND_KEY, commandKey);
        
        // Llamamos a la sincronización inicial al crear la acción.
        sincronizarEstadoConControlador();
    } // --- Fin del constructor SetFilterTypeAction ---

    @Override
    public void actionPerformed(ActionEvent e) {
        // 1. Le decimos al controlador que cambie el estado.
        generalController.solicitarCambioTipoFiltro(this.filterSource);
        
        // 2. Le decimos al controlador que notifique a TODAS las acciones de este tipo.
        generalController.sincronizarAccionesDeTipoFiltro();
    } // --- Fin del método actionPerformed ---

    /**
     * Consulta el estado en el GeneralController y actualiza su propia propiedad SELECTED_KEY.
     */
    public void sincronizarEstadoConControlador() {
        if (generalController != null) {
            boolean deberiaEstarSeleccionado = (generalController.getFiltroActivoSource() == this.filterSource);
            putValue(Action.SELECTED_KEY, deberiaEstarSeleccionado);
        }
    } // --- Fin del método sincronizarEstadoConControlador ---
    
    public FilterSource getFilterSource() {
        return this.filterSource;
    } // --- Fin del método getFilterSource ---

} // --- Fin de la clase SetFilterTypeAction ---