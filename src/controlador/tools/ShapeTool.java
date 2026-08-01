package controlador.tools;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import controlador.commands.AppActionCommands;
import modelo.editor.ImageLayer;

/**
 * Herramienta de formas.
 * <p>
 * Arrastra para dibujar formas geométricas (rectángulo, elipse, línea,
 * triángulo, hexágono). Durante el arrastre se muestra un preview en vivo del
 * objeto real (con Shift: línea a 45°, cuadrado/círculo). Al soltar se crea una
 * capa recortada al tamaño exacto del objeto pintado (sin los bordes
 * transparentes). Lee color de relleno, color de borde, grosor y tipo de forma
 * desde la barra de opciones (Parte B).
 */
public class ShapeTool extends Tool {

    private Point startPoint;
    private Rectangle currentRect;
    private boolean dragging;

    private BufferedImage previewCache;
    private String previewCacheKey;

    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_FORMAS;
    } // --- Fin del metodo getCommandKey ---

    @Override
    public void mousePressed(MouseEvent e) {
        startPoint = e.getPoint();
        currentRect = new Rectangle(startPoint.x, startPoint.y, 0, 0);
        dragging = true;
        previewCache = null;
        previewCacheKey = null;
    } // --- Fin del metodo mousePressed ---

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!dragging || startPoint == null) return;
        int dx = e.getX() - startPoint.x;
        int dy = e.getY() - startPoint.y;

        if (e.isShiftDown()) {
            String type = ctx.componentBar().getSelectedShapeType();
            if ("line".equals(type)) {
                // Línea restringida a múltiplos de 45°
                double angle = Math.atan2(dy, dx);
                angle = Math.round(angle / (Math.PI / 4)) * (Math.PI / 4);
                double len = Math.hypot(dx, dy);
                dx = (int) Math.round(len * Math.cos(angle));
                dy = (int) Math.round(len * Math.sin(angle));
            } else {
                // Rectángulo → cuadrado, elipse → círculo, etc.
                int size = Math.max(Math.abs(dx), Math.abs(dy));
                dx = size * Integer.signum(dx);
                dy = size * Integer.signum(dy);
            }
        }

        int x = Math.min(startPoint.x, startPoint.x + dx);
        int y = Math.min(startPoint.y, startPoint.y + dy);
        int w = Math.abs(dx);
        int h = Math.abs(dy);
        currentRect = new Rectangle(x, y, Math.max(w, 4), Math.max(h, 4));
    } // --- Fin del metodo mouseDragged ---

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!dragging || currentRect == null) return;
        dragging = false;

        if (currentRect.width < 4 || currentRect.height < 4) {
            startPoint = null;
            currentRect = null;
            return;
        }

        Color fill = ctx.componentBar().getShapeFillColor();
        Color stroke = ctx.componentBar().getShapeStrokeColor();
        float width = Math.max(0, ctx.componentBar().getShapeStrokeWidth());
        String type = ctx.componentBar().getSelectedShapeType();

        BufferedImage img = renderShape(currentRect.width, currentRect.height, type, fill, stroke, width);
        int origW = img.getWidth();
        int origH = img.getHeight();

        // Recortar la capa al tamaño exacto del objeto pintado (estilo Photoshop)
        Rectangle bounds = new Rectangle(currentRect.x, currentRect.y, img.getWidth(), img.getHeight());
        Rectangle content = contentBounds(img);
        if (content != null && (content.x != 0 || content.y != 0
                || content.width != img.getWidth() || content.height != img.getHeight())) {
            BufferedImage trimmed = new BufferedImage(content.width, content.height,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D tg = trimmed.createGraphics();
            tg.drawImage(img, -content.x, -content.y, null);
            tg.dispose();
            img = trimmed;
            bounds = new Rectangle(currentRect.x + content.x, currentRect.y + content.y,
                    content.width, content.height);
        }

        String name = "Forma " + (ctx.layerModel().size() + 1);
        ImageLayer layer = new ImageLayer(name, img, bounds);
        layer.setType(ImageLayer.LayerType.SHAPE);
        layer.setShapeType(type);
        layer.setShapeFill(fill);
        layer.setShapeStroke(stroke);
        layer.setShapeStrokeWidth(width);
        layer.setShapeRenderW(origW);
        layer.setShapeRenderH(origH);
        ctx.layerModel().addLayer(layer);
        ctx.layerModel().setActiveLayer(layer);

        startPoint = null;
        currentRect = null;
        previewCache = null;
        previewCacheKey = null;
    } // --- Fin del metodo mouseReleased ---

    /**
     * Renderiza la forma dentro de una imagen del tamaño dado. Lo usa tanto el
     * preview en vivo como la creación final de la capa, garantizando WYSIWYG.
     */
    public static BufferedImage renderShape(int iw, int ih, String type,
            Color fill, Color stroke, float width) {
        iw = Math.max(1, iw);
        ih = Math.max(1, ih);
        BufferedImage img = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int cx = iw / 2;
        int cy = ih / 2;
        int r = Math.max(2, Math.min(cx, cy) - Math.round(width));

        switch (type) {
            case "ellipse" -> {
                g2.setColor(fill);
                g2.fill(new Ellipse2D.Float(0, 0, iw - 1, ih - 1));
                g2.setColor(stroke);
                g2.draw(new Ellipse2D.Float(0, 0, iw - 1, ih - 1));
            }
            case "line" -> {
                g2.setColor(stroke);
                g2.draw(new Line2D.Float(0, 0, iw - 1, ih - 1));
            }
            case "triangle" -> {
                Path2D tri = new Path2D.Float();
                tri.moveTo(cx, cy - r);
                tri.lineTo(cx + r, cy + r);
                tri.lineTo(cx - r, cy + r);
                tri.closePath();
                g2.setColor(fill);
                g2.fill(tri);
                g2.setColor(stroke);
                g2.draw(tri);
            }
            case "polygon" -> {
                Path2D poly = new Path2D.Float();
                int n = 6;
                for (int i = 0; i < n; i++) {
                    double a = 2 * Math.PI * i / n - Math.PI / 2;
                    double px = cx + r * Math.cos(a);
                    double py = cy + r * Math.sin(a);
                    if (i == 0) {
                        poly.moveTo(px, py);
                    } else {
                        poly.lineTo(px, py);
                    }
                }
                poly.closePath();
                g2.setColor(fill);
                g2.fill(poly);
                g2.setColor(stroke);
                g2.draw(poly);
            }
            default -> {
                g2.setColor(fill);
                g2.fillRect(0, 0, iw, ih);
                g2.setColor(stroke);
                g2.drawRect(0, 0, iw, ih);
            }
        }

        g2.dispose();
        return img;
    } // --- Fin del metodo renderShape ---

    /**
     * Devuelve el rectángulo que engloba los píxeles no transparentes de la imagen.
     */
    public static Rectangle contentBounds(BufferedImage img) {
        int minX = img.getWidth(), minY = img.getHeight();
        int maxX = -1, maxY = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) != 0) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) return null;
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    } // --- Fin del metodo contentBounds ---

    /**
     * Preview en vivo del objeto mientras se arrastra (con cache para no
     * re-renderizar si no han cambiado los parámetros).
     */
    private BufferedImage getPreview() {
        if (currentRect == null || currentRect.width < 4 || currentRect.height < 4) return null;
        int iw = currentRect.width;
        int ih = currentRect.height;
        Color fill = ctx.componentBar().getShapeFillColor();
        Color stroke = ctx.componentBar().getShapeStrokeColor();
        float width = Math.max(0, ctx.componentBar().getShapeStrokeWidth());
        String type = ctx.componentBar().getSelectedShapeType();
        String key = type + "|" + iw + "|" + ih + "|" + fill.getRGB()
                + "|" + stroke.getRGB() + "|" + width;
        if (!key.equals(previewCacheKey)) {
            previewCache = renderShape(iw, ih, type, fill, stroke, width);
            previewCacheKey = key;
        }
        return previewCache;
    } // --- Fin del metodo getPreview ---

    @Override
    public void paintOverlay(Graphics2D g2) {
        if (!dragging || currentRect == null) return;
        if (currentRect.width < 4 || currentRect.height < 4) return;

        BufferedImage preview = getPreview();
        if (preview == null) return;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
        g2.drawImage(preview, currentRect.x, currentRect.y, null);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    } // --- Fin del metodo paintOverlay ---

} // --- Fin de la clase ShapeTool ---
