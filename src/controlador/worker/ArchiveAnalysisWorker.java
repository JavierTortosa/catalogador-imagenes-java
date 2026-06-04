package controlador.worker;

import javax.swing.*;
import java.util.List;
import java.nio.file.Path;
import modelo.proyecto.ExportItem;
import servicios.ArchiveAnalysisService;
import servicios.db.ArchiveMetadataDAO;
import modelo.datos.ArchiveMetadata;
import vista.dialogos.TaskProgressDialog;

public class ArchiveAnalysisWorker extends SwingWorker<Void, String> {
    private List<ExportItem> items;
    private TaskProgressDialog dialog;
    private Runnable onDone;

    public ArchiveAnalysisWorker(List<ExportItem> items, TaskProgressDialog dialog, Runnable onDone) {
        this.items = items;
        this.dialog = dialog;
        this.onDone = onDone;
        // Vinculamos el worker al diálogo para que funcione el botón "Cancelar"
        this.dialog.setWorkerAsociado(this);
    }

    @Override
    protected Void doInBackground() throws Exception {
        ArchiveAnalysisService service = new ArchiveAnalysisService();
        ArchiveMetadataDAO dao = new ArchiveMetadataDAO();
        
        int total = items.size();
        for (int i = 0; i < total; i++) {
            if (isCancelled()) return null; // Soporte para el botón cancelar

            ExportItem item = items.get(i);
            publish("Analizando: " + item.getRutaImagen().getFileName());
            
            if (item.getRutasArchivosAsociados() != null) {
                for (Path archivePath : item.getRutasArchivosAsociados()) {
                    ArchiveMetadata meta = dao.buscarPorRuta(archivePath.toString());
                    if (meta == null) {
                        meta = service.analyze(archivePath);
                        dao.guardar(meta);
                    }
                    item.setMetadata(meta);
                    // Auto-poblar solo campos que el usuario no haya editado (valor 0 = sin tocar)
                    if (item.getPiezasConSoporte() <= 0) {
                        item.setPiezasConSoporte(meta.supportedStlCount);
                    }
                    if (item.getPiezasSinSoporte() <= 0) {
                        item.setPiezasSinSoporte(meta.unsupportedStlCount);
                    }
                    if (item.getPiezas() <= 0 && meta.stlCount > 0) {
                        item.setPiezas(meta.stlCount);
                    }
                }
            }
            setProgress((i + 1) * 100 / total);
        }
        return null;
    }

    @Override
    protected void process(List<String> chunks) {
        String latestMessage = chunks.get(chunks.size() - 1);
        // Usamos el método que me has pasado en tu código de TaskProgressDialog
        dialog.updateStatusText(latestMessage);
    }

    @Override
    protected void done() {
        if (!isCancelled()) {
            SwingUtilities.invokeLater(() -> {
                dialog.closeDialog();
                if (onDone != null) onDone.run();
            });
        }
    }
}