package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import controlador.commands.AppActionCommands;
import controlador.managers.interfaces.IViewManager;
import modelo.VisorModel;
// No se necesita importar VisorView

public class MostrarDialogoListaAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private final VisorModel modelRef;
    private final VisorController controllerRef;
    private final IViewManager viewManagerRef;
    
    public MostrarDialogoListaAction(
            String name,
            ImageIcon icon,
            VisorModel model,
            VisorController controller,
            IViewManager viewManager) {
    	
        super(name, icon);
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null");
        this.viewManagerRef = Objects.requireNonNull(viewManager, "ViewManager no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Mostrar un diálogo con la lista de imágenes cargadas");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_VISTA_MOSTRAR_DIALOGO_LISTA);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (viewManagerRef != null) {
            viewManagerRef.mostrarDialogoListaImagenes();
        } else {
            // Log de error si la referencia es nula por alguna razón
            System.err.println("ERROR CRÍTICO [MostrarDialogoListaAction]: ViewManager es nulo.");
        }
    } // --- FIN de metodo actionPerformed ---


} // --- FIN de la clase MostrarDialogoListaAction ---