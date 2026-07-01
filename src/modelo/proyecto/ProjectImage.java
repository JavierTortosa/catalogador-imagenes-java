package modelo.proyecto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Representa una imagen dentro de la lista maestra del proyecto,
 * con estado independiente para el proyecto (selección/descarte)
 * y para el cliente (SELECTED/DISCARDED/UNDEFINED), más metadatos
 * de exportación, checkboxes internos y comentarios.
 */
public class ProjectImage {

    private String rutaImagen;
    private String etiqueta;
    private boolean enSeleccionProyecto;
    private double price = 0.0;
    private SelectionState estadoCliente;
    private SelectionState estadoClienteOriginal;
    private String codigoCatalogo;
    private ExportConfig exportConfig;
    private List<ImageCheckboxOverlay> checkboxes;
    private CommentOverlay commentOverlay;
    private String comment;
    private CommentThread commentThread;

    public ProjectImage() {
        this.estadoCliente = SelectionState.UNDEFINED;
        this.estadoClienteOriginal = SelectionState.UNDEFINED;
        this.checkboxes = new ArrayList<>();
        this.enSeleccionProyecto = true;
    }

    public ProjectImage(String rutaImagen) {
        this();
        this.rutaImagen = rutaImagen;
    }

    public String getRutaImagen() {
        return rutaImagen;
    }

    public void setRutaImagen(String rutaImagen) {
        this.rutaImagen = rutaImagen;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public void setEtiqueta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public boolean isEnSeleccionProyecto() {
        return enSeleccionProyecto;
    }

    public void setEnSeleccionProyecto(boolean enSeleccionProyecto) {
        this.enSeleccionProyecto = enSeleccionProyecto;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public SelectionState getEstadoCliente() {
        return estadoCliente;
    }

    public void setEstadoCliente(SelectionState estadoCliente) {
        this.estadoCliente = estadoCliente;
    }

    public SelectionState getEstadoClienteOriginal() {
        return estadoClienteOriginal;
    }

    public void setEstadoClienteOriginal(SelectionState estadoClienteOriginal) {
        this.estadoClienteOriginal = estadoClienteOriginal;
    }

    public String getCodigoCatalogo() {
        return codigoCatalogo;
    }

    public void setCodigoCatalogo(String codigoCatalogo) {
        this.codigoCatalogo = codigoCatalogo;
    }

    public ExportConfig getExportConfig() {
        return exportConfig;
    }

    public void setExportConfig(ExportConfig exportConfig) {
        this.exportConfig = exportConfig;
    }

    public List<ImageCheckboxOverlay> getCheckboxes() {
        if (checkboxes == null) {
            checkboxes = new ArrayList<>();
        }
        return checkboxes;
    }

    public void setCheckboxes(List<ImageCheckboxOverlay> checkboxes) {
        this.checkboxes = checkboxes;
    }

    public CommentOverlay getCommentOverlay() {
        return commentOverlay;
    }

    public void setCommentOverlay(CommentOverlay commentOverlay) {
        this.commentOverlay = commentOverlay;
    }

    public String getComment() {
        if (comment != null && !comment.isEmpty()) return comment;
        if (commentThread != null && !commentThread.isEmpty()) {
            return commentThread.getLastText();
        }
        return null;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * @return la lista de mensajes (no modificable) para lectura.
     */
    public List<Mensaje> getCommentThread() {
        if (commentThread == null) {
            commentThread = new CommentThread();
            if (comment != null && !comment.isEmpty()) {
                commentThread.add("nosotros", comment, 0);
            }
        }
        return commentThread.getMessages();
    }

    /**
     * @return el objeto CommentThread para escritura (a&ntilde;adir/borrar).
     */
    public CommentThread getCommentThreadAccess() {
        getCommentThread(); // asegura inicializaci&oacute;n
        return commentThread;
    }

    public void setCommentThread(List<Mensaje> thread) {
        if (commentThread == null) {
            commentThread = new CommentThread();
        }
        commentThread.setMessages(thread);
    }

    /**
     * @deprecated Usar {@link #getCommentThread()} y {@link CommentThread#add(String, String, int)}
     */
    @Deprecated
    public void addMensaje(String de, String texto) {
        getCommentThread();
        if (commentThread != null) {
            commentThread.add(de, texto, 0);
        }
    }

    public void addMensaje(String de, String texto, int iteracion) {
        getCommentThread();
        if (commentThread != null) {
            commentThread.add(de, texto, iteracion);
        }
    }

    public boolean hasThreadMessages() {
        return commentThread != null && !commentThread.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectImage that = (ProjectImage) o;
        return enSeleccionProyecto == that.enSeleccionProyecto
                && Double.compare(that.price, price) == 0
                && Objects.equals(rutaImagen, that.rutaImagen)
                && Objects.equals(etiqueta, that.etiqueta)
                && estadoCliente == that.estadoCliente
                && estadoClienteOriginal == that.estadoClienteOriginal
                && Objects.equals(codigoCatalogo, that.codigoCatalogo)
                && Objects.equals(exportConfig, that.exportConfig)
                && Objects.equals(checkboxes, that.checkboxes)
                && Objects.equals(commentOverlay, that.commentOverlay)
                && Objects.equals(comment, that.comment)
                && Objects.equals(commentThread, that.commentThread);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rutaImagen, etiqueta, enSeleccionProyecto, price,
                estadoCliente, estadoClienteOriginal, codigoCatalogo,
                exportConfig, checkboxes, commentOverlay, comment, commentThread);
    }

} // --- Fin de la clase ProjectImage ---
