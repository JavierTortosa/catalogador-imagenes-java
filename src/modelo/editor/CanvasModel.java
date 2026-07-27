package modelo.editor;

import java.awt.Color;

public class CanvasModel {

    private int width;
    private int height;
    private Color backgroundColor;
    private boolean transparent;

    public CanvasModel(int width, int height) {
        this.width = width;
        this.height = height;
        this.backgroundColor = Color.WHITE;
        this.transparent = false;
    } // --- Fin del constructor CanvasModel ---

    public int getWidth() {
        return width;
    } // --- Fin del metodo getWidth ---

    public int getHeight() {
        return height;
    } // --- Fin del metodo getHeight ---

    public void setSize(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    } // --- Fin del metodo setSize ---

    public Color getBackgroundColor() {
        return backgroundColor;
    } // --- Fin del metodo getBackgroundColor ---

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    } // --- Fin del metodo setBackgroundColor ---

    public boolean isTransparent() {
        return transparent;
    } // --- Fin del metodo isTransparent ---

    public void setTransparent(boolean transparent) {
        this.transparent = transparent;
    } // --- Fin del metodo setTransparent ---

} // --- Fin de la clase CanvasModel ---
