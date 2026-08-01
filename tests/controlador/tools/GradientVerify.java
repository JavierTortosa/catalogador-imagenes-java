package controlador.tools;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Verificador headless de regresión para {@link GradientTool}.
 * <p>
 * Renderiza cada tipo de degradado y comprueba que NO sea un color plano, que
 * no haya transparencia y que Angular/Reflejado sigan el comportamiento
 * esperado. No forma parte del build Maven: se compila y ejecuta a mano.
 *
 * Uso:
 * <pre>
 *   javac -cp target/classes -d target/tests tests/controlador/tools/GradientVerify.java
 *   java -cp target/classes;target/tests controlador.tools.GradientVerify
 * </pre>
 */
public final class GradientVerify {

    private static int failures = 0;

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");

        int cw = 500;
        int ch = 400;
        int x1 = 100, y1 = 80;
        int x2 = 300, y2 = 200;
        Color c1 = Color.YELLOW;
        Color c2 = Color.BLUE;

        for (String tipo : new String[]{"Lineal", "Radial", "Angular", "Reflejado", "Diamante"}) {
            BufferedImage img = GradientTool.renderGradient(cw, ch, x1, y1, x2, y2, tipo, c1, c2);
            int variacion = variation(img);
            boolean opaco = cornersOpaque(img);
            check("[" + tipo + "] variacion > 100 (no plano): " + variacion, variacion > 100);
            check("[" + tipo + "] esquinas opacas", opaco);
        }

        // Angular: el color c1 debe aparecer en la dirección del arrastre y distinto a 180°
        {
            BufferedImage img = GradientTool.renderGradient(cw, ch, x1, y1, x2, y2, "Angular", c1, c2);
            double ang = Math.atan2(y2 - y1, x2 - x1);
            int r = 120;
            // Punto A: ~5° dentro del sector inicial (cerca de c1, sin caer en la costura)
            double aA = ang + 0.08;
            // Punto B: opuesto al arrastre (~180°), color intermedio
            double aB = ang + Math.PI;
            int ax = Math.max(0, Math.min(cw - 1, x1 + (int) (r * Math.cos(aA))));
            int ay = Math.max(0, Math.min(ch - 1, y1 + (int) (r * Math.sin(aA))));
            int bx = Math.max(0, Math.min(cw - 1, x1 + (int) (r * Math.cos(aB))));
            int by = Math.max(0, Math.min(ch - 1, y1 + (int) (r * Math.sin(aB))));
            int pxA = img.getRGB(ax, ay);
            int pxB = img.getRGB(bx, by);
            boolean cercaDeC1 = dist(pxA, c1.getRGB()) < 80;
            boolean difiere = dist(pxA, pxB) > 100;
            check("Angular: c1 en direccion del arrastre", cercaDeC1);
            check("Angular: opuesto al arrastre es distinto", difiere);
        }

        // Reflejado: simetría de espejo f(1-u) = f(1+u) respecto al punto final
        {
            BufferedImage img = GradientTool.renderGradient(cw, ch, x1, y1, x2, y2, "Reflejado", c1, c2);
            int dx = x2 - x1, dy = y2 - y1;
            int t0 = img.getRGB(x1, y1);
            int t1 = img.getRGB(x2, y2);
            int ux = x1 + (int) (0.6 * dx), uy = y1 + (int) (0.6 * dy);
            int vx = x1 + (int) (1.4 * dx), vy = y1 + (int) (1.4 * dy);
            int fu = img.getRGB(ux, uy);
            int fv = img.getRGB(vx, vy);
            check("Reflejado: inicio ~ c1 (amarillo)", dist(t0, c1.getRGB()) < 80);
            check("Reflejado: extremo ~ c2 (azul)", dist(t1, c2.getRGB()) < 80);
            check("Reflejado: simetria espejo f(0.6)~f(1.4)", dist(fu, fv) < 40);
        }

        System.out.println(failures == 0
                ? "RESULTADO: TODOS OK"
                : "RESULTADO: " + failures + " comprobaciones fallidas");
        System.exit(failures == 0 ? 0 : 1);
    } // --- Fin del metodo main ---

    private static void check(String nombre, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + nombre);
        if (!ok) failures++;
    } // --- Fin del metodo check ---

    private static int variation(BufferedImage img) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int y = 0; y < img.getHeight(); y += 8) {
            for (int x = 0; x < img.getWidth(); x += 8) {
                int rgb = img.getRGB(x, y) & 0x00FFFFFF;
                int v = ((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF);
                if (v < min) min = v;
                if (v > max) max = v;
            }
        }
        return max - min;
    } // --- Fin del metodo variation ---

    private static boolean cornersOpaque(BufferedImage img) {
        int w = img.getWidth() - 1;
        int h = img.getHeight() - 1;
        return (img.getRGB(0, 0) >>> 24) == 255
                && (img.getRGB(w, 0) >>> 24) == 255
                && (img.getRGB(0, h) >>> 24) == 255
                && (img.getRGB(w, h) >>> 24) == 255;
    } // --- Fin del metodo cornersOpaque ---

    private static int dist(int a, int b) {
        int dr = Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF));
        int dg = Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF));
        int db = Math.abs((a & 0xFF) - (b & 0xFF));
        return dr + dg + db;
    } // --- Fin del metodo dist ---

} // --- Fin de la clase GradientVerify ---
