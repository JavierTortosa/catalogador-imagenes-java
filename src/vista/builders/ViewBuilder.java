package vista.builders;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;
import java.util.Objects;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.RenderController;
import controlador.managers.ToolbarManager;
import controlador.managers.interfaces.IProjectManager;
import controlador.managers.tree.FolderTreeManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.image.ThumbnailService;
import vista.VisorView;
import vista.panels.GridDisplayPanel;
import vista.panels.ImageDisplayPanel;
import vista.panels.PolaroidDisplayPanel;
import vista.renderers.MiniaturaListCellRenderer;
import vista.renderers.NombreArchivoRenderer;
import vista.theme.ThemeManager;
import vista.util.IconUtils;
import vista.util.ThumbnailPreviewer; // <<< AÑADIR IMPORT

/**
 * Construye y ensambla los componentes visuales de la ventana principal
 * y sus subpaneles (visor, miniaturas, barras de herramientas, etc.).
 * Centraliza la creacion de la UI delegando en builders especializados.
 */
public class ViewBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ViewBuilder.class);

    // --- Dependencias Clave (Lista Simplificada) ---
    private final ComponentRegistry registry;
    private final VisorModel model;
    private final ThemeManager themeManager;
    private final ConfigurationManager configuration;
    private final IconUtils iconUtils;
    private final IProjectManager projectManager;

    private final ThumbnailService thumbnailService;
    private final ThumbnailService gridThumbnailService;

    private ProjectBuilder projectBuilder;
    private ClientBuilder clientBuilder;
    private DataBuilder dataBuilder;

    private Map<String, Action> actionMap;
    private ToolbarManager toolbarManager;

    private FolderTreeManager folderTreeManager;
    private RenderController renderController;

    private Map<controlador.managers.filter.FilterCriterion.Logic, javax.swing.Icon> logicIcons;
    private Map<controlador.managers.filter.FilterCriterion.SourceType, javax.swing.Icon> typeIcons;
    private javax.swing.Icon deleteIcon;

    // --- Paneles de visualización compartidos (una única instancia para todos los WorkModes) ---
    private JPanel sharedDisplayModesContainer;
    private ImageDisplayPanel sharedSingleImagePanel;
    private GridDisplayPanel sharedGridPanel;
    private PolaroidDisplayPanel sharedPolaroidPanel;

    public ViewBuilder(
            ComponentRegistry registry,
            VisorModel model,
            ThemeManager themeManager,
            ConfigurationManager configuration,
            IconUtils iconUtils,
            ThumbnailService thumbnailService,
            ThumbnailService gridThumbnailService,
            IProjectManager projectManager,
            ProjectBuilder projectBuilder,
            ClientBuilder clientBuilder) {

        logger.info("[ViewBuilder] Iniciando...");

        this.registry = registry;
        this.model = model;
        this.themeManager = themeManager;
        this.configuration = configuration;
        this.iconUtils = iconUtils;
        this.projectManager = projectManager;
        this.projectBuilder = projectBuilder;
        this.clientBuilder = clientBuilder;
        this.thumbnailService = Objects.requireNonNull(thumbnailService, "thumbnailService (global) no puede ser null");
        this.gridThumbnailService = Objects.requireNonNull(gridThumbnailService,
                "gridThumbnailService no puede ser null");

    } // --- Fin del método ViewBuilder (constructor) ---

    public void setToolbarManager(ToolbarManager toolbarManager) {
        this.toolbarManager = Objects.requireNonNull(toolbarManager,
                "ToolbarManager no puede ser null en ViewBuilder.");
    } // --- Fin del método setToolbarManager ---

    /**
     * Crea los paneles de visualización compartidos (Single, Grid, Polaroid)
     * que serán reutilizados por todos los WorkModes mediante reparenting.
     * Se registran con claves ESTÁNDAR (no per-mode).
     */
    private void buildSharedDisplayPanels() {
        logger.debug("  [ViewBuilder] Creando paneles de visualización compartidos...");

        // 1. Crear el contenedor CardLayout compartido
        sharedDisplayModesContainer = new JPanel(new CardLayout());
        registry.register("container.displaymodes", sharedDisplayModesContainer);

        // 2. ImageDisplayPanel (Vista única)
        sharedSingleImagePanel = new ImageDisplayPanel(this.themeManager, this.model);
        if (this.projectManager != null) {
            sharedSingleImagePanel.setProjectManager(this.projectManager);
        }
        registry.register("panel.display.imagen", sharedSingleImagePanel);
        registry.register("label.imagenPrincipal", sharedSingleImagePanel.getInternalLabel(), "WHEEL_NAVIGABLE");

        // 3. GridDisplayPanel (Parrilla)
        ThumbnailPreviewer gridPreviewer = new ThumbnailPreviewer(null, model, themeManager, null, registry);
        sharedGridPanel = new GridDisplayPanel(this.model, this.gridThumbnailService, this.themeManager,
                this.iconUtils, gridPreviewer, this.registry);
        if (this.projectManager != null) {
            sharedGridPanel.setProjectManager(this.projectManager);
        }
        registry.register("panel.display.grid", sharedGridPanel);
        registry.register("scroll.grid.visualizador", sharedGridPanel.getScrollPane());
        JList<String> gridList = sharedGridPanel.getGridList();
        registry.register("list.grid", gridList, "WHEEL_NAVIGABLE", "GRID_NAVIGABLE");

        // 4. PolaroidDisplayPanel
        sharedPolaroidPanel = new PolaroidDisplayPanel(this.themeManager, this.model);
        registry.register("panel.display.polaroid", sharedPolaroidPanel);
        registry.register("panel.display.polaroid.image", sharedPolaroidPanel.getImagePanel());
        registry.register("label.polaroid.imagen", sharedPolaroidPanel.getInternalLabel(), "WHEEL_NAVIGABLE");

        // 5. Añadir al CardLayout
        sharedDisplayModesContainer.add(sharedSingleImagePanel, "VISTA_SINGLE_IMAGE");
        sharedDisplayModesContainer.add(sharedGridPanel, "VISTA_GRID");
        sharedDisplayModesContainer.add(sharedPolaroidPanel, "VISTA_POLAROID");

        logger.debug("  [ViewBuilder] Paneles compartidos creados y registrados.");
    } // --- FIN de metodo buildSharedDisplayPanels ---

    /**
     * Crea el marco principal de la aplicación, configurando su estructura general
     * con un CardLayout para los diferentes modos de trabajo (Visualizador,
     * Proyecto, etc.).
     * 
     * @return La instancia de VisorView (JFrame) completamente ensamblada.
     */
    public VisorView createMainFrame() {
        logger.debug("  [ViewBuilder] Iniciando la construcción del frame principal con estructura CardLayout...");

        VisorView mainFrame = new VisorView(
                100, // Altura inicial del panel de miniaturas, considera hacerla configurable
                this.model,
                this.thumbnailService,
                this.themeManager,
                this.configuration,
                this.registry,
                this.iconUtils,
                this.projectManager);
        registry.register("frame.main", mainFrame);
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // === ESTABLECER ICONO Y TÍTULO ===
        mainFrame.setTitle("ModelTag - Your visual STL manager");

        // Asumimos que has llamado a tu archivo de icono "modeltag-icon.png"
        // y lo has puesto en resources/iconos/comunes/
        ImageIcon appIcon = iconUtils.getAppIcon("application/modeltag icono.png");
        if (appIcon != null) {
            mainFrame.setIconImage(appIcon.getImage());
        } else {
            logger.warn("[ViewBuilder] No se pudo cargar el icono de la aplicación.");
        }

        mainFrame.setLayout(new BorderLayout());

        JPanel toolbarContainer = createToolbarContainer();
        JPanel topInfoPanel = createTopInfoPanel();
        JPanel northWrapper = new JPanel(new BorderLayout());

        registry.register("panel.north.wrapper", northWrapper);

        northWrapper.add(toolbarContainer, BorderLayout.CENTER);
        northWrapper.add(topInfoPanel, BorderLayout.SOUTH);

        mainFrame.add(northWrapper, BorderLayout.NORTH);

        // === BARRA LATERAL VERTICAL DE MODOS ===
        JToolBar modeToolbar = this.toolbarManager.getToolbar("modo");
        modeToolbar.setOrientation(JToolBar.VERTICAL);
        modeToolbar.setFloatable(false);
        modeToolbar.setBorder(null);
        for (Component comp : modeToolbar.getComponents()) {
            if (comp instanceof AbstractButton) {
                ((AbstractButton) comp).setMargin(new java.awt.Insets(12, 4, 12, 4));
            }
        }
        modeToolbar.revalidate();

        JToolBar bottomToolbar = this.toolbarManager.getToolbar("modo_bottom");
        bottomToolbar.setOrientation(JToolBar.VERTICAL);
        bottomToolbar.setFloatable(false);
        bottomToolbar.setBorder(null);
        for (Component comp : bottomToolbar.getComponents()) {
            if (comp instanceof AbstractButton) {
                ((AbstractButton) comp).setMargin(new java.awt.Insets(12, 4, 12, 4));
            }
        }
        bottomToolbar.revalidate();

        JPanel sidebarPanel = new JPanel(new BorderLayout());
        sidebarPanel.setBackground(modeToolbar.getBackground());
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, java.awt.Color.GRAY));
        sidebarPanel.add(modeToolbar, BorderLayout.NORTH);
        sidebarPanel.add(Box.createVerticalGlue(), BorderLayout.CENTER);
        sidebarPanel.add(bottomToolbar, BorderLayout.SOUTH);
        mainFrame.add(sidebarPanel, BorderLayout.WEST);
        // ===========================================
        
        // === BARRA LATERAL VERTICAL DERECHA (EAST Toolbars) ===
        JPanel eastSidebarPanel = new JPanel();
        eastSidebarPanel.setLayout(new BoxLayout(eastSidebarPanel, BoxLayout.Y_AXIS));
        eastSidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, java.awt.Color.GRAY));
        registry.register("container.toolbars.east", eastSidebarPanel);
        mainFrame.add(eastSidebarPanel, BorderLayout.EAST);
        // ===========================================

        JPanel bottomStatusBar = createBottomStatusBar();
        mainFrame.add(bottomStatusBar, BorderLayout.SOUTH);

        // --- Crear paneles de visualización COMPARTIDOS (una única instancia) ---
        buildSharedDisplayPanels();

        // --- CardLayout para MODOS DE TRABAJO (WorkModes) ---
        JPanel workModesContainer = new JPanel(new CardLayout());
        registry.register("container.workmodes", workModesContainer);

        // Panel para el WorkMode VISUALIZADOR
        JPanel visualizerWorkModePanel = createVisualizerWorkModePanel(mainFrame);
        workModesContainer.add(visualizerWorkModePanel, "VISTA_VISUALIZADOR");

        registry.register("panel.workmode.visualizador", visualizerWorkModePanel);

        // Panel para el WorkMode PROYECTO
        JPanel projectWorkModePanel = this.projectBuilder.buildProjectViewPanel();
        workModesContainer.add(projectWorkModePanel, "VISTA_PROYECTOS");
        registry.register("panel.workmode.proyectos", projectWorkModePanel);

        // Panel para el WorkMode CARROUSEL
        // 1. El panel principal para la tarjeta del Carrusel sigue usando BorderLayout.
        JPanel carouselWorkModePanel = new JPanel(new BorderLayout());
        registry.register("panel.workmode.carousel", carouselWorkModePanel);

        // 2. ***** INICIO DE LA CORRECCIÓN ESTRUCTURAL *****
        // Creamos un panel "envoltorio" para la zona central. Este es el truco clave.
        // Este panel aislará el JLayeredPane del JScrollPane de las miniaturas.
        JPanel centerWrapperPanel = new JPanel(new BorderLayout());
        centerWrapperPanel.setOpaque(false); // Es transparente para que se vea el fondo de la app (checkered).

        // 3. Creamos el JLayeredPane. Ahora será hijo del 'centerWrapperPanel'.
        javax.swing.JLayeredPane layeredPane = new javax.swing.JLayeredPane();
        layeredPane.setOpaque(false); // También es transparente por la misma razón.

        // 4. Creamos el panel de la imagen.
        ImageDisplayPanel carouselDisplayPanel = new ImageDisplayPanel(this.themeManager, this.model);
        // ¡IMPORTANTE! El ImageDisplayPanel DEBE ser NO-OPACO para que el fondo
        // checkered se vea a través de él.
        carouselDisplayPanel.setOpaque(false);
        registry.register("panel.display.carousel", carouselDisplayPanel);
        registry.register("label.carousel.imagen", carouselDisplayPanel.getInternalLabel(), "WHEEL_NAVIGABLE");

        // 5. Creamos la etiqueta del temporizador (sin cambios).
        JLabel carouselTimerOverlayLabel = new JLabel("--:--", SwingConstants.CENTER);
        carouselTimerOverlayLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
        carouselTimerOverlayLabel.setForeground(java.awt.Color.WHITE);
        carouselTimerOverlayLabel.setBackground(new java.awt.Color(0, 0, 0, 128));
        carouselTimerOverlayLabel.setOpaque(true);
        carouselTimerOverlayLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        carouselTimerOverlayLabel.setVisible(false);
        registry.register("label.carousel.timer.overlay", carouselTimerOverlayLabel);

        // 5b. Creamos un JLabel para el indicador de estado (play/pause).
        JLabel statusIndicatorLabel = new JLabel();
        statusIndicatorLabel.setSize(32, 32); // Le damos un tamaño fijo
        statusIndicatorLabel.setOpaque(false);
        statusIndicatorLabel.setVisible(false); // Inicialmente oculto
        registry.register("label.carousel.status.indicator", statusIndicatorLabel);

        // 6. Añadimos la imagen y el timer al JLayeredPane (sin cambios).
        layeredPane.add(carouselDisplayPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(carouselTimerOverlayLabel, javax.swing.JLayeredPane.PALETTE_LAYER);
        layeredPane.add(statusIndicatorLabel, javax.swing.JLayeredPane.POPUP_LAYER);

        // 7. Añadimos el listener al JLayeredPane para posicionar a sus hijos (sin
        // cambios).
        layeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                carouselDisplayPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                Dimension labelSize = carouselTimerOverlayLabel.getPreferredSize();
                int padding = 15;
                carouselTimerOverlayLabel.setBounds(
                        layeredPane.getWidth() - labelSize.width - padding,
                        layeredPane.getHeight() - labelSize.height - padding,
                        labelSize.width,
                        labelSize.height);

                statusIndicatorLabel.setLocation(
                        layeredPane.getWidth() - statusIndicatorLabel.getWidth() - padding,
                        padding);
            }
        });

        // 8. Añadimos el JLayeredPane al CENTRO de nuestro panel "envoltorio".
        centerWrapperPanel.add(layeredPane, BorderLayout.CENTER);

        // 9. ***** FIN DE LA CORRECCIÓN ESTRUCTURAL *****
        // Ahora, añadimos el 'centerWrapperPanel' (NO el layeredPane) al centro del
        // panel principal del carrusel.
        carouselWorkModePanel.add(centerWrapperPanel, BorderLayout.CENTER);

        // 10. La tira de miniaturas se añade al SUR del panel principal, como antes.
        JScrollPane carouselThumbnailScrollPane = createThumbnailScrollPane("list.miniaturas.carousel",
                "scroll.miniaturas.carousel");
        carouselWorkModePanel.add(carouselThumbnailScrollPane, BorderLayout.SOUTH);

        // 11. Finalmente, añadimos el panel del carrusel completo a la "tarjeta" del
        // CardLayout.
        workModesContainer.add(carouselWorkModePanel, "VISTA_CARROUSEL_WORKMODE");

        // Paneles para otros WorkModes futuros
        JPanel dataWorkModePanel;
        if (this.dataBuilder != null) {
            dataWorkModePanel = this.dataBuilder.buildDataModePanel();
        } else {
            // Fallback si el DataBuilder no fue inyectado
            dataWorkModePanel = new JPanel();
            dataWorkModePanel.add(new JLabel("Error: DataBuilder no inicializado."));
            registry.register("panel.workmode.datos", dataWorkModePanel);
        }
        workModesContainer.add(dataWorkModePanel, "VISTA_DATOS");

        
        // Panel para el WorkMode CLIENTE
        JPanel clientWorkModePanel = this.clientBuilder.buildClientViewPanel();
        workModesContainer.add(clientWorkModePanel, "VISTA_CLIENTE");
        registry.register("panel.workmode.cliente", clientWorkModePanel);


        // Panel para el WorkMode RENDER
        vista.panels.render.RenderPanel renderWorkModePanel = new vista.panels.render.RenderPanel();
        renderWorkModePanel.setThemeManager(this.themeManager);
        renderWorkModePanel.setName("VISTA_RENDER");
        workModesContainer.add(renderWorkModePanel, "VISTA_RENDER");
        registry.register("panel.workmode.render", renderWorkModePanel);
        registry.register("panel.render.grid", renderWorkModePanel.getGridCardPanel());
        registry.register("panel.render.right", renderWorkModePanel.getRightPanel());
        registry.register("panel.render.viewer", renderWorkModePanel.getViewerCardPanel());
        registry.register("panel.render.layers.toolbar", renderWorkModePanel.getLayersToolbarContainer());
        this.renderController = new controlador.RenderController(renderWorkModePanel, configuration, mainFrame);
        this.renderController.setRegistry(this.registry);
        javax.swing.Icon paletteIcon = iconUtils.getScaledCommonIcon("paint-palette--streamline-core.png", 16, 16);
        if (paletteIcon != null) {
            renderWorkModePanel.setColorPickerIcons(paletteIcon);
        }
        javax.swing.Icon brightnessIcon = iconUtils.getScaledCommonIcon("brightness.png", 16, 16);
        javax.swing.Icon contrastIcon = iconUtils.getScaledCommonIcon("contrast.png", 16, 16);
        javax.swing.Icon antiAliasIcon = iconUtils.getScaledIcon("antialiasing.png", 16, 16);
        javax.swing.Icon fillLightIcon = iconUtils.getScaledIcon("luz-de-relleno.png", 16, 16);
        javax.swing.Icon crosshairIcon = iconUtils.getScaledIcon("crosshair.png", 16, 16);
        javax.swing.Icon wireframeIcon = iconUtils.getScaledIcon("contorno.png", 16, 16);
        javax.swing.Icon moveIcon = iconUtils.getScaledIcon("move-preview.png", 16, 16);
        javax.swing.Icon rotateIcon = iconUtils.getScaledIcon("rotate-preview.png", 16, 16);
        if (brightnessIcon != null) renderWorkModePanel.setBrightnessIcon(brightnessIcon);
        if (contrastIcon != null) renderWorkModePanel.setContrastIcon(contrastIcon);
        if (antiAliasIcon != null) renderWorkModePanel.setAntiAliasIcon(antiAliasIcon);
        if (fillLightIcon != null) renderWorkModePanel.setFillLightIcon(fillLightIcon);
        if (crosshairIcon != null) renderWorkModePanel.setCrosshairIcon(crosshairIcon);
        if (wireframeIcon != null) renderWorkModePanel.setWireframeIcon(wireframeIcon);
        if (moveIcon != null) renderWorkModePanel.setMoveIcon(moveIcon);
        if (rotateIcon != null) renderWorkModePanel.setRotateIcon(rotateIcon);

        // Asignar el CardLayout de WorkModes al centro del mainFrame
        mainFrame.add(workModesContainer, BorderLayout.CENTER);

        int x = configuration.getInt(ConfigKeys.WINDOW_X, -1);
        int y = configuration.getInt(ConfigKeys.WINDOW_Y, -1);
        int w = configuration.getInt(ConfigKeys.WINDOW_WIDTH, 1280);
        int h = configuration.getInt(ConfigKeys.WINDOW_HEIGHT, 720);
        if (w > 50 && h > 50 && x != -1 && y != -1) {
            mainFrame.setBounds(x, y, w, h);
        } else {
            mainFrame.setSize(w, h);
            mainFrame.setLocationRelativeTo(null);
        }

        boolean wasMaximized = this.configuration.getBoolean(ConfigKeys.WINDOW_MAXIMIZED, false);
        if (wasMaximized) {
            SwingUtilities.invokeLater(() -> mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH));
        }

        SwingUtilities.invokeLater(() -> {
            JSplitPane splitPane = registry.get("splitpane.main");
            if (splitPane != null) {
                double dividerLocation = configuration.getDouble(ConfigKeys.SPLITPANE_MAIN_DIVIDER_LOCATION, 0.25);
                splitPane.setDividerLocation(dividerLocation);
            }
        });

        logger.debug("  [ViewBuilder] Frame principal construido y ensamblado.");
        return mainFrame;
    } // --- Fin del método createMainFrame ---

    /**
     * Crea el panel principal para el WorkMode VISUALIZADOR.
     * Este panel contendrá su propio CardLayout para los diferentes DisplayModes
     * (SINGLE_IMAGE, GRID, POLAROID).
     * 
     * @return El JPanel configurado para el WorkMode Visualizador.
     */
    private JPanel createVisualizerWorkModePanel(VisorView mainFrame) {
        JPanel visualizerPanel = new JPanel(new BorderLayout()); // Panel para contener toda la UI del visualizador.

        // --- SplitPane para la lista de archivos y el área de visualización ---
        JSplitPane mainSplitPane = createMainSplitPane(mainFrame); // Este ya contiene panel.izquierdo.listaArchivos y
                                                                   // panel.derecho.visor
        visualizerPanel.add(mainSplitPane, BorderLayout.CENTER); // Añadir el split pane al centro del visualizerPanel.

        // --- Panel de miniaturas inferior ---
        JScrollPane thumbnailScrollPane = createThumbnailScrollPane("list.miniaturas", "scroll.miniaturas");

        visualizerPanel.add(thumbnailScrollPane, BorderLayout.SOUTH);

        // --- CardLayout para los DisplayModes DENTRO del panel de visualizador ---
        // Este CardLayout se ubica en el "panel.derecho.visor" (el lado derecho del
        // SplitPane)
        // en lugar de directamente en el visualizerPanel.
        // Entonces, createRightSplitComponent necesita ser modificado para tener su
        // propio CardLayout.
        // Pero para simplificar el cambio por ahora, vamos a hacer que el
        // ImageDisplayPanel
        // sea la "tarjeta" por defecto de SINGLE_IMAGE, y el CardLayout sea en
        // createRightSplitComponent.

        // La lógica del ImageDisplayPanel y sus controles de fondo ya están en
        // createRightSplitComponent.
        // En lugar de que createRightSplitComponent devuelva ImageDisplayPanel,
        // hará que ImageDisplayPanel sea una de las "tarjetas" dentro de él.

        // NOTA: EL CardLayout para los DisplayModes NO VA AQUI, sino DENTRO de
        // createRightSplitComponent
        // (el lado derecho del split pane). El 'visualizerPanel' es el contenedor
        // general del modo.

        return visualizerPanel;

    } // Fin del metodo createVisualizerWorkModePanel

    private JPanel createToolbarContainer() {

        // 1. El contenedor principal ahora usa BorderLayout.
        JPanel mainToolbarContainer = new JPanel(new BorderLayout());
        mainToolbarContainer.setOpaque(true);
        // Registramos el contenedor principal, como antes.
        registry.register("container.toolbars", mainToolbarContainer);

        // 2. Creamos tres sub-paneles, uno para cada alineamiento.
        // Cada uno usa un FlowLayout para que las toolbars dentro de él fluyan.

        // Panel para las barras alineadas a la izquierda.
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        leftPanel.setOpaque(true);

        registry.register("container.toolbars.left", leftPanel); // Lo registramos

        // Panel para las barras alineadas al centro.
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 2));
        centerPanel.setOpaque(true);
        registry.register("container.toolbars.center", centerPanel); // Lo registramos

        // Panel para las barras alineadas a la derecha.
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        rightPanel.setOpaque(true);
        registry.register("container.toolbars.right", rightPanel); // Lo registramos

        // 3. Añadimos los tres sub-paneles al contenedor principal en sus respectivas
        // zonas.
        mainToolbarContainer.add(leftPanel, BorderLayout.WEST);
        mainToolbarContainer.add(centerPanel, BorderLayout.CENTER);
        mainToolbarContainer.add(rightPanel, BorderLayout.EAST);

        logger.debug("  [ViewBuilder] Toolbar container creado con estructura BorderLayout (WEST, CENTER, EAST).");

        // Devolvemos el contenedor principal.
        return mainToolbarContainer;

    } // --- Fin del método createToolbarContainer ---

    private JScrollPane createThumbnailScrollPane(String listRegistryKey, String scrollRegistryKey) {
        // Usa el modelo de miniaturas compartido que se le inyecta al builder
        JList<String> thumbnailList = new JList<>();

        registry.register(listRegistryKey, thumbnailList, "WHEEL_NAVIGABLE");

        MiniaturaListCellRenderer renderer = new MiniaturaListCellRenderer(
                this.thumbnailService,
                this.model,
                this.projectManager,
                this.themeManager,
                this.iconUtils,
                configuration.getInt(ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, 40),
                configuration.getInt(ConfigKeys.MINIATURAS_TAMANO_NORM_ALTO, 40),
                configuration.getBoolean(ConfigKeys.VISTA_MOSTRAR_NOMBRES_MINIATURAS_STATE, true));
        thumbnailList.setCellRenderer(renderer);
        thumbnailList.setFixedCellWidth(renderer.getAnchoCalculadaDeCelda());
        thumbnailList.setFixedCellHeight(renderer.getAlturaCalculadaDeCelda());
        thumbnailList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        thumbnailList.setVisibleRowCount(-1);
        thumbnailList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JScrollPane scrollPane = new JScrollPane(thumbnailList);

        TitledBorder border = BorderFactory.createTitledBorder("Miniaturas");
        scrollPane.setBorder(border);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        registry.register(scrollRegistryKey, scrollPane, "WHEEL_NAVIGABLE");

        return scrollPane;

    } // --- Fin del método createThumbnailScrollPane (con parámetros) ---

    private JSplitPane createMainSplitPane(VisorView mainFrame) {
        // AQUÍ ESTÁ EL CAMBIO: Pasamos 'mainFrame' al método que crea el panel
        // izquierdo
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftSplitComponent(mainFrame),
                createRightSplitComponent());
        splitPane.setResizeWeight(0.25);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        registry.register("splitpane.main", splitPane);

        return splitPane;
    } // --- Fin del método createMainSplitPane ---

    private JPanel createLeftSplitComponent(VisorView mainFrame) {
        // --- 1. Creación de los paneles individuales (sin cambios) ---

        // Panel para la Pestaña "Lista"
        JPanel panelListaArchivos = new JPanel(new BorderLayout());
        TitledBorder borderLista = BorderFactory.createTitledBorder("Archivos: 0");
        panelListaArchivos.setBorder(borderLista);
        registry.register("panel.izquierdo.listaArchivos", panelListaArchivos);

        mainFrame.setPanelListaArchivos(panelListaArchivos);
        mainFrame.setBorderListaArchivos(borderLista);

        JList<String> fileList = new JList<>(model.getModeloLista());
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileList.setCellRenderer(new NombreArchivoRenderer(themeManager, model));
        registry.register("list.nombresArchivo", fileList, "WHEEL_NAVIGABLE");

        JScrollPane scrollPaneLista = new JScrollPane(fileList);
        registry.register("scroll.nombresArchivo", scrollPaneLista);
        panelListaArchivos.add(scrollPaneLista, BorderLayout.CENTER);

        JToolBar ordenToolbar = toolbarManager.getToolbar("botonesOrdenLista");
        if (ordenToolbar != null) {
            ordenToolbar.setFloatable(false);
            ordenToolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            panelListaArchivos.add(ordenToolbar, BorderLayout.NORTH);
        }

        // Panel para la Pestaña "Carpetas"
        JPanel panelArbol = null;
        if (this.folderTreeManager != null) {
            panelArbol = this.folderTreeManager.crearPanelDelArbol();
            registry.register("panel.izquierdo.arbol", panelArbol);
            registry.register("tree.carpetas", this.folderTreeManager.getTree());
        } else {
            logger.error("[ViewBuilder] FolderTreeManager es nulo. No se puede construir la pestaña de carpetas.");
            panelArbol = new JPanel();
            panelArbol.add(new JLabel("Error al crear vista de árbol"));
        }

        // Panel para la zona de Filtros (que ahora irá abajo)
        JPanel panelFiltros = new JPanel(new BorderLayout());
        TitledBorder borderFiltros = BorderFactory.createTitledBorder("Filtros Activos");
        panelFiltros.setBorder(borderFiltros);
        registry.register("panel.izquierdo.filtros", panelFiltros);

        // Guardamos las referencias en VisorView para poder actualizar el título
        // dinámicamente
        mainFrame.setPanelFiltrosActivos(panelFiltros);
        mainFrame.setBorderFiltrosActivos(borderFiltros);

        JList<controlador.managers.filter.FilterCriterion> filterList = new JList<>();

        // --- Carga de iconos y nuevo renderer ---
        if (this.logicIcons != null && this.typeIcons != null && this.deleteIcon != null) {
            filterList.setCellRenderer(
                    new vista.renderers.FilterCriterionCellRenderer(this.logicIcons, this.typeIcons, this.deleteIcon));
            filterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        } else {
            logger.error("¡Iconos para el renderer de filtros no fueron inyectados en ViewBuilder!");
        }

        filterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        registry.register("list.filtrosActivos", filterList, "WHEEL_NAVIGABLE");

        JScrollPane scrollPaneFiltros = new JScrollPane(filterList);
        registry.register("scroll.filtrosActivos", scrollPaneFiltros);
        panelFiltros.add(scrollPaneFiltros, BorderLayout.CENTER);

        JToolBar filterToolbar = toolbarManager.getToolbar("barra_filtros");
        if (filterToolbar != null) {
            filterToolbar.setFloatable(false);
            // Le damos un borde para que no quede pegado a los bordes del panel
            filterToolbar.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            panelFiltros.add(filterToolbar, BorderLayout.NORTH);
        }

        // --- 2. Reestructuración de la Interfaz ---

        // El JTabbedPane ahora solo contendrá las pestañas superiores.
        javax.swing.JTabbedPane tabbedPaneSuperior = new javax.swing.JTabbedPane();
        registry.register("tabbedpane.izquierdo", tabbedPaneSuperior); // Mantenemos la clave por compatibilidad

        tabbedPaneSuperior.addTab("Lista", panelListaArchivos);
        tabbedPaneSuperior.addTab("Carpetas", panelArbol);

        // Creamos el JSplitPane vertical que será el nuevo layout principal
        JSplitPane splitPaneIzquierdoVertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPaneIzquierdoVertical.setTopComponent(tabbedPaneSuperior); // Arriba van las pestañas
        splitPaneIzquierdoVertical.setBottomComponent(panelFiltros); // Abajo va el panel de filtros
        splitPaneIzquierdoVertical.setResizeWeight(0.75); // Da el 75% del espacio a la parte superior
        splitPaneIzquierdoVertical.setContinuousLayout(true);
        splitPaneIzquierdoVertical.setBorder(null);
        registry.register("splitpane.izquierdo.vertical", splitPaneIzquierdoVertical);

        // --- 3. Ensamblaje Final ---

        JPanel panelIzquierdoContenedor = new JPanel(new BorderLayout());

        // El contenedor principal ahora alberga el JSplitPane en lugar del JTabbedPane
        panelIzquierdoContenedor.add(splitPaneIzquierdoVertical, BorderLayout.CENTER);

        registry.register("panel.izquierdo.contenedorPrincipal", panelIzquierdoContenedor);

        return panelIzquierdoContenedor;
    } // ---FIN de metodo createLeftSplitComponent---

    private JPanel createRightSplitComponent() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        registry.register("panel.derecho.visor", rightPanel);
        rightPanel.setOpaque(false);

        // --- Configurar el panel SINGLE_IMAGE compartido (flechas, foco) ---
        if (this.actionMap != null && this.iconUtils != null) {
            javax.swing.Action prevAction = this.actionMap.get(controlador.commands.AppActionCommands.CMD_NAV_ANTERIOR);
            javax.swing.Action nextAction = this.actionMap
                    .get(controlador.commands.AppActionCommands.CMD_NAV_SIGUIENTE);
            javax.swing.Icon prevIcon = this.iconUtils.getScaledIcon("1002-anterior_48x48.png", 48, 48);
            javax.swing.Icon nextIcon = this.iconUtils.getScaledIcon("1003-siguiente_48x48.png", 48, 48);
            sharedSingleImagePanel.setNavigationActions(prevAction, nextAction, prevIcon, nextIcon);
            sharedSingleImagePanel.setNavigationArrowsVisible(
                this.configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_MOSTRAR_FLECHAS, true));
        }

        sharedSingleImagePanel.setFocusable(true);
        java.awt.event.MouseAdapter focusRequester = new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                sharedSingleImagePanel.requestFocusInWindow();
            }
        };
        sharedSingleImagePanel.addMouseListener(focusRequester);
        sharedSingleImagePanel.getInternalLabel().addMouseListener(focusRequester);

        // --- Configurar el GRID compartido (toolbar de tamaño) ---
        if (this.toolbarManager != null) {
            JToolBar tamanoToolbar = this.toolbarManager.getToolbar("barra_grid_tamano");
            if (tamanoToolbar != null) {
                java.util.List<JToolBar> toolbarsParaGrid = new java.util.ArrayList<>();
                toolbarsParaGrid.add(tamanoToolbar);
                sharedGridPanel.setToolbars(toolbarsParaGrid);
            }
        }

        // --- Configurar el POLAROID compartido (flechas, foco) ---
        ImageDisplayPanel polaroidImagePanel = sharedPolaroidPanel.getImagePanel();
        if (this.actionMap != null && this.iconUtils != null) {
            javax.swing.Action prevAction = this.actionMap.get(controlador.commands.AppActionCommands.CMD_NAV_ANTERIOR);
            javax.swing.Action nextAction = this.actionMap
                    .get(controlador.commands.AppActionCommands.CMD_NAV_SIGUIENTE);
            javax.swing.Icon prevIcon = this.iconUtils.getScaledIcon("1002-anterior_48x48.png", 48, 48);
            javax.swing.Icon nextIcon = this.iconUtils.getScaledIcon("1003-siguiente_48x48.png", 48, 48);
            sharedPolaroidPanel.setNavigationActions(prevAction, nextAction, prevIcon, nextIcon);
            sharedPolaroidPanel.setNavigationArrowsVisible(
                this.configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_MOSTRAR_FLECHAS, true));
        }
        polaroidImagePanel.setFocusable(true);
        java.awt.event.MouseAdapter polaroidFocusRequester = new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                polaroidImagePanel.requestFocusInWindow();
            }
        };
        polaroidImagePanel.addMouseListener(polaroidFocusRequester);
        polaroidImagePanel.getInternalLabel().addMouseListener(polaroidFocusRequester);

        // --- Placeholder para el contenedor compartido (soporte para reparenting) ---
        JPanel displayPlaceholder = new JPanel(new BorderLayout());
        registry.register("placeholder.display.visualizador", displayPlaceholder);
        displayPlaceholder.add(sharedDisplayModesContainer, BorderLayout.CENTER);

        rightPanel.add(displayPlaceholder, BorderLayout.CENTER);

        JToolBar imageControlsToolbar = createBackgroundControlPanel();
        if (imageControlsToolbar != null) {
            rightPanel.add(imageControlsToolbar, BorderLayout.SOUTH);
        }

        return rightPanel;
    } // --- Fin del método createRightSplitComponent ---

    public JToolBar createBackgroundControlPanel() {
        if (this.toolbarManager == null) {
            logger.error("ERROR [ViewBuilder.createBackgroundControlPanel]: ToolbarManager es nulo.");
            return new JToolBar();
        }

        JToolBar imageControlsToolbar = toolbarManager.getToolbar("controles_imagen_inferior");

        if (imageControlsToolbar == null) {
            logger.warn(
                    "WARN [ViewBuilder...]: La toolbar 'controles_imagen_inferior' no se encontró. Se devolverá una barra vacía.");
            return new JToolBar();
        }

        imageControlsToolbar.setLayout(new FlowLayout(FlowLayout.RIGHT, 3, 1));

        return imageControlsToolbar;
    } // --- Fin del método createBackgroundControlPanel ---

    /**
     * Crea la barra de estado inferior completa de la aplicación.
     * Esta barra contiene la ruta del archivo, los controles de estado (ahora en
     * una JToolBar)
     * y el área de mensajes.
     *
     * @return Un JPanel que representa la barra de estado inferior.
     */
    private JPanel createBottomStatusBar() {
        // 1. Panel principal de la barra de estado, usa BorderLayout.
        JPanel bottomStatusBar = new JPanel(new BorderLayout(5, 0));
        registry.register("panel.estado.inferior", bottomStatusBar);
        bottomStatusBar.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // --- INICIO DE LA MODIFICACIÓN 3 ---
        // Aplicamos el color personalizado al crearlo.
        bottomStatusBar.setBackground(UIManager.getColor(ThemeManager.KEY_STATUSBAR_BACKGROUND));

        // con este set opaque se elimina el problema del color de la toolbar
        bottomStatusBar.setOpaque(true);

        // --- FIN DE LA MODIFICACIÓN 3 ---

        // 2. Componente Izquierdo: Etiqueta para la carpeta raíz seleccionada.
        javax.swing.JTextField carpetaRaizTextField = new javax.swing.JTextField("Carpeta: (ninguna)");
        carpetaRaizTextField.setEditable(false);
        carpetaRaizTextField.setOpaque(false);
        carpetaRaizTextField.setBorder(null);
        carpetaRaizTextField.setFont(UIManager.getFont("Label.font"));
        carpetaRaizTextField.setForeground(UIManager.getColor("Label.foreground"));
        registry.register("textfield.estado.carpetaRaiz", carpetaRaizTextField);

        // Contenedor para la etiqueta de ruta y ayuda
        JPanel panelRuta = new JPanel(new GridBagLayout());
        panelRuta.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        panelRuta.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.6;
        gbc.gridx = 0;
        panelRuta.add(carpetaRaizTextField, gbc);

        // Etiqueta de ayuda para el usuario (hints, sugerencias)
        JLabel ayudaLabel = new JLabel(" ");
        ayudaLabel.setHorizontalAlignment(SwingConstants.CENTER);
        ayudaLabel.setFont(ayudaLabel.getFont().deriveFont(java.awt.Font.ITALIC));
        ayudaLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        registry.register("label.estado.ayuda", ayudaLabel);

        gbc.weightx = 0.4;
        gbc.gridx = 1;
        panelRuta.add(ayudaLabel, gbc);

        bottomStatusBar.add(panelRuta, BorderLayout.CENTER);

        // 3. Componente Derecho: Un contenedor que a su vez tendrá los controles y los
        // mensajes.
        JPanel panelDerechoContenedor = new JPanel(new BorderLayout(5, 0));
        panelDerechoContenedor.setOpaque(false);

        // 3a. Sub-componente Central (dentro del derecho): La nueva JToolBar de
        // controles.
        // Le pedimos al ToolbarManager la barra de herramientas que hemos definido.
        JToolBar statusBarControlsToolbar = toolbarManager.getToolbar("barra_estado_controles");

        // Verificamos que la toolbar se haya creado correctamente.
        if (statusBarControlsToolbar != null) {

            // Le decimos a FlatLaf que NO pinte el fondo de esta JToolBar específica.
            // Al poner 'background' a 'null', se vuelve transparente.
            statusBarControlsToolbar.putClientProperty("FlatLaf.style", "background: null");

            // Aunque FlatLaf usa la propiedad de arriba, es buena práctica mantener
            // estas llamadas para la consistencia de Swing.
            statusBarControlsToolbar.setOpaque(false);
            statusBarControlsToolbar.setBorder(null); // Quitar el borde es clave para la integración.

            // Configuramos su apariencia para que no parezca una toolbar flotante
            // tradicional.
            statusBarControlsToolbar.setFloatable(false);
            statusBarControlsToolbar.setRollover(false);

            // Creamos el panel que contendrá la toolbar, para poder registrarlo.
            JPanel panelControlesInferior = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            panelControlesInferior.setOpaque(false);
            panelControlesInferior.add(statusBarControlsToolbar);
            registry.register("panel.estado.controles", panelControlesInferior);

            // Añadimos nuestro panel con la toolbar al contenedor derecho.
            panelDerechoContenedor.add(panelControlesInferior, BorderLayout.CENTER);

        } else {
            logger.error(
                    "CRITICAL ERROR [ViewBuilder]: La toolbar 'barra_estado_controles' no se pudo crear o encontrar.");
        }

        // 3b. Sub-componente Derecho (dentro del derecho): El área de mensajes y
        // temporizador.
        // Esta parte no cambia respecto a tu código original.
        JPanel eastStatusBarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        eastStatusBarPanel.setOpaque(false);

        // Etiqueta para el temporizador del carrusel.
        JLabel carouselTimerLabel = new JLabel("--:--");
        carouselTimerLabel.setVisible(false);
        carouselTimerLabel.setToolTipText("Tiempo para la siguiente imagen");
        carouselTimerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        carouselTimerLabel.setPreferredSize(new java.awt.Dimension(60, carouselTimerLabel.getPreferredSize().height));
        registry.register("label.estado.carouselTimer", carouselTimerLabel);

        // Etiqueta para los mensajes de la aplicación.
        JLabel mensajesAppLabel = new JLabel(" ");
        registry.register("label.estado.mensajes", mensajesAppLabel);
        mensajesAppLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mensajesAppLabel.setPreferredSize(new java.awt.Dimension(150, mensajesAppLabel.getPreferredSize().height));

        // Añadir las etiquetas a su panel.

        eastStatusBarPanel.add(carouselTimerLabel);
        eastStatusBarPanel.add(mensajesAppLabel);

        // Añadir el panel de mensajes al contenedor derecho.
        panelDerechoContenedor.add(eastStatusBarPanel, BorderLayout.EAST);

        // 4. Añadir el contenedor derecho completo a la barra de estado principal.
        bottomStatusBar.add(panelDerechoContenedor, BorderLayout.EAST);

        // 5. Devolver la barra de estado completamente ensamblada.

        return bottomStatusBar;

    } // --- Fin del método createBottomStatusBar ---

    private JToggleButton createStatusBarToggleButton(String command, String iconName, int iconSize,
            Map<String, Action> actionMap) {
        JToggleButton button = new JToggleButton();

        button.putClientProperty("JButton.buttonType", "regular");

        // 1. Configurar apariencia base
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setMargin(new Insets(2, 2, 2, 2));
        button.setPreferredSize(new Dimension(26, 26));
        button.setText("");

        // 2. Asignar la Action
        Action action = actionMap.get(command);
        if (action != null) {
            button.setAction(action);
            button.setText(""); // Asegurar que no hay texto
        }

        // 3. Asignar el icono explícitamente
        ImageIcon icon = iconUtils.getScaledIcon(iconName, iconSize, iconSize);
        if (icon != null) {
            button.setIcon(icon);
        } else {
            button.setText("?"); // Marcador si el icono falla
        }

        return button;
    } // --- Fin del método createStatusBarToggleButton ---

    private JPanel createTopInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        // --- INICIO DE LA MODIFICACIÓN 4 ---
        // Aplicamos el color personalizado al crearlo.
        panel.setBackground(UIManager.getColor(ThemeManager.KEY_STATUSBAR_BACKGROUND));
        // --- FIN DE LA MODIFICACIÓN 4 ---

        panel.setOpaque(true);
        registry.register("panel.info.superior", panel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.insets = new Insets(0, 3, 0, 3);

        // La creación de JLabels se mantiene, pero eliminamos la asignación de color de
        // texto.
        javax.swing.JTextField rutaImagenTextField = new javax.swing.JTextField("Ruta: N/A");
        rutaImagenTextField.setEditable(false);
        rutaImagenTextField.setOpaque(false);
        rutaImagenTextField.setBorder(null);
        rutaImagenTextField.setFont(UIManager.getFont("Label.font"));
        rutaImagenTextField.setForeground(UIManager.getColor("Label.foreground"));
        rutaImagenTextField.setPreferredSize(new Dimension(400, 20));
        registry.register("textfield.info.rutaImagen", rutaImagenTextField);

        JLabel nombreArchivoInfoLabel = new JLabel("Archivo: N/A");
        nombreArchivoInfoLabel.setOpaque(false);
        nombreArchivoInfoLabel.setPreferredSize(new Dimension(300, 20));
        registry.register("label.info.nombreArchivo", nombreArchivoInfoLabel);

        JLabel indiceTotalInfoLabel = new JLabel("Idx: N/A");
        indiceTotalInfoLabel.setOpaque(false);
        indiceTotalInfoLabel.setPreferredSize(new Dimension(70, 20));
        registry.register("label.info.indiceTotal", indiceTotalInfoLabel);

        JLabel dimensionesOriginalesInfoLabel = new JLabel("Dim: N/A");
        dimensionesOriginalesInfoLabel.setOpaque(false);
        dimensionesOriginalesInfoLabel.setPreferredSize(new Dimension(100, 20));
        registry.register("label.info.dimensiones", dimensionesOriginalesInfoLabel);

        JLabel tamanoArchivoInfoLabel = new JLabel("Tam: N/A");
        tamanoArchivoInfoLabel.setOpaque(false);
        tamanoArchivoInfoLabel.setPreferredSize(new Dimension(80, 20));
        registry.register("label.info.tamano", tamanoArchivoInfoLabel);

        JLabel fechaArchivoInfoLabel = new JLabel("Fch: N/A");
        fechaArchivoInfoLabel.setOpaque(false);
        fechaArchivoInfoLabel.setPreferredSize(new Dimension(110, 20));
        registry.register("label.info.fecha", fechaArchivoInfoLabel);

        JLabel modoZoomNombreInfoLabel = new JLabel("Modo: N/A");
        modoZoomNombreInfoLabel.setOpaque(false);
        modoZoomNombreInfoLabel.setPreferredSize(new Dimension(150, 20));
        registry.register("label.info.modoZoom", modoZoomNombreInfoLabel);

        JLabel porcentajeZoomVisualRealInfoLabel = new JLabel("%Z: N/A");
        porcentajeZoomVisualRealInfoLabel.setOpaque(false);
        porcentajeZoomVisualRealInfoLabel.setPreferredSize(new Dimension(60, 20));
        registry.register("label.info.porcentajeZoom", porcentajeZoomVisualRealInfoLabel);

        JLabel formatoImagenInfoLabel = new JLabel("Fmt: N/A");
        formatoImagenInfoLabel.setOpaque(false);
        formatoImagenInfoLabel.setPreferredSize(new Dimension(60, 20));
        registry.register("label.info.formatoImagen", formatoImagenInfoLabel);
        formatoImagenInfoLabel.setToolTipText("Formato del archivo de imagen actual");

        // GridBagLayout
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(rutaImagenTextField, gbc);

        gbc.gridx = 1;
        panel.add(new JSeparator(SwingConstants.VERTICAL), gbc);

        gbc.gridx = 2;
        panel.add(nombreArchivoInfoLabel, gbc);

        gbc.gridx = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;

        gbc.gridx = 4;
        panel.add(new JSeparator(SwingConstants.VERTICAL), gbc);
        gbc.gridx = 5;
        panel.add(dimensionesOriginalesInfoLabel, gbc);
        gbc.gridx = 6;
        panel.add(new JSeparator(SwingConstants.VERTICAL), gbc);
        gbc.gridx = 7;
        panel.add(tamanoArchivoInfoLabel, gbc);
        gbc.gridx = 8;
        panel.add(new JSeparator(SwingConstants.VERTICAL), gbc);
        gbc.gridx = 9;
        panel.add(fechaArchivoInfoLabel, gbc);

        gbc.gridx = 10;
        gbc.insets = new Insets(0, 8, 0, 8);
        panel.add(new JSeparator(SwingConstants.VERTICAL), gbc);
        gbc.insets = new Insets(0, 3, 0, 3);

        gbc.gridx = 11;
        panel.add(indiceTotalInfoLabel, gbc);
        gbc.gridx = 12;
        panel.add(new JSeparator(SwingConstants.VERTICAL), gbc);
        gbc.gridx = 13;
        panel.add(porcentajeZoomVisualRealInfoLabel, gbc);
        gbc.gridx = 14;
        panel.add(new JSeparator(SwingConstants.VERTICAL), gbc);
        gbc.gridx = 15;
        panel.add(modoZoomNombreInfoLabel, gbc);
        gbc.gridx = 16;
        panel.add(new JSeparator(SwingConstants.VERTICAL), gbc);
        gbc.gridx = 17;
        panel.add(formatoImagenInfoLabel, gbc);

        return panel;
    } // --- Fin del método createTopInfoPanel ---

    public void setActionMap(Map<String, Action> actionMap) {
        this.actionMap = Objects.requireNonNull(actionMap, "ActionMap no puede ser nulo en ViewBuilder");
    } // ---FIN de metodo setActionMap---

    public void setFolderTreeManager(FolderTreeManager folderTreeManager) {
        this.folderTreeManager = Objects.requireNonNull(folderTreeManager,
                "FolderTreeManager no puede ser null en ViewBuilder.");
    } // --- Fin del método setFolderTreeManager ---

    public void setProjectBuilder(ProjectBuilder projectBuilder) {
        this.projectBuilder = Objects.requireNonNull(projectBuilder, "ProjectBuilder no puede ser null en ViewBuilder");
    }

    public void setFilterRendererIcons(
            Map<controlador.managers.filter.FilterCriterion.Logic, javax.swing.Icon> logicIcons,
            Map<controlador.managers.filter.FilterCriterion.SourceType, javax.swing.Icon> typeIcons,
            javax.swing.Icon deleteIcon) {

        this.logicIcons = logicIcons;
        this.typeIcons = typeIcons;
        this.deleteIcon = deleteIcon;

    } // ---FIN de metodo setFilterRendererIcons---

    @FunctionalInterface
    interface FilterInteractionListener {
        void onFilterClicked(java.awt.event.MouseEvent e, controlador.managers.filter.FilterCriterion criterion,
                int index);
    }

    public void setDataBuilder(DataBuilder dataBuilder) {
        this.dataBuilder = dataBuilder;
    } // ---FIN de metodo [setDataBuilder]---

    public void setClientBuilder(ClientBuilder clientBuilder) {
        this.clientBuilder = Objects.requireNonNull(clientBuilder, "ClientBuilder no puede ser null en ViewBuilder");
    } // ---FIN de metodo [setClientBuilder]---

    public RenderController getRenderController() {
        return renderController;
    } // ---FIN de metodo [getRenderController]---

} // --- FIN de la clase ViewBuilder ---