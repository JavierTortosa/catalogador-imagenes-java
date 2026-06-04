package controlador.services.proyecto;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.export.pdf.PDFExportPreflightService;
import modelo.export.pdf.PDFGeneratorService;
import modelo.proyecto.ExportConfig;
import modelo.proyecto.ExportItem;
import modelo.proyecto.ProjectModel;

public class PdfWorkflowService
{
    private static final Logger logger = LoggerFactory.getLogger(PdfWorkflowService.class);

    // Asigna códigos de catálogo correlativos (C001, C002, ...) a los items seleccionados
    public void asignarCodigosCatalogo(List<ExportItem> items)
    {
        if (items == null || items.isEmpty()) return;
        int numDigitos = String.valueOf(items.size()).length();
        if (numDigitos < 3) numDigitos = 3;
        String formato = "C%0" + numDigitos + "d";
        for (int i = 0; i < items.size(); i++)
        {
            items.get(i).setCodigoCatalogo(String.format(formato, i + 1));
        }
    } // --- Fin del metodo: asignarCodigosCatalogo ---


    // Ejecuta el preflight de PDF extrayendo las rutas de imagen de los items
    public PDFExportPreflightService.PreflightResult ejecutarPreflight(List<ExportItem> items)
    {
        List<Path> paths = items.stream()
                .map(ExportItem::getRutaImagen)
                .collect(Collectors.toList());
        return new PDFExportPreflightService().checkPreflight(paths);
    } // --- Fin del metodo: ejecutarPreflight ---


    // Genera el archivo PDF en la ruta destino con los items y notas del proyecto
    public void generarPDF(List<ExportItem> items, Path destino, String notasProyecto) throws Exception
    {
        new PDFGeneratorService().crearPresupuesto(items, destino.toFile(), notasProyecto);
    } // --- Fin del metodo: generarPDF ---


    // Persiste los datos de catálogo (código, piezas, lvl, pvp, notas) en el modelo del proyecto
    public boolean sincronizarDatosCatalogoConModelo(List<ExportItem> items, ProjectModel modeloActual)
    {
        if (modeloActual == null) return false;
        Map<String, ExportConfig> exportConfigsMap = modeloActual.getExportConfigs();
        boolean modificado = false;
        for (ExportItem item : items)
        {
            String claveImagen = item.getRutaImagen().toString().replace("\\", "/");
            ExportConfig config = exportConfigsMap.computeIfAbsent(claveImagen, k -> new ExportConfig());
            if (!Objects.equals(config.getCodigoCatalogo(), item.getCodigoCatalogo())
                    || config.getPiezas() != item.getPiezas()
                    || config.getPiezasConSoporte() != item.getPiezasConSoporte()
                    || config.getPiezasSinSoporte() != item.getPiezasSinSoporte()
                    || !Objects.equals(config.getLvl(), item.getLvl())
                    || !Objects.equals(config.getPvp(), item.getPvp())
                    || !Objects.equals(config.getNotas(), item.getNotas()))
            {
                modificado = true;
            }
            config.setCodigoCatalogo(item.getCodigoCatalogo());
            config.setPiezas(item.getPiezas());
            config.setPiezasConSoporte(item.getPiezasConSoporte());
            config.setPiezasSinSoporte(item.getPiezasSinSoporte());
            config.setLvl(item.getLvl());
            config.setPvp(item.getPvp());
            config.setNotas(item.getNotas());
        }
        return modificado;
    } // --- Fin del metodo: sincronizarDatosCatalogoConModelo ---

} // --- Fin de la clase PdfWorkflowService ---
