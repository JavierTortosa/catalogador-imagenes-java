package controlador.actions.archivo;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.GeneralController;
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import controlador.managers.FileOperationsManager;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;

/**
 * Acción para abrir una nueva carpeta de imágenes.
 */
public class OpenFileAction extends AbstractAction implements ContextSensitiveAction {

    private static final Logger logger = LoggerFactory.getLogger(OpenFileAction.class);
    
    private static final long serialVersionUID = 2L;
    private FileOperationsManager fileOperationManager;
    private VisorModel model;
    private GeneralController generalController;


    /**
     * Constructor para OpenFileAction.
     *
     * @param name              El nombre de la acción.
     * @param icon              El icono de la acción.
     * @param fileManager       El gestor de operaciones de archivos.
     * @param model             El modelo del visor.
     * @param generalController El controlador general.
     */
    public OpenFileAction(String name, ImageIcon icon, FileOperationsManager fileManager, VisorModel model, GeneralController generalController) {
        super(name, icon);
        this.fileOperationManager = Objects.requireNonNull(fileManager, "FileManager no puede ser null");
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null");
        putValue(Action.SHORT_DESCRIPTION, "Abrir una nueva carpeta de imágenes");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ARCHIVO_ABRIR);
    } // --- Fin del método OpenFileAction ---


    /**
     * Ejecuta la acción de abrir una nueva carpeta de imágenes.
     *
     * @param e El evento de acción.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (model.getCurrentWorkMode() == WorkMode.DATOS) return;

        if (model.getCurrentWorkMode() == WorkMode.PROYECTO) {
            generalController.handleOpenProject();
            return;
        }

        if (model.isSyncVisualizadorCarrusel()) {
            String mensaje = "<html>La <b>Sincronización</b> está activa.<br>"
                           + "Abrir una nueva carpeta desactivará la sincronización para evitar conflictos.<br><br>"
                           + "¿Desea continuar?</html>";
            int respuesta = JOptionPane.showConfirmDialog(null, mensaje, "Desactivar Sincronización", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (respuesta != JOptionPane.YES_OPTION) return;
            model.setSyncVisualizadorCarrusel(false);
            generalController.notificarAccionesSensiblesAlContexto();
        }

        if (this.fileOperationManager != null) {
            this.fileOperationManager.solicitarSeleccionNuevaCarpeta();
        } else {
            logger.error("ERROR CRÍTICO [OpenFileAction]: FileManager es nulo.");
        }
    } // --- Fin del método actionPerformed ---


    /**
     * Actualiza el estado de habilitación de la acción basándose en el modo de trabajo.
     *
     * @param model El modelo del visor.
     */
    @Override
    public void updateEnabledState(VisorModel model) {
        setEnabled(model.getCurrentWorkMode() != WorkMode.DATOS);
    } // --- Fin del método updateEnabledState ---

} // --- Fin de la clase OpenFileAction ---
