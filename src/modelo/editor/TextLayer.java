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
    public static final int ALIGN_JUSTIFY = 10;

    public static record TextRun(Font font, Color color, boolean underline, boolean strikethrough, String text) {}

    /** Segmento de texto con estilo, usado internamente para pintar las líneas de runs */
    private record RS(Font font, Color color, boolean underline, boolean strikethrough, String text) {}

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
    private double rotation;

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
                      boolean locked, boolean autoSize, List<TextRun> runs,
                      double rotation) {
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
        this.rotation = rotation;
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
    public double getRotation() { return rotation; }

    @Override
    public void setRotation(double rotation) { this.rotation = rotation; }


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
            if (rotation != 0) {
                g.rotate(Math.toRadians(rotation), bounds.getCenterX(), bounds.getCenterY());
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

        if (!flowColumns) {
            int cursorY = autoSize ? bounds.y + (bounds.height - totalH) / 2 : bounds.y;
            int maxW = autoSize ? maxOf(lineW) : bounds.width;
            for (int i = 0; i < n; i++) {
                int baseline = cursorY + asc[i];
                int x = switch (alignment) {
                    case ALIGN_CENTER -> bounds.x + (maxW - lineW[i]) / 2;
                    case ALIGN_RIGHT  -> bounds.x + maxW - lineW[i];
                    default -> bounds.x;
                };
                if (alignment == ALIGN_JUSTIFY && maxW > lineW[i] && hasSpaces(lines.get(i))) {
                    drawRunsLine(g, lines.get(i), x, baseline, maxW, asc[i]);
                } else {
                    drawRunsLine(g, lines.get(i), x, baseline, lineW[i], asc[i]);
                }
                cursorY += lineH[i];
                if (i < n - 1) cursorY += Math.round(lineH[i + 1] * (lineSpacing - 1.0f));
            }
            return;
        }

        // Flujo en columnas: distribuye las líneas en varias columnas
        int maxLineH = maxOf(lineH);
        int spacingPx = Math.round(maxLineH * (lineSpacing - 1.0f));
        int maxW = maxOf(lineW);
        int linesPerCol = Math.max(1, bounds.height / (maxLineH + spacingPx));
        int numCols = Math.max(1, (int) Math.ceil(n / (double) linesPerCol));
        if (flowColumns && n >= 2) numCols = Math.max(numCols, 2);
        int colWidth = bounds.width / numCols;
        if (colWidth < maxW) {
            numCols = Math.max(1, bounds.width / Math.max(1, maxW));
            colWidth = bounds.width / numCols;
        }
        int distPerCol = Math.max(1, (int) Math.ceil(n / (double) numCols));
        for (int i = 0; i < n; i++) {
            int col = i / distPerCol;
            int row = i % distPerCol;
            int colX = bounds.x + col * colWidth;
            int baseline = bounds.y + row * (maxLineH + spacingPx) + asc[i];
            int x = switch (alignment) {
                case ALIGN_CENTER -> colX + (colWidth - lineW[i]) / 2;
                case ALIGN_RIGHT  -> colX + colWidth - lineW[i];
                default -> colX;
            };
            if (alignment == ALIGN_JUSTIFY && colWidth > lineW[i] && hasSpaces(lines.get(i))) {
                drawRunsLine(g, lines.get(i), colX, baseline, colWidth, asc[i]);
            } else {
                drawRunsLine(g, lines.get(i), x, baseline, lineW[i], asc[i]);
            }
        }
    } // --- Fin del metodo paintRuns ---


    private int drawRunsLine(Graphics2D g, List<RS> line, int x, int baseline, int availWidth, int asc) {
        int lineW = 0;
        int numSpaces = 0;
        for (RS s : line) {
            lineW += g.getFontMetrics(s.font()).stringWidth(s.text());
            numSpaces += countSpaces(s.text());
        }
        float extra = (alignment == ALIGN_JUSTIFY && numSpaces > 0 && availWidth > lineW)
                ? (float) (availWidth - lineW) / numSpaces
                : 0f;
        int cx = x;
        for (RS s : line) {
            g.setFont(s.font());
            g.setColor(s.color());
            FontMetrics lfm = g.getFontMetrics();
            int segStart = cx;
            if (extra > 0f) {
                for (int i = 0; i < s.text().length(); i++) {
                    char c = s.text().charAt(i);
                    String cs = String.valueOf(c);
                    g.drawString(cs, cx, baseline);
                    cx += lfm.charWidth(c);
                    if (c == ' ') cx += Math.round(extra);
                }
            } else {
                g.drawString(s.text(), cx, baseline);
                cx += lfm.stringWidth(s.text());
            }
            if (s.underline()) g.drawLine(segStart, baseline + 2, cx, baseline + 2);
            if (s.strikethrough()) g.drawLine(segStart, baseline - asc / 3, cx, baseline - asc / 3);
        }
        return cx;
    } // --- Fin del metodo drawRunsLine ---


    private int countSpaces(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ' ') count++;
        }
        return count;
    } // --- Fin del metodo countSpaces ---


    private boolean hasSpaces(List<RS> line) {
        for (RS s : line) {
            if (countSpaces(s.text()) > 0) return true;
        }
        return false;
    } // --- Fin del metodo hasSpaces ---


    private static int maxOf(int[] values) {
        int max = 0;
        for (int v : values) max = Math.max(max, v);
        return max;
    } // --- Fin del metodo maxOf ---


    private void paintPlainText(Graphics2D g, FontMetrics fm) {
        List<String> lines = autoSize ? simpleLines() : wrappedLines(fm);
        int lineH = fm.getHeight();
        int spacingPx = Math.round(lineH * (lineSpacing - 1.0f));
        int n = lines.size();
        if (n == 0) return;

        if (!flowColumns) {
            int totalH = n * lineH + (n - 1) * spacingPx;
            int startY = autoSize
                    ? bounds.y + (bounds.height - totalH) / 2 + fm.getAscent()
                    : bounds.y + fm.getAscent();
            int maxW = autoSize ? maxLineWidth(fm, lines) : bounds.width;
            for (int i = 0; i < n; i++) {
                String line = lines.get(i);
                int lw = fm.stringWidth(line);
                int lineX = switch (alignment) {
                    case ALIGN_CENTER -> bounds.x + (maxW - lw) / 2;
                    case ALIGN_RIGHT  -> bounds.x + maxW - lw;
                    default -> bounds.x;
                };
                int lineY = startY + i * (lineH + spacingPx);
                int endX;
                if (alignment == ALIGN_JUSTIFY && countSpaces(line) > 0 && maxW > lw) {
                    endX = drawJustifiedText(g, fm, line, bounds.x, lineY, maxW);
                } else {
                    g.drawString(line, lineX, lineY);
                    endX = lineX + lw;
                }
                if (underline) g.drawLine(lineX, lineY + 2, endX, lineY + 2);
                if (strikethrough) g.drawLine(lineX, lineY - fm.getAscent() / 3, endX, lineY - fm.getAscent() / 3);
            }
            return;
        }

        // Flujo en columnas
        int maxW = maxLineWidth(fm, lines);
        int linesPerCol = Math.max(1, bounds.height / (lineH + spacingPx));
        int numCols = Math.max(1, (int) Math.ceil(n / (double) linesPerCol));
        if (flowColumns && n >= 2) numCols = Math.max(numCols, 2);
        int colWidth = autoSize ? maxW : bounds.width / numCols;
        if (colWidth < maxW) {
            numCols = Math.max(1, bounds.width / Math.max(1, maxW));
            colWidth = autoSize ? maxW : bounds.width / numCols;
        }
        List<String> colLines = autoSize ? lines : wrappedLines(fm, Math.max(1, colWidth));
        int cn = colLines.size();
        int cLinesPerCol = Math.max(1, bounds.height / (lineH + spacingPx));
        int cNumCols = Math.max(1, (int) Math.ceil(cn / (double) cLinesPerCol));
        if (flowColumns && cn >= 2) cNumCols = Math.max(cNumCols, 2);
        int cColWidth = autoSize ? maxW : bounds.width / cNumCols;
        if (cColWidth < maxW) {
            cNumCols = Math.max(1, bounds.width / Math.max(1, maxW));
            cColWidth = autoSize ? maxW : bounds.width / cNumCols;
        }
        int distPerCol = Math.max(1, (int) Math.ceil(cn / (double) cNumCols));
        for (int i = 0; i < cn; i++) {
            int col = i / distPerCol;
            int row = i % distPerCol;
            String line = colLines.get(i);
            int lw = fm.stringWidth(line);
            int colX = bounds.x + col * cColWidth;
            int lineX = switch (alignment) {
                case ALIGN_CENTER -> colX + (cColWidth - lw) / 2;
                case ALIGN_RIGHT  -> colX + cColWidth - lw;
                default -> colX;
            };
            int lineY = bounds.y + row * (lineH + spacingPx) + fm.getAscent();
            int endX;
            if (alignment == ALIGN_JUSTIFY && countSpaces(line) > 0 && cColWidth > lw) {
                endX = drawJustifiedText(g, fm, line, colX, lineY, cColWidth);
            } else {
                g.drawString(line, lineX, lineY);
                endX = lineX + lw;
            }
            if (underline) g.drawLine(lineX, lineY + 2, endX, lineY + 2);
            if (strikethrough) g.drawLine(lineX, lineY - fm.getAscent() / 3, endX, lineY - fm.getAscent() / 3);
        }
    } // --- Fin del metodo paintPlainText ---


    private int drawJustifiedText(Graphics2D g, FontMetrics fm, String line, int x, int baseline, int availWidth) {
        int lw = fm.stringWidth(line);
        int numSpaces = countSpaces(line);
        float extra = (numSpaces > 0 && availWidth > lw) ? (float) (availWidth - lw) / numSpaces : 0f;
        int cx = x;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            g.drawString(line.substring(i, i + 1), cx, baseline);
            cx += fm.charWidth(c);
            if (c == ' ') cx += Math.round(extra);
        }
        return cx;
    } // --- Fin del metodo drawJustifiedText ---


    private int maxLineWidth(FontMetrics fm, List<String> lines) {
        int max = 0;
        for (String l : lines) max = Math.max(max, fm.stringWidth(l));
        return max;
    } // --- Fin del metodo maxLineWidth ---


    private List<String> simpleLines() {
        String[] parts = text.split("\n", -1);
        List<String> result = new ArrayList<>(parts.length);
        for (String p : parts) result.add(p);
        return result;
    } // --- Fin del metodo simpleLines ---


    private List<String> wrappedLines(FontMetrics fm) {
        int maxW = bounds != null ? bounds.width : Integer.MAX_VALUE;
        return wrappedLines(fm, maxW);
    } // --- Fin del metodo wrappedLines ---


    private List<String> wrappedLines(FontMetrics fm, int width) {
        String[] hard = text.split("\n", -1);
        List<String> result = new ArrayList<>();
        int maxW = width > 0 ? width : Integer.MAX_VALUE;
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
        if (runs != null && !runs.isEmpty()) {
            paintRunsVertical(g);
            return;
        }
        paintPlainTextVertical(g, fm);
    } // --- Fin del metodo paintVertical ---


    private void paintPlainTextVertical(Graphics2D g, FontMetrics fm) {
        String[] rawLines = text.split("\n", -1);
        int charH = fm.getAscent() + fm.getDescent();
        int maxCw = 0;
        for (String line : rawLines) {
            for (int i = 0; i < line.length(); i++) maxCw = Math.max(maxCw, fm.charWidth(line.charAt(i)));
        }
        int colWidth = maxCw + 2;
        int charsPerCol = Math.max(1, bounds.height / Math.max(1, charH));
        int totalCols = 0;
        for (String line : rawLines) {
            totalCols += flowColumns ? Math.max(1, (line.length() + charsPerCol - 1) / charsPerCol) : 1;
        }
        if (totalCols == 0) totalCols = 1;

        int startX = bounds.x + Math.max(0, (bounds.width - totalCols * colWidth) / 2);
        if (startX + totalCols * colWidth > bounds.x + bounds.width) startX = bounds.x;
        int x = startX;
        int y = bounds.y;
        for (String line : rawLines) {
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (flowColumns && y + charH > bounds.y + bounds.height) {
                    y = bounds.y;
                    x += colWidth;
                }
                if (x + colWidth > bounds.x + bounds.width) return;
                int drawY = y + fm.getAscent();
                g.drawString(String.valueOf(c), x, drawY);
                if (underline) g.drawLine(x - 2, y, x - 2, y + charH);
                if (strikethrough) g.drawLine(x + fm.charWidth(c) / 2, y, x + fm.charWidth(c) / 2, y + charH);
                y += charH;
            }
            y = bounds.y;
            x += colWidth;
        }
    } // --- Fin del metodo paintPlainTextVertical ---


    private void paintRunsVertical(Graphics2D g) {
        record VC(Font font, Color color, boolean underline, boolean strikethrough, char c) {}

        List<VC> chars = new ArrayList<>();
        int maxCw = 0;
        int maxCharH = 0;
        for (TextRun r : runs) {
            FontMetrics lfm = g.getFontMetrics(r.font());
            maxCharH = Math.max(maxCharH, lfm.getAscent() + lfm.getDescent());
            for (int i = 0; i < r.text().length(); i++) {
                char c = r.text().charAt(i);
                maxCw = Math.max(maxCw, lfm.charWidth(c));
                chars.add(new VC(r.font(), r.color(), r.underline(), r.strikethrough(), c));
            }
        }
        if (chars.isEmpty()) return;

        int charH = Math.max(1, maxCharH);
        int colWidth = maxCw + 2;
        int charsPerCol = Math.max(1, bounds.height / charH);
        int totalCols = 0;
        int curLen = 0;
        for (VC vc : chars) {
            if (vc.c() == '\n') {
                totalCols += flowColumns ? Math.max(1, (curLen + charsPerCol - 1) / charsPerCol) : 1;
                curLen = 0;
            } else {
                curLen++;
            }
        }
        totalCols += flowColumns ? Math.max(1, (curLen + charsPerCol - 1) / charsPerCol) : (curLen > 0 ? 1 : 0);

        int startX = bounds.x + Math.max(0, (bounds.width - totalCols * colWidth) / 2);
        if (startX + totalCols * colWidth > bounds.x + bounds.width) startX = bounds.x;
        int x = startX;
        int y = bounds.y;
        for (VC vc : chars) {
            if (vc.c() == '\n') {
                y = bounds.y;
                x += colWidth;
                continue;
            }
            if (flowColumns && y + charH > bounds.y + bounds.height) {
                y = bounds.y;
                x += colWidth;
            }
            if (x + colWidth > bounds.x + bounds.width) return;
            g.setFont(vc.font());
            g.setColor(vc.color());
            FontMetrics lfm = g.getFontMetrics();
            int drawY = y + lfm.getAscent();
            g.drawString(String.valueOf(vc.c()), x, drawY);
            if (vc.underline()) g.drawLine(x - 2, y, x - 2, y + charH);
            if (vc.strikethrough()) g.drawLine(x + lfm.charWidth(vc.c()) / 2, y, x + lfm.charWidth(vc.c()) / 2, y + charH);
            y += charH;
        }
    } // --- Fin del metodo paintRunsVertical ---


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
                runs != null ? new ArrayList<>(runs) : null,
                rotation);
    } // --- Fin del metodo copy ---

} // --- Fin de la clase TextLayer ---
