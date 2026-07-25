package controlador.managers;

import java.awt.Color;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.renderer.StlMeshBuilder;
import modelo.renderer.Triangle;
import vista.panels.render.PreviewPanel3DFX;

/**
 * Controlador delegado encargado exclusivamente de gestionar la escena 3D (JavaFX),
 * mallas de triángulos STL, iluminación, cámara y ajustes de previsualización.
 */
public class RenderSceneController {

    private static final Logger logger = LoggerFactory.getLogger(RenderSceneController.class);

    private final PreviewPanel3DFX preview3DFX;

    public RenderSceneController(PreviewPanel3DFX preview3DFX) {
        this.preview3DFX = preview3DFX;
    } // --- Fin del constructor RenderSceneController ---

    public void cargarMalla3D(List<Triangle> triangles, int brightness, int contrast,
                             boolean checkerboard, boolean antiAlias, boolean crosshair) {
        if (triangles == null || triangles.isEmpty()) {
            limpiarEscena();
            return;
        }
        preview3DFX.setMesh(triangles);
        preview3DFX.setBrightness(brightness);
        preview3DFX.setContrast(contrast);
        preview3DFX.setCheckerboard(checkerboard);
        preview3DFX.setAntiAlias(antiAlias);
        preview3DFX.setCrosshairVisible(crosshair);

        float[] bb = StlMeshBuilder.boundingBox(triangles);
        logger.info("[RenderSceneController] Malla 3D cargada. Dimensiones x: {} y: {} z: {}",
                String.format("%.2f", bb[3] - bb[0]),
                String.format("%.2f", bb[4] - bb[1]),
                String.format("%.2f", bb[5] - bb[2]));
    } // --- Fin del metodo cargarMalla3D ---

    public void limpiarEscena() {
        if (preview3DFX != null) {
            preview3DFX.clearMesh();
        }
    } // --- Fin del metodo limpiarEscena ---

    public void aplicarAjustes(int brightness, int contrast, boolean antiAlias, boolean crosshair) {
        if (preview3DFX != null) {
            preview3DFX.setBrightness(brightness);
            preview3DFX.setContrast(contrast);
            preview3DFX.setAntiAlias(antiAlias);
            preview3DFX.setCrosshairVisible(crosshair);
        }
    } // --- Fin del metodo aplicarAjustes ---

    public void resetearRotacionYCamara() {
        if (preview3DFX != null) {
            preview3DFX.resetView();
        }
    } // --- Fin del metodo resetearRotacionYCamara ---

} // --- Fin de la clase RenderSceneController ---
