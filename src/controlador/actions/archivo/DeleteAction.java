package controlador.actions.archivo;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.commands.AppActionCommands;
import controlador.managers.FileOperationsManager; // Nueva dependencia
import modelo.VisorModel; // Solo para actualizar estado enabled

public class DeleteAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private FileOperationsManager fileOpsManager;
    private VisorModel model; // Para el método actualizarEstadoEnabled

    // Constructor Refactorizado
    public DeleteAction(String name, 
                        ImageIcon icon, 
                        FileOperationsManager fileOpsManager,
                        VisorModel model) { // VisorModel para actualizar su estado enabled
        super(name, icon);
        this.fileOpsManager = Objects.requireNonNull(fileOpsManager, "FileOperationsManager no puede ser null en DeleteAction");
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en DeleteAction para actualizar estado enabled");

        putValue(Action.SHORT_DESCRIPTION, "Eliminar la imagen seleccionada");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_IMAGEN_ELIMINAR);
        actualizarEstadoEnabled(); // Estado inicial basado en si hay selección
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("[DeleteAction actionPerformed] Comando: " + e.getActionCommand());
        if (this.fileOpsManager != null) {
            this.fileOpsManager.borrarArchivoSeleccionado();
        } else {
            System.err.println("ERROR CRÍTICO [DeleteAction]: FileOperationsManager es nulo.");
        }
    }
    
    /**
     * Actualiza el estado 'enabled' de esta Action basado en si hay una imagen seleccionada en el modelo.
     * Este método debería ser llamado cuando cambia la selección de imagen.
     * El ListCoordinator o VisorController podrían ser responsables de llamar a esto.
     */
    public void actualizarEstadoEnabled() {
        if (model != null) {
            setEnabled(model.getSelectedImageKey() != null && model.getRutaCompleta(model.getSelectedImageKey()) != null);
        } else {
            setEnabled(false);
        }
    }
}