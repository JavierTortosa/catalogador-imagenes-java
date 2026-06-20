package servicios.cliente;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.proyecto.ProjectModel;
import modelo.proyecto.SelectionState;

/**
 * Servicio encargado de generar el catálogo web para compartir con el cliente.
 * Produce: carpeta de miniaturas, data.json y el index.html desde un template.
 */
public class WebCatalogExporter {

    private static final Logger logger = LoggerFactory.getLogger(WebCatalogExporter.class);

    /** Tamaño máximo (en px) de las miniaturas generadas para el cliente. */
    private static final int THUMBNAIL_SIZE = 400;

    /**
     * Genera el catálogo web completo en la carpeta de destino indicada.
     *
     * @param project     Modelo del proyecto con las imágenes seleccionadas.
     * @param outputDir   Carpeta de destino donde se generará el catálogo.
     * @param iteracion   Número de iteración actual (1, 2, 3...).
     * @throws IOException Si ocurre algún error de E/S durante la exportación.
     */
    public void exportar(ProjectModel project, Path outputDir, int iteracion) throws IOException {
        logger.info("[WebCatalogExporter] Iniciando exportación a: {}", outputDir);

        Files.createDirectories(outputDir);

        // 1. Crear carpeta de miniaturas
        Path thumbsDir = outputDir.resolve("thumbs");
        Files.createDirectories(thumbsDir);

        // 2. Generar data.json con las imágenes seleccionadas
        String dataJson = generarDataJson(project, thumbsDir, iteracion);
        Path dataJsonPath = outputDir.resolve("data.json");
        Files.writeString(dataJsonPath, dataJson, StandardCharsets.UTF_8);
        logger.info("[WebCatalogExporter] data.json generado ({} bytes)", dataJson.length());

        // 3. Generar respuesta_vacia.json (estructura vacía para que el cliente la rellene)
        String projectSafeName = sanitizarNombre(project.getProjectName());
        String respuestaVacia = generarRespuestaVacia(project, projectSafeName, iteracion);
        Path respuestaPath = outputDir.resolve(projectSafeName + "_iteracion" + iteracion + "_respuesta.json");
        Files.writeString(respuestaPath, respuestaVacia, StandardCharsets.UTF_8);
        logger.info("[WebCatalogExporter] Respuesta vacía generada: {}", respuestaPath.getFileName());

        // 4. Copiar / generar el index.html desde el template embebido
        String htmlContent = generarHtml(project, projectSafeName, iteracion);
        Path htmlPath = outputDir.resolve("index.html");
        Files.writeString(htmlPath, htmlContent, StandardCharsets.UTF_8);
        logger.info("[WebCatalogExporter] index.html generado.");

        logger.info("[WebCatalogExporter] Exportación completada en: {}", outputDir.toAbsolutePath());
    } // --- FIN de metodo exportar ---


    /**
     * Genera el contenido del archivo data.json con las imágenes seleccionadas y sus miniaturas.
     */
    private String generarDataJson(ProjectModel project, Path thumbsDir, int iteracion) throws IOException {
        Map<String, String> selectedImages = project.getSelectedImages();
        String projectName = project.getProjectName() != null ? project.getProjectName() : "Proyecto";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"projectName\": ").append(jsonString(projectName)).append(",\n");
        sb.append("  \"iteracion\": ").append(iteracion).append(",\n");
        sb.append("  \"exportDate\": ").append(jsonString(timestamp)).append(",\n");
        sb.append("  \"imagenes\": [\n");

        int i = 0;
        for (Map.Entry<String, String> entry : selectedImages.entrySet()) {
            String rutaStr = entry.getKey();
            String etiqueta = entry.getValue() != null ? entry.getValue() : "";
            Path rutaImagen = Path.of(rutaStr);

            // Generar miniatura
            String thumbFilename = generarMiniatura(rutaImagen, thumbsDir);

            // ID único basado en nombre+peso (como acordamos)
            String imageId = generarId(rutaImagen);

            if (i > 0) sb.append(",\n");
            sb.append("    {\n");
            sb.append("      \"id\": ").append(jsonString(imageId)).append(",\n");
            sb.append("      \"nombre\": ").append(jsonString(rutaImagen.getFileName() != null
                    ? rutaImagen.getFileName().toString() : rutaStr)).append(",\n");
            sb.append("      \"etiqueta\": ").append(jsonString(etiqueta)).append(",\n");
            sb.append("      \"miniatura\": ").append(jsonString("thumbs/" + thumbFilename)).append(",\n");
            sb.append("      \"estado\": \"UNDEFINED\",\n");
            sb.append("      \"comentario\": \"\"\n");
            sb.append("    }");
            i++;
        }

        sb.append("\n  ]\n}");
        return sb.toString();
    } // --- FIN de metodo generarDataJson ---


    /**
     * Genera el contenido del archivo de respuesta vacío para que el cliente lo rellene.
     */
    private String generarRespuestaVacia(ProjectModel project, String projectSafeName, int iteracion) {
        Map<String, String> selectedImages = project.getSelectedImages();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"projectName\": ").append(jsonString(project.getProjectName())).append(",\n");
        sb.append("  \"iteracion\": ").append(iteracion).append(",\n");
        sb.append("  \"fechaEnvio\": ").append(jsonString(timestamp)).append(",\n");
        sb.append("  \"fechaRespuesta\": null,\n");
        sb.append("  \"respuestas\": [\n");

        int i = 0;
        for (String rutaStr : selectedImages.keySet()) {
            Path rutaImagen = Path.of(rutaStr);
            String imageId = generarId(rutaImagen);
            if (i > 0) sb.append(",\n");
            sb.append("    {\n");
            sb.append("      \"id\": ").append(jsonString(imageId)).append(",\n");
            sb.append("      \"estado\": \"UNDEFINED\",\n");
            sb.append("      \"comentario\": \"\"\n");
            sb.append("    }");
            i++;
        }

        sb.append("\n  ]\n}");
        return sb.toString();
    } // --- FIN de metodo generarRespuestaVacia ---


    /**
     * Genera una miniatura de la imagen en thumbsDir.
     */
    private String generarMiniatura(Path original, Path thumbsDir) throws IOException {
        String ext = obtenerExtension(original);
        String safeName = sanitizarNombre(
                original.getFileName() != null ? original.getFileName().toString() : "imagen") + "_thumb." + ext;
        Path thumbPath = thumbsDir.resolve(safeName);

        if (Files.exists(thumbPath)) {
            return safeName; // Ya existe, reutilizar
        }

        if (!Files.exists(original)) {
            logger.warn("[WebCatalogExporter] Imagen no encontrada: {}. Se omitirá la miniatura.", original);
            return safeName; // devolvemos el nombre aunque el archivo no exista
        }

        try {
            BufferedImage srcImg = ImageIO.read(original.toFile());
            if (srcImg == null) {
                // No es imagen reconocible por ImageIO (ej: webp sin plugin), copiar tal cual
                Files.copy(original, thumbPath, StandardCopyOption.REPLACE_EXISTING);
                return safeName;
            }

            int srcW = srcImg.getWidth();
            int srcH = srcImg.getHeight();
            double scale = Math.min((double) THUMBNAIL_SIZE / srcW, (double) THUMBNAIL_SIZE / srcH);
            int newW = Math.max(1, (int) (srcW * scale));
            int newH = Math.max(1, (int) (srcH * scale));

            BufferedImage thumb = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumb.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(srcImg, 0, 0, newW, newH, null);
            g.dispose();

            String formatName = ext.equalsIgnoreCase("jpg") ? "JPEG" : ext.toUpperCase();
            ImageIO.write(thumb, formatName, thumbPath.toFile());

        } catch (Exception e) {
            logger.warn("[WebCatalogExporter] Error generando miniatura para {}: {}", original.getFileName(), e.getMessage());
            // Como fallback, intentamos copiar el original
            try {
                Files.copy(original, thumbPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {}
        }

        return safeName;
    } // --- FIN de metodo generarMiniatura ---


    /**
     * Genera el HTML del catálogo para el cliente.
     */
    private String generarHtml(ProjectModel project, String projectSafeName, int iteracion) {
        String projectName = project.getProjectName() != null ? project.getProjectName() : "Catálogo";
        String respuestaFilename = projectSafeName + "_iteracion" + iteracion + "_respuesta.json";

        // Template HTML embebido directamente en el código
        return "<!DOCTYPE html>\n"
            + "<html lang=\"es\">\n"
            + "<head>\n"
            + "  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + escapeHtml(projectName) + " – Catálogo de Selección</title>\n"
            + "  <style>\n" + getCss() + "  </style>\n"
            + "</head>\n"
            + "<body>\n"
            + "  <header>\n"
            + "    <h1>📷 " + escapeHtml(projectName) + "</h1>\n"
            + "    <p class=\"subtitle\">Iteración #" + iteracion + " · Marca las figuras que te interesan</p>\n"
            + "  </header>\n"
            + "\n"
            + "  <div id=\"filters\">\n"
            + "    <button class=\"filter-btn active\" data-filter=\"all\">Todas</button>\n"
            + "    <button class=\"filter-btn\" data-filter=\"SELECTED\">✓ Seleccionadas</button>\n"
            + "    <button class=\"filter-btn\" data-filter=\"DISCARDED\">✗ Descartadas</button>\n"
            + "    <button class=\"filter-btn\" data-filter=\"UNDEFINED\">? Sin marcar</button>\n"
            + "    <span id=\"counter\"></span>\n"
            + "  </div>\n"
            + "\n"
            + "  <div id=\"gallery\" class=\"gallery\"></div>\n"
            + "\n"
            + "  <footer>\n"
            + "    <div id=\"summary\"></div>\n"
            + "    <button id=\"btnGenerar\" class=\"btn-primary\">📥 Descargar Respuesta</button>\n"
            + "  </footer>\n"
            + "\n"
            + "  <div id=\"modal\" class=\"modal hidden\">\n"
            + "    <div class=\"modal-content\">\n"
            + "      <button id=\"modalClose\" class=\"modal-close\">✕</button>\n"
            + "      <img id=\"modalImg\" src=\"\" alt=\"\">\n"
            + "      <div class=\"modal-info\">\n"
            + "        <h3 id=\"modalNombre\"></h3>\n"
            + "        <p id=\"modalEtiqueta\" class=\"etiqueta\"></p>\n"
            + "        <div class=\"modal-actions\">\n"
            + "          <button class=\"btn-estado\" data-estado=\"SELECTED\">✓ Seleccionar</button>\n"
            + "          <button class=\"btn-estado\" data-estado=\"DISCARDED\">✗ Descartar</button>\n"
            + "          <button class=\"btn-estado\" data-estado=\"UNDEFINED\">? Sin decidir</button>\n"
            + "        </div>\n"
            + "        <label>Comentario:</label>\n"
            + "        <textarea id=\"modalComentario\" rows=\"3\" placeholder=\"Escribe tu comentario aquí...\"></textarea>\n"
            + "      </div>\n"
            + "    </div>\n"
            + "  </div>\n"
            + "\n"
            + "  <script>\n" + getJs(respuestaFilename) + "  </script>\n"
            + "</body>\n"
            + "</html>\n";
    } // --- FIN de metodo generarHtml ---


    /**
     * Genera el CSS embebido en el HTML del catálogo.
     */
    private String getCss() {
        return """
            *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
            :root {
              --bg: #1a1a2e; --surface: #16213e; --accent: #0f3460;
              --selected: #4ecca3; --discarded: #e94560; --undefined: #888;
              --text: #eee; --text-muted: #aaa; --radius: 12px;
            }
            body { background: var(--bg); color: var(--text); font-family: 'Segoe UI', system-ui, sans-serif; min-height: 100vh; }
            header { text-align: center; padding: 2rem 1rem 1rem; }
            header h1 { font-size: 2rem; font-weight: 700; }
            header .subtitle { color: var(--text-muted); margin-top: .4rem; }
            #filters { display: flex; gap: .5rem; flex-wrap: wrap; justify-content: center; padding: 1rem; align-items: center; }
            .filter-btn { background: var(--surface); border: 1px solid #333; color: var(--text); padding: .4rem 1rem; border-radius: 20px; cursor: pointer; transition: all .2s; }
            .filter-btn:hover { border-color: var(--selected); }
            .filter-btn.active { background: var(--accent); border-color: var(--selected); }
            #counter { margin-left: auto; color: var(--text-muted); font-size: .9rem; }
            .gallery { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 1rem; padding: 1rem 2rem; }
            .card { background: var(--surface); border-radius: var(--radius); overflow: hidden; cursor: pointer; border: 2px solid transparent; transition: transform .2s, border-color .2s, box-shadow .2s; }
            .card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,.4); }
            .card.SELECTED { border-color: var(--selected); }
            .card.DISCARDED { border-color: var(--discarded); opacity: .75; }
            .card.UNDEFINED { border-color: #444; }
            .card img { width: 100%; height: 180px; object-fit: cover; display: block; }
            .card-body { padding: .6rem .8rem; }
            .card-body h4 { font-size: .85rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
            .card-body .badge { display: inline-block; margin-top: .3rem; font-size: .75rem; padding: .1rem .5rem; border-radius: 10px; font-weight: 600; }
            .badge.SELECTED { background: rgba(78,204,163,.2); color: var(--selected); }
            .badge.DISCARDED { background: rgba(233,69,96,.2); color: var(--discarded); }
            .badge.UNDEFINED { background: rgba(136,136,136,.15); color: var(--undefined); }
            .has-comment::after { content: ' 💬'; }
            footer { position: sticky; bottom: 0; background: var(--surface); border-top: 1px solid #333; padding: 1rem 2rem; display: flex; align-items: center; justify-content: space-between; z-index: 10; }
            #summary { font-size: .9rem; color: var(--text-muted); }
            .btn-primary { background: var(--selected); color: #000; border: none; padding: .6rem 1.4rem; border-radius: 8px; font-weight: 700; cursor: pointer; transition: opacity .2s; }
            .btn-primary:hover { opacity: .85; }
            .modal { position: fixed; inset: 0; background: rgba(0,0,0,.8); display: flex; align-items: center; justify-content: center; z-index: 100; }
            .modal.hidden { display: none; }
            .modal-content { background: var(--surface); border-radius: var(--radius); max-width: 700px; width: 95%; max-height: 90vh; overflow-y: auto; padding: 1.5rem; position: relative; }
            .modal-close { position: absolute; top: .8rem; right: .8rem; background: none; border: none; color: var(--text); font-size: 1.2rem; cursor: pointer; }
            .modal-content img { width: 100%; border-radius: 8px; margin-bottom: 1rem; max-height: 45vh; object-fit: contain; background: #0a0a1a; }
            .modal-info h3 { margin-bottom: .3rem; }
            .etiqueta { color: var(--text-muted); font-size: .85rem; margin-bottom: 1rem; }
            .modal-actions { display: flex; gap: .5rem; flex-wrap: wrap; margin-bottom: 1rem; }
            .btn-estado { border: 2px solid #444; background: transparent; color: var(--text); padding: .4rem .9rem; border-radius: 8px; cursor: pointer; transition: all .2s; font-weight: 600; }
            .btn-estado:hover, .btn-estado.active { border-color: var(--selected); background: rgba(78,204,163,.15); }
            .btn-estado[data-estado="DISCARDED"]:hover, .btn-estado[data-estado="DISCARDED"].active { border-color: var(--discarded); background: rgba(233,69,96,.15); }
            .btn-estado[data-estado="UNDEFINED"]:hover, .btn-estado[data-estado="UNDEFINED"].active { border-color: var(--undefined); background: rgba(136,136,136,.1); }
            label { display: block; margin-bottom: .3rem; font-size: .85rem; color: var(--text-muted); }
            textarea { width: 100%; background: var(--bg); border: 1px solid #444; border-radius: 6px; color: var(--text); padding: .5rem; font-size: .9rem; resize: vertical; }
            """;
    } // --- FIN de metodo getCss ---


    /**
     * Genera el JavaScript embebido para la interacción del catálogo.
     */
    private String getJs(String respuestaFilename) {
        return """
            let data = { imagenes: [] };
            let currentFilter = 'all';
            let currentIndex = -1;

            async function init() {
              try {
                const resp = await fetch('data.json');
                data = await resp.json();
                // Intentar cargar respuesta previa si existe
                try {
                  const prevResp = await fetch('%s');
                  const prevData = await prevResp.json();
                  if (prevData.respuestas) {
                    prevData.respuestas.forEach(r => {
                      const img = data.imagenes.find(i => i.id === r.id);
                      if (img) { img.estado = r.estado; img.comentario = r.comentario || ''; }
                    });
                  }
                } catch(e) { /* Sin respuesta previa */ }
                renderGallery();
              } catch(e) {
                document.getElementById('gallery').innerHTML = '<p style="color:red;padding:2rem">Error al cargar data.json: ' + e + '</p>';
              }
            }

            function renderGallery() {
              const gallery = document.getElementById('gallery');
              const filtered = data.imagenes.filter(img =>
                currentFilter === 'all' || img.estado === currentFilter);
              gallery.innerHTML = filtered.map((img, i) => {
                const idx = data.imagenes.indexOf(img);
                const hasComment = img.comentario && img.comentario.trim();
                return `<div class="card ${img.estado}${hasComment ? ' has-comment' : ''}" onclick="openModal(${idx})">
                  <img src="${img.miniatura}" alt="${escHtml(img.nombre)}" loading="lazy"
                       onerror="this.src='data:image/svg+xml,<svg xmlns=\\'http://www.w3.org/2000/svg\\' width=\\'200\\' height=\\'180\\'><rect width=\\'200\\' height=\\'180\\' fill=\\'%23222\\'/><text x=\\'50%%\\' y=\\'50%%\\' fill=\\'%23666\\' text-anchor=\\'middle\\' dy=\\'.3em\\'>Sin imagen</text></svg>'">
                  <div class="card-body">
                    <h4>${escHtml(img.nombre)}</h4>
                    <span class="badge ${img.estado}">${estadoLabel(img.estado)}</span>
                  </div>
                </div>`;
              }).join('');
              updateCounter();
              updateSummary();
            }

            function estadoLabel(e) {
              return e === 'SELECTED' ? '✓ Seleccionada' : e === 'DISCARDED' ? '✗ Descartada' : '? Sin marcar';
            }
            function escHtml(s) { return (s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }

            function updateCounter() {
              const sel = data.imagenes.filter(i => i.estado === 'SELECTED').length;
              const dis = data.imagenes.filter(i => i.estado === 'DISCARDED').length;
              const und = data.imagenes.filter(i => i.estado === 'UNDEFINED').length;
              document.getElementById('counter').textContent = `Total: ${data.imagenes.length} · ✓ ${sel} · ✗ ${dis} · ? ${und}`;
            }

            function updateSummary() {
              const sel = data.imagenes.filter(i => i.estado === 'SELECTED').length;
              const dis = data.imagenes.filter(i => i.estado === 'DISCARDED').length;
              const und = data.imagenes.filter(i => i.estado === 'UNDEFINED').length;
              document.getElementById('summary').textContent =
                `✓ ${sel} seleccionadas · ✗ ${dis} descartadas · ? ${und} sin marcar`;
            }

            // Filtros
            document.querySelectorAll('.filter-btn').forEach(btn => {
              btn.addEventListener('click', () => {
                currentFilter = btn.dataset.filter;
                document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                renderGallery();
              });
            });

            // Modal
            function openModal(index) {
              currentIndex = index;
              const img = data.imagenes[index];
              document.getElementById('modalImg').src = img.miniatura;
              document.getElementById('modalNombre').textContent = img.nombre;
              document.getElementById('modalEtiqueta').textContent = img.etiqueta || '';
              document.getElementById('modalComentario').value = img.comentario || '';
              document.querySelectorAll('.btn-estado').forEach(b => {
                b.classList.toggle('active', b.dataset.estado === img.estado);
              });
              document.getElementById('modal').classList.remove('hidden');
            }

            document.getElementById('modalClose').addEventListener('click', () => {
              guardarModal();
              document.getElementById('modal').classList.add('hidden');
            });

            document.getElementById('modal').addEventListener('click', e => {
              if (e.target === document.getElementById('modal')) {
                guardarModal();
                document.getElementById('modal').classList.add('hidden');
              }
            });

            document.querySelectorAll('.btn-estado').forEach(btn => {
              btn.addEventListener('click', () => {
                if (currentIndex < 0) return;
                data.imagenes[currentIndex].estado = btn.dataset.estado;
                document.querySelectorAll('.btn-estado').forEach(b =>
                  b.classList.toggle('active', b.dataset.estado === btn.dataset.estado));
              });
            });

            document.getElementById('modalComentario').addEventListener('input', e => {
              if (currentIndex >= 0) data.imagenes[currentIndex].comentario = e.target.value;
            });

            function guardarModal() {
              if (currentIndex >= 0) {
                data.imagenes[currentIndex].comentario =
                  document.getElementById('modalComentario').value;
              }
              renderGallery();
            }

            // Descargar respuesta
            document.getElementById('btnGenerar').addEventListener('click', () => {
              const und = data.imagenes.filter(i => i.estado === 'UNDEFINED').length;
              if (und > 0) {
                if (!confirm(`Atención: hay ${und} imagen(s) sin marcar. ¿Deseas descargar la respuesta igualmente?`)) return;
              }
              const respuesta = {
                projectName: data.projectName,
                iteracion: data.iteracion,
                fechaEnvio: data.exportDate,
                fechaRespuesta: new Date().toISOString(),
                respuestas: data.imagenes.map(img => ({
                  id: img.id,
                  estado: img.estado,
                  comentario: img.comentario || ''
                }))
              };
              const blob = new Blob([JSON.stringify(respuesta, null, 2)], {type: 'application/json'});
              const a = document.createElement('a');
              a.href = URL.createObjectURL(blob);
              a.download = '%s';
              a.click();
              URL.revokeObjectURL(a.href);
            });

            init();
            """.formatted(respuestaFilename, respuestaFilename);
    } // --- FIN de metodo getJs ---


    // -----------------------------------------------------------------------
    // Utilidades
    // -----------------------------------------------------------------------

    /**
     * Genera un identificador único para una imagen basado en su nombre y tamaño.
     */
    private String generarId(Path ruta) {
        String nombre = ruta.getFileName() != null ? ruta.getFileName().toString() : ruta.toString();
        long size = 0;
        try {
            if (Files.exists(ruta)) size = Files.size(ruta);
        } catch (IOException ignored) {}
        return sanitizarNombre(nombre) + "_" + size;
    } // --- FIN de metodo generarId ---


    /**
     * Sanitiza un nombre para usarlo como nombre de archivo (solo caracteres seguros).
     */
    private String sanitizarNombre(String nombre) {
        if (nombre == null) return "proyecto";
        return nombre.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    } // --- FIN de metodo sanitizarNombre ---


    /**
     * Obtiene la extensión de un archivo.
     */
    private String obtenerExtension(Path ruta) {
        String nombre = ruta.getFileName() != null ? ruta.getFileName().toString() : "";
        int dot = nombre.lastIndexOf('.');
        if (dot > 0) {
            String ext = nombre.substring(dot + 1).toLowerCase();
            return ext.isEmpty() ? "jpg" : ext;
        }
        return "jpg";
    } // --- FIN de metodo obtenerExtension ---


    /**
     * Convierte una cadena a una representación JSON segura.
     */
    private String jsonString(String valor) {
        if (valor == null) return "null";
        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"")
                           .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    } // --- FIN de metodo jsonString ---


    /**
     * Escapa caracteres HTML en una cadena.
     */
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    } // --- FIN de metodo escapeHtml ---


} // --- FIN de clase WebCatalogExporter ---
