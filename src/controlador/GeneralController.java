package controlador;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;

import controlador.commands.AppActionCommands;
import controlador.managers.ConfigApplicationManager;
import controlador.managers.InfobarStatusManager;
import controlador.managers.ViewManager;
import modelo.VisorModel;


/**
 * Controlador de aplicación de alto nivel.
 * Orquesta la interacción entre los controladores de modo (VisorController, ProjectController)
 * y gestiona el estado global de la aplicación, como el modo de trabajo actual y la
 * habilitación/deshabilitación de la UI correspondiente.
 */
public class GeneralController {

    // --- Dependencias Clave ---
    private VisorModel model;
    private VisorController visorController;
    private ProjectController projectController;
    private ViewManager viewManager;
    private Map<String, Action> actionMap;
    private InfobarStatusManager statusBarManager;
    private ConfigApplicationManager configAppManager;

    /**
     * Constructor de GeneralController.
     * Las dependencias se inyectarán a través de setters después de la creación.
     */
    public GeneralController() {
        // Constructor vacío. La inicialización se delega al método initialize.
    } // --- Fin del método GeneralController (constructor) ---

    /**
     * Inicializa el controlador después de que todas las dependencias hayan sido inyectadas.
     * Este método se usa para configurar el estado inicial de la UI.
     */
    public void initialize() {
        System.out.println("[GeneralController] Inicializado.");
        // Se asegura de que al arrancar, el botón del modo inicial (Visualizador) aparezca correctamente seleccionado.
        sincronizarEstadoBotonesDeModo();
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
    
//****************************************************************************************** Fin Setters
    
    /**
     * Orquesta la transición entre los diferentes modos de trabajo de la aplicación.
     * Es el punto de entrada central para cambiar de vista.
     * @param modoDestino El modo al que se desea cambiar (VISUALIZADOR o PROYECTO).
     */
    public void cambiarModoDeTrabajo(VisorModel.WorkMode modoDestino) {
        VisorModel.WorkMode modoActual = this.model.getCurrentWorkMode();
        if (modoActual == modoDestino) {
            System.out.println("[GeneralController] Intento de cambiar al modo que ya está activo: " + modoDestino + ". No se hace nada.");
            return; // Ya estamos en el modo deseado
        }

        System.out.println("\n--- [GeneralController] INICIANDO TRANSICIÓN DE MODO: " + modoActual + " -> " + modoDestino + " ---");

        // Lógica de pre-transición (ej. validar si el modo proyecto está listo)
        if (modoDestino == VisorModel.WorkMode.PROYECTO) {
            if (!this.projectController.prepararDatosProyecto()) {
                // Si el modo proyecto no está listo, no cambiamos y sincronizamos la UI
                // para que refleje que seguimos en el modo visualizador.
                sincronizarEstadoBotonesDeModo();
                System.out.println("--- [GeneralController] TRANSICIÓN CANCELADA: El modo proyecto no está listo. ---");
                return;
            }
        }
        
        // Ejecutar tareas de salida del modo actual
        salirModo(modoActual);
        
        // Cambiar el estado en el modelo
        this.model.setCurrentWorkMode(modoDestino);
        
        // Ejecutar tareas de entrada al nuevo modo
        entrarModo(modoDestino);

        System.out.println("--- [GeneralController] TRANSICIÓN DE MODO COMPLETADA a " + modoDestino + " ---\n");
    } // --- Fin del método cambiarModoDeTrabajo ---

    /**
     * Realiza las tareas de "limpieza" o "desactivación" de un modo antes de abandonarlo.
     * @param modoQueSeAbandona El modo que estamos dejando.
     */
	private void salirModo(VisorModel.WorkMode modoQueSeAbandona) {
        System.out.println("  [GeneralController] Saliendo del modo: " + modoQueSeAbandona);
        // Aquí iría la lógica común de salida si la hubiera.
        // Por ahora, solo se loguea.
    } // --- Fin del método salirModo ---

    /**
     * Realiza las tareas de "configuración" y "restauración" de la UI para un modo
     * en el que estamos entrando. Llama al método centralizador para ajustar la UI.
     * @param modoAlQueSeEntra El nuevo modo activo.
     */
	private void entrarModo(VisorModel.WorkMode modoAlQueSeEntra) {
        System.out.println("  [GeneralController] Entrando en modo: " + modoAlQueSeEntra);
        
        // Lógica específica de preparación de cada controlador
        switch (modoAlQueSeEntra) {
            case VISUALIZADOR:
                this.visorController.restaurarUiVisualizador();
                break;
            case PROYECTO:
                this.projectController.activarVistaProyecto();
                break;
        }

        // --- LÓGICA CENTRALIZADA PARA LA UI ---
        // Después de que el controlador específico ha preparado sus datos,
        // ajustamos el estado global de la UI.
        actualizarEstadoUiParaModo(modoAlQueSeEntra);
        
        // Cambiar la "página" visible en el CardLayout
        this.viewManager.cambiarAVista(modoAlQueSeEntra == VisorModel.WorkMode.PROYECTO ? "VISTA_PROYECTOS" : "VISTA_VISUALIZADOR");
        
        // Sincronizar los botones que indican el modo activo
        sincronizarEstadoBotonesDeModo();
    } // --- Fin del método entrarModo ---
    
    /**
     * MÉTODO CENTRALIZADOR: Habilita o deshabilita acciones y componentes de la UI
     * basándose en el modo de trabajo actual. Este es el "interruptor general"
     * para la interfaz.
     * 
     * @param modoActual El modo que se acaba de activar.
     */
    private void actualizarEstadoUiParaModo(VisorModel.WorkMode modoActual) {
        System.out.println("  [GeneralController] Actualizando estado de la UI para el modo: " + modoActual);
        boolean enModoVisualizador = (modoActual == VisorModel.WorkMode.VISUALIZADOR);

        // --- CONTROL DE LA FUNCIONALIDAD DE SUBCARPETAS ---
        Action subfolderAction = this.actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
        if (subfolderAction != null) {
            // La funcionalidad de explorar subcarpetas solo tiene sentido en el modo visualizador.
            subfolderAction.setEnabled(enModoVisualizador);
        }

        Action soloCarpetaAction = this.actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_SOLO_CARPETA);
        if (soloCarpetaAction != null) {
            soloCarpetaAction.setEnabled(enModoVisualizador);
        }

        Action conSubcarpetasAction = this.actionMap.get(AppActionCommands.CMD_CONFIG_CARGA_CON_SUBCARPETAS);
        if (conSubcarpetasAction != null) {
            conSubcarpetasAction.setEnabled(enModoVisualizador);
        }
        
        // Aquí se pueden añadir más reglas para otras acciones en el futuro
        // Ejemplo:
        // Action edicionAction = this.actionMap.get(AppActionCommands.CMD_IMAGEN_RECORTAR);
        // if(edicionAction != null) {
        //     edicionAction.setEnabled(enModoVisualizador);
        // }
        
        // --- LLAMADA PARA ACTUALIZAR LA BARRA DE ESTADO ---
        if (this.statusBarManager != null) {
            this.statusBarManager.actualizar();
        }
        
        System.out.println("  [GeneralController] Estado de la UI actualizado.");
    } // --- Fin del método actualizarEstadoUiParaModo ---
    
    /**
	 * Sincroniza el estado LÓGICO Y VISUAL de los botones de modo de trabajo.
	 * Asegura que solo el botón del modo activo esté seleccionado y que se aplique
     * el estilo visual personalizado.
	 */
	public void sincronizarEstadoBotonesDeModo() {
	    if (this.actionMap == null || this.model == null || this.configAppManager == null) {
	        System.err.println("WARN [GeneralController.sincronizarEstadoBotonesDeModo]: Dependencias nulas (ActionMap, Model o ConfigAppManager).");
	        return;
	    }

	    String comandoModoActivo = this.model.isEnModoProyecto()
	                               ? AppActionCommands.CMD_PROYECTO_GESTIONAR
	                               : AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR;

	    List<String> comandosDeModo = List.of(
	        AppActionCommands.CMD_PROYECTO_GESTIONAR,
	        AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR
	        // Añadir aquí el comando para el "Modo Datos" cuando exista.
	    );

	    for (String comando : comandosDeModo) {
	        Action action = this.actionMap.get(comando);
	        if (action != null) {
                // 1. Actualizar el estado lógico de la Action
	            boolean isSelected = comando.equals(comandoModoActivo);
	            action.putValue(Action.SELECTED_KEY, isSelected);

                // 2. Actualizar el estado visual del botón asociado
                this.configAppManager.actualizarAspectoBotonToggle(action, isSelected);
	        }
	    }
	    System.out.println("[GeneralController] Sincronizados botones de modo. Activo: " + comandoModoActivo);
	} // --- Fin del método sincronizarEstadoBotonesDeModo ---
    
    

} // --- Fin de la clase GeneralController ---