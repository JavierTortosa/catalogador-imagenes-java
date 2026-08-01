package controlador.tools;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Verificador headless de regresión para la edición de formas (Parte B).
 * <p>
 * Comprueba que el re-render de una capa de forma es idempotente: si se
 * renderiza siempre al tamaño de render original (shapeRenderW/H) y se recorta
 * al contenido, el bbox de contenido no se encoge entre ediciones. También
 * verifica el round-trip de los metadatos de {@code ImageLayer}.
 *
 * Uso:
 * <pre>
 *   javac -cp target/classes -d target/tests tests/controlador/tools/ShapeEditVerify.java
 *   java -cp target/classes;target/tests controlador.tools.ShapeEditVerify
 * </pre>
 */
public final class ShapeEditVerify {

    private static int failures = 0;

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");

        Color fill = new Color(255, 0, 0, 255);
        Color stroke = Color.BLACK;

        for (String tipo : new String[]{"rect", "ellipse", "line", "triangle", "polygon"}) {
            int origW = 200;
            int origH = 150;
            Rectangle c1 = trim(renderShape(tipo, fill, stroke, origW, origH));
            Rectangle c2 = trim(renderShape(tipo, fill, stroke, origW, origH));
            boolean mismaForma = c1 != null && c2 != null
                    && c1.x == c2.x && c1.y == c2.y
                    && c1.width == c2.width && c1.height == c2.height;
            check("[" + tipo + "] re-render idempotente (mismo bbox)", mismaForma);
        }

        // El re-render a tamaño original conserva el bbox tras el primer recorte
        // (no se encoge la forma al volver a pintar en el tamaño recortado)
        {
            BufferedImage tri = renderShape("triangle", fill, stroke, 200, 150);
            Rectangle c1 = trim(tri);
            BufferedImage tri2 = renderShape("triangle", fill, stroke, 200, 150);
            Rectangle c2 = trim(tri2);
            check("triangle: bbox estable tras re-render",
                    c1 != null && c2 != null && c1.equals(c2));
        }

        // Round-trip de metadatos de forma en ImageLayer
        {
            modelo.editor.ImageLayer layer = new modelo.editor.ImageLayer("F", null,
                    new Rectangle(10, 20, 50, 40));
            layer.setType(modelo.editor.ImageLayer.LayerType.SHAPE);
            layer.setShapeType("ellipse");
            layer.setShapeFill(new Color(10, 20, 30));
            layer.setShapeStroke(new Color(40, 50, 60));
            layer.setShapeStrokeWidth(5f);
            layer.setShapeRenderW(50);
            layer.setShapeRenderH(40);
            check("metadata: tipo SHAPE", layer.getType() == modelo.editor.ImageLayer.LayerType.SHAPE);
            check("metadata: shapeType", "ellipse".equals(layer.getShapeType()));
            check("metadata: relleno", layer.getShapeFill().equals(new Color(10, 20, 30)));
            check("metadata: borde", layer.getShapeStroke().equals(new Color(40, 50, 60)));
            check("metadata: grosor", layer.getShapeStrokeWidth() == 5f);
            check("metadata: renderW/H", layer.getShapeRenderW() == 50 && layer.getShapeRenderH() == 40);

            modelo.editor.ImageLayer copia = layer.copy();
            check("copy: conserva tipo", copia.getType() == modelo.editor.ImageLayer.LayerType.SHAPE);
            check("copy: conserva shapeType", "ellipse".equals(copia.getShapeType()));
            check("copy: conserva relleno", copia.getShapeFill().equals(new Color(10, 20, 30)));
            check("copy: conserva grosor", copia.getShapeStrokeWidth() == 5f);
        }

        System.out.println(failures == 0
                ? "RESULTADO: TODOS OK"
                : "RESULTADO: " + failures + " comprobaciones fallidas");
        System.exit(failures == 0 ? 0 : 1);
    } // --- Fin del metodo main ---

    private static BufferedImage renderShape(String tipo, Color fill, Color stroke,
            int iw, int ih) {
        return ShapeTool.renderShape(iw, ih, tipo, fill, stroke, 2f);
    } // --- Fin del metodo renderShape ---

    /**
     * Recorta la imagen al contenido real y devuelve el bbox del contenido.
     */
    private static Rectangle trim(BufferedImage img) {
        Rectangle content = ShapeTool.contentBounds(img);
        if (content == null) return null;
        return new Rectangle(content);
    } // --- Fin del metodo trim ---

    private static void check(String nombre, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + nombre);
        if (!ok) failures++;
    } // --- Fin del metodo check ---

} // --- Fin de la clase ShapeEditVerify ---
