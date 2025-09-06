package controlador.actions.archivo;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import java.util.Objects;

import controlador.GeneralController; // <-- DEPENDENCIA CAMBIADA
import controlador.commands.AppActionCommands;

public class RefreshAction extends AbstractAction {

    private static final long serialVersionUID = 2L;
    private final GeneralController generalController; // <-- DEPENDENCIA CAMBIADA

    public RefreshAction(String name, ImageIcon icon, GeneralController controller) { // <-- CONSTRUCTOR CAMBIADO
        super(name, icon);
        this.generalController = Objects.requireNonNull(controller, "GeneralController no puede ser null en RefreshAction");
        putValue(Action.SHORT_DESCRIPTION, "Recarga la lista de archivos y refresca la interfaz de usuario");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ESPECIAL_REFRESCAR); // Asignamos el comando
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // La Action solo notifica al GeneralController para que él decida qué hacer.
        generalController.solicitarRefrescoDelModoActivo();
    }
}


//package controlador.actions.archivo;
//
//import java.awt.event.ActionEvent;
//import javax.swing.AbstractAction;
//import javax.swing.Action;
//import javax.swing.ImageIcon;
//import java.util.Objects;
//
//import controlador.VisorController; // <-- DEPENDENCIA
//
//public class RefreshAction extends AbstractAction {
//
//    private static final long serialVersionUID = 2L;
//    private final VisorController controllerRef;
//
//    public RefreshAction(String name, ImageIcon icon, VisorController controller) {
//        super(name, icon);
//        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null en RefreshAction");
//        putValue(Action.SHORT_DESCRIPTION, "Recarga la lista de archivos y refresca la interfaz de usuario");
//    }
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        // La Action solo notifica al Controller para que haga todo el trabajo.
//        controllerRef.ejecutarRefrescoCompleto();
//    }
//} // --- FIN CLASE RefreshAction ---