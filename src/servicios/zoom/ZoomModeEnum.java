package servicios.zoom;

//public class ZoomModesEnum

public enum ZoomModeEnum {
    DISPLAY_ORIGINAL,      // zoomAutoAction
    FIT_TO_WIDTH,          // zoomAnchoAction
    FIT_TO_HEIGHT,         // zoomAltoAction
    FIT_TO_SCREEN,         // zoomFitAction
    MAINTAIN_CURRENT_ZOOM, // zoomFixedAction
    USER_SPECIFIED_PERCENTAGE // zoomFijadoAction
    // Podrías añadir: ACTUAL_PIXELS (100%)
    
/*
necesito crear una clase para los metodos de los diferentes tipos de zoom:
-zoomAutoAction = no aplica zoom. solo muestra la imagen original sin tener en cuenta si "Mantener Proporciones" esta activada o no
-zoomAnchoAction = ajusta la imagen al ancho maximo teniendo en cuenta "Mantener Proporciones"
-zoomAltoAction = ajusta la imagen al alto maximo teniendo en cuenta "Mantener Proporciones"
-zoomFitAction = ajusta la imagen al alto y ancho maximo teniendo en cuenta "Mantener Proporciones"
-zoomFixedAction  = escala las siguientes imagenes que se vean al mismo tamaño que la que estamos viendo y no tiene en cuenta "Mantener Proporciones"
-zoomFijadoAction = las imagenes se veran al tamaño de zoom que se ha especificado en el menu en la opcion "Zoom_Personalizado_%"

la opcion de "Mantener Proporciones" es para mantener o no las proporciones de las imagenes
	 
	 */
}
