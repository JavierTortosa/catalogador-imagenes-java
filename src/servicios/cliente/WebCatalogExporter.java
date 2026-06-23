package servicios.cliente;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import modelo.proyecto.CommentOverlay;
import modelo.proyecto.ImageCheckboxOverlay;
import modelo.proyecto.ProjectModel;
import modelo.proyecto.SelectionState;
import net.coobird.thumbnailator.Thumbnails;

/**
 * Exportador de catálogo web para el modo cliente.
 * Genera miniaturas (con Thumbnailator o fallback Graphics2D),
 * data.json, data.js, index.html (sin filtros, checkbox nativo)
 * y un archivo de respuesta vacío.
 */
public class WebCatalogExporter {

    private static final Logger logger = LoggerFactory.getLogger(WebCatalogExporter.class);

    private int thumbnailSize = 400;
    private double jpegQuality = 0.7;
    private boolean forceJpeg = true;

    public int getThumbnailSize() { return thumbnailSize; }
    public void setThumbnailSize(int thumbnailSize) { this.thumbnailSize = Math.max(100, thumbnailSize); }

    public double getJpegQuality() { return jpegQuality; }
    public void setJpegQuality(double jpegQuality) { this.jpegQuality = Math.max(0.1, Math.min(1.0, jpegQuality)); }

    public boolean isForceJpeg() { return forceJpeg; }
    public void setForceJpeg(boolean forceJpeg) { this.forceJpeg = forceJpeg; }

    public void exportar(ProjectModel project, Path outputDir, int iteracion,
                         java.util.function.Consumer<Integer> progressCallback) throws IOException {
        logger.info("[WebCatalogExporter] Iniciando exportación a: {}", outputDir);
        Files.createDirectories(outputDir);

        Path thumbsDir = outputDir.resolve("thumbs");
        Files.createDirectories(thumbsDir);

        Map<String, String> selectedImages = project.getSelectedImages();
        int total = selectedImages.size();
        int done = 0;

        for (String rutaStr : selectedImages.keySet()) {
            Path rutaImagen = Path.of(rutaStr);
            generarMiniatura(rutaImagen, thumbsDir);
            done++;
            if (progressCallback != null) {
                progressCallback.accept(done * 100 / Math.max(total, 1));
            }
        }

        String dataJson = generarDataJson(project, thumbsDir, iteracion);
        Path dataJsonPath = outputDir.resolve("data.json");
        Files.writeString(dataJsonPath, dataJson, StandardCharsets.UTF_8);
        logger.info("[WebCatalogExporter] data.json generado ({} bytes)", dataJson.length());

        String dataJsContent = "var CATALOG_DATA = " + dataJson + ";";
        Path dataJsPath = outputDir.resolve("data.js");
        Files.writeString(dataJsPath, dataJsContent, StandardCharsets.UTF_8);
        logger.info("[WebCatalogExporter] data.js generado.");

        if (progressCallback != null) progressCallback.accept(90);

        String projectSafeName = sanitizarNombre(project.getProjectName());
        String respuestaVacia = generarRespuestaVacia(project, projectSafeName, iteracion);
        Path respuestaPath = outputDir.resolve(projectSafeName + "_iteracion" + iteracion + "_respuesta.json");
        Files.writeString(respuestaPath, respuestaVacia, StandardCharsets.UTF_8);
        logger.info("[WebCatalogExporter] Respuesta vacía generada: {}", respuestaPath.getFileName());
        if (progressCallback != null) progressCallback.accept(95);

        String htmlContent = generarHtml(project, projectSafeName, iteracion);
        Path htmlPath = outputDir.resolve("index.html");
        Files.writeString(htmlPath, htmlContent, StandardCharsets.UTF_8);
        logger.info("[WebCatalogExporter] index.html generado.");
        if (progressCallback != null) progressCallback.accept(100);

        logger.info("[WebCatalogExporter] Exportación completada en: {}", outputDir.toAbsolutePath());
    } // --- Fin de metodo exportar ---


    public void exportar(ProjectModel project, Path outputDir, int iteracion) throws IOException {
        exportar(project, outputDir, iteracion, null);
    } // --- Fin de metodo exportar ---


    private String generarDataJson(ProjectModel project, Path thumbsDir, int iteracion) {
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
            Path rutaImagen = Path.of(rutaStr);
            String etiqueta = entry.getValue() != null ? entry.getValue() : "";

            String thumbFilename = nombreMiniatura(rutaImagen);
            String imageId = generarId(rutaImagen);
            String imageCode = project.getImageCodes().getOrDefault(rutaStr, "");

            List<ImageCheckboxOverlay> checkboxes = project.hasClientSelection()
                    ? project.getClientSelection().getImageCheckboxes(rutaStr)
                    : List.of();
            String comment = project.hasClientSelection()
                    ? project.getClientSelection().getComments().getOrDefault(rutaStr, "")
                    : "";

            if (i > 0) sb.append(",\n");
            sb.append("    {\n");
            sb.append("      \"id\": ").append(jsonString(imageId)).append(",\n");
            sb.append("      \"nombre\": ").append(jsonString(rutaImagen.getFileName() != null
                    ? rutaImagen.getFileName().toString() : rutaStr)).append(",\n");
            sb.append("      \"codigo\": ").append(jsonString(imageCode)).append(",\n");
            sb.append("      \"etiqueta\": ").append(jsonString(etiqueta)).append(",\n");
            sb.append("      \"miniatura\": ").append(jsonString("thumbs/" + thumbFilename)).append(",\n");
            sb.append("      \"estado\": \"UNDEFINED\",\n");
            sb.append("      \"comentario\": ").append(jsonString(comment)).append(",\n");
            // Comentario overlay con posicion
            var commentOverlay = project.hasClientSelection()
                    ? project.getClientSelection().getCommentOverlays().get(rutaStr)
                    : null;
            if (commentOverlay != null && commentOverlay.getText() != null && !commentOverlay.getText().isEmpty()) {
                sb.append("      \"commentOverlay\": {\n");
                sb.append("        \"texto\": ").append(jsonString(commentOverlay.getText())).append(",\n");
                sb.append("        \"x\": ").append(commentOverlay.getImageX()).append(",\n");
                sb.append("        \"y\": ").append(commentOverlay.getImageY()).append("\n");
                sb.append("      },\n");
            }
            sb.append("      \"checkboxes\": [\n");
            for (int j = 0; j < checkboxes.size(); j++) {
                ImageCheckboxOverlay cb = checkboxes.get(j);
                if (j > 0) sb.append(",\n");
                sb.append("        {\n");
                sb.append("          \"codigo\": ").append(jsonString(cb.getCheckboxCode())).append(",\n");
                sb.append("          \"x\": ").append(cb.getImageX()).append(",\n");
                sb.append("          \"y\": ").append(cb.getImageY()).append(",\n");
                sb.append("          \"tamano\": ").append(cb.getSize()).append(",\n");
                sb.append("          \"marcado\": ").append(cb.isChecked()).append(",\n");
                sb.append("          \"precio\": ").append(jsonString(cb.getPrice() > 0
                        ? String.format("%.2f", cb.getPrice()) : "")).append(",\n");
                sb.append("          \"comentario\": ").append(jsonString(cb.getComment() != null
                        ? cb.getComment() : "")).append("\n");
                sb.append("        }");
            }
            sb.append("\n      ]\n");
            sb.append("    }");
            i++;
        }

        sb.append("\n  ]\n}");
        String json = sb.toString();
        try {
            new Gson().fromJson(json, Object.class);
        } catch (JsonSyntaxException e) {
            logger.error("[WebCatalogExporter] data.json generado NO es JSON v\u00e1lido: {}", e.getMessage());
        }
        return json;
    } // --- Fin de metodo generarDataJson ---


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
            sb.append("      \"codigo\": ").append(jsonString(project.getImageCodes().getOrDefault(rutaStr, ""))).append(",\n");
            sb.append("      \"estado\": \"UNDEFINED\",\n");
            sb.append("      \"comentario\": \"\"\n");
            sb.append("    }");
            i++;
        }

        sb.append("\n  ]\n}");
        return sb.toString();
    } // --- Fin de metodo generarRespuestaVacia ---


    private String generarMiniatura(Path original, Path thumbsDir) throws IOException {
        String ext = obtenerExtension(original);
        boolean salidaJpeg = forceJpeg || "jpg".equals(ext) || "jpeg".equals(ext);
        String outExt = salidaJpeg ? "jpg" : ext;
        String safeName = sanitizarNombre(
                original.getFileName() != null ? original.getFileName().toString() : "imagen")
                + "_thumb." + outExt;
        Path thumbPath = thumbsDir.resolve(safeName);

        if (Files.exists(thumbPath)) {
            return safeName;
        }

        if (!Files.exists(original)) {
            logger.warn("[WebCatalogExporter] Imagen no encontrada: {}. Se omitirá la miniatura.", original);
            return safeName;
        }

        try {
            Thumbnails.of(original.toFile())
                    .size(thumbnailSize, thumbnailSize)
                    .outputFormat(salidaJpeg ? "jpg" : ext)
                    .outputQuality(jpegQuality)
                    .toFile(thumbPath.toFile());
        } catch (Exception e) {
            logger.warn("[WebCatalogExporter] Error con Thumbnailator para {}: {}. Usando fallback.",
                    original.getFileName(), e.getMessage());
            try {
                BufferedImage srcImg = ImageIO.read(original.toFile());
                if (srcImg == null) {
                    Files.copy(original, thumbPath, StandardCopyOption.REPLACE_EXISTING);
                    return safeName;
                }
                int srcW = srcImg.getWidth();
                int srcH = srcImg.getHeight();
                double scale = Math.min((double) thumbnailSize / srcW, (double) thumbnailSize / srcH);
                int newW = Math.max(1, (int) (srcW * scale));
                int newH = Math.max(1, (int) (srcH * scale));
                BufferedImage thumb = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = thumb.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.drawImage(srcImg, 0, 0, newW, newH, null);
                g.dispose();
                String formatName = salidaJpeg ? "JPEG" : ext.toUpperCase();
                ImageIO.write(thumb, formatName, thumbPath.toFile());
            } catch (Exception e2) {
                logger.warn("[WebCatalogExporter] Fallback también falló para {}: {}", original.getFileName(), e2.getMessage());
                try {
                    Files.copy(original, thumbPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {}
            }
        }

        return safeName;
    } // --- Fin de metodo generarMiniatura ---


    private String generarHtml(ProjectModel project, String projectSafeName, int iteracion) {
        String projectName = project.getProjectName() != null ? project.getProjectName() : "Cat\u00e1logo";
        String respuestaFilename = projectSafeName + "_iteracion" + iteracion + "_respuesta.json";
        String prjclFilename = projectSafeName + ".prjcl";

        return "<!DOCTYPE html>\n"
            + "<html lang=\"es\">\n"
            + "<head>\n"
            + "  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>" + escapeHtml(projectName) + " \u2013 Cat\u00e1logo</title>\n"
            + "  <style>\n" + getCss() + "  </style>\n"
            + "</head>\n"
            + "<body>\n"
            + "\n"
            + "  <header>\n"
            + "    <h1>" + escapeHtml(projectName) + "</h1>\n"
            + "    <p class=\"subtitle\">Iteraci\u00f3n #" + iteracion + " \u00b7 Marca los modelos que te interesan</p>\n"
            + "  </header>\n"
            + "\n"
            + "  <nav id=\"topbar\">\n"
            + "    <span id=\"counter\"></span>\n"
            + "  </nav>\n"
            + "\n"
            + "  <main id=\"gallery\" class=\"gallery\"></main>\n"
            + "\n"
            + "  <footer>\n"
            + "    <div id=\"summary\"></div>\n"
            + "    <div class=\"footer-buttons\">\n"
            + "      <button id=\"btnGenerar\" class=\"btn-primary\">\u2B07 Descargar Respuesta</button>\n"
            + "      <button id=\"btnDescargarPrjcl\" class=\"btn-secondary\">\u2B07 Descargar .prjcl</button>\n"
            + "    </div>\n"
            + "  </footer>\n"
            + "\n"
            + "  <div id=\"modal\" class=\"modal hidden\">\n"
            + "    <div class=\"modal-content\">\n"
            + "      <button id=\"modalClose\" class=\"modal-close\">&times;</button>\n"
            + "      <div class=\"modal-header\">\n"
            + "        <span class=\"modal-codigo\" id=\"modalCodigo\"></span>\n"
            + "        <h3 id=\"modalNombre\"></h3>\n"
            + "      </div>\n"
            + "      <img id=\"modalImg\" src=\"\" alt=\"\" class=\"modal-image\">\n"
            + "      <p id=\"modalEtiqueta\" class=\"etiqueta\"></p>\n"
            + "      <div id=\"modalCheckboxes\" class=\"modal-checkboxes\"></div>\n"
            + "      <div class=\"modal-checkbox-toggle\">\n"
            + "        <label><input type=\"checkbox\" id=\"modalCheckToggle\"> Marcar como seleccionado</label>\n"
            + "      </div>\n"
            + "      <label for=\"modalComentario\">Comentario:</label>\n"
            + "      <textarea id=\"modalComentario\" rows=\"3\" placeholder=\"Escribe aqu\u00ed tu comentario...\"></textarea>\n"
            + "    </div>\n"
            + "  </div>\n"
            + "\n"
            + "  <script src=\"data.js\"></script>\n"
            + "  <script>\n" + getJs(respuestaFilename, prjclFilename) + "  </script>\n"
            + "</body>\n"
            + "</html>\n";
    } // --- Fin de metodo generarHtml ---


    private String getCss() {
        return """
            *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
            :root {
              --bg: #0f0f1a;
              --surface: #1a1a2e;
              --card-bg: #222240;
              --accent: #3a3a6a;
              --selected: #2ecc71;
              --selected-bg: rgba(46,204,113,.12);
              --discarded: #e74c3c;
              --discarded-bg: rgba(231,76,60,.12);
              --text: #f0f0f0;
              --text-muted: #999;
              --radius: 10px;
            }
            body { background: var(--bg); color: var(--text); font-family: 'Segoe UI', system-ui, sans-serif; min-height: 100vh; }
            header { text-align: center; padding: 2rem 1rem .8rem; }
            header h1 { font-size: 1.8rem; font-weight: 700; letter-spacing: -.5px; }
            header .subtitle { color: var(--text-muted); margin-top: .3rem; font-size: .95rem; }
            #topbar { display: flex; justify-content: center; padding: .6rem 1rem; background: var(--surface); position: sticky; top: 0; z-index: 50; border-bottom: 1px solid #2a2a4a; }
            #counter { color: var(--text-muted); font-size: .85rem; }
            .gallery { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 1rem; padding: 1.2rem; max-width: 1400px; margin: 0 auto; }
            .card { background: var(--card-bg); border-radius: var(--radius); overflow: hidden; border: 2px solid #2a2a4a; transition: transform .2s, border-color .2s, box-shadow .2s; position: relative; display: flex; flex-direction: column; }
            .card:hover { transform: translateY(-3px); box-shadow: 0 6px 20px rgba(0,0,0,.35); }
            .card.SELECTED { border-color: var(--selected); }
            .card.DISCARDED { border-color: var(--discarded); opacity: .8; }
            .card .card-badge { position: absolute; top: 6px; left: 6px; background: #000; color: #fff; padding: 1px 7px; border-radius: 3px; font-size: .7rem; font-weight: 700; z-index: 2; letter-spacing: .3px; }
            .card-checkbox { position: absolute; top: 6px; right: 6px; z-index: 3; width: 24px; height: 24px; cursor: pointer; appearance: none; -webkit-appearance: none; background: rgba(0,0,0,.6); border: 2px solid #555; border-radius: 4px; display: flex; align-items: center; justify-content: center; transition: all .15s; }
            .card-checkbox:checked { background: var(--selected); border-color: var(--selected); }
            .card-checkbox:checked::after { content: '\\\\2713'; color: #000; font-weight: 700; font-size: 14px; line-height: 1; }
            .card img { width: 100%; height: 160px; object-fit: cover; display: block; border-bottom: 1px solid #2a2a4a; cursor: pointer; }
            .card-body { padding: .5rem .7rem .65rem; flex: 1; display: flex; flex-direction: column; gap: .3rem; }
            .card-body .card-name { font-size: .82rem; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; cursor: pointer; }
            .card-body .card-prices { font-size: .75rem; color: var(--text-muted); display: flex; gap: .4rem; flex-wrap: wrap; }
            .card-body .card-prices .price-tag { background: #000; color: #fff; padding: 0 5px; border-radius: 2px; font-weight: 600; }
            .card-body .card-footer { display: flex; align-items: center; justify-content: space-between; margin-top: auto; }
            .card-body .badge { font-size: .7rem; padding: .1rem .5rem; border-radius: 8px; font-weight: 600; }
            .badge-sel { background: var(--selected-bg); color: var(--selected); }
            .badge-dis { background: var(--discarded-bg); color: var(--discarded); }
            .comment-dot { display: inline-block; width: 16px; height: 16px; line-height: 16px; text-align: center; border-radius: 50%; background: var(--accent); color: #fff; font-size: .6rem; cursor: help; }
            footer { position: sticky; bottom: 0; background: var(--surface); border-top: 1px solid #2a2a4a; padding: .7rem 1.2rem; display: flex; align-items: center; justify-content: space-between; z-index: 10; flex-wrap: wrap; gap: .5rem; }
            #summary { font-size: .85rem; color: var(--text-muted); }
            .footer-buttons { display: flex; gap: .5rem; }
            .btn-primary { background: var(--selected); color: #000; border: none; padding: .5rem 1.2rem; border-radius: 6px; font-weight: 700; cursor: pointer; transition: opacity .2s; font-size: .85rem; }
            .btn-primary:hover { opacity: .85; }
            .btn-secondary { background: var(--accent); color: #fff; border: 1px solid #5a5a9a; padding: .5rem 1.2rem; border-radius: 6px; font-weight: 600; cursor: pointer; transition: opacity .2s; font-size: .85rem; }
            .btn-secondary:hover { opacity: .85; }
            .modal { position: fixed; inset: 0; background: rgba(0,0,0,.85); display: flex; align-items: center; justify-content: center; z-index: 100; }
            .modal.hidden { display: none; }
            .modal-content { background: var(--card-bg); border-radius: var(--radius); max-width: 680px; width: 94%; max-height: 92vh; overflow-y: auto; padding: 1.5rem; position: relative; border: 1px solid #3a3a5a; }
            .modal-close { position: absolute; top: .7rem; right: .9rem; background: none; border: none; color: var(--text-muted); font-size: 1.5rem; cursor: pointer; z-index: 3; line-height: 1; padding: 0 4px; }
            .modal-close:hover { color: #fff; }
            .modal-header { margin-bottom: .8rem; }
            .modal-header h3 { font-size: 1.1rem; font-weight: 600; margin-top: .2rem; }
            .modal-codigo { display: inline-block; background: #000; color: #fff; padding: 1px 8px; border-radius: 3px; font-size: .75rem; font-weight: 700; }
            .modal-image { width: 100%; border-radius: 6px; margin-bottom: .8rem; max-height: 40vh; object-fit: contain; background: #0a0a15; cursor: pointer; }
            .etiqueta { color: var(--text-muted); font-size: .82rem; margin-bottom: .5rem; }
            .modal-checkboxes { margin-bottom: .8rem; }
            .modal-checkboxes table { width: 100%; border-collapse: collapse; font-size: .82rem; }
            .modal-checkboxes th, .modal-checkboxes td { padding: 5px 8px; text-align: left; border-bottom: 1px solid #2a2a4a; }
            .modal-checkboxes th { color: var(--text-muted); font-weight: 600; }
            .modal-checkbox-toggle { margin-bottom: 1rem; }
            .modal-checkbox-toggle label { display: flex; align-items: center; gap: .5rem; cursor: pointer; font-size: .9rem; color: var(--text); }
            .modal-checkbox-toggle input[type=checkbox] { width: 18px; height: 18px; cursor: pointer; accent-color: var(--selected); }
            .ck { display: inline-block; font-weight: 700; width: 1.2em; text-align: center; }
            .ck-sel { color: var(--selected); }
            .ck-dis { color: var(--discarded); }
            label { display: block; margin-bottom: .3rem; font-size: .82rem; color: var(--text-muted); }
            textarea { width: 100%; background: var(--bg); border: 1px solid #3a3a5a; border-radius: 5px; color: var(--text); padding: .5rem; font-size: .9rem; resize: vertical; outline: none; }
            textarea:focus { border-color: var(--accent); }
            @media (max-width: 600px) {
              header h1 { font-size: 1.3rem; }
              .gallery { grid-template-columns: repeat(2, 1fr); gap: .5rem; padding: .5rem; }
              .card img { height: 110px; }
              footer { flex-direction: column; align-items: stretch; text-align: center; }
              .footer-buttons { flex-direction: column; }
              .btn-primary, .btn-secondary { width: 100%; }
            }
            """;
    } // --- Fin de metodo getCss ---


    private String getJs(String respuestaFilename, String prjclFilename) {
        return """
            var data = CATALOG_DATA || { imagenes: [] };
            var currentIndex = -1;

            renderGallery();

            function renderGallery() {
              var gallery = document.getElementById('gallery');
              var html = '';
              for (var i = 0; i < data.imagenes.length; i++) {
                var img = data.imagenes[i];
                var estado = img.estado || 'UNDEFINED';
                var checked = estado === 'SELECTED';
                var codigoHtml = img.codigo ? '<div class=\\"card-badge\\">' + escHtml(img.codigo) + '</div>' : '';
                var prices = [];
                if (img.checkboxes) {
                  for (var j = 0; j < img.checkboxes.length; j++) {
                    if (img.checkboxes[j].precio) prices.push(img.checkboxes[j].precio);
                  }
                }
                var pricesHtml = '';
                if (prices.length > 0) {
                  pricesHtml = '<div class=\\"card-prices\\">' + prices.map(function(p) { return '<span class=\\"price-tag\\">' + escHtml(p) + ' \\\\u20ac</span>'; }).join('') + '</div>';
                }
                var comentDot = (img.comentario && img.comentario.trim()) ? '<span class=\\"comment-dot\\" title=\\\\"Tiene comentario\\\\">\\\\u2709</span>' : '';
                var checkedAttr = checked ? 'checked' : '';
                html += '<div class=\\"card ' + estado + '\\">'
                  + '<input type=\\"checkbox\\" class=\\"card-checkbox\\" ' + checkedAttr + ' data-index="' + i + '">'
                  + codigoHtml
                  + '<img src=\\"' + img.miniatura + '\\" alt=\\"' + escHtml(img.nombre) + '\\" loading=\\"lazy\\"'
                  + ' onclick=\\"openModal(' + i + ')\\"'
                  + ' onerror=\\"this.src=' + "'data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' width='200' height='160'><rect width='200' height='160' fill='%%23222'/><text x='50%%' y='50%%' fill='%%23666' text-anchor='middle' dy='.3em'>Sin imagen</text></svg>'" + '\\">'
                  + '<div class=\\"card-body\\">'
                  + '<div class=\\"card-name\\" onclick=\\"openModal(' + i + ')\\">' + escHtml(img.nombre) + '</div>'
                  + pricesHtml
                  + '<div class=\\"card-footer\\">'
                  + '<span class=\\"badge ' + (checked ? 'badge-sel' : 'badge-dis') + '\\">' + (checked ? '\\\\u2713' : '\\\\u2717') + '</span>'
                  + comentDot
                  + '</div>'
                  + '</div>'
                  + '</div>';
              }
              gallery.innerHTML = html;

              // Attach checkbox change listeners
              gallery.querySelectorAll('.card-checkbox').forEach(function(cb) {
                cb.addEventListener('change', function() {
                  var idx = parseInt(cb.dataset.index);
                  if (isNaN(idx)) return;
                  var img = data.imagenes[idx];
                  img.estado = cb.checked ? 'SELECTED' : 'DISCARDED';
                  renderGallery();
                });
              });

              updateCounter();
              updateSummary();
            }

            function escHtml(s) { return (s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }

            function updateCounter() {
              var sel = data.imagenes.filter(function(i) { return i.estado === 'SELECTED'; }).length;
              var dis = data.imagenes.filter(function(i) { return i.estado === 'DISCARDED'; }).length;
              var und = data.imagenes.filter(function(i) { return !i.estado || i.estado === 'UNDEFINED'; }).length;
              document.getElementById('counter').textContent = 'Total: ' + data.imagenes.length + ' | \\\\u2713 ' + sel + ' \\\\u2717 ' + dis + ' ? ' + und;
            }

            function updateSummary() {
              var sel = data.imagenes.filter(function(i) { return i.estado === 'SELECTED'; }).length;
              var dis = data.imagenes.filter(function(i) { return i.estado === 'DISCARDED'; }).length;
              document.getElementById('summary').textContent = '\\\\u2713 ' + sel + ' seleccionadas \\\\u00b7 \\\\u2717 ' + dis + ' descartadas';
            }

            function openModal(index) {
              currentIndex = index;
              var img = data.imagenes[index];
              document.getElementById('modalCodigo').textContent = img.codigo || '';
              document.getElementById('modalNombre').textContent = img.nombre;
              document.getElementById('modalImg').src = img.miniatura;
              document.getElementById('modalEtiqueta').textContent = img.etiqueta || '';
              document.getElementById('modalComentario').value = img.comentario || '';
              document.getElementById('modalCheckToggle').checked = (img.estado === 'SELECTED');

              var cbContainer = document.getElementById('modalCheckboxes');
              if (img.checkboxes && img.checkboxes.length > 0) {
                var tblHtml = '<table><tr><th></th><th>C\\\\u00f3digo</th><th>Precio</th><th>Comentario</th></tr>';
                for (var j = 0; j < img.checkboxes.length; j++) {
                  var cb = img.checkboxes[j];
                  var iconoCb = cb.marcado ? '<span class=\\"ck ck-sel\\">\\\\u2713</span>' : '<span class=\\"ck ck-dis\\">\\\\u2717</span>';
                  var precio = cb.precio ? cb.precio + ' \\\\u20ac' : '\\\\u2014';
                  var coment = cb.comentario || '\\\\u2014';
                  var bubble = coment !== '\\\\u2014' ? ' <span class=\\"comment-dot\\" title=\\\\"Comentario\\\\">\\\\u2709</span>' : '';
                  tblHtml += '<tr><td>' + iconoCb + '</td><td>' + escHtml(cb.codigo||'') + '</td><td>' + precio + '</td><td>' + escHtml(coment) + bubble + '</td></tr>';
                }
                tblHtml += '</table>';
                cbContainer.innerHTML = tblHtml;
              } else {
                cbContainer.innerHTML = '<p style=\\"color:var(--text-muted);font-size:.82rem\\">Sin checkboxes</p>';
              }

              document.getElementById('modal').classList.remove('hidden');
            }

            document.getElementById('modalClose').addEventListener('click', function() {
              guardarModal();
              document.getElementById('modal').classList.add('hidden');
            });

            document.getElementById('modal').addEventListener('click', function(e) {
              if (e.target === document.getElementById('modal')) {
                guardarModal();
                document.getElementById('modal').classList.add('hidden');
              }
            });

            document.getElementById('modalCheckToggle').addEventListener('change', function() {
              if (currentIndex >= 0) {
                data.imagenes[currentIndex].estado = this.checked ? 'SELECTED' : 'DISCARDED';
                renderGallery();
              }
            });

            document.getElementById('modalComentario').addEventListener('input', function(e) {
              if (currentIndex >= 0) data.imagenes[currentIndex].comentario = e.target.value;
            });

            document.getElementById('modalImg').addEventListener('click', function() {
              // Close modal when clicking the image (toggle back)
            });

            function guardarModal() {
              if (currentIndex >= 0) {
                data.imagenes[currentIndex].comentario = document.getElementById('modalComentario').value;
              }
              renderGallery();
            }

            document.getElementById('btnGenerar').addEventListener('click', function() {
              var und = data.imagenes.filter(function(i) { return !i.estado || i.estado === 'UNDEFINED'; }).length;
              if (und > 0) {
                if (!confirm('Atenci\\\\u00f3n: hay ' + und + ' imagen(es) sin marcar. \\\\u00bfDeseas descargar la respuesta igualmente?')) return;
              }
              var respuesta = {
                projectName: data.projectName,
                iteracion: data.iteracion,
                fechaEnvio: data.exportDate,
                fechaRespuesta: new Date().toISOString(),
                respuestas: data.imagenes.map(function(img) {
                  return {
                    id: img.id,
                    codigo: img.codigo || '',
                    estado: img.estado || 'UNDEFINED',
                    comentario: img.comentario || '',
                    checkboxes: (img.checkboxes || []).map(function(cb) {
                      return { codigo: cb.codigo || '', marcado: cb.marcado || false };
                    })
                  };
                })
              };
              var blob = new Blob([JSON.stringify(respuesta, null, 2)], {type: 'application/json'});
              var a = document.createElement('a');
              a.href = URL.createObjectURL(blob);
              a.download = '%s';
              a.click();
              URL.revokeObjectURL(a.href);
            });

            document.getElementById('btnDescargarPrjcl').addEventListener('click', function() {
              var prjcl = {
                projectName: data.projectName,
                iteracion: data.iteracion,
                exportDate: data.exportDate,
                fechaRespuesta: new Date().toISOString(),
                respuestas: data.imagenes.map(function(img) {
                  return {
                    id: img.id,
                    codigo: img.codigo || '',
                    estado: img.estado || 'UNDEFINED',
                    comentario: img.comentario || '',
                    checkboxes: (img.checkboxes || []).map(function(cb) {
                      return {
                        codigo: cb.codigo || '',
                        marcado: cb.marcado || false,
                        x: cb.x || 0, y: cb.y || 0,
                        precio: cb.precio || '',
                        comentario: cb.comentario || ''
                      };
                    }),
                    commentOverlay: img.commentOverlay || null
                  };
                })
              };
              var blob = new Blob([JSON.stringify(prjcl, null, 2)], {type: 'application/json'});
              var a = document.createElement('a');
              a.href = URL.createObjectURL(blob);
              a.download = '%s';
              a.click();
              URL.revokeObjectURL(a.href);
            });

            """.formatted(respuestaFilename, prjclFilename);
    } // --- Fin de metodo getJs ---


    private String nombreMiniatura(Path original) {
        String ext = obtenerExtension(original);
        boolean salidaJpeg = forceJpeg || "jpg".equals(ext) || "jpeg".equals(ext);
        String outExt = salidaJpeg ? "jpg" : ext;
        return sanitizarNombre(
                original.getFileName() != null ? original.getFileName().toString() : "imagen")
                + "_thumb." + outExt;
    } // --- Fin de metodo nombreMiniatura ---


    // -----------------------------------------------------------------------
    // Utilidades
    // -----------------------------------------------------------------------

    private String generarId(Path ruta) {
        String nombre = ruta.getFileName() != null ? ruta.getFileName().toString() : ruta.toString();
        long size = 0;
        try {
            if (Files.exists(ruta)) size = Files.size(ruta);
        } catch (IOException ignored) {}
        return sanitizarNombre(nombre) + "_" + size;
    } // --- Fin de metodo generarId ---


    private String sanitizarNombre(String nombre) {
        if (nombre == null) return "proyecto";
        return nombre.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    } // --- Fin de metodo sanitizarNombre ---


    private String obtenerExtension(Path ruta) {
        String nombre = ruta.getFileName() != null ? ruta.getFileName().toString() : "";
        int dot = nombre.lastIndexOf('.');
        if (dot > 0) {
            String ext = nombre.substring(dot + 1).toLowerCase();
            return ext.isEmpty() ? "jpg" : ext;
        }
        return "jpg";
    } // --- Fin de metodo obtenerExtension ---


    private String jsonString(String valor) {
        if (valor == null) return "null";
        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"")
                           .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    } // --- Fin de metodo jsonString ---


    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    } // --- Fin de metodo escapeHtml ---

} // --- Fin de clase WebCatalogExporter ---


