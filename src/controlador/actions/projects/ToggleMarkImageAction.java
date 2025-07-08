package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.GeneralController;
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;

public class ToggleMarkImageAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;

    private final GeneralController generalControllerRef;

    public ToggleMarkImageAction(
            GeneralController controller,
            String name,
            ImageIcon icon
    ) {
        super(name, icon);
        this.generalControllerRef = Objects.requireNonNull(controller, "GeneralController no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Marcar o desmarcar la imagen actual para el proyecto activo");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
        putValue(Action.SELECTED_KEY, Boolean.FALSE);
    } // --- Fin del método ToggleMarkImageAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (generalControllerRef != null) {
            generalControllerRef.solicitudAlternarMarcaImagenActual(); 
        }
    } // --- FIN del método actionPerformed ---

    @Override
    public void updateEnabledState(VisorModel modelo) {
        if (modelo == null) {
            this.setEnabled(false);
            return;
        }

        boolean isEnabled = modelo.getSelectedImageKey() != null && !modelo.getSelectedImageKey().isEmpty();
        this.setEnabled(isEnabled);
        
        if (!isEnabled) {
            putValue(Action.SELECTED_KEY, Boolean.FALSE);
        }
    } // --- FIN del método updateEnabledState ---

} // --- FIN de la clase ToggleMarkImageAction ---


//package controlador.actions.projects;
//
//import java.awt.event.ActionEvent;
//import java.util.Objects;
//import javax.swing.AbstractAction;
//import javax.swing.Action;
//import javax.swing.ImageIcon;
//
//import controlador.VisorController;
//import controlador.commands.AppActionCommands;
//import controlador.interfaces.ContextSensitiveAction;
//import modelo.VisorModel;
//
//public class ToggleMarkImageAction extends AbstractAction implements ContextSensitiveAction {
//
//    private static final long serialVersionUID = 1L;
//
//    private final VisorController controllerRef;
//
//    public ToggleMarkImageAction(
//            VisorController controller,
//            String name,
//            ImageIcon icon
//    ) {
//        super(name, icon);
//        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null");
//
//        putValue(Action.SHORT_DESCRIPTION, "Marcar o desmarcar la imagen actual para el proyecto activo");
//        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
//        putValue(Action.SELECTED_KEY, Boolean.FALSE);
//    } // --- Fin del método ToggleMarkImageAction (constructor) ---
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        // La única responsabilidad de la acción es delegar la solicitud al controlador.
//        if (controllerRef != null) {
//            // Llama a un método PÚBLICO y NUEVO en el controlador que orquestará la operación.
//            controllerRef.solicitudAlternarMarcaDeImagenActual();
//        }
//    } // --- FIN del método actionPerformed ---
//
//    @Override
//    public void updateEnabledState(VisorModel modelo) {
//        if (modelo == null) {
//            this.setEnabled(false);
//            return;
//        }
//
//        boolean isEnabled = modelo.getSelectedImageKey() != null && !modelo.getSelectedImageKey().isEmpty();
//        this.setEnabled(isEnabled);
//        
//        if (!isEnabled) {
//            putValue(Action.SELECTED_KEY, Boolean.FALSE);
//        }
//    } // --- FIN del método updateEnabledState ---
//
//} // --- FIN de la clase ToggleMarkImageAction ---
//
