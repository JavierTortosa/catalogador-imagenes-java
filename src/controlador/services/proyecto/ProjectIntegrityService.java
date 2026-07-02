package controlador.services.proyecto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.DataManager;
import controlador.managers.interfaces.IProjectManager;
import modelo.proyecto.ProjectImage;
import modelo.proyecto.ProjectModel;

public class ProjectIntegrityService
{
    private static final Logger logger = LoggerFactory.getLogger(ProjectIntegrityService.class);
    private final IProjectManager projectManager;

    public ProjectIntegrityService(IProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

    public boolean autoRelocalizarImagenesHuerfanas(DataManager dm)
    {
        if (dm == null || projectManager.getCurrentProject() == null)
            return false;

        ProjectModel modeloActual = projectManager.getCurrentProject();
        boolean huboCambios = false;
        Map<String, ProjectImage> nuevosMaster = new java.util.LinkedHashMap<>();

        for (Map.Entry<String, ProjectImage> entry : modeloActual.getMasterImages().entrySet())
        {
            String oldKey = entry.getKey();
            ProjectImage pi = entry.getValue();

            if (!Files.exists(Path.of(oldKey)))
            {
                String filename = Path.of(oldKey).getFileName().toString();
                Optional<String> nuevoPathOpt = dm.findPathByFileName(filename);

                if (nuevoPathOpt.isPresent() && Files.exists(Path.of(nuevoPathOpt.get())))
                {
                    String nuevoPathStr = ProjectModel.normalizarClaveImagen(nuevoPathOpt.get());
                    logger.info("¡AUTO-HEAL! Imagen relocalizada automáticamente: {} -> {}", oldKey, nuevoPathStr);
                    pi.setRutaImagen(nuevoPathStr);
                    migrarExportConfig(modeloActual, oldKey, nuevoPathStr);
                    nuevosMaster.put(nuevoPathStr, pi);
                    huboCambios = true;
                    continue;
                }
            }
            nuevosMaster.put(oldKey, pi);
        }

        if (huboCambios)
        {
            modeloActual.setMasterImages(nuevosMaster);
            projectManager.notificarModificacion();
        }
        return huboCambios;
    } // --- Fin del metodo: autoRelocalizarImagenesHuerfanas ---


    private void migrarExportConfig(ProjectModel modeloActual, String oldKey, String newKey)
    {
        String claveVieja = oldKey.replace("\\", "/");
        String claveNueva = newKey.replace("\\", "/");
        if (modeloActual.getExportConfigs().containsKey(claveVieja))
        {
            modeloActual.getExportConfigs().put(claveNueva, modeloActual.getExportConfigs().remove(claveVieja));
        }
    } // --- Fin del metodo: migrarExportConfig ---


    public static class OrphanReport
    {
        public final List<String> seleccionadas = new ArrayList<>();
        public final List<String> descartadas = new ArrayList<>();
        public boolean isEmpty() { return seleccionadas.isEmpty() && descartadas.isEmpty(); }
        public int getTotal() { return seleccionadas.size() + descartadas.size(); }
    }


    public OrphanReport identificarHuerfanos()
    {
        OrphanReport report = new OrphanReport();
        ProjectModel modeloActual = projectManager.getCurrentProject();
        if (modeloActual == null)
            return report;

        for (ProjectImage pi : modeloActual.getMasterImages().values())
        {
            String pathStr = pi.getRutaImagen();
            if (pathStr != null && !Files.exists(Path.of(pathStr)))
            {
                if (pi.isEnSeleccionProyecto())
                    report.seleccionadas.add(pathStr);
                else
                    report.descartadas.add(pathStr);
            }
        }
        return report;
    } // --- Fin del metodo: identificarHuerfanos ---


    public int limpiarHuerfanos(OrphanReport report)
    {
        ProjectModel modeloActual = projectManager.getCurrentProject();
        if (modeloActual == null || report.isEmpty())
            return 0;

        for (String pathStr : report.seleccionadas)
        {
            String canonical = ProjectModel.normalizarClaveImagen(pathStr);
            modeloActual.getMasterImages().remove(canonical);
            modeloActual.getExportConfigs().remove(canonical);
        }

        for (String pathStr : report.descartadas)
        {
            String canonical = ProjectModel.normalizarClaveImagen(pathStr);
            modeloActual.getMasterImages().remove(canonical);
            modeloActual.getExportConfigs().remove(canonical);
        }
        return report.getTotal();
    } // --- Fin del metodo: limpiarHuerfanos ---

} // --- Fin de la clase ProjectIntegrityService ---
