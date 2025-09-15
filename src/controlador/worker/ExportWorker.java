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
        // --- 1. CÁLCULO DE PROGRESO PRECISO ---
        // Contamos cada imagen + CADA UNO de sus archivos asociados.
        int totalFilesToCopy = 0;
        for (ExportItem item : cola) {
            totalFilesToCopy++; // Contamos la imagen en sí.
            if (item.getEstadoArchivoComprimido() != modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO) {
                if (item.getRutasArchivosAsociados() != null) {
                    totalFilesToCopy += item.getRutasArchivosAsociados().size(); // Contamos TODOS los asociados.
                }
            }
        }

        int filesCopied = 0;
        
        // --- 2. BUCLE PRINCIPAL DE COPIA ---
        for (ExportItem item : cola) {
            if (isCancelled()) {
                return "Cancelado por el usuario.";
            }

            // --- 2a. Copiar el archivo de imagen ---
            publish("Copiando imagen: " + item.getRutaImagen().getFileName());
            copyFile(item.getRutaImagen());
            filesCopied++;
            setProgress((int) ((double) filesCopied / totalFilesToCopy * 100));

            // --- 2b. Copiar TODOS los archivos asociados (si procede) ---
            if (item.getEstadoArchivoComprimido() != modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO) {
                
                // Usamos el método correcto que devuelve la LISTA de archivos.
                List<Path> archivosAsociados = item.getRutasArchivosAsociados();
                
                if (archivosAsociados != null && !archivosAsociados.isEmpty()) {
                    // ¡EL BUCLE CLAVE! Iteramos sobre CADA archivo en la lista.
                    for (Path archivoAExportar : archivosAsociados) {
                        if (isCancelled()) return "Cancelado por el usuario.";
                        
                        publish("Copiando asociado: " + archivoAExportar.getFileName());
                        copyFile(archivoAExportar);
                        filesCopied++;
                        setProgress((int) ((double) filesCopied / totalFilesToCopy * 100));
                    }
                }
            }
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