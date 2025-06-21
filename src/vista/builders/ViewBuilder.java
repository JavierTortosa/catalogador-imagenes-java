package vista.builders;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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
    private final VisorModel model;
    private final ThemeManager themeManager;
    private final ConfigurationManager configuration;
    private final VisorController controller; // Para listeners de fallback
    private final IconUtils iconUtils;         // Para builders de UI
    private final UIDefinitionService uiDefService; // Para definiciones de UI
    private final ThumbnailService thumbnailService;
    private final ProjectBuilder projectBuilder;
    private ViewManager viewManager;
    
    public ViewBuilder(
            ComponentRegistry registry,
//            ActionFactory actionFactory,
            VisorModel model,
            ThemeManager themeManager,
            ConfigurationManager configuration,
            VisorController controller,
            IconUtils iconUtils,
            ThumbnailService thumbnailService,
            ProjectBuilder projectBuilder
//            ,ViewManager viewManager 
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
        this.projectBuilder = Objects.requireNonNull(projectBuilder, "ProjectBuilder no puede ser null");
//        this.viewManager = Objects.requireNonNull(viewManager, "ViewManager no puede ser null");
    } // --- Fin del constructor ViewBuilder ---


    /**
     * MÉTODO MODIFICADO: Ahora construye la UI principal con una estructura de CardLayout
     * para manejar múltiples vistas (Visualizador, Proyectos, etc.).
     */
    public VisorView createMainFrame() {
        System.out.println("  [ViewBuilder] Iniciando la construcción del frame principal con estructura CardLayout...");

        // --- FASE 1: Crear el contenedor principal (VisorView) ---
        VisorView mainFrame = new VisorView(
            100, // miniaturaPanelHeight, que podría ser obsoleto o redefinido
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
        
        // --- 2a. ZONA NORTE (Barras de herramientas e info) ---
        // Esta zona es común a todas las vistas, por lo que se añade directamente al JFrame.
        JPanel toolbarContainer = createToolbarContainer();
        JPanel topInfoPanel = createTopInfoPanel();
        JPanel northWrapper = new JPanel(new BorderLayout());
        northWrapper.add(toolbarContainer, BorderLayout.NORTH);
        northWrapper.add(topInfoPanel, BorderLayout.CENTER);
        mainFrame.add(northWrapper, BorderLayout.NORTH);
        
        // --- 2b. ZONA SUR (Barra de estado) ---
        // También es común a todas las vistas.
        JPanel bottomStatusBar = createBottomStatusBar();
        mainFrame.add(bottomStatusBar, BorderLayout.SOUTH);
        
        // --- 2c. ZONA CENTRAL (El Contenedor de Vistas con CardLayout) ---
        // Este panel usará CardLayout para cambiar entre la vista del visualizador y la de proyectos.
        JPanel vistasContainer = new JPanel(new CardLayout());
        registry.register("container.vistas", vistasContainer);

        // -- Construir la tarjeta/vista del "Visualizador" --
        // Delegamos su construcción a un método helper para mantener el código limpio.
        JPanel panelVisualizador = createVisualizadorViewPanel();
        
        // -- Construir la tarjeta/vista de "Proyectos" --
        // Delegamos su construcción al ProjectBuilder que recibimos en el constructor.
        JPanel panelProyectos = this.projectBuilder.buildProjectViewPanel();

        // -- Añadir las tarjetas al contenedor --
        // Les damos nombres únicos en mayúsculas para usarlos como identificadores.
        vistasContainer.add(panelVisualizador, "VISTA_VISUALIZADOR");
        vistasContainer.add(panelProyectos, "VISTA_PROYECTOS");

        // -- Añadir el contenedor de vistas al centro del JFrame --
        mainFrame.add(vistasContainer, BorderLayout.CENTER);

        // --- FASE 3: Lógica de configuración final de la ventana ---
        // (Esta parte no cambia)
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
        
        // ¡IMPORTANTE! La lógica del divisor del JSplitPane AHORA debe ejecutarse
        // después de que el panel del visualizador (y por tanto el splitpane) sea visible.
        // Lo dejamos aquí, ya que la vista del visualizador es la que se muestra por defecto.
        SwingUtilities.invokeLater(() -> {
            JSplitPane splitPane = registry.get("splitpane.main");
            if (splitPane != null) {
                double dividerLocation = configuration.getDouble("ui.splitpane.main.dividerLocation", 0.25);
                splitPane.setDividerLocation(dividerLocation);
            }
        });

        System.out.println("  [ViewBuilder] Frame principal construido y ensamblado.");
        return mainFrame;
    }
    
    /**
     * NUEVO MÉTODO HELPER: Construye y devuelve el panel completo para la vista "Visualizador".
     * Contiene la lógica que antes estaba en el centro del JFrame.
     */
    private JPanel createVisualizadorViewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // El método createMainContentPanel() ahora es redundante.
        // Ensamblamos sus partes directamente aquí.
        panel.add(createMainSplitPane(), BorderLayout.CENTER);
        panel.add(createThumbnailScrollPane(), BorderLayout.SOUTH);
        
        return panel;
    }


    // --- MÉTODOS DE CONSTRUCCIÓN PRIVADOS ---


    private JPanel createToolbarContainer() {
        JPanel toolbarContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolbarContainer.setOpaque(false);
        registry.register("container.toolbars", toolbarContainer);

        // La población real de las JToolBar la haría un ToolbarManager, que se crearía
        // en el AppInitializer y usaría este contenedor. El ViewBuilder solo crea el contenedor.
        
        return toolbarContainer;
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
    


    
// ************************************************************************************************* FIN PUNTOS DE COLOR DEL FONDO
    
// ************************************************************************************************** GETTERS Y SETTERS
    
    //public void setViewManager(ViewManager viewManager) {this.viewManager = viewManager;} 
    public void setViewManager(ViewManager viewManager) {
        this.viewManager = Objects.requireNonNull(viewManager, "ViewManager no puede ser null en el setter");
    }
} // --- Fin de la clase ViewBuilder ---