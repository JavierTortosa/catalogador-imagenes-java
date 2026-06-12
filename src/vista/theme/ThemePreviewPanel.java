package vista.theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JPanel;

public class ThemePreviewPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final Map<String, Color> colors = new HashMap<>();
    private Consumer<String> onZoneClicked;
    private String hoveredKey;
    private final Map<Rectangle2D.Float, String> zones = new LinkedHashMap<>();

    public ThemePreviewPanel() {
        setPreferredSize(new Dimension(600, 480));
        setBackground(new Color(45, 45, 48));

        MouseAdapter handler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (onZoneClicked == null) return;
                String key = findZoneAt(e.getX(), e.getY());
                if (key != null) onZoneClicked.accept(key);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                String key = findZoneAt(e.getX(), e.getY());
                setCursor(key != null
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());
                String prev = hoveredKey;
                hoveredKey = key;
                if ((prev == null) != (key == null) || (prev != null && !prev.equals(key))) {
                    repaint();
                }
            }
        };
        addMouseListener(handler);
        addMouseMotionListener(handler);
    }

    private String findZoneAt(int mx, int my) {
        float rx = (float) mx / getWidth();
        float ry = (float) my / getHeight();
        String found = null;
        for (Map.Entry<Rectangle2D.Float, String> entry : zones.entrySet()) {
            if (entry.getKey().contains(rx, ry)) found = entry.getValue();
        }
        return found;
    }

    public void setColors(Map<String, Color> newColors) {
        this.colors.putAll(newColors);
        repaint();
    }

    public void setOnZoneClicked(Consumer<String> listener) {
        this.onZoneClicked = listener;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        zones.clear();

        int w = getWidth();
        int h = getHeight();

        int vx = 20, vy = 20, vw = w - 40, vh = h - 40;
        int gap = 2;
        int lineH = Math.max(14, (int)(vh * 0.045f));

        // --- 1. MENU BAR ---
        drawZone(g2, "MENU", vx, vy, vw, lineH, "MenuBar.background", "MenuBar.foreground");

        // --- 2. TOOLBAR ---
        int toolbarY = vy + lineH + gap;
        int toolbarH = lineH * 2;
        drawZone(g2, "HERRAMIENTAS", vx, toolbarY, vw, toolbarH, "ToolBar.background", "Button.foreground");
        g2.setColor(getColor("Button.foreground", Color.BLACK));
        for (int i = 0; i < 6; i++) {
            g2.drawRoundRect(vx + 10 + i * 25, toolbarY + 6, 18, 18, 3, 3);
        }
        g2.setColor(getColor("Button.foreground", Color.BLACK));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
        g2.drawString("BOTONES", vx + 10, toolbarY + toolbarH - 4);

        // --- 3. UPPER STATUS BAR ---
        int infoY = toolbarY + toolbarH + gap;
        drawZone(g2, "BARRA ESTADO", vx, infoY, vw, lineH, "Visor.statusBarBackground", "Visor.statusBarForeground");

        int tbW = Math.max(80, (int)(vw * 0.40f));
        drawInteractiveRect(g2, vx + 2, infoY + 2, tbW, lineH - 4,
            "TextField.background", "TextField.foreground", "Ruta:");
        g2.setColor(getColor("Component.accentColor", Color.CYAN));
        g2.drawRect(vx + 2, infoY + 2, tbW, lineH - 4);

        int labelX = vx + tbW + gap;
        int labelW = vw - tbW - gap;
        g2.setColor(getColor("Visor.statusBarForeground", Color.WHITE));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        String infoText = "Zoom: 100%";
        FontMetrics fm = g2.getFontMetrics();
        int tx = labelX + (labelW - fm.stringWidth(infoText)) / 2;
        g2.drawString(infoText, tx, infoY + lineH - 6);
        registerZone("Visor.statusBarBackground", labelX, infoY, labelW, lineH);

        // --- MAIN BODY ---
        int footerH = lineH * 3;
        int statusBottomH = lineH;
        int mainY = infoY + lineH + gap;
        int availableH = vh - (mainY - vy) - footerH - statusBottomH - (gap * 2);
        int sideW = Math.max(80, (int)(vw * 0.28f));

        // --- 4. FILE LIST ---
        drawZone(g2, "LISTA", vx, mainY, sideW, availableH, "List.background", "List.foreground");
        int selH = lineH - 2;
        int selY = mainY + 14;
        drawInteractiveRect(g2, vx + 2, selY, sideW - 4, selH,
            "List.selectionBackground", "List.selectionForeground", "Seleccionado");
        int inactY = selY + selH + gap;
        drawInteractiveRect(g2, vx + 2, inactY, sideW - 4, selH,
            "List.selectionInactiveBackground", "List.selectionInactiveForeground", "Inactivo");

        // --- 5. IMAGE VIEWER ---
        int rightX = vx + sideW + gap;
        int rightW = vw - sideW - gap;
        int viewerW = (int)(rightW * 0.65f);
        drawZone(g2, "VISOR", rightX, mainY, viewerW, availableH, "windowBackground", Color.GRAY);

        // Focus border zone
        int focusX = rightX + 10;
        int focusY = mainY + 10;
        int focusW = viewerW - 20;
        int focusH = Math.max(20, (int)(availableH * 0.25f));
        g2.setColor(getColor("Visor.borderColor.focused", new Color(60, 120, 220)));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(focusX, focusY, focusW, focusH);
        registerZone("Visor.borderColor.focused", focusX, focusY, focusW, focusH);
        g2.setColor(getColor("windowBackground", Color.GRAY));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
        g2.drawString("borde enfoque", focusX + 3, focusY + 10);

        // Marked image border (small sample)
        int markedX = rightX + 10;
        int markedY = mainY + availableH - 25;
        int markedW = 55;
        int markedH = 16;
        g2.setColor(getColor("Visor.markedImageBorder", Color.MAGENTA));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(markedX, markedY, markedW, markedH);
        registerZone("Visor.markedImageBorder", markedX, markedY, markedW, markedH);
        g2.setColor(getColor("Visor.markedImageBorder", Color.MAGENTA));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
        g2.drawString("marcada", markedX + 3, markedY + 12);

        // --- 6. GRID AREA (Tree/Table) ---
        int gridX = rightX + viewerW + gap;
        int gridW = rightW - viewerW - gap;
        drawZone(g2, "GRILLA", gridX, mainY, gridW, availableH, "Panel.background", "Label.foreground");
        int treeSelY = mainY + 14;
        drawInteractiveRect(g2, gridX + 2, treeSelY, gridW - 4, selH,
            "Tree.selectionBackground", "Tree.selectionForeground", "Arbol");
        int tableSelY = treeSelY + selH + gap;
        drawInteractiveRect(g2, gridX + 2, tableSelY, gridW - 4, selH,
            "Table.selectionBackground", "Table.selectionForeground", "Tabla");

        // --- 7. THUMBNAILS ---
        int thumbY = mainY + availableH + gap;
        drawZone(g2, "MINIATURAS", vx, thumbY, vw, footerH, "Panel.background", "Label.foreground");
        g2.setColor(getColor("Visor.markedImageBorder", Color.MAGENTA));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(vx + sideW + 10, thumbY + 10, 40, 30);
        registerZone("Visor.markedImageBorder", vx + sideW + 10, thumbY + 10, 40, 30);

        // --- 8. LOWER STATUS BAR ---
        int statusBottomY = thumbY + footerH + gap;
        drawZone(g2, "BARRA ESTADO", vx, statusBottomY, vw, statusBottomH,
            "Visor.statusBarBackground", "Visor.statusBarForeground");
        drawInteractiveRect(g2, vx + 2, statusBottomY + 2, tbW, statusBottomH - 4,
            "TextField.background", "TextField.foreground", "Carpeta:");

        int labelX2 = vx + tbW + gap;
        int labelW2 = vw - tbW - gap;
        g2.setColor(getColor("Visor.statusBarForeground", Color.WHITE));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        String bottomText = "Listo";
        fm = g2.getFontMetrics();
        tx = labelX2 + (labelW2 - fm.stringWidth(bottomText)) / 2;
        g2.drawString(bottomText, tx, statusBottomY + statusBottomH - 6);
        registerZone("Visor.statusBarBackground", labelX2, statusBottomY, labelW2, statusBottomH);

        // --- HOVER HIGHLIGHT ---
        if (hoveredKey != null) {
            g2.setColor(new Color(255, 255, 255, 80));
            g2.setStroke(new BasicStroke(2f));
            for (Map.Entry<Rectangle2D.Float, String> entry : zones.entrySet()) {
                if (entry.getValue().equals(hoveredKey)) {
                    Rectangle2D.Float r = entry.getKey();
                    g2.drawRect((int)(r.x * w), (int)(r.y * h),
                        (int)(r.width * w), (int)(r.height * h));
                }
            }
        }
    }

    private void drawZone(Graphics2D g2, String label, int x, int y, int w, int h,
                          String bgKey, Object fg) {
        Color bgColor = getColor(bgKey, Color.GRAY);
        Color fgColor = (fg instanceof String) ? getColor((String) fg, Color.BLACK) : (Color) fg;

        g2.setColor(bgColor);
        g2.fillRect(x, y, w, h);

        g2.setColor(fgColor);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (w - fm.stringWidth(label)) / 2;
        int ty = y + ((h - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(label, tx, ty);

        registerZone(bgKey, x, y, w, h);
    }

    private void drawInteractiveRect(Graphics2D g2, int x, int y, int w, int h,
                                     String bgKey, String fgKey, String text) {
        Color bgColor = getColor(bgKey, Color.GRAY);
        Color fgColor = getColor(fgKey, Color.BLACK);

        g2.setColor(bgColor);
        g2.fillRect(x, y, w, h);
        if (text != null && !text.isEmpty()) {
            g2.setColor(fgColor);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, x + 3, y + ((h - fm.getHeight()) / 2) + fm.getAscent());
        }

        registerZone(bgKey, x, y, w, h);
    }

    private void registerZone(String key, int x, int y, int w, int h) {
        zones.put(new Rectangle2D.Float((float) x / getWidth(), (float) y / getHeight(),
            (float) w / getWidth(), (float) h / getHeight()), key);
    }

    private Color getColor(String key, Color fallback) {
        Color c = colors.get(key);
        return (c != null) ? c : fallback;
    }
}
