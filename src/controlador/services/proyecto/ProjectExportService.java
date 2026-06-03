package controlador.services.proyecto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.proyecto.ExportItem;
import modelo.proyecto.ExportStatus;

public class ProjectExportService
{
    private static final Logger logger = LoggerFactory.getLogger(ProjectExportService.class);

    // Validación previa a la exportación: carpeta destino y conflictos de sobrescritura
    public ExportPreflightReport validarPreflight(List<ExportItem> itemsAExportar, Path carpetaDestino)
    {
        if (itemsAExportar == null || itemsAExportar.isEmpty())
        {
            return new ExportPreflightReport(false, "No hay archivos seleccionados en la cola para exportar.", null, 0);
        }

        if (!Files.exists(carpetaDestino))
        {
            return new ExportPreflightReport(false,
                    "La carpeta de destino no existe:\n" + carpetaDestino, null, itemsAExportar.size());
        }
        if (!Files.isDirectory(carpetaDestino))
        {
            return new ExportPreflightReport(false,
                    "La ruta de destino no es una carpeta:\n" + carpetaDestino, null, itemsAExportar.size());
        }
        if (!Files.isWritable(carpetaDestino))
        {
            return new ExportPreflightReport(false,
                    "No se tienen permisos de escritura en la carpeta de destino:\n" + carpetaDestino, null,
                    itemsAExportar.size());
        }

        List<String> archivosEnConflicto = new ArrayList<>();
        for (ExportItem item : itemsAExportar)
        {
            Path pImg = item.getRutaImagen();
            Path fnImg = pImg.getFileName();
            Path destinoImagen = carpetaDestino.resolve((fnImg != null) ? fnImg : pImg);
            if (Files.exists(destinoImagen))
            {
                Path dImgFn = destinoImagen.getFileName();
                archivosEnConflicto.add((dImgFn != null) ? dImgFn.toString() : destinoImagen.toString());
            }
            for (Path asociado : item.getRutasArchivosAsociados())
            {
                Path fnAsc = asociado.getFileName();
                Path destinoAsociado = carpetaDestino.resolve((fnAsc != null) ? fnAsc : asociado);
                if (Files.exists(destinoAsociado))
                {
                    Path dAscFn = destinoAsociado.getFileName();
                    archivosEnConflicto.add((dAscFn != null) ? dAscFn.toString() : destinoAsociado.toString());
                }
            }
        }

        return new ExportPreflightReport(true, null, archivosEnConflicto, itemsAExportar.size());
    } // --- Fin del metodo: validarPreflight ---


    // Detecta conflictos de nombres duplicados (imagen y archivos asociados) en la cola de exportación
    public ExportStatusReport detectarConflictosEnCola(
            List<ExportItem> colaCompleta, List<ExportItem> itemsSeleccionados)
    {
        Set<String> nombresDeImagenDuplicados = itemsSeleccionados.stream()
                .map(item -> {
                    Path p = item.getRutaImagen();
                    Path fn = p.getFileName();
                    return (fn != null) ? fn.toString() : p.toString();
                })
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Map<String, List<Path>> archivosAsignadosPorNombre = itemsSeleccionados.stream()
                .filter(item -> item.getRutasArchivosAsociados() != null && !item.getRutasArchivosAsociados().isEmpty())
                .flatMap(item -> item.getRutasArchivosAsociados().stream())
                .collect(Collectors.groupingBy(path -> {
                    Path fn = path.getFileName();
                    return (fn != null) ? fn.toString() : path.toString();
                }));

        Set<String> nombresDeAsignadosDuplicados = archivosAsignadosPorNombre.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        for (ExportItem item : colaCompleta)
        {
            Path pImg = item.getRutaImagen();
            Path fnImg = pImg.getFileName();
            String sImg = (fnImg != null) ? fnImg.toString() : pImg.toString();
            boolean conflictoImagen = nombresDeImagenDuplicados.contains(sImg);
            item.setTieneConflictoDeNombre(conflictoImagen);

            if (!conflictoImagen && item.getRutasArchivosAsociados() != null)
            {
                boolean conflictoAsignado = item.getRutasArchivosAsociados().stream()
                        .anyMatch(path -> {
                            Path fn = path.getFileName();
                            String s = (fn != null) ? fn.toString() : path.toString();
                            return nombresDeAsignadosDuplicados.contains(s);
                        });
                if (conflictoAsignado)
                {
                    if (item.getEstadoArchivoComprimido() != ExportStatus.ASIGNADO_DUPLICADO)
                    {
                        item.setEstadoArchivoComprimido(ExportStatus.ASIGNADO_DUPLICADO);
                    }
                }
                else
                {
                    if (item.getEstadoArchivoComprimido() == ExportStatus.ASIGNADO_DUPLICADO)
                    {
                        if (item.getCandidatosArchivo() != null && !item.getCandidatosArchivo().isEmpty())
                        {
                            item.setEstadoArchivoComprimido(ExportStatus.ENCONTRADO_OK);
                        }
                        else
                        {
                            item.setEstadoArchivoComprimido(ExportStatus.ASIGNADO_MANUAL);
                        }
                    }
                }
            }
        }

        return new ExportStatusReport(nombresDeImagenDuplicados, nombresDeAsignadosDuplicados);
    } // --- Fin del metodo: detectarConflictosEnCola ---

} // --- Fin de la clase ProjectExportService ---
