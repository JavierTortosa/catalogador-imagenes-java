package controlador;

import java.awt.Container;

import javax.swing.JList;

/**
 * Utilidades de geometría para grids (JList con {@code HORIZONTAL_WRAP}).
 * Funciones puras: sin estado, sin dependencias de modo ni de negocio.
 */
public final class GridUtils {

    private GridUtils() {
    } // --- Fin del constructor privado ---

    /**
     * Calcula el número de columnas visibles por fila de un grid.
     * Replica la resolución de anchos hoy presente en {@code GlobalInputManager}:
     * usa el ancho del viewport visible y, si no está disponible, cae al padre
     * o al ancho del propio grid. Nunca devuelve menos de 1 columna.
     * @param gridList La JList configurada como grid.
     * @return El número de columnas por fila, como mínimo 1.
     */
    public static int calcularColumnasPorFila(JList<?> gridList) {
        int viewportWidth = gridList.getVisibleRect().width;
        int cellWidth = gridList.getFixedCellWidth();

        if (viewportWidth <= 0) {
            Container parent = gridList.getParent();
            if (parent != null) viewportWidth = parent.getWidth();
        }
        if (viewportWidth <= 0) viewportWidth = gridList.getWidth();
        if (cellWidth <= 0) cellWidth = viewportWidth;
        if (viewportWidth <= 0 || cellWidth <= 0) return 1;

        return calcularColumnasPorFila(viewportWidth, cellWidth);
    } // --- Fin del metodo calcularColumnasPorFila ---

    /**
     * Calcula el número de columnas por fila a partir del ancho del viewport
     * y del ancho de celda, nunca inferior a 1.
     * @param viewportWidth Ancho visible del grid.
     * @param cellWidth Ancho fijo de cada celda.
     * @return Las columnas por fila.
     */
    public static int calcularColumnasPorFila(int viewportWidth, int cellWidth) {
        if (viewportWidth <= 0 || cellWidth <= 0) return 1;
        return Math.max(1, viewportWidth / cellWidth);
    } // --- Fin del metodo calcularColumnasPorFila ---

    /**
     * Calcula el índice de fila (0-based) de un índice de lista dentro del grid.
     * @param index El índice de la lista.
     * @param columnas El número de columnas por fila.
     * @return La fila en la que se encuentra el índice.
     */
    public static int calcularFila(int index, int columnas) {
        return index / Math.max(1, columnas);
    } // --- Fin del metodo calcularFila ---

    /**
     * Calcula la columna (0-based) de un índice de lista dentro del grid.
     * @param index El índice de la lista.
     * @param columnas El número de columnas por fila.
     * @return La columna en la que se encuentra el índice.
     */
    public static int calcularColumna(int index, int columnas) {
        return index % Math.max(1, columnas);
    } // --- Fin del metodo calcularColumna ---

    /**
     * Calcula la primera columna (0-based) de una fila dada.
     * @param fila La fila (0-based).
     * @param columnas El número de columnas por fila.
     * @return El índice de la lista de la primera celda de esa fila.
     */
    public static int calcularIndicePrimeraColumna(int fila, int columnas) {
        return fila * Math.max(1, columnas);
    } // --- Fin del metodo calcularIndicePrimeraColumna ---

    /**
     * Calcula el índice de la fila anterior (misma columna, una fila arriba),
     * limitado al primer elemento de la lista.
     * @param index El índice actual de la lista.
     * @param columnas El número de columnas por fila.
     * @return El índice de la lista una fila antes, sin bajar de 0.
     */
    public static int calcularIndiceFilaAnterior(int index, int columnas) {
        return Math.max(0, index - Math.max(1, columnas));
    } // --- Fin del metodo calcularIndiceFilaAnterior ---

    /**
     * Calcula el índice de la fila siguiente (misma columna, una fila abajo),
     * limitado al último elemento de la lista.
     * @param index El índice actual de la lista.
     * @param columnas El número de columnas por fila.
     * @param tamanioModelo El número de elementos del modelo.
     * @return El índice de la lista una fila después, sin superar el último.
     */
    public static int calcularIndiceFilaSiguiente(int index, int columnas, int tamanioModelo) {
        return clamp(index + Math.max(1, columnas), 0, tamanioModelo - 1);
    } // --- Fin del metodo calcularIndiceFilaSiguiente ---

    /**
     * Limita un valor entre un mínimo y un máximo inclusivos.
     * @param valor El valor a limitar.
     * @param minimo El mínimo permitido.
     * @param maximo El máximo permitido.
     * @return El valor limitado al rango indicado.
     */
    public static int clamp(int valor, int minimo, int maximo) {
        return Math.max(minimo, Math.min(valor, maximo));
    } // --- Fin del metodo clamp ---

} // --- Fin de la clase GridUtils ---