package controlador;

import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.actions.filtro.SetFilterTypeAction;
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import controlador.interfaces.IModoController;
import controlador.managers.CarouselManager;
import controlador.managers.ConfigApplicationManager;
import controlador.managers.DisplayModeManager;
import controlador.managers.FilterManager;
import controlador.managers.FolderNavigationManager;
import controlador.managers.InfobarStatusManager;
import controlador.managers.ToolbarManager;
import controlador.managers.ViewManager;
import controlador.managers.filter.FilterCriterion;
import controlador.managers.filter.FilterCriterion.FilterSource;
import controlador.managers.filter.FilterCriterion.FilterType;
import controlador.managers.tree.FolderTreeManager;
import controlador.utils.ComponentRegistry;
import modelo.ListContext;
import modelo.VisorModel;
import modelo.VisorModel.DisplayMode;
import modelo.VisorModel.WorkMode;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.components.Direction;
import vista.panels.ImageDisplayPanel; 

/**
 * Controlador de aplicación de alto nivel.
 * Orquesta la interacción entre los controladores de modo (VisorController, ProjectController)
 * y gestiona el estado global de la aplicación, como el modo de trabajo actual y la
 * habilitación/deshabilitación de la UI correspondiente.
 */
public class GeneralController implements IModoController, KeyEventDispatcher, PropertyChangeListener, modelo.MasterListChangeListener, servicios.ProjectStateListener {

	private static final Logger logger = LoggerFactory.getLogger(GeneralController.class);
	
    // --- Dependencias Clave ---
    private VisorModel model;
    private VisorController visorController;
    private ProjectController projectController;
    private ViewManager viewManager;
    private InfobarStatusManager statusBarManager;
    private ConfigApplicationManager configAppManager;
    private ToolbarManager toolbarManager;
    private ComponentRegistry registry; 
    private DisplayModeManager displayModeManager; 
    private ConfigurationManager configuration;
    private FolderNavigationManager folderNavManager;
    private FolderTreeManager folderTreeManager;
    private FilterManager filterManager;
    
    //Variables de filtros
//    private DefaultListModel<String> contextoRealGuardado_modelo;
//    private Map<String, Path> contextoRealGuardado_mapaRutas;
//    private String contextoRealGuardado_punteroKey;
//    private boolean filtroPersistenteActivo = false;
    
    
 // Checkpoint para el FILTRO PERSISTENTE ---
    // Guardará la lista original cargada desde el disco, nuestra fuente de verdad.
    private DefaultListModel<String> persistente_listaMaestraOriginal;
    private String persistente_punteroOriginalKey;
    private boolean persistente_activo = false;
    private FilterSource filtroActivoSource = FilterSource.FILENAME;
    
//    private Map<String, Path> persistente_mapaRutasOriginal;
    
    // --- Checkpoint para el TORNADO (se mantiene igual) ---
    private DefaultListModel<String> masterModelSinFiltro;
//    private Map<String, Path> masterMapSinFiltro;
    private int indiceSeleccionadoAntesDeFiltrar = -1;
    
    
    private javax.swing.border.Border sortButtonActiveBorder;
    private javax.swing.border.Border sortButtonInactiveBorder;
    
    private Map<String, Action> actionMap;
    private boolean sortBordersInitialized = false;
    private int lastMouseX, lastMouseY;
    
    private volatile boolean isChangingSubfolderMode = false;
//    private java.beans.PropertyChangeListener listSelectionListener;
    
    private javax.swing.border.Border focusedBorder;
    private javax.swing.border.Border unfocusedBorder;
    private List<javax.swing.JComponent> focusablePanels;
    private TitledBorder borderListaArchivosOriginal;
    
    /**
     * Constructor de GeneralController.
     * Las dependencias se inyectarán a través de setters después de la creación.
     */
    public GeneralController() {
        // Constructor vacío. La inicialización se delega al método initialize.
    } // --- Fin del método GeneralController (constructor) ---

    
    
    public void initialize() {
        logger.debug("[GeneralController] Inicializado.");
        
        this.focusablePanels = new ArrayList<>();
        int thickness = 2;
        this.unfocusedBorder = javax.swing.BorderFactory.createEmptyBorder(thickness, thickness, thickness, thickness);
        this.focusedBorder = javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 153, 51), thickness);

        
        // --- Paneles Globales y Contenedores Principales ---
        registerFocusablePanel("tabbedpane.izquierdo");
        registerFocusablePanel("scroll.miniaturas");
        registerFocusablePanel("panel.derecho.visor"); // Contenedor del visor de imagen/grid

        // --- Pestaña 'Lista' ---
        // Registramos solo el JList. El foco va directamente a él.
        registerFocusablePanel("panel.izquierdo.listaArchivos");
        registerFocusablePanel("list.nombresArchivo"); 
        
        registerFocusablePanel("scroll.nombresArchivo");
        
        // --- Pestaña 'Carpetas' ---
        // Registramos el JTree y su ScrollPane para capturar todo el foco del área.
        registerFocusablePanel("scroll.arbol");
        registerFocusablePanel("tree.carpetas");

        // --- Pestaña 'Filtros' ---
        // Registramos el JList y su ScrollPane.
        registerFocusablePanel("panel.izquierdo.filtros");
        registerFocusablePanel("list.filtrosActivos");
        registerFocusablePanel("scroll.filtrosActivos");
        
        // --- Componentes del Visor (Lado Derecho) ---
        // Registramos los JScrollPane que contienen los grids. El JList interno ya está cubierto por el foco del scroll.
        registerFocusablePanel("panel.display.grid.proyecto");
        
        registerFocusablePanel("scroll.grid.visualizador");
        registerFocusablePanel("scroll.grid.proyecto");

        // --- Paneles del Modo Proyecto ---
        registerFocusablePanel("tabbedpane.proyecto.herramientas");
        registerFocusablePanel("scroll.proyecto.nombres");
        registerFocusablePanel("scroll.proyecto.descartes");
        
        // Área de Exportación (jerárquico)
        registerFocusablePanel("panel.proyecto.exportacion.completo"); 
        registerFocusablePanel("scroll.tabla.exportacion");
        registerFocusablePanel("panel.exportacion.detalles");
        
        // TextBox
        registerFocusablePanel("textfield.filtro.orden");
        registerFocusablePanel("textfield.export.destino");
        registerFocusablePanel("textfield.filtro.texto");
        
        // boton del panel exportar/Mostrar detalles
        registerFocusablePanel("interfaz.boton.acciones_exportacion.export_detalles_seleccion");
        
        
        SwingUtilities.invokeLater(() -> {
            JPanel panelLista = registry.get("panel.izquierdo.listaArchivos");
            if (panelLista != null && panelLista.getBorder() instanceof TitledBorder) {
                this.borderListaArchivosOriginal = (TitledBorder) panelLista.getBorder();
                logger.debug("[FOCUS_INIT] TitledBorder original de la lista de archivos capturado con éxito.");
            } else {
                logger.warn("[FOCUS_INIT] No se pudo capturar el TitledBorder para 'panel.izquierdo.listaArchivos'.");
            }
        });
        
        
        sincronizarEstadoBotonesDeModo();
        
        SwingUtilities.invokeLater(() -> {
            javax.swing.JTextField searchField = registry.get("textfield.filtro.orden");
            JList<String> fileList = registry.get("list.nombresArchivo");

            if (searchField == null || fileList == null) {
                logger.error("[GeneralController] ¡ERROR CRÍTICO! Faltan JTextField o JList para inicializar la búsqueda/filtro.");
                return;
            }

            searchField.addActionListener(e -> { if (!model.isLiveFilterActive()) { buscarSiguienteCoincidencia(); } });
            searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { actualizarFiltro(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { actualizarFiltro(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarFiltro(); }
            });
            configurePlaceholderText(searchField);

            fileList.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return;
                
                @SuppressWarnings("unchecked") 
                JList<String> sourceList = (JList<String>) e.getSource();
                int selectedIndexInView = sourceList.getSelectedIndex();
                if (selectedIndexInView == -1) return;

                if (model.isLiveFilterActive()) {
                    String selectedValue = sourceList.getSelectedValue();
                    DefaultListModel<String> masterModel = model.getCurrentListContext().getModeloLista();
                    int realIndexInMaster = masterModel.indexOf(selectedValue);
                    
                    if (realIndexInMaster != -1 && visorController.getListCoordinator().getOfficialSelectedIndex() != realIndexInMaster) {
                       visorController.getListCoordinator().seleccionarImagenPorIndice(realIndexInMaster);
                    }
                } else {
                     if (visorController.getListCoordinator().getOfficialSelectedIndex() != selectedIndexInView) {
                        visorController.getListCoordinator().seleccionarImagenPorIndice(selectedIndexInView);
                    }
                }
            });
            
            logger.debug("[GeneralController] Listeners de búsqueda/filtro configurados correctamente.");
            
            if(registry.get("panel.exportacion.detalles") instanceof JPanel) {
                JPanel detailPanel = registry.get("panel.exportacion.detalles");
                // Recorremos los componentes del detailPanel para encontrar el JScrollPane
                for(Component comp : detailPanel.getComponents()) {
                    // El JScrollPane está dentro de un 'centerPanel'
                    if (comp instanceof JPanel) {
                         for(Component innerComp : ((JPanel)comp).getComponents()){
                            // Lo identificamos por el nombre que le pusimos
                            if (innerComp instanceof JScrollPane && "scroll.detalles.exportacion".equals(innerComp.getName())) {
                                focusablePanels.add((JScrollPane) innerComp);
                                logger.info("[FOCUS_INIT] JScrollPane de detalles de exportación registrado para foco.");
                                break; // Encontrado, salimos del bucle interno
                            }
                         }
                    }
                }
            }
            
            if (projectController != null && projectController.getProjectManager() != null) {
                projectController.getProjectManager().addProjectStateListener(this);
                logger.debug("[GeneralController] Registrado como oyente de estado del proyecto.");
            }
        });
    } // --- Fin del método initialize ---
    
    
    // --- Setters para Inyección de Dependencias ---

    public void setModel(VisorModel model) {
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en GeneralController");
    } // --- Fin del método setModel ---

    public void setVisorController(VisorController visorController) {
        this.visorController = Objects.requireNonNull(visorController, "VisorController no puede ser null en GeneralController");
    } // --- Fin del método setVisorController ---

    public void setProjectController(ProjectController projectController) {
        this.projectController = Objects.requireNonNull(projectController, "ProjectController no puede ser null en GeneralController");
    } // --- Fin del método setProjectController ---
    
    public void setViewManager(ViewManager viewManager) {
        this.viewManager = Objects.requireNonNull(viewManager, "ViewManager no puede ser null en GeneralController");
    } // --- Fin del método setViewManager ---
    
    public void setActionMap(Map<String, Action> actionMap) {
        this.actionMap = Objects.requireNonNull(actionMap, "ActionMap no puede ser null en GeneralController");
    } // --- Fin del método setActionMap ---
    
    public VisorController getVisorController() {
        return this.visorController;
    } // --- Fin del método getVisorController ---

    public ProjectController getProjectController() {
        return this.projectController;
    } // --- Fin del método getProjectController ---
    
    public void setStatusBarManager(InfobarStatusManager statusBarManager) {
        this.statusBarManager = Objects.requireNonNull(statusBarManager, "InfobarStatusManager no puede ser null en GeneralController");
    } // --- Fin del método setStatusBarManager ---
    
    public void setConfigApplicationManager(ConfigApplicationManager configAppManager) {
        this.configAppManager = Objects.requireNonNull(configAppManager, "ConfigApplicationManager no puede ser null en GeneralController");
    } // --- Fin del método setConfigApplicationManager ---
    
    public void setToolbarManager(ToolbarManager toolbarManager) {
        this.toolbarManager = Objects.requireNonNull(toolbarManager, "ToolbarManager no puede ser null en GeneralController");
    } // --- Fin del método setToolbarManager ---

    public void setRegistry(ComponentRegistry registry) { // <-- NUEVO SETTER
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null en GeneralController");
    } // --- Fin del método setRegistry ---

//****************************************************************************************** Fin Setters
    
    
    /**
     * Delega la solicitud de actualizar el título de la ventana principal al VisorController.
     * Este método se llama después de operaciones que pueden cambiar el contexto,
     * como cargar un nuevo proyecto.
     */
    public void actualizarTituloVentana() {
        if (visorController != null) {
            visorController.actualizarTituloVentana();
        }
    } // ---FIN de metodo actualizarTituloVentana---
    
    
    /**
     * Orquesta la acción de "Nuevo Proyecto".
     * Comprueba si hay cambios sin guardar antes de proceder.
     */
    public void handleNewProject() {
        if (promptToSaveChangesIfNecessary() == UserChoice.CANCEL) {
            return; // El usuario canceló, no hacer nada.
        }
        
        projectController.solicitarNuevoProyecto();
        // solicitarNuevoProyecto ya se encarga de cambiar de modo y actualizar título.
    } // ---FIN de metodo handleNewProject---

    /**
     * Orquesta la acción de "Abrir Proyecto".
     * Comprueba si hay cambios sin guardar y luego muestra el diálogo para abrir un archivo.
     */
    public void handleOpenProject() {
        if (promptToSaveChangesIfNecessary() == UserChoice.CANCEL) {
            return; // El usuario canceló.
        }
        
        // La lógica del JFileChooser ahora reside aquí, en el orquestador.
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        fileChooser.setDialogTitle("Abrir Proyecto");
        fileChooser.setCurrentDirectory(projectController.getProjectManager().getCarpetaBaseProyectos().toFile());
        javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter("Archivos de Proyecto (*.prj)", "prj");
        fileChooser.setFileFilter(filter);
        
        int result = fileChooser.showOpenDialog(visorController.getView());
        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
            Path selectedFile = fileChooser.getSelectedFile().toPath();
            projectController.solicitarAbrirProyecto(selectedFile);
        }
    } // ---FIN de metodo handleOpenProject---

    /**
     * Orquesta la acción de "Guardar Proyecto".
     * Si el proyecto es nuevo (temporal), delega a "Guardar Como".
     */
    public void handleSaveProject() {
        if (projectController.getProjectManager().getArchivoProyectoActivo() == null) {
            handleSaveProjectAs(); // Es un proyecto temporal, necesita un nombre.
        } else {
            projectController.solicitarGuardarProyecto();
            // solicitarGuardarProyecto ya actualiza el título a través del listener
        }
    } // ---FIN de metodo handleSaveProject---

    /**
     * Orquesta la acción de "Guardar Proyecto Como".
     * Siempre muestra el diálogo para elegir una nueva ubicación.
     */
    public void handleSaveProjectAs() {
        projectController.solicitarGuardarProyectoComo();
    } // ---FIN de metodo handleSaveProjectAs---
    
    /**
     * Orquesta la acción de "Eliminar Proyecto".
     * Muestra los diálogos y delega la lógica de borrado.
     */
    public void handleDeleteProject() {
        // La lógica que estaba en EliminarProyectoAction se mueve aquí.
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        fileChooser.setDialogTitle("Seleccionar Proyecto a Eliminar");
        
        controlador.managers.interfaces.IProjectManager pm = projectController.getProjectManager();
        Path dirInicial = pm.getCarpetaBaseProyectos();
        if (dirInicial != null) {
            fileChooser.setCurrentDirectory(dirInicial.toFile());
        }

        javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter("Archivos de Proyecto (*.prj)", "prj");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showDialog(visorController.getView(), "Eliminar Seleccionado");

        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
            Path archivoAEliminar = fileChooser.getSelectedFile().toPath();
            
            int confirm = javax.swing.JOptionPane.showConfirmDialog(
                visorController.getView(),
                "¿Estás ABSOLUTAMENTE SEGURO de que quieres eliminar el proyecto '" + archivoAEliminar.getFileName() + "'?\n" +
                "Esta acción es irreversible y borrará el archivo del disco.",
                "Confirmar Eliminación Permanente",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE
            );

            if (confirm == javax.swing.JOptionPane.YES_OPTION) {
                try {
                    Files.delete(archivoAEliminar);
                    logger.info("Proyecto eliminado exitosamente: {}", archivoAEliminar);
                    javax.swing.JOptionPane.showMessageDialog(visorController.getView(), "El proyecto ha sido eliminado.", "Eliminación Completada", javax.swing.JOptionPane.INFORMATION_MESSAGE);

                    // Comprobar si el proyecto eliminado era el activo
                    if (Objects.equals(pm.getArchivoProyectoActivo(), archivoAEliminar)) {
                        pm.nuevoProyecto(); 
                        cambiarModoDeTrabajo(VisorModel.WorkMode.VISUALIZADOR);
                    }
                    
                } catch (java.io.IOException ex) {
                    logger.error("Error al intentar eliminar el archivo de proyecto: " + archivoAEliminar, ex);
                    javax.swing.JOptionPane.showMessageDialog(visorController.getView(), "No se pudo eliminar el archivo del proyecto.\nError: " + ex.getMessage(), "Error de Eliminación", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    } // ---FIN de metodo handleDeleteProject---

    /**
     * Orquesta el proceso de apagado limpio de la aplicación.
     * Es llamado desde el WindowListener de la vista principal.
     */
    public void handleApplicationShutdown() {
        logger.info("--- [GeneralController] Gestionando el cierre de la aplicación ---");

        UserChoice choice = promptToSaveChangesIfNecessary();

        if (choice == UserChoice.CANCEL) {
            logger.info("  -> Cierre de la aplicación CANCELADO por el usuario.");
            return; // Abortar el cierre.
        }
        
        controlador.managers.interfaces.IProjectManager pm = projectController.getProjectManager();

        if (choice == UserChoice.DONT_SAVE && pm.hayCambiosSinGuardar()) {
            logger.info("  -> Usuario eligió no guardar. Creando sesión de recuperación...");
            Path recoveryPath = pm.guardarSesionDeRecuperacion();
            if (recoveryPath != null) {
                // Usamos la nueva clave específica para la recuperación.
                configuration.setString(ConfigKeys.PROYECTO_RECUPERACION_PENDIENTE, recoveryPath.toAbsolutePath().toString());
            }
        } else {
            // Si no hay recuperación pendiente, nos aseguramos de que la clave esté vacía.
            configuration.setString(ConfigKeys.PROYECTO_RECUPERACION_PENDIENTE, "");
        }
        
        // --- GUARDADO FINAL DE CONFIGURACIÓN ---
        logger.debug("  -> Guardando estado final de la ventana y configuración...");
        visorController.guardarEstadoVentanaEnConfig(); // Le pedimos al VisorController que guarde el estado de su ventana
        
        try {
            configuration.guardarConfiguracion(configuration.getConfig());
        } catch (java.io.IOException e) {
            logger.error("### ERROR FATAL AL GUARDAR CONFIGURACIÓN DURANTE EL CIERRE: " + e.getMessage());
        }
        
        // --- APAGADO DE SERVICIOS ---
        visorController.apagarExecutorServiceOrdenadamente();
        
        logger.info("--- Apagado limpio completado. Saliendo de la JVM. ---");
        System.exit(0);
    } // ---FIN de metodo handleApplicationShutdown---
    
    
    /**
     * Orquesta el guardado explícito del archivo de configuración.
     * Esto NO guarda el proyecto, solo las preferencias de la aplicación.
     */
    public void handleSaveConfiguration() {
        if (configuration == null) {
            logger.error("ERROR [handleSaveConfiguration]: ConfigurationManager es nulo.");
            return;
        }

        logger.debug("  -> Guardando estado de la ventana y configuración a petición del usuario...");
        
        // Guardamos el estado de la ventana por si ha cambiado
        if (visorController != null) {
            visorController.guardarEstadoVentanaEnConfig();
        }

        try {
            configuration.guardarConfiguracion(configuration.getConfig());
            logger.info("Configuración guardada exitosamente en el archivo.");
            if (statusBarManager != null) {
                statusBarManager.mostrarMensajeTemporal("Configuración guardada.", 2000);
            }
        } catch (java.io.IOException e) {
            logger.error("### ERROR AL GUARDAR CONFIGURACIÓN MANUALMENTE: " + e.getMessage());
            if (statusBarManager != null) {
                statusBarManager.mostrarMensajeTemporal("Error al guardar configuración.", 3000);
            }
        }
    } // ---FIN de metodo handleSaveConfiguration---
    
    
    /**
     * Representa las posibles elecciones del usuario en el diálogo de "guardar cambios".
     */
    private enum UserChoice {
        SAVE, DONT_SAVE, CANCEL
    }

    /**
     * MÉTODO CLAVE REUTILIZABLE. Comprueba si hay cambios sin guardar y, si los hay,
     * pregunta al usuario qué hacer.
     * @return La elección del usuario (SAVE, DONT_SAVE, CANCEL). Si no había cambios,
     *         devuelve DONT_SAVE (indicando que se puede proceder sin guardar).
     */
    private UserChoice promptToSaveChangesIfNecessary() {
    	controlador.managers.interfaces.IProjectManager pm = projectController.getProjectManager();
        if (pm == null || !pm.hayCambiosSinGuardar()) {
            return UserChoice.DONT_SAVE; // No hay cambios, se puede proceder.
        }

        // Sincronizar la UI al modelo antes de preguntar, para asegurar que se guarde el estado más reciente.
        projectController.sincronizarModeloConUI();
        projectController.sincronizarArchivosAsociadosConModelo();
        projectController.sincronizarDescripcionDesdeUI();
        
        String[] options = {"Guardar", "No Guardar", "Cancelar"};
        int result = javax.swing.JOptionPane.showOptionDialog(
            visorController.getView(),
            "El proyecto '" + pm.getNombreProyectoActivo() + "' tiene cambios sin guardar. ¿Qué deseas hacer?",
            "Cambios sin Guardar",
            javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
            javax.swing.JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[0]
        );

        switch (result) {
            case javax.swing.JOptionPane.YES_OPTION: // Guardar
                handleSaveProject();
                // Si el usuario canceló el diálogo "Guardar Como", el proyecto seguirá "sucio".
                return pm.hayCambiosSinGuardar() ? UserChoice.CANCEL : UserChoice.SAVE;
            case javax.swing.JOptionPane.NO_OPTION: // No Guardar
                return UserChoice.DONT_SAVE;
            default: // Cancelar o cerrar el diálogo
                return UserChoice.CANCEL;
        }
    } // ---FIN de metodo promptToSaveChangesIfNecessary---
    
    /**
     * Orquesta la transición entre los diferentes modos de trabajo de la aplicación.
     * Es el punto de entrada central para cambiar de vista. Contiene la lógica
     * de sincronización y confirmación para el "Carrusel Megapower".
     * @param modoDestino El modo al que se desea cambiar (VISUALIZADOR o PROYECTO).
     */
    public void cambiarModoDeTrabajo(VisorModel.WorkMode modoDestino) {
        WorkMode modoActual = this.model.getCurrentWorkMode();
        if (modoActual == modoDestino) {
            logger.trace("[GeneralController] Intento de cambiar al modo que ya está activo: {}. No se hace nada.", modoDestino);
            return;
        }

        // --- PRE-VALIDACIÓN ESPECIAL PARA MODO PROYECTO ---
        if (modoDestino == WorkMode.PROYECTO) {
            boolean hayImagenesEnProyecto = !visorController.getProjectManager().getImagenesMarcadas().isEmpty();
            
            if (!hayImagenesEnProyecto) {
                // El proyecto está vacío. Llamamos al helper que maneja la selección de archivo.
                // Si el helper devuelve 'false', significa que el usuario canceló,
                // por lo tanto, debemos abortar la transición.
                if (!manejarAperturaDeProyectoVacio()) {
                    sincronizarEstadoBotonesDeModo(); // Revertir el estado visual del botón
                    return; // Abortar la transición
                }
                // Si el helper devuelve 'true', significa que un proyecto fue cargado exitosamente.
                // El flujo de este método continuará para completar la transición al modo proyecto.
            }
        }
        // --- FIN DE LA PRE-VALIDACIÓN ---


        logger.debug("--- [GeneralController] INICIANDO TRANSICIÓN DE MODO: {} -> {} ---", modoActual, modoDestino);

        // --- LÓGICA DE SEGURIDAD Y CONFIRMACIÓN PARA SINCRONIZACIÓN (Se mantiene igual) ---
        boolean esTransicionSincronizable = (modoActual == WorkMode.VISUALIZADOR && modoDestino == WorkMode.CARROUSEL) ||
                                           (modoActual == WorkMode.CARROUSEL && modoDestino == WorkMode.VISUALIZADOR);

        if (esTransicionSincronizable && model.isSyncVisualizadorCarrusel()) {
            String titulo = "Confirmar Transición Sincronizada";
            String mensaje = modoDestino == WorkMode.CARROUSEL
                ? "<html>El modo <b>Sincronización</b> está activo.<br>Se cargará el estado del Visualizador en el Carrusel.<br><br>¿Continuar?</html>"
                : "<html>El modo <b>Sincronización</b> está activo.<br>La posición actual del Carrusel se transferirá al Visualizador.<br><br>¿Continuar?</html>";
            
            int respuesta = javax.swing.JOptionPane.showConfirmDialog(null, mensaje, titulo, javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.INFORMATION_MESSAGE);
            if (respuesta != javax.swing.JOptionPane.YES_OPTION) {
                logger.debug("--- [GeneralController] TRANSICIÓN CANCELADA por el usuario. ---");
                sincronizarEstadoBotonesDeModo();
                return;
            }
        }
        // --- FIN LÓGICA DE SEGURIDAD ---

        salirModo(modoActual);
        this.model.setCurrentWorkMode(modoDestino);
        entrarModo(modoDestino);

        logger.debug("--- [GeneralController] TRANSICIÓN DE MODO COMPLETADA a {} ---\n", modoDestino);
    } // --- Fin del método cambiarModoDeTrabajo ---

    
    /**
     * Método helper que gestiona el flujo cuando se intenta entrar en modo proyecto
     * sin un proyecto cargado. Muestra un diálogo para abrir un archivo.
     * @return {@code true} si un proyecto fue seleccionado y cargado exitosamente, 
     *         {@code false} si el usuario canceló la operación.
     */
    private boolean manejarAperturaDeProyectoVacio() {
        logger.debug("[GeneralController] Manejando apertura de proyecto vacío...");
        
        // Obtenemos una referencia a la ventana principal para centrar el diálogo
        Component parent = (visorController != null && visorController.getView() != null) ? visorController.getView() : null;

        // Mostramos el JFileChooser
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        fileChooser.setDialogTitle("Abrir Proyecto");
        fileChooser.setCurrentDirectory(projectController.getProjectManager().getCarpetaBaseProyectos().toFile());
        javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter("Archivos de Proyecto (*.prj)", "prj");
        fileChooser.setFileFilter(filter);
        
        int result = fileChooser.showOpenDialog(parent);

        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
            // El usuario seleccionó un archivo.
            Path selectedFile = fileChooser.getSelectedFile().toPath();
            logger.debug(" -> Usuario seleccionó el archivo: {}", selectedFile);
            
            // Delegamos la carga de datos al ProjectController.
            projectController.solicitarAbrirProyecto(selectedFile);
            
            // Verificamos si la carga fue exitosa (ahora hay imágenes en el proyecto)
            if (!visorController.getProjectManager().getImagenesMarcadas().isEmpty()) {
                logger.debug(" -> Proyecto cargado exitosamente. Se procederá con el cambio de modo.");
                return true; // Éxito
            } else {
                logger.warn(" -> El proyecto seleccionado ({}) está vacío o no se pudo cargar.", selectedFile.getFileName());
                // Opcional: Mostrar un mensaje al usuario
                javax.swing.JOptionPane.showMessageDialog(parent, "El proyecto seleccionado está vacío o no es válido.", "Proyecto Vacío", javax.swing.JOptionPane.WARNING_MESSAGE);
                return false; // Fracaso
            }
        } else {
            // El usuario canceló el diálogo.
            logger.debug(" -> El usuario canceló la apertura del proyecto.");
            return false; // Cancelado
        }
    } // ---FIN de metodo manejarAperturaDeProyectoVacio---
    
    
    /**
     * Realiza las tareas de "limpieza" o guardado de estado de un modo antes de abandonarlo.
     * @param modoQueSeAbandona El modo que estamos dejando.
     */
    private void salirModo(VisorModel.WorkMode modoQueSeAbandona) {
        logger.debug("  [GeneralController] Saliendo del modo: " + modoQueSeAbandona);
        
        // --- LÓGICA DE GUARDADO AL SALIR DEL MODO PROYECTO ---
        if (modoQueSeAbandona == WorkMode.PROYECTO) {
            if (projectController != null) {
                // Sincronizamos el estado de la UI (listas, descripción, etc.) al modelo en memoria.
                // ESTO NO GUARDA EN DISCO, solo asegura que el ProjectModel esté actualizado.
                projectController.sincronizarModeloConUI();
                
                // Guardamos el estado del panel de exportación.
                model.setProjectExportPanelVisible(projectController.isExportPanelVisible());
                logger.debug("    -> Modo Proyecto: Estado de UI sincronizado con el modelo en memoria.");
            }
        }
        
        // --- LÓGICA DE GUARDADO AL SALIR DEL MODO VISUALIZADOR ---
        if (modoQueSeAbandona == WorkMode.VISUALIZADOR) {
            // Si salimos del modo visualizador Y hay cambios pendientes (el usuario marcó algo),
            // guardamos el estado en el archivo TEMPORAL. Esto preserva el "proyecto sin nombre".
            if (visorController != null && visorController.getProjectManager() != null && visorController.getProjectManager().hayCambiosSinGuardar()) {
                 // Solo guardamos si no tenemos un proyecto con nombre. Si lo tenemos, los cambios se quedan en memoria
                 // esperando un guardado explícito.
                 if (visorController.getProjectManager().getArchivoProyectoActivo() == null) {
                     logger.info("Saliendo del modo VISUALIZADOR con cambios en proyecto temporal. Guardando en archivo temporal...");
                     visorController.getProjectManager().guardarAArchivo(); // Esto guardará en "seleccion_temporal.prj"
                 }
            }
        }
        
        // --- LÓGICA DEL CARRUSEL (se mantiene igual) ---
        if (modoQueSeAbandona == WorkMode.CARROUSEL && !model.isSyncVisualizadorCarrusel()) {
            ListContext carruselCtx = model.getCarouselListContext();
            model.setUltimaCarpetaCarrusel(carruselCtx.getCarpetaRaizContexto());
            model.setUltimaImagenKeyCarrusel(carruselCtx.getSelectedImageKey());
            logger.debug("    -> Modo Carrusel Independiente: Guardando estado en el modelo.");
        }
        
        if (modoQueSeAbandona == WorkMode.CARROUSEL) {
            CarouselManager carouselManager = visorController.getActionFactory().getCarouselManager();
            if (carouselManager != null) {
                carouselManager.onCarouselModeChanged(false); // Notificar salida
            }
        }
        
    } // --- Fin del método salirModo ---

	
	private void entrarModo(WorkMode modoAlQueSeEntra) {
	    logger.debug("  [GeneralController] Entrando en modo: " + modoAlQueSeEntra);
	    if (displayModeManager != null) {
	        ListContext contextoDestino = model.getVisualizadorListContext();
	        if(modoAlQueSeEntra == WorkMode.PROYECTO) contextoDestino = model.getProyectoListContext();
	        DisplayMode modoGuardado = contextoDestino.getDisplayMode();
	        displayModeManager.switchToDisplayMode(modoGuardado);
	    }
	    
	    SwingUtilities.invokeLater(() -> {
            logger.debug("    -> [EDT-1] Cambiando tarjeta del CardLayout a: " + modoAlQueSeEntra);
	        
            switch (modoAlQueSeEntra) {
                case VISUALIZADOR:
                    // Nos aseguramos de que la barra de miniaturas esté visible si la configuración lo indica.
                    boolean miniaturasVisibles = configuration.getBoolean("interfaz.menu.vista.imagenes_en_miniatura.seleccionado", true);
                    if (registry.get("scroll.miniaturas") != null) {
                        registry.get("scroll.miniaturas").setVisible(miniaturasVisibles);
                    }

                    if (model.isSyncVisualizadorCarrusel()) {
                        model.getVisualizadorListContext().clonarDesde(model.getCarouselListContext());
                    }
                    viewManager.cambiarAVista("container.workmodes", "VISTA_VISUALIZADOR");
                    break;
                case PROYECTO: viewManager.cambiarAVista("container.workmodes", "VISTA_PROYECTOS"); break;
                case DATOS: viewManager.cambiarAVista("container.workmodes", "VISTA_DATOS"); break;
                case EDICION: viewManager.cambiarAVista("container.workmodes", "VISTA_EDICION"); break;
                case CARROUSEL: viewManager.cambiarAVista("container.workmodes", "VISTA_CARROUSEL_WORKMODE"); break;
            }

            JPanel workModesContainer = registry.get("container.workmodes");
            if (workModesContainer != null) {
                workModesContainer.revalidate();
                workModesContainer.repaint();
            }

            SwingUtilities.invokeLater(() -> {
                logger.debug("    -> [EDT-2] Restaurando y sincronizando UI para: " + modoAlQueSeEntra);
                switch (modoAlQueSeEntra) {
                    case VISUALIZADOR: visorController.restaurarUiVisualizador(); break;
                    case PROYECTO:
                        projectController.activarVistaProyecto();
                        projectController.configurarContextMenuTablaExportacion();
                        
                        if (model.isProjectExportPanelVisible()) {
                            // Le decimos al controlador que muestre el panel, pero sin cambiar el estado lógico.
                            projectController.setExportPanelVisible(true);
                            
                            // FORZAMOS LA ACTUALIZACIÓN de la cola de exportación
                            projectController.solicitarPreparacionColaExportacion();
                            
                            // Sincronizamos la selección de la tabla con la selección principal.
                            projectController.sincronizarSeleccionEnTablaExportacion();
                        }
                        
                        break;
                    case CARROUSEL:
                        ListContext contextoCarrusel = model.getCarouselListContext();
                        if (model.isSyncVisualizadorCarrusel()) contextoCarrusel.clonarDesde(model.getVisualizadorListContext());
                        else if (contextoCarrusel.getModeloLista() == null || contextoCarrusel.getModeloLista().isEmpty()) contextoCarrusel.clonarDesde(model.getVisualizadorListContext());
                        visorController.restaurarUiCarrusel();
                        if (visorController.getActionFactory().getCarouselManager() != null) visorController.getActionFactory().getCarouselManager().onCarouselModeChanged(true);
                        break;
                    case DATOS: case EDICION: break;
                }
                actualizarEstadoUiParaModo(modoAlQueSeEntra);
                if (toolbarManager != null) toolbarManager.reconstruirContenedorDeToolbars(modoAlQueSeEntra);
                if (modoAlQueSeEntra == WorkMode.CARROUSEL && visorController.getActionFactory().getCarouselManager() != null) {
                    visorController.getActionFactory().getCarouselManager().findAndWireUpFastMoveButtons();
                    visorController.getActionFactory().getCarouselManager().findAndWireUpSpeedButtons();
                    visorController.getActionFactory().getCarouselManager().wireUpEventListeners();
                }
                sincronizarEstadoBotonesDeModo();
                logger.debug("    -> [EDT-2] Restauración de UI para " + modoAlQueSeEntra + " completada.");
            });
        });
	} // --- Fin del método entrarModo ---
	
	
	/**
     * MÉTODO MAESTRO DE SINCRONIZACIÓN DE UI.
     * Habilita/deshabilita y selecciona/deselecciona componentes de la UI (acciones, botones)
     * basándose en el modo de trabajo actual y el estado del Modelo.
     * 
     * @param modoActual El modo que se acaba de activar.
     */
    private void actualizarEstadoUiParaModo(WorkMode modoActual) {
        logger.debug("  [GeneralController] Actualizando estado de la UI para el modo: " + modoActual);

        // --- 1. LÓGICA DE HABILITACIÓN/DESHABILITACIÓN (Enabled/Disabled) ---
        boolean subcarpetasHabilitado = (modoActual == WorkMode.VISUALIZADOR || modoActual == WorkMode.CARROUSEL);

	     // 2. Obtenemos todas las acciones relacionadas con esta funcionalidad.
	     Action subfolderAction = this.actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
	     Action soloCarpetaAction = this.actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA);
	     Action conSubcarpetasAction = this.actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS);
	
	     // 3. Aplicamos la misma regla a TODAS las acciones.
	     if (subfolderAction != null) {
	         subfolderAction.setEnabled(subcarpetasHabilitado);
	     }
	     if (soloCarpetaAction != null) {
	         soloCarpetaAction.setEnabled(subcarpetasHabilitado);
	     }
	     if (conSubcarpetasAction != null) {
	         conSubcarpetasAction.setEnabled(subcarpetasHabilitado);
	     }
	     
        // --- 2. LÓGICA DE SELECCIÓN (Selected/Deselected) para Toggles ---
        
        if (configAppManager != null) {
            // Sincronizar el toggle de subcarpetas
            if (subfolderAction != null) {
            	
                // Leemos el estado del contexto de lista ACTUALMENTE ACTIVO en el modelo.
                // model.isMostrarSoloCarpetaActual() ya es inteligente y devuelve el del contexto correcto.
            	
            	//LOG [DEBUG-SYNC] Modo:
            	logger.debug("  [DEBUG-SYNC] Modo: " + modoActual + ", Valor de isMostrarSoloCarpetaActual() en modelo: " + model.isMostrarSoloCarpetaActual());
            	
                boolean estadoModeloSubcarpetas = !model.isMostrarSoloCarpetaActual(); 
                
                subfolderAction.putValue(Action.SELECTED_KEY, estadoModeloSubcarpetas);
                configAppManager.actualizarAspectoBotonToggle(subfolderAction, estadoModeloSubcarpetas);
            }
            
            // Sincronizar el toggle de proporciones
            Action proporcionesAction = actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);
            if (proporcionesAction != null) {
                // De forma similar, model.isMantenerProporcion() leerá del contexto de zoom correcto.
                boolean estadoModeloProporciones = model.isMantenerProporcion();
                proporcionesAction.putValue(Action.SELECTED_KEY, estadoModeloProporciones);
                configAppManager.actualizarAspectoBotonToggle(proporcionesAction, estadoModeloProporciones);
            }
        }
        
        // --- 3. ACTUALIZACIÓN DE OTROS COMPONENTES ---
        
        if (this.statusBarManager != null) {
            this.statusBarManager.actualizar();
        }
        
        logger.debug("  [GeneralController] Estado de la UI actualizado.");
    } // --- Fin del método actualizarEstadoUiParaModo ---
    
    
    @Override
    public void onProjectStateChanged(boolean hasUnsavedChanges) {
        // Este método es llamado por ProjectManager cuando el estado de "sucio" cambia.
        // Su única responsabilidad es actualizar el título de la ventana.
        logger.debug("[GeneralController] Notificación recibida: el estado del proyecto ha cambiado. Actualizando título.");
        actualizarTituloVentana();
    } // ---FIN de metodo onProjectStateChanged---
    
    
    @Override
    public void propertyChange(java.beans.PropertyChangeEvent evt) {
        if ("focusOwner".equals(evt.getPropertyName())) {
            Component newFocusOwner = (Component) evt.getNewValue();

          // --- INICIO DEL CÓDIGO DE DIAGNÓSTICO PROFUNDO ---
          if (newFocusOwner != null) {
              logger.debug("--- [DIAGNÓSTICO DE FOCO PROFUNDO] ---");
              logger.debug("Componente con foco directo: {}", newFocusOwner.getClass().getName());

              int level = 0;
              for (Component c = newFocusOwner; c != null; c = c.getParent()) {
                  String borderInfo = (c instanceof JComponent && ((JComponent) c).getBorder() != null)
                                    ? ((JComponent) c).getBorder().getClass().getSimpleName()
                                    : "sin borde";
                  
                  logger.debug("  -> Nivel {}: Clase: {} | Nombre: {} | Borde: {}",
                               level++,
                               c.getClass().getName(),
                               c.getName(),
                               borderInfo);

                  // Si llegamos a la ventana principal, paramos.
                  if (c instanceof JFrame) break;
              }
              logger.debug("--- [FIN DEL DIAGNÓSTICO] ---");
          }
          // --- FIN DEL CÓDIGO DE DIAGNÓSTICO PROFUNDO ---

            java.awt.Color accentColor = javax.swing.UIManager.getColor("Component.accentColor");
            if (accentColor == null) accentColor = new java.awt.Color(255, 153, 51);
            this.focusedBorder = javax.swing.BorderFactory.createLineBorder(accentColor, 2);

            // Obtenemos el panel de la lista de archivos para tratarlo de forma especial
            JPanel panelListaArchivos = registry.get("panel.izquierdo.listaArchivos");

            for (javax.swing.JComponent panel : focusablePanels) {
                boolean debeTenerFoco = (newFocusOwner != null && (panel == newFocusOwner || SwingUtilities.isDescendingFrom(newFocusOwner, panel)));

                // === INICIO DE LA MODIFICACIÓN 3 (LÓGICA PRINCIPAL) ===
                
                // Si estamos procesando el panel de la lista Y tenemos su TitledBorder original...
                if (panel == panelListaArchivos && this.borderListaArchivosOriginal != null) {
                    
                    if (debeTenerFoco) {
                        // Creamos un borde compuesto: el foco por fuera, el título por dentro.
                        panel.setBorder(BorderFactory.createCompoundBorder(focusedBorder, this.borderListaArchivosOriginal));
                    } else {
                        // Creamos un borde compuesto con el borde "vacío" por fuera.
                        panel.setBorder(BorderFactory.createCompoundBorder(unfocusedBorder, this.borderListaArchivosOriginal));
                    }

                } else {
                    // Para todos los demás paneles, usamos la lógica original de reemplazar el borde.
                    panel.setBorder(debeTenerFoco ? focusedBorder : unfocusedBorder);
                }

                // === FIN DE LA MODIFICACIÓN 3 ===

                // ... (tu lógica de repaint se queda igual)
                if (panel instanceof JScrollPane) {
                    //...
                }
            }
        }
    } // --- FIN del metodo propertyChange ---
    
    
    /**
     * Orquesta la transición para entrar o salir del modo de pantalla completa.
     * Este método es llamado por la ToggleFullScreenAction y delega la manipulación
     * directa del JFrame al ViewManager, manteniendo la lógica de decisión centralizada.
     */
    public void solicitarToggleFullScreen() {
        logger.debug("[GeneralController] Solicitud para alternar pantalla completa.");

        if (viewManager == null || model == null) {
            logger.error("ERROR [solicitarToggleFullScreen]: ViewManager o Model son nulos.");
            return;
        }

        // 1. Determinar el nuevo estado invirtiendo el estado actual del MODELO.
        boolean nuevoEstado = !model.isModoPantallaCompletaActivado();

        // 2. Actualizar el MODELO con el nuevo estado.
        model.setModoPantallaCompletaActivado(nuevoEstado);

        // 3. Comandar al ViewManager para que aplique el cambio visual.
        viewManager.setFullScreen(nuevoEstado);
        
        // 4. Sincronizar la Action para que refleje el nuevo estado del MODELO.
        if (actionMap != null) {
            Action fullScreenAction = actionMap.get(AppActionCommands.CMD_VISTA_PANTALLA_COMPLETA);
            if (fullScreenAction != null) {
                fullScreenAction.putValue(Action.SELECTED_KEY, nuevoEstado);
            }
        }
    } // --- Fin del método solicitarToggleFullScreen ---
    
// *********************************************************************************************************************** INICIO SINCRONIZACION    
    
    /**
	 * Sincroniza el estado LÓGICO Y VISUAL de los botones de modo de trabajo.
	 * Asegura que solo el botón del modo activo esté seleccionado y que se aplique
     * el estilo visual personalizado.
	 */
	public void sincronizarEstadoBotonesDeModo() {
	    if (this.actionMap == null || this.model == null || this.configAppManager == null) {
	        logger.warn("WARN [GeneralController.sincronizarEstadoBotonesDeModo]: Dependencias nulas.");
	        return;
	    }

        // 1. Obtener el WorkMode actual del modelo.
        WorkMode modoActivo = this.model.getCurrentWorkMode();
        String comandoModoActivo;

        // 2. Mapear el WorkMode actual a su comando de acción correspondiente.
        switch (modoActivo) {
            case VISUALIZADOR:
                comandoModoActivo = AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR;
                break;
            case PROYECTO:
                comandoModoActivo = AppActionCommands.CMD_PROYECTO_GESTIONAR;
                break;
            case DATOS:
                comandoModoActivo = AppActionCommands.CMD_MODO_DATOS;
                break;
            case EDICION:
                comandoModoActivo = AppActionCommands.CMD_MODO_EDICION;
                break;
            case CARROUSEL:
                comandoModoActivo = AppActionCommands.CMD_VISTA_CAROUSEL;
                break;
            default:
                // Fallback por si acaso
                comandoModoActivo = AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR;
                break;
        }

	    // 3. Crear una lista de TODOS los comandos de los botones de modo.
	    List<String> comandosDeModo = List.of(
	        AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR,
	        AppActionCommands.CMD_PROYECTO_GESTIONAR,
	        AppActionCommands.CMD_MODO_DATOS,
	        AppActionCommands.CMD_MODO_EDICION,
	        AppActionCommands.CMD_VISTA_CAROUSEL
	    );

        // El resto del método se queda igual, ya que la lógica de iteración es correcta.
	    for (String comando : comandosDeModo) {
	        Action action = this.actionMap.get(comando);
	        if (action != null) {
                // a) Actualizar el estado lógico de la Action
	            boolean isSelected = comando.equals(comandoModoActivo);
	            action.putValue(Action.SELECTED_KEY, isSelected);

                // b) Actualizar el estado visual del botón asociado
                this.configAppManager.actualizarAspectoBotonToggle(action, isSelected);
	        }
	    }
	    logger.debug("[GeneralController] Sincronizados botones de modo. Activo: " + comandoModoActivo);
	} // --- Fin del método sincronizarEstadoBotonesDeModo ---
	
	
	/**
     * Orquesta una sincronización completa del estado lógico de todas las Actions
     * y la apariencia de la UI basándose en el estado actual del modelo.
     * Este es el método que se debe llamar al arrancar la aplicación para asegurar
     * que la vista inicial sea coherente.
     */
	public void sincronizarTodaLaUIConElModelo() {
        logger.info("--- [GeneralController] Iniciando sincronización maestra de ui ---");

        if (model == null || actionMap == null || visorController == null) {
            logger.error("  -> ERROR: Modelo, ActionMap o VisorController nulos. Abortando sincronización.");
            return;
        }

        // 1. Sincronizar los botones de MODO DE TRABAJO.
        sincronizarEstadoBotonesDeModo();
        
        // 2. Sincronizar los botones de MODO DE VISUALIZACIÓN (DisplayMode).
        displayModeManager.sincronizarEstadoBotonesDisplayMode();

        // 3. Delegar el resto de la sincronización específica del modo al VisorController.
        visorController.sincronizarComponentesDeModoVisualizador();

        // 4. Sincronizar los controles de subcarpetas de forma centralizada.
        sincronizarControlesDeSubcarpetas();
        
        // ***** INICIO DE LA MODIFICACIÓN *****
        // 5. Sincronizar el botón de ordenación.
        sincronizarBotonDeOrdenacion();
        // ***** FIN DE LA MODIFICACIÓN *****
        
        logger.debug("--- [GeneralController] SINCRONIZACIÓN MAESTRA DE UI COMPLETADA ---");
        
    } // --- FIN del metodo sincronizarTodaLaUIConElModelo ---
	
	
	/**
     * Recorre todas las actions del actionMap y, si son de tipo SetFilterTypeAction,
     * les ordena que se sincronicen con el estado actual del controlador.
     */
    public void sincronizarAccionesDeTipoFiltro() {
        if (actionMap == null) return;
        
        for (Action action : actionMap.values()) {
            if (action instanceof SetFilterTypeAction) {
                ((SetFilterTypeAction) action).sincronizarEstadoConControlador();
            }
        }
    } // --- Fin del método sincronizarAccionesDeTipoFiltro ---
	
// ************************************************************************************************************************** FIN SINCRONIZACION 


    /**
     * Panea la imagen al borde especificado del panel de visualización.
     * Esta es la lógica de paneo ABSOLUTO.
     * @param direction La dirección (UP, DOWN, LEFT, RIGHT) a la que panear la imagen.
     */
    public void panImageToEdge(Direction direction) {
        logger.debug("[GeneralController] Solicitud de paneo ABSOLUTO a: " + direction);
        if (model == null || model.getCurrentImage() == null || registry == null) {
            logger.error("ERROR [GeneralController.panImageToEdge]: Dependencias nulas o sin imagen actual.");
            return;
        }

        ImageDisplayPanel displayPanel = viewManager.getActiveDisplayPanel();

        if (displayPanel == null || displayPanel.getWidth() <= 0 || displayPanel.getHeight() <= 0) {
            logger.error("ERROR [GeneralController.panImageToEdge]: ImageDisplayPanel no encontrado o sin dimensiones válidas.");
            return;
        }

        BufferedImage currentImage = model.getCurrentImage();
        double zoomFactor = model.getZoomFactor();

        int imageScaledWidth = (int) (currentImage.getWidth() * zoomFactor);
        int imageScaledHeight = (int) (currentImage.getHeight() * zoomFactor);
        
        int panelWidth = displayPanel.getWidth();
        int panelHeight = displayPanel.getHeight();

        // Calcular el punto inicial del centrado (si la imagen estuviera centrada sin paneo)
        // La imagen se dibuja desde (xBase + offsetX, yBase + offsetY)
        double xBaseCentered = (double) (panelWidth - imageScaledWidth) / 2;
        double yBaseCentered = (double) (panelHeight - imageScaledHeight) / 2;

        int newOffsetX = model.getImageOffsetX(); // Partimos del offset actual del modelo
        int newOffsetY = model.getImageOffsetY();

        // Si la imagen es más pequeña que el panel en esa dimensión, el paneo al borde no tiene sentido.
        // En ese caso, la imagen ya está "en el borde" (y centrada) o no se puede mover más allá del centro.
        // Aquí solo calculamos si la imagen es MÁS GRANDE que el panel en esa dimensión,
        // de lo contrario, los offsets serán 0 (centrado) por defecto si se aplican los límites.

        switch (direction) {
            case UP:
                if (imageScaledHeight > panelHeight) {
                    newOffsetY = (int) -yBaseCentered; // Borde superior: el inicio de la imagen debe estar al inicio del panel
                } else { // Imagen más pequeña que el panel en vertical, centramos o dejamos en 0.
                    newOffsetY = 0; // O mantener el offset actual si ya está centrada.
                }
                break;
            case DOWN:
                if (imageScaledHeight > panelHeight) {
                    // Borde inferior: el final de la imagen (yBase + offsetY + imageScaledHeight) debe coincidir con el final del panel (panelHeight)
                    newOffsetY = (int) (panelHeight - imageScaledHeight - yBaseCentered);
                } else {
                    newOffsetY = 0;
                }
                break;
            case LEFT:
                if (imageScaledWidth > panelWidth) {
                    newOffsetX = (int) -xBaseCentered; // Borde izquierdo: el inicio de la imagen debe estar al inicio del panel
                } else {
                    newOffsetX = 0;
                }
                break;
            case RIGHT:
                if (imageScaledWidth > panelWidth) {
                    // Borde derecho: el final de la imagen (xBase + offsetX + imageScaledWidth) debe coincidir con el final del panel (panelWidth)
                    newOffsetX = (int) (panelWidth - imageScaledWidth - xBaseCentered);
                } else {
                    newOffsetX = 0;
                }
                break;
            case NONE:
                // No hacer nada
                return;
        }

        // Si la imagen escalada es más pequeña que el panel en una dimensión,
        // aseguramos que el offset no mueva la imagen fuera de un estado "centrado" en esa dimensión.
        // Esto es para que si paneas a la izquierda, y la imagen es más pequeña que el panel en X,
        // no se pegue al borde sino que se quede centrada.
        if (imageScaledWidth <= panelWidth) {
            newOffsetX = 0; // Si la imagen cabe, el offset es 0 (centrado)
        }
        if (imageScaledHeight <= panelHeight) {
            newOffsetY = 0; // Si la imagen cabe, el offset es 0 (centrado)
        }
        
        // Actualizar el modelo con los nuevos offsets
        model.setImageOffsetX(newOffsetX);
        model.setImageOffsetY(newOffsetY);

        // Solicitar el repintado del panel para que muestre la imagen en la nueva posición
        displayPanel.repaint();
        logger.debug("[GeneralController] Paneo absoluto a " + direction + " aplicado. Offset: (" + newOffsetX + ", " + newOffsetY + ")");
    } // --- Fin del método panImageToEdge ---


    /**
     * Panea la imagen de forma incremental en la dirección especificada por una cantidad fija.
     * Esta es la lógica de paneo INCREMENTAL.
     * @param direction La dirección (UP, DOWN, LEFT, RIGHT) del paneo incremental.
     * @param amount La cantidad de píxeles a mover en cada paso.
     */
    public void panImageIncrementally(Direction direction, int amount) {
        logger.debug("[GeneralController] Solicitud de paneo INCREMENTAL (" + amount + "px) a: " + direction);
        if (model == null || model.getCurrentImage() == null || registry == null) {
            logger.error("ERROR [GeneralController.panImageIncrementally]: Dependencias nulas o sin imagen actual.");
            return;
        }

        ImageDisplayPanel displayPanel = viewManager.getActiveDisplayPanel();
        
        // TODO: Igual que arriba, si hay múltiples paneles de display, obtener el correcto.
        if (displayPanel == null || displayPanel.getWidth() <= 0 || displayPanel.getHeight() <= 0) {
            logger.error("ERROR [GeneralController.panImageIncrementally]: ImageDisplayPanel no encontrado o sin dimensiones válidas.");
            return;
        }

        BufferedImage currentImage = model.getCurrentImage();
        double zoomFactor = model.getZoomFactor();

        int imageScaledWidth = (int) (currentImage.getWidth() * zoomFactor);
        int imageScaledHeight = (int) (currentImage.getHeight() * zoomFactor);

        int panelWidth = displayPanel.getWidth();
        int panelHeight = displayPanel.getHeight();

        int currentOffsetX = model.getImageOffsetX();
        int currentOffsetY = model.getImageOffsetY();

        int newOffsetX = currentOffsetX;
        int newOffsetY = currentOffsetY;

        // Calcular el nuevo offset incremental, y luego aplicar límites para que no se salga de la imagen.
        switch (direction) {
            case UP:    newOffsetY = currentOffsetY - amount; break;
            case DOWN:  newOffsetY = currentOffsetY + amount; break;
            case LEFT:  newOffsetX = currentOffsetX - amount; break;
            case RIGHT: newOffsetX = currentOffsetX + amount; break;
            case NONE: return;
        }
        
        // Lógica para limitar el paneo dentro de los límites de la imagen/panel
        // La imagen se dibuja a partir de (xBase + offsetX, yBase + offsetY)
        // xBase/yBase son los offsets para centrar la imagen si no hay paneo.
        double xBaseCentered = (double) (panelWidth - imageScaledWidth) / 2;
        double yBaseCentered = (double) (panelHeight - imageScaledHeight) / 2;

        // Calcular los límites máximos y mínimos para los offsets
        // (xBase + offsetX) debe estar entre (panelWidth - imageScaledWidth) y 0
        // Para que el borde izquierdo de la imagen no vaya más allá del borde izquierdo del panel (0),
        // y el borde derecho de la imagen no vaya más allá del borde derecho del panel (panelWidth).
        
        // minXOffset: (panelWidth - imageScaledWidth) - xBaseCentered
        // maxXOffset: 0 - xBaseCentered
        
        // Límites de paneo (el borde de la imagen no debe ir más allá del borde del panel)
        // minOffset: Lo más a la izquierda/arriba que puede ir el *inicio* de la imagen (relativo al panel)
        // maxOffset: Lo más a la derecha/abajo que puede ir el *inicio* de la imagen (relativo al panel)

        // Si la imagen es más grande que el panel:
        if (imageScaledWidth > panelWidth) {
            int minPossibleX = panelWidth - imageScaledWidth - (int)xBaseCentered;
            int maxPossibleX = (int)-xBaseCentered;
            newOffsetX = Math.max(minPossibleX, Math.min(newOffsetX, maxPossibleX));
        } else {
            newOffsetX = (int)-xBaseCentered; // Si la imagen es más pequeña, siempre se centra (offset es negativo de xBase)
            // Opcional: si la imagen es más pequeña, se podría forzar newOffsetX = 0 (si se prefiere pegar al centro visual)
        }
        
        if (imageScaledHeight > panelHeight) {
            int minPossibleY = panelHeight - imageScaledHeight - (int)yBaseCentered;
            int maxPossibleY = (int)-yBaseCentered;
            newOffsetY = Math.max(minPossibleY, Math.min(newOffsetY, maxPossibleY));
        } else {
            newOffsetY = (int)-yBaseCentered; // Si la imagen es más pequeña, siempre se centra
            // Opcional: si la imagen es más pequeña, se podría forzar newOffsetY = 0
        }

        // Actualizar el modelo con los nuevos offsets
        model.setImageOffsetX(newOffsetX);
        model.setImageOffsetY(newOffsetY);

        // Solicitar el repintado del panel
        displayPanel.repaint();
        logger.debug("[GeneralController] Paneo incremental aplicado. Offset: (" + newOffsetX + ", " + newOffsetY + ")");
    } // --- Fin del método panImageIncrementally ---
    
    /**
     * Actúa como un router para la acción de marcar/desmarcar una imagen.
     * Delega la solicitud al controlador del modo de trabajo activo.
     */
    public void solicitudAlternarMarcaImagenActual() {
        logger.debug("[GeneralController] Recibida solicitud para alternar marca. Modo actual: " + model.getCurrentWorkMode());
        if (model.isEnModoProyecto()) {
            projectController.solicitudAlternarMarcaImagen();
        } else {
            visorController.solicitudAlternarMarcaDeImagenActual();
        }
    } // --- Fin del método solicitudAlternarMarcaImagenActual ---
    
    
    public void solicitarEntrarEnModoProyecto() {
        logger.debug("[GeneralController] Solicitud para entrar en modo proyecto. Delegando a cambiarModoDeTrabajo...");
        // Toda la lógica compleja (comprobar si hay imágenes, pedir abrir archivo, etc.)
        // ahora reside directamente en el método cambiarModoDeTrabajo.
        cambiarModoDeTrabajo(VisorModel.WorkMode.PROYECTO);
    } // --- Fin del método solicitarEntrarEnModoProyecto ---
    
    
    
//  ************************************************************************************** IMPLEMENTACION INTERFAZ IModoController
    
    
 // REEMPLAZA ESTE MÉTODO COMPLETO EN GeneralController.java

    /**
     * Configura los listeners globales de teclado y ratón para la aplicación,
     * utilizando un sistema de etiquetado para desacoplar el controlador de la vista.
     * Añade un MouseWheelListener universal a todos los componentes etiquetados
     * como "WHEEL_NAVIGABLE" en el ComponentRegistry.
     */
    public void configurarListenersDeEntradaGlobal() {
        logger.debug("[GeneralController] Configurando listeners de entrada globales para todos los modos...");

        // --- Definición del Master Mouse Wheel Listener (Lógica Centralizada) ---
        java.awt.event.MouseWheelListener masterWheelListener = e -> {
            // Obtenemos las referencias a TODOS los componentes relevantes una sola vez, al principio del evento.
            JLabel etiquetaImagenVisualizador = registry.get("label.imagenPrincipal");
            JLabel etiquetaImagenProyecto = registry.get("label.proyecto.imagen");
            JLabel etiquetaImagenCarrusel = registry.get("label.carousel.imagen");
            JTable tablaExportacion = registry.get("tabla.exportacion");

            // Detección de la ubicación del cursor
            boolean sobreLaImagen = (e.getComponent() == etiquetaImagenVisualizador) ||
                                    (e.getComponent() == etiquetaImagenProyecto) ||
                                    (e.getComponent() == etiquetaImagenCarrusel);
            
            boolean sobreTablaExportacion = (tablaExportacion != null && SwingUtilities.isDescendingFrom(e.getComponent(), tablaExportacion));
            
            // --- LÓGICA DE PRIORIDADES ---

            // PRIORIDAD 1: Navegación especial por bloque con Ctrl+Alt
            if (e.isControlDown() && e.isAltDown()) {
                if (e.getWheelRotation() < 0) this.navegarBloqueAnterior();
                else this.navegarBloqueSiguiente();
                e.consume();
                return;
            }

            // PRIORIDAD 2: Si estamos sobre la IMAGEN
            if (sobreLaImagen) {
                // Y el MODO ZOOM MANUAL está ACTIVO...
                if (model.isZoomHabilitado()) {
                    // ...la rueda hace zoom o paneo rápido.
                    if (e.isShiftDown()) {
                        this.aplicarPan(-e.getWheelRotation() * 30, 0);
                    } else if (e.isControlDown()) {
                        this.aplicarPan(0, e.getWheelRotation() * 30);
                    } else {
                        this.aplicarZoomConRueda(e);
                    }
                } else {
                    // Si el zoom manual está DESACTIVADO, la rueda navega por la lista.
                    this.navegarSiguienteOAnterior(e.getWheelRotation());
                }
                e.consume();
                return;
            }
            
            // PRIORIDAD 3: Si estamos sobre la tabla de exportación en modo Proyecto
            if (sobreTablaExportacion && model.getCurrentWorkMode() == WorkMode.PROYECTO) {
                projectController.navegarTablaExportacionConRueda(e);
                e.consume();
                return;
            }

            // PRIORIDAD 4 (Por defecto): Navegación normal para otras listas (Nombres, Miniaturas)
            this.navegarSiguienteOAnterior(e.getWheelRotation());
            e.consume();
        };

        // --- Añadir el Master Wheel Listener a todos los componentes etiquetados ---
        List<Component> componentesConRueda = registry.getComponentsByTag("WHEEL_NAVIGABLE");
        logger.debug("[GeneralController] Encontrados " + componentesConRueda.size() + " componentes etiquetados como 'WHEEL_NAVIGABLE'.");
        for (Component c : componentesConRueda) {
            // Limpiamos listeners antiguos para evitar duplicados
            for (java.awt.event.MouseWheelListener mwl : c.getMouseWheelListeners()) {
                c.removeMouseWheelListener(mwl);
            }
            c.addMouseWheelListener(masterWheelListener);
        }

        // --- Listeners de clic y arrastre para paneo ---
        MouseAdapter paneoMouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent ev) {
                GeneralController.this.iniciarPaneo(ev);
            }
        };
        MouseMotionAdapter paneoMouseMotionAdapter = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent ev) {
                GeneralController.this.continuarPaneo(ev);
            }
        };
        
        // Obtenemos los componentes y aplicamos los listeners de paneo
        Component etiquetaVisor = registry.get("label.imagenPrincipal");
        Component etiquetaProyecto = registry.get("label.proyecto.imagen");
        Component etiquetaCarrusel = registry.get("label.carousel.imagen");

        if (etiquetaVisor != null) {
            etiquetaVisor.addMouseListener(paneoMouseAdapter);
            etiquetaVisor.addMouseMotionListener(paneoMouseMotionAdapter);
        }
        if (etiquetaProyecto != null) {
            etiquetaProyecto.addMouseListener(paneoMouseAdapter);
            etiquetaProyecto.addMouseMotionListener(paneoMouseMotionAdapter);
        }
        if (etiquetaCarrusel != null) {
            etiquetaCarrusel.addMouseListener(paneoMouseAdapter);
            etiquetaCarrusel.addMouseMotionListener(paneoMouseMotionAdapter);
        }

        logger.debug("[GeneralController] Listeners de entrada globales configurados.");
        
    } // --- Fin del método configurarListenersDeEntradaGlobal ---
    
    
    /**
     * Delega una solicitud de refresco al controlador del modo de trabajo activo.
     */
    public void solicitarRefrescoDelModoActivo() {
        logger.debug("[GeneralController] Enrutando solicitud de refresco para el modo: " + model.getCurrentWorkMode());
        if (model.isEnModoProyecto()) {
            projectController.solicitarRefresco(); // Llama al método de la interfaz
        } else {
            // Para el modo Visualizador o Carrusel, la lógica de refresco ya está en VisorController
            visorController.ejecutarRefrescoCompleto();
        }
    } // ---FIN del metodo solicitarRefrescoDelModoActivo ---
    
    
    /**
     * Implementación del método de la interfaz KeyEventDispatcher.
     * [VERSIÓN DEFINITIVA] Intercepta los atajos del NUMPAD aquí para tener control total
     * y evitar conflictos con el InputMap global.
     *
     * @param e El KeyEvent a procesar.
     * @return true si el evento fue consumido, false para continuar.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getID() != KeyEvent.KEY_PRESSED) {
            return false;
        }

        // =========================================================================
        // === INICIO DE LA SOLUCIÓN DEFINITIVA: MANEJO DE ATAJOS NUMPAD ===
        // Esta es ahora la ÚNICA fuente de verdad para los atajos numéricos.
        // =========================================================================
        String command = null;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_NUMPAD1: command = AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR; break;
            case KeyEvent.VK_NUMPAD2: command = AppActionCommands.CMD_ZOOM_TIPO_AUTO; break;
            case KeyEvent.VK_NUMPAD3: command = AppActionCommands.CMD_ZOOM_TIPO_ANCHO; break;
            case KeyEvent.VK_NUMPAD4: command = AppActionCommands.CMD_ZOOM_TIPO_ALTO; break;
            case KeyEvent.VK_NUMPAD5: command = AppActionCommands.CMD_ZOOM_TIPO_RELLENAR; break;
            case KeyEvent.VK_NUMPAD6: command = AppActionCommands.CMD_ZOOM_TIPO_FIJO; break;
            case KeyEvent.VK_NUMPAD7: command = AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO; break;
            case KeyEvent.VK_NUMPAD8: command = AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE; break;
            case KeyEvent.VK_NUMPAD9: command = AppActionCommands.CMD_ZOOM_RESET; break;
        }
        
        // Si se encontró un comando asociado a una tecla del Numpad...
        if (command != null) {
            Action action = actionMap.get(command);
            if (action != null && action.isEnabled()) {
                // Ejecutamos la acción, consumimos el evento y terminamos.
                action.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, command));
                e.consume();
                return true; // Muy importante: detenemos el procesamiento aquí.
            }
        }
        // =========================================================================
        // === FIN DE LA SOLUCIÓN DEFINITIVA ===
        // Si no se pulsó una tecla del Numpad, el código continúa y procesa
        // el resto de tus atajos y lógica de navegación.
        // =========================================================================


        // Atajos que solo funcionan en Modo Proyecto y Vista Grid
        if (model.getCurrentWorkMode() == WorkMode.PROYECTO && model.getCurrentDisplayMode() == DisplayMode.GRID) {
            Action action = null;
            if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_T) action = actionMap.get(AppActionCommands.CMD_GRID_SET_TEXT);
            else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_DELETE) action = actionMap.get(AppActionCommands.CMD_GRID_REMOVE_TEXT);
            else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ADD) action = actionMap.get(AppActionCommands.CMD_GRID_SIZE_UP_MINIATURA);
            else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_SUBTRACT) action = actionMap.get(AppActionCommands.CMD_GRID_SIZE_DOWN_MINIATURA);
            
            if (action != null) {
                action.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, null));
                e.consume();
                return true;
            }
        }
        
        // PRIORIDAD 1: Manejar la BARRA ESPACIADORA de forma global.
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            Component focusOwner = e.getComponent();
            if (focusOwner instanceof javax.swing.text.JTextComponent) {
                return false;
            }
            Action toggleMarkAction = actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
            if (toggleMarkAction != null && toggleMarkAction.isEnabled()) {
                toggleMarkAction.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA));
                return true; 
            }
        }
        
        // --- MANEJO ESPECIAL Y SEGURO DE LA TECLA ALT ---
        if (e.getKeyCode() == KeyEvent.VK_ALT) {
            Component source = e.getComponent();
            if (source instanceof javax.swing.text.JTextComponent) {
                return false;
            }
            if (visorController != null && visorController.getView() != null && visorController.getView().getJMenuBar() != null) {
                JMenuBar menuBar = visorController.getView().getJMenuBar();
                if (menuBar.isSelected()) {
                    menuBar.getSelectionModel().clearSelection();
                } else {
                    if (menuBar.getMenuCount() > 0) {
                        JMenu primerMenu = menuBar.getMenu(0);
                        if (primerMenu != null) primerMenu.doClick();
                    }
                }
                e.consume();
                return true;
            }
        }
        
        // Continuación de la lógica de navegación por teclado (movido de VisorController)
        if (model == null || registry == null) {
            return false;
        }

        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner == null) {
            return false;
        }

        JScrollPane scrollMiniaturas = registry.get("scroll.miniaturas");
        JList<String> listaNombres = registry.get("list.nombresArchivo");

        boolean focoEnAreaMiniaturas = (scrollMiniaturas != null && SwingUtilities.isDescendingFrom(focusOwner, scrollMiniaturas));
        boolean focoEnListaNombres = (listaNombres != null && SwingUtilities.isDescendingFrom(focusOwner, listaNombres));

        if (focoEnAreaMiniaturas || focoEnListaNombres) {
            boolean consumed = false;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP: case KeyEvent.VK_LEFT:
                    this.navegarAnterior();
                    consumed = true;
                    break;
                case KeyEvent.VK_DOWN: case KeyEvent.VK_RIGHT:
                    this.navegarSiguiente();
                    consumed = true;
                    break;
                case KeyEvent.VK_HOME:
                    this.navegarPrimero();
                    consumed = true;
                    break;
                case KeyEvent.VK_END:
                    this.navegarUltimo();
                    consumed = true;
                    break;
                case KeyEvent.VK_PAGE_UP:
                    this.navegarBloqueAnterior();
                    consumed = true;
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    this.navegarBloqueSiguiente();
                    consumed = true;
                    break;
            }
            if (consumed) {
                e.consume();
                return true;
            }
        }
        
        return false;
    }// --- Fin del método dispatchKeyEvent ---
    
    

// *************************************************************************************************************************
// *************************************************************************   IMPLEMENTACIÓN DE LA INTERFAZ IModoController
// *************************************************************************************************************************
    
    // --- Implementación de IModoController (delegando al controlador de modo activo) ---
    // NOTA: La lógica interna de estos métodos seguirá residiendo en VisorController y ProjectController
    // tal como están ahora. GeneralController solo actúa como un router.

    @Override
    public void navegarSiguiente() {
        logger.debug("[GeneralController] Delegando navegarSiguiente para modo: " + model.getCurrentWorkMode());
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.navegarSiguiente();
        } else {
            // Sirve tanto para VISUALIZADOR como para CARROUSEL
            visorController.navegarSiguiente();
        }
    } // --- FIN del metodo navegarSiguiente ---

    
    @Override
    public void navegarAnterior() {
        logger.debug("[GeneralController] Delegando navegarAnterior para modo: " + model.getCurrentWorkMode());
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.navegarAnterior();
        } else {
            visorController.navegarAnterior();
        }
    } // --- FIN del metodo navegarAnterior ---

    
    @Override
    public void navegarPrimero() {
        logger.debug("[GeneralController] Delegando navegarPrimero para modo: " + model.getCurrentWorkMode());
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.navegarPrimero();
        } else {
            visorController.navegarPrimero();
        }
    } // --- FIN del metodo navegarPrimero ---
    

    @Override
    public void navegarUltimo() {
        logger.debug("[GeneralController] Delegando navegarUltimo para modo: " + model.getCurrentWorkMode());
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.navegarUltimo();
        } else {
            visorController.navegarUltimo();
        }
    } // --- FIN del metodo navegarUltimo ---
    

    @Override
    public void navegarBloqueAnterior() {
        logger.debug("[GeneralController] Delegando navegarBloqueAnterior para modo: " + model.getCurrentWorkMode());
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.navegarBloqueAnterior();
        } else {
            visorController.navegarBloqueAnterior();
        }
    } // --- FIN del metodo navegarBloqueAnterior ---
    

    @Override
    public void navegarBloqueSiguiente() {
        logger.debug("[GeneralController] Delegando navegarBloqueSiguiente para modo: " + model.getCurrentWorkMode());
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.navegarBloqueSiguiente();
        } else {
            visorController.navegarBloqueSiguiente();
        }
    } // --- FIN del metodo navegarBloqueSiguiente ---
    

    /**
     * Helper para la navegación con rueda del ratón en listas (si no hay modificadores).
     * Mueve el selector de la lista en lugar del scroll.
     * Lógica movida de VisorController.
     */
    private void navegarSiguienteOAnterior(int wheelRotation) { // MÉTODO AÑADIDO (HELPER PRIVADO)
        if (wheelRotation < 0) { // Rueda hacia arriba
            navegarAnterior();
        } else { // Rueda hacia abajo
            navegarSiguiente();
        }
    
    }// --- FIN del metodo navegarSiguienteOAnterior ---
    

    @Override // ESTO ES UNA IMPLEMENTACIÓN DE LA INTERFAZ IModoController
    public void aplicarZoomConRueda(MouseWheelEvent e) {
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR) {
            visorController.aplicarZoomConRueda(e);
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.aplicarZoomConRueda(e);
        }else if (model.getCurrentWorkMode() == VisorModel.WorkMode.CARROUSEL) {
        	visorController.aplicarZoomConRueda(e);
        }
        
        //LOG [GeneralController] Delegando aplicarZoomConRueda
        logger.debug("[GeneralController] Delegando aplicarZoomConRueda a " + model.getCurrentWorkMode());
    
    }// --- FIN del metodo aplicarZoomConRueda ---
    

    @Override // ESTO ES UNA IMPLEMENTACIÓN DE LA INTERFAZ IModoController
    public void aplicarPan(int deltaX, int deltaY) {
        // La lógica de cálculo del pan (cuánto se mueve) reside en ZoomManager.
        // Aquí solo delegamos la acción de pan al controlador de modo activo.
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR) {
            visorController.aplicarPan(deltaX, deltaY);
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.aplicarPan(deltaX, deltaY);
        }else if (model.getCurrentWorkMode() == VisorModel.WorkMode.CARROUSEL) {
        	visorController.aplicarPan(deltaX, deltaY);
        }
        
        //LOG [GeneralController] Delegando aplicarPan
        logger.debug("[GeneralController] Delegando aplicarPan a " + model.getCurrentWorkMode());
    
    }// --- FIN del metodo aplicarPan ---
    

    @Override // ESTO ES UNA IMPLEMENTACIÓN DE LA INTERFAZ IModoController
    public void iniciarPaneo(MouseEvent e) {
        // En GeneralController, almacenamos lastMouseX/Y para el cálculo de delta en continuarPaneo.
        // Luego delegamos al controlador de modo, quien llamará al ZoomManager para iniciar el paneo.
        this.lastMouseX = e.getX(); // MODIFICACIÓN CLAVE: GeneralController mantiene el estado del ratón para el paneo
        this.lastMouseY = e.getY(); // MODIFICACIÓN CLAVE

        if (model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR) {
            visorController.iniciarPaneo(e);
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.iniciarPaneo(e);
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.CARROUSEL) {
        	visorController.iniciarPaneo(e);
        }
        
        //LOG [GeneralController] Delegando iniciarPaneo
        logger.debug("[GeneralController] Delegando iniciarPaneo a " + model.getCurrentWorkMode());
    
    }// --- FIN del metodo iniciarPaneo ---

    
    @Override // ESTO ES UNA IMPLEMENTACIÓN DE LA INTERFAZ IModoController
    public void continuarPaneo(MouseEvent e) {
        // Calculamos el delta de movimiento y actualizamos lastMouseX/Y aquí.
        int deltaX = e.getX() - this.lastMouseX; // MODIFICACIÓN CLAVE
        int deltaY = e.getY() - this.lastMouseY; // MODIFICACIÓN CLAVE
        this.lastMouseX = e.getX();
        this.lastMouseY = e.getY();

        // Luego delegamos la acción de aplicar el pan con los deltas calculados.
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR) {
            visorController.aplicarPan(deltaX, deltaY);
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.aplicarPan(deltaX, deltaY);
        }else if (model.getCurrentWorkMode() == VisorModel.WorkMode.CARROUSEL) {
        	visorController.aplicarPan(deltaX, deltaY);
        }
        
        
        //LOG [GeneralController] Delegando continuarPaneo
        logger.debug("[GeneralController] Delegando continuarPaneo a " + model.getCurrentWorkMode());
    
    }// --- FIN del metodo continuarPaneo ---
    
    
//    private boolean isComponentTagged(Component component, String tag) {
//        // Itera hacia arriba en la jerarquía de componentes
//        for (Component c = component; c != null; c = c.getParent()) {
//            // Comprueba si el ComponentRegistry tiene alguna etiqueta para este componente
//            // (Necesitaríamos un método en ComponentRegistry para buscar por componente,
//            // pero por ahora, vamos a simplificar asumiendo que el componente principal está etiquetado)
//            
//            // La forma más simple es comprobar si el componente es uno de los que etiquetamos.
//            if (c == registry.get("label.imagenPrincipal") ||
//                c == registry.get("label.proyecto.imagen") ||
//                c == registry.get("label.carousel.imagen")) {
//                return true;
//            }
//        }
//        return false;
//    } // --- Fin del método isComponentTagged ---
    
    
    /**
     * Notifica a todas las acciones sensibles al contexto para que actualicen su estado 'enabled'.
     * Este es el método central para llamar después de un cambio de estado global, como activar/desactivar la sincronización.
     */
    public void notificarAccionesSensiblesAlContexto() { //weno
        logger.debug("[GeneralController] Notificando a todas las acciones sensibles al contexto...");
        if (actionMap == null || model == null) return;

        // Itera por todas las acciones del mapa
        for (Action action : actionMap.values()) {
            // Comprueba si la acción implementa nuestra interfaz
            if (action instanceof ContextSensitiveAction) {
                // Si es así, la "castea" de forma segura y llama a su método de actualización
                ((ContextSensitiveAction) action).updateEnabledState(model);
            }
        }
        
        // Adicionalmente, forzamos la sincronización del botón de sync para asegurar su estado visual.
        Action syncAction = actionMap.get(AppActionCommands.CMD_TOGGLE_SYNC_VISOR_CARRUSEL);
        if (syncAction != null && configAppManager != null) {
            // Le pedimos al ConfigAppManager que aplique el estilo visual correcto al botón de Sync
            configAppManager.actualizarAspectoBotonToggle(syncAction, model.isSyncVisualizadorCarrusel());
        }
        
        // Aseguramos que el borde también se actualice en cualquier notificación general.
        actualizarBordeDeSincronizacion(model.isSyncVisualizadorCarrusel());
        
        logger.debug("[GeneralController] Notificación completada.");
    } // --- Fin del método notificarAccionesSensiblesAlContexto ---
    
    
    /**
     * Orquesta el cambio de modo de carga de subcarpetas para el modo Visualizador.
     * Este método se encarga de la lógica de alto nivel, incluyendo la sincronización final.
     * 
     * @param nuevoEstadoIncluirSubcarpetas El estado deseado: true para cargar subcarpetas, false para no hacerlo.
     */
    public void solicitarCambioModoCargaSubcarpetas(boolean nuevoEstadoIncluirSubcarpetas) {
        logger.debug("[GeneralController] Solicitud para cambiar modo de carga de subcarpetas a: " + nuevoEstadoIncluirSubcarpetas);

        // --- INICIO DE LA MODIFICACIÓN (LA GUARDA DE SEGURIDAD) ---
        // Comprobamos si el modelo YA está en el estado que se nos pide.
        // Si es así, no hay nada que hacer más que asegurar que la UI esté sincronizada.
        boolean estadoActualIncluyeSubcarpetas = !model.isMostrarSoloCarpetaActual();
        if (estadoActualIncluyeSubcarpetas == nuevoEstadoIncluirSubcarpetas) {
            logger.debug("  -> El modelo ya está en el estado deseado. Sincronizando UI por si acaso y deteniendo proceso.");
            sincronizarControlesDeSubcarpetas(); // Aseguramos que los botones reflejen el estado correcto.
            return; // Detenemos la ejecución para romper el bucle.
        }
        // --- FIN DE LA MODIFICACIÓN ---

        // 1. Validar que estemos en un modo compatible para esta operación.
        if (model.getCurrentWorkMode() != VisorModel.WorkMode.VISUALIZADOR && model.getCurrentWorkMode() != VisorModel.WorkMode.CARROUSEL) {
            logger.warn("  -> Operación cancelada: El modo actual (" + model.getCurrentWorkMode() + ") no soporta esta acción.");
            sincronizarControlesDeSubcarpetas(); // Revertimos visualmente por si acaso.
            return;
        }

        // 2. Validar dependencias.
        if (visorController == null || model == null || configuration == null || displayModeManager == null) {
            logger.error("  ERROR [GeneralController]: Dependencias críticas (visorController, model, config, displayModeManager) nulas. Abortando.");
            return;
        }

        // 3. Guardar la clave de la imagen actual ANTES de cualquier cambio.
        final String claveAntesDelCambio = model.getSelectedImageKey();
        logger.debug("  -> Clave de imagen a intentar mantener: " + claveAntesDelCambio);

        // 4. Actualizar el estado en el Modelo y la Configuración.
        model.setMostrarSoloCarpetaActual(!nuevoEstadoIncluirSubcarpetas);
        configuration.setString(ConfigKeys.COMPORTAMIENTO_CARGAR_SUBCARPETAS, String.valueOf(nuevoEstadoIncluirSubcarpetas));

        // 5. Definir la acción de sincronización que se ejecutará DESPUÉS de la carga.
        Runnable accionPostCarga = () -> {
            logger.debug("  [Callback Post-Carga] Tarea de carga finalizada. Ejecutando sincronización maestra...");
            
            // a) Sincronizar toda la UI (botones, menús, estados, etc.).
            this.sincronizarTodaLaUIConElModelo();
            
            // b) Repoblar el Grid con la nueva lista.
            if (displayModeManager != null) {
                displayModeManager.poblarGridConModelo(model.getModeloLista());
                displayModeManager.sincronizarSeleccionGrid();
            }
            
            logger.debug("  [Callback Post-Carga] Sincronización finalizada.");
        };

        // 6. Delegar la tarea de carga de bajo nivel al VisorController.
        logger.debug("  -> Delegando a VisorController la tarea de recargar la lista de imágenes...");
        visorController.cargarListaImagenes(claveAntesDelCambio, accionPostCarga);
        
    } // --- FIN del metodo solicitarCambioModoCargaSubcarpetas ---
    
    
    /**
     * MÉTODO DE SINCRONIZACIÓN CENTRALIZADO.
     * Lee el estado actual del modelo y actualiza el estado 'selected' y la apariencia
     * de TODOS los controles relacionados con la carga de subcarpetas (el botón toggle y los dos radio-botones del menú).
     * Esta es la ÚNICA fuente de verdad para la sincronización de estos componentes.
     */
    private void sincronizarControlesDeSubcarpetas() {
        if (model == null || actionMap == null || configAppManager == null) {
            logger.warn("WARN [sincronizarControlesDeSubcarpetas]: Dependencias nulas. No se puede sincronizar.");
            return;
        }

        // 1. Leer el estado "de verdad" desde el modelo UNA SOLA VEZ.
        boolean estadoActualIncluyeSubcarpetas = !model.isMostrarSoloCarpetaActual();

        // 2. Obtener las tres Actions relacionadas.
        Action toggleAction = actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
        Action radioIncluirAction = actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS);
        Action radioSoloAction = actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA);

        // 3. Sincronizar el botón Toggle principal.
        if (toggleAction != null) {
            toggleAction.putValue(Action.SELECTED_KEY, estadoActualIncluyeSubcarpetas);
            configAppManager.actualizarAspectoBotonToggle(toggleAction, estadoActualIncluyeSubcarpetas);
        }
        
        // 4. Sincronizar el radio-botón "Incluir Subcarpetas".
        if (radioIncluirAction != null) {
            radioIncluirAction.putValue(Action.SELECTED_KEY, estadoActualIncluyeSubcarpetas);
        }

        // 5. Sincronizar el radio-botón "Solo Carpeta Actual". Su estado es el inverso.
        if (radioSoloAction != null) {
            radioSoloAction.putValue(Action.SELECTED_KEY, !estadoActualIncluyeSubcarpetas);
        }
        
        logger.debug("  -> Sincronizados controles de subcarpetas. Estado actual (incluir): " + estadoActualIncluyeSubcarpetas);
    } // --- Fin del método sincronizarControlesDeSubcarpetas ---
    
    
    /**
     * Punto de entrada principal para cargar una nueva carpeta sin una preselección específica.
     * Delega a la versión más completa del método pasando null como clave a seleccionar.
     * @param nuevaCarpeta La nueva carpeta raíz a visualizar.
     */
    public void solicitarCargaDesdeNuevaRaiz(Path nuevaCarpeta) {
    	
        solicitarCargaDesdeNuevaRaiz(nuevaCarpeta, null);
    } // --- Fin del método solicitarCargaDesdeNuevaRaiz (simple) ---

    
    public void solicitarCargaDesdeNuevaRaiz(Path nuevaCarpeta, String claveASeleccionar) {
        logger.debug("--->>> [GeneralController] Solicitud para cargar desde nueva raíz: " + nuevaCarpeta);

        if (persistente_activo) {
            logger.info("[GC] Carga de nueva carpeta. Limpiando estado de filtro persistente.");
            filterManager.clearFilters();
            persistente_activo = false;
            persistente_punteroOriginalKey = null;
        }
        if (model.isLiveFilterActive()) {
            onLiveFilterStateChanged(false);
        }

        if (nuevaCarpeta == null || !Files.isDirectory(nuevaCarpeta)) { return; }
        if (model == null || visorController == null || displayModeManager == null) { return; }

        model.setCarpetaRaizActual(nuevaCarpeta);
        if (this.folderTreeManager != null) { this.folderTreeManager.sincronizarArbolConCarpeta(nuevaCarpeta); }

        Runnable accionPostCarga = () -> {
            logger.debug("  [Callback Post-Carga de Nueva Raíz] Tarea de carga finalizada.");
            
            if (model != null && model.getModeloLista() != null) {
                this.persistente_listaMaestraOriginal = clonarModelo(model.getModeloLista());
//                this.persistente_mapaRutasOriginal = new HashMap<>(model.getRutaCompletaMap());
                logger.info("[GC] Contexto Maestro Inmutable capturado. Tamaño: " + this.persistente_listaMaestraOriginal.getSize());
            } else {
                this.persistente_listaMaestraOriginal = null;
//                this.persistente_mapaRutasOriginal = null;
            }
            
            this.sincronizarTodaLaUIConElModelo();
            
            // =========================================================================
            // === CORRECCIÓN: Usar los métodos correctos del DisplayModeManager ===
            // =========================================================================
            if (displayModeManager != null) {
                displayModeManager.poblarGridConModelo(model.getModeloLista());
                displayModeManager.sincronizarSeleccionGrid();
            }
            // =========================================================================
            // === FIN DE LA CORRECCIÓN ===
            // =========================================================================
        };

        visorController.cargarListaImagenes(claveASeleccionar, accionPostCarga);
        
    } // --- Fin del método solicitarCargaDesdeNuevaRaiz (con preselección) ---
    
    
    private DefaultListModel<String> clonarModelo(DefaultListModel<String> original) {
        if (original == null) return null;
        DefaultListModel<String> copia = new DefaultListModel<>();
        for (int i = 0; i < original.getSize(); i++) {
            copia.addElement(original.getElementAt(i));
        }
        return copia;
    }// --- Fin del método helper clonarModelo ---
    
    
    
    
//  ********************************************************************************** FIN IMPLEMENTACION INTERFAZ IModoController
    
//  *************************************************************************************************************** INICIO GETTERS    
    
    public ToolbarManager getToolbarManager() {return this.toolbarManager;}
    public VisorModel getModel() { return this.model;}
    public void setDisplayModeManager(DisplayModeManager displayModeManager) {this.displayModeManager = Objects.requireNonNull(displayModeManager, "DisplayModeManager no puede ser nulo");}
    public void setConfiguration(ConfigurationManager configuration) {this.configuration = Objects.requireNonNull(configuration, "ConfigurationManager no puede ser nulo");}
    

//  ****************************************************************************************************************** FIN GETTERS
    
    
    /**
     * MÉTODO ORQUESTADOR CENTRAL PARA ALTERNAR EL MODO DE CARGA DE SUBCARPETAS.
     * Invierte el estado actual del modelo y luego inicia el proceso de recarga.
     * Utiliza un flag de bloqueo para evitar ejecuciones concurrentes.
     */
    public void solicitarToggleModoCargaSubcarpetas() {
        // Si ya hay una operación en curso, la ignoramos.
        if (isChangingSubfolderMode) {
            logger.warn("  [GeneralController] ADVERTENCIA: Se ha ignorado una solicitud de toggle de subcarpetas porque ya hay una en progreso.");
            return;
        }

        try {
            isChangingSubfolderMode = true; // --- BLOQUEAMOS ---
            logger.debug("[GeneralController] Solicitud para ALTERNAR modo de carga de subcarpetas.");
            
            // 1. Invertir el estado actual del modelo. Esta es la lógica central.
            boolean nuevoEstadoSoloCarpeta = !model.isMostrarSoloCarpetaActual();
            model.setMostrarSoloCarpetaActual(nuevoEstadoSoloCarpeta);
            
            // 2. Actualizar la configuración para que se guarde.
            configuration.setString(ConfigKeys.COMPORTAMIENTO_CARGAR_SUBCARPETAS, String.valueOf(!nuevoEstadoSoloCarpeta));
            logger.debug("  -> Estado del modelo cambiado a: isMostrarSoloCarpetaActual=" + nuevoEstadoSoloCarpeta);

            // 3. El resto de la lógica es la que ya teníamos...
            final String claveAntesDelCambio = model.getSelectedImageKey();
            logger.debug("  -> Clave de imagen a intentar mantener: " + claveAntesDelCambio);

            Runnable accionPostCarga = () -> {
                try {
                    logger.debug("  [Callback Post-Carga] Tarea de carga finalizada. Ejecutando sincronización maestra...");
                    this.sincronizarTodaLaUIConElModelo();
                    
                    if (displayModeManager != null) {
                        displayModeManager.poblarGridConModelo(model.getModeloLista());
                        displayModeManager.sincronizarSeleccionGrid();
                    }
                    
                    logger.debug("  [Callback Post-Carga] Sincronización finalizada.");
                } finally {
                    isChangingSubfolderMode = false; // --- DESBLOQUEAMOS ---
                }
            };

            visorController.cargarListaImagenes(claveAntesDelCambio, accionPostCarga);

        } catch (Exception e) {
            logger.error("ERROR INESPERADO en solicitarToggleModoCargaSubcarpetas: " + e.getMessage());
            e.printStackTrace();
            isChangingSubfolderMode = false; // --- DESBLOQUEAMOS EN CASO DE ERROR ---
        }
    } // --- FIN del metodo solicitarToggleModoCargaSubcarpetas ---
    
    
    /**
     * Reordena la lista de archivos actual basándose en el estado de sortDirection
     * del modelo y actualiza la UI del botón de ordenación.
     * [VERSIÓN CORREGIDA Y LIMPIA]
     */
    public void resortFileListAndSyncButton() {
        VisorModel.SortDirection direction = model.getSortDirection();
        ListContext currentContext = model.getCurrentListContext();
        if (currentContext == null) return;

        DefaultListModel<String> listModel = currentContext.getModeloLista();
        if (listModel.isEmpty()) {
            syncSortButtonUI(direction);
            return;
        }

        if (direction == VisorModel.SortDirection.NONE) {
            solicitarCargaDesdeNuevaRaiz(model.getCarpetaRaizActual());
            syncSortButtonUI(direction);
            return;
        }

        List<String> items = new ArrayList<>();
        for (int i = 0; i < listModel.getSize(); i++) {
            items.add(listModel.getElementAt(i));
        }

        String selectedKey = model.getSelectedImageKey();

        // Lógica de ordenación por NOMBRE DE ARCHIVO
        items.sort((pathStr1, pathStr2) -> {
            String fileName1 = Paths.get(pathStr1).getFileName().toString();
            String fileName2 = Paths.get(pathStr2).getFileName().toString();
            return fileName1.compareToIgnoreCase(fileName2);
        });

        // Si es descendente, simplemente invertimos la lista ya ordenada
        if (direction == VisorModel.SortDirection.DESCENDING) {
            Collections.reverse(items);
        }

        listModel.clear();
        listModel.addAll(items);
        
        int newIndex = (selectedKey != null) ? listModel.indexOf(selectedKey) : -1;
        if (newIndex == -1 && !listModel.isEmpty()) {
            newIndex = 0;
        }
        
        if (this.getVisorController().getListCoordinator() != null) {
            this.getVisorController().getListCoordinator().reiniciarYSeleccionarIndice(newIndex);
        }

        syncSortButtonUI(direction);
    } // ---FIN de metodo resortFileListAndSyncButton---
    
    
    /**
     * Sincroniza ÚNICAMENTE la apariencia del botón de ordenación basándose
     * en el estado actual del modelo, sin alterar la lista.
     */
    public void sincronizarBotonDeOrdenacion() {
        if (model == null) return;
        syncSortButtonUI(model.getSortDirection());
    } // --- FIN del metodo sincronizarBotonDeOrdenacion ---


    /**
     * Actualiza el icono, tooltip y BORDE del botón de ordenación, usando
     * el color de acento del tema actual, igual que el resto de la aplicación.
     * [VERSIÓN FINAL CON ARQUITECTURA DE TEMA]
     */
    private void syncSortButtonUI(VisorModel.SortDirection direction) {
        Action sortAction = this.actionMap.get(AppActionCommands.CMD_ORDEN_CICLO);
        if (sortAction == null) return;

        String buttonKey = "interfaz.boton.orden_lista.orden_ciclo";
        javax.swing.JButton sortButton = registry.get(buttonKey);
        
        if (sortButton == null) {
            return;
        }

        // --- INICIALIZACIÓN DE BORDES (se hace solo una vez) ---
        if (!sortBordersInitialized) {
            int thickness = 2; 

            // LA CLAVE: Usamos el color de acento definido por el tema.
            // Es el mismo que usa BackgroundControlManager a través del objeto Tema.
            java.awt.Color activeColor = javax.swing.UIManager.getColor("Component.accentColor");
            if (activeColor == null) {
                // Fallback por si la clave no existiera en algún tema raro.
                activeColor = javax.swing.UIManager.getColor("Component.focusColor");
                if (activeColor == null) {
                     activeColor = new java.awt.Color(59, 142, 255);
                }
            }
            
            this.sortButtonActiveBorder = javax.swing.BorderFactory.createLineBorder(activeColor, thickness);

            // El borde inactivo reserva el espacio para que el botón no "salte".
            this.sortButtonInactiveBorder = javax.swing.BorderFactory.createEmptyBorder(thickness, thickness, thickness, thickness);

            sortBordersInitialized = true;
        }

        // --- LÓGICA DE ACTUALIZACIÓN ---
        String iconKey;
        String tooltip;

        switch (direction) {
            case ASCENDING:
            case DESCENDING:
                if (direction == VisorModel.SortDirection.ASCENDING) {
                    iconKey = "30004-orden_ascendente.png";
                    tooltip = "Orden: Ascendente (clic para Z-A)";
                } else {
                    iconKey = "30005-orden_descendente.png";
                    tooltip = "Orden: Descendente (clic para apagar)";
                }
                sortButton.setBorder(this.sortButtonActiveBorder);
                break;
                
            case NONE:
            default:
                iconKey = "30006-orden_off.png";
                tooltip = "Orden: Apagado (clic para A-Z)";
                sortButton.setBorder(this.sortButtonInactiveBorder);
                break;
        }

        int iconSize = configuration.getInt("iconos.ancho", 24);
        ImageIcon newIcon = this.getVisorController().getIconUtils().getScaledIcon(iconKey, iconSize, iconSize);
        sortAction.putValue(Action.SMALL_ICON, newIcon);
        sortAction.putValue(Action.SHORT_DESCRIPTION, tooltip);
        
        sortButton.repaint();
    } // --- FIN del metodo syncSortButtonUI ---
    
    
    /**
     * Orquesta la búsqueda de la siguiente coincidencia usando el FilterManager.
     * Este método es llamado por el ActionListener del campo de búsqueda.
     */
    private void buscarSiguienteCoincidencia() {
        if (registry == null || model == null || visorController == null || filterManager == null || visorController.getListCoordinator() == null) {
            logger.warn("[GeneralController] No se puede buscar, faltan dependencias críticas.");
            return;
        }

        // 1. Obtener el campo de texto y el texto a buscar.
        javax.swing.JTextField searchField = registry.get("textfield.filtro.orden");
        if (searchField == null) {
            logger.error("[GeneralController] No se encontró el JTextField 'textfield.filtro.orden' en el registro.");
            return;
        }
        
        String searchText = searchField.getText();
        if (searchText.isBlank() || searchText.equals("Texto a buscar...")) {
            return; // No hay nada que buscar.
        }

        // 2. Obtener el contexto actual para la búsqueda.
        ListContext currentContext = model.getCurrentListContext();
        DefaultListModel<String> masterListModel = currentContext.getModeloLista();
        int startIndex = visorController.getListCoordinator().getOfficialSelectedIndex();

        // 3. Delegar la búsqueda al FilterManager.
        int foundIndex = filterManager.buscarSiguiente(masterListModel, startIndex, searchText);

        // 4. Procesar el resultado.
        if (foundIndex != -1) {
            // Coincidencia encontrada: usar el ListCoordinator para seleccionar.
            visorController.getListCoordinator().seleccionarImagenPorIndice(foundIndex);
            
        } else {
            // No se encontró: notificar al usuario.
            if (statusBarManager != null) {
                statusBarManager.mostrarMensajeTemporal("No se encontró: \"" + searchText + "\"", 3000);
            }
        }
    } // --- Fin del método buscarSiguienteCoincidencia ---
    
    
    /**
     * Configura el comportamiento del texto placeholder en un JTextField.
     */
    private void configurePlaceholderText(javax.swing.JTextField searchField) {
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getText().equals("Texto a buscar...")) {
                    searchField.setText("");
                    searchField.setForeground(javax.swing.UIManager.getColor("TextField.foreground"));
                }
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Texto a buscar...");
                    searchField.setForeground(javax.swing.UIManager.getColor("TextField.placeholderForeground"));
                }
            }
        });
        if (searchField.getText().equals("Texto a buscar...")) {
             searchField.setForeground(javax.swing.UIManager.getColor("TextField.placeholderForeground"));
        }
    } // --- Fin del método configurePlaceholderText ---

//    /**
//     * Se activa cuando el botón de filtro en vivo (huracán) es pulsado.
//     * Actualiza el estado en el modelo y aplica o limpia el filtro.
//     */
//    private void onToggleLiveFilter(boolean isSelected) {
//        model.setLiveFilterActive(isSelected);
//        logger.debug("[GeneralController] Modo Filtro en Vivo cambiado a: {}", isSelected);
//        
//        if (isSelected) {
//            actualizarFiltro(); // Aplicar filtro con el texto actual
//        } else {
//            limpiarFiltro(); // Volver a la lista maestra
//        }
//        
//        // Sincroniza la apariencia visual del botón
//        Action liveFilterAction = actionMap.get(AppActionCommands.CMD_FILTRO_TOGGLE_LIVE_FILTER);
//        configAppManager.actualizarAspectoBotonToggle(liveFilterAction, isSelected);
//
//    } // --- Fin del método onToggleLiveFilter ---

    
    /**
     * Reacciona a los cambios en el campo de texto cuando el filtro en vivo está activo.
     * Crea un criterio de filtro temporal y le pide al FilterManager que aplique los filtros.
     */
    private void actualizarFiltro() {
        if (!model.isLiveFilterActive() || this.masterModelSinFiltro == null) return;

        javax.swing.JTextField searchField = registry.get("textfield.filtro.orden");
        if (searchField == null) return;
        String searchText = searchField.getText().equals("Texto a buscar...") ? "" : searchField.getText();

        // 1. Preparamos el FilterManager con la regla de filtro actual
        filterManager.clearFilters();
        if (!searchText.isBlank()) {
            filterManager.addFilter(new FilterCriterion(searchText, FilterSource.FILENAME, FilterType.CONTAINS));
        }

        // 2. Generamos el contenido filtrado a partir de nuestra copia maestra
        DefaultListModel<String> filteredContent = filterManager.applyFilters(this.masterModelSinFiltro);

        // 3. Obtenemos el modelo de lista QUE ESTÁ EN USO y modificamos su contenido
        DefaultListModel<String> modeloEnUso = model.getModeloLista();
        modeloEnUso.clear();
        // --- LÍNEA CORREGIDA ---
        modeloEnUso.addAll(Collections.list(filteredContent.elements()));
        
        // 4. Reiniciamos el coordinador.
        visorController.getListCoordinator().reiniciarYSeleccionarIndice(modeloEnUso.isEmpty() ? -1 : 0);
            
        
    } // --- Fin del método actualizarFiltro ---
    

    /**
     * Restaura la JList de nombres para que muestre el modelo maestro original.
     */
    private void limpiarFiltro() {
        if (this.masterModelSinFiltro == null) return;

        // 1. Obtenemos el modelo de lista QUE ESTÁ EN USO
        DefaultListModel<String> modeloEnUso = model.getModeloLista();
        
        // 2. Restauramos su contenido a partir de nuestra copia maestra
        modeloEnUso.clear();
        // --- LÍNEA CORREGIDA ---
        
        modeloEnUso.addAll(Collections.list(this.masterModelSinFiltro.elements()));
        
        // 3. Le decimos al coordinador que vuelva al índice que teníamos guardado.
        visorController.getListCoordinator().reiniciarYSeleccionarIndice(this.indiceSeleccionadoAntesDeFiltrar);

        // 4. Limpiamos nuestra copia de seguridad.
        if (filterManager != null) {
            filterManager.clearFilters();
        }
        
        this.masterModelSinFiltro = null;
        this.indiceSeleccionadoAntesDeFiltrar = -1;
        
    } // --- Fin del método limpiarFiltro ---
    
    
    /**
     * Es llamado por la AddFilterAction. Orquesta la adición de un nuevo filtro.
     * @param source La fuente del filtro (FILENAME o FOLDER_PATH).
     * @param type El tipo de filtro (CONTAINS o DOES_NOT_CONTAIN).
     */
    public void solicitarAnadirFiltro(FilterSource source, FilterType type) {
    	
    	limpiarEstadoFiltroRapidoSiActivo();
    	
        javax.swing.JTextField tf = registry.get("textfield.filtro.texto");
        if (tf == null || tf.getText().isBlank()) return;
        
        filterManager.addFilter(new FilterCriterion(tf.getText(), this.filtroActivoSource, type));
        
        tf.setText("");
        
        gestionarFiltroPersistente();

    } // --- Fin del método solicitarAnadirFiltro ---

    /**
     * Es llamado por la RemoveFilterAction. Elimina el filtro actualmente seleccionado.
     */
    public void solicitarEliminarFiltroSeleccionado() {
    	
    	limpiarEstadoFiltroRapidoSiActivo(); 
    	
        JList<FilterCriterion> filterList = registry.get("list.filtrosActivos");
        if (filterList == null || filterList.getSelectedValue() == null) return;

        filterManager.removeFilter(filterList.getSelectedValue());
        gestionarFiltroPersistente();
    } // --- Fin del método solicitarEliminarFiltroSeleccionado ---

    /**
     * Es llamado por la ClearAllFiltersAction. Limpia todos los filtros activos.
     */
    public void solicitarLimpiarTodosLosFiltros() {
    	
    	limpiarEstadoFiltroRapidoSiActivo();
    	
        filterManager.clearFilters();
        gestionarFiltroPersistente();
        
    } // --- Fin del método solicitarLimpiarTodosLosFiltros ---
    
    
    /**
     * NUEVO MÉTODO HELPER.
     * Cumple la "Regla del Reset Total": si el filtro rápido ("Tornado") está
     * activo, lo desactiva y limpia su JTextField asociado.
     */
    private void limpiarEstadoFiltroRapidoSiActivo() {
        
    	javax.swing.JTextField searchField = registry.get("textfield.filtro.orden");
    	
    	if (model.isLiveFilterActive()) {
          // Desactiva la lógica del filtro rápido y restaura la lista
          onLiveFilterStateChanged(false); 
    	}
    	
    	if (searchField != null) {
          // Usamos invokeLater para asegurar que la limpieza ocurra sin conflictos
          // con otros eventos de la UI.
          SwingUtilities.invokeLater(() -> searchField.setText("")); 
    	}
        
    } // --- Fin del método limpiarEstadoFiltroRapidoSiActivo ---
    
    
    private void gestionarFiltroPersistente() {
        // Su única responsabilidad ahora es llamar al método de refresco.
        // Toda la lógica de checkpoint está ahora dentro de refrescarConFiltrosPersistentes.
        refrescarConFiltrosPersistentes();
    } // --- Fin del método gestionarFiltroPersistente ---
    
    
    /**
     * NUEVO MÉTODO CENTRALIZADO
     * La única función que aplica el filtro persistente y actualiza la UI.
     */
    private void refrescarConFiltrosPersistentes() {
        // Actualiza la UI de la lista de filtros (esto siempre es necesario)
        JList<FilterCriterion> filterListUI = registry.get("list.filtrosActivos");
        if (filterListUI != null) {
            DefaultListModel<FilterCriterion> modelUI = new DefaultListModel<>();
            modelUI.addAll(filterManager.getActiveFilters());
            filterListUI.setModel(modelUI);
        }

        // Si hay filtros, activamos el modo persistente y filtramos
        if (filterManager.isFilterActive()) {
            // Si el modo persistente no está activo, lo activamos y guardamos el checkpoint.
            if (!persistente_activo) {
                this.persistente_listaMaestraOriginal = clonarModelo(model.getModeloLista());
//                this.persistente_mapaRutasOriginal = new HashMap<>(model.getRutaCompletaMap());
                this.persistente_punteroOriginalKey = model.getSelectedImageKey();
                this.persistente_activo = true;
            }

            // Filtramos SIEMPRE sobre la lista guardada
            DefaultListModel<String> listaFiltrada = filterManager.applyFilters(this.persistente_listaMaestraOriginal);
            actualizarListaVisibleConResultado(listaFiltrada, 0, false);

        } else { // Si no hay filtros, restauramos
            if (persistente_activo) {
                // Restauramos la lista guardada
                DefaultListModel<String> listaRestaurada = this.persistente_listaMaestraOriginal;
                int indiceOriginal = (persistente_punteroOriginalKey != null) 
                                   ? listaRestaurada.indexOf(persistente_punteroOriginalKey) : -1;
                final int indiceFinal = (indiceOriginal != -1) ? indiceOriginal : 0;
                
                actualizarListaVisibleConResultado(listaRestaurada, indiceFinal, true);
                
                // Reseteamos el estado del checkpoint
                this.persistente_listaMaestraOriginal = null;
//                this.persistente_mapaRutasOriginal = null;
                this.persistente_punteroOriginalKey = null;
                this.persistente_activo = false;
            }
        }
    } // --- Fin del método refrescarConFiltrosPersistentes ---
    
    
    /**
     * Cambia el tipo de filtro que se usará al añadir un nuevo criterio.
     * Es llamado por las Actions de los JToggleButtons de tipo de filtro.
     * @param nuevoSource El nuevo FilterSource a establecer como activo.
     */
    public void solicitarCambioTipoFiltro(FilterSource nuevoSource) {
        if (this.filtroActivoSource != nuevoSource) {
            this.filtroActivoSource = nuevoSource;
            logger.debug("[GeneralController] Tipo de filtro activo cambiado a: {}", nuevoSource);
            
            // Aquí podrías añadir una notificación en la barra de estado si quisieras
            // statusBarManager.mostrarMensajeTemporal("Filtrar por: " + nuevoSource.toString(), 2000);
        }
    } // --- Fin del método solicitarCambioTipoFiltro ---
    
    
    /**
     * Actualiza la JList visible modificando el contenido de su modelo actual
     * y reinicia el coordinador en la posición deseada.
     * @param nuevaLista El nuevo contenido para la lista.
     * @param indiceASeleccionar El índice que se debe seleccionar tras la actualización.
     */
    private void actualizarListaVisibleConResultado(DefaultListModel<String> nuevaLista, int indiceASeleccionar, boolean resetearCheckpoint) {
        DefaultListModel<String> modeloEnUso = model.getModeloLista();
        
        final int finalIndice = indiceASeleccionar;

        SwingUtilities.invokeLater(() -> {
            // Técnica Tornado: modificar el modelo existente.
            modeloEnUso.clear();
            modeloEnUso.addAll(Collections.list(nuevaLista.elements()));
            
            // Actualizar el título
            String titulo = filterManager.isFilterActive() ? "Archivos (Filtro): " : "Archivos: ";
            visorController.getView().setTituloPanelIzquierdo(titulo + modeloEnUso.getSize());
            
            // Reiniciar el coordinador
            visorController.getListCoordinator().reiniciarYSeleccionarIndice(finalIndice);
            
            if (resetearCheckpoint) {
                logger.info("[GC] Reseteando estado del checkpoint persistente...");
                this.persistente_listaMaestraOriginal = null;
//                this.persistente_mapaRutasOriginal = null;
                this.persistente_punteroOriginalKey = null;
                this.persistente_activo = false;
            }
        });
    } // --- Fin del método actualizarListaVisibleConResultado ---
    
    
//    /**
//     * NUEVO MÉTODO HELPER.
//     * Actualiza la JList principal de la vista (la lista de nombres de archivo)
//     * con el modelo de datos que está actualmente activo en el VisorModel.
//     * También actualiza el título del panel que la contiene.
//     */
//    private void actualizarListaPrincipalDeLaVista() {
//        if (visorController == null || visorController.getView() == null || model == null) return;
//
//        // El modelo de la vista debe ser el que está activo en el modelo de datos
//        DefaultListModel<String> modeloActual = model.getModeloLista();
//        
//        // El método setListaImagenesModel ya está en tu VisorView y hace lo correcto
//        visorController.getView().setListaImagenesModel(modeloActual);
//        
//        // Actualizamos el título del panel
//        String titulo = filterManager.isFilterActive() ? "Archivos (Filtro): " : "Archivos: ";
//        visorController.getView().setTituloPanelIzquierdo(titulo + modeloActual.getSize());
//
//        logger.debug("[GeneralController] La JList principal de la vista ha sido actualizada. Tamaño: {}", modeloActual.getSize());
//        
//    } // --- Fin del método actualizarListaPrincipalDeLaVista ---

    
//    /**
//     * Helper para construir un mapa de rutas a partir de una lista filtrada.
//     */
//    private Map<String, Path> construirMapaDesdeLista(DefaultListModel<String> lista, Map<String, Path> mapaOriginal) {
//        Map<String, Path> nuevoMapa = new HashMap<>();
//        for (int i = 0; i < lista.getSize(); i++) {
//            String key = lista.getElementAt(i);
//            nuevoMapa.put(key, mapaOriginal.get(key));
//        }
//        return nuevoMapa;
//        
//    } // --- Fin del método construirMapaDesdeLista ---
    
    
    /**
     * Orquesta la conversión del filtro rápido (Tornado) en un filtro persistente.
     * Este método es llamado por la Action del botón "hacer persistente".
     * AÑADE el filtro del Tornado a los filtros persistentes existentes.
     */
    public void solicitarPersistenciaDeFiltroRapido() {
        logger.debug("[GeneralController] Solicitud para AÑADIR filtro rápido a persistentes...");

        // 1. Obtener el texto del filtro rápido
        javax.swing.JTextField searchField = registry.get("textfield.filtro.orden");
        if (searchField == null || searchField.getText().isBlank() || searchField.getText().equals("Texto a buscar...")) {
            return;
        }
        String textoFiltro = searchField.getText();

        // 2. Limpiar el estado del Tornado (pero SIN TOCAR LA LISTA VISIBLE)
        if (model.isLiveFilterActive()) {
            model.setLiveFilterActive(false);
            Action liveFilterAction = actionMap.get(AppActionCommands.CMD_FILTRO_TOGGLE_LIVE_FILTER);
            if (liveFilterAction != null) {
                liveFilterAction.putValue(Action.SELECTED_KEY, false);
                if (configAppManager != null) {
                    configAppManager.actualizarAspectoBotonToggle(liveFilterAction, false);
                }
            }
        }
        SwingUtilities.invokeLater(() -> searchField.setText(""));

        // 3. Añadir el nuevo criterio al FilterManager
        FilterCriterion nuevoFiltroPersistente = new FilterCriterion(textoFiltro, FilterSource.FILENAME, FilterType.CONTAINS);
        filterManager.addFilter(nuevoFiltroPersistente);

        // 4. Llamar a la función de refresco.
        refrescarConFiltrosPersistentes();

    } // --- Fin del método solicitarPersistenciaDeFiltroRapido ---
    
    
//    /**
//     * Método central para actualizar la UI después de cualquier cambio en los filtros.
//     * Refresca tanto la lista de filtros activos como la lista de archivos filtrada.
//     */
//    private void refrescarVistasDeFiltros() {
//        // 1. Actualizar la JList de la pestaña "Filtros" (esto es solo visual y no cambia)
//        JList<FilterCriterion> filterListUI = registry.get("list.filtrosActivos");
//        if (filterListUI != null) {
//            DefaultListModel<FilterCriterion> filterListModel = new DefaultListModel<>();
//            filterListModel.addAll(filterManager.getActiveFilters());
//            filterListUI.setModel(filterListModel);
//        }
//
//        // 2. Comprobar si hay filtros persistentes para aplicar
//        if (!filterManager.getActiveFilters().isEmpty()) {
//            // --- ACTIVAR MODO FILTRO PERSISTENTE ---
//            logger.debug("[Filtro Persistente] Activando modo. Hay {} filtros.", filterManager.getActiveFilters().size());
//            
//            // 2a. Guardar checkpoint del contexto real, SI NO LO HEMOS HECHO YA
//            if (!filtroPersistenteActivo) {
//                logger.info("[Filtro Persistente] Creando checkpoint del contexto real.");
//                ListContext contextoReal = model.getCurrentListContext();
//                
//                // Clonar el modelo de lista para no tener referencias cruzadas
//                contextoRealGuardado_modelo = new DefaultListModel<>();
//                DefaultListModel<String> modeloOriginal = contextoReal.getModeloLista();
//                if(modeloOriginal != null) {
//                    for(int i=0; i < modeloOriginal.getSize(); i++) {
//                        contextoRealGuardado_modelo.addElement(modeloOriginal.getElementAt(i));
//                    }
//                }
//
//                contextoRealGuardado_mapaRutas = new HashMap<>(contextoReal.getRutaCompletaMap());
//                contextoRealGuardado_punteroKey = contextoReal.getSelectedImageKey();
//                filtroPersistenteActivo = true;
//            }
//            
//            // 2b. Crear la "carpeta virtual" filtrando desde el checkpoint
//            DefaultListModel<String> listaVirtual = filterManager.applyFilters(contextoRealGuardado_modelo);
//            Map<String, Path> mapaVirtual = new HashMap<>();
//            for (String key : Collections.list(listaVirtual.elements())) {
//                mapaVirtual.put(key, contextoRealGuardado_mapaRutas.get(key));
//            }
//
//            // 2c. ¡EL GRAN INTERCAMBIO! Simulamos la carga de una nueva carpeta
//            visorController.cargarListaDesdeFiltro(new FilterManager.FilterResult(listaVirtual, mapaVirtual), null);
//
//        } else {
//            // --- DESACTIVAR MODO FILTRO PERSISTENTE ---
//            if (filtroPersistenteActivo) {
//                logger.info("[Filtro Persistente] Desactivando modo. Restaurando checkpoint.");
//                
//                // 2d. Restaurar el checkpoint
//                visorController.cargarListaDesdeFiltro(new FilterManager.FilterResult(contextoRealGuardado_modelo, contextoRealGuardado_mapaRutas), () -> {
//                    // Callback que se ejecuta DESPUÉS de la carga: restaurar el puntero
//                    if (visorController != null && visorController.getListCoordinator() != null && contextoRealGuardado_punteroKey != null) {
//                        int indiceOriginal = contextoRealGuardado_modelo.indexOf(contextoRealGuardado_punteroKey);
//                        if(indiceOriginal != -1) {
//                             visorController.getListCoordinator().seleccionarImagenPorIndice(indiceOriginal);
//                        }
//                    }
//                });
//
//                // 2e. Limpiar el estado
//                contextoRealGuardado_modelo = null;
//                contextoRealGuardado_mapaRutas = null;
//                contextoRealGuardado_punteroKey = null;
//                filtroPersistenteActivo = false;
//            }
//        }
//    } // --- Fin del método refrescarVistasDeFiltros ---
    
    
    /**
     * Es llamado por la ToggleLiveFilterAction cuando el estado del filtro cambia.
     * Orquesta la actualización de la UI aplicando o limpiando el filtro.
     * @param isSelected El nuevo estado del modo filtro.
     */
    public void onLiveFilterStateChanged(boolean isSelected) {
        model.setLiveFilterActive(isSelected);
        logger.debug("[GeneralController] Modo Filtro en Vivo cambiado a: {}", isSelected);
        
        if (isSelected) {
            // Guardamos el estado original ANTES de aplicar el primer filtro
            this.masterModelSinFiltro = new DefaultListModel<>();
            DefaultListModel<String> modeloActual = model.getModeloLista();
            for(int i = 0; i < modeloActual.getSize(); i++){
                this.masterModelSinFiltro.addElement(modeloActual.getElementAt(i));
            }
            this.indiceSeleccionadoAntesDeFiltrar = visorController.getListCoordinator().getOfficialSelectedIndex();
            
            actualizarFiltro(); // Aplicar filtro con el texto actual
        } else {
            limpiarFiltro(); // Volver a la lista maestra
        }
        
        Action liveFilterAction = actionMap.get(AppActionCommands.CMD_FILTRO_TOGGLE_LIVE_FILTER);
        if (configAppManager != null && liveFilterAction != null) {
            configAppManager.actualizarAspectoBotonToggle(liveFilterAction, isSelected);
        }
        
    } // --- Fin del método onLiveFilterStateChanged ---
    

    public JPopupMenu crearMenuContextualParaArbol() {
        JPopupMenu menu = new JPopupMenu();
        
        Action openAction = this.actionMap.get(AppActionCommands.CMD_TREE_OPEN_FOLDER);
        Action drillDownAction = this.actionMap.get(AppActionCommands.CMD_TREE_DRILL_DOWN_FOLDER);
        
        if (openAction != null) {
            menu.add(new JMenuItem(openAction));
        }
        if (drillDownAction != null) {
            menu.add(new JMenuItem(drillDownAction));
        }
        
        return menu;
    } // --- Fin del método crearMenuContextualParaArbol ---
    
    
    public void solicitarAbrirCarpetaDesdeArbol() {
        if (folderTreeManager != null) {
            folderTreeManager.handleOpenFolderAction();
        }
    } // --- Fin del método solicitarAbrirCarpetaDesdeArbol ---

    public void solicitarEntrarEnCarpetaDesdeArbol() {
        if (folderTreeManager != null) {
            folderTreeManager.handleDrillDownFolderAction();
        }
    } // --- Fin del método solicitarEntrarEnCarpetaDesdeArbol ---
    
    
    public void solicitarNavegarCarpetaAnterior() {
        if (folderNavManager != null) {
            // Esta llamada ahora es inteligente: usará el historial si puede,
            // o subirá al padre si no puede.
            folderNavManager.navegarACarpetaPadre();
        } else {
            logger.error("FolderNavigationManager no está inicializado.");
        }
    } // --- FIN del metodo solicitarNavegarCarpetaAnterior ---
    
    /**
     * Delega la solicitud de "entrar" en una subcarpeta al FolderNavigationManager.
     * Este método es llamado por la Action correspondiente.
     */
    public void solicitarNavegarCarpetaSiguiente() {
        if (folderNavManager != null) {
            folderNavManager.entrarEnSubcarpeta();
        } else {
            logger.error("[GeneralController] FolderNavigationManager no está inicializado. No se puede entrar en subcarpeta.");
        }
    } // --- Fin del método solicitarNavegarCarpetaSiguiente ---

    /**
     * Delega la solicitud de volver a la carpeta raíz al FolderNavigationManager.
     * Este método es llamado por la Action correspondiente.
     */
    public void solicitarNavegarCarpetaRaiz() {
        if (folderNavManager != null) {
            folderNavManager.volverACarpetaRaiz();
        } else {
            logger.error("[GeneralController] FolderNavigationManager no está inicializado. No se puede volver a la raíz.");
        }
    } // --- Fin del método solicitarNavegarCarpetaRaiz ---
    
    
    public void setFolderNavigationManager(FolderNavigationManager folderNavManager) {
        this.folderNavManager = Objects.requireNonNull(folderNavManager);
    } // --- FIN del metodo setFolderNavigationManager ---
    
    public void solicitarSalirDeSubcarpeta() {
        if (folderNavManager != null) {
            folderNavManager.salirDeSubcarpetaConHistorial();
        } else {
            logger.error("[GeneralController] FolderNavigationManager no está inicializado.");
        }
    }// --- FIN del metodo solicitarSalirDeSubcarpeta ---

    
    /**
     * Comanda a la VisorView para que actualice su borde visual de sincronización.
     * Este método actúa como un puente seguro entre las acciones y la vista.
     * @param activado El nuevo estado de sincronización.
     */
    public void actualizarBordeDeSincronizacion(boolean activado) {
        if (visorController != null && visorController.getView() != null) {
            visorController.getView().actualizarBordeDeSincronizacion(activado);
        }
    } // --- Fin del método actualizarBordeDeSincronizacion ---
    
    
    public void setFolderTreeManager(FolderTreeManager folderTreeManager) {
        this.folderTreeManager = Objects.requireNonNull(folderTreeManager);
    } // --- FIN del metodo setFolderTreeManager ---
    
    
    public void setFilterManager(FilterManager filterManager) {
        this.filterManager = Objects.requireNonNull(filterManager, "FilterManager no puede ser null en GeneralController");
    } // --- Fin del método setFilterManager ---
    
    
    public ComponentRegistry getRegistry() {
        return this.registry;
    }
    
    public FilterSource getFiltroActivoSource() {
        return this.filtroActivoSource;
    } // --- Fin del método getFiltroActivoSource ---


    @Override
    public void solicitarRefresco() {
        // Esta implementación del método de la interfaz simplemente llama
        // a nuestro método "router" más descriptivo.
        solicitarRefrescoDelModoActivo();
    }// FIN del metodo solicitarRefresco ---
    
    
    /**
     * Implementación de la interfaz MasterListChangeListener.
     * Este método es el "cartero" central. Se ejecuta cada vez que VisorModel
     * notifica un cambio en su lista maestra. Su única responsabilidad es
     * tomar esa nueva lista y entregarla al grid del modo de trabajo activo.
     * @param newMasterList El nuevo modelo de lista que se debe mostrar.
     * @param source El objeto que originó el cambio, para evitar bucles.
     */
    @Override
    public void onMasterListChanged(DefaultListModel<String> newMasterList, Object source) {
        if (registry == null || model == null) {
            logger.warn("WARN [onMasterListChanged]: Registry o Model nulos. No se puede actualizar el grid.");
            return;
        }

        JList<String> gridTarget;
        WorkMode currentMode = model.getCurrentWorkMode();

        if (currentMode == WorkMode.PROYECTO) {
            gridTarget = registry.get("list.grid.proyecto");
        } else {
            // --- CORRECCIÓN: Usamos la clave correcta "list.grid" ---
            gridTarget = registry.get("list.grid");
        }

        if (gridTarget != null) {
            SwingUtilities.invokeLater(() -> {
                gridTarget.setModel(newMasterList);
                logger.debug("[MasterListChangeListener] Grid para modo {} actualizado con {} elementos.", currentMode, newMasterList.getSize());
            });
        } else {
            logger.error("ERROR [onMasterListChanged]: No se encontró el JList del grid para el modo {}.", currentMode);
        }
    } // --- Fin del método onMasterListChanged ---
    
    
    private void registerFocusablePanel(String registryKey) {
        javax.swing.JComponent panel = registry.get(registryKey);
        if (panel != null) {
            focusablePanels.add(panel);
        } else {
            logger.warn("[FOCUS_INIT] No se pudo registrar el panel para foco: '{}'", registryKey);
        }
    } // --- FIN de metodo registerFocusablePanel ---
    
    
} // --- Fin de la clase GeneralController ---



