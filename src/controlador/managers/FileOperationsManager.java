package controlador.managers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer; // Para el callback

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import modelo.VisorModel;
import servicios.ConfigurationManager;
import vista.VisorView;

public class FileOperationsManager {

    private VisorModel model;
    private VisorView view;
    private ConfigurationManager configuration;
    private Consumer<Path> onNuevaCarpetaSeleccionadaCallback; // Callback para notificar al Controller

    public FileOperationsManager(VisorModel model, 
                       VisorView view, 
                       ConfigurationManager configuration,
                       Consumer<Path> onNuevaCarpetaSeleccionadaCallback) {
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en FileManager");
        this.view = Objects.requireNonNull(view, "VisorView no puede ser null en FileManager");
        this.configuration = Objects.requireNonNull(configuration, "ConfigurationManager no puede ser null en FileManager");
        this.onNuevaCarpetaSeleccionadaCallback = Objects.requireNonNull(onNuevaCarpetaSeleccionadaCallback, "Callback no puede ser null");
    }

    /**
     * Muestra un diálogo para que el usuario seleccione una nueva carpeta raíz.
     * Si se selecciona una carpeta válida y diferente a la actual,
     * actualiza el modelo, la configuración y luego invoca el callback
     * para que el sistema principal recargue la lista de imágenes.
     */
    public void solicitarSeleccionNuevaCarpeta() {
        System.out.println("[FileManager] Iniciando selección de nueva carpeta...");

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Seleccionar Nueva Carpeta de Imágenes");

        // Establecer directorio inicial del JFileChooser
        Path dirActual = model.getCarpetaRaizActual(); // Asumiendo que VisorModel tiene este getter
        if (dirActual != null && Files.isDirectory(dirActual)) {
            fileChooser.setCurrentDirectory(dirActual.toFile());
        } else {
            String dirConfig = configuration.getString(ConfigurationManager.KEY_INICIO_CARPETA, null); // Asume que tienes esta constante
            if (dirConfig != null) {
                File f = new File(dirConfig);
                if (f.isDirectory()) {
                    fileChooser.setCurrentDirectory(f);
                }
            }
        }

        int resultado = fileChooser.showOpenDialog(this.view.getFrame());

        if (resultado == JFileChooser.APPROVE_OPTION) {
            File carpetaSeleccionadaFile = fileChooser.getSelectedFile();
            if (carpetaSeleccionadaFile != null && carpetaSeleccionadaFile.isDirectory()) {
                Path nuevaCarpetaPath = carpetaSeleccionadaFile.toPath();
                
                if (!nuevaCarpetaPath.equals(model.getCarpetaRaizActual())) {
                    System.out.println("  [FileManager] Nueva carpeta seleccionada: " + nuevaCarpetaPath);
                    
                    // Actualizar modelo
                    model.setCarpetaRaizActual(nuevaCarpetaPath); // Asumiendo que VisorModel tiene este setter
                    
                    // Actualizar configuración
                    configuration.setString(ConfigurationManager.KEY_INICIO_CARPETA, nuevaCarpetaPath.toAbsolutePath().toString());
                    try {
                        configuration.guardarConfiguracion(configuration.getConfigMap());
                    } catch (IOException e) {
                        System.err.println("ERROR [FileManager] al guardar config tras seleccionar nueva carpeta: " + e.getMessage());
                        // No es fatal para la operación actual, pero sí para la persistencia.
                        JOptionPane.showMessageDialog(this.view.getFrame(), 
                                                      "No se pudo guardar la nueva carpeta en la configuración.", 
                                                      "Error de Configuración", JOptionPane.WARNING_MESSAGE);
                    }
                    
                    // Invocar el callback para que el VisorController recargue la lista
                    this.onNuevaCarpetaSeleccionadaCallback.accept(nuevaCarpetaPath); // El Path es informativo, el controller ya lo sabe por el modelo

                } else {
                    System.out.println("  [FileManager] La carpeta seleccionada es la misma que la actual.");
                    JOptionPane.showMessageDialog(this.view.getFrame(),
                                                  "La carpeta seleccionada ya es la carpeta actual.",
                                                  "Información", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                System.err.println("  [FileManager] Selección inválida o no es un directorio.");
                 JOptionPane.showMessageDialog(this.view.getFrame(), 
                                             "La selección no es una carpeta válida.", 
                                             "Selección Inválida", 
                                             JOptionPane.WARNING_MESSAGE);
            }
        } else {
            System.out.println("  [FileManager] Selección de carpeta cancelada.");
        }
    }
    
    
 // En src/controlador/managers/FileOperationsManager.java
 // ... (constructor y solicitarSeleccionNuevaCarpeta() como antes) ...

     /**
      * Intenta borrar la imagen actualmente seleccionada en el VisorModel.
      * Pide confirmación al usuario. Si se confirma y el borrado es exitoso,
      * invoca el callback onNuevaCarpetaSeleccionadaCallback (reutilizado aquí para
      * indicar que la lista debe recargarse/actualizarse).
      */
     public void borrarArchivoSeleccionado() {
         System.out.println("[FileOperationsManager] Iniciando borrado de archivo seleccionado...");

         if (model == null || view == null || configuration == null || onNuevaCarpetaSeleccionadaCallback == null) {
             System.err.println("ERROR CRÍTICO [FileOperationsManager.borrarArchivoSeleccionado]: Dependencias nulas.");
             return;
         }

         String claveImagenSeleccionada = model.getSelectedImageKey();
         if (claveImagenSeleccionada == null) {
             JOptionPane.showMessageDialog(view.getFrame(), "No hay ninguna imagen seleccionada para eliminar.", "Eliminar Imagen", JOptionPane.INFORMATION_MESSAGE);
             return;
         }

         Path rutaCompleta = model.getRutaCompleta(claveImagenSeleccionada);
         if (rutaCompleta == null) {
             JOptionPane.showMessageDialog(view.getFrame(), "No se pudo encontrar la ruta del archivo seleccionado.", "Error al Eliminar", JOptionPane.ERROR_MESSAGE);
             System.err.println("  [FileOperationsManager] No se pudo obtener ruta para clave: " + claveImagenSeleccionada);
             return;
         }

         int confirm = JOptionPane.showConfirmDialog(
             view.getFrame(),
             "¿Está seguro de que desea eliminar el archivo?\n" + rutaCompleta.getFileName().toString() +
             "\nEsta acción no se puede deshacer.",
             "Confirmar Eliminación",
             JOptionPane.YES_NO_OPTION,
             JOptionPane.WARNING_MESSAGE
         );

         if (confirm == JOptionPane.YES_OPTION) {
             System.out.println("  [FileOperationsManager] Usuario confirmó eliminación para: " + rutaCompleta);
             boolean borradoExitoso = false;
             try {
                 File archivoABorrar = rutaCompleta.toFile();
                 if (archivoABorrar.exists()) {
                     borradoExitoso = Files.deleteIfExists(rutaCompleta); // Usar Files.deleteIfExists
                 } else {
                     System.err.println("  [FileOperationsManager] El archivo a borrar no existe: " + rutaCompleta);
                 }
             } catch (SecurityException se) {
                 System.err.println("ERROR [FileOperationsManager]: Excepción de seguridad al borrar: " + se.getMessage());
                 JOptionPane.showMessageDialog(view.getFrame(), "No se tienen permisos para eliminar el archivo.", "Error de Permisos", JOptionPane.ERROR_MESSAGE);
                 return;
             } catch (IOException ioEx) {
                 System.err.println("ERROR [FileOperationsManager]: IOException al borrar: " + ioEx.getMessage());
                 JOptionPane.showMessageDialog(view.getFrame(), "Error de E/S al eliminar el archivo.", "Error al Eliminar", JOptionPane.ERROR_MESSAGE);
                 return;
             } catch (Exception ex) {
                 System.err.println("ERROR [FileOperationsManager]: Excepción inesperada al borrar: " + ex.getMessage());
                 ex.printStackTrace();
                 JOptionPane.showMessageDialog(view.getFrame(), "Ocurrió un error inesperado al eliminar el archivo.", "Error al Eliminar", JOptionPane.ERROR_MESSAGE);
                 return;
             }

             if (borradoExitoso) {
                 System.out.println("  [FileOperationsManager] Archivo borrado: " + rutaCompleta);
                 JOptionPane.showMessageDialog(view.getFrame(), "Archivo eliminado correctamente.", "Eliminación Exitosa", JOptionPane.INFORMATION_MESSAGE);
                 
                 // Notificar que la lista necesita ser actualizada.
                 // El path es solo informativo, el VisorController recargará la lista actual.
                 this.onNuevaCarpetaSeleccionadaCallback.accept(rutaCompleta); 
             } else {
                 System.err.println("  [FileOperationsManager] No se pudo borrar el archivo (Files.deleteIfExists devolvió false): " + rutaCompleta);
                 JOptionPane.showMessageDialog(view.getFrame(), "No se pudo eliminar el archivo.", "Error al Eliminar", JOptionPane.ERROR_MESSAGE);
             }
         } else {
             System.out.println("  [FileOperationsManager] Eliminación cancelada por el usuario.");
         }
     }

    
    // Aquí irían futuros métodos como:
    // public void refrescarListaActual() { ... }
    // public void borrarArchivo(Path archivoABorrar) { ... }
}



