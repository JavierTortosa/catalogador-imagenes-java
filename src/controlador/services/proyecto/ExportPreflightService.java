package controlador.services.proyecto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.proyecto.ProjectImage;
import modelo.proyecto.ProjectModel;

/**
 * Servicio de validación previa a la exportación al cliente.
 * Comprueba que todas las imágenes seleccionadas tengan código de catálogo asignado.
 */
public class ExportPreflightService {

    private static final Logger logger = LoggerFactory.getLogger(ExportPreflightService.class);

    public static List<String> validarAsignaciones(Map<String, ProjectImage> masterImages) {
        List<String> errores = new ArrayList<>();
        if (masterImages == null || masterImages.isEmpty()) {
            errores.add("No hay imágenes seleccionadas en el proyecto.");
            return errores;
        }

        boolean foundSelected = false;
        for (ProjectImage pi : masterImages.values()) {
            if (!pi.isEnSeleccionProyecto()) continue;
            foundSelected = true;
            if (pi.getCodigoCatalogo() == null || pi.getCodigoCatalogo().isBlank()) {
                errores.add("La imagen '" + pi.getRutaImagen() + "' no tiene código de catálogo asignado.");
            }
        }

        if (!foundSelected) {
            errores.add("No hay imágenes seleccionadas en el proyecto.");
        }

        return errores;
    } // --- Fin de metodo validarAsignaciones ---

} // --- Fin de clase ExportPreflightService ---
