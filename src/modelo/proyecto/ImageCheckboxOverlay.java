package modelo.proyecto;

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
    }

    public ImageCheckboxOverlay(int imageX, int imageY, boolean checked, String label) {
        this(imageX, imageY, checked, label, "", "", 0.0, 32, checked);
    }

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
    }

    public int getImageX() { return imageX; }
    public void setImageX(int imageX) { this.imageX = imageX; }

    public int getImageY() { return imageY; }
    public void setImageY(int imageY) { this.imageY = imageY; }

    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label != null ? label : ""; }

    public String getCheckboxCode() { return checkboxCode; }
    public void setCheckboxCode(String checkboxCode) { this.checkboxCode = checkboxCode != null ? checkboxCode : ""; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment != null ? comment : ""; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size > 0 ? size : 32; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

}
