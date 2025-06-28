package modelo;

import servicios.zoom.ZoomModeEnum;

/**
 * Encapsula el estado completo del zoom para un "contexto" o "modo" de la aplicación,
 * como el Visualizador, el modo Proyecto o el modo Datos.
 */
public class ZoomContext {
    private double zoomFactor = 1.0;
    private ZoomModeEnum zoomMode = ZoomModeEnum.FIT_TO_SCREEN;
    private int imageOffsetX = 0;
    private int imageOffsetY = 0;
    private boolean zoomHabilitado = false; // El permiso para paneo/zoom manual
    private boolean mantenerProporcion = false;
    
    // --- Getters y Setters ---
    public double getZoomFactor() { return zoomFactor; }
    public void setZoomFactor(double zoomFactor) { this.zoomFactor = zoomFactor; }
    
    public ZoomModeEnum getZoomMode() { return zoomMode; }
    public void setZoomMode(ZoomModeEnum zoomMode) { this.zoomMode = zoomMode; }

    public int getImageOffsetX() { return imageOffsetX; }
    public void setImageOffsetX(int imageOffsetX) { this.imageOffsetX = imageOffsetX; }

    public int getImageOffsetY() { return imageOffsetY; }
    public void setImageOffsetY(int imageOffsetY) { this.imageOffsetY = imageOffsetY; }

    public boolean isZoomHabilitado() { return zoomHabilitado; }
    public void setZoomHabilitado(boolean zoomHabilitado) { this.zoomHabilitado = zoomHabilitado; }
    
    public boolean isMantenerProporcion() { return mantenerProporcion; } 
    public void setMantenerProporcion(boolean mantener) { this.mantenerProporcion = mantener; }
    
    public void resetPan() {
        this.imageOffsetX = 0;
        this.imageOffsetY = 0;
    }
    
} // --- Fin de la clase ZoomContext ---