package vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
// Imports para el paintComponent de etiquetaImagen (si está aquí)
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border; // Para crearSeparadorVerticalBarraInfo y otros bordes
import javax.swing.border.TitledBorder; // Para panelIzquierdo

import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.image.ThumbnailService;
import vista.renderers.MiniaturaListCellRenderer; // Asumiendo que lo usas en inicializarPanelImagenesMiniatura
import vista.theme.Tema;
import vista.theme.ThemeManager;
import vista.util.IconUtils;


public class VisorView extends JFrame {

    private static final long serialVersionUID = 4L; // Incrementado por refactorización mayor

    // --- CAMPOS DE INSTANCIA PARA COMPONENTES UI PRINCIPALES ---
    // Paneles Contenedores Principales
//    private JPanel panelModoVisualizadorActual; 
//    private JPanel panelInfoSuperior;
//    private JPanel bottomStatusBar; 
//    private JLabel formatoImagenInfoLabel; // Para mostrar el formato de la imagen 

    // Componentes del Modo Normal/Detalle
//    private JSplitPane splitPane;
//    private JPanel panelContenedorIzquierdoSplit;
//    private JPanel panelContenedorDerechoSplit;
//    private JPanel panelIzquierdo; 
//    private JList<String> listaNombres;
//    private JLabel etiquetaImagen;
    private JScrollPane scrollListaMiniaturas;
//    private JList<String> listaMiniaturas;

    // Componentes Comunes (Barras, etc.)
    private JPanel panelDeBotones;        
//    private JPanel contenedorDeBarras; // El nuevo panel que contendrá las JToolBar
    private Map<String, JToolBar> barrasDeHerramientas;

    // Componentes de las Barras de Información
//    private JLabel nombreArchivoInfoLabel;
//    private JLabel indiceTotalInfoLabel;
//    private JLabel dimensionesOriginalesInfoLabel;
//    private JLabel modoZoomNombreInfoLabel;
//    private JLabel tamanoArchivoInfoLabel; 
//    private JLabel fechaArchivoInfoLabel;   
//    private JLabel porcentajeZoomVisualRealInfoLabel;
//    private JLabel indicadorZoomManualInfoLabel;
//    private JLabel indicadorMantenerPropInfoLabel;
//    private JLabel indicadorSubcarpetasInfoLabel;
//    private JLabel porcentajeZoomPersonalizadoLabel;
    
    // Componentes de la Barra de Estado Inferior (bottomStatusBar)
//    private JLabel rutaCompletaArchivoLabel; 
//    private JLabel mensajesAppLabel;
//    private JTextField textoRuta; // Usado como fallback temporalmente en crearPanelEstadoInferior
//	private JLabel iconoZoomManualLabel;
//	private JLabel iconoMantenerProporcionesLabel;
//	private JLabel iconoModoSubcarpetasLabel;
//	private JButton modoZoomActualIconoBoton;
    
    
    // --- REFERENCIAS EXTERNAS Y CONFIGURACIÓN ---
//    private final ViewUIConfig uiConfig;
    private final VisorModel model;
    private final ThumbnailService servicioThumbs;
    private final ThemeManager themeManagerRef;
    
    private Map<String, JMenuItem> menuItemsPorNombre; 
//    private Map<String, JButton> botonesPorNombre;   
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

    // --- CAMPOS para los controles de fondo de previsualización ---
    private JPanel panelControlesFondoIcono;    // El panel que contendrá los 7 puntos
    private List<JPanel> listaPuntosDeColor;      // Lista de los JPanel que actúan como selectores
    private Border bordePuntoNormal;
    private Border bordePuntoSeleccionado;
    private Color colorFondoNeutroParaSwatches;
    
    // --- Icono de Error de Imagen
    private ImageIcon iconoErrorGeneral;
    //private ImageIcon iconoErrorOriginal;
    
    private Rectangle lastNormalBounds;
    private final ConfigurationManager configurationManagerRef; 
    private final IconUtils iconUtilsRef;
    
    private final ComponentRegistry registry;
    private JLabel etiquetaImagen; 
    
    private DefaultListModel<String> modeloListaMiniaturas;
    
    
    /**
      * Constructor principal de la ventana VisorView.
      * Inicializa las dependencias y llama al método de construcción de la UI.
      *
      * @param miniaturaPanelHeight Altura deseada para el scrollpane de miniaturas.
      * @param config Objeto ViewUIConfig con parámetros de apariencia y referencias (como IconUtils).
      * @param modelo El VisorModel principal.
      * @param servicioThumbs El ThumbnailService para renderers de miniaturas.
      * @param themeManager Parametro para la gestion del fondo del label de imagen      
      * @param menuBar La JMenuBar pre-construida (o un placeholder si se construye después).
      * @param toolbarPanel El JPanel de la toolbar pre-construido (o un placeholder).
      * @param menuItems Mapa de JMenuItems generados (puede ser vacío inicialmente).
      * @param botones Mapa de JButtons generados (puede ser vacío inicialmente).
      */
    public VisorView(
            int miniaturaPanelHeight,
            VisorModel modelo,
            ThumbnailService servicioThumbs,
            ThemeManager themeManager,
            ConfigurationManager configurationManager,
            ComponentRegistry registry,
            IconUtils iconUtils
    ) {
        super("Visor/Catalogador de Imágenes");
        System.out.println("[VisorView Constructor SIMPLIFICADO] Iniciando...");

        // --- 1. ASIGNACIÓN DE DEPENDENCIAS ESENCIALES ---
        this.model = Objects.requireNonNull(modelo, "VisorModel no puede ser null");
        this.servicioThumbs = Objects.requireNonNull(servicioThumbs, "ThumbnailService no puede ser null");
        this.themeManagerRef = Objects.requireNonNull(themeManager, "ThemeManager no puede ser null");
        this.configurationManagerRef = Objects.requireNonNull(configurationManager, "ConfigurationManager no puede ser null");
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null");
        this.iconUtilsRef = Objects.requireNonNull(iconUtils, "IconUtils no puede ser null");
        this.miniaturaScrollPaneHeight = miniaturaPanelHeight;
        this.modeloListaMiniaturas = new DefaultListModel<>();

        // Inicializar mapas vacíos. Se poblarán desde fuera.
        this.menuItemsPorNombre = new HashMap<>();
        this.barrasDeHerramientas = new HashMap<>();

        // --- 2. CONFIGURACIÓN BÁSICA DEL JFRAME ---
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        Tema temaActual = this.themeManagerRef.getTemaActual();
        getContentPane().setBackground(temaActual.colorFondoPrincipal());
        
        // --- 3. LÓGICA INTERNA DE LA VENTANA (NO DE COMPONENTES) ---
        // Restaurar tamaño y posición
        if (this.configurationManagerRef != null) {
            int x = this.configurationManagerRef.getInt(ConfigKeys.WINDOW_X, -1);
            int y = this.configurationManagerRef.getInt(ConfigKeys.WINDOW_Y, -1);
            int w = this.configurationManagerRef.getInt(ConfigKeys.WINDOW_WIDTH, 1280);
            int h = this.configurationManagerRef.getInt(ConfigKeys.WINDOW_HEIGHT, 720);

            if (w > 50 && h > 50 && x != -1 && y != -1) {
                setBounds(x, y, w, h);
            } else {
                setSize(w, h);
                setLocationRelativeTo(null);
            }
            
            this.lastNormalBounds = getBounds();
            
            boolean wasMaximized = this.configurationManagerRef.getBoolean(ConfigKeys.WINDOW_MAXIMIZED, false);
            if (wasMaximized) {
                SwingUtilities.invokeLater(() -> setExtendedState(JFrame.MAXIMIZED_BOTH));
            }
        } else {
            setSize(1280, 720);
            setLocationRelativeTo(null);
            this.lastNormalBounds = getBounds();
        }

        // Listener para guardar las dimensiones al mover/redimensionar
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                if (getExtendedState() == JFrame.NORMAL) {
                    lastNormalBounds = getBounds();
                }
            }
            @Override
            public void componentResized(ComponentEvent e) {
                if (getExtendedState() == JFrame.NORMAL) {
                    lastNormalBounds = getBounds();
                }
            }
        });

    } // --- FIN del constructor VisorView ---
    
    
//    public VisorViewOLD(
//    	    int miniaturaPanelHeight,
//    	    VisorModel modelo,
//    	    ThumbnailService servicioThumbs,
//    	    ThemeManager themeManager,
//    	    ConfigurationManager configurationManager,
//    	    ComponentRegistry registry,
//    	    IconUtils iconUtils
//    	) {
//    	    super("Visor/Catalogador de Imágenes");
//    	    System.out.println("[VisorView Constructor SIMPLIFICADO] Iniciando...");
//
//    	    // --- 1. ASIGNACIÓN DE DEPENDENCIAS ESENCIALES ---
//    	    this.model = Objects.requireNonNull(modelo);
//    	    this.servicioThumbs = Objects.requireNonNull(servicioThumbs);
//    	    this.themeManagerRef = Objects.requireNonNull(themeManager);
//    	    this.configurationManagerRef = Objects.requireNonNull(configurationManager);
//    	    this.registry = Objects.requireNonNull(registry);
//    	    this.iconUtilsRef = Objects.requireNonNull(iconUtils);
//    	    this.miniaturaScrollPaneHeight = miniaturaPanelHeight > 0 ? miniaturaPanelHeight : 100;
//    	    
//    	    
//    	    // Inicializar mapas vacíos. Se poblarán desde fuera.
//    	    this.menuItemsPorNombre = new HashMap<>();
//    	    this.barrasDeHerramientas = new HashMap<>();
//    	    
//    	    
//    	    // --- 2. CONFIGURACIÓN BÁSICA DEL JFRAME ---
//    	    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//    	    setLayout(new BorderLayout());
//    	    Tema temaActual = this.themeManagerRef.getTemaActual();
//    	    getContentPane().setBackground(temaActual.colorFondoPrincipal());
//
//    	    // --- 3. INICIALIZAR COMPONENTES QUE SÍ PERTENECEN A VisorView ---
//    	    // Solo la etiqueta de la imagen y los controles de fondo del visor
//    	    inicializarEtiquetaMostrarImagen(); 
//    	    inicializarPanelControlesFondoIcono();
//        
//        
//
//        // --- 5. RESTAURAR ESTADO DE LA VENTANA (TAMAÑO, POSICIÓN, MAXIMIZADO) ---
//        // --- CAMBIO INICIO: Lógica refactorizada para restaurar el estado de la ventana ---
//        if (this.configurationManagerRef != null) {
//            System.out.println("  [VisorView Constructor] Restaurando estado de la ventana desde configuración...");
//            
//            // --- PASO A: Leer SIEMPRE los bounds normales (x, y, ancho, alto) ---
//            //   Estos valores definen el estado de la ventana cuando NO está maximizada.
//            int x = this.configurationManagerRef.getInt(ConfigKeys.WINDOW_X, -1);
//            int y = this.configurationManagerRef.getInt(ConfigKeys.WINDOW_Y, -1);
//            int w = this.configurationManagerRef.getInt(ConfigKeys.WINDOW_WIDTH, 1280);
//            int h = this.configurationManagerRef.getInt(ConfigKeys.WINDOW_HEIGHT, 720);
//            
//            // --- PASO B: Establecer SIEMPRE los bounds normales en el JFrame ---
//            //   Esto asegura que el JFrame sepa a qué tamaño y posición volver cuando se restaure.
//            if (w > 50 && h > 50 && x != -1 && y != -1) {
//                // Si tenemos valores guardados y válidos, los aplicamos.
//                System.out.println("    -> Aplicando bounds guardados: " + new Rectangle(x,y,w,h));
//                setBounds(x, y, w, h);
//            } else {
//                // Si no, aplicamos los valores por defecto y centramos la ventana.
//                System.out.println("    -> Usando bounds por defecto y centrando.");
//                setSize(w, h);
//                setLocationRelativeTo(null);
//            }
//            
//            // --- PASO C: Inicializar 'lastNormalBounds' con los bounds que acabamos de establecer ---
//            //   Ahora 'getBounds()' devolverá los valores correctos que hemos aplicado.
//            this.lastNormalBounds = getBounds();
//            System.out.println("    -> 'lastNormalBounds' inicializado a: " + this.lastNormalBounds);
//            
//            // --- PASO D: Leer y aplicar el estado maximizado DESPUÉS de haber establecido los bounds normales ---
//            boolean wasMaximized = this.configurationManagerRef.getBoolean(ConfigKeys.WINDOW_MAXIMIZED, false);
//            if (wasMaximized) {
//                System.out.println("    -> La configuración indica que la ventana debe iniciar maximizada. Aplicando estado...");
//                // Usamos SwingUtilities.invokeLater para dar tiempo a Swing a procesar los bounds
//                // antes de intentar maximizar. Es una práctica más segura.
//                SwingUtilities.invokeLater(() -> {
//                    setExtendedState(JFrame.MAXIMIZED_BOTH);
//                });
//            }
//            
//            System.out.println("  [VisorView Constructor] Estado de la ventana (tamaño/posición) restaurado desde configuración.");
//            
//        } else {
//            // --- Fallback si ConfigurationManager no está disponible ---
//            setSize(1280, 720);
//            setLocationRelativeTo(null);
//            // Es crucial inicializar lastNormalBounds también en el caso de fallback.
//            this.lastNormalBounds = getBounds();
//            System.err.println("WARN [VisorView Constructor]: uiConfig.configurationManager es null. Usando tamaño/posición de ventana por defecto.");
//        }
//        // --- CAMBIO FIN ---
//        
//        // --- COMPONENTLISTENER (sin cambios, ya estaba bien) ---
//        this.addComponentListener(new ComponentAdapter() {
//        	
//        	this.registry.register("label.imagenPrincipal", this.etiquetaImagen);
//        	
//            @Override
//            public void componentMoved(ComponentEvent e) {
//                if (getExtendedState() == JFrame.NORMAL) {
//                    lastNormalBounds = getBounds();
//                }
//            }
//            @Override
//            public void componentResized(ComponentEvent e) {
//                if (getExtendedState() == JFrame.NORMAL) {
//                    lastNormalBounds = getBounds();
//                }
//            }
//        });
//        
//        // --- 6. AJUSTE FINAL DEL DIVISOR DEL JSPLITPANE  ---
//        
//    }
    
    
//    public void configurarEstadoInicialUI() {
//        if (this.panelControlesFondoIcono != null && this.configurationManagerRef != null && this.themeManagerRef != null) {
//            
//            boolean esCuadrosActual = this.isFondoACuadrosActivado();
//            
//            JPanel puntoAResaltar = null;
//            if (esCuadrosActual) {
//                if (this.listaPuntosDeColor.size() >= 6) {
//                    puntoAResaltar = this.listaPuntosDeColor.get(this.listaPuntosDeColor.size() - 2);
//                }
//            } else {
//                String temaActualNombre = this.themeManagerRef.getTemaActual().nombreInterno();
//                for(JPanel punto : this.listaPuntosDeColor) {
//                    if (punto.getName().equals(temaActualNombre)) {
//                        puntoAResaltar = punto;
//                        break;
//                    }
//                }
//            }
//            
//            if (puntoAResaltar != null) {
//                actualizarResaltadoPuntosDeColor(puntoAResaltar);
//            } else if (!esCuadrosActual) {
//                if (!this.listaPuntosDeColor.isEmpty()) {
//                   actualizarResaltadoPuntosDeColor(this.listaPuntosDeColor.get(this.listaPuntosDeColor.size() - 1));
//                }
//            }
//        }
//    }
    
     /**
      * Devuelve los últimos bounds conocidos de la ventana cuando estaba en estado normal (no maximizada).
      * Si la ventana está actualmente en estado normal, devuelve sus bounds actuales.
      * Si está maximizada, devuelve el último valor guardado por el ComponentListener.
      * @return Un objeto Rectangle con los bounds normales, o null si no se han podido determinar.
      */
     public Rectangle getLastNormalBounds() {
         if (getExtendedState() == JFrame.NORMAL) {
             // Si la ventana ya está en estado normal, sus bounds actuales son los correctos.
             return getBounds();
         }
         // Si está maximizada o minimizada, debemos devolver el valor que guardamos
         // la última vez que SÍ estuvo en estado normal.
         return this.lastNormalBounds; 
     }
     
    
    

    
    

    
    


   

   


//    /**
//     * Crea un JSeparator vertical para usar en la barra de información.
//     * @return Un JSeparator configurado.
//     */
//    private JSeparator crearSeparadorVerticalBarraInfo() {
//        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
//        Dimension d = separator.getPreferredSize();
//        // Intentar basar la altura en la métrica de la fuente de un label de referencia
//        // Es importante que this.nombreArchivoInfoLabel ya esté inicializado cuando se llama esto
//        if (this.nombreArchivoInfoLabel != null && this.nombreArchivoInfoLabel.getFont() != null) {
//            FontMetrics fm = this.nombreArchivoInfoLabel.getFontMetrics(this.nombreArchivoInfoLabel.getFont());
//            // Una altura un poco mayor que la de la fuente puede quedar bien
//            d.height = fm.getHeight() + fm.getDescent() + 2; 
//        } else {
//            // Fallback si el label de referencia no está listo (debería estarlo)
//            d.height = 16;
//        }
//        d.width = 2; // Ancho fijo para el separador
//        separator.setPreferredSize(d);
//        return separator;
//    }


    

    

    
    
    
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
	        Tema temaActual = this.themeManagerRef.getTemaActual();
	        if (temaActual != null) {
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
        // 1. Obtener la JList de nombres desde el registro.
        JList<String> listaNombres = registry.get("list.nombresArchivo");

        // 2. Comprobar si la lista existe antes de asignarle el modelo.
        if (listaNombres != null) {
            listaNombres.setModel(nuevoModelo);
            System.out.println("[VisorView] Modelo asignado a 'list.nombresArchivo'. Tamaño: " + nuevoModelo.getSize());
        } else {
            System.err.println("WARN [setListaImagenesModel]: El componente 'list.nombresArchivo' no se encontró en el registro.");
        }
    } // --- FIN del metodo setListaImagenesModel ---
    

    public void setModeloListaMiniaturas(DefaultListModel<String> nuevoModeloMiniaturas) {
        // Obtener la lista desde el registro
        JList<String> listaMiniaturas = registry.get("list.miniaturas");
        
        if (listaMiniaturas != null && nuevoModeloMiniaturas != null) {
             System.out.println("[VisorView] Estableciendo nuevo modelo para listaMiniaturas (Tamaño: " + nuevoModeloMiniaturas.getSize() + ")");
             listaMiniaturas.setModel(nuevoModeloMiniaturas);
             listaMiniaturas.repaint();
        } else {
             if (listaMiniaturas == null) System.err.println("WARN [setModeloListaMiniaturas]: 'list.miniaturas' no encontrada en registro.");
             if (nuevoModeloMiniaturas == null) System.err.println("WARN [setModeloListaMiniaturas]: el nuevo modelo es null.");
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
    	
    	JPanel panelIzquierdo = registry.get("panel.izquierdo.listaArchivos");
    	
        if (panelIzquierdo == null || !(panelIzquierdo.getBorder() instanceof TitledBorder)) {
            System.err.println("WARN [setTituloPanelIzquierdo]: panelIzquierdo o su TitledBorder nulos.");
            return;
        }
        TitledBorder borde = (TitledBorder) panelIzquierdo.getBorder();
        borde.setTitle(titulo != null ? titulo : "");
        panelIzquierdo.repaint();
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
        // 1. Obtener los componentes necesarios desde el registro.
        // La clave para el panel izquierdo del split podría ser algo como "panel.split.izquierdo"
        // Asumiré que ViewBuilder lo registra con esa clave. Si no, ajusta el nombre.
        // NOTA: ViewBuilder crea un "panelIzquierdo" que contiene una JList, así que es ese el que debemos mostrar/ocultar.
        JPanel panelIzquierdo = registry.get("panel.izquierdo.listaArchivos");
        JSplitPane splitPane = registry.get("splitpane.main");

        // 2. Validar que los componentes existan en el registro.
        if (panelIzquierdo == null) {
            System.err.println("ERROR [VisorView setFileListVisible]: 'panel.izquierdo.listaArchivos' no encontrado en el registro.");
            return;
        }

        // 3. Aplicar la lógica de visibilidad.
        if (panelIzquierdo.isVisible() != visible) {
            System.out.println("  [VisorView setFileListVisible] Cambiando visibilidad del panel izquierdo a: " + visible);
            panelIzquierdo.setVisible(visible);

            if (splitPane != null) {
                if (visible) {
                    // Restaurar la posición del divisor cuando se vuelve a mostrar el panel.
                    SwingUtilities.invokeLater(() -> {
                        double dividerLocationPercentage = 0.25;
                        int newDividerLocation = (int) (splitPane.getWidth() * dividerLocationPercentage);
                        splitPane.setDividerLocation(newDividerLocation);
                        splitPane.revalidate();
                        splitPane.repaint();
                    });
                } else {
                    // Al ocultar el panel izquierdo, algunos L&F contraen el divisor.
                    // Podríamos guardarlo si quisiéramos restaurarlo al valor exacto.
                    splitPane.resetToPreferredSizes();
                }
            } else {
                System.err.println("WARN [VisorView setFileListVisible]: 'splitpane.main' no encontrado en el registro. No se puede ajustar el divisor.");
            }

            revalidateFrame();
            System.out.println("  [VisorView setFileListVisible] Frame revalidado y repintado.");
        } else {
            System.out.println("  [VisorView setFileListVisible] El panel izquierdo ya está en el estado de visibilidad deseado: " + visible);
        }
    }

    
//    public void setThumbnailsVisible(boolean visible) {
//        if (this.scrollListaMiniaturas != null && this.scrollListaMiniaturas.isVisible() != visible) {
//            this.scrollListaMiniaturas.setVisible(visible);
//            SwingUtilities.invokeLater(this::revalidateFrame);
//        }
//    }

    
    private void revalidateFrame() {
        this.revalidate();
        this.repaint();
    }

    public void setCheckeredBackgroundEnabled(boolean activado) {
        if (this.fondoACuadrosActivado != activado) {
            this.fondoACuadrosActivado = activado;
            
            JLabel etiquetaImagen = registry.get("label.imagenPrincipal");
            if (etiquetaImagen != null) {
                etiquetaImagen.repaint();
            }
        }
    }

    

    public void setEstadoMenuActivarZoomManual(boolean seleccionado) {
        String zoomMenuKey = "interfaz.menu.zoom.Activar_Zoom_Manual";
        if (menuItemsPorNombre == null) return;
        JMenuItem zoomMenuItem = this.menuItemsPorNombre.get(zoomMenuKey);
        if (zoomMenuItem instanceof JCheckBoxMenuItem) {
            JCheckBoxMenuItem cbItem = (JCheckBoxMenuItem) zoomMenuItem;
            if (cbItem.isSelected() != seleccionado) cbItem.setSelected(seleccionado);
        }
    }// --- FIN del metodo

    
    
    
    /**
     * Muestra un indicador de "Cargando..." en el área principal de la imagen.
     * Limpia cualquier imagen o icono de error previo.
     *
     * @param mensaje El mensaje a mostrar (ej. "Cargando: nombre_archivo...").
     */
    public void mostrarIndicadorCargaImagenPrincipal(String mensaje) {
        // 1. Obtener la etiqueta de imagen desde el registro.
        JLabel etiquetaImagen = registry.get("label.imagenPrincipal");
        if (etiquetaImagen == null) {
            System.err.println("ERROR [mostrarIndicadorCargaImagenPrincipal]: 'label.imagenPrincipal' no encontrado en el registro.");
            return;
        }

        String mensajeAMostrar = (mensaje != null && !mensaje.trim().isEmpty()) ? mensaje : "Cargando...";

        // 2. Modificar el estado interno de la vista (esto es correcto, ya que afecta al paintComponent).
        this.imagenReescaladaView = null; // No hay imagen para dibujar mientras se carga.

        // 3. Modificar el componente JLabel obtenido del registro.
        etiquetaImagen.setIcon(null);
        etiquetaImagen.setText(mensajeAMostrar);

        Tema temaActual = this.themeManagerRef.getTemaActual();
        if (temaActual != null) {
             etiquetaImagen.setForeground(temaActual.colorTextoPrimario());
        } else {
             etiquetaImagen.setForeground(Color.BLACK); // Fallback
        }
        
        etiquetaImagen.setHorizontalAlignment(SwingConstants.CENTER);
        etiquetaImagen.setVerticalAlignment(SwingConstants.CENTER);
        etiquetaImagen.setOpaque(false); // Para que el fondo personalizado de paintComponent se vea.
        etiquetaImagen.repaint();
        
    } // --- FIN del metodo mostrarIndicadorCargaImagenPrincipal ---
    
    
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

        // --- 1. OBTENER EL COMPONENTE DESDE EL REGISTRO ---
        JList<String> listaMiniaturas = registry.get("list.miniaturas");
        if (listaMiniaturas == null) {
            System.err.println("ERROR [solicitarRefrescoRenderersMiniaturas]: 'list.miniaturas' no encontrado en el registro.");
            return;
        }

        // --- 2. VALIDAR OTRAS DEPENDENCIAS (sin cambios) ---
        if (this.model == null || this.configurationManagerRef == null || this.servicioThumbs == null || this.themeManagerRef == null || this.iconUtilsRef == null) {
             System.err.println("ERROR [solicitarRefrescoRenderersMiniaturas]: Faltan dependencias esenciales.");
             return;
        }

        // --- 3. OBTENER CONFIGURACIONES (sin cambios) ---
        boolean mostrarNombresActual = this.configurationManagerRef.getBoolean(ConfigKeys.VISTA_MOSTRAR_NOMBRES_MINIATURAS_STATE, true);
        int thumbWidth = this.configurationManagerRef.getInt("miniaturas.tamano.normal.ancho", 40);
        int thumbHeight = this.configurationManagerRef.getInt("miniaturas.tamano.normal.alto", 40);

        // --- 4. CREAR EL NUEVO RENDERER (sin cambios) ---
        MiniaturaListCellRenderer newRenderer = new MiniaturaListCellRenderer(
            this.servicioThumbs,
            this.model,
            this.themeManagerRef,
            this.iconUtilsRef,
            thumbWidth,
            thumbHeight,
            mostrarNombresActual
        );

        // --- 5. APLICAR EL NUEVO RENDERER A LA JLIST OBTENIDA DEL REGISTRO ---
        listaMiniaturas.setCellRenderer(newRenderer);
        listaMiniaturas.setFixedCellHeight(newRenderer.getAlturaCalculadaDeCelda());
        listaMiniaturas.setFixedCellWidth(newRenderer.getAnchoCalculadaDeCelda());
        listaMiniaturas.revalidate();
        listaMiniaturas.repaint();

        System.out.println("[VisorView] Refresco de renderers de miniaturas completado.");
    } // --- FIN del metodo solicitarRefrescoRenderersMiniaturas ---
    
    
    
    
    
    

    
//    public void addToolbar(String nombre, JToolBar barra) {
//        if (nombre == null || barra == null) return;
//        this.barrasDeHerramientas.put(nombre, barra);
//        this.contenedorDeBarras.add(barra);
//    }

//    
    
    
    public void addListaNombresSelectionListener(javax.swing.event.ListSelectionListener listener) {
        JList<String> listaNombres = registry.get("list.nombresArchivo");
        if (listaNombres != null) {
            listaNombres.addListSelectionListener(listener);
        }
    }

    public void addListaMiniaturasSelectionListener(javax.swing.event.ListSelectionListener listener) {
        JList<String> listaMiniaturas = registry.get("list.miniaturas");
        if (listaMiniaturas != null) {
            listaMiniaturas.addListSelectionListener(listener);
        }
    }

    public void addEtiquetaImagenMouseWheelListener(java.awt.event.MouseWheelListener listener) {
        // etiquetaImagen SÍ es un campo de VisorView, por lo que este método puede quedar igual.
        // Solo añadimos una comprobación por si se refactoriza en el futuro.
        JLabel etiquetaImagen = registry.get("label.imagenPrincipal");
        if (etiquetaImagen != null) {
            etiquetaImagen.addMouseWheelListener(listener);
        }
    }

    public void addEtiquetaImagenMouseListener(java.awt.event.MouseListener listener) {
        JLabel etiquetaImagen = registry.get("label.imagenPrincipal");
        if (etiquetaImagen != null) {
            etiquetaImagen.addMouseListener(listener);
        }
    }

    public void addEtiquetaImagenMouseMotionListener(java.awt.event.MouseMotionListener listener) {
        JLabel etiquetaImagen = registry.get("label.imagenPrincipal");
        if (etiquetaImagen != null) {
            etiquetaImagen.addMouseMotionListener(listener);
        }
    }
    
    public Image getImagenReescaladaView() {
        return this.imagenReescaladaView;
    }
    
    

   
    // Getters para la barra de botones
    public JPanel getPanelDeBotones() {return this.panelDeBotones; }
    public Map<String, JToolBar> getToolbars() {return this.barrasDeHerramientas;}
    public boolean isFondoACuadrosActivado() {return this.fondoACuadrosActivado;}
	public DefaultListModel<String> getModeloListaMiniaturas(){ return this.modeloListaMiniaturas; }
    
    
} // Fin clase VisorView