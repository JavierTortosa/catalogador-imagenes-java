package controlador;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import controlador.interfaces.IModoController;
import controlador.managers.DisplayModeManager;
import controlador.managers.ExportQueueManager;
import controlador.managers.interfaces.IListCoordinator;
import controlador.managers.interfaces.IProjectManager;
import controlador.managers.interfaces.IViewManager;
import controlador.managers.interfaces.IZoomManager;
import controlador.utils.ComponentRegistry;
import controlador.utils.DesktopUtils;
import controlador.worker.ExportWorker;
import modelo.ListContext;
import modelo.VisorModel;
import modelo.proyecto.ExportItem;
import modelo.proyecto.ExportStatus;
import modelo.proyecto.ProjectModel;
import vista.VisorView;
import vista.dialogos.TaskProgressDialog;
import vista.panels.export.ExportDetailPanel;
import vista.panels.export.ExportPanel;
import vista.panels.export.ExportTableModel;

	
public class ProjectController implements IModoController {

	private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
	
	/**
	 * Define los posibles estados de visualización del panel de proyecto.
	 * Esto reemplaza el uso de flags booleanos complejos.
	 */
	private enum ProjectViewState {
	    VIEW_SELECTION, // Foco en la lista de Selección, Grid normal
	    VIEW_DISCARDS,  // Foco en la lista de Descartes, Grid normal
	    VIEW_EXPORT     // Panel de exportación activo, Grid muestra Selección con bordes de estado
	}

	private ProjectViewState currentViewState = ProjectViewState.VIEW_SELECTION; // Estado inicial
	
    private ComponentRegistry registry;
    private VisorView view;
    private VisorModel model;
    private ExportQueueManager exportQueueManager;
    private ProjectListCoordinator projectListCoordinator;
    private DisplayModeManager displayModeManager;
    private GeneralController generalController;
    
    private IProjectManager projectManager;
    private IZoomManager zoomManager;
    private IViewManager viewManager;
    private IListCoordinator listCoordinator; 
    
    private Map<String, Action> actionMap;
    private Map<String, ExportItem> exportItemMap = new HashMap<>();
    private int lastRightDividerLocation = -1;


    public ProjectController() {
        logger.debug("[ProjectController] Instancia creada.");
        this.exportQueueManager = new ExportQueueManager();
    } // --- Fin del método ProjectController (constructor) ---

    
    
    void configurarListeners() {
        if (registry == null || model == null || projectListCoordinator == null) {
            logger.error("ERROR [ProjectController]: Dependencias nulas (registry, model o projectListCoordinator).");
            return;
        }

        JList<String> projectList = registry.get("list.proyecto.nombres");
        JList<String> descartesList = registry.get("list.proyecto.descartes");
        
        // --- Listener para las listas de Selección y Descartes ---
        MouseAdapter listMouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JList<?> sourceList = (JList<?>) e.getSource();
                
                if (sourceList == descartesList) {
                    logger.info ("\n\n [MOUSELISTENER] HEMOS ENTRADO EN VIEW_DISCARDS \n\n");
                    setProjectViewState(ProjectViewState.VIEW_DISCARDS);
                } else { // Clic en la lista de Selección (projectList)
                    ProjectViewState targetState = isExportPanelVisible() ? ProjectViewState.VIEW_EXPORT : ProjectViewState.VIEW_SELECTION;
                    logger.info ("\n\n [MOUSELISTENER] HEMOS ENTRADO EN " + targetState + " \n\n");
                    setProjectViewState(targetState);
                }
            }
        };

        if (projectList != null) {
            projectList.addMouseListener(listMouseAdapter);
            projectList.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting() || projectListCoordinator.isSincronizandoUI()) return;
                if ("seleccion".equals(model.getProyectoListContext().getNombreListaActiva())) {
                    projectListCoordinator.seleccionarImagenPorIndice(projectList.getSelectedIndex());
                }
            });
        }

        if (descartesList != null) {
            descartesList.addMouseListener(listMouseAdapter);
            descartesList.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting() || projectListCoordinator.isSincronizandoUI()) return;
                if ("descartes".equals(model.getProyectoListContext().getNombreListaActiva())) {
                    projectListCoordinator.seleccionarImagenPorIndice(descartesList.getSelectedIndex());
                }
            });
        }
        
        // --- AÑADIR LISTENER AL ÁREA DE EXPORTACIÓN ---
        MouseAdapter exportViewMouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentViewState != ProjectViewState.VIEW_EXPORT) {
                    logger.info ("\n\n [MOUSELISTENER EXPORT] HEMOS ENTRADO EN VIEW_EXPORT \n\n");
                    setProjectViewState(ProjectViewState.VIEW_EXPORT);
                }
            }
        };

        ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");
        if (exportPanel != null) {
            JTable tablaExportacion = exportPanel.getTablaExportacion();
            if (tablaExportacion != null) {
                tablaExportacion.addMouseListener(exportViewMouseAdapter);
                Component parent = tablaExportacion.getParent();
                if (parent instanceof javax.swing.JViewport) {
                    Component grandparent = parent.getParent();
                    if (grandparent instanceof JScrollPane) {
                        grandparent.addMouseListener(exportViewMouseAdapter);
                    }
                }
            }
        }
        
        // El ChangeListener del JTabbedPane se elimina porque causaba conflictos.
        // La lógica de estado ahora es manejada por los clics directos en las áreas de trabajo.
    
    } // --- Fin del método configurarListeners ---
    
    
    /**
     * Limpia por completo la interfaz de usuario del modo Proyecto.
     * Vacía las JLists de selección y descartes, el grid, y deselecciona cualquier imagen.
     * Este método es seguro y no fallará incluso si las JLists no tienen el modelo esperado.
     */
    private void limpiarVistaProyecto() {
        logger.debug("[ProjectController] Limpiando la vista del modo proyecto...");

        // 1. Limpiar modelos de las JLists de la izquierda de forma segura
        JList<String> listaSeleccion = registry.get("list.proyecto.nombres");
        if (listaSeleccion != null) {
            // Comprobamos si el modelo es del tipo que esperamos
            if (listaSeleccion.getModel() instanceof DefaultListModel) {
                ((DefaultListModel<String>) listaSeleccion.getModel()).clear();
            } else {
                // Si no lo es, simplemente le asignamos un nuevo modelo vacío.
                listaSeleccion.setModel(new DefaultListModel<>());
            }
        }
        
        JList<String> listaDescartes = registry.get("list.proyecto.descartes");
        if (listaDescartes != null) {
            if (listaDescartes.getModel() instanceof DefaultListModel) {
                ((DefaultListModel<String>) listaDescartes.getModel()).clear();
            } else {
                listaDescartes.setModel(new DefaultListModel<>());
            }
        }

        // 2. Limpiar el modelo "maestro" del contexto del proyecto en el VisorModel.
        ListContext proyectoContext = model.getProyectoListContext();
        if (proyectoContext.getModeloLista() != null && !proyectoContext.getModeloLista().isEmpty()) {
             model.setMasterListAndNotify(new DefaultListModel<>(), new HashMap<>(), this);
        }

        // 3. Limpiar la imagen principal mostrada
        if (projectListCoordinator != null) {
            projectListCoordinator.reiniciarYSeleccionarIndice(-1);
        }

        // 4. Actualizar títulos y contadores
        actualizarAparienciaListasPorFoco();
        JTabbedPane herramientasTabbedPane = registry.get("tabbedpane.proyecto.herramientas");
        if (herramientasTabbedPane != null && herramientasTabbedPane.getTabCount() > 0) {
            // Buscamos la pestaña por el nombre para ser más robustos
            for (int i = 0; i < herramientasTabbedPane.getTabCount(); i++) {
                if (herramientasTabbedPane.getTitleAt(i).startsWith("Descartes")) {
                    herramientasTabbedPane.setTitleAt(i, "Descartes: 0");
                    break;
                }
            }
        }

        logger.debug("[ProjectController] Vista del proyecto limpiada.");
    } // --- fin de metodo limpiarVistaProyecto ---
    
    
    /**
     * Limpia de forma exhaustiva todo el estado relacionado con el proyecto actual.
     * Esto incluye la UI, la cola de exportación y los cachés internos.
     * Es el método a llamar para asegurar un borrón y cuenta nueva.
     */
    private void limpiarEstadoCompletoDelProyecto() {
        logger.debug("Iniciando limpieza completa del estado del proyecto...");

        // 1. Limpiar la vista (JLists, Grid, etc.)
        limpiarVistaProyecto();

        // 2. Limpiar la cola de exportación y el mapa de items.
        if (exportQueueManager != null) {
            exportQueueManager.limpiarCola();
        }
        actualizarMapaDeItemsExportacion(null); // Pasa null para limpiar el mapa

        // 3. Limpiar la tabla de exportación en la UI
        JTable tabla = getTablaExportacionDesdeRegistro();
        if (tabla != null && tabla.getModel() instanceof ExportTableModel) {
            ((ExportTableModel) tabla.getModel()).clear();
        }

        // 4. Limpiar los renderers que puedan tener caché
        limpiarCacheRenderersProyecto();

        logger.debug("Limpieza completa del estado del proyecto finalizada.");
    } // ---FIN de metodo [limpiarEstadoCompletoDelProyecto]---
    
    
    /**
     * El método orquestador central DEFINITIVO.
     * Cambia la UI del proyecto a un estado bien definido, implementando una
     * lógica de "Grid pegajoso" una vez que se activa.
     */
    private void setProjectViewState(ProjectViewState newState) {
        if (currentViewState == newState) {
            logger.trace("Ya estamos en el estado {}, no se realiza ninguna acción.", newState);
            return;
        }
        
        logger.info("Solicitando cambio de estado de la vista del proyecto a: {}", newState);
        
        // --- FASE 1: ACTUALIZACIÓN DE ESTADO Y DATOS ---
        this.currentViewState = newState;
        
        // --- REGLA: Determinar la FUENTE DE DATOS para el Grid ---
        if (newState == ProjectViewState.VIEW_DISCARDS) {
            model.getProyectoListContext().setNombreListaActiva("descartes");
        } else { // Para VIEW_SELECTION y VIEW_EXPORT, la fuente es "seleccion"
            model.getProyectoListContext().setNombreListaActiva("seleccion");
        }
        actualizarModeloPrincipalConListaDeProyectoActiva();

        // --- Lógica específica para el estado de EXPORTACIÓN ---
        if (newState == ProjectViewState.VIEW_EXPORT) {
            // Esta es la llamada que faltaba. Prepara los datos para la JTable de exportación.
            solicitarPreparacionColaExportacion();
        }

        // --- FASE 2: ACTUALIZACIÓN VISUAL (ENCOLADA en el EDT) ---
        SwingUtilities.invokeLater(() -> {
            // --- REGLA: Sincronizar APARIENCIA del Grid (Marcos de estado) ---
            boolean showStateBorders = isExportPanelVisible();
            
            Action toggleStateAction = actionMap.get(AppActionCommands.CMD_GRID_SHOW_STATE);
            if (toggleStateAction != null && model.isGridMuestraEstado() != showStateBorders) {
                toggleStateAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
            }
            
            actualizarAparienciaListasPorFoco();
            sincronizarSeleccionEnGridProyecto();
            refrescarGridProyecto();
        });
    } // ---FIN de metodo [setProjectViewState]---
    
    
    /**
     * Actualiza el mapa interno que se usa para buscar rápidamente un ExportItem por su clave (Path como String).
     * @param items La lista actual de items de la cola de exportación.
     */
    private void actualizarMapaDeItemsExportacion(List<ExportItem> items) {
        if (items == null) {
            this.exportItemMap.clear();
            return;
        }
        this.exportItemMap = items.stream()
            .filter(item -> item.getRutaImagen() != null)
            .collect(Collectors.toMap(
                item -> item.getRutaImagen().toString().replace("\\", "/"),
                Function.identity(),
                (existing, replacement) -> existing
            ));
        logger.debug("Mapa de items de exportación actualizado. Total: {} items.", this.exportItemMap.size());
    } // ---FIN de metodo actualizarMapaDeItemsExportacion---
    
    
    public ExportItem getExportItem(String clave) {
        // La condición de si estamos en modo exportación ya la comprueba el llamador (GridCellRenderer)
        // usando el método isExportViewActive(). Este método solo debe devolver el item si existe.
        if (clave == null) {
            return null;
        }
        return exportItemMap.get(clave);
    } // ---FIN de metodo getExportItem---
    

    /**
     * Alterna la visibilidad del panel de herramientas inferior derecho (Exportar/Etiquetar).
     * Guarda y restaura la posición del divisor para una mejor experiencia de usuario.
     * AHORA SOLO GESTIONA LA VISIBILIDAD, Y DELEGA EL CAMBIO DE ESTADO.
     */
    public void toggleExportView() {
        if (registry == null) {
            logger.error("Registry es nulo, no se puede alternar la vista de exportación.");
            return;
        }
        
        JSplitPane rightSplit = registry.get("splitpane.proyecto.right");
        JPanel toolsPanel = registry.get("panel.proyecto.herramientas.container");

        if (rightSplit == null || toolsPanel == null) {
            logger.error("No se encontró el JSplitPane derecho o el panel de herramientas en el registro.");
            return;
        }

        boolean ahoraSeraVisible = !toolsPanel.isVisible();
        logger.debug("Alternando vista de exportación. Nuevo estado visible: {}", ahoraSeraVisible);

        if (ahoraSeraVisible) {
            // --- MOSTRAR PANEL ---
            toolsPanel.setVisible(true);
            rightSplit.setDividerSize(5);
            
            // --- REGLA 1 (LA EXCEPCIÓN): Forzar GRID al abrir ---
            displayModeManager.switchToDisplayMode(VisorModel.DisplayMode.GRID);
            
            // La intención ahora es EXPORTAR
            setProjectViewState(ProjectViewState.VIEW_EXPORT);
            actualizarPanelDePropiedadesEnUI();
            
            ensureExportPanelIsFullyInitialized();
            
            // Aquí es donde faltaba la lógica para posicionar el divisor.
            SwingUtilities.invokeLater(() -> {
                if (lastRightDividerLocation > 0) {
                    rightSplit.setDividerLocation(lastRightDividerLocation);
                } else {
                    ajustarPosicionDivisorDerecho(); // Usamos el nuevo método centralizado
                }
            });

        } else {
            // --- OCULTAR PANEL ---
            lastRightDividerLocation = rightSplit.getDividerLocation();
            
            toolsPanel.setVisible(false);
            rightSplit.setDividerSize(0);
            
            // Al ocultar, la intención vuelve a la selección normal.
            setProjectViewState(ProjectViewState.VIEW_SELECTION);
        }
    } // --- FIN del metodo toggleExportView ---
    
    
    /**
     * Se llama DESPUÉS de que las barras de herramientas del modo Proyecto
     * han sido construidas y registradas. Es el momento seguro para configurar
     * listeners que dependen de componentes de la toolbar.
     */
    public void postToolbarInitialization() {
        logger.debug("[ProjectController] Realizando inicialización post-toolbars...");
        ensureExportPanelIsFullyInitialized();
        configurarContextMenuTablaExportacion();
        configurarListenersMetadatos();
        logger.debug("[ProjectController] Inicialización post-toolbars completada.");
        
    } // ---FIN de metodo [postToolbarInitialization]---
    
    /**
     * Añade un DocumentListener al área de descripción para que cualquier
     * cambio notifique al sistema que hay modificaciones sin guardar.
     */
    private void configurarListenersMetadatos() {
        vista.panels.export.ProjectMetadataPanel propsPanel = registry.get("panel.proyecto.propiedades");
        if (propsPanel == null || projectManager == null) return;

        javax.swing.event.DocumentListener listener = new javax.swing.event.DocumentListener() {
            private void notificar() {
                projectManager.notificarModificacion();
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { notificar(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { notificar(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { notificar(); }
        };

        propsPanel.getProjectDescriptionArea().getDocument().addDocumentListener(listener);
        logger.debug("DocumentListener añadido al área de descripción del proyecto.");
    } // ---FIN de metodo configurarListenersMetadatos---
    
    
public void notificarCambioEnProyecto() {
    	
    	logger.info("--- PASO 3: notificarCambioEnColaExportacion en ProjectController EJECUTADO ---");
    	
        if (projectManager != null && generalController != null) {

            // PASO A: Sincronizar el estado de la tabla (UI) con el modelo de datos principal.
            sincronizarArchivosAsociadosConModelo();
            
            // PASO B: Notificar al ProjectManager para que compruebe si hay cambios.
            projectManager.notificarModificacion(); 
            
            // --- Forzar la actualización de toda la UI de exportación ---
            // Esto asegura que el título, el tamaño total y el estado de los botones
            // se actualicen inmediatamente después de un cambio en la tabla.
            actualizarEstadoExportacionUI();
            
            logger.debug("[ProjectController] Cambio detectado, sincronizado y UI de exportación actualizada.");
        }
    } // ---FIN de metodo notificarCambioEnProyecto---
    
    
    /**
     * Resetea el layout del panel derecho a su estado por defecto (panel de herramientas oculto).
     * Se debe llamar cada vez que se activa el modo Proyecto para asegurar un estado inicial limpio.
     */
    public void resetProjectViewLayout() {
        lastRightDividerLocation = -1; 
        
        JSplitPane rightSplit = registry.get("splitpane.proyecto.right");
        JPanel toolsPanel = registry.get("panel.proyecto.herramientas.container");

        if (rightSplit != null && toolsPanel != null) {
            toolsPanel.setVisible(false); // La llamada más importante
            rightSplit.setDividerLocation(1.0);
            rightSplit.setDividerSize(0);
            logger.debug("[ProjectController] Layout del panel derecho reseteado a oculto.");
        }
        
        // --- Sincronizar el estado del botón de toggle ---
        if (actionMap != null) {
            Action toggleAction = actionMap.get(AppActionCommands.CMD_EXPORT_ASSIGN_PANNEL);
            if (toggleAction != null) {
                toggleAction.putValue(Action.SELECTED_KEY, false);
                logger.debug("[ProjectController] Estado del botón de toggle del panel de exportación reseteado.");
            }
        }
        
    } // --- FIN de metodo resetProjectViewLayout ---
    
    
    /**
     * Carga en la `masterList` del `VisorModel` la lista de datos correcta
     * ('seleccion' o 'descartes') basándose en el foco activo actual.
     * Utiliza el `ProjectManager` como única fuente de verdad.
     */
    public void actualizarModeloPrincipalConListaDeProyectoActiva() {
        if (model == null || projectManager == null) {
            logger.warn("WARN [actualizarModeloPrincipalConListaDeProyectoActiva]: Modelo o ProjectManager nulos. No se puede actualizar.");
            return;
        }

        String nombreListaActiva = model.getProyectoListContext().getNombreListaActiva();
        
        // Se determina la fuente de datos real desde el ProjectManager, no desde la UI.
        List<Path> sourceData;
        if ("descartes".equals(nombreListaActiva)) {
            sourceData = projectManager.getImagenesDescartadas();
            logger.debug("Fuente de datos para masterList: Descartes ({} elementos)", sourceData.size());
        } else { // "seleccion" (o cualquier otro caso por defecto)
            sourceData = projectManager.getImagenesMarcadas();
            logger.debug("Fuente de datos para masterList: Selección ({} elementos)", sourceData.size());
        }

        // Se construye un nuevo modelo de lista con los datos correctos.
        DefaultListModel<String> newMasterModel = new DefaultListModel<>();
        for (Path p : sourceData) {
            newMasterModel.addElement(p.toString().replace("\\", "/"));
        }
        
        // Se notifica al VisorModel del nuevo modelo de datos para el grid.
        // El mapa de rutas completo no cambia, solo la lista de claves a mostrar.
        model.setMasterListAndNotify(newMasterModel, model.getProyectoListContext().getRutaCompletaMap(), this);
        
        sincronizarSeleccionEnGridProyecto();
        
    } // --- Fin del nuevo método actualizarModeloPrincipalConListaDeProyectoActiva ---
    
    
    public void sincronizarSeleccionEnGridProyecto() {
        if (registry == null || projectListCoordinator == null) return;
        
        if (model.getCurrentDisplayMode() != VisorModel.DisplayMode.GRID) {
            return;
        }
        
        JList<String> gridList = registry.get("list.grid.proyecto");
        if (gridList == null) return;

        int indiceSeleccionado = projectListCoordinator.getOfficialSelectedIndex();
        
        SwingUtilities.invokeLater(() -> {
            if (indiceSeleccionado >= 0 && indiceSeleccionado < gridList.getModel().getSize()) {
                if (gridList.getSelectedIndex() != indiceSeleccionado) {
                    gridList.setSelectedIndex(indiceSeleccionado);
                }
                gridList.ensureIndexIsVisible(indiceSeleccionado);
            } else {
                gridList.clearSelection();
            }
        });
    } // --- Fin del nuevo método sincronizarSeleccionEnGridProyecto ---
    
    
    private void cambiarFocoListaActiva(String nuevoFoco) {
        if ("descartes".equals(nuevoFoco)) {
            setProjectViewState(ProjectViewState.VIEW_DISCARDS);
        } else {
            setProjectViewState(ProjectViewState.VIEW_SELECTION);
        }
    } // --- Fin del método cambiarFocoListaActiva ---

    
    public boolean prepararDatosProyecto() {
        logger.debug("  [ProjectController] Preparando datos para el modo proyecto...");
        if (projectManager == null || model == null) { return false; }

        int size = projectManager.getCurrentProject().getSelectedImages().size();
        logger.info(">>>>>>>>>> [PROYECTO - INICIO] Al preparar datos, ProjectModel en memoria tiene {} imágenes seleccionadas.", size);
        
        List<Path> imagenesMarcadas = projectManager.getImagenesMarcadas();
        List<Path> imagenesDescartadas = projectManager.getImagenesDescartadas();
        List<Path> todasLasImagenes = new java.util.ArrayList<>();
        todasLasImagenes.addAll(imagenesMarcadas);
        todasLasImagenes.addAll(imagenesDescartadas);
        todasLasImagenes = todasLasImagenes.stream().distinct().collect(Collectors.toList());
        todasLasImagenes.sort((p1, p2) -> p1.toString().compareToIgnoreCase(p2.toString()));

        if (todasLasImagenes.isEmpty()) {
            // Limpiamos el contexto por si tenía datos viejos.
            model.getProyectoListContext().actualizarContextoCompleto(new DefaultListModel<>(), new HashMap<>());
            return false;
        }

        DefaultListModel<String> modeloUnificado = new DefaultListModel<>();
        Map<String, Path> mapaRutasProyecto = new HashMap<>();
        for (Path rutaAbsoluta : todasLasImagenes) {
            String clave = rutaAbsoluta.toString().replace("\\", "/");
            modeloUnificado.addElement(clave);
            mapaRutasProyecto.put(clave, rutaAbsoluta);
        }

        model.getProyectoListContext().actualizarContextoCompleto(modeloUnificado, mapaRutasProyecto);
        logger.debug("    -> Datos del proyecto preparados. Total imágenes: " + modeloUnificado.getSize());
        return true;
    } // ---FIN de metodo prepararDatosProyecto---
    
    
    /**
     * Se llama desde GeneralController cuando se entra en el modo Proyecto.
     * Es el punto de entrada principal para activar y configurar la vista del proyecto.
     * Contiene la lógica para decidir si mostrar un proyecto existente o preguntar para abrir uno.
     */
    public void activarVistaProyecto() {
        logger.debug("  [ProjectController] Activando la UI de la vista de proyecto...");
        
        limpiarCacheRenderersProyecto();
        
        if (registry == null || model == null || projectManager == null || projectListCoordinator == null || generalController == null) {
            logger.error("ERROR [ProjectController.activarVistaProyecto]: Dependencias nulas.");
            return;
        }
        
        boolean hayDatosParaMostrar = prepararDatosProyecto();

        if (hayDatosParaMostrar) {
            logger.debug("   -> Hay imágenes en el proyecto. Poblando la vista...");

            poblarListasSeleccionYDescartes();

            String focoGuardado = model.getProyectoListContext().getNombreListaActiva();
            cambiarFocoListaActiva(focoGuardado != null ? focoGuardado : "seleccion");
            
            String claveInicial = determinarClaveASeleccionar(model.getProyectoListContext());
            if (claveInicial != null) {
                projectListCoordinator.seleccionarImagenPorClave(claveInicial);
            } else {
                projectListCoordinator.seleccionarImagenPorIndice(-1);
            }
            
            ajustarLayoutProyectoUI();
            
        } else {
            logger.debug("   -> No hay imágenes en el proyecto. Limpiando la vista...");
            limpiarVistaProyecto();
        }
        
        resetProjectViewLayout();

        
    } // ---FIN de metodo activarVistaProyecto---
    
    
    private void ensureExportPanelIsFullyInitialized() {
        ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");
        if (exportPanel != null) {
            exportPanel.setupHighlightingListener();
        }
    } // ---FIN de metodo [ensureExportPanelIsFullyInitialized]---
    
    
    
    /**
     * Rellena las JList de Selección y Descartes con los datos del ProjectManager.
     */
    private void poblarListasSeleccionYDescartes() {
        List<Path> imagenesMarcadas = projectManager.getImagenesMarcadas();
        DefaultListModel<String> modeloSeleccion = new DefaultListModel<>();
        for (Path p : imagenesMarcadas) {
            modeloSeleccion.addElement(p.toString().replace("\\", "/"));
        }
        JList<String> projectList = registry.get("list.proyecto.nombres");
        if (projectList != null) {
            projectList.setModel(modeloSeleccion);
        }
        poblarListaDescartes();
        actualizarContadoresDeTitulos();
        
    } // ---fin de metodo poblarListasSeleccionYDescartes---

    
    
    /**
     * Determina qué clave de imagen debe estar seleccionada al activar la vista.
     */
    private String determinarClaveASeleccionar(ListContext proyectoContext) {
        String focoActual = proyectoContext.getNombreListaActiva();
        String claveParaMostrar = null;

        if ("descartes".equals(focoActual)) {
            claveParaMostrar = proyectoContext.getDescartesListKey();
        } else {
            claveParaMostrar = proyectoContext.getSeleccionListKey();
        }
        
        // Si después de buscar la clave guardada, sigue siendo nula o no está en la lista actual...
        if (claveParaMostrar == null || !proyectoContext.getModeloLista().contains(claveParaMostrar)) {
            
            // ...intentamos seleccionar la primera de la lista activa.
            JList<String> listaActivaUI = "descartes".equals(focoActual)
                                        ? registry.get("list.proyecto.descartes")
                                        : registry.get("list.proyecto.nombres");
                                        
            if (listaActivaUI != null && listaActivaUI.getModel().getSize() > 0) {
                claveParaMostrar = listaActivaUI.getModel().getElementAt(0);
            } else {
                // Si incluso la lista activa está vacía, intentamos con la otra lista.
                JList<String> otraListaUI = "descartes".equals(focoActual)
                                            ? registry.get("list.proyecto.nombres")
                                            : registry.get("list.proyecto.descartes");
                if (otraListaUI != null && otraListaUI.getModel().getSize() > 0) {
                    claveParaMostrar = otraListaUI.getModel().getElementAt(0);
                }
            }
        }

        logger.debug("[ProjectController] Clave determinada para la selección inicial: {}", claveParaMostrar);
        return claveParaMostrar;
        
    } // ---fin de metodo determinarClaveASeleccionar---
    
    
    /**
     * Ajusta los componentes visuales de la UI del modo proyecto.
     * Este método se llama después de que los datos del proyecto han sido cargados.
     */
    private void ajustarLayoutProyectoUI() {
        SwingUtilities.invokeLater(() -> {
            actualizarAparienciaListasPorFoco();
            
            // Este método ya no es necesario aquí, el Builder lo maneja.
            // Si se necesita un ajuste dinámico, se puede añadir más lógica después.
            
            logger.debug("  [ProjectController] UI de la vista de proyecto activada y apariencia actualizada.");
        });
    } // ---fin de metodo ajustarLayoutProyectoUI---    
    
    @Override
    public void navegarSiguiente() {
        if (projectListCoordinator != null) {
            projectListCoordinator.seleccionarSiguiente();
        }
    } // ---FIN de metodo navegarSiguiente---
    
    
    @Override
    public void navegarAnterior() {
        if (projectListCoordinator != null) projectListCoordinator.seleccionarAnterior();
    } // ---FIN de metodo navegarAnterior---

    
    @Override
    public void navegarPrimero() {
        if (projectListCoordinator != null) projectListCoordinator.seleccionarPrimero();
    } // ---FIN de metodo navegarPrimero---

    
    @Override
    public void navegarUltimo() {
        if (projectListCoordinator != null) projectListCoordinator.seleccionarUltimo();
    } // ---FIN de metodo navegarUltimo---

    
    @Override
    public void navegarBloqueSiguiente() {
        if (projectListCoordinator != null) projectListCoordinator.seleccionarBloqueSiguiente();
    } // ---FIN de metodo navegarBloqueSiguiente---

    
    @Override
    public void navegarBloqueAnterior() {
        if (projectListCoordinator != null) projectListCoordinator.seleccionarBloqueAnterior();
    } // ---FIN de metodo navegarBloqueAnterior---

    
    @Override
    public void aplicarZoomConRueda(java.awt.event.MouseWheelEvent e) {
    	if (zoomManager != null) {
    	    zoomManager.aplicarZoomConRueda(e);
    	    if (generalController != null && generalController.getVisorController() != null) {
//    	        generalController.getVisorController().sincronizarEstadoVisualBotonesYRadiosZoom();
    	    	zoomManager.sincronizarEstadoVisualBotonesYRadiosZoom();
    	    }
    	}
    } // --- Fin del método aplicarZoomConRueda ---

    
    @Override
    public void aplicarPan(int deltaX, int deltaY) {
        if (zoomManager != null) {
            zoomManager.aplicarPan(deltaX, deltaY);
        }
    } // --- Fin del método aplicarPan ---

    
    @Override
    public void iniciarPaneo(java.awt.event.MouseEvent e) {
        if (zoomManager != null && model.isZoomHabilitado()) {
            zoomManager.iniciarPaneo(e);
        }
    } // --- Fin del método iniciarPaneo ---

    
    @Override
    public void continuarPaneo(java.awt.event.MouseEvent e) {
        if (zoomManager != null && model.isZoomHabilitado()) {
            zoomManager.continuarPaneo(e);
        }
    } // --- Fin del método continuarPaneo ---
    
    
    @Override
    public void solicitarRefresco() {
        logger.debug("[ProjectController] Solicitud de refresco recibida. Llamando a refrescarListasDeProyecto...");
        refrescarListasDeProyecto();
    } // --- Fin del método solicitarRefresco ---
    
    
    public void poblarListaDescartes() {
        if (registry == null || projectManager == null) {
            logger.warn("WARN [poblarListaDescartes]: Registry o ProjectManager nulos.");
            return;
        }
        JList<String> listaDescartesUI = registry.get("list.proyecto.descartes");
        if (listaDescartesUI == null) {
            logger.warn("WARN [poblarListaDescartes]: JList 'list.proyecto.descartes' no encontrada en el registro.");
            return;
        }

        List<Path> imagenesDescartadas = projectManager.getImagenesDescartadas();
        DefaultListModel<String> modeloDescartes = new DefaultListModel<>();
        
        for (Path rutaAbsoluta : imagenesDescartadas) {
            String clave = rutaAbsoluta.toString().replace("\\", "/");
            modeloDescartes.addElement(clave);
        }
        
        listaDescartesUI.setModel(modeloDescartes);
        
        logger.debug("  [ProjectController] Lista de descartes actualizada en la UI. Total: " + modeloDescartes.getSize());
        javax.swing.JTabbedPane herramientasTabbedPane = registry.get("tabbedpane.proyecto.herramientas");
        if (herramientasTabbedPane != null) {
            int tabCount = herramientasTabbedPane.getTabCount();
            for (int i = 0; i < tabCount; i++) {
                String tituloActual = herramientasTabbedPane.getTitleAt(i);
                if ("Descartes".equals(tituloActual) || tituloActual.startsWith("Descartes:")) {
                    herramientasTabbedPane.setTitleAt(i, "Descartes: " + modeloDescartes.getSize());
                    break;
                }
            }
        }
        
    } // --- Fin del método poblarListaDescartes ---
  
    
    public void moverSeleccionActualADescartes() {
        if (model == null || projectManager == null || registry == null) return;
        
        // 1. Obtener la lista y el índice ANTES de cualquier cambio.
        JList<String> listaSeleccionUI = registry.get("list.proyecto.nombres");
        if (listaSeleccionUI == null) return;
        int indiceOriginal = listaSeleccionUI.getSelectedIndex();
        if (indiceOriginal == -1) {
             logger.debug("No hay imagen seleccionada para mover a descartes.");
             return;
        }

        // 2. Obtener la clave de la imagen a mover.
        String claveSeleccionada = listaSeleccionUI.getSelectedValue();
        Path rutaAbsoluta = model.getProyectoListContext().getRutaCompleta(claveSeleccionada);
        if (rutaAbsoluta == null) return;

        // 3. Realizar la operación en el modelo de datos y guardar.
        projectManager.moverAdescartes(rutaAbsoluta);
        projectManager.notificarModificacion();

        // 4. Refrescar TODA la UI del proyecto. Esto reconstruirá los modelos de las JList.
        refrescarVistaProyectoCompleta();

        // 5. Calcular y aplicar la nueva selección de forma inteligente.
        SwingUtilities.invokeLater(() -> {
            // Volvemos a obtener la JList y su modelo, ya que han sido actualizados.
            JList<String> listaActualizada = registry.get("list.proyecto.nombres");
            if (listaActualizada == null) return;
            int nuevoTamanio = listaActualizada.getModel().getSize();
            
            if (nuevoTamanio > 0) {
                int nuevoIndiceASeleccionar = indiceOriginal;
                // Si el índice original ya no existe (porque era el último), seleccionamos el nuevo último.
                if (nuevoIndiceASeleccionar >= nuevoTamanio) {
                    nuevoIndiceASeleccionar = nuevoTamanio - 1;
                }
                
                // Usamos el coordinador para que la selección se propague a toda la UI
                projectListCoordinator.seleccionarImagenPorIndice(nuevoIndiceASeleccionar);
            } else {
                // Si la lista quedó vacía, deseleccionamos todo.
                projectListCoordinator.seleccionarImagenPorIndice(-1);
            }
        });
    } // --- Fin del método moverSeleccionActualADescartes ---

    
    public void restaurarDesdeDescartes() {
        if (registry == null || projectManager == null) return;

        // 1. Obtener la lista y el índice ANTES del cambio.
        JList<String> listaDescartesUI = registry.get("list.proyecto.descartes");
        if (listaDescartesUI == null) return;
        int indiceOriginal = listaDescartesUI.getSelectedIndex();
        if (indiceOriginal == -1) {
            logger.debug("No hay imagen seleccionada en descartes para restaurar.");
            return;
        }
        
        String claveSeleccionada = listaDescartesUI.getSelectedValue();
        Path rutaAbsoluta = java.nio.file.Paths.get(claveSeleccionada);
        
        // 2. Realizar la operación en el modelo de datos y guardar.
        projectManager.restaurarDeDescartes(rutaAbsoluta);
        projectManager.notificarModificacion();
        
        // 3. Refrescar TODA la UI del proyecto.
        refrescarVistaProyectoCompleta();

        // 4. Calcular y aplicar la nueva selección en la lista de DESCARTES.
        SwingUtilities.invokeLater(() -> {
            // Volvemos a obtener la JList y su modelo, ya que han sido actualizados.
            JList<String> listaActualizada = registry.get("list.proyecto.descartes");
            if (listaActualizada == null) return;
            int nuevoTamanio = listaActualizada.getModel().getSize();

            // Nos aseguramos de que el foco lógico esté en la lista de descartes
            setProjectViewState(ProjectViewState.VIEW_DISCARDS);

            if (nuevoTamanio > 0) {
                int nuevoIndiceASeleccionar = indiceOriginal;
                if (nuevoIndiceASeleccionar >= nuevoTamanio) {
                    nuevoIndiceASeleccionar = nuevoTamanio - 1;
                }
                projectListCoordinator.seleccionarImagenPorIndice(nuevoIndiceASeleccionar);
            } else {
                // Si la lista de descartes quedó vacía, movemos el foco a la de selección
                setProjectViewState(ProjectViewState.VIEW_SELECTION);
                projectListCoordinator.seleccionarImagenPorIndice(0); // Seleccionamos el primero de la otra lista
            }
        });
        
	} // --- Fin del método restaurarDesdeDescartes ---


    private void refrescarListasDeProyecto() {
        logger.debug("  [ProjectController] Refrescando ambas listas del proyecto...");
        prepararDatosProyecto(); 
        activarVistaProyecto(); 
    } // --- Fin del método refrescarListasDeProyecto ---
    
    
    public void solicitarPreparacionColaExportacion() {
        if (projectManager == null || exportQueueManager == null || registry == null) {
            logger.error("ERROR [solicitarPreparacionColaExportacion]: Dependencias nulas.");
            return;
        }
        List<Path> seleccionActual = projectManager.getImagenesMarcadas();
        Map<String, modelo.proyecto.ExportConfig> exportConfigs = projectManager.getCurrentProject().getExportConfigs();
        exportQueueManager.prepararColaDesdeSeleccion(seleccionActual, exportConfigs);
        
        actualizarMapaDeItemsExportacion(exportQueueManager.getColaDeExportacion());
        
        JTable tablaUI = getTablaExportacionDesdeRegistro();
        if (tablaUI != null && tablaUI.getModel() instanceof ExportTableModel) {
            ((ExportTableModel) tablaUI.getModel()).setCola(exportQueueManager.getColaDeExportacion());
            logger.debug("[ProjectController] Modelo de tabla de exportación actualizado.");
        } else {
            logger.warn("WARN [ProjectController]: No se pudo obtener la tabla de exportación o su modelo no es ExportTableModel.");
        }
        
        actualizarEstadoExportacionUI();
    } // --- Fin del método solicitarPreparacionColaExportacion ---
    
    
    public void solicitarSeleccionCarpetaDestino() {
        if (registry == null || view == null) {
            logger.error("ERROR [solicitarSeleccionCarpetaDestino]: Registry o View nulos.");
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar Carpeta de Destino para la Exportación");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        int resultado = fileChooser.showOpenDialog(view);
        
        
        if (resultado == JFileChooser.APPROVE_OPTION) {
            Path carpetaSeleccionada = fileChooser.getSelectedFile().toPath();
            logger.debug("  [ProjectController] Carpeta de destino seleccionada: " + carpetaSeleccionada);
            // --- INICIO DE LA MODIFICACIÓN ---
            // Usamos la clave correcta con la que se registró el panel en ProjectBuilder
            vista.panels.export.ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");
            if (exportPanel != null) {
                exportPanel.setRutaDestino(carpetaSeleccionada.toString());
            } else {
                logger.error("No se pudo encontrar el ExportPanel con la clave 'panel.proyecto.exportacion.completo' en el registro.");
            }
            
        } else {
            logger.debug("  [ProjectController] Selección de carpeta de destino cancelada por el usuario.");
        }
        actualizarEstadoExportacionUI();
    } // --- Fin del método solicitarSeleccionCarpetaDestino ---
    
    
    public void onExportItemManuallyAssigned(modelo.proyecto.ExportItem itemModificado) {
        logger.debug("  [ProjectController] Archivo asignado manualmente para: " + itemModificado.getRutaImagen().getFileName());
        actualizarEstadoExportacionUI();
    } // --- Fin del método onExportItemManuallyAssigned ---
    
    
    public void actualizarEstadoExportacionUI() {
        if (registry == null || exportQueueManager == null || actionMap == null) {
            logger.warn("Dependencias nulas (registry, exportQueueManager, o actionMap), abortando actualización de UI de exportación.");
            return;
        }

        vista.panels.export.ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");
        if (exportPanel == null) {
            logger.warn("No se encontró el ExportPanel 'panel.proyecto.exportacion.completo' en el registro.");
            return;
        }

        List<modelo.proyecto.ExportItem> colaCompleta = exportQueueManager.getColaDeExportacion();
        List<modelo.proyecto.ExportItem> itemsSeleccionadosParaExportar = colaCompleta.stream()
                .filter(modelo.proyecto.ExportItem::isSeleccionadoParaExportar)
                .collect(Collectors.toList());

        // --- LOGICA DE DETECCION DE CONFLICTOS MEJORADA ---

        // 1. Detectar conflictos de NOMBRE DE IMAGEN (el original)
        java.util.Set<String> nombresDeImagenDuplicados = itemsSeleccionadosParaExportar.stream()
            .map(item -> item.getRutaImagen().getFileName().toString())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        // 2. Detectar conflictos de NOMBRE DE ARCHIVO ASIGNADO (el nuevo)
        java.util.Map<String, List<Path>> archivosAsignadosPorNombre = itemsSeleccionadosParaExportar.stream()
            .filter(item -> item.getRutasArchivosAsociados() != null && !item.getRutasArchivosAsociados().isEmpty())
            .flatMap(item -> item.getRutasArchivosAsociados().stream()) // Aplanar la lista de archivos
            .collect(Collectors.groupingBy(path -> path.getFileName().toString()));
            
        java.util.Set<String> nombresDeAsignadosDuplicados = archivosAsignadosPorNombre.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        // 3. Actualizar el estado de TODOS los items
        boolean hayConflictos = !nombresDeImagenDuplicados.isEmpty() || !nombresDeAsignadosDuplicados.isEmpty();

        for (modelo.proyecto.ExportItem item : colaCompleta) {
            boolean conflictoImagen = nombresDeImagenDuplicados.contains(item.getRutaImagen().getFileName().toString());
            item.setTieneConflictoDeNombre(conflictoImagen);

            // Solo si no hay un conflicto de imagen (que es más grave), comprobamos el de asignado
            if (!conflictoImagen && item.getRutasArchivosAsociados() != null) {
                boolean conflictoAsignado = item.getRutasArchivosAsociados().stream()
                    .anyMatch(path -> nombresDeAsignadosDuplicados.contains(path.getFileName().toString()));
                if (conflictoAsignado) {
                    // Usamos el estado original para el conflicto, pero lo cambiamos solo si es necesario
                    if (item.getEstadoArchivoComprimido() != ExportStatus.ASIGNADO_DUPLICADO) {
                         item.setEstadoArchivoComprimido(ExportStatus.ASIGNADO_DUPLICADO);
                    }
                } else {
                    // Si ya NO hay conflicto, lo restauramos a su estado normal (OK o MANUAL)
                    if (item.getEstadoArchivoComprimido() == ExportStatus.ASIGNADO_DUPLICADO) {
                        if (item.getCandidatosArchivo() != null && !item.getCandidatosArchivo().isEmpty()) {
                            item.setEstadoArchivoComprimido(ExportStatus.ENCONTRADO_OK);
                        } else {
                            item.setEstadoArchivoComprimido(ExportStatus.ASIGNADO_MANUAL);
                        }
                    }
                }
            }
        }
        // --- FIN DE LA LOGICA DE DETECCION DE CONFLICTOS MEJORADA ---

        long totalItems = colaCompleta.size();
        long seleccionados = itemsSeleccionadosParaExportar.size();
        String rutaDestino = exportPanel.getRutaDestino();
        
        exportPanel.actualizarTituloExportacion((int) seleccionados, (int) totalItems);
        exportPanel.actualizarTamañoTotalExportacion();

        boolean carpetaOk = rutaDestino != null && !rutaDestino.isBlank() && !rutaDestino.equalsIgnoreCase("Seleccione una carpeta de destino...");

        boolean todosLosSeleccionadosEstanListos = itemsSeleccionadosParaExportar.stream().allMatch(item ->
                item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.ENCONTRADO_OK ||
                item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.ASIGNADO_MANUAL ||
                item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO
        );
        
        boolean puedeExportar = carpetaOk && todosLosSeleccionadosEstanListos && seleccionados > 0 && !hayConflictos;

        boolean resaltarDestino = seleccionados > 0 && !carpetaOk;
        exportPanel.resaltarRutaDestino(resaltarDestino);

        String mensajeResumen;
        if (hayConflictos) {
            mensajeResumen = "Conflicto de nombres detectado. Deseleccione los archivos duplicados para poder exportar.";
        } else if (!carpetaOk && seleccionados > 0) {
            mensajeResumen = "Falta carpeta destino.";
        } else if (seleccionados == 0 && totalItems > 0) {
            mensajeResumen = "No hay archivos seleccionados para exportar.";
        } else if (!todosLosSeleccionadosEstanListos && seleccionados > 0) {
            mensajeResumen = "Revisar archivos con error.";
        } else if (puedeExportar) {
            mensajeResumen = seleccionados + " de " + totalItems + " archivos listos para exportar.";
        } else if (totalItems == 0) {
            mensajeResumen = "No hay imágenes en la selección actual.";
        } else {
            mensajeResumen = "Cargue la selección para ver el estado."; 
        }

        actualizarTooltipAccion(AppActionCommands.CMD_INICIAR_EXPORTACION, mensajeResumen);
        actualizarTooltipAccion(AppActionCommands.CMD_EXPORT_SELECCIONAR_CARPETA, "Seleccionar la carpeta donde se exportarán los archivos");
        actualizarTooltipAccion(AppActionCommands.CMD_EXPORT_DETALLES_SELECCION, "Mostrar/Ocultar el panel de detalles de archivos asociados");
        actualizarTooltipAccion(AppActionCommands.CMD_EXPORT_ASIGNAR_ARCHIVO, "Asignar manually un archivo comprimido a la imagen seleccionada");
        actualizarTooltipAccion(AppActionCommands.CMD_EXPORT_IGNORAR_COMPRIMIDO, "Marcar la imagen seleccionada para exportar sin archivo comprimido");
        actualizarTooltipAccion(AppActionCommands.CMD_EXPORT_RELOCALIZAR_IMAGEN, "Buscar una nueva ubicación para la imagen seleccionada (si no se encuentra)");
        actualizarTooltipAccion(AppActionCommands.CMD_EXPORT_QUITAR_DE_COLA, "Mover la imagen seleccionada a la lista de Descartes (no se exportará)");
        actualizarTooltipAccion(AppActionCommands.CMD_EXPORT_REFRESH, "Vuelve a escanear el disco para actualizar el estado de los archivos");
        
        exportPanel.actualizarEstadoControles(puedeExportar, mensajeResumen);
        
        JTable tablaUI = getTablaExportacionDesdeRegistro();
        if (tablaUI != null) {
            tablaUI.repaint();
        }
        
        logger.debug("Estado de exportación UI actualizado. Puede exportar: {}. Mensaje: '{}'", puedeExportar, mensajeResumen);
    
        if (projectManager != null) {
            projectManager.notificarModificacion();
        }
        
    } // --- Fin del método actualizarEstadoExportacionUI ---
    
    
    /**
     * Método de ayuda para actualizar el tooltip (SHORT_DESCRIPTION) de una acción en el actionMap.
     * @param commandKey La clave de la acción en el mapa.
     * @param tooltipText El texto de ayuda a establecer.
     */
    private void actualizarTooltipAccion(String commandKey, String tooltipText) {
        if (actionMap != null) {
            Action action = actionMap.get(commandKey);
            if (action != null) {
                action.putValue(Action.SHORT_DESCRIPTION, tooltipText);
            }
        }
    } // ---FIN de metodo [actualizarTooltipAccion]---
    
    
    /**
     * Orquesta la adición de uno o más archivos asociados a un ExportItem.
     * Este método contiene toda la lógica de negocio, liberando a la Action de esa responsabilidad.
     */
    public void solicitarAnadirArchivoAsociado() {
        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) {
            JOptionPane.showMessageDialog(
                getView(), 
                "Por favor, seleccione una imagen de la tabla de exportación primero.", 
                "Acción no disponible", 
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        ExportTableModel tableModel = (ExportTableModel) tablaExportacion.getModel();
        int selectedRow = tablaExportacion.getSelectedRow();
        ExportItem selectedItem = tableModel.getItemAt(selectedRow);
        if (selectedItem == null) return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Añadir archivo(s) para: " + selectedItem.getRutaImagen().getFileName());
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        Path parentDir = selectedItem.getRutaImagen().getParent();
        if (parentDir != null && Files.isDirectory(parentDir)) {
            fileChooser.setCurrentDirectory(parentDir.toFile());
        }

        int result = fileChooser.showOpenDialog(getView());

        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            
            for (File file : selectedFiles) {
                selectedItem.addRutaArchivoAsociado(file.toPath());
                projectManager.addAssociatedFile(selectedItem.getRutaImagen(), file.toPath());
            }
            
            selectedItem.setEstadoArchivoComprimido(ExportStatus.ASIGNADO_MANUAL);
            
            // Notificar a toda la UI para que se refresque
            tableModel.fireTableRowsUpdated(selectedRow, selectedRow);
            actualizarEstadoExportacionUI();
            
            notificarCambioEnProyecto();
            
            ExportPanel exportPanel = getRegistry().get("panel.proyecto.exportacion.completo");
            if (exportPanel != null && exportPanel.getDetailPanel() != null) {
                exportPanel.getDetailPanel().updateDetails(selectedItem);
            }
        }
    } // ---FIN de metodo [solicitarAnadirArchivoAsociado]---
    
    
    /**
     * Orquesta la eliminación de un archivo asociado de un ExportItem.
     * Es llamado por la acción DeleteAssociatedFileAction.
     */
    public void solicitarQuitarArchivoAsociado() {
        ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");
        if (exportPanel == null) return;
        
        ExportDetailPanel detailPanel = exportPanel.getDetailPanel();
        if (detailPanel == null) return;

        Path archivoSeleccionado = detailPanel.getArchivoAsociadoSeleccionado();
        if (archivoSeleccionado == null) {
            JOptionPane.showMessageDialog(
                view, 
                "Por favor, seleccione un archivo de la lista de 'Detalles' para quitarlo.",
                "Ningún archivo seleccionado",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) return;

        ExportTableModel model = (ExportTableModel) tablaExportacion.getModel();
        int selectedRow = tablaExportacion.getSelectedRow();
        ExportItem selectedItem = model.getItemAt(selectedRow);

        if (selectedItem != null) {
            selectedItem.getRutasArchivosAsociados().remove(archivoSeleccionado);
            projectManager.removeAssociatedFile(selectedItem.getRutaImagen(), archivoSeleccionado);
            
            // Si ya no quedan archivos asignados manualmente, pero el buscador automático sí encontró candidatos,
            // volvemos al estado de "encontrado ok". Si no, a "no encontrado".
            if (selectedItem.getRutasArchivosAsociados().isEmpty()) {
                if (selectedItem.getCandidatosArchivo() != null && !selectedItem.getCandidatosArchivo().isEmpty()) {
                    selectedItem.setRutasArchivosAsociados(new ArrayList<>(selectedItem.getCandidatosArchivo()));
                    selectedItem.setEstadoArchivoComprimido(ExportStatus.ENCONTRADO_OK);
                } else {
                    selectedItem.setEstadoArchivoComprimido(ExportStatus.NO_ENCONTRADO);
                }
                
                notificarCambioEnProyecto();
            }
            
            model.fireTableRowsUpdated(selectedRow, selectedRow);
            detailPanel.updateDetails(selectedItem); // Actualiza la lista de detalles
            actualizarEstadoExportacionUI();
        }
    } // ---FIN de metodo [solicitarQuitarArchivoAsociado]---

    /**
     * Orquesta la localización (abrir explorador) de un archivo asociado.
     * Es llamado por la acción LocateAssociatedFileAction.
     */
    public void solicitarLocalizarArchivoAsociado() {
        ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");
        if (exportPanel == null) return;

        ExportDetailPanel detailPanel = exportPanel.getDetailPanel();
        if (detailPanel == null) return;

        Path archivoSeleccionado = detailPanel.getArchivoAsociadoSeleccionado();
        if (archivoSeleccionado == null) {
            JOptionPane.showMessageDialog(
                view,
                "Por favor, seleccione un archivo de la lista de 'Detalles' para localizarlo.",
                "Ningún archivo seleccionado",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        try {
            // Usamos openAndSelectFile, que intentará seleccionar el archivo si el SO lo soporta
            DesktopUtils.openAndSelectFile(archivoSeleccionado);
        } catch (Exception e) {
            logger.error("Error al intentar abrir y seleccionar el archivo asociado: {}", e.getMessage());
            JOptionPane.showMessageDialog(view, "No se pudo abrir la ubicación del archivo.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    } // ---FIN de metodo [solicitarLocalizarArchivoAsociado]---
    
    
    /**
     * Sincroniza la selección de la JTable de exportación para que coincida con la imagen
     * actualmente seleccionada en el modelo principal del visor.
     * Este método es crucial para mantener la consistencia de la UI cuando se navega
     * por las imágenes con los controles principales mientras el panel de exportación está visible.
     */
    public void sincronizarSeleccionEnTablaExportacion() {
        // Solo actuar si estamos en el estado de exportación
        if (!isExportPanelVisible() || model == null) {
            return;
        }

        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || !(tablaExportacion.getModel() instanceof ExportTableModel)) {
            return;
        }

        String claveSeleccionada = model.getSelectedImageKey();
        if (claveSeleccionada == null) {
            tablaExportacion.clearSelection();
            return;
        }

        ExportTableModel tableModel = (ExportTableModel) tablaExportacion.getModel();
        int rowIndex = tableModel.findRowIndexByPath(claveSeleccionada);

        SwingUtilities.invokeLater(() -> {
            if (rowIndex != -1) {
                // Si encontramos la fila, la seleccionamos y nos aseguramos de que sea visible
                if (tablaExportacion.getSelectedRow() != rowIndex) {
                    tablaExportacion.setRowSelectionInterval(rowIndex, rowIndex);
                    tablaExportacion.scrollRectToVisible(tablaExportacion.getCellRect(rowIndex, 0, true));
                    logger.trace("Tabla de exportación sincronizada a la fila {} para la clave {}", rowIndex, claveSeleccionada);
                }
            } else {
                // Si la imagen seleccionada no está en la tabla (ej. es un descarte), limpiamos la selección
                tablaExportacion.clearSelection();
                logger.trace("La clave {} no se encontró en la tabla de exportación. Selección limpiada.", claveSeleccionada);
            }
        });
    } // ---FIN de metodo [sincronizarSeleccionEnTablaExportacion]---
    
    
    public void solicitarInicioExportacion() {
        if (exportQueueManager == null || registry == null || view == null) {
            logger.error("ERROR [solicitarInicioExportacion]: Dependencias nulas.");
            return;
        }
        
        ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");
        if (exportPanel == null) {
            logger.error("CRITICAL: No se pudo encontrar el ExportPanel al iniciar la exportación.");
            JOptionPane.showMessageDialog(view, "Error interno: No se pudo encontrar el panel de exportación.", "Error Crítico", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        Path carpetaDestino = java.nio.file.Paths.get(exportPanel.getRutaDestino());

        // --- INICIO DE LA FASE 2: VALIDACIÓN PREVIA ---

        // 1. VALIDAR LA CARPETA DE DESTINO
        if (!Files.exists(carpetaDestino)) {
            JOptionPane.showMessageDialog(view, "La carpeta de destino no existe:\n" + carpetaDestino, "Error de Exportación", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!Files.isDirectory(carpetaDestino)) {
            JOptionPane.showMessageDialog(view, "La ruta de destino no es una carpeta:\n" + carpetaDestino, "Error de Exportación", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!Files.isWritable(carpetaDestino)) {
            JOptionPane.showMessageDialog(view, "No se tienen permisos de escritura en la carpeta de destino:\n" + carpetaDestino, "Error de Exportación", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        List<ExportItem> colaParaCopiar = exportQueueManager.getColaDeExportacion().stream()
            .filter(ExportItem::isSeleccionadoParaExportar)
            .collect(Collectors.toList());

        if (colaParaCopiar.isEmpty()) {
            JOptionPane.showMessageDialog(view, "No hay archivos seleccionados en la cola para exportar.", "Exportación Vacía", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 2. DETECTAR CONFLICTOS DE SOBRESCRITURA
        List<String> archivosEnConflicto = new ArrayList<>();
        for (ExportItem item : colaParaCopiar) {
            // Comprobar la imagen
            Path destinoImagen = carpetaDestino.resolve(item.getRutaImagen().getFileName());
            if (Files.exists(destinoImagen)) {
                archivosEnConflicto.add(destinoImagen.getFileName().toString());
            }
            // Comprobar los archivos asociados
            for (Path asociado : item.getRutasArchivosAsociados()) {
                Path destinoAsociado = carpetaDestino.resolve(asociado.getFileName());
                if (Files.exists(destinoAsociado)) {
                    archivosEnConflicto.add(destinoAsociado.getFileName().toString());
                }
            }
        }
        
        if (!archivosEnConflicto.isEmpty()) {
            String listaConflictos = String.join("\n- ", archivosEnConflicto);
            String mensaje = "El destino ya contiene archivos con los siguientes nombres:\n\n- " + 
                             listaConflictos + 
                             "\n\n¿Deseas reemplazar los archivos existentes?";

            int confirmacion = JOptionPane.showConfirmDialog(
                view,
                mensaje,
                "Confirmar Sobrescribir Archivos",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (confirmacion != JOptionPane.YES_OPTION) {
                logger.info("Exportación cancelada por el usuario debido a conflictos de sobrescritura.");
                return; // El usuario eligió "No"
            }
        }
        
        // --- FIN DE LA FASE 2: VALIDACIÓN PREVIA ---

        // Si hemos llegado hasta aquí, todas las validaciones han pasado.
        TaskProgressDialog dialogo = new TaskProgressDialog(
                view, 
                "Progreso de Exportación", 
                "Copiando archivos del proyecto..."
        );
        ExportWorker worker = new ExportWorker(colaParaCopiar, carpetaDestino, dialogo);
        
        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                dialogo.setProgress((Integer) evt.getNewValue());
            }
        });
        worker.execute();
        dialogo.setVisible(true);
    } // --- Fin del método solicitarInicioExportacion ---
    
    
    public void solicitarAbrirUbicacionImagen() {
        if (exportQueueManager == null || registry == null) return;
        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) return;
        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
        modelo.proyecto.ExportItem itemSeleccionado = modelTabla.getItemAt(tablaExportacion.getSelectedRow());
        if (itemSeleccionado != null) {
            try {
                DesktopUtils.openAndSelectFile(itemSeleccionado.getRutaImagen());
            } catch (Exception e) {
                logger.error("Error al intentar abrir y seleccionar el archivo: " + e.getMessage());
                JOptionPane.showMessageDialog(view, "No se pudo abrir la ubicación del archivo.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    } // --- Fin del método solicitarAbrirUbicacionImagen ---
    
    
    public void solicitarAlternarIgnorarComprimido() {
        if (registry == null) return;
        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) return;
        int filaSeleccionada = tablaExportacion.getSelectedRow();
        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
        modelo.proyecto.ExportItem item = modelTabla.getItemAt(filaSeleccionada);
        if (item != null) {
            if (item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.NO_ENCONTRADO) {
                item.setEstadoArchivoComprimido(modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO);
            } else if (item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO) {
                item.setEstadoArchivoComprimido(modelo.proyecto.ExportStatus.NO_ENCONTRADO);
            }
            modelTabla.fireTableRowsUpdated(filaSeleccionada, filaSeleccionada);
            
            notificarCambioEnProyecto();
            
            actualizarEstadoExportacionUI();
        }
    } // --- Fin del método solicitarAlternarIgnorarComprimido ---
    
    
    public void solicitarAsignacionManual() {
        // Esta acción ahora es idéntica a "Añadir Archivo Asociado".
        // Simplemente delegamos la llamada al método orquestador.
        solicitarAnadirArchivoAsociado();
    } // --- Fin del método solicitarAsignacionManual ---

    
    public void solicitarQuitarDeLaCola() {
        if (exportQueueManager == null || registry == null) return;
        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) return;
        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
        modelo.proyecto.ExportItem itemSeleccionado = modelTabla.getItemAt(tablaExportacion.getSelectedRow());
        if (itemSeleccionado != null) {
            exportQueueManager.getColaDeExportacion().remove(itemSeleccionado);
            modelTabla.setCola(exportQueueManager.getColaDeExportacion());
            actualizarEstadoExportacionUI();
        }
    } // --- Fin del método solicitarQuitarDeLaCola ---

    
    public void solicitudAlternarMarcaImagen() {
        if (model == null) {
            logger.error("ERROR [solicitudAlternarMarcaImagen]: El modelo es nulo.");
            return;
        }

        String listaActiva = model.getProyectoListContext().getNombreListaActiva();
        logger.debug("  [ProjectController] Solicitud para alternar marca. Lista activa: '" + listaActiva + "'");

        if ("seleccion".equals(listaActiva)) {
            logger.debug("    -> Foco en 'seleccion'. Moviendo a descartes...");
            this.moverSeleccionActualADescartes();
        } else if ("descartes".equals(listaActiva)) {
            logger.debug("    -> Foco en 'descartes'. Restaurando a selección...");
            this.restaurarDesdeDescartes();
        } else {
            logger.warn("WARN [solicitudAlternarMarcaImagen]: Lista activa desconocida ('" + listaActiva + "'). No se realiza ninguna acción.");
        }
        
        // Después de mover la imagen, notificamos el cambio y actualizamos el título.
        projectManager.notificarModificacion();
        
    } // --- Fin del método solicitudAlternarMarcaImagen ---
    
    
    
    
// ********************************************************************************************
// *********************************************** MÉTODOS PARA GESTIÓN DE ARCHIVOS DE PROYECTO    
// ********************************************************************************************

    
    /**
     * Orquesta la creación de un nuevo proyecto.
     * Limpia el estado del proyecto y devuelve al usuario al modo Visualizador.
     */
    public void solicitarNuevoProyecto() {
        if (projectManager == null || generalController == null) {
            logger.error("ERROR [solicitarNuevoProyecto]: Dependencias nulas.");
            return;
        }
        
        projectManager.nuevoProyecto();
        
        logger.info("Nuevo proyecto creado en el backend (ProjectManager).");
        
        limpiarCacheRenderersProyecto();
        limpiarVistaProyecto();
        
        JTable tabla = getTablaExportacionDesdeRegistro();
        if (tabla != null && tabla.getModel() instanceof ExportTableModel) {
            ((ExportTableModel) tabla.getModel()).clear();
            logger.debug("[ProjectController] Tabla de exportación limpiada para nuevo proyecto.");
        }
        
        logger.info("Volviendo al modo Visualizador después de crear nuevo proyecto...");
        generalController.cambiarModoDeTrabajo(VisorModel.WorkMode.VISUALIZADOR);
        
        generalController.actualizarTituloVentana();
    } // ---FIN de metodo solicitarNuevoProyecto---
    
    
    public void solicitarAbrirProyecto(Path rutaArchivo) {
        if (projectManager == null || generalController == null || model == null) {
            logger.error("ERROR [solicitarAbrirProyecto]: Dependencias nulas.");
            return;
        }

        try {
            // --- INICIO DE REFUERZO DE ROBUSTEZ ---
            // PASO 1: Limpiar COMPLETAMENTE el estado anterior ANTES de cargar el nuevo.
            // Esto es crucial para evitar "fugas" de datos entre proyectos.
            logger.debug("Limpiando estado del proyecto anterior antes de abrir uno nuevo...");
            limpiarEstadoCompletoDelProyecto();
            // --- FIN DE REFUERZO DE ROBUSTEZ ---

            // PASO 2: Cargar los datos del nuevo proyecto en el backend.
            projectManager.abrirProyecto(rutaArchivo);
            projectManager.markProjectAsSaved();
            
            // PASO 3: Actualizar el título de la ventana.
            generalController.actualizarTituloVentana();

            // PASO 4: Si estamos en modo proyecto, refrescar la vista para mostrar los nuevos datos.
            if (model.isEnModoProyecto()) {
                logger.info("Ya se está en modo proyecto. Refrescando la vista con el nuevo proyecto cargado...");
                activarVistaProyecto();
            }
            
        } catch (java.io.IOException e) {
            logger.error("Falló la carga del proyecto: {}", e.getMessage());
            Component parentWindow = (view != null) ? view : null;
            JOptionPane.showMessageDialog(
                parentWindow,
                "No se pudo abrir el archivo de proyecto.\n" + e.getMessage(),
                "Error al Abrir Proyecto",
                JOptionPane.ERROR_MESSAGE
            );
        }
    } // ---FIN de metodo solicitarAbrirProyecto---
    
    
    public void solicitarGuardarProyecto() {
        if (projectManager == null) return;

        if (projectManager.getArchivoProyectoActivo() == null) {
            // Si el proyecto es temporal, la acción "Guardar" debe comportarse como "Guardar Como".
            Action guardarComoAction = actionMap.get(AppActionCommands.CMD_PROYECTO_GUARDAR_COMO);
            if (guardarComoAction != null) {
                guardarComoAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
            }
        } else {
            // Si ya tiene nombre, simplemente guarda. El archivo ya está actualizado por las acciones.
        	sincronizarModeloConUI();
            sincronizarArchivosAsociadosConModelo();
            sincronizarDescripcionDesdeUI();
            sincronizarMetadatosParaGuardado();
            
            projectManager.guardarAArchivo();
            projectManager.markProjectAsSaved(); // Marcamos como "limpio" tras guardar.
            generalController.actualizarTituloVentana();
            
            
            logger.info("Proyecto {} guardado.", projectManager.getNombreProyectoActivo());
        }
        
        projectManager.markProjectAsSaved();
        
    } // ---FIN de metodo solicitarGuardarProyecto---

    
    /**
     * Orquesta el guardado del proyecto actual en una nueva ubicación.
     * Muestra un JFileChooser y, si tiene éxito, guarda el proyecto.
     * Confía en que el ProjectModel en memoria es la fuente de la verdad.
     */
    public void solicitarGuardarProyectoComo() {
    	
    	if (projectManager == null || generalController == null || view == null) {
            logger.error("ERROR [solicitarGuardarProyectoComo]: Dependencias nulas.");
            return;
        }
        
        int size = projectManager.getCurrentProject().getSelectedImages().size();
        logger.info(">>>>>>>>>> [GUARDADO] Al solicitar 'Guardar Como...', ProjectModel en memoria tiene {} imágenes seleccionadas.", size);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Proyecto Como...");
        fileChooser.setCurrentDirectory(projectManager.getCarpetaBaseProyectos().toFile());
        javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter("Archivos de Proyecto (*.prj)", "prj");
        fileChooser.setFileFilter(filter);
        fileChooser.setSelectedFile(new java.io.File("MiProyecto.prj"));

        int result = fileChooser.showSaveDialog(view);
        if (result != JFileChooser.APPROVE_OPTION) {
            return; 
        }

        Path archivoDestino = fileChooser.getSelectedFile().toPath();
        if (!archivoDestino.toString().toLowerCase().endsWith(".prj")) {
            archivoDestino = archivoDestino.resolveSibling(archivoDestino.getFileName().toString() + ".prj");
        }

        if (java.nio.file.Files.exists(archivoDestino)) {
            int overwriteConfirm = JOptionPane.showConfirmDialog(
                view, "El archivo ya existe. ¿Deseas sobrescribirlo?", "Confirmar Sobrescribir",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
            );
            if (overwriteConfirm != JOptionPane.YES_OPTION) {
                return;
            }
        }
        
        // Simplemente le pide al manager que guarde su estado actual en la nueva ruta.
        sincronizarModeloConUI();
        sincronizarArchivosAsociadosConModelo();
        sincronizarDescripcionDesdeUI();
        sincronizarMetadatosParaGuardado();
        
        projectManager.guardarProyectoComo(archivoDestino);
        projectManager.limpiarArchivoTemporal();
        projectManager.markProjectAsSaved();
        generalController.actualizarTituloVentana();
        
        // Restaura el feedback vital para el usuario.
        JOptionPane.showMessageDialog(
            view, 
            "El proyecto se ha guardado correctamente como '" + archivoDestino.getFileName().toString() + "'.", 
            "Proyecto Guardado", 
            JOptionPane.INFORMATION_MESSAGE
        );
    } // ---FIN de metodo solicitarGuardarProyectoComo---
    
    
    /**
     * Actualiza los metadatos del ProjectModel (nombre, fecha de modificación)
     * justo antes de una operación de guardado.
     * El nombre del proyecto se deriva del nombre del archivo activo.
     */
    private void sincronizarMetadatosParaGuardado() {
        if (projectManager == null) return;
        
        ProjectModel currentProject = projectManager.getCurrentProject();
        if (currentProject == null) return;
        
        Path archivoActivo = projectManager.getArchivoProyectoActivo();

        if (archivoActivo != null) {
            String fileName = archivoActivo.getFileName().toString();
            if (fileName.toLowerCase().endsWith(".prj")) {
                fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            }
            currentProject.setProjectName(fileName);
        } else {
            currentProject.setProjectName("Proyecto Temporal");
        }
        
        currentProject.setLastModifiedDate(System.currentTimeMillis());
        logger.debug("Metadatos del proyecto (nombre y fecha) sincronizados para guardado.");
    } // ---FIN de metodo sincronizarMetadatosParaGuardado---
    
    
    /**
     * Sincroniza el 'ProjectModel' en el ProjectManager con el estado actual
     * de las JLists de la interfaz de usuario (selección y descartes).
     * Este método es el puente que restaura la robustez del sistema, asegurando
     * que el estado visual actual se consolide en el modelo de datos antes de
     * cualquier operación de persistencia.
     */
    public void sincronizarModeloConUI() {
        if (projectManager == null || registry == null) {
            logger.warn("WARN [sincronizarModeloConUI]: ProjectManager o Registry son nulos. Sincronización abortada.");
            return;
        }

        logger.debug("[ProjectController] Iniciando sincronización de UI -> Modelo de Proyecto...");

        ProjectModel modeloActual = projectManager.getCurrentProject();
        if (modeloActual == null) {
            logger.error("ERROR CRÍTICO [sincronizarModeloConUI]: El ProjectModel en ProjectManager es nulo.");
            return;
        }
        
        // Preservamos el mapa de etiquetas existente para no perderlo al reconstruir la lista de selección.
        Map<String, String> etiquetasExistentes = new HashMap<>(modeloActual.getSelectedImages());

        // 1. Sincronizar la lista de SELECCIÓN desde la UI al Modelo
        JList<String> listaSeleccionUI = registry.get("list.proyecto.nombres");
        if (listaSeleccionUI != null && listaSeleccionUI.getModel() != null) {
            modeloActual.getSelectedImages().clear();
            javax.swing.ListModel<String> modeloUI = listaSeleccionUI.getModel();
            for (int i = 0; i < modeloUI.getSize(); i++) {
                String clave = modeloUI.getElementAt(i);
                String etiqueta = etiquetasExistentes.get(clave); // Recuperar etiqueta si existía
                modeloActual.getSelectedImages().put(clave, etiqueta);
            }
        }

        // 2. Sincronizar la lista de DESCARTES desde la UI al Modelo
        JList<String> listaDescartesUI = registry.get("list.proyecto.descartes");
        if (listaDescartesUI != null && listaDescartesUI.getModel() != null) {
            modeloActual.getDiscardedImages().clear();
            javax.swing.ListModel<String> modeloUI = listaDescartesUI.getModel();
            for (int i = 0; i < modeloUI.getSize(); i++) {
                modeloActual.getDiscardedImages().add(modeloUI.getElementAt(i));
            }
        }

        logger.info("[ProjectController] Sincronización de UI -> Modelo completada. El modelo está listo para guardarse.");
    } // ---FIN de metodo sincronizarModeloConUI---
    
    
    /**
     * Sincroniza el mapa de 'associatedFiles' en el ProjectModel con el estado
     * actual de la cola de exportación (ExportQueueManager).
     * ESTA ES LA FUENTE DE VERDAD PARA LA PERSISTENCIA DE ASOCIACIONES.
     * 1. Fuerza la actualización de la cola de exportación desde la selección actual.
     * 2. Limpia el mapa 'associatedFiles' en el modelo.
     * 3. Reconstruye el mapa 'associatedFiles' a partir de la cola actualizada.
     */
    public void sincronizarArchivosAsociadosConModelo() {
        if (projectManager == null || exportQueueManager == null) {
            logger.warn("[sincronizarArchivosAsociados] Sincronización abortada (dependencias nulas).");
            return;
        }
        
        logger.info("[ProjectController] Iniciando sincronización de Cola de Exportación -> Modelo de Proyecto...");
        
        ProjectModel modeloActual = projectManager.getCurrentProject();
        if (modeloActual == null) {
            logger.error("ERROR CRÍTICO [sincronizarArchivosAsociados]: El ProjectModel en ProjectManager es nulo.");
            return;
        }

        // 1. Obtener el mapa de configuraciones del modelo y limpiarlo.
        Map<String, modelo.proyecto.ExportConfig> exportConfigsMap = modeloActual.getExportConfigs();
        exportConfigsMap.clear();

        // 2. Obtener el estado actual de la cola de exportación.
        List<ExportItem> colaActual = exportQueueManager.getColaDeExportacion();
        int contador = 0;

        // 3. Iterar sobre la cola y construir un objeto ExportConfig para cada item.
        for (ExportItem item : colaActual) {
            // Creamos un nuevo objeto de configuración.
            modelo.proyecto.ExportConfig config = new modelo.proyecto.ExportConfig();

            // Guardamos el estado del checkbox.
            config.setExportEnabled(item.isSeleccionadoParaExportar());
            
            // Guardamos el estado de "ignorar".
            config.setIgnoreCompressed(item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO);
            
            // Guardamos la lista de archivos asociados.
            if (item.getRutasArchivosAsociados() != null && !item.getRutasArchivosAsociados().isEmpty()) {
                List<String> rutasComoString = item.getRutasArchivosAsociados().stream()
                    .map(path -> path.toString().replace("\\", "/"))
                    .collect(Collectors.toList());
                config.setAssociatedFiles(rutasComoString);
            }
            
            // 4. Guardar el objeto de configuración completo en el mapa del modelo.
            String claveImagen = item.getRutaImagen().toString().replace("\\", "/");
            exportConfigsMap.put(claveImagen, config);
            contador++;
        }

        logger.info("[ProjectController] Sincronización de configuración de exportación completada. Se persistirán {} entradas.", contador);
    } // ---FIN de metodo sincronizarArchivosAsociadosConModelo---
    
    
    /**
     * Carga los metadatos (nombre, descripción) desde el ProjectModel
     * al panel de propiedades en la UI.
     */
    private void actualizarPanelDePropiedadesEnUI() {
        if (projectManager == null || registry == null) return;
        
        vista.panels.export.ProjectMetadataPanel propsPanel = registry.get("panel.proyecto.propiedades");
        ProjectModel currentProject = projectManager.getCurrentProject();

        if (propsPanel != null && currentProject != null) {
            String name = projectManager.getNombreProyectoActivo(); // Usamos el método que ya es inteligente
            if (name.toLowerCase().endsWith(".prj")) {
                name = name.substring(0, name.lastIndexOf('.'));
            }
            
            String description = currentProject.getProjectDescription() != null ? currentProject.getProjectDescription() : "";

            propsPanel.getProjectNameLabel().setText(name);
            propsPanel.getProjectDescriptionArea().setText(description);
            logger.debug("Panel de propiedades en la UI actualizado con los datos del modelo.");
        }
    } // ---FIN de metodo [actualizarPanelDePropiedadesEnUI]---

    /**
     * Sincroniza la descripción del proyecto desde el campo de texto de la UI
     * hacia el ProjectModel en memoria.
     */
    void sincronizarDescripcionDesdeUI() {
        if (projectManager == null || registry == null) return;
        
        vista.panels.export.ProjectMetadataPanel propsPanel = registry.get("panel.proyecto.propiedades");
        ProjectModel currentProject = projectManager.getCurrentProject();

        if (propsPanel != null && currentProject != null) {
            currentProject.setProjectDescription(propsPanel.getProjectDescriptionArea().getText());
            logger.debug("Descripción del ProjectModel sincronizada desde la UI.");
        }
    } // ---FIN de metodo [sincronizarDescripcionDesdeUI]---
    
    
    /**
     * Actualiza los títulos de los paneles "Selección Actual" y "Descartes"
     * con el número correcto de elementos de sus respectivas listas.
     */
    public void actualizarContadoresDeTitulos() {
        if (registry == null || generalController == null || generalController.getVisorController() == null || generalController.getVisorController().getThemeManager() == null) return;
        
        JPanel panelSeleccion = registry.get("panel.proyecto.seleccion.container");
        JPanel panelDescartes = registry.get("panel.proyecto.descartes.container");
        java.awt.Color titleColor = generalController.getVisorController().getThemeManager().getTemaActual().colorBordeTitulo();

        // Actualizar título para "Selección Actual"
        if (panelSeleccion != null && panelSeleccion.getBorder() instanceof javax.swing.border.TitledBorder) {
            javax.swing.border.TitledBorder border = (javax.swing.border.TitledBorder) panelSeleccion.getBorder();
            JList<?> list = registry.get("list.proyecto.nombres");
            int count = (list != null && list.getModel() != null) ? list.getModel().getSize() : 0;
            border.setTitle("Selección Actual: " + count);
            border.setTitleColor(titleColor);
            panelSeleccion.repaint();
        }

        // Actualizar título para "Descartes"
        if (panelDescartes != null && panelDescartes.getBorder() instanceof javax.swing.border.TitledBorder) {
            javax.swing.border.TitledBorder border = (javax.swing.border.TitledBorder) panelDescartes.getBorder();
            JList<?> list = registry.get("list.proyecto.descartes");
            int count = (list != null && list.getModel() != null) ? list.getModel().getSize() : 0;
            border.setTitle("Descartes: " + count);
            border.setTitleColor(titleColor);
            panelDescartes.repaint();
        }
        logger.debug("[ProjectController] Contadores de títulos de paneles actualizados.");
    } // ---FIN de metodo [actualizarContadoresDeTitulos]---
    
// ********************************************************************************************
// ********************************************************** METODOS DE LA TOOLBAR DE PROYECTO    
// ********************************************************************************************
    
    
    public void solicitarEliminacionPermanente() {
        if (registry == null || projectManager == null || view == null) return;
        
        JList<String> listaDescartesUI = registry.get("list.proyecto.descartes");
        
        if (listaDescartesUI == null) return;
        
        String claveSeleccionada = listaDescartesUI.getSelectedValue();
        
        if (claveSeleccionada == null || claveSeleccionada.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(view, "¿Seguro que quieres eliminar esta imagen del proyecto?\n(No se borrará el archivo del disco)", "Confirmar Eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            Path rutaAbsoluta = java.nio.file.Paths.get(claveSeleccionada);
            // 1. Modifica el modelo en memoria
            projectManager.eliminarDeProyecto(rutaAbsoluta);
            projectManager.notificarModificacion();
            
            // 2. Guarda el estado actual y correcto INMEDIATAMENTE
            projectManager.guardarAArchivo();
            
            // 3. Refresca la UI (en este caso, refrescar las listas es suficiente)
            
            refrescarListasDeProyecto();
        }
    } // --- Fin del método solicitarEliminacionPermanente ---
    
        
    private void actualizarAparienciaListasPorFoco() {
    	if (registry == null || model == null || generalController == null || generalController.getVisorController() == null || generalController.getVisorController().getThemeManager() == null) return;
        JList<String> projectList = registry.get("list.proyecto.nombres");
        JList<String> descartesList = registry.get("list.proyecto.descartes");
        if (projectList == null || descartesList == null) return;
        String focoActivo = model.getProyectoListContext().getNombreListaActiva();
        vista.theme.Tema tema = generalController.getVisorController().getThemeManager().getTemaActual();
        java.awt.Color colorFondoActivo = tema.colorFondoSecundario();
        java.awt.Color colorFondoInactivo = tema.colorBorde(); 
        java.awt.Color colorTextoActivo = tema.colorTextoPrimario();
        java.awt.Color colorTextoInactivo = tema.colorTextoSecundario().brighter();
        if ("seleccion".equals(focoActivo)) {
            projectList.setBackground(colorFondoActivo);
            projectList.setForeground(colorTextoActivo);
            descartesList.setBackground(colorFondoInactivo);
            descartesList.setForeground(colorTextoInactivo);
        } else { 
            projectList.setBackground(colorFondoInactivo);
            projectList.setForeground(colorTextoInactivo);
            descartesList.setBackground(colorFondoActivo);
            descartesList.setForeground(colorTextoActivo);
        }
        
        projectList.repaint();
        descartesList.repaint();
    } // --- Fin del método actualizarAparienciaListasPorFoco ---
    
    
    /**
     * Crea un MouseListener que muestra un menú contextual en un JComponent.
     * @param component El componente al que se asociará el menú.
     * @param menuItems Una lista de Actions o Separators para el menú.
     * @return Un MouseAdapter configurado.
     */
    private java.awt.event.MouseAdapter createContextMenuListener(javax.swing.JComponent component, Object... menuItems) {
        return new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
            public void mouseReleased(java.awt.event.MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }

            
            private void showMenu(java.awt.event.MouseEvent e) {
                logger.debug("[ContextMenuListener] Evento de popup detectado en el componente: {}", component.getClass().getSimpleName());
                
                if (component instanceof JTable) {
                    JTable table = (JTable) component;
                    int row = table.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        if (table.getSelectedRow() != row) {
                            table.setRowSelectionInterval(row, row);
                        }
                    } else {
                        logger.debug("[ContextMenuListener] Clic en área vacía de la tabla. No se mostrará el menú.");
                        return;
                    }
                }
                
                javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();
                for (Object item : menuItems) {
                    if (item instanceof Action) {
                        Action action = (Action) item;
                        
                        // Si la acción es sensible al contexto, le pedimos que se actualice ahora mismo.
                        if (action instanceof ContextSensitiveAction) {
                            ((ContextSensitiveAction) action).updateEnabledState(model);
                        }
                        menu.add(action);
                    } else if (item instanceof javax.swing.JPopupMenu.Separator) {
                        menu.addSeparator();
                    }
                }
                
                if (menu.getComponentCount() > 0) {
                    logger.debug("[ContextMenuListener] Mostrando menú con {} componentes.", menu.getComponentCount());
                    menu.show(e.getComponent(), e.getX(), e.getY());
                } else {
                    logger.debug("[ContextMenuListener] El menú no tiene componentes, no se mostrará.");
                }
            }
        };
    } // ---FIN de metodo [createContextMenuListener]---
    
    
    /**
     * Construye y devuelve un JPopupMenu dinámico para el visor principal (Single o Grid)
     * del modo proyecto. El contenido del menú depende del estado de vista actual y del modo de visualización.
     * @return Un JPopupMenu configurado o null si no debe mostrarse.
     */
    public JPopupMenu crearMenuContextualVisorManualmente() {
        // Regla 1: No mostrar menú si no hay imagen seleccionada.
        if (model.getSelectedImageKey() == null || model.getSelectedImageKey().isEmpty()) {
            logger.debug("[MenuContextualVisor] No se muestra el menú porque no hay imagen seleccionada.");
            return null;
        }

        logger.debug("[MenuContextualVisor] Creando menú manualmente para el estado de vista: {} y modo display: {}", currentViewState, model.getCurrentDisplayMode());
        
        JPopupMenu menu = new JPopupMenu();
        boolean isGridMode = (model.getCurrentDisplayMode() == VisorModel.DisplayMode.GRID);

        // Paso 1: Añadir acciones basadas en el contexto de la lista activa (esto es igual para Grid y Single).
        switch (currentViewState) {
            case VIEW_SELECTION:
            case VIEW_EXPORT:
                menu.add(actionMap.get(AppActionCommands.CMD_PROYECTO_MOVER_A_DESCARTES));
                menu.add(actionMap.get(AppActionCommands.CMD_PROYECTO_LOCALIZAR_ARCHIVO));
                break;
            case VIEW_DISCARDS:
                menu.add(actionMap.get(AppActionCommands.CMD_PROYECTO_RESTAURAR_DE_DESCARTES));
                menu.add(actionMap.get(AppActionCommands.CMD_PROYECTO_LOCALIZAR_ARCHIVO));
                menu.addSeparator();
                menu.add(actionMap.get(AppActionCommands.CMD_PROYECTO_ELIMINAR_PERMANENTEMENTE));
                break;
        }
        
        menu.addSeparator();

        // Paso 2: Añadir acciones de Paneo/Zoom, pero deshabilitarlas si estamos en modo Grid.
        Action toggleZoomAction = actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
        if (toggleZoomAction != null) {
            JCheckBoxMenuItem toggleZoomItem = new JCheckBoxMenuItem(toggleZoomAction);
            toggleZoomItem.setSelected(model.isZoomHabilitado());
            toggleZoomItem.setEnabled(!isGridMode); // <-- LA CLAVE: Deshabilitado si es Grid
            menu.add(toggleZoomItem);
        }

        Action resetZoomAction = actionMap.get(AppActionCommands.CMD_ZOOM_RESET);
        if (resetZoomAction != null) {
            JMenuItem resetZoomItem = new JMenuItem(resetZoomAction);
            resetZoomItem.setEnabled(!isGridMode); // <-- LA CLAVE: Deshabilitado si es Grid
            menu.add(resetZoomItem);
        }
        
        return menu;
    } // ---FIN de metodo [crearMenuContextualVisorManualmente]---
    
    
    /**
     * El método central para refrescar TODA la UI del modo proyecto.
     * Repuebla las JLists, la tabla de exportación y sincroniza la lista maestra
     * basándose en el estado actual del ProjectManager.
     * Esta es la única fuente de verdad para actualizar la vista.
     */
    public void refrescarVistaProyectoCompleta() {
        logger.info("[ProjectController] Iniciando refresco completo de la vista del proyecto...");

        poblarListasSeleccionYDescartes();

        if (isExportPanelVisible()) {
            logger.debug(" -> Panel de exportación visible. Preparando nueva cola...");
            solicitarPreparacionColaExportacion();
        }

        actualizarModeloPrincipalConListaDeProyectoActiva();
        sincronizarSeleccionEnGridProyecto();
        
        refrescarGridProyecto(); // Asegura que el repintado ocurra siempre.

        logger.info("[ProjectController] Refresco completo de la vista del proyecto finalizado.");
    } // ---FIN de metodo [refrescarVistaProyectoCompleta]---
    
    
    /**
     * Configura el menú contextual para la tabla de exportación.
     * Debe ser llamado una vez durante la inicialización de la UI del proyecto.
     */
    public void configurarContextMenuTablaExportacion() {
        logger.debug("[DIAGNÓSTICO] Se ha llamado a configurarContextMenuTablaExportacion().");

        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || actionMap == null) {
            logger.error("[DIAGNÓSTICO] No se puede configurar menú: tablaExportacion es {} y actionMap es {}.",
                (tablaExportacion == null ? "NULL" : "OK"),
                (actionMap == null ? "NULL" : "OK"));
            return;
        }

        // Limpiamos listeners antiguos para evitar duplicados
        for(java.awt.event.MouseListener ml : tablaExportacion.getMouseListeners()){
            if(ml.getClass().getName().contains("ContextMenuListener")){ // Una forma de identificar nuestro listener
                tablaExportacion.removeMouseListener(ml);
            }
        }

        // Obtenemos las acciones que queremos en el menú
        Action quitarAction = actionMap.get(AppActionCommands.CMD_EXPORT_QUITAR_DE_COLA);
        Action asignarAction = actionMap.get(AppActionCommands.CMD_EXPORT_ASIGNAR_ARCHIVO);
        Action ignorarAction = actionMap.get(AppActionCommands.CMD_EXPORT_IGNORAR_COMPRIMIDO);
        Action relocalizarAction = actionMap.get(AppActionCommands.CMD_EXPORT_RELOCALIZAR_IMAGEN);
        Action abrirUbicacionAction = actionMap.get(AppActionCommands.CMD_EXPORT_ABRIR_UBICACION);

        // Creamos el listener usando el método helper
        java.awt.event.MouseAdapter contextMenuListener = createContextMenuListener(tablaExportacion,
            asignarAction,
            quitarAction,
            new javax.swing.JPopupMenu.Separator(),
            ignorarAction,
            relocalizarAction,
            new javax.swing.JPopupMenu.Separator(),
            abrirUbicacionAction
        );

        // Asignamos el listener a la tabla
        tablaExportacion.addMouseListener(contextMenuListener);
        logger.debug("[DIAGNÓSTICO] Menú contextual configurado y listener añadido a la tabla de exportación.");
    } // ---FIN de metodo [configurarContextMenuTablaExportacion]---
    
    
    public void solicitarRelocalizacionImagen() {
        if (registry == null || exportQueueManager == null || view == null) return;
        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) return;
        int filaSeleccionada = tablaExportacion.getSelectedRow();
        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
        modelo.proyecto.ExportItem item = modelTabla.getItemAt(filaSeleccionada);
        if (item == null || item.getEstadoArchivoComprimido() != modelo.proyecto.ExportStatus.IMAGEN_NO_ENCONTRADA) {
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Relocalizar Imagen: " + item.getRutaImagen().getFileName());
        javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter(
            "Imágenes (jpg, png, gif, bmp)", "jpg", "jpeg", "png", "gif", "bmp");
        fileChooser.setFileFilter(filter);
        int result = fileChooser.showOpenDialog(view);
        if (result == JFileChooser.APPROVE_OPTION) {
            Path nuevaRuta = fileChooser.getSelectedFile().toPath();
            ExportItem newItem = new ExportItem(nuevaRuta);
            exportQueueManager.buscarArchivoComprimidoAsociado(newItem);
            exportQueueManager.getColaDeExportacion().set(filaSeleccionada, newItem);
            modelTabla.fireTableRowsUpdated(filaSeleccionada, filaSeleccionada);
            actualizarEstadoExportacionUI();
        }
    } // --- Fin del método solicitarRelocalizacionImagen ---
    
    
    /**
     * Orquesta el movimiento de una imagen de la lista de selección a la de descartes.
     * Es llamado por acciones de la UI, como "Quitar de la cola de exportación".
     */
    public void solicitarMoverSeleccionadoAdescartes() {
        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) {
            logger.warn("Se intentó mover a descartes sin selección en la tabla de exportación.");
            return;
        }

        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
        ExportItem selectedItem = modelTabla.getItemAt(tablaExportacion.getSelectedRow());

        if (selectedItem != null) {
            logger.debug("Solicitud para mover a descartes: {}", selectedItem.getRutaImagen().getFileName());
            
            // Paso 1: Ordenar el cambio en la fuente de verdad de datos.
            projectManager.moverAdescartes(selectedItem.getRutaImagen());
            
            projectManager.notificarModificacion();
            
            // Paso 2: Ordenar un refresco completo y sincronizado de toda la UI.
            refrescarVistaProyectoCompleta();
        }
    } // ---FIN de metodo [solicitarMoverSeleccionadoAdescartes]---
    
    
    public void navegarTablaExportacionConRueda(java.awt.event.MouseWheelEvent e) {
        if (registry == null || model == null) {
            logger.warn("WARN [navegarTablaExportacionConRueda]: Registry o Modelo nulos.");
            return;
        }

        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getModel().getRowCount() == 0) {
            logger.debug("  [navegarTablaExportacionConRueda] Tabla de exportación vacía o no encontrada. No se puede navegar.");
            return;
        }

        int currentRow = tablaExportacion.getSelectedRow();
        int newRow;
        int totalRows = tablaExportacion.getModel().getRowCount();

        if (currentRow == -1) {
            newRow = (e.getWheelRotation() < 0) ? 0 : totalRows - 1;
        } else {
            newRow = currentRow + e.getWheelRotation();
            newRow = Math.max(0, Math.min(newRow, totalRows - 1));
        }

        if (newRow != currentRow) {
            tablaExportacion.setRowSelectionInterval(newRow, newRow);
            tablaExportacion.scrollRectToVisible(tablaExportacion.getCellRect(newRow, 0, true));
            logger.debug("  [navegarTablaExportacionConRueda] Selector movido a la fila: " + newRow);
        } else {
            logger.debug("  [navegarTablaExportacionConRueda] Selector no cambió. Fila actual: " + currentRow);
        }
    }// --- Fin del nuevo método navegarTablaExportacionConRueda ---
    
    
    public void mostrarImagenDeExportacion(Path rutaImagen) {
        logger.debug("[ProjectController] Solicitud para mostrar imagen de exportación: " + rutaImagen);
        
        if (model == null || projectListCoordinator == null) {
            logger.error("ERROR [mostrarImagenDeExportacion]: Modelo o ProjectListCoordinator nulos.");
            return;
        }
        if (rutaImagen == null) {
            logger.warn("WARN [mostrarImagenDeExportacion]: Ruta de imagen nula. Limpiando visor principal.");
            projectListCoordinator.seleccionarImagenPorIndice(-1); 
            return;
        }
        
        String claveImagen = rutaImagen.toString().replace("\\", "/");
        
        projectListCoordinator.seleccionarImagenPorClave(claveImagen);

    } // --- Fin del nuevo método mostrarImagenDeExportacion ---
    
    
    public void solicitarLocalizarArchivoSeleccionado() {
        if (model == null) return;
        String claveSeleccionada = model.getSelectedImageKey();
        if (claveSeleccionada == null || claveSeleccionada.isEmpty()) {
            logger.debug("[ProjectController] No hay imagen seleccionada para localizar.");
            return;
        }

        Path rutaAbsoluta = model.getProyectoListContext().getRutaCompleta(claveSeleccionada);
        if (rutaAbsoluta != null) {
            try {
                DesktopUtils.openAndSelectFile(rutaAbsoluta);
            } catch (Exception e) {
                logger.error("Error al intentar abrir y seleccionar el archivo: " + e.getMessage());
                JOptionPane.showMessageDialog(view, "No se pudo abrir la ubicación del archivo.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    } // --- Fin del nuevo método solicitarLocalizarArchivoSeleccionado ---
    

    public void solicitarVaciarDescartes() {
        if (projectManager == null || view == null) return;

        int confirm = JOptionPane.showConfirmDialog(
            view,
            "¿Seguro que quieres vaciar TODAS las imágenes de la lista de descartes?\n" +
            "(Las imágenes se eliminaran del proyecto pero no del disco)",
            "Confirmar Vaciar Descartes",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
        	
        	projectManager.vaciarDescartes();
        	projectManager.notificarModificacion();
            
            refrescarListasDeProyecto();
        }
    } // --- Fin del nuevo método solicitarVaciarDescartes ---
    
    
    public void solicitarEtiquetaParaImagenSeleccionada() {
        if (model.getCurrentDisplayMode() != VisorModel.DisplayMode.GRID) return;

        String claveSeleccionada = model.getSelectedImageKey();
        if (claveSeleccionada == null || claveSeleccionada.isBlank()) return;

        Path ruta = model.getRutaCompleta(claveSeleccionada);
        if (ruta == null) return;

        String etiquetaActual = projectManager.getEtiqueta(ruta);
        if (etiquetaActual == null) etiquetaActual = "";

        String nuevaEtiqueta = (String) JOptionPane.showInputDialog(
            view, 
            "Introduce la etiqueta para:\n" + ruta.getFileName().toString(),
            "Editar Etiqueta", 
            JOptionPane.PLAIN_MESSAGE, 
            null, 
            null, 
            etiquetaActual
        );

        if (nuevaEtiqueta != null) {
            projectManager.setEtiqueta(ruta, nuevaEtiqueta);
            
            projectManager.notificarModificacion();
            
            refrescarGridProyecto();
        }
    } // ---FIN de metodo solicitarEtiquetaParaImagenSeleccionada---

    
    public void solicitarBorradoEtiquetaParaImagenSeleccionada() {
        if (model.getCurrentDisplayMode() != VisorModel.DisplayMode.GRID) return;

        String claveSeleccionada = model.getSelectedImageKey();
        if (claveSeleccionada == null || claveSeleccionada.isBlank()) return;

        Path ruta = model.getRutaCompleta(claveSeleccionada);
        if (ruta != null) {
            projectManager.setEtiqueta(ruta, null);
            
            projectManager.notificarModificacion();
            
            refrescarGridProyecto();
        }
    } // ---FIN de metodo solicitarBorradoEtiquetaParaImagenSeleccionada---

    
    public void cambiarTamanoGrid(double factor) {
        if (model.getCurrentDisplayMode() != VisorModel.DisplayMode.GRID) return;
        
        vista.panels.GridDisplayPanel gridPanel = registry.get("panel.display.grid.proyecto");
        if (gridPanel == null) return;

        int currentWidth = gridPanel.getGridList().getFixedCellWidth();
        int currentHeight = gridPanel.getGridList().getFixedCellHeight();
        java.awt.Dimension currentSize = new java.awt.Dimension(currentWidth, currentHeight);
        
        if (currentSize == null || currentSize.width <= 0) {
             currentSize = new java.awt.Dimension(132, 132);
        }
        
        int nuevoAncho = (int) (currentSize.width * factor);
        int nuevoAlto = (int) (currentSize.height * factor);

        nuevoAncho = Math.max(50, Math.min(nuevoAncho, 500));
        nuevoAlto = Math.max(50, Math.min(nuevoAlto, 500));
        
        gridPanel.setGridCellSize(nuevoAncho, nuevoAlto);
        
        ajustarPosicionDivisorDerecho();
        
    } // ---FIN de metodo cambiarTamanoGrid---

    
    private void refrescarGridProyecto() {
        JList<String> gridList = registry.get("list.grid.proyecto");
        if (gridList != null) {
            gridList.revalidate();
            gridList.repaint();
        }
    } // ---FIN de metodo refrescarGridProyecto---
    
    
    /**
     * Busca los renderers de las listas de proyecto en el registro y limpia su caché interno.
     * Esto es crucial para asegurar que el estado de "archivo no encontrado" se re-evalúe
     * cuando se carga o refresca un proyecto.
     */
    private void limpiarCacheRenderersProyecto() {
        if (registry == null) {
            return;
        }
        
        // Limpiar caché de la lista de Selección
        JList<String> listaNombres = registry.get("list.proyecto.nombres");
        if (listaNombres != null && listaNombres.getCellRenderer() instanceof vista.renderers.ProjectListCellRenderer) {
            ((vista.renderers.ProjectListCellRenderer) listaNombres.getCellRenderer()).clearCache();
        }

        // Limpiar caché de la lista de Descartes
        JList<String> listaDescartes = registry.get("list.proyecto.descartes");
        if (listaDescartes != null && listaDescartes.getCellRenderer() instanceof vista.renderers.ProjectListCellRenderer) {
            ((vista.renderers.ProjectListCellRenderer) listaDescartes.getCellRenderer()).clearCache();
        }
        
        logger.debug("[ProjectController] Caché de los renderers de listas de proyecto limpiado.");
    } // ---FIN de metodo [limpiarCacheRenderersProyecto]---
    
    
    public JTable getTablaExportacionDesdeRegistro() {
        if (registry == null) return null;
        
        // Usamos la clave correcta con la que registramos el panel en ProjectBuilder.
        vista.panels.export.ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");
        
        if (exportPanel != null) {
            return exportPanel.getTablaExportacion(); 
        }
        
        // Este log ahora nos ayudará a depurar si vuelve a fallar.
        logger.warn("WARN [ProjectController]: No se pudo encontrar 'ExportPanel' en el registro con la clave 'panel.proyecto.exportacion.completo'.");
        return null;
    } // --- Fin del método getTablaExportacionDesdeRegistro ---
    
    /**
     * Comprueba si el panel de herramientas de la derecha (que contiene la exportación)
     * está actualmente visible para el usuario.
     * @return true si el panel es visible, false en caso contrario.
     */
    public boolean isExportPanelVisible() {
        if (registry == null) return false;
        JPanel toolsPanel = registry.get("panel.proyecto.herramientas.container");
        return toolsPanel != null && toolsPanel.isVisible();
    } // ---FIN de metodo [isExportPanelVisible]---
    
    
    /**
     * Establece la visibilidad del panel de herramientas de exportación sin alterar el
     * estado lógico de la vista (ProjectViewState). Este método es utilizado por
     * GeneralController para restaurar la UI al volver al modo proyecto.
     *
     * @param visible true para mostrar el panel, false para ocultarlo.
     */
    public void setExportPanelVisible(boolean visible) {
        if (registry == null) return;
        JSplitPane rightSplit = registry.get("splitpane.proyecto.right");
        JPanel toolsPanel = registry.get("panel.proyecto.herramientas.container");
        if (rightSplit == null || toolsPanel == null) return;

        if (visible) {
            if (!toolsPanel.isVisible()) {
                toolsPanel.setVisible(true);
                rightSplit.setDividerSize(5);
                
                ensureExportPanelIsFullyInitialized();
                
                SwingUtilities.invokeLater(() -> {
                    if (lastRightDividerLocation > 0) {
                        rightSplit.setDividerLocation(lastRightDividerLocation);
                    } else {
                    	ajustarPosicionDivisorDerecho();
                    }
                });
                
                logger.debug("[ProjectController] Panel de exportación restaurado a visible.");
            }
        } else {
            if (toolsPanel.isVisible()) {
                lastRightDividerLocation = rightSplit.getDividerLocation();
                toolsPanel.setVisible(false);
                rightSplit.setDividerSize(0);
                logger.debug("[ProjectController] Panel de exportación ocultado programáticamente.");
            }
        }
        
        // Sincronizar el estado del botón de toggle
        Action toggleAction = actionMap.get(AppActionCommands.CMD_EXPORT_ASSIGN_PANNEL);
        if (toggleAction != null) {
            toggleAction.putValue(Action.SELECTED_KEY, visible);
        }
    } // ---FIN de metodo setExportPanelVisible---
    
    
    /**
     * Calcula y ajusta la posición del divisor del split pane derecho (vertical).
     * La posición se calcula para dejar espacio suficiente para ~1.5 filas de miniaturas
     * en el panel superior (el visor del grid).
     */
    public void ajustarPosicionDivisorDerecho() {
        if (registry == null) return;

        JSplitPane rightSplit = registry.get("splitpane.proyecto.right");
        vista.panels.GridDisplayPanel gridPanel = registry.get("panel.display.grid.proyecto");

        if (rightSplit != null && gridPanel != null && rightSplit.isVisible()) {
            SwingUtilities.invokeLater(() -> {
                int cellHeight = gridPanel.getGridList().getFixedCellHeight();
                if (cellHeight <= 0) cellHeight = 132;

                int desiredHeight = (int) (cellHeight * 1.5) + 15;
                
                // Asegurarnos de no poner el divisor en una posición inválida
                int maxLocation = rightSplit.getHeight() - rightSplit.getDividerSize() - 50; // Dejar un mínimo para el panel inferior
                desiredHeight = Math.min(desiredHeight, maxLocation);

                rightSplit.setDividerLocation(desiredHeight);
                logger.debug("Posición del divisor derecho ajustada a {}px.", desiredHeight);
            });
        }
    } // ---FIN de metodo [ajustarPosicionDivisorDerecho]---
    
    
    public void setProjectListCoordinator(ProjectListCoordinator coordinator) {
        this.projectListCoordinator = coordinator;
        if (this.projectListCoordinator != null) {
            this.projectListCoordinator.setProjectController(this);
        }
    }
    
    public void setGeneralController(GeneralController generalController) {
    	this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null en ProjectController");
    }
    
    public void setViewManager(IViewManager viewManager) { this.viewManager = Objects.requireNonNull(viewManager); }
    public void setProjectManager(IProjectManager projectManager) {this.projectManager = Objects.requireNonNull(projectManager);}
    public void setRegistry(ComponentRegistry registry) { this.registry = Objects.requireNonNull(registry); }
    public void setZoomManager(IZoomManager zoomManager) { this.zoomManager = Objects.requireNonNull(zoomManager); }
    public void setListCoordinator(IListCoordinator listCoordinator) { this.listCoordinator = Objects.requireNonNull(listCoordinator); }
    public void setView(VisorView view) { this.view = Objects.requireNonNull(view); }
    public void setActionMap(Map<String, Action> actionMap) { this.actionMap = Objects.requireNonNull(actionMap); }
    public void setModel(VisorModel model) { this.model = Objects.requireNonNull(model); }
    
    public void setDisplayModeManager(DisplayModeManager displayModeManager) {this.displayModeManager = displayModeManager;}
    
    public IProjectManager getProjectManager() {return this.projectManager;    }
    public ProjectListCoordinator getProjectListCoordinator() {return this.projectListCoordinator;}
    public VisorView getView() { return this.view; }
    public ComponentRegistry getRegistry() { return this.registry; }

    public GeneralController getGeneralController() {return this.generalController;}
    public Map<String, Action> getActionMap() {return this.actionMap;}
    
} // --- FIN de la clase ProjectController ---


