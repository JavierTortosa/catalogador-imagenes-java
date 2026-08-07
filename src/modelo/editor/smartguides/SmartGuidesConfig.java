package modelo.editor.smartguides;

import java.awt.Color;

/**
 * Configuración en memoria del sistema Smart Guides.
 * <p>
 * Es un singleton con los parámetros globales que lee la herramienta de
 * edición durante el arrastre de capas. El panel de Configuración Avanzada y el
 * {@code ConfigApplicationManager} la sincronizan desde las claves persistentes
 * de {@code ConfigKeys}.
 */
public final class SmartGuidesConfig {

    private static final SmartGuidesConfig INSTANCIA = new SmartGuidesConfig();

    private boolean showGuides = true;
    private boolean snapToCanvas = true;
    private boolean snapToLayers = true;
    private int snapDistance = 6;
    private int stickyDistance = 3;
    private Color guideColor = new Color(0xFF00FF);
    private float guideStrokeWidth = 1.0f;

    private SmartGuidesConfig() {
    } // --- Fin del constructor SmartGuidesConfig ---


    /**
     * Devuelve la instancia única de configuración.
     *
     * @return la configuración del sistema Smart Guides
     */
    public static SmartGuidesConfig get() {
        return INSTANCIA;
    } // --- Fin del metodo get ---


    /**
     * Indica si las Smart Guides están activas durante el arrastre.
     *
     * @return true si hay que calcular y dibujar guías
     */
    public boolean isShowGuides() {
        return showGuides;
    } // --- Fin del metodo isShowGuides ---


    public void setShowGuides(boolean showGuides) {
        this.showGuides = showGuides;
    } // --- Fin del metodo setShowGuides ---


    /**
     * Indica si el arrastre se ajusta a los bordes y centro del lienzo.
     *
     * @return true si se ajusta al lienzo
     */
    public boolean isSnapToCanvas() {
        return snapToCanvas;
    } // --- Fin del metodo isSnapToCanvas ---


    public void setSnapToCanvas(boolean snapToCanvas) {
        this.snapToCanvas = snapToCanvas;
    } // --- Fin del metodo setSnapToCanvas ---


    /**
     * Indica si el arrastre se ajusta a los bordes y centros de otras capas.
     *
     * @return true si se ajusta a otras capas
     */
    public boolean isSnapToLayers() {
        return snapToLayers;
    } // --- Fin del metodo isSnapToLayers ---


    public void setSnapToLayers(boolean snapToLayers) {
        this.snapToLayers = snapToLayers;
    } // --- Fin del metodo setSnapToLayers ---


    /**
     * Distancia máxima (px) a la que se activa el ajuste.
     *
     * @return distancia de ajuste en píxeles
     */
    public int getSnapDistance() {
        return snapDistance;
    } // --- Fin del metodo getSnapDistance ---


    public void setSnapDistance(int snapDistance) {
        this.snapDistance = Math.max(0, snapDistance);
    } // --- Fin del metodo setSnapDistance ---


    /**
     * Distancia de histéresis (px): una vez ajustado, la guía se mantiene
     * mientras el cursor no se aleje más de este valor.
     *
     * @return distancia de retención en píxeles
     */
    public int getStickyDistance() {
        return stickyDistance;
    } // --- Fin del metodo getStickyDistance ---


    public void setStickyDistance(int stickyDistance) {
        this.stickyDistance = Math.max(0, stickyDistance);
    } // --- Fin del metodo setStickyDistance ---


    /**
     * Color con el que se dibujan las guías temporales.
     *
     * @return color de las guías
     */
    public Color getGuideColor() {
        return guideColor;
    } // --- Fin del metodo getGuideColor ---


    public void setGuideColor(Color guideColor) {
        this.guideColor = guideColor;
    } // --- Fin del metodo setGuideColor ---


    /**
     * Grosor de trazo (px) de las líneas guía.
     *
     * @return grosor de trazo
     */
    public float getGuideStrokeWidth() {
        return guideStrokeWidth;
    } // --- Fin del metodo getGuideStrokeWidth ---


    public void setGuideStrokeWidth(float guideStrokeWidth) {
        this.guideStrokeWidth = Math.max(0.25f, guideStrokeWidth);
    } // --- Fin del metodo setGuideStrokeWidth ---

} // --- Fin de la clase SmartGuidesConfig ---
