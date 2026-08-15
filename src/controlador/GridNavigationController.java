package controlador;

import java.util.Objects;

import javax.swing.JList;

/**
 * Controlador de la física de navegación de un grid (JList con
 * {@code HORIZONTAL_WRAP}). Mueve la selección de la JList; el modo
 * correspondiente reacciona al cambio de selección mediante sus listeners.
 * No conoce modos, no toca contextos ni ejecuta acciones de negocio.
 */
public class GridNavigationController {

    private final JList<?> gridList;
    private boolean circular;
    private int pageScrollIncrement;

    /**
     * Constructor del controlador de navegación de un grid.
     * @param gridList La JList configurada como grid sobre la que se navega.
     */
    public GridNavigationController(JList<?> gridList) {
        this.gridList = Objects.requireNonNull(gridList, "gridList no puede ser null");
        this.circular = false;
        this.pageScrollIncrement = 10;
    } // --- Fin del constructor ---

    /**
     * Devuelve la JList sobre la que navega este controlador.
     * @return La JList del grid.
     */
    public JList<?> getGridList() {
        return gridList;
    } // --- Fin del metodo getGridList ---

    /**
     * Establece si la navegación debe ser circular (envuelve al principio/final).
     * @param circular {@code true} para activar la navegación circular.
     */
    public void setCircular(boolean circular) {
        this.circular = circular;
    } // --- Fin del metodo setCircular ---

    /**
     * Establece el incremento de salto de bloque para las páginas (PageUp/PageDown).
     * @param pageScrollIncrement Número de elementos por salto de bloque.
     */
    public void setPageScrollIncrement(int pageScrollIncrement) {
        this.pageScrollIncrement = Math.max(1, pageScrollIncrement);
    } // --- Fin del metodo setPageScrollIncrement ---

    /**
     * Devuelve el índice actualmente seleccionado en la JList.
     * @return El índice seleccionado, o -1 si no hay selección.
     */
    public int getSelectedIndex() {
        return gridList.getSelectedIndex();
    } // --- Fin del metodo getSelectedIndex ---

    /**
     * Devuelve el número de elementos del modelo de la JList.
     * @return El tamaño del modelo.
     */
    public int getModelSize() {
        return gridList.getModel().getSize();
    } // --- Fin del metodo getModelSize ---

    /**
     * Devuelve el número de columnas visibles por fila del grid.
     * @return Las columnas por fila (como mínimo 1).
     */
    public int getColumnasPorFila() {
        return GridUtils.calcularColumnasPorFila(gridList);
    } // --- Fin del metodo getColumnasPorFila ---

    /**
     * Selecciona el índice indicado y lo hace visible. Método principal:
     * los listeners del modo reaccionan al cambio de selección.
     * @param index El índice destino.
     */
    public void moveTo(int index) {
        if (index < 0 || index >= getModelSize()) return;
        gridList.setSelectedIndex(index);
        gridList.ensureIndexIsVisible(index);
    } // --- Fin del metodo moveTo ---

    /**
     * Mueve la selección un delta de elementos (positivo = siguiente, negativo = anterior),
     * aplicando circularidad o limitación de bordes según configuración.
     * @param delta El número de posiciones a avanzar (negativo para retroceder).
     */
    public void moveDelta(int delta) {
        int size = getModelSize();
        if (size == 0) return;
        int base = gridList.getSelectedIndex();
        moveTo(calcularIndiceEnRango(base + delta, size));
    } // --- Fin del metodo moveDelta ---

    /**
     * Mueve la selección un número de filas (positivo = siguiente, negativo = anterior),
     * conservando la columna. Usado por la rueda y las flechas arriba/abajo.
     * @param deltaFilas El número de filas a desplazar.
     */
    public void moveRow(int deltaFilas) {
        int size = getModelSize();
        if (size == 0) return;
        int base = gridList.getSelectedIndex();
        int columnas = getColumnasPorFila();
        moveTo(calcularIndiceEnRango(base + deltaFilas * columnas, size));
    } // --- Fin del metodo moveRow ---

    /**
     * Selecciona el elemento siguiente (columna siguiente; en la última columna de una
     * fila, el primero de la siguiente fila).
     */
    public void next() {
        moveDelta(1);
    } // --- Fin del metodo next ---

    /**
     * Selecciona el elemento anterior (columna anterior; en la primera columna de una
     * fila, el último de la fila anterior).
     */
    public void previous() {
        moveDelta(-1);
    } // --- Fin del metodo previous ---

    /**
     * Mueve la selección a la misma columna de la fila anterior (o el primero si ya
     * se está en la primera fila sin navegación circular).
     */
    public void previousRow() {
        int size = getModelSize();
        if (size == 0) return;
        int base = Math.max(0, gridList.getSelectedIndex());
        int columnas = getColumnasPorFila();
        if (circular && base < columnas) {
            int filas = calcularFilas(size, columnas);
            int ultimaFila = Math.max(0, filas - 1);
            int columna = GridUtils.calcularColumna(base, columnas);
            int destino = GridUtils.calcularIndicePrimeraColumna(ultimaFila, columnas) + columna;
            moveTo(Math.min(destino, size - 1));
        } else {
            moveTo(GridUtils.calcularIndiceFilaAnterior(base, columnas));
        }
    } // --- Fin del metodo previousRow ---

    /**
     * Mueve la selección a la misma columna de la fila siguiente (o el último si ya se
     * está en la última fila sin navegación circular).
     */
    public void nextRow() {
        int size = getModelSize();
        if (size == 0) return;
        int base = Math.max(0, gridList.getSelectedIndex());
        int columnas = getColumnasPorFila();
        if (circular) {
            int filaActual = GridUtils.calcularFila(base, columnas);
            int filas = calcularFilas(size, columnas);
            if (filaActual >= filas - 1) {
                int columna = GridUtils.calcularColumna(base, columnas);
                moveTo(Math.min(columna, size - 1));
            } else {
                moveTo(calcularIndiceEnRango(base + columnas, size));
            }
        } else {
            moveTo(GridUtils.calcularIndiceFilaSiguiente(base, columnas, size));
        }
    } // --- Fin del metodo nextRow ---

    /**
     * Selecciona el primer elemento del grid.
     */
    public void first() {
        if (getModelSize() > 0) moveTo(0);
    } // --- Fin del metodo first ---

    /**
     * Selecciona el último elemento del grid.
     */
    public void last() {
        if (getModelSize() > 0) moveTo(getModelSize() - 1);
    } // --- Fin del metodo last ---

    /**
     * Avanza un bloque (PageDown) según el salto de bloque configurado.
     */
    public void nextPage() {
        int size = getModelSize();
        if (size == 0) return;
        int base = Math.max(0, gridList.getSelectedIndex());
        moveTo(Math.min(base + pageScrollIncrement, size - 1));
    } // --- Fin del metodo nextPage ---

    /**
     * Retrocede un bloque (PageUp) según el salto de bloque configurado.
     */
    public void previousPage() {
        if (getModelSize() == 0) return;
        int base = Math.max(0, gridList.getSelectedIndex());
        moveTo(Math.max(0, base - pageScrollIncrement));
    } // --- Fin del metodo previousPage ---

    /**
     * Calcula el número de filas completas o parciales del grid.
     * @param size El tamaño del modelo.
     * @param columnas Las columnas por fila.
     * @return El número de filas.
     */
    private int calcularFilas(int size, int columnas) {
        return (size + columnas - 1) / columnas;
    } // --- Fin del metodo calcularFilas ---

    /**
     * Aplica la circularidad o el límite de bordes a un índice candidato.
     * @param candidato El índice solicitado.
     * @param size El tamaño del modelo.
     * @return El índice final dentro de rango.
     */
    private int calcularIndiceEnRango(int candidato, int size) {
        if (circular) {
            if (candidato < 0) return size - 1;
            if (candidato >= size) return 0;
            return candidato;
        }
        return GridUtils.clamp(candidato, 0, size - 1);
    } // --- Fin del metodo calcularIndiceEnRango ---

} // --- Fin de la clase GridNavigationController ---