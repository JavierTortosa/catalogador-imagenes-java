// En vista.util.ImageDisplayUtils.java
package vista.util;

import java.awt.Image;
import java.awt.image.BufferedImage;

import modelo.VisorModel;
import servicios.zoom.ZoomModeEnum;
import vista.VisorView;

public class ImageDisplayUtils {

	public static Image reescalarImagenParaAjustar(BufferedImage imagenOriginal, VisorModel model, VisorView view) {
	    if (imagenOriginal == null) {
	        // System.err.println("[ImageDisplayUtils.reescalarImagenParaAjustar] ERROR: La imagen original es nula.");
	        return null;
	    }
	    // Ya no necesita model ni view si solo devuelve la original.
	    // System.out.println("  [ImageDisplayUtils (S3)] Devolviendo imagen original sin escalado base.");
	    return imagenOriginal;
	}	

//	public static Image reescalarImagenParaAjustar(BufferedImage imagenOriginal, VisorModel model, VisorView view) {
//	    // --- SECCIÓN 1: VALIDACIONES DE ENTRADA ---
//	    // 1.1. Validar que los objetos principales no sean nulos.
//	    if (imagenOriginal == null) {
//	        System.err.println("[ImageDisplayUtils.reescalarImagenParaAjustar] ERROR: La imagen original es nula.");
//	        return null;
//	    }
//	    if (model == null) {
//	        System.err.println("[ImageDisplayUtils.reescalarImagenParaAjustar] ERROR: VisorModel es nulo.");
//	        return null;
//	    }
//	    if (view == null || view.getEtiquetaImagen() == null) {
//	        System.err.println("[ImageDisplayUtils.reescalarImagenParaAjustar] ERROR: VisorView o su EtiquetaImagen son nulos.");
//	        return null;
//	    }
//
//	    // 1.2. Obtener las dimensiones del área de destino (el JLabel donde se mostrará la imagen).
//	    int etiquetaAncho = view.getEtiquetaImagen().getWidth();
//	    int etiquetaAlto = view.getEtiquetaImagen().getHeight();
//
//	    // 1.3. Validar que el área de destino tenga dimensiones válidas.
//	    //      Si el JLabel aún no tiene tamaño (ej. al inicio, antes de ser renderizado), no se puede escalar.
//	    if (etiquetaAncho <= 0 || etiquetaAlto <= 0) {
//	        System.out.println("[ImageDisplayUtils.reescalarImagenParaAjustar] WARN: EtiquetaImagen sin tamaño válido aún (" + etiquetaAncho + "x" + etiquetaAlto + "). No se puede reescalar.");
//	        return null; // O devolver la imagen original si se prefiere no hacer nada.
//	    }
//
//	    // 1.4. Obtener las dimensiones de la imagen original.
//	    int imgOriginalAncho = imagenOriginal.getWidth();
//	    int imgOriginalAlto = imagenOriginal.getHeight();
//
//	    // 1.5. Validar que la imagen original tenga dimensiones válidas.
//	    if (imgOriginalAncho <= 0 || imgOriginalAlto <= 0) {
//	        System.err.println("[ImageDisplayUtils.reescalarImagenParaAjustar] ERROR: Imagen original con dimensiones inválidas ("+imgOriginalAncho+"x"+imgOriginalAlto+").");
//	        return null;
//	    }
//
//	    // --- SECCIÓN 2: OBTENER CONFIGURACIÓN DEL MODELO ---
//	    // 2.1. Obtener el estado del toggle "Mantener Proporciones" (o "Encajar Totalmente") del modelo.
//	    boolean mantenerProporcionToggleGlobal = model.isMantenerProporcion();
//	    // 2.2. Obtener el modo de zoom actual del modelo.
//	    ZoomModeEnum modoZoomActualDelModelo = model.getCurrentZoomMode();
//
//	    // --- SECCIÓN 3: CASO ESPECIAL PARA DISPLAY_ORIGINAL CON TOGGLE OFF ---
//	    // 3.1. Si el modo de zoom es DISPLAY_ORIGINAL y el toggle "Mantener Proporciones" está APAGADO,
//	    //      se devuelve la imagen original sin ningún escalado base. El ZoomManager ya habrá
//	    //      establecido el zoomFactor del modelo a 1.0, por lo que se pintará pixel por pixel,
//	    //      permitiendo desbordes.
//	    if (modoZoomActualDelModelo == ZoomModeEnum.DISPLAY_ORIGINAL && !mantenerProporcionToggleGlobal) {
//	        // System.out.println("  [ImageDisplayUtils] Modo DISPLAY_ORIGINAL y MantenerProporciones=OFF: Devolviendo imagen original sin escalado base.");
//	        return imagenOriginal; // Devuelve la BufferedImage original tal cual.
//	    }
//
//	    // --- SECCIÓN 4: CÁLCULO DE DIMENSIONES FINALES PARA LA IMAGEN BASE ---
//	    // 4.1. Variables para las dimensiones finales de la imagen base.
//	    int anchoFinalImagenBase;
//	    int altoFinalImagenBase;
//
//	    // 4.2. Decidir cómo escalar la imagen base según el toggle "Mantener Proporciones".
//	    if (mantenerProporcionToggleGlobal) {
//	        // 4.2.1. Si "Mantener Proporciones" está ON: Escalar para que quepa completamente, manteniendo la proporción.
//	        //         Esta es la lógica "contain" o "letterbox/pillarbox".
//	        double ratioImagenOriginal = (double) imgOriginalAncho / imgOriginalAlto;
//	        double ratioAreaDestino = (double) etiquetaAncho / etiquetaAlto;
//
//	        if (ratioAreaDestino > ratioImagenOriginal) {
//	            // El área de destino es proporcionalmente más ancha que la imagen.
//	            // Ajustar al alto del área de destino y calcular el ancho proporcionalmente.
//	            altoFinalImagenBase = etiquetaAlto;
//	            anchoFinalImagenBase = (int) (etiquetaAlto * ratioImagenOriginal);
//	        } else {
//	            // El área de destino es proporcionalmente más alta (o igual) que la imagen.
//	            // Ajustar al ancho del área de destino y calcular el alto proporcionalmente.
//	            anchoFinalImagenBase = etiquetaAncho;
//	            altoFinalImagenBase = (int) (etiquetaAncho / ratioImagenOriginal);
//	        }
//	        // System.out.println("  [ImageDisplayUtils] MantenerProporciones=ON. Escalado base proporcional a: " + anchoFinalImagenBase + "x" + altoFinalImagenBase);
//
//	    } else {
//	        // 4.2.2. Si "Mantener Proporciones" está OFF (y no es DISPLAY_ORIGINAL): Estirar para llenar el área.
//	        //         Esto solo debería aplicarse si el modo de zoom final (ej. FIT_TO_SCREEN con toggle OFF)
//	        //         requiere un estiramiento. Los modos FIT_TO_WIDTH/HEIGHT con toggle OFF
//	        //         mantendrán su proporción intrínseca a través del zoomFactor del ZoomManager.
//	        anchoFinalImagenBase = etiquetaAncho;
//	        altoFinalImagenBase = etiquetaAlto;
//	        // System.out.println("  [ImageDisplayUtils] MantenerProporciones=OFF. Escalado base (estirado) a: " + anchoFinalImagenBase + "x" + altoFinalImagenBase);
//	    }
//
//	    // 4.3. Asegurar que las dimensiones calculadas no sean menores que 1.
//	    anchoFinalImagenBase = Math.max(1, anchoFinalImagenBase);
//	    altoFinalImagenBase = Math.max(1, altoFinalImagenBase);
//
//	    // --- SECCIÓN 5: REALIZAR EL ESCALADO Y DEVOLVER LA IMAGEN BASE ---
//	    // 5.1. Usar getScaledInstance para obtener una versión escalada de la imagen original.
//	    //      Image.SCALE_SMOOTH prioriza la calidad del escalado.
//	    
//	    try {
//	        return imagenOriginal.getScaledInstance(anchoFinalImagenBase, altoFinalImagenBase, Image.SCALE_SMOOTH);
//	    
////	    try {
////	        Image imagenBaseEscalada = imagenOriginal.getScaledInstance(anchoFinalImagenBase, altoFinalImagenBase, Image.SCALE_SMOOTH);
////	        if (imagenBaseEscalada == null) {
////	            System.err.println("ERROR [ImageDisplayUtils.reescalarImagenParaAjustar]: getScaledInstance devolvió null.");
////	            return null;
////	        }
////	        return imagenBaseEscalada;
//	    } catch (Exception e) {
//	         System.err.println("ERROR [ImageDisplayUtils.reescalarImagenParaAjustar] durante getScaledInstance: " + e.getMessage());
//	         e.printStackTrace();
//	         return null; // Devolver null si hay un error en el escalado.
//	    }
//	}
}