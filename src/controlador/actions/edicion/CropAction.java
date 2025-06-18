// Archivo: controlador/actions/edicion/CropAction.java

package controlador.actions.edicion;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import controlador.VisorController; // Importar VisorController
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;
// No se necesita importar VisorView

public class CropAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;

    private final VisorModel modelRef;
    private final VisorController controllerRef; // Referencia al controlador

    public CropAction(
            VisorModel model,
            VisorController controller, // Recibe el controlador
            String name,
            ImageIcon icon) {
        super(name, icon);
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null en CropAction");
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null en CropAction");

        putValue(Action.SHORT_DESCRIPTION, "Recortar la imagen actual (Funcionalidad Pendiente)");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_IMAGEN_RECORTAR);

        updateEnabledState(this.modelRef);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Obtenemos el frame principal directamente desde el controlador.
        JFrame mainFrame = (controllerRef != null) ? controllerRef.getView() : null;

        JOptionPane.showMessageDialog(
            mainFrame, // Usamos el frame obtenido
            "La funcionalidad de 'Recortar' aún no está implementada.",
            "Funcionalidad Pendiente",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    @Override
    public void updateEnabledState(VisorModel currentModel) {
        if (currentModel != null) {
            setEnabled(currentModel.getCurrentImage() != null);
        } else {
            setEnabled(false);
        }
    }
} // --- FIN de la clase CropAction ---