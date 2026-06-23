package modelo.proyecto;

/**
 * Representa un checkbox superpuesto en una imagen,
 * con posición, estado booleano (checked), código único, comentario,
 * precio (PVP) y tamaño configurable.
 */
public class ImageCheckboxOverlay {

    private int imageX;
    private int imageY;
    private boolean checked;
    private String label;
    private String checkboxCode;
    private String comment;
    private double price;
    private int size;
    private boolean selected;

    public ImageCheckboxOverlay() {
        this(0, 0, false, "", "", "", 0.0, 32, false);
    } // --- Fin de metodo ImageCheckboxOverlay (constructor) ---


    public ImageCheckboxOverlay(int imageX, int imageY, boolean checked, String label) {
        this(imageX, imageY, checked, label, "", "", 0.0, 32, checked);
    } // --- Fin de metodo ImageCheckboxOverlay (constructor con parámetros) ---


    public ImageCheckboxOverlay(int imageX, int imageY, boolean checked, String label,
                                String checkboxCode, String comment, double price, int size, boolean selected) {
        this.imageX = imageX;
        this.imageY = imageY;
        this.checked = checked;
        this.label = label != null ? label : "";
        this.checkboxCode = checkboxCode != null ? checkboxCode : "";
        this.comment = comment != null ? comment : "";
        this.price = price;
        this.size = size > 0 ? size : 32;
        this.selected = selected;
    } // --- Fin de metodo ImageCheckboxOverlay (constructor completo) ---


    public int getImageX() {
        return imageX;
    } // --- Fin de metodo getImageX ---


    public void setImageX(int imageX) {
        this.imageX = imageX;
    } // --- Fin de metodo setImageX ---


    public int getImageY() {
        return imageY;
    } // --- Fin de metodo getImageY ---


    public void setImageY(int imageY) {
        this.imageY = imageY;
    } // --- Fin de metodo setImageY ---


    public boolean isChecked() {
        return checked;
    } // --- Fin de metodo isChecked ---


    public void setChecked(boolean checked) {
        this.checked = checked;
    } // --- Fin de metodo setChecked ---


    public String getLabel() {
        return label;
    } // --- Fin de metodo getLabel ---


    public void setLabel(String label) {
        this.label = label != null ? label : "";
    } // --- Fin de metodo setLabel ---


    public String getCheckboxCode() {
        return checkboxCode;
    } // --- Fin de metodo getCheckboxCode ---


    public void setCheckboxCode(String checkboxCode) {
        this.checkboxCode = checkboxCode != null ? checkboxCode : "";
    } // --- Fin de metodo setCheckboxCode ---


    public String getComment() {
        return comment;
    } // --- Fin de metodo getComment ---


    public void setComment(String comment) {
        this.comment = comment != null ? comment : "";
    } // --- Fin de metodo setComment ---


    public double getPrice() {
        return price;
    } // --- Fin de metodo getPrice ---


    public void setPrice(double price) {
        this.price = price;
    } // --- Fin de metodo setPrice ---


    public int getSize() {
        return size;
    } // --- Fin de metodo getSize ---


    public void setSize(int size) {
        this.size = size > 0 ? size : 32;
    } // --- Fin de metodo setSize ---


    public boolean isSelected() {
        return selected;
    } // --- Fin de metodo isSelected ---


    public void setSelected(boolean selected) {
        this.selected = selected;
    } // --- Fin de metodo setSelected ---

} // --- Fin de clase ImageCheckboxOverlay ---
