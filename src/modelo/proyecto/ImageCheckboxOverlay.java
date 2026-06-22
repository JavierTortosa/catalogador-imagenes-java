package modelo.proyecto;

/**
 * Representa un checkbox superpuesto sobre una imagen en el Modo Cliente.
 * Almacena la posición, estado de selección y etiqueta de precio.
 */
public class ImageCheckboxOverlay {

    private int imageX;
    private int imageY;
    private boolean checked;
    private String label;

    public ImageCheckboxOverlay() {
        this(0, 0, false, "");
    }

    public ImageCheckboxOverlay(int imageX, int imageY, boolean checked, String label) {
        this.imageX = imageX;
        this.imageY = imageY;
        this.checked = checked;
        this.label = label != null ? label : "";
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

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label != null ? label : "";
    }

} // --- FIN de clase ImageCheckboxOverlay ---
