package vista.util;

import java.awt.Image;
import java.awt.image.BufferedImage;

import modelo.VisorModel;
import servicios.zoom.ZoomModeEnum;
import vista.VisorView;

public class ImageDisplayUtils {

	public static Image reescalarImagenParaAjustar(BufferedImage imagenOriginal, VisorModel model, VisorView view) {
	    // --- SECCIÓN 1: VALIDACIONES --- (como las tienes)
	    if (imagenOriginal == null || model == null || view == null || view.getEtiquetaImagen() == null) return null;
	    int etiquetaAncho = view.getEtiquetaImagen().getWidth();
	    int etiquetaAlto = view.getEtiquetaImagen().getHeight();
	    if (etiquetaAncho <= 0 || etiquetaAlto <= 0) { // Añadido para ser más robusto al inicio
	        // Si la etiqueta no tiene tamaño, no podemos hacer escalados relativos a ella.
	        // Devolver la original para que ZoomManager decida con factor 1.0 si es DISPLAY_ORIGINAL, etc.
	        return imagenOriginal;
	    }
	    int imgOriginalAncho = imagenOriginal.getWidth();
	    int imgOriginalAlto = imagenOriginal.getHeight();
	    if (imgOriginalAncho <= 0 || imgOriginalAlto <= 0) return null;

	    // --- SECCIÓN 2: OBTENER ESTADO DEL MODELO ---
	    boolean mantenerProporcionGlobal = model.isMantenerProporcion(); // El toggle "prop"
	    ZoomModeEnum currentMode = model.getCurrentZoomMode();

	    // --- SECCIÓN 3: DECIDIR ESCALADO BASE ---
	    int anchoFinalBase;
	    int altoFinalBase;

	    // Si el modo de zoom NO es FIT_TO_SCREEN, ImageDisplayUtils devuelve la imagen original.
	    // ZoomManager se encargará de calcular el factor correcto para DISPLAY_ORIGINAL, FIT_TO_WIDTH, FIT_TO_HEIGHT, etc.
	    // aplicado a la imagen original.
	    if (currentMode != ZoomModeEnum.FIT_TO_SCREEN) {
	        // System.out.println("  [ImageDisplayUtils] Modo " + currentMode + ": Devolviendo original. ZoomManager calculará factor.");
	        return imagenOriginal;
	    }

	    // Si LLEGAMOS AQUÍ, el modo es FIT_TO_SCREEN.
	    // Aquí sí aplicamos la lógica del toggle "mantenerProporcionGlobal".
	    if (mantenerProporcionGlobal) { // prop ON para FIT_TO_SCREEN
	        // Ajustar para que quepa manteniendo proporción ("contain")
	        double ratioImg = (double) imgOriginalAncho / imgOriginalAlto;
	        double ratioEtiqueta = (double) etiquetaAncho / etiquetaAlto;
	        if (ratioEtiqueta > ratioImg) {
	            altoFinalBase = etiquetaAlto;
	            anchoFinalBase = (int) (etiquetaAlto * ratioImg);
	        } else {
	            anchoFinalBase = etiquetaAncho;
	            altoFinalBase = (int) (etiquetaAncho / ratioImg);
	        }
	        // System.out.println("  [ImageDisplayUtils] FIT_TO_SCREEN prop ON: Base ajustada proporcionalmente a: " + anchoFinalBase + "x" + altoFinalBase);
	    } else { // prop OFF para FIT_TO_SCREEN
	        // Estirar para llenar la etiqueta
	        anchoFinalBase = etiquetaAncho;
	        altoFinalBase = etiquetaAlto;
	        // System.out.println("  [ImageDisplayUtils] FIT_TO_SCREEN prop OFF: Base estirada a: " + anchoFinalBase + "x" + altoFinalBase);
	    }

	    anchoFinalBase = Math.max(1, anchoFinalBase);
	    altoFinalBase = Math.max(1, altoFinalBase);

	    try {
	        return imagenOriginal.getScaledInstance(anchoFinalBase, altoFinalBase, Image.SCALE_SMOOTH);
	    } catch (Exception e) { 
	         System.err.println("ERROR [ImageDisplayUtils.reescalarImagenParaAjustar] durante getScaledInstance: " + e.getMessage());
	         e.printStackTrace();
	         return null; // Devolver null si hay un error en el escalado.
	    }
	}
}