package vista.panels.render;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

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
import javafx.scene.SubScene;
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
    private double zoom = 500;

    private double pressX, pressY;
    private SceneAntialiasing currentAA;
    private double pressRotX, pressRotY;
    private double pressCamX, pressCamY;
    private double fitZoom = 500;
    private MouseButton dragButton;

    private boolean checkerboard;
    private boolean sceneInitialized;
    private SubScene subScene;
    private Group subRoot;
    private LinearGradient backgroundGradient;
    
    
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

        rotationGroup = new Group();
        rotationGroup.getTransforms().addAll(rotateY, rotateX);
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
        hLine.setVisible(visible);        vLine.setVisible(visible);
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
        fillLight2.setVisible(visible);
    } // --- Fin del metodo setFillLight2Visible ---


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
        zoom = 500;
        fitZoom = 500;
        rotateX.setAngle(0);
        rotateY.setAngle(0);
        camera.setTranslateZ(-zoom);
        camera.setTranslateX(0);
        camera.setTranslateY(0);
        modelGroup.setTranslateX(0);
        modelGroup.setTranslateY(0);
    } // --- Fin del metodo resetView ---


    /**
     * Ajusta el zoom para que el modelo quede encuadrado en el viewport.
     */
    private void fitToView(javafx.geometry.Bounds bounds) {
        rotateX.setAngle(0);
        rotateY.setAngle(0);
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
        pressRotX = rotateY.getAngle();
        pressRotY = rotateX.getAngle();
        pressCamX = camera.getTranslateX();
        pressCamY = camera.getTranslateY();
    } // --- Fin del metodo onMousePressed ---


    /**
     * Maneja el arrastre del ratón: botón izquierdo rota, botón derecho panea.
     */
    private void onMouseDragged(MouseEvent e) {
        double dx = e.getSceneX() - pressX;
        double dy = e.getSceneY() - pressY;

        if (dragButton == MouseButton.SECONDARY) {
            camera.setTranslateX(pressCamX - dx);
            camera.setTranslateY(pressCamY - dy);
        } else {
            double sensitivity = 0.6;
            double newRotY = pressRotX - dx * sensitivity;
            double newRotX = Math.max(-90, Math.min(90, pressRotY + dy * sensitivity));
            rotateY.setAngle(newRotY);
            rotateX.setAngle(newRotX);
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


    public double getPanX() {
        return camera.getTranslateX();
    } // --- Fin del metodo getPanX ---


    public double getPanY() {
        return camera.getTranslateY();
    } // --- Fin del metodo getPanY ---


    public double getZoomFactor() {
        return fitZoom > 0 ? fitZoom / zoom : 1.0;
    } // --- Fin del metodo getZoomFactor ---


} // --- Fin de la clase PreviewPanel3DFX ---
