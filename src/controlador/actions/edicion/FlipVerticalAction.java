// En controlador.actions.edicion.FlipVerticalAction.java
package controlador.actions.edicion;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import controlador.managers.EditionManager;
import modelo.VisorModel;

public class FlipVerticalAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L; // Considera generar uno nuevo

    private EditionManager editionManagerRef;
    private VisorModel modelRef;

    public FlipVerticalAction(
            EditionManager editionManager,
            VisorModel model,
            String name,
            ImageIcon icon) {
        super(name, icon);
        this.editionManagerRef = Objects.requireNonNull(editionManager, "EditionManager no puede ser null en FlipVerticalAction");
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null en FlipVerticalAction");

        putValue(Action.SHORT_DESCRIPTION, "Voltear la imagen verticalmente");
        // Asegúrate que AppActionCommands.CMD_IMAGEN_VOLTEAR_V exista y sea la clave correcta
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_IMAGEN_VOLTEAR_V); 

        updateEnabledState(this.modelRef); // Estado inicial
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (editionManagerRef == null) {
            System.err.println("ERROR CRÍTICO [FlipVerticalAction]: EditionManager es nulo.");
            return;
        }
        // System.out.println("[FlipVerticalAction actionPerformed] Comando: " + e.getActionCommand());
        
        editionManagerRef.aplicarVolteoVertical();
    }

    @Override
    public void updateEnabledState(VisorModel currentModel) {
        if (currentModel != null) {
            // Habilitar solo si hay una imagen cargada en el modelo
            setEnabled(currentModel.getCurrentImage() != null);
        } else {
            setEnabled(false);
        }
    }
}