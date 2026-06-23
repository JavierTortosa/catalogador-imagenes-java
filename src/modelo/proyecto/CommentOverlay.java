package modelo.proyecto;

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
    } // --- FIN de metodo CommentOverlay (constructor) ---


    public CommentOverlay(int imageX, int imageY, String text) {
        this.imageX = imageX;
        this.imageY = imageY;
        this.text = text != null ? text : "";
    } // --- FIN de metodo CommentOverlay (constructor con parámetros) ---


    public int getImageX() {
        return imageX;
    } // --- FIN de metodo getImageX ---


    public void setImageX(int imageX) {
        this.imageX = imageX;
    } // --- FIN de metodo setImageX ---


    public int getImageY() {
        return imageY;
    } // --- FIN de metodo getImageY ---


    public void setImageY(int imageY) {
        this.imageY = imageY;
    } // --- FIN de metodo setImageY ---


    public String getText() {
        return text;
    } // --- FIN de metodo getText ---


    public void setText(String text) {
        this.text = text != null ? text : "";
    } // --- FIN de metodo setText ---

} // --- FIN de clase CommentOverlay ---
