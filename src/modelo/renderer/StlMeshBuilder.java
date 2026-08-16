package modelo.renderer;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.shape.TriangleMesh;

/**
 * Convierte una lista de Triangle del parser STL en un TriangleMesh de JavaFX.
 * Los vértices coincidentes se sueldan y las normales se promedian por vértice
 * para obtener un sombreado suave (Gouraud) en lugar del aspecto facetado.
 */
public class StlMeshBuilder {

    private static final Logger logger = LoggerFactory.getLogger(StlMeshBuilder.class);

    /**
     * Construye un TriangleMesh a partir de la lista de triángulos.
     * El resultado visual es idéntico a la versión anterior (mismo redondeo de
     * soldadura, mismas normales, mismo orden de caras), pero sin crear objetos
     * temporales por vértice/triángulo y dimensionando los acumuladores con el
     * número real de vértices únicos.
     */
    public static TriangleMesh build(List<Triangle> triangles) {
        long t0 = System.nanoTime();
        TriangleMesh mesh = new TriangleMesh();

        int n = triangles.size();
        int[] faces = new int[n * 6];

        // --- Soldadura de vértices (pasada 1) ---
        LongTripletMap vertexIndex = new LongTripletMap(n);
        List<float[]> vertexData = new ArrayList<>(n);
        int[] vi = new int[3];
        float[][] verts = new float[3][];
        int fi = 0;
        for (int i = 0; i < n; i++) {
            Triangle t = triangles.get(i);
            verts[0] = t.v0;
            verts[1] = t.v1;
            verts[2] = t.v2;
            for (int k = 0; k < 3; k++) {
                float[] v = verts[k];
                long kx = Math.round(v[0] * 1e4f);
                long ky = Math.round(v[1] * 1e4f);
                long kz = Math.round(v[2] * 1e4f);
                int idx = vertexIndex.get(kx, ky, kz);
                if (idx < 0) {
                    idx = vertexIndex.size();
                    vertexIndex.put(kx, ky, kz, idx);
                    vertexData.add(v);
                }
                vi[k] = idx;
                faces[fi++] = idx;
                faces[fi++] = 0;
            }
        }
        long tWelding = System.nanoTime();

        int vertexCount = vertexIndex.size();

        // --- Normales por vértice (pasada 2, acumulador de tamaño exacto) ---
        int[] normAccum = new int[vertexCount * 3];
        for (int i = 0; i < n; i++) {
            Triangle t = triangles.get(i);
            float e1x = t.v1[0] - t.v0[0];
            float e1y = t.v1[1] - t.v0[1];
            float e1z = t.v1[2] - t.v0[2];
            float e2x = t.v2[0] - t.v0[0];
            float e2y = t.v2[1] - t.v0[1];
            float e2z = t.v2[2] - t.v0[2];
            float nx = e1y * e2z - e1z * e2y;
            float ny = e1z * e2x - e1x * e2z;
            float nz = e1x * e2y - e1y * e2x;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 0.001f) {
                nx /= len;
                ny /= len;
                nz /= len;
            }
            int base = i * 6;
            for (int k = 0; k < 3; k++) {
                int vk = faces[base + k * 2] * 3;
                normAccum[vk] = (int) (normAccum[vk] + nx);
                normAccum[vk + 1] = (int) (normAccum[vk + 1] + ny);
                normAccum[vk + 2] = (int) (normAccum[vk + 2] + nz);
            }
        }
        long tNormals = System.nanoTime();

        float[] normOut = new float[vertexCount * 3];
        float[] pointsOut = new float[vertexCount * 3];
        for (int i = 0; i < vertexCount; i++) {
            int base = i * 3;
            float nx = normAccum[base];
            float ny = normAccum[base + 1];
            float nz = normAccum[base + 2];
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 0.001f) {
                normOut[base] = nx / len;
                normOut[base + 1] = ny / len;
                normOut[base + 2] = nz / len;
            } else {
                normOut[base + 2] = 1;
            }
            float[] v = vertexData.get(i);
            pointsOut[base] = v[0];
            pointsOut[base + 1] = v[1];
            pointsOut[base + 2] = v[2];
        }
        long tArrays = System.nanoTime();

        mesh.getPoints().setAll(pointsOut);
        mesh.getNormals().setAll(normOut);
        mesh.getTexCoords().setAll(new float[]{0, 0});
        mesh.getFaces().setAll(faces);
        long tSetAll = System.nanoTime();

        if (logger.isInfoEnabled()) {
            logger.info("[MESH PERF] triangles={} uniqueVertices={} welding={}ms normals={}ms arrays={}ms setAll={}ms total={}ms",
                    n, vertexCount,
                    (tWelding - t0) / 1_000_000,
                    (tNormals - tWelding) / 1_000_000,
                    (tArrays - tNormals) / 1_000_000,
                    (tSetAll - tArrays) / 1_000_000,
                    (tSetAll - t0) / 1_000_000);
        }
        return mesh;
    } // --- Fin del metodo build ---


    /**
     * Calcula el factor de escala para que el modelo mida ~200 unidades en su eje mayor.
     */
    public static float computeScale(List<Triangle> triangles) {
        return computeScale(boundingBox(triangles));
    } // --- Fin del metodo computeScale ---


    /**
     * Calcula el factor de escala a partir de una caja envolvente ya calculada.
     * Evita recorrer los triángulos dos veces cuando el llamador ya tiene la caja.
     */
    public static float computeScale(float[] bb) {
        float size = Math.max(bb[3] - bb[0], Math.max(bb[4] - bb[1], bb[5] - bb[2]));
        return size > 0.001f ? 200f / size : 1f;
    } // --- Fin del metodo computeScale ---


    /**
     * Devuelve la caja envolvente [minX, minY, minZ, maxX, maxY, maxZ] de los triángulos.
     */
    public static float[] boundingBox(List<Triangle> triangles) {
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (Triangle t : triangles) {
            for (int k = 0; k < 3; k++) {
                float[] v = k == 0 ? t.v0 : (k == 1 ? t.v1 : t.v2);
                if (v[0] < minX) minX = v[0];
                if (v[0] > maxX) maxX = v[0];
                if (v[1] < minY) minY = v[1];
                if (v[1] > maxY) maxY = v[1];
                if (v[2] < minZ) minZ = v[2];
                if (v[2] > maxZ) maxZ = v[2];
            }
        }
        return new float[]{minX, minY, minZ, maxX, maxY, maxZ};
    } // --- Fin del metodo boundingBox ---


    /**
     * Devuelve el centro geométrico de la caja envolvente.
     */
    public static float[] center(List<Triangle> triangles) {
        float[] bb = boundingBox(triangles);
        return new float[]{
            (bb[0] + bb[3]) / 2f,
            (bb[1] + bb[4]) / 2f,
            (bb[2] + bb[5]) / 2f
        };
    } // --- Fin del metodo center ---


    /**
     * Devuelve el centroide del modelo ponderado por el área de los triángulos.
     * Es más preciso que el centro geométrico para modelos asimétricos.
     */
    public static float[] centroid(List<Triangle> triangles) {
        float cx = 0, cy = 0, cz = 0;
        double totalArea = 0;

        for (Triangle t : triangles) {
            float ax = t.v0[0], ay = t.v0[1], az = t.v0[2];
            float bx = t.v1[0], by = t.v1[1], bz = t.v1[2];
            float cx_ = t.v2[0], cy_ = t.v2[1], cz_ = t.v2[2];

            float centroidX = (ax + bx + cx_) / 3f;
            float centroidY = (ay + by + cy_) / 3f;
            float centroidZ = (az + bz + cz_) / 3f;

            float e1x = bx - ax, e1y = by - ay, e1z = bz - az;
            float e2x = cx_ - ax, e2y = cy_ - ay, e2z = cz_ - az;
            float nx = e1y * e2z - e1z * e2y;
            float ny = e1z * e2x - e1x * e2z;
            float nz = e1x * e2y - e1y * e2x;
            double area = Math.sqrt(nx * nx + ny * ny + nz * nz) / 2.0;

            cx += (float)(centroidX * area);
            cy += (float)(centroidY * area);
            cz += (float)(centroidZ * area);
            totalArea += area;
        }

        if (totalArea > 0) {
            return new float[]{cx / (float)totalArea, cy / (float)totalArea, cz / (float)totalArea};
        }
        return center(triangles);
    } // --- Fin del metodo centroid ---


} // --- Fin de la clase StlMeshBuilder ---