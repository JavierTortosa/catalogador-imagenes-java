package vista.panels.render;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import modelo.renderer.ImageEntry;
import modelo.renderer.ImageLayer;
import modelo.renderer.StlEntry;
import servicios.renderer.Zip2PngScanner.RenderCandidate;
import vista.components.ThemedToggleButton;
import vista.theme.Tema;
import vista.theme.ThemeChangeListener;
import vista.theme.ThemeManager;

public class RenderPanel extends JPanel implements ThemeChangeListener {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// --- Listas de candidatos (panel izquierdo) ---
    private final DefaultListModel<RenderCandidate> listModelSinImagen;
    private final JList<RenderCandidate> candidateListSinImagen;
    private final DefaultListModel<RenderCandidate> listModelConImagen;
    private final JList<RenderCandidate> candidateListConImagen;
    private final JTabbedPane candidateTabs;
    private final java.util.Set<RenderCandidate> marcadosParaProcesar = new java.util.HashSet<>();
    private final DefaultListModel<StlEntry> contentListModel;
    private final JList<StlEntry> contentList;
    private final DefaultListModel<ImageEntry> contentImageListModel;
    private final JList<ImageEntry> contentImageList;
    private final JPanel bottomCardPanel;
    private static final String CARD_CONTENT_STL = "stl";
    private static final String CARD_CONTENT_IMG = "img";

    // --- Grid de resultados (panel central) ---
    private final JPanel gridCardPanel;
    private final JPanel imagenesGrid;
    private final JPanel rendersGrid;
    private final AdvanceEditPanel advanceEditPanel;
    private final ScannerPanel scannerPanel;
    private boolean scannerActive;
    private static final String CARD_GRID_IMG = "img";
    private static final String CARD_GRID_RENDER = "render";
    private static final String CARD_GRID_ADVANCE_EDIT = "advance_edit";
    private static final String CARD_GRID_SCANNER = "scanner";

    // --- Visor (panel derecho) ---
    private final PreviewPanel3DFX preview3DFX;
    private final JPanel imageDisplayPanel;
    private BufferedImage currentImage2D;
    private double imageZoom = 1.0;
    private boolean collageMode;

    // --- Capas (collage multi-imagen) ---
    private final DefaultListModel<ImageLayer> layersListModel;
    private final JList<ImageLayer> layersList;
    private int selectedLayerIndex = -1;

    private double imageOffsetX = 0;
    private double imageOffsetY = 0;
    private int lastPanX;
    private int lastPanY;
    private final JPanel viewerCardPanel;
    private JSplitPane leftSplit;
    private boolean fullscreen;
    private final JPanel rightPanel;
    private static final String CARD_VISTA_3D = "Vista3D";
    private static final String CARD_VISTA_2D = "Vista2D";

    // --- Controles de imagen ---
    private final JLabel brightnessLabel;
    private final JSlider brightnessSlider;
    private final JTextField brightnessField;
    private final JLabel contrastLabel;
    private final JSlider contrastSlider;
    private final JTextField contrastField;
    private final ThemedToggleButton chkCrosshair;
    private final ThemedToggleButton chkWireframe;
    private final ThemedToggleButton chkAntiAlias;
    private final ThemedToggleButton chkFillLight2;
    private final JComboBox<String> cboCalidad;
    // --- Controles de fondo ---
    private final ButtonGroup bgGroup;
    private final JRadioButton rbSolid;
    private final JRadioButton rbGradient;
    private final JRadioButton rbImage;
    private final JRadioButton rbTransparent;

    private final JPanel solidColorPreview;
    private final JButton btnSolidColor;
    private Color solidBgColor = new Color(60, 60, 65);

    private final JPanel gradientPreview;
    private final JPanel gradientStartPreview;
    private final JPanel gradientEndPreview;
    private final JButton btnGradientStart;
    private final JButton btnGradientEnd;
    private Color gradientStartColor = new Color(45, 45, 50);
    private Color gradientEndColor = new Color(75, 75, 80);

    private final JTextField bgImageField;
    private final JButton btnBrowseImage;
    private final JSlider bgImageScaleSlider;
    private final JLabel bgImageScaleLabel;

    private final JCheckBox chkCheckerboard;

    private final JTabbedPane tabbedPane;
    private JScrollPane layersScroll;
    private final JPanel cardPanel;

    private ThemeManager themeManager;

    private static Color themeColor(String key, int r, int g, int b) {
        Color c = UIManager.getColor(key);
        return c != null ? c : new Color(r, g, b);
    }

    private static Color themeColorLabel() {
        return themeColor("Label.foreground", 255, 255, 255);
    }

    private static Color themeColorDisabled() {
        return themeColor("Label.disabledForeground", 180, 180, 190);
    }

    /**
     * Asigna el ThemeManager y se registra como listener de cambios de tema.
     * Aplica el tema actual a todos los subcomponentes.
     */
    public void setThemeManager(ThemeManager tm) {
        this.themeManager = tm;
        if (tm != null) {
            chkAntiAlias.setThemeManager(tm);
            chkFillLight2.setThemeManager(tm);
            chkCrosshair.setThemeManager(tm);
            chkWireframe.setThemeManager(tm);
            tm.addThemeChangeListener(this);
            applyTheme(tm.getTemaActual());
        }
    } // --- Fin del metodo setThemeManager ---


    @Override
    public void onThemeChanged(Tema tema) {
        applyTheme(tema);
    } // --- Fin del metodo onThemeChanged ---


    private void applyTheme(Tema tema) {
        SwingUtilities.invokeLater(() -> {
            setBackground(tema.colorFondoPrincipal());
            candidateTabs.setBackground(tema.colorFondoPrincipal());
            candidateTabs.setForeground(tema.colorTextoPrimario());
            imagenesGrid.setBackground(tema.colorFondoSecundario());
            rendersGrid.setBackground(tema.colorFondoSecundario());
            rightPanel.setBackground(tema.colorFondoSecundario());
            viewerCardPanel.setBackground(tema.colorFondoSecundario());
            scannerPanel.setBackground(tema.colorFondoSecundario());
            tabbedPane.setBackground(tema.colorFondoPrincipal());
            tabbedPane.setForeground(tema.colorTextoPrimario());
            imageDisplayPanel.setBackground(tema.colorFondoSecundario());

            // Pestaña Imagen
            for (java.awt.Component c : ((JPanel)tabbedPane.getComponentAt(0)).getComponents()) {
                if (c instanceof JPanel p) {
                    p.setBackground(tema.colorFondoSecundario());
                    for (java.awt.Component child : p.getComponents()) {
                        if (child instanceof JLabel) child.setForeground(tema.colorTextoPrimario());
                        if (child instanceof JCheckBox cb) {
                            cb.setBackground(tema.colorFondoSecundario());
                            cb.setForeground(tema.colorTextoPrimario());
                        }
                        if (child instanceof JToggleButton tb) {
                            tb.setBackground(tema.colorFondoSecundario());
                            tb.setForeground(tema.colorTextoPrimario());
                        }
                    }
                }
            }
            chkAntiAlias.setBackground(tema.colorFondoSecundario());
            chkAntiAlias.setForeground(tema.colorTextoPrimario());
            chkFillLight2.setBackground(tema.colorFondoSecundario());
            chkFillLight2.setForeground(tema.colorTextoPrimario());
            chkCrosshair.setBackground(tema.colorFondoSecundario());
            chkCrosshair.setForeground(tema.colorTextoPrimario());
            chkWireframe.setBackground(tema.colorFondoSecundario());
            chkWireframe.setForeground(tema.colorTextoPrimario());

            // Pestaña Fondo
            java.awt.Component fondoComp = tabbedPane.getComponentAt(1);
            if (fondoComp instanceof JPanel fondo) {
                fondo.setBackground(tema.colorFondoSecundario());
                actualizarColoresFondo(fondo, tema);
            }

            // Pestaña Capas
            layersList.setBackground(tema.colorFondoSecundario());
            layersList.setForeground(tema.colorTextoPrimario());

            revalidate();
            repaint();
        });
    } // --- Fin del metodo applyTheme ---


    private void actualizarColoresFondo(JPanel panel, Tema tema) {
        for (java.awt.Component c : panel.getComponents()) {
            if (c instanceof JPanel p) {
                p.setBackground(tema.colorFondoSecundario());
                actualizarColoresFondo(p, tema);
            }
            if (c instanceof JRadioButton rb) {
                rb.setBackground(tema.colorFondoSecundario());
                rb.setForeground(tema.colorTextoPrimario());
            }
            if (c instanceof JCheckBox cb) {
                cb.setBackground(tema.colorFondoSecundario());
                cb.setForeground(tema.colorTextoPrimario());
            }
            if (c instanceof JLabel l) {
                l.setForeground(tema.colorTextoSecundario());
            }
            if (c instanceof JSlider s) {
                s.setBackground(tema.colorFondoPrincipal());
            }
        }
    } // --- Fin del metodo actualizarColoresFondo ---


    // --- Filmstrip (galería de imágenes del ZIP) ---
    private final JPanel filmstripPanel;
    private final JList<ImageLayer> filmstripList;
    private final DefaultListModel<ImageLayer> filmstripListModel;
    private final JProgressBar galleryProgress;
    private boolean filmstripVisible;
    private static final String CARD_SOLID = "solid";
    private static final String CARD_GRADIENT = "gradient";
    private static final String CARD_IMAGE = "image";
    private static final String CARD_TRANSPARENT = "transparent";

    public RenderPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setBackground(themeColor("Panel.background", 35, 35, 40));

        // ---------- PANEL IZQUIERDO: listas de candidatos ----------
        RenderListCellRenderer rendererSin = new RenderListCellRenderer();
        rendererSin.setMarcadoProvider(this::isMarcadoParaProcesar);
        RenderListCellRenderer rendererCon = new RenderListCellRenderer();
        rendererCon.setMarcadoProvider(this::isMarcadoParaProcesar);

        listModelSinImagen = new DefaultListModel<>();
        candidateListSinImagen = new JList<>(listModelSinImagen);
        candidateListSinImagen.setCellRenderer(rendererSin);
        candidateListSinImagen.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane listScrollSin = new JScrollPane(candidateListSinImagen);

        listModelConImagen = new DefaultListModel<>();
        candidateListConImagen = new JList<>(listModelConImagen);
        candidateListConImagen.setCellRenderer(rendererCon);
        candidateListConImagen.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane listScrollCon = new JScrollPane(candidateListConImagen);

        candidateTabs = new JTabbedPane();
        candidateTabs.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        candidateTabs.setForeground(themeColorLabel());
        candidateTabs.addTab("Sin renderizar", listScrollSin);
        candidateTabs.addTab("Con imagen", listScrollCon);

        // --- Contenido del ZIP (STLs) ---
        contentListModel = new DefaultListModel<>();
        contentList = new JList<>(contentListModel);
        contentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane contentScroll = new JScrollPane(contentList);
        contentScroll.setBorder(BorderFactory.createTitledBorder("STLs en el ZIP"));

        // --- Contenido del ZIP (imágenes) ---
        contentImageListModel = new DefaultListModel<>();
        contentImageList = new JList<>(contentImageListModel);
        contentImageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane contentImageScroll = new JScrollPane(contentImageList);
        contentImageScroll.setBorder(BorderFactory.createTitledBorder("Imágenes en el ZIP"));

        // CardLayout: contenido inferior contextual a la pestaña activa
        bottomCardPanel = new JPanel(new CardLayout());
        bottomCardPanel.add(contentScroll, CARD_CONTENT_STL);
        bottomCardPanel.add(contentImageScroll, CARD_CONTENT_IMG);

        leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, candidateTabs, bottomCardPanel);
        leftSplit.setResizeWeight(0.5);
        leftSplit.setDividerLocation(0.5);
        leftSplit.setBorder(null);
        leftSplit.setPreferredSize(new Dimension(280, 0));

        // ---------- PANEL CENTRAL: grid contextual a la pestaña de candidatos ----------
        imagenesGrid = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 6));
        imagenesGrid.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        JScrollPane imagenesScroll = new JScrollPane(imagenesGrid);
        imagenesScroll.setBorder(BorderFactory.createTitledBorder("Imágenes extraídas"));

        rendersGrid = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 6));
        rendersGrid.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        JScrollPane rendersScroll = new JScrollPane(rendersGrid);
        rendersScroll.setBorder(BorderFactory.createTitledBorder("Renders 3D generados"));

        gridCardPanel = new JPanel(new CardLayout());
        gridCardPanel.add(imagenesScroll, CARD_GRID_IMG);
        gridCardPanel.add(rendersScroll, CARD_GRID_RENDER);

        advanceEditPanel = new AdvanceEditPanel();
        gridCardPanel.add(advanceEditPanel, CARD_GRID_ADVANCE_EDIT);

        scannerPanel = new ScannerPanel();
        gridCardPanel.add(scannerPanel, CARD_GRID_SCANNER);

        // ---------- PANEL DERECHO: visor dual + controles ----------
        rightPanel = new JPanel(new BorderLayout(4, 4));
        rightPanel.setBackground(themeColor("TabbedPane.contentAreaColor", 30, 30, 35));
        rightPanel.setPreferredSize(new Dimension(340, 0));

        // CardLayout para conmutar vista 3D / 2D
        viewerCardPanel = new JPanel(new CardLayout());
        viewerCardPanel.setBackground(themeColor("TabbedPane.contentAreaColor", 30, 30, 35));

        preview3DFX = new PreviewPanel3DFX();
        preview3DFX.setBorder(BorderFactory.createTitledBorder("Preview 3D"));
        viewerCardPanel.add(preview3DFX, CARD_VISTA_3D);

        imageDisplayPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int w = getWidth();
                int h = getHeight();

                if (collageMode && layersListModel != null && !layersListModel.isEmpty()) {
                    for (int i = 0; i < layersListModel.size(); i++) {
                        ImageLayer layer = layersListModel.getElementAt(i);
                        if (!layer.isVisible()) continue;

                        BufferedImage img = layer.getImage();
                        double layerZoom = layer.getZoom();
                        double lx = layer.getOffsetX();
                        double ly = layer.getOffsetY();
                        double scale = layerZoom * Math.min((double) w / img.getWidth(), (double) h / img.getHeight());
                        double xOff = (w - img.getWidth() * scale) / 2 + lx;
                        double yOff = (h - img.getHeight() * scale) / 2 + ly;
                        AffineTransform at = AffineTransform.getTranslateInstance(xOff, yOff);
                        at.scale(scale, scale);
                        g2.drawImage(img, at, null);
                    }
                    g2.dispose();
                    return;
                }

                if (currentImage2D == null) {
                    g2.setColor(imageDisplayPanel.getBackground());
                    g2.fillRect(0, 0, w, h);
                    g2.setColor(themeColorDisabled());
                    String msg = "Sin imagen";
                    java.awt.FontMetrics fm = g2.getFontMetrics();
                    int x = (w - fm.stringWidth(msg)) / 2;
                    int y = h / 2;
                    g2.drawString(msg, x, y);
                    g2.dispose();
                    return;
                }

                double imgW = currentImage2D.getWidth();
                double imgH = currentImage2D.getHeight();
                double scale = imageZoom * Math.min((double) w / imgW, (double) h / imgH);
                double xOff = (w - imgW * scale) / 2 + imageOffsetX;
                double yOff = (h - imgH * scale) / 2 + imageOffsetY;
                AffineTransform at = AffineTransform.getTranslateInstance(xOff, yOff);
                at.scale(scale, scale);
                g2.drawImage(currentImage2D, at, null);
                g2.dispose();
            }
        };
        imageDisplayPanel.setBackground(themeColor("TabbedPane.contentAreaColor", 30, 30, 35));
        imageDisplayPanel.setFocusable(true);
        // Zoom con rueda del ratón
        imageDisplayPanel.addMouseWheelListener(e -> {
            if (collageMode) {
                ImageLayer layer = getSelectedLayer();
                if (layer == null) return;
                double oldZoom = layer.getZoom();
                double rot = e.getPreciseWheelRotation();
                double newZoom = rot < 0 ? oldZoom * 1.15 : oldZoom / 1.15;
                newZoom = Math.max(0.05, Math.min(50.0, newZoom));
                layer.setZoom(newZoom);
                imageDisplayPanel.repaint();
                return;
            }
            double oldZoom = imageZoom;
            double rot = e.getPreciseWheelRotation();
            if (rot < 0) {
                imageZoom *= 1.15;
            } else {
                imageZoom /= 1.15;
            }
            imageZoom = Math.max(0.05, Math.min(50.0, imageZoom));
            double factor = imageZoom / oldZoom;
            java.awt.Point mp = e.getPoint();
            double panelW = imageDisplayPanel.getWidth();
            double panelH = imageDisplayPanel.getHeight();
            double imgW = currentImage2D != null ? currentImage2D.getWidth() : 1;
            double imgH = currentImage2D != null ? currentImage2D.getHeight() : 1;
            double scaleBase = Math.min(panelW / imgW, panelH / imgH);
            double mx = (mp.x - (panelW - imgW * oldZoom * scaleBase) / 2 - imageOffsetX) / (oldZoom * scaleBase);
            double my = (mp.y - (panelH - imgH * oldZoom * scaleBase) / 2 - imageOffsetY) / (oldZoom * scaleBase);
            imageOffsetX = mp.x - (panelW - imgW * imageZoom * scaleBase) / 2 - mx * imageZoom * scaleBase;
            imageOffsetY = mp.y - (panelH - imgH * imageZoom * scaleBase) / 2 - my * imageZoom * scaleBase;
            imageDisplayPanel.repaint();
        });
        // Pan con arrastre
        MouseAdapter panAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                lastPanX = e.getX();
                lastPanY = e.getY();
                imageDisplayPanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                imageDisplayPanel.setCursor(Cursor.getDefaultCursor());
            }
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (collageMode) {
                    ImageLayer layer = getSelectedLayer();
                    if (layer == null) return;
                    int dx = e.getX() - lastPanX;
                    int dy = e.getY() - lastPanY;
                    layer.setOffsetX(layer.getOffsetX() + dx);
                    layer.setOffsetY(layer.getOffsetY() + dy);
                    lastPanX = e.getX();
                    lastPanY = e.getY();
                    imageDisplayPanel.repaint();
                    return;
                }
                int dx = e.getX() - lastPanX;
                int dy = e.getY() - lastPanY;
                imageOffsetX += dx;
                imageOffsetY += dy;
                lastPanX = e.getX();
                lastPanY = e.getY();
                imageDisplayPanel.repaint();
            }
        };
        imageDisplayPanel.addMouseListener(panAdapter);
        imageDisplayPanel.addMouseMotionListener(panAdapter);
        JScrollPane imagePreviewScroll = new JScrollPane(imageDisplayPanel);
        imagePreviewScroll.setBorder(BorderFactory.createTitledBorder("Vista previa"));
        viewerCardPanel.add(imagePreviewScroll, CARD_VISTA_2D);

        // --- Filmstrip (galería de imágenes del ZIP) ---
        filmstripListModel = new DefaultListModel<>();
        filmstripList = new JList<>(filmstripListModel);
        filmstripList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        filmstripList.setVisibleRowCount(1);
        filmstripList.setCellRenderer(new DefaultListCellRenderer() {
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = new JLabel();
                if (value instanceof ImageLayer layer && layer.getImage() != null) {
                    java.awt.Image img = layer.getImage().getScaledInstance(80, 80, java.awt.Image.SCALE_SMOOTH);
                    label.setIcon(new javax.swing.ImageIcon(img));
                    label.setToolTipText(layer.getName());
                }
                label.setPreferredSize(new java.awt.Dimension(90, 90));
                label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                if (isSelected) {
                    label.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.YELLOW, 2));
                } else {
                    label.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
                }
                return label;
            }
        });
        filmstripList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ImageLayer sel = filmstripList.getSelectedValue();
                if (sel != null) {
                    firePropertyChange("filmstripSelected", null, sel);
                }
            }
        });
        JScrollPane filmstripScroll = new JScrollPane(filmstripList);
        filmstripScroll.setPreferredSize(new java.awt.Dimension(200, 100));
        filmstripScroll.setBorder(BorderFactory.createTitledBorder("Contenido del ZIP"));
        galleryProgress = new JProgressBar(0, 100);
        galleryProgress.setStringPainted(true);
        galleryProgress.setVisible(false);

        filmstripPanel = new JPanel(new BorderLayout());
        filmstripPanel.add(filmstripScroll, BorderLayout.CENTER);
        filmstripPanel.add(galleryProgress, BorderLayout.SOUTH);
        filmstripPanel.setVisible(false);
        filmstripVisible = false;

        JPanel viewerWrapper = new JPanel(new BorderLayout());
        viewerWrapper.add(viewerCardPanel, BorderLayout.CENTER);
        viewerWrapper.add(filmstripPanel, BorderLayout.SOUTH);
        rightPanel.add(viewerWrapper, BorderLayout.CENTER);

        // --- Pestañas de configuración (Imagen + Fondo) ---
        this.tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(themeColor("Panel.background", 40, 40, 45));
        tabbedPane.setForeground(themeColorLabel());

        // --- Tab "Imagen" ---
        JPanel imagenTab = new JPanel(new BorderLayout(2, 4));
        imagenTab.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));

        JPanel slidersPanel = new JPanel(new GridLayout(0, 1, 2, 2));
        slidersPanel.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        imagenTab.add(slidersPanel, BorderLayout.NORTH);

        brightnessLabel = new JLabel("Brillo");
        brightnessLabel.setForeground(themeColorLabel());
        brightnessLabel.setPreferredSize(new Dimension(70, 20));
        brightnessSlider = new JSlider(-100, 100, 0);
        brightnessField = new JTextField("0", 5);
        slidersPanel.add(buildSliderRow(brightnessLabel, brightnessSlider, brightnessField));

        contrastLabel = new JLabel("Contraste");
        contrastLabel.setForeground(themeColorLabel());
        contrastLabel.setPreferredSize(new Dimension(70, 20));
        contrastSlider = new JSlider(-100, 100, 0);
        contrastField = new JTextField("0", 5);
        slidersPanel.add(buildSliderRow(contrastLabel, contrastSlider, contrastField));

        chkAntiAlias = new ThemedToggleButton(null);
        chkAntiAlias.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        chkAntiAlias.setForeground(themeColorLabel());
        chkAntiAlias.setToolTipText("Activa el antialiasing del preview 3D");

        chkWireframe = new ThemedToggleButton(null);
        chkWireframe.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        chkWireframe.setForeground(themeColorLabel());
        chkWireframe.setToolTipText("Contorno (wireframe)");

        chkFillLight2 = new ThemedToggleButton(null);
        chkFillLight2.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        chkFillLight2.setForeground(themeColorLabel());
        chkFillLight2.setToolTipText("Luz tenue inferior-izquierda para aclarar la zona de sombra");

        chkCrosshair = new ThemedToggleButton(null);
        chkCrosshair.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        chkCrosshair.setForeground(themeColorLabel());
        chkCrosshair.setSelected(true);
        chkCrosshair.setToolTipText("Cruceta (ejes)");

        JLabel calidadLabel = new JLabel("Calidad del render:");
        calidadLabel.setForeground(themeColorLabel());
        cboCalidad = new JComboBox<>(new String[]{"R\u00E1pida", "Normal", "Alta"});
        cboCalidad.setSelectedIndex(1);

        // Mini toolbar inferior: botones de preview (icono + tooltip) y calidad
        JPanel miniToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        miniToolbar.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        miniToolbar.add(chkAntiAlias);
        miniToolbar.add(chkWireframe);
        miniToolbar.add(chkFillLight2);
        miniToolbar.add(crearSeparadorToolbar());
        miniToolbar.add(chkCrosshair);
        miniToolbar.add(crearSeparadorToolbar());
        miniToolbar.add(calidadLabel);
        miniToolbar.add(cboCalidad);

        imagenTab.add(miniToolbar, BorderLayout.SOUTH);

        tabbedPane.addTab("Imagen", imagenTab);

        // --- Tab "Fondo" ---
        JPanel fondoTab = new JPanel(new BorderLayout(4, 4));
        fondoTab.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));

        rbSolid = new JRadioButton("S\u00F3lido");
        rbGradient = new JRadioButton("Degradado");
        rbImage = new JRadioButton("Cargar fondo");
        rbTransparent = new JRadioButton("Fondo transparente");
        for (JRadioButton rb : new JRadioButton[]{rbSolid, rbGradient, rbImage, rbTransparent}) {
            rb.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
            rb.setForeground(themeColorLabel());
        }
        bgGroup = new ButtonGroup();
        bgGroup.add(rbSolid);
        bgGroup.add(rbGradient);
        bgGroup.add(rbImage);
        bgGroup.add(rbTransparent);
        rbSolid.setSelected(true);

        JPanel radioPanel = new JPanel(new GridLayout(2, 2, 4, 2));
        radioPanel.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        radioPanel.add(rbSolid);
        radioPanel.add(rbGradient);
        radioPanel.add(rbImage);
        radioPanel.add(rbTransparent);

        JPanel radioContainer = new JPanel(new BorderLayout());
        radioContainer.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        radioContainer.add(radioPanel, BorderLayout.NORTH);
        radioContainer.add(new javax.swing.JSeparator(), BorderLayout.SOUTH);
        fondoTab.add(radioContainer, BorderLayout.NORTH);

        cardPanel = new JPanel(new CardLayout());
        cardPanel.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));

        // Card: Sólido
        JPanel solidCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        solidCard.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        solidColorPreview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(solidBgColor);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.DARK_GRAY);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        solidColorPreview.setPreferredSize(new Dimension(32, 24));
        btnSolidColor = new JButton();
        btnSolidColor.setPreferredSize(new Dimension(24, 24));
        solidCard.add(new JLabel("Color:"));
        solidCard.add(solidColorPreview);
        solidCard.add(btnSolidColor);
        cardPanel.add(solidCard, CARD_SOLID);

        // Card: Degradado
        JPanel gradientCard = new JPanel(new BorderLayout(4, 4));
        gradientCard.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));

        JPanel gradientTop = new JPanel(new GridLayout(1, 2, 8, 0));
        gradientTop.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));

        JPanel startPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        startPanel.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        gradientStartPreview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(gradientStartColor);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.DARK_GRAY);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        gradientStartPreview.setPreferredSize(new Dimension(32, 24));
        btnGradientStart = new JButton();
        btnGradientStart.setPreferredSize(new Dimension(24, 24));
        startPanel.add(new JLabel("Inicio:"));
        startPanel.add(gradientStartPreview);
        startPanel.add(btnGradientStart);

        JPanel endPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        endPanel.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        gradientEndPreview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(gradientEndColor);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.DARK_GRAY);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        gradientEndPreview.setPreferredSize(new Dimension(32, 24));
        btnGradientEnd = new JButton();
        btnGradientEnd.setPreferredSize(new Dimension(24, 24));
        endPanel.add(new JLabel("Fin:"));
        endPanel.add(gradientEndPreview);
        endPanel.add(btnGradientEnd);

        gradientTop.add(startPanel);
        gradientTop.add(endPanel);

        gradientPreview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, gradientStartColor,
                        getWidth(), 0, gradientEndColor);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        gradientPreview.setPreferredSize(new Dimension(0, 24));

        gradientCard.add(gradientTop, BorderLayout.NORTH);
        gradientCard.add(gradientPreview, BorderLayout.CENTER);
        cardPanel.add(gradientCard, CARD_GRADIENT);

        // Card: Cargar fondo
        JPanel imageCard = new JPanel(new BorderLayout(4, 4));
        imageCard.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        JPanel imageTop = new JPanel(new BorderLayout(4, 4));
        imageTop.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        bgImageField = new JTextField();
        bgImageField.setEditable(false);
        btnBrowseImage = new JButton("...");
        btnBrowseImage.setPreferredSize(new Dimension(28, 22));
        imageTop.add(bgImageField, BorderLayout.CENTER);
        imageTop.add(btnBrowseImage, BorderLayout.EAST);
        JPanel imageScaleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        imageScaleRow.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        bgImageScaleSlider = new JSlider(10, 100, 50);
        bgImageScaleSlider.setBackground(themeColor("Panel.background", 50, 50, 55));
        bgImageScaleLabel = new JLabel("0.5x");
        bgImageScaleLabel.setForeground(themeColorLabel());
        bgImageScaleLabel.setPreferredSize(new Dimension(35, 20));
        imageScaleRow.add(new JLabel("Escala:"));
        imageScaleRow.add(bgImageScaleSlider);
        imageScaleRow.add(bgImageScaleLabel);
        imageCard.add(imageTop, BorderLayout.NORTH);
        imageCard.add(imageScaleRow, BorderLayout.SOUTH);
        cardPanel.add(imageCard, CARD_IMAGE);

        // Card: Transparente
        JPanel transparentCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        transparentCard.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        JLabel lblTransparent = new JLabel("Sin fondo (PNG con canal alfa)");
        lblTransparent.setForeground(themeColorDisabled());
        transparentCard.add(lblTransparent);
        cardPanel.add(transparentCard, CARD_TRANSPARENT);

        fondoTab.add(cardPanel, BorderLayout.CENTER);

        chkCheckerboard = new JCheckBox("Fondo a cuadros (preview)");
        chkCheckerboard.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        chkCheckerboard.setForeground(themeColorLabel());
        JPanel cbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        cbPanel.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        cbPanel.add(chkCheckerboard);
        fondoTab.add(cbPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Fondo", fondoTab);

        // --- Tab "Capas" ---
        layersListModel = new DefaultListModel<>();
        layersList = new JList<>(layersListModel);
        layersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        layersList.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        layersList.setForeground(themeColorLabel());
        layersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedLayerIndex = layersList.getSelectedIndex();
            }
        });
        layersList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int idx = layersList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        ImageLayer layer = layersListModel.getElementAt(idx);
                        layer.setVisible(!layer.isVisible());
                        layersList.repaint();
                        imageDisplayPanel.repaint();
                    }
                }
            }
        });
        this.layersScroll = new JScrollPane(layersList);
        layersScroll.setBorder(BorderFactory.createTitledBorder("Capas"));
        tabbedPane.addTab("Capas", layersScroll);

        rightPanel.add(tabbedPane, BorderLayout.SOUTH);

        // Sincronizar grid con la pestaña inicial: por defecto se muestra el
        // Scanner de Huérfanos (espacio de trabajo) hasta que haya candidatos.
        scannerActive = true;
        showGridCard(CARD_GRID_SCANNER);

        // ---------- ENSAMBLAR PANEL PRINCIPAL ----------
        Color wrapBg = themeColor("TabbedPane.contentAreaColor", 48, 48, 53);
        Color wrapFg = themeColorDisabled();
        Color wrapBorder = themeColor("Component.borderColor", 60, 60, 65);
        add(wrapCollapsible("", leftSplit, true, true, wrapBg, wrapFg, wrapBorder), BorderLayout.WEST);
        add(gridCardPanel, BorderLayout.CENTER);
        add(wrapCollapsible("", rightPanel, true, false, wrapBg, wrapFg, wrapBorder), BorderLayout.EAST);
    }

    private JPanel wrapCollapsible(String title, JComponent content, boolean expanded, boolean leftSide,
                                   Color bgHeader, Color fgTitle, Color borderColor) {

        String expandedArrow = leftSide ? "\u25C0" : "\u25B6";
        String collapsedArrow = leftSide ? "\u25B6" : "\u25C0";

        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(bgHeader);

        int align = leftSide ? FlowLayout.RIGHT : FlowLayout.LEFT;
        JPanel header = new JPanel(new FlowLayout(align, 4, 2));
        header.setBackground(bgHeader);
        header.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, borderColor));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel arrowLbl = new JLabel(expanded ? expandedArrow : collapsedArrow);
        arrowLbl.setFont(new Font("SansSerif", Font.PLAIN, 9));
        arrowLbl.setForeground(fgTitle);

        if (leftSide) {
            // Panel izquierdo: título a la izquierda, flecha a la derecha
            if (title != null && !title.isEmpty()) {
                JLabel titleLbl = new JLabel(title);
                titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 11f));
                titleLbl.setForeground(fgTitle);
                header.add(titleLbl);
            }
            header.add(arrowLbl);
        } else {
            // Panel derecho: flecha a la izquierda, título a la derecha
            header.add(arrowLbl);
            if (title != null && !title.isEmpty()) {
                JLabel titleLbl = new JLabel(title);
                titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 11f));
                titleLbl.setForeground(fgTitle);
                header.add(titleLbl);
            }
        }

        section.add(header, BorderLayout.NORTH);
        section.add(content, BorderLayout.CENTER);

        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                boolean visible = !content.isVisible();
                content.setVisible(visible);
                arrowLbl.setText(visible ? expandedArrow : collapsedArrow);
                section.revalidate();
                Container p = section.getParent();
                if (p != null) {
                    p.revalidate();
                }
            } // ---FIN de metodo mouseClicked---
        });

        return section;
    } // --- Fin del método wrapCollapsible ---

    private JPanel buildSliderRow(JLabel lbl, JSlider slider, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        row.add(lbl, BorderLayout.WEST);
        slider.setBackground(themeColor("Panel.background", 50, 50, 55));
        row.add(slider, BorderLayout.CENTER);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setPreferredSize(new Dimension(40, 20));
        field.setMaximumSize(new Dimension(40, 20));
        field.setFont(field.getFont().deriveFont(11f));
        row.add(field, BorderLayout.EAST);
        return row;
    } // --- Fin del metodo buildSliderRow ---


    /**
     * Crea un separador vertical fino para la mini toolbar de la pestaña Imagen.
     *
     * @return separador vertical con alto y ancho fijos
     */
    private javax.swing.JComponent crearSeparadorToolbar() {
        javax.swing.JSeparator sep = new javax.swing.JSeparator(javax.swing.SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(2, 18));
        sep.setBackground(themeColor("TabbedPane.contentAreaColor", 40, 40, 45));
        return sep;
    } // --- Fin del metodo crearSeparadorToolbar ---

    public void repaintGradientPreview() {
        gradientPreview.repaint();
    }

    public void repaintColorPreviews() {
        solidColorPreview.repaint();
        gradientStartPreview.repaint();
        gradientEndPreview.repaint();
    }

    // --- Getters backward compat (delegan a "Sin imagen") ---
    public DefaultListModel<RenderCandidate> getListModel() { return listModelSinImagen; }
    public JList<RenderCandidate> getCandidateList() { return candidateListSinImagen; }

    // --- Getters duales ---
    public DefaultListModel<RenderCandidate> getListModelSinImagen() { return listModelSinImagen; }
    public JList<RenderCandidate> getCandidateListSinImagen() { return candidateListSinImagen; }
    public DefaultListModel<RenderCandidate> getListModelConImagen() { return listModelConImagen; }
    public JList<RenderCandidate> getCandidateListConImagen() { return candidateListConImagen; }

    /**
     * Comprueba si un candidato está marcado para procesar.
     *
     * @param candidate candidato a consultar
     * @return true si está marcado
     */
    public boolean isMarcadoParaProcesar(RenderCandidate candidate) {
        return marcadosParaProcesar.contains(candidate);
    } // --- Fin del metodo isMarcadoParaProcesar ---


    /**
     * Alterna el marcado de un candidato para procesar.
     *
     * @param candidate candidato cuyo marcado se alterna
     */
    public void toggleMarcadoParaProcesar(RenderCandidate candidate) {
        if (candidate == null) return;
        if (!marcadosParaProcesar.remove(candidate)) {
            marcadosParaProcesar.add(candidate);
        }
    } // --- Fin del metodo toggleMarcadoParaProcesar ---


    /**
     * Candidatos actualmente marcados para procesar.
     *
     * @return copia del conjunto de candidatos marcados
     */
    public List<RenderCandidate> getMarcadosParaProcesar() {
        return new java.util.ArrayList<>(marcadosParaProcesar);
    } // --- Fin del metodo getMarcadosParaProcesar ---


    /**
     * Vacía el conjunto de candidatos marcados para procesar.
     */
    public void limpiarMarcados() {
        marcadosParaProcesar.clear();
    } // --- Fin del metodo limpiarMarcados ---

    public void actualizarTitulosPestanyas() {
        int sinCount = listModelSinImagen.getSize();
        int conCount = listModelConImagen.getSize();
        candidateTabs.setTitleAt(0, "Sin renderizar (" + sinCount + ")");
        candidateTabs.setTitleAt(1, "Con imagen (" + conCount + ")");
    }

    public DefaultListModel<StlEntry> getContentListModel() { return contentListModel; }
    public JList<StlEntry> getContentList() { return contentList; }
    public DefaultListModel<ImageEntry> getContentImageListModel() { return contentImageListModel; }
    public JList<ImageEntry> getContentImageList() { return contentImageList; }

    public JTabbedPane getCandidateTabs() { return candidateTabs; }

    public void selectCandidateTab(int index) {
        candidateTabs.setSelectedIndex(index);
    }

    public void showContentCard(String card) {
        ((CardLayout) bottomCardPanel.getLayout()).show(bottomCardPanel, card);
    }

    public boolean isCandidateTabSinRenderizar() {
        return candidateTabs.getSelectedIndex() == 0;
    }

    public boolean isCandidateTabConImagen() {
        return candidateTabs.getSelectedIndex() == 1;
    }

    public JPanel getImagenesGrid() { return imagenesGrid; }
    public JPanel getRendersGrid() { return rendersGrid; }

    // Backward compat: getThumbnailGrid() devuelve el grid de renders 3D
    public JPanel getThumbnailGrid() { return rendersGrid; }

    public boolean isCollageMode() { return collageMode; }

    public void setCollageMode(boolean collageMode) {
        this.collageMode = collageMode;
        // Mostrar/ocultar la pestaña Capas
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if ("Capas".equals(tabbedPane.getTitleAt(i))) {
                tabbedPane.setEnabledAt(i, collageMode);
                break;
            }
        }
        if (collageMode) {
            show2DView();
            imageDisplayPanel.setBorder(BorderFactory.createTitledBorder("Composición (collage)"));
        } else {
            imageDisplayPanel.setBorder(null);
        }
        imageDisplayPanel.repaint();
    }

    // --- Gestión de capas ---
    public DefaultListModel<ImageLayer> getLayersListModel() { return layersListModel; }
    public JList<ImageLayer> getLayersList() { return layersList; }
    public int getSelectedLayerIndex() { return selectedLayerIndex; }

    public ImageLayer getSelectedLayer() {
        if (selectedLayerIndex >= 0 && selectedLayerIndex < layersListModel.size()) {
            return layersListModel.getElementAt(selectedLayerIndex);
        }
        return null;
    }

    public void addLayer(ImageLayer layer) {
        layersListModel.addElement(layer);
        int idx = layersListModel.size() - 1;
        layersList.setSelectedIndex(idx);
        imageDisplayPanel.repaint();
    }

    public void removeLayer(int index) {
        if (index >= 0 && index < layersListModel.size()) {
            layersListModel.remove(index);
            if (selectedLayerIndex == index) selectedLayerIndex = -1;
            if (selectedLayerIndex > index) selectedLayerIndex--;
            imageDisplayPanel.repaint();
        }
    }

    public void moveLayer(int from, int to) {
        if (from < 0 || from >= layersListModel.size() || to < 0 || to >= layersListModel.size()) return;
        ImageLayer layer = layersListModel.remove(from);
        layersListModel.add(to, layer);
        layersList.setSelectedIndex(to);
        imageDisplayPanel.repaint();
    }

    public void clearLayers() {
        layersListModel.clear();
        selectedLayerIndex = -1;
        imageDisplayPanel.repaint();
    }

    // --- Filmstrip (galería del ZIP) ---
    public boolean isFilmstripVisible() { return filmstripVisible; }
    public DefaultListModel<ImageLayer> getFilmstripListModel() { return filmstripListModel; }
    public JList<ImageLayer> getFilmstripList() { return filmstripList; }

    public void setFilmstripVisible(boolean visible) {
        filmstripVisible = visible;
        filmstripPanel.setVisible(visible);
        revalidate();
        repaint();
    }

    public void clearFilmstrip() {
        filmstripListModel.clear();
    }

    public JProgressBar getGalleryProgress() { return galleryProgress; }

    public void showGalleryProgress(int min, int max) {
        galleryProgress.setMinimum(min);
        galleryProgress.setMaximum(max);
        galleryProgress.setValue(min);
        galleryProgress.setVisible(true);
    }

    public void hideGalleryProgress() {
        galleryProgress.setVisible(false);
    }

    public void showGridCard(String card) {
        ((CardLayout) gridCardPanel.getLayout()).show(gridCardPanel, card);
        gridCardPanel.revalidate();
        gridCardPanel.repaint();
    }

    public void syncGridToCandidateTab() {
        if (scannerActive) {
            showGridCard(CARD_GRID_SCANNER);
            return;
        }
        if (advanceEditPanel.isActive()) {
            showGridCard(CARD_GRID_ADVANCE_EDIT);
            return;
        }
        if (isCandidateTabSinRenderizar()) {
            showGridCard(CARD_GRID_RENDER);
        } else {
            showGridCard(CARD_GRID_IMG);
        }
    }

    public void setScannerActive(boolean active) {
        this.scannerActive = active;
        if (active) {
            showGridCard(CARD_GRID_SCANNER);
        } else {
            syncGridToCandidateTab();
        }
    }

    public boolean isScannerActive() {
        return scannerActive;
    }

    public ScannerPanel getScannerPanel() {
        return scannerPanel;
    }

    public void setAdvanceEditActive(boolean active) {
        advanceEditPanel.setActive(active);
        if (active) {
            showGridCard(CARD_GRID_ADVANCE_EDIT);
        } else {
            syncGridToCandidateTab();
        }
    }

    public boolean isAdvanceEditActive() {
        return advanceEditPanel.isActive();
    }

    public AdvanceEditPanel getAdvanceEditPanel() {
        return advanceEditPanel;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setEditorFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        Container leftWrapper = leftSplit != null ? leftSplit.getParent() : null;
        Container rightWrapper = rightPanel != null ? rightPanel.getParent() : null;
        if (leftWrapper instanceof JComponent) leftWrapper.setVisible(!fullscreen);
        if (rightWrapper instanceof JComponent) rightWrapper.setVisible(!fullscreen);
        revalidate();
        repaint();
    } // --- Fin del metodo setEditorFullscreen ---

    public void toggleEditorFullscreen() {
        setEditorFullscreen(!fullscreen);
    } // --- Fin del metodo toggleEditorFullscreen ---

    public PreviewPanel3DFX getPreview3DFX() { return preview3DFX; }
    public JPanel getImageDisplayPanel() { return imageDisplayPanel; }
    public JPanel getGridCardPanel() { return gridCardPanel; }
    public JPanel getRightPanel() { return rightPanel; }
    public JPanel getViewerCardPanel() { return viewerCardPanel; }

    // --- Visor dual ---
    public void show3DView() {
        ((CardLayout) viewerCardPanel.getLayout()).show(viewerCardPanel, CARD_VISTA_3D);
    }

    public void show2DView() {
        ((CardLayout) viewerCardPanel.getLayout()).show(viewerCardPanel, CARD_VISTA_2D);
    }

    public boolean isShowing3DView() {
        return viewerCardPanel.getComponents().length > 0
                && preview3DFX.isShowing();
    }

    public boolean isShowing2DView() {
        return currentImage2D != null && imageDisplayPanel.isShowing();
    }

    public void set2DImage(BufferedImage image) {
        this.currentImage2D = image;
        resetImageZoom();
        imageDisplayPanel.repaint();
    }

    public BufferedImage getCurrentImage2D() {
        return currentImage2D;
    }

    public void clearViewer2D() {
        this.currentImage2D = null;
        resetImageZoom();
        imageDisplayPanel.repaint();
    }

    public double getImageZoom() { return imageZoom; }
    public double getImageOffsetX() { return imageOffsetX; }
    public double getImageOffsetY() { return imageOffsetY; }

    public void resetImageZoom() {
        imageZoom = 1.0;
        imageOffsetX = 0;
        imageOffsetY = 0;
    }

    public BufferedImage capturarVistaActual() {
        int w = imageDisplayPanel.getWidth();
        int h = imageDisplayPanel.getHeight();
        if (w < 1 || h < 1 || currentImage2D == null) return null;
        BufferedImage capture = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = capture.createGraphics();
        imageDisplayPanel.paint(g2);
        g2.dispose();
        return capture;
    }

    public int getBrightness() {
        try { return Integer.parseInt(brightnessField.getText().trim()); }
        catch (NumberFormatException e) { return brightnessSlider.getValue(); }
    }

    public int getContrast() {
        try { return Integer.parseInt(contrastField.getText().trim()); }
        catch (NumberFormatException e) { return contrastSlider.getValue(); }
    }

    public boolean isCheckerboard() { return chkCheckerboard.isSelected(); }
    public boolean isAntiAlias() { return chkAntiAlias.isSelected(); }
    public boolean isCrosshair() { return chkCrosshair.isSelected(); }

    public JSlider getBrightnessSlider() { return brightnessSlider; }
    public JSlider getContrastSlider() { return contrastSlider; }
    public JTextField getBrightnessField() { return brightnessField; }
    public JTextField getContrastField() { return contrastField; }
    public JCheckBox getChkCheckerboard() { return chkCheckerboard; }
    public JToggleButton getChkAntiAlias() { return chkAntiAlias; }
    public JToggleButton getChkCrosshair() { return chkCrosshair; }
    public JToggleButton getChkWireframe() { return chkWireframe; }
    public JToggleButton getChkFillLight2() { return chkFillLight2; }
    public JComboBox<String> getCboCalidad() { return cboCalidad; }

    // --- Getters fondo ---
    public ButtonGroup getBgGroup() { return bgGroup; }
    public JRadioButton getRbSolid() { return rbSolid; }
    public JRadioButton getRbGradient() { return rbGradient; }
    public JRadioButton getRbImage() { return rbImage; }
    public JRadioButton getRbTransparent() { return rbTransparent; }

    public Color getSolidBgColor() { return solidBgColor; }
    public void setSolidBgColor(Color c) { this.solidBgColor = c; solidColorPreview.repaint(); }
    public Color getGradientStartColor() { return gradientStartColor; }
    public void setGradientStartColor(Color c) { this.gradientStartColor = c; gradientStartPreview.repaint(); gradientPreview.repaint(); }
    public Color getGradientEndColor() { return gradientEndColor; }
    public void setGradientEndColor(Color c) { this.gradientEndColor = c; gradientEndPreview.repaint(); gradientPreview.repaint(); }

    public JPanel getSolidColorPreview() { return solidColorPreview; }
    public JButton getBtnSolidColor() { return btnSolidColor; }
    public JButton getBtnGradientStart() { return btnGradientStart; }
    public JButton getBtnGradientEnd() { return btnGradientEnd; }
    public JPanel getGradientPreview() { return gradientPreview; }

    public JTextField getBgImageField() { return bgImageField; }
    public JButton getBtnBrowseImage() { return btnBrowseImage; }
    public JSlider getBgImageScaleSlider() { return bgImageScaleSlider; }
    public JLabel getBgImageScaleLabel() { return bgImageScaleLabel; }

    public String getSelectedBgMode() {
        if (rbSolid.isSelected()) return CARD_SOLID;
        if (rbGradient.isSelected()) return CARD_GRADIENT;
        if (rbImage.isSelected()) return CARD_IMAGE;
        return CARD_TRANSPARENT;
    }

    public void showBgCard(String card) {
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, card);
    }

    public void setColorPickerIcons(javax.swing.Icon icon) {
        btnSolidColor.setIcon(icon);
        btnGradientStart.setIcon(icon);
        btnGradientEnd.setIcon(icon);
    }

    public void setBrightnessIcon(javax.swing.Icon icon) { brightnessLabel.setIcon(icon); }
    public void setContrastIcon(javax.swing.Icon icon) { contrastLabel.setIcon(icon); }
    public void setAntiAliasIcon(javax.swing.Icon icon) { chkAntiAlias.setIcon(icon); }
    public void setFillLightIcon(javax.swing.Icon icon) { chkFillLight2.setIcon(icon); }
    public void setCrosshairIcon(javax.swing.Icon icon) { chkCrosshair.setIcon(icon); }
    public void setWireframeIcon(javax.swing.Icon icon) { chkWireframe.setIcon(icon); }

} // --- Fin de la clase RenderPanel ---
