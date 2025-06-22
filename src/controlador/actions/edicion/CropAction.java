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

// No se necesita importar VisorView

public class CropAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;

//    private final VisorModel modelRef;
//    private final VisorController controllerRef; 
    private final IEditionManager editionManager;
    
    public CropAction(IEditionManager editionManager, VisorModel model, String name, ImageIcon icon) {
        super(name, icon);
        this.editionManager = Objects.requireNonNull(editionManager, "IEditionManager no puede ser null en CropAction");

        putValue(Action.SHORT_DESCRIPTION, "Recortar la imagen actual");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_IMAGEN_RECORTAR);

        // El estado inicial se basa en el modelo que se pasa la primera vez.
        updateEnabledState(model); 
    } // --- Fin del método CropAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (this.editionManager != null) {
            // Simplemente delegamos la llamada al manager.
            // El manager se encargará de mostrar el JOptionPane.
            this.editionManager.aplicarRecorte();
        }
    } // --- Fin del método actionPerformed ---

    @Override
    public void updateEnabledState(VisorModel currentModel) {
        if (currentModel != null) {
            setEnabled(currentModel.getCurrentImage() != null);
        } else {
            setEnabled(false);
        }
    }
} // --- FIN de la clase CropAction ---