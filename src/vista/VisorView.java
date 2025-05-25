package vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics; // Para crearSeparadorVerticalBarraInfo
// Imports para el paintComponent de etiquetaImagen (si está aquí)
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image; // Necesario para el campo imagenReescaladaView
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField; // Mantenido si textoRuta se usa como fallback
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border; // Para crearSeparadorVerticalBarraInfo y otros bordes
import javax.swing.border.TitledBorder; // Para panelIzquierdo

import modelo.VisorModel;
import servicios.image.ThumbnailService;
import vista.config.ViewUIConfig;
import vista.renderers.MiniaturaListCellRenderer; // Asumiendo que lo usas en inicializarPanelImagenesMiniatura
import vista.renderers.NombreArchivoRenderer; // Asumiendo que lo usas en inicializarPanelIzquierdo


public class VisorView extends JFrame {

    private static final long serialVersionUID = 4L; // Incrementado por refactorización mayor

    // --- CAMPOS DE INSTANCIA PARA COMPONENTES UI PRINCIPALES ---
    // Paneles Contenedores Principales
    private JPanel panelModoVisualizadorActual; 
    private JPanel panelInfoSuperior;
    private JPanel bottomStatusBar; 

    // Componentes del Modo Normal/Detalle
    private JSplitPane splitPane;
    private JPanel panelContenedorIzquierdoSplit;
    private JPanel panelContenedorDerechoSplit;
    private JPanel panelIzquierdo; 
    private JList<String> listaNombres;
    private JLabel etiquetaImagen;
    private JScrollPane scrollListaMiniaturas;
    private JList<String> listaMiniaturas;

    // Componentes Comunes (Barras, etc.)
    private final JMenuBar mainMenuBar;       
    private final JPanel mainToolbarPanel;  
    private JPanel panelDeBotones;          

    // Componentes de las Barras de Información
    private JLabel nombreArchivoInfoLabel;
    private JLabel indiceTotalInfoLabel;
    private JLabel dimensionesOriginalesInfoLabel;
    private JLabel modoZoomNombreInfoLabel;
    private JLabel porcentajeZoomVisualRealInfoLabel;
    private JLabel indicadorZoomManualInfoLabel;
    private JLabel indicadorMantenerPropInfoLabel;
    private JLabel indicadorSubcarpetasInfoLabel;

    // Componentes de la Barra de Estado Inferior (bottomStatusBar)
    private JLabel rutaCompletaArchivoLabel; 
    private JLabel mensajesAppLabel;
    private JTextField textoRuta; // Usado como fallback temporalmente en crearPanelEstadoInferior

    // --- REFERENCIAS EXTERNAS Y CONFIGURACIÓN ---
    private final ViewUIConfig uiConfig;
    private final VisorModel model;
    private final ThumbnailService servicioThumbs;
    private Map<String, JMenuItem> menuItemsPorNombre; 
    private Map<String, JButton> botonesPorNombre;   
    private int miniaturaScrollPaneHeight;
    
    // --- Estado Interno de la Vista (para pintura, etc.) ---
    private Image imagenReescaladaView; // Usada por paintComponent de etiquetaImagen
    private double zoomFactorView = 1.0;
    private int imageOffsetXView = 0;
    private int imageOffsetYView = 0;
    private boolean fondoACuadrosActivado = false;
    private final Color colorCuadroClaro = new Color(204, 204, 204); // Podrían venir de uiConfig/Tema
    private final Color colorCuadroOscuro = new Color(255, 255, 255);
    private final int TAMANO_CUADRO = 16;


    /**
     * Constructor principal de la ventana VisorView.
     * Inicializa las dependencias y llama al método de construcción de la UI.
     *
     * @param miniaturaPanelHeight Altura deseada para el scrollpane de miniaturas.
     * @param config Objeto ViewUIConfig con parámetros de apariencia.
     * @param modelo El VisorModel principal.
     * @param servicioThumbs El ThumbnailService para renderers.
     * @param menuBar La JMenuBar pre-construida.
     * @param toolbarPanel El JPanel de la toolbar pre-construido.
     * @param menuItems Mapa de JMenuItems generados.
     * @param botones Mapa de JButtons generados.
     */
    public VisorView(
            int miniaturaPanelHeight,
            ViewUIConfig config,
            VisorModel modelo,
            ThumbnailService servicioThumbs,
            JMenuBar menuBar,
            JPanel toolbarPanel,
            Map<String, JMenuItem> menuItems,
            Map<String, JButton> botones
    ) {
        super("Visor/Catalogador de Imágenes"); 
        System.out.println("[VisorView Constructor] Iniciando...");

        this.uiConfig = Objects.requireNonNull(config, "ViewUIConfig no puede ser null");
        this.model = Objects.requireNonNull(modelo, "VisorModel no puede ser null");
        this.servicioThumbs = Objects.requireNonNull(servicioThumbs, "ThumbnailService no puede ser null");
        this.miniaturaScrollPaneHeight = miniaturaPanelHeight > 0 ? miniaturaPanelHeight : 100;

        this.mainMenuBar = menuBar;
        this.mainToolbarPanel = toolbarPanel;
        this.panelDeBotones = this.mainToolbarPanel; 
        this.menuItemsPorNombre = (menuItems != null) ? menuItems : new HashMap<>();
        this.botonesPorNombre = (botones != null) ? botones : new HashMap<>();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout()); 
        if (this.uiConfig != null && this.uiConfig.colorFondoPrincipal != null) {
            getContentPane().setBackground(this.uiConfig.colorFondoPrincipal);
        } else {
            getContentPane().setBackground(Color.LIGHT_GRAY); // Fallback
        }

        // Llamada al método de inicialización refactorizado
        inicializarComponentes(this.model.getModeloLista());

        // --- Restaurar Estado de la Ventana (COPIA TU LÓGICA AQUÍ) ---
        // Ejemplo simplificado (debes usar tu lógica completa de ConfigurationManager):
        if (this.uiConfig.configurationManager != null) {
            boolean wasMaximized = this.uiConfig.configurationManager.getBoolean("window.maximized", false);
            if (wasMaximized) {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
            } else {
                int x = this.uiConfig.configurationManager.getInt("window.x", -1);
                int y = this.uiConfig.configurationManager.getInt("window.y", -1);
                int w = this.uiConfig.configurationManager.getInt("window.width", 1280); // Default más grande
                int h = this.uiConfig.configurationManager.getInt("window.height", 720); // Default más grande
                
                if (w > 50 && h > 50 && x != -1 && y != -1) { // Chequeo básico
                     setBounds(x, y, w, h);
                } else {
                     setSize(w,h);
                     setLocationRelativeTo(null); // Centrar si no hay posición guardada
                }
            }
        } else {
            setSize(1280, 720);
            setLocationRelativeTo(null);
        }
        
        // Ajuste final del divisor del JSplitPane
        SwingUtilities.invokeLater(() -> {
            if (this.splitPane != null) {
                // Podrías leer esta posición inicial de la configuración también
                this.splitPane.setDividerLocation(0.25); 
                System.out.println("  [Constructor EDT] Divisor JSplitPane establecido.");
            }
        });

        System.out.println("[VisorView Constructor] Finalizado.");
        // setVisible(true) será llamado por AppInitializer o VisorController
    }

    /**
     * Orquesta la creación y ensamblaje de todos los componentes de la UI.
     * @param modeloNombresLista El DefaultListModel para la lista de nombres de archivo.
     */
    private void inicializarComponentes(DefaultListModel<String> modeloNombresLista) {
        System.out.println("--- [VisorView] Iniciando inicializarComponentes (Modular) ---");

        // --- FASE 1: Creación y Configuración de Paneles y Componentes Individuales ---
        this.panelInfoSuperior = crearPanelInfoSuperior();
        this.panelModoVisualizadorActual = crearPanelModoNormal(modeloNombresLista);
        this.bottomStatusBar = crearPanelEstadoInferior();

        // --- FASE 2: Ensamblaje de la Jerarquía Visual Principal ---
        ensamblarUIGeneral(this.panelModoVisualizadorActual);

        System.out.println("--- [VisorView] Fin inicializarComponentes (Modular) ---");
    }

    /**
     * Crea y ensambla el panel principal para el modo de visualización "Normal" o "Detalle".
     * Este panel contiene el JSplitPane (con la lista de nombres y el visor de imagen)
     * y la barra de miniaturas debajo.
     * @param modeloNombresLista El DefaultListModel para la lista de nombres.
     * @return El JPanel raíz para el modo normal/detalle.
     */
    private JPanel crearPanelModoNormal(DefaultListModel<String> modeloNombresLista) {
        System.out.println("  [VisorView] Creando PanelModoNormal...");
        JPanel panelModoNormalPrincipal = new JPanel(new BorderLayout(0, 0));
        if (this.uiConfig != null && this.uiConfig.colorFondoPrincipal != null) {
            panelModoNormalPrincipal.setBackground(this.uiConfig.colorFondoPrincipal);
            panelModoNormalPrincipal.setOpaque(true);
        }

        // 1. Crear contenedores para el SplitPane
        this.panelContenedorIzquierdoSplit = crearPanelContenedorParaSplit("Izquierdo (Modo Normal)");
        this.panelContenedorDerechoSplit = crearPanelContenedorParaSplit("Derecho (Modo Normal)");

        // 2. Configurar los componentes que van DENTRO de los contenedores del SplitPane
        configurarComponentesDelSplitIzquierdo(modeloNombresLista);
        configurarComponentesDelSplitDerecho();

        // 3. Crear y configurar el JSplitPane
        this.splitPane = crearYConfigurarSplitPane(this.panelContenedorIzquierdoSplit, this.panelContenedorDerechoSplit);
        
        // 4. Configurar el panel de miniaturas
        configurarPanelDeMiniaturas();

        // 5. Ensamblar los componentes DENTRO de panelModoNormalPrincipal
        if (this.splitPane != null) {
            panelModoNormalPrincipal.add(this.splitPane, BorderLayout.CENTER);
        } else { System.err.println("WARN [crearPanelModoNormal]: splitPane es nulo, no se pudo añadir.");}
        
        if (this.scrollListaMiniaturas != null) {
            panelModoNormalPrincipal.add(this.scrollListaMiniaturas, BorderLayout.SOUTH);
        } else { System.err.println("WARN [crearPanelModoNormal]: scrollListaMiniaturas es nulo, no se pudo añadir.");}
        
        System.out.println("  [VisorView] PanelModoNormal creado y ensamblado.");
        return panelModoNormalPrincipal;
    }

    /**
     * Ensambla la estructura general de la UI del JFrame, colocando la barra de menú,
     * la barra de botones, el panel de información superior, el panel del modo activo
     * y la barra de estado inferior.
     * @param panelDelModoActivo El JPanel que representa el contenido del modo actual (ej. visualizador, catalogador).
     */
    private void ensamblarUIGeneral(JPanel panelDelModoActivo) {
        System.out.println("  [VisorView] Iniciando ensamblarUIGeneral...");

        // 1. Establecer Layout del JFrame (asegurado en el constructor)
        // this.setLayout(new BorderLayout()); 

        // 2. Añadir Barra de Menú
        if (this.mainMenuBar != null) {
            setJMenuBar(this.mainMenuBar);
            System.out.println("    -> Barra de Menú ensamblada.");
        }

        // 3. Añadir Barra de Botones Principal
        if (this.mainToolbarPanel != null) {
            add(this.mainToolbarPanel, BorderLayout.NORTH);
            System.out.println("    -> Barra de Botones Principal ensamblada en JFrame.NORTH.");
        }

        // 4. Crear y Ensamblar el Panel que contiene la Barra de Info Superior y el Modo Activo
        JPanel topAndCenterWrapper = new JPanel(new BorderLayout(0, 0));
        if (this.uiConfig != null && this.uiConfig.colorFondoPrincipal != null) {
            topAndCenterWrapper.setBackground(this.uiConfig.colorFondoPrincipal);
            topAndCenterWrapper.setOpaque(true);
        }

        //   4a. Añadir Barra de Información Superior
        if (this.panelInfoSuperior != null) {
            topAndCenterWrapper.add(this.panelInfoSuperior, BorderLayout.NORTH);
            System.out.println("    -> PanelInfoSuperior ensamblado en topAndCenterWrapper.NORTH.");
        }

        //   4b. Añadir Panel del Modo Activo
        if (panelDelModoActivo != null) {
            topAndCenterWrapper.add(panelDelModoActivo, BorderLayout.CENTER);
            System.out.println("    -> PanelDelModoActivo ensamblado en topAndCenterWrapper.CENTER.");
        }
        add(topAndCenterWrapper, BorderLayout.CENTER); // Añadir este wrapper al centro del JFrame
        System.out.println("    -> topAndCenterWrapper ensamblado en JFrame.CENTER.");

        // 5. Añadir Barra de Estado Inferior
        if (this.bottomStatusBar != null) { 
            add(this.bottomStatusBar, BorderLayout.SOUTH);
            System.out.println("    -> PanelEstadoInferior ensamblado en JFrame.SOUTH.");
        }
        System.out.println("  [VisorView] Fin ensamblarUIGeneral.");
    }

    /**
     * Crea y configura el panel de información superior.
     * @return El JPanel configurado para la barra de información superior.
     */
    private JPanel crearPanelInfoSuperior() {
        System.out.println("    [VisorView] Creando PanelInfoSuperior...");
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        if (this.uiConfig != null && this.uiConfig.colorFondoSecundario != null) {
            panel.setBackground(this.uiConfig.colorFondoSecundario);
        } else {
            panel.setBackground(Color.GRAY); // Fallback
        }
        panel.setOpaque(true);

        // Inicializar JLabels que son campos de instancia
        this.nombreArchivoInfoLabel = new JLabel("Archivo: N/A");
        this.indiceTotalInfoLabel = new JLabel("0/0");
        this.dimensionesOriginalesInfoLabel = new JLabel("Dim: N/A");
        this.modoZoomNombreInfoLabel = new JLabel("Modo Zoom: N/A");
        this.porcentajeZoomVisualRealInfoLabel = new JLabel("Zoom: N/A");
        this.indicadorZoomManualInfoLabel = new JLabel("ZM:OFF");
        this.indicadorMantenerPropInfoLabel = new JLabel("Prop:OFF");
        this.indicadorSubcarpetasInfoLabel = new JLabel("SubC:OFF");

        // Aplicar colores de texto del tema
        Color colorTextoInfo = (this.uiConfig != null && this.uiConfig.colorTextoSecundario != null)
                                ? this.uiConfig.colorTextoSecundario : Color.BLACK;
        this.nombreArchivoInfoLabel.setForeground(colorTextoInfo);
        this.indiceTotalInfoLabel.setForeground(colorTextoInfo);
        this.dimensionesOriginalesInfoLabel.setForeground(colorTextoInfo);
        this.modoZoomNombreInfoLabel.setForeground(colorTextoInfo);
        this.porcentajeZoomVisualRealInfoLabel.setForeground(colorTextoInfo);
        this.indicadorZoomManualInfoLabel.setForeground(colorTextoInfo);
        this.indicadorMantenerPropInfoLabel.setForeground(colorTextoInfo);
        this.indicadorSubcarpetasInfoLabel.setForeground(colorTextoInfo);

        // Añadir JLabels al panel con GridBagLayout
        GridBagConstraints gbcInfo = new GridBagConstraints();
        gbcInfo.gridx = 0; gbcInfo.gridy = 0; gbcInfo.anchor = GridBagConstraints.LINE_START; gbcInfo.insets = new Insets(0, 0, 0, 5); panel.add(this.nombreArchivoInfoLabel, gbcInfo);
        gbcInfo.gridx++; panel.add(crearSeparadorVerticalBarraInfo(), gbcInfo);
        gbcInfo.gridx++; panel.add(this.indiceTotalInfoLabel, gbcInfo);
        gbcInfo.gridx++; panel.add(crearSeparadorVerticalBarraInfo(), gbcInfo);
        gbcInfo.gridx++; panel.add(this.dimensionesOriginalesInfoLabel, gbcInfo);
        
        gbcInfo.gridx++; gbcInfo.weightx = 1.0; gbcInfo.fill = GridBagConstraints.HORIZONTAL; panel.add(Box.createHorizontalGlue(), gbcInfo);
        gbcInfo.weightx = 0.0; gbcInfo.fill = GridBagConstraints.NONE; 

        gbcInfo.gridx++; gbcInfo.insets = new Insets(0, 5, 0, 5); panel.add(this.modoZoomNombreInfoLabel, gbcInfo);
        gbcInfo.gridx++; panel.add(crearSeparadorVerticalBarraInfo(), gbcInfo);
        gbcInfo.gridx++; panel.add(this.porcentajeZoomVisualRealInfoLabel, gbcInfo);
        gbcInfo.gridx++; panel.add(crearSeparadorVerticalBarraInfo(), gbcInfo);
        gbcInfo.gridx++; panel.add(this.indicadorZoomManualInfoLabel, gbcInfo);
        gbcInfo.gridx++; gbcInfo.insets = new Insets(0, 2, 0, 5); panel.add(this.indicadorMantenerPropInfoLabel, gbcInfo);
        gbcInfo.gridx++; panel.add(this.indicadorSubcarpetasInfoLabel, gbcInfo);
        
        System.out.println("    [VisorView] PanelInfoSuperior creado.");
        return panel;
    }

    /**
     * Crea un panel contenedor simple para ser usado en uno de los lados del JSplitPane.
     * @param nombreParaLog Un identificador para los mensajes de log.
     * @return Un nuevo JPanel configurado con BorderLayout.
     */
    private JPanel crearPanelContenedorParaSplit(String nombreParaLog) {
        System.out.println("    [VisorView] Creando PanelContenedorParaSplit (" + nombreParaLog + ")...");
        JPanel panel = new JPanel(new BorderLayout());
        if (this.uiConfig != null && this.uiConfig.colorFondoPrincipal != null) {
            panel.setBackground(this.uiConfig.colorFondoPrincipal);
            panel.setOpaque(true);
        }
        return panel;
    }

    /**
     * Configura los componentes para el lado izquierdo del JSplitPane (lista de nombres).
     * Inicializa this.panelIzquierdo y this.listaNombres, y añade panelIzquierdo
     * a this.panelContenedorIzquierdoSplit.
     * @param modeloNombresLista El DefaultListModel para la lista de nombres.
     */
    private void configurarComponentesDelSplitIzquierdo(DefaultListModel<String> modeloNombresLista) {
        System.out.println("    [VisorView] Configurando ComponentesDelSplitIzquierdo...");
        
        // --- 1. LLAMAR A TU LÓGICA EXISTENTE (inicializarPanelIzquierdo) ---
        // Este método debe configurar this.panelIzquierdo y this.listaNombres.
        inicializarPanelIzquierdo(this.model, modeloNombresLista); 

        // --- 2. AÑADIR EL panelIzquierdo CONFIGURADO AL CONTENEDOR DEL SPLIT ---
        if (this.panelIzquierdo != null && this.panelContenedorIzquierdoSplit != null) {
            this.panelContenedorIzquierdoSplit.add(this.panelIzquierdo, BorderLayout.CENTER);
            System.out.println("      -> panelIzquierdo añadido a panelContenedorIzquierdoSplit.");
        } else {
            System.err.println("ERROR CRÍTICO: panelIzquierdo o panelContenedorIzquierdoSplit nulos en configurarComponentesDelSplitIzquierdo.");
            // Considera crear un panel de error como fallback si esto puede ocurrir
        }
    }

    /**
     * Configura los componentes para el lado derecho del JSplitPane (visor de imagen).
     * Inicializa this.etiquetaImagen y la añade a this.panelContenedorDerechoSplit.
     */
    private void configurarComponentesDelSplitDerecho() {
        System.out.println("    [VisorView] Configurando ComponentesDelSplitDerecho...");

        // --- 1. LLAMAR A TU LÓGICA EXISTENTE (inicializarEtiquetaMostrarImagen) ---
        // Este método debe configurar this.etiquetaImagen.
        inicializarEtiquetaMostrarImagen(); 

        // --- 2. AÑADIR la etiquetaImagen CONFIGURADA AL CONTENEDOR DEL SPLIT ---
        if (this.etiquetaImagen != null && this.panelContenedorDerechoSplit != null) {
            this.panelContenedorDerechoSplit.add(this.etiquetaImagen, BorderLayout.CENTER);
            System.out.println("      -> etiquetaImagen añadida a panelContenedorDerechoSplit.");
        } else {
            System.err.println("ERROR CRÍTICO: etiquetaImagen o panelContenedorDerechoSplit nulos en configurarComponentesDelSplitDerecho.");
        }
    }

    /**
     * Crea y configura el JSplitPane principal.
     * @param panelIzquierdoCont El panel para el lado izquierdo del split.
     * @param panelDerechoCont El panel para el lado derecho del split.
     * @return El JSplitPane configurado.
     */
    private JSplitPane crearYConfigurarSplitPane(JPanel panelIzquierdoCont, JPanel panelDerechoCont) {
        System.out.println("    [VisorView] Creando y configurando JSplitPane...");
        if (panelIzquierdoCont == null || panelDerechoCont == null) {
            System.err.println("ERROR CRÍTICO: Paneles contenedores para JSplitPane son nulos. Creando JSplitPane vacío.");
            return new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); // Evitar NullPointer
        }
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierdoCont, panelDerechoCont);
        sp.setResizeWeight(0.25);
        sp.setContinuousLayout(true); // Recomendado para mejor feedback visual al arrastrar
        if (this.uiConfig != null && this.uiConfig.colorFondoPrincipal != null) {
            sp.setBackground(this.uiConfig.colorFondoPrincipal);
            // JSplitPane es un poco especial con la opacidad, a veces el L&F lo maneja.
            // sp.setOpaque(true); // Podría no ser necesario o incluso contraproducente según el L&F
        }
        sp.setBorder(null); // Quitar el borde por defecto del JSplitPane, si se desea una apariencia más limpia
        System.out.println("    [VisorView] JSplitPane creado.");
        return sp;
    }

    /**
     * Configura el panel de miniaturas.
     * Inicializa this.scrollListaMiniaturas y this.listaMiniaturas.
     */
    private void configurarPanelDeMiniaturas() {
        System.out.println("    [VisorView] Configurando PanelDeMiniaturas...");
        // --- 1. LLAMAR A TU LÓGICA EXISTENTE (inicializarPanelImagenesMiniatura) ---
        // Este método debe configurar this.scrollListaMiniaturas y this.listaMiniaturas.
        inicializarPanelImagenesMiniatura(this.model, this.servicioThumbs);
        System.out.println("    [VisorView] PanelDeMiniaturas configurado.");
    }

    /**
     * Crea y configura el panel para la barra de estado inferior.
     * @return El JPanel configurado para la barra de estado inferior.
     */
    private JPanel crearPanelEstadoInferior() {
        System.out.println("    [VisorView] Creando PanelEstadoInferior...");
        JPanel panel = new JPanel(new BorderLayout(5, 0)); // Espacio horizontal de 5px entre componentes
        if (this.uiConfig != null && this.uiConfig.colorFondoSecundario != null) {
            panel.setBackground(this.uiConfig.colorFondoSecundario);
        } else {
            panel.setBackground(Color.decode("#D6D9DF")); // Un gris claro por defecto
        }
        panel.setOpaque(true);
        // Borde superior para separarlo del contenido
        Border lineaExternaStatus = BorderFactory.createMatteBorder(1, 0, 0, 0,
            (this.uiConfig != null && this.uiConfig.colorBorde != null) ? this.uiConfig.colorBorde : Color.GRAY);
        // Padding interno
        Border paddingInternoStatus = BorderFactory.createEmptyBorder(3, 5, 3, 5); // Arriba, Izquierda, Abajo, Derecha
        panel.setBorder(BorderFactory.createCompoundBorder(lineaExternaStatus, paddingInternoStatus));

        // Componentes de la Barra Inferior
        this.rutaCompletaArchivoLabel = new JLabel("Ruta: (ninguna imagen seleccionada)");
        if (this.uiConfig != null && this.uiConfig.colorTextoPrimario != null) {
            this.rutaCompletaArchivoLabel.setForeground(this.uiConfig.colorTextoPrimario);
        } else {
            this.rutaCompletaArchivoLabel.setForeground(Color.BLACK);
        }
        // Envolver en un panel para que se expanda y no empuje a los mensajes
        JPanel panelRuta = new JPanel(new BorderLayout());
        panelRuta.setOpaque(false); // Hereda el fondo del panel principal de la barra de estado
        panelRuta.add(this.rutaCompletaArchivoLabel, BorderLayout.CENTER);
        panel.add(panelRuta, BorderLayout.CENTER); // Ruta al centro (se expandirá)

        this.mensajesAppLabel = new JLabel(" "); // Espacio para que tenga altura inicial
        if (this.uiConfig != null && this.uiConfig.colorTextoSecundario != null) {
            this.mensajesAppLabel.setForeground(this.uiConfig.colorTextoSecundario);
        } else {
            this.mensajesAppLabel.setForeground(Color.DARK_GRAY);
        }
        this.mensajesAppLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(this.mensajesAppLabel, BorderLayout.EAST); // Mensajes a la derecha

        // this.textoRuta ya no se usa directamente aquí si usamos rutaCompletaArchivoLabel y mensajesAppLabel
        // Podrías eliminar el campo this.textoRuta o marcarlo como obsoleto.

        System.out.println("    [VisorView] PanelEstadoInferior creado con JLabels para ruta y mensajes.");
        return panel;
    }

    /**
     * Crea un JSeparator vertical para usar en la barra de información.
     * @return Un JSeparator configurado.
     */
    private JSeparator crearSeparadorVerticalBarraInfo() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        Dimension d = separator.getPreferredSize();
        // Intentar basar la altura en la métrica de la fuente de un label de referencia
        // Es importante que this.nombreArchivoInfoLabel ya esté inicializado cuando se llama esto
        if (this.nombreArchivoInfoLabel != null && this.nombreArchivoInfoLabel.getFont() != null) {
            FontMetrics fm = this.nombreArchivoInfoLabel.getFontMetrics(this.nombreArchivoInfoLabel.getFont());
            // Una altura un poco mayor que la de la fuente puede quedar bien
            d.height = fm.getHeight() + fm.getDescent() + 2; 
        } else {
            // Fallback si el label de referencia no está listo (debería estarlo)
            d.height = 16;
        }
        d.width = 2; // Ancho fijo para el separador
        separator.setPreferredSize(d);
        return separator;
    }

    // --- MÉTODOS DE INICIALIZACIÓN INTERNA ESPECÍFICOS (COPIA TU LÓGICA AQUÍ) ---
    // Estos métodos deben configurar los campos de instancia de VisorView
    // (this.panelIzquierdo, this.listaNombres, this.etiquetaImagen, 
    //  this.scrollListaMiniaturas, this.listaMiniaturas)

    /**
     * Inicializa el panel izquierdo que contiene la lista de nombres de archivo.
     * Configura this.panelIzquierdo y this.listaNombres.
     * @param modeloVisorApp El modelo principal de la aplicación.
     * @param modeloNombresParam El DefaultListModel para la lista de nombres.
     */
    private void inicializarPanelIzquierdo(VisorModel modeloVisorApp, DefaultListModel<String> modeloNombresParam) {
        System.out.println("      -> [VisorView] inicializarPanelIzquierdo...");
        // >>> COPIA AQUÍ TU CÓDIGO DE inicializarPanelIzquierdo <<<
        // Asegúrate de que asigna a this.panelIzquierdo y this.listaNombres
        // Ejemplo:
        this.listaNombres = new JList<>(modeloNombresParam);
        this.listaNombres.setPrototypeCellValue("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");
        this.listaNombres.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        if (uiConfig != null) {
            this.listaNombres.setBackground(this.uiConfig.colorFondoSecundario);
            this.listaNombres.setForeground(this.uiConfig.colorTextoPrimario);
            this.listaNombres.setSelectionBackground(this.uiConfig.colorSeleccionFondo);
            this.listaNombres.setSelectionForeground(this.uiConfig.colorSeleccionTexto);
            this.listaNombres.setCellRenderer(new NombreArchivoRenderer(this.uiConfig));
        } // ... resto de tu configuración para listaNombres ...
        
        JScrollPane scrollPaneListaNombres = new JScrollPane(this.listaNombres);
        if (uiConfig != null) {
            scrollPaneListaNombres.getViewport().setBackground(this.uiConfig.colorFondoSecundario);
            scrollPaneListaNombres.setBorder(BorderFactory.createLineBorder(this.uiConfig.colorBorde));
        }

        this.panelIzquierdo = new JPanel(new BorderLayout());
        if (uiConfig != null) {
            this.panelIzquierdo.setBackground(this.uiConfig.colorFondoPrincipal);
            Border bordeLinea = BorderFactory.createLineBorder(this.uiConfig.colorBorde);
            TitledBorder bordeTitulado = BorderFactory.createTitledBorder(bordeLinea, "Archivos: 0"); // Título inicial
            bordeTitulado.setTitleColor(this.uiConfig.colorTextoSecundario);
            this.panelIzquierdo.setBorder(bordeTitulado);
        }
        this.panelIzquierdo.add(scrollPaneListaNombres, BorderLayout.CENTER);
        System.out.println("      -> PanelIzquierdo y ListaNombres configurados por inicializarPanelIzquierdo.");
    }

    /**
     * Inicializa el panel (JScrollPane) que contiene la lista de miniaturas.
     * Configura this.scrollListaMiniaturas y this.listaMiniaturas.
     * @param modeloVisorParam El modelo principal de la aplicación.
     * @param servicioThumbsParam El servicio de miniaturas.
     */
    private void inicializarPanelImagenesMiniatura(VisorModel modeloVisorParam, ThumbnailService servicioThumbsParam) {
        System.out.println("      -> [VisorView] inicializarPanelImagenesMiniatura...");
        // >>> COPIA AQUÍ TU CÓDIGO DE inicializarPanelImagenesMiniatura <<<
        // Asegúrate de que asigna a this.scrollListaMiniaturas y this.listaMiniaturas.
        // ...
        // Ejemplo (muy simplificado, usa tu lógica completa):
        this.listaMiniaturas = new JList<>(new DefaultListModel<>()); // Modelo inicial vacío
        // ... configuración de listaMiniaturas, renderer, fixedCellWidth/Height ...
        if (uiConfig != null && uiConfig.configurationManager != null && servicioThumbsParam != null && modeloVisorParam != null) {
            int thumbWidth = uiConfig.configurationManager.getInt("miniaturas.tamano.normal.ancho", 40);
            int thumbHeight = uiConfig.configurationManager.getInt("miniaturas.tamano.normal.alto", 40);
            boolean mostrarNombres = uiConfig.configurationManager.getBoolean("ui.miniaturas.mostrar_nombres", true);
            MiniaturaListCellRenderer renderer = new MiniaturaListCellRenderer(
                servicioThumbsParam, modeloVisorParam, thumbWidth, thumbHeight, mostrarNombres,
                uiConfig.colorFondoSecundario, uiConfig.colorSeleccionFondo,
                uiConfig.colorTextoPrimario, uiConfig.colorSeleccionTexto, Color.ORANGE // O uiConfig.colorBordeSeleccionActiva
            );
            this.listaMiniaturas.setCellRenderer(renderer);
            this.listaMiniaturas.setFixedCellWidth(renderer.getAnchoCalculadaDeCelda());
            this.listaMiniaturas.setFixedCellHeight(renderer.getAlturaCalculadaDeCelda());
            this.listaMiniaturas.setLayoutOrientation(JList.HORIZONTAL_WRAP);
            this.listaMiniaturas.setVisibleRowCount(-1);

            // Panel wrapper para centrar la lista
            JPanel wrapperListaMiniaturas = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
            wrapperListaMiniaturas.setOpaque(false);
            wrapperListaMiniaturas.add(this.listaMiniaturas);

            JPanel panelCentrador = new JPanel(new GridBagLayout());
            panelCentrador.setOpaque(true);
            panelCentrador.setBackground(uiConfig.colorFondoPrincipal);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.HORIZONTAL; // Que se expanda horizontalmente
            gbc.weightx = 1.0;
            gbc.weighty = 1.0; // También permitir expansión vertical si es necesario
            panelCentrador.add(wrapperListaMiniaturas, gbc);

            this.scrollListaMiniaturas = new JScrollPane(panelCentrador);
            this.scrollListaMiniaturas.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            this.scrollListaMiniaturas.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            this.scrollListaMiniaturas.setPreferredSize(new Dimension(100, this.miniaturaScrollPaneHeight));
            this.scrollListaMiniaturas.getViewport().setBackground(uiConfig.colorFondoPrincipal);
            TitledBorder bordeTituladoMini = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(uiConfig.colorBorde), "Miniaturas");
            bordeTituladoMini.setTitleColor(uiConfig.colorTextoSecundario);
            this.scrollListaMiniaturas.setBorder(bordeTituladoMini);
        } else {
            this.scrollListaMiniaturas = new JScrollPane(new JLabel("Error init miniaturas")); // Fallback
        }
        System.out.println("      -> PanelDeMiniaturas (scroll y lista) configurados por inicializarPanelImagenesMiniatura.");
    }

    /**
     * Inicializa la etiqueta principal donde se mostrará la imagen.
     * Configura this.etiquetaImagen.
     */
    private void inicializarEtiquetaMostrarImagen() {
        System.out.println("      -> [VisorView] inicializarEtiquetaMostrarImagen...");
        // >>> COPIA AQUÍ TU CÓDIGO DE inicializarEtiquetaMostrarImagen <<<
        // (Incluyendo el paintComponent sobrescrito)
        // Asegúrate de que asigna a this.etiquetaImagen.
        // Ejemplo:
        this.etiquetaImagen = new JLabel() { // Inicio JLabel anónimo
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); // Llamar al super es una buena práctica inicial

                int width = getWidth();
                int height = getHeight();

                // 1. Dibujar Fondo (cuadros o sólido)
                if (fondoACuadrosActivado) {
                    Graphics2D g2dFondo = (Graphics2D) g.create();
                    try {
                        for (int row = 0; row < height; row += TAMANO_CUADRO) {
                            for (int col = 0; col < width; col += TAMANO_CUADRO) {
                                g2dFondo.setColor(
                                        (((row / TAMANO_CUADRO) % 2) == ((col / TAMANO_CUADRO) % 2)) ? colorCuadroClaro
                                                : colorCuadroOscuro);
                                g2dFondo.fillRect(col, row, TAMANO_CUADRO, TAMANO_CUADRO);
                            }
                        }
                    } finally {
                        g2dFondo.dispose();
                    }
                } else {
                    g.setColor(getBackground()); // Usar el background del JLabel
                    g.fillRect(0, 0, width, height);
                }

                // 2. Dibujar Imagen (si existe)
                if (imagenReescaladaView != null) {
                    Graphics2D g2dImagen = (Graphics2D) g.create();
                    try {
                        g2dImagen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2dImagen.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        g2dImagen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        
                        // El tamaño base de la imagenReescaladaView ya está ajustado por ImageDisplayUtils
                        int baseW = imagenReescaladaView.getWidth(null);
                        int baseH = imagenReescaladaView.getHeight(null);

                        if (baseW > 0 && baseH > 0) {
                            // Aplicar el zoomFactorView y los offsets del modelo (que vienen de VisorModel)
                            int finalW = (int) (baseW * zoomFactorView);
                            int finalH = (int) (baseH * zoomFactorView);
                            int drawX = (width - finalW) / 2 + imageOffsetXView;
                            int drawY = (height - finalH) / 2 + imageOffsetYView;
                            g2dImagen.drawImage(imagenReescaladaView, drawX, drawY, finalW, finalH, null);
                        }
                    } finally {
                        g2dImagen.dispose();
                    }
                }
                // 3. Dibujar Texto "Cargando..." (si no hay imagen y el JLabel tiene texto)
                else if (getText() != null && !getText().isEmpty()) {
                    g.setColor(getForeground());
                    g.setFont(getFont());
                    FontMetrics fm = g.getFontMetrics();
                    int textW = fm.stringWidth(getText());
                    int textH = fm.getAscent(); // Altura del texto desde la línea base
                    int textX = (width - textW) / 2;
                    int textY = (height - fm.getHeight()) / 2 + textH; // Centrar verticalmente
                    g.drawString(getText(), textX, textY);
                }
            } // Fin paintComponent
        }; // Fin JLabel anónimo

        this.etiquetaImagen.setHorizontalAlignment(SwingConstants.CENTER);
        this.etiquetaImagen.setVerticalAlignment(SwingConstants.CENTER);
        this.etiquetaImagen.setOpaque(false); // Importante: NO opaco para que el fondo pintado se vea
        if (uiConfig != null) {
            this.etiquetaImagen.setBackground(this.uiConfig.colorFondoSecundario); // Color base si no es a cuadros
            this.etiquetaImagen.setForeground(this.uiConfig.colorTextoPrimario);   // Color para texto "Cargando"
        } else {
            this.etiquetaImagen.setBackground(Color.DARK_GRAY); // Fallback
            this.etiquetaImagen.setForeground(Color.WHITE);     // Fallback
        }
        System.out.println("      -> EtiquetaImagen configurada por inicializarEtiquetaMostrarImagen.");
    }
    
    // --- MÉTODOS DE ACTUALIZACIÓN DE LA VISTA (DE TU CÓDIGO ANTERIOR) ---
    // (Adapta estos según sea necesario, especialmente setTextoRuta)
    
    public void setListaImagenesModel(DefaultListModel<String> nuevoModelo) {
        if (this.listaNombres != null) {
            this.listaNombres.setModel(nuevoModelo);
        }
        // Si listaMiniaturas usa un modelo diferente (como el modeloMiniaturas del controller),
        // ese se actualizaría por separado. Si usa el mismo modelo principal, esta línea es redundante
        // o debería quitarse si causa problemas con la lógica de ListCoordinator.
        // Por ahora, si tu lógica es que AMBAS usan el mismo modelo principal:
        if (this.listaMiniaturas != null) {
            this.listaMiniaturas.setModel(nuevoModelo); // Cuidado aquí si tienes lógica separada para miniaturas
        }
         System.out.println("[VisorView setListaImagenesModel] Modelo asignado a listaNombres (y potencialmente listaMiniaturas). Tamaño: " + nuevoModelo.getSize());
    }

    public void setModeloListaMiniaturas(DefaultListModel<String> nuevoModeloMiniaturas) {
        if (this.listaMiniaturas != null && nuevoModeloMiniaturas != null) {
             System.out.println("[VisorView] Estableciendo nuevo modelo para listaMiniaturas (Tamaño: " + nuevoModeloMiniaturas.getSize() + ")");
             this.listaMiniaturas.setModel(nuevoModeloMiniaturas);
             this.listaMiniaturas.repaint();
        } else {
             System.err.println("WARN [setModeloListaMiniaturas]: listaMiniaturas o nuevo modelo es null.");
        }
    }
    
    public void setImagenMostrada(Image imagenReescalada, double zoom, int offsetX, int offsetY) {
        if (this.etiquetaImagen == null) return;
        this.etiquetaImagen.setText(null);
        this.etiquetaImagen.setIcon(null); 
        this.imagenReescaladaView = imagenReescalada; 
        this.zoomFactorView = zoom;       
        this.imageOffsetXView = offsetX;  
        this.imageOffsetYView = offsetY;  
        this.etiquetaImagen.repaint();
    }

    public void limpiarImagenMostrada() {
        if (this.etiquetaImagen == null) return;
        this.etiquetaImagen.setText(null); 
        this.etiquetaImagen.setIcon(null);  
        this.imagenReescaladaView = null;
        this.zoomFactorView = 1.0;
        this.imageOffsetXView = 0;
        this.imageOffsetYView = 0;
        this.etiquetaImagen.repaint();
    }

    public void setTituloPanelIzquierdo(String titulo) {
        if (this.panelIzquierdo == null || !(this.panelIzquierdo.getBorder() instanceof TitledBorder)) {
            System.err.println("WARN [setTituloPanelIzquierdo]: panelIzquierdo o su TitledBorder nulos.");
            return;
        }
        TitledBorder borde = (TitledBorder) this.panelIzquierdo.getBorder();
        borde.setTitle(titulo != null ? titulo : "");
        this.panelIzquierdo.repaint();
    }

    public void setJMenuBarVisible(boolean visible) {
        JMenuBar mb = getJMenuBar();
        if (mb != null && mb.isVisible() != visible) {
            mb.setVisible(visible);
            SwingUtilities.invokeLater(this::revalidateFrame);
        }
    }
    
    public void setToolBarVisible(boolean visible) {
        if (this.panelDeBotones != null && this.panelDeBotones.isVisible() != visible) {
            this.panelDeBotones.setVisible(visible);
            // El padre de panelDeBotones es el contentPane del JFrame.
             SwingUtilities.invokeLater(this::revalidateFrame);
        }
    }

    public void setFileListVisible(boolean visible) {
        if (this.panelContenedorIzquierdoSplit != null && this.panelContenedorIzquierdoSplit.isVisible() != visible) {
            this.panelContenedorIzquierdoSplit.setVisible(visible); // Ocultar/mostrar el contenedor
            if (this.splitPane != null) {
                 SwingUtilities.invokeLater(() -> this.splitPane.resetToPreferredSizes());
            }
            SwingUtilities.invokeLater(this::revalidateFrame);
        }
    }

    public void setThumbnailsVisible(boolean visible) {
        if (this.scrollListaMiniaturas != null && this.scrollListaMiniaturas.isVisible() != visible) {
            this.scrollListaMiniaturas.setVisible(visible);
            SwingUtilities.invokeLater(this::revalidateFrame);
        }
    }

    public void setLocationBarVisible(boolean visible) {
        if (this.bottomStatusBar != null && this.bottomStatusBar.isVisible() != visible) {
            this.bottomStatusBar.setVisible(visible);
            SwingUtilities.invokeLater(this::revalidateFrame);
        }
    }
    
    private void revalidateFrame() {
        this.revalidate();
        this.repaint();
    }

    public void setCheckeredBackgroundEnabled (boolean activado) {
        if (this.fondoACuadrosActivado != activado) {
            this.fondoACuadrosActivado = activado;
            if (etiquetaImagen != null) etiquetaImagen.repaint();
        }
    }
    public boolean isFondoACuadrosActivado() { // Getter añadido
        return this.fondoACuadrosActivado;
    }

    public void actualizarEstadoControlesZoom(boolean zoomManualActivado, boolean resetHabilitado) {
        if (botonesPorNombre == null || menuItemsPorNombre == null || uiConfig == null) return;
        String zoomButtonKey = "interfaz.boton.zoom.Zoom_48x48"; // Asume que esta es la clave correcta
        JButton zoomButton = this.botonesPorNombre.get(zoomButtonKey);
        if (zoomButton != null) {
            zoomButton.setBackground(zoomManualActivado ? this.uiConfig.colorBotonActivado : this.uiConfig.colorBotonFondo);
            zoomButton.setOpaque(true);
        }
        String resetButtonKey = "interfaz.boton.zoom.Reset_48x48";
        JButton resetButton = this.botonesPorNombre.get(resetButtonKey);
        if (resetButton != null) resetButton.setEnabled(resetHabilitado);
        
        String resetMenuKey = "interfaz.menu.zoom.resetear_zoom";
        JMenuItem resetMenuItem = this.menuItemsPorNombre.get(resetMenuKey);
        if (resetMenuItem != null) resetMenuItem.setEnabled(resetHabilitado);
    }

    public void setEstadoMenuActivarZoomManual(boolean seleccionado) {
        String zoomMenuKey = "interfaz.menu.zoom.Activar_Zoom_Manual";
        if (menuItemsPorNombre == null) return;
        JMenuItem zoomMenuItem = this.menuItemsPorNombre.get(zoomMenuKey);
        if (zoomMenuItem instanceof JCheckBoxMenuItem) {
            JCheckBoxMenuItem cbItem = (JCheckBoxMenuItem) zoomMenuItem;
            if (cbItem.isSelected() != seleccionado) cbItem.setSelected(seleccionado);
        }
    }

    public void aplicarAnimacionBoton(String actionCommandDelBoton) {
        // Tu lógica de animación existente
        // ...
    }
    
    // Métodos add...Listener (ya los tenías, asegúrate que referencian a this.etiquetaImagen, etc.)
    public void addListaNombresSelectionListener (javax.swing.event.ListSelectionListener listener) {
        if (listaNombres != null) listaNombres.addListSelectionListener(listener);
    }
    public void addListaMiniaturasSelectionListener (javax.swing.event.ListSelectionListener listener) {
        if (listaMiniaturas != null) listaMiniaturas.addListSelectionListener(listener);
    }
    public void addEtiquetaImagenMouseWheelListener(java.awt.event.MouseWheelListener listener) {
        if (etiquetaImagen != null) etiquetaImagen.addMouseWheelListener(listener);
    }
    public void addEtiquetaImagenMouseListener(java.awt.event.MouseListener listener) {
        if (etiquetaImagen != null) etiquetaImagen.addMouseListener(listener);
    }
    public void addEtiquetaImagenMouseMotionListener(java.awt.event.MouseMotionListener listener) {
        if (etiquetaImagen != null) etiquetaImagen.addMouseMotionListener(listener);
    }
    
    public void mostrarIndicadorCargaImagenPrincipal(String mensaje) {
        if (this.etiquetaImagen == null) return;
        String mensajeAMostrar = (mensaje != null && !mensaje.trim().isEmpty()) ? mensaje : "Cargando...";
        this.imagenReescaladaView = null; 
        this.etiquetaImagen.setIcon(null);  
        this.etiquetaImagen.setText(mensajeAMostrar);
        if(this.uiConfig != null) { 
             this.etiquetaImagen.setForeground(this.uiConfig.colorTextoPrimario);
        } else { this.etiquetaImagen.setForeground(Color.BLACK); }
        this.etiquetaImagen.setHorizontalAlignment(SwingConstants.CENTER);
        this.etiquetaImagen.setVerticalAlignment(SwingConstants.CENTER);
        this.etiquetaImagen.repaint();
    }
    
    // Métodos para actualizar los mapas y barras (recibidos de AppInitializer)
    public void setActualJMenuBar(JMenuBar menuBar) {
        if (menuBar != null) this.setJMenuBar(menuBar);
    }
    public void setActualToolbarPanel(JPanel toolbarPanel) {
        if (toolbarPanel != null) {
            if (this.panelDeBotones != null) remove(this.panelDeBotones); // Quitar el placeholder si existe
            this.panelDeBotones = toolbarPanel;
            add(this.panelDeBotones, BorderLayout.NORTH); // Añadir el real
            revalidateFrame();
        }
    }
    public void setActualMenuItemsMap(Map<String, JMenuItem> menuItems) {
        this.menuItemsPorNombre = (menuItems != null) ? menuItems : new HashMap<>();
    }
    public void setActualBotonesMap(Map<String, JButton> botones) {
        this.botonesPorNombre = (botones != null) ? botones : new HashMap<>();
    }
    public void setUiConfig(ViewUIConfig uiConfig) { // Setter para ViewUIConfig
        //this.uiConfig = Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser nulo en VisorView.setUiConfig");
    }
    
 // En vista.VisorView.java

    public void solicitarRefrescoRenderersMiniaturas() { // WENO
        System.out.println("[VisorView] Solicitando refresco completo de renderers de miniaturas...");

        // Corrección: Usar 'this.listaMiniaturas' directamente
        if (this.listaMiniaturas != null && // <<-- CAMBIO AQUÍ
            this.model != null && 
            this.servicioThumbs != null && 
            this.uiConfig != null && 
            this.uiConfig.configurationManager != null) {

            boolean mostrarNombresActual = this.uiConfig.configurationManager.getBoolean("miniaturas.ui.mostrar_nombres", true);
            int thumbWidth = this.uiConfig.configurationManager.getInt("miniaturas.tamano.normal.ancho", 40);
            int thumbHeight = this.uiConfig.configurationManager.getInt("miniaturas.tamano.normal.alto", 40);
            
            Color colorFondoMiniatura = this.uiConfig.colorFondoSecundario;
            Color colorFondoSeleccionMiniatura = this.uiConfig.colorSeleccionFondo;
            Color colorTextoMiniatura = this.uiConfig.colorTextoPrimario;
            Color colorTextoSeleccionMiniatura = this.uiConfig.colorSeleccionTexto;
            Color colorBordeSeleccionMiniatura = this.uiConfig.colorBordeSeleccionActiva; 

            MiniaturaListCellRenderer newRenderer = new MiniaturaListCellRenderer(
                this.servicioThumbs, 
                this.model,
                thumbWidth, 
                thumbHeight, 
                mostrarNombresActual,
                colorFondoMiniatura, 
                colorFondoSeleccionMiniatura, 
                colorTextoMiniatura, 
                colorTextoSeleccionMiniatura, 
                colorBordeSeleccionMiniatura
            );

            // Acceder a this.listaMiniaturas directamente
            this.listaMiniaturas.setCellRenderer(newRenderer); // <<-- USA EL CAMPO DIRECTAMENTE
            this.listaMiniaturas.setFixedCellHeight(newRenderer.getAlturaCalculadaDeCelda());
            this.listaMiniaturas.setFixedCellWidth(newRenderer.getAnchoCalculadaDeCelda());
            this.listaMiniaturas.revalidate();
            this.listaMiniaturas.repaint();
            System.out.println("  -> Renderers de miniaturas refrescados.");
            
        } else {
            System.err.println("WARN [solicitarRefrescoRenderersMiniaturas]: No se pudo refrescar renderers debido a dependencias nulas.");
            System.err.println("  -> this.listaMiniaturas: " + (this.listaMiniaturas != null)); // <<-- CAMBIO AQUÍ
            System.err.println("  -> this.model: " + (this.model != null));
            System.err.println("  -> this.servicioThumbs: " + (this.servicioThumbs != null));
            System.err.println("  -> this.uiConfig: " + (this.uiConfig != null));
            if (this.uiConfig != null) {
                System.err.println("  -> this.uiConfig.configurationManager: " + (this.uiConfig.configurationManager != null));
            }
        }
    }
    public void actualizarAspectoBotonToggle(Action action, boolean isSelected) {
        if (action == null || this.uiConfig == null || this.botonesPorNombre == null) return;
        JButton botonAsociado = null;
        for (Map.Entry<String, JButton> entry : this.botonesPorNombre.entrySet()) {
            if (action.equals(entry.getValue().getAction())) {
                botonAsociado = entry.getValue();
                break;
            }
        }
        if (botonAsociado != null) {
            Color colorFondoDestino = isSelected ? this.uiConfig.colorBotonActivado : this.uiConfig.colorBotonFondo;
            botonAsociado.setBackground(colorFondoDestino);
            botonAsociado.setOpaque(true);
            botonAsociado.setSelected(isSelected); // Importante para JToggleButton
        }
    }
    public Image getImagenReescaladaView() {
        return this.imagenReescaladaView;
    }
    
    
    /**
     * Devuelve la etiqueta (JLabel) principal donde se muestra la imagen.
     * Es el componente que responde a eventos de ratón para zoom/pan y
     * donde ImageDisplayUtils/paintComponent dibujan la imagen.
     *
     * @return La instancia de JLabel para la imagen principal, o null si no ha sido inicializada.
     */
    public JLabel getEtiquetaImagen() {
        return this.etiquetaImagen;
    }
    
    /**
     * Devuelve la JList utilizada para mostrar las imágenes en miniatura.
     *
     * @return La instancia de JList para las miniaturas, o null si no ha sido inicializada.
     */
    public JList<String> getListaMiniaturas() {
        return this.listaMiniaturas;
    }
    
    /**
     * Devuelve el mapa de JButtons de la barra de herramientas, donde la clave
     * es la clave de configuración larga del botón y el valor es la instancia de JButton.
     * Este mapa es generalmente construido por ToolbarBuilder y pasado a VisorView.
     *
     * @return Un mapa de (String claveConfig -> JButton), o null/vacío si no está inicializado.
     */
    public Map<String, JButton> getBotonesPorNombre() {
        return this.botonesPorNombre;
    }
    
    /**
     * Devuelve el mapa de JMenuItems de la barra de menú, donde la clave
     * es la clave de configuración larga del ítem y el valor es la instancia de JMenuItem.
     * Este mapa es generalmente construido por MenuBarBuilder y pasado a VisorView.
     *
     * @return Un mapa de (String claveConfig -> JMenuItem), o null/vacío si no está inicializado.
     */
    public Map<String, JMenuItem> getMenuItemsPorNombre() {
        return this.menuItemsPorNombre;
    }
    
    /**
     * Devuelve la JList utilizada para mostrar los nombres de los archivos de imagen.
     *
     * @return La instancia de JList para los nombres de archivo, o null si no ha sido inicializada.
     */
    public JList<String> getListaNombres() {
        return this.listaNombres;
    }

    /**
     * Devuelve el JScrollPane que contiene la lista de miniaturas.
     * Este componente es útil para añadir listeners de rueda de ratón para el scroll horizontal
     * o para gestionar su visibilidad.
     *
     * @return La instancia de JScrollPane para las miniaturas, o null si no ha sido inicializado.
     */
    public JScrollPane getScrollListaMiniaturas() {
        return this.scrollListaMiniaturas;
    }
    
    /**
     * Devuelve la instancia de este JFrame.
     * Útil cuando se necesita pasar la ventana principal como padre para diálogos.
     *
     * @return Esta instancia de VisorView como JFrame.
     */
    public JFrame getFrame() {
        return this; // 'this' ya es un JFrame porque VisorView extends JFrame
    }
    
    /**
     * Establece el texto en la etiqueta de la barra de estado inferior
     * que muestra la ruta del archivo.
     * @param texto La ruta del archivo a mostrar.
     */
    public void setTextoBarraEstadoRuta(String texto) {
        if (this.rutaCompletaArchivoLabel != null) {
            this.rutaCompletaArchivoLabel.setText(texto != null ? texto : "Ruta: N/A"); // Evitar null y dar un placeholder
        } else {
            System.err.println("WARN [VisorView.setTextoBarraEstadoRuta]: rutaCompletaArchivoLabel es null.");
        }
    }

    /**
     * Establece un mensaje temporal en la etiqueta de mensajes de la aplicación
     * en la barra de estado inferior.
     * @param mensaje El mensaje a mostrar.
     */
    public void setTextoBarraEstadoMensaje(String mensaje) {
        if (this.mensajesAppLabel != null) {
            this.mensajesAppLabel.setText(mensaje != null ? mensaje : " "); // Evitar null y mantener espacio
        } else {
            System.err.println("WARN [VisorView.setTextoBarraEstadoMensaje]: mensajesAppLabel es null.");
        }
    }
    
    /**
     * Devuelve el panel izquierdo que contiene la lista de nombres de archivo.
     * Este panel usualmente tiene un TitledBorder y contiene el JScrollPane de listaNombres.
     *
     * @return El JPanel para el lado izquierdo, o null si no ha sido inicializado.
     */
    public JPanel getPanelIzquierdo() {
        return this.panelIzquierdo;
    }
   
    /**
     * Devuelve el panel que contiene la barra de botones principal (toolbar).
     * Este panel es usualmente el que se recibe del ToolbarBuilder.
     *
     * @return El JPanel de la barra de botones, o null si no ha sido inicializado.
     */
    public JPanel getPanelDeBotones() {
        return this.panelDeBotones; // o this.mainToolbarPanel, si panelDeBotones es solo una referencia a él
    }
    
    /**
     * Devuelve el JTextField que se utilizaba anteriormente como barra de estado/ubicación.
     * Este método podría quedar obsoleto si this.textoRuta se elimina completamente
     * en favor de los componentes dentro de bottomStatusBar.
     *
     * @return El JTextField de la barra de estado, o null si no ha sido inicializado.
     */
    public JTextField getTextoRuta() {
        return this.textoRuta;
    }
    
    // Getters para la Barra de Información Superior
 	public JLabel getNombreArchivoInfoLabel() { return this.nombreArchivoInfoLabel; }
 	public JLabel getIndiceTotalInfoLabel() { return this.indiceTotalInfoLabel; } // <<-- EL QUE NECESITAS AÑADIR
 	public JLabel getDimensionesOriginalesInfoLabel() { return this.dimensionesOriginalesInfoLabel; }
 	public JLabel getModoZoomNombreInfoLabel() { return this.modoZoomNombreInfoLabel; }
 	public JLabel getPorcentajeZoomVisualRealInfoLabel() { return this.porcentajeZoomVisualRealInfoLabel; }
 	public JLabel getIndicadorZoomManualInfoLabel() { return this.indicadorZoomManualInfoLabel; }
 	public JLabel getIndicadorMantenerPropInfoLabel() { return this.indicadorMantenerPropInfoLabel; }
 	public JLabel getIndicadorSubcarpetasInfoLabel() { return this.indicadorSubcarpetasInfoLabel; }

     // Getters para la Barra de Estado Inferior (si ya los tienes, si no, los añadiremos)
     // public JLabel getRutaCompletaArchivoLabel() { return this.rutaCompletaArchivoLabel; }
     // public JLabel getMensajesAppLabel() { return this.mensajesAppLabel; }
 	
 	
    
} // Fin clase VisorView