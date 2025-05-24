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
import javax.swing.JOptionPane;

import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;
import vista.VisorView;

public class LocateFileAction extends AbstractAction implements ContextSensitiveAction { // <<< IMPLEMENTAR INTERFAZ

    private static final long serialVersionUID = 1L; // Considera generar uno nuevo

    private VisorModel modelRef;
    private VisorView viewRef; // Para el JOptionPane

    // Constructor REFACTORIZADO
    public LocateFileAction(VisorModel model, 
                            VisorView view, 
                            String name, 
                            ImageIcon icon) {
        super(name, icon);
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null en LocateFileAction");
        this.viewRef = Objects.requireNonNull(view, "VisorView no puede ser null en LocateFileAction");

        putValue(Action.SHORT_DESCRIPTION, "Abre la carpeta que contiene la imagen actual en el explorador de archivos");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_IMAGEN_LOCALIZAR);

        // El estado inicial se establecerá la primera vez que se llame a updateEnabledState
        // desde el ListCoordinator o AppInitializer. O puedes llamarlo aquí si lo prefieres.
        updateEnabledState(this.modelRef); 
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // La lógica de actionPerformed no cambia mucho, solo usa modelRef y viewRef
        if (modelRef == null) {
            System.err.println("ERROR [LocateFileAction]: Model (modelRef) es null.");
            return;
        }
        
        

        String selectedKey = modelRef.getSelectedImageKey();
        if (selectedKey == null || selectedKey.isEmpty()) {
            // Podríamos deshabilitar la acción en lugar de mostrar un diálogo aquí,
            // ya que updateEnabledState debería manejarlo.
            // System.out.println("[LocateFileAction] No hay imagen seleccionada.");
            return;
        }

        Path filePath = modelRef.getRutaCompleta(selectedKey);
        if (filePath == null || !Files.exists(filePath)) {
             System.err.println("ERROR [LocateFileAction]: No se encontró la ruta completa o el archivo no existe para la clave: " + selectedKey);
             JOptionPane.showMessageDialog(viewRef != null ? viewRef.getFrame() : null,
                     "No se pudo encontrar la ruta del archivo seleccionado.", "Error al Localizar", JOptionPane.ERROR_MESSAGE);
             return;
        }

     // --- INICIO CÓDIGO MODIFICADO PARA SELECCIONAR ARCHIVO ---
        String osName = System.getProperty("os.name").toLowerCase();

        try {
            if (osName.contains("win")) {
                // Comando específico de Windows para abrir el explorador y seleccionar el archivo
                String command = "explorer.exe /select,\"" + filePath.toAbsolutePath().toString() + "\"";
                System.out.println("  [LocateFileAction] Ejecutando en Windows: " + command);
                Runtime.getRuntime().exec(command);
            } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                // Fallback para otros sistemas operativos (o si el comando de Windows falla por alguna razón):
                // Abrir solo el directorio padre.
                Path directoryPath = filePath.getParent();
                if (directoryPath != null && Files.isDirectory(directoryPath)) {
                    System.out.println("  [LocateFileAction] Abriendo directorio padre en SO no Windows (o fallback): " + directoryPath);
                    Desktop.getDesktop().open(directoryPath.toFile());
                } else {
                    throw new IOException("No se pudo obtener el directorio padre.");
                }
            } else {
                System.err.println("ERROR [LocateFileAction]: La API Desktop o la acción OPEN no son soportadas.");
                JOptionPane.showMessageDialog(viewRef != null ? viewRef.getFrame() : null,
                        "La interacción con el explorador de archivos no es compatible con este sistema.", "Funcionalidad no Soportada", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IOException ioe) {
            System.err.println("ERROR [LocateFileAction]: IOException: " + ioe.getMessage());
            JOptionPane.showMessageDialog(viewRef != null ? viewRef.getFrame() : null,
                    "Error al intentar interactuar con el explorador de archivos:\n" + ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SecurityException se) {
            System.err.println("ERROR [LocateFileAction]: SecurityException: " + se.getMessage());
            JOptionPane.showMessageDialog(viewRef != null ? viewRef.getFrame() : null,
                    "No se tienen permisos para esta operación.", "Error de Permisos", JOptionPane.ERROR_MESSAGE);
        } catch (UnsupportedOperationException uoe) {
            System.err.println("ERROR [LocateFileAction]: Operación no soportada: " + uoe.getMessage());
            JOptionPane.showMessageDialog(viewRef != null ? viewRef.getFrame() : null,
                    "Esta operación no es compatible con este sistema.", "Operación no Soportada", JOptionPane.WARNING_MESSAGE);
        }
        // --- FIN CÓDIGO MODIFICADO ---
    }
        
        
        
        
//        Path directoryPath = filePath.getParent();
//        if (directoryPath == null || !Files.isDirectory(directoryPath)) {
//             System.err.println("ERROR [LocateFileAction]: No se pudo obtener el directorio padre para: " + filePath);
//              JOptionPane.showMessageDialog(viewRef != null ? viewRef.getFrame() : null,
//                      "No se pudo determinar la carpeta contenedora.", "Error al Localizar", JOptionPane.ERROR_MESSAGE);
//             return;
//        }
//
//        if (Desktop.isDesktopSupported()) {
//            Desktop desktop = Desktop.getDesktop();
//            if (desktop.isSupported(Desktop.Action.OPEN)) {
//                try {
//                    desktop.open(directoryPath.toFile());
//                } catch (IOException ioe) {
//                     System.err.println("ERROR [LocateFileAction]: IOException al intentar abrir el directorio: " + ioe.getMessage());
//                     JOptionPane.showMessageDialog(viewRef != null ? viewRef.getFrame() : null,
//                             "Error al intentar abrir la carpeta:\n" + ioe.getMessage(), "Error de Apertura", JOptionPane.ERROR_MESSAGE);
//                } catch (SecurityException se) {
//                     System.err.println("ERROR [LocateFileAction]: SecurityException: " + se.getMessage());
//                      JOptionPane.showMessageDialog(viewRef != null ? viewRef.getFrame() : null,
//                              "No se tienen permisos para abrir la carpeta.", "Error de Permisos", JOptionPane.ERROR_MESSAGE);
//                } catch (UnsupportedOperationException uoe) {
//                     System.err.println("ERROR [LocateFileAction]: Operación OPEN no soportada: " + uoe.getMessage());
//                      JOptionPane.showMessageDialog(viewRef != null ? viewRef.getFrame() : null,
//                              "La apertura de carpetas no es compatible con este sistema.", "Operación no Soportada", JOptionPane.WARNING_MESSAGE);
//                }
//            } else {
//                 System.err.println("ERROR [LocateFileAction]: La acción Desktop.Action.OPEN no es soportada.");
//                  JOptionPane.showMessageDialog(viewRef != null ? viewRef.getFrame() : null,
//                          "La apertura de carpetas no es compatible con este sistema.", "Operación no Soportada", JOptionPane.WARNING_MESSAGE);
//            }
//        } else {
//            System.err.println("ERROR [LocateFileAction]: La API Desktop no es soportada.");
//             JOptionPane.showMessageDialog(viewRef != null ? viewRef.getFrame() : null,
//                     "La interacción con el escritorio no es compatible con este sistema.", "Funcionalidad no Soportada", JOptionPane.WARNING_MESSAGE);
//        }
//    }

    // Implementación del método de la interfaz ContextSensitiveAction
    @Override
    public void updateEnabledState(VisorModel currentModel) {
         if (currentModel != null) {
             // La acción está habilitada si hay una clave de imagen seleccionada en el modelo.
             // Podrías añadir más condiciones si fuera necesario (ej. si la ruta es válida).
             setEnabled(currentModel.getSelectedImageKey() != null);
         } else {
              setEnabled(false); // Deshabilitar si el modelo no está disponible
         }
    }
}


