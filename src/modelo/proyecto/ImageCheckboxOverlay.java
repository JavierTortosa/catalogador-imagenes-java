package modelo.proyecto;

import java.util.Objects;

/**
 * Representa un checkbox superpuesto en una imagen,
 * con posición, estado (SELECTED, DISCARDED, UNDEFINED), código, comentario,
 * PVP y tamaño configurable.
 */
public class ImageCheckboxOverlay {

    private int imageX;
    private int imageY;
    private SelectionState state;
    private String label;
    private String checkboxCode;
    private String comment;
    private double price;
    private int size;

    public ImageCheckboxOverlay() {
        this(0, 0, SelectionState.UNDEFINED, "", "", "", 0.0, 32);
    }

    public ImageCheckboxOverlay(int imageX, int imageY, SelectionState state, String label) {
        this(imageX, imageY, state, label, "", "", 0.0, 32);
    }

    public ImageCheckboxOverlay(int imageX, int imageY, SelectionState state, String label,
                                String checkboxCode, String comment, double price, int size) {
        this.imageX = imageX;
        this.imageY = imageY;
        this.state = state != null ? state : SelectionState.UNDEFINED;
        this.label = label != null ? label : "";
        this.checkboxCode = checkboxCode != null ? checkboxCode : "";
        this.comment = comment != null ? comment : "";
        this.price = price;
        this.size = size > 0 ? size : 32;
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

    public SelectionState getState() {
        return state;
    }

    public void setState(SelectionState state) {
        this.state = state != null ? state : SelectionState.UNDEFINED;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label != null ? label : "";
    }

    public String getCheckboxCode() {
        return checkboxCode;
    }

    public void setCheckboxCode(String checkboxCode) {
        this.checkboxCode = checkboxCode != null ? checkboxCode : "";
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment != null ? comment : "";
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size > 0 ? size : 32;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageCheckboxOverlay that = (ImageCheckboxOverlay) o;
        return imageX == that.imageX && imageY == that.imageY && size == that.size &&
                Double.compare(that.price, price) == 0 && Objects.equals(state, that.state) &&
                Objects.equals(label, that.label) && Objects.equals(checkboxCode, that.checkboxCode) &&
                Objects.equals(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageX, imageY, state, label, checkboxCode, comment, price, size);
    }

}
