package vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
// Importa ActionListener si lo necesitas TEMPORALMENTE aquí, aunque luego se moverá
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel; // Import necesario si el modelo se pasa aquí
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
import javax.swing.SwingConstants;
import javax.swing.Timer;

import vista.builders.MenuBarBuilder;
import vista.builders.ToolbarBuilder;
import vista.config.ViewUIConfig;

public class VisorView extends JFrame
{
	private static final long serialVersionUID = 1L; // Nuevo SerialVersionUID para la View

	// --- Componentes UI (Movidos desde VisorV2) ---
	private JList<String> listaImagenes;
	private JLabel etiquetaImagen;
	private JSplitPane splitPane;
	private JTextField textoRuta;
	private JPanel panelPrincipal;
	private JPanel panelIzquierdo;
	private JPanel panelImagenesMiniatura;
	
	// --- Mapas para acceder a componentes por nombre (Pertenecen a la View) ---
	private Map<String, JButton> botonesPorNombre = new HashMap<>();
	private Map<String, JMenuItem> menuItemsPorNombre = new HashMap<>();
	// Variable para guardar el mapa de actions ---
	private Map<String, Action> actionMap;
	
	// --- Variables de Apariencia UI (Movidas desde VisorV2) ---
	private int miniaturaScrollPaneHeight; // Variable para guardar la altura deseada del panel de miniaturas

	// --- NUEVO: Variables para UI Config ---
//    private Color colorFondo;
//    private Color colorBotonActivado;
//    private Color colorBotonAnimacion;
//    private int iconoAncho;
//    private int iconoAlto;
	
	// --- Variable para guardar el objeto de config ---
	private final ViewUIConfig uiConfig;
	
	// TODO --- Modelo de Lista (Temporalmente aquí, idealmente se pasa desde fuera) ---
	// OJO: La View NO debería ser dueña del Modelo de datos.
	// Por ahora, lo creamos aquí para que compile, pero se lo pasaremos desde el
	// Controller.
	private DefaultListModel<String> modeloLista;
	

	
	// TODO --- Referencia a la imagen reescalada (para paintComponent) ---
	// La View necesita saber qué dibujar, pero la imagen la gestionará el
	// Controller/Model
	
	private Image imagenReescaladaView;
	private double zoomFactorView = 1.0;
	private int imageOffsetXView = 0;
	private int imageOffsetYView = 0;
	
	// --- Constructor ---
    //public VisorView(int miniaturaPanelHeight, Map<String, Action> actionMap) { // Acepta el mapa
	//TODO cambiar la recepcion de tanto parametro por getters en VisorController
	 public VisorView(int miniaturaPanelHeight, ViewUIConfig config) 
	{
        super("Visor de Imágenes");
        if (config == null) { // Validación
            throw new IllegalArgumentException("ViewUIConfig no puede ser null");
        }
        
        this.miniaturaScrollPaneHeight = miniaturaPanelHeight;
        this.uiConfig = config; // Guarda la referencia al objeto de configuración
        
        
        // ... setDefaultCloseOperation, setLayout, setSize ...
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(1500, 600);

        // Aplicar color de fondo general (usando el valor del objeto config)
        // getContentPane().setBackground(this.uiConfig.colorFondo);
        
        this.modeloLista = new DefaultListModel<>();
        inicializarComponentes(); // Llama al método que AHORA usará el actionMap
    }
	

	// --- Métodos de Inicialización UI ---
    private void inicializarComponentes() {
        panelPrincipal = new JPanel(new BorderLayout());

        // 1. Crear y usar ToolbarBuilder (Pasar el mapa de actions)
//        ToolbarBuilder toolbarBuilder = new ToolbarBuilder(this.actionMap); // Pasa el mapa guardado
        ToolbarBuilder toolbarBuilder = new ToolbarBuilder(
                this.uiConfig.actionMap, // Mapa de actions desde uiConfig
                this.uiConfig.colorFondo, // Color desde uiConfig
                this.uiConfig.iconoAncho, // Ancho desde uiConfig
                this.uiConfig.iconoAlto,  // Alto desde uiConfig
                this.uiConfig.iconUtils
        );
    
        JPanel panelDeBotones = toolbarBuilder.buildToolbar();
        this.botonesPorNombre = toolbarBuilder.getBotonesPorNombreMap();

        // 2. Crear el builder pasando la definición y las actions
        MenuBarBuilder menuBuilder = new MenuBarBuilder(getMenuDefinitionString(), this.uiConfig.actionMap);
        
        // 3. Construir la barra
        JMenuBar laBarraMenu = menuBuilder.buildMenuBar();

        // 4. Obtener el mapa de items
        this.menuItemsPorNombre = menuBuilder.getMenuItemsMap();
        
        // 5. Establecer la barra en el JFrame
        setJMenuBar(laBarraMenu);
        // -----------------------------------
        
        // ... resto ...
        // Aplicar color a otros paneles si es necesario
        // panelIzquierdo.setBackground(this.colorFondo);
        // etiquetaImagen.setBackground(Color.DARK_GRAY); // Mantener oscuro para imagen? O usar this.colorFondo?
        
        inicializarPanelIzquierdo();
        panelPrincipal.add(panelDeBotones, BorderLayout.NORTH);
        JPanel panelTempImagenesMiniatura = inicializarPanelImagenesMiniatura();
        panelPrincipal.add(splitPane, BorderLayout.CENTER);
        panelPrincipal.add(panelTempImagenesMiniatura, BorderLayout.SOUTH);
        add(panelPrincipal, BorderLayout.CENTER);
        textoRuta = new JTextField();
        textoRuta.setEditable(false);
        add(textoRuta, BorderLayout.SOUTH);
    }
    

	private void inicializarPanelIzquierdo ()
	{
		// Crear la lista de imágenes (USANDO EL MODELO TEMPORAL)
		listaImagenes = new JList<>(this.modeloLista); // Usa el modelo de esta clase
		listaImagenes.setCellRenderer(new principal.NombreArchivoRenderer()); // Asegúrate que el import sea correcto
		JScrollPane scrollPaneLista = new JScrollPane(listaImagenes);

		// Panel izquierdo (lista de imágenes)
		panelIzquierdo = new JPanel(new BorderLayout());
		panelIzquierdo.setBorder(BorderFactory.createTitledBorder("Lista de Archivos"));
		panelIzquierdo.add(scrollPaneLista, BorderLayout.CENTER);

		// inicializar etiqueta para mostrar la imagen
		inicializarEtiquetaMostrarImagen();

		// SplitPane para dividir la interfaz
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierdo, etiquetaImagen);
		splitPane.setResizeWeight(0.5); // Ajusta según necesidad inicial
		splitPane.setContinuousLayout(true);

	}

	private JPanel inicializarPanelImagenesMiniatura ()
	{
		panelImagenesMiniatura = new JPanel();
		panelImagenesMiniatura.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
		panelImagenesMiniatura.setBorder(BorderFactory.createTitledBorder("Miniaturas"));
		panelImagenesMiniatura.setBackground(Color.WHITE);

		JScrollPane scrollPane = new JScrollPane(panelImagenesMiniatura);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(new Dimension(1280, 150)); // O usa el tamaño de config

		//FIXME TENER EN CUENTA altura del panel de miniaturas 
		// --- ¡CAMBIO AQUÍ! ---
        // Usar la altura recibida. Mantenemos el ancho flexible por ahora,
        // o podrías poner un valor inicial grande como antes.
        // BorderLayout respetará la altura preferida en la región SOUTH.
		scrollPane.setPreferredSize(new Dimension(1280, this.miniaturaScrollPaneHeight));		
		// Alternativa (si quieres que el ancho inicial sea menos fijo):
        // scrollPane.setPreferredSize(new Dimension(scrollPane.getPreferredSize().width, this.miniaturaScrollPaneHeight));
		
		JPanel contenedor = new JPanel(new BorderLayout());
		contenedor.add(scrollPane, BorderLayout.CENTER);

		return contenedor; // Devuelve el contenedor con el scrollpane
	}

	
	//*************************************************************************************
	
/*	
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
//						"---* Botones de Navegacion",
//						"_",
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
//						"---*Selector_de_Carpetas",
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
				"--<Temas",
					"--{",
					"---.Negro",
					"---.Blanco",
					"---.Verde",
					"---.Naranja",
					"---.Azul",
					"--}",
				"_",
				"--- Guardar Configuracion Actual",
				"--- Cargar Configuracion Inicial",
			"_",
			"--- Version"
	    };
*/	
	
	
    // Método para obtener la definición del menú como String
    private String getMenuDefinitionString() {
        // Copia aquí TODO el contenido de tu array String[] menuOptions
        // como un único String multilinea. Asegúrate de que los saltos
        // de línea sean correctos (puedes usar \n).
        return
            "- Archivo\n"+
            	"--- Abrir Archivo\n"+
            	"--- Abrir en ventana nueva\n"+
            	"--- Guardar\n"+
            	"--- Guardar Como\n"+
            	
            	"_\n"+
            	
            	"--- Abrir Con...\n"+
            	"--- Editar Imagen\n"+
            	"--- Imprimir\n"+
            	"--- Compartir\n"+
            	
            	"_\n"+
            	
            	"--- Refrescar Imagen\n"+
            	"--- Volver a Cargar\n"+
            	"--- Recargar Lista de Imagenes\n"+
            	"--- Unload Imagen\n"+
            "\n"+
            	
            "- Navegacion\n"+
	            "--- Primera Imagen\n"+
	            "--- Imagen Aterior\n"+
	            "--- Imagen Siguiente\n"+
	            "--- Ultima Imagen\n"+
	            
	            "_\n"+
	            
	            "--- Ir a...\n"+
	            "--- Primera Imagen\n"+
	            "--- Ultima Imagen\n"+
	            
	            "_\n"+
	            
	            "--- Anterior Fotograma\n"+
	            "--- Siguiente Fotograma\n"+
	            "--- Primer Fotograma\n"+
	            "--- Ultimo Fotograma\n"+
            "\n"+
	            
            "- Zoom\n"+
	            "--- Acercar\n"+
	            "--- Alejar\n"+
	            "--- Zoom Personalizado %\n"+
	            "--- Zoom Tamaño Real\n"+
	            "---* Mantener Proporciones\n"+
	            
	            "_\n"+
	            
	            "---* Activar Zoom Manual\n"+
	            "--- Resetear Zoom\n"+
	            
	            "_\n"+
	            
	            "--< Tipos de Zoom\n"+
		            "--{\n"+
		            "---. Zoom Automatico\n"+
		            "---. Zoom a lo Ancho\n"+
		            "---. Zoom a lo Alto\n"+
		            "---. Escalar Para Ajustar\n"+
		            "---. Zoom Actual Fijo\n"+
		            "---. Zoom Especificado\n"+
		            "---. Escalar Para Rellenar\n"+
		            "--}\n"+
	            "-->\n"+
            "\n"+
	            
            "- Imagen\n"+
	            "--< Carga y Orden\n"+
			        "--{\n"+
			            "----. Nombre por Defecto\n"+
			            "----. Tamaño de Archivo\n"+
			            "----. Fecha de Creacion\n"+
			            "----. Extension\n"+
			        "--}\n"+
			            
		            "_\n"+
		            
			        "--{\n"+
			            "----. Sin Ordenar\n"+
			            "----. Ascendente\n"+
			            "----. Descendente\n"+
			        "--}\n"+
		            
	            "-->\n"+
	            "--< Edicion\n"+
		            "---- Girar Izquierda\n"+
		            "---- Girar Derecha\n"+
		            "---- Voltear Horizontal\n"+
		            "---- Voltear Vertical\n"+
	            "-->\n"+
		            
	            "_\n"+
	            
	            "--- Cambiar Nombre de la Imagen\n"+
	            "--- Mover a la Papelera\n"+
	            "--- Eliminar Permanentemente\n"+
	            
	            "_\n"+
	            
	            "--- Establecer Como Fondo de Escritorio\n"+
	            "--- Establecer Como Imagen de Bloqueo\n"+
	            "--- Abrir Ubicacion del Archivo\n"+
	            
	            "_\n"+
	            
	            "--- Propiedades de la imagen\n"+
            "\n"+
	            
            "- Vista\n"+
	            "---* Barra de Menu\n"+
	            "---* Barra de Botones\n"+
	            "---* Mostrar/Ocultar la Lista de Archivos\n"+
	            "---* Imagenes en Miniatura\n"+
	            "---* Linea de Ubicacion del Archivo\n"+
	            
	            "_\n"+
	            
	            "---* Fondo a Cuadros\n"+
	            "---* Mantener Ventana Siempre Encima\n"+
	            
	            "_\n"+
	            
	            "--- Mostrar Dialogo Lista de Imagenes\n"+
            "\n"+
	            
            "- Configuracion\n"+
	            "--< Carga de Imagenes\n"+
            
		            "--{\n"+
		            "---. Mostrar Solo Carpeta Actual\n"+
		            "---. Mostrar Imagenes de Subcarpetas\n"+
		            "--}\n"+
		            
		            "_\n"+
		        
		            "---- Miniaturas en la Barra de Imagenes\n"+
		        "-->\n"+
		            
	            "_\n"+
	            
	            "--< General\n"+
		            "---* Mostrar Imagen de Bienvenida\n"+
		            "---* Abrir Ultima Imagen Vista\n"+
		            
		            "_\n"+
		            
		            "---* Volver a la Primera Imagen al Llegar al final de la Lista\n"+
		            "---* Mostrar Flechas de Navegacion\n"+
	            "-->\n"+
		            
	            "_\n"+
	            
	            "--< Visualizar Botones\n"+
		            "---* Botón Rotar Izquierda\n"+
		            "---* Botón Rotar Derecha\n"+
		            "---* Botón Espejo Horizontal\n"+
		            "---* Botón Espejo Vertical\n"+
		            "---* Botón Recortar\n"+
		            
		            "_\n"+
		            
		            "---* Botón Zoom\n"+
		            "---* Botón Zoom Automatico\n"+
		            "---* Botón Ajustar al Ancho\n"+
		            "---* Botón Ajustar al Alto\n"+
		            "---* Botón Escalar para Ajustar\n"+
		            "---* Botón Zoom Fijo\n"+
		            "---* Botón Reset Zoom\n"+
		            
		            "_\n"+
		            
		            "---* Botón Panel-Galeria\n"+
		            "---* Botón Grid\n"+
		            "---* Botón Pantalla Completa\n"+
		            "---* Botón Lista\n"+
		            "---* Botón Carrousel\n"+
		            
		            "_\n"+
		            
		            "---* Botón Refrescar\n"+
		            "---* Botón Subcarpetas\n"+
		            "---* Botón Lista de Favoritos\n"+
		            
		            "_\n"+
		            
		            "---* Botón Borrar\n"+
		            
		            "_\n"+
		            
		            "---* Botón Menu\n"+
		            "---* Mostrar Boton de Botones Ocultos\n"+
		        "-->\n"+
		            
	            "_\n"+
	            
	            "--< Barra de Informacion\n"+
		            "--{\n"+
		            "---. Nombre del Archivo\n"+
		            "---. Ruta y Nombre del Archivo\n"+
		            "--}\n"+
		            
		            "_\n"+
		            
		            "---* Numero de Imagenes en la Carpeta Actual\n"+
		            "---* % de Zoom actual\n"+
		            "---* Tamaño del Archivo\n"+
		            "---* Fecha y Hora de la Imagen\n"+
	            "-->\n"+
		            
		        "--< Tema\n"+
	            	"--{\n"+
	            	"---. Negro\n"+
	            	"---. Blanco\n"+
	            	"---. Azul\n"+
	            	"---. Naranja\n"+
	            	"---. Verde\n"+
	            	"--}\n"+
	            "-->\n"+
	            	
	            "_\n"+
	            
	            "--- Guardar Configuracion Actual\n"+
	            "--- Cargar Configuracion Inicial\n"+
	            "_\n"+
	            "--- Version";
    }
	
	
	private void inicializarEtiquetaMostrarImagen ()
	{
		etiquetaImagen = new JLabel()
		{
			private static final long serialVersionUID = 1L; // Nuevo UID

			@Override
			protected void paintComponent (Graphics g)
			{

				
				super.paintComponent(g);

				// Usa las variables de la View para dibujar
				if (imagenReescaladaView != null)
				{

					Graphics2D g2d = (Graphics2D) g.create();
					// Aplicar antialiasing puede mejorar la calidad al escalar
					g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
					g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
					g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
					// Obtenemos el tamaño ACTUAL del componente JLabel donde dibujamos
                    int panelWidth = getWidth();
                    int panelHeight = getHeight();
                    
                    // Obtenemos el tamaño de la IMAGEN BASE (la pre-escalada)
                    int baseImageWidth = imagenReescaladaView.getWidth(null);
                    int baseImageHeight = imagenReescaladaView.getHeight(null);
                    
                    // Si la imagen base no tiene tamaño, no podemos dibujar
                    if (baseImageWidth <= 0 || baseImageHeight <= 0) 
                    {
                        g2d.dispose(); // Liberamos la copia de Graphics
                        return;       // Salimos del método
                    }                    

                    // Calculamos el tamaño FINAL que tendrá la imagen DESPUÉS de aplicar el zoom
                    int finalZoomedWidth = (int) (baseImageWidth * zoomFactorView);
                    int finalZoomedHeight = (int) (baseImageHeight * zoomFactorView);
                    
                    // Calculamos dónde empezar a dibujar (esquina superior izquierda de la imagen)
                    // para que quede CENTRADA en el panel Y con el DESPLAZAMIENTO aplicado.
                    int drawX = (panelWidth - finalZoomedWidth) / 2 + imageOffsetXView;
                    int drawY = (panelHeight - finalZoomedHeight) / 2 + imageOffsetYView;
                    
                    // ¡La orden de dibujar!
                    // Dibuja la imagen BASE (imagenReescaladaView)
                    // en la posición (drawX, drawY)
                    // y con el tamaño FINAL calculado (finalZoomedWidth, finalZoomedHeight).
                    g2d.drawImage(imagenReescaladaView, drawX, drawY, finalZoomedWidth, finalZoomedHeight, null);

                    // Ya hemos terminado de dibujar, liberamos la copia de Graphics
                    g2d.dispose();

                } else {
                    // Opcional: poner aquí un texto como "Cargando..." o "Sin imagen" 
                }
			}
		};
		
		etiquetaImagen.setHorizontalAlignment(SwingConstants.CENTER);
		etiquetaImagen.setVerticalAlignment(SwingConstants.CENTER);
		// Quitar el borde si no es necesario o manejarlo dinámicamente
		// etiquetaImagen.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		etiquetaImagen.setOpaque(true); // Puede ser útil para ver el área
		etiquetaImagen.setBackground(Color.DARK_GRAY); // Color de fondo mientras no hay imagen

	}
	
	
	public void mostrarIndicadorCargaImagenPrincipal(String mensaje) {
	    // Limpia la imagen anterior
	    this.imagenReescaladaView = null;
	    // Limpia cualquier icono que pudiera tener JLabel
	    this.etiquetaImagen.setIcon(null);
	    // Pone el texto de carga
	    this.etiquetaImagen.setText(mensaje != null ? mensaje : "Cargando...");
	    this.etiquetaImagen.setForeground(Color.WHITE); // O un color visible sobre fondo oscuro
	    this.etiquetaImagen.setHorizontalAlignment(SwingConstants.CENTER);
	    this.etiquetaImagen.setVerticalAlignment(SwingConstants.CENTER);
	    // Asegura repintado
	    this.etiquetaImagen.repaint();
	}

	
	// --- Métodos para que el Controller actualice la View ---

	public void setListaImagenesModel (DefaultListModel<String> model)
	{
		this.modeloLista = model; // Guarda la referencia (útil si la view necesita el modelo)

		if (this.listaImagenes != null)
		{ 
			this.listaImagenes.setModel(model); // ¡LA LÍNEA CLAVE!
			System.out.println("[View] Modelo asignado a JList. Tamaño actual: " + model.getSize()); // Log

		} else
		{

			System.err.println("[View] ERROR: Intento de asignar modelo a JList nula!");

		}

	}

	public void setImagenMostrada (Image imagenReescalada, double zoom, int offsetX, int offsetY) {
	    this.etiquetaImagen.setText(null); // <-- QUITAR TEXTO DE CARGA
	    this.imagenReescaladaView = imagenReescalada;
	    this.zoomFactorView = zoom;
	    this.imageOffsetXView = offsetX;
	    this.imageOffsetYView = offsetY;
	    if (this.etiquetaImagen != null) {
	        this.etiquetaImagen.repaint();
	    }
	}

	public void limpiarImagenMostrada () {
	    this.etiquetaImagen.setText(null); // <-- QUITAR TEXTO DE CARGA
	    this.imagenReescaladaView = null;
	    this.zoomFactorView = 1.0;
	    this.imageOffsetXView = 0;
	    this.imageOffsetYView = 0;
	    this.etiquetaImagen.setIcon(null);
	    this.etiquetaImagen.repaint();
	}

	public void setTextoRuta (String texto)
	{
		this.textoRuta.setText(texto);
	}

	public void setTituloPanelIzquierdo (String titulo)
	{
		this.panelIzquierdo.setBorder(BorderFactory.createTitledBorder(titulo));
	}

	public void limpiarPanelMiniaturas ()
	{
		this.panelImagenesMiniatura.removeAll();
		this.panelImagenesMiniatura.revalidate(); // Revalida para actualizar layout
		this.panelImagenesMiniatura.repaint();
	}

	public void agregarMiniatura (JLabel miniaturaLabel)
	{
		this.panelImagenesMiniatura.add(miniaturaLabel);
	}

	public void refrescarPanelMiniaturas ()
	{
		this.panelImagenesMiniatura.revalidate();
		this.panelImagenesMiniatura.repaint();
	}

	
	/**
	 * Actualiza el estado visual del botón de Zoom y el estado 'enabled'
	 * de los componentes de Reset Zoom.
	 * @param zoomManualActivado true si el zoom manual está activo.
	 * @param resetHabilitado true si los componentes de reset deben estar habilitados.
	 */
	public void actualizarEstadoControlesZoom(boolean zoomManualActivado, boolean resetHabilitado) {
        String zoomButtonKey = "interfaz.boton.zoom.Zoom_48x48";
        JButton zoomButton = this.botonesPorNombre.get(zoomButtonKey);
        if (zoomButton != null) {
            // Usar colores desde el objeto uiConfig
            zoomButton.setBackground(zoomManualActivado ? this.uiConfig.colorBotonActivado : this.uiConfig.colorFondo);
        } else { /*...*/ }

        String resetButtonKey = "interfaz.boton.zoom.Reset_48x48";
        JButton resetButton = this.botonesPorNombre.get(resetButtonKey);
        if (resetButton != null) { resetButton.setEnabled(resetHabilitado); } else { /*...*/ }

        String resetMenuKey = "interfaz.menu.zoom.Resetear_Zoom";
        JMenuItem resetMenuItem = this.menuItemsPorNombre.get(resetMenuKey);
        if (resetMenuItem != null) { resetMenuItem.setEnabled(resetHabilitado); } else { /*...*/ }
    }
	
	
	/**
	 * Asegura que el estado seleccionado del JCheckBoxMenuItem "Activar Zoom Manual"
	 * coincida con el estado proporcionado.
	 * @param seleccionado El estado deseado para el checkbox.
	 */
	public void setEstadoMenuActivarZoomManual(boolean seleccionado) 
	{
	    String zoomMenuKey = "interfaz.menu.zoom.Activar_Zoom_Manual"; // Clave LARGA
	    JMenuItem zoomMenuItem = this.menuItemsPorNombre.get(zoomMenuKey);
	    if (zoomMenuItem instanceof JCheckBoxMenuItem) {
	        JCheckBoxMenuItem cbItem = (JCheckBoxMenuItem) zoomMenuItem;
	        if (cbItem.isSelected() != seleccionado) {
	            cbItem.setSelected(seleccionado);
	            // System.out.println("  [View] Checkbox Activar Zoom Manual setSelected(" + seleccionado + ")");
	        }
	    } else if (zoomMenuItem != null){
	         System.err.println("WARN [View]: Item de menú NO es JCheckBoxMenuItem: " + zoomMenuKey);
	    } else {
	         System.err.println("WARN [View]: Item de menú Activar Zoom Manual no encontrado: " + zoomMenuKey);
	    }
	}

	
	
	public void setBotonZoomActivoVisualmente (boolean activo)
	{
		String zoomButtonKey = "interfaz.boton.zoom.Zoom_48x48"; // Clave larga correcta
		JButton zoomButton = botonesPorNombre.get(zoomButtonKey);

		if (zoomButton != null)
		{			
			Color colorActivo = new Color(84, 144, 164);    // Obtener de config/constante
			Color colorOriginal = new Color(238, 238, 238); // Obtener de config/constante
			zoomButton.setBackground(activo ? colorActivo : colorOriginal);
		} else {
			System.err.println("WARN: Botón Zoom no encontrado en el mapa con clave: " + zoomButtonKey);
		}

	}

	
	 // --- Método aplicarAnimacionBoton ---
	
	/**
     * Aplica una breve animación de cambio de color a un botón específico.
     * NOTA: Este método actualmente busca el botón iterando el mapa botonesPorNombre,
     * lo cual puede ser ineficiente. Considerar un mapa inverso (comando corto -> botón)
     * si se va a usar frecuentemente o si hay muchos botones.
     * La animación está desactivada por defecto en la lógica actual post-Action.
     *
     * @param nombreBotonBase El ActionCommand CORTO del botón a animar.
     */
	 public void aplicarAnimacionBoton(String nombreBotonBase) {
         System.out.println("[VisorView] Intentando animar botón con AC Corto: " + nombreBotonBase);

         JButton botonEncontrado = null; // Variable temporal para la búsqueda

         // --- Búsqueda del Botón ---
         System.out.println("  -> Buscando botón en mapa (Claves Largas)...");
         if (botonesPorNombre != null && !botonesPorNombre.isEmpty()) {
             for (Map.Entry<String, JButton> entry : botonesPorNombre.entrySet()) {
                  if (entry.getKey().endsWith("." + nombreBotonBase)) {
                      botonEncontrado = entry.getValue(); // Asigna a la variable temporal
                      System.out.println("    -> Botón encontrado con clave larga: " + entry.getKey());
                      break;
                  }
             }
         } else { System.err.println("WARN [Animacion]: Mapa 'botonesPorNombre' vacío o null."); }

         // --- Aplicar animación SOLO si se encontró ---
         // Crear variable final DESPUÉS de la búsqueda
         final JButton jBotonFinal = botonEncontrado;

         if (jBotonFinal != null && !nombreBotonBase.equals("Zoom_48x48")) { // Usa la variable final
             System.out.println("    -> Aplicando animación...");
             Color colorOriginal = jBotonFinal.getBackground();
             if (colorOriginal == null) colorOriginal = this.uiConfig.colorFondo; // Usa color base de la vista

             jBotonFinal.setBackground(this.uiConfig.colorBotonAnimacion); // Usa color animación de la vista

             final Color finalColorOriginal = colorOriginal;
             Timer timer = new Timer(200, (ActionEvent evt) -> {
                 // Usa la variable final jBotonFinal dentro de la lambda
                 if (jBotonFinal != null) { // Chequeo extra nunca está de más
                    jBotonFinal.setBackground(finalColorOriginal);
                 }
             });
             timer.setRepeats(false);
             timer.start();
         } else if (jBotonFinal == null) {
              System.err.println("WARN [Animacion]: No se encontró botón para AC corto: " + nombreBotonBase);
         } else {
              // No se anima el botón de zoom
              // System.out.println("INFO [Animacion]: No se anima el botón de Zoom.");
         }
    }

	
	// Metodo que estabelce la relacion de los botones y las opciones del menu
	private void relacionBotonMenuItem (String botonItem)
	{
		
/*		
	boton									accion de teclado
	------									-----------------
	
	"01-Anterior_48x48.png",				flecha izquierda
    "02-Siguiente_48x48.png"				flecha derecha
    
    ------------------------------------------------
    botones que hacen lo mismo que opciones del menu
    ------------------------------------------------
    
	boton									menu item									accion
	------									---------									------
    "03-Rotar_Izquierda_48x48.png",			imagen | edicion | girar izquierda			gira la imagen a la izquierda
	"04-Rotar_Derecha_48x48.png",			imagen | edicion | girar derecha			gira la imagen a la derecha
    "05-Espejo_Horizontal_48x48.png",		imagen | edicion | voltear horizontal		voltea la imagen en horizontal
    "06-Espejo_Vertical_48x48.png",			imagen | edicion | voltear vertical			voltea la imagen en vertical
    "07-Recortar_48x48.png"					imagen | edicion | 							recorta la imagen a lo que se ve en pantalla

    "08-Zoom_48x48.png",					zoom | activar zoom manual					mueve, acerca y aleja con el mouse
    "09-Zoom_Auto_48x48.png",				zoom | tipos de zoom | zoom automatico		zoom automatico (maximo visible)
    "10-Ajustar_al_Ancho_48x48.png",		zoom | tipos de zoom | zoom a lo ancho		ajusta la imagen al ancho permitido
    "11-Ajustar_al_Alto_48x48.png",			zoom | tipos de zoom | zoom a lo alto		ajusta la imagen al alto permitido
    "12-Escalar_Para_Ajustar_48x48.png",	zoom | tipos de zoom | escalar para ajustar	maximo visible
    "13-Zoom_Fijo_48x48.png",				zoom | tipos de zoom | zoom actual fijo		establecer el factor de zoom actual para todas las imagenes
    "14-Reset_48x48.png"					zoom | reset zoom							resetea la imagen a maximo visible							

    "23-Borrar_48x48.png"					Imagen | Eliminar Permanentemente 			Borra la imagen del disco

    "24-Selector_de_Carpetas_48x48.png",	Archivo | Abrir Archivo						Abre el selector de carpeta

    "15-Panel-Galeria_48x48.png",			muestra la lista de imagenes, la imagen y la barra de miniaturas
    "16-Grid_48x48.png",					oculta la lista de imagenes y la barra de miniaturas y muestra un grid de 4 imagenes 
    "17-Pantalla_Completa_48x48.png",		muestra la imagen a pantalla completa, doble click en la imagen o tecla esc cierra
    "18-Lista_48x48.png",					abre la lista de imagen a parte
    "19-Carrousel_48x48.png"				muestra, a pantalla completa, todas las imagenes una a una (carrousel)
 
 
    "21-Subcarpetas_48x48.png",				Configuracion | Carga de Imagenes | Mostrar carpeta actual / Mostrar imagenes de subcarpeta
 
 
 	-------------------------------
    Botones sin opciones en el menu
    -------------------------------
    
    "20-Refrescar_48x48.png",				actualiza la vista actual
    "22-lista_de_favoritos_48x48.png",		Muestra la lista de imagenes favoritas		
    "25-Menu_48x48.png",					muestra el menu de la barra superior
    "26-Botones_Ocultos_48x48.png"			en caso de reducir la ventana y ocultar botones, deben aparecer aqui


	------------------
	botones que faltan
	------------------
	marcar como favorito					marca la imagen y la añade a la lista de favoritos
?	mostrar panel de lista de imagenes		muestra/oculta la lista de imagenes
?	mostrar panel de miniaturas				muestra/oculta las miniaturas 								
?	mostrar panel de botones 				muestra/oculta la barra de botones
?	mostrar panel de status 				muestra/oculta la barra inferior de status
	
	
	
*/		
	}
	
	
	// --- Métodos para que el Controller añada Listeners ---

	public void addListaImagenesSelectionListener (javax.swing.event.ListSelectionListener listener)
	{
		this.listaImagenes.addListSelectionListener(listener);
	}

	public void addEtiquetaImagenMouseWheelListener (java.awt.event.MouseWheelListener listener)
	{
		this.etiquetaImagen.addMouseWheelListener(listener);
	}

	public void addEtiquetaImagenMouseListener (java.awt.event.MouseListener listener)
	{
		this.etiquetaImagen.addMouseListener(listener);
	}

	public void addEtiquetaImagenMouseMotionListener (java.awt.event.MouseMotionListener listener)
	{
		this.etiquetaImagen.addMouseMotionListener(listener);
	}

	public void addButtonActionListener (String configKey, ActionListener listener)
	{
		JButton button = botonesPorNombre.get(configKey);
		if (button != null)
		{
			button.addActionListener(listener);
			
		} else
		{

			System.err.println("WARN: Intento de añadir listener a botón no encontrado (clave larga): " + configKey);

		}
	}

	
	 // Asegurémonos que este método busca por CLAVE LARGA
	 public void addMenuItemActionListener(String configKey, ActionListener listener) { // Cambiado nombre parámetro
	     JMenuItem menuItem = menuItemsPorNombre.get(configKey); // Busca por CLAVE LARGA
	     if (menuItem != null) {
	         if (!(menuItem instanceof JMenu)) {
	             menuItem.addActionListener(listener);
	         }
	     } else {
	         System.err.println("WARN: Intento de añadir listener a menú no encontrado (clave larga): " + configKey);
	     }
	 }
	

	// --- Getters para componentes específicos que el Controller necesita ---

	public JList<String> getListaImagenes ()
	{
		return listaImagenes;
	}

	public JLabel getEtiquetaImagen ()
	{
		return etiquetaImagen;
	}

	public JFrame getFrame ()
	{
		return this; // Devuelve la instancia del JFrame (la propia View)
	}

	public Map<String, JButton> getBotonesPorNombre ()
	{
		return botonesPorNombre;
	}

	public Map<String, JMenuItem> getMenuItemsPorNombre ()
	{
		return menuItemsPorNombre;
	}

	// Getter para el panel de miniaturas si el Controller necesita añadir
	// directamente
	public JPanel getPanelImagenesMiniatura ()
	{
		return panelImagenesMiniatura;
	}

} //fin VisorView

