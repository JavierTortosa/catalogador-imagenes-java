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

import modelo.proyecto.ImageCheckboxOverlay;
import modelo.proyecto.Mensaje;
import modelo.proyecto.ProjectModel;
import modelo.proyecto.SelectionState;
import modelo.proyecto.CommentThread; // Importación de la clase que maneja los estados
import net.coobird.thumbnailator.Thumbnails;

public class WebCatalogExporter {

    private static final Logger logger = LoggerFactory.getLogger(WebCatalogExporter.class);

    private static record QualityLevel(int maxDimension, double jpegQuality) {}
    private static record ThumbnailResult(String base64, int width, int height, int origWidth, int origHeight) {}

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
    
    } // --- Fin del metodo exportar ---

    
    public void exportar(ProjectModel project, Path outputDir, int iteracion) throws IOException {
        exportar(project, outputDir, iteracion, null);
    
    } // --- Fin del metodo exportar ---
    
    
    
    private QualityLevel determinarCalidad(int imageCount) {
        if (imageCount <= 20)  return new QualityLevel(920, 0.85);
        if (imageCount <= 50)  return new QualityLevel(690, 0.75);
        return new QualityLevel(550, 0.70);
    
    } // --- Fin del Metodo determinarCalidad ---

    
    public void exportarHtmlCliente(ProjectModel project, Path outputFile, int iteracion) throws IOException {
        this.exportarHtmlCliente(project, outputFile, iteracion, null);
    
    } // --- Fin del Metodo exportarHtmlCliente ---

    
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
            imagesJson.append("  \"precio\": ").append(jsonString(pi.getPrice() > 0 ? String.format("%.2f", pi.getPrice()) : "")).append(",\n");
            imagesJson.append("  \"comentario\": ").append(jsonThread(pi.getCommentThreadAccess(), comment)).append(",\n"); // Usar CommentThread
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
                    imagesJson.append("      \"comentario\": ").append(jsonThread(cb.getCommentThreadAccess(), cb.getComment())).append("\n"); // Usar CommentThread
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
    
    } // --- Fin del Metodo exportarHtmlCliente ---

    
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
    
    } // --- Fin del Metodo generarMiniaturaBase64 --- 

    
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
    
    } // --- Fin del Metodo generarMiniatura --- 

    
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
            + "    <p class=\"subtitle\">Iteraci\u00f3n #" + iteracion + " \u00b7 Marca los modelos que te interesan y revisa los mensajes</p>\n"
            + "  </header>\n"
            + "  <nav id=\"topbar\">\n"
            + "    <div class=\"topbar-section\">\n"
            + "      <span id=\"counter\"></span>\n"
            + "    </div>\n"
            + "    <div class=\"topbar-section sort-controls\">\n"
            + "      <span style=\"font-size:0.8rem; color:var(--text-muted);\">Ordenar:</span>\n"
            + "      <select id=\"sortSelect\" class=\"control-select\">\n"
            + "        <option value=\"original\">Orden original</option>\n"
            + "        <option value=\"nombre\">Por Nombre (Alfabeto)</option>\n"
            + "        <option value=\"estado\">Por Estado (Sel. primero)</option>\n"
            + "        <option value=\"mensajes\">Con mensajes primero</option>\n"
            + "      </select>\n"
            + "      <button id=\"sortDir\" data-dir=\"asc\" class=\"btn-icon\" title=\"Orden normal\">&#x2193;</button>\n"
            + "    </div>\n"
            + "    <div class=\"topbar-section footer-buttons\">\n"
            + "      <button id=\"btnCopiar\" class=\"btn-primary\">Copiar respuesta</button>\n"
            + "      <button id=\"btnDescargar\" class=\"btn-secondary\">Descargar respuesta</button>\n"
            + "    </div>\n"
            + "  </nav>\n"
            + "  <main id=\"gallery\" class=\"gallery\"></main>\n"
            + "  <footer>\n"
            + "    <div id=\"summary\"></div>\n"
            + "  </footer>\n"
            + "  <div id=\"modal\" class=\"modal hidden\">\n"
            + "    <div class=\"modal-content\">\n"
            + "      <button id=\"modalClose\" class=\"modal-close\">&times;</button>\n"
            + "      <div class=\"modal-header\">\n"
            + "        <div class=\"modal-info\">\n"
            + "          <span class=\"modal-codigo\" id=\"modalCodigo\"></span>\n"
            + "          <span class=\"modal-nombre\" id=\"modalNombre\"></span>\n"
            + "          <span class=\"tristate-cb\" id=\"modalCheckPrincipal\" tabindex=\"0\" title=\"Seleccionar/Descartar\">&#x25cb;</span>\n"
            + "        </div>\n"
            + "      </div>\n"
            + "      <div class=\"modal-body-layout\">\n"
            + "        <div id=\"modalImageWrap\" class=\"modal-image-wrap\">\n"

            + "          <img id=\"modalImg\" src=\"\" alt=\"\" class=\"modal-image\" draggable=\"false\">\n"
            + "          <div id=\"checkboxOverlays\" class=\"checkbox-overlays\"></div>\n"
            + "        </div>\n"
            + "        <div class=\"modal-chat-sidebar\">\n"
            + "          <div class=\"modal-chat-title\">Chat general de la imagen</div>\n"
            + "          <div id=\"modalChatMessages\" class=\"chat-messages\"></div>\n"
            + "          <div class=\"chat-input-row\">\n"
            + "            <textarea id=\"modalComentario\" rows=\"3\" placeholder=\"Escribe aquí y pulsa Enviar (o Intro)...\"></textarea>\n"
            + "            <button id=\"btnEnviarChat\" class=\"btn-enviar\">Enviar</button>\n"
            + "          </div>\n"
            + "        </div>\n"
            + "      </div>\n"
            + "    </div>\n"
            + "  </div>\n"
            + "  <script>\n" + getClientJs(respuestaFilename) + "  </script>\n"
            + "</body>\n"
            + "</html>\n";
        
    } // --- Fin del Metodo generarHtmlCliente ---
    
    
    private String getClientCss() {
         return "/* === RESET & COLOR VARIABLES === */\n"
              + "*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }\n"
              + ":root { --bg: #0f0f1a; --card-bg: #222240; --text: #f0f0f0; --accent: #3a3a6a; --red: #ff3333; --green: #33cc33; --blue: #3399ff; --gray: #aaa; --selected: #2ecc71; --selected-bg: rgba(46,204,113,.12); --discarded: #e74c3c; --discarded-bg: rgba(231,76,60,.12); --undefined: #f39c12; --undefined-bg: rgba(243,156,18,.08); --text-muted: #999; --radius: 8px; --surface: #1a1a2e; }\n"
              + "body { background: var(--bg); color: var(--text); font-family: sans-serif }\n"
              
              + "/* === HEADER & TOPBAR === */\n"
              + "header { text-align: center; padding: 2rem; }\n"
              + "#topbar { position: sticky; top: 0; z-index: 50; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 15px; padding: .8rem 1rem; background: rgba(26,26,46,0.95); backdrop-filter: blur(5px); border-bottom: 1px solid #2a2a4a; }\n"
              + ".topbar-section { display: flex; align-items: center; gap: 10px; }\n"
              + "#counter { color: var(--text-muted); font-size: .9rem; font-weight: bold; }\n"
              + ".sort-controls { background: var(--card-bg); padding: 5px 10px; border-radius: 6px; border: 1px solid var(--accent); }\n"
              + ".control-select { background: var(--bg); color: var(--text); border: 1px solid #3a3a6a; padding: .4rem; border-radius: 4px; outline: none; font-size: .85rem; cursor: pointer; }\n"
              + ".btn-icon { background: var(--bg); color: var(--text); border: 1px solid #3a3a6a; padding: .3rem .6rem; border-radius: 4px; cursor: pointer; font-size: 1.1rem; }\n"
              + ".btn-icon:hover { background: var(--accent); }\n"
              
              + "/* === GALERIA DE TARJETAS === */\n"
              + ".gallery { display: grid; grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); gap: 15px; padding: 20px; max-width: 1200px; margin: 0 auto; }\n"
              + ".card { background: var(--card-bg); border-radius: 8px; border: 1px solid #3a3a6a; overflow: hidden; display: flex; flex-direction: column; position: relative; }\n"
              + ".card-header { display: flex; align-items: center; gap: .5rem; padding: .5rem; background: #1a1a2e; border-bottom: 1px solid #3a3a6a; }\n"
              + ".card-codigo { background: #000; color: #fff; padding: 1px 6px; border-radius: 3px; font-size: .7rem; font-weight: 700; white-space: nowrap; flex-shrink: 0; }\n"
              + ".card-name { font-size: .85rem; font-weight: bold; flex-grow: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer; }\n"
             
              + "/* === INDICADOR DE MENSAJES NO LEIDOS === */\n"
              + ".msg-indicator { display: inline-block; width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; vertical-align: middle; }\n"
              + ".msg-indicator.has-unread { background: #ffd700; box-shadow: 0 0 4px rgba(255,215,0,.6); }\n"
              + ".msg-indicator.has-read { background: #3498db; box-shadow: 0 0 4px rgba(52,152,219,.6); }\n"

              + "/* === ICONO DE COMENTARIO (sobre en galeria) === */\n"
              + ".comment-icon { display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; cursor: pointer; flex-shrink: 0; background: #000; border-radius: 3px; background-image: url('data:image/svg+xml;utf8,<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"%23666\"><path d=\"M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z\"/></svg>'); background-size: 15px; background-repeat: no-repeat; background-position: center; transition: all 0.2s; }\n"
              + ".comment-icon:hover { opacity: 0.8; }\n"
              
              + "/* Colores segun estado: rojo=sin leer, verde=leido, azul=contestado */\n"
              + ".comment-icon.has-unread { background-image: url('data:image/svg+xml;utf8,<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"%23ff3333\"><path d=\"M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z\"/></svg>'); }\n"
              + ".comment-icon.has-read { background-image: url('data:image/svg+xml;utf8,<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"%233399ff\"><path d=\"M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z\"/></svg>'); }\n"
              + ".comment-icon.has-read-pending { background-image: url('data:image/svg+xml;utf8,<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"%2333cc33\"><path d=\"M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z\"/></svg>'); }\n"
          
              + "/* === TRISTATE EN TARJETA === */\n"
              + ".card-tristate { display: inline-flex; align-items: center; justify-content: center; width: 22px; height: 22px; border-radius: 4px; cursor: pointer; font-size: .9rem; font-weight: 700; user-select: none; background: var(--bg); border: 2px solid var(--accent); flex-shrink: 0; }\n"
              + ".card-price { font-size: .75rem; font-weight: 600; color: #2ecc71; margin-left: auto; flex-shrink: 0; padding: 0 4px; white-space: nowrap; }\n"
              + ".card-tristate.state-selected { background: var(--selected-bg); color: var(--selected); border-color: var(--selected); }\n"
              + ".card-tristate.state-discarded { background: var(--discarded-bg); color: var(--discarded); border-color: var(--discarded); }\n"
              + ".card-tristate.state-undefined { background: var(--undefined-bg); color: var(--undefined); border-color: var(--undefined); }\n"
              + ".card-comment { font-size: .8rem; color: var(--text-muted); padding: .4rem .5rem; text-align: center; border-bottom: 1px solid #2a2a4a; background: #15152a; word-break: break-word; }\n"
              + ".card img { width: 100%; height: 207px; object-fit: cover; display: block; background: #000; cursor: pointer; margin-top: auto; }\n"
              + ".card-prices { font-size: .75rem; color: var(--text-muted); display: flex; gap: .4rem; flex-wrap: wrap; justify-content:center; }\n"
              + ".card-prices .price-tag { background: #000; color: #fff; padding: 0 5px; border-radius: 2px; font-weight: 600; }\n"
              + "footer { border-top: 1px solid #2a2a4a; padding: .7rem 1.2rem; display: flex; align-items: center; justify-content: center; background: var(--surface); }\n"
              + "#summary { font-size: .85rem; color: var(--text-muted); }\n"
              + ".footer-buttons { display: flex; gap: .5rem; }\n"
              + ".btn-primary { background: var(--selected); color: #000; border: none; padding: .5rem 1.2rem; border-radius: 6px; font-weight: 700; cursor: pointer; font-size: .85rem; }\n"
              + ".btn-primary:hover { opacity: .8; }\n"
              + ".btn-secondary { background: var(--accent); color: #fff; border: 1px solid #5a5a9a; padding: .5rem 1.2rem; border-radius: 6px; font-weight: 600; cursor: pointer; font-size: .85rem; }\n"
              + ".btn-secondary:hover { opacity: .8; }\n"
          
              + "/* === MODAL (imagen ampliada + overlays + chat) === */\n"
              + ".modal { position: fixed; inset: 0; background: rgba(0,0,0,0.9); display: flex; align-items: center; justify-content: center; z-index: 100; }\n"
              + ".modal.hidden { display: none !important; }\n"
              + ".hidden { display: none !important; }\n"
              + ".modal-content { background: var(--card-bg); width: 95%; max-width: 1200px; padding: 20px; border-radius: 8px; position: relative; max-height: 95vh; display: flex; flex-direction: column; border: 1px solid #3a3a5a; }\n"
              + ".modal-close { position: absolute; top: 10px; right: 15px; cursor: pointer; font-size: 2rem; color: #fff; border: none; background: none; z-index: 10;}\n"
              + ".modal-header { margin-bottom: .8rem; padding-right: 30px; }\n"
              + ".modal-info { display: flex; align-items: center; gap: .5rem; }\n"
              + ".modal-nombre { font-size: 1.05rem; font-weight: 600; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }\n"
              + ".modal-codigo { display: inline-block; background: #000; color: #fff; padding: 1px 8px; border-radius: 3px; font-size: .7rem; font-weight: 700; flex-shrink: 0; }\n"
              + ".modal-body-layout { display: flex; gap: 15px; flex: 1; min-height: 0; }\n"
              + ".modal-image-wrap { flex: 2; overflow: hidden; border-radius: 6px; background: #000; position: relative; cursor: grab; touch-action: none; }\n"
              + ".modal-image-wrap:active { cursor: grabbing; }\n"
              + ".modal-image { width: 100%; height: 100%; object-fit: contain; display: block; transform-origin: 0 0; user-select: none; -webkit-user-select: none; pointer-events: none; }\n"
              + ".checkbox-overlays { position: absolute; top: 0; left: 0; pointer-events: none; transform-origin: 0 0; }\n"
       
              + "/* === OVERLAYS DE CHECKBOX + BURBUJA DE MENSAJES === */\n"
              + ".cb-overlay { position: absolute; display: flex; align-items: center; gap: 3px; pointer-events: auto; transform: translate(-50%, -50%); background: rgba(0,0,0,.85); border-radius: 4px; padding: 2px 5px; white-space: nowrap; cursor: pointer; border: 1px solid rgba(255,255,255,.2); user-select: none; }\n"
              + ".cb-overlay .cb-indicator { display: inline-flex; align-items: center; justify-content: center; width: 18px; height: 18px; border-radius: 3px; font-weight: 700; font-size: .7rem; }\n"
              + ".cb-overlay .cb-indicator.sel { background: var(--selected); color: #000; }\n"
              + ".cb-overlay .cb-indicator.dis { background: var(--discarded); color: #fff; }\n"
              + ".cb-overlay .cb-indicator.und { background: transparent; color: var(--undefined); border: 2px solid var(--undefined); }\n"
              + ".cb-overlay .cb-codigo { color: var(--text-muted); font-size: .65rem; }\n"
              + ".cb-overlay .cb-price { color: #fff; font-size: .7rem; font-weight: 600; }\n"
          
              + "/* Burbuja de comentarios: rojo=no leido, verde=leido, azul=contestado, gris=sin msgs */\n"
              + ".cb-overlay .cb-bubble { color: #aaa; font-size: .8rem; cursor: pointer; padding: 0 2px; transition: all .3s; }\n"
              + ".cb-overlay .cb-bubble:hover { color: #fff; }\n"
              + ".cb-overlay .cb-bubble.read { color: var(--green); }\n"
              + ".cb-overlay .cb-bubble.unread { color: var(--red); text-shadow: 0 0 6px rgba(255,51,51,.6); animation: pulse-comment 2s infinite; }\n"
              + ".cb-overlay .cb-bubble.replied { color: var(--blue); }\n"
              + ".cb-overlay .cb-bubble.empty { color: var(--gray); }\n"
              + "@keyframes pulse-comment { 0% { text-shadow: 0 0 4px rgba(255,215,0,.4); } 50% { text-shadow: 0 0 12px rgba(255,215,0,.9); } 100% { text-shadow: 0 0 4px rgba(255,215,0,.4); } }\n"
         
              + "/* === CHAT LATERAL EN MODAL === */\n"
              + ".modal-chat-sidebar { flex: 1; min-width: 320px; max-width: 400px; display: flex; flex-direction: column; background: var(--surface); padding: 12px; border-radius: 8px; border: 1px solid var(--accent); }\n"
              + ".modal-chat-sidebar { flex: 1; min-width: 320px; max-width: 400px; display: flex; flex-direction: column; background: var(--surface); padding: 12px; border-radius: 8px; border: 1px solid var(--accent); }\n"
              + ".modal-chat-title { font-size: .85rem; color: var(--text-muted); text-transform: uppercase; margin-bottom: 8px; font-weight: bold; letter-spacing: .5px; border-bottom: 1px solid #3a3a6a; padding-bottom: 6px; }\n"
              + ".chat-messages { flex: 1; overflow-y: auto; padding: 10px; background: #0a0a15; border-radius: 5px; margin-bottom: 10px; display: flex; flex-direction: column; gap: 8px; }\n"
              + ".chat-msg { position: relative; padding: .5rem .7rem; border-radius: 6px; font-size: .85rem; max-width: 90%; word-break: break-word; }\n"
              + ".chat-msg.nosotros { background: #b8860b; align-self: flex-end; border-radius: 6px 6px 0 6px; }\n"
              + ".chat-msg.cliente { background: #2a2a4a; align-self: flex-start; border-radius: 6px 6px 6px 0; }\n"
              + ".btn-borrar { position: absolute; top: 2px; right: 5px; cursor: pointer; color: #ff6666; font-size: 10px; }\n"
              + ".chat-input-row { display: flex; flex-direction: column; gap: .5rem; }\n"
              + ".chat-input-row textarea { flex: 1; background: var(--bg); border: 1px solid #3a3a5a; border-radius: 5px; color: var(--text); padding: .6rem; font-size: .9rem; resize: none; outline: none; min-height: 4rem; }\n"
              + ".chat-input-row textarea:focus { border-color: var(--selected); }\n"
              + ".btn-enviar { background: var(--selected); color: #000; border: none; border-radius: 5px; padding: .6rem; font-weight: 700; cursor: pointer; font-size: .95rem; text-transform: uppercase; transition: background .1s; }\n"
              + ".btn-enviar:hover { background: #27ae60; }\n"
        
              + "/* === RESPONSIVE === */\n"
              + "@media (max-width: 800px) {\n"
              + "  .modal-body-layout { flex-direction: column; overflow-y: auto; }\n"
              + "  .modal-image-wrap { min-height: 40vh; flex: none; }\n"
              + "  .modal-chat-sidebar { min-height: 40vh; flex: none; max-width: 100%; }\n"
              + "}\n"
              + "@media (max-width: 600px) {\n"
              + "  #topbar { flex-direction: column; align-items: stretch; text-align: center; }\n"
              + "  .topbar-section { justify-content: center; flex-wrap: wrap; }\n"
              + "  .gallery { grid-template-columns: repeat(2, 1fr); gap: .5rem; padding: .5rem; }\n"
              + "  .footer-buttons { flex-direction: column; width: 100%; }\n"
              + "  .btn-primary, .btn-secondary { width: 100%; }\n"
              + "}\n";
        
    } // --- Fin del Metodo getClientCss --- 

    

    private String getClientJs(String respuestaFilename) {
         return "// === VARIABLES GLOBALES ===\n"
              + "var data = CATALOG_DATA || { imagenes: [] };\n"
              + "var currentIndex = -1;\n"
              + "var currentContext = { type: 'IMAGE', index: 0, cbIndex: -1 };\n"
              + "var zoomScale = 1, panX = 0, panY = 0, isDragging = false, startX = 0, startY = 0, lastTouchDist = 0, lastTapTime = 0;\n"
              + "for(var i=0; i<data.imagenes.length; i++) { data.imagenes[i]._origIndex = i; }\n"
             
              + "// === FUNCIONES AUXILIARES DE MENSAJES ===\n"
              + "function getCommentText(c) { if (typeof c === 'string') return c; if (c && c.hilo && c.hilo.length) return c.hilo[c.hilo.length-1].texto; return ''; }\n"
           
              + "// return true si el cliente ha respondido al menos una vez en el hilo\n"
              + "function hasClientReplied(c) {\n"
              + "  if (!c || !c.hilo) return false;\n"
              + "  for (var k = 0; k < c.hilo.length; k++) { if (c.hilo[k].de === 'cliente') return true; }\n"
              + "  return false;\n"
              + "}\n"
            
              + "// Estados: UNREAD (rojo), PENDING (verde/leido), READ (azul/contestado), NONE (gris)\n"
              + "function getMessageStatus(c) {\n"
              + "  if (!c) return 'NONE';\n"
              + "  var st = typeof c.estadoHtml !== 'undefined' ? c.estadoHtml : 0;\n"
              + "  if (st === 1) return 'PENDING';\n" // 1: mensaje leído no contestado -> verde
              + "  if (st === 2) return 'READ';\n"    // 2: mensaje contestado -> azul
              + "  if (st === 3) return 'UNREAD';\n"  // 3: mensaje nuevo no leído -> rojo
              + "  return 'NONE';\n"
              + "}\n"
            
              + "// Peso de mensajes para ordenacion y color del icono: 3=UNREAD, 2=PENDING, 1=READ, 0=NONE\n"
              + "function getCardMessageWeight(img) {\n"
              + "  var st = getMessageStatus(img.comentario);\n"
              + "  var maxW = st === 'UNREAD' ? 3 : (st === 'PENDING' ? 2 : (st === 'READ' ? 1 : 0));\n"
              + "  if (img.checkboxes) {\n"
              + "    for(var j=0; j<img.checkboxes.length; j++) {\n"
              + "      var cbSt = getMessageStatus(img.checkboxes[j].comentario);\n"
              + "      var w = cbSt === 'UNREAD' ? 3 : (cbSt === 'PENDING' ? 2 : (cbSt === 'READ' ? 1 : 0));\n"
              + "      if (w > maxW) maxW = w;\n"
              + "    }\n"
              + "  }\n"
              + "  return maxW;\n"
              + "}\n"
              + "renderGallery();\n"
           
              + "// === ORDENACION ===\n"
              + "function sortGallery() {\n"
              + "  var sortBy = document.getElementById('sortSelect').value;\n"
              + "  var asc = document.getElementById('sortDir').dataset.dir === 'asc' ? 1 : -1;\n"
              + "  data.imagenes.sort(function(a, b) {\n"
              + "    var valA, valB;\n"
              + "    if (sortBy === 'nombre') { valA = (a.nombre || '').toLowerCase(); valB = (b.nombre || '').toLowerCase(); }\n"
              + "    else if (sortBy === 'estado') { var w = {'SELECTED':1, 'UNDEFINED':2, 'DISCARDED':3}; valA = w[a.estado||'UNDEFINED']; valB = w[b.estado||'UNDEFINED']; }\n"
              + "    else if (sortBy === 'mensajes') { valA = getCardMessageWeight(a); valB = getCardMessageWeight(b); if(valA < valB) return 1 * asc; if(valA > valB) return -1 * asc; return a._origIndex - b._origIndex; }\n"
              + "    else { valA = a._origIndex; valB = b._origIndex; }\n"
              + "    if (valA < valB) return -1 * asc;\n"
              + "    if (valA > valB) return 1 * asc;\n"
              + "    return a._origIndex - b._origIndex;\n"
              + "  });\n"
              + "  renderGallery();\n"
              + "}\n"
              + "document.getElementById('sortSelect').addEventListener('change', sortGallery);\n"
              + "document.getElementById('sortDir').addEventListener('click', function() {\n"
              + "  var isAsc = this.dataset.dir === 'asc';\n"
              + "  this.dataset.dir = isAsc ? 'desc' : 'asc';\n"
              + "  this.innerHTML = isAsc ? '&#x2191;' : '&#x2193;';\n"
              + "  this.title = isAsc ? 'Orden invertido' : 'Orden normal';\n"
              + "  sortGallery();\n"
              + "});\n"
          
              + "// === RENDERIZADO DE GALERIA ===\n"
              + "function renderGallery() {\n"
              + "  var gallery = document.getElementById('gallery');\n"
              + "  var html = '';\n"
              + "  for (var i = 0; i < data.imagenes.length; i++) {\n"
              + "    var img = data.imagenes[i];\n"
              + "    var estado = img.estado || 'UNDEFINED';\n"
              + "    var tristateClass = estado === 'SELECTED' ? 'state-selected' : (estado === 'DISCARDED' ? 'state-discarded' : 'state-undefined');\n"
              + "    var tristateChar = estado === 'SELECTED' ? escHtml('\\u2713') : (estado === 'DISCARDED' ? escHtml('\\u2717') : escHtml('\\u25cb'));\n"
              + "    var codeSpan = img.codigo ? '<span class=\"card-codigo\">' + escHtml(img.codigo) + '</span>' : '';\n"
              + "    var priceSpan = img.precio ? '<span class=\"card-price\">' + escHtml(img.precio) + ' \\u20ac</span>' : '';\n"
              + "    var prices = [];\n"
              + "    if (img.checkboxes) { for (var j = 0; j < img.checkboxes.length; j++) { if (img.checkboxes[j].precio) prices.push(img.checkboxes[j].precio); } }\n"
              + "    var msgWeight = getCardMessageWeight(img);\n"
              + "    var iconClass = 'comment-icon';\n"
              + "    var iconTitle = 'Sin mensajes';\n"
              + "    if (msgWeight === 3) { iconClass += ' has-unread'; iconTitle = '\u00a1Tienes mensajes del taller!'; }\n"
              + "    else if (msgWeight === 2) { iconClass += ' has-read-pending'; iconTitle = 'Mensaje le\u00eddo'; }\n"
              + "    else if (msgWeight === 1) { iconClass += ' has-read'; iconTitle = 'Mensaje contestado'; }\n"
              
              
//              + "    var indicatorHtml = msgWeight === 3 ? '<span class=\"msg-indicator has-unread\"></span>' : (msgWeight === 1 ? '<span class=\"msg-indicator has-read\"></span>' : '');\n"
              + "var indicatorHtml = '';\n"
              
         + "    var commentHtml = '';\n"
              + "    var genStatus = getMessageStatus(img.comentario);\n"
              + "    if (genStatus !== 'NONE' || prices.length > 0) {\n"
              + "      var prHtml = '';\n"
              + "      if (prices.length === 1 && !img.precio) {\n"
              + "        prHtml = '<div class=\"card-prices\"><span class=\"price-tag\">' + escHtml(prices[0]) + ' \\u20ac</span></div>';\n"
              + "      } else if (prices.length > 0) {\n"
              + "        prHtml = '<div class=\"card-prices\" style=\"color:var(--text-muted);font-size:.75rem;\">Precios \\u27a1 clic</div>';\n"
              + "      }\n"
              + "      var commText = '';\n"
              + "      if (genStatus !== 'NONE') {\n"
              + "         var lastM = img.comentario.hilo[img.comentario.hilo.length-1];\n"
              + "         var deStr = lastM.de === 'nosotros' ? '<span style=\"color:#ffd700\">TALLER:</span> ' : '<span style=\"color:#3498db\">TÚ:</span> ';\n"
              + "         commText = '<div style=\"font-weight:600; font-size:.8rem; margin-top:2px;\">' + deStr + escHtml(lastM.texto) + '</div>';\n"
              + "      }\n"
              + "      commentHtml = '<div class=\"card-comment\">' + prHtml + commText + '</div>';\n"
              + "    }\n"
              + "    html += '<div class=\"card ' + estado + '\" data-index=\"' + i + '\">'\n"
              + "      + '  <div class=\"card-header\">'\n"
              + "      + '    ' + codeSpan\n"
              + "      + '    <span class=\"card-name\" onclick=\"openModal(' + i + ')\">' + escHtml(img.nombre) + '</span>'\n"
              + "      + '    ' + indicatorHtml\n"
              + "      + '    <span class=\"' + iconClass + '\" title=\"' + iconTitle + '\" onclick=\"openModal(' + i + ')\"></span>'\n"
              + "      + '    ' + priceSpan\n"
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
              + "  if (img.checkboxes) { for (var k = 0; k < img.checkboxes.length; k++) { img.checkboxes[k].estado = img.estado; } }\n"
              + "  renderGallery();\n"
              + "}\n"
              + "function escHtml(s) { return (s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\"/g,'&quot;'); }\n"
              + "function makeMsgClickHandler(texto) { return function() { document.getElementById('modalComentario').value = texto; document.getElementById('modalComentario').focus(); }; }\n"
              + "function updateCounter() {\n"
              + "  var sel = data.imagenes.filter(function(i) { return i.estado === 'SELECTED'; }).length;\n"
              + "  var dis = data.imagenes.filter(function(i) { return i.estado === 'DISCARDED'; }).length;\n"
              + "  var und = data.imagenes.filter(function(i) { return i.estado === 'UNDEFINED'; }).length;\n"
              + "  document.getElementById('counter').textContent = 'Total: ' + data.imagenes.length + ' | \\u2713 ' + sel + ' \\u2717 ' + dis + ' \\u25cb ' + und;\n"
              + "  document.getElementById('summary').textContent = '\\u2713 ' + sel + ' seleccionadas \\u00b7 \\u2717 ' + dis + ' descartadas \\u00b7 \\u25cb ' + und + ' sin marcar';\n"
              + "}\n"
        
              + "// === MODAL: ABRIR, OVERLAYS, ZOOM ===\n"
              + "function openModal(index) {\n"
              + "  currentIndex = index;\n"
              + "  var img = data.imagenes[index];\n"
              + "  document.getElementById('modalCodigo').textContent = img.codigo || '';\n"
              + "  document.getElementById('modalNombre').textContent = img.nombre;\n"
              + "  document.getElementById('modalImg').src = img.miniatura;\n"
              + "  setTristate(document.getElementById('modalCheckPrincipal'), img.estado || 'UNDEFINED');\n"
              + "  currentContext = { type: 'IMAGE', index: index, cbIndex: -1 }; renderChat();\n"
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
        
              + "// Posiciona los overlays de checkboxes sobre la imagen, escalando segun el contenedor\n"
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
              + "    var st = getMessageStatus(cb.comentario);\n"
              + "    var bubbleClass = 'cb-bubble';\n"
              + "    if (st === 'UNREAD') bubbleClass += ' unread';\n"
              + "    else if (st === 'PENDING') bubbleClass += ' read';\n"
              + "    else if (st === 'READ') bubbleClass += ' replied';\n"
              + "    else bubbleClass += ' empty';\n"
              + "    var bubbleOpacity = st === 'NONE' ? 'style=\"opacity:.7\"' : '';\n"
              + "    var bubbleHtml = '<span class=\"' + bubbleClass + '\" ' + bubbleOpacity + ' title=\"Comentarios\">\\u2709</span>';\n"
              + "    var priceHtml = cb.precio ? '<span class=\"cb-price\">' + escHtml(cb.precio) + '</span>' : '';\n"
              + "    var overlay = document.createElement('div');\n"
              + "    overlay.className = 'cb-overlay';\n"
              + "    overlay.style.left = x + 'px'; overlay.style.top = y + 'px';\n"
              + "    overlay.innerHTML = '<span class=\"cb-indicator ' + indicatorClass + '\">' + indicatorText + '</span>' + priceHtml + bubbleHtml;\n"
              + "    container.appendChild(overlay);\n"
              + "    overlay.addEventListener('click', function(e) {\n"
              + "      if (e.target.closest('.cb-bubble')) return;\n"
              + "      var idx = Array.prototype.indexOf.call(container.children, this);\n"
              + "      if (idx >= 0 && data.imagenes[currentIndex].checkboxes[idx]) {\n"
              + "        var cbData = data.imagenes[currentIndex].checkboxes[idx];\n"
              + "        cbData.estado = cycleState(cbData.estado || 'UNDEFINED');\n"
              + "        var hasSel = false, allDis = true;\n"
              + "        for(var k=0; k<data.imagenes[currentIndex].checkboxes.length; k++) {\n"
              + "          var cbSt = data.imagenes[currentIndex].checkboxes[k].estado || 'UNDEFINED';\n"
              + "          if (cbSt === 'SELECTED') hasSel = true;\n"
              + "          if (cbSt !== 'DISCARDED') allDis = false;\n"
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
              + "        currentContext = { type: 'CHECKBOX', index: currentIndex, cbIndex: idx }; renderChat();\n"
              + "      }\n"
              + "    });\n"
              + "  }\n"
              + "}\n"
              + "function cycleState(c) { return c === 'SELECTED' ? 'DISCARDED' : (c === 'DISCARDED' ? 'UNDEFINED' : 'SELECTED'); }\n"
              + "function setTristate(el, state) { el.className = 'tristate-cb state-' + state.toLowerCase(); el.textContent = state === 'SELECTED' ? '\\u2713' : (state === 'DISCARDED' ? '\\u2717' : '\\u25cb'); }\n"
              + "document.getElementById('modalClose').addEventListener('click', function() { guardarModal(); document.getElementById('modal').classList.add('hidden'); });\n"
              + "document.getElementById('modalCheckPrincipal').addEventListener('click', function() { if (currentIndex >= 0) { var img = data.imagenes[currentIndex]; img.estado = cycleState(img.estado || 'UNDEFINED'); if (img.checkboxes) { for (var k = 0; k < img.checkboxes.length; k++) { img.checkboxes[k].estado = img.estado; } setTristate(this, img.estado); positionOverlays(document.getElementById('checkboxOverlays'), img); renderGallery(); } } });\n"
              + "function ensureThread(img) {\n"
              + "  if (typeof img.comentario === 'object' && img.comentario && img.comentario.hilo) {\n"
              + "    if (typeof img.comentario.estadoPr === 'undefined') img.comentario.estadoPr = 0;\n"
              + "    if (typeof img.comentario.estadoHtml === 'undefined') img.comentario.estadoHtml = 0;\n"
              + "    return;\n"
              + "  }\n"
              + "  if (typeof img.comentario === 'string' && img.comentario.trim()) {\n"
              + "    img.comentario = {hilo: [{de: 'nosotros', texto: img.comentario}], estadoPr: 2, estadoHtml: 3};\n"
              + "  } else {\n"
              + "    img.comentario = {hilo: [], estadoPr: 0, estadoHtml: 0};\n"
              + "  }\n"
              + "}\n"

              + "// === CHAT UNIFICADO (imagen o checkbox segun currentContext) ===\n"
              + "function renderChat() {\n"
              + "  var chatDiv = document.getElementById('modalChatMessages');\n"
              + "  if (!chatDiv) return;\n"
              + "  var img = data.imagenes[currentContext.index];\n"
              + "  if (!img) return;\n"
              + "  var target = (currentContext.type === 'IMAGE') ? img : img.checkboxes[currentContext.cbIndex];\n"
              + "  if (!target) return;\n"
              + "  ensureThread(target);\n"
              + "  \n"
              + "  // ---> CAMBIO DE ESTADO AL LEER (EL CLIENTE ABRE EL MENSAJE) <---\n"
              + "  if (target.comentario && target.comentario.estadoHtml === 3) {\n"
              + "    target.comentario.estadoHtml = 1;\n" // 1: mensaje leído no contestado -> verde
              + "    target.comentario.estadoPr = 1;\n"   // 1: leido en Java también
              + "    renderGallery();\n" // Refresca galería principal
              + "  }\n"
              + "  \n"
              + "  var thread = (target.comentario && target.comentario.hilo) ? target.comentario.hilo : [];\n"
              + "  chatDiv.innerHTML = '';\n"
              + "  if (thread.length === 0) { chatDiv.innerHTML = '<div class=\"chat-empty\" style=\"color:#555;font-style:italic;font-size:.85rem;text-align:center;margin-top:15px;\">No hay mensajes</div>'; return; }\n"
              + "  thread.forEach(function(msg, i) {\n"
              + "    var div = document.createElement('div');\n"
              + "    div.className = 'chat-msg ' + (msg.de === 'nosotros' ? 'nosotros' : 'cliente');\n"
              + "    div.innerHTML = '<div>' + escHtml(msg.texto) + '</div>' + (msg._sessionMsg ? '<span class=\"btn-borrar\" onclick=\"borrarMsg('+i+')\">X</span>' : '');\n"
              + "    chatDiv.appendChild(div);\n"
              + "  });\n"
              + "  chatDiv.scrollTop = chatDiv.scrollHeight;\n"
              + "}\n"
        
              + "// Borra solo mensajes creados en esta sesion (_sessionMsg)\n"
              + "function borrarMsg(i) {\n"
              + "  var img = data.imagenes[currentContext.index];\n"
              + "  if (!img) return;\n"
              + "  var target = (currentContext.type === 'IMAGE') ? img : img.checkboxes[currentContext.cbIndex];\n"
              + "  if (!target || !target.comentario || !target.comentario.hilo) return;\n"
              + "  if (!target.comentario.hilo[i]._sessionMsg) return;\n"
              + "  target.comentario.hilo.splice(i, 1);\n"
              + "  \n"
              + "  // ---> RESTABLECER ESTADOS SI EL HILO QUEDA VACÍO <---\n"
              + "  if (target.comentario.hilo.length === 0) {\n"
              + "    target.comentario.estadoPr = 0;\n"
              + "    target.comentario.estadoHtml = 0;\n"
              + "  }\n"
              + "  \n"
              + "  renderChat();\n"
              + "  renderGallery();\n"
              + "}\n"
              + "document.getElementById('btnEnviarChat').onclick = function() {\n"
              + "  var ta = document.getElementById('modalComentario');\n"
              + "  if(!ta || !ta.value.trim()) return;\n"
              + "  var img = data.imagenes[currentContext.index];\n"
              + "  if (!img) return;\n"
              + "  var target = (currentContext.type === 'IMAGE') ? img : img.checkboxes[currentContext.cbIndex];\n"
              + "  if (!target) return;\n"
              + "  if(!target.comentario) target.comentario = {hilo:[]};\n"
              + "  if(!target.comentario.hilo) target.comentario.hilo = [];\n"
              + "  target.comentario.hilo.push({de: 'cliente', texto: ta.value, _sessionMsg: true});\n"
              + "  \n"
              + "  // ---> CAMBIO DE ESTADO AL RESPONDER EL CLIENTE <---\n"
              + "  target.comentario.estadoPr = 3;\n"   // 3: mensaje nuevo no leido (rojo para ti)
              + "  target.comentario.estadoHtml = 2;\n" // 2: mensaje contestado (azul para el cliente)
              + "  \n"
              + "  ta.value = '';\n"
              + "  renderChat();\n"
              + "  renderGallery();\n"
              + "};\n"
              + "document.getElementById('modalComentario').addEventListener('keydown', function(e) {\n"
              + "  if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); document.getElementById('btnEnviarChat').click(); }\n"
              + "});\n"
        
              + "// === ZOOM / PAN / TACTIL ===\n"
              + "function guardarModal() { if (currentIndex < 0) return; renderGallery(); }\n"
              + "function resetZoom() { zoomScale = 1; panX = 0; panY = 0; applyTransform(); }\n"
              + "function applyTransform() { var transformStr = 'translate(' + panX + 'px, ' + panY + 'px) scale(' + zoomScale + ')'; document.getElementById('modalImg').style.transform = transformStr; document.getElementById('checkboxOverlays').style.transform = transformStr; }\n"
              + "var wrap = document.getElementById('modalImageWrap');\n"
              + "document.getElementById('modalImg').addEventListener('dragstart', function(e) { e.preventDefault(); });\n"
              + "wrap.addEventListener('mousedown', function(e) { if (e.button !== 0) return; e.preventDefault(); isDragging = true; startX = e.clientX; startY = e.clientY; wrap.style.cursor = 'grabbing'; });\n"
              + "document.addEventListener('mousemove', function(e) { if (!isDragging) return; panX += (e.clientX - startX); panY += (e.clientY - startY); startX = e.clientX; startY = e.clientY; applyTransform(); });\n"
              + "document.addEventListener('mouseup', function() { isDragging = false; wrap.style.cursor = 'grab'; });\n"
				+ "wrap.addEventListener('wheel', function(e) {\n"
				+ "  e.preventDefault();\n"
				+ "  var rect = wrap.getBoundingClientRect();\n"
				+ "  var mouseX = e.clientX - rect.left;\n"
				+ "  var mouseY = e.clientY - rect.top;\n"
				+ "  var oldZoom = zoomScale;\n"
				+ "  var newZoom = Math.max(0.3, Math.min(6, oldZoom + (e.deltaY > 0 ? -0.15 : 0.15)));\n"
				+ "  if (newZoom !== oldZoom) {\n"
				+ "    var worldX = (mouseX - panX) / oldZoom;\n"
				+ "    var worldY = (mouseY - panY) / oldZoom;\n"
				+ "    zoomScale = newZoom;\n"
				+ "    panX = mouseX - worldX * newZoom;\n"
				+ "    panY = mouseY - worldY * newZoom;\n"
				+ "    applyTransform();\n"
				+ "  }\n"
				+ "}, { passive: false });\n"
              + "wrap.addEventListener('touchstart', function(e) { if (e.touches.length === 1) { isDragging = true; startX = e.touches[0].clientX; startY = e.touches[0].clientY; var now = Date.now(); if (now - lastTapTime < 300) { e.preventDefault(); if (zoomScale > 1.5) resetZoom(); else { zoomScale = 2.5; applyTransform(); } lastTapTime = 0; } else { lastTapTime = now; } } else if (e.touches.length === 2) { e.preventDefault(); var dx = e.touches[0].clientX - e.touches[1].clientX; var dy = e.touches[0].clientY - e.touches[1].clientY; lastTouchDist = Math.sqrt(dx*dx + dy*dy); } }, { passive: false });\n"
              + "wrap.addEventListener('touchmove', function(e) { if (e.touches.length === 1 && isDragging) { e.preventDefault(); panX += (e.touches[0].clientX - startX); panY += (e.touches[0].clientY - startY); startX = e.touches[0].clientX; startY = e.touches[0].clientY; applyTransform(); } else if (e.touches.length === 2) { e.preventDefault(); var dx = e.touches[0].clientX - e.touches[1].clientX; var dy = e.touches[0].clientY - e.touches[1].clientY; var dist = Math.sqrt(dx*dx + dy*dy); if (lastTouchDist > 0) { zoomScale = Math.max(0.3, Math.min(6, zoomScale * (dist / lastTouchDist))); applyTransform(); } lastTouchDist = dist; } }, { passive: false });\n"
              + "wrap.addEventListener('touchend', function(e) { if (e.touches.length < 2) lastTouchDist = 0; if (e.touches.length === 0) isDragging = false; });\n"
         
              + "// === BOTONES DE RESPUESTA: COPIAR Y DESCARGAR ===\n"
              + "document.getElementById('btnCopiar').addEventListener('click', function() { var respuesta = buildResponse(); var text = JSON.stringify(respuesta, null, 2); if (navigator.clipboard && navigator.clipboard.writeText) { navigator.clipboard.writeText(text).then(function() { showToast('Respuesta copiada al portapapeles'); }).catch(function() { fallbackCopy(text); }); } else { fallbackCopy(text); } });\n"
              + "function fallbackCopy(text) { var ta = document.createElement('textarea'); ta.value = text; ta.style.position = 'fixed'; ta.style.opacity = '0'; document.body.appendChild(ta); ta.select(); try { document.execCommand('copy'); showToast('Respuesta copiada'); } catch(e) { alert('No se pudo copiar. Selecciona el texto manualmente.'); } document.body.removeChild(ta); }\n"
              + "document.getElementById('btnDescargar').addEventListener('click', function() { var respuesta = buildResponse(); var json = JSON.stringify(respuesta, null, 2); var blob = new Blob([json], {type: 'application/json'}); var a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = '" + respuestaFilename + "'; a.click(); URL.revokeObjectURL(a.href); });\n"
              + "function ensureThreadObj(c) { if (typeof c === 'object' && c && c.hilo) return c; if (typeof c === 'string' && c.trim()) return {hilo: [{de: 'cliente', texto: c}]}; return {hilo: []}; }\n"
         
              + "// Genera el JSON de respuesta con estados y comentarios\n"
              + "function buildResponse() {\n"
              + "  guardarModal();\n"
              + "  var sortedData = data.imagenes.slice().sort(function(a,b){ return a._origIndex - b._origIndex; });\n"
              + "  for (var i = 0; i < sortedData.length; i++) {\n"
              + "    ensureThread(sortedData[i]);\n"
              + "    if (sortedData[i].checkboxes) { for (var j = 0; j < sortedData[i].checkboxes.length; j++) { sortedData[i].checkboxes[j].comentario = ensureThreadObj(sortedData[i].checkboxes[j].comentario); } }\n"
              + "  }\n"
              + "  return { projectName: data.projectName, iteracion: data.iteracion, fechaEnvio: data.fechaEnvio, fechaRespuesta: new Date().toISOString(), respuestas: sortedData.map(function(img) { return { id: img.id, codigo: img.codigo || '', estado: img.estado || 'DISCARDED', comentario: img.comentario, checkboxes: (img.checkboxes || []).map(function(cb) { return { codigo: cb.codigo || '', estado: cb.estado || 'UNDEFINED', comentario: cb.comentario }; }) }; }) }; }\n"
              + "function showToast(msg) { var t = document.createElement('div'); t.textContent = msg; t.style.cssText = 'position:fixed;bottom:80px;left:50%;transform:translateX(-50%);background:#333;color:#fff;padding:8px 16px;border-radius:6px;font-size:.85rem;z-index:200;opacity:0;transition:opacity .3s'; document.body.appendChild(t); requestAnimationFrame(function() { t.style.opacity = '1'; }); setTimeout(function() { t.style.opacity = '0'; setTimeout(function() { t.remove(); }, 300); }, 2000); }\n";

    } // --- Fin del Metodo getClientJs --- 
    
    
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
            sb.append("      \"comentario\": ").append(jsonThread(pi.getCommentThreadAccess(), comment)).append(",\n"); // Usar CommentThread
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
                sb.append("          \"comentario\": ").append(jsonThread(cb.getCommentThreadAccess(), cb.getComment())).append("\n"); // Usar CommentThread
                sb.append("        }");
            }
            sb.append("\n      ]\n");
            sb.append("    }");
            i++;
        }

        sb.append("\n  ]\n}");
        return sb.toString();
    
    } // --- Fin del Metodo generarDataJson --- 

    
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
    
    } // --- Fin del Metodo generarRespuestaVacia --- 

    
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
    
    } // --- Fin del Metodo generarHtml --- 

    
    private String getCss() {
        return "body { background: #0f0f1a; color: #f0f0f0; }";
    
    } // --- Fin del Metodo getCss ---
    

    private String getJs(String respuestaFilename, String prjclFilename) {
        return "console.log('Use exportarHtmlCliente for fully embedded features');";
    
    } // --- Fin del Metodo getJs ---

    
    private String nombreMiniatura(Path original) {
        String ext = obtenerExtension(original);
        boolean salidaJpeg = forceJpeg || "jpg".equals(ext) || "jpeg".equals(ext);
        String outExt = salidaJpeg ? "jpg" : ext;
        return sanitizarNombre(original.getFileName() != null ? original.getFileName().toString() : "imagen") + "_thumb." + outExt;
    
    } // --- Fin del Metodo nombreMiniatura ---

    
    private String generarId(Path ruta) {
        String nombre = ruta.getFileName() != null ? ruta.getFileName().toString() : ruta.toString();
        long size = 0;
        try { if (Files.exists(ruta)) size = Files.size(ruta); } catch (IOException ignored) {}
        return sanitizarNombre(nombre) + "_" + size;
        
    } // --- Fin del Metodo generarId ---

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
        
    } // --- Fin del Metodo obtenerExtension ---

    
    private String jsonString(String valor) {
        if (valor == null) return "null";
        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
        
    } // --- Fin del Metodo jsonString ---

    
    private String jsonThread(CommentThread thread, String oldComment) {
        if (thread != null && !thread.isEmpty()) {
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"estadoPr\":").append(thread.getEstadoPr()).append(",");
            sb.append("\"estadoHtml\":").append(thread.getEstadoHtml()).append(",");
            sb.append("\"hilo\":[");
            for (int i = 0; i < thread.getMessages().size(); i++) {
                Mensaje m = thread.getMessages().get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"de\":").append(jsonString(m.de()))
                  .append(",\"texto\":").append(jsonString(m.texto())).append("}");
            }
            sb.append("]}");
            return sb.toString();
        }
        if (thread != null) {
            return "{\"estadoPr\":" + thread.getEstadoPr() + ",\"estadoHtml\":" + thread.getEstadoHtml() + ",\"hilo\":[]}";
        }
        return jsonString(oldComment);
        
    } // --- Fin del Metodo jsonThread ---

    
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    
    } // --- Fin del Metodo escapeHtml ---
    
    
} // --- Fin de clase WebCatalogExporter ---
