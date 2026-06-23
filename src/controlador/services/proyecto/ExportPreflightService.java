package controlador.services.proyecto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.proyecto.ExportConfig;
import modelo.proyecto.ProjectModel;

/**
 * Servicio de validación previa a la exportación al cliente.
 * Comprueba que todas las imágenes seleccionadas tengan código de catálogo asignado.
 */
public class ExportPreflightService {

    private static final Logger logger = LoggerFactory.getLogger(ExportPreflightService.class);

    public static List<String> validarAsignaciones(ProjectModel project) {
        List<String> errores = new ArrayList<>();
        if (project == null) {
            errores.add("No hay proyecto activo.");
            return errores;
        }

        Map<String, String> selected = project.getSelectedImages();
        if (selected == null || selected.isEmpty()) {
            errores.add("No hay imágenes seleccionadas en el proyecto.");
            return errores;
        }

        Map<String, ExportConfig> configs = project.getExportConfigs();

        for (String rutaImagen : selected.keySet()) {
            ExportConfig config = configs.get(rutaImagen);
            if (config == null || config.getCodigoCatalogo() == null || config.getCodigoCatalogo().isBlank()) {
                errores.add("La imagen '" + rutaImagen + "' no tiene código de catálogo asignado.");
            }
        }

        return errores;
    } // --- Fin de metodo validarAsignaciones ---

} // --- Fin de clase ExportPreflightService ---
