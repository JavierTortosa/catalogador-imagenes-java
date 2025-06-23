package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;

public class ToggleMarkImageAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;

    private final VisorController controllerRef;

    public ToggleMarkImageAction(
            VisorController controller,
            String name,
            ImageIcon icon
    ) {
        super(name, icon);
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Marcar o desmarcar la imagen actual para el proyecto activo");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
        putValue(Action.SELECTED_KEY, Boolean.FALSE);
    } // --- Fin del método ToggleMarkImageAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        // La única responsabilidad de la acción es delegar la solicitud al controlador.
        if (controllerRef != null) {
            // Llama a un método PÚBLICO y NUEVO en el controlador que orquestará la operación.
            controllerRef.solicitudAlternarMarcaDeImagenActual();
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
//import java.nio.file.Path;
//import java.util.Objects;
//import javax.swing.AbstractAction;
//import javax.swing.Action;
//import javax.swing.ImageIcon;
//import javax.swing.JCheckBoxMenuItem;
//import javax.swing.JToggleButton;
//
//import controlador.VisorController;
//import controlador.commands.AppActionCommands;
//import controlador.interfaces.ContextSensitiveAction;
//import controlador.managers.interfaces.IProjectManager;
//import modelo.VisorModel;
//
//public class ToggleMarkImageAction extends AbstractAction implements ContextSensitiveAction {
//
//    private static final long serialVersionUID = 1L;
//
//    private final IProjectManager projectManager;
//    private final VisorModel modelRef;
//    private final VisorController controllerRef;
//
//    public ToggleMarkImageAction(
//            IProjectManager projectManager,
//            VisorModel model,
//            VisorController controller,
//            String name,
//            ImageIcon icon
//    ) {
//        super(name, icon);
//        this.projectManager = Objects.requireNonNull(projectManager, "IProjectManager no puede ser null");
//        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null");
//        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null");
//
//        putValue(Action.SHORT_DESCRIPTION, "Marcar o desmarcar la imagen actual para el proyecto activo");
//        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
//        putValue(Action.SELECTED_KEY, Boolean.FALSE);
//    } // --- Fin del método ToggleMarkImageAction (constructor) ---
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        System.out.println("[ToggleMarkImageAction actionPerformed] Comando: " + e.getActionCommand());
//        if (projectManager == null || modelRef == null || controllerRef == null) {
//            System.err.println("ERROR CRÍTICO [ToggleMarkImageAction]: Dependencias nulas.");
//            return;
//        }
//
//        String claveActual = modelRef.getSelectedImageKey();
//        if (claveActual == null || claveActual.isEmpty()) {
//            System.out.println("  -> No hay imagen seleccionada para marcar/desmarcar.");
//            controllerRef.actualizarEstadoVisualBotonMarcarYBarraEstado(false, null);
//            return;
//        }
//
//        Path rutaAbsoluta = modelRef.getRutaCompleta(claveActual);
//        if (rutaAbsoluta == null) {
//            System.err.println("ERROR [ToggleMarkImageAction]: No se pudo obtener ruta absoluta para la clave: " + claveActual);
//            controllerRef.actualizarEstadoVisualBotonMarcarYBarraEstado(false, null);
//            return;
//        }
//
//        boolean marcarImagenAhora = false;
//        if (e.getSource() instanceof JToggleButton) {
//            marcarImagenAhora = ((JToggleButton) e.getSource()).isSelected();
//        } else if (e.getSource() instanceof JCheckBoxMenuItem) {
//            marcarImagenAhora = ((JCheckBoxMenuItem) e.getSource()).isSelected();
//        } else {
//             marcarImagenAhora = !Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
//        }
//
//        putValue(Action.SELECTED_KEY, marcarImagenAhora);
//
//        System.out.println("  -> Intención del usuario: " + (marcarImagenAhora ? "MARCAR" : "DESMARCAR") + " imagen: " + rutaAbsoluta);
//        
//        if (marcarImagenAhora) {
//            projectManager.marcarImagenInterno(rutaAbsoluta);
//        } else {
//            projectManager.desmarcarImagenInterno(rutaAbsoluta);
//        }
//        System.out.println("  -> Imagen '" + rutaAbsoluta.getFileName() + (marcarImagenAhora ? "' MARCADA." : "' DESMARCADA."));
//
//        controllerRef.actualizarEstadoVisualBotonMarcarYBarraEstado(marcarImagenAhora, rutaAbsoluta);
//    } // --- FIN del método actionPerformed ---
//
//    @Override
//    public void updateEnabledState(VisorModel modelo) {
//        if (modelo == null || projectManager == null || controllerRef == null) {
//            this.setEnabled(false);
//            putValue(Action.SELECTED_KEY, Boolean.FALSE);
//            return;
//        }
//
//        String claveSeleccionada = modelo.getSelectedImageKey();
//        if (claveSeleccionada != null && !claveSeleccionada.isEmpty()) {
//            Path rutaImagen = modelo.getRutaCompleta(claveSeleccionada);
//            if (rutaImagen != null) {
//                this.setEnabled(true);
//                boolean estaMarcada = projectManager.estaMarcada(rutaImagen);
//                putValue(Action.SELECTED_KEY, estaMarcada);
//                controllerRef.actualizarEstadoVisualBotonMarcarYBarraEstado(estaMarcada, rutaImagen);
//                return;
//            }
//        }
//
//        this.setEnabled(false);
//        putValue(Action.SELECTED_KEY, Boolean.FALSE);
//        controllerRef.actualizarEstadoVisualBotonMarcarYBarraEstado(false, null);
//    } // --- FIN del método updateEnabledState ---
//
//} // --- FIN de la clase ToggleMarkImageAction ---