package servicios.cliente;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import modelo.proyecto.ImageCheckboxOverlay;
import modelo.proyecto.Mensaje;
import modelo.proyecto.ProjectImage;
import modelo.proyecto.ProjectModel;
import modelo.proyecto.SelectionState;

/**
 * Importa el archivo JSON de respuesta enviado por el cliente y actualiza
 * los campos estadoCliente, estadoClienteOriginal, comment y checkboxes
 * de cada ProjectImage en masterImages. Busca por c&oacute;digo de cat&aacute;logo.
 * Nunca modifica enSeleccionProyecto.
 */
public class ClientResponseImporter {

    private static final Logger logger = LoggerFactory.getLogger(ClientResponseImporter.class);

    /**
     * Lee el archivo JSON de respuesta y actualiza masterImages del proyecto.
     */
    public ImportReport importar(ProjectModel project, Path respuestaJson) throws IOException {
        logger.info("[ClientResponseImporter] Importando respuesta: {}", respuestaJson);
        if (!Files.exists(respuestaJson)) {
            throw new IOException("El archivo de respuesta no existe: " + respuestaJson.toAbsolutePath());
        }
        String contenido = Files.readString(respuestaJson, StandardCharsets.UTF_8);
        return importarDesdeString(project, contenido);
    } // --- FIN de metodo importar ---


    /**
     * Importa la respuesta del cliente desde un String JSON.
     * Busca cada respuesta por c&oacute;digo de cat&aacute;logo en masterImages.
     */
    public ImportReport importarDesdeString(ProjectModel project, String jsonContent) throws IOException {
        logger.info("[ClientResponseImporter] Importando respuesta desde string...");
        JsonObject root;
        try {
            root = JsonParser.parseString(jsonContent).getAsJsonObject();
        } catch (Exception e) {
            throw new IOException("El archivo no es un JSON v\u00e1lido: " + e.getMessage(), e);
        }

        if (!root.has("respuestas")) {
            throw new IOException("El JSON no tiene el campo 'respuestas'.");
        }

        var master = project.getMasterImages();
        if (master == null || master.isEmpty()) {
            throw new IOException("El proyecto no tiene im\u00e1genes en la lista maestra.");
        }

        // Almacenar metadatos de la respuesta
        if (root.has("fechaRespuesta") && !root.get("fechaRespuesta").isJsonNull()) {
            project.getClientSelection().setFechaRespuesta(root.get("fechaRespuesta").getAsString());
        } else {
            project.getClientSelection().setFechaRespuesta(
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (root.has("iteracion")) {
            project.setSharedIteration(root.get("iteracion").getAsInt());
        }

        JsonArray respuestas = root.getAsJsonArray("respuestas");
        int countSelected = 0, countDiscarded = 0, countUndefined = 0, countIgnored = 0;

        for (JsonElement elem : respuestas) {
            JsonObject resp = elem.getAsJsonObject();

            String codigo = resp.has("codigo") ? resp.get("codigo").getAsString() : null;
            String estado = resp.has("estado") ? resp.get("estado").getAsString() : "DISCARDED";
            JsonElement commentEl = resp.has("comentario") ? resp.get("comentario") : null;

            if (codigo == null || codigo.isEmpty()) {
                countIgnored++;
                continue;
            }

            // Buscar por c�digo de cat�logo en masterImages
            ProjectImage pi = null;
            for (ProjectImage p : master.values()) {
                if (codigo.equals(p.getCodigoCatalogo())) {
                    pi = p;
                    break;
                }
            }
            if (pi == null) {
                logger.warn("[ClientResponseImporter] No se encontr\u00f3 imagen con c\u00f3digo '{}'. Ignorada.", codigo);
                countIgnored++;
                continue;
            }

            // Preservar estado original antes de sobreescribir
            pi.setEstadoClienteOriginal(pi.getEstadoCliente());

            SelectionState state;
            try {
                state = SelectionState.valueOf(estado);
            } catch (IllegalArgumentException e) {
                logger.warn("[ClientResponseImporter] Estado desconocido '{}' para c\u00f3digo '{}'. Se asigna DISCARDED.", estado, codigo);
                state = SelectionState.DISCARDED;
            }
            pi.setEstadoCliente(state);

            if (commentEl != null && !commentEl.isJsonNull()) {
                if (commentEl.isJsonObject()) {
                    JsonObject commentObj = commentEl.getAsJsonObject();
                    if (commentObj.has("hilo")) {
                        JsonArray hilo = commentObj.getAsJsonArray("hilo");
                        List<Mensaje> thread = new ArrayList<>();
                        for (var me : hilo) {
                            JsonObject mObj = me.getAsJsonObject();
                            String de = mObj.has("de") ? mObj.get("de").getAsString() : "cliente";
                            String txt = mObj.has("texto") ? mObj.get("texto").getAsString() : "";
                            thread.add(new Mensaje(de, txt));
                        }
                        pi.setCommentThread(thread);
                        if (!thread.isEmpty()) {
                            pi.setComment(thread.get(thread.size() - 1).texto());
                        }
                    }
                } else if (commentEl.isJsonPrimitive()) {
                    String comment = commentEl.getAsString();
                    if (comment != null && !comment.trim().isEmpty()) {
                        pi.setComment(comment.trim());
                    }
                }
            }

            // Procesar checkboxes internos
            if (resp.has("checkboxes")) {
                JsonArray checkboxesArr = resp.getAsJsonArray("checkboxes");
                List<ImageCheckboxOverlay> overlays = pi.getCheckboxes();
                for (int ci = 0; ci < checkboxesArr.size(); ci++) {
                    JsonObject cbResp = checkboxesArr.get(ci).getAsJsonObject();
                    String cbCodigo = cbResp.has("codigo") ? cbResp.get("codigo").getAsString() : "";
                    String cbEstado = cbResp.has("estado") ? cbResp.get("estado").getAsString() : "UNDEFINED";
                    JsonElement cbCommentEl = cbResp.has("comentario") ? cbResp.get("comentario") : null;

                    SelectionState cbState;
                    try {
                        cbState = SelectionState.valueOf(cbEstado);
                    } catch (IllegalArgumentException e) {
                        logger.warn("[ClientResponseImporter] Estado desconocido '{}' para checkbox '{}'. Se asigna UNDEFINED.", cbEstado, cbCodigo);
                        cbState = SelectionState.UNDEFINED;
                    }

                    // Buscar overlay que coincida por c�digo de checkbox
                    ImageCheckboxOverlay target = null;
                    if (!cbCodigo.isEmpty()) {
                        for (ImageCheckboxOverlay ov : overlays) {
                            if (cbCodigo.equals(ov.getCheckboxCode())) {
                                target = ov;
                                break;
                            }
                        }
                    }
                    if (target != null) {
                        target.setState(cbState);
                        if (cbCommentEl != null && !cbCommentEl.isJsonNull()) {
                            if (cbCommentEl.isJsonObject()) {
                                JsonObject cbObj = cbCommentEl.getAsJsonObject();
                                if (cbObj.has("hilo")) {
                                    JsonArray hilo = cbObj.getAsJsonArray("hilo");
                                    List<Mensaje> thread = new ArrayList<>();
                                    for (var me : hilo) {
                                        JsonObject mObj = me.getAsJsonObject();
                                        String de = mObj.has("de") ? mObj.get("de").getAsString() : "cliente";
                                        String txt = mObj.has("texto") ? mObj.get("texto").getAsString() : "";
                                        thread.add(new Mensaje(de, txt));
                                    }
                                    target.setCommentThread(thread);
                                    if (!thread.isEmpty()) {
                                        target.setComment(thread.get(thread.size() - 1).texto());
                                    }
                                }
                            } else if (cbCommentEl.isJsonPrimitive()) {
                                String cbComment = cbCommentEl.getAsString();
                                if (cbComment != null && !cbComment.trim().isEmpty()) {
                                    target.setComment(cbComment.trim());
                                }
                            }
                        }
                    } else {
                        logger.warn("[ClientResponseImporter] No se encontr\u00f3 checkbox '{}' en imagen con c\u00f3digo '{}'.", cbCodigo, codigo);
                    }
                }
            }

            switch (state) {
                case SELECTED   -> countSelected++;
                case DISCARDED  -> countDiscarded++;
                default         -> countUndefined++;
            }
        }

        logger.info("[ClientResponseImporter] Importaci\u00f3n completada: {} seleccionadas, {} descartadas, {} sin marcar, {} ignoradas.",
                countSelected, countDiscarded, countUndefined, countIgnored);

        return new ImportReport(countSelected, countDiscarded, countUndefined, countIgnored);
    } // --- FIN de metodo importarDesdeString ---


    // -----------------------------------------------------------------------
    // Clase de resultado
    // -----------------------------------------------------------------------


    /**
     * Resumen de una operaci&oacute;n de importaci&oacute;n de respuesta del cliente.
     */
    public record ImportReport(int selected, int discarded, int undefined, int ignored) {

        /** Total de im&aacute;genes procesadas (excluye ignoradas). */
        public int total() { return selected + discarded + undefined; } // --- Fin del metodo total ---

        /** Indica si hay im&aacute;genes sin marcar. */
        public boolean tieneUndefined() { return undefined > 0; } // --- Fin del metodo tieneUndefined ---

        /** Resumen legible de la importaci&oacute;n. */
        public String resumen() {
            return String.format(
                "Importaci\u00f3n completada: %d seleccionadas, %d descartadas, %d sin marcar, %d ignoradas.",
                selected, discarded, undefined, ignored);
        } // --- FIN de metodo resumen ---

    } // --- FIN de record ImportReport ---


} // --- FIN de clase ClientResponseImporter ---
