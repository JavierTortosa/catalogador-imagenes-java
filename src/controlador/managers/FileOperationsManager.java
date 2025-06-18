// Archivo: controlador/managers/FileOperationsManager.java

package controlador.managers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import controlador.VisorController; // <-- NUEVO IMPORT
import modelo.VisorModel;
import servicios.ConfigurationManager;
// No se necesita importar VisorView

public class FileOperationsManager {

    private final VisorModel model;
    private final VisorController controller; // <-- CAMBIO
    private final ConfigurationManager configuration;
    private final Consumer<Path> onNuevaCarpetaSeleccionadaCallback;

    public FileOperationsManager(
        VisorModel model, 
        VisorController controller, // <-- CAMBIO
        ConfigurationManager configuration,
        Consumer<Path> onNuevaCarpetaSeleccionadaCallback) {
        
        this.model = Objects.requireNonNull(model);
        this.controller = Objects.requireNonNull(controller); // <-- CAMBIO
        this.configuration = Objects.requireNonNull(configuration);
        this.onNuevaCarpetaSeleccionadaCallback = Objects.requireNonNull(onNuevaCarpetaSeleccionadaCallback);
    }

    public void solicitarSeleccionNuevaCarpeta() {
        System.out.println("[FileManager] Iniciando selección de nueva carpeta...");
        JFrame mainFrame = controller.getView(); // Obtener el frame una sola vez
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Seleccionar Nueva Carpeta de Imágenes");

        Path dirActual = model.getCarpetaRaizActual();
        if (dirActual != null && Files.isDirectory(dirActual)) {
            fileChooser.setCurrentDirectory(dirActual.toFile());
        } else {
            String dirConfig = configuration.getString(ConfigurationManager.KEY_INICIO_CARPETA, null);
            if (dirConfig != null) {
                File f = new File(dirConfig);
                if (f.isDirectory()) fileChooser.setCurrentDirectory(f);
            }
        }

        int resultado = fileChooser.showOpenDialog(mainFrame);

        if (resultado == JFileChooser.APPROVE_OPTION) {
            File carpetaSeleccionadaFile = fileChooser.getSelectedFile();
            if (carpetaSeleccionadaFile != null && carpetaSeleccionadaFile.isDirectory()) {
                Path nuevaCarpetaPath = carpetaSeleccionadaFile.toPath();
                if (!nuevaCarpetaPath.equals(model.getCarpetaRaizActual())) {
                    model.setCarpetaRaizActual(nuevaCarpetaPath);
                    configuration.setString(ConfigurationManager.KEY_INICIO_CARPETA, nuevaCarpetaPath.toAbsolutePath().toString());
                    try {
                        configuration.guardarConfiguracion(configuration.getConfigMap());
                    } catch (IOException e) {
                        System.err.println("ERROR [FileManager] al guardar config: " + e.getMessage());
                        JOptionPane.showMessageDialog(mainFrame, "No se pudo guardar la nueva carpeta en la configuración.", "Error de Configuración", JOptionPane.WARNING_MESSAGE);
                    }
                    this.onNuevaCarpetaSeleccionadaCallback.accept(nuevaCarpetaPath);
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "La carpeta seleccionada ya es la carpeta actual.", "Información", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                 JOptionPane.showMessageDialog(mainFrame, "La selección no es una carpeta válida.", "Selección Inválida", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            System.out.println("  [FileManager] Selección de carpeta cancelada.");
        }
    }

    public void borrarArchivoSeleccionado() {
        System.out.println("[FileOperationsManager] Iniciando borrado...");
        JFrame mainFrame = controller.getView(); // Obtener el frame una sola vez

        if (model == null) return;

        String claveImagenSeleccionada = model.getSelectedImageKey();
        if (claveImagenSeleccionada == null) {
            JOptionPane.showMessageDialog(mainFrame, "No hay ninguna imagen seleccionada para eliminar.", "Eliminar Imagen", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Path rutaCompleta = model.getRutaCompleta(claveImagenSeleccionada);
        if (rutaCompleta == null) {
            JOptionPane.showMessageDialog(mainFrame, "No se pudo encontrar la ruta del archivo seleccionado.", "Error al Eliminar", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            mainFrame,
            "¿Está seguro de que desea eliminar el archivo?\n" + rutaCompleta.getFileName().toString(),
            "Confirmar Eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean borradoExitoso = Files.deleteIfExists(rutaCompleta);
                if (borradoExitoso) {
                    System.out.println("  [FileOperationsManager] Archivo borrado: " + rutaCompleta);
                    JOptionPane.showMessageDialog(mainFrame, "Archivo eliminado correctamente.", "Eliminación Exitosa", JOptionPane.INFORMATION_MESSAGE);
                    this.onNuevaCarpetaSeleccionadaCallback.accept(rutaCompleta);
                } else {
                    System.err.println("  [FileOperationsManager] No se pudo borrar el archivo: " + rutaCompleta);
                    JOptionPane.showMessageDialog(mainFrame, "No se pudo eliminar el archivo.", "Error al Eliminar", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                System.err.println("ERROR [FileOperationsManager] al borrar: " + ex.getMessage());
                JOptionPane.showMessageDialog(mainFrame, "Ocurrió un error al eliminar el archivo:\n" + ex.getMessage(), "Error al Eliminar", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            System.out.println("  [FileOperationsManager] Eliminación cancelada por el usuario.");
        }
    }
} // --- FIN de la clase FileOperationsManager ---