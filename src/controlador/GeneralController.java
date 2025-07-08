package controlador;

import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import controlador.commands.AppActionCommands;
import controlador.interfaces.IModoController;
import controlador.managers.ConfigApplicationManager;
import controlador.managers.InfobarStatusManager;
import controlador.managers.ToolbarManager; // <-- Importación necesaria
import controlador.managers.ViewManager;
import controlador.utils.ComponentRegistry; // <-- NUEVO: Importación para ComponentRegistry
import modelo.VisorModel;
import modelo.VisorModel.WorkMode; // <-- Importación necesaria
import vista.components.Direction; // <-- NUEVO: Importación para Direction
import vista.panels.ImageDisplayPanel; // <-- NUEVO: Importación para ImageDisplayPanel


/**
 * Controlador de aplicación de alto nivel.
 * Orquesta la interacción entre los controladores de modo (VisorController, ProjectController)
 * y gestiona el estado global de la aplicación, como el modo de trabajo actual y la
 * habilitación/deshabilitación de la UI correspondiente.
 */
public class GeneralController implements IModoController, KeyEventDispatcher{

    // --- Dependencias Clave ---
    private VisorModel model;
    private VisorController visorController;
    private ProjectController projectController;
    private ViewManager viewManager;
    private Map<String, Action> actionMap;
    private InfobarStatusManager statusBarManager;
    private ConfigApplicationManager configAppManager;
    private ToolbarManager toolbarManager;
    private ComponentRegistry registry; // <-- NUEVO: Referencia al ComponentRegistry
    
    private int lastMouseX, lastMouseY;

    
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
    
    public void setToolbarManager(ToolbarManager toolbarManager) {
        this.toolbarManager = Objects.requireNonNull(toolbarManager, "ToolbarManager no puede ser null en GeneralController");
    } // --- Fin del método setToolbarManager ---

    public void setRegistry(ComponentRegistry registry) { // <-- NUEVO SETTER
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null en GeneralController");
    } // --- Fin del método setRegistry ---

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
	private void entrarModo(WorkMode modoAlQueSeEntra) {
        System.out.println("  [GeneralController] Entrando en modo: " + modoAlQueSeEntra);
        
        // Lógica específica de preparación de cada controlador
        switch (modoAlQueSeEntra) {
            case VISUALIZADOR:
                this.visorController.restaurarUiVisualizador();
                break;
            case PROYECTO:
                this.projectController.activarVistaProyecto();
                break;
            case DATOS:
                // Lógica para el modo datos en el futuro
                break;
        }

        // --- LÓGICA CENTRALIZADA PARA LA UI ---
        // Después de que el controlador específico ha preparado sus datos,
        // ajustamos el estado global de la UI.
        actualizarEstadoUiParaModo(modoAlQueSeEntra);
        
        // Cambiar la "página" visible en el CardLayout
        this.viewManager.cambiarAVista(modoAlQueSeEntra == VisorModel.WorkMode.PROYECTO ? "VISTA_PROYECTOS" : "VISTA_VISUALIZADOR");
        
        // --- LLAMADA AÑADIDA para reconstruir las barras de herramientas ---
        if (this.toolbarManager != null) {
            this.toolbarManager.reconstruirContenedorDeToolbars(modoAlQueSeEntra);
        }
        
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
    private void actualizarEstadoUiParaModo(WorkMode modoActual) {
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


    // --- INICIO BLOQUE DE MODIFICACIÓN ---
    // Línea anterior: public void sincronizarEstadoBotonesDeModo() { ... }

    /**
     * Panea la imagen al borde especificado del panel de visualización.
     * Esta es la lógica de paneo ABSOLUTO.
     * @param direction La dirección (UP, DOWN, LEFT, RIGHT) a la que panear la imagen.
     */
    public void panImageToEdge(Direction direction) {
        System.out.println("[GeneralController] Solicitud de paneo ABSOLUTO a: " + direction);
        if (model == null || model.getCurrentImage() == null || registry == null) {
            System.err.println("ERROR [GeneralController.panImageToEdge]: Dependencias nulas o sin imagen actual.");
            return;
        }

        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen"); // Obtener el panel activo.
        // TODO: Si tu aplicación usa múltiples paneles de visualización (ej. para modo proyecto),
        // necesitas una lógica más sofisticada aquí para obtener el panel CORRECTO.
        // Una forma sería: ImageDisplayPanel displayPanel = (model.getCurrentWorkMode() == WorkMode.PROYECTO) ? registry.get("panel.proyecto.display") : registry.get("panel.display.imagen");

        if (displayPanel == null || displayPanel.getWidth() <= 0 || displayPanel.getHeight() <= 0) {
            System.err.println("ERROR [GeneralController.panImageToEdge]: ImageDisplayPanel no encontrado o sin dimensiones válidas.");
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
        System.out.println("[GeneralController] Paneo absoluto a " + direction + " aplicado. Offset: (" + newOffsetX + ", " + newOffsetY + ")");
    } // --- Fin del método panImageToEdge ---


    /**
     * Panea la imagen de forma incremental en la dirección especificada por una cantidad fija.
     * Esta es la lógica de paneo INCREMENTAL.
     * @param direction La dirección (UP, DOWN, LEFT, RIGHT) del paneo incremental.
     * @param amount La cantidad de píxeles a mover en cada paso.
     */
    public void panImageIncrementally(Direction direction, int amount) {
        System.out.println("[GeneralController] Solicitud de paneo INCREMENTAL (" + amount + "px) a: " + direction);
        if (model == null || model.getCurrentImage() == null || registry == null) {
            System.err.println("ERROR [GeneralController.panImageIncrementally]: Dependencias nulas o sin imagen actual.");
            return;
        }

        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen"); // Obtener el panel activo
        // TODO: Igual que arriba, si hay múltiples paneles de display, obtener el correcto.
        if (displayPanel == null || displayPanel.getWidth() <= 0 || displayPanel.getHeight() <= 0) {
            System.err.println("ERROR [GeneralController.panImageIncrementally]: ImageDisplayPanel no encontrado o sin dimensiones válidas.");
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
        System.out.println("[GeneralController] Paneo incremental aplicado. Offset: (" + newOffsetX + ", " + newOffsetY + ")");
    } // --- Fin del método panImageIncrementally ---
    
    /**
     * Actúa como un router para la acción de marcar/desmarcar una imagen.
     * Delega la solicitud al controlador del modo de trabajo activo.
     */
    public void solicitudAlternarMarcaImagenActual() {
        System.out.println("[GeneralController] Recibida solicitud para alternar marca. Modo actual: " + model.getCurrentWorkMode());
        if (model.isEnModoProyecto()) {
            projectController.solicitudAlternarMarcaImagen();
        } else {
            visorController.solicitudAlternarMarcaDeImagenActual();
        }
    } // --- Fin del método solicitudAlternarMarcaImagenActual ---
    
    
//  ************************************************************************************** IMPLEMENTACION INTERFAZ IModoController
    
    /**
     * Configura los listeners globales de teclado y ratón para la aplicación.
     * MODIFICADO: Ahora adjunta listeners a los componentes de AMBOS modos (Visualizador y Proyecto).
     */
    public void configurarListenersDeEntradaGlobal() {
        System.out.println("[GeneralController] Configurando listeners de entrada globales para todos los modos...");

        // --- Obtener componentes de la UI desde el registro para AMBOS modos ---
        
        // Componentes del modo VISUALIZADOR
        JLabel etiquetaImagenVisualizador = registry.get("label.imagenPrincipal");
        JList<String> listaNombresVisualizador = registry.get("list.nombresArchivo");
        JScrollPane scrollMiniaturas = registry.get("scroll.miniaturas");

        // Componentes del modo PROYECTO
        ImageDisplayPanel panelDisplayProyecto = registry.get("panel.proyecto.display");
        JLabel etiquetaImagenProyecto = (panelDisplayProyecto != null) ? panelDisplayProyecto.getInternalLabel() : null;
        JList<String> listaNombresProyecto = registry.get("list.proyecto.nombres");

        // --- Master Mouse Wheel Listener (sin cambios en su lógica interna) ---
        java.awt.event.MouseWheelListener masterWheelListener = e -> {
            boolean sobreLaImagen = (etiquetaImagenVisualizador != null && e.getComponent() == etiquetaImagenVisualizador) ||
                                    (etiquetaImagenProyecto != null && e.getComponent() == etiquetaImagenProyecto);

            if (e.isControlDown() && e.isAltDown()) {
                if (e.getWheelRotation() < 0) this.navegarBloqueAnterior();
                else this.navegarBloqueSiguiente();
            } else if (sobreLaImagen && model.isZoomHabilitado()) {
                if (e.isControlDown() && !e.isShiftDown()) {
                    this.aplicarPan(0, e.getWheelRotation() * 30);
                } else if (e.isShiftDown() && !e.isControlDown()) {
                    this.aplicarPan(-e.getWheelRotation() * 30, 0);
                } else {
                    this.aplicarZoomConRueda(e);
                }
            } else {
                this.navegarSiguienteOAnterior(e.getWheelRotation());
            }
            e.consume();
        };

        // --- Adjuntar el master MouseWheelListener a los componentes de AMBOS modos ---
        Component[] componentesConRueda = {
            listaNombresVisualizador, scrollMiniaturas, etiquetaImagenVisualizador,
            listaNombresProyecto, etiquetaImagenProyecto
        };

        for (Component c : componentesConRueda) {
            if (c != null) {
                // Es seguro añadir el listener aunque ya exista; no se duplicará si es la misma instancia.
                c.addMouseWheelListener(masterWheelListener);
            }
        }

        // --- Listeners de clic y arrastre para paneo para AMBAS etiquetas de imagen ---
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
        
        if (etiquetaImagenVisualizador != null) {
            etiquetaImagenVisualizador.addMouseListener(paneoMouseAdapter);
            etiquetaImagenVisualizador.addMouseMotionListener(paneoMouseMotionAdapter);
        }
        if (etiquetaImagenProyecto != null) {
            etiquetaImagenProyecto.addMouseListener(paneoMouseAdapter);
            etiquetaImagenProyecto.addMouseMotionListener(paneoMouseMotionAdapter);
        }

        System.out.println("[GeneralController] Listeners de entrada globales configurados para todos los modos.");
        
    } // --- Fin del método configurarListenersDeEntradaGlobal ---
    

    /**
     * Implementación del método de la interfaz KeyEventDispatcher.
     * Intercepta eventos de teclado globales, movida desde VisorController.
     * @param e El KeyEvent a procesar.
     * @return true si el evento fue consumido, false para continuar.
     */
    @Override // ESTO ES UNA IMPLEMENTACIÓN DE LA INTERFAZ KeyEventDispatcher
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getID() != KeyEvent.KEY_PRESSED) {
            return false;
        }

        // --- MANEJO ESPECIAL Y SEGURO DE LA TECLA ALT (movido de VisorController) ---
        if (e.getKeyCode() == KeyEvent.VK_ALT) {
            // Necesita acceso a la vista, se obtiene a través de visorController.getView()
            if (visorController != null && visorController.getView() != null && visorController.getView().getJMenuBar() != null) {
                JMenuBar menuBar = visorController.getView().getJMenuBar();
                if (menuBar.isSelected()) { // Comprueba si algún menú ya está abierto (seleccionado)
                    menuBar.getSelectionModel().clearSelection(); // Cierra la selección actual
                    System.out.println("--- [GeneralController Dispatcher] ALT: Menú ya activo. Cerrando selección.");
                } else {
                    if (menuBar.getMenuCount() > 0) { // Si no hay ningún menú activo, activamos el primero.
                        JMenu primerMenu = menuBar.getMenu(0);
                        if (primerMenu != null) {
                        	
                        	//LOG [GeneralController Dispatcher] ALT: Simulando clic en el menú 'Archivo'
                            //System.out.println("--- [GeneralController Dispatcher] ALT: Simulando clic en el menú 'Archivo'...");
                        	
                            primerMenu.doClick(); // Simula un clic del ratón, abriendo el menú.
                        }
                    }
                }
                e.consume(); // Consumimos el evento ALT
                return true;
            }
        }
        
        // Continuación de la lógica de navegación por teclado (movido de VisorController)
        if (model == null || registry == null) { // Dependencias mínimas
            return false;
        }

        java.awt.Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
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
                    this.navegarAnterior(); // Delega a los métodos IModoController de GeneralController
                    consumed = true;
                    break;
                case KeyEvent.VK_DOWN: case KeyEvent.VK_RIGHT:
                    this.navegarSiguiente(); // Delega a los métodos IModoController de GeneralController
                    consumed = true;
                    break;
                case KeyEvent.VK_HOME:
                    this.navegarPrimero(); // Delega a los métodos IModoController de GeneralController
                    consumed = true;
                    break;
                case KeyEvent.VK_END:
                    this.navegarUltimo(); // Delega a los métodos IModoController de GeneralController
                    consumed = true;
                    break;
                case KeyEvent.VK_PAGE_UP:
                    this.navegarBloqueAnterior(); // Delega a los métodos IModoController de GeneralController
                    consumed = true;
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    this.navegarBloqueSiguiente(); // Delega a los métodos IModoController de GeneralController
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

    // --- Implementación de IModoController (delegando al controlador de modo activo) ---
    // NOTA: La lógica interna de estos métodos seguirá residiendo en VisorController y ProjectController
    // tal como están ahora. GeneralController solo actúa como un router.

    @Override // ESTO ES UNA IMPLEMENTACIÓN DE LA INTERFAZ IModoController
    public void navegarSiguiente() {
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR) {
            visorController.navegarSiguiente();
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.navegarSiguiente();
        }
        
        //LOG [GeneralController] Delegando navegarSiguiente
        //System.out.println("[GeneralController] Delegando navegarSiguiente a " + model.getCurrentWorkMode());
        
    }// --- FIN del metodo navegarSiguiente ---

    
    @Override // ESTO ES UNA IMPLEMENTACIÓN DE LA INTERFAZ IModoController
    public void navegarAnterior() {
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR) {
            visorController.navegarAnterior();
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.navegarAnterior();
        }
        // LOG [GeneralController] Delegando navegarAnterior
        //System.out.println("[GeneralController] Delegando navegarAnterior a " + model.getCurrentWorkMode());
        
    }// --- FIN del metodo navegarAnterior ---

    
    @Override // ESTO ES UNA IMPLEMENTACIÓN DE LA INTERFAZ IModoController
    public void navegarPrimero() {
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR) {
            visorController.navegarPrimero();
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.navegarPrimero();
        }
        
        // LOG [GeneralController] Delegando navegarPrimero
        //System.out.println("[GeneralController] Delegando navegarPrimero a " + model.getCurrentWorkMode());
    
    }// --- FIN del metodo navegarPrimero ---
    

    @Override // ESTO ES UNA IMPLEMENTACIÓN DE LA INTERFAZ IModoController
    public void navegarUltimo() {
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR) {
            visorController.navegarUltimo();
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.navegarUltimo();
        }
        // LOG [GeneralController] Delegando navegarUltimo
        //System.out.println("[GeneralController] Delegando navegarUltimo a " + model.getCurrentWorkMode());
    
    }// --- FIN del metodo navegarUltimo ---
    

    @Override // ESTO ES UNA IMPLEMENTACIÓN DE LA INTERFAZ IModoController
    public void navegarBloqueAnterior() {
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR) {
            visorController.navegarBloqueAnterior();
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.navegarBloqueAnterior();
        }
        
        //LOG [GeneralController] Delegando navegarBloqueAnterior
        //System.out.println("[GeneralController] Delegando navegarBloqueAnterior a " + model.getCurrentWorkMode());
    
    }// --- FIN del metodo navegarBloqueAnterior ---
    

    @Override // ESTO ES UNA IMPLEMENTACIÓN DE LA INTERFAZ IModoController
    public void navegarBloqueSiguiente() {
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR) {
            visorController.navegarBloqueSiguiente();
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.navegarBloqueSiguiente();
        }
        //LOG [GeneralController] Delegando navegarBloqueSiguiente
        //System.out.println("[GeneralController] Delegando navegarBloqueSiguiente a " + model.getCurrentWorkMode());
    
    }// --- FIN del metodo navegarBloqueSiguiente ---
    

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
        }
        
        //LOG [GeneralController] Delegando aplicarZoomConRueda
        //System.out.println("[GeneralController] Delegando aplicarZoomConRueda a " + model.getCurrentWorkMode());
    
    }// --- FIN del metodo aplicarZoomConRueda ---
    

    @Override // ESTO ES UNA IMPLEMENTACIÓN DE LA INTERFAZ IModoController
    public void aplicarPan(int deltaX, int deltaY) {
        // La lógica de cálculo del pan (cuánto se mueve) reside en ZoomManager.
        // Aquí solo delegamos la acción de pan al controlador de modo activo.
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR) {
            visorController.aplicarPan(deltaX, deltaY);
        } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            projectController.aplicarPan(deltaX, deltaY);
        }
        //LOG [GeneralController] Delegando aplicarPan
        //System.out.println("[GeneralController] Delegando aplicarPan a " + model.getCurrentWorkMode());
    
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
        }
        
        //LOG [GeneralController] Delegando iniciarPaneo
        //System.out.println("[GeneralController] Delegando iniciarPaneo a " + model.getCurrentWorkMode());
    
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
        }
        
        //LOG [GeneralController] Delegando continuarPaneo
        //System.out.println("[GeneralController] Delegando continuarPaneo a " + model.getCurrentWorkMode());
    
    }// --- FIN del metodo continuarPaneo ---
    
    
//  ********************************************************************************** FIN IMPLEMENTACION INTERFAZ IModoController
    
//  *************************************************************************************************************** INICIO GETTERS    
    
    public ToolbarManager getToolbarManager() {
        return this.toolbarManager;
    }


//  ****************************************************************************************************************** FIN GETTERS
    
} // --- Fin de la clase GeneralController ---


