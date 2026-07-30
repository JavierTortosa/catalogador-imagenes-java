package modelo.editor;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.swing.SwingConstants;

public class TextLayer implements Layer {

    public static final int ALIGN_LEFT   = SwingConstants.LEFT;
    public static final int ALIGN_CENTER = SwingConstants.CENTER;
    public static final int ALIGN_RIGHT  = SwingConstants.RIGHT;

    public static record TextRun(Font font, Color color, boolean underline, boolean strikethrough, String text) {}

    private final String id;
    private String name;
    private String text;
    private Font font;
    private Color color;
    private int alignment;
    private boolean vertical;
    private float lineSpacing;
    private Rectangle bounds;
    private boolean visible;
    private boolean locked;
    private boolean autoSize;
    private boolean underline;
    private boolean strikethrough;
    private boolean flowColumns;
    private float opacity;

    /** Runs con formato por carácter (rich text desde JTextPane) */
    private List<TextRun> runs;

    public transient boolean editingInline;


    public TextLayer(String name, String text, Font font, Color color, Rectangle bounds) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.text = text;
        this.font = font;
        this.color = color;
        this.alignment = ALIGN_LEFT;
        this.vertical = false;
        this.underline = false;
        this.strikethrough = false;
        this.flowColumns = false;
        this.lineSpacing = 1.2f;
        this.bounds = Objects.requireNonNull(bounds);
        this.visible = true;
        this.locked = false;
        this.autoSize = false;
        this.opacity = 1.0f;
    } // --- Fin del constructor TextLayer ---


    private TextLayer(String id, String name, String text, Font font, Color color,
                      int alignment, boolean vertical, boolean underline,
                      boolean strikethrough, boolean flowColumns, float lineSpacing,
                      Rectangle bounds, float opacity, boolean visible,
                      boolean locked, boolean autoSize, List<TextRun> runs) {
        this.id = id;
        this.name = name;
        this.text = text;
        this.font = font;
        this.color = color;
        this.alignment = alignment;
        this.vertical = vertical;
        this.underline = underline;
        this.strikethrough = strikethrough;
        this.flowColumns = flowColumns;
        this.lineSpacing = lineSpacing;
        this.bounds = bounds;
        this.opacity = opacity;
        this.visible = visible;
        this.locked = locked;
        this.autoSize = autoSize;
        this.runs = runs;
    } // --- Fin del constructor privado TextLayer ---


    // ==================== Getters / Setters específicos ====================


    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Font getFont() { return font; }
    public void setFont(Font font) { this.font = font; }

    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }

    public int getAlignment() { return alignment; }
    public void setAlignment(int alignment) { this.alignment = alignment; }

    public boolean isVertical() { return vertical; }
    public void setVertical(boolean vertical) { this.vertical = vertical; }

    public boolean isUnderline() { return underline; }
    public void setUnderline(boolean underline) { this.underline = underline; }

    public boolean isStrikethrough() { return strikethrough; }
    public void setStrikethrough(boolean strikethrough) { this.strikethrough = strikethrough; }

    public boolean isFlowColumns() { return flowColumns; }
    public void setFlowColumns(boolean flowColumns) { this.flowColumns = flowColumns; }

    public float getLineSpacing() { return lineSpacing; }
    public void setLineSpacing(float lineSpacing) { this.lineSpacing = lineSpacing; }

    public boolean isAutoSize() { return autoSize; }
    public void setAutoSize(boolean autoSize) { this.autoSize = autoSize; }

    public List<TextRun> getRuns() { return runs; }
    public void setRuns(List<TextRun> runs) { this.runs = runs; }


    // ==================== Layer ====================


    @Override
    public String getId() { return id; }

    @Override
    public String getName() { return name; }

    @Override
    public void setName(String name) { this.name = name; }

    @Override
    public Rectangle getBounds() { return bounds; }

    @Override
    public void setBounds(Rectangle bounds) { this.bounds = bounds; }

    @Override
    public boolean isVisible() { return visible; }

    @Override
    public void setVisible(boolean visible) { this.visible = visible; }

    @Override
    public boolean isLocked() { return locked; }

    @Override
    public void setLocked(boolean locked) { this.locked = locked; }

    @Override
    public float getOpacity() { return opacity; }

    @Override
    public void setOpacity(float opacity) { this.opacity = opacity; }


    @Override
    public void paint(Graphics2D g2) {
        if (!visible || text == null || text.isBlank() || bounds == null) return;
        if (editingInline) return;

        Graphics2D g = (Graphics2D) g2.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (opacity < 1.0f) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            }

            g.setFont(font);
            g.setColor(color);

            FontMetrics fm = g.getFontMetrics();

            if (vertical) {
                paintVertical(g, fm);
            } else {
                paintHorizontal(g, fm);
            }
        } finally {
            g.dispose();
        }
    } // --- Fin del metodo paint ---


    private void paintHorizontal(Graphics2D g, FontMetrics fm) {
        if (runs != null && !runs.isEmpty()) {
            paintRuns(g);
            return;
        }
        paintPlainText(g, fm);
    } // --- Fin del metodo paintHorizontal ---


    private void paintRuns(Graphics2D g) {
        record RS(Font font, Color color, boolean underline, boolean strikethrough, String text) {}

        // Divide runs en líneas según \n
        List<List<RS>> lines = new ArrayList<>();
        List<RS> cur = new ArrayList<>();
        for (TextRun r : runs) {
            String txt = r.text();
            int start = 0;
            while (true) {
                int nl = txt.indexOf('\n', start);
                if (nl < 0) break;
                if (nl > start) cur.add(new RS(r.font(), r.color(), r.underline(), r.strikethrough(), txt.substring(start, nl)));
                if (!cur.isEmpty()) { lines.add(cur); cur = new ArrayList<>(); }
                start = nl + 1;
            }
            if (start < txt.length()) cur.add(new RS(r.font(), r.color(), r.underline(), r.strikethrough(), txt.substring(start)));
        }
        if (!cur.isEmpty()) lines.add(cur);
        if (lines.isEmpty()) return;

        // Métricas por línea
        int n = lines.size();
        int[] lineH = new int[n];
        int[] lineW = new int[n];
        int[] asc = new int[n];
        int totalH = 0;
        for (int i = 0; i < n; i++) {
            for (RS s : lines.get(i)) {
                FontMetrics lfm = g.getFontMetrics(s.font());
                lineH[i] = Math.max(lineH[i], lfm.getHeight());
                lineW[i] += lfm.stringWidth(s.text());
                asc[i] = Math.max(asc[i], lfm.getAscent());
            }
            totalH += lineH[i];
            if (i > 0) totalH += Math.round(lineH[i] * (lineSpacing - 1.0f));
        }

        int cursorY = autoSize ? bounds.y + (bounds.height - totalH) / 2 : bounds.y;

        for (int i = 0; i < n; i++) {
            int baseline = cursorY + asc[i];
            int x = switch (alignment) {
                case ALIGN_CENTER -> bounds.x + (bounds.width - lineW[i]) / 2;
                case ALIGN_RIGHT  -> bounds.x + bounds.width - lineW[i];
                default -> bounds.x;
            };
            for (RS s : lines.get(i)) {
                g.setFont(s.font());
                g.setColor(s.color());
                FontMetrics lfm = g.getFontMetrics();
                int sw = lfm.stringWidth(s.text());
                g.drawString(s.text(), x, baseline);
                if (s.underline()) g.drawLine(x, baseline + 2, x + sw, baseline + 2);
                if (s.strikethrough()) g.drawLine(x, baseline - asc[i] / 3, x + sw, baseline - asc[i] / 3);
                x += sw;
            }
            cursorY += lineH[i];
            if (i < n - 1) cursorY += Math.round(lineH[i + 1] * (lineSpacing - 1.0f));
        }
    } // --- Fin del metodo paintRuns ---


    private void paintPlainText(Graphics2D g, FontMetrics fm) {
        List<String> wrapped = autoSize ? simpleLines() : wrappedLines(fm);
        int lineH = fm.getHeight();
        int spacingPx = Math.round(lineH * (lineSpacing - 1.0f));
        int totalH = wrapped.size() * lineH + (wrapped.size() - 1) * spacingPx;
        int startY = autoSize
                ? bounds.y + (bounds.height - totalH) / 2 + fm.getAscent()
                : bounds.y + fm.getAscent();
        int maxW = autoSize && !wrapped.isEmpty()
                ? fm.stringWidth(wrapped.get(0))
                : bounds.width;

        for (int i = 0; i < wrapped.size(); i++) {
            String line = wrapped.get(i);
            int lw = fm.stringWidth(line);
            int lineX = switch (alignment) {
                case ALIGN_CENTER -> bounds.x + (maxW - lw) / 2;
                case ALIGN_RIGHT  -> bounds.x + maxW - lw;
                default -> bounds.x;
            };
            int lineY = startY + i * (lineH + spacingPx);
            g.drawString(line, lineX, lineY);

            if (underline) {
                g.drawLine(lineX, lineY + 2, lineX + lw, lineY + 2);
            }
            if (strikethrough) {
                int sY = lineY - fm.getAscent() / 3;
                g.drawLine(lineX, sY, lineX + lw, sY);
            }
        }
    } // --- Fin del metodo paintPlainText ---


    private List<String> simpleLines() {
        String[] parts = text.split("\n", -1);
        List<String> result = new ArrayList<>(parts.length);
        for (String p : parts) result.add(p);
        return result;
    } // --- Fin del metodo simpleLines ---


    private List<String> wrappedLines(FontMetrics fm) {
        String[] hard = text.split("\n", -1);
        List<String> result = new ArrayList<>();
        int maxW = bounds != null ? bounds.width : Integer.MAX_VALUE;
        for (String line : hard) {
            if (fm.stringWidth(line) <= maxW) {
                result.add(line);
                continue;
            }
            String[] words = line.split(" ");
            StringBuilder cur = new StringBuilder();
            for (String w : words) {
                String test = cur.isEmpty() ? w : cur + " " + w;
                if (fm.stringWidth(test) <= maxW) {
                    cur = new StringBuilder(test);
                } else {
                    if (!cur.isEmpty()) result.add(cur.toString());
                    cur = new StringBuilder(w);
                }
            }
            if (!cur.isEmpty()) result.add(cur.toString());
        }
        return result;
    } // --- Fin del metodo wrappedLines ---


    private void paintVertical(Graphics2D g, FontMetrics fm) {
        char[] chars = text.toCharArray();
        int charH = fm.getAscent() + fm.getDescent();
        int startX = bounds.x + (bounds.width - fm.charWidth(chars.length > 0 ? chars[0] : ' ')) / 2;

        for (int i = 0; i < chars.length; i++) {
            int cy = bounds.y + i * charH;
            if (cy + charH > bounds.y + bounds.height) break;
            int drawY = cy + fm.getAscent();
            g.drawString(String.valueOf(chars[i]), startX, drawY);

            if (underline) {
                g.drawLine(startX - 2, cy, startX - 2, cy + charH);
            }
            if (strikethrough) {
                g.drawLine(startX + fm.charWidth(chars[i]) / 2, cy, startX + fm.charWidth(chars[i]) / 2, cy + charH);
            }
        }
    } // --- Fin del metodo paintVertical ---


    @Override
    public BufferedImage renderThumbnail(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Font thumbFont = font;
            Color thumbColor = color;
            if (runs != null && !runs.isEmpty()) {
                thumbFont = runs.get(0).font();
                thumbColor = runs.get(0).color();
            }
            float scale = size / 80f;
            g.setFont(thumbFont.deriveFont(thumbFont.getSize2D() * scale));
            g.setColor(thumbColor);

            FontMetrics fm = g.getFontMetrics();
            String label = text != null && !text.isEmpty()
                    ? (text.length() > 10 ? text.substring(0, 10) + "..." : text)
                    : "Txt";
            int tx = (size - fm.stringWidth(label)) / 2;
            int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(label, tx, ty);
        } finally {
            g.dispose();
        }
        return img;
    } // --- Fin del metodo renderThumbnail ---


    @Override
    public Layer copy() {
        return new TextLayer(
                UUID.randomUUID().toString(),
                name + " (copia)",
                text, font, color,
                alignment, vertical, underline, strikethrough, flowColumns, lineSpacing,
                new Rectangle(bounds),
                opacity, visible, locked, autoSize,
                runs != null ? new ArrayList<>(runs) : null);
    } // --- Fin del metodo copy ---

} // --- Fin de la clase TextLayer ---
