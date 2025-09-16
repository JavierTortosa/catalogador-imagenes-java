package controlador.managers.interfaces;

import java.awt.Component;
import java.nio.file.Path;
import java.util.List;

import servicios.ProyectoIOException;

/**
 * Interfaz (Contrato) que define las responsabilidades del ProjectManager.
 * Define las operaciones para marcar, gestionar y recuperar selecciones de imágenes,
 * incluyendo las listas de selección principal y de descartes.
 */
public interface IProjectManager {

	
	/**
     * Inicia un nuevo proyecto vacío, limpiando la selección actual y los descartes.
     * Pasa a trabajar sobre el archivo temporal.
     */
    void nuevoProyecto();

    /**
     * Carga un proyecto desde el archivo especificado, reemplazando la selección actual.
     * @param rutaArchivo La ruta completa al archivo .prj del proyecto a abrir.
     */
    void abrirProyecto(Path rutaArchivo) throws ProyectoIOException;

    /**
     * Guarda el proyecto actual en un archivo específico, estableciéndolo como el proyecto activo.
     * @param rutaArchivo La ruta completa donde se guardará el archivo .prj.
     */
    void guardarProyectoComo(Path rutaArchivo);

    /**
     * Devuelve la ruta del archivo de proyecto actualmente activo (.prj).
     * @return El Path del proyecto activo, o null si se está trabajando en el proyecto temporal.
     */
    Path getArchivoProyectoActivo();

    /**
     * Devuelve el nombre del archivo del proyecto activo, o un nombre genérico si es temporal.
     * @return El nombre del proyecto para mostrar en la UI.
     */
    String getNombreProyectoActivo();

    /**
     * Devuelve la ruta de la carpeta base donde se almacenan los proyectos.
     * @return El Path de la carpeta de proyectos.
     */
    Path getCarpetaBaseProyectos();
	
	
    /**
     * Devuelve una lista de los Paths absolutos de todas las imágenes en la SELECCIÓN PRINCIPAL.
     * @return Una nueva lista de Paths (ordenada).
     */
    List<Path> getImagenesMarcadas();

    /**
     * Devuelve la lista de imágenes actualmente en la sección de descartes.
     * @return Una lista ordenada de Paths de las imágenes descartadas.
     */
    List<Path> getImagenesDescartadas();

    /**
     * Mueve una imagen de la selección actual a la lista de descartes.
     * @param rutaAbsolutaImagen La ruta de la imagen a mover.
     */
    void moverAdescartes(Path rutaAbsolutaImagen);

    /**
     * Mueve una imagen de la lista de descartes de vuelta a la selección actual.
     * @param rutaAbsolutaImagen La ruta de la imagen a restaurar.
     */
    void restaurarDeDescartes(Path rutaAbsolutaImagen);

    /**
     * Comprueba si una imagen está actualmente en la lista de descartes.
     * @param rutaAbsolutaImagen La ruta de la imagen a comprobar.
     * @return true si está en descartes, false en caso contrario.
     */
    boolean estaEnDescartes(Path rutaAbsolutaImagen);

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
     * Verifica si una imagen (dada por su Path absoluto) está actualmente marcada en la SELECCIÓN PRINCIPAL.
     * @param rutaAbsolutaImagen El Path absoluto de la imagen a verificar.
     * @return true si la imagen está marcada, false en caso contrario.
     */
    boolean estaMarcada(Path rutaAbsolutaImagen);

    /**
     * Alterna el estado de marca de una imagen (dada por su Path absoluto) en la SELECCIÓN PRINCIPAL.
     * @param rutaAbsolutaImagen El Path absoluto de la imagen.
     * @return true si la imagen quedó marcada, false si quedó desmarcada.
     */
    boolean alternarMarcaImagen(Path rutaAbsolutaImagen);
    
    /**
     * Elimina permanentemente una imagen del proyecto, quitándola tanto de la
     * lista de selección actual como de la de descartes.
     *
     * @param rutaAbsolutaImagen La ruta de la imagen a eliminar del proyecto.
     */
    void eliminarDeProyecto(Path rutaAbsolutaImagen);
    
    
    void vaciarDescartes();
    
    /**
     * Obtiene la etiqueta personalizada para una imagen del proyecto.
     * @param rutaImagen La ruta de la imagen.
     * @return La etiqueta como un String, o null si no tiene ninguna.
     */
    String getEtiqueta(Path rutaImagen);

    /**
     * Establece o actualiza la etiqueta para una imagen del proyecto.
     * Si la etiqueta es null o vacía, se considera borrada.
     * @param rutaImagen La ruta de la imagen a etiquetar.
     * @param etiqueta El texto de la etiqueta.
     */
    void setEtiqueta(Path rutaImagen, String etiqueta);

    
    public void limpiarArchivoTemporal();
    void guardarAArchivo();
    void notificarModificacion();
    public boolean hayCambiosSinGuardar();
    void archivarTemporalAlCerrar();

} // --- FIN de la interfaz IProjectManager ---