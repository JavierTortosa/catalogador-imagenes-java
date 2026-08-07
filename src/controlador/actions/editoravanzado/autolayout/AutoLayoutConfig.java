package controlador.actions.editoravanzado.autolayout;

/**
 * Configuración en memoria del sistema Auto Layout.
 * <p>
 * Es un singleton con los parámetros globales que comparten todas las
 * herramientas. El panel de Opciones del editor la muta en tiempo de ejecución y
 * el motor la inyecta en el {@link AutoLayoutContext}, de modo que los
 * algoritmos jamás acceden al singleton directamente.
 */
public final class AutoLayoutConfig {

    /**
     * Modo de empaquetado del algoritmo Pack.
     */
    public enum PackMode {

        /** Estantes: ordena por altura descendente y coloca en filas. */
        SHELF,

        /** Skyline: ordena por área descendente y minimiza la altura de la silueta. */
        SKYLINE,

        /** Guillotina: divide el espacio libre en rectángulos y coloca por área. */
        GUILLOTINE
    }

    /**
     * Comportamiento de las capas visibles no seleccionadas durante una operación.
     */
    public enum NoSeleccionadasMode {

        /** Se trasladan fuera del canvas (x = ancho del lienzo + margen). */
        SACAR_FUERA,

        /** Se dejan exactamente donde están. */
        IGNORAR
    }

    private static final AutoLayoutConfig INSTANCIA = new AutoLayoutConfig();

    private int margin = 24;
    private int spacing = 20;
    private int fitMargin = 24;
    private double heroScale = 0.5;
    private double mosaicVariance = 0.15;
    private PackMode packMode = PackMode.SHELF;
    private NoSeleccionadasMode noSeleccionadas = NoSeleccionadasMode.SACAR_FUERA;

    private AutoLayoutConfig() {
    } // --- Fin del constructor AutoLayoutConfig ---


    /**
     * Devuelve la instancia única de configuración.
     *
     * @return la configuración del sistema Auto Layout
     */
    public static AutoLayoutConfig get() {
        return INSTANCIA;
    } // --- Fin del metodo get ---


    /**
     * Margen general (px) aplicado al borde del lienzo en cuadrículas y empaquetados.
     *
     * @return margen en píxeles
     */
    public int getMargin() {
        return margin;
    } // --- Fin del metodo getMargin ---


    public void setMargin(int margin) {
        this.margin = Math.max(0, margin);
    } // --- Fin del metodo setMargin ---


    /**
     * Separación (px) entre celdas o capas dentro de una composición.
     *
     * @return separación en píxeles
     */
    public int getSpacing() {
        return spacing;
    } // --- Fin del metodo getSpacing ---


    public void setSpacing(int spacing) {
        this.spacing = Math.max(0, spacing);
    } // --- Fin del metodo setSpacing ---


    /**
     * Margen (px) usado por la herramienta Ajustar al Canvas.
     *
     * @return margen de ajuste en píxeles
     */
    public int getFitMargin() {
        return fitMargin;
    } // --- Fin del metodo getFitMargin ---


    public void setFitMargin(int fitMargin) {
        this.fitMargin = Math.max(0, fitMargin);
    } // --- Fin del metodo setFitMargin ---


    /**
     * Proporción del espacio que ocupa la capa principal en el Layout Hero.
     *
     * @return proporción entre 0,2 y 0,8
     */
    public double getHeroScale() {
        return heroScale;
    } // --- Fin del metodo getHeroScale ---


    public void setHeroScale(double heroScale) {
        this.heroScale = Math.max(0.2, Math.min(0.8, heroScale));
    } // --- Fin del metodo setHeroScale ---


    /**
     * Variación máxima (tanto por uno) del tamaño de una capa en el Mosaico.
     *
     * @return variación entre 0 y 0,5
     */
    public double getMosaicVariance() {
        return mosaicVariance;
    } // --- Fin del metodo getMosaicVariance ---


    public void setMosaicVariance(double mosaicVariance) {
        this.mosaicVariance = Math.max(0.0, Math.min(0.5, mosaicVariance));
    } // --- Fin del metodo setMosaicVariance ---


    /**
     * Modo de empaquetado del algoritmo Compactar.
     *
     * @return modo de empaquetado activo
     */
    public PackMode getPackMode() {
        return packMode;
    } // --- Fin del metodo getPackMode ---


    public void setPackMode(PackMode packMode) {
        this.packMode = packMode;
    } // --- Fin del metodo setPackMode ---


    /**
     * Comportamiento de las capas visibles no seleccionadas.
     *
     * @return modo de capas no seleccionadas activo
     */
    public NoSeleccionadasMode getNoSeleccionadas() {
        return noSeleccionadas;
    } // --- Fin del metodo getNoSeleccionadas ---


    public void setNoSeleccionadas(NoSeleccionadasMode noSeleccionadas) {
        this.noSeleccionadas = noSeleccionadas;
    } // --- Fin del metodo setNoSeleccionadas ---

} // --- Fin de la clase AutoLayoutConfig ---
