package vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;

// Imports para el paintComponent de etiquetaImagen (si está aquí)
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
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
import servicios.ConfigurationManager;
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
    private JLabel formatoImagenInfoLabel; // Para mostrar el formato de la imagen 

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
    private JLabel tamanoArchivoInfoLabel; 
    private JLabel fechaArchivoInfoLabel;   
    private JLabel porcentajeZoomVisualRealInfoLabel;
    private JLabel indicadorZoomManualInfoLabel;
    private JLabel indicadorMantenerPropInfoLabel;
    private JLabel indicadorSubcarpetasInfoLabel;
    private JLabel porcentajeZoomPersonalizadoLabel;
    
    // Componentes de la Barra de Estado Inferior (bottomStatusBar)
    private JLabel rutaCompletaArchivoLabel; 
    private JLabel mensajesAppLabel;
    private JTextField textoRuta; // Usado como fallback temporalmente en crearPanelEstadoInferior
	private JLabel iconoZoomManualLabel;
	private JLabel iconoMantenerProporcionesLabel;
	private JLabel iconoModoSubcarpetasLabel;
	private JButton modoZoomActualIconoBoton;
	private JButton porcentajeZoomEspecificadoBoton;
    
    
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

    // --- Icono de Error de Imagen
    private ImageIcon iconoErrorGeneral;
    //private ImageIcon iconoErrorOriginal;


    /**
      * Constructor principal de la ventana VisorView.
      * Inicializa las dependencias y llama al método de construcción de la UI.
      *
      * @param miniaturaPanelHeight Altura deseada para el scrollpane de miniaturas.
      * @param config Objeto ViewUIConfig con parámetros de apariencia y referencias (como IconUtils).
      * @param modelo El VisorModel principal.
      * @param servicioThumbs El ThumbnailService para renderers de miniaturas.
      * @param menuBar La JMenuBar pre-construida (o un placeholder si se construye después).
      * @param toolbarPanel El JPanel de la toolbar pre-construido (o un placeholder).
      * @param menuItems Mapa de JMenuItems generados (puede ser vacío inicialmente).
      * @param botones Mapa de JButtons generados (puede ser vacío inicialmente).
      */
     public VisorView(
             int miniaturaPanelHeight,
             ViewUIConfig config,        // Contiene IconUtils, ConfigurationManager, colores, etc.
             VisorModel modelo,
             ThumbnailService servicioThumbs,
             JMenuBar menuBar,           // Puede ser un placeholder inicial
             JPanel toolbarPanel,        // Puede ser un placeholder inicial
             Map<String, JMenuItem> menuItems, // Puede ser un placeholder inicial
             Map<String, JButton> botones      // Puede ser un placeholder inicial
     ) {
         super("Visor/Catalogador de Imágenes"); // Título de la ventana
         System.out.println("[VisorView Constructor] Iniciando constructor principal de VisorView...");

         // --- 1. ASIGNACIÓN DE DEPENDENCIAS INYECTADAS ---
         //    Usa Objects.requireNonNull para asegurar que las dependencias críticas no sean nulas.
         this.uiConfig = Objects.requireNonNull(config, "ViewUIConfig (config) no puede ser null en VisorView constructor");
         this.model = Objects.requireNonNull(modelo, "VisorModel (modelo) no puede ser null en VisorView constructor");
         this.servicioThumbs = Objects.requireNonNull(servicioThumbs, "ThumbnailService (servicioThumbs) no puede ser null en VisorView constructor");
         
         // Determinar la altura del panel de miniaturas, con un fallback.
         this.miniaturaScrollPaneHeight = miniaturaPanelHeight > 0 ? miniaturaPanelHeight : 100; // Ejemplo: 100px si no se especifica > 0

         // Asignar placeholders o componentes reales de menú/toolbar. Serán actualizados después por el Controller si es necesario.
         this.mainMenuBar = (menuBar != null) ? menuBar : new JMenuBar(); // Usar placeholder si es null
         this.mainToolbarPanel = (toolbarPanel != null) ? toolbarPanel : new JPanel(); // Usar placeholder si es null
         this.panelDeBotones = this.mainToolbarPanel; // Referencia inicial
         
         // Mapas para componentes de UI (botones/menús) que serán poblados/actualizados por el Controller.
         this.menuItemsPorNombre = (menuItems != null) ? menuItems : new HashMap<>();
         this.botonesPorNombre = (botones != null) ? botones : new HashMap<>();

         // --- 2. CONFIGURACIÓN BÁSICA DEL JFRAME ---
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Comportamiento al cerrar la ventana.
         setLayout(new BorderLayout()); // Establecer el layout principal del content pane.
         
         // Establecer color de fondo principal del content pane.
         if (this.uiConfig.colorFondoPrincipal != null) {
             getContentPane().setBackground(this.uiConfig.colorFondoPrincipal);
         } else {
             getContentPane().setBackground(Color.LIGHT_GRAY); // Color de fallback.
             System.err.println("WARN [VisorView Constructor]: uiConfig.colorFondoPrincipal es null. Usando fallback LIGHT_GRAY.");
         }

         // --- 3. CARGA DEL ICONO DE ERROR GENERAL (ESCALADO PARA EL VISOR PRINCIPAL) ---
         //    Este icono se mostrará en etiquetaImagen cuando no se pueda cargar una imagen.
         if (this.uiConfig.iconUtils != null) {
             // Definir el tamaño deseado para el icono de error en el visor principal.
             // Estos valores pueden ajustarse según tus preferencias visuales.
             int anchoIconoErrorVisor = 128;  // Ejemplo: 128 píxeles de ancho
             int altoIconoErrorVisor = 128;   // Ejemplo: 128 píxeles de alto
                                              // Si quieres mantener proporción, pon -1 en una de las dimensiones, ej. (128, -1)

             this.iconoErrorGeneral = this.uiConfig.iconUtils.getScaledCommonIcon(
                 "imagen-rota.png",           // Nombre del archivo del icono (en /iconos/comunes/)
                 anchoIconoErrorVisor,        // Ancho deseado para el visor principal
                 altoIconoErrorVisor          // Alto deseado para el visor principal
             );

             if (this.iconoErrorGeneral == null) {
                 System.err.println("WARN [VisorView Constructor]: No se pudo cargar o escalar el icono de error general 'imagen-rota.png' para el visor principal. Se usará texto de error.");
             } else {
                 System.out.println("  [VisorView Constructor] Icono de error general ('imagen-rota.png') cargado y escalado para visor principal a: " +
                                    anchoIconoErrorVisor + "x" + altoIconoErrorVisor);
             }
         } else {
             this.iconoErrorGeneral = null; // No se puede cargar el icono si iconUtils es null.
             System.err.println("WARN [VisorView Constructor]: uiConfig.iconUtils es null. No se pudo cargar/inicializar iconoErrorGeneral.");
         }
         
         // --- 4. INICIALIZACIÓN DE COMPONENTES DE LA UI ---
         //    Llama al método que construye y ensambla los paneles y componentes internos.
         //    Se pasa el modelo de lista del VisorModel para la lista de nombres de archivo.
         if (this.model.getModeloLista() != null) {
             inicializarComponentes(this.model.getModeloLista());
         } else {
             System.err.println("ERROR CRÍTICO [VisorView Constructor]: this.model.getModeloLista() es null. Inicializando componentes con modelo vacío.");
             inicializarComponentes(new DefaultListModel<>()); // Fallback para evitar NullPointerException
         }

         // --- 5. RESTAURAR ESTADO DE LA VENTANA (TAMAÑO, POSICIÓN, MAXIMIZADO) ---
         //    Se lee desde ConfigurationManager (accesible a través de uiConfig).
         if (this.uiConfig.configurationManager != null) {
             boolean wasMaximized = this.uiConfig.configurationManager.getBoolean(ConfigurationManager.KEY_WINDOW_MAXIMIZED, false);
             if (wasMaximized) {
                 setExtendedState(JFrame.MAXIMIZED_BOTH); // Maximizar la ventana.
             } else {
                 // Obtener dimensiones y posición guardadas, con valores por defecto razonables.
                 int x = this.uiConfig.configurationManager.getInt(ConfigurationManager.KEY_WINDOW_X, -1);
                 int y = this.uiConfig.configurationManager.getInt(ConfigurationManager.KEY_WINDOW_Y, -1);
                 int w = this.uiConfig.configurationManager.getInt(ConfigurationManager.KEY_WINDOW_WIDTH, 1280); // Default ancho
                 int h = this.uiConfig.configurationManager.getInt(ConfigurationManager.KEY_WINDOW_HEIGHT, 720);  // Default alto
                 
                 // Aplicar solo si las dimensiones y posición son válidas.
                 if (w > 50 && h > 50 && x != -1 && y != -1) { 
                      setBounds(x, y, w, h);
                 } else { // Si no hay posición guardada o es inválida, usar tamaño por defecto y centrar.
                      setSize(w,h);
                      setLocationRelativeTo(null); 
                 }
             }
             System.out.println("  [VisorView Constructor] Estado de la ventana (tamaño/posición) restaurado desde configuración.");
         } else {
             // Fallback si ConfigurationManager no está disponible en uiConfig.
             setSize(1280, 720); // Tamaño por defecto.
             setLocationRelativeTo(null); // Centrar en pantalla.
             System.err.println("WARN [VisorView Constructor]: uiConfig.configurationManager es null. Usando tamaño/posición de ventana por defecto.");
         }
         
         // --- 6. AJUSTE FINAL DEL DIVISOR DEL JSPLITPANE (SI EXISTE) ---
         //    Se usa invokeLater para asegurar que el JSplitPane ya esté realizado y tenga dimensiones.
         SwingUtilities.invokeLater(() -> {
             if (this.splitPane != null) { // splitPane se inicializa en crearPanelModoNormal
                 // Leer la posición inicial del divisor desde la configuración o usar un default.
                 double dividerLocationPercentage = 0.25; // Ejemplo: 25% para el panel izquierdo.
                 if (this.uiConfig.configurationManager != null) {
                     // Ejemplo de cómo podrías guardar/leer esto:
                     // dividerLocationPercentage = this.uiConfig.configurationManager.getDouble("ui.splitpane.main.dividerLocation", 0.25);
                 }
                 this.splitPane.setDividerLocation(dividerLocationPercentage); 
                 System.out.println("  [VisorView Constructor - EDT] Divisor del JSplitPane principal establecido a " + (dividerLocationPercentage * 100) + "%.");
             } else {
                 System.err.println("WARN [VisorView Constructor - EDT]: splitPane es null. No se pudo establecer la posición del divisor.");
             }
         });

         System.out.println("[VisorView Constructor] Constructor de VisorView finalizado.");
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
        //this.rutaCompletaArchivoLabel = new JLabel("Ruta: (ninguna imagen seleccionada en inicializarComponenentes)");

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
     * Muestra información sobre el archivo actual, sus propiedades y el estado de visualización.
     * @return El JPanel configurado para la barra de información superior.
     */
    private JPanel crearPanelInfoSuperior() {
        System.out.println("    [VisorView] Creando PanelInfoSuperior (v2)...");
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5)); // Padding general
        if (this.uiConfig != null && this.uiConfig.colorFondoSecundario != null) {
            panel.setBackground(this.uiConfig.colorFondoSecundario);
        } else {
            panel.setBackground(Color.LIGHT_GRAY); // Fallback
        }
        panel.setOpaque(true);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START; // Alineación por defecto para la mayoría
        gbc.insets = new Insets(0, 3, 0, 3); // Espaciado pequeño entre componentes

        // --- Componentes (inicializar campos de instancia) ---
        this.nombreArchivoInfoLabel = new JLabel("Archivo: N/A");
        this.indiceTotalInfoLabel = new JLabel("Idx: N/A");
        this.dimensionesOriginalesInfoLabel = new JLabel("Dim: N/A");
        this.tamanoArchivoInfoLabel = new JLabel("Tam: N/A"); // Placeholder
        this.fechaArchivoInfoLabel = new JLabel("Fch: N/A");   // Placeholder
        this.modoZoomNombreInfoLabel = new JLabel("Modo: N/A");
        this.porcentajeZoomVisualRealInfoLabel = new JLabel("%Z: N/A");
        this.formatoImagenInfoLabel = new JLabel("Fmt: N/A"); // Placeholder inicial
        this.formatoImagenInfoLabel.setToolTipText("Formato del archivo de imagen actual (Futuro)");

        // Aplicar color de texto (asumimos que todos usarán colorTextoSecundario de uiConfig)
        Color colorTextoInfo = (this.uiConfig != null && this.uiConfig.colorTextoSecundario != null)
                                ? this.uiConfig.colorTextoSecundario : Color.BLACK;
        
        this.nombreArchivoInfoLabel.setForeground(colorTextoInfo);
        this.indiceTotalInfoLabel.setForeground(colorTextoInfo);
        this.dimensionesOriginalesInfoLabel.setForeground(colorTextoInfo);
        this.tamanoArchivoInfoLabel.setForeground(colorTextoInfo);
        this.fechaArchivoInfoLabel.setForeground(colorTextoInfo);
        this.modoZoomNombreInfoLabel.setForeground(colorTextoInfo);
        this.porcentajeZoomVisualRealInfoLabel.setForeground(colorTextoInfo);

        // --- Ensamblaje con GridBagLayout ---

        // 1. Nombre del Archivo (Izquierda)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0; // No queremos que este label se expanda por sí mismo
        gbc.fill = GridBagConstraints.NONE;
        panel.add(this.nombreArchivoInfoLabel, gbc);

        // 2. Componente Elástico (Glue)
        gbc.gridx = 1;
        gbc.weightx = 1.0; // Este tomará todo el espacio extra
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalGlue(), gbc);

        // Resetear weightx y fill para los siguientes componentes
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END; // Alinear este grupo a la derecha del espacio que les queda

        // 3. Sección Centro (Propiedades Imagen) - Alineada a la derecha del glue
        gbc.gridx = 2;
        panel.add(this.dimensionesOriginalesInfoLabel, gbc);
        
        gbc.gridx = 3;
        panel.add(crearSeparadorVerticalBarraInfo(), gbc);

        gbc.gridx = 4;
        panel.add(this.tamanoArchivoInfoLabel, gbc);

        gbc.gridx = 5;
        panel.add(crearSeparadorVerticalBarraInfo(), gbc);

        gbc.gridx = 6;
        panel.add(this.fechaArchivoInfoLabel, gbc);

        // --- AÑADIR FORMATO IMAGEN AQUÍ ---
        gbc.gridx = 7; // Siguiente posición
        panel.add(crearSeparadorVerticalBarraInfo(), gbc); // Separador antes de formato

        gbc.gridx = 8; // Siguiente posición
        panel.add(this.formatoImagenInfoLabel, gbc); // Añadir el nuevo JLabel

        // 4. Separador Doble/Más Grande
        // Los gridx de los siguientes componentes se desplazan
        gbc.gridx = 9; // Antes era 7
        gbc.insets = new Insets(0, 8, 0, 8);
        panel.add(crearSeparadorVerticalBarraInfo(), gbc);
        gbc.insets = new Insets(0, 3, 0, 3);

        // 5. Sección Derecha (Estado App)
        gbc.gridx = 10; // Antes era 8
        panel.add(this.indiceTotalInfoLabel, gbc);

        gbc.gridx = 11; // Antes era 9
        panel.add(crearSeparadorVerticalBarraInfo(), gbc);
        
        gbc.gridx = 12; // Antes era 10
        panel.add(this.porcentajeZoomVisualRealInfoLabel, gbc);

        gbc.gridx = 13; // Antes era 11
        panel.add(crearSeparadorVerticalBarraInfo(), gbc);

        gbc.gridx = 14; // Antes era 12
        panel.add(this.modoZoomNombreInfoLabel, gbc);
        
        System.out.println("    [VisorView] PanelInfoSuperior (v2) creado.");
        return panel;
    } // FIN del metodo crearPanelInfoSuperior
    
    
    /**
     * Crea y configura el panel para la barra de estado/control inferior.
     * Este panel mostrará la ruta del archivo, indicadores de estado y mensajes de la aplicación.
     * También incluirá los nuevos controles para el porcentaje de zoom personalizado y el modo de zoom actual.
     *
     * @return El JPanel configurado para la barra de estado inferior.
     */
    private JPanel crearPanelEstadoInferior() {
        // --- SECCIÓN 0: Log de Inicio ---
        System.out.println("    [VisorView] Creando PanelEstadoInferior (v3.1 - con controles de zoom y prueba magenta)...");

        // --- SECCIÓN 1: CREACIÓN DEL PANEL PRINCIPAL DE LA BARRA DE ESTADO ---
        // 1.1. Crear el panel principal para la barra de estado inferior usando BorderLayout.
        //      Este panel (this.bottomStatusBar) se asigna al campo de instancia.
        //      Se usa un BorderLayout con un pequeño gap horizontal entre las regiones.
        this.bottomStatusBar = new JPanel(new BorderLayout(5, 0)); // 5px de espacio horizontal

        // --- SECCIÓN 2: CONFIGURACIÓN DE APARIENCIA BASE DEL PANEL PRINCIPAL ---
        // 2.1. Establecer el color de fondo.
        //      Intenta obtenerlo de uiConfig, si no, usa un color de fallback.
        if (this.uiConfig != null && this.uiConfig.colorFondoSecundario != null) {
            this.bottomStatusBar.setBackground(this.uiConfig.colorFondoSecundario);
        } else {
            this.bottomStatusBar.setBackground(Color.decode("#D6D9DF")); // Fallback si uiConfig o color no están disponibles
        }
        // 2.2. Asegurar que el panel sea opaco para que el color de fondo se muestre.
        this.bottomStatusBar.setOpaque(true);

        // 2.3. Añadir un borde para separar visualmente y dar padding.
        // 2.3.1. Borde superior (línea externa) para separarlo del contenido superior.
        Border lineaExternaStatus = BorderFactory.createMatteBorder(1, 0, 0, 0,
            (this.uiConfig != null && this.uiConfig.colorBorde != null) ? this.uiConfig.colorBorde : Color.GRAY);
        // 2.3.2. Padding interno para dar espacio alrededor del contenido.
        Border paddingInternoStatus = BorderFactory.createEmptyBorder(2, 5, 2, 5); // Arriba, Izquierda, Abajo, Derecha
        // 2.3.3. Combinar ambos bordes y asignarlos al panel.
        this.bottomStatusBar.setBorder(BorderFactory.createCompoundBorder(lineaExternaStatus, paddingInternoStatus));

        // --- SECCIÓN 3: SECCIÓN IZQUIERDA/CENTRO - RUTA DEL ARCHIVO ---
        // 3.1. Inicializar el JLabel para la ruta del archivo (campo de instancia).
        this.rutaCompletaArchivoLabel = new JLabel("Ruta: (ninguna imagen seleccionada)");
        // 3.2. Aplicar color de texto (desde uiConfig o fallback).
        if (this.uiConfig != null && this.uiConfig.colorTextoPrimario != null) {
            this.rutaCompletaArchivoLabel.setForeground(this.uiConfig.colorTextoPrimario);
        } else {
            this.rutaCompletaArchivoLabel.setForeground(Color.BLACK); // Fallback
        }
        // 3.3. Envolver el JLabel de la ruta en un panel con BorderLayout para que se expanda.
        JPanel panelRuta = new JPanel(new BorderLayout());
        panelRuta.setOpaque(false); // Hacerlo transparente para heredar el fondo de bottomStatusBar.
        panelRuta.add(this.rutaCompletaArchivoLabel, BorderLayout.CENTER);
        // 3.4. Añadir el panel de la ruta a la región CENTER de la barra de estado para que se expanda.
        this.bottomStatusBar.add(panelRuta, BorderLayout.CENTER);

        // --- SECCIÓN 4: SECCIÓN DERECHA - CONTENEDOR PARA CONTROLES Y MENSAJES ---
        // 4.1. Crear un panel contenedor para la parte derecha, usando BorderLayout.
        //      Este panel agrupará los controles y el label de mensajes.
        JPanel panelDerechoContenedor = new JPanel(new BorderLayout(5, 0)); // Espacio horizontal entre sub-secciones
        panelDerechoContenedor.setOpaque(false); // Transparente para heredar fondo.

            // --- SUB-SECCIÓN 4A: Panel para Controles Rápidos e Indicadores (Iconos y Controles de Zoom) ---
            // 4A.1. Crear panel con FlowLayout alineado a la DERECHA para los iconos/controles.
            //       Esto hará que los componentes se apilen desde la derecha.
            JPanel panelControlesInferior = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)); // 4px horizontal, 0px vertical gap
            panelControlesInferior.setOpaque(false); // Transparente.

                // 4A.2. Definir tamaño para los iconos de esta barra (ZM, Prop, SubC, Modo Zoom).
                int iconSizeBarraInf = 18; // Puedes ajustar este valor (ej. 16, 20)
                Dimension indicadorIconoDimension = new Dimension(iconSizeBarraInf + 6, iconSizeBarraInf + 4); // Icono + padding

                // 4A.3. Crear e inicializar JLabel para el icono de Zoom Manual (ZM) - Campo de instancia.
                ImageIcon iconoZM = (this.uiConfig != null && this.uiConfig.iconUtils != null) ?
                                    this.uiConfig.iconUtils.getScaledIcon("3001-Zoom_48x48.png", iconSizeBarraInf, iconSizeBarraInf) : null;
                this.iconoZoomManualLabel = new JLabel(iconoZM);
                this.iconoZoomManualLabel.setOpaque(true); // Para que el setBackground funcione.
                this.iconoZoomManualLabel.setHorizontalAlignment(SwingConstants.CENTER);
                this.iconoZoomManualLabel.setVerticalAlignment(SwingConstants.CENTER);
                this.iconoZoomManualLabel.setPreferredSize(indicadorIconoDimension);
                this.iconoZoomManualLabel.setToolTipText("Zoom Manual: Desactivado"); // Tooltip inicial.

                // 4A.4. Crear e inicializar JLabel para el icono de Mantener Proporciones (Prop) - Campo de instancia.
                ImageIcon iconoProp = (this.uiConfig != null && this.uiConfig.iconUtils != null) ?
                                      this.uiConfig.iconUtils.getScaledIcon("7002-Mantener_Proporciones_48x48.png", iconSizeBarraInf, iconSizeBarraInf) : null;
                this.iconoMantenerProporcionesLabel = new JLabel(iconoProp);
                this.iconoMantenerProporcionesLabel.setOpaque(true);
                this.iconoMantenerProporcionesLabel.setHorizontalAlignment(SwingConstants.CENTER);
                this.iconoMantenerProporcionesLabel.setVerticalAlignment(SwingConstants.CENTER);
                this.iconoMantenerProporcionesLabel.setPreferredSize(indicadorIconoDimension);
                this.iconoMantenerProporcionesLabel.setToolTipText("Mantener Proporciones: Desactivado");

                // 4A.5. Crear e inicializar JLabel para el icono de Modo Subcarpetas (SubC) - Campo de instancia.
                ImageIcon iconoSubC = (this.uiConfig != null && this.uiConfig.iconUtils != null) ?
                                      this.uiConfig.iconUtils.getScaledIcon("7001-Subcarpetas_48x48.png", iconSizeBarraInf, iconSizeBarraInf) : null;
                this.iconoModoSubcarpetasLabel = new JLabel(iconoSubC);
                this.iconoModoSubcarpetasLabel.setOpaque(true);
                this.iconoModoSubcarpetasLabel.setHorizontalAlignment(SwingConstants.CENTER);
                this.iconoModoSubcarpetasLabel.setVerticalAlignment(SwingConstants.CENTER);
                this.iconoModoSubcarpetasLabel.setPreferredSize(indicadorIconoDimension);
                this.iconoModoSubcarpetasLabel.setToolTipText("Incluir Subcarpetas: Desactivado");

                // 4A.6. Crear e inicializar JLabel para el Porcentaje de Zoom Personalizado - Campo de instancia.
                this.porcentajeZoomPersonalizadoLabel = new JLabel("Z: 100%"); // Texto inicial.
                this.porcentajeZoomPersonalizadoLabel.setName("porcentajeZoomPersonalizadoLabel");
                this.porcentajeZoomPersonalizadoLabel.setToolTipText("Porcentaje de zoom personalizado. Click para cambiar.");
                this.porcentajeZoomPersonalizadoLabel.setOpaque(true); // Para que el fondo sea visible.
                this.porcentajeZoomPersonalizadoLabel.setHorizontalAlignment(SwingConstants.CENTER);
                if (this.uiConfig != null) {
                    this.porcentajeZoomPersonalizadoLabel.setForeground(this.uiConfig.colorTextoPrimario);
                    this.porcentajeZoomPersonalizadoLabel.setBackground(this.uiConfig.colorFondoSecundario); // Fondo inicial
                    this.porcentajeZoomPersonalizadoLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5)); // Padding
                }

                // 4A.7. Crear e inicializar JButton para el Icono del Modo de Zoom Actual - Campo de instancia.
                this.modoZoomActualIconoBoton = new JButton();
                this.modoZoomActualIconoBoton.setName("modoZoomActualIconoBoton");
                this.modoZoomActualIconoBoton.setToolTipText("Modo de zoom actual. Click para cambiar.");
                this.modoZoomActualIconoBoton.setFocusable(false); // Para que no robe el foco.
                this.modoZoomActualIconoBoton.setPreferredSize(new Dimension(iconSizeBarraInf + 8, iconSizeBarraInf + 6)); // Tamaño similar a otros iconos.
                
                // 4A.8. Añadir los componentes al panel 'panelControlesInferior'.
                panelControlesInferior.add(this.iconoZoomManualLabel);         // Más a la derecha de los indicadores
                panelControlesInferior.add(this.iconoMantenerProporcionesLabel);
                panelControlesInferior.add(this.iconoModoSubcarpetasLabel);

                // Separador visual entre los indicadores de estado y los controles de zoom.
                panelControlesInferior.add(Box.createHorizontalStrut(5)); // Pequeño espacio.
                javax.swing.JSeparator separadorVerticalZoom = new javax.swing.JSeparator(SwingConstants.VERTICAL);
                separadorVerticalZoom.setPreferredSize(new Dimension(2, iconSizeBarraInf + 2)); // Hacerlo visible.
                if (this.uiConfig != null && this.uiConfig.colorBorde != null) {
                    separadorVerticalZoom.setForeground(this.uiConfig.colorBorde);
                    separadorVerticalZoom.setBackground(this.uiConfig.colorBorde); // Algunos L&F lo necesitan
                } else {
                     separadorVerticalZoom.setForeground(Color.GRAY);
                }
                panelControlesInferior.add(separadorVerticalZoom);
                panelControlesInferior.add(Box.createHorizontalStrut(5)); // Pequeño espacio.

                panelControlesInferior.add(this.porcentajeZoomPersonalizadoLabel); // Label de porcentaje de zoom.
                panelControlesInferior.add(Box.createHorizontalStrut(2));      // Espacio pequeño.
                panelControlesInferior.add(this.modoZoomActualIconoBoton);        // Botón de icono de modo zoom.

            // 4A.9. Añadir el panel de controles al 'panelDerechoContenedor'.
            panelDerechoContenedor.add(panelControlesInferior, BorderLayout.CENTER);

            // --- SUB-SECCIÓN 4B: Sección Extrema Derecha - Mensajes de la Aplicación ---
            // 4B.1. Inicializar el JLabel para mensajes (campo de instancia).
            this.mensajesAppLabel = new JLabel(" "); // Espacio para que tenga altura inicial.
            // 4B.2. Aplicar color de texto (desde uiConfig o fallback).
            if (this.uiConfig != null && this.uiConfig.colorTextoSecundario != null) {
                this.mensajesAppLabel.setForeground(this.uiConfig.colorTextoSecundario);
            } else {
                this.mensajesAppLabel.setForeground(Color.DARK_GRAY); // Fallback
            }
            // 4B.3. Alinear texto a la derecha.
            this.mensajesAppLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            // 4B.4. Darle un ancho preferido para que no colapse si el mensaje es corto.
            //       La altura se ajustará automáticamente.
            this.mensajesAppLabel.setPreferredSize(new Dimension(200, this.mensajesAppLabel.getPreferredSize().height));
            // 4B.5. Añadir el label de mensajes a la región EAST del 'panelDerechoContenedor'.
            panelDerechoContenedor.add(this.mensajesAppLabel, BorderLayout.EAST);

        // 4.2. Añadir el 'panelDerechoContenedor' (que ahora tiene controles y mensajes)
        //      a la región EAST de la barra de estado principal (this.bottomStatusBar).
        this.bottomStatusBar.add(panelDerechoContenedor, BorderLayout.EAST);

        // --- SECCIÓN 5: Log Final y Retorno ---
        System.out.println("    [VisorView] PanelEstadoInferior (v3.1) creado y ensamblado.");
        return this.bottomStatusBar; // Devolver el panel principal de la barra inferior.

    } // --- FIN del metodo crearPanelEstadoInferior ---
    
    
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
        
        scrollPaneListaNombres.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPaneListaNombres.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); 
        
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
                    servicioThumbsParam,
                    modeloVisorParam,
                    thumbWidth,
                    thumbHeight,
                    mostrarNombres,
                    uiConfig.colorFondoSecundario,    // colorFondoDefault
                    uiConfig.colorSeleccionFondo,   // colorFondoSeleccionDefault
                    uiConfig.colorTextoPrimario,    // colorTextoDefault
                    uiConfig.colorSeleccionTexto,   // colorTextoSeleccionDefault
                    uiConfig.colorBordeSeleccionActiva, // colorBordeSeleccion (o Color.ORANGE si era placeholder)
                    // YA NO PASAS el iconoErrorGeneral aquí
                    this.uiConfig.iconUtils          // <<< NUEVO PARÁMETRO: La instancia de IconUtils
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
            
            
            //TEST
            @Override
            protected void paintComponent(Graphics g) {
                // ---- DIBUJA TU FONDO PRIMERO ----
                int width = getWidth();
                int height = getHeight();

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
                    // Si no es fondo a cuadros, y quieres un color de fondo sólido específico
                    // que tú controlas, píntalo. Si quieres el fondo por defecto del JLabel,
                    // entonces setOpaque(true) y deja que el super lo pinte.
                    // Asumiendo que quieres tu propio color de fondo sólido:
                    if (uiConfig != null && uiConfig.colorFondoSecundario != null) { // O el color que uses como fondo de visor
                        g.setColor(uiConfig.colorFondoSecundario);
                    } else {
                        g.setColor(Color.DARK_GRAY); // Tu fallback
                    }
                    g.fillRect(0, 0, width, height);
                }

                // ---- AHORA LLAMA AL SUPER (para que dibuje icono/texto encima de tu fondo) ----
                super.paintComponent(g);

                // ---- LUEGO DIBUJA LA IMAGEN REESCALADA (si existe, encima de todo) ----
                if (imagenReescaladaView != null) {
                    Graphics2D g2dImagen = (Graphics2D) g.create();
                    try {
                        g2dImagen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        // ... resto de tus hints ...
                        
                        int baseW = imagenReescaladaView.getWidth(null);
                        int baseH = imagenReescaladaView.getHeight(null);

                        if (baseW > 0 && baseH > 0) {
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
                
            }// FIN del paintComponent
            
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
    } // FIN del metodo inicializarEtiquetaMostrarImagen
    
    
	/**
	 * Muestra una indicación de error en el área principal de visualización de imágenes (etiquetaImagen).
	 * Si se ha cargado un icono de error general (this.iconoErrorGeneral), lo muestra.
	 * De lo contrario, muestra un mensaje de texto formateado con HTML.
	 * También limpia cualquier imagen previamente mostrada por el sistema de pintura personalizado.
	 *
	 * @param nombreArchivo    El nombre del archivo que no se pudo cargar (para mostrar en el mensaje de error).
	 * @param mensajeDetallado Una descripción más detallada del error (para mostrar en el mensaje de error).
	 */
	public void mostrarErrorEnVisorPrincipal(String nombreArchivo, String mensajeDetallado) {
	    if (this.etiquetaImagen == null) {
	        System.err.println("ERROR CRÍTICO [VisorView.mostrarErrorEnVisorPrincipal]: etiquetaImagen es null.");
	        return;
	    }
	
	    System.out.println("  [VisorView] Mostrando error en visor principal para archivo: " + (nombreArchivo != null ? nombreArchivo : "desconocido"));
	
	    this.imagenReescaladaView = null; // Asegurar que no se intente pintar una imagen válida anterior
	                                       // por el método paintComponent personalizado.
	    // Si usas un flag adicional en paintComponent para el estado de error, actualízalo aquí:
	    // this.errorAlCargarImagenActual = true;
	
	    if (this.iconoErrorGeneral != null) {
	        // Mostrar el icono de error general.
	        // Puedes decidir escalarlo aquí si es necesario para el visor principal,
	        // o si ya lo escalaste a un tamaño adecuado al cargarlo en el constructor.
	        // Ejemplo si quieres escalarlo ahora a un tamaño específico:
	        // ImageIcon iconoParaDisplay = this.uiConfig.iconUtils.scaleImageIcon(this.iconoErrorGeneral, 256, 256); // Ajusta tamaño
	        // this.etiquetaImagen.setIcon(iconoParaDisplay != null ? iconoParaDisplay : this.iconoErrorGeneral);
	
	        // Mostrando el icono tal cual se cargó (o como se escaló en el constructor)
	        this.etiquetaImagen.setIcon(this.iconoErrorGeneral);
	        this.etiquetaImagen.setText(null); // Limpiar cualquier texto ("Cargando...", etc.)
	         
	         
	        this.etiquetaImagen.revalidate(); // Puede ayudar si el contenido cambió de texto a icono o viceversa
	        this.etiquetaImagen.repaint();
	         
	    } else {
	        // Fallback a mensaje de texto si iconoErrorGeneral no se pudo cargar.
	        this.etiquetaImagen.setIcon(null);
	        String mensajeHtml = "<html><body style='text-align:center;'>" +
	                             "<b>Error al Cargar Imagen</b><br><br>" +
	                             (nombreArchivo != null ? "Archivo: " + escapeHtml(nombreArchivo) + "<br>" : "") +
	                             "<font color='gray' size='-1'><i>" +
	                             (mensajeDetallado != null ? escapeHtml(mensajeDetallado) : "Detalles no disponibles.") +
	                             "</i></font>" +
	                             "</body></html>";
	        this.etiquetaImagen.setText(mensajeHtml);
	         
	        // Establecer color de texto para el error
	        if (uiConfig != null && uiConfig.colorTextoPrimario != null) {
	            this.etiquetaImagen.setForeground(Color.RED); // O un color de error del tema
	        } else {
	            this.etiquetaImagen.setForeground(Color.RED);
	        }
	    }
	
	    this.etiquetaImagen.setHorizontalAlignment(SwingConstants.CENTER);
	    this.etiquetaImagen.setVerticalAlignment(SwingConstants.CENTER);
	
	    // Importante si tu etiquetaImagen tiene un paintComponent personalizado para el fondo
	    this.etiquetaImagen.setOpaque(false); 
	    this.etiquetaImagen.repaint();
	    System.out.println("  [VisorView] etiquetaImagen repintada para mostrar error/icono de error.");
	}

    
    /**
     * Escapa caracteres HTML básicos para evitar problemas al mostrar texto en un JLabel con HTML.
     * @param text El texto a escapar.
     * @return El texto con caracteres HTML escapados, o una cadena vacía si text es null.
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&")
                   .replace("<", "<")
                   .replace(">", ">")
                   .replace("\n", "<br>"); // Convertir saltos de línea a <br> para HTML
    }
    
    
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
    
    
    /**
     * Establece la imagen principal que se va a mostrar, limpiando cualquier
     * indicador de error o carga previo.
     *
     * @param imagenReescalada La imagen ya reescalada y lista para ser referenciada por paintComponent.
     * @param zoom El factor de zoom actual para esta imagen.
     * @param offsetX El desplazamiento X actual para esta imagen.
     * @param offsetY El desplazamiento Y actual para esta imagen.
     */
    public void setImagenMostrada(Image imagenReescalada, double zoom, int offsetX, int offsetY) {
        if (this.etiquetaImagen == null) return;

        // this.errorAlCargarImagenActual = false; // Si usas un flag para paintComponent
        this.imagenReescaladaView = imagenReescalada; // Para tu paintComponent personalizado
        this.zoomFactorView = zoom;
        this.imageOffsetXView = offsetX;
        this.imageOffsetYView = offsetY;

        // Limpiar cualquier texto o icono de error/carga del JLabel
        this.etiquetaImagen.setText(null);
        this.etiquetaImagen.setIcon(null);

        this.etiquetaImagen.repaint(); // paintComponent usará imagenReescaladaView
    }
    
    
    /**
     * Limpia el área de visualización de la imagen principal, eliminando la imagen
     * actual, cualquier icono de error, o texto de carga.
     * El paintComponent dibujará el fondo (a cuadros o sólido).
     */
    public void limpiarImagenMostrada() {
        if (this.etiquetaImagen == null) return;

        // this.errorAlCargarImagenActual = false; // Si usas un flag para paintComponent
        this.imagenReescaladaView = null; // No hay imagen para paintComponent
        this.zoomFactorView = 1.0;
        this.imageOffsetXView = 0;
        this.imageOffsetYView = 0;

        // Limpiar cualquier texto o icono de error/carga del JLabel
        this.etiquetaImagen.setText(null);
        this.etiquetaImagen.setIcon(null);

        this.etiquetaImagen.repaint(); // paintComponent dibujará el fondo
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
            System.out.println("  [VisorView setFileListVisible] Cambiando visibilidad de panelContenedorIzquierdoSplit a: " + visible);
            this.panelContenedorIzquierdoSplit.setVisible(visible);

            if (this.splitPane != null) {
                if (visible) {
                    SwingUtilities.invokeLater(() -> {
                        // Intenta primero con una posición fija. Si funciona, podemos hacerla configurable.
                        double dividerLocationPercentage = 0.25; // Ejemplo: 25% para el panel izquierdo
                        int newDividerLocation = (int) (splitPane.getWidth() * dividerLocationPercentage);
                        
                        System.out.println("    -> [EDT] Intentando splitPane.setDividerLocation(" + newDividerLocation + ")");
                        this.splitPane.setDividerLocation(newDividerLocation);

                        // this.splitPane.resetToPreferredSizes(); // Lo has comentado, está bien probar sin él primero.

                        this.splitPane.revalidate();
                        this.splitPane.repaint();
                        System.out.println("    -> [EDT] JSplitPane revalidado y repintado después de setDividerLocation.");
                    });
                }
                // No es necesario hacer mucho cuando se oculta
            } else { 
                System.err.println("WARN [VisorView setFileListVisible]: splitPane es null.");
            }

            revalidateFrame(); // Llama a this.revalidate() y this.repaint() del JFrame
            System.out.println("  [VisorView setFileListVisible] Frame revalidado y repintado.");

        } else if (this.panelContenedorIzquierdoSplit == null) {
            System.err.println("ERROR [VisorView setFileListVisible]: panelContenedorIzquierdoSplit es null.");
        } else {
            System.out.println("  [VisorView setFileListVisible] panelContenedorIzquierdoSplit ya está en estado: " + visible);
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
    
    
    /**
     * Muestra un indicador de "Cargando..." en el área principal de la imagen.
     * Limpia cualquier imagen o icono de error previo.
     *
     * @param mensaje El mensaje a mostrar (ej. "Cargando: nombre_archivo...").
     */
    public void mostrarIndicadorCargaImagenPrincipal(String mensaje) {
        if (this.etiquetaImagen == null) return;

        String mensajeAMostrar = (mensaje != null && !mensaje.trim().isEmpty()) ? mensaje : "Cargando...";

        // this.errorAlCargarImagenActual = false; // Si usas un flag para paintComponent
        this.imagenReescaladaView = null; // No hay imagen mientras se carga
        this.etiquetaImagen.setIcon(null);  // No mostrar icono de error
        this.etiquetaImagen.setText(mensajeAMostrar); // Mostrar texto de carga

        if (this.uiConfig != null && this.uiConfig.colorTextoPrimario != null) {
             this.etiquetaImagen.setForeground(this.uiConfig.colorTextoPrimario);
        } else {
             this.etiquetaImagen.setForeground(Color.BLACK); // Fallback
        }
        this.etiquetaImagen.setHorizontalAlignment(SwingConstants.CENTER);
        this.etiquetaImagen.setVerticalAlignment(SwingConstants.CENTER);
        this.etiquetaImagen.setOpaque(false); // Para que el fondo de paintComponent se vea
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
    
    
    /**
     * Solicita un refresco completo de los renderers de la lista de miniaturas.
     * Esto es útil cuando cambian las configuraciones que afectan la apariencia
     * de las celdas de las miniaturas (ej. mostrar/ocultar nombres, cambio de tema,
     * cambio de tamaño de miniaturas).
     * Crea una nueva instancia del renderer con la configuración actual y la asigna
     * a la JList de miniaturas, forzando una revalidación y repintado.
     */
    public void solicitarRefrescoRenderersMiniaturas() {
        System.out.println("[VisorView] Iniciando solicitud de refresco para renderers de miniaturas...");

        // --- 1. VALIDACIÓN DE DEPENDENCIAS ESENCIALES ---
        //    Necesitamos la lista de miniaturas, el modelo, el servicio de thumbs,
        //    y la configuración de UI (que contiene IconUtils y ConfigurationManager).
        if (this.listaMiniaturas == null) {
            System.err.println("WARN [solicitarRefrescoRenderersMiniaturas]: listaMiniaturas es null. No se puede refrescar.");
            return;
        }
        if (this.model == null) {
            System.err.println("WARN [solicitarRefrescoRenderersMiniaturas]: VisorModel (this.model) es null.");
            return;
        }
        if (this.servicioThumbs == null) {
            System.err.println("WARN [solicitarRefrescoRenderersMiniaturas]: ThumbnailService (this.servicioThumbs) es null.");
            return;
        }
        if (this.uiConfig == null) {
            System.err.println("WARN [solicitarRefrescoRenderersMiniaturas]: ViewUIConfig (this.uiConfig) es null.");
            return;
        }
        if (this.uiConfig.configurationManager == null) {
            System.err.println("WARN [solicitarRefrescoRenderersMiniaturas]: uiConfig.configurationManager es null.");
            return;
        }
        if (this.uiConfig.iconUtils == null) { // Nueva verificación
            System.err.println("WARN [solicitarRefrescoRenderersMiniaturas]: uiConfig.iconUtils es null. No se podrá pasar al renderer.");
            // Podrías decidir continuar sin iconUtils y que el renderer maneje el null, o retornar.
            // Por ahora, continuaremos, pero el icono de error en miniatura podría no funcionar.
        }


        // --- 2. OBTENER CONFIGURACIONES ACTUALES ---
        //    Leer los valores actuales de la configuración que afectan al renderer.
        boolean mostrarNombresActual = this.uiConfig.configurationManager.getBoolean("miniaturas.ui.mostrar_nombres", true);
        int thumbWidth = this.uiConfig.configurationManager.getInt("miniaturas.tamano.normal.ancho", 40);
        int thumbHeight = this.uiConfig.configurationManager.getInt("miniaturas.tamano.normal.alto", 40);

        // Obtener los colores del tema actual desde uiConfig
        Color colorFondoMiniatura = this.uiConfig.colorFondoSecundario;
        Color colorFondoSeleccionMiniatura = this.uiConfig.colorSeleccionFondo;
        Color colorTextoMiniatura = this.uiConfig.colorTextoPrimario;
        Color colorTextoSeleccionMiniatura = this.uiConfig.colorSeleccionTexto;
        Color colorBordeSeleccionMiniatura = this.uiConfig.colorBordeSeleccionActiva; // O tu Color.ORANGE si era un placeholder

        System.out.println("  [Refresco Renderers] Configs actuales: MostrarNombres=" + mostrarNombresActual +
                           ", AnchoThumb=" + thumbWidth + ", AltoThumb=" + thumbHeight);

        // --- 3. CREAR UNA NUEVA INSTANCIA DEL RENDERER ---
        //    Se usa la nueva firma del constructor que espera IconUtils.
        MiniaturaListCellRenderer newRenderer = new MiniaturaListCellRenderer(
            this.servicioThumbs,
            this.model, // Pasar el VisorModel de la VisorView
            thumbWidth,
            thumbHeight,
            mostrarNombresActual,
            colorFondoMiniatura,
            colorFondoSeleccionMiniatura,
            colorTextoMiniatura,
            colorTextoSeleccionMiniatura,
            colorBordeSeleccionMiniatura,
            // this.iconoErrorGeneral, // YA NO SE PASA EL ICONO PRE-CARGADO
            this.uiConfig.iconUtils     // <<< SE PASA LA INSTANCIA DE IconUtils
        );
        System.out.println("  [Refresco Renderers] Nueva instancia de MiniaturaListCellRenderer creada.");

        // --- 4. APLICAR EL NUEVO RENDERER Y ACTUALIZAR LA JLIST ---
        this.listaMiniaturas.setCellRenderer(newRenderer);
        System.out.println("    -> Nuevo renderer asignado a listaMiniaturas.");

        // Es crucial actualizar las dimensiones fijas de las celdas si el nuevo renderer
        // las calcula de manera diferente (ej. si mostrarNombres afecta la altura).
        this.listaMiniaturas.setFixedCellHeight(newRenderer.getAlturaCalculadaDeCelda());
        this.listaMiniaturas.setFixedCellWidth(newRenderer.getAnchoCalculadaDeCelda());
        System.out.println("    -> AlturaFijaCelda: " + newRenderer.getAlturaCalculadaDeCelda() +
                           ", AnchoFijoCelda: " + newRenderer.getAnchoCalculadaDeCelda());

        // Forzar a la JList a recalcular su layout y repintarse completamente.
        this.listaMiniaturas.revalidate();
        this.listaMiniaturas.repaint();
        System.out.println("    -> listaMiniaturas revalidada y repintada.");

        // Opcional: Si el cambio en el tamaño de celda es significativo, podrías
        // necesitar notificar al ListCoordinator para que recalcule la ventana
        // deslizante de miniaturas, aunque a menudo un revalidate/repaint es suficiente
        // si la JList está dentro de un JScrollPane que se ajusta bien.
        // if (controller != null && controller.getListCoordinator() != null) {
        //     int indiceActual = controller.getListCoordinator().getIndiceOficialSeleccionado();
        //     if (indiceActual != -1) {
        //         controller.actualizarModeloYVistaMiniaturas(indiceActual);
        //     }
        // }

        System.out.println("[VisorView] Refresco de renderers de miniaturas completado.");
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
    public JLabel getRutaCompletaArchivoLabel() { return this.rutaCompletaArchivoLabel; }
    public JLabel getIconoZoomManualLabel() { return iconoZoomManualLabel; }
    public JLabel getIconoMantenerProporcionesLabel() { return iconoMantenerProporcionesLabel; }
    public JLabel getIconoModoSubcarpetasLabel() { return iconoModoSubcarpetasLabel; }
    public JLabel getTamanoArchivoInfoLabel() {return this.tamanoArchivoInfoLabel;}
    public JLabel getFechaArchivoInfoLabel() {return this.fechaArchivoInfoLabel;}
    public JLabel getPorcentajeZoomPersonalizadoLabel() { return this.porcentajeZoomPersonalizadoLabel; }
//    public JButton getPorcentajeZoomPersonalizadoBoton() { return this.porcentajeZoomPersonalizadoBoton; }
    public JButton getModoZoomActualIconoBoton() { return this.modoZoomActualIconoBoton; }
    public JLabel getMensajesAppLabel() { return this.mensajesAppLabel; }
 	
    public JPanel getPanelBarraSuperior() {return panelInfoSuperior;}
    public JPanel getPanelBarraEstado() {return bottomStatusBar;}
    public JLabel getFormatoImagenInfoLabel() {return formatoImagenInfoLabel;}
    
    
    public JPanel getPanelContenedorIzquierdoSplit() {return this.panelContenedorIzquierdoSplit;}
} // Fin clase VisorView