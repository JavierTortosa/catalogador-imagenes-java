package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.GeneralController; // <-- CAMBIO DE IMPORT
import controlador.commands.AppActionCommands;
import modelo.VisorModel;

public class SwitchToVisualizadorAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    
    // --- CAMBIO: El campo ahora es de tipo GeneralController ---
    private final GeneralController generalController;

    /**
     * Constructor de SwitchToVisualizadorAction.
     * @param name El nombre de la acción.
     * @param icon El icono de la acción.
     * @param generalController El controlador general de la aplicación.
     */
    public SwitchToVisualizadorAction(String name, ImageIcon icon, GeneralController generalController) { // <-- CAMBIO DE PARÁMETRO
        super(name, icon);
        // --- CAMBIO: Se asigna el GeneralController ---
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Volver a la vista del Visualizador de Imágenes");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR);
    } // --- Fin del método SwitchToVisualizadorAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (generalController == null) {
            System.err.println("ERROR CRÍTICO [SwitchToVisualizadorAction]: GeneralController es nulo.");
            return;
        }
        
        // --- CAMBIO: Llamamos al método que estará en el GeneralController ---
        System.out.println("[SwitchToVisualizadorAction] Solicitando cambio al modo VISUALIZADOR...");
        // La siguiente línea dará un error de compilación hasta que movamos el método
        // a GeneralController. Lo haremos más adelante.
         generalController.cambiarModoDeTrabajo(VisorModel.WorkMode.VISUALIZADOR);

    } // --- Fin del método actionPerformed ---
    
} // --- Fin de la clase SwitchToVisualizadorAction ---