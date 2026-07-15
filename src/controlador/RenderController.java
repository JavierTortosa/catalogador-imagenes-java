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
    private volatile Path currentPreviewPath;
    private List<Triangle> currentTriangles;
    private Zip2PngWorker currentWorker;
    private volatile boolean loadingTriangles;
    private ComponentRegistry registry;

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
        panel.getCandidateList().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            onCandidateSelected();
        });

        panel.getContentList().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onStlDoubleClick();
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

    private void onCandidateSelected() {
        RenderCandidate selected = panel.getCandidateList().getSelectedValue();
        contentModel.clear();
        actualizarInfobarRender(selected);
        if (selected == null || !selected.esComprimido) return;

        try {
            List<StlEntry> entries = ZipExtractor.listStlContents(selected.path);
            entries.sort(Comparator.comparingLong(StlEntry::sizeBytes).reversed());
            for (StlEntry entry : entries) {
                contentModel.addElement(entry);
            }
        } catch (Exception ex) {
            logger.warn("No se pudo listar contenido de {}: {}", selected.nombreBase, ex.getMessage());
        }
    }

    private void actualizarInfobarRender(RenderCandidate candidate) {
        if (registry == null) return;
        java.util.List<RenderCandidate> all = java.util.Collections.list(panel.getListModel().elements());
        int total = all.size();
        int idx = -1;
        String nombre = "(ninguno)";
        String ruta = "N/A";
        String tam = "N/A";
        String fecha = "N/A";
        String fmt = "N/A";

        if (candidate != null) {
            nombre = candidate.path.getFileName().toString();
            ruta = candidate.path.getParent() != null ? candidate.path.getParent().toString() : "";
            idx = all.indexOf(candidate);
            fmt = candidate.esComprimido ? extraerExtension(candidate.path) : "STL/OBJ";
            try {
                java.nio.file.attribute.BasicFileAttributes attrs = java.nio.file.Files.readAttributes(candidate.path, java.nio.file.attribute.BasicFileAttributes.class);
                tam = formatFileSize(attrs.size());
                fecha = new java.text.SimpleDateFormat("dd/MM/yy HH:mm").format(new java.util.Date(attrs.lastModifiedTime().toMillis()));
            } catch (Exception ignored) {}
        }

        javax.swing.JTextField pathField = registry.get("textfield.info.rutaImagen");
        if (pathField != null) pathField.setText("Ruta: " + ruta);

        javax.swing.JLabel label;
        label = registry.get("label.info.nombreArchivo");
        if (label != null) label.setText("Archivo: " + nombre);

        label = registry.get("label.info.dimensiones");
        if (label != null) label.setText("Dim: N/A");

        label = registry.get("label.info.indiceTotal");
        if (label != null) label.setText("Idx: " + (idx >= 0 ? (idx + 1) + "/" + total : "0/0"));

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
        RenderCandidate candidate = panel.getCandidateList().getSelectedValue();
        StlEntry stl = panel.getContentList().getSelectedValue();
        if (candidate == null || stl == null) return;

        String stem = stl.filename().replaceFirst("(?i)\\.stl$", "");
        stem = stem.replaceAll("[\\\\/:*?\"<>|]", "_");
        String pngName = candidate.nombreBase + "_" + stem + ".png";
        Path pngPath = outputDir.resolve(pngName);

        SourceInfo info = new SourceInfo(pngPath, candidate, stl);

        if (Files.exists(pngPath)) {
            pngSourceMap.put(pngPath, info);
            selectPreviewThumbnail(pngPath);
            return;
        }

        TaskProgressDialog dialog = new TaskProgressDialog(getParentFrame(),
                "Renderizando", "Extrayendo y renderizando " + stl.filename() + "...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Path tempDir = ZipExtractor.extractToTemp(candidate.path);
                try {
                    Path stlPath = tempDir.resolve(stl.filename());
                    if (!Files.exists(stlPath)) return null;
                    List<Triangle> triangles = StlParser.parse(stlPath.toFile());
                    BufferedImage img = renderer.renderizar(triangles);
                    ImageIO.write(img, "PNG", pngPath.toFile());
                    pngSourceMap.put(pngPath, info);
                    triangleCache.put(pngPath, triangles);
                } finally {
                    ZipExtractor.deleteDir(tempDir);
                }
                return null;
            }
            @Override
            protected void done() {
                dialog.closeDialog();
                if (Files.exists(pngPath)) {
                    refreshThumbnails(pngPath);
                }
            }
        }.execute();
        dialog.setVisible(true);
    }

    private void selectPreviewThumbnail(Path pngPath) {
        showPreview(pngPath);
        for (Component comp : panel.getThumbnailGrid().getComponents()) {
            if (comp instanceof JLabel label) {
                String tip = label.getToolTipText();
                if (tip != null && tip.equals(pngPath.getFileName().toString())) {
                    label.setBorder(javax.swing.BorderFactory.createLineBorder(
                            new java.awt.Color(100, 200, 255), 3));
                } else if (label.getBorder() != null) {
                    label.setBorder(null);
                }
            }
        }
    }

    private JFrame getParentFrame() {
        if (parentFrame instanceof JFrame) return (JFrame) parentFrame;
        java.awt.Window win = SwingUtilities.getWindowAncestor(parentFrame);
        return (win instanceof JFrame) ? (JFrame) win : null;
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

        panel.getListModel().clear();
        contentModel.clear();
        pngSourceMap.clear();
        triangleCache.clear();

        TaskProgressDialog dialog = new TaskProgressDialog(frame, "Escaneando", "Buscando archivos sin imagen...");
        new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() {
                Zip2PngScanner scanner = new Zip2PngScanner(limiteBytes);
                List<RenderCandidate> candidates = scanner.scanFolder(folder, c -> {
                    SwingUtilities.invokeLater(() -> panel.getListModel().addElement(c));
                });
                publish(candidates.size());
                logger.info("Escaneo completado: {} candidatos encontrados en {}", candidates.size(), folder);
                return null;
            }
            @Override
            protected void process(List<Integer> chunks) {
                int count = chunks.get(chunks.size() - 1);
                dialog.updateStatusText("Archivos sin imagen: " + count);
            }
            @Override
            protected void done() {
                dialog.closeDialog();
            }
        }.execute();
        dialog.setVisible(true);
    }

    private void onProcess(ActionEvent e) {
        if (panel.getListModel().isEmpty()) {
            JOptionPane.showMessageDialog(parentFrame,
                    "No hay candidatos. Escanea una carpeta primero.",
                    "Zip2PNG", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        limpiarOutputDir();
        panel.getThumbnailGrid().removeAll();
        panel.getThumbnailGrid().revalidate();
        panel.getThumbnailGrid().repaint();
        pngSourceMap.clear();
        triangleCache.clear();
        currentPreviewPath = null;
        currentTriangles = null;
        currentWorker = null;

        List<RenderCandidate> candidates = java.util.Collections.list(panel.getListModel().elements());
        JFrame frame = getParentFrame();
        if (frame == null) {
            logger.warn("No se pudo obtener JFrame padre para TaskProgressDialog");
            return;
        }
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
        SwingUtilities.invokeLater(() -> refreshThumbnails(null));
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
        panel.getThumbnailGrid().removeAll();
        try (var files = Files.list(outputDir)) {
            List<Path> sorted = files.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
                    .sorted()
                    .toList();
            logger.debug("Thumbnails a mostrar: {} (total={})", sorted, sorted.size());
            for (Path p : sorted) {
                try {
                    var icon = new javax.swing.ImageIcon(
                            new javax.swing.ImageIcon(p.toFile().toURI().toURL())
                                    .getImage()
                                    .getScaledInstance(180, 180, java.awt.Image.SCALE_SMOOTH));
                    var label = new javax.swing.JLabel(icon);
                    label.setToolTipText(p.getFileName().toString());
                    label.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

                    boolean highlighted = highlightPng != null
                            && p.getFileName().equals(highlightPng.getFileName());
                    if (highlighted) {
                        label.setBorder(javax.swing.BorderFactory.createLineBorder(
                                new java.awt.Color(100, 200, 255), 3));
                    }

                    label.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent ev) {
                            showPreview(p);
                        }
                    });
                    panel.getThumbnailGrid().add(label);
                } catch (Exception ex) {
                    logger.warn("No se pudo cargar thumbnail: {}", p);
                }
            }
        } catch (IOException ex) {
            logger.error("Error listando thumbnails", ex);
        }
        panel.getThumbnailGrid().revalidate();
        panel.getThumbnailGrid().repaint();
    }

    private void showPreview(Path pngPath) {
        logger.debug("[RenderController] showPreview: " + pngPath.getFileName());
        this.currentPreviewPath = pngPath;
        this.loadingTriangles = false;

        panel.getBrightnessSlider().setValue(0);
        panel.getBrightnessField().setText("0");
        panel.getContrastSlider().setValue(0);
        panel.getContrastField().setText("0");
        panel.getChkCheckerboard().setSelected(false);
        panel.getChkAntiAlias().setSelected(false);
        panel.getChkCrosshair().setSelected(true);
        
        resaltarThumbnail(pngPath);

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
        for (java.awt.Component comp : panel.getThumbnailGrid().getComponents()) {
            if (comp instanceof javax.swing.JLabel label) {
                String tip = label.getToolTipText();
                if (tip != null && tip.equals(pngPath.getFileName().toString())) {
                    label.setBorder(javax.swing.BorderFactory.createLineBorder(
                            new java.awt.Color(100, 200, 255), 3));
                } else if (label.getBorder() != null) {
                    label.setBorder(null);
                }
            }
        }
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

    private void copyToSource() {
        RenderCandidate selected = panel.getCandidateList().getSelectedValue();
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
        RenderCandidate selected = panel.getCandidateList().getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(parentFrame,
                    "Selecciona un candidato de la lista primero.",
                    "Procesar Archivo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        limpiarOutputDir();
        panel.getThumbnailGrid().removeAll();
        panel.getThumbnailGrid().revalidate();
        panel.getThumbnailGrid().repaint();
        pngSourceMap.clear();
        triangleCache.clear();
        currentPreviewPath = null;
        currentTriangles = null;
        currentWorker = null;

        JFrame frame = getParentFrame();
        if (frame == null) return;

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

        String rutaOrigen = panel.getCandidateList().getModel().getSize() > 0
                ? panel.getCandidateList().getModel().getElementAt(0).path.getParent().toString()
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
    } // --- Fin del metodo asignarPreviewAlArchivo ---


} // --- Fin de la clase RenderController ---
