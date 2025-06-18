package controlador.actions.archivo;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import java.util.Objects;

import controlador.VisorController; // <-- DEPENDENCIA

public class RefreshAction extends AbstractAction {

    private static final long serialVersionUID = 2L;
    private final VisorController controllerRef;

    public RefreshAction(String name, ImageIcon icon, VisorController controller) {
        super(name, icon);
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null en RefreshAction");
        putValue(Action.SHORT_DESCRIPTION, "Recarga la lista de archivos y refresca la interfaz de usuario");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // La Action solo notifica al Controller para que haga todo el trabajo.
        controllerRef.ejecutarRefrescoCompleto();
    }
} // --- FIN CLASE RefreshAction ---