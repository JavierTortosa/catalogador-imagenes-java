package controlador.tools;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Verificador headless de regresión para {@link ShapeTool}.
 * <p>
 * Comprueba que cada forma se renderiza (no transparente), que el recorte al
 * contenido real reduce el bbox cuando hay esquinas vacías (triángulo, hexágono,
 * línea) y que un rectángulo/elipse llenos no se recortan.
 *
 * Uso:
 * <pre>
 *   javac -cp target/classes -d target/tests tests/controlador/tools/ShapeVerify.java
 *   java -cp target/classes;target/tests controlador.tools.ShapeVerify
 * </pre>
 */
public final class ShapeVerify {

    private static int failures = 0;

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");

        Color fill = new Color(255, 0, 0, 255);
        Color stroke = Color.BLACK;

        for (String tipo : new String[]{"rect", "ellipse", "line", "triangle", "polygon"}) {
            BufferedImage img = ShapeTool.renderShape(200, 150, tipo, fill, stroke, 2f);
            boolean hayContenido = contentNonTransparent(img);
            check("[" + tipo + "] se renderiza (hay pixeles)", hayContenido);
            Rectangle cb = ShapeTool.contentBounds(img);
            check("[" + tipo + "] contentBounds no nulo", cb != null);
            if (cb != null) {
                check("[" + tipo + "] contentBounds dentro de la imagen",
                        cb.x >= 0 && cb.y >= 0
                                && cb.x + cb.width <= 200 && cb.y + cb.height <= 150);
            }
        }

        // Recorte: el triángulo deja esquinas vacías → bbox más pequeño que el lienzo
        {
            BufferedImage tri = ShapeTool.renderShape(200, 150, "triangle", fill, stroke, 2f);
            Rectangle cb = ShapeTool.contentBounds(tri);
            boolean recorta = cb != null && (cb.width < 200 || cb.height < 150);
            check("triangle: bbox recortado (esquinas vacias)", recorta);
        }

        // Sin recorte: rectángulo relleno ocupa todo el lienzo
        {
            BufferedImage rect = ShapeTool.renderShape(200, 150, "rect", fill, stroke, 2f);
            Rectangle cb = ShapeTool.contentBounds(rect);
            check("rect: bbox ocupa todo el lienzo",
                    cb != null && cb.width == 200 && cb.height == 150);
        }

        // Círculo con Shift: lienzo cuadrado y la elipse inscrita casi lo llena
        {
            BufferedImage circ = ShapeTool.renderShape(200, 200, "ellipse", fill, stroke, 2f);
            Rectangle cb = ShapeTool.contentBounds(circ);
            boolean casiCuadrado = cb != null && cb.width >= 190 && cb.height >= 190;
            check("ellipse en lienzo cuadrado: contenido casi cuadrado", casiCuadrado);
        }

        System.out.println(failures == 0
                ? "RESULTADO: TODOS OK"
                : "RESULTADO: " + failures + " comprobaciones fallidas");
        System.exit(failures == 0 ? 0 : 1);
    } // --- Fin del metodo main ---

    private static boolean contentNonTransparent(BufferedImage img) {
        for (int y = 0; y < img.getHeight(); y += 4) {
            for (int x = 0; x < img.getWidth(); x += 4) {
                if ((img.getRGB(x, y) >>> 24) != 0) return true;
            }
        }
        return false;
    } // --- Fin del metodo contentNonTransparent ---

    private static void check(String nombre, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + nombre);
        if (!ok) failures++;
    } // --- Fin del metodo check ---

} // --- Fin de la clase ShapeVerify ---
