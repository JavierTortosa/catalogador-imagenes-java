package servicios.renderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

import modelo.renderer.Triangle;

public class AwtModelRenderer implements ModelRenderer {

    public static final int SIZE = 512;
    public static final int MARGIN = 24;

    private int superSample = 2;

    private static final float AMBIENT = 0.30f;

    private static final float[][] LIGHTS = {
        { 0.6f, -0.8f,  0.5f, 0.50f },
        {-0.4f, -0.3f,  0.8f, 0.25f },
        { 0.0f,  0.7f, -0.7f, 0.15f },
        { 0.0f,  0.0f,  1.0f, 0.30f }
    };

    private static final float COLOR_RANGE = 210f;

    @Override
    public void render(File input, File output) throws Exception {
        List<Triangle> triangles = StlParser.parse(input);
        BufferedImage img = renderizar(triangles);
        ImageIO.write(img, "PNG", output);
    }

    public BufferedImage renderizar(List<Triangle> triangles) {
        return renderizar(triangles, 20, -25);
    }

    public BufferedImage renderizarInteractive(List<Triangle> triangles, double rotX, double rotY) {
        return renderizarInterno(triangles, rotX, rotY, 0, false, false);
    }

    public BufferedImage renderizarFast(List<Triangle> triangles, double rotX, double rotY) {
        return renderizarInterno(triangles, rotX, rotY, 0, false, true);
    }

    public BufferedImage renderizar(List<Triangle> triangles, double rotX, double rotY) {
        return renderizarInterno(triangles, rotX, rotY, 0, true, true);
    }


    /**
     * Renderiza con ajustes completos: rotación, brillo, contraste, AA, fondo,
     * wireframe opcional y encuadre (pan/zoom). Se renderiza a resolución
     * aumentada (supersampling) y se reduce a {@link #SIZE} con interpolación
     * bilineal para suavizar facetas y bordes.
     *
     * @param panX    desplazamiento horizontal en píxeles del render
     * @param panY    desplazamiento vertical en píxeles del render
     * @param zoomScale factor de zoom multiplicativo sobre la escala de encaje
     */
    public BufferedImage renderizarConAjustes(List<Triangle> triangles,
            double rotX, double rotY, double rotZ, boolean antiAlias,
            int brightness, int contrast,
            String bgMode, Color solidColor,
            Color gradientStart, Color gradientEnd,
            BufferedImage bgImage, double bgImageScale, boolean wireframe,
            double panX, double panY, double zoomScale) {
        boolean transparent = "transparent".equals(bgMode);
        int renderSize = SIZE * superSample;
        BufferedImage hiRes = new BufferedImage(renderSize, renderSize,
                transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = hiRes.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

            if (!transparent) {
                paintBackground(g, bgMode, solidColor, gradientStart, gradientEnd, bgImage, bgImageScale, renderSize);
            }

            if (triangles.isEmpty()) {
                g.setColor(Color.RED);
                g.drawString("No triangles", 10, 20);
            } else {
                float ambientLevel = 0.20f + (brightness + 100f) / 200f * 0.60f;
                float contrastScale = 0.30f + (contrast + 100f) / 200f * 0.70f;

                List<Triangle> transformed = transformTriangles(triangles, rotX, rotY, rotZ, renderSize,
                        panX, panY, zoomScale);
                transformed.sort(Comparator.comparingDouble(
                        t -> -(t.v0[2] + t.v1[2] + t.v2[2]) / 3f));

                for (Triangle t : transformed) {
                    float[] normal = normalizar(t);
                    float intensity = shade(normal, ambientLevel, contrastScale);
                    int gray = Math.round(80 + COLOR_RANGE * intensity);
                    gray = Math.max(80, Math.min(255, gray));
                    g.setColor(new Color(gray, gray, gray));
                    g.fillPolygon(new int[]{
                        Math.round(t.v0[0]), Math.round(t.v1[0]), Math.round(t.v2[0])
                    }, new int[]{
                        Math.round(t.v0[1]), Math.round(t.v1[1]), Math.round(t.v2[1])
                    }, 3);
                }

                if (wireframe) {
                    BasicStroke wireStroke = new BasicStroke(0.7f);
                    g.setStroke(wireStroke);
                    for (Triangle t : transformed) {
                        float[] normal = normalizar(t);
                        float intensity = shade(normal, ambientLevel, contrastScale);
                        int gray = Math.round(80 + COLOR_RANGE * intensity);
                        gray = Math.max(80, Math.min(255, gray));
                        int wireGray = Math.max(60, gray - 50);
                        g.setColor(new Color(wireGray, wireGray, wireGray));
                        g.drawPolygon(new int[]{
                            Math.round(t.v0[0]), Math.round(t.v1[0]), Math.round(t.v2[0])
                        }, new int[]{
                            Math.round(t.v0[1]), Math.round(t.v1[1]), Math.round(t.v2[1])
                        }, 3);
                    }
                }
            }
        } finally {
            g.dispose();
        }
        if (superSample > 1) {
            BufferedImage out = new BufferedImage(SIZE, SIZE, hiRes.getType());
            Graphics2D g2 = out.createGraphics();
            try {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(hiRes, 0, 0, SIZE, SIZE, null);
            } finally {
                g2.dispose();
            }
            return out;
        }
        return hiRes;
    }


    /**
     * Pinta el fondo según el modo seleccionado.
     */
    private void paintBackground(Graphics2D g, String bgMode,
            Color solidColor, Color gradientStart, Color gradientEnd,
            BufferedImage bgImage, double bgImageScale, int renderSize) {
        switch (bgMode) {
            case "solid":
                g.setColor(solidColor != null ? solidColor : new Color(60, 60, 65));
                g.fillRect(0, 0, renderSize, renderSize);
                break;
            case "gradient":
                Color start = gradientStart != null ? gradientStart : new Color(45, 45, 50);
                Color end = gradientEnd != null ? gradientEnd : new Color(75, 75, 80);
                GradientPaint gp = new GradientPaint(0, 0, start, 0, renderSize, end);
                g.setPaint(gp);
                g.fillRect(0, 0, renderSize, renderSize);
                break;
            case "image":
                if (bgImage != null) {
                    int w = (int) Math.round(bgImage.getWidth() * bgImageScale);
                    int h = (int) Math.round(bgImage.getHeight() * bgImageScale);
                    g.drawImage(bgImage, 0, 0, w, h, null);
                } else {
                    g.setColor(new Color(60, 60, 65));
                    g.fillRect(0, 0, renderSize, renderSize);
                }
                break;
            default:
                g.setColor(new Color(60, 60, 65));
                g.fillRect(0, 0, renderSize, renderSize);
                break;
        }
    }


    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }


    public int getSuperSample() { return superSample; }


    public void setSuperSample(int superSample) {
        this.superSample = Math.max(1, superSample);
    }


    private BufferedImage renderizarInterno(List<Triangle> triangles, double rotX, double rotY, double rotZ,
            boolean antiAlias, boolean wireframe) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

            if (triangles.isEmpty()) {
                g.setColor(Color.RED);
                g.drawString("No triangles", 10, 20);
                return img;
            }

            List<Triangle> transformed = transformTriangles(triangles, rotX, rotY, rotZ, SIZE, 0, 0, 1.0);

            transformed.sort(Comparator.comparingDouble(
                    t -> -(t.v0[2] + t.v1[2] + t.v2[2]) / 3f));

            for (Triangle t : transformed) {
                float[] normal = normalizar(t);
                float intensity = shade(normal, AMBIENT, 1f);
                int gray = Math.round(80 + COLOR_RANGE * intensity);
                gray = Math.max(80, Math.min(255, gray));
                g.setColor(new Color(gray, gray, gray));

                int[] xp = new int[]{
                    Math.round(t.v0[0]), Math.round(t.v1[0]), Math.round(t.v2[0])
                };
                int[] yp = new int[]{
                    Math.round(t.v0[1]), Math.round(t.v1[1]), Math.round(t.v2[1])
                };
                g.fillPolygon(xp, yp, 3);
            }

            if (wireframe) {
                BasicStroke wireStroke = new BasicStroke(0.7f);
                g.setStroke(wireStroke);
                for (Triangle t : transformed) {
                    float[] normal = normalizar(t);
                    float intensity = shade(normal, AMBIENT, 1f);
                    int gray = Math.round(80 + COLOR_RANGE * intensity);
                    gray = Math.max(80, Math.min(255, gray));
                    int wireGray = Math.max(60, gray - 50);
                    g.setColor(new Color(wireGray, wireGray, wireGray));

                    int[] xp = new int[]{
                        Math.round(t.v0[0]), Math.round(t.v1[0]), Math.round(t.v2[0])
                    };
                    int[] yp = new int[]{
                        Math.round(t.v0[1]), Math.round(t.v1[1]), Math.round(t.v2[1])
                    };
                    g.drawPolygon(xp, yp, 3);
                }
            }
        } finally {
            g.dispose();
        }
        return img;
    }

    private List<Triangle> transformTriangles(List<Triangle> triangles, double rotX, double rotY, double rotZ,
            int renderSize, double panX, double panY, double zoomScale) {
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = Float.MIN_VALUE;
        for (Triangle t : triangles) {
            for (float[] v : new float[][]{t.v0, t.v1, t.v2}) {
                if (v[0] < minX) minX = v[0]; if (v[0] > maxX) maxX = v[0];
                if (v[1] < minY) minY = v[1]; if (v[1] > maxY) maxY = v[1];
                if (v[2] < minZ) minZ = v[2]; if (v[2] > maxZ) maxZ = v[2];
            }
        }
        float cx = (minX + maxX) / 2f;
        float cy = (minY + maxY) / 2f;
        float cz = (minZ + maxZ) / 2f;
        float sizeX = maxX - minX;
        float sizeY = maxY - minY;
        float sizeZ = maxZ - minZ;
        float maxDim = Math.max(sizeX, Math.max(sizeY, sizeZ));
        float scale = (maxDim > 0.001f) ? (renderSize - 2f * MARGIN) / maxDim : 1f;
        scale *= (float) zoomScale;

        double az = Math.toRadians(rotZ);
        double ax = Math.toRadians(rotX);
        double ay = Math.toRadians(rotY);
        double czA = Math.cos(az), szA = Math.sin(az);
        double cxA = Math.cos(ax), sxA = Math.sin(ax);
        double cyA = Math.cos(ay), syA = Math.sin(ay);

        float offsetX = (float) (renderSize / 2f + panX);
        float offsetY = (float) (renderSize / 2f + panY);

        List<Triangle> result = new ArrayList<>(triangles.size());
        for (Triangle t : triangles) {
            float[] tv0 = xform(t.v0, cx, cy, cz, scale, czA, szA, cxA, sxA, cyA, syA, offsetX, offsetY);
            float[] tv1 = xform(t.v1, cx, cy, cz, scale, czA, szA, cxA, sxA, cyA, syA, offsetX, offsetY);
            float[] tv2 = xform(t.v2, cx, cy, cz, scale, czA, szA, cxA, sxA, cyA, syA, offsetX, offsetY);
            result.add(new Triangle(tv0, tv1, tv2, t.nx, t.ny, t.nz));
        }
        return result;
    }

    private float[] xform(float[] v, float cx, float cy, float cz, float s,
            double czA, double szA, double cxA, double sxA, double cyA, double syA,
            float offsetX, float offsetY) {
        float x = (v[0] - cx) * s;
        float y = (v[1] - cy) * s;
        float z = (v[2] - cz) * s;
        float xz = (float) (x * czA - y * szA);
        float yz = (float) (x * szA + y * czA);
        float y1 = (float) (yz * cxA - z * sxA);
        float z1 = (float) (yz * sxA + z * cxA);
        float x2 = (float) (xz * cyA + z1 * syA);
        float z2 = (float) (-xz * syA + z1 * cyA);
        return new float[]{ x2 + offsetX, y1 + offsetY, z2 };
    }


    /**
     * Devuelve la normal normalizada de un triángulo; si la normal almacenada
     * es nula la recalcula a partir de sus aristas. Devuelve null si no es
     * posible calcularla.
     */
    private float[] normalizar(Triangle t) {
        float nx = t.nx, ny = t.ny, nz = t.nz;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0.001f) { nx /= len; ny /= len; nz /= len; return new float[]{nx, ny, nz}; }

        float[] e1 = { t.v1[0] - t.v0[0], t.v1[1] - t.v0[1], t.v1[2] - t.v0[2] };
        float[] e2 = { t.v2[0] - t.v0[0], t.v2[1] - t.v0[1], t.v2[2] - t.v0[2] };
        nx = e1[1] * e2[2] - e1[2] * e2[1];
        ny = e1[2] * e2[0] - e1[0] * e2[2];
        nz = e1[0] * e2[1] - e1[1] * e2[0];
        len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len <= 0.001f) return null;
        nx /= len; ny /= len; nz /= len;
        return new float[]{nx, ny, nz};
    }


    /**
     * Calcula la intensidad de iluminación de una normal aplicando la luz
     * ambiental y las luces direccionales de {@link #LIGHTS}.
     */
    private float shade(float[] n, float ambientLevel, float contrastScale) {
        if (n == null) return ambientLevel;
        float intensity = ambientLevel;
        for (float[] light : LIGHTS) {
            float lx = light[0], ly = light[1], lz = light[2];
            float llen = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
            if (llen > 0.001f) { lx /= llen; ly /= llen; lz /= llen; }
            float dot = n[0] * lx + n[1] * ly + n[2] * lz;
            if (dot > 0) intensity += dot * light[3] * contrastScale;
        }
        return Math.min(1f, intensity);
    }

} // --- Fin de la clase AwtModelRenderer ---
