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

//package vista.util;
//
//import java.awt.Image;
//import java.awt.image.BufferedImage;
//
//import modelo.VisorModel;
//import servicios.zoom.ZoomModeEnum;
//
//public class ImageDisplayUtils {
//
//    /**
//     * Re-escala una imagen original basándose en el modo de zoom y las dimensiones
//     * del contenedor. Esta utilidad ahora contiene toda la lógica de cálculo de
//     * tamaño para los modos de zoom automáticos.
//     *
//     * @param imagenOriginal La imagen fuente.
//     * @param model          El modelo de la aplicación, para obtener el modo de zoom y la opción de mantener proporciones.
//     * @param containerWidth El ancho del panel contenedor.
//     * @param containerHeight El alto del panel contenedor.
//     * @return Una nueva instancia de Image escalada, o null si hay un error.
//     */
//    public static Image reescalarImagenParaAjustar(
//        BufferedImage imagenOriginal, 
//        VisorModel model, 
//        int containerWidth,
//        int containerHeight
//    ) {
//        // --- SECCIÓN 1: VALIDACIONES ---
//        if (imagenOriginal == null || model == null || containerWidth <= 0 || containerHeight <= 0) {
//            return null;
//        }
//        
//        int imgOriginalAncho = imagenOriginal.getWidth();
//        int imgOriginalAlto = imagenOriginal.getHeight();
//        if (imgOriginalAncho <= 0 || imgOriginalAlto <= 0) {
//            return null;
//        }
//
//        // --- SECCIÓN 2: OBTENER ESTADO DEL MODELO ---
//        boolean mantenerProporciones = model.isMantenerProporcion();
//        ZoomModeEnum modo = model.getCurrentZoomMode();
//        double zoomFactorManual = model.getZoomFactor(); // Para el modo MAINTAIN_CURRENT_ZOOM
//
//        // --- SECCIÓN 3: CÁLCULO DE DIMENSIONES FINALES BASADO EN EL MODO ---
//        int anchoFinal;
//        int altoFinal;
//
//        switch (modo) {
//            case FIT_TO_SCREEN:
//                if (mantenerProporciones) {
//                    double ratioImg = (double) imgOriginalAncho / imgOriginalAlto;
//                    double ratioContenedor = (double) containerWidth / containerHeight;
//                    if (ratioContenedor > ratioImg) {
//                        altoFinal = containerHeight;
//                        anchoFinal = (int) (containerHeight * ratioImg);
//                    } else {
//                        anchoFinal = containerWidth;
//                        altoFinal = (int) (containerWidth / ratioImg);
//                    }
//                } else { // Estirar
//                    anchoFinal = containerWidth;
//                    altoFinal = containerHeight;
//                }
//                break;
//
//            case FIT_TO_WIDTH:
//                anchoFinal = containerWidth;
//                altoFinal = mantenerProporciones ? (int) (containerWidth / ((double) imgOriginalAncho / imgOriginalAlto)) : containerHeight;
//                break;
//
//            case FIT_TO_HEIGHT:
//                altoFinal = containerHeight;
//                anchoFinal = mantenerProporciones ? (int) (containerHeight * ((double) imgOriginalAncho / imgOriginalAlto)) : containerWidth;
//                break;
//            
//            case FILL:
//                // El modo FILL es un caso especial. Si se mantienen proporciones, es el más grande de FIT_WIDTH/HEIGHT.
//                if (mantenerProporciones) {
//                    double ratioImg = (double) imgOriginalAncho / imgOriginalAlto;
//                    double ratioContenedor = (double) containerWidth / containerHeight;
//                    if (ratioContenedor > ratioImg) { // Panel más ancho
//                        anchoFinal = containerWidth;
//                        altoFinal = (int) (containerWidth / ratioImg);
//                    } else { // Panel más alto
//                        altoFinal = containerHeight;
//                        anchoFinal = (int) (containerHeight * ratioImg);
//                    }
//                } else { // Estirar para llenar
//                    anchoFinal = containerWidth;
//                    altoFinal = containerHeight;
//                }
//                break;
//
//            case DISPLAY_ORIGINAL:
//                anchoFinal = imgOriginalAncho;
//                altoFinal = imgOriginalAlto;
//                break;
//
//            case MAINTAIN_CURRENT_ZOOM:
//            case USER_SPECIFIED_PERCENTAGE: // Los tratamos igual que el modo manual
//                anchoFinal = (int) (imgOriginalAncho * zoomFactorManual);
//                altoFinal = (int) (imgOriginalAlto * zoomFactorManual);
//                break;
//
//            default:
//                anchoFinal = imgOriginalAncho;
//                altoFinal = imgOriginalAlto;
//                break;
//        }
//
//        anchoFinal = Math.max(1, anchoFinal);
//        altoFinal = Math.max(1, altoFinal);
//
//        // --- SECCIÓN 4: RETORNAR LA IMAGEN ESCALADA ---
//        try {
//            // Usamos SCALE_FAST porque es mucho más rápido y para la visualización es suficiente.
//            // SCALE_SMOOTH puede causar ralentizaciones notorias al cambiar rápidamente de imagen.
//            return imagenOriginal.getScaledInstance(anchoFinal, altoFinal, Image.SCALE_FAST);
//        } catch (Exception e) { 
//             System.err.println("ERROR [ImageDisplayUtils] durante getScaledInstance: " + e.getMessage());
//             e.printStackTrace();
//             return null;
//        }
//    } // --- FIN del metodo reescalarImagenParaAjustar ---
//}