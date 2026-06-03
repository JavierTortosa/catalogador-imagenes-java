package controlador.services.proyecto;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.ExportQueueManager;
import controlador.managers.interfaces.IProjectManager;
import modelo.proyecto.ExportItem;
import modelo.proyecto.ExportStatus;
import modelo.proyecto.ProjectModel;

public class ProjectSyncService
{
    private static final Logger logger = LoggerFactory.getLogger(ProjectSyncService.class);

    private final IProjectManager projectManager;
    private final ExportQueueManager exportQueueManager;

    public ProjectSyncService(IProjectManager projectManager, ExportQueueManager exportQueueManager)
    {
        this.projectManager = projectManager;
        this.exportQueueManager = exportQueueManager;
    }

    public void sincronizarListas(ProjectModel model,
            List<String> elementosSeleccion, List<String> elementosDescartes,
            Map<String, String> etiquetasExistentes)
    {
        if (model == null) return;

        model.getSelectedImages().clear();
        for (String clave : elementosSeleccion)
        {
            String etiqueta = etiquetasExistentes.get(clave);
            model.getSelectedImages().put(clave, etiqueta);
        }

        model.getDiscardedImages().clear();
        model.getDiscardedImages().addAll(elementosDescartes);
    } // --- Fin del metodo: sincronizarListas ---


    public void sincronizarArchivosAsociadosConModelo()
    {
        if (projectManager == null || exportQueueManager == null) return;

        ProjectModel modeloActual = projectManager.getCurrentProject();
        if (modeloActual == null) return;

        Map<String, modelo.proyecto.ExportConfig> exportConfigsMap = modeloActual.getExportConfigs();
        exportConfigsMap.clear();

        List<ExportItem> colaActual = exportQueueManager.getColaDeExportacion();
        int contador = 0;

        for (ExportItem item : colaActual)
        {
            modelo.proyecto.ExportConfig config = new modelo.proyecto.ExportConfig();

            config.setExportEnabled(item.isSeleccionadoParaExportar());
            config.setIgnoreCompressed(
                    item.getEstadoArchivoComprimido() == ExportStatus.IGNORAR_COMPRIMIDO);
            config.setStatus(item.getEstadoArchivoComprimido());

            if (item.getRutasArchivosAsociados() != null && !item.getRutasArchivosAsociados().isEmpty())
            {
                List<String> rutasComoString = item.getRutasArchivosAsociados().stream()
                        .map(path -> path.toString().replace("\\", "/"))
                        .collect(Collectors.toList());
                config.setAssociatedFiles(rutasComoString);
            }

            config.setCodigoCatalogo(item.getCodigoCatalogo());
            config.setPiezas(item.getPiezas());
            config.setLvl(item.getLvl());
            config.setPvp(item.getPvp());
            config.setNotas(item.getNotas());

            String claveImagen = item.getRutaImagen().toString().replace("\\", "/");
            exportConfigsMap.put(claveImagen, config);
            contador++;
        }

        logger.info("Sincronización de configuración de exportación completada. Se persistirán {} entradas.", contador);
    } // --- Fin del metodo: sincronizarArchivosAsociadosConModelo ---


    public void sincronizarDescripcion(ProjectModel model, String descripcionTexto)
    {
        if (model == null) return;
        model.setProjectDescription(descripcionTexto);
        logger.debug("Descripción del ProjectModel sincronizada.");
    } // --- Fin del metodo: sincronizarDescripcion ---


    public List<String> getSourceData(String nombreListaActiva)
    {
        List<Path> sourceData;
        if ("descartes".equals(nombreListaActiva))
        {
            sourceData = projectManager.getImagenesDescartadas();
            logger.debug("Fuente de datos para masterList: Descartes ({} elementos)", sourceData.size());
        }
        else
        {
            sourceData = projectManager.getImagenesMarcadas();
            logger.debug("Fuente de datos para masterList: Selección ({} elementos)", sourceData.size());
        }
        return sourceData.stream()
                .map(p -> p.toString().replace("\\", "/"))
                .collect(Collectors.toList());
    } // --- Fin del metodo: getSourceData ---

} // --- Fin de la clase ProjectSyncService ---
