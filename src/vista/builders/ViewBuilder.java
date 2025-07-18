package vista.builders;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import controlador.VisorController;
import controlador.commands.AppActionCommands;
import controlador.managers.ToolbarManager;
import controlador.managers.interfaces.IViewManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.image.ThumbnailService;
import vista.VisorView;
import vista.config.ButtonType;
import vista.config.UIDefinitionService;
import vista.panels.ImageDisplayPanel;
import vista.renderers.MiniaturaListCellRenderer;
import vista.renderers.NombreArchivoRenderer;
import vista.theme.ThemeManager;
import vista.util.IconUtils;


public class ViewBuilder {

    // --- Dependencias Clave ---
    private final ComponentRegistry registry;
    private final VisorModel model;
    private final ThemeManager themeManager;
    private final ConfigurationManager configuration;
    private final VisorController controller;
    private final IconUtils iconUtils;
    private final UIDefinitionService uiDefService;
    private final ThumbnailService thumbnailService;
    private final ProjectBuilder projectBuilder;
    private final DefaultListModel<String> modeloMiniaturas;
    
    private Map<String, Action> actionMap;
    private ToolbarManager toolbarManager;
    private IViewManager viewManager;

    private final List<JButton> backgroundControlButtons;
    private final Border bordePuntoNormal;
    private final Border bordePuntoSeleccionado;
    
    public ViewBuilder(
            ComponentRegistry registry,
            VisorModel model,
            ThemeManager themeManager,
            ConfigurationManager configuration,
            VisorController controller,
            IconUtils iconUtils,
            ThumbnailService thumbnailService,
            ProjectBuilder projectBuilder,
            DefaultListModel<String> modeloMiniaturas
        ){
        this.registry = registry;
        this.model = model;
        this.themeManager = themeManager;
        this.configuration = configuration;
        this.controller = controller;
        this.iconUtils = iconUtils;
        this.uiDefService = new UIDefinitionService();
        this.thumbnailService = Objects.requireNonNull(thumbnailService);
        this.projectBuilder = Objects.requireNonNull(projectBuilder, "ProjectBuilder no puede ser null");
        this.modeloMiniaturas = Objects.requireNonNull(modeloMiniaturas, "El modelo de miniaturas no puede ser null en el constructor de ViewBuilder.");

        this.backgroundControlButtons = new ArrayList<>();
        this.bordePuntoNormal = BorderFactory.createLineBorder(Color.GRAY);
        this.bordePuntoSeleccionado = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.CYAN, 2),
            BorderFactory.createLineBorder(Color.BLACK)
        );
    } // --- Fin del método ViewBuilder (constructor) ---

    public void setToolbarManager(ToolbarManager toolbarManager) {
        this.toolbarManager = Objects.requireNonNull(toolbarManager, "ToolbarManager no puede ser null en ViewBuilder.");
    } // --- Fin del método setToolbarManager ---

    public void setViewManager(IViewManager viewManager) {
        this.viewManager = Objects.requireNonNull(viewManager, "IViewManager no puede ser null en el setter");
    } // --- Fin del método setViewManager ---


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
        
        // --- INICIO CORRECCIÓN: CardLayout para MODOS DE TRABAJO (WorkModes) ---
        // Este será el CardLayout de nivel superior para cambiar entre Visualizador, Proyecto, Carrusel, Datos.
        JPanel workModesContainer = new JPanel(new CardLayout());
        registry.register("container.workmodes", workModesContainer); // Nueva clave en el registro
        
        // Panel para el WorkMode VISUALIZADOR (que tendrá su propio CardLayout para DisplayModes)
        JPanel visualizerWorkModePanel = createVisualizerWorkModePanel(); 
        workModesContainer.add(visualizerWorkModePanel, "VISTA_VISUALIZADOR"); // Clave para el CardLayout
        registry.register("panel.workmode.visualizador", visualizerWorkModePanel); // Registrar el panel del WorkMode

        // Panel para el WorkMode PROYECTO (ya existente)
        JPanel projectWorkModePanel = this.projectBuilder.buildProjectViewPanel();
        workModesContainer.add(projectWorkModePanel, "VISTA_PROYECTOS"); // Clave para el CardLayout
        registry.register("panel.workmode.proyectos", projectWorkModePanel); // Registrar el panel del WorkMode

        // Paneles para otros WorkModes futuros (por ahora vacíos como placeholder)
        JPanel carouselWorkModePanel = new JPanel(); // Placeholder para el WorkMode.CARROUSEL
        carouselWorkModePanel.add(new JLabel("Modo Carrusel en desarrollo...", SwingConstants.CENTER));
        workModesContainer.add(carouselWorkModePanel, "VISTA_CAROUSEL_WORKMODE");
        registry.register("panel.workmode.carousel", carouselWorkModePanel);

        JPanel dataWorkModePanel = new JPanel(); // Placeholder para el WorkMode.DATOS
        dataWorkModePanel.add(new JLabel("Modo Datos en desarrollo...", SwingConstants.CENTER));
        workModesContainer.add(dataWorkModePanel, "VISTA_DATOS");
        registry.register("panel.workmode.datos", dataWorkModePanel);
        
        // Asignar el CardLayout de WorkModes al centro del mainFrame
        mainFrame.add(workModesContainer, BorderLayout.CENTER);
        // --- FIN CORRECCIÓN ---


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
        JScrollPane thumbnailScrollPane = createThumbnailScrollPane();
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
    
    private JPanel createVisualizadorViewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        panel.add(createMainSplitPane(), BorderLayout.CENTER);
        
        panel.add(createThumbnailScrollPane(), BorderLayout.SOUTH);
        
        return panel;
    } // --- Fin del método createVisualizadorViewPanel ---


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
        registry.register("list.nombresArchivo", fileList);

        JScrollPane scrollPane = new JScrollPane(fileList);
        registry.register("scroll.nombresArchivo", scrollPane);

        panelIzquierdo.add(scrollPane, BorderLayout.CENTER);
        return panelIzquierdo;
    } // --- Fin del método createLeftSplitComponent ---
    
    
    private JScrollPane createThumbnailScrollPane() {
        JList<String> thumbnailList = new JList<>(this.modeloMiniaturas);
        registry.register("list.miniaturas", thumbnailList);

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
        // border.setTitleColor(themeManager.getTemaActual().colorBordeTitulo()); // <-- COMENTADO
        scrollPane.setBorder(border);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        registry.register("scroll.miniaturas", scrollPane);

        return scrollPane;
    } // --- Fin del método createThumbnailScrollPane ---
    
    
    private JPanel createRightSplitComponent() {
        // El panel general derecho sigue usando BorderLayout. Contendrá el CardLayout y los controles inferiores.
        JPanel rightPanel = new JPanel(new BorderLayout());
        registry.register("panel.derecho.visor", rightPanel);

        // --- INICIO DE LA MODIFICACIÓN ---

        // 1. Crear el contenedor que usará CardLayout para los DisplayModes.
        //    Este es el panel que tu GeneralController está buscando.
        JPanel displayModesContainer = new JPanel(new CardLayout());
        registry.register("container.displaymodes", displayModesContainer); // ¡REGISTRO CLAVE!

        // 2. Crear el panel para la vista SINGLE_IMAGE (el que ya tenías).
        ImageDisplayPanel singleImageViewPanel = new ImageDisplayPanel(this.themeManager, this.model);
        registry.register("panel.display.imagen", singleImageViewPanel); // Mantenemos el registro para el zoom, etc.
        registry.register("label.imagenPrincipal", singleImageViewPanel.getInternalLabel());
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

        // --- FIN DE LA MODIFICACIÓN ---

        // La lógica de los controles de fondo no cambia, se queda en la parte sur.
        JToolBar imageControlsToolbar = createBackgroundControlPanel();
        if (imageControlsToolbar != null) {
            rightPanel.add(imageControlsToolbar, BorderLayout.SOUTH);
        }

        return rightPanel;
    } // --- Fin del método createRightSplitComponent ---
    
    
	private JToolBar createBackgroundControlPanel() {
	    if (this.toolbarManager == null) {
	        System.err.println("ERROR [ViewBuilder.createBackgroundControlPanel]: ToolbarManager es nulo.");
	        return new JToolBar();
	    }

	    JToolBar imageControlsToolbar = toolbarManager.getToolbar("controles_imagen_inferior");
	    if (imageControlsToolbar == null) {
	        System.err.println("WARN [ViewBuilder.createBackgroundControlPanel]: La toolbar 'controles_imagen_inferior' no se encontró en ToolbarManager.");
	        return new JToolBar();
	    }

	    imageControlsToolbar.setLayout(new FlowLayout(FlowLayout.RIGHT, 3, 1));
	    imageControlsToolbar.setOpaque(false);

	    this.backgroundControlButtons.clear(); 
	    
	    for (java.awt.Component comp : imageControlsToolbar.getComponents()) {
	        if (comp instanceof JButton) {
	            JButton button = (JButton) comp;
	            
	            ButtonType buttonType = (ButtonType) button.getClientProperty("buttonType");
	            
	            if (buttonType == ButtonType.TRANSPARENT) {
	                String baseIconName = (String) button.getClientProperty("baseIconName");
	                String customOverlayKey = (String) button.getClientProperty("customOverlayKey");
	                int iconSize = 16;

	                if ("checkered".equals(customOverlayKey)) {
	                    if (iconUtils.getCheckeredOverlayIcon(baseIconName, iconSize, iconSize) != null) {
	                        button.setIcon(iconUtils.getCheckeredOverlayIcon(baseIconName, iconSize, iconSize));
	                    }
	                    backgroundControlButtons.add(button);
	                } else if (customOverlayKey != null) {
	                    Color colorFondoOriginalTema = themeManager.getFondoSecundarioParaTema(customOverlayKey);
	                    if (colorFondoOriginalTema == null) colorFondoOriginalTema = Color.GRAY;
	                    
	                    Color colorParaTintar = colorFondoOriginalTema;
	                    int luminosidad = colorFondoOriginalTema.getRed() + colorFondoOriginalTema.getGreen() + colorFondoOriginalTema.getBlue();
	                    if (luminosidad < 150) { 
	                        colorParaTintar = aclararColor(colorFondoOriginalTema, 70);
	                    } else if (luminosidad > 600) { 
	                        colorParaTintar = oscurecerColor(colorFondoOriginalTema, 70);
	                    }
	                    
	                    ImageIcon mascaraIcono = iconUtils.getScaledCommonIcon(baseIconName, iconSize, iconSize);
	                    
	                    if (mascaraIcono != null) {
	                        java.awt.image.BufferedImage tintedImage = new java.awt.image.BufferedImage(
	                            iconSize, iconSize, java.awt.image.BufferedImage.TYPE_INT_ARGB);
	                        java.awt.Graphics2D g2d = tintedImage.createGraphics();
	                        
	                        g2d.drawImage(mascaraIcono.getImage(), 0, 0, null);
	                        g2d.setComposite(java.awt.AlphaComposite.SrcAtop);
	                        g2d.setColor(colorParaTintar);
	                        g2d.fillRect(0, 0, iconSize, iconSize);
	                        g2d.dispose();
	                        
	                        button.setIcon(new javax.swing.ImageIcon(tintedImage));
	                    }
	                    backgroundControlButtons.add(button);
	                }
	                
	                button.addMouseListener(new MouseAdapter() {
	                    @Override
	                    public void mousePressed(MouseEvent e) {
	                        if (customOverlayKey != null) {
	                            selectBackgroundControlButton(button);
	                        }
	                    }
	                });
	            }
	        }
	    }

	    SwingUtilities.invokeLater(() -> {
	        String currentThemeName = themeManager.getTemaActual().nombreInterno();
	        JButton defaultButton = null;
	        for (JButton btn : backgroundControlButtons) {
	            String overlayKey = (String) btn.getClientProperty("customOverlayKey");
	            if (currentThemeName.equals(overlayKey)) {
	                defaultButton = btn;
	                break;
	            }
	        }
	        if (defaultButton != null) {
	            selectBackgroundControlButton(defaultButton);
	        } else {
	             boolean isCheckered = configuration.getBoolean("interfaz.menu.vista.fondo_a_cuadros.seleccionado", false);
	             if (isCheckered) {
	                for (JButton btn : backgroundControlButtons) {
	                    if ("checkered".equals(btn.getClientProperty("customOverlayKey"))) {
	                        selectBackgroundControlButton(btn);
	                        break;
	                    }
	                }
	             }
	        }
	    });

	    return imageControlsToolbar;
	} // --- Fin del método createBackgroundControlPanel ---
	

    private Color aclararColor(Color colorOriginal, int cantidadAclarar) {
        if (colorOriginal == null) return Color.LIGHT_GRAY;
        int r = Math.min(255, colorOriginal.getRed() + cantidadAclarar);
        int g = Math.min(255, colorOriginal.getGreen() + cantidadAclarar);
        int b = Math.min(255, colorOriginal.getBlue() + cantidadAclarar);
        return new Color(r, g, b);
    } // --- Fin del método aclararColor ---

    private Color oscurecerColor(Color colorOriginal, int cantidadOscurecer) {
        if (colorOriginal == null) return Color.DARK_GRAY;
        int r = Math.max(0, colorOriginal.getRed() - cantidadOscurecer);
        int g = Math.max(0, colorOriginal.getGreen() - cantidadOscurecer);
        int b = Math.max(0, colorOriginal.getBlue() - cantidadOscurecer);
        return new Color(r, g, b);
    } // --- Fin del método oscurecerColor ---

    private void selectBackgroundControlButton(JButton selectedButton) {
        for (JButton btn : backgroundControlButtons) {
            btn.setBorder(bordePuntoNormal);
        }
        if (selectedButton != null) {
            selectedButton.setBorder(bordePuntoSeleccionado);
        }
    } // --- Fin del método selectBackgroundControlButton ---

    
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

        // --- CORRECCIÓN ---
        // Asegurarse de que this.actionMap ha sido inyectado por AppInitializer
        if (this.actionMap == null) {
            System.err.println("CRITICAL ERROR [ViewBuilder]: El campo 'actionMap' de la clase es nulo. Se usará un mapa vacío.");
            this.actionMap = new java.util.HashMap<>();
        }

        // Crear los botones usando this.actionMap
        JToggleButton zoomManualButton = createStatusBarToggleButton(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE, "3001-Zoom_48x48.png", iconSize, this.actionMap);
        JToggleButton subcarpetasButton = createStatusBarToggleButton(AppActionCommands.CMD_TOGGLE_SUBCARPETAS, "7001-Subcarpetas_48x48.png", iconSize, this.actionMap);
        JToggleButton proporcionesButton = createStatusBarToggleButton(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES, "7002-Mantener_Proporciones_48x48.png", iconSize, this.actionMap);
        JToggleButton mantenerEncimaButton = createStatusBarToggleButton(AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP, "7004-siempre_encima_48x48.png", iconSize, this.actionMap);
        
        // ... registro y añadido de botones no cambia ...
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

        JLabel mensajesAppLabel = new JLabel(" ");
        registry.register("label.estado.mensajes", mensajesAppLabel);
        mensajesAppLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mensajesAppLabel.setPreferredSize(new Dimension(200, mensajesAppLabel.getPreferredSize().height));
        panelDerechoContenedor.add(mensajesAppLabel, BorderLayout.EAST);

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
        
        // La lógica de GridBagLayout se mantiene igual
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
    
    
    private void configurarIndicadorLabel(JLabel label, Dimension dim, String tooltip) {
        label.setOpaque(true);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setPreferredSize(dim);
        label.setToolTipText(tooltip);
    } // --- Fin del método configurarIndicadorLabel ---
    
    
    public void setActionMap(Map<String, Action> actionMap) {
        this.actionMap = Objects.requireNonNull(actionMap, "ActionMap no puede ser nulo en ViewBuilder");
    }
    
    
    
} // --- FIN de la clase ViewBuilder ---

