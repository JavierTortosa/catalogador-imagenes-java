package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.managers.ViewManager;

public class SwitchToVisualizadorAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final ViewManager viewManager;

    public SwitchToVisualizadorAction(String name, ImageIcon icon, ViewManager viewManager) {
        super(name, icon);
        this.viewManager = Objects.requireNonNull(viewManager, "ViewManager no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Volver a la vista del Visualizador de Imágenes");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (viewManager == null) {
            System.err.println("ERROR [SwitchToVisualizadorAction]: ViewManager es nulo.");
            return;
        }
        
        System.out.println("[SwitchToVisualizadorAction] Solicitando cambio a la vista del Visualizador...");
        viewManager.cambiarAVista("VISTA_VISUALIZADOR");
    }
}