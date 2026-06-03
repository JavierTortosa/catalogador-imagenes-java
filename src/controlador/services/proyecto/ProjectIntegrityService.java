package controlador.services.proyecto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.DataManager;
import controlador.managers.interfaces.IProjectManager;
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

        Map<String, String> seleccionadas = new HashMap<>(modeloActual.getSelectedImages());
        Map<String, String> nuevasSeleccionadas = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : seleccionadas.entrySet())
        {
            String pathStr = entry.getKey();
            String etiqueta = entry.getValue();
            Path path = Path.of(pathStr);

            if (!Files.exists(path))
            {
                String filename = path.getFileName().toString();
                Optional<String> nuevoPathOpt = dm.findPathByFileName(filename);

                if (nuevoPathOpt.isPresent() && Files.exists(Path.of(nuevoPathOpt.get())))
                {
                    String nuevoPathStr = nuevoPathOpt.get();
                    logger.info("¡AUTO-HEAL! Imagen relocalizada automáticamente: {} -> {}", pathStr, nuevoPathStr);
                    nuevasSeleccionadas.put(nuevoPathStr, etiqueta);
                    migrarExportConfig(modeloActual, pathStr, nuevoPathStr);
                    huboCambios = true;
                    continue;
                }
            }
            nuevasSeleccionadas.put(pathStr, etiqueta);
        }
        if (huboCambios)
            modeloActual.setSelectedImages(nuevasSeleccionadas);

        List<String> descartadas = new ArrayList<>(modeloActual.getDiscardedImages());
        List<String> nuevasDescartadas = new ArrayList<>();
        boolean huboCambiosDescartadas = false;

        for (String pathStr : descartadas)
        {
            Path path = Path.of(pathStr);
            if (!Files.exists(path))
            {
                String filename = path.getFileName().toString();
                Optional<String> nuevoPathOpt = dm.findPathByFileName(filename);

                if (nuevoPathOpt.isPresent() && Files.exists(Path.of(nuevoPathOpt.get())))
                {
                    String nuevoPathStr = nuevoPathOpt.get();
                    logger.info("¡AUTO-HEAL! Descartes relocalizado: {} -> {}", pathStr, nuevoPathStr);
                    nuevasDescartadas.add(nuevoPathStr);
                    migrarExportConfig(modeloActual, pathStr, nuevoPathStr);
                    huboCambiosDescartadas = true;
                    continue;
                }
            }
            nuevasDescartadas.add(pathStr);
        }

        if (huboCambiosDescartadas)
        {
            modeloActual.setDiscardedImages(nuevasDescartadas);
            huboCambios = true;
        }

        if (huboCambios)
            projectManager.notificarModificacion();
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

        for (String pathStr : modeloActual.getSelectedImages().keySet())
        {
            if (!Files.exists(Path.of(pathStr)))
                report.seleccionadas.add(pathStr);
        }
        for (String pathStr : modeloActual.getDiscardedImages())
        {
            if (!Files.exists(Path.of(pathStr)))
                report.descartadas.add(pathStr);
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
            modeloActual.getSelectedImages().remove(pathStr);
            modeloActual.getExportConfigs().remove(pathStr.replace("\\", "/"));
        }

        modeloActual.getDiscardedImages().removeAll(report.descartadas);
        for (String pathStr : report.descartadas)
        {
            modeloActual.getExportConfigs().remove(pathStr.replace("\\", "/"));
        }
        return report.getTotal();
    } // --- Fin del metodo: limpiarHuerfanos ---

} // --- Fin de la clase ProjectIntegrityService ---
