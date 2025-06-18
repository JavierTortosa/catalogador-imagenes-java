package vista.builders;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import controlador.VisorController;
import controlador.managers.ViewManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.image.ThumbnailService;
import vista.VisorView;
import vista.config.UIDefinitionService;
import vista.panels.ImageDisplayPanel;
import vista.renderers.MiniaturaListCellRenderer;
import vista.renderers.NombreArchivoRenderer;
import vista.theme.Tema;
import vista.theme.ThemeManager;
import vista.util.IconUtils;

/**
 * Fábrica responsable de construir la interfaz gráfica principal de la aplicación.
 * Centraliza la creación y el ensamblaje de todos los componentes de la UI,
 * registrándolos en un ComponentRegistry para su posterior manipulación.
 */
public class ViewBuilder {

    // --- Dependencias Clave ---
    private final ComponentRegistry registry;
//    private final ActionFactory actionFactory;
    private final VisorModel model;
    private final ThemeManager themeManager;
    private final ConfigurationManager configuration;
    private final VisorController controller; // Para listeners de fallback
    private final IconUtils iconUtils;         // Para builders de UI
    private final UIDefinitionService uiDefService; // Para definiciones de UI
    private final ThumbnailService thumbnailService;
    private ViewManager viewManager;
    
    public ViewBuilder(
            ComponentRegistry registry,
//            ActionFactory actionFactory,
            VisorModel model,
            ThemeManager themeManager,
            ConfigurationManager configuration,
            VisorController controller,
            IconUtils iconUtils,
            ThumbnailService thumbnailService
        ){
        this.registry = registry;
//        this.actionFactory = actionFactory;
        this.model = model;
        this.themeManager = themeManager;
        this.configuration = configuration;
        this.controller = controller;
        this.iconUtils = iconUtils;
        this.uiDefService = new UIDefinitionService();
        this.thumbnailService = Objects.requireNonNull(thumbnailService);
    } // --- Fin del constructor ViewBuilder ---

 // DENTRO DE LA CLASE ViewBuilder.java

    

    /**
     * Método principal que crea, ensambla y devuelve la ventana principal de la aplicación.
     * Orquesta la llamada a todos los métodos privados de construcción y ensambla
     * la jerarquía de componentes completa.
     */
    public VisorView createMainFrame() {
        System.out.println("  [ViewBuilder] Iniciando la construcción del frame principal COMPLETO...");

        // --- FASE 1: Crear el contenedor principal (VisorView) ---
        //    El constructor de VisorView ya está simplificado.
        VisorView mainFrame = new VisorView(
        	    100,
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

     // --- FASE 2: CONSTRUIR Y ENSAMBLAR LAS PIEZAS DE LA UI ---
        
        // 2a. Construir la Barra de Menú y establecerla en el frame.
//        mainFrame.setJMenuBar(createMenuBar());
        
        // 2b. Construir el panel que contendrá la(s) barra(s) de herramientas.
        //     Este panel se añade a un contenedor norte para agruparlo con la barra de info.
        JPanel toolbarContainer = createToolbarContainer();
        
        // 2c. Construir la barra de información superior.
        JPanel topInfoPanel = createTopInfoPanel();
        
        // 2d. Crear un panel "wrapper" para el norte, que contendrá tanto las
        //     barras de herramientas como la barra de información superior, una encima de la otra.
        JPanel northWrapper = new JPanel(new BorderLayout());
        northWrapper.add(toolbarContainer, BorderLayout.NORTH); // Toolbars arriba del todo
        northWrapper.add(topInfoPanel, BorderLayout.CENTER);     // Barra de info justo debajo
        
        // 2e. Añadir el wrapper completo a la zona NORTE del frame principal.
        mainFrame.add(northWrapper, BorderLayout.NORTH);
        
        // 2f. Construir el panel de contenido principal (el que tiene el JSplitPane).
        JPanel mainContentPanel = createMainContentPanel();
        
        // 2g. Añadir el contenido principal a la zona CENTRAL del frame.
        mainFrame.add(mainContentPanel, BorderLayout.CENTER);

        // 2h. Construir la barra de estado inferior.
        JPanel bottomStatusBar = createBottomStatusBar();

        // 2i. Añadir la barra de estado a la zona SUR del frame.
        mainFrame.add(bottomStatusBar, BorderLayout.SOUTH);
        
        
//        //    Cada método `create...` construye una parte y el `mainFrame.add` la coloca.
//        
//        // 2a. Barra de Menú (se establece, no se añade con `add`)
//        mainFrame.setJMenuBar(createMenuBar());
//
//        // 2b. Panel Norte (contiene toolbars y la barra de info superior)
//        JPanel northPanel = new JPanel(new BorderLayout());
//        northPanel.add(createToolbarContainer(), BorderLayout.NORTH);
//        northPanel.add(createTopInfoPanel(), BorderLayout.SOUTH);
//        mainFrame.add(northPanel, BorderLayout.NORTH);
//
//        // 2c. Contenido Central (el JSplitPane con todo su interior)
//        mainFrame.add(createMainContentPanel(), BorderLayout.CENTER);
//
//        // 2d. Barra de Estado Inferior
//        mainFrame.add(createBottomStatusBar(), BorderLayout.SOUTH);
        
        // --- FASE 3: Lógica de configuración final de la ventana ---
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
        
        // Ajustar el divisor del JSplitPane después de que la ventana se haya dimensionado
        SwingUtilities.invokeLater(() -> {
            JSplitPane splitPane = registry.get("splitpane.main");
            if (splitPane != null) {
                // Nota: La clave para la posición del divisor debería estar en ConfigKeys
                double dividerLocation = configuration.getDouble("ui.splitpane.main.dividerLocation", 0.25);
                splitPane.setDividerLocation(dividerLocation);
            }
        });

        System.out.println("  [ViewBuilder] Frame principal construido y ensamblado.");
        return mainFrame;

    } // --- Fin del método createMainFrame ---
    

    // --- MÉTODOS DE CONSTRUCCIÓN PRIVADOS ---

//    private JMenuBar createMenuBar() {
//        MenuBarBuilder menuBuilder = new MenuBarBuilder(controller, configuration);
//        menuBuilder.setControllerGlobalActionListener(this.controller);
//        JMenuBar menuBar = menuBuilder.buildMenuBar(uiDefService.generateMenuStructure(), actionFactory.getActionMap());
//        registry.register("menubar.main", menuBar);
//
//        // Registrar todos los JMenuItems individuales que el builder creó
//        menuBuilder.getMenuItemsMap().forEach((key, item) -> {
//            registry.register(key, item);
//        });
//
//        return menuBar;
//    } // --- Fin del método createMenuBar ---

    private JPanel createToolbarContainer() {
        JPanel toolbarContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolbarContainer.setOpaque(false);
        registry.register("container.toolbars", toolbarContainer);

        // La población real de las JToolBar la haría un ToolbarManager, que se crearía
        // en el AppInitializer y usaría este contenedor. El ViewBuilder solo crea el contenedor.
        
        return toolbarContainer;
    } // --- Fin del método createToolbarContainer ---
    
    private JPanel createCenterWrapper() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(true);
        wrapper.setBackground(themeManager.getTemaActual().colorFondoPrincipal());
        registry.register("wrapper.center", wrapper);

        wrapper.add(createTopInfoPanel(), BorderLayout.NORTH);
        wrapper.add(createMainContentPanel(), BorderLayout.CENTER);
        
        return wrapper;
    } // --- Fin del método createCenterWrapper ---

    private JPanel createMainContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(themeManager.getTemaActual().colorFondoPrincipal());
        registry.register("panel.modo.visualizador", panel);

        panel.add(createMainSplitPane(), BorderLayout.CENTER);
        panel.add(createThumbnailScrollPane(), BorderLayout.SOUTH);
        
        return panel;
    } // --- Fin del método createMainContentPanel ---
    
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
        panelIzquierdo.setBackground(themeManager.getTemaActual().colorFondoPrincipal());
        TitledBorder border = BorderFactory.createTitledBorder("Archivos: 0");
        border.setTitleColor(themeManager.getTemaActual().colorBordeTitulo());
        panelIzquierdo.setBorder(border);
        registry.register("panel.izquierdo.listaArchivos", panelIzquierdo);

        JList<String> fileList = new JList<>(model.getModeloLista());
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Construimos la JList con el renderer "inteligente" que depende del ThemeManager
        fileList.setCellRenderer(new NombreArchivoRenderer(themeManager));
        registry.register("list.nombresArchivo", fileList);

        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setBorder(BorderFactory.createLineBorder(themeManager.getTemaActual().colorBorde()));
        registry.register("scroll.nombresArchivo", scrollPane);

        panelIzquierdo.add(scrollPane, BorderLayout.CENTER);
        return panelIzquierdo;
    } // --- Fin del método createLeftSplitComponent ---

    private JScrollPane createThumbnailScrollPane() {
        // La JList ahora se obtiene del registro, porque VisorView ya la tiene
        JList<String> thumbnailList = registry.get("list.miniaturas");
        
        // Si no existe, es un error, pero creamos una para evitar NullPointerException
        if (thumbnailList == null) {
            thumbnailList = new JList<>();
            registry.register("list.miniaturas", thumbnailList);
        }
        
        // Configurar el renderer con las dependencias del builder
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

        // El resto del ensamblaje del panel
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrapper.setOpaque(false);
        wrapper.add(thumbnailList);
        
        JScrollPane scrollPane = new JScrollPane(wrapper);
        TitledBorder border = BorderFactory.createTitledBorder("Miniaturas");
        border.setTitleColor(themeManager.getTemaActual().colorBordeTitulo());
        scrollPane.setBorder(border);
        registry.register("scroll.miniaturas", scrollPane);

        return scrollPane;
    } // --- FIN del metodo createThumbnailScrollPane ---
    
    
//    /**
//     * Inicializa la etiqueta principal donde se mostrará la imagen.
//     * Configura this.etiquetaImagen.
//     */
//    private JLabel inicializarEtiquetaMostrarImagen() {
//        System.out.println("      -> [VisorView] inicializarEtiquetaMostrarImagen...");
//        // >>> COPIA AQUÍ TU CÓDIGO DE inicializarEtiquetaMostrarImagen <<<
//        
//        JLabel etiquetaImagen = new JLabel() { 
//            private static final long serialVersionUID = 1L;
//            
//            
//            //TEST
//            @Override
//            protected void paintComponent(Graphics g) {
//            	Tema temaActual = VisorView.this.themeManagerRef.getTemaActual();
//                // ---- DIBUJA TU FONDO PRIMERO ----
//                int width = getWidth();
//                int height = getHeight();
//
//                if (fondoACuadrosActivado) {
//                    Graphics2D g2dFondo = (Graphics2D) g.create();
//                    try {
//                        for (int row = 0; row < height; row += TAMANO_CUADRO) {
//                            for (int col = 0; col < width; col += TAMANO_CUADRO) {
//                                g2dFondo.setColor(
//                                        (((row / TAMANO_CUADRO) % 2) == ((col / TAMANO_CUADRO) % 2)) ? colorCuadroClaro
//                                                : colorCuadroOscuro);
//                                g2dFondo.fillRect(col, row, TAMANO_CUADRO, TAMANO_CUADRO);
//                            }
//                        }
//                    } finally {
//                        g2dFondo.dispose();
//                    }
//                } else {
//                    // Si no es fondo a cuadros, y quieres un color de fondo sólido específico
//                    // que tú controlas, píntalo. Si quieres el fondo por defecto del JLabel,
//                    // entonces setOpaque(true) y deja que el super lo pinte.
//                    // Asumiendo que quieres tu propio color de fondo sólido:
//                    if (temaActual != null) { // O el color que uses como fondo de visor
//                        g.setColor(temaActual.colorFondoSecundario());
//                    } else {
//                        g.setColor(Color.DARK_GRAY); // Tu fallback
//                    }
//                    g.fillRect(0, 0, width, height);
//                }
//
//                // ---- AHORA LLAMA AL SUPER (para que dibuje icono/texto encima de tu fondo) ----
//                super.paintComponent(g);
//
//                // ---- LUEGO DIBUJA LA IMAGEN REESCALADA (si existe, encima de todo) ----
//                if (imagenReescaladaView != null) {
//                    Graphics2D g2dImagen = (Graphics2D) g.create();
//                    try {
//                        g2dImagen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//                        // ... resto de tus hints ...
//                        
//                        int baseW = imagenReescaladaView.getWidth(null);
//                        int baseH = imagenReescaladaView.getHeight(null);
//
//                        if (baseW > 0 && baseH > 0) {
//                            int finalW = (int) (baseW * zoomFactorView);
//                            int finalH = (int) (baseH * zoomFactorView);
//                            int drawX = (width - finalW) / 2 + imageOffsetXView;
//                            int drawY = (height - finalH) / 2 + imageOffsetYView;
//                            g2dImagen.drawImage(imagenReescaladaView, drawX, drawY, finalW, finalH, null);
//                        }
//                    } finally {
//                        g2dImagen.dispose();
//                    }
//                }
//                
//            }// FIN del paintComponent
//            
//        }; // Fin JLabel anónimo
//
//        Tema temaActual = this.themeManagerRef.getTemaActual();
//        this.etiquetaImagen.setHorizontalAlignment(SwingConstants.CENTER);
//        this.etiquetaImagen.setVerticalAlignment(SwingConstants.CENTER);
//        this.etiquetaImagen.setOpaque(false); // Importante: NO opaco para que el fondo pintado se vea
//        if (temaActual != null) {
//            this.etiquetaImagen.setBackground(temaActual.colorFondoSecundario()); // Color base si no es a cuadros
//            this.etiquetaImagen.setForeground(temaActual.colorTextoPrimario());   // Color para texto "Cargando"
//        } else {
//            this.etiquetaImagen.setBackground(Color.DARK_GRAY); // Fallback
//            this.etiquetaImagen.setForeground(Color.WHITE);     // Fallback
//        }
//        System.out.println("      -> EtiquetaImagen configurada por inicializarEtiquetaMostrarImagen.");
//    } // FIN del metodo inicializarEtiquetaMostrarImagen
    
    
    private JPanel createRightSplitComponent() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(themeManager.getTemaActual().colorFondoSecundario());
        registry.register("panel.derecho.visor", rightPanel);

        // 1. Crear el panel de visualización de la imagen
        ImageDisplayPanel imageDisplayPanel = new ImageDisplayPanel(this.themeManager);
        registry.register("panel.display.imagen", imageDisplayPanel);
        registry.register("label.imagenPrincipal", imageDisplayPanel.getInternalLabel());
        
        // Creamos un borde similar al de la lista de archivos para ver los límites.
        TitledBorder border = BorderFactory.createTitledBorder(""); // Le damos un título
        border.setTitleColor(themeManager.getTemaActual().colorBordeTitulo());
        imageDisplayPanel.setBorder(border);
        
        
        rightPanel.add(imageDisplayPanel, BorderLayout.CENTER);

        // 2. Crear el panel de controles de fondo
        JPanel backgroundControlsPanel = createBackgroundControlPanel();
        
        // 3. Crear un panel "wrapper" para alinear los controles a la derecha
        JPanel southWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        southWrapper.setOpaque(false);
        southWrapper.add(backgroundControlsPanel);

        // 4. Añadir el wrapper con los controles a la zona SUR del panel derecho.
        rightPanel.add(southWrapper, BorderLayout.SOUTH);

        return rightPanel;
    } // --- FIN del metodo createRightSplitComponent ---
    

    /**
     * Crea, ensambla y registra el panel de la barra de estado inferior y todos sus componentes.
     * @return El JPanel configurado para la barra de estado inferior.
     */
    private JPanel createBottomStatusBar() {
        System.out.println("    [ViewBuilder] Creando PanelEstadoInferior...");

        JPanel bottomStatusBar = new JPanel(new BorderLayout(5, 0));
        registry.register("panel.estado.inferior", bottomStatusBar);

        Tema tema = themeManager.getTemaActual();
        bottomStatusBar.setBackground(tema.colorFondoSecundario());
        bottomStatusBar.setOpaque(true);

        Border lineaExterna = BorderFactory.createMatteBorder(1, 0, 0, 0, tema.colorBorde());
        Border paddingInterno = BorderFactory.createEmptyBorder(2, 5, 2, 5);
        bottomStatusBar.setBorder(BorderFactory.createCompoundBorder(lineaExterna, paddingInterno));

        // --- Sección Izquierda/Centro (Ruta del archivo) ---
        JLabel rutaCompletaArchivoLabel = new JLabel("Ruta: (ninguna imagen seleccionada)");
        registry.register("label.estado.ruta", rutaCompletaArchivoLabel);
        rutaCompletaArchivoLabel.setForeground(tema.colorTextoPrimario());
        
        JPanel panelRuta = new JPanel(new BorderLayout());
        panelRuta.setOpaque(false);
        panelRuta.add(rutaCompletaArchivoLabel, BorderLayout.CENTER);
        bottomStatusBar.add(panelRuta, BorderLayout.CENTER);

        // --- Sección Derecha (Controles y Mensajes) ---
        JPanel panelDerechoContenedor = new JPanel(new BorderLayout(5, 0));
        panelDerechoContenedor.setOpaque(false);

        // --- Sub-sección de Controles ---
        JPanel panelControlesInferior = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        panelControlesInferior.setOpaque(false);
        registry.register("panel.estado.controles", panelControlesInferior);

        int iconSize = 18;
        Dimension indicadorDimension = new Dimension(iconSize + 6, iconSize + 4);

        ImageIcon iconoZM = iconUtils.getScaledIcon("3001-Zoom_48x48.png", iconSize, iconSize);
        JLabel iconoZoomManualLabel = new JLabel(iconoZM);
        registry.register("label.indicador.zoomManual", iconoZoomManualLabel);
        configurarIndicadorLabel(iconoZoomManualLabel, indicadorDimension, "Zoom Manual: Desactivado");

        ImageIcon iconoProp = iconUtils.getScaledIcon("7002-Mantener_Proporciones_48x48.png", iconSize, iconSize);
        JLabel iconoMantenerProporcionesLabel = new JLabel(iconoProp);
        registry.register("label.indicador.proporciones", iconoMantenerProporcionesLabel);
        configurarIndicadorLabel(iconoMantenerProporcionesLabel, indicadorDimension, "Mantener Proporciones: Desactivado");

        ImageIcon iconoSubC = iconUtils.getScaledIcon("7001-Subcarpetas_48x48.png", iconSize, iconSize);
        JLabel iconoModoSubcarpetasLabel = new JLabel(iconoSubC);
        registry.register("label.indicador.subcarpetas", iconoModoSubcarpetasLabel);
        configurarIndicadorLabel(iconoModoSubcarpetasLabel, indicadorDimension, "Incluir Subcarpetas: Desactivado");

        JLabel porcentajeZoomLabel = new JLabel("Z: 100%");
        registry.register("label.control.zoomPorcentaje", porcentajeZoomLabel);
        // ... (configuración adicional para este label si es necesaria)

        JButton modoZoomBoton = new JButton();
        registry.register("button.control.modoZoom", modoZoomBoton);
        // ... (configuración para este botón)
        
        // Ensamblar controles
        panelControlesInferior.add(iconoZoomManualLabel);
        panelControlesInferior.add(iconoMantenerProporcionesLabel);
        panelControlesInferior.add(iconoModoSubcarpetasLabel);
        panelControlesInferior.add(new JSeparator(SwingConstants.VERTICAL));
        panelControlesInferior.add(porcentajeZoomLabel);
        panelControlesInferior.add(modoZoomBoton);
        
        panelDerechoContenedor.add(panelControlesInferior, BorderLayout.CENTER);

        // --- Sub-sección de Mensajes ---
        JLabel mensajesAppLabel = new JLabel(" ");
        registry.register("label.estado.mensajes", mensajesAppLabel);
        mensajesAppLabel.setForeground(tema.colorTextoSecundario());
        mensajesAppLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mensajesAppLabel.setPreferredSize(new Dimension(200, mensajesAppLabel.getPreferredSize().height));
        panelDerechoContenedor.add(mensajesAppLabel, BorderLayout.EAST);

        bottomStatusBar.add(panelDerechoContenedor, BorderLayout.EAST);

        System.out.println("    [ViewBuilder] PanelEstadoInferior creado y ensamblado.");
        return bottomStatusBar;
        
    } // --- Fin del método createBottomStatusBar ---
    
    

    /**
     * Crea, ensambla y registra el panel de información superior y todos sus componentes.
     * @return El JPanel configurado para la barra de información superior.
     */
    private JPanel createTopInfoPanel() {
        System.out.println("    [ViewBuilder] Creando PanelInfoSuperior...");
        
        // 1. Crear el panel principal y registrarlo
        JPanel panel = new JPanel(new GridBagLayout());
        registry.register("panel.info.superior", panel);

        // 2. Configurar la apariencia base del panel
        Tema tema = themeManager.getTemaActual();
        
        Border paddingParaContenido = BorderFactory.createEmptyBorder(3, 5, 3, 5);
        Border lineaInferiorExterna = BorderFactory.createMatteBorder(2, 2, 2, 2, tema.colorBorde());
        panel.setBorder(BorderFactory.createCompoundBorder(lineaInferiorExterna, paddingParaContenido));
        
        panel.setBackground(tema.colorFondoSecundario());
        panel.setOpaque(true);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.insets = new Insets(0, 3, 0, 3);

        // 3. Crear y registrar los componentes (como variables locales)
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

        // Aplicar color de texto a todos los labels
        Color colorTextoInfo = tema.colorTextoSecundario();
        nombreArchivoInfoLabel.setForeground(colorTextoInfo);
        indiceTotalInfoLabel.setForeground(colorTextoInfo);
        dimensionesOriginalesInfoLabel.setForeground(colorTextoInfo);
        tamanoArchivoInfoLabel.setForeground(colorTextoInfo);
        fechaArchivoInfoLabel.setForeground(colorTextoInfo);
        modoZoomNombreInfoLabel.setForeground(colorTextoInfo);
        porcentajeZoomVisualRealInfoLabel.setForeground(colorTextoInfo);
        formatoImagenInfoLabel.setForeground(colorTextoInfo);

        // 4. Ensamblaje con GridBagLayout
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
        
        System.out.println("    [ViewBuilder] PanelInfoSuperior creado y ensamblado.");
        return panel;
        
    } // --- Fin del método createTopInfoPanel ---

//    // He movido el método de crear separadores aquí también para que sea autónomo
//    private JSeparator createSeparatorForInfoBar() {
//        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
//        Dimension d = new Dimension(2, 18); // Altura fija para consistencia
//        separator.setPreferredSize(d);
//        return separator;
//    } // --- Fin del metodo createSeparatorForInfoBar ---
    
    
    /**
     * Método helper para aplicar una configuración estándar a los JLabels
     * que actúan como indicadores en la barra de estado.
     */
    private void configurarIndicadorLabel(JLabel label, Dimension dim, String tooltip) {
        label.setOpaque(true);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setPreferredSize(dim);
        label.setToolTipText(tooltip);
    } // --- Fin del método configurarIndicadorLabel ---

    
// **************************************************************************************************** PUNTOS DE COLOR DEL FONDO
    
    private Color aclararColor(Color colorOriginal, int cantidadAclarar) {
        if (colorOriginal == null) return Color.LIGHT_GRAY;
        int r = Math.min(255, colorOriginal.getRed() + cantidadAclarar);
        int g = Math.min(255, colorOriginal.getGreen() + cantidadAclarar);
        int b = Math.min(255, colorOriginal.getBlue() + cantidadAclarar);
        return new Color(r, g, b);
    } // --- FIN del metodo aclararColor ---

    private Color oscurecerColor(Color colorOriginal, int cantidadOscurecer) {
        if (colorOriginal == null) return Color.DARK_GRAY;
        int r = Math.max(0, colorOriginal.getRed() - cantidadOscurecer);
        int g = Math.max(0, colorOriginal.getGreen() - cantidadOscurecer);
        int b = Math.max(0, colorOriginal.getBlue() - cantidadOscurecer);
        return new Color(r, g, b);
    } // --- FIN del metodo oscurecerColor ---

    /**
     * Crea el panel que contiene los selectores de color para el fondo del visor.
     * Este método ahora pertenece al ViewBuilder.
     * @return El JPanel con los controles de color.
     */
    private JPanel createBackgroundControlPanel() {
        if (this.themeManager == null || this.iconUtils == null) {
            System.err.println("ERROR [ViewBuilder.createBackgroundControlPanel]: ThemeManager o IconUtils son nulos.");
            return new JPanel();
        }

        JPanel panelControlesFondoIcono = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 1)); // ALINEADO A LA DERECHA
        panelControlesFondoIcono.setOpaque(false);
        registry.register("panel.controles.fondo", panelControlesFondoIcono);

        Border bordePuntoNormal = BorderFactory.createLineBorder(Color.GRAY);
        Border bordePuntoSeleccionado = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.CYAN, 2),
            BorderFactory.createLineBorder(Color.BLACK)
        );
        
        String[] nombresTemas = {"clear", "dark", "blue", "orange", "green"};
        String[] tooltipsTemas = { "Fondo Tema Claro", "Fondo Tema Oscuro", "Fondo Tema Azul", "Fondo Tema Naranja", "Fondo Tema Verde" };

        for (int i = 0; i < nombresTemas.length; i++) {
            Color colorFondoOriginalTema = this.themeManager.getFondoSecundarioParaTema(nombresTemas[i]);
            Color colorParaElPuntoSelector;
            int luminosidad = colorFondoOriginalTema.getRed() + colorFondoOriginalTema.getGreen() + colorFondoOriginalTema.getBlue();
            
            if (luminosidad < 150) colorParaElPuntoSelector = aclararColor(colorFondoOriginalTema, 70);
            else if (luminosidad > 600) colorParaElPuntoSelector = oscurecerColor(colorFondoOriginalTema, 70);
            else colorParaElPuntoSelector = colorFondoOriginalTema;
            
            JPanel punto = crearPuntoSelectorFondo(colorFondoOriginalTema, tooltipsTemas[i], false, false, nombresTemas[i], colorParaElPuntoSelector, bordePuntoNormal, bordePuntoSeleccionado);
            panelControlesFondoIcono.add(punto);
        }
        
        JPanel puntoCuadros = crearPuntoSelectorFondo(null, "Fondo a Cuadros", true, false, "cuadros", null, bordePuntoNormal, bordePuntoSeleccionado);
        panelControlesFondoIcono.add(puntoCuadros);

        JPanel puntoPersonalizado = crearPuntoSelectorFondo(null, "Seleccionar Color Personalizado...", false, true, "personalizado", null, bordePuntoNormal, bordePuntoSeleccionado);
        panelControlesFondoIcono.add(puntoPersonalizado);
        
        System.out.println("  [ViewBuilder] Panel de Controles de Fondo de Icono creado.");
        return panelControlesFondoIcono;
    } // --- FIN del metodo createBackgroundControlPanel ---

    private JPanel crearPuntoSelectorFondo(
            final java.awt.Color colorQueAplicaAlPreview,
            String tooltip,
            final boolean esSelectorCuadros,
            final boolean esSelectorPersonalizado,
            String identificadorPunto,
            final java.awt.Color colorParaPintarElSelectorPropio,
            final javax.swing.border.Border bordeNormal,
            final javax.swing.border.Border bordePuntoSeleccionado) {

        // --- La creación del JPanel 'punto' se mantiene exactamente igual ---
        final JPanel punto = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                if (esSelectorCuadros && isVisible()) {
                    java.awt.Graphics2D g2d = (java.awt.Graphics2D) g.create();
                    int w = getWidth(); int h = getHeight();
                    int tamCuadroPunto = Math.max(2, Math.min(w, h) / 4);
                    for (int row = 0; row < h; row += tamCuadroPunto) {
                        for (int col = 0; col < w; col += tamCuadroPunto) {
                            g2d.setColor((((row / tamCuadroPunto) % 2) == ((col / tamCuadroPunto) % 2)) ? java.awt.Color.WHITE : java.awt.Color.LIGHT_GRAY);
                            g2d.fillRect(col, row, tamCuadroPunto, tamCuadroPunto);
                        }
                    }
                    g2d.dispose();
                }
            }
        };
        punto.setPreferredSize(new java.awt.Dimension(16, 16));
        punto.setToolTipText(tooltip);
        punto.setBorder(bordeNormal);
        punto.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        punto.setName(identificadorPunto);
        punto.setOpaque(true);

        if (esSelectorPersonalizado) {
            punto.setLayout(new java.awt.BorderLayout());
            punto.setBackground(new java.awt.Color(220, 220, 220));
            if (this.iconUtils != null) {
                javax.swing.ImageIcon paletteIcon = this.iconUtils.getScaledCommonIcon("Paint-Palette--Streamline-Core.png", 10, 10);
                if (paletteIcon != null) punto.add(new javax.swing.JLabel(paletteIcon), java.awt.BorderLayout.CENTER);
            }
        } else if (esSelectorCuadros) {
            punto.setBackground(java.awt.Color.LIGHT_GRAY);
        } else {
            punto.setBackground(colorParaPintarElSelectorPropio != null ? colorParaPintarElSelectorPropio : colorQueAplicaAlPreview);
        }
        // --- Fin de la creación del JPanel 'punto' ---


        // --- INICIO DEL CÓDIGO MODIFICADO ---
        punto.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // Asegurarse de que el ViewManager ha sido inyectado
                if (ViewBuilder.this.viewManager == null) {
                    System.err.println("ERROR [ViewBuilder]: ViewManager no ha sido inyectado. No se puede cambiar el fondo.");
                    return;
                }
                
                // Lógica de resalte (responsabilidad de la vista)
                JPanel parentContainer = (JPanel) punto.getParent();
                for (java.awt.Component comp : parentContainer.getComponents()) {
                    if (comp instanceof JPanel) ((JPanel) comp).setBorder(bordeNormal);
                }
                punto.setBorder(bordePuntoSeleccionado);
                
                // Notificar al ViewManager para que ejecute la acción
                if (esSelectorPersonalizado) {
                    ViewBuilder.this.viewManager.requestCustomBackgroundColor();
                } else if (esSelectorCuadros) {
                    ViewBuilder.this.viewManager.setSessionCheckeredBackground();
                } else {
                    ViewBuilder.this.viewManager.setSessionBackgroundColor(colorQueAplicaAlPreview);
                }
            }
        });
        // --- FIN DEL CÓDIGO MODIFICADO ---

        return punto;
    } // --- Fin del método crearPuntoSelectorFondo ---
    
//    private JPanel crearPuntoSelectorFondo(
//            final Color colorQueAplicaAlPreview,
//            String tooltip,
//            final boolean esSelectorCuadros,
//            final boolean esSelectorPersonalizado,
//            String identificadorPunto,
//            final Color colorParaPintarElSelectorPropio,
//            final Border bordeNormal,
//            final Border bordePuntoSeleccionado) {
//
//        final JPanel punto = new JPanel() {
//            private static final long serialVersionUID = 1L;
//            @Override
//            protected void paintComponent(Graphics g) {
//                super.paintComponent(g);
//                if (esSelectorCuadros && isVisible()) {
//                    Graphics2D g2d = (Graphics2D) g.create();
//                    int w = getWidth(); int h = getHeight();
//                    int tamCuadroPunto = Math.max(2, Math.min(w, h) / 4);
//                    for (int row = 0; row < h; row += tamCuadroPunto) {
//                        for (int col = 0; col < w; col += tamCuadroPunto) {
//                            g2d.setColor((((row / tamCuadroPunto) % 2) == ((col / tamCuadroPunto) % 2)) ? Color.WHITE : Color.LIGHT_GRAY);
//                            g2d.fillRect(col, row, tamCuadroPunto, tamCuadroPunto);
//                        }
//                    }
//                    g2d.dispose();
//                }
//            }
//        };
//        punto.setPreferredSize(new Dimension(16, 16));
//        punto.setToolTipText(tooltip);
//        punto.setBorder(bordeNormal);
//        punto.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
//        punto.setName(identificadorPunto);
//        punto.setOpaque(true);
//
//        if (esSelectorPersonalizado) {
//            punto.setLayout(new BorderLayout());
//            punto.setBackground(new Color(220, 220, 220));
//            if (this.iconUtils != null) {
//                ImageIcon paletteIcon = this.iconUtils.getScaledCommonIcon("Paint-Palette--Streamline-Core.png", 10, 10);
//                if (paletteIcon != null) punto.add(new JLabel(paletteIcon), BorderLayout.CENTER);
//            }
//        } else if (esSelectorCuadros) {
//            punto.setBackground(Color.LIGHT_GRAY);
//        } else {
//            punto.setBackground(colorParaPintarElSelectorPropio != null ? colorParaPintarElSelectorPropio : colorQueAplicaAlPreview);
//        }
//
//        // --- MOUSE LISTENER CORREGIDO ---
//        punto.addMouseListener(new java.awt.event.MouseAdapter() {
//            @Override
//            public void mouseClicked(java.awt.event.MouseEvent e) {
//                // 1. Obtener el panel de visualización directamente del registro.
//                ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
//                if (displayPanel == null) {
//                    System.err.println("ERROR [ViewBuilder]: 'panel.display.imagen' no encontrado en el registro.");
//                    return;
//                }
//                
//                // 2. Lógica para resaltar el punto clicado
//                JPanel parentContainer = (JPanel) punto.getParent();
//                for (Component comp : parentContainer.getComponents()) {
//                    if (comp instanceof JPanel) ((JPanel) comp).setBorder(bordeNormal);
//                }
//                punto.setBorder(bordePuntoSeleccionado);
//                
//                // 3. Lógica para aplicar el cambio de fondo, llamando a los métodos de ImageDisplayPanel
//                if (esSelectorPersonalizado) {
//                    Color colorActual = displayPanel.getBackground();
//                    Color colorElegido = JColorChooser.showDialog(SwingUtilities.getWindowAncestor(punto), "Seleccionar Color de Fondo", colorActual);
//                    if (colorElegido != null) {
//                        displayPanel.setSolidBackgroundColor(colorElegido);
//                    }
//                } else if (esSelectorCuadros) {
//                    displayPanel.setCheckeredBackground(true);
//                } else {
//                    displayPanel.setSolidBackgroundColor(colorQueAplicaAlPreview);
//                }
//            }
//        });
//        // --- FIN MOUSE LISTENER ---
//
//        return punto;
//    } // --- FIN del metodo crearPuntoSelectorFondo ---

    
// ************************************************************************************************* FIN PUNTOS DE COLOR DEL FONDO
    
// ************************************************************************************************** GETTERS Y SETTERS
    
    public void setViewManager(ViewManager viewManager) {this.viewManager = viewManager;} 
    
} // --- Fin de la clase ViewBuilder ---