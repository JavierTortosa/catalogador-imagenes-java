package servicios.zoom;

//public class ZoomModesEnum

public enum ZoomModeEnum {
//    DISPLAY_ORIGINAL,      // zoomAutoAction
//    FIT_TO_WIDTH,          // zoomAnchoAction
//    FIT_TO_SCREEN,         // zoomFitAction
	FIT_TO_SCREEN("Ajustar a Pantalla"),
	DISPLAY_ORIGINAL("Tamaño Real (100%)"), 
    FIT_TO_HEIGHT("Ajustar a Alto"),         // zoomAltoAction
    FIT_TO_WIDTH("Ajustar a Ancho"),
    MAINTAIN_CURRENT_ZOOM("Mantener Zoom Actual"), // zoomFixedAction
    USER_SPECIFIED_PERCENTAGE("Porcentaje Personalizado"); // zoomFijadoAction
	// ... otros modos
	// Podrías añadir: ACTUAL_PIXELS (100%)

    private final String nombreLegible;

    ZoomModeEnum(String nombreLegible) {
        this.nombreLegible = nombreLegible;
    }

    public String getNombreLegible() {
        return nombreLegible;
    }
    // Opcional: Sobrescribir toString() si quieres que siempre devuelva esto
    @Override public String toString() { return nombreLegible; }
    
}
