package modelo.editor;

import java.awt.Rectangle;

public class SelectionModel {

    private int x;
    private int y;
    private int width;
    private int height;
    private int feather;
    private boolean active;

    public SelectionModel() {
        this.active = false;
        this.feather = 0;
    } // --- Fin del constructor SelectionModel ---

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    } // --- Fin del metodo getBounds ---

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.active = (width > 0 && height > 0);
    } // --- Fin del metodo setBounds ---

    public void setBounds(Rectangle rect) {
        if (rect != null) {
            setBounds(rect.x, rect.y, rect.width, rect.height);
        }
    } // --- Fin del metodo setBounds ---

    public int getX() {
        return x;
    } // --- Fin del metodo getX ---

    public int getY() {
        return y;
    } // --- Fin del metodo getY ---

    public int getWidth() {
        return width;
    } // --- Fin del metodo getWidth ---

    public int getHeight() {
        return height;
    } // --- Fin del metodo getHeight ---

    public boolean isActive() {
        return active;
    } // --- Fin del metodo isActive ---

    public int getFeather() {
        return feather;
    } // --- Fin del metodo getFeather ---

    public void setFeather(int feather) {
        this.feather = Math.max(0, feather);
    } // --- Fin del metodo setFeather ---

    public boolean contains(int px, int py) {
        return active && px >= x && px <= x + width && py >= y && py <= y + height;
    } // --- Fin del metodo contains ---

    public boolean intersects(Rectangle rect) {
        return active && rect != null && getBounds().intersects(rect);
    } // --- Fin del metodo intersects ---

    public void clear() {
        this.x = 0;
        this.y = 0;
        this.width = 0;
        this.height = 0;
        this.active = false;
    } // --- Fin del metodo clear ---

} // --- Fin de la clase SelectionModel ---
