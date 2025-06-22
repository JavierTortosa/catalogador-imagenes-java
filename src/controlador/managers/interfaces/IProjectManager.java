package controlador.managers.interfaces;

import java.awt.Component;
import java.nio.file.Path;
import java.util.List;

/**
 * Interfaz (Contrato) que define las responsabilidades del ProjectManager.
 * Define las operaciones para marcar, gestionar y recuperar selecciones de imágenes.
 */
public interface IProjectManager {

    /**
     * Devuelve una lista de los Paths absolutos de todas las imágenes marcadas.
     * @return Una nueva lista de Paths (ordenada).
     */
    List<Path> getImagenesMarcadas();

    /**
     * Muestra un diálogo o una vista para gestionar los proyectos/selecciones.
     * @param parentComponent El componente padre para el diálogo (puede ser null).
     */
    void gestionarSeleccionProyecto(Component parentComponent);

    /**
     * Marca una imagen para el proyecto actual.
     * @param rutaAbsoluta El Path absoluto de la imagen.
     */
    void marcarImagenInterno(Path rutaAbsoluta);

    /**
     * Desmarca una imagen del proyecto actual.
     * @param rutaAbsoluta El Path absoluto de la imagen.
     */
    void desmarcarImagenInterno(Path rutaAbsoluta);

    /**
     * Verifica si una imagen (dada por su Path absoluto) está actualmente marcada.
     * @param rutaAbsolutaImagen El Path absoluto de la imagen a verificar.
     * @return true si la imagen está marcada, false en caso contrario.
     */
    boolean estaMarcada(Path rutaAbsolutaImagen);

    /**
     * Alterna el estado de marca de una imagen (dada por su Path absoluto).
     * @param rutaAbsolutaImagen El Path absoluto de la imagen.
     * @return true si la imagen quedó marcada, false si quedó desmarcada.
     */
    boolean alternarMarcaImagen(Path rutaAbsolutaImagen);

} // --- FIN de la interfaz IProjectManager ---