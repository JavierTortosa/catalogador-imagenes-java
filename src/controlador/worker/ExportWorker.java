package controlador.worker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import modelo.proyecto.ExportItem;
import vista.dialogos.ExportProgressDialog;

public class ExportWorker extends SwingWorker<String, String> {

    private final List<ExportItem> cola;
    private final Path carpetaDestino;
    private final ExportProgressDialog dialogo;

    public ExportWorker(List<ExportItem> cola, Path carpetaDestino, ExportProgressDialog dialogo) {
        this.cola = cola;
        this.carpetaDestino = carpetaDestino;
        this.dialogo = dialogo;
    } // --- Fin del método ExportWorker (constructor) ---

    @Override
    protected String doInBackground() throws Exception {
        int totalFiles = cola.size() * 2; // Cada item tiene imagen + archivo comprimido
        int filesCopied = 0;
        
        for (ExportItem item : cola) {
            if (isCancelled()) {
                return "Cancelado por el usuario.";
            }

            // Copiar archivo de imagen
            publish("Copiando imagen: " + item.getRutaImagen().getFileName());
            copyFile(item.getRutaImagen());
            filesCopied++;
            setProgress((int) ((double) filesCopied / totalFiles * 100));

            // Copiar archivo comprimido
            publish("Copiando archivo: " + item.getRutaArchivoComprimido().getFileName());
            copyFile(item.getRutaArchivoComprimido());
            filesCopied++;
            setProgress((int) ((double) filesCopied / totalFiles * 100));
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
            String resultado = get();
            JOptionPane.showMessageDialog(dialogo.getParent(), resultado, "Resultado de la Exportación", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(dialogo.getParent(), "Ocurrió un error durante la exportación:\n" + e.getCause().getMessage(), "Error de Exportación", JOptionPane.ERROR_MESSAGE);
        }
    } // --- Fin del método done ---

} // --- FIN de la clase ExportWorker ---