package controlador;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.utils.ComponentRegistry;
import controlador.worker.Zip2PngWorker;
import controlador.worker.Zip2PngWorker.SourceInfo;
import modelo.renderer.ImageEntry;
import modelo.renderer.StlEntry;
import modelo.renderer.StlMeshBuilder;
import modelo.renderer.Triangle;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.renderer.AwtModelRenderer;
import servicios.renderer.StlParser;
import servicios.renderer.ZipExtractor;
import servicios.renderer.Zip2PngScanner;
import servicios.renderer.Zip2PngScanner.RenderCandidate;
import vista.dialogos.TaskProgressDialog;
import vista.panels.render.PreviewPanel3DFX;
import vista.panels.render.RenderPanel;

public class RenderController {

    private static final Logger logger = LoggerFactory.getLogger(RenderController.class);

    private final RenderPanel panel;
    private final ConfigurationManager config;
    private final Component parentFrame;
    private final DefaultListModel<StlEntry> contentModel;
    private final Map<Path, SourceInfo> pngSourceMap = new HashMap<>();
    private final Map<Path, List<Triangle>> triangleCache = new HashMap<>();
    private final AwtModelRenderer renderer = new AwtModelRenderer();

    private Path lastScanFolder;
    private Path outputDir;
    private Path imagesDir;
    private volatile Path currentPreviewPath;
    private List<Triangle> currentTriangles;
    private Zip2PngWorker currentWorker;
    private volatile boolean loadingTriangles;
    private ComponentRegistry registry;

    // Punteros de selección por pestaña
    private int pointerSinRenderizar = 0;
    private int pointerConImagen = 0;
    private boolean syncingFromGrid;

    public RenderController(RenderPanel panel, ConfigurationManager config, Component parentFrame) {
        this.panel = panel;
        this.config = config;
        this.parentFrame = parentFrame;
        this.contentModel = panel.getContentListModel();
        initOutputDir();
        wireControls();
        wireBackgroundControls();
    }

    public void setRegistry(ComponentRegistry registry) {
        this.registry = registry;
    }

    private void initOutputDir() {
        String temp = config.getString(ConfigKeys.ZIP2PNG_CARPETA_TEMP,
                System.getProperty("java.io.tmpdir") + File.separator + "visor_zip2png");
        outputDir = Path.of(temp);
        imagesDir = outputDir.resolve("imagenes");
        try {
            if (Files.exists(outputDir)) {
                try (var files = Files.list(outputDir)) {
                    files.forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ex) {} });
                }
            }
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            logger.error("No se pudo inicializar directorio temporal: {}", outputDir, e);
        }
    }

    private void wireControls() {
        // Listener para ambas listas de candidatos
        javax.swing.event.ListSelectionListener candidateListener = e -> {
            if (e.getValueIsAdjusting()) return;
            onCandidateSelected();
        };
        panel.getCandidateListSinImagen().addListSelectionListener(candidateListener);
        panel.getCandidateListConImagen().addListSelectionListener(candidateListener);

        // Cambio de pestaña de candidatos: actualiza contenido inferior y card
        panel.getCandidateTabs().addChangeListener(e -> {
            onCandidateTabChanged();
        });

        // Doble clic en STL del ZIP
        panel.getContentList().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onStlDoubleClick();
                }
            }
        });

        // Doble clic en imagen del ZIP → extrae y muestra en visor 2D
        panel.getContentImageList().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onImageDoubleClick();
                }
            }
        });

        javax.swing.event.ChangeListener sliderListener = ev -> {
            JSlider src = (javax.swing.JSlider) ev.getSource();
            if (src == panel.getBrightnessSlider()) {
                panel.getBrightnessField().setText(String.valueOf(src.getValue()));
            } else if (src == panel.getContrastSlider()) {
                panel.getContrastField().setText(String.valueOf(src.getValue()));
            }
            applyAdjustments();
        };
        panel.getBrightnessSlider().addChangeListener(sliderListener);
        panel.getContrastSlider().addChangeListener(sliderListener);

        java.awt.event.KeyAdapter fieldListener = new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent ev) {
                javax.swing.JTextField field = (javax.swing.JTextField) ev.getSource();
                try {
                    int val = Integer.parseInt(field.getText().trim());
                    val = Math.max(-100, Math.min(100, val));
                    if (field == panel.getBrightnessField()) {
                        panel.getBrightnessSlider().setValue(val);
                    } else if (field == panel.getContrastField()) {
                        panel.getContrastSlider().setValue(val);
                    }
                    applyAdjustments();
                } catch (NumberFormatException ignored) {}
            }
        };
        panel.getBrightnessField().addKeyListener(fieldListener);
        panel.getContrastField().addKeyListener(fieldListener);

        panel.getChkCheckerboard().addActionListener(e -> applyAdjustments());
        panel.getChkAntiAlias().addActionListener(e -> {
            try {
                applyAdjustments();
            } catch (Exception ex) {
                logger.error("[RenderController] Error al aplicar antialiasing", ex);
            }
        });
        panel.getChkCrosshair().addActionListener(e -> applyAdjustments());
    }

    private void wireBackgroundControls() {
        java.awt.event.ActionListener radioListener = e -> {
            syncBackgroundCard();
            syncBackgroundToPreview();
        };
        panel.getRbSolid().addActionListener(radioListener);
        panel.getRbGradient().addActionListener(radioListener);
        panel.getRbImage().addActionListener(radioListener);
        panel.getRbTransparent().addActionListener(radioListener);

        panel.getBtnSolidColor().addActionListener(e -> {
            Color c = JColorChooser.showDialog(parentFrame, "Color de fondo s\u00F3lido", panel.getSolidBgColor());
            if (c != null) {
                panel.setSolidBgColor(c);
                panel.getPreview3DFX().setSolidBgColor(c);
            }
        });

        panel.getBtnGradientStart().addActionListener(e -> {
            Color c = JColorChooser.showDialog(parentFrame,
                    "Color inicio degradado", panel.getGradientStartColor());
            if (c != null) {
                panel.setGradientStartColor(c);
                panel.getPreview3DFX().setGradientBgColors(
                        panel.getGradientStartColor(), panel.getGradientEndColor());
            }
        });

        panel.getBtnGradientEnd().addActionListener(e -> {
            Color c = JColorChooser.showDialog(parentFrame,
                    "Color fin degradado", panel.getGradientEndColor());
            if (c != null) {
                panel.setGradientEndColor(c);
                panel.getPreview3DFX().setGradientBgColors(
                        panel.getGradientStartColor(), panel.getGradientEndColor());
            }
        });

        panel.getBtnBrowseImage().addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter(
                    "Im\u00E1genes (PNG, JPG, JPEG, BMP, GIF)", "png", "jpg", "jpeg", "bmp", "gif"));
            if (chooser.showOpenDialog(parentFrame) == JFileChooser.APPROVE_OPTION) {
                Path path = chooser.getSelectedFile().toPath();
                panel.getBgImageField().setText(path.toString());
                loadAndSetBgImage(path);
            }
        });

        panel.getBgImageScaleSlider().addChangeListener(e -> {
            if (!panel.getBgImageScaleSlider().getValueIsAdjusting()) {
                int val = panel.getBgImageScaleSlider().getValue();
                double scale = val / 50.0;
                panel.getBgImageScaleLabel().setText(String.format("%.1fx", scale));
                panel.getPreview3DFX().setBackgroundImageScale(scale);
            }
        });
    }

    private void syncBackgroundCard() {
        panel.showBgCard(panel.getSelectedBgMode());
    }

    private void syncBackgroundToPreview() {
        String mode = panel.getSelectedBgMode();
        switch (mode) {
            case "solid":
                panel.getPreview3DFX().setBackgroundMode(PreviewPanel3DFX.BgMode.SOLID);
                panel.getPreview3DFX().setSolidBgColor(panel.getSolidBgColor());
                break;
            case "gradient":
                panel.getPreview3DFX().setBackgroundMode(PreviewPanel3DFX.BgMode.GRADIENT);
                panel.getPreview3DFX().setGradientBgColors(panel.getGradientStartColor(), panel.getGradientEndColor());
                break;
            case "image":
                panel.getPreview3DFX().setBackgroundMode(PreviewPanel3DFX.BgMode.IMAGE);
                break;
            case "transparent":
                panel.getPreview3DFX().setBackgroundMode(PreviewPanel3DFX.BgMode.TRANSPARENT);
                break;
        }
    }

    private void loadAndSetBgImage(Path path) {
        try {
            javafx.scene.image.Image fxImage = new javafx.scene.image.Image(path.toUri().toURL().toString());
            panel.getPreview3DFX().setBackgroundImage(fxImage);
            if ("image".equals(panel.getSelectedBgMode())) {
                panel.getPreview3DFX().setBackgroundMode(PreviewPanel3DFX.BgMode.IMAGE);
            }
        } catch (Exception e) {
            logger.warn("No se pudo cargar imagen de fondo: {}", path, e);
            JOptionPane.showMessageDialog(parentFrame,
                    "No se pudo cargar la imagen:\n" + e.getMessage(),
                    "Fondo", JOptionPane.ERROR_MESSAGE);
        }
    }

    private BufferedImage loadBgImageAWT(Path path) {
        try {
            return ImageIO.read(path.toFile());
        } catch (IOException e) {
            logger.warn("No se pudo cargar imagen AWT: {}", path, e);
            return null;
        }
    }

    private void applyAdjustments() {
        panel.getPreview3DFX().setBrightness(panel.getBrightness());
        panel.getPreview3DFX().setContrast(panel.getContrast());
        panel.getPreview3DFX().setCheckerboard(panel.isCheckerboard());
        panel.getPreview3DFX().setAntiAlias(panel.isAntiAlias());
        panel.getPreview3DFX().setCrosshairVisible(panel.isCrosshair());
    }

    private RenderCandidate getSelectedCandidate() {
        if (panel.isCandidateTabSinRenderizar()) {
            return panel.getCandidateListSinImagen().getSelectedValue();
        }
        return panel.getCandidateListConImagen().getSelectedValue();
    }

    private void onCandidateTabChanged() {
        guardarPunteroActual();
        restaurarPunteroNuevo();
        panel.syncGridToCandidateTab();
    }

    private void guardarPunteroActual() {
        int idxSin = panel.getCandidateListSinImagen().getSelectedIndex();
        if (idxSin >= 0) pointerSinRenderizar = idxSin;
        int idxCon = panel.getCandidateListConImagen().getSelectedIndex();
        if (idxCon >= 0) pointerConImagen = idxCon;
    }

    private void restaurarPunteroNuevo() {
        if (panel.isCandidateTabSinRenderizar()) {
            int size = panel.getListModelSinImagen().getSize();
            int idx = Math.min(pointerSinRenderizar, size - 1);
            if (idx >= 0 && idx < size) {
                panel.getCandidateListSinImagen().clearSelection();
                panel.getCandidateListSinImagen().setSelectedIndex(idx);
            }
        } else {
            int size = panel.getListModelConImagen().getSize();
            int idx = Math.min(pointerConImagen, size - 1);
            if (idx >= 0 && idx < size) {
                panel.getCandidateListConImagen().clearSelection();
                panel.getCandidateListConImagen().setSelectedIndex(idx);
            }
        }
    }

    private void onCandidateSelected() {
        if (syncingFromGrid) return;
        RenderCandidate selected = getSelectedCandidate();
        contentModel.clear();
        panel.getContentImageListModel().clear();
        actualizarInfobarRender(selected);
        int idxSin = panel.getCandidateListSinImagen().getSelectedIndex();
        if (idxSin >= 0) pointerSinRenderizar = idxSin;
        int idxCon = panel.getCandidateListConImagen().getSelectedIndex();
        if (idxCon >= 0) pointerConImagen = idxCon;
        if (selected == null || !selected.esComprimido) return;

        actualizarContenidoInferior();
        if (panel.isCandidateTabSinRenderizar()) {
            seleccionarThumbnailDeCandidato(selected);
        }
    }

    private void seleccionarThumbnailDeCandidato(RenderCandidate candidate) {
        Path thumbnailPath = encontrarThumbnailDelCandidato(candidate);
        if (thumbnailPath != null && Files.exists(thumbnailPath)) {
            showPreview(thumbnailPath);
        } else if (panel.isCandidateTabSinRenderizar() && candidate.esComprimido) {
            // Sin thumbnail todavía: mostrar el primer STL en el visor
            try {
                List<StlEntry> entries = ZipExtractor.listStlContents(candidate.path);
                if (!entries.isEmpty()) {
                    mostrarStlEnVisor(candidate, entries.get(0));
                }
            } catch (Exception ex) {
                logger.warn("No se pudo listar STLs para preview: {}", candidate.nombreBase, ex);
            }
        }
    }

    private Path encontrarThumbnailDelCandidato(RenderCandidate candidate) {
        Path assigned = outputDir.resolve(candidate.nombreBase + ".png");
        if (Files.exists(assigned)) return assigned;
        if (candidate.tieneImagenesDentro()) {
            Path imgDir = imagesDir.resolve(candidate.nombreBase);
            if (Files.isDirectory(imgDir)) {
                try (var walk = Files.walk(imgDir, 3)) {
                    return walk.filter(Files::isRegularFile)
                            .filter(p -> {
                                String n = p.getFileName().toString().toLowerCase();
                                return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg");
                            })
                            .findFirst().orElse(null);
                } catch (IOException ignored) {}
            }
        }
        return null;
    }

    private void actualizarContenidoInferior() {
        RenderCandidate selected = getSelectedCandidate();
        contentModel.clear();
        panel.getContentImageListModel().clear();
        panel.getImagenesGrid().removeAll();
        panel.getImagenesGrid().revalidate();
        panel.getImagenesGrid().repaint();
        if (selected == null || !selected.esComprimido) {
            panel.showContentCard(panel.isCandidateTabSinRenderizar() ? "stl" : "img");
            return;
        }

        if (panel.isCandidateTabSinRenderizar()) {
            panel.showContentCard("stl");
            try {
                List<StlEntry> entries = ZipExtractor.listStlContents(selected.path);
                entries.sort(Comparator.comparingLong(StlEntry::sizeBytes).reversed());
                for (StlEntry entry : entries) {
                    contentModel.addElement(entry);
                }
            } catch (Exception ex) {
                logger.warn("No se pudo listar STLs de {}: {}", selected.nombreBase, ex.getMessage());
            }
        } else {
            panel.showContentCard("img");
            try {
                List<ImageEntry> images = ZipExtractor.listImageContents(selected.path);
                for (ImageEntry img : images) {
                    panel.getContentImageListModel().addElement(img);
                }
            } catch (Exception ex) {
                logger.warn("No se pudo listar imágenes de {}: {}", selected.nombreBase, ex.getMessage());
            }
            extraerImagenesCandidato(selected);
        }
    }

    private void mostrarStlEnVisor(RenderCandidate candidate, StlEntry stl) {
        panel.show3DView();
        new SwingWorker<Void, Void>() {
            private Path tempDir;
            @Override
            protected Void doInBackground() throws Exception {
                Path stlPath;
                if (candidate.esComprimido) {
                    tempDir = ZipExtractor.extractToTemp(candidate.path);
                    stlPath = tempDir.resolve(stl.filename());
                    if (!Files.exists(stlPath)) return null;
                } else {
                    stlPath = candidate.path;
                }
                List<Triangle> triangles = StlParser.parse(stlPath.toFile());
                SwingUtilities.invokeLater(() -> {
                    panel.getPreview3DFX().setMesh(triangles);
                    resetPreviewAdjustments();
                });
                return null;
            }
            @Override
            protected void done() {
                if (tempDir != null) ZipExtractor.deleteDir(tempDir);
            }
        }.execute();
    }

    private void extraerImagenesCandidato(RenderCandidate candidate) {
        if (candidate == null || !candidate.esComprimido || candidate.imagenesInternas.isEmpty()) return;

        new SwingWorker<Void, Void>() {
            private final String baseName = candidate.nombreBase;
            @Override
            protected Void doInBackground() throws Exception {
                Path imgOut = imagesDir.resolve(baseName);
                // Limpiar extracción previa de este candidato
                if (Files.exists(imgOut)) {
                    try (var walk = Files.walk(imgOut)) {
                        walk.sorted(java.util.Comparator.reverseOrder())
                                .forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
                    }
                }
                Files.createDirectories(imgOut);
                for (ImageEntry img : candidate.imagenesInternas) {
                    if (isCancelled()) return null;
                    try {
                        ZipExtractor.extractSingleFile(candidate.path, img.filename(), imgOut);
                    } catch (Exception ex) {
                        logger.warn("No se pudo extraer {} de {}: {}", img.filename(), baseName, ex.getMessage());
                    }
                }
                return null;
            }
            @Override
            protected void done() {
                refreshThumbnails(null);
                panel.syncGridToCandidateTab();
                RenderCandidate current = getSelectedCandidate();
                if (current != null && current.nombreBase.equals(baseName)) {
                    seleccionarThumbnailDeCandidato(current);
                }
            }
        }.execute();
    }

    private void actualizarInfobarRender(RenderCandidate candidate) {
        if (registry == null) return;
        String nombre = "(ninguno)";
        String ruta = "N/A";
        String tam = "N/A";
        String fecha = "N/A";
        String fmt = "N/A";
        String idxStr = "0/0";

        if (candidate != null) {
            nombre = candidate.path.getFileName().toString();
            ruta = candidate.path.getParent() != null ? candidate.path.getParent().toString() : "";
            fmt = candidate.esComprimido ? extraerExtension(candidate.path) : "STL/OBJ";
            try {
                java.nio.file.attribute.BasicFileAttributes attrs = java.nio.file.Files.readAttributes(candidate.path, java.nio.file.attribute.BasicFileAttributes.class);
                tam = formatFileSize(attrs.size());
                fecha = new java.text.SimpleDateFormat("dd/MM/yy HH:mm").format(new java.util.Date(attrs.lastModifiedTime().toMillis()));
            } catch (Exception ignored) {}

            // Índice relativo a la pestaña activa
            if (panel.isCandidateTabSinRenderizar()) {
                List<RenderCandidate> lista = java.util.Collections.list(panel.getListModelSinImagen().elements());
                int idx = lista.indexOf(candidate);
                if (idx >= 0) idxStr = (idx + 1) + "/" + lista.size();
            } else {
                List<RenderCandidate> lista = java.util.Collections.list(panel.getListModelConImagen().elements());
                int idx = lista.indexOf(candidate);
                if (idx >= 0) idxStr = (idx + 1) + "/" + lista.size();
            }
        }

        javax.swing.JTextField pathField = registry.get("textfield.info.rutaImagen");
        if (pathField != null) pathField.setText("Ruta: " + ruta);

        javax.swing.JLabel label;
        label = registry.get("label.info.nombreArchivo");
        if (label != null) label.setText("Archivo: " + nombre);

        label = registry.get("label.info.dimensiones");
        if (label != null) label.setText("Dim: N/A");

        label = registry.get("label.info.indiceTotal");
        if (label != null) label.setText("Idx: " + idxStr);

        label = registry.get("label.info.tamano");
        if (label != null) label.setText("Tam: " + tam);

        label = registry.get("label.info.fecha");
        if (label != null) label.setText("Fch: " + fecha);

        label = registry.get("label.info.formatoImagen");
        if (label != null) label.setText("Fmt: " + fmt);

        label = registry.get("label.info.modoZoom");
        if (label != null) label.setText("Modo: N/A");

        label = registry.get("label.info.porcentajeZoom");
        if (label != null) label.setText("%Z: N/A");
    }

    private String extraerExtension(java.nio.file.Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toUpperCase() : "?";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre).replace(',', '.');
    }

    private void onStlDoubleClick() {
        RenderCandidate candidate = getSelectedCandidate();
        StlEntry stl = panel.getContentList().getSelectedValue();
        if (candidate == null || stl == null) return;

        panel.show3DView();

        TaskProgressDialog dialog = new TaskProgressDialog(getParentFrame(),
                "Previsualizando", "Cargando " + stl.filename() + "...");

        new SwingWorker<Void, Void>() {
            private Path tempDir;
            @Override
            protected Void doInBackground() throws Exception {
                Path stlPath;
                if (candidate.esComprimido) {
                    tempDir = ZipExtractor.extractToTemp(candidate.path);
                    stlPath = tempDir.resolve(stl.filename());
                    if (!Files.exists(stlPath)) return null;
                } else {
                    stlPath = candidate.path;
                }
                List<Triangle> triangles = StlParser.parse(stlPath.toFile());
                SwingUtilities.invokeLater(() -> {
                    panel.getPreview3DFX().setMesh(triangles);
                    resetPreviewAdjustments();
                });
                return null;
            }
            @Override
            protected void done() {
                if (tempDir != null) ZipExtractor.deleteDir(tempDir);
                dialog.closeDialog();
            }
        }.execute();
        dialog.setVisible(true);
    }

    private void resetPreviewAdjustments() {
        panel.getBrightnessSlider().setValue(0);
        panel.getBrightnessField().setText("0");
        panel.getContrastSlider().setValue(0);
        panel.getContrastField().setText("0");
        panel.getChkCheckerboard().setSelected(false);
        panel.getChkAntiAlias().setSelected(false);
        panel.getChkCrosshair().setSelected(true);
    }

    private void onImageDoubleClick() {
        RenderCandidate candidate = getSelectedCandidate();
        ImageEntry img = panel.getContentImageList().getSelectedValue();
        if (candidate == null || img == null) return;

        // Extraer la imagen a outputDir/imagenes/ y mostrarla
        TaskProgressDialog dialog = new TaskProgressDialog(getParentFrame(),
                "Extrayendo imagen", "Extrayendo " + img.filename() + "...");

        new SwingWorker<Path, Void>() {
            @Override
            protected Path doInBackground() throws Exception {
                Path imgOutput = imagesDir.resolve(candidate.nombreBase);
                Files.createDirectories(imgOutput);
                ZipExtractor.extractSingleFile(candidate.path, img.filename(), imgOutput);
                Path extracted = imgOutput.resolve(img.filename());
                if (Files.exists(extracted)) return extracted;
                // Buscar recursivamente
                try (var walk = Files.walk(imgOutput)) {
                    return walk.filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().equalsIgnoreCase(
                                    Path.of(img.filename()).getFileName().toString()))
                            .findFirst().orElse(null);
                }
            }
            @Override
            protected void done() {
                dialog.closeDialog();
                try {
                    Path extracted = get();
                    if (extracted != null && Files.exists(extracted)) {
                        showImagePreview(extracted);
                    }
                } catch (Exception ex) {
                    logger.warn("Error extrayendo imagen {}", img.filename(), ex);
                }
            }
        }.execute();
        dialog.setVisible(true);
    }

    private void onScan(ActionEvent e) {
        JFileChooser chooser = new JFileChooser(lastScanFolder != null ? lastScanFolder.toFile() : null);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Seleccionar carpeta para escanear");
        if (chooser.showOpenDialog(parentFrame) != JFileChooser.APPROVE_OPTION) return;

        Path folder = chooser.getSelectedFile().toPath();
        lastScanFolder = folder;

        JFrame frame = getParentFrame();
        if (frame == null) {
            logger.warn("No se pudo obtener JFrame padre para TaskProgressDialog");
            return;
        }

        long limiteMb = config.getInt(ConfigKeys.ZIP2PNG_LIMITE_MB, 512);
        long limiteBytes = limiteMb * 1024L * 1024L;

        panel.getListModelSinImagen().clear();
        panel.getListModelConImagen().clear();
        panel.actualizarTitulosPestanyas();
        contentModel.clear();
        panel.getContentImageListModel().clear();
        pngSourceMap.clear();
        triangleCache.clear();

        TaskProgressDialog dialog = new TaskProgressDialog(frame, "Escaneando", "Buscando archivos sin imagen...");
        new SwingWorker<List<RenderCandidate>, Integer>() {
            @Override
            protected List<RenderCandidate> doInBackground() {
                Zip2PngScanner scanner = new Zip2PngScanner(limiteBytes);
                List<RenderCandidate> candidates = scanner.scanFolder(folder);
                Zip2PngScanner.detectarImagenesEnArchivos(candidates);
                publish(candidates.size());
                logger.info("Escaneo completado: {} candidatos encontrados en {}", candidates.size(), folder);
                return candidates;
            }
            @Override
            protected void process(List<Integer> chunks) {
                int count = chunks.get(chunks.size() - 1);
                dialog.updateStatusText("Candidatos encontrados: " + count);
            }
            @Override
            protected void done() {
                try {
                    List<RenderCandidate> candidates = get();
                    int sinImg = 0, conImg = 0;
                    for (RenderCandidate c : candidates) {
                        if (c.tieneImagenesDentro()) {
                            panel.getListModelConImagen().addElement(c);
                            conImg++;
                        } else {
                            panel.getListModelSinImagen().addElement(c);
                            sinImg++;
                        }
                    }
                    panel.actualizarTitulosPestanyas();
                    dialog.updateStatusText("Sin renderizar: " + sinImg + " | Con imagen: " + conImg);
                } catch (Exception ex) {
                    logger.error("Error al finalizar escaneo", ex);
                }
                dialog.closeDialog();
            }
        }.execute();
        dialog.setVisible(true);
    }

    private List<RenderCandidate> getAllCandidates() {
        List<RenderCandidate> all = new ArrayList<>();
        java.util.Collections.list(panel.getListModelSinImagen().elements()).forEach(all::add);
        java.util.Collections.list(panel.getListModelConImagen().elements()).forEach(all::add);
        return all;
    }

    private void onProcess(ActionEvent e) {
        List<RenderCandidate> candidates = getAllCandidates();
        if (candidates.isEmpty()) {
            JOptionPane.showMessageDialog(parentFrame,
                    "No hay candidatos. Escanea una carpeta primero.",
                    "Zip2PNG", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        limpiarOutputDir();
        panel.getImagenesGrid().removeAll();
        panel.getRendersGrid().removeAll();
        panel.getRendersGrid().revalidate();
        panel.getRendersGrid().repaint();
        panel.getImagenesGrid().revalidate();
        panel.getImagenesGrid().repaint();
        pngSourceMap.clear();
        triangleCache.clear();
        currentPreviewPath = null;
        currentTriangles = null;
        currentWorker = null;

        JFrame frame = getParentFrame();
        if (frame == null) {
            logger.warn("No se pudo obtener JFrame padre para TaskProgressDialog");
            return;
        }
        panel.actualizarTitulosPestanyas();
        TaskProgressDialog dialog = new TaskProgressDialog(frame, "Zip2PNG", "Procesando...");

        Zip2PngWorker worker = new Zip2PngWorker(candidates, outputDir, dialog, () -> {
            onProcessCompleted();
        });
        this.currentWorker = worker;
        worker.execute();
        dialog.setVisible(true);
    }

    private void onProcessCompleted() {
        if (currentWorker != null) {
            for (SourceInfo si : currentWorker.getSourceInfos()) {
                pngSourceMap.put(si.pngPath(), si);
            }
        }
        SwingUtilities.invokeLater(() -> {
            refreshThumbnails(null);
            // Auto-seleccionar primer candidato en "Sin renderizar"
            if (panel.getListModelSinImagen().getSize() > 0) {
                panel.getCandidateTabs().setSelectedIndex(0);
                pointerSinRenderizar = 0;
                panel.getCandidateListSinImagen().setSelectedIndex(0);
            } else if (panel.getListModelConImagen().getSize() > 0) {
                panel.getCandidateTabs().setSelectedIndex(1);
                pointerConImagen = 0;
                panel.getCandidateListConImagen().setSelectedIndex(0);
            }
            panel.syncGridToCandidateTab();
        });
    }

    private void limpiarOutputDir() {
        if (outputDir == null) return;
        try {
            if (Files.exists(outputDir)) {
                try (var walk = Files.walk(outputDir)) {
                    walk.sorted(java.util.Comparator.reverseOrder())
                            .forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ex) {} });
                }
            }
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            logger.warn("No se pudo limpiar directorio temporal: {}", outputDir, e);
        }
    }

    private void refreshThumbnails(Path highlightPng) {
        panel.getRendersGrid().removeAll();
        panel.getImagenesGrid().removeAll();

        // Renders 3D: un thumbnail por candidato sin imagen
        java.util.Enumeration<RenderCandidate> sinRender = panel.getListModelSinImagen().elements();
        while (sinRender.hasMoreElements()) {
            RenderCandidate c = sinRender.nextElement();
            Path png = outputDir.resolve(c.nombreBase + ".png");
            if (Files.exists(png)) {
                addThumbnailToGrid(png, panel.getRendersGrid(), highlightPng, c.nombreBase, c);
            }
        }

        // Imágenes: un thumbnail por candidato con imagen (primera imagen encontrada)
        java.util.Enumeration<RenderCandidate> conImg = panel.getListModelConImagen().elements();
        while (conImg.hasMoreElements()) {
            RenderCandidate c = conImg.nextElement();
            Path png = outputDir.resolve(c.nombreBase + ".png");
            if (Files.exists(png)) {
                addThumbnailToGrid(png, panel.getImagenesGrid(), highlightPng, c.nombreBase, c);
                continue;
            }
            Path imgDir = imagesDir.resolve(c.nombreBase);
            if (Files.isDirectory(imgDir)) {
                try (var walk = Files.walk(imgDir, 3)) {
                    walk.filter(Files::isRegularFile)
                            .filter(p -> {
                                String n = p.getFileName().toString().toLowerCase();
                                return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
                                        || n.endsWith(".gif") || n.endsWith(".bmp") || n.endsWith(".webp");
                            })
                            .findFirst()
                            .ifPresent(imgPath -> addThumbnailToGrid(imgPath, panel.getImagenesGrid(), highlightPng, c.nombreBase, c));
                } catch (IOException ignored) {}
            }
        }

        panel.getRendersGrid().revalidate();
        panel.getRendersGrid().repaint();
        panel.getImagenesGrid().revalidate();
        panel.getImagenesGrid().repaint();
    }

    private void addThumbnailToGrid(Path p, JPanel grid, Path highlightPng, String labelText, RenderCandidate candidate) {
        try {
            var icon = new javax.swing.ImageIcon(
                    new javax.swing.ImageIcon(p.toFile().toURI().toURL())
                            .getImage()
                            .getScaledInstance(180, 180, java.awt.Image.SCALE_SMOOTH));
            var label = new javax.swing.JLabel(icon);
            String tooltip = labelText + " - " + p.getFileName().toString();
            label.setToolTipText(tooltip);
            label.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

            if (highlightPng != null && p.getFileName().equals(highlightPng.getFileName())) {
                label.setBorder(javax.swing.BorderFactory.createLineBorder(
                        new java.awt.Color(100, 200, 255), 3));
            }

            label.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent ev) {
                    seleccionarCandidatoEnLista(candidate);
                    showPreview(p);
                }
            });
            grid.add(label);
        } catch (Exception ex) {
            logger.warn("No se pudo cargar thumbnail: {}", p);
        }
    }

    private void seleccionarCandidatoEnLista(RenderCandidate candidate) {
        if (candidate == null) return;
        syncingFromGrid = true;
        if (panel.getListModelSinImagen().getSize() > 0) {
            int idx = java.util.Collections.list(panel.getListModelSinImagen().elements()).indexOf(candidate);
            if (idx >= 0) {
                panel.getCandidateTabs().setSelectedIndex(0);
                panel.getCandidateListSinImagen().setSelectedIndex(idx);
                syncingFromGrid = false;
                return;
            }
        }
        if (panel.getListModelConImagen().getSize() > 0) {
            int idx = java.util.Collections.list(panel.getListModelConImagen().elements()).indexOf(candidate);
            if (idx >= 0) {
                panel.getCandidateTabs().setSelectedIndex(1);
                panel.getCandidateListConImagen().setSelectedIndex(idx);
            }
        }
        syncingFromGrid = false;
    }

    private boolean isPathInImagesDir(Path p) {
        return p.startsWith(imagesDir);
    }

    private void showPreview(Path pngPath) {
        logger.debug("[RenderController] showPreview: " + pngPath.getFileName());
        this.currentPreviewPath = pngPath;
        this.loadingTriangles = false;

        resetPreviewAdjustments();

        resaltarThumbnail(pngPath);

        // En pestaña "Con imagen" siempre mostrar en 2D (imagen asignada o extraída)
        if (panel.isCandidateTabConImagen() || isPathInImagesDir(pngPath)) {
            showImagePreview(pngPath);
        } else {
            showRenderPreview(pngPath);
        }
    }

    private void showImagePreview(Path imagePath) {
        panel.show2DView();
        try {
            BufferedImage img = ImageIO.read(imagePath.toFile());
            panel.set2DImage(img);
        } catch (IOException e) {
            logger.warn("No se pudo cargar imagen 2D: {}", imagePath, e);
            panel.set2DImage(null);
        }
    }

    private void showRenderPreview(Path pngPath) {
        panel.show3DView();
        currentTriangles = triangleCache.get(pngPath);
        logger.debug("[RenderController] triangleCache hit: " + (currentTriangles != null));
        if (currentTriangles != null) {
            panel.getPreview3DFX().setMesh(currentTriangles);
            panel.getPreview3DFX().setBrightness(0);
            panel.getPreview3DFX().setContrast(0);
            panel.getPreview3DFX().setCheckerboard(false);
            float[] bb = StlMeshBuilder.boundingBox(currentTriangles);
            logger.info("Renderizando el archivo: {} x: {} y: {} z: {}",
                pngPath.getFileName(), String.format("%.2f", bb[3] - bb[0]),
                String.format("%.2f", bb[4] - bb[1]), String.format("%.2f", bb[5] - bb[2]));
        } else {
            panel.getPreview3DFX().clearMesh();
            cargarTriangulosAsync();
        }
    }

    private void resaltarThumbnail(Path pngPath) {
        resaltarEnGrid(pngPath, panel.getRendersGrid());
        resaltarEnGrid(pngPath, panel.getImagenesGrid());
    }

    private void resaltarEnGrid(Path pngPath, JPanel grid) {
        for (java.awt.Component comp : grid.getComponents()) {
            if (comp instanceof javax.swing.JLabel label) {
                String tip = label.getToolTipText();
                if (tip != null && (tip.startsWith(pngPath.getFileName().toString())
                        || tip.endsWith(pngPath.getFileName().toString()))) {
                    label.setBorder(javax.swing.BorderFactory.createLineBorder(
                            new java.awt.Color(100, 200, 255), 3));
                } else if (label.getBorder() != null) {
                    label.setBorder(null);
                }
            }
        }
    }

    private void selectPreviewThumbnail(Path pngPath) {
        showPreview(pngPath);
        resaltarThumbnail(pngPath);
    }

    private void mostrarErrorCarga(String mensaje) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JOptionPane.showMessageDialog(parentFrame,
                    mensaje, "Error al cargar STL",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            panel.getPreview3DFX().clearMesh();
        });
    }

    private void cargarTriangulosAsync() {
        if (currentPreviewPath == null) { logger.warn("[RenderController] cargarTriangulosAsync: currentPreviewPath is null"); return; }
        if (loadingTriangles) { logger.info("[RenderController] cargarTriangulosAsync: ya cargando"); return; }
        loadingTriangles = true;

        final Path pathSiendoCargado = currentPreviewPath;
        logger.debug("[RenderController] cargarTriangulosAsync iniciando para: " + pathSiendoCargado.getFileName());
        logger.debug("[RenderController] pngSourceMap.size() = " + pngSourceMap.size());

        new SwingWorker<List<Triangle>, Void>() {
            private String errorMsg;
            @Override
            protected List<Triangle> doInBackground() throws Exception {
                logger.debug("[RenderController] Worker doInBackground: buscando SourceInfo...");
                SourceInfo info = pngSourceMap.get(pathSiendoCargado);
                if (info == null) {
                    errorMsg = "No se encontró información del origen para:\n"
                            + pathSiendoCargado.getFileName()
                            + "\n(pps registrados: " + pngSourceMap.size() + ")";
                    logger.error("[RenderController] ERROR: " + errorMsg);
                    return null;
                }
                logger.debug("[RenderController] SourceInfo encontrado: " + info.candidate().nombreBase
                        + ", esComprimido=" + info.candidate().esComprimido
                        + ", stl=" + info.stlEntry().filename());
                if (info.candidate().esComprimido) {
                    logger.debug("[RenderController] Extrayendo comprimido...");
                    Path tempDir = ZipExtractor.extractToTemp(info.candidate().path);
                    try {
                        Path stlPath = tempDir.resolve(info.stlEntry().filename());
                        logger.debug("[RenderController] STL path: " + stlPath);
                        if (!Files.exists(stlPath)) {
                            errorMsg = "No se encontró el archivo STL:\n" + info.stlEntry().filename();
                            logger.error("[RenderController] ERROR: " + errorMsg);
                            return null;
                        }
                        logger.debug("[RenderController] Parseando STL...");
                        List<Triangle> tris = StlParser.parse(stlPath.toFile());
                        logger.debug("[RenderController] STL parseado: " + tris.size() + " triángulos");
                        return tris;
                    } finally {
                        ZipExtractor.deleteDir(tempDir);
                    }
                } else {
                    logger.debug("[RenderController] Parseando STL (no comprimido)...");
                    return StlParser.parse(info.candidate().path.toFile());
                }
            }
            @Override
            protected void done() {
                loadingTriangles = false;
                logger.debug("[RenderController] Worker done()");
                try {
                    List<Triangle> tris = get();
                    logger.debug("[RenderController] tris = " + (tris != null ? tris.size() + " triángulos" : "null"));
                    if (tris != null && !tris.isEmpty()) {
                        triangleCache.put(pathSiendoCargado, tris);
                        if (pathSiendoCargado.equals(currentPreviewPath)) {
                            currentTriangles = tris;
                            logger.debug("[RenderController] Llamando setMesh...");
                            panel.getPreview3DFX().setMesh(tris);
                            panel.getPreview3DFX().setBrightness(panel.getBrightness());
                            panel.getPreview3DFX().setContrast(panel.getContrast());
                            panel.getPreview3DFX().setCheckerboard(panel.isCheckerboard());
                            panel.getPreview3DFX().setAntiAlias(panel.isAntiAlias());
                            panel.getPreview3DFX().setCrosshairVisible(panel.isCrosshair());
                            float[] bb = StlMeshBuilder.boundingBox(tris);
                            logger.info("Renderizando el archivo: {} x: {} y: {} z: {}",
                                pathSiendoCargado.getFileName(), String.format("%.2f", bb[3] - bb[0]),
                                String.format("%.2f", bb[4] - bb[1]), String.format("%.2f", bb[5] - bb[2]));
                            logger.debug("[RenderController] setMesh completado");
                        } else {
                            logger.warn("[RenderController] Omitiendo setMesh: path ha cambiado");
                        }
                    } else if (errorMsg != null) {
                        logger.error("[RenderController] Mostrando error: " + errorMsg);
                        mostrarErrorCarga(errorMsg);
                    } else {
                        logger.error("[RenderController] Error: triángulos vacíos");
                        mostrarErrorCarga("El archivo STL no contiene triángulos o está vacío.");
                    }
                } catch (Exception e) {
                    logger.warn("[RenderController] Excepción en done(): " + e.getMessage());
                    e.printStackTrace();
                    logger.warn("No se pudieron cargar tri\u00E1ngulos para preview", e);
                    mostrarErrorCarga("Error al cargar STL:\n" + e.getMessage());
                }
            }
        }.execute();
    }

    private JFrame getParentFrame() {
        if (parentFrame instanceof JFrame) return (JFrame) parentFrame;
        java.awt.Window win = SwingUtilities.getWindowAncestor(parentFrame);
        return (win instanceof JFrame) ? (JFrame) win : null;
    }

    private void copyToSource() {
        RenderCandidate selected = getSelectedCandidate();
        if (selected == null) {
            JOptionPane.showMessageDialog(parentFrame, "Selecciona un candidato de la lista.",
                    "Copiar a origen", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<Path> pngs = new ArrayList<>();
        try {
            String prefix = selected.nombreBase;
            try (var files = Files.list(outputDir)) {
                files.filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".png")
                            && (name.equals(prefix + ".png") || name.startsWith(prefix + "_"));
                }).forEach(pngs::add);
            }
        } catch (IOException ex) {
            logger.error("Error listando PNGs para copiar", ex);
        }

        if (pngs.isEmpty()) {
            JOptionPane.showMessageDialog(parentFrame, "El PNG a\u00FAn no se ha generado.",
                    "Copiar a origen", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path parent = selected.path.getParent();
        int copied = 0;
        for (Path png : pngs) {
            Path target = parent.resolve(png.getFileName());
            try {
                Files.copy(png, target, StandardCopyOption.REPLACE_EXISTING);
                copied++;
            } catch (IOException ex) {
                logger.error("Error copiando {} a {}", png, target, ex);
            }
        }
        JOptionPane.showMessageDialog(parentFrame, "Copiados " + copied + " archivo(s) a:\n" + parent,
                "Copiar a origen", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Procesa un único ZIP seleccionado en la lista de candidatos.
     */
    public void procesarArchivo() {
        RenderCandidate selected = getSelectedCandidate();
        if (selected == null) {
            JOptionPane.showMessageDialog(parentFrame,
                    "Selecciona un candidato de la lista primero.",
                    "Procesar Archivo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        limpiarOutputDir();
        panel.getRendersGrid().removeAll();
        panel.getRendersGrid().revalidate();
        panel.getRendersGrid().repaint();
        panel.getImagenesGrid().removeAll();
        panel.getImagenesGrid().revalidate();
        panel.getImagenesGrid().repaint();
        pngSourceMap.clear();
        triangleCache.clear();
        currentPreviewPath = null;
        currentTriangles = null;
        currentWorker = null;

        JFrame frame = getParentFrame();
        if (frame == null) return;

        panel.actualizarTitulosPestanyas();

        List<RenderCandidate> single = List.of(selected);
        TaskProgressDialog dialog = new TaskProgressDialog(frame, "Zip2PNG",
                "Procesando " + selected.nombreBase + "...");

        Zip2PngWorker worker = new Zip2PngWorker(single, outputDir, dialog, () -> {
            onProcessCompleted();
        });
        this.currentWorker = worker;
        worker.execute();
        dialog.setVisible(true);
    } // --- Fin del metodo procesarArchivo ---


    /**
     * Abre un diálogo para copiar los PNGs generados a la carpeta elegida.
     */
    public void copiarArchivos() {
        List<Path> pngs = new ArrayList<>();
        try (var files = Files.list(outputDir)) {
            files.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png")).forEach(pngs::add);
        } catch (IOException ex) {
            logger.error("Error listando PNGs para copiar", ex);
            return;
        }
        if (pngs.isEmpty()) {
            JOptionPane.showMessageDialog(parentFrame, "No hay PNGs generados para copiar.",
                    "Copiar Archivos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String rutaOrigen = !panel.getListModelSinImagen().isEmpty()
                ? panel.getListModelSinImagen().getElementAt(0).path.getParent().toString()
                : "";

        JDialog dialog = new JDialog(getParentFrame(), "Copiar Archivos Generados", true);
        dialog.setLayout(new BorderLayout(10, 10));
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;

        JRadioButton rbTemp = new JRadioButton("Carpeta temporal");
        JRadioButton rbOrigen = new JRadioButton("Carpeta de origen");
        JRadioButton rbCustom = new JRadioButton("Carpeta personalizada");
        ButtonGroup group = new ButtonGroup();
        group.add(rbTemp);
        group.add(rbOrigen);
        group.add(rbCustom);

        JTextField tfRuta = new JTextField(outputDir.toString());
        tfRuta.setEditable(false);
        JButton btnSeleccionar = new JButton("...");

        rbTemp.setSelected(true);
        rbTemp.addActionListener(ev -> tfRuta.setText(outputDir.toString()));
        rbOrigen.addActionListener(ev -> { if (!rutaOrigen.isEmpty()) tfRuta.setText(rutaOrigen); });
        rbCustom.addActionListener(ev -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                tfRuta.setText(chooser.getSelectedFile().toPath().toString());
            } else {
                rbTemp.setSelected(true);
                tfRuta.setText(outputDir.toString());
            }
        });

        gbc.gridy = 0;
        mainPanel.add(new JLabel("Destino:"), gbc);
        gbc.gridy = 1;
        mainPanel.add(rbTemp, gbc);
        gbc.gridy = 2;
        mainPanel.add(rbOrigen, gbc);
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        mainPanel.add(rbCustom, gbc);
        mainPanel.add(btnSeleccionar, gbc);
        gbc.gridwidth = 2;
        gbc.gridy = 4;
        mainPanel.add(tfRuta, gbc);
        gbc.gridy = 5;
        mainPanel.add(new JLabel(pngs.size() + " archivos a copiar"), gbc);

        dialog.add(mainPanel, BorderLayout.CENTER);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCopiar = new JButton("Copiar");
        JButton btnCancelar = new JButton("Cancelar");
        btnPanel.add(btnCopiar);
        btnPanel.add(btnCancelar);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        btnCopiar.addActionListener(ev -> {
            Path dest = Path.of(tfRuta.getText());
            if (!Files.exists(dest)) {
                try { Files.createDirectories(dest); } catch (IOException ex) {
                    logger.error("No se pudo crear carpeta {}", dest, ex);
                    return;
                }
            }
            for (Path p : pngs) {
                try {
                    Files.copy(p, dest.resolve(p.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    logger.error("Error copiando {} a {}", p, dest, ex);
                }
            }
            dialog.dispose();
            JOptionPane.showMessageDialog(parentFrame, "Copiados " + pngs.size() + " archivos a:\n" + dest,
                    "Copiar Archivos", JOptionPane.INFORMATION_MESSAGE);
        });
        btnCancelar.addActionListener(ev -> dialog.dispose());

        dialog.setSize(450, 320);
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setVisible(true);
    } // --- Fin del metodo copiarArchivos ---


    private void copyToFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Seleccionar carpeta de destino");
        if (chooser.showSaveDialog(parentFrame) != JFileChooser.APPROVE_OPTION) return;

        Path destFolder = chooser.getSelectedFile().toPath();
        try (var files = Files.list(outputDir)) {
            files.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
                    .forEach(p -> {
                        try {
                            Files.copy(p, destFolder.resolve(p.getFileName()),
                                    StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException ex) {
                            logger.error("Error copiando {} a {}", p, destFolder, ex);
                        }
                    });
            JOptionPane.showMessageDialog(parentFrame, "Copiado a:\n" + destFolder,
                    "Copiar a carpeta", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            logger.error("Error listando archivos para copiar", ex);
        }
    }

    private void openTempFolder() {
        try {
            Desktop.getDesktop().open(outputDir.toFile());
        } catch (IOException | UnsupportedOperationException e) {
            logger.warn("No se pudo abrir carpeta temporal: {}", outputDir, e);
            JOptionPane.showMessageDialog(parentFrame,
                    "Carpeta temporal:\n" + outputDir,
                    "Zip2PNG", JOptionPane.INFORMATION_MESSAGE);
        }
    } // --- Fin del metodo openTempFolder ---


    /**
     * Wrapper público para escanear una carpeta (desde toolbar).
     */
    public void ejecutarScan() {
        onScan(null);
    } // --- Fin del metodo ejecutarScan ---


    /**
     * Wrapper público para procesar todos los candidatos (desde toolbar).
     */
    public void ejecutarProcess() {
        onProcess(null);
    } // --- Fin del metodo ejecutarProcess ---


    /**
     * Wrapper público para abrir la carpeta temporal (desde toolbar).
     */
    public void ejecutarAbrirTemp() {
        openTempFolder();
    } // --- Fin del metodo ejecutarAbrirTemp ---


    /**
     * Re-renderiza el STL actual con la orientación del preview 3D y los
     * ajustes de brillo/contraste/AA/fondo. Sobrescribe el PNG en outputDir
     * y actualiza la rejilla de thumbnails.
     */
    public void asignarPreviewAlArchivo() {
        if (panel.isCandidateTabConImagen()) {
            asignarImagenAlGrid();
        } else {
            asignarRenderAlGrid();
        }
    } // --- Fin del metodo asignarPreviewAlArchivo ---

    private void asignarImagenAlGrid() {
        RenderCandidate selected = getSelectedCandidate();
        if (selected == null || currentPreviewPath == null || !Files.exists(currentPreviewPath)) {
            JOptionPane.showMessageDialog(parentFrame,
                    "No hay imagen cargada en el visor.\n"
                    + "Haz doble clic en una imagen de la lista primero.",
                    "Asignar al grid", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            BufferedImage captura = panel.capturarVistaActual();
            Path dest = outputDir.resolve(selected.nombreBase + ".png");
            if (captura != null) {
                ImageIO.write(captura, "PNG", dest.toFile());
                logger.info("Vista capturada guardada: {} (zoom={})",
                        dest.getFileName(), String.format("%.2f", panel.getImageZoom()));
            } else {
                Files.copy(currentPreviewPath, dest, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Imagen original copiada al grid: {}", dest.getFileName());
            }
            refreshThumbnails(dest);
        } catch (IOException e) {
            logger.error("Error al asignar imagen al grid", e);
            JOptionPane.showMessageDialog(parentFrame,
                    "Error al guardar la imagen:\n" + e.getMessage(),
                    "Asignar al grid", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void asignarRenderAlGrid() {
        if (currentTriangles == null || currentPreviewPath == null) {
            JOptionPane.showMessageDialog(parentFrame,
                    "No hay ning\u00FAn modelo cargado en el preview.\n"
                    + "Haz clic en un thumbnail de la rejilla primero.",
                    "Asignar preview", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double rotX = panel.getPreview3DFX().getRotateXAngle();
        double rotY = panel.getPreview3DFX().getRotateYAngle();
        int brightness = panel.getBrightnessSlider().getValue();
        int contrast = panel.getContrastSlider().getValue();
        boolean antiAlias = panel.getChkAntiAlias().isSelected();

        String bgMode = panel.getSelectedBgMode();
        Color solidColor = panel.getSolidBgColor();
        Color gradientStart = panel.getGradientStartColor();
        Color gradientEnd = panel.getGradientEndColor();
        BufferedImage bgImage = null;
        double bgImageScale = 1.0;
        if ("image".equals(bgMode)) {
            String imgPath = panel.getBgImageField().getText();
            if (!imgPath.isEmpty()) {
                bgImage = loadBgImageAWT(Path.of(imgPath));
            }
            bgImageScale = panel.getBgImageScaleSlider().getValue() / 50.0;
        }

        try {
            BufferedImage img = renderer.renderizarConAjustes(currentTriangles,
                    rotX, rotY, antiAlias, brightness, contrast,
                    bgMode, solidColor, gradientStart, gradientEnd,
                    bgImage, bgImageScale);
            ImageIO.write(img, "PNG", currentPreviewPath.toFile());
            logger.info("Preview re-renderizado y guardado: {} (rotX={}, rotY={}, AA={}, fondo={})",
                    currentPreviewPath.getFileName(),
                    String.format("%.1f", rotX), String.format("%.1f", rotY),
                    antiAlias, bgMode);
            triangleCache.put(currentPreviewPath, currentTriangles);
            refreshThumbnails(currentPreviewPath);
        } catch (IOException e) {
            logger.error("Error al guardar PNG re-renderizado: {}", currentPreviewPath, e);
            JOptionPane.showMessageDialog(parentFrame,
                    "Error al guardar la imagen:\n" + e.getMessage(),
                    "Asignar preview", JOptionPane.ERROR_MESSAGE);
        }
    } // --- Fin del metodo asignarRenderAlGrid ---


} // --- Fin de la clase RenderController ---
