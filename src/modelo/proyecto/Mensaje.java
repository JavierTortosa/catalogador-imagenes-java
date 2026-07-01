package modelo.proyecto;

public record Mensaje(String de, String texto, int iteracion, boolean leido) {

    // Constructor de 2 parámetros (compatibilidad)
    public Mensaje(String de, String texto) {
        this(de, texto, 0, false);
    }

    // Constructor de 3 parámetros (el que usa CommentThread por defecto)
    public Mensaje(String de, String texto, int iteracion) {
        this(de, texto, iteracion, false);
    }

    public boolean isCompartido(int sharedIteration) {
        return iteracion < sharedIteration;
    }

}