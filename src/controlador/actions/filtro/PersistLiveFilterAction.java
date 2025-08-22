package controlador.actions.filtro;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import controlador.GeneralController;

public class PersistLiveFilterAction extends AbstractAction {
    
    private static final long serialVersionUID = 1L;
    private final GeneralController generalController;

    public PersistLiveFilterAction(GeneralController generalController, String name, ImageIcon icon) {
        super(name, icon);
        this.generalController = generalController;
    } // --- Fin del constructor PersistLiveFilterAction ---

    @Override
    public void actionPerformed(ActionEvent e) {
        generalController.solicitarPersistenciaDeFiltroRapido();
    } // --- Fin del método actionPerformed ---

} // --- Fin de la clase PersistLiveFilterAction ---