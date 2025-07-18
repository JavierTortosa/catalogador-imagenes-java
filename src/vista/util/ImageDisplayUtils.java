package vista.util;

import java.awt.Image;
import java.awt.image.BufferedImage;

public class ImageDisplayUtils {
    public static Image escalar(BufferedImage imagenOriginal, int anchoFinal, int altoFinal) {
        if (imagenOriginal == null) return null;
        int w = Math.max(1, anchoFinal);
        int h = Math.max(1, altoFinal);
        try {
            return imagenOriginal.getScaledInstance(w, h, Image.SCALE_FAST);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
// --- FIN de la clase ImageDisplayUtils ---
