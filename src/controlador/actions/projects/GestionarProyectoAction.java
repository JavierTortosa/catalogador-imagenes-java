package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.GeneralController; // <-- CAMBIO DE IMPORT
import controlador.commands.AppActionCommands;
import modelo.VisorModel;

public class GestionarProyectoAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    
    // --- CAMBIO: La dependencia ahora es GeneralController ---
    private final GeneralController generalController; 

    /**
     * Constructor de GestionarProyectoAction.
     * @param generalController El controlador general que gestiona los modos.
     * @param name El nombre de la acción.
     * @param icon El icono de la acción.
     */
    public GestionarProyectoAction(
    		GeneralController generalController, // <-- CAMBIO DE PARÁMETRO
            String name,
            ImageIcon icon) 
    {
        super(name, icon);
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Ver/Gestionar la selección de imágenes del proyecto");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_GESTIONAR);
    } // --- Fin del método GestionarProyectoAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (generalController == null) {
            System.err.println("ERROR CRÍTICO [GestionarProyectoAction]: GeneralController es nulo.");
            return;
        }
        
        // --- CAMBIO: Llama directamente al método en GeneralController ---
        System.out.println("[GestionarProyectoAction] Solicitando cambio al modo PROYECTO...");
        generalController.cambiarModoDeTrabajo(VisorModel.WorkMode.PROYECTO);

    } // --- Fin del método actionPerformed ---

} // --- Fin de la clase GestionarProyectoAction ---