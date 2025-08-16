package controlador.actions.tree;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import controlador.GeneralController;

public class OpenFolderAction extends AbstractAction {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final GeneralController generalController;

    public OpenFolderAction(String name, GeneralController generalController) {
        super(name);
        this.generalController = generalController;
    } // --- Fin del método OpenFolderAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (generalController != null) {
            generalController.solicitarAbrirCarpetaDesdeArbol();
        }
    } // --- Fin del método actionPerformed ---
} // --- FIN DE LA CLASE OpenFolderAction ---