package vista.panels.render;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.configurations.Antialiasing;
import net.coobird.thumbnailator.resizers.configurations.Rendering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.DirectionalLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.paint.Stop;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Line;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import modelo.renderer.StlMeshBuilder;
import modelo.renderer.Triangle;

/**
 * JFXPanel que renderiza una escena JavaFX 3D con un modelo STL.
 * Gestiona cámara, iluminación, rotación del modelo y zoom.
 */
public class PreviewPanel3DFX extends JFXPanel {
	
	private static final Logger logger = LoggerFactory.getLogger(PreviewPanel3DFX.class);

    private static final long serialVersionUID = 1L;
    private StackPane root;
    private Group modelGroup;
    private Group rotationGroup;
    private MeshView meshView;
    private PhongMaterial material;
    private PerspectiveCamera camera;
    private AmbientLight ambientLight;
    private DirectionalLight dirLight;
    private DirectionalLight fillLight;
    private DirectionalLight fillLight2;
    private Line hLine;
    private Line vLine;
    private Rectangle bgRect;
    private Text loadingText;

    private Rotate rotateX;
    private Rotate rotateY;
    private Rotate rotateZ;
    private double zoom = 500;

    private double pressX, pressY;
    private SceneAntialiasing currentAA;
    private double pressAngX, pressAngY, pressAngZ;
    private double pressCamX, pressCamY;
    private double fitZoom = 500;
    private MouseButton dragButton;

    private boolean checkerboard;
    private boolean sceneInitialized;
    private SubScene subScene;
    private Group subRoot;
    private LinearGradient backgroundGradient;
    private int fillLight2Intensity = 40;
    
    
    // --- Background mode support ---
    public enum BgMode { SOLID, GRADIENT, IMAGE, TRANSPARENT }

    private BgMode bgMode = BgMode.GRADIENT;
    private javafx.scene.paint.Color solidColor = javafx.scene.paint.Color.rgb(60, 60, 65);
    private javafx.scene.paint.Color gradientStartColor = javafx.scene.paint.Color.rgb(45, 45, 50);
    private javafx.scene.paint.Color gradientEndColor = javafx.scene.paint.Color.rgb(75, 75, 80);
    private javafx.scene.image.Image bgImage;
    private double bgImageScale = 1.0;

    /**
     * Constructor: inicializa el panel cuando recibe un tamaño válido.
     */
    public PreviewPanel3DFX() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (!sceneInitialized && getWidth() > 0 && getHeight() > 0) {
                    sceneInitialized = true;
                    Platform.runLater(() -> {
                        try {
                            initScene();
                        } catch (Exception ex) {
                            logger.error("[PreviewPanel3DFX] Error en initScene desde constructor", ex);
                        }
                    });
                }
            }
        });
    } // --- Fin del constructor PreviewPanel3DFX ---


    /**
     * Inicializa la escena JavaFX 3D con fondo, luces, cámara y SubScene.
     */
    private void initScene() {
    	logger.info("[PreviewPanel3DFX] initScene iniciando... ancho=" + (int)getWidth() + " alto=" + (int)getHeight());
        try {
        root = new StackPane();

        bgRect = new Rectangle();
        backgroundGradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(45, 45, 50)),
            new Stop(1, Color.rgb(75, 75, 80)));
        bgRect.setFill(backgroundGradient);
        bgRect.widthProperty().bind(root.widthProperty());
        bgRect.heightProperty().bind(root.heightProperty());

        loadingText = new Text("Selecciona un thumbnail");
        loadingText.setFill(Color.rgb(120, 120, 130));
        loadingText.setFont(Font.font(14));

        ambientLight = new AmbientLight(Color.rgb(90, 90, 90));
        dirLight = new DirectionalLight(Color.rgb(180, 180, 180));
        dirLight.setDirection(new Point3D(-1, -0.5, -1));
        fillLight = new DirectionalLight(Color.rgb(80, 80, 100));
        fillLight.setDirection(new Point3D(0.5, 0.5, 0.5));
        fillLight2 = new DirectionalLight(Color.rgb(70, 70, 90));
        fillLight2.setDirection(new Point3D(-0.7, 0.8, 0.4));
        fillLight2.setVisible(false);

        modelGroup = new Group();
        modelGroup.setVisible(false);

        rotateX = new Rotate(0, Rotate.X_AXIS);
        rotateY = new Rotate(0, Rotate.Y_AXIS);
        rotateZ = new Rotate(0, Rotate.Z_AXIS);

        rotationGroup = new Group();
        rotationGroup.getTransforms().addAll(rotateZ, rotateY, rotateX);
        rotationGroup.getChildren().add(modelGroup);

        material = new PhongMaterial();
        material.setDiffuseColor(Color.rgb(180, 180, 180));
        material.setSpecularColor(Color.rgb(40, 40, 40));
        material.setSpecularPower(30);

        meshView = new MeshView();
        meshView.setMaterial(material);
        modelGroup.getChildren().add(meshView);

        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        camera.setTranslateZ(-zoom);

        subRoot = new Group();
        subRoot.getChildren().addAll(ambientLight, dirLight, fillLight, fillLight2, rotationGroup);

        subScene = new SubScene(subRoot, getWidth(), getHeight(), true, SceneAntialiasing.DISABLED);
        subScene.setCamera(camera);
        subScene.setFill(Color.TRANSPARENT);
        subScene.widthProperty().bind(root.widthProperty());
        subScene.heightProperty().bind(root.heightProperty());

        subScene.setOnMousePressed(this::onMousePressed);
        subScene.setOnMouseDragged(this::onMouseDragged);
        subScene.setOnScroll(this::onScroll);

        hLine = new Line(-1000, 0, 1000, 0);
        hLine.setStroke(Color.LIME);
        hLine.setStrokeWidth(1);
        vLine = new Line(0, -1000, 0, 1000);
        vLine.setStroke(Color.LIME);
        vLine.setStrokeWidth(1);
        root.getChildren().addAll(bgRect, subScene, hLine, vLine, loadingText);
        StackPane.setAlignment(loadingText, Pos.CENTER);

        Scene scene = new Scene(root, Color.rgb(30, 30, 35));

        logger.debug("[PreviewPanel3DFX] Llamando setScene...");
        setScene(scene);
        logger.debug("[PreviewPanel3DFX] initScene completado. Scene set=" + (getScene() != null)
            + " size=" + getWidth() + "x" + getHeight());
        Platform.runLater(() -> {
            logger.debug("[PreviewPanel3DFX] Post-initScene check: scene=" + getScene()
                + " root.children=" + root.getChildren().size()
                + " bgRect.fill=" + bgRect.getFill()
                + " scene.fill=" + scene.getFill());
        });
        } catch (Exception e) {
            logger.error("[PreviewPanel3DFX] ERROR en initScene", e);
        }
    } // --- Fin del metodo initScene ---


    /**
     * Asigna un nuevo modelo a partir de la lista de triángulos.
     */
    public void setMesh(List<Triangle> triangles) {
        logger.debug("[PreviewPanel3DFX] setMesh llamado: " + (triangles != null ? triangles.size() + " tri" + (char)225 + "ngulos" : "null")
            + " | JFXPanel size=" + getWidth() + "x" + getHeight()
            + " | scene set=" + (getScene() != null));
        Platform.runLater(() -> {
            try {
                doSetMesh(triangles);
            } catch (Exception e) {
                logger.error("[PreviewPanel3DFX] Excepción en doSetMesh", e);
            }
        });
    } // --- Fin del metodo setMesh ---


    /**
     * Construye el TriangleMesh, centra el modelo y ajusta la vista.
     */
    private void doSetMesh(List<Triangle> triangles) {
        if (triangles == null || triangles.isEmpty()) return;

        modelGroup.getTransforms().clear();
        modelGroup.setTranslateX(0);
        modelGroup.setTranslateY(0);
        modelGroup.setTranslateZ(0);

        TriangleMesh mesh = StlMeshBuilder.build(triangles);
        meshView.setMesh(mesh);

        float[] bb = StlMeshBuilder.boundingBox(triangles);
        float centerX = (bb[0] + bb[3]) / 2.0f;
        float centerY = (bb[1] + bb[4]) / 2.0f;
        float centerZ = (bb[2] + bb[5]) / 2.0f;
        float scale = StlMeshBuilder.computeScale(triangles);

        meshView.setTranslateX(-centerX);
        meshView.setTranslateY(-centerY);
        meshView.setTranslateZ(-centerZ);

        modelGroup.setScaleX(scale);
        modelGroup.setScaleY(scale);
        modelGroup.setScaleZ(scale);

        rotateX.setAngle(0);
        rotateY.setAngle(0);
        rotateZ.setAngle(0);

        double width = (bb[3] - bb[0]) * scale;
        double height = (bb[4] - bb[1]) * scale;
        double depth = (bb[5] - bb[2]) * scale;
        fitToView(new javafx.geometry.BoundingBox(0, 0, 0, width, height, depth));

        modelGroup.setVisible(true);
        loadingText.setVisible(false);
    } // --- Fin del metodo doSetMesh ---


    /**
     * Limpia el modelo actual y vuelve al estado inicial.
     */
    public void clearMesh() {
        Platform.runLater(() -> {
            try {
                meshView.setMesh(null);
                modelGroup.setVisible(false);
                loadingText.setVisible(true);
            } catch (Exception e) {
                logger.error("[PreviewPanel3DFX] Error en clearMesh", e);
            }
        });
    } // --- Fin del metodo clearMesh ---


    /**
     * Ajusta la intensidad de la luz ambiental.
     */
    public void setBrightness(int value) {
        Platform.runLater(() -> {
            try {
                float level = 0.2f + (value + 100f) / 200f * 0.6f;
                int c = Math.round(Math.max(0, Math.min(255, level * 255)));
                ambientLight.setColor(Color.rgb(c, c, c));
            } catch (Exception e) {
                logger.error("[PreviewPanel3DFX] Error en setBrightness", e);
            }
        });
    } // --- Fin del metodo setBrightness ---


    /**
     * Ajusta el contraste (intensidad de la luz direccional y brillo especular).
     */
    public void setContrast(int value) {
        Platform.runLater(() -> {
            try {
                float level = 0.3f + (value + 100f) / 200f * 0.7f;
                int c = Math.round(Math.max(0, Math.min(255, level * 255)));
                dirLight.setColor(Color.rgb(c, c, c));
                float specPower = 5f + (value + 100f) / 200f * 95f;
                material.setSpecularPower(specPower);
            } catch (Exception e) {
                logger.error("[PreviewPanel3DFX] Error en setContrast", e);
            }
        });
    } // --- Fin del metodo setContrast ---


    public void setCrosshairVisible(boolean visible) {
        Platform.runLater(() -> {
            if (hLine != null) hLine.setVisible(visible);
            if (vLine != null) vLine.setVisible(visible);
        });
    } // --- Fin del metodo setCrosshairVisible ---


    /**
     * Alterna entre modo relleno (sombreado suave) y modo alambre (wireframe)
     * en el preview 3D.
     */
    public void setWireframe(boolean wireframe) {
        Platform.runLater(() -> {
            try {
                meshView.setDrawMode(wireframe ? DrawMode.LINE : DrawMode.FILL);
            } catch (Exception e) {
                logger.error("[PreviewPanel3DFX] Error en setWireframe", e);
            }
        });
    } // --- Fin del metodo setWireframe ---


    /**
     * Activa o desactiva la luz tenue de relleno inferior-izquierda.
     */
    public void setFillLight2Visible(boolean visible) {
        Platform.runLater(() -> {
            if (fillLight2 != null) fillLight2.setVisible(visible);
        });
    } // --- Fin del metodo setFillLight2Visible ---


    /**
     * Ajusta la intensidad de la luz tenue de relleno (0-100). El valor se
     * escala linealmente sobre el color base de la luz y se aplica aunque la
     * luz esté visible o no. En 40 se reproduce el aspecto tenue original;
     * en 100 la luz resulta claramente perceptible.
     *
     * @param value intensidad en el rango 0-100
     */
    public void setFillLight2Intensity(int value) {
        this.fillLight2Intensity = Math.max(0, Math.min(100, value));
        double f = fillLight2Intensity / 100.0;
        Platform.runLater(() -> {
            if (fillLight2 == null) return;
            fillLight2.setColor(Color.rgb(
                    (int) Math.round(180 * f),
                    (int) Math.round(180 * f),
                    (int) Math.round(200 * f)));
        });
    } // --- Fin del metodo setFillLight2Intensity ---


    public int getFillLight2Intensity() {
        return fillLight2Intensity;
    } // --- Fin del metodo getFillLight2Intensity ---


    /**
     * Activa o desactiva el patrón de cuadros (checkerboard) de fondo,
     * SOLO para preview visual. No afecta al PNG renderizado.
     */
    public void setCheckerboard(boolean checked) {
        this.checkerboard = checked;
        Platform.runLater(() -> {
            try {
                applyBackground();
            } catch (Exception e) {
                logger.error("[PreviewPanel3DFX] Error en setCheckerboard", e);
            }
        });
    } // --- Fin del metodo setCheckerboard ---


    /**
     * Aplica el fondo actual al bgRect según el modo y el estado del checkerboard.
     */
    private void applyBackground() {
        if (bgRect == null) return;
        if (checkerboard) {
            int tile = 32;
            WritableImage pattern = new WritableImage(tile * 2, tile * 2);
            PixelWriter pw = pattern.getPixelWriter();
            for (int y = 0; y < tile * 2; y++) {
                for (int x = 0; x < tile * 2; x++) {
                    boolean white = (x / tile + y / tile) % 2 == 0;
                    pw.setColor(x, y, white ? Color.rgb(220, 220, 220) : Color.rgb(180, 180, 180));
                }
            }
            bgRect.setFill(new ImagePattern(pattern, 0, 0, tile * 2, tile * 2, false));
            return;
        }

        switch (bgMode) {
            case SOLID:
                bgRect.setFill(solidColor);
                break;
            case GRADIENT:
                bgRect.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, gradientStartColor), new Stop(1, gradientEndColor)));
                break;
            case IMAGE:
                if (bgImage != null) {
                    bgRect.setFill(new ImagePattern(bgImage, 0, 0,
                            bgImage.getWidth() * bgImageScale,
                            bgImage.getHeight() * bgImageScale, false));
                } else {
                    bgRect.setFill(backgroundGradient != null ? backgroundGradient : Color.rgb(45, 45, 50));
                }
                break;
            case TRANSPARENT:
                bgRect.setFill(Color.TRANSPARENT);
                break;
        }
    } // --- Fin del metodo applyBackground ---


    // ========================================================================
    // Métodos públicos para control de fondo
    // ========================================================================

    public void setBackgroundMode(BgMode mode) {
        this.bgMode = mode;
        Platform.runLater(() -> {
            try {
                applyBackground();
            } catch (Exception e) {
                logger.error("[PreviewPanel3DFX] Error en setBackgroundMode", e);
            }
        });
    } // --- Fin del metodo setBackgroundMode ---


    public BgMode getBackgroundMode() {
        return bgMode;
    } // --- Fin del metodo getBackgroundMode ---


    public void setSolidBgColor(java.awt.Color c) {
        this.solidColor = Color.rgb(c.getRed(), c.getGreen(), c.getBlue());
        if (bgMode == BgMode.SOLID) {
            Platform.runLater(() -> {
                try {
                    applyBackground();
                } catch (Exception e) {
                    logger.error("[PreviewPanel3DFX] Error en setSolidBgColor", e);
                }
            });
        }
    } // --- Fin del metodo setSolidBgColor ---


    public void setGradientBgColors(java.awt.Color start, java.awt.Color end) {
        this.gradientStartColor = Color.rgb(start.getRed(), start.getGreen(), start.getBlue());
        this.gradientEndColor = Color.rgb(end.getRed(), end.getGreen(), end.getBlue());
        if (bgMode == BgMode.GRADIENT) {
            Platform.runLater(() -> {
                try {
                    applyBackground();
                } catch (Exception e) {
                    logger.error("[PreviewPanel3DFX] Error en setGradientBgColors", e);
                }
            });
        }
    } // --- Fin del metodo setGradientBgColors ---


    public void setBackgroundImage(javafx.scene.image.Image img) {
        this.bgImage = img;
        if (bgMode == BgMode.IMAGE) {
            Platform.runLater(() -> {
                try {
                    applyBackground();
                } catch (Exception e) {
                    logger.error("[PreviewPanel3DFX] Error en setBackgroundImage", e);
                }
            });
        }
    } // --- Fin del metodo setBackgroundImage ---


    public void setBackgroundImageScale(double scale) {
        this.bgImageScale = scale;
        if (bgMode == BgMode.IMAGE && bgImage != null) {
            Platform.runLater(() -> {
                try {
                    applyBackground();
                } catch (Exception e) {
                    logger.error("[PreviewPanel3DFX] Error en setBackgroundImageScale", e);
                }
            });
        }
    } // --- Fin del metodo setBackgroundImageScale ---


    // --- Getters de colores fondo ---
    public java.awt.Color getSolidBgColorAWT() {
        return new java.awt.Color((float) solidColor.getRed(), (float) solidColor.getGreen(),
                (float) solidColor.getBlue(), (float) solidColor.getOpacity());
    }

    public java.awt.Color getGradientStartAWT() {
        return new java.awt.Color((float) gradientStartColor.getRed(), (float) gradientStartColor.getGreen(),
                (float) gradientStartColor.getBlue(), (float) gradientStartColor.getOpacity());
    }

    public java.awt.Color getGradientEndAWT() {
        return new java.awt.Color((float) gradientEndColor.getRed(), (float) gradientEndColor.getGreen(),
                (float) gradientEndColor.getBlue(), (float) gradientEndColor.getOpacity());
    }

    public javafx.scene.image.Image getBackgroundImage() {
        return bgImage;
    }

    public double getBackgroundImageScale() {
        return bgImageScale;
    }


    /**
     * Activa o desactiva el antialiasing del SubScene 3D.
     */
    public void setAntiAlias(boolean enabled) {
        SceneAntialiasing aa = enabled ? SceneAntialiasing.BALANCED : SceneAntialiasing.DISABLED;
        if (aa == currentAA) return;
        
        Platform.runLater(() -> {
            try {
                rebuildSubScene(aa);
            } catch (Exception e) {
                logger.error("[PreviewPanel3DFX] Error en setAntiAlias", e);
            }
        });
    } // --- Fin del metodo setAntiAlias ---


    /**
     * Reemplaza el SubScene actual por uno nuevo con el antialiasing indicado.
     */
    private void rebuildSubScene(SceneAntialiasing aa) {
    	
    	if (subRoot == null) return;

        // 1. Desvincular la cámara de la subScene vieja
        subScene.setCamera(null);
        
        // 2. ¡IMPORTANTE! Desvincular el subRoot de la subScene vieja
        subScene.setRoot(new Group()); 

        int idx = root.getChildren().indexOf(subScene);
        if (idx >= 0) {
            root.getChildren().remove(idx);
        }
        
        // 3. Crear la nueva subScene con el mismo subRoot que ya quedó libre
        SubScene nueva = new SubScene(subRoot, getWidth(), getHeight(), true, aa);
        
        nueva.setCamera(camera); 
        nueva.setFill(Color.TRANSPARENT);
        nueva.widthProperty().bind(root.widthProperty());
        nueva.heightProperty().bind(root.heightProperty());
        nueva.setOnMousePressed(this::onMousePressed);
        nueva.setOnMouseDragged(this::onMouseDragged);
        nueva.setOnScroll(this::onScroll);
        
        if (idx >= 0) {
            root.getChildren().add(idx, nueva);
        } else {
            root.getChildren().add(1, nueva);
        }
        
        subScene = nueva;
        
        if (currentAA != aa) {
            logger.info("[PreviewPanel3DFX] Antialiasing cambiado a {}", aa);
            currentAA = aa;
        }
        
    } // --- Fin del metodo rebuildSubScene ---


    /**
     * Restablece la vista a valores por defecto.
     */
    public void resetView() {
        Platform.runLater(() -> {
            if (camera == null) return;
            zoom = 500;
            fitZoom = 500;
            if (rotateX != null) rotateX.setAngle(0);
            if (rotateY != null) rotateY.setAngle(0);
            if (rotateZ != null) rotateZ.setAngle(0);
            camera.setTranslateZ(-zoom);
            camera.setTranslateX(0);
            camera.setTranslateY(0);
            if (modelGroup != null) {
                modelGroup.setTranslateX(0);
                modelGroup.setTranslateY(0);
            }
        });
    } // --- Fin del metodo resetView ---


    /**
     * Ajusta el zoom para que el modelo quede encuadrado en el viewport.
     */
    private void fitToView(javafx.geometry.Bounds bounds) {
        rotateX.setAngle(0);
        rotateY.setAngle(0);
        rotateZ.setAngle(0);
        modelGroup.setTranslateX(0);
        modelGroup.setTranslateY(0);

        double radius = Math.max(
            Math.max(bounds.getWidth(), bounds.getHeight()),
            bounds.getDepth()
        ) / 2.0;

        double fov = camera.getFieldOfView();
        if (fov <= 0) fov = 30;
        double halfFov = Math.toRadians(fov / 2.0);

        zoom = radius / Math.tan(halfFov) * 2.5;
        zoom = Math.max(50, Math.min(10000, zoom));
        fitZoom = zoom;
        camera.setTranslateZ(-zoom);
        camera.setTranslateX(0);
        camera.setTranslateY(0);
        logger.debug("[PreviewPanel3DFX] fitToView: radius=" + radius + " zoom=" + zoom);
    } // --- Fin del metodo fitToView ---


    /**
     * Maneja la pulsación del ratón para rotación y paneo.
     */
    private void onMousePressed(MouseEvent e) {
        dragButton = e.getButton();
        pressX = e.getSceneX();
        pressY = e.getSceneY();
        pressAngX = rotateX.getAngle();
        pressAngY = rotateY.getAngle();
        pressAngZ = rotateZ.getAngle();
        pressCamX = camera.getTranslateX();
        pressCamY = camera.getTranslateY();
    } // --- Fin del metodo onMousePressed ---


    /**
     * Maneja el arrastre del ratón. Convención (figura de pie frente a la
     * cámara): arrastre vertical con botón izquierdo rota sobre X (lanzamiento
     * hacia delante/atrás); arrastre horizontal con botón izquierdo rota sobre
     * Z (inclinación lateral sobre el eje de la cámara); con Shift, arrastre
     * horizontal rota sobre Y (giro de guiñada) y arrastre vertical desplaza
     * la cámara. Pulsando el botón central o derecho se panea la cámara.
     */
    private void onMouseDragged(MouseEvent e) {
        double dx = e.getSceneX() - pressX;
        double dy = e.getSceneY() - pressY;

        if (dragButton == MouseButton.SECONDARY || dragButton == MouseButton.MIDDLE) {
            camera.setTranslateX(pressCamX - dx);
            camera.setTranslateY(pressCamY - dy);
        } else if (e.isShiftDown()) {
            double sensitivity = 0.6;
            double newRotY = pressAngY + dx * sensitivity;
            rotateY.setAngle(newRotY);
            camera.setTranslateY(pressCamY - dy);
        } else {
            double sensitivity = 0.6;
            double newRotX = pressAngX + dy * sensitivity;
            double newRotZ = pressAngZ - dx * sensitivity;
            rotateX.setAngle(newRotX);
            rotateZ.setAngle(newRotZ);
        }
    } // --- Fin del metodo onMouseDragged ---


    /**
     * Maneja la rueda del ratón para hacer zoom.
     */
    private void onScroll(ScrollEvent e) {
        try {
            double factor = Math.pow(1.05, -e.getDeltaY() / 40.0);
            zoom = Math.max(100, Math.min(10000, zoom * factor));
            camera.setTranslateZ(-zoom);
        } catch (Exception ex) {
            logger.error("[PreviewPanel3DFX] Error en onScroll", ex);
        }
    } // --- Fin del metodo onScroll ---


    public double getRotateXAngle() {
        return rotateX.getAngle();
    } // --- Fin del metodo getRotateXAngle ---


    public double getRotateYAngle() {
        return rotateY.getAngle();
    } // --- Fin del metodo getRotateYAngle ---


    public double getRotateZAngle() {
        return rotateZ.getAngle();
    } // --- Fin del metodo getRotateZAngle ---


    public double getPanX() {
        return -camera.getTranslateX();
    } // --- Fin del metodo getPanX ---


    public double getPanY() {
        return -camera.getTranslateY();
    } // --- Fin del metodo getPanY ---


    public double getZoomFactor() {
        return fitZoom > 0 ? fitZoom / zoom : 1.0;
    } // --- Fin del metodo getZoomFactor ---


    /**
     * Snapshot inmutable del estado de la vista 3D (rotación, pan y zoom) tal
     * como lo aplica la escena en un instante concreto. Todos los valores se
     * leen dentro del hilo de JavaFX para garantizar coherencia.
     */
    public static final class EstadoVista3D {

        public final double rotX;
        public final double rotY;
        public final double rotZ;
        public final double panX;
        public final double panY;
        public final double zoomFactor;

        private EstadoVista3D(double rotX, double rotY, double rotZ,
                double panX, double panY, double zoomFactor) {
            this.rotX = rotX;
            this.rotY = rotY;
            this.rotZ = rotZ;
            this.panX = panX;
            this.panY = panY;
            this.zoomFactor = zoomFactor;
        } // --- Fin del constructor EstadoVista3D ---
    } // --- Fin de la clase EstadoVista3D ---


    /**
     * Lee el estado completo de la vista 3D dentro del hilo de JavaFX y lo
     * devuelve como snapshot coherente, a diferencia de los getters individuales
     * que se leen desde el hilo del llamador.
     *
     * @return snapshot con la rotación, pan y zoom actuales, o null si el hilo
     *         FX no responde en el timeout
     */
    public EstadoVista3D capturarEstadoVista() {
        return ejecutarEnFxThread(() -> new EstadoVista3D(
                getRotateXAngle(),
                getRotateYAngle(),
                getRotateZAngle(),
                getPanX(),
                getPanY(),
                getZoomFactor()), 15000);
    } // --- Fin del metodo capturarEstadoVista ---


    /**
     * Aplica una rotación absoluta (grados) sobre el eje X del modelo.
     */
    public void setRotateXAngle(double deg) {
        Platform.runLater(() -> {
            if (rotateX != null) rotateX.setAngle(deg);
        });
    } // --- Fin del metodo setRotateXAngle ---


    /**
     * Aplica una rotación absoluta (grados) sobre el eje Y del modelo.
     */
    public void setRotateYAngle(double deg) {
        Platform.runLater(() -> {
            if (rotateY != null) rotateY.setAngle(deg);
        });
    } // --- Fin del metodo setRotateYAngle ---


    /**
     * Aplica una rotación absoluta (grados) sobre el eje Z del modelo.
     */
    public void setRotateZAngle(double deg) {
        Platform.runLater(() -> {
            if (rotateZ != null) rotateZ.setAngle(deg);
        });
    } // --- Fin del metodo setRotateZAngle ---


    /**
     * Desplaza el objeto en pantalla (panX positivo = hacia la derecha),
     * negando la traslación de cámara para mantener la convención del ratón.
     */
    public void setPanX(double panX) {
        Platform.runLater(() -> {
            if (camera != null) camera.setTranslateX(-panX);
        });
    } // --- Fin del metodo setPanX ---


    /**
     * Desplaza el objeto en pantalla (panY positivo = hacia abajo).
     */
    public void setPanY(double panY) {
        Platform.runLater(() -> {
            if (camera != null) camera.setTranslateY(-panY);
        });
    } // --- Fin del metodo setPanY ---


    /**
     * Aplica un factor de zoom absoluto (1.0 = 100%, encuadre original).
     * Escribe la distancia de cámara derivada del zoom de encaje.
     */
    public void setZoomFactor(double factor) {
        if (factor <= 0) return;
        Platform.runLater(() -> {
            double nuevoZoom = fitZoom / factor;
            nuevoZoom = Math.max(100, Math.min(10000, nuevoZoom));
            zoom = nuevoZoom;
            camera.setTranslateZ(-zoom);
        });
    } // --- Fin del metodo setZoomFactor ---


    /**
     * Comportamiento legacy/fallback: captura en el hilo de JavaFX la SubScene
     * 3D actual (modelo + luces) a la resolución natural del panel, sin
     * supersampling. Se conserva para los flujos que no requieren alta
     * resolución.
     *
     * @return imagen con el modelo renderizado y fondo transparente, o null si
     *         no está lista, no hay modelo cargado o falla la captura
     */
    public BufferedImage capturarEscena3D() {
        if (subScene == null || modelGroup == null || !modelGroup.isVisible()) return null;

        WritableImage snap = snapshotEnFxThread(() -> subScene.snapshot(null, null), 3000);
        if (snap == null) return null;
        return toBufferedImage(snap);
    } // --- Fin del metodo capturarEscena3D ---


    /**
     * Captura la SubScene JavaFX con supersampling y la reduce al lado mayor
     * indicado. NO cambia cámara, FOV, transforms, zoom, rotación, iluminación
     * ni el tamaño visual del panel: solo aplica una escala de proyección en el
     * momento del snapshot. El aspect ratio de la SubScene se conserva.
     *
     * @param targetLadoMayor lado mayor (px) de la imagen final
     * @return imagen final ARGB, o null si la captura falla o supera el timeout
     */
    public BufferedImage capturarEscena3DSuperSampled(int targetLadoMayor) {
        if (subScene == null || modelGroup == null || !modelGroup.isVisible()) return null;
        if (targetLadoMayor <= 0) return capturarEscena3D();

        final double[] dims = new double[2];
        final int[] factorUsado = new int[1];
        final int[] snapSize = new int[2];
        final long[] tSnapNs = new long[1];

        WritableImage snap = snapshotEnFxThread(() -> {
            double w0 = subScene.getWidth();
            double h0 = subScene.getHeight();
            dims[0] = w0;
            dims[1] = h0;
            if (w0 <= 0 || h0 <= 0) return null;

            double ladoMayor = Math.max(w0, h0);
            // Factor adaptativo: mínimo 2x, suficiente para alcanzar el objetivo
            // y limitado para no superar 2048 px internos.
            int factorTarget = (int) Math.ceil(targetLadoMayor / ladoMayor);
            int factorFit = (int) Math.max(1, Math.floor(2048.0 / ladoMayor));
            int factor = Math.min(Math.max(2, factorTarget), factorFit);
            factorUsado[0] = factor;

            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            params.setDepthBuffer(true);
            params.setTransform(Transform.scale(factor, factor));

            int sw = (int) Math.rint(w0 * factor);
            int sh = (int) Math.rint(h0 * factor);
            snapSize[0] = sw;
            snapSize[1] = sh;

            long t0 = System.nanoTime();
            try {
                return subScene.snapshot(params, new WritableImage(sw, sh));
            } finally {
                tSnapNs[0] = System.nanoTime() - t0;
            }
        }, 15000);

        if (snap == null) return null;

        long tConv0 = System.nanoTime();
        BufferedImage grande = toBufferedImage(snap);
        long tConv = System.nanoTime() - tConv0;

        long tDown0 = System.nanoTime();
        BufferedImage finalImg;
        try {
            finalImg = Thumbnails.of(grande)
                    .size(targetLadoMayor, targetLadoMayor)
                    .keepAspectRatio(true)
                    .rendering(Rendering.QUALITY)
                    .antialiasing(Antialiasing.ON)
                    .asBufferedImage();
        } catch (IOException ex) {
            logger.warn("[PreviewPanel3DFX] Error en downsample del supersample", ex);
            return null;
        }
        long tDown = System.nanoTime() - tDown0;

        logger.info("[PreviewPanel3DFX] Supersample: subScene={}x{} factor={} interno={}x{} final={}x{} | snapshot={}ms conversion={}ms downsample={}ms",
                (int) dims[0], (int) dims[1], factorUsado[0], snapSize[0], snapSize[1],
                finalImg.getWidth(), finalImg.getHeight(),
                tSnapNs[0] / 1_000_000, tConv / 1_000_000, tDown / 1_000_000);
        return finalImg;
    } // --- Fin del metodo capturarEscena3DSuperSampled ---


    /**
     * Ejecuta la captura en el hilo de JavaFX y espera el resultado con timeout.
     * Devuelve null si la captura falla o no completa a tiempo.
     */
    private WritableImage snapshotEnFxThread(Supplier<WritableImage> captura, long timeoutMs) {
        return ejecutarEnFxThread(captura, timeoutMs);
    } // --- Fin del metodo snapshotEnFxThread ---


    /**
     * Ejecuta una acción en el hilo de JavaFX y espera el resultado con timeout.
     * Devuelve null si la acción falla, es null o no completa a tiempo. Reutiliza
     * el mismo patrón latch+timeout que la captura de escena.
     *
     * @param accion    proveedor a ejecutar dentro del hilo FX
     * @param timeoutMs milisegundos máximos de espera
     * @return resultado de la acción, o null en caso de error o timeout
     */
    private <T> T ejecutarEnFxThread(Supplier<T> accion, long timeoutMs) {
        final T[] resultado = (T[]) new Object[1];
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                resultado[0] = accion.get();
            } catch (Exception ex) {
                logger.warn("[PreviewPanel3DFX] Error ejecutando acción en el hilo FX", ex);
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                logger.warn("[PreviewPanel3DFX] Timeout ejecutando acción en el hilo FX ({} ms)", timeoutMs);
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return resultado[0];
    } // --- Fin del metodo ejecutarEnFxThread ---


    /**
     * Convierte un {@link WritableImage} JavaFX a {@link BufferedImage} ARGB.
     * Usa acceso masivo a píxeles ({@link PixelReader#getPixels}) en vez del
     * bucle getColor/setRGB píxel a píxel.
     *
     * @param snap imagen JavaFX a convertir
     * @return imagen ARGB equivalente
     */
    private BufferedImage toBufferedImage(WritableImage snap) {
        int w = (int) snap.getWidth();
        int h = (int) snap.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] px = new int[w * h];
        PixelReader pr = snap.getPixelReader();
        pr.getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), px, 0, w);
        out.setRGB(0, 0, w, h, px, 0, w);
        return out;
    } // --- Fin del metodo toBufferedImage ---


    /**
     * PRUEBA TEMPORAL: genera A (snapshot normal), B (snapshot supersampleado x4)
     * y C (B reducido al lado mayor indicado), los guarda como PNG y vuelca
     * métricas de encuadre y detalle. Devuelve true si pudo capturar.
     */
    public boolean generarPruebaSnapshot(Path dir, int targetLadoMayor) {
        if (subScene == null || modelGroup == null || !modelGroup.isVisible()) return false;
        final boolean[] resultado = new boolean[]{false};
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                Files.createDirectories(dir);
                double w = subScene.getWidth();
                double h = subScene.getHeight();

                long t0 = System.nanoTime();
                // A) snapshot normal (como producción)
                WritableImage a = subScene.snapshot(null, null);
                long tSnapA = (System.nanoTime() - t0) / 1_000_000;

                // B) supersample x4 con la misma escena (cámara/FOV/aspect intactos)
                double s = 4.0;
                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT);
                params.setDepthBuffer(true);
                params.setTransform(Transform.scale(s, s));
                t0 = System.nanoTime();
                WritableImage b = subScene.snapshot(params,
                        new WritableImage((int) Math.rint(w * s), (int) Math.rint(h * s)));
                long tSnapB = (System.nanoTime() - t0) / 1_000_000;

                t0 = System.nanoTime();
                BufferedImage aBuf = toBufferedImage(a);
                BufferedImage bBuf = toBufferedImage(b);
                long tConv = (System.nanoTime() - t0) / 1_000_000;

                t0 = System.nanoTime();
                BufferedImage cBuf = Thumbnails.of(bBuf)
                        .size(targetLadoMayor, targetLadoMayor)
                        .asBufferedImage();
                long tDownC = (System.nanoTime() - t0) / 1_000_000;

                // VALIDACIÓN: B reducido EXACTAMENTE al tamaño de A. Si ambos
                // representan la misma escena, A ≈ downB (solo difiere por el
                // antialiasing/upscale) y el RMS debe ser bajo.
                t0 = System.nanoTime();
                BufferedImage downB = Thumbnails.of(bBuf)
                        .size(aBuf.getWidth(), aBuf.getHeight())
                        .keepAspectRatio(true)
                        .asBufferedImage();
                long tDownB = (System.nanoTime() - t0) / 1_000_000;

                Path pa = dir.resolve("prueba_A.png");
                Path pb = dir.resolve("prueba_B.png");
                Path pc = dir.resolve("prueba_C.png");
                Path pd = dir.resolve("prueba_downB.png");
                ImageIO.write(aBuf, "PNG", pa.toFile());
                ImageIO.write(bBuf, "PNG", pb.toFile());
                ImageIO.write(cBuf, "PNG", pc.toFile());
                ImageIO.write(downB, "PNG", pd.toFile());

                double[] rmsAB = rmsContenido(aBuf, downB);

                int[] bboxA = bboxContenido(aBuf);
                int[] bboxC = bboxContenido(cBuf);
                double aspectA = (bboxA[2] - bboxA[0] + 1) / (double) (bboxA[3] - bboxA[1] + 1);
                double aspectC = (bboxC[2] - bboxC[0] + 1) / (double) (bboxC[3] - bboxC[1] + 1);
                double cxA = (bboxA[0] + bboxA[2]) / 2.0 / aBuf.getWidth();
                double cyA = (bboxA[1] + bboxA[3]) / 2.0 / aBuf.getHeight();
                double cxC = (bboxC[0] + bboxC[2]) / 2.0 / cBuf.getWidth();
                double cyC = (bboxC[1] + bboxC[3]) / 2.0 / cBuf.getHeight();

                // Referencia: upscale bicúbico de A al tamaño de C (si B fuese un
                // upscale, C sería casi idéntico a esto y el RMS sería ~0)
                BufferedImage upA = new BufferedImage(cBuf.getWidth(), cBuf.getHeight(),
                        BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = upA.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(aBuf, 0, 0, upA.getWidth(), upA.getHeight(), null);
                g.dispose();

                double[] rms = rmsContenido(cBuf, upA);
                long gradC = gradientCount(cBuf);
                long gradUpA = gradientCount(upA);

                StringBuilder sb = new StringBuilder();
                sb.append("[PruebaSnapshot] A=").append(aBuf.getWidth()).append('x').append(aBuf.getHeight())
                        .append(" B=").append(bBuf.getWidth()).append('x').append(bBuf.getHeight())
                        .append(" C=").append(cBuf.getWidth()).append('x').append(cBuf.getHeight())
                        .append(" downB=").append(downB.getWidth()).append('x').append(downB.getHeight()).append('\n');
                sb.append("[PruebaSnapshot] Encuadre A: cx=").append(String.format("%.3f", cxA))
                        .append(" cy=").append(String.format("%.3f", cyA))
                        .append(" aspectBBox=").append(String.format("%.3f", aspectA)).append('\n');
                sb.append("[PruebaSnapshot] Encuadre C: cx=").append(String.format("%.3f", cxC))
                        .append(" cy=").append(String.format("%.3f", cyC))
                        .append(" aspectBBox=").append(String.format("%.3f", aspectC)).append('\n');
                sb.append("[PruebaSnapshot] RMS C vs upscale(A)=").append(String.format("%.3f", rms[0]))
                        .append(" fraccionCambiada=").append(String.format("%.3f", rms[1])).append('\n');
                sb.append("[PruebaSnapshot] VALIDACION A vs downB: rms=").append(String.format("%.3f", rmsAB[0]))
                        .append(" fraccionCambiada=").append(String.format("%.3f", rmsAB[1])).append('\n');
                sb.append("[PruebaSnapshot] Tiempos: snapA=").append(tSnapA).append("ms snapB=").append(tSnapB)
                        .append("ms conv=").append(tConv).append("ms downC=").append(tDownC)
                        .append("ms downB=").append(tDownB).append("ms\n");
                sb.append("[PruebaSnapshot] Gradientes fuertes: C=").append(gradC)
                        .append(" upA=").append(gradUpA).append('\n');
                sb.append("[PruebaSnapshot] Guardados: ").append(pa).append(" | ").append(pb)
                        .append(" | ").append(pc).append(" | ").append(pd);
                logger.info(sb.toString());
                System.out.println(sb.toString());
                resultado[0] = true;
            } catch (Exception ex) {
                logger.error("[PruebaSnapshot] Error generando prueba", ex);
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) {
                logger.warn("[PruebaSnapshot] Timeout generando prueba");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return resultado[0];
    } // --- Fin del metodo generarPruebaSnapshot ---


    /** Bounding box (minX, minY, maxX, maxY) de los píxeles no transparentes. */
    private int[] bboxContenido(BufferedImage img) {
        int minX = img.getWidth(), minY = img.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (((img.getRGB(x, y) >>> 24) & 0xff) > 0) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) return new int[]{0, 0, 0, 0};
        return new int[]{minX, minY, maxX, maxY};
    } // --- Fin del metodo bboxContenido ---


    /** RMS de diferencia de color entre dos imágenes sobre píxeles con contenido. */
    private double[] rmsContenido(BufferedImage a, BufferedImage b) {
        int w = Math.min(a.getWidth(), b.getWidth());
        int h = Math.min(a.getHeight(), b.getHeight());
        double acc = 0;
        long n = 0, changed = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pa = a.getRGB(x, y), pb = b.getRGB(x, y);
                boolean ca = ((pa >>> 24) & 0xff) > 0;
                boolean cb = ((pb >>> 24) & 0xff) > 0;
                if (!ca && !cb) continue;
                double dr = ((pa >> 16) & 0xff) - ((pb >> 16) & 0xff);
                double dg = ((pa >> 8) & 0xff) - ((pb >> 8) & 0xff);
                double db = (pa & 0xff) - (pb & 0xff);
                double d2 = (dr * dr + dg * dg + db * db) / (3.0 * 255.0 * 255.0);
                acc += d2;
                n++;
                if (d2 > (10.0 / 255.0) * (10.0 / 255.0)) changed++;
            }
        }
        if (n == 0) return new double[]{0, 0};
        return new double[]{Math.sqrt(acc / n), changed / (double) n};
    } // --- Fin del metodo rmsContenido ---


    /** Cuenta píxeles con gradiente de luminancia fuerte (>40). */
    private long gradientCount(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[] lum = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                if (((argb >>> 24) & 0xff) == 0) {
                    lum[y * w + x] = -1;
                    continue;
                }
                int r = (argb >> 16) & 0xff, g = (argb >> 8) & 0xff, b = argb & 0xff;
                lum[y * w + x] = (r * 299 + g * 587 + b * 114) / 1000;
            }
        }
        long count = 0;
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int l = lum[y * w + x];
                if (l < 0) continue;
                int up = lum[(y - 1) * w + x], dn = lum[(y + 1) * w + x];
                int lf = lum[y * w + x - 1], rg = lum[y * w + x + 1];
                if (up < 0 || dn < 0 || lf < 0 || rg < 0) continue;
                if (Math.max(Math.abs(rg - lf), Math.abs(dn - up)) > 40) count++;
            }
        }
        return count;
    } // --- Fin del metodo gradientCount ---


} // --- Fin de la clase PreviewPanel3DFX ---
