package modelo.editor;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.UUID;

public class ImageLayer {

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

    public ImageLayer(String name, BufferedImage image, Rectangle bounds) {
        this.id = UUID.randomUUID().toString();
        this.name = Objects.requireNonNull(name);
        this.image = image;
        this.bounds = bounds;
        this.visible = true;
        this.locked = false;
        this.opacity = 1.0f;
        this.type = LayerType.IMAGE;
    } // --- Fin del constructor ImageLayer ---

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

    public ImageLayer copy() {
        ImageLayer clone = new ImageLayer(this.name, this.image, this.bounds);
        clone.visible = this.visible;
        clone.locked = this.locked;
        clone.opacity = this.opacity;
        clone.type = this.type;
        return clone;
    } // --- Fin del metodo copy ---

} // --- Fin de la clase ImageLayer ---
