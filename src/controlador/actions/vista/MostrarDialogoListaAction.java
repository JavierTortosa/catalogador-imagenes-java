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

public class MostrarDialogoListaAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private final IViewManager viewManagerRef;
    
    public MostrarDialogoListaAction(
            String name,
            ImageIcon icon,
            VisorModel model,
            VisorController controller,
            IViewManager viewManager) {
    	
        super(name, icon);
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