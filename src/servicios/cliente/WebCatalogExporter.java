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
        if (imageCount <= 20)  return new QualityLevel(800, 0.85);
        if (imageCount <= 50)  return new QualityLevel(600, 0.75);
        return new QualityLevel(400, 0.60);
    
    } // --- Fin del Metodo determinarCalidad ---

    
    public void exportarHtmlCliente(ProjectModel project, Path outputFile, int iteracion) throws IOException {
        this.exportarHtmlCliente(project, outputFile, iteracion, null);
    
    } // --- Fin del Metodo exportarHtmlCliente ---

    
    public void exportarHtmlCliente(ProjectModel project, Path outputFile, int iteracion,
                                    java.util.function.Consumer<Integer> progressCallback) throws IOException {
        logger.info("[WebCatalogExporter] Generando HTML cliente único: {}", outputFile);

        java.util.List<String> imageKeys = new java.util.ArrayList<>();
        for (var pi : project.getMasterImages().values()) {
            if (true) {
                imageKeys.add(pi.getRutaImagen());
            }
        }
        if (imageKeys.isEmpty()) {
            throw new IOException("El proyecto no tiene imágenes.");
        }

        int total = imageKeys.size();
        QualityLevel quality = determinarCalidad(total);

        StringBuilder imagesJson = new StringBuilder();
        imagesJson.append("[");
        String projectName = project.getProjectName() != null ? project.getProjectName() : "Proyecto";
        StringBuilder staticGallery = new StringBuilder();

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

            String estadoStr = state != null ? state.name() : "UNDEFINED";
            String tristateClass = estadoStr.equals("SELECTED") ? "state-selected" : (estadoStr.equals("DISCARDED") ? "state-discarded" : "state-undefined");
            String tristateChar = estadoStr.equals("SELECTED") ? "&#x2713;" : (estadoStr.equals("DISCARDED") ? "&#x2717;" : "&#x25cb;");
            
            staticGallery.append("<div class=\"card ").append(estadoStr).append("\">\n");
            staticGallery.append("  <div class=\"card-header\">\n");
            staticGallery.append("    <span class=\"card-tristate ").append(tristateClass).append("\">").append(tristateChar).append("</span>\n");
            if (imageCode != null && !imageCode.isEmpty()) {
                staticGallery.append("    <span class=\"card-codigo\">").append(escapeHtml(imageCode)).append("</span>\n");
            }
            staticGallery.append("    <span class=\"card-name\">").append(escapeHtml(nombre)).append("</span>\n");
            
            if (pi.getPrice() > 0) {
                staticGallery.append("    <span class=\"card-price\">").append(String.format(java.util.Locale.US, "%.2f", pi.getPrice())).append(" &euro;</span>\n");
            }
            
            java.util.List<String> prices = new java.util.ArrayList<>();
            if (overlays != null) {
                for (var cb : overlays) {
                    if (cb.getPrice() > 0) prices.add(String.format(java.util.Locale.US, "%.2f", cb.getPrice()));
                }
            }
            
            CommentThread ct = pi.getCommentThreadAccess();
            int maxSt = ct != null ? ct.getEstadoHtml() : 0;
            if (overlays != null) {
                for (var cb : overlays) {
                    CommentThread cbCt = cb.getCommentThreadAccess();
                    if (cbCt != null && cbCt.getEstadoHtml() > maxSt) {
                        maxSt = cbCt.getEstadoHtml();
                    }
                }
            }
            String iconClass = "comment-icon";
            String iconTitle = "Sin mensajes";
            if (maxSt == 3) { iconClass += " has-unread"; iconTitle = "\u00a1Tienes mensajes del taller!"; }
            else if (maxSt == 1) { iconClass += " has-read-pending"; iconTitle = "Mensaje le\u00eddo"; }
            else if (maxSt == 2) { iconClass += " has-read"; iconTitle = "Mensaje contestado"; }
            
            staticGallery.append("    <span class=\"").append(iconClass).append("\" title=\"").append(iconTitle).append("\"></span>\n");
            staticGallery.append("  </div>\n");
            
            int st = ct != null ? ct.getEstadoHtml() : 0;
            String genStatus = "NONE";
            if (st == 3) genStatus = "UNREAD";
            else if (st == 1) genStatus = "PENDING";
            else if (st == 2) genStatus = "READ";
            
            if (!genStatus.equals("NONE") || !prices.isEmpty()) {
                staticGallery.append("  <div class=\"card-comment\">\n");
                if (prices.size() == 1 && pi.getPrice() <= 0) {
                    staticGallery.append("    <div class=\"card-prices\"><span class=\"price-tag\">").append(prices.get(0)).append(" &euro;</span></div>\n");
                } else if (!prices.isEmpty()) {
                    staticGallery.append("    <div class=\"card-prices\" style=\"color:var(--text-muted);font-size:.75rem;\">Precios &#x27a1; clic</div>\n");
                }
                
                if (!genStatus.equals("NONE") && ct != null && !ct.isEmpty()) {
                    modelo.proyecto.Mensaje lastM = ct.getLast();
                    String deStr = "nosotros".equals(lastM.de()) ? "<span style=\"color:#ffd700\">TALLER:</span> " : "<span style=\"color:#3498db\">TÚ:</span> ";
                    staticGallery.append("    <div style=\"font-weight:600; font-size:.8rem; margin-top:2px;\">").append(deStr).append(escapeHtml(lastM.texto())).append("</div>\n");
                }
                staticGallery.append("  </div>\n");
            }
            
            staticGallery.append("  <img src=\"data:image/jpeg;base64,").append(base64).append("\" alt=\"").append(escapeHtml(nombre)).append("\">\n");
            staticGallery.append("</div>\n");

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
        String htmlContent = generarHtmlCliente(projectName, dataJson.toString(), respuestaFilename, iteracion, staticGallery.toString());

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

    
    private String generarHtmlCliente(String projectName, String dataJson, String respuestaFilename, int iteracion, String staticGallery) {
        return "<!DOCTYPE html>\n"
            + "<html lang=\"es\">\n"
            + "<head>\n"
            + "  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes\">\n"
            + "  <title>" + escapeHtml(projectName) + " \u2013 Cat\u00e1logo</title>\n"
            + "  <style>\n" + getClientCss() + "  </style>\n"
            + "</head>\n"
            + "<body>\n"
            + "  <script>\nvar CATALOG_DATA = " + dataJson + ";\n  </script>\n"
            + "  <header>\n"
            + "    <h1>" + escapeHtml(projectName) + "</h1>\n"
            + "    <p class=\"subtitle\">Iteraci\u00f3n #" + iteracion + " \u00b7 Marca los modelos que te interesan y revisa los mensajes</p>\n"
            + "    <div style=\"font-size:0.7rem; color:var(--text-muted); margin-top:10px;\">Cat\u00e1logo generado con ModelTag by Fco. Javier Tortorsa</div>\n"
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
            + "      <button id=\"btnCompartir\" class=\"btn-primary\">Compartir respuesta</button>\n"
            + "      <button id=\"btnDescargar\" class=\"btn-secondary\">Descargar respuesta</button>\n"
            + "    </div>\n"
            + "  </nav>\n"
            + "  <noscript>\n"
            + "    <div style=\"background:#e74c3c; color:white; padding:15px; text-align:center; font-weight:bold; font-size:1rem; border-bottom:2px solid #c0392b;\">\n"
            + "      \u26a0\ufe0f EST\u00c1S EN MODO VISTA PREVIA<br>\n"
            + "      Para poder seleccionar fotos y usar el bot\u00f3n de Compartir, debes abrir este archivo en Safari (pulsa el bot\u00f3n Compartir de iOS y elige \"Abrir en Safari\").\n"
            + "    </div>\n"
            + "  </noscript>\n"
            + "  <main id=\"gallery\" class=\"gallery\">\n"
            + staticGallery
            + "  </main>\n"
            + "  <footer>\n"
            + "    <div style=\"font-size:0.7rem; color:var(--text-muted); padding:10px;\">Cat\u00e1logo generado con ModelTag by Fco. Javier Tortorsa</div>\n"
            + "  </footer>\n"
            + "  <div id=\"modal\" class=\"modal hidden\">\n"
            + "    <div class=\"modal-content\">\n"
            + "      <button id=\"modalClose\" class=\"modal-close\">&times;</button>\n"
            + "      <div class=\"modal-header\">\n"
            + "        <div class=\"modal-info\">\n"
            + "          <span class=\"tristate-cb\" id=\"modalCheckPrincipal\" tabindex=\"0\" title=\"Seleccionar/Descartar\">&#x25cb;</span>\n"
            + "          <span class=\"modal-codigo\" id=\"modalCodigo\"></span>\n"
            + "          <span class=\"modal-nombre\" id=\"modalNombre\"></span>\n"
            + "        </div>\n"
            + "      </div>\n"
            + "      <div class=\"modal-body-layout\">\n"
            + "        <div id=\"modalImageWrap\" class=\"modal-image-wrap\">\n"
            + "          <img id=\"modalImg\" src=\"\" alt=\"\" class=\"modal-image\" draggable=\"false\">\n"
            + "          <div id=\"checkboxOverlays\" class=\"checkbox-overlays\"></div>\n"
            + "        </div>\n"
            + "        <div class=\"modal-chat-sidebar collapsed\">\n"
            + "          <div class=\"modal-chat-title\" onclick=\"toggleChat()\"><span id=\"modalChatTitleText\">Chat general de la imagen</span><span id=\"chatToggle\" class=\"chat-toggle\">^</span></div>\n"
            + "          <div id=\"modalChatMessages\" class=\"chat-messages\"></div>\n"
            + "          <div class=\"chat-input-row\">\n"
            + "            <textarea id=\"modalComentario\" rows=\"2\" placeholder=\"Escribe aquí y pulsa Enviar (o Intro)...\"></textarea>\n"
            + "            <button id=\"btnEnviarChat\" class=\"btn-enviar\">Enviar</button>\n"
            + "          </div>\n"
            + "        </div>\n"
            + "      </div>\n"
            + "    </div>\n"
            + "  </div>\n"
            + "  <script>\n" + getClientJs(respuestaFilename, projectName) + "  </script>\n"
            + "</body>\n"
            + "</html>\n";
        
    } // --- Fin del Metodo generarHtmlCliente ---
    
    
    private String getClientCss() {
         return "/* === RESET & COLOR VARIABLES === */\n"
              + "*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }\n"
              + ":root { --bg: #0f0f1a; --card-bg: #222240; --text: #f0f0f0; --accent: #3a3a6a; --red: #ff3333; --green: #33cc33; --blue: #3399ff; --gray: #aaa; --selected: #2ecc71; --selected-bg: rgba(46,204,113,.12); --discarded: #e74c3c; --discarded-bg: rgba(231,76,60,.12); --undefined: #f39c12; --undefined-bg: rgba(243,156,18,.08); --text-muted: #999; --radius: 8px; --surface: #1a1a2e; }\n"
              + "body { background: var(--bg); color: var(--text); font-family: sans-serif; overscroll-behavior-y: contain; }\n"
              
              + "/* === HEADER & TOPBAR === */\n"
              + "header { text-align: center; padding: 2rem; }\n"
              + "#topbar { position: sticky; top: 0; z-index: 50; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 15px; padding: .8rem 1rem; background: rgba(26,26,46,0.95); border-bottom: 1px solid #2a2a4a; }\n"
              + ".topbar-section { display: flex; align-items: center; gap: 10px; }\n"
              + "#counter { color: var(--text-muted); font-size: .9rem; font-weight: bold; }\n"
              + ".sort-controls { background: var(--card-bg); padding: 5px 10px; border-radius: 6px; border: 1px solid var(--accent); }\n"
              + ".control-select { background: var(--bg); color: var(--text); border: 1px solid #3a3a6a; padding: .4rem; border-radius: 4px; outline: none; font-size: .85rem; cursor: pointer; }\n"
              + ".btn-icon { background: var(--bg); color: var(--text); border: 1px solid #3a3a6a; padding: .3rem .6rem; border-radius: 4px; cursor: pointer; font-size: 1.1rem; min-width: 44px; min-height: 44px; display: inline-flex; align-items: center; justify-content: center; }\n"
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

              + "/* === ICONO DE COMENTARIO === */\n"
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
               + ".tristate-cb { display: inline-flex; align-items: center; justify-content: center; width: 24px; height: 24px; border-radius: 4px; cursor: pointer; font-size: 1rem; font-weight: 700; user-select: none; background: var(--bg); border: 2px solid var(--accent); flex-shrink: 0; }\n"
               + ".tristate-cb.state-selected { background: var(--selected-bg); color: var(--selected); border-color: var(--selected); }\n"
               + ".tristate-cb.state-discarded { background: var(--discarded-bg); color: var(--discarded); border-color: var(--discarded); }\n"
               + ".tristate-cb.state-undefined { background: var(--undefined-bg); color: var(--undefined); border-color: var(--undefined); }\n"
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
              + ".count-icon { display: inline-flex; align-items: center; justify-content: center; width: 16px; height: 16px; border: 1px solid #555; border-radius: 3px; font-size: 0.7rem; margin: 0 4px; vertical-align: middle; }\n"
              + ".count-sel { background: var(--selected-bg); color: var(--selected); border-color: var(--selected); }\n"
              + ".count-dis { background: var(--discarded-bg); color: var(--discarded); border-color: var(--discarded); }\n"
              + ".count-und { background: var(--undefined-bg); color: var(--undefined); border-color: var(--undefined); }\n"
          
              + "/* === MODAL & CONTROLES === */\n"
              + "body.modal-open { overflow: hidden; position: fixed; width: 100%; height: 100%; }\n"
              + ".modal { position: fixed; top: 0; left: 0; width: 100%; height: 100dvh; background: rgba(0,0,0,0.9); display: flex; align-items: center; justify-content: center; z-index: 100; }\n"
              + ".modal.hidden { display: none !important; }\n"
              + ".hidden { display: none !important; }\n"
              + ".modal-content { background: var(--card-bg); width: 95%; max-width: 1200px; padding: 20px; border-radius: 8px; position: relative; max-height: 95vh; display: flex; flex-direction: column; border: 1px solid #3a3a5a; }\n"
              + ".modal-close { position: absolute; top: 10px; right: 15px; cursor: pointer; font-size: 2rem; color: #fff; border: none; background: none; z-index: 10;}\n"
              + ".modal-header { margin-bottom: .8rem; padding-right: 30px; }\n"
              + ".modal-info { display: flex; align-items: center; gap: .5rem; }\n"
              + ".modal-nombre { font-size: 1.05rem; font-weight: 600; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }\n"
              + ".modal-codigo { display: inline-block; background: #000; color: #fff; padding: 1px 8px; border-radius: 3px; font-size: .7rem; font-weight: 700; flex-shrink: 0; }\n"
              + ".modal-body-layout { display: flex; gap: 15px; flex: 1; min-height: 0; }\n"
              + ".modal-image-wrap { flex: 2; overflow: hidden; border-radius: 6px; background: #000; position: relative; cursor: grab; touch-action: none; contain: layout; }\n"
              + ".modal-image-wrap:active { cursor: grabbing; }\n"
              + ".modal-image { width: 100%; height: 100%; object-fit: contain; display: block; transform-origin: 0 0; user-select: none; -webkit-user-select: none; pointer-events: none; }\n"
              + ".checkbox-overlays { position: absolute; top: 0; left: 0; pointer-events: none; transform-origin: 0 0; contain: layout; }\n"
       
              + "/* === OVERLAYS DE CHECKBOX + BURBUJA DE MENSAJES === */\n"
              + ".cb-overlay { position: absolute; display: flex; align-items: center; gap: 3px; pointer-events: auto; transform: translate(-50%, -50%); background: rgba(0,0,0,.85); border-radius: 4px; padding: 2px 5px; white-space: nowrap; cursor: pointer; border: 1px solid rgba(255,255,255,.2); user-select: none; }\n"
              + ".cb-overlay.active { outline: 2px solid #ffd700; box-shadow: 0 0 10px rgba(255,215,0,.7); }\n"
              + ".cb-overlay .cb-indicator { display: inline-flex; align-items: center; justify-content: center; width: 18px; height: 18px; border-radius: 3px; font-weight: 700; font-size: .7rem; }\n"
              + ".cb-overlay .cb-indicator.sel { background: var(--selected); color: #000; }\n"
              + ".cb-overlay .cb-indicator.dis { background: var(--discarded); color: #fff; }\n"
              + ".cb-overlay .cb-indicator.und { background: transparent; color: var(--undefined); border: 2px solid var(--undefined); }\n"
              + ".cb-overlay .cb-codigo { color: var(--text-muted); font-size: .65rem; }\n"
              + ".cb-overlay .cb-price { color: #fff; font-size: .7rem; font-weight: 600; }\n"
          
              + ".cb-overlay .cb-bubble { color: #aaa; font-size: 1.6rem; cursor: pointer; width: 44px; height: 44px; display: inline-flex; align-items: center; justify-content: center; transition: all .3s; }\n"
              + ".cb-overlay .cb-bubble:hover { color: #fff; }\n"
              + ".cb-overlay .cb-bubble.read { color: var(--green); }\n"
              + ".cb-overlay .cb-bubble.unread { color: var(--red); text-shadow: 0 0 6px rgba(255,51,51,.6); animation: pulse-comment 2s infinite; }\n"
              + ".cb-overlay .cb-bubble.replied { color: var(--blue); }\n"
              + ".cb-overlay .cb-bubble.empty { color: var(--gray); }\n"
              + "@keyframes pulse-comment { 0% { text-shadow: 0 0 4px rgba(255,215,0,.4); } 50% { text-shadow: 0 0 12px rgba(255,215,0,.9); } 100% { text-shadow: 0 0 4px rgba(255,215,0,.4); } }\n"
         
              + "/* === CHAT LATERAL EN MODAL === */\n"
              + ".modal-chat-sidebar { flex: 1; min-width: 320px; max-width: 400px; display: flex; flex-direction: column; background: var(--surface); padding: 12px; border-radius: 8px; border: 1px solid var(--accent); }\n"
              + ".modal-chat-title { font-size: .85rem; color: var(--text-muted); text-transform: uppercase; margin-bottom: 8px; font-weight: bold; letter-spacing: .5px; border-bottom: 1px solid #3a3a6a; padding-bottom: 6px; display: flex; align-items: center; justify-content: space-between; }\n"
              + ".chat-toggle { display: none; cursor: pointer; font-size: 1rem; color: var(--text-muted); padding: 0 4px; user-select: none; }\n"
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
        
              + "/* === RESPONSIVE (TRANSFORMACIÓN A BOTTOM SHEET) === */\n"
              + "@media (max-width: 800px) {\n"
              + "  .modal-content { height: 85vh; height: 82dvh !important; padding: 12px; } /* Evita el colapso de la altura del modal en móvil */\n"
              + "  .modal-body-layout { position: relative; flex: 1; min-height: 0; display: block; }\n"
              + "  .modal-image-wrap { position: absolute; inset: 0; width: 100%; height: 100%; min-height: 0; flex: none; }\n"
              + "  .modal-chat-sidebar {\n"
              + "    position: absolute; bottom: 0; left: 0; width: 100%; height: 60%;\n"
              + "    background: var(--surface); border-top: 2px solid var(--accent); border-radius: 12px 12px 0 0; z-index: 10;\n"
              + "    transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1); display: flex; flex-direction: column;\n"
              + "    max-width: 100%; min-width: 0; padding: 10px; transform: translateY(0);\n"
              + "  }\n"
              + "  /* Estado contraído de la persiana en móvil */\n"
              + "  .modal-chat-sidebar.collapsed { transform: translateY(calc(100% - 40px)); }\n"
              + "  .chat-toggle { display: inline-block; cursor: pointer; font-size: 1.2rem; color: var(--text-muted); padding: 0 10px; user-select: none; }\n"
              + "  .chat-messages { flex: 1; min-height: 0; display: flex; }\n"
              + "  .modal-chat-title { height: 40px; font-size: .85rem; margin-bottom: 4px; padding-bottom: 3px; cursor: pointer; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #3a3a6a; }\n"
              + "  .chat-input-row textarea, .chat-input-row input { font-size: 16px !important; min-height: 2.6rem; }\n"
              + "  .btn-enviar { padding: .5rem; font-size: .85rem; }\n"
              + "}\n"
              + "@media (max-width: 600px) {\n"
              + "  #topbar { flex-direction: column; align-items: stretch; text-align: center; }\n"
              + "  .topbar-section { justify-content: center; flex-wrap: wrap; }\n"
              + "  .gallery { grid-template-columns: repeat(2, 1fr); gap: .5rem; padding: .5rem; }\n"
              + "  .footer-buttons { flex-direction: column; width: 100%; }\n"
              + "  .btn-primary, .btn-secondary { width: 100%; }\n"
              + "}\n";
        
    } // --- Fin del Metodo getClientCss --- 

    

    private String getClientJs(String respuestaFilename, String projectName) {
         return "// === VARIABLES GLOBALES ===\n"
               + "window.onerror = function(msg, url, line) { var d=document.createElement('div'); d.style.cssText='position:fixed;top:0;left:0;right:0;z-index:9999;background:#c00;color:#fff;padding:10px;font-size:14px;word-break:break-all'; d.textContent='Error L'+line+': '+msg; document.body.appendChild(d); };\n"
               + "var data = (typeof CATALOG_DATA !== 'undefined') ? CATALOG_DATA : { imagenes: [] };\n"
               + "var currentIndex = -1;\n"
              + "var currentContext = { type: 'IMAGE', index: 0, cbIndex: -1 };\n"
              + "var zoomScale = 1, panX = 0, panY = 0, isDragging = false, startX = 0, startY = 0, lastTouchDist = 0, lastTapTime = 0;\n"
               + "for(var i=0; i<data.imagenes.length; i++) { data.imagenes[i]._origIndex = i; }\n"
              
               + "// === PERSISTENCIA LOCALSTORAGE ===\n"
               + "function getStorageKey() {\n"
               + "  return 'cat_' + (data.projectName||'').replace(/[^a-z0-9]/gi,'_') + '_it' + data.iteracion + '_' + (data.fechaEnvio||'').replace(/[^a-z0-9_-]/gi,'');\n"
               + "}\n"
               + "function saveState() {\n"
               + "  var out = [];\n"
               + "  for (var i = 0; i < data.imagenes.length; i++) {\n"
               + "    var img = data.imagenes[i];\n"
               + "    var si = { codigo: img.codigo, estado: img.estado || 'UNDEFINED' };\n"
               + "    var has = false;\n"
               + "    if (img.comentario && img.comentario.hilo && img.comentario.hilo.length > 0) {\n"
               + "      si.hilo = JSON.parse(JSON.stringify(img.comentario.hilo));\n"
               + "      si.estadoPr = img.comentario.estadoPr || 0;\n"
               + "      si.estadoHtml = img.comentario.estadoHtml || 0;\n"
               + "      has = true;\n"
               + "    }\n"
               + "    if (si.estado !== 'UNDEFINED') has = true;\n"
               + "    var cbs = [];\n"
               + "    if (img.checkboxes && img.checkboxes.length > 0) {\n"
               + "      for (var j = 0; j < img.checkboxes.length; j++) {\n"
               + "        var cb = img.checkboxes[j];\n"
               + "        var scb = { codigo: cb.codigo, estado: cb.estado || 'UNDEFINED' };\n"
               + "        var cbHas = false;\n"
               + "        if (cb.comentario && cb.comentario.hilo && cb.comentario.hilo.length > 0) {\n"
               + "          scb.hilo = JSON.parse(JSON.stringify(cb.comentario.hilo));\n"
               + "          scb.estadoPr = cb.comentario.estadoPr || 0;\n"
               + "          scb.estadoHtml = cb.comentario.estadoHtml || 0;\n"
               + "          cbHas = true;\n"
               + "        }\n"
               + "        if (scb.estado !== 'UNDEFINED') cbHas = true;\n"
               + "        if (cbHas) cbs.push(scb);\n"
               + "      }\n"
               + "    }\n"
               + "    if (cbs.length > 0) { si.checkboxes = cbs; has = true; }\n"
               + "    if (has) out.push(si);\n"
               + "  }\n"
               + "  try { localStorage.setItem(getStorageKey(), JSON.stringify({ projectName: data.projectName, iteracion: data.iteracion, fechaEnvio: data.fechaEnvio, imagenes: out })); } catch(e) {}\n"
               + "}\n"
               + "function restoreState() {\n"
               + "  var saved;\n"
               + "  try { saved = JSON.parse(localStorage.getItem(getStorageKey())); } catch(e) { return; }\n"
               + "  if (!saved || !saved.imagenes) return;\n"
               + "  if (saved.iteracion !== data.iteracion || saved.fechaEnvio !== data.fechaEnvio) { try { localStorage.removeItem(getStorageKey()); } catch(e) {} return; }\n"
               + "  for (var a = 0; a < saved.imagenes.length; a++) {\n"
               + "    var si = saved.imagenes[a];\n"
               + "    var img = null;\n"
               + "    if (si.codigo) { for (var b = 0; b < data.imagenes.length; b++) { if (data.imagenes[b].codigo === si.codigo) { img = data.imagenes[b]; break; } } }\n"
               + "    if (!img && a < data.imagenes.length) img = data.imagenes[a];\n"
               + "    if (!img) continue;\n"
               + "    if (si.estado !== undefined) img.estado = si.estado;\n"
               + "    if (si.hilo) { img.comentario = { hilo: JSON.parse(JSON.stringify(si.hilo)), estadoPr: si.estadoPr||0, estadoHtml: si.estadoHtml||0 }; }\n"
               + "    if (si.checkboxes && img.checkboxes) {\n"
               + "      for (var c = 0; c < si.checkboxes.length; c++) {\n"
               + "        var scb = si.checkboxes[c];\n"
               + "        var cb = null;\n"
               + "        if (scb.codigo) { for (var d = 0; d < img.checkboxes.length; d++) { if (img.checkboxes[d].codigo === scb.codigo) { cb = img.checkboxes[d]; break; } } }\n"
               + "        if (cb && scb.estado !== undefined) cb.estado = scb.estado;\n"
               + "        if (cb && scb.hilo) { cb.comentario = { hilo: JSON.parse(JSON.stringify(scb.hilo)), estadoPr: scb.estadoPr||0, estadoHtml: scb.estadoHtml||0 }; }\n"
               + "      }\n"
               + "    }\n"
               + "  }\n"
               + "}\n"
               + "\n"
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
               + "try { restoreState(); } catch(e) { console.error('Error restoring state:', e); }\n"
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
              + "  gallery.innerHTML = '';\n"
              + "  for (var i = 0; i < data.imagenes.length; i++) {\n"
              + "    var img = data.imagenes[i];\n"
              + "    var estado = img.estado || 'UNDEFINED';\n"
              + "    var tristateClass = estado === 'SELECTED' ? 'state-selected' : (estado === 'DISCARDED' ? 'state-discarded' : 'state-undefined');\n"
              + "    var tristateChar = estado === 'SELECTED' ? '\\u2713' : (estado === 'DISCARDED' ? '\\u2717' : '\\u25cb');\n"
              + "    \n"
              + "    var card = document.createElement('div');\n"
              + "    card.className = 'card ' + estado;\n"
              + "    card.dataset.index = i;\n"
              + "    \n"
              + "    var header = document.createElement('div');\n"
              + "    header.className = 'card-header';\n"
              + "    \n"
              + "    var spanTristate = document.createElement('span');\n"
              + "    spanTristate.className = 'card-tristate ' + tristateClass;\n"
              + "    spanTristate.textContent = tristateChar;\n"
              + "    spanTristate.tabIndex = 0;\n"
              + "    spanTristate.dataset.idx = i;\n"
              + "    spanTristate.onclick = function(e) { toggleCardState(parseInt(this.dataset.idx), e); };\n"
              + "    header.appendChild(spanTristate);\n"
              + "    \n"
              + "    if (img.codigo) {\n"
              + "      var spanCode = document.createElement('span');\n"
              + "      spanCode.className = 'card-codigo';\n"
              + "      spanCode.textContent = img.codigo;\n"
              + "      header.appendChild(spanCode);\n"
              + "    }\n"
              + "    \n"
              + "    var spanName = document.createElement('span');\n"
              + "    spanName.className = 'card-name';\n"
              + "    spanName.textContent = img.nombre;\n"
              + "    spanName.dataset.idx = i;\n"
              + "    spanName.onclick = function() { openModal(parseInt(this.dataset.idx)); };\n"
              + "    header.appendChild(spanName);\n"
              + "    \n"
              + "    if (img.precio) {\n"
              + "      var spanPrice = document.createElement('span');\n"
              + "      spanPrice.className = 'card-price';\n"
              + "      spanPrice.textContent = img.precio + ' \\u20ac';\n"
              + "      header.appendChild(spanPrice);\n"
              + "    }\n"
              + "    \n"
              + "    var prices = [];\n"
              + "    if (img.checkboxes) { for (var j = 0; j < img.checkboxes.length; j++) { if (img.checkboxes[j].precio) prices.push(img.checkboxes[j].precio); } }\n"
              + "    var msgWeight = getCardMessageWeight(img);\n"
              + "    var iconClass = 'comment-icon';\n"
              + "    var iconTitle = 'Sin mensajes';\n"
              + "    if (msgWeight === 3) { iconClass += ' has-unread'; iconTitle = '\\u00a1Tienes mensajes del taller!'; }\n"
              + "    else if (msgWeight === 2) { iconClass += ' has-read-pending'; iconTitle = 'Mensaje le\\u00eddo'; }\n"
              + "    else if (msgWeight === 1) { iconClass += ' has-read'; iconTitle = 'Mensaje contestado'; }\n"
              + "    \n"
              + "    var iconBtn = document.createElement('span');\n"
              + "    iconBtn.className = iconClass;\n"
              + "    iconBtn.title = iconTitle;\n"
              + "    iconBtn.dataset.idx = i;\n"
              + "    iconBtn.onclick = function() { openModal(parseInt(this.dataset.idx), true); };\n"
              + "    header.appendChild(iconBtn);\n"
              + "    card.appendChild(header);\n"
              + "    \n"
              + "    var genStatus = getMessageStatus(img.comentario);\n"
              + "    if (genStatus !== 'NONE' || prices.length > 0) {\n"
              + "      var commentDiv = document.createElement('div');\n"
              + "      commentDiv.className = 'card-comment';\n"
              + "      if (prices.length === 1 && !img.precio) {\n"
              + "        var prDiv = document.createElement('div'); prDiv.className = 'card-prices';\n"
              + "        var tSpan = document.createElement('span'); tSpan.className = 'price-tag'; tSpan.textContent = prices[0] + ' \\u20ac';\n"
              + "        prDiv.appendChild(tSpan); commentDiv.appendChild(prDiv);\n"
              + "      } else if (prices.length > 0) {\n"
              + "        var prDiv2 = document.createElement('div'); prDiv2.className = 'card-prices'; prDiv2.style.cssText = 'color:var(--text-muted);font-size:.75rem;'; prDiv2.textContent = 'Precios \\u27a1 clic';\n"
              + "        commentDiv.appendChild(prDiv2);\n"
              + "      }\n"
              + "      if (genStatus !== 'NONE') {\n"
              + "         var lastM = img.comentario.hilo[img.comentario.hilo.length-1];\n"
              + "         var deStr = lastM.de === 'nosotros' ? '<span style=\"color:#ffd700\">TALLER:</span> ' : '<span style=\"color:#3498db\">TÚ:</span> ';\n"
              + "         var txtDiv = document.createElement('div'); txtDiv.style.cssText = 'font-weight:600; font-size:.8rem; margin-top:2px;';\n"
              + "         txtDiv.innerHTML = deStr + escHtml(lastM.texto);\n"
              + "         commentDiv.appendChild(txtDiv);\n"
              + "      }\n"
              + "      card.appendChild(commentDiv);\n"
              + "    }\n"
              + "    \n"
              + "    var imgEl = document.createElement('img');\n"
              + "    imgEl.src = img.miniatura;\n"
              + "    imgEl.alt = img.nombre;\n"
              + "    imgEl.loading = 'lazy';\n"
              + "    imgEl.dataset.idx = i;\n"
              + "    imgEl.onclick = function() { openModal(parseInt(this.dataset.idx)); };\n"
              + "    imgEl.onerror = function() { setErrorImage(this); };\n"
              + "    card.appendChild(imgEl);\n"
              + "    \n"
              + "    gallery.appendChild(card);\n"
              + "  }\n"
              + "  updateCounter();\n"
              + "}\n"
              + "function toggleCardState(i, e) {\n"
              + "  if (e) e.stopPropagation();\n"
              + "  var img = data.imagenes[i];\n"
              + "  var estados = ['SELECTED', 'DISCARDED', 'UNDEFINED'];\n"
              + "  img.estado = estados[(estados.indexOf(img.estado || 'UNDEFINED') + 1) % estados.length];\n"
              + "  if (img.checkboxes) { for (var k = 0; k < img.checkboxes.length; k++) { img.checkboxes[k].estado = img.estado; } }\n"
              + "  renderGallery();\n"
              + "  saveState();\n"
              + "}\n"
              + "function escHtml(s) { return (s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\"/g,'&quot;'); }\n"
              + "function setErrorImage(el) { el.onerror=null; el.src='data:image/svg+xml,'+encodeURIComponent('<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"200\" height=\"200\"><rect width=\"200\" height=\"200\" fill=\"%23222\"/><text x=\"50%\" y=\"50%\" fill=\"%23666\" text-anchor=\"middle\" dy=\".3em\">Sin imagen</text></svg>'); }\n"
              + "function makeMsgClickHandler(texto) { return function() { document.getElementById('modalComentario').value = texto; document.getElementById('modalComentario').focus(); }; }\n"
               + "function updateCounter() {\n"
               + "  var sel = data.imagenes.filter(function(i) { return i.estado === 'SELECTED'; }).length;\n"
               + "  var dis = data.imagenes.filter(function(i) { return i.estado === 'DISCARDED'; }).length;\n"
               + "  var und = data.imagenes.filter(function(i) { return i.estado === 'UNDEFINED'; }).length;\n"
               + "  document.getElementById('counter').innerHTML = 'Total: ' + data.imagenes.length + ' | <span class=\"count-icon count-sel\">&#x2713;</span> ' + sel + ' <span class=\"count-icon count-dis\">&#x2717;</span> ' + dis + ' <span class=\"count-icon count-und\">&#x25cb;</span> ' + und;\n"
               + "}\n"

              + "// === FUNCIÓN DE CONTROL DE CAJÓN DESPLEGABLE (BOTTOM SHEET) ===\n"
              + "function toggleChat() {\n"
              + "  if (window.innerWidth > 800) return; // Solo funciona en formato movil\n"
              + "  var sidebar = document.querySelector('.modal-chat-sidebar');\n"
              + "  if (!sidebar) return;\n"
              + "  sidebar.classList.toggle('collapsed');\n"
              + "  var toggleBtn = document.getElementById('chatToggle');\n"
              + "  if (toggleBtn) {\n"
              + "    toggleBtn.textContent = sidebar.classList.contains('collapsed') ? '^' : 'v';\n"
              + "  }\n"
              + "}\n"
        
              + "// === MODAL: ABRIR, OVERLAYS, ZOOM ===\n"
               + "function openModal(index, expandChat) {\n"
               + "  currentIndex = index;\n"
               + "  var img = data.imagenes[index];\n"
               + "  document.getElementById('modalCodigo').textContent = img.codigo || '';\n"
               + "  document.getElementById('modalNombre').textContent = img.nombre;\n"
               + "  document.getElementById('modalImg').src = img.miniatura;\n"
               + "  setTristate(document.getElementById('modalCheckPrincipal'), img.estado || 'UNDEFINED');\n"
               + "  currentContext = { type: 'IMAGE', index: index, cbIndex: -1 }; renderChat();\n"
               + "  \n"
               + "  // Configuración del cajón al abrir en móviles\n"
               + "  var sidebar = document.querySelector('.modal-chat-sidebar');\n"
               + "  if (sidebar) {\n"
               + "    if (expandChat && window.innerWidth <= 800) {\n"
               + "      sidebar.classList.remove('collapsed');\n"
               + "      var toggleBtn = document.getElementById('chatToggle');\n"
               + "      if (toggleBtn) toggleBtn.textContent = 'v';\n"
               + "    } else if (window.innerWidth <= 800) {\n"
               + "      sidebar.classList.add('collapsed');\n"
               + "      var toggleBtn = document.getElementById('chatToggle');\n"
               + "      if (toggleBtn) toggleBtn.textContent = '^';\n"
               + "    }\n"
               + "  }\n"
               + "  resetZoom();\n"
              + "  var overlayContainer = document.getElementById('checkboxOverlays');\n"
              + "  overlayContainer.innerHTML = '';\n"
              + "  document.getElementById('modalImg').onload = function() { requestAnimationFrame(function() { sizeOverlayContainer(); if (img.checkboxes && img.checkboxes.length > 0) positionOverlays(overlayContainer, img); }); };\n"
               + "  if (document.getElementById('modalImg').complete && document.getElementById('modalImg').naturalWidth > 0) { requestAnimationFrame(function() { sizeOverlayContainer(); if (img.checkboxes && img.checkboxes.length > 0) positionOverlays(overlayContainer, img); }); }\n"
               + "  document.body.classList.add('modal-open');\n"
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
               + "    var activeClass = (currentContext.type === 'CHECKBOX' && currentContext.cbIndex === j) ? ' active' : '';\n"
               + "    overlay.className = 'cb-overlay' + activeClass;\n"
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
              + "        saveState();\n"
              + "      }\n"
              + "    });\n"
              + "    var bubble = overlay.querySelector('.cb-bubble');\n"
              + "    bubble.addEventListener('click', function(e) {\n"
              + "      e.stopPropagation();\n"
              + "      var idx = Array.prototype.indexOf.call(container.children, this.closest('.cb-overlay'));\n"
              + "      if (idx >= 0) {\n"
              + "        var cbData = data.imagenes[currentIndex].checkboxes[idx];\n"
              + "        currentContext = { type: 'CHECKBOX', index: currentIndex, cbIndex: idx }; renderChat(); positionOverlays(document.getElementById('checkboxOverlays'), data.imagenes[currentIndex]);\n"
              + "        \n"
              + "        // Al pulsar un pin flotante, expandimos automáticamente la persiana en móvil\n"
              + "        var sidebar = document.querySelector('.modal-chat-sidebar');\n"
              + "        if (sidebar) {\n"
              + "          sidebar.classList.remove('collapsed');\n"
              + "          var toggleBtn = document.getElementById('chatToggle');\n"
              + "          if (toggleBtn) toggleBtn.textContent = 'v';\n"
              + "        }\n"
              + "      }\n"
              + "    });\n"
              + "  }\n"
              + "}\n"
              + "function cycleState(c) { return c === 'SELECTED' ? 'DISCARDED' : (c === 'DISCARDED' ? 'UNDEFINED' : 'SELECTED'); }\n"
              + "function setTristate(el, state) { el.className = 'tristate-cb state-' + state.toLowerCase(); el.textContent = state === 'SELECTED' ? '\\u2713' : (state === 'DISCARDED' ? '\\u2717' : '\\u25cb'); }\n"
              + "document.getElementById('modalClose').addEventListener('click', function() { guardarModal(); document.getElementById('modal').classList.add('hidden'); document.body.classList.remove('modal-open'); });\n"
               + "document.getElementById('modalCheckPrincipal').addEventListener('click', function() { if (currentIndex >= 0) { var img = data.imagenes[currentIndex]; img.estado = cycleState(img.estado || 'UNDEFINED'); if (img.checkboxes) { for (var k = 0; k < img.checkboxes.length; k++) { img.checkboxes[k].estado = img.estado; } } setTristate(this, img.estado); positionOverlays(document.getElementById('checkboxOverlays'), img); renderGallery(); saveState(); } });\n"
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
              + "  // Actualizar título dinámico\n"
              + "  var titleEl = document.getElementById('modalChatTitleText');\n"
              + "  if (titleEl) {\n"
              + "    if (currentContext.type === 'IMAGE') {\n"
              + "      titleEl.textContent = 'Chat general de la imagen';\n"
              + "    } else {\n"
              + "      titleEl.textContent = 'Chat Opción: ' + (target.codigo || 'Seleccionada');\n"
              + "    }\n"
              + "  }\n"
              + "  \n"
              + "  // ---> CAMBIO DE ESTADO AL LEER (EL CLIENTE ABRE EL MENSAJE) <---\n"
              + "  if (target.comentario && target.comentario.estadoHtml === 3) {\n"
              + "    target.comentario.estadoHtml = 1;\n" // 1: mensaje leído no contestado -> verde
              + "    target.comentario.estadoPr = 1;\n"   // 1: leido en Java también
              + "    renderGallery();\n" // Refresca galería principal
              + "    saveState();\n"
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
               + "  setTimeout(function() { chatDiv.scrollTop = chatDiv.scrollHeight; }, 100);\n"
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
              + "  saveState();\n"
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
               + "  positionOverlays(document.getElementById('checkboxOverlays'), img);\n"
               + "  saveState();\n"
               + "};\n"
              + "document.getElementById('modalComentario').addEventListener('keydown', function(e) {\n"
              + "  if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); document.getElementById('btnEnviarChat').click(); }\n"
              + "});\n"
         
               + "/* === ZOOM / PAN / TACTIL === */\n"
               + "function guardarModal() { if (currentIndex < 0) return; renderGallery(); }\n"
               + "function resetZoom() { zoomScale = 1; panX = 0; panY = 0; applyTransform(); }\n"
               + "function applyTransform() { var ts = 'translate(' + panX + 'px, ' + panY + 'px) scale(' + zoomScale + ')'; document.getElementById('modalImg').style.transform = ts; document.getElementById('checkboxOverlays').style.transform = ts; }\n"
               + "var wrap = document.getElementById('modalImageWrap');\n"
               + "document.getElementById('modalImg').addEventListener('dragstart', function(e) { e.preventDefault(); });\n"
               + "wrap.addEventListener('mousedown', function(e) { if (e.button !== 0) return; e.preventDefault(); isDragging = true; startX = e.clientX; startY = e.clientY; wrap.style.cursor = 'grabbing'; });\n"
               + "document.addEventListener('mousemove', function(e) { if (!isDragging) return; panX += (e.clientX - startX); panY += (e.clientY - startY); startX = e.clientX; startY = e.clientY; applyTransform(); });\n"
               + "document.addEventListener('mouseup', function() { isDragging = false; wrap.style.cursor = 'grab'; });\n"
               + "wrap.addEventListener('wheel', function(e) {\n"
               + "  e.preventDefault();\n"
               + "  var rect = wrap.getBoundingClientRect();\n"
               + "  var mx = e.clientX - rect.left;\n"
               + "  var my = e.clientY - rect.top;\n"
               + "  var old = zoomScale;\n"
               + "  var nz = Math.max(0.3, Math.min(6, old + (e.deltaY > 0 ? -0.15 : 0.15)));\n"
               + "  if (nz !== old) {\n"
               + "    var wx = (mx - panX) / old;\n"
               + "    var wy = (my - panY) / old;\n"
               + "    zoomScale = nz;\n"
               + "    panX = mx - wx * nz;\n"
               + "    panY = my - wy * nz;\n"
               + "    applyTransform();\n"
               + "  }\n"
               + "}, { passive: false });\n"
               + "wrap.addEventListener('touchstart', function(e) {\n"
               + "  if (e.touches.length === 1) {\n"
               + "    isDragging = true;\n"
               + "    startX = e.touches[0].clientX;\n"
               + "    startY = e.touches[0].clientY;\n"
               + "    var now = Date.now();\n"
               + "    if (now - lastTapTime < 300 && lastTapTime > 0) {\n"
               + "      e.preventDefault();\n"
               + "      if (zoomScale > 1.5) { resetZoom(); }\n"
               + "      else {\n"
               + "        var rect = wrap.getBoundingClientRect();\n"
               + "        var tx = e.touches[0].clientX - rect.left;\n"
               + "        var ty = e.touches[0].clientY - rect.top;\n"
               + "        var old = zoomScale;\n"
               + "        var nz = 2.5;\n"
               + "        var wx = (tx - panX) / old;\n"
               + "        var wy = (ty - panY) / old;\n"
               + "        zoomScale = nz;\n"
               + "        panX = tx - wx * nz;\n"
               + "        panY = ty - wy * nz;\n"
               + "        applyTransform();\n"
               + "      }\n"
               + "      lastTapTime = 0;\n"
               + "    } else {\n"
               + "      lastTapTime = now;\n"
               + "    }\n"
               + "  } else if (e.touches.length === 2) {\n"
               + "    e.preventDefault();\n"
               + "    isDragging = false;\n"
               + "    lastTapTime = 0;\n"
               + "    var dx = e.touches[0].clientX - e.touches[1].clientX;\n"
               + "    var dy = e.touches[0].clientY - e.touches[1].clientY;\n"
               + "    lastTouchDist = Math.sqrt(dx*dx + dy*dy);\n"
               + "  }\n"
               + "}, { passive: false });\n"
               + "wrap.addEventListener('touchmove', function(e) {\n"
               + "  e.preventDefault();\n"
               + "  if (e.touches.length === 1 && isDragging) {\n"
               + "    panX += (e.touches[0].clientX - startX);\n"
               + "    panY += (e.touches[0].clientY - startY);\n"
               + "    startX = e.touches[0].clientX;\n"
               + "    startY = e.touches[0].clientY;\n"
               + "    applyTransform();\n"
               + "  } else if (e.touches.length === 2) {\n"
               + "    var dx = e.touches[0].clientX - e.touches[1].clientX;\n"
               + "    var dy = e.touches[0].clientY - e.touches[1].clientY;\n"
               + "    var dist = Math.sqrt(dx*dx + dy*dy);\n"
               + "    if (lastTouchDist > 0) {\n"
               + "      var rect = wrap.getBoundingClientRect();\n"
               + "      var mx = ((e.touches[0].clientX + e.touches[1].clientX) / 2) - rect.left;\n"
               + "      var my = ((e.touches[0].clientY + e.touches[1].clientY) / 2) - rect.top;\n"
               + "      var old = zoomScale;\n"
               + "      var nz = Math.max(0.3, Math.min(6, old * (dist / lastTouchDist)));\n"
               + "      if (nz !== old) {\n"
               + "        var wx = (mx - panX) / old;\n"
               + "        var wy = (my - panY) / old;\n"
               + "        zoomScale = nz;\n"
               + "        panX = mx - wx * nz;\n"
               + "        panY = my - wy * nz;\n"
               + "        applyTransform();\n"
               + "      }\n"
               + "    }\n"
               + "    lastTouchDist = dist;\n"
               + "  }\n"
               + "}, { passive: false });\n"
               + "wrap.addEventListener('touchend', function(e) {\n"
               + "  if (e.touches.length === 0) {\n"
               + "    isDragging = false;\n"
               + "    lastTouchDist = 0;\n"
               + "  } else if (e.touches.length === 1) {\n"
               + "    startX = e.touches[0].clientX;\n"
               + "    startY = e.touches[0].clientY;\n"
               + "    isDragging = true;\n"
               + "    lastTouchDist = 0;\n"
               + "  }\n"
               + "});\n"
         
               + "// === BOTONES DE RESPUESTA: COMPARTIR Y DESCARGAR ===\n"
               + "function recuperarEnfoque() {\n"
               + "  if (document.activeElement) document.activeElement.blur();\n"
               + "  window.scrollBy(0, 1);\n"
               + "  window.scrollBy(0, -1);\n"
               + "  document.querySelectorAll('.fallback-overlay').forEach(function(el) { el.remove(); });\n"
               + "}\n"
               + "function compartirJson() {\n"
               + "  var respuesta = buildResponse(); var json = JSON.stringify(respuesta, null, 2);\n"
               + "  if (!navigator.share) { copiarTexto(json); recuperarEnfoque(); return; }\n"
               + "  try {\n"
               + "    var blob = new Blob([json], {type: 'text/plain;charset=utf-8'});\n"
               + "    var nombreTxt = '" + respuestaFilename + "'.replace('.json', '.txt');\n"
               + "    var file = new File([blob], nombreTxt, {type: 'text/plain'});\n"
               + "    if (navigator.canShare && navigator.canShare({files: [file]})) {\n"
               + "      navigator.share({files: [file], title: 'Respuesta de " + escapeHtml(projectName) + "', text: 'Selección del catálogo.'}).then(recuperarEnfoque, recuperarEnfoque);\n"
               + "    } else {\n"
               + "      navigator.share({title: 'Respuesta de " + escapeHtml(projectName) + "', text: json}).then(recuperarEnfoque, recuperarEnfoque);\n"
               + "    }\n"
               + "  } catch(e) {\n"
               + "    copiarTexto(json);\n"
               + "  }\n"
               + "}\n"
               + "function copiarTexto(text) {\n"
               + "  if (navigator.clipboard && navigator.clipboard.writeText) {\n"
               + "    navigator.clipboard.writeText(text).then(function() { showToast('\\u2713 JSON copiado al portapapeles.'); }).catch(function() { fallbackCopy(text); });\n"
               + "  } else {\n"
               + "    fallbackCopy(text);\n"
               + "  }\n"
               + "}\n"
               + "function fallbackCopy(text) {\n"
               + "  var ov = document.createElement('div');\n"
               + "  ov.className = 'fallback-overlay';\n"
               + "  ov.style.cssText = 'position:fixed;inset:0;z-index:999;background:rgba(15,15,26,.95);display:flex;flex-direction:column;padding:20px 16px;gap:10px;align-items:stretch';\n"
               + "  var lbl = document.createElement('div');\n"
               + "  lbl.textContent = 'Copia el texto manualmente (Ctrl+C / mantener presionado)';\n"
               + "  lbl.style.cssText = 'color:#999;font-size:.85rem;text-align:center;padding:4px 0';\n"
               + "  var ta = document.createElement('textarea');\n"
               + "  ta.value = text;\n"
               + "  ta.style.cssText = 'flex:1;width:100%;min-height:200px;background:#1a1a2e;color:#f0f0f0;border:1px solid #3a3a6a;border-radius:6px;padding:10px;font-size:.8rem;resize:none;outline:none';\n"
               + "  ta.readOnly = true;\n"
               + "  ov.appendChild(lbl);\n"
               + "  ov.appendChild(ta);\n"
               + "  var row = document.createElement('div');\n"
               + "  row.style.cssText = 'display:flex;gap:10px;justify-content:center;padding:4px 0';\n"
               + "  var btn = document.createElement('button');\n"
               + "  btn.textContent = 'Cerrar';\n"
               + "  btn.style.cssText = 'background:#3a3a6a;color:#fff;border:1px solid #5a5a9a;padding:.5rem 1.2rem;border-radius:6px;font-weight:600;cursor:pointer;font-size:.85rem';\n"
               + "  btn.onclick = function() { document.body.removeChild(ov); recuperarEnfoque(); };\n"
               + "  row.appendChild(btn);\n"
               + "  ov.appendChild(row);\n"
               + "  document.body.appendChild(ov);\n"
               + "  ta.focus();\n"
               + "  ta.select();\n"
               + "  try { document.execCommand('copy'); } catch(e) {}\n"
               + "}\n"
               + "document.getElementById('btnCompartir').addEventListener('click', compartirJson);\n"
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
        String escaped = valor
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\f", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("</", "<\\/");
        // Escapar separadores de línea y párrafo que Safari/WebKit no toleran
        escaped = escaped.replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
        return "\"" + escaped + "\"";
        
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


