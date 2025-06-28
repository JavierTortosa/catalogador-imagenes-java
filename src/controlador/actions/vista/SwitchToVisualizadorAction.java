package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.VisorController; // <--- NUEVA DEPENDENCIA
import controlador.commands.AppActionCommands;
import modelo.VisorModel;

public class SwitchToVisualizadorAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final VisorController controllerRef; // <--- CAMPO MODIFICADO

    public SwitchToVisualizadorAction(String name, ImageIcon icon, VisorController controller) { // <--- CONSTRUCTOR MODIFICADO
        super(name, icon);
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Volver a la vista del Visualizador de Imágenes");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR);
    } // --- Fin del método SwitchToVisualizadorAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controllerRef == null) {
            System.err.println("ERROR CRÍTICO [SwitchToVisualizadorAction]: VisorController es nulo.");
            return;
        }
        
        // --- CAMBIO: Llamamos al método genérico para cambiar de modo ---
        System.out.println("[SwitchToVisualizadorAction] Solicitando cambio al modo VISUALIZADOR...");
//        controllerRef.solicitudCambiarModoDeTrabajo(AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR);
        controllerRef.cambiarModoDeTrabajo(VisorModel.WorkMode.VISUALIZADOR);

    } // --- Fin del método actionPerformed ---
    
} // --- FIN de la clase SwitchToVisualizadorAction ---


