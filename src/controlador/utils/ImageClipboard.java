package controlador.utils;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilidad de acceso al portapapeles del sistema para im\u00E1genes.
 * <p>
 * Permite copiar una {@link BufferedImage} al portapapeles (visible para
 * aplicaciones externas como WhatsApp) y leer la imagen que haya en \u00E9l
 * (tanto si la copi\u00F3 la propia aplicaci\u00F3n como una externa).
 */
public final class ImageClipboard {

    private static final Logger logger = LoggerFactory.getLogger(ImageClipboard.class);

    private ImageClipboard() {
    } // --- Fin del constructor privado ImageClipboard ---


    /**
     * Copia la imagen indicada al portapapeles del sistema.
     *
     * @param image imagen a copiar; si es {@code null} no hace nada
     * @return {@code true} si se copi\u00F3 correctamente
     */
    public static boolean copy(BufferedImage image) {
        if (image == null) {
            return false;
        }
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new Transferable() {

                        @Override
                        public DataFlavor[] getTransferDataFlavors() {
                            return new DataFlavor[] { DataFlavor.imageFlavor };
                        } // --- Fin del metodo getTransferDataFlavors ---


                        @Override
                        public boolean isDataFlavorSupported(DataFlavor flavor) {
                            return DataFlavor.imageFlavor.equals(flavor);
                        } // --- Fin del metodo isDataFlavorSupported ---


                        @Override
                        public Object getTransferData(DataFlavor flavor) {
                            return image;
                        } // --- Fin del metodo getTransferData ---

                    }, null);
            return true;
        } catch (Exception e) {
            logger.error("Error al copiar imagen al portapapeles del sistema.", e);
            return false;
        }
    } // --- Fin del metodo copy ---


    /**
     * Lee la imagen que haya en el portapapeles del sistema.
     *
     * @return la imagen, o {@code null} si no hay ninguna v\u00E1lida
     */
    public static BufferedImage read() {
        try {
            Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (t != null && t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                Object data = t.getTransferData(DataFlavor.imageFlavor);
                if (data instanceof BufferedImage img) {
                    return img;
                }
            }
        } catch (Exception e) {
            logger.debug("No hay imagen en el portapapeles del sistema: {}", e.getMessage());
        }
        return null;
    } // --- Fin del metodo read ---

} // --- Fin de la clase ImageClipboard ---
