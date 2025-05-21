// En vista.util.ImageDisplayUtils.java
package vista.util;

import java.awt.Image;
import java.awt.image.BufferedImage;
import modelo.VisorModel;
import vista.VisorView;

public class ImageDisplayUtils {

    public static Image reescalarImagenParaAjustar(BufferedImage imagenOriginal, VisorModel model, VisorView view) {
        // 1. Validaciones (copiadas y adaptadas de tu método original)
        if (imagenOriginal == null || model == null || view == null || view.getEtiquetaImagen() == null) {
            if (imagenOriginal == null) System.err.println("[ImageDisplayUtils] Imagen original es null.");
            if (model == null) System.err.println("[ImageDisplayUtils] VisorModel es null.");
            if (view == null) System.err.println("[ImageDisplayUtils] VisorView es null.");
            else if (view.getEtiquetaImagen() == null) System.err.println("[ImageDisplayUtils] EtiquetaImagen en VisorView es null.");
            return null;
        }

        // 2. Obtener dimensiones del componente de destino (la etiqueta)
        int anchoDestino = view.getEtiquetaImagen().getWidth();
        int altoDestino = view.getEtiquetaImagen().getHeight();

        if (anchoDestino <= 0 || altoDestino <= 0) {
            System.out.println("[ImageDisplayUtils] WARN: Etiqueta sin tamaño válido aún (" + anchoDestino + "x" + altoDestino + ").");
            return null;
        }

        // 3. Determinar dimensiones finales según 'mantener proporción'
        int anchoFinal;
        int altoFinal;
        boolean mantenerProporcion = model.isMantenerProporcion(); // Leer del modelo pasado como parámetro

        if (mantenerProporcion) {
            int anchoOriginalImg = imagenOriginal.getWidth();
            int altoOriginalImg = imagenOriginal.getHeight();
            if (anchoOriginalImg <= 0 || altoOriginalImg <= 0) {
                 System.err.println("[ImageDisplayUtils] Imagen original con dimensiones inválidas ("+anchoOriginalImg+"x"+altoOriginalImg+").");
                 return null;
            }

            double ratioImagen = (double) anchoOriginalImg / altoOriginalImg;
            double ratioDestino = (double) anchoDestino / altoDestino;

            if (ratioDestino > ratioImagen) {
                altoFinal = altoDestino;
                anchoFinal = (int) (altoDestino * ratioImagen);
            } else {
                anchoFinal = anchoDestino;
                altoFinal = (int) (anchoDestino / ratioImagen);
            }
        } else {
            anchoFinal = anchoDestino;
            altoFinal = altoDestino;
        }

        // 4. Asegurar dimensiones mínimas (1x1)
        anchoFinal = Math.max(1, anchoFinal);
        altoFinal = Math.max(1, altoFinal);

        // 5. Realizar el escalado
        try {
            return imagenOriginal.getScaledInstance(anchoFinal, altoFinal, Image.SCALE_SMOOTH);
        } catch (Exception e) {
             System.err.println("ERROR [ImageDisplayUtils.reescalarImagenParaAjustar - getScaledInstance]: " + e.getMessage());
             e.printStackTrace();
             return null;
        }
    }
}