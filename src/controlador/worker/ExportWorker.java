package controlador.worker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.proyecto.ExportItem;
import vista.dialogos.TaskProgressDialog;

public class ExportWorker extends SwingWorker<String, String> {

	private static final Logger logger = LoggerFactory.getLogger(ExportWorker.class);
	
    private final List<ExportItem> cola;
    private final Path carpetaDestino;
    private final TaskProgressDialog dialogo;

    public ExportWorker(List<ExportItem> cola, Path carpetaDestino, TaskProgressDialog dialogo) {
        this.cola = cola;
        this.carpetaDestino = carpetaDestino;
        this.dialogo = dialogo;
    } // --- Fin del método ExportWorker (constructor) ---

    @Override
    protected String doInBackground() throws Exception {
        // Calcular el número total de archivos a copiar de forma más precisa.
        // Contamos una vez por cada imagen + una vez por cada archivo comprimido que NO se ignore.
        long archivosComprimidosACopiar = cola.stream()
                                           .filter(item -> item.getEstadoArchivoComprimido() != modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO)
                                           .count();
        int totalFilesToCopy = cola.size() + (int) archivosComprimidosACopiar;
        int filesCopied = 0;
        
        for (ExportItem item : cola) {
            if (isCancelled()) {
                return "Cancelado por el usuario.";
            }

            // --- Copiar archivo de imagen ---
            publish("Copiando imagen: " + item.getRutaImagen().getFileName());
            copyFile(item.getRutaImagen());
            filesCopied++;
            setProgress((int) ((double) filesCopied / totalFilesToCopy * 100));

            // --- Copiar archivo comprimido SOLO si no se ha ignorado ---
            if (item.getEstadoArchivoComprimido() != modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO) {
                // Verificar que la ruta del archivo comprimido no sea nula antes de intentar copiar
                if (item.getRutaArchivoComprimido() != null) {
                    publish("Copiando archivo: " + item.getRutaArchivoComprimido().getFileName());
                    copyFile(item.getRutaArchivoComprimido());
                    filesCopied++;
                    setProgress((int) ((double) filesCopied / totalFilesToCopy * 100));
                } else {
                    // Esto es un caso de error, la lógica debería haber prevenido llegar aquí
                    // con un estado ENCONTRADO_OK pero una ruta nula. Lo saltamos.
                    logger.warn("WARN [ExportWorker]: Se intentó copiar un archivo comprimido nulo para la imagen " + item.getRutaImagen().getFileName());
                }
            }
            // --- FIN DE LA MODIFICACIÓN ---
        }
        return "Exportación completada con éxito. " + filesCopied + " archivos copiados.";
    } // --- Fin del método doInBackground ---
    
    private void copyFile(Path source) throws IOException {
        Path destination = carpetaDestino.resolve(source.getFileName());
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    } // --- Fin del método copyFile ---

    @Override
    protected void process(List<String> chunks) {
        // Actualizar la etiqueta del archivo actual en el diálogo
        String ultimoMensaje = chunks.get(chunks.size() - 1);
        dialogo.setCurrentFileText(ultimoMensaje);
    } // --- Fin del método process ---

    
    @Override
    protected void done() {
        dialogo.setVisible(false);
        dialogo.dispose();
        
        try {
            String resultado = get(); // Obtenemos el mensaje de éxito o cancelación.

            // Si la operación fue cancelada, mostramos un mensaje simple y terminamos.
            if (isCancelled()) {
                JOptionPane.showMessageDialog(dialogo.getParent(), resultado, "Exportación Cancelada", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 1. Definimos los botones que queremos mostrar.
            Object[] options = {"Aceptar", "Abrir Carpeta de Destino"};

            // 2. Mostramos el diálogo de opciones.
            int choice = JOptionPane.showOptionDialog(
                dialogo.getParent(),                    // Componente padre
                resultado,                              // Mensaje a mostrar (ej. "Exportación completada...")
                "Resultado de la Exportación",          // Título del diálogo
                JOptionPane.DEFAULT_OPTION,             // Tipo de opción (no afecta mucho aquí)
                JOptionPane.INFORMATION_MESSAGE,        // Icono de información
                null,                                   // Sin icono personalizado
                options,                                // Los botones que hemos definido
                options[0]                              // Botón por defecto ("Aceptar")
            );

            // 3. Evaluamos la elección del usuario.
            //    choice será 0 para "Aceptar", 1 para "Abrir Carpeta de Destino".
            if (choice == 1) {
                logger.debug("[ExportWorker] El usuario ha elegido abrir la carpeta de destino: " + carpetaDestino);
                try {
                    // Usamos java.awt.Desktop para abrir la carpeta. Es la forma más compatible.
                    java.awt.Desktop.getDesktop().open(carpetaDestino.toFile());
                } catch (IOException ex) {
                    logger.error("Error al intentar abrir la carpeta de destino: " + ex.getMessage());
                    JOptionPane.showMessageDialog(
                        dialogo.getParent(), 
                        "No se pudo abrir la carpeta:\n" + carpetaDestino.toString(), 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }

        } catch (Exception e) {
            // La gestión de errores se queda igual.
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                dialogo.getParent(), 
                "Ocurrió un error durante la exportación:\n" + e.getMessage(), 
                "Error de Exportación", 
                JOptionPane.ERROR_MESSAGE
            );
        }
        
    } // --- Fin del método done ---
    

} // --- FIN de la clase ExportWorker ---