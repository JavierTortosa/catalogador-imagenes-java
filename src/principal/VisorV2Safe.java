package principal;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import servicios.ConfigurationManager;

/**
 * The Class VisorV2.
 */
public class VisorV2Safe extends JFrame implements ActionListener, ClipboardOwner
{
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

// ************************************************************************ DECLARACIÓN DE COMPONENTES PRINCIPALES *****

	/** The menu bar. */
// componentes
	private JMenuBar menuBar; // Barra de menú
	private JList<String> listaImagenes; // Lista de imágenes cargadas
	private JLabel etiquetaImagen; // Etiqueta para mostrar la imagen seleccionada
	private JSplitPane splitPane; // Panel dividido para organizar la interfaz
	private JTextField textoRuta; // Campo de texto para mostrar la ruta de la imagen seleccionada
	private JPanel panelPrincipal; // Panel principal que actuará como contenedor general
	private JPanel panelSuperior; // Panel superior que contiene los botones y otros componentes
	private JPanel panelIzquierdo;
	private JPanel panelBotones; // Panel general para los botones
	private JPanel panelBotonesIzquierda; // Panel para los botones de la izquierda
	private JPanel panelBotonesCentro; // Panel para los botones centrales
	private JPanel panelBotonesDerecha; // Panel para los botones derechos
	private JPanel panelImagenesMiniatura; // Panel para las imagenes en miniaturas
	private JButton[] botonesCentro; // Botones centrales
	
	// variables puente
	private BufferedImage currentImage; // Imagen actualmente cargada
	private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()); // Pool
	private Image imagenReescalada; // Imagen reescalada para ajustar al tamaño de la etiqueta
	private Map<String, JButton> botonesPorNombre = new HashMap<>();
	private Map<String, JMenuItem> menuItemsPorNombre = new HashMap<>();


	//se cargaran los valores en config.cfg
	private Color colorOriginalFondoBoton = new Color (238,238,238); //gris Color inicial del fondo de los botones
	private Color colorFondoBotonActivado = new Color (84, 144, 164); //azul oscuro Color inicial del fondo de los botones
	private Color colorFondoBotonAnimacion = new Color(173, 216, 230); //azul claro Color de fondo para la animacion
	
	private int miniaturasAntes  =7;		// Antes: imagenesMiniaturaIndiceInicial
	private int miniaturasDespues  =7;		// Antes: imagenesMiniaturaIndiceFinal
	private int miniaturaSelAncho = 90;   	// Antes: anchoMiniaturaSeleccionada
	private int miniaturaSelAlto = 90;    	// Antes: altoMiniaturaSeleccionada
	private int miniaturaNormAncho = 70;  	// Antes: anchoMiniaturaNoSeleccionada
	private int miniaturaNormAlto = 70;   	// Antes: altoMiniaturaNoSeleccionada
	private boolean mostrarSoloCarpetaActual = false; // Por defecto, muestra imágenes con subcarpetas
	
	private boolean zoomHabilitado = false; // Indica si el zoom está activado

	// variables de usuario
	private double zoomFactor = 1.0; // Factor de zoom aplicado a la imagen
	private DefaultListModel<String> modeloLista; // Modelo de datos para la lista de imágenes
	private Map<String, ImageIcon> miniaturasMap = new HashMap<>(); // Mapa para almacenar miniaturas
	private Map<String, Path> rutaCompletaMap = new HashMap<>(); // Mapa para almacenar rutas completas de las imágenes
	private int imageOffsetX = 0, imageOffsetY = 0; // Desplazamiento de la imagen al hacer zoom
	private int lastMouseX, lastMouseY; // Coordenadas del ratón para el arrastre
	private volatile boolean estaCargandoLista = false; // Flag para controlar estado
	private Future<?> cargaImagenesFuture; // Futuro para controlar la tarea de carga de imágenes
    private Future<?> cargaMiniaturasFuture; // Para cancelar carga de miniaturas
	
    
    // Variables movidss a ConfigurationManager
//    	private Map<String, String> config;
    	
    // constantes
    	//private static final String CONFIG_FILE_PATH = "config.cfg";
    
    // Interaccion con otras clases
    ConfigurationManager configuration;
    
//    VisorUiHelper uiHelper = new VisorUiHelper();
    
// ***** CONSTRUCTOR PRINCIPAL *****
	public VisorV2Safe()
	{
		super("Visor de Imágenes");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		setSize(1500, 600);
//			setSize(800, 600);
//			setExtendedState(MAXIMIZED_BOTH);
		
		
		//Ideas para implementar
			//TODO Buscador de imagenes
			//TODO catalogar las imagenes
			//TODO filtro por favoritos, por letra inicial...
			//TODO que muestre una cuadrado en negro en la barra de miniaturas cuando una imagen no se puede cargar
	
			//TODO establecer los paneles visibles segun config.cfg
			//TODO asignar los estados a los botones y menuItems
		
		//bugs encontrados y tareas a mejorar
			//BUG cuando activamos el zoom y seleccionamos una imagen diferente el zoom se desactiva
			//BUG cuando activamos el zoom se reinicia la imagen
			//BUG el selector de carpeta reescribe el config.cfg 
			//BUG mezcla todas las imagenes de las carpetas
		
		// --- Inicialización de Configuración ---
        try {
            // Instancia de ConfigurationManages para comprobar que todo ha ido bien
            configuration = new ConfigurationManager();
        } catch (IOException e) {
            // Error IRRECUPERABLE al cargar/crear config.
            JOptionPane.showMessageDialog(this,
                    "Error fatal al cargar o crear el archivo de configuración:\n" + e.getMessage() +
                    "\nLa aplicación podría no funcionar correctamente o usará valores por defecto.",
                    "Error de Configuración Crítico", JOptionPane.ERROR_MESSAGE);
		
		            // Alternativa: Si no puedes funcionar sin config, podrías salir:
            			// System.err.println("Error crítico de configuración. Saliendo.");
		            	// System.exit(1);
		
            
             if (configuration == null) 
             {
                 System.err.println("Fallo en la inicialización de ConfigurationManager. Usando mapa vacío.");
             }
        }
            
		// Inicializar componentes
        	inicializarComponentes();

    	// Cargar y aplicar configuración (SI configuration se inicializó)
        if (configuration != null) 
        {
            aplicarConfiguracion(); // Ya no necesita el Map como argumento
            cargarEstadoInicial(); // Ya no necesita el Map como argumento
        } else {
             System.err.println("ADVERTENCIA: ConfigurationManager no inicializado. La UI usará valores codificados.");
             
             // aplicar valores por defecto codificados si hay que asegurar algo básico.
             //configuracionInicialMenu(); 
        }
        	
		// Configurar eventos
		configurarEventos();
		
		// Hook de cierre
		Runtime.getRuntime().addShutdownHook(new Thread(() -> 
		{
			
	        //Guardar configuración al cerrar
			System.out.println("Guardando configuración al cerrar...");
	        
			if (configuration != null) // Solo guardar si se inicializó bien 
			{ 
				guardarConfiguracionActual();
			} else {
		         System.out.println("Hook: ConfigurationManager nulo, no se guarda config.");
		    }
			System.out.println("Hook: guardarConfiguracionActual() terminado.");
			

			//Apagar ExecutorService y ESPERAR su terminación
		    System.out.println("Hook: Iniciando apagado del ExecutorService...");
		    if (executorService != null && !executorService.isShutdown()) 
		    {
		         executorService.shutdown(); // Paso 1: Iniciar apagado ordenado
		         try 
		         {
		             // Paso 2: Esperar a que las tareas existentes terminen
		             System.out.println("Hook: Esperando terminación del ExecutorService (hasta 5 segundos)...");
		             if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) 
		             { // <-- LA ESPERA ESTÁ AQUÍ
		            	 
		                 // Paso 3: Si no terminaron a tiempo, forzar apagado
		                 System.err.println("Hook: ExecutorService no terminó en 5s, forzando apagado (shutdownNow)...");
		                 List<Runnable> droppedTasks = executorService.shutdownNow(); // Intenta interrumpir
		                 System.err.println("Hook: Tareas interrumpidas/no iniciadas: " + droppedTasks.size());
		                 
		                 // Paso 4: Esperar un poco más después de forzar
		                 if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) 
		                 {
		                     System.err.println("¡¡¡Hook - ERROR CRITICO: ExecutorService no terminó incluso después de shutdownNow()!!!");
		                 } else {
		                      System.out.println("Hook: ExecutorService terminó después de shutdownNow().");
		                 }
		             } else {
		                 System.out.println("Hook: ExecutorService terminó ordenadamente.");
		             }
		         } catch (InterruptedException e) {
		             System.err.println("Hook: Interrumpido mientras esperaba ExecutorService.");
		             executorService.shutdownNow(); // Asegurarse de forzar si se interrumpe
		             Thread.currentThread().interrupt();
		         }
		    } else if (executorService != null) {
		         System.out.println("Hook: ExecutorService ya estaba apagado.");
		    } else {
		         System.out.println("Hook: ExecutorService era nulo.");
		    }
		    
		    System.out.println("--- Shutdown Hook Terminado ---"); // Este mensaje debería aparecer justo antes de que la JVM termine
		    
		}, "AppShutdownThread")); // Fin del Hook
//	    }));
	    
	    setVisible(true);
	}
	
	
	/**
	 * Cargar estado inicial.
	 *
	 *	Carga el estado inicial de la aplicación (carpeta inicial y última imagen seleccionada) desde el archivo de configuración.
	 *
	 * @param config the config
	 */
	private void cargarEstadoInicial() 
	{
		if (configuration == null) return; // Seguridad
		
	    String folderInit = configuration.getString("inicio.carpeta", ""); 
	    if (!folderInit.isEmpty()) 
	    {
	        cargarCarpetaInicial(folderInit);
	        
	    } else {
	    	System.out.println("No hay carpeta inicial definida en la configuración.");
            
	    	// Podrías limpiar la UI aquí si es necesario (lista, imagen, etc.)
	    	limpiarUI();
	    	
//	    	  modeloLista.clear();
//            rutaCompletaMap.clear();
//            miniaturasMap.clear();
//            etiquetaImagen.setIcon(null);
//            textoRuta.setText("");
//            panelIzquierdo.setBorder(BorderFactory.createTitledBorder("Lista de Archivos"));
//            panelImagenesMiniatura.removeAll();
//            panelImagenesMiniatura.repaint();
	    }
	}
	
	/**
	 * Cargar carpeta inicial.
	 *
	 * @param folderInit the folder init
	 */
	private void cargarCarpetaInicial(String folderInit) 
	{
		if (configuration == null) return; // Seguridad
		
	    File carpetaInicial = new File(folderInit);
	    if (carpetaInicial.exists() && carpetaInicial.isDirectory()) 
	    {
	    	cargarListaImagenes();
	    	
	    	// Cargar la imagen inicial y luego actualizar las miniaturas
	        String imageInit = configuration.getString("inicio.imagen", "");
	        cargarImagenInicial(imageInit); // Este método usa el string
	        
	        if (modeloLista.getSize() > 0) 
	        {
	            int indiceInicial = listaImagenes.getSelectedIndex() >= 0 ? listaImagenes.getSelectedIndex() : 0;
	            
	            actualizarImagenesMiniaturaConRango(indiceInicial); // Cargar miniaturas al inicio
	        }
	    } else {
	    	System.err.println("La carpeta inicial '" + folderInit + "' no existe o no es un directorio.");
            JOptionPane.showMessageDialog(this, "La carpeta inicial configurada\n'" + folderInit + "'\nno es válida.",
                                          "Error de Carpeta Inicial", JOptionPane.WARNING_MESSAGE);
            
         // Limpiar UI
            limpiarUI();
//            modeloLista.clear();
//            rutaCompletaMap.clear();
//            miniaturasMap.clear();
//            etiquetaImagen.setIcon(null);
//            textoRuta.setText("");
//            panelIzquierdo.setBorder(BorderFactory.createTitledBorder("Lista de Archivos"));
//            panelImagenesMiniatura.removeAll();
//            panelImagenesMiniatura.repaint();
	    }
	}

	// Limpia toda la interfaz
	private void limpiarUI()
	{
		modeloLista.clear();
        rutaCompletaMap.clear();
        miniaturasMap.clear();
        etiquetaImagen.setIcon(null);
        textoRuta.setText("");
        panelIzquierdo.setBorder(BorderFactory.createTitledBorder("Lista de Archivos"));
        panelImagenesMiniatura.removeAll();
        panelImagenesMiniatura.repaint();
	}

	/**
	 * Cargar imagen inicial.
	 *
	 * @param imageInit the image init
	 */
	private void cargarImagenInicial(String imageInit) 
	{
	    if (!imageInit.isEmpty()) 
	    {
	        for (int i = 0; i < modeloLista.getSize(); i++) 
	        {
	            String nombreArchivo = modeloLista.getElementAt(i);
	            if (nombreArchivo.equals(imageInit)) 
	            {
	                listaImagenes.setSelectedIndex(i);
	                mostrarImagenSeleccionada();
	                break;
	            }
	        }
	    }
	}
	
	
	/**
	 * Inicializar componentes.
	 * 
	 * Inicializa los componentes principales de la interfaz gráfica (paneles, botones, menús, etc.).
	 */
	// ***** INICIALIZACIÓN DE COMPONENTES *****
	private void inicializarComponentes()
	{
		// ** Iniciamos componentes independientes

		// Inicializar el panel principal
		panelPrincipal = new JPanel(new BorderLayout());
		
		// Creamos la barra de menu
		inicializarMenu();
		
		// ** Inicializar componentes específicos
		
		// inicializamos panelIzquierdo
		inicializarPanelIzquierdo();

		// Inicializamos panelSuperior
		JPanel panelTempSuperior = inicializarPanelSuperior();

		// inicializamos panelImagenesMiniatura
		JPanel panelTempImagenesMiniatura = inicializarPanelImagenesMiniatura();

		// ** Agregar componentes al panel principal
		panelPrincipal.add(panelTempSuperior, BorderLayout.NORTH);
		panelPrincipal.add(splitPane, BorderLayout.CENTER);
		panelPrincipal.add(panelTempImagenesMiniatura, BorderLayout.SOUTH);
		
		// Añadir el panel principal al JFrame
		add(panelPrincipal, BorderLayout.CENTER);

		// Añadir la barra de ruta completa fuera del panel principal
		add(textoRuta, BorderLayout.SOUTH);
	}
	
	
	/**
	 * Inicializar panel superior.
	 *
	 * @return  JPanel
	 */
	private JPanel inicializarPanelSuperior()
	{
		panelSuperior = new JPanel(new BorderLayout());
		
		// Paneles de botones
		panelSuperior.add(inicializarBotonesCentrales());
		
		return panelSuperior;
	}

	
	/**
	 * Inicializar panel izquierdo.
	 */
	private void inicializarPanelIzquierdo()
	{
		// Lista de imágenes
		modeloLista = new DefaultListModel<>();
		listaImagenes = new JList<>(modeloLista);
		listaImagenes.setCellRenderer(new NombreArchivoRenderer());
		JScrollPane scrollPaneLista = new JScrollPane(listaImagenes);

		// Texto de ruta
		textoRuta = new JTextField();
		textoRuta.setEditable(false);

		// Panel izquierdo (lista de imágenes)
		panelIzquierdo = new JPanel(new BorderLayout());
		panelIzquierdo.setBorder(BorderFactory.createTitledBorder("Lista de Archivos"));
		panelIzquierdo.add(scrollPaneLista, BorderLayout.CENTER);
		panelIzquierdo.add(textoRuta, BorderLayout.SOUTH);

		// inicializar etiqueta para mostrar la imagen
		inicializarEtiquetaMostrarImagen();
		
		// SplitPane para dividir la interfaz
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierdo, etiquetaImagen);
		splitPane.setResizeWeight(0.5);
		splitPane.setContinuousLayout(true);
	}

	
	/**
	 * Inicializar panel imagenes miniatura.
	 *
	 * @return the j panel
	 */
	private JPanel inicializarPanelImagenesMiniatura() 
	{
	    panelImagenesMiniatura = new JPanel();
		    panelImagenesMiniatura.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
		    panelImagenesMiniatura.setBorder(BorderFactory.createTitledBorder("Miniaturas"));
		    panelImagenesMiniatura.setBackground(Color.WHITE);

	    JScrollPane scrollPane = new JScrollPane(panelImagenesMiniatura);
		    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		    scrollPane.setPreferredSize(new Dimension(1280, 150));

	    JPanel contenedor = new JPanel(new BorderLayout());
	    	contenedor.add(scrollPane, BorderLayout.CENTER);
	    	
	    return contenedor;
	}
	
	
// *************************************************************************************************** GESTION DE BOTONES
	
	
	/**
	 * Inicializar botones centrales.
	 *
	 * @return the j panel
	 */
	// Crear botones de forma dinamica
	private JPanel inicializarBotonesCentrales()
	{
//		System.out.println("iniciamos las variables y los arrays");
		int alineacion = 1; // valor de alineacion: 0 izquierda - 1 centro - 2 derecha

		panelBotonesIzquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5)); // Alineado al centro
		panelBotonesCentro = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5)); // Alineado al centro
		panelBotonesDerecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5)); // Alineado al centro

		String[] nombresIconosCentro =
		{

				// espacio al inicio de la barra de botones
				"separadorI",

				// desplazamiento
				"01-Anterior_48x48.png", 
				"02-Siguiente_48x48.png",

				// edicion
				"separadorC", 
				"03-Rotar_Izquierda_48x48.png", 
				"04-Rotar_Derecha_48x48.png",
				"05-Espejo_Horizontal_48x48.png", 
				"06-Espejo_Vertical_48x48.png", 
				"07-Recortar_48x48.png",

				// tamaño
				"separadorC", 
				"08-Zoom_48x48.png", 
				"09-Zoom_Auto_48x48.png", 
				"10-Ajustar_al_Ancho_48x48.png",
				"11-Ajustar_al_Alto_48x48.png", 
				"12-Escalar_Para_Ajustar_48x48.png", 
				"13-Zoom_Fijo_48x48.png",
				"14-Reset_48x48.png",

				// visualizacion
				"separadorC", 
				"15-Panel-Galeria_48x48.png",
//				"16-panel-galeria_lateral_48x48.png",
				"16-Grid_48x48.png", 
				"17-Pantalla_Completa_48x48.png",
				
				// opciones especiales
//				"separadorC",
				"18-Lista_48x48.png", 
				"19-Carrousel_48x48.png",

				// comportamiento
				"separadorC", 
				"20-Refrescar_48x48.png", 
				"21-Subcarpetas_48x48.png", 
				"22-lista_de_favoritos_48x48.png",

				// archivo
				"separadorC", 
				"23-Borrar_48x48.png",

				// especiales
				"separadorD", 
				"24-Selector_de_Carpetas_48x48.png", 
				"25-Menu_48x48.png", 
				"26-Botones_Ocultos_48x48.png",

				// espacio al final del panel de botones
				"separadorD" };

		// creamos el primer boton
		botonesCentro = new JButton[nombresIconosCentro.length];

		for (int i = 0; i < nombresIconosCentro.length; i++)
		{
			String nombreIcono = nombresIconosCentro[i];

			if ("separadorI".equals(nombreIcono))
				alineacion = 0; // alineacion = izquierda
			if ("separadorC".equals(nombreIcono))
				alineacion = 1; // alineacion = centro
			if ("separadorD".equals(nombreIcono))
				alineacion = 2; // alineacion = derecha

			if ("separadorI".equals(nombreIcono) || "separadorC".equals(nombreIcono)
					|| "separadorD".equals(nombreIcono))
			{
				separadorVisualParaBotones(alineacion); // Separador visual
			} else
			{
				procesarBotonNormal(nombreIcono, i, alineacion);
			}
		}

		// añadir detalles a los botones
//configuracionInicialBotones();

		// Panel superior (contiene ambos paneles de botones)
		panelBotones = new JPanel(new BorderLayout());

		panelBotones.add(panelBotonesIzquierda, BorderLayout.WEST);
		panelBotones.add(panelBotonesCentro, BorderLayout.CENTER);
		panelBotones.add(panelBotonesDerecha, BorderLayout.EAST);

		return panelBotones;
	}


	/**
	 * Procesar boton normal.
	 *
	 * @param nombreIcono the nombre icono
	 * @param i the i
	 * @param alineacion the alineacion
	 */
	private void procesarBotonNormal(String nombreIcono, int i, int alineacion) 
	{
	    botonesCentro[i] = new JButton();
	    try 
	    {

//LOG carga de iconos	    	
//System.out.println("cargamos icono " + nombreIcono);
	    	
	        ImageIcon icon = new ImageIcon(ImageIO.read(getClass().getResource("/iconos/" + nombreIcono)));
	        ImageIcon icon1 = new ImageIcon(icon.getImage().getScaledInstance(32, -1, java.awt.Image.SCALE_DEFAULT));
	        botonesCentro[i].setIcon(icon1);
	        
	        botonesCentro[i].setToolTipText(nombreIcono.replace(".png", "")
	        		.replace("_", " ")
	        		.replace("48x48", "")
	        		.replaceAll("\\d+-", ""));
	        
	    } catch (IOException e) {
	        JOptionPane.showMessageDialog(this, "No se pudo cargar el icono: " + nombreIcono
	        		, "Error", JOptionPane.ERROR_MESSAGE);
	    }

	    // Configurar propiedades del botón
	    botonesCentro[i].setPreferredSize(new Dimension(32, 32));
	    botonesCentro[i].setMargin(new Insets(0, 0, 0, 0));
	    botonesCentro[i].setBorderPainted(false);
	    botonesCentro[i].setFocusPainted(false);
		botonesCentro[i].setContentAreaFilled(true);

	    // Generar un nombre único y añadir al Map
	    String nombreBoton = nombreIcono.replaceAll("\\d+-", "").replace(".png", "");
	    botonesCentro[i].setActionCommand(nombreBoton);
	    botonesPorNombre.put(nombreBoton, botonesCentro[i]);

	    // Añadir listener y agregar al panel correspondiente
	    addButtonListener(botonesCentro[i], nombreIcono);
	    if (alineacion == 0) panelBotonesIzquierda.add(botonesCentro[i]);
	    if (alineacion == 1) panelBotonesCentro.add(botonesCentro[i]);
	    if (alineacion == 2) panelBotonesDerecha.add(botonesCentro[i]);
	}
	
	
	/**
	 * Separador visual para botones.
	 *
	 * @param izquierdaCentroDerecha the izquierda centro derecha
	 */
	// Crea un boton vacio y lo añade al panel correspondiente
	private void separadorVisualParaBotones(int izquierdaCentroDerecha)
	{
		if (izquierdaCentroDerecha == 0)
			panelBotonesIzquierda.add(Box.createHorizontalStrut(10));
		if (izquierdaCentroDerecha == 1)
			panelBotonesCentro.add(Box.createHorizontalStrut(10));
		if (izquierdaCentroDerecha == 2)
			panelBotonesDerecha.add(Box.createHorizontalStrut(10));
	}
	
	
	/**
	 * Configuracion inicial botones.
	 */
/*	
	// FIXME este metodo se sustituira por el config.cfg
	private void configuracionInicialBotones()
	{
		botonesPorNombre.get("Reset_48x48").setEnabled(false);	//boton reset zoom desactivado
		botonesPorNombre.get("Botones_Ocultos_48x48").setEnabled(true);// boton de botones oculto desactivado
//		botonesPorNombre.get("Botones_Ocultos_48x48").setVisible(false);
		
		botonesPorNombre.get("Ajustar_al_Alto_48x48").setEnabled(true);
		botonesPorNombre.get("Panel-Galeria_48x48").setEnabled(true);
		botonesPorNombre.get("Lista_48x48").setEnabled(true);
		
	}
*/


	/**
	 * Add button listener.
	 *
	 * Asocia un ActionListener a un botón, generando un nombre único (actionCommand) basado en el nombre del icono.
	 * 
	 * @param boton El boton
	 * @param nombreIcono El nombre icono
	 */
	private void addButtonListener(JButton boton, String nombreIcono) {
        boton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Generar un nombre de método basado en el nombre del icono
                String nombreMetodo = nombreIcono
                		.replaceAll("\\d+-", "") // Eliminar números iniciales
//                		.replace("_", "")    // Eliminar guiones bajos
                		.replace(".png", ""); // Eliminar la extensión
                
                // Imprimir el nombre del método generado para depuración
                System.out.println("El método asociado al boton es: " + nombreMetodo);
                
                // Establecer el comando de acción para identificarlo más fácilmente
                boton.setActionCommand(nombreMetodo);
                
                // Llamar al método que procesa los eventos de los botones
                metodosBarraBotones(e); // Pasar el evento original
            }
        });
    }
	

    /**
     * Metodos barra botones.
     *
     *  Procesa los eventos generados por los botones centrales. Usa un switch para ejecutar lógica específica según el botón pulsado
     *
     * @param e the e
     */
    private void metodosBarraBotones(ActionEvent e) 
    {
    	
//LOG inicio de: metodosBarraBotones 
//System.out.print("-->>>estoy en el metodo de los metodosBarraBotones");
    	
        String actionCommand = e.getActionCommand();
        aplicarAnimacionBoton(actionCommand);

        switch (actionCommand) 
        {
          	//TODO Implantacion de los metodos para los botones
        	  case "Anterior_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              case "Siguiente_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              	
              case "Rotar_Izquierda_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              case "Rotar_Derecha_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              case "Espejo_Horizontal_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              case "Espejo_Vertical_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              case "Recortar_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              
              case "Zoom_48x48" -> toggleZoom(!zoomHabilitado); 
              case "Zoom_Auto_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              case "Ajustar_al_Ancho_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              case "Ajustar_al_Alto_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              case "Escalar_Para_Ajustar_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              case "Zoom_Fijo_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              case "Reset_48x48" -> aplicarResetZoom();
              
              case "Panel-Galeria_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
//				case "panel-galeria_lateral_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              case "Grid_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              case "Pantalla_Completa_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              
              case "Lista_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              case "Carrousel_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              
              case "Refrescar_48x48" -> refrescarListaImagenes();
              case "Subcarpetas_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              case "Lista_de_Favoritos_48x48" -> System.out.println(actionCommand + " pendiente de implementar");

              case "Borrar_48x48" -> System.out.println(actionCommand + " pendiente de implementar");

              case "Selector_de_Carpetas_48x48" -> abrirSelectorDeCarpeta();
              case "Menu_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              case "Botones_Ocultos_48x48" -> System.out.println(actionCommand + " pendiente de implementar");
              default -> System.out.println("Acción no implementada para: " + actionCommand);
          };
      }
	
    
    /**
     * Aplicar animacion boton.
     *
     * Aplica una animación visual temporal al botón pulsado cambiando su color de fondo durante un breve período.
     *
     * @param boton the boton
     */
    private void aplicarAnimacionBoton(String boton)
	{
		JButton jBoton = botonesPorNombre.get(boton);
		
		if (jBoton != botonesPorNombre.get("Zoom_48x48"))
		{
			// Cambia el color del botón temporalmente
			jBoton.setBackground(colorFondoBotonAnimacion); // Azul claro

			// Restaura el color original después de 200 ms
			Timer timer = new Timer(0, evt -> {
				jBoton.setBackground(colorOriginalFondoBoton);
			});

			timer.setRepeats(false);
			timer.start();
		}
	}
	
// *************************************************************************************************** CREAR MENU
	
//	**** INSTRUCCIONES GENERADOR DE BARRA DE MENUS	
		// TODO 	pendiente de modificar los prefijos
	
//		// niveles
//			-	barra de menu
//			--  opciones del menu
//			--- opciones del submenu
//			
//		// tipo de elemento
//			_ barra de separacion
//			= nommal
//			* checkbox
//			. radiobutton
//			
//		// inicios y finales
//			< inicio submenu 
//			> fin submenu
//			
//			{ inicio radioButtonGroup 
//			} fin radioButtonGroup
//			
//		formato de la opcion del menu:
//			nivel tipo etiqueta_visible
			
	
	/**
 * Inicializar menu.
 */
// crea el menu en la parte superior
	private void inicializarMenu()
	{

		String[] menuOptions = {
			"- Archivo",
			
			"--- Abrir Archivo",
			"--- Abrir en ventana nueva",
			"--- Guardar",
			"--- Guardar Como",
			"_",
			"--- Abrir Con...",
			"--- Editar Imagen",
			"--- Imprimir",
			"--- Compartir",
			"_",
			"--- Refrescar Imagen",
			"--- Volver a Cargar",
			"--- Recargar Lista de Imagenes",
			"--- Unload Imagen",

			
			"- Navegacion",

			"--- Imagen Aterior",
			"--- Imagen Siguiente",
			"_",
			"--- Ir a...",
			"--- Primera Imagen",
			"--- Ultima Imagen",
			"_",
			"--- Anterior Fotograma",
			"--- Siguiente Fotograma",
			"--- Primer Fotograma",
			"--- Ultimo Fotograma",


			"- Zoom",

			"--- Acercar",
			"--- Alejar",
			"--- Zoom Personalizado %",
			"--- Zoom Tamaño Real",
			"---* Mantener Proporciones",
			"_",
			"---* Activar Zoom Manual",//-*
			"--- Resetear Zoom",
			"_",
			"--< Tipos de Zoom",
				"--{",
				"---. Zoom Automatico",
				"---. Zoom a lo Ancho",//-.
				"---. Zoom a lo Alto",//-.
				"---. Escalar Para Ajustar",//-.
				"---. Zoom Actual Fijo",//-.
				"---. Zoom Especificado",//-.
				"---. Escalar Para Rellenar",//-.
				"--}",
				"-->",

			
			"- Imagen",

				"--< Carga y Orden",
					"--{",
					"----. Nombre por Defecto",
					"----. Tamaño de Archivo",
					"----. Fecha de Creacion",
					"----. Extension",
					"--}",
					"_",
					"--{",
					"----. Sin Ordenar",
					"----. Ascendente",
					"----. Descendente",
					"--}",
					"-->",
				"--< Edicion",
					"---- Girar Izquierda",
					"---- Girar Derecha",
					"---- Voltear Horizontal",
					"---- Voltear Vertical",
					"-->",
				"_",
				"--- Cambiar Nombre de la Imagen",
				"--- Mover a la Papelera",
				"--- Eliminar Permanentemente",
				"_",
				"--- Establecer Como Fondo de Escritorio",
				"--- Establecer Como Imagen de Bloqueo",
				"--- Abrir Ubicacion del Archivo",
				"_",
				"--- Propiedades de la imagen",

			
			"- Vista",

				"---* Barra de Menu",
				"---* Barra de Botones",
				"---* Mostrar/Ocultar la Lista de Archivos",
				"---* Imagenes en Miniatura",
				"---* Linea de Ubicacion del Archivo",
				"_",
				"---* Fondo a Cuadros",
				"---* Mantener Ventana Siempre Encima",
				"_",
				"--- Mostrar Dialogo Lista de Imagenes",
				
				
			"- Configuracion",
			
				"--< Carga de Imagenes",
					"--{",
					"---. Mostrar Solo Carpeta Actual",
					"---. Mostrar Imagenes de Subcarpetas",
					"--}",
					"_",
					"---- Miniaturas en la Barra de Imagenes",
				"-->",
				"_",
				"--< General",
					"---* Mostrar Imagen de Bienvenida",
					"---* Abrir Ultima Imagen Vista",
					"_",
					"---* Volver a la Primera Imagen al Llegar al final de la Lista",
					"---* Mostrar Flechas de Navegacion",
				"-->",	
				"_",	
				"--< Visualizar Botones",
//					"---* Botones de Navegacion",
//					"_",
					"---* Botón Rotar Izquierda",
					"---* Botón Rotar Derecha",
					"---* Botón Espejo Horizontal",
					"---* Botón Espejo Vertical",
					"---* Botón Recortar",
					"_",   
					"---* Botón Zoom",
					"---* Botón Zoom Automatico",
					"---* Botón Ajustar al Ancho",
					"---* Botón Ajustar al Alto",
					"---* Botón Escalar para Ajustar",
					"---* Botón Zoom Fijo",
					"---* Botón Reset Zoom",
					"_",   
					"---* Botón Panel-Galeria",
					"---* Botón Grid",
					"---* Botón Pantalla Completa",
					"---* Botón Lista",
					"---* Botón Carrousel",
					"_",  
					"---* Botón Refrescar",
					"---* Botón Subcarpetas",
					"---* Botón Lista de Favoritos",
					"_",   
					"---* Botón Borrar",
					"_",   
//					"---*Selector_de_Carpetas",
					"---* Botón Menu",
					"---* Mostrar Boton de Botones Ocultos",
				"-->",
				"_",
				"--< Barra de Informacion",
					"--{",
					"---. Nombre del Archivo",
					"---. Ruta y Nombre del Archivo",
					"--}",
					"_",
					"---* Numero de Imagenes en la Carpeta Actual",
					"---* % de Zoom actual",
					"---* Tamaño del Archivo",
					"---* Fecha y Hora de la Imagen",
				"-->",
				"_",
				"--- Guardar Configuracion Actual",
				"--- Cargar Configuracion Inicial",
				
				
			"_",
			"--- Version"
			
				
		};
		
		// Crear la barra de menú
		menuBar = new JMenuBar();
		
			// Variables para rastrear los menús actuales
        JMenu currentMenu = null; // Menú principal o submenú actual
        JMenu subMenu = null;  // Submenú dentro de un submenú
		
		// Variable para el ButtonGroup actual
        ButtonGroup currentButtonGroup = null;
		
        // Procesar cada opción del array
        for (String option : menuOptions) 
        {
            if (option.startsWith("-")) 
            {
                // Determinar el nivel del menú según el prefijo
                if (option.equals("-->")) 
                {
                    // CERRAR SUBMENÚ: Reiniciar subMenu a null
                    subMenu = null;
                } else if (option.equals("--{")) {
                    // INICIO DE UN GRUPO DE RADIOBUTTON
                    currentButtonGroup = new ButtonGroup(); // Crear un nuevo ButtonGroup
                } else if (option.equals("--}")) {
                    // FIN DE UN GRUPO DE RADIOBUTTON
                    currentButtonGroup = null; // Reiniciar el ButtonGroup actual
                } else if (option.startsWith("----")) {
                    // SUB-SUBMENÚ: Nivel 4
                    String text = option.substring(4); // Eliminar los primeros cuatro caracteres ("----")
                    JMenuItem menuItem = createMenuItem(text, currentButtonGroup);
                    asignarActionCommandMenuItem(text, menuItem);
                    addMenuItemListener(menuItem); // Añadir ActionListener
                    if (subMenu != null) 
                    {
                        subMenu.add(menuItem); // Agregar al sub-submenú
                    } else if (currentMenu != null) {
                        currentMenu.add(menuItem); // Agregar al menú actual si no hay sub-submenú
                    }
                } else if (option.startsWith("---")) {
                    // SUBMENÚ: Nivel 3
                    String text = option.substring(3); // Eliminar los primeros tres caracteres ("---")
                    if (text.startsWith("*") || text.startsWith(".")) 
                    {
                        // Si es un componente (CheckBox o RadioButton)
                        JMenuItem menuItem = createMenuItem(text, currentButtonGroup);

                        // Asignar un ActionCommand y añadir al Map
                        asignarActionCommandMenuItem(text, menuItem);
                        
                        addMenuItemListener(menuItem); // Añadir ActionListener
                        if (subMenu != null) 
                        {
                            subMenu.add(menuItem); // Agregar al submenú
                        } else if (currentMenu != null) {
                            currentMenu.add(menuItem); // Agregar al menú actual
                        }
                    } else {
                        // Si es una opción normal
                        JMenuItem menuItem = new JMenuItem(text);
                        
                        // Asignar un ActionCommand y añadir al Map
                        asignarActionCommandMenuItem(text, menuItem);                        
                        
                        addMenuItemListener(menuItem); // Añadir ActionListener
                        if (subMenu != null) 
                        {
                            subMenu.add(menuItem); // Agregar al submenú
                        } else if (currentMenu != null) {
                            currentMenu.add(menuItem); // Agregar al menú actual
                        }
                    }
                } else if (option.startsWith("--<")) {
                    // SUBMENÚ QUE CONTIENE OPCIONES: Nivel 2 (--< >--)
                    String text = option.substring(3); // Eliminar los primeros tres caracteres ("--/")
                    JMenu newSubMenu = new JMenu(text); // Crear un submenú
                    
                    // Asignar un ActionCommand y añadir al Map
                    asignarActionCommandMenuItem(text, newSubMenu);
                    
                    if (currentMenu != null) 
                    {
                        currentMenu.add(newSubMenu); // Agregar al menú actual
                    } else {
                        menuBar.add(newSubMenu); // Agregar directamente a la barra de menú si no hay menú activo
                    }
                    subMenu = newSubMenu; // Actualizar el submenú actual
                } else if (option.startsWith("-")) {
                    // MENÚ PRINCIPAL: Nivel 1
                    String text = option.substring(1); // Eliminar el primer carácter ("-")
                    JMenu mainMenu = new JMenu(text); // Crear el menú principal
                    
                    // Asignar un ActionCommand y añadir al Map
                    asignarActionCommandMenuItem(text, mainMenu);
                    
                    menuBar.add(mainMenu); // Agregar el menú principal a la barra de menú
                    currentMenu = mainMenu; // Actualizar el menú actual al menú principal recién creado
                    subMenu = null; // Reiniciar el submenú porque estamos en un nuevo nivel
                }
            } else if (option.equals("_")) {
                // SEPARADOR: Agregar un separador
                if (subMenu != null) {
                    subMenu.addSeparator(); // Agregar separador al submenú
                } else if (currentMenu != null) {
                    currentMenu.addSeparator(); // Agregar separador al menú actual
                }
            }
        }

        //configuracionInicialMenu();
        
		setJMenuBar(menuBar);
	}

	/**
	 * Asignar action command menu item.
	 *
	 * @param text the text
	 * @param menuItem the menu item
	 */
	private void asignarActionCommandMenuItem(String text, JMenuItem menuItem)
	{
		// Asignar un ActionCommand y añadir al Map del menuItem
		String actionCommand = text.replace("_", "").replace("*", "").replace(".","").trim().replace(" ", "_");
	//System.out.println("-->>> FOR -> opcion del menu - :" + actionCommand);
		menuItem.setActionCommand(actionCommand);
		menuItemsPorNombre.put(actionCommand, menuItem);
	}

	
	/**
	 * Método auxiliar para crear un JMenuItem según el tipo de opción.
	 *
	 * @param text El texto de la opción (puede incluir prefijos como "*" o ".").
	 * @param buttonGroup the button group
	 * @return Un JMenuItem configurado según el tipo de opción.
	 */
    private static JMenuItem createMenuItem(String text, ButtonGroup buttonGroup) 
    {
        if (text.startsWith("*")) 
        {
            // CHECKBOX: Crear un JCheckBoxMenuItem
        	
            return new JCheckBoxMenuItem(text.substring(1)); // Eliminar el "*"
            
        } else if (text.startsWith(".")) {
        	
            // RADIOBUTTON: Crear un JRadioButtonMenuItem y agruparlo
            
        	JRadioButtonMenuItem radioButtonMenuItem = new JRadioButtonMenuItem(text.substring(1)); // Eliminar el "."
            if (buttonGroup != null) 
            {
                buttonGroup.add(radioButtonMenuItem); // Agrupar los radio buttons
            }
            return radioButtonMenuItem;
            
        } else {
            // OPCIÓN NORMAL: Crear un JMenuItem
            return new JMenuItem(text);
        }
    }
    
	
    /**
     * Enable menu item.
     *
     * @param actionCommand the action command
     * @param activar the activar
     */
    // Activa/Desactiva un menuitem del menu
    public void enableMenuItem(String actionCommand, boolean activar) 
    {
        JMenuItem menuItem = menuItemsPorNombre.get(actionCommand);
        if (menuItem != null) 
        {
            menuItem.setEnabled(activar);

//LOG activado o no menuitem
//System.out.println("Menú '" + actionCommand + "' " + (activar ? "activado" : "desactivado"));
        }
    }
    
    
    /**
     * Sets the check box menu item state.
     *
     * @param actionCommand the action command
     * @param seleccionado the seleccionado
     */
    // activa o desactiva un checkbox del menu
    public void setCheckBoxMenuItemState(String actionCommand, boolean seleccionado) 
    {
        JMenuItem menuItem = menuItemsPorNombre.get(actionCommand);
        if (menuItem instanceof JCheckBoxMenuItem) 
        {
            ((JCheckBoxMenuItem) menuItem).setSelected(seleccionado);
            
//LOG checkbox activado            
//System.out.println("Checkbox '" + actionCommand + "' " + (seleccionado ? "seleccionado" : "deseleccionado"));

        } else {
        	
        	JOptionPane.showMessageDialog (this, "El menú '" + actionCommand + "' no es un JCheckBoxMenuItem"
        			, "Error" ,JOptionPane.ERROR_MESSAGE);
        }
    }
    
 
    /**
     * Adds the menu item listener.
     *
     *	Asocia un ActionListener a un elemento del menú, generando un nombre único (actionCommand) basado en el texto del menú.
     *
     * @param menuItem the menu item
     */
    private void addMenuItemListener(JMenuItem menuItem) 
    {
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	
            	String nombreAccion = menuItem.getText().trim().replaceAll(" ", "_");
                
// LOG Imprimir el nombre del método generado en menuitem para depuración
System.out.println("El método asociado es: " + nombreAccion);
                
                // Establecer el comando de acción para identificarlo más fácilmente
                menuItem.setActionCommand(nombreAccion);
                
                // Llamar al método que procesa los eventos del menú
                metodosBarraMenu(e); // Pasar el evento original
            }
        });
    }
    

    /**
     * Método para procesar los eventos del menú.
     * 
     * @param e El ActionEvent generado por el menú.
     */
    private void metodosBarraMenu(ActionEvent e) 
    {
        String actionCommand = e.getActionCommand();

// LOG Procesar el evento según la opción seleccionada
//System.out.println("Opción seleccionada: " + actionCommand);

        // Ejemplo de lógica específica para algunas opciones
        switch (actionCommand) 
        {
	        //"- Archivo",
	
	        case "Abrir_Archivo" -> abrirSelectorDeCarpeta();														//"Abrir Archivo",
			case "Abrir_en_ventana_nueva" -> System.out.println("Abrir en ventana nueva");							//"Abrir en ventana nueva",
	        case "Guardar" -> System.out.println("Guardar");														//"Guardar",
			case "Guardar_Como" -> System.out.println("Guardar Como");												//"Guardar Como",
			case "Abrir_Con" -> System.out.println("Abrir Con");													//"Abrir Con...",
			case "Editar_Imagen" -> System.out.println("Editar Imagen");											//"Editar Imagen",
			case "Imprimir" -> System.out.println("Imprimir");														//"Imprimir",
			case "Compartir" -> System.out.println("Compartir");													//"Compartir",
			case "Refrescar_Imagen" -> System.out.println("Refrescar Imagen");										//"Refrescar Imagen",
			case "Volver_a_Cargar" -> System.out.println("Volver a Cargar");										//"Volver a Cargar",
			case "Recargar_Lista_de_Imagenes" -> System.out.println("Recargar Lista de Imagenes");					//"Recargar Lista de Imagenes",
			case "Unload_Imagen" -> System.out.println("Unload Imagen");											//"Unload Imagen",
	
			
			//"Navegacion",
			
			case "Imagen_Aterior" ->  System.out.println("Imagen Aterior");											//"Imagen Aterior",
			case "Imagen_Siguiente" ->  System.out.println("Imagen Siguiente");										//"Imagen Siguiente",
			case "Ir_a" ->  System.out.println("Ir a...");															//"Ir a...",
			case "Primera_Imagen" ->  System.out.println("Primera Imagen");											//"Primera Imagen",
			case "Ultima_Imagen" ->  System.out.println("Ultima Imagen");											//"Ultima Imagen",
			case "Anterior_Fotograma" ->  System.out.println("Anterior Fotograma");									//"Anterior Fotograma",
			case "Siguiente_Fotograma" ->  System.out.println("Siguiente Fotograma");								//"Siguiente Fotograma",
			case "Primer_Fotograma" ->  System.out.println("Primer Fotograma");										//"Primer Fotograma",
			case "Ultimo_Fotograma" ->  System.out.println("Ultimo Fotograma");										//"Ultimo Fotograma",
	
	
			//"Zoom",
	
			case "Acercar"->  System.out.println("Acercar");														//"Acercar",
			case "Alejar"->  System.out.println("Alejar");															//"Alejar",
			case "Zoom_Personalizado_%"->  System.out.println("Zoom Personalizado %");								//"Zoom Personalizado %",
			case "Zoom_Tamaño_Real"->  System.out.println("Zoom Tamaño Real");										//"Zoom Tamaño Real",
	        case "Resetear_Zoom"-> aplicarResetZoom();																//"Resetear Zoom",
	        
			case "Mantener_Proporciones"-> System.out.println("Mantener Proporciones");								//"---* Mantener Proporciones",
	        case "Activar_Zoom_Manual" -> toggleZoom(!zoomHabilitado);												//"---* Activar Zoom Manual"
	        
			//"--< Tipos de Zoom",
	
			case  "Zoom_Automatico"->  System.out.println("Zoom Automatico");										//	"---. Zoom Automatico",
			case  "Zoom_a_lo_Ancho"->  System.out.println("Zoom a lo Ancho");										//	"---. Zoom a lo Ancho",//-.
			case  "Zoom_a_lo_Alto"->  System.out.println("Zoom a lo Alto");											//	"---. Zoom a lo Alto",//-.
			case  "Escalar_Para_Ajustar"->  System.out.println("Escalar Para Ajustar");								//	"---. Escalar Para Ajustar",//-.
			case  "Zoom_Actual_Fijo"->  System.out.println("Zoom Actual Fijo");										//	"---. Zoom Actual Fijo",//-.
			case  "Zoom_Especificado"->  System.out.println("Zoom Especificado");									//	"---. Zoom Especificado",//-.
			case  "Escalar_Para_Rellenar"->  System.out.println("Escalar Para Rellenar");							//	"---. Escalar Para Rellenar",//-.
	
	        		
			//"- Imagen",
	
	//		"--< Carga y Orden",
			case "Nombre_por_Defecto" -> System.out.println("Nombre por Defecto");									//		"----. Nombre por Defecto",
			case "Tamaño_de_Archivo"-> System.out.println("Tamaño de Archivo");										//		"----. Tamaño de Archivo",
			case "Fecha_de_Creacion"-> System.out.println("Fecha de Creacion");										//		"----. Fecha de Creacion",
			case "Extension"-> System.out.println("Extension");														//		"----. Extension",
	        case "Sin_Ordenar" -> System.out.println("Sin ordenar...");												//"----. Sin Ordenar",
	        case "Ascendente" -> System.out.println("Ordenando en orden ascendente...");							//"----. Ascendente",
	        case "Descendente" -> System.out.println("Ordenando en orden descendente...");							//"----. Descendente",
	
	        
			case "Edicion" ->  System.out.println("Edicion");														//"--< Edicion",
			case "Girar_Izquierda" ->  System.out.println("Girar Izquierda");										//"---- Girar Izquierda",
			case "Girar_Derecha" ->  System.out.println("Girar Derecha");											//"---- Girar Derecha",
			case "Voltear_Horizontal" ->  System.out.println("Voltear Horizontal");									//"---- Voltear Horizontal",
			case "Voltear_Vertical" ->  System.out.println("Voltear Vertical");										//"---- Voltear Vertical",
			case "Cambiar_Nombre_de_la_Imagen" ->  System.out.println("Cambiar Nombre de la Imagen");				//"--- Cambiar Nombre de la Imagen",
			case "Mover_a_la_Papelera" ->  System.out.println("Mover a la Papelera");								//"--- Mover a la Papelera",
			case "Eliminar_Permanentemente" ->  System.out.println("Eliminar Permanentemente");						//"--- Eliminar Permanentemente",
			case "Establecer_Como_Fondo_de_Escritorio" ->  System.out.println("Establecer Como Fondo de Escritorio");//"--- Establecer Como Fondo de Escritorio",
			case "Establecer_Como_Imagen_de_Bloqueo" ->  System.out.println("Establecer Como Imagen de Bloqueo");	//"--- Establecer Como Imagen de Bloqueo",
			case "Abrir_Ubicacion_del_Archivo" ->  System.out.println("Abrir Ubicacion del Archivo");				//"--- Abrir Ubicacion del Archivo",
			case "Propiedades_de_la_imagen" ->  System.out.println("Propiedades de la imagen");						//"--- Propiedades de la imagen",
			
			
			//"Vista",
	
			case "Barra_de_Menu" -> System.out.println("Barra de Menu");											//"---* Barra de Menu",
			case "Barra_de_Botones" -> System.out.println("Barra de Botones");										//"---* Barra de Botones",
			case "Mostrar/Ocultar_la_Lista_de_Archivos" -> System.out.println("Mostrar/Ocultar la Lista de Archivos");//"---* Mostrar/Ocultar la Lista de Archivos",
			case "Imagenes_en_Miniatura" -> System.out.println("Imagenes en Miniatura");							//"---* Imagenes en Miniatura",
			case "Linea_de_Ubicacion_del_Archivo" -> System.out.println("Linea de Ubicacion del Archivo");			//"---* Linea de Ubicacion del Archivo",
			case "Fondo_a_Cuadros" -> System.out.println("Fondo a Cuadros");										//"---* Fondo a Cuadros",
			case "Mantener_Ventana_Siempre_Encima" -> System.out.println("Mantener Ventana Siempre Encima");		//"---* Mantener Ventana Siempre Encima",
	        case "Mostrar_Dialogo_Lista_de_Imagenes" -> mostrarDialogoListaImagenes(); 								//"--- Mostrar Dialogo Lista de Imagenes",
				
	        
			//"Configuracion",
			
	//			"--< Carga de Imagenes",
	       	case "Mostrar_Solo_Carpeta_Actual" -> alternarModoCarga(true);											//"---. Mostrar Solo Carpeta Actual",
	        case "Mostrar_Imagenes_de_Subcarpetas" -> alternarModoCarga(false);										//"---. Mostrar Imagenes de Subcarpetas",
			case "Miniaturas_en_la_Barra_de_Imagenes" ->  System.out.println("Miniaturas en la Barra de Imagenes");	//"Miniaturas en la Barra de Imagenes",
			
			
			//"--< General",
			
			case "Mostrar_Imagen_de_Bienvenida" ->  System.out.println("Mostrar Imagen de Bienvenida");				//"---* Mostrar Imagen de Bienvenida",
			case "Abrir_Ultima_Imagen_Vista" ->  System.out.println("Abrir Ultima Imagen Vista");					//"---* Abrir Ultima Imagen Vista",
			case "Volver_a_la_Primera_Imagen_al_Llegar_al_final_de_la_Lista" ->  System.out.println("Volver a la Primera Imagen al Llegar al final de la Lista");//"---* Volver a la Primera Imagen al Llegar al final de la Lista",
			case "Mostrar_Flechas_de_Navegacion" ->  System.out.println("Mostrar Flechas de Navegacion");			//"---* Mostrar Flechas de Navegacion",
	
	//		//"--< Visualizar Botones",
			case "Botón_Rotar_Izquierda" ->  System.out.println("Botón Rotar Izquierda");							//"---* Rotar Izquierda",
			case "Botón_Rotar_Derecha" ->  System.out.println("Botón Rotar Derecha");								//"---* Rotar Derecha",
			case "Botón_Espejo_Horizontal" ->  System.out.println("Botón Espejo Horizontal");						//"---* Espejo Horizontal",
			case "Botón_Espejo_Vertical" ->  System.out.println("Botón Espejo Vertical");							//"---* Espejo Vertical",
			case "Botón_Recortar" ->  System.out.println("Botón Recortar");											//"---* Recortar",
			case "Botón_Zoom" ->  System.out.println("Botón Zoom");													//"---* Zoom",
			case "Botón_Zoom_Automatico" ->  System.out.println("Botón Zoom Automatico");							//"---* Zoom Automatico",
			case "Botón_Ajustar_al_Ancho" ->  System.out.println("Botón Ajustar al Ancho");							//"---* Ajustar al Ancho",
			case "Botón_Ajustar_al_Alto" ->  System.out.println("Botón Ajustar al Alto");							//"---* Ajustar al Alto",
			case "Botón_Escalar_para_Ajustar" ->  System.out.println("Botón Escalar para Ajustar");					//"---* Escalar para Ajustar",
			case "Botón_Zoom_Fijo" ->  System.out.println("Botón Zoom Fijo");										//"---* Zoom Fijo",
			case "Botón_Reset_Zoom" ->  System.out.println("Botón Reset Zoom");										//"---* Reset Zoom",
			case "Botón_Panel-Galeria" ->  System.out.println("Botón Panel-Galeria");								//"---* Panel-Galeria",
			case "Botón_Grid" ->  System.out.println("Botón Grid");													//"---* Grid",
			case "Botón_Pantalla_Completa" ->  System.out.println("Botón Pantalla Completa");						//"---* Pantalla Completa",
			case "Botón_Lista" ->  System.out.println("Botón Lista");												//"---* Lista",
			case "Botón_Carrousel" ->  System.out.println("Botón Carrousel");										//"---* Carrousel",
			case "Botón_Refrescar" ->  System.out.println("Botón Refrescar");										//"---* Refrescar",
			case "Botón_Subcarpetas" ->  System.out.println("Botón Subcarpetas");									//"---* Subcarpetas",
			case "Botón_Lista_de_Favoritos" ->  System.out.println("Botón Lista de Favoritos");						//"---* Lista_de_Favoritos",
			case "Botón_Borrar" ->  System.out.println("Botón Borrar");												//"---* Borrar",
			case "Botón_Menu" ->  System.out.println("Botón Menu");													//"---* Menu",
			case "Mostrar_Boton_de_Botones_Ocultos" ->  System.out.println("Mostrar Boton de Botones Ocultos");		//"---* Mostrar Boton de Botones Ocultos",
			
	//		"--< Barra de Informacion",
			case "Nombre_del_Archivo" ->  System.out.println("Nombre del Archivo");									//"---. Nombre del Archivo",
			case "Ruta_y_Nombre_del_Archivo" ->  System.out.println("Ruta y Nombre del Archivo");					//"---. Ruta y Nombre del Archivo",
			case "Numero_de_Imagenes_en_la_Carpeta_Actual" ->  System.out.println("Numero de Imagenes en la Carpeta Actual");//"---* Numero de Imagenes en la Carpeta Actual",
			case "%_de_Zoom_actual" ->  System.out.println("% de Zoom actual");										//"---* % de Zoom actual",
			case "Tamaño_del_Archivo" ->  System.out.println("Tamaño del Archivo");									//"---* Tamaño del Archivo",
			case "Fecha_Hora_de_la_Imagen" ->  System.out.println("Fecha y Hora de la Imagen");						//"---* Fecha y Hora de la Imagen",
	    	case "Guardar_Configuracion_Actual" -> guardarConfiguracionActual();									//"--- Guardar Configuracion Actual", 
			case "Cargar_Configuracion_Inicial" -> System.out.println("Cargar Configuracion Inicial");				//	"--- Cargar Configuracion Inicial",
			case "Version" -> System.out.println("Version");														//"Version"
        
            default -> System.out.println("Acción no implementada para: " + actionCommand);
        }
    }
  
    
	/**
	 * Inicializar etiqueta mostrar imagen.
	 */
	private void inicializarEtiquetaMostrarImagen()
	{
		etiquetaImagen = new JLabel()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);

				if (imagenReescalada != null)
				{
					Graphics2D g2d = (Graphics2D) g.create();
					int scaledWidth = (int) (imagenReescalada.getWidth(null) * zoomFactor);
					int scaledHeight = (int) (imagenReescalada.getHeight(null) * zoomFactor);
					int x = (getWidth() - scaledWidth) / 2 + imageOffsetX;
					int y = (getHeight() - scaledHeight) / 2 + imageOffsetY;
					g2d.translate(x, y);
					g2d.scale(zoomFactor, zoomFactor);
					g2d.drawImage(imagenReescalada, 0, 0, null);
					g2d.dispose();
				}
			}
		};
		
		etiquetaImagen.setHorizontalAlignment(SwingConstants.CENTER);
		etiquetaImagen.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		etiquetaImagen.setVerticalAlignment(SwingConstants.CENTER);
	}


// ***************************************************************************************************** EVENTOS  
	
	
	
	
	
	
// ***************************************************************************************************** EVENTOS INTERNOS	
	
		


		


// ***************************************************************************************************** IMAGENES EN MINIATURA
		

	// ***** FUNCIONALIDAD PARA CARGAR IMÁGENES EN SEGUNDO PLANO *****

	/**
 * Carga las miniaturas para el rango visible alrededor del índice seleccionado.
 * Cancela cualquier carga de miniaturas anterior.
 * Ejecuta la carga de imágenes y reescalado en segundo plano.
 *
 * @param indiceSeleccionado the indice seleccionado
 */
	private void actualizarImagenesMiniaturaConRango(int indiceSeleccionado) 
	{
	
	    // --- INICIO: Cancelar tarea MINIATURAS anterior ---
	    if (cargaMiniaturasFuture != null && !cargaMiniaturasFuture.isDone()) 
	    {
	    	System.out.println("Intentando cancelar tarea de MINIATURAS anterior...");
	    	boolean cancelled = cargaMiniaturasFuture.cancel(true);
			System.out.println("Tarea de MINIATURAS anterior cancelada: " + cancelled);
	    }
	    // --- FIN: Cancelar tarea MINIATURAS anterior ---
	
//String metodoLlamador = Thread.currentThread().getStackTrace()[2].getMethodName();
//System.out.println("El método actualizarImagenesMiniaturaConRango fue llamado desde: " + metodoLlamador);
	
	    // Limpieza inmediata del panel en EDT
	    SwingUtilities.invokeLater(() -> {
	        panelImagenesMiniatura.removeAll();
	        panelImagenesMiniatura.validate(); // Usar validate() es a veces preferible a revalidate() aquí
	        panelImagenesMiniatura.repaint();
	        // System.out.println("Panel de miniaturas limpiado.");
	    });
	
	    int totalImagenes = modeloLista.getSize();
	    if (totalImagenes == 0) 
	    {
	    	JOptionPane.showMessageDialog (this, "No hay imágenes en la lista, no se cargan miniaturas.", 
	    			"Error", JOptionPane.ERROR_MESSAGE);
	        return;
	    }
	
	    int inicio = Math.max(0, indiceSeleccionado - miniaturasAntes );
	    int fin = Math.min(totalImagenes - 1, indiceSeleccionado + miniaturasDespues );
//LOG rango de miniaturas inicio y fin   
System.out.println("Rango Miniaturas: inicio = " + inicio + ", fin = " + fin + " (índice sel: " + indiceSeleccionado + ")");
	
	    // Iniciar nueva tarea y guardar su Future
	    cargaMiniaturasFuture = executorService.submit(() -> {
	        List<JLabel> miniaturasParaAgregar = new ArrayList<>(); // Lista temporal
	        
//long taskMiniStartTime = System.currentTimeMillis();
//LOG Tarea background (miniaturas) iniciada
//System.out.println("-->>> Tarea background (miniaturas) iniciada @ " + taskMiniStartTime);
	        
	        try 
	        { // Envolver bucle en try para capturar interrupciones/errores
	            for (int i = inicio; i <= fin; i++) 
	            {
	                // Chequeo de interrupción al inicio de cada iteración
	                if (Thread.currentThread().isInterrupted()) 
	                {
	                    System.out.println("Carga de miniaturas cancelada en bucle (índice " + i + ").");
	                    return; // Salir de la tarea
	                }
	
	                String uniqueKey = modeloLista.getElementAt(i); // Obtener la clave (ruta relativa)
	                Path ruta = rutaCompletaMap.get(uniqueKey);
	
	                if (ruta == null) 
	                {
	                	JOptionPane.showMessageDialog(this, "WARN: No se encontró ruta para la clave (miniatura): " + 
	                			uniqueKey + " ", "Error", JOptionPane.ERROR_MESSAGE);
//System.err.println("WARN: No se encontró ruta para la clave (miniatura): " + uniqueKey);
	                     
// --------------------------------------------------------------------------------------------------------------- placeholder

	                	//FIXME implementar el placeholder, este no funciona
	                     // Crea un placeholder visual para miniaturas no encontradas
	                	
//LOG añadiendo un placeholder            	
//System.out.println("-*>añadiendo un placeholder");
	                	
	                	// --- Placeholder Mejorado ---
//	                    System.out.println("-*> Añadiendo placeholder por ruta nula para key: " + uniqueKey);
	                    JLabel placeholder = new JLabel(); // Sin texto
	                    placeholder.setOpaque(true);
	                    placeholder.setBackground(Color.BLACK); // Fondo negro
	                    // Establecer tamaño igual a miniatura normal
	                    placeholder.setPreferredSize(new Dimension(miniaturaNormAncho, miniaturaNormAlto));
	                    placeholder.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY)); // Borde sutil opcional
	                    placeholder.setToolTipText("Error: No se encontró la ruta para " + uniqueKey);
	                    miniaturasParaAgregar.add(placeholder);
	                    // --- Fin Placeholder ---
	                	
	                	
//	                      JLabel placeholder = new JLabel("Error");
//	                      placeholder.setOpaque(true); placeholder.setBackground(Color.RED); //...
//	                      miniaturasParaAgregar.add(placeholder);
	                      
// ---------------------------------------------------------------------------------------------------------------                       
	                     continue;
	                }
	
	                try {
	                    final boolean esSeleccionada = (i == indiceSeleccionado);
	                    final Path rutaFinal = ruta; // Necesario para listener
	                    final int indiceActual = i;  // Necesario para listener
	                    
	                 // --- Obtener/Crear la versión PEQUEÑA del ImageIcon ---
	                    ImageIcon miniaturaPequena = miniaturasMap.get(uniqueKey);
	                    Image imagenOriginal = null; // Para reescalar si es necesario
	                    
	                    if (miniaturaPequena == null) {
	                        BufferedImage bufferedImg = ImageIO.read(ruta.toFile());
	                        if (bufferedImg == null) { /*...*/ continue; }
	                        
	                        imagenOriginal = bufferedImg; // Guardamos referencia por si hay que escalar a grande
	
	                        // Escalar SIEMPRE a tamaño PEQUEÑO para el caché
	                        Image escaladaPequena = imagenOriginal.getScaledInstance(
	                            miniaturaNormAncho, 
	                            miniaturaNormAlto, 
	                            Image.SCALE_SMOOTH);
	                            
	                        miniaturaPequena = new ImageIcon(escaladaPequena);
	                        miniaturasMap.put(uniqueKey, miniaturaPequena); // Cachear la versión pequeña
	                    }
	                    // --- Fin obtener/crear versión PEQUEÑA ---
	                    
	                 // --- Determinar el ImageIcon FINAL a mostrar ---
	                    ImageIcon iconoAMostrar;
	                    if (esSeleccionada) 
	                    {
	                        // Reescalar al vuelo a tamaño GRANDE desde la imagen de la miniatura pequeña
	                        if (imagenOriginal == null) // Si la pequeña vino del caché, obtener su Image 
	                        { 
	                             imagenOriginal = miniaturaPequena.getImage();
	                        }
	                         Image escaladaGrande = imagenOriginal.getScaledInstance(
	                            miniaturaSelAncho, 
	                            miniaturaSelAlto, 
	                            Image.SCALE_SMOOTH);
	                        iconoAMostrar = new ImageIcon(escaladaGrande); // Crear icono grande temporalmente
//LOG Miniatura GRANDE generada al vuelo para 	                        
//System.out.println("->Miniatura GRANDE generada al vuelo para: " + uniqueKey);
	                    } else {
	                        // Usar directamente la versión pequeña (del caché o recién creada)
	                        iconoAMostrar = miniaturaPequena;
	                    }
	                    // --- Fin determinar ImageIcon FINAL ---
	
	                 // Crear JLabel con el icono determinado
	                    JLabel etiqueta = new JLabel(iconoAMostrar); // <--- Usa iconoAMostrar
	                    etiqueta.setOpaque(true);
	                    etiqueta.setToolTipText(rutaFinal.getFileName().toString());
	                    
	                    
	                 // Estilo y Listener
	                    if (esSeleccionada) {
	                        etiqueta.setBorder(BorderFactory.createLineBorder(Color.BLUE, 3));
	                        etiqueta.setBackground(new Color(200, 200, 255));
	                    } else {
	                        etiqueta.setBorder(null);
	                        etiqueta.setBackground(null); // Fondo transparente si no está seleccionada
	                    }
	                    etiqueta.addMouseListener(new MouseAdapter() {
	                        @Override
	                        public void mouseClicked(MouseEvent e) {
	                             if (estaCargandoLista) { // Prevenir clics durante carga principal
//LOG Clic en miniatura ignorado durante carga de lista 	                            	 
//System.out.println("Clic en miniatura ignorado durante carga de lista.");
	                                 return;
	                             }
	                             
	                            listaImagenes.setSelectedIndex(indiceActual);
	                        }
	                    });
	
	                    miniaturasParaAgregar.add(etiqueta); // Añadir a lista temporal
	
	                } catch (IOException e) {
	                    if (!Thread.currentThread().isInterrupted()) 
	                    {
	                    	JOptionPane.showMessageDialog(this, "Error IO cargando miniatura: " + ruta + " - " + e.getMessage(), 
	            					"Error", JOptionPane.ERROR_MESSAGE);
	                         // Opcional: Añadir placeholder
	                    } else {
	                    	JOptionPane.showMessageDialog(this, "IOException en miniatura ignorada por cancelación." 
	                    			+ e.getMessage(),"Error", JOptionPane.ERROR_MESSAGE);
	                    }
	                } catch (Exception ex) { // Capturar otros errores inesperados por imagen
	                     if (!Thread.currentThread().isInterrupted()) 
	                     {
	                    	 JOptionPane.showMessageDialog(this, "Error IO cargando miniatura: " + ruta + " - " + ex.getMessage(), 
		            					"Error", JOptionPane.ERROR_MESSAGE);
	                    	 
	                          ex.printStackTrace();
	                          // Opcional: Añadir placeholder
	                     }
	                }
	            }
	
	            // Actualizar UI solo si no fue interrumpido
	            if (!Thread.currentThread().isInterrupted()) 
	            {
	                SwingUtilities.invokeLater(() -> {

//LOG AÑADIR IMAGEN EN MINIATURA AL PANEL	                	
//System.out.println("Añadiendo " + miniaturasParaAgregar.size() + " miniaturas al panel.");
	                    
	                    for (JLabel miniatura : miniaturasParaAgregar) 
	                    {
	                        panelImagenesMiniatura.add(miniatura);
	                    }
	                    
	                    panelImagenesMiniatura.validate(); // Asegurar que el layout se recalcule
	                    panelImagenesMiniatura.repaint();
	                });
	            }
	
	        } catch (Exception e) { // Capturar cualquier error a nivel de tarea (ej. OutOfMemory)
	             if (!Thread.currentThread().isInterrupted()) 
	             {
	            	 
	            	 JOptionPane.showMessageDialog(this, "Error grave en tarea de miniaturas: " + e.getMessage() 
         					,"Error", JOptionPane.ERROR_MESSAGE);
	                  e.printStackTrace();
	             }
	        } finally {

//long taskMiniEndTime = System.currentTimeMillis();            
//LOG tiempo de carga de imagenes en segundo plano 	            
//System.out.println("-->>> Tarea background (miniaturas) terminada @ " + taskMiniEndTime);
//System.out.println("-->>> Duración TOTAL Tarea background (miniaturas): " + (taskMiniEndTime - taskMiniStartTime) + " ms");
//System.out.println("-->>>Tarea de carga de miniaturas terminada (o cancelada).");
	        }
	    }); // Fin submit miniaturas
	}

	
//	****************************************************************************** metodo nunca usado	
	/**
	 * Aplicar animacion imagen en miniatura.
	 *
	 * @param etiquetaMiniatura the etiqueta miniatura
	 */
	private void aplicarAnimacionMiniatura(JLabel etiquetaMiniatura) 
	{
	    Color colorOriginal = etiquetaMiniatura.getBackground();
	    etiquetaMiniatura.setBackground(new Color(173, 216, 230)); // Azul claro
	
	    Timer timer = new Timer(500, e -> etiquetaMiniatura.setBackground(colorOriginal));
	    timer.setRepeats(false);
	    timer.start();
	}


//	****************************************************************************** metodo nunca usado	
	/**
	 * Cargar imagen desde miniatura.
	 *
	 * @param ruta the ruta
	 */
	// ***** MÉTODO PARA CARGAR UNA IMAGEN DESDE UNA MINIATURA *****
	private void cargarImagenDesdeMiniatura(Path ruta)
	{
		try
		{
			BufferedImage imagen = ImageIO.read(ruta.toFile());
			currentImage = imagen;
			reescalarImagenParaAjustar();
			textoRuta.setText(ruta.toString());
			etiquetaImagen.repaint();
		} catch (IOException ex){
			
			JOptionPane.showMessageDialog(this, "Error al cargar la imagen: " + ex.getMessage(), 
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	
//	****************************************************************************** metodo nunca usado	
	/**
	 * Obtener indice por nombre.
	 *
	 * @param nombreArchivo the nombre archivo
	 * @return the int
	 */
	private int obtenerIndicePorNombre(String nombreArchivo)
	{
		for (int i = 0; i < modeloLista.getSize(); i++)
		{
			if (modeloLista.getElementAt(i).equals(nombreArchivo))
			{
				return i;
			}
		}
		return -1; // No encontrado
	}


//	****************************************************************************** metodo nunca usado	
	/**
	* Load image as thumbnail.
	*
	* @param file the file
	* @param width the width
	* @param height the height
	* @return the buffered image
	*/
	private BufferedImage loadImageAsThumbnail(File file, int width, int height)
	{
		try
		{
			// Leer la imagen original
			BufferedImage originalImage = ImageIO.read(file);
	
			if (originalImage == null)
			{
				System.err.println("La imagen no es válida: " + file.getName());
				return null;
			}
	
			// Crear una miniatura redimensionada
			BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = thumbnail.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2d.drawImage(originalImage, 0, 0, width, height, null);
			g2d.dispose();
	
			return thumbnail;
		} catch (IOException e){
			JOptionPane.showMessageDialog(this, "Error al cargar la miniatura: " + file.getName(), 
					"Error", JOptionPane.ERROR_MESSAGE);
//			System.err.println("Error al cargar la miniatura: " + file.getName());
			return null;
		}
	
	}
	
	
	/**
	 * Mostrar imagen seleccionada.
	 */
	// ***** MOSTRAR IMAGEN SELECCIONADA *****
	private void mostrarImagenSeleccionada()
	{
		String archivoSeleccionado = listaImagenes.getSelectedValue();

		if (archivoSeleccionado != null)
		{
			
System.out.println("\nSeleccionado en lista: '" + archivoSeleccionado + "'");
			Path rutaCompleta = rutaCompletaMap.get(archivoSeleccionado);
System.out.println("Ruta encontrada en map: " + rutaCompleta +"\n"); // Verifica si es null
			

			if (rutaCompleta != null)
			{
				textoRuta.setText(rutaCompleta.toString());

				try
				{
					currentImage = ImageIO.read(rutaCompleta.toFile());
					
					if (!zoomHabilitado) 
					{ // SOLO resetea si el zoom NO está activo
						
						aplicarResetZoom();
				    
				        // Actualizar estado de botones/menús relacionados si es necesario
//				        	botonesPorNombre.get("Reset_48x48").setEnabled(false);
//				        	menuItemsPorNombre.get("Resetear_Zoom").setEnabled(false);
				        	// menuItemZoom.setSelected(false); // Ya debería estar deseleccionado
				        
				    } // Si zoomHabilitado es true, NO hacemos nada con zoomFactor/Offset aquí
					
					
					reescalarImagenParaAjustar();
					etiquetaImagen.repaint();

					// Actualizar el título o lo que sea necesario
					
//					menuItemResetZoom.setEnabled(false);
//					menuItemZoom.setSelected(false); // Desactivar el check de Zoom
					zoomHabilitado = false;

				} catch (IOException e){
					
					etiquetaImagen.setIcon(null);
					currentImage = null;
					
					imagenReescalada = null; // Importante limpiar la imagen reescalada
					
//				    zoomFactor = 1.0;
//				    imageOffsetX = 0;
//				    imageOffsetY = 0;
					aplicarResetZoom();
					
				    
				    zoomHabilitado = false; // Desactivar zoom si la imagen falla
				    
				    toggleZoom(false); // Actualiza UI del botón/menú zoom
				    
				    textoRuta.setText("Error al cargar: " + rutaCompleta.getFileName());
				    etiquetaImagen.repaint();
				    
					JOptionPane.showMessageDialog(this, "Error al cargar la imagen: " + e.getMessage(), 
							"Error", JOptionPane.ERROR_MESSAGE);
				}

			} else{
				
				etiquetaImagen.setIcon(null);
				textoRuta.setText("");
				currentImage = null;
			}

		} else{
			
			etiquetaImagen.setIcon(null);
			textoRuta.setText("");
			currentImage = null;
		}

	}

	/**
	 * Reescalar imagen para ajustar.
	 */
	// ***** REESCALAR IMAGEN PARA AJUSTAR AL TAMAÑO DE LA ETIQUETA *****
	private void reescalarImagenParaAjustar()
	{
		if (currentImage != null)
		{
			int width = etiquetaImagen.getWidth();
			int height = etiquetaImagen.getHeight();

			if (width > 0 && height > 0)
			{
				double imageAspectRatio = (double) currentImage.getWidth() / currentImage.getHeight();
				int newWidth = width;
				int newHeight = height;

				if ((double) width / height > imageAspectRatio)
				{
					newWidth = (int) (height * imageAspectRatio);
				} else{
					
					newHeight = (int) (width / imageAspectRatio);
				}

				imagenReescalada = currentImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
			}
		}
	}

	
	// FIXME que hacer con este metodo obligatorio
	/**
	 * Action performed.
	 *
	 * @param e the e
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{

	

	}

// ************************************************************************************************ ACCIONES DE BOTONES	
	
	/**
 * Toggle zoom.
 *
 *	Activa o desactiva el zoom en la imagen. Cambia el color del botón asociado al estado del zoom.	
 *
 * @param activar the activar
 */
	public void toggleZoom(boolean activar)
	{
		// Actualizar el estado del zoom y el color del boton de zoom
		zoomHabilitado = activar;
		
		// Habilitar/deshabilitar el fondo del botón
		toggleFondoBoton(activar, colorOriginalFondoBoton);
		
		menuItemsPorNombre.get("Activar_Zoom_Manual").setSelected(activar);
		botonesPorNombre.get("Reset_48x48").setEnabled(activar); 
		menuItemsPorNombre.get("Resetear_Zoom").setEnabled(activar);

		// Si se desactiva el zoom, restablecer los valores predeterminados
		if (activar)
		{
			aplicarResetZoom();
			
		}

		// Opcional: Mostrar un mensaje en la consola para depuración
//LOG zoom activado o no		
//System.out.println("Zoom " + (activar ? "activado" : "desactivado"));
	}
	
	/**
	 * Toggle fondo boton.
	 *
	 * @param activo the activo
	 * @param colorBtnZoom the color btn zoom
	 */
	// color del fondo del boton cuando esta o no activo
	private void toggleFondoBoton(boolean activo, Color colorBtnZoom)
	{
		if (activo)
		{
			botonesPorNombre.get("Zoom_48x48").setBackground(colorFondoBotonActivado);
		} else{
			botonesPorNombre.get("Zoom_48x48").setBackground(colorBtnZoom);
		}
	}
	
	/**
	 * Aplicar reset zoom.
	 * 
	 * Restablece el zoom a su estado inicial (factor de escala 1.0).
	 * 
	 */
	// Metodo para aplicar el reset del zoom
	private void aplicarResetZoom()
	{
		zoomFactor = 1.0;
		imageOffsetX = 0;
		imageOffsetY = 0;
		
		etiquetaImagen.repaint();
	}
	

// ******************************************************************************************** ACCIONES COMUNES	
	
	
/**
 * Relacion boton menu item.
 *
 * @param boton the boton
 * @param itemMenu the item menu
 */
//	****************************************************************************** metodo en proceso	

	private void relacionBotonMenuItem (String boton, String itemMenu)
	{
		
		//TODO si accionamos un boton se activa en el menu y viceversa
		
		if (boton != null) {
			
			if (boton.equals("Zoom_48x48")) itemMenu="Activar_Zoom_Manual";
			if (boton.equals("Reset_48x48")) itemMenu ="Resetear_Zoom";
			
			if (boton.equals("Zoom_Auto_48x48")) itemMenu="";
			
			if (boton.equals("Ajustar_al_Ancho_48x48")) itemMenu="";
			if (boton.equals("Ajustar_al_Alto_48x48")) itemMenu="";
			if (boton.equals("Escalar_Para_Ajustar_48x48")) itemMenu="";
			if (boton.equals("Zoom_Fijo_48x48")) itemMenu="";
			
			if (boton.equals("Panel-Galeria_48x48")) itemMenu="";
			if (boton.equals("")) itemMenu="";
			
		}
		
	}
	
	
	/**
	 * Abrir selector de carpeta.
	 *
	 *	Abre un selector de carpetas para que el usuario elija una carpeta. 
	 *  Actualiza la lista de imágenes con las imágenes de la carpeta seleccionada.
	 *
	 * @param config the config
	 */
	private void abrirSelectorDeCarpeta() //(Map<String, String> config)
	{
		if (configuration == null) 
		{
			System.err.println("Error: ConfigurationManager nulo en abrirSelectorDeCarpeta.");
	         JOptionPane.showMessageDialog(this, "Error interno de configuración.",
	                                         "Error", JOptionPane.ERROR_MESSAGE);
			return; // Seguridad
		}
		
	    JFileChooser fileChooser = new JFileChooser();
	    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    
	    // Establecer la carpeta inicial desde el archivo de configuración
	    String folderInit = configuration.getString("inicio.carpeta", "");
	    
	    if (!folderInit.isEmpty())
	    {
	        File carpetaInicial = new File(folderInit);
	        if (carpetaInicial.exists() && carpetaInicial.isDirectory())
	        {
	            fileChooser.setCurrentDirectory(carpetaInicial);
	        }
	    }
	    
	    int resultado = fileChooser.showOpenDialog(this);
	    if (resultado == JFileChooser.APPROVE_OPTION)
	    {
	        File carpetaSeleccionada = fileChooser.getSelectedFile();
	        if (carpetaSeleccionada != null)
	        {
	            String rutaCarpeta = carpetaSeleccionada.getAbsolutePath();
//System.out.println("Carpeta seleccionada: " + rutaCarpeta);	            
	            
				configuration.setInicioCarpeta(rutaCarpeta);
	            
//System.out.println("Llamando a cargarListaImagenes() después de actualizar config en memoria.");
	            cargarListaImagenes();
	        }
	    }
	}

	/**
	 * Mostrar dialogo lista imagenes.
	 * 
	 * Muestra un diálogo emergente con la lista completa de imágenes cargadas. Permite copiar la lista al portapapeles.
	 */
	private void mostrarDialogoListaImagenes()
	{
		JDialog dialogoLista = new JDialog(this, "Lista de Imágenes", false);
		dialogoLista.setSize(800, 600);
		dialogoLista.setLocationRelativeTo(this);

		DefaultListModel<String> modeloListaDialogo = new DefaultListModel<>();
		JList<String> listaImagenesDialogo = new JList<>(modeloListaDialogo);
		JScrollPane scrollPaneListaDialogo = new JScrollPane(listaImagenesDialogo);
		JCheckBox checkBoxMostrarRutas = new JCheckBox("Mostrar Rutas");

		JButton botonCopiarLista = new JButton("Copiar Lista");
		botonCopiarLista.addActionListener(e -> copiarListaAlPortapapeles(modeloListaDialogo));

		JPanel panelSuperiorDialog = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panelSuperiorDialog.add(botonCopiarLista);
		panelSuperiorDialog.add(checkBoxMostrarRutas);

		dialogoLista.add(panelSuperiorDialog, BorderLayout.NORTH);
		dialogoLista.add(scrollPaneListaDialogo, BorderLayout.CENTER);

		checkBoxMostrarRutas.addActionListener(e -> {
			actualizarListaEnDialogo(modeloListaDialogo, checkBoxMostrarRutas.isSelected());
		});

		actualizarListaEnDialogo(modeloListaDialogo, checkBoxMostrarRutas.isSelected());
		dialogoLista.setVisible(true);
	}

	
	/**
	 * Actualizar lista en dialogo.
	 *
	 *	Actualiza la lista de imágenes en el diálogo emergente. 
	 * Puede mostrar solo los nombres de archivo o incluir las rutas completas.
	 *
	 * @param modeloListaDialogo the modelo lista dialogo
	 * @param mostrarRutas the mostrar rutas
	 */
	private void actualizarListaEnDialogo(DefaultListModel<String> modeloListaDialogo, boolean mostrarRutas)
	{
		modeloListaDialogo.clear();

		for (int i = 0; i < modeloLista.getSize(); i++)
		{
			String nombreArchivo = modeloLista.getElementAt(i);
			String textoAAgregar = mostrarRutas ? rutaCompletaMap.get(nombreArchivo).toString() : nombreArchivo;
			modeloListaDialogo.addElement(textoAAgregar);
		}

	}
	

	/**
	 * Copiar lista al portapapeles.
	 *
	 *	Copia la lista de imágenes al portapapeles del sistema operativo.
	 *
	 * @param modeloLista the modelo lista
	 */
	private void copiarListaAlPortapapeles(DefaultListModel<String> modeloLista)
	{
		List<String> nombresImagenes = new ArrayList<>();

		for (int i = 0; i < modeloLista.getSize(); i++)
		{
			nombresImagenes.add(modeloLista.getElementAt(i));
		}

		String listaComoString = String.join("\n", nombresImagenes);
		StringSelection stringSelection = new StringSelection(listaComoString);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, this);
	}


// ***************************************************************************************************** EVENTOS INTERNOS	
	
	/**
	 * Configurar eventos.
	 * 
	 * 	Configura los listeners para los eventos de los botones y menús.
	 * 
	 */
// ***** CONFIGURACIÓN DE EVENTOS *****
	private void configurarEventos()
	{
		// Eventos para la lista de imágenes
		listaImagenes.addListSelectionListener(e -> {
			
			if (!e.getValueIsAdjusting())
			{
				int indiceSeleccionado = listaImagenes.getSelectedIndex();

				if (indiceSeleccionado >= 0)
				{
					mostrarImagenSeleccionada();
					
// LOG llamamos a actualizarImagenesMiniaturaConRango desde configurarEventos  					
//System.out.println("llamamos a actualizarImagenesMiniaturaConRango desde configurarEventos");
					actualizarImagenesMiniaturaConRango(indiceSeleccionado); // Actualizar las imagenes en miniatura con el nuevo rango
				}
			}
		});

		// Eventos para el zoom y arrastre de la imagen
		etiquetaImagen.addMouseWheelListener(e -> {

			if (zoomHabilitado)
			{
				int notches = e.getWheelRotation();
				zoomFactor += notches < 0 ? 0.1 : -0.1;
				zoomFactor = Math.max(0.1, Math.min(zoomFactor, 10.0));
				etiquetaImagen.repaint();
			}
		});
		etiquetaImagen.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (zoomHabilitado)
				{
					lastMouseX = e.getX();
					lastMouseY = e.getY();
				}
			}
		});
		etiquetaImagen.addMouseMotionListener(new MouseMotionAdapter()
		{
			@Override
			public void mouseDragged(MouseEvent e)
			{

				if (zoomHabilitado)
				{
					imageOffsetX += e.getX() - lastMouseX;
					imageOffsetY += e.getY() - lastMouseY;
					lastMouseX = e.getX();
					lastMouseY = e.getY();
					etiquetaImagen.repaint();
				}

			}
		});
	}
	
	
	/**
	 * Lost ownership.
	 *
	 * @param clipboard the clipboard
	 * @param contents the contents
	 */
	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents)
	{
		System.out.println("Lost Clipboard Ownership");
	}

	/**
	 * Dispose.
	 */
	@Override
	public void dispose()
	{
		super.dispose();
	}


//	****************************************************************************** metodo nunca usado	
	/**
	 * Cargar imagen inicial.
	 *
	 * @param config the config
	 */
	// Cargar la Imagen Inicial
	private void cargarImagenInicial(Map<String, String> config)
	{
		String folderInit = config.getOrDefault("folderinit", "");
		String imageInit = config.getOrDefault("imageinit", "");

		if (!folderInit.isEmpty() && !imageInit.isEmpty())
		{
			Path rutaCompleta = Paths.get(folderInit, imageInit);

			if (Files.exists(rutaCompleta))
			{
				try
				{
					currentImage = ImageIO.read(rutaCompleta.toFile());
					textoRuta.setText(rutaCompleta.toString());
					etiquetaImagen.setIcon(new ImageIcon(currentImage));
					reescalarImagenParaAjustar();
				} catch (IOException e){
					JOptionPane.showMessageDialog(this, "Error al cargar la imagen inicial: " + e.getMessage(), 
							"Error", JOptionPane.ERROR_MESSAGE);
				}

			} else{
				JOptionPane.showMessageDialog(this, "La imagen inicial no existe en la ruta especificada.", 
						"Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args)
	{

		if (args.length > 0)
		{
			System.out.println("Inicio con parametros");
			//TODO ejecutar el programa cuando hacen doble click en una imagen abriendo esa imagen y en esa carpeta
			
		} else{
			System.out.println("Inicio sin parametros");
			SwingUtilities.invokeLater(() -> new VisorV2());
		}

	}
	
	/**
	 * Carga o recarga la lista de imágenes basándose en la configuración actual
	 * (carpeta 'folderinit' y flag 'mostrarSoloCarpetaActual').
	 * Cancela cualquier carga anterior en progreso.
	 * Usa carga progresiva para actualizar la JList sin bloquear el EDT.
	 * Gestiona el flag 'estaCargandoLista' para desacoplar la carga de miniaturas.
	 */
	
	/**
	 * Carga o recarga la lista de imágenes... (Usando reemplazo de modelo)
	 */
	private void cargarListaImagenes() 
	{

		if (configuration == null) // Seguridad 
		{ 
            System.err.println("ERROR: Intento de cargar lista de imágenes sin ConfigurationManager inicializado.");
             JOptionPane.showMessageDialog(this, "Error interno: No se puede acceder a la configuración para cargar imágenes.",
                                          "Error de Configuración", JOptionPane.ERROR_MESSAGE);
            return;
        }
		
//LOG INICIO CARGAR LISTA (Reemplazo Modelo) 		
//System.out.println("\n\n-->>> INICIO CARGAR LISTA (Reemplazo Modelo) ***");
	    this.estaCargandoLista = true;

	 // Cancelar tareas anteriores 
	    if (cargaImagenesFuture != null && !cargaImagenesFuture.isDone()) 
	    {
            System.out.println("Cancelando tarea de carga de lista anterior...");
            cargaImagenesFuture.cancel(true);
        }
	    
	    if (cargaMiniaturasFuture != null && !cargaMiniaturasFuture.isDone()) 
	    {
             System.out.println("Cancelando tarea de carga de miniaturas anterior...");
             cargaMiniaturasFuture.cancel(true);
	    }
	    
	 // Leer desde el configuration manager
        final String carpetaInicial = configuration.getString("inicio.carpeta", "");//inicio.carpeta
	    // Leer el modo de carga desde la variable de instancia (que fue seteada por aplicarConfiguracion)
        final boolean usarSoloCarpetaActual = this.mostrarSoloCarpetaActual;
	    
//LOG -->>>Iniciando carga para 	    
System.out.println("-->>>Iniciando carga para: " + carpetaInicial + " | Solo Carpeta Actual: " + usarSoloCarpetaActual);

	    if (!carpetaInicial.isEmpty()) 
	    {
	         // Limpieza inicial UI en EDT (más simple ahora)
	         SwingUtilities.invokeLater(() -> 
	         {
	             // No limpiamos modeloLista aquí, lo reemplazaremos
	             panelImagenesMiniatura.removeAll(); panelImagenesMiniatura.validate(); 
	             panelImagenesMiniatura.repaint();
	             etiquetaImagen.setIcon(null); textoRuta.setText(""); currentImage = null;
	             panelIzquierdo.setBorder(BorderFactory.createTitledBorder("Cargando Lista..."));
	             listaImagenes.setModel(new DefaultListModel<>()); // Poner un modelo vacío temporalmente
	         });

	        // Limpiar mapas
	        rutaCompletaMap.clear();
	        miniaturasMap.clear();

	        final Path startPath = Paths.get(carpetaInicial);

	        // Iniciar tarea en segundo plano
	        cargaImagenesFuture = executorService.submit(() -> {

	            // --- Crear NUEVO modelo y mapa localmente en este hilo ---
	            DefaultListModel<String> nuevoModelo = new DefaultListModel<>();
	            Map<String, Path> nuevoRutaCompletaMap = new HashMap<>();
	            Set<String> archivosAgregados = new HashSet<>();
	            // ---------------------------------------------------------

long taskStartTime = System.currentTimeMillis();
System.out.println("-->>> Tarea background (lista) iniciada @ " + taskStartTime);

	            try 
	            {
	                if (Thread.currentThread().isInterrupted()) { /*...*/ return; }
	                int depth = usarSoloCarpetaActual ? 1 : Integer.MAX_VALUE;
	                
System.out.println("-->>>Ejecutando walk con depth: " + depth + " en " + startPath);
long walkStartTime = System.currentTimeMillis();
System.out.println("-->>> Iniciando Files.walk @ " + walkStartTime);

	                try (Stream<Path> stream = Files.walk(startPath, depth)) 
	                {
	                    stream
		                    .filter(path -> !path.equals(startPath))
	                        .filter(Files::isRegularFile)
	                        .filter(path -> 
	                        { 
	                             String fileName = path.getFileName().toString();
	                             int i = fileName.lastIndexOf('.'); String extension = "";
	                             if (i > 0 && i < fileName.length() - 1) { extension = fileName.substring(i + 1).toLowerCase(); }
	                             return extension.equals("jpg") || 
	                            		 extension.equals("jpeg") || 
	                            		 extension.equals("png") || 
	                            		 extension.equals("gif") || 
	                            		 extension.equals("bmp");
	                        })
	                        .forEach(path -> 
	                        {
	                            if (Thread.currentThread().isInterrupted()) 
	                            { 
	                            	throw new RuntimeException("Tarea cancelada"); 
	                            }
	                            Path relativePath = startPath.relativize(path);
	                            String uniqueKey = relativePath.toString();
	                            if (archivosAgregados.add(uniqueKey)) 
	                            {
	                                // --- Llenar el modelo y mapa ---
	                                nuevoModelo.addElement(uniqueKey);
	                                nuevoRutaCompletaMap.put(uniqueKey, path);
	                                // -------------------------------------
	                            }
	                        });
	                }

long walkEndTime = System.currentTimeMillis();
System.out.println("-->>> Files.walk terminado @ " + walkEndTime);
System.out.println("-->>> Duración Files.walk + Stream processing: " + (walkEndTime - walkStartTime) + " ms");
System.out.println("-->>> Elementos encontrados: " + nuevoModelo.getSize());


	                 // --- INICIO: Actualización ÚNICA en EDT ---
	                 if (!Thread.currentThread().isInterrupted()) 
	                 {
	                     SwingUtilities.invokeLater(() ->
	                     {
	                    	 
System.out.println("-->>> Reemplazando modelo en EDT con " + nuevoModelo.getSize() + " elementos.");
	                         
	                         // --- Reemplazar el modelo y el mapa ---
	                         this.modeloLista = nuevoModelo; // Reasignar referencia
	                         this.rutaCompletaMap = nuevoRutaCompletaMap; // Reasignar referencia
	                         listaImagenes.setModel(this.modeloLista); // ¡Actualizar JList!
	                         
	                         // ------------------------------------
	                         
	                         panelIzquierdo.setBorder(BorderFactory.createTitledBorder("Archivos: " + this.modeloLista.getSize()));

	                         // --- Marcar fin de carga ANTES de seleccionar ---
	                         estaCargandoLista = false;
	                         
System.out.println("-->>> estaCargandoLista = false");
	                         // ---------------------------------------------

	                         // Seleccionar índice 0 si hay elementos
	                         if (this.modeloLista.getSize() > 0) 
	                         {
System.out.println("-->>> Seleccionando índice 0...");
	                             listaImagenes.setSelectedIndex(0); // Dispara listener
	                         } else {
System.out.println("-->>> Lista vacía, no se selecciona índice.");
	                         }
	                     });
	                 } else { // Interrumpido antes de actualizar UI
	                     SwingUtilities.invokeLater(() -> {
	                         panelIzquierdo.setBorder(BorderFactory.createTitledBorder("Carga Cancelada"));
	                         estaCargandoLista = false;
System.out.println("-->>> estaCargandoLista = false (cancelado antes de UI update)");
	                         // Asegurarse que la JList tenga un modelo vacío si se canceló antes de reemplazar
	                         if (listaImagenes.getModel().getSize() > 0) 
	                         {
	                             listaImagenes.setModel(new DefaultListModel<>());
	                         }
	                     });
	                 }
	                 // --- FIN: Actualización ÚNICA en EDT ---

	            } catch (RuntimeException | IOException e) { // Capturar ambos tipos de error
	                final String errorMsg = e.getMessage();
	                SwingUtilities.invokeLater(() -> 
	                {
	                     if (!"Tarea cancelada".equals(errorMsg)) 
	                     { 
	                          JOptionPane.showMessageDialog(this, "Error durante carga: " + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
	                          panelIzquierdo.setBorder(BorderFactory.createTitledBorder("Error al Cargar"));
	                     } else {
	                          panelIzquierdo.setBorder(BorderFactory.createTitledBorder("Carga Cancelada"));
	                     }
	                     
	                     estaCargandoLista = false;
System.out.println("-->>> estaCargandoLista = false (por error/cancelación)");
	                     // Asegurar modelo vacío en error
	                      listaImagenes.setModel(new DefaultListModel<>());
	                 });
	            } // Fin try-catch principal

long taskEndTime = System.currentTimeMillis();
System.out.println("-->>> Tarea background (lista) terminada @ " + taskEndTime);
System.out.println("-->>> Duración TOTAL Tarea background (lista): " + (taskEndTime - taskStartTime) + " ms");
System.out.println("-->Tarea de carga de lista en segundo plano terminada.");

	        }); // Fin submit

	    } else { // Carpeta inválida o no definida
	    	System.out.println("No se puede cargar la lista: Carpeta inicial no definida o inválida.");
	    	
	    	SwingUtilities.invokeLater(() ->
            {
                panelImagenesMiniatura.removeAll(); panelImagenesMiniatura.validate();
                panelImagenesMiniatura.repaint();
                etiquetaImagen.setIcon(null); textoRuta.setText(""); currentImage = null;
                panelIzquierdo.setBorder(BorderFactory.createTitledBorder("Lista de Archivos"));
                modeloLista.clear(); // Limpiar modelo existente
                rutaCompletaMap.clear();
                miniaturasMap.clear();
                // listaImagenes.setModel(new DefaultListModel<>()); // Ya se limpia el modelo
            });
	        estaCargandoLista = false; // Marcar como no cargando
	    }
//LOG -->>> FIN CARGAR LISTA (Reemplazo Modelo) 	     
//System.out.println("-->>> FIN CARGAR LISTA (Reemplazo Modelo) ***\n\n");
	}
	
	
	/**
	 * Alterna entre "Mostrar solo carpeta actual" y "Mostrar imágenes con subcarpetas".
	 * Actualiza la configuración y recarga la lista.
	 * @param mostrarSoloCarpeta true para mostrar solo la carpeta actual, false para incluir subcarpetas.
	 */
	private void alternarModoCarga(boolean mostrarSoloCarpeta) 
	{
		if (configuration == null) return; // Seguridad
		
	    this.mostrarSoloCarpetaActual = mostrarSoloCarpeta;

	    // Guardar la configuración actualizada en el archivo de configuración
//	    config.put("mostrarSoloCarpetaActual", String.valueOf(mostrarSoloCarpeta));
	    
//	    configuration.guardarConfiguracion(config);

	    try {
            Map<String, String> currentConfig = configuration.getConfigMap();
            // La clave es si carga subcarpetas, que es lo inverso a mostrarSoloCarpetaActual
            currentConfig.put("comportamiento.carpeta.cargarSubcarpetas", String.valueOf(!this.mostrarSoloCarpetaActual));
            configuration.guardarConfiguracion(currentConfig);
            System.out.println("Modo de carga de carpetas guardado en configuración.");
        } catch (IOException ex) {
            System.err.println("Error al guardar el modo de carga de carpetas: " + ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "No se pudo guardar el cambio en el modo de carga de carpetas:\n" + ex.getMessage(),
                    "Error al Guardar Configuración", JOptionPane.ERROR_MESSAGE);
        }
	    
	    // Recargar las imágenes con la nueva configuración
	    cargarListaImagenes();

	    // Mostrar un mensaje al usuario
	    SwingUtilities.invokeLater(() -> {
	    	
	        String mensaje = mostrarSoloCarpeta
	                ? "\nMostrando solo imágenes de la carpeta actual.\n"
	                : "\nMostrando imágenes de la carpeta actual y subcarpetas.\n";
	        
	        // Podrías usar un JLabel temporal en la UI en lugar de JOptionPane
	        JOptionPane.showMessageDialog(this, mensaje, "Cambio de Configuración", JOptionPane.INFORMATION_MESSAGE);
	        System.out.println(mensaje);
	    });
	}
	
	/**
	 * Refrescar lista imagenes.
	 */
	private void refrescarListaImagenes() {
	    cargarListaImagenes();
	    
//    	modeloLista.clear();
//        rutaCompletaMap.clear();
//        miniaturasMap.clear();
//        etiquetaImagen.setIcon(null);
//        textoRuta.setText("");
//        panelIzquierdo.setBorder(BorderFactory.createTitledBorder("Lista de Archivos"));
//        panelImagenesMiniatura.removeAll();
//        panelImagenesMiniatura.repaint();

	    //actualizar imagen
	    //actualizar lista de miniaturas
	}

	
	// CONFIG Método para aplicar la configuración cargada
	private void aplicarConfiguracion() //(Map<String, String> cfg) {
	{
	    System.out.println("Aplicando configuración...");
	    
	    // Asegurarse que configuration no sea null antes de usarlo
        if (configuration == null) {
             System.err.println("ERROR en aplicarConfiguracion: ConfigurationManager es null.");
             return; // Salir si no hay configuración
        }
	    
	    // --- Aplicar Configuraciones de Comportamiento y Miniaturas ---
        
//	    try {
        
        miniaturasAntes  	= configuration.getInt("miniaturas.cantidad.antes", 7); //miniaturasAntes 
        miniaturasDespues  	= configuration.getInt("miniaturas.cantidad.despues", 7); //miniaturasDespues 

        miniaturaSelAncho  	= configuration.getInt("miniaturas.tamano.seleccionada.ancho", 90);
        miniaturaSelAlto 	= configuration.getInt("miniaturas.tamano.seleccionada.alto", 90);
        miniaturaNormAncho  = configuration.getInt("thumbnails.size.normal.width", 70);
        miniaturaNormAlto  	= configuration.getInt("miniaturas.tamano.normal.alto", 70);
        
        // OJO: La clave es si carga subfolders. El flag es si muestra SOLO la actual. Son inversos.
        boolean cargarSubcarpetas = configuration.getBoolean("comportamiento.carpeta.cargarSubcarpetas", true);
        this.mostrarSoloCarpetaActual = !cargarSubcarpetas; // Actualizar el flag interno
	        
	     // --- Aplicar Estados de Interfaz Usuario ---
	    // Botones
	    for (Map.Entry<String, JButton> entry : botonesPorNombre.entrySet()) {
	        String baseKey = "interfaz.boton." + entry.getKey(); // Usar la clave base correcta
	        JButton button = entry.getValue();
	        button.setEnabled(configuration.getBoolean(baseKey + ".activado", true));
	        button.setVisible(configuration.getBoolean(baseKey + ".visible", true));
	    }
	        // Podríamos añadir lógica para estado 'selected' si algunos botones son toggles visuales
	    

	    // Menús
	    menuItemsPorNombre.forEach((actionCommand, menuItem) -> {
            String baseKey = "interfaz.menu." + actionCommand;
             if (menuItem instanceof JMenu) {
                 menuItem.setEnabled(configuration.getBoolean(baseKey + ".activado", true));
                 menuItem.setVisible(configuration.getBoolean(baseKey + ".visible", true));
             } else {
                 menuItem.setEnabled(configuration.getBoolean(baseKey + ".activado", true));
                 menuItem.setVisible(configuration.getBoolean(baseKey + ".visible", true));

                 if (menuItem instanceof JCheckBoxMenuItem) {
                     ((JCheckBoxMenuItem) menuItem).setSelected(configuration.getBoolean(baseKey + ".seleccionado", false));
                 } else if (menuItem instanceof JRadioButtonMenuItem) {
                     // El ajuste basado en this.mostrarSoloCarpetaActual ya está bien aquí
                     if (actionCommand.equals("Mostrar_Solo_Carpeta_Actual")) {
                         ((JRadioButtonMenuItem) menuItem).setSelected(this.mostrarSoloCarpetaActual);
                     } else if (actionCommand.equals("Mostrar_Imagenes_de_Subcarpetas")) {
                         ((JRadioButtonMenuItem) menuItem).setSelected(!this.mostrarSoloCarpetaActual);
                     } else {
                         // Leer el estado de los otros radios desde la configuración
                         ((JRadioButtonMenuItem) menuItem).setSelected(configuration.getBoolean(baseKey + ".seleccionado", false));
                         // Aplicar defaults si la clave no existe explícitamente
                          if (actionCommand.equals("Zoom_a_lo_Ancho") && !configuration.getConfigMap().containsKey(baseKey + ".seleccionado")) {
                              ((JRadioButtonMenuItem) menuItem).setSelected(true);
                          }
                           if (actionCommand.equals("Nombre_por_Defecto") && !configuration.getConfigMap().containsKey(baseKey + ".seleccionado")) {
                               ((JRadioButtonMenuItem) menuItem).setSelected(true);
                           }
                           if (actionCommand.equals("Sin_Ordenar") && !configuration.getConfigMap().containsKey(baseKey + ".seleccionado")) {
                               ((JRadioButtonMenuItem) menuItem).setSelected(true);
                           }
                            if (actionCommand.equals("Nombre_del_Archivo") && !configuration.getConfigMap().containsKey(baseKey + ".seleccionado")) {
                                ((JRadioButtonMenuItem) menuItem).setSelected(true);
                            }
                     }
                 }
            }
        });
	    
	 // Forzar actualización de estados dependientes (Reset Zoom)
        // OJO: La clave en tu config default era "interfaz.menu.Activar_Zoom_Manual.seleccionado"
        boolean zoomInicialActivado = configuration.getBoolean("interfaz.menu.Activar_Zoom_Manual.seleccionado", false);
        toggleZoom(zoomInicialActivado); // Aplicar estado inicial y habilitar/deshabilitar ResetZoom

        System.out.println("Configuración aplicada a la UI.");
	}


	private void guardarConfiguracionActual() 
	{
//		JOptionPane.showMessageDialog(this, "Se ha guardado la configuracion actual" 
//    			, "Guardado de la Configuracion Actual", JOptionPane.INFORMATION_MESSAGE); 
		
        if (configuration == null) {
             System.err.println("ERROR CRÍTICO al guardar: ConfigurationManager es null.");
             return; // No se puede guardar si no hay configuración
        }

		// mapa para recopilar los valores ACTUALES
        Map<String, String> currentStateToSave = new HashMap<>();

        // --- Recopilar estado ---
        // Inicio (mantener la carpeta actual)
        currentStateToSave.put("inicio.carpeta", configuration.getString("inicio.carpeta", ""));

        // Opcional: Última imagen vista (obtenerla de la lista)
        String ultimaImagenKey = "";
        int selectedIndex = listaImagenes.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < modeloLista.getSize()) {
             ultimaImagenKey = modeloLista.getElementAt(selectedIndex);
        }
        
        currentStateToSave.put("inicio.imagen", ultimaImagenKey); // Guardar ruta relativa o nombre

        // Comportamiento
        currentStateToSave.put("comportamiento.carpeta.cargarSubcarpetas", String.valueOf(!this.mostrarSoloCarpetaActual));

        // Miniaturas (guardar los valores actuales de las variables)
        currentStateToSave.put("miniaturas.cantidad.antes", String.valueOf(this.miniaturasAntes));
        currentStateToSave.put("miniaturas.cantidad.despues", String.valueOf(this.miniaturasDespues));
        currentStateToSave.put("miniaturas.tamano.seleccionada.ancho", String.valueOf(this.miniaturaSelAncho));
        currentStateToSave.put("miniaturas.tamano.seleccionada.alto", String.valueOf(this.miniaturaSelAlto));
        currentStateToSave.put("miniaturas.tamano.normal.ancho", String.valueOf(this.miniaturaNormAncho));
        currentStateToSave.put("miniaturas.tamano.normal.alto", String.valueOf(this.miniaturaNormAlto));

        // Estados UI - Botones
        botonesPorNombre.forEach((actionCommand, button) -> {
             String baseKey = "interfaz.boton." + actionCommand;
             currentStateToSave.put(baseKey + ".activado", String.valueOf(button.isEnabled()));
             currentStateToSave.put(baseKey + ".visible", String.valueOf(button.isVisible()));
             // Si tuvieras botones tipo toggle, guardarías su estado 'selected' aquí
        });

        // Estados UI - Menus
         menuItemsPorNombre.forEach((actionCommand, menuItem) -> {
             String baseKey = "interfaz.menu." + actionCommand;
             currentStateToSave.put(baseKey + ".activado", String.valueOf(menuItem.isEnabled()));
             currentStateToSave.put(baseKey + ".visible", String.valueOf(menuItem.isVisible()));
              if (menuItem instanceof JCheckBoxMenuItem) {
                  currentStateToSave.put(baseKey + ".seleccionado", String.valueOf(((JCheckBoxMenuItem) menuItem).isSelected()));
              } else if (menuItem instanceof JRadioButtonMenuItem) {
                   currentStateToSave.put(baseKey + ".seleccionado", String.valueOf(((JRadioButtonMenuItem) menuItem).isSelected()));
              }
         });

         // Añadir cualquier otro estado que quieras persistir (ej. zoom activo, etc.)
         // currentStateToSave.put("estado.zoom.habilitado", String.valueOf(this.zoomHabilitado));

        // Llamar al método que escribe el mapa recopilado, manejando la excepción SIN UI
        try {
              configuration.guardarConfiguracion(currentStateToSave); // Guardar el estado recopilado
              System.out.println("Configuración actual guardada exitosamente al cerrar.");
         } catch (IOException e) {
              System.err.println("### ERROR AL GUARDAR CONFIGURACIÓN AL CERRAR: " + e.getMessage() + " ###");
              // Considera escribir en un archivo de log si necesitas más detalles del error.
         }
	}
}



