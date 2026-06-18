package servicios.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.Component;


/**
 * Clase de utilidad para operaciones con archivos de imagen.
 */
public class ImageFileUtils {

    /**
     * Constructor privado para evitar instanciación.
     */
    private ImageFileUtils() {
    } // --- Fin de la clase/metodo ImageFileUtils ---


    /**
     * Guarda una imagen en un archivo, permitiendo al usuario elegir el formato.
     *
     * @param imagen La imagen a guardar.
     * @param parent El componente padre para el JFileChooser.
     */
    public static void guardarImagenComo(BufferedImage imagen, Component parent) {
        if (imagen == null) {
            return;
        }

        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle("Guardar imagen como");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG (*.png)", "png"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("JPEG (*.jpg, *.jpeg)", "jpg", "jpeg"));

        if (chooser.showSaveDialog(parent) == javax.swing.JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String format;

            if (chooser.getFileFilter() instanceof FileNameExtensionFilter) {
                String desc = chooser.getFileFilter().getDescription();
                if (desc.contains("JPEG")) {
                    format = "jpg";
                } else {
                    format = "png";
                }
            } else {
                format = "png";
            }

            if (!file.getName().toLowerCase().endsWith("." + format)) {
                file = new File(file.getAbsolutePath() + "." + format);
            }

            try {
                ImageIO.write(imagen, format, file);
                JOptionPane.showMessageDialog(parent, "Imagen guardada en:\n" + file.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(parent, "Error al guardar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    } // --- Fin de la clase/metodo guardarImagenComo ---

} // --- Fin de la clase ImageFileUtils ---
