package controlador;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
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
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

// Actions
import controlador.actions.archivo.OpenFileAction;
import controlador.actions.edicion.FlipHorizontalAction;
import controlador.actions.edicion.FlipVerticalAction;
import controlador.actions.edicion.RotateLeftAction;
import controlador.actions.edicion.RotateRightAction;
import controlador.actions.navegacion.FirstImageAction;
import controlador.actions.navegacion.LastImageAction;
import controlador.actions.navegacion.NextImageAction;
import controlador.actions.navegacion.PreviousImageAction;
import controlador.actions.tema.ToggleThemeAction;
import controlador.actions.toggle.ToggleProporcionesAction;
import controlador.actions.toggle.ToggleSubfoldersAction;
import controlador.actions.vista.ToggleAlwaysOnTopAction;
import controlador.actions.vista.ToggleCheckeredBackgroundAction;
import controlador.actions.vista.ToggleFileListAction;
import controlador.actions.vista.ToggleLocationBarAction;
import controlador.actions.vista.ToggleMenuBarAction;
import controlador.actions.vista.ToggleThumbnailsAction;
import controlador.actions.vista.ToggleToolBarAction;
import controlador.actions.zoom.ResetZoomAction;
import controlador.actions.zoom.ToggleZoomManualAction;
import controlador.actions.zoom.ZoomAltoAction;
import controlador.actions.zoom.ZoomAnchoAction;
import controlador.actions.zoom.ZoomAutoAction;
import controlador.actions.zoom.ZoomFijadoAction;
import controlador.actions.zoom.ZoomFitAction;
import controlador.actions.zoom.ZoomFixedAction;
import controlador.imagen.LocateFileAction;
import controlador.worker.BuscadorArchivosWorker;
// Imports de mis clases
import modelo.VisorModel;
import servicios.ConfigurationManager;
import servicios.image.ImageEdition;
import servicios.image.ThumbnailService;
import vista.VisorView;
import vista.config.ViewUIConfig;
import vista.dialogos.ProgresoCargaDialog;
import vista.theme.Tema;
import vista.theme.ThemeManager; // Importar ThemeManager
import vista.util.IconUtils;

/**
 * Controlador principal para el Visor de Imágenes. Orquesta la interacción
 * entre el Modelo y la Vista.
 */
// Implementa ActionListener para manejar eventos de la Vista.
public class VisorController implements ActionListener, ClipboardOwner

{

	// --- Referencias ---
	  private VisorModel model;						//Clase principal de Modelo (MVC)
	  private VisorView view; 						//clase principal de Vista (MVC) (interfaz de usuario)
	  private ConfigurationManager configuration;	//Configuracion en base a config.cfg
	  private IconUtils iconUtils;					//Clase de utilidad para cargar y gestionar iconos de la aplicación.
	  private ThemeManager themeManager;			//Temas y personalizacion
	  private ThumbnailService servicioMiniaturas;	//Carga de miniaturas optimizada
	  
	// --- Servicios ---
	  private ExecutorService executorService;
      
	// --- Estado del Controlador (cosas que no son datos puros del modelo) ---
	  private int lastMouseX, lastMouseY; // Para drag
	  private Future<?> cargaImagenesFuture;
	  private Future<?> cargaMiniaturasFuture;
	  private Future<?> cargaImagenPrincipalFuture = null;
      
	// --- Variables temporales y marcadores
	  private volatile boolean estaCargandoLista = false; // Flag de estado de carga
	  private Path carpetaRaizActual = null; // Variable para la carpeta raíz seleccionada por el usuario
	  
	// --- Instancias de Action ---
	  // TODO: Añade actions para todas las demás funcionalidades
	  
	  //Navegacion 
	  private Action firstImageAction;
	  private Action previousImageAction;
	  private Action nextImageAction;
	  private Action lastImageAction;
	  
	  //Archivo
      private Action openAction;
      //private Action toggleSubfoldersAction;
      private Action deleteAction;
      
      //Edicion
      private Action rotateLeftAction;
      private Action rotateRightAction;
      private Action flipHorizontalAction;
      private Action flipVerticalAction;
      private Action cortarAction;
      
      //Zoom 
      private Action toggleZoomManualAction;
      private Action zoomAutoAction;
      private Action resetZoomAction;
      //private Action zoomWidthAction;
      private Action zoomAnchoAction;
      private Action zoomAltoAction;
      private Action zoomFitAction;
      private Action zoomFixedAction;
      private Action zoomFijadoAction;
      
      //Imagen
      private Action locateFileAction;
      
      //Vista
      private Action toggleMenuBarAction;
      private Action toggleToolBarAction; // Renombrado desde toggleButtonBarAction
      private Action toggleFileListAction;
      private Action toggleThumbnailsAction;
      private Action toggleLocationBarAction;
      private Action toggleCheckeredBgAction;
      private Action toggleAlwaysOnTopAction;
      
      //Servicios
      private Action refreshAction;
      private Action menuAction;
      private Action hiddenButtonsAction;
      
      //Tema
      private Action toggleThemeAction;
      private Action temaClearAction;
      private Action temaDarkAction;
      private Action temaBlueAction;
      private Action temaGreenAction;
      private Action temaOrangeAction;
      
      //toggle
      private Action toggleSubfoldersAction;
      private Action toggleProporcionesAction;
      
      // Guardar lista para fácil manejo
      private List<Action> themeActions;
      
      // Mapa a pasar
      private Map<String, Action> actionMap; 
    
    
	// --- Constructor ---
      public VisorController(){
    
    	// ********************* COSAS A IMPLEMENTAR EN UN FUTURO: ***************************
    	
    	//TODO TwelveMonkeys: 	Para ampliar los tipos de imagenes soportados
    	//TODO FlatLaf:			Para configurar los colores profundamente
    	
    	// ***********************************************************************************
    	
        System.out.println ("Iniciando Visor de Imágenes V2 (con ThemeManager)...");
        // 1. Inicializar Modelo
        model = new VisorModel();

        // 2. Inicializar ConfigurationManager
        try {
        	configuration = new ConfigurationManager();
        } catch (IOException e) { /* Manejo de error fatal */ System.exit(1); }

        // 3. Inicializar ThemeManager (DESPUÉS de ConfigurationManager)
        this.themeManager = new ThemeManager(configuration);

        // 4. Inicializar IconUtils (DESPUÉS de ThemeManager)
        this.iconUtils = new IconUtils(this.themeManager); // Pasa ThemeManager

        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // 5. Inicializar ThumbnailService
        this.servicioMiniaturas = new ThumbnailService(); // Creamos la instancia del servicio
        System.out.println("[Controller] ThumbnailService instanciado.");
        
        // --- 6. LEER VALORES DE UI INICIALES ---
        // Obtener el objeto Tema actual
        Tema temaInicial = this.themeManager.getTemaActual();
        
        Objects.requireNonNull(temaInicial, "El tema inicial no puede ser nulo");
        
        // Extraer colores específicos del objeto Tema
        Color cFondoPrincipal 		= temaInicial.colorFondoPrincipal();
        Color cFondoSecundario 		= temaInicial.colorFondoSecundario();
        Color cTextoPrimario 		= temaInicial.colorTextoPrimario();
        Color cTextoSecundario 		= temaInicial.colorTextoSecundario();
        Color cBorde 				= temaInicial.colorBorde();
        Color cBordeTitulo 			= temaInicial.colorBordeTitulo();
        Color cSeleccionFondo 		= temaInicial.colorSeleccionFondo();
        Color cSeleccionTexto 		= temaInicial.colorSeleccionTexto();
        Color cBotonFondo 			= temaInicial.colorBotonFondo();
        Color cBotonTexto 			= temaInicial.colorBotonTexto();
        Color cBotonFondoActivado 	= temaInicial.colorBotonFondoActivado();
        Color cBotonFondoAnimacion 	= temaInicial.colorBotonFondoAnimacion();

        // Leer tamaño iconos (desde ConfigurationManager como antes)
        int anchoIconoLeido 		= configuration.getInt("iconos.ancho", 24);
        int altoIconoLeido 			= configuration.getInt("iconos.alto", 24);
        // -----------------------------------------------------------------------

        // --- 6. Inicializar Actions ---
        initializeActions(anchoIconoLeido, altoIconoLeido); // Pasa IconUtils implícitamente

        // --- 7. Crear mapa de actions ---
        this.actionMap = createActionMap();

        // --- 8. Calcular Altura Miniaturas ---
        int selThumbH = configuration.getInt("miniaturas.tamano.seleccionada.alto");
        int paddingVerticalTotal = 40;
        final int calculatedMiniaturePanelHeight = selThumbH + paddingVerticalTotal;

        // --- 9. Crear instancia de ViewUIConfig ---
        final ViewUIConfig uiConfig = new ViewUIConfig(
        		cFondoPrincipal, // Pasando color principal como 'colorFondo' obsoleto
                cBotonFondoActivado, // Pasando color activado
                cBotonFondoAnimacion, // Pasando color animación
                // Configuración estándar
                anchoIconoLeido, altoIconoLeido, this.actionMap, this.iconUtils,
                // Nuevos colores específicos (ASEGURA QUE ViewUIConfig LOS ACEPTA)
                cFondoPrincipal, cFondoSecundario, cTextoPrimario, cTextoSecundario,
                cBorde, cBordeTitulo, cSeleccionFondo, cSeleccionTexto,
                cBotonFondo, cBotonTexto, cBotonFondoActivado, cBotonFondoAnimacion,
                this.configuration
        );

        // --- 10. Crear Vista y Conectar en el EDT ---
        final VisorController controllerInstance = this;
        SwingUtilities.invokeLater(() -> {
        	//System.out.println("[EDT] Creando VisorView...");
            view = new VisorView(calculatedMiniaturePanelHeight, uiConfig);
            //System.out.println("[EDT] Asignando modelo a JList...");
            view.setListaImagenesModel(model.getModeloLista());
            
         // --- Modificación: Obtener y Validar carpeta raíz inicial ---
            String folderInitPath = configuration.getString("inicio.carpeta", "");
            if (!folderInitPath.isEmpty()) {
                try {
                    Path tempPath = Paths.get(folderInitPath); // Usa variable temporal
                    if (Files.isDirectory(tempPath)) { // Comprueba si es directorio válido
                         this.carpetaRaizActual = tempPath; // Asigna a la variable de instancia
                         System.out.println("[Controller EDT] Carpeta Raíz Inicial establecida desde config: " + this.carpetaRaizActual);
                    } else {
                         System.err.println("WARN [Controller EDT]: Carpeta raíz inicial de config NO es un directorio válido: " + folderInitPath);
                         this.carpetaRaizActual = null;
                    }
                } catch (java.nio.file.InvalidPathException ipe) {
                     System.err.println("WARN [Controller EDT]: Ruta de carpeta raíz inicial inválida en config: " + folderInitPath);
                     this.carpetaRaizActual = null;
                }
            } else {
                System.out.println("[Controller EDT] No hay carpeta inicial en la configuración.");
                this.carpetaRaizActual = null;
            }
         //--------------------
            
            //System.out.println("[EDT] Llamando a aplicarConfiguracionInicial...");
            controllerInstance.aplicarConfiguracionInicial(); // Aplica estados enabled/visible/selected
            //System.out.println("[EDT] Llamando a cargarEstadoInicial...");
            controllerInstance.cargarEstadoInicial();       // Carga carpeta e imagen inicial
            //System.out.println("[EDT] Llamando a configurarListenersNoAction...");
            controllerInstance.configurarListenersNoAction(); // JList, Mouse, etc.
            //System.out.println("[EDT] Llamando a configurarComponentActions...");
            controllerInstance.configurarComponentActions(); // setAction para botones/menus
            
            //System.out.println("[EDT] Haciendo visible la vista...");
            view.setVisible(true);
            
         // --- Sincronizar estado inicial Fondo a Cuadros ---
            // Después de que todo es visible, leer el estado de la Action
            // y aplicarlo explícitamente a la lógica de pintado de la vista.
            //System.out.println("[EDT] Sincronizando estado visual inicial de Fondo a Cuadros...");
            if (toggleCheckeredBgAction != null && view != null) {
                 boolean initialState = Boolean.TRUE.equals(toggleCheckeredBgAction.getValue(Action.SELECTED_KEY));
                 //System.out.println("  -> Estado inicial de Action Fondo a Cuadros: " + initialState);
                 view.setCheckeredBackgroundEnabled(initialState); // Llamada explícita AHORA
            } else {
                 System.err.println("ERROR: No se pudo sincronizar estado inicial de Fondo a Cuadros (Action o View nula).");
            }

             // El forzado visual de los Radio Buttons de tema (si aún lo tienes) también iría aquí o en su propio invokeLater
             // SwingUtilities.invokeLater(() -> { /* ... forzado visual radios ... */ });

            
            System.out.println("[Controller] Inicialización de UI en EDT completada.");
        }); // Fin invokeLater

        // 11. Configurar Hook de cierre
        configurarShutdownHook();
        System.out.println("[Controller] Constructor finalizado.");
    }
    
	
	// --- Método para inicializar las Actions (CORREGIDO) ---
	private void initializeActions(int iconoAncho, int iconoAlto) 
	{
		//LOG [Controller] Inicializando Actions
	    System.out.println("[Controller] Inicializando Actions...");

	    if (this.iconUtils == null) { // Validación importante
            System.err.println("FATAL [Controller]: IconUtils no está inicializado antes de initializeActions!");
            // Considerar lanzar excepción o salir
            return;
        }
	    
	    // --- Instanciar TODAS las Actions usando sus clases dedicadas ---
	    // Asegúrate de haber creado los archivos .java correspondientes en controlador.actions

	  //Navegacion
	    firstImageAction 		= new FirstImageAction(this, this.iconUtils, iconoAncho, iconoAlto); // Pasa tamaño
	    previousImageAction 	= new PreviousImageAction(this, this.iconUtils, iconoAncho, iconoAlto); // Pasa tamaño
	    nextImageAction 		= new NextImageAction(this, this.iconUtils, iconoAncho, iconoAlto); // Pasa tamaño
	    lastImageAction 		= new LastImageAction(this, this.iconUtils, iconoAncho, iconoAlto); // Pasa tamaño
	    
	  //Archivo
	    openAction 				= new OpenFileAction(this, this.iconUtils, iconoAncho, iconoAlto); // Asume que OpenFileAction existe y carga icono
	    
	  //Edicion
	    rotateLeftAction 		= new RotateLeftAction(this, this.iconUtils, iconoAncho, iconoAlto);
	    rotateRightAction 		= new RotateRightAction(this, this.iconUtils, iconoAncho, iconoAlto);
	    flipHorizontalAction 	= new FlipHorizontalAction(this, this.iconUtils, iconoAncho, iconoAlto);
	    flipVerticalAction 		= new FlipVerticalAction(this, this.iconUtils, iconoAncho, iconoAlto);
	    //cropAction = new CropAction(this, this.iconUtils, iconoAncho, iconoAlto);
	    
	  //Imagen
	    locateFileAction = new LocateFileAction(this, this.iconUtils, iconoAncho, iconoAlto);
	    
	  //Zoom
	    toggleZoomManualAction 	= new ToggleZoomManualAction(this, this.iconUtils, iconoAncho, iconoAlto); // Asume que ToggleZoomManualAction existe y carga icono
	    zoomAutoAction 			= new ZoomAutoAction(this, this.iconUtils, iconoAncho, iconoAlto);
	    zoomAnchoAction 		= new ZoomAnchoAction(this, this.iconUtils, iconoAncho, iconoAlto);
	    zoomAltoAction 			= new ZoomAltoAction(this, this.iconUtils, iconoAncho, iconoAlto);
	    zoomFitAction 			= new ZoomFitAction(this, this.iconUtils, iconoAncho, iconoAlto);
	    zoomFixedAction 		= new ZoomFixedAction(this, this.iconUtils, iconoAncho, iconoAlto);
	    zoomFijadoAction	 	= new ZoomFijadoAction(this, this.iconUtils, iconoAncho, iconoAlto);
	    resetZoomAction 		= new ResetZoomAction(this, this.iconUtils, iconoAncho, iconoAlto); // Asume que ResetZoomAction existe y carga icono
	    
	  //Vista
	    toggleMenuBarAction = new ToggleMenuBarAction(this); // No necesita icono ni tamaño
        toggleToolBarAction = new ToggleToolBarAction(this); // Cambiado nombre clase
        toggleFileListAction = new ToggleFileListAction(this);
        toggleThumbnailsAction = new ToggleThumbnailsAction(this);
        toggleLocationBarAction = new ToggleLocationBarAction(this);
        toggleCheckeredBgAction = new ToggleCheckeredBackgroundAction(this);
        toggleAlwaysOnTopAction = new ToggleAlwaysOnTopAction(this);
	  //Servicios
	    //refreshAction = new RefreshAction(this, this.iconUtils, iconoAncho, iconoAlto);
	    //deleteAction = new DeleteAction(this, this.iconUtils, iconoAncho, iconoAlto);
	    
	  //Tema
	    temaClearAction 		= new ToggleThemeAction(this, "clear", "Tema Clear");
	    temaDarkAction 			= new ToggleThemeAction(this, "dark", "Tema Dark");
	    temaBlueAction 			= new ToggleThemeAction(this, "blue", "Tema Blue");
	    temaGreenAction 		= new ToggleThemeAction(this, "green", "Tema Gren");
	    temaOrangeAction 		= new ToggleThemeAction(this, "orange", "Tema Orange");
	    
	  //toggle
	    toggleSubfoldersAction = new ToggleSubfoldersAction (this, this.iconUtils, iconoAncho, iconoAlto);
	    toggleProporcionesAction= new ToggleProporcionesAction (this, this.iconUtils, iconoAncho, iconoAlto);
	    themeActions = List.of(
	    		temaClearAction, temaDarkAction, temaBlueAction, temaGreenAction, temaOrangeAction
	    	);
	    

	    // --- Configuración adicional (como el estado inicial de SELECTED_KEY) ---
	    // Esto SÍ puede quedar aquí o moverse al constructor de la Action específica
	    boolean zoomInicialActivado = configuration.getBoolean("interfaz.menu.zoom.Activar_Zoom_Manual.seleccionado", false);
	    if (toggleZoomManualAction != null) { // Comprobar si la action se instanció
	         toggleZoomManualAction.putValue(Action.SELECTED_KEY, zoomInicialActivado);
	    }
	     if (resetZoomAction != null) { // Comprobar si la action se instanció
	          resetZoomAction.setEnabled(false); // Estado inicial deshabilitado
	     }


	    // LOG --- DEBUG: Verificar icono (opcional ahora, pero puede quedar) ---
//	    if (previousImageAction != null && previousImageAction.getValue(Action.SMALL_ICON) != null) {
//	        System.out.println("  [Controller DEBUG Post-Init] Icono para previousImageAction RECUPERADO. HashCode Action: " + previousImageAction.hashCode());
//	    } else if (previousImageAction != null) {
//	        System.err.println("  [Controller DEBUG Post-Init] ERROR: Icono para previousImageAction es NULL. HashCode Action: " + previousImageAction.hashCode());
//	    } else {
//	         System.err.println("  [Controller DEBUG Post-Init] ERROR: previousImageAction es NULL.");
//	    }
	    // --- FIN DEBUG ---

	     
	    //LOG [Controller] Actions inicializadas.
	     //System.out.println("[Controller] Actions inicializadas.");
	}
	
    
    private Map<String, Action> createActionMap() 
    {
    	
        Map<String, Action> mapaDeAcciones = new HashMap<>();
        // Clave = ActionCommand CORTO, Valor = Instancia de Action
        
        // TODO --- AÑADIR LAS OTRAS ACTIONS AL MAPA ---
        
      //Navegacion
        mapaDeAcciones.put("Primera_Imagen", firstImageAction);
        mapaDeAcciones.put("Primera_48x48", this.firstImageAction);//); // Botón
        
        mapaDeAcciones.put("Imagen_Siguiente", nextImageAction);
        mapaDeAcciones.put("Siguiente_48x48", this.nextImageAction);//); // Botón
        
        mapaDeAcciones.put("Imagen_Aterior", previousImageAction); // Cuidado con Typo si existe
        mapaDeAcciones.put("Anterior_48x48", this.previousImageAction); //previousImageAction); // Botón
        
        mapaDeAcciones.put("Ultima_Imagen", lastImageAction);
        mapaDeAcciones.put("Ultima_48x48", this.lastImageAction);//); // Boton
        
      // Archivo
        mapaDeAcciones.put("Abrir_Archivo", openAction);
        mapaDeAcciones.put("Selector_de_Carpetas_48x48", this.openAction); //openAction); // Botón

      // Zoom  
        mapaDeAcciones.put("Resetear_Zoom", resetZoomAction);
        mapaDeAcciones.put("Reset_48x48", this.resetZoomAction); //resetZoomAction); // Botón

        mapaDeAcciones.put("Zoom_Automatico", zoomAutoAction); //tamaño de la imagen original
        mapaDeAcciones.put("Zoom_Auto_48x48", zoomAutoAction);

        mapaDeAcciones.put("Zoom_a_lo_Ancho", zoomAnchoAction);
        mapaDeAcciones.put("Ajustar_al_Ancho_48x48", zoomAnchoAction);

        mapaDeAcciones.put("Zoom_a_lo_Alto", zoomAltoAction);
        mapaDeAcciones.put("Ajustar_al_Alto_48x48", zoomAltoAction);

        mapaDeAcciones.put("Escalar_Para_Ajustar", zoomFitAction);
        mapaDeAcciones.put("Escalar_Para_Ajustar_48x48", zoomFitAction);

        mapaDeAcciones.put("Zoom_Actual_Fijo", zoomFixedAction);
        mapaDeAcciones.put("Zoom_Fijo_48x48", zoomFixedAction);
        
        mapaDeAcciones.put("Zoom_Especificado", zoomFijadoAction);
        mapaDeAcciones.put("Zoom_Especifico_48x48", zoomFijadoAction);
        
        mapaDeAcciones.put("Activar_Zoom_Manual", toggleZoomManualAction);
        mapaDeAcciones.put("Zoom_48x48", this.toggleZoomManualAction);// toggleZoomManualAction); // Botón
        
      //Edicion
        mapaDeAcciones.put("Girar_Izquierda", rotateLeftAction);
        mapaDeAcciones.put("Rotar_Izquierda_48x48", rotateLeftAction);
        
        mapaDeAcciones.put("Girar_Derecha", rotateRightAction);
        mapaDeAcciones.put("Rotar_Derecha_48x48", rotateRightAction);
        
        mapaDeAcciones.put("Voltear_Horizontal", flipHorizontalAction);
        mapaDeAcciones.put("Espejo_Horizontal_48x48", flipHorizontalAction);
        
        mapaDeAcciones.put("Voltear_Vertical", flipVerticalAction);
        mapaDeAcciones.put("Espejo_Vertical_48x48", flipVerticalAction);
        
        mapaDeAcciones.put("Recortar_48x48", cortarAction);
        
      // Imagen
        mapaDeAcciones.put("Abrir_Ubicacion_del_Archivo", locateFileAction);
        mapaDeAcciones.put("Ubicacion_del_Archivo_48x48", locateFileAction);

      //Vista
        mapaDeAcciones.put("Barra_de_Menu", toggleMenuBarAction);
        mapaDeAcciones.put("Barra_de_Botones", toggleToolBarAction); // Clave del menú
        mapaDeAcciones.put("Mostrar/Ocultar_la_Lista_de_Archivos", toggleFileListAction);
        mapaDeAcciones.put("Imagenes_en_Miniatura", toggleThumbnailsAction);
        mapaDeAcciones.put("Linea_de_Ubicacion_del_Archivo", toggleLocationBarAction);
        mapaDeAcciones.put("Fondo_a_Cuadros", toggleCheckeredBgAction);
        mapaDeAcciones.put("Mantener_Ventana_Siempre_Encima", toggleAlwaysOnTopAction);
        
      // Servicios
        mapaDeAcciones.put("Menu_48x48",menuAction);         
        
        mapaDeAcciones.put("Botones_Ocultos_48x48",hiddenButtonsAction);
        
        mapaDeAcciones.put("Subcarpetas_48x48", toggleSubfoldersAction);             // Botón

        mapaDeAcciones.put("Refrescar_48x48", refreshAction); // Solo botón
        
        mapaDeAcciones.put("Recargar_Lista_de_Imagenes", refreshAction); // Opción de menú

        mapaDeAcciones.put("Eliminar_Permanentemente", deleteAction);
        
        mapaDeAcciones.put("Borrar_48x48", deleteAction);

        //Mostrar Subcarpeta Si/No 
        //mapaDeAcciones.put("Mostrar_Imagenes_de_Subcarpetas", toggleSubfoldersAction); // Radio button 1
        //mapaDeAcciones.put("Mostrar_Solo_Carpeta_Actual", toggleSubfoldersAction);    // Radio button 2 (necesitarán lógica especial en la Action)
        
        // Mostrar Tema
        mapaDeAcciones.put("Tema_Clear", 	temaClearAction);
        mapaDeAcciones.put("Tema_Dark", 	temaDarkAction);
    	mapaDeAcciones.put("Tema_Blue", 	temaBlueAction);
    	mapaDeAcciones.put("Tema_Green", 	temaGreenAction);
    	mapaDeAcciones.put("Tema_Orange", 	temaOrangeAction);
        
    	//Botones toggle
//    	mapaDeAcciones.put("Subcarpetas_48x48", toggleSubfoldersAction);
//    	mapaDeAcciones.put("Mostrar_Imagenes_de_Subcarpetas", toggleSubfoldersAction);
//    	mapaDeAcciones.put("Mostrar_Solo_Carpeta_Actual", toggleSubfoldersAction);
    	
    		//Mostrar Subcarpeta Si/No
    	mapaDeAcciones.put("Mantener_Proporciones", toggleProporcionesAction);
    	mapaDeAcciones.put("Mantener_Proporciones_48x48", toggleProporcionesAction);
    	
        // ... Añade el resto ...

        return mapaDeAcciones;
    }
	
    
    /**
     * Cambia el tema actual usando ThemeManager, guarda SOLO el nombre en config,
     * actualiza las Actions y notifica al usuario.
     */
    public void cambiarTemaYNotificar(String nuevoTema) {
        if (themeManager == null || nuevoTema == null || nuevoTema.trim().isEmpty()) { // Verificar themeManager
            System.err.println("Error: No se puede cambiar tema (ThemeManager o nombre inválido).");
            return;
        }
        System.out.println("[Controller] Solicitud para cambiar tema a: " + nuevoTema);

        // --- TEXTO MODIFICADO ---
        // 1. Establecer tema en ThemeManager (este llama a configManager.setString internamente)
        boolean cambiado = themeManager.setTemaActual(nuevoTema);
        // Ya NO se llama a configuration.setTemaActualYGuardar()
        // --- FIN MODIFICACION ---

        if (cambiado) { // Solo actualizar/notificar si realmente cambió
            // 2. Actualizar Estado de Selección de TODAS las Actions de Tema
            if (themeActions != null) {
                String temaConfirmado = themeManager.getTemaActual().nombreInterno(); // Obtener el nombre actual confirmado
                for (Action action : themeActions) {
                    if (action instanceof ToggleThemeAction) {
                        ((ToggleThemeAction) action).actualizarEstadoSeleccion(temaConfirmado);
                    }
                }
                 System.out.println("[Controller] Estado de selección de Actions de tema actualizado.");
            }

            // 3. Notificar al Usuario (Reinicio Necesario)
            String nombreTemaDisplay = nuevoTema.substring(0, 1).toUpperCase() + nuevoTema.substring(1);
            JOptionPane.showMessageDialog(
                (view != null ? view.getFrame() : null), // Manejar vista nula
                "El tema se ha cambiado a '" + nombreTemaDisplay + "'.\n" +
                "Por favor, reinicie la aplicación para aplicar los cambios visuales.",
                "Cambio de Tema",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }
    
    
    //Método para configurar Actions en la Vista ---
    // Este método reemplazará la necesidad de addActionListener para componentes con Actions
    // --- INICIO MÉTODO COMPLETO Y CORREGIDO: configurarComponentActions ---
    /**
     * Configura las Actions y Listeners para los componentes interactivos de la vista (botones y menús).
     * Utiliza setAction cuando es posible para aprovechar la sincronización automática de Swing,
     * especialmente para estados como enabled, texto, icono y SELECTED_KEY.
     * Para casos específicos como los JRadioButtonMenuItem agrupados que comparten lógica
     * pero necesitan texto diferente, se usa ActionListener y se gestiona el estado manualmente.
     */
    private void configurarComponentActions () {
        System.out.println("[Controller] Configurando Actions y Listeners en componentes...");
        if (view == null) {
            System.err.println("ERROR: Vista no inicializada al configurar actions/listeners.");
            return;
        }

        // --- 1. Asignar Actions a los BOTONES ---
        Map<String, JButton> botones = view.getBotonesPorNombre();
        if (botones != null) {
            System.out.println("  -> Asignando Actions a Botones...");

            // --- Botones Toggle ---
            setActionForKey(botones, "interfaz.boton.toggle.Subcarpetas_48x48", toggleSubfoldersAction);
            setActionForKey(botones, "interfaz.boton.toggle.Mantener_Proporciones_48x48", toggleProporcionesAction);
            // setActionForKey(botones, "interfaz.boton.toggle.Mostrar_Favoritos_48x48", toggleFavoritosAction); // Cuando la tengas
            setActionForKey(botones, "interfaz.boton.control.Ubicacion_de_Archivo_48x48" , locateFileAction);
//+ "interfaz.boton.toggle.Ubicacion_de_Archivo_48x48"

            // --- Botones Navegación ---
            setActionForKey(botones, "interfaz.boton.movimiento.Primera_48x48", firstImageAction);
            setActionForKey(botones, "interfaz.boton.movimiento.Anterior_48x48", previousImageAction);
            setActionForKey(botones, "interfaz.boton.movimiento.Siguiente_48x48", nextImageAction);
            setActionForKey(botones, "interfaz.boton.movimiento.Ultima_48x48", lastImageAction);

            // --- Botones Edición ---
            setActionForKey(botones, "interfaz.boton.edicion.Rotar_Izquierda_48x48", rotateLeftAction);
            setActionForKey(botones, "interfaz.boton.edicion.Rotar_Derecha_48x48", rotateRightAction);
            setActionForKey(botones, "interfaz.boton.edicion.Espejo_Horizontal_48x48", flipHorizontalAction);
            setActionForKey(botones, "interfaz.boton.edicion.Espejo_Vertical_48x48", flipVerticalAction);
            // setActionForKey(botones, "interfaz.boton.edicion.Recortar_48x48", cortarAction); // Cuando la tengas

            //---- Botones Imagen
            
            
             // --- Botones Zoom ---
            setActionForKey(botones, "interfaz.boton.zoom.Zoom_48x48", toggleZoomManualAction);
            setActionForKey(botones, "interfaz.boton.zoom.Zoom_Auto_48x48", zoomAutoAction);
            setActionForKey(botones, "interfaz.boton.zoom.Ajustar_al_Ancho_48x48", zoomAnchoAction);
            setActionForKey(botones, "interfaz.boton.zoom.Ajustar_al_Alto_48x48", zoomAltoAction);
            setActionForKey(botones, "interfaz.boton.zoom.Escalar_Para_Ajustar_48x48", zoomFitAction);
            setActionForKey(botones, "interfaz.boton.zoom.Zoom_Fijo_48x48", zoomFixedAction);
            // setActionForKey(botones, "interfaz.boton.zoom.zoom_especifico_48x48", zoomFijadoAction); // Ajusta si la acción existe
            setActionForKey(botones, "interfaz.boton.zoom.Reset_48x48", resetZoomAction);

             // --- Botones Vista ---
            // setActionForKey(botones, "interfaz.boton.vista.Panel-Galeria_48x48", panelGaleriaAction); // Cuando las tengas
            // setActionForKey(botones, "interfaz.boton.vista.Grid_48x48", gridAction);
            // setActionForKey(botones, "interfaz.boton.vista.Pantalla_Completa_48x48", pantallaCompletaAction);
            // setActionForKey(botones, "interfaz.boton.vista.Lista_48x48", listaAction);
            // setActionForKey(botones, "interfaz.boton.vista.Carrousel_48x48", carrouselAction);
            
             // --- Botones Control ---
            // setActionForKey(botones, "interfaz.boton.control.Refrescar_48x48", refreshAction); // Cuando la tengas
            // setActionForKey(botones, "interfaz.boton.control.lista_de_favoritos_48x48", listaFavoritosAction);
            // setActionForKey(botones, "interfaz.boton.control.Borrar_48x48", deleteAction);

             // --- Botones Especiales ---
            setActionForKey(botones, "interfaz.boton.especiales.Selector_de_Carpetas_48x48", openAction); // Reutiliza OpenFileAction
            // setActionForKey(botones, "interfaz.boton.especiales.Menu_48x48", menuAction); // Cuando la tengas
            // setActionForKey(botones, "interfaz.boton.especiales.Botones_Ocultos_48x48", hiddenButtonsAction);


            System.out.println("  -> Actions asignadas a Botones.");
        } else {
            System.err.println("WARN: Mapa de botones en la vista es null.");
        }


        // --- 2. Asignar Actions y Listeners a los MENÚS ---
        Map<String, JMenuItem> menuItems = view.getMenuItemsPorNombre();
        if (menuItems != null) {

            // --- Asignar Actions a items de menú que SÍ la usarán directamente ---
            System.out.println("  -> Asignando Actions a Menús (donde aplica)...");

            // Archivo
            setActionForKey(menuItems, "interfaz.menu.archivo.Abrir_Archivo", openAction);
            // setActionForKey(menuItems, "interfaz.menu.archivo.Guardar", guardarAction); // Cuando las tengas
            // setActionForKey(menuItems, "interfaz.menu.archivo.Guardar_Como", guardarComoAction);
            // ... otros items de Archivo

            // Navegación
            setActionForKey(menuItems, "interfaz.menu.navegacion.Primera_Imagen", firstImageAction);
            setActionForKey(menuItems, "interfaz.menu.navegacion.Imagen_Aterior", previousImageAction);
            setActionForKey(menuItems, "interfaz.menu.navegacion.Imagen_Siguiente", nextImageAction);
            setActionForKey(menuItems, "interfaz.menu.navegacion.Ultima_Imagen", lastImageAction);
            // ... otros items de Navegación

            //Imagen 
          //Imagen
            setActionForKey(menuItems, "interfaz.menu.imagen.Abrir_Ubicacion_del_Archivo", locateFileAction);
            
            // Zoom
            setActionForKey(menuItems, "interfaz.menu.zoom.Acercar", null); // TODO: Crear ZoomInAction
            setActionForKey(menuItems, "interfaz.menu.zoom.Alejar", null); // TODO: Crear ZoomOutAction
            setActionForKey(menuItems, "interfaz.menu.zoom.Activar_Zoom_Manual", toggleZoomManualAction);    // CheckBox SÍ usa setAction
            setActionForKey(menuItems, "interfaz.menu.zoom.Resetear_Zoom", resetZoomAction);
            setActionForKey(menuItems, "interfaz.menu.zoom.tipos_de_zoom.Zoom_Automatico", zoomAutoAction);
            setActionForKey(menuItems, "interfaz.menu.zoom.tipos_de_zoom.Zoom_a_lo_Ancho", zoomAnchoAction);
            setActionForKey(menuItems, "interfaz.menu.zoom.tipos_de_zoom.Zoom_a_lo_Alto", zoomAltoAction);
            setActionForKey(menuItems, "interfaz.menu.zoom.tipos_de_zoom.Escalar_Para_Ajustar", zoomFitAction);
            setActionForKey(menuItems, "interfaz.menu.zoom.tipos_de_zoom.Zoom_Actual_Fijo", zoomFixedAction);
            setActionForKey(menuItems, "interfaz.menu.zoom.Mantener_Proporciones", toggleProporcionesAction); // CheckBox SÍ usa setAction

            // setActionForKey(menuItems, "interfaz.menu.zoom.Zoom_Personalizado_%", zoomPersonalizadoAction);
            // setActionForKey(menuItems, "interfaz.menu.zoom.Zoom_Tamaño_Real", zoomTamanioRealAction);
            // Tipos de Zoom (Radios) - Estos probablemente necesiten ActionListeners específicos o una Action más compleja
            // setActionForKey(menuItems, "interfaz.menu.zoom.tipos_de_zoom.Zoom_Especificado", zoomFijadoAction);
            // setActionForKey(menuItems, "interfaz.menu.zoom.tipos_de_zoom.Escalar_Para_Rellenar", zoomRellenarAction);


            // Imagen
            // Carga y Orden (Radios) -> Necesitarán listeners específicos
            // Edicion
            setActionForKey(menuItems, "interfaz.menu.imagen.edicion.Girar_Izquierda", rotateLeftAction);
            setActionForKey(menuItems, "interfaz.menu.imagen.edicion.Girar_Derecha", rotateRightAction);
            setActionForKey(menuItems, "interfaz.menu.imagen.edicion.Voltear_Horizontal", flipHorizontalAction);
            setActionForKey(menuItems, "interfaz.menu.imagen.edicion.Voltear_Vertical", flipVerticalAction);
            // ... otros items de Imagen

            // Vista (Checkboxes usan setAction)
            setActionForKey(menuItems, "interfaz.menu.vista.Barra_de_Menu", toggleMenuBarAction);
            setActionForKey(menuItems, "interfaz.menu.vista.Barra_de_Botones", toggleToolBarAction);
            setActionForKey(menuItems, "interfaz.menu.vista.Mostrar/Ocultar_la_Lista_de_Archivos", toggleFileListAction);
            setActionForKey(menuItems, "interfaz.menu.vista.Imagenes_en_Miniatura", toggleThumbnailsAction);
            setActionForKey(menuItems, "interfaz.menu.vista.Linea_de_Ubicacion_del_Archivo", toggleLocationBarAction);
            setActionForKey(menuItems, "interfaz.menu.vista.Fondo_a_Cuadros", toggleCheckeredBgAction);
            setActionForKey(menuItems, "interfaz.menu.vista.Mantener_Ventana_Siempre_Encima", toggleAlwaysOnTopAction);
            // ... otros items de Vista

            // Configuracion
            // Carga de Imagenes (Radios - configuración especial abajo)
            // General (Checkboxes usan setAction)
            // setActionForKey(menuItems, "interfaz.menu.configuracion.general.Mostrar_Imagen_de_Bienvenida", toggleBienvenidaAction);
            // setActionForKey(menuItems, "interfaz.menu.configuracion.general.Abrir_Ultima_Imagen_Vista", toggleUltimaImagenAction);
            // setActionForKey(menuItems, "interfaz.menu.configuracion.general.Volver_a_la_Primera_Imagen_al_Llegar_al_final_de_la_Lista", toggleWrapAroundAction);
            // setActionForKey(menuItems, "interfaz.menu.configuracion.general.Mostrar_Flechas_de_Navegacion", toggleFlechasAction);
            // Visualizar Botones (Checkboxes) -> Necesitarán Actions o Listeners específicos
            // Barra de Informacion (Radios/Checkboxes) -> Necesitarán Actions o Listeners específicos
            // Tema (Radios usan setAction)
            setActionForKey(menuItems, "interfaz.menu.configuracion.tema.Tema_Clear", temaClearAction);
            setActionForKey(menuItems, "interfaz.menu.configuracion.tema.Tema_Dark", temaDarkAction);
            setActionForKey(menuItems, "interfaz.menu.configuracion.tema.Tema_Blue", temaBlueAction);
            setActionForKey(menuItems, "interfaz.menu.configuracion.tema.Tema_Green", temaGreenAction);
            setActionForKey(menuItems, "interfaz.menu.configuracion.tema.Tema_Orange", temaOrangeAction);
            // Guardar/Cargar Configuración -> Usarán Listeners de fallback probablemente


            // --- ASIGNACION ESPECIAL PARA RADIOS DE SUBCARPETAS (USANDO ActionListener) ---
            // --- ASIGNACIÓN PARA RADIOS DE SUBCARPETAS (SIMPLIFICANDO LISTENERS) ---
            System.out.println("  -> Asignando ActionListener SIMPLIFICADO a Radios de Subcarpetas...");
            String keyMostrarSub = "interfaz.menu.configuracion.carga_de_imagenes.Mostrar_Imagenes_de_Subcarpetas";
            String keyMostrarSolo = "interfaz.menu.configuracion.carga_de_imagenes.Mostrar_Solo_Carpeta_Actual";

            JMenuItem radioMostrarSub = menuItems.get(keyMostrarSub);
            if (radioMostrarSub instanceof JRadioButtonMenuItem) {
                for(ActionListener al : radioMostrarSub.getActionListeners()) radioMostrarSub.removeActionListener(al);
                // Llama SIEMPRE al controller con 'true' al hacer clic
                radioMostrarSub.addActionListener(e -> {
                    System.out.println("ActionListener: Clic en 'Mostrar Subcarpetas'");
                    setMostrarSubcarpetasAndUpdateConfig(true);
                });
                radioMostrarSub.setText("Mostrar Imágenes de Subcarpetas");
            } else { /* ... error ... */ }

            JMenuItem radioMostrarSolo = menuItems.get(keyMostrarSolo);
            if (radioMostrarSolo instanceof JRadioButtonMenuItem) {
                for(ActionListener al : radioMostrarSolo.getActionListeners()) radioMostrarSolo.removeActionListener(al);
                 // Llama SIEMPRE al controller con 'false' al hacer clic
                radioMostrarSolo.addActionListener(e -> {
                    System.out.println("ActionListener: Clic en 'Mostrar Solo Carpeta Actual'");
                    setMostrarSubcarpetasAndUpdateConfig(false);
                });
                radioMostrarSolo.setText("Mostrar Solo Carpeta Actual");
            } else { /* ... error ... */ }
            System.out.println("  -> ActionListener SIMPLIFICADO asignado a Radios.");
        } else {
            System.err.println("WARN: Mapa de menús en la vista es null.");
        }

        System.out.println("[Controller] Actions y Listeners configurados.");
    }
// --- FIN MÉTODO COMPLETO Y CORREGIDO: configurarComponentActions ---
    
    
    // --- TEXTO NUEVO: Método para manejar visibilidad y configuración ---
    /**
     * Actualiza la visibilidad de un componente específico en la vista y
     * guarda el nuevo estado en la configuración en memoria.
     *
     * @param nombreComponente Identificador del componente (debe coincidir con parte de la clave de config).
     * @param visible          El nuevo estado de visibilidad (true para mostrar, false para ocultar).
     */
    public void setComponenteVisibleAndUpdateConfig(String nombreComponente, boolean visible) 
    {
        if (view == null || configuration == null) {
            System.err.println("Error: Vista o Configuración no disponibles para actualizar visibilidad.");
            return;
        }

        System.out.println("[Controller] Cambiando visibilidad de '" + nombreComponente + "' a: " + visible);

        // Construir la clave de configuración correspondiente al estado ".seleccionado"
        // Esto asume una estructura consistente de claves. Ajustar si es necesario.
        String configKeyBase = null;
        
        switch (nombreComponente) {
            case "Barra_de_Menu":
                configKeyBase = "interfaz.menu.vista.Barra_de_Menu";
                view.setJMenuBarVisible(visible); // Necesitas este método en VisorView
                break;
            case "Barra_de_Botones":
                configKeyBase = "interfaz.menu.vista.Barra_de_Botones";
                view.setToolBarVisible(visible); // Necesitas este método en VisorView
                break;
            case "Mostrar/Ocultar_la_Lista_de_Archivos": // Nombre largo del menú
                configKeyBase = "interfaz.menu.vista.Mostrar/Ocultar_la_Lista_de_Archivos";
                view.setFileListVisible(visible); // Necesitas este método en VisorView
                break;
            case "Imagenes_en_Miniatura":
                configKeyBase = "interfaz.menu.vista.Imagenes_en_Miniatura";
                view.setThumbnailsVisible(visible); // Necesitas este método en VisorView
                break;
             case "Linea_de_Ubicacion_del_Archivo":
                 configKeyBase = "interfaz.menu.vista.Linea_de_Ubicacion_del_Archivo";
                 view.setLocationBarVisible(visible); // Necesitas este método en VisorView
                 break;
             case "Fondo_a_Cuadros":
            	 System.out.println("entro en el case Fondo_a_Cuadros");
                 configKeyBase = "interfaz.menu.vista.Fondo_a_Cuadros";
                 if (view != null) {
                	 //view.setCheckeredBackground(visible); // Necesitas este método en VisorView
                	 view.setCheckeredBackgroundEnabled(visible);
                	 //LOG-> [Controller] Llamando a view.setCheckeredBackgroundEnabled
                	 System.out.println("  -> [Controller] Llamando a view.setCheckeredBackgroundEnabled(" + visible + ")");
                 }
                 System.out.println("Justo antes del break de Fondo_a_Cuadros");
                 break;
             case "Mantener_Ventana_Siempre_Encima":
                  configKeyBase = "interfaz.menu.vista.Mantener_Ventana_Siempre_Encima";
                  view.setAlwaysOnTop(visible); // Método estándar de JFrame
                  break;
            default:
                System.err.println("WARN [setComponenteVisibleAndUpdateConfig]: Nombre de componente no reconocido: " + nombreComponente);
                return; // Salir si no sabemos qué componente es
        }

        // Construir clave completa y actualizar configuración en memoria
        if (configKeyBase != null) {
            String fullConfigKey = configKeyBase + ".seleccionado";
            configuration.setString(fullConfigKey, String.valueOf(visible));
            System.out.println("  -> Configuración en memoria actualizada: " + fullConfigKey + "=" + visible);
            // El guardado real al archivo ocurrirá en el ShutdownHook al llamar a guardarConfiguracionActual()
        }

        // Revalidar y repintar el frame principal puede ser necesario
        view.getFrame().revalidate();
        view.getFrame().repaint();
    }
    
 // --- Método auxiliar para setAction ---
    private <T extends AbstractButton> void setActionForKey(Map<String, T> componentMap, String key, Action action) {
        T component = componentMap.get(key);
        if (component != null && action != null) {
            component.setAction(action);
             // Opcional: Limpiar texto si es un botón y la Action ya tiene icono
             if (component instanceof JButton && action.getValue(Action.SMALL_ICON) != null) {
                 // ((JButton) component).setText(""); // Descomentar si quieres solo icono
                 ((JButton) component).setHideActionText(true); // Mejor opción
             }
        } else {
            if (component == null) System.err.println("WARN [setActionForKey]: Componente no encontrado para clave: " + key);
            if (action == null) System.err.println("WARN [setActionForKey]: Action es null para clave: " + key);
        }
    }

     // --- NUEVO: Método auxiliar para añadir listeners a items sin Action ---
     private void addFallbackListeners(Map<String, JMenuItem> menuItems) {
         menuItems.forEach((key, item) -> {
             // Si el item es final (no JMenu) Y NO tiene una Action asignada aún
             if (!(item instanceof JMenu) && item.getAction() == null) {
            	 
            	 //LOG -> Añadiendo Fallback Listener para
                 //System.out.println("  -> Añadiendo Fallback Listener para: " + key + " (AC Corto: "+item.getActionCommand()+")");
            	 
                 // Asegurarse de no añadir múltiples veces
                  for (ActionListener al : item.getActionListeners()) {
                      item.removeActionListener(al);
                  }
                 item.addActionListener(this); // Añadir el listener del controller
             }
         });
     }

    // --- NUEVO: Configurar listeners que NO se manejan con Action ---
    private void configurarListenersNoAction() {
         if (view == null) return;
         // Listener para selección en JList
         view.addListaImagenesSelectionListener(e -> {
        	 
        	 //LOG [ListSelectionListener] Evento detectado. ValueIsAdjusting=
        	 System.out.println("[ListSelectionListener] Evento detectado. ValueIsAdjusting=" + e.getValueIsAdjusting() + ", estaCargandoLista=" + estaCargandoLista);
        	 
             if (!e.getValueIsAdjusting() && !estaCargandoLista) {
                 int indiceSeleccionado = view.getListaImagenes().getSelectedIndex();
                 if (indiceSeleccionado >= 0) {
                     String selectedKey = model.getModeloLista().getElementAt(indiceSeleccionado);
                     if (!selectedKey.equals(model.getSelectedImageKey())) {
                         mostrarImagenSeleccionada();
                     }
                     actualizarImagenesMiniaturaConRango(indiceSeleccionado);
                 }
             }
         });

         // Listeners para Zoom/Pan en JLabel (estos no son Actions)
         view.addEtiquetaImagenMouseWheelListener(e -> {
             if (model.isZoomHabilitado()) {
                 // ... (lógica zoom rueda) ...
                 int notches = e.getWheelRotation();
                 double zoomIncrement = 0.1;
                 double newZoomFactor = model.getZoomFactor() + (notches < 0 ? zoomIncrement : -zoomIncrement);
                 model.setZoomFactor(newZoomFactor);
                 Image currentReescaled = reescalarImagenParaAjustar();
                 view.setImagenMostrada(currentReescaled, model.getZoomFactor(), model.getImageOffsetX(), model.getImageOffsetY());
             }
         });
         view.addEtiquetaImagenMouseListener(new java.awt.event.MouseAdapter() {
             @Override
             public void mousePressed(java.awt.event.MouseEvent e) {
                 if (model.isZoomHabilitado()) {
                     lastMouseX = e.getX();
                     lastMouseY = e.getY();
                 }
             }
         });
         view.addEtiquetaImagenMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
             @Override
             public void mouseDragged(java.awt.event.MouseEvent e) {
                 if (model.isZoomHabilitado()) {
                     int deltaX = e.getX() - lastMouseX;
                     int deltaY = e.getY() - lastMouseY;
                     model.addImageOffsetX(deltaX);
                     model.addImageOffsetY(deltaY);
                     lastMouseX = e.getX();
                     lastMouseY = e.getY();
                     Image currentReescaled = reescalarImagenParaAjustar();
                     view.setImagenMostrada(currentReescaled, model.getZoomFactor(), model.getImageOffsetX(), model.getImageOffsetY());
                 }
             }
         });
         System.out.println("Listeners específicos (Lista, Mouse) añadidos.");
    }

    
    // Asegúrate de que getModeloLista() sigue devolviendo la referencia actual
//	public DefaultListModel<String> getModeloLista ()
//	{
//		return modeloLista;
//	}
	
	
	// --- Métodos de Lógica (Movidos desde VisorV2 y Adaptados) ---

    private void aplicarConfiguracionInicial () {
	    System.out.println("Aplicando configuración inicial..."); // Log inicio

	    // --- Validación: Asegurarse que todo está listo ---
	    if (configuration == null || view == null || model == null) {
	         System.err.println("ERROR [aplicarConfiguracionInicial]: Configuración, Vista o Modelo nulos. Abortando.");
	         return;
	    }

	    // --- 1. Aplicar configuración al MODELO ---
	    aplicarConfiguracionAlModelo();

	    // --- 2. Aplicar configuración a la VISTA (Botones) ---
	    aplicarConfigAVistaBotones();

	    // --- 3. Aplicar configuración a la VISTA (Menús) ---
	    aplicarConfigAVistaMenus();

	    // --- 4. Aplicar estado inicial de Zoom Manual ---
	    aplicarEstadoInicialZoomManual();
	    
	    // --- 4b. Aplicar estado inicial de Proporciones
	    aplicarEstadoInicialProporciones();

	    // --- 5. Aplicar estado inicial Subcarpetas (Y SINCRONIZAR UI) ---
        aplicarEstadoInicialSubcarpetas();
        
	    System.out.println("Configuración inicial aplicada (Modelo y Vista)."); // Log fin
	}

  //********************************************************************************************** PARTES DE aplicarConfiguracionInicial
    
    
 // --- NUEVO MÉTODO para estado inicial proporciones ---
    /**
     * Lee el estado inicial de "Mantener Proporciones" desde la configuración,
     * actualiza el modelo y el estado lógico de la Action correspondiente.
     * La UI (CheckBox y Botón) se actualizará automáticamente vía setAction.
     */
    private void aplicarEstadoInicialProporciones() {
        System.out.println("Aplicando estado inicial de Mantener Proporciones...");
        try {
            // Clave de configuración para el estado seleccionado
            String configKey = "interfaz.menu.zoom.Mantener_Proporciones.seleccionado";
            // Leer estado guardado (default true)
            boolean proporcionesInicial = configuration.getBoolean(configKey, true);
            System.out.println("  -> Estado leído de config (" + configKey + "): " + proporcionesInicial);

            // 1. Actualizar Modelo
            if (model != null) {
                model.setMantenerProporcion(proporcionesInicial);
            } else {
                 System.err.println("WARN [aplicarEstadoInicialProporciones]: Modelo es null.");
            }

            // 2. Actualizar Estado Lógico de la Action
            if (toggleProporcionesAction != null) {
                toggleProporcionesAction.putValue(Action.SELECTED_KEY, proporcionesInicial);
                System.out.println("  -> Estado inicial de Action Proporciones establecido a: " + proporcionesInicial);

                // 3. (Opcional pero bueno) Actualizar aspecto visual del BOTÓN explícitamente
                actualizarAspectoBotonToggle(toggleProporcionesAction, proporcionesInicial);
                 System.out.println("  -> Aspecto visual inicial del botón Proporciones actualizado.");

            } else {
                System.err.println("WARN [aplicarEstadoInicialProporciones]: toggleProporcionesAction es null.");
            }
             System.out.println("  -> Estado inicial de Mantener Proporciones aplicado.");

        } catch (Exception e) {
            System.err.println("ERROR [aplicarEstadoInicialProporciones]: Excepción al aplicar estado: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    // Parte 1 de aplicarConfiguracionInicial
	private void aplicarEstadoInicialSubcarpetas ()
	{

		System.out.println("Aplicando estado inicial de Subcarpetas..."); // Añadir log específico

		try
		{
			// 1. Leer el estado guardado
			boolean subcarpetasInicial = configuration.getBoolean("comportamiento.carpeta.cargarSubcarpetas", true); // Default
																														// true

			// 2. Verificar que la Action exista
			if (toggleSubfoldersAction != null)
			{
				// 1. Establecer el estado lógico de la Action (SELECTED_KEY)
				// Esto es importante para que la Action sepa su estado inicial.
				toggleSubfoldersAction.putValue(Action.SELECTED_KEY, subcarpetasInicial);
				System.out.println("  -> Estado inicial de Action Subcarpetas: " + subcarpetasInicial);

				// 2. Sincronizar el aspecto visual del BOTÓN Toggle
				actualizarAspectoBotonToggle(toggleSubfoldersAction, subcarpetasInicial);

				 // 3. ESTABLECER ESTADO INICIAL DE LOS RADIOS EXPLÍCITAMENTE
                restaurarSeleccionRadiosSubcarpetas(subcarpetasInicial); // <-- LLAMADA AÑADIDA
                System.out.println("  -> Estado visual inicial de Radios aplicado.");
				
				// 5. Sincronizar el estado visual de los RADIO BUTTONS del menú
//				sincronizarRadiosSubcarpetas(subcarpetasInicial);

				System.out.println("  -> Estado inicial de Subcarpetas aplicado y UI sincronizada.");
			} else
			{
				System.err.println(
						"WARN [aplicarEstadoInicialSubcarpetas]: toggleSubfoldersAction es null, no se puede aplicar estado inicial.");
			}

		} catch (Exception e)
		{
			// Solo imprimir el error específico de esta parte
			System.err
					.println("ERROR [aplicarEstadoInicialSubcarpetas]: Excepción al aplicar estado: " + e.getMessage());
			e.printStackTrace(); // Útil para depurar si hay errores inesperados
		}
	}

	// Parte 2 de aplicarConfiguracionInicial
	private void aplicarEstadoInicialZoomManual ()
	{

		// (Esto no depende directamente de un componente específico de menú/botón,
	    // sino de un estado guardado que afecta a varios componentes y al modelo)
        try {
            // Usar la clave larga y correcta
            boolean zoomInicialActivado = configuration.getBoolean("interfaz.menu.zoom.Activar_Zoom_Manual.seleccionado", false);
            // Llamar al método del controlador que actualiza modelo y UI relacionada
            setManualZoomEnabled(zoomInicialActivado); // Este método actualiza la Action y la UI
            System.out.println("  -> Estado inicial de Zoom Manual aplicado.");
        } catch (Exception e) {
             System.err.println("ERROR [aplicarConfiguracionInicial]: Aplicando estado Zoom Manual: " + e.getMessage());
        }
        
//        try {
//            boolean subcarpetasInicial = configuration.getBoolean("comportamiento.carpeta.cargarSubcarpetas", true);
//            if(toggleSubfoldersAction != null) {
//                toggleSubfoldersAction.putValue(Action.SELECTED_KEY, subcarpetasInicial); // Asegurar estado Action
//                actualizarAspectoBotonToggle(toggleSubfoldersAction, subcarpetasInicial); // Actualizar botón
//                sincronizarRadiosSubcarpetas(subcarpetasInicial); // Sincronizar radios
//            }
//            System.out.println("  -> Estado inicial de Subcarpetas aplicado.");
//        } catch (Exception e) {
//             System.err.println("ERROR [aplicarConfiguracionInicial]: Aplicando estado Subcarpetas: " + e.getMessage());
//        }

	}

	// Parte 3 de aplicarConfiguracionInicial
	// Dentro de VisorController.aplicarConfigAVistaMenus

	private void aplicarConfigAVistaMenus() {
	    Map<String, JMenuItem> menuItems = view.getMenuItemsPorNombre();
	    if (menuItems != null) {
	        System.out.println("  -> Aplicando estado inicial a Menús...");
	        menuItems.forEach((claveCompletaMenu, menuItem) -> {
	             // --- LOG para depurar qué item se procesa ---
	             // System.out.println(claveCompletaMenu); // Puedes mantenerlo si quieres

	             try {
	                 // Aplicar Enabled y Visible a TODOS los items (incluyendo JMenu si es necesario)
	                 // Podrías querer aplicar esto también a los JMenu contenedores
	                 menuItem.setEnabled(configuration.getBoolean(claveCompletaMenu + ".activado", true));
	                 menuItem.setVisible(configuration.getBoolean(claveCompletaMenu + ".visible", true));

	                 // --- SOLO aplicar estado seleccionado si NO tiene una Action que maneje SELECTED_KEY ---
	                 // Verificamos si el item es de tipo seleccionable Y si tiene una Action asignada
	                 boolean esSeleccionable = (menuItem instanceof JCheckBoxMenuItem || menuItem instanceof JRadioButtonMenuItem);
	                 boolean tieneAction = (menuItem.getAction() != null); // Comprueba si se le asignó una Action

	                 if (esSeleccionable && !tieneAction) {
	                      // Si es seleccionable PERO NO tiene Action asignada,
	                      // leemos y aplicamos el .seleccionado desde config.cfg
	                      System.out.println("    -> Aplicando .seleccionado desde config (sin Action): " + claveCompletaMenu); // Log
	                      if (menuItem instanceof JCheckBoxMenuItem) {
	                          ((JCheckBoxMenuItem) menuItem).setSelected(configuration.getBoolean(claveCompletaMenu + ".seleccionado", false));
	                      } else if (menuItem instanceof JRadioButtonMenuItem) {
	                          ((JRadioButtonMenuItem) menuItem).setSelected(configuration.getBoolean(claveCompletaMenu + ".seleccionado", false));
	                      }
	                 } else if (esSeleccionable && tieneAction) {
	                      // Si es seleccionable Y SÍ tiene una Action,
	                      // *NO HACEMOS NADA* con setSelected aquí. El estado inicial
	                      // de Action.SELECTED_KEY (establecido en el constructor de la Action)
	                      // se encargará de la selección inicial.
	                       //System.out.println("    -> Omitiendo .seleccionado (manejado por Action): " + claveCompletaMenu); // Log opcional
	                 }
	                 // Para items no seleccionables (JMenuItem normal, JMenu), no hacemos nada con .seleccionado

	             } catch (Exception e) {
	                System.err.println("ERROR [aplicarConfiguracionInicial]: Aplicando estado a Menú '" + claveCompletaMenu + "': " + e.getMessage());
	             }
	        });
	         System.out.println("  -> Estado inicial de Menús aplicado.");
	    } else {
	         System.err.println("WARN [aplicarConfiguracionInicial]: view.getMenuItemsPorNombre() es null.");
	    }
	}

	// Parte 4 de aplicarConfiguracionInicial
	private void aplicarConfigAVistaBotones ()
	{

		Map<String, JButton> botones = view.getBotonesPorNombre();
	    if (botones != null) {
             System.out.println("  -> Aplicando estado inicial a Botones...");
	        botones.forEach((claveCompletaBoton, button) -> {
                try {
	                button.setEnabled(configuration.getBoolean(claveCompletaBoton + ".activado", true));
	                button.setVisible(configuration.getBoolean(claveCompletaBoton + ".visible", true));
                } catch (Exception e) {
                    System.err.println("ERROR [aplicarConfiguracionInicial]: Aplicando estado a Botón '" + claveCompletaBoton + "': " + e.getMessage());
                }
	        });
            System.out.println("  -> Estado inicial de Botones aplicado.");
	    } else {
	         System.err.println("WARN [aplicarConfiguracionInicial]: view.getBotonesPorNombre() es null.");
	    }

	}

	// Parte 5 de aplicarConfiguracionInicial
	private void aplicarConfiguracionAlModelo ()
	{
	    // (Estos valores afectan la lógica, no directamente la UI inicial visible)
	    try { // Es buena idea envolver lecturas de config en try-catch por si acaso
	        model.setMiniaturasAntes(configuration.getInt("miniaturas.cantidad.antes", 7)); // Usar defaults seguros
	        model.setMiniaturasDespues(configuration.getInt("miniaturas.cantidad.despues", 7));
	        model.setMiniaturaSelAncho(configuration.getInt("miniaturas.tamano.seleccionada.ancho", 60));
	        model.setMiniaturaSelAlto(configuration.getInt("miniaturas.tamano.seleccionada.alto", 60));
	        model.setMiniaturaNormAncho(configuration.getInt("miniaturas.tamano.normal.ancho", 40));
	        model.setMiniaturaNormAlto(configuration.getInt("miniaturas.tamano.normal.alto", 40));
	        boolean cargarSubcarpetas = configuration.getBoolean("comportamiento.carpeta.cargarSubcarpetas", true); // Default seguro
	        model.setMostrarSoloCarpetaActual(!cargarSubcarpetas);
             System.out.println("  -> Configuración del Modelo aplicada.");
	    } catch (Exception e) {
	        System.err.println("ERROR [aplicarConfiguracionInicial]: Excepción al aplicar configuración al Modelo: " + e.getMessage());
	        // Considerar mostrar error al usuario o usar valores por defecto más robustos
	    }

	}


	//**********************************************************************************************
	
	private void guardarConfiguracionActual() 
	{
	    if (configuration == null || view == null || model == null) 
	    {
	        System.err.println("Error: Configuración, Vista o Modelo nulos en guardarConfiguracionActual.");
	        return;
	    }
	    
	    Map<String, String> currentStateToSave = configuration.getConfigMap();//new HashMap<>();
	    System.out.println("Recopilando estado actual para guardar configuración...");

	    // --- Obtener estado del MODELO (Sin cambios aquí) ---
	    currentStateToSave.put("inicio.carpeta", configuration.getString("inicio.carpeta", ""));
	    currentStateToSave.put("inicio.imagen", model.getSelectedImageKey() != null ? model.getSelectedImageKey() : "");
	    currentStateToSave.put("comportamiento.carpeta.cargarSubcarpetas", String.valueOf(!model.isMostrarSoloCarpetaActual()));
	    currentStateToSave.put("miniaturas.cantidad.antes", String.valueOf(model.getMiniaturasAntes()));
	    currentStateToSave.put("miniaturas.cantidad.despues", String.valueOf(model.getMiniaturasDespues()));
	    currentStateToSave.put("miniaturas.tamano.seleccionada.ancho", String.valueOf(model.getMiniaturaSelAncho()));
	    currentStateToSave.put("miniaturas.tamano.seleccionada.alto", String.valueOf(model.getMiniaturaSelAlto()));
	    currentStateToSave.put("miniaturas.tamano.normal.ancho", String.valueOf(model.getMiniaturaNormAncho()));
	    currentStateToSave.put("miniaturas.tamano.normal.alto", String.valueOf(model.getMiniaturaNormAlto()));
	    // currentStateToSave.put("interfaz.menu.zoom.Activar_Zoom_Manual.seleccionado", String.valueOf(model.isZoomHabilitado())); // Opcional guardar estado zoom

	    // --- Obtener estado de la VISTA (botones y menús) ---

	    // Estado de Botones (Sin cambios aquí, ya usaba la clave completa del mapa)
	    if (view.getBotonesPorNombre() != null) {
	        view.getBotonesPorNombre().forEach( (claveCompletaBoton, button) -> {
	            currentStateToSave.put(claveCompletaBoton + ".activado", String.valueOf(button.isEnabled()));
	            currentStateToSave.put(claveCompletaBoton + ".visible", String.valueOf(button.isVisible()));
	        });
	    } else {
	        System.err.println("Advertencia: view.getBotonesPorNombre() es null en guardarConfiguracionActual.");
	    }

	    // Estado de Menús (MODIFICADO: Asegurarse de usar las claves COMPLETAS del mapa)
	    if (view.getMenuItemsPorNombre() != null) 
	    {
	        view.getMenuItemsPorNombre().forEach( (claveCompletaMenu, menuItem) -> { // MODIFICADO: claveCompletaMenu AHORA es la clave correcta
	             if (!(menuItem instanceof JMenu)) { // Solo guardar estado de items reales
	                // MODIFICADO: Usar claveCompletaMenu directamente para guardar
	                currentStateToSave.put(claveCompletaMenu + ".activado", String.valueOf(menuItem.isEnabled()));
	                currentStateToSave.put(claveCompletaMenu + ".visible", String.valueOf(menuItem.isVisible()));

	                if (menuItem instanceof JCheckBoxMenuItem) {
	                    currentStateToSave.put(claveCompletaMenu + ".seleccionado",
	                            String.valueOf(((JCheckBoxMenuItem) menuItem).isSelected()));
	                } else if (menuItem instanceof JRadioButtonMenuItem) {
	                    currentStateToSave.put(claveCompletaMenu + ".seleccionado",
	                            String.valueOf(((JRadioButtonMenuItem) menuItem).isSelected()));
	                }
	             }
	        });
	    } else {
	        System.err.println("Advertencia: view.getMenuItemsPorNombre() es null en guardarConfiguracionActual.");
	    }

	    // --- ¡IMPORTANTE! Asegurarse que el valor de tema.nombre sea el ÚLTIMO actualizado ---
	    // Aunque ya debería estar en el mapa por iniciar con getConfigMap(),
	    // podemos reconfirmarlo obteniéndolo directamente de configuration justo antes de guardar.
	    
	    //LOG -> Valor final de 'tema.nombre' a guardar (desde mapa inicial):
	    System.out.println("  -> Valor final de 'tema.nombre' a guardar (desde mapa inicial): " + currentStateToSave.get("tema.nombre"));
	    
	    String temaActualParaGuardar = configuration.getTemaActual(); // Obtiene el valor más reciente en memoria
	    currentStateToSave.put("tema.nombre", temaActualParaGuardar); // Sobrescribe o añade la clave
	    System.out.println("  -> Valor final de 'tema.nombre' a guardar: " + temaActualParaGuardar);
	    
	    
	    // --- Guardar usando ConfigurationManager ---
	    try {
	        // Pasamos el mapa que hemos construido con el estado actual
	        configuration.guardarConfiguracion(currentStateToSave);
	        System.out.println("Configuración actual guardada exitosamente.");
	    } catch (IOException e) {
	        System.err.println("### ERROR AL GUARDAR CONFIGURACIÓN: " + e.getMessage() + " ###");
	        // Considera mostrar un mensaje al usuario aquí si el guardado falla
	        // JOptionPane.showMessageDialog(view.getFrame(), "Error al guardar la configuración.", "Error", JOptionPane.ERROR_MESSAGE);
	    }
	}


	/**
	 * Carga la lista de imágenes inicial basada en la configuración.
	 */
	private void cargarEstadoInicial ()
	{

		if (configuration == null)
			return;
		String folderInit = configuration.getString("inicio.carpeta", "");

		if (!folderInit.isEmpty())
		{

			cargarCarpetaInicial(folderInit);

		} else
		{

			System.out.println("No hay carpeta inicial definida en la configuración.");
			limpiarUI(); // Llama a método local que actualiza modelo y vista

		}

	}

	/**
	 * Carga el contenido de una carpeta específica.
	 */
	private void cargarCarpetaInicial (String folderPath)
	{

		if (configuration == null)
			return;
		File carpetaInicial = new File(folderPath);

		if (carpetaInicial.exists() && carpetaInicial.isDirectory())
		{

			cargarListaImagenes(); // Carga la lista (actualiza modelo y vista)

			// Selecciona la imagen inicial si está definida
			String imageInitKey = configuration.getString("inicio.imagen", "");
			cargarImagenInicial(imageInitKey); // Selecciona en modelo y actualiza vista

			// Carga miniaturas para la imagen seleccionada (o la primera)
			if (model.getModeloLista().getSize() > 0)
			{

				int indiceInicial = view.getListaImagenes().getSelectedIndex();
				if (indiceInicial < 0)
					indiceInicial = 0;
				actualizarImagenesMiniaturaConRango(indiceInicial); // Carga miniaturas (actualiza vista)

			}

		} else
		{

			System.err.println("La carpeta inicial '" + folderPath + "' no existe o no es un directorio.");
			JOptionPane.showMessageDialog(view.getFrame(),
					"La carpeta inicial configurada\n'" + folderPath + "'\nno es válida.", "Error de Carpeta Inicial",
					JOptionPane.WARNING_MESSAGE);
			limpiarUI();

		}

	}

	/**
	 * Selecciona una imagen en la lista basada en su clave (ruta relativa).
	 */
	private void cargarImagenInicial (String imageKey)
	{

		if (imageKey != null && !imageKey.isEmpty())
		{

			for (int i = 0; i < model.getModeloLista().getSize(); i++)
			{

				String nombreArchivo = model.getModeloLista().getElementAt(i);

				if (nombreArchivo.equals(imageKey))
				{

					// Establece la selección en la VISTA (disparará el listener)
					view.getListaImagenes().setSelectedIndex(i);
					break;

				}

			}

			// Si no se encontró y la lista no está vacía, selecciona el primero
			if (view.getListaImagenes().getSelectedIndex() < 0 && model.getModeloLista().getSize() > 0)
			{

				view.getListaImagenes().setSelectedIndex(0);

			}

		} else if (model.getModeloLista().getSize() > 0)
		{

			// Si no hay imagen inicial especificada, selecciona la primera
			view.getListaImagenes().setSelectedIndex(0);

		}

	}

	/**
	 * Limpia el estado del modelo y actualiza la vista.
	 */
	private void limpiarUI ()
	{

		// Limpiar Modelo
		model.limpiarModeloLista();
		model.limpiarRutaCompletaMap();
		model.setCurrentImage(null);
		model.setSelectedImageKey(null);
		model.resetZoomState();
		// model.setZoomHabilitado(false); // Opcional: desactivar zoom al limpiar

		// Actualizar Vista
		// view.setListaImagenesModel(model.getModeloLista()); // El modelo ya está
		// vacío
		view.limpiarImagenMostrada();
		view.setTextoRuta("");
		view.setTituloPanelIzquierdo("Lista de Archivos");
		view.limpiarPanelMiniaturas();

		 // --- Actualizar estado de locateFileAction ---
        if (locateFileAction instanceof LocateFileAction) {
             ((LocateFileAction) locateFileAction).updateEnabledState(); // Se deshabilitará porque no hay imagen
        }
        // -------------------------------------------
		
		// Desactivar zoom si estaba activo
		// if (model.isZoomHabilitado()) {
		// setManualZoomEnabled(false);
		// }
	}
	
    // --- INICIO CÓDIGO NUEVO/MODIFICADO en VisorController (cargarListaImagenes) ---

    /**
     * Método público o de conveniencia para iniciar la carga usando la
     * carpeta raíz actual y seleccionando la primera imagen.
     */
    private void cargarListaImagenes () {
        System.out.println("  -> cargarListaImagenes() llamado (seleccionará la primera imagen por defecto).");
        cargarListaImagenes(null); // Llama a la versión detallada sin clave específica a mantener
    }


    // --- INICIO CÓDIGO COMPLETO Y CORREGIDO (Versión 3): cargarListaImagenes(String claveImagenAMantener) ---
 // --- Dentro de la clase VisorController ---

 // --- Asegúrate de tener estos imports (o los necesarios según tu estructura) ---
     /**
      * Carga o recarga la lista de imágenes usando un SwingWorker para mostrar progreso.
      * Permite especificar una imagen a mantener seleccionada y ajusta la carpeta
      * de inicio según el modo (Subcarpetas vs Solo Carpeta).
      *
      * @param claveImagenAMantener La clave (ruta relativa a carpetaRaizActual)
      *                             de la imagen que se intentará reseleccionar,
      *                             o null para seleccionar la primera.
      */
     private void cargarListaImagenes(String claveImagenAMantener) { // Inicio del método

         // --- 1. Verificaciones Previas ---
         if (configuration == null || model == null) {
             System.err.println("ERROR: ConfigurationManager o Modelo nulos en cargarListaImagenes.");
             if (view != null) SwingUtilities.invokeLater(this::limpiarUI);
             estaCargandoLista = false; // Asegurar flag a false si salimos
             return;
         }
         System.out.println("\n\n-->>> INICIO CARGAR LISTA DETALLADA (Controller) *** | Mantener Clave: " + claveImagenAMantener);
         this.estaCargandoLista = true; // Marcar como cargando ANTES de empezar

         // --- 2. Cancelar Tareas Anteriores ---
         if (cargaImagenesFuture != null && !cargaImagenesFuture.isDone()) {
              System.out.println("  -> Cancelando tarea de carga de lista anterior...");
              cargaImagenesFuture.cancel(true);
         }
         if (cargaMiniaturasFuture != null && !cargaMiniaturasFuture.isDone()) {
              System.out.println("  -> Cancelando tarea de carga de miniaturas anterior...");
              cargaMiniaturasFuture.cancel(true);
         }

         // --- 3. Determinar Carpeta de Inicio y Profundidad ---
         final boolean mostrarSoloCarpeta = model.isMostrarSoloCarpetaActual();
         Path pathDeInicioWalk = null;
         int depth;

         if (mostrarSoloCarpeta) {
             System.out.println("  -> Modo: Solo Carpeta Actual.");
             depth = 1;
             String claveReferencia = claveImagenAMantener != null ? claveImagenAMantener : model.getSelectedImageKey();
             Path rutaCompletaReferencia = claveReferencia != null ? model.getRutaCompleta(claveReferencia) : null;

             if (rutaCompletaReferencia != null && Files.isRegularFile(rutaCompletaReferencia)) {
                  pathDeInicioWalk = rutaCompletaReferencia.getParent();
                  if (pathDeInicioWalk == null || !Files.isDirectory(pathDeInicioWalk)){
                       System.out.println("    -> No se pudo obtener carpeta padre válida para " + rutaCompletaReferencia + ". Usando raíz actual.");
                       pathDeInicioWalk = this.carpetaRaizActual;
                  } else {
                       System.out.println("    -> Iniciando búsqueda desde carpeta de imagen: " + pathDeInicioWalk);
                  }
             } else {
                 System.out.println("    -> No se pudo obtener carpeta de imagen válida (Clave: " + claveReferencia + "). Usando carpeta raíz actual como fallback.");
                 pathDeInicioWalk = this.carpetaRaizActual;
             }
         } else {
             System.out.println("  -> Modo: Subcarpetas.");
             depth = Integer.MAX_VALUE;
             pathDeInicioWalk = this.carpetaRaizActual;
              if (pathDeInicioWalk == null) {
                  System.out.println("    -> Carpeta raíz actual es null. No se puede iniciar búsqueda.");
                  if (view != null) SwingUtilities.invokeLater(this::limpiarUI);
                  estaCargandoLista = false;
                  return;
              }
             System.out.println("    -> Iniciando búsqueda desde carpeta raíz actual: " + pathDeInicioWalk);
         }

         // --- 4. Validar pathDeInicioWalk Final y Proceder ---
         if (pathDeInicioWalk != null && Files.isDirectory(pathDeInicioWalk)) {

             final Path finalStartPath = pathDeInicioWalk;
             final int finalDepth = depth;
             final String finalClaveImagenAMantener = claveImagenAMantener;

             // --- 5. Limpieza Inicial UI (EDT) ---
             if (view != null) {
                 SwingUtilities.invokeLater(() -> {
                     view.limpiarPanelMiniaturas();
                     view.limpiarImagenMostrada();
                     view.setTextoRuta("");
                     view.setTituloPanelIzquierdo("Iniciando escaneo...");
                 });
             }
             // if (servicioMiniaturas != null) { servicioMiniaturas.limpiarCache(); } // Opcional

             // --- 6. Crear y Configurar el SwingWorker y el Diálogo ---

             // *** PASO 1: Crear el diálogo (aún no visible) ***
             // Pasamos null como worker inicialmente, lo asignaremos después.
             final ProgresoCargaDialog dialogo = new ProgresoCargaDialog(view != null ? view.getFrame() : null, null);

             // *** PASO 2: Crear el Worker, pasando el diálogo ***
             BuscadorArchivosWorker worker = new BuscadorArchivosWorker(
                 finalStartPath,
                 finalDepth,
                 this.carpetaRaizActual,
                 this::esArchivoImagenSoportado,
                 dialogo // Pasamos la instancia del diálogo aquí
             );

             // *** PASO 3: Asignar el worker al diálogo para el botón Cancelar ***
             dialogo.setWorkerAsociado(worker);

             // Guardar la referencia al worker actual
             this.cargaImagenesFuture = worker;

             // *** PASO 4: Añadir el PropertyChangeListener para manejar el estado DONE ***
             worker.addPropertyChangeListener(evt -> {
                 if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE.equals(evt.getNewValue())) {
                     // Este código se ejecuta en el EDT cuando el worker termina

                     dialogo.cerrar(); // Cerrar el diálogo primero

                     if (worker.isCancelled()) {
                         System.out.println("[Controller EDT - done] La tarea fue cancelada.");
                         if(view != null) {
                              limpiarUI();
                              view.setTituloPanelIzquierdo("Carga Cancelada");
                         }
                         estaCargandoLista = false;
                         return;
                     }

                     try {
                         Map<String, Path> mapaResultado = worker.get();

                         if (mapaResultado != null) {
                             DefaultListModel<String> nuevoModeloLista = new DefaultListModel<>();
                             List<String> clavesOrdenadas = new ArrayList<>(mapaResultado.keySet());
                             Collections.sort(clavesOrdenadas);
                             for (String clave : clavesOrdenadas) {
                                 nuevoModeloLista.addElement(clave);
                             }

                             System.out.println("[Controller EDT - done] Actualizando modelo y vista con resultado ("+ nuevoModeloLista.getSize() +" elementos)...");

                             model.actualizarListaCompleta(nuevoModeloLista, mapaResultado);
                             view.setListaImagenesModel(model.getModeloLista());
                             view.setTituloPanelIzquierdo("Archivos: " + model.getModeloLista().getSize());

                             estaCargandoLista = false;
                             System.out.println("-->>> estaCargandoLista = false (en done, después de actualizar UI)");

                             int indiceSeleccionado = -1;
                             DefaultListModel<String> modeloActualizado = model.getModeloLista();
                             if (finalClaveImagenAMantener != null && modeloActualizado.getSize() > 0) {
                                 for (int i = 0; i < modeloActualizado.getSize(); i++) {
                                     if (finalClaveImagenAMantener.equals(modeloActualizado.getElementAt(i))) {
                                         indiceSeleccionado = i; break;
                                     }
                                 }
                                 if (indiceSeleccionado == -1){
                                      System.out.println("    -> Imagen a mantener NO encontrada. Seleccionando índice 0.");
                                      indiceSeleccionado = 0;
                                 } else {
                                      System.out.println("    -> Imagen a mantener encontrada en índice: " + indiceSeleccionado);
                                 }
                             } else if (modeloActualizado.getSize() > 0) {
                                  System.out.println("    -> No había imagen a mantener o lista estaba vacía. Seleccionando índice 0.");
                                  indiceSeleccionado = 0;
                             }

                             if (indiceSeleccionado != -1) {
                                 // Comprobar vista antes de usarla
                                 if (view != null && view.getListaImagenes() != null) {
                                      System.out.println("    -> Aplicando selección al índice: " + indiceSeleccionado);
                                      view.getListaImagenes().setSelectedIndex(indiceSeleccionado);
                                      view.getListaImagenes().ensureIndexIsVisible(indiceSeleccionado);
                                 } else {
                                      System.err.println("WARN [Controller EDT - done]: Vista o JList no disponible al intentar seleccionar índice.");
                                 }
                             } else {
                                 System.out.println("    -->>> Lista vacía tras carga. Limpiando UI.");
                                 limpiarUI();
                             }
                         } else {
                              System.out.println("[Controller EDT - done] El resultado del worker fue null (cancelado?).");
                              if(view != null) { limpiarUI(); view.setTituloPanelIzquierdo("Carga Incompleta/Cancelada"); }
                              estaCargandoLista = false;
                         }
                     } catch (CancellationException ce) {
                         System.out.println("[Controller EDT - done] La tarea fue cancelada (detectado en get).");
                          if(view != null) { limpiarUI(); view.setTituloPanelIzquierdo("Carga Cancelada"); }
                         estaCargandoLista = false;
                     } catch (InterruptedException ie) {
                         System.err.println("[Controller EDT - done] Hilo interrumpido mientras esperaba resultado.");
                          if(view != null) { limpiarUI(); view.setTituloPanelIzquierdo("Carga Interrumpida"); }
                         Thread.currentThread().interrupt();
                         estaCargandoLista = false;
                     } catch (ExecutionException ee) {
                         System.err.println("[Controller EDT - done] Error durante la ejecución del worker: " + ee.getCause());
                         Throwable causa = ee.getCause();
                         String mensajeError = (causa != null) ? causa.getMessage() : ee.getMessage();
                         if(view != null) {
                             JOptionPane.showMessageDialog(view.getFrame(),
                                                           "Error durante la carga:\n" + mensajeError,
                                                           "Error de Carga", JOptionPane.ERROR_MESSAGE);
                             limpiarUI();
                             view.setTituloPanelIzquierdo("Error de Carga");
                         }
                         if (causa != null) causa.printStackTrace(); else ee.printStackTrace();
                         estaCargandoLista = false;
                     } finally {
                         if (estaCargandoLista) {
                              System.out.println("WARN [Controller EDT - done]: estaCargandoLista aún era true al final. Forzando a false.");
                              estaCargandoLista = false;
                         }
                          if (cargaImagenesFuture == worker) {
                              cargaImagenesFuture = null;
                          }
                     }
                 } // Fin if ("state" == DONE)
             }); // Fin addPropertyChangeListener

             // --- 7. Ejecutar Worker y Mostrar Diálogo ---
             worker.execute(); // Iniciar la tarea en segundo plano

             SwingUtilities.invokeLater(() -> {
                 System.out.println("[Controller EDT] Mostrando diálogo de progreso...");
                 dialogo.setVisible(true); // Mostrar el diálogo modal
                 // La ejecución se reanuda aquí cuando el diálogo se cierra desde el listener
                  System.out.println("[Controller EDT] Diálogo de progreso cerrado.");
             });

         } else { // Else (pathDeInicioWalk NO válido)
             System.out.println("No se puede cargar la lista: Carpeta de inicio no válida o no establecida.");
             if (view != null) SwingUtilities.invokeLater(this::limpiarUI);
             estaCargandoLista = false;
         } // Fin else (pathDeInicioWalk NO válido)

     } // Fin del método cargarListaImagenes(String claveImagenAMantener)




    // Asegúrate de que el método esArchivoImagenSoportado siga existiendo en VisorController
	/**
	 * Verifica si un archivo tiene una extensión de imagen soportada.
	 */
	private boolean esArchivoImagenSoportado (Path path)
	{
		String fileName = path.getFileName().toString().toLowerCase();
		return fileName.endsWith(".jpg") ||
				fileName.endsWith(".jpeg") ||
				fileName.endsWith(".png") ||
				fileName.endsWith(".gif") ||
				fileName.endsWith(".bmp");
	}

    // El método limpiarUI() sigue igual
    // ...
    // --- FIN CÓDIGO COMPLETO Y CORREGIDO (Versión 3): cargarListaImagenes(String claveImagenAMantener) ---
    
	
	
// --- MÉTODO: cargarListaImagenes --- 
    /**
     * @deprecated 
     * 
     * Carga o recarga la lista de imágenes desde la carpeta configurada.
     * Funciona en segundo plano para no bloquear la interfaz de usuario.
     * Actualiza el modelo de datos (lista de archivos y rutas) y la vista (JList, título).
     * Después de cargar, selecciona automáticamente el primer elemento de la nueva lista.
     */
    private void cargarListaImagenesOLD () {

        // --- 1. Verificaciones Previas ---
        if (configuration == null) {
            System.err.println("ERROR: ConfigurationManager nulo en cargarListaImagenes.");
            if (view != null) {
                JOptionPane.showMessageDialog(view.getFrame(), "Error interno de configuración.", "Error", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }
        System.out.println("\n\n-->>> INICIO CARGAR LISTA (Controller) ***");
        this.estaCargandoLista = true; // Indicar que la carga ha comenzado

        // --- 2. Cancelar Tareas Anteriores Pendientes ---
        // Evita que cargas antiguas interfieran con la nueva
        if (cargaImagenesFuture != null && !cargaImagenesFuture.isDone()) {
            System.out.println("[Controller] Cancelando tarea de carga de lista anterior...");
            cargaImagenesFuture.cancel(true);
        }
        if (cargaMiniaturasFuture != null && !cargaMiniaturasFuture.isDone()) {
            System.out.println("[Controller] Cancelando tarea de carga de miniaturas anterior...");
            cargaMiniaturasFuture.cancel(true);
        }

        // --- 3. Obtener Configuración y Validar Carpeta ---
        final String carpetaInicial = configuration.getString("inicio.carpeta", "");
        // Leer modo desde el MODELO (importante que el modelo ya tenga el estado correcto)
        final boolean usarSoloCarpetaActual = model.isMostrarSoloCarpetaActual();
        Path startPath = null;
        if (carpetaInicial != null && !carpetaInicial.isEmpty()) {
            try {
                startPath = Paths.get(carpetaInicial);
                if (!Files.isDirectory(startPath)) {
                    System.err.println("Error: La ruta inicial no es un directorio válido: " + carpetaInicial);
                    startPath = null;
                }
            } catch (java.nio.file.InvalidPathException ipe) {
                System.err.println("Error: La ruta inicial no es válida: " + carpetaInicial + " - " + ipe.getMessage());
                startPath = null;
            }
        }

        // --- 4. Proceder solo si la Carpeta es Válida ---
        if (startPath != null) {
            final Path finalStartPath = startPath; // Necesario para usar dentro de la lambda

            // --- 5. Limpieza Inicial de UI y Modelo (en el Hilo de Eventos de Swing - EDT) ---
            // Prepara la interfaz para la nueva carga
            SwingUtilities.invokeLater(() -> {
                if (view != null) { // Comprobación extra por si acaso
                    view.limpiarPanelMiniaturas();
                    view.limpiarImagenMostrada();
                    view.setTextoRuta("");
                    view.setTituloPanelIzquierdo("Cargando Lista...");
                }
                if (model != null) { // Comprobación extra
                    model.limpiarModeloLista();
                    model.limpiarRutaCompletaMap();
                }
            });

            // --- 6. Iniciar Tarea de Búsqueda de Archivos en Segundo Plano ---
            cargaImagenesFuture = executorService.submit(() -> {
                // Colecciones temporales SÓLO para este hilo de fondo
                DefaultListModel<String> nuevoModeloTemp = new DefaultListModel<>();
                Map<String, Path> nuevoRutaCompletaMapTemp = new HashMap<>();
                Set<String> archivosAgregadosTemp = new HashSet<>(); // Evita duplicados

                try {
                    // --- 7. Recorrer Sistema de Archivos (Files.walk) ---
                    int depth = usarSoloCarpetaActual ? 1 : Integer.MAX_VALUE; // Profundidad según modo
                    System.out.println("[Controller BG] Iniciando Files.walk con depth: " + depth + " en " + finalStartPath);

                    try (Stream<Path> stream = Files.walk(finalStartPath, depth)) {
                        stream
                            .filter(path -> !path.equals(finalStartPath) && Files.isRegularFile(path)) // Solo archivos, no el directorio raíz
                            .filter(this::esArchivoImagenSoportado) // Filtrar por extensión
                            .forEach(path -> {
                                // Comprobar interrupción dentro del bucle
                                if (Thread.currentThread().isInterrupted()) {
                                    System.out.println("[Controller BG] Tarea interrumpida durante forEach.");
                                    throw new RuntimeException("Tarea cancelada"); // Para salir del forEach
                                }
                                // Generar clave única (ruta relativa)
                                Path relativePath = finalStartPath.relativize(path);
                                String uniqueKey = relativePath.toString().replace("\\", "/");

                                // Añadir a colecciones temporales si no existe ya
                                if (archivosAgregadosTemp.add(uniqueKey)) {
                                    nuevoModeloTemp.addElement(uniqueKey);
                                    nuevoRutaCompletaMapTemp.put(uniqueKey, path);
                                } else {
                                     System.out.println("[Controller BG] Clave duplicada ignorada: " + uniqueKey);
                                }
                            });
                    } catch (IOException | SecurityException ioOrSecEx) { // Captura errores de walk
                        System.err.println("[Controller BG] Error durante Files.walk en " + finalStartPath + ": " + ioOrSecEx.getMessage());
                        String errorType = (ioOrSecEx instanceof IOException) ? "Error al leer directorio" : "Error de permisos directorio";
                        throw new RuntimeException(errorType, ioOrSecEx); // Re-lanzar para manejo general
                    }

                    System.out.println("[Controller BG] Files.walk terminado. Elementos encontrados: " + nuevoModeloTemp.getSize());

                    // --- 8. Actualizar Modelo y Vista (Solo si la tarea no fue cancelada) ---
                    if (!Thread.currentThread().isInterrupted()) {
                        System.out.println("[Controller BG] Modelo temporal llenado. Tamaño: " + nuevoModeloTemp.getSize());

                        // Pasar datos al Hilo de Eventos de Swing (EDT) para actualizar UI
                        final DefaultListModel<String> finalNuevoModeloTemp = nuevoModeloTemp;
                        final Map<String, Path> finalNuevoRutaCompletaMapTemp = nuevoRutaCompletaMapTemp;

                        SwingUtilities.invokeLater(() -> { // <--- Bloque de actualización en EDT
                            // Verificar que los componentes aún existen
                            if (model == null || view == null || view.getListaImagenes() == null) {
                                System.err.println("ERROR [Controller EDT]: Modelo o Vista no disponibles al actualizar tras carga.");
                                estaCargandoLista = false;
                                return;
                            }

                            // --- 8a. Actualizar el Modelo Principal ---
                            DefaultListModel<String> modeloActual = model.getModeloLista();
                            modeloActual.clear(); // Limpiar datos antiguos
                            for (int idx = 0; idx < finalNuevoModeloTemp.getSize(); idx++) {
                                modeloActual.addElement(finalNuevoModeloTemp.getElementAt(idx)); // Añadir nuevos
                            }
                            model.setRutaCompletaMap(finalNuevoRutaCompletaMapTemp); // Actualizar mapa de rutas
                            System.out.println("[Controller EDT] Contenido del modelo principal actualizado. Tamaño: " + modeloActual.getSize());

                            // --- 8b. Actualizar Título de la Vista ---
                            view.setTituloPanelIzquierdo("Archivos: " + modeloActual.getSize());
                            System.out.println("  -> Limpiando selectedImageKey en modelo antes de seleccionar índice...");
                            model.setSelectedImageKey(null); // Forzará la recarga en el listener
                            
                            // --- 8c. Marcar Fin de Carga Lógica ---
                            // Importante hacerlo ANTES de cambiar la selección para que los listeners sepan que no estamos cargando
                            estaCargandoLista = false;
                            System.out.println("-->>> estaCargandoLista = false (después de actualizar modelo)");

                            // --- 8d. Seleccionar el Primer Elemento (¡LA MODIFICACIÓN CLAVE!) ---
                            if (modeloActual.getSize() > 0) {
                                System.out.println("[Controller EDT] Seleccionando índice 0 en la nueva lista...");
                                view.getListaImagenes().setSelectedIndex(0); // Establece la selección
                                // La línea anterior disparará automáticamente el ListSelectionListener,
                                // que llamará a mostrarImagenSeleccionada() para cargar la imagen 0.
                            } else {
                                // Si la lista quedó vacía después de la carga
                                System.out.println("-->>> Lista vacía tras carga. Limpiando UI.");
                                limpiarUI(); // Limpiar imagen, ruta, miniaturas, etc.
                            }
                            // --- FIN MODIFICACIÓN ---

                        }); // <-- FIN del invokeLater para actualizar UI

                    } else {
                        // --- 9a. Manejo si la Tarea fue Interrumpida Antes de Actualizar UI ---
                        System.out.println("[Controller BG] Tarea interrumpida ANTES de actualizar UI.");
                        SwingUtilities.invokeLater(() -> {
                            if(view != null) view.setTituloPanelIzquierdo("Carga Cancelada");
                            estaCargandoLista = false;
                             System.out.println("-->>> estaCargandoLista = false (cancelado antes de UI update)");
                            limpiarUI();
                        });
                    }

                } catch (RuntimeException e) { // Captura errores generales de la tarea (walk, interrupción forEach)
                    // --- 9b. Manejo de Errores de la Tarea Background ---
                    final String errorMsg = e.getMessage();
                    System.err.println("[Controller BG] Error en tarea background: " + errorMsg);
                    SwingUtilities.invokeLater(() -> { // Actualizar UI sobre el error en EDT
                         if(view != null) {
                             String dialogTitle = "Error de Carga";
                             String dialogMessage = "Error durante la carga: " + errorMsg;
                             // ... (código para personalizar mensaje de error según causa, igual que antes) ...
                              if ("Tarea cancelada".equals(errorMsg)) {
                                   view.setTituloPanelIzquierdo("Carga Cancelada");
                                   // No mostrar diálogo por cancelación
                              } else {
                                   JOptionPane.showMessageDialog(view.getFrame(), dialogMessage, dialogTitle, JOptionPane.ERROR_MESSAGE);
                                   view.setTituloPanelIzquierdo("Error de Carga");
                              }
                         }
                         estaCargandoLista = false;
                         System.out.println("-->>> estaCargandoLista = false (por error/cancelación en BG)");
                         limpiarUI(); // Limpiar en caso de error grave
                    });
                    if (!"Tarea cancelada".equals(errorMsg)) { e.printStackTrace(); } // Loguear error si no fue cancelación
                } // Fin try-catch principal de la tarea background

            }); // Fin del submit (inicio de la tarea background)

        } else {
            // --- 10. Caso: Carpeta Inicial Inválida ---
            System.out.println("No se puede cargar la lista: Carpeta inicial no definida o inválida: \"" + carpetaInicial + "\"");
            SwingUtilities.invokeLater(this::limpiarUI); // Limpiar UI en EDT
            estaCargandoLista = false; // Marcar como no cargando
        }
        System.out.println("-->>> FIN MÉTODO cargarListaImagenes (Controller) ***");
    }// --- FIN MÉTODO cargarListaImagenes ---
	

	/**
	 * Muestra la imagen seleccionada en la lista.
	 */
	private void mostrarImagenSeleccionada() {
	    // Obtener clave seleccionada de la VISTA (sin cambios)
	    String archivoSeleccionadoKey = view.getListaImagenes().getSelectedValue();

	    if (archivoSeleccionadoKey == null) return; // No hay selección

	    // --- CANCELAR CARGA ANTERIOR ---
	    if (cargaImagenPrincipalFuture != null && !cargaImagenPrincipalFuture.isDone()) {
	        System.out.println("[Controller] Cancelando carga de imagen principal anterior...");
	        cargaImagenPrincipalFuture.cancel(true);
	    }
	    // ------------------------------

	    // Evitar recarga si ya es la misma clave Y la imagen ya está cargada en el modelo
	    // (Podríamos incluso comprobar si la imagen en la VISTA es la correcta)
	    if (archivoSeleccionadoKey.equals(model.getSelectedImageKey()) && model.getCurrentImage() != null) {
	         System.out.println("[Controller] Imagen " + archivoSeleccionadoKey + " ya está seleccionada y cargada.");
	         // Podríamos forzar repintado por si acaso, pero usualmente no es necesario
	         // view.getEtiquetaImagen().repaint();
	        return;
	    }

	    System.out.println("\n[Controller] Iniciando carga para: '" + archivoSeleccionadoKey + "'");

	    
	    // --- Actualizar estado de locateFileAction ---
        if (locateFileAction instanceof LocateFileAction) { // Buena práctica comprobar tipo
             ((LocateFileAction) locateFileAction).updateEnabledState();
        }
        // ------------------------------
	    
	    // --- Actualizar Modelo y UI INMEDIATAMENTE (antes de cargar) ---
	    model.setSelectedImageKey(archivoSeleccionadoKey); // Actualizar clave seleccionada
	    model.setCurrentImage(null); // Limpiar imagen anterior del modelo
	    Path rutaCompleta = model.getRutaCompleta(archivoSeleccionadoKey);

	    if (rutaCompleta != null) {
	        view.setTextoRuta(rutaCompleta.toString()); // Mostrar ruta
	        // Mostrar indicador de carga en la vista
	        view.mostrarIndicadorCargaImagenPrincipal("Cargando: " + rutaCompleta.getFileName() + "...");
	    } else {
	        System.err.println("WARN: No se encontró ruta para la clave: " + archivoSeleccionadoKey);
	        model.setSelectedImageKey(null);
	        view.limpiarImagenMostrada();
	        view.setTextoRuta("Error: Ruta no encontrada");
	        return; // Salir si no hay ruta
	    }
	    // -------------------------------------------------------------


	    // --- Iniciar Tarea de Carga en Background ---
	    final String finalKey = archivoSeleccionadoKey; // Clave para usar en lambda
	    final Path finalPath = rutaCompleta;           // Ruta para usar en lambda

	    cargaImagenPrincipalFuture = executorService.submit(() -> {
	        System.out.println("  [BG Carga Imagen] Iniciando lectura para: " + finalPath);
	        BufferedImage img = null;
	        String errorMsg = null;

	        try {
	            // --- Carga Real ---
	            img = ImageIO.read(finalPath.toFile());
	            // ---------------

	            if (Thread.currentThread().isInterrupted()) {
	            	
	            	//LOG [BG Carga Imagen] Tarea interrumpida después de leer.
	                 System.out.println("  [BG Carga Imagen] Tarea interrumpida después de leer.");
	                 
	                 return; // Salir si se canceló durante la carga
	            }

	            if (img == null) {
	                 errorMsg = "Formato no soportado o archivo corrupto.";
	                 System.err.println("  [BG Carga Imagen] Error: " + errorMsg + " para " + finalPath);
	            } else {
	                 System.out.println("  [BG Carga Imagen] Imagen leída correctamente.");
	            }

	        } catch (IOException ioEx) {
	            errorMsg = "Error de E/S: " + ioEx.getMessage();
	            System.err.println("  [BG Carga Imagen] " + errorMsg + " para " + finalPath);
	        } catch (OutOfMemoryError oom) {
	             errorMsg = "Memoria insuficiente para cargar la imagen.";
	             System.err.println("  [BG Carga Imagen] " + errorMsg + " para " + finalPath);
	             // Podrías intentar liberar caches aquí si tienes
	             // System.gc(); // No recomendado generalmente, pero opción extrema
	        } catch (Exception ex) {
	             errorMsg = "Error inesperado: " + ex.getMessage();
	             System.err.println("  [BG Carga Imagen] " + errorMsg + " para " + finalPath);
	             ex.printStackTrace();
	        }

	        // --- Actualizar Modelo y Vista en EDT ---
	        final BufferedImage finalImg = img; // Variable final para lambda
	        final String finalErrorMsg = errorMsg;

	        // Solo actualizar si la imagen que acabamos de cargar sigue siendo la seleccionada
	        // y si la tarea no fue cancelada.
	         if (!Thread.currentThread().isInterrupted() && finalKey.equals(model.getSelectedImageKey())) {
	            SwingUtilities.invokeLater(() -> {
	                if (finalImg != null) 
	                {
	                    // --- Éxito ---
	                	
	                	//LOG   [EDT Carga Imagen] Asignando imagen al modelo y vista
	                    //System.out.println("  [EDT Carga Imagen] Asignando imagen al modelo y vista.");
	                    model.setCurrentImage(finalImg); // Actualizar modelo

	                    // Resetear zoom si no está habilitado el modo manual
	                    if (!model.isZoomHabilitado()) {
	                        model.resetZoomState();
	                    }

	                    // Calcular imagen reescalada (usar método existente)
	                    Image reescalada = reescalarImagenParaAjustar(); // Puede devolver null si la vista no tiene tamaño

	                    // Actualizar VISTA con la nueva imagen y estado de zoom del modelo
	                    if (reescalada != null) {
	                         view.setImagenMostrada(reescalada, model.getZoomFactor(), model.getImageOffsetX(), model.getImageOffsetY());
	                    } else {
	                         // Si no se pudo reescalar (vista sin tamaño?), mostrar original o limpiar?
	                         // Por ahora limpiamos para evitar problemas.
	                    	
	                    	//LOG   [EDT Carga Imagen] No se pudo reescalar, limpiando vista
	                         //System.out.println("  [EDT Carga Imagen] No se pudo reescalar, limpiando vista.");
	                    	
	                         view.limpiarImagenMostrada();
	                         // O podrías intentar un repaint posterior:
	                         // SwingUtilities.invokeLater(this::mostrarImagenSeleccionada); // Intentar de nuevo más tarde
	                    }
	                } else {
	                    // --- Error ---
	                	//LOG   [EDT Carga Imagen] Error al cargar, limpiando modelo y vista.
	                    System.out.println("  [EDT Carga Imagen] Error al cargar, limpiando modelo y vista.");
	                    
	                    model.setCurrentImage(null); // Limpiar modelo
	                    view.limpiarImagenMostrada(); // Limpiar vista
	                    view.setTextoRuta("Error al cargar: " + finalPath.getFileName() + (finalErrorMsg != null ? " ("+finalErrorMsg+")" : ""));
	                    // Mostrar diálogo de error
//	                    JOptionPane.showMessageDialog(view.getFrame(),
//	                        "Error al cargar la imagen:\n" + finalPath.getFileName() +
//	                        (finalErrorMsg != null ? "\n" + finalErrorMsg : ""),
//	                        "Error de Carga", JOptionPane.ERROR_MESSAGE);
	                     // Resetear zoom por si acaso
	                     model.resetZoomState();
	                     if(model.isZoomHabilitado()) setManualZoomEnabled(false);
	                }
	            });
	         } else {
	        	 
	        	 //LOG   [BG Carga Imagen] Carga cancelada o selección cambiada. Descartando resultado para 
	             System.out.println("  [BG Carga Imagen] Carga cancelada o selección cambiada. Descartando resultado para " + finalKey);
	         }
	    }); // Fin submit
	}

	
	

	/**
	 * Calcula la imagen reescalada basada en la imagen actual del modelo y el
	 * tamaño de la etiqueta de la vista.
	 * 
	 * @return La imagen reescalada o null.
	 */
    // --- INICIO CÓDIGO A MODIFICAR en VisorController.reescalarImagenParaAjustar ---
    /**
     * Calcula la imagen reescalada basada en la imagen actual del modelo,
     * el tamaño de la etiqueta de la vista y el estado de 'mantenerProporcion' del modelo.
     *
     * @return La imagen reescalada o null si no hay imagen o la vista no tiene tamaño.
     */
    private Image reescalarImagenParaAjustar() {
        if (model == null || view == null || view.getEtiquetaImagen() == null) return null; // Verificar todo

        BufferedImage current = model.getCurrentImage();
        if (current == null) return null;

        int targetWidth = view.getEtiquetaImagen().getWidth();
        int targetHeight = view.getEtiquetaImagen().getHeight();

        if (targetWidth <= 0 || targetHeight <= 0) {
            System.out.println("[reescalar] WARN: Etiqueta sin tamaño aún. No se puede escalar.");
            return null; // No podemos escalar si no hay tamaño de destino
        }

        // --- NUEVA LÓGICA DE ESCALADO ---
        int newWidth;
        int newHeight;
        boolean mantenerProp = model.isMantenerProporcion(); // Leer estado del modelo
        System.out.println("[reescalar] Escalando a " + targetWidth + "x" + targetHeight + " | MantenerProporcion=" + mantenerProp);


        if (mantenerProp) {
            // --- LÓGICA ANTIGUA (MANTIENE PROPORCIÓN) ---
            double imageAspectRatio = (double) current.getWidth() / current.getHeight();
            double panelAspectRatio = (double) targetWidth / targetHeight;

            if (panelAspectRatio > imageAspectRatio) { // Panel más ancho que imagen (relativamente) -> ajustar a ALTO
                newHeight = targetHeight;
                newWidth = (int) (targetHeight * imageAspectRatio);
            } else { // Panel más alto que imagen (relativamente) -> ajustar a ANCHO
                newWidth = targetWidth;
                //newHeight = (int) (targetWidth / imageAspectRatio); // <-- ¡CORRECCIÓN! Aquí debe ser targetWidth
                // Corrección:
                newHeight = (int) (targetWidth / imageAspectRatio);
            }
             System.out.println("  -> Con proporción: newW=" + newWidth + ", newH=" + newHeight);
            // Asegurarse de que al menos sea 1x1 si los cálculos dan 0
            newWidth = Math.max(1, newWidth);
            newHeight = Math.max(1, newHeight);
            // -------------------------------------------

        } else {
            // --- NUEVA LÓGICA (NO MANTIENE PROPORCIÓN - ESTIRA/ENCOGE) ---
            // Simplemente usar el tamaño del contenedor
            newWidth = targetWidth;
            newHeight = targetHeight;
             System.out.println("  -> SIN proporción: newW=" + newWidth + ", newH=" + newHeight);
            // ---------------------------------------------------------
        }

        // Usar SCALE_SMOOTH para mejor calidad
        // Importante: getScaledInstance puede ser lento y asíncrono a veces.
        // Si tienes problemas de rendimiento o parpadeo, investiga alternativas
        // como usar AffineTransform o librerías como imgscalr.
        try {
             return current.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        } catch (Exception e) {
             System.err.println("ERROR [reescalar] Excepción en getScaledInstance: " + e.getMessage());
             // Devolver original como fallback o null? Null es más seguro.
             return null;
        }
    }
    
	
	public void setManualZoomEnabled(boolean enable) 
	{
        //LOG [Controller] setManualZoomEnabled
		//System.out.println("[Controller] setManualZoomEnabled llamado con: " + enable);

        // 1. Actualizar Modelo
        model.setZoomHabilitado(enable);

        // 2. Actualizar Estado de Actions relacionadas
        // La Action de Reset se habilita/deshabilita
        if(resetZoomAction != null) resetZoomAction.setEnabled(enable);
        // El estado SELECTED de la Action de toggle se actualiza (lo hace su propio actionPerformed)
        
        // Actualizar Action de Toggle (SELECTED_KEY)
        if(toggleZoomManualAction != null) toggleZoomManualAction.putValue(Action.SELECTED_KEY, enable);
        
        // 3. Actualizar la VISTA (Componentes que NO usan Action directamente o necesitan UI específica)
        if (view != null) {
            // El botón de zoom cambia su fondo (esto no lo hace Action automáticamente)
             view.actualizarEstadoControlesZoom(enable, enable); // Actualiza fondo botón zoom
             // El checkbox del menú debería actualizarse automáticamente si usa setAction(toggleZoomManualAction)
             // view.setEstadoMenuActivarZoomManual(enable); // No debería ser necesario si usa Action
        }

        // 4. Lógica adicional
        if (enable) {
            if (!model.isZoomHabilitado()) { // Solo resetear si realmente se activó
            	
            	//LOG Zoom Manual activado. Aplicando reset inicial
                //System.out.println("Zoom Manual activado. Aplicando reset inicial...");
                
            	aplicarResetZoomAction(); // Llama a lógica de reseteo
            }
        } else {
        	//LOG Zoom Manual desactivado. Reajustando imagen
            //System.out.println("Zoom Manual desactivado. Reajustando imagen...");
            
            model.resetZoomState();
            Image reescalada = reescalarImagenParaAjustar();
            if (view != null) {
                view.setImagenMostrada(reescalada, model.getZoomFactor(), model.getImageOffsetX(), model.getImageOffsetY());
            }
        }
        //LOG [Controller] setManualZoomEnabled terminado.
         //System.out.println("[Controller] setManualZoomEnabled terminado.");
    }
	
	
	/**
	 * Ejecuta la acción de resetear el zoom/offset.
	 */
	public void aplicarResetZoomAction ()
	{

		System.out.println("Ejecutando acción de Reset Zoom...");
		model.resetZoomState();
		Image reescalada = reescalarImagenParaAjustar(); // Recalcular imagen base
		view.setImagenMostrada(reescalada, model.getZoomFactor(), model.getImageOffsetX(), model.getImageOffsetY());
		view.aplicarAnimacionBoton("Reset_48x48");
	}

	
    // --- INICIO MÉTODO COMPLETO Y CORREGIDO: abrirSelectorDeCarpeta ---
    /**
     * Abre el selector de carpetas del sistema, permitiendo al usuario
     * elegir una nueva carpeta raíz. Actualiza la variable interna
     * 'carpetaRaizActual', guarda la nueva ruta en la configuración
     * y recarga la lista de imágenes.
     */
    public void abrirSelectorDeCarpeta() {
        if (configuration == null) {
             System.err.println("ERROR: ConfigurationManager nulo en abrirSelectorDeCarpeta.");
             return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Seleccionar Carpeta Raíz");

        // Establecer directorio inicial del selector:
        // 1. Usar la carpeta raíz actual si es válida.
        // 2. Si no, intentar usar la de la configuración.
        // 3. Si no, usar el directorio por defecto del JFileChooser.
        Path currentDirToShow = null;
        if (this.carpetaRaizActual != null && Files.isDirectory(this.carpetaRaizActual)) {
            currentDirToShow = this.carpetaRaizActual;
        } else {
            String folderInitConfig = configuration.getString("inicio.carpeta", "");
            if (!folderInitConfig.isEmpty()) {
                try {
                    Path configPath = Paths.get(folderInitConfig);
                    if (Files.isDirectory(configPath)) {
                        currentDirToShow = configPath;
                    }
                } catch (Exception e) { /* Ignorar ruta inválida en config */ }
            }
        }
        if (currentDirToShow != null) {
            fileChooser.setCurrentDirectory(currentDirToShow.toFile());
            System.out.println("  -> JFileChooser iniciado en: " + currentDirToShow);
        } else {
             System.out.println("  -> JFileChooser iniciado en directorio por defecto.");
        }


        int resultado = fileChooser.showOpenDialog(view != null ? view.getFrame() : null);

        if (resultado == JFileChooser.APPROVE_OPTION) {
            File carpetaSeleccionada = fileChooser.getSelectedFile();

            if (carpetaSeleccionada != null && carpetaSeleccionada.isDirectory()) {
                String rutaCarpeta = carpetaSeleccionada.getAbsolutePath();
                Path nuevaCarpetaRaiz = carpetaSeleccionada.toPath();

                // Solo proceder si la carpeta seleccionada es diferente de la actual
                if (!nuevaCarpetaRaiz.equals(this.carpetaRaizActual)) {
                    System.out.println("[Controller] Nueva Carpeta Raíz seleccionada: " + nuevaCarpetaRaiz);

                    // Actualizar la variable de instancia
                    this.carpetaRaizActual = nuevaCarpetaRaiz;

                    // Actualizar y guardar configuración
                    configuration.setInicioCarpeta(rutaCarpeta); // Actualiza en memoria
                    try {
                        // Usar getConfigMap() para obtener todos los datos actuales antes de guardar
                        configuration.guardarConfiguracion(configuration.getConfigMap());
                        System.out.println("  -> Nueva carpeta inicial guardada en config.cfg: " + rutaCarpeta);
                    } catch (IOException e) {
                        System.err.println("  -> ERROR al guardar nueva carpeta inicial en config.cfg: " + e.getMessage());
                         if(view != null) JOptionPane.showMessageDialog(view.getFrame(), "No se pudo guardar la nueva carpeta inicial.", "Error", JOptionPane.WARNING_MESSAGE);
                    }

                    // Recargar la lista desde la nueva raíz, seleccionando la primera imagen
                    System.out.println("  -> Llamando a cargarListaImagenes(null) para la nueva raíz...");
                    cargarListaImagenes(null); // Pasar null para seleccionar la primera imagen

                } else {
                    System.out.println("[Controller] La carpeta seleccionada es la misma que la actual. No se recarga.");
                }

            } else if (carpetaSeleccionada != null) { // Si seleccionó un archivo o algo inválido
                 if(view != null) JOptionPane.showMessageDialog(view.getFrame(), "Debe seleccionar una carpeta válida.", "Selección Inválida", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            System.out.println("[Controller] Selección de carpeta cancelada por el usuario.");
        }
    }
    // --- FIN MÉTODO: abrirSelectorDeCarpeta ---
	

	/**
	 * Abre el selector de carpetas y carga la seleccionada.
	 */
	public void abrirSelectorDeCarpetaOLD ()
	{

		if (configuration == null) 
		{
			System.err.println("ERROR: ConfigurationManager nulo en abrirSelectorDeCarpeta.");
			return;
		}
		
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		String folderInit = configuration.getString("inicio.carpeta", "");

		if (folderInit != null && !folderInit.isEmpty())
		{

			File initDir = new File(folderInit);

			if (initDir.isDirectory())
			{

				fileChooser.setCurrentDirectory(initDir);

			}

		}

		int resultado = fileChooser.showOpenDialog(view.getFrame());

		if (resultado == JFileChooser.APPROVE_OPTION)
		{

			File carpetaSeleccionada = fileChooser.getSelectedFile();

			if (carpetaSeleccionada != null)
			{

				String rutaCarpeta = carpetaSeleccionada.getAbsolutePath();
				// Actualizar configuración EN MEMORIA y persistir
				configuration.setInicioCarpeta(rutaCarpeta);

				// Guardar inmediatamente la nueva carpeta inicial
				try
				{

					Map<String, String> currentConfig = configuration.getConfigMap(); // Obtiene copia
					currentConfig.put("inicio.carpeta", rutaCarpeta); // Actualiza la clave
					configuration.guardarConfiguracion(currentConfig); // Guarda el mapa modificado
					System.out.println("Nueva carpeta inicial guardada: " + rutaCarpeta);

				} catch (IOException e)
				{

					System.err.println("Error al guardar nueva carpeta inicial: " + e.getMessage());
					JOptionPane.showMessageDialog(view.getFrame(), "No se pudo guardar la nueva carpeta inicial.",
							"Error", JOptionPane.WARNING_MESSAGE);

				}

				cargarListaImagenes(); // Recargar con la nueva carpeta

			}

		}

	}

	/**
	 * Muestra un diálogo con la lista de archivos.
	 */
	private void mostrarDialogoListaImagenes ()
	{

		JDialog dialogoLista = new JDialog(view.getFrame(), "Lista de Imágenes", true); // Modal
		dialogoLista.setSize(600, 400);
		dialogoLista.setLocationRelativeTo(view.getFrame());

		DefaultListModel<String> modeloListaDialogo = new DefaultListModel<>();
		JList<String> listaImagenesDialogo = new JList<>(modeloListaDialogo);
		JScrollPane scrollPaneListaDialogo = new JScrollPane(listaImagenesDialogo);
		JCheckBox checkBoxMostrarRutas = new JCheckBox("Mostrar Rutas Completas");

		JButton botonCopiarLista = new JButton("Copiar Lista");
		// Pasamos el modelo del diálogo al método de copiar
		botonCopiarLista.addActionListener(e -> copiarListaAlPortapapeles(modeloListaDialogo));

		JPanel panelSuperiorDialog = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panelSuperiorDialog.add(botonCopiarLista);
		panelSuperiorDialog.add(checkBoxMostrarRutas);

		dialogoLista.add(panelSuperiorDialog, BorderLayout.NORTH);
		dialogoLista.add(scrollPaneListaDialogo, BorderLayout.CENTER);

		checkBoxMostrarRutas.addActionListener(e -> {

			// Llama al método local que lee del modelo principal
			actualizarListaEnDialogo(modeloListaDialogo, checkBoxMostrarRutas.isSelected());

		});

		// Carga inicial del diálogo
		actualizarListaEnDialogo(modeloListaDialogo, checkBoxMostrarRutas.isSelected());
		dialogoLista.setVisible(true); // Hacer visible al final

	}

	
	/**
	 * Actualiza el modelo del diálogo de lista.
	 */
	private void actualizarListaEnDialogo (DefaultListModel<String> modeloDialogo, boolean mostrarRutas)
	{

		modeloDialogo.clear();
		DefaultListModel<String> modeloPrincipal = model.getModeloLista(); // Obtener modelo principal
		Map<String, Path> mapaRutas = model.getRutaCompletaMap(); // Obtener mapa de rutas

		for (int i = 0; i < modeloPrincipal.getSize(); i++)
		{

			String nombreArchivoKey = modeloPrincipal.getElementAt(i);
			Path rutaCompleta = mapaRutas.get(nombreArchivoKey);
			String textoAAgregar = nombreArchivoKey; // Por defecto, la clave/ruta relativa

			if (mostrarRutas && rutaCompleta != null)
			{

				textoAAgregar = rutaCompleta.toString();

			} else if (mostrarRutas && rutaCompleta == null)
			{

				textoAAgregar = nombreArchivoKey + " (¡Ruta no encontrada!)";

			}
			modeloDialogo.addElement(textoAAgregar);

		}

	}

	
	/**
	 * Copia el contenido de un DefaultListModel al portapapeles.
	 */
	private void copiarListaAlPortapapeles (DefaultListModel<String> listModel)
	{

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < listModel.getSize(); i++)
		{

			sb.append(listModel.getElementAt(i)).append("\n");

		}
		StringSelection stringSelection = new StringSelection(sb.toString());
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, this);
		System.out.println("Lista copiada al portapapeles.");

	}

	// --- Configuración de Eventos de la Vista ---
	/**
	 * Añade los listeners del controlador a los componentes de la vista.
	 */
	
	
	
	// --- Implementación de ActionListener ---
	@Override
	public void actionPerformed (ActionEvent e)
	{
		//------------------------------------------------------
		//LOG metodo mostrar nombres de botones y menus
		logActionInfo(e); // <-- LLAMAR AL MÉTODO DE logActionInfo PRIMERO
		//------------------------------------------------------
		
		// Redirige a los métodos específicos basados en el source
		Object source = e.getSource();
		String command = e.getActionCommand(); // Comando CORTO
		
		//LOG Evento recibido de:
        System.out.println("\n\n[ActionListener Central] Evento recibido de: "
                           + source.getClass().getSimpleName() + ", Comando: " + command);
        // --- IMPRIMIR EL COMANDO RECIBIDO ---
        System.out.println(">>> Acción Recibida - Comando: [" + command + "]\n\n");
        
        // Ya no necesitas los métodos handleButtonAction/handleMenuAction
        // si todo va por Actions.
        // La lógica ahora está DENTRO del actionPerformed de cada Action.


        
     // Mantenemos un switch aquí para los FALLBACKS (items sin Action)
        // o acciones muy específicas que no justificaban una clase Action.
        switch (command) {
            // --- Archivo (Ejemplo Fallback) ---
             case "Guardar_Configuracion_Actual": // Si no tuvo Action
                 guardarConfiguracionActual();
                 break;

             // --- Navegación (Manejado por Actions) ---

             // --- Zoom (Manejado por Actions) ---

             // --- Imagen (Ejemplo Fallback) ---
             case "Cambiar_Nombre_de_la_Imagen": // Si no tuvo Action
                 System.out.println("TODO: Fallback - Cambiar Nombre Imagen");
                 break;

             // --- Vista (Ejemplo Fallback) ---
             case "Mostrar_Dialogo_Lista_de_Imagenes": // Si no tuvo Action
                 mostrarDialogoListaImagenes();
                 break;
             case "Barra_de_Menu": // Checkbox de visibilidad (puede no tener Action)
             case "Barra_de_Botones":
             case "Mostrar/Ocultar_la_Lista_de_Archivos":
             case "Imagenes_en_Miniatura":
             case "Linea_de_Ubicacion_del_Archivo":
             case "Fondo_a_Cuadros":
             case "Mantener_Ventana_Siempre_Encima":
                 if (source instanceof JCheckBoxMenuItem) {
                     togglePanelVisibility(command, ((JCheckBoxMenuItem) source).isSelected());
                 }
                 break;


             // --- Configuración (Ejemplo Fallback) ---
             case "Cargar_Configuracion_Inicial": // Si no tuvo Action
                 System.out.println("TODO: Fallback - Cargar Config Inicial");
                 break;
             case "Botón_Rotar_Izquierda": // Checkbox de visibilidad (puede no tener Action)
                 // ... (Lógica para manejar visibilidad de botones si no usan Action) ...
                  if (source instanceof JCheckBoxMenuItem) {
                     boolean visible = ((JCheckBoxMenuItem) source).isSelected();
                     System.out.println("TODO: Fallback - Cambiar visibilidad botón Rotar_Izquierda a " + visible);
                     // view.getBotonesPorNombre().get("interfaz.boton.edicion.Rotar_Izquierda_48x48").setVisible(visible);
                     // guardarVisibilidadEnConfig("interfaz.boton.edicion.Rotar_Izquierda_48x48.visible", visible);
                 }
                 break;

            // --- Ayuda (Ejemplo Fallback) ---
             case "Version": // Si no tuvo Action
                 mostrarVersion();
                 break;

             default:
                 //System.out.println("WARN: Comando no manejado en ActionListener central: " + command);
                 //break;
            	 if (!(source instanceof JMenuItem)) {
                     System.out.println("WARN: Comando fallback no manejado: " + command + " de " + 
                    		 source.getClass().getSimpleName());
                 }
                 break;
        }
		
	}


	// --- Métodos Auxiliares de Lógica ---

	/**
	 * Navega a la imagen anterior o siguiente.
	 * 
	 * @param direccion -1 para anterior, 1 para siguiente.
	 */
	public void navegarImagen (int direccion)
	{

		if (model.getModeloLista().isEmpty())
			return; // No hay nada a donde navegar

		int currentIndex = view.getListaImagenes().getSelectedIndex();
		int total = model.getModeloLista().getSize();
		int nextIndex = currentIndex + direccion;

		// Lógica de bucle (wrap around) - Hacer configurable?
		boolean wrapAround = true; // Leer de config/modelo si es necesario

		if (wrapAround)
		{

			if (nextIndex < 0)
			{

				nextIndex = total - 1;

			} else if (nextIndex >= total)
			{

				nextIndex = 0;

			}

		} else
		{

			// Sin bucle, simplemente limitar al rango
			nextIndex = Math.max(0, Math.min(nextIndex, total - 1));

		}

		// Si el índice cambió, seleccionar en la vista (disparará listener)
		if (nextIndex != currentIndex)
		{

			view.getListaImagenes().setSelectedIndex(nextIndex);
			view.getListaImagenes().ensureIndexIsVisible(nextIndex); // Asegurar visibilidad

		}

	}

	/**
	 * Navega a un índice específico en la lista.
	 */
	public void navegarAIndice (int index)
	{

		if (model.getModeloLista().isEmpty() || index < 0 || index >= model.getModeloLista().getSize())
		{

			return; // Índice inválido

		}
		view.getListaImagenes().setSelectedIndex(index);
		view.getListaImagenes().ensureIndexIsVisible(index);
	}
	
	
	/**
	 * Devuelve el número actual de elementos en el modelo de la lista de imágenes.
	 * @return El tamaño de la lista, o 0 si el modelo no está inicializado.
	 */
	public int getTamanioListaImagenes() {
	    if (model != null && model.getModeloLista() != null) {
	        return model.getModeloLista().getSize();
	    }
	    return 0; // Devuelve 0 si algo no está listo
	}
	

	/**
	 * Muestra u oculta paneles principales de la interfaz.
	 */
	private void togglePanelVisibility (String panelName, boolean visible)
	{

		System.out.println("TODO: Implementar togglePanelVisibility para " + panelName + " -> " + visible);

		// Aquí iría la lógica para:
		// 1. Obtener el componente JPanel de la VISTA (ej.
		// view.getPanelListaArchivos())
		// 2. Llamar a setVisible(visible) en ese panel.
		// 3. Opcional: Ajustar divisores de JSplitPane si es necesario.
		// 4. Guardar el estado de visibilidad en ConfigurationManager.
		// 5. Podrías necesitar llamar a revalidate() en el contenedor principal de la
		// vista.
	}

	/**
	 * Muestra información de la versión.
	 */
	private void mostrarVersion ()
	{

		// Podrías leer la versión de un archivo de propiedades o hardcodearla
		String version = "1.0.0-MVC"; // Ejemplo
		JOptionPane.showMessageDialog(view.getFrame(),
				"Visor de Imágenes V2\nVersión: " + version + "\n(c) 2024 TuNombre", "Acerca de VisorV2",
				JOptionPane.INFORMATION_MESSAGE);

	}

	// --- Implementación de ClipboardOwner ---
	@Override
	public void lostOwnership (Clipboard clipboard, Transferable contents)
	{

		// Este método raramente necesita lógica específica
		System.out.println("Lost Clipboard Ownership");

	}

	
	// --- Dentro de la clase VisorController ---

	 /**
	  * Carga y muestra las miniaturas para el rango visible alrededor del índice seleccionado.
	  * Muestra placeholders inmediatamente, carga/escala en background usando ThumbnailService,
	  * y actualiza la UI.
	  * Maneja errores específicos como IOException y OutOfMemoryError a través del servicio.
	  *
	  * @param indiceSeleccionado El índice de la imagen actualmente seleccionada en la lista principal.
	  */
	 private void actualizarImagenesMiniaturaConRango(int indiceSeleccionado) {
	     // --- Verificar si los servicios necesarios están disponibles ---
	     // --- INICIO CÓDIGO MODIFICADO (añadir chequeo servicioMiniaturas) ---
	     if (executorService == null || executorService.isShutdown() || servicioMiniaturas == null) {
	         System.err.println("[Miniaturas] No se pueden cargar: ExecutorService o ThumbnailService no está activo/inicializado.");
	         return;
	     }
	     // --- FIN CÓDIGO MODIFICADO ---
	     if (model == null) {
	          System.err.println("[Miniaturas] No se pueden cargar: Modelo no inicializado.");
	          return;
	     }
	      if (view == null) {
	          System.err.println("[Miniaturas] No se pueden cargar: Vista no inicializada.");
	          return;
	      }
	     // --- FIN Verificar ---

	     // --- Cancelar tarea anterior ---
	     if (cargaMiniaturasFuture != null && !cargaMiniaturasFuture.isDone()) {
	         System.out.println("[Miniaturas] Cancelando tarea anterior...");
	         cargaMiniaturasFuture.cancel(true);
	     }
	     // --- FIN Cancelar ---

	     // --- Obtener total, validar índice ---
	     int totalImagenes = model.getModeloLista().getSize();
	     if (totalImagenes == 0) {
	         System.out.println("[Miniaturas] Lista de imágenes vacía, no hay nada que cargar.");
	          if(view != null) SwingUtilities.invokeLater(view::limpiarPanelMiniaturas); // Limpiar por si acaso
	         return;
	     }
	     // Corregir índice si está fuera de rango (sin cambios)
	     if (indiceSeleccionado < 0 || indiceSeleccionado >= totalImagenes) {
	          System.err.println("[Miniaturas] Índice seleccionado inválido: " + indiceSeleccionado + ". Usando 0.");
	          indiceSeleccionado = 0;
	          final int finalIndiceValidado = indiceSeleccionado; // Usar una nueva variable final
	          SwingUtilities.invokeLater(() -> {
	              if (view != null && view.getListaImagenes() != null) { // Chequeos extra
	                   // Solo seleccionar si el índice actual no es ya el corregido
	                   if (view.getListaImagenes().getSelectedIndex() != finalIndiceValidado) {
	                        view.getListaImagenes().setSelectedIndex(finalIndiceValidado);
	                   }
	              }
	          });
	     }
	     // --- FIN Validar índice ---

	     // --- Calcular rango y hacerlos FINAL --- (sin cambios)
	     final int inicio = Math.max(0, indiceSeleccionado - model.getMiniaturasAntes());
	     final int fin = Math.min(totalImagenes - 1, indiceSeleccionado + model.getMiniaturasDespues());
	     // -------------------------------------

	     System.out.println("[Miniaturas] Rango calculado: " + inicio + " a " + fin + " (Seleccionado: " + indiceSeleccionado + ")");

	     final int finalIndiceSeleccionado = indiceSeleccionado; // Usar el índice potencialmente corregido

	     // --- Placeholders en EDT --- (sin cambios en esta parte)
	     final List<String> keysInRange = new ArrayList<>();
	     for (int i = inicio; i <= fin; i++) {
	         if (i >= 0 && i < model.getModeloLista().getSize()) {
	              keysInRange.add(model.getModeloLista().getElementAt(i));
	         } else {
	              System.err.println("WARN [Miniaturas]: Índice " + i + " fuera de rango del modelo al crear lista de claves.");
	              keysInRange.add(null); // Añadir null como marcador de error de índice
	         }
	     }

	     SwingUtilities.invokeLater(() -> { // <-- Lambda para Placeholders
	          if (view == null) return;
	          view.limpiarPanelMiniaturas();
	          for (int i = 0; i < keysInRange.size(); i++) {
	              String currentKey = keysInRange.get(i);
	              boolean esSel = (inicio + i == finalIndiceSeleccionado);

	              // Calcular tamaño objetivo para el placeholder
	              int phAncho = esSel ? model.getMiniaturaSelAncho() : model.getMiniaturaNormAncho();
	              int phAlto = esSel ? model.getMiniaturaSelAlto() : model.getMiniaturaNormAlto();
	              String phTooltip;
	              if (currentKey != null) {
	                 phTooltip = "Cargando: " + currentKey;
	              } else {
	                  phTooltip = "Error: Índice inválido";
	                  phAncho = model.getMiniaturaNormAncho(); // Usar tamaño normal para error de índice
	                  phAlto = model.getMiniaturaNormAlto();
	                  esSel = false; // No puede ser seleccionada si el índice es inválido
	              }

	              // Llamar con los 4 argumentos
	              view.agregarMiniatura(crearPlaceholderMiniatura(phTooltip, phAncho, phAlto, esSel));
	          }
	          view.refrescarPanelMiniaturas();
	          System.out.println("[Miniaturas EDT] Placeholders mostrados para rango " + inicio + "-" + fin);
	     }); // <-- Fin Lambda Placeholders

	     // --- Tarea en Background ---
	     cargaMiniaturasFuture = executorService.submit(() -> { // <-- Lambda Background
	         Map<Integer, JLabel> miniaturasRealesMap = new HashMap<>();
	         long miniTaskStart = System.currentTimeMillis();

	         System.out.println("[Miniaturas BG] Tarea iniciada para rango " + inicio + "-" + fin);

	         try { // <-- TRY principal de la tarea
	             for (int i = inicio; i <= fin; i++) { // <-- Abre FOR BG
	                  if (Thread.currentThread().isInterrupted()) {
	                      System.out.println("[Miniaturas BG] Tarea interrumpida en bucle (índice " + i + ").");
	                      return;
	                  }

	                  // Validar índice de nuevo por seguridad
	                  if (i < 0 || i >= model.getModeloLista().getSize()) {
	                      System.err.println("WARN [Miniaturas BG]: Índice " + i + " fuera de rango del modelo.");
	                      miniaturasRealesMap.put(i, null); // Marcar como error para la UI
	                      continue; // Saltar esta iteración
	                  }
	                  String uniqueKey = model.getModeloLista().getElementAt(i);
	                  Path ruta = model.getRutaCompleta(uniqueKey);

	                  if (ruta == null) { // Ya no necesitamos verificar Files.exists aquí, el servicio lo hará
	                       System.err.println("[Miniaturas BG] Ruta es null para clave: " + uniqueKey);
	                       miniaturasRealesMap.put(i, null); // Marcar como error para la UI
	                       continue; // Saltar
	                  }

	                  JLabel etiquetaReal = null;
	                  ImageIcon miniaturaIcon = null; // Para guardar el resultado del servicio

	                  // --- INICIO CÓDIGO MODIFICADO ---
	                  // Ya no hay try-catch individual aquí, el servicio maneja sus errores internos.
	                  // Calculamos si es seleccionada y las dimensiones objetivo
	                  final boolean esSeleccionada = (i == finalIndiceSeleccionado);
	                  final int anchoObjetivo = esSeleccionada ? model.getMiniaturaSelAncho() : model.getMiniaturaNormAncho();
	                  final int altoObjetivo = esSeleccionada ? model.getMiniaturaSelAlto() : model.getMiniaturaNormAlto();

	                  // Llamamos al servicio para obtener la miniatura
	                  // Pasamos '!esSeleccionada' como 'esTamanoNormal' porque solo queremos cachear
	                  // las miniaturas de tamaño normal, no las seleccionadas (que son más grandes).
	                  miniaturaIcon = servicioMiniaturas.obtenerOCrearMiniatura(ruta, uniqueKey, anchoObjetivo, altoObjetivo, !esSeleccionada);

	                  // Si el servicio devolvió un icono (no hubo error grave)...
	                  if (miniaturaIcon != null) {
	                      // Crear JLabel real
	                      etiquetaReal = new JLabel(miniaturaIcon);
	                      // Calcular alto real por si se mantuvo proporción (importante si altoObjetivo era <= 0)
	                      int altoReal = miniaturaIcon.getIconHeight(); // El icono ya tiene el alto correcto
	                      // Asegurar que el alto no sea 0 si algo falló en el cálculo de proporción
	                      if (altoReal <= 0) altoReal = altoObjetivo > 0 ? altoObjetivo : anchoObjetivo;

	                      etiquetaReal.setPreferredSize(new Dimension(anchoObjetivo, altoReal));
	                      etiquetaReal.setOpaque(true);
	                      etiquetaReal.setToolTipText(ruta.getFileName().toString());
	                      etiquetaReal.setHorizontalAlignment(SwingConstants.CENTER);
	                      etiquetaReal.setVerticalAlignment(SwingConstants.CENTER);

	                      // Aplicar estilo (borde/fondo) basado en si es seleccionada
	                      if (esSeleccionada) {
	                          etiquetaReal.setBorder(BorderFactory.createLineBorder(Color.BLUE, 3));
	                          etiquetaReal.setBackground(new Color(200, 200, 255)); // Considerar usar colores del tema
	                      } else {
	                          etiquetaReal.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY)); // Considerar usar colores del tema
	                          etiquetaReal.setBackground(Color.WHITE); // Considerar usar colores del tema
	                      }

	                      // Añadir Listener (sin cambios)
	                      final int indiceListener = i; // Usar una variable final diferente para el listener
	                      etiquetaReal.addMouseListener(new java.awt.event.MouseAdapter() {
	                           @Override
	                           public void mouseClicked(java.awt.event.MouseEvent e) {
	                                if (estaCargandoLista) { return; }
	                                if (view != null && view.getListaImagenes().getSelectedIndex() != indiceListener) {
	                                     view.getListaImagenes().setSelectedIndex(indiceListener);
	                                     view.getListaImagenes().ensureIndexIsVisible(indiceListener);
	                                }
	                           }
	                      });
	                  } else {
	                       // Si servicioMiniaturas.obtenerOCrearMiniatura devolvió null,
	                       // etiquetaReal permanecerá null, indicando un error para la UI.
	                       System.out.println("[Miniaturas BG] ThumbnailService devolvió null para: " + uniqueKey);
	                  }
	                  // --- FIN CÓDIGO MODIFICADO ---


	                  // Guardar la etiqueta (o null si hubo error) en el mapa
	                  miniaturasRealesMap.put(i, etiquetaReal); // Guardar por índice absoluto


	             } // <-- Cierra FOR BG

	             // --- Actualizar la VISTA en el EDT --- (sin cambios en esta parte)
	             if (!Thread.currentThread().isInterrupted()) { // <-- Abre IF (!interrupted)
	                 // Crear copia final del mapa para la lambda
	                 final Map<Integer, JLabel> finalMiniaturasMap = new HashMap<>(miniaturasRealesMap);
	                 SwingUtilities.invokeLater(() -> { // <-- Abre invokeLater anidado
	                    if (view != null) { // <-- Abre IF (view != null)
	                        System.out.println("[Miniaturas EDT] Reemplazando placeholders con resultados.");
	                        view.limpiarPanelMiniaturas(); // Limpiar placeholders
	                        // Añadir en el orden correcto del rango
	                        for (int idx = inicio; idx <= fin; idx++) { // <-- Abre FOR EDT Update
	                             JLabel lbl = finalMiniaturasMap.get(idx); // Obtener del mapa final
	                             if (lbl != null) {
	                                 view.agregarMiniatura(lbl); // Añadir real
	                             } else {
	                                 // Si falló (era null en el mapa), añadir placeholder de error
	                                 String keyFallida = (idx >= 0 && idx < model.getModeloLista().getSize())
	                                                     ? model.getModeloLista().getElementAt(idx) : "Índice inválido";
	                                 boolean esSelFallida = (idx == finalIndiceSeleccionado);
	                                 int phAnchoErr = esSelFallida ? model.getMiniaturaSelAncho() : model.getMiniaturaNormAncho();
	                                 int phAltoErr = esSelFallida ? model.getMiniaturaSelAlto() : model.getMiniaturaNormAlto();
	                                 view.agregarMiniatura(crearPlaceholderMiniatura(
	                                      "Error: " + keyFallida, phAnchoErr, phAltoErr, esSelFallida
	                                 ));
	                             }
	                        } // <-- Cierra FOR EDT Update
	                        view.refrescarPanelMiniaturas();
	                        System.out.println("[Miniaturas EDT] Panel actualizado con miniaturas reales/errores.");
	                    } else {
	                         System.out.println("[Miniaturas EDT] Vista es nula, no se puede actualizar.");
	                    } // <-- Cierra IF (view != null)
	                 }); // <-- Cierra invokeLater anidado
	             } else {
	                  System.out.println("[Miniaturas BG] Tarea interrumpida ANTES de actualizar UI.");
	             } // <-- Cierra IF (!interrupted)

	         } catch (Exception e) { // <-- Catch TAREA GRAVE (inesperado fuera del servicio)
	             System.err.println("[Miniaturas] Error GRAVE en tarea background: " + e.getMessage());
	             e.printStackTrace();
	         } finally { // <-- Abre FINALLY
	             long miniTaskEnd = System.currentTimeMillis();
	             System.out.println("[Miniaturas] Tarea Miniaturas terminada en " + (miniTaskEnd - miniTaskStart) + " ms.");
	         } // <-- Cierra FINALLY

	     }); // <-- Cierra submit lambda
	 } // <-- Cierra MÉTODO actualizarImagenesMiniaturaConRango

	
    private JLabel crearPlaceholderMiniatura(String toolTipText, int ancho, int alto, boolean esSeleccionada) {//weno
        JLabel placeholder = new JLabel("...", SwingConstants.CENTER);
        placeholder.setOpaque(true);
        placeholder.setForeground(Color.GRAY);
        int phAncho = Math.max(10, ancho);
        int phAlto = Math.max(10, (alto <= 0 ? ancho : alto) );
        placeholder.setPreferredSize(new Dimension(phAncho, phAlto));
        if (esSeleccionada) {
            placeholder.setBorder(BorderFactory.createDashedBorder(Color.BLUE, 1, 5));
            placeholder.setBackground(new Color(230, 230, 255));
        } else {
            placeholder.setBorder(BorderFactory.createDashedBorder(Color.LIGHT_GRAY, 1, 5));
            placeholder.setBackground(Color.WHITE);
        }
        placeholder.setToolTipText(toolTipText);
        return placeholder;
   }

	

	// --- Hook de Cierre ---
    // --- INICIO CÓDIGO CORREGIDO en VisorController.configurarShutdownHook ---
    private void configurarShutdownHook () {
        Runtime.getRuntime().addShutdownHook(new Thread( () -> {
            System.out.println("--- Hook de Cierre Iniciado ---");

            // --- GUARDAR ESTADO DE LA VENTANA ---
            if (view != null && configuration != null) {
                 System.out.println("  -> Guardando estado de la ventana...");
                 try {
                     boolean isMaximized = (view.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
                     // --- AÑADIR ConfigurationManager. ---
                     configuration.setString(ConfigurationManager.KEY_WINDOW_MAXIMIZED, String.valueOf(isMaximized));

                     if (!isMaximized) {
                         java.awt.Rectangle bounds = view.getBounds();
                         configuration.setString(ConfigurationManager.KEY_WINDOW_X, String.valueOf(bounds.x));
                         configuration.setString(ConfigurationManager.KEY_WINDOW_Y, String.valueOf(bounds.y));
                         configuration.setString(ConfigurationManager.KEY_WINDOW_WIDTH, String.valueOf(bounds.width));
                         configuration.setString(ConfigurationManager.KEY_WINDOW_HEIGHT, String.valueOf(bounds.height));
                          System.out.println("    -> Bounds guardados: " + bounds);
                     } else {
                          System.out.println("    -> Ventana maximizada, no se guardan bounds específicos.");
                     }
                     // ---------------------------------
                 } catch (Exception e) {
                      System.err.println("  -> ERROR al guardar estado de ventana: " + e.getMessage());
                 }
            } else {
                 System.out.println("  -> No se pudo guardar estado de ventana (Vista o Config null).");
            }

            // --- Guardar configuración general ---
            System.out.println("  -> Guardando configuración general...");
            if (configuration != null) {
                guardarConfiguracionActual();
            } else {
                 System.err.println("  -> ConfigurationManager null, no se puede guardar config.");
            }

            // --- Apagar ExecutorService ---
            System.out.println("  -> Apagando ExecutorService...");
            if (executorService != null && !executorService.isShutdown()) {
               // ... (lógica de apagado) ...
               executorService.shutdown();
               try {
                   if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                       executorService.shutdownNow();
                       if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                            System.err.println("  -> ExecutorService no terminó.");
                       }
                   }
               } catch (InterruptedException ie) {
                   executorService.shutdownNow();
                   Thread.currentThread().interrupt();
               }
               System.out.println("     -> ExecutorService apagado.");
            } else {
                System.out.println("     -> ExecutorService ya apagado o nulo.");
            }
            System.out.println("--- Hook de Cierre Terminado ---");

        }, "AppShutdownThread"));
    }
    // --- FIN CÓDIGO CORREGIDO en VisorController.configurarShutdownHook ---
    
    
	/**
	 * Devuelve si el zoom manual está habilitado actualmente en el modelo.
	 * Necesario para que ToggleZoomManualAction sepa cómo alternar desde un botón.
	 * @return true si el zoom manual está habilitado.
	 */
	public boolean isZoomManualCurrentlyEnabled() {
	    return model != null && model.isZoomHabilitado();
	}

	// --- NUEVO: Método centralizado para loguear información de acción ---
    public void logActionInfo(ActionEvent e) {
        if (e == null) return;

        Object source = e.getSource();
        String command = e.getActionCommand(); // Comando CORTO (o largo si viene de JMenu)
        String sourceClass = (source != null) ? source.getClass().getSimpleName() : "null";

        String longConfigKey = findLongKeyForComponent(source); // Buscar clave larga
        String iconName = findIconNameForComponent(source);     // Intentar obtener nombre icono

        System.out.println("--- Acción Detectada ---");
        System.out.println("  Fuente     : " + sourceClass);
        System.out.println("  Comando    : " + command); // Puede ser corto (JButton/JMenuItem) o largo (JMenu)
        System.out.println("  Clave Larga: " + (longConfigKey != null ? longConfigKey : "(No encontrada)"));
        if (iconName != null) {
             System.out.println("  Icono      : " + iconName);
        }
        System.out.println("-------------------------");
    }

    // --- Helper: Buscar Clave Larga (igual que antes) ---
    private String findLongKeyForComponent(Object source) {
        if (view == null || !(source instanceof Component)) return null;
        Component comp = (Component) source;
        // Buscar en botones
        if (view.getBotonesPorNombre() != null) {
            for (Map.Entry<String, JButton> entry : view.getBotonesPorNombre().entrySet()) {
                if (entry.getValue() == comp) return entry.getKey();
            }
        }
        // Buscar en menús
        if (view.getMenuItemsPorNombre() != null) {
            for (Map.Entry<String, JMenuItem> entry : view.getMenuItemsPorNombre().entrySet()) {
                 if (entry.getValue() == comp) return entry.getKey();
            }
        }
        return null;
    }

     // --- Helper: Intentar obtener nombre de icono (igual que antes - limitado) ---
    private String findIconNameForComponent(Object source) {
         if (source instanceof JButton) {
             JButton button = (JButton) source;
             Icon icon = button.getIcon();
             if (icon instanceof ImageIcon) {
                 // Intentar inferir MUY BÁSICAMENTE desde la clave larga
                 String longKey = findLongKeyForComponent(source);
                  if (longKey != null && longKey.startsWith("interfaz.boton.")) {
                     String[] parts = longKey.split("\\.");
                     if (parts.length >= 4) {
                          // Devuelve la última parte, que es el nombre base
                          return parts[parts.length - 1] + ".png"; // Supone extensión .png
                     }
                 }
                 return "(Icono presente, nombre archivo no inferido)";
             }
         }
         return null;
    }

    
 // --- Getter para ConfigurationManager (necesario para el constructor de ToggleThemeAction) ---
    /**
     * Devuelve la instancia del ConfigurationManager.
     * @return la instancia de ConfigurationManager.
     */
    public ConfigurationManager getConfigurationManager() {
        return configuration;
    }

    
 // Método helper para parsear Color desde String "R, G, B" ---
    private Color parseColor(String rgbString) 
    {
        if (rgbString == null || rgbString.trim().isEmpty()) 
        {
            return Color.LIGHT_GRAY; // Default si es inválido
        }
        
        String[] components = rgbString.split(",");
        if (components.length == 3) 
        {
            try 
            {
                int r = Integer.parseInt(components[0].trim());
                int g = Integer.parseInt(components[1].trim());
                int b = Integer.parseInt(components[2].trim());
                // Validar rango 0-255
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                return new Color(r, g, b);
            } catch (NumberFormatException e) {
                System.err.println("WARN: Formato de color inválido en config: '" + rgbString + "'. Usando Gris Claro.");
                return Color.LIGHT_GRAY;
            }
        } else {
             System.err.println("WARN: Formato de color debe ser R,G,B: '" + rgbString + "'. Usando Gris Claro.");
             return Color.LIGHT_GRAY;
        }
    }

    
    /**
	 * Alterna el modo de carga (con/sin subcarpetas).
	 */
	private void alternarModoCarga (boolean soloCarpetaActual)
	{

		if (configuration == null)
			return;
		// 1. Actualizar Modelo
		model.setMostrarSoloCarpetaActual(soloCarpetaActual);

		// 2. Actualizar Vista (Radios del menú)
		JMenuItem rbSolo = view.getMenuItemsPorNombre().get("Mostrar_Solo_Carpeta_Actual");
		if (rbSolo instanceof JRadioButtonMenuItem)
			((JRadioButtonMenuItem) rbSolo).setSelected(soloCarpetaActual);
		JMenuItem rbSub = view.getMenuItemsPorNombre().get("Mostrar_Imagenes_de_Subcarpetas");
		if (rbSub instanceof JRadioButtonMenuItem)
			((JRadioButtonMenuItem) rbSub).setSelected(!soloCarpetaActual);

		// 3. Guardar Configuración
		try
		{

			Map<String, String> currentConfig = configuration.getConfigMap();
			currentConfig.put("comportamiento.carpeta.cargarSubcarpetas",
					String.valueOf(!model.isMostrarSoloCarpetaActual()));
			configuration.guardarConfiguracion(currentConfig);

		} catch (IOException ex)
		{

			System.err.println("Error al guardar modo carga: " + ex.getMessage());
			JOptionPane.showMessageDialog(view.getFrame(), "Error al guardar config:\n" + ex.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);

		}

		// 4. Recargar Lista
		cargarListaImagenes();

		// 5. Notificar Usuario (Vista)
		SwingUtilities.invokeLater( () -> {

			String mensaje = soloCarpetaActual ? "Mostrando solo imágenes de la carpeta actual."
					: "Mostrando imágenes de la carpeta actual y subcarpetas.";
			JOptionPane.showMessageDialog(view.getFrame(), mensaje, "Cambio de Configuración",
					JOptionPane.INFORMATION_MESSAGE);
			System.out.println(mensaje);

		});

	}
	

	/**
	 * Recarga la lista de imágenes actual.
	 */
	private void refrescarListaImagenes ()
	{

		cargarListaImagenes();

	}

	
	private void configurarEventosVista ()
	{

		// Listener para selección en JList
		view.addListaImagenesSelectionListener(e -> {

			if (!e.getValueIsAdjusting() && !estaCargandoLista)
			{ // Evitar durante ajuste y carga

				int indiceSeleccionado = view.getListaImagenes().getSelectedIndex();

				if (indiceSeleccionado >= 0)
				{

					String selectedKey = model.getModeloLista().getElementAt(indiceSeleccionado);

					// Evitar recargar si ya es la imagen seleccionada en el modelo
					if (!selectedKey.equals(model.getSelectedImageKey()))
					{

						mostrarImagenSeleccionada();

					}
					// Siempre actualizar miniaturas al seleccionar
					actualizarImagenesMiniaturaConRango(indiceSeleccionado);

				}

			}

		});

		// Listeners para Zoom/Pan en JLabel
		view.addEtiquetaImagenMouseWheelListener(e -> {

			if (model.isZoomHabilitado())
			{ // Usa estado del MODELO

				int notches = e.getWheelRotation();
				double zoomIncrement = 0.1; // Hacer configurable?
				double newZoomFactor = model.getZoomFactor() + (notches < 0 ? zoomIncrement : -zoomIncrement);
				// Actualizar MODELO
				model.setZoomFactor(newZoomFactor);
				// Actualizar VISTA
				Image currentReescaled = reescalarImagenParaAjustar();
				view.setImagenMostrada(currentReescaled, model.getZoomFactor(), model.getImageOffsetX(),
						model.getImageOffsetY());

			}

		});
		view.addEtiquetaImagenMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mousePressed (java.awt.event.MouseEvent e)
			{

				if (model.isZoomHabilitado())
				{

					lastMouseX = e.getX(); // Guarda estado local del Controller
					lastMouseY = e.getY();

				}

			}
		});
		view.addEtiquetaImagenMouseMotionListener(new java.awt.event.MouseMotionAdapter()
		{
			@Override
			public void mouseDragged (java.awt.event.MouseEvent e)
			{

				if (model.isZoomHabilitado())
				{

					int deltaX = e.getX() - lastMouseX;
					int deltaY = e.getY() - lastMouseY;
					// Actualizar MODELO
					model.addImageOffsetX(deltaX);
					model.addImageOffsetY(deltaY);
					// Actualizar estado local del Controller
					lastMouseX = e.getX();
					lastMouseY = e.getY();
					// Actualizar VISTA
					Image currentReescaled = reescalarImagenParaAjustar();
					view.setImagenMostrada(currentReescaled, model.getZoomFactor(), model.getImageOffsetX(),
							model.getImageOffsetY());

				}

			}
		});

		// Añadir ActionListener a TODOS los botones y menús relevantes
		// 'this' (el Controller) implementa ActionListener y redirigirá
		if (view.getBotonesPorNombre() != null)
		{

			//view.getBotonesPorNombre().keySet().forEach(nombreBoton -> view.addButtonActionListener(nombreBoton, this));
			view.getBotonesPorNombre().keySet().forEach(configKey -> { // configKey es la clave LARGA
	            view.addButtonActionListener(configKey, this); // Pasa la clave LARGA para añadir listener
			});
			
		}else {
	         System.err.println("WARN: Mapa de botones en la vista es null al configurar eventos.");
	    }

		if (view.getMenuItemsPorNombre() != null)
		{

			//view.getMenuItemsPorNombre().keySet().forEach(actionCommand -> view.addMenuItemActionListener(actionCommand, this));

		}else {
	         System.err.println("WARN: Mapa de menús en la vista es null al configurar eventos.");
	    }
		
		 if (view.getMenuItemsPorNombre() != null) {
		        // ***** RESTAURAR ESTE BUCLE *****
		        view.getMenuItemsPorNombre().keySet().forEach(configKey -> { // Itera claves LARGAS
		            // Llama al método PÚBLICO de la vista, pasándole la clave larga
		            view.addMenuItemActionListener(configKey, this);
		        });
		        // ********************************
		    } else {
		        System.err.println("WARN: Mapa de menús en la vista es null al configurar eventos.");
		    }
		
		System.out.println("Listeners del Controller añadidos a la Vista.");

	}
	
	
	//*********************************** METODOS DE EDICION DE IMAGENES ***************************************
	
    public void aplicarVolteoHorizontal() {
        if (model == null || view == null) return;
        BufferedImage imagenOriginal = model.getCurrentImage();
        if (imagenOriginal == null) { /*...*/ return; }

        System.out.println("[Volteo H] Solicitando volteo a ImageEditor...");
        // --- TEXTO MODIFICADO ---
        // Llamar al método estático de ImageEditor
        BufferedImage imagenVolteada = ImageEdition.flipHorizontal(imagenOriginal);
        // --- FIN MODIFICACION ---

        if (imagenVolteada != null) {
            // Actualizar Modelo
            model.setCurrentImage(imagenVolteada);
            // Resetear zoom?
            // model.resetZoomState();
            // Actualizar Vista
            Image imagenReescalada = reescalarImagenParaAjustar();
            if (view != null) {
                view.setImagenMostrada(imagenReescalada, model.getZoomFactor(), model.getImageOffsetX(), model.getImageOffsetY());
                view.aplicarAnimacionBoton("Espejo_Horizontal_48x48");
            }
            System.out.println("[Volteo H] Volteo aplicado y vista actualizada.");
        } else {
            System.err.println("ERROR [Volteo H] ImageEditor devolvió null.");
            JOptionPane.showMessageDialog(view.getFrame(), "Error al intentar voltear la imagen.", "Error de Volteo", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- NUEVOS MÉTODOS PARA OTRAS EDICIONES ---
    /**
     * Aplica un volteo vertical llamando a ImageEditor y actualiza modelo/vista.
     */
    public void aplicarVolteoVertical() {
        if (model == null || view == null) return;
        BufferedImage imagenOriginal = model.getCurrentImage();
        if (imagenOriginal == null) { /*...*/ return; }
        System.out.println("[Volteo V] Solicitando volteo a ImageEditor...");
        BufferedImage imagenVolteada = ImageEdition.flipVertical(imagenOriginal); // Llamar al método correspondiente
        if (imagenVolteada != null) {
            model.setCurrentImage(imagenVolteada);
            Image imagenReescalada = reescalarImagenParaAjustar();
            if (view != null) {
                view.setImagenMostrada(imagenReescalada, model.getZoomFactor(), model.getImageOffsetX(), model.getImageOffsetY());
                view.aplicarAnimacionBoton("Espejo_Vertical_48x48"); // Usar nombre correcto
            }
            System.out.println("[Volteo V] Volteo aplicado y vista actualizada.");
        } else { /*...*/ }
    }

     /**
     * Aplica rotación izquierda llamando a ImageEditor y actualiza modelo/vista.
     */
    public void aplicarRotarIzquierda() {
        if (model == null || view == null) return;
        BufferedImage imagenOriginal = model.getCurrentImage();
        if (imagenOriginal == null) { /*...*/ return; }
        System.out.println("[Rotar Izq] Solicitando rotación a ImageEditor...");
        BufferedImage imagenRotada = ImageEdition.rotateLeft(imagenOriginal); // Llamar al método correspondiente
        if (imagenRotada != null) {
            model.setCurrentImage(imagenRotada);
            // ¡Importante! Resetear zoom/pan después de rotar porque las dimensiones cambian
            model.resetZoomState();
            Image imagenReescalada = reescalarImagenParaAjustar();
            if (view != null) {
                view.setImagenMostrada(imagenReescalada, model.getZoomFactor(), model.getImageOffsetX(), model.getImageOffsetY());
                view.aplicarAnimacionBoton("Rotar_Izquierda_48x48"); // Usar nombre correcto
            }
            System.out.println("[Rotar Izq] Rotación aplicada y vista actualizada.");
        } else { /*...*/ }
    }

     /**
     * Aplica rotación derecha llamando a ImageEditor y actualiza modelo/vista.
     */
    public void aplicarRotarDerecha() {
         if (model == null || view == null) return;
        BufferedImage imagenOriginal = model.getCurrentImage();
        if (imagenOriginal == null) { /*...*/ return; }
        System.out.println("[Rotar Der] Solicitando rotación a ImageEditor...");
        BufferedImage imagenRotada = ImageEdition.rotateRight(imagenOriginal); // Llamar al método correspondiente
        if (imagenRotada != null) {
            model.setCurrentImage(imagenRotada);
            model.resetZoomState(); // Resetear zoom
            Image imagenReescalada = reescalarImagenParaAjustar();
            if (view != null) {
                view.setImagenMostrada(imagenReescalada, model.getZoomFactor(), model.getImageOffsetX(), model.getImageOffsetY());
                view.aplicarAnimacionBoton("Rotar_Derecha_48x48"); // Usar nombre correcto
            }
            System.out.println("[Rotar Der] Rotación aplicada y vista actualizada.");
        } else { /*...*/ }
    }

    //*********************************** METODOS DE BOTONES TOGGLE ***************************************

    
    // --- INICIO MÉTODO COMPLETO Y CORREGIDO: setMostrarSubcarpetasAndUpdateConfig ---
    /**
     * Actualiza el estado lógico y visual para mostrar/ocultar subcarpetas.
     * Guarda el nuevo estado en la configuración y recarga la lista de imágenes
     * intentando mantener la imagen seleccionada actualmente.
     *
     * @param mostrarSubcarpetasDeseado true si se deben mostrar subcarpetas, false si solo la carpeta actual.
     */
    public void setMostrarSubcarpetasAndUpdateConfig(boolean mostrarSubcarpetasDeseado) {
        System.out.println("\n[Controller setMostrarSubcarpetas] INICIO. Deseado: " + mostrarSubcarpetasDeseado);

        if (model == null || configuration == null || toggleSubfoldersAction == null || view == null) {
            System.err.println("  -> ERROR: Componentes necesarios nulos. Saliendo.");
            return;
        }

        // Comprobar si el cambio es necesario
        boolean estadoLogicoActual = Boolean.TRUE.equals(toggleSubfoldersAction.getValue(Action.SELECTED_KEY));
        System.out.println("  -> Estado Lógico Actual (Action.SELECTED_KEY): " + estadoLogicoActual);
        if (mostrarSubcarpetasDeseado == estadoLogicoActual) {
            System.out.println("  -> Estado deseado ya es el actual. No se hace nada (solo sincronizar radios).");
            restaurarSeleccionRadiosSubcarpetas(estadoLogicoActual); // Asegurar UI visual
             System.out.println("[Controller setMostrarSubcarpetas] FIN (Sin cambios).");
            return;
        }

        System.out.println("  -> Aplicando cambio a estado: " + mostrarSubcarpetasDeseado);

        // --- ¡Guardar la clave ANTES de cambiar nada! ---
        final String claveAntesDelCambio = model.getSelectedImageKey();
        System.out.println("    -> Clave a intentar mantener: " + claveAntesDelCambio);
        // ---------------------------------------------

        // 1. Actualizar Action (para el botón y estado lógico central)
        System.out.println("    1. Actualizando Action.SELECTED_KEY...");
        toggleSubfoldersAction.putValue(Action.SELECTED_KEY, mostrarSubcarpetasDeseado);
         System.out.println("       -> Action.SELECTED_KEY AHORA ES: " + Boolean.TRUE.equals(toggleSubfoldersAction.getValue(Action.SELECTED_KEY)));

        // 2. Actualizar Modelo (estado lógico de visualización)
         System.out.println("    2. Actualizando Modelo...");
        model.setMostrarSoloCarpetaActual(!mostrarSubcarpetasDeseado);
         System.out.println("       -> Modelo.isMostrarSoloCarpetaActual() AHORA ES: " + model.isMostrarSoloCarpetaActual());

        // 3. Actualizar Configuración en Memoria
         System.out.println("    3. Actualizando Configuración en Memoria...");
        configuration.setString("comportamiento.carpeta.cargarSubcarpetas", String.valueOf(mostrarSubcarpetasDeseado));
         System.out.println("       -> Config 'comportamiento.carpeta.cargarSubcarpetas' AHORA ES: " + configuration.getString("comportamiento.carpeta.cargarSubcarpetas"));

        // 4. Sincronizar UI (Botón y Radios)
        System.out.println("    4. Sincronizando UI (Botón y Radios)...");
        actualizarAspectoBotonToggle(toggleSubfoldersAction, mostrarSubcarpetasDeseado); // Actualiza color/apariencia botón
        restaurarSeleccionRadiosSubcarpetas(mostrarSubcarpetasDeseado); // Actualiza estado setSelected radios

        // 5. Recargar Lista de Imágenes (Pasando la clave guardada)
        System.out.println("    5. Programando recarga de lista en EDT (manteniendo clave)...");
        SwingUtilities.invokeLater(() -> {
            System.out.println("      -> [EDT] Llamando a cargarListaImagenes(\"" + claveAntesDelCambio + "\") para recargar...");
            cargarListaImagenes(claveAntesDelCambio); // <--- PASAR LA CLAVE GUARDADA
        });

        System.out.println("[Controller setMostrarSubcarpetas] FIN (Cambio aplicado y recarga programada).");
    }
    // --- FIN MÉTODO COMPLETO Y CORREGIDO: setMostrarSubcarpetasAndUpdateConfig ---
    
    
    // --- sincronizar los Radio Buttons ---
    /**
     * Asegura que los JRadioButtonMenuItem de subcarpetas reflejen el estado actual.
     * @param mostrarSubcarpetas El estado actual (true si se muestran subcarpetas).
     */
    public void sincronizarRadiosSubcarpetas(boolean mostrarSubcarpetas) {
         if (view == null || view.getMenuItemsPorNombre() == null) return;
         Map<String, JMenuItem> menuItems = view.getMenuItemsPorNombre();

         JMenuItem radioMostrarSub = menuItems.get("interfaz.menu.configuracion.carga_de_imagenes.Mostrar_Imagenes_de_Subcarpetas");
         JMenuItem radioMostrarSolo = menuItems.get("interfaz.menu.configuracion.carga_de_imagenes.Mostrar_Solo_Carpeta_Actual");

         if (radioMostrarSub instanceof JRadioButtonMenuItem) {
              ((JRadioButtonMenuItem)radioMostrarSub).setSelected(mostrarSubcarpetas);
         }
         if (radioMostrarSolo instanceof JRadioButtonMenuItem) {
              ((JRadioButtonMenuItem)radioMostrarSolo).setSelected(!mostrarSubcarpetas);
         }
    }
    
    
    /**
     * Asegura que los JRadioButtonMenuItem de subcarpetas reflejen el estado lógico actual.
     * Es SEGURO llamar a setSelected aquí porque los radios ya NO usan setAction.
     * @param mostrarSubcarpetas El estado lógico actual (true si se deben mostrar subcarpetas).
     */
    public void restaurarSeleccionRadiosSubcarpetas(boolean mostrarSubcarpetas) { // Renombrado para claridad
         if (view == null || view.getMenuItemsPorNombre() == null) return;
         Map<String, JMenuItem> menuItems = view.getMenuItemsPorNombre();
         System.out.println("  -> [Controller] Sincronizando estado visual de Radios a: " + (mostrarSubcarpetas ? "Subcarpetas" : "Solo Carpeta"));

         JMenuItem radioMostrarSub = menuItems.get("interfaz.menu.configuracion.carga_de_imagenes.Mostrar_Imagenes_de_Subcarpetas");
         JMenuItem radioMostrarSolo = menuItems.get("interfaz.menu.configuracion.carga_de_imagenes.Mostrar_Solo_Carpeta_Actual");

         // Aplicar setSelected SIN miedo al bucle
         if (radioMostrarSub instanceof JRadioButtonMenuItem) {
             // Solo llamar si el estado es diferente para evitar eventos redundantes del ButtonGroup
             if (((JRadioButtonMenuItem)radioMostrarSub).isSelected() != mostrarSubcarpetas) {
                  ((JRadioButtonMenuItem)radioMostrarSub).setSelected(mostrarSubcarpetas);
             }
         }
         if (radioMostrarSolo instanceof JRadioButtonMenuItem) {
              if (((JRadioButtonMenuItem)radioMostrarSolo).isSelected() == mostrarSubcarpetas) { // Es la inversa
                  ((JRadioButtonMenuItem)radioMostrarSolo).setSelected(!mostrarSubcarpetas);
              }
         }
         // IMPORTANTE: Habilitar los radios de nuevo si estaban deshabilitados
          if (radioMostrarSub != null) radioMostrarSub.setEnabled(true);
          if (radioMostrarSolo != null) radioMostrarSolo.setEnabled(true);
    }


    /**
     * Actualiza el color de fondo de un botón asociado a una Action de tipo toggle
     * para indicar visualmente si está "pulsado" (activo/seleccionado) o no.
     * Utiliza los colores definidos en el tema actual.
     *
     * @param action La instancia de la Action toggle (ej. toggleSubfoldersAction).
     *               Se usa para obtener el estado SELECTED_KEY y potencialmente identificar el botón.
     * @param isSelected El estado lógico actual (true si debe parecer activo, false si no).
     *                   Se pasa explícitamente para asegurar la sincronización.
     */
     public void actualizarAspectoBotonToggle(Action action, boolean isSelected) {
        // --- Validaciones Iniciales ---
        if (action == null) {
             System.err.println("ERROR [actualizarAspectoBotonToggle]: La Action proporcionada es null.");
             return;
        }
        if (view == null || view.getBotonesPorNombre() == null) {
             System.err.println("ERROR [actualizarAspectoBotonToggle]: Vista o mapa de botones no disponible.");
             return;
        }
        if (themeManager == null || themeManager.getTemaActual() == null) {
             System.err.println("ERROR [actualizarAspectoBotonToggle]: ThemeManager o Tema actual no disponible.");
             return;
        }

        // --- Determinar la Clave Larga del Botón en el Mapa ---
        // Necesitamos encontrar qué botón corresponde a esta Action.
        // La forma más fiable es buscar en el mapa de botones qué botón
        // tiene asignada esta Action específica.
        String claveBoton = null;
        for (Map.Entry<String, JButton> entry : view.getBotonesPorNombre().entrySet()) {
            if (action.equals(entry.getValue().getAction())) { // Compara la instancia de la Action
                claveBoton = entry.getKey();
                break; // Encontrado
            }
        }

        // --- Alternativa (Menos fiable): Usar ActionCommand ---
        // Si la búsqueda anterior falla (por alguna razón la Action no está asignada
        // directamente al botón, aunque debería), podemos intentar usar el ActionCommand.
        if (claveBoton == null) {
             String actionCommand = (String) action.getValue(Action.ACTION_COMMAND_KEY);
             // Si el ActionCommand no está, intentar con el NAME
              if (actionCommand == null) {
                  Object name = action.getValue(Action.NAME);
                  if (name instanceof String) actionCommand = (String)name;
              }

             if (actionCommand != null) {
                 System.out.println("WARN [actualizarAspectoBotonToggle]: No se encontró botón por instancia de Action, intentando por comando: " + actionCommand);
                 // Construir clave larga basada en el comando (requiere saber la categoría)
                 // ¡AQUÍ ESTÁ EL PUNTO CLAVE DE LA CORRECCIÓN ANTERIOR!
                 // Debes usar la categoría correcta ("toggle" en tu caso)
                 if (action == toggleSubfoldersAction || "Subcarpetas_48x48".equals(actionCommand)) {
                     claveBoton = "interfaz.boton.toggle.Subcarpetas_48x48";
                 } else if (action == /* toggleMantenerProporcionesAction */ null || "Mantener_Proporciones_48x48".equals(actionCommand)) {
                     // Descomenta y ajusta cuando tengas esta action
                     // claveBoton = "interfaz.boton.toggle.Mantener_Proporciones_48x48";
                 } else if (action == /* toggleMostrarFavoritosAction */ null || "Mostrar_Favoritos_48x48".equals(actionCommand)) {
                      // Descomenta y ajusta cuando tengas esta action
                     // claveBoton = "interfaz.boton.toggle.Mostrar_Favoritos_48x48";
                 }
                 // Añadir más 'else if' para otros botones toggle
                 else {
                      System.err.println("WARN [actualizarAspectoBotonToggle]: No se pudo determinar la clave del botón para el comando: " + actionCommand);
                 }
             } else {
                 System.err.println("ERROR [actualizarAspectoBotonToggle]: No se pudo obtener ActionCommand ni Name para la Action.");
                 return; // No podemos encontrar el botón
             }
        }


        // --- Aplicar el Cambio de Aspecto ---
        JButton button = null;
        if (claveBoton != null) {
            button = view.getBotonesPorNombre().get(claveBoton);
        }

        if (button != null) {
            Tema temaActual = themeManager.getTemaActual();
            // Obtener los colores específicos del tema actual
            Color colorActivo = temaActual.colorBotonFondoActivado();
            Color colorNormal = temaActual.colorBotonFondo();

            // Aplicar el color correspondiente al estado 'isSelected'
            button.setBackground(isSelected ? colorActivo : colorNormal);
            // Opcional: Cambiar también el borde o el texto si quieres más feedback visual
            // if (isSelected) {
            //     button.setBorder(BorderFactory.createLoweredBevelBorder());
            // } else {
            //     button.setBorderPainted(false); // O el borde normal
            // }
            System.out.println("  -> Aspecto botón '" + claveBoton + "' actualizado a: " + (isSelected ? "Activo" : "Normal"));
        } else {
             System.err.println("WARN [actualizarAspectoBotonToggle]: No se encontró el botón para la clave final: " + claveBoton);
        }
    }

     // --- Necesitas un getter para el Model si LocateFileAction lo usa ---
     public VisorModel getModel() {
         return model;
     }
     // --- Y para la View si muestra JOptionPanes ---
     public VisorView getView() {
         return view;
     }
     // -----------------------------------------------------------------


     /**
      * Actualiza el estado lógico y visual para mantener/no mantener proporciones.
      * Guarda el nuevo estado en la configuración, actualiza la Action, el modelo
      * y el aspecto del botón. Finalmente, repinta la imagen principal.
      *
      * @param mantener True si se deben mantener las proporciones, false si no.
      */
     public void setMantenerProporcionesAndUpdateConfig(boolean mantener) {
         System.out.println("\n[Controller setMantenerProporciones] INICIO. Deseado: " + mantener);

         if (model == null || configuration == null || toggleProporcionesAction == null || view == null) {
             System.err.println("  -> ERROR: Componentes necesarios nulos. Saliendo.");
             return;
         }

         // Comprobar si el cambio es necesario (comparando con el estado actual de la Action)
         boolean estadoActualAction = Boolean.TRUE.equals(toggleProporcionesAction.getValue(Action.SELECTED_KEY));
         System.out.println("  -> Estado Lógico Actual (Action.SELECTED_KEY): " + estadoActualAction);
         if (mantener == estadoActualAction) {
             System.out.println("  -> Estado deseado ya es el actual. No se hace nada.");
              // Podríamos llamar a actualizarAspectoBotonToggle por si acaso, pero no es estrictamente necesario
              // actualizarAspectoBotonToggle(toggleProporcionesAction, estadoActualAction);
              System.out.println("[Controller setMantenerProporciones] FIN (Sin cambios).");
             return;
         }

         System.out.println("  -> Aplicando cambio a estado: " + mantener);

         // 1. Actualizar estado lógico Action
         System.out.println("    1. Actualizando Action.SELECTED_KEY...");
         toggleProporcionesAction.putValue(Action.SELECTED_KEY, mantener);
         System.out.println("       -> Action.SELECTED_KEY AHORA ES: " + Boolean.TRUE.equals(toggleProporcionesAction.getValue(Action.SELECTED_KEY)));

         // 2. Actualizar Modelo
          System.out.println("    2. Actualizando Modelo...");
         model.setMantenerProporcion(mantener); // Llama al nuevo setter del modelo
          // El log ya está dentro del setter del modelo

         // 3. Actualizar Configuración en Memoria
          System.out.println("    3. Actualizando Configuración en Memoria...");
         String configKey = "interfaz.menu.zoom.Mantener_Proporciones.seleccionado";
         configuration.setString(configKey, String.valueOf(mantener));
         System.out.println("       -> Config '" + configKey + "' AHORA ES: " + configuration.getString(configKey));

         // 4. Sincronizar UI (Botón) - El CheckBox se actualiza solo por setAction
         System.out.println("    4. Sincronizando UI (Botón)...");
         actualizarAspectoBotonToggle(toggleProporcionesAction, mantener); // Actualiza color/apariencia botón

         // 5. Repintar la Imagen Principal
         // Como cambiar la proporción afecta directamente cómo se ve, forzamos repintado.
         System.out.println("    5. Programando repintado de imagen principal en EDT...");
         SwingUtilities.invokeLater(() -> {
             System.out.println("      -> [EDT] Llamando a reescalar y mostrar imagen...");
             Image imagenReescalada = reescalarImagenParaAjustar(); // Volver a calcular con el nuevo estado del modelo
             if (view != null && model != null) { // Doble chequeo
                 // Pasar el zoom/offset actual del modelo
                 view.setImagenMostrada(imagenReescalada, model.getZoomFactor(), model.getImageOffsetX(), model.getImageOffsetY());
             }
         });

         System.out.println("[Controller setMantenerProporciones] FIN (Cambio aplicado y repintado programado).");
     }
	
} // Fin de VisorController


