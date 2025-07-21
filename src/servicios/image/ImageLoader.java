package servicios.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public class ImageLoader {

    /**
     * Carga una imagen desde una ruta de archivo.
     * Maneja excepciones internamente y devuelve null en caso de error.
     * @param path La ruta al archivo de imagen.
     * @return La imagen como un BufferedImage, o null si ocurre un error.
     */
    public static BufferedImage loadImage(Path path) {
        if (path == null) {
            return null;
        }
        try {
            return ImageIO.read(path.toFile());
        } catch (IOException e) {
            System.err.println("Error al cargar la imagen para previsualización: " + path.toString());
            e.printStackTrace();
            return null;
        }
    }
}