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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

// Actions
import controlador.actions.archivo.OpenFileAction;
import controlador.actions.navegacion.FirstImageAction;
import controlador.actions.navegacion.LastImageAction;
import controlador.actions.navegacion.NextImageAction;
import controlador.actions.navegacion.PreviousImageAction;
import controlador.actions.tema.ToggleThemeAction;
import controlador.actions.zoom.ResetZoomAction;
import controlador.actions.zoom.ToggleZoomManualAction;
// Imports de mis clases
import modelo.VisorModel;
import servicios.ConfigurationManager;
import vista.VisorView;
import vista.config.ViewUIConfig;
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
	
	private VisorModel model;
	private VisorView view;
	private ConfigurationManager configuration;
	private IconUtils iconUtils;
	private ThemeManager themeManager;
	
	// --- Servicios ---
	private ExecutorService executorService;

	// --- Estado del Controlador (cosas que no son datos puros del modelo) ---
	private int lastMouseX, lastMouseY; // Para drag
	private volatile boolean estaCargandoLista = false; // Flag de estado de carga
	private Future<?> cargaImagenesFuture;
	private Future<?> cargaMiniaturasFuture;
	private Future<?> cargaImagenPrincipalFuture = null;
	
	// --- Instancias de Action ---
	// TODO: Añade actions para todas las demás funcionalidades
	
	//Navegacion 
	private Action firstImageAction;
	private Action previousImageAction;
	private Action nextImageAction;
	private Action lastImageAction;
	
	//Archivo
    private Action openAction;
    private Action toggleSubfoldersAction;
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
    // Guardar lista para fácil manejo
    private List<Action> themeActions;
    
    // Mapa a pasar
    private Map<String, Action> actionMap;
    
	// --- Constructor ---
    public VisorController()
    {
    	// ********************* COSAS A IMPLEMENTAR EN UN FUTURO: ***************************
    	
    	//TODO TwelveMonkeys: 	Para ampliar los tipos de imagenes soportados
    	//TODO FlatLaf:			Para configurar los colores profundamente
    	
    	// ***********************************************************************************
    	
        System.out.println("Iniciando Visor de Imágenes V2 (con ThemeManager)...");
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

        // --- 5. LEER VALORES DE UI INICIALES ---
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
                cBotonFondo, cBotonTexto, cBotonFondoActivado, cBotonFondoAnimacion
        );

        // --- 10. Crear Vista y Conectar en el EDT ---
        final VisorController controllerInstance = this;
        SwingUtilities.invokeLater(() -> {
            view = new VisorView(calculatedMiniaturePanelHeight, uiConfig);
            view.setListaImagenesModel(model.getModeloLista());
            controllerInstance.aplicarConfiguracionInicial(); // Aplica estados enabled/visible/selected
            controllerInstance.cargarEstadoInicial();       // Carga carpeta e imagen inicial
            controllerInstance.configurarListenersNoAction(); // JList, Mouse, etc.
            controllerInstance.configurarComponentActions(); // setAction para botones/menus
            view.setVisible(true);
            System.out.println("[Controller] Inicialización de UI en EDT completada.");
        }); // Fin invokeLater

        // 11. Configurar Hook de cierre
        configurarShutdownHook();
        System.out.println("[Controller] Constructor finalizado.");
    }
    

	
	
	
	// --- Método para inicializar las Actions ---
	// EN: controlador.VisorController.java

	// --- NUEVO: Método para inicializar las Actions (CORREGIDO) ---
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
	    firstImageAction = new FirstImageAction(this, this.iconUtils, iconoAncho, iconoAlto); // Pasa tamaño
	    previousImageAction = new PreviousImageAction(this, this.iconUtils, iconoAncho, iconoAlto); // Pasa tamaño
	    nextImageAction = new NextImageAction(this, this.iconUtils, iconoAncho, iconoAlto); // Pasa tamaño
	    lastImageAction = new LastImageAction(this, this.iconUtils, iconoAncho, iconoAlto); // Pasa tamaño
	    
	  //Archivo
	    openAction = new OpenFileAction(this, this.iconUtils, iconoAncho, iconoAlto); // Asume que OpenFileAction existe y carga icono
	    
	  //Edicion
	    //rotateLeftAction = new RotateLeftAction(this);
	    //rotateRightAction = new RotateRightAction(this);
	    //flipHorizontalAction = new FlipHorizontalAction(this);
	    //flipVerticalAction = new FlipVerticalAction(this);
	    //cropAction = new CropAction(this);
	    
	  //Zoom
	    toggleZoomManualAction = new ToggleZoomManualAction(this, this.iconUtils, iconoAncho, iconoAlto); // Asume que ToggleZoomManualAction existe y carga icono
	    //zoomAutoAction = new ZoomAutoAction(this);
	    //zoomWidthAction = new ZoomWidthAction(this);
	    //zoomHeightAction = new ZoomHeightAction(this);
	    //zoomFitAction = new ZoomFitAction(this);
	    //zoomFixedAction = new ZoomFixedAction(this);
	    resetZoomAction = new ResetZoomAction(this, this.iconUtils, iconoAncho, iconoAlto); // Asume que ResetZoomAction existe y carga icono
	    
	  //Servicios
	    //toggleSubfoldersAction = new ToggleSubfoldersAction(this);
	    //refreshAction = new RefreshAction(this);
	    //deleteAction = new DeleteAction(this);
	    
	  //Tema
	    temaClearAction = new ToggleThemeAction(this, "clear", "Tema Clear");
	    temaDarkAction = new ToggleThemeAction(this, "dark", "Tema Dark");
	    temaBlueAction = new ToggleThemeAction(this, "blue", "Tema Blue");
	    temaGreenAction = new ToggleThemeAction(this, "green", "Tema Gren");
	    temaOrangeAction = new ToggleThemeAction(this, "orange", "Tema Orange");
	    
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

        mapaDeAcciones.put("Zoom_Automatico", zoomAutoAction);
        mapaDeAcciones.put("Zoom_Auto_48x48", zoomAutoAction);

        mapaDeAcciones.put("Zoom_a_lo_Ancho", zoomAnchoAction);
        mapaDeAcciones.put("Ajustar_al_Ancho_48x48", zoomAnchoAction);

        mapaDeAcciones.put("Zoom_a_lo_Alto", zoomAltoAction);
        mapaDeAcciones.put("Ajustar_al_Alto_48x48", zoomAltoAction);

        mapaDeAcciones.put("Escalar_Para_Ajustar", zoomFitAction);
        mapaDeAcciones.put("Escalar_Para_Ajustar_48x48", zoomFitAction);

        mapaDeAcciones.put("Zoom_Actual_Fijo", zoomFixedAction);
        mapaDeAcciones.put("Zoom_Fijo_48x48", zoomFixedAction);
        
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
        

      //Visualizar
        
        
      // Servicios
        mapaDeAcciones.put("Menu_48x48",menuAction);         
        
        mapaDeAcciones.put("Botones_Ocultos_48x48",hiddenButtonsAction);
        
        mapaDeAcciones.put("Subcarpetas_48x48", toggleSubfoldersAction);             // Botón

        mapaDeAcciones.put("Refrescar_48x48", refreshAction); // Solo botón
        
        mapaDeAcciones.put("Recargar_Lista_de_Imagenes", refreshAction); // Opción de menú

        mapaDeAcciones.put("Eliminar_Permanentemente", deleteAction);
        
        mapaDeAcciones.put("Borrar_48x48", deleteAction);

      // Acciones de RadioButtons
        //Mostrar Subcarpeta Si/No 
        mapaDeAcciones.put("Mostrar_Imagenes_de_Subcarpetas", toggleSubfoldersAction); // Radio button 1
        mapaDeAcciones.put("Mostrar_Solo_Carpeta_Actual", toggleSubfoldersAction);    // Radio button 2 (necesitarán lógica especial en la Action)
        
        // Mostrar Tema
        mapaDeAcciones.put("Tema_Clear", 	temaClearAction);
        mapaDeAcciones.put("Tema_Dark", 	temaDarkAction);
    	mapaDeAcciones.put("Tema_Blue", 	temaBlueAction);
    	mapaDeAcciones.put("Tema_Green", 	temaGreenAction);
    	mapaDeAcciones.put("Tema_Orange", 	temaOrangeAction);
        
    	
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
    private void configurarComponentActions() 
    {
        System.out.println("[Controller] Configurando Actions en componentes...");
        if (view == null) {
            System.err.println("ERROR: Vista no inicializada al configurar actions.");
            return;
        }

        // --- MODIFICADO: Crear el builder DENTRO de la vista ---
        // La vista es la que debe crear sus builders internos.
        // El controller solo necesita asegurarse de que las actions se asignen.
        // Esta llamada a new ToolbarBuilder aquí es incorrecta porque
        // la toolbar ya se creó DENTRO de la vista.
        /*
        Map<String, Action> actionMap = createActionMap(); // Ya tienes this.actionMap
        ToolbarBuilder toolbarBuilder = new ToolbarBuilder(this.actionMap,
                this.colorFondoUI, // <--- USA el nombre correcto: colorFondoUI
                this.iconoAncho,
                this.iconoAlto);
        // MenuBarBuilder menuBuilder = new MenuBarBuilder(getMenuDefinitionString(), this.actionMap);
        */

        // --- TODO Asignar Actions a los botones OBTENIDOS de la vista ---
        
        Map<String, JButton> botones = view.getBotonesPorNombre(); // Obtiene los botones ya creados por la vista/builder
        
        if (botones != null) {
            // Mapear Clave Larga de Configuración -> Action
            setActionForKey(botones, "interfaz.boton.movimiento.Anterior_48x48", previousImageAction);
            setActionForKey(botones, "interfaz.boton.movimiento.Siguiente_48x48", nextImageAction);
            setActionForKey(botones, "interfaz.boton.zoom.Reset_48x48", resetZoomAction);
            setActionForKey(botones, "interfaz.boton.zoom.Zoom_48x48", toggleZoomManualAction);
            setActionForKey(botones, "interfaz.boton.especiales.Selector_de_Carpetas_48x48", openAction);
            // TODO: Añadir setActionForKey para TODOS los demás botones...

        } else { System.err.println("WARN: Mapa de botones es null."); }

        // --- TODO Asignar Actions a los menuitems OBTENIDOS de la vista ---
        
        Map<String, JMenuItem> menuItems = view.getMenuItemsPorNombre(); // Obtiene los items ya creados por la vista/builder
        if (menuItems != null) {
            // Mapear Clave Larga de Configuración -> Action
            setActionForKey(menuItems, "interfaz.menu.archivo.Abrir_Archivo", openAction);
            setActionForKey(menuItems, "interfaz.menu.navegacion.Imagen_Aterior", previousImageAction);
            setActionForKey(menuItems, "interfaz.menu.navegacion.Imagen_Siguiente", nextImageAction);
            setActionForKey(menuItems, "interfaz.menu.zoom.Resetear_Zoom", resetZoomAction);
            setActionForKey(menuItems, "interfaz.menu.zoom.Activar_Zoom_Manual", toggleZoomManualAction);
            // TODO: Añadir setActionForKey para TODOS los demás items de menú...

            //LOG   -> Asignando Actions de Tema a Menús...
            System.out.println("  -> Asignando Actions de Tema a Menús...");
            setActionForKey(menuItems, "interfaz.menu.configuracion.tema.Tema_Clear", temaClearAction); // Clave LARGA usada por MenuBuilder
            setActionForKey(menuItems, "interfaz.menu.configuracion.tema.Tema_Dark", temaDarkAction);
            setActionForKey(menuItems, "interfaz.menu.configuracion.tema.Tema_Blue", temaBlueAction);
            setActionForKey(menuItems, "interfaz.menu.configuracion.tema.Tema_Green", temaGreenAction);
            setActionForKey(menuItems, "interfaz.menu.configuracion.tema.Tema_Orange", temaOrangeAction);
            
            addFallbackListeners(menuItems); // Añadir listeners a los que no tienen Action

        } else { System.err.println("WARN: Mapa de menús es null."); }

        // configurarListenersNoAction(); // Esta llamada ya debería estar en el invokeLater después de crear la vista

        System.out.println("[Controller] Actions configuradas.");
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

	
	// --- Métodos de Lógica (Movidos desde VisorV2 y Adaptados) ---

    private void aplicarConfiguracionInicial() {
	    System.out.println("Aplicando configuración inicial..."); // Log inicio

	    // --- Validación: Asegurarse que todo está listo ---
	    if (configuration == null || view == null || model == null) {
	         System.err.println("ERROR [aplicarConfiguracionInicial]: Configuración, Vista o Modelo nulos. Abortando.");
	         return;
	    }

	    // --- 1. Aplicar configuración al MODELO ---
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


	    // --- 2. Aplicar configuración a la VISTA (Botones) ---
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


	    // --- 3. Aplicar configuración a la VISTA (Menús) ---
	    Map<String, JMenuItem> menuItems = view.getMenuItemsPorNombre();
	    if (menuItems != null) {
	        System.out.println("  -> Aplicando estado inicial a Menús...");
	        menuItems.forEach((claveCompletaMenu, menuItem) -> {
                 try {
                     // Solo aplicar a items finales, no a los JMenu contenedores
                     if (!(menuItem instanceof JMenu)) {
                        // Aplicar Enabled y Visible a todos los items finales
                        menuItem.setEnabled(configuration.getBoolean(claveCompletaMenu + ".activado", true));
                        menuItem.setVisible(configuration.getBoolean(claveCompletaMenu + ".visible", true));

                        // Aplicar estado seleccionado para CheckBox y RadioButton
                        if (menuItem instanceof JCheckBoxMenuItem) {
                            ((JCheckBoxMenuItem) menuItem).setSelected(configuration.getBoolean(claveCompletaMenu + ".seleccionado", false));
                        } else if (menuItem instanceof JRadioButtonMenuItem) {

                            // --- TEXTO MODIFICADO: LÓGICA PARA RADIO BUTTONS ---

                            // Identificar si es un RadioButton de Tema
                            boolean esTemaRadioButton = claveCompletaMenu.startsWith("interfaz.menu.configuracion.tema.Tema_"); // Comprobar prefijo

                            if (esTemaRadioButton) {
                                // *** NO HACER NADA AQUÍ ***
                                // El estado 'seleccionado' de los radio buttons de tema
                                // es controlado automáticamente por la propiedad Action.SELECTED_KEY
                                // de la ToggleThemeAction asignada. Esta propiedad se establece
                                // correctamente en el constructor de ToggleThemeAction al inicio,
                                // basándose en el tema leído de la configuración.
                                // Leer y establecer 'setSelected' desde el archivo aquí
                                // causaría inconsistencias visuales.
                                // System.out.println("    -> Omitiendo setSelected para RadioButton de tema: " + claveCompletaMenu); // Log opcional

                            } else if (claveCompletaMenu.equals("interfaz.menu.configuracion.carga_de_imagenes.Mostrar_Solo_Carpeta_Actual")) {
                                 // Caso especial: Radio de Subcarpetas (depende del Modelo)
                                 ((JRadioButtonMenuItem) menuItem).setSelected(model.isMostrarSoloCarpetaActual());
                            } else if (claveCompletaMenu.equals("interfaz.menu.configuracion.carga_de_imagenes.Mostrar_Imagenes_de_Subcarpetas")) {
                                 // Caso especial: Radio de Subcarpetas (depende del Modelo)
                                 ((JRadioButtonMenuItem) menuItem).setSelected(!model.isMostrarSoloCarpetaActual());
                            } else {
                                // Para CUALQUIER OTRO JRadioButtonMenuItem, leer su estado
                                // 'seleccionado' directamente del archivo de configuración.
                                ((JRadioButtonMenuItem) menuItem).setSelected(configuration.getBoolean(claveCompletaMenu + ".seleccionado", false));
                            }
                            // --- FIN TEXTO MODIFICADO ---
                        }
                     } // Fin if (!(menuItem instanceof JMenu))
                 } catch (Exception e) {
                    System.err.println("ERROR [aplicarConfiguracionInicial]: Aplicando estado a Menú '" + claveCompletaMenu + "': " + e.getMessage());
                 }
	        });
             System.out.println("  -> Estado inicial de Menús aplicado.");
	    } else {
	         System.err.println("WARN [aplicarConfiguracionInicial]: view.getMenuItemsPorNombre() es null.");
	    }


	    // --- 4. Aplicar estado inicial de Zoom Manual ---
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

	    System.out.println("Configuración inicial aplicada (Modelo y Vista)."); // Log fin
	}


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
		model.limpiarMiniaturasMap();
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

		// Desactivar zoom si estaba activo
		// if (model.isZoomHabilitado()) {
		// setManualZoomEnabled(false);
		// }
	}

	/**
	 * Carga o recarga la lista de imágenes desde la carpeta configurada. Actualiza
	 * el modelo y la vista correspondientes.
	 */
	private void cargarListaImagenes ()
	{

		if (configuration == null)
		{

			System.err.println("ERROR: ConfigurationManager nulo en cargarListaImagenes.");

			// Podríamos intentar mostrar un error en la vista si ya está inicializada
			if (view != null)
			{

				JOptionPane.showMessageDialog(view.getFrame(), "Error interno de configuración.", "Error",
						JOptionPane.ERROR_MESSAGE);

			}
			return;

		}
		//LOG \n\n-->>> INICIO CARGAR LISTA (Controller) ***
		System.out.println("\n\n-->>> INICIO CARGAR LISTA (Controller) ***");
		this.estaCargandoLista = true; // Marcar inicio de carga

		// Cancelar tareas anteriores
		if (cargaImagenesFuture != null && !cargaImagenesFuture.isDone())
		{

			System.out.println("[Controller] Cancelando tarea de carga de lista anterior...");
			cargaImagenesFuture.cancel(true);

		}

		if (cargaMiniaturasFuture != null && !cargaMiniaturasFuture.isDone())
		{

			System.out.println("[Controller] Cancelando tarea de carga de miniaturas anterior...");
			cargaMiniaturasFuture.cancel(true);

		}

		// Obtener configuración necesaria
		final String carpetaInicial = configuration.getString("inicio.carpeta", "");
		// Leer modo desde el MODELO (ya que aplicarConfiguracionInicial lo puso ahí)
		final boolean usarSoloCarpetaActual = model.isMostrarSoloCarpetaActual();

		//LOG -->>>Iniciando carga para:
		//System.out.println("-->>>Iniciando carga para: \"" + carpetaInicial + "\" | Solo Carpeta Actual: "+ usarSoloCarpetaActual);

		// Validar carpeta inicial
		Path startPath = null;

		if (carpetaInicial != null && !carpetaInicial.isEmpty())
		{

			try
			{

				startPath = Paths.get(carpetaInicial);

				if (!Files.isDirectory(startPath))
				{

					System.err.println("Error: La ruta inicial no es un directorio válido: " + carpetaInicial);
					startPath = null; // Invalidar si no es directorio

				}

			} catch (java.nio.file.InvalidPathException ipe)
			{

				System.err.println("Error: La ruta inicial no es válida: " + carpetaInicial + " - " + ipe.getMessage());
				startPath = null; // Invalidar si la ruta es incorrecta

			}

		}

		if (startPath != null)
		{

			final Path finalStartPath = startPath; // Necesario para lambda

			// Limpieza inicial UI y Modelo en EDT
			SwingUtilities.invokeLater( () -> {

				view.limpiarPanelMiniaturas();
				view.limpiarImagenMostrada();
				view.setTextoRuta("");
				view.setTituloPanelIzquierdo("Cargando Lista...");
				// Limpiamos el contenido del modelo existente y mapas asociados
				model.limpiarModeloLista();
				model.limpiarRutaCompletaMap();
				model.limpiarMiniaturasMap();

				// No necesitamos reasignar el modelo a la JList aquí
			});

			// Iniciar tarea en background
			cargaImagenesFuture = executorService.submit( () -> {

				// Colecciones temporales para este hilo
				DefaultListModel<String> nuevoModeloTemp = new DefaultListModel<>();
				Map<String, Path> nuevoRutaCompletaMapTemp = new HashMap<>();
				Set<String> archivosAgregadosTemp = new HashSet<>(); // Para evitar duplicados si walk los devuelve
				//long taskStartTime = System.currentTimeMillis();
				
				//LOG -->>> Tarea background (lista) iniciada
				//System.out.println("-->>> Tarea background (lista) iniciada @ " + taskStartTime);

				try
				{

					int depth = usarSoloCarpetaActual ? 1 : Integer.MAX_VALUE;
					System.out.println(
							"[Controller BG] Iniciando Files.walk con depth: " + depth + " en " + finalStartPath);

					// Usar try-with-resources para el Stream
					try (Stream<Path> stream = Files.walk(finalStartPath, depth))
					{

						stream.filter(path -> !path.equals(finalStartPath) && Files.isRegularFile(path)) // Ignorar el
																											// directorio
																											// raíz y
																											// solo
																											// archivos
								.filter(this::esArchivoImagenSoportado) // Filtrar por extensión
								.forEach(path -> {

									if (Thread.currentThread().isInterrupted())
									{

										System.out.println("[Controller BG] Tarea interrumpida durante forEach.");
										throw new RuntimeException("Tarea cancelada");

									}
									// Obtener ruta relativa y normalizarla
									Path relativePath = finalStartPath.relativize(path);
									String uniqueKey = relativePath.toString().replace("\\", "/");

									// Añadir a colecciones temporales si no está duplicado
									if (archivosAgregadosTemp.add(uniqueKey))
									{

										nuevoModeloTemp.addElement(uniqueKey);
										nuevoRutaCompletaMapTemp.put(uniqueKey, path);

									} else
									{

										System.out.println("[Controller BG] Clave duplicada ignorada: " + uniqueKey);
									}

								});

					} catch (IOException ioEx)
					{

						System.err.println("[Controller BG] Error IO durante Files.walk en " + finalStartPath + ": "
								+ ioEx.getMessage());
						throw new RuntimeException("Error al leer directorio", ioEx); // Re-lanzar para captura general

					} catch (SecurityException secEx)
					{

						System.err.println("[Controller BG] Error de seguridad durante Files.walk en " + finalStartPath
								+ ": " + secEx.getMessage());
						throw new RuntimeException("Error de permisos directorio", secEx); // Re-lanzar

					}

					System.out.println("[Controller BG] Files.walk terminado. Elementos encontrados: "
							+ nuevoModeloTemp.getSize());

					// Solo proceder a actualizar UI si no se interrumpió
					if (!Thread.currentThread().isInterrupted())
					{

						System.out.println(
								"[Controller BG] Modelo temporal llenado. Tamaño: " + nuevoModeloTemp.getSize());

						if (!nuevoModeloTemp.isEmpty())
						{

							System.out.println("[Controller BG] Primer elemento: " + nuevoModeloTemp.getElementAt(0));

						}

						// Actualizar MODELO y VISTA en EDT
						final DefaultListModel<String> finalNuevoModeloTemp = nuevoModeloTemp; // Final para lambda
						final Map<String, Path> finalNuevoRutaCompletaMapTemp = nuevoRutaCompletaMapTemp; // Final para
																											// lambda
						SwingUtilities.invokeLater( () -> {

							//LOG -->>> Actualizando contenido del modelo existente en EDT. Tamaño temporal
							//System.out.println("-->>> Actualizando contenido del modelo existente en EDT. Tamaño temporal: "+ finalNuevoModeloTemp.getSize());

							// --- Actualizar Modelo (¡CORREGIDO!) ---
							DefaultListModel<String> modeloActual = model.getModeloLista();
							modeloActual.clear(); // Limpiar el existente

							for (int idx = 0; idx < finalNuevoModeloTemp.getSize(); idx++)
							{

								modeloActual.addElement(finalNuevoModeloTemp.getElementAt(idx)); // Añadir nuevos

							}
							model.setRutaCompletaMap(finalNuevoRutaCompletaMapTemp); // Actualizar mapa de rutas

							//LOG [Controller EDT] Contenido del modelo principal actualizado. Tamaño:
							//System.out.println("[Controller EDT] Contenido del modelo principal actualizado. Tamaño: "+ model.getModeloLista().getSize());

							// --- Actualizar Vista ---
							view.setTituloPanelIzquierdo("Archivos: " + model.getModeloLista().getSize());

							// Marcar fin de carga lógica ANTES de seleccionar índice
							estaCargandoLista = false;
							System.out.println("-->>> estaCargandoLista = false");

							// Seleccionar imagen inicial (o la primera)
							if (model.getModeloLista().getSize() > 0)
							{

								//LOG [Controller EDT] Intentando seleccionar imagen inicial...
								//System.out.println("[Controller EDT] Intentando seleccionar imagen inicial...");
								cargarImagenInicial(configuration.getString("inicio.imagen", null)); // Pasar null si no
																										// hay default

							} else
							{

								System.out.println("-->>> Lista vacía tras carga. Limpiando UI.");
								limpiarUI(); // Asegura limpieza si no se encontraron imágenes

							}

						});

					} else
					{

						// Manejo si fue interrumpido ANTES de actualizar UI
						//LOG "[Controller BG] Tarea interrumpida ANTES de actualizar UI.
						//System.out.println("[Controller BG] Tarea interrumpida ANTES de actualizar UI.");
						SwingUtilities.invokeLater( () -> {

							view.setTituloPanelIzquierdo("Carga Cancelada");
							estaCargandoLista = false;
							
							//LOG -->>> estaCargandoLista = false (cancelado antes de UI update
							//System.out.println("-->>> estaCargandoLista = false (cancelado antes de UI update)");
							
							limpiarUI(); // Limpia todo

						});

					}

				} catch (RuntimeException e)
				{ // Captura errores del forEach, IO o Security de walk

					final String errorMsg = e.getMessage();
					final Throwable cause = e.getCause(); // Obtener causa original si existe
					
					//LOG [Controller BG] Error en tarea background:
					//System.err.println("[Controller BG] Error en tarea background: " + errorMsg + (cause != null ? " Causa: " + cause.getMessage() : ""));

					SwingUtilities.invokeLater( () -> { // Actualizar UI sobre el error

						String dialogTitle = "Error de Carga";
						String dialogMessage = "Error durante la carga de imágenes: " + errorMsg;

						if ("Tarea cancelada".equals(errorMsg))
						{

							view.setTituloPanelIzquierdo("Carga Cancelada");

							// No mostrar diálogo por cancelación normal
						} else
						{

							if ("Error al leer directorio".equals(errorMsg))
							{

								dialogTitle = "Error de Acceso";
								dialogMessage = "No se pudo leer el contenido de la carpeta:\n" + finalStartPath
										+ (cause != null ? "\nDetalle: " + cause.getMessage() : "");

							} else if ("Error de permisos directorio".equals(errorMsg))
							{

								dialogTitle = "Error de Permisos";
								dialogMessage = "No se tienen permisos para leer la carpeta:\n" + finalStartPath
										+ (cause != null ? "\nDetalle: " + cause.getMessage() : "");

							}
							JOptionPane.showMessageDialog(view.getFrame(), dialogMessage, dialogTitle,
									JOptionPane.ERROR_MESSAGE);
							view.setTituloPanelIzquierdo(dialogTitle); // Poner título indicando error

						}

						estaCargandoLista = false;
//						System.out.println("-->>> estaCargandoLista = false (por error/cancelación en BG)");
						limpiarUI(); // Limpia todo en caso de error

					});

					// Imprimir stack trace para depuración interna si no fue cancelación
					if (!"Tarea cancelada".equals(errorMsg))
					{

						e.printStackTrace();

					}

				} // Fin try-catch principal del submit
//				long taskEndTime = System.currentTimeMillis();
//				System.out.println("-->>> Duración TOTAL Tarea background (lista): " + (taskEndTime - taskStartTime) + " ms");

			}); // Fin submit

		} else
		{

			// Carpeta inicial no válida o no definida
			System.out.println(
					"No se puede cargar la lista: Carpeta inicial no definida o inválida: \"" + carpetaInicial + "\"");
			// Limpiar UI en el EDT
			SwingUtilities.invokeLater(this::limpiarUI);
			estaCargandoLista = false; // Marcar como no cargando

		}
//		System.out.println("-->>> FIN MÉTODO cargarListaImagenes (Controller) ***\n\n"); // Indica que el método
																							// principal terminó

	}

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
	                    JOptionPane.showMessageDialog(view.getFrame(),
	                        "Error al cargar la imagen:\n" + finalPath.getFileName() +
	                        (finalErrorMsg != null ? "\n" + finalErrorMsg : ""),
	                        "Error de Carga", JOptionPane.ERROR_MESSAGE);
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
	private Image reescalarImagenParaAjustar ()
	{

		BufferedImage current = model.getCurrentImage();
		if (current == null)
			return null;

		int width = view.getEtiquetaImagen().getWidth();
		int height = view.getEtiquetaImagen().getHeight();

		if (width <= 0 || height <= 0)
		{

			// Si la etiqueta aún no tiene tamaño, no podemos escalar bien.
			// Devolver la imagen original o null. Devolver null evita dibujar algo
			// incorrecto.
			return null;

			// Alternativa: return current; // Pero puede ser muy grande
		}

		double imageAspectRatio = (double) current.getWidth() / current.getHeight();
		double panelAspectRatio = (double) width / height;
		int newWidth;
		int newHeight;

		if (panelAspectRatio > imageAspectRatio)
		{

			newHeight = height;
			newWidth = (int) (height * imageAspectRatio);

		} else
		{

			newWidth = width;
			newHeight = (int) (width / imageAspectRatio);

		}

		// Usar SCALE_SMOOTH para mejor calidad, aunque es más lento
		return current.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);

		// Alternativa más rápida pero de peor calidad: Image.SCALE_FAST
		// Alternativa intermedia: Image.SCALE_REPLICATE
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

	
	

	/**
	 * Abre el selector de carpetas y carga la seleccionada.
	 */
	public void abrirSelectorDeCarpeta ()
	{

		if (configuration == null)
			return;
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

	
    /**
     * Carga y muestra las miniaturas para el rango visible alrededor del índice seleccionado.
     * Muestra placeholders inmediatamente, carga/escala en background, y actualiza la UI.
     * Maneja errores específicos como IOException y OutOfMemoryError.
     *
     * @param indiceSeleccionado El índice de la imagen actualmente seleccionada en la lista principal.
     */
    private void actualizarImagenesMiniaturaConRango(int indiceSeleccionado) {
        // --- Verificar si los servicios necesarios están disponibles ---
        if (executorService == null || executorService.isShutdown()) {
            System.err.println("[Miniaturas] No se pueden cargar: ExecutorService no está activo.");
            return;
        }
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
        // Corregir índice si está fuera de rango
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

        // --- Calcular rango y hacerlos FINAL ---
        final int inicio = Math.max(0, indiceSeleccionado - model.getMiniaturasAntes());
        final int fin = Math.min(totalImagenes - 1, indiceSeleccionado + model.getMiniaturasDespues());
        // -------------------------------------

        System.out.println("[Miniaturas] Rango calculado: " + inicio + " a " + fin + " (Seleccionado: " + indiceSeleccionado + ")");

        final int finalIndiceSeleccionado = indiceSeleccionado; // Usar el índice potencialmente corregido

        // --- Placeholders en EDT ---
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
            
            //LOG [Miniaturas BG] Tarea iniciada para rango
            //System.out.println("[Miniaturas BG] Tarea iniciada para rango " + inicio + "-" + fin);

            try { // <-- TRY principal de la tarea
                for (int i = inicio; i <= fin; i++) { // <-- Abre FOR BG
                     if (Thread.currentThread().isInterrupted()) {
                    	 
                    	 //LOG [Miniaturas BG] Tarea interrumpida en bucle (índice
                         System.out.println("[Miniaturas BG] Tarea interrumpida en bucle (índice " + i + ").");
                         return;
                     }

                     // Validar índice de nuevo por seguridad
                     if (i < 0 || i >= model.getModeloLista().getSize()) {
                    	 
                         System.err.println("WARN [Miniaturas BG]: Índice " + i + " fuera de rango del modelo.");
                         
                         continue; // Saltar esta iteración
                     }
                     String uniqueKey = model.getModeloLista().getElementAt(i);
                     Path ruta = model.getRutaCompleta(uniqueKey);

                     if (ruta == null || !Files.exists(ruta)) {
                    	  
                          System.err.println("[Miniaturas BG] Ruta inválida o no encontrada para clave: " + uniqueKey);
                          continue; // Saltar, el placeholder de error ya está (o debería estar)
                     }

                     JLabel etiquetaReal = null;
                     // --- Bloque TRY-CATCH para UNA miniatura ---
                     try { // <-- Abre TRY miniatura individual
                         final boolean esSeleccionada = (i == finalIndiceSeleccionado);
//                         final int indiceActual = i; // Para listener

                         ImageIcon miniaturaIcon = model.getMiniatura(uniqueKey);
                         Image imagenBase = null;

                         final int anchoObjetivo = esSeleccionada ? model.getMiniaturaSelAncho() : model.getMiniaturaNormAncho();
                         final int altoObjetivo = esSeleccionada ? model.getMiniaturaSelAlto() : model.getMiniaturaNormAlto();

                         if (anchoObjetivo <= 0 || altoObjetivo <= 0) {
                              throw new IllegalArgumentException("Dimensiones objetivo inválidas ("+anchoObjetivo+"x"+altoObjetivo+")");
                         }

                         boolean necesitaCargaOReescalado = false;
                          if (miniaturaIcon == null) {
                              necesitaCargaOReescalado = true;
                          } else if (esSeleccionada && (miniaturaIcon.getIconWidth() != anchoObjetivo || miniaturaIcon.getIconHeight() != altoObjetivo)) {
                              necesitaCargaOReescalado = true;
                          } else if (!esSeleccionada && (miniaturaIcon.getIconWidth() != anchoObjetivo || miniaturaIcon.getIconHeight() != altoObjetivo)) {
                              necesitaCargaOReescalado = true;
                              
                              System.out.println("WARN [Miniaturas BG]: Miniatura normal en caché con tamaño incorrecto para " + uniqueKey + ". Reescalando/recargando.");
                          }

                         if (necesitaCargaOReescalado) {
                              if (miniaturaIcon != null && esSeleccionada) { // Si es seleccionada y tenemos caché (normal), reescalamos desde ahí
                                   imagenBase = miniaturaIcon.getImage();
                                   // System.out.println("  [Miniaturas BG] Reescalando desde caché para " + uniqueKey + " a " + anchoObjetivo + "x" + altoObjetivo);
                              } else { // Si no hay caché, o no es seleccionada (queremos tamaño normal exacto), o caché normal tenía tamaño erróneo
                                  // Cargar desde disco
                                  // System.out.println("  [Miniaturas BG] Cargando desde disco para " + uniqueKey);
                                  BufferedImage bufferedImg = ImageIO.read(ruta.toFile());
                                  if (bufferedImg == null) throw new IOException("Formato no soportado o archivo inválido para ImageIO");
                                  imagenBase = bufferedImg;
                              }

                              // Escalar
                              int altoFinal = (altoObjetivo <= 0) ? -1 : altoObjetivo;
                              Image escalada = imagenBase.getScaledInstance(anchoObjetivo, altoFinal, Image.SCALE_SMOOTH);
                              if (escalada == null) throw new IOException("Resultado de escalado fue null");
                              miniaturaIcon = new ImageIcon(escalada);
                              // Esperar a que la imagen escalada esté lista (importante!)
                              if (miniaturaIcon.getImageLoadStatus() != java.awt.MediaTracker.COMPLETE) {
                                  System.out.println("  [Miniaturas BG] Esperando carga de imagen escalada para " + uniqueKey);
                                  // Podríamos añadir un bucle con espera corta, pero ImageIcon suele ser síncrono aquí
                                  // Si sigue dando problemas, investigar MediaTracker explícito
                              }


                              // Guardar en caché SI es tamaño NORMAL y la carga fue exitosa
                              boolean esTamanoNormal = !esSeleccionada &&
                                                       (miniaturaIcon.getIconWidth() == model.getMiniaturaNormAncho()) &&
                                                       ( (model.getMiniaturaNormAlto() <= 0 && miniaturaIcon.getIconHeight() > 0) ||
                                                         (miniaturaIcon.getIconHeight() == model.getMiniaturaNormAlto()) );

                              if (esTamanoNormal) {
                            	  
                            	  //LOG [Miniaturas BG] Guardando miniatura NORMAL en caché
                                  //System.out.println("  [Miniaturas BG] Guardando miniatura NORMAL en caché para " + uniqueKey);
                                   
                                   model.putMiniatura(uniqueKey, miniaturaIcon);
                              }
                         } else {
                              // System.out.println("  [Miniaturas BG] Usando miniatura desde caché para " + uniqueKey);
                         }

                         // Crear JLabel real
                         etiquetaReal = new JLabel(miniaturaIcon);
                         // Calcular alto real por si se mantuvo proporción
                         int altoReal = (altoObjetivo <=0 && miniaturaIcon != null) ? miniaturaIcon.getIconHeight() : altoObjetivo;
                         if(altoReal <= 0) altoReal = anchoObjetivo; // Fallback si algo falla
                         etiquetaReal.setPreferredSize(new Dimension(anchoObjetivo, altoReal));

                         etiquetaReal.setOpaque(true);
                         etiquetaReal.setToolTipText(ruta.getFileName().toString());
                         etiquetaReal.setHorizontalAlignment(SwingConstants.CENTER);
                         etiquetaReal.setVerticalAlignment(SwingConstants.CENTER);
                         if (esSeleccionada) {
                             etiquetaReal.setBorder(BorderFactory.createLineBorder(Color.BLUE, 3));
                             etiquetaReal.setBackground(new Color(200, 200, 255));
                         } else {
                             etiquetaReal.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                             etiquetaReal.setBackground(Color.WHITE);
                         }

                         // Añadir Listener
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

                     // --- Catch Específicos para UNA miniatura ---
                     } catch (IOException ioe) {
                          System.err.println("[Miniaturas BG] Error IO procesando miniatura " + uniqueKey + ": " + ioe.getMessage());
                          etiquetaReal = null;
                     } catch (IllegalArgumentException iae) {
                          System.err.println("[Miniaturas BG] Error Args procesando miniatura " + uniqueKey + ": " + iae.getMessage());
                          etiquetaReal = null;
                     } catch (OutOfMemoryError oom) {
                         System.err.println("[Miniaturas BG] Falta de memoria procesando miniatura " + uniqueKey + ": " + oom.getMessage());
                         etiquetaReal = null;
                         model.limpiarMiniaturasMap();
                         // Considera no continuar el bucle si falta memoria
                         // break; // O return; si quieres parar toda la tarea
                     } catch (Exception e) {
                         System.err.println("[Miniaturas BG] Error INESPERADO procesando miniatura " + uniqueKey + ": " + e.getMessage());
                         e.printStackTrace();
                         etiquetaReal = null;
                     } // --- Fin TRY-CATCH miniatura individual ---


                     if (etiquetaReal != null) {
                         miniaturasRealesMap.put(i, etiquetaReal); // Guardar por índice absoluto
                     } else {
                         // Asegurarse de que el mapa contenga algo (incluso null) para este índice
                         // para que el bucle de actualización sepa que hubo un error
                          miniaturasRealesMap.put(i, null);
                     }


                } // <-- Cierra FOR BG

                // --- Actualizar la VISTA en el EDT ---
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

            } catch (Exception e) { // <-- Catch TAREA GRAVE
                System.err.println("[Miniaturas] Error GRAVE en tarea background: " + e.getMessage());
                e.printStackTrace();
            } finally { // <-- Abre FINALLY
                long miniTaskEnd = System.currentTimeMillis();
                System.out.println(" Tarea Miniaturas terminada en " + (miniTaskEnd - miniTaskStart) + " ms.");
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
	private void configurarShutdownHook ()
	{

		Runtime.getRuntime().addShutdownHook(new Thread( () -> {

			System.out.println("Hook: Guardando configuración...");

			if (configuration != null)
			{

				guardarConfiguracionActual(); // Llama a método del controller

			} else
			{

				/* ... */ }

			System.out.println("Hook: Apagando ExecutorService...");

			if (executorService != null && !executorService.isShutdown())
			{

				// ... (lógica de apagado del ExecutorService igual que antes) ...
				executorService.shutdown();

				try
				{

					if (!executorService.awaitTermination(5, TimeUnit.SECONDS))
					{

						executorService.shutdownNow();

						if (!executorService.awaitTermination(5, TimeUnit.SECONDS))
						{

							/* ... */ }

					}

				} catch (InterruptedException e)
				{

					executorService.shutdownNow();
					Thread.currentThread().interrupt();

				}

			} else
			{

				/* ... */ }
			System.out.println("--- Shutdown Hook Terminado ---");

		}, "AppShutdownThread"));

	}
	
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
} // Fin de VisorController


