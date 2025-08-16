package controlador.actions.tree;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import controlador.GeneralController;

public class DrillDownFolderAction extends AbstractAction {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final GeneralController generalController;

    public DrillDownFolderAction(String name, GeneralController generalController) {
        super(name);
        this.generalController = generalController;
    } // --- Fin del método DrillDownFolderAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (generalController != null) {
            generalController.solicitarEntrarEnCarpetaDesdeArbol();
        }
    } // --- Fin del método actionPerformed ---
} // --- FIN DE LA CLASE DrillDownFolderAction ---