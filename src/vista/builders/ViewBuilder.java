package vista.builders;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
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

import controlador.managers.ToolbarManager;
import controlador.managers.tree.FolderTreeManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.image.ThumbnailService;
import vista.VisorView;
import vista.panels.GridDisplayPanel;
import vista.panels.ImageDisplayPanel;
import vista.renderers.MiniaturaListCellRenderer;
import vista.renderers.NombreArchivoRenderer;
import vista.theme.ThemeManager;
import vista.util.IconUtils;
import vista.util.ThumbnailPreviewer; // <<< AÑADIR IMPORT

public class ViewBuilder{
	
	private static final Logger logger = LoggerFactory.getLogger(ViewBuilder.class);
	
	// --- Dependencias Clave (Lista Simplificada) ---
    private final ComponentRegistry registry;
    private final VisorModel model;
    private final ThemeManager themeManager;
    private final ConfigurationManager configuration;
    private final IconUtils iconUtils;
    
    private final ThumbnailService thumbnailService;
    private final ThumbnailService gridThumbnailService;
    
    private ProjectBuilder projectBuilder;
    
    private Map<String, Action> actionMap;
    private ToolbarManager toolbarManager;

    private FolderTreeManager folderTreeManager;
    
    private Map<controlador.managers.filter.FilterCriterion.Logic, javax.swing.Icon> logicIcons;
    private Map<controlador.managers.filter.FilterCriterion.SourceType, javax.swing.Icon> typeIcons;
    private javax.swing.Icon deleteIcon;
    
    
    /**
     * Constructor modificado para aceptar AMBOS servicios de miniaturas.
     * @param registry
     * @param model
     * @param themeManager
     * @param configuration
     * @param iconUtils
     * @param thumbnailService Servicio GLOBAL para la barra de miniaturas.
     * @param gridThumbnailService Servicio DEDICADO para el GridDisplayPanel.
     * @param projectBuilder
     */
    public ViewBuilder(
            ComponentRegistry registry,
            VisorModel model,
            ThemeManager themeManager,
            ConfigurationManager configuration,
            IconUtils iconUtils,
            ThumbnailService thumbnailService,
            ThumbnailService gridThumbnailService,
            ProjectBuilder projectBuilder
        ){
    	
    	logger.info ("[ViewBuilder] Iniciando...");
    	
        this.registry = registry;
        this.model = model;
        this.themeManager = themeManager;
        this.configuration = configuration;
        this.iconUtils = iconUtils;
        this.thumbnailService = Objects.requireNonNull(thumbnailService, "thumbnailService (global) no puede ser null");
        this.gridThumbnailService = Objects.requireNonNull(gridThumbnailService, "gridThumbnailService no puede ser null");
        
    } // --- Fin del método ViewBuilder (constructor) ---


    public void setToolbarManager(ToolbarManager toolbarManager) {
        this.toolbarManager = Objects.requireNonNull(toolbarManager, "ToolbarManager no puede ser null en ViewBuilder.");
    } // --- Fin del método setToolbarManager ---


    /**
     * Crea el marco principal de la aplicación, configurando su estructura general
     * con un CardLayout para los diferentes modos de trabajo (Visualizador, Proyecto, etc.).
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
            this.iconUtils
        );
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
        
        JPanel bottomStatusBar = createBottomStatusBar();
        mainFrame.add(bottomStatusBar, BorderLayout.SOUTH);
        
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
        // ¡IMPORTANTE! El ImageDisplayPanel DEBE ser NO-OPACO para que el fondo checkered se vea a través de él.
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

        // 7. Añadimos el listener al JLayeredPane para posicionar a sus hijos (sin cambios).
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
                    labelSize.height
                );
                
                statusIndicatorLabel.setLocation(
                    layeredPane.getWidth() - statusIndicatorLabel.getWidth() - padding,
                    padding
                );
            }
        });

        // 8. Añadimos el JLayeredPane al CENTRO de nuestro panel "envoltorio".
        centerWrapperPanel.add(layeredPane, BorderLayout.CENTER);

        // 9. ***** FIN DE LA CORRECCIÓN ESTRUCTURAL *****
        // Ahora, añadimos el 'centerWrapperPanel' (NO el layeredPane) al centro del panel principal del carrusel.
        carouselWorkModePanel.add(centerWrapperPanel, BorderLayout.CENTER);

        // 10. La tira de miniaturas se añade al SUR del panel principal, como antes.
        JScrollPane carouselThumbnailScrollPane = createThumbnailScrollPane("list.miniaturas.carousel", "scroll.miniaturas.carousel");
        carouselWorkModePanel.add(carouselThumbnailScrollPane, BorderLayout.SOUTH);

        // 11. Finalmente, añadimos el panel del carrusel completo a la "tarjeta" del CardLayout.
        workModesContainer.add(carouselWorkModePanel, "VISTA_CARROUSEL_WORKMODE");
        
        // Paneles para otros WorkModes futuros
        JPanel dataWorkModePanel = new JPanel();
        dataWorkModePanel.add(new JLabel("Modo Datos en desarrollo..."));
        workModesContainer.add(dataWorkModePanel, "VISTA_DATOS");
        registry.register("panel.workmode.datos", dataWorkModePanel);

        JPanel editionWorkModePanel = new JPanel();
        editionWorkModePanel.add(new JLabel("Modo Edición en desarrollo..."));
        workModesContainer.add(editionWorkModePanel, "VISTA_EDICION");
        registry.register("panel.workmode.edicion", editionWorkModePanel);

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
                double dividerLocation = configuration.getDouble("ui.splitpane.main.dividerLocation", 0.25);
                splitPane.setDividerLocation(dividerLocation);
            }
        });

        logger.debug("  [ViewBuilder] Frame principal construido y ensamblado.");
        return mainFrame;
    }  // --- Fin del método createMainFrame ---
    
    
    /**
     * Crea el panel principal para el WorkMode VISUALIZADOR.
     * Este panel contendrá su propio CardLayout para los diferentes DisplayModes (SINGLE_IMAGE, GRID, POLAROID).
     * @return El JPanel configurado para el WorkMode Visualizador.
     */
    private JPanel createVisualizerWorkModePanel(VisorView mainFrame) {
        JPanel visualizerPanel = new JPanel(new BorderLayout()); // Panel para contener toda la UI del visualizador.

        // --- SplitPane para la lista de archivos y el área de visualización ---
        JSplitPane mainSplitPane = createMainSplitPane(mainFrame); // Este ya contiene panel.izquierdo.listaArchivos y panel.derecho.visor
        visualizerPanel.add(mainSplitPane, BorderLayout.CENTER); // Añadir el split pane al centro del visualizerPanel.

        // --- Panel de miniaturas inferior ---
        JScrollPane thumbnailScrollPane = createThumbnailScrollPane("list.miniaturas", "scroll.miniaturas");
        
        visualizerPanel.add(thumbnailScrollPane, BorderLayout.SOUTH);

        // --- CardLayout para los DisplayModes DENTRO del panel de visualizador ---
        // Este CardLayout se ubica en el "panel.derecho.visor" (el lado derecho del SplitPane)
        // en lugar de directamente en el visualizerPanel.
        // Entonces, createRightSplitComponent necesita ser modificado para tener su propio CardLayout.
        // Pero para simplificar el cambio por ahora, vamos a hacer que el ImageDisplayPanel
        // sea la "tarjeta" por defecto de SINGLE_IMAGE, y el CardLayout sea en createRightSplitComponent.

        // La lógica del ImageDisplayPanel y sus controles de fondo ya están en createRightSplitComponent.
        // En lugar de que createRightSplitComponent devuelva ImageDisplayPanel,
        // hará que ImageDisplayPanel sea una de las "tarjetas" dentro de él.
        
        // NOTA: EL CardLayout para los DisplayModes NO VA AQUI, sino DENTRO de createRightSplitComponent
        // (el lado derecho del split pane). El 'visualizerPanel' es el contenedor general del modo.

        return visualizerPanel;
        
    } // Fin del metodo createVisualizerWorkModePanel
    

    private JPanel createToolbarContainer() {

        // 1. El contenedor principal ahora usa BorderLayout.
        JPanel mainToolbarContainer = new JPanel(new BorderLayout());
        mainToolbarContainer.setOpaque(true); 
        // Registramos el contenedor principal, como antes.
        registry.register("container.toolbars", mainToolbarContainer);

        // 2. Creamos tres sub-paneles, uno para cada alineamiento.
        //    Cada uno usa un FlowLayout para que las toolbars dentro de él fluyan.
        
        // Panel para las barras alineadas a la izquierda.
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(true);
        
        registry.register("container.toolbars.left", leftPanel); // Lo registramos

        // Panel para las barras alineadas al centro.
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerPanel.setOpaque(true);
        registry.register("container.toolbars.center", centerPanel); // Lo registramos

        // Panel para las barras alineadas a la derecha.
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(true);
        registry.register("container.toolbars.right", rightPanel); // Lo registramos

        // 3. Añadimos los tres sub-paneles al contenedor principal en sus respectivas zonas.
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
            this.themeManager,
            this.iconUtils,
            configuration.getInt(ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, 40),
            configuration.getInt(ConfigKeys.MINIATURAS_TAMANO_NORM_ALTO, 40),
            configuration.getBoolean(ConfigKeys.VISTA_MOSTRAR_NOMBRES_MINIATURAS_STATE, true)
        );
        thumbnailList.setCellRenderer(renderer);
        thumbnailList.setFixedCellWidth(renderer.getAnchoCalculadaDeCelda());
        thumbnailList.setFixedCellHeight(renderer.getAlturaCalculadaDeCelda());
        thumbnailList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        thumbnailList.setVisibleRowCount(-1);

        JScrollPane scrollPane = new JScrollPane(thumbnailList);
        
        TitledBorder border = BorderFactory.createTitledBorder("Miniaturas");
        scrollPane.setBorder(border);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        registry.register(scrollRegistryKey, scrollPane, "WHEEL_NAVIGABLE");

        return scrollPane;
        
    } // --- Fin del método createThumbnailScrollPane (con parámetros) ---
    
    
    private JSplitPane createMainSplitPane(VisorView mainFrame) {
        // AQUÍ ESTÁ EL CAMBIO: Pasamos 'mainFrame' al método que crea el panel izquierdo
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftSplitComponent(mainFrame), createRightSplitComponent());
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
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
        
        // Guardamos las referencias en VisorView para poder actualizar el título dinámicamente
        mainFrame.setPanelFiltrosActivos(panelFiltros);
        mainFrame.setBorderFiltrosActivos(borderFiltros);
        
        
        JList<controlador.managers.filter.FilterCriterion> filterList = new JList<>();
        
        
        // --- Carga de iconos y nuevo renderer ---
        if (this.logicIcons != null && this.typeIcons != null && this.deleteIcon != null) {
        	filterList.setCellRenderer(new vista.renderers.FilterCriterionCellRenderer(this.logicIcons, this.typeIcons, this.deleteIcon));
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
        splitPaneIzquierdoVertical.setTopComponent(tabbedPaneSuperior);   // Arriba van las pestañas
        splitPaneIzquierdoVertical.setBottomComponent(panelFiltros);      // Abajo va el panel de filtros
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
        // El panel general derecho sigue usando BorderLayout. Contendrá el CardLayout y los controles inferiores.
        JPanel rightPanel = new JPanel(new BorderLayout());
        registry.register("panel.derecho.visor", rightPanel);
        
        rightPanel.setOpaque(false);

        // 1. Crear el contenedor que usará CardLayout para los DisplayModes.
        JPanel displayModesContainer = new JPanel(new CardLayout());
        registry.register("container.displaymodes", displayModesContainer); 

        // 2. Crear el panel para la vista SINGLE_IMAGE (el que ya tenías).
        ImageDisplayPanel singleImageViewPanel = new ImageDisplayPanel(this.themeManager, this.model);
        registry.register("panel.display.imagen", singleImageViewPanel);
        registry.register("label.imagenPrincipal", singleImageViewPanel.getInternalLabel(), "WHEEL_NAVIGABLE");

        TitledBorder border = BorderFactory.createTitledBorder("");
        singleImageViewPanel.setBorder(border);

        // --- INICIO DE LA CORRECCIÓN DE FOCO ---
        // 1. Hacemos que el panel pueda recibir el foco.
        singleImageViewPanel.setFocusable(true);

        // 2. Creamos un MouseListener para solicitar el foco al hacer clic.
        java.awt.event.MouseAdapter focusRequester = new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                singleImageViewPanel.requestFocusInWindow();
            }
        };

        // 3. Añadimos el listener TANTO al panel contenedor COMO a su JLabel interno.
        //            Esto asegura que el foco se pida sin importar dónde exactamente se haga clic.
        singleImageViewPanel.addMouseListener(focusRequester);
        singleImageViewPanel.getInternalLabel().addMouseListener(focusRequester);
        // --- FIN DE LA CORRECCIÓN DE FOCO ---
        
	    // 3. Crear una instancia ÚNICA del ThumbnailPreviewer para el grid.
        //    Le pasamos null como JList porque se usará con múltiples listas,
        //    y le pasamos TODAS las dependencias necesarias.
        ThumbnailPreviewer gridPreviewer = new ThumbnailPreviewer(null, model, themeManager, null, registry);
        //    El 'null' para IViewManager es deliberado, ya que el previsualizador
        //    no necesita esta dependencia para la funcionalidad actual.
		
	    // 4. Crear una instancia del GridDisplayPanel, pasándole AHORA el previewer.
	    GridDisplayPanel gridViewPanel = new GridDisplayPanel(this.model, this.gridThumbnailService, this.themeManager, this.iconUtils, gridPreviewer, this.registry);
	    
	 // --- INICIO DE LA MODIFICACIÓN: Añadir la toolbar de tamaño al grid del visualizador ---
	    if (this.toolbarManager != null) {
	        // 1. Obtenemos solo la toolbar de tamaño.
	        JToolBar tamanoToolbar = this.toolbarManager.getToolbar("barra_grid_tamano");

	        // 2. La metemos en una lista.
	        java.util.List<JToolBar> toolbarsParaGrid = new java.util.ArrayList<>();
	        if (tamanoToolbar != null) toolbarsParaGrid.add(tamanoToolbar);

	        // 3. Pasamos la lista (con un solo elemento) al panel del grid.
	        if (!toolbarsParaGrid.isEmpty()) {
	            gridViewPanel.setToolbars(toolbarsParaGrid);
	        }
	    }
	    // --- FIN DE LA MODIFICACIÓN ---
		
	    registry.register("panel.display.grid", gridViewPanel);
	    JList<String> gridList = gridViewPanel.getGridList(); 
	    registry.register("list.grid", gridList, "WHEEL_NAVIGABLE");
	    logger.debug("<<<<< DEBUG: 'GridDisplayPanel' y su 'list.grid' interna han sido registrados. >>>>>");
		         
        JPanel polaroidViewPanel = new JPanel();
        polaroidViewPanel.add(new JLabel("Vista POLAROID en construcción..."));
        registry.register("panel.display.polaroid", polaroidViewPanel);

        // 5. Añadir todas las vistas al contenedor CardLayout con las claves correctas.
        displayModesContainer.add(singleImageViewPanel, "VISTA_SINGLE_IMAGE");
        displayModesContainer.add(gridViewPanel, "VISTA_GRID");
        displayModesContainer.add(polaroidViewPanel, "VISTA_POLAROID");

        // 6. Añadir el contenedor CardLayout al centro del panel derecho.
        rightPanel.add(displayModesContainer, BorderLayout.CENTER);

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
            logger.warn("WARN [ViewBuilder...]: La toolbar 'controles_imagen_inferior' no se encontró. Se devolverá una barra vacía.");
            return new JToolBar();
        }

        imageControlsToolbar.setLayout(new FlowLayout(FlowLayout.RIGHT, 3, 1));
        
        return imageControlsToolbar;
    } // --- Fin del método createBackgroundControlPanel ---
    

    /**
     * Crea la barra de estado inferior completa de la aplicación.
     * Esta barra contiene la ruta del archivo, los controles de estado (ahora en una JToolBar)
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
        
        //FIXME con este set opaque se elimina el problema del color de la toolbar
        bottomStatusBar.setOpaque(true);
        
        // --- FIN DE LA MODIFICACIÓN 3 ---
        

        // 2. Componente Izquierdo: Etiqueta para la ruta del archivo.
        JLabel rutaCompletaArchivoLabel = new JLabel("Ruta: (ninguna imagen seleccionada)");
        registry.register("label.estado.ruta", rutaCompletaArchivoLabel);
        
        // Contenedor para la etiqueta de ruta (para que ocupe el espacio central sobrante)
        JPanel panelRuta = new JPanel(new BorderLayout());
        panelRuta.setOpaque(false);
        panelRuta.add(rutaCompletaArchivoLabel, BorderLayout.CENTER);
        bottomStatusBar.add(panelRuta, BorderLayout.CENTER);

        // 3. Componente Derecho: Un contenedor que a su vez tendrá los controles y los mensajes.
        JPanel panelDerechoContenedor = new JPanel(new BorderLayout(5, 0));
        panelDerechoContenedor.setOpaque(false);

        // 3a. Sub-componente Central (dentro del derecho): La nueva JToolBar de controles.
        //     Le pedimos al ToolbarManager la barra de herramientas que hemos definido.
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
        	
            // Configuramos su apariencia para que no parezca una toolbar flotante tradicional.
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
            logger.error("CRITICAL ERROR [ViewBuilder]: La toolbar 'barra_estado_controles' no se pudo crear o encontrar.");
        }

        // 3b. Sub-componente Derecho (dentro del derecho): El área de mensajes y temporizador.
        //     Esta parte no cambia respecto a tu código original.
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
    

    private JToggleButton createStatusBarToggleButton(String command, String iconName, int iconSize, Map<String, Action> actionMap) {
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

        // La creación de JLabels se mantiene, pero eliminamos la asignación de color de texto.
        JLabel nombreArchivoInfoLabel = new JLabel("Archivo: N/A");
        nombreArchivoInfoLabel.setOpaque(false);
        registry.register("label.info.nombreArchivo", nombreArchivoInfoLabel);

        JLabel indiceTotalInfoLabel = new JLabel("Idx: N/A");
        indiceTotalInfoLabel.setOpaque(false);
        registry.register("label.info.indiceTotal", indiceTotalInfoLabel);

        JLabel dimensionesOriginalesInfoLabel = new JLabel("Dim: N/A");
        dimensionesOriginalesInfoLabel.setOpaque(false);
        registry.register("label.info.dimensiones", dimensionesOriginalesInfoLabel);

        JLabel tamanoArchivoInfoLabel = new JLabel("Tam: N/A");
        tamanoArchivoInfoLabel.setOpaque(false);
        registry.register("label.info.tamano", tamanoArchivoInfoLabel);

        JLabel fechaArchivoInfoLabel = new JLabel("Fch: N/A");
        fechaArchivoInfoLabel.setOpaque(false);
        registry.register("label.info.fecha", fechaArchivoInfoLabel);

        JLabel modoZoomNombreInfoLabel = new JLabel("Modo: N/A");
        modoZoomNombreInfoLabel.setOpaque(false);
        registry.register("label.info.modoZoom", modoZoomNombreInfoLabel);

        JLabel porcentajeZoomVisualRealInfoLabel = new JLabel("%Z: N/A");
        porcentajeZoomVisualRealInfoLabel.setOpaque(false);
        registry.register("label.info.porcentajeZoom", porcentajeZoomVisualRealInfoLabel);

        JLabel formatoImagenInfoLabel = new JLabel("Fmt: N/A");
        formatoImagenInfoLabel.setOpaque(false);
        registry.register("label.info.formatoImagen", formatoImagenInfoLabel);
        formatoImagenInfoLabel.setToolTipText("Formato del archivo de imagen actual");
        
        // GridBagLayout 
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE;
        panel.add(nombreArchivoInfoLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.LINE_END;

        gbc.gridx = 2; panel.add(new JSeparator(SwingConstants.VERTICAL), gbc);
        gbc.gridx = 3; panel.add(dimensionesOriginalesInfoLabel, gbc);
        gbc.gridx = 4; panel.add(new JSeparator(SwingConstants.VERTICAL), gbc);
        gbc.gridx = 5; panel.add(tamanoArchivoInfoLabel, gbc);
        gbc.gridx = 6; panel.add(new JSeparator(SwingConstants.VERTICAL), gbc);
        gbc.gridx = 7; panel.add(fechaArchivoInfoLabel, gbc);

        gbc.gridx = 8; gbc.insets = new Insets(0, 8, 0, 8);
        panel.add(new JSeparator(SwingConstants.VERTICAL), gbc);
        gbc.insets = new Insets(0, 3, 0, 3);

        gbc.gridx = 9;  panel.add(indiceTotalInfoLabel, gbc);
        gbc.gridx = 10; panel.add(new JSeparator(SwingConstants.VERTICAL), gbc);
        gbc.gridx = 11; panel.add(porcentajeZoomVisualRealInfoLabel, gbc);
        gbc.gridx = 12; panel.add(new JSeparator(SwingConstants.VERTICAL), gbc);
        gbc.gridx = 13; panel.add(modoZoomNombreInfoLabel, gbc);
        gbc.gridx = 14; panel.add(new JSeparator(SwingConstants.VERTICAL), gbc);
        gbc.gridx = 15; panel.add(formatoImagenInfoLabel, gbc);
        
        return panel;
    } // --- Fin del método createTopInfoPanel ---
    
    
    public void setActionMap(Map<String, Action> actionMap) {
        this.actionMap = Objects.requireNonNull(actionMap, "ActionMap no puede ser nulo en ViewBuilder");
    } // ---FIN de metodo setActionMap---
    
    public void setFolderTreeManager(FolderTreeManager folderTreeManager) {
        this.folderTreeManager = Objects.requireNonNull(folderTreeManager, "FolderTreeManager no puede ser null en ViewBuilder.");
    } // --- Fin del método setFolderTreeManager ---
    
    public void setProjectBuilder(ProjectBuilder projectBuilder) {
        this.projectBuilder = Objects.requireNonNull(projectBuilder, "ProjectBuilder no puede ser null en ViewBuilder");
    }
    
    public void setFilterRendererIcons(Map<controlador.managers.filter.FilterCriterion.Logic, javax.swing.Icon> logicIcons,
        Map<controlador.managers.filter.FilterCriterion.SourceType, javax.swing.Icon> typeIcons, javax.swing.Icon deleteIcon) {
    	
			this.logicIcons = logicIcons;
			this.typeIcons = typeIcons;
			this.deleteIcon = deleteIcon;
			
    } // ---FIN de metodo setFilterRendererIcons---
    
    @FunctionalInterface
    interface FilterInteractionListener {
        void onFilterClicked(java.awt.event.MouseEvent e, controlador.managers.filter.FilterCriterion criterion, int index);
    }
    
} // --- FIN de la clase ViewBuilder ---