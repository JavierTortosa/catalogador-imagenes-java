package controlador.tools;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import javax.swing.SwingUtilities;

import controlador.utils.EditorHotkeys;
import modelo.editor.CanvasModel;
import modelo.editor.LayerModel;
import modelo.editor.SelectionModel;
import modelo.gizmo.TransformGizmo;
import vista.panels.render.CanvasPanel;
import vista.panels.render.EditorComponentBar;

/**
 * Controlador que gestiona las herramientas del editor.
 * <p>
 * Instala listeners de ratón y teclado en el CanvasPanel, mantiene un registro
 * de herramientas por commandKey y delega los eventos a la herramienta activa.
 */
public class CanvasController {

    private final Map<String, Tool> toolMap;
    private Tool activeTool;
    private final CanvasPanel canvasPanel;
    private final EditorComponentBar componentBar;
    private final LayerPicker layerPicker;
    private final controlador.tools.editors.LayerEditorRegistry layerEditorRegistry;
    private ToolContext sharedContext;
    private Runnable fullscreenToggle;
    private BooleanSupplier fullscreenEscapeHandler;
    private Runnable contentChangeCallback;
    private Runnable pasteCallback;
    private boolean panning;
    private int lastPanX;
    private int lastPanY;

    /**
     * @param canvasPanel   vista del canvas
     * @param componentBar  barra superior del editor (Parte A + B)
     * @param canvasModel   modelo del lienzo
     * @param layerModel    modelo de capas
     * @param selectionModel modelo de selección
     * @param gizmo         gizmo de transformación
     */
    public CanvasController(CanvasPanel canvasPanel, EditorComponentBar componentBar,
            CanvasModel canvasModel, LayerModel layerModel,
            SelectionModel selectionModel, TransformGizmo gizmo) {
        this.toolMap = new HashMap<>();
        this.canvasPanel = Objects.requireNonNull(canvasPanel);
        this.componentBar = Objects.requireNonNull(componentBar);
        this.sharedContext = new ToolContext(canvasModel, layerModel,
                selectionModel, gizmo, canvasPanel, componentBar, null, null);
        this.layerPicker = new LayerPicker(sharedContext);
        this.layerEditorRegistry = new controlador.tools.editors.LayerEditorRegistry();
        this.sharedContext = new ToolContext(canvasModel, layerModel,
                selectionModel, gizmo, canvasPanel, componentBar,
                layerPicker, layerEditorRegistry);

        installListeners();
    } // --- Fin del constructor CanvasController ---


    private void installListeners() {
        MouseHandler handler = new MouseHandler();
        canvasPanel.addMouseListener(handler);
        canvasPanel.addMouseMotionListener(handler);
        canvasPanel.addKeyListener(new KeyHandler());
        canvasPanel.setFocusable(true);
    } // --- Fin del metodo installListeners ---


    /**
     * Convierte coordenadas del panel a coordenadas del canvas aplicando la
     * transformación inversa (zoom + desplazamiento).
     */
    private Point toCanvasCoords(MouseEvent e) {
        double zoom = canvasPanel.getZoom();
        double ox = canvasPanel.getOffsetX();
        double oy = canvasPanel.getOffsetY();
        int sx = e.getX();
        int sy = e.getY();
        int cx = (int) Math.round((sx - ox) / zoom);
        int cy = (int) Math.round((sy - oy) / zoom);
        return new Point(cx, cy);
    } // --- Fin del metodo toCanvasCoords ---


    /**
     * Inicia una operación de paneo con el ratón en la posición dada.
     *
     * @param e evento de pulsación del ratón
     */
    private void startPan(MouseEvent e) {
        panning = true;
        lastPanX = e.getX();
        lastPanY = e.getY();
        updateCursor();
    } // --- Fin del metodo startPan ---


    /**
     * Indica si el punto de pantalla cae fuera del rectángulo del lienzo
     * (zona oscura de trabajo). Con el clic izquierdo en esa zona se paneea.
     *
     * @param sx coordenada X de pantalla (relativa al panel)
     * @param sy coordenada Y de pantalla (relativa al panel)
     * @return {@code true} si el punto está fuera del lienzo visible
     */
    private boolean isOutsideCanvas(int sx, int sy) {
        CanvasModel cm = sharedContext.canvasModel();
        if (cm == null) return true;
        int cw = cm.getWidth();
        int ch = cm.getHeight();
        if (cw <= 0 || ch <= 0) return true;
        double zoom = canvasPanel.getZoom();
        double ox = canvasPanel.getOffsetX();
        double oy = canvasPanel.getOffsetY();
        return sx < ox || sy < oy || sx > ox + cw * zoom || sy > oy + ch * zoom;
    } // --- Fin del metodo isOutsideCanvas ---


    /**
     * @return la herramienta registrada con ese commandKey, o null
     */
    public Tool getTool(String commandKey) {
        return toolMap.get(commandKey);
    } // --- Fin del metodo getTool ---


    /**
     * @return el sharedContext (se reconstruye si cambian los modelos)
     */
    public ToolContext getContext() {
        return sharedContext;
    } // --- Fin del metodo getContext ---


    /**
     * Reemplaza el contexto compartido (útil si se reconstruyen los modelos).
     */
    public void setContext(CanvasModel canvasModel, LayerModel layerModel,
            SelectionModel selectionModel, TransformGizmo gizmo) {
        this.sharedContext = new ToolContext(canvasModel, layerModel,
                selectionModel, gizmo, canvasPanel, componentBar,
                layerPicker, layerEditorRegistry);
        layerPicker.setContext(sharedContext);
        for (Tool tool : toolMap.values()) {
            tool.setContext(sharedContext);
        }
    } // --- Fin del metodo setContext ---

    /**
     * @return el registro de editores de capa (para registrar adapters por tipo)
     */
    public controlador.tools.editors.LayerEditorRegistry getLayerEditorRegistry() {
        return layerEditorRegistry;
    } // --- Fin del metodo getLayerEditorRegistry ---


    /**
     * Registra una herramienta asociada a su commandKey.
     */
    public void registerTool(Tool tool) {
        Objects.requireNonNull(tool);
        tool.setContext(sharedContext);
        toolMap.put(tool.getCommandKey(), tool);
    } // --- Fin del metodo registerTool ---


    /**
     * Activa la herramienta identificada por commandKey.
     * <p>
     * Desactiva la anterior, activa la nueva, sincroniza el combo y actualiza
     * el cursor.
     *
     * @param commandKey identificador de la herramienta (null → ninguna)
     */
    public void setActiveTool(String commandKey) {
        if (panning) {
            panning = false;
        }
        if (activeTool != null) {
            activeTool.onDeactivate();
        }
        activeTool = commandKey != null ? toolMap.get(commandKey) : null;
        if (activeTool != null) {
            activeTool.onActivate();
            componentBar.selectToolByCommand(commandKey);
        }
        updateCursor();
    } // --- Fin del metodo setActiveTool ---


    /**
     * @return la herramienta activa, o null si no hay ninguna
     */
    public Tool getActiveTool() {
        return activeTool;
    } // --- Fin del metodo getActiveTool ---


    /**
     * Actualiza el cursor del CanvasPanel según la herramienta activa.
     * Durante un paneo se muestra el cursor de mover.
     */
    public void updateCursor() {
        Cursor cursor;
        if (panning) {
            cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        } else {
            cursor = activeTool != null ? activeTool.getCursor()
                    : Cursor.getDefaultCursor();
        }
        canvasPanel.setCursor(cursor);
    } // --- Fin del metodo updateCursor ---


    /**
     * Registra el callback de alternar pantalla completa (tecla F).
     *
     * @param fullscreenToggle acción a ejecutar, o null para desactivarla
     */
    public void setFullscreenToggle(Runnable fullscreenToggle) {
        this.fullscreenToggle = fullscreenToggle;
    } // --- Fin del metodo setFullscreenToggle ---


    /**
     * Registra el handler de ESC cuando el editor está en pantalla completa.
     * <p>
     * Debe devolver {@code true} si ha salido de pantalla completa (el ESC se
     * considera resuelto) o {@code false} si no estaba en fullscreen.
     *
     * @param fullscreenEscapeHandler handler de salida de fullscreen, o null
     */
    public void setFullscreenEscapeHandler(BooleanSupplier fullscreenEscapeHandler) {
        this.fullscreenEscapeHandler = fullscreenEscapeHandler;
    } // --- Fin del metodo setFullscreenEscapeHandler ---


    /**
     * Registra el callback que se invoca cuando una herramienta que modifica
     * contenido completa un gesto (pintar, transformar, crear capa, recortar).
     * Permite al {@code EditorDocumentManager} marcar el documento como sucio.
     *
     * @param contentChangeCallback callback de contenido modificado, o null
     */
    public void setContentChangeCallback(Runnable contentChangeCallback) {
        this.contentChangeCallback = contentChangeCallback;
    } // --- Fin del metodo setContentChangeCallback ---


    /**
     * Registra el callback de pegado (Ctrl+V) para el editor.
     *
     * @param pasteCallback callback de pegado, o null
     */
    public void setPasteCallback(Runnable pasteCallback) {
        this.pasteCallback = pasteCallback;
    } // --- Fin del metodo setPasteCallback ---


    /**
     * Notifica al documento que el contenido ha cambiado (llamado por las
     * herramientas modificadoras al completar un gesto).
     */
    public void notifyContentChanged() {
        if (contentChangeCallback != null) {
            contentChangeCallback.run();
        }
    } // --- Fin del metodo notifyContentChanged ---


    /**
     * Procesa la tecla ESC: primero se intenta salir de pantalla completa, si
     * no aplica se cancela la operación en curso de la herramienta activa y, si
     * tampoco había nada que cancelar, se deselecciona la capa activa.
     */
    public void handleEscape() {
        boolean handled = false;
        if (fullscreenEscapeHandler != null) {
            handled = fullscreenEscapeHandler.getAsBoolean();
        }
        if (!handled && activeTool != null) {
            handled = activeTool.cancel();
        }
        if (!handled) {
            if (sharedContext.selectionModel() != null) {
                sharedContext.selectionModel().clear();
            }
            layerPicker.clearSelection();
        }
        canvasPanel.repaint();
    } // --- Fin del metodo handleEscape ---


    // ======================== LISTENER INTERNO ========================


    private class MouseHandler implements MouseListener, MouseMotionListener {

        @Override
        public void mousePressed(MouseEvent e) {
            // Paneo: con el botón central siempre; con el izquierdo solo si el
            // clic cae fuera del lienzo (zona oscura, "el canvas como fondo").
            if (e.getButton() == MouseEvent.BUTTON2
                    || (e.getButton() == MouseEvent.BUTTON1 && isOutsideCanvas(e.getX(), e.getY()))) {
                startPan(e);
                return;
            }
            if (activeTool == null) return;
            canvasPanel.requestFocusInWindow();
            Point cp = toCanvasCoords(e);
            MouseEvent canvasEvent = new MouseEvent(
                    (java.awt.Component) e.getSource(), e.getID(),
                    e.getWhen(), e.getModifiersEx(),
                    cp.x, cp.y, e.getClickCount(),
                    e.isPopupTrigger(), e.getButton());
            activeTool.mousePressed(canvasEvent);
            canvasPanel.repaint();
        } // --- Fin del metodo mousePressed ---

        @Override
        public void mouseDragged(MouseEvent e) {
            if (panning) {
                int dx = e.getX() - lastPanX;
                int dy = e.getY() - lastPanY;
                lastPanX = e.getX();
                lastPanY = e.getY();
                canvasPanel.setPan(canvasPanel.getOffsetX() + dx,
                        canvasPanel.getOffsetY() + dy);
                return;
            }
            if (activeTool == null) return;
            Point cp = toCanvasCoords(e);
            MouseEvent canvasEvent = new MouseEvent(
                    (java.awt.Component) e.getSource(), e.getID(),
                    e.getWhen(), e.getModifiersEx(),
                    cp.x, cp.y, e.getClickCount(),
                    e.isPopupTrigger(), e.getButton());
            activeTool.mouseDragged(canvasEvent);
            canvasPanel.repaint();
        } // --- Fin del metodo mouseDragged ---

        @Override
        public void mouseReleased(MouseEvent e) {
            if (panning && (e.getButton() == MouseEvent.BUTTON1
                    || e.getButton() == MouseEvent.BUTTON2)) {
                panning = false;
                updateCursor();
                return;
            }
            if (activeTool == null) return;
            Point cp = toCanvasCoords(e);
            MouseEvent canvasEvent = new MouseEvent(
                    (java.awt.Component) e.getSource(), e.getID(),
                    e.getWhen(), e.getModifiersEx(),
                    cp.x, cp.y, e.getClickCount(),
                    e.isPopupTrigger(), e.getButton());
            activeTool.mouseReleased(canvasEvent);
            if (activeTool.modifiesContent()) {
                notifyContentChanged();
            }
            canvasPanel.repaint();
        } // --- Fin del metodo mouseReleased ---

        @Override
        public void mouseMoved(MouseEvent e) {
            if (activeTool == null) return;
            Point cp = toCanvasCoords(e);
            MouseEvent canvasEvent = new MouseEvent(
                    (java.awt.Component) e.getSource(), e.getID(),
                    e.getWhen(), e.getModifiersEx(),
                    cp.x, cp.y, e.getClickCount(),
                    e.isPopupTrigger(), e.getButton());
            activeTool.mouseMoved(canvasEvent);
            updateCursor();
        } // --- Fin del metodo mouseMoved ---

        @Override
        public void mouseClicked(MouseEvent e) { }

        @Override
        public void mouseEntered(MouseEvent e) { }

        @Override
        public void mouseExited(MouseEvent e) { }

    } // --- Fin de la clase MouseHandler ---


    private class KeyHandler implements KeyListener {

        @Override
        public void keyPressed(KeyEvent e) {
            int modifiers = e.getModifiersEx();
            boolean clean = (modifiers & (KeyEvent.CTRL_DOWN_MASK
                    | KeyEvent.ALT_DOWN_MASK | KeyEvent.META_DOWN_MASK)) == 0;
            boolean noShift = (modifiers & KeyEvent.SHIFT_DOWN_MASK) == 0;

            if (clean && e.getKeyCode() == KeyEvent.VK_F) {
                if (fullscreenToggle != null) {
                    fullscreenToggle.run();
                }
                e.consume();
                return;
            }
            if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0
                    && e.getKeyCode() == KeyEvent.VK_V) {
                if (pasteCallback != null) {
                    pasteCallback.run();
                }
                e.consume();
                return;
            }
            if (clean && noShift) {
                String command = EditorHotkeys.commandForKeyCode(e.getKeyCode());
                if (command != null) {
                    setActiveTool(command);
                    e.consume();
                    return;
                }
            }
            if (activeTool != null) {
                activeTool.keyPressed(e);
            }
        } // --- Fin del metodo keyPressed ---

        @Override
        public void keyReleased(KeyEvent e) { }

        @Override
        public void keyTyped(KeyEvent e) { }

    } // --- Fin de la clase KeyHandler ---

} // --- Fin de la clase CanvasController ---
