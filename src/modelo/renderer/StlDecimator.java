package modelo.renderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Decima una lista de triángulos mediante "vertex clustering" sobre una rejilla
 * uniforme: los vértices que caen en la misma celda se fusionan en su centroide
 * y las caras degeneradas se descartan. El resultado conserva la forma global
 * del modelo con una fracción de los triángulos, suficiente para una
 * previsualización interactiva fluida.
 */
public class StlDecimator {

    /**
     * Reduce la geometría a un número aproximado de triángulos objetivo.
     * Devuelve la misma lista sin modificar si ya está por debajo del objetivo.
     */
    public static List<Triangle> decimate(List<Triangle> triangles, int targetTriangles) {
        int n = triangles.size();
        if (n <= targetTriangles || targetTriangles <= 0) {
            return triangles;
        }

        float[] bb = StlMeshBuilder.boundingBox(triangles);
        float sx = bb[3] - bb[0];
        float sy = bb[4] - bb[1];
        float sz = bb[5] - bb[2];
        float maxDim = Math.max(sx, Math.max(sy, sz));
        if (maxDim <= 1e-6f) {
            return triangles;
        }

        // Tamaño de celda derivado del área de la caja envolvente para que el
        // resultado ronde el objetivo (nº de caras ≈ área / celda²), con límites
        // que evitan tanto un detalle excesivo como un borrado total.
        float boxSurf = 2f * (sx * sy + sx * sz + sy * sz);
        float cellSize = boxSurf > 0 ? (float) Math.sqrt(boxSurf / targetTriangles) : maxDim / 64f;
        cellSize = Math.max(cellSize, maxDim / 2048f);
        cellSize = Math.min(cellSize, maxDim / 32f);

        LongTripletMap cellMap = new LongTripletMap(n);
        List<float[]> reps = new ArrayList<>(Math.max(64, targetTriangles));
        float[] sums = new float[4096 * 3];
        int[] counts = new int[4096];
        int[] faceCells = new int[n * 3];

        for (int i = 0; i < n; i++) {
            Triangle t = triangles.get(i);
            for (int k = 0; k < 3; k++) {
                float[] v = k == 0 ? t.v0 : (k == 1 ? t.v1 : t.v2);
                long cx = (long) Math.floor((v[0] - bb[0]) / cellSize);
                long cy = (long) Math.floor((v[1] - bb[1]) / cellSize);
                long cz = (long) Math.floor((v[2] - bb[2]) / cellSize);
                int id = cellMap.get(cx, cy, cz);
                if (id < 0) {
                    id = cellMap.size();
                    cellMap.put(cx, cy, cz, id);
                    if (id >= counts.length) {
                        int newLen = counts.length;
                        while (newLen <= id) newLen <<= 1;
                        float[] newSums = new float[newLen * 3];
                        System.arraycopy(sums, 0, newSums, 0, counts.length * 3);
                        sums = newSums;
                        int[] newCounts = new int[newLen];
                        System.arraycopy(counts, 0, newCounts, 0, counts.length);
                        counts = newCounts;
                    }
                    reps.add(null);
                }
                sums[id * 3] += v[0];
                sums[id * 3 + 1] += v[1];
                sums[id * 3 + 2] += v[2];
                counts[id]++;
                faceCells[i * 3 + k] = id;
            }
        }

        int ncells = cellMap.size();
        for (int id = 0; id < ncells; id++) {
            float inv = 1f / counts[id];
            float[] rep = new float[]{
                sums[id * 3] * inv,
                sums[id * 3 + 1] * inv,
                sums[id * 3 + 2] * inv
            };
            reps.set(id, rep);
        }

        List<Triangle> out = new ArrayList<>(Math.max(64, targetTriangles));
        for (int i = 0; i < n; i++) {
            int a = faceCells[i * 3];
            int b = faceCells[i * 3 + 1];
            int c = faceCells[i * 3 + 2];
            if (a == b || b == c || a == c) continue;
            out.add(new Triangle(reps.get(a), reps.get(b), reps.get(c), 0f, 0f, 0f));
        }
        return out.isEmpty() ? triangles : out;
    } // --- Fin del metodo decimate ---


} // --- Fin de la clase StlDecimator ---