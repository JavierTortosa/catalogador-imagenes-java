package controlador.managers.interfaces;

/**
 * Interfaz (Contrato) que define las responsabilidades del FileOperationsManager.
 * Define las operaciones de alto nivel sobre archivos y carpetas, como
 * abrir un selector de carpetas o eliminar un archivo.
 */
public interface IFileOperationsManager {

    /**
     * Abre un diálogo para que el usuario seleccione una nueva carpeta raíz.
     * Si la selección es exitosa, actualiza la configuración y dispara un callback.
     */
    void solicitarSeleccionNuevaCarpeta();

    /**
     * Inicia el proceso para borrar el archivo actualmente seleccionado en el modelo.
     * Pide confirmación al usuario antes de proceder.
     */
    void borrarArchivoSeleccionado();

} // --- FIN de la interfaz IFileOperationsManager ---