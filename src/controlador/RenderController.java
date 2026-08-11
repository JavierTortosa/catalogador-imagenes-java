package controlador;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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
import javax.swing.JList;
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

import controlador.commands.AppActionCommands;
import controlador.managers.RenderSceneController;
import controlador.utils.ComponentRegistry;
import controlador.worker.Zip2PngWorker;
import controlador.worker.Zip2PngWorker.SourceInfo;
import modelo.VisorModel;
import modelo.editor.CanvasModel;
import modelo.editor.LayerModel;
import modelo.renderer.ImageEntry;
import modelo.renderer.ImageLayer;
import modelo.renderer.StlEntry;
import modelo.renderer.Triangle;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.renderer.AwtModelRenderer;
import servicios.renderer.RenderTempFileManager;
import servicios.renderer.ScannerFolderResult;
import servicios.renderer.StlParser;
import servicios.renderer.Zip2PngScanner;
import servicios.renderer.Zip2PngScanner.RenderCandidate;
import servicios.renderer.ZipExtractor;
import vista.config.UIDefinitionService;
import vista.dialogos.TaskProgressDialog;
import vista.panels.render.PreviewPanel3DFX;
import vista.panels.render.RenderPanel;
import vista.theme.ThemeManager;
import vista.util.IconUtils;

public class RenderController {

    private static final Logger logger = LoggerFactory.getLogger(RenderController.class);

    private final RenderPanel panel;
    private final ConfigurationManager config;
    private IconUtils iconUtils;
    private final Component parentFrame;
    private final DefaultListModel<StlEntry> contentModel;
    private final Map<Path, SourceInfo> pngSourceMap = new HashMap<>();
    private final Map<Path, SoftReference<List<Triangle>>> triangleCache = new HashMap<>();
    private final AwtModelRenderer renderer = new AwtModelRenderer();
    private servicios.editor.EditorDocumentManager editorDocumentManager;
    private controlador.managers.ViewManager viewManager;

    private final RenderTempFileManager tempFileManager;
    private final RenderSceneController sceneController;
    private controlador.tools.CanvasController canvasController;

    // --- Fase 2.6: documentos independientes RENDER / EDITOR ---
    // Ambos modos comparten el mismo AdvanceEditPanel, pero cada uno conserva su
    // propio par (CanvasModel, LayerModel). Al cambiar de modo se hace un swap
    // en el panel. RENDER mantiene su composición en memoria; EDITOR gestiona su
    // documento .edoc a través de EditorDocumentManager.
    private CanvasModel canvasModelRender;
    private LayerModel layerModelRender;
    private CanvasModel canvasModelEditor;
    private LayerModel layerModelEditor;
    private modelo.gizmo.TransformGizmo transformGizmo;
    private boolean editorDocumentoCargadoEnPanel;

    private Path lastScanFolder;
    private Path outputDir;
    private Path imagesDir;
    private final Set<Path> thumbnailsAprobados = new HashSet<>();
    private volatile Path currentPreviewPath;
    private List<Triangle> currentTriangles;
    private Zip2PngWorker currentWorker;
    private SwingWorker<List<Triangle>, Void> currentTriangleWorker;
    private volatile boolean loadingTriangles;
    private ComponentRegistry registry;
    private javax.swing.SwingWorker<?, ?> galleryWorker;
    private VisorModel model;

    // Punteros de selección por pestaña
    private int pointerSinRenderizar = 0;
    private int pointerConImagen = 0;
    private boolean syncingFromGrid;

    // Vista activa del modo Render (grupo excluyente: scanner, grid 3D, grid 2D)
    private static final String VISTA_SCANNER = "scanner";
    private static final String VISTA_GRID3D = "grid3d";
    private static final String VISTA_GRID2D = "grid2d";
    private String vistaActivaRender = VISTA_SCANNER;

    public RenderController(RenderPanel panel, ConfigurationManager config, Component parentFrame) {
        this.panel = panel;
        this.config = config;
        this.parentFrame = parentFrame;
        this.contentModel = panel.getContentListModel();
        this.tempFileManager = new RenderTempFileManager(config);
        this.sceneController = new RenderSceneController(panel.getPreview3DFX());
        this.outputDir = tempFileManager.getOutputDir();
        this.imagesDir = tempFileManager.getImagesDir();
        wireControls();
        wireBackgroundControls();
        initAdvanceEditIconSize();
        wireScannerPanel();
    }

    public void setModel(VisorModel model) {
        this.model = model;
    }


    /**
     * Ruta a localizar en el explorador según la selección actual del modo
     * Render: el candidato seleccionado en las pestañas, o si no el preview
     * que se está mostrando.
     *
     * @return ruta del candidato/preview actual, o null si no hay nada
     */
    public Path getRutaActualParaLocalizar() {
        RenderCandidate candidate = getSelectedCandidate();
        if (candidate != null) {
            return candidate.path;
        }
        return currentPreviewPath;
    } // --- Fin del metodo getRutaActualParaLocalizar ---

    private void wireScannerPanel() {
        var sp = panel.getScannerPanel();
        sp.setOnEscanear(this::ejecutarEscanearHuerfanos);
        sp.setOnProcesarSeleccion(this::abrirSeleccionScanner);
        sp.addSubcarpetasChangeListener(e -> {
            config.setString(ConfigKeys.RENDER_SCAN_INCLUDE_SUBFOLDERS,
                    String.valueOf(sp.isIncludeSubfolders()));
            try {
                config.guardarConfiguracion(config.getConfig());
            } catch (IOException ex) {
                logger.warn("No se pudo guardar la configuración del scanner", ex);
            }
        });
        sp.addTableSelectionListener(e -> sp.refrescarResumen());
    }

    public void setRegistry(ComponentRegistry registry) {
        this.registry = registry;
    }

    public void setIconUtils(IconUtils iconUtils) {
        this.iconUtils = iconUtils;
        panel.getAdvanceEditPanel().setIconUtils(iconUtils);
    }


    public void setUiDefinitionService(UIDefinitionService service) {
        panel.getAdvanceEditPanel().setUiDefinitionService(service);
    }


    public void setThemeManager(ThemeManager themeManager) {
        panel.getAdvanceEditPanel().setThemeManager(themeManager);
    }

    private void wireControls() {
        // Listener para ambas listas de candidatos
        javax.swing.event.ListSelectionListener candidateListener = e -> {
            if (e.getValueIsAdjusting()) return;
            onCandidateSelected();
        };
        panel.getCandidateListSinImagen().addListSelectionListener(candidateListener);
        panel.getCandidateListConImagen().addListSelectionListener(candidateListener);

        // Clic en la zona del checkbox de una fila -> alterna el marcado para procesar
        java.awt.event.MouseAdapter checkboxToggle = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() != 1) return;
                JList<?> list = (JList<?>) e.getSource();
                int index = list.locationToIndex(e.getPoint());
                if (index < 0 || index >= list.getModel().getSize()) return;
                if (e.getX() <= 24) {
                    Object value = list.getModel().getElementAt(index);
                    if (value instanceof RenderCandidate) {
                        panel.toggleMarcadoParaProcesar((RenderCandidate) value);
                        list.repaint();
                    }
                }
            }
        };
        panel.getCandidateListSinImagen().addMouseListener(checkboxToggle);
        panel.getCandidateListConImagen().addMouseListener(checkboxToggle);

        // Cambio de pestaña de candidatos: actualiza contenido inferior y card
        panel.getCandidateTabs().addChangeListener(e -> {
            onCandidateTabChanged();
            refreshGallery();
        });

        // Refrescar galería al cambiar selección (si está visible)
        panel.getCandidateListSinImagen().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) refreshGallery();
        });
        panel.getCandidateListConImagen().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) refreshGallery();
        });

        // Selección en filmstrip → cargar en visor 2D + sincronizar lista de contenido
        panel.addPropertyChangeListener("filmstripSelected", evt -> {
            ImageLayer sel = (ImageLayer) evt.getNewValue();
            if (sel == null || sel.getImage() == null) return;
            panel.set2DImage(sel.getImage());
            panel.show2DView();
            sincronizarFilmstripConListaContenido(sel);
        });

        // Sincronizar lista de contenido → filmstrip
        javax.swing.event.ListSelectionListener syncContentToFilmstrip = e -> {
            if (e.getValueIsAdjusting() || !panel.isFilmstripVisible()) return;
            String nombre = null;
            if (panel.isCandidateTabConImagen()) {
                ImageEntry entry = panel.getContentImageList().getSelectedValue();
                if (entry != null) nombre = entry.filename();
            }
            if (nombre == null) return;
            var model = panel.getFilmstripListModel();
            for (int i = 0; i < model.size(); i++) {
                if (model.getElementAt(i).getName().equals(nombre)
                        || model.getElementAt(i).getName().endsWith(nombre)) {
                    panel.getFilmstripList().setSelectedIndex(i);
                    panel.getFilmstripList().ensureIndexIsVisible(i);
                    break;
                }
            }
        };
        panel.getContentImageList().addListSelectionListener(syncContentToFilmstrip);

        // Sincronizar lista de STLs → filmstrip
        panel.getContentList().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || !panel.isFilmstripVisible() || panel.isCandidateTabConImagen()) return;
            StlEntry entry = panel.getContentList().getSelectedValue();
            if (entry == null) return;
            String stlName = entry.filename();
            var model = panel.getFilmstripListModel();
            for (int i = 0; i < model.size(); i++) {
                if (model.getElementAt(i).getName().equals(stlName)) {
                    panel.getFilmstripList().setSelectedIndex(i);
                    panel.getFilmstripList().ensureIndexIsVisible(i);
                    break;
                }
            }
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

        panel.getChkWireframe().addActionListener(e -> applyAdjustments());
        panel.getChkFillLight2().addActionListener(e -> applyAdjustments());
        panel.getCboCalidad().addActionListener(e -> applyAdjustments());

        wireMoverRotarControls();
    }

    /**
     * Cablea los botones Mover/Rotar con sus popups de spinners.
     * Los spinners representan estado absoluto de la vista activa (3D o 2D).
     */
    private void wireMoverRotarControls() {
        wirePopupMoverRotar(panel.getBtnMover(), panel.getPopupMover());
        wirePopupMoverRotar(panel.getBtnRotar(), panel.getPopupRotar());

        javax.swing.event.ChangeListener spinnerListener = ev -> {
            if (!(ev.getSource() instanceof javax.swing.JSpinner)) return;
            javax.swing.JSpinner spinner = (javax.swing.JSpinner) ev.getSource();
            if (!spinner.isShowing()) return;
            if (panel.isShowing2DView()) {
                aplicarSpinnerMoverRotar2D();
            } else {
                aplicarSpinnerMoverRotar3D();
            }
        };
        for (javax.swing.JSpinner sp : new javax.swing.JSpinner[]{
                panel.getSpnMoverX(), panel.getSpnMoverY(), panel.getSpnMoverZ(),
                panel.getSpnRotarX(), panel.getSpnRotarY(), panel.getSpnRotarZ()}) {
            sp.addChangeListener(spinnerListener);
            configurarRuedaSpinner(sp);
        }
    }

    /**
     * Muestra el popup bajo el botón al seleccionarlo, sincronizando los
     * spinners con el estado real de la vista; al cerrarse deselecciona el botón.
     */
    private void wirePopupMoverRotar(javax.swing.JToggleButton btn, javax.swing.JPopupMenu popup) {
        btn.addItemListener(e -> {
            if (btn.isSelected()) {
                panel.syncSpinnersDesdeVista();
                popup.show(btn, 0, btn.getHeight());
            } else {
                popup.setVisible(false);
            }
        });
        popup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}
            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                btn.setSelected(false);
            }
            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                btn.setSelected(false);
            }
        });
    }

    /**
     * Aplica los valores absolutos de los spinners a la escena 3D.
     */
    private void aplicarSpinnerMoverRotar3D() {
        double rotX = ((Number) panel.getSpnRotarX().getValue()).doubleValue();
        double rotY = ((Number) panel.getSpnRotarY().getValue()).doubleValue();
        double rotZ = ((Number) panel.getSpnRotarZ().getValue()).doubleValue();
        double panX = ((Number) panel.getSpnMoverX().getValue()).doubleValue();
        double panY = ((Number) panel.getSpnMoverY().getValue()).doubleValue();
        double zoomFactor = ((Number) panel.getSpnMoverZ().getValue()).doubleValue() / 100.0;
        sceneController.aplicarRotacion(rotX, rotY, rotZ);
        sceneController.aplicarMovimiento(panX, panY, zoomFactor);
    }

    /**
     * Aplica los valores absolutos de los spinners a la imagen 2D del visor.
     */
    private void aplicarSpinnerMoverRotar2D() {
        double panX = ((Number) panel.getSpnMoverX().getValue()).doubleValue();
        double panY = ((Number) panel.getSpnMoverY().getValue()).doubleValue();
        double zoom = ((Number) panel.getSpnMoverZ().getValue()).doubleValue() / 100.0;
        double rotZ = ((Number) panel.getSpnRotarZ().getValue()).doubleValue();
        panel.setImageOffset2D(panX, panY);
        panel.setImageZoom2D(zoom);
        panel.setImageRotation(rotZ);
    }

    /**
     * Configura la rueda del ratón sobre un spinner al estilo Chitubox:
     * paso fino normal, paso 5 con Shift.
     */
    private void configurarRuedaSpinner(javax.swing.JSpinner spinner) {
        java.awt.event.MouseWheelListener rueda = e -> {
            javax.swing.SpinnerNumberModel model =
                    (javax.swing.SpinnerNumberModel) spinner.getModel();
            Object current = spinner.getValue();
            boolean entero = current instanceof Integer || current instanceof Long;
            double paso = e.isShiftDown() ? 5.0 : (entero ? 1.0 : 0.5);
            double actual = ((Number) current).doubleValue();
            double nuevo = e.getWheelRotation() < 0 ? actual + paso : actual - paso;
            if (entero) nuevo = Math.floor(nuevo);
            spinner.setValue(clampSpinnerValor(model, nuevo));
        };
        spinner.addMouseWheelListener(rueda);
        if (spinner.getEditor() instanceof javax.swing.JSpinner.DefaultEditor de) {
            de.getTextField().addMouseWheelListener(rueda);
        }
    }

    /**
     * Ajusta un valor a los límites del modelo del spinner.
     */
    private static Object clampSpinnerValor(javax.swing.SpinnerNumberModel model, double valor) {
        Comparable<?> min = model.getMinimum();
        Comparable<?> max = model.getMaximum();
        double lo = min instanceof Number n ? n.doubleValue() : Double.NEGATIVE_INFINITY;
        double hi = max instanceof Number n ? n.doubleValue() : Double.POSITIVE_INFINITY;
        double clamped = Math.max(lo, Math.min(hi, valor));
        Object cur = model.getValue();
        if (cur instanceof Number) {
            Class<?> cls = cur.getClass();
            if (cls == Integer.class) return (int) Math.round(clamped);
            if (cls == Long.class) return (long) Math.round(clamped);
            if (cls == Float.class) return (float) clamped;
            return clamped;
        }
        return clamped;
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

    private void initAdvanceEditIconSize() {
        int w = config.getInt(ConfigKeys.ICONOS_ANCHO, 24);
        int h = config.getInt(ConfigKeys.ICONOS_ALTO, 24);
        panel.getAdvanceEditPanel().setIconSize(w, h);
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
        panel.getPreview3DFX().setWireframe(panel.getChkWireframe().isSelected());
        panel.getPreview3DFX().setFillLight2Visible(panel.getChkFillLight2().isSelected());
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
        }
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
            private List<Triangle> loadedTriangles;
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
                loadedTriangles = StlParser.parse(stlPath.toFile());
                return null;
            }
            @Override
            protected void done() {
                if (tempDir != null) ZipExtractor.deleteDir(tempDir);
                if (loadedTriangles != null && !loadedTriangles.isEmpty()) {
                    // El modelo pasa a ser el preview 3D actual: queda disponible
                    // para "Guardar preview" aunque no venga de un render procesado.
                    currentTriangles = loadedTriangles;
                    panel.getPreview3DFX().setMesh(loadedTriangles);
                    resetPreviewAdjustments();
                }
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
        // La cruceta respeta el estado actual del control, no se fuerza.
        applyAdjustments();
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

        vaciarCandidatos();

        TaskProgressDialog dialog = new TaskProgressDialog(frame, "Escaneando", "Buscando archivos sin imagen...");
        new SwingWorker<List<RenderCandidate>, String>() {
            @Override
            protected List<RenderCandidate> doInBackground() {
                publish("Contando archivos...");
                long totalEntradas = contarEntradas(folder, true);
                Zip2PngScanner scanner = new Zip2PngScanner(limiteBytes);
                List<RenderCandidate> candidates;
                int[] candidatos = {0};
                long[] entradas = {0};
                int[] ultimoPct = {0};

                candidates = scanner.scanFolder(folder, true, null, null, p -> {
                    entradas[0]++;
                    if (totalEntradas > 0) {
                        int pct = (int) Math.min(100, (entradas[0] * 100L) / totalEntradas);
                        if (pct != ultimoPct[0]) {
                            ultimoPct[0] = pct;
                            dialog.updateProgress(pct, 100, null);
                            publish("Carpetas | Candidatos: " + candidatos[0]);
                        }
                    }
                });
                for (RenderCandidate c : candidates) candidatos[0]++;

                publish("Comprobando imágenes dentro de los comprimidos...");
                ultimoPct[0] = 0;
                Zip2PngScanner.detectarImagenesEnArchivos(candidates, (procesados, total) -> {
                    int pct = total > 0 ? (int) Math.min(100, (procesados * 100L) / total) : 100;
                    if (pct != ultimoPct[0]) {
                        ultimoPct[0] = pct;
                        dialog.updateProgress(pct, 100, null);
                        publish("Comprobando imágenes... " + procesados + " de " + total);
                    }
                });

                logger.info("Escaneo completado: {} candidatos encontrados en {}", candidates.size(), folder);
                return candidates;
            }
            @Override
            protected void process(List<String> chunks) {
                dialog.updateStatusText(chunks.get(chunks.size() - 1));
            }
            @Override
            protected void done() {
                try {
                    List<RenderCandidate> candidates = get();
                    poblarPestanyasConCandidates(candidates);
                    int sinImg = panel.getListModelSinImagen().getSize();
                    int conImg = panel.getListModelConImagen().getSize();
                    dialog.updateStatusText("Sin renderizar: " + sinImg + " | Con imagen: " + conImg);
                    mostrarGrid3D();
                } catch (Exception ex) {
                    logger.error("Error al finalizar escaneo", ex);
                }
                dialog.closeDialog();
            }
        }.execute();
        dialog.setVisible(true);
    }

    /**
     * Reparte los candidatos en las pestañas "Sin renderizar" y "Con imagen" y
     * actualiza sus títulos. Es el paso común entre {@code onScan} y el Scanner.
     *
     * @param candidates candidatos a distribuir
     */
    private void poblarPestanyasConCandidates(List<RenderCandidate> candidates) {
        panel.getListModelSinImagen().clear();
        panel.getListModelConImagen().clear();
        for (RenderCandidate c : candidates) {
            if (c.tieneImagenesDentro()) {
                panel.getListModelConImagen().addElement(c);
            } else {
                panel.getListModelSinImagen().addElement(c);
            }
        }
        panel.actualizarTitulosPestanyas();
    } // --- Fin del metodo poblarPestanyasConCandidates ---


    /**
     * Vacía el estado temporal del pipeline de candidatos: listas de las
     * pestañas, marcados, contenido inferior, cachés y aprobados. Se invoca al
     * iniciar cualquier escaneo para que las pestañas reflejen solo el disco.
     */
    private void vaciarCandidatos() {
        panel.getListModelSinImagen().clear();
        panel.getListModelConImagen().clear();
        panel.limpiarMarcados();
        panel.actualizarTitulosPestanyas();
        contentModel.clear();
        panel.getContentImageListModel().clear();
        panel.getImagenesGrid().removeAll();
        panel.getImagenesGrid().revalidate();
        panel.getImagenesGrid().repaint();
        panel.getRendersGrid().removeAll();
        panel.getRendersGrid().revalidate();
        panel.getRendersGrid().repaint();
        pngSourceMap.clear();
        triangleCache.clear();
        thumbnailsAprobados.clear();
        clearPreview();
        panel.syncGridToCandidateTab();
    } // --- Fin del metodo vaciarCandidatos ---


    /**
     * Muestra el Scanner de Huérfanos como una vista más del grupo excluyente
     * (scanner / grid 3D / grid 2D). Al seleccionarlo se deseleccionan los otros
     * dos toggles; nunca se "apaga" dejando sin vista activa.
     */
    public void toggleScannerView() {
        prepararScannerParaMostrar();
        panel.setScannerActive(true);
        seleccionarToggleScanner();
        deseleccionarToggle("button.render.grid3d");
        deseleccionarToggle("button.render.grid2d");
        vistaActivaRender = VISTA_SCANNER;
    } // --- Fin del metodo toggleScannerView ---


    /**
     * Activa el Scanner de Huérfanos como vista actual del modo Render.
     * Pre-rellena la carpeta raíz del visor, selecciona su toggle y deselecciona
     * los toggles de Grid3D/Grid2D para que la vista quede coherente con la
     * exclusividad del grupo.
     */
    public void mostrarScannerPorDefecto() {
        prepararScannerParaMostrar();
        panel.setScannerActive(true);
        seleccionarToggleScanner();
        deseleccionarToggle("button.render.grid3d");
        deseleccionarToggle("button.render.grid2d");
        vistaActivaRender = VISTA_SCANNER;
    } // --- Fin del metodo mostrarScannerPorDefecto ---


    /**
     * Restaura la última vista activa del modo Render al entrar de nuevo en el
     * modo. La primera vez (o si aún no se ha trabajado) se muestra el Scanner.
     */
    public void restaurarVistaRender() {
        // Al entrar en el modo Render el collage siempre arranca desactivado:
        // la pestaña "Capas" solo está disponible dentro de COLLAGE.
        if (panel.isCollageMode()) {
            panel.setCollageMode(false);
        }
        switch (vistaActivaRender) {
            case VISTA_GRID3D:
                mostrarGrid3D();
                break;
            case VISTA_GRID2D:
                mostrarGrid2D();
                break;
            default:
                mostrarScannerPorDefecto();
                break;
        }
    } // --- Fin del metodo restaurarVistaRender ---


    private void prepararScannerParaMostrar() {
        var sp = panel.getScannerPanel();
        if (sp.getCarpetaInicial() == null) {
            Path inicial = (model != null) ? model.getCarpetaRaizActual() : null;
            if (inicial == null) inicial = lastScanFolder;
            sp.setCarpetaInicial(inicial);
        }
        sp.setIncludeSubfolders(config.getBoolean(ConfigKeys.RENDER_SCAN_INCLUDE_SUBFOLDERS, true));
    } // --- Fin del metodo prepararScannerParaMostrar ---


    /**
     * Ejecuta el escaneo de huérfanos sobre la carpeta del panel del Scanner.
     * Muestra un TaskProgressDialog modal con contador de carpetas y candidatos
     * en vivo; al terminar rellena la tabla agrupando por carpeta.
     */
    public void ejecutarEscanearHuerfanos() {
        var sp = panel.getScannerPanel();
        Path root = sp.getCarpetaInicial();
        if (root == null) {
            Path inicial = (model != null) ? model.getCarpetaRaizActual() : null;
            if (inicial == null) inicial = lastScanFolder;
            if (inicial != null) sp.setCarpetaInicial(inicial);
            root = sp.getCarpetaInicial();
        }
        if (root == null || !Files.isDirectory(root)) {
            JOptionPane.showMessageDialog(parentFrame,
                    "Indica una carpeta válida para escanear.",
                    "Scanner de huérfanos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        final Path scanRoot = root;
        final boolean incluir = sp.isIncludeSubfolders();

        JFrame frame = getParentFrame();
        if (frame == null) {
            logger.warn("No se pudo obtener JFrame padre para TaskProgressDialog");
            return;
        }
        long limiteMb = config.getInt(ConfigKeys.ZIP2PNG_LIMITE_MB, 512);
        long limiteBytes = limiteMb * 1024L * 1024L;

        sp.setResultados(List.of());
        vaciarCandidatos();
        TaskProgressDialog dialog = new TaskProgressDialog(frame, "Escaneando huérfanos",
                "Buscando archivos sin representación...");
        SwingWorker<List<RenderCandidate>, String> worker = new SwingWorker<>() {
            @Override
            protected List<RenderCandidate> doInBackground() {
                publish("Contando archivos...");
                long totalEntradas = contarEntradas(scanRoot, incluir);
                Zip2PngScanner scanner = new Zip2PngScanner(limiteBytes);
                int[] carpetas = {0};
                int[] candidatos = {0};
                long[] entradas = {0};
                int[] ultimoPct = {0};
                List<RenderCandidate> found = scanner.scanFolder(scanRoot, incluir,
                        c -> {
                            candidatos[0]++;
                            publish("Casos encontrados: " + candidatos[0]);
                        },
                        p -> {
                            carpetas[0]++;
                            publish("Carpetas: " + carpetas[0] + " | Candidatos: " + candidatos[0]);
                        },
                        p -> {
                            entradas[0]++;
                            if (totalEntradas > 0) {
                                int pct = (int) Math.min(100, (entradas[0] * 100L) / totalEntradas);
                                if (pct != ultimoPct[0]) {
                                    ultimoPct[0] = pct;
                                    dialog.updateProgress(pct, 100, null);
                                }
                            }
                        });
                publish("Comprobando imágenes dentro de los comprimidos...");
                ultimoPct[0] = 0;
                Zip2PngScanner.detectarImagenesEnArchivos(found, (procesados, total) -> {
                    int pct = total > 0 ? (int) Math.min(100, (procesados * 100L) / total) : 100;
                    if (pct != ultimoPct[0]) {
                        ultimoPct[0] = pct;
                        dialog.updateProgress(pct, 100, null);
                        publish("Comprobando imágenes... " + procesados + " de " + total);
                    }
                });
                return found;
            }

            @Override
            protected void process(List<String> chunks) {
                dialog.updateStatusText(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    dialog.closeDialog();
                    return;
                }
                try {
                    List<RenderCandidate> found = get();
                    Map<Path, List<RenderCandidate>> porCarpeta = new HashMap<>();
                    for (RenderCandidate c : found) {
                        Path parent = c.path.getParent();
                        if (parent == null) parent = scanRoot;
                        porCarpeta.computeIfAbsent(parent, k -> new ArrayList<>()).add(c);
                    }
                    List<ScannerFolderResult> resultados = new ArrayList<>();
                    for (Map.Entry<Path, List<RenderCandidate>> e : porCarpeta.entrySet()) {
                        ScannerFolderResult r = new ScannerFolderResult(e.getKey(), List.copyOf(e.getValue()));
                        if (r.pendientes() >= 1) {
                            resultados.add(r);
                        }
                    }
                    sp.setResultados(resultados);
                    long totalPendientes = resultados.stream().mapToLong(ScannerFolderResult::pendientes).sum();
                    long totalPreview = resultados.stream().mapToLong(ScannerFolderResult::conPreview).sum();
                    dialog.updateStatusText("Carpetas: " + resultados.size()
                            + " | Pendientes: " + totalPendientes
                            + " | Con preview: " + totalPreview);
                } catch (Exception ex) {
                    logger.error("Error al finalizar escaneo de huérfanos", ex);
                }
                dialog.closeDialog();
            }
        };
        dialog.setWorkerAsociado(worker);
        worker.execute();
        dialog.setVisible(true);
    } // --- Fin del metodo ejecutarEscanearHuerfanos ---


    /**
     * Añade los candidatos de las carpetas marcadas en la tabla del Scanner a las
     * pestañas del pipeline de render. Trabaja SOLO sobre el snapshot en memoria;
     * nunca vuelve a escanear el disco. No procesa ni rellena el grid: el usuario
     * revisa los candidatos con doble clic.
     */
    public void abrirSeleccionScanner() {
        var sp = panel.getScannerPanel();
        List<ScannerFolderResult> seleccion = sp.getSelectedResults();
        if (seleccion.isEmpty()) {
            JOptionPane.showMessageDialog(parentFrame,
                    "Marca al menos una carpeta en la tabla para procesar.",
                    "Abrir selección", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<RenderCandidate> candidatos = new ArrayList<>();
        for (ScannerFolderResult r : seleccion) {
            candidatos.addAll(r.candidates());
        }
        poblarPestanyasConCandidates(candidatos);
        panel.limpiarMarcados();

        mostrarGrid3D();
    } // --- Fin del metodo abrirSeleccionScanner ---


    private void seleccionarToggleScanner() {
        if (registry == null) return;
        Object btn = registry.get("button.render.scanner");
        if (btn instanceof javax.swing.AbstractButton b) {
            b.setSelected(true);
        }
    } // --- Fin del metodo seleccionarToggleScanner ---


    private void deseleccionarToggle(String key) {
        if (registry == null) return;
        Object btn = registry.get(key);
        if (btn instanceof javax.swing.AbstractButton b) {
            b.setSelected(false);
        }
    } // --- Fin del metodo deseleccionarToggle ---


    private void seleccionarToggle(String key) {
        if (registry == null) return;
        Object btn = registry.get(key);
        if (btn instanceof javax.swing.AbstractButton b) {
            b.setSelected(true);
        }
    } // --- Fin del metodo seleccionarToggle ---

    private List<RenderCandidate> getAllCandidates() {
        List<RenderCandidate> all = new ArrayList<>();
        java.util.Collections.list(panel.getListModelSinImagen().elements()).forEach(all::add);
        java.util.Collections.list(panel.getListModelConImagen().elements()).forEach(all::add);
        return all;
    }

    private void onProcess(ActionEvent e) {
        List<RenderCandidate> candidatos = new ArrayList<>(panel.getMarcadosParaProcesar());
        if (candidatos.isEmpty()) {
            candidatos = getAllCandidates();
            if (candidatos.isEmpty()) {
                JOptionPane.showMessageDialog(parentFrame,
                        "No hay candidatos. Escanea una carpeta primero.",
                        "Zip2PNG", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (candidatos.size() > 10) {
                int opcion = JOptionPane.showConfirmDialog(parentFrame,
                        "No hay archivos marcados.\n\u00BFProcesar los " + candidatos.size()
                                + " archivos de la lista?",
                        "Procesar lote", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (opcion != JOptionPane.OK_OPTION) return;
            }
        }
        if (candidatos.isEmpty()) {
            JOptionPane.showMessageDialog(parentFrame,
                    "No hay candidatos. Escanea una carpeta primero.",
                    "Zip2PNG", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        lanzarProcesamiento(candidatos);
    }

    private void lanzarProcesamiento(List<RenderCandidate> candidatos) {
        limpiarOutputDeCandidatos(candidatos);
        panel.getImagenesGrid().removeAll();
        panel.getRendersGrid().removeAll();
        panel.getRendersGrid().revalidate();
        panel.getRendersGrid().repaint();
        panel.getImagenesGrid().revalidate();
        panel.getImagenesGrid().repaint();
        this.currentWorker = null;

        JFrame frame = getParentFrame();
        if (frame == null) {
            logger.warn("No se pudo obtener JFrame padre para TaskProgressDialog");
            return;
        }
        panel.actualizarTitulosPestanyas();
        TaskProgressDialog dialog = new TaskProgressDialog(frame, "Zip2PNG", "Procesando...");

        Zip2PngWorker worker = new Zip2PngWorker(candidatos, outputDir, dialog, () -> {
            panel.limpiarMarcados();
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
            reordenarProcesadosAlInicio();
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
        if (tempFileManager != null) {
            tempFileManager.limpiarOutputDir();
        }
    }

    /**
     * Elimina únicamente la salida temporal de los candidatos indicados
     * (PNG asociado y su carpeta de imágenes extraídas), sin tocar el resto.
     *
     * @param candidatos candidatos cuya salida previa se limpia
     */
    private void limpiarOutputDeCandidatos(List<RenderCandidate> candidatos) {
        for (RenderCandidate c : candidatos) {
            try {
                Path png = outputDir.resolve(c.nombreBase + ".png");
                Files.deleteIfExists(png);
                pngSourceMap.remove(png);
                triangleCache.remove(png);
                Path imgDir = imagesDir.resolve(c.nombreBase);
                if (Files.isDirectory(imgDir)) {
                    RenderTempFileManager.deleteDir(imgDir);
                }
            } catch (Exception ex) {
                logger.warn("No se pudo limpiar la salida previa de {}", c.nombreBase, ex);
            }
        }
    }

    /**
     * Mueve al principio de sus pestañas los candidatos que ya tienen PNG
     * generado en la salida temporal, conservando el orden relativo del resto.
     * Mantiene una cola de trabajo/revisión: los procesados quedan arriba pero
     * siguen seleccionables para decidir si la representación es buena.
     */
    private void reordenarProcesadosAlInicio() {
        DefaultListModel<RenderCandidate> sinModel = panel.getListModelSinImagen();
        DefaultListModel<RenderCandidate> conModel = panel.getListModelConImagen();
        reordenarModelo(sinModel);
        reordenarModelo(conModel);
    }

    private void reordenarModelo(DefaultListModel<RenderCandidate> model) {
        List<RenderCandidate> procesados = new ArrayList<>();
        List<RenderCandidate> resto = new ArrayList<>();
        java.util.Enumeration<RenderCandidate> en = model.elements();
        while (en.hasMoreElements()) {
            RenderCandidate c = en.nextElement();
            Path png = outputDir.resolve(c.nombreBase + ".png");
            if (Files.exists(png)) {
                procesados.add(c);
            } else {
                resto.add(c);
            }
        }
        if (procesados.isEmpty()) return;
        model.clear();
        procesados.forEach(model::addElement);
        resto.forEach(model::addElement);
        panel.actualizarTitulosPestanyas();
    }

    private void refreshThumbnails(Path highlightPng) {
        panel.getRendersGrid().removeAll();
        panel.getImagenesGrid().removeAll();

        // Renders 3D: un thumbnail por candidato sin imagen
        java.util.Enumeration<RenderCandidate> sinRender = panel.getListModelSinImagen().elements();
        while (sinRender.hasMoreElements()) {
            RenderCandidate c = sinRender.nextElement();
            try {
                Path png = outputDir.resolve(c.nombreBase + ".png");
                if (Files.exists(png)) {
                    addThumbnailToGrid(png, panel.getRendersGrid(), highlightPng, c.nombreBase, c);
                }
            } catch (InvalidPathException ex) {
                logger.warn("Nombre base inválido para render 3D: '{}'", c.nombreBase);
            }
        }

        // Imágenes: un thumbnail por candidato con imagen (primera imagen encontrada)
        java.util.Enumeration<RenderCandidate> conImg = panel.getListModelConImagen().elements();
        while (conImg.hasMoreElements()) {
            RenderCandidate c = conImg.nextElement();
            try {
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
            } catch (InvalidPathException ex) {
                logger.warn("Nombre base inválido para imagen: '{}'", c.nombreBase);
            }
        }

        panel.getRendersGrid().revalidate();
        panel.getRendersGrid().repaint();
        panel.getImagenesGrid().revalidate();
        panel.getImagenesGrid().repaint();
    }

    private void addThumbnailToGrid(Path p, JPanel grid, Path highlightPng, String labelText, RenderCandidate candidate) {
        try {
            BufferedImage src = ImageIO.read(p.toFile());
            if (src == null) {
                logger.warn("No se pudo leer la imagen: {}", p);
                return;
            }
            Image base = src.getScaledInstance(180, 180, java.awt.Image.SCALE_SMOOTH);

            boolean aprobado = thumbnailsAprobados.contains(p);

            javax.swing.Icon icon;
            if (aprobado) {
                icon = crearIconoConCheck(base);
            } else {
                icon = new javax.swing.ImageIcon(base);
            }

            var label = new javax.swing.JLabel(icon);
            String tooltip = labelText + " - " + p.getFileName().toString();
            label.setToolTipText(tooltip);
            label.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

            if (aprobado) {
                label.setBorder(javax.swing.BorderFactory.createLineBorder(new Color(0, 160, 0), 3));
            } else if (highlightPng != null && p.getFileName().equals(highlightPng.getFileName())) {
                label.setBorder(javax.swing.BorderFactory.createLineBorder(new Color(100, 200, 255), 3));
            }

            label.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent ev) {
                    if (ev.getClickCount() == 2) {
                        toggleAprobado(p);
                        return;
                    }
                    seleccionarCandidatoEnLista(candidate);
                    showPreview(p);
                }
            });
            grid.add(label);
        } catch (Exception ex) {
            logger.warn("No se pudo cargar thumbnail: {}", p);
        }
    }

    private javax.swing.Icon crearIconoConCheck(Image base) {
        BufferedImage img = new BufferedImage(180, 180, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(base, 0, 0, null);
        g.setColor(new Color(0, 160, 0));
        g.setFont(g.getFont().deriveFont(java.awt.Font.BOLD, 22f));
        g.drawString("\u2714", 153, 24);
        g.dispose();
        return new javax.swing.ImageIcon(img);
    }


    private void toggleAprobado(Path p) {
        if (thumbnailsAprobados.contains(p)) {
            thumbnailsAprobados.remove(p);
        } else {
            thumbnailsAprobados.add(p);
        }
        refreshThumbnails(null);
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

    private List<Triangle> getCachedTriangles(Path path) {
        if (path == null) return null;
        SoftReference<List<Triangle>> ref = triangleCache.get(path);
        return ref != null ? ref.get() : null;
    }

    private void putCachedTriangles(Path path, List<Triangle> tris) {
        if (path != null && tris != null) {
            triangleCache.put(path, new SoftReference<>(tris));
        }
    }

    private void showRenderPreview(Path pngPath) {
        panel.show3DView();
        currentTriangles = getCachedTriangles(pngPath);
        logger.debug("[RenderController] triangleCache hit: " + (currentTriangles != null));
        if (currentTriangles != null) {
            sceneController.cargarMalla3D(currentTriangles, 0, 0, false, panel.isAntiAlias(), panel.isCrosshair());
        } else {
            sceneController.limpiarEscena();
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
            sceneController.limpiarEscena();
        });
    }

    private void cargarTriangulosAsync() {
        if (currentPreviewPath == null) { logger.warn("[RenderController] cargarTriangulosAsync: currentPreviewPath is null"); return; }
        
        // Cancelar trabajador anterior si está en ejecución para evitar race conditions
        if (currentTriangleWorker != null && !currentTriangleWorker.isDone()) {
            currentTriangleWorker.cancel(true);
        }

        final Path pathSiendoCargado = currentPreviewPath;
        logger.debug("[RenderController] cargarTriangulosAsync iniciando para: " + pathSiendoCargado.getFileName());
        logger.debug("[RenderController] pngSourceMap.size() = " + pngSourceMap.size());

        currentTriangleWorker = new SwingWorker<List<Triangle>, Void>() {
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
                        RenderTempFileManager.deleteDir(tempDir);
                    }
                } else {
                    logger.debug("[RenderController] Parseando STL (no comprimido)...");
                    return StlParser.parse(info.candidate().path.toFile());
                }
            }
            @Override
            protected void done() {
                if (isCancelled()) {
                    logger.debug("[RenderController] Worker cancelado. Omitiendo render.");
                    return;
                }
                logger.debug("[RenderController] Worker done()");
                try {
                    List<Triangle> tris = get();
                    logger.debug("[RenderController] tris = " + (tris != null ? tris.size() + " triángulos" : "null"));
                    if (tris != null && !tris.isEmpty()) {
                        putCachedTriangles(pathSiendoCargado, tris);
                        if (pathSiendoCargado.equals(currentPreviewPath)) {
                            currentTriangles = tris;
                            logger.debug("[RenderController] Llamando cargarMalla3D...");
                            sceneController.cargarMalla3D(tris, panel.getBrightness(), panel.getContrast(),
                                    panel.isCheckerboard(), panel.isAntiAlias(), panel.isCrosshair());
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
                    if (!isCancelled()) {
                        logger.warn("[RenderController] Excepción en done(): " + e.getMessage());
                        logger.warn("No se pudieron cargar triángulos para preview", e);
                        mostrarErrorCarga("Error al cargar STL:\n" + e.getMessage());
                    }
                }
            }
        };
        currentTriangleWorker.execute();
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

        limpiarOutputDeCandidatos(List.of(selected));
        panel.getRendersGrid().removeAll();
        panel.getRendersGrid().revalidate();
        panel.getRendersGrid().repaint();
        panel.getImagenesGrid().removeAll();
        panel.getImagenesGrid().revalidate();
        panel.getImagenesGrid().repaint();
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
     * Consulta si hay renders aprobados pendientes de copiar y, de ser así,
     * muestra un diálogo con opciones para que el usuario decida qué hacer.
     * @return true si se debe continuar con el cierre, false para cancelarlo.
     */
    public boolean handleCloseWithPendingApprovedRenders() {
        if (thumbnailsAprobados.isEmpty()) {
            return true;
        }

        String msg = "Hay " + thumbnailsAprobados.size() + " render(s) aprobado(s) en la carpeta temporal:\n"
                + outputDir.toAbsolutePath() + "\n\n¿Qué deseas hacer?";
        String[] options = { "Salir", "Abrir carpeta temporal y salir", "Cancelar", "Copiar PNGs a..." };
        int choice = JOptionPane.showOptionDialog(parentFrame,
                msg, "Render sin copiar",
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[2]);

        switch (choice) {
            case 0: // Salir
                tempFileManager.limpiarOutputDir();
                return true;
            case 1: // Abrir carpeta temporal y salir
                openTempFolder();
                return true;
            case 3: { // Copiar PNGs a...
                Path dir = lastScanFolder != null ? lastScanFolder : Path.of(System.getProperty("user.home"));
                JFileChooser chooser = new JFileChooser(dir.toFile());
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setDialogTitle("Seleccionar carpeta de destino");
                if (chooser.showOpenDialog(parentFrame) != JFileChooser.APPROVE_OPTION) {
                    return false;
                }
                Path dest = chooser.getSelectedFile().toPath();
                int copied = 0;
                for (Path png : thumbnailsAprobados) {
                    try {
                        Files.copy(png, dest.resolve(png.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                        copied++;
                    } catch (IOException ex) {
                        logger.error("Error copiando {} a {}", png, dest, ex);
                    }
                }
                tempFileManager.limpiarOutputDir();
                JOptionPane.showMessageDialog(parentFrame,
                        "Copiados " + copied + " archivo(s) a:\n" + dest,
                        "Copiar renders", JOptionPane.INFORMATION_MESSAGE);
                return true;
            }
            default: // Cancelar o cerrar diálogo
                return false;
        }
    } // --- Fin del metodo handleCloseWithPendingApprovedRenders ---


    /**
     * Asigna la imagen del preview al destino según el contexto: si el editor
     * avanzado está activo la añade como capa nueva; si el visor muestra una
     * imagen 2D (imagen cargada o composición exportada al preview) la manda al
     * grid; en otro caso re-renderiza el STL 3D y lo asigna al grid.
     */
    public void asignarPreviewAlArchivo() {
        if (panel.isAdvanceEditActive()) {
            agregarPreviewAlEditor();
        } else if (panel.isShowing2DView()) {
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
            thumbnailsAprobados.add(dest);
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
                    "No hay ningún modelo cargado en el preview.\n"
                    + "Haz clic en un thumbnail de la rejilla primero.",
                    "Asignar preview", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            BufferedImage img = renderizarPreview();
            if (img == null) return;
            ImageIO.write(img, "PNG", currentPreviewPath.toFile());
            logger.info("Preview re-renderizado y guardado: {} ({}x{})",
                    currentPreviewPath.getFileName(), img.getWidth(), img.getHeight());
            putCachedTriangles(currentPreviewPath, currentTriangles);
            thumbnailsAprobados.add(currentPreviewPath);
            refreshThumbnails(currentPreviewPath);
        } catch (IOException e) {
            logger.error("Error al guardar PNG re-renderizado: {}", currentPreviewPath, e);
            JOptionPane.showMessageDialog(parentFrame,
                    "Error al guardar la imagen:\n" + e.getMessage(),
                    "Asignar preview", JOptionPane.ERROR_MESSAGE);
        }
    } // --- Fin del metodo asignarRenderAlGrid ---


    /**
     * Renderiza el STL actual con la orientación y los ajustes de
     * brillo/contraste/AA/fondo del panel de preview.
     *
     * @return la imagen renderizada, o null si no hay modelo cargado
     */
    private BufferedImage renderizarPreview() {
        if (currentTriangles == null) return null;

        double rotX = panel.getPreview3DFX().getRotateXAngle();
        double rotY = panel.getPreview3DFX().getRotateYAngle();
        double rotZ = panel.getPreview3DFX().getRotateZAngle();
        int brightness = panel.getBrightnessSlider().getValue();
        int contrast = panel.getContrastSlider().getValue();
        boolean antiAlias = panel.getChkAntiAlias().isSelected();
        boolean wireframe = panel.getChkWireframe().isSelected();
        int superSample = panel.getCboCalidad().getSelectedIndex() + 1;
        renderer.setSuperSample(superSample);

        // El preview normaliza el modelo a ~200 unidades; el render AWT lo
        // encaja en (SIZE - 2*MARGIN) píxeles. Se convierte el pan de la cámara
        // (unidades de escena) a píxeles del render.
        double zoomScale = panel.getPreview3DFX().getZoomFactor();
        int renderSize = AwtModelRenderer.SIZE * superSample;
        double pxPorUnidad = (renderSize - 2f * AwtModelRenderer.MARGIN) / 200.0 * zoomScale;
        double panX = panel.getPreview3DFX().getPanX() * pxPorUnidad;
        double panY = panel.getPreview3DFX().getPanY() * pxPorUnidad;

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

        return renderer.renderizarConAjustes(currentTriangles,
                rotX, rotY, rotZ, antiAlias, brightness, contrast,
                bgMode, solidColor, gradientStart, gradientEnd,
                bgImage, bgImageScale, wireframe, panX, panY, zoomScale);
    } // --- Fin del metodo renderizarPreview ---


    /**
     * Añade la imagen del preview actual como capa nueva en el editor avanzado.
     * Si el visor muestra una imagen 2D usa esa imagen; en modo 3D re-renderiza
     * el STL con los ajustes actuales. Si el canvas está vacío se redimensiona
     * al tamaño de la imagen.
     */
    private void agregarPreviewAlEditor() {
        BufferedImage img = panel.isShowing2DView()
                ? panel.getCurrentImage2D()
                : renderizarPreview();
        if (img == null) {
            JOptionPane.showMessageDialog(parentFrame,
                    "No hay imagen en el preview.\n"
                    + "Carga una imagen o un modelo en la rejilla primero.",
                    "Agregar al editor", JOptionPane.WARNING_MESSAGE);
            return;
        }

        var aep = panel.getAdvanceEditPanel();
        if (aep == null || aep.getCanvas() == null) return;
        var cm = aep.getCanvas().getCanvasModel();
        var lm = aep.getCanvas().getLayerModel();
        if (cm == null || lm == null) return;

        if (lm.size() == 0) {
            cm.setSize(img.getWidth(), img.getHeight());
        }

        RenderCandidate selected = getSelectedCandidate();
        String nombre = selected != null ? selected.nombreBase : "Preview";
        modelo.editor.ImageLayer capa = new modelo.editor.ImageLayer(nombre, img,
                new java.awt.Rectangle(0, 0, img.getWidth(), img.getHeight()));
        lm.addLayer(capa);
        lm.setActiveLayer(capa);
        aep.getCanvas().repaint();
        logger.info("[RenderController] Preview añadido al editor avanzado como capa: {} ({}x{})",
                nombre, img.getWidth(), img.getHeight());
    } // --- Fin del metodo agregarPreviewAlEditor ---


    // ========== Métodos de control de vista ==========


    /**
     * Limpia el panel de preview (visor 2D y 3D). La cruceta del visor 3D
     * respeta el estado actual del control, no se fuerza.
     */
    public void clearPreview() {
        panel.clearViewer2D();
        panel.getPreview3DFX().clearMesh();
        panel.getPreview3DFX().setCrosshairVisible(panel.isCrosshair());
        currentPreviewPath = null;
        currentTriangles = null;
        logger.info("[RenderController] Preview limpiado.");
    } // --- Fin del metodo clearPreview ---


    /**
     * Cambia a la pestaña "Sin renderizar" y sincroniza el grid. Selecciona el
     * toggle de Grid3D y deselecciona los de Scanner y Grid2D (exclusividad de
     * vistas).
     */
    public void mostrarGrid3D() {
        panel.setScannerActive(false);
        deseleccionarToggle("button.render.scanner");
        deseleccionarToggle("button.render.grid2d");
        seleccionarToggle("button.render.grid3d");
        panel.selectCandidateTab(0);
        vistaActivaRender = VISTA_GRID3D;
    } // --- Fin del metodo mostrarGrid3D ---


    /**
     * Cambia a la pestaña "Con imagen" y sincroniza el grid. Selecciona el
     * toggle de Grid2D y deselecciona los de Scanner y Grid3D (exclusividad de
     * vistas).
     */
    public void mostrarGrid2D() {
        panel.setScannerActive(false);
        deseleccionarToggle("button.render.scanner");
        deseleccionarToggle("button.render.grid3d");
        seleccionarToggle("button.render.grid2d");
        panel.selectCandidateTab(1);
        vistaActivaRender = VISTA_GRID2D;
    } // --- Fin del metodo mostrarGrid2D ---


    /**
     * Alterna el modo collage (multi-imagen).
     */
    public void toggleCollageMode() {
        boolean nuevo = !panel.isCollageMode();
        panel.setCollageMode(nuevo);
        logger.info("[RenderController] Modo collage: {}", nuevo);
    } // --- Fin del metodo toggleCollageMode ---


    public void toggleAdvanceEditMode() {
        // En el Modo Editor el panel muestra el documento del editor: el toggle de
        // la barra de render no debe desactivar el editor ni tocar su documento.
        if (editorDocumentoCargadoEnPanel) {
            return;
        }
        boolean nuevo = !panel.isAdvanceEditActive();
        if (nuevo) {
            inicializarCanvasEditor();
        }
        panel.setAdvanceEditActive(nuevo);
        logger.info("[RenderController] Modo editor avanzado: {}", nuevo);
    } // --- Fin del metodo toggleAdvanceEditMode ---


    /**
     * Activa el Modo Editor como modo de trabajo: garantiza que el editor avanzado
     * esté inicializado, carga el documento del editor en el panel (swap del slot
     * RENDER al slot EDITOR) y activa el fullscreen del editor (ocultando los
     * paneles laterales de preview y archivos).
     */
    public void activarModoEditor() {
        inicializarCanvasEditor();
        inicializarGestorDocumento();
        cargarDocumentoEnPanel(canvasModelEditor, layerModelEditor, true);
        panel.setAdvanceEditActive(true);
        panel.setEditorFullscreen(true);
        panel.getAdvanceEditPanel().setModoEditorActivo(true);
        refrescarTituloEditor();
        logger.info("[RenderController] Modo Editor activado.");
    } // --- Fin del metodo activarModoEditor ---


    /**
     * Garantiza que el canvas del editor tenga el documento del RENDER (slot que
     * el panel muestra por defecto) y el controlador de herramientas (idempotente).
     * Se llama tanto al activar el Modo Editor como al usar el editor en modo
     * RENDER. No toca el documento del EDITOR.
     */
    private void inicializarCanvasEditor() {
        var aep = panel.getAdvanceEditPanel();
        if (canvasModelRender == null) {
            canvasModelRender = new CanvasModel(1920, 1080);
        }
        if (layerModelRender == null) {
            layerModelRender = new LayerModel();
        }
        aep.setCanvasModel(canvasModelRender);
        aep.setLayerModel(layerModelRender);
        if (transformGizmo == null) {
            transformGizmo = new modelo.gizmo.TransformGizmo();
        }
        if (canvasController == null) {
            var sm = aep.getCanvas().getSelectionModel();
            canvasController = new controlador.tools.CanvasController(
                    aep.getCanvas(), aep.getComponentBar(), canvasModelRender, layerModelRender, sm, transformGizmo);
            canvasController.registerTool(new controlador.tools.TransformTool());
            canvasController.registerTool(new controlador.tools.MarqueeSelectionTool());
            canvasController.registerTool(new controlador.tools.LayerSelectionTool());
            canvasController.registerTool(new controlador.tools.MagicWandTool());
            canvasController.registerTool(new controlador.tools.PaintBucketTool());
            canvasController.registerTool(new controlador.tools.ColorPickerTool());
            canvasController.registerTool(new controlador.tools.CropTool());
            canvasController.registerTool(new controlador.tools.TextTool());
            canvasController.registerTool(new controlador.tools.ShapeTool());
            canvasController.registerTool(new controlador.tools.GradientTool());
            canvasController.registerTool(new controlador.tools.ZoomTool());
            canvasController.registerTool(new controlador.tools.EditTool());
            var textTool = (controlador.tools.TextTool) canvasController.getTool(
                    AppActionCommands.CMD_ADVANCED_EDITOR_TEXTO);
            canvasController.getLayerEditorRegistry()
                    .register(new controlador.tools.editors.TextLayerEditor(textTool));
            canvasController.getLayerEditorRegistry()
                    .register(new controlador.tools.editors.ShapeLayerEditor());
            aep.setCanvasController(canvasController);
            aep.getCanvas().setCanvasController(canvasController);
            canvasController.setActiveTool(AppActionCommands.CMD_ADVANCED_EDITOR_EDICION);
            canvasController.setContentChangeCallback(this::notificarModificacionDocumentoActivo);
            canvasController.setPasteCallback(this::pegarImagenEditor);
            canvasController.setGestureStartCallback(this::iniciarGestoEditor);
            canvasController.setUndoCallback(this::deshacerEditor);
            canvasController.setRedoCallback(this::rehacerEditor);
            canvasController.setDeleteCallback(this::borrarCapaEditor);
            canvasController.setInvertSelectionCallback(this::invertirSeleccionEditor);
        }
        editorDocumentoCargadoEnPanel = false;
    } // --- Fin del metodo inicializarCanvasEditor ---


    /**
     * Inicializa (si hace falta) el gestor de documento del editor, ligándolo
     * SIEMPRE al slot del EDITOR (independiente del slot RENDER que muestra el
     * panel por defecto).
     */
    private void inicializarGestorDocumento() {
        if (editorDocumentManager == null) {
            editorDocumentManager = new servicios.editor.EditorDocumentManager(config);
        }
        if (canvasModelEditor == null) {
            canvasModelEditor = new CanvasModel(1920, 1080);
        }
        if (layerModelEditor == null) {
            layerModelEditor = new LayerModel();
        }
        if (editorDocumentManager.getCanvasModel() != canvasModelEditor
                || editorDocumentManager.getLayerModel() != layerModelEditor) {
            editorDocumentManager.setDocument(canvasModelEditor, layerModelEditor);
            editorDocumentManager.setDirtyNotifier(this::refrescarTituloEditor);
        }
    } // --- Fin del metodo inicializarGestorDocumento ---


    /**
     * Carga un par (CanvasModel, LayerModel) en el panel del editor (swap de
     * documento) y re-apunta el contexto del controlador de herramientas.
     *
     * @param cm               modelo del lienzo a cargar
     * @param lm               modelo de capas a cargar
     * @param esDocumentoEditor {@code true} si es el documento del Modo Editor
     */
    private void cargarDocumentoEnPanel(CanvasModel cm, LayerModel lm, boolean esDocumentoEditor) {
        var aep = panel.getAdvanceEditPanel();
        aep.setCanvasModel(cm);
        aep.setLayerModel(lm);
        if (esDocumentoEditor && editorDocumentManager != null) {
            // setLayerModel reinstala el listener de la vista (limpia los demás),
            // así que se re-engancha el de suciedad del documento del editor.
            editorDocumentManager.setDocument(cm, lm);
            conectarHistorialConBarra();
        }
        if (canvasController != null) {
            canvasController.setContext(cm, lm, aep.getCanvas().getSelectionModel(), transformGizmo);
        }
        editorDocumentoCargadoEnPanel = esDocumentoEditor;
        aep.getCanvas().repaint();
        aep.refreshLayerCards();
    } // --- Fin del metodo cargarDocumentoEnPanel ---


    /**
     * Marca sucio el documento del editor solo si es el documento activo en el
     * panel (las modificaciones sobre el slot RENDER no deben ensuciarlo).
     */
    private void notificarModificacionDocumentoActivo() {
        if (editorDocumentoCargadoEnPanel && editorDocumentManager != null) {
            editorDocumentManager.notificarModificacion();
        }
    } // --- Fin del metodo notificarModificacionDocumentoActivo ---


    /**
     * Conecta el historial de undo/redo del documento del editor con la barra
     * de opciones (botones Deshacer/Rehacer/Historial) y los callbacks de
     * notificación.
     */
    private void conectarHistorialConBarra() {
        if (editorDocumentManager == null) return;
        var history = editorDocumentManager.getHistory();
        if (history == null) return;
        var aep = panel.getAdvanceEditPanel();
        if (aep == null) return;
        aep.setEditorHistory(history);
        var bar = aep.getComponentBar();
        if (bar != null) {
            bar.setEditorHistory(history);
        }
        history.setCambioCallback(() -> {
            if (aep.getComponentBar() != null) {
                aep.getComponentBar().updateHistoryButtons();
            }
        });
        if (bar != null) {
            bar.updateHistoryButtons();
        }
    } // --- Fin del metodo conectarHistorialConBarra ---


    /**
     * Abre una transacción de undo/redo al inicio de un gesto de herramienta
     * que modifica contenido, etiquetándola con el nombre de la herramienta.
     */
    private void iniciarGestoEditor() {
        if (!editorDocumentoCargadoEnPanel) return;
        var history = getOrCreateEditorDocumentManager().getHistory();
        if (history == null) return;
        String nombre = servicios.editor.EditorHistory.NOMBRE_PASO_GENERICO;
        boolean esPixeles = false;
        if (canvasController != null && canvasController.getActiveTool() != null) {
            var tool = canvasController.getActiveTool();
            esPixeles = tool instanceof controlador.tools.PaintBucketTool;
            nombre = nombreHerramienta(tool.getCommandKey());
        }
        // Cierra una transacción pendiente que haya podido quedar abierta
        // (p. ej. un clic de selección en una herramienta marcada como modificadora)
        history.endGesture();
        history.beginGesture(nombre, esPixeles);
    } // --- Fin del metodo iniciarGestoEditor ---


    /**
     * Deshace el último paso del historial del documento del editor (Ctrl+Z).
     */
    private void deshacerEditor() {
        if (!editorDocumentoCargadoEnPanel) return;
        var history = getOrCreateEditorDocumentManager().getHistory();
        if (history != null) {
            history.undo();
        }
    } // --- Fin del metodo deshacerEditor ---


    /**
     * Rehace el siguiente paso del historial del documento del editor
     * (Ctrl+Y / Ctrl+Shift+Z).
     */
    private void rehacerEditor() {
        if (!editorDocumentoCargadoEnPanel) return;
        var history = getOrCreateEditorDocumentManager().getHistory();
        if (history != null) {
            history.redo();
        }
    } // --- Fin del metodo rehacerEditor ---


    /**
     * Elimina las capas seleccionadas del documento del editor (Supr /
     * Retroceso). Si no hay selección múltiple, elimina la capa activa. La
     * operación se registra en el historial como "Eliminar capa".
     */
    private void borrarCapaEditor() {
        if (!editorDocumentoCargadoEnPanel) return;
        var aep = panel.getAdvanceEditPanel();
        if (aep == null) return;
        var lm = aep.getCanvas().getLayerModel();
        if (lm == null || lm.size() == 0) return;

        // Con selección de píxeles activa, Supr borra el contenido seleccionado
        // de la capa activa de imagen (como Photoshop) en lugar de la capa entera.
        var sm = aep.getCanvas().getSelectionModel();
        if (sm != null && sm.isActive()) {
            var activa = lm.getActiveLayer();
            if (activa instanceof modelo.editor.ImageLayer imgLayer && !activa.isLocked()) {
                if (borrarContenidoSeleccionado(imgLayer, sm)) {
                    var bar = aep.getComponentBar();
                    if (bar != null) {
                        bar.updateEditLayerFields(lm.getActiveLayer());
                    }
                    aep.getCanvas().repaint();
                    return;
                }
            }
        }

        List<Integer> indices = new ArrayList<>(lm.getSelectedIndices());
        if (indices.isEmpty()) {
            int activa = lm.getActiveIndex();
            if (activa >= 0) {
                indices.add(activa);
            }
        }
        if (indices.isEmpty()) return;
        indices.sort((a, b) -> Integer.compare(b, a));

        var history = getOrCreateEditorDocumentManager().getHistory();
        Runnable operacion = () -> {
            for (int idx : indices) {
                lm.removeLayer(idx);
            }
        };
        if (history != null) {
            history.record("Eliminar capa", operacion);
        } else {
            operacion.run();
        }

        var bar = aep.getComponentBar();
        if (bar != null) {
            bar.updateEditLayerFields(lm.getActiveLayer());
        }
        aep.getCanvas().repaint();
    } // --- Fin del metodo borrarCapaEditor ---


    /**
     * Borra los píxeles de la capa de imagen dentro de la selección activa
     * (coordenadas de canvas), mapeado al espacio de la imagen por escala. Si
     * la selección tiene máscara, se borra solo la forma irregular; si no, se
     * borra el rectángulo completo. La operación se registra en el historial
     * como paso de píxeles para que el undo/redo capture el contenido borrado.
     *
     * @param layer capa de imagen sobre la que borrar
     * @param sm    selección de píxeles activa
     * @return {@code true} si la selección cruza la imagen y se borró algo
     */
    private boolean borrarContenidoSeleccionado(modelo.editor.ImageLayer layer, modelo.editor.SelectionModel sm) {
        Rectangle bounds = layer.getBounds();
        BufferedImage img = layer.getImage();
        if (bounds == null || img == null || sm == null || !sm.isActive()) return false;

        Rectangle sel = sm.getBounds();
        int w = img.getWidth();
        int h = img.getHeight();
        int x0 = Math.max(0, (int) ((long) (sel.x - bounds.x) * w / bounds.width));
        int y0 = Math.max(0, (int) ((long) (sel.y - bounds.y) * h / bounds.height));
        int x1 = Math.min(w - 1, (int) ((long) (sel.x + sel.width - bounds.x) * w / bounds.width));
        int y1 = Math.min(h - 1, (int) ((long) (sel.y + sel.height - bounds.y) * h / bounds.height));
        if (x1 < x0 || y1 < y0) return false;

        var history = getOrCreateEditorDocumentManager().getHistory();
        Runnable operacion;
        if (sm.hasMask()) {
            // Máscara irregular: borrar solo los píxeles seleccionados
            java.util.BitSet maskImg = new java.util.BitSet(w * h);
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    int cx = bounds.x + (int) ((long) x * bounds.width / w);
                    int cy = bounds.y + (int) ((long) y * bounds.height / h);
                    if (sm.contains(cx, cy)) {
                        maskImg.set(y * w + x);
                    }
                }
            }
            operacion = () -> layer.clearMask(maskImg);
        } else {
            operacion = () -> layer.clearRegion(new Rectangle(x0, y0, x1 - x0 + 1, y1 - y0 + 1));
        }
        if (history != null) {
            // Operación de píxeles: se fuerza el paso porque la mutación
            // in-place no es detectable por comparación de snapshots.
            history.endGesture();
            history.beginGesture("Borrar selección", true);
            operacion.run();
            history.endGesture();
        } else {
            operacion.run();
        }
        notificarModificacionDocumentoActivo();
        return true;
    } // --- Fin del metodo borrarContenidoSeleccionado ---


    /**
     * Invierte la selección de capas del documento cargado en el panel (RENDER
     * o EDITOR), de modo que el atajo Ctrl+Shift+I funcione en ambos modos.
     */
    private void invertirSeleccionEditor() {
        var aep = panel.getAdvanceEditPanel();
        if (aep == null) return;
        aep.invertirSeleccion();
    } // --- Fin del metodo invertirSeleccionEditor ---


    /**
     * Traduce el comando canónico de una herramienta a un nombre legible para
     * el paso de historial.
     *
     * @param cmd comando canónico de la herramienta
     * @return nombre legible del paso
     */
    private String nombreHerramienta(String cmd) {
        if (cmd == null) return servicios.editor.EditorHistory.NOMBRE_PASO_GENERICO;
        return switch (cmd) {
            case AppActionCommands.CMD_ADVANCED_EDITOR_EDICION -> "Edición";
            case AppActionCommands.CMD_ADVANCED_EDITOR_TRANSFORMAR -> "Transformar";
            case AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_MARCO -> "Selección marco";
            case AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_CAPA -> "Selección de capa";
            case AppActionCommands.CMD_ADVANCED_EDITOR_VARITA -> "Varita mágica";
            case AppActionCommands.CMD_ADVANCED_EDITOR_RECORTAR -> "Recortar";
            case AppActionCommands.CMD_ADVANCED_EDITOR_BOTE_PINTURA -> "Bote de pintura";
            case AppActionCommands.CMD_ADVANCED_EDITOR_DEGRADADO -> "Degradado";
            case AppActionCommands.CMD_ADVANCED_EDITOR_TEXTO -> "Texto";
            case AppActionCommands.CMD_ADVANCED_EDITOR_FORMAS -> "Formas";
            default -> servicios.editor.EditorHistory.NOMBRE_PASO_GENERICO;
        };
    } // --- Fin del metodo nombreHerramienta ---


    private void refrescarTituloEditor() {
        if (editorDocumentManager == null) return;
        String sufijo = editorDocumentManager.getArchivoActivo() != null
                ? "[Documento: " + editorDocumentManager.getNombreDocumento() + "]"
                : "[" + editorDocumentManager.getNombreDocumento() + "]";
        if (editorDocumentManager.hayCambiosSinGuardar()) {
            sufijo = "*" + sufijo;
        }
        if (viewManager != null) {
            viewManager.setEditorDocumentoTitulo(sufijo);
            viewManager.actualizarTituloVentana();
        }
        logger.debug("[RenderController] Documento ({}) sucio={}",
                editorDocumentManager.getNombreDocumento(),
                editorDocumentManager.hayCambiosSinGuardar());
    } // --- Fin del metodo refrescarTituloEditor ---


    public void setViewManager(controlador.managers.ViewManager viewManager) {
        this.viewManager = viewManager;
    } // --- Fin del metodo setViewManager ---


    public servicios.editor.EditorDocumentManager getEditorDocumentManager() {
        return editorDocumentManager;
    } // --- Fin del metodo getEditorDocumentManager ---


    /**
     * Devuelve el gestor de documento del editor, cre\u00E1ndolo si hace falta
     * (por ejemplo, al comprobar recuperaciones antes de entrar en el modo).
     *
     * @return el gestor de documento del editor (nunca {@code null})
     */
    public servicios.editor.EditorDocumentManager getOrCreateEditorDocumentManager() {
        if (editorDocumentManager == null) {
            inicializarGestorDocumento();
        }
        return editorDocumentManager;
    } // --- Fin del metodo getOrCreateEditorDocumentManager ---


    /**
     * Pega la imagen del portapapeles del sistema como una nueva capa
     * centrada en el lienzo activo del editor (Ctrl+V / bot\u00F3n Pegar). El
     * pegado se aplica al documento que el panel esté mostrando en ese momento
     * (slot RENDER si se usa el editor dentro del modo Render, slot EDITOR si
     * se usa el Modo Editor).
     */
    public void pegarImagenEditor() {
        var aep = panel.getAdvanceEditPanel();
        var cm = aep.getCanvas().getCanvasModel();
        var lm = aep.getCanvas().getLayerModel();
        if (cm == null || lm == null) return;

        BufferedImage img = controlador.utils.ImageClipboard.read();
        if (img == null) {
            JOptionPane.showMessageDialog(parentFrame,
                    "No hay ninguna imagen en el portapapeles del sistema.",
                    "Pegar imagen", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        var history = editorDocumentoCargadoEnPanel && editorDocumentManager != null
                ? editorDocumentManager.getHistory() : null;
        Runnable operacion = () -> {
            BufferedImage finalImg = img;
            if (finalImg.getWidth() > cm.getWidth() || finalImg.getHeight() > cm.getHeight()) {
                int w = Math.max(1, Math.min(finalImg.getWidth(), cm.getWidth()));
                int h = Math.max(1, Math.min(finalImg.getHeight(), cm.getHeight()));
                BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = scaled.createGraphics();
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(finalImg, 0, 0, w, h, null);
                g.dispose();
                finalImg = scaled;
            }

            int x = (cm.getWidth() - finalImg.getWidth()) / 2;
            int y = (cm.getHeight() - finalImg.getHeight()) / 2;
            modelo.editor.ImageLayer layer = new modelo.editor.ImageLayer("Imagen pegada",
                    finalImg, new java.awt.Rectangle(x, y, finalImg.getWidth(), finalImg.getHeight()));
            lm.addLayer(layer);
            lm.setActiveLayer(layer);
        };
        if (history != null) {
            history.record("Pegar imagen", operacion);
        } else {
            operacion.run();
        }
        notificarModificacionDocumentoActivo();
        refrescarUiEditor();
        logger.info("[RenderController] Imagen pegada como nueva capa en el editor.");
    } // --- Fin del metodo pegarImagenEditor ---


    /**
     * Devuelve una imagen plana de lo visible en el panel render/editor para
     * copiar al portapapeles: si el editor avanzado est\u00E1 activo aplana sus
     * capas visibles; si se muestra la vista 2D captura esa vista.
     *
     * @return la imagen aplanada, o {@code null} si no hay nada copiable
     */
    public BufferedImage copiarImagenVisible() {
        if (panel.isAdvanceEditActive()) {
            var aep = panel.getAdvanceEditPanel();
            if (aep != null) {
                BufferedImage img = aep.getComponentBar().flattenVisibleEditorImage();
                if (img != null) return img;
            }
        }
        if (panel.isShowing2DView()) {
            return panel.capturarVistaActual();
        }
        return null;
    } // --- Fin del metodo copiarImagenVisible ---


    public void nuevoDocumentoEditor() {
        if (editorDocumentManager == null) {
            inicializarGestorDocumento();
        }
        editorDocumentManager.nuevoDocumento();
        refrescarUiEditor();
    } // --- Fin del metodo nuevoDocumentoEditor ---


    public void guardarDocumentoEditor() {
        if (editorDocumentManager == null) return;
        editorDocumentManager.guardarAArchivo();
        refrescarUiEditor();
    } // --- Fin del metodo guardarDocumentoEditor ---


    public void guardarDocumentoComoEditor(java.nio.file.Path ruta) {
        if (editorDocumentManager == null) return;
        editorDocumentManager.guardarComo(ruta);
        refrescarUiEditor();
    } // --- Fin del metodo guardarDocumentoComoEditor ---


    public void abrirDocumentoEditor(java.nio.file.Path ruta) {
        if (editorDocumentManager == null) {
            inicializarGestorDocumento();
        }
        if (editorDocumentManager.abrirDocumento(ruta)) {
            refrescarUiEditor();
        }
    } // --- Fin del metodo abrirDocumentoEditor ---


    private void refrescarUiEditor() {
        var aep = panel.getAdvanceEditPanel();
        aep.getCanvas().repaint();
        aep.refreshLayerCards();
    } // --- Fin del metodo refrescarUiEditor ---


    public void abrirDocumentoEditorDialogo() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setDialogTitle("Abrir documento del editor");
        fc.setFileFilter(new FileNameExtensionFilter("Documento del editor (*.edoc)", "edoc"));
        if (fc.showOpenDialog(parentFrame) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        abrirDocumentoEditor(fc.getSelectedFile().toPath());
    } // --- Fin del metodo abrirDocumentoEditorDialogo ---


    public void guardarDocumentoEditorDialogo() {
        if (editorDocumentManager == null || editorDocumentManager.getArchivoActivo() == null) {
            guardarDocumentoComoEditorDialogo();
            return;
        }
        guardarDocumentoEditor();
    } // --- Fin del metodo guardarDocumentoEditorDialogo ---


    public void guardarDocumentoComoEditorDialogo() {
        if (editorDocumentManager == null) {
            inicializarGestorDocumento();
        }
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setDialogTitle("Guardar documento del editor");
        fc.setFileFilter(new FileNameExtensionFilter("Documento del editor (*.edoc)", "edoc"));
        java.nio.file.Path actual = editorDocumentManager.getArchivoActivo();
        if (actual != null) {
            fc.setSelectedFile(actual.toFile());
        } else {
            fc.setSelectedFile(new java.io.File(editorDocumentManager.getNombreDocumento() + ".edoc"));
        }
        if (fc.showSaveDialog(parentFrame) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        java.io.File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".edoc")) {
            f = new java.io.File(f.getParentFile(), f.getName() + ".edoc");
        }
        if (f.exists()) {
            int opcion = javax.swing.JOptionPane.showConfirmDialog(parentFrame,
                    "El archivo \"" + f.getName() + "\" ya existe.\n\u00BFSobrescribirlo?",
                    "Confirmar sobrescritura", javax.swing.JOptionPane.YES_NO_OPTION,
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            if (opcion != javax.swing.JOptionPane.YES_OPTION) return;
        }
        guardarDocumentoComoEditor(f.toPath());
    } // --- Fin del metodo guardarDocumentoComoEditorDialogo ---


    /**
     * Desactiva el Modo Editor: restaura el documento del RENDER en el panel
     * (swap del slot EDITOR al slot RENDER), sale del fullscreen y restaura la
     * vista normal del modo render.
     */
    public void desactivarModoEditor() {
        if (canvasModelRender == null || layerModelRender == null) {
            inicializarCanvasEditor();
        }
        cargarDocumentoEnPanel(canvasModelRender, layerModelRender, false);
        panel.setEditorFullscreen(false);
        panel.setAdvanceEditActive(false);
        panel.getAdvanceEditPanel().setModoEditorActivo(false);
        if (viewManager != null) {
            viewManager.setEditorDocumentoTitulo(null);
            viewManager.actualizarTituloVentana();
        }
        logger.info("[RenderController] Modo Editor desactivado.");
    } // --- Fin del metodo desactivarModoEditor ---


    /**
     * Restaura la UI del Modo Editor al entrar en él (re-afirma el estado activo de
     * forma idempotente).
     */
    public void restaurarUiModoEditor() {
        if (panel.isAdvanceEditActive()) {
            panel.setEditorFullscreen(true);
        }
    } // --- Fin del metodo restaurarUiModoEditor ---


    // ========== Stubs gestión de capas ==========


    public void capaAlFrente() {
        int idx = panel.getSelectedLayerIndex();
        int size = panel.getLayersListModel().getSize();
        if (idx >= 0 && idx < size - 1) {
            panel.moveLayer(idx, size - 1);
        }
    }


    public void capaSubirNivel() {
        int idx = panel.getSelectedLayerIndex();
        int size = panel.getLayersListModel().getSize();
        if (idx >= 0 && idx < size - 1) {
            panel.moveLayer(idx, idx + 1);
        }
    }


    public void capaBajarNivel() {
        int idx = panel.getSelectedLayerIndex();
        if (idx > 0) {
            panel.moveLayer(idx, idx - 1);
        }
    }


    public void capaAlFondo() {
        int idx = panel.getSelectedLayerIndex();
        if (idx > 0) {
            panel.moveLayer(idx, 0);
        }
    }


    public void cleanAndAddLayer() {
        if (!panel.isCollageMode()) {
            JOptionPane.showMessageDialog(parentFrame,
                    "Activa el modo collage primero.",
                    "Capas", JOptionPane.WARNING_MESSAGE);
            return;
        }
        panel.clearLayers();
        addLayerInternal();
    }


    public void addLayer() {
        if (!panel.isCollageMode()) {
            JOptionPane.showMessageDialog(parentFrame,
                    "Activa el modo collage primero.",
                    "Capas", JOptionPane.WARNING_MESSAGE);
            return;
        }
        addLayerInternal();
    }


    private void addLayerInternal() {
        RenderCandidate candidate = getSelectedCandidate();
        if (candidate == null) return;

        // Si la galería está visible y hay una selección, usar esa imagen directamente
        if (panel.isFilmstripVisible()) {
            ImageLayer filmstripSel = panel.getFilmstripList().getSelectedValue();
            if (filmstripSel != null && filmstripSel.getImage() != null) {
                panel.addLayer(new ImageLayer(filmstripSel.getImage(), filmstripSel.getName()));
                logger.info("Capa añadida desde filmstrip: {}", filmstripSel.getName());
                return;
            }
        }

        // Fallback: extraer del ZIP vía lista de contenido (tab "Con imagen")
        if (!panel.isCandidateTabConImagen()) return;
        ImageEntry img = panel.getContentImageList().getSelectedValue();
        if (img == null) {
            JOptionPane.showMessageDialog(parentFrame,
                    "Selecciona una imagen de la galería o de la lista de contenido.",
                    "Añadir capa", JOptionPane.WARNING_MESSAGE);
            return;
        }

        TaskProgressDialog dialog = new TaskProgressDialog(getParentFrame(),
                "Extrayendo imagen", "Extrayendo " + img.filename() + "...");

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                Path imgOutput = imagesDir.resolve(candidate.nombreBase);
                Files.createDirectories(imgOutput);
                ZipExtractor.extractSingleFile(candidate.path, img.filename(), imgOutput);
                Path extracted = imgOutput.resolve(img.filename());
                if (!Files.exists(extracted)) {
                    try (var walk = Files.walk(imgOutput)) {
                        extracted = walk.filter(Files::isRegularFile)
                                .filter(p -> p.getFileName().toString()
                                        .equalsIgnoreCase(Path.of(img.filename()).getFileName().toString()))
                                .findFirst().orElse(null);
                    }
                }
                if (extracted != null && Files.exists(extracted)) {
                    return ImageIO.read(extracted.toFile());
                }
                return null;
            }
            @Override
            protected void done() {
                dialog.closeDialog();
                try {
                    BufferedImage bi = get();
                    if (bi != null) {
                        ImageLayer layer = new ImageLayer(bi, img.filename());
                        panel.addLayer(layer);
                        logger.info("Capa añadida: {}", img.filename());
                    }
                } catch (Exception ex) {
                    logger.warn("Error al añadir capa {}", img.filename(), ex);
                }
            }
        }.execute();
        dialog.setVisible(true);
    }

    public void deleteLayer() {
        int idx = panel.getSelectedLayerIndex();
        if (idx >= 0) {
            panel.removeLayer(idx);
        }
    }


    /**
     * Abre un selector de carpeta y guarda la imagen actual del visor como PNG.
     * En modo "Con imagen" captura la vista 2D con zoom/pan.
     * En modo "Sin renderizar" re-renderiza el 3D con los ajustes actuales.
     */
    public void descargarPreview() {
        RenderCandidate selected = getSelectedCandidate();
        if (selected == null) {
            JOptionPane.showMessageDialog(parentFrame,
                    "Selecciona un candidato de la lista primero.",
                    "Guardar Preview", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path folderInicial = selected.path.getParent();

        JFileChooser chooser = new JFileChooser(folderInicial.toFile());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Guardar preview");

        // Formato por defecto: PNG. El usuario puede cambiar extensión/nombre libremente.
        List<String> formatos = formatosSoportados();
        FileNameExtensionFilter filtroPng = null;
        List<FileNameExtensionFilter> filtros = new ArrayList<>();
        for (String f : formatos) {
            FileNameExtensionFilter ff = new FileNameExtensionFilter(
                    f.toUpperCase() + " Image (*." + f + ")", f);
            filtros.add(ff);
            if ("png".equals(f)) filtroPng = ff;
        }
        for (FileNameExtensionFilter ff : filtros) {
            chooser.addChoosableFileFilter(ff);
        }
        FileNameExtensionFilter filtroInicial = (filtroPng != null) ? filtroPng
                : (filtros.isEmpty() ? null : filtros.get(0));
        if (filtroInicial != null) {
            chooser.setFileFilter(filtroInicial);
        }

        // Nombre por defecto: el del archivo original sin extensión de compresión,
        // conservando mayúsculas. Editable por el usuario.
        String nombrePorDefecto = nombreArchivoSinCompresion(selected.path);
        chooser.setSelectedFile(new File(folderInicial.toFile(), nombrePorDefecto + ".png"));

        // Al cambiar el filtro de extensión, actualizar la extensión del nombre propuesto
        chooser.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, ev -> {
            if (ev.getNewValue() instanceof FileNameExtensionFilter nf && nf.getExtensions().length > 0) {
                File actual = chooser.getSelectedFile();
                if (actual != null && actual.getParentFile() != null) {
                    String nombre = actual.getName();
                    int dot = nombre.lastIndexOf('.');
                    String base = dot >= 0 ? nombre.substring(0, dot) : nombre;
                    chooser.setSelectedFile(new File(actual.getParentFile(), base + "." + nf.getExtensions()[0]));
                }
            }
        });

        if (chooser.showSaveDialog(parentFrame) != JFileChooser.APPROVE_OPTION) return;

        Path destFile = chooser.getSelectedFile().toPath();
        String formato = detectarFormato(destFile, chooser.getFileFilter());
        if (formato == null) {
            JOptionPane.showMessageDialog(parentFrame,
                    "Formato no soportado. Elige una extensión de las ofrecidas.",
                    "Guardar Preview", JOptionPane.WARNING_MESSAGE);
            return;
        }
        destFile = asegurarExtension(destFile, formato);

        if (Files.exists(destFile)) {
            int opcion = JOptionPane.showConfirmDialog(parentFrame,
                    "Ya existe:\n" + destFile + "\n\n¿Deseas sobrescribirlo?",
                    "Guardar Preview", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (opcion != JOptionPane.OK_OPTION) return;
        }

        try {
            if (panel.isCandidateTabConImagen()) {
                BufferedImage captura = panel.capturarVistaActual();
                if (captura != null) {
                    escribirPreview(captura, formato, destFile);
                    logger.info("Preview 2D descargado: {} (zoom={})",
                            destFile.getFileName(), String.format("%.2f", panel.getImageZoom()));
                } else {
                    if (currentPreviewPath == null || !Files.exists(currentPreviewPath)) {
                        JOptionPane.showMessageDialog(parentFrame,
                                "No hay imagen cargada en el visor.",
                                "Guardar Preview", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    BufferedImage img = ImageIO.read(currentPreviewPath.toFile());
                    if (img == null) {
                        Files.copy(currentPreviewPath, destFile, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        escribirPreview(img, formato, destFile);
                    }
                    logger.info("Imagen original copiada: {}", destFile.getFileName());
                }
            } else {
                if (currentTriangles == null) {
                    JOptionPane.showMessageDialog(parentFrame,
                            "No hay ning\u00FAn modelo cargado en el preview.",
                            "Guardar Preview", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                BufferedImage img = renderizarPreview();
                if (img == null) return;
                escribirPreview(img, formato, destFile);
                logger.info("Preview 3D descargado: {} ({}x{})",
                        destFile.getFileName(), img.getWidth(), img.getHeight());
            }

            JOptionPane.showMessageDialog(parentFrame,
                    "Preview guardado en:\n" + destFile,
                    "Guardar Preview", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            logger.error("Error al descargar preview", e);
            JOptionPane.showMessageDialog(parentFrame,
                    "Error al guardar el preview:\n" + e.getMessage(),
                    "Guardar Preview", JOptionPane.ERROR_MESSAGE);
        }
    } // --- Fin del metodo descargarPreview ---


    /**
     * Devuelve los formatos de imagen con escritor disponible en ImageIO,
     * en orden de preferencia.
     *
     * @return lista de extensiones (p. ej. "png", "jpg", "bmp")
     */
    private List<String> formatosSoportados() {
        Set<String> escritores = new HashSet<>();
        for (String n : ImageIO.getWriterFormatNames()) {
            escritores.add(n.toLowerCase());
        }
        String[] preferidos = { "png", "jpg", "jpeg", "bmp", "gif", "tiff", "webp" };
        List<String> resultado = new ArrayList<>();
        for (String p : preferidos) {
            if (escritores.contains(p)) {
                resultado.add(p);
            }
        }
        return resultado;
    } // --- Fin del metodo formatosSoportados ---


    /**
     * Deriva el formato de salida a partir de la extensión del archivo elegido,
     * o del filtro de formato activo en el diálogo.
     *
     * @param destFile archivo seleccionado
     * @param filtro   filtro activo en el JFileChooser
     * @return extensión de formato soportada, o null si no hay coincidencia
     */
    private String detectarFormato(Path destFile, javax.swing.filechooser.FileFilter filtro) {
        String nombre = destFile.getFileName().toString().toLowerCase();
        for (String f : formatosSoportados()) {
            if (nombre.endsWith("." + f)) {
                return f;
            }
        }
        if (filtro instanceof FileNameExtensionFilter nf && nf.getExtensions().length > 0) {
            return nf.getExtensions()[0].toLowerCase();
        }
        return null;
    } // --- Fin del metodo detectarFormato ---


    /**
     * Garantiza que el archivo de salida use la extensión del formato indicado:
     * la añade si el nombre no tiene extensión y reemplaza una extensión no
     * soportada por la del formato activo.
     *
     * @param destFile archivo destino
     * @param formato  extensión de formato (p. ej. "png")
     * @return archivo con extensión de formato garantizada
     */
    private Path asegurarExtension(Path destFile, String formato) {
        String nombre = destFile.getFileName().toString();
        int dot = nombre.lastIndexOf('.');
        if (dot < 0) {
            return destFile.resolveSibling(nombre + "." + formato);
        }
        String ext = nombre.substring(dot + 1).toLowerCase();
        if (ext.equals(formato)) {
            return destFile;
        }
        String base = nombre.substring(0, dot);
        return destFile.resolveSibling(base + "." + formato);
    } // --- Fin del metodo asegurarExtension ---


    /**
     * Obtiene el nombre del archivo original sin la extensión de compresión,
     * conservando las mayúsculas originales (el nombre base del pipeline está
     * normalizado a minúsculas).
     *
     * @param path archivo del candidato
     * @return nombre base legible (p. ej. "ChibiSTL - Scarlet Witch Chibi - PlanetSTL")
     */
    private String nombreArchivoSinCompresion(Path path) {
        String name = path.getFileName().toString();
        String[] compresiones = { ".rar", ".zip", ".7z", ".tar", ".gz", ".bz2", ".xz", ".zst", ".lz", ".lz4",
                ".001", ".stl", ".obj", ".3mf" };
        while (true) {
            String lower = name.toLowerCase();
            String ext = null;
            for (String e : compresiones) {
                if (lower.endsWith(e)) {
                    ext = e;
                    break;
                }
            }
            if (ext == null) break;
            String sinExt = name.substring(0, name.length() - ext.length());
            if (sinExt.isEmpty()) break;
            name = sinExt;
        }
        name = name.replaceAll("(?i)\\.part\\d+$", "");
        name = name.replaceAll("(?i)\\.r\\d+$", "");
        name = name.replaceAll("(?i)\\.z\\d{2}$", "");
        name = name.replaceAll("(?i)\\.7z\\.\\d{3,}$", "");
        return name;
    } // --- Fin del metodo nombreArchivoSinCompresion ---


    /**
     * Escribe una imagen en el formato y destino indicados.
     *
     * @param img      imagen a guardar
     * @param formato  "png", "jpg", etc.
     * @param destFile archivo de destino
     * @throws IOException si falla la escritura
     */
    private void escribirPreview(BufferedImage img, String formato, Path destFile) throws IOException {
        ImageIO.write(img, formato, destFile.toFile());
    } // --- Fin del metodo escribirPreview ---


    /**
     * Exporta los PNG aprobados del grid a la carpeta de su ZIP original.
     * Cada archivo aprobado se copia a {@code carpetaDelComprimido/<nombreBase>}
     * con la extensión del PNG aprobado. Antes pide confirmación y avisa de los
     * archivos destino que ya existen.
     */
    public void exportarAprobados() {
        if (thumbnailsAprobados.isEmpty()) {
            JOptionPane.showMessageDialog(parentFrame,
                    "No hay renders aprobados en el grid.\n"
                    + "Haz doble clic en un thumbnail para aprobarlo.",
                    "Exportar aprobados", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Path> aprobados = new ArrayList<>(thumbnailsAprobados);
        List<Path> aExportar = new ArrayList<>();
        List<String> destinos = new ArrayList<>();
        for (Path png : aprobados) {
            RenderCandidate c = buscarCandidatoDePng(png);
            if (c == null) continue;
            Path carpeta = c.path.getParent();
            if (carpeta == null || !Files.isDirectory(carpeta)) continue;
            String ext = png.getFileName().toString();
            int dot = ext.lastIndexOf('.');
            ext = dot >= 0 ? ext.substring(dot + 1) : "png";
            Path destino = carpeta.resolve(c.nombreBase + "." + ext);
            aExportar.add(png);
            destinos.add(destino.toString());
        }
        if (aExportar.isEmpty()) {
            JOptionPane.showMessageDialog(parentFrame,
                    "No se pudo asociar ningún aprobado a su comprimido original.",
                    "Exportar aprobados", JOptionPane.WARNING_MESSAGE);
            return;
        }

        long yaExisten = destinos.stream().filter(d -> Files.exists(Path.of(d))).count();
        StringBuilder msg = new StringBuilder("Se exportarán " + aExportar.size()
                + " representación(es) a la carpeta de su comprimido:");
        if (yaExisten > 0) {
            msg.append("\n\nAVISO: ").append(yaExisten)
                    .append(" archivo(s) de destino ya existen y serán sobrescritos.");
        }
        int opcion = JOptionPane.showConfirmDialog(parentFrame, msg.toString(),
                "Exportar aprobados", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) return;

        int copiados = 0;
        for (int i = 0; i < aExportar.size(); i++) {
            try {
                Path origen = aExportar.get(i);
                Path destino = Path.of(destinos.get(i));
                Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING);
                copiados++;
            } catch (IOException ex) {
                logger.error("Error exportando aprobado a {}", destinos.get(i), ex);
            }
        }
        JOptionPane.showMessageDialog(parentFrame,
                "Copiados " + copiados + " representación(es) a sus carpetas originales.",
                "Exportar aprobados", JOptionPane.INFORMATION_MESSAGE);
    } // --- Fin del metodo exportarAprobados ---


    /**
     * Asocia un PNG aprobado con su candidato comprimido original, usando primero
     * el mapa de fuentes y, si no está, buscando por coincidencia de nombre base.
     *
     * @param png ruta del PNG aprobado
     * @return candidato asociado, o null si no se encuentra
     */
    private RenderCandidate buscarCandidatoDePng(Path png) {
        SourceInfo info = pngSourceMap.get(png);
        if (info != null) return info.candidate();
        for (RenderCandidate c : getAllCandidates()) {
            Path esperado = outputDir.resolve(c.nombreBase + ".png");
            if (esperado.equals(png)) return c;
            if (png.startsWith(imagesDir.resolve(c.nombreBase))) return c;
        }
        return null;
    } // --- Fin del metodo buscarCandidatoDePng ---


    private void sincronizarFilmstripConListaContenido(ImageLayer sel) {
        String selName = sel.getName();
        if (panel.isCandidateTabConImagen()) {
            for (int i = 0; i < panel.getContentImageListModel().getSize(); i++) {
                var entry = panel.getContentImageListModel().getElementAt(i);
                if (entry.filename().equals(selName) || entry.filename().endsWith(selName)) {
                    panel.getContentImageList().setSelectedIndex(i);
                    break;
                }
            }
        } else {
            // Pestaña "Sin renderizar": sincronizar con lista de STLs
            String stlName = selName.endsWith(".stl") ? selName : null;
            if (stlName == null) return;
            for (int i = 0; i < panel.getContentListModel().getSize(); i++) {
                var entry = panel.getContentListModel().getElementAt(i);
                if (entry.filename().equals(stlName)) {
                    panel.getContentList().setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void refreshGallery() {
        if (!panel.isFilmstripVisible()) return;
        cancelGalleryWorker();
        panel.clearFilmstrip();
        loadGalleryContent();
    }

    private void cancelGalleryWorker() {
        if (galleryWorker != null && !galleryWorker.isDone()) {
            galleryWorker.cancel(true);
        }
        galleryWorker = null;
    }

    public void toggleGallery(java.awt.event.ActionEvent e) {
        boolean nuevo = !panel.isFilmstripVisible();
        if (!nuevo) {
            cancelGalleryWorker();
            panel.setFilmstripVisible(false);
            panel.clearFilmstrip();
            return;
        }

        boolean conImagen = panel.getCandidateTabs().getSelectedIndex() == 1;
        RenderCandidate candidate = conImagen
                ? panel.getCandidateListConImagen().getSelectedValue()
                : panel.getCandidateListSinImagen().getSelectedValue();
        if (candidate == null) {
            if (e.getSource() instanceof javax.swing.JToggleButton btn) {
                btn.setSelected(false);
            }
            JOptionPane.showMessageDialog(parentFrame,
                    "Seleccione un archivo comprimido en la lista de candidatos.",
                    "Galería ZIP", JOptionPane.WARNING_MESSAGE);
            return;
        }

        panel.setFilmstripVisible(true);
        panel.clearFilmstrip();
        loadGalleryContent();
    } // --- Fin del metodo toggleGallery ---


    private void loadGalleryContent() {
        boolean conImagen = panel.getCandidateTabs().getSelectedIndex() == 1;
        RenderCandidate candidate = conImagen
                ? panel.getCandidateListConImagen().getSelectedValue()
                : panel.getCandidateListSinImagen().getSelectedValue();
        if (candidate == null) return;

        final java.nio.file.Path zipPath = candidate.path;
        final String nombreBase = candidate.nombreBase;

        if (!conImagen) {
            logger.info("[Gallery] Buscando renders para: {}", nombreBase);
            var model = panel.getFilmstripListModel();
            model.clear();
            cancelGalleryWorker();

            // Placeholders STL + worker de renderizado (mismo orden que la lista izquierda)
            try {
                java.util.List<StlEntry> stls = ZipExtractor.listStlContents(zipPath);
                stls.sort(java.util.Comparator.comparingLong(StlEntry::sizeBytes).reversed());
                if (!stls.isEmpty()) {
                    int total = stls.size();
                    for (StlEntry entry : stls) {
                        var ph = crearPlaceholder(entry.filename());
                        var phLayer = new ImageLayer(ph, entry.filename());
                        phLayer.setPlaceholder(true);
                        model.addElement(phLayer);
                    }
                    panel.showGalleryProgress(0, total);

                    var renders = new java.util.ArrayList<java.awt.image.BufferedImage>();

                        galleryWorker = new SwingWorker<Void, Integer>() {
                            private java.nio.file.Path tempDir;

                            @Override
                            protected Void doInBackground() throws Exception {
                                tempDir = ZipExtractor.extractToTemp(zipPath);
                                if (tempDir == null) return null;
                                for (int i = 0; i < total; i++) {
                                    if (isCancelled()) return null;
                                    StlEntry entry = stls.get(i);
                                    try {
                                        java.nio.file.Path stlFile = tempDir.resolve(entry.filename());
                                        if (java.nio.file.Files.exists(stlFile)) {
                                            java.util.List<Triangle> tris = StlParser.parse(stlFile.toFile());
                                            var renderImg = new AwtModelRenderer().renderizar(tris);
                                            synchronized (renders) {
                                                renders.add(renderImg);
                                            }
                                            publish(i);
                                        }
                                    } catch (Exception ex) {
                                        logger.warn("[Gallery] Error renderizando {}: {}", entry.filename(), ex.getMessage());
                                    }
                                }
                                return null;
                            }

                            @Override
                            protected void process(java.util.List<Integer> chunks) {
                                int lastIdx = chunks.get(chunks.size() - 1);
                                synchronized (renders) {
                                    if (lastIdx < renders.size()) {
                                        var img = renders.get(lastIdx);
                                        if (img != null && lastIdx < model.size()) {
                                            model.get(lastIdx).setImage(img);
                                            model.get(lastIdx).setPlaceholder(false);
                                        }
                                    }
                                }
                                panel.getGalleryProgress().setValue(lastIdx + 1);
                            }

                            @Override
                            protected void done() {
                                if (tempDir != null) ZipExtractor.deleteDir(tempDir);
                                panel.hideGalleryProgress();
                                renders.clear();
                            }
                        };
                        galleryWorker.execute();
                    }
                } catch (Exception ex) {
                    logger.warn("[Gallery] Error al extraer STLs: {}", ex.getMessage());
                }
            logger.info("[Gallery] Sin renderizar: {} elementos", model.size());
            return;
        }

        // Pestaña "Con imagen": extraer imágenes del ZIP
        logger.info("[Gallery] Extrayendo imágenes de: {}", zipPath.getFileName());

        final javax.swing.ProgressMonitor monitor = new javax.swing.ProgressMonitor(parentFrame,
                "Extrayendo imágenes del archivo comprimido...",
                zipPath.getFileName().toString(), 0, 100);
        monitor.setMillisToDecideToPopup(500);
        monitor.setMillisToPopup(1000);

        var worker = new SwingWorker<Void, Integer>() {
            private final java.util.List<ImageLayer> extractedLayers = new java.util.ArrayList<>();

            @Override
            protected Void doInBackground() throws Exception {
                publish(5);
                Path tempDir = ZipExtractor.extractToTemp(zipPath);
                if (tempDir == null) return null;
                try {
                    java.util.List<java.nio.file.Path> imageFiles;
                    try (var ws = java.nio.file.Files.walk(tempDir)) {
                        imageFiles = ws
                            .filter(p -> java.nio.file.Files.isRegularFile(p))
                            .filter(p -> {
                                String name = p.getFileName().toString().toLowerCase();
                                return name.endsWith(".png") || name.endsWith(".jpg")
                                    || name.endsWith(".jpeg") || name.endsWith(".gif")
                                    || name.endsWith(".bmp") || name.endsWith(".webp")
                                    || name.endsWith(".tiff") || name.endsWith(".tif");
                            })
                            .collect(java.util.stream.Collectors.toList());
                    }

                    int total = imageFiles.size();
                    for (int i = 0; i < total && !monitor.isCanceled(); i++) {
                        publish(10 + (i * 90 / Math.max(total, 1)));
                        try {
                            var img = javax.imageio.ImageIO.read(imageFiles.get(i).toFile());
                            if (img != null)
                                extractedLayers.add(new ImageLayer(img, tempDir.relativize(imageFiles.get(i)).toString()));
                        } catch (Exception ex) {
                            logger.warn("[Gallery] Error leyendo {}", imageFiles.get(i).getFileName());
                        }
                    }
                    if (extractedLayers.isEmpty()) {
                        for (var entry : ZipExtractor.listStlContents(zipPath)) {
                            if (monitor.isCanceled()) break;
                            var ph = new java.awt.image.BufferedImage(80, 80, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                            var g2d = ph.createGraphics();
                            g2d.setColor(java.awt.Color.DARK_GRAY);
                            g2d.fillRect(0, 0, 80, 80);
                            g2d.setColor(java.awt.Color.WHITE);
                            g2d.drawString("STL", 25, 45);
                            g2d.dispose();
                            extractedLayers.add(new ImageLayer(ph, entry.filename()));
                        }
                    }
                } finally {
                    ZipExtractor.deleteDir(tempDir);
                }
                publish(100);
                return null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                monitor.setProgress(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                monitor.close();
                // Si este worker ya no es el activo, ignorar resultado
                if (galleryWorker != this) return;
                if (monitor.isCanceled()) {
                    panel.clearFilmstrip();
                    return;
                }
                try {
                    get();
                    if (galleryWorker != this) return;
                    var model = panel.getFilmstripListModel();
                    model.clear();
                    extractedLayers.forEach(model::addElement);
                    logger.info("[Gallery] Con imagen: {} elementos", extractedLayers.size());
                } catch (Exception ex) {
                    logger.error("[Gallery] Error extrayendo ZIP", ex);
                }
            }
        };
        galleryWorker = worker;
        worker.execute();
    } // --- Fin del metodo loadGalleryContent ---


    private java.awt.image.BufferedImage crearPlaceholder(String nombre) {
        var ph = new java.awt.image.BufferedImage(80, 80, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var g2d = ph.createGraphics();
        try {
            g2d.setColor(java.awt.Color.DARK_GRAY);
            g2d.fillRect(0, 0, 80, 80);
            g2d.setColor(java.awt.Color.WHITE);
            String label = nombre;
            if (label.length() > 15) label = label.substring(0, 12) + "...";
            g2d.drawString(label, 5, 45);
            g2d.setColor(java.awt.Color.GRAY);
            g2d.drawRect(5, 5, 70, 70);
        } finally {
            g2d.dispose();
        }
        return ph;
    }


    /**
     * Cuenta el número total de entradas (directorios y archivos) de la carpeta
     * según la profundidad de escaneo, para poder mostrar un progreso real en la
     * barra del diálogo.
     *
     * @param root             carpeta raíz del escaneo
     * @param includeSubfolders true para recorrido recursivo completo
     * @return total de entradas visitadas, o 0 si no se puede contar
     */
    private long contarEntradas(Path root, boolean includeSubfolders) {
        int depth = includeSubfolders ? Integer.MAX_VALUE : 1;
        try (Stream<Path> walk = Files.walk(root, depth)) {
            return walk.count();
        } catch (IOException e) {
            logger.warn("[RenderController] No se pudo contar las entradas de {}: {}", root, e.getMessage());
            return 0;
        }
    } // --- Fin del metodo contarEntradas ---


} // --- Fin de la clase RenderController ---
