package servicios.editor;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import modelo.editor.CanvasModel;
import modelo.editor.ImageLayer;
import modelo.editor.Layer;
import modelo.editor.LayerModel;
import modelo.editor.TextLayer;
import modelo.editor.TextLayer.TextRun;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;

/**
 * Gestor del documento del Modo Editor (espejo de {@code ProjectManager}).
 * <p>
 * Encapsula el estado de "documento con nombre / temporal", el flag de cambios
 * sin guardar y la persistencia en formato {@code .edoc} (JSON con Gson). El
 * documento su cuenta con las transformaciones de las capas y exporta a PNG de
 * respaldo en una carpeta {@code <archivo>_capas/} las capas r\u00E1ster que no tienen
 * origen en un archivo externo.
 */
public class EditorDocumentManager {

    private static final Logger logger = LoggerFactory.getLogger(EditorDocumentManager.class);

    /** Nombre por defecto mientras el documento no tiene archivo. */
    public static final String NOMBRE_SIN_TITULO = "Sin t\u00EDtulo";

    private final ConfigurationManager config;
    private final Gson gson;

    // --- Estado del documento ---
    private CanvasModel canvasModel;
    private LayerModel layerModel;
    private boolean dirty;
    private Path archivoActivo;
    private String nombreDocumento;

    // --- Rutas ---
    private Path carpetaBaseDocs;
    private Path archivoTemporal;

    /** Notificador externo de suciedad (p. ej. para refrescar el t\u00EDtulo). */
    private Runnable dirtyNotifier;

    public EditorDocumentManager(ConfigurationManager config) {
        this.config = config;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        this.dirty = false;
        this.archivoActivo = null;
        this.nombreDocumento = NOMBRE_SIN_TITULO;
        inicializarRutas();
    } // --- Fin del constructor EditorDocumentManager ---


    private void inicializarRutas() {
        String carpeta = config.getString(ConfigKeys.EDITOR_CARPETA_BASE, ".editor_docs");
        this.carpetaBaseDocs = Paths.get(carpeta);
        if (!this.carpetaBaseDocs.isAbsolute()) {
            String userHome = System.getProperty("user.home");
            this.carpetaBaseDocs = Paths.get(userHome, ".miVisorImagenesApp", carpeta).toAbsolutePath();
        }
        try {
            if (!Files.exists(this.carpetaBaseDocs)) {
                Files.createDirectories(this.carpetaBaseDocs);
            }
        } catch (IOException e) {
            logger.warn("No se pudo crear el directorio base de documentos del editor: {}", carpetaBaseDocs, e);
            this.carpetaBaseDocs = Paths.get("").toAbsolutePath();
        }

        String nombreTemporal = config.getString(ConfigKeys.EDITOR_ARCHIVO_TEMPORAL, "editor_temporal.edoc");
        this.archivoTemporal = this.carpetaBaseDocs.resolve(nombreTemporal);
    } // --- Fin del metodo inicializarRutas ---


    /**
     * Asigna los modelos del documento activo y suscribe el listener de cambios
     * para marcar el documento como sucio ante cualquier modificaci\u00F3n.
     */
    public void setDocument(CanvasModel canvasModel, LayerModel layerModel) {
        this.canvasModel = canvasModel;
        this.layerModel = layerModel;
        if (layerModel != null) {
            layerModel.addChangeListener(() -> notificarModificacion());
        }
    } // --- Fin del metodo setDocument ---


    public void setDirtyNotifier(Runnable dirtyNotifier) {
        this.dirtyNotifier = dirtyNotifier;
    } // --- Fin del metodo setDirtyNotifier ---


    public boolean hayCambiosSinGuardar() {
        return dirty;
    } // --- Fin del metodo hayCambiosSinGuardar ---


    public void markDocumentAsSaved() {
        this.dirty = false;
        notifyDirty();
    } // --- Fin del metodo markDocumentAsSaved ---


    /**
     * Marca el documento como modificado (sucio).
     */
    public void notificarModificacion() {
        if (!this.dirty) {
            this.dirty = true;
            notifyDirty();
        }
    } // --- Fin del metodo notificarModificacion ---


    private void notifyDirty() {
        if (dirtyNotifier != null) {
            dirtyNotifier.run();
        }
    } // --- Fin del metodo notifyDirty ---


    public Path getArchivoActivo() {
        return archivoActivo;
    } // --- Fin del metodo getArchivoActivo ---


    public String getNombreDocumento() {
        return nombreDocumento;
    } // --- Fin del metodo getNombreDocumento ---


    public CanvasModel getCanvasModel() {
        return canvasModel;
    } // --- Fin del metodo getCanvasModel ---


    public LayerModel getLayerModel() {
        return layerModel;
    } // --- Fin del metodo getLayerModel ---


    /**
     * Crea un nuevo documento vac\u00EDo (lienzo por defecto, sin capas).
     */
    public void nuevoDocumento() {
        if (canvasModel != null) {
            canvasModel.setSize(1920, 1080);
            canvasModel.setBackgroundColor(Color.WHITE);
            canvasModel.setTransparent(false);
        }
        if (layerModel != null) {
            layerModel.clear();
        }
        this.archivoActivo = null;
        this.nombreDocumento = NOMBRE_SIN_TITULO;
        this.dirty = false;
        notifyDirty();
        logger.info("[EditorDocumentManager] Nuevo documento creado.");
    } // --- Fin del metodo nuevoDocumento ---


    /**
     * Guarda el documento en el archivo activo o, si no hay, en el temporal.
     */
    public void guardarAArchivo() {
        Path ruta = (archivoActivo != null) ? archivoActivo : archivoTemporal;
        guardarEn(ruta);
        if (archivoActivo != null) {
            this.dirty = false;
            notifyDirty();
        }
    } // --- Fin del metodo guardarAArchivo ---


    /**
     * Guarda el documento en la ruta indicada y la establece como archivo activo.
     */
    public void guardarComo(Path rutaArchivo) {
        this.archivoActivo = rutaArchivo;
        this.nombreDocumento = nombreDesdeRuta(rutaArchivo);
        guardarEn(rutaArchivo);
        this.dirty = false;
        notifyDirty();
    } // --- Fin del metodo guardarComo ---


    /**
     * Abre un documento .edoc y lo aplica a los modelos activos.
     */
    public boolean abrirDocumento(Path rutaArchivo) {
        if (rutaArchivo == null || !Files.isReadable(rutaArchivo)) {
            logger.error("[EditorDocumentManager] Ruta no legible: {}", rutaArchivo);
            return false;
        }
        try (Reader reader = new FileReader(rutaArchivo.toFile())) {
            EditorDoc doc = gson.fromJson(reader, EditorDoc.class);
            aplicarDocumento(doc, rutaArchivo.toAbsolutePath().getParent());
            this.archivoActivo = rutaArchivo;
            this.nombreDocumento = nombreDesdeRuta(rutaArchivo);
            this.dirty = false;
            notifyDirty();
            return true;
        } catch (IOException e) {
            logger.error("[EditorDocumentManager] Error al abrir documento {}: {}", rutaArchivo, e.getMessage(), e);
            return false;
        }
    } // --- Fin del metodo abrirDocumento ---


    private String nombreDesdeRuta(Path ruta) {
        if (ruta == null || ruta.getFileName() == null) return NOMBRE_SIN_TITULO;
        String name = ruta.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    } // --- Fin del metodo nombreDesdeRuta ---


    // ==================== SERIALIZACI\u00D3N ====================


    private EditorDoc generarDocumento() {
        EditorDoc doc = new EditorDoc();
        doc.version = EditorDoc.VERSION_ACTUAL;

        EditorDoc.CanvasDTO canvas = new EditorDoc.CanvasDTO();
        canvas.width = canvasModel.getWidth();
        canvas.height = canvasModel.getHeight();
        Color bg = canvasModel.getBackgroundColor();
        canvas.backgroundColorArgb = (bg != null) ? bg.getRGB() : 0;
        canvas.transparent = canvasModel.isTransparent();
        doc.canvas = canvas;

        List<EditorDoc.LayerDTO> layers = new ArrayList<>();
        if (layerModel != null) {
            for (Layer l : layerModel.getLayers()) {
                layers.add(capaADto(l));
            }
        }
        doc.layers = layers;
        return doc;
    } // --- Fin del metodo generarDocumento ---


    private EditorDoc.LayerDTO capaADto(Layer l) {
        EditorDoc.LayerDTO dto = new EditorDoc.LayerDTO();
        Rectangle b = l.getBounds();
        dto.id = l.getId();
        dto.name = l.getName();
        dto.boundsX = b.x;
        dto.boundsY = b.y;
        dto.boundsW = b.width;
        dto.boundsH = b.height;
        dto.rotation = l.getRotation();
        dto.visible = l.isVisible();
        dto.locked = l.isLocked();
        dto.opacity = l.getOpacity();

        if (l instanceof ImageLayer il) {
            dto.type = il.getType().name();
            dto.shapeType = il.getShapeType();
            dto.shapeFillArgb = colorToInt(il.getShapeFill());
            dto.shapeStrokeArgb = colorToInt(il.getShapeStroke());
            dto.shapeStrokeWidth = il.getShapeStrokeWidth();
            dto.shapeRenderW = il.getShapeRenderW();
            dto.shapeRenderH = il.getShapeRenderH();

            // IMAGE con origen externo: se referencia tal cual.
            if (il.getSrcPath() != null && !esRutaSidecar(il.getSrcPath())) {
                dto.srcPath = il.getSrcPath();
            } else {
                // Capa r\u00E1ster sin origen (pintada/degradado/fusi\u00F3n) o shape:
                // se exporta a la carpeta "<archivo>_capas/" del documento actual.
                String rel = carpetaCapasRelativa();
                if (il.getSrcPath() == null || !il.getSrcPath().startsWith(rel + "/")) {
                    String ruta = exportarPng(l, rel);
                    dto.srcPath = ruta;
                    il.setSrcPath(ruta);
                } else {
                    dto.srcPath = il.getSrcPath();
                }
            }
        } else if (l instanceof TextLayer tl) {
            dto.type = "TEXT";
            dto.text = tl.getText();
            dto.fontFamily = tl.getFont() != null ? tl.getFont().getFamily() : null;
            dto.fontStyle = tl.getFont() != null ? tl.getFont().getStyle() : null;
            dto.fontSize = tl.getFont() != null ? tl.getFont().getSize() : null;
            dto.textColorArgb = colorToInt(tl.getColor());
            dto.alignment = tl.getAlignment();
            dto.vertical = tl.isVertical();
            dto.lineSpacing = tl.getLineSpacing();
            dto.autoSize = tl.isAutoSize();
            dto.underline = tl.isUnderline();
            dto.strikethrough = tl.isStrikethrough();
            dto.flowColumns = tl.isFlowColumns();
            if (tl.getRuns() != null && !tl.getRuns().isEmpty()) {
                dto.runs = new ArrayList<>();
                for (TextRun r : tl.getRuns()) {
                    EditorDoc.TextRunDTO rdto = new EditorDoc.TextRunDTO();
                    rdto.fontFamily = r.font() != null ? r.font().getFamily() : null;
                    rdto.fontStyle = r.font() != null ? r.font().getStyle() : null;
                    rdto.fontSize = r.font() != null ? r.font().getSize() : null;
                    rdto.colorArgb = colorToInt(r.color());
                    rdto.underline = r.underline();
                    rdto.strikethrough = r.strikethrough();
                    rdto.text = r.text();
                    dto.runs.add(rdto);
                }
            }
        }
        return dto;
    } // --- Fin del metodo capaADto ---


    private Integer colorToInt(Color c) {
        return c != null ? Integer.valueOf(c.getRGB()) : null;
    } // --- Fin del metodo colorToInt ---


    /**
     * Exporta una capa r\u00E1ster a un PNG sidecar en la carpeta
     * {@code <archivo>_capas/} junto al archivo activo (o al temporal).
     * Devuelve la ruta relativa al documento.
     */
    private String exportarPng(Layer l, String carpetaRelativa) {
        Path baseDir = (archivoActivo != null ? archivoActivo : archivoTemporal).toAbsolutePath().getParent();
        Path capasDir = baseDir.resolve(carpetaRelativa);
        try {
            Files.createDirectories(capasDir);
            Rectangle b = l.getBounds();
            BufferedImage img = new BufferedImage(Math.max(1, b.width), Math.max(1, b.height), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            try {
                g.translate(-b.x, -b.y);
                l.paint(g);
            } finally {
                g.dispose();
            }
            Path archivo = capasDir.resolve(l.getId() + ".png");
            ImageIO.write(img, "png", archivo.toFile());
            logger.debug("[EditorDocumentManager] PNG exportado: {}", archivo);
        } catch (IOException e) {
            logger.error("[EditorDocumentManager] Error exportando PNG de la capa {}", l.getId(), e);
        }
        return carpetaRelativa + "/" + l.getId() + ".png";
    } // --- Fin del metodo exportarPng ---


    /** Devuelve el nombre base del archivo de destino (activo o temporal). */
    private String baseNombreArchivo() {
        Path target = (archivoActivo != null) ? archivoActivo : archivoTemporal;
        String fn = target.getFileName().toString();
        int dot = fn.lastIndexOf('.');
        return dot > 0 ? fn.substring(0, dot) : fn;
    } // --- Fin del metodo baseNombreArchivo ---


    /** Nombre de la carpeta sidecar de capas: "<archivo>_capas". */
    private String carpetaCapasRelativa() {
        return baseNombreArchivo() + "_capas";
    } // --- Fin del metodo carpetaCapasRelativa ---


    /** True si la ruta apunta a una carpeta sidecar de capas (contiene "_capas/"). */
    private boolean esRutaSidecar(String srcPath) {
        return srcPath != null && srcPath.contains("_capas/");
    } // --- Fin del metodo esRutaSidecar ---


    private void guardarEn(Path ruta) {
        if (canvasModel == null || layerModel == null) {
            logger.error("[EditorDocumentManager] Modelos nulos; no se puede guardar.");
            return;
        }
        try {
            Files.createDirectories(ruta.toAbsolutePath().getParent());
            try (FileWriter writer = new FileWriter(ruta.toFile())) {
                gson.toJson(generarDocumento(), writer);
            }
            logger.info("[EditorDocumentManager] Documento guardado en {}", ruta.toAbsolutePath());
        } catch (IOException e) {
            logger.error("[EditorDocumentManager] Error al guardar documento en {}", ruta, e);
        }
    } // --- Fin del metodo guardarEn ---


    private void aplicarDocumento(EditorDoc doc, Path baseDocDir) {
        if (canvasModel != null && doc.canvas != null) {
            canvasModel.setSize(doc.canvas.width, doc.canvas.height);
            canvasModel.setBackgroundColor(new Color(doc.canvas.backgroundColorArgb, true));
            canvasModel.setTransparent(doc.canvas.transparent);
        }
        if (layerModel == null) return;
        layerModel.clear();
        if (doc.layers == null) return;

        for (EditorDoc.LayerDTO dto : doc.layers) {
            Layer l = dtoACapa(dto, baseDocDir);
            if (l != null) {
                layerModel.addLayer(l);
            }
        }
    } // --- Fin del metodo aplicarDocumento ---


    private Layer dtoACapa(EditorDoc.LayerDTO dto, Path baseDocDir) {
        Rectangle b = new Rectangle(dto.boundsX, dto.boundsY, dto.boundsW, dto.boundsH);
        String tipo = dto.type != null ? dto.type : "IMAGE";
        try {
            if ("TEXT".equals(tipo)) {
                return textoCapa(dto, b);
            }
            BufferedImage img = cargarImagen(dto.srcPath, baseDocDir);
            ImageLayer il = new ImageLayer(dto.name != null ? dto.name : "Capa", img, b);
            il.setType("SHAPE".equals(tipo) ? ImageLayer.LayerType.SHAPE : ImageLayer.LayerType.IMAGE);
            if (il.getType() == ImageLayer.LayerType.SHAPE) {
                il.setShapeType(dto.shapeType);
                il.setShapeFill(colorFromInt(dto.shapeFillArgb));
                il.setShapeStroke(colorFromInt(dto.shapeStrokeArgb));
                il.setShapeStrokeWidth(dto.shapeStrokeWidth != null ? dto.shapeStrokeWidth : 0f);
                il.setShapeRenderW(dto.shapeRenderW != null ? dto.shapeRenderW : 0);
                il.setShapeRenderH(dto.shapeRenderH != null ? dto.shapeRenderH : 0);
            }
            il.setSrcPath(dto.srcPath);
            rellenarComun(il, dto, b);
            return il;
        } catch (Exception e) {
            logger.error("[EditorDocumentManager] Error reconstruyendo capa {}: {}", dto.name, e.getMessage(), e);
            return null;
        }
    } // --- Fin del metodo dtoACapa ---


    private Layer textoCapa(EditorDoc.LayerDTO dto, Rectangle b) {
        Font font = new Font(dto.fontFamily != null ? dto.fontFamily : "SansSerif",
                dto.fontStyle != null ? dto.fontStyle : Font.PLAIN,
                dto.fontSize != null ? dto.fontSize : 24);
        TextLayer tl = new TextLayer(dto.name != null ? dto.name : "Texto",
                dto.text != null ? dto.text : "", font,
                colorFromInt(dto.textColorArgb),
                b);
        tl.setAlignment(dto.alignment != null ? dto.alignment : TextLayer.ALIGN_LEFT);
        tl.setVertical(Boolean.TRUE.equals(dto.vertical));
        tl.setUnderline(Boolean.TRUE.equals(dto.underline));
        tl.setStrikethrough(Boolean.TRUE.equals(dto.strikethrough));
        tl.setFlowColumns(Boolean.TRUE.equals(dto.flowColumns));
        tl.setLineSpacing(dto.lineSpacing != null ? dto.lineSpacing : 1.2f);
        tl.setAutoSize(Boolean.TRUE.equals(dto.autoSize));
        if (dto.runs != null && !dto.runs.isEmpty()) {
            List<TextRun> runs = new ArrayList<>();
            for (EditorDoc.TextRunDTO rdto : dto.runs) {
                Font rf = new Font(rdto.fontFamily != null ? rdto.fontFamily : "SansSerif",
                        rdto.fontStyle != null ? rdto.fontStyle : Font.PLAIN,
                        rdto.fontSize != null ? rdto.fontSize : 24);
                runs.add(new TextRun(rf, colorFromInt(rdto.colorArgb),
                        Boolean.TRUE.equals(rdto.underline), Boolean.TRUE.equals(rdto.strikethrough),
                        rdto.text != null ? rdto.text : ""));
            }
            tl.setRuns(runs);
        }
        rellenarComun(tl, dto, b);
        return tl;
    } // --- Fin del metodo textoCapa ---


    private void rellenarComun(Layer l, EditorDoc.LayerDTO dto, Rectangle b) {
        l.setRotation(dto.rotation);
        l.setVisible(dto.visible);
        l.setLocked(dto.locked);
        l.setOpacity(dto.opacity);
        l.setBounds(b);
    } // --- Fin del metodo rellenarComun ---


    private BufferedImage cargarImagen(String srcPath, Path baseDocDir) {
        if (srcPath == null || srcPath.isEmpty() || baseDocDir == null) return null;
        try {
            Path p = baseDocDir.resolve(srcPath);
            if (!Files.isReadable(p)) {
                logger.warn("[EditorDocumentManager] No se pudo leer la imagen de la capa: {}", p);
                return null;
            }
            return ImageIO.read(p.toFile());
        } catch (IOException e) {
            logger.error("[EditorDocumentManager] Error cargando imagen {}: {}", srcPath, e.getMessage(), e);
            return null;
        }
    } // --- Fin del metodo cargarImagen ---


    private Color colorFromInt(Integer argb) {
        return argb != null ? new Color(argb, true) : null;
    } // --- Fin del metodo colorFromInt ---

} // --- Fin de la clase EditorDocumentManager ---