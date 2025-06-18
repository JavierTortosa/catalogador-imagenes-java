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
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;     // Placeholder
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

import controlador.actions.config.SetInfoBarTextFormatAction;
import controlador.actions.config.SetSubfolderReadModeAction;
import controlador.actions.tema.ToggleThemeAction;
import controlador.actions.toggle.ToggleProporcionesAction;
import controlador.actions.toggle.ToggleSubfoldersAction;
import controlador.actions.zoom.AplicarModoZoomAction;
import controlador.actions.zoom.ToggleZoomManualAction;
import controlador.commands.AppActionCommands;
import controlador.managers.ConfigApplicationManager;
import controlador.managers.InfobarImageManager;
import controlador.managers.InfobarStatusManager;
import controlador.managers.ZoomManager;
import controlador.utils.ComponentRegistry;
import controlador.worker.BuscadorArchivosWorker;
// --- Imports de Modelo, Servicios y Vista ---
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.ProjectManager;
import servicios.image.ThumbnailService;
import servicios.zoom.ZoomModeEnum;
import utils.StringUtils;
import vista.VisorView;
import vista.config.UIDefinitionService;
import vista.config.ViewUIConfig;
import vista.dialogos.ProgresoCargaDialog;
import vista.panels.ImageDisplayPanel;
import vista.renderers.MiniaturaListCellRenderer;
import vista.theme.Tema;
import vista.theme.ThemeApplier;
import vista.theme.ThemeManager;
import vista.util.IconUtils;
import vista.util.ImageDisplayUtils;

//import servicios.zoom.ZoomModeEnum;


/**
 * Controlador principal para el Visor de Imágenes (Versión con 2 JList sincronizadas).
 * Orquesta la interacción entre Modelo y Vista, maneja acciones y lógica de negocio.
 */
public class VisorController implements ActionListener, ClipboardOwner, KeyEventDispatcher {

    // --- 1. Referencias a Componentes del Sistema ---
	private StringUtils stringUtils;				// Utilidades de Strings y log dinamico con dynamicLogç

	public VisorModel model;						// El modelo de datos principal de la aplicación
    public VisorView view;							// Clase principal de la Interfaz Grafica
    private ConfigurationManager configuration;		// Gestor del archivo de configuracion
    private IconUtils iconUtils;					// utilidad para cargar y gestionar iconos de la aplicación
    private ThemeManager themeManager;				// Gestor de tema visual de la interfaz
    private ThumbnailService servicioMiniaturas;	// Servicio para gestionar las miniaturas
    private ListCoordinator listCoordinator;		// El coordinador para la selección y navegación en las listas
    private ProjectManager projectManager;			// Gestor de proyectos (imagenes favoritas)
    private ZoomManager zoomManager;				// Responsable de los metodos de zoom
    private ComponentRegistry registry;
    private InfobarImageManager infobarImageManager; 
    private InfobarStatusManager statusBarManager;

    // --- Comunicación con AppInitializer ---
    private ViewUIConfig uiConfigForView;			// Necesario para el renderer (para colores y config de thumbWidth/Height)
    private int calculatedMiniaturePanelHeight;		//

    private ExecutorService executorService;		 
    
    // --- 2. Estado Interno del Controlador ---
    private int lastMouseX, lastMouseY;
    private Future<?> cargaImagenesFuture;
    // private Future<?> cargaMiniaturasFuture; // Eliminado
    private Future<?> cargaImagenPrincipalFuture;
//    private Path carpetaRaizActual = null;
    private volatile boolean estaCargandoLista = false;
    private volatile boolean seleccionInicialEnCurso = false; // Flag para ignorar listener durante selección inicial
    
    private DefaultListModel<String> modeloMiniaturas;
    
    private Map<String, Action> actionMap;

    // Constantes de seguridad de imagenes antes y despues de la seleccionada
    public static final int DEFAULT_MINIATURAS_ANTES_FALLBACK = 8;
    public static final int DEFAULT_MINIATURAS_DESPUES_FALLBACK = 8;
    
    
    private boolean zoomManualEstabaActivoAntesDeError = false;
    
    private ConfigApplicationManager configAppManager;
    
    private Map<String, JButton> botonesPorNombre;
    
    private Map<String, JMenuItem> menuItemsPorNombre;
    
    private volatile boolean cargaInicialEnCurso = false;
    
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
        if (registry.get("list.miniaturas") == null) {
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
            
            if (this.model != null) { // Asegurarse que el modelo existe antes de intentar ponerle null
                this.model.setCarpetaRaizActual(null); // <<< CAMBIO AQUÍ: Actualizar el modelo
            } else {
                // Esto sería un problema de orden de inicialización muy grave si model es null aquí
                System.err.println("ERROR CRÍTICO [establecerCarpetaRaizDesdeConfigInternal]: VisorModel también es null. No se puede establecer carpetaRaizActual a null.");
            }
            
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
                	
                	this.model.setCarpetaRaizActual(candidatePath);
                	
                    System.out.println("      -> Carpeta raíz establecida a: " +
                    		//this.carpetaRaizActual);
                    		this.model.getCarpetaRaizActual());
                } else {
                    // 2.3.1.2.2. Si la ruta existe pero no es un directorio, loguear advertencia y poner `carpetaRaizActual` a null.
                    System.err.println("WARN [establecerCarpetaRaizDesdeConfigInternal]: La ruta en config no es un directorio: " + folderInitPath);
                    this.model.setCarpetaRaizActual(null);
                }
            // 2.3.2. Capturar excepciones (p.ej., formato de ruta inválido).
            } catch (Exception e) {
                // 2.3.2.1. Loguear el error y poner `carpetaRaizActual` a null.
                System.err.println("WARN [establecerCarpetaRaizDesdeConfigInternal]: Ruta de carpeta inicial inválida en config: '" + folderInitPath + "' - Error: " + e.getMessage());
                
                this.model.setCarpetaRaizActual(null);
                
            }
        // 2.4. Si la ruta en la configuración estaba vacía.
        } else {
            // 2.4.1. Log indicando que no había ruta definida.
            System.out.println("      -> No hay ruta de inicio definida en la configuración.");
            // 2.4.2. Asegurar que `carpetaRaizActual` sea null.
            this.model.setCarpetaRaizActual(null);
        }

        // --- SECCIÓN 3: Log Final ---
        // 3.1. Indicar si se estableció una carpeta raíz o no.
        
        if (this.model.getCarpetaRaizActual() != null) {
        	
            System.out.println("    [EDT Internal] Fin establecerCarpetaRaizDesdeConfigInternal. Raíz actual: " 
            		//+ this.carpetaRaizActual);
            		+ this.model.getCarpetaRaizActual());
            
        } else {
        	System.out.println("    [EDT Internal] Fin establecerCarpetaRaizDesdeConfigInternal. No se estableció carpeta raíz válida en MODELO.");
        }

    } // --- FIN establecerCarpetaRaizDesdeConfigInternal ---

    
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
	                
	                this.model.setCarpetaRaizActual(folderPath);// <<< CAMBIO AQUÍ
	                
	                System.out.println("    -> Carpeta inicial válida encontrada: " + folderPath);
	            } else {
	                // 2.4.2.2. Log si la ruta existe pero no es un directorio.
	                 System.err.println("WARN [cargarEstadoInicialInternal]: Carpeta inicial en config no es un directorio válido: " + folderInit);
	                 
	                 this.model.setCarpetaRaizActual(null);// <<< CAMBIO AQUÍ
	                 
	            }
	        // 2.4.3. Capturar cualquier excepción durante la conversión/verificación de la ruta.
	        } catch (Exception e) {
	            System.err.println("WARN [cargarEstadoInicialInternal]: Ruta de carpeta inicial inválida en config: " + folderInit + " - " + e.getMessage());
	            
	            this.model.setCarpetaRaizActual(null);// <<< CAMBIO AQUÍ
	            
	        }
	    } else {
	        // 2.5. Log si la clave "inicio.carpeta" no estaba definida en la configuración.
	        System.out.println("    -> No hay definida una carpeta inicial en la configuración.");
	        
	        this.model.setCarpetaRaizActual(null); // <<< CAMBIO AQUÍ
	    }
	
	    // --- SECCIÓN 3: Cargar Lista de Imágenes o Limpiar UI ---
	    // 3.1. Proceder a cargar la lista SOLO si se encontró una carpeta inicial válida.
	    
	    if (carpetaValida && this.model.getCarpetaRaizActual() != null) { // <<< CAMBIO AQUÍ
	
	    	
	        // 3.1.1. Log indicando que se procederá a la carga.
	        
	    	System.out.println("    -> Cargando lista para carpeta inicial (desde MODELO): " + this.model.getCarpetaRaizActual()); // <<< CAMBIO AQUÍ
	        
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
	        cargarListaImagenes(imagenInicialKey, null);
	
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
     * Configura todos los listeners de la aplicación, asegurando que la UI se
     * actualice en tiempo real tras la interacción del usuario.
     */
	void configurarListenersVistaInternal() {
        if (view == null || listCoordinator == null || model == null || registry == null) {
            System.err.println("WARN [configurarListenersVistaInternal]: Dependencias críticas nulas. Abortando.");
            return;
        }
        System.out.println("[Controller Internal] Configurando Listeners de Vista...");

        // --- SECCIÓN 1: LISTENERS DE SELECCIÓN DE LISTAS ---
        JList<String> listaNombres = registry.get("list.nombresArchivo");
        if (listaNombres != null) {
            for (javax.swing.event.ListSelectionListener lsl : listaNombres.getListSelectionListeners()) {
                listaNombres.removeListSelectionListener(lsl);
            }
            listaNombres.addListSelectionListener(e -> {
                // ¡CONDICIÓN MODIFICADA!
                if (!e.getValueIsAdjusting() && listCoordinator != null && !listCoordinator.isSincronizandoUI() && !cargaInicialEnCurso) {
                    listCoordinator.seleccionarDesdeNombres(listaNombres.getSelectedIndex());
                }
            });
        }
        JList<String> listaMiniaturas = registry.get("list.miniaturas");
        if (listaMiniaturas != null) {
            for (javax.swing.event.ListSelectionListener lsl : listaMiniaturas.getListSelectionListeners()) {
                listaMiniaturas.removeListSelectionListener(lsl);
            }
            listaMiniaturas.addListSelectionListener(e -> {
                 // ¡CONDICIÓN MODIFICADA!
                 if (!e.getValueIsAdjusting() && listCoordinator != null && !listCoordinator.isSincronizandoUI() && !cargaInicialEnCurso) {
                    int indiceRelativo = listaMiniaturas.getSelectedIndex();
                    if (indiceRelativo != -1) {
                        String clave = listaMiniaturas.getModel().getElementAt(indiceRelativo);
                        int indicePrincipal = model.getModeloLista().indexOf(clave);
                        listCoordinator.seleccionarDesdeMiniaturas(indicePrincipal);
                    }
                 }
            });
        }

        // --- SECCIÓN 2: LISTENERS DE RATÓN PARA IMAGEN PRINCIPAL (CON LA ACTUALIZACIÓN DE BARRAS) ---
        
        JLabel etiquetaImagenPrincipal = registry.get("label.imagenPrincipal");
        if (etiquetaImagenPrincipal != null) {
            // Limpiamos listeners para evitar duplicados si se llama a este método más de una vez.
            for(java.awt.event.MouseWheelListener l : etiquetaImagenPrincipal.getMouseWheelListeners()) etiquetaImagenPrincipal.removeMouseWheelListener(l);
            for(java.awt.event.MouseListener l : etiquetaImagenPrincipal.getMouseListeners()) etiquetaImagenPrincipal.removeMouseListener(l);
            for(java.awt.event.MouseMotionListener l : etiquetaImagenPrincipal.getMouseMotionListeners()) etiquetaImagenPrincipal.removeMouseMotionListener(l);

            // --- Listener para la rueda del ratón ---
            etiquetaImagenPrincipal.addMouseWheelListener(e -> {
                // Si el Modo Paneo está desactivado, la rueda navega entre imágenes.
                if (!model.isZoomHabilitado()) {
                    listCoordinator.seleccionarSiguienteOAnterior(e.getWheelRotation());
                    return; // Salimos, no hay más que hacer.
                }

                // Si el Modo Paneo SÍ está activo, la rueda gestiona zoom y paneo.
                zoomManager.manejarRuedaInteracciona(e);
                
                // --- AÑADIDO CLAVE ---
                // Tras la interacción, notificamos a las barras para que se actualicen.
                if (infobarImageManager != null) infobarImageManager.actualizar();
                if (statusBarManager != null) statusBarManager.actualizar();
            });

            // --- Listener para los clics (iniciar paneo) ---
            etiquetaImagenPrincipal.addMouseListener(new MouseAdapter() {
                public void mousePressed(java.awt.event.MouseEvent e) {
                    if (model.isZoomHabilitado()) {
                        zoomManager.iniciarPaneo(e);
                    }
                }
            });
            
            // --- Listener para el arrastre (continuar paneo) ---
            etiquetaImagenPrincipal.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(java.awt.event.MouseEvent e) {
                    if (model.isZoomHabilitado()) {
                        zoomManager.continuarPaneo(e);
                        
                        // --- AÑADIDO CLAVE ---
                        // También actualizamos las barras durante el paneo.
                        if (infobarImageManager != null) infobarImageManager.actualizar();
                        if (statusBarManager != null) statusBarManager.actualizar();
                    }
                }
            });
        }
        
        configurarListenerDePrimerRenderizado();
    }
    // --- FIN del metodo configurarListenersVistaInternal ---
	
    
    /**
     * Configura un listener que se dispara UNA SOLA VEZ para corregir el zoom inicial.
     * Espera a que el panel de la imagen tenga un tamaño válido y haya una imagen cargada,
     * y entonces fuerza un refresco del zoom y de las barras de información.
     */
    private void configurarListenerDePrimerRenderizado() {
        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
        if (displayPanel == null) {
            System.err.println("WARN [configurarListenerDePrimerRenderizado]: ImageDisplayPanel no encontrado en el registro.");
            return;
        }

        displayPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                // Se activa cuando el panel es redimensionado, lo que incluye la primera vez que obtiene un tamaño.
                if (displayPanel.getWidth() > 0 && displayPanel.getHeight() > 0 && model != null && model.getCurrentImage() != null) {
                    
                    System.out.println("--- [Listener Primer Renderizado]: Panel listo (" + displayPanel.getWidth() + "x" + displayPanel.getHeight() + "). Forzando refresco de zoom y barras de info. ---");
                    
                    if (zoomManager != null) {
                        // 1. Calcula y aplica el zoom correcto en el modelo
                        zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());

                        // 2. Notifica a las barras para que lean el nuevo estado de zoom correcto
                        if (infobarImageManager != null) infobarImageManager.actualizar();
                        if (statusBarManager != null) statusBarManager.actualizar();
                    }
                    
                    // 3. ¡Importante! Eliminar el listener después de que se haya ejecutado una vez.
                    displayPanel.removeComponentListener(this);
                    System.out.println("--- [Listener Primer Renderizado]: Tarea completada. Listener eliminado. ---");
                }
            }
        });
    } // --- FIN del metodo configurarListenerDePrimerRenderizado ---
    
    
    /**
     * Método llamado por Actions (como ToggleUIElementVisibilityAction o las Actions
     * para mostrar/ocultar MenuBar, ToolBar, etc.) para notificar que el estado de
     * visibilidad de un elemento o zona de la UI ha cambiado y que la vista necesita
     * ser actualizada.
     *
     * @param uiElementIdentifier El identificador de la zona, panel o componente principal
     *                            de la UI que necesita ser actualizado. Ejemplos:
     *                            "REFRESH_INFO_BAR_SUPERIOR", "Barra_de_Menu".
     * @param configKey           (Principalmente informativo) La clave de configuración específica
     *                            que fue modificada por la Action que originó la llamada.
     * @param nuevoEstadoVisible  El nuevo estado booleano (ej. true para visible) que la Action
     *                            ya guardó en la configuración. Este valor SE USA para los
     *                            componentes principales de la UI.
     */
    public void solicitarActualizacionInterfaz(String uiElementIdentifier, String configKey, boolean nuevoEstadoVisible) {
        // 0. --- LOG DE ENTRADA Y VALIDACIÓN DE DEPENDENCIAS ---
        System.out.println(
            "[VisorController.solicitarActualizacionInterfaz] Solicitud recibida." +
            "\n  UI Element Identifier: '" + uiElementIdentifier + "'" +
            (configKey != null ? "\n  Config Key Afectada: '" + configKey + "'" : "") +
            "\n  Nuevo Estado Lógico (desde Action): " + nuevoEstadoVisible
        );

        if (view == null) {
            System.err.println("  ERROR CRÍTICO [VisorController.solicitarActualizacionInterfaz]: VisorView es null. No se puede proceder.");
            return;
        }
        if (uiElementIdentifier == null || uiElementIdentifier.trim().isEmpty()) {
            System.err.println("  WARN [VisorController.solicitarActualizacionInterfaz]: uiElementIdentifier es nulo o vacío.");
            return;
        }

        boolean necesitaRevalidateRepaintGeneralDelFrame = false;
        
        // Manejo específico para la visibilidad de botones individuales en toolbars.
        if (configKey != null && configKey.startsWith("interfaz.boton.")) {
            // La configKey es del tipo "interfaz.boton.edicion.imagen_rotar_izq.visible"
            // Extraemos la clave base para buscar el botón en el mapa.
            String buttonBaseKey = configKey.replace(".visible", "");
            
            System.out.println("  -> Solicitud detectada para botón de toolbar. Clave Base: '" + buttonBaseKey + "'");

            if (this.botonesPorNombre != null) {
                JButton botonAfectado = this.botonesPorNombre.get(buttonBaseKey);

                if (botonAfectado != null) {
                    // ¡ACCIÓN CLAVE! Aplicar el cambio de visibilidad al componente Swing.
                    botonAfectado.setVisible(nuevoEstadoVisible);
                    System.out.println("    -> Visibilidad del JButton '" + buttonBaseKey + "' establecida a: " + nuevoEstadoVisible);

                    // Revalidar el contenedor de la toolbar para que se ajuste.
                    this.revalidateToolbarContainer();
                    System.out.println("    -> Contenedor de toolbars revalidado.");

                    // La solicitud ha sido manejada completamente.
                    return; // Salir del método.
                } else {
                    System.err.println("  ERROR: No se encontró el botón con clave base '" + buttonBaseKey + "' en el mapa de la vista.");
                }
            } else {
                System.err.println("  ERROR: El mapa de botones en la vista es nulo.");
            }
        }
        
        UIDefinitionService uiDefs = new UIDefinitionService();
        List<String> toolbarKeys = uiDefs.getToolbarKeys();

        // Comprobar si el uiElementIdentifier corresponde a una barra de herramientas.
        if (toolbarKeys.contains(uiElementIdentifier)) {
            System.out.println("  -> Solicitud detectada para barra de herramientas completa. UI ID: '" + uiElementIdentifier + "'");

            if (view != null && view.getToolbars() != null) {
                // Obtener la JToolBar específica del mapa de la vista.
                JToolBar toolbarAfectada = view.getToolbars().get(uiElementIdentifier);

                if (toolbarAfectada != null) {
                    // ¡ACCIÓN CLAVE! Aplicar el cambio de visibilidad a la JToolBar.
                    toolbarAfectada.setVisible(nuevoEstadoVisible);
                    System.out.println("    -> Visibilidad de la JToolBar '" + uiElementIdentifier + "' establecida a: " + nuevoEstadoVisible);
                    
                    // Revalidar el contenedor para que el espacio se ajuste.
//                    view.revalidateToolbarContainer();
                    this.revalidateToolbarContainer();
                    necesitaRevalidateRepaintGeneralDelFrame = true;

                } else {
                    System.err.println("  ERROR: No se encontró la JToolBar con ID '" + uiElementIdentifier + "' en el mapa de la vista.");
                }
            } else {
                System.err.println("  ERROR: Vista o mapa de toolbars nulo.");
            }
        
        } else {
        
        // 3. --- DESPACHO DE LA SOLICITUD BASADO EN uiElementIdentifier ---
        switch (uiElementIdentifier) {
            // 2.1. CASOS PARA LAS BARRAS DE INFORMACIÓN:
            case "REFRESH_INFO_BAR_SUPERIOR":
            	System.out.println("  -> [VisorController] UI ID para InfoBar Superior. Delegando a infobarImageManager.");
            	if (infobarImageManager != null) {
                    infobarImageManager.actualizar();
                }
            	break;
            	
            case "REFRESH_INFO_BAR_INFERIOR":
            	System.out.println("  -> [VisorController] UI ID para InfoBar Inferior. Delegando a StatusBarManager.");
                if (statusBarManager != null) {
                    statusBarManager.actualizar();
                }
                break;

            // 2.2. CASOS PARA OTROS COMPONENTES PRINCIPALES DE LA UI:
            case "Barra_de_Menu": // uiElementIdentifier usado por ToggleMenuBarAction
                System.out.println("  -> [VisorController] UI ID: Barra_de_Menu. Visibilidad a: " + nuevoEstadoVisible);
                if (view.getJMenuBar() != null && view.getJMenuBar().isVisible() != nuevoEstadoVisible) {
                    view.setJMenuBarVisible(nuevoEstadoVisible);
                    necesitaRevalidateRepaintGeneralDelFrame = true;
                }
                break;

            case "Barra_de_Botones": // uiElementIdentifier usado por ToggleToolBarAction
                System.out.println("  -> [VisorController] UI ID: Barra_de_Botones. Visibilidad a: " + nuevoEstadoVisible);
                if (view.getPanelDeBotones() != null && view.getPanelDeBotones().isVisible() != nuevoEstadoVisible) {
                    view.setToolBarVisible(nuevoEstadoVisible);
                    necesitaRevalidateRepaintGeneralDelFrame = true;
                }
                break;
                
            case AppActionCommands.CMD_REFRESH_TOOLBARS:
                System.out.println("  -> [VisorController] UI ID: REFRESH_TOOLBARS. Delegando a ToolbarManager...");
                // if (toolbarManager != null) {
                //    toolbarManager.refrescarVisibilidadBarras();
                // }
                break;

            case "mostrar_ocultar_la_lista_de_archivos":
                System.out.println("  -> [VisorController] UI ID: mostrar_ocultar_la_lista_de_archivos. Visibilidad a: " + nuevoEstadoVisible);
                
                // <<< INICIO DEL CAMBIO >>>
                
                // 1. Obtener el panel desde el registro.
                JPanel panelIzquierdo = registry.get("panel.izquierdo.listaArchivos");

                if (panelIzquierdo != null) {
                    if (panelIzquierdo.isVisible() != nuevoEstadoVisible) {
                        panelIzquierdo.setVisible(nuevoEstadoVisible);
                        
                        // Lógica del divisor del SplitPane
                        JSplitPane splitPane = registry.get("splitpane.main");
                        if (splitPane != null) {
                            if (nuevoEstadoVisible) {
                                SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.25));
                            } else {
                                splitPane.resetToPreferredSizes();
                            }
                        }
                        
                        necesitaRevalidateRepaintGeneralDelFrame = true;
                    } else {
                        System.out.println("  -> El panel de lista de archivos ya está en el estado de visibilidad deseado.");
                    }
                } else {
                    System.err.println("  ERROR: 'panel.izquierdo.listaArchivos' no encontrado en el registro.");
                }
                // <<< FIN DEL CAMBIO >>>
                break;

            case "imagenes_en_miniatura":
                System.out.println("  -> [VisorController] UI ID: imagenes_en_miniatura. Visibilidad a: " + nuevoEstadoVisible);
                
                // <<< INICIO DEL CAMBIO >>>
                
                // 1. Obtener el JScrollPane desde el registro.
                JScrollPane scrollMiniaturas = registry.get("scroll.miniaturas");

                if (scrollMiniaturas != null) {
                    if (scrollMiniaturas.isVisible() != nuevoEstadoVisible) {
                        scrollMiniaturas.setVisible(nuevoEstadoVisible);
                        necesitaRevalidateRepaintGeneralDelFrame = true;
                    } else {
                         System.out.println("  -> El panel de miniaturas ya está en el estado de visibilidad deseado.");
                    }
                } else {
                    System.err.println("  ERROR: 'scroll.miniaturas' no encontrado en el registro.");
                }
                // <<< FIN DEL CAMBIO >>>
                break;
            
            // Este caso se activa si ToggleLocationBarAction usa "linea_de_ubicacion_del_archivo" como uiElementId.
            // Si ToggleLocationBarAction fue modificada para usar "REFRESH_INFO_BAR_INFERIOR",
            // entonces este 'case' específico ya no es necesario aquí.
            // Por coherencia con el menú "Vista", lo mantenemos, asumiendo que puede ser un panel diferente
            // o que decidiste mantener este uiElementId específico para ella.
//            case "linea_de_ubicacion_del_archivo":
//                System.out.println("  -> [VisorController] UI ID: linea_de_ubicacion_del_archivo.");
//                // Si "Linea de Ubicacion" es PARTE de la barra inferior y su Action ahora usa
//                // "REFRESH_INFO_BAR_INFERIOR", este case no se dispararía por esa Action.
//                // Si es un panel separado que VisorView maneja con setLocationBarVisible:
//                if (view.getTextoRuta() != null && view.getTextoRuta().isVisible() != nuevoEstadoVisible) { // Suponiendo que getTextoRuta() es el componente
//                     view.setLocationBarVisible(nuevoEstadoVisible);
//                     necesitaRevalidateRepaintGeneralDelFrame = true;
//                     
//                } else if (infobarImageManager != null || statusBarManager != null) {
//                	if (infobarImageManager != null) {
//                			infobarImageManager.actualizar();
//                	}
//                 	if (statusBarManager != null) {
//                 		statusBarManager.actualizar();
//                 	}
//                }
//                break;

            // 2.3. CASO POR DEFECTO:
            default:
                System.err.println("  WARN [VisorController.solicitarActualizacionInterfaz]: uiElementIdentifier no reconocido: '" +
                                   uiElementIdentifier + "'. No se realizó acción de UI específica.");
                break;
	        }
	    }

     // 3. --- REVALIDACIÓN Y REPINTADO DEL FRAME (SI ES NECESARIO) ---
        if (necesitaRevalidateRepaintGeneralDelFrame && view != null) {
            System.out.println("  -> [VisorController] Programando revalidate y repaint del frame principal.");
            
            // Usamos la variable 'view' directamente, ya que es el JFrame.
            javax.swing.SwingUtilities.invokeLater(() -> {
                if (view != null) { // Doble chequeo por si se cierra la app mientras el invokeLater espera
                    view.revalidate();
                    view.repaint();
                }
            });
        }


        // 4. --- LOG FINAL ---
        System.out.println("[VisorController.solicitarActualizacionInterfaz] Procesamiento finalizado para UI ID: " + uiElementIdentifier);
    }// FIN del metodo solicitarActualizacionInterfaz
    
    
    /**
     * Revalida y repinta el panel que contiene las barras de herramientas.
     * Es útil después de mostrar u ocultar una barra de herramientas individual o un botón.
     */
    public void revalidateToolbarContainer() {
        if (registry == null) {
            System.err.println("ERROR [revalidateToolbarContainer]: ComponentRegistry es nulo.");
            return;
        }
        
        // Obtenemos el contenedor de las barras de herramientas desde el registro.
        // ViewBuilder debe haberlo registrado con esta clave.
        JPanel toolbarContainer = registry.get("container.toolbars");
        
        if (toolbarContainer != null) {
            // Revalidate recalcula el layout, repaint lo redibuja.
            toolbarContainer.revalidate();
            toolbarContainer.repaint();
        } else {
            System.err.println("WARN [revalidateToolbarContainer]: 'container.toolbars' no encontrado en el registro.");
        }
    } // --- FIN del metodo revalidateToolbarContainer ---
    
    	
   
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
     * Orquesta el proceso de apagado limpio de la aplicación.
     * Es llamado cuando el usuario cierra la ventana principal.
     */
    public void shutdownApplication() {
        System.out.println("--- [Controller] Iniciando apagado de la aplicación ---");
        
        // --- 1. Guardar la configuración ---
        // Guardar el estado de la ventana (tamaño, posición)
        if (view != null && configuration != null) {
            boolean isMaximized = (view.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
            configuration.setString(ConfigKeys.WINDOW_MAXIMIZED, String.valueOf(isMaximized));
            
            java.awt.Rectangle normalBounds = view.getLastNormalBounds();
            if (normalBounds != null) {
                configuration.setString(ConfigKeys.WINDOW_X, String.valueOf(normalBounds.x));
                configuration.setString(ConfigKeys.WINDOW_Y, String.valueOf(normalBounds.y));
                configuration.setString(ConfigKeys.WINDOW_WIDTH, String.valueOf(normalBounds.width));
                configuration.setString(ConfigKeys.WINDOW_HEIGHT, String.valueOf(normalBounds.height));
            }
        }
        
        // Guardar el resto de la configuración (última imagen, etc.)
        guardarConfiguracionActual();

        // --- 2. Apagar el ExecutorService de forma ordenada ---
        if (executorService != null && !executorService.isShutdown()) {
           System.out.println("  -> Apagando ExecutorService...");
           executorService.shutdown(); // No acepta nuevas tareas, intenta terminar las actuales.
           try {
               // Espera un máximo de 2 segundos a que las tareas en curso terminen.
               if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                   System.err.println("  -> ExecutorService no terminó a tiempo, forzando apagado con shutdownNow()...");
                   executorService.shutdownNow(); // Intenta cancelar las tareas en ejecución.
               } else {
                   System.out.println("  -> ExecutorService terminado ordenadamente.");
               }
           } catch (InterruptedException ex) {
               System.err.println("  -> Hilo principal interrumpido mientras esperaba al ExecutorService.");
               executorService.shutdownNow();
               Thread.currentThread().interrupt();
           }
        }
        
        // --- 3. Forzar la salida de la JVM ---
        // Esto asegura que la aplicación se cierre incluso si otro hilo no-demonio
        // estuviera bloqueando la salida. Ahora que hemos limpiado todo, es seguro.
        System.out.println("  -> Apagado limpio completado. Saliendo de la JVM con System.exit(0).");
        System.exit(0);
        
    } // --- FIN del metodo shutdownApplication ---
    
    
    /**
     * Método helper PRIVADO para guardar el estado actual de la ventana (posición,
     * tamaño, estado maximizado) en el ConfigurationManager en memoria.
     * Se llama desde el Shutdown Hook.
     */
    private void guardarEstadoVentanaEnConfig() {
        // 1. Validar que la Vista y la Configuración existan.
        if (view == null || configuration == null) {
            System.out.println("  [Hook - Ventana] No se pudo guardar estado (Vista=" + (view == null ? "null" : "existe") + 
                               ", Config=" + (configuration == null ? "null" : "existe") + ").");
            return;
        }
        System.out.println("  [Hook - Ventana] Guardando estado de la ventana en config...");

        try {
            // 2.1. Comprobar si la ventana está maximizada.
            boolean isMaximized = (view.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
            // 2.2. Guardar el estado de maximización en ConfigurationManager.
            configuration.setString(ConfigKeys.WINDOW_MAXIMIZED, String.valueOf(isMaximized));
            System.out.println("    -> Estado Maximized guardado en config: " + isMaximized);

            // 2.3. Obtener y guardar SIEMPRE los "últimos bounds normales"
            //      Estos bounds son los que tenía la ventana la última vez que estuvo en estado NORMAL.
            Rectangle normalBoundsToSave = view.getLastNormalBounds(); 
            
            if (normalBoundsToSave != null) {
                configuration.setString(ConfigKeys.WINDOW_X, String.valueOf(normalBoundsToSave.x));
                configuration.setString(ConfigKeys.WINDOW_Y, String.valueOf(normalBoundsToSave.y));
                configuration.setString(ConfigKeys.WINDOW_WIDTH, String.valueOf(normalBoundsToSave.width));
                configuration.setString(ConfigKeys.WINDOW_HEIGHT, String.valueOf(normalBoundsToSave.height));
                System.out.println("    -> Últimos Bounds Normales guardados en config: " + normalBoundsToSave);
            } else {
                // Esto sería inesperado si lastNormalBounds se inicializa bien en VisorView
                // y la vista existe.
                System.err.println("  WARN [Hook - Ventana]: view.getLastNormalBounds() devolvió null. No se pudieron guardar bounds normales detallados.");
                // Considera si quieres guardar valores por defecto aquí o dejar las claves como están en config.
                // Si las dejas, ConfigurationManager usará sus defaults al leer si las claves no existen.
            }

        } catch (Exception e) {
            System.err.println("  [Hook - Ventana] ERROR al guardar estado de la ventana: " + e.getMessage());
            e.printStackTrace();
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
    
    
    
    
 // ******************************************************************************************************************* CARGA      

    /**
     * Carga o recarga la lista de imágenes desde disco para una carpeta específica,
     * utilizando un SwingWorker para no bloquear el EDT. Muestra un diálogo de
     * progreso durante la carga. Una vez cargada la lista: 
     * - Actualiza el modelo principal de datos (`VisorModel`). 
     * - Actualiza las JList en la vista (`VisorView`). 
     * - Inicia el precalentamiento ASÍNCRONO y DIRIGIDO del caché de miniaturas. 
     * - Selecciona una imagen específica (si se proporciona `claveImagenAMantener`) 
     *   o la primera imagen de la lista. 
     * - Ejecuta un callback opcional al finalizar con éxito.
     *
     * @param claveImagenAMantener La clave única (ruta relativa) de la imagen que
     *                             se intentará seleccionar después de que la lista
     *                             se cargue. Si es `null`, se seleccionará la
     *                             primera imagen (índice 0).
     * @param alFinalizarConExito Un objeto Runnable cuya lógica se ejecutará en el EDT
     *                            después de que la carga y el procesamiento de la lista
     *                            hayan finalizado con éxito. Puede ser `null`.
     */
    public void cargarListaImagenes(String claveImagenAMantener, Runnable alFinalizarConExito) {
        System.out.println("\n-->>> INICIO cargarListaImagenes(String, Runnable) | Mantener Clave: " + claveImagenAMantener);

        // --- 1. VALIDACIONES PREVIAS ---
        if (configuration == null || model == null || executorService == null || executorService.isShutdown() || view == null) {
            System.err.println("ERROR [cargarListaImagenes]: Dependencias nulas (Config, Modelo, Executor o Vista) o Executor apagado.");
            if (view != null) SwingUtilities.invokeLater(this::limpiarUI);
            return;
        }

        // --- 2. ESTABLECER FLAGS DE ESTADO ---
        this.estaCargandoLista = true;
        this.cargaInicialEnCurso = true; // <-- ¡NUEVO! Bloquea los listeners de usuario

        // --- 3. CANCELAR TAREAS ANTERIORES ---
        if (cargaImagenesFuture != null && !cargaImagenesFuture.isDone()) {
            System.out.println("  -> Cancelando tarea de carga de lista anterior...");
            cargaImagenesFuture.cancel(true); 
        }

        // --- 4. DETERMINAR PARÁMETROS DE BÚSQUEDA ---
        final boolean mostrarSoloCarpeta = model.isMostrarSoloCarpetaActual();
        int depth = mostrarSoloCarpeta ? 1 : Integer.MAX_VALUE;
        Path pathDeInicioWalk = null;
        if (mostrarSoloCarpeta) {
            String claveReferenciaParaCarpeta = claveImagenAMantener != null ? claveImagenAMantener : model.getSelectedImageKey();
            Path rutaImagenReferencia = claveReferenciaParaCarpeta != null ? model.getRutaCompleta(claveReferenciaParaCarpeta) : null;
            if (rutaImagenReferencia != null && Files.isRegularFile(rutaImagenReferencia)) {
                pathDeInicioWalk = rutaImagenReferencia.getParent();
            }
            if (pathDeInicioWalk == null || !Files.isDirectory(pathDeInicioWalk)) {
                pathDeInicioWalk = this.model.getCarpetaRaizActual();
            }
        } else {
            pathDeInicioWalk = this.model.getCarpetaRaizActual();
        }

        // --- 5. VALIDAR PATH DE INICIO Y PROCEDER ---
        if (pathDeInicioWalk != null && Files.isDirectory(pathDeInicioWalk)) {
            if (this.servicioMiniaturas != null) {
                System.out.println("  -> Limpiando caché de ThumbnailService antes de nueva carga...");
                this.servicioMiniaturas.limpiarCache();
            }
            
            final Path finalStartPath = pathDeInicioWalk;
            final Path finalRutaRaizParaRelativizar = this.model.getCarpetaRaizActual();

            // --- 6. LIMPIEZA INICIAL DE LA UI ---
            limpiarUI(); // Llama a tu método de limpieza, que ahora está protegido por el flag

            // --- 7. CREAR DIÁLOGO Y WORKER ---
            final ProgresoCargaDialog dialogo = new ProgresoCargaDialog(view, null);
            final BuscadorArchivosWorker worker = new BuscadorArchivosWorker(
                finalStartPath, 
                depth, 
                finalRutaRaizParaRelativizar,
                this::esArchivoImagenSoportado, 
                dialogo
            );
            dialogo.setWorkerAsociado(worker); 
            this.cargaImagenesFuture = worker;

            // --- 8. CONFIGURAR EL LISTENER PARA CUANDO EL WORKER TERMINE ---
            worker.addPropertyChangeListener(evt -> {
                if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE.equals(evt.getNewValue())) {
                    if (dialogo != null) dialogo.cerrar();
                    if (worker.isCancelled()) {
                        System.out.println("    -> Tarea CANCELADA por el usuario.");
                        limpiarUI();
                        if (view != null) view.setTituloPanelIzquierdo("Carga Cancelada");
                        this.estaCargandoLista = false; 
                        this.cargaInicialEnCurso = false; // Asegurarse de desactivar el flag
                        return; 
                    }

                    try {
                        Map<String, Path> mapaResultado = worker.get();
                        if (mapaResultado != null) {
                            System.out.println("    WORKER HA TERMINADO. Número de archivos encontrados: " + mapaResultado.size());
                            DefaultListModel<String> nuevoModeloListaPrincipal = new DefaultListModel<>();
                            List<String> clavesOrdenadas = new ArrayList<>(mapaResultado.keySet());
                            java.util.Collections.sort(clavesOrdenadas);
                            clavesOrdenadas.forEach(nuevoModeloListaPrincipal::addElement);
                            
                            model.actualizarListaCompleta(nuevoModeloListaPrincipal, mapaResultado);
                            if (view != null) {
                                view.setListaImagenesModel(model.getModeloLista());
                                view.setTituloPanelIzquierdo("Archivos: " + model.getModeloLista().getSize());
                            }

                            // --- LÓGICA DE SELECCIÓN DIRECTA Y ROBUSTA ---
                            int indiceCalculado = -1;
                            if (claveImagenAMantener != null && !claveImagenAMantener.isEmpty()) {
                                indiceCalculado = model.getModeloLista().indexOf(claveImagenAMantener);
                            }
                            if (indiceCalculado == -1 && !model.getModeloLista().isEmpty()) {
                                indiceCalculado = 0;
                            }

                            if (indiceCalculado != -1 && listCoordinator != null) {
                                // Llamada directa al coordinador, evitando los listeners de usuario.
                                listCoordinator.reiniciarYSeleccionarIndice(indiceCalculado);
                            } else {
                                limpiarUI();
                            }
                            
                            if (alFinalizarConExito != null) {
                                alFinalizarConExito.run();
                            }
                        } else {
                            System.out.println("    -> Resultado del worker fue null. Carga fallida.");
                            limpiarUI();
                        }
                    } catch (Exception e) {
                        System.err.println("    -> ERROR durante la ejecución del worker: " + e.getMessage());
                        e.printStackTrace();
                        limpiarUI();
                    } finally {
                        // --- ¡CAMBIO CLAVE! Desactivar los flags aquí ---
                        this.estaCargandoLista = false;
                        this.cargaInicialEnCurso = false;
                        
                        if (cargaImagenesFuture == worker) { 
                            cargaImagenesFuture = null;
                        }
                        if (infobarImageManager != null) infobarImageManager.actualizar();
                        if (statusBarManager != null) statusBarManager.actualizar();
                    }
                } 
            });

            // --- 9. EJECUTAR EL WORKER ---
            worker.execute(); 
            SwingUtilities.invokeLater(() -> { 
                if (dialogo != null) { 
                    dialogo.setVisible(true); 
                }
            });

        } else {
            System.out.println("[cargarListaImagenes] No se puede cargar la lista: Carpeta de inicio inválida o nula: " + pathDeInicioWalk);
            limpiarUI();
            this.estaCargandoLista = false;
            this.cargaInicialEnCurso = false;
        }
    } // --- FIN del metodo cargarListaImagenes ---
    
     
//    /**
//     * Carga o recarga la lista de imágenes desde disco para una carpeta específica,
//     * utilizando un SwingWorker para no bloquear el EDT. Muestra un diálogo de
//     * progreso durante la carga. Una vez cargada la lista: 
//     * - Actualiza el modelo principal de datos (`VisorModel`). 
//     * - Actualiza las JList en la vista (`VisorView`). 
//     * - Inicia el precalentamiento ASÍNCRONO y DIRIGIDO del caché de miniaturas. 
//     * - Selecciona una imagen específica (si se proporciona `claveImagenAMantener`) 
//     *   o la primera imagen de la lista. 
//     * - Maneja la selección inicial de forma segura usando el flag `seleccionInicialEnCurso`.
//     *
//     * @param claveImagenAMantener La clave única (ruta relativa) de la imagen que
//     *                             se intentará seleccionar después de que la lista
//     *                             se cargue. Si es `null`, se seleccionará la
//     *                             primera imagen (índice 0).
//     */
//    public void cargarListaImagenes(String claveImagenAMantener) {
//
//        // --- 1. LOG INICIO Y VALIDACIONES PREVIAS ---
//        // 1.1. Log detallado del inicio y la clave a mantener.
//        System.out.println("\n-->>> INICIO cargarListaImagenes(String) | Mantener Clave: " + claveImagenAMantener);
//
//        // 1.2. Verificar dependencias críticas del sistema.
//        if (configuration == null || model == null || executorService == null || executorService.isShutdown() || view == null) {
//            System.err.println("ERROR [cargarListaImagenes]: Dependencias nulas (Config, Modelo, Executor o Vista) o Executor apagado.");
//            if (view != null) SwingUtilities.invokeLater(this::limpiarUI);
//            estaCargandoLista = false;
//            return;
//        }
//
//        // 1.3. Marcar que la carga de la lista está en curso.
//        estaCargandoLista = true;
//
//        // --- 2. CANCELAR TAREAS ANTERIORES ---
//        // 2.1. Cancelar cualquier tarea previa de carga de lista de imágenes que aún esté activa.
//        if (cargaImagenesFuture != null && !cargaImagenesFuture.isDone()) {
//            System.out.println("  -> Cancelando tarea de carga de lista anterior...");
//            cargaImagenesFuture.cancel(true); 
//        }
//
//        // --- 3. DETERMINAR PARÁMETROS DE BÚSQUEDA DE ARCHIVOS ---
//        // 3.1. Determinar si se deben incluir subcarpetas, leyendo del modelo.
//        final boolean mostrarSoloCarpeta = model.isMostrarSoloCarpetaActual();
//        // 3.2. Establecer la profundidad de búsqueda para Files.walk.
//        int depth = mostrarSoloCarpeta ? 1 : Integer.MAX_VALUE;
//        System.out.println("  -> Modo búsqueda: " + (mostrarSoloCarpeta ? "Solo Carpeta Actual (depth=1)" : "Subcarpetas (depth=MAX)"));
//
//        // 3.3. Determinar la carpeta desde donde iniciar la búsqueda (`pathDeInicioWalk`).
//        Path pathDeInicioWalk = null;
//        if (mostrarSoloCarpeta) {
//            String claveReferenciaParaCarpeta = claveImagenAMantener != null ? claveImagenAMantener : model.getSelectedImageKey();
//            Path rutaImagenReferencia = claveReferenciaParaCarpeta != null ? model.getRutaCompleta(claveReferenciaParaCarpeta) : null;
//            if (rutaImagenReferencia != null && Files.isRegularFile(rutaImagenReferencia)) {
//                pathDeInicioWalk = rutaImagenReferencia.getParent();
//            }
//            if (pathDeInicioWalk == null || !Files.isDirectory(pathDeInicioWalk)) {
//                System.out.println("    -> [cargarListaImagenes] No se pudo obtener carpeta de imagen de referencia válida para 'solo carpeta'. Usando carpeta raíz actual del MODELO: " + this.model.getCarpetaRaizActual());
//                pathDeInicioWalk = this.model.getCarpetaRaizActual();
//            } else {
//                System.out.println("    -> [cargarListaImagenes] Iniciando búsqueda (solo carpeta) desde carpeta de imagen de referencia: " + pathDeInicioWalk);
//            }
//        } else {
//            pathDeInicioWalk = this.model.getCarpetaRaizActual();
//            System.out.println("    -> [cargarListaImagenes] Iniciando búsqueda (con subcarpetas) desde carpeta raíz del MODELO: " + pathDeInicioWalk);
//        }
//        
//
//        // --- 4. VALIDAR PATH DE INICIO Y PROCEDER CON LA CARGA ---
//        // 4.1. Comprobar si el `pathDeInicioWalk` calculado es un directorio válido.
//        if (pathDeInicioWalk != null && Files.isDirectory(pathDeInicioWalk)) {
//        	
//        	// --- LIMPIEZA DE CACHÉ ---
//            if (this.servicioMiniaturas != null) {
//                System.out.println("  -> Limpiando caché de ThumbnailService antes de nueva carga...");
//                this.servicioMiniaturas.limpiarCache();
//            }
//            // -------------------------
//        	
//            // 4.2. Crear variables finales para ser accesibles por lambdas y el SwingWorker.
//            final Path finalStartPath = pathDeInicioWalk;
//            final int finalDepth = depth;
//            final String finalClaveImagenAMantenerWorker = claveImagenAMantener;
//            final Path finalRutaRaizParaRelativizar = this.model.getCarpetaRaizActual();
//
//         // --- 5. LIMPIEZA INICIAL DE LA INTERFAZ DE USUARIO (UI) ---
//         // 5.1. Programar la limpieza en el EDT.
//         SwingUtilities.invokeLater(() -> {
//             System.out.println("  [cargarListaImagenes EDT] Limpiando UI para nueva carga...");
//
//             // 5.1.1. Limpiar el área de la imagen principal.
//             //        Está bien mantener este método en VisorView si solo afecta a variables internas de pintado.
//             if (view != null) {
//                 view.limpiarImagenMostrada();
//             }
//
//             // 5.1.2. Actualizar el título del panel izquierdo directamente a través del registro.
//             //         Esto reemplaza a view.setTituloPanelIzquierdo(...).
//             Component panelIzquierdoComp = registry.get("panel.izquierdo.listaArchivos");
//             if (panelIzquierdoComp instanceof JPanel && ((JPanel) panelIzquierdoComp).getBorder() instanceof TitledBorder) {
//                 TitledBorder border = (TitledBorder) ((JPanel) panelIzquierdoComp).getBorder();
//                 border.setTitle("Escaneando: " + finalStartPath.getFileName() + "...");
//                 panelIzquierdoComp.repaint();
//             }
//
//             // 5.1.3. Limpiar las miniaturas.
//             //         No es necesario pasar por la vista, podemos modificar el modelo directamente.
//             if (this.modeloMiniaturas != null) { 
//                 this.modeloMiniaturas.clear();
//             }
//
//             // 5.1.4. Actualizar las barras de información a través de sus managers.
//             //         Esto reemplaza a view.setTextoBarraEstadoRuta("").
//             //         Los managers pondrán el texto apropiado para un estado sin selección.
//             if (this.infobarImageManager != null) {
//                 this.infobarImageManager.actualizar();
//             }
//             if (this.statusBarManager != null) {
//                 this.statusBarManager.actualizar();
//             }
//             
//             System.out.println("  [cargarListaImagenes EDT] Limpieza de UI completada.");
//
//         }); // Fin del SwingUtilities.invokeLater
//            
//         	// --- 6. CREAR DIÁLOGO DE PROGRESO Y EL SWINGWORKER ---
//	         System.out.println("  -> [cargarListaImagenes] Creando diálogo y worker para búsqueda en: " + finalStartPath + " (Relativizar a: " + finalRutaRaizParaRelativizar + ")");
//	
//	         // 6.1. Crear el diálogo, pasando la 'view' directamente como el frame padre.
//	         final ProgresoCargaDialog dialogo = new ProgresoCargaDialog(view, null);
//	
//	         // 6.2. Crear el BuscadorArchivosWorker, pasándole la referencia al diálogo.
//	         final BuscadorArchivosWorker worker = new BuscadorArchivosWorker(
//	             finalStartPath, 
//	             finalDepth, 
//	             finalRutaRaizParaRelativizar,
//	             this::esArchivoImagenSoportado, 
//	             dialogo
//	         );
//	         // 6.3. Asociar el worker al diálogo.
//	         dialogo.setWorkerAsociado(worker); 
//	         this.cargaImagenesFuture = worker;
//	         System.out.println("  -> [cargarListaImagenes] Diálogo y Worker creados y asociados.");
//
//            // --- 7. CONFIGURAR EL LISTENER PARA CUANDO EL WORKER TERMINE ('done') ---
//            System.out.println("  -> [cargarListaImagenes] Añadiendo PropertyChangeListener al worker...");
//            worker.addPropertyChangeListener(evt -> {
//                // 7.1. Comprobar si la propiedad que cambió es "state" y si el nuevo estado es DONE.
//                if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE.equals(evt.getNewValue())) {
//                    System.out.println("  [EDT Worker Listener] Tarea de búsqueda (" + worker.getClass().getSimpleName() + ") ha finalizado (DONE). Procesando resultado...");
//                    if (dialogo != null) dialogo.cerrar(); // 7.1.1
//
//                    // 7.2. Comprobar si la tarea fue cancelada.
//                    if (worker.isCancelled()) {
//                        System.out.println("    -> Tarea CANCELADA por el usuario.");
//                        if (view != null) {
//                            limpiarUI(); 
//                            view.setTituloPanelIzquierdo("Carga Cancelada");
//                        }
//                        estaCargandoLista = false; 
//                        return; 
//                    }
//
//                    // 7.3. Obtener el resultado.
//                    try {
//                        Map<String, Path> mapaResultado = worker.get();
//
//                        // 7.4. Procesar el resultado.
//                        if (mapaResultado != null) {
//                            System.out.println("    WORKER HA TERMINADO. Número de archivos encontrados: " + mapaResultado.size());
//                            DefaultListModel<String> nuevoModeloListaPrincipal = new DefaultListModel<>();
//                            List<String> clavesOrdenadas = new ArrayList<>(mapaResultado.keySet());
//                            Collections.sort(clavesOrdenadas);
//                            clavesOrdenadas.forEach(nuevoModeloListaPrincipal::addElement); // 7.4.1
//                            System.out.println("    -> Resultado obtenido del worker: " + nuevoModeloListaPrincipal.getSize() + " archivos. Actualizando modelo y vista...");
//                            
//                            model.actualizarListaCompleta(nuevoModeloListaPrincipal, mapaResultado); // 7.4.2
//                            DefaultListModel<String> modeloPrincipalActualizado = model.getModeloLista();
//
//                            if (view != null) { // 7.4.3
//                                view.setListaImagenesModel(modeloPrincipalActualizado);
//                                view.setTituloPanelIzquierdo("Archivos: " + modeloPrincipalActualizado.getSize());
//                            }
//
//                            estaCargandoLista = false; // 7.4.4
//                            System.out.println("    -> Flag estaCargandoLista puesto a FALSE.");
//
//                            // 7.4.5. Iniciar precalentamiento DIRIGIDO y LIMITADO del caché de miniaturas.
//                            List<Path> rutasParaPrecalentamientoDirigido = new ArrayList<>();
//                            List<Path> todasLasRutasEncontradas = new ArrayList<>(mapaResultado.values()); 
//                            if (!todasLasRutasEncontradas.isEmpty()) {
//                                int limitePrecalentamientoInicial = Math.min(todasLasRutasEncontradas.size(), 30);
//                                for (int i = 0; i < limitePrecalentamientoInicial; i++) {
//                                    rutasParaPrecalentamientoDirigido.add(todasLasRutasEncontradas.get(i));
//                                }
//                                System.out.println("      -> [Precalentamiento] Añadidas " + limitePrecalentamientoInicial + " rutas iniciales para precalentar.");
//                                if (finalClaveImagenAMantenerWorker != null && !finalClaveImagenAMantenerWorker.isEmpty()) {
//                                    int indiceClaveAMantener = modeloPrincipalActualizado.indexOf(finalClaveImagenAMantenerWorker);
//                                    if (indiceClaveAMantener != -1) { 
//                                        int miniAntesConfig = model.getMiniaturasAntes(); 
//                                        int miniDespuesConfig = model.getMiniaturasDespues();
//                                        int inicioRangoMantener = Math.max(0, indiceClaveAMantener - miniAntesConfig);
//                                        int finRangoMantener = Math.min(todasLasRutasEncontradas.size() - 1, indiceClaveAMantener + miniDespuesConfig);
//                                        System.out.println("      -> [Precalentamiento] Intentando precalentar alrededor de '" + finalClaveImagenAMantenerWorker + 
//                                                           "' (índice " + indiceClaveAMantener + "), rango en modelo principal [" + inicioRangoMantener + ".." + finRangoMantener + "]");
//                                        for (int i = inicioRangoMantener; i <= finRangoMantener; i++) {
//                                            if (i >= 0 && i < modeloPrincipalActualizado.getSize()) {
//                                                String claveEnRango = modeloPrincipalActualizado.getElementAt(i);
//                                                Path rutaEnRango = mapaResultado.get(claveEnRango); 
//                                                if (rutaEnRango != null && !rutasParaPrecalentamientoDirigido.contains(rutaEnRango)) {
//                                                    rutasParaPrecalentamientoDirigido.add(rutaEnRango);
//                                                }
//                                            }
//                                        }
//                                    } else { 
//                                         System.out.println("      -> [Precalentamiento] Clave a mantener '" + finalClaveImagenAMantenerWorker + "' no encontrada en la nueva lista. No se precalienta rango específico.");
//                                    }
//                                }
//                                System.out.println("    -> [VisorController] Solicitando precalentamiento DIRIGIDO para un total de " + 
//                                                   rutasParaPrecalentamientoDirigido.size() + " miniaturas.");
//                                precalentarCacheMiniaturasAsync(rutasParaPrecalentamientoDirigido);
//                            } else { 
//                                System.out.println("    -> [VisorController] No hay rutas para precalentar (mapaResultado vacío o lista de rutas vacía).");
//                            }
//
//                            // 7.4.6. Calcular el índice inicial a seleccionar.
//                            int indiceCalculadoParaSeleccion = -1; 
//                            if (finalClaveImagenAMantenerWorker != null && !finalClaveImagenAMantenerWorker.isEmpty() && !modeloPrincipalActualizado.isEmpty()) {
//                                indiceCalculadoParaSeleccion = modeloPrincipalActualizado.indexOf(finalClaveImagenAMantenerWorker);
//                            }
//                            if (indiceCalculadoParaSeleccion == -1 && !modeloPrincipalActualizado.isEmpty()) {
//                                indiceCalculadoParaSeleccion = 0;
//                            }
//                            System.out.println("      -> Índice final a seleccionar después de carga: " + indiceCalculadoParaSeleccion +
//                                               (finalClaveImagenAMantenerWorker != null ? " (intentando mantener: '" + finalClaveImagenAMantenerWorker + "')" : " (default 0 si hay elementos)"));
//                            
//                            // 7.4.7. Aplicar la selección inicial.
//                            final int indiceFinalParaSeleccionEnEDT = indiceCalculadoParaSeleccion;
//                            if (indiceFinalParaSeleccionEnEDT != -1) {
//                            	if (view != null && this.registry.get("list.nombresArchivo") != null && listCoordinator != null) {
//                                    System.out.println("    -> Aplicando selección inicial programática al índice: " + indiceFinalParaSeleccionEnEDT);
//                                    seleccionInicialEnCurso = true;
//                                    System.out.println("      -> Flag seleccionInicialEnCurso puesto a TRUE.");
//                                    
//                                    JList<String> listaNombres = registry.get("list.nombresArchivo");
//                                    if (listaNombres != null) {
//                                        listaNombres.setSelectedIndex(indiceFinalParaSeleccionEnEDT);
//                                    }
//                                    
//                                    
//                                    //registry.get("list.nombresArchivo").setSelectedIndex(indiceFinalParaSeleccionEnEDT);
//                                    SwingUtilities.invokeLater(() -> {
//                                        SwingUtilities.invokeLater(() -> {
//                                            if (listCoordinator != null) {
//                                                System.out.println("      -> [WorkerDone Doble InvokeLater] Llamando MANUALMENTE a ListCoordinator para procesar índice inicial: " + indiceFinalParaSeleccionEnEDT);
//                                                listCoordinator.seleccionarIndiceYActualizarUICompleta(indiceFinalParaSeleccionEnEDT);
//                                                
//                                             // << --- ACTUALIZAR BARRAS DE STATUS --- >>
//                                                if (infobarImageManager != null) {
//                                                    infobarImageManager.actualizar();
//                                                }
//                                                if (statusBarManager != null) {
//                                                    statusBarManager.actualizar();
//                                                }
//                                                
//                                            } else { 
//                                                System.err.println("ERROR [WorkerDone Doble InvokeLater]: ListCoordinator es null al intentar procesar índice inicial.");
//                                            }
//                                        });
//                                    });
//                                    Timer timerFinSeleccion = new Timer(200, (evtTimer) -> {
//                                        seleccionInicialEnCurso = false;
//                                        System.out.println("    -> [Timer Fin Selección Inicial] Flag seleccionInicialEnCurso puesto a FALSE. (Índice que se intentó seleccionar: " + indiceFinalParaSeleccionEnEDT + ")");
//                                        if (listCoordinator != null) {
//                                             listCoordinator.asegurarVisibilidadAmbasListasSiVisibles(listCoordinator.getIndiceOficialSeleccionado());
//                                             
//                                          // << --- ACTUALIZAR BARRAS DE STATUS --- >>
//                                             if (infobarImageManager != null) {
//                                        	    infobarImageManager.actualizar();
//                                        	 }
//                                        	 if (statusBarManager != null) {
//                                        	    statusBarManager.actualizar();
//                                        	 }
//                                        }
//                                    });
//                                    timerFinSeleccion.setRepeats(false);
//                                    timerFinSeleccion.start();
//                                } else { 
//                                    System.err.println("WARN [EDT WorkerDone]: Vista, listaNombres o ListCoordinator nulos al aplicar selección inicial.");
//                                }
//                            } else { // Lista vacía
//                                System.out.println("    -> Lista vacía después de la carga/filtro. Limpiando UI.");
//                                limpiarUI();
//                                if (listCoordinator != null) listCoordinator.forzarActualizacionEstadoNavegacion();
//                            }
//                        } else { // Si mapaResultado fue null
//                            System.out.println("    -> Resultado del worker fue null. Carga fallida.");
//                            if (view != null) {
//                                limpiarUI();
//                                view.setTituloPanelIzquierdo("Carga Incompleta (resultado nulo)");
//                            }
//                            estaCargandoLista = false;
//                        }
//                    // 7.5. Manejar excepciones.
//                    } catch (CancellationException ce) {
//                        System.out.println("    -> Tarea CANCELADA (detectado en worker.get() o durante Files.walk).");
//                        if (view != null) { limpiarUI(); view.setTituloPanelIzquierdo("Carga Cancelada"); }
//                        estaCargandoLista = false;
//                    } catch (InterruptedException ie) {
//                        System.err.println("    -> Hilo INTERRUMPIDO esperando resultado del worker.");
//                        if (view != null) { limpiarUI(); view.setTituloPanelIzquierdo("Carga Interrumpida"); }
//                        Thread.currentThread().interrupt();
//                        estaCargandoLista = false;
//                    } catch (ExecutionException ee) {
//                        System.err.println("    -> ERROR durante la ejecución del worker: " + ee.getCause());
//                        Throwable causa = ee.getCause();
//                        String msg = (causa != null) ? causa.getMessage() : ee.getMessage();
//                        
//                        // <<< INICIO DEL CAMBIO >>>
//                        if (view != null) {
//                            // Usamos 'view' directamente como el frame padre del diálogo
//                            JOptionPane.showMessageDialog(
//                                view, 
//                                "Error durante la carga de archivos:\n" + msg, 
//                                "Error de Carga", 
//                                JOptionPane.ERROR_MESSAGE
//                            );
//                            
//                            limpiarUI();
//                            
//                            // La llamada a este método se mantiene, pero nos aseguramos
//                            // de que su implementación DENTRO de VisorView use el ComponentRegistry.
//                            view.setTituloPanelIzquierdo("Error de Carga");
//                        }
//                        // <<< FIN DEL CAMBIO >>>
//
//                        if (causa != null) causa.printStackTrace(); else ee.printStackTrace();
//                        estaCargandoLista = false;
//                    
//                    } finally {
//                        // 7.6. Asegurar reseteo de flags y limpieza de future.
//                        if (estaCargandoLista) {
//                            System.out.println("WARN [EDT Worker Listener - finally]: estaCargandoLista aún era true. Forzando a false.");
//                            estaCargandoLista = false;
//                        }
//                        if (cargaImagenesFuture == worker) { 
//                            cargaImagenesFuture = null;
//                        }
//                        System.out.println("  [EDT Worker Listener] Procesamiento del resultado finalizado.");
//                        
//                     // << --- ACTUALIZAR BARRAS DE STATUS --- >>
//                        if (infobarImageManager != null) {
//                            infobarImageManager.actualizar();
//                        }
//                        if (statusBarManager != null) {
//                            statusBarManager.actualizar();
//                        }
//                    }
//                } 
//            }); 
//
//            // --- 8. EJECUTAR EL SWINGWORKER Y MOSTRAR EL DIÁLOGO DE PROGRESO ---
//            System.out.println("  -> [cargarListaImagenes] Ejecutando worker y programando muestra de diálogo...");
//            worker.execute(); 
//            SwingUtilities.invokeLater(() -> { 
//                if (dialogo != null) { 
//                    System.out.println("    [EDT Carga Lista] Mostrando diálogo de progreso...");
//                    dialogo.setVisible(true); 
//                }
//            });
//
//        } else { // Si pathDeInicioWalk es null o no es un directorio
//            // --- 9. MANEJAR ERROR DE CARPETA DE INICIO INVÁLIDA ---
//            System.out.println("[cargarListaImagenes] No se puede cargar la lista: Carpeta de inicio inválida o nula: " + pathDeInicioWalk);
//            if (view != null) SwingUtilities.invokeLater(this::limpiarUI);
//            estaCargandoLista = false;
//        }
//
//        // --- 10. LOG FINAL DEL MÉTODO ---
//        System.out.println("-->>> FIN cargarListaImagenes(String) | Clave mantenida: " + claveImagenAMantener);
//    } // --- FIN del metodo cargarListaImagenes
    
    
    
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
        JList<String> listaNombres = registry.get("list.nombresArchivo");
        if (listaNombres != null) {
            ActionMap actionMapNombres = listaNombres.getActionMap();
            InputMap inputMapNombres = listaNombres.getInputMap(JComponent.WHEN_FOCUSED);

            inputMapNombres.put(KeyStroke.getKeyStroke("UP"), actPrev);
            inputMapNombres.put(KeyStroke.getKeyStroke("DOWN"), actNext);
            inputMapNombres.put(KeyStroke.getKeyStroke("LEFT"), actPrev);
            inputMapNombres.put(KeyStroke.getKeyStroke("RIGHT"), actNext);
            inputMapNombres.put(KeyStroke.getKeyStroke("HOME"), actFirst);
            inputMapNombres.put(KeyStroke.getKeyStroke("END"), actLast);
            inputMapNombres.put(KeyStroke.getKeyStroke("PAGE_UP"), actPrevBlock);
            inputMapNombres.put(KeyStroke.getKeyStroke("PAGE_DOWN"), actNextBlock);

            actionMapNombres.put(actPrev, new AbstractAction(actPrev) { 			@Override public void actionPerformed(ActionEvent e) 	{ /*logActionOrigin("Nombres", "UP/LEFT"); 		*/if (listCoordinator != null) listCoordinator.seleccionarAnterior(); }});
            actionMapNombres.put(actNext, new AbstractAction(actNext) { 			@Override public void actionPerformed(ActionEvent e) 	{ /*logActionOrigin("Nombres", "DOWN/RIGHT"); 	*/if (listCoordinator != null) listCoordinator.seleccionarSiguiente(); }});
            actionMapNombres.put(actFirst, new AbstractAction(actFirst) { 			@Override public void actionPerformed(ActionEvent e) 	{ /*logActionOrigin("Nombres", "HOME"); 		*/if(listCoordinator != null) listCoordinator.seleccionarPrimero(); }});
            actionMapNombres.put(actLast, new AbstractAction(actLast) { 			@Override public void actionPerformed(ActionEvent e) 	{ /*logActionOrigin("Nombres", "END"); 			*/if(listCoordinator != null) listCoordinator.seleccionarUltimo(); }});
            actionMapNombres.put(actPrevBlock, new AbstractAction(actPrevBlock) { 	@Override public void actionPerformed(ActionEvent e) 	{ /*logActionOrigin("Nombres", "PAGE_UP"); 	*/if (listCoordinator != null) listCoordinator.seleccionarBloqueAnterior(); }});
            actionMapNombres.put(actNextBlock, new AbstractAction(actNextBlock) { 	@Override public void actionPerformed(ActionEvent e) 	{ /*logActionOrigin("Nombres", "PAGE_DOWN"); */if (listCoordinator != null) listCoordinator.seleccionarBloqueSiguiente(); }});
        } else {
        	System.err.println("WARN [interceptarAccionesTecladoListas]: listaNombres es null.");
        }

        // --- SECCIÓN 2: listaMiniaturas (WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) ---
        // SOLO mapeamos las flechas aquí. HOME/END/PGUP/DN se manejarán globalmente.
        JList<String> listaMiniaturas = registry.get("list.miniaturas");
        if (listaMiniaturas != null) {
            ActionMap actionMapMiniaturas = listaMiniaturas.getActionMap();
            InputMap inputMapMiniaturas = listaMiniaturas.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

            // Mapear SOLO las flechas
            inputMapMiniaturas.put(KeyStroke.getKeyStroke("LEFT"), actPrev);  // LEFT -> ANTERIOR
            inputMapMiniaturas.put(KeyStroke.getKeyStroke("RIGHT"), actNext); // RIGHT -> SIGUIENTE
            // --- NO MAPEAR HOME, END, PAGE_UP, PAGE_DOWN AQUÍ ---

            // Añadir/Reemplazar acciones SOLO para las flechas
            actionMapMiniaturas.put(actPrev, new AbstractAction(actPrev) { @Override public void actionPerformed(ActionEvent e) { /*logActionOrigin("Miniaturas", "UP/LEFT");*/ if (listCoordinator != null) listCoordinator.seleccionarAnterior(); }});
            actionMapMiniaturas.put(actNext, new AbstractAction(actNext) { @Override public void actionPerformed(ActionEvent e) { /*logActionOrigin("Miniaturas", "DOWN/RIGHT");*/ if (listCoordinator != null) listCoordinator.seleccionarSiguiente(); }});
            // --- NO AÑADIR ACCIONES PARA HOME, END, PAGE_UP, PAGE_DOWN AQUÍ ---

        } else { 
        	System.err.println("WARN [interceptarAccionesTecladoListas]: listaMiniaturas es null.");
        	
        }

        // --- SECCIÓN 2.5: NO TOCAR EL SCROLLPANE ---
//        System.out.println("    -> NO se modifican bindings del ScrollPane/Viewport aquí.");

        // --- SECCIÓN 3: Log Final ---
//        System.out.println("  -> Configuración de bindings de JLists completada.");

    } // --- FIN interceptarAccionesTecladoListas ---

    
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
        if (registry == null) {
            return false;
        }
        
        // 2. Obtener referencias a componentes relevantes desde el registro.
        JScrollPane scrollMin = registry.get("scroll.miniaturas");
        JList<String> listaNom = registry.get("list.nombresArchivo");
        
        // Verificar si el foco está dentro del scroll pane de miniaturas
        boolean focoEnAreaMiniaturas = focusOwner != null && scrollMin != null &&
                                       SwingUtilities.isDescendingFrom(focusOwner, scrollMin);

        // Si el foco está en el área de miniaturas Y NO es la listaNombres principal...
        if (focoEnAreaMiniaturas && focusOwner != listaNom) {
            int keyCode = e.getKeyCode(); // Obtener el código de la tecla presionada
            boolean consumed = false; // Flag para saber si consumimos

            // Comprobar si es una de las teclas que queremos interceptar aquí
            switch (keyCode) {
                case KeyEvent.VK_HOME:
                    if (listCoordinator != null) listCoordinator.seleccionarPrimero();
                    consumed = true;
                    break; // Salir del switch

                case KeyEvent.VK_END:
                    if (listCoordinator != null) listCoordinator.seleccionarUltimo();
                    consumed = true;
                    break;

                case KeyEvent.VK_PAGE_UP:
                    if (listCoordinator != null) listCoordinator.seleccionarBloqueAnterior();
                    consumed = true;
                    break;

                case KeyEvent.VK_PAGE_DOWN:
                    if (listCoordinator != null) listCoordinator.seleccionarBloqueSiguiente();
                    consumed = true;
                    break;

                // Dejamos pasar las flechas para que los bindings de listaMiniaturas las cojan
                case KeyEvent.VK_UP:
                    if (listCoordinator != null) listCoordinator.seleccionarAnterior();
                    consumed = true;
                    break;
                    
                case KeyEvent.VK_DOWN:
                    if (listCoordinator != null) listCoordinator.seleccionarSiguiente();
                    consumed = true;
                    break;
                	
                //esto se maneja en interceptarAccionesTecladoListas                    
                case KeyEvent.VK_LEFT: 
                case KeyEvent.VK_RIGHT:
                     break;

                default:
                    break; // No hacer nada, consumed sigue false
            }

            if (consumed) {
            	
            	//LOG Si consumimos (era una flecha u otra tecla)
                //System.out.println("    -> Evento CONSUMIDO por Dispatcher.");
                
            	e.consume();
                return true; // Indicar que lo hemos manejado
            } else {
            	
                //LOG Si no consumimos (era una flecha u otra tecla)
                //System.out.println("    -> Evento NO consumido por dispatcher (pasará a bindings JList si los hay).");
                
            	return false; // Dejar que siga
            }
        }

        // Si el foco no estaba en area miniaturas O era listaNombres,
        // dejar que el evento continúe normalmente.
        return false;
    }// FIN dispatchKeyEvent    
    
	
 // DENTRO DE VisorController.java

    /**
     * Navega a la imagen anterior o siguiente en la lista principal.
     * Calcula el nuevo índice basado en la dirección y el modo 'wrapAround'.
     * Si el índice calculado es diferente al actual, actualiza la selección
     * en la JList de nombres obteniéndola desde el ComponentRegistry.
     *
     * @param direccion Un entero que indica la dirección de navegación: -1 para anterior, 1 para siguiente.
     */
    public void navegarImagen(int direccion) {
        // 1. Validar dependencias y estado
        if (model == null || registry == null || model.getModeloLista() == null) {
            System.err.println("WARN [navegarImagen]: Modelo o Registry no inicializados.");
            return;
        }

        DefaultListModel<String> modeloActual = model.getModeloLista();
        if (modeloActual.isEmpty()) {
            System.out.println("[navegarImagen] Lista vacía, no se puede navegar.");
            return;
        }

        // 2. Obtener el componente JList desde el registro
        JList<String> listaNombres = registry.get("list.nombresArchivo");
        if (listaNombres == null) {
            System.err.println("ERROR [navegarImagen]: El componente 'list.nombresArchivo' no se encontró en el registro.");
            return;
        }

        // 3. Obtener estado actual
        int indiceActual = listaNombres.getSelectedIndex();
        int totalImagenes = modeloActual.getSize();

        if (indiceActual < 0) {
            if (direccion > 0) {
                indiceActual = -1;
            } else if (direccion < 0) {
                indiceActual = totalImagenes;
            } else {
                return;
            }
        }

        // 4. Calcular el próximo índice
        int indiceSiguiente = indiceActual + direccion;

        // 5. Aplicar lógica de 'Wrap Around'
        //    (Asumo que 'wrapAround' es una configuración que podrías querer leer más adelante)
        boolean wrapAround = configuration.getBoolean(ConfigKeys.COMPORTAMIENTO_NAVEGACION_CIRCULAR, false); 

        if (wrapAround) {
            if (indiceSiguiente < 0) {
                indiceSiguiente = totalImagenes - 1;
            } else if (indiceSiguiente >= totalImagenes) {
                indiceSiguiente = 0;
            }
        } else {
            indiceSiguiente = Math.max(0, Math.min(indiceSiguiente, totalImagenes - 1));
        }

        // 6. Actualizar selección en la JList si el índice ha cambiado
        if (indiceSiguiente != indiceActual && indiceSiguiente >= 0 && indiceSiguiente < totalImagenes) {
            System.out.println("[navegarImagen] Cambiando índice de " + indiceActual + " a " + indiceSiguiente);
            
            // <<< LA CORRECCIÓN: Usar la variable local `listaNombres` >>>
            listaNombres.setSelectedIndex(indiceSiguiente);
            
        } else {
            System.out.println("[navegarImagen] El índice no cambió o es inválido. Índice actual: " + indiceActual + ", Siguiente calculado: " + indiceSiguiente);
        }

    } // --- FIN navegarImagen ---


 // DENTRO DE LA CLASE VisorController.java

    /**
     * Navega directamente a un índice específico en la lista principal (listaNombres).
     * Valida el índice proporcionado antes de intentar cambiar la selección.
     * Si el índice es válido y diferente al actual, actualiza la selección
     * en la JList de nombres, obteniéndola desde el ComponentRegistry.
     *
     * @param index El índice del elemento (imagen) al que se desea navegar.
     *              Debe estar dentro del rango [0, tamañoLista - 1].
     */
    public void navegarAIndice(int index) {
        // --- 1. Validar dependencias y estado ---
        if (model == null || registry == null || model.getModeloLista() == null) {
            System.err.println("WARN [navegarAIndice]: Modelo o Registry no inicializados.");
            return;
        }

        DefaultListModel<String> modeloActual = model.getModeloLista();
        int totalImagenes = modeloActual.getSize();

        // --- 2. Validar el índice proporcionado ---
        if (modeloActual.isEmpty()) {
            System.out.println("[navegarAIndice] Lista vacía, no se puede navegar al índice " + index + ".");
            return;
        }
        if (index < 0 || index >= totalImagenes) {
            System.err.println("WARN [navegarAIndice]: Índice solicitado (" + index + ") fuera de rango [0, " + (totalImagenes - 1) + "].");
            return;
        }

        // --- 3. Obtener el componente JList desde el registro ---
        JList<String> listaNombres = registry.get("list.nombresArchivo");
        if (listaNombres == null) {
            System.err.println("ERROR [navegarAIndice]: El componente 'list.nombresArchivo' no se encontró en el registro.");
            return;
        }

        // --- 4. Obtener índice actual y comparar ---
        int indiceActual = listaNombres.getSelectedIndex();

        // --- 5. Actualizar selección en la JList si el índice es diferente ---
        if (index != indiceActual) {
            System.out.println("[navegarAIndice] Navegando a índice: " + index);
            
            // Usar la referencia obtenida del registro para manipular el componente.
            listaNombres.setSelectedIndex(index);
            listaNombres.ensureIndexIsVisible(index);

        } else {
            System.out.println("[navegarAIndice] El índice solicitado (" + index + ") ya es el actual. No se hace nada.");
        }
        
    } // --- Fin del método navegarAIndice ---
    
    
    
// ********************************************************************************************************* FIN DE NAVEGACION    
// ***************************************************************************************************************************    

// ***************************************************************************************************************************    
// ****************************************************************************************************************** UTILIDAD

    
    /**
     * Ejecuta un "soft reset" de la aplicación, realizando todas las tareas
     * definidas para la acción de Refresco Completo.
     */
    public void ejecutarRefrescoCompleto() {
        System.out.println("--- [VisorController] Ejecutando Refresco Completo ---");

        if (model == null || zoomManager == null) {
            System.err.println("ERROR [ejecutarRefrescoCompleto]: Modelo o ZoomManager nulos.");
            return;
        }

        // 1. Recordar el estado actual ANTES del refresco.
        final String claveASeleccionar = model.getSelectedImageKey();
        final servicios.zoom.ZoomModeEnum modoZoomARestaurar = model.getCurrentZoomMode();
        System.out.println("  -> Estado a restaurar: Clave='" + claveASeleccionar + "', ModoZoom='" + modoZoomARestaurar + "'");

        // 2. Definir la acción que se ejecutará DESPUÉS de que la lista se haya cargado.
        Runnable accionAlFinalizar = () -> {
            System.out.println("    -> [Callback post-refresco] Re-aplicando modo de zoom: " + modoZoomARestaurar);
            // Re-aplicamos el modo de zoom que habíamos guardado.
            zoomManager.aplicarModoDeZoom(modoZoomARestaurar);
            
            // También forzamos la actualización de las barras de info para asegurar consistencia.
            if (infobarImageManager != null) infobarImageManager.actualizar();
            if (statusBarManager != null) statusBarManager.actualizar();
        };

        // 3. Ejecutar las partes síncronas del refresco.
        ejecutarRefrescoUI(); 
        refrescarFondoAlPorDefecto();
        // TODO: Lógica para resaltar el punto de color.

        // 4. Iniciar la recarga de la lista, pasando la acción de finalización.
        System.out.println("  -> Recargando lista de imágenes y programando restauración de zoom...");
        cargarListaImagenes(claveASeleccionar, accionAlFinalizar);
        
        System.out.println("--- [VisorController] Refresco Completo encolado/ejecutado. ---");
    } // --- Fin del método ejecutarRefrescoCompleto ---
    
    
    /**
	  * Inicia el proceso de carga y visualización de la imagen principal.
	  * Llamado por ListCoordinator después de actualizar el índice oficial, o
	  * cuando se necesita refrescar la imagen por otras razones.
	  * Este método valida el estado, prepara la UI, y lanza una tarea en segundo
	  * plano para leer la imagen del disco, actualizando la UI en el EDT al finalizar.
	  *
	  * @param indiceSeleccionado El índice de la imagen a mostrar en el modelo principal.
	  */
   public void actualizarImagenPrincipal(int indiceSeleccionado) {
       if (view == null || model == null || executorService == null || executorService.isShutdown() || registry == null) {
           System.err.println("WARN [actualizarImagenPrincipal]: Dependencias no listas.");
           return;
       }

       if (indiceSeleccionado == -1 || model.getSelectedImageKey() == null) {
           limpiarUI();
           if (listCoordinator != null) listCoordinator.forzarActualizacionEstadoAcciones();
           return;
       }
       
       String archivoSeleccionadoKey = model.getSelectedImageKey();
       System.out.println("--> [actualizarImagenPrincipal] Iniciando carga para clave: '" + archivoSeleccionadoKey + "' (Índice: " + indiceSeleccionado + ")");

       if (cargaImagenPrincipalFuture != null && !cargaImagenPrincipalFuture.isDone()) {
           cargaImagenPrincipalFuture.cancel(true);
       }

       Path rutaCompleta = model.getRutaCompleta(archivoSeleccionadoKey);

       if (rutaCompleta == null) {
           System.err.println("ERROR [actualizarImagenPrincipal]: No se pudo encontrar la ruta para la clave: " + archivoSeleccionadoKey);
           // Manejar el error, por ejemplo, mostrando un mensaje en la UI
           ImageDisplayPanel panelError = registry.get("panel.display.imagen");
           if (panelError != null) {
                panelError.mostrarError("Ruta no encontrada para:\n" + archivoSeleccionadoKey, null);
           }
           return;
       }
       
       ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
       if (displayPanel != null) {
           displayPanel.mostrarCargando("Cargando: " + rutaCompleta.getFileName() + "...");
       }

       final String finalKeyParaWorker = archivoSeleccionadoKey;
       final Path finalPathParaWorker = rutaCompleta;

       cargaImagenPrincipalFuture = executorService.submit(() -> {
           BufferedImage imagenCargadaDesdeDisco = null;
           try {
               if (!Files.exists(finalPathParaWorker)) throw new IOException("El archivo no existe: " + finalPathParaWorker);
               imagenCargadaDesdeDisco = ImageIO.read(finalPathParaWorker.toFile());
               if (imagenCargadaDesdeDisco == null) throw new IOException("Formato no soportado o archivo inválido.");
           } catch (Exception ex) {
               System.err.println("Error al cargar la imagen: " + ex.getMessage());
           }

           final BufferedImage finalImagenCargada = imagenCargadaDesdeDisco;

           if (Thread.currentThread().isInterrupted() || !finalKeyParaWorker.equals(model.getSelectedImageKey())) {
               return; // Tarea cancelada o ya se está cargando otra imagen, no hacer nada.
           }

           SwingUtilities.invokeLater(() -> {
               if (view == null || model == null || zoomManager == null || registry == null) return;
               
               if (finalImagenCargada != null) {
                   model.setCurrentImage(finalImagenCargada);
                   
                   // ¡LLAMADA A APLICAR ZOOM ELIMINADA DE AQUÍ!
                   // El listener de primer renderizado se encargará de esto si es la primera imagen.
                   // Para las imágenes siguientes, el modo de zoom se aplicará por el cambio de tamaño o la acción del usuario.
                   // No obstante, SÍ debemos refrescar la vista para que la imagen se pinte.
                   zoomManager.refrescarVistaSincrono();

                   Action toggleMarkAction = actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
                   if (projectManager != null && toggleMarkAction != null) {
                       boolean marcada = projectManager.estaMarcada(finalPathParaWorker);
                       toggleMarkAction.putValue(Action.SELECTED_KEY, marcada);
                   }

               } else {
                   model.setCurrentImage(null);
                   // Si hay error, también refrescamos para limpiar la vista.
                   zoomManager.refrescarVistaSincrono();
                   
                   ImageDisplayPanel panel = registry.get("panel.display.imagen");
                   if (panel != null && iconUtils != null) {
                        ImageIcon errorIcon = iconUtils.getScaledCommonIcon("imagen-rota.png", 128, 128);
                        panel.mostrarError("Error al cargar:\n" + finalPathParaWorker.getFileName().toString(), errorIcon);
                   }
               }

               // Actualizar las barras de información sigue siendo importante aquí
               // para datos que no dependen del zoom (nombre, dimensiones, etc.).
               // La corrección final del zoom la hará el listener.
               if (infobarImageManager != null) infobarImageManager.actualizar();
               if (statusBarManager != null) statusBarManager.actualizar();
               if (listCoordinator != null) listCoordinator.forzarActualizacionEstadoAcciones();
           });
       });
       
       System.out.println("--> [actualizarImagenPrincipal] Tarea de carga lanzada para: " + archivoSeleccionadoKey);
   } // --- FIN del metodo actualizarImagenPrincipal ---
    
    
//	 /**
//	  * Inicia el proceso de carga y visualización de la imagen principal.
//	  * Llamado por ListCoordinator después de actualizar el índice oficial, o
//	  * cuando se necesita refrescar la imagen por otras razones.
//	  * Este método valida el estado, prepara la UI, y lanza una tarea en segundo
//	  * plano para leer la imagen del disco, actualizando la UI en el EDT al finalizar.
//	  *
//	  * @param indiceSeleccionado El índice de la imagen a mostrar en el modelo principal.
//	  */
//    public void actualizarImagenPrincipal(int indiceSeleccionado) {
//        if (view == null || model == null || executorService == null || executorService.isShutdown()) {
//            System.err.println("WARN [actualizarImagenPrincipal]: Dependencias no listas.");
//            return;
//        }
//
//        if (indiceSeleccionado == -1 || model.getSelectedImageKey() == null) {
//            limpiarUI();
//            if (listCoordinator != null) listCoordinator.forzarActualizacionEstadoAcciones();
//            return;
//        }
//        
//        String archivoSeleccionadoKey = model.getSelectedImageKey();
//        System.out.println("--> [actualizarImagenPrincipal] Iniciando carga para clave: '" + archivoSeleccionadoKey + "' (Índice: " + indiceSeleccionado + ")");
//
//        if (cargaImagenPrincipalFuture != null && !cargaImagenPrincipalFuture.isDone()) {
//            cargaImagenPrincipalFuture.cancel(true);
//        }
//
//        Path rutaCompleta = model.getRutaCompleta(archivoSeleccionadoKey);
//
//        if (rutaCompleta == null) {
//            // ... (tu manejo de error de ruta no encontrada) ...
//            return;
//        }
//        
//        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
//        if (displayPanel != null) {
//            displayPanel.getInternalLabel().setText("Cargando: " + rutaCompleta.getFileName() + "...");
//            displayPanel.getInternalLabel().setIcon(null);
//        }
//
//        final String finalKeyParaWorker = archivoSeleccionadoKey;
//        final Path finalPathParaWorker = rutaCompleta;
//
//        cargaImagenPrincipalFuture = executorService.submit(() -> {
//            BufferedImage imagenCargadaDesdeDisco = null;
//            try {
//                if (!Files.exists(finalPathParaWorker)) throw new IOException("El archivo no existe: " + finalPathParaWorker);
//                imagenCargadaDesdeDisco = ImageIO.read(finalPathParaWorker.toFile());
//                if (imagenCargadaDesdeDisco == null) throw new IOException("Formato no soportado o archivo inválido.");
//            } catch (Exception ex) {
//                System.err.println("Error al cargar la imagen: " + ex.getMessage());
//            }
//
//            final BufferedImage finalImagenCargada = imagenCargadaDesdeDisco;
//
//            if (Thread.currentThread().isInterrupted() || !finalKeyParaWorker.equals(model.getSelectedImageKey())) {
//                return;
//            }
//
//            SwingUtilities.invokeLater(() -> {
//                if (view == null || model == null || zoomManager == null) return;
//                
//                if (finalImagenCargada != null) {
//                    model.setCurrentImage(finalImagenCargada);
//                    
//                    // ESTA LLAMADA AHORA FUNCIONARÁ CORRECTAMENTE.
//                    zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
//
//                    Action toggleMarkAction = actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
//                    if (projectManager != null && toggleMarkAction != null) {
//                        boolean marcada = projectManager.estaMarcada(finalPathParaWorker);
//                        toggleMarkAction.putValue(Action.SELECTED_KEY, marcada);
//                    }
//
//                } else {
//                    model.setCurrentImage(null);
////                    zoomManager.refrescarVistaPrincipalConEstadoActualDelModelo(); // Refresca para limpiar la vista
//                    zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
//                    
//                    ImageDisplayPanel panel = registry.get("panel.display.imagen");
//                    if (panel != null && iconUtils != null) {
//                         ImageIcon errorIcon = iconUtils.getScaledCommonIcon("imagen-rota.png", 128, 128);
//                         panel.mostrarError("Error al cargar:\n" + finalPathParaWorker.getFileName().toString(), errorIcon);
//                    }
//                }
//
//                // Actualizar las barras y acciones siempre
//                if (infobarImageManager != null) infobarImageManager.actualizar();
//                if (statusBarManager != null) statusBarManager.actualizar();
//                if (listCoordinator != null) listCoordinator.forzarActualizacionEstadoAcciones();
//            });
//        });
//        
//        System.out.println("--> [actualizarImagenPrincipal] Tarea de carga lanzada para: " + archivoSeleccionadoKey);
//    } // --- FIN del metodo actualizarImagenPrincipal ---
    
   
	   public void limpiarUI() {
	       System.out.println("[Controller] Limpiando UI y Modelo a estado vacío...");
	
	       if (listCoordinator != null) {
	           listCoordinator.setSincronizandoUI(true); // <-- ¡BLOQUEAR LISTENERS!
	       }
	
	       try {
	           if (model != null) {
	               model.actualizarListaCompleta(new DefaultListModel<>(), new HashMap<>());
	               model.setCurrentImage(null);
	               model.setSelectedImageKey(null);
	               model.resetZoomState();
	               System.out.println("  -> Modelo limpiado.");
	           }
	
	           if (view != null) {
	               if (model != null) {
	                   view.setListaImagenesModel(model.getModeloLista());
	               } else {
	                   view.setListaImagenesModel(new DefaultListModel<>());
	               }
	               view.limpiarImagenMostrada();
	               if (this.modeloMiniaturas != null) {
	                   this.modeloMiniaturas.clear();
	               }
	               System.out.println("  -> Vista actualizada a estado vacío.");
	           }
	
	           if (servicioMiniaturas != null) {
	               servicioMiniaturas.limpiarCache();
	               System.out.println("  -> Caché de miniaturas limpiado.");
	           }
	
	           Action toggleMarkImageAction = this.actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
	           if (toggleMarkImageAction != null) {
	               toggleMarkImageAction.setEnabled(false);
	               toggleMarkImageAction.putValue(Action.SELECTED_KEY, Boolean.FALSE);
	               actualizarEstadoVisualBotonMarcarYBarraEstado(false, null);
	           }
	       } finally {
	           if (listCoordinator != null) {
	               // Usar invokeLater para asegurar que se libere después de que se procesen los eventos de limpieza
	               SwingUtilities.invokeLater(() -> listCoordinator.setSincronizandoUI(false)); // <-- ¡LIBERAR LISTENERS!
	           }
	       }
	       
	       if (listCoordinator != null) {
	           listCoordinator.forzarActualizacionEstadoAcciones();
	       }
	       
	       if (infobarImageManager != null) {
	           infobarImageManager.actualizar();
	       }
	       if (statusBarManager != null) {
	           statusBarManager.actualizar();
	       }
	       
	       System.out.println("[Controller] Limpieza de UI y Modelo completada.");
	   } // --- FIN limpiarUI ---		
   
    
//     /**
//      * Limpia el estado del modelo de datos y actualiza la interfaz de usuario
//      * a un estado vacío o por defecto. Se utiliza al iniciar sin carpeta,
//      * si la carpeta seleccionada es inválida, o al manejar ciertos errores.
//      */
//     public void limpiarUI() {
//         // 1. Log inicio
//         System.out.println("[Controller] Limpiando UI y Modelo a estado vacío...");
//
//         //FIXME limpiar todas las listas para refrescar la pantalla (seria util? mejor un metodo al que llamar desde un boton?)
//         // 2. Limpiar el Modelo de Datos Principal ('model')
//         if (model != null) {
//             // 2.1. Limpiar la lista de archivos y el mapa de rutas
//             //      Llamar a actualizarListaCompleta con listas/mapas vacíos es
//             //      la forma más consistente ahora que este método actualiza la vista.
//             model.actualizarListaCompleta(new DefaultListModel<>(), new HashMap<>());
//             // Las siguientes llamadas ya no son estrictamente necesarias porque
//             // actualizarListaCompleta ya pone selectedKey y currentImage a null,
//             // pero las dejamos por claridad o si actualizarListaCompleta cambiara.
//             model.setCurrentImage(null);
//             model.setSelectedImageKey(null);
//             // 2.2. Resetear el estado del zoom/paneo
//             model.resetZoomState();
//              System.out.println("  -> Modelo limpiado.");
//         } else {
//              System.err.println("WARN [limpiarUI]: Modelo es null. No se pudo limpiar.");
//         }
//
//      // --- 3. Actualizar la Vista ('view') para reflejar el estado vacío ---
//         if (view != null) {
//             // 3.1. Asegurar que la JList principal usa el modelo ahora vacío.
//             if (model != null) {
//                 view.setListaImagenesModel(model.getModeloLista());
//             } else {
//                 view.setListaImagenesModel(new DefaultListModel<>());
//             }
//
//             // 3.2. Limpiar el área de la imagen principal.
//             view.limpiarImagenMostrada();
//             
//             // 3.3. Limpiar y repintar la lista de miniaturas.
//             if (this.modeloMiniaturas != null) {
//                 this.modeloMiniaturas.clear();
//             }
//
//             System.out.println("  -> Vista actualizada a estado vacío.");
//         } else {
//             System.err.println("WARN [limpiarUI]: Vista es null. No se pudo actualizar UI.");
//         }
//         
////         // 3. Actualizar la Vista ('view')
////         if (view != null) {
////             // 3.1. Asegurar que las JList usan el modelo ahora vacío
////             if (model != null) { // Asegurarse que modelo no es null para getModeloLista
////                 view.setListaImagenesModel(model.getModeloLista()); // Actualiza ambas listas
////             } else {
////                 // Si el modelo es null, pasar un modelo vacío directamente
////                  view.setListaImagenesModel(new DefaultListModel<>());
////             }
////
////             // 3.2. Limpiar la etiqueta de la imagen principal
////             view.limpiarImagenMostrada();
////
////             // 3.3. Limpiar la barra de estado (texto de ruta)
////             view.setTextoBarraEstadoRuta("");
////
////             // 3.4. Establecer título por defecto en el panel de la lista
////             view.setTituloPanelIzquierdo("Lista de Archivos");
////
////             // 3.5. Repintar la lista de miniaturas (ahora vacía)
////             //      No necesitamos limpiar un panel, solo repintar la JList.
////             if (registry.get("list.miniaturas") != null) {
////            	 if (this.modeloMiniaturas != null) { // modeloMiniaturas es el campo en VisorController
////                     this.modeloMiniaturas.clear();
////            	 }
////             
////                  System.out.println("  -> Lista de miniaturas repintada (vacía).");
////             }
////
////             // 3.6. Actualizar estado de Actions que dependen de la selección
////             //      (Ej: Deshabilitar "Localizar Archivo", Edición, etc.)
////             
////             Action delActionClean = this.actionMap.get(AppActionCommands.CMD_IMAGEN_ELIMINAR);
////             if (delActionClean instanceof DeleteAction) {((DeleteAction) delActionClean).actualizarEstadoEnabled();}
////
////              System.out.println("  -> Vista actualizada a estado vacío.");
////
////         } else {
////             System.err.println("WARN [limpiarUI]: Vista es null. No se pudo actualizar UI.");
////         }
//
//         // 4. Limpiar caché de miniaturas (Opcional pero recomendado al limpiar todo)
//         if (servicioMiniaturas != null) {
//             servicioMiniaturas.limpiarCache();
//             System.out.println("  -> Caché de miniaturas limpiado.");
//         }
//
//         // 5. Actualizar imagenes de proyectos marcadas
//         Action toggleMarkImageAction = this.actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
//         if (toggleMarkImageAction != null) {
//        	    toggleMarkImageAction.setEnabled(false); // Deshabilitar si no hay imagen
//        	    toggleMarkImageAction.putValue(Action.SELECTED_KEY, Boolean.FALSE);
//        	    // Actualizar la UI para reflejar que no hay nada marcado
//        	    actualizarEstadoVisualBotonMarcarYBarraEstado(false, null);
//        	}
//         
//         // 6. Log fin
//         if (listCoordinator != null) {
//             listCoordinator.forzarActualizacionEstadoNavegacion();
//         }
//         
//      // << --- ACTUALIZAR BARRAS AL FINAL DE LA LIMPIEZA --- >>
//         if (infobarImageManager != null) {
//        	    infobarImageManager.actualizar();
//        	}
//        	if (statusBarManager != null) {
//        	    statusBarManager.actualizar();
//        	}
//         
//         System.out.println("[Controller] Limpieza de UI y Modelo completada.");
//
//     } // --- FIN limpiarUI ---
   

     /**
      * Restablece el fondo del visor a su estado POR DEFECTO, según lo define el modelo.
      * Lee si el fondo por defecto debe ser a cuadros o el color del tema actual
      * y da la orden correspondiente al ImageDisplayPanel.
      */
     public void refrescarFondoAlPorDefecto() {
         if (registry == null || model == null || themeManager == null) {
             System.err.println("WARN [refrescarFondoAlPorDefecto]: Dependencias nulas (registry, model o themeManager).");
             return;
         }

         ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
         if (displayPanel == null) {
             System.err.println("ERROR [refrescarFondoAlPorDefecto]: 'panel.display.imagen' no encontrado en el registro.");
             return;
         }

         // Lee el estado por defecto desde el modelo
         if (model.isFondoACuadrosPorDefecto()) {
             // Si el defecto es a cuadros, le da esa orden al panel
             displayPanel.setCheckeredBackground(true);
         } else {
             // Si no, obtiene el color del tema actual y se lo pasa al panel
             Color colorTema = themeManager.getTemaActual().colorFondoSecundario();
             displayPanel.setSolidBackgroundColor(colorTema);
         }
     } // --- Fin del método refrescarFondoAlPorDefecto ---
     
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
                     Path carpetaRaizDelModelo = this.model.getCarpetaRaizActual(); // <<< OBTENER DEL MODELO
                     
                     if (carpetaRaizDelModelo != null) {      // <<< CAMBIO AQUÍ
                    	 
                          try {
                        	  
                              // Intentar relativizar respecto a la carpeta raíz actual
                        	  relativePath = carpetaRaizDelModelo.relativize(ruta);   // <<< CAMBIO AQUÍ
                              
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
                     servicioMiniaturas.obtenerOCrearMiniatura(
                    		 ruta, claveUnica, anchoNormal, altoNormal, true // <- true para indicar que es tamaño normal
                     );

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
         if (view != null && registry.get("list.miniaturas") != null) {
             SwingUtilities.invokeLater(() -> {
                 if (view != null && registry.get("list.miniaturas") != null) { // Doble chequeo
                      System.out.println("  -> Solicitando repintado inicial de listaMiniaturas.");
                      registry.get("list.miniaturas").repaint();
                 }
             });
         }

     } // --- FIN precalentarCacheMiniaturasAsync ---


// ************************************************************************************************************* FIN DE LOGICA     
// ***************************************************************************************************************************
      
// ***************************************************************************************************************************
// ********************************************************************************************************************** ZOOM     

     private void refrescarManualmenteLaVistaPrincipal() {
         // --- INICIO DE LA CORRECCIÓN ---
         if (this.zoomManager != null) {
        	 
             // La forma más segura de forzar un refresco es volver a aplicar el modo de zoom actual.
             if (this.model != null) {
                 this.zoomManager.aplicarModoDeZoom(this.model.getCurrentZoomMode());
             }
         } else {
             System.err.println("ERROR [VisorController.refrescarManualmente]: ZoomManager es nulo.");
         }
 	}
     // --- FIN del metodo refrescarManualmenteLaVistaPrincipal ---


// *************************************************************************************************************** FIN DE ZOOM     
// ***************************************************************************************************************************

// ***************************************************************************************************************************
// ******************************************************************************************************************* ARCHIVO     
     
     
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
        Action toggleSubfoldersAction = (this.actionMap != null) ? this.actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS) : null;
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

        // 5. Actualizar el estado en el Modelo
        System.out.println("    2. Actualizando Modelo...");
        model.setMostrarSoloCarpetaActual(!mostrarSubcarpetasDeseado);

        // 6. Actualizar la Configuración en Memoria
        System.out.println("    3. Actualizando Configuración en Memoria...");
        configuration.setString("comportamiento.carpeta.cargarSubcarpetas", String.valueOf(mostrarSubcarpetasDeseado));

        // 7. Sincronizar la Interfaz de Usuario (Botón y Radios del Menú)
        System.out.println("    4. Sincronizando UI...");
        // Actualizar aspecto visual del botón toggle asociado a la acción
        
        if (this.configAppManager != null) {
            this.configAppManager.actualizarAspectoBotonToggle(toggleSubfoldersAction, mostrarSubcarpetasDeseado);
        } else {
            System.err.println("WARN [setMostrarSubcarpetasAndUpdateConfig]: configAppManager es nulo, no se puede actualizar el botón toggle.");
        }
        
        // Actualizar estado 'selected' de los radio buttons del menú
        restaurarSeleccionRadiosSubcarpetas(mostrarSubcarpetasDeseado);
        sincronizarControlesSubcarpetas();

        // 8. Recargar la Lista de Imágenes
        //    Se llama a la versión detallada de cargarListaImagenes, pasando la clave
        //    guardada para intentar mantener la selección. La carga ocurrirá en segundo plano.
        System.out.println("    5. Programando recarga de lista en EDT (manteniendo clave)...");
        SwingUtilities.invokeLater(() -> {
            System.out.println("      -> [EDT] Llamando a cargarListaImagenes(\"" + claveAntesDelCambio + "\") para recargar...");
            // Esta llamada iniciará el SwingWorker con la nueva configuración de profundidad
            cargarListaImagenes(claveAntesDelCambio, null);
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
         if (view == null) {
              System.err.println("WARN [restaurarSeleccionRadiosSubcarpetas]: Vista es nulo.");
              return; // No se puede hacer nada si no hay menús
         }
         
         if (this.menuItemsPorNombre == null) {
             System.err.println("WARN [restaurarSeleccionRadiosSubcarpetas]: El mapa de menús en el controlador es nulo.");
             return;
         }
         
         Map<String, JMenuItem> menuItems = this.menuItemsPorNombre;

         // 2. Log del estado deseado
         System.out.println("  [Controller] Sincronizando estado visual de Radios Subcarpetas a: " + (mostrarSubcarpetas ? "Mostrar Subcarpetas" : "Mostrar Solo Carpeta"));

         // 3. Obtener las referencias a los JRadioButtonMenuItems específicos
         //    Usar las claves largas definidas en la configuración y usadas por MenuBarBuilder.
         JMenuItem radioMostrarSub = menuItems.get("interfaz.menu.configuracion.carga_de_imagenes.mostrar_imagenes_de_subcarpetas");
         JMenuItem radioMostrarSolo = menuItems.get("interfaz.menu.configuracion.carga_de_imagenes.mostrar_solo_carpeta_actual");

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
         Action toggleProporcionesAction = (this.actionMap != null) ? this.actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES) : null;
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
         if (this.configAppManager != null) {
             this.configAppManager.actualizarAspectoBotonToggle(toggleProporcionesAction, mantener);
         } else {
             System.err.println("WARN [setMantenerProporcionesAndUpdateConfig]: configAppManager es nulo, no se puede actualizar el botón toggle.");
         }

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
             System.out.println("      -> [EDT] Llamando a ZoomManager para refrescar con nueva proporción...");
             
             if (this.zoomManager != null) {

            	 //LOG VisorController DEBUG
//                 System.out.println("  [VisorController DEBUG] Estado del MODELO ANTES DE REFRESCAR ZOOM: model.isMantenerProporcion()=" + model.isMantenerProporcion());
            	 this.zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
                 
             } else {
                 System.err.println("ERROR [setMantenerProporciones EDT]: ZoomManager es null.");
                 refrescarManualmenteLaVistaPrincipal(); // Fallback
             }
         });

         System.out.println("[Controller setMantenerProporciones] FIN (Cambio aplicado y repintado programado).");
     } // --- FIN setMantenerProporcionesAndUpdateConfig ---
     

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
         if (themeManager == null) {
             System.err.println("ERROR [cambiarTemaYNotificar]: ThemeManager es nulo.");
             return;
         }
         if (nuevoTema == null || nuevoTema.trim().isEmpty()) {
             System.err.println("ERROR [cambiarTemaYNotificar]: El nombre del nuevo tema no puede ser nulo o vacío.");
             return;
         }
         String temaLimpio = nuevoTema.trim().toLowerCase();
         System.out.println("[VisorController] Solicitud para cambiar tema a: " + temaLimpio);

         // Delegar al ThemeManager.
         // ThemeManager internamente:
         // 1. Cambia su temaActual.
         // 2. Actualiza ConfigurationManager.
         // 3. Llama a this.sincronizarEstadoDeTodasLasToggleThemeActions() (donde 'this' es VisorController).
         // 4. Muestra el JOptionPane.
         themeManager.setTemaActual(temaLimpio); 

         System.out.println("[VisorController] Fin cambiarTemaYNotificar.");
     }
     
     
     /**
      * Muestra un diálogo modal que contiene una lista de los archivos de imagen
      * actualmente cargados en el modelo principal. Permite al usuario ver la lista
      * completa y, opcionalmente, copiarla al portapapeles, mostrando nombres de archivo
      * relativos o rutas completas.
      */
      public void mostrarDialogoListaImagenes() {
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
          final JDialog dialogoLista = new JDialog(view, "Lista de Imágenes Cargadas", true);
          dialogoLista.setSize(600, 400); // Tamaño inicial razonable
          dialogoLista.setLocationRelativeTo(view); // Centrar sobre la ventana principal
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
      public void copiarListaAlPortapapeles(DefaultListModel<String> listModel) {
      // 1. Validación de entrada
      if (listModel == null) {
          System.err.println("ERROR [copiarListaAlPortapapeles]: El listModel proporcionado es null.");
          // Opcional: Mostrar mensaje al usuario si la vista está disponible
          
          if (view != null) {
        	  JOptionPane.showMessageDialog(
        	            view, // Usar 'view' directamente como el componente padre
        	            "Error interno al intentar copiar la lista.",
        	            "Error al Copiar",
        	            JOptionPane.WARNING_MESSAGE
        	            );
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
               JOptionPane.showMessageDialog(view,
                                             "Error al acceder al portapapeles del sistema.",
                                             "Error al Copiar", 
                                             JOptionPane.ERROR_MESSAGE);
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
               JOptionPane.showMessageDialog(view,
                                             "No se pudo copiar la lista al portapapeles.\n" +
                                             "Puede que otra aplicación lo esté usando.",
                                             "Error al Copiar", JOptionPane.WARNING_MESSAGE);
            }
      } catch (Exception e) {
           // Capturar otros errores inesperados
           System.err.println("ERROR INESPERADO [copiarListaAlPortapapeles]: " + e.getMessage());
           e.printStackTrace();
            if (view != null) {
               JOptionPane.showMessageDialog(view,
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
       

    /**
     * Manejador central de eventos para componentes que NO utilizan directamente
     * el sistema de Actions de Swing (p.ej., JMenuItems a los que se les añadió
     * 'this' como ActionListener en addFallbackListeners por MenuBarBuilder)
     * o para acciones muy específicas que no justificaban una clase Action separada.
     *
     * Prioriza el manejo de los checkboxes para la visibilidad de los botones de la toolbar.
     * Si no es uno de esos, pasa a un switch para otros comandos fallback.
     *
     * @param e El ActionEvent generado por el componente Swing.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // 1. --- LOG INICIAL DETALLADO ---
        //     Ayuda a depurar qué componente y comando dispararon el evento.
        logActionInfo(e);

        // 2. --- OBTENER INFORMACIÓN DEL EVENTO ---
        Object source = e.getSource();         // El JMenuItem que fue clickeado.
        String command = e.getActionCommand(); // El ActionCommand configurado para ese JMenuItem.

        // 2.1. Validar que el comando no sea nulo.
        if (command == null) {
            System.err.println("WARN [VisorController.actionPerformed]: ActionCommand es null para la fuente: " +
                               (source != null ? source.getClass().getSimpleName() : "null") +
                               ". No se puede procesar.");
            return;
        }
        
        // 4. --- MANEJO DE OTROS COMANDOS (FALLBACK GENERAL o para ítems de menú que usan VisorController como listener directo) ---
        //    Si el código llega aquí, el evento NO FUE de un checkbox de "Visualizar Botones Toolbar".
        //    El 'command' aquí será el AppActionCommands.CMD_... si el JMenuItem fue configurado
        //    con un comando de ese tipo pero SIN una Action directa del actionMap.
        System.out.println("\n  [VC.actionPerformed General Switch] Procesando comando fallback/directo: '" + command + "'");

        switch (command) {
            // 4.1. --- Configuración ---
            case AppActionCommands.CMD_CONFIG_GUARDAR:
                System.out.println("    -> Acción: Guardar Configuración Actual");
                guardarConfiguracionActual();
                break;
            case AppActionCommands.CMD_CONFIG_CARGAR_INICIAL:
            	
            	System.out.println("    -> Acción: Restaurar Configuración por Defecto");
                // <<< CAMBIO CLAVE >>>
                if (this.configAppManager != null) {
                    this.configAppManager.restaurarConfiguracionPredeterminada();
                    // La notificación al usuario ya puede estar dentro del método del manager.
                } else {
                    System.err.println("ERROR: ConfigApplicationManager es nulo. No se puede restaurar la configuración.");
                }
                break;
            	
            // 4.2. --- Zoom ---
            case AppActionCommands.CMD_ZOOM_PERSONALIZADO: // Para "Establecer Zoom %..." del menú
                System.out.println("    -> Acción: Establecer Zoom % desde Menú");
                handleSetCustomZoomFromMenu();
                break;

            // 4.3. --- Carga de Carpetas/Subcarpetas (Radios del Menú) ---
            //     Estas Actions (SetSubfolderReadModeAction) son responsables de su propia lógica.
            //     Es muy raro que este 'case' se active si las Actions están bien asignadas.
            //     Esto actuaría como un fallback si, por alguna razón, el ActionListener
            //     del JRadioButtonMenuItem fuera 'this' (VisorController) en lugar de su Action.
            case AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA:
                System.out.println("    -> Acción (Fallback Radio): Mostrar Solo Carpeta Actual");
                if (actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA) != null) {
                    actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA).actionPerformed(
                        new ActionEvent(source, ActionEvent.ACTION_PERFORMED, command)
                    );
                }
                break;
            case AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS:
                System.out.println("    -> Acción (Fallback Radio): Mostrar Imágenes de Subcarpetas");
                if (actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS) != null) {
                    actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS).actionPerformed(
                        new ActionEvent(source, ActionEvent.ACTION_PERFORMED, command)
                    );
                }
                break;

            // 4.4. --- Comandos de Imagen (Placeholders) ---
            case AppActionCommands.CMD_IMAGEN_RENOMBRAR:
                System.out.println("    TODO: Implementar Cambiar Nombre Imagen");
                break;
            case AppActionCommands.CMD_IMAGEN_MOVER_PAPELERA:
                System.out.println("    TODO: Implementar Mover a Papelera");
                break;
            case AppActionCommands.CMD_IMAGEN_FONDO_ESCRITORIO:
                System.out.println("    TODO: Implementar Fondo Escritorio");
                break;
            case AppActionCommands.CMD_IMAGEN_FONDO_BLOQUEO:
                System.out.println("    TODO: Implementar Imagen Bloqueo");
                break;
            case AppActionCommands.CMD_IMAGEN_PROPIEDADES:
                System.out.println("    TODO: Implementar Propiedades Imagen");
                break;
            // 4.5. --- Ayuda ---
            case AppActionCommands.CMD_CONFIG_MOSTRAR_VERSION:
                System.out.println("    -> Acción: Mostrar Versión");
                mostrarVersion();
                break;
            case AppActionCommands.CMD_ESPECIAL_REFRESCAR_UI:
            	System.out.println("    -> Acción: Refrescar UI");
            	ejecutarRefrescoUI();
                
            // 4.6. --- Default Case ---
            default:
                // Si un JMenuItem (que no sea un JMenu contenedor) tiene este VisorController
                // como ActionListener y su ActionCommand no coincide con ninguno de los 'case' anteriores.
                if (source instanceof JMenuItem && !(source instanceof JMenu)) {
                    System.out.println("  WARN [VisorController.actionPerformed]: Comando fallback no manejado explícitamente: '" + command +
                                       "' originado por: " + source.getClass().getSimpleName() +
                                       " con texto: '" + ((JMenuItem)source).getText() + "'");
                }
                break;
        } // Fin del switch general
    } // --- FIN actionPerformed ---
	

    /**
     * Orquesta un refresco completo de la interfaz de usuario.
     * Es llamado por la acción del menú "Refrescar UI".
     * Utiliza el ComponentRegistry y el ThemeApplier existentes para aplicar
     * el tema actual y repintar los componentes.
     */
    private void ejecutarRefrescoUI() {
        System.out.println("\n--- [VisorController] INICIANDO REFRESCO COMPLETO DE LA UI ---");

        // 1. Validar que las dependencias de la nueva arquitectura existan.
        //    El 'registry' y el 'themeManager' son inyectados por AppInitializer.
        if (this.registry == null || this.themeManager == null) {
            System.err.println("ERROR: ComponentRegistry o ThemeManager son nulos. No se puede refrescar la UI.");
            return;
        }

        // 2. Crear una instancia de ThemeApplier al momento.
        //    Le pasamos el registro que el controlador ya posee.
        ThemeApplier themeApplier = new ThemeApplier(this.registry);

        // 3. Obtener el tema que debe estar activo.
        Tema temaParaAplicar = this.themeManager.getTemaActual();

        // 4. Programar la aplicación del tema en el EDT.
        SwingUtilities.invokeLater(() -> {
            System.out.println("-> Aplicando tema '" + temaParaAplicar.nombreDisplay() + "' a la UI existente...");
            
            // 4a. El ThemeApplier aplica los colores a los componentes "simples".
            themeApplier.applyTheme(temaParaAplicar);
            
            // 4b. Forzamos un refresco de los renderers "inteligentes".
            //     La vista todavía puede tener métodos para estas operaciones complejas.
            if (this.view != null) {
                this.view.solicitarRefrescoRenderersMiniaturas();
                // Si tienes un método similar para la lista de nombres, llámalo aquí también.
                // this.view.solicitarRefrescoRendererNombres();
            }
            
            // 4c. Forzar revalidación y repintado de toda la ventana.
            if (this.view != null) {
                this.view.revalidate();
                this.view.repaint();
            }
            
            System.out.println("--- REFRESCO DE UI COMPLETADO ---");
        });
    } // --- Fin del método ejecutarRefrescoUI ---


    /**
     * REFACTORIZADO: Método para manejar la lógica cuando se selecciona "Zoom Personalizado
     * %..." desde el menú principal. Ahora delega al ZoomManager.
     */
	private void handleSetCustomZoomFromMenu () {
		if (this.view == null || this.zoomManager == null) {
			System.err.println("ERROR [handleSetCustomZoomFromMenu]: Vista o ZoomManager nulos.");
			return;
		}

		String input = JOptionPane.showInputDialog(this.view,
				"Introduce el porcentaje de zoom deseado (ej: 150):",
				"Establecer Preajuste de Zoom y Fijar Modo",
				JOptionPane.PLAIN_MESSAGE);

		if (input != null && !input.trim().isEmpty()){
			try {
				input = input.replace("%", "").trim();
				double percentValue = Double.parseDouble(input);

				if (percentValue >= 1 && percentValue <= 5000) {
					this.zoomManager.aplicarModoDeZoom(ZoomModeEnum.USER_SPECIFIED_PERCENTAGE);

                    sincronizarEstadoVisualBotonesYRadiosZoom();
				} else {
					JOptionPane.showMessageDialog(this.view,
							"Porcentaje inválido. Debe estar entre 1 y 5000.",
                            "Error de Entrada",
							JOptionPane.ERROR_MESSAGE);
				}
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(this.view, "Entrada inválida. Por favor, introduce un número.",
						"Error de Formato", JOptionPane.ERROR_MESSAGE);
			}
		} else {
			System.out.println("  -> Establecer Preajuste de Zoom cancelado por el usuario.");
		}
	}
    // --- FIN del metodo handleSetCustomZoomFromMenu ---
    
    
    /**
     * Sincroniza explícitamente el estado visual de los JCheckBoxMenuItems que controlan
     * la visibilidad de los botones de la toolbar.
     * Se asegura de que su estado "seleccionado" coincida con la configuración de visibilidad
     * del botón que controlan. Se debe llamar después de que toda la UI haya sido construida.
     */
    public void sincronizarEstadoVisualCheckboxesDeBotones() {
        System.out.println("[VisorController] Sincronizando estado visual de Checkboxes de visibilidad de botones...");
        
        // Validaciones para evitar NullPointerException
        if (this.menuItemsPorNombre == null || configuration == null) {
            System.err.println("  WARN: No se puede sincronizar, el mapa de menús o la configuración son nulos.");
            return;
        }

        Map<String, JMenuItem> menuItems = this.menuItemsPorNombre;
        
        // Iteramos sobre todos los items de menú que hemos creado y mapeado.
        for (Map.Entry<String, JMenuItem> entry : menuItems.entrySet()) {
            JMenuItem item = entry.getValue();
            
            // Nos interesan solo los que son JCheckBoxMenuItem y cuya Action es del tipo correcto.
            if (item instanceof JCheckBoxMenuItem && item.getAction() instanceof controlador.actions.config.ToggleToolbarButtonVisibilityAction) {
                JCheckBoxMenuItem checkbox = (JCheckBoxMenuItem) item;
                controlador.actions.config.ToggleToolbarButtonVisibilityAction action = (controlador.actions.config.ToggleToolbarButtonVisibilityAction) item.getAction();
                
                // Usamos el nuevo getter para obtener la clave de visibilidad del botón.
                String buttonVisibilityKey = action.getButtonVisibilityKey();

                if (buttonVisibilityKey != null) {
                    // Leemos el estado REAL que debería tener el checkbox.
                    boolean estadoCorrecto = configuration.getBoolean(buttonVisibilityKey, true);
                    
                    // Si el estado visual actual del checkbox no coincide, lo forzamos.
                    if (checkbox.isSelected() != estadoCorrecto) {
                        System.out.println("  -> CORRIGIENDO estado para '" + checkbox.getText().trim() + "'. Debería ser: " + estadoCorrecto + " (Estaba: " + checkbox.isSelected() + ")");
                        checkbox.setSelected(estadoCorrecto);
                    }
                }
            }
        }
        System.out.println("[VisorController] Sincronización de checkboxes de visibilidad finalizada.");
    } // --- FIN del método sincronizarEstadoVisualCheckboxesDeBotones ---
    
    
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
            (view != null ? view : null), // Componente padre (o null si view no existe)
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
        if (e == null) {
            System.out.println("--- Acción Detectada (Evento Nulo) ---");
            return;
        }

        Object source = e.getSource();
        String commandFromEvent = e.getActionCommand(); // Comando del evento
        String sourceClass = (source != null) ? source.getClass().getSimpleName() : "null";
        String sourceId = (source != null) ? " (ID: " + System.identityHashCode(source) + ")" : "";

        // Información adicional a obtener
        String longConfigKey = findLongKeyForComponent(source);
        String assignedActionClass = "NINGUNA";
        String canonicalCommand = "(No aplicable o no encontrado)";

        if (source instanceof AbstractButton) {
            AbstractButton comp = (AbstractButton) source;
            Action assignedAction = comp.getAction(); // Obtener Action asignada

            if (assignedAction != null) {
                assignedActionClass = assignedAction.getClass().getName();
                Object cmdValue = assignedAction.getValue(Action.ACTION_COMMAND_KEY);
                if (cmdValue instanceof String) {
                    canonicalCommand = (String) cmdValue;
                } else {
                    canonicalCommand = "(Action sin ACTION_COMMAND_KEY)";
                }
            } else {
                 // Si no hay Action, el comando canónico "esperado" sería el ActionCommand del componente
                 canonicalCommand = commandFromEvent;
            }
        } else {
             // Si no es un AbstractButton, el comando canónico podría ser el del evento
             canonicalCommand = commandFromEvent;
        }

        // Imprimir log formateado
        System.out.println("--- DEBUG: Acción Detectada ---");
        System.out.println("  > Fuente        : " + sourceClass + sourceId);
        System.out.println("  > Event Command : " + (commandFromEvent != null ? "'" + commandFromEvent + "'" : "null"));
        System.out.println("  > Config Key    : " + (longConfigKey != null ? "'" + longConfigKey + "'" : "(No encontrada)"));
        System.out.println("  > Comando Canon.: " + (canonicalCommand != null ? "'" + canonicalCommand + "'" : "(null)"));
        System.out.println("  > Action Class  : " + assignedActionClass);
        System.out.println("------------------------------");
    }// ---FIN logActionInfo
    

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
        if (this.botonesPorNombre != null) {
            for (Map.Entry<String, JButton> entry : this.botonesPorNombre.entrySet()) {
                if (entry.getValue() == comp) {
                    return entry.getKey();
                }
            }
        }

        // Buscar en menús
        if (this.menuItemsPorNombre != null) {
            for (Map.Entry<String, JMenuItem> entry : this.menuItemsPorNombre.entrySet()) {
                 if (entry.getValue() == comp) {
                     return entry.getKey();
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
  
  private void guardarConfiguracionActual() {
	    if (configuration == null) {
	        System.err.println("ERROR [guardarConfiguracionActual]: Configuración nula.");
	        return;
	    }
	    System.out.println("  [Guardar] Guardando estado final...");

	    // La única cosa que necesita ser guardada al final es la última imagen vista.
	    // Todo lo demás (visibilidad, toggles, etc.) ya ha sido actualizado en memoria por las Actions.
	    if (model != null && model.getSelectedImageKey() != null) {
	        configuration.setString(ConfigKeys.INICIO_IMAGEN, model.getSelectedImageKey());
	    } else {
	        configuration.setString(ConfigKeys.INICIO_IMAGEN, "");
	    }
	    
	    // El estado de la ventana se guarda por separado en el ShutdownHook.

	    try {
	        // Le decimos al ConfigurationManager que escriba su estado actual en el disco.
	        configuration.guardarConfiguracion(configuration.getConfig());
	        System.out.println("  [Guardar] Configuración guardada exitosamente.");
	    } catch (IOException e) {
	        System.err.println("### ERROR FATAL AL GUARDAR CONFIGURACIÓN: " + e.getMessage());
	    }
	} // --- FIN del metodo guardarConfiguracionActual ---  
     
     
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
     public void actualizarModeloYVistaMiniaturas(int indiceSeleccionadoPrincipal) {
    	 
    	 //FIXME separar o juntar la lista miniaturas para que se ajusten al ancho de pantalla disponible
    	 //FIXME cambiar cantidad de miniaturas visibles segun espacio disponible

         // --- SECCIÓN 1: VALIDACIONES INICIALES Y PREPARACIÓN (Fuera del EDT) ---

         // 1.1. Log de inicio del método.
         System.out.println("\n--- INICIO actualizarModeloYVistaMiniaturas --- Índice Principal Recibido: " + indiceSeleccionadoPrincipal);

         // 1.2. Validar dependencias críticas del controlador (modelo, vista, coordinador).
         //      Si alguna falta, no se puede proceder.
         if (model == null || model.getModeloLista() == null || view == null || registry.get("list.miniaturas") == null || listCoordinator == null) {
             System.err.println("WARN [actualizarMiniaturas]: Dependencias nulas (Modelo, Vista, ListaMiniaturas o Coordinator). Abortando.");
             return;
         }
         
         // 1.2.2. --- NUEVA SECCIÓN: VALIDACIÓN DE REDUNDANCIA ---
         JList<String> listaMiniaturasActual = registry.get("list.miniaturas");
         if (listaMiniaturasActual.getModel().getSize() > 0) {
             int indiceRelativoActual = listaMiniaturasActual.getSelectedIndex();
             if (indiceRelativoActual != -1) {
                 try {
                     String claveSeleccionadaEnMiniaturas = listaMiniaturasActual.getModel().getElementAt(indiceRelativoActual);
                     String claveDeseadaEnModeloPrincipal = model.getModeloLista().getElementAt(indiceSeleccionadoPrincipal);
                     
                     // Si la clave ya seleccionada en la lista de miniaturas es la misma que la que queremos seleccionar,
                     // y el número de miniaturas visibles no ha cambiado drásticamente (heurística),
                     // entonces no hacemos nada.
                     if (claveSeleccionadaEnMiniaturas.equals(claveDeseadaEnModeloPrincipal)) {
                         System.out.println("  -> [actualizarMiniaturas] La selección ya es correcta. Se omite la actualización redundante.");
                         listCoordinator.asegurarVisibilidadAmbasListasSiVisibles(indiceSeleccionadoPrincipal); // Solo aseguramos visibilidad
                         return; // ¡Salimos para evitar el trabajo doble!
                     }
                 } catch (ArrayIndexOutOfBoundsException e) {
                     // Puede pasar si los modelos están desincronizados. Continuamos para corregirlo.
                     System.err.println("WARN [actualizarMiniaturas]: Inconsistencia de índice detectada. Procediendo a reconstruir miniaturas.");
                 }
             }
         }
         // --- FIN NUEVA SECCIÓN ---
         

         // 1.3. Obtener el modelo principal de datos y su tamaño.
         final DefaultListModel<String> modeloPrincipal = model.getModeloLista(); // 'final' para acceso en lambda
         final int totalPrincipal = modeloPrincipal.getSize();                     // 'final' para acceso en lambda
         System.out.println("  [actualizarMiniaturas] Tamaño modeloPrincipal: " + totalPrincipal);

         // 1.4. Manejar caso de lista principal vacía o índice principal inválido.
         //      Si no hay datos o el índice no es válido, se programa una limpieza de la UI de miniaturas y se sale.
         if (totalPrincipal == 0 || indiceSeleccionadoPrincipal < 0 || indiceSeleccionadoPrincipal >= totalPrincipal) {
             System.out.println("  [actualizarMiniaturas] Índice principal inválido o lista principal vacía. Limpiando UI de miniaturas y saliendo.");

             SwingUtilities.invokeLater(() -> {
                 if (view != null && registry.get("list.miniaturas") != null && listCoordinator != null) {
                     listCoordinator.setSincronizandoUI(true); // Proteger la UI
                     try {
                         JList<String> lMini = registry.get("list.miniaturas");
                         // Establecer un tamaño preferido mínimo para evitar colapso visual.
                         lMini.setPreferredSize(new Dimension(
                             lMini.getFixedCellWidth() > 0 ? lMini.getFixedCellWidth() : 50,
                             lMini.getFixedCellHeight() > 0 ? lMini.getFixedCellHeight() : 50
                         ));
                         view.setModeloListaMiniaturas(new DefaultListModel<>()); // Asignar modelo vacío
                         if (lMini.getParent() != null) {
                             lMini.getParent().revalidate(); // Revalidar el contenedor del wrapper
                         }
                         lMini.clearSelection();
                         lMini.repaint();
                     } finally {
                         // Asegurar que el flag se desactive, incluso con errores, en un invokeLater anidado.
                         SwingUtilities.invokeLater(() -> {
                             if (listCoordinator != null) listCoordinator.setSincronizandoUI(false);
                         });
                     }
                 }
             });
             System.out.println("--- FIN actualizarModeloYVistaMiniaturas (Caso Vacío/Inválido) ---");
             return;
         }

         // 1.5. (Fuera del EDT aún) Preparar lista de todas las rutas para un precalentamiento general si se desea.
         //      Esto es opcional y podría hacerse de forma más selectiva.
         // List<Path> todasLasRutasParaPrecalentar = new ArrayList<>();
         // for (int i = 0; i < totalPrincipal; i++) {
         //     Path ruta = model.getRutaCompleta(modeloPrincipal.getElementAt(i));
         //     if (ruta != null) todasLasRutasParaPrecalentar.add(ruta);
         // }
         // precalentarCacheMiniaturasAsync(todasLasRutasParaPrecalentar); // Podría ser demasiado si la lista es enorme.

         // --- SECCIÓN 2: PROGRAMAR ACTUALIZACIÓN DE UI EN EL EVENT DISPATCH THREAD (EDT) ---
         //      Toda la lógica de cálculo de rango, construcción del modelo de miniaturas,
         //      y actualización de la JList se hará dentro del invokeLater.

         // 2.1. Declarar variables finales para que sean accesibles dentro de la lambda.
         final int finalIndiceSeleccionadoPrincipal = indiceSeleccionadoPrincipal;
         System.out.println("  [actualizarMiniaturas] Programando actualización de UI en EDT para índice principal: " + finalIndiceSeleccionadoPrincipal);

         SwingUtilities.invokeLater(() -> { // Inicio de la lambda que se ejecuta en el EDT

             // --- SECCIÓN 3: VALIDACIONES Y PREPARACIÓN DENTRO DEL EDT ---

             // 3.1. Log de inicio de la ejecución en el EDT.
             //System.out.println("   -> [EDT Miniaturas Update] Ejecutando actualización UI...");

             // 3.2. Re-validar dependencias críticas (Vista, JList de miniaturas, Coordinador) DENTRO del EDT.
             //      Es una buena práctica por si el estado de la aplicación hubiera cambiado.
             if (view == null || registry.get("list.miniaturas") == null || listCoordinator == null || model == null) {
                 System.err.println("ERROR [actualizarMiniaturas EDT]: Dependencias nulas en invokeLater. Abortando.");
                 return;
             }

             // 3.3. Establecer flag para evitar bucles de eventos de selección.
             listCoordinator.setSincronizandoUI(true);
             System.out.println("   -> [EDT Miniaturas Update] Flag sincronizandoUI puesto a TRUE.");

             try { // Bloque try-finally para asegurar que el flag sincronizandoUI se desactive.

                 // --- SECCIÓN 4: CÁLCULO DINÁMICO DEL RANGO DE MINIATURAS (DENTRO DEL EDT) ---

                 // 4.1. Llamar a `calcularNumMiniaturasDinamicas()`. Este método ahora leerá
                 //      las dimensiones del viewport del JScrollPane en el EDT, que deberían ser las más actuales.
                 RangoMiniaturasCalculado rangoDinamico = calcularNumMiniaturasDinamicas();
                 int miniAntesDinamicas = rangoDinamico.antes;
                 int miniDespuesDinamicas = rangoDinamico.despues;
                 System.out.println("   -> [EDT Miniaturas Update] Rango dinámico calculado -> Antes: " + miniAntesDinamicas + ", Despues: " + miniDespuesDinamicas);

                 // 4.2. Calcular los índices de inicio y fin del rango en el modelo PRINCIPAL
                 //      usando los valores dinámicos obtenidos.
                 int inicioRango = Math.max(0, finalIndiceSeleccionadoPrincipal - miniAntesDinamicas);
                 int finRango = Math.min(totalPrincipal - 1, finalIndiceSeleccionadoPrincipal + miniDespuesDinamicas);
                 System.out.println("   -> [EDT Miniaturas Update] Rango final en modelo principal: [" + inicioRango + ".." + finRango + "]");


                 // --- SECCIÓN 5: CONSTRUCCIÓN DEL NUEVO MODELO PARA LA JLIST DE MINIATURAS (DENTRO DEL EDT) ---

                 // 5.1. Crear un nuevo `DefaultListModel` que contendrá solo las claves de las miniaturas a mostrar.
                 DefaultListModel<String> nuevoModeloParaLaVista = new DefaultListModel<>();
                 // 5.2. Inicializar el índice que estará seleccionado DENTRO de este nuevo modelo de miniaturas.
                 int indiceRelativoSeleccionadoEnNuevoModelo = -1;
                 // 5.3. Preparar lista de Paths para el precalentamiento selectivo del caché.
                 List<Path> rutasEnRangoVisible = new ArrayList<>();

                 System.out.println("   -> [EDT Miniaturas Update] Llenando nuevo modelo de miniaturas...");
                 // 5.4. Iterar sobre el rango calculado y poblar el nuevo modelo.
                 for (int i = inicioRango; i <= finRango; i++) {
                     String clave = modeloPrincipal.getElementAt(i); // Obtener clave del modelo principal
                     nuevoModeloParaLaVista.addElement(clave);       // Añadir al nuevo modelo de miniaturas

                     Path ruta = model.getRutaCompleta(clave);
                     if (ruta != null) {
                         rutasEnRangoVisible.add(ruta); // Añadir a la lista para precalentar
                     }

                     // 5.5. Si el índice actual (i) del modelo principal es el que queremos seleccionar,
                     //      calcular su posición RELATIVA en el `nuevoModeloParaLaVista`.
                     if (i == finalIndiceSeleccionadoPrincipal) {
                         indiceRelativoSeleccionadoEnNuevoModelo = nuevoModeloParaLaVista.getSize() - 1;
                     }
                 }
                 System.out.println("   -> [EDT Miniaturas Update] Nuevo modelo de miniaturas llenado. Tamaño: "
                                  + nuevoModeloParaLaVista.getSize() + ". Índice relativo seleccionado: " + indiceRelativoSeleccionadoEnNuevoModelo);

                 // --- SECCIÓN 6: PRE-CALENTAMIENTO SELECTIVO DEL CACHÉ DE MINIATURAS (DENTRO DEL EDT, PERO LANZA TAREAS BG) ---
                 //      Llamar a precalentar solo para las miniaturas que estarán en el rango visible.
                 precalentarCacheMiniaturasAsync(rutasEnRangoVisible);


                 // --- SECCIÓN 7: ACTUALIZACIÓN DE LA JLIST DE MINIATURAS EN LA VISTA (DENTRO DEL EDT) ---
                 JList<String> listaMiniaturasEnVista = registry.get("list.miniaturas");

                 // 7.1. Actualizar el `PreferredSize` de la `JList` de miniaturas ANTES de cambiar su modelo.
                 //      Esto es crucial para que el `FlowLayout` del panel wrapper (`wrapperListaMiniaturas`)
                 //      pueda centrar la `JList` correctamente si esta es más estrecha que el wrapper.
                 int cellWidthActual = listaMiniaturasEnVista.getFixedCellWidth();
                 int cellHeightActual = listaMiniaturasEnVista.getFixedCellHeight();

                 if (cellWidthActual > 0 && cellHeightActual > 0) { // Solo si las celdas tienen tamaño válido
                     int numItemsEnNuevoModelo = nuevoModeloParaLaVista.getSize();
                     int nuevoAnchoPreferido = numItemsEnNuevoModelo * cellWidthActual;
                     // Asegurar un ancho mínimo si el modelo está vacío (para evitar colapso a 0)
                     if (numItemsEnNuevoModelo == 0) {
                         nuevoAnchoPreferido = cellWidthActual;
                     }
                     Dimension nuevoTamanoPreferido = new Dimension(nuevoAnchoPreferido, cellHeightActual);

                     // Cambiar el PreferredSize solo si es diferente, para evitar revalidaciones innecesarias.
                     if (!nuevoTamanoPreferido.equals(listaMiniaturasEnVista.getPreferredSize())) {
                         listaMiniaturasEnVista.setPreferredSize(nuevoTamanoPreferido);
                         System.out.println("   -> [EDT Miniaturas Update] PreferredSize de JList miniaturas actualizado a: " + nuevoTamanoPreferido);

                         // Revalidar el panel contenedor de la JList (el wrapper con FlowLayout)
                         // para que el FlowLayout se reajuste.
                         if (listaMiniaturasEnVista.getParent() != null) {
                             listaMiniaturasEnVista.getParent().revalidate();
                         }
                     }
                 }

                 // 7.2. Asignar el nuevo modelo de datos (`nuevoModeloParaLaVista`) a la `JList`.
                 //      Hacerlo solo si el modelo es realmente diferente para evitar eventos extra.
                 if (listaMiniaturasEnVista.getModel() != nuevoModeloParaLaVista) {
                     view.setModeloListaMiniaturas(nuevoModeloParaLaVista);
                     // El método setModeloListaMiniaturas en VisorView ya debería imprimir su propio log.
                 }

                 // 7.3. Establecer la selección en la `JList` de miniaturas.
                 //      Usar el `indiceRelativoSeleccionadoEnNuevoModelo` calculado anteriormente.
                 if (indiceRelativoSeleccionadoEnNuevoModelo >= 0 &&
                     indiceRelativoSeleccionadoEnNuevoModelo < nuevoModeloParaLaVista.getSize()) {
                     // Cambiar la selección solo si es diferente a la actual en la JList
                     if (listaMiniaturasEnVista.getSelectedIndex() != indiceRelativoSeleccionadoEnNuevoModelo) {
                         listaMiniaturasEnVista.setSelectedIndex(indiceRelativoSeleccionadoEnNuevoModelo);
                         System.out.println("   -> [EDT Miniaturas Update] setSelectedIndex(" + indiceRelativoSeleccionadoEnNuevoModelo + ") en JList miniaturas.");
                     }
                 } else {
                     // Si el índice relativo no es válido (ej. lista vacía), limpiar la selección.
                     if (listaMiniaturasEnVista.getSelectedIndex() != -1) {
                         listaMiniaturasEnVista.clearSelection();
                     }
                     System.err.println("WARN [actualizarMiniaturas EDT]: Índice relativo inválido para selección en JList: " + indiceRelativoSeleccionadoEnNuevoModelo);
                 }

                 // 7.4. Asegurar visibilidad del ítem seleccionado y repintar.
                 //      Llamar a ensureIndexIsVisible DESPUÉS de que el layout haya tenido oportunidad de ajustarse.
                 //      Un invokeLater anidado puede ayudar aquí, pero primero probemos sin él.
                 //      El repintado es importante para que los cambios sean visibles.
                 if (indiceRelativoSeleccionadoEnNuevoModelo != -1) {
                     try {
                         listaMiniaturasEnVista.ensureIndexIsVisible(indiceRelativoSeleccionadoEnNuevoModelo);
                     } catch (Exception ex) {
                         System.err.println("ERROR ensureIndexIsVisible(Miniaturas): " + ex.getMessage());
                     }
                 }
                 listaMiniaturasEnVista.repaint();

             } finally {
                 // --- SECCIÓN 8: DESACTIVACIÓN DEL FLAG DE SINCRONIZACIÓN (DENTRO DEL EDT) ---
                 // 8.1. Usar un invokeLater anidado para asegurar que se desactive después
                 //      de que todos los eventos de cambio de modelo/selección se hayan procesado.
                 SwingUtilities.invokeLater(() -> {
                     if (listCoordinator != null) {
                         listCoordinator.setSincronizandoUI(false);
                         // El método setSincronizandoUI ya tiene su propio log.
                     }
                 });
             } // Fin del bloque try-finally principal del EDT

             // 3.4. Log final de la ejecución en el EDT.
             System.out.println("   -> [EDT Miniaturas Update] FIN Ejecución actualización UI.");

         }); // Fin del SwingUtilities.invokeLater principal

         // --- SECCIÓN 9: LOG FINAL DEL MÉTODO (Fuera del EDT) ---
         System.out.println("--- FIN actualizarModeloYVistaMiniaturas ---");

     } // --- Fin del metodo actualizarModeloYVistaMiniaturas
     
     
      

     /**
      * Calcula dinámicamente el número de miniaturas a mostrar antes y después de la
      * miniatura central, basándose en el ancho disponible del viewport del JScrollPane
      * de miniaturas y el ancho de cada celda de miniatura.
      * Respeta los máximos configurados por el usuario.
      *
      * @return Un objeto RangoMiniaturasCalculado con los valores 'antes' y 'despues'.
      */
     public RangoMiniaturasCalculado calcularNumMiniaturasDinamicas() {
    	    // --- 1. OBTENER LÍMITES SUPERIORES DE CONFIGURACIÓN/MODELO (sin cambios) ---
    	    int cfgMiniaturasAntes, cfgMiniaturasDespues;
    	    if (model != null) {
    	        cfgMiniaturasAntes = model.getMiniaturasAntes();
    	        cfgMiniaturasDespues = model.getMiniaturasDespues();
    	    } else if (configuration != null) {
    	        cfgMiniaturasAntes = configuration.getInt("miniaturas.cantidad.antes", DEFAULT_MINIATURAS_ANTES_FALLBACK);
    	        cfgMiniaturasDespues = configuration.getInt("miniaturas.cantidad.despues", DEFAULT_MINIATURAS_DESPUES_FALLBACK);
    	        System.out.println("  [CalcularMiniaturas] WARN: Modelo nulo, usando valores de config/fallback.");
    	    } else {
    	        cfgMiniaturasAntes = DEFAULT_MINIATURAS_ANTES_FALLBACK;
    	        cfgMiniaturasDespues = DEFAULT_MINIATURAS_DESPUES_FALLBACK;
    	        System.err.println("  [CalcularMiniaturas] ERROR: Modelo y Config nulos, usando fallbacks.");
    	    }

    	    // --- 2. OBTENER COMPONENTES DE LA VISTA DESDE EL REGISTRO ---
    	    JScrollPane scrollPane = registry.get("scroll.miniaturas");
    	    JList<String> listaMin = registry.get("list.miniaturas");

    	    // --- 3. VALIDAR DISPONIBILIDAD DE COMPONENTES ---
    	    if (scrollPane == null || listaMin == null) {
    	        System.out.println("  [CalcularMiniaturas] WARN: ScrollPane o JList de miniaturas nulos en registro. Devolviendo máximos configurados.");
    	        return new RangoMiniaturasCalculado(cfgMiniaturasAntes, cfgMiniaturasDespues);
    	    }

    	    // --- 4. OBTENER DIMENSIONES ACTUALES DE LA UI ---
    	    int viewportWidth = scrollPane.getViewport().getWidth();
    	    int cellWidth = listaMin.getFixedCellWidth();

    	    // Log de depuración
    	    // System.out.println("  [CalcularMiniaturas DEBUG] ViewportWidth: " + ...);

    	    // --- 5. LÓGICA DE FALLBACK MEJORADA ---
    	    if (viewportWidth <= 0 || cellWidth <= 0 || !scrollPane.isShowing()) {
    	        System.out.println("  [CalcularMiniaturas] WARN: Viewport/Cell inválido o ScrollPane no visible. Usando MÁXIMOS configurados como fallback.");
    	        return new RangoMiniaturasCalculado(cfgMiniaturasAntes, cfgMiniaturasDespues);
    	    }

    	    // --- 6. CÁLCULO Y DISTRIBUCIÓN (sin cambios) ---
    	    int totalMiniaturasQueCaben = viewportWidth / cellWidth;
    	    int numAntesCalculado;
    	    int numDespuesCalculado;
    	    int maxTotalConfigurado = cfgMiniaturasAntes + 1 + cfgMiniaturasDespues;

    	    if (totalMiniaturasQueCaben >= maxTotalConfigurado) {
    	        numAntesCalculado = cfgMiniaturasAntes;
    	        numDespuesCalculado = cfgMiniaturasDespues;
    	    } else if (totalMiniaturasQueCaben <= 1) {
    	        numAntesCalculado = 0;
    	        numDespuesCalculado = 0;
    	    } else {
    	        int miniaturasLateralesDisponibles = totalMiniaturasQueCaben - 1;
    	        double ratioAntesOriginal = (cfgMiniaturasAntes + cfgMiniaturasDespues > 0) ? (double) cfgMiniaturasAntes / (cfgMiniaturasAntes + cfgMiniaturasDespues) : 0.5;
    	        numAntesCalculado = (int) Math.round(miniaturasLateralesDisponibles * ratioAntesOriginal);
    	        numDespuesCalculado = miniaturasLateralesDisponibles - numAntesCalculado;
    	        numAntesCalculado = Math.min(numAntesCalculado, cfgMiniaturasAntes);
    	        numDespuesCalculado = Math.min(numDespuesCalculado, cfgMiniaturasDespues);
    	    }

    	    // --- 7. DEVOLVER EL RESULTADO CALCULADO ---
    	    System.out.println("  [CalcularMiniaturas] Rango dinámico calculado -> Antes: " + numAntesCalculado + ", Despues: " + numDespuesCalculado);
    	    return new RangoMiniaturasCalculado(numAntesCalculado, numDespuesCalculado);
    	}// --- FIN del metodo calcularNumMiniaturasDinamicas ---
     
     
	  public void navegarSiguienteViaCoordinador() {
	      if (listCoordinator != null) {
	          listCoordinator.seleccionarSiguiente();
	      } else {
	          System.err.println("Error: ListCoordinator es null al intentar navegar siguiente.");
	      }
	  }

	  public void navegarAnteriorViaCoordinador() {
	      if (listCoordinator != null) {
	          listCoordinator.seleccionarAnterior();
	      } else {
	          System.err.println("Error: ListCoordinator es null al intentar navegar anterior.");
	      }
	  }

	  public void navegarPrimeroViaCoordinador() {
	      if (listCoordinator != null) {
	          listCoordinator.seleccionarPrimero();
	      } else {
	          System.err.println("Error: ListCoordinator es null al intentar navegar primero.");
	      }
	  }

	  public void navegarUltimoViaCoordinador() {
	      if (listCoordinator != null) {
	          listCoordinator.seleccionarUltimo();
	      } else {
	          System.err.println("Error: ListCoordinator es null al intentar navegar último.");
	      }
	  }

	// Si también tienes actions para PageUp/PageDown que llaman a ListCoordinator:
	public void navegarBloqueSiguienteViaCoordinador() {
	    if (listCoordinator != null) {
	        listCoordinator.seleccionarBloqueSiguiente();
	    } else {
	         System.err.println("Error: ListCoordinator es null al intentar navegar bloque siguiente.");
	    }
	}

	public void navegarBloqueAnteriorViaCoordinador() {
	    if (listCoordinator != null) {
	        listCoordinator.seleccionarBloqueAnterior();
	    } else {
	         System.err.println("Error: ListCoordinator es null al intentar navegar bloque anterior.");
	    }
	}	  

// ***************************************************************************** FIN METODOS DE MOVIMIENTO CON LISTCOORDINATOR
// ***************************************************************************************************************************

// ****************************************************************************************************** GESTION DE PROYECTOS
// ***************************************************************************************************************************	  
	  
	  
	/**
	 * Actualiza el estado visual de los componentes relacionados con la marca de proyecto.
	 * Este método se asegura de que el estado de la Action, el botón de la toolbar
	 * y la barra de estado reflejen si la imagen actual está marcada o no.
	 *
	 * @param estaMarcada true si la imagen está marcada, false en caso contrario.
	 * @param rutaParaBarraEstado La ruta de la imagen, para mostrar en la barra de estado (puede ser null).
	 */
	public void actualizarEstadoVisualBotonMarcarYBarraEstado(boolean estaMarcada, Path rutaParaBarraEstado) {
	    if (actionMap == null) return;

	    // --- 1. Sincronizar la Action ---
	    //    Aseguramos que el estado lógico de la Action esté correcto.
	    //    Esto actualizará automáticamente cualquier JCheckBoxMenuItem asociado.
	    Action toggleMarkImageAction = actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
	    if (toggleMarkImageAction != null) {
	        // Solo cambiar si es diferente para evitar eventos innecesarios.
	        if (!Objects.equals(toggleMarkImageAction.getValue(Action.SELECTED_KEY), estaMarcada)) {
	            toggleMarkImageAction.putValue(Action.SELECTED_KEY, estaMarcada);
	        }
	    }

	    // --- 2. Sincronizar el Botón de la Toolbar (a través del ConfigManager y el ThemeApplier) ---
	    //    En lugar de decirle a la vista que cambie el color del botón, dejamos que
	    //    el ConfigApplicationManager se encargue de refrescar los botones toggle.
	    //    O, para un refresco más inmediato y específico:
	    if (configAppManager != null) {
	        configAppManager.sincronizarEstadoVisualBotonesToggle();
	    }
	    
	    // --- 3. Actualizar la Barra de Estado ---
	    //    Le decimos al manager de la barra de estado que se actualice.
	    //    Él se encargará de leer el modelo (que sabe si la imagen está marcada
	    //    a través del ProjectManager) y mostrar "[MARCADA]" si es necesario.
	    if (statusBarManager != null) {
	        statusBarManager.actualizar();
	    }

	    System.out.println("  [Controller] Estado visual de 'Marcar' actualizado. Marcada: " + estaMarcada);
	} // --- Fin del método actualizarEstadoVisualBotonMarcarYBarraEstado ---
  
	
	  
//	public void actualizarEstadoVisualBotonMarcarYBarraEstado (boolean estaMarcada)
//	{
//
//		if (view == null || model == null)
//			return;
//
//		// 1. Actualizar el aspecto del botón toggle en la toolbar
//	    Action toggleMarkImageAction = (this.actionMap != null) ? this.actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA) : null;
//		if (toggleMarkImageAction != null)
//		{ // toggleMarkImageAction es el campo de la Action en VisorController
//			// Asegurarse que el SELECTED_KEY de la action está sincronizado (aunque ya
//			// debería estarlo
//			// si la llamada vino de la propia action).
//			toggleMarkImageAction.putValue(Action.SELECTED_KEY, estaMarcada);
//			this.view.actualizarAspectoBotonToggle(toggleMarkImageAction, estaMarcada);
//		}
//
//		// 2. Actualizar la barra de estado
//		String rutaActual = model.getRutaCompleta(model.getSelectedImageKey()) != null
//				? model.getRutaCompleta(model.getSelectedImageKey()).toString()
//				: (model.getSelectedImageKey() != null ? model.getSelectedImageKey() : "");
//
//		if (estaMarcada)
//		{
//			view.setTextoBarraEstadoRuta(rutaActual + " [MARCADA]");
//		} else
//		{
//			view.setTextoBarraEstadoRuta(rutaActual);
//		}
//		System.out.println("  [Controller] Barra de estado y botón 'Marcar' actualizados. Marcada: " + estaMarcada);
//
//	}

	public void toggleMarcaImagenActual (boolean marcarDeseado)
	{ // 'marcarDeseado' es el estado final que queremos
	    Action toggleMarkImageAction = (this.actionMap != null) ? this.actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA) : null;
		if (model == null || projectManager == null || toggleMarkImageAction == null)
		{
			System.err.println("ERROR [toggleMarcaImagenActual]: Modelo, ProjectManager o Action nulos.");
			return;
		}
		String claveActualVisor = model.getSelectedImageKey();

		if (claveActualVisor == null || claveActualVisor.isEmpty())
		{
			System.out.println("[Controller toggleMarca] No hay imagen seleccionada.");
			// Si no hay imagen, el estado de marca debe ser 'false'
			toggleMarkImageAction.putValue(Action.SELECTED_KEY, Boolean.FALSE);
			actualizarEstadoVisualBotonMarcarYBarraEstado(false, null);
			return;
		}

		Path rutaAbsolutaImagen = model.getRutaCompleta(claveActualVisor);

		if (rutaAbsolutaImagen == null)
		{
			System.err.println(
					"ERROR [toggleMarcaImagenActual]: No se pudo obtener ruta absoluta para " + claveActualVisor);
			toggleMarkImageAction.putValue(Action.SELECTED_KEY, Boolean.FALSE);
			actualizarEstadoVisualBotonMarcarYBarraEstado(false, null);
			return;
		}

		// La lógica de alternar ahora está en ProjectManager si se quiere,
		// pero la Action ya nos dice el estado final deseado.
		// Así que simplemente le decimos al ProjectManager que marque o desmarque.
		if (marcarDeseado)
		{
			projectManager.marcarImagenInterno(rutaAbsolutaImagen);
		} else
		{
			projectManager.desmarcarImagenInterno(rutaAbsolutaImagen);
		}

		// Sincronizar el SELECTED_KEY de la Action con el estado real (que debería ser
		// marcarDeseado)
		toggleMarkImageAction.putValue(Action.SELECTED_KEY, marcarDeseado);
		actualizarEstadoVisualBotonMarcarYBarraEstado(marcarDeseado, rutaAbsolutaImagen);
		System.out.println("  [Controller] Estado de marca procesado para: " + rutaAbsolutaImagen + ". Marcada: "
				+ marcarDeseado);

	}

//	public void actualizarEstadoVisualBotonMarcarYBarraEstado(boolean estaMarcada, Path rutaParaBarraEstado) {
//	    if (view == null) return;
//	    
//	    // 1. Actualizar el botón de la toolbar
//	    Action toggleMarkImageAction = (this.actionMap != null) ? this.actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA) : null;
//	    if (toggleMarkImageAction != null) {
//	        if (!Objects.equals(toggleMarkImageAction.getValue(Action.SELECTED_KEY), estaMarcada)) {
//	            toggleMarkImageAction.putValue(Action.SELECTED_KEY, estaMarcada);
//	        }
//	        this.view.actualizarAspectoBotonToggle(toggleMarkImageAction, estaMarcada);
//	    }
//
//	    // 2. Notificar al InfoBarManager que se encargue del resto.
//	    if (infobarImageManager != null) {
//	        infobarImageManager.actualizar();
//	    }
//	    if (statusBarManager != null) {
//	        statusBarManager.actualizar();
//	    }
//	} // --- FIN del método ---
	
	
// ************************************************************************************************** FIN GESTION DE PROYECTOS
// ***************************************************************************************************************************
	  
	  
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
	
	
	public void setZoomManager(ZoomManager zoomManager) {
	    this.zoomManager = zoomManager;
	}		

	/**
	 * Devuelve el mapa completo de Actions (incluyendo navegación).
	 * Usado por AppInitializer para pasarlo a los builders durante la creación de la UI.
	 * Es package-private para limitar su visibilidad.
	 *
	 * @return El mapa de acciones (comando canónico -> Action).
	 */
	Map<String, Action> getActionMap() { // Sin modificador de acceso = package-private
	    return this.actionMap;
	}


// Setters

	// --- NUEVO: Getters para que AppInitializer obtenga datos ---
	public ViewUIConfig getUiConfigForView() { return uiConfigForView; }
	public ThumbnailService getServicioMiniaturas() { return servicioMiniaturas; }
	public int getCalculatedMiniaturePanelHeight() { return calculatedMiniaturePanelHeight; }
	public IconUtils getIconUtils() { return iconUtils; } // Necesario para createNavigationActions...	  
	  
	public void setProjectManager(ProjectManager projectManager) {
	    this.projectManager = projectManager;
	}

    
    /**
     * Solicita que la JList de miniaturas se repinte.
     * Esto hará que MiniaturaListCellRenderer se ejecute para las celdas visibles
     * y pueda leer el nuevo estado de configuración para mostrar/ocultar nombres.
     */
    public void solicitarRefrescoRenderersMiniaturas() {
        if (view != null && registry.get("list.miniaturas") != null) {
            System.out.println("  [Controller] Solicitando repintado de listaMiniaturas.");
            registry.get("list.miniaturas").repaint();

            // Si ocultar/mostrar nombres cambia la ALTURA de las celdas,
            // podrías necesitar más que un simple repaint().
            // Por ahora, asumamos que la altura de la celda es fija y solo cambia
            // la visibilidad del JLabel del nombre.
            // Si la altura cambia, necesitarías:
            // 1. Que MiniaturaListCellRenderer devuelva una nueva PreferredSize.
            // 2. Invalidar el layout de la JList:
            //    registry.get("list.miniaturas").revalidate();
            //    registry.get("list.miniaturas").repaint();
            // 3. Posiblemente recalcular el número de miniaturas visibles si la altura de celda cambió.
            //    Esto haría que el `ComponentListener` de redimensionamiento sea más complejo
            //    o que necesites llamar a actualizarModeloYVistaMiniaturas aquí también.
            // ¡POR AHORA, MANTENGAMOSLO SIMPLE CON SOLO REPAINT!
        }
    } // --- FIN metodo solicitarRefrescoRenderersMiniaturas 
    
    
    /**
     * Establece si se deben mostrar los nombres de archivo debajo de las miniaturas
     * y refresca el renderer para que el cambio visual sea inmediato.
     *
     * @param mostrar El nuevo estado deseado: true para mostrar nombres, false para ocultarlos.
     */
    public void setMostrarNombresMiniaturas(boolean mostrar) {
        System.out.println("[VisorController] Solicitud para cambiar 'Mostrar Nombres en Miniaturas' a: " + mostrar);

        // --- 1. VALIDACIÓN DE DEPENDENCIAS ESENCIALES ---
        if (configuration == null || view == null || registry.get("list.miniaturas") == null || this.model == null ||
            this.servicioMiniaturas == null || this.themeManager == null || this.iconUtils == null) {
            System.err.println("ERROR CRÍTICO [setMostrarNombresMiniaturas]: Faltan dependencias esenciales (config, view, model, etc.). Operación cancelada.");
            return;
        }

        // --- 2. ACTUALIZAR LA CONFIGURACIÓN PERSISTENTE ---
        configuration.setString(ConfigKeys.VISTA_MOSTRAR_NOMBRES_MINIATURAS_STATE, String.valueOf(mostrar));
        System.out.println("  -> Configuración '" + ConfigKeys.VISTA_MOSTRAR_NOMBRES_MINIATURAS_STATE + "' actualizada en memoria a: " + mostrar);

        // --- 3. RECREAR Y APLICAR EL RENDERER DE MINIATURAS EN LA VISTA ---
        System.out.println("  -> Preparando para recrear y asignar nuevo MiniaturaListCellRenderer...");

        // 3.1. Obtener solo las dimensiones (los colores ya no son necesarios aquí).
        int thumbWidth = configuration.getInt("miniaturas.tamano.normal.ancho", 40);
        int thumbHeight = configuration.getInt("miniaturas.tamano.normal.alto", 40);

        // 3.2. Crear la nueva instancia del renderer usando el CONSTRUCTOR MODERNO.
        MiniaturaListCellRenderer newRenderer = new MiniaturaListCellRenderer(
            this.servicioMiniaturas,
            this.model,
            this.themeManager,         // <--- Le pasamos el ThemeManager
            this.iconUtils,            // <--- Le pasamos el IconUtils
            thumbWidth,
            thumbHeight,
            mostrar                    // <--- Le pasamos el flag de comportamiento
        );
        System.out.println("    -> Nueva instancia de MiniaturaListCellRenderer creada con el constructor moderno.");

        // 3.3. Asignar el nuevo renderer a la JList (sin cambios en esta parte).
        final MiniaturaListCellRenderer finalRenderer = newRenderer;
        SwingUtilities.invokeLater(() -> {
            JList<String> listaMin = registry.get("list.miniaturas");
            listaMin.setCellRenderer(finalRenderer);
            listaMin.setFixedCellHeight(finalRenderer.getAlturaCalculadaDeCelda());
            listaMin.setFixedCellWidth(finalRenderer.getAnchoCalculadaDeCelda());
            listaMin.revalidate();
            listaMin.repaint();
            System.out.println("      [EDT] Nuevo renderer asignado y lista de miniaturas actualizada.");

            // ... (el resto de tu lógica de actualización del listCoordinator se mantiene)
        });

        System.out.println("[VisorController] setMostrarNombresMiniaturas completado.");
    }// --- FIN del metodo setMostrarNombresMiniaturas ---

	
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
    
    
    public ProjectManager getProjectManager() {
        return this.projectManager;
    }

    
	/** Getters para Modelo/Vista/Config (usados por Actions). */
    public VisorModel getModel() { return model; }
    public VisorView getView() { return view; }
    public ConfigurationManager getConfigurationManager() { return configuration; }
     
     
    public ListCoordinator getListCoordinator() {return this.listCoordinator;}
	 
    public void setConfigApplicationManager(ConfigApplicationManager manager) { this.configAppManager = manager; }
    public ConfigApplicationManager getConfigApplicationManager() { return this.configAppManager; }
     
     
    /**
     * Método centralizado para cambiar el estado de "Navegación Circular".
     * Actualiza el modelo, la configuración, y sincroniza la UI (incluyendo el
     * JCheckBoxMenuItem asociado y los botones de navegación).
     * Este método es llamado por ToggleNavegacionCircularAction.
     * @param nuevoEstadoCircular El nuevo estado deseado para la navegación circular.
     */
    public void setNavegacionCircularLogicaYUi(boolean nuevoEstadoCircular) {
        System.out.println("[VisorController setNavegacionCircularLogicaYUi] Nuevo estado deseado: " + nuevoEstadoCircular);

        if (model == null || configuration == null || actionMap == null || getListCoordinator() == null) {
            System.err.println("  ERROR [VisorController setNavegacionCircularLogicaYUi]: Dependencias nulas. Abortando.");
            return;
        }

        // 1. Actualizar el VisorModel
        model.setNavegacionCircularActivada(nuevoEstadoCircular);
        // El modelo ya imprime su log: "[VisorModel] Navegación Circular cambiada a: ..."

        // 2. Actualizar ConfigurationManager (en memoria)
        String configKey = "comportamiento.navegacion.circular"; // La clave que usa la Action
        configuration.setString(configKey, String.valueOf(nuevoEstadoCircular));
        System.out.println("  -> Configuración '" + configKey + "' actualizada en memoria a: " + nuevoEstadoCircular);

        // 3. Sincronizar la UI:
        //    a) El JCheckBoxMenuItem asociado a la Action
        Action toggleAction = actionMap.get(AppActionCommands.CMD_TOGGLE_WRAP_AROUND);
        if (toggleAction != null) {
            // Comprobar si el estado de la Action ya es el correcto, para evitar ciclos si algo más lo cambió.
            // Aunque en este flujo, nosotros somos la fuente del cambio.
            if (!Boolean.valueOf(nuevoEstadoCircular).equals(toggleAction.getValue(Action.SELECTED_KEY))) {
                toggleAction.putValue(Action.SELECTED_KEY, nuevoEstadoCircular);
                System.out.println("    -> Action.SELECTED_KEY para CMD_TOGGLE_WRAP_AROUND actualizado a: " + nuevoEstadoCircular);
            }
        } else {
            System.err.println("WARN [VisorController setNavegacionCircularLogicaYUi]: No se encontró Action para CMD_TOGGLE_WRAP_AROUND.");
        }

        //    b) El estado de los botones de navegación
        getListCoordinator().forzarActualizacionEstadoNavegacion();
        System.out.println("    -> Forzada actualización del estado de botones de navegación.");

        System.out.println("[VisorController setNavegacionCircularLogicaYUi] Proceso completado.");
    }
     
     
    /**
     * Método centralizado para cambiar el estado de "Mantener Proporciones".
     * Actualiza el modelo, la configuración, refresca la imagen y sincroniza la UI.
     * Este método es llamado por ToggleProporcionesAction.
     * @param nuevoEstadoMantener El nuevo estado deseado para mantener proporciones.
     */
 	public void setMantenerProporcionesLogicaYUi(boolean nuevoEstadoMantener) { // Parámetro
 	    System.out.println("[VisorController setMantenerProporcionesLogicaYUi] Nuevo estado deseado: " + nuevoEstadoMantener);
 	
 	    if (model == null || configuration == null || zoomManager == null || actionMap == null || view == null) {
 	        System.err.println("  ERROR [VisorController setMantenerProporcionesLogicaYUi]: Dependencias nulas. Abortando.");
 	        return;
 	    }
 	
 	    // 1. Actualizar el VisorModel
 	    model.setMantenerProporcion(nuevoEstadoMantener); 
 	
 	    // 2. Actualizar ConfigurationManager (en memoria)
 	    String configKey = "interfaz.menu.zoom.mantener_proporciones.seleccionado";
 	    configuration.setString(configKey, String.valueOf(nuevoEstadoMantener)); // Usa el parámetro
 	    System.out.println("  -> Configuración '" + configKey + "' actualizada en memoria a: " + nuevoEstadoMantener);
 	
 	    // 3. SINCRONIZAR LA UI
 	    sincronizarUiControlesProporciones(nuevoEstadoMantener); // Usa el parámetro
 	
 	    // 4. Refrescar la imagen principal en la vista
 	    System.out.println("  -> Solicitando a ZoomManager que refresque la imagen principal...");
 	     
 	    if (this.zoomManager != null && this.model != null && this.model.getCurrentZoomMode() != null) {
 	        System.out.println("  [VisorController] Reaplicando modo de zoom actual: " + model.getCurrentZoomMode() + " debido a cambio de proporciones.");

// 	        boolean modoDeZoomCambiadoEnManager = this.zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
 	        this.zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
 	        
 	        // DESPUÉS de que ZoomManager ha hecho su trabajo y el modelo (currentZoomMode y zoomFactor)
 	        // está actualizado, AHORA sincronizamos los botones/radios de los modos de zoom.
 	        sincronizarEstadoVisualBotonesYRadiosZoom(); 

 	        // El 'modoDeZoomCambiadoEnManager' nos dice si el *tipo* de modo cambió.
 	        // Incluso si no cambió (ej. seguía siendo FIT_TO_SCREEN), el factor SÍ pudo haber cambiado
 	        // debido al nuevo estado de 'mantenerProporciones', por lo que la sincronización
 	        // de los botones de zoom (para que el correcto esté activo) sigue siendo necesaria.

 	    } else if (this.zoomManager != null) { 
 	        // Si no hay un modo de zoom automático (ej. podría estar en zoom manual), 
 	        // simplemente refrescar con el estado de proporciones actual.
 	    	this.zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
 	    } else {
 	        System.err.println("ERROR [setMantenerProporcionesLogicaYUi]: ZoomManager es null al intentar refrescar.");
 	    }
 	     
 	    //LOG VisorController DEBUG
// 	    System.out.println("  [VisorController DEBUG] Estado del MODELO ANTES DE REFRESCAR ZOOM: model.isMantenerProporcion()=" + model.isMantenerProporcion());
 	
 	  // <--- ACTUALIZAR BARRAS --->  
 	     if (infobarImageManager != null) {
 	    	 infobarImageManager.actualizar();
 	     }
 	     if (statusBarManager != null) {
 	    	 statusBarManager.actualizar();
 	     }	     
 	     
 	     System.out.println("[VisorController setMantenerProporcionesLogicaYUi] Proceso completado.");
 	} //--- FIN del metodo setMantenerProporcionesLogicaYUi ---
     
 	 
 	public void setMostrarSubcarpetasLogicaYUi(boolean nuevoEstadoIncluirSubcarpetas) {
 				
 	    System.out.println("[VisorController setMostrarSubcarpetasLogicaYUi] Nuevo estado deseado (incluir): " + nuevoEstadoIncluirSubcarpetas);
 	    
 	    if (model == null || configuration == null 
 	    		// || fileOperationsManager == null  
 	    		|| actionMap == null || view == null) {
 	        System.err.println("  ERROR [VisorController setMostrarSubcarpetasLogicaYUi]: Dependencias nulas. Abortando.");
 	        return;
 	    }

 	    // 1. Actualizar VisorModel
 	    System.out.println("  MODELO ANTES DE CAMBIO: model.isMostrarSoloCarpetaActual() = " + model.isMostrarSoloCarpetaActual());
 	    model.setMostrarSoloCarpetaActual(!nuevoEstadoIncluirSubcarpetas); // Usa el parámetro (con la negación correcta)
 	    System.out.println("  MODELO DESPUÉS DE CAMBIO: model.isMostrarSoloCarpetaActual() = " + model.isMostrarSoloCarpetaActual());
 	    
 	    // 2. Actualizar ConfigurationManager
 	    configuration.setString("comportamiento.carpeta.cargarSubcarpetas", String.valueOf(nuevoEstadoIncluirSubcarpetas)); // Usa el parámetro

 	    // 3. SINCRONIZAR LA UI
 	    sincronizarUiControlesSubcarpetas(nuevoEstadoIncluirSubcarpetas); // Usa el parámetro

 	    // 4. Disparar recarga de la lista de imágenes
 	    String claveAntesDelCambio = model.getSelectedImageKey(); 
 	    System.out.println("  -> Solicitando recarga de lista de imágenes (manteniendo si es posible: " + claveAntesDelCambio + ")");
 	    
 	    //LOG 
 	    claveAntesDelCambio = model.getSelectedImageKey();
 	    System.out.println("  LLAMANDO A cargarListaImagenes con claveAntesDelCambio=" + claveAntesDelCambio + 
                 " y model.isMostrarSoloCarpetaActual()=" + model.isMostrarSoloCarpetaActual());
 	    this.cargarListaImagenes(claveAntesDelCambio, null);

 	    System.out.println("[VisorController setMostrarSubcarpetasLogicaYUi] Proceso completado.");
 	}// FIN metodo setMostrarSubcarpetasLogicaYUi
 	
 	
 	public void setInfobarImageManager(InfobarImageManager manager) { this.infobarImageManager = manager; }
 	public void setStatusBarManager(InfobarStatusManager manager) { this.statusBarManager = manager; }
     
 	public void setComponentRegistry(ComponentRegistry registry) {this.registry = registry;}
    public ComponentRegistry getComponentRegistry() {return this.registry;}
 	 	
    public void setBotonesPorNombre(Map<String, JButton> botones) {this.botonesPorNombre = (botones != null) ? botones : new HashMap<>();}
    public Map<String, JButton> getBotonesPorNombre() {return this.botonesPorNombre;}
    
    public void setMenuItemsPorNombre(Map<String, JMenuItem> menuItems) {this.menuItemsPorNombre = (menuItems != null) ? menuItems : new HashMap<>();}
    
    public ExecutorService getExecutorService() {return this.executorService;}
    
    
// ***************************************************************************************************** FIN GETTERS Y SETTERS
// ***************************************************************************************************************************    

// ***************************************************************************************************************************
// ************************************************************************************************* METODOS DE SINCRONIZACION

    
    public void sincronizarEstadoDeTodasLasToggleThemeActions() {
        System.out.println("[VisorController] Sincronizando estado de todas las ToggleThemeAction...");
        if (this.actionMap == null || this.themeManager == null) return;

        for (Action action : this.actionMap.values()) {
            if (action instanceof ToggleThemeAction) {
                ((ToggleThemeAction) action).sincronizarEstadoSeleccionConManager();
            }
        }
    }
    
    
    public void sincronizarControlesSubcarpetas() {
        if (model == null || actionMap == null) return;

        boolean estadoActualIncluirSubcarpetas = !model.isMostrarSoloCarpetaActual(); // ¡Ojo con la negación!

        // Sincronizar la Action del toggle general
        Action toggleAction = actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
        if (toggleAction instanceof ToggleSubfoldersAction) { // O un método genérico si tienes una interfaz
            // ((ToggleSubfoldersAction) toggleAction).sincronizarSelectedKey(estadoActualIncluirSubcarpetas);
            // Es mejor que la propia ToggleSubfoldersAction lea del modelo en su setSelectedKey.
            // Por ahora, podemos forzarlo:
            if (!Objects.equals(toggleAction.getValue(Action.SELECTED_KEY), estadoActualIncluirSubcarpetas)) {
                toggleAction.putValue(Action.SELECTED_KEY, estadoActualIncluirSubcarpetas);
            }
        }

        // Sincronizar la Action del radio "Solo Carpeta"
        Action soloCarpetaAction = actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA);
        if (soloCarpetaAction instanceof SetSubfolderReadModeAction) {
            ((SetSubfolderReadModeAction) soloCarpetaAction).sincronizarSelectedKey(estadoActualIncluirSubcarpetas);
        }

        // Sincronizar la Action del radio "Con Subcarpetas"
        Action conSubcarpetasAction = actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS);
        if (conSubcarpetasAction instanceof SetSubfolderReadModeAction) {
            ((SetSubfolderReadModeAction) conSubcarpetasAction).sincronizarSelectedKey(estadoActualIncluirSubcarpetas);
        }
        
        // Adicionalmente, el método que ya tenías para los radios directamente:
        restaurarSeleccionRadiosSubcarpetas(estadoActualIncluirSubcarpetas);
     // Y para el botón de la toolbar:
        toggleAction = actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
        if (toggleAction != null) {
            // <<< INICIO DEL CAMBIO >>>
            
            // En lugar de llamar a view.actualizarAspectoBotonToggle(...)
            // llamamos al método en ConfigApplicationManager.
            if (this.configAppManager != null) {
                this.configAppManager.actualizarAspectoBotonToggle(toggleAction, estadoActualIncluirSubcarpetas);
            } else {
                System.err.println("WARN [sincronizarControlesSubcarpetas]: configAppManager es nulo, no se puede actualizar el botón toggle.");
            }
            
            // <<< FIN DEL CAMBIO >>>
        }
    } // --- FIN del metodo sincronizarControlesSubcarpetas ---
    
    
    
	
    
    /**
     * Sincroniza el estado SELECTED_KEY de la ToggleProporcionesAction
     * y la apariencia del botón de la toolbar asociado.
     * @param estadoActualMantenerProporciones El estado actual de 'mantenerProporciones' según el modelo.
     */
    private void sincronizarUiControlesProporciones(boolean estadoActualMantenerProporciones) {
        System.out.println("  [VisorController sincronizarUiControlesProporciones] Sincronizando UI con estado: " + estadoActualMantenerProporciones);
        if (actionMap == null) return; // Ya no necesitamos 'view' para esta lógica

        Action action = actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);
        
        // La sincronización del estado de la Action se mantiene igual
        if (action instanceof ToggleProporcionesAction) {
            ((ToggleProporcionesAction) action).sincronizarSelectedKeyConModelo(estadoActualMantenerProporciones);
        } else if (action != null) {
            if (!Objects.equals(action.getValue(Action.SELECTED_KEY), estadoActualMantenerProporciones)) {
                action.putValue(Action.SELECTED_KEY, estadoActualMantenerProporciones);
            }
        }

        // Actualizar el aspecto visual del botón de la toolbar
        if (action != null) {
            // <<< INICIO DEL CAMBIO >>>
            if (this.configAppManager != null) {
                this.configAppManager.actualizarAspectoBotonToggle(action, estadoActualMantenerProporciones);
            } else {
                System.err.println("WARN [sincronizarUiControlesProporciones]: configAppManager es nulo, no se puede actualizar el botón toggle.");
            }
            // <<< FIN DEL CAMBIO >>>
        }
    } // --- FIN del metodo sincronizarUiControlesProporciones ---
    
    
    /**
     * Sincroniza el estado visual de todos los controles de la UI relacionados
     * con la configuración de "incluir subcarpetas" (toggle general y radios del menú)
     * para que coincidan con el estado actual del modelo.
     *
     * @param estadoModeloIncluirSubcarpetas true si el modelo indica que se deben incluir subcarpetas,
     *                                       false si solo se debe mostrar la carpeta actual.
     */
    private void sincronizarUiControlesSubcarpetas(boolean estadoModeloIncluirSubcarpetas) {
        // --- SECCIÓN 1: LOG INICIAL Y VALIDACIONES ---
        System.out.println("  [VisorController sincronizarUiControlesSubcarpetas] Iniciando sincronización de UI. Estado modelo (incluir subcarpetas): " + estadoModeloIncluirSubcarpetas);

        // 1.1. Validar que las dependencias necesarias (actionMap y view) existan.
        if (actionMap == null || view == null) {
            System.err.println("    ERROR [sincronizarUiControlesSubcarpetas]: actionMap o view son nulos. No se puede sincronizar UI.");
            return;
        }

        // --- SECCIÓN 2: SINCRONIZAR LA ACTION DEL TOGGLE GENERAL (JCHECKBOXMENUITEM O JTOGGLEBUTTON) ---
        // 2.1. Obtener la Action del toggle general desde el actionMap.
        Action toggleGeneralAction = actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);

        // 2.2. Verificar si la Action existe.
        if (toggleGeneralAction != null) {
            // 2.2.1. Comprobar si es la instancia esperada (opcional pero bueno para asegurar el tipo).
            if (toggleGeneralAction instanceof ToggleSubfoldersAction) {
                // 2.2.1.1. Llamar al método de sincronización específico de la Action.
                ((ToggleSubfoldersAction) toggleGeneralAction).sincronizarSelectedKeyConModelo(estadoModeloIncluirSubcarpetas);
            } else {
                // 2.2.1.2. Fallback genérico: actualizar SELECTED_KEY directamente si no es del tipo esperado pero existe.
                System.out.println("    WARN [sincronizarUiControlesSubcarpetas]: CMD_TOGGLE_SUBCARPETAS no es instancia de ToggleSubfoldersAction. Actualizando SELECTED_KEY genéricamente.");
                if (!Objects.equals(toggleGeneralAction.getValue(Action.SELECTED_KEY), estadoModeloIncluirSubcarpetas)) {
                    toggleGeneralAction.putValue(Action.SELECTED_KEY, estadoModeloIncluirSubcarpetas);
                }
            }
            // 2.2.2. Actualizar el aspecto visual del botón de la toolbar asociado a esta Action.
            System.out.println("VisorController Sync: Pasando Action@" + Integer.toHexString(System.identityHashCode(toggleGeneralAction)) + 
                    " a view.actualizarAspectoBotonToggle.");
            
            if (this.configAppManager != null) {
                this.configAppManager.actualizarAspectoBotonToggle(toggleGeneralAction, estadoModeloIncluirSubcarpetas);
            } else {
                System.err.println("WARN [sincronizarUiControlesSubcarpetas]: configAppManager es nulo, no se puede actualizar el botón toggle.");
            }
            
        } else {
            System.err.println("    ERROR [sincronizarUiControlesSubcarpetas]: No se encontró Action para CMD_TOGGLE_SUBCARPETAS en actionMap.");
        }

        // --- SECCIÓN 3: SINCRONIZAR LAS ACTIONS DE LOS JRADIOBUTTONMENUITEM DEL MENÚ ---

        // 3.1. Sincronizar la Action para "Mostrar Solo Carpeta Actual"
        //      - El estado seleccionado de este radio debe ser el OPUESTO a estadoModeloIncluirSubcarpetas.
        boolean estadoParaRadioSoloCarpeta = !estadoModeloIncluirSubcarpetas;
        Action soloCarpetaAction = actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA);
        if (soloCarpetaAction != null) {
            if (soloCarpetaAction instanceof SetSubfolderReadModeAction) {
                // El método sincronizarSelectedKey en SetSubfolderReadModeAction compara
                // el estadoModeloIncluirSubcarpetas con el estadoQueRepresentaLaAction.
                ((SetSubfolderReadModeAction) soloCarpetaAction).sincronizarSelectedKey(estadoModeloIncluirSubcarpetas);
            } else {
                System.out.println("    WARN [sincronizarUiControlesSubcarpetas]: CMD_CONFIG_CARGA_SOLO_CARPETA no es instancia de SetSubfolderReadModeAction. Actualizando SELECTED_KEY genéricamente.");
                if (!Objects.equals(soloCarpetaAction.getValue(Action.SELECTED_KEY), estadoParaRadioSoloCarpeta)) {
                    soloCarpetaAction.putValue(Action.SELECTED_KEY, estadoParaRadioSoloCarpeta);
                }
            }
        } else {
            System.err.println("    ERROR [sincronizarUiControlesSubcarpetas]: No se encontró Action para CMD_CONFIG_CARGA_SOLO_CARPETA.");
        }

        // 3.2. Sincronizar la Action para "Mostrar Imágenes de Subcarpetas"
        //      - El estado seleccionado de este radio debe ser IGUAL a estadoModeloIncluirSubcarpetas.
        boolean estadoParaRadioConSubcarpetas = estadoModeloIncluirSubcarpetas;
        Action conSubcarpetasAction = actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS);
        if (conSubcarpetasAction != null) {
            if (conSubcarpetasAction instanceof SetSubfolderReadModeAction) {
                ((SetSubfolderReadModeAction) conSubcarpetasAction).sincronizarSelectedKey(estadoModeloIncluirSubcarpetas);
            } else {
                System.out.println("    WARN [sincronizarUiControlesSubcarpetas]: CMD_CONFIG_CARGA_CON_SUBCARPETAS no es instancia de SetSubfolderReadModeAction. Actualizando SELECTED_KEY genéricamente.");
                if (!Objects.equals(conSubcarpetasAction.getValue(Action.SELECTED_KEY), estadoParaRadioConSubcarpetas)) {
                    conSubcarpetasAction.putValue(Action.SELECTED_KEY, estadoParaRadioConSubcarpetas);
                }
            }
        } else {
            System.err.println("    ERROR [sincronizarUiControlesSubcarpetas]: No se encontró Action para CMD_CONFIG_CARGA_CON_SUBCARPETAS.");
        }

        // 3.3. (Opcional si las Actions y ButtonGroup no son suficientes) Restaurar selección visual de radios.
        //      Si MenuBarBuilder configuró correctamente los JRadioButtonMenuItems con sus Actions
        //      y los añadió a un ButtonGroup, el cambio en Action.SELECTED_KEY debería ser suficiente
        //      para que el ButtonGroup actualice visualmente cuál radio está seleccionado.
        //      Si esto no ocurre, restaurarSeleccionRadiosSubcarpetas puede ser un reaseguro.
        //      Por ahora, lo comentaremos para ver si las Actions son suficientes.
        // restaurarSeleccionRadiosSubcarpetas(estadoModeloIncluirSubcarpetas);
        // System.out.println("    -> (Opcional) Llamada a restaurarSeleccionRadiosSubcarpetas con: " + estadoModeloIncluirSubcarpetas);


        // --- SECCIÓN 4: LOG FINAL ---
        System.out.println("  [VisorController sincronizarUiControlesSubcarpetas] Sincronización de UI completada.");
    } // --- FIN del metodo sincronizarUiControlesSubcarpetas
    
    

    public void sincronizarEstadoVisualBotonesYRadiosZoom() {
        if (this.actionMap == null || this.model == null || this.configAppManager == null) {
            System.err.println("WARN [sincronizarEstadoVisualBotonesYRadiosZoom]: Dependencias nulas.");
            return;
        }
        
        ZoomModeEnum modoActivo = model.getCurrentZoomMode();
        boolean permisoManualActivo = model.isZoomHabilitado();
        System.out.println("[VisorController] Sincronizando UI de Zoom. Modo: " + modoActivo + ", Permiso Manual: " + permisoManualActivo);

        if (modoActivo == null) return;

        // Guardar el último modo en la configuración en memoria
        if (configuration != null) {
            configuration.setString(
                ConfigKeys.COMPORTAMIENTO_ZOOM_ULTIMO_MODO,
                modoActivo.name()
            );
        }
        
        // Lista de todos los comandos de acción para los modos de zoom
        java.util.List<String> zoomModeCommands = java.util.List.of(
            AppActionCommands.CMD_ZOOM_TIPO_AUTO,
            AppActionCommands.CMD_ZOOM_TIPO_ANCHO,
            AppActionCommands.CMD_ZOOM_TIPO_ALTO,
            AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR,
            AppActionCommands.CMD_ZOOM_TIPO_FIJO,
            AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO,
            AppActionCommands.CMD_ZOOM_TIPO_RELLENAR
        );

        // Iterar sobre los comandos y actualizar cada Action y su botón asociado
        for (String command : zoomModeCommands) {
            Action action = actionMap.get(command);
            if (action instanceof AplicarModoZoomAction) {
                AplicarModoZoomAction zoomAction = (AplicarModoZoomAction) action;
                
                boolean isSelected = zoomAction.getModoAsociado() == modoActivo;
                
                action.putValue(Action.SELECTED_KEY, isSelected);
                this.configAppManager.actualizarAspectoBotonToggle(action, isSelected);
            }
            
            
        }
        
        // Sincronizar la Action de Reset
        Action resetAction = actionMap.get(AppActionCommands.CMD_ZOOM_RESET);
        if (resetAction != null) {
            boolean zoomNoEsDefault = Math.abs(model.getZoomFactor() - 1.0) > 0.001;
            boolean panNoEsDefault = model.getImageOffsetX() != 0 || model.getImageOffsetY() != 0;
            resetAction.setEnabled(permisoManualActivo || zoomNoEsDefault || panNoEsDefault);
        }
        
        // Sincronizar el Toggle de Permiso Manual
        Action toggleManualAction = actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
        if(toggleManualAction instanceof ToggleZoomManualAction){
            ((ToggleZoomManualAction)toggleManualAction).sincronizarEstadoConModelo();
            configAppManager.actualizarAspectoBotonToggle(toggleManualAction, permisoManualActivo);
        }
        
        // Actualizar las barras de información
        if (infobarImageManager != null) infobarImageManager.actualizar();
        if (statusBarManager != null) statusBarManager.actualizar();
        
        System.out.println("[VisorController] Sincronización de UI de Zoom completada.");
        
        
    }
    // --- FIN del metodo sincronizarEstadoVisualBotonesYRadiosZoom ---
    
    
    public void notificarCambioEstadoZoomManual() {
        System.out.println("[VisorController] Notificado cambio de estado de zoom manual. Actualizando barras...");
        
     // << --- ACTUALIZAR BARRAS AL FINAL DE LA LIMPIEZA --- >>  
        if (infobarImageManager != null) {
            infobarImageManager.actualizar();
        }
        if (statusBarManager != null) {
            statusBarManager.actualizar();
        }
    }
    
    
    public void sincronizarAccionesFormatoBarraSuperior() {
        System.out.println("[VisorController] Sincronizando Actions de formato para Barra Superior...");
        if (actionMap == null) return;

        String[] comandosFormatoSuperior = {
            AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_SOLO_NOMBRE,
            AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_SUPERIOR_NOMBRE_RUTA_RUTA_COMPLETA
        };

        for (String cmd : comandosFormatoSuperior) {
            Action action = actionMap.get(cmd);
            if (action instanceof SetInfoBarTextFormatAction) {
                ((SetInfoBarTextFormatAction) action).sincronizarSelectedKeyConConfig();
            }
        }
    }

    public void sincronizarAccionesFormatoBarraInferior() {
        System.out.println("[VisorController] Sincronizando Actions de formato para Barra Inferior...");
        if (actionMap == null) return;

        String[] comandosFormatoInferior = {
            AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_SOLO_NOMBRE,
            AppActionCommands.CMD_INFOBAR_CONFIG_FORMATO_INFERIOR_NOMBRE_RUTA_RUTA_COMPLETA
        };

        for (String cmd : comandosFormatoInferior) {
            Action action = actionMap.get(cmd);
            if (action instanceof SetInfoBarTextFormatAction) {
                ((SetInfoBarTextFormatAction) action).sincronizarSelectedKeyConConfig();
            }
        }
    }


    // --- En AppInitializer, o en un método de inicialización DENTRO de VisorController que AppInitializer llame:
    //     Después de que todas las Actions se han creado y el actionMap está poblado.
    /*package-private*/ void sincronizarEstadoVisualInicialDeRadiosDeFormato() {
        sincronizarAccionesFormatoBarraSuperior();
        sincronizarAccionesFormatoBarraInferior();
    }
    
    
    /**
     * REFACTORIZADO: Configura un listener que se dispara UNA SOLA VEZ, cuando la
     * ventana principal es mostrada y tiene dimensiones válidas por primera vez.
     * Su único propósito es corregir el zoom inicial.
     */
    void configurarListenerRedimensionVentana() {
        if (view == null) {
            System.err.println("ERROR [Controller - configurarListenerRedimensionamiento]: Vista nula.");
            return;
        }
        
        System.out.println("    [Controller] Configurando ComponentListener para el primer arranque...");

        view.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
                
                // Solo actuar si el panel tiene un tamaño válido y hay una imagen cargada.
                if (displayPanel != null && displayPanel.getWidth() > 0 && model != null && model.getCurrentImage() != null) {
                    
                    System.out.println("--- [Listener de Ventana] Primer redimensionado válido detectado. Re-aplicando modo de zoom inicial. ---");
                    
                    if (zoomManager != null) {
                        // Llama al método que ya tienes, que usará las dimensiones ahora correctas del panel.
                        zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
                    }
                    
                    // ¡Importante! Eliminar el listener después de que se haya ejecutado una vez.
                    view.removeComponentListener(this);
                    System.out.println("--- [Listener de Ventana] Tarea completada. Listener eliminado. ---");
                }
            }
        });
    } // --- FIN del metodo configurarListenerRedimensionVentana ---
    
    
    public void sincronizarUiControlesZoom(Action action, boolean isSelected) {
        if (configAppManager != null) {
            // Delega la actualización visual al manager correspondiente.
            configAppManager.actualizarEstadoControlesZoom(isSelected, isSelected);
            configAppManager.actualizarAspectoBotonToggle(action, isSelected);
        } else {
            System.err.println("WARN [sincronizarUiControlesZoom]: configAppManager es nulo.");
        }
    }
    
// ********************************************************************************************* FIN METODOS DE SINCRONIZACION
// ***************************************************************************************************************************    
    
    
// *************************************************************************** CLASE ANIDADA DE CONTROL DE MINIATURAS VISIBLES
// ***************************************************************************************************************************    
     
     
     public static class RangoMiniaturasCalculado { // Puede ser public o package-private
         public final int antes;
         public final int despues;

         public RangoMiniaturasCalculado(int antes, int despues) {
             this.antes = antes;
             this.despues = despues;
         }
     }

     
// ************************************************************************FIN CLASE ANIDADA DE CONTROL DE MINIATURAS VISIBLES
// ***************************************************************************************************************************    
    
} // --- FIN CLASE VisorController ---




