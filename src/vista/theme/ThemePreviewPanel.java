package vista.theme;

import java.awt.BasicStroke;
import java.awt.Color;
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

/**
 * Previsualización paso a paso - Paso 10: Soporte para múltiples zonas con la misma clave.
 */
public class ThemePreviewPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final Map<String, Color> colors = new HashMap<>();
    private Consumer<String> onZoneClicked;
    // Cambiamos el mapa para que la clave sea el rectángulo y el valor la propiedad
    private final Map<Rectangle2D.Float, String> zones = new LinkedHashMap<>();

    public ThemePreviewPanel() {
        setPreferredSize(new Dimension(600, 480));
        setBackground(new Color(45, 45, 48));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (onZoneClicked == null) return;
                float rx = (float) e.getX() / getWidth();
                float ry = (float) e.getY() / getHeight();
                
                // Buscamos de atrás hacia adelante (capas superiores primero)
                Rectangle2D.Float foundRect = null;
                String foundKey = null;
                
                for (Map.Entry<Rectangle2D.Float, String> entry : zones.entrySet()) {
                    if (entry.getKey().contains(rx, ry)) {
                        foundRect = entry.getKey();
                        foundKey = entry.getValue();
                        // No rompemos para que el último (más pequeño/arriba) gane
                    }
                }
                
                if (foundKey != null) {
                    onZoneClicked.accept(foundKey);
                }
            }
        });
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

        int vx = 25, vy = 25, vw = w - 50, vh = h - 50;
        int gap = 2;
        int lineH = (int)(vh * 0.05f);
        
        // --- 1. MENÚ ---
        drawInteractiveZone(g2, "Menú", vx, vy, vw, lineH, "MenuBar.background", "MenuBar.foreground");

        // --- 2. BOTONES ---
        int toolbarY = vy + lineH + gap;
        int toolbarH = lineH * 2;
        drawInteractiveZone(g2, "", vx, toolbarY, vw, toolbarH, "ToolBar.background", "Button.foreground");
        g2.setColor(getColor("Button.foreground", Color.BLACK));
        for(int i=0; i<6; i++) g2.drawRoundRect(vx + 10 + i*25, toolbarY + 6, 18, 18, 3, 3);

        // --- 3. STATUS BAR SUPERIOR ---
        int infoY = toolbarY + toolbarH + gap;
        drawInteractiveZone(g2, "", vx, infoY, vw, lineH, "Visor.statusBarBackground", "Visor.statusBarForeground");
        
        int tbW = (int)(vw * 0.45f);
        // TextBox Ruta
        drawInteractiveZone(g2, "Ruta: F:\\Juegos...", vx + 2, infoY + 2, tbW, lineH - 4, "TextField.background", "TextField.foreground");
        g2.setColor(getColor("Component.accentColor", Color.CYAN));
        g2.drawRect(vx + 2, infoY + 2, tbW, lineH - 4);
        // Texto Status Bar
        drawLabelOnly(g2, "Texto", vx + tbW + gap, infoY, vw - tbW - gap, lineH, "Visor.statusBarForeground");

        // --- CUERPO ---
        int footerH = lineH * 3;
        int statusBottomH = lineH;
        int mainY = infoY + lineH + gap;
        int imgH = vh - (mainY - vy) - footerH - statusBottomH - (gap * 2);
        int sideW = (int)(vw * 0.3f);
        
        // --- 4. LISTA ---
        drawInteractiveZone(g2, "Lista", vx, mainY, sideW, imgH, "List.background", "List.foreground");
        // Selección
        int selY = mainY + 15;
        drawInteractiveZone(g2, "Seleccionado", vx + 2, selY, sideW - 4, lineH, "List.selectionBackground", "List.selectionForeground");

        // --- 5. VISOR ---
        int rightX = vx + sideW + gap;
        int rightW = vw - sideW - gap;
        drawInteractiveZone(g2, "Imagen", rightX, mainY, rightW, imgH, "windowBackground", Color.GRAY);

        // --- 6. MINIATURAS ---
        int thumbY = mainY + imgH + gap;
        drawInteractiveZone(g2, "Miniaturas", vx, thumbY, vw, footerH, "Panel.background", "Label.foreground");
        // Marco selección
        g2.setColor(getColor("Visor.markedImageBorder", Color.MAGENTA));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(vx + sideW + 10, thumbY + 10, 40, 30);
        registerZone("Visor.markedImageBorder", vx + sideW + 10, thumbY + 10, 40, 30);

        // --- 7. STATUS BAR INFERIOR ---
        int statusBottomY = thumbY + footerH + gap;
        drawInteractiveZone(g2, "", vx, statusBottomY, vw, statusBottomH, "Visor.statusBarBackground", "Visor.statusBarForeground");
        // TextBox Carpeta
        drawInteractiveZone(g2, "Carpeta: Chess", vx + 2, statusBottomY + 2, tbW, statusBottomH - 4, "TextField.background", "TextField.foreground");
        // Texto Status Bar
        drawLabelOnly(g2, "Texto", vx + tbW + gap, statusBottomY, vw - tbW - gap, statusBottomH, "Visor.statusBarForeground");
    }

    private void drawInteractiveZone(Graphics2D g2, String text, int x, int y, int w, int h, String bgKey, Object fg) {
        Color bgColor = getColor(bgKey, Color.GRAY);
        Color fgColor = (fg instanceof String) ? getColor((String)fg, Color.BLACK) : (Color)fg;
        
        g2.setColor(bgColor);
        g2.fillRect(x, y, w, h);
        
        if (text != null && !text.isEmpty()) {
            g2.setColor(fgColor);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + 5;
            if (text.equals("Imagen") || text.equals("Menú") || text.equals("Miniaturas")) tx = x + (w - fm.stringWidth(text)) / 2;
            int ty = y + ((h - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(text, tx, ty);
        }
        
        registerZone(bgKey, x, y, w, h);
    }

    private void drawLabelOnly(Graphics2D g2, String text, int x, int y, int w, int h, String fgKey) {
        g2.setColor(getColor(fgKey, Color.WHITE));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (w - fm.stringWidth(text)) / 2;
        int ty = y + ((h - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(text, tx, ty);
        
        // El área de texto de la barra de estado también debe ser clicable para el color de la barra
        registerZone("Visor.statusBarBackground", x, y, w, h);
    }

    private void registerZone(String key, int x, int y, int w, int h) {
        zones.put(new Rectangle2D.Float((float)x/getWidth(), (float)y/getHeight(), (float)w/getWidth(), (float)h/getHeight()), key);
    }

    private Color getColor(String key, Color fallback) {
        Color c = colors.get(key);
        return (c != null) ? c : fallback;
    }
}
