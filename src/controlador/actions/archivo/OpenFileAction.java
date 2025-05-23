package controlador.actions.archivo;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.commands.AppActionCommands;
import controlador.managers.FileOperationsManager; // Nueva dependencia

public class OpenFileAction extends AbstractAction { // Ya no hereda de BaseVisorAction

    private static final long serialVersionUID = 1L;
    private FileOperationsManager fileOperationManager;

    public OpenFileAction(String name, ImageIcon icon, FileOperationsManager fileManager) {
        super(name, icon);
        this.fileOperationManager = Objects.requireNonNull(fileManager, "FileManager no puede ser null en OpenFileAction");

        putValue(Action.SHORT_DESCRIPTION, "Abrir una nueva carpeta de imágenes");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ARCHIVO_ABRIR);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("[OpenFileAction actionPerformed] Comando: " + e.getActionCommand());
        if (this.fileOperationManager != null) {
            this.fileOperationManager.solicitarSeleccionNuevaCarpeta();
        } else {
            System.err.println("ERROR CRÍTICO [OpenFileAction]: FileManager es nulo.");
        }
    }
}

