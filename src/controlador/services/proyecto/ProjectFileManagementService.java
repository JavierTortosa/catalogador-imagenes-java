package controlador.services.proyecto;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.ExportQueueManager;
import controlador.managers.interfaces.IProjectManager;
import modelo.proyecto.ExportItem;
import modelo.proyecto.ExportStatus;
import modelo.proyecto.ProjectImage;
import modelo.proyecto.ProjectModel;

public class ProjectFileManagementService
{
    private static final Logger logger = LoggerFactory.getLogger(ProjectFileManagementService.class);

    private final IProjectManager projectManager;
    private final ExportQueueManager exportQueueManager;

    public ProjectFileManagementService(IProjectManager projectManager, ExportQueueManager exportQueueManager)
    {
        this.projectManager = projectManager;
        this.exportQueueManager = exportQueueManager;
    }

    public void addAssociatedFile(ExportItem item, Path filePath)
    {
        item.addRutaArchivoAsociado(filePath);
        projectManager.addAssociatedFile(item.getRutaImagen(), filePath);
        item.setEstadoArchivoComprimido(ExportStatus.ASIGNADO_MANUAL);
    } // --- Fin del metodo: addAssociatedFile ---


    public void removeAssociatedFile(ExportItem item, Path filePath)
    {
        item.getRutasArchivosAsociados().remove(filePath);
        projectManager.removeAssociatedFile(item.getRutaImagen(), filePath);

        if (item.getRutasArchivosAsociados().isEmpty())
        {
            if (item.getCandidatosArchivo() != null && !item.getCandidatosArchivo().isEmpty())
            {
                item.setRutasArchivosAsociados(new ArrayList<>(item.getCandidatosArchivo()));
                item.setEstadoArchivoComprimido(ExportStatus.ENCONTRADO_OK);
            }
            else
            {
                item.setEstadoArchivoComprimido(ExportStatus.NO_ENCONTRADO);
            }
        }
    } // --- Fin del metodo: removeAssociatedFile ---


    public void addFilesToProject(List<Path> files)
    {
        for (Path file : files)
        {
            projectManager.marcarImagen(file);
        }
    } // --- Fin del metodo: addFilesToProject ---


    public void relocalizarImagenEnCola(ExportItem oldItem, Path newPath, int fila)
    {
        ExportItem newItem = new ExportItem(newPath);
        exportQueueManager.buscarArchivoComprimidoAsociado(newItem);
        exportQueueManager.getColaDeExportacion().set(fila, newItem);
    } // --- Fin del metodo: relocalizarImagenEnCola ---


    public void migrarClaveEnModelo(ProjectModel projectModel, Path oldPath, Path newPath)
    {
        if (projectModel == null) return;
        String oldNorm = ProjectModel.normalizarClaveImagen(oldPath.toString());
        String newNorm = ProjectModel.normalizarClaveImagen(newPath.toString());

        ProjectImage pi = projectModel.getMasterImages().remove(oldNorm);
        if (pi != null)
        {
            pi.setRutaImagen(newNorm);
            projectModel.getMasterImages().put(newNorm, pi);
        }

        String oldClaveExport = oldNorm;
        String newClaveExport = newNorm;
        if (projectModel.getExportConfigs().containsKey(oldClaveExport))
        {
            modelo.proyecto.ExportConfig cfg = projectModel.getExportConfigs().remove(oldClaveExport);
            projectModel.getExportConfigs().put(newClaveExport, cfg);
        }
    } // --- Fin del metodo: migrarClaveEnModelo ---


    public int eliminarDelProyecto(List<Path> rutas)
    {
        return projectManager.eliminarVariosDeProyecto(rutas);
    } // --- Fin del metodo: eliminarDelProyecto ---


    public void relocalizarImagen(Path oldPath, Path newPath)
    {
        projectManager.relocalizarImagen(oldPath, newPath);
    } // --- Fin del metodo: relocalizarImagen ---

} // --- Fin de la clase ProjectFileManagementService ---
