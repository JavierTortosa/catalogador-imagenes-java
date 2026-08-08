package modelo.editor;

import java.awt.Rectangle;
import java.util.BitSet;

/**
 * Selección de píxeles del editor.
 * <p>
 * Mantiene un rectángulo delimitador en coordenadas de canvas y, opcionalmente,
 * una <strong>máscara de píxeles</strong> (Bitmap) que representa la forma
 * irregular real de la selección. Si no hay máscara, la selección es el
 * rectángulo completo (comportamiento clásico de la marquesa).
 * <p>
 * La máscara se guarda como un {@link BitSet} en orden row-major relativo al
 * rectángulo delimitador (tamaño {@code width*height}); el bit {@code 1}
 * significa píxel seleccionado.
 */
public class SelectionModel {

    private int x;
    private int y;
    private int width;
    private int height;
    private int feather;
    private boolean active;
    private BitSet mask;

    public SelectionModel() {
        this.active = false;
        this.feather = 0;
        this.mask = null;
    } // --- Fin del constructor SelectionModel ---

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    } // --- Fin del metodo getBounds ---

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.mask = null;
        this.active = (width > 0 && height > 0);
    } // --- Fin del metodo setBounds ---

    public void setBounds(Rectangle rect) {
        if (rect != null) {
            setBounds(rect.x, rect.y, rect.width, rect.height);
        }
    } // --- Fin del metodo setBounds ---

    public int getX() {
        return x;
    } // --- Fin del metodo getX ---

    public int getY() {
        return y;
    } // --- Fin del metodo getY ---

    public int getWidth() {
        return width;
    } // --- Fin del metodo getWidth ---

    public int getHeight() {
        return height;
    } // --- Fin del metodo getHeight ---

    public boolean isActive() {
        return active;
    } // --- Fin del metodo isActive ---

    public int getFeather() {
        return feather;
    } // --- Fin del metodo getFeather ---

    public void setFeather(int feather) {
        this.feather = Math.max(0, feather);
    } // --- Fin del metodo setFeather ---

    /**
     * @return {@code true} si la selección tiene forma irregular (máscara)
     */
    public boolean hasMask() {
        return mask != null;
    } // --- Fin del metodo hasMask ---

    /**
     * @return la máscara de píxeles actual, o null si la selección es
     *         puramente rectangular
     */
    public BitSet getMask() {
        return mask;
    } // --- Fin del metodo getMask ---

    /**
     * Establece una máscara de píxeles para la selección actual. La máscara
     * debe tener tamaño {@code width*height} en orden row-major y se interpreta
     * relativa a los bounds actuales.
     *
     * @param mask máscara de píxeles seleccionados
     */
    public void setMask(BitSet mask) {
        this.mask = mask;
        this.active = mask != null && !mask.isEmpty();
    } // --- Fin del metodo setMask ---

    /**
     * Construye la máscara a partir de un predicado sobre coordenadas de
     * canvas. Utiliza el rectángulo delimitador actual y lo afina al contenido
     * real (recalcula min/max sobre los píxeles seleccionados).
     *
     * @param selector predicado {@code (canvasX, canvasY) -> seleccionado}
     */
    public void setMaskFromPredicate(PixelSelector selector) {
        int minX = width, minY = height, maxX = -1, maxY = -1;
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                if (selector.test(x + px, y + py)) {
                    if (px < minX) minX = px;
                    if (px > maxX) maxX = px;
                    if (py < minY) minY = py;
                    if (py > maxY) maxY = py;
                }
            }
        }
        if (maxX < 0) {
            clear();
            return;
        }

        int newW = maxX - minX + 1;
        int newH = maxY - minY + 1;
        BitSet newMask = new BitSet(newW * newH);
        for (int py = minY; py <= maxY; py++) {
            for (int px = minX; px <= maxX; px++) {
                if (selector.test(x + px, y + py)) {
                    newMask.set((py - minY) * newW + (px - minX));
                }
            }
        }
        this.x += minX;
        this.y += minY;
        this.width = newW;
        this.height = newH;
        this.mask = newMask;
        this.active = true;
    } // --- Fin del metodo setMaskFromPredicate ---

    public boolean contains(int px, int py) {
        if (!active) return false;
        if (px < x || py < y || px >= x + width || py >= y + height) return false;
        if (mask == null) return true;
        return mask.get((py - y) * width + (px - x));
    } // --- Fin del metodo contains ---

    public boolean intersects(Rectangle rect) {
        return active && rect != null && getBounds().intersects(rect);
    } // --- Fin del metodo intersects ---

    public void clear() {
        this.x = 0;
        this.y = 0;
        this.width = 0;
        this.height = 0;
        this.mask = null;
        this.active = false;
    } // --- Fin del metodo clear ---

    /**
     * Sustituye la selección por la unión de la actual con el rectángulo dado.
     * Si ambas son rectangulares, el resultado es la unión de rectángulos; si
     * la actual tiene máscara, se fusiona píxel a píxel.
     *
     * @param rect rectángulo (canvas) a añadir a la selección
     */
    public void union(Rectangle rect) {
        if (rect == null || rect.isEmpty()) return;
        if (!active) {
            setBounds(rect);
            return;
        }
        Rectangle union = getBounds().union(rect);
        BitSet newMask = buildRectMask(union);
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                if (contains(x + px, y + py)) {
                    newMask.set((y + py - union.y) * union.width + (x + px - union.x));
                }
            }
        }
        applyMask(union, newMask);
    } // --- Fin del metodo union ---

    /**
     * Sustituye la selección por la intersección de la actual con el
     * rectángulo dado. Si no hay intersección, se deselecciona.
     *
     * @param rect rectángulo (canvas) a intersecar con la selección
     */
    public void intersect(Rectangle rect) {
        if (rect == null || rect.isEmpty() || !active) {
            clear();
            return;
        }
        Rectangle inter = getBounds().intersection(rect);
        if (inter.isEmpty()) {
            clear();
            return;
        }
        BitSet newMask = new BitSet(inter.width * inter.height);
        boolean any = false;
        for (int py = 0; py < inter.height; py++) {
            for (int px = 0; px < inter.width; px++) {
                if (contains(inter.x + px, inter.y + py)) {
                    newMask.set(py * inter.width + px);
                    any = true;
                }
            }
        }
        if (!any) {
            clear();
            return;
        }
        applyMask(inter, newMask);
    } // --- Fin del metodo intersect ---

    /**
     * Sustituye la selección por la diferencia de la actual menos el
     * rectángulo dado (resta).
     *
     * @param rect rectángulo (canvas) a quitar de la selección
     */
    public void subtract(Rectangle rect) {
        if (rect == null || rect.isEmpty() || !active) return;
        BitSet newMask = new BitSet(width * height);
        boolean any = false;
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int cx = x + px;
                int cy = y + py;
                if (contains(cx, cy) && !rect.contains(cx, cy)) {
                    newMask.set(py * width + px);
                    any = true;
                }
            }
        }
        if (!any) {
            clear();
            return;
        }
        applyMask(getBounds(), newMask);
    } // --- Fin del metodo subtract ---

    /**
     * Invierte la selección dentro de los bounds dados (normalmente todo el
     * lienzo): lo que estaba seleccionado deja de estarlo y viceversa. El
     * resultado siempre es una máscara.
     *
     * @param canvas bounds del área a considerar (habitualmente el lienzo)
     */
    public void invert(Rectangle canvas) {
        if (canvas == null || canvas.isEmpty()) return;
        BitSet newMask = new BitSet(canvas.width * canvas.height);
        boolean any = false;
        for (int py = 0; py < canvas.height; py++) {
            for (int px = 0; px < canvas.width; px++) {
                if (!contains(canvas.x + px, canvas.y + py)) {
                    newMask.set(py * canvas.width + px);
                    any = true;
                }
            }
        }
        if (!any) {
            clear();
            return;
        }
        applyMask(canvas, newMask);
    } // --- Fin del metodo invert ---

    /**
     * Transforma la selección a un nuevo rectángulo delimitador re-muestreando
     * la máscara (para mover/escalar con el gizmo sobre el target "marco").
     * Si la selección es rectangular pura, se reemplaza directamente.
     *
     * @param newBounds nuevo rectángulo delimitador
     */
    public void transformBounds(Rectangle newBounds) {
        if (newBounds == null || newBounds.isEmpty()) {
            clear();
            return;
        }
        if (mask == null) {
            setBounds(newBounds);
            return;
        }
        int nw = newBounds.width;
        int nh = newBounds.height;
        BitSet newMask = new BitSet(nw * nh);
        boolean any = false;
        for (int py = 0; py < nh; py++) {
            int sy = y + (int) ((long) py * height / nh);
            for (int px = 0; px < nw; px++) {
                int sx = x + (int) ((long) px * width / nw);
                if (contains(sx, sy)) {
                    newMask.set(py * nw + px);
                    any = true;
                }
            }
        }
        this.x = newBounds.x;
        this.y = newBounds.y;
        this.width = nw;
        this.height = nh;
        this.mask = newMask;
        this.active = any;
    } // --- Fin del metodo transformBounds ---

    /**
     * Sustituye la selección por la unión de la actual con una máscara dada
     * (definida sobre su propio rectángulo delimitador).
     *
     * @param otherMask máscara a fusionar (row-major sobre {@code otherRect})
     * @param otherRect rectángulo delimitador de {@code otherMask}
     */
    public void unionMask(BitSet otherMask, Rectangle otherRect) {
        if (otherMask == null || otherRect == null || otherRect.isEmpty()) return;
        if (!active) {
            applyMask(otherRect, otherMask);
            return;
        }
        Rectangle union = getBounds().union(otherRect);
        BitSet newMask = new BitSet(union.width * union.height);
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                if (contains(x + px, y + py)) {
                    newMask.set((y + py - union.y) * union.width + (x + px - union.x));
                }
            }
        }
        for (int i = otherMask.nextSetBit(0); i >= 0; i = otherMask.nextSetBit(i + 1)) {
            if (i >= otherRect.width * otherRect.height) break;
            int px = i % otherRect.width;
            int py = i / otherRect.width;
            newMask.set((otherRect.y + py - union.y) * union.width + (otherRect.x + px - union.x));
        }
        applyMask(union, newMask);
    } // --- Fin del metodo unionMask ---

    /**
     * Convierte el rectángulo dado en una máscara completamente llena.
     */
    private BitSet buildRectMask(Rectangle rect) {
        BitSet bits = new BitSet(rect.width * rect.height);
        bits.set(0, rect.width * rect.height);
        return bits;
    } // --- Fin del metodo buildRectMask ---

    /**
     * Aplica un rectángulo y su máscara como estado de la selección.
     */
    private void applyMask(Rectangle rect, BitSet newMask) {
        this.x = rect.x;
        this.y = rect.y;
        this.width = rect.width;
        this.height = rect.height;
        this.mask = newMask;
        this.active = true;
    } // --- Fin del metodo applyMask ---

    /**
     * Predicado de píxel usado para construir una máscara desde un criterio
     * externo (varita, contenido opaco de capa, etc.).
     */
    @FunctionalInterface
    public interface PixelSelector {

        /**
         * Indica si el píxel de canvas está seleccionado.
         *
         * @param canvasX coordenada X de canvas
         * @param canvasY coordenada Y de canvas
         * @return {@code true} si el píxel pertenece a la selección
         */
        boolean test(int canvasX, int canvasY);
    } // --- Fin de la interfaz PixelSelector ---

} // --- Fin de la clase SelectionModel ---
