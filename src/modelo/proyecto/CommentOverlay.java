package modelo.proyecto;

import java.util.Objects;

/**
 * Representa un comentario superpuesto en una imagen del cliente,
 * con posición independiente (imageX, imageY) y texto editable.
 */
public class CommentOverlay {

    private int imageX;
    private int imageY;
    private String text;

    public CommentOverlay() {
        this(0, 0, "");
    }

    public CommentOverlay(int imageX, int imageY, String text) {
        this.imageX = imageX;
        this.imageY = imageY;
        this.text = text != null ? text : "";
    }

    public int getImageX() {
        return imageX;
    }

    public void setImageX(int imageX) {
        this.imageX = imageX;
    }

    public int getImageY() {
        return imageY;
    }

    public void setImageY(int imageY) {
        this.imageY = imageY;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentOverlay that = (CommentOverlay) o;
        return imageX == that.imageX && imageY == that.imageY && Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageX, imageY, text);
    }

}
