// En controlador.actions.edicion.CropAction.java
package controlador.actions.edicion;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane; // Para el mensaje de pendiente

import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
// No necesita EditionManager por ahora, ya que solo muestra un mensaje.
// Cuando implementes la funcionalidad, sí lo necesitará.
// import controlador.managers.EditionManager; 
import modelo.VisorModel;
import vista.VisorView; // Para ser el padre del JOptionPane

public class CropAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L; // Considera generar uno nuevo

    // private EditionManager editionManagerRef; // Se añadirá cuando se implemente
    private VisorModel modelRef;
    private VisorView viewRef; // Para el JOptionPane

    public CropAction(
            // EditionManager editionManager, // Se añadirá después
            VisorModel model,
            VisorView view, // Para el JOptionPane
            String name,
            ImageIcon icon) {
        super(name, icon);
        // this.editionManagerRef = editionManager; // Se asignará después
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null en CropAction");
        this.viewRef = Objects.requireNonNull(view, "VisorView no puede ser null en CropAction");


        putValue(Action.SHORT_DESCRIPTION, "Recortar la imagen actual (Funcionalidad Pendiente)");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_IMAGEN_RECORTAR);

        updateEnabledState(this.modelRef); // Estado inicial
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // System.out.println("[CropAction actionPerformed] Comando: " + e.getActionCommand());
        
        // Mostrar mensaje de funcionalidad pendiente
        if (viewRef != null) {
            JOptionPane.showMessageDialog(
                viewRef.getFrame(),
                "La funcionalidad de 'Recortar' aún no está implementada.",
                "Funcionalidad Pendiente",
                JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            System.out.println("Funcionalidad de 'Recortar' pendiente (VisorView no disponible para diálogo).");
        }

        // Cuando implementes la lógica:
        // if (editionManagerRef == null) {
        //     System.err.println("ERROR CRÍTICO [CropAction]: EditionManager es nulo.");
        //     return;
        // }
        // editionManagerRef.iniciarProcesoDeRecorte(); // O como se llame el método
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