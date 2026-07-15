package modelo.renderer;

/**
 * Triángulo 3D con sus tres vértices y la normal del triángulo.
 */
public class Triangle {

    public final float[] v0, v1, v2;
    public final float nx, ny, nz;

    public Triangle(float[] v0, float[] v1, float[] v2, float nx, float ny, float nz) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
    }

} // --- Fin de la clase Triangle ---
