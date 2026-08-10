package modelo.renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.scene.shape.TriangleMesh;

/**
 * Convierte una lista de Triangle del parser STL en un TriangleMesh de JavaFX.
 * Los vértices coincidentes se sueldan y las normales se promedian por vértice
 * para obtener un sombreado suave (Gouraud) en lugar del aspecto facetado.
 */
public class StlMeshBuilder {

    /**
     * Construye un TriangleMesh a partir de la lista de triángulos.
     */
    public static TriangleMesh build(List<Triangle> triangles) {
        TriangleMesh mesh = new TriangleMesh();

        int n = triangles.size();
        int[] faces = new int[n * 6];

        Map<VertexKey, Integer> vertexIndex = new HashMap<>();
        List<float[]> vertexData = new ArrayList<>();
        int[] normAccum = new int[n * 9];

        int fi = 0;
        for (int i = 0; i < n; i++) {
            Triangle t = triangles.get(i);
            float[] faceNormal = computeFaceNormal(t);
            int[] vi = new int[3];
            float[][] verts = new float[][]{t.v0, t.v1, t.v2};
            for (int k = 0; k < 3; k++) {
                VertexKey key = new VertexKey(verts[k]);
                Integer idx = vertexIndex.get(key);
                int viK;
                if (idx == null) {
                    viK = vertexIndex.size();
                    vertexIndex.put(key, viK);
                    vertexData.add(verts[k]);
                } else {
                    viK = idx;
                }
                vi[k] = viK;
            }

            for (int k = 0; k < 3; k++) {
                faces[fi++] = vi[k];
                faces[fi++] = 0;
            }

            for (int k = 0; k < 3; k++) {
                int vk = vi[k] * 3;
                normAccum[vk]     += faceNormal[0];
                normAccum[vk + 1] += faceNormal[1];
                normAccum[vk + 2] += faceNormal[2];
            }
        }

        int vertexCount = vertexIndex.size();
        float[] normOut = new float[vertexCount * 3];
        for (int i = 0; i < vertexCount; i++) {
            int base = i * 3;
            float nx = normAccum[base], ny = normAccum[base + 1], nz = normAccum[base + 2];
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 0.001f) {
                normOut[base] = nx / len;
                normOut[base + 1] = ny / len;
                normOut[base + 2] = nz / len;
            } else {
                normOut[base] = 0;
                normOut[base + 1] = 0;
                normOut[base + 2] = 1;
            }
        }

        float[] pointsOut = new float[vertexCount * 3];
        for (int i = 0; i < vertexCount; i++) {
            float[] v = vertexData.get(i);
            pointsOut[i * 3] = v[0];
            pointsOut[i * 3 + 1] = v[1];
            pointsOut[i * 3 + 2] = v[2];
        }

        mesh.getPoints().setAll(pointsOut);
        mesh.getNormals().setAll(normOut);
        mesh.getTexCoords().setAll(new float[]{0, 0});
        mesh.getFaces().setAll(faces);

        return mesh;
    } // --- Fin del metodo build ---


    /**
     * Clave de vértice para soldar posiciones coincidentes con redondeo.
     */
    private static final class VertexKey {
        private final long x;
        private final long y;
        private final long z;

        VertexKey(float[] v) {
            x = Math.round(v[0] * 1e4f);
            y = Math.round(v[1] * 1e4f);
            z = Math.round(v[2] * 1e4f);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof VertexKey)) return false;
            VertexKey k = (VertexKey) o;
            return x == k.x && y == k.y && z == k.z;
        }

        @Override
        public int hashCode() {
            int result = (int) (x ^ (x >>> 32));
            result = 31 * result + (int) (y ^ (y >>> 32));
            result = 31 * result + (int) (z ^ (z >>> 32));
            return result;
        }
    }


    /**
     * Calcula la normal de la cara de un triángulo a partir de sus aristas.
     */
    private static float[] computeFaceNormal(Triangle t) {
        float[] e1 = { t.v1[0] - t.v0[0], t.v1[1] - t.v0[1], t.v1[2] - t.v0[2] };
        float[] e2 = { t.v2[0] - t.v0[0], t.v2[1] - t.v0[1], t.v2[2] - t.v0[2] };
        float nx = e1[1] * e2[2] - e1[2] * e2[1];
        float ny = e1[2] * e2[0] - e1[0] * e2[2];
        float nz = e1[0] * e2[1] - e1[1] * e2[0];
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0.001f) { nx /= len; ny /= len; nz /= len; }
        return new float[]{nx, ny, nz};
    } // --- Fin del metodo computeFaceNormal ---


    /**
     * Calcula el factor de escala para que el modelo mida ~200 unidades en su eje mayor.
     */
    public static float computeScale(List<Triangle> triangles) {
        float[] bb = boundingBox(triangles);
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
            for (float[] v : new float[][]{t.v0, t.v1, t.v2}) {
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
