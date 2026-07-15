package modelo.renderer;

import java.util.List;

import javafx.scene.shape.TriangleMesh;

/**
 * Convierte una lista de Triangle del parser STL en un TriangleMesh de JavaFX.
 */
public class StlMeshBuilder {

    /**
     * Construye un TriangleMesh a partir de la lista de triángulos.
     */
    public static TriangleMesh build(List<Triangle> triangles) {
        TriangleMesh mesh = new TriangleMesh();

        int n = triangles.size();
        float[] points = new float[n * 9];
        float[] texCoords = new float[]{0, 0};
        int[] faces = new int[n * 6];

        for (int i = 0; i < n; i++) {
            Triangle t = triangles.get(i);
            int vi = i * 9;
            points[vi]      = t.v0[0];
            points[vi + 1]  = t.v0[1];
            points[vi + 2]  = t.v0[2];
            points[vi + 3]  = t.v1[0];
            points[vi + 4]  = t.v1[1];
            points[vi + 5]  = t.v1[2];
            points[vi + 6]  = t.v2[0];
            points[vi + 7]  = t.v2[1];
            points[vi + 8]  = t.v2[2];

            int fi = i * 6;
            int base = i * 3;
            faces[fi]     = base;
            faces[fi + 1] = 0;
            faces[fi + 2] = base + 1;
            faces[fi + 3] = 0;
            faces[fi + 4] = base + 2;
            faces[fi + 5] = 0;
        }

        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(texCoords);
        mesh.getFaces().setAll(faces);

        return mesh;
    } // --- Fin del metodo build ---


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
