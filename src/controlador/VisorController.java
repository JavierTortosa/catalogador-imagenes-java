package controlador;

import java.awt.BorderLayout;
// --- Imports Esenciales ---
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import controlador.actions.archivo.DeleteAction;
import controlador.actions.config.SetInfoBarTextFormatAction;
import controlador.actions.config.SetSubfolderReadModeAction;
import controlador.actions.tema.ToggleThemeAction;
import controlador.actions.toggle.ToggleProporcionesAction;
import controlador.actions.toggle.ToggleSubfoldersAction;
import controlador.actions.zoom.AplicarModoZoomAction;
import controlador.commands.AppActionCommands;
import controlador.managers.InfoBarManager;
import controlador.managers.ZoomManager;
import controlador.worker.BuscadorArchivosWorker;
// --- Imports de Modelo, Servicios y Vista ---
import modelo.VisorModel;
import servicios.ConfigurationManager;
import servicios.ProjectManager;
import servicios.image.ThumbnailService;
import servicios.zoom.ZoomModeEnum;
import utils.StringUtils;
import vista.VisorView;
import vista.config.ViewUIConfig;
import vista.dialogos.ProgresoCargaDialog;
import vista.renderers.MiniaturaListCellRenderer;
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
    private InfoBarManager infoBarManager;			// Responsable de las barras de status
    
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
//            this.carpetaRaizActual = null; // Asegurar que sea null si no hay config.
            
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
        
        
     // --- SECCIÓN 3: Aplicar Configuración a Menús (Visibilidad, Enabled y Selección) ---
        // 3.1. Obtener el mapa de items de menú desde la vista.
        Map<String, JMenuItem> menuItems = view.getMenuItemsPorNombre();

        // 3.2. Comprobar si el mapa de menús existe.
        if (menuItems != null) {
            System.out.println("    -> Aplicando config a Menús (Vista)...");

            // 3.3. Iterar sobre cada entrada del mapa (claveCompletaMenu -> menuItem).
            //      'claveCompletaMenu' es la clave jerárquica generada por MenuBarBuilder
            //      (ej. "interfaz.menu.vista.barra_de_menu").
            menuItems.forEach((claveCompletaMenu, menuItem) -> {
                // 3.4. Bloque try-catch para manejar errores para un item específico.
                try {
                    // 3.4.1. Leer y aplicar el estado 'activado' (enabled) del JMenuItem.
                    //        Default: true (la mayoría de los menús están habilitados por defecto).
                    //        La clave en config.cfg sería, por ejemplo:
                    //        "interfaz.menu.vista.barra_de_menu.activado = true"
                    boolean activado = configuration.getBoolean(claveCompletaMenu + ".activado", true);
                    menuItem.setEnabled(activado);

                    // 3.4.2. Leer y aplicar el estado 'visible' del JMenuItem.
                    //        Default: true (la mayoría de los menús son visibles por defecto).
                    //        La clave en config.cfg sería, por ejemplo:
                    //        "interfaz.menu.vista.barra_de_menu.visible = true"
                    boolean visible = configuration.getBoolean(claveCompletaMenu + ".visible", true);
                    menuItem.setVisible(visible);

                    // 3.4.3. Determinar si el JMenuItem es un tipo seleccionable (Checkbox o RadioButton).
                    boolean esSeleccionable = (menuItem instanceof JCheckBoxMenuItem || menuItem instanceof JRadioButtonMenuItem);

                    // 3.4.4. Determinar si el JMenuItem tiene una Action directamente asignada DESDE NUESTRO actionMap.
                    //          Esto es importante para no interferir con Actions que ya manejan su propio estado SELECTED_KEY.
                    Action assignedAction = menuItem.getAction();
                    boolean tieneActionDeNuestroActionMap = (assignedAction != null &&
                                                            actionMap != null && // Asegurarse que actionMap del controller está inicializado
                                                            actionMap.containsValue(assignedAction) &&
                                                            !(assignedAction instanceof javax.swing.text.TextAction)); // Excluir actions de texto por defecto

                    // Log para depuración de cada JMenuItem seleccionable
                    if (esSeleccionable) {
                        System.out.println("  [APLICAR_CONFIG_VISTA - Menú Seleccionable] Clave: " + claveCompletaMenu +
                                           ", Texto: '" + menuItem.getText() +
                                           "', Tiene Action de ActionMap: " + tieneActionDeNuestroActionMap +
                                           ", ActionCommand del JMenuItem: '" + menuItem.getActionCommand() + "'");
                    }


                    // 3.4.5. Lógica para establecer el estado .selected INICIALMENTE:
                    if (esSeleccionable && !tieneActionDeNuestroActionMap) {
                        // CASO A: Es un JCheckBoxMenuItem o JRadioButtonMenuItem que NO tiene una Action de nuestro actionMap.
                        //         Esto incluye los checkboxes de "Visualizar Botones Toolbar".

                        System.out.println("    -> Menú Seleccionable SIN Action de ActionMap: '" + claveCompletaMenu + "'. Configurando su estado 'selected'...");

                        if (claveCompletaMenu.startsWith("interfaz.menu.configuracion.visualizar_botones.")) {
                            // SUB-CASO A1: Es un checkbox de "Visualizar Botones Toolbar".
                            // Su estado 'selected' debe reflejar el estado '.visible' del BOTÓN DE LA TOOLBAR que controla.
                            // El ActionCommand del JCheckBoxMenuItem (menuItem.getActionCommand())
                            // fue establecido por MenuBarBuilder para ser la clave del botón de la toolbar.
                            String toolbarButtonKey = menuItem.getActionCommand();
                            System.out.println("      -> Es Checkbox de 'Visualizar Botones'. ActionCommand (ToolbarButtonKey): '" + toolbarButtonKey + "'");

                            if (toolbarButtonKey != null && !toolbarButtonKey.isEmpty()) {
                                // Leer la configuración de visibilidad del BOTÓN DE LA TOOLBAR.
                                boolean botonDebeSerVisibleConfig = configuration.getBoolean(toolbarButtonKey + ".visible", true); // Default true si no se encuentra
                                ((JCheckBoxMenuItem) menuItem).setSelected(botonDebeSerVisibleConfig);
                                // menuItem.setEnabled(true); // El .setEnabled general ya se hizo arriba.
                                System.out.println("      -> Checkbox '" + claveCompletaMenu + "' SET_SELECTED a: " + botonDebeSerVisibleConfig +
                                                   " (basado en config '" + toolbarButtonKey + ".visible')");
                            } else {
                                System.err.println("      -> ERROR: ToolbarButtonKey (ActionCommand del checkbox) es null o vacío para Checkbox '" + claveCompletaMenu + "'. Checkbox se dejará desmarcado.");
                                ((JCheckBoxMenuItem) menuItem).setSelected(false);
                                // menuItem.setEnabled(false); // Podrías deshabilitarlo si no tiene objetivo
                            }
                        } else {
                            // SUB-CASO A2: Es otro JCheckBoxMenuItem o JRadioButtonMenuItem sin Action de nuestro actionMap
                            //              (si existieran y quisieras persistir su estado .seleccionado con la clave larga).
                            //              Por ahora, la mayoría de tus otros checkboxes/radios SÍ tienen Actions.
                            boolean seleccionadoConfig = configuration.getBoolean(claveCompletaMenu + ".seleccionado", false);
                            if (menuItem instanceof JCheckBoxMenuItem) {
                                ((JCheckBoxMenuItem) menuItem).setSelected(seleccionadoConfig);
                            } else if (menuItem instanceof JRadioButtonMenuItem) {
                                ((JRadioButtonMenuItem) menuItem).setSelected(seleccionadoConfig);
                            }
                             System.out.println("    -> Otro Checkbox/Radio sin Action de ActionMap. Clave: '" + claveCompletaMenu + "'. SET_SELECTED a: " + seleccionadoConfig);
                        }
                    } else if (esSeleccionable && tieneActionDeNuestroActionMap) {
                        // CASO B: Es un JCheckBoxMenuItem o JRadioButtonMenuItem que SÍ tiene una Action de nuestro actionMap.
                        //         (Ej. ToggleUIElementVisibilityAction, ToggleThemeAction, SetSubfolderReadModeAction, ToggleProporcionesAction, etc.)
                        //         Estas Actions YA leyeron la configuración y establecieron su Action.SELECTED_KEY
                        //         en sus propios constructores. El JMenuItem vinculado a ellas ya debería estar
                        //         visualmente correcto (marcado/desmarcado). No necesitamos hacer menuItem.setSelected() aquí.
                        System.out.println("    -> Menú Seleccionable CON Action de ActionMap: '" + claveCompletaMenu +
                                           "'. Su estado 'selected' es manejado por su Action. Valor actual de Action.SELECTED_KEY: " +
                                           assignedAction.getValue(Action.SELECTED_KEY));
                    }
                    // Fin de la lógica para .selected

                // 3.5. Capturar cualquier excepción durante la lectura/aplicación de config para este menuItem.
                } catch (Exception ex) {
                    System.err.println("ERROR aplicando config a Menú (Vista) '" + claveCompletaMenu + "': " + ex.getMessage());
                    ex.printStackTrace(); // Para más detalle
                }
            }); // Fin del menuItems.forEach

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
        	Action toggleZoomManualAction = (this.actionMap != null) ? this.actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE) : null;
            boolean zoomManualInicial = (toggleZoomManualAction != null) && Boolean.TRUE.equals(toggleZoomManualAction.getValue(Action.SELECTED_KEY));
            if (view != null) { // Chequeo de view
                 view.actualizarEstadoControlesZoom(zoomManualInicial, zoomManualInicial);
            } else { System.err.println("WARN [aplicarConfigAlaVistaInternal]: Vista nula al actualizar controles zoom.");}


            // 4.1.2. Sincronizar estado visual del botón de Mantener Proporciones.
        	Action toggleProporcionesAction = (this.actionMap != null) ? this.actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES) : null;            
            boolean proporcionesInicial = (toggleProporcionesAction != null) && Boolean.TRUE.equals(toggleProporcionesAction.getValue(Action.SELECTED_KEY));
            this.view.actualizarAspectoBotonToggle(toggleProporcionesAction, proporcionesInicial); // Este método ya chequea si action es null

            // 4.1.3. Sincronizar estado visual del botón y radios de Cargar Subcarpetas.
            Action toggleSubfoldersAction = (this.actionMap != null) ? this.actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS) : null;
            boolean subcarpetasInicial = (toggleSubfoldersAction != null) && Boolean.TRUE.equals(toggleSubfoldersAction.getValue(Action.SELECTED_KEY));
            this.view.actualizarAspectoBotonToggle(toggleSubfoldersAction, subcarpetasInicial);
            // ¡MOVER LA LLAMADA AQUÍ DENTRO DEL TRY!
            sincronizarRadiosSubcarpetasVisualmente(subcarpetasInicial); // Asegurar que los radios del menú se marquen correctamente

            // 4.1.4. (Comentario sin cambios)

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
 
	
	// En controlador.VisorController.java

    /**
     * Configura los listeners principales de la vista (Selección de listas,
     * rueda de ratón para zoom/pan, scroll de miniaturas) y otros eventos UI.
     * Se llama desde AppInitializer (en el EDT) después de crear la Vista y el ListCoordinator.
     * Asigna los listeners adecuados a los componentes correspondientes de la VisorView.
     */
    /*package-private*/ void configurarListenersVistaInternal() {
    
        // --- SECCIÓN 0: VALIDACIÓN INICIAL Y LOG ---
        // 0.1. Validar que las dependencias críticas (Vista y ListCoordinator) existan.
        if (view == null || listCoordinator == null) {
            System.err.println("WARN [VisorController.configurarListenersVistaInternal]: Vista o ListCoordinator nulos. Abortando configuración de listeners.");
            return;
        }
        // 0.2. Validar que ZoomManager haya sido inyectado (necesario para los listeners de zoom/pan).
        if (this.zoomManager == null) {
            System.err.println("ERROR CRÍTICO [VisorController.configurarListenersVistaInternal]: ZoomManager es null. Los listeners de zoom/pan no funcionarán correctamente si se activa el zoom manual.");
        }
        // 0.3. Validar que el Modelo exista, ya que los listeners lo consultarán.
        if (this.model == null) {
            System.err.println("ERROR CRÍTICO [VisorController.configurarListenersVistaInternal]: VisorModel es null. Los listeners no podrán operar correctamente.");
            return;
        }
        
        // 0.4. Log indicando el inicio de la configuración.
        System.out.println("[Controller Internal] Configurando Listeners de Vista...");

        // --- SECCIÓN 1: LISTENERS DE SELECCIÓN DE LISTAS (listaNombres y listaMiniaturas) ---
        //     Estos listeners delegan la lógica de selección al ListCoordinator.
        // 1.1. Configurar Listener para la JList de Nombres de Archivo (listaNombres).
        JList<String> listaNombres = view.getListaNombres();
        if (listaNombres != null) {
            // 1.1.1. Limpiar listeners de selección previos para evitar duplicados (práctica defensiva).
            for (javax.swing.event.ListSelectionListener lsl : listaNombres.getListSelectionListeners()) {
                // Identificar y remover listeners añadidos previamente por esta clase o lambdas.
                if (lsl.getClass().getName().contains("$Lambda") || lsl.getClass().getName().contains(this.getClass().getSimpleName())) {
                    listaNombres.removeListSelectionListener(lsl);
                }
            }
            // 1.1.2. Añadir el nuevo ListSelectionListener para manejar cambios de selección.
            listaNombres.addListSelectionListener(e -> { // Inicio lambda
                // Ignorar eventos intermedios (valueIsAdjusting), o si la selección inicial o sincronización de UI están en curso.
                boolean isIgnored = e.getValueIsAdjusting() || 
                                    seleccionInicialEnCurso || 
                                    (listCoordinator != null && listCoordinator.isSincronizandoUI());
                
                int indicePrincipal = listaNombres.getSelectedIndex(); // Índice seleccionado en la lista de nombres.
                
                // System.out.println(">>> LISTENER NOMBRES: Evento. Seleccionado: " + indicePrincipal + ". Ignorado=" + isIgnored); // Log detallado
                
                if (!isIgnored) { // Solo procesar si no se debe ignorar
                    // System.out.println(">>> LISTENER NOMBRES: Procesando -> Llamando a ListCoordinator.seleccionarDesdeNombres(" + indicePrincipal + ")");
                    try {
                        if (listCoordinator != null) {
                             listCoordinator.seleccionarDesdeNombres(indicePrincipal);
                        } else {
                            System.err.println("ERROR CRÍTICO [Listener Nombres]: ListCoordinator es null. No se puede procesar selección.");
                        }
                    } catch (Exception ex) { // Capturar cualquier error inesperado durante el procesamiento.
                         System.err.println("### EXCEPCIÓN INESPERADA EN LISTENER NOMBRES (Índice: " + indicePrincipal + ") ###");
                         ex.printStackTrace();
                    }
                }
            }); // Fin lambda
            System.out.println("  -> Listener de Selección (ListSelectionListener) añadido a listaNombres.");

            // 1.1.3. Configurar Listener de Rueda del Ratón para Navegación en listaNombres.
            //          Permite navegar entre ítems usando la rueda del ratón.
			JList<String> listaNombresParaRueda = view.getListaNombres(); // Re-obtener por si acaso, o usar la misma referencia.
			if (listaNombresParaRueda != null) {
				// Limpiar MouseWheelListeners previos para evitar duplicados.
				for (java.awt.event.MouseWheelListener mwl : listaNombresParaRueda.getMouseWheelListeners()) {
					listaNombresParaRueda.removeMouseWheelListener(mwl);
				}	
				// System.out.println("  -> MouseWheelListeners previos en listaNombres eliminados (si existían).");
		
				listaNombresParaRueda.addMouseWheelListener(e -> { // Inicio lambda
					if (listCoordinator == null) {
						System.err.println("WARN [MouseWheel Nombres]: ListCoordinator es null. No se puede navegar con rueda.");
						return;
					}
					int notches = e.getWheelRotation(); // Dirección de la rueda.
					if (notches < 0) { // Rueda hacia ARRIBA
						// System.out.println("    -> Rueda ARRIBA sobre Nombres: Llamando a listCoordinator.seleccionarAnterior()");
						listCoordinator.seleccionarAnterior();
					} else if (notches > 0) { // Rueda hacia ABAJO
						// System.out.println("    -> Rueda ABAJO sobre Nombres: Llamando a listCoordinator.seleccionarSiguiente()");
						listCoordinator.seleccionarSiguiente();
					}
					e.consume(); // Consumir el evento para PREVENIR el scroll por defecto del JScrollPane.
				}); // Fin lambda
				System.out.println("  -> Listener de Rueda (MouseWheelListener) para NAVEGACIÓN ÍTEM A ÍTEM añadido a listaNombres.");
			} else { // Debería ser el mismo caso que listaNombres == null
				// System.err.println("WARN [VisorController.configurarListenersVistaInternal]: listaNombres es null. No se pudo añadir listener de navegación por rueda.");
			}

        } else { // Si listaNombres es null
            System.err.println("WARN [VisorController.configurarListenersVistaInternal]: view.getListaNombres() devolvió null. No se añadieron listeners.");
        }

		// 1.2. Configurar Listener para la JList de Miniaturas (listaMiniaturas).
	    JList<String> listaMiniaturas = view.getListaMiniaturas();
	    if (listaMiniaturas != null) {
	        // 1.2.1. Limpiar listeners de selección previos.
	        for (javax.swing.event.ListSelectionListener lsl : listaMiniaturas.getListSelectionListeners()) {
	            if (lsl.getClass().getName().contains("$Lambda") || lsl.getClass().getName().contains(this.getClass().getSimpleName())) {
	                listaMiniaturas.removeListSelectionListener(lsl);
	            }
	        }
	        // 1.2.2. Añadir el nuevo ListSelectionListener.
	        listaMiniaturas.addListSelectionListener(e -> { // Inicio lambda
	             boolean isIgnored = e.getValueIsAdjusting() || 
	                                 seleccionInicialEnCurso || 
	                                 (listCoordinator != null && listCoordinator.isSincronizandoUI());
	             
	             int indiceRelativoMiniatura = listaMiniaturas.getSelectedIndex(); // Índice en el modelo de miniaturas (que es un subconjunto).
	             // System.out.println(">>> LISTENER MINIATURAS: Evento. Seleccionado (relativo): " + indiceRelativoMiniatura + ". Ignorado=" + isIgnored); // Log detallado
	             
	             if (!isIgnored) { // Solo procesar si no se debe ignorar
	                // System.out.println(">>> LISTENER MINIATURAS: Procesando...");
	                int indicePrincipalTraducido = -1; // El índice correspondiente en el modelo principal.
	                try {
	                    if (indiceRelativoMiniatura != -1) { // Si hay una selección válida en la lista de miniaturas.
	                        ListModel<String> modeloMiniaturasActual = listaMiniaturas.getModel(); // Obtener el modelo actual de la JList
	                         // Validar que el modelo y el índice sean consistentes.
	                         if (modeloMiniaturasActual != null && indiceRelativoMiniatura < modeloMiniaturasActual.getSize()) {
	                             String claveSeleccionadaEnMiniatura = modeloMiniaturasActual.getElementAt(indiceRelativoMiniatura);
	                             // Traducir la clave de la miniatura a un índice en el modelo principal.
	                             if (claveSeleccionadaEnMiniatura != null && model != null && model.getModeloLista() != null) {
	                                 indicePrincipalTraducido = model.getModeloLista().indexOf(claveSeleccionadaEnMiniatura);
	                                 if (indicePrincipalTraducido == -1) { // Error si la clave no se encuentra en el modelo principal.
	                                     System.err.println("ERROR CRÍTICO [Listener Miniaturas]: Clave '" + claveSeleccionadaEnMiniatura + 
	                                                        "' de miniatura no encontrada en modelo principal! Modelo Principal Tamaño: " + model.getModeloLista().getSize());
	                                     if(listCoordinator != null) listCoordinator.seleccionarDesdeMiniaturas(-1); // Intentar limpiar selección.
	                                     return; 
	                                 }
	                             } else { // Error si las dependencias para la traducción no están listas.
	                                 System.err.println("ERROR CRÍTICO [Listener Miniaturas]: Clave nula o modelos nulos durante traducción de índice.");
	                                 if(listCoordinator != null) listCoordinator.seleccionarDesdeMiniaturas(-1);
	                                 return;
	                             }
	                         } else { // Error si el modelo de miniaturas es inconsistente con el índice.
	                             System.err.println("WARN [Listener Miniaturas]: No se pudo obtener clave del modelo de miniaturas o índice relativo (" + 
	                                                indiceRelativoMiniatura + ") fuera de rango (Tamaño: " + 
	                                                (modeloMiniaturasActual != null ? modeloMiniaturasActual.getSize() : "null") + ").");
	                             if(listCoordinator != null) listCoordinator.seleccionarDesdeMiniaturas(-1);
	                             return;
	                         }
	                    } else { // Si no hay selección en la lista de miniaturas (índice relativo es -1).
	                         indicePrincipalTraducido = -1; // Indicar deselección al coordinador.
	                    }
	                    
	                    // System.out.println(">>> LISTENER MINIATURAS: Procesando -> Llamando a ListCoordinator.seleccionarDesdeMiniaturas(" + indicePrincipalTraducido + ")");
	                     if (listCoordinator != null) {
	                          listCoordinator.seleccionarDesdeMiniaturas(indicePrincipalTraducido);
	                     } else {
	                         System.err.println("ERROR CRÍTICO [Listener Miniaturas]: ListCoordinator es null. No se puede procesar selección.");
	                     }
	                } catch (Exception ex) { // Capturar cualquier error inesperado.
	                     System.err.println("### EXCEPCIÓN INESPERADA EN LISTENER MINIATURAS (Índice Relativo: " + indiceRelativoMiniatura + ") ###");
	                     ex.printStackTrace();
	                     if(listCoordinator != null) listCoordinator.seleccionarDesdeMiniaturas(-1); // Intentar limpiar.
	                }
	             }
	        }); // Fin lambda
	        System.out.println("  -> Listener de Selección (ListSelectionListener) añadido a listaMiniaturas.");
	    } else { // Si listaMiniaturas es null
	         System.err.println("WARN [VisorController.configurarListenersVistaInternal]: view.getListaMiniaturas() devolvió null. No se añadieron listeners.");
	    }

	    // --- SECCIÓN 2: LISTENERS DE RATÓN PARA IMAGEN PRINCIPAL (etiquetaImagen para ZOOM/PAN) ---
	    // 2.1. Obtener la referencia al JLabel que muestra la imagen principal desde la Vista.
	    JLabel etiquetaImagenPrincipal = view.getEtiquetaImagen();

	    // 2.2. Comprobar si la etiqueta existe antes de añadir listeners.
	    if (etiquetaImagenPrincipal != null) {
	    
	        // 2.2.1. Listener para la Rueda del Ratón (MouseWheelListener) en etiquetaImagenPrincipal.
	        //          Se usa para el zoom y, con Shift, para paneo vertical rápido.
	    	etiquetaImagenPrincipal.addMouseWheelListener(e -> { // Inicio lambda
	    	    // --- INICIO MODIFICACIÓN: No procesar si no hay imagen válida ---
	    	    if (VisorController.this.model == null || VisorController.this.model.getCurrentImage() == null) {
	    	        // System.out.println("  [MouseWheel Listener en EtiquetaImagen] No hay imagen válida cargada en el modelo o modelo es nulo. Ignorando evento de rueda.");
	    	        e.consume(); // Consumir para evitar que el evento se propague (ej. a un JScrollPane padre)
	    	        return;
	    	    }
	    	    // --- FIN MODIFICACIÓN ---

	    	    if (VisorController.this.zoomManager == null) { // Validar que ZoomManager esté disponible.
	    	        System.err.println("CRITICAL [MouseWheelListener en EtiquetaImagen]: ZoomManager es nulo. No se puede procesar zoom/pan.");
	    	        return;
	    	    }
	    	    
	    	    if (e.isShiftDown()) { 
	    	        // --- LÓGICA DE PANEO VERTICAL RÁPIDO con Shift ---
	    	        // (Tu lógica existente para paneo con Shift aquí)
	    	        // ...
                    int scrollAmount = e.getWheelRotation(); 
                    int panStep = VisorController.this.view.getEtiquetaImagen().getHeight() / 4; 
                    int deltaY = -scrollAmount * panStep; 
                    
                    boolean puedePanear = false;
                    if (VisorController.this.model.isZoomHabilitado()) {
                        puedePanear = true;
                    } else {
                        Image imagenBase = VisorController.this.view.getImagenReescaladaView(); 
                        if (imagenBase != null) {
                            int imagenPintadaAlto = (int)(imagenBase.getHeight(null) * VisorController.this.model.getZoomFactor());
                            if (imagenPintadaAlto > VisorController.this.view.getEtiquetaImagen().getHeight()) {
                                puedePanear = true;
                            }
                        }
                    }
                    if (puedePanear) {
                        VisorController.this.zoomManager.aplicarPan(0, deltaY); 
                        VisorController.this.zoomManager.refrescarVistaPrincipalConEstadoActualDelModelo();
                    }
	    	        // ...
	    	    } else if (VisorController.this.model.isZoomHabilitado()) { // Solo hacer zoom si el zoom manual está activo
	    	        // --- LÓGICA DE ZOOM NORMAL (sin Shift y zoom manual activo) ---
	    	        // (Tu lógica existente para zoom normal aquí)
	    	        // ...
                    int notches = e.getWheelRotation();
                    double currentZoomFactor = VisorController.this.model.getZoomFactor();
                    double zoomIncrement = 0.1; 
                    double newZoomFactor = currentZoomFactor + (notches < 0 ? zoomIncrement : -zoomIncrement);
                    newZoomFactor = Math.max(0.01, Math.min(newZoomFactor, 20.0)); 
                    if (Math.abs(newZoomFactor - currentZoomFactor) > 0.001){
                        boolean cambioHechoEnModelo = VisorController.this.zoomManager.establecerFactorZoom(newZoomFactor);
                        if (cambioHechoEnModelo) {
                            if (VisorController.this.model.getCurrentZoomMode() == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM) {
                                double porcentajeParaGuardar = newZoomFactor * 100.0;
                                if (VisorController.this.configuration != null) {
                                    VisorController.this.configuration.setZoomPersonalizadoPorcentaje(porcentajeParaGuardar);
                                    // System.out.println("  [VisorController MouseWheel] Zoom Manual + Modo 'MAINTAIN_CURRENT_ZOOM': Config actualizada...");
                                }
                            }
                            VisorController.this.zoomManager.refrescarVistaPrincipalConEstadoActualDelModelo();
                        }
                    }
	    	        // ...
	    	    }
	    	    // Si no es ShiftDown y el zoom manual no está habilitado, no se hace nada aquí.
	    	}); // Fin lambda
	    	
	        // 2.2.2. Listener para Eventos Básicos del Ratón (MouseListener - mousePressed para Paneo) en etiquetaImagenPrincipal.
	        etiquetaImagenPrincipal.addMouseListener(new MouseAdapter() { // Inicio de la clase anónima MouseAdapter
	            @Override 
	            public void mousePressed(java.awt.event.MouseEvent e) {
	                // --- INICIO MODIFICACIÓN: No procesar si no hay imagen válida ---
	                if (VisorController.this.model == null || VisorController.this.model.getCurrentImage() == null) {
	                    // System.out.println("  [MousePressed Listener en EtiquetaImagen] No hay imagen válida en modelo. Ignorando.");
	                    return;
	                }
	                // --- FIN MODIFICACIÓN ---

	                // Solo iniciar el paneo si el zoom manual está habilitado Y se presiona el botón izquierdo.
	                if (VisorController.this.model.isZoomHabilitado() && 
	                    SwingUtilities.isLeftMouseButton(e)) {
	                    VisorController.this.lastMouseX = e.getX(); // Guardar coords para el arrastre.
	                    VisorController.this.lastMouseY = e.getY();
	                    // System.out.println("  [MousePressed EtiquetaImagen] Paneo iniciado en: (" + VisorController.this.lastMouseX + ", " + VisorController.this.lastMouseY + ")");
	                }
	            } 
	        }); // Fin MouseAdapter
	        
	        // 2.2.3. Listener para Eventos de Movimiento del Ratón (MouseMotionListener - mouseDragged para Paneo) en etiquetaImagenPrincipal.
	        etiquetaImagenPrincipal.addMouseMotionListener(new MouseMotionAdapter() { // Inicio MouseMotionAdapter
	            @Override 
	            public void mouseDragged(java.awt.event.MouseEvent e) {
	                // --- INICIO MODIFICACIÓN: No procesar si no hay imagen válida ---
	                if (VisorController.this.model == null || VisorController.this.model.getCurrentImage() == null) {
	                    // System.out.println("  [MouseDragged Listener en EtiquetaImagen] No hay imagen válida en modelo. Ignorando.");
	                    return;
	                }
	                // --- FIN MODIFICACIÓN ---
	                
	                // Solo procesar arrastre para paneo si el zoom manual está habilitado, ZoomManager existe, y es botón izquierdo.
	                if (VisorController.this.model.isZoomHabilitado() && 
	                    VisorController.this.zoomManager != null && 
	                    SwingUtilities.isLeftMouseButton(e)) {
	                    
	                    int deltaX = e.getX() - VisorController.this.lastMouseX;
	                    int deltaY = e.getY() - VisorController.this.lastMouseY;
	                    
	                    VisorController.this.zoomManager.aplicarPan(deltaX, deltaY); // Aplica el paneo al modelo.
	                    
	                    VisorController.this.lastMouseX = e.getX(); // Actualiza las últimas coords.
	                    VisorController.this.lastMouseY = e.getY();
	                    
	                    VisorController.this.zoomManager.refrescarVistaPrincipalConEstadoActualDelModelo(); // Repinta la imagen.
	                    
	                } else if (VisorController.this.zoomManager == null && VisorController.this.model != null && VisorController.this.model.isZoomHabilitado()) {
	                    System.err.println("CRITICAL [MouseMotionListener.mouseDragged VisorController]: ZoomManager es null. No se puede procesar el paneo.");
	                }
	            } 
	        }); // Fin MouseMotionAdapter
	        
	        System.out.println("  -> Listeners de Zoom/Pan (Rueda, Pressed, Dragged) añadidos a etiquetaImagenPrincipal.");
	    } else { // Si etiquetaImagenPrincipal es null
	        System.err.println("WARN [VisorController.configurarListenersVistaInternal]: view.getEtiquetaImagen() devolvió null. No se añadieron listeners de zoom/pan.");
	    }

	    // --- SECCIÓN 3: LISTENER DE RUEDA PARA NAVEGACIÓN EN MINIATURAS (JScrollPane de Miniaturas) ---
	    JScrollPane scrollMiniaturas = view.getScrollListaMiniaturas();
	    if (scrollMiniaturas != null) {
	        // Limpiar MouseWheelListeners previos para evitar duplicados.
	        for (java.awt.event.MouseWheelListener mwl : scrollMiniaturas.getMouseWheelListeners()) {
	            scrollMiniaturas.removeMouseWheelListener(mwl);
	        }
	        // System.out.println("  -> MouseWheelListeners previos en scrollListaMiniaturas eliminados (si existían).");

	        // Añadir el nuevo MouseWheelListener para NAVEGACIÓN entre ítems.
	        scrollMiniaturas.addMouseWheelListener(e -> { // Inicio lambda
	            if (listCoordinator == null) {
	                System.err.println("WARN [MouseWheel Miniaturas]: ListCoordinator es null. No se puede navegar con rueda.");
	                return;
	            }
	            int notches = e.getWheelRotation();
	            if (notches < 0) { // Rueda hacia ARRIBA
	                // System.out.println("    -> Rueda ARRIBA sobre Miniaturas: Llamando a listCoordinator.seleccionarAnterior()");
	                listCoordinator.seleccionarAnterior();
	            } else if (notches > 0) { // Rueda hacia ABAJO
	                // System.out.println("    -> Rueda ABAJO sobre Miniaturas: Llamando a listCoordinator.seleccionarSiguiente()");
	                listCoordinator.seleccionarSiguiente();
	            }
	            e.consume(); // Evitar que el JScrollPane intente su scroll por defecto.
	        }); // Fin lambda
	        System.out.println("  -> Listener de Rueda (MouseWheelListener) para NAVEGACIÓN ÍTEM A ÍTEM añadido a scrollListaMiniaturas.");
	    } else { // Si scrollListaMiniaturas es null
	        System.err.println("WARN [VisorController.configurarListenersVistaInternal]: view.getScrollListaMiniaturas() es null. No se pudo añadir listener de navegación por rueda.");
	    }
	    
	    // --- SECCIÓN 4: GESTIONAR FOCO EN MINIATURAS AL HACER CLIC EN EL SCROLLPANE ---
	    //    Esto ayuda a que los bindings de teclado de la lista de miniaturas funcionen si se hace clic en el área vacía del scrollpane.
	    JScrollPane scrollMiniaturasParaFoco = view.getScrollListaMiniaturas(); 
	    JList<String> listaMiniaturasParaFoco = view.getListaMiniaturas(); 
	    if (scrollMiniaturasParaFoco != null && listaMiniaturasParaFoco != null) {
	        // Limpiar listeners de ratón previos (defensivo).
	        for (java.awt.event.MouseListener ml : scrollMiniaturasParaFoco.getMouseListeners()) {
	            if (ml.getClass().isAnonymousClass() || ml.getClass().getName().contains("MouseAdapter")) {
	                 scrollMiniaturasParaFoco.removeMouseListener(ml);
	            }
	        }
	        // Añadir el nuevo MouseAdapter.
	        final JList<String> finalListaMinParaFoco = listaMiniaturasParaFoco; // Necesaria para la clase anónima.
	        scrollMiniaturasParaFoco.addMouseListener(new MouseAdapter() {
	            @Override
	            public void mousePressed(java.awt.event.MouseEvent e) {
	                // Si la lista de miniaturas es usable, pedir el foco para ella.
	                if (finalListaMinParaFoco.isEnabled() && finalListaMinParaFoco.isFocusable()) {
	                    // System.out.println("-> Clic en ScrollMiniaturas detectado. Solicitando foco para listaMiniaturas...");
	                    finalListaMinParaFoco.requestFocusInWindow();
	                }
	            }
	        });
	        System.out.println("  -> Listener de Ratón (MouseListener) añadido a scrollListaMiniaturas para gestionar foco.");
	    } else { // Si alguno de los componentes para el foco es null
	         System.err.println("WARN [VisorController.configurarListenersVistaInternal]: No se pudo añadir listener de foco a scrollListaMiniaturas (scroll o lista nulos).");
	    }
	    
	    // --- SECCIÓN 5: LISTENERS PARA ICONOS/CONTROLES DE LA BARRA INFERIOR ---
	    //     Esto permite que los JLabels que actúan como botones en la barra inferior respondan a clics.
	    System.out.println("  -> Configurando Listeners para Iconos/Controles de la Barra Inferior...");

	    // 5.1 Listener para Icono/Label de Zoom Manual en la barra inferior.
	    final JLabel zmIconLabel = view.getIconoZoomManualLabel(); 
	    final Action zoomManualAction = (actionMap != null) ? actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE) : null;
	    if (zmIconLabel != null && zoomManualAction != null) {
	        zmIconLabel.addMouseListener(new MouseAdapter() {
	            @Override
	            public void mouseClicked(MouseEvent e) {
	                System.out.println("  [VisorController Listener BarraInf] Clic en icono/label Zoom Manual.");
	                // NO es necesario invertir SELECTED_KEY aquí; la Action lo hará.
	                // Simplemente disparamos la acción.
	                zoomManualAction.actionPerformed(new ActionEvent(zmIconLabel, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE));
	            }
	            @Override public void mouseEntered(MouseEvent e) { zmIconLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
	            @Override public void mouseExited(MouseEvent e) { zmIconLabel.setCursor(Cursor.getDefaultCursor()); }
	        });
	        System.out.println("    -> MouseListener añadido a iconoZoomManualLabel (Barra Inferior).");
	    } else {
	        if (zmIconLabel == null) System.err.println("WARN [VC Listeners BarraInf]: iconoZoomManualLabel es null.");
	        if (zoomManualAction == null) System.err.println("WARN [VC Listeners BarraInf]: Action CMD_ZOOM_MANUAL_TOGGLE no encontrada en actionMap.");
	    }

	    // 5.2 Listener para Icono/Label de Mantener Proporciones en la barra inferior.
	    final JLabel propIconLabel = view.getIconoMantenerProporcionesLabel();
	    final Action proporcionesAction = (actionMap != null) ? actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES) : null;
	    if (propIconLabel != null && proporcionesAction != null) {
	        propIconLabel.addMouseListener(new MouseAdapter() {
	            @Override
	            public void mouseClicked(MouseEvent e) {
	                System.out.println("  [VisorController Listener BarraInf] Clic en icono/label Mantener Proporciones.");
	                proporcionesAction.actionPerformed(new ActionEvent(propIconLabel, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES));
	            }
	            @Override public void mouseEntered(MouseEvent e) { propIconLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
	            @Override public void mouseExited(MouseEvent e) { propIconLabel.setCursor(Cursor.getDefaultCursor()); }
	        });
	        System.out.println("    -> MouseListener añadido a iconoMantenerProporcionesLabel (Barra Inferior).");
	    } else {
	        if (propIconLabel == null) System.err.println("WARN [VC Listeners BarraInf]: iconoMantenerProporcionesLabel es null.");
	        if (proporcionesAction == null) System.err.println("WARN [VC Listeners BarraInf]: Action CMD_TOGGLE_MANTENER_PROPORCIONES no encontrada.");
	    }

	    // 5.3 Listener para Icono/Label de Modo Subcarpetas en la barra inferior.
	    final JLabel subcIconLabel = view.getIconoModoSubcarpetasLabel();
	    final Action subcarpetasAction = (actionMap != null) ? actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS) : null;
	    if (subcIconLabel != null && subcarpetasAction != null) {
	        subcIconLabel.addMouseListener(new MouseAdapter() {
	            @Override
	            public void mouseClicked(MouseEvent e) {
	                System.out.println("  [VisorController Listener BarraInf] Clic en icono/label Modo Subcarpetas.");
	                subcarpetasAction.actionPerformed(new ActionEvent(subcIconLabel, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_TOGGLE_SUBCARPETAS));
	            }
	            @Override public void mouseEntered(MouseEvent e) { subcIconLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
	            @Override public void mouseExited(MouseEvent e) { subcIconLabel.setCursor(Cursor.getDefaultCursor()); }
	        });
	        System.out.println("    -> MouseListener añadido a iconoModoSubcarpetasLabel (Barra Inferior).");
	    } else {
	        if (subcIconLabel == null) System.err.println("WARN [VC Listeners BarraInf]: iconoModoSubcarpetasLabel es null.");
	        if (subcarpetasAction == null) System.err.println("WARN [VC Listeners BarraInf]: Action CMD_TOGGLE_SUBCARPETAS no encontrada.");
	    }
	    
	    // --- SECCIÓN 6: LOG FINAL ---
	    System.out.println("[Controller Internal] Configuración de Listeners de Vista finalizada.");

} // --- FIN configurarListenersVistaInternal ---
	
    
//	/**
//	 * Configura los listeners principales de la vista (Selección de listas,
//	 * rueda de ratón para zoom/pan, scroll de miniaturas) y otros eventos UI.
//	 * Se llama desde AppInitializer (en el EDT) después de crear la Vista y el ListCoordinator.
//	 * Asigna los listeners adecuados a los componentes correspondientes de la VisorView.
//	 */
//	/*package-private*/ void configurarListenersVistaInternal() {
//    
//	    // --- SECCIÓN 0: VALIDACIÓN INICIAL Y LOG ---
//	    // 0.1. Validar que las dependencias críticas (Vista y ListCoordinator) existan.
//	    if (view == null || listCoordinator == null) {
//	        System.err.println("WARN [VisorController.configurarListenersVistaInternal]: Vista o ListCoordinator nulos. Abortando configuración de listeners.");
//	        return;
//	    }
//	    // 0.2. Validar que ZoomManager haya sido inyectado (necesario para los listeners de zoom/pan).
//	    if (this.zoomManager == null) {
//	        System.err.println("ERROR CRÍTICO [VisorController.configurarListenersVistaInternal]: ZoomManager es null. Los listeners de zoom/pan no funcionarán correctamente.");
//	        // Podrías decidir continuar sin estos listeners o tratarlo como un error más grave.
//	    }
//	    
//	    // 0.3. Log indicando el inicio de la configuración.
//	    System.out.println("[Controller Internal] Configurando Listeners de Vista...");
//	
//	    // --- SECCIÓN 1: LISTENERS DE SELECCIÓN DE LISTAS ---
//	    //    (Esta sección parece que ya la tienes bien con ListCoordinator, la mantengo como estaba en tu código)
//	    // 1.1. Configurar Listener para listaNombres.
//	    JList<String> listaNombres = view.getListaNombres();
//	    if (listaNombres != null) {
//	        // 1.1.1. Limpiar listeners de selección previos (defensivo).
//	        for (javax.swing.event.ListSelectionListener lsl : listaNombres.getListSelectionListeners()) {
//	            if (lsl.getClass().getName().contains("$Lambda") || lsl.getClass().getName().contains(this.getClass().getSimpleName())) {
//	                listaNombres.removeListSelectionListener(lsl);
//	            }
//	        }
//	        // 1.1.2. Añadir el nuevo ListSelectionListener.
//	        listaNombres.addListSelectionListener(e -> {
//	            boolean isIgnored = e.getValueIsAdjusting() || seleccionInicialEnCurso || (listCoordinator != null && listCoordinator.isSincronizandoUI());
//	            int indicePrincipal = listaNombres.getSelectedIndex();
//	            // System.out.println(">>> LISTENER NOMBRES: Evento. ... Ignorado=" + isIgnored); // Log reducido
//	            if (!isIgnored) {
//	                // System.out.println(">>> LISTENER NOMBRES: Procesando -> Coordinator.seleccionarDesdeNombres(" + indicePrincipal + ")");
//	                try {
//	                    if (listCoordinator != null) {
//	                         listCoordinator.seleccionarDesdeNombres(indicePrincipal);
//	                    } else {
//	                        System.err.println("ERROR CRÍTICO: ListCoordinator es null en listener Nombres");
//	                    }
//	                } catch (Exception ex) {
//	                     System.err.println("### EXCEPCIÓN LISTENER NOMBRES (Índice: " + indicePrincipal + ") ###");
//	                     ex.printStackTrace();
//	                }
//	            }
//	        });
//	        System.out.println("  -> Listener de Selección añadido a listaNombres.");
//	    } else {
//	        System.err.println("WARN [VisorController.configurarListenersVistaInternal]: listaNombres es null.");
//	    }
//	
//	    
//	    	// 1.1.3. --- LISTENER DE RUEDA EN LISTA DE NOMBRES ---
//			JList<String> listaNombresParaRueda = view.getListaNombres(); 
//		
//			if (listaNombresParaRueda != null) {
//				// Limpiar MouseWheelListeners previos de listaNombresParaRueda
//				for (java.awt.event.MouseWheelListener mwl : listaNombresParaRueda.getMouseWheelListeners()) {
//					listaNombresParaRueda.removeMouseWheelListener(mwl);
//				}	
//				System.out.println("  -> MouseWheelListeners previos en listaNombres eliminados (si existían).");
//		
//				listaNombresParaRueda.addMouseWheelListener(e -> {
//					if (listCoordinator == null) {
//						System.err.println("WARN [MouseWheel Nombres]: ListCoordinator es null. No se puede navegar.");
//						return;
//					}
//					int notches = e.getWheelRotation();
//					if (notches < 0) { // Rueda hacia ARRIBA
//						// System.out.println("    -> Rueda ARRIBA sobre Nombres: Llamando a listCoordinator.seleccionarAnterior()");
//						listCoordinator.seleccionarAnterior();
//					} else if (notches > 0) { // Rueda hacia ABAJO
//						// System.out.println("    -> Rueda ABAJO sobre Nombres: Llamando a listCoordinator.seleccionarSiguiente()");
//						listCoordinator.seleccionarSiguiente();
//					}
//		
//					// Consumir el evento para PREVENIR el scroll por defecto de JScrollPane,
//					// ya que la navegación debería hacer que el ensureIndexIsVisible se encargue del scroll.
//					e.consume();
//				});
//				System.out.println("  -> Nuevo MouseWheelListener para NAVEGACIÓN ÍTEM A ÍTEM añadido a listaNombres.");
//			} else {
//				System.err.println("WARN [VisorController.configurarListenersVistaInternal]: listaNombres es null. No se pudo añadir listener de navegación por rueda.");
//			}
//	
//		
//		// 1.2. Configurar Listener para listaMiniaturas.
//	    JList<String> listaMiniaturas = view.getListaMiniaturas();
//	    if (listaMiniaturas != null) {
//	        // 1.2.1. Limpiar listeners de selección previos.
//	        for (javax.swing.event.ListSelectionListener lsl : listaMiniaturas.getListSelectionListeners()) {
//	            if (lsl.getClass().getName().contains("$Lambda") || lsl.getClass().getName().contains(this.getClass().getSimpleName())) {
//	                listaMiniaturas.removeListSelectionListener(lsl);
//	            }
//	        }
//	        // 1.2.2. Añadir el nuevo ListSelectionListener.
//	        listaMiniaturas.addListSelectionListener(e -> {
//	             boolean isIgnored = e.getValueIsAdjusting() || seleccionInicialEnCurso || (listCoordinator != null && listCoordinator.isSincronizandoUI());
//	             int indiceRelativo = listaMiniaturas.getSelectedIndex();
//	             // System.out.println(">>> LISTENER MINIATURAS: Evento. ... Ignorado=" + isIgnored); // Log reducido
//	             if (!isIgnored) {
//	                // System.out.println(">>> LISTENER MINIATURAS: Procesando...");
//	                int indicePrincipalTraducido = -1;
//	                try {
//	                    if (indiceRelativo != -1) {
//	                        ListModel<String> modeloMinActual = listaMiniaturas.getModel();
//	                         if (modeloMinActual != null && indiceRelativo < modeloMinActual.getSize()) {
//	                             String claveSeleccionada = modeloMinActual.getElementAt(indiceRelativo);
//	                             if (claveSeleccionada != null && model != null && model.getModeloLista() != null) {
//	                                 indicePrincipalTraducido = model.getModeloLista().indexOf(claveSeleccionada);
//	                                 if (indicePrincipalTraducido == -1) {
//	                                     System.err.println("ERROR CRÍTICO: Clave '" + claveSeleccionada + "' de miniatura no encontrada en modelo principal!");
//	                                     if(listCoordinator != null) listCoordinator.seleccionarDesdeMiniaturas(-1); // Intenta limpiar
//	                                     return; 
//	                                 }
//	                             } else {
//	                                 System.err.println("ERROR CRÍTICO: Clave o modelos nulos durante traducción de índice en listener Miniaturas.");
//	                                 if(listCoordinator != null) listCoordinator.seleccionarDesdeMiniaturas(-1);
//	                                 return;
//	                             }
//	                         } else {
//	                             System.err.println("WARN: No se pudo obtener clave del modelo de miniaturas o índice relativo fuera de rango: " + indiceRelativo);
//	                             if(listCoordinator != null) listCoordinator.seleccionarDesdeMiniaturas(-1);
//	                             return;
//	                         }
//	                    } else {
//	                         indicePrincipalTraducido = -1; // Deselección
//	                    }
//	                    // System.out.println(">>> LISTENER MINIATURAS: Procesando -> Coordinator.seleccionarDesdeMiniaturas(" + indicePrincipalTraducido + ")");
//	                     if (listCoordinator != null) {
//	                          listCoordinator.seleccionarDesdeMiniaturas(indicePrincipalTraducido);
//	                     } else {
//	                         System.err.println("ERROR CRÍTICO: ListCoordinator es null en listener Miniaturas");
//	                     }
//	                } catch (Exception ex) {
//	                     System.err.println("### EXCEPCIÓN LISTENER MINIATURAS (Índice Relativo: " + indiceRelativo + ") ###");
//	                     ex.printStackTrace();
//	                     if(listCoordinator != null) listCoordinator.seleccionarDesdeMiniaturas(-1);
//	                }
//	             }
//	        });
//	        System.out.println("  -> Listener de Selección añadido a listaMiniaturas.");
//	    } else {
//	         System.err.println("WARN [VisorController.configurarListenersVistaInternal]: listaMiniaturas es null.");
//	    }
//	
//	    // --- SECCIÓN 2: LISTENERS DE RATÓN PARA IMAGEN PRINCIPAL (ZOOM/PAN) ---
//	    // 2.1. Obtener la referencia al JLabel que muestra la imagen principal.
//	    JLabel etiquetaImagenPrincipal = view.getEtiquetaImagen();
//	
//	    // 2.2. Comprobar si la etiqueta existe.
//	    if (etiquetaImagenPrincipal != null) {
//	    
//	        // 2.2.1. Listener para la Rueda del Ratón (Zoom).
//	    	etiquetaImagenPrincipal.addMouseWheelListener(e -> {
//	    		
//	    		if (VisorController.this.model == null || VisorController.this.model.getCurrentImage() == null) {
//	    	        // System.out.println("  [MouseWheel Listener] No hay imagen válida cargada en el modelo o modelo es nulo. Ignorando evento de rueda.");
//	    	        e.consume(); // Importante para evitar que el JScrollPane (si lo hubiera) procese el scroll
//	    	        return;
//	    	    }
//	    		
////	    	    if (this.model == null || this.zoomManager == null) { // Validar dependencias primero
////	    	        System.err.println("CRITICAL [MouseWheelListener]: Model o ZoomManager nulos.");
////	    	        return;
////	    	    }
//	    	    
//	    	    
//	    	    //FIXME ahora shif sube y baja pero sin shif acerca aleja... mejorar este comportamiento
//	    	    if (e.isShiftDown()) { 
//	    	        // --- LÓGICA DE PANEO VERTICAL RÁPIDO con Shift ---
//	    	        int scrollAmount = e.getWheelRotation(); 
//	    	        int panStep = this.view.getEtiquetaImagen().getHeight() / 4; // Asegúrate que view esté disponible
//	    	                                                                  // o pasa etiquetaImagenPrincipal directamente.
//	    	        int deltaY = -scrollAmount * panStep; 
//	    	        
//	    	        // Solo panear si la imagen realmente desborda y el zoom manual está activo,
//	    	        // o si el zoom manual está inactivo pero un modo de zoom causa desborde.
//	    	        boolean puedePanear = false;
//	    	        if (this.model.isZoomHabilitado()) {
//	    	            puedePanear = true;
//	    	        } else {
//	    	            // Comprobar si la imagen actual está desbordando verticalmente
//	    	            // Necesitamos la altura REAL de la imagen pintada
//	    	            Image imagenBase = this.view.getImagenReescaladaView(); // Necesitas un getter en VisorView para esto
//	    	            if (imagenBase != null) {
//	    	                int imagenPintadaAlto = (int)(imagenBase.getHeight(null) * this.model.getZoomFactor());
//	    	                if (imagenPintadaAlto > this.view.getEtiquetaImagen().getHeight()) {
//	    	                    puedePanear = true;
//	    	                }
//	    	            }
//	    	        }
//	
//	    	        if (puedePanear) {
//	    	            this.zoomManager.aplicarPan(0, deltaY); 
//	    	            this.zoomManager.refrescarVistaPrincipalConEstadoActualDelModelo();
//	    	        }
//	
//	    	    } else if (this.model.isZoomHabilitado()) { 
//	    	        // --- LÓGICA DE ZOOM NORMAL (sin Shift y zoom manual activo) ---
//	    	        // Esta es tu lógica de zoom original, ahora dentro del else.
//	    	        int notches = e.getWheelRotation();
//	    	        double currentZoomFactor = this.model.getZoomFactor();
//	    	        double zoomIncrement = 0.1; 
//	    	        double newZoomFactor = currentZoomFactor + (notches < 0 ? zoomIncrement : -zoomIncrement);
//	    	        newZoomFactor = Math.max(0.01, Math.min(newZoomFactor, 20.0)); 
//	    	        if (Math.abs(newZoomFactor - currentZoomFactor) > 0.001){
//	    	            boolean cambioHechoEnModelo = this.zoomManager.establecerFactorZoom(newZoomFactor);
//	    	            if (cambioHechoEnModelo) {
//	    	            	
//	    	            	// nueva logica para actualizar el popup
//	    	            	if (this.model.getCurrentZoomMode() == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM) {
//	    	                    // El 'newZoomFactor' es el que se acaba de establecer en el modelo.
//	    	                    double porcentajeParaGuardar = newZoomFactor * 100.0;
//	
//	    	                    if (this.configuration != null) { // this.configuration es el campo en VisorController
//	    	                        this.configuration.setZoomPersonalizadoPorcentaje(porcentajeParaGuardar); // Usa el método que ya formateará el String
//	    	                        System.out.println("  [VisorController MouseWheel] Zoom Manual + Modo 'MAINTAIN_CURRENT_ZOOM':");
//	    	                        System.out.println("    -> Config '" + ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_PERSONALIZADO_PORCENTAJE +
//	    	                                           "' actualizado a " + String.format("%.2f", porcentajeParaGuardar) + "%");
//	    	                    } else {
//	    	                        System.err.println("WARN [VisorController MouseWheel]: ConfigurationManager es null. No se pudo guardar el porcentaje personalizado.");
//	    	                    }
//	    	            	}
//	    	            	
//	    	                this.zoomManager.refrescarVistaPrincipalConEstadoActualDelModelo();
//	    	            }
//	    	        }
//	    	    }
//	    	    // Si no es ShiftDown y el zoom manual no está habilitado, no hace nada con la rueda.
//	    	});
//	    	
//	        // --- SECCIÓN 2.2.2: Listener para Eventos Básicos del Ratón (mousePressed para Paneo) ---
//	        //         Se añade un MouseAdapter para sobrescribir solo el método mousePressed.
//	        //         Este método se dispara cuando se presiona un botón del ratón sobre 'etiquetaImagenPrincipal'.
//	        etiquetaImagenPrincipal.addMouseListener(new MouseAdapter() { // Inicio de la clase anónima MouseAdapter
//	            
//	            /**
//	             * Se invoca cuando un botón del ratón ha sido presionado sobre el componente.
//	             * @param e El evento del ratón.
//	             */
//	            @Override 
//	            public void mousePressed(java.awt.event.MouseEvent e) {
//	            	
//	            	
//	                // 2.2.2.1. Acceder a los campos de la instancia de VisorController usando 'VisorController.this'.
//	                //          Esto es necesario porque estamos dentro de una clase interna anónima.
//	                //          Verificar que el modelo exista y que el zoom manual esté habilitado.
//	                //          Solo iniciar el paneo si se presiona el botón izquierdo del ratón.
////	                if (VisorController.this.model != null && 
////	                    VisorController.this.model.isZoomHabilitado() && 
////	                    SwingUtilities.isLeftMouseButton(e)) {
//	            	
//	            	if (VisorController.this.model == null || VisorController.this.model.getCurrentImage() == null) {
//	                    // System.out.println("  [MousePressed Listener] No hay imagen válida en modelo. Ignorando.");
//	                    return;
//	                }
//	                    
//	                    // 2.2.2.2. Guardar las coordenadas X e Y actuales del puntero del ratón.
//	                    //          Estas coordenadas se usarán como punto de referencia para calcular
//	                    //          el desplazamiento (delta) cuando el ratón se arrastre (en mouseDragged).
//	                    //          'lastMouseX' y 'lastMouseY' son campos de la clase VisorController.
//	                    VisorController.this.lastMouseX = e.getX();
//	                    VisorController.this.lastMouseY = e.getY();
//	                    
//	                    // (Opcional) Log para depuración:
//	                    // System.out.println("  [MousePressed] Paneo iniciado en: (" + VisorController.this.lastMouseX + ", " + VisorController.this.lastMouseY + ")");
//	                }
//	            } // Fin del método mousePressed
//	        }); // Fin de la clase anónima MouseAdapter y de la llamada a addMouseListener
//	        
//	        
//	        // --- SECCIÓN 2.2.3: Listener para Eventos de Movimiento del Ratón (mouseDragged para Paneo) ---
//	        //         Se añade un MouseMotionAdapter para sobrescribir solo el método mouseDragged.
//	        //         Este método se dispara cuando el ratón se mueve mientras un botón está presionado.
//	        etiquetaImagenPrincipal.addMouseMotionListener(new MouseMotionAdapter() { // Inicio de la clase anónima MouseMotionAdapter
//	            
//	            /**
//	             * Se invoca cuando el ratón es arrastrado (movido con un botón presionado) sobre el componente.
//	             * @param e El evento del ratón.
//	             */
//	            @Override 
//	            public void mouseDragged(java.awt.event.MouseEvent e) {
//	                // 2.2.3.1. Acceder a los campos de la instancia de VisorController usando 'VisorController.this'.
//	                //          Verificar que el modelo y ZoomManager existan, que el zoom manual esté habilitado,
//	                //          y que el arrastre se esté haciendo con el botón izquierdo del ratón.
//	                if (VisorController.this.model != null && 
//	                    VisorController.this.model.isZoomHabilitado() && 
//	                    VisorController.this.zoomManager != null && // Verificar que ZoomManager esté disponible
//	                    SwingUtilities.isLeftMouseButton(e)) {
//	                    
//	                    // 2.2.3.2. Calcular el desplazamiento (delta) en X e Y desde la última posición registrada.
//	                    //          'lastMouseX' y 'lastMouseY' son campos de VisorController que se actualizaron en mousePressed.
//	                    int deltaX = e.getX() - VisorController.this.lastMouseX;
//	                    int deltaY = e.getY() - VisorController.this.lastMouseY;
//	                    
//	                    // 2.2.3.3. Llamar al ZoomManager para que aplique este delta de paneo al modelo.
//	                    //          El método 'aplicarPan' en ZoomManager actualizará los offsets en VisorModel.
//	                    VisorController.this.zoomManager.aplicarPan(deltaX, deltaY);
//	                    
//	                    // 2.2.3.4. Actualizar las últimas coordenadas conocidas del ratón a la posición actual.
//	                    //          Esto es crucial para que el siguiente evento mouseDragged calcule el delta correctamente.
//	                    VisorController.this.lastMouseX = e.getX();
//	                    VisorController.this.lastMouseY = e.getY();
//	                    
//	                    // 2.2.3.5. Solicitar al ZoomManager que refresque la vista principal.
//	                    //          Esto hará que la imagen se redibuje con los nuevos offsets.
//	                    
//	                    //LOG VisorController DEBUG
//	//                    System.out.println("  [VisorController DEBUG] Estado del MODELO ANTES DE REFRESCAR ZOOM: model.isMantenerProporcion()=" + model.isMantenerProporcion());
//	                    VisorController.this.zoomManager.refrescarVistaPrincipalConEstadoActualDelModelo();
//	                    
//	                    // (Opcional) Log para depuración:
//	                    // System.out.println("  [MouseDragged] Paneo aplicado. Delta: (" + deltaX + ", " + deltaY + 
//	                    //                    "). Nuevos Offsets Modelo: (" + VisorController.this.model.getImageOffsetX() + 
//	                    //                    ", " + VisorController.this.model.getImageOffsetY() + ")");
//	
//	                } else if (VisorController.this.zoomManager == null && VisorController.this.model != null && VisorController.this.model.isZoomHabilitado()) {
//	                    // Log de error si ZoomManager no está inicializado pero se intentó panear.
//	                    System.err.println("CRITICAL [MouseMotionListener.mouseDragged en VisorController]: ZoomManager es null. No se puede procesar el paneo.");
//	                }
//	            } // Fin del método mouseDragged
//	        }); // Fin de la clase anónima MouseMotionAdapter y de la llamada a addMouseMotionListener
//	        
//	        // 2.2.4. Log confirmando adición de listeners de ratón para zoom/pan.
//	        System.out.println("  -> Listeners de Zoom/Pan (Rueda y Arrastre) añadidos a etiquetaImagenPrincipal.");
//	
//	    // 2.3. Log de advertencia si la etiqueta de imagen no existe.
//	    } else {
//	        System.err.println("WARN [VisorController.configurarListenersVistaInternal]: etiquetaImagenPrincipal es null. No se añadieron listeners de zoom/pan.");
//	    }
//	
//	
//	 // --- SECCIÓN 3: LISTENER DE RUEDA PARA NAVEGACIÓN EN MINIATURAS ---
//	    JScrollPane scrollMiniaturas = view.getScrollListaMiniaturas();
//	    if (scrollMiniaturas != null) {
//	        // 3.1. Limpiar TODOS los MouseWheelListeners previos para evitar duplicados o conflictos.
//	        //      Es más seguro quitar todos y luego añadir el nuestro.
//	        for (java.awt.event.MouseWheelListener mwl : scrollMiniaturas.getMouseWheelListeners()) {
//	            scrollMiniaturas.removeMouseWheelListener(mwl);
//	        }
//	        System.out.println("  -> MouseWheelListeners previos en scrollListaMiniaturas eliminados (si existían).");
//	
//	        // 3.2. Añadir el nuevo MouseWheelListener para NAVEGACIÓN.
//	        scrollMiniaturas.addMouseWheelListener(e -> { // Inicio de la lambda para el listener
//	            // 3.2.1. Validar que ListCoordinator esté disponible.
//	            if (listCoordinator == null) {
//	                System.err.println("WARN [MouseWheel Miniaturas]: ListCoordinator es null. No se puede navegar.");
//	                return; // Salir del listener si no hay coordinador.
//	            }
//	
//	            // 3.2.2. Obtener la dirección de rotación de la rueda.
//	            // e.getWheelRotation() devuelve:
//	            //   - Un número negativo (generalmente -1) si la rueda se movió hacia arriba (hacia el usuario).
//	            //   - Un número positivo (generalmente +1) si la rueda se movió hacia abajo (alejándose del usuario).
//	            int notches = e.getWheelRotation();
//	
//	            // Log para depuración (opcional, puedes comentarlo una vez funcione)
//	            // System.out.println("  [MouseWheel Miniaturas] Rueda movida. Notches: " + notches);
//	
//	            // 3.2.3. Determinar la acción de navegación basada en la dirección de la rueda.
//	            if (notches < 0) {
//	                // Rueda hacia ARRIBA -> Navegar a la imagen ANTERIOR
//	                // System.out.println("    -> Rueda ARRIBA sobre Miniaturas: Llamando a listCoordinator.seleccionarAnterior()");
//	                listCoordinator.seleccionarAnterior();
//	            } else if (notches > 0) {
//	                // Rueda hacia ABAJO -> Navegar a la imagen SIGUIENTE
//	                // System.out.println("    -> Rueda ABAJO sobre Miniaturas: Llamando a listCoordinator.seleccionarSiguiente()");
//	                listCoordinator.seleccionarSiguiente();
//	            }
//	            // Si notches es 0 (lo cual es raro para una rueda física), no se hace nada.
//	
//	            // 3.2.4. Consumir el evento.
//	            //         Esto es importante para evitar que el JScrollPane intente realizar
//	            //         su comportamiento de scroll por defecto (aunque las barras estén ocultas,
//	            //         el Look & Feel podría intentar alguna acción).
//	            e.consume();
//	
//	        }); // Fin de la lambda para el listener
//	
//	        System.out.println("  -> Nuevo MouseWheelListener para NAVEGACIÓN ÍTEM A ÍTEM añadido a scrollListaMiniaturas.");
//	
//	    } else {
//	        // Este log se mantiene si scrollListaMiniaturas es null al inicio.
//	        System.err.println("WARN [VisorController.configurarListenersVistaInternal]: scrollListaMiniaturas es null. No se pudo añadir listener de navegación por rueda.");
//	    }
//	    // --- FIN SECCIÓN 3 ---
//	    
//	    // --- SECCIÓN 4: GESTIONAR FOCO EN MINIATURAS AL HACER CLIC EN EL SCROLLPANE ---
//	    JScrollPane scrollMiniaturasParaFoco = view.getScrollListaMiniaturas(); 
//	    JList<String> listaMiniaturasParaFoco = view.getListaMiniaturas(); 
//	
//	    if (scrollMiniaturasParaFoco != null && listaMiniaturasParaFoco != null) {
//	        // 4.1. Limpiar listeners de ratón previos (si es necesario).
//	        for (java.awt.event.MouseListener ml : scrollMiniaturasParaFoco.getMouseListeners()) {
//	            if (ml.getClass().isAnonymousClass() || ml.getClass().getName().contains("MouseAdapter")) { // Identificación simple
//	                 scrollMiniaturasParaFoco.removeMouseListener(ml);
//	            }
//	        }
//	        // 4.2. Añadir el nuevo MouseAdapter.
//	        final JList<String> finalListaMinParaFoco = listaMiniaturasParaFoco; // Variable final para la lambda/clase anónima
//	        scrollMiniaturasParaFoco.addMouseListener(new MouseAdapter() {
//	            @Override
//	            public void mousePressed(java.awt.event.MouseEvent e) {
//	                if (finalListaMinParaFoco.isEnabled() && finalListaMinParaFoco.isFocusable()) {
//	                    // System.out.println("-> Clic en ScrollMiniaturas detectado. Solicitando foco para listaMiniaturas...");
//	                    finalListaMinParaFoco.requestFocusInWindow();
//	                }
//	            }
//	        });
//	        System.out.println("  -> MouseListener añadido a scrollListaMiniaturas para gestionar foco.");
//	    } else {
//	         System.err.println("WARN [VisorController.configurarListenersVistaInternal]: No se pudo añadir listener de foco (scroll o lista nulos).");
//	    }
//	    
//	    
//	 // --- SECCIÓN 5: NUEVO - CONFIGURAR LISTENERS PARA ICONOS DE BARRA INFERIOR ---
//	    System.out.println("  -> Configurando Listeners para Iconos de Barra Inferior...");
//	
//	    // 5.1 Listener para Icono Zoom Manual
//	    final JLabel zmIconLabel = view.getIconoZoomManualLabel(); // Usa el getter de VisorView
//	    final Action zoomManualAction = actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
//	    if (zmIconLabel != null && zoomManualAction != null) {
//	        zmIconLabel.addMouseListener(new MouseAdapter() {
//	            @Override
//	            public void mouseClicked(MouseEvent e) {
//	                System.out.println("  [VisorController Listener] Clic en icono Zoom Manual.");
//	                // Invertir el estado de SELECTED_KEY en la Action antes de ejecutarla
//	                boolean nuevoEstado = !Boolean.TRUE.equals(zoomManualAction.getValue(Action.SELECTED_KEY));
//	                zoomManualAction.putValue(Action.SELECTED_KEY, nuevoEstado);
//	                zoomManualAction.actionPerformed(new ActionEvent(zmIconLabel, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE));
//	            }
//	            @Override public void mouseEntered(MouseEvent e) { zmIconLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
//	            @Override public void mouseExited(MouseEvent e) { zmIconLabel.setCursor(Cursor.getDefaultCursor()); }
//	        });
//	        System.out.println("    -> MouseListener añadido a iconoZoomManualLabel.");
//	    } else {
//	        if (zmIconLabel == null) System.err.println("WARN [VC Listeners]: iconoZoomManualLabel es null.");
//	        if (zoomManualAction == null) System.err.println("WARN [VC Listeners]: zoomManualAction no encontrada en actionMap.");
//	    }
//	
//	    // 5.2 Listener para Icono Mantener Proporciones
//	    final JLabel propIconLabel = view.getIconoMantenerProporcionesLabel();
//	    final Action proporcionesAction = actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);
//	    if (propIconLabel != null && proporcionesAction != null) {
//	        propIconLabel.addMouseListener(new MouseAdapter() {
//	            @Override
//	            public void mouseClicked(MouseEvent e) {
//	                System.out.println("  [VisorController Listener] Clic en icono Mantener Proporciones.");
//	                boolean nuevoEstado = !Boolean.TRUE.equals(proporcionesAction.getValue(Action.SELECTED_KEY));
//	                proporcionesAction.putValue(Action.SELECTED_KEY, nuevoEstado);
//	                proporcionesAction.actionPerformed(new ActionEvent(propIconLabel, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES));
//	            }
//	            @Override public void mouseEntered(MouseEvent e) { propIconLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
//	            @Override public void mouseExited(MouseEvent e) { propIconLabel.setCursor(Cursor.getDefaultCursor()); }
//	        });
//	        System.out.println("    -> MouseListener añadido a iconoMantenerProporcionesLabel.");
//	    } else {
//	        if (propIconLabel == null) System.err.println("WARN [VC Listeners]: iconoMantenerProporcionesLabel es null.");
//	        if (proporcionesAction == null) System.err.println("WARN [VC Listeners]: proporcionesAction no encontrada en actionMap.");
//	    }
//	
//	    // 5.3 Listener para Icono Modo Subcarpetas
//	    final JLabel subcIconLabel = view.getIconoModoSubcarpetasLabel();
//	    final Action subcarpetasAction = actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
//	    if (subcIconLabel != null && subcarpetasAction != null) {
//	        subcIconLabel.addMouseListener(new MouseAdapter() {
//	            @Override
//	            public void mouseClicked(MouseEvent e) {
//	                System.out.println("  [VisorController Listener] Clic en icono Modo Subcarpetas.");
//	                boolean nuevoEstado = !Boolean.TRUE.equals(subcarpetasAction.getValue(Action.SELECTED_KEY));
//	                subcarpetasAction.putValue(Action.SELECTED_KEY, nuevoEstado);
//	                subcarpetasAction.actionPerformed(new ActionEvent(subcIconLabel, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_TOGGLE_SUBCARPETAS));
//	            }
//	            @Override public void mouseEntered(MouseEvent e) { subcIconLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
//	            @Override public void mouseExited(MouseEvent e) { subcIconLabel.setCursor(Cursor.getDefaultCursor()); }
//	        });
//	        System.out.println("    -> MouseListener añadido a iconoModoSubcarpetasLabel.");
//	    } else {
//	        if (subcIconLabel == null) System.err.println("WARN [VC Listeners]: iconoModoSubcarpetasLabel es null.");
//	        if (subcarpetasAction == null) System.err.println("WARN [VC Listeners]: subcarpetasAction no encontrada en actionMap.");
//	    }
//	    
//	    // --- SECCIÓN 6: LOG FINAL ---
//	    System.out.println("[Controller Internal] Configuración de Listeners de Vista finalizada.");
//	
//} // --- FIN configurarListenersVistaInternal ---
    
    
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
        Action toggleCheckeredBgAction = this.actionMap.get(AppActionCommands.CMD_VISTA_TOGGLE_CHECKERED_BG);
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
        Action toggleAlwaysOnTopAction = this.actionMap.get(AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP);
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
        // 1. --- LOG DE ENTRADA Y VALIDACIÓN DE DEPENDENCIAS ---
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

        // 2. --- DESPACHO DE LA SOLICITUD BASADO EN uiElementIdentifier ---
        switch (uiElementIdentifier) {
            // 2.1. CASOS PARA LAS BARRAS DE INFORMACIÓN:
            case "REFRESH_INFO_BAR_SUPERIOR":
            case "REFRESH_INFO_BAR_INFERIOR":
                System.out.println("  -> [VisorController] UI ID para InfoBar: '" + uiElementIdentifier + "'. Delegando a InfoBarManager.");
                if (infoBarManager != null) {
                    infoBarManager.actualizarBarrasDeInfo();
                } else { /* ... error ... */ }
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

            case "mostrar_ocultar_la_lista_de_archivos": // uiElementIdentifier usado por ToggleFileListAction
            	System.out.println("  -> [VisorController] UI ID: mostrar_ocultar_la_lista_de_archivos. Visibilidad a: " + nuevoEstadoVisible);
                JPanel panelControlado = view.getPanelContenedorIzquierdoSplit(); // Asumiendo que tienes este getter

                if (panelControlado != null) {
                    if (panelControlado.isVisible() != nuevoEstadoVisible) {
                        view.setFileListVisible(nuevoEstadoVisible);
                        necesitaRevalidateRepaintGeneralDelFrame = true;
                    } else {
                        System.out.println("  -> [VisorController] El panelContenedorIzquierdoSplit ya está en el estado de visibilidad deseado: " + nuevoEstadoVisible);
                    }
                } else {
                    System.err.println("  ERROR [VisorController]: El panel a controlar (panelContenedorIzquierdoSplit) es null en VisorView.");
                }
                break;
            	

            case "imagenes_en_miniatura": // uiElementIdentifier usado por ToggleThumbnailsAction
                System.out.println("  -> [VisorController] UI ID: imagenes_en_miniatura. Visibilidad a: " + nuevoEstadoVisible);
                if (view.getScrollListaMiniaturas() != null && view.getScrollListaMiniaturas().isVisible() != nuevoEstadoVisible) {
                    view.setThumbnailsVisible(nuevoEstadoVisible);
                    necesitaRevalidateRepaintGeneralDelFrame = true;
                }
                break;
            
            // Este caso se activa si ToggleLocationBarAction usa "linea_de_ubicacion_del_archivo" como uiElementId.
            // Si ToggleLocationBarAction fue modificada para usar "REFRESH_INFO_BAR_INFERIOR",
            // entonces este 'case' específico ya no es necesario aquí.
            // Por coherencia con el menú "Vista", lo mantenemos, asumiendo que puede ser un panel diferente
            // o que decidiste mantener este uiElementId específico para ella.
            case "linea_de_ubicacion_del_archivo":
                System.out.println("  -> [VisorController] UI ID: linea_de_ubicacion_del_archivo.");
                // Si "Linea de Ubicacion" es PARTE de la barra inferior y su Action ahora usa
                // "REFRESH_INFO_BAR_INFERIOR", este case no se dispararía por esa Action.
                // Si es un panel separado que VisorView maneja con setLocationBarVisible:
                if (view.getTextoRuta() != null && view.getTextoRuta().isVisible() != nuevoEstadoVisible) { // Suponiendo que getTextoRuta() es el componente
                     view.setLocationBarVisible(nuevoEstadoVisible);
                     necesitaRevalidateRepaintGeneralDelFrame = true;
                } else if (infoBarManager != null) {
                    // O si decidiste que esta opción también refresca la barra inferior
                    // (porque ToggleLocationBarAction usa configKey de la barra inf y uiElementId REFRESH_INFO_BAR_INFERIOR)
                     infoBarManager.actualizarBarrasDeInfo();
                }
                break;

            // 2.3. CASO POR DEFECTO:
            default:
                System.err.println("  WARN [VisorController.solicitarActualizacionInterfaz]: uiElementIdentifier no reconocido: '" +
                                   uiElementIdentifier + "'. No se realizó acción de UI específica.");
                break;
        }

        // 3. --- REVALIDACIÓN Y REPINTADO DEL FRAME (SI ES NECESARIO) ---
        if (necesitaRevalidateRepaintGeneralDelFrame && view.getFrame() != null) {
            System.out.println("  -> [VisorController] Programando revalidate y repaint del frame principal.");
            javax.swing.SwingUtilities.invokeLater(() -> {
                if (view != null && view.getFrame() != null) {
                    view.getFrame().revalidate();
                    view.getFrame().repaint();
                }
            });
        }

        // 4. --- LOG FINAL ---
        System.out.println("[VisorController.solicitarActualizacionInterfaz] Procesamiento finalizado para UI ID: " + uiElementIdentifier);
    }// FIN del metodo solicitarActualizacionInterfaz
    
    
   
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
    
    
    
    
 // ******************************************************************************************************************* CARGA      

     
    /**
     * Carga la configuración inicial de la interfaz de usuario (visibilidad, selección de menús/botones)
     * leyendo los valores desde ConfigurationManager y aplicándolos a la Vista y al Modelo.
     * Se llama desde el constructor (en el EDT) después de crear la Vista.
     */
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
        	Action toggleZoomManualAction = (this.actionMap != null) ? this.actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE) : null;
            boolean zoomManualInicial = Boolean.TRUE.equals(toggleZoomManualAction.getValue(Action.SELECTED_KEY));
            if(view != null) view.actualizarEstadoControlesZoom(zoomManualInicial, zoomManualInicial); // Habilita Reset si Zoom está activo

            // Sincronizar estado visual del botón de Proporciones
        	Action toggleProporcionesAction = (this.actionMap != null) ? this.actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES) : null;
            boolean proporcionesInicial = Boolean.TRUE.equals(toggleProporcionesAction.getValue(Action.SELECTED_KEY));
            this.view.actualizarAspectoBotonToggle(toggleProporcionesAction, proporcionesInicial);

            // Sincronizar estado visual del botón y radios de Subcarpetas
        	Action toggleSubfoldersAction = (this.actionMap != null) ? this.actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS) : null;
            boolean subcarpetasInicial = Boolean.TRUE.equals(toggleSubfoldersAction.getValue(Action.SELECTED_KEY));
            this.view.actualizarAspectoBotonToggle(toggleSubfoldersAction, subcarpetasInicial);
            restaurarSeleccionRadiosSubcarpetas(subcarpetasInicial); // Asegurar estado visual radios
             System.out.println("    -> Estados iniciales específicos (Zoom, Prop, Sub) aplicados a UI.");
        } catch(Exception e) { System.err.println("ERROR aplicando estados específicos: " + e.getMessage()); }


        System.out.println("  [Apply Config] Finalizado.");
        
    }// --- FIN del metodo aplicarConfiguracionInicial


    /**
     * Carga o recarga la lista de imágenes desde disco para una carpeta específica,
     * utilizando un SwingWorker para no bloquear el EDT. Muestra un diálogo de
     * progreso durante la carga. Una vez cargada la lista: 
     * - Actualiza el modelo principal de datos (`VisorModel`). 
     * - Actualiza las JList en la vista (`VisorView`). 
     * - Inicia el precalentamiento ASÍNCRONO y DIRIGIDO del caché de miniaturas. 
     * - Selecciona una imagen específica (si se proporciona `claveImagenAMantener`) 
     *   o la primera imagen de la lista. 
     * - Maneja la selección inicial de forma segura usando el flag `seleccionInicialEnCurso`.
     *
     * @param claveImagenAMantener La clave única (ruta relativa) de la imagen que
     *                             se intentará seleccionar después de que la lista
     *                             se cargue. Si es `null`, se seleccionará la
     *                             primera imagen (índice 0).
     */
    public void cargarListaImagenes(String claveImagenAMantener) {

        // --- 1. LOG INICIO Y VALIDACIONES PREVIAS ---
        // 1.1. Log detallado del inicio y la clave a mantener.
        System.out.println("\n-->>> INICIO cargarListaImagenes(String) | Mantener Clave: " + claveImagenAMantener);

        // 1.2. Verificar dependencias críticas del sistema.
        if (configuration == null || model == null || executorService == null || executorService.isShutdown() || view == null) {
            System.err.println("ERROR [cargarListaImagenes]: Dependencias nulas (Config, Modelo, Executor o Vista) o Executor apagado.");
            if (view != null) SwingUtilities.invokeLater(this::limpiarUI);
            estaCargandoLista = false;
            return;
        }

        // 1.3. Marcar que la carga de la lista está en curso.
        estaCargandoLista = true;

        // --- 2. CANCELAR TAREAS ANTERIORES ---
        // 2.1. Cancelar cualquier tarea previa de carga de lista de imágenes que aún esté activa.
        if (cargaImagenesFuture != null && !cargaImagenesFuture.isDone()) {
            System.out.println("  -> Cancelando tarea de carga de lista anterior...");
            cargaImagenesFuture.cancel(true); 
        }

        // --- 3. DETERMINAR PARÁMETROS DE BÚSQUEDA DE ARCHIVOS ---
        // 3.1. Determinar si se deben incluir subcarpetas, leyendo del modelo.
        final boolean mostrarSoloCarpeta = model.isMostrarSoloCarpetaActual();
        // 3.2. Establecer la profundidad de búsqueda para Files.walk.
        int depth = mostrarSoloCarpeta ? 1 : Integer.MAX_VALUE;
        System.out.println("  -> Modo búsqueda: " + (mostrarSoloCarpeta ? "Solo Carpeta Actual (depth=1)" : "Subcarpetas (depth=MAX)"));
        
        // 3.3. Determinar la carpeta desde donde iniciar la búsqueda (`pathDeInicioWalk`).
        Path pathDeInicioWalk = null;
        if (mostrarSoloCarpeta) {
            String claveReferenciaParaCarpeta = claveImagenAMantener != null ? claveImagenAMantener : model.getSelectedImageKey();
            Path rutaImagenReferencia = claveReferenciaParaCarpeta != null ? model.getRutaCompleta(claveReferenciaParaCarpeta) : null;
            if (rutaImagenReferencia != null && Files.isRegularFile(rutaImagenReferencia)) {
                pathDeInicioWalk = rutaImagenReferencia.getParent();
            }
            if (pathDeInicioWalk == null || !Files.isDirectory(pathDeInicioWalk)) {
                System.out.println("    -> [cargarListaImagenes] No se pudo obtener carpeta de imagen de referencia válida para 'solo carpeta'. Usando carpeta raíz actual del MODELO: " + this.model.getCarpetaRaizActual());
                pathDeInicioWalk = this.model.getCarpetaRaizActual();
            } else {
                System.out.println("    -> [cargarListaImagenes] Iniciando búsqueda (solo carpeta) desde carpeta de imagen de referencia: " + pathDeInicioWalk);
            }
        } else {
            pathDeInicioWalk = this.model.getCarpetaRaizActual();
            System.out.println("    -> [cargarListaImagenes] Iniciando búsqueda (con subcarpetas) desde carpeta raíz del MODELO: " + pathDeInicioWalk);
        }

        // --- 4. VALIDAR PATH DE INICIO Y PROCEDER CON LA CARGA ---
        // 4.1. Comprobar si el `pathDeInicioWalk` calculado es un directorio válido.
        if (pathDeInicioWalk != null && Files.isDirectory(pathDeInicioWalk)) {
            // 4.2. Crear variables finales para ser accesibles por lambdas y el SwingWorker.
            final Path finalStartPath = pathDeInicioWalk;
            final int finalDepth = depth;
            final String finalClaveImagenAMantenerWorker = claveImagenAMantener;
            final Path finalRutaRaizParaRelativizar = this.model.getCarpetaRaizActual();

            // --- 5. LIMPIEZA INICIAL DE LA INTERFAZ DE USUARIO (UI) ---
            // 5.1. Programar la limpieza en el EDT.
            if (view != null) {
                SwingUtilities.invokeLater(() -> {
                    if (view != null) { 
                        view.limpiarImagenMostrada();
                        view.setTextoBarraEstadoRuta(""); 
                        view.setTituloPanelIzquierdo("Escaneando: " + finalStartPath.getFileName() + "...");
                        if (view.getListaMiniaturas() != null) {
                            if (this.modeloMiniaturas != null) { 
                                this.modeloMiniaturas.clear();
                            }
                        }
                    }
                });
            }
            
            // --- 6. CREAR DIÁLOGO DE PROGRESO Y EL SWINGWORKER ---
            System.out.println("  -> [cargarListaImagenes] Creando diálogo y worker para búsqueda en: " + finalStartPath + " (Relativizar a: " + finalRutaRaizParaRelativizar + ")");
            // 6.1. Crear el diálogo, pasando null para el worker inicialmente.
            final ProgresoCargaDialog dialogo = new ProgresoCargaDialog((view != null ? view.getFrame() : null), null); 
            // 6.2. Crear el BuscadorArchivosWorker, pasándole la referencia al diálogo.
            final BuscadorArchivosWorker worker = new BuscadorArchivosWorker(
                finalStartPath, 
                finalDepth, 
                finalRutaRaizParaRelativizar,
                this::esArchivoImagenSoportado, 
                dialogo
            );
            // 6.3. Asociar el worker al diálogo.
            dialogo.setWorkerAsociado(worker); 
            this.cargaImagenesFuture = worker;
            System.out.println("  -> [cargarListaImagenes] Diálogo y Worker creados y asociados.");

            // --- 7. CONFIGURAR EL LISTENER PARA CUANDO EL WORKER TERMINE ('done') ---
            System.out.println("  -> [cargarListaImagenes] Añadiendo PropertyChangeListener al worker...");
            worker.addPropertyChangeListener(evt -> {
                // 7.1. Comprobar si la propiedad que cambió es "state" y si el nuevo estado es DONE.
                if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE.equals(evt.getNewValue())) {
                    System.out.println("  [EDT Worker Listener] Tarea de búsqueda (" + worker.getClass().getSimpleName() + ") ha finalizado (DONE). Procesando resultado...");
                    if (dialogo != null) dialogo.cerrar(); // 7.1.1

                    // 7.2. Comprobar si la tarea fue cancelada.
                    if (worker.isCancelled()) {
                        System.out.println("    -> Tarea CANCELADA por el usuario.");
                        if (view != null) {
                            limpiarUI(); 
                            view.setTituloPanelIzquierdo("Carga Cancelada");
                        }
                        estaCargandoLista = false; 
                        return; 
                    }

                    // 7.3. Obtener el resultado.
                    try {
                        Map<String, Path> mapaResultado = worker.get();

                        // 7.4. Procesar el resultado.
                        if (mapaResultado != null) {
                            System.out.println("    WORKER HA TERMINADO. Número de archivos encontrados: " + mapaResultado.size());
                            DefaultListModel<String> nuevoModeloListaPrincipal = new DefaultListModel<>();
                            List<String> clavesOrdenadas = new ArrayList<>(mapaResultado.keySet());
                            Collections.sort(clavesOrdenadas);
                            clavesOrdenadas.forEach(nuevoModeloListaPrincipal::addElement); // 7.4.1
                            System.out.println("    -> Resultado obtenido del worker: " + nuevoModeloListaPrincipal.getSize() + " archivos. Actualizando modelo y vista...");
                            
                            model.actualizarListaCompleta(nuevoModeloListaPrincipal, mapaResultado); // 7.4.2
                            DefaultListModel<String> modeloPrincipalActualizado = model.getModeloLista();

                            if (view != null) { // 7.4.3
                                view.setListaImagenesModel(modeloPrincipalActualizado);
                                view.setTituloPanelIzquierdo("Archivos: " + modeloPrincipalActualizado.getSize());
                            }

                            estaCargandoLista = false; // 7.4.4
                            System.out.println("    -> Flag estaCargandoLista puesto a FALSE.");

                            // 7.4.5. Iniciar precalentamiento DIRIGIDO y LIMITADO del caché de miniaturas.
                            List<Path> rutasParaPrecalentamientoDirigido = new ArrayList<>();
                            List<Path> todasLasRutasEncontradas = new ArrayList<>(mapaResultado.values()); 
                            if (!todasLasRutasEncontradas.isEmpty()) {
                                int limitePrecalentamientoInicial = Math.min(todasLasRutasEncontradas.size(), 30);
                                for (int i = 0; i < limitePrecalentamientoInicial; i++) {
                                    rutasParaPrecalentamientoDirigido.add(todasLasRutasEncontradas.get(i));
                                }
                                System.out.println("      -> [Precalentamiento] Añadidas " + limitePrecalentamientoInicial + " rutas iniciales para precalentar.");
                                if (finalClaveImagenAMantenerWorker != null && !finalClaveImagenAMantenerWorker.isEmpty()) {
                                    int indiceClaveAMantener = modeloPrincipalActualizado.indexOf(finalClaveImagenAMantenerWorker);
                                    if (indiceClaveAMantener != -1) { 
                                        int miniAntesConfig = model.getMiniaturasAntes(); 
                                        int miniDespuesConfig = model.getMiniaturasDespues();
                                        int inicioRangoMantener = Math.max(0, indiceClaveAMantener - miniAntesConfig);
                                        int finRangoMantener = Math.min(todasLasRutasEncontradas.size() - 1, indiceClaveAMantener + miniDespuesConfig);
                                        System.out.println("      -> [Precalentamiento] Intentando precalentar alrededor de '" + finalClaveImagenAMantenerWorker + 
                                                           "' (índice " + indiceClaveAMantener + "), rango en modelo principal [" + inicioRangoMantener + ".." + finRangoMantener + "]");
                                        for (int i = inicioRangoMantener; i <= finRangoMantener; i++) {
                                            if (i >= 0 && i < modeloPrincipalActualizado.getSize()) {
                                                String claveEnRango = modeloPrincipalActualizado.getElementAt(i);
                                                Path rutaEnRango = mapaResultado.get(claveEnRango); 
                                                if (rutaEnRango != null && !rutasParaPrecalentamientoDirigido.contains(rutaEnRango)) {
                                                    rutasParaPrecalentamientoDirigido.add(rutaEnRango);
                                                }
                                            }
                                        }
                                    } else { 
                                         System.out.println("      -> [Precalentamiento] Clave a mantener '" + finalClaveImagenAMantenerWorker + "' no encontrada en la nueva lista. No se precalienta rango específico.");
                                    }
                                }
                                System.out.println("    -> [VisorController] Solicitando precalentamiento DIRIGIDO para un total de " + 
                                                   rutasParaPrecalentamientoDirigido.size() + " miniaturas.");
                                precalentarCacheMiniaturasAsync(rutasParaPrecalentamientoDirigido);
                            } else { 
                                System.out.println("    -> [VisorController] No hay rutas para precalentar (mapaResultado vacío o lista de rutas vacía).");
                            }

                            // 7.4.6. Calcular el índice inicial a seleccionar.
                            int indiceCalculadoParaSeleccion = -1; 
                            if (finalClaveImagenAMantenerWorker != null && !finalClaveImagenAMantenerWorker.isEmpty() && !modeloPrincipalActualizado.isEmpty()) {
                                indiceCalculadoParaSeleccion = modeloPrincipalActualizado.indexOf(finalClaveImagenAMantenerWorker);
                            }
                            if (indiceCalculadoParaSeleccion == -1 && !modeloPrincipalActualizado.isEmpty()) {
                                indiceCalculadoParaSeleccion = 0;
                            }
                            System.out.println("      -> Índice final a seleccionar después de carga: " + indiceCalculadoParaSeleccion +
                                               (finalClaveImagenAMantenerWorker != null ? " (intentando mantener: '" + finalClaveImagenAMantenerWorker + "')" : " (default 0 si hay elementos)"));
                            
                            // 7.4.7. Aplicar la selección inicial.
                            final int indiceFinalParaSeleccionEnEDT = indiceCalculadoParaSeleccion;
                            if (indiceFinalParaSeleccionEnEDT != -1) {
                                if (view != null && view.getListaNombres() != null && listCoordinator != null) {
                                    System.out.println("    -> Aplicando selección inicial programática al índice: " + indiceFinalParaSeleccionEnEDT);
                                    seleccionInicialEnCurso = true;
                                    System.out.println("      -> Flag seleccionInicialEnCurso puesto a TRUE.");
                                    view.getListaNombres().setSelectedIndex(indiceFinalParaSeleccionEnEDT);
                                    SwingUtilities.invokeLater(() -> {
                                        SwingUtilities.invokeLater(() -> {
                                            if (listCoordinator != null) {
                                                System.out.println("      -> [WorkerDone Doble InvokeLater] Llamando MANUALMENTE a ListCoordinator para procesar índice inicial: " + indiceFinalParaSeleccionEnEDT);
                                                listCoordinator.seleccionarIndiceYActualizarUICompleta(indiceFinalParaSeleccionEnEDT);
                                                
                                             // << --- ACTUALIZAR BARRAS DE STATUS --- >>
                                                if (infoBarManager != null) {
                                                    infoBarManager.actualizarBarrasDeInfo();
                                                }
                                                
                                            } else { 
                                                System.err.println("ERROR [WorkerDone Doble InvokeLater]: ListCoordinator es null al intentar procesar índice inicial.");
                                            }
                                        });
                                    });
                                    Timer timerFinSeleccion = new Timer(200, (evtTimer) -> {
                                        seleccionInicialEnCurso = false;
                                        System.out.println("    -> [Timer Fin Selección Inicial] Flag seleccionInicialEnCurso puesto a FALSE. (Índice que se intentó seleccionar: " + indiceFinalParaSeleccionEnEDT + ")");
                                        if (listCoordinator != null) {
                                             listCoordinator.asegurarVisibilidadAmbasListasSiVisibles(listCoordinator.getIndiceOficialSeleccionado());
                                             
                                          // << --- ACTUALIZAR BARRAS DE STATUS --- >>
                                             if (infoBarManager != null) {
                                                 infoBarManager.actualizarBarrasDeInfo();
                                             }
                                        }
                                    });
                                    timerFinSeleccion.setRepeats(false);
                                    timerFinSeleccion.start();
                                } else { 
                                    System.err.println("WARN [EDT WorkerDone]: Vista, listaNombres o ListCoordinator nulos al aplicar selección inicial.");
                                }
                            } else { // Lista vacía
                                System.out.println("    -> Lista vacía después de la carga/filtro. Limpiando UI.");
                                limpiarUI();
                                if (listCoordinator != null) listCoordinator.forzarActualizacionEstadoNavegacion();
                            }
                        } else { // Si mapaResultado fue null
                            System.out.println("    -> Resultado del worker fue null. Carga fallida.");
                            if (view != null) {
                                limpiarUI();
                                view.setTituloPanelIzquierdo("Carga Incompleta (resultado nulo)");
                            }
                            estaCargandoLista = false;
                        }
                    // 7.5. Manejar excepciones.
                    } catch (CancellationException ce) {
                        System.out.println("    -> Tarea CANCELADA (detectado en worker.get() o durante Files.walk).");
                        if (view != null) { limpiarUI(); view.setTituloPanelIzquierdo("Carga Cancelada"); }
                        estaCargandoLista = false;
                    } catch (InterruptedException ie) {
                        System.err.println("    -> Hilo INTERRUMPIDO esperando resultado del worker.");
                        if (view != null) { limpiarUI(); view.setTituloPanelIzquierdo("Carga Interrumpida"); }
                        Thread.currentThread().interrupt();
                        estaCargandoLista = false;
                    } catch (ExecutionException ee) {
                        System.err.println("    -> ERROR durante la ejecución del worker: " + ee.getCause());
                        Throwable causa = ee.getCause();
                        String msg = (causa != null) ? causa.getMessage() : ee.getMessage();
                        if (view != null) {
                            JOptionPane.showMessageDialog(view.getFrame(), "Error durante la carga de archivos:\n" + msg, "Error de Carga", JOptionPane.ERROR_MESSAGE);
                            limpiarUI(); view.setTituloPanelIzquierdo("Error de Carga");
                        }
                        if (causa != null) causa.printStackTrace(); else ee.printStackTrace();
                        estaCargandoLista = false;
                    } finally {
                        // 7.6. Asegurar reseteo de flags y limpieza de future.
                        if (estaCargandoLista) {
                            System.out.println("WARN [EDT Worker Listener - finally]: estaCargandoLista aún era true. Forzando a false.");
                            estaCargandoLista = false;
                        }
                        if (cargaImagenesFuture == worker) { 
                            cargaImagenesFuture = null;
                        }
                        System.out.println("  [EDT Worker Listener] Procesamiento del resultado finalizado.");
                        
                     // << --- ACTUALIZAR BARRAS DE STATUS --- >>
                        if (infoBarManager != null) {
                            infoBarManager.actualizarBarrasDeInfo();
                        }
                    }
                } 
            }); 

            // --- 8. EJECUTAR EL SWINGWORKER Y MOSTRAR EL DIÁLOGO DE PROGRESO ---
            System.out.println("  -> [cargarListaImagenes] Ejecutando worker y programando muestra de diálogo...");
            worker.execute(); 
            SwingUtilities.invokeLater(() -> { 
                if (dialogo != null) { 
                    System.out.println("    [EDT Carga Lista] Mostrando diálogo de progreso...");
                    dialogo.setVisible(true); 
                }
            });

        } else { // Si pathDeInicioWalk es null o no es un directorio
            // --- 9. MANEJAR ERROR DE CARPETA DE INICIO INVÁLIDA ---
            System.out.println("[cargarListaImagenes] No se puede cargar la lista: Carpeta de inicio inválida o nula: " + pathDeInicioWalk);
            if (view != null) SwingUtilities.invokeLater(this::limpiarUI);
            estaCargandoLista = false;
        }

        // --- 10. LOG FINAL DEL MÉTODO ---
        System.out.println("-->>> FIN cargarListaImagenes(String) | Clave mantenida: " + claveImagenAMantener);
    } // --- FIN del metodo cargarListaImagenes
    
    
    
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

        
        //FIXME establecer que la rueda del mouse sobre las listas actue como up y down y sobre la imagen de zoom 
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

            actionMapNombres.put(actPrev, new AbstractAction(actPrev) { 			@Override public void actionPerformed(ActionEvent e) 	{ /*logActionOrigin("Nombres", "UP/LEFT"); 		*/if (listCoordinator != null) listCoordinator.seleccionarAnterior(); }});
            actionMapNombres.put(actNext, new AbstractAction(actNext) { 			@Override public void actionPerformed(ActionEvent e) 	{ /*logActionOrigin("Nombres", "DOWN/RIGHT"); 	*/if (listCoordinator != null) listCoordinator.seleccionarSiguiente(); }});
            actionMapNombres.put(actFirst, new AbstractAction(actFirst) { 			@Override public void actionPerformed(ActionEvent e) 	{ /*logActionOrigin("Nombres", "HOME"); 		*/if(listCoordinator != null) listCoordinator.seleccionarPrimero(); }});
            actionMapNombres.put(actLast, new AbstractAction(actLast) { 			@Override public void actionPerformed(ActionEvent e) 	{ /*logActionOrigin("Nombres", "END"); 			*/if(listCoordinator != null) listCoordinator.seleccionarUltimo(); }});
            actionMapNombres.put(actPrevBlock, new AbstractAction(actPrevBlock) { 	@Override public void actionPerformed(ActionEvent e) 	{ /*logActionOrigin("Nombres", "PAGE_UP"); 	*/if (listCoordinator != null) listCoordinator.seleccionarBloqueAnterior(); }});
            actionMapNombres.put(actNextBlock, new AbstractAction(actNextBlock) { 	@Override public void actionPerformed(ActionEvent e) 	{ /*logActionOrigin("Nombres", "PAGE_DOWN"); */if (listCoordinator != null) listCoordinator.seleccionarBloqueSiguiente(); }});
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
    	
    	//FIXME añadir botones de flecha superpuestas sobre la imagen para imagen siguiente o anterior
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
            view.getListaNombres().setSelectedIndex(indiceSiguiente);

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
            view.getListaNombres().setSelectedIndex(index);

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

    
 // En controlador.VisorController.java

 // ... (otros campos de VisorController, incluyendo):
 // private boolean zoomManualEstabaActivoAntesDeError = false; // Asegúrate que este campo existe

     /**
      * Inicia el proceso de carga y visualización de la imagen principal.
      * Llamado por ListCoordinator después de actualizar el índice oficial, o
      * cuando se necesita refrescar la imagen por otras razones (ej. cambio de modo de zoom).
      * 
      * @param indiceSeleccionado El índice de la imagen a mostrar en el modelo principal.
      *                         Aunque la lógica principal usa model.getSelectedImageKey(),
      *                         el índice es útil para logs o lógica condicional si fuera necesaria.
      */
     public void actualizarImagenPrincipal(int indiceSeleccionado) { // El índice es principalmente informativo aquí

         // --- SECCIÓN 1: VALIDACIONES INICIALES Y OBTENCIÓN DE CLAVE/RUTA ---
         // 1.1. Validar dependencias críticas del VisorController.
         if (view == null || model == null || executorService == null || executorService.isShutdown()) {
             System.err.println("WARN [VisorController.actualizarImagenPrincipal]: Vista, Modelo o ExecutorService no están listos o están apagados. Abortando carga de imagen principal.");
             // Podríamos limpiar la UI aquí si fuera un estado irrecuperable,
             // pero usualmente ListCoordinator maneja la selección vacía.
             return;
         }
         
         // 1.2. Condición para limpiar la UI si no hay imagen seleccionada o el índice es inválido.
         //      Esto maneja el caso donde ListCoordinator indica que no hay selección.
         if (indiceSeleccionado == -1 || model.getSelectedImageKey() == null) {
             System.out.println("[VisorController.actualizarImagenPrincipal] No hay clave seleccionada en modelo o índice es -1. Limpiando área de imagen principal.");
             if (view != null) { // Solo limpiar la parte visual de la imagen
                 view.limpiarImagenMostrada(); // Esto ya resetea zoomFactorView y offsets en VisorView
                 // Mostrar la carpeta raíz en la barra de estado si está disponible
                 view.setTextoBarraEstadoRuta(model.getCarpetaRaizActual() != null ? model.getCarpetaRaizActual().toString() : "Ninguna carpeta seleccionada");
             }
             if (model != null) {
                 model.setCurrentImage(null); // Asegurar que no hay imagen en el modelo
                  // Si el zoom estaba habilitado, se podría considerar desactivarlo aquí
                  // o resetear el zoom, pero limpiarImagenMostrada en VisorView ya resetea los factores de vista.
                  // Por ahora, dejamos que el estado de zoomHabilitado persista según el usuario.
             }
             if (listCoordinator != null) {
                 listCoordinator.forzarActualizacionEstadoAcciones(); // Actualizar estado de acciones (ej. Eliminar, Rotar)
             }
             // Actualizar InfoBarManager si existe
             if (infoBarManager != null) {
                 infoBarManager.actualizarBarrasDeInfo();
             }
             return; // Salir, no hay imagen para cargar
         }
         
         // 1.3. Obtener la CLAVE de la imagen seleccionada DESDE EL MODELO.
         //      Usamos el modelo como la fuente de verdad para la selección actual.
         String archivoSeleccionadoKey = model.getSelectedImageKey();

         // 1.4. Validar la clave obtenida. Si es nula (aunque el chequeo anterior debería cubrirlo),
         //      se trata como si no hubiera imagen seleccionada.
         if (archivoSeleccionadoKey == null) {
              System.out.println("[VisorController.actualizarImagenPrincipal] Clave seleccionada en modelo es null (inesperado después de chequeo de índice). Limpiando UI.");
              limpiarUI(); // Llama al método de limpieza general del VisorController.
             return;
         }
         
         System.out.println("--> [VisorController.actualizarImagenPrincipal] Iniciando carga para clave: '" + archivoSeleccionadoKey + "' (Índice informativo: " + indiceSeleccionado + ")");

         // 1.5. Cancelar cualquier carga de imagen principal anterior que aún esté en curso.
         //      Esto previene que una carga antigua (más lenta) sobreescriba una nueva.
         if (cargaImagenPrincipalFuture != null && !cargaImagenPrincipalFuture.isDone()) {
             System.out.println("  -> Cancelando carga de imagen principal anterior...");
             cargaImagenPrincipalFuture.cancel(true); // Intentar interrumpir la tarea anterior.
         }

         // 1.6. Actualizar el estado 'enabled' de acciones sensibles al contexto (ej. Eliminar).
         //      Esto asegura que las acciones estén correctas para la nueva imagen que se va a cargar.
         Action delAction = this.actionMap.get(AppActionCommands.CMD_IMAGEN_ELIMINAR);
         if (delAction instanceof DeleteAction) { // Asumiendo que DeleteAction implementa una interfaz o tiene un método específico
             ((DeleteAction) delAction).actualizarEstadoEnabled();
         }
         // (Añadir aquí otras Actions si dependen de que haya una imagen seleccionada y necesitan actualizar su estado enabled).

         // 1.7. Obtener la ruta completa (Path) del archivo a cargar desde el modelo.
         Path rutaCompleta = model.getRutaCompleta(archivoSeleccionadoKey);

         // 1.8. Validar la ruta completa. Si es null, es un error grave de consistencia de datos.
         if (rutaCompleta == null) {
             System.err.println("ERROR GRAVE [VisorController.actualizarImagenPrincipal]: No se encontró ruta completa en el modelo para la clave válida: " + archivoSeleccionadoKey);
             model.setSelectedImageKey(null); // Deshacer selección en modelo como medida de seguridad.
             model.setCurrentImage(null);
             limpiarUI(); // Limpiar completamente la UI
             if (view != null) {
                  view.setTextoBarraEstadoRuta("Error CRÍTICO: Ruta no encontrada para " + archivoSeleccionadoKey);
                  // Mostrar un mensaje de error más prominente al usuario
                  JOptionPane.showMessageDialog(view.getFrame(), 
                                                "Error crítico: No se pudo encontrar la ruta para la imagen seleccionada.\n" +
                                                "Clave: " + archivoSeleccionadoKey, 
                                                "Error de Datos", JOptionPane.ERROR_MESSAGE);
             }
             return; // Salir si no hay ruta.
         }

         // --- SECCIÓN 2: PREPARAR UI PARA LA CARGA ---
         // 2.1. Mostrar la ruta completa en la barra de estado de la vista.
         if (view != null) {
             view.setTextoBarraEstadoRuta(rutaCompleta.toString());
         
             // 2.2. Mostrar un indicador visual de "Cargando..." en el panel de la imagen principal.
             //      VisorView.mostrarIndicadorCargaImagenPrincipal ya resetea zoomFactorView y offsets.
             view.mostrarIndicadorCargaImagenPrincipal("Cargando: " + rutaCompleta.getFileName() + "...");
         }

         // --- SECCIÓN 3: LANZAR TAREA DE CARGA DE IMAGEN EN SEGUNDO PLANO ---
         // 3.1. Crear variables finales para usar dentro de la lambda del SwingWorker o submit.
         final String finalKeyParaWorker = archivoSeleccionadoKey;
         final Path finalPathParaWorker = rutaCompleta;
         System.out.println("    [VisorController.actualizarImagenPrincipal] Lanzando tarea de carga en background para: " + finalPathParaWorker);

         // 3.2. Enviar la tarea de carga al ExecutorService.
         //      La lambda se ejecutará en un hilo del pool.
         cargaImagenPrincipalFuture = executorService.submit(() -> { // Inicio de la lambda para la tarea en segundo plano.
             
             // 3.2.1. Log de inicio de la tarea en background.
             System.out.println("      [BG Img Load - " + Thread.currentThread().getName() + "] Iniciando lectura para: " + finalPathParaWorker);
             BufferedImage imagenCargadaDesdeDisco = null;
             String mensajeErrorCarga = null;

             // 3.2.2. Bloque try-catch para la lectura del archivo de imagen desde disco.
             try {
                 // Verificar si el archivo existe antes de intentar leerlo.
                 if (!Files.exists(finalPathParaWorker)) {
                     // Esto debería ser raro si la lista de archivos se generó correctamente,
                     // pero es una buena comprobación por si el archivo fue eliminado externamente.
                     throw new IOException("El archivo no existe en la ruta especificada: " + finalPathParaWorker);
                 }
                 // Intentar leer la imagen.
                 imagenCargadaDesdeDisco = ImageIO.read(finalPathParaWorker.toFile());

                 // Comprobar si el hilo actual (de carga) fue interrumpido mientras leía.
                 if (Thread.currentThread().isInterrupted()) {
                     System.out.println("      [BG Img Load] Tarea interrumpida DURANTE o DESPUÉS de leer. Descartando resultado.");
                     return; // Salir de la tarea lambda, no procesar más.
                 }

                 // Si ImageIO.read devuelve null, el formato no es soportado o el archivo está corrupto/no es una imagen.
                 if (imagenCargadaDesdeDisco == null) {
                     mensajeErrorCarga = "Formato no soportado o archivo de imagen inválido/corrupto.";
                     System.err.println("      [BG Img Load] Error: " + mensajeErrorCarga + " (" + finalPathParaWorker.getFileName() + ")");
                 } else {
                     System.out.println("      [BG Img Load] Lectura de imagen desde disco correcta para: " + finalPathParaWorker.getFileName());
                 }

             } catch (IOException ioEx) {
                 mensajeErrorCarga = "Error de Entrada/Salida: " + ioEx.getMessage();
                 System.err.println("      [BG Img Load] " + mensajeErrorCarga + " (" + finalPathParaWorker.getFileName() + ")");
             } catch (OutOfMemoryError oom) {
                 mensajeErrorCarga = "Memoria insuficiente para cargar la imagen. Intente cerrar otras aplicaciones.";
                 System.err.println("      [BG Img Load] " + mensajeErrorCarga + " (" + finalPathParaWorker.getFileName() + ")");
                 if (servicioMiniaturas != null) servicioMiniaturas.limpiarCache(); // Intentar liberar memoria de miniaturas
             } catch (Exception ex) { // Captura genérica para otros errores inesperados.
                 mensajeErrorCarga = "Error inesperado al cargar la imagen: " + ex.getClass().getSimpleName() + " - " + ex.getMessage();
                 System.err.println("      [BG Img Load] " + mensajeErrorCarga + " (" + finalPathParaWorker.getFileName() + ")");
                 ex.printStackTrace(); // Imprimir stack trace para depuración.
             }

             // --- SECCIÓN 4: PROCESAR RESULTADO DE LA CARGA EN EL EDT (Event Dispatch Thread) ---
             //    Es crucial actualizar la UI solo desde el EDT.
             //    Solo continuar si la tarea no fue interrumpida externamente Y si la imagen
             //    que se intentó cargar sigue siendo la seleccionada en el modelo (para evitar
             //    actualizar con una imagen obsoleta si el usuario seleccionó otra muy rápido).

             final BufferedImage finalImagenCargadaParaEDT = imagenCargadaDesdeDisco; // Variable final para la lambda
             final String finalMensajeErrorParaEDT = mensajeErrorCarga;               // Variable final para la lambda

             // Comprobar si la tarea fue interrumpida O si la selección cambió mientras se cargaba.
             if (Thread.currentThread().isInterrupted() || !finalKeyParaWorker.equals(model.getSelectedImageKey())) {
                  System.out.println("      [BG Img Load] Carga cancelada o selección de imagen cambió (" +
                                    "Actual: " + model.getSelectedImageKey() + ", Cargada: " + finalKeyParaWorker + 
                                    "). Descartando resultado.");
                  return; // No actualizar la UI.
             }
             
             // Si llegamos aquí, la tarea no fue interrumpida y la selección no cambió.
             // Proceder a actualizar la UI en el EDT.
             SwingUtilities.invokeLater(() -> { // Inicio de la lambda para actualizar la UI en el EDT.
                 
                 // 4.1. Log de entrada al bloque EDT.
                 System.out.println("      [EDT Img Update - " + Thread.currentThread().getName() + "] Procesando resultado de carga para clave: " + finalKeyParaWorker);

                 // 4.2. Re-validar dependencias críticas DENTRO del EDT.
                 if (view == null || model == null || zoomManager == null) {
                      System.err.println("      [EDT Img Update] ERROR CRÍTICO: Vista, Modelo o ZoomManager nulos en invokeLater! No se puede actualizar UI.");
                      return; 
                 }
                 
                 // 4.3. Comprobar si la carga desde disco fue exitosa (finalImagenCargadaParaEDT no es null).
                 if (finalImagenCargadaParaEDT != null) {
                     // === Caso Éxito: Imagen Cargada Correctamente ===
                     System.out.println("        -> [EDT Img Update] Carga desde disco fue EXITOSA. Actualizando modelo y UI...");
                 
                     // 4.3.1. Actualizar la imagen original en el VisorModel.
                     model.setCurrentImage(finalImagenCargadaParaEDT);
                     
                     // --- LÓGICA PARA RESTAURAR ZOOM MANUAL (SI ESTABA ACTIVO ANTES DE UN POSIBLE ERROR PREVIO) ---
                     if (this.zoomManualEstabaActivoAntesDeError) {
                         // Solo restaurar si el zoom manual NO está ya activo actualmente en el modelo
                         // (para no interferir si el usuario lo cambió mientras se mostraba un error).
                         if (!model.isZoomHabilitado()) {
                             Action toggleAction = actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
                             if (toggleAction != null) { // Ya no es necesario chequear instanceof si confiamos en el actionMap
                                 System.out.println("        -> [EDT Img Update] Zoom manual estaba activo antes del error y ahora está inactivo. Disparando Action para restaurarlo.");
                                 toggleAction.actionPerformed(
                                     new ActionEvent(this.view.getFrame(), // Fuente del evento
                                                     ActionEvent.ACTION_PERFORMED,
                                                     AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE)
                                 );
                                 // La Action se encargará de llamar a ZoomManager, actualizar config, SELECTED_KEY, y UI.
                             } else {
                                  System.err.println("        -> [EDT Img Update] WARN: No se pudo obtener ToggleZoomManualAction para restaurar zoom.");
                             }
                         } else {
                              System.out.println("        -> [EDT Img Update] Zoom manual ya estaba activo (o fue reactivado por el usuario). No se restaura explícitamente.");
                         }
                     }
                     this.zoomManualEstabaActivoAntesDeError = false; // Resetear el flag después de usarlo.
                     // --- FIN LÓGICA RESTAURAR ZOOM MANUAL ---

                     // 4.3.2. Decidir cómo manejar el zoom/pan para la nueva imagen cargada.
                     //         El estado de model.isZoomHabilitado() ya es el correcto después de la posible restauración.
                     if (!model.isZoomHabilitado()) { // Si el zoom manual NO está activo:
                         // Aplicar el modo de zoom actual del modelo (ej. FIT_TO_SCREEN, MAINTAIN_CURRENT_ZOOM, etc.)
                         // O resetear el zoom si el modo actual no es "mantener".
                         if (model.getCurrentZoomMode() == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM && zoomManager != null) {
                             System.out.println("        -> [EDT Img Update] Modo MAINTAIN_CURRENT_ZOOM activo. Reaplicándolo para nueva imagen.");
                             zoomManager.aplicarModoDeZoom(ZoomModeEnum.MAINTAIN_CURRENT_ZOOM);
                             // aplicarModoDeZoom ya llama a refrescarVistaPrincipal... y actualiza InfoBar.
                         } else {
                             // Para otros modos, o si el zoom manual no está activo, reseteamos el estado de zoom y pan.
                             model.resetZoomState();
                             System.out.println("        -> [EDT Img Update] Zoom manual no activo (y modo no es MAINTAIN). Estado de zoom/pan reseteado para nueva imagen.");
                             if (zoomManager != null) zoomManager.refrescarVistaPrincipalConEstadoActualDelModelo(); // Refrescar con el estado reseteado
                         }
                     } else { // Si el zoom manual SÍ está activo:
                         // No reseteamos el zoom/pan, se mantiene el actual para la nueva imagen.
                         System.out.println("        -> [EDT Img Update] Zoom manual activo. Se mantendrá zoom/pan actual para nueva imagen.");
                         if (zoomManager != null) zoomManager.refrescarVistaPrincipalConEstadoActualDelModelo(); // Refrescar con el zoom/pan actual
                     }

                     // 4.3.3. Actualizar estado de la marca de proyecto en la UI.
                     Path rutaMostrada = model.getRutaCompleta(model.getSelectedImageKey());
                     Action toggleMarkImageAction = this.actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
                     if (projectManager != null && toggleMarkImageAction != null && rutaMostrada != null) {
                         boolean marcada = projectManager.estaMarcada(rutaMostrada);
                         // Sincronizar el estado de la Action y la UI del botón/barra de estado
                         toggleMarkImageAction.putValue(Action.SELECTED_KEY, marcada);
                         actualizarEstadoVisualBotonMarcarYBarraEstado(marcada, rutaMostrada);
                     } else if (rutaMostrada == null && toggleMarkImageAction != null) { // Caso raro: clave existe pero ruta no
                         toggleMarkImageAction.putValue(Action.SELECTED_KEY, Boolean.FALSE);
                         actualizarEstadoVisualBotonMarcarYBarraEstado(false, null);
                     }
                     System.out.println("        -> [EDT Img Update] FIN PROCESO ÉXITO (Vista refrescada, marca actualizada).");

                 } else {
                     // === Caso Error: Falla al Cargar la Imagen desde Disco ===
                     System.err.println("        -> [EDT Img Update] Error detectado al cargar desde disco: " + 
                                        (finalMensajeErrorParaEDT != null ? finalMensajeErrorParaEDT : "(Mensaje de error nulo)"));
                     
                     model.setCurrentImage(null); // Asegurar que no hay imagen válida en el modelo.
                     
                     // --- LÓGICA PARA DESACTIVAR ZOOM MANUAL (SI ESTABA ACTIVO) ---
                     // Guardar el estado actual del zoom manual ANTES de intentar desactivarlo.
                     this.zoomManualEstabaActivoAntesDeError = model.isZoomHabilitado(); 

                     if (model.isZoomHabilitado()) { // Si estaba activo, intentar desactivarlo
                         Action toggleAction = actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
                         if (toggleAction != null) {
                             System.out.println("        -> [EDT Img Update - Error Carga] Zoom manual está activo. Disparando ToggleZoomManualAction para desactivarlo.");
                             toggleAction.actionPerformed(
                                 new ActionEvent(this.view.getFrame(),
                                                 ActionEvent.ACTION_PERFORMED,
                                                 AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE)
                             );
                             // La Action se encarga de resetear el zoom en el modelo y UI.
                         } else {
                             System.err.println("        -> [EDT Img Update - Error Carga] WARN: No se pudo obtener ToggleZoomManualAction (CMD_ZOOM_MANUAL_TOGGLE) del actionMap.");
                             // Fallback muy básico si la Action no se encuentra (menos ideal que la Action lo haga)
                             // if (zoomManager != null) zoomManager.activarODesactivarZoomManual(false);
                             // if (view != null) view.actualizarEstadoControlesZoom(false, false);
                             // if (zoomManager != null) zoomManager.refrescarVistaPrincipalConEstadoActualDelModelo();
                         }
                     }
                     // --- FIN LÓGICA DESACTIVAR ZOOM MANUAL ---
                     
                     // Mostrar el error en la vista (VisorView se encarga de resetear sus propios factores de zoom/pan)
                     if (view != null) {
                         view.mostrarErrorEnVisorPrincipal(
                             (finalPathParaWorker != null ? finalPathParaWorker.getFileName().toString() : "Archivo Desconocido"),
                             (finalMensajeErrorParaEDT != null ? finalMensajeErrorParaEDT : "Formato no soportado o archivo de imagen inválido.")
                         );

                         // Actualizar la barra de estado inferior para reflejar el error.
                         String nombreArchivoProblematico = (finalPathParaWorker != null ? finalPathParaWorker.getFileName().toString() : "archivo desconocido");
                         String mensajeBarraEstado = "Error al cargar: " + nombreArchivoProblematico;
                         if (finalMensajeErrorParaEDT != null && !finalMensajeErrorParaEDT.isEmpty()) {
                             mensajeBarraEstado += " (" + finalMensajeErrorParaEDT + ")";
                         } else {
                             mensajeBarraEstado += " (Formato no soportado o archivo inválido.)";
                         }
                         view.setTextoBarraEstadoRuta(mensajeBarraEstado);
                     }
                     System.out.println("        -> [EDT Img Update] FIN PROCESO ERROR (Error mostrado en vista).");
                 }
                 
                 // Después de procesar éxito o error, siempre actualizar las barras de información
                 if (infoBarManager != null) {
                     infoBarManager.actualizarBarrasDeInfo();
                 }
                 
             }); // Fin de la lambda para actualizar la UI en el EDT (SwingUtilities.invokeLater).

         }); // --- FIN de la Tarea Background (executorService.submit) ---

         // --- SECCIÓN 5: LOG FINAL DEL MÉTODO PRINCIPAL (fuera de la tarea background) ---
         System.out.println("--> [VisorController.actualizarImagenPrincipal] Tarea de carga de imagen principal lanzada para: " + archivoSeleccionadoKey);

     } // --- FIN del método actualizarImagenPrincipal ---
    
    
//    /**
//     * Inicia el proceso de carga y visualización de la imagen principal.
//     * Llamado por ListCoordinator después de actualizar el índice oficial.
//     * @param indiceSeleccionado El índice de la imagen a mostrar (aunque usa model.getSelectedImageKey()).
//     *                         El índice es útil aquí principalmente para logs o lógica futura.
//     */
//	    public void actualizarImagenPrincipal(int indiceSeleccionado) { // El índice es informativo
//	
//	        // --- SECCIÓN 1: VALIDACIONES INICIALES Y OBTENCIÓN DE CLAVE/RUTA ---
//	        // 1.1. Validar dependencias críticas del VisorController.
//	        if (view == null || model == null || executorService == null || executorService.isShutdown()) {
//	            System.err.println("WARN [VisorController.actualizarImagenPrincipal]: Vista, Modelo o Executor no listos. Abortando carga.");
//	            return;
//	        }
//	        
//	        
//	        // condicion para limpiar la ui
//	        if (indiceSeleccionado == -1 || model.getSelectedImageKey() == null) { // Condición para limpiar
//	            System.out.println("[VisorController.actualizarImagenPrincipal] No hay clave seleccionada o índice es -1. Limpiando imagen principal.");
//	            if (view != null) { // Solo limpiar la parte visual de la imagen
//	                view.limpiarImagenMostrada();
//	                view.setTextoBarraEstadoRuta(model.getCarpetaRaizActual() != null ? model.getCarpetaRaizActual().toString() : ""); // Mostrar carpeta raíz si existe
//	            }
//	            if (model != null) model.setCurrentImage(null);
//	            // NO LLAMAR A limpiarUI() completo aquí, solo lo referente a la imagen principal.
//	            // Actualizar acciones sensibles al contexto
//	            if(listCoordinator != null) listCoordinator.forzarActualizacionEstadoAcciones();
//	            return;
//	        }
//	        
//	
//	        // 1.2. Obtener la CLAVE de la imagen seleccionada DESDE EL MODELO.
//	        String archivoSeleccionadoKey = model.getSelectedImageKey();
//	
//	        // 1.3. Validar la clave obtenida. Si es nula, no hay imagen seleccionada.
//	        if (archivoSeleccionadoKey == null) {
//	             System.out.println("[VisorController.actualizarImagenPrincipal] No hay clave seleccionada en modelo. Limpiando UI.");
//	             limpiarUI(); // Llama al método de limpieza general del VisorController.
//	            return;
//	        }
//	
//	        
//	        System.out.println("--> [VisorController.actualizarImagenPrincipal] Iniciando carga para clave: '" + archivoSeleccionadoKey + "' (Índice informativo: " + indiceSeleccionado + ")");
//	
//	        // 1.4. Cancelar cualquier carga de imagen principal anterior que aún esté en curso.
//	        if (cargaImagenPrincipalFuture != null && !cargaImagenPrincipalFuture.isDone()) {
//	            System.out.println("  -> Cancelando carga de imagen principal anterior...");
//	            cargaImagenPrincipalFuture.cancel(true); // Intentar interrumpir la tarea anterior.
//	        }
//	
//        Action delAction = this.actionMap.get(AppActionCommands.CMD_IMAGEN_ELIMINAR);
//        if (delAction instanceof DeleteAction) {
//            ((DeleteAction) delAction).actualizarEstadoEnabled();
//        }
//        
//        // (Añadir aquí otras Actions si dependen de que haya una imagen seleccionada y necesitan actualizar su estado enabled).
//
//        // 1.6. Obtener la ruta completa (Path) del archivo a cargar.
//        Path rutaCompleta = model.getRutaCompleta(archivoSeleccionadoKey);
//
//        // 1.7. Validar la ruta completa.
//        if (rutaCompleta == null) {
//            System.err.println("ERROR GRAVE [VisorController.actualizarImagenPrincipal]: No se encontró ruta completa para la clave válida: " + archivoSeleccionadoKey);
//            model.setSelectedImageKey(null); // Deshacer selección en modelo como medida de seguridad.
//            limpiarUI();
//            view.setTextoBarraEstadoRuta("Error CRÍTICO: Ruta no encontrada para " + archivoSeleccionadoKey);
//            return; // Salir si no hay ruta.
//        }
//
//        // --- SECCIÓN 2: PREPARAR UI PARA LA CARGA ---
//        // 2.1. Mostrar la ruta completa en la barra de estado de la vista.
//        view.setTextoBarraEstadoRuta(rutaCompleta.toString());
//        
//        // 2.2. Mostrar un indicador visual de "Cargando..." en el panel de la imagen principal.
//        view.mostrarIndicadorCargaImagenPrincipal("Cargando: " + rutaCompleta.getFileName() + "...");
//
//        // --- SECCIÓN 3: LANZAR TAREA DE CARGA DE IMAGEN EN SEGUNDO PLANO ---
//        // 3.1. Crear variables finales para usar dentro de la lambda del SwingWorker o submit.
//        final String finalKeyParaWorker = archivoSeleccionadoKey;
//        final Path finalPathParaWorker = rutaCompleta;
//        System.out.println("    [VisorController.actualizarImagenPrincipal] Lanzando tarea de carga en background para: " + finalPathParaWorker);
//
//        // 3.2. Enviar la tarea de carga al ExecutorService.
//        cargaImagenPrincipalFuture = executorService.submit(() -> { // Inicio de la lambda para la tarea en segundo plano.
//            
//            // 3.2.1. Log de inicio de la tarea en background.
//            System.out.println("      [BG Img Load - " + Thread.currentThread().getName() + "] Iniciando lectura para: " + finalPathParaWorker);
//            BufferedImage imagenCargadaDesdeDisco = null;
//            String mensajeErrorCarga = null;
//
//            // 3.2.2. Bloque try-catch para la lectura del archivo de imagen.
//            try {
//                if (!Files.exists(finalPathParaWorker)) {
//                    throw new IOException("El archivo no existe en la ruta especificada: " + finalPathParaWorker);
//                }
//                imagenCargadaDesdeDisco = ImageIO.read(finalPathParaWorker.toFile());
//
//                if (Thread.currentThread().isInterrupted()) {
//                    System.out.println("      [BG Img Load] Tarea interrumpida DESPUÉS de leer (o durante). Descartando.");
//                    return; 
//                }
//
//                if (imagenCargadaDesdeDisco == null) {
//                    mensajeErrorCarga = "Formato no soportado o archivo inválido.";
//                    System.err.println("      [BG Img Load] Error: " + mensajeErrorCarga + " (" + finalPathParaWorker.getFileName() + ")");
//                } else {
//                    System.out.println("      [BG Img Load] Lectura de imagen desde disco correcta.");
//                }
//
//            } catch (IOException ioEx) {
//                mensajeErrorCarga = "Error de E/S: " + ioEx.getMessage();
//                System.err.println("      [BG Img Load] " + mensajeErrorCarga + " (" + finalPathParaWorker.getFileName() + ")");
//            } catch (OutOfMemoryError oom) {
//                mensajeErrorCarga = "Memoria insuficiente para cargar la imagen.";
//                System.err.println("      [BG Img Load] " + mensajeErrorCarga + " (" + finalPathParaWorker.getFileName() + ")");
//                if (servicioMiniaturas != null) servicioMiniaturas.limpiarCache();
//            } catch (Exception ex) {
//                mensajeErrorCarga = "Error inesperado al cargar: " + ex.getClass().getSimpleName() + " - " + ex.getMessage();
//                System.err.println("      [BG Img Load] " + mensajeErrorCarga + " (" + finalPathParaWorker.getFileName() + ")");
//                ex.printStackTrace();
//            }
//
//            // --- SECCIÓN 4: PROCESAR RESULTADO DE LA CARGA EN EL EDT (Event Dispatch Thread) ---
//            //    Solo continuar si la tarea no fue interrumpida y la imagen seleccionada no ha cambiado mientras tanto.
//            final BufferedImage finalImagenCargada = imagenCargadaDesdeDisco; // Para usar en la lambda
//            final String finalMensajeError = mensajeErrorCarga;               // Para usar en la lambda
//
//            if (!Thread.currentThread().isInterrupted() && finalKeyParaWorker.equals(model.getSelectedImageKey())) {
//                
//                SwingUtilities.invokeLater(() -> { // Inicio de la lambda para actualizar la UI en el EDT.
//                    
//                    // 4.1. Log de entrada al bloque EDT.
//                    System.out.println("      [EDT Img Update - " + Thread.currentThread().getName() + "] Procesando resultado para: " + finalKeyParaWorker);
//
//                    // 4.2. Re-validar dependencias dentro del EDT.
//                    if (view == null || model == null || zoomManager == null) { // Añadida validación de zoomManager
//                         System.err.println("      [EDT Img Update] ERROR: Vista, Modelo o ZoomManager nulos en invokeLater!");
//                         return; 
//                    }
//                    
//                    // 4.3. Comprobar si la carga fue exitosa (finalImagenCargada no es null).
//                    if (finalImagenCargada != null) {
//                        // === Caso Éxito: Imagen Cargada Correctamente ===
//                        System.out.println("      [EDT Img Update] => Carga exitosa. Actualizando modelo...");
//                    
//                        // 4.3.1. Actualizar la imagen original en el VisorModel.
//                        model.setCurrentImage(finalImagenCargada);
//                        
//                        // 4.3.2. Decidir si resetear el zoom/pan.
//                        if (!model.isZoomHabilitado()) {
//                            model.resetZoomState(); 
//                            System.out.println("      [EDT Img Update] Zoom manual no activo. Estado de zoom/pan reseteado para nueva imagen.");
//                        } else {
//                            System.out.println("      [EDT Img Update] Zoom manual activo. Se mantendrá zoom/pan actual para nueva imagen.");
//                        }
//
//                        // 4.3.3. *** LLAMADA AL ZOOM MANAGER PARA REFRESCAR LA VISTA ***
//                        System.out.println("      [EDT Img Update] => Solicitando a ZoomManager que refresque la vista...");
//                        
//                        // LOG VisorController DEBUG
////                        System.out.println("  [VisorController DEBUG] Estado del MODELO ANTES DE REFRESCAR ZOOM: model.isMantenerProporcion()=" + model.isMantenerProporcion());
//                        zoomManager.refrescarVistaPrincipalConEstadoActualDelModelo();
//                        System.out.println("      [EDT Img Update] => FIN ÉXITO (Vista refrescada por ZoomManager).");
//
//                        // 4.3.4. Actualizar estado de la marca de proyecto.
//                        Path rutaMostrada = model.getRutaCompleta(model.getSelectedImageKey());
//                        Action toggleMarkImageAction = this.actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
//                        if (projectManager != null && toggleMarkImageAction != null && rutaMostrada != null) {
//                            boolean marcada = projectManager.estaMarcada(rutaMostrada);
//                            toggleMarkImageAction.putValue(Action.SELECTED_KEY, marcada);
//                            actualizarEstadoVisualBotonMarcarYBarraEstado(marcada, rutaMostrada);
//                        } else if (rutaMostrada == null && toggleMarkImageAction != null) {
//                            toggleMarkImageAction.putValue(Action.SELECTED_KEY, Boolean.FALSE);
//                            actualizarEstadoVisualBotonMarcarYBarraEstado(false, null);
//                        }
//
//                    } else {
//                        // === Caso Error: Falla al Cargar la Imagen ===
//                        System.err.println("      [EDT Img Update] => Error detectado al cargar: " + finalMensajeError);
//                        
//                        model.setCurrentImage(null); // Asegurar que no hay imagen en el modelo.
//                        
//                        if (view != null) { // Verificar que la vista exista
//                            
//                        	// LLAMADA AL MÉTODO DE VisorView para la imagen de error
//                        	view.mostrarErrorEnVisorPrincipal(
//                                    (finalPathParaWorker != null ? finalPathParaWorker.getFileName().toString() : "Desconocido"),
//                                    (finalMensajeError != null ? finalMensajeError : "Formato no soportado o archivo inválido.")
//                                );
//
//                            // Actualizar la barra de estado inferior (esto ya lo tenías bien)
//                            view.setTextoBarraEstadoRuta("Error cargando: " +
//                                                      (finalPathParaWorker != null ? finalPathParaWorker.getFileName() : "archivo desconocido") +
//                                                      (finalMensajeError != null ? " (" + finalMensajeError + ")" : " (Formato no soportado o archivo inválido.)"));
//                        }
//                        
//                        
//                        // Si el zoom manual estaba activo, podría ser buena idea desactivarlo
//                        // o al menos resetear el estado de zoom para evitar confusión.
//                        if (model.isZoomHabilitado()) {
//                            // zoomManager.activarODesactivarZoomManual(false); // Esto haría más cosas (actualizar actions, etc.)
//                            // O simplemente resetear el modelo y actualizar la UI de zoom
//                            model.resetZoomState();
//                            view.actualizarEstadoControlesZoom(false, false);
//                            
//                            Action resetZoomAction = this.actionMap.get(AppActionCommands.CMD_ZOOM_RESET);
//                            if (resetZoomAction != null) resetZoomAction.setEnabled(false);
//                            Action toggleZoomManualAction = this.actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
//                            if (toggleZoomManualAction != null) toggleZoomManualAction.putValue(Action.SELECTED_KEY, false);
//
//                        }
//                    }
//                    
//                }); // Fin de la lambda para actualizar la UI en el EDT.
//
//            } else {
//                 // 4.4. La tarea fue cancelada O la selección de imagen cambió mientras se cargaba.
//                 System.out.println("      [BG Img Load] Carga cancelada o selección de imagen cambió. Descartando resultado para: " + finalKeyParaWorker);
//            }
//        }); // --- FIN de la Tarea Background (executorService.submit) ---
//
//        // --- SECCIÓN 5: LOG FINAL DEL MÉTODO PRINCIPAL ---
//        System.out.println("--> [VisorController.actualizarImagenPrincipal] Tarea de carga de imagen principal lanzada para: " + archivoSeleccionadoKey);
//
//    } // --- FIN del método actualizarImagenPrincipal ---    
    
     
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
             view.setTextoBarraEstadoRuta("");

             // 3.4. Establecer título por defecto en el panel de la lista
             view.setTituloPanelIzquierdo("Lista de Archivos");

             // 3.5. Repintar la lista de miniaturas (ahora vacía)
             //      No necesitamos limpiar un panel, solo repintar la JList.
             if (view.getListaMiniaturas() != null) {
            	 if (this.modeloMiniaturas != null) { // modeloMiniaturas es el campo en VisorController
                     this.modeloMiniaturas.clear();
            	 }
             
                  System.out.println("  -> Lista de miniaturas repintada (vacía).");
             }

             // 3.6. Actualizar estado de Actions que dependen de la selección
             //      (Ej: Deshabilitar "Localizar Archivo", Edición, etc.)
             
             Action delActionClean = this.actionMap.get(AppActionCommands.CMD_IMAGEN_ELIMINAR);
             if (delActionClean instanceof DeleteAction) {((DeleteAction) delActionClean).actualizarEstadoEnabled();}

              System.out.println("  -> Vista actualizada a estado vacío.");

         } else {
             System.err.println("WARN [limpiarUI]: Vista es null. No se pudo actualizar UI.");
         }

         // 4. Limpiar caché de miniaturas (Opcional pero recomendado al limpiar todo)
         if (servicioMiniaturas != null) {
             servicioMiniaturas.limpiarCache();
             System.out.println("  -> Caché de miniaturas limpiado.");
         }

         // 5. Actualizar imagenes de proyectos marcadas
         Action toggleMarkImageAction = this.actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
         if (toggleMarkImageAction != null) {
        	    toggleMarkImageAction.setEnabled(false); // Deshabilitar si no hay imagen
        	    toggleMarkImageAction.putValue(Action.SELECTED_KEY, Boolean.FALSE);
        	    // Actualizar la UI para reflejar que no hay nada marcado
        	    actualizarEstadoVisualBotonMarcarYBarraEstado(false, null);
        	}
         
         // 6. Log fin
         if (listCoordinator != null) {
             listCoordinator.forzarActualizacionEstadoNavegacion();
         }
         
      // << --- ACTUALIZAR BARRAS AL FINAL DE LA LIMPIEZA --- >>
         if (infoBarManager != null) {
             infoBarManager.actualizarBarrasDeInfo();
         }
         
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
         if (view != null && view.getListaMiniaturas() != null) {
             SwingUtilities.invokeLater(() -> {
                 if (view != null && view.getListaMiniaturas() != null) { // Doble chequeo
                      System.out.println("  -> Solicitando repintado inicial de listaMiniaturas.");
                      view.getListaMiniaturas().repaint();
                 }
             });
         }

     } // --- FIN precalentarCacheMiniaturasAsync ---


// ************************************************************************************************************* FIN DE LOGICA     
// ***************************************************************************************************************************
      
// ***************************************************************************************************************************
// ********************************************************************************************************************** ZOOM     

	private void refrescarManualmenteLaVistaPrincipal ()
	{

		if (model == null || view == null)
		{
			System.err.println("ERROR [VisorController.refrescarManualmente]: Modelo o Vista nulos.");
			return;
		}
		BufferedImage imgOriginal = model.getCurrentImage();

		if (imgOriginal == null)
		{
			view.limpiarImagenMostrada();
			return;
		}
		Image imagenBaseEscalada = ImageDisplayUtils.reescalarImagenParaAjustar(imgOriginal, this.model, this.view);

		if (imagenBaseEscalada != null)
		{
			view.setImagenMostrada(imagenBaseEscalada, model.getZoomFactor(), model.getImageOffsetX(),
					model.getImageOffsetY());
		} else
		{
			System.err.println(
					"[VisorController.refrescarManualmente] ImageDisplayUtils.reescalarImagenParaAjustar devolvió null.");
			view.limpiarImagenMostrada();
		}

	} // FIN del metodo refrescarManualmenteLaVistaPrincipal


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
        
        this.view.actualizarAspectoBotonToggle(toggleSubfoldersAction, mostrarSubcarpetasDeseado);
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
         this.view.actualizarAspectoBotonToggle(toggleProporcionesAction, mantener); // Actualiza color/apariencia

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
                 this.zoomManager.refrescarVistaPrincipalConEstadoActualDelModelo();
                 
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
      public void copiarListaAlPortapapeles(DefaultListModel<String> listModel) {
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
       

	
	// En controlador.VisorController.java

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
        
        
        String checkBoxMenuItemConfigKey = findLongKeyForComponent(source); // Ej: "interfaz.menu.configuracion.visualizar_botones.boton_rotar_izquierda"

        if (source instanceof JCheckBoxMenuItem &&
            checkBoxMenuItemConfigKey != null &&
            checkBoxMenuItemConfigKey.startsWith("interfaz.menu.configuracion.visualizar_botones.")) {

            JCheckBoxMenuItem checkBox = (JCheckBoxMenuItem) source;
            boolean nuevoEstadoVisibleParaBotonToolbar = checkBox.isSelected(); // El estado del checkbox determina la visibilidad del botón

            // 'command' (de e.getActionCommand()) ES la clave del BOTÓN DE LA TOOLBAR.
            // En UIDefinitionService, para estos checkboxes, el primer parámetro fue la clave del botón.
            String claveDelBotonToolbarControlado = command;

            System.out.println("  [VC.actionPerformed] Checkbox 'Visualizar Botones Toolbar' clickeado:" +
                               "\n    - CheckBox Key (larga) : " + checkBoxMenuItemConfigKey +
                               "\n    - Controla Botón Key   : " + claveDelBotonToolbarControlado +
                               "\n    - Nuevo estado visible para botón: " + nuevoEstadoVisibleParaBotonToolbar);

            // 3.1. Guardar el estado del PROPIO JCheckBoxMenuItem en la configuración
            //      (usando la clave larga generada por MenuBarBuilder para este JCheckBoxMenuItem)
            if (configuration != null) {
                configuration.setString(checkBoxMenuItemConfigKey + ".seleccionado", String.valueOf(nuevoEstadoVisibleParaBotonToolbar));
            }

            // 3.2. Aplicar el cambio de VISIBILIDAD al BOTÓN DE LA TOOLBAR objetivo
            if (view != null && view.getBotonesPorNombre() != null) {
                JButton botonDeToolbar = view.getBotonesPorNombre().get(claveDelBotonToolbarControlado);

                if (botonDeToolbar != null) {
                    // ESTA ES LA LÍNEA CLAVE PARA ACTUALIZAR LA UI DEL BOTÓN
                    botonDeToolbar.setVisible(nuevoEstadoVisibleParaBotonToolbar);
                    System.out.println("    -> Visibilidad del Botón de Toolbar '" + claveDelBotonToolbarControlado + "' establecida a: " + botonDeToolbar.isVisible());

                    // 3.3. Guardar la preferencia de VISIBILIDAD del BOTÓN DE LA TOOLBAR
                    //      en la configuración, usando la clave del BOTÓN + ".visible".
                    if (configuration != null) {
                        configuration.setString(claveDelBotonToolbarControlado + ".visible", String.valueOf(nuevoEstadoVisibleParaBotonToolbar));
                        System.out.println("    -> Configuración de visibilidad del Botón Toolbar '" + claveDelBotonToolbarControlado + ".visible' guardada como: " + nuevoEstadoVisibleParaBotonToolbar);
                    }

                    // 3.4. Revalidar y repintar el panel de la toolbar
                    if (view.getPanelDeBotones() != null && view.getPanelDeBotones().isVisible()) {
                         final JPanel toolbarPanel = view.getPanelDeBotones();
                         SwingUtilities.invokeLater(() -> {
                             toolbarPanel.revalidate();
                             toolbarPanel.repaint();
                             System.out.println("    -> Panel de la Toolbar revalidado y repintado.");
                         });
                    }

                } else {
                    System.err.println("  ERROR [VC.actionPerformed]: Botón de Toolbar '" + claveDelBotonToolbarControlado +
                                       "' NO ENCONTRADO en view.getBotonesPorNombre(). Verificar claves en UIDefinitionService y ToolbarBuilder.");
                }
            } else {
                System.err.println("  ERROR [VC.actionPerformed]: VisorView o su mapa de botones (getBotonesPorNombre()) es null.");
            }
            return; // Evento manejado
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
                System.out.println("    -> Acción: Cargar Configuración Inicial");
                aplicarConfiguracionInicial(); // Aplica defaults a UI y Modelo
                cargarEstadoInicialInternal(); // Recarga lista de imágenes
                if (infoBarManager != null) infoBarManager.actualizarBarrasDeInfo(); // Refrescar barras
                if (view != null && view.getFrame() != null) { // Revalidar y repintar
                    SwingUtilities.invokeLater(() -> {
                        view.getFrame().revalidate();
                        view.getFrame().repaint();
                    });
                }
                JOptionPane.showMessageDialog(view.getFrame(),
                    "Configuración por defecto aplicada. Algunos cambios pueden requerir reiniciar la aplicación.",
                    "Configuración Restaurada", JOptionPane.INFORMATION_MESSAGE);
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
	 * Método helper para manejar la lógica cuando se selecciona "Zoom Personalizado
	 * %..." desde el menú.
	 */
	private void handleSetCustomZoomFromMenu ()
	{

		if (this.view == null || this.configuration == null || this.actionMap == null)
		{
			System.err.println("ERROR [handleSetCustomZoomFromMenu]: Vista, Configuración o ActionMap nulos.");
			return;
		}

		// 1. Mostrar JOptionPane para obtener el porcentaje del usuario
		String input = JOptionPane.showInputDialog(this.view.getFrame(), // Padre del diálogo
				"Introduce el porcentaje de zoom deseado (ej: 150):", // Mensaje
				"Establecer Zoom Personalizado", // Título
				JOptionPane.PLAIN_MESSAGE);

		// 2. Procesar la entrada
		if (input != null && !input.trim().isEmpty()){

			try{
				input = input.replace("%", "").trim(); // Limpiar
				double percentValue = Double.parseDouble(input);

				// Validar rango (igual que en InfoBarManager)
				if (percentValue >= 1 && percentValue <= 5000){ // Ajusta límites si es necesario
					// 2a. Actualizar la configuración en memoria
					this.configuration.setZoomPersonalizadoPorcentaje(percentValue);
					System.out.println("    -> Configuración '"
							+ ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_PERSONALIZADO_PORCENTAJE
							+ "' actualizada a: " + percentValue + "%");

					// 2b. Obtener y ejecutar la Action para aplicar el modo
					// USER_SPECIFIED_PERCENTAGE
					Action aplicarUserSpecifiedAction = this.actionMap
							.get(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO);

					if (aplicarUserSpecifiedAction != null){
						
						aplicarUserSpecifiedAction.actionPerformed(new ActionEvent(this.view.getFrame(), // Fuente puede
																											// ser el
																											// frame
						ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO));
						System.out.println("    -> Action CMD_ZOOM_TIPO_ESPECIFICADO ejecutada para aplicar el nuevo zoom.");
					} else{
						
						System.err.println("ERROR [handleSetCustomZoomFromMenu]: Action CMD_ZOOM_TIPO_ESPECIFICADO no encontrada en actionMap.");
					}
				} else{
					
					JOptionPane.showMessageDialog(this.view.getFrame(),
							"Porcentaje inválido. Debe estar entre 1 y 5000 (o el rango definido).", "Error de Entrada",
							JOptionPane.ERROR_MESSAGE);
				}
			} catch (NumberFormatException ex){
				
				JOptionPane.showMessageDialog(this.view.getFrame(), "Entrada inválida. Por favor, introduce un número.",
						"Error de Formato", JOptionPane.ERROR_MESSAGE);
			}
		} else
		{
			System.out.println("  -> Establecer Zoom Personalizado cancelado por el usuario o entrada vacía.");
		}

		// La actualización de InfoBarManager (para el JLabel de porcentaje) ocurrirá
		// cuando ZoomManager refresque la vista y llame a actualizarBarrasDeInfo().
	}
    
    
    /**
     * Este método se encargaría de marcar el radio correcto en el menú para mostrar carpeta o subcarpetas.
     * 
     * @param mostrarSubcarpetas
     */
    private void sincronizarRadiosSubcarpetasVisualmente(boolean mostrarSubcarpetas) {
        if (view == null || view.getMenuItemsPorNombre() == null) return;
        Map<String, JMenuItem> menuItems = view.getMenuItemsPorNombre();

        // Claves generadas por MenuBarBuilder (probablemente minúsculas)
        JMenuItem radioSolo = menuItems.get("interfaz.menu.configuracion.carga_de_imagenes.mostrar_solo_carpeta_actual");
        JMenuItem radioConSub = menuItems.get("interfaz.menu.configuracion.carga_de_imagenes.mostrar_imagenes_de_subcarpetas");

        if (radioSolo instanceof JRadioButtonMenuItem) {
            ((JRadioButtonMenuItem) radioSolo).setSelected(!mostrarSubcarpetas);
        }
        if (radioConSub instanceof JRadioButtonMenuItem) {
            ((JRadioButtonMenuItem) radioConSub).setSelected(mostrarSubcarpetas);
        }
    }    
    
    
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
  
  
     /** Getters para Modelo/Vista/Config (usados por Actions). */
     public VisorModel getModel() { return model; }
     public VisorView getView() { return view; }
     public ConfigurationManager getConfigurationManager() { return configuration; }
     
     
     public ListCoordinator getListCoordinator() {return this.listCoordinator;}

//     /**
//      * Recopila el estado final de la aplicación y lo persiste en el archivo de configuración.
//      * Este método se llama típicamente desde el ShutdownHook.
//      *
//      * El proceso general es:
//      * 1. Comenzar con una copia de todas las configuraciones por defecto (DEFAULT_CONFIG).
//      * 2. Superponer los valores que se cargaron del archivo config.cfg y cualquier
//      *    modificación realizada en `this.configuration` (el ConfigurationManager en memoria)
//      *    durante la sesión actual. Esto incluye estados de toggles de UI (checkboxes, radios)
//      *    que son actualizados por sus Actions o por actionPerformed.
//      * 3. Sobrescribir explícitamente en este mapa combinado aquellos valores críticos que
//      *    dependen directamente del estado del VisorModel en el momento del cierre (ej. última
//      *    imagen, configuraciones de miniaturas) o de Actions cuyo estado podría no haberse
//      *    reflejado directamente en `this.configuration` pero es importante persistir.
//      * 4. Pasar el mapa resultante al ConfigurationManager para que lo escriba al archivo,
//      *    manejando la preservación de comentarios y la adición de nuevas claves.
//      */
//     private void guardarConfiguracionActual() {//BEST segun IA
//         // --- SECCIÓN 1: VALIDACIÓN DE DEPENDENCIAS ESENCIALES ---
//         if (configuration == null || model == null || view == null || actionMap == null) {
//             System.err.println("ERROR CRÍTICO [VisorController.guardarConfiguracionActual]: " +
//                                "Dependencias esenciales (configuration, model, view, o actionMap) son nulas. " +
//                                "No se puede guardar la configuración.");
//             return;
//         }
//         System.out.println("  [ShutdownHook/GuardarConfig] Iniciando recopilación del estado actual para guardar...");
//
//         // --- SECCIÓN 2: CONSTRUIR EL MAPA FINAL DE CONFIGURACIÓN A GUARDAR ---
//
//         // 2.1. Empezar con una copia de TODAS las configuraciones por defecto.
//         //      Esto asegura que cualquier clave definida en DEFAULT_CONFIG (incluyendo nuevas
//         //      añadidas por el desarrollador) sea considerada para el guardado.
//         Map<String, String> estadoFinalAGuardar = new HashMap<>(ConfigurationManager.DEFAULT_CONFIG);
//         System.out.println("    [GuardarConfig] Paso 1: Mapa inicializado con " + estadoFinalAGuardar.size() + " claves desde DEFAULT_CONFIG.");
//
//         // 2.2. Superponer los valores actualmente en memoria en `this.configuration`.
//         //      Este mapa (`configuration.getConfigMap()`) contiene:
//         //      - Los valores que se cargaron del archivo `config.cfg` al inicio.
//         //      - Los valores por defecto para claves que no estaban en el archivo pero sí en DEFAULT_CONFIG (si `ConfigurationManager.cargarConfiguracion` los añade).
//         //      - **Más importante:** Cualquier cambio realizado durante la sesión a través de `configuration.setString()`.
//         //        Esto incluye el estado `.seleccionado` de los checkboxes/radios del menú que son
//         //        manejados por Actions que actualizan `this.configuration`, o por `actionPerformed`
//         //        (como los checkboxes de visibilidad de botones de la toolbar).
//         //        También incluye el estado de la ventana si `guardarEstadoVentanaEnConfig()` actualiza `this.configuration`.
//         Map<String, String> configEnMemoria = configuration.getConfigMap();
//         if (configEnMemoria != null) {
//             estadoFinalAGuardar.putAll(configEnMemoria);
//             System.out.println("    [GuardarConfig] Paso 2: Mapa actualizado con " + configEnMemoria.size() +
//                                " claves desde configuration.getConfigMap(). Tamaño actual: " + estadoFinalAGuardar.size());
//         } else {
//             System.err.println("WARN [GuardarConfig]: configuration.getConfigMap() devolvió null. No se pudieron superponer los valores de sesión.");
//         }
//
//         // 2.3. Sobrescribir explícitamente en 'estadoFinalAGuardar' ciertos valores críticos
//         //      que deben tomarse directamente del estado del MODELO o de las ACTIONS en el
//         //      momento del cierre, para asegurar la máxima fidelidad.
//         System.out.println("    [GuardarConfig] Paso 3: Asegurando valores críticos del Modelo y Actions...");
//         try {
//             // --- ESTADO DE LA VENTANA ---
//             // Asumimos que guardarEstadoVentanaEnConfig() (llamado antes en el ShutdownHook)
//             // ya actualizó las claves KEY_WINDOW_X, ..., KEY_WINDOW_MAXIMIZED en 'this.configuration'.
//             // Por lo tanto, el putAll(configEnMemoria) anterior ya debería tener los valores correctos.
//             // No es necesario volver a ponerlos aquí si ese es el caso.
//             // Solo como verificación, podríamos loguearlos:
//             System.out.println("      - Ventana X (desde config en memoria): " + estadoFinalAGuardar.get(ConfigurationManager.KEY_WINDOW_X));
//
//
//             // --- ÚLTIMA CARPETA E IMAGEN ---
//             // `inicio.carpeta` debería estar actualizado en `configEnMemoria` si el usuario
//             // usó "Abrir Carpeta...". Nos aseguramos de que no sea null.
//             estadoFinalAGuardar.put(ConfigurationManager.KEY_INICIO_CARPETA,
//                 configEnMemoria.getOrDefault(ConfigurationManager.KEY_INICIO_CARPETA, // Obtener valor actual de config en memoria
//                     ConfigurationManager.getDefault(ConfigurationManager.KEY_INICIO_CARPETA,""))); // Fallback al default de defaults
//
//             String ultimaImagen = model.getSelectedImageKey();
//             estadoFinalAGuardar.put("inicio.imagen",
//                 ultimaImagen != null ? ultimaImagen : ConfigurationManager.getDefault("inicio.imagen",""));
//             System.out.println("      - inicio.imagen (desde modelo): " + (ultimaImagen != null ? ultimaImagen : "(ninguna)"));
//
//
//             // --- ESTADOS DE COMPORTAMIENTO PRINCIPALES (Desde el Modelo o Actions) ---
//
//             // Comportamiento: Cargar Subcarpetas (Tomado del estado de la Action, que debería reflejar el Modelo)
//             Action toggleSubfoldersAct = actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
//             if (toggleSubfoldersAct != null) {
//                 estadoFinalAGuardar.put("comportamiento.carpeta.cargarSubcarpetas",
//                     String.valueOf(Boolean.TRUE.equals(toggleSubfoldersAct.getValue(Action.SELECTED_KEY))));
//             } else { // Fallback al modelo si la action no existe
//                 estadoFinalAGuardar.put("comportamiento.carpeta.cargarSubcarpetas",
//                     String.valueOf(!model.isMostrarSoloCarpetaActual()));
//             }
//             System.out.println("      - comportamiento.carpeta.cargarSubcarpetas: " + estadoFinalAGuardar.get("comportamiento.carpeta.cargarSubcarpetas"));
//
//             // Comportamiento: Zoom Manual Activo (Tomado del estado de la Action)
//             Action toggleZoomManualAct = actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
//             if (toggleZoomManualAct != null) {
//                 estadoFinalAGuardar.put(ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_MANUAL_INICIAL_ACTIVO,
//                     String.valueOf(Boolean.TRUE.equals(toggleZoomManualAct.getValue(Action.SELECTED_KEY))));
//             } else { // Fallback al modelo
//                 estadoFinalAGuardar.put(ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_MANUAL_INICIAL_ACTIVO,
//                     String.valueOf(model.isZoomHabilitado()));
//             }
//             System.out.println("      - " + ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_MANUAL_INICIAL_ACTIVO + ": " +
//                                estadoFinalAGuardar.get(ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_MANUAL_INICIAL_ACTIVO));
//
//             // Comportamiento: Último Modo de Zoom Seleccionado (Tomado del Modelo, ya que AplicarModoZoomAction actualiza el modelo)
//             if (model.getCurrentZoomMode() != null) {
//                 estadoFinalAGuardar.put(ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_ULTIMO_MODO_SELECCIONADO,
//                     model.getCurrentZoomMode().name());
//             }
//             System.out.println("      - " + ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_ULTIMO_MODO_SELECCIONADO + ": " +
//                                estadoFinalAGuardar.get(ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_ULTIMO_MODO_SELECCIONADO));
//
//             // Comportamiento: Porcentaje de Zoom Personalizado
//             // Este valor es actualizado en 'this.configuration' por InfoBarManager o MouseWheelListener.
//             // Así que ya debería estar correcto en 'estadoFinalAGuardar' después del putAll(configEnMemoria).
//             System.out.println("      - " + ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_PERSONALIZADO_PORCENTAJE + ": " +
//                                estadoFinalAGuardar.get(ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_PERSONALIZADO_PORCENTAJE));
//
//             // Comportamiento: Mantener Proporciones (Tomado del estado de la Action)
//             Action toggleProporcionesAct = actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);
//             if (toggleProporcionesAct != null) {
//                 estadoFinalAGuardar.put("interfaz.menu.zoom.mantener_proporciones.seleccionado", // Clave que usa la Action
//                     String.valueOf(Boolean.TRUE.equals(toggleProporcionesAct.getValue(Action.SELECTED_KEY))));
//             } else { // Fallback al modelo
//                 estadoFinalAGuardar.put("interfaz.menu.zoom.mantener_proporciones.seleccionado",
//                     String.valueOf(model.isMantenerProporcion()));
//             }
//             System.out.println("      - interfaz.menu.zoom.mantener_proporciones.seleccionado: " +
//                                estadoFinalAGuardar.get("interfaz.menu.zoom.mantener_proporciones.seleccionado"));
//
//
//             // --- CONFIGURACIONES DEL MODELO (MINIATURAS) ---
//             estadoFinalAGuardar.put("miniaturas.cantidad.antes", String.valueOf(model.getMiniaturasAntes()));
//             estadoFinalAGuardar.put("miniaturas.cantidad.despues", String.valueOf(model.getMiniaturasDespues()));
//             estadoFinalAGuardar.put("miniaturas.tamano.seleccionada.ancho", String.valueOf(model.getMiniaturaSelAncho()));
//             estadoFinalAGuardar.put("miniaturas.tamano.seleccionada.alto", String.valueOf(model.getMiniaturaSelAlto()));
//             estadoFinalAGuardar.put("miniaturas.tamano.normal.ancho", String.valueOf(model.getMiniaturaNormAncho()));
//             estadoFinalAGuardar.put("miniaturas.tamano.normal.alto", String.valueOf(model.getMiniaturaNormAlto()));
//             // 'miniaturas.ui.mostrar_nombres' es manejado por ToggleMiniatureTextAction, que actualiza 'this.configuration',
//             // así que ya está cubierto por putAll(configEnMemoria).
//
//             // --- TEMA VISUAL ---
//             // ThemeManager actualiza 'this.configuration' con KEY_TEMA_NOMBRE cuando cambia el tema.
//             // Así que ya está cubierto por putAll(configEnMemoria).
//             System.out.println("      - " + ConfigurationManager.KEY_TEMA_NOMBRE + ": " +
//                                estadoFinalAGuardar.get(ConfigurationManager.KEY_TEMA_NOMBRE));
//
//
//             // --- ESTADOS DE CHECKBOXES/RADIOS DEL MENÚ ---
//             // Los JCheckBoxMenuItems y JRadioButtonMenuItems que tienen una ACTION asignada
//             // (ej. ToggleThemeAction, ToggleUIElementVisibilityAction, SetSubfolderReadModeAction, etc.)
//             // son responsables de llamar a `configuration.setString("su.clave.config.seleccionado", ...)`
//             // cuando su estado cambia. Por lo tanto, sus estados ya están en `configEnMemoria` y, por ende,
//             // en `estadoFinalAGuardar`.
//
//             // Para los JCheckBoxMenuItems que controlan la visibilidad de botones de la toolbar
//             // (y que son manejados por VisorController.actionPerformed):
//             // `actionPerformed` ya llama a `configuration.setString(claveDelCheckBox + ".seleccionado", ...)` y
//             // a `configuration.setString(claveDelBotonToolbar + ".visible", ...)`.
//             // Así que estos también ya están en `configEnMemoria` y en `estadoFinalAGuardar`.
//
//             // Conclusión: NO es necesario iterar sobre view.getMenuItemsPorNombre() aquí
//             // si la lógica de actualización de `this.configuration` durante la sesión es correcta.
//
//             System.out.println("    [GuardarConfig] Paso 3: Valores críticos asegurados.");
//
//         } catch (Exception e) {
//             System.err.println("  ERROR GRAVE [VisorController.guardarConfiguracionActual] al recopilar estado explícito: " + e.getMessage());
//             e.printStackTrace();
//             // Considerar no continuar con el guardado si hay un error aquí,
//             // o guardar lo que se tenga pero con una advertencia.
//         }
//
//         // --- SECCIÓN 3: GUARDAR EL MAPA RESULTANTE EN EL ARCHIVO ---
//         try {
//             System.out.println("    [GuardarConfig] Paso 4: Pasando " + estadoFinalAGuardar.size() +
//                                " claves a ConfigurationManager.guardarConfiguracion()...");
//             configuration.guardarConfiguracion(estadoFinalAGuardar); // ConfigurationManager se encarga de la escritura inteligente
//             System.out.println("  [ShutdownHook/GuardarConfig] Configuración actual procesada por ConfigurationManager.");
//         } catch (IOException e) {
//             System.err.println("### ERROR FATAL AL GUARDAR CONFIGURACIÓN (IOException): " + e.getMessage() + " ###");
//             e.printStackTrace();
//         } catch (Exception e) {
//             System.err.println("### ERROR INESPERADO AL GUARDAR CONFIGURACIÓN: " + e.getMessage() + " ###");
//             e.printStackTrace();
//         }
//         System.out.println("  [ShutdownHook/GuardarConfig] Fin de guardarConfiguracionActual.");
//     }//---FIN del metodo guardarConfiguracionActual nuevo (mejor segun ia)
     
     
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
     private void guardarConfiguracionActual() { //WENO segun yo
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
             
             // 3.3. Botones Toggle subcarpetas y partes de zoom
             // 3.3.1. Estado de Modo Subcarpetas
             estadoActualParaGuardar.put("comportamiento.carpeta.cargarSubcarpetas", String.valueOf(!model.isMostrarSoloCarpetaActual()));
             
             // 3.3.2. Estado de Zoom Manual activado 
             if (this.model != null) {
                 estadoActualParaGuardar.put(
                     ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_MANUAL_INICIAL_ACTIVO,
                     String.valueOf(this.model.isZoomHabilitado()) // Guardar el estado real del modelo
                 );
                 System.out.println("    -> Modelo: Guardando estado de zoomHabilitado: " + this.model.isZoomHabilitado() + 
                                    " en clave: " + ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_MANUAL_INICIAL_ACTIVO);
             }
             
             // 3.3.3. Estado de Tipo de Zoom activo
             if (this.model != null && this.model.getCurrentZoomMode() != null) {
                 estadoActualParaGuardar.put(
                     ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_ULTIMO_MODO_SELECCIONADO,
                     this.model.getCurrentZoomMode().name() // Guardar el nombre del enum
                 );
                 System.out.println("    -> Modelo: Guardando ultimo_modo_seleccionado: " + this.model.getCurrentZoomMode().name());
             }
             
             
          // Estado de 'navegacionCircular'
             // La Action ToggleNavegacionCircularAction llama a configuration.setString(),
             // así que el valor en configEnMemoria (leído con configuration.getConfigMap()) ya debería ser el correcto.
             // No necesitamos tomarlo de la Action.SELECTED_KEY ni del modelo aquí, porque la Action ya lo guardó en 'this.configuration'.
             // Solo nos aseguramos de que la clave esté.
             estadoActualParaGuardar.putIfAbsent("comportamiento.navegacion.circular",
                                        String.valueOf(model.isNavegacionCircularActivada())); // Default del modelo si no estaba
             System.out.println("      - comportamiento.navegacion.circular (desde config en memoria): " + estadoActualParaGuardar.get("comportamiento.navegacion.circular"));
             
             // 3.4. Configuración de miniaturas (si se pudieran cambiar)
             estadoActualParaGuardar.put("miniaturas.cantidad.antes", String.valueOf(model.getMiniaturasAntes()));
             estadoActualParaGuardar.put("miniaturas.cantidad.despues", String.valueOf(model.getMiniaturasDespues()));
             estadoActualParaGuardar.put("miniaturas.tamano.seleccionada.ancho", String.valueOf(model.getMiniaturaSelAncho()));
             estadoActualParaGuardar.put("miniaturas.tamano.seleccionada.alto", String.valueOf(model.getMiniaturaSelAlto()));
             estadoActualParaGuardar.put("miniaturas.tamano.normal.ancho", String.valueOf(model.getMiniaturaNormAncho()));
             estadoActualParaGuardar.put("miniaturas.tamano.normal.alto", String.valueOf(model.getMiniaturaNormAlto()));
             
             // 3.5. Estado de toggles (leer desde la Action asociada es más fiable que desde el modelo)
             Action toggleZoomManualAction = (this.actionMap != null) ? this.actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE) : null;
             if (toggleZoomManualAction != null) estadoActualParaGuardar.put("interfaz.menu.zoom.activar_zoom_manual.seleccionado", 
	                     String.valueOf(Boolean.TRUE.equals(toggleZoomManualAction.getValue(Action.SELECTED_KEY))));
	         Action toggleProporcionesAction = (this.actionMap != null) ? this.actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES) : null;
	         if (toggleProporcionesAction != null) estadoActualParaGuardar.put("interfaz.menu.zoom.mantener_proporciones.seleccionado", 
	        		 String.valueOf(Boolean.TRUE.equals(toggleProporcionesAction.getValue(Action.SELECTED_KEY))));
              
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
                    	 
                    	 //FIXME desmarcar estas lineas para guardar activado y visible de las opciones del menu
//                         estadoActualParaGuardar.put(claveLarga + ".activado", String.valueOf(item.isEnabled()));
//                         estadoActualParaGuardar.put(claveLarga + ".visible", String.valueOf(item.isVisible()));
                         
                         // Guardar estado seleccionado para checkboxes y radios
                         if (item instanceof JCheckBoxMenuItem) estadoActualParaGuardar.put(claveLarga + ".seleccionado", String.valueOf(((JCheckBoxMenuItem) item).isSelected()));
                         else if (item instanceof JRadioButtonMenuItem) estadoActualParaGuardar.put(claveLarga + ".seleccionado", String.valueOf(((JRadioButtonMenuItem) item).isSelected()));
                     }
                 });
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

     } // --- FIN guardarConfiguracionActual WENO segun yo---
     
     
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
         if (model == null || model.getModeloLista() == null || view == null || view.getListaMiniaturas() == null || listCoordinator == null) {
             System.err.println("WARN [actualizarMiniaturas]: Dependencias nulas (Modelo, Vista, ListaMiniaturas o Coordinator). Abortando.");
             return;
         }

         // 1.3. Obtener el modelo principal de datos y su tamaño.
         final DefaultListModel<String> modeloPrincipal = model.getModeloLista(); // 'final' para acceso en lambda
         final int totalPrincipal = modeloPrincipal.getSize();                     // 'final' para acceso en lambda
         System.out.println("  [actualizarMiniaturas] Tamaño modeloPrincipal: " + totalPrincipal);

         // 1.4. Manejar caso de lista principal vacía o índice principal inválido.
         //      Si no hay datos o el índice no es válido, se programa una limpieza de la UI de miniaturas y se sale.
         if (totalPrincipal == 0 || indiceSeleccionadoPrincipal < 0 || indiceSeleccionadoPrincipal >= totalPrincipal) {
             System.out.println("  [actualizarMiniaturas] Índice principal inválido o lista principal vacía. Limpiando UI de miniaturas y saliendo.");

             SwingUtilities.invokeLater(() -> {
                 if (view != null && view.getListaMiniaturas() != null && listCoordinator != null) {
                     listCoordinator.setSincronizandoUI(true); // Proteger la UI
                     try {
                         JList<String> lMini = view.getListaMiniaturas();
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
             if (view == null || view.getListaMiniaturas() == null || listCoordinator == null || model == null) {
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
                 JList<String> listaMiniaturasEnVista = view.getListaMiniaturas();

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
         // --- 1. OBTENER LÍMITES SUPERIORES DE CONFIGURACIÓN/MODELO ---
         //    Estos son los valores máximos de 'antes' y 'después' que el usuario ha configurado
         //    o los defaults de la aplicación.
         int cfgMiniaturasAntes, cfgMiniaturasDespues;
         if (model != null) {
             cfgMiniaturasAntes = model.getMiniaturasAntes();
             cfgMiniaturasDespues = model.getMiniaturasDespues();
         } else if (configuration != null) {
             cfgMiniaturasAntes = configuration.getInt("miniaturas.cantidad.antes", DEFAULT_MINIATURAS_ANTES_FALLBACK);
             cfgMiniaturasDespues = configuration.getInt("miniaturas.cantidad.despues", DEFAULT_MINIATURAS_DESPUES_FALLBACK);
             System.out.println("  [CalcularMiniaturas] WARN: Modelo nulo, usando valores de config/fallback para antes/después.");
         } else {
             cfgMiniaturasAntes = DEFAULT_MINIATURAS_ANTES_FALLBACK;
             cfgMiniaturasDespues = DEFAULT_MINIATURAS_DESPUES_FALLBACK;
             System.err.println("  [CalcularMiniaturas] ERROR: Modelo y Config nulos, usando hardcoded fallbacks.");
         }

         // --- 2. VALIDAR DISPONIBILIDAD DE COMPONENTES DE LA VISTA ---
         //    Si la vista o sus componentes clave no están listos, no podemos calcular dinámicamente.
         if (view == null || view.getScrollListaMiniaturas() == null || view.getListaMiniaturas() == null) {
             System.out.println("  [CalcularMiniaturas] WARN: Vista o componentes de miniaturas nulos. Devolviendo máximos configurados.");
             return new RangoMiniaturasCalculado(cfgMiniaturasAntes, cfgMiniaturasDespues);
         }

         // --- 3. OBTENER DIMENSIONES ACTUALES DE LA UI ---
         JScrollPane scrollPane = view.getScrollListaMiniaturas();
         JList<String> listaMin = view.getListaMiniaturas(); // Referencia a la JList de miniaturas

         // Es crucial obtener el ancho del viewport del JScrollPane, ya que es el área visible.
         int viewportWidth = scrollPane.getViewport().getWidth();
         // El ancho de cada celda, que debería estar fijado en la JList.
         int cellWidth = listaMin.getFixedCellWidth();

         // Log de depuración para ver el estado de las dimensiones y visibilidad
         System.out.println("  [CalcularMiniaturas DEBUG] ViewportWidth: " + viewportWidth +
                            ", ViewportHeight: " + scrollPane.getViewport().getHeight() +
                            ", CellWidth: " + cellWidth +
                            ", ScrollPane Showing: " + scrollPane.isShowing() +
                            ", ListaMiniaturas Showing: " + listaMin.isShowing());

         // --- 4. LÓGICA DE FALLBACK MEJORADA ---
         //    Si el viewport aún no tiene un ancho válido (ej. 0, o el JScrollPane no se muestra aún),
         //    o si el ancho de celda no es válido, no podemos calcular de forma fiable.
         //    En este caso, devolvemos los máximos configurados para intentar mostrar "algo"
         //    y confiamos en que una futura llamada (ej. por redimensionamiento) lo corrija.
         if (viewportWidth <= 0 || cellWidth <= 0 || !scrollPane.isShowing()) {
             System.out.println("  [CalcularMiniaturas] WARN: Viewport/Cell inválido o ScrollPane no visible. Usando MÁXIMOS configurados como fallback.");
             return new RangoMiniaturasCalculado(cfgMiniaturasAntes, cfgMiniaturasDespues);
         }

         // --- 5. CALCULAR EL NÚMERO TOTAL DE MINIATURAS QUE CABEN ---
         int totalMiniaturasQueCaben = viewportWidth / cellWidth;
         // No es necesario loguear de nuevo, el DEBUG ya lo hace.

         int numAntesCalculado;
         int numDespuesCalculado;

         // --- 6. DISTRIBUIR EL ESPACIO DISPONIBLE, RESPETANDO LOS MÁXIMOS CONFIGURADOS ---
         int maxTotalConfigurado = cfgMiniaturasAntes + 1 + cfgMiniaturasDespues; // +1 por la miniatura central

         if (totalMiniaturasQueCaben >= maxTotalConfigurado) {
             // 6.1. Si caben todas las miniaturas configuradas (o más), usamos los máximos configurados.
             numAntesCalculado = cfgMiniaturasAntes;
             numDespuesCalculado = cfgMiniaturasDespues;
         } else if (totalMiniaturasQueCaben <= 1) {
             // 6.2. Si solo cabe una miniatura o ninguna (ej. viewport muy estrecho).
             //      Mostramos solo la central (0 antes, 0 después).
             numAntesCalculado = 0;
             numDespuesCalculado = 0;
         } else {
             // 6.3. Si caben algunas, pero no todas las configuradas.
             //      Distribuimos las miniaturas laterales disponibles.
             int miniaturasLateralesDisponibles = totalMiniaturasQueCaben - 1; // Espacio para las de antes y después

             // Intentar mantener la proporción de 'antes' vs 'después' de la configuración.
             double ratioAntesOriginal = 0.5; // Default: distribuir equitativamente
             if ((cfgMiniaturasAntes + cfgMiniaturasDespues) > 0) { // Evitar división por cero
                 ratioAntesOriginal = (double) cfgMiniaturasAntes / (cfgMiniaturasAntes + cfgMiniaturasDespues);
             }

             numAntesCalculado = (int) Math.round(miniaturasLateralesDisponibles * ratioAntesOriginal);
             numDespuesCalculado = miniaturasLateralesDisponibles - numAntesCalculado;

             // Como chequeo final, nos aseguramos de no exceder los máximos individuales configurados
             // (aunque la lógica con totalMiniaturasQueCaben ya debería haber manejado esto implícitamente).
             numAntesCalculado = Math.min(numAntesCalculado, cfgMiniaturasAntes);
             numDespuesCalculado = Math.min(numDespuesCalculado, cfgMiniaturasDespues);
         }

         // --- 7. DEVOLVER EL RESULTADO CALCULADO ---
         System.out.println("  [CalcularMiniaturas] Rango dinámico calculado -> Antes: " + numAntesCalculado + ", Despues: " + numDespuesCalculado);
         return new RangoMiniaturasCalculado(numAntesCalculado, numDespuesCalculado);
     }
     
     
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
	  
	  
	public void actualizarEstadoVisualBotonMarcarYBarraEstado (boolean estaMarcada)
	{

		if (view == null || model == null)
			return;

		// 1. Actualizar el aspecto del botón toggle en la toolbar
	    Action toggleMarkImageAction = (this.actionMap != null) ? this.actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA) : null;
		if (toggleMarkImageAction != null)
		{ // toggleMarkImageAction es el campo de la Action en VisorController
			// Asegurarse que el SELECTED_KEY de la action está sincronizado (aunque ya
			// debería estarlo
			// si la llamada vino de la propia action).
			toggleMarkImageAction.putValue(Action.SELECTED_KEY, estaMarcada);
			this.view.actualizarAspectoBotonToggle(toggleMarkImageAction, estaMarcada);
		}

		// 2. Actualizar la barra de estado
		String rutaActual = model.getRutaCompleta(model.getSelectedImageKey()) != null
				? model.getRutaCompleta(model.getSelectedImageKey()).toString()
				: (model.getSelectedImageKey() != null ? model.getSelectedImageKey() : "");

		if (estaMarcada)
		{
			view.setTextoBarraEstadoRuta(rutaActual + " [MARCADA]");
		} else
		{
			view.setTextoBarraEstadoRuta(rutaActual);
		}
		System.out.println("  [Controller] Barra de estado y botón 'Marcar' actualizados. Marcada: " + estaMarcada);

	}

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

	// En VisorController.java
	public void actualizarEstadoVisualBotonMarcarYBarraEstado (boolean estaMarcada, Path rutaParaBarraEstado)
	{

		if (view == null)
			return;
	    Action toggleMarkImageAction = (this.actionMap != null) ? this.actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA) : null;
		if (toggleMarkImageAction != null)
		{

			// Reafirmar el SELECTED_KEY por si la llamada vino de otro sitio que no sea la
			// propia action
			if (!Objects.equals(toggleMarkImageAction.getValue(Action.SELECTED_KEY), estaMarcada))
			{
				toggleMarkImageAction.putValue(Action.SELECTED_KEY, estaMarcada);
			}
			this.view.actualizarAspectoBotonToggle(toggleMarkImageAction, estaMarcada);
		}

		String textoRuta = "";

		if (rutaParaBarraEstado != null)
		{
			textoRuta = rutaParaBarraEstado.toString();
		} else if (model != null && model.getSelectedImageKey() != null)
		{ // Fallback si no se pasó ruta
			Path p = model.getRutaCompleta(model.getSelectedImageKey());
			if (p != null)
				textoRuta = p.toString();
			else
				textoRuta = model.getSelectedImageKey();
		}

		if (estaMarcada)
		{
			view.setTextoBarraEstadoRuta(textoRuta + " [MARCADA]");
		} else
		{
			view.setTextoBarraEstadoRuta(textoRuta);
		}

		// System.out.println(" [Controller] Barra de estado y botón 'Marcar'
		// actualizados. Marcada: " + estaMarcada);
	}	  
	
	

	
	  
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
        if (view != null && view.getListaMiniaturas() != null) {
            System.out.println("  [Controller] Solicitando repintado de listaMiniaturas.");
            view.getListaMiniaturas().repaint();

            // Si ocultar/mostrar nombres cambia la ALTURA de las celdas,
            // podrías necesitar más que un simple repaint().
            // Por ahora, asumamos que la altura de la celda es fija y solo cambia
            // la visibilidad del JLabel del nombre.
            // Si la altura cambia, necesitarías:
            // 1. Que MiniaturaListCellRenderer devuelva una nueva PreferredSize.
            // 2. Invalidar el layout de la JList:
            //    view.getListaMiniaturas().revalidate();
            //    view.getListaMiniaturas().repaint();
            // 3. Posiblemente recalcular el número de miniaturas visibles si la altura de celda cambió.
            //    Esto haría que el `ComponentListener` de redimensionamiento sea más complejo
            //    o que necesites llamar a actualizarModeloYVistaMiniaturas aquí también.
            // ¡POR AHORA, MANTENGAMOSLO SIMPLE CON SOLO REPAINT!
        }
    } // --- FIN metodo solicitarRefrescoRenderersMiniaturas 
    
    
    /**
     * Establece si se deben mostrar los nombres de archivo debajo de las miniaturas
     * en la barra de miniaturas.
     * Esta acción actualiza la configuración persistente y luego refresca el
     * renderer de la JList de miniaturas para que el cambio visual sea inmediato.
     *
     * @param mostrar El nuevo estado deseado: true para mostrar nombres, false para ocultarlos.
     */
    public void setMostrarNombresMiniaturas(boolean mostrar) {
        System.out.println("[VisorController] Solicitud para cambiar 'Mostrar Nombres en Miniaturas' a: " + mostrar);

        // --- 1. VALIDACIÓN DE DEPENDENCIAS ESENCIALES ---
        // Necesitamos ConfigurationManager para guardar el estado, y varios componentes
        // (view, model, servicioMiniaturas, uiConfigForView que contiene IconUtils)
        // para poder recrear y aplicar el renderer.
        if (configuration == null) {
            System.err.println("ERROR CRÍTICO [setMostrarNombresMiniaturas]: ConfigurationManager es null. No se puede guardar el estado.");
            // Podrías decidir si continuar sin guardar o retornar. Por ahora, solo logueamos.
        }
        if (view == null || view.getListaMiniaturas() == null) {
            System.err.println("ERROR CRÍTICO [setMostrarNombresMiniaturas]: VisorView o su listaMiniaturas son null. No se puede actualizar el renderer.");
            return; // No se puede hacer nada sin la vista/lista.
        }
        if (this.model == null) {
            System.err.println("ERROR CRÍTICO [setMostrarNombresMiniaturas]: VisorModel (this.model) es null.");
            return;
        }
        if (this.servicioMiniaturas == null) {
            System.err.println("ERROR CRÍTICO [setMostrarNombresMiniaturas]: ThumbnailService (this.servicioMiniaturas) es null.");
            return;
        }
        if (this.uiConfigForView == null) { // uiConfigForView es inyectado por AppInitializer
            System.err.println("ERROR CRÍTICO [setMostrarNombresMiniaturas]: ViewUIConfig (this.uiConfigForView) es null.");
            return;
        }
        if (this.uiConfigForView.configurationManager == null) { // El ConfigurationManager dentro de uiConfigForView
            System.err.println("ERROR CRÍTICO [setMostrarNombresMiniaturas]: uiConfigForView.configurationManager es null.");
            return;
        }
        if (this.uiConfigForView.iconUtils == null) {
            System.err.println("WARN [setMostrarNombresMiniaturas]: uiConfigForView.iconUtils es null. " +
                               "El renderer de miniaturas podría no mostrar iconos de error correctamente.");
            // Continuamos, pero es una advertencia.
        }

        // --- 2. ACTUALIZAR LA CONFIGURACIÓN PERSISTENTE ---
        if (configuration != null) {
            configuration.setString("ui.miniaturas.mostrar_nombres", String.valueOf(mostrar));
            System.out.println("  -> Configuración 'ui.miniaturas.mostrar_nombres' actualizada en memoria a: " + mostrar);
            // La configuración se guardará al archivo al cerrar la aplicación (vía ShutdownHook).
        }

        // --- 3. RECREAR Y APLICAR EL RENDERER DE MINIATURAS EN LA VISTA ---
        //    Esto debe hacerse para que el cambio visual sea inmediato.
        System.out.println("  -> Preparando para recrear y asignar nuevo MiniaturaListCellRenderer...");

        // 3.1. Obtener las dimensiones y colores actuales para el renderer.
        //      Estos vienen de uiConfigForView, que debería tener los valores más recientes del tema y configuración.
        int thumbWidth = this.uiConfigForView.configurationManager.getInt("miniaturas.tamano.normal.ancho", 40);
        int thumbHeight = this.uiConfigForView.configurationManager.getInt("miniaturas.tamano.normal.alto", 40);

        Color colorFondoMiniatura = this.uiConfigForView.colorFondoSecundario;
        Color colorFondoSeleccionMiniatura = this.uiConfigForView.colorSeleccionFondo;
        Color colorTextoMiniatura = this.uiConfigForView.colorTextoPrimario;
        Color colorTextoSeleccionMiniatura = this.uiConfigForView.colorSeleccionTexto;
        Color colorBordeSeleccionMiniatura = this.uiConfigForView.colorBordeSeleccionActiva; // O tu fallback

        System.out.println("    Valores para nuevo renderer: MostrarNombres=" + mostrar +
                           ", AnchoThumb=" + thumbWidth + ", AltoThumb=" + thumbHeight);

        // 3.2. Crear la nueva instancia del renderer, pasando el nuevo estado 'mostrar'
        //      y la referencia a IconUtils.
        MiniaturaListCellRenderer newRenderer = new MiniaturaListCellRenderer(
            this.servicioMiniaturas,
            this.model,
            thumbWidth,
            thumbHeight,
            mostrar, // <<< El nuevo estado para mostrar/ocultar nombres
            colorFondoMiniatura,
            colorFondoSeleccionMiniatura,
            colorTextoMiniatura,
            colorTextoSeleccionMiniatura,
            colorBordeSeleccionMiniatura,
            this.uiConfigForView.iconUtils // <<< Pasar la instancia de IconUtils
        );
        System.out.println("    -> Nueva instancia de MiniaturaListCellRenderer creada.");

        // 3.3. Asignar el nuevo renderer a la JList de miniaturas en la Vista.
        //      Y actualizar las dimensiones fijas de las celdas.
        //      Es importante hacerlo en el EDT.
        final MiniaturaListCellRenderer finalRenderer = newRenderer; // Para usar en lambda
        SwingUtilities.invokeLater(() -> {
            if (view != null && view.getListaMiniaturas() != null) {
                JList<String> listaMin = view.getListaMiniaturas();
                listaMin.setCellRenderer(finalRenderer);
                System.out.println("      [EDT] Nuevo renderer asignado a listaMiniaturas.");

                int nuevaAlturaCelda = finalRenderer.getAlturaCalculadaDeCelda();
                int nuevoAnchoCelda = finalRenderer.getAnchoCalculadaDeCelda();
                listaMin.setFixedCellHeight(nuevaAlturaCelda);
                listaMin.setFixedCellWidth(nuevoAnchoCelda);
                System.out.println("      [EDT] AlturaFijaCelda: " + nuevaAlturaCelda + ", AnchoFijoCelda: " + nuevoAnchoCelda);

                listaMin.revalidate(); // Importante si el tamaño de celda cambió (puede cambiar si se ocultan nombres)
                listaMin.repaint();
                System.out.println("      [EDT] listaMiniaturas revalidada y repintada.");

                // Opcional: Si el cambio de altura/ancho de celda afecta significativamente
                // el layout de la ventana de miniaturas, podrías necesitar recalcularla.
                if (listCoordinator != null) {
                    int indiceActual = listCoordinator.getIndiceOficialSeleccionado();
                    if (indiceActual != -1) {
                         // System.out.println("      [EDT] Solicitando actualización del modelo y vista de miniaturas al ListCoordinator.");
                         // controller.actualizarModeloYVistaMiniaturas(indiceActual); // 'controller' es 'this' aquí
                         this.actualizarModeloYVistaMiniaturas(indiceActual);
                    }
                }
            } else {
                System.err.println("ERROR [setMostrarNombresMiniaturas EDT]: Vista o listaMiniaturas son null al intentar aplicar renderer.");
            }
        });

        System.out.println("[VisorController] setMostrarNombresMiniaturas completado. Cambio visual programado en EDT.");
    }

	
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

    
	 public void setInfoBarManager (InfoBarManager infoBarManager)
	 {
		this.infoBarManager = infoBarManager;
	 }     
    
    

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
    
    
 // En VisorController.java
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
        if (toggleAction != null) {
        	this.view.actualizarAspectoBotonToggle(toggleAction, estadoActualIncluirSubcarpetas);
        }
    }
    
    
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

	         boolean modoDeZoomCambiadoEnManager = this.zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
	     
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
	         this.zoomManager.refrescarVistaPrincipalConEstadoActualDelModelo();
	     } else {
	         System.err.println("ERROR [setMantenerProporcionesLogicaYUi]: ZoomManager es null al intentar refrescar.");
	     }
	     
	     //LOG VisorController DEBUG
//	     System.out.println("  [VisorController DEBUG] Estado del MODELO ANTES DE REFRESCAR ZOOM: model.isMantenerProporcion()=" + model.isMantenerProporcion());
	
	   // <--- ACTUALIZAR BARRAS --->  
	     if (infoBarManager != null) {
	    	    infoBarManager.actualizarBarrasDeInfo();
	    	}	     
	     
	     System.out.println("[VisorController setMantenerProporcionesLogicaYUi] Proceso completado.");
	 }
    
	 
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
	    this.cargarListaImagenes(claveAntesDelCambio);

	    System.out.println("[VisorController setMostrarSubcarpetasLogicaYUi] Proceso completado.");
	}// FIN metodo setMostrarSubcarpetasLogicaYUi
	
    
    /**
     * Sincroniza el estado SELECTED_KEY de la ToggleProporcionesAction
     * y la apariencia del botón de la toolbar asociado.
     * @param estadoActualMantenerProporciones El estado actual de 'mantenerProporciones' según el modelo.
     */
    private void sincronizarUiControlesProporciones(boolean estadoActualMantenerProporciones) {
        System.out.println("  [VisorController sincronizarUiControlesProporciones] Sincronizando UI con estado: " + estadoActualMantenerProporciones);
        if (actionMap == null || view == null) return;

        Action action = actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);
        if (action instanceof ToggleProporcionesAction) {
            ((ToggleProporcionesAction) action).sincronizarSelectedKeyConModelo(estadoActualMantenerProporciones);
        } else if (action != null) {
            // Fallback si no es la clase esperada pero existe, actualizar su SELECTED_KEY genéricamente
            if (!Objects.equals(action.getValue(Action.SELECTED_KEY), estadoActualMantenerProporciones)) {
                action.putValue(Action.SELECTED_KEY, estadoActualMantenerProporciones);
            }
        }

        // Actualizar el aspecto visual del botón de la toolbar
        // Asumiendo que VisorView.actualizarAspectoBotonToggle busca el botón por la Action.
        if (action != null) {
            view.actualizarAspectoBotonToggle(action, estadoActualMantenerProporciones);
        }
    }
    
    
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
            
            view.actualizarAspectoBotonToggle(toggleGeneralAction, estadoModeloIncluirSubcarpetas);
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
        if (this.actionMap == null || this.model == null || this.view == null) {
            System.err.println("WARN [sincronizarEstadoVisualBotonesYRadiosZoom]: actionMap, model o view nulos.");
            return;
        }
        System.out.println("[VisorController] Sincronizando estado visual de botones/radios de Zoom con modo modelo: " + model.getCurrentZoomMode());

        // --- GUARDAR EL MODO ACTUAL EN LA CONFIGURACIÓN ---
        if (configuration != null && model.getCurrentZoomMode() != null) {
            configuration.setString(
                ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_ULTIMO_MODO_SELECCIONADO,
                model.getCurrentZoomMode().name()
            );
            System.out.println("  -> Config '" + ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_ULTIMO_MODO_SELECCIONADO + "' actualizada a: " + model.getCurrentZoomMode().name());
        } else {
            if (configuration == null) System.err.println("WARN [sincronizar...Zoom]: ConfigurationManager es null. No se guardó último modo zoom.");
            if (model.getCurrentZoomMode() == null) System.err.println("WARN [sincronizar...Zoom]: model.getCurrentZoomMode() es null. No se guardó último modo zoom.");
        }
        // --- FIN DE GUARDAR EN CONFIGURACIÓN ---

        for (Action action : this.actionMap.values()) {
            if (action instanceof AplicarModoZoomAction) {
                AplicarModoZoomAction zoomAction = (AplicarModoZoomAction) action;
                zoomAction.sincronizarEstadoSeleccionConModelo();
                
             // Actualizar el aspecto visual del botón de la toolbar asociado a esta Action
                this.view.actualizarAspectoBotonToggle(zoomAction, Boolean.TRUE.equals(zoomAction.getValue(Action.SELECTED_KEY)));
            }
        }
        
     // Actualizar las barras de información después de cualquier cambio de zoom
        if (infoBarManager != null) {
            infoBarManager.actualizarBarrasDeInfo();
        }
        System.out.println("[VisorController] Sincronización de botones/radios de zoom finalizada.");
    }
    
    
//    public void sincronizarEstadoVisualBotonesYRadiosZoom() {
//        if (this.actionMap == null || this.model == null || this.view == null) {
//            System.err.println("WARN [sincronizarEstadoVisualBotonesYRadiosZoom]: actionMap, model o view nulos.");
//            return;
//        }
//        System.out.println("[VisorController] Sincronizando estado visual de botones/radios de Zoom con modo modelo: " + model.getCurrentZoomMode());
//
//        for (Action action : this.actionMap.values()) {
//            if (action instanceof AplicarModoZoomAction) {
//                AplicarModoZoomAction zoomAction = (AplicarModoZoomAction) action;
//                zoomAction.sincronizarEstadoSeleccionConModelo(); // Esto actualiza el SELECTED_KEY de la Action (y por ende del JRadioButtonMenuItem)
//                
//                // Ahora, actualizar el aspecto del botón de la toolbar asociado a esta zoomAction
//                this.view.actualizarAspectoBotonToggle(zoomAction, Boolean.TRUE.equals(zoomAction.getValue(Action.SELECTED_KEY)));
//            }
//        }
//        
//     // << --- ACTUALIZAR BARRAS AL FINAL DE LA LIMPIEZA --- >>
//        if (infoBarManager != null) {
//            infoBarManager.actualizarBarrasDeInfo();
//        }
//        
//        System.out.println("[Controller] Limpieza de UI y Modelo completada.");
//        
//    } // FIN del metodo sincronizarEstadoVisualBotonesYRadiosZoom
    
    
    public void notificarCambioEstadoZoomManual() {
        System.out.println("[VisorController] Notificado cambio de estado de zoom manual. Actualizando barras...");
        
     // << --- ACTUALIZAR BARRAS AL FINAL DE LA LIMPIEZA --- >>  
        if (infoBarManager != null) {
            infoBarManager.actualizarBarrasDeInfo();
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

    // El método solicitarActualizacionInterfaz ya lo tienes bien.
    // public void solicitarActualizacionInterfaz(String uiElementIdentifier, String configKey, boolean nuevoEstadoVisible) { ... }


    // --- En AppInitializer, o en un método de inicialización DENTRO de VisorController que AppInitializer llame:
    //     Después de que todas las Actions se han creado y el actionMap está poblado.
    /*package-private*/ void sincronizarEstadoVisualInicialDeRadiosDeFormato() {
        sincronizarAccionesFormatoBarraSuperior();
        sincronizarAccionesFormatoBarraInferior();
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


   
     
// *************************************************************************** CLASE ANIDADA DE CONTROL DE MINIATURAS VISIBLES
// ***************************************************************************************************************************    
    
} // --- FIN CLASE VisorController ---




