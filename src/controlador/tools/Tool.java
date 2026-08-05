package controlador.tools;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

/**
 * Clase base abstracta para todas las herramientas del editor.
 * <p>
 * Cada herramienta recibe un {@link ToolContext} con las referencias al modelo,
 * vista y barra de opciones. Los métodos de evento vienen con implementación
 * vacía por defecto; cada herramienta sobrescribe solo los que necesita.
 */
public abstract class Tool {

    protected ToolContext ctx;

    /**
     * @param ctx contexto compartido (modelo, vista, barra)
     */
    public void setContext(ToolContext ctx) {
        this.ctx = ctx;
    } // --- Fin del metodo setContext ---

    /**
     * @return el commandKey que identifica esta herramienta (ej.
     *         {@code CMD_ADVANCED_EDITOR_TRANSFORMAR})
     */
    public abstract String getCommandKey();

    /**
     * Indica si esta herramienta modifica el contenido del documento (pinta,
     * transforma, crea capas, recorta...). Las herramientas de solo selecci\u00F3n
     * o navegaci\u00F3n devuelven {@code false}. Permite al controlador marcar el
     * documento como "sucio" cuando la herramienta completa un gesto.
     *
     * @return {@code true} si la herramienta altera el contenido
     */
    public boolean modifiesContent() {
        return false;
    } // --- Fin del metodo modifiesContent ---


    /**
     * Se ejecuta al hacer clic en el canvas.
     */
    public void mousePressed(MouseEvent e) {
    } // --- Fin del metodo mousePressed ---

    /**
     * Se ejecuta al arrastrar con el botón pulsado.
     */
    public void mouseDragged(MouseEvent e) {
    } // --- Fin del metodo mouseDragged ---

    /**
     * Se ejecuta al soltar el botón.
     */
    public void mouseReleased(MouseEvent e) {
    } // --- Fin del metodo mouseReleased ---

    /**
     * Se ejecuta al mover el ratón sin pulsar.
     */
    public void mouseMoved(MouseEvent e) {
    } // --- Fin del metodo mouseMoved ---

    /**
     * Se ejecuta al pulsar una tecla con el canvas enfocado.
     */
    public void keyPressed(KeyEvent e) {
    } // --- Fin del metodo keyPressed ---

    /**
     * Dibuja elementos temporales sobre el canvas (gizmo, preview, selección).
     * <p>
     * Se llama desde CanvasPanel.paintComponent() después de renderizar las
     * capas.
     *
     * @param g2 contexto gráfico (ya transformado a coordenadas canvas)
     */
    public void paintOverlay(Graphics2D g2) {
    } // --- Fin del metodo paintOverlay ---

    /**
     * @return panel de opciones para la barra Parte B, o null si no tiene
     */
    public JPanel getOptionsPanel() {
        return null;
    } // --- Fin del metodo getOptionsPanel ---

    /**
     * Se ejecuta cuando esta herramienta se activa (se selecciona en el combo).
     */
    public void onActivate() {
    } // --- Fin del metodo onActivate ---

    /**
     * Se ejecuta cuando esta herramienta se desactiva (se selecciona otra).
     */
    public void onDeactivate() {
    } // --- Fin del metodo onDeactivate ---

    /**
     * Cancela la operación en curso (arrastre, edición inline, etc.) cuando el
     * usuario pulsa ESC.
     * <p>
     * Debe devolver {@code true} si había una operación que cancelar; si
     * devuelve {@code false} el editor continúa con la cadena de deselección.
     *
     * @return {@code true} si se canceló algo
     */
    public boolean cancel() {
        return false;
    } // --- Fin del metodo cancel ---

    /**
     * @return cursor que debe mostrarse cuando esta herramienta está activa
     */
    public Cursor getCursor() {
        return Cursor.getDefaultCursor();
    } // --- Fin del metodo getCursor ---

} // --- Fin de la clase Tool ---
