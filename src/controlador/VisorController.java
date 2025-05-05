package controlador;

import java.awt.BorderLayout;
// --- Imports Esenciales ---
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;     // Placeholder
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

// --- Imports de Actions ---
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
// --- Imports de Modelo, Servicios y Vista ---
import modelo.VisorModel;
import servicios.ConfigurationManager;
import servicios.image.ImageEdition;
import servicios.image.ThumbnailService;
import utils.StringUtils;
import vista.VisorView;
import vista.config.ViewUIConfig;
import vista.dialogos.ProgresoCargaDialog;
import vista.theme.Tema;
import vista.theme.ThemeManager;
import vista.util.IconUtils;

/**
 * Controlador principal para el Visor de Imágenes (Versión con 2 JList sincronizadas).
 * Orquesta la interacción entre Modelo y Vista, maneja acciones y lógica de negocio.
 */
public class VisorController implements ActionListener, ClipboardOwner, KeyEventDispatcher {

    // --- 1. Referencias a Componentes del Sistema ---
    private VisorModel model;						// El modelo de datos principal de la aplicación
    private VisorView view;							// Clase principal de la Interfaz Grafica
    private ConfigurationManager configuration;		// Gestor del archivo de configuracion
    private IconUtils iconUtils;					// utilidad para cargar y gestionar iconos de la aplicación
    private ThemeManager themeManager;				// Gestor de tema visual de la interfaz
    private ThumbnailService servicioMiniaturas;	// Servicio para gestionar las miniaturas
    private ListCoordinator listCoordinator;		// El coordinador para la selección y navegación en las listas
    private StringUtils stringUtils;				// Utilidades de Strings y log dinamico con dynamicLog
    // --- Comunicación con AppInitializer ---
    private ViewUIConfig uiConfigForView;			//
    private int calculatedMiniaturePanelHeight;		//

    private ExecutorService executorService;		 
    
    // --- 2. Estado Interno del Controlador ---
    private int lastMouseX, lastMouseY;
    private Future<?> cargaImagenesFuture;
    // private Future<?> cargaMiniaturasFuture; // Eliminado
    private Future<?> cargaImagenPrincipalFuture;
    private Path carpetaRaizActual = null;
    private volatile boolean estaCargandoLista = false;
    private volatile boolean seleccionInicialEnCurso = false; // Flag para ignorar listener durante selección inicial
    
    private DefaultListModel<String> modeloMiniaturas;
    
    // --- 3. Instancias de Actions ---
    private Action firstImageAction, previousImageAction, nextImageAction, lastImageAction;
    private Action openAction, deleteAction;
    private Action rotateLeftAction, rotateRightAction, flipHorizontalAction, flipVerticalAction, cortarAction;
    private Action toggleZoomManualAction, zoomAutoAction, resetZoomAction, zoomAnchoAction, zoomAltoAction, zoomFitAction, zoomFixedAction, zoomFijadoAction;
    private Action locateFileAction;
    private Action toggleMenuBarAction, toggleToolBarAction, toggleFileListAction, toggleThumbnailsAction, toggleLocationBarAction, toggleCheckeredBgAction, toggleAlwaysOnTopAction;
    private Action refreshAction, menuAction, hiddenButtonsAction;
    private Action temaClearAction, temaDarkAction, temaBlueAction, temaGreenAction, temaOrangeAction;
    private List<Action> themeActions;
    private Action toggleSubfoldersAction, toggleProporcionesAction;
    private Map<String, Action> actionMap;


    /**
     * Constructor principal (AHORA SIMPLIFICADO).
     * Delega toda la inicialización a AppInitializer.
     */
    public VisorController() {
        System.out.println("--- Iniciando VisorController (Constructor Simple) ---");
        AppInitializer initializer = new AppInitializer(this); // Pasa 'this'
        boolean success = initializer.initialize(); // Llama al método orquestador

        // Manejar fallo de inicialización si ocurre
        if (!success) {
             // AppInitializer ya debería haber mostrado un error y salido,
             // pero podemos añadir un log extra aquí si queremos.
             System.err.println("VisorController: La inicialización falló (ver logs de AppInitializer).");
             // Podríamos lanzar una excepción aquí o simplemente no continuar.
             // Si AppInitializer llama a System.exit(), este código no se alcanzará.
        } else {
             System.out.println("--- VisorController: Inicialización delegada completada con éxito ---");
        }
    } // --- FIN CONSTRUCTOR ---
    
    
// ----------------------------------- Métodos de Inicialización Interna -----------------------------------------------

// ************************************************************************************************************* ACTIONS

    
    /**
     * Inicializa las instancias de Action de la aplicación, EXCEPTO las de
     * navegación (Next, Previous, First, Last), ya que estas dependen del
     * ListCoordinator que se crea posteriormente en el EDT.
     * Asigna iconos a las actions usando IconUtils.
     * Configura el estado inicial de algunas actions (ej. Zoom Manual, Reset Zoom).
     * Es llamado por AppInitializer durante la fase de inicialización base.
     *
     * @param iconoAncho El ancho deseado para los iconos de las actions.
     * @param iconoAlto El alto deseado para los iconos de las actions (-1 para mantener proporción).
     */
    /*package-private*/ void initializeActionsInternal(int iconoAncho, int iconoAlto) {
        // --- SECCIÓN 1: Log de Inicio y Validación ---
        // 1.1. Imprimir log indicando el inicio de esta fase.
        System.out.println("  [Init Actions Internal] Inicializando Actions (sin navegación)...");
        // 1.2. Validar dependencias necesarias (iconUtils y configuration ya deberían estar inyectadas).
        if (this.iconUtils == null) {
            // Usar handleFatalError si iconUtils es absolutamente crítico aquí.
            handleFatalError("IconUtils nulo en initializeActionsInternal", null);
            return; // Salir si falta una dependencia crítica.
        }
         if (this.configuration == null) {
            // Podría ser menos crítico, quizás continuar con defaults, pero mejor fallar pronto.
            handleFatalError("ConfigurationManager nulo en initializeActionsInternal", null);
            return;
        }

        // --- SECCIÓN 2: Inicialización de Actions (Archivo, Edición, Zoom, Vista, Tema, Toggles) ---
        // 2.1. Actions de Archivo
        openAction = new OpenFileAction(this, this.iconUtils, iconoAncho, iconoAlto);
        // deleteAction = new DeleteAction(...); // Si la tienes
        // refreshAction = new RefreshAction(...); // Si la tienes
        // ... otras actions de archivo ...

        // 2.2. Actions de Edición
        rotateLeftAction = new RotateLeftAction(this, this.iconUtils, iconoAncho, iconoAlto);
        rotateRightAction = new RotateRightAction(this, this.iconUtils, iconoAncho, iconoAlto);
        flipHorizontalAction = new FlipHorizontalAction(this, this.iconUtils, iconoAncho, iconoAlto);
        flipVerticalAction = new FlipVerticalAction(this, this.iconUtils, iconoAncho, iconoAlto);
        // cortarAction = new CutAction(...); // Si la tienes

        // 2.3. Actions de Zoom (Todas excepto navegación)
        toggleZoomManualAction = new ToggleZoomManualAction(this, this.iconUtils, iconoAncho, iconoAlto);
        zoomAutoAction = new ZoomAutoAction(this, this.iconUtils, iconoAncho, iconoAlto);
        zoomAnchoAction = new ZoomAnchoAction(this, this.iconUtils, iconoAncho, iconoAlto);
        zoomAltoAction = new ZoomAltoAction(this, this.iconUtils, iconoAncho, iconoAlto);
        zoomFitAction = new ZoomFitAction(this, this.iconUtils, iconoAncho, iconoAlto);
        zoomFixedAction = new ZoomFixedAction(this, this.iconUtils, iconoAncho, iconoAlto);
        zoomFijadoAction = new ZoomFijadoAction(this, this.iconUtils, iconoAncho, iconoAlto);
        resetZoomAction = new ResetZoomAction(this, this.iconUtils, iconoAncho, iconoAlto);

        // 2.4. Actions de Imagen (Localizar, etc.)
        locateFileAction = new LocateFileAction(this, this.iconUtils, iconoAncho, iconoAlto);

        // 2.5. Actions de Vista (Toggles de visibilidad)
        toggleMenuBarAction = new ToggleMenuBarAction(this);
        toggleToolBarAction = new ToggleToolBarAction(this);
        toggleFileListAction = new ToggleFileListAction(this);
        toggleThumbnailsAction = new ToggleThumbnailsAction(this);
        toggleLocationBarAction = new ToggleLocationBarAction(this);
        toggleCheckeredBgAction = new ToggleCheckeredBackgroundAction(this);
        toggleAlwaysOnTopAction = new ToggleAlwaysOnTopAction(this);

        // 2.6. Actions de Tema
        temaClearAction = new ToggleThemeAction(this, "clear", "Tema Clear");
        temaDarkAction = new ToggleThemeAction(this, "dark", "Tema Dark");
        temaBlueAction = new ToggleThemeAction(this, "blue", "Tema Blue");
        temaGreenAction = new ToggleThemeAction(this, "green", "Tema Green");
        temaOrangeAction = new ToggleThemeAction(this, "orange", "Tema Orange");
        // Agruparlas para facilitar la actualización de selección
        themeActions = List.of(temaClearAction, temaDarkAction, temaBlueAction, temaGreenAction, temaOrangeAction);

        // 2.7. Actions de Toggle Generales (Subcarpetas, Proporciones)
        toggleSubfoldersAction = new ToggleSubfoldersAction (this, this.iconUtils, iconoAncho, iconoAlto);
        toggleProporcionesAction = new ToggleProporcionesAction (this, this.iconUtils, iconoAncho, iconoAlto);

        // 2.8 Actions Misceláneas (si las hubiera, ej. botones especiales sin categoría clara)
         // menuAction = ...
         // hiddenButtonsAction = ...
         // deleteAction = ... (Si no la pusiste en Archivo)
         // refreshAction = ... (Si no la pusiste en Archivo)


        // --- SECCIÓN 3: Configuración del Estado Inicial de Actions Específicas ---
        // 3.1. Leer estado inicial de config para Toggles que lo necesiten.
        try {
    
        	 // 3.1.1. Estado inicial de Zoom Manual
             if (toggleZoomManualAction != null) {
                 boolean zoomManualInicial = configuration.getBoolean("interfaz.menu.zoom.Activar_Zoom_Manual.seleccionado", false);
                 toggleZoomManualAction.putValue(Action.SELECTED_KEY, zoomManualInicial);
                 System.out.println("    -> Estado inicial Action 'toggleZoomManual' (SELECTED_KEY) puesto a: " + zoomManualInicial);
             }
             
             // 3.1.2. Estado inicial de Reset Zoom (depende de Zoom Manual)
             if (resetZoomAction != null) {
                  // Se habilita solo si el zoom manual está activo inicialmente.
                 resetZoomAction.setEnabled(Boolean.TRUE.equals(toggleZoomManualAction.getValue(Action.SELECTED_KEY)));
                 System.out.println("    -> Estado inicial Action 'resetZoom' (enabled) puesto a: " + resetZoomAction.isEnabled());
             }
             
             // 3.1.3. Estado inicial de Mantener Proporciones
             if (toggleProporcionesAction != null) {
                 boolean propInicial = configuration.getBoolean("interfaz.menu.zoom.Mantener_Proporciones.seleccionado", true);
                 toggleProporcionesAction.putValue(Action.SELECTED_KEY, propInicial);
                  System.out.println("    -> Estado inicial Action 'toggleProporciones' (SELECTED_KEY) puesto a: " + propInicial);
             }
             
              // 3.1.4. Estado inicial de Cargar Subcarpetas
             if (toggleSubfoldersAction != null) {
                  boolean subInicial = configuration.getBoolean("comportamiento.carpeta.cargarSubcarpetas", true);
                  toggleSubfoldersAction.putValue(Action.SELECTED_KEY, subInicial);
                   System.out.println("    -> Estado inicial Action 'toggleSubfolders' (SELECTED_KEY) puesto a: " + subInicial);
             }
              // 3.1.5. Estado inicial de Fondo a Cuadros
             if (toggleCheckeredBgAction != null) {
                 boolean checkInicial = configuration.getBoolean("interfaz.menu.vista.Fondo_a_Cuadros.seleccionado", false);
                 toggleCheckeredBgAction.putValue(Action.SELECTED_KEY, checkInicial);
                  System.out.println("    -> Estado inicial Action 'toggleCheckeredBg' (SELECTED_KEY) puesto a: " + checkInicial);
             }
             // 3.1.6. Estado inicial de Siempre Encima
             if (toggleAlwaysOnTopAction != null) {
                 boolean topInicial = configuration.getBoolean("interfaz.menu.vista.Mantener_Ventana_Siempre_Encima.seleccionado", false);
                 toggleAlwaysOnTopAction.putValue(Action.SELECTED_KEY, topInicial);
                 System.out.println("    -> Estado inicial Action 'toggleAlwaysOnTop' (SELECTED_KEY) puesto a: " + topInicial);
             }
             // 3.1.7. Estado inicial de visibilidad de componentes (MenuBar, ToolBar, etc.)
             //        Las Actions ToggleMenuBarAction, etc., leen su estado inicial
             //        internamente desde ConfigurationManager cuando se crean o se usan.
             //        No es estrictamente necesario ponerles SELECTED_KEY aquí, pero
             //        podría hacerse por consistencia si se quisiera. Ejemplo:

             // 3.1.8. Sincronizar estado inicial de selección de Tema (marca el radio correcto)
             if (themeActions != null && themeManager != null) {
                 String temaInicialConfig = themeManager.getTemaActual().nombreInterno();
                  System.out.println("    -> Sincronizando estado inicial radios de Tema a: " + temaInicialConfig);
                 for(Action themeAction : themeActions) {
                     if (themeAction instanceof ToggleThemeAction) {
                         ((ToggleThemeAction)themeAction).actualizarEstadoSeleccion(temaInicialConfig);
                     }
                 }
             }


        } catch (Exception e) {
            System.err.println("ERROR configurando estado inicial de Actions: " + e.getMessage());
            // Considerar si continuar o tratar como fatal
        }

        // --- SECCIÓN 4: Log Final ---
        // 4.1. Imprimir log indicando la finalización de esta fase.
        System.out.println("  [Init Actions Internal] Finalizado.");

    } // --- FIN initializeActionsInternal ---    
    

    /**
     * Crea y devuelve el mapa que asocia comandos CORTOS (generalmente derivados
     * del nombre del icono/acción base) a las instancias de Action correspondientes,
     * EXCLUYENDO las actions de navegación (Next, Previous, First, Last).
     * Este mapa parcial se usa inicialmente para construir la UI (Toolbar, Menú)
     * antes de que el ListCoordinator esté disponible. Las actions de navegación
     * se añaden posteriormente en `createNavigationActionsAndUpdateMap`.
     *
     * Es llamado por AppInitializer durante la fase de inicialización base.
     *
     * @return Un Map<String, Action> que contiene las actions no relacionadas con la navegación.
     */
    /*package-private*/ Map<String, Action> createActionMapInternal() {
        // --- SECCIÓN 1: Creación e Inicialización del Mapa ---
        // 1.1. Crear una nueva instancia de HashMap para almacenar las actions.
        Map<String, Action> mapaParcial = new HashMap<>();
        // 1.2. Log indicando el inicio de la creación del mapa parcial.
        System.out.println("  [Create ActionMap Internal] Creando mapa parcial de actions (sin navegación)...");

        // --- SECCIÓN 2: Poblado del Mapa (Action por Action) ---
        // 2.1. Añadir Actions de Archivo/Control (usando comando CORTO como clave).
        //      Asegurarse de que las variables de Action (openAction, etc.) ya han sido inicializadas
        //      en `initializeActionsInternal`. Se añade una comprobación `!= null` por seguridad.
        if (openAction != null) {
             mapaParcial.put("Abrir_Archivo", openAction); // Clave de Menú
             mapaParcial.put("Selector_de_Carpetas_48x48", openAction); // Clave de Botón Toolbar
        }
        if (locateFileAction != null) {
             mapaParcial.put("Abrir_Ubicacion_del_Archivo", locateFileAction); // Clave de Menú
             mapaParcial.put("Ubicacion_del_Archivo_48x48", locateFileAction); // Clave de Botón Toolbar
        }
        if (refreshAction != null) { // Asumiendo que refreshAction existe
             mapaParcial.put("Refrescar_48x48", refreshAction); // Clave de Botón Toolbar
             mapaParcial.put("Recargar_Lista_de_Imagenes", refreshAction); // Clave de Menú
             // mapaParcial.put("Refrescar_Imagen", refreshAction); // ¿Otra clave de menú?
             // mapaParcial.put("Volver_a_Cargar", refreshAction); // ¿Otra clave de menú?
        }
         if (deleteAction != null) { // Asumiendo que deleteAction existe
             mapaParcial.put("Eliminar_Permanentemente", deleteAction); // Clave de Menú
             mapaParcial.put("Borrar_48x48", deleteAction); // Clave de Botón Toolbar
         }
        // ... añadir otras actions de archivo/control si existen ...

        // 2.2. Añadir Actions de Edición
        if (rotateLeftAction != null) {
             mapaParcial.put("Girar_Izquierda", rotateLeftAction);
             mapaParcial.put("Rotar_Izquierda_48x48", rotateLeftAction);
        }
        if (rotateRightAction != null) {
             mapaParcial.put("Girar_Derecha", rotateRightAction);
             mapaParcial.put("Rotar_Derecha_48x48", rotateRightAction);
        }
        if (flipHorizontalAction != null) {
             mapaParcial.put("Voltear_Horizontal", flipHorizontalAction);
             mapaParcial.put("Espejo_Horizontal_48x48", flipHorizontalAction);
        }
        if (flipVerticalAction != null) {
             mapaParcial.put("Voltear_Vertical", flipVerticalAction);
             mapaParcial.put("Espejo_Vertical_48x48", flipVerticalAction);
        }
        if (cortarAction != null) { // Asumiendo que cortarAction existe
             mapaParcial.put("Recortar_48x48", cortarAction);
             // mapaParcial.put("Recortar", cortarAction); // Si hay un menú item
        }

        // 2.3. Añadir Actions de Zoom
        if (toggleZoomManualAction != null) {
            mapaParcial.put("Activar_Zoom_Manual", toggleZoomManualAction);
            mapaParcial.put("Zoom_48x48", toggleZoomManualAction);
        }
        if (resetZoomAction != null) {
            mapaParcial.put("Resetear_Zoom", resetZoomAction);
            mapaParcial.put("Reset_48x48", resetZoomAction);
        }
        if (zoomAutoAction != null) {
             mapaParcial.put("Zoom_Automatico", zoomAutoAction);
             mapaParcial.put("Zoom_Auto_48x48", zoomAutoAction);
        }
        if (zoomAnchoAction != null) {
             mapaParcial.put("Zoom_a_lo_Ancho", zoomAnchoAction);
             mapaParcial.put("Ajustar_al_Ancho_48x48", zoomAnchoAction);
        }
        if (zoomAltoAction != null) {
             mapaParcial.put("Zoom_a_lo_Alto", zoomAltoAction);
             mapaParcial.put("Ajustar_al_Alto_48x48", zoomAltoAction);
        }
        if (zoomFitAction != null) {
             mapaParcial.put("Escalar_Para_Ajustar", zoomFitAction);
             mapaParcial.put("Escalar_Para_Ajustar_48x48", zoomFitAction);
        }
        if (zoomFixedAction != null) {
             mapaParcial.put("Zoom_Actual_Fijo", zoomFixedAction);
             mapaParcial.put("Zoom_Fijo_48x48", zoomFixedAction);
        }
        if (zoomFijadoAction != null) {
             mapaParcial.put("Zoom_Especificado", zoomFijadoAction);
             mapaParcial.put("Zoom_Especifico_48x48", zoomFijadoAction);
        }
        // ... otras actions de zoom ...

        // 2.4. Añadir Actions de Vista (Toggles Visibilidad)
        if (toggleMenuBarAction != null) mapaParcial.put("Barra_de_Menu", toggleMenuBarAction);
        if (toggleToolBarAction != null) mapaParcial.put("Barra_de_Botones", toggleToolBarAction);
        if (toggleFileListAction != null) mapaParcial.put("Mostrar/Ocultar_la_Lista_de_Archivos", toggleFileListAction);
        if (toggleThumbnailsAction != null) mapaParcial.put("Imagenes_en_Miniatura", toggleThumbnailsAction);
        if (toggleLocationBarAction != null) mapaParcial.put("Linea_de_Ubicacion_del_Archivo", toggleLocationBarAction);
        if (toggleCheckeredBgAction != null) mapaParcial.put("Fondo_a_Cuadros", toggleCheckeredBgAction);
        if (toggleAlwaysOnTopAction != null) mapaParcial.put("Mantener_Ventana_Siempre_Encima", toggleAlwaysOnTopAction);
        // ... otras actions de vista ...

        // 2.5. Añadir Actions de Tema
        if (temaClearAction != null) mapaParcial.put("Tema_Clear", temaClearAction);
        if (temaDarkAction != null) mapaParcial.put("Tema_Dark", temaDarkAction);
        if (temaBlueAction != null) mapaParcial.put("Tema_Blue", temaBlueAction);
        if (temaGreenAction != null) mapaParcial.put("Tema_Green", temaGreenAction);
        if (temaOrangeAction != null) mapaParcial.put("Tema_Orange", temaOrangeAction);

        // 2.6. Añadir Actions de Toggle Generales
        if (toggleSubfoldersAction != null) {
            // Puede que solo tenga botón, o también menú item
            mapaParcial.put("Subcarpetas_48x48", toggleSubfoldersAction);
            // mapaParcial.put("Mostrar_Imagenes_de_Subcarpetas", toggleSubfoldersAction); // Si hay menú item CheckBox
        }
        if (toggleProporcionesAction != null) {
             mapaParcial.put("Mantener_Proporciones", toggleProporcionesAction); // Menú
             mapaParcial.put("Mantener_Proporciones_48x48", toggleProporcionesAction); // Botón
        }

        // 2.7. Añadir Actions Misceláneas / Especiales
        if (menuAction != null) mapaParcial.put("Menu_48x48", menuAction);
        if (hiddenButtonsAction != null) mapaParcial.put("Botones_Ocultos_48x48", hiddenButtonsAction);
        // ... otras ...


        // --- SECCIÓN 3: Log Final y Retorno ---
        // 3.1. Imprimir log indicando la finalización y el tamaño del mapa creado.
        System.out.println("  [Create ActionMap Internal] Finalizado. Mapa parcial creado con " + mapaParcial.size() + " entradas.");
        // 3.2. Devolver el mapa parcial creado.
        return mapaParcial;

    } // --- FIN createActionMapInternal ---   
    

    /**
     * Crea las instancias de Action específicas para la navegación
     * (Siguiente, Anterior, Primero, Último), las cuales dependen
     * de la existencia del ListCoordinator.
     * Una vez creadas, las añade al mapa principal de acciones (`this.actionMap`)
     * que ya contenía las actions parciales.
     * Se llama desde AppInitializer (en el EDT) después de que ListCoordinator
     * ha sido creado.
     *
     * @param anchoIcono El ancho para los iconos de navegación.
     * @param altoIcono  El alto para los iconos de navegación.
     */
    /*package-private*/ void createNavigationActionsAndUpdateMap(int anchoIcono, int altoIcono) {
        // --- SECCIÓN 1: Log de Inicio y Validaciones ---
        // 1.1. Imprimir log indicando que se van a crear las actions de navegación.
        System.out.println("    [EDT Internal] Creando Actions de Navegación y actualizando actionMap...");
        // 1.2. Validar dependencias críticas: ListCoordinator e IconUtils deben existir.
        if (listCoordinator == null) {
            System.err.println("ERROR CRÍTICO [Create Nav Actions]: ListCoordinator es null. No se pueden crear actions de navegación.");
            // Podría ser un error fatal, considerar llamar a handleFatalError o lanzar excepción.
            return;
        }
        if (iconUtils == null) {
            System.err.println("ERROR CRÍTICO [Create Nav Actions]: IconUtils es null. No se pueden crear actions de navegación con iconos.");
            // Podría ser fatal si los iconos son obligatorios.
            return;
        }
        // 1.3. Validar que el mapa principal 'actionMap' exista (debería haber sido creado e inyectado antes).
        if (this.actionMap == null) {
             System.err.println("ERROR CRÍTICO [Create Nav Actions]: El mapa principal 'actionMap' es null. Creando uno nuevo, pero puede haber inconsistencias.");
             // Crear un mapa vacío como fallback para evitar NullPointerException, pero indicar el problema grave.
             this.actionMap = new HashMap<>();
        }

        // --- SECCIÓN 2: Creación de las Instancias de Action de Navegación ---
        // 2.1. Bloque try-catch para manejar posibles errores durante la instanciación.
        try {
            // 2.1.1. Crear Action para "Siguiente Imagen".
            //        Se pasa el listCoordinator (para ejecutar la lógica de navegación),
            //        iconUtils (para el icono) y las dimensiones.
            nextImageAction = new NextImageAction(listCoordinator, iconUtils, anchoIcono, altoIcono);

            // 2.1.2. Crear Action para "Imagen Anterior".
            previousImageAction = new PreviousImageAction(listCoordinator, iconUtils, anchoIcono, altoIcono);

            // 2.1.3. Crear Action para "Primera Imagen".
            firstImageAction = new FirstImageAction(listCoordinator, iconUtils, anchoIcono, altoIcono);

            // 2.1.4. Crear Action para "Última Imagen".
            lastImageAction = new LastImageAction(listCoordinator, iconUtils, anchoIcono, altoIcono);

            // 2.1.5. Log de éxito en la creación.
            System.out.println("      -> Instancias de Actions de Navegación creadas.");

        // 2.2. Capturar excepciones durante la creación.
        } catch (Exception e) {
             System.err.println("ERROR [EDT Internal] creando instancias de Actions de Navegación: " + e.getMessage());
             e.printStackTrace();
             // Considerar si la aplicación puede continuar sin estas actions o si es un error fatal.
             return; // Salir si no se pudieron crear las actions.
        }

        // --- SECCIÓN 3: Actualización del Mapa Principal de Actions ---
        // 3.1. Añadir las actions recién creadas al mapa `this.actionMap`.
        //      Es importante usar las MISMAS CLAVES CORTAS que esperan los builders
        //      (ToolbarBuilder y MenuBarBuilder) para poder encontrarlas.
        try {
            // 3.1.1. Añadir NextImageAction con sus claves (Menú y Botón).
            if (nextImageAction != null) {
                actionMap.put("Imagen_Siguiente", nextImageAction); // Clave usada por MenuBarBuilder
                actionMap.put("Siguiente_48x48", nextImageAction);   // Clave usada por ToolbarBuilder
            }
            // 3.1.2. Añadir PreviousImageAction.
            if (previousImageAction != null) {
                actionMap.put("Imagen_Aterior", previousImageAction); // Clave usada por MenuBarBuilder
                actionMap.put("Anterior_48x48", previousImageAction); // Clave usada por ToolbarBuilder
            }
            // 3.1.3. Añadir FirstImageAction.
            if (firstImageAction != null) {
                actionMap.put("Primera_Imagen", firstImageAction); // Clave usada por MenuBarBuilder
                actionMap.put("Primera_48x48", firstImageAction);  // Clave usada por ToolbarBuilder
            }
            // 3.1.4. Añadir LastImageAction.
            if (lastImageAction != null) {
                actionMap.put("Ultima_Imagen", lastImageAction); // Clave usada por MenuBarBuilder
                actionMap.put("Ultima_48x48", lastImageAction);  // Clave usada por ToolbarBuilder
            }
            // 3.1.5. Log confirmando la actualización del mapa y su tamaño final.
            System.out.println("      -> Actions de Navegación añadidas al mapa principal. Tamaño final actionMap: " + actionMap.size());
        // 3.2. Capturar posibles errores (ej. NullPointerException si actionMap era null y el fallback falló).
        } catch (Exception e) {
             System.err.println("ERROR [EDT Internal] añadiendo Actions de Navegación al mapa: " + e.getMessage());
             e.printStackTrace();
        }

        // --- SECCIÓN 4: Log Final del Método ---
        System.out.println("    [EDT Internal] Fin createNavigationActionsAndUpdateMap.");

    } // --- FIN createNavigationActionsAndUpdateMap ---
    
    
    /**
     * Asigna el modelo de datos de las miniaturas (`this.modeloMiniaturas`, que es
     * gestionado por el VisorController y actualizado por
     * `actualizarModeloYVistaMiniaturas`) a la JList correspondiente en la VisorView.
     *
     * Se llama desde AppInitializer (en el EDT) después de crear la Vista y
     * antes de cargar el estado inicial, para asegurar que la JList de miniaturas
     * tenga un modelo asignado desde el principio (aunque inicialmente esté vacío).
     *
     * También se podría llamar a este método si se necesitara cambiar
     * fundamentalmente el *tipo* de modelo usado por las miniaturas en tiempo de ejecución,
     * pero su uso principal aquí es durante la inicialización.
     */
    /*package-private*/ void assignModeloMiniaturasToViewInternal() {
        // --- SECCIÓN 1: Log de Inicio y Validaciones ---
        // 1.1. Imprimir log indicando la acción que se va a realizar.
        System.out.println("    [EDT Internal] Asignando modelo de miniaturas a la Vista...");
        // 1.2. Validar que la Vista exista.
        if (view == null) {
            System.err.println("ERROR [assignModeloMiniaturasToViewInternal]: Vista es null. No se puede asignar el modelo.");
            return; // Salir si no hay vista.
        }
        // 1.3. Validar que la JList de miniaturas dentro de la Vista exista.
        if (view.getListaMiniaturas() == null) {
             System.err.println("ERROR [assignModeloMiniaturasToViewInternal]: listaMiniaturas en Vista es null. No se puede asignar el modelo.");
             return; // Salir si el componente específico no existe.
        }
        // 1.4. Validar que el modelo de miniaturas del controlador (`this.modeloMiniaturas`) exista.
        //      AppInitializer debería haberlo creado e inyectado.
        if (this.modeloMiniaturas == null) {
             System.err.println("ERROR [assignModeloMiniaturasToViewInternal]: El modelo de miniaturas del controlador es null. Creando uno vacío como fallback.");
             // Crear un modelo vacío para evitar NullPointerException en setModeloListaMiniaturas,
             // aunque esto indica un problema en la inicialización previa.
             this.modeloMiniaturas = new DefaultListModel<>();
        }

        // --- SECCIÓN 2: Asignación del Modelo ---
        // 2.1. Llamar al método de la Vista (`setModeloListaMiniaturas`) para que
        //      la `JList` de miniaturas utilice el `DefaultListModel` que gestiona
        //      este controlador (`this.modeloMiniaturas`).
        //      Inicialmente, este modelo estará vacío. Se poblará dinámicamente
        //      por `actualizarModeloYVistaMiniaturas`.
        try {
            view.setModeloListaMiniaturas(this.modeloMiniaturas);
            // 2.2. Log de confirmación (el método setModeloListaMiniaturas ya tiene su propio log).
            // System.out.println("      -> Modelo de miniaturas asignado a JList en Vista.");
        } catch (Exception e) {
            // 2.3. Capturar cualquier excepción inesperada durante la asignación.
             System.err.println("ERROR [assignModeloMiniaturasToViewInternal]: Excepción al asignar modelo de miniaturas a la vista: " + e.getMessage());
             e.printStackTrace();
        }

        // --- SECCIÓN 3: Log Final ---
        // 3.1. Indicar que la asignación ha finalizado.
        System.out.println("    [EDT Internal] Fin assignModeloMiniaturasToViewInternal.");

    } // --- FIN assignModeloMiniaturasToViewInternal ---

    
    /**
     * Establece la variable de instancia `carpetaRaizActual` leyendo la ruta
     * guardada en la configuración (clave "inicio.carpeta").
     * Valida que la ruta leída desde la configuración sea un directorio válido
     * antes de asignarla a `carpetaRaizActual`.
     * Si la ruta de la configuración no es válida o no existe, `carpetaRaizActual`
     * puede quedar como null (indicando que no hay una carpeta raíz válida definida).
     *
     * Se llama desde AppInitializer (en el EDT, aunque podría llamarse antes si
     * no interactúa con UI) durante la inicialización.
     */
    /*package-private*/ void establecerCarpetaRaizDesdeConfigInternal() {
        // --- SECCIÓN 1: Log de Inicio y Validación de Dependencias ---
        // 1.1. Imprimir log indicando la acción.
        System.out.println("    [EDT Internal] Estableciendo carpeta raíz inicial desde config...");
        // 1.2. Validar que el gestor de configuración exista.
        if (configuration == null) {
            System.err.println("ERROR [establecerCarpetaRaizDesdeConfigInternal]: ConfigurationManager es null. No se puede leer la carpeta.");
            this.carpetaRaizActual = null; // Asegurar que sea null si no hay config.
            return; // Salir si falta la configuración.
        }

        // --- SECCIÓN 2: Lectura y Validación de la Ruta ---
        // 2.1. Obtener la cadena de la ruta desde la configuración.
        //      Usar la clave "inicio.carpeta" y "" como valor por defecto si no se encuentra.
        String folderInitPath = configuration.getString("inicio.carpeta", "");
        // 2.2. Inicializar el Path resultante a null.
        Path candidatePath = null;

        // 2.3. Comprobar si se obtuvo una ruta no vacía desde la configuración.
        if (!folderInitPath.isEmpty()) {
            // 2.3.1. Bloque try-catch para manejar errores al convertir la cadena a Path o al verificar el directorio.
            try {
                // 2.3.1.1. Intentar crear un objeto Path desde la cadena.
                candidatePath = Paths.get(folderInitPath);
                // 2.3.1.2. Verificar si el Path resultante es un directorio válido y existente.
                if (Files.isDirectory(candidatePath)) {
                    // 2.3.1.2.1. Si es válido, asignar este Path a la variable de instancia `carpetaRaizActual`.
                    this.carpetaRaizActual = candidatePath;
                    System.out.println("      -> Carpeta raíz establecida a: " + this.carpetaRaizActual);
                } else {
                    // 2.3.1.2.2. Si la ruta existe pero no es un directorio, loguear advertencia y poner `carpetaRaizActual` a null.
                    System.err.println("WARN [establecerCarpetaRaizDesdeConfigInternal]: La ruta en config no es un directorio: " + folderInitPath);
                    this.carpetaRaizActual = null;
                }
            // 2.3.2. Capturar excepciones (p.ej., formato de ruta inválido).
            } catch (Exception e) {
                // 2.3.2.1. Loguear el error y poner `carpetaRaizActual` a null.
                System.err.println("WARN [establecerCarpetaRaizDesdeConfigInternal]: Ruta de carpeta inicial inválida en config: '" + folderInitPath + "' - Error: " + e.getMessage());
                this.carpetaRaizActual = null;
            }
        // 2.4. Si la ruta en la configuración estaba vacía.
        } else {
            // 2.4.1. Log indicando que no había ruta definida.
            System.out.println("      -> No hay ruta de inicio definida en la configuración.");
            // 2.4.2. Asegurar que `carpetaRaizActual` sea null.
            this.carpetaRaizActual = null;
        }

        // --- SECCIÓN 3: Log Final ---
        // 3.1. Indicar si se estableció una carpeta raíz o no.
        if (this.carpetaRaizActual != null) {
            System.out.println("    [EDT Internal] Fin establecerCarpetaRaizDesdeConfigInternal. Raíz actual: " + this.carpetaRaizActual);
        } else {
             System.out.println("    [EDT Internal] Fin establecerCarpetaRaizDesdeConfigInternal. No se estableció carpeta raíz válida.");
        }

    } // --- FIN establecerCarpetaRaizDesdeConfigInternal ---

    
    /**
     * Método helper para aplicar la configuración inicial SOLO a los componentes
     * de la VISTA. Se llama DESPUÉS de crear la Vista, dentro del invokeLater
     * del AppInitializer.
     * Configura el estado inicial de visibilidad y habilitación de botones y menús,
     * y sincroniza el estado visual de controles específicos (como los de Zoom).
     */
    /*package-private*/ void aplicarConfigAlaVistaInternal() {
        // --- SECCIÓN 1: Log de Inicio y Validaciones ---
        // 1.1. Indicar el inicio de la aplicación de configuración a la vista.
        System.out.println("  [Apply Config View Internal] Aplicando configuración a la Vista...");
        // 1.2. Validar dependencias críticas: configuration y view deben existir.
        //      Model también se incluye por si alguna lógica futura lo necesita aquí.
        if (configuration == null || view == null || model == null) {
             System.err.println("ERROR [aplicarConfigAlaVistaInternal]: Configuración, Vista o Modelo nulos. Abortando.");
             return; // No se puede continuar sin estos componentes.
        }

        // --- SECCIÓN 2: Aplicar Configuración a Botones (Visibilidad y Estado Enabled) ---
        // 2.1. Obtener el mapa de botones desde la vista.
        Map<String, JButton> botones = view.getBotonesPorNombre();
        // 2.2. Comprobar si el mapa de botones existe.
        if (botones != null) {
            System.out.println("    -> Aplicando config a Botones (Vista)...");
            // 2.3. Iterar sobre cada entrada del mapa (clave larga -> botón).
            botones.forEach((claveCompletaBoton, button) -> {
                // 2.4. Bloque try-catch para manejar errores al leer config para un botón específico.
                try {
                    // 2.4.1. Leer el estado 'activado' (enabled) desde la configuración.
                    //        Usa la clave larga del botón + ".activado". Proporciona 'true' como valor por defecto.
                    boolean activado = configuration.getBoolean(claveCompletaBoton + ".activado", true);
                    // 2.4.2. Establecer el estado 'enabled' del botón.
                    button.setEnabled(activado);

                    // 2.4.3. Leer el estado 'visible' desde la configuración.
                    //        Usa la clave larga del botón + ".visible". Proporciona 'true' como valor por defecto.
                    boolean visible = configuration.getBoolean(claveCompletaBoton + ".visible", true);
                    // 2.4.4. Establecer la visibilidad del botón.
                    button.setVisible(visible);
                    // 2.4.5. Log opcional para depuración.
                    // System.out.println("      -> Botón '" + claveCompletaBoton + "': enabled=" + activado + ", visible=" + visible);

                // 2.5. Capturar cualquier excepción durante la lectura/aplicación de config para este botón.
                } catch (Exception e) {
                    System.err.println("ERROR aplicando config a Botón (Vista) '" + claveCompletaBoton + "': " + e.getMessage());
                    // Continuar con el siguiente botón si uno falla.
                }
            });
            // 2.6. Log indicando fin de configuración de botones.
            System.out.println("    -> Config Botones (Vista) OK.");
        // 2.7. Log de advertencia si el mapa de botones no se encontró en la vista.
        } else {
            System.err.println("WARN [aplicarConfigAlaVistaInternal]: Mapa de botones ('botonesPorNombre') nulo en la Vista.");
        }

        // --- SECCIÓN 3: Aplicar Configuración a Menús (Visibilidad, Enabled y Selección sin Action) ---
        // 3.1. Obtener el mapa de items de menú desde la vista.
        Map<String, JMenuItem> menuItems = view.getMenuItemsPorNombre();
        // 3.2. Comprobar si el mapa de menús existe.
	    if (menuItems != null) {
	        System.out.println("    -> Aplicando config a Menús (Vista)...");
	        // 3.3. Iterar sobre cada entrada del mapa (clave larga -> item de menú).
	        menuItems.forEach((claveCompletaMenu, menuItem) -> {
	             // 3.4. Bloque try-catch para manejar errores para un item específico.
	             try {
	                 // 3.4.1. Leer y aplicar el estado 'activado' (enabled). Default: true.
	                 menuItem.setEnabled(configuration.getBoolean(claveCompletaMenu + ".activado", true));
	                 // 3.4.2. Leer y aplicar el estado 'visible'. Default: true.
	                 menuItem.setVisible(configuration.getBoolean(claveCompletaMenu + ".visible", true));

	                 // 3.4.3. Aplicar estado 'seleccionado' SOLO si es un componente seleccionable
	                 //        (CheckBox o RadioButton) Y si NO tiene una Action asociada
	                 //        (porque la Action ya maneja su propio estado 'SELECTED_KEY').
	                 boolean esSeleccionable = (menuItem instanceof JCheckBoxMenuItem || menuItem instanceof JRadioButtonMenuItem);
	                 boolean tieneAction = (menuItem.getAction() != null);

	                 // 3.4.4. Si es seleccionable y no tiene Action, leer y aplicar estado.
	                 if (esSeleccionable && !tieneAction) {
	                      // Leer el estado 'seleccionado' de la configuración. Default: false.
	                      boolean seleccionado = configuration.getBoolean(claveCompletaMenu + ".seleccionado", false);
	                      // Aplicar el estado usando el método específico del componente.
	                      if (menuItem instanceof JCheckBoxMenuItem) {
	                          ((JCheckBoxMenuItem) menuItem).setSelected(seleccionado);
	                      } else if (menuItem instanceof JRadioButtonMenuItem) {
	                          ((JRadioButtonMenuItem) menuItem).setSelected(seleccionado);
	                      }
                         // Log opcional
                         // System.out.println("      -> Menú '" + claveCompletaMenu + "' (sin Action): seleccionado=" + seleccionado);
	                 }
                     // Log opcional general para el item
                     // System.out.println("      -> Menú '" + claveCompletaMenu + "': enabled=" + menuItem.isEnabled() + ", visible=" + menuItem.isVisible());

	             // 3.5. Capturar cualquier excepción durante la lectura/aplicación de config.
	             } catch (Exception e) {
	                 System.err.println("ERROR aplicando config a Menú (Vista) '" + claveCompletaMenu + "': " + e.getMessage());
	                 // Continuar con el siguiente item si uno falla.
	             }
	        });
	        // 3.6. Log indicando fin de configuración de menús.
	        System.out.println("    -> Config Menús (Vista) OK.");
	    // 3.7. Log de advertencia si el mapa de menús no se encontró.
	    } else {
	        System.err.println("WARN [aplicarConfigAlaVistaInternal]: Mapa de menús ('menuItemsPorNombre') nulo en la Vista.");
        }

        // --- SECCIÓN 4: Sincronizar Estados Visuales Específicos (dependientes de Actions) ---
        // 4.1. Iniciar bloque try-catch para esta sección.
        try {
            // 4.1.1. Sincronizar controles de Zoom Manual y Reset.
            //        Obtener el estado lógico actual de la Action 'toggleZoomManualAction'.
            boolean zoomManualInicial = (toggleZoomManualAction != null) && Boolean.TRUE.equals(toggleZoomManualAction.getValue(Action.SELECTED_KEY));
            // Llamar al método de la vista para actualizar la apariencia de los botones/menús de zoom.
            view.actualizarEstadoControlesZoom(zoomManualInicial, zoomManualInicial); // El segundo parámetro habilita Reset si Zoom está activo.

            // 4.1.2. Sincronizar estado visual del botón de Mantener Proporciones.
            //        Obtener estado lógico de la Action.
            boolean proporcionesInicial = (toggleProporcionesAction != null) && Boolean.TRUE.equals(toggleProporcionesAction.getValue(Action.SELECTED_KEY));
            // Llamar al helper para actualizar la apariencia del botón asociado.
            actualizarAspectoBotonToggle(toggleProporcionesAction, proporcionesInicial);

            // 4.1.3. Sincronizar estado visual del botón y radios de Cargar Subcarpetas.
            //        Obtener estado lógico de la Action.
            boolean subcarpetasInicial = (toggleSubfoldersAction != null) && Boolean.TRUE.equals(toggleSubfoldersAction.getValue(Action.SELECTED_KEY));
            // Actualizar apariencia del botón asociado.
            actualizarAspectoBotonToggle(toggleSubfoldersAction, subcarpetasInicial);
            // Asegurar que los RadioButtons del menú reflejen el estado lógico.
            restaurarSeleccionRadiosSubcarpetas(subcarpetasInicial);

            // 4.1.4. Sincronizar estado visual de otros toggles si fuera necesario.
            //        Por ejemplo, si los JCheckBoxMenuItems para visibilidad de barras
            //        NO usaran Actions (aunque sí las usan), tendrías que llamar a
            //        view.setJMenuBarVisible(), view.setToolBarVisible(), etc., aquí,
            //        leyendo el estado desde `configuration` o desde la `Action` correspondiente.
            //        Como usan Actions, su estado visual se maneja automáticamente.

            // 4.1.5. Log confirmando la sincronización visual.
            System.out.println("    -> Estados iniciales específicos (Zoom, Prop, Sub) aplicados visualmente a UI.");

        // 4.2. Capturar excepciones durante la sincronización visual.
        } catch(Exception e) {
            System.err.println("ERROR aplicando estados visuales específicos a la Vista: " + e.getMessage());
            e.printStackTrace();
        }

        // --- SECCIÓN 5: Log Final ---
        // 5.1. Indicar que la configuración visual ha terminado.
        System.out.println("  [Apply Config View Internal] Finalizado.");

    } // --- FIN aplicarConfigAlaVistaInternal ---
    
    
    /**
     * Carga la carpeta y la imagen iniciales definidas en la configuración.
     * Si no hay configuración válida, limpia la interfaz.
     * Se llama desde AppInitializer (en el EDT) después de aplicar la config inicial a la vista.
     * Llama a `cargarListaImagenes` para iniciar la carga en segundo plano.
     */
    /*package-private*/ void cargarEstadoInicialInternal() {
        // --- SECCIÓN 1: Log de Inicio y Verificación de Dependencias ---
        // 1.1. Imprimir log indicando el inicio de la carga del estado.
        System.out.println("  [Load Initial State Internal] Cargando estado inicial (carpeta/imagen)...");
        // 1.2. Verificar que las dependencias necesarias (configuration, model, view) existan.
        //      Son necesarias para determinar qué cargar y para limpiar la UI si falla.
        if (configuration == null || model == null || view == null) {
            System.err.println("ERROR [cargarEstadoInicialInternal]: Config, Modelo o Vista nulos. No se puede cargar estado.");
            // 1.2.1. Intentar limpiar la UI si faltan componentes esenciales.
            limpiarUI(); // Llama al método de limpieza general.
            return; // Salir del método.
        }

        // --- SECCIÓN 2: Determinar y Validar la Carpeta Inicial ---
        // 2.1. Obtener la ruta de la carpeta inicial desde ConfigurationManager.
        //      Se usa "" como valor por defecto si la clave "inicio.carpeta" no existe.
        String folderInit = configuration.getString("inicio.carpeta", "");
        // 2.2. Variable para almacenar el Path de la carpeta validada.
        Path folderPath = null;
        // 2.3. Flag para indicar si la carpeta encontrada es válida.
        boolean carpetaValida = false;

        // 2.4. Comprobar si la ruta obtenida no está vacía.
        if (!folderInit.isEmpty()) {
            // 2.4.1. Intentar convertir la cadena de ruta en un objeto Path.
            try {
                folderPath = Paths.get(folderInit);
                // 2.4.2. Verificar si el Path resultante es realmente un directorio existente.
                if (Files.isDirectory(folderPath)) {
                    // 2.4.2.1. Si es un directorio válido, marcar como válida y actualizar
                    //          la variable de instancia `carpetaRaizActual` del controlador.
                    carpetaValida = true;
                    this.carpetaRaizActual = folderPath; // Establecer como la raíz para futuras operaciones.
                    System.out.println("    -> Carpeta inicial válida encontrada: " + folderPath);
                } else {
                    // 2.4.2.2. Log si la ruta existe pero no es un directorio.
                     System.err.println("WARN [cargarEstadoInicialInternal]: Carpeta inicial en config no es un directorio válido: " + folderInit);
                     this.carpetaRaizActual = null; // Asegurar que no quede una ruta inválida.
                }
            // 2.4.3. Capturar cualquier excepción durante la conversión/verificación de la ruta.
            } catch (Exception e) {
                System.err.println("WARN [cargarEstadoInicialInternal]: Ruta de carpeta inicial inválida en config: " + folderInit + " - " + e.getMessage());
                this.carpetaRaizActual = null; // Asegurar que no quede una ruta inválida.
            }
        } else {
            // 2.5. Log si la clave "inicio.carpeta" no estaba definida en la configuración.
            System.out.println("    -> No hay definida una carpeta inicial en la configuración.");
            this.carpetaRaizActual = null; // Asegurar que no quede una ruta inválida.
        }

        // --- SECCIÓN 3: Cargar Lista de Imágenes o Limpiar UI ---
        // 3.1. Proceder a cargar la lista SOLO si se encontró una carpeta inicial válida.
        if (carpetaValida && this.carpetaRaizActual != null) {
            // 3.1.1. Log indicando que se procederá a la carga.
            System.out.println("    -> Cargando lista para carpeta inicial: " + this.carpetaRaizActual);
            // 3.1.2. Obtener la clave de la imagen inicial desde la configuración.
            //        Puede ser null si no hay una imagen específica guardada.
            String imagenInicialKey = configuration.getString("inicio.imagen", null);
            System.out.println("    -> Clave de imagen inicial a intentar seleccionar: " + imagenInicialKey);

            // 3.1.3. Llamar al método `cargarListaImagenes`. Este método se encargará de:
            //        - Ejecutar la búsqueda de archivos en segundo plano (SwingWorker).
            //        - Mostrar un diálogo de progreso.
            //        - Actualizar el modelo y la vista cuando termine.
            //        - Seleccionar la `imagenInicialKey` si se proporciona y se encuentra,
            //          o seleccionar el primer elemento (índice 0) si no.
            cargarListaImagenes(imagenInicialKey);

        // 3.2. Si NO se encontró una carpeta inicial válida.
        } else {
            // 3.2.1. Log indicando que no se cargará nada y se limpiará la UI.
            System.out.println("    -> No hay carpeta inicial válida configurada o accesible. Limpiando UI.");
            // 3.2.2. Llamar al método que resetea el modelo y la vista a un estado vacío.
            limpiarUI();
        }

        // --- SECCIÓN 4: Log Final ---
        // 4.1. Indicar que el proceso de carga del estado inicial ha finalizado (o se ha iniciado la carga en background).
        System.out.println("  [Load Initial State Internal] Finalizado.");

    } // --- FIN cargarEstadoInicialInternal ---
    

    /**
     * Configura los listeners principales de la vista (Selección de listas,
     * rueda de ratón para zoom/pan, scroll de miniaturas) y otros eventos UI.
     * Se llama desde AppInitializer (en el EDT) después de crear la Vista y el ListCoordinator.
     * Asigna los listeners adecuados a los componentes correspondientes de la VisorView.
     */
    /*package-private*/ void configurarListenersVistaInternal() {
    	
        // --- SECCIÓN 0: Validación Inicial y Log ---
        // 0.1. Validar que las dependencias críticas (Vista y ListCoordinator) existan.
        if (view == null || listCoordinator == null) {
            System.err.println("WARN [configurarListenersVistaInternal]: Vista o ListCoordinator nulos. Abortando configuración de listeners.");
            return; // Salir si faltan componentes esenciales.
        }
        
        // 0.2. Log indicando el inicio de la configuración y el modo de selección (separados).
        System.out.println("[Controller Internal] Configurando Listeners (Modelos Selección SEPARADOS)...");

        // --- SECCIÓN 1: Listeners de Selección de Listas (Separados) ---
        //    Ahora cada JList tiene su propio ListSelectionModel y su propio listener.

        // 1.1. Configurar Listener para listaNombres.
        // 1.1.1. Obtener la referencia a la JList de nombres desde la vista.
        JList<String> listaNombres = view.getListaNombres();
        
        // 1.1.2. Comprobar si la lista existe.
        if (listaNombres != null) {
        	
            // 1.1.3. Limpiar listeners de selección previos (defensivo, evita duplicados si se llama varias veces).
            for (javax.swing.event.ListSelectionListener lsl : listaNombres.getListSelectionListeners()) {
                // Eliminar lambdas o listeners que pertenezcan a esta clase de controlador.
                if (lsl.getClass().getName().contains("$Lambda") || lsl.getClass().getName().contains(this.getClass().getSimpleName())) {
                    listaNombres.removeListSelectionListener(lsl);
                }
            }
            
            // 1.1.4. Añadir el nuevo ListSelectionListener usando una expresión lambda.
            listaNombres.addListSelectionListener(e -> { // Inicio lambda listener Nombres
            	
                // 1.1.4.1. Determinar si el evento debe ser ignorado.
                boolean isIgnored = e.getValueIsAdjusting() ||    // Ignorar eventos intermedios durante el arrastre del ratón.
                                    seleccionInicialEnCurso ||    // Ignorar si la selección inicial está en curso.
                                    (listCoordinator != null && listCoordinator.isSincronizandoUI()); // Ignorar si el Coordinator está actualizando programáticamente.

                // 1.1.4.2. Obtener el índice seleccionado directamente de esta JList.
                //          Este índice corresponde al modelo principal de datos.
                int indicePrincipal = listaNombres.getSelectedIndex();

                // 1.1.4.3. Log detallado del evento (útil para depuración).
                System.out.println(">>> LISTENER NOMBRES: Evento. isAdjusting=" + e.getValueIsAdjusting() +
                                   ", isInicial=" + seleccionInicialEnCurso +
                                   ", isSincronizando=" + (listCoordinator != null && listCoordinator.isSincronizandoUI()) +
                                   ", IndicePrincipal=" + indicePrincipal +
                                   ", Ignorado=" + isIgnored);

                // 1.1.4.4. Procesar el evento solo si NO debe ser ignorado.
                if (!isIgnored) {
                	
                    // 1.1.4.4.1. Log indicando que se procesa.
                    System.out.println(">>> LISTENER NOMBRES: Procesando -> Llamando a Coordinator.seleccionarDesdeNombres(" + indicePrincipal + ")");
                   
                    // 1.1.4.4.2. Bloque try-catch para manejar excepciones inesperadas.
                    try {
                      
                    	// 1.1.4.4.3. Validar que el ListCoordinator exista.
                        if (listCoordinator != null) {
                        
                        	// 1.1.4.4.4. Llamar al método del ListCoordinator específico para selecciones desde la lista de nombres.
                             listCoordinator.seleccionarDesdeNombres(indicePrincipal);
                        } else {
                            // Error crítico si el coordinador no existe.
                            System.err.println("ERROR CRÍTICO: ListCoordinator es null en listener Nombres");
                        }
               
                    // 1.1.4.4.5. Capturar y loguear excepciones.
                    } catch (Exception ex) {
                         System.err.println("### EXCEPCIÓN LISTENER NOMBRES (Índice: " + indicePrincipal + ") ###");
                         ex.printStackTrace();
                    }
                }
            }); // Fin lambda listener Nombres
            
            // 1.1.5. Log confirmando que se añadió el listener.
            System.out.println("  -> Listener SEPARADO añadido a listaNombres.");
            
        // 1.1.6. Log de advertencia si la lista de nombres no existe.
        } else {
            System.err.println("WARN [configurarListenersVistaInternal]: listaNombres es null.");
        }

        // 1.2. Configurar Listener para listaMiniaturas.
        
        // 1.2.1. Obtener la referencia a la JList de miniaturas.
        JList<String> listaMiniaturas = view.getListaMiniaturas();
        
        // 1.2.2. Comprobar si la lista existe.
        if (listaMiniaturas != null) {
        
        	// 1.2.3. Limpiar listeners de selección previos.
            for (javax.swing.event.ListSelectionListener lsl : listaMiniaturas.getListSelectionListeners()) {
                if (lsl.getClass().getName().contains("$Lambda") || lsl.getClass().getName().contains(this.getClass().getSimpleName())) {
                    listaMiniaturas.removeListSelectionListener(lsl);
                }
            }
         
            // 1.2.4. Añadir el nuevo ListSelectionListener usando una expresión lambda.
            listaMiniaturas.addListSelectionListener(e -> { // Inicio lambda listener Miniaturas
      
            	// 1.2.4.1. Determinar si el evento debe ser ignorado.
                 boolean isIgnored = e.getValueIsAdjusting() ||
                                     seleccionInicialEnCurso ||
                                     (listCoordinator != null && listCoordinator.isSincronizandoUI());
        
                 // 1.2.4.2. Obtener el índice seleccionado RELATIVO al modelo actual de esta lista (el modelo 7+1+7).
                 int indiceRelativo = listaMiniaturas.getSelectedIndex();

                 // 1.2.4.3. Log detallado del evento.
                 System.out.println(">>> LISTENER MINIATURAS: Evento. isAdjusting=" + e.getValueIsAdjusting() +
                                    ", isInicial=" + seleccionInicialEnCurso +
                                    ", isSincronizando=" + (listCoordinator != null && listCoordinator.isSincronizandoUI()) +
                                    ", IndiceRelativo=" + indiceRelativo +
                                    ", Ignorado=" + isIgnored);

                 // 1.2.4.4. Procesar solo si no se ignora.
                 if (!isIgnored) {
                    System.out.println(">>> LISTENER MINIATURAS: Procesando...");
                    // Variable para almacenar el índice principal después de la traducción.
                    int indicePrincipalTraducido = -1;
                 
                    // 1.2.4.4.1. Bloque try-catch para manejar excepciones.
                    try {
                    
                    	// 1.2.4.4.2. Si se seleccionó un ítem válido en miniaturas (índice >= 0).
                        if (indiceRelativo != -1) {
                        
                        	// TRADUCIR índice relativo a índice principal.
                            // 1.2.4.4.2.1. Obtener el modelo de datos actual de la lista de miniaturas.
                            ListModel<String> modeloMinActual = listaMiniaturas.getModel();
                            
                            // 1.2.4.4.2.2. Validar que el modelo exista y el índice relativo sea válido.
                             if (modeloMinActual != null && indiceRelativo < modeloMinActual.getSize()) {
                                 // Obtener la clave (ruta relativa) del ítem seleccionado en miniaturas.
                                 String claveSeleccionada = modeloMinActual.getElementAt(indiceRelativo);

                                 // Validar que la clave y los modelos principales existan.
                                 if (claveSeleccionada != null && model != null && model.getModeloLista() != null) {

                                	 // Buscar la clave en el modelo PRINCIPAL para obtener el índice principal.
                                     indicePrincipalTraducido = model.getModeloLista().indexOf(claveSeleccionada);
                                     
                                     // Verificar si se encontró.
                                     if (indicePrincipalTraducido != -1) {
                                     
                                    	 // Log de éxito en la traducción.
                                         System.out.println("    -> Traducción OK: Índice Relativo " + indiceRelativo + " -> Clave '" + claveSeleccionada + "' -> Índice Principal " + indicePrincipalTraducido);
                                         
                                     } else {
                                         // Error crítico si la clave no está en el modelo principal (inconsistencia).
                                         System.err.println("ERROR CRÍTICO: Clave '" + claveSeleccionada + "' de miniatura no encontrada en modelo principal!");
                                         
                                         // Intentar limpiar la selección como medida de recuperación.
                                         if(listCoordinator != null) listCoordinator.seleccionarDesdeMiniaturas(-1);
                                         return; // Salir del listener si hay error grave.
                                     }
                                 } else {
                                	 
                                     // Error si faltan datos para la traducción.
                                     System.err.println("ERROR CRÍTICO: Clave o modelos nulos durante traducción de índice.");
                                     if(listCoordinator != null) listCoordinator.seleccionarDesdeMiniaturas(-1);
                                     return;
                                 }
                             } else {
                            	 
                                 // Error si no se puede acceder al modelo de miniaturas o el índice es inválido.
                                 System.err.println("WARN: No se pudo obtener clave del modelo de miniaturas o índice relativo fuera de rango: " + indiceRelativo);
                                 if(listCoordinator != null) listCoordinator.seleccionarDesdeMiniaturas(-1);
                                 return;
                             }
                             
                        // 1.2.4.4.3. Si se deseleccionó en miniaturas (indiceRelativo == -1).
                        } else {
                             
                        	// Establecer el índice principal a -1 para indicar deselección.
                             indicePrincipalTraducido = -1;
                             System.out.println("    -> Deselección detectada en Miniaturas.");
                        }

                         // 1.2.4.4.4. Llamar al ListCoordinator con el ÍNDICE PRINCIPAL TRADUCIDO (o -1).
                         System.out.println(">>> LISTENER MINIATURAS: Procesando -> Llamando a Coordinator.seleccionarDesdeMiniaturas(" + indicePrincipalTraducido + ")");

                         // Validar que el coordinador exista.
                         if (listCoordinator != null) {
                         
                        	  // Llamar al método específico del coordinador.
                              listCoordinator.seleccionarDesdeMiniaturas(indicePrincipalTraducido);
                              
                         } else {
                             
                        	 // Error crítico si falta el coordinador.
                             System.err.println("ERROR CRÍTICO: ListCoordinator es null en listener Miniaturas");
                         }

                    // 1.2.4.4.5. Capturar y loguear excepciones inesperadas.
                    } catch (Exception ex) {
                         System.err.println("### EXCEPCIÓN LISTENER MINIATURAS (Índice Relativo: " + indiceRelativo + ") ###");
                         ex.printStackTrace();

                         // Intentar limpiar la selección como medida de recuperación.
                         if(listCoordinator != null) listCoordinator.seleccionarDesdeMiniaturas(-1);
                    }
                 } // Fin if (!isIgnored)
            }); // Fin lambda listener Miniaturas
            
            // 1.2.5. Log confirmando que se añadió el listener.
            System.out.println("  -> Listener SEPARADO añadido a listaMiniaturas.");
            
        // 1.2.6. Log de advertencia si la lista de miniaturas no existe.
        } else {
             System.err.println("WARN [configurarListenersVistaInternal]: listaMiniaturas es null.");
        }


        // --- SECCIÓN 2: Listeners de Ratón para Imagen Principal (Zoom/Pan) ---
        // 2.1. Obtener la referencia al JLabel de la imagen principal.
        JLabel etiquetaImg = view.getEtiquetaImagen();

        // 2.2. Comprobar si la etiqueta existe.
        if (etiquetaImg != null) {
        
        	// 2.2.1. Añadir listener para la Rueda del Ratón (Zoom).
            etiquetaImg.addMouseWheelListener(e -> {
            
            	 // Comprobar si el modelo existe y el zoom manual está habilitado.
                 if (model != null && model.isZoomHabilitado()) {
                      int notches = e.getWheelRotation(); // Negativo: arriba/acercar, Positivo: abajo/alejar
                      double zoomIncrement = 0.1;
                      double currentZoom = model.getZoomFactor();
                      double newZoom = currentZoom + (notches < 0 ? zoomIncrement : -zoomIncrement);
                      newZoom = Math.max(0.1, Math.min(newZoom, 10.0)); // Limitar zoom entre 10% y 1000%

                      // Actualizar solo si el zoom cambió efectivamente.
                      if (newZoom != currentZoom) {
                           model.setZoomFactor(newZoom); // Actualizar modelo
                           Image reesc = reescalarImagenParaAjustar(); // Recalcular imagen escalada base
                           if (view != null && reesc != null) { // Actualizar vista
                                view.setImagenMostrada(reesc, model.getZoomFactor(), model.getImageOffsetX(), model.getImageOffsetY());
                           }
                      }
                 }
            });
            
            // 2.2.2. Añadir listener para Presionar Botón del Ratón (Inicio Paneo).
            etiquetaImg.addMouseListener(new java.awt.event.MouseAdapter() {
                 @Override public void mousePressed(java.awt.event.MouseEvent e) {
                     // Solo actuar con botón izquierdo y si el zoom manual está activo.
                     if (model!=null && model.isZoomHabilitado() && SwingUtilities.isLeftMouseButton(e)) {
                         lastMouseX = e.getX(); // Guardar coordenadas iniciales del clic.
                         lastMouseY = e.getY();
                     }
                 }
            });
            
            // 2.2.3. Añadir listener para Arrastrar Ratón (Realizar Paneo).
            etiquetaImg.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                 @Override public void mouseDragged(java.awt.event.MouseEvent e) {
            
                	 // Solo actuar con botón izquierdo y si el zoom manual está activo.
                     if (model!=null && model.isZoomHabilitado() && SwingUtilities.isLeftMouseButton(e)) {
                         int dX = e.getX() - lastMouseX; // Calcular desplazamiento horizontal.
                         int dY = e.getY() - lastMouseY; // Calcular desplazamiento vertical.
                         model.addImageOffsetX(dX);      // Actualizar offset X en el modelo.
                         model.addImageOffsetY(dY);      // Actualizar offset Y en el modelo.
                         lastMouseX = e.getX();          // Actualizar última posición del ratón.
                         lastMouseY = e.getY();
                         Image reesc = reescalarImagenParaAjustar(); // Recalcular imagen base.
                         if (view!=null && reesc != null) { // Actualizar vista con nuevo offset.
                             view.setImagenMostrada(reesc, model.getZoomFactor(), model.getImageOffsetX(), model.getImageOffsetY());
                         }
                     }
                 }
            });
            
            // 2.2.4. Log confirmando adición de listeners de ratón.
            System.out.println("  -> Listeners de Zoom/Pan añadidos a etiquetaImagen.");

        // 2.3. Log de advertencia si la etiqueta de imagen no existe.
        } else {
            System.err.println("WARN [configurarListenersVistaInternal]: etiquetaImagen es null.");
        }


        // --- SECCIÓN 3: Listener de Rueda para Scroll Horizontal de Miniaturas ---
        // 3.1. Obtener referencia al JScrollPane de miniaturas.
        JScrollPane scrollMiniaturas = view.getScrollListaMiniaturas();

        // 3.2. Comprobar si el scrollpane existe.
        if (scrollMiniaturas != null) {
        
        	// 3.2.1. Limpiar listeners previos de rueda (defensivo).
             for(java.awt.event.MouseWheelListener mwl : scrollMiniaturas.getMouseWheelListeners()){
                 if(mwl.getClass().getName().contains(this.getClass().getSimpleName()) || mwl.getClass().getName().contains("$Lambda")) {
                     scrollMiniaturas.removeMouseWheelListener(mwl);
                 }
             }
             
             // 3.2.2. Añadir listener para la rueda del ratón.
            scrollMiniaturas.addMouseWheelListener(e -> {
                
            	// 3.2.2.1. Obtener la barra de scroll horizontal.
                javax.swing.JScrollBar hScrollBar = scrollMiniaturas.getHorizontalScrollBar();
                
                // 3.2.2.2. Actuar solo si la barra horizontal existe y es visible (o si Shift está presionado - opcional).
                // if (e.isShiftDown() || (hScrollBar != null && hScrollBar.isVisible())) { // Alternativa con Shift
                if (hScrollBar != null && hScrollBar.isVisible()) {
                     
                	 // 3.2.2.2.1. Calcular la cantidad de scroll (ajustar el multiplicador para sensibilidad).
                     int amount = (int)(e.getPreciseWheelRotation() * hScrollBar.getUnitIncrement() * 5); // 5 es un multiplicador de ejemplo
                     
                     // 3.2.2.2.2. Establecer el nuevo valor de la barra de scroll.
                     hScrollBar.setValue(hScrollBar.getValue() + amount);
                     
                     // 3.2.2.2.3. Consumir el evento para evitar que el contenedor padre también haga scroll.
                     e.consume();
                }
                
                // 3.2.2.3. (Opcional) Si no se consumió (ej. no había barra H), pasar el evento al padre (para scroll vertical de ventana).
                // else if (!e.isConsumed()) {
                //    Component parent = scrollMiniaturas.getParent();
                //    if (parent != null) {
                //        parent.dispatchEvent(SwingUtilities.convertMouseEvent(scrollMiniaturas, e, parent));
                //    }
                // }
            });
             
             // 3.2.3. Log confirmando adición del listener.
             System.out.println("  -> MouseWheelListener añadido a scrollListaMiniaturas para scroll horizontal.");

        // 3.3. Log de advertencia si el scrollpane no existe.
        } else {
            System.err.println("WARN [configurarListenersVistaInternal]: scrollListaMiniaturas es null.");
        }
        
        // --- SECCIÓN NUEVA: Gestionar Foco en Miniaturas ---
        // REUTILIZA la variable scrollMiniaturas y listaMiniaturas declaradas ANTES

        JScrollPane scrollMiniaturasExistente = view.getScrollListaMiniaturas(); // Usa la variable existente si es necesario (o la que ya tenías)
        JList<String> listaMinRefExistente = view.getListaMiniaturas(); // Usa la variable existente

        if (scrollMiniaturasExistente != null && listaMinRefExistente != null) {
            // Asegúrate de no añadir el listener múltiples veces si este método se llama más de una vez
            // (Opcional pero recomendado: Limpiar listeners de ratón previos)
            for (java.awt.event.MouseListener ml : scrollMiniaturasExistente.getMouseListeners()) {
                // Quita adaptadores anónimos o específicos si puedes identificarlos
                // Esto es un poco más complejo que con ActionListener
                if (ml.getClass().isAnonymousClass() || ml.getClass().getName().contains("MouseAdapter")) {
                     scrollMiniaturasExistente.removeMouseListener(ml);
                }
            }

            // Añade el nuevo MouseAdapter
            scrollMiniaturasExistente.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    // Cuando se presiona el ratón sobre el scrollpane...
                    // USA la variable local listaMinRefExistente, que es efectivamente final
                    if (listaMinRefExistente.isEnabled() && listaMinRefExistente.isFocusable()) {
                        // ...pedir el foco para la JList interna.
                        System.out.println("-> Clic en ScrollMiniaturas detectado. Solicitando foco para listaMiniaturas...");
                        listaMinRefExistente.requestFocusInWindow(); // Intenta obtener el foco
                    }
                     // --- DEBUG ADICIONAL ---
//                     logCurrentFocus("MousePressed en ScrollMiniaturas"); // Llama al logger de foco
                     // --- FIN DEBUG ---
                }
            });
            System.out.println("  -> MouseListener añadido a scrollListaMiniaturas para gestionar foco.");
        } else {
             System.err.println("WARN [configurarListenersVistaInternal]: No se pudo añadir listener de foco a miniaturas (scroll o lista nulos).");
        }
        

        // --- SECCIÓN 4: Log Final ---
        // 4.1. Indicar que la configuración de todos los listeners de vista ha finalizado.
        System.out.println("[Controller Internal] Configuración de Listeners de Vista finalizada.");

    } // --- FIN configurarListenersVistaInternal --- 


    /**
     * Asigna las instancias de Action (previamente inicializadas y almacenadas
     * en `this.actionMap`) a los componentes correspondientes de la UI
     * (botones de la barra de herramientas y elementos del menú) obtenidos
     * de la VisorView.
     * Utiliza el método helper `setActionForKey` para realizar la asignación
     * buscando los componentes por su clave de configuración larga.
     * También añade listeners fallback a items de menú que no usan Actions.
     * Se llama desde AppInitializer (en el EDT) después de crear la Vista
     * y las Actions.
     */
    /*package-private*/ void configurarComponentActionsInternal() {
        // --- SECCIÓN 1: Log de Inicio y Validaciones ---
        // 1.1. Imprimir log indicando el inicio de la configuración.
        System.out.println("  [Config Comp Actions Internal] Configurando Actions en Componentes UI...");
        // 1.2. Validar que la Vista exista. Es necesaria para obtener los mapas de componentes.
        if (view == null) {
            System.err.println("ERROR [configurarComponentActionsInternal]: Vista es null. No se pueden configurar Actions.");
            return; // Salir si no hay vista.
        }
        // 1.3. Validar que el mapa de Actions principal (`this.actionMap`) exista.
        //      (Aunque AppInitializer debería haberlo creado e inyectado).
        if (this.actionMap == null) {
             System.err.println("ERROR [configurarComponentActionsInternal]: El mapa principal 'actionMap' es null.");
             return; // Salir si no hay mapa de acciones.
        }


        // --- SECCIÓN 2: Asignar Actions a Botones de la Barra de Herramientas ---
        // 2.1. Obtener el mapa de botones desde la vista (clave larga -> JButton).
        Map<String, JButton> botones = view.getBotonesPorNombre();
        // 2.2. Comprobar si el mapa de botones existe y no está vacío.
        if (botones != null && !botones.isEmpty()) {
            System.out.println("    -> Asignando Actions a Botones...");
            // 2.3. Llamar al método helper `setActionForKey` para cada botón que tenga una Action asociada.
            //      Se usa la clave LARGA de configuración para buscar el botón en el mapa `botones`,
            //      y se pasa la instancia de la Action correspondiente.

            // 2.3.1. Botones Toggle
            setActionForKey(botones, "interfaz.boton.toggle.Subcarpetas_48x48", toggleSubfoldersAction);
            setActionForKey(botones, "interfaz.boton.toggle.Mantener_Proporciones_48x48", toggleProporcionesAction);
            // setActionForKey(botones, "interfaz.boton.toggle.Mostrar_Favoritos_48x48", mostrarFavoritosAction); // Ejemplo si existiera

            // 2.3.2. Botones de Control/Archivo
            setActionForKey(botones, "interfaz.boton.control.Ubicacion_de_Archivo_48x48" , locateFileAction);
            setActionForKey(botones, "interfaz.boton.control.Refrescar_48x48", refreshAction); // Asumiendo que refreshAction existe
            setActionForKey(botones, "interfaz.boton.control.lista_de_favoritos_48x48", null); // Ejemplo sin action aún
            setActionForKey(botones, "interfaz.boton.control.Borrar_48x48", deleteAction); // Asumiendo que deleteAction existe

            // 2.3.3. Botones de Movimiento (Ahora usan las Actions creadas en EDT)
            setActionForKey(botones, "interfaz.boton.movimiento.Primera_48x48", firstImageAction);
            setActionForKey(botones, "interfaz.boton.movimiento.Anterior_48x48", previousImageAction);
            setActionForKey(botones, "interfaz.boton.movimiento.Siguiente_48x48", nextImageAction);
            setActionForKey(botones, "interfaz.boton.movimiento.Ultima_48x48", lastImageAction);

            // 2.3.4. Botones de Edición
            setActionForKey(botones, "interfaz.boton.edicion.Rotar_Izquierda_48x48", rotateLeftAction);
            setActionForKey(botones, "interfaz.boton.edicion.Rotar_Derecha_48x48", rotateRightAction);
            setActionForKey(botones, "interfaz.boton.edicion.Espejo_Horizontal_48x48", flipHorizontalAction);
            setActionForKey(botones, "interfaz.boton.edicion.Espejo_Vertical_48x48", flipVerticalAction);
            setActionForKey(botones, "interfaz.boton.edicion.Recortar_48x48", cortarAction); // Asumiendo que cortarAction existe

            // 2.3.5. Botones de Zoom
            setActionForKey(botones, "interfaz.boton.zoom.Zoom_48x48", toggleZoomManualAction);
            setActionForKey(botones, "interfaz.boton.zoom.Zoom_Auto_48x48", zoomAutoAction);
            setActionForKey(botones, "interfaz.boton.zoom.Ajustar_al_Ancho_48x48", zoomAnchoAction);
            setActionForKey(botones, "interfaz.boton.zoom.Ajustar_al_Alto_48x48", zoomAltoAction);
            setActionForKey(botones, "interfaz.boton.zoom.Escalar_Para_Ajustar_48x48", zoomFitAction);
            setActionForKey(botones, "interfaz.boton.zoom.Zoom_Fijo_48x48", zoomFixedAction);
            setActionForKey(botones, "interfaz.boton.zoom.zoom_especifico_48x48", zoomFijadoAction); // Corregido nombre
            setActionForKey(botones, "interfaz.boton.zoom.Reset_48x48", resetZoomAction);

            // 2.3.6. Botones de Vista (Layouts, etc. - Si tuvieran actions directas)
            // setActionForKey(botones, "interfaz.boton.vista.Panel-Galeria_48x48", panelGaleriaAction); // Ejemplo
            // setActionForKey(botones, "interfaz.boton.vista.Grid_48x48", gridAction); // Ejemplo
            // setActionForKey(botones, "interfaz.boton.vista.Pantalla_Completa_48x48", pantallaCompletaAction); // Ejemplo
            // setActionForKey(botones, "interfaz.boton.vista.Lista_48x48", listaAction); // Ejemplo
            // setActionForKey(botones, "interfaz.boton.vista.Carrousel_48x48", carrouselAction); // Ejemplo

            // 2.3.7. Botones Especiales
            setActionForKey(botones, "interfaz.boton.especiales.Selector_de_Carpetas_48x48", openAction);
            setActionForKey(botones, "interfaz.boton.especiales.Menu_48x48", menuAction); // Asumiendo que menuAction existe
            setActionForKey(botones, "interfaz.boton.especiales.Botones_Ocultos_48x48", hiddenButtonsAction); // Asumiendo que existe

            // 2.4. Log indicando fin de asignación a botones.
            System.out.println("    -> Actions asignadas a Botones.");
        // 2.5. Log de advertencia si no se encontró el mapa de botones.
        } else {
            System.err.println("WARN [configurarComponentActionsInternal]: Mapa de botones ('botonesPorNombre') nulo o vacío en la Vista.");
        }


        // --- SECCIÓN 3: Asignar Actions y Listeners a Elementos del Menú ---
        // 3.1. Obtener el mapa de items de menú desde la vista (clave larga -> JMenuItem).
        Map<String, JMenuItem> menuItems = view.getMenuItemsPorNombre();
        // 3.2. Comprobar si el mapa de menús existe y no está vacío.
        if (menuItems != null && !menuItems.isEmpty()) {
            System.out.println("    -> Asignando Actions/Listeners a Menús...");
            // 3.3. Llamar a `setActionForKey` para cada item de menú que deba usar una Action.
            //      Nuevamente, se usa la clave LARGA para buscar el JMenuItem en el mapa `menuItems`.

            // 3.3.1. Menú Archivo
            setActionForKey(menuItems, "interfaz.menu.archivo.Abrir_Archivo", openAction);
            // setActionForKey(menuItems, "interfaz.menu.archivo.Guardar", guardarAction); // Ejemplo
            // setActionForKey(menuItems, "interfaz.menu.archivo.Recargar_Lista_de_Imagenes", refreshAction); // Ejemplo
            // setActionForKey(menuItems, "interfaz.menu.archivo.Eliminar_Permanentemente", deleteAction); // Ejemplo
            setActionForKey(menuItems, "interfaz.menu.archivo.Abrir_Ubicacion_del_Archivo", locateFileAction); // Si está aquí
            // ... otros items de Archivo con Actions ...

            // 3.3.2. Menú Navegación
            setActionForKey(menuItems, "interfaz.menu.navegacion.Primera_Imagen", firstImageAction);
            setActionForKey(menuItems, "interfaz.menu.navegacion.Imagen_Aterior", previousImageAction);
            setActionForKey(menuItems, "interfaz.menu.navegacion.Imagen_Siguiente", nextImageAction);
            setActionForKey(menuItems, "interfaz.menu.navegacion.Ultima_Imagen", lastImageAction);
            // ... otros items de Navegación con Actions ...

            // 3.3.3. Menú Zoom
            // setActionForKey(menuItems, "interfaz.menu.zoom.Acercar", zoomInAction); // Ejemplo
            // setActionForKey(menuItems, "interfaz.menu.zoom.Alejar", zoomOutAction); // Ejemplo
            setActionForKey(menuItems, "interfaz.menu.zoom.Mantener_Proporciones", toggleProporcionesAction); // Checkbox
            setActionForKey(menuItems, "interfaz.menu.zoom.Activar_Zoom_Manual", toggleZoomManualAction); // Checkbox
            setActionForKey(menuItems, "interfaz.menu.zoom.Resetear_Zoom", resetZoomAction);
            // Radios de Tipos de Zoom (manejados por sus Actions respectivas)
            setActionForKey(menuItems, "interfaz.menu.zoom.tipos_de_zoom.Zoom_Automatico", zoomAutoAction);
            setActionForKey(menuItems, "interfaz.menu.zoom.tipos_de_zoom.Zoom_a_lo_Ancho", zoomAnchoAction);
            setActionForKey(menuItems, "interfaz.menu.zoom.tipos_de_zoom.Zoom_a_lo_Alto", zoomAltoAction);
            setActionForKey(menuItems, "interfaz.menu.zoom.tipos_de_zoom.Escalar_Para_Ajustar", zoomFitAction);
            setActionForKey(menuItems, "interfaz.menu.zoom.tipos_de_zoom.Zoom_Actual_Fijo", zoomFixedAction);
            setActionForKey(menuItems, "interfaz.menu.zoom.tipos_de_zoom.Zoom_Especificado", zoomFijadoAction);
            // setActionForKey(menuItems, "interfaz.menu.zoom.tipos_de_zoom.Escalar_Para_Rellenar", zoomFillAction); // Ejemplo

            // 3.3.4. Menú Imagen
            setActionForKey(menuItems, "interfaz.menu.imagen.edicion.Girar_Izquierda", rotateLeftAction);
            setActionForKey(menuItems, "interfaz.menu.imagen.edicion.Girar_Derecha", rotateRightAction);
            setActionForKey(menuItems, "interfaz.menu.imagen.edicion.Voltear_Horizontal", flipHorizontalAction);
            setActionForKey(menuItems, "interfaz.menu.imagen.edicion.Voltear_Vertical", flipVerticalAction);
            // setActionForKey(menuItems, "interfaz.menu.imagen.Eliminar_Permanentemente", deleteAction); // Si está aquí

            // 3.3.5. Menú Vista (Toggles de visibilidad)
            setActionForKey(menuItems, "interfaz.menu.vista.Barra_de_Menu", toggleMenuBarAction);
            setActionForKey(menuItems, "interfaz.menu.vista.Barra_de_Botones", toggleToolBarAction);
            setActionForKey(menuItems, "interfaz.menu.vista.Mostrar/Ocultar_la_Lista_de_Archivos", toggleFileListAction);
            setActionForKey(menuItems, "interfaz.menu.vista.Imagenes_en_Miniatura", toggleThumbnailsAction);
            setActionForKey(menuItems, "interfaz.menu.vista.Linea_de_Ubicacion_del_Archivo", toggleLocationBarAction);
            setActionForKey(menuItems, "interfaz.menu.vista.Fondo_a_Cuadros", toggleCheckeredBgAction);
            setActionForKey(menuItems, "interfaz.menu.vista.Mantener_Ventana_Siempre_Encima", toggleAlwaysOnTopAction);

            // 3.3.6. Menú Configuración
            // Radios de Tema (manejados por ToggleThemeAction)
            setActionForKey(menuItems, "interfaz.menu.configuracion.tema.Tema_Clear", temaClearAction);
            setActionForKey(menuItems, "interfaz.menu.configuracion.tema.Tema_Dark", temaDarkAction);
            setActionForKey(menuItems, "interfaz.menu.configuracion.tema.Tema_Blue", temaBlueAction);
            setActionForKey(menuItems, "interfaz.menu.configuracion.tema.Tema_Green", temaGreenAction);
            setActionForKey(menuItems, "interfaz.menu.configuracion.tema.Tema_Orange", temaOrangeAction);
            // Radios de Carga de Imágenes (NO usan Action, manejados por ActionListener específico) -> No usar setActionForKey

            // 3.4. Añadir Listener Fallback a items SIN Action.
            //      Llama al método helper que itera y añade 'this' como ActionListener
            //      a los JMenuItems que no tienen una Action asignada.
            addFallbackListeners(menuItems);

            // 3.5. Log indicando fin de asignación a menús.
            System.out.println("    -> Actions/Listeners asignados a Menús.");
        // 3.6. Log de advertencia si no se encontró el mapa de menús.
        } else {
            System.err.println("WARN [configurarComponentActionsInternal]: Mapa de menús ('menuItemsPorNombre') nulo o vacío en la Vista.");
        }

        // --- SECCIÓN 4: Log Final ---
        // 4.1. Indicar que la configuración de Actions en componentes ha finalizado.
        System.out.println("  [Config Comp Actions Internal] Finalizado.");

    } // --- FIN configurarComponentActionsInternal ---
    
    
    /**
     * Realiza sincronizaciones visuales finales después de que todos los
     * componentes principales, listeners y actions han sido configurados.
     * Principalmente se usa para asegurar que el estado visual de ciertos
     * componentes (como el fondo a cuadros) coincida con el estado lógico
     * inicial de sus Actions asociadas.
     * Se llama desde AppInitializer (en el EDT) como uno de los últimos pasos
     * de la inicialización de la UI.
     */
    /*package-private*/ void sincronizarUIFinalInternal() {
        // --- SECCIÓN 1: Log de Inicio y Validaciones ---
        // 1.1. Imprimir log indicando el inicio de la sincronización final.
        System.out.println("    [EDT Internal] Sincronizando UI Final...");
        // 1.2. Validar que la Vista exista, ya que vamos a interactuar con ella.
        if (view == null) {
             System.err.println("ERROR [sincronizarUIFinalInternal]: Vista es null. No se puede sincronizar UI final.");
             return; // Salir si no hay vista.
        }

        // --- SECCIÓN 2: Sincronizar Estado Visual del Fondo a Cuadros ---
        // 2.1. Comprobar si la Action para el fondo a cuadros existe.
        if (toggleCheckeredBgAction != null) {
            // 2.1.1. Obtener el estado lógico inicial (seleccionado/no seleccionado)
            //        desde la propiedad SELECTED_KEY de la Action. Este valor
            //        fue establecido previamente en initializeActionsInternal
            //        leyendo desde la configuración.
            boolean estadoInicialFondo = Boolean.TRUE.equals(toggleCheckeredBgAction.getValue(Action.SELECTED_KEY));
            // 2.1.2. Log del estado que se aplicará.
            System.out.println("      -> Sincronizando Fondo a Cuadros a estado: " + estadoInicialFondo);
            // 2.1.3. Llamar al método correspondiente en la Vista para aplicar
            //        el estado visual (activar/desactivar el pintado de cuadros).
            view.setCheckeredBackgroundEnabled(estadoInicialFondo);
        // 2.2. Log de advertencia si la Action no existe.
        } else {
            System.err.println("WARN [sincronizarUIFinalInternal]: toggleCheckeredBgAction es null. No se pudo sincronizar fondo.");
        }

        // --- SECCIÓN 3: Sincronizar Otros Estados Visuales (Si es Necesario) ---
        // 3.1. Sincronizar "Siempre Encima" (Always On Top)
        //      Aunque la Action lo maneja, podemos forzar la sincronización inicial aquí por seguridad.
        if (toggleAlwaysOnTopAction != null) {
            boolean estadoInicialTop = Boolean.TRUE.equals(toggleAlwaysOnTopAction.getValue(Action.SELECTED_KEY));
             // Llamar directamente al método del JFrame (VisorView hereda de JFrame)
             if (view.isAlwaysOnTop() != estadoInicialTop) { // Solo si es diferente
                 System.out.println("      -> Sincronizando Siempre Encima a estado: " + estadoInicialTop);
                 view.setAlwaysOnTop(estadoInicialTop);
             }
        } else {
             System.err.println("WARN [sincronizarUIFinalInternal]: toggleAlwaysOnTopAction es null.");
        }

        // 3.2. Añadir aquí cualquier otra sincronización visual final que sea necesaria.
        //      Por ejemplo, si el estado visual de algún componente no depende directamente
        //      de una Action y necesita establecerse basado en configuración u otro estado
        //      del modelo al finalizar la inicialización.
        //      Ejemplo hipotético:
        //      boolean algunOtroEstado = model.getAlgunOtroEstado();
        //      view.actualizarAparienciaSegunOtroEstado(algunOtroEstado);

        // --- SECCIÓN 4: Log Final ---
        // 4.1. Indicar que la sincronización final ha concluido.
        System.out.println("    [EDT Internal] Sincronización UI Final completada.");

    } // --- FIN sincronizarUIFinalInternal ---   
    
    
// *********************************************************************************************** configurarShutdownHookInternal
    

    /**
     * Configura un 'Shutdown Hook', que es un hilo que la JVM intentará ejecutar
     * cuando la aplicación está a punto de cerrarse (ya sea normalmente o por
     * una señal externa como Ctrl+C, pero no necesariamente en caso de crash).
     *
     * El propósito principal de este hook es llamar a `guardarConfiguracionActual()`
     * para persistir el estado de la aplicación (tamaño/posición de ventana,
     * última carpeta/imagen, configuraciones UI) y apagar ordenadamente el
     * ExecutorService.
     *
     * Se llama desde AppInitializer como uno de los últimos pasos de la inicialización.
     */
    /*package-private*/ void configurarShutdownHookInternal() {
        // --- SECCIÓN 1: Log de Inicio ---
        // 1.1. Indicar que se está configurando el hook.
        System.out.println("    [Internal] Configurando Shutdown Hook...");

        // --- SECCIÓN 2: Crear el Hilo del Hook ---
        // 2.1. Crear una nueva instancia de Thread.
        // 2.2. Pasar una expresión lambda como el Runnable que define la tarea a ejecutar al cerrar.
        // 2.3. Darle un nombre descriptivo al hilo (útil para depuración y perfiles).
        Thread shutdownThread = new Thread(() -> { // Inicio de la lambda para el hilo del hook
            // --- TAREA EJECUTADA AL CIERRE DE LA JVM ---

            // 2.3.1. Log indicando que el hook se ha activado.
            System.out.println("--- Hook de Cierre Ejecutándose ---");

            // 2.3.2. GUARDAR ESTADO DE LA VENTANA (si es posible)
            //        Llama a un método helper para encapsular esta lógica.
            guardarEstadoVentanaEnConfig();

            // 2.3.3. GUARDAR CONFIGURACIÓN GENERAL
            //        Llama al método que recopila todo el estado relevante y lo guarda en el archivo.
            System.out.println("  -> Llamando a guardarConfiguracionActual() desde hook...");
            guardarConfiguracionActual(); // Llama al método privado existente

            // 2.3.4. APAGAR ExecutorService de forma ordenada.
            //        Llama a un método helper para encapsular esta lógica.
            apagarExecutorServiceOrdenadamente();

            // 2.3.5. Log indicando que el hook ha terminado su trabajo.
            System.out.println("--- Hook de Cierre Terminado ---");

        }, "VisorShutdownHookThread"); // Nombre del hilo

        // --- SECCIÓN 3: Registrar el Hook en la JVM ---
        // 3.1. Obtener la instancia del Runtime de la JVM.
        Runtime runtime = Runtime.getRuntime();
        // 3.2. Añadir el hilo creado como un hook de cierre. La JVM lo llamará al salir.
        runtime.addShutdownHook(shutdownThread);

        // --- SECCIÓN 4: Log Final ---
        // 4.1. Confirmar que el hook ha sido registrado.
        System.out.println("    [Internal] Shutdown Hook registrado en la JVM.");

    } // --- FIN configurarShutdownHookInternal ---


    /**
     * Método helper PRIVADO para guardar el estado actual de la ventana (posición,
     * tamaño, estado maximizado) en el ConfigurationManager en memoria.
     * Se llama desde el Shutdown Hook.
     */
    private void guardarEstadoVentanaEnConfig() {
        // 1. Validar que la Vista y la Configuración existan.
        if (view == null || configuration == null) {
            System.out.println("  [Hook - Ventana] No se pudo guardar estado (Vista=" + view + ", Config=" + configuration + ").");
            return; // Salir si falta algo.
        }
        System.out.println("  [Hook - Ventana] Guardando estado de la ventana en config...");

        // 2. Bloque try-catch para manejar posibles excepciones al interactuar con la vista.
        try {
            // 2.1. Comprobar si la ventana está maximizada.
            //      Usa una máscara de bits para verificar el estado.
            boolean isMaximized = (view.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
            // 2.2. Guardar el estado de maximización en ConfigurationManager.
            configuration.setString(ConfigurationManager.KEY_WINDOW_MAXIMIZED, String.valueOf(isMaximized));

            // 2.3. Si la ventana NO está maximizada, guardar sus dimensiones y posición.
            if (!isMaximized) {
                // 2.3.1. Obtener el objeto Rectangle con los bounds actuales de la ventana.
                java.awt.Rectangle bounds = view.getBounds();
                // 2.3.2. Guardar las coordenadas X, Y y las dimensiones Ancho, Alto en ConfigurationManager.
                configuration.setString(ConfigurationManager.KEY_WINDOW_X, String.valueOf(bounds.x));
                configuration.setString(ConfigurationManager.KEY_WINDOW_Y, String.valueOf(bounds.y));
                configuration.setString(ConfigurationManager.KEY_WINDOW_WIDTH, String.valueOf(bounds.width));
                configuration.setString(ConfigurationManager.KEY_WINDOW_HEIGHT, String.valueOf(bounds.height));
                // 2.3.3. Log informando los bounds guardados.
                System.out.println("    -> Bounds guardados en memoria config: " + bounds);
            } else {
                // 2.3.4. Log si la ventana está maximizada (no se guardan bounds).
                System.out.println("    -> Ventana maximizada, no se guardan bounds específicos.");
            }
        // 2.4. Capturar y loguear excepciones.
        } catch (Exception e) {
            System.err.println("  [Hook - Ventana] ERROR al guardar estado: " + e.getMessage());
            e.printStackTrace(); // Imprimir detalles del error.
        }
    } // --- FIN guardarEstadoVentanaEnConfig ---


    /**
     * Método helper PRIVADO para apagar el ExecutorService de forma ordenada,
     * esperando un tiempo prudencial para que las tareas finalicen.
     * Se llama desde el Shutdown Hook.
     */
    private void apagarExecutorServiceOrdenadamente() {
        // 1. Indicar inicio del apagado.
        System.out.println("  [Hook - Executor] Apagando ExecutorService...");
        // 2. Comprobar si el ExecutorService existe y no está ya apagado.
        if (executorService != null && !executorService.isShutdown()) {
           // 2.1. Iniciar el apagado "suave": no acepta nuevas tareas,
           //      pero permite que las tareas en ejecución terminen.
           executorService.shutdown();
           // 2.2. Bloque try-catch para manejar InterruptedException durante la espera.
           try {
               // 2.2.1. Esperar un máximo de 5 segundos para que terminen las tareas.
               if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                   // 2.2.1.1. Si no terminaron a tiempo, forzar el apagado inmediato.
                   System.err.println("    -> ExecutorService no terminó en 5s. Forzando shutdownNow()...");
                   // shutdownNow() intenta interrumpir las tareas en ejecución.
                   List<Runnable> tareasPendientes = executorService.shutdownNow();
                   System.err.println("    -> Tareas que no llegaron a ejecutarse: " + tareasPendientes.size());
                   // 2.2.1.2. Esperar un poco más después de forzar.
                   if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) { // Espera más corta
                        System.err.println("    -> ExecutorService AÚN no terminó después de shutdownNow().");
                   } else {
                        System.out.println("    -> ExecutorService finalmente terminado después de shutdownNow().");
                   }
               } else {
                    // 2.2.1.3. Si terminaron a tiempo.
                    System.out.println("    -> ExecutorService terminado ordenadamente.");
               }
           // 2.2.2. Capturar si el hilo del hook es interrumpido mientras espera.
           } catch (InterruptedException ie) {
               System.err.println("    -> Hilo ShutdownHook interrumpido mientras esperaba apagado de ExecutorService.");
               // Forzar apagado inmediato si el hook es interrumpido.
               executorService.shutdownNow();
               // Re-establecer el estado de interrupción del hilo actual.
               Thread.currentThread().interrupt();
           // 2.2.3. Capturar otras excepciones inesperadas.
           } catch (Exception e) {
                System.err.println("    -> ERROR inesperado durante apagado de ExecutorService: " + e.getMessage());
                e.printStackTrace();
           }
        // 2.3. Casos donde el ExecutorService no necesita apagado.
        } else if (executorService == null){
             System.out.println("    -> ExecutorService es null. No se requiere apagado.");
        } else { // Ya estaba shutdown
             System.out.println("    -> ExecutorService ya estaba apagado.");
        }
    } // --- FIN apagarExecutorServiceOrdenadamente ---   

    
// ******************************************************************************************* FIN configurarShutdownHookInternal    
    
    
    /**
     * Asigna Actions a botones/menús y configura listeners específicos.
     */
    private void configurarComponentActions() {
         System.out.println("  [Config Comp Actions] Configurando...");
         if (view == null) { System.err.println("ERROR: Vista nula en configurarComponentActions"); return; }

         // Asignar Actions a Botones
         Map<String, JButton> botones = view.getBotonesPorNombre();
         if (botones != null) {
             // ... (Tu código setActionForKey para todos los botones) ...
              setActionForKey(botones, "interfaz.boton.toggle.Subcarpetas_48x48", toggleSubfoldersAction);
              setActionForKey(botones, "interfaz.boton.toggle.Mantener_Proporciones_48x48", toggleProporcionesAction);
              setActionForKey(botones, "interfaz.boton.control.Ubicacion_de_Archivo_48x48" , locateFileAction);
              setActionForKey(botones, "interfaz.boton.movimiento.Primera_48x48", firstImageAction);
              setActionForKey(botones, "interfaz.boton.movimiento.Anterior_48x48", previousImageAction);
              setActionForKey(botones, "interfaz.boton.movimiento.Siguiente_48x48", nextImageAction);
              setActionForKey(botones, "interfaz.boton.movimiento.Ultima_48x48", lastImageAction);
              setActionForKey(botones, "interfaz.boton.edicion.Rotar_Izquierda_48x48", rotateLeftAction);
              setActionForKey(botones, "interfaz.boton.edicion.Rotar_Derecha_48x48", rotateRightAction);
              setActionForKey(botones, "interfaz.boton.edicion.Espejo_Horizontal_48x48", flipHorizontalAction);
              setActionForKey(botones, "interfaz.boton.edicion.Espejo_Vertical_48x48", flipVerticalAction);
              setActionForKey(botones, "interfaz.boton.zoom.Zoom_48x48", toggleZoomManualAction);
              setActionForKey(botones, "interfaz.boton.zoom.Zoom_Auto_48x48", zoomAutoAction);
              setActionForKey(botones, "interfaz.boton.zoom.Ajustar_al_Ancho_48x48", zoomAnchoAction);
              setActionForKey(botones, "interfaz.boton.zoom.Ajustar_al_Alto_48x48", zoomAltoAction);
              setActionForKey(botones, "interfaz.boton.zoom.Escalar_Para_Ajustar_48x48", zoomFitAction);
              setActionForKey(botones, "interfaz.boton.zoom.Zoom_Fijo_48x48", zoomFixedAction);
              setActionForKey(botones, "interfaz.boton.zoom.Reset_48x48", resetZoomAction);
              setActionForKey(botones, "interfaz.boton.especiales.Selector_de_Carpetas_48x48", openAction);
              // ... etc ...
             System.out.println("    -> Actions asignadas a Botones.");
         } else { System.err.println("WARN: Mapa de botones nulo."); }

         // Asignar Actions/Listeners a Menús
         Map<String, JMenuItem> menuItems = view.getMenuItemsPorNombre();
         if (menuItems != null) {
              System.out.println("    -> Asignando Actions/Listeners a Menús...");
              // ... (Tu código setActionForKey para menús que usan Action) ...
               setActionForKey(menuItems, "interfaz.menu.archivo.Abrir_Archivo", openAction);
               setActionForKey(menuItems, "interfaz.menu.navegacion.Primera_Imagen", firstImageAction);
               // ... etc ...
               setActionForKey(menuItems, "interfaz.menu.configuracion.tema.Tema_Orange", temaOrangeAction);

              // Listeners específicos para radios de subcarpetas
              String keyMostrarSub = "interfaz.menu.configuracion.carga_de_imagenes.Mostrar_Imagenes_de_Subcarpetas";
              String keyMostrarSolo = "interfaz.menu.configuracion.carga_de_imagenes.Mostrar_Solo_Carpeta_Actual";
              JMenuItem radioMostrarSub = menuItems.get(keyMostrarSub);
              JMenuItem radioMostrarSolo = menuItems.get(keyMostrarSolo);
              
              if (radioMostrarSub instanceof JRadioButtonMenuItem) {
                   for(ActionListener al : radioMostrarSub.getActionListeners()) radioMostrarSub.removeActionListener(al);
                   radioMostrarSub.addActionListener(e -> setMostrarSubcarpetasAndUpdateConfig(true));
              }
              
              if (radioMostrarSolo instanceof JRadioButtonMenuItem) {
                   for(ActionListener al : radioMostrarSolo.getActionListeners()) radioMostrarSolo.removeActionListener(al);
                   radioMostrarSolo.addActionListener(e -> setMostrarSubcarpetasAndUpdateConfig(false));
              }
              
              // Añadir ActionListener central a items sin Action
              addFallbackListeners(menuItems);
              System.out.println("    -> Actions/Listeners asignados a Menús.");
              
         } else { System.err.println("WARN: Mapa de menús nulo."); }

         System.out.println("  [Config Comp Actions] Finalizado.");
    
    } // --- FIN metodo configurarComponentActions
    
    
    /**
     * Método auxiliar para asignar una Action a un componente (JButton, JMenuItem, etc.)
     * buscando el componente en un mapa proporcionado usando su clave de configuración larga.
     *
     * Además, si el componente es un JButton y la Action tiene un icono,
     * configura el botón para ocultar su texto y mostrar solo el icono.
     *
     * Imprime warnings si el componente o la Action no se encuentran.
     *
     * @param componentMap El mapa que contiene los componentes (JButton, JMenuItem),
     *                     donde la clave es la String de configuración larga
     *                     (ej. "interfaz.boton.movimiento.Siguiente_48x48").
     * @param key La clave de configuración larga del componente al que se asignará la Action.
     * @param action La instancia de la Action a asignar. Si es null, se imprimirá un warning.
     * @param <T> El tipo del componente, debe extender AbstractButton (JButton, JMenuItem, etc.).
     */
    private <T extends AbstractButton> void setActionForKey(Map<String, T> componentMap, String key, Action action) 
    {
        // 1. Validación de entradas (Mapa y Clave)
        if (componentMap == null) {
            System.err.println("ERROR [setActionForKey]: El mapa de componentes es null (para clave: " + key + ")");
            return;
        }
        if (key == null || key.trim().isEmpty()) {
             System.err.println("ERROR [setActionForKey]: La clave proporcionada es null o vacía.");
             return;
        }

        // 2. Buscar el componente en el mapa usando la clave
        T component = componentMap.get(key);

        // 3. Validar si el componente y la acción existen
        if (component != null && action != null) {
            
        	// 3.1. Ambos existen: Asignar la Action al componente
            //      Esto configura automáticamente texto, icono (si existen en la Action),
            //      estado enabled, tooltip (SHORT_DESCRIPTION), y maneja el actionPerformed.
            component.setAction(action);
            // System.out.println("  -> Action '" + action.getValue(Action.NAME) + "' asignada a componente con clave: " + key); // Log detallado opcional

            // 3.2. Opcional: Configuración específica para JButton con icono
            //      Si es un botón y la acción le proporcionó un icono, ocultar el texto.
             if (component instanceof JButton && action.getValue(Action.SMALL_ICON) != null) {
                 // setHideActionText(true) es preferible a setText("") porque
                 // permite que el Look and Feel decida si mostrar texto si NO hay icono.
                 ((JButton) component).setHideActionText(true);
                 // System.out.println("    -> Ocultando texto para JButton con icono."); // Log opcional
             }

        } else {
            // 4. Manejar casos donde algo falta
            if (component == null) {
                // No se encontró el componente en el mapa con esa clave
                System.err.println("WARN [setActionForKey]: Componente NO encontrado en el mapa para la clave: '" + key + "'");
            }
            if (action == null) {
                // Se encontró el componente, pero la Action proporcionada era null
                System.err.println("WARN [setActionForKey]: La Action proporcionada es NULL para la clave: '" + key + "' (Componente: " + (component != null ? component.getClass().getSimpleName() : "null") + ")");
                // Podríamos querer deshabilitar el componente si la acción es null
                 if (component != null) {
                     component.setEnabled(false); // Deshabilitar si no tiene acción
                     component.setToolTipText("Acción no disponible"); // Tooltip indicativo
                 }
            }
        }
    } // --- FIN setActionForKey ---
    

    /**
     * Añade el ActionListener principal del controlador ('this') a aquellos
     * JMenuItems que no tienen una clase Action dedicada asignada mediante setAction().
     *
     * Esto se usa como un mecanismo "fallback" para manejar clics en items de menú
     * simples (como "Versión", "Guardar Configuración") o en radios/checkboxes
     * que requieran lógica personalizada que no encaja bien en una Action estándar.
     *
     * El ActionListener central (el método actionPerformed de VisorController)
     * deberá usar el ActionCommand del evento para determinar qué item fue presionado.
     *
     * @param menuItems El mapa que contiene todos los JMenuItems creados,
     *                  donde la clave es la clave de configuración larga y el valor
     *                  es la instancia de JMenuItem.
     */
     private void addFallbackListeners(Map<String, JMenuItem> menuItems) {
         // 1. Validación de entrada
         if (menuItems == null) {
             System.err.println("WARN [addFallbackListeners]: El mapa de menuItems es null. No se añadieron listeners fallback.");
             return;
         }
         System.out.println("    [Fallback Listeners] Añadiendo ActionListener central a items sin Action...");
         int listenersAñadidos = 0;

         // 2. Iterar sobre todos los items de menú en el mapa
         //    La clave (key) es la clave de configuración larga, el valor (item) es el JMenuItem.
         for (Map.Entry<String, JMenuItem> entry : menuItems.entrySet()) {
             JMenuItem item = entry.getValue();
             //String key = entry.getKey(); // Clave larga (útil para logs)

             // 3. Comprobar condiciones para añadir el listener fallback:
             //    a) El item debe ser un item real clickeable (no un JMenu separador o contenedor).
             //    b) El item NO debe tener ya una Action asignada (item.getAction() == null).
             if (item != null && !(item instanceof JMenu) && item.getAction() == null) {

                 // 4. Limpiar listeners previos (buena práctica por si se llama varias veces)
                 //    Esto evita que 'this' se añada múltiples veces al mismo item.
                 for (ActionListener al : item.getActionListeners()) {
                     // Podríamos ser más específicos y solo quitar instancias de VisorController,
                     // pero quitar todos los ActionListeners antes de añadir el nuestro es más simple.
                     item.removeActionListener(al);
                 }

                 // 5. Añadir 'this' (la instancia actual de VisorController) como ActionListener
                 item.addActionListener(this);
                 listenersAñadidos++;
                 // Log detallado (opcional)
                 // System.out.println("      -> Listener fallback añadido a: '" + key + "' (Comando: '" + item.getActionCommand() + "')");

             }
             // Si es un JMenu o ya tiene una Action, no hacemos nada.
             // else {
             //     System.out.println("      -> Omitiendo listener fallback para: '" + key + "' (Es JMenu o ya tiene Action)"); // Log detallado opcional
             // }
         } // Fin del bucle for

         // 6. Log final
         System.out.println("    [Fallback Listeners] Finalizado. Listeners fallback añadidos a " + listenersAñadidos + " items.");

     } // --- FIN addFallbackListeners ---
    
    
    
 // ********************************************************************************************************** FIN DE ACTIONS     
    
    
 // ******************************************************************************************************************* CARGA      

     
    /**
     * Carga la configuración inicial de la interfaz de usuario (visibilidad, selección de menús/botones)
     * leyendo los valores desde ConfigurationManager y aplicándolos a la Vista y al Modelo.
     * Se llama desde el constructor (en el EDT) después de crear la Vista.
     */
     //FIXME ajustar este metodo que ahora mismo esta repetido. por uno refactorizado en condiciones 
    private void aplicarConfiguracionInicial() { // Solo para el boton del menu
        System.out.println("  [Apply Config] Aplicando configuración inicial...");
        // Verificar dependencias primero
        if (configuration == null || view == null || model == null) {
            System.err.println("ERROR [aplicarConfiguracionInicial]: Configuración, Vista o Modelo nulos. Abortando.");
            return;
        }

        // 6.1. Aplicar configuración al Modelo (valores que afectan lógica)
        try {
            model.setMiniaturasAntes(configuration.getInt("miniaturas.cantidad.antes", 7));
            model.setMiniaturasDespues(configuration.getInt("miniaturas.cantidad.despues", 7));
            model.setMiniaturaSelAncho(configuration.getInt("miniaturas.tamano.seleccionada.ancho", 60));
            model.setMiniaturaSelAlto(configuration.getInt("miniaturas.tamano.seleccionada.alto", 60));
            model.setMiniaturaNormAncho(configuration.getInt("miniaturas.tamano.normal.ancho", 40));
            model.setMiniaturaNormAlto(configuration.getInt("miniaturas.tamano.normal.alto", 40));
            boolean cargarSubcarpetas = configuration.getBoolean("comportamiento.carpeta.cargarSubcarpetas", true);
            model.setMostrarSoloCarpetaActual(!cargarSubcarpetas);
            System.out.println("    -> Config Modelo OK.");
        } catch (Exception e) { System.err.println("ERROR aplicando config al Modelo: " + e.getMessage()); }

        // 6.2. Aplicar configuración a Botones (Enabled/Visible)
        Map<String, JButton> botones = view.getBotonesPorNombre();
	    if (botones != null) {
             System.out.println("    -> Aplicando config a Botones...");
	        botones.forEach((claveCompletaBoton, button) -> {
                try {
	                button.setEnabled(configuration.getBoolean(claveCompletaBoton + ".activado", true));
	                button.setVisible(configuration.getBoolean(claveCompletaBoton + ".visible", true));
                } catch (Exception e) { System.err.println("ERROR aplicando a Botón '" + claveCompletaBoton + "': " + e.getMessage()); }
	        });
             System.out.println("    -> Config Botones OK.");
	    } else { System.err.println("WARN: Mapa de botones nulo."); }

        // 6.3. Aplicar configuración a Menús (Enabled/Visible/Selected sin Action)
        Map<String, JMenuItem> menuItems = view.getMenuItemsPorNombre();
	    if (menuItems != null) {
	        System.out.println("    -> Aplicando config a Menús...");
	        menuItems.forEach((claveCompletaMenu, menuItem) -> {
	             try {
	                 menuItem.setEnabled(configuration.getBoolean(claveCompletaMenu + ".activado", true));
	                 menuItem.setVisible(configuration.getBoolean(claveCompletaMenu + ".visible", true));
	                 // Aplicar .seleccionado solo si es seleccionable Y NO tiene Action
	                 boolean esSeleccionable = (menuItem instanceof JCheckBoxMenuItem || menuItem instanceof JRadioButtonMenuItem);
	                 boolean tieneAction = (menuItem.getAction() != null);
	                 if (esSeleccionable && !tieneAction) {
	                      // System.out.println("      -> Aplicando .seleccionado (sin Action): " + claveCompletaMenu);
	                      if (menuItem instanceof JCheckBoxMenuItem) { ((JCheckBoxMenuItem) menuItem).setSelected(configuration.getBoolean(claveCompletaMenu + ".seleccionado", false)); }
	                      else if (menuItem instanceof JRadioButtonMenuItem) { ((JRadioButtonMenuItem) menuItem).setSelected(configuration.getBoolean(claveCompletaMenu + ".seleccionado", false)); }
	                 }
	             } catch (Exception e) { System.err.println("ERROR aplicando a Menú '" + claveCompletaMenu + "': " + e.getMessage()); }
	        });
	         System.out.println("    -> Config Menús OK.");
	    } else { System.err.println("WARN: Mapa de menús nulo."); }

        // 6.4. Aplicar estados iniciales específicos (manejados por Actions o lógica dedicada)
        //    - El estado SELECTED de Actions (como Zoom Manual, Proporciones, Subcarpetas, Tema)
        //      ya se establece al inicializar la Action (en initializeActions) leyendo la config.
        //    - Solo necesitamos sincronizar la UI que NO depende directamente de la Action.
        try {
            // Sincronizar estado visual del botón/checkbox de Zoom Manual (si la Action no lo hace automát.)
             boolean zoomManualInicial = Boolean.TRUE.equals(toggleZoomManualAction.getValue(Action.SELECTED_KEY));
             if(view != null) view.actualizarEstadoControlesZoom(zoomManualInicial, zoomManualInicial); // Habilita Reset si Zoom está activo

            // Sincronizar estado visual del botón de Proporciones
            boolean proporcionesInicial = Boolean.TRUE.equals(toggleProporcionesAction.getValue(Action.SELECTED_KEY));
            actualizarAspectoBotonToggle(toggleProporcionesAction, proporcionesInicial);

            // Sincronizar estado visual del botón y radios de Subcarpetas
            boolean subcarpetasInicial = Boolean.TRUE.equals(toggleSubfoldersAction.getValue(Action.SELECTED_KEY));
            actualizarAspectoBotonToggle(toggleSubfoldersAction, subcarpetasInicial);
            restaurarSeleccionRadiosSubcarpetas(subcarpetasInicial); // Asegurar estado visual radios
             System.out.println("    -> Estados iniciales específicos (Zoom, Prop, Sub) aplicados a UI.");
        } catch(Exception e) { System.err.println("ERROR aplicando estados específicos: " + e.getMessage()); }


        System.out.println("  [Apply Config] Finalizado.");
        
    }// --- FIN del metodo aplicarConfiguracionInicial


    /**
     * Carga la carpeta y la imagen iniciales definidas en la configuración.
     * Si no hay configuración válida, limpia la interfaz.
     * Se llama desde el constructor (en el EDT) después de aplicar la config inicial.
     */
    private void cargarEstadoInicial() {
        System.out.println("  [Load Initial State] Cargando estado inicial...");
        // Verificar dependencias
        if (configuration == null || model == null || view == null) {
            System.err.println("ERROR [cargarEstadoInicial]: Config, Modelo o Vista nulos.");
            limpiarUI(); // Limpiar si faltan componentes esenciales
            return;
        }

        String folderInit = configuration.getString("inicio.carpeta", "");
        Path folderPath = null;
        boolean carpetaValida = false;

        // Validar la carpeta inicial
        if (!folderInit.isEmpty()) {
            try {
                folderPath = Paths.get(folderInit);
                if (Files.isDirectory(folderPath)) {
                    carpetaValida = true;
                    this.carpetaRaizActual = folderPath; // Establecer como raíz actual
                } else {
                     System.err.println("WARN [cargarEstadoInicial]: Carpeta inicial en config no es un directorio válido: " + folderInit);
                }
            } catch (Exception e) {
                System.err.println("WARN [cargarEstadoInicial]: Ruta de carpeta inicial inválida en config: " + folderInit + " - " + e.getMessage());
            }
        }

        // Proceder a cargar solo si la carpeta es válida
        if (carpetaValida) {
            System.out.println("    -> Cargando lista para carpeta inicial: " + folderPath);
            // Obtener la clave de la imagen inicial ANTES de llamar a cargarListaImagenes
            // porque cargarListaImagenes ahora puede cambiar la selección.
            String imagenInicialKey = configuration.getString("inicio.imagen", null);

            // Llamar a cargarListaImagenes. Esta se encargará de la carga en background
            // y de seleccionar la imagen (la primera o la 'imagenInicialKey' si se pasa).
            cargarListaImagenes(imagenInicialKey);

        } else {
            System.out.println("    -> No hay carpeta inicial válida en config o no está definida. Limpiando UI.");
            limpiarUI();
        }
        System.out.println("  [Load Initial State] Finalizado.");
    }// --- FIN del metodo cargarEstadoInicial
    
    
	/**
	 * Carga o recarga la lista de imágenes desde disco para una carpeta específica,
	 * utilizando un SwingWorker para no bloquear el EDT. Muestra un diálogo de
	 * progreso durante la carga. Una vez cargada la lista: - Actualiza el modelo
	 * principal de datos (`VisorModel`). - Actualiza las JList en la vista
	 * (`VisorView`). - Inicia el precalentamiento asíncrono del caché de
	 * miniaturas. - Selecciona una imagen específica (si se proporciona
	 * `claveImagenAMantener`) o la primera imagen de la lista. - Maneja la
	 * selección inicial de forma segura usando el flag `seleccionInicialEnCurso`.
	 *
	 * @param claveImagenAMantener La clave única (ruta relativa) de la imagen que
	 *                             se intentará seleccionar después de que la lista
	 *                             se cargue. Si es `null`, se seleccionará la
	 *                             primera imagen (índice 0).
	 */
	private void cargarListaImagenes (String claveImagenAMantener)
	{

		// --- 1. LOG INICIO Y VALIDACIONES PREVIAS ---
		// 1.1. Log detallado del inicio y la clave a mantener
		System.out.println("\n-->>> INICIO cargarListaImagenes(String) | Mantener Clave: " + claveImagenAMantener);

		// 1.2. Verificar dependencias críticas del sistema
		if (configuration == null || model == null || executorService == null || executorService.isShutdown()
				|| view == null)
		{
			System.err.println(
					"ERROR [cargarListaImagenes]: Dependencias nulas (Config, Modelo, Executor o Vista) o Executor apagado.");

			// Intentar limpiar la UI si es posible
			if (view != null)
				SwingUtilities.invokeLater(this::limpiarUI);
			estaCargandoLista = false; // Asegurar que el estado de carga sea falso
			return; // Salir del método
		}

		// 1.3. Marcar que la carga de la lista está en curso
		estaCargandoLista = true;

		// --- 2. CANCELAR TAREAS ANTERIORES ---

		// 2.1. Cancelar cualquier tarea previa de carga de lista que aún esté
		if (cargaImagenesFuture != null && !cargaImagenesFuture.isDone())
		{
			System.out.println("  -> Cancelando tarea de carga de lista anterior...");
			cargaImagenesFuture.cancel(true); // Intentar interrumpir la tarea
		}

		// 2.2. (Si existiera lógica similar para cancelar precalentamiento de
		// miniaturas, iría aquí)

		// --- 3. DETERMINAR PARÁMETROS DE BÚSQUEDA ---

		// 3.1. Determinar si se deben incluir subcarpetas (basado en el estado del
		// modelo)
		final boolean mostrarSoloCarpeta = model.isMostrarSoloCarpetaActual(); // Necesita ser final para lambda/worker

		// 3.2. Establecer la profundidad de búsqueda para Files.walk
		int depth = mostrarSoloCarpeta ? 1 : Integer.MAX_VALUE; // 1 para solo carpeta actual, MAX_VALUE para recursivo
		System.out.println("  -> Modo búsqueda: "
				+ (mostrarSoloCarpeta ? "Solo Carpeta Actual (depth=1)" : "Subcarpetas (depth=MAX)"));

		// 3.3. Determinar la carpeta desde donde iniciar la búsqueda
		Path pathDeInicioWalk = null;

		if (mostrarSoloCarpeta)
		{

			// Si es solo carpeta actual, intentar usar la carpeta de la imagen a
			// mantener/seleccionada
			String claveRef = claveImagenAMantener != null ? claveImagenAMantener : model.getSelectedImageKey();
			Path rutaRef = claveRef != null ? model.getRutaCompleta(claveRef) : null;
			// Si tenemos una ruta válida y es un archivo, usar su carpeta padre

			if (rutaRef != null && Files.isRegularFile(rutaRef))
			{
				pathDeInicioWalk = rutaRef.getParent();
			}

			// Fallback: si no se pudo obtener la carpeta de la imagen, usar la raíz actual
			if (pathDeInicioWalk == null || !Files.isDirectory(pathDeInicioWalk))
			{
				System.out.println("    -> No se pudo obtener carpeta de imagen actual válida. Usando carpeta raíz: "
						+ this.carpetaRaizActual);
				pathDeInicioWalk = this.carpetaRaizActual; // Usar la carpeta raíz guardada en el controller

			} else
			{
				System.out.println("    -> Iniciando búsqueda desde carpeta de imagen: " + pathDeInicioWalk);
			}

		} else
		{
			// Si es modo subcarpetas, siempre empezar desde la raíz actual
			pathDeInicioWalk = this.carpetaRaizActual;
			System.out.println("    -> Iniciando búsqueda desde carpeta raíz: " + this.carpetaRaizActual);
		}

		// --- 4. VALIDAR PATH DE INICIO Y PROCEDER ---

		// 4.1. Comprobar si el path calculado es un directorio válido
		if (pathDeInicioWalk != null && Files.isDirectory(pathDeInicioWalk))
		{
			// 4.2. Crear variables finales para usar en lambdas y worker
			final Path finalStartPath = pathDeInicioWalk;
			final int finalDepth = depth;
			final String finalClaveImagenAMantener = claveImagenAMantener; // Para el listener done

			// --- 5. LIMPIEZA INICIAL DE LA UI ---
			// 5.1. Programar limpieza en el EDT
			if (view != null)
			{
				SwingUtilities.invokeLater( () -> {

					// 5.1.1. Limpiar imagen principal y texto de ruta
					view.limpiarImagenMostrada();
					view.setTextoRuta("");

					// 5.1.2. Indicar que se está escaneando
					view.setTituloPanelIzquierdo("Escaneando: " + finalStartPath.getFileName() + "...");

					// 5.1.3. Limpiar visualmente la lista de miniaturas asignándole un modelo vacío
					// nuevo
					if (view.getListaMiniaturas() != null)
					{

						// Limpiar también el modelo de instancia del controller
						if (this.modeloMiniaturas != null)
						{
							this.modeloMiniaturas.clear();
						}
						view.getListaMiniaturas().setModel(new DefaultListModel<>());
					}
				});
			}

			// FIXME 5.2. Limpiar caché de miniaturas (opcional, pero puede liberar memoria)
			// if (servicioMiniaturas != null) { servicioMiniaturas.limpiarCache(); }

			// --- 6. CREAR DIÁLOGO DE PROGRESO Y SWINGWORKER ---
			// 6.1. Log
			System.out.println("  -> Creando diálogo y worker para: " + finalStartPath);

			// 6.2. Crear el Diálogo primero (pasando null como worker inicial)
			final ProgresoCargaDialog dialogo = new ProgresoCargaDialog(view != null ? view.getFrame() : null, // Padre
					null // Worker se asigna después
			);

			// 6.3. Crear el Worker, pasándole el diálogo
			System.out.println("  -> Creando Worker...");
			final BuscadorArchivosWorker worker = new BuscadorArchivosWorker(finalStartPath, finalDepth,
					this.carpetaRaizActual, // Para relativizar
					this::esArchivoImagenSoportado, // Filtro
					dialogo // Referencia al diálogo para actualizar progreso
			);

			// 6.4. Asociar el worker al diálogo (para el botón Cancelar)
			System.out.println("  -> Asociando worker al diálogo...");
			dialogo.setWorkerAsociado(worker);

			// 6.5. Guardar referencia a la tarea futura
			this.cargaImagenesFuture = worker;
			System.out.println("  -> Diálogo y Worker creados y asociados.");

			// --- 7. CONFIGURAR LISTENER PARA EL FIN DEL WORKER ('done') ---
			System.out.println("  -> Añadiendo PropertyChangeListener al worker...");
			worker.addPropertyChangeListener(evt -> { // Inicio lambda listener 'done'

				// 7.1. Comprobar si el evento es el de finalización
				if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE.equals(evt.getNewValue()))
				{
					// --- Código Ejecutado en EDT al finalizar el Worker ---
					System.out.println(" [EDT Worker Done] Tarea finalizada. Procesando resultado...");

					// 7.2. Cerrar el diálogo de progreso
					dialogo.cerrar();

					// 7.3. Comprobar si la tarea fue cancelada
					if (worker.isCancelled())
					{
						System.out.println("   -> Tarea CANCELADA.");

						if (view != null)
						{
							limpiarUI();
							view.setTituloPanelIzquierdo("Carga Cancelada");
						}
						estaCargandoLista = false; // Resetear estado
						return; // Salir del listener
					}

					// 7.4. Obtener resultado del worker (puede lanzar excepciones)
					try
					{
						Map<String, Path> mapaResultado = worker.get(); // Obtiene el mapa <Clave, Path>

						// 7.5. Procesar si el resultado es válido
						if (mapaResultado != null)
						{
							// 7.5.1. Crear nuevo modelo de lista principal ordenado
							DefaultListModel<String> nuevoModeloLista = new DefaultListModel<>();
							List<String> clavesOrdenadas = new ArrayList<>(mapaResultado.keySet());
							Collections.sort(clavesOrdenadas); // Ordenar alfabéticamente (o como se desee)
							clavesOrdenadas.forEach(nuevoModeloLista::addElement);
							System.out.println("   -> Resultado obtenido: " + nuevoModeloLista.getSize()
									+ " archivos. Actualizando...");

							// 7.5.2. Actualizar el Modelo principal de la aplicación
							model.actualizarListaCompleta(nuevoModeloLista, mapaResultado);

							// 7.5.3. Actualizar la Vista (asignar modelo principal a las JList)
							if (view != null)
							{
								view.setListaImagenesModel(model.getModeloLista()); // Actualiza listaNombres y
																					// listaMiniaturas (con modelo
																					// principal)
								view.setTituloPanelIzquierdo("Archivos: " + model.getModeloLista().getSize());
							}

							// 7.5.4. Marcar que la carga lógica ha finalizado
							estaCargandoLista = false;
							System.out.println("   -> estaCargandoLista = false");

							// 7.5.5. Iniciar precalentamiento del caché de miniaturas
							precalentarCacheMiniaturasAsync(new ArrayList<>(mapaResultado.values()));

							// 7.5.6. Calcular el Índice Inicial a Seleccionar
							int indiceSeleccionadoLocal = -1;
							DefaultListModel<String> modeloActualizado = model.getModeloLista();

							// Intentar encontrar la clave a mantener
							if (finalClaveImagenAMantener != null && !modeloActualizado.isEmpty())
							{
								indiceSeleccionadoLocal = modeloActualizado.indexOf(finalClaveImagenAMantener); // Más
																												// eficiente

								if (indiceSeleccionadoLocal != -1)
								{
									System.out.println("     -> Clave a mantener encontrada en índice: "
											+ indiceSeleccionadoLocal);
								}
							}

							// Si no se encontró la clave o no había que mantener, o lista vacía,
							// seleccionar 0 si es posible
							if (indiceSeleccionadoLocal == -1 && !modeloActualizado.isEmpty())
							{
								System.out.println("     -> Seleccionando índice 0 por defecto.");
								indiceSeleccionadoLocal = 0;
							}

							// 7.5.7. Aplicar Selección Inicial y Forzar Procesamiento Completo
							if (indiceSeleccionadoLocal != -1)
							{

								if (view != null && view.getListaNombres() != null && listCoordinator != null)
								{
									System.out.println(
											"   -> [ANTES de setSelectedIndex] Intentando aplicar selección inicial a índice: "
													+ indiceSeleccionadoLocal);
									final int indiceSeleccionadoFinal = indiceSeleccionadoLocal;

									// Iniciar Flag
									seleccionInicialEnCurso = true; //
									System.out.println("   -> Flag seleccionInicialEnCurso puesto a true.");

									// Establecer selección VISUAL inicial (listener será ignorado)
									view.getListaNombres().setSelectedIndex(indiceSeleccionadoFinal);
									System.out.println(
											"   -> [DESPUÉS de setSelectedIndex] Selección visual inicial aplicada a índice: "
													+ indiceSeleccionadoFinal + ". Selección JList: "
													+ view.getListaNombres().getSelectedIndex());

									// FORZAR llamada al Coordinator para cargar imagen y actualizar miniaturas
									System.out.println(
											"   -> Llamando MANUALMENTE a Coordinator para procesar índice inicial: "
													+ indiceSeleccionadoFinal);
									listCoordinator.seleccionarIndiceYActualizarUICompleta(indiceSeleccionadoFinal); // <<--
																														// LLAMADA
																														// EXPLÍCITA
									System.out.println("   -> Llamada MANUAL a Coordinator completada.");

									// Timer para desactivar flag y asegurar visibilidad
									Timer timerFinSeleccionInicial = new Timer(150, (evtTimer) -> {
										System.out.println(
												"   -> [Timer Fin Selección Inicial] Fin espera. Flag seleccionInicialEnCurso -> false.");
										seleccionInicialEnCurso = false; // <<-- DESACTIVAR FLAG
										System.out.println(
												"   -> [Timer Fin Selección Inicial] Asegurando visibilidad final para índice: "
														+ indiceSeleccionadoFinal);
									});
									timerFinSeleccionInicial.setRepeats(false);
									timerFinSeleccionInicial.start();

								} else
								{
									System.err.println(
											"WARN [EDT Done]: Vista, listaNombres o listCoordinator nulos al aplicar selección.");
								}
							} else
							{ // Lista vacía después de carga

								System.out.println("   -> Lista vacía después de carga. Limpiando UI.");
								limpiarUI();
							}

						} else
						{ // mapaResultado fue null
							System.out.println("   -> Resultado del worker fue null.");

							if (view != null)
							{
								limpiarUI();
								view.setTituloPanelIzquierdo("Carga Incompleta");
							}
							estaCargandoLista = false;
						}

						// 7.6. Manejar Excepciones durante worker.get()
					} catch (CancellationException ce)
					{
						System.out.println("   -> Tarea CANCELADA (detectado en get).");

						if (view != null)
						{
							limpiarUI();
							view.setTituloPanelIzquierdo("Carga Cancelada");
						}
						estaCargandoLista = false;

					} catch (InterruptedException ie)
					{
						System.err.println("   -> Hilo INTERRUMPIDO esperando resultado.");

						if (view != null)
						{
							limpiarUI();
							view.setTituloPanelIzquierdo("Carga Interrumpida");
						}
						Thread.currentThread().interrupt(); // Re-interrumpir hilo
						estaCargandoLista = false;

					} catch (ExecutionException ee)
					{
						System.err.println("   -> ERROR durante ejecución del worker: " + ee.getCause());
						Throwable causa = ee.getCause();
						String msg = (causa != null) ? causa.getMessage() : ee.getMessage();

						if (view != null)
						{
							JOptionPane.showMessageDialog(view.getFrame(), "Error durante la carga:\n" + msg,
									"Error Carga", JOptionPane.ERROR_MESSAGE);
							limpiarUI();
							view.setTituloPanelIzquierdo("Error de Carga");
						}

						if (causa != null)
							causa.printStackTrace();
						else
							ee.printStackTrace();
						estaCargandoLista = false;

					} finally
					{

						// 7.7. Asegurar que el estado de carga se resetea
						if (estaCargandoLista)
						{
							System.out.println(
									"WARN [EDT Done - finally]: estaCargandoLista aún era true. Forzando a false.");
							estaCargandoLista = false;
						}

						// 7.8. Limpiar referencia a la tarea futura
						if (cargaImagenesFuture == worker)
						{
							cargaImagenesFuture = null;
						}
						System.out.println(" [EDT Worker Done] Procesamiento finalizado.");

					} // Fin finally
				} // Fin if ("state" == DONE)
			}); // ----- FIN BLOQUE listener done DEL WORKER -----

			// --- 8. EJECUTAR WORKER Y MOSTRAR DIÁLOGO ---

			// 8.1. Log
			System.out.println("  -> Ejecutando worker y mostrando diálogo...");

			// 8.2. Ejecutar el worker (inicia doInBackground en otro hilo)
			worker.execute();

			// 8.3. Mostrar el diálogo de progreso (en el EDT)
			SwingUtilities.invokeLater( () -> {
				System.out.println(" [EDT] Mostrando diálogo de progreso...");
				dialogo.setVisible(true); // Esto bloqueará hasta que se cierre
				System.out.println(" [EDT] Diálogo de progreso cerrado o ya no visible.");
			});

		} else
		{ // pathDeInicioWalk no era válido

			// --- 9. MANEJAR ERROR DE CARPETA INVÁLIDA ---
			System.out.println("No se puede cargar la lista: Carpeta de inicio inválida o nula: " + pathDeInicioWalk);

			// Limpiar la UI si no se puede cargar
			if (view != null)
				SwingUtilities.invokeLater(this::limpiarUI);
			estaCargandoLista = false; // Resetear estado
		}

		// --- 10. LOG FINAL DEL MÉTODO ---
		System.out.println("-->>> FIN cargarListaImagenes(String) | Mantener Clave: " + claveImagenAMantener);

	} // --- FIN del método cargarListaImagenes(String claveImagenAMantener) ---    
    
    
    
// ************************************************************************************************************ FIN DE CARGA    

    
// *************************************************************************************************************** NAVEGACION
    
	
    /**
     * Configura los bindings de teclado personalizados para las JList, enfocándose
     * principalmente en las flechas direccionales. Las teclas HOME, END, PAGE_UP, PAGE_DOWN
     * serán manejadas globalmente por el KeyEventDispatcher cuando el foco esté
     * en el área de miniaturas.
     */
    @SuppressWarnings("serial")
    /*package-private*/ void interceptarAccionesTecladoListas() {
    	
        if (view == null || listCoordinator == null) {
            System.err.println("WARN [interceptarAccionesTecladoListas]: Vista o ListCoordinator nulos.");
            return;
        }
        System.out.println("  -> Configurando bindings de teclado para JLists (Principalmente Flechas)...");

        // Nombres de Acción Únicos
        String actPrev = "coordSelectPrevious";
        String actNext = "coordSelectNext";
        String actFirst = "coordSelectFirst"; // Lo necesitaremos para listaNombres
        String actLast = "coordSelectLast";   // Lo necesitaremos para listaNombres
        String actPrevBlock = "coordSelectPrevBlock"; // Lo necesitaremos para listaNombres
        String actNextBlock = "coordSelectNextBlock"; // Lo necesitaremos para listaNombres

        // --- SECCIÓN 1: listaNombres (WHEN_FOCUSED) ---
        // Mantenemos todos los bindings aquí, ya que WHEN_FOCUSED tiene alta prioridad
        // y el KeyEventDispatcher puede diferenciar por el foco.
        JList<String> listaNombres = view.getListaNombres();
        if (listaNombres != null) {
            ActionMap actionMapNombres = listaNombres.getActionMap();
            InputMap inputMapNombres = listaNombres.getInputMap(JComponent.WHEN_FOCUSED);
//            System.out.println("    -> Configurando listaNombres (WHEN_FOCUSED)...");

            inputMapNombres.put(KeyStroke.getKeyStroke("UP"), actPrev);
            inputMapNombres.put(KeyStroke.getKeyStroke("DOWN"), actNext);
            inputMapNombres.put(KeyStroke.getKeyStroke("LEFT"), actPrev);
            inputMapNombres.put(KeyStroke.getKeyStroke("RIGHT"), actNext);
            inputMapNombres.put(KeyStroke.getKeyStroke("HOME"), actFirst);
            inputMapNombres.put(KeyStroke.getKeyStroke("END"), actLast);
            inputMapNombres.put(KeyStroke.getKeyStroke("PAGE_UP"), actPrevBlock);
            inputMapNombres.put(KeyStroke.getKeyStroke("PAGE_DOWN"), actNextBlock);

            actionMapNombres.put(actPrev, new AbstractAction(actPrev) { @Override public void actionPerformed(ActionEvent e) 	{ /*logActionOrigin("Nombres", "UP/LEFT"); 		*/if (listCoordinator != null) listCoordinator.seleccionarAnterior(); }});
            actionMapNombres.put(actNext, new AbstractAction(actNext) { @Override public void actionPerformed(ActionEvent e) 	{ /*logActionOrigin("Nombres", "DOWN/RIGHT"); 	*/if (listCoordinator != null) listCoordinator.seleccionarSiguiente(); }});
            actionMapNombres.put(actFirst, new AbstractAction(actFirst) { @Override public void actionPerformed(ActionEvent e) 	{ /*logActionOrigin("Nombres", "HOME"); 		*/if(listCoordinator != null) listCoordinator.seleccionarPrimero(); }});
            actionMapNombres.put(actLast, new AbstractAction(actLast) { @Override public void actionPerformed(ActionEvent e) 	{ /*logActionOrigin("Nombres", "END"); 			*/if(listCoordinator != null) listCoordinator.seleccionarUltimo(); }});
            actionMapNombres.put(actPrevBlock, new AbstractAction(actPrevBlock) { @Override public void actionPerformed(ActionEvent e) { /*logActionOrigin("Nombres", "PAGE_UP"); 	*/if (listCoordinator != null) listCoordinator.seleccionarBloqueAnterior(); }});
            actionMapNombres.put(actNextBlock, new AbstractAction(actNextBlock) { @Override public void actionPerformed(ActionEvent e) { /*logActionOrigin("Nombres", "PAGE_DOWN"); */if (listCoordinator != null) listCoordinator.seleccionarBloqueSiguiente(); }});
//            System.out.println("    -> Acciones de teclado configuradas en listaNombres.");
        } else {
        	System.err.println("WARN [interceptarAccionesTecladoListas]: listaNombres es null.");
        }

        // --- SECCIÓN 2: listaMiniaturas (WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) ---
        // SOLO mapeamos las flechas aquí. HOME/END/PGUP/DN se manejarán globalmente.
        JList<String> listaMiniaturas = view.getListaMiniaturas();
        if (listaMiniaturas != null) {
            ActionMap actionMapMiniaturas = listaMiniaturas.getActionMap();
            InputMap inputMapMiniaturas = listaMiniaturas.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
//            System.out.println("    -> Configurando listaMiniaturas (WHEN_ANCESTOR) - SOLO FLECHAS...");

            // Mapear SOLO las flechas
            inputMapMiniaturas.put(KeyStroke.getKeyStroke("LEFT"), actPrev);  // LEFT -> ANTERIOR
            inputMapMiniaturas.put(KeyStroke.getKeyStroke("RIGHT"), actNext); // RIGHT -> SIGUIENTE
            // --- NO MAPEAR HOME, END, PAGE_UP, PAGE_DOWN AQUÍ ---

            // Añadir/Reemplazar acciones SOLO para las flechas
            actionMapMiniaturas.put(actPrev, new AbstractAction(actPrev) { @Override public void actionPerformed(ActionEvent e) { /*logActionOrigin("Miniaturas", "UP/LEFT");*/ if (listCoordinator != null) listCoordinator.seleccionarAnterior(); }});
            actionMapMiniaturas.put(actNext, new AbstractAction(actNext) { @Override public void actionPerformed(ActionEvent e) { /*logActionOrigin("Miniaturas", "DOWN/RIGHT");*/ if (listCoordinator != null) listCoordinator.seleccionarSiguiente(); }});
            // --- NO AÑADIR ACCIONES PARA HOME, END, PAGE_UP, PAGE_DOWN AQUÍ ---

//            System.out.println("    -> Acciones de teclado (solo flechas) configuradas en listaMiniaturas.");
        } else { 
        	System.err.println("WARN [interceptarAccionesTecladoListas]: listaMiniaturas es null.");
        	
        }

        // --- SECCIÓN 2.5: NO TOCAR EL SCROLLPANE ---
//        System.out.println("    -> NO se modifican bindings del ScrollPane/Viewport aquí.");

        // --- SECCIÓN 3: Log Final ---
//        System.out.println("  -> Configuración de bindings de JLists completada.");

    } // --- FIN interceptarAccionesTecladoListas ---


    // --- MÉTODOS HELPER (Asegúrate de tenerlos en VisorController) ---

    /**
     * Metodo para controlar los Logs de interceptarAccionesTecladoListas
     * @param listName
     * @param keyName
     */
    private void logActionOrigin(String listName, String keyName) {//weno
        System.out.println(">>> Acción Teclado Ejecutada [Origen: " + listName + ", Tecla(s): " + keyName + "]");
        java.awt.Component focusOwner = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        System.out.println("    Foco actual: " + (focusOwner != null ? focusOwner.getClass().getName() + " (Name: " + focusOwner.getName() + ")" : "null"));
    }// --- FIN logActionOrigin

    /**
     * Metodo para controlar los Logs de interceptarAccionesTecladoListas
     * @param context
     */
    private void logCurrentFocus(String context) {
         java.awt.Component focusOwner = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
         System.out.println("### DEBUG FOCO ["+ context +"]: " + (focusOwner != null ? focusOwner.getClass().getName() + " (Name: "+focusOwner.getName()+", Hash: " + System.identityHashCode(focusOwner) + ")" : "null") + " ###");
     }// --- FIN logCurrentFocus

    
    /**
     * Intercepta eventos de teclado a nivel global ANTES de que lleguen
     * al componente enfocado. Se utiliza para manejar específicamente
     * HOME, END, PAGE_UP, PAGE_DOWN cuando el foco está en el área
     * de la lista de miniaturas, anulando el comportamiento por defecto
     * del JScrollPane.
     *
     * @param e El KeyEvent a procesar.
     * @return true si el evento fue consumido (manejado aquí), false para
     *         permitir que el evento continúe su procesamiento normal.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        // Solo procesar eventos KEY_PRESSED
        if (e.getID() != KeyEvent.KEY_PRESSED) {
            return false; // Dejar pasar otros tipos de evento
        }

        // Obtener el componente con el foco actual
        java.awt.Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

        // Referencias seguras a componentes relevantes
        JScrollPane scrollMin = (view != null) ? view.getScrollListaMiniaturas() : null;
        JList<String> listaNom = (view != null) ? view.getListaNombres() : null; // Necesitamos listaNombres para excluirla

        // Verificar si el foco está dentro del scroll pane de miniaturas
        boolean focoEnAreaMiniaturas = focusOwner != null && scrollMin != null &&
                                       SwingUtilities.isDescendingFrom(focusOwner, scrollMin);

        // --- LOG INICIAL ---
//         System.out.println("--- dispatchKeyEvent --- Tecla: " + KeyEvent.getKeyText(e.getKeyCode()) +
//                            ", Foco En Area Minis?: " + focoEnAreaMiniaturas +
//                            ", Componente Foco: " + (focusOwner != null ? focusOwner.getClass().getName() + " ("+focusOwner.getName()+")" : "null"));
         // --- FIN LOG INICIAL ---


        // Si el foco está en el área de miniaturas Y NO es la listaNombres principal...
        if (focoEnAreaMiniaturas && focusOwner != listaNom) {
            int keyCode = e.getKeyCode(); // Obtener el código de la tecla presionada
            boolean consumed = false; // Flag para saber si consumimos

            // Comprobar si es una de las teclas que queremos interceptar aquí
            switch (keyCode) {
                case KeyEvent.VK_HOME:
//                    System.out.println("    -> HOME detectado por Dispatcher. Llamando a seleccionarPrimero...");
                    if (listCoordinator != null) listCoordinator.seleccionarPrimero();
                    consumed = true;
                    break; // Salir del switch

                case KeyEvent.VK_END:
//                    System.out.println("    -> END detectado por Dispatcher. Llamando a seleccionarUltimo...");
                    if (listCoordinator != null) listCoordinator.seleccionarUltimo();
                    consumed = true;
                    break;

                case KeyEvent.VK_PAGE_UP:
//                    System.out.println("    -> PAGE_UP detectado por Dispatcher. Llamando a seleccionarBloqueAnterior...");
                    if (listCoordinator != null) listCoordinator.seleccionarBloqueAnterior();
                    consumed = true;
                    break;

                case KeyEvent.VK_PAGE_DOWN:
//                    System.out.println("    -> PAGE_DOWN detectado por Dispatcher. Llamando a seleccionarBloqueSiguiente...");
                    if (listCoordinator != null) listCoordinator.seleccionarBloqueSiguiente();
                    consumed = true;
                    break;

                // Dejamos pasar las flechas para que los bindings de listaMiniaturas las cojan
                case KeyEvent.VK_UP:
//                	System.out.println("    -> PAGE_DOWN detectado por Dispatcher. Llamando a seleccionarBloqueSiguiente...");
                    if (listCoordinator != null) listCoordinator.seleccionarAnterior();
                    consumed = true;
                    break;
                    
                case KeyEvent.VK_DOWN:
//                	System.out.println("    -> PAGE_DOWN detectado por Dispatcher. Llamando a seleccionarBloqueSiguiente...");
                    if (listCoordinator != null) listCoordinator.seleccionarSiguiente();
                    consumed = true;
                    break;
                	
                //esto se maneja en interceptarAccionesTecladoListas                    
                case KeyEvent.VK_LEFT: 
                case KeyEvent.VK_RIGHT:
//                     System.out.println("    -> Flecha detectada por Dispatcher. Dejando pasar (consumed=false).");
                     // No hacemos nada, consumed sigue false
                     break;

                default:
                    // Otra tecla, no la manejamos aquí
                    break; // No hacer nada, consumed sigue false
            }

            if (consumed) {
                System.out.println("    -> Evento CONSUMIDO por Dispatcher.");
                e.consume();
                return true; // Indicar que lo hemos manejado
            } else {
                // Si no consumimos (era una flecha u otra tecla)
                System.out.println("    -> Evento NO consumido por dispatcher (pasará a bindings JList si los hay).");
                return false; // Dejar que siga
            }
        }

        // Si el foco no estaba en area miniaturas O era listaNombres,
        // dejar que el evento continúe normalmente.
        return false;
    }// FIN dispatchKeyEvent    
    
	
    /**
     * Navega a la imagen anterior o siguiente en la lista principal (listaNombres).
     * Calcula el nuevo índice basado en la dirección y el modo 'wrapAround' (actualmente fijo a true).
     * Si el índice calculado es diferente al actual, actualiza la selección
     * en la JList de nombres (view.getListaNombres()), lo que a su vez
     * disparará el ListSelectionListener para cargar la nueva imagen y sincronizar
     * la lista de miniaturas.
     *
     * @param direccion Un entero que indica la dirección de navegación:
     *                  -1 para ir a la imagen anterior.
     *                   1 para ir a la imagen siguiente.
     *                  (Otros valores podrían usarse para saltos mayores si se modifica la lógica).
     */
    public void navegarImagen(int direccion) {
        // 1. Validar dependencias y estado
        if (model == null || view == null || view.getListaNombres() == null || model.getModeloLista() == null) {
            System.err.println("WARN [navegarImagen]: Modelo, Vista o ListaNombres no inicializados.");
            return;
        }

        DefaultListModel<String> modeloActual = model.getModeloLista();
        if (modeloActual.isEmpty()) {
            System.out.println("[navegarImagen] Lista vacía, no se puede navegar.");
            return; // No hay nada a donde navegar
        }

        // 2. Obtener estado actual
        int indiceActual = view.getListaNombres().getSelectedIndex();
        int totalImagenes = modeloActual.getSize();

        // Si no hay nada seleccionado (índiceActual == -1), empezar desde el principio o final
        if (indiceActual < 0) {
            if (direccion > 0) { // Si vamos hacia adelante, empezar por la primera
                indiceActual = -1; // Para que nextIndex sea 0
            } else if (direccion < 0) { // Si vamos hacia atrás, empezar por la última
                indiceActual = totalImagenes; // Para que nextIndex sea total-1
            } else {
                return; // Dirección 0, no hacer nada
            }
        }

        // 3. Calcular el próximo índice
        int indiceSiguiente = indiceActual + direccion;

        // 4. Aplicar lógica de 'Wrap Around' (dar la vuelta al llegar al final/inicio)
        //    FIXME: Hacer 'wrapAround' configurable leyendo de 'configuration' si se desea.
        boolean wrapAround = true; // Actualmente siempre da la vuelta

        if (wrapAround) {
            if (indiceSiguiente < 0) {
                // Si nos pasamos por el principio, vamos al final
                indiceSiguiente = totalImagenes - 1;
            } else if (indiceSiguiente >= totalImagenes) {
                // Si nos pasamos por el final, vamos al principio
                indiceSiguiente = 0;
            }
            // Si estamos dentro de los límites (0 a total-1), indiceSiguiente no cambia
            
        } else {
            // Sin 'Wrap Around': Limitar el índice al rango válido [0, totalImagenes - 1]
            indiceSiguiente = Math.max(0, Math.min(indiceSiguiente, totalImagenes - 1));
        }

        // 5. Actualizar selección en la Vista si el índice ha cambiado
        //    Comprobamos también que el índice calculado sea válido
        if (indiceSiguiente != indiceActual && indiceSiguiente >= 0 && indiceSiguiente < totalImagenes) {
            System.out.println("[navegarImagen] Cambiando índice de " + indiceActual + " a " + indiceSiguiente);
            // Establecer la selección en la lista de nombres.
            // Esto disparará el ListSelectionListener que hemos configurado,
            // el cual se encargará de:
            //   a) Llamar a mostrarImagenSeleccionada() para cargar la imagen grande.
            //   b) Sincronizar la selección en listaMiniaturas.
            //   c) Llamar a ensureIndexIsVisible en ambas listas (dentro de los listeners).
            view.getListaNombres().setSelectedIndex(indiceSiguiente);

            // Ya no necesitamos llamar a ensureIndexIsVisible aquí, los listeners lo hacen.
            // view.getListaNombres().ensureIndexIsVisible(indiceSiguiente);
        } else {
            System.out.println("[navegarImagen] El índice no cambió o es inválido. Índice actual: " + indiceActual + ", Siguiente calculado: " + indiceSiguiente);
        }

    } // --- FIN navegarImagen ---


    /**
     * Navega directamente a un índice específico en la lista principal (listaNombres).
     * Valida el índice proporcionado antes de intentar cambiar la selección.
     * Si el índice es válido y diferente al actual, actualiza la selección
     * en la JList de nombres (view.getListaNombres()), lo que a su vez
     * disparará el ListSelectionListener para cargar la nueva imagen y sincronizar
     * la lista de miniaturas.
     *
     * @param index El índice del elemento (imagen) al que se desea navegar.
     *              Debe estar dentro del rango [0, tamañoLista - 1].
     */
    public void navegarAIndice(int index) {
        // 1. Validar dependencias y estado
        if (model == null || view == null || view.getListaNombres() == null || model.getModeloLista() == null) {
            System.err.println("WARN [navegarAIndice]: Modelo, Vista o ListaNombres no inicializados.");
            return;
        }

        DefaultListModel<String> modeloActual = model.getModeloLista();
        int totalImagenes = modeloActual.getSize();

        // 2. Validar el índice proporcionado
        if (modeloActual.isEmpty()) {
            System.out.println("[navegarAIndice] Lista vacía, no se puede navegar al índice " + index + ".");
            return; // No hay elementos
        }
        if (index < 0 || index >= totalImagenes) {
            System.err.println("WARN [navegarAIndice]: Índice solicitado (" + index + ") fuera de rango [0, " + (totalImagenes - 1) + "].");
            return; // Índice inválido
        }

        // 3. Obtener índice actual y comparar
        int indiceActual = view.getListaNombres().getSelectedIndex();

        // 4. Actualizar selección en la Vista si el índice es diferente
        if (index != indiceActual) {
            System.out.println("[navegarAIndice] Navegando a índice: " + index);
            // Establecer la selección en la lista de nombres.
            // Esto disparará el ListSelectionListener configurado,
            // que cargará la imagen y sincronizará las listas.
            view.getListaNombres().setSelectedIndex(index);

            // Asegurar visibilidad (el listener también lo hace, pero
            // ponerlo aquí puede dar una respuesta visual más inmediata si
            // la llamada viene de una acción directa como HOME/END).
            // Asegurar visibilidad en la lista de nombres si es visible
             JPanel pIzq = view.getPanelIzquierdo();
             if(pIzq != null && pIzq.isVisible()) {
                  view.getListaNombres().ensureIndexIsVisible(index);
             }
             // Asegurar visibilidad en la lista de miniaturas si es visible
             JScrollPane scrollMinis = view.getScrollListaMiniaturas();
             JList<String> listaMinis = view.getListaMiniaturas();
             if (scrollMinis != null && scrollMinis.isVisible() && listaMinis != null) {
                  listaMinis.ensureIndexIsVisible(index);
             }

        } else {
            System.out.println("[navegarAIndice] El índice solicitado (" + index + ") ya es el actual. No se hace nada.");
        }

    } // --- FIN navegarAIndice ---
    
    
    
// ********************************************************************************************************* FIN DE NAVEGACION    
// ***************************************************************************************************************************    

// ***************************************************************************************************************************    
// ****************************************************************************************************************** UTILIDAD
    
    /**
     * Inicia el proceso de carga y visualización de la imagen principal.
     * Llamado por ListCoordinator después de actualizar el índice oficial.
     * @param indiceSeleccionado El índice de la imagen a mostrar (aunque usa model.getSelectedImageKey()).
     *                         El índice es útil aquí principalmente para logs o lógica futura.
     */
     public void actualizarImagenPrincipal(int indiceSeleccionado) { // El índice es informativo
         // 1. Validar dependencias críticas
         if (view == null || model == null || executorService == null || executorService.isShutdown()) {
             System.err.println("WARN [actualizarImagenPrincipal]: Vista, Modelo o Executor no listos. Abortando carga.");
             return;
         }

         // 2. Obtener la CLAVE de la imagen seleccionada DESDE EL MODELO
         String archivoSeleccionadoKey = model.getSelectedImageKey();

         // 3. Validar la clave obtenida
         if (archivoSeleccionadoKey == null) {
              System.out.println("[actualizarImagenPrincipal] No hay clave seleccionada en modelo. Limpiando UI si es necesario.");

              if(model.getCurrentImage() != null || !"Lista de Archivos".equals(view.getPanelIzquierdo().getBorder().toString())) { // Comprobación extra para no limpiar innecesariamente
                   limpiarUI();
              }
             return;
         }

         System.out.println("--> [Controller] Iniciando carga IMAGEN PRINCIPAL para (delegado por Coordinator): '" + archivoSeleccionadoKey + "'");

         // 4. Cancelar carga de imagen principal anterior si estuviera en curso
         if (cargaImagenPrincipalFuture != null && !cargaImagenPrincipalFuture.isDone()) {
             System.out.println("  -> Cancelando carga imagen principal anterior...");
             cargaImagenPrincipalFuture.cancel(true); // Intentar interrumpir la tarea anterior
         }

         // 5. Actualizar estado de Actions que dependen de la selección (Ej: Localizar)
         if (locateFileAction instanceof LocateFileAction) {
             ((LocateFileAction) locateFileAction).updateEnabledState();
         }
         // (Añadir aquí otras Actions si dependen de que haya una imagen seleccionada)

         // 6. Obtener la ruta completa y actualizar la UI al estado "Cargando"
         Path rutaCompleta = model.getRutaCompleta(archivoSeleccionadoKey);

         if (rutaCompleta != null) {
        	 
             // 6.1 Mostrar ruta en barra de estado
             view.setTextoRuta(rutaCompleta.toString());
             
             // 6.2 Mostrar indicador visual de carga en el panel de imagen
             view.mostrarIndicadorCargaImagenPrincipal("Cargando: " + rutaCompleta.getFileName() + "...");
         } else {
        	 
             // 6.3 Manejar error si la clave válida no tiene ruta (inconsistencia grave)
             System.err.println("ERROR GRAVE [actualizarImagenPrincipal]: No se encontró ruta completa para la clave válida: " + archivoSeleccionadoKey);
             model.setSelectedImageKey(null); // Deshacer selección en modelo como medida de seguridad
             
             if(view != null) {
                 view.limpiarImagenMostrada();
                 view.setTextoRuta("Error CRÍTICO: Ruta no encontrada para " + archivoSeleccionadoKey);
             }
             // Podríamos incluso mostrar un JOptionPane aquí, ya que es un error inesperado.
             return; // Salir si no hay ruta
         }

         // 7. Enviar Tarea de Carga al ExecutorService
         final String finalKey = archivoSeleccionadoKey; // Clave final para lambda
         final Path finalPath = rutaCompleta;           // Ruta final para lambda
         System.out.println("    Lanzando tarea de carga en background para: " + finalPath);

         cargaImagenPrincipalFuture = executorService.submit(() -> { // Inicio lambda tarea background
             
        	 // 7.1 Log inicio tarea background
             System.out.println("      [BG Img Load] Iniciando lectura para: " + finalPath);
             BufferedImage img = null; // Variable para la imagen cargada
             String errorMsg = null;   // Variable para mensaje de error

             // 7.2 Bloque try-catch para la lectura del archivo
             try {
             
            	 // 7.2.1 Comprobar existencia del archivo (defensivo)
                  if (!Files.exists(finalPath)) {
                      throw new IOException("El archivo no existe en la ruta especificada: " + finalPath);
                  }
                 
                  // 7.2.2 Leer la imagen usando ImageIO
                 img = ImageIO.read(finalPath.toFile());

                  // 7.2.3 Comprobar si la tarea fue interrumpida DESPUÉS de leer
                  if (Thread.currentThread().isInterrupted()) {
                      System.out.println("      [BG Img Load] Tarea interrumpida DESPUÉS de leer.");
                      // No procesar la imagen si fue interrumpida
                      return; // Salir del lambda
                  }

                 // 7.2.4 Comprobar si ImageIO devolvió null (formato no soportado, error silencioso)
                 if (img == null) {
                      errorMsg = "Formato no soportado o archivo inválido.";
                      System.err.println("      [BG Img Load] Error: " + errorMsg + " (" + finalPath.getFileName() + ")");
                 } else {
                      System.out.println("      [BG Img Load] Lectura correcta.");
                 }

             // 7.3 Capturar excepciones específicas
             } catch (IOException ioEx) {
                 errorMsg = "Error de E/S: " + ioEx.getMessage();
                 System.err.println("      [BG Img Load] " + errorMsg + " (" + finalPath.getFileName() + ")");
                 
             } catch (OutOfMemoryError oom) {
                  errorMsg = "Memoria insuficiente para cargar la imagen.";
                  System.err.println("      [BG Img Load] " + errorMsg + " (" + finalPath.getFileName() + ")");
                  if(servicioMiniaturas != null) servicioMiniaturas.limpiarCache(); // Intentar liberar memoria
                  
             } catch (Exception ex) { // Captura genérica para otros errores
                  errorMsg = "Error inesperado al cargar: " + ex.getClass().getSimpleName() + " - " + ex.getMessage();
                  System.err.println("      [BG Img Load] " + errorMsg + " (" + finalPath.getFileName() + ")");
                  ex.printStackTrace(); // Imprimir stack trace completo para depuración
             }

             // 8. Actualizar Modelo y Vista en el EDT (después de la carga)
             //    Solo si la tarea no fue interrumpida Y la clave seleccionada AÚN es la misma
             final BufferedImage finalImg = img;           // Imagen final (puede ser null)
             final String finalErrorMsg = errorMsg; // Mensaje de error final (puede ser null)

             if (!Thread.currentThread().isInterrupted() && finalKey.equals(model.getSelectedImageKey())) {
                 
            	 // 8.1 Ejecutar actualización de UI en el Event Dispatch Thread
                 SwingUtilities.invokeLater(() -> {
                 
                	 // 8.1.1 Log de entrada al invokeLater
                     System.out.println("      [EDT Img Load] Ejecutando invokeLater para: " + finalKey);

                     // 8.1.2 Re-validar dependencias dentro del EDT
                     if (view == null || model == null) {
                          System.err.println("      [EDT Img Load] ERROR: Vista o Modelo nulos en invokeLater!");
                          return; // Salir si algo desapareció
                     }
                     
                     // 8.1.3 Logs de depuración adicionales
//                     System.out.println("      [EDT Img Load] Imagen final (finalImg) es null? " + (finalImg == null));
                     if(finalImg != null) System.out.println("      [EDT Img Load] Tamaño imagen original: " + finalImg.getWidth() + "x" + finalImg.getHeight());
//                     System.out.println("      [EDT Img Load] Mensaje de error (finalErrorMsg): " + finalErrorMsg);

                     // 8.1.4 Comprobar si la carga fue exitosa (finalImg no es null)
                     if (finalImg != null) {
                         // === Caso Éxito ===
                         System.out.println("      [EDT Img Load] => Éxito. Actualizando modelo...");
                     
                         // 8.1.4.a Actualizar imagen en el modelo
                         model.setCurrentImage(finalImg);
                         
                         // 8.1.4.b Resetear zoom/pan si el modo manual no está activo
                         if (!model.isZoomHabilitado()) {
                             model.resetZoomState();
                         }

                         // 8.1.4.c Calcular imagen reescalada para mostrar en la vista
                         System.out.println("      [EDT Img Load] => Llamando a reescalar...");
                         Image reescalada = reescalarImagenParaAjustar();
                         System.out.println("      [EDT Img Load] Imagen reescalada es null? " + (reescalada == null));

                         // 8.1.4.d Actualizar la vista con la imagen reescalada
                         if (reescalada != null) {
                              System.out.println("      [EDT Img Load] => Llamando a view.setImagenMostrada...");
                              view.setImagenMostrada(reescalada, model.getZoomFactor(), model.getImageOffsetX(), model.getImageOffsetY());
                              System.out.println("      [EDT Img Load] => FIN ÉXITO.");
                         
                         } else {
                         
                        	 // Error durante el reescalado (raro si la imagen original es válida)
                              System.err.println("WARN [EDT Img Load]: No se pudo reescalar la imagen cargada. Limpiando vista.");
                              view.limpiarImagenMostrada(); // Limpiar por seguridad
                              // Podríamos mostrar un mensaje de error más específico aquí si quisiéramos
                         }
                         
                     } else {
                         
                    	 // === Caso Error ===
                         System.err.println("      [EDT Img Load] => Error detectado al cargar: " + finalErrorMsg);
                         
                         // 8.1.4.e Asegurar que no hay imagen en el modelo
                         model.setCurrentImage(null);
                         
                         // 8.1.4.f Limpiar la vista
                         view.limpiarImagenMostrada();
                         
                         // 8.1.4.g Mostrar mensaje de error en la barra de estado
                         view.setTextoRuta("Error cargando: " + finalPath.getFileName() + (finalErrorMsg != null ? " ("+finalErrorMsg+")" : ""));
                         
                         // 8.1.4.h Resetear zoom/pan por si acaso
                         model.resetZoomState();
                         if(model.isZoomHabilitado()) {
                              setManualZoomEnabled(false); // Desactivar zoom manual si hubo error
                         }
                     }
                 }); // Fin invokeLater para actualizar UI
             } else {
            	 
                  // 8.2 La tarea fue cancelada O la selección cambió mientras se cargaba
                  System.out.println("      [BG Img Load] Carga cancelada o selección cambiada. Descartando resultado para: " + finalKey);
             }
         }); // --- FIN Tarea Background (executorService.submit) ---

         // 9. Log final del método principal
         System.out.println("--> Tarea carga IMAGEN PRINCIPAL lanzada para: " + archivoSeleccionadoKey);

     } // --- FIN actualizarImagenPrincipal ---
    
     
     /**
      * Limpia el estado del modelo de datos y actualiza la interfaz de usuario
      * a un estado vacío o por defecto. Se utiliza al iniciar sin carpeta,
      * si la carpeta seleccionada es inválida, o al manejar ciertos errores.
      */
     public void limpiarUI() {
         // 1. Log inicio
         System.out.println("[Controller] Limpiando UI y Modelo a estado vacío...");

         //FIXME limpiar todas las listas para refrescar la pantalla (seria util? mejor un metodo al que llamar desde un boton?)
         // 2. Limpiar el Modelo de Datos Principal ('model')
         if (model != null) {
             // 2.1. Limpiar la lista de archivos y el mapa de rutas
             //      Llamar a actualizarListaCompleta con listas/mapas vacíos es
             //      la forma más consistente ahora que este método actualiza la vista.
             model.actualizarListaCompleta(new DefaultListModel<>(), new HashMap<>());
             // Las siguientes llamadas ya no son estrictamente necesarias porque
             // actualizarListaCompleta ya pone selectedKey y currentImage a null,
             // pero las dejamos por claridad o si actualizarListaCompleta cambiara.
             model.setCurrentImage(null);
             model.setSelectedImageKey(null);
             // 2.2. Resetear el estado del zoom/paneo
             model.resetZoomState();
              System.out.println("  -> Modelo limpiado.");
         } else {
              System.err.println("WARN [limpiarUI]: Modelo es null. No se pudo limpiar.");
         }

         // 3. Actualizar la Vista ('view')
         if (view != null) {
             // 3.1. Asegurar que las JList usan el modelo ahora vacío
             if (model != null) { // Asegurarse que modelo no es null para getModeloLista
                 view.setListaImagenesModel(model.getModeloLista()); // Actualiza ambas listas
             } else {
                 // Si el modelo es null, pasar un modelo vacío directamente
                  view.setListaImagenesModel(new DefaultListModel<>());
             }

             // 3.2. Limpiar la etiqueta de la imagen principal
             view.limpiarImagenMostrada();

             // 3.3. Limpiar la barra de estado (texto de ruta)
             view.setTextoRuta("");

             // 3.4. Establecer título por defecto en el panel de la lista
             view.setTituloPanelIzquierdo("Lista de Archivos");

             // 3.5. Repintar la lista de miniaturas (ahora vacía)
             //      No necesitamos limpiar un panel, solo repintar la JList.
             if (view.getListaMiniaturas() != null) {
                  view.getListaMiniaturas().repaint();
                  System.out.println("  -> Lista de miniaturas repintada (vacía).");
             }

             // 3.6. Actualizar estado de Actions que dependen de la selección
             //      (Ej: Deshabilitar "Localizar Archivo", Edición, etc.)
             if (locateFileAction instanceof LocateFileAction) {
                  ((LocateFileAction) locateFileAction).updateEnabledState(); // Debería deshabilitarse
             }
             // Llamar a métodos similares o forzar reevaluación del enabled
             // para otras actions si es necesario. Por ejemplo:
             if (previousImageAction != null) previousImageAction.setEnabled(false);
             if (nextImageAction != null) nextImageAction.setEnabled(false);
             if (firstImageAction != null) firstImageAction.setEnabled(false);
             if (lastImageAction != null) lastImageAction.setEnabled(false);
             if (rotateLeftAction != null) rotateLeftAction.setEnabled(false);
             //FIXME faltan metodos? rotateRightAction ... 
             // ... etc. para otras actions relevantes ...

              System.out.println("  -> Vista actualizada a estado vacío.");

         } else {
             System.err.println("WARN [limpiarUI]: Vista es null. No se pudo actualizar UI.");
         }

         // 4. Limpiar caché de miniaturas (Opcional pero recomendado al limpiar todo)
         if (servicioMiniaturas != null) {
             servicioMiniaturas.limpiarCache();
             System.out.println("  -> Caché de miniaturas limpiado.");
         }

         // 5. Log fin
         System.out.println("[Controller] Limpieza de UI y Modelo completada.");

     } // --- FIN limpiarUI ---

     
    
// *********************************************************************************************************** FIN DE UTILIDAD  
// ***************************************************************************************************************************    

// ***************************************************************************************************************************     
// ******************************************************************************************************************** LOGICA
     
     
     /**
      * Verifica si un archivo, dado por su Path, tiene una extensión
      * correspondiente a los formatos de imagen que la aplicación soporta actualmente.
      * La comparación de extensiones ignora mayúsculas/minúsculas.
      *
      * Formatos soportados actualmente: JPG, JPEG, PNG, GIF, BMP.
      *
      * @param path El objeto Path que representa la ruta del archivo a verificar.
      *             No debe ser null.
      * @return true si el archivo tiene una extensión de imagen soportada,
      *         false si no la tiene, si el path es null, o si no tiene nombre de archivo.
      */
     private boolean esArchivoImagenSoportado(Path path) {
         // 1. Validación de entrada: Asegurar que el Path no sea null
         if (path == null) {
             // No imprimir error aquí, es normal que se llame con null a veces
             return false;
         }

         // 2. Obtener el nombre del archivo del Path
         Path nombreArchivoPath = path.getFileName();
         if (nombreArchivoPath == null) {
             // Path representa un directorio raíz o algo sin nombre de archivo
             return false;
         }
         String nombreArchivo = nombreArchivoPath.toString();

         // 3. Evitar procesar archivos ocultos o carpetas (defensivo)
         try {
              if (!Files.isRegularFile(path) || Files.isHidden(path)) {
                   return false;
              }
         } catch (IOException e) {
              // Error al acceder a atributos del archivo, tratar como no soportado
               System.err.println("WARN [esArchivoImagenSoportado]: Error al comprobar atributos de " + path + ": " + e.getMessage());
               return false;
         } catch (SecurityException se) {
              // No tenemos permisos para leer atributos
               System.err.println("WARN [esArchivoImagenSoportado]: Sin permisos para comprobar atributos de " + path);
               return false;
         }


         // 4. Encontrar la posición del último punto (separador de extensión)
         int lastDotIndex = nombreArchivo.lastIndexOf('.');
         if (lastDotIndex <= 0 || lastDotIndex == nombreArchivo.length() - 1) {
             // No hay punto, empieza con punto (oculto en Unix), o termina con punto (sin extensión)
             return false;
         }

         // 5. Extraer la extensión y convertir a minúsculas
         String extension = nombreArchivo.substring(lastDotIndex + 1).toLowerCase();

         // FIXME preparar para cuando haya mas extensiones si procede 
         // 6. Comprobar si la extensión está en la lista de soportadas
         //    Usar un switch es legible para pocas extensiones
         switch (extension) {
             case "jpg":
             case "jpeg":
             case "png":
             case "gif":
             case "bmp":
                 // TODO: Añadir más si se soportan (tiff, webp, etc.)
                 return true; // Es una extensión soportada
             default:
                 return false; // No es una extensión soportada
         }

         /* Alternativa con List.of y contains (un poco más flexible si tienes muchas):
            List<String> extensionesSoportadas = List.of("jpg", "jpeg", "png", "gif", "bmp");
            return extensionesSoportadas.contains(extension);
         */

     } // --- FIN esArchivoImagenSoportado ---

 
     /**
      * Lanza tareas en segundo plano usando el ExecutorService para generar y cachear
      * las miniaturas de tamaño normal para la lista de rutas de imágenes proporcionada.
      * Esto ayuda a que el MiniaturaListCellRenderer encuentre las miniaturas ya listas
      * en el caché la mayoría de las veces, mejorando la fluidez del scroll.
      *
      * @param rutas La lista de objetos Path correspondientes a todas las imágenes
      *              cargadas actualmente en el modelo principal.
      */
     private void precalentarCacheMiniaturasAsync(List<Path> rutas) {
         // 1. Validar dependencias y entrada
         if (servicioMiniaturas == null) {
              System.err.println("ERROR [Precalentar Cache]: ThumbnailService es nulo.");
              return;
         }
         if (executorService == null || executorService.isShutdown()) {
              System.err.println("ERROR [Precalentar Cache]: ExecutorService no está disponible o está apagado.");
              return;
         }
         if (rutas == null || rutas.isEmpty()) {
             System.out.println("[Precalentar Cache]: Lista de rutas vacía o nula. No hay nada que precalentar.");
             return;
         }
         if (model == null) { // Necesitamos el modelo para obtener las dimensiones normales
              System.err.println("ERROR [Precalentar Cache]: Modelo es nulo.");
              return;
         }

         System.out.println("[Controller] Iniciando pre-calentamiento de caché para " + rutas.size() + " miniaturas...");

         // 2. Obtener Dimensiones Normales del Modelo
         //    Usamos las dimensiones configuradas para las miniaturas "no seleccionadas"
         final int anchoNormal = model.getMiniaturaNormAncho();
         final int altoNormal = model.getMiniaturaNormAlto();

         // Verificar que las dimensiones sean válidas
         if (anchoNormal <= 0) {
             System.err.println("ERROR [Precalentar Cache]: Ancho normal de miniatura inválido (" + anchoNormal + "). Abortando.");
             return;
         }
         // Nota: altoNormal puede ser <= 0 si se quiere mantener proporción basada en anchoNormal

         // 3. Enviar una Tarea al Executor por cada Imagen
         //    Cada tarea generará (si no existe ya) y cacheará una miniatura.
         int tareasLanzadas = 0;
         for (Path ruta : rutas) {
             // Saltar si la ruta es nula (aunque no debería pasar si la carga fue correcta)
             if (ruta == null) continue;

             // Enviar la tarea al ExecutorService
             executorService.submit(() -> { // Inicio lambda tarea individual
                 try {
                	 
                     // 3.1. Generar Clave Única para el Caché
                     //      (Debe ser consistente con cómo se genera en otros lugares)
                     Path relativePath = null;
                     if (this.carpetaRaizActual != null) { // Necesita acceso a carpetaRaizActual
                          try {
                        	  
                              // Intentar relativizar respecto a la carpeta raíz actual
                              relativePath = this.carpetaRaizActual.relativize(ruta);
                              
                          } catch (IllegalArgumentException e) {
                               // Si no se puede relativizar (ej. están en unidades diferentes), usar nombre archivo
                               // System.err.println("WARN [Precalentar Cache BG]: No se pudo relativizar " + ruta + ". Usando nombre.");
                               relativePath = ruta.getFileName();
                               
                          } catch (Exception e) {
                               // Otro error inesperado al relativizar
                               System.err.println("ERROR [Precalentar Cache BG]: Relativizando " + ruta + ": " + e.getMessage());
                               relativePath = ruta.getFileName(); // Fallback
                          }
                          
                     } else {
                          // Si no hay carpeta raíz definida, usar solo el nombre del archivo
                          // System.err.println("WARN [Precalentar Cache BG]: Carpeta raíz actual es null. Usando nombre archivo.");
                          relativePath = ruta.getFileName();
                     }

                     // Asegurar que relativePath no sea null y obtener clave
                     if (relativePath == null) {
                          System.err.println("ERROR [Precalentar Cache BG]: No se pudo obtener ruta relativa para " + ruta);
                          return; // Salir de esta tarea lambda específica
                     }
                     String claveUnica = relativePath.toString().replace("\\", "/");


                     // 3.2. Llamar al Servicio para Obtener/Crear y Cachear
                     //      Pasamos 'true' para 'esTamanoNormal' para que se guarde en caché.
                     //      El servicio ya comprueba internamente si existe en caché antes de crearla.
                     servicioMiniaturas.obtenerOCrearMiniatura(
                    		 ruta, claveUnica, anchoNormal, altoNormal, true // <- true para indicar que es tamaño normal
                     );

                     // 3.3. Log (Opcional)
                     // if (miniaturaCacheada != null) {
                     //     // System.out.println("  [Precalentar Cache BG] Miniatura OK para: " + claveUnica);
                     // } else {
                     //     // El servicio ya debería haber logueado el error específico
                     //     // System.err.println("  [Precalentar Cache BG] Falló obtención/creación para: " + claveUnica);
                     // }

                 } catch (Exception e) {
                     // Captura cualquier error inesperado dentro de la tarea submit
                     System.err.println("ERROR INESPERADO [Precalentar Cache BG] Procesando " + ruta + ": " + e.getMessage());
                     e.printStackTrace();
                 }
             }); // Fin lambda tarea individual
             tareasLanzadas++;
         } // Fin bucle for

         // 4. Log Final (Informa que las tareas fueron enviadas)
         System.out.println("[Controller] " + tareasLanzadas + " tareas de pre-calentamiento de caché lanzadas al ExecutorService.");

         // 5. Repintado Inicial Opcional
         //    Repintar la lista de miniaturas por si alguna miniatura ya estaba
         //    en el caché de ejecuciones anteriores y el renderer puede mostrarla ya.
         if (view != null && view.getListaMiniaturas() != null) {
             SwingUtilities.invokeLater(() -> {
                 if (view != null && view.getListaMiniaturas() != null) { // Doble chequeo
                      System.out.println("  -> Solicitando repintado inicial de listaMiniaturas.");
                      view.getListaMiniaturas().repaint();
                 }
             });
         }

     } // --- FIN precalentarCacheMiniaturasAsync ---

     
     /**
      * Actualiza la apariencia visual (típicamente el color de fondo) de un botón
      * que actúa como un 'toggle' (activo/inactivo) para reflejar su estado lógico.
      * Busca el botón correspondiente a la Action dada y aplica los colores
      * de fondo normal o activado definidos en el ThemeManager actual.
      *
      * @param action La instancia de la Action de tipo toggle cuyo estado se quiere reflejar
      *               (p.ej., toggleSubfoldersAction, toggleProporcionesAction). Se usa
      *               para obtener el estado lógico (SELECTED_KEY) y para encontrar el botón
      *               asociado en la vista.
      * @param isSelected El estado lógico explícito que debe reflejar el botón
      *                   (true si debe parecer 'activo' o 'pulsado', false si 'normal').
      *                   Se recomienda pasar el estado actual en lugar de leerlo de la action
      *                   dentro de este método para asegurar la sincronización correcta.
      */
      public void actualizarAspectoBotonToggle(Action action, boolean isSelected) {
         // 1. Validaciones Iniciales de Dependencias
         if (action == null) {
              System.err.println("ERROR [actualizarAspectoBotonToggle]: La Action proporcionada es null.");
              return;
         }
         // Verificar Vista y su mapa de botones
         if (view == null || view.getBotonesPorNombre() == null) {
              System.err.println("ERROR [actualizarAspectoBotonToggle]: Vista o mapa de botones no disponible.");
              // Si la vista no está lista, no podemos actualizarla.
              return;
         }
         // Verificar Gestor de Temas y tema actual
         if (themeManager == null || themeManager.getTemaActual() == null) {
              System.err.println("ERROR [actualizarAspectoBotonToggle]: ThemeManager o Tema actual no disponible.");
              // Podríamos usar colores por defecto como fallback, pero es mejor indicar el error.
              return;
         }
         // Log inicial
         // System.out.println("[actualizarAspectoBotonToggle] Actualizando aspecto para Action: " + action.getValue(Action.NAME) + " a estado: " + (isSelected ? "Activo" : "Normal"));


         // 2. Encontrar el JButton asociado a la Action
         String claveBoton = null;
         JButton botonAsociado = null;

         // 2.1. Método Preferido: Buscar por instancia de Action en el mapa de botones
         for (Map.Entry<String, JButton> entry : view.getBotonesPorNombre().entrySet()) {
             // Comparamos la instancia de la Action asignada al botón con la que recibimos
             if (action.equals(entry.getValue().getAction())) {
                 claveBoton = entry.getKey(); // Guardamos la clave larga (útil para logs)
                 botonAsociado = entry.getValue(); // Guardamos la referencia al botón
                 // System.out.println("  -> Botón encontrado por instancia de Action. Clave: " + claveBoton);
                 break; // Salir del bucle una vez encontrado
             }
         }

         // 2.2. Método Alternativo (Fallback): Buscar usando ActionCommand (menos fiable)
         //      Solo se intenta si no se encontró por instancia.
         if (botonAsociado == null) {
              String actionCommand = (String) action.getValue(Action.ACTION_COMMAND_KEY);
              // Si no hay ActionCommand, intentar con NAME
              if (actionCommand == null) {
                  Object nameValue = action.getValue(Action.NAME);
                  if (nameValue instanceof String) actionCommand = (String) nameValue;
              }

              if (actionCommand != null) {
                   System.out.println("WARN [actualizarAspectoBotonToggle]: No se encontró botón por instancia. Buscando por Comando/Nombre: " + actionCommand);
                   // Intentar encontrar el botón cuya Action tenga este comando/nombre
                   for (Map.Entry<String, JButton> entry : view.getBotonesPorNombre().entrySet()) {
                        Action btnAction = entry.getValue().getAction();
                        if (btnAction != null) {
                             String btnCmd = (String) btnAction.getValue(Action.ACTION_COMMAND_KEY);
                             String btnName = (String) btnAction.getValue(Action.NAME);
                             if (actionCommand.equals(btnCmd) || actionCommand.equals(btnName)) {
                                  claveBoton = entry.getKey();
                                  botonAsociado = entry.getValue();
                                   System.out.println("    -> Botón encontrado por Comando/Nombre. Clave: " + claveBoton);
                                  break;
                             }
                        }
                   }
              } else {
                  // Si no se pudo obtener ni comando ni nombre de la action, no podemos buscar por este método.
                  System.err.println("ERROR [actualizarAspectoBotonToggle]: No se pudo obtener ActionCommand ni Name de la Action proporcionada para buscar botón alternativo.");
              }
         }


         // 3. Aplicar el Cambio de Aspecto si se encontró el botón
         if (botonAsociado != null) {
             Tema temaActual = themeManager.getTemaActual();
             // Obtener los colores específicos para estado normal y activado del tema
             Color colorFondoActivo = temaActual.colorBotonFondoActivado();
             Color colorFondoNormal = temaActual.colorBotonFondo();

             // Establecer el color de fondo del botón según el estado 'isSelected'
             botonAsociado.setBackground(isSelected ? colorFondoActivo : colorFondoNormal);

             // Asegurar que el botón es opaco para que el fondo se vea
             // (Aunque setContentAreaFilled(true) debería ser suficiente, setOpaque(true) no hace daño)
             botonAsociado.setOpaque(true);

             // Log del cambio aplicado
             System.out.println("  -> Aspecto botón '" + (claveBoton != null ? claveBoton : "?") + "' actualizado a: " + (isSelected ? "Activo" : "Normal"));

             // Opcional: Añadir más cambios visuales si se desea (borde, icono diferente, etc.)
             // if (isSelected) {
             //     botonAsociado.setBorder(BorderFactory.createLoweredBevelBorder()); // Ejemplo borde hundido
             // } else {
             //     // Restaurar borde normal (puede ser complejo si el L&F lo gestiona)
             //     // botonAsociado.setBorder(UIManager.getBorder("Button.border"));
             //     botonAsociado.setBorderPainted(false); // O quitar el borde pintado
             // }

         } else {
              // Si no se encontró el botón asociado a la Action
              System.err.println("WARN [actualizarAspectoBotonToggle]: No se encontró el botón asociado a la Action: " + action.getValue(Action.NAME));
         }

         // System.out.println("[actualizarAspectoBotonToggle] Finalizado."); // Log final opcional

      } // --- FIN actualizarAspectoBotonToggle ---
     

      

// ************************************************************************************************************* FIN DE LOGICA     
// ***************************************************************************************************************************
      
// ***************************************************************************************************************************
// ********************************************************************************************************************** ZOOM     

     /**
      * Habilita o deshabilita el modo de zoom manual.
      *
      * Cuando se activa el zoom manual:
      * - Se actualiza el estado en el Modelo.
      * - Se actualiza el estado 'selected' de la Action 'toggleZoomManualAction'.
      * - Se actualiza la apariencia del botón de zoom en la barra de herramientas.
      * - Se habilita la Action 'resetZoomAction'.
      * - Se resetea el estado de zoom/paneo en el modelo (para empezar desde 100% centrado).
      * - Se repinta la imagen principal.
      *
      * Cuando se desactiva el zoom manual:
      * - Se actualiza el estado en el Modelo.
      * - Se actualiza el estado 'selected' de la Action 'toggleZoomManualAction'.
      * - Se actualiza la apariencia del botón de zoom en la barra de herramientas.
      * - Se deshabilita la Action 'resetZoomAction'.
      * - Se resetea el estado de zoom/paneo en el modelo.
      * - Se recalcula la imagen reescalada para ajustarse automáticamente a la vista.
      * - Se repinta la imagen principal.
      *
      * @param activar true para activar el zoom manual, false para desactivarlo.
      */
     public void setManualZoomEnabled(boolean activar) {
         // 1. Log inicio y validación
         System.out.println("[Controller] setManualZoomEnabled llamado con: " + activar);
         if (model == null) {
             System.err.println("ERROR [setManualZoomEnabled]: Modelo es null.");
             return;
         }
         
         // Evitar acciones si el estado ya es el deseado
         if (model.isZoomHabilitado() == activar) {
              System.out.println("  -> El modo zoom manual ya está en el estado solicitado (" + activar + "). No se hacen cambios.");
         
              // Asegurarse de que la UI esté sincronizada por si acaso
              if(toggleZoomManualAction != null) toggleZoomManualAction.putValue(Action.SELECTED_KEY, activar);
              if(resetZoomAction != null) resetZoomAction.setEnabled(activar);
              if(view != null) view.actualizarEstadoControlesZoom(activar, activar);
              return;
         }

         // 2. Actualizar el Modelo
         model.setZoomHabilitado(activar);
         System.out.println("  -> Modelo actualizado: zoomHabilitado = " + model.isZoomHabilitado());

         // 3. Actualizar Estado de Actions Relacionadas

         // 3.1. Action de Toggle (actualizar su estado interno 'SELECTED_KEY')
         if (toggleZoomManualAction != null) {
             toggleZoomManualAction.putValue(Action.SELECTED_KEY, activar);
              System.out.println("  -> Estado Action 'toggleZoomManualAction' (SELECTED_KEY) actualizado a: " + activar);
         
         } else { System.err.println("WARN [setManualZoomEnabled]: toggleZoomManualAction es null."); }

         // 3.2. Action de Reset (habilitar/deshabilitar)
         if (resetZoomAction != null) {
             resetZoomAction.setEnabled(activar);
             System.out.println("  -> Estado Action 'resetZoomAction' (enabled) actualizado a: " + activar);
         
         } else { System.err.println("WARN [setManualZoomEnabled]: resetZoomAction es null."); }

         // 4. Actualizar la Apariencia de la Vista (componentes no controlados directamente por Action)
         if (view != null) {
             // Actualizar el fondo del botón de zoom y el estado enabled del botón/menú de reset
             view.actualizarEstadoControlesZoom(activar, activar);
             System.out.println("  -> Apariencia controles zoom en Vista actualizada.");
             // El estado del JCheckBoxMenuItem del menú debería actualizarse automáticamente
             // porque tiene asignada la 'toggleZoomManualAction' mediante setAction.
         
         } else { System.err.println("WARN [setManualZoomEnabled]: Vista es null, no se pudo actualizar UI."); }

         // 5. Aplicar Lógica Específica según activación/desactivación
         if (activar) {
             
        	 // 5.1. Al ACTIVAR el zoom manual: Resetear estado inicial
             System.out.println("  -> Zoom Manual ACTIVADO. Reseteando estado de zoom/paneo.");
             // Llamar al método que resetea en el modelo Y actualiza la vista
             aplicarResetZoomAction(); // Este ya resetea modelo y repinta vista
         
         } else {
         
        	 // 5.2. Al DESACTIVAR el zoom manual: Resetear y reajustar imagen
             System.out.println("  -> Zoom Manual DESACTIVADO. Reseteando estado y reajustando imagen.");
             model.resetZoomState(); // Resetear zoom/offset en el modelo
             
             // Recalcular imagen para que se ajuste automáticamente (sin zoom/offset manual)
             Image imagenReescalada = reescalarImagenParaAjustar();
             // Actualizar la vista para mostrar la imagen reajustada
             if (view != null) {
                 view.setImagenMostrada(imagenReescalada, model.getZoomFactor(), model.getImageOffsetX(), model.getImageOffsetY());
             }
         }

         System.out.println("[Controller] setManualZoomEnabled finalizado.");
     } // --- FIN setManualZoomEnabled ---

    

     /**
      * Ejecuta la acción de resetear el estado del zoom y el paneo (desplazamiento).
      * 
      * Realiza las siguientes acciones:
      * 1. Llama a model.resetZoomState() para establecer zoomFactor a 1.0 y offsets X/Y a 0.
      * 2. Recalcula la imagen reescalada (que ahora será sin zoom aplicado por el modelo).
      * 3. Actualiza la vista para mostrar la imagen reescalada con los nuevos factores/offsets (1.0, 0, 0).
      * 4. (Opcional) Aplica una animación visual al botón de reset como feedback.
      */
     public void aplicarResetZoomAction() {
         // 1. Log de inicio
         System.out.println("[Controller] Ejecutando acción de Reset Zoom/Pan...");

         // 2. Validar dependencias
         if (model == null || view == null) {
             System.err.println("ERROR [aplicarResetZoomAction]: Modelo o Vista nulos.");
             return;
         }
         
         // 3. Resetear el estado en el Modelo
         model.resetZoomState();
         System.out.println("  -> Estado de zoom/pan reseteado en el Modelo (Zoom: " + model.getZoomFactor() + ", OffsetX: " + model.getImageOffsetX() + ", OffsetY: " + model.getImageOffsetY() + ")");

         // 4. Recalcular la imagen base reescalada (sin el zoom/offset del modelo aplicado aquí)
         //    El método reescalarImagenParaAjustar ya calcula el tamaño base ajustado a la ventana.
         Image imagenReescaladaBase = reescalarImagenParaAjustar();

         // 5. Actualizar la Vista
         //    Pasamos los factores reseteados del modelo (1.0, 0, 0) para que la vista
         //    muestre la imagen reescaladaBase sin zoom ni desplazamiento adicional.
         if (imagenReescaladaBase != null) {
              view.setImagenMostrada(
                  imagenReescaladaBase, 
                  model.getZoomFactor(), // Debería ser 1.0
                  model.getImageOffsetX(), // Debería ser 0
                  model.getImageOffsetY()  // Debería ser 0
              );
              System.out.println("  -> Vista actualizada con imagen reseteada.");
              
         } else {
              // Si no se pudo reescalar (p.ej., no había imagen original), limpiar la vista
              System.err.println("WARN [aplicarResetZoomAction]: No se pudo obtener imagen reescalada base. Limpiando vista.");
              view.limpiarImagenMostrada();
         }


         // 6. Aplicar Animación al Botón (Feedback visual)
         //    Llama al método de la vista que aplica la animación usando el nombre CORTO del ActionCommand del botón.
         if (view != null) { // Comprobar vista de nuevo por si acaso
              view.aplicarAnimacionBoton("Reset_48x48"); // Asume que este es el ActionCommand corto del botón
              System.out.println("  -> Animación aplicada al botón Reset.");
         }
         
         System.out.println("[Controller] Acción Reset Zoom/Pan finalizada.");

     } // --- FIN aplicarResetZoomAction ---
     
     
     /**
      * Comprueba si el modo de zoom manual está actualmente habilitado
      * según el estado almacenado en el VisorModel.
      *
      * Este método es útil principalmente para que las Actions (como ToggleZoomManualAction)
      * puedan consultar el estado actual antes de decidir si deben activar o desactivar el zoom.
      *
      * @return true si el modelo existe y tiene el zoom manual habilitado (isZoomHabilitado() devuelve true),
      *         false en caso contrario (modelo es null o zoom manual está deshabilitado).
      */
     public boolean isZoomManualCurrentlyEnabled() {
         // 1. Verificar que la referencia al modelo no sea nula
         if (model != null) {
             // 2. Devolver el valor actual de la propiedad 'zoomHabilitado' del modelo
             return model.isZoomHabilitado();
         } else {
             // 3. Si el modelo es nulo, asumir que el zoom no está habilitado (estado seguro)
             System.err.println("WARN [isZoomManualCurrentlyEnabled]: Modelo es null. Devolviendo false.");
             return false;
         }
     } // --- FIN isZoomManualCurrentlyEnabled ---
     
     
     /**
      * Calcula una versión reescalada de la imagen principal actual (model.getCurrentImage())
      * para ajustarse a las dimensiones de la etiqueta de visualización (view.getEtiquetaImagen()),
      * respetando la configuración de 'mantener proporción' del modelo.
      *
      * Este método NO modifica el estado, solo calcula y devuelve la imagen escalada.
      * La imagen devuelta es de tipo java.awt.Image (adecuada para ImageIcon/JLabel).
      *
      * @return Una instancia de java.awt.Image reescalada, o null si no hay imagen
      *         original, la vista/etiqueta no están disponibles, o las dimensiones
      *         de destino son inválidas.
      */
     private Image reescalarImagenParaAjustar() {
         // 1. Validar dependencias y estado actual
         if (model == null || view == null || view.getEtiquetaImagen() == null) {
             System.err.println("ERROR [reescalarImagenParaAjustar]: Modelo, Vista o EtiquetaImagen nulos.");
             return null;
         }

         BufferedImage imagenOriginal = model.getCurrentImage();
         if (imagenOriginal == null) {
             // No hay imagen cargada en el modelo, no hay nada que reescalar.
             // System.out.println("[reescalarImagenParaAjustar] No hay imagen original en el modelo.");
             return null;
         }

         // 2. Obtener dimensiones del componente de destino (la etiqueta)
         int anchoDestino = view.getEtiquetaImagen().getWidth();
         int altoDestino = view.getEtiquetaImagen().getHeight();

         // Validar dimensiones de destino
         if (anchoDestino <= 0 || altoDestino <= 0) {
             System.out.println("[reescalarImagenParaAjustar] WARN: Etiqueta sin tamaño válido aún ("+anchoDestino+"x"+altoDestino+"). No se puede escalar.");
             // Si la etiqueta aún no tiene tamaño, no podemos calcular la escala.
             // Devolver null es lo más seguro para evitar errores.
             return null;
         }

         // 3. Determinar dimensiones finales según 'mantener proporción'
         int anchoFinal;
         int altoFinal;
         boolean mantenerProporcion = model.isMantenerProporcion(); // Leer del modelo

         if (mantenerProporcion) {

        	 // 3.1. Calcular manteniendo la proporción
             int anchoOriginal = imagenOriginal.getWidth();
             int altoOriginal = imagenOriginal.getHeight();

             // Evitar división por cero si la imagen original no tiene dimensiones
             if (anchoOriginal <= 0 || altoOriginal <= 0) {
                  System.err.println("ERROR [reescalarImagenParaAjustar]: Imagen original con dimensiones inválidas ("+anchoOriginal+"x"+altoOriginal+").");
                  return null;
             }

             double ratioImagen = (double) anchoOriginal / altoOriginal;
             double ratioDestino = (double) anchoDestino / altoDestino;

             if (ratioDestino > ratioImagen) {
                 // El área de destino es más ancha (proporcionalmente) que la imagen.
                 // Ajustar al alto del destino y calcular el ancho proporcionalmente.
                 altoFinal = altoDestino;
                 anchoFinal = (int) (altoDestino * ratioImagen);
             } else {
                 // El área de destino es más alta (o igual proporción) que la imagen.
                 // Ajustar al ancho del destino y calcular el alto proporcionalmente.
                 anchoFinal = anchoDestino;
                 altoFinal = (int) (anchoDestino / ratioImagen);
             }

         } else {
             // 3.2. No mantener proporción: usar dimensiones de destino directamente
             anchoFinal = anchoDestino;
             altoFinal = altoDestino;
         }

         // 4. Asegurar dimensiones mínimas (1x1)
         anchoFinal = Math.max(1, anchoFinal);
         altoFinal = Math.max(1, altoFinal);

         // 5. Realizar el escalado usando getScaledInstance
         //    Image.SCALE_SMOOTH ofrece mejor calidad que SCALE_DEFAULT a costa de
         //    un poco más de tiempo de procesamiento.
         try {
             // System.out.println("  -> Llamando a getScaledInstance("+anchoFinal+", "+altoFinal+", SCALE_SMOOTH)");
             Image imagenEscalada = imagenOriginal.getScaledInstance(anchoFinal, altoFinal, Image.SCALE_SMOOTH);

             // Comprobar si getScaledInstance devolvió null (raro, pero posible)
             if (imagenEscalada == null) {
                  System.err.println("ERROR [reescalarImagenParaAjustar]: getScaledInstance devolvió null.");
                  return null;
             }

             return imagenEscalada; // Devolver la imagen escalada

         } catch (Exception e) {
              // Capturar cualquier excepción inesperada durante el escalado
              System.err.println("ERROR [reescalarImagenParaAjustar]: Excepción en getScaledInstance: " + e.getMessage());
              e.printStackTrace(); // Útil para depuración
              return null; // Devolver null si el escalado falla
         }
     } // --- FIN reescalarImagenParaAjustar ---


// *************************************************************************************************************** FIN DE ZOOM     
// ***************************************************************************************************************************

// ***************************************************************************************************************************
// ******************************************************************************************************************* ARCHIVO     
     
     
	/**
	 * Abre un diálogo JFileChooser para que el usuario seleccione un directorio.
	 * 
	 * Pasos: 1. Valida que ConfigurationManager no sea null. 2. Crea un
	 * JFileChooser configurado para seleccionar solo directorios. 3. Intenta
	 * establecer el directorio inicial del JFileChooser basado en: a) La
	 * `carpetaRaizActual` si es válida. b) La carpeta guardada en `configuration`
	 * si la anterior no es válida. c) El directorio por defecto del sistema si
	 * ninguna de las anteriores es válida. 4. Muestra el diálogo "Abrir". 5. Si el
	 * usuario aprueba (selecciona una carpeta y pulsa Abrir): a) Obtiene la carpeta
	 * seleccionada (File y Path). b) Verifica que sea un directorio válido. c)
	 * Comprueba si es DIFERENTE de la `carpetaRaizActual`. d) Si es diferente: i.
	 * Actualiza la variable de instancia `carpetaRaizActual`. ii. Actualiza la
	 * configuración en memoria (`configuration.setInicioCarpeta`). iii.Guarda TODA
	 * la configuración actual en el archivo `config.cfg`. iv. Llama a
	 * `cargarListaImagenes(null)` para recargar el contenido desde la nueva
	 * carpeta. e) Si es la misma carpeta, informa al usuario y no hace nada más. f)
	 * Si la selección no es un directorio válido, muestra un mensaje de
	 * advertencia. 6. Si el usuario cancela, simplemente se loguea la cancelación.
	 */
	public void abrirSelectorDeCarpeta ()
	{

		// 1. Validación inicial
		if (configuration == null)
		{
			System.err.println("ERROR [abrirSelectorDeCarpeta]: ConfigurationManager es nulo.");
			// Podríamos mostrar un JOptionPane aquí, pero quizás es mejor solo loguear
			return;
		}
		System.out.println("[Controller] Abriendo selector de carpeta...");

		// 2. Crear y configurar JFileChooser
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Solo permitir seleccionar directorios
		fileChooser.setDialogTitle("Seleccionar Carpeta de Imágenes");
		// Opcional: Configurar otras propiedades como MultiSelection, FileFilters
		// (aunque no aplican aquí), etc.
		// fileChooser.setAcceptAllFileFilterUsed(false); // No mostrar "Todos los
		// archivos"

		// 3. Establecer Directorio Inicial del Selector
		Path directorioInicial = null;

		// Prioridad 1: Usar la carpeta raíz actual si es válida
		if (this.carpetaRaizActual != null && Files.isDirectory(this.carpetaRaizActual))
		{
			directorioInicial = this.carpetaRaizActual;
			System.out.println("  -> JFileChooser se iniciará en carpeta raíz actual: " + directorioInicial);
		} else
		{
			
			// Prioridad 2: Intentar usar la carpeta de la configuración
			String folderInitConfig = configuration.getString("inicio.carpeta", "");

			if (!folderInitConfig.isEmpty())
			{

				try
				{
					Path configPath = Paths.get(folderInitConfig);

					if (Files.isDirectory(configPath))
					{
						directorioInicial = configPath;
						System.out.println(
								"  -> JFileChooser se iniciará en carpeta de config: " + directorioInicial);
					}
			
				} catch (Exception e)
				{
					System.out.println("  -> Ruta en config inválida (" + folderInitConfig
							+ "). Usando directorio por defecto.");
					// No hacer nada, directorioInicial seguirá null
				}
			
			} else
			{
				System.out.println("  -> Sin carpeta en config. Usando directorio por defecto.");
				// directorioInicial sigue null
			}
		}

		// Asignar el directorio inicial si se encontró uno válido
		if (directorioInicial != null)
		{
			fileChooser.setCurrentDirectory(directorioInicial.toFile());
		
		} else
		{
			System.out.println("  -> JFileChooser se iniciará en directorio por defecto del sistema.");
		}

		// 4. Mostrar Diálogo "Abrir"
		// Usar view.getFrame() como padre para centrar el diálogo sobre la ventana
		// principal
		int resultado = fileChooser.showOpenDialog(view != null ? view.getFrame() : null);

		// 5. Procesar Resultado si el Usuario Aprobó
		if (resultado == JFileChooser.APPROVE_OPTION)
		{
			File carpetaSeleccionadaFile = fileChooser.getSelectedFile();

			// 5.a/b: Validar selección
			if (carpetaSeleccionadaFile != null && carpetaSeleccionadaFile.isDirectory())
			{
				Path nuevaCarpetaRaiz = carpetaSeleccionadaFile.toPath();
				String rutaAbsoluta = carpetaSeleccionadaFile.getAbsolutePath();
				System.out.println("  -> Usuario seleccionó carpeta válida: " + nuevaCarpetaRaiz);

				// 5.c: Comprobar si es diferente de la actual
				if (!nuevaCarpetaRaiz.equals(this.carpetaRaizActual))
				{
					System.out.println("    -> La carpeta es DIFERENTE de la actual. Actualizando...");

					// 5.d.i: Actualizar variable de instancia
					this.carpetaRaizActual = nuevaCarpetaRaiz;

					// 5.d.ii: Actualizar configuración en memoria
					configuration.setInicioCarpeta(rutaAbsoluta);

					// 5.d.iii: Guardar configuración completa en archivo
					try
					{
						// Obtener el mapa actual completo para preservar otras configuraciones
						configuration.guardarConfiguracion(configuration.getConfigMap());
						System.out
								.println("      -> Nueva carpeta inicial guardada en config.cfg: " + rutaAbsoluta);
					} catch (IOException e)
					{
						System.err.println(
								"      -> ERROR al guardar nueva carpeta inicial en config.cfg: " + e.getMessage());
						if (view != null)
							JOptionPane.showMessageDialog(view.getFrame(),
									"No se pudo guardar la nueva carpeta inicial en la configuración.",
									"Error de Guardado", JOptionPane.WARNING_MESSAGE);
						// Continuar de todos modos, la carpeta se usará en esta sesión
					}

					// 5.d.iv: Recargar lista de imágenes (pasando null para seleccionar la primera)
					System.out.println("    -> Recargando lista de imágenes desde la nueva carpeta...");
					cargarListaImagenes(null);

				} else
				{
					// 5.e: Carpeta seleccionada es la misma que la actual
					System.out.println(
							"    -> La carpeta seleccionada es la misma que la actual. No se requiere recarga.");
					if (view != null)
						JOptionPane.showMessageDialog(view.getFrame(),
								"La carpeta seleccionada ya es la carpeta actual.", "Información",
								JOptionPane.INFORMATION_MESSAGE);
				}

			} else if (carpetaSeleccionadaFile != null)
			{
				// 5.f: Selección inválida (no es directorio)
				System.err.println("  -> Selección inválida (no es un directorio): "
						+ carpetaSeleccionadaFile.getAbsolutePath());
				if (view != null)
					JOptionPane.showMessageDialog(view.getFrame(), "La selección no es una carpeta válida.",
							"Selección Inválida", JOptionPane.WARNING_MESSAGE);
			} else
			{
				// Caso raro: APPROVE_OPTION pero getSelectedFile() es null
				System.err.println("  -> JFileChooser aprobó pero devolvió archivo nulo.");
			}
		} else
		{
			// 6. Usuario Canceló
			System.out.println("[Controller] Selección de carpeta cancelada por el usuario.");
		}

		System.out.println("[Controller] Fin abrirSelectorDeCarpeta.");

	} // --- FIN abrirSelectorDeCarpeta ---
  

    /**
     * Actualiza el estado lógico y visual para mostrar u ocultar las imágenes de subcarpetas.
     * Guarda el nuevo estado en la configuración ('comportamiento.carpeta.cargarSubcarpetas')
     * y luego recarga la lista de imágenes desde la carpeta raíz actual, intentando
     * mantener la imagen que estaba seleccionada antes del cambio de modo.
     *
     * @param mostrarSubcarpetasDeseado true si se deben buscar y mostrar imágenes en subcarpetas,
     *                                  false si solo se deben mostrar las de la carpeta actual/seleccionada.
     */
    public void setMostrarSubcarpetasAndUpdateConfig(boolean mostrarSubcarpetasDeseado) 
    {
        // 1. Log inicio y validación de dependencias
        System.out.println("\n[Controller setMostrarSubcarpetas] INICIO. Estado deseado (mostrar subcarpetas): " + mostrarSubcarpetasDeseado);
        if (model == null || configuration == null || toggleSubfoldersAction == null || view == null) {
            System.err.println("  -> ERROR: Dependencias nulas (Modelo, Config, Action Subfolders o Vista). Abortando.");
            return;
        }

        // 2. Comprobar si el cambio es realmente necesario
        //    El estado lógico lo determina la Action 'toggleSubfoldersAction'
        boolean estadoLogicoActual = Boolean.TRUE.equals(toggleSubfoldersAction.getValue(Action.SELECTED_KEY));
        System.out.println("  -> Estado lógico actual (Action): " + estadoLogicoActual);
        if (mostrarSubcarpetasDeseado == estadoLogicoActual) {
            System.out.println("  -> Estado deseado ya es el actual. No se realizan cambios (solo se asegura UI).");
            // Asegurar que la UI (radios) esté sincronizada por si acaso
            restaurarSeleccionRadiosSubcarpetas(estadoLogicoActual);
            System.out.println("[Controller setMostrarSubcarpetas] FIN (Sin cambios necesarios).");
            return;
        }

        System.out.println("  -> Aplicando cambio a estado (mostrar subcarpetas): " + mostrarSubcarpetasDeseado);

        // 3. Guardar la clave de la imagen actual ANTES de cualquier cambio
        //    para intentar restaurar la selección después de recargar la lista.
        final String claveAntesDelCambio = model.getSelectedImageKey();
        System.out.println("    -> Clave a intentar mantener: " + claveAntesDelCambio);

        // 4. Actualizar el estado lógico de la Action
        //    Esto centraliza el estado lógico del modo de carga.
        System.out.println("    1. Actualizando Action.SELECTED_KEY...");
        toggleSubfoldersAction.putValue(Action.SELECTED_KEY, mostrarSubcarpetasDeseado);
        // Verificar que cambió (debug)
        // System.out.println("       -> Action.SELECTED_KEY AHORA ES: " + Boolean.TRUE.equals(toggleSubfoldersAction.getValue(Action.SELECTED_KEY)));

        // 5. Actualizar el estado en el Modelo
        //    model.isMostrarSoloCarpetaActual() debe ser lo opuesto a mostrarSubcarpetasDeseado.
        System.out.println("    2. Actualizando Modelo...");
        model.setMostrarSoloCarpetaActual(!mostrarSubcarpetasDeseado);
        // Verificar que cambió (debug)
        // System.out.println("       -> Modelo.isMostrarSoloCarpetaActual() AHORA ES: " + model.isMostrarSoloCarpetaActual());

        // 6. Actualizar la Configuración en Memoria
        System.out.println("    3. Actualizando Configuración en Memoria...");
        configuration.setString("comportamiento.carpeta.cargarSubcarpetas", String.valueOf(mostrarSubcarpetasDeseado));
        // Verificar que cambió (debug)
        // System.out.println("       -> Config 'comportamiento...' AHORA ES: " + configuration.getString("comportamiento.carpeta.cargarSubcarpetas"));
        // Nota: La configuración se guardará al archivo en el ShutdownHook.

        // 7. Sincronizar la Interfaz de Usuario (Botón y Radios del Menú)
        System.out.println("    4. Sincronizando UI...");
        // Actualizar aspecto visual del botón toggle asociado a la acción
        actualizarAspectoBotonToggle(toggleSubfoldersAction, mostrarSubcarpetasDeseado);
        // Actualizar estado 'selected' de los radio buttons del menú
        restaurarSeleccionRadiosSubcarpetas(mostrarSubcarpetasDeseado);

        // 8. Recargar la Lista de Imágenes
        //    Se llama a la versión detallada de cargarListaImagenes, pasando la clave
        //    guardada para intentar mantener la selección. La carga ocurrirá en segundo plano.
        System.out.println("    5. Programando recarga de lista en EDT (manteniendo clave)...");
        SwingUtilities.invokeLater(() -> {
            System.out.println("      -> [EDT] Llamando a cargarListaImagenes(\"" + claveAntesDelCambio + "\") para recargar...");
            // Esta llamada iniciará el SwingWorker con la nueva configuración de profundidad
            cargarListaImagenes(claveAntesDelCambio);
        });

        System.out.println("[Controller setMostrarSubcarpetas] FIN (Cambio aplicado y recarga programada).");
    } // --- FIN setMostrarSubcarpetasAndUpdateConfig ---
    

    /**
     * Asegura que los JRadioButtonMenuItem del menú correspondientes a la
     * configuración de carga de subcarpetas reflejen visualmente el estado lógico
     * proporcionado (marcando el correcto como seleccionado).
     *
     * Es seguro llamar a setSelected() en los radios aquí porque estos componentes
     * específicos usan un ActionListener personalizado en lugar de setAction() para
     * evitar bucles de eventos.
     *
     * @param mostrarSubcarpetas El estado lógico actual. Si es true, se seleccionará
     *                           el radio "Mostrar Imágenes de Subcarpetas"; si es false,
     *                           se seleccionará "Mostrar Solo Carpeta Actual".
     */
    private void restaurarSeleccionRadiosSubcarpetas(boolean mostrarSubcarpetas) {
        // 1. Validar que la vista y el mapa de menús existan
         if (view == null || view.getMenuItemsPorNombre() == null) {
              System.err.println("WARN [restaurarSeleccionRadiosSubcarpetas]: Vista o mapa de menús nulos.");
              return; // No se puede hacer nada si no hay menús
         }
         Map<String, JMenuItem> menuItems = view.getMenuItemsPorNombre();

         // 2. Log del estado deseado
         System.out.println("  [Controller] Sincronizando estado visual de Radios Subcarpetas a: " + (mostrarSubcarpetas ? "Mostrar Subcarpetas" : "Mostrar Solo Carpeta"));

         // 3. Obtener las referencias a los JRadioButtonMenuItems específicos
         //    Usar las claves largas definidas en la configuración y usadas por MenuBarBuilder.
         JMenuItem radioMostrarSub = menuItems.get("interfaz.menu.configuracion.carga_de_imagenes.Mostrar_Imagenes_de_Subcarpetas");
         JMenuItem radioMostrarSolo = menuItems.get("interfaz.menu.configuracion.carga_de_imagenes.Mostrar_Solo_Carpeta_Actual");

         // 4. Aplicar el estado 'selected' al radio correcto
         //    Se hace de forma segura llamando a setSelected directamente.

         // 4.1. Configurar el radio "Mostrar Subcarpetas"
         if (radioMostrarSub instanceof JRadioButtonMenuItem) {
             JRadioButtonMenuItem radioSub = (JRadioButtonMenuItem) radioMostrarSub;
             // Solo llamar a setSelected si el estado actual es diferente al deseado
             // para evitar eventos innecesarios del ButtonGroup (aunque no debería causar problemas graves).
             if (radioSub.isSelected() != mostrarSubcarpetas) {
                  // System.out.println("    -> Estableciendo 'Mostrar Subcarpetas' a: " + mostrarSubcarpetas); // Log detallado opcional
                  radioSub.setSelected(mostrarSubcarpetas);
             }
             // Asegurar que esté habilitado (podría haberse deshabilitado por error)
             radioSub.setEnabled(true);
         } else if (radioMostrarSub != null) {
              System.err.println("WARN [restaurarSeleccionRadios]: Item 'Mostrar_Imagenes_de_Subcarpetas' no es un JRadioButtonMenuItem.");
         } else {
              System.err.println("WARN [restaurarSeleccionRadios]: Item 'Mostrar_Imagenes_de_Subcarpetas' no encontrado.");
         }


         // 4.2. Configurar el radio "Mostrar Solo Carpeta Actual" (estado inverso)
         if (radioMostrarSolo instanceof JRadioButtonMenuItem) {
             JRadioButtonMenuItem radioSolo = (JRadioButtonMenuItem) radioMostrarSolo;
             // El estado seleccionado de este debe ser el opuesto a mostrarSubcarpetas
             boolean estadoDeseadoSolo = !mostrarSubcarpetas;
             if (radioSolo.isSelected() != estadoDeseadoSolo) {
                  // System.out.println("    -> Estableciendo 'Mostrar Solo Carpeta' a: " + estadoDeseadoSolo); // Log detallado opcional
                  radioSolo.setSelected(estadoDeseadoSolo);
             }
             // Asegurar que esté habilitado
             radioSolo.setEnabled(true);
         } else if (radioMostrarSolo != null) {
              System.err.println("WARN [restaurarSeleccionRadios]: Item 'Mostrar_Solo_Carpeta_Actual' no es un JRadioButtonMenuItem.");
         } else {
              System.err.println("WARN [restaurarSeleccionRadios]: Item 'Mostrar_Solo_Carpeta_Actual' no encontrado.");
         }

         // 5. Log final
         System.out.println("  [Controller] Estado visual de Radios Subcarpetas sincronizado.");

    } // --- FIN restaurarSeleccionRadiosSubcarpetas ---

    
	
// ************************************************************************************************************ FIN DE ARCHIVO
// ***************************************************************************************************************************

// ***************************************************************************************************************************
// ******************************************************************************************************************* EDICION	
	

   /**
    * Aplica un volteo horizontal a la imagen principal actualmente mostrada.
    * 
    * Pasos:
    * 1. Valida que el modelo y la vista existan.
    * 2. Obtiene la imagen original (BufferedImage) del modelo.
    * 3. Si no hay imagen, no hace nada.
    * 4. Llama al método estático ImageEdition.flipHorizontal() para realizar el volteo.
    * 5. Si el volteo es exitoso (devuelve una nueva imagen):
    *    a) Actualiza la imagen en el modelo (model.setCurrentImage).
    *    b) Recalcula la imagen reescalada para la vista.
    *    c) Actualiza la vista para mostrar la imagen volteada (view.setImagenMostrada).
    *    d) (Opcional) Aplica una animación al botón correspondiente.
    * 6. Si el volteo falla (ImageEdition devuelve null), muestra un mensaje de error.
    */
	public void aplicarVolteoHorizontal() {
	// 1. Validar dependencias
       if (model == null || view == null) {
           System.err.println("ERROR [aplicarVolteoHorizontal]: Modelo o Vista nulos.");
           return; // Salir si no tenemos lo necesario
       }
       // 2. Obtener imagen actual del modelo
       BufferedImage imagenOriginal = model.getCurrentImage();

       // 3. Validar si hay imagen para voltear
       if (imagenOriginal == null) {
           System.out.println("[aplicarVolteoHorizontal] No hay imagen cargada para voltear.");
           // Opcional: Mostrar un mensaje al usuario
           // JOptionPane.showMessageDialog(view.getFrame(), "No hay imagen cargada para voltear.", "Acción no disponible", JOptionPane.INFORMATION_MESSAGE);
           return;
       }

       System.out.println("[Volteo H] Solicitando volteo horizontal a ImageEdition...");

       // 4. Llamar al servicio/utilidad de edición de imágenes
       BufferedImage imagenVolteada = null;
       try {
           imagenVolteada = ImageEdition.flipHorizontal(imagenOriginal);
       } catch (Exception e) {
           // Capturar cualquier excepción inesperada durante el volteo
           System.err.println("ERROR [aplicarVolteoHorizontal] Excepción en ImageEdition.flipHorizontal: " + e.getMessage());
           e.printStackTrace();
           JOptionPane.showMessageDialog(view.getFrame(),
                                         "Ocurrió un error inesperado al intentar voltear la imagen.",
                                         "Error de Edición", JOptionPane.ERROR_MESSAGE);
           return; // Salir si hay error en la edición
       }


       // 5. Procesar el resultado del volteo
       if (imagenVolteada != null) {
           // 5.a. Actualizar Modelo
           System.out.println("  -> Volteo exitoso. Actualizando modelo...");
           model.setCurrentImage(imagenVolteada); // Reemplazar la imagen en el modelo

           // 5.b. Recalcular vista reescalada
           // Es importante recalcular porque la imagen base ha cambiado
           System.out.println("  -> Recalculando imagen para la vista...");
           Image imagenReescaladaParaVista = reescalarImagenParaAjustar();

           // 5.c. Actualizar Vista
           if (view != null && imagenReescaladaParaVista != null) { // Doble chequeo
                // Mostrar la imagen volteada y reescalada, manteniendo zoom/pan actual
               view.setImagenMostrada(imagenReescaladaParaVista,
                                      model.getZoomFactor(),
                                      model.getImageOffsetX(),
                                      model.getImageOffsetY());
                System.out.println("  -> Vista actualizada con imagen volteada.");

                // 5.d. Aplicar animación al botón (feedback)
                view.aplicarAnimacionBoton("Espejo_Horizontal_48x48"); // Usar el ActionCommand CORTO del botón

           } else if (view == null) {
               System.err.println("WARN [aplicarVolteoHorizontal]: Vista es null, no se pudo actualizar UI.");
           } else { // imagenReescaladaParaVista es null
                System.err.println("WARN [aplicarVolteoHorizontal]: No se pudo reescalar la imagen volteada. Limpiando vista.");
                view.limpiarImagenMostrada();
           }

           System.out.println("[Volteo H] Volteo horizontal aplicado correctamente.");

       } else {
           // 6. Manejar fallo del volteo (si ImageEdition devolvió null)
           System.err.println("ERROR [aplicarVolteoHorizontal]: ImageEdition.flipHorizontal devolvió null.");
           JOptionPane.showMessageDialog(view.getFrame(),
                                         "No se pudo realizar el volteo horizontal de la imagen.",
                                         "Error de Volteo", JOptionPane.ERROR_MESSAGE);
       }
	} // --- FIN aplicarVolteoHorizontal ---
       

    /**
     * Aplica un volteo vertical a la imagen principal actualmente mostrada.
     * 
     * Pasos:
     * 1. Valida que el modelo y la vista existan.
     * 2. Obtiene la imagen original (BufferedImage) del modelo.
     * 3. Si no hay imagen, no hace nada.
     * 4. Llama al método estático ImageEdition.flipVertical() para realizar el volteo.
     * 5. Si el volteo es exitoso (devuelve una nueva imagen):
     *    a) Actualiza la imagen en el modelo (model.setCurrentImage).
     *    b) Recalcula la imagen reescalada para la vista.
     *    c) Actualiza la vista para mostrar la imagen volteada (view.setImagenMostrada).
     *    d) (Opcional) Aplica una animación al botón correspondiente.
     * 6. Si el volteo falla (ImageEdition devuelve null), muestra un mensaje de error.
     */
    public void aplicarVolteoVertical() {
        // 1. Validar dependencias
        if (model == null || view == null) {
            System.err.println("ERROR [aplicarVolteoVertical]: Modelo o Vista nulos.");
            return;
        }
        // 2. Obtener imagen actual del modelo
        BufferedImage imagenOriginal = model.getCurrentImage();

        // 3. Validar si hay imagen para voltear
        if (imagenOriginal == null) {
            System.out.println("[aplicarVolteoVertical] No hay imagen cargada para voltear.");
            return;
        }

        System.out.println("[Volteo V] Solicitando volteo vertical a ImageEdition...");

        // 4. Llamar al servicio/utilidad de edición de imágenes
        BufferedImage imagenVolteada = null;
        try {
            // Llamar al método específico para volteo vertical
            imagenVolteada = ImageEdition.flipVertical(imagenOriginal); 
        } catch (Exception e) {
            System.err.println("ERROR [aplicarVolteoVertical] Excepción en ImageEdition.flipVertical: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(view.getFrame(),
                                          "Ocurrió un error inesperado al intentar voltear la imagen verticalmente.",
                                          "Error de Edición", JOptionPane.ERROR_MESSAGE);
            return; 
        }

        // 5. Procesar el resultado del volteo
        if (imagenVolteada != null) {
        	
            // 5.a. Actualizar Modelo
            System.out.println("  -> Volteo vertical exitoso. Actualizando modelo...");
            model.setCurrentImage(imagenVolteada); // Reemplazar imagen en modelo

            // 5.b. Recalcular vista reescalada
            System.out.println("  -> Recalculando imagen para la vista...");
            Image imagenReescaladaParaVista = reescalarImagenParaAjustar();

            // 5.c. Actualizar Vista
            if (view != null && imagenReescaladaParaVista != null) { 
                view.setImagenMostrada(imagenReescaladaParaVista,
                                       model.getZoomFactor(),
                                       model.getImageOffsetX(),
                                       model.getImageOffsetY());
                 System.out.println("  -> Vista actualizada con imagen volteada verticalmente.");

                 // 5.d. Aplicar animación al botón (usar el ActionCommand corto correcto)
                 view.aplicarAnimacionBoton("Espejo_Vertical_48x48"); 

            } else if (view == null) {
                System.err.println("WARN [aplicarVolteoVertical]: Vista es null, no se pudo actualizar UI.");
            
            } else { 
                 System.err.println("WARN [aplicarVolteoVertical]: No se pudo reescalar la imagen volteada. Limpiando vista.");
                 view.limpiarImagenMostrada();
            }

            System.out.println("[Volteo V] Volteo vertical aplicado correctamente.");

        } else {
            // 6. Manejar fallo del volteo
            System.err.println("ERROR [aplicarVolteoVertical]: ImageEdition.flipVertical devolvió null.");
            JOptionPane.showMessageDialog(view.getFrame(),
                                          "No se pudo realizar el volteo vertical de la imagen.",
                                          "Error de Volteo", JOptionPane.ERROR_MESSAGE);
        }
    } // --- FIN aplicarVolteoVertical ---
        

     /**
      * Aplica una rotación de 90 grados hacia la izquierda a la imagen principal
      * actualmente mostrada.
      *
      * Pasos:
      * 1. Valida que el modelo y la vista existan.
      * 2. Obtiene la imagen original (BufferedImage) del modelo.
      * 3. Si no hay imagen, no hace nada.
      * 4. Llama al método estático ImageEdition.rotateLeft() para realizar la rotación.
      * 5. Si la rotación es exitosa (devuelve una nueva imagen):
      *    a) Actualiza la imagen en el modelo (model.setCurrentImage).
      *    b) **Importante:** Resetea el estado de zoom/paneo en el modelo, ya que
      *       las dimensiones de la imagen (ancho/alto) cambian con la rotación.
      *    c) Recalcula la imagen reescalada para la vista (se ajustará a las nuevas dimensiones).
      *    d) Actualiza la vista para mostrar la imagen rotada (view.setImagenMostrada) con zoom/pan reseteados.
      *    e) (Opcional) Aplica una animación al botón correspondiente.
      * 6. Si la rotación falla (ImageEdition devuelve null), muestra un mensaje de error.
      */
     public void aplicarRotarIzquierda() {
         // 1. Validar dependencias
         if (model == null || view == null) {
             System.err.println("ERROR [aplicarRotarIzquierda]: Modelo o Vista nulos.");
             return;
         }
         
         // 2. Obtener imagen actual del modelo
         BufferedImage imagenOriginal = model.getCurrentImage();

         // 3. Validar si hay imagen para rotar
         if (imagenOriginal == null) {
             System.out.println("[aplicarRotarIzquierda] No hay imagen cargada para rotar.");
             return;
         }

         System.out.println("[Rotar Izq] Solicitando rotación izquierda a ImageEdition...");

         // 4. Llamar al servicio/utilidad de edición de imágenes
         BufferedImage imagenRotada = null;
         try {
             // Llamar al método específico para rotación izquierda
             imagenRotada = ImageEdition.rotateLeft(imagenOriginal);
         
         } catch (Exception e) {
             System.err.println("ERROR [aplicarRotarIzquierda] Excepción en ImageEdition.rotateLeft: " + e.getMessage());
             e.printStackTrace();
             JOptionPane.showMessageDialog(view.getFrame(),
                                           "Ocurrió un error inesperado al intentar rotar la imagen.",
                                           "Error de Edición", JOptionPane.ERROR_MESSAGE);
             return;
         }

         // 5. Procesar el resultado de la rotación
         if (imagenRotada != null) {
             // 5.a. Actualizar Modelo
             System.out.println("  -> Rotación izquierda exitosa. Actualizando modelo...");
             model.setCurrentImage(imagenRotada); // Reemplazar imagen en modelo

             // 5.b. Resetear Zoom/Pan (¡CRUCIAL por cambio de dimensiones!)
             System.out.println("  -> Reseteando zoom/pan debido a cambio de dimensiones por rotación.");
             model.resetZoomState();

             // 5.c. Recalcular vista reescalada (se adaptará a las nuevas dimensiones)
             System.out.println("  -> Recalculando imagen para la vista...");
             Image imagenReescaladaParaVista = reescalarImagenParaAjustar();

             // 5.d. Actualizar Vista (con zoom/pan reseteados)
             if (view != null && imagenReescaladaParaVista != null) {
                 view.setImagenMostrada(imagenReescaladaParaVista,
                                        model.getZoomFactor(),  // Será 1.0
                                        model.getImageOffsetX(), // Será 0
                                        model.getImageOffsetY()); // Será 0
                  System.out.println("  -> Vista actualizada con imagen rotada.");

                  // 5.e. Aplicar animación al botón (usar el ActionCommand corto correcto)
                  view.aplicarAnimacionBoton("Rotar_Izquierda_48x48");

             } else if (view == null) {
                 System.err.println("WARN [aplicarRotarIzquierda]: Vista es null, no se pudo actualizar UI.");
             
             } else {
                  System.err.println("WARN [aplicarRotarIzquierda]: No se pudo reescalar la imagen rotada. Limpiando vista.");
                  view.limpiarImagenMostrada();
             }

             System.out.println("[Rotar Izq] Rotación izquierda aplicada correctamente.");

         } else {
             // 6. Manejar fallo de la rotación
             System.err.println("ERROR [aplicarRotarIzquierda]: ImageEdition.rotateLeft devolvió null.");
             JOptionPane.showMessageDialog(view.getFrame(),
                                           "No se pudo realizar la rotación izquierda de la imagen.",
                                           "Error de Rotación", JOptionPane.ERROR_MESSAGE);
         }
     } // --- FIN aplicarRotarIzquierda ---
     
     
     /**
      * Aplica una rotación de 90 grados hacia la derecha a la imagen principal
      * actualmente mostrada.
      *
      * Pasos:
      * 1. Valida que el modelo y la vista existan.
      * 2. Obtiene la imagen original (BufferedImage) del modelo.
      * 3. Si no hay imagen, no hace nada.
      * 4. Llama al método estático ImageEdition.rotateRight() para realizar la rotación.
      * 5. Si la rotación es exitosa (devuelve una nueva imagen):
      *    a) Actualiza la imagen en el modelo (model.setCurrentImage).
      *    b) **Importante:** Resetea el estado de zoom/paneo en el modelo.
      *    c) Recalcula la imagen reescalada para la vista.
      *    d) Actualiza la vista para mostrar la imagen rotada (view.setImagenMostrada) con zoom/pan reseteados.
      *    e) (Opcional) Aplica una animación al botón correspondiente.
      * 6. Si la rotación falla (ImageEdition devuelve null), muestra un mensaje de error.
      */
     public void aplicarRotarDerecha() {
         // 1. Validar dependencias
         if (model == null || view == null) {
             System.err.println("ERROR [aplicarRotarDerecha]: Modelo o Vista nulos.");
             return;
         }
         // 2. Obtener imagen actual del modelo
         BufferedImage imagenOriginal = model.getCurrentImage();

         // 3. Validar si hay imagen para rotar
         if (imagenOriginal == null) {
             System.out.println("[aplicarRotarDerecha] No hay imagen cargada para rotar.");
             return;
         }

         System.out.println("[Rotar Der] Solicitando rotación derecha a ImageEdition...");

         // 4. Llamar al servicio/utilidad de edición de imágenes
         BufferedImage imagenRotada = null;
         try {
             // Llamar al método específico para rotación derecha
             imagenRotada = ImageEdition.rotateRight(imagenOriginal);
         } catch (Exception e) {
             System.err.println("ERROR [aplicarRotarDerecha] Excepción en ImageEdition.rotateRight: " + e.getMessage());
             e.printStackTrace();
             JOptionPane.showMessageDialog(view.getFrame(),
                                           "Ocurrió un error inesperado al intentar rotar la imagen.",
                                           "Error de Edición", JOptionPane.ERROR_MESSAGE);
             return;
         }

         // 5. Procesar el resultado de la rotación
         if (imagenRotada != null) {
             
        	 // 5.a. Actualizar Modelo
             System.out.println("  -> Rotación derecha exitosa. Actualizando modelo...");
             model.setCurrentImage(imagenRotada); // Reemplazar imagen en modelo

             // 5.b. Resetear Zoom/Pan (¡CRUCIAL!)
             System.out.println("  -> Reseteando zoom/pan debido a cambio de dimensiones por rotación.");
             model.resetZoomState();

             // 5.c. Recalcular vista reescalada
             System.out.println("  -> Recalculando imagen para la vista...");
             Image imagenReescaladaParaVista = reescalarImagenParaAjustar();

             // 5.d. Actualizar Vista (con zoom/pan reseteados)
             if (view != null && imagenReescaladaParaVista != null) {
                 view.setImagenMostrada(imagenReescaladaParaVista,
                                        model.getZoomFactor(),  // 1.0
                                        model.getImageOffsetX(), // 0
                                        model.getImageOffsetY()); // 0
                  System.out.println("  -> Vista actualizada con imagen rotada.");

                  // 5.e. Aplicar animación al botón
                  view.aplicarAnimacionBoton("Rotar_Derecha_48x48"); // Usar ActionCommand corto

             } else if (view == null) {
                 System.err.println("WARN [aplicarRotarDerecha]: Vista es null, no se pudo actualizar UI.");
             } else {
                  System.err.println("WARN [aplicarRotarDerecha]: No se pudo reescalar la imagen rotada. Limpiando vista.");
                  view.limpiarImagenMostrada();
             }

             System.out.println("[Rotar Der] Rotación derecha aplicada correctamente.");

         } else {
             
        	 // 6. Manejar fallo de la rotación
             System.err.println("ERROR [aplicarRotarDerecha]: ImageEdition.rotateRight devolvió null.");
             JOptionPane.showMessageDialog(view.getFrame(),
                                           "No se pudo realizar la rotación derecha de la imagen.",
                                           "Error de Rotación", JOptionPane.ERROR_MESSAGE);
         }
     } // --- FIN aplicarRotarDerecha ---
     
     
// ************************************************************************************************************ FIN DE EDICION     
// ***************************************************************************************************************************

     

     /**
      * Actualiza el estado lógico y visual para mantener (o no) las proporciones
      * de la imagen al reescalarla para ajustarse a la vista.
      * Guarda el nuevo estado en la configuración ('interfaz.menu.zoom.Mantener_Proporciones.seleccionado'),
      * actualiza la Action 'toggleProporcionesAction', actualiza el estado en el Modelo,
      * sincroniza la apariencia del botón toggle asociado y finalmente repinta la imagen principal
      * para que refleje el nuevo modo de escalado.
      *
      * @param mantener True si se deben mantener las proporciones originales de la imagen,
      *                 false si se debe estirar/encoger para rellenar el área de visualización.
      */
     public void setMantenerProporcionesAndUpdateConfig(boolean mantener) {
         // 1. Log inicio y validación de dependencias
         System.out.println("\n[Controller setMantenerProporciones] INICIO. Estado deseado (mantener): " + mantener);
         if (model == null || configuration == null || toggleProporcionesAction == null || view == null) {
             System.err.println("  -> ERROR: Dependencias nulas (Modelo, Config, Action Proporciones o Vista). Abortando.");
             return;
         }

         // 2. Comprobar si el cambio es realmente necesario
         //    Comparamos el estado deseado con el estado actual de la Action.
         boolean estadoActualAction = Boolean.TRUE.equals(toggleProporcionesAction.getValue(Action.SELECTED_KEY));
         System.out.println("  -> Estado Lógico Actual (Action.SELECTED_KEY): " + estadoActualAction);
         if (mantener == estadoActualAction) {
             System.out.println("  -> Estado deseado ya es el actual. No se realizan cambios.");
             // Opcional: asegurar que la UI del botón esté sincronizada por si acaso
             // actualizarAspectoBotonToggle(toggleProporcionesAction, estadoActualAction);
             System.out.println("[Controller setMantenerProporciones] FIN (Sin cambios necesarios).");
             return;
         }

         System.out.println("  -> Aplicando cambio a estado (mantener proporciones): " + mantener);

         // 3. Actualizar el estado lógico de la Action ('SELECTED_KEY')
         //    Esto permite que los componentes asociados (JCheckBoxMenuItem) se actualicen.
         System.out.println("    1. Actualizando Action.SELECTED_KEY...");
         toggleProporcionesAction.putValue(Action.SELECTED_KEY, mantener);
         // System.out.println("       -> Action.SELECTED_KEY AHORA ES: " + Boolean.TRUE.equals(toggleProporcionesAction.getValue(Action.SELECTED_KEY)));

         // 4. Actualizar el estado en el Modelo
         System.out.println("    2. Actualizando Modelo...");
         model.setMantenerProporcion(mantener); // Llama al setter específico en el modelo
         // El modelo imprime su propio log al cambiar

         // 5. Actualizar la Configuración en Memoria
         System.out.println("    3. Actualizando Configuración en Memoria...");
         String configKey = "interfaz.menu.zoom.Mantener_Proporciones.seleccionado";
         configuration.setString(configKey, String.valueOf(mantener));
         // System.out.println("       -> Config '" + configKey + "' AHORA ES: " + configuration.getString(configKey));
         // Se guardará al archivo en el ShutdownHook

         // 6. Sincronizar la Interfaz de Usuario (Botón Toggle)
         //    El JCheckBoxMenuItem se actualiza automáticamente por la Action.
         System.out.println("    4. Sincronizando UI (Botón)...");
         actualizarAspectoBotonToggle(toggleProporcionesAction, mantener); // Actualiza color/apariencia

         // 7. Repintar la Imagen Principal para aplicar el nuevo modo de escalado
         //    Llamamos a reescalarImagenParaAjustar que ahora usará el nuevo valor
         //    de model.isMantenerProporcion() y luego actualizamos la vista.
         System.out.println("    5. Programando repintado de imagen principal en EDT...");
         SwingUtilities.invokeLater(() -> {
             // Verificar que la vista y el modelo sigan disponibles
             if (view == null || model == null) {
                 System.err.println("ERROR [EDT Repintar Proporciones]: Vista o Modelo nulos.");
                 return;
             }
             System.out.println("      -> [EDT] Llamando a reescalar y mostrar imagen con nueva proporción...");
             Image imagenReescalada = reescalarImagenParaAjustar(); // Recalcula la imagen
             // Mostrar la imagen reescalada con el zoom/offset actual
             view.setImagenMostrada(imagenReescalada, model.getZoomFactor(), model.getImageOffsetX(), model.getImageOffsetY());
         });

         System.out.println("[Controller setMantenerProporciones] FIN (Cambio aplicado y repintado programado).");
     } // --- FIN setMantenerProporcionesAndUpdateConfig ---
     

     /**
      * Actualiza la visibilidad de un componente principal de la interfaz
      * (Barra de Menú, Barra de Botones, Lista de Archivos, Panel de Miniaturas, Barra de Estado, Fondo a Cuadros)
      * y guarda el nuevo estado de visibilidad en la configuración en memoria.
      * La clave de configuración se asume que termina en ".seleccionado" para guardar el estado booleano.
      *
      * @param nombreComponente Identificador del componente (generalmente el texto del JCheckBoxMenuItem
      *                         o un identificador interno consistente). Debe coincidir con las claves
      *                         usadas en el switch interno.
      * @param visible          El nuevo estado de visibilidad deseado (true para mostrar, false para ocultar).
      */
     public void setComponenteVisibleAndUpdateConfig(String nombreComponente, boolean visible) {
         // 1. Validar dependencias
         if (view == null || configuration == null) {
             System.err.println("ERROR [setComponenteVisible]: Vista o Configuración nulos.");
             return;
         }
         // Log inicial
         System.out.println("[Controller] Solicitud para cambiar visibilidad de '" + nombreComponente + "' a: " + visible);

         // 2. Determinar la clave base de configuración y actualizar la vista
         String configKeyBase = null; // Clave sin el sufijo ".seleccionado"
         boolean cambioRealizado = false; // Flag para saber si la visibilidad realmente cambió

         switch (nombreComponente) {
             case "Barra_de_Menu":
                 configKeyBase = "interfaz.menu.vista.Barra_de_Menu";
                 // Comprobar si el estado actual es diferente antes de cambiar
                  if (view.getJMenuBar() != null && view.getJMenuBar().isVisible() != visible) {
                      view.setJMenuBarVisible(visible); // Método específico en VisorView
                      cambioRealizado = true;
                  }
                 break;
             case "Barra_de_Botones":
                 configKeyBase = "interfaz.menu.vista.Barra_de_Botones";
                  if (view.getPanelDeBotones() != null && view.getPanelDeBotones().isVisible() != visible) { // Necesita getter para panelDeBotones
                     view.setToolBarVisible(visible); // Método específico en VisorView
                     cambioRealizado = true;
                  }
                 break;
             case "Mostrar/Ocultar_la_Lista_de_Archivos": // Clave del menú
                 configKeyBase = "interfaz.menu.vista.Mostrar/Ocultar_la_Lista_de_Archivos";
                  if (view.getPanelIzquierdo() != null && view.getPanelIzquierdo().isVisible() != visible) { // Necesita getter para panelIzquierdo
                     view.setFileListVisible(visible); // Método específico en VisorView
                     cambioRealizado = true;
                  }
                 break;
             case "Imagenes_en_Miniatura": // Clave del menú
                 configKeyBase = "interfaz.menu.vista.Imagenes_en_Miniatura";
                  if (view.getScrollListaMiniaturas() != null && view.getScrollListaMiniaturas().isVisible() != visible) { // Usa el ScrollPane
                     view.setThumbnailsVisible(visible); // Método específico en VisorView
                     cambioRealizado = true;
                  }
                 break;
              case "Linea_de_Ubicacion_del_Archivo": // Clave del menú
                  configKeyBase = "interfaz.menu.vista.Linea_de_Ubicacion_del_Archivo";
                   if (view.getTextoRuta() != null && view.getTextoRuta().isVisible() != visible) { // Necesita getter para textoRuta
                      view.setLocationBarVisible(visible); // Método específico en VisorView
                      cambioRealizado = true;
                   }
                  break;
              case "Fondo_a_Cuadros": // Clave del menú
                  configKeyBase = "interfaz.menu.vista.Fondo_a_Cuadros";
                   cambioRealizado = true; // Asumir que siempre se guarda el estado
                  System.out.println("  -> Estado Fondo a Cuadros actualizado a: " + visible);
                  break;
              case "Mantener_Ventana_Siempre_Encima": // Clave del menú
                   configKeyBase = "interfaz.menu.vista.Mantener_Ventana_Siempre_Encima";
                    if (view.isAlwaysOnTop() != visible) {
                        view.setAlwaysOnTop(visible); // Método estándar de JFrame/Window
                        cambioRealizado = true;
                    }
                   break;
             default:
                 System.err.println("WARN [setComponenteVisible]: Nombre de componente no reconocido: '" + nombreComponente + "'. No se cambió visibilidad ni configuración.");
                 return; // Salir si no se reconoce el componente
         }

         // 3. Actualizar configuración en memoria SOLO si hubo un cambio o es Fondo a Cuadros
         if (configKeyBase != null && (cambioRealizado || nombreComponente.equals("Fondo_a_Cuadros"))) {
             String fullConfigKey = configKeyBase + ".seleccionado";
             // Usamos el valor 'visible' que se pasó al método
             configuration.setString(fullConfigKey, String.valueOf(visible));
             System.out.println("  -> Configuración en memoria actualizada: " + fullConfigKey + " = " + visible);
         } else if (configKeyBase != null) {
              System.out.println("  -> Visibilidad no cambió para '" + nombreComponente + "'. No se actualizó configuración.");
         }

         // 4. Revalidar y Repintar el Frame Principal si hubo cambio visual
         //    (No necesario para 'Fondo a Cuadros' si solo cambia el pintado interno,
         //     ni para 'AlwaysOnTop' que es una propiedad de ventana).
         if (cambioRealizado && !nombreComponente.equals("Fondo_a_Cuadros") && !nombreComponente.equals("Mantener_Ventana_Siempre_Encima")) {
              System.out.println("  -> Revalidando y repintando el Frame...");
              // Es más seguro hacerlo en invokeLater por si acaso
              SwingUtilities.invokeLater(() -> {
                   if (view != null && view.getFrame() != null) { // Chequear view y frame
                       view.getFrame().revalidate();
                       view.getFrame().repaint();
                   }
              });
         }

         System.out.println("[Controller] Fin setComponenteVisibleAndUpdateConfig para '" + nombreComponente + "'.");
     } // --- FIN setComponenteVisibleAndUpdateConfig ---
     
         
      
    
      /**
       * Cambia el tema visual actual de la aplicación.
       * 
       * Pasos:
       * 1. Valida las dependencias (ThemeManager) y el nombre del nuevo tema.
       * 2. Llama a themeManager.setTemaActual() para:
       *    a) Cambiar el objeto Tema actual en memoria.
       *    b) Actualizar la clave 'tema.nombre' en el ConfigurationManager en memoria.
       * 3. Si el tema realmente cambió (setTemaActual devuelve true):
       *    a) Itera sobre la lista 'themeActions' (que contiene las Actions de los radios del menú de tema).
       *    b) Llama a actualizarEstadoSeleccion() en cada ToggleThemeAction para que
       *       el radio button correspondiente al nuevo tema quede marcado como seleccionado.
       *    c) Muestra un JOptionPane informando al usuario que el cambio de tema
       *       requiere reiniciar la aplicación para que los cambios visuales (colores, etc.)
       *       tengan efecto completo, ya que muchos colores se aplican durante la inicialización
       *       de la UI.
       *
       * Nota: Este método NO guarda la configuración en el archivo inmediatamente. El guardado
       * ocurrirá a través del ShutdownHook al cerrar la aplicación.
       *
       * @param nuevoTema El nombre interno del nuevo tema a aplicar (ej. "dark", "clear", "blue").
       */
      public void cambiarTemaYNotificar(String nuevoTema) {
          // 1. Validaciones iniciales
          if (themeManager == null) {
              System.err.println("ERROR [cambiarTema]: ThemeManager es nulo. No se puede cambiar el tema.");
              return;
          }
          if (nuevoTema == null || nuevoTema.trim().isEmpty()) {
              System.err.println("ERROR [cambiarTema]: El nombre del nuevo tema no puede ser nulo o vacío.");
              return;
          }
          // Limpiar el nombre por si acaso
          String temaLimpio = nuevoTema.trim().toLowerCase();
          System.out.println("[Controller] Solicitud para cambiar tema a: " + temaLimpio);

          // 2. Intentar establecer el nuevo tema usando ThemeManager
          //    Este método internamente también actualiza la clave en ConfigurationManager.
          boolean temaCambiado = themeManager.setTemaActual(temaLimpio);

          // 3. Actuar solo si el tema realmente cambió
          if (temaCambiado) {
              System.out.println("  -> Tema cambiado exitosamente en ThemeManager (y Config en memoria).");

              // 3.a. Actualizar estado visual de los Radio Buttons del Menú de Tema
              if (themeActions != null) {
                  // Obtener el nombre confirmado del tema actual (podría haber fallbacks en ThemeManager)
                  String temaConfirmado = themeManager.getTemaActual().nombreInterno();
                  System.out.println("  -> Actualizando estado de selección de Actions de tema para coincidir con: " + temaConfirmado);
                  for (Action action : themeActions) {
                      // Asegurarse de que es la Action correcta y llamar a su método de actualización
                      if (action instanceof ToggleThemeAction) {
                          ((ToggleThemeAction) action).actualizarEstadoSeleccion(temaConfirmado);
                      }
                  }
              } else {
                   System.err.println("WARN [cambiarTema]: La lista 'themeActions' es nula. No se pudo actualizar estado de radios.");
              }

              // 3.c. Notificar al Usuario sobre la necesidad de reiniciar
              System.out.println("  -> Mostrando diálogo de reinicio necesario...");
              
              // Crear un nombre más legible para el diálogo
              String nombreTemaDisplay = temaLimpio.substring(0, 1).toUpperCase() + temaLimpio.substring(1);
              
              // Usar invokeLater para asegurar que el diálogo se muestra correctamente en el EDT
              SwingUtilities.invokeLater(() -> {
                   JOptionPane.showMessageDialog(
                       (view != null ? view.getFrame() : null), // Padre del diálogo (ventana principal si existe)
                       "El tema se ha cambiado a '" + nombreTemaDisplay + "'.\n" +
                       "Los cambios visuales completos se aplicarán la próxima vez\n" +
                       "que inicie la aplicación.",
                       "Cambio de Tema", // Título del diálogo
                       JOptionPane.INFORMATION_MESSAGE // Tipo de mensaje
                   );
              });
          } else {
        	  
               // Si themeManager.setTemaActual devolvió false, significa que ya era ese tema
               // o que el nombre del tema no era válido según la lógica interna de ThemeManager.
              System.out.println("  -> El tema solicitado ('" + temaLimpio + "') ya era el actual o no es válido. No se realizaron cambios.");
              // Opcional: Podríamos forzar la sincronización de los radios por si acaso estaban desincronizados.
               if (themeActions != null) {
                    String temaConfirmado = themeManager.getTemaActual().nombreInterno();
                    for (Action action : themeActions) {
                        if (action instanceof ToggleThemeAction) { ((ToggleThemeAction) action).actualizarEstadoSeleccion(temaConfirmado); }
                    }
               }
          }
          System.out.println("[Controller] Fin cambiarTemaYNotificar.");
      } // --- FIN cambiarTemaYNotificar ---
      
    
      /**
       * Muestra un diálogo modal que contiene una lista de los archivos de imagen
       * actualmente cargados en el modelo principal. Permite al usuario ver la lista
       * completa y, opcionalmente, copiarla al portapapeles, mostrando nombres de archivo
       * relativos o rutas completas.
       */
      private void mostrarDialogoListaImagenes() {
          // 1. Validar dependencias (Vista y Modelo necesarios)
          if (view == null || model == null) {
              System.err.println("ERROR [mostrarDialogoListaImagenes]: Vista o Modelo nulos. No se puede mostrar el diálogo.");
              // Podríamos mostrar un JOptionPane de error aquí si fuera crítico
              return;
          }
          System.out.println("[Controller] Abriendo diálogo de lista de imágenes...");

          // 2. Crear el JDialog
          //    - Lo hacemos modal (true) para que bloquee la ventana principal mientras está abierto.
          //    - Usamos view.getFrame() como padre para que se centre correctamente.
          final JDialog dialogoLista = new JDialog(view.getFrame(), "Lista de Imágenes Cargadas", true);
          dialogoLista.setSize(600, 400); // Tamaño inicial razonable
          dialogoLista.setLocationRelativeTo(view.getFrame()); // Centrar sobre la ventana principal
          dialogoLista.setLayout(new BorderLayout(5, 5)); // Layout principal del diálogo

          // 3. Crear componentes internos del diálogo
          
          // 3.1. Modelo para la JList del diálogo (será llenado dinámicamente)
          final DefaultListModel<String> modeloListaDialogo = new DefaultListModel<>();
          
          // 3.2. JList que usará el modelo anterior
          JList<String> listaImagenesDialogo = new JList<>(modeloListaDialogo);
          
          // 3.3. ScrollPane para la JList (indispensable si la lista es larga)
          JScrollPane scrollPaneListaDialogo = new JScrollPane(listaImagenesDialogo);
          
          // 3.4. CheckBox para alternar entre nombres relativos y rutas completas
          final JCheckBox checkBoxMostrarRutas = new JCheckBox("Mostrar Rutas Completas");
          
          // 3.5. Botón para copiar la lista visible al portapapeles
          JButton botonCopiarLista = new JButton("Copiar Lista");

          // 4. Configurar Panel Superior (Botón Copiar y CheckBox)
          JPanel panelSuperiorDialog = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Alineación izquierda
          panelSuperiorDialog.add(botonCopiarLista);
          panelSuperiorDialog.add(checkBoxMostrarRutas);

          // 5. Añadir Componentes al Layout del Diálogo
          dialogoLista.add(panelSuperiorDialog, BorderLayout.NORTH);  // Panel superior arriba
          dialogoLista.add(scrollPaneListaDialogo, BorderLayout.CENTER); // Lista (en scroll) en el centro

          // 6. Añadir ActionListeners a los controles interactivos
          
          // 6.1. Listener para el CheckBox (actualiza la lista cuando cambia su estado)
          checkBoxMostrarRutas.addActionListener(e -> {
              // Llama al método helper para refrescar el contenido de la lista del diálogo
              // pasándole el modelo del diálogo y el estado actual del checkbox.
              actualizarListaEnDialogo(modeloListaDialogo, checkBoxMostrarRutas.isSelected());
          });

          // 6.2. Listener para el Botón Copiar
          botonCopiarLista.addActionListener(e -> {
          
        	  // Llama al método helper que copia el contenido del modelo del diálogo
              copiarListaAlPortapapeles(modeloListaDialogo);
              
              // Opcional: Mostrar un feedback breve
              // FIXME mostrar un joptionpane o un mensaje en una barra de informacion....
              //JOptionPane.showMessageDialog(dialogoLista, "Lista copiada al portapapeles.", "Copiado", JOptionPane.INFORMATION_MESSAGE);
          });

          // 7. Cargar el contenido inicial de la lista en el diálogo
          //    Se llama una vez antes de mostrar el diálogo, usando el estado inicial del checkbox (desmarcado).
          System.out.println("  -> Actualizando contenido inicial del diálogo...");
          actualizarListaEnDialogo(modeloListaDialogo, checkBoxMostrarRutas.isSelected());

          // 8. Hacer visible el diálogo
          //    Como es modal, la ejecución se detendrá aquí hasta que el usuario cierre el diálogo.
          System.out.println("  -> Mostrando diálogo...");
          dialogoLista.setVisible(true);

          // 9. Código después de cerrar el diálogo (si es necesario)
          //    Aquí podríamos hacer algo una vez el diálogo se cierra, pero usualmente no es necesario.
          System.out.println("[Controller] Diálogo de lista de imágenes cerrado.");

      } // --- FIN mostrarDialogoListaImagenes ---
      
    
      /**
       * Actualiza el contenido del DefaultListModel proporcionado (que pertenece
       * al diálogo de la lista de imágenes) basándose en el modelo principal
       * de la aplicación (model.getModeloLista()) y el mapa de rutas completas
       * (model.getRutaCompletaMap()).
       *
       * Llena el modelo del diálogo con las claves relativas o las rutas absolutas
       * de los archivos, según el valor del parámetro 'mostrarRutas'.
       *
       * @param modeloDialogo El DefaultListModel del JList que se encuentra en el diálogo.
       *                      Este método modificará su contenido (lo limpia y lo vuelve a llenar).
       * @param mostrarRutas  boolean que indica qué formato mostrar:
       *                      - true: Muestra la ruta completa (absoluta) de cada archivo.
       *                      - false: Muestra la clave única (ruta relativa) de cada archivo.
       */
      private void actualizarListaEnDialogo(DefaultListModel<String> modeloDialogo, boolean mostrarRutas) {
          // 1. Validación de entradas
          if (modeloDialogo == null) {
              System.err.println("ERROR [actualizarListaEnDialogo]: El modelo del diálogo es null.");
              return;
          }
          if (model == null || model.getModeloLista() == null || model.getRutaCompletaMap() == null) {
              System.err.println("ERROR [actualizarListaEnDialogo]: El modelo principal o sus componentes internos son null.");
              modeloDialogo.clear(); // Limpiar el diálogo si no hay datos fuente
              modeloDialogo.addElement("Error: No se pudo acceder a los datos de la lista principal.");
              return;
          }

          // 2. Referencias al modelo principal y al mapa de rutas
          DefaultListModel<String> modeloPrincipal = model.getModeloLista();
          Map<String, Path> mapaRutas = model.getRutaCompletaMap();

          // 3. Log informativo
          System.out.println("  [Dialogo Lista] Actualizando contenido. Mostrar Rutas: " + mostrarRutas + ". Elementos en modelo principal: " + modeloPrincipal.getSize());

          // 4. Limpiar el modelo del diálogo antes de llenarlo
          modeloDialogo.clear();

          // 5. Iterar sobre el modelo principal y añadir elementos al modelo del diálogo
          if (modeloPrincipal.isEmpty()) {
              modeloDialogo.addElement("(La lista principal está vacía)");
          } else {
              for (int i = 0; i < modeloPrincipal.getSize(); i++) {
                  // 5.1. Obtener la clave del modelo principal
                  String claveArchivo = modeloPrincipal.getElementAt(i);
                  if (claveArchivo == null) { // Seguridad extra
                      claveArchivo = "(Clave nula en índice " + i + ")";
                  }

                  // 5.2. Determinar qué texto añadir al diálogo
                  String textoAAgregar = claveArchivo; // Por defecto, la clave

                  if (mostrarRutas) {
                      // Si se deben mostrar rutas completas, obtenerla del mapa
                      Path rutaCompleta = mapaRutas.get(claveArchivo);
                      if (rutaCompleta != null) {
                          // Usar la ruta completa si se encontró
                          textoAAgregar = rutaCompleta.toString();
                      } else {
                          // Si no se encontró la ruta (inconsistencia en datos), indicarlo
                          System.err.println("WARN [Dialogo Lista]: No se encontró ruta para la clave: " + claveArchivo);
                          textoAAgregar = claveArchivo + " (¡Ruta no encontrada!)";
                      }
                  }
                  // Si mostrarRutas es false, textoAAgregar simplemente mantiene la claveArchivo.

                  // 5.3. Añadir el texto determinado al modelo del diálogo
                  modeloDialogo.addElement(textoAAgregar);
                  
              } // Fin del bucle for
          } // Fin else (modeloPrincipal no está vacío)

          // 6. Log final (opcional)
           System.out.println("  [Dialogo Lista] Contenido actualizado. Elementos añadidos al diálogo: " + modeloDialogo.getSize());

          // Nota: No necesitamos repintar la JList del diálogo aquí.
          // El DefaultListModel notifica automáticamente a la JList asociada
          // sobre los cambios (clear y addElement disparan ListDataEvents).

      } // --- FIN actualizarListaEnDialogo ---
      
      
	
      /**
	   * Copia el contenido actual de un DefaultListModel (que se asume contiene
	   * Strings, una por línea) al portapapeles del sistema.
	   * Cada elemento del modelo se añade como una línea separada en el texto copiado.
	   *
	   * @param listModel El DefaultListModel<String> cuyo contenido se copiará.
	   *                  Típicamente, este será el modelo de la JList del diálogo
	   *                  (modeloListaDialogo).
	   */
      private void copiarListaAlPortapapeles(DefaultListModel<String> listModel) {
      // 1. Validación de entrada
      if (listModel == null) {
          System.err.println("ERROR [copiarListaAlPortapapeles]: El listModel proporcionado es null.");
          // Opcional: Mostrar mensaje al usuario si la vista está disponible
          
          if (view != null) {
              JOptionPane.showMessageDialog(view.getFrame(),
                                            "Error interno al intentar copiar la lista.",
                                            "Error al Copiar", JOptionPane.WARNING_MESSAGE);
          }
          
          return;
      }

      // 2. Construir el String a copiar
      StringBuilder sb = new StringBuilder();
      int numeroElementos = listModel.getSize();

      System.out.println("[Portapapeles] Preparando para copiar " + numeroElementos + " elementos...");

      // Iterar sobre todos los elementos del modelo
      for (int i = 0; i < numeroElementos; i++) {
          String elemento = listModel.getElementAt(i);
          if (elemento != null) { // Añadir solo si no es null
              sb.append(elemento); // Añadir el texto del elemento
              
              // Añadir un salto de línea después de cada elemento, excepto el último
              if (i < numeroElementos - 1) {
                  sb.append("\n"); // Usar salto de línea estándar del sistema
                  // Alternativa: sb.append(System.lineSeparator());
              }
          }
      }

      // 3. Crear el objeto Transferable (StringSelection)
      //    StringSelection es una implementación de Transferable para texto plano.
      String textoCompleto = sb.toString();
      StringSelection stringSelection = new StringSelection(textoCompleto);

      // 4. Obtener el Portapapeles del Sistema
      Clipboard clipboard = null;
      try {
          clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      } catch (Exception e) {
           System.err.println("ERROR [copiarListaAlPortapapeles]: No se pudo acceder al portapapeles del sistema: " + e.getMessage());
            if (view != null) {
               JOptionPane.showMessageDialog(view.getFrame(),
                                             "Error al acceder al portapapeles del sistema.",
                                             "Error al Copiar", JOptionPane.ERROR_MESSAGE);
            }
           return; // Salir si no podemos obtener el clipboard
      }


      // 5. Establecer el contenido en el Portapapeles
      try {
          // El segundo argumento 'this' indica que nuestra clase VisorController
          // actuará como "dueño" temporal del contenido (implementa ClipboardOwner).
          clipboard.setContents(stringSelection, this);
          System.out.println("[Portapapeles] Lista copiada exitosamente (" + numeroElementos + " líneas).");
          // Opcional: Mostrar mensaje de éxito
           if (view != null) {
               // Podríamos usar un mensaje no modal o una etiqueta temporal
               // JOptionPane.showMessageDialog(view.getFrame(), "Lista copiada al portapapeles.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
           }
      } catch (IllegalStateException ise) {
          // Puede ocurrir si el clipboard no está disponible o está siendo usado
           System.err.println("ERROR [copiarListaAlPortapapeles]: No se pudo establecer el contenido en el portapapeles: " + ise.getMessage());
            if (view != null) {
               JOptionPane.showMessageDialog(view.getFrame(),
                                             "No se pudo copiar la lista al portapapeles.\n" +
                                             "Puede que otra aplicación lo esté usando.",
                                             "Error al Copiar", JOptionPane.WARNING_MESSAGE);
            }
      } catch (Exception e) {
           // Capturar otros errores inesperados
           System.err.println("ERROR INESPERADO [copiarListaAlPortapapeles]: " + e.getMessage());
           e.printStackTrace();
            if (view != null) {
               JOptionPane.showMessageDialog(view.getFrame(),
                                             "Ocurrió un error inesperado al copiar la lista.",
                                             "Error al Copiar", JOptionPane.ERROR_MESSAGE);
            }
      }

  } // --- FIN copiarListaAlPortapapeles ---


	/**
	 * Método requerido por la interfaz ClipboardOwner. Se llama cuando otra
	 * aplicación toma posesión del contenido del portapapeles que esta aplicación
	 * había puesto previamente.
	 * 
	 * En la mayoría de los casos, especialmente cuando solo copiamos texto simple,
	 * no necesitamos realizar ninguna acción específica cuando perdemos la
	 * posesión. Dejamos el método implementado pero vacío.
	 *
	 * @param clipboard El portapapeles que perdió la posesión.
	 * @param contents  El contenido Transferable que estaba en el portapapeles.
	 */
	@Override
	public void lostOwnership (Clipboard clipboard, Transferable contents)
	{
		// 1. Log (Opcional, útil para depuración o entender el flujo)
		// System.out.println("[Clipboard] Se perdió la propiedad del contenido del
		// portapapeles.");

		// 2. Lógica Adicional (Normalmente no necesaria para copia de texto simple)
		// - Si estuvieras manejando recursos más complejos o datos que necesitan
		// liberarse cuando ya no están en el portapapeles, podrías hacerlo aquí.
		// - Para StringSelection, no hay nada que liberar.

		// -> Método intencionalmente vacío en este caso. <-

	} // --- FIN lostOwnership ---       
       
    // (Asegúrate de que la clase declara 'implements ActionListener')

    /**
     * Manejador central de eventos para componentes que NO utilizan directamente
     * el sistema de Actions de Swing (p.ej., JMenuItems a los que se les añadió
     * 'this' como ActionListener en addFallbackListeners) o para acciones
     * muy específicas que no justificaban una clase Action separada.
     *
     * Identifica el componente que originó el evento y/o el comando de acción,
     * y delega la lógica al método apropiado.
     *
     * @param e El ActionEvent generado por el componente Swing.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // 1. Log inicial detallado (muy útil para depurar qué se está presionando)
        logActionInfo(e); // Llama al helper para imprimir fuente, comando, etc.

        // 2. Obtener información del evento
        Object source = e.getSource();         // El componente que disparó el evento
        String command = e.getActionCommand(); // El comando asociado (puede ser corto o largo)

        // 3. Procesar el comando usando un switch
        //    Este switch maneja principalmente casos "fallback" donde no usamos setAction().
        //    Las Actions asignadas con setAction() ejecutan su propio actionPerformed
        //    y normalmente no pasarán por este método central.
        if (command == null) {
             System.err.println("WARN [actionPerformed Central]: Comando es null para fuente: " + (source != null ? source.getClass().getSimpleName() : "null"));
             return; // Salir si no hay comando
        }

        switch (command) {
            // --- Comandos de Menú (Ejemplos Fallback) ---

            // Configuración
            case "Guardar_Configuracion_Actual":
                System.out.println("-> Acción Fallback: Guardar Configuración Actual");
                guardarConfiguracionActual(); // Llama al método que guarda
                break;
            case "Cargar_Configuracion_Inicial":
                 System.out.println("-> Acción Fallback: Cargar Configuración Inicial (Recargar UI)");
                 // Volver a aplicar toda la configuración y recargar la lista
                 aplicarConfiguracionInicial();
                 cargarEstadoInicial(); // Esto recargará la lista
                 break;

            // Imagen
            case "Cambiar_Nombre_de_la_Imagen":
                System.out.println("TODO: Acción Fallback - Implementar Cambiar Nombre Imagen");
                // Aquí iría la lógica para mostrar un diálogo y renombrar el archivo
                break;
            case "Mover_a_la_Papelera":
                 System.out.println("TODO: Acción Fallback - Implementar Mover a Papelera");
                 // Lógica para mover archivo actual a papelera (requiere JNA o similar)
                 break;
            case "Establecer_Como_Fondo_de_Escritorio":
                 System.out.println("TODO: Acción Fallback - Implementar Fondo Escritorio");
                 // Lógica específica de SO para cambiar fondo
                 break;
            case "Establecer_Como_Imagen_de_Bloqueo":
                 System.out.println("TODO: Acción Fallback - Implementar Imagen Bloqueo");
                 // Lógica específica de SO
                 break;
            case "Propiedades_de_la_imagen":
                 System.out.println("TODO: Acción Fallback - Mostrar Propiedades Imagen");
                 // Mostrar diálogo con información EXIF/Detalles del archivo
                 break;

            // Vista
            case "Mostrar_Dialogo_Lista_de_Imagenes":
                System.out.println("-> Acción Fallback: Mostrar Diálogo Lista Imágenes");
                mostrarDialogoListaImagenes(); // Llama al método que abre el diálogo
                break;

            // Ayuda
            case "Version":
                System.out.println("-> Acción Fallback: Mostrar Versión");
                mostrarVersion(); // Llama al método que muestra el JOptionPane
                break;

            // --- Otros Comandos Específicos ---
            // Añade aquí otros 'case' para ActionCommands que no estén manejados
            // por clases Action dedicadas y a los que hayas añadido 'this' como listener.

            // --- Default Case ---
            default:
                // Este caso se alcanza si un componente al que añadimos 'this' como listener
                // tiene un ActionCommand que no hemos listado arriba.
                // Podría ser un JMenuItem de visibilidad de botón si no tuviera Action.
                 if (source instanceof JMenuItem && !(source instanceof JMenu)) { // Solo para items finales
                     System.out.println("WARN [actionPerformed Central]: Comando fallback no manejado explícitamente: '" + command + "' de " + source.getClass().getSimpleName());
                     // Podríamos añadir lógica genérica aquí si fuera necesario,
                     // pero es mejor tener casos específicos o usar Actions.
                 }
                 // No imprimir warning para JMenu o JButton ya que estos deberían usar Actions.
                 break;
        } // Fin switch

        // Log final (opcional)
        // System.out.println("[ActionListener Central] Procesamiento finalizado para comando: " + command);

    } // --- FIN actionPerformed ---  

    
    /**
     * Muestra un diálogo simple (JOptionPane) con información básica
     * sobre la versión de la aplicación.
     * El número de versión y el autor están actualmente hardcodeados,
     * pero podrían leerse de un archivo de propiedades o de metadatos del MANIFEST.
     */
    private void mostrarVersion() {
        // 1. Definir la información a mostrar
        //    TODO: Considerar leer estos valores de un archivo externo o MANIFEST.MF
        String nombreApp = "Visor de Imágenes V2";
        String version = "1.1.0-MVC-SyncLists"; // Ejemplo de número de versión
        String autor = "(c) 2024 Javier Tortosa"; // ¡Tu nombre aquí!
        String mensaje = nombreApp + "\nVersión: " + version + "\n" + autor;
        String tituloDialogo = "Acerca de " + nombreApp;

        // 2. Log (Opcional)
        System.out.println("[Controller] Mostrando diálogo de versión...");

        // 3. Mostrar el JOptionPane
        //    - Usamos view.getFrame() como componente padre para centrar el diálogo.
        //    - message: El texto a mostrar.
        //    - title: El título de la ventana del diálogo.
        //    - messageType: El icono a mostrar (INFORMATION_MESSAGE es un icono 'i').
        JOptionPane.showMessageDialog(
            (view != null ? view.getFrame() : null), // Componente padre (o null si view no existe)
            mensaje,                                 // Mensaje a mostrar
            tituloDialogo,                           // Título de la ventana
            JOptionPane.INFORMATION_MESSAGE          // Tipo de icono
        );

        // 4. Log final (Opcional)
        // System.out.println("  -> Diálogo de versión cerrado.");

    } // --- FIN mostrarVersion ---
    
       
    /**
     * Imprime en la consola información detallada sobre un ActionEvent recibido.
     * Útil para depurar y entender qué componente/acción generó un evento.
     * Intenta obtener la clase de la fuente, el comando de acción, la clave larga
     * de configuración asociada (si se encuentra) y el nombre del icono (si es un botón).
     *
     * @param e El ActionEvent a analizar.
     */
    public void logActionInfo(ActionEvent e) {
        // 1. Validar el evento
        if (e == null) {
            System.out.println("--- Acción Detectada (Evento Nulo) ---");
            return; // No podemos hacer nada si el evento es nulo
        }

        // 2. Obtener información básica del evento
        Object source = e.getSource();
        String command = e.getActionCommand(); // El comando asociado al evento
        String sourceClass = (source != null) ? source.getClass().getSimpleName() : "null";

        // 3. Intentar obtener información adicional (clave larga, icono)
        //    Estos métodos helper buscan en los mapas de botones/menús de la vista.
        String longConfigKey = findLongKeyForComponent(source); // Puede devolver null
        String iconName = findIconNameForComponent(source);     // Puede devolver null

        // 4. Imprimir la información formateada en la consola
        System.out.println("--- Acción Detectada ---");
        System.out.println("  Fuente     : " + sourceClass + (source != null ? " (ID: "+ System.identityHashCode(source) +")" : "")); // Añadir ID objeto
        System.out.println("  Comando    : " + (command != null ? "'" + command + "'" : "null"));
        System.out.println("  Clave Larga: " + (longConfigKey != null ? "'" + longConfigKey + "'" : "(No encontrada)"));
        // Mostrar icono solo si se encontró
        if (iconName != null) {
             System.out.println("  Icono      : " + iconName);
        }
        // Opcional: Imprimir modificadores si son relevantes (Shift, Ctrl, etc.)
        // String modifiers = ActionEvent.getModifiersText(e.getModifiers());
        // if (!modifiers.isEmpty()) {
        //      System.out.println("  Modificadores: " + modifiers);
        // }
        System.out.println("-------------------------");

    } // --- FIN logActionInfo ---

    // --- Métodos Helper (Asegúrate de que existan y sean correctos) ---

    /**
     * Busca en los mapas de botones y menús de la vista para encontrar la
     * clave de configuración larga asociada a un componente Swing dado.
     * @param source El componente (JButton, JMenuItem, etc.).
     * @return La clave larga de configuración (ej. "interfaz.boton.movimiento.Siguiente_48x48")
     *         o null si no se encuentra o la vista/mapas no están inicializados.
     */
    public String findLongKeyForComponent(Object source) {
        // Validar dependencias
        if (view == null || !(source instanceof Component)) {
             // System.err.println("WARN [findLongKey]: Vista nula o fuente no es Componente."); // Log opcional
            return null;
        }
        Component comp = (Component) source;

        // Buscar en botones
        Map<String, JButton> botones = view.getBotonesPorNombre();
        if (botones != null) {
            for (Map.Entry<String, JButton> entry : botones.entrySet()) {
                if (entry.getValue() == comp) { // Comparar por referencia de objeto
                    return entry.getKey(); // Devuelve la clave larga
                }
            }
        }

        // Buscar en menús
        Map<String, JMenuItem> menuItems = view.getMenuItemsPorNombre();
        if (menuItems != null) {
            for (Map.Entry<String, JMenuItem> entry : menuItems.entrySet()) {
                 if (entry.getValue() == comp) { // Comparar por referencia de objeto
                     return entry.getKey(); // Devuelve la clave larga
                 }
            }
        }

        // Si no se encontró en ninguno de los mapas
        // System.out.println("INFO [findLongKey]: No se encontró clave larga para: " + source.getClass().getSimpleName()); // Log opcional
        return null;
    }

     /**
     * Intenta inferir el nombre del archivo de icono asociado a un JButton.
     * Es una heurística simple basada en la clave larga del componente.
     * @param source El componente fuente (se espera que sea un JButton).
     * @return El nombre inferido del archivo PNG del icono (ej. "Siguiente_48x48.png")
     *         o null si no es un JButton, no tiene icono, o no se puede inferir.
     */
    public String findIconNameForComponent(Object source) {
         if (source instanceof JButton) {
             JButton button = (JButton) source;
             Icon icon = button.getIcon(); // Obtener el icono actual del botón

             // Proceder solo si el icono es un ImageIcon (no otros tipos de Icon)
             if (icon instanceof ImageIcon) {
                 // Intentar obtener la clave larga para inferir el nombre
                 String longKey = findLongKeyForComponent(source);
                  if (longKey != null && longKey.startsWith("interfaz.boton.")) {
                      // Separar la clave por puntos
                     String[] parts = longKey.split("\\.");
                     // Si tenemos suficientes partes (interfaz.boton.categoria.nombreBoton)
                     if (parts.length >= 4) {
                          // La última parte debería ser el nombre base del icono
                          return parts[parts.length - 1] + ".png"; // Asumir extensión .png
                     }
                 }
                 // Si no se pudo inferir desde la clave, devolver un mensaje genérico
                  // return "(Icono: " + ((ImageIcon) icon).getDescription() + ")"; // Opcional: usar descripción si la tiene
                 return "(Icono presente, nombre no inferido)";
             }
             // Si no tiene icono ImageIcon
             // else { return "(Sin ImageIcon)"; }
         }
         // Si no es un JButton
         return null;
    }// fin findIconNameForComponent
    

     
     
//   FIXME (Opcionalmente, podría estar en una clase de Utilidades si se usa en más sitios)

  /**
   * Convierte una cadena de texto que representa un color en formato "R, G, B"
   * (donde R, G, B son números enteros entre 0 y 255) en un objeto java.awt.Color.
   *
   * Ignora espacios alrededor de los números y las comas.
   * Valida que los componentes numéricos estén en el rango [0, 255].
   *
   * @param rgbString La cadena de texto a parsear (ej. "238, 238, 238", " 0, 0,0 ").
   *                  Si es null, vacía o tiene un formato incorrecto, se devolverá
   *                  un color por defecto (gris claro).
   * @return El objeto Color correspondiente a la cadena RGB, o Color.LIGHT_GRAY
   *         si la cadena no se pudo parsear correctamente.
   */
  private Color parseColor(String rgbString) {
      // 1. Manejar entrada nula o vacía
      if (rgbString == null || rgbString.trim().isEmpty()) {
          System.err.println("WARN [parseColor]: Cadena RGB nula o vacía. Usando color por defecto (Gris Claro).");
          return Color.LIGHT_GRAY; // Color por defecto seguro
      }

      // 2. Separar la cadena por las comas
      String[] components = rgbString.split(",");

      // 3. Validar que tengamos exactamente 3 componentes
      if (components.length == 3) {
          try {
              // 3.1. Parsear cada componente a entero, quitando espacios (trim)
              int r = Integer.parseInt(components[0].trim());
              int g = Integer.parseInt(components[1].trim());
              int b = Integer.parseInt(components[2].trim());

              // 3.2. Validar el rango [0, 255] para cada componente
              //      Usamos Math.max/min para asegurar que el valor quede dentro del rango.
              r = Math.max(0, Math.min(255, r));
              g = Math.max(0, Math.min(255, g));
              b = Math.max(0, Math.min(255, b));

              // 3.3. Crear y devolver el objeto Color
              return new Color(r, g, b);

          } catch (NumberFormatException e) {
              // Error si alguno de los componentes no es un número entero válido
              System.err.println("WARN [parseColor]: Formato numérico inválido en '" + rgbString + "'. Usando color por defecto (Gris Claro). Error: " + e.getMessage());
              return Color.LIGHT_GRAY; // Devolver color por defecto
          } catch (Exception e) {
               // Capturar otros posibles errores inesperados durante el parseo
               System.err.println("ERROR INESPERADO [parseColor] parseando '" + rgbString + "': " + e.getMessage());
               e.printStackTrace();
               return Color.LIGHT_GRAY; // Devolver color por defecto
          }
      	} else {
          // Error si no se encontraron exactamente 3 componentes después de split(',')
           System.err.println("WARN [parseColor]: Formato de color debe ser R,G,B. Recibido: '" + rgbString + "'. Usando color por defecto (Gris Claro).");
           return Color.LIGHT_GRAY; // Devolver color por defecto
      	}
  	} // --- FIN parseColor ---
  

  
  
	/**
	 * Crea y configura un JLabel que actúa como un marcador de posición
	 * ('placeholder') para una miniatura que aún se está cargando o que ha fallado
	 * al cargar.
	 *
	 * Configura el texto (generalmente "..."), el tamaño preferido, el color de
	 * fondo, el color de texto, el borde (punteado, con color diferente si está
	 * seleccionada) y el tooltip.
	 *
	 * @param toolTipText    El texto a mostrar cuando el usuario pase el ratón por
	 *                       encima (ej. "Cargando: imagen.jpg" o "Error:
	 *                       imagen_corrupta.png").
	 * @param ancho          El ancho deseado para el placeholder (debe coincidir
	 *                       con el tamaño de celda esperado en la lista de
	 *                       miniaturas).
	 * @param alto           El alto deseado para el placeholder. Si es <= 0, se
	 *                       usará el ancho.
	 * @param esSeleccionada true si este placeholder representa la miniatura que
	 *                       corresponde a la imagen actualmente seleccionada en la
	 *                       lista principal, para aplicarle un borde distintivo.
	 * @return Un JLabel configurado como placeholder.
	 */
	private JLabel crearPlaceholderMiniatura (String toolTipText, int ancho, int alto, boolean esSeleccionada)
	{

		// 1. Crear el JLabel con texto inicial "..." y centrado
		JLabel placeholder = new JLabel("...", SwingConstants.CENTER);

		// 2. Hacerlo opaco para que el color de fondo sea visible
		placeholder.setOpaque(true);

		// 3. Establecer color de texto (gris para indicar que no es contenido real)
		placeholder.setForeground(Color.GRAY);

		// 4. Calcular y establecer tamaño preferido
		// Asegurar un tamaño mínimo y manejar el caso de alto <= 0
		int phAncho = Math.max(10, ancho); // Ancho mínimo de 10px
		int phAlto = Math.max(10, (alto <= 0 ? phAncho : alto)); // Alto mínimo, o igual al ancho si alto<=0
		placeholder.setPreferredSize(new Dimension(phAncho, phAlto));

		// 5. Configurar borde y fondo según si está seleccionada
		if (esSeleccionada)
		{
			// Borde punteado azul y fondo azul claro para placeholder seleccionado
			// TODO: Considerar usar colores del tema (uiConfig) si están disponibles aquí
			placeholder.setBorder(BorderFactory.createDashedBorder(Color.BLUE, 1, 5, 3, false)); // Ajustar estilo de
																									// línea si se desea
			placeholder.setBackground(new Color(220, 230, 255)); // Azul muy pálido
		} else
		{
			// Borde punteado gris claro y fondo blanco para placeholder normal
			placeholder.setBorder(BorderFactory.createDashedBorder(Color.LIGHT_GRAY, 1, 5, 3, false));
			placeholder.setBackground(Color.WHITE); // O un gris muy claro
		}

		// 6. Establecer el Tooltip (mensaje informativo al pasar el ratón)
		placeholder.setToolTipText(toolTipText != null ? toolTipText : "Cargando..."); // Tooltip por defecto si es null

		// 7. Devolver el JLabel configurado
		return placeholder;

	} // --- FIN crearPlaceholderMiniatura ---

     /** Getters para Actions (si se usan externamente). */
     // public Action getFirstImageAction() { return firstImageAction; }
     // public Action getLastImageAction() { return lastImageAction; }
     
     
     /** Getters para Modelo/Vista/Config (usados por Actions). */
     public VisorModel getModel() { return model; }
     public VisorView getView() { return view; }
     public ConfigurationManager getConfigurationManager() { return configuration; }
     
     
     /**
      * Configura un 'Shutdown Hook', que es un hilo que la JVM intentará ejecutar
      * cuando la aplicación está a punto de cerrarse (ya sea normalmente o por
      * una señal externa como Ctrl+C, pero no necesariamente en caso de crash).
      *
      * El propósito principal de este hook es guardar el estado actual de la
      * aplicación (tamaño/posición de ventana, última carpeta/imagen, configuraciones UI)
      * en el archivo config.cfg para poder restaurarlo la próxima vez que se inicie.
      * También se encarga de apagar de forma ordenada el ExecutorService.
      */
     private void configurarShutdownHook() {
         // Crear un nuevo hilo (sin nombre específico, pero se podría añadir)
         Thread shutdownThread = new Thread(() -> { // Inicio de la lambda para el hilo
             // Este código se ejecutará cuando la JVM inicie el proceso de cierre

             System.out.println("--- Hook de Cierre Iniciado ---");

             // --- 1. GUARDAR ESTADO DE LA VENTANA ---
             //      Solo si la vista y la configuración están disponibles
             if (view != null && configuration != null) {
                  System.out.println("  -> Guardando estado de la ventana...");
                  try {
                      // Comprobar si la ventana está maximizada
                      boolean isMaximized = (view.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
                      // Guardar el estado de maximización en la configuración
                      configuration.setString(ConfigurationManager.KEY_WINDOW_MAXIMIZED, String.valueOf(isMaximized));

                      // Si NO está maximizada, guardar posición y tamaño (bounds)
                      if (!isMaximized) {
                          java.awt.Rectangle bounds = view.getBounds(); // Obtener bounds actuales
                          configuration.setString(ConfigurationManager.KEY_WINDOW_X, String.valueOf(bounds.x));
                          configuration.setString(ConfigurationManager.KEY_WINDOW_Y, String.valueOf(bounds.y));
                          configuration.setString(ConfigurationManager.KEY_WINDOW_WIDTH, String.valueOf(bounds.width));
                          configuration.setString(ConfigurationManager.KEY_WINDOW_HEIGHT, String.valueOf(bounds.height));
                           System.out.println("    -> Bounds guardados en memoria config: " + bounds);
                      } else {
                           System.out.println("    -> Ventana maximizada, no se guardan bounds específicos.");
                      }
                  } catch (Exception e) {
                       // Capturar cualquier error inesperado al obtener/guardar estado ventana
                       System.err.println("  -> ERROR al guardar estado de ventana: " + e.getMessage());
                       e.printStackTrace(); // Imprimir stack trace para depuración
                  }
             } else {
                  // Informar si no se puede guardar estado (vista o config nulas)
                  System.out.println("  -> No se pudo guardar estado de ventana (Vista=" + view + ", Config=" + configuration + ").");
             }

             // --- 2. GUARDAR CONFIGURACIÓN GENERAL ---
             //      Llama al método que recopila todo el estado actual y lo pasa
             //      al ConfigurationManager para escribirlo en el archivo.
             System.out.println("  -> Llamando a guardarConfiguracionActual()...");
             // Verificar 'configuration' de nuevo por si acaso
             
             if (configuration != null) {
                 // Llamar al método que se encarga de obtener el estado y guardar
                  guardarConfiguracionActual(); // Este método llama internamente a configuration.guardarConfiguracion()
                  
             } else {
                  System.err.println("  -> ConfigurationManager null, no se puede llamar a guardarConfiguracionActual().");
             }

             // --- 3. APAGAR ExecutorService ---
             //      Es importante cerrar el pool de hilos de forma ordenada.
             System.out.println("  -> Apagando ExecutorService...");
             
             if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown(); // Iniciar apagado: no acepta nuevas tareas, espera a las actuales
                
                try {
             
                	// Esperar un tiempo razonable para que las tareas terminen
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        // Si no terminaron, forzar el apagado inmediato
                        System.err.println("    -> ExecutorService no terminó en 5s. Forzando shutdownNow()...");
                        executorService.shutdownNow(); // Intenta interrumpir tareas activas
                        
                        // Esperar un poco más después de forzar
                        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                             System.err.println("    -> ExecutorService AÚN no terminó después de shutdownNow().");
                        } else {
                             System.out.println("    -> ExecutorService finalmente terminado después de shutdownNow().");
                        }
                    } else {
                         System.out.println("    -> ExecutorService terminado ordenadamente.");
                    }

                } catch (InterruptedException ie) {
                    // Si el hilo del hook es interrumpido mientras espera
                    System.err.println("    -> Hilo ShutdownHook interrumpido mientras esperaba apagado de ExecutorService.");
                    executorService.shutdownNow(); // Forzar apagado inmediato
                    Thread.currentThread().interrupt(); // Re-establecer estado interrumpido del hook
                
                } catch (Exception e) {
                     // Capturar otros posibles errores durante el apagado
                     System.err.println("    -> ERROR inesperado durante apagado de ExecutorService: " + e.getMessage());
                     e.printStackTrace();
                }
                
             } else if (executorService == null){
                  System.out.println("    -> ExecutorService es null.");
                  
             } else { // Ya estaba shutdown
                  System.out.println("    -> ExecutorService ya estaba apagado.");
                  
             }

             // 4. Log final del hook
             System.out.println("--- Hook de Cierre Terminado ---");

         // Nombre opcional para el hilo del hook (útil en perfiles/debugging)
         }, "VisorShutdownHookThread"); 

         // Registrar el hilo creado como Shutdown Hook en la JVM
         Runtime.getRuntime().addShutdownHook(shutdownThread);
         System.out.println(" -> Shutdown Hook registrado en la JVM."); // Log confirmando registro

     } // --- FIN configurarShutdownHook ---

     
     /**
      * Recopila el estado actual relevante de la aplicación (desde el Modelo y la Vista)
      * y lo persiste en el archivo de configuración (`config.cfg`) utilizando el
      * servicio ConfigurationManager.
      *
      * Este método se llama típicamente desde el ShutdownHook para asegurar que
      * el último estado se guarda al cerrar la aplicación.
      *
      * Incluye:
      * - Última carpeta e imagen vistas.
      * - Configuración de carga de subcarpetas.
      * - Parámetros de la barra de miniaturas.
      * - Estado de activación del zoom manual y mantener proporciones.
      * - Estado de visibilidad y habilitación de botones y menús.
      * - Nombre del tema actual.
      * - (Implícitamente, guarda también el estado de la ventana si fue actualizado
      *   en el ConfigurationManager antes de llamar a este método).
      */
     private void guardarConfiguracionActual() { //weno
         // 1. Validar dependencias críticas (Config, Vista, Modelo)
         if (configuration == null || view == null || model == null) {
             System.err.println("ERROR [guardarConfiguracionActual]: Configuración, Vista o Modelo nulos. No se puede guardar.");
             // Salir si falta algo esencial para recopilar el estado.
             return;
         }
         System.out.println("  [ShutdownHook] Recopilando estado actual para guardar...");

         // 2. Obtener un mapa con la configuración actual en memoria
         //    Esto sirve como base, y actualizaremos los valores que dependen
         //    del estado actual de la UI o el modelo.
         Map<String, String> estadoActualParaGuardar = configuration.getConfigMap();

         // 3. Actualizar el mapa con valores del Modelo
         try {
             
        	 // 3.1. Última carpeta: Ya debería estar actualizada en 'configuration' si se usó el selector.
             //      Podríamos reconfirmar: estadoActualParaGuardar.put("inicio.carpeta", configuration.getString("inicio.carpeta", ""));
             
        	 // 3.2. Última imagen seleccionada
             estadoActualParaGuardar.put("inicio.imagen", model.getSelectedImageKey() != null ? model.getSelectedImageKey() : "");
             
             // 3.3. Modo subcarpetas
             estadoActualParaGuardar.put("comportamiento.carpeta.cargarSubcarpetas", String.valueOf(!model.isMostrarSoloCarpetaActual()));
             
             // 3.4. Configuración de miniaturas (si se pudieran cambiar)
             estadoActualParaGuardar.put("miniaturas.cantidad.antes", String.valueOf(model.getMiniaturasAntes()));
             estadoActualParaGuardar.put("miniaturas.cantidad.despues", String.valueOf(model.getMiniaturasDespues()));
             estadoActualParaGuardar.put("miniaturas.tamano.seleccionada.ancho", String.valueOf(model.getMiniaturaSelAncho()));
             estadoActualParaGuardar.put("miniaturas.tamano.seleccionada.alto", String.valueOf(model.getMiniaturaSelAlto()));
             estadoActualParaGuardar.put("miniaturas.tamano.normal.ancho", String.valueOf(model.getMiniaturaNormAncho()));
             estadoActualParaGuardar.put("miniaturas.tamano.normal.alto", String.valueOf(model.getMiniaturaNormAlto()));
             
             // 3.5. Estado de toggles (leer desde la Action asociada es más fiable que desde el modelo)
              if (toggleZoomManualAction != null) estadoActualParaGuardar.put("interfaz.menu.zoom.Activar_Zoom_Manual.seleccionado", String.valueOf(Boolean.TRUE.equals(toggleZoomManualAction.getValue(Action.SELECTED_KEY))));
              if (toggleProporcionesAction != null) estadoActualParaGuardar.put("interfaz.menu.zoom.Mantener_Proporciones.seleccionado", String.valueOf(Boolean.TRUE.equals(toggleProporcionesAction.getValue(Action.SELECTED_KEY))));
              // Estado subcarpetas (ya se guarda arriba desde el modelo, pero reconfirmar desde Action no hace daño)
              // if (toggleSubfoldersAction != null) estadoActualParaGuardar.put("comportamiento.carpeta.cargarSubcarpetas", String.valueOf(Boolean.TRUE.equals(toggleSubfoldersAction.getValue(Action.SELECTED_KEY))));

              // System.out.println("    -> Estado del Modelo recopilado."); // Log opcional
         } catch (Exception e) { System.err.println("ERROR recopilando estado del Modelo: " + e.getMessage()); }

         // 4. Actualizar el mapa con el estado de la Vista (Enabled/Visible/Selected)
         try {
             
        	 // 4.1. Estado de Botones
             Map<String, JButton> botones = view.getBotonesPorNombre();
             if (botones != null) {
                 botones.forEach((claveLarga, boton) -> {
                     estadoActualParaGuardar.put(claveLarga + ".activado", String.valueOf(boton.isEnabled()));
                     estadoActualParaGuardar.put(claveLarga + ".visible", String.valueOf(boton.isVisible()));
                 });
                 // System.out.println("    -> Estado de Botones recopilado."); // Log opcional
             } else { System.err.println("WARN [guardarConfig]: Mapa de botones nulo."); }

             // 4.2. Estado de Menús (solo items finales)
             Map<String, JMenuItem> menuItems = view.getMenuItemsPorNombre();
             if (menuItems != null) {
                 menuItems.forEach((claveLarga, item) -> {
                     if (!(item instanceof JMenu)) { // Ignorar menús contenedores
                         estadoActualParaGuardar.put(claveLarga + ".activado", String.valueOf(item.isEnabled()));
                         estadoActualParaGuardar.put(claveLarga + ".visible", String.valueOf(item.isVisible()));
                         // Guardar estado seleccionado para checkboxes y radios
                         if (item instanceof JCheckBoxMenuItem) estadoActualParaGuardar.put(claveLarga + ".seleccionado", String.valueOf(((JCheckBoxMenuItem) item).isSelected()));
                         else if (item instanceof JRadioButtonMenuItem) estadoActualParaGuardar.put(claveLarga + ".seleccionado", String.valueOf(((JRadioButtonMenuItem) item).isSelected()));
                     }
                 });
                  // System.out.println("    -> Estado de Menús recopilado."); // Log opcional
             } else { System.err.println("WARN [guardarConfig]: Mapa de menús nulo."); }
         } catch (Exception e) { System.err.println("ERROR recopilando estado de la Vista: " + e.getMessage()); }

         // 5. Asegurar que el nombre del tema es el correcto
         //    Obtenerlo directamente de ConfigurationManager, que a su vez lo tiene del ThemeManager.
         String temaActualConfirmado = configuration.getTemaActual();
         estadoActualParaGuardar.put(ConfigurationManager.KEY_TEMA_NOMBRE, temaActualConfirmado);
         System.out.println("    -> Estado recopilado completo. Tema a guardar: '" + temaActualConfirmado + "'");

         // 6. Llamar al ConfigurationManager para guardar el mapa en el archivo
         try {
             System.out.println("    -> Llamando a configuration.guardarConfiguracion con " + estadoActualParaGuardar.size() + " claves...");
             configuration.guardarConfiguracion(estadoActualParaGuardar); // Pasa el mapa completo
             System.out.println("  [ShutdownHook] Configuración actual guardada exitosamente."); // Mensaje de éxito final
         } catch (IOException e) {
             // Error específico de IO durante el guardado
             System.err.println("### ERROR FATAL AL GUARDAR CONFIGURACIÓN EN SHUTDOWN HOOK (IOException): " + e.getMessage() + " ###");
             e.printStackTrace();
         } catch (Exception e) {
              // Otros errores inesperados durante el guardado
              System.err.println("### ERROR INESPERADO AL GUARDAR CONFIGURACIÓN EN SHUTDOWN HOOK: " + e.getMessage() + " ###");
              e.printStackTrace();
         }
         System.out.println("  [ShutdownHook] Fin guardarConfiguracionActual.");

     } // --- FIN guardarConfiguracionActual ---
     
     
     /** Método helper para errores fatales. */
     private void handleFatalError(String message, Throwable cause) {
         System.err.println("FATAL: " + message);
         if (cause != null) cause.printStackTrace();
         JOptionPane.showMessageDialog(null, message + (cause != null ? "\n" + cause.getMessage() : ""), "Error Fatal", JOptionPane.ERROR_MESSAGE);
         System.exit(1);
     }
     
     

	/**
	 * Calcula el rango de miniaturas a mostrar basándose en la selección principal,
	 * reconstruye el modelo de datos específico para la JList de miniaturas
	 * (`this.modeloMiniaturas`), y actualiza la vista (JList) en el EDT para
	 * reflejar el nuevo rango y seleccionar el elemento correcto. Utiliza un modelo
	 * temporal para evitar modificar el modelo en uso por la JList directamente,
	 * previniendo así eventos de deselección inesperados. También pre-calienta el
	 * caché para las miniaturas del nuevo rango.
	 *
	 * @param indiceSeleccionadoPrincipal Índice (0-based) en el modelo PRINCIPAL
	 *                                    (`model.getModeloLista()`).
	 */
      
	public void actualizarModeloYVistaMiniaturas (int indiceSeleccionadoPrincipal)
	{

		// --- 1. VALIDACIONES INICIALES Y PREPARACIÓN ---
		// 1.1. Log de inicio
		System.out.println("\n--- INICIO actualizarModeloYVistaMiniaturas --- Índice Principal Recibido: "
				+ indiceSeleccionadoPrincipal);

		// 1.2. Validar dependencias críticas (Modelo, Vista, Lista de Miniaturas,
		// Coordinador)
		if (model == null || model.getModeloLista() == null || view == null || view.getListaMiniaturas() == null
				|| listCoordinator == null)
		{
			System.err.println(
					"WARN [actualizarMiniaturas]: Dependencias nulas (Modelo, Vista, ListaMiniaturas o Coordinator). Abortando.");
			return; // Salir si falta algo esencial
		}
		// 1.3. Obtener modelo principal y su tamaño
		DefaultListModel<String> modeloPrincipal = model.getModeloLista();
		int totalPrincipal = modeloPrincipal.getSize();
		System.out.println("  [actualizarMiniaturas] Tamaño modeloPrincipal: " + totalPrincipal);

		// 1.4. Crear un NUEVO DefaultListModel temporal para las miniaturas
		// Esto evita modificar el modelo actualmente en uso por la JList.
		DefaultListModel<String> nuevoModeloMiniaturasTemp = new DefaultListModel<>();
		// 1.5. Inicializar índice relativo (al modelo temporal)
		int indiceSeleccionadoEnModeloMiniatura = -1;

		// 1.6. Manejar caso de lista principal vacía o índice inválido
		if (totalPrincipal == 0 || indiceSeleccionadoPrincipal < 0 || indiceSeleccionadoPrincipal >= totalPrincipal)
		{
			System.out.println("  [actualizarMiniaturas] Índice inválido o lista principal vacía. Saliendo.");

			// Asegurar que la JList de miniaturas esté vacía si no lo estaba
			if (view.getListaMiniaturas().getModel().getSize() != 0)
			{
				// Programar la actualización en el EDT para asignar el modelo temporal vacío
				final DefaultListModel<String> modeloVacio = nuevoModeloMiniaturasTemp; // Referencia final
				SwingUtilities.invokeLater( () -> {

					if (view != null && view.getListaMiniaturas() != null)
					{
						// Usar el flag para proteger la asignación del modelo vacío
						listCoordinator.setSincronizandoUI(true);

						try
						{
							System.out.println(
									"  -> [actualizarMiniaturas] Estableciendo modelo vacío en JList Miniaturas.");
							view.setModeloListaMiniaturas(modeloVacio);
						} finally
						{
							SwingUtilities.invokeLater( () -> {
								if (listCoordinator != null)
									listCoordinator.setSincronizandoUI(false);
							});
						}
					}
				});
			}

			// Limpiar selección visual por si acaso
			if (view.getListaMiniaturas().getSelectedIndex() != -1)
			{
				view.getListaMiniaturas().clearSelection();
			}
			System.out.println("--- FIN actualizarModeloYVistaMiniaturas (Caso Vacío/Inválido) ---");
			return; // Salir del método
		}

		// --- 2. CALCULAR RANGO DE MINIATURAS ---
		// 2.1. Obtener configuración de cuántas mostrar antes/después
		int miniAntes = model.getMiniaturasAntes();
		int miniDespues = model.getMiniaturasDespues();
		
		//LOG dynamicLog
		StringUtils.dynamicLog("** actualizarModeloYVistaMiniaturas **", 
				"miniAntes", miniAntes,
				"miniDespues", miniDespues
				);
		
		System.out.println("\n  [actualizarMiniaturas] Valores para rango: miniAntes=" + miniAntes + ", miniDespues=" + miniDespues);
		// 2.2. Calcular límites del rango [inicio..fin], ajustados a los límites del
		// modelo principal [0..totalPrincipal-1]
		int inicioRango = Math.max(0, indiceSeleccionadoPrincipal - miniAntes);
		int finRango = Math.min(totalPrincipal - 1, indiceSeleccionadoPrincipal + miniDespues);
		System.out.println("  [actualizarMiniaturas] Rango calculado: [" + inicioRango + ".." + finRango + "]\n");

		// --- 3. RECONSTRUIR MODELO TEMPORAL DE MINIATURAS ---
		// 3.1. Preparar lista de rutas para precalentamiento
		List<Path> rutasEnRango = new ArrayList<>();
		System.out.println("  [actualizarMiniaturas] Llenando modelo TEMPORAL de miniaturas...");

		// 3.2. Iterar sobre los índices del modelo principal que caen en el rango
		for (int i = inicioRango; i <= finRango; i++)
		{
			// 3.2.1. Obtener clave del modelo principal
			String clave = modeloPrincipal.getElementAt(i);
			// 3.2.2. Añadir clave al modelo TEMPORAL
			nuevoModeloMiniaturasTemp.addElement(clave);
			// 3.2.3. Añadir ruta a la lista para precalentar (si existe)
			Path ruta = model.getRutaCompleta(clave);

			if (ruta != null)
			{
				rutasEnRango.add(ruta);
			}

			// 3.2.4. Calcular el índice RELATIVO si esta es la imagen seleccionada
			// principal
			if (i == indiceSeleccionadoPrincipal)
			{
				// El índice relativo es el tamaño actual del modelo temporal menos 1
				indiceSeleccionadoEnModeloMiniatura = i- inicioRango;//nuevoModeloMiniaturasTemp.getSize() - 1;
			}
		}
		// 3.3. Log del resultado del llenado
		System.out.println("  [actualizarMiniaturas] Modelo TEMPORAL miniaturas llenado. Tamaño: "
				+ nuevoModeloMiniaturasTemp.getSize() + ". Índice relativo calculado: "
				+ indiceSeleccionadoEnModeloMiniatura);

		// --- 4. PRE-CALENTAR CACHÉ DE MINIATURAS (ASÍNCRONO) ---
		// 4.1. Llamar al método que envía las tareas de caché al ExecutorService
		precalentarCacheMiniaturasAsync(rutasEnRango);

		// --- 5. ACTUALIZAR LA VISTA (JLIST MINIATURAS) EN EL EVENT DISPATCH THREAD
		
		// (EDT) ---
		// 5.1. Preparar variables finales para usar dentro de la lambda de invokeLater
		final DefaultListModel<String> finalNuevoModelo = nuevoModeloMiniaturasTemp; // El modelo TEMPORAL reconstruido
		final int finalIndiceRelativo = indiceSeleccionadoEnModeloMiniatura; // El índice relativo calculado
		
		// 5.2. Log antes de programar la actualización
		System.out.println("  [actualizarMiniaturas] Programando actualización de UI en EDT...");

		SwingUtilities.invokeLater( () -> { // Inicio de la lambda que se ejecuta en el EDT
		
			// 5.3. Log de inicio y re-validación de dependencias DENTRO del EDT
			System.out.println("   -> [EDT Miniaturas] Ejecutando actualización UI...");

			if (view == null || view.getListaMiniaturas() == null || listCoordinator == null)
			{
				System.err.println("ERROR [actualizarMiniaturas EDT]: Dependencias nulas en invokeLater.");
				return; // Salir si faltan componentes esenciales
			}

			// 5.4. *** MANEJO DEL FLAG SINCRONIZANDO UI ***
			// Activar el flag ANTES de modificar la JList (setModel o setSelectedIndex)
			// y desactivarlo DESPUÉS en un finally con invokeLater anidado.
			
			// ----- INICIO BLOQUE PROTEGIDO POR FLAG -----
			listCoordinator.setSincronizandoUI(true); // <-- ACTIVAR FLAG
			System.out.println("   -> [EDT Miniaturas] Flag sincronizandoUI puesto a true.");

			try
			{ // Usar try-finally para garantizar la desactivación del flag

				// 5.5. ASIGNAR EL NUEVO MODELO (TEMPORAL) A LA JLIST
				// Esta operación reemplaza el modelo anterior de la JList.
				boolean modeloCambiado = false; // Flag local para seguimiento
				// Comprobar si el modelo que tiene la JList es diferente al nuevo

				if (view.getListaMiniaturas().getModel() != finalNuevoModelo)
				{
					System.out.println("   -> [EDT Miniaturas] Llamando a view.setModeloListaMiniaturas (Tamaño: "
							+ finalNuevoModelo.getSize() + ")");
					view.setModeloListaMiniaturas(finalNuevoModelo); // Asigna el modelo reconstruido
					modeloCambiado = true;
				} else
				{
					// Esto sería raro si usamos un modelo temporal nuevo cada vez
					System.out.println("   -> [EDT Miniaturas] El modelo ya era el correcto (¿inesperado?).");
				}

				// 5.6. SELECCIONAR EL ÍNDICE RELATIVO CALCULADO EN LA JLIST
				// Solo si el índice es válido.
				if (finalIndiceRelativo >= 0 && finalIndiceRelativo < finalNuevoModelo.getSize())
				{

					// Comprobar si la selección actual es diferente O si acabamos de cambiar el
					// modelo
					if (modeloCambiado || view.getListaMiniaturas().getSelectedIndex() != finalIndiceRelativo)
					{
						System.out.println(
								"   -> [EDT Miniaturas] Llamando a setSelectedIndex(" + finalIndiceRelativo + ")");
						view.getListaMiniaturas().setSelectedIndex(finalIndiceRelativo); // Establece la selección
																							// visual
					} else
					{
						System.out.println("   -> [EDT Miniaturas] Índice relativo " + finalIndiceRelativo
								+ " ya estaba seleccionado.");
					}

					// 5.7. ASEGURAR VISIBILIDAD DEL ÍNDICE SELECCIONADO
					// Se hace en un invokeLater anidado para darle tiempo a la UI a procesar la
					// selección.
					final int indexToEnsure = finalIndiceRelativo;
					SwingUtilities.invokeLater( () -> { // Inicio invokeLater anidado para visibilidad
						// Re-validar componentes necesarios

						if (view != null && view.getListaMiniaturas() != null && view.getScrollListaMiniaturas() != null
								&& view.getScrollListaMiniaturas().isVisible())
						{

							// Re-validar índice contra el modelo actual de la JList
							if (indexToEnsure >= 0 && indexToEnsure < view.getListaMiniaturas().getModel().getSize())
							{
								System.out
										.println("     -> [EDT Miniaturas Visibilidad] Llamando a ensureIndexIsVisible("
												+ indexToEnsure + ")");

								try
								{
									view.getListaMiniaturas().ensureIndexIsVisible(indexToEnsure); // Hace scroll si es
																									// necesario
								} catch (Exception ex)
								{
									System.err.println("Error ensureVis(M):" + ex.getMessage());
								}
							} else
							{
								System.out.println(
										"WARN [EDT Visibilidad]: Índice relativo fuera de rango al asegurar visibilidad: "
												+ indexToEnsure);
							}
						}
					}); // Fin invokeLater anidado para visibilidad

				} else
				{ // El índice relativo calculado no era válido
					// Deseleccionar si había algo seleccionado en la JList

					if (view.getListaMiniaturas().getSelectedIndex() != -1)
					{
						view.getListaMiniaturas().clearSelection();
					}
					System.err.println(
							"WARN [actualizarMiniaturas EDT]: Índice relativo inválido al intentar seleccionar: "
									+ finalIndiceRelativo);
				}

				// 5.8. RE-PINTAR LA JLIST (Asegura actualización visual)
				System.out.println("   -> [EDT Miniaturas] Llamando a repaint()");
				view.getListaMiniaturas().repaint();

			} finally
			{
				// 5.9. DESACTIVAR EL FLAG (Siempre, y en otro invokeLater para seguridad)
				SwingUtilities.invokeLater( () -> {

					if (listCoordinator != null)
					{ // Re-chequear por si acaso
						listCoordinator.setSincronizandoUI(false); // El setter tiene log interno
					}
					
				}); // Fin invokeLater para desactivar flag
			} // ----- FIN BLOQUE PROTEGIDO POR FLAG -----

			// 5.10. Log final de la ejecución en EDT
			System.out.println("   -> [EDT Miniaturas] FIN Ejecución actualización UI.");
			
		}); // Fin invokeLater principal

		// --- 6. Log Final del Método ---
		System.out.println("--- FIN actualizarModeloYVistaMiniaturas ---");

	} // --- FIN actualizarModeloYVistaMiniaturas ---      
      
	  
	  // Delegados de movimiento
	  
	  public void navegarSiguienteViaCoordinador() {
	      if (listCoordinator != null) {
	          listCoordinator.seleccionarSiguiente();
	      }
	  }
	  public void navegarAnteriorViaCoordinador() {
	      if (listCoordinator != null) {
	          listCoordinator.seleccionarAnterior();
	      }
	  }
	  public void navegarPrimeroViaCoordinador() {
	      if (listCoordinator != null) {
	          listCoordinator.seleccionarPrimero();
	      }
	  }
	  public void navegarUltimoViaCoordinador() {
	      if (listCoordinator != null) {
	          listCoordinator.seleccionarUltimo();
	      }
	  }

	  
// ********************************************************************************************************* GETTERS Y SETTERS
// ***************************************************************************************************************************

	  
	// --- NUEVO: Setters para Inyección de Dependencias desde AppInitializer ---
	public void setModel(VisorModel model) { this.model = model; }
	public void setConfigurationManager(ConfigurationManager configuration) { this.configuration = configuration; }
	public void setThemeManager(ThemeManager themeManager) { this.themeManager = themeManager; }
	public void setIconUtils(IconUtils iconUtils) { this.iconUtils = iconUtils; }
	public void setServicioMiniaturas(ThumbnailService servicioMiniaturas) { this.servicioMiniaturas = servicioMiniaturas; }
	public void setExecutorService(ExecutorService executorService) { this.executorService = executorService; }
	public void setActionMap(Map<String, Action> actionMap) { this.actionMap = actionMap; }
	public void setUiConfigForView(ViewUIConfig uiConfigForView) { this.uiConfigForView = uiConfigForView; }
	public void setCalculatedMiniaturePanelHeight(int calculatedMiniaturePanelHeight) { this.calculatedMiniaturePanelHeight = calculatedMiniaturePanelHeight; }
	public void setView(VisorView view) { this.view = view; }
	public void setListCoordinator(ListCoordinator listCoordinator) { this.listCoordinator = listCoordinator; }
	public void setModeloMiniaturas(DefaultListModel<String> modeloMiniaturas) { this.modeloMiniaturas = modeloMiniaturas;}

	// Setters
	
	// --- NUEVO: Getters para que AppInitializer obtenga datos ---
	public ViewUIConfig getUiConfigForView() { return uiConfigForView; }
	public ThumbnailService getServicioMiniaturas() { return servicioMiniaturas; }
	public int getCalculatedMiniaturePanelHeight() { return calculatedMiniaturePanelHeight; }
	public IconUtils getIconUtils() { return iconUtils; } // Necesario para createNavigationActions...	  
	  
	// Getters
	public DefaultListModel<String> getModeloMiniaturas () { return modeloMiniaturas; }

    /**
     * Devuelve el número actual de elementos (imágenes) en el modelo de la lista principal.
     * Es un método seguro que comprueba la existencia del modelo y su lista interna.
     *
     * @return El tamaño (número de elementos) de la lista de imágenes,
     *         o 0 si el modelo o la lista no están inicializados o están vacíos.
     */
    public int getTamanioListaImagenes() {
        // 1. Verificar que el modelo principal ('model') no sea null
        if (model != null) {
            // 2. Obtener el DefaultListModel interno del modelo principal
            DefaultListModel<String> modeloLista = model.getModeloLista();
            // 3. Verificar que el DefaultListModel obtenido no sea null
            if (modeloLista != null) {
                // 4. Devolver el tamaño del modelo de lista
                return modeloLista.getSize();
            } else {
                // Log si el modelo interno es null (inesperado si el modelo principal no es null)
                System.err.println("WARN [getTamanioListaImagenes]: El modelo interno (modeloLista) es null.");
                return 0;
            }
        } else {
            // Log si el modelo principal es null
            System.err.println("WARN [getTamanioListaImagenes]: El modelo principal (model) es null.");
            return 0; // Devuelve 0 si el modelo principal no está listo
        }
    } // --- FIN getTamanioListaImagenes ---	


// ***************************************************************************************************** FIN GETTERS Y SETTERS
// ***************************************************************************************************************************    
    
    
} // --- FIN CLASE VisorController ---

/*
NOTAS A CONSIDERAR

limpiarUI y Deshabilitar Actions: En el método limpiarUI (punto 3.6), estás deshabilitando manualmente algunas acciones (previousImageAction, nextImageAction, etc.). Esto está bien, pero asegúrate de que también las vuelves a habilitar cuando la lista sí tiene elementos (probablemente después de una carga exitosa en el listener done de cargarListaImagenes). Una forma más robusta es que las propias clases Action (como NextImageAction, PreviousImageAction) implementen su lógica isEnabled() basándose en el estado actual del modelo o del coordinador (ej., listCoordinator.puedeNavegarSiguiente()), así no tienes que habilitarlas/deshabilitarlas manualmente desde tantos sitios.

navegarAIndice vs. ListCoordinator: El método navegarAIndice en VisorController llama directamente a view.getListaNombres().setSelectedIndex(index). Aunque esto dispara el listener correcto que a su vez llama al ListCoordinator, podría ser conceptualmente más limpio que navegarAIndice simplemente llame a listCoordinator.seleccionarIndiceYActualizarUICompleta_Helper(index). De esta forma, toda la lógica de selección pasa siempre por el coordinador. No es un error grave como está, pero es una posible mejora de coherencia.

navegarImagen: Similar a navegarAIndice, este método también manipula setSelectedIndex directamente. Podría delegar el cálculo del índice siguiente/anterior y la llamada al ListCoordinator (ej. listCoordinator.seleccionarSiguiente()). Esto haría que los métodos de navegación por botones/menú (que usan las Actions que llaman al ListCoordinator) y este método (si se usa desde otro sitio) sean más consistentes. Edición: Veo que las Actions de navegación ya llaman a los métodos del ListCoordinator, así que quizás navegarImagen ya no se usa o es un remanente. Si no se usa, podrías eliminarlo.

parseColor y crearPlaceholderMiniatura: Son métodos private. Si solo se usan dentro de VisorController, está perfecto. Si alguna vez los necesitaras en otra clase (poco probable para estos), tendrías que moverlos a una clase de utilidad.

Logs de Depuración: Aún quedan bastantes System.out.println detallados (como los de los listeners o dentro de actualizarModeloYVistaMiniaturas). Evalúa cuáles son realmente útiles para monitorizar el flujo normal y cuáles eran específicos de la depuración del problema anterior y podrían eliminarse o comentarse para reducir el "ruido" en la consola.

*/
