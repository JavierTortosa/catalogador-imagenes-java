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

import javax.swing.SwingUtilities;

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
    private ToolContext sharedContext;

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
                selectionModel, gizmo, canvasPanel, componentBar);

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
                selectionModel, gizmo, canvasPanel, componentBar);
        for (Tool tool : toolMap.values()) {
            tool.setContext(sharedContext);
        }
    } // --- Fin del metodo setContext ---


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
     */
    public void updateCursor() {
        Cursor cursor = activeTool != null ? activeTool.getCursor()
                : Cursor.getDefaultCursor();
        canvasPanel.setCursor(cursor);
    } // --- Fin del metodo updateCursor ---


    // ======================== LISTENER INTERNO ========================


    private class MouseHandler implements MouseListener, MouseMotionListener {

        @Override
        public void mousePressed(MouseEvent e) {
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
            if (activeTool == null) return;
            Point cp = toCanvasCoords(e);
            MouseEvent canvasEvent = new MouseEvent(
                    (java.awt.Component) e.getSource(), e.getID(),
                    e.getWhen(), e.getModifiersEx(),
                    cp.x, cp.y, e.getClickCount(),
                    e.isPopupTrigger(), e.getButton());
            activeTool.mouseReleased(canvasEvent);
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
