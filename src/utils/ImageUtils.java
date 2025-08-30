package utils;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

public class ImageUtils {

	public static String getImageFormat(Path path) {
        if (path == null) return "N/A";
        String fileName = path.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1); // No es necesario toUpperCase aquí si lo haces en InfoBarManager
        }
        return "Desconocido";
    }
	
	
	/**
     * Corrige la orientación de una BufferedImage basándose en sus metadatos EXIF.
     * Lee la etiqueta de orientación del archivo original y aplica la transformación
     * necesaria a la imagen cargada en memoria.
     *
     * @param image La BufferedImage que se acaba de cargar.
     * @param imagePath El Path del archivo original, necesario para leer los metadatos.
     * @return Una nueva BufferedImage con la orientación corregida, o la imagen original si no hay
     *         información de orientación o si ocurre un error.
     */
    public static BufferedImage correctImageOrientation(BufferedImage image, Path imagePath) {
        if (image == null || imagePath == null) {
            return image;
        }

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imagePath.toFile());
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

            if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                int orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                AffineTransform transform = getExifTransform(orientation, image.getWidth(), image.getHeight());

                if (transform != null) {
                    // Determinar las nuevas dimensiones de la imagen
                    int newWidth = image.getWidth();
                    int newHeight = image.getHeight();
                    if (orientation >= 5 && orientation <= 8) { // Rotaciones de 90/270 grados
                        newWidth = image.getHeight();
                        newHeight = image.getWidth();
                    }

                    BufferedImage newImage = new BufferedImage(newWidth, newHeight, image.getType());
                    AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC);
                    op.filter(image, newImage);
                    
                    // logger.info("Orientación EXIF {} aplicada a la imagen {}", orientation, imagePath.getFileName());
                    return newImage;
                }
            }
        } catch (Exception e) {
            // No es un error crítico, simplemente no se pudo leer el EXIF.
            // logger.debug("No se pudo leer/aplicar metadatos EXIF para {}: {}", imagePath.getFileName(), e.getMessage());
        }

        return image; // Devuelve la imagen original si no se aplicó ninguna transformación.
    } // ---FIN de metodo correctImageOrientation---

    /**
     * Calcula la transformación afín necesaria para una orientación EXIF dada.
     */
    private static AffineTransform getExifTransform(int orientation, int width, int height) {
        AffineTransform t = new AffineTransform();
        switch (orientation) {
            case 1: // Normal
                return null;
            case 2: // Flip Horizontal
                t.scale(-1.0, 1.0);
                t.translate(-width, 0);
                break;
            case 3: // Rotar 180
                t.translate(width, height);
                t.rotate(Math.PI);
                break;
            case 4: // Flip Vertical
                t.scale(1.0, -1.0);
                t.translate(0, -height);
                break;
            case 5: // Transpose (Rotar 90 a la izq. y flip vertical)
                t.translate(height, 0);
                t.rotate(Math.PI / 2);
                t.scale(1.0, -1.0);
                break;
            case 6: // Rotar 90 a la der.
                t.translate(height, 0);
                t.rotate(Math.PI / 2);
                break;
            case 7: // Transverse (Rotar 90 a la der. y flip vertical)
                t.translate(0, 0);
                t.rotate(-Math.PI / 2);
                t.scale(1.0, -1.0);
                t.translate(0, -width);
                break;
            case 8: // Rotar 90 a la izq.
                t.translate(0, width);
                t.rotate(-Math.PI / 2);
                break;
            default:
                return null;
        }
        return t;
    } // ---FIN de metodo getExifTransform---

}