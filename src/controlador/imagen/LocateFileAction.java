// --- INICIO NUEVO ARCHIVO: controlador/actions/archivo/LocateFileAction.java ---
package controlador.imagen; // O el paquete que elijas

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils;
// Quita IconUtils si no pones icono, o añádelo si sí
// import vista.util.IconUtils;

public class LocateFileAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L;

    public LocateFileAction(VisorController controller, IconUtils iconUtils, int width, int height) {
        // Llama al constructor de BaseVisorAction
        super("Abrir Ubicacion del Archivo", controller); // Texto que aparecerá si no hay icono

        // Configurar propiedades de la Action
        putValue(Action.SHORT_DESCRIPTION, "Abre la carpeta que contiene la imagen actual en el explorador de archivos");
        // putValue(Action.MNEMONIC_KEY, KeyEvent.VK_L); // Opcional: Atajo de teclado (Alt+L)

        // Opcional: Añadir un icono (si tienes uno apropiado)
        
        ImageIcon icon = iconUtils.getScaledIcon("7004-Ubicacion_de_Archivo_48x48.png", width, height);
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
        }
        

        // Habilitar/Deshabilitar inicialmente (depende de si hay imagen al inicio)
        // Lo haremos dinámicamente en el controller o actualizando la action.
        // Por defecto, podría empezar deshabilitada hasta que se cargue una imagen.
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller == null || controller.getModel() == null) {
            System.err.println("ERROR [LocateFileAction]: Controller o Model son null.");
            return;
        }
        controller.logActionInfo(e); // Loguear acción

        // 1. Obtener la clave de la imagen seleccionada del modelo
        String selectedKey = controller.getModel().getSelectedImageKey();
        if (selectedKey == null || selectedKey.isEmpty()) {
            System.out.println("[LocateFileAction] No hay imagen seleccionada.");
            // Opcional: Mostrar mensaje al usuario
            // JOptionPane.showMessageDialog(controller.getView().getFrame(), "No hay ninguna imagen seleccionada.", "Localizar Archivo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // 2. Obtener la ruta completa del archivo desde el modelo
        Path filePath = controller.getModel().getRutaCompleta(selectedKey);
        if (filePath == null || !Files.exists(filePath)) {
             System.err.println("ERROR [LocateFileAction]: No se encontró la ruta completa o el archivo no existe para la clave: " + selectedKey);
             JOptionPane.showMessageDialog(controller.getView() != null ? controller.getView().getFrame() : null,
                     "No se pudo encontrar la ruta del archivo seleccionado.", "Error al Localizar", JOptionPane.ERROR_MESSAGE);
             return;
        }

        // 3. Obtener el directorio padre
        Path directoryPath = filePath.getParent();
        if (directoryPath == null || !Files.isDirectory(directoryPath)) {
             System.err.println("ERROR [LocateFileAction]: No se pudo obtener el directorio padre para: " + filePath);
              JOptionPane.showMessageDialog(controller.getView() != null ? controller.getView().getFrame() : null,
                      "No se pudo determinar la carpeta contenedora.", "Error al Localizar", JOptionPane.ERROR_MESSAGE);
             return;
        }

        // 4. Intentar abrir el directorio usando Desktop API
        if (Desktop.isDesktopSupported()) { // Verificar si Desktop API es compatible
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) { // Verificar si la acción OPEN es compatible
                try {
                    System.out.println("[LocateFileAction] Abriendo directorio: " + directoryPath);
                    desktop.open(directoryPath.toFile()); // ¡Abrir la carpeta!
                } catch (IOException ioe) {
                     System.err.println("ERROR [LocateFileAction]: IOException al intentar abrir el directorio: " + ioe.getMessage());
                     JOptionPane.showMessageDialog(controller.getView() != null ? controller.getView().getFrame() : null,
                             "Error al intentar abrir la carpeta:\n" + ioe.getMessage(), "Error de Apertura", JOptionPane.ERROR_MESSAGE);
                     ioe.printStackTrace(); // Para depuración
                } catch (SecurityException se) {
                     System.err.println("ERROR [LocateFileAction]: SecurityException al intentar abrir el directorio: " + se.getMessage());
                      JOptionPane.showMessageDialog(controller.getView() != null ? controller.getView().getFrame() : null,
                              "No se tienen permisos para abrir la carpeta.", "Error de Permisos", JOptionPane.ERROR_MESSAGE);
                } catch (UnsupportedOperationException uoe) {
                     System.err.println("ERROR [LocateFileAction]: La operación OPEN no es soportada en esta plataforma para directorios: " + uoe.getMessage());
                      JOptionPane.showMessageDialog(controller.getView() != null ? controller.getView().getFrame() : null,
                              "La apertura de carpetas no es compatible con este sistema.", "Operación no Soportada", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                 System.err.println("ERROR [LocateFileAction]: La acción Desktop.Action.OPEN no es soportada.");
                  JOptionPane.showMessageDialog(controller.getView() != null ? controller.getView().getFrame() : null,
                          "La apertura de carpetas no es compatible con este sistema.", "Operación no Soportada", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            System.err.println("ERROR [LocateFileAction]: La API Desktop no es soportada en esta plataforma.");
             JOptionPane.showMessageDialog(controller.getView() != null ? controller.getView().getFrame() : null,
                     "La interacción con el escritorio no es compatible con este sistema.", "Funcionalidad no Soportada", JOptionPane.WARNING_MESSAGE);
        }
    } // Fin actionPerformed

    // Método para actualizar el estado enabled (llamado desde el Controller)
    public void updateEnabledState() {
         if (controller != null && controller.getModel() != null) {
             boolean enabled = controller.getModel().getSelectedImageKey() != null;
             // System.out.println("[LocateFileAction updateEnabledState] Estado: " + enabled); // Log opcional
             setEnabled(enabled);
         } else {
              setEnabled(false); // Deshabilitar si no hay controller/model
         }
    }

} // Fin clase LocateFileAction
// --- FIN NUEVO ARCHIVO: controlador/actions/archivo/LocateFileAction.java ---