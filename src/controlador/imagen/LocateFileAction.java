// Archivo: controlador/imagen/LocateFileAction.java

package controlador.imagen;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import controlador.VisorController; // <-- NUEVO IMPORT
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;
// No se necesita importar VisorView

public class LocateFileAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;

    private final VisorModel modelRef;
    private final VisorController controllerRef; // <-- CAMBIO

    public LocateFileAction(VisorModel model, 
                            VisorController controller, // <-- CAMBIO
                            String name, 
                            ImageIcon icon) {
        super(name, icon);
        this.modelRef = Objects.requireNonNull(model);
        this.controllerRef = Objects.requireNonNull(controller); // <-- CAMBIO

        putValue(Action.SHORT_DESCRIPTION, "Abre la carpeta que contiene la imagen actual en el explorador de archivos");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_IMAGEN_LOCALIZAR);

        updateEnabledState(this.modelRef);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (modelRef == null || controllerRef == null) {
            System.err.println("ERROR [LocateFileAction]: Model o Controller nulos.");
            return;
        }
        
        JFrame mainFrame = controllerRef.getView(); // Obtenemos el frame una sola vez

        String selectedKey = modelRef.getSelectedImageKey();
        if (selectedKey == null || selectedKey.isEmpty()) {
            return;
        }

        Path filePath = modelRef.getRutaCompleta(selectedKey);
        if (filePath == null || !Files.exists(filePath)) {
             JOptionPane.showMessageDialog(mainFrame, "No se pudo encontrar la ruta del archivo seleccionado.", "Error al Localizar", JOptionPane.ERROR_MESSAGE);
             return;
        }

        String osName = System.getProperty("os.name").toLowerCase();

        try {
            if (osName.contains("win")) {
                String command = "explorer.exe /select,\"" + filePath.toAbsolutePath().toString() + "\"";
                Runtime.getRuntime().exec(command);
            } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Path directoryPath = filePath.getParent();
                if (directoryPath != null && Files.isDirectory(directoryPath)) {
                    Desktop.getDesktop().open(directoryPath.toFile());
                } else {
                    throw new IOException("No se pudo obtener el directorio padre.");
                }
            } else {
                JOptionPane.showMessageDialog(mainFrame, "La interacción con el explorador de archivos no es compatible.", "Funcionalidad no Soportada", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(mainFrame, "Error al intentar interactuar con el explorador de archivos:\n" + ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) { // Captura más genérica para SecurityException, etc.
            JOptionPane.showMessageDialog(mainFrame, "Ocurrió un error inesperado:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void updateEnabledState(VisorModel currentModel) {
        if (currentModel != null) {
             setEnabled(currentModel.getSelectedImageKey() != null);
        } else {
              setEnabled(false);
        }
    }
} // --- FIN de la clase LocateFileAction ---