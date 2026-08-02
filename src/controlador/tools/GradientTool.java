package controlador.tools;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.MultipleGradientPaint.CycleMethod;

import controlador.commands.AppActionCommands;
import modelo.editor.ImageLayer;

/**
 * Herramienta degradado.
 * <p>
 * Arrastra para definir el vector del degradado. Al soltar, crea una nueva capa
 * con un relleno degradado. Lee colores, tipo (Lineal, Radial, Angular,
 * Reflejado, Diamante) y opacidad desde la barra de opciones (Parte B).
 * <ul>
 * <li><b>Lineal</b>: degradado del punto inicial al final del arrastre.</li>
 * <li><b>Radial</b>: radial centrado en el punto inicial con radio = arrastre.</li>
 * <li><b>Angular</b>: cónico alrededor del punto inicial (el arrastre fija el
 * ángulo de inicio).</li>
 * <li><b>Reflejado</b>: espejo a lo largo del vector de arrastre (c1 en el
 * inicio, c2 en el extremo y c2→c1 hacia atrás, estilo Photoshop).</li>
 * <li><b>Diamante</b>: radial simétrico c1→c2→c1 centrado en el lienzo.</li>
 * </ul>
 */
public class GradientTool extends Tool {

    private Point startPoint;
    private Point endPoint;
    private boolean dragging;

    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_DEGRADADO;
    } // --- Fin del metodo getCommandKey ---

    @Override
    public void mousePressed(MouseEvent e) {
        startPoint = e.getPoint();
        endPoint = null;
        dragging = true;
    } // --- Fin del metodo mousePressed ---

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!dragging || startPoint == null) return;
        endPoint = e.getPoint();
    } // --- Fin del metodo mouseDragged ---

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!dragging || startPoint == null || endPoint == null) return;
        dragging = false;

        int cw = ctx.canvasModel() != null ? ctx.canvasModel().getWidth() : 1920;
        int ch = ctx.canvasModel() != null ? ctx.canvasModel().getHeight() : 1080;

        Color c1 = withAlpha(ctx.componentBar().getGradientStartColor(),
                ctx.componentBar().getGradientOpacity());
        Color c2 = withAlpha(ctx.componentBar().getGradientEndColor(),
                ctx.componentBar().getGradientOpacity());

        BufferedImage img = renderGradient(cw, ch, startPoint.x, startPoint.y,
                endPoint.x, endPoint.y, ctx.componentBar().getGradientType(), c1, c2);

        finishLayer(img, cw, ch);
    } // --- Fin del metodo mouseReleased ---


    @Override
    public boolean cancel() {
        if (!dragging) return false;
        dragging = false;
        startPoint = null;
        endPoint = null;
        ctx.canvasPanel().repaint();
        return true;
    } // --- Fin del metodo cancel ---

    /**
     * Renderiza un lienzo completo con el degradado solicitado. Independiente del
     * contexto (también lo usa el verificador headless de regresión).
     *
     * @param cw   ancho del lienzo
     * @param ch   alto del lienzo
     * @param x1,y1 punto inicial del vector de arrastre
     * @param x2,y2 punto final del vector de arrastre
     * @param type tipo de degradado (Lineal, Radial, Angular, Reflejado, Diamante)
     * @param c1   color inicial
     * @param c2   color final
     * @return imagen ARGB con el degradado pintado
     */
    public static BufferedImage renderGradient(int cw, int ch, int x1, int y1, int x2, int y2,
            String type, Color c1, Color c2) {
        BufferedImage img = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (type) {
            case "Radial" -> {
                float radius = Math.max(1f, (float) Point.distance(x1, y1, x2, y2));
                g2.setPaint(new RadialGradientPaint(x1, y1, radius,
                        new float[]{0f, 1f}, new Color[]{c1, c2}, CycleMethod.NO_CYCLE));
                g2.fillRect(0, 0, cw, ch);
            }
            case "Angular" -> paintConical(g2, cw, ch, x1, y1, x2, y2, c1, c2);
            case "Reflejado" -> paintReflected(img, cw, ch, x1, y1, x2, y2, c1, c2);
            case "Diamante" -> {
                float r = (float) Math.max(cw, ch) * 0.7f;
                g2.setPaint(new RadialGradientPaint(cw / 2f, ch / 2f, r,
                        new float[]{0f, 0.5f, 1f}, new Color[]{c1, c2, c1},
                        CycleMethod.NO_CYCLE));
                g2.fillRect(0, 0, cw, ch);
            }
            default -> {
                g2.setPaint(new GradientPaint(x1, y1, c1, x2, y2, c2));
                g2.fillRect(0, 0, cw, ch);
            }
        }

        g2.dispose();
        return img;
    } // --- Fin del metodo renderGradient ---

    /**
     * Pinta un degradado cónico (estilo Photoshop) alrededor del punto inicial.
     * <p>
     * Se dibuja un abanico de triángulos desde el centro, interpolando el color
     * entre c1 (ángulo de inicio = dirección del arrastre) y c2 (360°). El radio
     * cubre siempre el rectángulo completo del lienzo.
     */
    private static void paintConical(Graphics2D g2, int cw, int ch,
            float x1, float y1, float x2, float y2, Color c1, Color c2) {
        double baseAngle = Math.atan2(y2 - y1, x2 - x1);
        double maxR = 0;
        for (double[] corner : new double[][]{{0, 0}, {0, ch}, {cw, 0}, {cw, ch}}) {
            maxR = Math.max(maxR, Math.hypot(corner[0] - x1, corner[1] - y1));
        }
        maxR += 8;

        Object aa = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        int n = 180;
        double prevA = baseAngle;
        for (int i = 1; i <= n; i++) {
            double a = baseAngle + 2 * Math.PI * i / n;
            g2.setColor(lerp(c1, c2, (double) i / n));
            Path2D.Double tri = new Path2D.Double();
            tri.moveTo(x1, y1);
            tri.lineTo(x1 + maxR * Math.cos(prevA), y1 + maxR * Math.sin(prevA));
            tri.lineTo(x1 + maxR * Math.cos(a), y1 + maxR * Math.sin(a));
            tri.closePath();
            g2.fill(tri);
            prevA = a;
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
    } // --- Fin del metodo paintConical ---

    /**
     * Pinta un degradado reflejado a lo largo del vector de arrastre.
     * <p>
     * El color depende de la proyección de cada píxel sobre el eje del arrastre:
     * antes del inicio se fija en c1, entre inicio y extremo interpola c1→c2 y
     * más allá del extremo refleja c2→c1 hasta volver a c1.
     */
    private static void paintReflected(BufferedImage img, int cw, int ch,
            float x1, float y1, float x2, float y2, Color c1, Color c2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len2 = dx * dx + dy * dy;
        if (len2 < 1) {
            dx = 1;
            len2 = 1;
        }

        // LUT de 1024 colores para t en [0, 2] (512 por unidad)
        int[] lut = new int[1024];
        for (int i = 0; i < lut.length; i++) {
            double tt = i / 512.0;
            lut[i] = tt <= 1.0 ? lerpToArgb(c1, c2, tt) : lerpToArgb(c2, c1, tt - 1.0);
        }

        int w = Math.max(1, cw);
        int h = Math.max(1, ch);
        int[] px = new int[w * h];
        double k = -(x1 * dx + y1 * dy);
        double step = dx / len2;
        int idx = 0;
        for (int y = 0; y < h; y++) {
            double tBase = (y * dy + k) / len2;
            for (int x = 0; x < w; x++) {
                double t = tBase + x * step;
                double tt = t < 0 ? 0 : (t > 2 ? 2 : t);
                int li = (int) (tt * 512);
                if (li >= 1024) li = 1023;
                px[idx++] = lut[li];
            }
        }
        img.setRGB(0, 0, w, h, px, 0, w);
    } // --- Fin del metodo paintReflected ---

    private void finishLayer(BufferedImage img, int cw, int ch) {
        Rectangle bounds = new Rectangle(0, 0, cw, ch);
        String name = "Degradado " + (ctx.layerModel().size() + 1);
        ImageLayer layer = new ImageLayer(name, img, bounds);
        ctx.layerModel().addLayer(layer);
        ctx.layerModel().setActiveLayer(layer);

        startPoint = null;
        endPoint = null;
    } // --- Fin del metodo finishLayer ---

    private static Color withAlpha(Color c, int opacityPct) {
        int alpha = Math.max(0, Math.min(100, opacityPct)) * 255 / 100;
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    } // --- Fin del metodo withAlpha ---

    private static Color lerp(Color a, Color b, double t) {
        if (t <= 0) return a;
        if (t >= 1) return b;
        return new Color(
                a.getRed() + (int) Math.round((b.getRed() - a.getRed()) * t),
                a.getGreen() + (int) Math.round((b.getGreen() - a.getGreen()) * t),
                a.getBlue() + (int) Math.round((b.getBlue() - a.getBlue()) * t),
                a.getAlpha() + (int) Math.round((b.getAlpha() - a.getAlpha()) * t));
    } // --- Fin del metodo lerp ---

    private static int lerpToArgb(Color a, Color b, double t) {
        if (t <= 0) return a.getRGB();
        if (t >= 1) return b.getRGB();
        int ar = a.getAlpha() + (int) Math.round((b.getAlpha() - a.getAlpha()) * t);
        int rr = a.getRed() + (int) Math.round((b.getRed() - a.getRed()) * t);
        int gg = a.getGreen() + (int) Math.round((b.getGreen() - a.getGreen()) * t);
        int bb = a.getBlue() + (int) Math.round((b.getBlue() - a.getBlue()) * t);
        return (ar << 24) | (rr << 16) | (gg << 8) | bb;
    } // --- Fin del metodo lerpToArgb ---

    @Override
    public void paintOverlay(Graphics2D g2) {
        if (!dragging || startPoint == null || endPoint == null) return;

        g2.setColor(new Color(100, 100, 100, 80));
        g2.setStroke(new java.awt.BasicStroke(2f));
        g2.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);

        // Draw direction arrow
        int dx = endPoint.x - startPoint.x;
        int dy = endPoint.y - startPoint.y;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len > 4) {
            double nx = dx / len;
            double ny = dy / len;
            int ax = endPoint.x - (int) (nx * 8);
            int ay = endPoint.y - (int) (ny * 8);
            g2.fillOval(ax - 2, ay - 2, 4, 4);
        }
    } // --- Fin del metodo paintOverlay ---

} // --- Fin de la clase GradientTool ---
