package modelo.editor;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.UUID;

import javax.swing.SwingConstants;

public class TextLayer implements Layer {

    public static final int ALIGN_LEFT   = SwingConstants.LEFT;
    public static final int ALIGN_CENTER = SwingConstants.CENTER;
    public static final int ALIGN_RIGHT  = SwingConstants.RIGHT;

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
                      boolean locked, boolean autoSize) {
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
        String[] lines = text.split("\n", -1);
        int lineH = fm.getHeight();
        int spacingPx = Math.round(lineH * (lineSpacing - 1.0f));
        int totalH = lines.length * lineH + (lines.length - 1) * spacingPx;
        int startY = bounds.y + (bounds.height - totalH) / 2 + fm.getAscent();

        // En modo autoSize el alineado usa el ancho de la primera línea como referencia
        int alignWidth = autoSize && lines.length > 0
                ? fm.stringWidth(lines[0])
                : bounds.width;

        for (int i = 0; i < lines.length; i++) {
            int lineX = switch (alignment) {
                case ALIGN_CENTER -> bounds.x + (alignWidth - fm.stringWidth(lines[i])) / 2;
                case ALIGN_RIGHT  -> bounds.x + alignWidth - fm.stringWidth(lines[i]);
                default -> bounds.x; // LEFT
            };
            int lineY = startY + i * (lineH + spacingPx);
            g.drawString(lines[i], lineX, lineY);

            int strWidth = fm.stringWidth(lines[i]);
            if (underline) {
                int uY = lineY + 2;
                g.drawLine(lineX, uY, lineX + strWidth, uY);
            }
            if (strikethrough) {
                int sY = lineY - fm.getAscent() / 3;
                g.drawLine(lineX, sY, lineX + strWidth, sY);
            }
        }
    } // --- Fin del metodo paintHorizontal ---


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

            float scale = size / 80f;
            g.setFont(font.deriveFont(font.getSize2D() * scale));
            g.setColor(color);

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
                opacity, visible, locked, autoSize);
    } // --- Fin del metodo copy ---

} // --- Fin de la clase TextLayer ---
