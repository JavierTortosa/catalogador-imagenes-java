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
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
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
import javax.swing.border.TitledBorder;

import controlador.commands.AppActionCommands;
import controlador.managers.ToolbarManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.image.ThumbnailService;
import vista.VisorView;
import vista.panels.ImageDisplayPanel;
import vista.renderers.MiniaturaListCellRenderer;
import vista.renderers.NombreArchivoRenderer;
import vista.theme.ThemeManager;
import vista.util.IconUtils;


public class ViewBuilder{
	
	// --- Dependencias Clave (Lista Simplificada) ---
    private final ComponentRegistry registry;
    private final VisorModel model;
    private final ThemeManager themeManager;
    private final ConfigurationManager configuration;
    private final IconUtils iconUtils;
    private final ThumbnailService thumbnailService;
    private final ProjectBuilder projectBuilder;
    
    private Map<String, Action> actionMap;
    private ToolbarManager toolbarManager;

    
    public ViewBuilder(
            ComponentRegistry registry,
            VisorModel model,
            ThemeManager themeManager,
            ConfigurationManager configuration,
            IconUtils iconUtils,
            ThumbnailService thumbnailService,
            ProjectBuilder projectBuilder
        ){
        this.registry = registry;
        this.model = model;
        this.themeManager = themeManager;
        this.configuration = configuration;
        this.iconUtils = iconUtils;
        this.thumbnailService = Objects.requireNonNull(thumbnailService);
        this.projectBuilder = Objects.requireNonNull(projectBuilder, "ProjectBuilder no puede ser null");

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
        System.out.println("  [ViewBuilder] Iniciando la construcción del frame principal con estructura CardLayout...");

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
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLayout(new BorderLayout());

        JPanel toolbarContainer = createToolbarContainer();
        JPanel topInfoPanel = createTopInfoPanel();
        JPanel northWrapper = new JPanel(new BorderLayout());
        northWrapper.add(toolbarContainer, BorderLayout.NORTH);
        northWrapper.add(topInfoPanel, BorderLayout.CENTER);
        mainFrame.add(northWrapper, BorderLayout.NORTH);
        
        JPanel bottomStatusBar = createBottomStatusBar();
        mainFrame.add(bottomStatusBar, BorderLayout.SOUTH);
        
        
        // --- CardLayout para MODOS DE TRABAJO (WorkModes) ---
        JPanel workModesContainer = new JPanel(new CardLayout());
        registry.register("container.workmodes", workModesContainer);
        
        
        // Panel para el WorkMode VISUALIZADOR
        JPanel visualizerWorkModePanel = createVisualizerWorkModePanel(); 
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

        System.out.println("  [ViewBuilder] Frame principal construido y ensamblado.");
        return mainFrame;
    }  // --- Fin del método createMainFrame ---
    
    
    /**
     * Crea el panel principal para el WorkMode VISUALIZADOR.
     * Este panel contendrá su propio CardLayout para los diferentes DisplayModes (SINGLE_IMAGE, GRID, POLAROID).
     * @return El JPanel configurado para el WorkMode Visualizador.
     */
    private JPanel createVisualizerWorkModePanel() {
        JPanel visualizerPanel = new JPanel(new BorderLayout()); // Panel para contener toda la UI del visualizador.

        // --- SplitPane para la lista de archivos y el área de visualización ---
        JSplitPane mainSplitPane = createMainSplitPane(); // Este ya contiene panel.izquierdo.listaArchivos y panel.derecho.visor
        visualizerPanel.add(mainSplitPane, BorderLayout.CENTER); // Añadir el split pane al centro del visualizadorPanel.

        // --- Panel de miniaturas inferior ---
//        JScrollPane thumbnailScrollPane = createThumbnailScrollPane();
        
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
        mainToolbarContainer.setOpaque(false); // Sigue siendo transparente.
        // Registramos el contenedor principal, como antes.
        registry.register("container.toolbars", mainToolbarContainer);

        // 2. Creamos tres sub-paneles, uno para cada alineamiento.
        //    Cada uno usa un FlowLayout para que las toolbars dentro de él fluyan.
        
        // Panel para las barras alineadas a la izquierda.
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        registry.register("container.toolbars.left", leftPanel); // Lo registramos

        // Panel para las barras alineadas al centro.
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerPanel.setOpaque(false);
        registry.register("container.toolbars.center", centerPanel); // Lo registramos

        // Panel para las barras alineadas a la derecha.
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);
        registry.register("container.toolbars.right", rightPanel); // Lo registramos

        // 3. Añadimos los tres sub-paneles al contenedor principal en sus respectivas zonas.
        mainToolbarContainer.add(leftPanel, BorderLayout.WEST);
        mainToolbarContainer.add(centerPanel, BorderLayout.CENTER);
        mainToolbarContainer.add(rightPanel, BorderLayout.EAST);

        System.out.println("  [ViewBuilder] Toolbar container creado con estructura BorderLayout (WEST, CENTER, EAST).");
        
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
    
    
    private JSplitPane createMainSplitPane() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftSplitComponent(), createRightSplitComponent());
        splitPane.setResizeWeight(0.25);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        registry.register("splitpane.main", splitPane);
        
        return splitPane;
    } // --- Fin del método createMainSplitPane ---
    

    private JPanel createLeftSplitComponent() {
        JPanel panelIzquierdo = new JPanel(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder("Archivos: 0");
        panelIzquierdo.setBorder(border);
        registry.register("panel.izquierdo.listaArchivos", panelIzquierdo);

        JList<String> fileList = new JList<>(model.getModeloLista());
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setCellRenderer(new NombreArchivoRenderer(themeManager, model));
        registry.register("list.nombresArchivo", fileList, "WHEEL_NAVIGABLE");

        JScrollPane scrollPane = new JScrollPane(fileList);
        registry.register("scroll.nombresArchivo", scrollPane);

        panelIzquierdo.add(scrollPane, BorderLayout.CENTER);
        return panelIzquierdo;
        
    } // --- Fin del método createLeftSplitComponent ---
    
    
    private JPanel createRightSplitComponent() {
        // El panel general derecho sigue usando BorderLayout. Contendrá el CardLayout y los controles inferiores.
        JPanel rightPanel = new JPanel(new BorderLayout());
        registry.register("panel.derecho.visor", rightPanel);

        // 1. Crear el contenedor que usará CardLayout para los DisplayModes.
        //    Este es el panel que tu GeneralController está buscando.
        JPanel displayModesContainer = new JPanel(new CardLayout());
        registry.register("container.displaymodes", displayModesContainer); // ¡REGISTRO CLAVE!

        // 2. Crear el panel para la vista SINGLE_IMAGE (el que ya tenías).
        ImageDisplayPanel singleImageViewPanel = new ImageDisplayPanel(this.themeManager, this.model);
        registry.register("panel.display.imagen", singleImageViewPanel); // Mantenemos el registro para el zoom, etc.
        registry.register("label.imagenPrincipal", singleImageViewPanel.getInternalLabel(), "WHEEL_NAVIGABLE");
        
        TitledBorder border = BorderFactory.createTitledBorder("");
        singleImageViewPanel.setBorder(border);
        
        // 3. Crear paneles "placeholder" para las otras vistas.
        //    Más adelante, los reemplazarás con tus clases reales (GridPanel, PolaroidPanel).
        JPanel gridViewPanel = new JPanel();
        gridViewPanel.add(new JLabel("Vista GRID en construcción..."));
        registry.register("panel.display.grid", gridViewPanel); // Buena práctica registrarlos también

        JPanel polaroidViewPanel = new JPanel();
        polaroidViewPanel.add(new JLabel("Vista POLAROID en construcción..."));
        registry.register("panel.display.polaroid", polaroidViewPanel);

        // 4. Añadir todas las vistas al contenedor CardLayout con las claves correctas.
        //    Estas claves DEBEN coincidir con las que genera mapDisplayModeToCardLayoutViewName en GeneralController.
        displayModesContainer.add(singleImageViewPanel, "VISTA_SINGLE_IMAGE");
        displayModesContainer.add(gridViewPanel, "VISTA_GRID");
        displayModesContainer.add(polaroidViewPanel, "VISTA_POLAROID");

        // 5. Añadir el contenedor CardLayout al centro del panel derecho.
        rightPanel.add(displayModesContainer, BorderLayout.CENTER);

        // La lógica de los controles de fondo no cambia, se queda en la parte sur.
        JToolBar imageControlsToolbar = createBackgroundControlPanel();
        if (imageControlsToolbar != null) {
            rightPanel.add(imageControlsToolbar, BorderLayout.SOUTH);
        }

        return rightPanel;
    } // --- Fin del método createRightSplitComponent ---
    
    
    public JToolBar createBackgroundControlPanel() {
        if (this.toolbarManager == null) {
            System.err.println("ERROR [ViewBuilder.createBackgroundControlPanel]: ToolbarManager es nulo.");
            return new JToolBar(); 
        }
        
        JToolBar imageControlsToolbar = toolbarManager.getToolbar("controles_imagen_inferior");

        if (imageControlsToolbar == null) {
            System.err.println("WARN [ViewBuilder...]: La toolbar 'controles_imagen_inferior' no se encontró. Se devolverá una barra vacía.");
            return new JToolBar();
        }

        imageControlsToolbar.setLayout(new FlowLayout(FlowLayout.RIGHT, 3, 1));
        imageControlsToolbar.setOpaque(false);

        return imageControlsToolbar;
    } // --- Fin del método createBackgroundControlPanel ---
    
    
    private JPanel createBottomStatusBar() {
        JPanel bottomStatusBar = new JPanel(new BorderLayout(5, 0));
        registry.register("panel.estado.inferior", bottomStatusBar);

        bottomStatusBar.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        JLabel rutaCompletaArchivoLabel = new JLabel("Ruta: (ninguna imagen seleccionada)");
        registry.register("label.estado.ruta", rutaCompletaArchivoLabel);
        
        JPanel panelRuta = new JPanel(new BorderLayout());
        panelRuta.setOpaque(false);
        panelRuta.add(rutaCompletaArchivoLabel, BorderLayout.CENTER);
        bottomStatusBar.add(panelRuta, BorderLayout.CENTER);

        JPanel panelDerechoContenedor = new JPanel(new BorderLayout(5, 0));
        panelDerechoContenedor.setOpaque(false);

        JPanel panelControlesInferior = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        panelControlesInferior.setOpaque(false);
        registry.register("panel.estado.controles", panelControlesInferior);

        final int iconSize = 18;

        if (this.actionMap == null) {
            System.err.println("CRITICAL ERROR [ViewBuilder]: El campo 'actionMap' de la clase es nulo. Se usará un mapa vacío.");
            this.actionMap = new java.util.HashMap<>();
        }

        JToggleButton zoomManualButton = createStatusBarToggleButton(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE, "3001-zoom_48x48.png", iconSize, this.actionMap);
        JToggleButton subcarpetasButton = createStatusBarToggleButton(AppActionCommands.CMD_TOGGLE_SUBCARPETAS, "7001-subcarpetas_48x48.png", iconSize, this.actionMap);
        JToggleButton proporcionesButton = createStatusBarToggleButton(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES, "7002-mantener_proporciones_48x48.png", iconSize, this.actionMap);
        JToggleButton mantenerEncimaButton = createStatusBarToggleButton(AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP, "7004-siempre_encima_48x48.png", iconSize, this.actionMap);
        
        registry.register("button.indicador.zoomManual", zoomManualButton);
        registry.register("button.indicador.proporciones", proporcionesButton);
        registry.register("button.indicador.subcarpetas", subcarpetasButton);
        registry.register("button.indicador.mantenerEncima", mantenerEncimaButton);

        JLabel porcentajeZoomLabel = new JLabel("%Z: 100%");
        registry.register("label.control.zoomPorcentaje", porcentajeZoomLabel);
        
        JButton modoZoomBoton = new JButton();
        registry.register("button.control.modoZoom", modoZoomBoton);
        
        panelControlesInferior.add(zoomManualButton);
        panelControlesInferior.add(subcarpetasButton);
        panelControlesInferior.add(proporcionesButton);
        panelControlesInferior.add(mantenerEncimaButton);
        panelControlesInferior.add(new JSeparator(SwingConstants.VERTICAL));
        panelControlesInferior.add(porcentajeZoomLabel);
        panelControlesInferior.add(modoZoomBoton);
        
        panelDerechoContenedor.add(panelControlesInferior, BorderLayout.CENTER);

        // --- INICIO DE LA MODIFICACIÓN ---
        // Creamos un subpanel para la parte derecha, para que el contador y los mensajes convivan.
        JPanel eastStatusBarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        eastStatusBarPanel.setOpaque(false);

        // Creamos el nuevo JLabel para el temporizador del carrusel.
        JLabel carouselTimerLabel = new JLabel("--:--");
        carouselTimerLabel.setVisible(false); // Inicialmente oculto
        carouselTimerLabel.setToolTipText("Tiempo para la siguiente imagen");
        
     // Centramos el texto DENTRO del JLabel
        carouselTimerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        // Le damos un tamaño mínimo para que tenga presencia
        carouselTimerLabel.setPreferredSize(new java.awt.Dimension(60, carouselTimerLabel.getPreferredSize().height));
        
        registry.register("label.estado.carouselTimer", carouselTimerLabel); // Lo registramos
        
        JLabel mensajesAppLabel = new JLabel(" ");
        registry.register("label.estado.mensajes", mensajesAppLabel);
        
        // Establecemos la alineación del texto al CENTRO del JLabel
        mensajesAppLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Creamos un tamaño preferido. 150 píxeles de ancho es un buen punto de partida.
        // La altura se calcula automáticamente.
        mensajesAppLabel.setPreferredSize(new java.awt.Dimension(150, mensajesAppLabel.getPreferredSize().height));
        
        // Añadimos AMBOS labels al nuevo subpanel.
        eastStatusBarPanel.add(carouselTimerLabel);
        eastStatusBarPanel.add(mensajesAppLabel);

        // Añadimos el subpanel al contenedor derecho.
        panelDerechoContenedor.add(eastStatusBarPanel, BorderLayout.EAST);
        // --- FIN DE LA MODIFICACIÓN ---

        bottomStatusBar.add(panelDerechoContenedor, BorderLayout.EAST);

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
        registry.register("panel.info.superior", panel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.insets = new Insets(0, 3, 0, 3);

        // La creación de JLabels se mantiene, pero eliminamos la asignación de color de texto.
        JLabel nombreArchivoInfoLabel = new JLabel("Archivo: N/A");
        registry.register("label.info.nombreArchivo", nombreArchivoInfoLabel);

        JLabel indiceTotalInfoLabel = new JLabel("Idx: N/A");
        registry.register("label.info.indiceTotal", indiceTotalInfoLabel);

        JLabel dimensionesOriginalesInfoLabel = new JLabel("Dim: N/A");
        registry.register("label.info.dimensiones", dimensionesOriginalesInfoLabel);

        JLabel tamanoArchivoInfoLabel = new JLabel("Tam: N/A");
        registry.register("label.info.tamano", tamanoArchivoInfoLabel);

        JLabel fechaArchivoInfoLabel = new JLabel("Fch: N/A");
        registry.register("label.info.fecha", fechaArchivoInfoLabel);

        JLabel modoZoomNombreInfoLabel = new JLabel("Modo: N/A");
        registry.register("label.info.modoZoom", modoZoomNombreInfoLabel);

        JLabel porcentajeZoomVisualRealInfoLabel = new JLabel("%Z: N/A");
        registry.register("label.info.porcentajeZoom", porcentajeZoomVisualRealInfoLabel);

        JLabel formatoImagenInfoLabel = new JLabel("Fmt: N/A");
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
    }
    
    
} // --- FIN de la clase ViewBuilder ---

