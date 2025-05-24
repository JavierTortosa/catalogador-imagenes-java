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
	    if (etiquetaAncho <= 0 || etiquetaAlto <= 0) return null;
	    int imgOriginalAncho = imagenOriginal.getWidth();
	    int imgOriginalAlto = imagenOriginal.getHeight();
	    if (imgOriginalAncho <= 0 || imgOriginalAlto <= 0) return null;

	    // --- SECCIÓN 2: OBTENER ESTADO DEL MODELO ---
	    boolean mantenerProporcionGlobal = model.isMantenerProporcion();
	    ZoomModeEnum currentMode = model.getCurrentZoomMode();

	    // --- SECCIÓN 3: DECIDIR ESCALADO BASE ---
	    int anchoFinalBase;
	    int altoFinalBase;

	    // Para DISPLAY_ORIGINAL, MAINTAIN_CURRENT_ZOOM, USER_SPECIFIED_PERCENTAGE,
	    // el "escalado base" es la imagen original. El factor del ZoomManager se aplicará a esto.
	    if (currentMode == ZoomModeEnum.DISPLAY_ORIGINAL ||
	        currentMode == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM ||
	        currentMode == ZoomModeEnum.USER_SPECIFIED_PERCENTAGE) {
	        // System.out.println("  [ImageDisplayUtils] Modo " + currentMode + ": Devolviendo original como base.");
	        return imagenOriginal;
	    }

	    // Para FIT_TO_WIDTH, FIT_TO_HEIGHT, FIT_TO_SCREEN, preparamos una imagen base
	    // que ya está ajustada o estirada según el toggle 'mantenerProporcionGlobal'.
	    // El ZoomManager luego aplicará un factor (probablemente 1.0 para estos si este método hace el trabajo).
	    if (mantenerProporcionGlobal) { // Si el toggle está ON
	        // Ajustar para que quepa manteniendo proporción ("contain")
	        double ratioImg = (double) imgOriginalAncho / imgOriginalAlto;
	        double ratioEtiqueta = (double) etiquetaAncho / etiquetaAlto;
	        if (ratioEtiqueta > ratioImg) { // Etiqueta más ancha que imagen (o igual alto)
	            altoFinalBase = etiquetaAlto;
	            anchoFinalBase = (int) (etiquetaAlto * ratioImg);
	        } else { // Etiqueta más alta que imagen (o igual ancho)
	            anchoFinalBase = etiquetaAncho;
	            altoFinalBase = (int) (etiquetaAncho / ratioImg);
	        }
	    } else { // Si el toggle está OFF
	        // Estirar para llenar la etiqueta
	        anchoFinalBase = etiquetaAncho;
	        altoFinalBase = etiquetaAlto;
	    }

	    anchoFinalBase = Math.max(1, anchoFinalBase);
	    altoFinalBase = Math.max(1, altoFinalBase);

	    try {
	        // System.out.println("  [ImageDisplayUtils] Escalando base a: " + anchoFinalBase + "x" + altoFinalBase);
	        return imagenOriginal.getScaledInstance(anchoFinalBase, altoFinalBase, Image.SCALE_SMOOTH);
	    } catch (Exception e) { 
	         System.err.println("ERROR [ImageDisplayUtils.reescalarImagenParaAjustar] durante getScaledInstance: " + e.getMessage());
	         e.printStackTrace();
	         return null; // Devolver null si hay un error en el escalado.
	    }
	}
}