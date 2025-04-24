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
import java.util.Objects;

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
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

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
	
	// --- Guardar referencia al panel de botones ---
	private JPanel panelDeBotones; 
	
	// --- Mapas para acceder a componentes por nombre (Pertenecen a la View) ---
	private Map<String, JButton> botonesPorNombre = new HashMap<>();
	private Map<String, JMenuItem> menuItemsPorNombre = new HashMap<>();
	// Variable para guardar el mapa de actions ---
	
	// --- Variables de Apariencia UI  ---
	private final ViewUIConfig uiConfig;
	private int miniaturaScrollPaneHeight; // Variable para guardar la altura deseada del panel de miniaturas
	
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
	public VisorView(int miniaturaPanelHeight, ViewUIConfig config) 
	{
		 super("Visor de Imágenes");

		 if (config == null) { // Validación
			 throw new IllegalArgumentException("ViewUIConfig no puede ser null");
		 }
		 
		 this.uiConfig = Objects.requireNonNull(config, "ViewUIConfig no puede ser null");
        
		 this.miniaturaScrollPaneHeight = miniaturaPanelHeight;
		 //this.uiConfig = config; // Guarda la referencia al objeto de configuración
        
        
		 // ... setDefaultCloseOperation, setLayout, setSize ...
		 setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		 setLayout(new BorderLayout());
		 setSize(1500, 600);

		 // Aplicar color de fondo general (usando el valor del objeto config)
		 getContentPane().setBackground(this.uiConfig.colorFondoPrincipal);
        
        
		 this.modeloLista = new DefaultListModel<>();
		 inicializarComponentes(); // Llama al método que AHORA usará el actionMap
	}
	

	// --- Métodos de Inicialización UI ---
    private void inicializarComponentes() 
    {
    	
        panelPrincipal = new JPanel(new BorderLayout());
        panelPrincipal.setBackground(this.uiConfig.colorFondoPrincipal);

        // 1. Crear y usar ToolbarBuilder (Pasar el mapa de actions)
        ToolbarBuilder toolbarBuilder = new ToolbarBuilder(
                this.uiConfig.actionMap,
                this.uiConfig.colorBotonFondo, // Color fondo normal botón
                this.uiConfig.colorBotonTexto, // Color texto normal botón
                this.uiConfig.colorBotonActivado, // Color fondo botón activado (si lo usa)
                this.uiConfig.colorBotonAnimacion, // Color fondo animación (si lo usa)
                this.uiConfig.iconoAncho,
                this.uiConfig.iconoAlto,
                this.uiConfig.iconUtils
            );
        
     // --- Guardar referencia ---
        this.panelDeBotones = toolbarBuilder.buildToolbar(); // Guarda la referencia
        
        //JPanel panelDeBotones = toolbarBuilder.buildToolbar();
        this.botonesPorNombre = toolbarBuilder.getBotonesPorNombreMap();

        // 2. Crear el builder pasando la definición y las actions
        MenuBarBuilder menuBuilder = new MenuBarBuilder(this.uiConfig.actionMap);
       
        // 3. Construir la barra
        JMenuBar laBarraMenu = menuBuilder.buildMenuBar();

        // 4. Obtener el mapa de items
        this.menuItemsPorNombre = menuBuilder.getMenuItemsMap();
        
        // 5. Establecer la barra en el JFrame
        setJMenuBar(laBarraMenu);
        // -----------------------------------
        
        
        inicializarPanelIzquierdo();
        inicializarEtiquetaMostrarImagen();
        
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierdo, etiquetaImagen);
        splitPane.setResizeWeight(0.5);
        splitPane.setContinuousLayout(true);
        // Aplicar color de fondo (puede necesitar ajustes finos o UIManager para el divisor)
        splitPane.setBackground(this.uiConfig.colorFondoPrincipal);
        splitPane.setOpaque(true); // Importante para que el fondo se vea
        // Considerar quitar borde si interfiere con el diseño del tema
        // splitPane.setBorder(BorderFactory.createEmptyBorder());
        
        JPanel panelTempImagenesMiniatura = inicializarPanelImagenesMiniatura();
        
        panelPrincipal.add(panelDeBotones, BorderLayout.NORTH);
        panelPrincipal.add(splitPane, BorderLayout.CENTER);
        panelPrincipal.add(panelTempImagenesMiniatura, BorderLayout.SOUTH);
        add(panelPrincipal, BorderLayout.CENTER);
        
        textoRuta = new JTextField();
        textoRuta.setEditable(false);
        textoRuta.setBackground(this.uiConfig.colorFondoSecundario); // Fondo
        textoRuta.setForeground(this.uiConfig.colorTextoPrimario);   // Texto
        textoRuta.setCaretColor(this.uiConfig.colorTextoPrimario);   // Cursor
     // Borde con color del tema y padding
        Border lineaExternaRuta = BorderFactory.createLineBorder(this.uiConfig.colorBorde);
        Border paddingInternoRuta = BorderFactory.createEmptyBorder(2, 5, 2, 5); // Ajusta padding si es necesario
        textoRuta.setBorder(BorderFactory.createCompoundBorder(lineaExternaRuta, paddingInternoRuta));
        add(textoRuta, BorderLayout.SOUTH);
    }
    

    // --- Métodos para controlar visibilidad de los paneles---
    
    public void setJMenuBarVisible(boolean visible) {
        JMenuBar mb = getJMenuBar();
        if (mb != null) {
            mb.setVisible(visible);
        }
    }
    
    
    public void setToolBarVisible(boolean visible) {
        if (this.panelDeBotones != null) { // Usa la variable de instancia guardada
            this.panelDeBotones.setVisible(visible);
        }
    }
    
    
    public void setFileListVisible(boolean visible) {
        if (panelIzquierdo != null) {
            panelIzquierdo.setVisible(visible);
            // Puede ser necesario ajustar el divisor del JSplitPane después
            if (splitPane != null) {
                splitPane.resetToPreferredSizes(); // Intenta reajustar
                // O podrías necesitar guardar/restaurar la posición del divisor:
                // if (!visible) lastDividerLocation = splitPane.getDividerLocation();
                // else splitPane.setDividerLocation(lastDividerLocation);
            }
        }
    }
    
    
    public void setThumbnailsVisible(boolean visible) {
        // Asumiendo que 'panelTempImagenesMiniatura' es el contenedor que se añade al SOUTH
         if (panelImagenesMiniatura != null && panelImagenesMiniatura.getParent() instanceof JPanel) {
             ((JPanel)panelImagenesMiniatura.getParent()).setVisible(visible); // Ocultar el contenedor
         } else if (panelImagenesMiniatura != null) {
             panelImagenesMiniatura.setVisible(visible); // Ocultar solo el panel interno si no hay contenedor
         }
        // Necesita revalidate/repaint en el contenedor principal
        if (panelPrincipal != null) {
            panelPrincipal.revalidate();
            panelPrincipal.repaint();
        }
    }
    
    
    public void setLocationBarVisible(boolean visible) {
        if (textoRuta != null) {
            textoRuta.setVisible(visible);
        }
    }
    
    
    public void setCheckeredBackground(boolean checkered) {
        System.out.println("TODO: Implementar fondo a cuadros en paintComponent de etiquetaImagen si checkered=" + checkered);
        // Necesitarías modificar el método paintComponent de etiquetaImagen
        // para dibujar un patrón de cuadros debajo de la imagen si este flag está activo.
        // Y luego llamar a etiquetaImagen.repaint().
        etiquetaImagen.repaint(); // Forzar repintado para que paintComponent se ejecute
    }
    //-----------------------------------------------------------------------------------------------
    
	private void inicializarPanelIzquierdo ()
	{
		// Crear la lista de imágenes (USANDO EL MODELO TEMPORAL)
		listaImagenes = new JList<>(this.modeloLista); // Usa el modelo de esta clase
		
			listaImagenes.setBackground(this.uiConfig.colorFondoSecundario); // Fondo Lista
			listaImagenes.setForeground(this.uiConfig.colorTextoPrimario);   // Texto Lista
			listaImagenes.setSelectionBackground(this.uiConfig.colorSeleccionFondo); // Fondo Selección
			listaImagenes.setSelectionForeground(this.uiConfig.colorSeleccionTexto); // Texto Selección
		
			// FIXME Si tienes un Renderer personalizado, pásale uiConfig para usar los colores allí también
			listaImagenes.setCellRenderer(new principal.NombreArchivoRenderer(this.uiConfig));
	//		listaImagenes.setCellRenderer(new principal.NombreArchivoRenderer()); // Asegúrate que el import sea correcto
		
		JScrollPane scrollPaneLista = new JScrollPane(listaImagenes);
			scrollPaneLista.getViewport().setBackground(this.uiConfig.colorFondoSecundario); // Fondo detrás de la lista
			scrollPaneLista.setBorder(BorderFactory.createLineBorder(this.uiConfig.colorBorde)); // Borde del ScrollPane
		
		// Panel izquierdo (lista de imágenes)
		panelIzquierdo = new JPanel(new BorderLayout());
			panelIzquierdo.setBackground(this.uiConfig.colorFondoPrincipal); // Fondo panel izquierdo
			
			// Borde Titulado con Colores del Tema
			Border lineaIzquierdo = BorderFactory.createLineBorder(this.uiConfig.colorBorde);
			TitledBorder bordeTituladoIzquierdo = BorderFactory.createTitledBorder(lineaIzquierdo, "Lista de Archivos");
			bordeTituladoIzquierdo.setTitleColor(this.uiConfig.colorTextoSecundario); // Color del título del borde
			panelIzquierdo.setBorder(bordeTituladoIzquierdo);
			
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
			panelImagenesMiniatura.setBackground(this.uiConfig.colorFondoPrincipal); // Fondo panel interno miniaturas
			// Borde Titulado con Colores del Tema
			Border lineaMini = BorderFactory.createLineBorder(this.uiConfig.colorBorde);
	        TitledBorder bordeTituladoMini = BorderFactory.createTitledBorder(lineaMini, "Miniaturas");
	        bordeTituladoMini.setTitleColor(this.uiConfig.colorTextoSecundario); // Color título borde
	        panelImagenesMiniatura.setBorder(bordeTituladoMini);
        

		JScrollPane scrollPane = new JScrollPane(panelImagenesMiniatura);
			scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
			scrollPane.setPreferredSize(new Dimension(1280, this.miniaturaScrollPaneHeight)); 

			scrollPane.getViewport().setBackground(this.uiConfig.colorFondoPrincipal); // Fondo detrás miniaturas
			
			// Decide qué borde quieres para el scrollpane
	        // scrollPane.setBorder(BorderFactory.createEmptyBorder()); // Sin borde
	        scrollPane.setBorder(BorderFactory.createLineBorder(this.uiConfig.colorBorde)); // Borde con color tema
		
		JPanel contenedor = new JPanel(new BorderLayout());
			contenedor.setBackground(this.uiConfig.colorFondoPrincipal); // Fondo contenedor externo
			contenedor.add(scrollPane, BorderLayout.CENTER);

		return contenedor; // Devuelve el contenedor con el scrollpane
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
		etiquetaImagen.setBackground(this.uiConfig.colorFondoSecundario); //Color de fondo cuando NO hay imagen
	}
	
	
	public void mostrarIndicadorCargaImagenPrincipal(String mensaje) 
	{
	    // Limpia la imagen anterior
	    this.imagenReescaladaView = null;
	    // Limpia cualquier icono que pudiera tener JLabel
	    this.etiquetaImagen.setIcon(null);
	    // Pone el texto de carga
	    this.etiquetaImagen.setText(mensaje != null ? mensaje : "Cargando...");
	    // Usar color de texto primario del tema
        this.etiquetaImagen.setForeground(this.uiConfig.colorTextoPrimario);
        
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
		// Recrear el borde titulado con los colores actuales del tema
        Border linea = BorderFactory.createLineBorder(this.uiConfig.colorBorde);
        TitledBorder bordeTitulado = BorderFactory.createTitledBorder(linea, titulo);
        bordeTitulado.setTitleColor(this.uiConfig.colorTextoSecundario);
		this.panelIzquierdo.setBorder(bordeTitulado);
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
	public void actualizarEstadoControlesZoom(boolean zoomManualActivado, boolean resetHabilitado) 
	{
		JButton zoomButton = this.botonesPorNombre.get("interfaz.boton.zoom.Zoom_48x48");
        if (zoomButton != null) {

        	// Usar colorBotonActivado y colorBotonFondo de uiConfig
            zoomButton.setBackground(zoomManualActivado ? this.uiConfig.colorBotonActivado : this.uiConfig.colorBotonFondo);
        }
        
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
	 public void aplicarAnimacionBoton(String nombreBotonBase) 
	 {
		 //LOG [VisorView] Intentando animar botón con AC Corto:
         //System.out.println("[VisorView] Intentando animar botón con AC Corto: " + nombreBotonBase);

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

       // Usa la variable final
         if (jBotonFinal != null && !nombreBotonBase.equals("Zoom_48x48")) 
         { 
            //LOG -> Aplicando animación...
        	 System.out.println("    -> Aplicando animación...");
        	 
             Color colorOriginal = this.uiConfig.colorBotonFondo; // Fondo normal del botón
             jBotonFinal.setBackground(this.uiConfig.colorBotonAnimacion); // Color de animación

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

