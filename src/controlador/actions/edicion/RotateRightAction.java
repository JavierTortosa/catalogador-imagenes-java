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

public class RotateRightAction extends AbstractAction implements ContextSensitiveAction { // <<< CAMBIAR HERENCIA E IMPLEMENTAR

    private static final long serialVersionUID = 1L; // Considera generar uno nuevo

    private IEditionManager editionManagerRef;
    private VisorModel modelRef; // Para updateEnabledState

    // Constructor REFACTORIZADO
    public RotateRightAction(
            IEditionManager editionManager,
            VisorModel model, // Para gestionar su estado enabled
            String name,
            ImageIcon icon) {
        super(name, icon);
        this.editionManagerRef = Objects.requireNonNull(editionManager, "EditionManager no puede ser null en RotateRightAction");
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null en RotateRightAction");

        putValue(Action.SHORT_DESCRIPTION, "Girar la imagen 90 grados a la derecha");
        // Asegúrate que AppActionCommands.CMD_IMAGEN_ROTAR_DER exista y sea la clave correcta
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_IMAGEN_ROTAR_DER); 

        // El estado inicial se establecerá la primera vez que se llame a updateEnabledState
        updateEnabledState(this.modelRef);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (editionManagerRef == null) {
            System.err.println("ERROR CRÍTICO [RotateRightAction]: EditionManager es nulo.");
            return;
        }
        // System.out.println("[RotateRightAction actionPerformed] Comando: " + e.getActionCommand());
        
        // Delegar la acción al EditionManager
        editionManagerRef.aplicarRotarDerecha();
    }

    // Implementación del método de la interfaz ContextSensitiveAction
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