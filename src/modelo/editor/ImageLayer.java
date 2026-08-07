package modelo.editor;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.UUID;

public class ImageLayer implements Layer {

    public enum LayerType {
        IMAGE, TEXT, SHAPE
    } // --- Fin del enum LayerType ---

    private final String id;
    private String name;
    private BufferedImage image;
    private Rectangle bounds;
    private boolean visible;
    private boolean locked;
    private float opacity;
    private LayerType type;
    private double rotation;

    // Metadatos de forma (solo para capas LayerType.SHAPE, null/0 en el resto)
    private String shapeType;
    private Color shapeFill;
    private Color shapeStroke;
    private float shapeStrokeWidth;
    private int shapeRenderW;
    private int shapeRenderH;

    // Ruta de origen de la imagen (relativa al .edoc) cuando la capa proviene de
    // un archivo externo. Si es null, la imagen se exporta a un PNG sidecar en
    // la carpeta "_capas/" junto al documento.
    private String srcPath;

    public ImageLayer(String name, BufferedImage image, Rectangle bounds) {
        this(UUID.randomUUID().toString(), name, image, bounds);
    } // --- Fin del constructor ImageLayer ---


    private ImageLayer(String id, String name, BufferedImage image, Rectangle bounds) {
        this.id = id;
        this.name = Objects.requireNonNull(name);
        this.image = image;
        this.bounds = bounds;
        this.visible = true;
        this.locked = false;
        this.opacity = 1.0f;
        this.type = LayerType.IMAGE;
    } // --- Fin del constructor privado ImageLayer ---

    public String getId() {
        return id;
    } // --- Fin del metodo getId ---

    public String getName() {
        return name;
    } // --- Fin del metodo getName ---

    public void setName(String name) {
        this.name = name;
    } // --- Fin del metodo setName ---

    public BufferedImage getImage() {
        return image;
    } // --- Fin del metodo getImage ---

    public void setImage(BufferedImage image) {
        this.image = image;
    } // --- Fin del metodo setImage ---

    public Rectangle getBounds() {
        return bounds;
    } // --- Fin del metodo getBounds ---

    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    } // --- Fin del metodo setBounds ---

    @Override
    public double getRotation() {
        return rotation;
    } // --- Fin del metodo getRotation ---

    @Override
    public void setRotation(double rotation) {
        this.rotation = rotation;
    } // --- Fin del metodo setRotation ---

    public boolean isVisible() {
        return visible;
    } // --- Fin del metodo isVisible ---

    public void setVisible(boolean visible) {
        this.visible = visible;
    } // --- Fin del metodo setVisible ---

    public boolean isLocked() {
        return locked;
    } // --- Fin del metodo isLocked ---

    public void setLocked(boolean locked) {
        this.locked = locked;
    } // --- Fin del metodo setLocked ---

    public float getOpacity() {
        return opacity;
    } // --- Fin del metodo getOpacity ---

    public void setOpacity(float opacity) {
        this.opacity = Math.max(0f, Math.min(1f, opacity));
    } // --- Fin del metodo setOpacity ---

    public LayerType getType() {
        return type;
    } // --- Fin del metodo getType ---

    public void setType(LayerType type) {
        this.type = type;
    } // --- Fin del metodo setType ---

    public String getShapeType() {
        return shapeType;
    } // --- Fin del metodo getShapeType ---

    public void setShapeType(String shapeType) {
        this.shapeType = shapeType;
    } // --- Fin del metodo setShapeType ---

    public Color getShapeFill() {
        return shapeFill;
    } // --- Fin del metodo getShapeFill ---

    public void setShapeFill(Color shapeFill) {
        this.shapeFill = shapeFill;
    } // --- Fin del metodo setShapeFill ---

    public Color getShapeStroke() {
        return shapeStroke;
    } // --- Fin del metodo getShapeStroke ---

    public void setShapeStroke(Color shapeStroke) {
        this.shapeStroke = shapeStroke;
    } // --- Fin del metodo setShapeStroke ---

    public float getShapeStrokeWidth() {
        return shapeStrokeWidth;
    } // --- Fin del metodo getShapeStrokeWidth ---

    public void setShapeStrokeWidth(float shapeStrokeWidth) {
        this.shapeStrokeWidth = shapeStrokeWidth;
    } // --- Fin del metodo setShapeStrokeWidth ---

    public int getShapeRenderW() {
        return shapeRenderW;
    } // --- Fin del metodo getShapeRenderW ---

    public void setShapeRenderW(int shapeRenderW) {
        this.shapeRenderW = shapeRenderW;
    } // --- Fin del metodo setShapeRenderW ---

    public int getShapeRenderH() {
        return shapeRenderH;
    } // --- Fin del metodo getShapeRenderH ---

    public void setShapeRenderH(int shapeRenderH) {
        this.shapeRenderH = shapeRenderH;
    } // --- Fin del metodo setShapeRenderH ---

    public String getSrcPath() {
        return srcPath;
    } // --- Fin del metodo getSrcPath ---

    public void setSrcPath(String srcPath) {
        this.srcPath = srcPath;
    } // --- Fin del metodo setSrcPath ---

    @Override
    public void paint(Graphics2D g2) {
        if (!visible || image == null || bounds == null) return;

        Graphics2D g = (Graphics2D) g2.create();
        try {
            if (opacity < 1.0f) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            }
            if (rotation != 0) {
                g.rotate(Math.toRadians(rotation), bounds.getCenterX(), bounds.getCenterY());
            }
            g.drawImage(image, bounds.x, bounds.y, bounds.width, bounds.height, null);
        } finally {
            g.dispose();
        }
    } // --- Fin del metodo paint ---


    @Override
    public BufferedImage renderThumbnail(int size) {
        if (image == null) return null;
        int w = Math.min(image.getWidth(), size);
        int h = Math.min(image.getHeight(), size);
        BufferedImage thumb = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics g = thumb.getGraphics();
        int x = (size - w) / 2;
        int y = (size - h) / 2;
        g.drawImage(image, x, y, w, h, null);
        g.dispose();
        return thumb;
    } // --- Fin del metodo renderThumbnail ---


    @Override
    public ImageLayer copy() {
        ImageLayer clone = new ImageLayer(this.name, this.image, this.bounds);
        clone.visible = this.visible;
        clone.locked = this.locked;
        clone.opacity = this.opacity;
        clone.type = this.type;
        clone.shapeType = this.shapeType;
        clone.shapeFill = this.shapeFill;
        clone.shapeStroke = this.shapeStroke;
        clone.shapeStrokeWidth = this.shapeStrokeWidth;
        clone.shapeRenderW = this.shapeRenderW;
        clone.shapeRenderH = this.shapeRenderH;
        clone.rotation = this.rotation;
        return clone;
    } // --- Fin del metodo copy ---


    @Override
    public ImageLayer copiaParaHistorial() {
        ImageLayer clone = new ImageLayer(this.id, this.name, clonarImagen(this.image),
                this.bounds != null ? new Rectangle(this.bounds) : null);
        clone.visible = this.visible;
        clone.locked = this.locked;
        clone.opacity = this.opacity;
        clone.type = this.type;
        clone.shapeType = this.shapeType;
        clone.shapeFill = this.shapeFill;
        clone.shapeStroke = this.shapeStroke;
        clone.shapeStrokeWidth = this.shapeStrokeWidth;
        clone.shapeRenderW = this.shapeRenderW;
        clone.shapeRenderH = this.shapeRenderH;
        clone.rotation = this.rotation;
        clone.srcPath = this.srcPath;
        return clone;
    } // --- Fin del metodo copiaParaHistorial ---


    @Override
    public boolean mismoEstado(Layer otra) {
        if (!(otra instanceof ImageLayer o)) return false;
        if (this.bounds == null || o.bounds == null) return this.bounds == o.bounds;
        return Objects.equals(this.name, o.name)
                && this.bounds.equals(o.bounds)
                && Double.compare(this.rotation, o.rotation) == 0
                && this.visible == o.visible
                && this.locked == o.locked
                && Float.compare(this.opacity, o.opacity) == 0
                && this.type == o.type
                && Objects.equals(this.shapeType, o.shapeType)
                && Objects.equals(this.shapeFill, o.shapeFill)
                && Objects.equals(this.shapeStroke, o.shapeStroke)
                && Float.compare(this.shapeStrokeWidth, o.shapeStrokeWidth) == 0
                && this.shapeRenderW == o.shapeRenderW
                && this.shapeRenderH == o.shapeRenderH
                && Objects.equals(this.srcPath, o.srcPath);
    } // --- Fin del metodo mismoEstado ---


    private static BufferedImage clonarImagen(BufferedImage img) {
        if (img == null) return null;
        int tipo = img.getType();
        if (tipo == BufferedImage.TYPE_CUSTOM || tipo == BufferedImage.TYPE_BYTE_BINARY) {
            tipo = BufferedImage.TYPE_INT_ARGB;
        }
        BufferedImage copia = new BufferedImage(img.getWidth(), img.getHeight(), tipo);
        Graphics2D g = copia.createGraphics();
        try {
            g.drawImage(img, 0, 0, null);
        } finally {
            g.dispose();
        }
        return copia;
    } // --- Fin del metodo clonarImagen ---

} // --- Fin de la clase ImageLayer ---
