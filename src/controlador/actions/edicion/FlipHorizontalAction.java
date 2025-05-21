// En controlador.actions.edicion.FlipHorizontalAction.java
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

public class FlipHorizontalAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L; // Considera generar uno nuevo

    private EditionManager editionManagerRef;
    private VisorModel modelRef;

    public FlipHorizontalAction(
            EditionManager editionManager,
            VisorModel model,
            String name,
            ImageIcon icon) {
        super(name, icon);
        this.editionManagerRef = Objects.requireNonNull(editionManager, "EditionManager no puede ser null");
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Voltear la imagen horizontalmente (efecto espejo)");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_IMAGEN_VOLTEAR_H);

        updateEnabledState(this.modelRef); // Estado inicial
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (editionManagerRef == null) {
            System.err.println("ERROR CRÍTICO [FlipHorizontalAction]: EditionManager es nulo.");
            return;
        }
        editionManagerRef.aplicarVolteoHorizontal();
    }

    @Override
    public void updateEnabledState(VisorModel currentModel) {
        if (currentModel != null) {
            setEnabled(currentModel.getCurrentImage() != null);
        } else {
            setEnabled(false);
        }
    }
}