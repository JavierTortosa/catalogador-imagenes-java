package vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component; // Import faltante para getParent
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import controlador.VisorController;
import controlador.commands.AppActionCommands;
import modelo.VisorModel;
import servicios.ConfigurationManager;
import servicios.image.ThumbnailService;
import vista.config.ViewUIConfig;
import vista.renderers.MiniaturaListCellRenderer;
import vista.renderers.NombreArchivoRenderer;

/**
 * Clase Vista principal (JFrame) del Visor de Imágenes. Responsable de
 * construir la interfaz gráfica, ensamblar los componentes, y exponerlos para
 * que el Controlador los maneje. Utiliza el enfoque de dos JList (nombres y
 * miniaturas) que comparten el mismo ListModel. La navegación por teclado es
 * manejada por defecto por la JList que tenga el foco.
 */
public class VisorView extends JFrame
{ 

	private static final long serialVersionUID = 2L; // Incrementar SUID por cambios

	// --- Componentes UI ---
	private JList<String> listaNombres;
	private JList<String> listaMiniaturas;
	private JScrollPane scrollListaMiniaturas;
	private JLabel etiquetaImagen;
	private JSplitPane splitPane;
	private JTextField textoRuta;
	private JPanel panelPrincipal;
	private JPanel panelIzquierdo;
	private JPanel panelDeBotones; // Referencia al panel de la toolbar recibido

    // --- Mapas para acceder a componentes por nombre ---
    // ¡Ahora se inicializan y se asignan en el constructor!
    private Map<String, JButton> botonesPorNombre;
    private Map<String, JMenuItem> menuItemsPorNombre;

	// --- Referencias Externas y Configuración UI ---
	private ViewUIConfig uiConfig; // guarda la configuracion
	private final VisorModel model; // Guardar referencia al modelo si otros métodos lo usan
    private final ThumbnailService servicioThumbs; // Guardar referencia si otros métodos lo usan
    private final JMenuBar mainMenuBar; // <-- Campo para guardar la menubar recibida
    private final JPanel mainToolbarPanel; // <-- Campo para guardar la toolbar recibida

    private int miniaturaScrollPaneHeight;
    private DefaultListModel<String> modeloLista; // Referencia al modelo compartido

    // --- Constantes para represenracion de las miniaturas ---
    private static final int MINIMUM_THUMBNAIL_CELL_DIMENSION 			= 15; //tamaño de la celda
    private static final int THUMBNAIL_CELL_HORIZONTAL_PADDING 			= 15; //espacio entre miniaturas
    private static final int THUMBNAIL_CELL_VERTICAL_PADDING_WITH_TEXT 	= 30; //espacio vertical con texto
    private static final int THUMBNAIL_CELL_VERTICAL_PADDING_NO_TEXT 	= 10; //espacio vertical sin texto
    
    // --- Constante Animacion de Botones
    private static final int BUTTON_CLICK_ANIMATION_DURATION_MS = 200;
	
	// --- Estado de Dibujo Imagen Principal ---
	private Image imagenReescaladaView;
	private double zoomFactorView = 1.0;
	private int imageOffsetXView = 0;
	private int imageOffsetYView = 0;

	// --- Estado Fondo a Cuadros ---
	private boolean fondoACuadrosActivado = false;
	private Color colorCuadroClaro = new Color(204, 204, 204);
	private Color colorCuadroOscuro = new Color(255, 255, 255);
	private final int TAMANO_CUADRO = 16;
	

  
	/**
     * Constructor MODIFICADO de la ventana principal.
     * Recibe configuración, modelo, servicio de miniaturas y los componentes
     * de menú y toolbar ya construidos.
     *
     * @param miniaturaPanelHeight Altura deseada para el scrollpane de miniaturas.
     * @param config               Objeto ViewUIConfig con parámetros de apariencia y referencias.
     * @param modelo               El VisorModel principal.
     * @param servicioThumbs       El ThumbnailService para renderers.
     * @param menuBar              La JMenuBar ya construida por MenuBarBuilder.
     * @param toolbarPanel         El JPanel de la toolbar ya construido por ToolbarBuilder.
     * @param menuItems            El mapa de JMenuItems (clave larga -> item) generado por MenuBarBuilder.
     * @param botones              El mapa de JButtons (clave larga -> botón) generado por ToolbarBuilder.
     */
	public VisorView(
            int miniaturaPanelHeight,
            ViewUIConfig config,
            VisorModel modelo,
            ThumbnailService servicioThumbs,
            // --- Nuevos Parámetros ---
            JMenuBar menuBar,
            JPanel toolbarPanel,
            Map<String, JMenuItem> menuItems,
            Map<String, JButton> botones
    ) {	
	
		// 1. Llamada al constructor de JFrame
		super("Visor de Imágenes");
		System.out.println("[VisorView Constructor] Iniciando...");

		// 2. Validación y Almacenamiento de Dependencias y Config
		this.uiConfig = Objects.requireNonNull(config, "ViewUIConfig no puede ser null");
		this.model = Objects.requireNonNull(modelo, "VisorModel no puede ser null");
        this.servicioThumbs = Objects.requireNonNull(servicioThumbs, "ThumbnailService no puede ser null");
		this.miniaturaScrollPaneHeight = miniaturaPanelHeight > 0 ? miniaturaPanelHeight : 100;
		
		// --- 3. Almacenar Menú, Toolbar y Mapas Recibidos ---
        this.mainMenuBar = menuBar; // Guardar referencia
        this.mainToolbarPanel = toolbarPanel; // Guardar referencia
        this.menuItemsPorNombre = (menuItems != null) ? menuItems : new HashMap<>(); // Asignar mapa recibido
        this.botonesPorNombre = (botones != null) ? botones : new HashMap<>();   // Asignar mapa recibido
        System.out.println("  [Constructor] Menú, Toolbar y Mapas recibidos.");
//        System.out.println("    -> Tamaño mapa Menús recibido: " + this.menuItemsPorNombre.size());
//        System.out.println("    -> Tamaño mapa Botones recibido: " + this.botonesPorNombre.size());
//		
//		System.out.println("  [Constructor] Dependencias validadas (Config, Modelo, Thumbs).");

		// 4. Configuración Básica del JFrame
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		getContentPane().setBackground(this.uiConfig.colorFondoPrincipal);
		System.out.println("  [Constructor] Configuración básica JFrame completada.");

		// 5. Obtener referencia inicial al modelo de la lista
        this.modeloLista = modelo.getModeloLista(); // Asignar PRIMERO
        
        if (this.modeloLista == null) {
            System.err.println("ERROR CRÍTICO [VisorView Constructor]: VisorModel.getModeloLista() devolvió null. Creando uno vacío.");
            this.modeloLista = new DefaultListModel<>();
        }
        
        System.out.println("  [Constructor] Referencia a modeloLista obtenida (Tamaño inicial: " + this.modeloLista.getSize() + ")");

		// 6. Crear e Inicializar TODOS los Componentes Internos
		// Se pasan las referencias necesarias a este método.
		System.out.println("  [Constructor] Llamando a inicializarComponentes...");
		
		inicializarComponentes(this.modeloLista); // Pasar solo el modelo de lista
		
		System.out.println("  [Constructor] Componentes internos inicializados.");

		// 7. Restaurar Estado de la Ventana (Tamaño/Posición/Maximizado)
		boolean restoredState = false;
		ConfigurationManager cfg = this.uiConfig.configurationManager; // Obtener de uiConfig

		if (cfg != null)
		{
			System.out.println("  [Constructor] Intentando restaurar estado de ventana...");

			try
			{
				boolean wasMaximized = cfg.getBoolean(ConfigurationManager.KEY_WINDOW_MAXIMIZED, false);

				if (wasMaximized)
				{
					setExtendedState(JFrame.MAXIMIZED_BOTH);
					restoredState = true;
					System.out.println("    -> Estado MAXIMIZED restaurado.");
				} else
				{
					int x = cfg.getInt(ConfigurationManager.KEY_WINDOW_X, -1);
					int y = cfg.getInt(ConfigurationManager.KEY_WINDOW_Y, -1);
					int w = cfg.getInt(ConfigurationManager.KEY_WINDOW_WIDTH, -1);
					int h = cfg.getInt(ConfigurationManager.KEY_WINDOW_HEIGHT, -1);

					if (w > 50 && h > 50)
					{
						java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
						w = Math.min(w, screenSize.width);
						h = Math.min(h, screenSize.height);
						x = Math.max(0, Math.min(x, screenSize.width - w));
						y = Math.max(0, Math.min(y, screenSize.height - h));
						setBounds(x, y, w, h);
						restoredState = true;
						System.out.println("    -> Bounds restaurados a: " + getBounds());
					} else
					{
						System.out.println("    -> Bounds no válidos en config.");
					}
				}
			} catch (Exception e)
			{
				System.err.println("    -> ERROR restaurando estado: " + e.getMessage());
			}
		} else
		{
			System.err.println("WARN [VisorView Constructor]: ConfigurationManager es null en uiConfig.");
		}
		System.out.println("  [Constructor] Intento de restauración de estado finalizado.");

		// 8. Dimensionamiento y Posicionamiento Final
		if (!restoredState)
		{
			System.out.println("  [Constructor] No se restauró estado. Usando pack() y centrando.");
			pack(); // Ajustar al contenido si no se restauró
			setLocationRelativeTo(null); // Centrar
		} else
		{
			System.out.println("  [Constructor] Estado restaurado. Omitiendo pack()/centrado. Validando layout...");
			validate(); // Validar layout si se usó setBounds/setExtendedState
		}
		System.out.println("  [Constructor] Dimensionamiento/Posicionamiento finalizado.");

		// 9. Establecer Posición Inicial del Divisor (en EDT)
		SwingUtilities.invokeLater( () -> {

			if (splitPane != null)
			{
				splitPane.setDividerLocation(0.25);
				System.out.println("  [Constructor EDT] Divisor JSplitPane establecido.");
			} else
			{
				System.err.println("ERROR [Constructor EDT]: splitPane es null al ajustar divisor.");
			}
		});

		// 10. Hacer Visible la Ventana
		System.out.println("  [Constructor] Haciendo ventana visible...");
		setVisible(true);
		System.out.println("[VisorView Constructor] Finalizado.");

	} // --- FIN DEL CONSTRUCTOR ---

	
    /**
     * Crea y ensambla los principales componentes de la interfaz,
     * EXCEPTO la barra de menú y la barra de herramientas, que se reciben
     * pre-construidas.
     *
     * @param modeloNombres El DefaultListModel<String> que usarán las listas.
     */
	private void inicializarComponentes (DefaultListModel<String> modeloNombres) 
	{
	    // --- 0. INICIO MÉTODO ---
	    System.out.println("--- Iniciando inicializarComponentes ---");

	    // --- 1. CREAR PANEL PRINCIPAL ---
	    panelPrincipal = new JPanel(new BorderLayout());
	    panelPrincipal.setBackground(this.uiConfig.colorFondoPrincipal);
	    System.out.println("1. Panel Principal creado.");

	    // --- 2. CREAR Y AÑADIR TOOLBAR ---
	    if (this.mainToolbarPanel != null) {
            this.panelDeBotones = this.mainToolbarPanel; // panelDeBotones apunta al panel recibido
            panelPrincipal.add(this.mainToolbarPanel, BorderLayout.NORTH);
            System.out.println("2. Toolbar (pre-construida) añadida al NORTE.");
        } else {
             System.err.println("WARN [VisorView]: No se recibió JPanel de Toolbar pre-construido.");
             panelPrincipal.add(new JLabel("Toolbar no disponible"), BorderLayout.NORTH);
        }
	    
	    // --- 3. CREAR Y ESTABLECER MENÚ ---
        if (this.mainMenuBar != null) {
            setJMenuBar(this.mainMenuBar); // Establece la JMenuBar recibida
            System.out.println("3. Barra de Menú (pre-construida) establecida.");
        } else {
            System.err.println("WARN [VisorView]: No se recibió JMenuBar pre-construida.");
        }
	    

	    // --- 4. INICIALIZAR PANEL IZQUIERDO (CON LISTA DE NOMBRES) ---
	    //      Este método interno CREARÁ la instancia de `listaNombres`.
	    try {
	    	inicializarPanelIzquierdo(this.model, this.modeloLista);
	        System.out.println("4. Panel Izquierdo (con listaNombres) inicializado: " + (panelIzquierdo != null && listaNombres != null));
	    } catch (Exception e) {
	        System.err.println("ERROR inicializando Panel Izquierdo: " + e.getMessage());
	        e.printStackTrace();
	        panelIzquierdo = null;
	        listaNombres = null; // Asegurar que sea null si falla
	    }

	    // --- 5. INICIALIZAR PANEL DE MINIATURAS (CON LISTA DE MINIATURAS) ---
	    //      Este método interno CREARÁ la instancia de `listaMiniaturas`.
	    try {
	    	inicializarPanelImagenesMiniatura(this.model, this.servicioThumbs);
	        System.out.println("5. Panel Miniaturas (con listaMiniaturas) inicializado: " + (scrollListaMiniaturas != null && listaMiniaturas != null));
	    
	    } catch (Exception e) {
	        System.err.println("ERROR inicializando Panel Miniaturas: " + e.getMessage());
	        e.printStackTrace();
	        scrollListaMiniaturas = null;
	        listaMiniaturas = null; // Asegurar que sea null si falla
	    }

	    // --- 6. COMPARTIR LISTSELECTIONMODEL ---
	    System.out.println("6. ListSelectionModel NO compartido. Cada lista tiene el suyo.");
	    
	    // --- 7. INICIALIZAR ETIQUETA IMAGEN PRINCIPAL ---
	    //      Esto crea el JLabel `etiquetaImagen`.
	    try {
	        inicializarEtiquetaMostrarImagen();
	        System.out.println("7. Etiqueta Imagen inicializada: " + (etiquetaImagen != null));
	    } catch (Exception e) {
	        System.err.println("ERROR inicializando Etiqueta Imagen: " + e.getMessage());
	        e.printStackTrace();
	        etiquetaImagen = null;
	    }

	    // --- 8. CREAR Y AÑADIR SPLITPANE ---
	    splitPane = null; // Resetear por si acaso
	    
	    if (panelIzquierdo != null && etiquetaImagen != null) {
	        try {
	            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierdo, etiquetaImagen);
	            splitPane.setResizeWeight(0.25);
	            splitPane.setContinuousLayout(true);
	            splitPane.setBackground(this.uiConfig.colorFondoPrincipal);
	            splitPane.setOpaque(true);
	            panelPrincipal.add(splitPane, BorderLayout.CENTER);
	            System.out.println("8. SplitPane añadido al CENTRO.");
	            
	        } catch (Exception e) {
	            System.err.println("ERROR creando/añadiendo SplitPane: " + e.getMessage());
	            e.printStackTrace();
	            splitPane = null;
	            panelPrincipal.add(new JLabel("Error SplitPane"), BorderLayout.CENTER);
	        }
	        
	    } else {
	        System.err.println("ERROR: panelIzquierdo o etiquetaImagen nulos antes de crear SplitPane.");
	        panelPrincipal.add(new JLabel("Error Layout Izquierdo"), BorderLayout.WEST);
	        panelPrincipal.add(new JLabel("Error Layout Central"), BorderLayout.CENTER);
	    }

	    // --- 9. AÑADIR PANEL DE MINIATURAS (SCROLLPANE) AL SUR ---
	    //      Usa `scrollListaMiniaturas` creado en el paso 5.
	    if (scrollListaMiniaturas != null) {
	        panelPrincipal.add(scrollListaMiniaturas, BorderLayout.SOUTH);
	        System.out.println("9. ScrollPane Miniaturas añadido al SUR.");
	    } else {
	        // Error si el panel de miniaturas no se creó
	        System.err.println("ERROR: scrollListaMiniaturas nulo. No se pudo añadir al layout.");
	        panelPrincipal.add(new JLabel("Error Panel Miniaturas"), BorderLayout.SOUTH);
	    }

	    // --- 10. AÑADIR PANEL PRINCIPAL AL JFRAME ---
	    try {
	        add(panelPrincipal, BorderLayout.CENTER);
	        System.out.println("10. Panel Principal añadido al JFrame.");
	    } catch (Exception e) {
	        System.err.println("ERROR añadiendo panelPrincipal al JFrame: " + e.getMessage());
	        e.printStackTrace();
	    }

	    // --- 11. CREAR Y AÑADIR BARRA DE ESTADO (TEXTORUTA) ---
	    textoRuta = null; // Resetear
	    try {
	        textoRuta = new JTextField();
	        textoRuta.setEditable(false);
	        textoRuta.setBackground(this.uiConfig.colorFondoSecundario);
	        textoRuta.setForeground(this.uiConfig.colorTextoPrimario);
	        textoRuta.setCaretColor(this.uiConfig.colorTextoPrimario);
	        Border lineaExternaRuta = BorderFactory.createLineBorder(this.uiConfig.colorBorde);
	        Border paddingInternoRuta = BorderFactory.createEmptyBorder(2, 5, 2, 5);
	        textoRuta.setBorder(BorderFactory.createCompoundBorder(lineaExternaRuta, paddingInternoRuta));

	        add(textoRuta, BorderLayout.SOUTH);
	        System.out.println("11. Barra de Estado (textoRuta) añadida al JFrame.");
	    } catch (Exception e) {
	        System.err.println("ERROR creando/añadiendo textoRuta: " + e.getMessage());
	        e.printStackTrace();
	    }

	    // --- 12. FIN MÉTODO ---
	    System.out.println("--- Inicialización de Componentes Completada ---");

	} // Fin inicializarComponentes
	
	
    /**
     * Inicializa el panel izquierdo de la interfaz, que contiene la JList
     * utilizada para mostrar los nombres de los archivos de imagen.
     *
     * @param modeloApp El modelo principal de la aplicación (VisorModel), necesario
     *                  para obtener la referencia al ListModel compartido y,
     *                  opcionalmente, para pasar al CellRenderer si este lo requiere.
     * @param modeloNombres El DefaultListModel<String> que contiene las claves/nombres
     *                      de todos los archivos y que será usado por esta JList.
     *                      (En la arquitectura de modelo compartido, este es el mismo
     *                      que se pasa al constructor de VisorView desde VisorModel).
     */
    private void inicializarPanelIzquierdo(VisorModel modeloApp, DefaultListModel<String> modeloNombres) {
        // 1. Log inicio
        System.out.println("  [Init Comp] Inicializando Panel Izquierdo...");

        // 2. Crear la JList para los nombres de archivo (`listaNombres`)
        //    Se le pasa el modelo de datos principal recibido como parámetro.
        listaNombres = new JList<>(modeloNombres);
        listaNombres.setPrototypeCellValue("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");

        // 3. Configurar propiedades básicas de la JList
        listaNombres.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Permitir seleccionar solo un item
        listaNombres.setBackground(this.uiConfig.colorFondoSecundario);    // Color de fondo de la lista
        listaNombres.setForeground(this.uiConfig.colorTextoPrimario);      // Color del texto normal
        listaNombres.setSelectionBackground(this.uiConfig.colorSeleccionFondo); // Color de fondo del item seleccionado
        listaNombres.setSelectionForeground(this.uiConfig.colorSeleccionTexto); // Color del texto del item seleccionado
        //FIXME configurar Fuente?
        // Opcional: Configurar fuente si se desea
        // listaNombres.setFont(new Font("Arial", Font.PLAIN, 12));

        // 4. Establecer el Cell Renderer personalizado para los nombres
        //    Este renderer se encarga de cómo se dibuja cada celda (cada nombre).
        try {
             listaNombres.setCellRenderer(new NombreArchivoRenderer(this.uiConfig));
             System.out.println("    -> NombreArchivoRenderer asignado a listaNombres.");
        } catch (Exception e) {
             // Fallback si el renderer personalizado falla
             System.err.println("ERROR [inicializarPanelIzquierdo] estableciendo NombreArchivoRenderer: " + e.getMessage());
             e.printStackTrace();
             listaNombres.setCellRenderer(new DefaultListCellRenderer()); // Usar renderer por defecto
        }

        // 5. Crear el JScrollPane para la lista de nombres
        //    Esto permite el desplazamiento vertical si la lista es muy larga.
        JScrollPane scrollPaneListaNombres = new JScrollPane(listaNombres);

        // Configurar apariencia del ScrollPane
        scrollPaneListaNombres.getViewport().setBackground(this.uiConfig.colorFondoSecundario); // Color detrás de la lista
        scrollPaneListaNombres.setBorder(BorderFactory.createLineBorder(this.uiConfig.colorBorde)); // Borde del scrollpane

        // 6. Crear el JPanel que contendrá el ScrollPane (panelIzquierdo)
        panelIzquierdo = new JPanel(new BorderLayout()); // Usar BorderLayout para que el scrollpane ocupe todo
        panelIzquierdo.setBackground(this.uiConfig.colorFondoPrincipal); // Fondo del panel

        // 7. Crear y aplicar el Borde Titulado al panel izquierdo
        Border bordeLinea = BorderFactory.createLineBorder(this.uiConfig.colorBorde);
        TitledBorder bordeTitulado = BorderFactory.createTitledBorder(bordeLinea, "Lista de Archivos"); // Título inicial
        bordeTitulado.setTitleColor(this.uiConfig.colorTextoSecundario); // Color del título
        panelIzquierdo.setBorder(bordeTitulado);

        // 8. Añadir el ScrollPane (con la lista dentro) al panel izquierdo
        panelIzquierdo.add(scrollPaneListaNombres, BorderLayout.CENTER);

        // 9. Log final
        System.out.println("  [Init Comp] Panel Izquierdo finalizado.");

    } // --- FIN inicializarPanelIzquierdo ---
	
	
    /**
     * Inicializa el JScrollPane y la JList que se usarán para mostrar las miniaturas.
     * Configura la JList, establece el renderer, define el layout y tamaño de celdas.
     * Usa un panel intermediario con GridBagLayout para centrar la JList horizontalmente.
     * Calcula el tamaño preferido de la JList basado en la configuración del modelo.
     *
     * @param modeloParam El VisorModel principal, necesario para que el renderer obtenga rutas
     *               y para leer la configuración de cantidad de miniaturas.
     * @param servicioThumbsLocal El ThumbnailService, necesario para que el renderer cargue miniaturas.
     */
    private void inicializarPanelImagenesMiniatura(VisorModel modeloParam, ThumbnailService servicioThumbsParam) {
        // --- SECCIÓN 1: Log de Inicio (Sin cambios) ---
        System.out.println("  [Init Comp] Inicializando Panel Miniaturas...");

        // --- SECCIÓN 2: Creación de la JList de Miniaturas (Sin cambios) ---
        listaMiniaturas = new JList<>(new DefaultListModel<>()); // Modelo inicial vacío
        listaMiniaturas.setFocusable(true);
        System.out.println("    [Init Miniaturas] listaMiniaturas setFocusable(true)");

        // --- SECCIÓN 3: Configuración de Propiedades Básicas de la JList (Sin cambios) ---
        listaMiniaturas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaMiniaturas.setBackground(this.uiConfig.colorFondoSecundario); // Color DETRÁS de las celdas
        listaMiniaturas.setSelectionBackground(this.uiConfig.colorSeleccionFondo);
        listaMiniaturas.setSelectionForeground(this.uiConfig.colorSeleccionTexto);
        listaMiniaturas.setOpaque(false); // Para que se vea el fondo del panel wrapper/centrador

        // --- SECCIÓN 4: Configuración del Layout de la JList (Sin cambios) ---
        listaMiniaturas.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        listaMiniaturas.setVisibleRowCount(-1); // Importante para que sea una sola fila horizontal

        // --- SECCIÓN 5: Configuración del Renderer y Tamaño Fijo de Celda ---
        int cellWidth = 60;  // Fallback si todo falla
        int cellHeight = 60; // Fallback

        try {
            // 5.2.1. Obtener dimensiones del icono desde config
            int thumbWidth = MINIMUM_THUMBNAIL_CELL_DIMENSION;
            int thumbHeight = MINIMUM_THUMBNAIL_CELL_DIMENSION;
            if (this.uiConfig != null && this.uiConfig.configurationManager != null) {
                thumbWidth = this.uiConfig.configurationManager.getInt("miniaturas.tamano.normal.ancho", MINIMUM_THUMBNAIL_CELL_DIMENSION);
                thumbHeight = this.uiConfig.configurationManager.getInt("miniaturas.tamano.normal.alto", MINIMUM_THUMBNAIL_CELL_DIMENSION);
            }

            // 5.2.2. Saber si tenemos que mostrar nombres
            boolean mostrarNombresEnThumbs = true;
            if (this.uiConfig != null && this.uiConfig.configurationManager != null) {
                mostrarNombresEnThumbs = this.uiConfig.configurationManager.getBoolean("ui.miniaturas.mostrar_nombres", true);
            }

            // 5.2.3. Crear y asignar el renderer
            Color colorFondoDef = (this.uiConfig != null) ? this.uiConfig.colorFondoSecundario : Color.WHITE;
            Color colorFondoSelDef = (this.uiConfig != null) ? this.uiConfig.colorSeleccionFondo : new Color(57, 105, 138);
            Color colorTextoDef = (this.uiConfig != null) ? this.uiConfig.colorTextoPrimario : Color.BLACK;
            Color colorTextoSelDef = (this.uiConfig != null) ? this.uiConfig.colorSeleccionTexto : Color.WHITE;
            Color colorBordeSel = Color.ORANGE; // O desde uiConfig

            MiniaturaListCellRenderer rendererMiniaturas = new MiniaturaListCellRenderer(
                servicioThumbsParam, modeloParam,
                thumbWidth, thumbHeight, mostrarNombresEnThumbs,
                colorFondoDef, colorFondoSelDef, colorTextoDef, colorTextoSelDef, colorBordeSel
            );
            listaMiniaturas.setCellRenderer(rendererMiniaturas);

            // Obtener dimensiones de celda DESDE el renderer
            cellWidth = rendererMiniaturas.getAnchoCalculadaDeCelda();
            cellHeight = rendererMiniaturas.getAlturaCalculadaDeCelda();

            listaMiniaturas.setFixedCellWidth(cellWidth);
            listaMiniaturas.setFixedCellHeight(cellHeight);
            System.out.println("    [Init Miniaturas] Renderer asignado. Tamaño de celda FIJO: " + cellWidth + "x" + cellHeight);

        } catch (Exception e) {
            System.err.println("ERROR [inicializarMiniaturas] creando/asignando Renderer o calculando tamaño celda: " + e.getMessage());
            e.printStackTrace();
            listaMiniaturas.setCellRenderer(new DefaultListCellRenderer());
            listaMiniaturas.setFixedCellWidth(cellWidth); // Usa el fallback 60
            listaMiniaturas.setFixedCellHeight(cellHeight);// Usa el fallback 60
        }

        // --- SECCIÓN 5.4: Calcular Tamaño Preferido de la JList ---
        // Este tamaño preferido es para la JList en sí misma, basado en el MÁXIMO de ítems
        // que podría mostrar según la configuración, para que el JScrollPane tenga una idea
        // de cuán grande *podría* llegar a ser la lista.
        try {
            int maxNumAntes = (modeloParam != null) ? modeloParam.getMiniaturasAntes() : VisorController.DEFAULT_MINIATURAS_ANTES_FALLBACK;
            int maxNumDespues = (modeloParam != null) ? modeloParam.getMiniaturasDespues() : VisorController.DEFAULT_MINIATURAS_DESPUES_FALLBACK;
            int maxTotalMiniaturas = maxNumAntes + 1 + maxNumDespues;

            int preferredListWidth = maxTotalMiniaturas * cellWidth;
            int preferredListHeight = cellHeight; // Solo una fila de alto

            listaMiniaturas.setPreferredSize(new Dimension(preferredListWidth, preferredListHeight));
            System.out.println("    [Init Miniaturas] Tamaño Preferido JList (para max items) establecido a: " + preferredListWidth + "x" + preferredListHeight);
        } catch (Exception e) {
            System.err.println("ERROR [inicializarMiniaturas] calculando/estableciendo tamaño preferido JList: " + e.getMessage());
            e.printStackTrace();
            if (cellWidth <= 0) cellWidth = 60; if (cellHeight <= 0) cellHeight = 60; // Asegurar defaults
            listaMiniaturas.setPreferredSize(new Dimension(15 * cellWidth, cellHeight));
        }

        // --- SECCIÓN 6: Creación del Panel Wrapper para Centrado Interno de la JList ---
        // Este panel usará FlowLayout.CENTER para centrar la JList si esta es más estrecha que el wrapper.
        JPanel wrapperListaMiniaturas = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)); // Centrado, sin gaps h/v
        wrapperListaMiniaturas.setOpaque(false); // Hacerlo transparente para que se vea el fondo del panelCentrador
        wrapperListaMiniaturas.add(listaMiniaturas); // Añadir la JList a este wrapper
        System.out.println("    [Init Miniaturas] Panel wrapper para JList creado con FlowLayout.CENTER.");


        // --- SECCIÓN 7: Creación del Panel Intermediario (`panelCentrador`) ---
        // Este panel usa GridBagLayout para centrar el 'wrapperListaMiniaturas'.
        JPanel panelCentrador = new JPanel(new GridBagLayout());
        panelCentrador.setOpaque(true);
        panelCentrador.setBackground(this.uiConfig.colorFondoPrincipal); // El fondo que se ve detrás de las miniaturas

        // --- SECCIÓN 8: Configuración de GridBagConstraints para Centrar el WRAPPER ---
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0; // Permite que la celda del GBL (y por ende el wrapper) se expanda horizontalmente
        gbc.weighty = 1.0; // Permite que la celda del GBL (y por ende el wrapper) se expanda verticalmente
        gbc.anchor = GridBagConstraints.CENTER; // ANCLA el wrapper al centro de la celda del GBL
        gbc.fill = GridBagConstraints.HORIZONTAL;//.BOTH;   // HACE QUE EL WRAPPER LLENE la celda del GBL (horizontal y verticalmente)

        // --- SECCIÓN 9: Añadir el WRAPPER (que contiene la JList) al Panel Intermediario ---
        panelCentrador.add(wrapperListaMiniaturas, gbc); // Añadir el wrapper, no la JList directamente
        System.out.println("    [Init Miniaturas] Wrapper de JList añadido a panelCentrador (GridBagLayout).");

        // --- SECCIÓN 10: Creación del JScrollPane ---
        scrollListaMiniaturas = new JScrollPane(panelCentrador); // El scrollpane ahora ve el panelCentrador
        System.out.println("    [Init Miniaturas] JScrollPane creado con panelCentrador.");

        // --- SECCIÓN 11: Configuración de las Barras de Scroll del ScrollPane ---
        scrollListaMiniaturas.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // Como lo querías
        scrollListaMiniaturas.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        // --- SECCIÓN 12: Establecimiento del Tamaño Preferido del ScrollPane ---
        // Ya no establecemos un preferredSize muy pequeño y fijo para el scrollPane.
        // Dejamos que su tamaño preferido se derive de su contenido (panelCentrador -> wrapper -> listaMiniaturas)
        // y de la altura que calculamos para las celdas + TitledBorder.
        // Sin embargo, this.miniaturaScrollPaneHeight (calculado en AppInitializer) sigue siendo relevante
        // para el BorderLayout en el panelPrincipal.
        // Para asegurar que el JScrollPane tenga al menos la altura deseada:
        Dimension scrollPrefSize = new Dimension(100, this.miniaturaScrollPaneHeight); // Ancho mínimo, alto deseado
        scrollListaMiniaturas.setPreferredSize(scrollPrefSize);
        scrollListaMiniaturas.setMinimumSize(new Dimension(50, this.miniaturaScrollPaneHeight / 2)); // Un mínimo razonable

        // --- SECCIÓN 13: Configuración Visual del Viewport y Borde ---
        scrollListaMiniaturas.getViewport().setBackground(this.uiConfig.colorFondoPrincipal);
        Border lineaMini = BorderFactory.createLineBorder(this.uiConfig.colorBorde);
        TitledBorder bordeTituladoMini = BorderFactory.createTitledBorder(lineaMini, "Miniaturas");
        bordeTituladoMini.setTitleColor(this.uiConfig.colorTextoSecundario);
        scrollListaMiniaturas.setBorder(bordeTituladoMini);

        // --- SECCIÓN 14: Log Final ---
        System.out.println("  [Init Comp] Panel Miniaturas (Wrapper + GridBagLayout) finalizado. Altura scrollPaneHint: " + this.miniaturaScrollPaneHeight);

    } // --- FIN del metodo inicializarPanelImagenesMiniatura
    
    
	// --- Método para inicializar Etiqueta Imagen ---
	private void inicializarEtiquetaMostrarImagen ()
	{

		System.out.println("  [Init Comp] Inicializando Etiqueta Imagen...");
		etiquetaImagen = new JLabel()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent (Graphics g)
			{

//				System.out.println("%%% [paintComponent] llamado. imagenReescaladaView es null? " + (imagenReescaladaView == null));
				if (imagenReescaladaView != null) {
//		            System.out.println("%%% [paintComponent] Dibujando imagen. Zoom: " + zoomFactorView + " Offset: " + imageOffsetXView + "," + imageOffsetYView);
				}
				
		        int width = getWidth();
				int height = getHeight();

				// 1. Dibujar Fondo (cuadros o sólido)
				if (fondoACuadrosActivado)
				{
					Graphics2D g2dFondo = (Graphics2D) g.create();

					try
					{

						for (int row = 0; row < height; row += TAMANO_CUADRO)
						{

							for (int col = 0; col < width; col += TAMANO_CUADRO)
							{
								g2dFondo.setColor(
										(((row / TAMANO_CUADRO) % 2) == ((col / TAMANO_CUADRO) % 2)) ? colorCuadroClaro
												: colorCuadroOscuro);
								g2dFondo.fillRect(col, row, TAMANO_CUADRO, TAMANO_CUADRO);
							}
						}
					} finally
					{
						g2dFondo.dispose();
					}
				} else
				{
					g.setColor(getBackground());
					g.fillRect(0, 0, width, height);
				}

				// 2. Dibujar Imagen (si existe)
				if (imagenReescaladaView != null)
				{
					Graphics2D g2dImagen = (Graphics2D) g.create();

					try
					{
						g2dImagen.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
								RenderingHints.VALUE_INTERPOLATION_BILINEAR);
						g2dImagen.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
						g2dImagen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
						int baseW = imagenReescaladaView.getWidth(null);
						int baseH = imagenReescaladaView.getHeight(null);

						if (baseW > 0 && baseH > 0)
						{
							int finalW = (int) (baseW * zoomFactorView);
							int finalH = (int) (baseH * zoomFactorView);
							int drawX = (width - finalW) / 2 + imageOffsetXView;
							int drawY = (height - finalH) / 2 + imageOffsetYView;
							g2dImagen.drawImage(imagenReescaladaView, drawX, drawY, finalW, finalH, null);
						}
					} finally
					{
						g2dImagen.dispose();
					}
				}
				// 3. Dibujar Texto "Cargando..." (si no hay imagen y hay texto)
				else if (getText() != null && !getText().isEmpty())
				{
					g.setColor(getForeground());
					g.setFont(getFont());
					java.awt.FontMetrics fm = g.getFontMetrics();
					int textW = fm.stringWidth(getText());
					int textH = fm.getAscent();
					int textX = (width - textW) / 2;
					int textY = (height - fm.getHeight()) / 2 + textH;
					g.drawString(getText(), textX, textY);
				}

			} // Fin paintComponent
		}; // Fin JLabel anónimo

		etiquetaImagen.setHorizontalAlignment(SwingConstants.CENTER);
		etiquetaImagen.setVerticalAlignment(SwingConstants.CENTER);
		etiquetaImagen.setOpaque(false); // Importante: NO opaco para que el fondo pintado se vea
		etiquetaImagen.setBackground(this.uiConfig.colorFondoSecundario); // Color base si no es a cuadros
		etiquetaImagen.setForeground(this.uiConfig.colorTextoPrimario); // Color para texto "Cargando"
		System.out.println("  [Init Comp] Etiqueta Imagen finalizada.");

	}

	// --- Métodos para Actualizar la Vista ---

    /**
     * Asigna un nuevo modelo de datos a las dos JList (nombres y miniaturas).
     * Limpia la lista de miniaturas si se está estableciendo un nuevo modelo principal.
     * @param nuevoModelo El DefaultListModel que contiene las claves de imagen.
     */
     public void setListaImagenesModel(DefaultListModel<String> nuevoModelo) {
         // Guardar la referencia al nuevo modelo principal (sobrescribe la anterior)
         this.modeloLista = nuevoModelo; 
         
         String logMsg = "[VisorView setListaImagenesModel] ";
         int tamanoFinal = 0; // Para guardar el tamaño final

         // 1. Actualizar listaNombres
         if (this.listaNombres != null) {
             this.listaNombres.setModel(nuevoModelo); // Asignar nuevo modelo
             logMsg += " asignado a listaNombres.";
             this.listaNombres.repaint();
             tamanoFinal = nuevoModelo.getSize(); // Obtener tamaño aquí
         } else {
             logMsg += " ¡listaNombres es NULL! ";
         }

         // 2. Actualizar listaMiniaturas (ASIGNANDO EL MISMO MODELO)
         //    Ahora que la lógica de filtrado está en el Controller, ambas
         //    listas deben usar el MISMO modelo principal para que la selección funcione.
         if (this.listaMiniaturas != null) {
             this.listaMiniaturas.setModel(nuevoModelo); // Asignar el MISMO nuevo modelo
             logMsg += " asignado a listaMiniaturas.";
             this.listaMiniaturas.repaint();
             if (tamanoFinal == 0) tamanoFinal = nuevoModelo.getSize(); // Obtener tamaño si no se hizo antes
         } else {
             logMsg += " ¡listaMiniaturas es NULL! ";
         }

         // 3. Imprimir log con el tamaño final
         logMsg += " Tamaño modelo: " + tamanoFinal;
         System.out.println(logMsg);

     } // --- FIN setListaImagenesModel ---

	
	/**
     * Reemplaza el modelo de datos de la lista de miniaturas.
     * @param nuevoModeloMiniaturas El modelo que contiene solo las claves a mostrar.
     */
    public void setModeloListaMiniaturas(DefaultListModel<String> nuevoModeloMiniaturas) {
        if (this.listaMiniaturas != null && nuevoModeloMiniaturas != null) {
             System.out.println("[VisorView] Estableciendo nuevo modelo para listaMiniaturas (Tamaño: " + nuevoModeloMiniaturas.getSize() + ")");
             
             this.listaMiniaturas.setModel(nuevoModeloMiniaturas);
             // Repintar es importante, setModel no siempre lo hace visualmente inmediato
             this.listaMiniaturas.repaint();
        } else {
             System.err.println("WARN [setModeloListaMiniaturas]: listaMiniaturas o nuevo modelo es null.");
        }
    } // --- FIN setModeloListaMiniaturas ---
    

    /**
     * Actualiza la etiqueta principal para mostrar la imagen proporcionada,
     * aplicando el factor de zoom y los desplazamientos (paneo) especificados.
     * También limpia cualquier texto de "Cargando..." que pudiera haber.
     *
     * @param imagenReescalada La instancia de `java.awt.Image` ya reescalada
     *                         (calculada por `VisorController.reescalarImagenParaAjustar`)
     *                         que se va a dibujar. Puede ser `null` si hubo un error o se quiere limpiar.
     * @param zoom El factor de zoom actual a aplicar al dibujar (1.0 = 100%).
     * @param offsetX El desplazamiento horizontal (paneo) en píxeles.
     * @param offsetY El desplazamiento vertical (paneo) en píxeles.
     */
    public void setImagenMostrada(Image imagenReescalada, double zoom, int offsetX, int offsetY) {
        // 1. Validar que la etiqueta donde se muestra la imagen exista
        if (this.etiquetaImagen == null) {
            System.err.println("ERROR [setImagenMostrada]: etiquetaImagen es null. No se puede mostrar la imagen.");
            return;
        }

        // 2. Limpiar cualquier texto previo (ej. "Cargando...")
        //    Es importante hacerlo ANTES de asignar la nueva imagen interna
        //    y llamar a repaint(), para que el texto desaparezca correctamente.
        this.etiquetaImagen.setText(null);
        this.etiquetaImagen.setIcon(null); // Asegurarse de que no hay icono

        // 3. Actualizar las variables de instancia que usa paintComponent
        this.imagenReescaladaView = imagenReescalada; // Guardar referencia a la imagen a dibujar
        this.zoomFactorView = zoom;                   // Guardar zoom actual
        this.imageOffsetXView = offsetX;              // Guardar offset X actual
        this.imageOffsetYView = offsetY;              // Guardar offset Y actual

        // 4. Solicitar repintado de la etiqueta
        //    Esto llamará al método paintComponent de la etiqueta (sobrescrito)
        //    que usará las variables imagenReescaladaView, zoomFactorView, etc.,
        //    para dibujar la imagen correctamente escalada y posicionada.
        //    Se asume que este método se llama desde el EDT.
        // System.out.println(">>> [VisorView.setImagenMostrada] Llamado con imagen: " + (imagenReescalada != null ? "VÁLIDA" : "NULL") + ", Zoom: " + zoom + ", Offset: " + offsetX + "," + offsetY); // Log de depuración
        this.etiquetaImagen.repaint();

    } // --- FIN setImagenMostrada ---


    /**
     * Limpia la etiqueta principal de visualización de imagen, eliminando
     * cualquier imagen, icono o texto que estuviera mostrando y reseteando
     * las variables internas de zoom y paneo a sus valores por defecto (1.0, 0, 0).
     * Llama a repaint() para que el cambio sea visible (normalmente mostrará el fondo).
     */
    public void limpiarImagenMostrada() {
        // 1. Validar que la etiqueta exista
        if (this.etiquetaImagen == null) {
            System.err.println("ERROR [limpiarImagenMostrada]: etiquetaImagen es null. No se puede limpiar.");
            return;
        }
        System.out.println("[VisorView] Limpiando etiqueta de imagen principal...");

        // 2. Limpiar contenido visual de JLabel
        this.etiquetaImagen.setText(null); // Eliminar cualquier texto (ej. "Cargando...")
        this.etiquetaImagen.setIcon(null);   // Eliminar cualquier icono

        // 3. Limpiar la referencia a la imagen interna usada por paintComponent
        this.imagenReescaladaView = null;

        // 4. Resetear variables internas de zoom y paneo a valores por defecto
        this.zoomFactorView = 1.0;
        this.imageOffsetXView = 0;
        this.imageOffsetYView = 0;
        // Nota: No necesariamente resetea el estado en el *Modelo*, solo en la *Vista*.
        //       El Controller debería llamar a model.resetZoomState() si es necesario.

        // 5. Solicitar repintado
        //    Esto ejecutará paintComponent, que al encontrar imagenReescaladaView = null
        //    y texto/icono = null, simplemente pintará el fondo (sólido o a cuadros).
        this.etiquetaImagen.repaint();

        System.out.println("  -> Etiqueta de imagen limpiada.");

    } // --- FIN limpiarImagenMostrada ---


    /**
     * Establece el texto que se muestra en la barra de estado inferior
     * (el componente JTextField llamado 'textoRuta').
     * Se utiliza típicamente para mostrar la ruta completa del archivo
     * seleccionado o mensajes de error relacionados con la carga.
     *
     * @param texto La cadena de texto a mostrar en la barra de estado.
     *              Si es null, se mostrará una cadena vacía.
     */
    public void setTextoRuta(String texto) {
        // 1. Validar que el componente JTextField exista
        if (this.textoRuta == null) {
            System.err.println("ERROR [setTextoRuta]: textoRuta (JTextField) es null. No se puede establecer texto.");
            return;
        }

        // 2. Asignar el texto al JTextField
        //    Si el texto recibido es null, asigna una cadena vacía para evitar NullPointerException.
        String textoAMostrar = (texto != null) ? texto : "";
        this.textoRuta.setText(textoAMostrar);

        // 3. Opcional: Mover el cursor al inicio (si fuera editable, pero no lo es)
        // this.textoRuta.setCaretPosition(0);

        // 4. Log (opcional)
        // System.out.println("[VisorView] Texto de ruta actualizado a: '" + textoAMostrar + "'");

        // Nota: No suele ser necesario llamar a repaint() para un JTextField,
        //       ya que setText() normalmente se encarga de actualizar la vista del componente.

    } // --- FIN setTextoRuta ---


    /**
     * Actualiza el título mostrado en el borde titulado del panel izquierdo
     * (el panel que contiene la lista de nombres de archivo).
     * Útil para mostrar información como "Lista de Archivos" o "Archivos: 165".
     * Recrea el TitledBorder usando los colores actuales del tema definidos
     * en uiConfig para mantener la consistencia visual.
     *
     * @param titulo El nuevo texto a mostrar como título del borde.
     *               Si es null, se usará una cadena vacía.
     */
    public void setTituloPanelIzquierdo(String titulo) {
        // 1. Validar que el panel izquierdo y la configuración UI existan
        if (this.panelIzquierdo == null) {
            System.err.println("ERROR [setTituloPanelIzquierdo]: panelIzquierdo es null. No se puede establecer título.");
            return;
        }
        if (this.uiConfig == null) {
            System.err.println("WARN [setTituloPanelIzquierdo]: uiConfig es null. Se usará borde por defecto.");
            // Crear un borde simple como fallback si no hay config de colores
            this.panelIzquierdo.setBorder(BorderFactory.createTitledBorder(titulo != null ? titulo : ""));
            return;
        }

        // 2. Determinar el texto del título (evitar null)
        String textoTitulo = (titulo != null) ? titulo : "";
        // System.out.println("[VisorView] Estableciendo título del panel izquierdo a: '" + textoTitulo + "'"); // Log opcional

        // 3. Crear un nuevo TitledBorder con el título y colores del tema
        // 3.1. Crear el borde de línea base usando el color de borde del tema
        Border bordeLinea = BorderFactory.createLineBorder(this.uiConfig.colorBorde);
        // 3.2. Crear el TitledBorder usando el borde de línea y el nuevo texto
        TitledBorder bordeTitulado = BorderFactory.createTitledBorder(bordeLinea, textoTitulo);
        // 3.3. Establecer el color del texto del título usando el color secundario del tema
        bordeTitulado.setTitleColor(this.uiConfig.colorTextoSecundario);

        // 4. Asignar el nuevo borde al panel izquierdo
        this.panelIzquierdo.setBorder(bordeTitulado);

        // 5. Solicitar repintado del panel para que el nuevo borde sea visible
        //    Normalmente setBorder se encarga de esto, pero un repaint no hace daño.
        this.panelIzquierdo.repaint();

    } // --- FIN setTituloPanelIzquierdo ---


    /**
     * Muestra u oculta la barra de menú principal (JMenuBar) de la ventana (JFrame).
     * Obtiene la JMenuBar actual del JFrame y llama a setVisible() sobre ella.
     * Si no hay una JMenuBar establecida actualmente en el JFrame, no hace nada.
     *
     * @param visible true para mostrar la barra de menú, false para ocultarla.
     */
    public void setJMenuBarVisible(boolean visible) {
        // 1. Obtener la referencia a la JMenuBar actualmente establecida en este JFrame
        JMenuBar barraMenuActual = getJMenuBar(); // Método heredado de JFrame

        // 2. Verificar si existe una barra de menú
        if (barraMenuActual != null) {
            // 3. Comprobar si el estado de visibilidad actual es diferente al solicitado
            if (barraMenuActual.isVisible() != visible) {
                // 4. Establecer la nueva visibilidad
                System.out.println("[VisorView] Cambiando visibilidad de JMenuBar a: " + visible);
                barraMenuActual.setVisible(visible);

                // 5. Revalidar el JFrame (importante al añadir/quitar componentes como la barra de menú)
                //    Esto indica al layout manager que recalcule el espacio.
                //    Es más seguro hacerlo en invokeLater.
                SwingUtilities.invokeLater(() -> {
                    if (this != null) { // 'this' se refiere al JFrame (VisorView)
                         // System.out.println("  -> Revalidando JFrame por cambio en JMenuBar..."); // Log opcional
                         this.revalidate();
                         // Un repaint también puede ser útil para asegurar la actualización visual
                         this.repaint();
                    }
                });
            } else {
                 // System.out.println("[VisorView] Visibilidad de JMenuBar ya es " + visible + ". No se cambia."); // Log opcional
            }
        } else {
            // Informar si no hay barra de menú que mostrar/ocultar
            System.err.println("WARN [setJMenuBarVisible]: No hay JMenuBar establecida en este JFrame.");
        }
    } // --- FIN setJMenuBarVisible ---

    
    /**
     * Muestra u oculta la barra de botones (Toolbar) de la aplicación.
     * Asume que la barra de botones es el componente 'panelDeBotones'
     * y que está añadido al layout principal (probablemente en BorderLayout.NORTH
     * del 'panelPrincipal').
     *
     * @param visible true para mostrar la barra de botones, false para ocultarla.
     */
    public void setToolBarVisible(boolean visible) {
        // 1. Verificar que la referencia al panel de la barra de botones exista
        if (this.panelDeBotones != null) {
            // 2. Comprobar si el estado de visibilidad actual es diferente al solicitado
            if (this.panelDeBotones.isVisible() != visible) {
                // 3. Establecer la nueva visibilidad del panel
                System.out.println("[VisorView] Cambiando visibilidad de ToolBar (panelDeBotones) a: " + visible);
                this.panelDeBotones.setVisible(visible);

                // 4. Revalidar el contenedor padre del panel de botones
                //    Normalmente es 'panelPrincipal'. Esto es crucial para que el
                //    BorderLayout ajuste el espacio de los otros componentes (CENTER, SOUTH).
                //    Hacerlo en invokeLater es más seguro.
                SwingUtilities.invokeLater(() -> {
                    if (panelPrincipal != null) {
                         // System.out.println("  -> Revalidando panelPrincipal por cambio en ToolBar..."); // Log opcional
                         panelPrincipal.revalidate();
                         panelPrincipal.repaint(); // Repintar también por si acaso
                    } else {
                         System.err.println("WARN [setToolBarVisible]: panelPrincipal es null al revalidar.");
                    }
                });
            } else {
                 // System.out.println("[VisorView] Visibilidad de ToolBar ya es " + visible + ". No se cambia."); // Log opcional
            }
        } else {
            // Informar si la referencia al panel de botones no existe
            System.err.println("WARN [setToolBarVisible]: panelDeBotones es null. No se puede cambiar visibilidad.");
        }
    } // --- FIN setToolBarVisible ---

    
    /**
     * Muestra u oculta el panel izquierdo, que contiene la lista de nombres de archivos (JList).
     * Asume que este panel ('panelIzquierdo') es uno de los componentes dentro del
     * JSplitPane principal ('splitPane'). Ocultar/mostrar este panel afectará
     * la distribución del espacio en el JSplitPane.
     *
     * @param visible true para mostrar el panel de la lista de archivos, false para ocultarlo.
     */
    public void setFileListVisible(boolean visible) {
        // 1. Verificar que el panel izquierdo exista
        if (this.panelIzquierdo != null) {
            // 2. Comprobar si el estado de visibilidad actual es diferente al solicitado
            if (this.panelIzquierdo.isVisible() != visible) {
                // 3. Establecer la nueva visibilidad del panel izquierdo
                System.out.println("[VisorView] Cambiando visibilidad de FileList (panelIzquierdo) a: " + visible);
                this.panelIzquierdo.setVisible(visible);

                // 4. Reajustar el divisor del JSplitPane
                //    Cuando un componente del JSplitPane se oculta/muestra, es
                //    necesario reajustar el divisor para que el espacio se redistribuya
                //    correctamente. resetToPreferredSizes() intenta hacerlo basado
                //    en los tamaños preferidos de los componentes visibles.
                if (this.splitPane != null) {
                     System.out.println("  -> Reajustando divisor del JSplitPane...");
                     // Hacerlo en invokeLater puede ser más seguro para asegurar
                     // que el cambio de visibilidad se haya procesado.
                     SwingUtilities.invokeLater(() -> {
                          if (splitPane != null) { // Doble chequeo
                               splitPane.resetToPreferredSizes();
                               // Opcionalmente, podrías querer guardar/restaurar
                               // la posición del divisor manualmente si resetToPreferredSizes
                               // no da el resultado deseado.
                               // Ejemplo:
                               // if (!visible) {
                               //     lastDividerLocation = splitPane.getDividerLocation();
                               // } else if (lastDividerLocation > 0) {
                               //     splitPane.setDividerLocation(lastDividerLocation);
                               // } else {
                               //     splitPane.setDividerLocation(0.25); // Valor por defecto
                               // }
                          }
                     });
                } else {
                     System.err.println("WARN [setFileListVisible]: splitPane es null, no se pudo reajustar divisor.");
                }

                // 5. Revalidar/Repintar el contenedor padre (opcional, pero no suele hacer daño)
                //    El reajuste del splitPane normalmente se encarga de esto.
                // if (panelPrincipal != null) {
                //      SwingUtilities.invokeLater(() -> {
                //          panelPrincipal.revalidate();
                //          panelPrincipal.repaint();
                //      });
                // }

            } else {
                 // System.out.println("[VisorView] Visibilidad de FileList ya es " + visible + ". No se cambia."); // Log opcional
            }
        } else {
            // Informar si la referencia al panel izquierdo no existe
            System.err.println("WARN [setFileListVisible]: panelIzquierdo es null. No se puede cambiar visibilidad.");
        }
    } // --- FIN setFileListVisible ---


    /**
     * Muestra u oculta el panel (JScrollPane) que contiene la lista de miniaturas.
     * Asume que este componente es 'scrollListaMiniaturas' y que está añadido
     * directamente al layout principal (probablemente en BorderLayout.SOUTH
     * del 'panelPrincipal').
     *
     * @param visible true para mostrar el panel de miniaturas, false para ocultarlo.
     */
    public void setThumbnailsVisible(boolean visible) {
        // 1. Verificar que la referencia al JScrollPane de las miniaturas exista
        if (this.scrollListaMiniaturas != null) {
            // 2. Comprobar si el estado de visibilidad actual es diferente al solicitado
            if (this.scrollListaMiniaturas.isVisible() != visible) {
                // 3. Establecer la nueva visibilidad del JScrollPane
                System.out.println("[VisorView] Cambiando visibilidad de Thumbnails (scrollListaMiniaturas) a: " + visible);
                this.scrollListaMiniaturas.setVisible(visible);

                // 4. Revalidar y Repintar el CONTENEDOR PADRE del scrollPane
                //    Esto es crucial para que el layout manager (ej. BorderLayout
                //    en panelPrincipal) ajuste el espacio correctamente cuando el
                //    componente SOUTH se oculta o se muestra.
                //    Usar invokeLater para asegurar que se haga después de procesar
                //    el cambio de visibilidad en el hilo de eventos.
                SwingUtilities.invokeLater(() -> {
                     // Obtener el padre directo del scrollPane
                     Component padre = scrollListaMiniaturas.getParent();
                     if (padre instanceof JPanel) { // Comprobar si es un JPanel (como panelPrincipal)
                         // System.out.println("  -> Revalidando contenedor padre: " + padre.getClass().getSimpleName()); // Log opcional
                         ((JPanel) padre).revalidate();
                         ((JPanel) padre).repaint();
                     } else if (padre != null) {
                          // Si el padre no es un JPanel, intentar revalidar/repintar de todos modos
                          // System.out.println("  -> Revalidando contenedor padre (no JPanel): " + padre.getClass().getSimpleName()); // Log opcional
                          padre.revalidate();
                          padre.repaint();
                     } else {
                          // Si no tiene padre (raro si está en la UI), loguear warning
                          System.err.println("WARN [setThumbnailsVisible]: scrollListaMiniaturas no tiene padre al revalidar.");
                          // Podríamos intentar revalidar el panelPrincipal como fallback
                           if (panelPrincipal != null) {
                                panelPrincipal.revalidate();
                                panelPrincipal.repaint();
                           }
                     }
                });
            } else {
                 // System.out.println("[VisorView] Visibilidad de Thumbnails ya es " + visible + ". No se cambia."); // Log opcional
            }
        } else {
            // Informar si la referencia al scrollPane de miniaturas no existe
            System.err.println("WARN [setThumbnailsVisible]: scrollListaMiniaturas es null. No se puede cambiar visibilidad.");
        }
    } // --- FIN setThumbnailsVisible ---
    

    /**
     * Muestra u oculta la barra de estado/ubicación (el JTextField 'textoRuta')
     * que normalmente se encuentra en la parte inferior de la ventana principal.
     * Asume que 'textoRuta' está añadido directamente al layout del JFrame
     * (probablemente en BorderLayout.SOUTH).
     *
     * @param visible true para mostrar la barra de estado, false para ocultarla.
     */
    public void setLocationBarVisible(boolean visible) {
        // 1. Verificar que la referencia al JTextField exista
        if (this.textoRuta != null) {
            // 2. Comprobar si el estado de visibilidad actual es diferente al solicitado
            if (this.textoRuta.isVisible() != visible) {
                // 3. Establecer la nueva visibilidad del JTextField
                System.out.println("[VisorView] Cambiando visibilidad de LocationBar (textoRuta) a: " + visible);
                this.textoRuta.setVisible(visible);

                // 4. Revalidar y Repintar el CONTENEDOR PADRE del JTextField
                //    En este caso, el padre es el ContentPane del JFrame.
                //    Es necesario para que el BorderLayout del JFrame ajuste el espacio.
                //    Usar invokeLater es más seguro.
                SwingUtilities.invokeLater(() -> {
                    // Obtener el ContentPane (el contenedor principal del JFrame)
                    Container contentPane = getContentPane();
                    if (contentPane != null) {
                         // System.out.println("  -> Revalidando ContentPane por cambio en LocationBar..."); // Log opcional
                         contentPane.revalidate();
                         contentPane.repaint();
                         // Revalidar el propio JFrame también puede ayudar en algunos L&F
                         this.revalidate(); // 'this' es el JFrame
                         this.repaint();
                    } else {
                         System.err.println("WARN [setLocationBarVisible]: ContentPane es null al revalidar.");
                    }
                });
            } else {
                 // System.out.println("[VisorView] Visibilidad de LocationBar ya es " + visible + ". No se cambia."); // Log opcional
            }
        } else {
            // Informar si la referencia al JTextField no existe
            System.err.println("WARN [setLocationBarVisible]: textoRuta es null. No se puede cambiar visibilidad.");
        }
    } // --- FIN setLocationBarVisible ---
    

	/**
	 * Activa o desactiva el pintado del fondo a cuadros detrás de la imagen
	 * principal.
	 * 
	 * @param activado true para mostrar cuadros, false para fondo sólido.
	 */
	public void setCheckeredBackgroundEnabled (boolean activado)
	{

		System.out.println(
				"[VisorView setCheckeredBG] Solicitado: " + activado + ", Actual: " + this.fondoACuadrosActivado);

		if (this.fondoACuadrosActivado != activado)
		{
			this.fondoACuadrosActivado = activado;

			if (etiquetaImagen != null)
			{
				etiquetaImagen.repaint(); // Repintar para aplicar cambio
				System.out.println("  -> Fondo a cuadros " + (activado ? "ACTIVADO" : "DESACTIVADO") + ". Repintando.");
			}
		}

	}

	// --- Dentro de la clase VisorView.java ---

    /**
     * Actualiza el estado visual de los controles relacionados con el zoom manual
     * y el reseteo del zoom, basándose en si el modo de zoom manual está activo
     * y si la acción de reseteo debe estar habilitada.
     *
     * Específicamente:
     * - Cambia el color de fondo del botón de activación/desactivación de zoom manual
     *   (asociado a 'toggleZoomManualAction') para indicar visualmente si está "pulsado".
     * - Habilita o deshabilita el botón y el item de menú para resetear el zoom
     *   (asociados a 'resetZoomAction').
     *
     * @param zoomManualActivado true si el modo de zoom manual está actualmente activo.
     *                           Determina el color de fondo del botón de zoom.
     * @param resetHabilitado true si los controles de reseteo (botón y menú) deben estar habilitados.
     *                        Normalmente será igual a 'zoomManualActivado', pero se pasa
     *                        explícitamente por flexibilidad.
     */
    public void actualizarEstadoControlesZoom(boolean zoomManualActivado, boolean resetHabilitado) {
        // 1. Log inicial (opcional)
        // System.out.println("[VisorView] Actualizando estado controles zoom -> Manual Activo: " + zoomManualActivado + ", Reset Habilitado: " + resetHabilitado);

        // 2. Validar dependencias necesarias (mapas de componentes y config UI)
        if (botonesPorNombre == null || menuItemsPorNombre == null || uiConfig == null) {
             System.err.println("WARN [actualizarEstadoControlesZoom]: Mapas de botones/menús o uiConfig son nulos. No se puede actualizar estado.");
             return;
        }

        // 3. Actualizar Botón de Activación/Desactivación de Zoom Manual
        //    Identificar el botón por su clave larga de configuración.
        String zoomButtonKey = "interfaz.boton.zoom.Zoom_48x48";
        JButton zoomButton = this.botonesPorNombre.get(zoomButtonKey);
        if (zoomButton != null) {
            // Obtener los colores del tema desde uiConfig
            Color colorActivo = this.uiConfig.colorBotonActivado;
            Color colorNormal = this.uiConfig.colorBotonFondo;
            // Establecer el color de fondo según el estado 'zoomManualActivado'
            zoomButton.setBackground(zoomManualActivado ? colorActivo : colorNormal);
            // Asegurar que sea opaco para que se vea el color
            zoomButton.setOpaque(true);
            // System.out.println("  -> Fondo botón Zoom ('"+zoomButtonKey+"') actualizado a: " + (zoomManualActivado ? "Activo" : "Normal")); // Log opcional
        } else {
             System.err.println("WARN [actualizarEstadoControlesZoom]: Botón de Zoom no encontrado con clave: " + zoomButtonKey);
        }

        // 4. Actualizar Estado 'Enabled' del Botón de Reset Zoom
        String resetButtonKey = "interfaz.boton.zoom.Reset_48x48";
        JButton resetButton = this.botonesPorNombre.get(resetButtonKey);
        if (resetButton != null) {
            // Habilitar o deshabilitar el botón según 'resetHabilitado'
            if (resetButton.isEnabled() != resetHabilitado) { // Cambiar solo si es necesario
                 resetButton.setEnabled(resetHabilitado);
                 // System.out.println("  -> Botón Reset ('"+resetButtonKey+"') setEnabled(" + resetHabilitado + ")"); // Log opcional
            }
        } else {
            System.err.println("WARN [actualizarEstadoControlesZoom]: Botón de Reset no encontrado con clave: " + resetButtonKey);
        }

        // 5. Actualizar Estado 'Enabled' del Menú Item de Reset Zoom
        String resetMenuKey = "interfaz.menu.zoom.resetear_zoom";
        JMenuItem resetMenuItem = this.menuItemsPorNombre.get(resetMenuKey);
        
        if (resetMenuItem != null) {
            // Habilitar o deshabilitar el item de menú según 'resetHabilitado'
            if (resetMenuItem.isEnabled() != resetHabilitado) { // Cambiar solo si es necesario
                 resetMenuItem.setEnabled(resetHabilitado);
                 // System.out.println("  -> Menú Reset ('"+resetMenuKey+"') setEnabled(" + resetHabilitado + ")"); // Log opcional
            }
        } else {
             System.err.println("WARN [actualizarEstadoControlesZoom]: Menú Item de Reset no encontrado con clave: " + resetMenuKey);
        }

        // 6. Log final (opcional)
        // System.out.println("[VisorView] Actualización de estado de controles de zoom completada.");

    } // --- FIN actualizarEstadoControlesZoom ---
    
    
    /**
     * Asegura que el estado visual 'seleccionado' del JCheckBoxMenuItem
     * correspondiente a "Activar Zoom Manual" en el menú coincida con el
     * estado lógico proporcionado.
     *
     * Este método se usa principalmente para sincronizar la UI del menú si el estado
     * de zoom manual cambia por otras vías (p.ej., al limpiar UI), aunque si se usa
     * setAction() correctamente con la ToggleZoomManualAction, esta sincronización
     * debería ser automática. Sin embargo, llamarlo explícitamente puede añadir robustez.
     *
     * @param seleccionado El estado deseado para el checkbox del menú
     *                     (true si debe aparecer marcado, false si desmarcado).
     */
    public void setEstadoMenuActivarZoomManual(boolean seleccionado) {
        // 1. Obtener la clave de configuración larga del item de menú
        String zoomMenuKey = "interfaz.menu.zoom.Activar_Zoom_Manual";

        // 2. Validar que el mapa de items de menú exista
        if (menuItemsPorNombre == null) {
             System.err.println("WARN [setEstadoMenuActivarZoomManual]: El mapa de menuItems es null.");
             return;
        }

        // 3. Obtener el JMenuItem del mapa usando la clave
        JMenuItem zoomMenuItem = this.menuItemsPorNombre.get(zoomMenuKey);

        // 4. Verificar si el item existe y es del tipo correcto (JCheckBoxMenuItem)
        if (zoomMenuItem instanceof JCheckBoxMenuItem) {
            // 4.1. Convertir a JCheckBoxMenuItem
            JCheckBoxMenuItem cbItem = (JCheckBoxMenuItem) zoomMenuItem;

            // 4.2. Comprobar si el estado visual actual es diferente al deseado
            if (cbItem.isSelected() != seleccionado) {
                // 4.3. Establecer el nuevo estado visual (seleccionado/deseleccionado)
                 System.out.println("  [View] Sincronizando Checkbox Activar Zoom Manual a: " + seleccionado);
                cbItem.setSelected(seleccionado);
            }
            // Si el estado ya es el correcto, no hacemos nada para evitar eventos innecesarios.

        } else if (zoomMenuItem != null) {
             // El item existe pero no es un JCheckBoxMenuItem
             System.err.println("WARN [setEstadoMenuActivarZoomManual]: Item de menú encontrado con clave '" + zoomMenuKey + "' pero NO es un JCheckBoxMenuItem (es " + zoomMenuItem.getClass().getSimpleName() + ").");
        } else {
             // El item no se encontró en el mapa
             System.err.println("WARN [setEstadoMenuActivarZoomManual]: Item de menú Activar Zoom Manual no encontrado con clave: '" + zoomMenuKey + "'.");
        }
    } // --- FIN setEstadoMenuActivarZoomManual ---
    

    /**
     * Aplica una breve animación de cambio de color de fondo a un botón específico
     * para dar feedback visual al usuario de que la acción asociada se ha ejecutado.
     * El botón cambia temporalmente al color de animación definido en uiConfig
     * y luego vuelve a su color de fondo normal.
     *
     * Busca el botón en el mapa 'botonesPorNombre' usando una comparación del
     * final de la clave larga con el nombre base proporcionado.
     *
     * Excluye de la animación al botón de Zoom ('Zoom_48x48') porque este
     * ya cambia su fondo de forma persistente para indicar su estado activo/inactivo.
     *
     * @param actionCommandDelBoton El nombre base del botón (generalmente coincide con
     *                        el ActionCommand corto o la parte final de la clave de config,
     *                        ej. "Siguiente_48x48", "Reset_48x48").
     */
     public void aplicarAnimacionBoton(String actionCommandDelBoton) {
         // 1. Log inicial (opcional)
         System.out.println("[VisorView] Solicitud de animación para botón base: " + actionCommandDelBoton);

         // 2. Validaciones iniciales
         if (actionCommandDelBoton == null || actionCommandDelBoton.trim().isEmpty()) {
              System.err.println("WARN [aplicarAnimacionBoton]: nombreBotonBase es nulo o vacío.");
              return;
         }
         if (botonesPorNombre == null || botonesPorNombre.isEmpty()) {
              System.err.println("WARN [aplicarAnimacionBoton]: Mapa 'botonesPorNombre' vacío o null.");
              return;
         }
          if (uiConfig == null) {
               System.err.println("WARN [aplicarAnimacionBoton]: uiConfig es null. No se pueden obtener colores.");
               return;
          }

         // 3. Excluir el botón de Zoom manual de la animación
          if (AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE.equals(actionCommandDelBoton)) {
              System.out.println("  -> Animación omitida para el botón de Zoom Manual (tiene su propio manejo de color de fondo).");
              return;
          } 
         
//         if ("Zoom_48x48".equals(actionCommandDelBoton)) {
//              return;
//         }

         // 4. Buscar el botón correspondiente en el mapa
         JButton botonParaAnimar = null;
         String claveDelBotonIdentificado = null; // Guardar la clave para logs
         
         // System.out.println("  -> Buscando botón cuya clave larga termine en '." + nombreBotonBase + "'..."); // Log opcional
         
         for (Map.Entry<String, JButton> entry : botonesPorNombre.entrySet()) {
             JButton botonActual = entry.getValue();
             if (botonActual != null) {
                 Action accionDelBoton = botonActual.getAction();
                 if (accionDelBoton != null) {
                     Object cmdKeyDeLaAction = accionDelBoton.getValue(Action.ACTION_COMMAND_KEY);
                     if (actionCommandDelBoton.equals(cmdKeyDeLaAction)) {
                         botonParaAnimar = botonActual;
                         claveDelBotonIdentificado = entry.getKey();
                         System.out.println("    -> Botón para animar encontrado por ActionCommand. Clave: " + claveDelBotonIdentificado);
                         break; // Encontramos el botón
                     }
                 }
             }
         }
         
         // 5. Aplicar animación si se encontró el botón
         if (botonParaAnimar != null) {
        	 final String claveFinalDelBotonAnimado = claveDelBotonIdentificado;
             System.out.println("    -> Aplicando animación al botón: " + claveFinalDelBotonAnimado);

             // 5.1. Obtener colores del tema
             final Color colorOriginal = this.uiConfig.colorBotonFondo; // Color normal
             final Color colorAnimacion = this.uiConfig.colorBotonAnimacion; // Color para el "flash"
             final JButton botonFinalParaTimer = botonParaAnimar;
             
             // 5.2. Cambiar al color de animación inmediatamente
             if (!Objects.equals(botonFinalParaTimer.getBackground(), colorAnimacion)) {
                 botonFinalParaTimer.setBackground(colorAnimacion);
                 botonFinalParaTimer.setOpaque(true); // Asegurar opacidad
                 // botonFinalParaTimer.repaint(); // Opcional, el Timer lo hará al final
             }
             
             // 5.3. Crear y empezar un Timer para restaurar el color original
             //      Usamos una variable final para el botón dentro de la lambda del Timer.
//             final JButton botonFinalParaTimer = botonParaAnimar;
             
             Timer timer = new Timer(BUTTON_CLICK_ANIMATION_DURATION_MS, (ActionEvent evt) -> {
                 // Este código se ejecuta en el EDT después del delay
                 if (botonFinalParaTimer != null) {
//                     boolean esToggleActivoConColorPropio = false;
                     Action accionDelBoton = botonFinalParaTimer.getAction();
                     Color colorRestauracion;
                     
                     if (accionDelBoton != null && Boolean.TRUE.equals(accionDelBoton.getValue(Action.SELECTED_KEY))) {
                         if (uiConfig.colorBotonActivado != null) { 
                        	 colorRestauracion = uiConfig.colorBotonActivado;
//                              botonFinalParaTimer.setBackground(uiConfig.colorBotonActivado);
//                              esToggleActivoConColorPropio = true; // Marcamos que ya se gestionó
                         } else {
                        	 colorRestauracion = colorOriginal;
                         }
                     } else {
                    	 colorRestauracion = colorOriginal;
                     }

//                     if (!esToggleActivoConColorPropio && !Objects.equals(botonFinalParaTimer.getBackground(), colorOriginal)) {
                     if (!Objects.equals(botonFinalParaTimer.getBackground(), colorRestauracion)) {
                         botonFinalParaTimer.setBackground(colorRestauracion);
                     }
                     // botonFinalParaTimer.repaint(); // Opcional
                     System.out.println("      -> Animación finalizada. Color del botón '" + claveFinalDelBotonAnimado + 
                    		 "' ahora es: " + botonFinalParaTimer.getBackground());
                 }
             });
             timer.setRepeats(false);
             timer.start();

         } else {
        	 System.err.println("WARN [VisorView aplicarAnimacionBoton]: No se encontró botón con ActionCommand: '" + 
        			 actionCommandDelBoton + "'");         }
    } // --- FIN aplicarAnimacionBoton ---

	// --- Métodos para que el Controller Añada Listeners ---
	public void addListaNombresSelectionListener (javax.swing.event.ListSelectionListener listener)
	{ // Renombrado

		if (listaNombres != null)
			listaNombres.addListSelectionListener(listener);
		else
			System.err.println("WARN addListaNombresSelectionListener: listaNombres es null.");

	}

	// Listener para la OTRA lista (miniaturas) - por si acaso, aunque la selección
	// es compartida
	public void addListaMiniaturasSelectionListener (javax.swing.event.ListSelectionListener listener)
	{

		if (listaMiniaturas != null)
			listaMiniaturas.addListSelectionListener(listener);
		else
			System.err.println("WARN addListaMiniaturasSelectionListener: listaMiniaturas es null.");

	}


    /**
     * Añade un listener para detectar eventos de la rueda del ratón
     * específicamente sobre el componente JLabel que muestra la imagen principal.
     *
     * Permite al VisorController implementar lógica personalizada cuando el usuario
     * gira la rueda del ratón mientras el cursor está sobre la imagen
     * (típicamente para hacer zoom).
     *
     * @param listener La instancia de MouseWheelListener (normalmente una lambda
     *                 o una instancia de clase interna/anónima definida en el Controller)
     *                 que manejará los eventos de la rueda del ratón.
     */
    public void addEtiquetaImagenMouseWheelListener(java.awt.event.MouseWheelListener listener) {
        // 1. Validar que la etiqueta de imagen exista
        if (this.etiquetaImagen != null) {
            // 2. Validar que el listener proporcionado no sea null
            if (listener != null) {
                // 3. Añadir el listener al componente etiquetaImagen
                //    Este componente ya es el JLabel personalizado con paintComponent sobrescrito.
                this.etiquetaImagen.addMouseWheelListener(listener);
                // System.out.println("[VisorView] MouseWheelListener añadido a etiquetaImagen."); // Log opcional
            } else {
                System.err.println("WARN [addEtiquetaImagenMouseWheelListener]: El listener proporcionado es null.");
            }
        } else {
            // Informar si la etiqueta aún no ha sido inicializada
            System.err.println("ERROR [addEtiquetaImagenMouseWheelListener]: etiquetaImagen es null. No se puede añadir listener.");
        }
    } // --- FIN addEtiquetaImagenMouseWheelListener ---
    

    /**
     * Añade un listener para detectar eventos básicos del ratón (clics,
     * presionar/soltar botón, entrar/salir del componente) específicamente
     * sobre el componente JLabel que muestra la imagen principal.
     *
     * Permite al VisorController implementar lógica personalizada para estos
     * eventos, como por ejemplo, iniciar el paneo (drag) cuando se presiona
     * el botón del ratón sobre la imagen.
     *
     * @param listener La instancia de MouseListener (normalmente una instancia
     *                 de java.awt.event.MouseAdapter definida en el Controller)
     *                 que manejará los eventos básicos del ratón.
     */
    public void addEtiquetaImagenMouseListener(java.awt.event.MouseListener listener) {
        // 1. Validar que la etiqueta de imagen exista
        if (this.etiquetaImagen != null) {
            // 2. Validar que el listener proporcionado no sea null
            if (listener != null) {
                // 3. Añadir el listener al componente etiquetaImagen
                this.etiquetaImagen.addMouseListener(listener);
                // System.out.println("[VisorView] MouseListener añadido a etiquetaImagen."); // Log opcional
            } else {
                System.err.println("WARN [addEtiquetaImagenMouseListener]: El listener proporcionado es null.");
            }
        } else {
            // Informar si la etiqueta aún no ha sido inicializada
            System.err.println("ERROR [addEtiquetaImagenMouseListener]: etiquetaImagen es null. No se puede añadir listener.");
        }
    } // --- FIN addEtiquetaImagenMouseListener ---
    

    /**
     * Añade un listener para detectar eventos de movimiento del ratón
     * (arrastrar con botón presionado, mover el cursor) específicamente
     * sobre el componente JLabel que muestra la imagen principal.
     *
     * Permite al VisorController implementar lógica personalizada para estos
     * eventos, como por ejemplo, realizar el paneo (drag) de la imagen
     * cuando el usuario arrastra el ratón con el botón presionado.
     *
     * @param listener La instancia de MouseMotionListener (normalmente una instancia
     *                 de java.awt.event.MouseMotionAdapter definida en el Controller)
     *                 que manejará los eventos de movimiento del ratón.
     */
    public void addEtiquetaImagenMouseMotionListener(java.awt.event.MouseMotionListener listener) {
        // 1. Validar que la etiqueta de imagen exista
        if (this.etiquetaImagen != null) {
            // 2. Validar que el listener proporcionado no sea null
            if (listener != null) {
                // 3. Añadir el listener al componente etiquetaImagen
                this.etiquetaImagen.addMouseMotionListener(listener);
                // System.out.println("[VisorView] MouseMotionListener añadido a etiquetaImagen."); // Log opcional
            } else {
                System.err.println("WARN [addEtiquetaImagenMouseMotionListener]: El listener proporcionado es null.");
            }
        } else {
            // Informar si la etiqueta aún no ha sido inicializada
            System.err.println("ERROR [addEtiquetaImagenMouseMotionListener]: etiquetaImagen es null. No se puede añadir listener.");
        }
    } // --- FIN addEtiquetaImagenMouseMotionListener ---

    
    /**
     * Añade un ActionListener específico a un JButton de la barra de herramientas,
     * identificando el botón mediante su clave de configuración larga.
     *
     * Este método se utiliza principalmente para conectar botones que NO usan
     * el sistema de Actions de Swing directamente (casos raros o botones con
     * lógica muy específica que no encaja en una Action reutilizable) a un
     * manejador de eventos (normalmente el ActionListener central del VisorController).
     *
     * Para botones que SÍ tienen una Action asociada, es preferible usar
     * `component.setAction(action)` en el VisorController (método configurarComponentActions),
     * ya que `setAction` maneja automáticamente el ActionListener, texto, icono, estado enabled, etc.
     *
     * @param configKey La clave de configuración larga del JButton al que se
     *                  añadirá el listener (ej. "interfaz.boton.especiales.MiBotonEspecial").
     *                  Esta clave debe coincidir con una entrada en el mapa 'botonesPorNombre'.
     * @param listener  La instancia de ActionListener que manejará los eventos de acción
     *                  del botón (normalmente `this` si se llama desde el VisorController).
     */
    public void addButtonActionListener(String configKey, ActionListener listener) {
        // 1. Validar que el mapa de botones y el listener no sean nulos
        if (botonesPorNombre == null) {
            System.err.println("ERROR [addButtonActionListener]: El mapa 'botonesPorNombre' es null.");
            return;
        }
        if (listener == null) {
            System.err.println("WARN [addButtonActionListener]: El listener proporcionado es null para la clave: " + configKey);
            // Podríamos decidir no hacer nada o quitar listeners existentes si listener es null
            return;
        }
        if (configKey == null || configKey.trim().isEmpty()) {
             System.err.println("WARN [addButtonActionListener]: La clave (configKey) es null o vacía.");
             return;
        }


        // 2. Buscar el botón en el mapa usando la clave larga
        JButton button = botonesPorNombre.get(configKey);

        // 3. Si se encuentra el botón, añadir el listener
        if (button != null) {
            // 3.1. Opcional: Limpiar listeners previos del mismo tipo si se quiere evitar duplicados
            //      Esto es útil si este método pudiera llamarse varias veces para el mismo botón.
            // for (ActionListener al : button.getActionListeners()) {
            //     if (al == listener) { // O if (al.getClass() == listener.getClass())
            //          System.out.println("  -> Quitando listener previo para botón: " + configKey);
            //          button.removeActionListener(al);
            //     }
            // }

            // 3.2. Añadir el nuevo listener
            button.addActionListener(listener);
            // System.out.println("[VisorView] ActionListener añadido al botón con clave: " + configKey); // Log opcional
        } else {
            // 4. Informar si no se encontró el botón con esa clave
            System.err.println("WARN [addButtonActionListener]: No se encontró botón en el mapa con clave larga: '" + configKey + "'. No se añadió listener.");
        }
    } // --- FIN addButtonActionListener ---
    

 // --- Dentro de la clase VisorView.java ---

    /**
     * Añade un ActionListener específico a un JMenuItem (que no sea un JMenu)
     * de la barra de menú, identificando el item mediante su clave de configuración larga.
     *
     * Este método se usa principalmente para conectar items de menú que NO usan
     * el sistema de Actions de Swing directamente (ej. items con lógica muy
     * específica, radios/checkboxes manejados manualmente, o items "fallback" como "Versión")
     * a un manejador de eventos (normalmente el ActionListener central del VisorController).
     *
     * Para items de menú que SÍ tienen una Action asociada, es preferible usar
     * `component.setAction(action)` en el VisorController (método configurarComponentActions).
     *
     * @param configKey La clave de configuración larga del JMenuItem al que se
     *                  añadirá el listener (ej. "interfaz.menu.archivo.Guardar_Configuracion").
     *                  Debe coincidir con una entrada en el mapa 'menuItemsPorNombre'.
     * @param listener  La instancia de ActionListener que manejará los eventos de acción
     *                  del item (normalmente `this` si se llama desde VisorController).
     */
     public void addMenuItemActionListener(String configKey, ActionListener listener) {
         // 1. Validar que el mapa de items de menú y el listener no sean nulos
         if (menuItemsPorNombre == null) {
             System.err.println("ERROR [addMenuItemActionListener]: El mapa 'menuItemsPorNombre' es null.");
             return;
         }
         if (listener == null) {
             System.err.println("WARN [addMenuItemActionListener]: El listener proporcionado es null para la clave: " + configKey);
             return;
         }
          if (configKey == null || configKey.trim().isEmpty()) {
              System.err.println("WARN [addMenuItemActionListener]: La clave (configKey) es null o vacía.");
              return;
         }

         // 2. Buscar el item de menú en el mapa usando la clave larga
         JMenuItem menuItem = menuItemsPorNombre.get(configKey);

         // 3. Si se encuentra el item y NO es un JMenu (contenedor)
         if (menuItem != null) {
             if (!(menuItem instanceof JMenu)) { // Asegurarse de que no es un menú contenedor

                 // 3.1. Opcional: Limpiar listeners previos del mismo tipo
                 // for (ActionListener al : menuItem.getActionListeners()) {
                 //     if (al == listener) { menuItem.removeActionListener(al); }
                 // }

                 // 3.2. Añadir el nuevo listener
                 menuItem.addActionListener(listener);
                 // System.out.println("[VisorView] ActionListener añadido al menú item con clave: " + configKey); // Log opcional
             } else {
                  // Informar si se intentó añadir a un JMenu (que no dispara ActionEvent así)
                  System.out.println("INFO [addMenuItemActionListener]: Se intentó añadir listener a un JMenu (contenedor): '" + configKey + "'. Los JMenu no disparan ActionEvents de esta forma.");
             }
         } else {
             // 4. Informar si no se encontró el item con esa clave
             System.err.println("WARN [addMenuItemActionListener]: No se encontró menú item en el mapa con clave larga: '" + configKey + "'. No se añadió listener.");
         }
     } // --- FIN addMenuItemActionListener ---
     

	// --- Getters para Componentes Clave ---
	public JList<String> getListaNombres ()
	{

		return listaNombres;

	}

	public JList<String> getListaMiniaturas ()
	{

		return listaMiniaturas;

	}

	public JScrollPane getScrollListaMiniaturas ()
	{

		return scrollListaMiniaturas;

	}

	public JLabel getEtiquetaImagen ()
	{

		return etiquetaImagen;

	}

	public JFrame getFrame ()
	{

		return this;

	}

	public Map<String, JButton> getBotonesPorNombre ()
	{

		return botonesPorNombre;

	}

	public Map<String, JMenuItem> getMenuItemsPorNombre ()
	{

		return menuItemsPorNombre;

	}

	// Añadir getter para ContentPane si fuera necesario para bindings
	public Component getContentPaneForBindings ()
	{

		return getContentPane();

	}

	public JPanel getPanelIzquierdo() {
        return panelIzquierdo;
    }

	
    /**
     * Devuelve la referencia al JPanel que contiene la barra de botones (Toolbar).
     * Necesario para que el Controller pueda controlar su visibilidad.
     *
     * @return El JPanel de la barra de botones, o null si no se ha inicializado.
     */
    public JPanel getPanelDeBotones() {
        return panelDeBotones;
    }

    /**
     * Devuelve la referencia al JTextField que actúa como barra de estado/ruta.
     * Necesario para que el Controller pueda controlar su visibilidad y actualizar su texto.
     *
     * @return El JTextField de la barra de estado, o null si no se ha inicializado.
     */
    public JTextField getTextoRuta() {
        return textoRuta;
    }
    
    
 // --- Dentro de la clase VisorView.java ---

    /**
     * Configura la etiqueta principal de visualización de imagen para mostrar
     * un mensaje de "Cargando..." o similar, indicando al usuario que la
     * imagen solicitada se está procesando en segundo plano.
     *
     * Limpia cualquier imagen o icono previamente mostrado en la etiqueta.
     *
     * @param mensaje El texto específico a mostrar (ej. "Cargando: imagen.jpg...").
     *                Si es null o vacío, se usará un mensaje genérico "Cargando...".
     */
    public void mostrarIndicadorCargaImagenPrincipal(String mensaje) {
        // 1. Validar que la etiqueta de imagen exista
        if (this.etiquetaImagen == null) {
            System.err.println("ERROR [mostrarIndicadorCarga]: etiquetaImagen es null. No se puede mostrar indicador.");
            return;
        }

        // 2. Determinar el mensaje a mostrar
        String mensajeAMostrar = (mensaje != null && !mensaje.trim().isEmpty()) ? mensaje : "Cargando...";
         System.out.println("[VisorView] Mostrando indicador de carga: '" + mensajeAMostrar + "'");

        // 3. Limpiar contenido previo de la etiqueta
        //    Es importante quitar la imagen anterior para que se vea el texto.
        this.imagenReescaladaView = null; // Limpiar la referencia a la imagen
        this.etiquetaImagen.setIcon(null);   // Quitar cualquier icono que pudiera tener

        // 4. Establecer el texto de carga
        this.etiquetaImagen.setText(mensajeAMostrar);

        // 5. Configurar apariencia del texto (opcional pero recomendado)
        //    Asegurar que el texto sea visible con el color primario del tema.
        if(this.uiConfig != null) { // Usar colores de la configuración si está disponible
             this.etiquetaImagen.setForeground(this.uiConfig.colorTextoPrimario);
        } else {
             this.etiquetaImagen.setForeground(Color.BLACK); // Fallback a negro
        }
        // Centrar el texto
        this.etiquetaImagen.setHorizontalAlignment(SwingConstants.CENTER);
        this.etiquetaImagen.setVerticalAlignment(SwingConstants.CENTER);

        // 6. Forzar repintado de la etiqueta
        //    Esto asegura que los cambios (quitar imagen, poner texto) se reflejen
        //    inmediatamente en la interfaz gráfica. Se debe hacer en el EDT,
        //    pero como este método es llamado desde el Controller (que a su vez
        //    fue llamado por un evento o está en invokeLater), normalmente ya
        //    estamos en el EDT. Si hubiera dudas, se podría envolver en invokeLater.
        this.etiquetaImagen.repaint();

    } // --- FIN mostrarIndicadorCargaImagenPrincipal ---

    
    public void setActualJMenuBar(JMenuBar menuBar) { // Nombre diferente al setJMenuBar() de JFrame
        if (menuBar != null) {
            this.setJMenuBar(menuBar); // Llama al método de JFrame
            // this.mainMenuBar = menuBar; // Si tenías un campo para guardarlo
            System.out.println("  [VisorView] JMenuBar real asignada.");
        }
    }

    public void setActualToolbarPanel(JPanel toolbarPanel) {
        if (toolbarPanel != null) {
            if (this.panelDeBotones != null && this.panelPrincipal != null) { // Si el panel antiguo existe
                this.panelPrincipal.remove(this.panelDeBotones); // Quitar el temporal
            }
            this.panelDeBotones = toolbarPanel; // Guardar referencia al nuevo
            if (this.panelPrincipal != null) {
                this.panelPrincipal.add(this.panelDeBotones, BorderLayout.NORTH); // Añadir el nuevo
                this.panelPrincipal.revalidate();
                this.panelPrincipal.repaint();
            }
            System.out.println("  [VisorView] ToolbarPanel real asignado.");
        }
    }

    // Métodos para actualizar los mapas si es necesario
    public void setActualMenuItemsMap(Map<String, JMenuItem> menuItems) {
        this.menuItemsPorNombre = (menuItems != null) ? menuItems : new HashMap<>();
         System.out.println("  [VisorView] Map<String, JMenuItem> real asignado. Tamaño: " + this.menuItemsPorNombre.size());
    }

    public void setActualBotonesMap(Map<String, JButton> botones) {
        this.botonesPorNombre = (botones != null) ? botones : new HashMap<>();
         System.out.println("  [VisorView] Map<String, JButton> real asignado. Tamaño: " + this.botonesPorNombre.size());
    }


	public boolean isFondoACuadrosActivado ()
	{

		// TODO Auto-generated method stub
		return false;

	}    
	
	// Dentro de la clase VisorView

	/**
	 * Establece la configuración de la UI para esta vista.
	 * Este método es llamado por AppInitializer después de que uiConfig
	 * ha sido completamente construido (incluyendo el actionMap final).
	 * La vista puede usar este uiConfig para acceder a colores, iconos, actions, etc.
	 * si los necesita directamente después de la construcción inicial.
	 *
	 * @param uiConfig El objeto ViewUIConfig con la configuración de la UI.
	 */
	public void setUiConfig(ViewUIConfig uiConfig) {
	    this.uiConfig = Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser nulo en VisorView");
	    System.out.println("  [VisorView] ViewUIConfig asignado/actualizado.");

	    // Opcional: Si la vista necesita reaccionar inmediatamente a este nuevo uiConfig
	    // (por ejemplo, si algunos componentes ya creados necesitan colores del tema
	    // que solo están disponibles a través de uiConfig y no se pasaron en el constructor),
	    // podrías llamar a un método de actualización aquí.
	    // Ejemplo:
	    // aplicarColoresDesdeUIConfig();
	    // repintarComponentesNecesarios();
	    // Por ahora, es probable que el constructor de VisorView ya use los colores iniciales
	    // y que el uiConfig sea más para referencia o para que los builders lo usen.
	}

	
	/**
	 * Recrea y reasigna el CellRenderer para la lista de miniaturas.
	 * Este método lee la configuración actual para determinar si se deben mostrar
	 * los nombres de archivo y otros parámetros visuales, y luego actualiza
	 * la JList de miniaturas para reflejar estos cambios.
	 * Es típicamente llamado cuando una preferencia que afecta el renderizado
	 * de las miniaturas ha cambiado (ej. mostrar/ocultar nombres).
	 */
	public void solicitarRefrescoRenderersMiniaturas() {
	    System.out.println("[VisorView] Solicitando refresco completo de renderers de miniaturas...");

	    // 1. Validar dependencias críticas para recrear el renderer.
	    //    Si alguna falta, no se puede proceder y se emite un error.
	    if (this.listaMiniaturas == null) {
	        System.err.println("ERROR [solicitarRefrescoRenderersMiniaturas]: this.listaMiniaturas es null.");
	        return;
	    }
	    if (this.model == null) {
	        System.err.println("ERROR [solicitarRefrescoRenderersMiniaturas]: this.model (VisorModel) es null.");
	        return;
	    }
	    if (this.servicioThumbs == null) {
	        System.err.println("ERROR [solicitarRefrescoRenderersMiniaturas]: this.servicioThumbs (ThumbnailService) es null.");
	        return;
	    }
	    if (this.uiConfig == null || this.uiConfig.configurationManager == null) {
	        System.err.println("ERROR [solicitarRefrescoRenderersMiniaturas]: this.uiConfig o su .configurationManager son null.");
	        return;
	    }

	    // 2. Obtener el estado actual de "mostrar nombres" DESDE LA CONFIGURACIÓN.
	    //    Se asume que la Action que disparó este refresco ya actualizó el valor en ConfigurationManager.
	    boolean mostrarNombresActual = this.uiConfig.configurationManager.getBoolean(
	        "miniaturas.ui.mostrar_nombres", // La clave de configuración exacta
	        true // Valor por defecto si la clave no existe
	    );
	    System.out.println("  -> Estado actual de 'mostrarNombresEnMiniaturas' (leído de config): " + mostrarNombresActual);

	    // 3. Obtener las dimensiones de la IMAGEN dentro de la miniatura y los colores del tema.
	    //    Estos valores también se leen de la configuración a través de uiConfig.
	    int thumbWidth = this.uiConfig.configurationManager.getInt("miniaturas.tamano.normal.ancho", 40);
	    int thumbHeight = this.uiConfig.configurationManager.getInt("miniaturas.tamano.normal.alto", 40);
	    
	    Color colorFondoMiniatura = this.uiConfig.colorFondoSecundario; // O un color específico para miniaturas si lo tienes en uiConfig
	    Color colorFondoSeleccionMiniatura = this.uiConfig.colorSeleccionFondo;
	    Color colorTextoMiniatura = this.uiConfig.colorTextoPrimario;
	    Color colorTextoSeleccionMiniatura = this.uiConfig.colorSeleccionTexto;
	    Color colorBordeSeleccionMiniatura = this.uiConfig.colorBordeSeleccionActiva; // Asumiendo que tienes esta clave en uiConfig o usa un color fijo

	    // 4. Crear una NUEVA instancia del MiniaturaListCellRenderer con la configuración actualizada.
	    System.out.println("  -> Creando nuevo MiniaturaListCellRenderer con mostrarNombres=" + mostrarNombresActual);
	    MiniaturaListCellRenderer newRenderer = new MiniaturaListCellRenderer(
	        this.servicioThumbs,
	        this.model, // El VisorModel completo, el renderer puede necesitar acceso a más que solo la lista
	        thumbWidth,
	        thumbHeight,
	        mostrarNombresActual, // El estado crucial que puede haber cambiado
	        colorFondoMiniatura,
	        colorFondoSeleccionMiniatura,
	        colorTextoMiniatura,
	        colorTextoSeleccionMiniatura,
	        colorBordeSeleccionMiniatura
	    );

	    // 5. Asignar el nuevo renderer a la JList de miniaturas.
	    this.listaMiniaturas.setCellRenderer(newRenderer);
	    System.out.println("  -> Nuevo renderer asignado a listaMiniaturas.");

	    // 6. MUY IMPORTANTE: Actualizar la altura y el ancho fijos de las celdas en la JList
	    //    para que coincidan con las dimensiones que el nuevo renderer espera ocupar.
	    //    El renderer debería tener métodos para exponer estas dimensiones calculadas.
	    int nuevaAlturaCelda = newRenderer.getAlturaCalculadaDeCelda();
	    int nuevoAnchoCelda = newRenderer.getAnchoCalculadaDeCelda();

	    // Solo actualizar si realmente cambiaron para evitar ciclos innecesarios
	    if (this.listaMiniaturas.getFixedCellHeight() != nuevaAlturaCelda) {
	        this.listaMiniaturas.setFixedCellHeight(nuevaAlturaCelda);
	        System.out.println("  -> Altura de celda de miniaturas actualizada a: " + nuevaAlturaCelda);
	    }
	    if (this.listaMiniaturas.getFixedCellWidth() != nuevoAnchoCelda) {
	        this.listaMiniaturas.setFixedCellWidth(nuevoAnchoCelda);
	        System.out.println("  -> Ancho de celda de miniaturas actualizado a: " + nuevoAnchoCelda);
	    }
	    
	    // 7. Forzar a la JList a revalidar su layout y repintarse completamente.
	    //    revalidate() es importante si el tamaño de las celdas cambió,
	    //    lo que afectará el layout del JList y su contenedor (el JScrollPane).
	    System.out.println("  -> Revalidando y repintando listaMiniaturas...");
	    this.listaMiniaturas.revalidate(); // Importante si el tamaño de celda cambió
	    this.listaMiniaturas.repaint();    // Asegura que se redibujen todas las celdas visibles

	    // 8. Opcional: Si el cambio en el tamaño de las celdas puede afectar cuántas
	    //    miniaturas caben en el viewport del JScrollPane, y por lo tanto,
	    //    necesita que el "sliding window" de miniaturas (manejado por VisorController.actualizarModeloYVistaMiniaturas)
	    //    se recalcule, entonces se necesitaría una forma de notificar ese cambio.
	    //    Por ahora, asumimos que el controller llamará a actualizarModeloYVistaMiniaturas
	    //    si es necesario después de que esta acción de toggle se complete, o que
	    //    el listener de redimensionamiento de ventana se encargará.
	    //    Si esta llamada causa problemas de bucle, se podría necesitar una lógica más fina.
	    //    if (this.getController() instanceof VisorController && ((VisorController)this.getController()).getListCoordinator() != null) {
	    //        VisorController vc = (VisorController)this.getController();
	    //        int indiceActual = vc.getListCoordinator().getIndiceOficialSeleccionado();
	    //        if (indiceActual != -1) {
	    //            System.out.println("  -> Solicitando actualización del modelo de vista de miniaturas por cambio de renderer.");
	    //            vc.actualizarModeloYVistaMiniaturas(indiceActual);
	    //        }
	    //    }

	    System.out.println("[VisorView] Refresco de renderers de miniaturas completado.");
	}
	
	
	/**
     * Actualiza la apariencia visual (típicamente el color de fondo) de un botón
     * de la toolbar que actúa como un 'toggle' para reflejar su estado lógico.
     *
     * @param action La instancia de la Action de tipo toggle cuyo estado se quiere reflejar.
     *               Se usa para encontrar el botón asociado en la vista.
     * @param isSelected El estado lógico que debe reflejar el botón
     *                   (true si debe parecer 'activo' o 'pulsado', false si 'normal').
     */
	public void actualizarAspectoBotonToggle(Action action, boolean isSelected) {
	    // System.out.println("[VisorView actualizarAspectoBotonToggle] Buscando botón para ActionParam@" + Integer.toHexString(System.identityHashCode(action)));
	    
	    if (action == null) {
	        System.err.println("ERROR [VisorView actualizarAspectoBotonToggle]: La Action proporcionada es null.");
	        return;
	    }
	    if (this.uiConfig == null) {
	        System.err.println("ERROR [VisorView actualizarAspectoBotonToggle]: uiConfig es null. No se pueden obtener colores.");
	        return;
	    }
	    if (this.botonesPorNombre == null || this.botonesPorNombre.isEmpty()) {
	        System.err.println("ERROR [VisorView actualizarAspectoBotonToggle]: Mapa 'botonesPorNombre' vacío o null.");
	        return;
	    }
	    
	    Object actionCommandValue = action.getValue(Action.ACTION_COMMAND_KEY);
	    String actionCommandParaLog = (actionCommandValue instanceof String) ? (String) actionCommandValue : "DESCONOCIDO";

	    System.out.println("[VisorView actualizarAspectoBotonToggle] Para Action: " + actionCommandParaLog + 
	                       ", isSelected: " + isSelected);

	    JButton botonAsociado = null;
	    String claveDelBotonEncontrado = null;

	    for (Map.Entry<String, JButton> entry : this.botonesPorNombre.entrySet()) {
	        JButton botonActual = entry.getValue();
	        if (botonActual != null && action.equals(botonActual.getAction())) {
	            botonAsociado = botonActual;
	            claveDelBotonEncontrado = entry.getKey();
	            // System.out.println("  -> Botón encontrado por Action: " + claveDelBotonEncontrado); // Log reducido
	            break;
	        }
	    }

	    if (botonAsociado != null) {
	        Color colorFondoDestino;
	        if (isSelected) {
	            colorFondoDestino = this.uiConfig.colorBotonActivado; 
	        } else {
	            colorFondoDestino = this.uiConfig.colorBotonFondo;    
	        }

	        // Aplicar el color SIEMPRE y asegurar opacidad, similar a actualizarEstadoControlesZoom
	        botonAsociado.setBackground(colorFondoDestino);
	        botonAsociado.setOpaque(true); 
	        botonAsociado.setSelected(isSelected);
	        // botonAsociado.repaint(); // Normalmente no es necesario, setBackground lo dispara

	        System.out.println("  -> Aspecto botón '" + claveDelBotonEncontrado + "' FORZADO. Color fondo: " + colorFondoDestino);
	    } else {
	        System.err.println("WARN [VisorView actualizarAspectoBotonToggle]: No se encontró botón en la toolbar asociado a la Action con comando: " + actionCommandParaLog);
	    }
	} // --- FIN del metodo actualizarAspectoBotonToggle
	
	public Image getImagenReescaladaView() { // Getter público
	    return this.imagenReescaladaView;
	}

} // Fin VisorView