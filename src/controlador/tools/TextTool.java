package controlador.tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import controlador.commands.AppActionCommands;
import modelo.editor.Layer;
import modelo.editor.TextLayer;
import modelo.gizmo.TransformGizmo;
import modelo.gizmo.TransformGizmo.Handle;
import vista.panels.render.CanvasPanel;

/**
 * Herramienta de texto con edici\u00F3n inline sobre el canvas.
 * <p>
 * Arrastrar crea un rect\u00E1ngulo donde aparece un JTextField para escribir.
 * Doble clic sobre una TextLayer existente permite re-editar el texto.
 * Las propiedades (fuente, tama\u00F1o, color, estilo) se leen del panel de
 * opciones y se aplican tanto al crear como al modificar.
 * <p>
 * Cuando hay una TextLayer seleccionada, se dibuja el TransformGizmo alrededor
 * de sus bounds, permitiendo mover y redimensionar la capa sin cambiar de
 * herramienta.
 */
public class TextTool extends Tool {

    // --- estado del drag para nuevo texto ---
    private Point dragStart;
    private Rectangle dragRect;
    private boolean dragging;

    // --- edici\u00F3n inline ---
    private JTextComponent inlineField;
    private TextLayer editingLayer;   // non-null durante edici\u00F3n inline
    private Rectangle creationRect;   // rect\u00E1ngulo en coords canvas para la nueva capa

    // --- gizmo (transformaci\u00F3n) ---
    private TextLayer selectedLayer;   // capa que muestra el gizmo
    private boolean gizmoDragActive;
    private Point gizmoDragStart;

    // --- auto-size (single click) ---
    private boolean autoSize;
    private Point autoSizeClickPoint;

    // true cuando TextTool se activa desde EditTool (doble clic)
    private boolean returnToEditOnEmptyClick;

    // --- propiedades del texto actual ---
    private String fontFamily = "SansSerif";
    private int fontSize = 24;
    private boolean bold;
    private boolean italic;
    private Color textColor = Color.BLACK;
    private int alignment = SwingConstants.LEFT;
    private boolean vertical;

    private static final float DASH[] = { 4f, 4f };


    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_TEXTO;
    } // --- Fin del metodo getCommandKey ---


    @Override
    public void onActivate() {
        returnToEditOnEmptyClick = false;
        dragStart = null;
        dragRect = null;
        dragging = false;
        gizmoDragActive = false;
        gizmoDragStart = null;
        autoSize = false;
        autoSizeClickPoint = null;

        // Recuperar la TextLayer activa del modelo, si la hay
        Layer active = ctx.layerModel().getActiveLayer();
        selectedLayer = active instanceof TextLayer tl ? tl : null;
    } // --- Fin del metodo onActivate ---


    @Override
    public void onDeactivate() {
        returnToEditOnEmptyClick = false;
        clearSelection();
    } // --- Fin del metodo onDeactivate ---


    // ==================== Construir Font ====================


    private Font buildFont() {
        int style = Font.PLAIN;
        if (bold)   style |= Font.BOLD;
        if (italic) style |= Font.ITALIC;
        return new Font(fontFamily, style, fontSize);
    } // --- Fin del metodo buildFont ---


    // ==================== Setters para el panel de opciones ====================


    public void setFontFamily(String ff) {
        this.fontFamily = ff;
        TextLayer target = editingLayer != null ? editingLayer : selectedLayer;
        if (target != null) {
            target.setFont(buildFont());
            ctx.canvasPanel().repaint();
        }
    } // --- Fin del metodo setFontFamily ---


    public void setFontSize(int size) {
        this.fontSize = size;
        TextLayer target = editingLayer != null ? editingLayer : selectedLayer;
        if (target != null) {
            target.setFont(buildFont());
            ctx.canvasPanel().repaint();
        }
        updateInlineFieldFont();
    } // --- Fin del metodo setFontSize ---


    public void setBold(boolean b) {
        this.bold = b;
        TextLayer target = editingLayer != null ? editingLayer : selectedLayer;
        if (target != null) {
            target.setFont(buildFont());
            ctx.canvasPanel().repaint();
        }
        updateInlineFieldFont();
    } // --- Fin del metodo setBold ---


    public void setItalic(boolean i) {
        this.italic = i;
        TextLayer target = editingLayer != null ? editingLayer : selectedLayer;
        if (target != null) {
            target.setFont(buildFont());
            ctx.canvasPanel().repaint();
        }
        updateInlineFieldFont();
    } // --- Fin del metodo setItalic ---


    public void setTextColor(Color c) {
        this.textColor = c;
        if (inlineField != null) {
            inlineField.setForeground(c);
            inlineField.setCaretColor(c);
        }
        TextLayer target = editingLayer != null ? editingLayer : selectedLayer;
        if (target != null) {
            target.setColor(c);
            ctx.canvasPanel().repaint();
        }
    } // --- Fin del metodo setTextColor ---


    public void setAlignment(int align) {
        this.alignment = align;
        TextLayer target = editingLayer != null ? editingLayer : selectedLayer;
        if (target != null) {
            target.setAlignment(align);
            ctx.canvasPanel().repaint();
        }
    } // --- Fin del metodo setAlignment ---


    private boolean underline;
    private boolean strikethrough;
    private boolean flowColumns;


    public void setUnderline(boolean u) {
        this.underline = u;
        TextLayer target = editingLayer != null ? editingLayer : selectedLayer;
        if (target != null) {
            target.setUnderline(u);
            ctx.canvasPanel().repaint();
        }
    } // --- Fin del metodo setUnderline ---


    public void setStrikethrough(boolean s) {
        this.strikethrough = s;
        TextLayer target = editingLayer != null ? editingLayer : selectedLayer;
        if (target != null) {
            target.setStrikethrough(s);
            ctx.canvasPanel().repaint();
        }
    } // --- Fin del metodo setStrikethrough ---


    public void setFlowColumns(boolean fc) {
        this.flowColumns = fc;
        TextLayer target = editingLayer != null ? editingLayer : selectedLayer;
        if (target != null) {
            target.setFlowColumns(fc);
            ctx.canvasPanel().repaint();
        }
    } // --- Fin del metodo setFlowColumns ---


    // ==================== Lectura para sincronización ====================


    public String getFontFamily()    { return fontFamily; }
    public int getFontSize()         { return fontSize; }
    public boolean isBold()          { return bold; }
    public boolean isItalic()        { return italic; }
    public Color getTextColor()      { return textColor; }
    public int getAlignment()        { return alignment; }
    public boolean isVertical()      { return vertical; }
    public boolean isUnderline()     { return underline; }
    public boolean isStrikethrough() { return strikethrough; }
    public boolean isFlowColumns()   { return flowColumns; }


    // ==================== Eventos de rat\u00F3n ====================


    @Override
    public void mousePressed(MouseEvent e) {
        // Si hay edición inline activa, confirmarla antes de procesar el clic
        if (inlineField != null) {
            commitInlineText();
            if (returnToEditOnEmptyClick) {
                returnToEditOnEmptyClick = false;
                switchToEditTool();
                return;
            }
            // Fall through: el mismo clic continúa (seleccionar otra capa o crear nuevo texto)
        }

        Point p = e.getPoint();

        // 1 — Gizmo sobre la capa seleccionada (arrastrar tirador)
        if (selectedLayer != null && selectedLayer.getBounds() != null
                && selectedLayer.getBounds().contains(p)) {

            // 1a — Doble clic → editar inline
            if (e.getClickCount() >= 2) {
                beginInlineEdit(selectedLayer);
                return;
            }

            // 1b — Gizmo hit test
            Handle h = ctx.gizmo().hitTest(p, selectedLayer.getBounds());
            if (h != Handle.NONE && h != Handle.ROTATE) {
                ctx.gizmo().startDrag(h, new Rectangle(selectedLayer.getBounds()), null);
                gizmoDragActive = true;
                gizmoDragStart = p;
                return;
            }

            // 1c — Clic en el cuerpo (Handle.MOVE se captura arriba); no hacer nada
            return;
        }

        // 2 — Clic en una TextLayer diferente → seleccionarla
        TextLayer clicked = findTextLayerAt(p);
        if (clicked != null) {
            selectLayer(clicked);
            return;
        }

        // 3 — Clic en vacío: volver a EditTool si vine desde allí, o crear nuevo texto
        if (returnToEditOnEmptyClick) {
            returnToEditOnEmptyClick = false;
            clearSelection();
            switchToEditTool();
            return;
        }
        clearSelection();
        dragStart = p;
    } // --- Fin del metodo mousePressed ---


    @Override
    public void mouseDragged(MouseEvent e) {
        // Gizmo drag (transformar capa seleccionada)
        if (gizmoDragActive && selectedLayer != null && gizmoDragStart != null) {
            int dx = e.getX() - gizmoDragStart.x;
            int dy = e.getY() - gizmoDragStart.y;
            Rectangle newBounds = ctx.gizmo().drag(dx, dy);
            if (newBounds != null) {
                selectedLayer.setBounds(newBounds);
                ctx.canvasPanel().repaint();
            }
            // No actualizamos gizmoDragStart para que dx/dy sean acumulativos
            // desde el punto de inicio del drag (as\u00ED lo maneja drag())
            return;
        }

        // Drag para crear nuevo texto
        if (dragStart == null) return;
        if (!dragging) {
            dragging = true;
            dragRect = new Rectangle(dragStart.x, dragStart.y, 0, 0);
        }
        int x = Math.min(dragStart.x, e.getX());
        int y = Math.min(dragStart.y, e.getY());
        int w = Math.abs(e.getX() - dragStart.x);
        int h = Math.abs(e.getY() - dragStart.y);
        dragRect = new Rectangle(x, y, Math.max(w, 10), Math.max(h, 10));
    } // --- Fin del metodo mouseDragged ---


    @Override
    public void mouseReleased(MouseEvent e) {
        // Fin de gizmo drag
        if (gizmoDragActive) {
            ctx.gizmo().endDrag();
            gizmoDragActive = false;
            gizmoDragStart = null;
            // La capa se actualiz\u00F3 en mouseDragged
            updateActiveLayerInModel();
            return;
        }

        // Creación de nuevo texto
        if (dragStart == null) return;

        if (!dragging) {
            // Single click → auto-size (las dimensiones se calculan del texto)
            autoSize = true;
            autoSizeClickPoint = new Point(dragStart);
            Rectangle fieldRect = new Rectangle(dragStart.x, dragStart.y, 200, 30);
            showInlineFieldAt(fieldRect, null);
            dragStart = null;
            return;
        }

        // Drag → dimensiones explícitas
        dragging = false;

        if (dragStart.distance(e.getPoint()) < 5) {
            dragStart = null;
            dragRect = null;
            return;
        }

        if (dragRect.width < 10 || dragRect.height < 10) {
            dragStart = null;
            dragRect = null;
            return;
        }

        creationRect = new Rectangle(dragRect);
        showInlineFieldAt(dragRect, null);
        dragStart = null;
        dragRect = null;
    } // --- Fin del metodo mouseReleased ---


    @Override
    public void mouseMoved(MouseEvent e) {
        // Cambiar cursor seg\u00FAn el tirador del gizmo
        if (selectedLayer != null && selectedLayer.getBounds() != null
                && selectedLayer.getBounds().contains(e.getPoint())) {
            Handle h = ctx.gizmo().hitTest(e.getPoint(), selectedLayer.getBounds());
            if (h != Handle.NONE) {
                ctx.canvasPanel().setCursor(ctx.gizmo().getCursor(h));
                return;
            }
        }
        ctx.canvasPanel().setCursor(getCursor());
    } // --- Fin del metodo mouseMoved ---


    @Override
    public void paintOverlay(Graphics2D g2) {
        // Gizmo alrededor de la capa seleccionada (solo si no hay edici\u00F3n inline activa)
        if (inlineField == null && selectedLayer != null && selectedLayer.getBounds() != null) {
            ctx.gizmo().draw(g2, selectedLayer.getBounds());
        }

        // Rect\u00E1ngulo de arrastre para nuevo texto
        if (dragging && dragRect != null) {
            Stroke orig = g2.getStroke();
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL, 0, DASH, 0));
            g2.setColor(new Color(0, 120, 215));
            g2.draw(dragRect);
            g2.setStroke(orig);
        }
    } // --- Fin del metodo paintOverlay ---


    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
    } // --- Fin del metodo getCursor ---


    // ==================== Gesti\u00F3n de selecci\u00F3n ====================


    private void selectLayer(TextLayer layer) {
        ctx.layerModel().setActiveLayer(layer);
        syncSettingsFromLayer(layer);
        syncToComponentBar();
        removeInlineField();
        editingLayer = null;
        creationRect = null;
        selectedLayer = layer;
    } // --- Fin del metodo selectLayer ---


    private void clearSelection() {
        removeInlineField();
        editingLayer = null;
        creationRect = null;
        selectedLayer = null;
        autoSize = false;
        autoSizeClickPoint = null;
    } // --- Fin del metodo clearSelection ---


    /**
     * Sincroniza el selectedLayer como capa activa del modelo por si el gizmo
     * cambi\u00F3 sus bounds y la vista necesita conocer la selecci\u00F3n actual.
     */
    private void updateActiveLayerInModel() {
        if (selectedLayer != null) {
            ctx.layerModel().setActiveLayer(selectedLayer);
        }
    } // --- Fin del metodo updateActiveLayerInModel ---


    // ==================== Buscar TextLayer bajo el cursor ====================


    private TextLayer findTextLayerAt(Point p) {
        java.util.List<Layer> layers = ctx.layerModel().getLayers();
        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer l = layers.get(i);
            if (l instanceof TextLayer tl && l.isVisible() && l.getBounds() != null
                    && l.getBounds().contains(p)) {
                return tl;
            }
        }
        return null;
    } // --- Fin del metodo findTextLayerAt ---


    // ==================== Editor inline (JTextField sobre el canvas) ====================


    private void showInlineFieldAt(Rectangle canvasRect, String existingText) {
        removeInlineField();

        boolean paragraph = editingLayer != null ? !editingLayer.isAutoSize()
                : creationRect != null || !autoSize;

        CanvasPanel panel = ctx.canvasPanel();
        double zoom = panel.getZoom();
        double ox = panel.getOffsetX();
        double oy = panel.getOffsetY();

        int px = (int) Math.round(canvasRect.x * zoom + ox);
        int py = (int) Math.round(canvasRect.y * zoom + oy);
        int pw = (int) Math.round(canvasRect.width * zoom);
        int ph = Math.max((int) Math.round(canvasRect.height * zoom), 24);

        if (paragraph) {
            JTextArea area = new JTextArea();
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setBounds(px, py, Math.max(pw, 50), Math.max(ph, 60));
            area.setFont(buildFont().deriveFont(buildFont().getSize2D() * (float) zoom));
            area.setForeground(textColor);
            area.setCaretColor(textColor);
            area.setBackground(new Color(255, 255, 255, 200));
            area.setBorder(BorderFactory.createLineBorder(new Color(0, 120, 215)));
            area.setOpaque(false);

            if (existingText != null) {
                area.setText(existingText);
                area.selectAll();
            }

            area.getInputMap().put(KeyStroke.getKeyStroke("control ENTER"), "commitInline");
            area.getActionMap().put("commitInline", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    commitInlineText();
                }
            });

            area.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    SwingUtilities.invokeLater(() -> {
                        if (inlineField != null && !inlineField.hasFocus()) {
                            commitInlineText();
                        }
                    });
                }
            });

            inlineField = area;
        } else {
            JTextField field = new JTextField();
            field.setBounds(px, py, Math.max(pw, 50), ph);
            field.setFont(buildFont().deriveFont(buildFont().getSize2D() * (float) zoom));
            field.setForeground(textColor);
            field.setCaretColor(textColor);
            field.setBackground(new Color(255, 255, 255, 200));
            field.setBorder(BorderFactory.createLineBorder(new Color(0, 120, 215)));
            field.setOpaque(false);

            if (existingText != null) {
                field.setText(existingText);
                field.selectAll();
            }

            field.addActionListener(e -> commitInlineText());

            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    SwingUtilities.invokeLater(() -> {
                        if (inlineField != null && !inlineField.hasFocus()) {
                            commitInlineText();
                        }
                    });
                }
            });

            inlineField = field;
        }

        if (editingLayer != null) {
            editingLayer.editingInline = true;
        }

        panel.setLayout(null);
        panel.add(inlineField);
        inlineField.requestFocusInWindow();
        panel.repaint();
    } // --- Fin del metodo showInlineFieldAt ---


    private void beginInlineEdit(TextLayer layer) {
        removeInlineField();
        editingLayer = layer;
        selectedLayer = layer;
        syncSettingsFromLayer(layer);
        syncToComponentBar();
        showInlineFieldAt(layer.getBounds(), layer.getText());
    } // --- Fin del metodo beginInlineEdit ---


    /**
     * Activa la edici\u00F3n inline sobre una capa de texto existente.
     * Llamado desde EditTool al hacer doble clic.
     */
    public void editExistingLayer(TextLayer layer) {
        removeInlineField();
        editingLayer = layer;
        selectedLayer = layer;
        returnToEditOnEmptyClick = true;
        syncSettingsFromLayer(layer);
        syncToComponentBar();
        showInlineFieldAt(layer.getBounds(), layer.getText());
    } // --- Fin del metodo editExistingLayer ---


    private void syncSettingsFromLayer(TextLayer layer) {
        fontFamily    = layer.getFont().getFamily();
        fontSize      = layer.getFont().getSize();
        bold          = layer.getFont().isBold();
        italic        = layer.getFont().isItalic();
        textColor     = layer.getColor();
        alignment     = layer.getAlignment();
        vertical      = layer.isVertical();
        underline     = layer.isUnderline();
        strikethrough = layer.isStrikethrough();
        flowColumns   = layer.isFlowColumns();
        autoSize      = layer.isAutoSize();
    } // --- Fin del metodo syncSettingsFromLayer ---


    public void cancelInlineEdit() {
        if (inlineField == null) return;
        removeInlineField();
        editingLayer = null;
        creationRect = null;
        autoSize = false;
        autoSizeClickPoint = null;
        dragStart = null;
        ctx.canvasPanel().repaint();
    } // --- Fin del metodo cancelInlineEdit ---


    public void commitInlineText() {
        if (inlineField == null) return;
        String text = inlineField.getText();
        if (text == null || text.isBlank()) {
            removeInlineField();
            editingLayer = null;
            ctx.canvasPanel().repaint();
            return;
        }

        if (editingLayer != null) {
            editingLayer.setText(text);
            editingLayer.setFont(buildFont());
            editingLayer.setColor(textColor);
            editingLayer.setAlignment(alignment);
            editingLayer.setVertical(vertical);
            editingLayer.setUnderline(underline);
            editingLayer.setStrikethrough(strikethrough);
            editingLayer.setFlowColumns(flowColumns);
            selectedLayer = editingLayer;
        } else if (autoSize && autoSizeClickPoint != null) {
            // Auto-size (single click): dimensiones se ajustan al texto
            String name = text.length() > 30 ? text.substring(0, 30) + "..." : text;
            TextLayer layer = new TextLayer(name, text, buildFont(), textColor,
                    new Rectangle(autoSizeClickPoint.x, autoSizeClickPoint.y, 1, 1));
            layer.setAlignment(alignment);
            layer.setVertical(vertical);
            layer.setUnderline(underline);
            layer.setStrikethrough(strikethrough);
            layer.setFlowColumns(flowColumns);
            layer.setAutoSize(true);
            recalcAutoBounds(layer);
            ctx.layerModel().addLayer(layer);
            ctx.layerModel().setActiveLayer(layer);
            selectedLayer = layer;
            autoSize = false;
            autoSizeClickPoint = null;
        } else if (creationRect != null) {
            String name = text.length() > 30 ? text.substring(0, 30) + "..." : text;
            TextLayer layer = new TextLayer(name, text, buildFont(), textColor,
                    new Rectangle(creationRect));
            layer.setAlignment(alignment);
            layer.setVertical(vertical);
            layer.setUnderline(underline);
            layer.setStrikethrough(strikethrough);
            layer.setFlowColumns(flowColumns);
            ctx.layerModel().addLayer(layer);
            ctx.layerModel().setActiveLayer(layer);
            selectedLayer = layer;
            creationRect = null;
        }

        removeInlineField();
        editingLayer = null;
        ctx.canvasPanel().repaint();

        if (returnToEditOnEmptyClick) {
            returnToEditOnEmptyClick = false;
            switchToEditTool();
        }
    } // --- Fin del metodo commitInlineText ---


    /**
     * Recalcula los bounds de la capa para que se ajusten al texto,
     * usando el punto de clic como referencia según el alineado.
     */
    private void recalcAutoBounds(TextLayer layer) {
        if (autoSizeClickPoint == null) return;
        String txt = layer.getText();
        if (txt == null || txt.isBlank()) return;

        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tmp.createGraphics();
        try {
            g.setFont(layer.getFont());
            FontMetrics fm = g.getFontMetrics();
            String[] lines = txt.split("\n", -1);
            int[] lw = new int[lines.length];
            for (int i = 0; i < lines.length; i++) {
                lw[i] = fm.stringWidth(lines[i]);
            }
            int firstLineWidth = lw.length > 0 ? lw[0] : 0;
            int maxWidth = 0;
            for (int w : lw) maxWidth = Math.max(maxWidth, w);
            int lineH = fm.getHeight();
            int spacingPx = Math.round(lineH * (layer.getLineSpacing() - 1.0f));
            int totalH = lines.length * lineH + (lines.length - 1) * spacingPx;
            int pad = 4;

            int x = switch (layer.getAlignment()) {
                case TextLayer.ALIGN_CENTER -> autoSizeClickPoint.x - firstLineWidth / 2 - pad;
                case TextLayer.ALIGN_RIGHT  -> autoSizeClickPoint.x - firstLineWidth - pad * 2;
                default -> autoSizeClickPoint.x; // LEFT
            };
            int y = autoSizeClickPoint.y - totalH / 2;
            layer.setBounds(new Rectangle(x, y, maxWidth + pad * 2, totalH));
        } finally {
            g.dispose();
        }
    } // --- Fin del metodo recalcAutoBounds ---


    private void removeInlineField() {
        if (editingLayer != null) {
            editingLayer.editingInline = false;
        }
        if (inlineField != null) {
            Container parent = inlineField.getParent();
            if (parent != null) {
                parent.remove(inlineField);
                parent.repaint();
            }
            inlineField = null;
        }
    } // --- Fin del metodo removeInlineField ---


    private void updateInlineFieldFont() {
        if (inlineField != null) {
            double zoom = ctx.canvasPanel().getZoom();
            inlineField.setFont(buildFont().deriveFont(buildFont().getSize2D() * (float) zoom));
        }
    } // --- Fin del metodo updateInlineFieldFont ---


    // ==================== Sincronizaci\u00F3n con el panel de opciones ====================


    public void syncFromComponentBar() {
        var bar = ctx.componentBar();
        if (bar == null) return;
        String ff = bar.getTextFontFamily();
        if (ff != null) fontFamily = ff;
        int fs = bar.getTextFontSize();
        if (fs > 0) fontSize = fs;
        Boolean b = bar.isTextBold();
        if (b != null) bold = b;
        Boolean i = bar.isTextItalic();
        if (i != null) italic = i;
        Color c = bar.getTextColor();
        if (c != null) textColor = c;
        int a = bar.getTextAlignment();
        if (a >= 0) alignment = a;
        Boolean v = bar.isTextVertical();
        if (v != null) vertical = v;
        Boolean u = bar.isTextUnderline();
        if (u != null) underline = u;
        Boolean s = bar.isTextStrikethrough();
        if (s != null) strikethrough = s;
        Boolean fc = bar.isTextFlowColumns();
        if (fc != null) flowColumns = fc;
    } // --- Fin del metodo syncFromComponentBar ---


    public void syncToComponentBar() {
        var bar = ctx.componentBar();
        if (bar == null) return;
        bar.setTextFontFamily(fontFamily);
        bar.setTextFontSize(fontSize);
        bar.setTextBold(bold);
        bar.setTextItalic(italic);
        bar.setTextColor(textColor);
        bar.setTextAlignment(alignment);
        bar.setTextVertical(vertical);
        bar.setTextUnderline(underline);
        bar.setTextStrikethrough(strikethrough);
        bar.setTextFlowColumns(flowColumns);
    } // --- Fin del metodo syncToComponentBar ---


    /**
     * Cambia a la herramienta de edici\u00F3n universal (EditTool).
     * Llamado al hacer clic fuera de toda capa o tras confirmar texto inline.
     */
    private void switchToEditTool() {
        var cc = ctx.componentBar();
        if (cc == null) return;
        cc.getCanvasController().setActiveTool(AppActionCommands.CMD_ADVANCED_EDITOR_EDICION);
    } // --- Fin del metodo switchToEditTool ---

} // --- Fin de la clase TextTool ---
