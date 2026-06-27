package servicios.cliente;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import modelo.proyecto.ImageCheckboxOverlay;
import modelo.proyecto.ProjectModel;
import modelo.proyecto.SelectionState;
import net.coobird.thumbnailator.Thumbnails;

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

        String dataJsContent = "var CATALOG_DATA = " + dataJson + ";";
        Path dataJsPath = outputDir.resolve("data.js");
        Files.writeString(dataJsPath, dataJsContent, StandardCharsets.UTF_8);

        if (progressCallback != null) progressCallback.accept(90);

        String projectSafeName = sanitizarNombre(project.getProjectName());
        String respuestaVacia = generarRespuestaVacia(project, projectSafeName, iteracion);
        Path respuestaPath = outputDir.resolve(projectSafeName + "_iteracion" + iteracion + "_respuesta.json");
        Files.writeString(respuestaPath, respuestaVacia, StandardCharsets.UTF_8);
        if (progressCallback != null) progressCallback.accept(95);

        String htmlContent = generarHtml(project, projectSafeName, iteracion);
        Path htmlPath = outputDir.resolve("index.html");
        Files.writeString(htmlPath, htmlContent, StandardCharsets.UTF_8);
        if (progressCallback != null) progressCallback.accept(100);

        logger.info("[WebCatalogExporter] Exportación completada en: {}", outputDir.toAbsolutePath());
    }

    public void exportar(ProjectModel project, Path outputDir, int iteracion) throws IOException {
        exportar(project, outputDir, iteracion, null);
    }

    private static record QualityLevel(int maxDimension, double jpegQuality) {}
    private static record ThumbnailResult(String base64, int width, int height, int origWidth, int origHeight) {}

    private QualityLevel determinarCalidad(int imageCount) {
        if (imageCount <= 20)  return new QualityLevel(800, 0.85);
        if (imageCount <= 50)  return new QualityLevel(600, 0.75);
        return new QualityLevel(400, 0.60);
    }

    public void exportarHtmlCliente(ProjectModel project, Path outputFile, int iteracion) throws IOException {
        this.exportarHtmlCliente(project, outputFile, iteracion, null);
    }

    public void exportarHtmlCliente(ProjectModel project, Path outputFile, int iteracion,
                                    java.util.function.Consumer<Integer> progressCallback) throws IOException {
        logger.info("[WebCatalogExporter] Generando HTML cliente único: {}", outputFile);

        java.util.List<String> imageKeys = new java.util.ArrayList<>();
        for (var pi : project.getMasterImages().values()) {
            if (pi.isEnSeleccionProyecto()) {
                imageKeys.add(pi.getRutaImagen());
            }
        }
        if (imageKeys.isEmpty()) {
            throw new IOException("El proyecto no tiene imágenes compartidas con el cliente.");
        }

        int total = imageKeys.size();
        QualityLevel quality = determinarCalidad(total);

        StringBuilder imagesJson = new StringBuilder();
        imagesJson.append("[");
        String projectName = project.getProjectName() != null ? project.getProjectName() : "Proyecto";

        for (int i = 0; i < imageKeys.size(); i++) {
            String key = imageKeys.get(i);
            var pi = project.getMasterImages().get(key);
            if (pi == null) continue;
            String imageCode = pi.getCodigoCatalogo();
            String comment = pi.getComment() != null ? pi.getComment() : "";
            var overlays = pi.getCheckboxes();
            var commentOverlay = pi.getCommentOverlay();

            int thumbW = 0, thumbH = 0;
            int origW = 1, origH = 1;
            String base64 = "";
            if (key != null) {
                Path rutaImagen = Path.of(key);
                try {
                    ThumbnailResult tr = generarMiniaturaBase64(rutaImagen, quality);
                    base64 = tr.base64();
                    thumbW = tr.width();
                    thumbH = tr.height();
                    origW = tr.origWidth();
                    origH = tr.origHeight();
                } catch (Exception e) {
                    logger.warn("[WebCatalogExporter] Error generando miniatura para {}: {}", key, e.getMessage());
                }
            }
            if (progressCallback != null) {
                progressCallback.accept((i + 1) * 100 / Math.max(total, 1));
            }
            String id = generarId(key);

            if (i > 0) imagesJson.append(",");
            imagesJson.append("{\n");
            imagesJson.append("  \"id\": ").append(jsonString(id)).append(",\n");
            imagesJson.append("  \"codigo\": ").append(jsonString(imageCode)).append(",\n");
            String nombre = Path.of(key).getFileName() != null ? Path.of(key).getFileName().toString() : key;
            imagesJson.append("  \"nombre\": ").append(jsonString(nombre)).append(",\n");
            SelectionState state = pi.getEstadoCliente();
            imagesJson.append("  \"estado\": ").append(jsonString(state != null ? state.name() : "UNDEFINED")).append(",\n");
            imagesJson.append("  \"comentario\": ").append(jsonString(comment)).append(",\n");
            imagesJson.append("  \"ancho\": ").append(thumbW).append(",\n");
            imagesJson.append("  \"alto\": ").append(thumbH).append(",\n");
            imagesJson.append("  \"anchoOriginal\": ").append(origW).append(",\n");
            imagesJson.append("  \"altoOriginal\": ").append(origH).append(",\n");
            imagesJson.append("  \"miniatura\": ").append(jsonString("data:image/jpeg;base64," + base64)).append(",\n");

            if (commentOverlay != null && commentOverlay.getText() != null && !commentOverlay.getText().isEmpty()) {
                imagesJson.append("  \"commentOverlay\": {\n");
                imagesJson.append("    \"texto\": ").append(jsonString(commentOverlay.getText())).append(",\n");
                imagesJson.append("    \"x\": ").append(commentOverlay.getImageX()).append(",\n");
                imagesJson.append("    \"y\": ").append(commentOverlay.getImageY()).append("\n");
                imagesJson.append("  },\n");
            }

            imagesJson.append("  \"checkboxes\": [\n");
            if (overlays != null) {
                for (int j = 0; j < overlays.size(); j++) {
                    var cb = overlays.get(j);
                    if (j > 0) imagesJson.append(",\n");
                    imagesJson.append("    {\n");
                    imagesJson.append("      \"codigo\": ").append(jsonString(cb.getCheckboxCode())).append(",\n");
                    imagesJson.append("      \"x\": ").append(cb.getImageX()).append(",\n");
                    imagesJson.append("      \"y\": ").append(cb.getImageY()).append(",\n");
                    imagesJson.append("      \"tamano\": ").append(cb.getSize()).append(",\n");
                    imagesJson.append("      \"precio\": ").append(jsonString(cb.getPrice() > 0 ? String.format("%.2f", cb.getPrice()) : "")).append(",\n");
                    imagesJson.append("      \"estado\": ").append(jsonString(cb.getState().name())).append(",\n");
                    imagesJson.append("      \"comentario\": ").append(jsonString(cb.getComment() != null ? cb.getComment() : "")).append("\n");
                    imagesJson.append("    }");
                }
            }
            imagesJson.append("\n  ]\n");
            imagesJson.append("}");
        }
        imagesJson.append("]");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        StringBuilder dataJson = new StringBuilder();
        dataJson.append("{\n");
        dataJson.append("  \"projectName\": ").append(jsonString(projectName)).append(",\n");
        dataJson.append("  \"iteracion\": ").append(iteracion).append(",\n");
        dataJson.append("  \"fechaEnvio\": ").append(jsonString(timestamp)).append(",\n");
        dataJson.append("  \"imagenes\": ").append(imagesJson).append("\n");
        dataJson.append("}");

        String projectSafeName = sanitizarNombre(projectName);
        String respuestaFilename = "respuesta_" + projectSafeName + "_iteracion" + iteracion + ".json";
        String htmlContent = generarHtmlCliente(projectName, dataJson.toString(), respuestaFilename, iteracion);

        Files.writeString(outputFile, htmlContent, StandardCharsets.UTF_8);
        logger.info("[WebCatalogExporter] HTML cliente generado: {}", outputFile.getFileName());
    }

    private ThumbnailResult generarMiniaturaBase64(Path ruta, QualityLevel quality) throws IOException {
        if (!Files.exists(ruta)) return new ThumbnailResult("", 0, 0, 1, 1);
        int targetSize = quality.maxDimension;
        int thumbW = 0, thumbH = 0;
        int srcW = 1, srcH = 1;

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try {
            BufferedImage srcImg = ImageIO.read(ruta.toFile());
            if (srcImg == null) return new ThumbnailResult("", 0, 0, 1, 1);
            srcW = srcImg.getWidth();
            srcH = srcImg.getHeight();
            double scale = Math.min((double) targetSize / srcW, (double) targetSize / srcH);
            thumbW = Math.max(1, (int) (srcW * scale));
            thumbH = Math.max(1, (int) (srcH * scale));
            BufferedImage thumb = new BufferedImage(thumbW, thumbH, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = thumb.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(srcImg, 0, 0, thumbW, thumbH, null);
            g.dispose();
            ImageIO.write(thumb, "JPEG", baos);
        } catch (Exception e) {
            return new ThumbnailResult("", 0, 0, 1, 1);
        }

        byte[] bytes = baos.toByteArray();
        String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
        return new ThumbnailResult(base64, thumbW, thumbH, srcW, srcH);
    }

    private String generarMiniatura(Path original, Path thumbsDir) throws IOException {
        String ext = obtenerExtension(original);
        boolean salidaJpeg = forceJpeg || "jpg".equals(ext) || "jpeg".equals(ext);
        String outExt = salidaJpeg ? "jpg" : ext;
        String safeName = sanitizarNombre(original.getFileName() != null ? original.getFileName().toString() : "imagen")
                + "_thumb." + outExt;
        Path thumbPath = thumbsDir.resolve(safeName);

        if (Files.exists(thumbPath)) return safeName;

        try {
            Thumbnails.of(original.toFile())
                    .size(thumbnailSize, thumbnailSize)
                    .crop(net.coobird.thumbnailator.geometry.Positions.CENTER)
                    .outputFormat(salidaJpeg ? "jpg" : ext)
                    .outputQuality(jpegQuality)
                    .toFile(thumbPath.toFile());
        } catch (Exception e) {
            BufferedImage srcImg = ImageIO.read(original.toFile());
            if (srcImg != null) {
                int w = Math.min(srcImg.getWidth(), thumbnailSize);
                int h = Math.min(srcImg.getHeight(), thumbnailSize);
                BufferedImage thumb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = thumb.createGraphics();
                g.drawImage(srcImg, 0, 0, w, h, null);
                g.dispose();
                ImageIO.write(thumb, salidaJpeg ? "jpg" : ext, thumbPath.toFile());
            }
        }
        return safeName;
    }

    private String generarHtmlCliente(String projectName, String dataJson, String respuestaFilename, int iteracion) {
        return "<!DOCTYPE html>\n"
            + "<html lang=\"es\">\n"
            + "<head>\n"
            + "  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\">\n"
            + "  <title>" + escapeHtml(projectName) + " \u2013 Cat\u00e1logo</title>\n"
            + "  <style>\n" + getClientCss() + "  </style>\n"
            + "</head>\n"
            + "<body>\n"
            + "  <script>\nvar CATALOG_DATA = " + dataJson + ";\n  </script>\n"
            + "  <header>\n"
            + "    <h1>" + escapeHtml(projectName) + "</h1>\n"
            + "    <p class=\"subtitle\">Iteraci\u00f3n #" + iteracion + " \u00b7 Marca los modelos que te interesan</p>\n"
            + "  </header>\n"
            + "  <nav id=\"topbar\"><span id=\"counter\"></span></nav>\n"
            + "  <main id=\"gallery\" class=\"gallery\"></main>\n"
            + "  <footer>\n"
            + "    <div id=\"summary\"></div>\n"
            + "    <div class=\"footer-buttons\">\n"
            + "      <button id=\"btnCopiar\" class=\"btn-primary\">Copiar respuesta</button>\n"
            + "      <button id=\"btnDescargar\" class=\"btn-secondary\">Descargar respuesta</button>\n"
            + "    </div>\n"
            + "  </footer>\n"
            + "  <div id=\"modal\" class=\"modal hidden\">\n"
            + "    <div class=\"modal-content\">\n"
            + "      <button id=\"modalClose\" class=\"modal-close\">&times;</button>\n"
            + "      <div class=\"modal-header\">\n"
            + "        <div class=\"modal-info\">\n"
            + "          <span class=\"modal-codigo\" id=\"modalCodigo\"></span>\n"
            + "          <span class=\"modal-nombre\" id=\"modalNombre\"></span>\n"
            + "          <span class=\"tristate-cb\" id=\"modalCheckPrincipal\" tabindex=\"0\">&#x25cb;</span>\n"
            + "        </div>\n"
            + "      </div>\n"
            + "      <div id=\"modalComentarioDisplay\" class=\"modal-comment-display\"></div>\n"
            + "      <div id=\"modalImageWrap\" class=\"modal-image-wrap\">\n"
            + "        <img id=\"modalImg\" src=\"\" alt=\"\" class=\"modal-image\" draggable=\"false\">\n"
            + "        <div id=\"checkboxOverlays\" class=\"checkbox-overlays\"></div>\n"
            + "      </div>\n"
            + "      <label for=\"modalComentario\">Editar comentario general:</label>\n"
            + "      <textarea id=\"modalComentario\" rows=\"3\" placeholder=\"Escribe aqu\u00ed tu comentario...\"></textarea>\n"
            + "    </div>\n"
            + "  </div>\n"
            + "  <script>\n" + getClientJs(respuestaFilename) + "  </script>\n"
            + "</body>\n"
            + "</html>\n";
    }

    private String getClientCss() {
        return "*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }\n"
             + ":root { --bg: #0f0f1a; --card-bg: #222240; --text: #f0f0f0; --accent: #3a3a6a; --selected: #2ecc71; --selected-bg: rgba(46,204,113,.12); --discarded: #e74c3c; --discarded-bg: rgba(231,76,60,.12); --undefined: #f39c12; --undefined-bg: rgba(243,156,18,.08); --text-muted: #999; --radius: 8px; --surface: #1a1a2e; }\n"
             + "body { background: var(--bg); color: var(--text); font-family: sans-serif; margin: 0; padding: 0; }\n"
             + "header { text-align: center; padding: 2rem; }\n"
             + ".gallery { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 15px; padding: 20px; max-width: 1200px; margin: 0 auto; }\n"
             + ".card { background: var(--card-bg); border-radius: 8px; border: 1px solid #3a3a6a; overflow: hidden; display: flex; flex-direction: column; position: relative; }\n"
             + ".card-header { display: flex; align-items: center; gap: .5rem; padding: .5rem; background: #1a1a2e; border-bottom: 1px solid #3a3a6a; }\n"
             + ".card-codigo { background: #000; color: #fff; padding: 1px 6px; border-radius: 3px; font-size: .7rem; font-weight: 700; white-space: nowrap; flex-shrink: 0; }\n"
             + ".card-name { font-size: .85rem; font-weight: bold; flex-grow: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer; }\n"
             + ".comment-icon { display: inline-block; width: 22px; height: 22px; cursor: pointer; opacity: 0.4; flex-shrink: 0; background-image: url('data:image/svg+xml;utf8,<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"%23ffffff\"><path d=\"M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z\"/></svg>'); background-size: contain; background-repeat: no-repeat; transition: opacity 0.2s; }\n"
             + ".comment-icon:hover { opacity: 0.8; }\n"
             + ".comment-icon.has-comment { opacity: 1; filter: drop-shadow(0 0 3px var(--selected)); }\n"
             + ".card-tristate { display: inline-flex; align-items: center; justify-content: center; width: 22px; height: 22px; border-radius: 4px; cursor: pointer; font-size: .9rem; font-weight: 700; user-select: none; background: var(--bg); border: 2px solid var(--accent); flex-shrink: 0; }\n"
             + ".card-tristate.state-selected { background: var(--selected-bg); color: var(--selected); border-color: var(--selected); }\n"
             + ".card-tristate.state-discarded { background: var(--discarded-bg); color: var(--discarded); border-color: var(--discarded); }\n"
             + ".card-tristate.state-undefined { background: var(--undefined-bg); color: var(--undefined); border-color: var(--undefined); }\n"
             + ".card-comment { font-size: .8rem; color: var(--text-muted); padding: .4rem .5rem; text-align: center; border-bottom: 1px solid #2a2a4a; background: #15152a; word-break: break-all; }\n"
             + ".card img { width: 100%; height: 180px; object-fit: cover; display: block; background: #000; cursor: pointer; margin-top: auto; }\n"
             + ".card-prices { font-size: .75rem; color: var(--text-muted); display: flex; gap: .4rem; flex-wrap: wrap; justify-content:center; }\n"
             + ".card-prices .price-tag { background: #000; color: #fff; padding: 0 5px; border-radius: 2px; font-weight: 600; }\n"
             + "#topbar { display: flex; justify-content: center; padding: .6rem 1rem; background: var(--surface); border-bottom: 1px solid #2a2a4a; }\n"
             + "#counter { color: var(--text-muted); font-size: .85rem; }\n"
             + "footer { border-top: 1px solid #2a2a4a; padding: .7rem 1.2rem; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: .5rem; background: var(--surface); }\n"
             + "#summary { font-size: .85rem; color: var(--text-muted); }\n"
             + ".footer-buttons { display: flex; gap: .5rem; }\n"
             + ".btn-primary { background: var(--selected); color: #000; border: none; padding: .5rem 1.2rem; border-radius: 6px; font-weight: 700; cursor: pointer; font-size: .85rem; }\n"
             + ".btn-primary:hover { opacity: .8; }\n"
             + ".btn-secondary { background: var(--accent); color: #fff; border: 1px solid #5a5a9a; padding: .5rem 1.2rem; border-radius: 6px; font-weight: 600; cursor: pointer; font-size: .85rem; }\n"
             + ".btn-secondary:hover { opacity: .8; }\n"
             + ".modal { position: fixed; inset: 0; background: rgba(0,0,0,0.9); display: flex; align-items: center; justify-content: center; z-index: 100; }\n"
             + ".modal.hidden { display: none !important; }\n"
             + ".modal-content { background: var(--card-bg); width: 90%; max-width: 800px; padding: 20px; border-radius: 8px; position: relative; max-height: 90vh; overflow-y: auto; border: 1px solid #3a3a5a; display: flex; flex-direction: column;}\n"
             + ".modal-close { position: absolute; top: 10px; right: 15px; cursor: pointer; font-size: 2rem; color: #fff; border: none; background: none; z-index: 10;}\n"
             + ".modal-header { margin-bottom: .4rem; padding-right: 30px; }\n"
             + ".modal-info { display: flex; align-items: center; gap: .5rem; }\n"
             + ".modal-nombre { font-size: 1.05rem; font-weight: 600; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }\n"
             + ".modal-codigo { display: inline-block; background: #000; color: #fff; padding: 1px 8px; border-radius: 3px; font-size: .7rem; font-weight: 700; flex-shrink: 0; }\n"
             + ".tristate-cb { display: inline-flex; align-items: center; justify-content: center; width: 26px; height: 26px; border-radius: 4px; cursor: pointer; font-size: 1.1rem; font-weight: 700; user-select: none; background: var(--bg); border: 2px solid var(--accent); flex-shrink: 0; }\n"
             + ".tristate-cb.state-selected { background: var(--selected-bg); color: var(--selected); border-color: var(--selected); }\n"
             + ".tristate-cb.state-discarded { background: var(--discarded-bg); color: var(--discarded); border-color: var(--discarded); }\n"
             + ".tristate-cb.state-undefined { background: var(--undefined-bg); color: var(--undefined); border-color: var(--undefined); }\n"
             + ".modal-image-wrap { width: 100%; max-height: 60vh; overflow: hidden; border-radius: 6px; background: #000; margin-bottom: .8rem; position: relative; cursor: grab; touch-action: none; }\n"
             + ".modal-image-wrap:active { cursor: grabbing; }\n"
             + ".modal-image { width: 100%; height: auto; max-height: 60vh; object-fit: contain; display: block; transform-origin: 0 0; user-select: none; -webkit-user-select: none; pointer-events: none; }\n"
             + ".checkbox-overlays { position: absolute; top: 0; left: 0; pointer-events: none; transform-origin: 0 0; }\n"
             + ".cb-overlay { position: absolute; display: flex; align-items: center; gap: 3px; pointer-events: auto; transform: translate(-50%, -50%); background: rgba(0,0,0,.85); border-radius: 4px; padding: 2px 5px; white-space: nowrap; cursor: pointer; border: 1px solid rgba(255,255,255,.2); user-select: none; }\n"
             + ".cb-overlay .cb-indicator { display: inline-flex; align-items: center; justify-content: center; width: 18px; height: 18px; border-radius: 3px; font-weight: 700; font-size: .7rem; }\n"
             + ".cb-overlay .cb-indicator.sel { background: var(--selected); color: #000; }\n"
             + ".cb-overlay .cb-indicator.dis { background: var(--discarded); color: #fff; }\n"
             + ".cb-overlay .cb-indicator.und { background: transparent; color: var(--undefined); border: 2px solid var(--undefined); }\n"
             + ".cb-overlay .cb-codigo { color: var(--text-muted); font-size: .65rem; }\n"
             + ".cb-overlay .cb-price { color: #fff; font-size: .7rem; font-weight: 600; }\n"
             + ".cb-overlay .cb-bubble { color: #aaa; font-size: .8rem; cursor: pointer; padding: 0 2px; }\n"
             + ".cb-overlay .cb-bubble:hover { color: #fff; }\n"
              + ".modal-comment-display { font-size: .85rem; color: var(--text-muted); padding: .4rem 0; margin-bottom: .4rem; border-bottom: 1px solid #2a2a4a; }\n"
              + "label { display: block; margin-bottom: .3rem; font-size: .82rem; color: var(--text-muted); }\n"
             + "textarea { width: 100%; background: var(--bg); border: 1px solid #3a3a5a; border-radius: 5px; color: var(--text); padding: .5rem; font-size: .9rem; resize: vertical; outline: none; }\n"
             + "textarea:focus { border-color: var(--accent); }\n"
             + "@media (max-width: 600px) {\n"
             + "  .gallery { grid-template-columns: repeat(2, 1fr); gap: .5rem; padding: .5rem; }\n"
             + "  footer { flex-direction: column; align-items: stretch; }\n"
             + "  .footer-buttons { flex-direction: column; }\n"
             + "  .btn-primary, .btn-secondary { width: 100%; }\n"
             + "}\n";
    }

    private String getClientJs(String respuestaFilename) {
        return "var data = CATALOG_DATA || { imagenes: [] };\n"
             + "var currentIndex = -1;\n"
             + "var zoomScale = 1, panX = 0, panY = 0, isDragging = false, startX = 0, startY = 0, lastTouchDist = 0, lastTapTime = 0;\n"
             + "renderGallery();\n"
             + "function renderGallery() {\n"
             + "  var gallery = document.getElementById('gallery');\n"
             + "  var html = '';\n"
             + "  for (var i = 0; i < data.imagenes.length; i++) {\n"
             + "    var img = data.imagenes[i];\n"
             + "    var estado = img.estado || 'UNDEFINED';\n"
             + "    var tristateClass = estado === 'SELECTED' ? 'state-selected' : (estado === 'DISCARDED' ? 'state-discarded' : 'state-undefined');\n"
             + "    var tristateChar = estado === 'SELECTED' ? escHtml('\\u2713') : (estado === 'DISCARDED' ? escHtml('\\u2717') : escHtml('\\u25cb'));\n"
             + "    var codeSpan = img.codigo ? '<span class=\"card-codigo\">' + escHtml(img.codigo) + '</span>' : '';\n"
             + "    var prices = [];\n"
             + "    if (img.checkboxes) { for (var j = 0; j < img.checkboxes.length; j++) { if (img.checkboxes[j].precio) prices.push(img.checkboxes[j].precio); } }\n"
             + "    var commentHtml = '';\n"
             + "    var hasGeneralComment = img.comentario && img.comentario.trim();\n"
             + "    var iconClass = (hasGeneralComment || prices.length > 0) ? 'comment-icon has-comment' : 'comment-icon';\n"
             + "    if (hasGeneralComment || prices.length > 0) {\n"
             + "      var prHtml = '';\n"
             + "      if (prices.length > 0) prHtml = '<div class=\"card-prices\">' + prices.map(function(p){return '<span class=\"price-tag\">'+escHtml(p)+' \\u20ac</span>';}).join('') + '</div>';\n"
             + "      var commText = hasGeneralComment ? '<div style=\"font-weight:600; font-size:.8rem;\">' + escHtml(img.comentario) + '</div>' : '';\n"
             + "      commentHtml = '<div class=\"card-comment\">' + prHtml + commText + '</div>';\n"
             + "    }\n"
             + "    html += '<div class=\"card ' + estado + '\" data-index=\"' + i + '\">'\n"
             + "      + '  <div class=\"card-header\">'\n"
             + "      + '    ' + codeSpan\n"
             + "      + '    <span class=\"card-name\" onclick=\"openModal(' + i + ')\">' + escHtml(img.nombre) + '</span>'\n"
             + "      + '    <span class=\"' + iconClass + '\" title=\"Comentarios\" onclick=\"openModal(' + i + ')\"></span>'\n"
             + "      + '    <span class=\"card-tristate ' + tristateClass + '\" onclick=\"toggleCardState(' + i + ', event)\" tabindex=\"0\">' + tristateChar + '</span>'\n"
             + "      + '  </div>'\n"
             + "      + '  ' + commentHtml\n"
             + "      + '  <img src=\"' + img.miniatura + '\" alt=\"' + escHtml(img.nombre) + '\" loading=\"lazy\" onclick=\"openModal(' + i + ')\" onerror=\"this.src=\\'data:image/svg+xml,<svg xmlns=\\'http://www.w3.org/2000/svg\\' width=\\'200\\' height=\\'200\\'><rect width=\\'200\\' height=\\'200\\' fill=\\'%23222\\'/><text x=\\'50%\\' y=\\'50%\\' fill=\\'%23666\\' text-anchor=\\'middle\\' dy=\\'.3em\\'>Sin imagen</text></svg>\\'\">'\n"
             + "      + '</div>';\n"
             + "  }\n"
             + "  gallery.innerHTML = html;\n"
             + "  updateCounter();\n"
             + "}\n"
              + "function toggleCardState(i, e) {\n"
              + "  if (e) e.stopPropagation();\n"
              + "  var img = data.imagenes[i];\n"
              + "  var estados = ['SELECTED', 'DISCARDED', 'UNDEFINED'];\n"
              + "  img.estado = estados[(estados.indexOf(img.estado || 'UNDEFINED') + 1) % estados.length];\n"
              + "  if (img.checkboxes) {\n"
              + "    for (var k = 0; k < img.checkboxes.length; k++) {\n"
              + "      img.checkboxes[k].estado = img.estado;\n"
              + "    }\n"
              + "  }\n"
              + "  renderGallery();\n"
              + "}\n"
             + "function escHtml(s) { return (s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\"/g,'&quot;'); }\n"
             + "function updateCounter() {\n"
             + "  var sel = data.imagenes.filter(function(i) { return i.estado === 'SELECTED'; }).length;\n"
             + "  var dis = data.imagenes.filter(function(i) { return i.estado === 'DISCARDED'; }).length;\n"
             + "  var und = data.imagenes.filter(function(i) { return i.estado === 'UNDEFINED'; }).length;\n"
             + "  document.getElementById('counter').textContent = 'Total: ' + data.imagenes.length + ' | \\u2713 ' + sel + ' \\u2717 ' + dis + ' \\u25cb ' + und;\n"
             + "  document.getElementById('summary').textContent = '\\u2713 ' + sel + ' seleccionadas \\u00b7 \\u2717 ' + dis + ' descartadas \\u00b7 \\u25cb ' + und + ' sin marcar';\n"
             + "}\n"
             + "function openModal(index) {\n"
             + "  currentIndex = index;\n"
             + "  var img = data.imagenes[index];\n"
              + "  document.getElementById('modalCodigo').textContent = img.codigo || '';\n"
              + "  document.getElementById('modalNombre').textContent = img.nombre;\n"
              + "  document.getElementById('modalImg').src = img.miniatura;\n"
              + "  setTristate(document.getElementById('modalCheckPrincipal'), img.estado || 'UNDEFINED');\n"
             + "  var commentDisplay = document.getElementById('modalComentarioDisplay');\n"
             + "  if (img.comentario && img.comentario.trim()) { commentDisplay.textContent = 'Comentario: ' + img.comentario; commentDisplay.style.display = 'block'; } else { commentDisplay.textContent = ''; commentDisplay.style.display = 'none'; }\n"
             + "  document.getElementById('modalComentario').value = img.comentario || '';\n"
             + "  resetZoom();\n"
             + "  var overlayContainer = document.getElementById('checkboxOverlays');\n"
             + "  overlayContainer.innerHTML = '';\n"
             + "  document.getElementById('modalImg').onload = function() { sizeOverlayContainer(); if (img.checkboxes && img.checkboxes.length > 0) positionOverlays(overlayContainer, img); };\n"
             + "  if (document.getElementById('modalImg').complete && document.getElementById('modalImg').naturalWidth > 0) { sizeOverlayContainer(); if (img.checkboxes && img.checkboxes.length > 0) positionOverlays(overlayContainer, img); }\n"
             + "  document.getElementById('modal').classList.remove('hidden');\n"
             + "}\n"
             + "function sizeOverlayContainer() {\n"
             + "  var modalImg = document.getElementById('modalImg');\n"
             + "  var ov = document.getElementById('checkboxOverlays');\n"
             + "  ov.style.width = modalImg.clientWidth + 'px';\n"
             + "  ov.style.height = modalImg.clientHeight + 'px';\n"
             + "}\n"
             + "function positionOverlays(container, imgData) {\n"
             + "  var modalImg = document.getElementById('modalImg');\n"
             + "  var origW = imgData.anchoOriginal || imgData.ancho || modalImg.naturalWidth || 1;\n"
             + "  var origH = imgData.altoOriginal || imgData.alto || modalImg.naturalHeight || 1;\n"
             + "  var containerW = modalImg.clientWidth;\n"
             + "  var containerH = modalImg.clientHeight;\n"
             + "  var scale = Math.min(containerW / origW, containerH / origH);\n"
             + "  var renderW = origW * scale;\n"
             + "  var renderH = origH * scale;\n"
             + "  var offsetX = (containerW - renderW) / 2;\n"
             + "  var offsetY = (containerH - renderH) / 2;\n"
             + "  container.innerHTML = '';\n"
             + "  for (var j = 0; j < imgData.checkboxes.length; j++) {\n"
             + "    var cb = imgData.checkboxes[j];\n"
             + "    var x = (cb.x || 0) * scale + offsetX;\n"
             + "    var y = (cb.y || 0) * scale + offsetY;\n"
             + "    var estado = cb.estado || 'UNDEFINED';\n"
             + "    var indicatorClass = estado === 'SELECTED' ? 'sel' : (estado === 'DISCARDED' ? 'dis' : 'und');\n"
             + "    var indicatorText = estado === 'SELECTED' ? '\\u2713' : (estado === 'DISCARDED' ? '\\u2717' : '\\u25cb');\n"
             + "    var bubbleHtml = cb.comentario && cb.comentario.trim() ? '<span class=\"cb-bubble\" title=\"'+escHtml(cb.comentario)+'\">\\u2709</span>' : '<span class=\"cb-bubble\" style=\"opacity:.5\" title=\"A\\u00f1adir comentario\">\\u2709</span>';\n"
             + "    var priceHtml = cb.precio ? '<span class=\"cb-price\">' + escHtml(cb.precio) + '</span>' : '';\n"
             + "    var codigoHtml = cb.codigo ? '<span class=\"cb-codigo\">' + escHtml(cb.codigo) + '</span>' : '';\n"
             + "    var overlay = document.createElement('div');\n"
             + "    overlay.className = 'cb-overlay';\n"
             + "    overlay.style.left = x + 'px'; overlay.style.top = y + 'px';\n"
             + "    overlay.innerHTML = '<span class=\"cb-indicator ' + indicatorClass + '\">' + indicatorText + '</span>' + codigoHtml + priceHtml + bubbleHtml;\n"
             + "    container.appendChild(overlay);\n"
             + "    overlay.addEventListener('click', function(e) {\n"
             + "      if (e.target.closest('.cb-bubble')) return;\n"
             + "      var idx = Array.prototype.indexOf.call(container.children, this);\n"
             + "      if (idx >= 0 && data.imagenes[currentIndex].checkboxes[idx]) {\n"
             + "        var cbData = data.imagenes[currentIndex].checkboxes[idx];\n"
             + "        cbData.estado = cycleState(cbData.estado || 'UNDEFINED');\n"
             + "        var hasSel = false, allDis = true;\n"
             + "        for(var k=0; k<data.imagenes[currentIndex].checkboxes.length; k++) {\n"
             + "          var st = data.imagenes[currentIndex].checkboxes[k].estado || 'UNDEFINED';\n"
             + "          if (st === 'SELECTED') hasSel = true;\n"
             + "          if (st !== 'DISCARDED') allDis = false;\n"
             + "        }\n"
             + "        if (hasSel) data.imagenes[currentIndex].estado = 'SELECTED';\n"
             + "        else if (allDis && data.imagenes[currentIndex].checkboxes.length > 0) data.imagenes[currentIndex].estado = 'DISCARDED';\n"
             + "        else data.imagenes[currentIndex].estado = 'UNDEFINED';\n"
              + "        positionOverlays(container, data.imagenes[currentIndex]);\n"
              + "        setTristate(document.getElementById('modalCheckPrincipal'), data.imagenes[currentIndex].estado);\n"
              + "        guardarModal();\n"
             + "      }\n"
             + "    });\n"
             + "    var bubble = overlay.querySelector('.cb-bubble');\n"
             + "    bubble.addEventListener('click', function(e) {\n"
             + "      e.stopPropagation();\n"
             + "      var idx = Array.prototype.indexOf.call(container.children, this.closest('.cb-overlay'));\n"
             + "      if (idx >= 0) {\n"
             + "        var cbData = data.imagenes[currentIndex].checkboxes[idx];\n"
             + "        var nuevo = prompt('Comentario para ' + (cbData.codigo || 'checkbox') + ':', cbData.comentario || '');\n"
             + "        if (nuevo !== null) { cbData.comentario = nuevo; positionOverlays(container, data.imagenes[currentIndex]); }\n"
             + "      }\n"
             + "    });\n"
             + "  }\n"
             + "}\n"
             + "function cycleState(c) { return c === 'SELECTED' ? 'DISCARDED' : (c === 'DISCARDED' ? 'UNDEFINED' : 'SELECTED'); }\n"
             + "function setTristate(el, state) { el.className = 'tristate-cb state-' + state.toLowerCase(); el.textContent = state === 'SELECTED' ? '\\u2713' : (state === 'DISCARDED' ? '\\u2717' : '\\u25cb'); }\n"
              + "document.getElementById('modalClose').addEventListener('click', function() { guardarModal(); document.getElementById('modal').classList.add('hidden'); });\n"
               + "document.getElementById('modalCheckPrincipal').addEventListener('click', function() { if (currentIndex >= 0) { var img = data.imagenes[currentIndex]; img.estado = cycleState(img.estado || 'UNDEFINED'); if (img.checkboxes) { for (var k = 0; k < img.checkboxes.length; k++) { img.checkboxes[k].estado = img.estado; } setTristate(this, img.estado); positionOverlays(document.getElementById('checkboxOverlays'), img); renderGallery(); } } });\n"
             + "document.getElementById('modalComentario').addEventListener('input', function(e) { if (currentIndex >= 0) { data.imagenes[currentIndex].comentario = e.target.value; var display = document.getElementById('modalComentarioDisplay'); if (e.target.value.trim()) { display.textContent = 'Comentario: ' + e.target.value; display.style.display = 'block'; } else { display.textContent = ''; display.style.display = 'none'; } } });\n"
             + "function guardarModal() { if (currentIndex < 0) return; renderGallery(); }\n"
             + "function resetZoom() { zoomScale = 1; panX = 0; panY = 0; applyTransform(); }\n"
             + "function applyTransform() { var transformStr = 'translate(' + panX + 'px, ' + panY + 'px) scale(' + zoomScale + ')'; document.getElementById('modalImg').style.transform = transformStr; document.getElementById('checkboxOverlays').style.transform = transformStr; }\n"
             + "var wrap = document.getElementById('modalImageWrap');\n"
             + "document.getElementById('modalImg').addEventListener('dragstart', function(e) { e.preventDefault(); });\n"
             + "wrap.addEventListener('mousedown', function(e) { if (e.button !== 0) return; e.preventDefault(); isDragging = true; startX = e.clientX; startY = e.clientY; wrap.style.cursor = 'grabbing'; });\n"
             + "document.addEventListener('mousemove', function(e) { if (!isDragging) return; panX += (e.clientX - startX); panY += (e.clientY - startY); startX = e.clientX; startY = e.clientY; applyTransform(); });\n"
             + "document.addEventListener('mouseup', function() { isDragging = false; wrap.style.cursor = 'grab'; });\n"
             + "wrap.addEventListener('wheel', function(e) { e.preventDefault(); zoomScale = Math.max(0.3, Math.min(6, zoomScale + (e.deltaY > 0 ? -0.15 : 0.15))); applyTransform(); }, { passive: false });\n"
             + "wrap.addEventListener('touchstart', function(e) { if (e.touches.length === 1) { isDragging = true; startX = e.touches[0].clientX; startY = e.touches[0].clientY; var now = Date.now(); if (now - lastTapTime < 300) { e.preventDefault(); if (zoomScale > 1.5) resetZoom(); else { zoomScale = 2.5; applyTransform(); } lastTapTime = 0; } else { lastTapTime = now; } } else if (e.touches.length === 2) { e.preventDefault(); var dx = e.touches[0].clientX - e.touches[1].clientX; var dy = e.touches[0].clientY - e.touches[1].clientY; lastTouchDist = Math.sqrt(dx*dx + dy*dy); } }, { passive: false });\n"
             + "wrap.addEventListener('touchmove', function(e) { if (e.touches.length === 1 && isDragging) { e.preventDefault(); panX += (e.touches[0].clientX - startX); panY += (e.touches[0].clientY - startY); startX = e.touches[0].clientX; startY = e.touches[0].clientY; applyTransform(); } else if (e.touches.length === 2) { e.preventDefault(); var dx = e.touches[0].clientX - e.touches[1].clientX; var dy = e.touches[0].clientY - e.touches[1].clientY; var dist = Math.sqrt(dx*dx + dy*dy); if (lastTouchDist > 0) { zoomScale = Math.max(0.3, Math.min(6, zoomScale * (dist / lastTouchDist))); applyTransform(); } lastTouchDist = dist; } }, { passive: false });\n"
             + "wrap.addEventListener('touchend', function(e) { if (e.touches.length < 2) lastTouchDist = 0; if (e.touches.length === 0) isDragging = false; });\n"
             + "document.getElementById('btnCopiar').addEventListener('click', function() { var respuesta = buildResponse(); var text = JSON.stringify(respuesta, null, 2); if (navigator.clipboard && navigator.clipboard.writeText) { navigator.clipboard.writeText(text).then(function() { showToast('Respuesta copiada al portapapeles'); }).catch(function() { fallbackCopy(text); }); } else { fallbackCopy(text); } });\n"
             + "function fallbackCopy(text) { var ta = document.createElement('textarea'); ta.value = text; ta.style.position = 'fixed'; ta.style.opacity = '0'; document.body.appendChild(ta); ta.select(); try { document.execCommand('copy'); showToast('Respuesta copiada'); } catch(e) { alert('No se pudo copiar. Selecciona el texto manualmente.'); } document.body.removeChild(ta); }\n"
             + "document.getElementById('btnDescargar').addEventListener('click', function() { var respuesta = buildResponse(); var json = JSON.stringify(respuesta, null, 2); var blob = new Blob([json], {type: 'application/json'}); var a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = '" + respuestaFilename + "'; a.click(); URL.revokeObjectURL(a.href); });\n"
             + "function buildResponse() { guardarModal(); return { projectName: data.projectName, iteracion: data.iteracion, fechaEnvio: data.fechaEnvio, fechaRespuesta: new Date().toISOString(), respuestas: data.imagenes.map(function(img) { return { id: img.id, codigo: img.codigo || '', estado: img.estado || 'DISCARDED', comentario: img.comentario || '', checkboxes: (img.checkboxes || []).map(function(cb) { return { codigo: cb.codigo || '', estado: cb.estado || 'UNDEFINED' }; }) }; }) }; }\n"
             + "function showToast(msg) { var t = document.createElement('div'); t.textContent = msg; t.style.cssText = 'position:fixed;bottom:80px;left:50%;transform:translateX(-50%);background:#333;color:#fff;padding:8px 16px;border-radius:6px;font-size:.85rem;z-index:200;opacity:0;transition:opacity .3s'; document.body.appendChild(t); requestAnimationFrame(function() { t.style.opacity = '1'; }); setTimeout(function() { t.style.opacity = '0'; setTimeout(function() { t.remove(); }, 300); }, 2000); }\n";
    }

    private String generarDataJson(ProjectModel project, Path thumbsDir, int iteracion) {
        String projectName = project.getProjectName() != null ? project.getProjectName() : "Proyecto";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"projectName\": ").append(jsonString(projectName)).append(",\n");
        sb.append("  \"iteracion\": ").append(iteracion).append(",\n");
        sb.append("  \"exportDate\": ").append(jsonString(timestamp)).append(",\n");
        sb.append("  \"imagenes\": [\n");

        int i = 0;
        for (var pi : project.getMasterImages().values()) {
            String rutaStr = pi.getRutaImagen();
            if (rutaStr == null) continue;
            Path rutaImagen = Path.of(rutaStr);
            String etiqueta = pi.getEtiqueta() != null ? pi.getEtiqueta() : "";

            int origW = 1, origH = 1;
            try {
                BufferedImage b = ImageIO.read(rutaImagen.toFile());
                if (b != null) { origW = b.getWidth(); origH = b.getHeight(); }
            } catch (Exception ignored) {}

            String thumbFilename = nombreMiniatura(rutaImagen);
            String imageId = generarId(rutaImagen);
            String imageCode = pi.getCodigoCatalogo() != null ? pi.getCodigoCatalogo() : "";

            List<ImageCheckboxOverlay> checkboxes = pi.getCheckboxes();
            String comment = pi.getComment() != null ? pi.getComment() : "";

            if (i > 0) sb.append(",\n");
            sb.append("    {\n");
            sb.append("      \"id\": ").append(jsonString(imageId)).append(",\n");
            sb.append("      \"nombre\": ").append(jsonString(rutaImagen.getFileName() != null ? rutaImagen.getFileName().toString() : rutaStr)).append(",\n");
            sb.append("      \"codigo\": ").append(jsonString(imageCode)).append(",\n");
            sb.append("      \"etiqueta\": ").append(jsonString(etiqueta)).append(",\n");
            sb.append("      \"miniatura\": ").append(jsonString("thumbs/" + thumbFilename)).append(",\n");
            sb.append("      \"estado\": \"DISCARDED\",\n");
            sb.append("      \"comentario\": ").append(jsonString(comment)).append(",\n");
            sb.append("      \"anchoOriginal\": ").append(origW).append(",\n");
            sb.append("      \"altoOriginal\": ").append(origH).append(",\n");

            var commentOverlay = pi.getCommentOverlay();
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
                sb.append("          \"marcado\": ").append(cb.getState() == SelectionState.SELECTED).append(",\n");
                sb.append("          \"precio\": ").append(jsonString(cb.getPrice() > 0 ? String.format("%.2f", cb.getPrice()) : "")).append(",\n");
                sb.append("          \"comentario\": ").append(jsonString(cb.getComment() != null ? cb.getComment() : "")).append("\n");
                sb.append("        }");
            }
            sb.append("\n      ]\n");
            sb.append("    }");
            i++;
        }

        sb.append("\n  ]\n}");
        return sb.toString();
    }

    private String generarRespuestaVacia(ProjectModel project, String projectSafeName, int iteracion) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"projectName\": ").append(jsonString(project.getProjectName())).append(",\n");
        sb.append("  \"iteracion\": ").append(iteracion).append(",\n");
        sb.append("  \"fechaEnvio\": ").append(jsonString(timestamp)).append(",\n");
        sb.append("  \"fechaRespuesta\": null,\n");
        sb.append("  \"respuestas\": [\n");

        int i = 0;
        for (var pi : project.getMasterImages().values()) {
            String rutaStr = pi.getRutaImagen();
            if (rutaStr == null) continue;
            Path rutaImagen = Path.of(rutaStr);
            String imageId = generarId(rutaImagen);
            if (i > 0) sb.append(",\n");
            sb.append("    {\n");
            sb.append("      \"id\": ").append(jsonString(imageId)).append(",\n");
            sb.append("      \"codigo\": ").append(jsonString(pi.getCodigoCatalogo() != null ? pi.getCodigoCatalogo() : "")).append(",\n");
            sb.append("      \"estado\": \"DISCARDED\",\n");
            sb.append("      \"comentario\": \"\"\n");
            sb.append("    }");
            i++;
        }

        sb.append("\n  ]\n}");
        return sb.toString();
    }

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
            + "  <header>\n"
            + "    <h1>" + escapeHtml(projectName) + "</h1>\n"
            + "    <p class=\"subtitle\">Iteraci\u00f3n #" + iteracion + " \u00b7 Marca los modelos que te interesan</p>\n"
            + "  </header>\n"
            + "  <nav id=\"topbar\"><span id=\"counter\"></span></nav>\n"
            + "  <main id=\"gallery\" class=\"gallery\"></main>\n"
            + "  <footer>\n"
            + "    <div id=\"summary\"></div>\n"
            + "    <div class=\"footer-buttons\">\n"
            + "      <button id=\"btnGenerar\" class=\"btn-primary\">\u2B07 Descargar Respuesta</button>\n"
            + "      <button id=\"btnDescargarPrjcl\" class=\"btn-secondary\">\u2B07 Descargar .prjcl</button>\n"
            + "    </div>\n"
            + "  </footer>\n"
            + "  <script src=\"data.js\"></script>\n"
            + "  <script>\n" + getJs(respuestaFilename, prjclFilename) + "  </script>\n"
            + "</body>\n"
            + "</html>\n";
    }

    private String getCss() {
        return "body { background: #0f0f1a; color: #f0f0f0; }";
    }

    private String getJs(String respuestaFilename, String prjclFilename) {
        return "console.log('Use exportarHtmlCliente for fully embedded features');";
    }

    private String nombreMiniatura(Path original) {
        String ext = obtenerExtension(original);
        boolean salidaJpeg = forceJpeg || "jpg".equals(ext) || "jpeg".equals(ext);
        String outExt = salidaJpeg ? "jpg" : ext;
        return sanitizarNombre(original.getFileName() != null ? original.getFileName().toString() : "imagen") + "_thumb." + outExt;
    }

    private String generarId(Path ruta) {
        String nombre = ruta.getFileName() != null ? ruta.getFileName().toString() : ruta.toString();
        long size = 0;
        try { if (Files.exists(ruta)) size = Files.size(ruta); } catch (IOException ignored) {}
        return sanitizarNombre(nombre) + "_" + size;
    }

    private String generarId(String rutaStr) { return generarId(Path.of(rutaStr)); }

    private String sanitizarNombre(String nombre) { return nombre == null ? "proyecto" : nombre.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase(); }

    private String obtenerExtension(Path ruta) {
        String nombre = ruta.getFileName() != null ? ruta.getFileName().toString() : "";
        int dot = nombre.lastIndexOf('.');
        if (dot > 0) {
            String ext = nombre.substring(dot + 1).toLowerCase();
            return ext.isEmpty() ? "jpg" : ext;
        }
        return "jpg";
    }

    private String jsonString(String valor) {
        if (valor == null) return "null";
        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
//package servicios.cliente;
//
//import java.awt.Graphics2D;
//import java.awt.RenderingHints;
//import java.awt.image.BufferedImage;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//import java.util.Map;
//
//import javax.imageio.ImageIO;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.google.gson.Gson;
//import com.google.gson.JsonSyntaxException;
//
//import modelo.proyecto.ImageCheckboxOverlay;
//import modelo.proyecto.ProjectModel;
//import modelo.proyecto.SelectionState;
//import net.coobird.thumbnailator.Thumbnails;
//
///**
// * Exportador de catálogo web para el modo cliente.
// * Genera miniaturas (con Thumbnailator o fallback Graphics2D),
// * data.json, data.js, index.html (sin filtros, checkbox nativo)
// * y un archivo de respuesta vacío.
// */
//public class WebCatalogExporter {
//
//    private static final Logger logger = LoggerFactory.getLogger(WebCatalogExporter.class);
//
//    private int thumbnailSize = 400;
//    private double jpegQuality = 0.7;
//    private boolean forceJpeg = true;
//
//    public int getThumbnailSize() { return thumbnailSize; }
//    public void setThumbnailSize(int thumbnailSize) { this.thumbnailSize = Math.max(100, thumbnailSize); }
//
//    public double getJpegQuality() { return jpegQuality; }
//    public void setJpegQuality(double jpegQuality) { this.jpegQuality = Math.max(0.1, Math.min(1.0, jpegQuality)); }
//
//    public boolean isForceJpeg() { return forceJpeg; }
//    public void setForceJpeg(boolean forceJpeg) { this.forceJpeg = forceJpeg; }
//
//    public void exportar(ProjectModel project, Path outputDir, int iteracion,
//                         java.util.function.Consumer<Integer> progressCallback) throws IOException {
//        logger.info("[WebCatalogExporter] Iniciando exportación a: {}", outputDir);
//        Files.createDirectories(outputDir);
//
//        Path thumbsDir = outputDir.resolve("thumbs");
//        Files.createDirectories(thumbsDir);
//
//        Map<String, String> selectedImages = project.getSelectedImages();
//        int total = selectedImages.size();
//        int done = 0;
//
//        for (String rutaStr : selectedImages.keySet()) {
//            Path rutaImagen = Path.of(rutaStr);
//            generarMiniatura(rutaImagen, thumbsDir);
//            done++;
//            if (progressCallback != null) {
//                progressCallback.accept(done * 100 / Math.max(total, 1));
//            }
//        }
//
//        String dataJson = generarDataJson(project, thumbsDir, iteracion);
//        Path dataJsonPath = outputDir.resolve("data.json");
//        Files.writeString(dataJsonPath, dataJson, StandardCharsets.UTF_8);
//        logger.info("[WebCatalogExporter] data.json generado ({} bytes)", dataJson.length());
//
//        String dataJsContent = "var CATALOG_DATA = " + dataJson + ";";
//        Path dataJsPath = outputDir.resolve("data.js");
//        Files.writeString(dataJsPath, dataJsContent, StandardCharsets.UTF_8);
//        logger.info("[WebCatalogExporter] data.js generado.");
//
//        if (progressCallback != null) progressCallback.accept(90);
//
//        String projectSafeName = sanitizarNombre(project.getProjectName());
//        String respuestaVacia = generarRespuestaVacia(project, projectSafeName, iteracion);
//        Path respuestaPath = outputDir.resolve(projectSafeName + "_iteracion" + iteracion + "_respuesta.json");
//        Files.writeString(respuestaPath, respuestaVacia, StandardCharsets.UTF_8);
//        logger.info("[WebCatalogExporter] Respuesta vacía generada: {}", respuestaPath.getFileName());
//        if (progressCallback != null) progressCallback.accept(95);
//
//        String htmlContent = generarHtml(project, projectSafeName, iteracion);
//        Path htmlPath = outputDir.resolve("index.html");
//        Files.writeString(htmlPath, htmlContent, StandardCharsets.UTF_8);
//        logger.info("[WebCatalogExporter] index.html generado.");
//        if (progressCallback != null) progressCallback.accept(100);
//
//        logger.info("[WebCatalogExporter] Exportación completada en: {}", outputDir.toAbsolutePath());
//    } // --- Fin de metodo exportar ---
//
//
//    public void exportar(ProjectModel project, Path outputDir, int iteracion) throws IOException {
//        exportar(project, outputDir, iteracion, null);
//    } // --- Fin de metodo exportar ---
//
//
//    // =========================================================================
//    // Exportar HTML autónomo (archivo único con todo embebido)
//    // =========================================================================
//
//    private static record QualityLevel(int maxDimension, double jpegQuality) {}
//    private static record ThumbnailResult(String base64, int width, int height) {}
//
//    private QualityLevel determinarCalidad(int imageCount) {
//        if (imageCount <= 20)  return new QualityLevel(800, 0.85);
//        if (imageCount <= 50)  return new QualityLevel(600, 0.75);
//        return new QualityLevel(400, 0.60);
//    }
//
//    /**
//     * Genera un único archivo HTML con miniaturas base64, datos y lógica JS
//     * para que el cliente revise, marque y devuelva su respuesta.
//     */
//    public void exportarHtmlCliente(ProjectModel project, Path outputFile, int iteracion) throws IOException {
//        logger.info("[WebCatalogExporter] Generando HTML cliente único: {}", outputFile);
//
//        if (!project.hasClientSelection()) {
//            throw new IOException("El proyecto no tiene selección de cliente.");
//        }
//        var clientSel = project.getClientSelection();
//
//        // Recopilar solo claves de imagen reales (no compuestas)
//        java.util.List<String> imageKeys = new java.util.ArrayList<>();
//        for (String key : clientSel.getImages().keySet()) {
//            if (project.esClaveRutaImagen(key)) {
//                imageKeys.add(key);
//            }
//        }
//
//        int total = imageKeys.size();
//        QualityLevel quality = determinarCalidad(total);
//        logger.info("[WebCatalogExporter] {} imágenes, calidad: max={}px, JPEG={}%",
//                total, quality.maxDimension, (int)(quality.jpegQuality * 100));
//
//        // Construir JSON de datos con miniaturas base64
//        StringBuilder imagesJson = new StringBuilder();
//        imagesJson.append("[");
//        String projectName = project.getProjectName() != null ? project.getProjectName() : "Proyecto";
//
//        for (int i = 0; i < imageKeys.size(); i++) {
//            String key = imageKeys.get(i);
//            String rutaCanon = project.resolverClaveImagenCanonica(key);
//            String imageCode = project.getCodigoImagen(key);
//            String comment = clientSel.getComments().getOrDefault(key, "");
//            var overlays = clientSel.getImageCheckboxes(rutaCanon);
//            var commentOverlay = clientSel.getCommentOverlays().get(key);
//
//            // Generar base64
//            int thumbW = 0, thumbH = 0;
//            String base64 = "";
//            if (rutaCanon != null) {
//                Path rutaImagen = Path.of(rutaCanon);
//                try {
//                    ThumbnailResult tr = generarMiniaturaBase64(rutaImagen, quality);
//                    base64 = tr.base64();
//                    thumbW = tr.width();
//                    thumbH = tr.height();
//                } catch (Exception e) {
//                    logger.warn("[WebCatalogExporter] Error generando miniatura para {}: {}", key, e.getMessage());
//                }
//            }
//            String id = generarId(key);
//
//            if (i > 0) imagesJson.append(",");
//            imagesJson.append("{\n");
//            imagesJson.append("  \"id\": ").append(jsonString(id)).append(",\n");
//            imagesJson.append("  \"codigo\": ").append(jsonString(imageCode)).append(",\n");
//            String nombre = Path.of(key).getFileName() != null ? Path.of(key).getFileName().toString() : key;
//            imagesJson.append("  \"nombre\": ").append(jsonString(nombre)).append(",\n");
//            SelectionState state = clientSel.getImages().get(key);
//            imagesJson.append("  \"estado\": ").append(jsonString(state != null ? state.name() : "UNDEFINED")).append(",\n");
//            imagesJson.append("  \"comentario\": ").append(jsonString(comment)).append(",\n");
//            imagesJson.append("  \"ancho\": ").append(thumbW).append(",\n");
//            imagesJson.append("  \"alto\": ").append(thumbH).append(",\n");
//            imagesJson.append("  \"miniatura\": ").append(jsonString("data:image/jpeg;base64," + base64)).append(",\n");
//
//            if (commentOverlay != null && commentOverlay.getText() != null && !commentOverlay.getText().isEmpty()) {
//                imagesJson.append("  \"commentOverlay\": {\n");
//                imagesJson.append("    \"texto\": ").append(jsonString(commentOverlay.getText())).append(",\n");
//                imagesJson.append("    \"x\": ").append(commentOverlay.getImageX()).append(",\n");
//                imagesJson.append("    \"y\": ").append(commentOverlay.getImageY()).append("\n");
//                imagesJson.append("  },\n");
//            }
//
//            imagesJson.append("  \"checkboxes\": [\n");
//            if (overlays != null) {
//                for (int j = 0; j < overlays.size(); j++) {
//                    var cb = overlays.get(j);
//                    if (j > 0) imagesJson.append(",\n");
//                    imagesJson.append("    {\n");
//                    imagesJson.append("      \"codigo\": ").append(jsonString(cb.getCheckboxCode())).append(",\n");
//                    imagesJson.append("      \"x\": ").append(cb.getImageX()).append(",\n");
//                    imagesJson.append("      \"y\": ").append(cb.getImageY()).append(",\n");
//                    imagesJson.append("      \"tamano\": ").append(cb.getSize()).append(",\n");
//                    imagesJson.append("      \"precio\": ").append(jsonString(cb.getPrice() > 0
//                            ? String.format("%.2f", cb.getPrice()) : "")).append(",\n");
//                    imagesJson.append("      \"estado\": ").append(jsonString(cb.getState().name())).append(",\n");
//                    imagesJson.append("      \"comentario\": ").append(jsonString(cb.getComment() != null
//                            ? cb.getComment() : "")).append("\n");
//                    imagesJson.append("    }");
//                }
//            }
//            imagesJson.append("\n  ]\n");
//            imagesJson.append("}");
//        }
//        imagesJson.append("]");
//
//        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
//        StringBuilder dataJson = new StringBuilder();
//        dataJson.append("{\n");
//        dataJson.append("  \"projectName\": ").append(jsonString(projectName)).append(",\n");
//        dataJson.append("  \"iteracion\": ").append(iteracion).append(",\n");
//        dataJson.append("  \"fechaEnvio\": ").append(jsonString(timestamp)).append(",\n");
//        dataJson.append("  \"imagenes\": ").append(imagesJson).append("\n");
//        dataJson.append("}");
//
//        // Validar JSON
//        try {
//            new com.google.gson.Gson().fromJson(dataJson.toString(), Object.class);
//        } catch (com.google.gson.JsonSyntaxException e) {
//            logger.error("[WebCatalogExporter] JSON generado NO es válido: {}", e.getMessage());
//        }
//
//        String projectSafeName = sanitizarNombre(projectName);
//        String respuestaFilename = "respuesta_" + projectSafeName + "_iteracion" + iteracion + ".json";
//        String htmlContent = generarHtmlCliente(projectName, dataJson.toString(), respuestaFilename, iteracion);
//
//        Files.writeString(outputFile, htmlContent, StandardCharsets.UTF_8);
//        logger.info("[WebCatalogExporter] HTML cliente generado: {} ({} imágenes, calidad {})",
//                outputFile.getFileName(), total, quality.maxDimension);
//    } // --- Fin de metodo exportarHtmlCliente ---
//
//
//    private ThumbnailResult generarMiniaturaBase64(Path ruta, QualityLevel quality) throws IOException {
//        if (!Files.exists(ruta)) {
//            logger.warn("[WebCatalogExporter] Imagen no encontrada: {}", ruta);
//            return new ThumbnailResult("", 0, 0);
//        }
//        int targetSize = quality.maxDimension;
//        int thumbW = 0, thumbH = 0;
//
//        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
//        try {
//            BufferedImage srcImg = ImageIO.read(ruta.toFile());
//            if (srcImg == null) return new ThumbnailResult("", 0, 0);
//            int srcW = srcImg.getWidth();
//            int srcH = srcImg.getHeight();
//            double scale = Math.min((double) targetSize / srcW, (double) targetSize / srcH);
//            thumbW = Math.max(1, (int) (srcW * scale));
//            thumbH = Math.max(1, (int) (srcH * scale));
//            BufferedImage thumb = new BufferedImage(thumbW, thumbH, BufferedImage.TYPE_INT_RGB);
//            java.awt.Graphics2D g = thumb.createGraphics();
//            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//            g.drawImage(srcImg, 0, 0, thumbW, thumbH, null);
//            g.dispose();
//            ImageIO.write(thumb, "JPEG", baos);
//        } catch (Exception e) {
//            logger.warn("[WebCatalogExporter] Error generando miniatura para {}: {}.", ruta.getFileName(), e.getMessage());
//            return new ThumbnailResult("", 0, 0);
//        }
//
//        byte[] bytes = baos.toByteArray();
//        String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
//        return new ThumbnailResult(base64, thumbW, thumbH);
//    } // --- Fin de metodo generarMiniaturaBase64 ---
//
//
//    private String generarHtmlCliente(String projectName, String dataJson, String respuestaFilename, int iteracion) {
//        return "<!DOCTYPE html>\n"
//            + "<html lang=\"es\">\n"
//            + "<head>\n"
//            + "  <meta charset=\"UTF-8\">\n"
//            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\">\n"
//            + "  <title>" + escapeHtml(projectName) + " \u2013 Cat\u00e1logo</title>\n"
//            + "  <style>\n" + getClientCss() + "  </style>\n"
//            + "</head>\n"
//            + "<body>\n"
//            + "  <script>\nvar CATALOG_DATA = " + dataJson + ";\n  </script>\n"
//            + "\n"
//            + "  <header>\n"
//            + "    <h1>" + escapeHtml(projectName) + "</h1>\n"
//            + "    <p class=\"subtitle\">Iteraci\u00f3n #" + iteracion + " \u00b7 Marca los modelos que te interesan</p>\n"
//            + "  </header>\n"
//            + "\n"
//            + "  <nav id=\"topbar\">\n"
//            + "    <span id=\"counter\"></span>\n"
//            + "  </nav>\n"
//            + "\n"
//            + "  <main id=\"gallery\" class=\"gallery\"></main>\n"
//            + "\n"
//            + "  <footer>\n"
//            + "    <div id=\"summary\"></div>\n"
//            + "    <div class=\"footer-buttons\">\n"
//            + "      <button id=\"btnCopiar\" class=\"btn-primary\">Copiar respuesta</button>\n"
//            + "      <button id=\"btnDescargar\" class=\"btn-secondary\">Descargar respuesta</button>\n"
//            + "    </div>\n"
//            + "  </footer>\n"
//            + "\n"
//            + "  <div id=\"modal\" class=\"modal hidden\">\n"
//            + "    <div class=\"modal-content\">\n"
//            + "      <button id=\"modalClose\" class=\"modal-close\">&times;</button>\n"
//            + "      <div class=\"modal-header\">\n"
//            + "        <div class=\"modal-info\">\n"
//            + "          <span class=\"modal-codigo\" id=\"modalCodigo\"></span>\n"
//            + "          <span class=\"modal-nombre\" id=\"modalNombre\"></span>\n"
//            + "          <span class=\"tristate-cb\" id=\"modalCheckPrincipal\" tabindex=\"0\">&#x25cb;</span>\n"
//            + "          <span class=\"comment-dot\" id=\"modalBubblePrincipal\" title=\"A\u00f1adir comentario\">&#x2709;</span>\n"
//            + "        </div>\n"
//            + "      </div>\n"
//            + "      <div id=\"modalComentarioDisplay\" class=\"modal-comment-display\"></div>\n"
//            + "      <div id=\"modalImageWrap\" class=\"modal-image-wrap\">\n"
//            + "        <img id=\"modalImg\" src=\"\" alt=\"\" class=\"modal-image\" draggable=\"false\">\n"
//            + "        <div id=\"checkboxOverlays\" class=\"checkbox-overlays\"></div>\n"
//            + "      </div>\n"
//            + "      <select id=\"modalEstado\" class=\"modal-estado-select\">\n"
//            + "        <option value=\"SELECTED\">Seleccionado</option>\n"
//            + "        <option value=\"UNDEFINED\">Sin marcar</option>\n"
//            + "        <option value=\"DISCARDED\">Descartado</option>\n"
//            + "      </select>\n"
//            + "      <label for=\"modalComentario\">Editar comentario:</label>\n"
//            + "      <textarea id=\"modalComentario\" rows=\"3\" placeholder=\"Escribe aqu\u00ed tu comentario...\"></textarea>\n"
//            + "    </div>\n"
//            + "  </div>\n"
//            + "\n"
//            + "  <script>\n" + getClientJs(respuestaFilename) + "  </script>\n"
//            + "</body>\n"
//            + "</html>\n";
//    } // --- Fin de metodo generarHtmlCliente ---
//
//
//    private String getClientCss() {
//        return """
//            *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
//            :root { --bg: #0f0f1a; --card-bg: #222240; --text: #f0f0f0; --accent: #3a3a6a; --selected: #2ecc71; --selected-bg: rgba(46,204,113,.12); --discarded: #e74c3c; --discarded-bg: rgba(231,76,60,.12); --undefined: #f39c12; --undefined-bg: rgba(243,156,18,.08); --text-muted: #999; --radius: 8px; --surface: #1a1a2e; }
//            body { background: var(--bg); color: var(--text); font-family: sans-serif; margin: 0; padding: 0; }
//            header { text-align: center; padding: 2rem; }
//            .gallery { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 15px; padding: 20px; max-width: 1200px; margin: 0 auto; }
//            .card { background: var(--card-bg); border-radius: 8px; border: 1px solid #3a3a6a; overflow: hidden; display: flex; flex-direction: column; position: relative; }
//            .card-header { display: flex; align-items: center; gap: .5rem; padding: .5rem; background: #1a1a2e; border-bottom: 1px solid #3a3a6a; }
//            .card-codigo { background: #000; color: #fff; padding: 1px 6px; border-radius: 3px; font-size: .7rem; font-weight: 700; white-space: nowrap; flex-shrink: 0; }
//            .card-name { font-size: .85rem; font-weight: bold; flex-grow: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer; }
//            .card-tristate { display: inline-flex; align-items: center; justify-content: center; width: 22px; height: 22px; border-radius: 4px; cursor: pointer; font-size: .9rem; font-weight: 700; user-select: none; background: var(--bg); border: 2px solid var(--accent); flex-shrink: 0; }
//            .card-tristate.state-selected { background: var(--selected-bg); color: var(--selected); border-color: var(--selected); }
//            .card-tristate.state-discarded { background: var(--discarded-bg); color: var(--discarded); border-color: var(--discarded); }
//            .card-tristate.state-undefined { background: var(--undefined-bg); color: var(--undefined); border-color: var(--undefined); }
//            .card-comment { font-size: .8rem; color: var(--text-muted); padding: .4rem .5rem; text-align: center; border-bottom: 1px solid #2a2a4a; background: #15152a; word-break: break-all; }
//            .card img { width: 100%; height: 180px; object-fit: cover; display: block; background: #000; cursor: pointer; margin-top: auto; }
//            .card-prices { font-size: .75rem; color: var(--text-muted); display: flex; gap: .4rem; flex-wrap: wrap; }
//            .card-prices .price-tag { background: #000; color: #fff; padding: 0 5px; border-radius: 2px; font-weight: 600; }
//            .badge { font-size: .7rem; padding: .1rem .5rem; border-radius: 8px; font-weight: 600; }
//            .badge-sel { background: var(--selected-bg); color: var(--selected); }
//            .badge-dis { background: var(--discarded-bg); color: var(--discarded); }
//            .badge-und { background: var(--undefined-bg); color: var(--undefined); }
//            .comment-dot { display: inline-flex; align-items: center; justify-content: center; width: 20px; height: 20px; border-radius: 50%; background: var(--accent); color: #fff; font-size: .75rem; cursor: help; }
//            #topbar { display: flex; justify-content: center; padding: .6rem 1rem; background: var(--surface); border-bottom: 1px solid #2a2a4a; }
//            #counter { color: var(--text-muted); font-size: .85rem; }
//            footer { border-top: 1px solid #2a2a4a; padding: .7rem 1.2rem; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: .5rem; background: var(--surface); }
//            #summary { font-size: .85rem; color: var(--text-muted); }
//            .footer-buttons { display: flex; gap: .5rem; }
//            .btn-primary { background: var(--selected); color: #000; border: none; padding: .5rem 1.2rem; border-radius: 6px; font-weight: 700; cursor: pointer; font-size: .85rem; }
//            .btn-primary:hover { opacity: .8; }
//            .btn-secondary { background: var(--accent); color: #fff; border: 1px solid #5a5a9a; padding: .5rem 1.2rem; border-radius: 6px; font-weight: 600; cursor: pointer; font-size: .85rem; }
//            .btn-secondary:hover { opacity: .8; }
//            .modal { position: fixed; inset: 0; background: rgba(0,0,0,0.9); display: flex; align-items: center; justify-content: center; z-index: 100; }
//            .modal.hidden { display: none !important; }
//            .modal-content { background: var(--card-bg); width: 90%; max-width: 700px; padding: 20px; border-radius: 8px; position: relative; max-height: 90vh; overflow-y: auto; border: 1px solid #3a3a5a; }
//            .modal-close { position: absolute; top: 10px; right: 15px; cursor: pointer; font-size: 2rem; color: #fff; border: none; background: none; }
//            .modal-header { margin-bottom: .4rem; }
//            .modal-info { display: flex; align-items: center; gap: .5rem; }
//            .modal-nombre { font-size: 1.05rem; font-weight: 600; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
//            .modal-codigo { display: inline-block; background: #000; color: #fff; padding: 1px 8px; border-radius: 3px; font-size: .7rem; font-weight: 700; flex-shrink: 0; }
//            .tristate-cb { display: inline-flex; align-items: center; justify-content: center; width: 26px; height: 26px; border-radius: 4px; cursor: pointer; font-size: 1.1rem; font-weight: 700; user-select: none; background: var(--bg); border: 2px solid var(--accent); flex-shrink: 0; }
//            .tristate-cb.state-selected { background: var(--selected-bg); color: var(--selected); border-color: var(--selected); }
//            .tristate-cb.state-discarded { background: var(--discarded-bg); color: var(--discarded); border-color: var(--discarded); }
//            .tristate-cb.state-undefined { background: var(--undefined-bg); color: var(--undefined); border-color: var(--undefined); }
//            .modal-image { width: 100%; height: auto; max-height: 70vh; object-fit: contain; display: block; margin: 0 auto; background: #000; }
//            .modal-image-wrap { width: 100%; max-height: 70vh; overflow: hidden; border-radius: 6px; background: #000; margin-bottom: .8rem; position: relative; cursor: grab; touch-action: none; }
//            .modal-image-wrap:active { cursor: grabbing; }
//            .checkbox-overlays { position: absolute; pointer-events: none; transform-origin: 0 0; }
//            .cb-overlay { position: absolute; display: flex; align-items: center; gap: 3px; pointer-events: auto; transform: translate(-50%, -50%); background: rgba(0,0,0,.75); border-radius: 4px; padding: 2px 5px; white-space: nowrap; cursor: pointer; border: 1px solid rgba(255,255,255,.15); user-select: none; }
//            .cb-overlay .cb-indicator { display: inline-flex; align-items: center; justify-content: center; width: 18px; height: 18px; border-radius: 3px; font-weight: 700; font-size: .7rem; }
//            .cb-overlay .cb-indicator.sel { background: var(--selected); color: #000; }
//            .cb-overlay .cb-indicator.dis { background: var(--discarded); color: #fff; }
//            .cb-overlay .cb-indicator.und { background: transparent; color: var(--undefined); border: 2px solid var(--undefined); }
//            .cb-overlay .cb-codigo { color: var(--text-muted); font-size: .65rem; }
//            .cb-overlay .cb-price { color: #fff; font-size: .7rem; font-weight: 600; }
//            .cb-overlay .cb-bubble { color: #aaa; font-size: .8rem; cursor: pointer; padding: 0 2px; }
//            .cb-overlay .cb-bubble:hover { color: #fff; }
//            .modal-comment-display { font-size: .85rem; color: var(--text-muted); padding: .4rem 0; margin-bottom: .4rem; border-bottom: 1px solid #2a2a4a; }
//            .modal-estado-select { width: 100%; background: var(--bg); color: var(--text); border: 1px solid #3a3a5a; border-radius: 5px; padding: .4rem .6rem; font-size: .85rem; outline: none; margin-bottom: .6rem; }
//            .modal-estado-select:focus { border-color: var(--accent); }
//            label { display: block; margin-bottom: .3rem; font-size: .82rem; color: var(--text-muted); }
//            textarea { width: 100%; background: var(--bg); border: 1px solid #3a3a5a; border-radius: 5px; color: var(--text); padding: .5rem; font-size: .9rem; resize: vertical; outline: none; }
//            textarea:focus { border-color: var(--accent); }
//            @media (max-width: 600px) {
//              .gallery { grid-template-columns: repeat(2, 1fr); gap: .5rem; padding: .5rem; }
//              footer { flex-direction: column; align-items: stretch; }
//              .footer-buttons { flex-direction: column; }
//              .btn-primary, .btn-secondary { width: 100%; }
//            }
//            """;
//    } // --- Fin del metodo getClientCss ---
//
//    
////    private String getClientJs(String respuestaFilename) {
////        return """
////            var data = CATALOG_DATA || { imagenes: [] };
////            var currentIndex = -1;
////
////            // --- Zoom / Pan state ---
////            var zoomScale = 1;
////            var panX = 0, panY = 0;
////            var isDragging = false;
////            var dragStartX, dragStartY;
////            var lastTouchDist = 0;
////            var lastTapTime = 0;
////
////            renderGallery();
////
////             // ===== GALLERY =====
////             function renderGallery() {
////               var gallery = document.getElementById('gallery');
////               var html = '';
////               for (var i = 0; i < data.imagenes.length; i++) {
////                 var img = data.imagenes[i];
////                 var estado = img.estado || 'UNDEFINED';
////                 var tristateClass = estado === 'SELECTED' ? 'state-selected' : (estado === 'DISCARDED' ? 'state-discarded' : 'state-undefined');
////                 var tristateChar = estado === 'SELECTED' ? escHtml('\\u2713') : (estado === 'DISCARDED' ? escHtml('\\u2717') : escHtml('\\u25cb'));
////                 var codeSpan = img.codigo ? '<span class=\\"card-codigo\\">' + escHtml(img.codigo) + '</span>' : '';
////                 
////                 var prices = [];
////                 if (img.checkboxes) {
////                   for (var j = 0; j < img.checkboxes.length; j++) {
////                     if (img.checkboxes[j].precio) prices.push(img.checkboxes[j].precio);
////                   }
////                 }
////
////                 var commentHtml = '';
////                 var hasGeneralComment = img.comentario && img.comentario.trim();
////                 if (hasGeneralComment || prices.length > 0) {
////                   var prHtml = '';
////                   if (prices.length > 0) {
////                     prHtml = '<div class=\\"card-prices\\" style=\\"display:flex; gap:.4rem; justify-content:center; margin-bottom:4px;\\">' 
////                       + prices.map(function(p) { return '<span class=\\"price-tag\\">' + escHtml(p) + ' ' + escHtml('\\u20ac') + '</span>'; }).join('') 
////                       + '</div>';
////                   }
////                   var commText = hasGeneralComment ? '<div style=\\"font-weight:600; font-size:.8rem;\\">' + escHtml(img.comentario) + '</div>' : '';
////                   commentHtml = '<div class=\\"card-comment\\">' + prHtml + commText + '</div>';
////                 }
////
////                 html += '<div class=\\"card ' + estado + '\\" data-index=\\"' + i + '\\">'
////                   + '  <div class=\\"card-header\\">'
////                   + '    ' + codeSpan
////                   + '    <span class=\\"card-name\\" onclick=\\"openModal(' + i + ')\\">' + escHtml(img.nombre) + '</span>'
////                   + '    <span class=\\"card-tristate ' + tristateClass + '\\" onclick=\\"toggleCardState(' + i + ', event)\\" tabindex=\\"0\\">' + tristateChar + '</span>'
////                   + '  </div>'
////                   + '  ' + commentHtml
////                   + '  <img src=\\"' + img.miniatura + '\\" alt=\\"' + escHtml(img.nombre) + '\\" loading=\\"lazy\\"'
////                   + '    onclick=\\"openModal(' + i + ')\\"'
////                   + '    onerror=\\"this.src=' + "'data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' width='200' height='200'><rect width='200' height='200' fill='%%23222'/><text x='50%%' y='50%%' fill='%%23666' text-anchor='middle' dy='.3em'>Sin imagen</text></svg>'" + '\\">'
////                   + '</div>';
////               }
////               gallery.innerHTML = html;
////
////               // Click en card (fuera de img/name/tristate) abre modal
////               gallery.querySelectorAll('.card').forEach(function(card) {
////                 card.addEventListener('click', function(e) {
////                   if (e.target.closest('img') || e.target.closest('.card-name') || e.target.closest('.card-tristate')) return;
////                   var idx = parseInt(card.dataset.index);
////                   if (!isNaN(idx)) openModal(idx);
////                 });
////               });
////
////               updateCounter();
////             }
////
////             function toggleCardState(i, e) {
////               if (e) e.stopPropagation();
////               var img = data.imagenes[i];
////               var estados = ['SELECTED', 'DISCARDED', 'UNDEFINED'];
////               var currentIdx = estados.indexOf(img.estado || 'UNDEFINED');
////                var nextIdx = (currentIdx + 1) %% estados.length;
////               img.estado = estados[nextIdx];
////               renderGallery();
////             }
////
////            function escHtml(s) { return (s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }
////
////            function updateCounter() {
////              var sel = data.imagenes.filter(function(i) { return i.estado === 'SELECTED'; }).length;
////              var dis = data.imagenes.filter(function(i) { return i.estado === 'DISCARDED'; }).length;
////              var und = data.imagenes.filter(function(i) { return i.estado === 'UNDEFINED'; }).length;
////              document.getElementById('counter').textContent = 'Total: ' + data.imagenes.length + ' | \\u2713 ' + sel + ' \\u2717 ' + dis + ' \\u25cb ' + und;
////              document.getElementById('summary').textContent = '\\u2713 ' + sel + ' seleccionadas \\u00b7 \\u2717 ' + dis + ' descartadas \\u00b7 \\u25cb ' + und + ' sin marcar';
////            }
////
////            // ===== MODAL =====
////            function openModal(index) {
////              currentIndex = index;
////              var img = data.imagenes[index];
////              document.getElementById('modalCodigo').textContent = img.codigo || '';
////              document.getElementById('modalNombre').textContent = img.nombre;
////              document.getElementById('modalImg').src = img.miniatura;
////              document.getElementById('modalEstado').value = img.estado || 'UNDEFINED';
////              setTristate(document.getElementById('modalCheckPrincipal'), img.estado || 'UNDEFINED');
////              var bubbleEl = document.getElementById('modalBubblePrincipal');
////              bubbleEl.title = (img.comentario && img.comentario.trim()) ? img.comentario : 'A\u00f1adir comentario';
////              bubbleEl.style.opacity = (img.comentario && img.comentario.trim()) ? '1' : '.5';
////              // Show existing comment in display area
////              var commentDisplay = document.getElementById('modalComentarioDisplay');
////              if (img.comentario && img.comentario.trim()) {
////                commentDisplay.textContent = 'Comentario: ' + img.comentario;
////                commentDisplay.style.display = 'block';
////              } else {
////                commentDisplay.textContent = '';
////                commentDisplay.style.display = 'none';
////              }
////              document.getElementById('modalComentario').value = img.comentario || '';
////              resetZoom();
////
////              // Render overlays on the image
////              var overlayContainer = document.getElementById('checkboxOverlays');
////              overlayContainer.innerHTML = '';
////              document.getElementById('modalImg').onload = function() {
////                sizeOverlayContainer();
////                if (img.checkboxes && img.checkboxes.length > 0) {
////                  positionOverlays(overlayContainer, img);
////                }
////              };
////              if (img.checkboxes && img.checkboxes.length > 0) {
////                // If already loaded
////                if (document.getElementById('modalImg').complete && document.getElementById('modalImg').naturalWidth > 0) {
////                  sizeOverlayContainer();
////                  positionOverlays(overlayContainer, img);
////                }
////              } else {
////                // Even without checkboxes, size the container
////                if (document.getElementById('modalImg').complete && document.getElementById('modalImg').naturalWidth > 0) {
////                  sizeOverlayContainer();
////                }
////              }
////
////              document.getElementById('modal').classList.remove('hidden');
////            }
////
////            function sizeOverlayContainer() {
////              var modalImg = document.getElementById('modalImg');
////              var ov = document.getElementById('checkboxOverlays');
////              ov.style.width = modalImg.clientWidth + 'px';
////              ov.style.height = modalImg.clientHeight + 'px';
////            }
////
////            function positionOverlays(container, imgData) {
////              var modalImg = document.getElementById('modalImg');
////              var scale = modalImg.clientWidth / (imgData.ancho || modalImg.naturalWidth || 1);
////              container.innerHTML = '';
////              for (var j = 0; j < imgData.checkboxes.length; j++) {
////                var cb = imgData.checkboxes[j];
////                var x = (cb.x || 0) * scale;
////                var y = (cb.y || 0) * scale;
////                var estado = cb.estado || 'UNDEFINED';
////                var indicatorClass = estado === 'SELECTED' ? 'sel' : (estado === 'DISCARDED' ? 'dis' : 'und');
////                var indicatorText = estado === 'SELECTED' ? '\\u2713' : (estado === 'DISCARDED' ? '\\u2717' : '\\u25cb');
////                var bubbleHtml = cb.comentario && cb.comentario.trim()
////                  ? '<span class=\\"cb-bubble\\" title=\\"' + escHtml(cb.comentario) + '\\">\\u2709</span>'
////                  : '<span class=\\"cb-bubble\\" style=\\"opacity:.5\\" title=\\"A\\u00f1adir comentario\\">\\u2709</span>';
////                var priceHtml = cb.precio ? '<span class=\\"cb-price\\">' + escHtml(cb.precio) + '</span>' : '';
////                var codigoHtml = cb.codigo ? '<span class=\\"cb-codigo\\">' + escHtml(cb.codigo) + '</span>' : '';
////                var overlay = document.createElement('div');
////                overlay.className = 'cb-overlay';
////                overlay.style.left = x + 'px';
////                overlay.style.top = y + 'px';
////                overlay.innerHTML = '<span class=\\"cb-indicator ' + indicatorClass + '\\">' + indicatorText + '</span>'
////                  + codigoHtml
////                  + priceHtml
////                  + bubbleHtml;
////                container.appendChild(overlay);
////
////                // Click on overlay (or indicator) cycles state
////                overlay.addEventListener('click', function(e) {
////                  if (e.target.closest('.cb-bubble')) return;
////                  var idx = Array.prototype.indexOf.call(container.children, this);
////                  if (idx >= 0 && data.imagenes[currentIndex].checkboxes[idx]) {
////                    var cbData = data.imagenes[currentIndex].checkboxes[idx];
////                    var next = cycleState(cbData.estado || 'UNDEFINED');
////                    cbData.estado = next;
////                    positionOverlays(container, data.imagenes[currentIndex]);
////                    guardarModal();
////                  }
////                });
////                // Bubble click -> prompt for comment
////                var bubble = overlay.querySelector('.cb-bubble');
////                bubble.addEventListener('click', function(e) {
////                  e.stopPropagation();
////                  var overlayEl = this.closest('.cb-overlay');
////                  var idx = Array.prototype.indexOf.call(container.children, overlayEl);
////                  if (idx >= 0 && data.imagenes[currentIndex].checkboxes[idx]) {
////                    var cbData = data.imagenes[currentIndex].checkboxes[idx];
////                    var nuevo = prompt('Comentario para ' + (cbData.codigo || 'checkbox') + ':', cbData.comentario || '');
////                    if (nuevo !== null) {
////                      cbData.comentario = nuevo;
////                      positionOverlays(container, data.imagenes[currentIndex]);
////                    }
////                  }
////                });
////              }
////            }
////
////            function cycleState(current) {
////              if (current === 'SELECTED') return 'DISCARDED';
////              if (current === 'DISCARDED') return 'UNDEFINED';
////              return 'SELECTED';
////            }
////
////            function setTristate(el, state) {
////              el.className = 'tristate-cb state-' + state.toLowerCase();
////              el.textContent = state === 'SELECTED' ? '\\u2713' : (state === 'DISCARDED' ? '\\u2717' : '\\u25cb');
////            }
////
////            // Modal close
////            document.getElementById('modalClose').addEventListener('click', function() {
////              guardarModal();
////              document.getElementById('modal').classList.add('hidden');
////            });
////            document.getElementById('modal').addEventListener('click', function(e) {
////              if (e.target === document.getElementById('modal')) {
////                guardarModal();
////                document.getElementById('modal').classList.add('hidden');
////              }
////            });
////            document.getElementById('modalEstado').addEventListener('change', function() {
////              if (currentIndex >= 0) {
////                data.imagenes[currentIndex].estado = this.value;
////                setTristate(document.getElementById('modalCheckPrincipal'), this.value);
////                renderGallery();
////              }
////            });
////            document.getElementById('modalCheckPrincipal').addEventListener('click', function() {
////              if (currentIndex >= 0) {
////                var next = cycleState(data.imagenes[currentIndex].estado || 'UNDEFINED');
////                data.imagenes[currentIndex].estado = next;
////                setTristate(this, next);
////                document.getElementById('modalEstado').value = next;
////                renderGallery();
////              }
////            });
////            document.getElementById('modalBubblePrincipal').addEventListener('click', function(e) {
////              e.stopPropagation();
////              if (currentIndex < 0) return;
////              var img = data.imagenes[currentIndex];
////              var nuevo = prompt('Comentario general para ' + (img.codigo || img.nombre) + ':', img.comentario || '');
////              if (nuevo !== null) {
////                img.comentario = nuevo;
////                this.title = nuevo.trim() ? nuevo : 'A\u00f1adir comentario';
////                this.style.opacity = nuevo.trim() ? '1' : '.5';
////                var display = document.getElementById('modalComentarioDisplay');
////                if (nuevo.trim()) {
////                  display.textContent = 'Comentario: ' + nuevo;
////                  display.style.display = 'block';
////                } else {
////                  display.textContent = '';
////                  display.style.display = 'none';
////                }
////                document.getElementById('modalComentario').value = nuevo;
////              }
////            });
////            document.getElementById('modalComentario').addEventListener('input', function(e) {
////              if (currentIndex >= 0) {
////                data.imagenes[currentIndex].comentario = e.target.value;
////                var display = document.getElementById('modalComentarioDisplay');
////                if (e.target.value.trim()) {
////                  display.textContent = 'Comentario: ' + e.target.value;
////                  display.style.display = 'block';
////                } else {
////                  display.textContent = '';
////                  display.style.display = 'none';
////                }
////              }
////            });
////
////            function guardarModal() {
////              if (currentIndex < 0) return;
////              renderGallery();
////            }
////
////            // ===== ZOOM & PAN (modal image) =====
////            function resetZoom() {
////              zoomScale = 1;
////              panX = 0; panY = 0;
////              applyTransform();
////              document.getElementById('checkboxOverlays').style.transform = 'scale(1)';
////              document.getElementById('checkboxOverlays').style.left = '0px';
////              document.getElementById('checkboxOverlays').style.top = '0px';
////            }
////
////            function applyTransform() {
////              var img = document.getElementById('modalImg');
////              img.style.transform = 'scale(' + zoomScale + ')';
////              img.style.left = panX + 'px';
////              img.style.top = panY + 'px';
////              // Scale overlays too
////              var ov = document.getElementById('checkboxOverlays');
////              ov.style.transform = 'scale(' + zoomScale + ')';
////              ov.style.left = panX + 'px';
////              ov.style.top = panY + 'px';
////            }
////
////            // Mouse wheel zoom
////            document.getElementById('modalImageWrap').addEventListener('wheel', function(e) {
////              if (!document.getElementById('modalImg').src) return;
////              e.preventDefault();
////              var delta = e.deltaY > 0 ? -0.15 : 0.15;
////              zoomScale = Math.max(0.3, Math.min(6, zoomScale + delta));
////              applyTransform();
////            }, { passive: false });
////
////            // Mouse drag pan
////            document.getElementById('modalImageWrap').addEventListener('mousedown', function(e) {
////              if (e.button !== 0) return;
////              isDragging = true;
////              dragStartX = e.clientX - panX;
////              dragStartY = e.clientY - panY;
////              e.preventDefault();
////            });
////            document.addEventListener('mousemove', function(e) {
////              if (!isDragging) return;
////              panX = e.clientX - dragStartX;
////              panY = e.clientY - dragStartY;
////              applyTransform();
////            });
////            document.addEventListener('mouseup', function() { isDragging = false; });
////
////            // Touch support
////            document.getElementById('modalImageWrap').addEventListener('touchstart', function(e) {
////              if (e.touches.length === 1) {
////                var t = e.touches[0];
////                dragStartX = t.clientX - panX;
////                dragStartY = t.clientY - panY;
////                var now = Date.now();
////                if (now - lastTapTime < 300) {
////                  e.preventDefault();
////                  zoomScale = zoomScale > 1.5 ? 1 : 3;
////                  applyTransform();
////                  lastTapTime = 0;
////                } else {
////                  lastTapTime = now;
////                }
////              } else if (e.touches.length === 2) {
////                e.preventDefault();
////                var dx = e.touches[0].clientX - e.touches[1].clientX;
////                var dy = e.touches[0].clientY - e.touches[1].clientY;
////                lastTouchDist = Math.sqrt(dx*dx + dy*dy);
////              }
////            }, { passive: false });
////            document.getElementById('modalImageWrap').addEventListener('touchmove', function(e) {
////              if (e.touches.length === 1) {
////                var t = e.touches[0];
////                panX = t.clientX - dragStartX;
////                panY = t.clientY - dragStartY;
////                applyTransform();
////              } else if (e.touches.length === 2) {
////                e.preventDefault();
////                var dx = e.touches[0].clientX - e.touches[1].clientX;
////                var dy = e.touches[0].clientY - e.touches[1].clientY;
////                var dist = Math.sqrt(dx*dx + dy*dy);
////                if (lastTouchDist > 0) {
////                  zoomScale = Math.max(0.3, Math.min(6, zoomScale * (dist / lastTouchDist)));
////                  applyTransform();
////                }
////                lastTouchDist = dist;
////              }
////            }, { passive: false });
////            document.getElementById('modalImageWrap').addEventListener('touchend', function() { lastTouchDist = 0; });
////
////            // ===== BUTTONS =====
////            document.getElementById('btnCopiar').addEventListener('click', function() {
////              var respuesta = buildResponse();
////              var text = JSON.stringify(respuesta, null, 2);
////              if (navigator.clipboard && navigator.clipboard.writeText) {
////                navigator.clipboard.writeText(text).then(function() {
////                  showToast('Respuesta copiada al portapapeles');
////                }).catch(function() {
////                  fallbackCopy(text);
////                });
////              } else {
////                fallbackCopy(text);
////              }
////            });
////
////            function fallbackCopy(text) {
////              var ta = document.createElement('textarea');
////              ta.value = text;
////              ta.style.position = 'fixed'; ta.style.opacity = '0';
////              document.body.appendChild(ta);
////              ta.select();
////              try { document.execCommand('copy'); showToast('Respuesta copiada'); } catch(e) { alert('No se pudo copiar. Selecciona el texto manualmente.'); }
////              document.body.removeChild(ta);
////            }
////
////            document.getElementById('btnDescargar').addEventListener('click', function() {
////              var respuesta = buildResponse();
////              var json = JSON.stringify(respuesta, null, 2);
////              var blob = new Blob([json], {type: 'application/json'});
////              var a = document.createElement('a');
////              a.href = URL.createObjectURL(blob);
////              a.download = '%s';
////              a.click();
////              URL.revokeObjectURL(a.href);
////            });
////
////            function buildResponse() {
////              guardarModal();
////              return {
////                projectName: data.projectName,
////                iteracion: data.iteracion,
////                fechaEnvio: data.fechaEnvio,
////                fechaRespuesta: new Date().toISOString(),
////                respuestas: data.imagenes.map(function(img) {
////                  return {
////                    id: img.id,
////                    codigo: img.codigo || '',
////                    estado: img.estado || 'DISCARDED',
////                    comentario: img.comentario || '',
////                    checkboxes: (img.checkboxes || []).map(function(cb) {
////                      return { codigo: cb.codigo || '', estado: cb.estado || 'UNDEFINED' };
////                    })
////                  };
////                })
////              };
////            }
////
////            function showToast(msg) {
////              var t = document.createElement('div');
////              t.textContent = msg;
////              t.style.cssText = 'position:fixed;bottom:80px;left:50%%;transform:translateX(-50%%);background:#333;color:#fff;padding:8px 16px;border-radius:6px;font-size:.85rem;z-index:200;opacity:0;transition:opacity .3s';
////              document.body.appendChild(t);
////              requestAnimationFrame(function() { t.style.opacity = '1'; });
////              setTimeout(function() { t.style.opacity = '0'; setTimeout(function() { t.remove(); }, 300); }, 2000);
////            }
////
////            """.formatted(respuestaFilename);
////    } // --- Fin de metodo getClientJs ---
//    
//    
//    private String getClientJs(String respuestaFilename) {
//        return """
//            var data = CATALOG_DATA || { imagenes: [] };
//            var currentIndex = -1;
//
//            // --- Zoom / Pan state ---
//            var zoomScale = 1;
//            var panX = 0, panY = 0;
//            var isDragging = false;
//            var dragStartX = 0, dragStartY = 0;
//            var lastTouchDist = 0;
//            var lastTapTime = 0;
//
//            renderGallery();
//
//             // ===== GALLERY =====
//             function renderGallery() {
//               var gallery = document.getElementById('gallery');
//               var html = '';
//               for (var i = 0; i < data.imagenes.length; i++) {
//                 var img = data.imagenes[i];
//                 var estado = img.estado || 'UNDEFINED';
//                 var tristateClass = estado === 'SELECTED' ? 'state-selected' : (estado === 'DISCARDED' ? 'state-discarded' : 'state-undefined');
//                 var tristateChar = estado === 'SELECTED' ? escHtml('\\u2713') : (estado === 'DISCARDED' ? escHtml('\\u2717') : escHtml('\\u25cb'));
//                 var codeSpan = img.codigo ? '<span class=\\"card-codigo\\">' + escHtml(img.codigo) + '</span>' : '';
//                 
//                 var prices = [];
//                 if (img.checkboxes) {
//                   for (var j = 0; j < img.checkboxes.length; j++) {
//                     if (img.checkboxes[j].precio) prices.push(img.checkboxes[j].precio);
//                   }
//                 }
//
//                 var commentHtml = '';
//                 var hasGeneralComment = img.comentario && img.comentario.trim();
//                 if (hasGeneralComment || prices.length > 0) {
//                   var prHtml = '';
//                   if (prices.length > 0) {
//                     prHtml = '<div class=\\"card-prices\\" style=\\"display:flex; gap:.4rem; justify-content:center; margin-bottom:4px;\\">' 
//                       + prices.map(function(p) { return '<span class=\\"price-tag\\">' + escHtml(p) + ' ' + escHtml('\\u20ac') + '</span>'; }).join('') 
//                       + '</div>';
//                   }
//                   var commText = hasGeneralComment ? '<div style=\\"font-weight:600; font-size:.8rem;\\">' + escHtml(img.comentario) + '</div>' : '';
//                   commentHtml = '<div class=\\"card-comment\\">' + prHtml + commText + '</div>';
//                 }
//
//                 html += '<div class=\\"card ' + estado + '\\" data-index=\\"' + i + '\\">'
//                   + '  <div class=\\"card-header\\">'
//                   + '    ' + codeSpan
//                   + '    <span class=\\"card-name\\" onclick=\\"openModal(' + i + ')\\">' + escHtml(img.nombre) + '</span>'
//                   + '    <span class=\\"card-tristate ' + tristateClass + '\\" onclick=\\"toggleCardState(' + i + ', event)\\" tabindex=\\"0\\">' + tristateChar + '</span>'
//                   + '  </div>'
//                   + '  ' + commentHtml
//                   + '  <img src=\\"' + img.miniatura + '\\" alt=\\"' + escHtml(img.nombre) + '\\" loading=\\"lazy\\"'
//                   + '    onclick=\\"openModal(' + i + ')\\"'
//                   + '    onerror=\\"this.src=' + "'data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' width='200' height='200'><rect width='200' height='200' fill='%%23222'/><text x='50%%' y='50%%' fill='%%23666' text-anchor='middle' dy='.3em'>Sin imagen</text></svg>'" + '\\">'
//                   + '</div>';
//               }
//               gallery.innerHTML = html;
//
//               // Click en card (fuera de img/name/tristate) abre modal
//               gallery.querySelectorAll('.card').forEach(function(card) {
//                 card.addEventListener('click', function(e) {
//                   if (e.target.closest('img') || e.target.closest('.card-name') || e.target.closest('.card-tristate')) return;
//                   var idx = parseInt(card.dataset.index);
//                   if (!isNaN(idx)) openModal(idx);
//                 });
//               });
//
//               updateCounter();
//             }
//
//             function toggleCardState(i, e) {
//               if (e) e.stopPropagation();
//               var img = data.imagenes[i];
//               var estados = ['SELECTED', 'DISCARDED', 'UNDEFINED'];
//               var currentIdx = estados.indexOf(img.estado || 'UNDEFINED');
//                var nextIdx = (currentIdx + 1) %% estados.length;
//               img.estado = estados[nextIdx];
//               renderGallery();
//             }
//
//            function escHtml(s) { return (s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }
//
//            function updateCounter() {
//              var sel = data.imagenes.filter(function(i) { return i.estado === 'SELECTED'; }).length;
//              var dis = data.imagenes.filter(function(i) { return i.estado === 'DISCARDED'; }).length;
//              var und = data.imagenes.filter(function(i) { return i.estado === 'UNDEFINED'; }).length;
//              document.getElementById('counter').textContent = 'Total: ' + data.imagenes.length + ' | \\u2713 ' + sel + ' \\u2717 ' + dis + ' \\u25cb ' + und;
//              document.getElementById('summary').textContent = '\\u2713 ' + sel + ' seleccionadas \\u00b7 \\u2717 ' + dis + ' descartadas \\u00b7 \\u25cb ' + und + ' sin marcar';
//            }
//
//            // ===== MODAL =====
//            function openModal(index) {
//              currentIndex = index;
//              var img = data.imagenes[index];
//              document.getElementById('modalCodigo').textContent = img.codigo || '';
//              document.getElementById('modalNombre').textContent = img.nombre;
//              document.getElementById('modalImg').src = img.miniatura;
//              document.getElementById('modalEstado').value = img.estado || 'UNDEFINED';
//              setTristate(document.getElementById('modalCheckPrincipal'), img.estado || 'UNDEFINED');
//              var bubbleEl = document.getElementById('modalBubblePrincipal');
//              bubbleEl.title = (img.comentario && img.comentario.trim()) ? img.comentario : 'A\u00f1adir comentario';
//              bubbleEl.style.opacity = (img.comentario && img.comentario.trim()) ? '1' : '.5';
//              // Show existing comment in display area
//              var commentDisplay = document.getElementById('modalComentarioDisplay');
//              if (img.comentario && img.comentario.trim()) {
//                commentDisplay.textContent = 'Comentario: ' + img.comentario;
//                commentDisplay.style.display = 'block';
//              } else {
//                commentDisplay.textContent = '';
//                commentDisplay.style.display = 'none';
//              }
//              document.getElementById('modalComentario').value = img.comentario || '';
//              resetZoom();
//
//              // Render overlays on the image
//              var overlayContainer = document.getElementById('checkboxOverlays');
//              overlayContainer.innerHTML = '';
//              document.getElementById('modalImg').onload = function() {
//                sizeOverlayContainer();
//                if (img.checkboxes && img.checkboxes.length > 0) {
//                  positionOverlays(overlayContainer, img);
//                }
//              };
//              if (img.checkboxes && img.checkboxes.length > 0) {
//                // If already loaded
//                if (document.getElementById('modalImg').complete && document.getElementById('modalImg').naturalWidth > 0) {
//                  sizeOverlayContainer();
//                  positionOverlays(overlayContainer, img);
//                }
//              } else {
//                // Even without checkboxes, size the container
//                if (document.getElementById('modalImg').complete && document.getElementById('modalImg').naturalWidth > 0) {
//                  sizeOverlayContainer();
//                }
//              }
//
//              document.getElementById('modal').classList.remove('hidden');
//            }
//
//            function sizeOverlayContainer() {
//              var modalImg = document.getElementById('modalImg');
//              var ov = document.getElementById('checkboxOverlays');
//              ov.style.width = modalImg.clientWidth + 'px';
//              ov.style.height = modalImg.clientHeight + 'px';
//            }
//
//            function positionOverlays(container, imgData) {
//              var modalImg = document.getElementById('modalImg');
//              var scale = modalImg.clientWidth / (imgData.ancho || modalImg.naturalWidth || 1);
//              container.innerHTML = '';
//              for (var j = 0; j < imgData.checkboxes.length; j++) {
//                var cb = imgData.checkboxes[j];
//                var x = (cb.x || 0) * scale;
//                var y = (cb.y || 0) * scale;
//                var estado = cb.estado || 'UNDEFINED';
//                var indicatorClass = estado === 'SELECTED' ? 'sel' : (estado === 'DISCARDED' ? 'dis' : 'und');
//                var indicatorText = estado === 'SELECTED' ? '\\u2713' : (estado === 'DISCARDED' ? '\\u2717' : '\\u25cb');
//                var bubbleHtml = cb.comentario && cb.comentario.trim()
//                  ? '<span class=\\"cb-bubble\\" title=\\"' + escHtml(cb.comentario) + '\\">\\u2709</span>'
//                  : '<span class=\\"cb-bubble\\" style=\\"opacity:.5\\" title=\\"A\\u00f1adir comentario\\">\\u2709</span>';
//                var priceHtml = cb.precio ? '<span class=\\"cb-price\\">' + escHtml(cb.precio) + '</span>' : '';
//                var codigoHtml = cb.codigo ? '<span class=\\"cb-codigo\\">' + escHtml(cb.codigo) + '</span>' : '';
//                var overlay = document.createElement('div');
//                overlay.className = 'cb-overlay';
//                overlay.style.left = x + 'px';
//                overlay.style.top = y + 'px';
//                overlay.innerHTML = '<span class=\\"cb-indicator ' + indicatorClass + '\\">' + indicatorText + '</span>'
//                  + codigoHtml
//                  + priceHtml
//                  + bubbleHtml;
//                container.appendChild(overlay);
//
//                // Click on overlay (or indicator) cycles state
//                overlay.addEventListener('click', function(e) {
//                  if (e.target.closest('.cb-bubble')) return;
//                  var idx = Array.prototype.indexOf.call(container.children, this);
//                  if (idx >= 0 && data.imagenes[currentIndex].checkboxes[idx]) {
//                    var cbData = data.imagenes[currentIndex].checkboxes[idx];
//                    var next = cycleState(cbData.estado || 'UNDEFINED');
//                    cbData.estado = next;
//                    positionOverlays(container, data.imagenes[currentIndex]);
//                    guardarModal();
//                  }
//                });
//                // Bubble click -> prompt for comment
//                var bubble = overlay.querySelector('.cb-bubble');
//                bubble.addEventListener('click', function(e) {
//                  e.stopPropagation();
//                  var overlayEl = this.closest('.cb-overlay');
//                  var idx = Array.prototype.indexOf.call(container.children, overlayEl);
//                  if (idx >= 0 && data.imagenes[currentIndex].checkboxes[idx]) {
//                    var cbData = data.imagenes[currentIndex].checkboxes[idx];
//                    var nuevo = prompt('Comentario para ' + (cbData.codigo || 'checkbox') + ':', cbData.comentario || '');
//                    if (nuevo !== null) {
//                      cbData.comentario = nuevo;
//                      positionOverlays(container, data.imagenes[currentIndex]);
//                    }
//                  }
//                });
//              }
//            }
//
//            function cycleState(current) {
//              if (current === 'SELECTED') return 'DISCARDED';
//              if (current === 'DISCARDED') return 'UNDEFINED';
//              return 'SELECTED';
//            }
//
//            function setTristate(el, state) {
//              el.className = 'tristate-cb state-' + state.toLowerCase();
//              el.textContent = state === 'SELECTED' ? '\\u2713' : (state === 'DISCARDED' ? '\\u2717' : '\\u25cb');
//            }
//
//            // Modal close
//            document.getElementById('modalClose').addEventListener('click', function() {
//              guardarModal();
//              document.getElementById('modal').classList.add('hidden');
//            });
//            document.getElementById('modal').addEventListener('click', function(e) {
//              if (e.target === document.getElementById('modal')) {
//                guardarModal();
//                document.getElementById('modal').classList.add('hidden');
//              }
//            });
//            document.getElementById('modalEstado').addEventListener('change', function() {
//              if (currentIndex >= 0) {
//                data.imagenes[currentIndex].estado = this.value;
//                setTristate(document.getElementById('modalCheckPrincipal'), this.value);
//                renderGallery();
//              }
//            });
//            document.getElementById('modalCheckPrincipal').addEventListener('click', function() {
//              if (currentIndex >= 0) {
//                var next = cycleState(data.imagenes[currentIndex].estado || 'UNDEFINED');
//                data.imagenes[currentIndex].estado = next;
//                setTristate(this, next);
//                document.getElementById('modalEstado').value = next;
//                renderGallery();
//              }
//            });
//            document.getElementById('modalBubblePrincipal').addEventListener('click', function(e) {
//              e.stopPropagation();
//              if (currentIndex < 0) return;
//              var img = data.imagenes[currentIndex];
//              var nuevo = prompt('Comentario general para ' + (img.codigo || img.nombre) + ':', img.comentario || '');
//              if (nuevo !== null) {
//                img.comentario = nuevo;
//                this.title = nuevo.trim() ? nuevo : 'A\u00f1adir comentario';
//                this.style.opacity = nuevo.trim() ? '1' : '.5';
//                var display = document.getElementById('modalComentarioDisplay');
//                if (nuevo.trim()) {
//                  display.textContent = 'Comentario: ' + nuevo;
//                  display.style.display = 'block';
//                } else {
//                  display.textContent = '';
//                  display.style.display = 'none';
//                }
//                document.getElementById('modalComentario').value = nuevo;
//              }
//            });
//            document.getElementById('modalComentario').addEventListener('input', function(e) {
//              if (currentIndex >= 0) {
//                data.imagenes[currentIndex].comentario = e.target.value;
//                var display = document.getElementById('modalComentarioDisplay');
//                if (e.target.value.trim()) {
//                  display.textContent = 'Comentario: ' + e.target.value;
//                  display.style.display = 'block';
//                } else {
//                  display.textContent = '';
//                  display.style.display = 'none';
//                }
//              }
//            });
//
//            function guardarModal() {
//              if (currentIndex < 0) return;
//              renderGallery();
//            }
//
//
//
/////*
////            // ===== ZOOM & PAN (modal image) =====
////            function resetZoom() {
////              zoomScale = 1;
////              panX = 0; 
////              panY = 0;
////              applyTransform();
////            }
////
////            function applyTransform() {
////              var img = document.getElementById('modalImg');
////              var ov = document.getElementById('checkboxOverlays');
////              
////              // Se aplica Translate en lugar de left/top para evitar problemas de offset
////              var transformStr = 'translate(' + panX + 'px, ' + panY + 'px) scale(' + zoomScale + ')';
////              img.style.transform = transformStr;
////              ov.style.transform = transformStr;
////              
////              // Reseteamos left/top por si estaban establecidos
////              img.style.left = '0px';
////              img.style.top = '0px';
////              ov.style.left = '0px';
////              ov.style.top = '0px';
////            }
////
////            // Mouse wheel zoom (PC)
////            document.getElementById('modalImageWrap').addEventListener('wheel', function(e) {
////              if (!document.getElementById('modalImg').src) return;
////              e.preventDefault();
////              var delta = e.deltaY > 0 ? -0.15 : 0.15;
////              zoomScale = Math.max(0.3, Math.min(6, zoomScale + delta));
////              applyTransform();
////            }, { passive: false });
////
////            // Mouse drag pan (PC)
////            document.getElementById('modalImageWrap').addEventListener('mousedown', function(e) {
////              if (e.button !== 0) return;
////              isDragging = true;
////              dragStartX = e.clientX - panX;
////              dragStartY = e.clientY - panY;
////              e.preventDefault();
////            });
////            
////            document.addEventListener('mousemove', function(e) {
////              if (!isDragging) return;
////              panX = e.clientX - dragStartX;
////              panY = e.clientY - dragStartY;
////              applyTransform();
////            });
////            
////            document.addEventListener('mouseup', function() { 
////              isDragging = false; 
////            });
////
////            // Touch support (Pan, Pinch Zoom, Double Tap para móviles)
////            document.getElementById('modalImageWrap').addEventListener('touchstart', function(e) {
////              if (e.touches.length === 1) {
////                var t = e.touches[0];
////                dragStartX = t.clientX - panX;
////                dragStartY = t.clientY - panY;
////                
////                var now = Date.now();
////                if (now - lastTapTime < 300) {
////                  e.preventDefault();
////                  if (zoomScale > 1.5) {
////                    // Si ya hay zoom, doble tap resetea al centro
////                    zoomScale = 1; panX = 0; panY = 0;
////                  } else {
////                    // Doble tap hace zoom in
////                    zoomScale = 3;
////                  }
////                  applyTransform();
////                  lastTapTime = 0;
////                } else {
////                  lastTapTime = now;
////                }
////              } else if (e.touches.length === 2) {
////                e.preventDefault();
////                var dx = e.touches[0].clientX - e.touches[1].clientX;
////                var dy = e.touches[0].clientY - e.touches[1].clientY;
////                lastTouchDist = Math.sqrt(dx*dx + dy*dy);
////              }
////            }, { passive: false });
////
////            document.getElementById('modalImageWrap').addEventListener('touchmove', function(e) {
////              if (e.touches.length === 1) {
////                // Arrastre con un dedo
////                var t = e.touches[0];
////                panX = t.clientX - dragStartX;
////                panY = t.clientY - dragStartY;
////                applyTransform();
////                e.preventDefault(); // Evita que la pantalla completa del movil haga scroll
////              } else if (e.touches.length === 2) {
////                // Pinch to zoom con 2 dedos
////                e.preventDefault();
////                var dx = e.touches[0].clientX - e.touches[1].clientX;
////                var dy = e.touches[0].clientY - e.touches[1].clientY;
////                var dist = Math.sqrt(dx*dx + dy*dy);
////                if (lastTouchDist > 0) {
////                  zoomScale = Math.max(0.3, Math.min(6, zoomScale * (dist / lastTouchDist)));
////                  applyTransform();
////                }
////                lastTouchDist = dist;
////              }
////            }, { passive: false });
////            
////            document.getElementById('modalImageWrap').addEventListener('touchend', function() { 
////              lastTouchDist = 0; 
////            });
////
////*/
//
//        	// ===== ZOOM & PAN (modal image) =====
//            var zoomScale = 1;
//            var panX = 0, panY = 0;
//            var isDragging = false;
//            var startX = 0, startY = 0;
//            var lastTouchDist = 0;
//            var lastTapTime = 0;
//
//            function resetZoom() {
//              zoomScale = 1; panX = 0; panY = 0;
//              applyTransform();
//            }
//
//            function applyTransform() {
//              var img = document.getElementById('modalImg');
//              var ov = document.getElementById('checkboxOverlays');
//              
//              var transformStr = 'translate(' + panX + 'px, ' + panY + 'px) scale(' + zoomScale + ')';
//              img.style.transform = transformStr;
//              ov.style.transform = transformStr;
//            }
//
//            var wrap = document.getElementById('modalImageWrap');
//            var modalImg = document.getElementById('modalImg');
//
//            // BLOQUEO CLAVE: Evita que el navegador intente "descargar" o "arrastrar" la imagen nativamente
//            modalImg.addEventListener('dragstart', function(e) { e.preventDefault(); });
//
//            // --- RATON (PC) ---
//            wrap.addEventListener('mousedown', function(e) {
//              if (e.button !== 0) return; // Solo clic izquierdo
//              e.preventDefault(); 
//              isDragging = true;
//              startX = e.clientX;
//              startY = e.clientY;
//              wrap.style.cursor = 'grabbing';
//            });
//
//            document.addEventListener('mousemove', function(e) {
//              if (!isDragging) return;
//              // Calculamos la diferencia (delta) y la sumamos al Pan
//              panX += (e.clientX - startX);
//              panY += (e.clientY - startY);
//              startX = e.clientX;
//              startY = e.clientY;
//              applyTransform();
//            });
//
//            document.addEventListener('mouseup', function() {
//              isDragging = false;
//              wrap.style.cursor = 'grab';
//            });
//
//            wrap.addEventListener('wheel', function(e) {
//              e.preventDefault();
//              var delta = e.deltaY > 0 ? -0.15 : 0.15;
//              zoomScale = Math.max(0.3, Math.min(6, zoomScale + delta));
//              applyTransform();
//            }, { passive: false });
//
//            // --- TACTIL (MOVIL) ---
//            wrap.addEventListener('touchstart', function(e) {
//              if (e.touches.length === 1) {
//                isDragging = true;
//                startX = e.touches[0].clientX;
//                startY = e.touches[0].clientY;
//                
//                // Doble tap para hacer Zoom In / Zoom Out
//                var now = Date.now();
//                if (now - lastTapTime < 300) {
//                  e.preventDefault();
//                  if (zoomScale > 1) { resetZoom(); } 
//                  else { zoomScale = 2.5; applyTransform(); }
//                  lastTapTime = 0;
//                } else {
//                  lastTapTime = now;
//                }
//              } else if (e.touches.length === 2) {
//                e.preventDefault();
//                var dx = e.touches[0].clientX - e.touches[1].clientX;
//                var dy = e.touches[0].clientY - e.touches[1].clientY;
//                lastTouchDist = Math.sqrt(dx*dx + dy*dy);
//              }
//            }, { passive: false });
//
//            wrap.addEventListener('touchmove', function(e) {
//              if (e.touches.length === 1 && isDragging) {
//                e.preventDefault(); // Clave para evitar el scroll nativo de la pantalla en móviles
//                panX += (e.touches[0].clientX - startX);
//                panY += (e.touches[0].clientY - startY);
//                startX = e.touches[0].clientX;
//                startY = e.touches[0].clientY;
//                applyTransform();
//              } else if (e.touches.length === 2) {
//                e.preventDefault();
//                var dx = e.touches[0].clientX - e.touches[1].clientX;
//                var dy = e.touches[0].clientY - e.touches[1].clientY;
//                var dist = Math.sqrt(dx*dx + dy*dy);
//                if (lastTouchDist > 0) {
//                  zoomScale = Math.max(0.3, Math.min(6, zoomScale * (dist / lastTouchDist)));
//                  applyTransform();
//                }
//                lastTouchDist = dist;
//              }
//            }, { passive: false });
//
//            wrap.addEventListener('touchend', function(e) {
//              if (e.touches.length < 2) lastTouchDist = 0;
//              if (e.touches.length === 0) isDragging = false;
//            });
//
//            // ===== BUTTONS =====
//            document.getElementById('btnCopiar').addEventListener('click', function() {
//              var respuesta = buildResponse();
//              var text = JSON.stringify(respuesta, null, 2);
//              if (navigator.clipboard && navigator.clipboard.writeText) {
//                navigator.clipboard.writeText(text).then(function() {
//                  showToast('Respuesta copiada al portapapeles');
//                }).catch(function() {
//                  fallbackCopy(text);
//                });
//              } else {
//                fallbackCopy(text);
//              }
//            });
//
//            function fallbackCopy(text) {
//              var ta = document.createElement('textarea');
//              ta.value = text;
//              ta.style.position = 'fixed'; ta.style.opacity = '0';
//              document.body.appendChild(ta);
//              ta.select();
//              try { document.execCommand('copy'); showToast('Respuesta copiada'); } catch(e) { alert('No se pudo copiar. Selecciona el texto manualmente.'); }
//              document.body.removeChild(ta);
//            }
//
//            document.getElementById('btnDescargar').addEventListener('click', function() {
//              var respuesta = buildResponse();
//              var json = JSON.stringify(respuesta, null, 2);
//              var blob = new Blob([json], {type: 'application/json'});
//              var a = document.createElement('a');
//              a.href = URL.createObjectURL(blob);
//              a.download = '%s';
//              a.click();
//              URL.revokeObjectURL(a.href);
//            });
//
//            function buildResponse() {
//              guardarModal();
//              return {
//                projectName: data.projectName,
//                iteracion: data.iteracion,
//                fechaEnvio: data.fechaEnvio,
//                fechaRespuesta: new Date().toISOString(),
//                respuestas: data.imagenes.map(function(img) {
//                  return {
//                    id: img.id,
//                    codigo: img.codigo || '',
//                    estado: img.estado || 'DISCARDED',
//                    comentario: img.comentario || '',
//                    checkboxes: (img.checkboxes || []).map(function(cb) {
//                      return { codigo: cb.codigo || '', estado: cb.estado || 'UNDEFINED' };
//                    })
//                  };
//                })
//              };
//            }
//
//            function showToast(msg) {
//              var t = document.createElement('div');
//              t.textContent = msg;
//              t.style.cssText = 'position:fixed;bottom:80px;left:50%%;transform:translateX(-50%%);background:#333;color:#fff;padding:8px 16px;border-radius:6px;font-size:.85rem;z-index:200;opacity:0;transition:opacity .3s';
//              document.body.appendChild(t);
//              requestAnimationFrame(function() { t.style.opacity = '1'; });
//              setTimeout(function() { t.style.opacity = '0'; setTimeout(function() { t.remove(); }, 300); }, 2000);
//            }
//
//            """.formatted(respuestaFilename);
//    } // --- Fin de metodo getClientJs ---
//
//
//    private String generarDataJson(ProjectModel project, Path thumbsDir, int iteracion) {
//        Map<String, String> selectedImages = project.getSelectedImages();
//        String projectName = project.getProjectName() != null ? project.getProjectName() : "Proyecto";
//        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
//
//        StringBuilder sb = new StringBuilder();
//        sb.append("{\n");
//        sb.append("  \"projectName\": ").append(jsonString(projectName)).append(",\n");
//        sb.append("  \"iteracion\": ").append(iteracion).append(",\n");
//        sb.append("  \"exportDate\": ").append(jsonString(timestamp)).append(",\n");
//        sb.append("  \"imagenes\": [\n");
//
//        int i = 0;
//        for (Map.Entry<String, String> entry : selectedImages.entrySet()) {
//            String rutaStr = entry.getKey();
//            Path rutaImagen = Path.of(rutaStr);
//            String etiqueta = entry.getValue() != null ? entry.getValue() : "";
//
//            String thumbFilename = nombreMiniatura(rutaImagen);
//            String imageId = generarId(rutaImagen);
//            String imageCode = project.getImageCodes().getOrDefault(rutaStr, "");
//
//            List<ImageCheckboxOverlay> checkboxes = project.hasClientSelection()
//                    ? project.getClientSelection().getImageCheckboxes(rutaStr)
//                    : List.of();
//            String comment = project.hasClientSelection()
//                    ? project.getClientSelection().getComments().getOrDefault(rutaStr, "")
//                    : "";
//
//            if (i > 0) sb.append(",\n");
//            sb.append("    {\n");
//            sb.append("      \"id\": ").append(jsonString(imageId)).append(",\n");
//            sb.append("      \"nombre\": ").append(jsonString(rutaImagen.getFileName() != null
//                    ? rutaImagen.getFileName().toString() : rutaStr)).append(",\n");
//            sb.append("      \"codigo\": ").append(jsonString(imageCode)).append(",\n");
//            sb.append("      \"etiqueta\": ").append(jsonString(etiqueta)).append(",\n");
//            sb.append("      \"miniatura\": ").append(jsonString("thumbs/" + thumbFilename)).append(",\n");
//            sb.append("      \"estado\": \"DISCARDED\",\n");
//            sb.append("      \"comentario\": ").append(jsonString(comment)).append(",\n");
//            var commentOverlay = project.hasClientSelection()
//                    ? project.getClientSelection().getCommentOverlays().get(rutaStr)
//                    : null;
//            if (commentOverlay != null && commentOverlay.getText() != null && !commentOverlay.getText().isEmpty()) {
//                sb.append("      \"commentOverlay\": {\n");
//                sb.append("        \"texto\": ").append(jsonString(commentOverlay.getText())).append(",\n");
//                sb.append("        \"x\": ").append(commentOverlay.getImageX()).append(",\n");
//                sb.append("        \"y\": ").append(commentOverlay.getImageY()).append("\n");
//                sb.append("      },\n");
//            }
//            sb.append("      \"checkboxes\": [\n");
//            for (int j = 0; j < checkboxes.size(); j++) {
//                ImageCheckboxOverlay cb = checkboxes.get(j);
//                if (j > 0) sb.append(",\n");
//                sb.append("        {\n");
//                sb.append("          \"codigo\": ").append(jsonString(cb.getCheckboxCode())).append(",\n");
//                sb.append("          \"x\": ").append(cb.getImageX()).append(",\n");
//                sb.append("          \"y\": ").append(cb.getImageY()).append(",\n");
//                sb.append("          \"tamano\": ").append(cb.getSize()).append(",\n");
//                sb.append("          \"marcado\": ").append(cb.getState() == SelectionState.SELECTED).append(",\n");
//                sb.append("          \"precio\": ").append(jsonString(cb.getPrice() > 0
//                        ? String.format("%.2f", cb.getPrice()) : "")).append(",\n");
//                sb.append("          \"comentario\": ").append(jsonString(cb.getComment() != null
//                        ? cb.getComment() : "")).append("\n");
//                sb.append("        }");
//            }
//            sb.append("\n      ]\n");
//            sb.append("    }");
//            i++;
//        }
//
//        sb.append("\n  ]\n}");
//        String json = sb.toString();
//        try {
//            new Gson().fromJson(json, Object.class);
//        } catch (JsonSyntaxException e) {
//            logger.error("[WebCatalogExporter] data.json generado NO es JSON v\u00e1lido: {}", e.getMessage());
//        }
//        return json;
//    } // --- Fin de metodo generarDataJson ---
//
//
//    private String generarRespuestaVacia(ProjectModel project, String projectSafeName, int iteracion) {
//        Map<String, String> selectedImages = project.getSelectedImages();
//        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
//
//        StringBuilder sb = new StringBuilder();
//        sb.append("{\n");
//        sb.append("  \"projectName\": ").append(jsonString(project.getProjectName())).append(",\n");
//        sb.append("  \"iteracion\": ").append(iteracion).append(",\n");
//        sb.append("  \"fechaEnvio\": ").append(jsonString(timestamp)).append(",\n");
//        sb.append("  \"fechaRespuesta\": null,\n");
//        sb.append("  \"respuestas\": [\n");
//
//        int i = 0;
//        for (String rutaStr : selectedImages.keySet()) {
//            Path rutaImagen = Path.of(rutaStr);
//            String imageId = generarId(rutaImagen);
//            if (i > 0) sb.append(",\n");
//            sb.append("    {\n");
//            sb.append("      \"id\": ").append(jsonString(imageId)).append(",\n");
//            sb.append("      \"codigo\": ").append(jsonString(project.getImageCodes().getOrDefault(rutaStr, ""))).append(",\n");
//            sb.append("      \"estado\": \"DISCARDED\",\n");
//            sb.append("      \"comentario\": \"\"\n");
//            sb.append("    }");
//            i++;
//        }
//
//        sb.append("\n  ]\n}");
//        return sb.toString();
//    } // --- Fin de metodo generarRespuestaVacia ---
//
//
////    private String generarMiniatura(Path original, Path thumbsDir) throws IOException {
////        String ext = obtenerExtension(original);
////        boolean salidaJpeg = forceJpeg || "jpg".equals(ext) || "jpeg".equals(ext);
////        String outExt = salidaJpeg ? "jpg" : ext;
////        String safeName = sanitizarNombre(
////                original.getFileName() != null ? original.getFileName().toString() : "imagen")
////                + "_thumb." + outExt;
////        Path thumbPath = thumbsDir.resolve(safeName);
////
////        if (Files.exists(thumbPath)) {
////            return safeName;
////        }
////
////        if (!Files.exists(original)) {
////            logger.warn("[WebCatalogExporter] Imagen no encontrada: {}. Se omitirá la miniatura.", original);
////            return safeName;
////        }
////
////        try {
////            Thumbnails.of(original.toFile())
////                    .size(thumbnailSize, thumbnailSize)
////                    .crop(net.coobird.thumbnailator.geometry.Positions.CENTER)
////                    .outputFormat(salidaJpeg ? "jpg" : ext)
////                    .outputQuality(jpegQuality)
////                    .toFile(thumbPath.toFile());
////        } catch (Exception e) {
////            logger.warn("[WebCatalogExporter] Error con Thumbnailator para {}: {}. Usando fallback.",
////                    original.getFileName(), e.getMessage());
////            try {
////                BufferedImage srcImg = ImageIO.read(original.toFile());
////                if (srcImg == null) {
////                    Files.copy(original, thumbPath, StandardCopyOption.REPLACE_EXISTING);
////                    return safeName;
////                }
////                int srcW = srcImg.getWidth();
////                int srcH = srcImg.getHeight();
////                double scale = Math.min((double) thumbnailSize / srcW, (double) thumbnailSize / srcH);
////                int newW = Math.max(1, (int) (srcW * scale));
////                int newH = Math.max(1, (int) (srcH * scale));
////                BufferedImage thumb = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
////                Graphics2D g = thumb.createGraphics();
////                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
////                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
////                g.drawImage(srcImg, 0, 0, newW, newH, null);
////                g.dispose();
////                String formatName = salidaJpeg ? "JPEG" : ext.toUpperCase();
////                ImageIO.write(thumb, formatName, thumbPath.toFile());
////            } catch (Exception e2) {
////                logger.warn("[WebCatalogExporter] Fallback también falló para {}: {}", original.getFileName(), e2.getMessage());
////                try {
////                    Files.copy(original, thumbPath, StandardCopyOption.REPLACE_EXISTING);
////                } catch (IOException ignored) {}
////            }
////        }
////
////        return safeName;
////    } // --- Fin de metodo generarMiniatura ---
//    
//    
////  *************************************************************************************************************** funciona
////    private String generarMiniatura(Path original, Path thumbsDir) throws IOException {
////        String ext = obtenerExtension(original);
////        boolean salidaJpeg = forceJpeg || "jpg".equals(ext) || "jpeg".equals(ext);
////        String outExt = salidaJpeg ? "jpg" : ext;
////        String safeName = sanitizarNombre(
////                original.getFileName() != null ? original.getFileName().toString() : "imagen")
////                + "_thumb." + outExt;
////        Path thumbPath = thumbsDir.resolve(safeName);
////
////        if (Files.exists(thumbPath)) return safeName;
////
////        try {
////            // Usamos .crop(Positions.CENTER) para que las miniaturas no se estiren
////            Thumbnails.of(original.toFile())
////                    .size(thumbnailSize, thumbnailSize)
////                    .crop(net.coobird.thumbnailator.geometry.Positions.CENTER)
////                    .outputFormat(salidaJpeg ? "jpg" : ext)
////                    .outputQuality(jpegQuality)
////                    .toFile(thumbPath.toFile());
////        } catch (Exception e) {
////            // Fallback simple si falla Thumbnailator
////            BufferedImage srcImg = ImageIO.read(original.toFile());
////            if (srcImg != null) {
////                BufferedImage thumb = new BufferedImage(thumbnailSize, thumbnailSize, BufferedImage.TYPE_INT_RGB);
////                Graphics2D g = thumb.createGraphics();
////                g.drawImage(srcImg, 0, 0, thumbnailSize, thumbnailSize, null);
////                g.dispose();
////                ImageIO.write(thumb, salidaJpeg ? "jpg" : ext, thumbPath.toFile());
////            }
////        }
////        return safeName;
////    }
////  *************************************************************************************************************** fin de funciona
//
//    
//    private String generarMiniatura(Path original, Path thumbsDir) throws IOException {
//        String ext = obtenerExtension(original);
//        boolean salidaJpeg = forceJpeg || "jpg".equals(ext) || "jpeg".equals(ext);
//        String outExt = salidaJpeg ? "jpg" : ext;
//        String safeName = sanitizarNombre(original.getFileName() != null ? original.getFileName().toString() : "imagen")
//                + "_thumb." + outExt;
//        Path thumbPath = thumbsDir.resolve(safeName);
//
//        if (Files.exists(thumbPath)) return safeName;
//
//        try {
//            // Mantenemos el tamaño pero usamos el centrado para evitar deformación
//            Thumbnails.of(original.toFile())
//                    .size(thumbnailSize, thumbnailSize)
//                    .crop(net.coobird.thumbnailator.geometry.Positions.CENTER)
//                    .outputFormat(salidaJpeg ? "jpg" : ext)
//                    .outputQuality(jpegQuality)
//                    .toFile(thumbPath.toFile());
//        } catch (Exception e) {
//            // Fallback robusto
//            BufferedImage srcImg = ImageIO.read(original.toFile());
//            if (srcImg != null) {
//                int w = Math.min(srcImg.getWidth(), thumbnailSize);
//                int h = Math.min(srcImg.getHeight(), thumbnailSize);
//                BufferedImage thumb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
//                Graphics2D g = thumb.createGraphics();
//                g.drawImage(srcImg, 0, 0, w, h, null);
//                g.dispose();
//                ImageIO.write(thumb, salidaJpeg ? "jpg" : ext, thumbPath.toFile());
//            }
//        }
//        return safeName;
//    }
//    
//    
//    private String generarHtml(ProjectModel project, String projectSafeName, int iteracion) {
//        String projectName = project.getProjectName() != null ? project.getProjectName() : "Cat\u00e1logo";
//        String respuestaFilename = projectSafeName + "_iteracion" + iteracion + "_respuesta.json";
//        String prjclFilename = projectSafeName + ".prjcl";
//
//        return "<!DOCTYPE html>\n"
//            + "<html lang=\"es\">\n"
//            + "<head>\n"
//            + "  <meta charset=\"UTF-8\">\n"
//            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
//            + "  <title>" + escapeHtml(projectName) + " \u2013 Cat\u00e1logo</title>\n"
//            + "  <style>\n" + getCss() + "  </style>\n"
//            + "</head>\n"
//            + "<body>\n"
//            + "\n"
//            + "  <header>\n"
//            + "    <h1>" + escapeHtml(projectName) + "</h1>\n"
//            + "    <p class=\"subtitle\">Iteraci\u00f3n #" + iteracion + " \u00b7 Marca los modelos que te interesan</p>\n"
//            + "  </header>\n"
//            + "\n"
//            + "  <nav id=\"topbar\">\n"
//            + "    <span id=\"counter\"></span>\n"
//            + "  </nav>\n"
//            + "\n"
//            + "  <main id=\"gallery\" class=\"gallery\"></main>\n"
//            + "\n"
//            + "  <footer>\n"
//            + "    <div id=\"summary\"></div>\n"
//            + "    <div class=\"footer-buttons\">\n"
//            + "      <button id=\"btnGenerar\" class=\"btn-primary\">\u2B07 Descargar Respuesta</button>\n"
//            + "      <button id=\"btnDescargarPrjcl\" class=\"btn-secondary\">\u2B07 Descargar .prjcl</button>\n"
//            + "    </div>\n"
//            + "  </footer>\n"
//            + "\n"
//            + "  <div id=\"modal\" class=\"modal hidden\">\n"
//            + "    <div class=\"modal-content\">\n"
//            + "      <button id=\"modalClose\" class=\"modal-close\">&times;</button>\n"
//            + "      <div class=\"modal-header\">\n"
//            + "        <span class=\"modal-codigo\" id=\"modalCodigo\"></span>\n"
//            + "        <h3 id=\"modalNombre\"></h3>\n"
//            + "      </div>\n"
//            + "      <img id=\"modalImg\" src=\"\" alt=\"\" class=\"modal-image\">\n"
//            + "      <p id=\"modalEtiqueta\" class=\"etiqueta\"></p>\n"
//            + "      <div id=\"modalCheckboxes\" class=\"modal-checkboxes\"></div>\n"
//            + "      <div class=\"modal-checkbox-toggle\">\n"
//            + "        <label><input type=\"checkbox\" id=\"modalCheckToggle\"> Marcar como seleccionado</label>\n"
//            + "      </div>\n"
//            + "      <label for=\"modalComentario\">Comentario:</label>\n"
//            + "      <textarea id=\"modalComentario\" rows=\"3\" placeholder=\"Escribe aqu\u00ed tu comentario...\"></textarea>\n"
//            + "    </div>\n"
//            + "  </div>\n"
//            + "\n"
//            + "  <script src=\"data.js\"></script>\n"
//            + "  <script>\n" + getJs(respuestaFilename, prjclFilename) + "  </script>\n"
//            + "</body>\n"
//            + "</html>\n";
//    } // --- Fin de metodo generarHtml ---
//
//
//    private String getCss() {
//        return """
//            *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
//            :root {
//              --bg: #0f0f1a;
//              --surface: #1a1a2e;
//              --card-bg: #222240;
//              --accent: #3a3a6a;
//              --selected: #2ecc71;
//              --selected-bg: rgba(46,204,113,.12);
//              --discarded: #e74c3c;
//              --discarded-bg: rgba(231,76,60,.12);
//              --text: #f0f0f0;
//              --text-muted: #999;
//              --radius: 10px;
//            }
//            body { background: var(--bg); color: var(--text); font-family: 'Segoe UI', system-ui, sans-serif; min-height: 100vh; }
//            header { text-align: center; padding: 2rem 1rem .8rem; }
//            header h1 { font-size: 1.8rem; font-weight: 700; letter-spacing: -.5px; }
//            header .subtitle { color: var(--text-muted); margin-top: .3rem; font-size: .95rem; }
//            #topbar { display: flex; justify-content: center; padding: .6rem 1rem; background: var(--surface); position: sticky; top: 0; z-index: 50; border-bottom: 1px solid #2a2a4a; }
//            #counter { color: var(--text-muted); font-size: .85rem; }
//            .gallery { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 1rem; padding: 1.2rem; max-width: 1400px; margin: 0 auto; }
//            .card { background: var(--card-bg); border-radius: var(--radius); overflow: hidden; border: 2px solid #2a2a4a; transition: transform .2s, border-color .2s, box-shadow .2s; position: relative; display: flex; flex-direction: column; }
//            .card:hover { transform: translateY(-3px); box-shadow: 0 6px 20px rgba(0,0,0,.35); }
//            .card.SELECTED { border-color: var(--selected); }
//            .card.DISCARDED { border-color: var(--discarded); opacity: .8; }
//            .card .card-badge { position: absolute; top: 6px; left: 6px; background: #000; color: #fff; padding: 1px 7px; border-radius: 3px; font-size: .7rem; font-weight: 700; z-index: 2; letter-spacing: .3px; }
//            .card-checkbox { position: absolute; top: 6px; right: 6px; z-index: 3; width: 24px; height: 24px; cursor: pointer; appearance: none; -webkit-appearance: none; background: rgba(0,0,0,.6); border: 2px solid #555; border-radius: 4px; display: flex; align-items: center; justify-content: center; transition: all .15s; }
//            .card-checkbox:checked { background: var(--selected); border-color: var(--selected); }
//            .card-checkbox:checked::after { content: '\\\\2713'; color: #000; font-weight: 700; font-size: 14px; line-height: 1; }
//            .card img { width: 100%; height: 160px; object-fit: cover; display: block; border-bottom: 1px solid #2a2a4a; cursor: pointer; }
//            .card-body { padding: .5rem .7rem .65rem; flex: 1; display: flex; flex-direction: column; gap: .3rem; }
//            .card-body .card-name { font-size: .82rem; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; cursor: pointer; }
//            .card-body .card-prices { font-size: .75rem; color: var(--text-muted); display: flex; gap: .4rem; flex-wrap: wrap; }
//            .card-body .card-prices .price-tag { background: #000; color: #fff; padding: 0 5px; border-radius: 2px; font-weight: 600; }
//            .card-body .card-footer { display: flex; align-items: center; justify-content: space-between; margin-top: auto; }
//            .card-body .badge { font-size: .7rem; padding: .1rem .5rem; border-radius: 8px; font-weight: 600; }
//            .badge-sel { background: var(--selected-bg); color: var(--selected); }
//            .badge-dis { background: var(--discarded-bg); color: var(--discarded); }
//            .comment-dot { display: inline-block; width: 16px; height: 16px; line-height: 16px; text-align: center; border-radius: 50%; background: var(--accent); color: #fff; font-size: .6rem; cursor: help; }
//            footer { position: sticky; bottom: 0; background: var(--surface); border-top: 1px solid #2a2a4a; padding: .7rem 1.2rem; display: flex; align-items: center; justify-content: space-between; z-index: 10; flex-wrap: wrap; gap: .5rem; }
//            #summary { font-size: .85rem; color: var(--text-muted); }
//            .footer-buttons { display: flex; gap: .5rem; }
//            .btn-primary { background: var(--selected); color: #000; border: none; padding: .5rem 1.2rem; border-radius: 6px; font-weight: 700; cursor: pointer; transition: opacity .2s; font-size: .85rem; }
//            .btn-primary:hover { opacity: .85; }
//            .btn-secondary { background: var(--accent); color: #fff; border: 1px solid #5a5a9a; padding: .5rem 1.2rem; border-radius: 6px; font-weight: 600; cursor: pointer; transition: opacity .2s; font-size: .85rem; }
//            .btn-secondary:hover { opacity: .85; }
//            .modal { position: fixed; inset: 0; background: rgba(0,0,0,.85); display: flex; align-items: center; justify-content: center; z-index: 100; }
//            .modal.hidden { display: none; }
//            .modal-content { background: var(--card-bg); border-radius: var(--radius); max-width: 680px; width: 94%; max-height: 92vh; overflow-y: auto; padding: 1.5rem; position: relative; border: 1px solid #3a3a5a; }
//            .modal-close { position: absolute; top: .7rem; right: .9rem; background: none; border: none; color: var(--text-muted); font-size: 1.5rem; cursor: pointer; z-index: 3; line-height: 1; padding: 0 4px; }
//            .modal-close:hover { color: #fff; }
//            .modal-header { margin-bottom: .8rem; }
//            .modal-header h3 { font-size: 1.1rem; font-weight: 600; margin-top: .2rem; }
//            .modal-codigo { display: inline-block; background: #000; color: #fff; padding: 1px 8px; border-radius: 3px; font-size: .75rem; font-weight: 700; }
//            .modal-image { width: 100%; height: auto; max-height: 70vh; object-fit: contain; display: block; margin: 0 auto .8rem; background: #0a0a15; cursor: pointer; }
//            .etiqueta { color: var(--text-muted); font-size: .82rem; margin-bottom: .5rem; }
//            .modal-checkboxes { margin-bottom: .8rem; }
//            .modal-checkboxes table { width: 100%; border-collapse: collapse; font-size: .82rem; }
//            .modal-checkboxes th, .modal-checkboxes td { padding: 5px 8px; text-align: left; border-bottom: 1px solid #2a2a4a; }
//            .modal-checkboxes th { color: var(--text-muted); font-weight: 600; }
//            .modal-checkbox-toggle { margin-bottom: 1rem; }
//            .modal-checkbox-toggle label { display: flex; align-items: center; gap: .5rem; cursor: pointer; font-size: .9rem; color: var(--text); }
//            .modal-checkbox-toggle input[type=checkbox] { width: 18px; height: 18px; cursor: pointer; accent-color: var(--selected); }
//            .ck { display: inline-block; font-weight: 700; width: 1.2em; text-align: center; }
//            .ck-sel { color: var(--selected); }
//            .ck-dis { color: var(--discarded); }
//            label { display: block; margin-bottom: .3rem; font-size: .82rem; color: var(--text-muted); }
//            textarea { width: 100%; background: var(--bg); border: 1px solid #3a3a5a; border-radius: 5px; color: var(--text); padding: .5rem; font-size: .9rem; resize: vertical; outline: none; }
//            textarea:focus { border-color: var(--accent); }
//            @media (max-width: 600px) {
//              header h1 { font-size: 1.3rem; }
//              .gallery { grid-template-columns: repeat(2, 1fr); gap: .5rem; padding: .5rem; }
//              .card img { height: 110px; }
//              footer { flex-direction: column; align-items: stretch; text-align: center; }
//              .footer-buttons { flex-direction: column; }
//              .btn-primary, .btn-secondary { width: 100%; }
//            }
//            """;
//    } // --- Fin de metodo getCss ---
//
//
//    private String getJs(String respuestaFilename, String prjclFilename) {
//        return """
//            var data = CATALOG_DATA || { imagenes: [] };
//            var currentIndex = -1;
//
//            renderGallery();
//
//            function renderGallery() {
//              var gallery = document.getElementById('gallery');
//              var html = '';
//              for (var i = 0; i < data.imagenes.length; i++) {
//                var img = data.imagenes[i];
//                var estado = img.estado || 'DISCARDED';
//                var checked = estado === 'SELECTED';
//                var codigoHtml = img.codigo ? '<div class=\\"card-badge\\">' + escHtml(img.codigo) + '</div>' : '';
//                var prices = [];
//                if (img.checkboxes) {
//                  for (var j = 0; j < img.checkboxes.length; j++) {
//                    if (img.checkboxes[j].precio) prices.push(img.checkboxes[j].precio);
//                  }
//                }
//                var pricesHtml = '';
//                if (prices.length > 0) {
//                  pricesHtml = '<div class=\\"card-prices\\">' + prices.map(function(p) { return '<span class=\\"price-tag\\">' + escHtml(p) + ' \\u20ac</span>'; }).join('') + '</div>';
//                }
//                var comentDot = (img.comentario && img.comentario.trim()) ? '<span class=\\"comment-dot\\" title=\\\\"Tiene comentario\\\\">\\u2709</span>' : '';
//                var checkedAttr = checked ? 'checked' : '';
//                html += '<div class=\\"card ' + estado + '\\">'
//                  + '<input type=\\"checkbox\\" class=\\"card-checkbox\\" ' + checkedAttr + ' data-index="' + i + '">'
//                  + codigoHtml
//                  + '<img src=\\"' + img.miniatura + '\\" alt=\\"' + escHtml(img.nombre) + '\\" loading=\\"lazy\\"'
//                  + ' onclick=\\"openModal(' + i + ')\\"'
//                  + ' onerror=\\"this.src=' + "'data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' width='200' height='160'><rect width='200' height='160' fill='%%23222'/><text x='50%%' y='50%%' fill='%%23666' text-anchor='middle' dy='.3em'>Sin imagen</text></svg>'" + '\\">'
//                  + '<div class=\\"card-body\\">'
//                  + '<div class=\\"card-name\\" onclick=\\"openModal(' + i + ')\\">' + escHtml(img.nombre) + '</div>'
//                  + pricesHtml
//                  + '<div class=\\"card-footer\\">'
//                  + '<span class=\\"badge ' + (checked ? 'badge-sel' : 'badge-dis') + '\\">' + (checked ? '\\u2713' : '\\u2717') + '</span>'
//                  + comentDot
//                  + '</div>'
//                  + '</div>'
//                  + '</div>';
//              }
//              gallery.innerHTML = html;
//
//              // Attach checkbox change listeners
//              gallery.querySelectorAll('.card-checkbox').forEach(function(cb) {
//                cb.addEventListener('change', function() {
//                  var idx = parseInt(cb.dataset.index);
//                  if (isNaN(idx)) return;
//                  var img = data.imagenes[idx];
//                  img.estado = cb.checked ? 'SELECTED' : 'DISCARDED';
//                  renderGallery();
//                });
//              });
//
//              updateCounter();
//              updateSummary();
//            }
//
//            function escHtml(s) { return (s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }
//
//            function updateCounter() {
//              var sel = data.imagenes.filter(function(i) { return i.estado === 'SELECTED'; }).length;
//              var dis = data.imagenes.filter(function(i) { return i.estado === 'DISCARDED'; }).length;
//              document.getElementById('counter').textContent = 'Total: ' + data.imagenes.length + ' | \\u2713 ' + sel + ' \\u2717 ' + dis;
//            }
//
//            function updateSummary() {
//              var sel = data.imagenes.filter(function(i) { return i.estado === 'SELECTED'; }).length;
//              var dis = data.imagenes.filter(function(i) { return i.estado === 'DISCARDED'; }).length;
//              document.getElementById('summary').textContent = '\\u2713 ' + sel + ' seleccionadas \\u00b7 \\u2717 ' + dis + ' descartadas';
//            }
//
//            function openModal(index) {
//              currentIndex = index;
//              var img = data.imagenes[index];
//              document.getElementById('modalCodigo').textContent = img.codigo || '';
//              document.getElementById('modalNombre').textContent = img.nombre;
//              document.getElementById('modalImg').src = img.miniatura;
//              document.getElementById('modalEtiqueta').textContent = img.etiqueta || '';
//              document.getElementById('modalComentario').value = img.comentario || '';
//              document.getElementById('modalCheckToggle').checked = (img.estado === 'SELECTED');
//
//              var cbContainer = document.getElementById('modalCheckboxes');
//              if (img.checkboxes && img.checkboxes.length > 0) {
//                var tblHtml = '<table><tr><th></th><th>C\\u00f3digo</th><th>Precio</th><th>Comentario</th></tr>';
//                for (var j = 0; j < img.checkboxes.length; j++) {
//                  var cb = img.checkboxes[j];
//                  var iconoCb = cb.marcado ? '<span class=\\"ck ck-sel\\">\\u2713</span>' : '<span class=\\"ck ck-dis\\">\\u2717</span>';
//                  var precio = cb.precio ? cb.precio + ' \\u20ac' : '\\u2014';
//                  var coment = cb.comentario || '\\u2014';
//                  var bubble = coment !== '\\u2014' ? ' <span class=\\"comment-dot\\" title=\\\\"Comentario\\\\">\\u2709</span>' : '';
//                  tblHtml += '<tr><td>' + iconoCb + '</td><td>' + escHtml(cb.codigo||'') + '</td><td>' + precio + '</td><td>' + escHtml(coment) + bubble + '</td></tr>';
//                }
//                tblHtml += '</table>';
//                cbContainer.innerHTML = tblHtml;
//              } else {
//                cbContainer.innerHTML = '<p style=\\"color:var(--text-muted);font-size:.82rem\\">Sin checkboxes</p>';
//              }
//
//              document.getElementById('modal').classList.remove('hidden');
//            }
//
//            document.getElementById('modalClose').addEventListener('click', function() {
//              guardarModal();
//              document.getElementById('modal').classList.add('hidden');
//            });
//
//            document.getElementById('modal').addEventListener('click', function(e) {
//              if (e.target === document.getElementById('modal')) {
//                guardarModal();
//                document.getElementById('modal').classList.add('hidden');
//              }
//            });
//
//            document.getElementById('modalCheckToggle').addEventListener('change', function() {
//              if (currentIndex >= 0) {
//                data.imagenes[currentIndex].estado = this.checked ? 'SELECTED' : 'DISCARDED';
//                renderGallery();
//              }
//            });
//
//            document.getElementById('modalComentario').addEventListener('input', function(e) {
//              if (currentIndex >= 0) data.imagenes[currentIndex].comentario = e.target.value;
//            });
//
//            document.getElementById('modalImg').addEventListener('click', function() {
//              // Close modal when clicking the image (toggle back)
//            });
//
//            function guardarModal() {
//              if (currentIndex >= 0) {
//                data.imagenes[currentIndex].comentario = document.getElementById('modalComentario').value;
//              }
//              renderGallery();
//            }
//
//            document.getElementById('btnGenerar').addEventListener('click', function() {
//              var dis = data.imagenes.filter(function(i) { return i.estado === 'DISCARDED'; }).length;
//              if (dis > 0) {
//                if (!confirm('Atenci\\u00f3n: hay ' + dis + ' imagen(es) descartadas. \\u00bfDeseas descargar la respuesta igualmente?')) return;
//              }
//              var respuesta = {
//                projectName: data.projectName,
//                iteracion: data.iteracion,
//                fechaEnvio: data.exportDate,
//                fechaRespuesta: new Date().toISOString(),
//                respuestas: data.imagenes.map(function(img) {
//                  return {
//                    id: img.id,
//                    codigo: img.codigo || '',
//                    estado: img.estado || 'DISCARDED',
//                    comentario: img.comentario || '',
//                    checkboxes: (img.checkboxes || []).map(function(cb) {
//                      return { codigo: cb.codigo || '', marcado: cb.marcado || false };
//                    })
//                  };
//                })
//              };
//              var blob = new Blob([JSON.stringify(respuesta, null, 2)], {type: 'application/json'});
//              var a = document.createElement('a');
//              a.href = URL.createObjectURL(blob);
//              a.download = '%s';
//              a.click();
//              URL.revokeObjectURL(a.href);
//            });
//
//            document.getElementById('btnDescargarPrjcl').addEventListener('click', function() {
//              var prjcl = {
//                projectName: data.projectName,
//                iteracion: data.iteracion,
//                exportDate: data.exportDate,
//                fechaRespuesta: new Date().toISOString(),
//                respuestas: data.imagenes.map(function(img) {
//                  return {
//                    id: img.id,
//                    codigo: img.codigo || '',
//                    estado: img.estado || 'DISCARDED',
//                    comentario: img.comentario || '',
//                    checkboxes: (img.checkboxes || []).map(function(cb) {
//                      return {
//                        codigo: cb.codigo || '',
//                        marcado: cb.marcado || false,
//                        x: cb.x || 0, y: cb.y || 0,
//                        precio: cb.precio || '',
//                        comentario: cb.comentario || ''
//                      };
//                    }),
//                    commentOverlay: img.commentOverlay || null
//                  };
//                })
//              };
//              var blob = new Blob([JSON.stringify(prjcl, null, 2)], {type: 'application/json'});
//              var a = document.createElement('a');
//              a.href = URL.createObjectURL(blob);
//              a.download = '%s';
//              a.click();
//              URL.revokeObjectURL(a.href);
//            });
//
//            """.formatted(respuestaFilename, prjclFilename);
//    } // --- Fin de metodo getJs ---
//
//
//    private String nombreMiniatura(Path original) {
//        String ext = obtenerExtension(original);
//        boolean salidaJpeg = forceJpeg || "jpg".equals(ext) || "jpeg".equals(ext);
//        String outExt = salidaJpeg ? "jpg" : ext;
//        return sanitizarNombre(
//                original.getFileName() != null ? original.getFileName().toString() : "imagen")
//                + "_thumb." + outExt;
//    } // --- Fin de metodo nombreMiniatura ---
//
//
//    // -----------------------------------------------------------------------
//    // Utilidades
//    // -----------------------------------------------------------------------
//
//    private String generarId(Path ruta) {
//        String nombre = ruta.getFileName() != null ? ruta.getFileName().toString() : ruta.toString();
//        long size = 0;
//        try {
//            if (Files.exists(ruta)) size = Files.size(ruta);
//        } catch (IOException ignored) {}
//        return sanitizarNombre(nombre) + "_" + size;
//    } // --- Fin de metodo generarId ---
//
//    private String generarId(String rutaStr) {
//        return generarId(Path.of(rutaStr));
//    } // --- Fin de metodo generarId (String) ---
//
//
//    private String sanitizarNombre(String nombre) {
//        if (nombre == null) return "proyecto";
//        return nombre.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
//    } // --- Fin de metodo sanitizarNombre ---
//
//
//    private String obtenerExtension(Path ruta) {
//        String nombre = ruta.getFileName() != null ? ruta.getFileName().toString() : "";
//        int dot = nombre.lastIndexOf('.');
//        if (dot > 0) {
//            String ext = nombre.substring(dot + 1).toLowerCase();
//            return ext.isEmpty() ? "jpg" : ext;
//        }
//        return "jpg";
//    } // --- Fin de metodo obtenerExtension ---
//
//
//    private String jsonString(String valor) {
//        if (valor == null) return "null";
//        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"")
//                           .replace("\n", "\\n").replace("\r", "\\r") + "\"";
//    } // --- Fin de metodo jsonString ---
//
//
//    private String escapeHtml(String s) {
//        if (s == null) return "";
//        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
//    } // --- Fin de metodo escapeHtml ---
//
//} // --- Fin de clase WebCatalogExporter ---
//
//
