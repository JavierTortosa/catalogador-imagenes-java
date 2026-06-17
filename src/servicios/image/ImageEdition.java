package servicios.image; 

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

public class ImageEdition {

 /**
  * Voltea una imagen horizontalmente.
  * @param source La imagen original.
  * @return Una NUEVA BufferedImage volteada, o null si la entrada es null o hay error.
  */
 public static BufferedImage flipHorizontal(BufferedImage source) {
     if (source == null) return null;

     try {
         AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
         tx.translate(-source.getWidth(null), 0);
         // Usar interpolación NEAREST_NEIGHBOR para volteos/rotaciones 90º es más rápido
         // y no introduce artefactos. Bilinear es mejor para escalados.
         AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

         // Crear imagen destino compatible
         BufferedImage destination = createCompatibleImage(source);
         op.filter(source, destination); // Aplicar
         return destination;

     } catch (Exception e) {
         System.err.println("ERROR [ImageEditor.flipHorizontal]: " + e.getMessage());
         e.printStackTrace();
         return null; // Devolver null en caso de error
     }
 }

 /**
  * Voltea una imagen verticalmente.
  * @param source La imagen original.
  * @return Una NUEVA BufferedImage volteada, o null si la entrada es null o hay error.
  */
 public static BufferedImage flipVertical(BufferedImage source) {
     if (source == null) return null;

     try {
         AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
         tx.translate(0, -source.getHeight(null));
         AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

         BufferedImage destination = createCompatibleImage(source);
         op.filter(source, destination);
         return destination;

     } catch (Exception e) {
         System.err.println("ERROR [ImageEditor.flipVertical]: " + e.getMessage());
         e.printStackTrace();
         return null;
     }
 }

 /**
  * Rota una imagen 90 grados a la izquierda (sentido antihorario).
  * @param source La imagen original.
  * @return Una NUEVA BufferedImage rotada, o null si la entrada es null o hay error.
  */
 public static BufferedImage rotateLeft(BufferedImage source) {
     if (source == null) return null;

     try {
         int width = source.getWidth();
         int height = source.getHeight();

         // Crear transformación: rotar -90º alrededor del centro (width/2, height/2)
         // y luego trasladar para que la esquina (0,0) quede correcta.
         AffineTransform tx = new AffineTransform();
         tx.translate(0, width); // Nueva altura es el ancho original
         tx.rotate(Math.toRadians(-90)); // Rotar -90 grados

         AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

         // La imagen destino tendrá dimensiones intercambiadas
         BufferedImage destination = createCompatibleImage(source, height, width); // Ancho y alto invertidos
         op.filter(source, destination);
         return destination;

      } catch (Exception e) {
         System.err.println("ERROR [ImageEditor.rotateLeft]: " + e.getMessage());
         e.printStackTrace();
         return null;
     }
 }

  /**
  * Rota una imagen 90 grados a la derecha (sentido horario).
  * @param source La imagen original.
  * @return Una NUEVA BufferedImage rotada, o null si la entrada es null o hay error.
  */
 public static BufferedImage rotateRight(BufferedImage source) {
      if (source == null) return null;

     try {
         int width = source.getWidth();
         int height = source.getHeight();

         AffineTransform tx = new AffineTransform();
         tx.translate(height, 0); // Nueva anchura es el alto original
         tx.rotate(Math.toRadians(90));

         AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

         BufferedImage destination = createCompatibleImage(source, height, width); // Ancho y alto invertidos
         op.filter(source, destination);
         return destination;

      } catch (Exception e) {
         System.err.println("ERROR [ImageEditor.rotateRight]: " + e.getMessage());
         e.printStackTrace();
         return null;
     }
 }

 /**
  * Recorta una región rectangular de la imagen.
  * @param source La imagen original.
  * @param x Coordenada X de inicio del recorte.
  * @param y Coordenada Y de inicio del recorte.
  * @param width Ancho del recorte.
  * @param height Alto del recorte.
  * @return Una NUEVA BufferedImage con la región recortada, o null si la entrada es null o hay error.
  */
 public static BufferedImage crop(BufferedImage source, int x, int y, int width, int height) {
     if (source == null) return null;
     if (width <= 0 || height <= 0) return null;
     if (x < 0 || y < 0 || x + width > source.getWidth() || y + height > source.getHeight()) return null;

     try {
         BufferedImage sub = source.getSubimage(x, y, width, height);
         BufferedImage copy = new BufferedImage(width, height, sub.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : sub.getType());
         copy.getGraphics().drawImage(sub, 0, 0, null);
         return copy;
     } catch (Exception e) {
         System.err.println("ERROR [ImageEditor.crop]: " + e.getMessage());
         e.printStackTrace();
         return null;
     }
 }


 /**
  * Helper para crear una imagen compatible con la original, manejando la transparencia.
  */
 private static BufferedImage createCompatibleImage(BufferedImage source) {
     return createCompatibleImage(source, source.getWidth(), source.getHeight());
 }

 /**
  * Helper para crear una imagen compatible con la original, con dimensiones específicas.
  */
  private static BufferedImage createCompatibleImage(BufferedImage source, int width, int height) {
      // Usar el tipo de la imagen original si es posible, si no, un tipo común como ARGB
      // Ojo con tipos indexados, pueden dar problemas. ARGB es más seguro.
      int imageType = source.getType();
      if (imageType == BufferedImage.TYPE_BYTE_INDEXED || imageType == BufferedImage.TYPE_BYTE_BINARY) {
          imageType = BufferedImage.TYPE_INT_ARGB; // Usar ARGB para tipos indexados/binarios
      } else if (imageType == BufferedImage.TYPE_CUSTOM) {
          imageType = BufferedImage.TYPE_INT_ARGB; // Usar ARGB para tipos custom
      }

      BufferedImage compatibleImage = new BufferedImage(width, height, imageType);

      // Copiar información de transparencia si existe (importante para PNGs)
      if (source.getColorModel().hasAlpha()) {
          // Forzar un canal alfa en la imagen destino si no lo tenía por defecto
          if (!compatibleImage.getColorModel().hasAlpha()) {
              compatibleImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
          }
          // Copiar el raster alfa (esto es una simplificación, puede fallar en algunos casos)
          // WritableRaster alphaRaster = source.getAlphaRaster();
          // if (alphaRaster != null) {
          //     compatibleImage.getAlphaRaster().setRect(alphaRaster);
          // }
      }
      return compatibleImage;
  }
}
