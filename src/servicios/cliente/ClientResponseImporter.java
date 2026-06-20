package servicios.cliente;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import modelo.proyecto.ProjectModel;
import modelo.proyecto.ProjectModel.ClientSelection;
import modelo.proyecto.SelectionState;

/**
 * Importa el archivo JSON de respuesta enviado por el cliente y actualiza
 * el ClientSelection del proyecto activo.
 */
public class ClientResponseImporter {

    private static final Logger logger = LoggerFactory.getLogger(ClientResponseImporter.class);

    /**
     * Lee el archivo JSON de respuesta y actualiza el ClientSelection del proyecto.
     *
     * @param project       Proyecto activo donde se almacenará la selección del cliente.
     * @param respuestaJson Path al archivo JSON de respuesta devuelto por el cliente.
     * @return              Resumen de la importación (cuántas imágenes seleccionadas, descartadas, sin marcar).
     * @throws IOException  Si el archivo no puede ser leído o tiene formato inválido.
     */
    public ImportReport importar(ProjectModel project, Path respuestaJson) throws IOException {
        logger.info("[ClientResponseImporter] Importando respuesta: {}", respuestaJson);

        if (!Files.exists(respuestaJson)) {
            throw new IOException("El archivo de respuesta no existe: " + respuestaJson.toAbsolutePath());
        }

        String contenido = Files.readString(respuestaJson, StandardCharsets.UTF_8);
        JsonObject root;
        try {
            root = JsonParser.parseString(contenido).getAsJsonObject();
        } catch (Exception e) {
            throw new IOException("El archivo no es un JSON válido: " + e.getMessage(), e);
        }

        // Validar estructura básica
        if (!root.has("respuestas")) {
            throw new IOException("El JSON no tiene el campo 'respuestas'. ¿Es un archivo de respuesta válido?");
        }

        // Obtener o crear la ClientSelection del proyecto
        ClientSelection clientSelection = project.getClientSelection(); // Lazy init
        clientSelection.setIteracionNumero(
                root.has("iteracion") ? root.get("iteracion").getAsInt() : 1);
        clientSelection.setFechaRespuesta(
                root.has("fechaRespuesta") && !root.get("fechaRespuesta").isJsonNull()
                ? root.get("fechaRespuesta").getAsString()
                : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        JsonArray respuestas = root.getAsJsonArray("respuestas");
        int countSelected = 0, countDiscarded = 0, countUndefined = 0, countIgnored = 0;

        for (JsonElement elem : respuestas) {
            JsonObject resp = elem.getAsJsonObject();

            String id    = resp.has("id")         ? resp.get("id").getAsString()        : null;
            String estado = resp.has("estado")    ? resp.get("estado").getAsString()     : "UNDEFINED";
            String comment = resp.has("comentario") ? resp.get("comentario").getAsString() : "";

            if (id == null) {
                logger.warn("[ClientResponseImporter] Entrada sin 'id'. Se ignora.");
                countIgnored++;
                continue;
            }

            // Buscar la ruta real del proyecto usando el id (nombre+tamaño)
            String rutaKey = buscarRutaPorId(project, id);
            if (rutaKey == null) {
                logger.warn("[ClientResponseImporter] No se encontró imagen con id '{}'. Se ignora.", id);
                countIgnored++;
                continue;
            }

            SelectionState state;
            try {
                state = SelectionState.valueOf(estado);
            } catch (IllegalArgumentException e) {
                logger.warn("[ClientResponseImporter] Estado desconocido '{}' para id '{}'. Se asigna UNDEFINED.", estado, id);
                state = SelectionState.UNDEFINED;
            }

            clientSelection.getImages().put(rutaKey, state);
            if (comment != null && !comment.trim().isEmpty()) {
                clientSelection.getComments().put(rutaKey, comment.trim());
            }

            switch (state) {
                case SELECTED   -> countSelected++;
                case DISCARDED  -> countDiscarded++;
                default         -> countUndefined++;
            }
        }

        logger.info("[ClientResponseImporter] Importación completada: {} seleccionadas, {} descartadas, {} sin marcar, {} ignoradas.",
                countSelected, countDiscarded, countUndefined, countIgnored);

        return new ImportReport(countSelected, countDiscarded, countUndefined, countIgnored);
    } // --- FIN de metodo importar ---


    /**
     * Busca la clave de ruta en el proyecto que corresponde al id dado.
     * El id se construyó como "nombre_tamaño", así que buscamos por nombre de archivo
     * y por coincidencia de tamaño.
     */
    private String buscarRutaPorId(ProjectModel project, String id) {
        for (String rutaKey : project.getSelectedImages().keySet()) {
            try {
                Path ruta = Path.of(rutaKey);
                String nombre = ruta.getFileName() != null ? ruta.getFileName().toString() : "";
                long size = Files.exists(ruta) ? Files.size(ruta) : 0;
                String candidateId = sanitizarNombre(nombre) + "_" + size;
                if (candidateId.equals(id)) {
                    return rutaKey;
                }
            } catch (Exception e) {
                logger.warn("[ClientResponseImporter] Error calculando id para {}: {}", rutaKey, e.getMessage());
            }
        }
        return null;
    } // --- FIN de metodo buscarRutaPorId ---


    /**
     * Limpia y normaliza un nombre para usarlo como identificador en el JSON.
     */
    private String sanitizarNombre(String nombre) {
        if (nombre == null) return "";
        return nombre.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    } // --- FIN de metodo sanitizarNombre ---


    // -----------------------------------------------------------------------
    // Clase de resultado
    // -----------------------------------------------------------------------


    /**
     * Resumen de una operación de importación de respuesta del cliente.
     */
    public record ImportReport(int selected, int discarded, int undefined, int ignored) {

        /** Total de imágenes procesadas (excluye ignoradas). */
        public int total() { return selected + discarded + undefined; } // --- Fin del metodo total ---

        /** Indica si hay imágenes sin marcar. */
        public boolean tieneUndefined() { return undefined > 0; } // --- Fin del metodo tieneUndefined ---

        /** Resumen legible de la importación. */
        public String resumen() {
            return String.format(
                "Importación completada: %d seleccionadas, %d descartadas, %d sin marcar, %d ignoradas.",
                selected, discarded, undefined, ignored);
        } // --- FIN de metodo resumen ---

    } // --- FIN de record ImportReport ---


} // --- FIN de clase ClientResponseImporter ---
