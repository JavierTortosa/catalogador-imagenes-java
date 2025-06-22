// En controlador.actions.edicion.RotateLeftAction.java
package controlador.actions.edicion;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import controlador.managers.interfaces.IEditionManager;
import modelo.VisorModel;

public class RotateLeftAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L; // Considera generar uno nuevo

    private IEditionManager editionManagerRef;
    private VisorModel modelRef;

    public RotateLeftAction(
            IEditionManager editionManager,
            VisorModel model,
            String name,
            ImageIcon icon) {
        super(name, icon);
        this.editionManagerRef = Objects.requireNonNull(editionManager, "EditionManager no puede ser null en RotateLeftAction");
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null en RotateLeftAction");

        putValue(Action.SHORT_DESCRIPTION, "Girar la imagen 90 grados a la izquierda");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_IMAGEN_ROTAR_IZQ);

        updateEnabledState(this.modelRef); // Estado inicial
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (editionManagerRef == null) {
            System.err.println("ERROR CRÍTICO [RotateLeftAction]: EditionManager es nulo.");
            return;
        }
        // System.out.println("[RotateLeftAction actionPerformed] Comando: " + e.getActionCommand());
        
        editionManagerRef.aplicarRotarIzquierda();
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