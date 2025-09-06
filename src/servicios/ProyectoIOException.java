package servicios; 

public class ProyectoIOException extends java.io.IOException {
    private static final long serialVersionUID = 1L;

    public ProyectoIOException(String message) {
        super(message);
    }

    public ProyectoIOException(String message, Throwable cause) {
        super(message, cause);
    }
}