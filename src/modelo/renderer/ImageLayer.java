package modelo.renderer;

import java.awt.image.BufferedImage;

public class ImageLayer {

    private final BufferedImage image;
    private final String name;
    private double zoom = 1.0;
    private double offsetX;
    private double offsetY;
    private boolean visible = true;

    public ImageLayer(BufferedImage image, String name) {
        this.image = image;
        this.name = name;
    }

    public BufferedImage getImage() { return image; }
    public String getName() { return name; }

    public double getZoom() { return zoom; }
    public void setZoom(double zoom) { this.zoom = zoom; }

    public double getOffsetX() { return offsetX; }
    public void setOffsetX(double offsetX) { this.offsetX = offsetX; }

    public double getOffsetY() { return offsetY; }
    public void setOffsetY(double offsetY) { this.offsetY = offsetY; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public void resetTransform() {
        zoom = 1.0;
        offsetX = 0;
        offsetY = 0;
    }

    @Override
    public String toString() {
        return name + (visible ? "" : " (oculta)");
    }

} // --- Fin de la clase ImageLayer ---
