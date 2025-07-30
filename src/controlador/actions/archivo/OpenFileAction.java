package controlador.actions.archivo;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import controlador.GeneralController;
import controlador.commands.AppActionCommands;
import controlador.managers.FileOperationsManager; // Nueva dependencia
import modelo.VisorModel;

public class OpenFileAction extends AbstractAction { // Ya no hereda de BaseVisorAction

    private static final long serialVersionUID = 1L;
    private FileOperationsManager fileOperationManager;
    private VisorModel model;
    private GeneralController generalController;
    
    public OpenFileAction(String name, ImageIcon icon, FileOperationsManager fileManager, VisorModel model, GeneralController generalController) {
        super(name, icon);
        this.fileOperationManager = Objects.requireNonNull(fileManager, "FileManager no puede ser null");
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Abrir una nueva carpeta de imágenes");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ARCHIVO_ABRIR);
    } // --- Fin del constructor ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (model.isSyncVisualizadorCarrusel()) {
            String mensaje = "<html>La <b>Sincronización</b> está activa.<br>"
                           + "Abrir una nueva carpeta desactivará la sincronización para evitar conflictos.<br><br>"
                           + "¿Desea continuar?</html>";
            int respuesta = JOptionPane.showConfirmDialog(null, mensaje, "Desactivar Sincronización", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (respuesta != JOptionPane.YES_OPTION) {
                return;
            }
            model.setSyncVisualizadorCarrusel(false);
            // ¡Llamamos al notificador para que toda la UI se entere del cambio!
            generalController.notificarAccionesSensiblesAlContexto();
        }

        if (this.fileOperationManager != null) {
            this.fileOperationManager.solicitarSeleccionNuevaCarpeta();
        } else {
            System.err.println("ERROR CRÍTICO [OpenFileAction]: FileManager es nulo.");
        }
    } // --- Fin del método actionPerformed ---
    
} // --- Fin de la clase OpenFileAction ---

