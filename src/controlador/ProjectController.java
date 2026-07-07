package controlador;

import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
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

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.commands.AppActionCommands;
import controlador.interfaces.IModoController;
import controlador.managers.DataManager;
import controlador.managers.DisplayModeManager;
import controlador.managers.ExportQueueManager;
import controlador.managers.interfaces.IProjectManager;
import controlador.managers.interfaces.IZoomManager;
import controlador.services.proyecto.ExportPreflightReport;
import controlador.services.proyecto.ExportStatusReport;
import controlador.services.proyecto.PdfWorkflowService;
import controlador.services.proyecto.ProjectExportService;
import controlador.services.proyecto.ProjectFileManagementService;
import controlador.services.proyecto.ProjectIntegrityService;
import controlador.services.proyecto.ProjectSyncService;
import controlador.ui.ProjectUIManager;
import controlador.utils.ComponentRegistry;
import controlador.utils.DesktopUtils;
import controlador.worker.ArchiveAnalysisWorker;
import controlador.worker.ExportWorker;
import modelo.ListContext;
import modelo.VisorModel;
import modelo.proyecto.ExportItem;
import modelo.proyecto.ProjectModel;
import vista.VisorView;
import vista.dialogos.PDFPreviewDialog;
import vista.dialogos.TaskProgressDialog;
import vista.panels.export.ExportDetailPanel;
import vista.panels.export.ExportPanel;
import vista.panels.export.ExportTableModel;
import vista.theme.Tema;

/**
 * Controlador principal del modo PROYECTO.
 * Orquesta la logica de seleccion/descartes, gestion de imagenes marcadas,
 * configuracion de exportacion, y coordinacion de las vistas del proyecto.
 */
public class ProjectController implements IModoController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

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
    private ProjectExportService exportService;
    private PdfWorkflowService pdfWorkflowService;
    private ProjectFileManagementService fileManagementService;
    private ProjectIntegrityService integrityService;
    private ProjectSyncService syncService;
    private ProjectUIManager uiManager;

    private Map<String, Action> actionMap;
    private Map<String, ExportItem> exportItemMap = new HashMap<>();
    private int lastRightDividerLocation = -1;

    private ProjectLayout currentLayout = ProjectLayout.DEFAULT;
    private Component storedRightComponent = null;

    // Constructor que inicializa los servicios headless de exportacion y PDF
    /**
     * Constructor que inicializa los servicios headless de exportación y PDF.
     */
    public ProjectController() {
        logger.debug("[ProjectController] Instancia creada.");
        this.exportQueueManager = new ExportQueueManager();
        this.exportService = new ProjectExportService();
        this.pdfWorkflowService = new PdfWorkflowService();
    } // --- Fin del metodo: ProjectController (constructor) ---


    /**
     * Registra los MouseListeners y ListSelectionListeners en las listas de proyecto, descartes y la tabla de exportación.
     */
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
                    logger.info("\n\n [MOUSELISTENER] HEMOS ENTRADO EN VIEW_DISCARDS \n\n");
                    setProjectViewState(ProjectViewState.VIEW_DISCARDS);
                } else { // Clic en la lista de Selección (projectList)
                    ProjectViewState targetState = isExportPanelVisible() ? ProjectViewState.VIEW_EXPORT
                            : ProjectViewState.VIEW_SELECTION;
                    logger.info("\n\n [MOUSELISTENER] HEMOS ENTRADO EN " + targetState + " \n\n");
                    setProjectViewState(targetState);
                }
            }
        };

        if (projectList != null) {
            projectList.addMouseListener(listMouseAdapter);
            projectList.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting() || projectListCoordinator.isSincronizandoUI())
                    return;
                if ("seleccion".equals(model.getProyectoListContext().getNombreListaActiva())) {
                    projectListCoordinator.sincronizarVistaConSeleccionLista(projectList);
                }
            });
        }

        if (descartesList != null) {
            descartesList.addMouseListener(listMouseAdapter);
            descartesList.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting() || projectListCoordinator.isSincronizandoUI())
                    return;
                if ("descartes".equals(model.getProyectoListContext().getNombreListaActiva())) {
                    projectListCoordinator.sincronizarVistaConSeleccionLista(descartesList);
                }
            });
        }

        // --- AÑADIR LISTENER AL ÁREA DE EXPORTACIÓN ---
        MouseAdapter exportViewMouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentViewState != ProjectViewState.VIEW_EXPORT) {
                    logger.info("\n\n [MOUSELISTENER EXPORT] HEMOS ENTRADO EN VIEW_EXPORT \n\n");
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
        // La lógica de estado ahora es manejada por los clics directos en las áreas de
        // trabajo.

    } // --- Fin del metodo: configurarListeners ---


    /**
     * Limpia por completo la interfaz de usuario del modo Proyecto.
     */
    private void limpiarVistaProyecto() {
        logger.debug("[ProjectController] Limpiando la vista del modo proyecto...");

        uiManager.limpiarListasProyecto();

        ListContext proyectoContext = model.getProyectoListContext();
        if (proyectoContext.getModeloLista() != null && !proyectoContext.getModeloLista().isEmpty()) {
            model.setMasterListAndNotify(new DefaultListModel<>(), new HashMap<>(), this);
        }

        if (projectListCoordinator != null) {
            projectListCoordinator.reiniciarYSeleccionarIndice(-1);
        }

        actualizarAparienciaListasPorFoco();

        logger.debug("[ProjectController] Vista del proyecto limpiada.");
    } // --- Fin del metodo: limpiarVistaProyecto ---


    /**
     * Limpia de forma exhaustiva todo el estado relacionado con el proyecto actual.
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
    } // --- Fin del metodo: limpiarEstadoCompletoDelProyecto ---


    /**
     * Método orquestador central que gestiona los cambios de estado del proyecto.
     *
     * @param newState Nuevo estado de la vista del proyecto
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
        
        // La restauración de la selección se delega al método que activa la vista o al coordinador
        // para evitar conflictos de sincronización durante la carga inicial.


        // --- Lógica específica para el estado de EXPORTACIÓN ---
        if (newState == ProjectViewState.VIEW_EXPORT) {
            // Esta es la llamada que faltaba. Prepara los datos para la JTable de
            // exportación.
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
    } // --- Fin del metodo: setProjectViewState ---


    /**
     * Actualiza el mapa interno que se usa para buscar rápidamente un ExportItem.
     *
     * @param items Lista de items de exportación, o null para limpiar el mapa
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
                        (existing, replacement) -> existing));
        logger.debug("Mapa de items de exportación actualizado. Total: {} items.", this.exportItemMap.size());
    } // --- Fin del metodo: actualizarMapaDeItemsExportacion ---


    /**
     * Devuelve el ExportItem correspondiente a una clave de imagen.
     *
     * @param clave Clave de la imagen
     * @return ExportItem encontrado, o null si no existe
     */
    public ExportItem getExportItem(String clave) {
        // La condición de si estamos en modo exportación ya la comprueba el llamador
        // (GridCellRenderer)
        // usando el método isExportViewActive(). Este método solo debe devolver el item
        // si existe.
        if (clave == null) {
            return null;
        }
        return exportItemMap.get(clave);
    } // --- Fin del metodo: getExportItem ---


    /**
     * Alterna la visibilidad del panel de herramientas inferior derecho (exportación).
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
            // --- ABRIR: Primero reorganizar layout a ASSIGNMENT ---
            if (currentLayout == ProjectLayout.DEFAULT) {
                toggleProjectLayout();
            }

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

            // --- Luego revertir layout a DEFAULT ---
            if (currentLayout == ProjectLayout.ASSIGNMENT) {
                toggleProjectLayout();
            }
        }
    } // --- Fin del metodo: toggleExportView ---


    /**
     * Se llama después de que las barras de herramientas del modo Proyecto se hayan inicializado.
     */
    public void postToolbarInitialization() {
        logger.debug("[ProjectController] Realizando inicialización post-toolbars...");
        ensureExportPanelIsFullyInitialized();
        configurarContextMenuTablaExportacion();
        configurarListenersMetadatos();
        logger.debug("[ProjectController] Inicialización post-toolbars completada.");

    } // --- Fin del metodo: postToolbarInitialization ---


    /**
     * Añade un DocumentListener al área de descripción para sincronizar cambios.
     */
    private void configurarListenersMetadatos() {
        vista.panels.export.ProjectMetadataPanel propsPanel = registry.get("panel.proyecto.propiedades");
        if (propsPanel == null || projectManager == null)
            return;

        javax.swing.event.DocumentListener listener = new javax.swing.event.DocumentListener() {
            private void notificar() {
                sincronizarDescripcionDesdeUI();
                projectManager.notificarModificacion();
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                notificar();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                notificar();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                notificar();
            }
        };

        propsPanel.getProjectDescriptionArea().getDocument().addDocumentListener(listener);
        logger.debug("DocumentListener añadido al área de descripción del proyecto.");
    } // --- Fin del metodo: configurarListenersMetadatos ---


    /**
     * Orquesta la sincronización de archivos asociados, notifica al ProjectManager y refresca la UI de exportación.
     */
    public void notificarCambioEnProyecto() {

        logger.info("--- PASO 3: notificarCambioEnColaExportacion en ProjectController EJECUTADO ---");

        if (projectManager != null && generalController != null) {

            // PASO A: Sincronizar el estado de la tabla (UI) con el modelo de datos
            // principal.
            sincronizarArchivosAsociadosConModelo();

            // PASO B: Notificar al ProjectManager para que compruebe si hay cambios.
            projectManager.notificarModificacion();

            // --- Forzar la actualización de toda la UI de exportación ---
            // Esto asegura que el título, el tamaño total y el estado de los botones
            // se actualicen inmediatamente después de un cambio en la tabla.
            actualizarEstadoExportacionUI();

            logger.debug("[ProjectController] Cambio detectado, sincronizado y UI de exportación actualizada.");
        }
    } // --- Fin del metodo: notificarCambioEnProyecto ---


    /**
     * Alterna entre layout DEFAULT (izquierda + derecha) y ASSIGNMENT (reordenado).
     */
    public void toggleProjectLayout() {
        if (registry == null) return;
        if (uiManager == null) return;

        if (currentLayout == ProjectLayout.DEFAULT) {
            uiManager.aplicarLayoutAsignacion();
            currentLayout = ProjectLayout.ASSIGNMENT;
        } else {
            uiManager.aplicarLayoutDefault();
            currentLayout = ProjectLayout.DEFAULT;
        }

        Action toggleAction = actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_LAYOUT);
        if (toggleAction != null) {
            toggleAction.putValue(Action.SELECTED_KEY, currentLayout == ProjectLayout.ASSIGNMENT);
        }
    }


    /**
     * Resetea el layout del panel derecho a su estado por defecto.
     */
    public void resetProjectViewLayout() {
        lastRightDividerLocation = -1;
        // Si estamos en layout ASSIGNMENT, restaurar a DEFAULT primero
        if (currentLayout == ProjectLayout.ASSIGNMENT && storedRightComponent != null) {
            JSplitPane mainSplit = registry.get("splitpane.proyecto.main");
            if (mainSplit != null) {
                mainSplit.add(storedRightComponent, JSplitPane.RIGHT);
                mainSplit.setDividerSize(5);
                mainSplit.setDividerLocation(0.25);
                mainSplit.revalidate();
                mainSplit.repaint();
            }
            storedRightComponent = null;
            currentLayout = ProjectLayout.DEFAULT;

            Action toggleAction = actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_LAYOUT);
            if (toggleAction != null) {
                toggleAction.putValue(Action.SELECTED_KEY, false);
            }
        }
        uiManager.resetLayoutProyecto(actionMap);
    } // --- Fin del metodo: resetProjectViewLayout ---


    /**
     * Carga en la masterList del VisorModel la lista de datos correcta según la lista activa.
     */
    public void actualizarModeloPrincipalConListaDeProyectoActiva() {
        if (model == null || projectManager == null) {
            logger.warn(
                    "WARN [actualizarModeloPrincipalConListaDeProyectoActiva]: Modelo o ProjectManager nulos. No se puede actualizar.");
            return;
        }

        String nombreListaActiva = model.getProyectoListContext().getNombreListaActiva();
        List<String> sourceData = syncService.getSourceData(nombreListaActiva);

        DefaultListModel<String> newMasterModel = new DefaultListModel<>();
        for (String pathStr : sourceData) {
            newMasterModel.addElement(pathStr);
        }

        model.setMasterListAndNotify(newMasterModel, model.getProyectoListContext().getRutaCompletaMap(), this);

        sincronizarSeleccionEnGridProyecto();
    } // --- Fin del metodo: actualizarModeloPrincipalConListaDeProyectoActiva ---


    /**
     * Alinea la selección de la JList del grid con el índice oficial del ProjectListCoordinator.
     */
    public void sincronizarSeleccionEnGridProyecto() {
        if (registry == null || projectListCoordinator == null)
            return;

        if (model.getCurrentDisplayMode() != VisorModel.DisplayMode.GRID) {
            return;
        }

        int indiceSeleccionado = projectListCoordinator.getOfficialSelectedIndex();
        uiManager.sincronizarSeleccionGrid(indiceSeleccionado);
    } // --- Fin del metodo: sincronizarSeleccionEnGridProyecto ---


    /**
     * Cambia el estado de la vista a VIEW_DISCARDS o VIEW_SELECTION según la lista indicada.
     *
     * @param nuevoFoco Nombre de la lista activa ("seleccion" o "descartes")
     */
    private void cambiarFocoListaActiva(String nuevoFoco) {
        if ("descartes".equals(nuevoFoco)) {
            setProjectViewState(ProjectViewState.VIEW_DISCARDS);
        } else {
            setProjectViewState(ProjectViewState.VIEW_SELECTION);
        }
    } // --- Fin del metodo: cambiarFocoListaActiva ---


    /**
     * Carga las listas de imágenes marcadas y descartadas desde el ProjectManager al contexto del modelo.
     *
     * @return true si hay datos para mostrar, false en caso contrario
     */
    public boolean prepararDatosProyecto() {
        logger.debug("  [ProjectController] Preparando datos para el modo proyecto...");
        if (projectManager == null || model == null) {
            return false;
        }

        int size = projectManager.getCurrentProject().getSelectedImages().size();
        logger.info(
                ">>>>>>>>>> [PROYECTO - INICIO] Al preparar datos, ProjectModel en memoria tiene {} imágenes seleccionadas.",
                size);

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
    } // --- Fin del metodo: prepararDatosProyecto ---


    /**
     * Activa la UI de la vista de proyecto. Se llama desde GeneralController al entrar en modo Proyecto.
     */
    public void activarVistaProyecto() {
        logger.debug("  [ProjectController] Activando la UI de la vista de proyecto...");

        limpiarCacheRenderersProyecto();

        if (registry == null || model == null || projectManager == null || projectListCoordinator == null
                || generalController == null) {
            logger.error("ERROR [ProjectController.activarVistaProyecto]: Dependencias nulas.");
            return;
        }

        // Resetear estado interno para forzar restauración completa al re-entrar
        this.currentViewState = null;

        boolean hayDatosParaMostrar = prepararDatosProyecto();

        SwingUtilities.invokeLater(() -> {
            if (hayDatosParaMostrar) {
                logger.debug("   -> Hay imágenes en el proyecto. Poblando la vista...");

                poblarListasSeleccionYDescartes();

                String focoGuardado = model.getProyectoListContext().getNombreListaActiva();
                cambiarFocoListaActiva(focoGuardado != null ? focoGuardado : "seleccion");

                // Re-seleccionar en la JList y actualizar visor usando el coordinador
                String claveRestaurada = determinarClaveASeleccionar(model.getProyectoListContext());
                if (claveRestaurada != null) {
                    // Forzamos la actualización del visor limpiando la clave actual antes de seleccionar
                    model.getProyectoListContext().setSelectedImageKey(null);
                    projectListCoordinator.seleccionarImagenPorClave(claveRestaurada);
                }

                ajustarLayoutProyectoUI();

            } else {
                logger.debug("   -> No hay imágenes en el proyecto. Limpiando la vista...");
                limpiarVistaProyecto();
            }

            resetProjectViewLayout();

            if (model.isProjectExportPanelVisible()) {
                setExportPanelVisible(true);
                solicitarPreparacionColaExportacion();
                sincronizarSeleccionEnTablaExportacion();
            }
        });

    } // --- Fin del metodo: activarVistaProyecto ---


    /**
     * Asegura que el ExportPanel tenga registrado su highlight listener de selección.
     */
    private void ensureExportPanelIsFullyInitialized() {
        ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");
        if (exportPanel != null) {
            exportPanel.setupHighlightingListener();
        }
    } // --- Fin del metodo: ensureExportPanelIsFullyInitialized ---


    /**
     * Rellena las JList de Selección y Descartes con los datos del ProjectManager.
     */
    private void poblarListasSeleccionYDescartes() {
        List<String> seleccion = projectManager.getImagenesMarcadas().stream()
                .map(p -> p.toString().replace("\\", "/"))
                .collect(Collectors.toList());
        List<String> descartes = projectManager.getImagenesDescartadas().stream()
                .map(p -> p.toString().replace("\\", "/"))
                .collect(Collectors.toList());
        uiManager.poblarListaSeleccion(seleccion);
        uiManager.poblarListaDescartes(descartes);
        actualizarContadoresDeTitulos();
    } // --- Fin del metodo: poblarListasSeleccionYDescartes ---


    /**
     * Determina qué clave de imagen debe estar seleccionada al activar la vista.
     *
     * @param proyectoContext Contexto del proyecto
     * @return Clave de la imagen a seleccionar
     */
    private String determinarClaveASeleccionar(ListContext proyectoContext) {
        String focoActual = proyectoContext.getNombreListaActiva();
        String claveParaMostrar = null;

        if ("descartes".equals(focoActual)) {
            claveParaMostrar = proyectoContext.getDescartesListKey();
        } else {
            claveParaMostrar = proyectoContext.getSeleccionListKey();
        }

        if (claveParaMostrar == null) {
            JList<String> listaActivaUI = "descartes".equals(focoActual)
                    ? registry.get("list.proyecto.descartes")
                    : registry.get("list.proyecto.nombres");

            if (listaActivaUI != null && listaActivaUI.getModel().getSize() > 0) {
                claveParaMostrar = listaActivaUI.getModel().getElementAt(0);
            } else {
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

    } // --- Fin del metodo: determinarClaveASeleccionar ---


    /**
     * Ajusta los componentes visuales de la UI del modo proyecto.
     */
    private void ajustarLayoutProyectoUI() {
        SwingUtilities.invokeLater(() -> {
            actualizarAparienciaListasPorFoco();
            logger.debug("  [ProjectController] UI de la vista de proyecto activada y apariencia actualizada.");
        });
    } // --- Fin del metodo: ajustarLayoutProyectoUI ---


    /**
     * Selecciona la siguiente imagen en la lista activa del proyecto.
     */
    @Override
    public void navegarSiguiente() {
        if (projectListCoordinator != null) {
            projectListCoordinator.seleccionarSiguiente();
        }
    } // --- Fin del metodo: navegarSiguiente ---


    /**
     * Selecciona la imagen anterior en la lista activa del proyecto.
     */
    @Override
    public void navegarAnterior() {
        if (projectListCoordinator != null)
            projectListCoordinator.seleccionarAnterior();
    } // --- Fin del metodo: navegarAnterior ---


    /**
     * Selecciona la primera imagen de la lista activa del proyecto.
     */
    @Override
    public void navegarPrimero() {
        if (projectListCoordinator != null)
            projectListCoordinator.seleccionarPrimero();
    } // --- Fin del metodo: navegarPrimero ---


    /**
     * Selecciona la última imagen de la lista activa del proyecto.
     */
    @Override
    public void navegarUltimo() {
        if (projectListCoordinator != null)
            projectListCoordinator.seleccionarUltimo();
    } // --- Fin del metodo: navegarUltimo ---


    /**
     * Avanza un bloque completo de imágenes en la lista activa.
     */
    @Override
    public void navegarBloqueSiguiente() {
        if (projectListCoordinator != null)
            projectListCoordinator.seleccionarBloqueSiguiente();
    } // --- Fin del metodo: navegarBloqueSiguiente ---


    /**
     * Retrocede un bloque completo de imágenes en la lista activa.
     */
    @Override
    public void navegarBloqueAnterior() {
        if (projectListCoordinator != null)
            projectListCoordinator.seleccionarBloqueAnterior();
    } // --- Fin del metodo: navegarBloqueAnterior ---


    /**
     * Aplica zoom incremental según el movimiento de la rueda del ratón y sincroniza los botones de zoom.
     *
     * @param e Evento de rueda del ratón
     */
    @Override
    public void aplicarZoomConRueda(java.awt.event.MouseWheelEvent e) {
        if (zoomManager != null) {
            zoomManager.aplicarZoomConRueda(e);
            if (generalController != null && generalController.getVisorController() != null) {
                zoomManager.sincronizarEstadoVisualBotonesYRadiosZoom();
            }
        }
    } // --- Fin del metodo: aplicarZoomConRueda ---


    /**
     * Desplaza la imagen visible en la dirección indicada.
     *
     * @param deltaX Desplazamiento horizontal
     * @param deltaY Desplazamiento vertical
     */
    @Override
    public void aplicarPan(int deltaX, int deltaY) {
        if (zoomManager != null) {
            zoomManager.aplicarPan(deltaX, deltaY);
        }
    } // --- Fin del metodo: aplicarPan ---


    /**
     * Inicia la operación de paneo arrastrando con el ratón si el zoom está habilitado.
     *
     * @param e Evento de ratón
     */
    @Override
    public void iniciarPaneo(java.awt.event.MouseEvent e) {
        if (zoomManager != null && model.isZoomHabilitado()) {
            zoomManager.iniciarPaneo(e);
        }
    } // --- Fin del metodo: iniciarPaneo ---


    /**
     * Fuerza un refresco completo de ambas listas del proyecto.
     */
    @Override
    public void solicitarRefresco() {
        logger.debug("[ProjectController] Solicitud de refresco recibida. Llamando a refrescarListasDeProyecto...");
        refrescarListasDeProyecto();
    } // --- Fin del metodo: solicitarRefresco ---


    /**
     * Incrementa un 20% el tamaño de las miniaturas del grid de proyecto.
     */
    @Override
    public void aumentarTamanoMiniaturas() {
        cambiarTamanoGrid(1.2);
    } // --- Fin del metodo: aumentarTamanoMiniaturas ---


    /**
     * Reduce un 20% el tamaño de las miniaturas del grid de proyecto.
     */
    @Override
    public void reducirTamanoMiniaturas() {
        cambiarTamanoGrid(0.8);
    } // --- Fin del metodo: reducirTamanoMiniaturas ---


    /**
     * Rellena la JList de Descartes con las imágenes descartadas del ProjectManager y actualiza el título del tab.
     */
    public void poblarListaDescartes() {
        if (projectManager == null) {
            logger.warn("WARN [poblarListaDescartes]: ProjectManager nulo.");
            return;
        }
        List<String> descartes = projectManager.getImagenesDescartadas().stream()
                .map(p -> p.toString().replace("\\", "/"))
                .collect(Collectors.toList());
        uiManager.poblarListaDescartes(descartes);
    } // --- Fin del metodo: poblarListaDescartes ---


    /**
     * Mueve las imágenes seleccionadas en la lista de Selección a la lista de Descartes.
     */
    public void moverSeleccionActualADescartes() {
        if (model == null || projectManager == null || registry == null) {
            return;
        }

        // --- Fase 7: guardia proyecto compartido ---
        if (projectManager.getCurrentProject() != null
                && projectManager.getCurrentProject().isSharedWithClient()) {
            int confirm = JOptionPane.showConfirmDialog(view,
                    "Estás modificando las especificaciones enviadas al cliente.\n"
                    + "¿Continuar?",
                    "Proyecto Compartido",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        JList<String> listaSeleccionUI = registry.get("list.proyecto.nombres");
        if (listaSeleccionUI == null) {
            return;
        }

        int indiceAncla = obtenerIndiceAnclaDeLista(listaSeleccionUI);
        List<Path> rutasAMover = obtenerRutasDesdeListaSeleccionada(listaSeleccionUI);
        if (rutasAMover.isEmpty()) {
            logger.debug("No hay imágenes seleccionadas para mover a descartes.");
            return;
        }

        int movidos = projectManager.moverVariosAdescartes(rutasAMover);
        if (movidos == 0) {
            return;
        }

        // Preservar selección de la tabla de exportación antes del refresco
        int exportViewRow = -1;
        JTable tablaExport = getTablaExportacionDesdeRegistro();
        if (tablaExport != null) {
            exportViewRow = tablaExport.getSelectionModel().getLeadSelectionIndex();
        }

        logger.debug("  [ProjectController] {} imagen(es) movida(s) a descartes.", movidos);
        refrescarVistaProyectoCompleta();
        reubicarSeleccionTrasOperacionEnLista(registry.get("list.proyecto.nombres"), indiceAncla, "seleccion");

        // Restaurar selección en la misma posición visual del orden actual
        final int exportViewRowFinal = exportViewRow;
        final JTable tablaExportFinal = tablaExport;
        if (tablaExportFinal != null && exportViewRowFinal >= 0) {
            SwingUtilities.invokeLater(() -> {
                int maxRow = tablaExportFinal.getRowCount() - 1;
                int rowToSelect = Math.min(exportViewRowFinal, maxRow);
                if (rowToSelect >= 0) {
                    tablaExportFinal.setRowSelectionInterval(rowToSelect, rowToSelect);
                    tablaExportFinal.scrollRectToVisible(tablaExportFinal.getCellRect(rowToSelect, 0, true));
                }
            });
        }
    } // --- Fin del metodo: moverSeleccionActualADescartes ---


    /**
     * Restaura las imágenes seleccionadas en la lista de Descartes a la lista de Selección.
     */
    public void restaurarDesdeDescartes() {
        if (registry == null || projectManager == null) {
            return;
        }

        // --- Fase 7: guardia proyecto compartido ---
        if (projectManager.getCurrentProject() != null
                && projectManager.getCurrentProject().isSharedWithClient()) {
            int confirm = JOptionPane.showConfirmDialog(view,
                    "Estás modificando las especificaciones enviadas al cliente.\n"
                    + "¿Continuar?",
                    "Proyecto Compartido",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        JList<String> listaDescartesUI = registry.get("list.proyecto.descartes");
        if (listaDescartesUI == null) {
            return;
        }

        int indiceAncla = obtenerIndiceAnclaDeLista(listaDescartesUI);
        List<Path> rutasARestaurar = obtenerRutasDesdeListaSeleccionada(listaDescartesUI);
        if (rutasARestaurar.isEmpty()) {
            logger.debug("No hay imágenes seleccionadas en descartes para restaurar.");
            return;
        }

        int restaurados = projectManager.restaurarVariosDeDescartes(rutasARestaurar);
        if (restaurados == 0) {
            return;
        }

        // Preservar selección de la tabla de exportación antes del refresco
        int exportViewRow = -1;
        JTable tablaExport = getTablaExportacionDesdeRegistro();
        if (tablaExport != null) {
            exportViewRow = tablaExport.getSelectionModel().getLeadSelectionIndex();
        }

        logger.debug("  [ProjectController] {} imagen(es) restaurada(s) desde descartes.", restaurados);
        refrescarVistaProyectoCompleta();

        // Restaurar selección en la misma posición visual del orden actual
        final int rowToPreserve = exportViewRow;
        final JTable exportTableRef = tablaExport;
        if (exportTableRef != null && rowToPreserve >= 0) {
            SwingUtilities.invokeLater(() -> {
                int maxRow = exportTableRef.getRowCount() - 1;
                int rowToSelect = Math.min(rowToPreserve, maxRow);
                if (rowToSelect >= 0) {
                    exportTableRef.setRowSelectionInterval(rowToSelect, rowToSelect);
                    exportTableRef.scrollRectToVisible(exportTableRef.getCellRect(rowToSelect, 0, true));
                }
            });
        }

        SwingUtilities.invokeLater(() -> {
            JList<String> listaActualizada = registry.get("list.proyecto.descartes");
            if (listaActualizada == null) {
                return;
            }
            int nuevoTamanio = listaActualizada.getModel().getSize();
            setProjectViewState(ProjectViewState.VIEW_DISCARDS);

            if (nuevoTamanio > 0) {
                reubicarSeleccionTrasOperacionEnLista(listaActualizada, indiceAncla, "descartes");
            } else {
                setProjectViewState(ProjectViewState.VIEW_SELECTION);
                JList<String> listaSeleccion = registry.get("list.proyecto.nombres");
                if (listaSeleccion != null && listaSeleccion.getModel().getSize() > 0) {
                    projectListCoordinator.seleccionarImagenPorIndice(0);
                } else {
                    projectListCoordinator.seleccionarImagenPorIndice(-1);
                }
            }
        });
    } // --- Fin del metodo: restaurarDesdeDescartes ---


    /**
     * Obtiene las rutas absolutas de todos los elementos seleccionados en una JList de proyecto.
     *
     * @param lista JList del proyecto
     * @return Lista de rutas seleccionadas
     */
    private List<Path> obtenerRutasDesdeListaSeleccionada(JList<String> lista) {
        if (lista == null || lista.getModel() == null) {
            return java.util.Collections.emptyList();
        }
        int[] indices = lista.getSelectedIndices();
        if (indices.length == 0) {
            return java.util.Collections.emptyList();
        }
        List<Path> rutas = new java.util.ArrayList<>();
        for (int indice : indices) {
            String clave = lista.getModel().getElementAt(indice);
            if (clave != null && !clave.isEmpty()) {
                rutas.add(java.nio.file.Paths.get(clave));
            }
        }
        return rutas;
    } // --- Fin del metodo: obtenerRutasDesdeListaSeleccionada ---


    /**
     * Obtiene el índice de referencia para re-seleccionar tras una operación por lotes.
     *
     * @param lista JList del proyecto
     * @return Índice ancla o mínimo seleccionado
     */
    private int obtenerIndiceAnclaDeLista(JList<String> lista) {
        if (lista == null) {
            return -1;
        }
        int ancla = lista.getAnchorSelectionIndex();
        if (ancla >= 0) {
            return ancla;
        }
        return lista.getMinSelectionIndex();
    } // --- Fin del metodo: obtenerIndiceAnclaDeLista ---


    /**
     * Tras refrescar una lista, selecciona un índice coherente con la operación anterior.
     *
     * @param lista Lista sobre la que actuar
     * @param indiceAncla Índice de referencia
     * @param focoLista Lista activa ("seleccion" o "descartes")
     */
    private void reubicarSeleccionTrasOperacionEnLista(JList<String> lista, int indiceAncla, String focoLista) {
        SwingUtilities.invokeLater(() -> {
            if (lista == null) {
                return;
            }
            if (focoLista != null) {
                cambiarFocoListaActiva(focoLista);
            }
            int nuevoTamanio = lista.getModel().getSize();
            if (nuevoTamanio <= 0) {
                projectListCoordinator.seleccionarImagenPorIndice(-1);
                return;
            }
            int nuevoIndice = indiceAncla;
            if (nuevoIndice < 0 || nuevoIndice >= nuevoTamanio) {
                nuevoIndice = Math.min(Math.max(indiceAncla, 0), nuevoTamanio - 1);
            }
            projectListCoordinator.seleccionarImagenPorIndice(nuevoIndice);
        });
    } // --- Fin del metodo: reubicarSeleccionTrasOperacionEnLista ---


    /**
     * Reprepara los datos del proyecto y reactiva la vista para refrescar ambas listas.
     */
    private void refrescarListasDeProyecto() {
        logger.debug("  [ProjectController] Refrescando ambas listas del proyecto...");
        prepararDatosProyecto();
        activarVistaProyecto();
    } // --- Fin del metodo: refrescarListasDeProyecto ---


    /**
     * Prepara la cola de exportación desde la selección actual y actualiza la tabla UI (sin forzar escaneo).
     */
    public void solicitarPreparacionColaExportacion() {
        solicitarPreparacionColaExportacion(false);
    } // --- Fin del metodo: solicitarPreparacionColaExportacion (sin params) ---


    /**
     * Prepara la cola de exportación desde la selección actual y actualiza la tabla UI.
     *
     * @param forzarEscaneoDisco true para forzar el refresco de búsqueda en disco
     */
    public void solicitarPreparacionColaExportacion(boolean forzarEscaneoDisco) {
        if (projectManager == null || exportQueueManager == null || registry == null) {
            logger.error("ERROR [solicitarPreparacionColaExportacion]: Dependencias nulas.");
            return;
        }
        List<Path> seleccionActual = projectManager.getImagenesMarcadas();
        Map<String, modelo.proyecto.ExportConfig> exportConfigs = projectManager.getCurrentProject().getExportConfigs();
        exportQueueManager.prepararColaDesdeSeleccion(seleccionActual, exportConfigs);

        if (forzarEscaneoDisco) {
            exportQueueManager.forzarRefrescoDeBusquedaEnDisco();
        }

        actualizarMapaDeItemsExportacion(exportQueueManager.getColaDeExportacion());

        JTable tablaUI = getTablaExportacionDesdeRegistro();
        if (tablaUI != null && tablaUI.getModel() instanceof ExportTableModel) {
            ((ExportTableModel) tablaUI.getModel()).setCola(exportQueueManager.getColaDeExportacion());
            logger.debug("[ProjectController] Modelo de tabla de exportación actualizado.");
        } else {
            logger.warn(
                    "WARN [ProjectController]: No se pudo obtener la tabla de exportación o su modelo no es ExportTableModel.");
        }

        // Cargar carpeta destino si está guardada
        vista.panels.export.ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");
        if (exportPanel != null && projectManager.getCurrentProject() != null) {
            String destinoGuardado = projectManager.getCurrentProject().getExportDestinationFolder();
            if (destinoGuardado != null) {
                exportPanel.setRutaDestino(destinoGuardado);
            }
            exportPanel.refreshPdfDetailsTable();
        }
        
        actualizarEstadoExportacionUI();
    } // --- Fin del metodo: solicitarPreparacionColaExportacion ---


    /**
     * Abre un JFileChooser para seleccionar la carpeta de destino de exportación.
     */
    public void solicitarSeleccionCarpetaDestino() {
        if (registry == null || view == null) {
            logger.error("ERROR [solicitarSeleccionCarpetaDestino]: Registry o View nulos.");
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar Carpeta de Destino para la Exportación");
        
        vista.panels.export.ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");
        if (exportPanel != null && exportPanel.getRutaDestino() != null && !exportPanel.getRutaDestino().isEmpty()) {
            java.io.File currentDir = new java.io.File(exportPanel.getRutaDestino());
            if (currentDir.exists() && currentDir.isDirectory()) {
                fileChooser.setCurrentDirectory(currentDir);
            }
        }
        
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        int resultado = fileChooser.showOpenDialog(view);

        if (resultado == JFileChooser.APPROVE_OPTION) {
            Path carpetaSeleccionada = fileChooser.getSelectedFile().toPath();
            logger.debug("  [ProjectController] Carpeta de destino seleccionada: " + carpetaSeleccionada);
            // --- INICIO DE LA MODIFICACIÓN ---
            // Usamos la clave correcta con la que se registró el panel en ProjectBuilder
            if (exportPanel != null) {
                exportPanel.setRutaDestino(carpetaSeleccionada.toString());
                if (projectManager.getCurrentProject() != null) {
                    projectManager.getCurrentProject().setExportDestinationFolder(carpetaSeleccionada.toString());
                    projectManager.notificarModificacion();
                }
            } else {
                logger.error(
                        "No se pudo encontrar el ExportPanel con la clave 'panel.proyecto.exportacion.completo' en el registro.");
            }

        } else {
            logger.debug("  [ProjectController] Selección de carpeta de destino cancelada por el usuario.");
        }
        actualizarEstadoExportacionUI();
    } // --- Fin del metodo: solicitarSeleccionCarpetaDestino ---


    /**
     * Callback invocado tras asignar manualmente un archivo a un ExportItem; refresca la UI de exportación.
     *
     * @param itemModificado Item de exportación modificado
     */
    public void onExportItemManuallyAssigned(modelo.proyecto.ExportItem itemModificado) {
        logger.debug("  [ProjectController] Archivo asignado manualmente para: "
                + itemModificado.getRutaImagen().getFileName());
        actualizarEstadoExportacionUI();
    } // --- Fin del metodo: onExportItemManuallyAssigned ---


    /**
     * Actualiza títulos, contadores, conflictos y estado de botones del panel de exportación.
     */
    public void actualizarEstadoExportacionUI() {
        if (registry == null || exportQueueManager == null || actionMap == null) {
            logger.warn(
                    "Dependencias nulas (registry, exportQueueManager, o actionMap), abortando actualización de UI de exportación.");
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

        ExportStatusReport conflictReport = exportService.detectarConflictosEnCola(colaCompleta, itemsSeleccionadosParaExportar);

        boolean hayConflictos = conflictReport.hayConflictos();
        long totalItems = colaCompleta.size();
        long seleccionados = itemsSeleccionadosParaExportar.size();
        String rutaDestino = exportPanel.getRutaDestino();

        exportPanel.actualizarTituloExportacion((int) seleccionados, (int) totalItems);
        exportPanel.actualizarTamañoTotalExportacion();

        boolean carpetaOk = rutaDestino != null && !rutaDestino.isBlank()
                && !rutaDestino.equalsIgnoreCase("Seleccione una carpeta de destino...");

        boolean todosLosSeleccionadosEstanListos = itemsSeleccionadosParaExportar.stream()
                .allMatch(item -> item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.ENCONTRADO_OK ||
                        item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.ASIGNADO_MANUAL ||
                        item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO);

        boolean enModoCliente = model != null && model.getCurrentWorkMode() == modelo.VisorModel.WorkMode.CLIENTE;

        boolean puedeExportar = !enModoCliente && carpetaOk && todosLosSeleccionadosEstanListos && seleccionados > 0 && !hayConflictos;

        boolean resaltarDestino = seleccionados > 0 && !carpetaOk;
        exportPanel.resaltarRutaDestino(resaltarDestino);

        String mensajeResumen;
        if (enModoCliente) {
            mensajeResumen = "En modo cliente no se puede exportar. Cierra el modo cliente para exportar.";
        } else if (hayConflictos) {
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
        actualizarTooltipAccion(AppActionCommands.CMD_EXPORT_SELECCIONAR_CARPETA,
                "Seleccionar la carpeta donde se exportarán los archivos");
        actualizarTooltipAccion(AppActionCommands.CMD_EXPORT_DETALLES_SELECCION,
                "Mostrar/Ocultar el panel de detalles de archivos asociados");
        actualizarTooltipAccion(AppActionCommands.CMD_EXPORT_ASIGNAR_ARCHIVO,
                "Asignar manually un archivo comprimido a la imagen seleccionada");
        actualizarTooltipAccion(AppActionCommands.CMD_EXPORT_IGNORAR_COMPRIMIDO,
                "Marcar la imagen seleccionada para exportar sin archivo comprimido");
        actualizarTooltipAccion(AppActionCommands.CMD_EXPORT_RELOCALIZAR_IMAGEN,
                "Buscar una nueva ubicación para la imagen seleccionada (si no se encuentra)");
        actualizarTooltipAccion(AppActionCommands.CMD_EXPORT_QUITAR_DE_COLA,
                "Mover la imagen seleccionada a la lista de Descartes (no se exportará)");
        actualizarTooltipAccion(AppActionCommands.CMD_EXPORT_REFRESH,
                "Vuelve a escanear el disco para actualizar el estado de los archivos");

        boolean puedeExportarPDF = !enModoCliente && todosLosSeleccionadosEstanListos && seleccionados > 0 && !hayConflictos;
        String mensajePDF;
        if (enModoCliente) {
            mensajePDF = "En modo cliente no se puede exportar. Cierra el modo cliente para exportar.";
        } else if (hayConflictos) {
            mensajePDF = "No se puede generar PDF: hay conflictos de nombres.";
        } else if (!todosLosSeleccionadosEstanListos && seleccionados > 0) {
            mensajePDF = "No se puede generar PDF: hay imágenes con errores.";
        } else if (seleccionados == 0 && totalItems > 0) {
            mensajePDF = "Para generar PDF, seleccione al menos una imagen.";
        } else if (seleccionados == 0) {
            mensajePDF = "";
        } else {
            mensajePDF = "PDF listo para generar.";
        }
        exportPanel.actualizarEstadoControles(puedeExportar, mensajeResumen, puedeExportarPDF, mensajePDF);

        // Control de botones de compartir/exportar seg&uacute;n modo
        Action accionCompartir = actionMap.get(AppActionCommands.CMD_PROYECTO_COMPARTIR_CLIENTE);
        Action accionExportHtml = actionMap.get(AppActionCommands.CMD_CLIENTE_EXPORTAR_HTML);
        Action accionExportWeb = actionMap.get(AppActionCommands.CMD_CLIENTE_EXPORTAR_WEB);
        if (enModoCliente) {
            // En modo cliente: exportar web/HTML siempre activo; compartir no aplica
            if (accionCompartir != null) accionCompartir.setEnabled(false);
            if (accionExportHtml != null) accionExportHtml.setEnabled(true);
            if (accionExportWeb != null) accionExportWeb.setEnabled(true);
        } else {
            boolean puedeCompartirCliente = todosLosSeleccionadosEstanListos && seleccionados > 0 && !hayConflictos;
            if (accionCompartir != null) accionCompartir.setEnabled(puedeCompartirCliente);
            // Export buttons only if project is shared with client
            ProjectModel proyecto = projectManager != null ? projectManager.getCurrentProject() : null;
            boolean proyectoCompartido = proyecto != null && proyecto.isSharedWithClient();
            boolean puedeExportarCliente = puedeCompartirCliente && proyectoCompartido;
            if (accionExportHtml != null) accionExportHtml.setEnabled(puedeExportarCliente);
            if (accionExportWeb != null) accionExportWeb.setEnabled(puedeExportarCliente);
        }

        // Control del bot&oacute;n Modo Cliente: deshabilitar si el proyecto compartido tiene im&aacute;genes sin c&oacute;digo
        Action accionModoCliente = actionMap.get(AppActionCommands.CMD_MODO_CLIENTE);
        if (accionModoCliente != null) {
            if (enModoCliente) {
                accionModoCliente.setEnabled(true);
            } else {
                ProjectModel proyecto = projectManager != null ? projectManager.getCurrentProject() : null;
                boolean bloquearCliente = proyecto != null && proyecto.isSharedWithClient()
                        && proyecto.hasAnyImageWithoutCode();
                accionModoCliente.setEnabled(!bloquearCliente);
            }
        }

        JTable tablaUI = getTablaExportacionDesdeRegistro();
        if (tablaUI != null) {
            tablaUI.repaint();
        }

        logger.debug("Estado de exportación UI actualizado. Puede exportar: {}. Mensaje: '{}'", puedeExportar,
                mensajeResumen);

        if (projectManager != null) {
            projectManager.notificarModificacion();
        }

    } // --- Fin del metodo: actualizarEstadoExportacionUI ---


    /**
     * Actualiza el tooltip (SHORT_DESCRIPTION) de una acción.
     *
     * @param commandKey Clave del comando
     * @param tooltipText Texto del tooltip
     */
    private void actualizarTooltipAccion(String commandKey, String tooltipText) {
        if (actionMap != null) {
            Action action = actionMap.get(commandKey);
            if (action != null) {
                action.putValue(Action.SHORT_DESCRIPTION, tooltipText);
            }
        }
    } // --- Fin del metodo: actualizarTooltipAccion ---


    /**
     * Orquesta la adición de uno o más archivos asociados a un ExportItem.
     */
    public void solicitarAnadirArchivoAsociado() {
        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) {
            JOptionPane.showMessageDialog(
                    getView(),
                    "Por favor, seleccione una imagen de la tabla de exportación primero.",
                    "Acción no disponible",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        ExportTableModel tableModel = (ExportTableModel) tablaExportacion.getModel();
        int modelRow = tablaExportacion.convertRowIndexToModel(tablaExportacion.getSelectedRow());
        ExportItem selectedItem = tableModel.getItemAt(modelRow);
        if (selectedItem == null)
            return;

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
            for (File file : fileChooser.getSelectedFiles()) {
                fileManagementService.addAssociatedFile(selectedItem, file.toPath());
            }

            tableModel.fireTableRowsUpdated(modelRow, modelRow);
            actualizarEstadoExportacionUI();
            notificarCambioEnProyecto();

            ExportPanel exportPanel = getRegistry().get("panel.proyecto.exportacion.completo");
            if (exportPanel != null && exportPanel.getDetailPanel() != null) {
                exportPanel.getDetailPanel().updateDetails(selectedItem);
            }
        }
    } // --- Fin del metodo: solicitarAnadirArchivoAsociado ---


    /**
     * Orquesta la eliminación de un archivo asociado de un ExportItem.
     */
    public void solicitarQuitarArchivoAsociado() {
        ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");
        if (exportPanel == null)
            return;

        ExportDetailPanel detailPanel = exportPanel.getDetailPanel();
        if (detailPanel == null)
            return;

        Path archivoSeleccionado = detailPanel.getArchivoAsociadoSeleccionado();
        if (archivoSeleccionado == null) {
            JOptionPane.showMessageDialog(
                    view,
                    "Por favor, seleccione un archivo de la lista de 'Detalles' para quitarlo.",
                    "Ningún archivo seleccionado",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1)
            return;

        ExportTableModel model = (ExportTableModel) tablaExportacion.getModel();
        int modelRow = tablaExportacion.convertRowIndexToModel(tablaExportacion.getSelectedRow());
        ExportItem selectedItem = model.getItemAt(modelRow);

        if (selectedItem != null) {
            fileManagementService.removeAssociatedFile(selectedItem, archivoSeleccionado);

            if (selectedItem.getRutasArchivosAsociados().isEmpty()) {
                notificarCambioEnProyecto();
            }

            model.fireTableRowsUpdated(modelRow, modelRow);
            detailPanel.updateDetails(selectedItem);
            actualizarEstadoExportacionUI();
        }
    } // --- Fin del metodo: solicitarQuitarArchivoAsociado ---


    // Orquesta la localización (abrir explorador) de un archivo asociado
    public void solicitarLocalizarArchivoAsociado() {
        ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");
        if (exportPanel == null)
            return;

        ExportDetailPanel detailPanel = exportPanel.getDetailPanel();
        if (detailPanel == null)
            return;

        Path archivoSeleccionado = detailPanel.getArchivoAsociadoSeleccionado();
        if (archivoSeleccionado == null) {
            JOptionPane.showMessageDialog(
                    view,
                    "Por favor, seleccione un archivo de la lista de 'Detalles' para localizarlo.",
                    "Ningún archivo seleccionado",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            // Usamos openAndSelectFile, que intentará seleccionar el archivo si el SO lo
            // soporta
            DesktopUtils.openAndSelectFile(archivoSeleccionado);
        } catch (Exception e) {
            logger.error("Error al intentar abrir y seleccionar el archivo asociado: {}", e.getMessage());
            JOptionPane.showMessageDialog(view, "No se pudo abrir la ubicación del archivo.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    } // --- Fin del metodo: solicitarLocalizarArchivoAsociado ---


    /**
     * Abre un selector de archivos para añadir una o varias imágenes al proyecto.
     */
    public void solicitarAnadirArchivosAlProyecto() {
        if (view == null || projectManager == null) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Añadir archivos al proyecto");
        fileChooser.setMultiSelectionEnabled(true);

        javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter(
                "Todos los archivos soportados", "jpg", "jpeg", "png", "gif", "bmp", "stl", "obj", "3mf", "zip", "rar", "7z");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(view);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            if (selectedFiles != null && selectedFiles.length > 0) {
                // --- Fase 7: guardia proyecto compartido ---
                if (projectManager.getCurrentProject() != null
                        && projectManager.getCurrentProject().isSharedWithClient()) {
                    int confirm = JOptionPane.showConfirmDialog(view,
                            "El proyecto está compartido con el cliente.\n"
                            + "Las nuevas imágenes se añadirán a la selección del proyecto y del cliente.\n"
                            + "¿Continuar?",
                            "Proyecto Compartido",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (confirm != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                logger.info("[ProjectController] Añadiendo {} archivos al proyecto...", selectedFiles.length);

                List<Path> rutas = new ArrayList<>();
                for (File file : selectedFiles) {
                    rutas.add(file.toPath());
                }
                fileManagementService.addFilesToProject(rutas);

                prepararDatosProyecto();
                refrescarVistaProyectoCompleta();

                if (selectedFiles.length == 1) {
                    String clave = selectedFiles[0].toPath().toString().replace("\\", "/");
                    if (projectListCoordinator != null) {
                        projectListCoordinator.seleccionarImagenPorClave(clave);
                    }
                }
            }
        }
    } // --- Fin del metodo: solicitarAnadirArchivosAlProyecto ---


    /**
     * Sincroniza la selección de la JTable de exportación con la imagen activa del visor.
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

        SwingUtilities.invokeLater(() -> {
            int modelRow = tableModel.findRowIndexByPath(claveSeleccionada);
            if (modelRow != -1) {
                int viewRow = tablaExportacion.convertRowIndexToView(modelRow);
                if (viewRow != -1 && viewRow < tablaExportacion.getRowCount()) {
                    if (tablaExportacion.getSelectedRow() != viewRow) {
                        tablaExportacion.setRowSelectionInterval(viewRow, viewRow);
                        tablaExportacion.scrollRectToVisible(tablaExportacion.getCellRect(viewRow, 0, true));
                    }
                }
            } else {
                tablaExportacion.clearSelection();
            }
            // Sincronizar también la tabla de detalles del PDF
            ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");
            if (exportPanel != null) {
                exportPanel.getPdfDetailsTablePanel().selectRowByPath(claveSeleccionada);
            }
        });
    } // --- Fin del metodo: sincronizarSeleccionEnTablaExportacion ---


    /**
     * Valida la cola de exportación, resuelve conflictos y lanza el ExportWorker en background.
     */
    public void solicitarInicioExportacion() {
        if (exportQueueManager == null || registry == null || view == null) {
            logger.error("ERROR [solicitarInicioExportacion]: Dependencias nulas.");
            return;
        }

        ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");
        if (exportPanel == null) {
            logger.error("CRITICAL: No se pudo encontrar el ExportPanel al iniciar la exportación.");
            JOptionPane.showMessageDialog(view, "Error interno: No se pudo encontrar el panel de exportación.",
                    "Error Crítico", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Path carpetaDestino = java.nio.file.Paths.get(exportPanel.getRutaDestino());

        List<ExportItem> colaParaCopiar = exportQueueManager.getColaDeExportacion().stream()
                .filter(ExportItem::isSeleccionadoParaExportar)
                .collect(Collectors.toList());

        ExportPreflightReport report = exportService.validarPreflight(colaParaCopiar, carpetaDestino);

        if (!report.isDestinoValido())
        {
            JOptionPane.showMessageDialog(view, report.getMensajeError(),
                    "Error de Exportación", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean soloModificados = false;
        boolean limpiarDestino = false;

        if (report.hayConflictos())
        {
            Object[] options = {"Sincronizar", "Sobrescribir", "Limpiar Carpeta", "Cancelar"};
            int choice = JOptionPane.showOptionDialog(
                    view,
                    "La carpeta de destino ya contiene " + report.getArchivosEnConflicto().size()
                            + " de los archivos.\n"
                            + "¿Cómo deseas proceder?\n\n"
                            + "- Sincronizar: Solo añade lo que falta. Respeta lo que hay.\n"
                            + "- Sobrescribir: Reemplaza archivos, pero deja el resto.\n"
                            + "- Limpiar: Vacía la carpeta antes de exportar.",
                    "Conflicto de archivos en mesa de trabajo",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null, options, options[0]);

            if (choice == 0)
            {
                soloModificados = true;
                logger.info("[ProjectController] Exportación: Sincronizar.");
            }
            else if (choice == 1)
            {
                logger.info("[ProjectController] Exportación: Sobrescribir.");
            }
            else if (choice == 2)
            {
                limpiarDestino = true;
                logger.info("[ProjectController] Exportación: Limpieza total previa.");
            }
            else
            {
                logger.info("[ProjectController] Exportación cancelada por el usuario.");
                return;
            }
        }

        boolean isMoveOperation = exportPanel.isMoveOperationActive();

        if (isMoveOperation)
        {
            javax.swing.ImageIcon warnIcon = generalController.getVisorController().getIconUtils()
                    .getScaledCommonIcon("status-warning.png", 48, 48);
            int confirm = JOptionPane.showConfirmDialog(
                    view,
                    "¡ATENCIÓN!\n\nEl botón de 'Mover' está activado.\n"
                            + "Los archivos se MOVERÁN a la carpeta destino y se ELIMINARÁN de la biblioteca original.\n\n"
                            + "¿Estás seguro de que deseas continuar con el movimiento?",
                    "Confirmar Movimiento de Archivos",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    warnIcon);

            if (confirm != JOptionPane.YES_OPTION)
            {
                logger.info("[ProjectController] Exportación (Mover) cancelada por el usuario.");
                return;
            }
        }

        TaskProgressDialog dialogo = new TaskProgressDialog(
                view,
                "Progreso de Exportación",
                isMoveOperation ? "Moviendo archivos del proyecto..." : "Copiando archivos del proyecto...");
        ExportWorker worker = new ExportWorker(colaParaCopiar, carpetaDestino, dialogo,
                soloModificados, limpiarDestino, isMoveOperation, exportPanel);

        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName()))
            {
                dialogo.setProgress((Integer) evt.getNewValue());
            }
        });
        worker.execute();
        dialogo.setVisible(true);
    } // --- Fin del metodo: solicitarInicioExportacion ---


    // Orquesta el flujo completo de generacion de PDF: asignacion de codigos, preflight, vista previa y guardado
    public void generarCatalogoPDF() {
        ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");
        if (exportPanel == null) return;

        // Forzar confirmación de cualquier edición pendiente en ambas tablas
        exportPanel.getPdfDetailsTablePanel().stopEditing();
        exportPanel.stopExportTableEditing();

        ExportTableModel modelTable = (ExportTableModel) exportPanel.getTablaExportacion().getModel();
        List<ExportItem> seleccionados = modelTable.getCola().stream()
                .filter(ExportItem::isSeleccionadoParaExportar)
                .collect(Collectors.toList());

        if (seleccionados.isEmpty()) {
            JOptionPane.showMessageDialog(view, "Selecciona elementos en la tabla.");
            return;
        }

        // --- PASO 1: ANALIZAR (Aquí se llena la BD y los ExportItem) ---
        asegurarAnalisisTecnico(seleccionados, () -> {
            
            // --- PASO 2: PRECARGAR VISTA PREVIA (Con barra de progreso) ---
            asegurarPrevisualizacionPDF(seleccionados, thumbnailCache -> {
                
                // --- PASO 3: VISTA PREVIA (Ahora con thumbnails en caché) ---
                pdfWorkflowService.asignarCodigosCatalogo(seleccionados);
                PDFPreviewDialog preview = new PDFPreviewDialog(view, seleccionados, thumbnailCache);
                preview.setVisible(true);
                
                if (!preview.isConfirmed()) return;

                // --- PASO 4: GUARDAR ARCHIVO ---
                ejecutarGuardadoFinalPDF(seleccionados);
                
            });
        });
    } // --- Fin del metodo: generarCatalogoPDF ---


    /**
     * Asegura que los archivos estén analizados antes de mostrar cualquier UI de PDF.
     *
     * @param seleccionados Lista de items seleccionados
     * @param onSuccess Acción a ejecutar tras el análisis
     */
    public void asegurarAnalisisTecnico(List<ExportItem> seleccionados, Runnable onSuccess) {
        long totalPendingBytes = 0;
        servicios.db.ArchiveMetadataDAO dao = new servicios.db.ArchiveMetadataDAO();
        
        for (ExportItem item : seleccionados) {
            if (item.getRutasArchivosAsociados() != null) {
                for (java.nio.file.Path p : item.getRutasArchivosAsociados()) {
                    try {
                        if (dao.buscarPorRuta(p.toString()) == null) {
                            totalPendingBytes += java.nio.file.Files.size(p);
                        }
                    } catch (Exception e) {
                        logger.warn("Error calculando tamaño para análisis técnico: " + p, e);
                    }
                }
            }
        }
        
        long thresholdBytes = 500L * 1024 * 1024; // 500 MB
        if (totalPendingBytes > thresholdBytes) {
            double sizeInGb = totalPendingBytes / (1024.0 * 1024.0 * 1024.0);
            String msg = String.format(
                "Hay archivos comprimidos nuevos que ocupan %.2f GB en total.\n" +
                "El análisis profundo (para contar piezas, comprobar soportes, etc.) podría tardar varios minutos.\n\n" +
                "¿Deseas omitir el análisis profundo para ir más rápido?\n" +
                "(Se usarán los datos ya conocidos y podrás generar el PDF inmediatamente)",
                sizeInGb);
                
            int resp = javax.swing.JOptionPane.showConfirmDialog(view, msg, "Análisis de gran tamaño", javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE);
            if (resp == javax.swing.JOptionPane.YES_OPTION) {
                // Saltar el análisis
                if (onSuccess != null) {
                    onSuccess.run();
                }
                return;
            }
        }

        TaskProgressDialog analysisDialog = new TaskProgressDialog(view, "Análisis de Archivos", "Verificando contenido técnico...");
        
        ArchiveAnalysisWorker worker = new ArchiveAnalysisWorker(seleccionados, analysisDialog, onSuccess);
        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                analysisDialog.setProgress((Integer) evt.getNewValue());
            }
        });
        worker.execute();
        analysisDialog.setVisible(true);
    } // --- Fin del metodo: asegurarAnalisisTecnico ---


    /**
     * Pre-carga los thumbnails en segundo plano para que el diálogo de previsualización no congele la UI.
     *
     * @param items Lista de items a previsualizar
     * @param onDone Callback con el mapa de thumbnails generados
     */
    private void asegurarPrevisualizacionPDF(List<ExportItem> items, java.util.function.Consumer<Map<String, ImageIcon>> onDone) {
        TaskProgressDialog dialog = new TaskProgressDialog(view, "Preparando vista previa", "Generando miniaturas...");
        
        Map<String, ImageIcon> thumbnailCache = new HashMap<>();
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                int total = items.size();
                for (int i = 0; i < total; i++) {
                    if (isCancelled()) return null;
                    ExportItem item = items.get(i);
                    try {
                        BufferedImage bi = ImageIO.read(item.getRutaImagen().toFile());
                        if (bi != null) {
                            float scale = Math.min(140f / bi.getWidth(), 105f / bi.getHeight());
                            if (scale > 1f) scale = 1f;
                            int w = (int) (bi.getWidth() * scale);
                            int h = (int) (bi.getHeight() * scale);
                            Image scaled = bi.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                            thumbnailCache.put(item.getRutaImagen().toString(), new ImageIcon(scaled));
                        }
                    } catch (Exception e) {
                        logger.warn("No se pudo cargar thumbnail para: {}", item.getRutaImagen());
                    }
                    setProgress((i + 1) * 100 / total);
                }
                return null;
            }
            
            @Override
            protected void done() {
                dialog.closeDialog();
                if (!isCancelled() && onDone != null) onDone.accept(thumbnailCache);
            }
        };
        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                dialog.setProgress((Integer) evt.getNewValue());
            }
        });
        worker.execute();
        dialog.setVisible(true);
    } // --- Fin del metodo: asegurarPrevisualizacionPDF ---
    

    /**
     * Realiza el paso final: elegir destino y generar físicamente el PDF.
     *
     * @param seleccionados Lista de items seleccionados
     */
    private void ejecutarGuardadoFinalPDF(List<ExportItem> seleccionados) {
        JFileChooser chooser = new JFileChooser();
        javax.swing.filechooser.FileNameExtensionFilter pdfFilter =
                new javax.swing.filechooser.FileNameExtensionFilter("Archivos PDF (*.pdf)", "pdf");
        chooser.setFileFilter(pdfFilter);
        chooser.setAcceptAllFileFilterUsed(false);

        // Si el proyecto ya tiene una carpeta de exportación, la usamos como inicio
        if (projectManager.getCurrentProject() != null && projectManager.getCurrentProject().getExportDestinationFolder() != null) {
            chooser.setCurrentDirectory(new File(projectManager.getCurrentProject().getExportDestinationFolder()));
        }

        if (chooser.showSaveDialog(view) == JFileChooser.APPROVE_OPTION) {
            File destinoFinal = chooser.getSelectedFile();
            
            // Forzar extensión .pdf
            if (!destinoFinal.getName().toLowerCase().endsWith(".pdf")) {
                destinoFinal = new File(destinoFinal.getAbsolutePath() + ".pdf");
            }
            
            // Confirmar sobrescritura
            if (destinoFinal.exists()) {
                int resp = JOptionPane.showConfirmDialog(view,
                        "El archivo ya existe. ¿Deseas sobrescribirlo?",
                        "Confirmar sobrescritura",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (resp != JOptionPane.YES_OPTION) return;
            }

            final File archivoAGuardar = destinoFinal;

            // --- BARRA DE PROGRESO PARA LA ESCRITURA DEL DISCO ---
            TaskProgressDialog saveDialog = new TaskProgressDialog(view, "Generando PDF", "Escribiendo archivo...");
            saveDialog.setProgress(50); // Simulación de progreso intermedio

            SwingWorker<Void, Void> saveWorker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    // 1. Sincronizar datos de la UI al modelo antes de guardar
                    sincronizarDescripcionDesdeUI();
                    
                    String notasProyecto = (projectManager != null && projectManager.getCurrentProject() != null)
                            ? projectManager.getCurrentProject().getProjectDescription()
                            : null;

                    logger.info("[ProjectController] Guardando PDF físicamente en: {}", archivoAGuardar.getAbsolutePath());
                    
                    // 2. Generar el PDF real
                    pdfWorkflowService.generarPDF(seleccionados, archivoAGuardar.toPath(), notasProyecto);
                    
                    // 3. Sincronizar códigos y datos del catálogo con el modelo del proyecto
                    ProjectModel modeloActual = projectManager != null ? projectManager.getCurrentProject() : null;
                    if (pdfWorkflowService.sincronizarDatosCatalogoConModelo(seleccionados, modeloActual)) {
                        projectManager.notificarModificacion();
                    }
                    return null;
                }

                @Override
                protected void done() {
                    saveDialog.closeDialog();
                    if (isCancelled()) return;
                    try {
                        get();
                        JOptionPane.showMessageDialog(view, "PDF Creado con éxito.", "Finalizado", JOptionPane.INFORMATION_MESSAGE);
                    } catch (java.util.concurrent.ExecutionException e) {
                        logger.error("Error al generar el PDF", e.getCause());
                        JOptionPane.showMessageDialog(view, "Error crítico al generar el PDF:\n" + e.getCause().getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            };

            // Vincular worker al diálogo para el botón cancelar si fuera necesario
            saveDialog.setWorkerAsociado(saveWorker);
            saveWorker.execute();
            saveDialog.setVisible(true);
        }
    } // --- Fin del metodo: ejecutarGuardadoFinalPDF ---
    

    /**
     * Ejecuta el análisis técnico de los archivos asociados de la selección.
     *
     * @param seleccionados Lista de items seleccionados
     */
    public void ejecutarAnalisisTecnicoDeSeleccion(List<ExportItem> seleccionados) {
        // Método stub para análisis técnico futuro
    } // --- Fin del metodo: ejecutarAnalisisTecnicoDeSeleccion ---
    
    
    /**
     * Abre el explorador de archivos y selecciona la imagen activa de la tabla de exportación.
     */
    public void solicitarAbrirUbicacionImagen() {
        if (exportQueueManager == null || registry == null)
            return;
        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1)
            return;
        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
        int modelRow = tablaExportacion.convertRowIndexToModel(tablaExportacion.getSelectedRow());
        modelo.proyecto.ExportItem itemSeleccionado = modelTabla.getItemAt(modelRow);
        if (itemSeleccionado != null) {
            try {
                DesktopUtils.openAndSelectFile(itemSeleccionado.getRutaImagen());
            } catch (Exception e) {
                logger.error("Error al intentar abrir y seleccionar el archivo: " + e.getMessage());
                JOptionPane.showMessageDialog(view, "No se pudo abrir la ubicación del archivo.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    } // --- Fin del metodo: solicitarAbrirUbicacionImagen ---


    /**
     * Alterna el estado de un item de exportación entre NO_ENCONTRADO e IGNORAR_COMPRIMIDO.
     */
    public void solicitarAlternarIgnorarComprimido() {
        if (registry == null)
            return;
        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1)
            return;
        int modelRow = tablaExportacion.convertRowIndexToModel(tablaExportacion.getSelectedRow());
        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
        modelo.proyecto.ExportItem item = modelTabla.getItemAt(modelRow);
        if (item != null) {
            if (item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.NO_ENCONTRADO) {
                item.setEstadoArchivoComprimido(modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO);
            } else if (item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO) {
                item.setEstadoArchivoComprimido(modelo.proyecto.ExportStatus.NO_ENCONTRADO);
            }
            modelTabla.fireTableRowsUpdated(modelRow, modelRow);

            notificarCambioEnProyecto();

            actualizarEstadoExportacionUI();
        }
    } // --- Fin del metodo: solicitarAlternarIgnorarComprimido ---


    /**
     * Delega en solicitarAnadirArchivoAsociado para asignar un archivo manualmente a la cola.
     */
    public void solicitarAsignacionManual() {
        // Esta acción ahora es idéntica a "Añadir Archivo Asociado".
        // Simplemente delegamos la llamada al método orquestador.
        solicitarAnadirArchivoAsociado();
    } // --- Fin del metodo: solicitarAsignacionManual ---


    /**
     * Elimina el item seleccionado de la cola de exportación y refresca la tabla.
     */
    public void solicitarQuitarDeLaCola() {
        if (exportQueueManager == null || registry == null)
            return;
        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1)
            return;
        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
        int modelRow = tablaExportacion.convertRowIndexToModel(tablaExportacion.getSelectedRow());
        modelo.proyecto.ExportItem itemSeleccionado = modelTabla.getItemAt(modelRow);
        if (itemSeleccionado != null) {
            exportQueueManager.getColaDeExportacion().remove(itemSeleccionado);
            modelTabla.setCola(exportQueueManager.getColaDeExportacion());
            actualizarEstadoExportacionUI();
        }
    } // --- Fin del metodo: solicitarQuitarDeLaCola ---


    /**
     * Mueve a descartes si está en Selección, o restaura si está en Descartes.
     */
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
            logger.warn("WARN [solicitudAlternarMarcaImagen]: Lista activa desconocida ('" + listaActiva
                    + "'). No se realiza ninguna acción.");
        }

    } // --- Fin del metodo: solicitudAlternarMarcaImagen ---


    // ********************************************************************************************
    // *********************************************** MÉTODOS PARA GESTIÓN DE
    // ARCHIVOS DE PROYECTO
    // ********************************************************************************************

    /**
     * Orquesta la creación de un nuevo proyecto.
     */
    public void solicitarNuevoProyecto() {
        if (projectManager == null || generalController == null) {
            logger.error("ERROR [solicitarNuevoProyecto]: Dependencias nulas.");
            return;
        }

        projectManager.nuevoProyecto();

        logger.info("Nuevo proyecto creado en el backend (ProjectManager).");

        currentLayout = ProjectLayout.DEFAULT;
        if (model != null) {
            model.setProjectExportPanelVisible(false);
        }

        limpiarCacheRenderersProyecto();
        limpiarVistaProyecto();

        JTable tabla = getTablaExportacionDesdeRegistro();
        if (tabla != null && tabla.getModel() instanceof ExportTableModel) {
            ((ExportTableModel) tabla.getModel()).clear();
            logger.debug("[ProjectController] Tabla de exportación limpiada para nuevo proyecto.");
        }

        logger.info("Yendo al modo Proyecto después de crear nuevo proyecto...");
        generalController.cambiarModoDeTrabajo(VisorModel.WorkMode.PROYECTO);

        generalController.actualizarTituloVentana();
    } // --- Fin del metodo: solicitarNuevoProyecto ---


    /**
     * Limpia el estado anterior, carga un proyecto desde archivo y refresca la vista.
     *
     * @param rutaArchivo Ruta del archivo de proyecto
     */
    public void solicitarAbrirProyecto(Path rutaArchivo) {
        if (projectManager == null || generalController == null || model == null) {
            logger.error("ERROR [solicitarAbrirProyecto]: Dependencias nulas.");
            return;
        }

        // Recordar en qué modo estábamos antes de abrir (influirá en la UX del diálogo).
        boolean estabaEnCliente = (model.getCurrentWorkMode() == VisorModel.WorkMode.CLIENTE);

        try {
            // --- INICIO DE REFUERZO DE ROBUSTEZ ---
            // PASO 1: Limpiar COMPLETAMENTE el estado anterior ANTES de cargar el nuevo.
            logger.debug("Limpiando estado del proyecto anterior antes de abrir uno nuevo...");
            limpiarEstadoCompletoDelProyecto();
            // --- FIN DE REFUERZO DE ROBUSTEZ ---

            // PASO 2: Cargar los datos del nuevo proyecto en el backend.
            projectManager.abrirProyecto(rutaArchivo);
            projectManager.markProjectAsSaved();

            // PASO 3: Actualizar el título de la ventana.
            generalController.actualizarTituloVentana();

            // PASO 4: Refrescar vista según modo.
            if (estabaEnCliente) {
                // Proyecto NO compartido (no lanzó redirect)
                logger.info("Proyecto sin compartir abierto desde modo cliente. Informando y pasando a proyecto.");
                JOptionPane.showMessageDialog(
                        (view != null) ? view : null,
                        "El proyecto abierto no está compartido con cliente.\n"
                        + "Cambiando a modo proyecto.",
                        "Proyecto sin compartir",
                        JOptionPane.INFORMATION_MESSAGE);
                generalController.cambiarModoDeTrabajo(VisorModel.WorkMode.PROYECTO);
            } else if (model.isEnModoProyecto()) {
                logger.info("Ya se está en modo proyecto. Refrescando la vista con el nuevo proyecto cargado...");
                activarVistaProyecto();
            }
            projectManager.markProjectAsSaved();
            generalController.actualizarTituloVentana();

        } catch (java.io.IOException e) {
            String msg = e.getMessage();
            if (servicios.ProjectManager.REDIRECT_TO_CLIENTE.equals(msg)) {
                // Proyecto compartido
                logger.info("Proyecto compartido detectado. Modo actual: {}", model.getCurrentWorkMode());
                projectManager.markProjectAsSaved();
                if (estabaEnCliente) {
                    // Ya estamos en cliente, solo refrescar (sin preguntar)
                    logger.info("Ya en modo cliente. Refrescando vista cliente con el proyecto compartido.");
                    if (generalController.getClientController() != null) {
                        generalController.getClientController().activarVistaCliente();
                    }
                } else {
                    // Preguntar si quiere ir a modo cliente
                    int option = JOptionPane.showConfirmDialog(
                            (view != null) ? view : null,
                            "Este proyecto tiene una sesión de cliente activa.\n¿Entrar en modo cliente?",
                            "Sesión de Cliente Detectada",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    if (option == JOptionPane.YES_OPTION) {
                        generalController.cambiarModoDeTrabajo(VisorModel.WorkMode.CLIENTE);
                    } else if (model.isEnModoProyecto()) {
                        activarVistaProyecto();
                    }
                }
                return;
            }
            logger.error("Falló la carga del proyecto: {}", msg);
            Component parentWindow = (view != null) ? view : null;
            JOptionPane.showMessageDialog(
                    parentWindow,
                    "No se pudo abrir el archivo de proyecto.\n" + msg,
                    "Error al Abrir Proyecto",
                    JOptionPane.ERROR_MESSAGE);
        }
    } // --- Fin del metodo: solicitarAbrirProyecto ---


    /**
     * Guarda el proyecto en su archivo actual; si es temporal, delega en Guardar Como.
     */
    public void solicitarGuardarProyecto() {
        if (projectManager == null)
            return;

        if (projectManager.getArchivoProyectoActivo() == null) {
            solicitarGuardarProyectoComo();
        } else {
            sincronizarModeloConUI();
            sincronizarArchivosAsociadosConModelo();
            sincronizarDescripcionDesdeUI();
            sincronizarMetadatosParaGuardado();

            projectManager.guardarAArchivo();
            projectManager.markProjectAsSaved();
            generalController.actualizarTituloVentana();

            logger.info("Proyecto {} guardado.", projectManager.getNombreProyectoActivo());
        }

    } // --- Fin del metodo: solicitarGuardarProyecto ---


    /**
     * Orquesta el guardado del proyecto actual en una nueva ubicación.
     */
    public void solicitarGuardarProyectoComo() {
        if (projectManager == null || generalController == null || view == null) {
            logger.error("ERROR [solicitarGuardarProyectoComo]: Dependencias nulas.");
            return;
        }

        int size = projectManager.getCurrentProject().getSelectedImages().size();
        logger.info(
                ">>>>>>>>>> [GUARDADO] Al solicitar 'Guardar Como...', ProjectModel en memoria tiene {} imágenes seleccionadas.",
                size);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Proyecto Como...");
        fileChooser.setCurrentDirectory(projectManager.getCarpetaBaseProyectos().toFile());
        javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter(
                "Archivos de Proyecto (*.prj)", "prj");
        fileChooser.setFileFilter(filter);
        fileChooser.setSelectedFile(new java.io.File("MiProyecto.prj"));

        int result = fileChooser.showSaveDialog(view);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path archivoDestino = fileChooser.getSelectedFile().toPath();
        if (!archivoDestino.toString().toLowerCase().endsWith(".prj")) {
            Path fn = archivoDestino.getFileName();
            String s = (fn != null) ? fn.toString() : archivoDestino.toString();
            archivoDestino = archivoDestino.resolveSibling(s + ".prj");
        }

        if (java.nio.file.Files.exists(archivoDestino)) {
            int overwriteConfirm = JOptionPane.showConfirmDialog(
                    view, "El archivo ya existe. ¿Deseas sobrescribirlo?", "Confirmar Sobrescribir",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (overwriteConfirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // Simplemente le pide al manager que guarde su estado actual en la nueva ruta.
        sincronizarModeloConUI();
        sincronizarArchivosAsociadosConModelo();
        sincronizarDescripcionDesdeUI();

        // Primero se establece la nueva ruta activa...
        projectManager.guardarProyectoComo(archivoDestino);

        // ...y DESPUÉS se actualiza el nombre del proyecto a partir de la nueva ruta.
        sincronizarMetadatosParaGuardado();

        // Se re-guarda con el nombre correcto.
        projectManager.guardarAArchivo();
        projectManager.limpiarArchivoTemporal();
        projectManager.markProjectAsSaved();
        generalController.actualizarTituloVentana();

        // Restaura el feedback vital para el usuario.
        Path fnFinal = archivoDestino.getFileName();
        String sFinal = (fnFinal != null) ? fnFinal.toString() : archivoDestino.toString();
        JOptionPane.showMessageDialog(
                view,
                "El proyecto se ha guardado correctamente como '" + sFinal + "'.",
                "Proyecto Guardado",
                JOptionPane.INFORMATION_MESSAGE);
    } // --- Fin del metodo: solicitarGuardarProyectoComo ---


    /**
     * Actualiza los metadatos del ProjectModel (nombre, fecha de modificación) antes de guardar.
     */
    private void sincronizarMetadatosParaGuardado() {
        if (projectManager == null)
            return;

        ProjectModel currentProject = projectManager.getCurrentProject();
        if (currentProject == null)
            return;

        Path archivoActivo = projectManager.getArchivoProyectoActivo();

        if (archivoActivo != null) {
            Path fn = archivoActivo.getFileName();
            String fileName = (fn != null) ? fn.toString() : archivoActivo.toString();
            if (fileName.toLowerCase().endsWith(".prj")) {
                fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            }
            currentProject.setProjectName(fileName);
        } else {
            currentProject.setProjectName("Proyecto Temporal");
        }

        currentProject.setLastModifiedDate(System.currentTimeMillis());
        logger.debug("Metadatos del proyecto (nombre y fecha) sincronizados para guardado.");
    } // --- Fin del metodo: sincronizarMetadatosParaGuardado ---


    /**
     * Sincroniza el ProjectModel en el ProjectManager con el estado actual de la UI.
     */
    public void sincronizarModeloConUI() {
        if (projectManager == null || registry == null) {
            logger.warn("WARN [sincronizarModeloConUI]: ProjectManager o Registry son nulos. Sincronización abortada.");
            return;
        }

        ProjectModel modeloActual = projectManager.getCurrentProject();
        if (modeloActual == null) {
            logger.error("ERROR CRÍTICO [sincronizarModeloConUI]: El ProjectModel en ProjectManager es nulo.");
            return;
        }

        Map<String, String> etiquetasExistentes = new HashMap<>(modeloActual.getSelectedImages());

        JList<String> listaSeleccionUI = registry.get("list.proyecto.nombres");
        List<String> elementosSeleccion = listaSeleccionUI != null && listaSeleccionUI.getModel() != null
                ? listModelToList(listaSeleccionUI.getModel()) : List.of();

        JList<String> listaDescartesUI = registry.get("list.proyecto.descartes");
        List<String> elementosDescartes = listaDescartesUI != null && listaDescartesUI.getModel() != null
                ? listModelToList(listaDescartesUI.getModel()) : List.of();

        // Si ambas listas de UI están vacías (modo cliente, .prjcl directo, etc.)
        // NO reemplazar masterImages para evitar borrar los datos del proyecto.
        if (elementosSeleccion.isEmpty() && elementosDescartes.isEmpty()) {
            logger.info("[sincronizarModeloConUI]: Listas de UI vacías, se omite sincronización para preservar masterImages.");
            return;
        }

        syncService.sincronizarListas(modeloActual, elementosSeleccion, elementosDescartes, etiquetasExistentes);
    } // --- Fin del metodo: sincronizarModeloConUI ---


    /**
     * Delega en ProjectSyncService para sincronizar los archivos asociados de la cola con el ProjectModel.
     */
    public void sincronizarArchivosAsociadosConModelo() {
        if (projectManager == null || exportQueueManager == null) {
            logger.warn("[sincronizarArchivosAsociados] Sincronización abortada (dependencias nulas).");
            return;
        }
        syncService.sincronizarArchivosAsociadosConModelo();
    } // --- Fin del metodo: sincronizarArchivosAsociadosConModelo ---


    /**
     * Carga los metadatos (nombre, descripción) desde el ProjectModel y actualiza el panel de propiedades.
     */
    private void actualizarPanelDePropiedadesEnUI() {
        if (projectManager == null)
            return;

        ProjectModel currentProject = projectManager.getCurrentProject();
        if (currentProject == null)
            return;

        String name = projectManager.getNombreProyectoActivo();
        if (name.toLowerCase().endsWith(".prj")) {
            name = name.substring(0, name.lastIndexOf('.'));
        }
        String description = currentProject.getProjectDescription() != null ? currentProject.getProjectDescription() : "";

        uiManager.actualizarPanelPropiedades(name, description);
        logger.debug("Panel de propiedades en la UI actualizado con los datos del modelo.");
    } // --- Fin del metodo: actualizarPanelDePropiedadesEnUI ---


    /**
     * Sincroniza la descripción del proyecto desde el campo de texto de la UI.
     */
    public void sincronizarDescripcionDesdeUI() {
        if (projectManager == null || registry == null)
            return;

        vista.panels.export.ProjectMetadataPanel propsPanel = registry.get("panel.proyecto.propiedades");
        ProjectModel currentProject = projectManager.getCurrentProject();

        if (propsPanel != null && currentProject != null) {
            syncService.sincronizarDescripcion(currentProject, propsPanel.getProjectDescriptionArea().getText());
        }
    } // --- Fin del metodo: sincronizarDescripcionDesdeUI ---


    /**
     * Actualiza los títulos de los paneles "Selección Actual" y "Descartes".
     */
    public void actualizarContadoresDeTitulos() {
        if (registry == null || generalController == null || generalController.getVisorController() == null
                || generalController.getVisorController().getThemeManager() == null)
            return;
        Tema tema = generalController.getVisorController().getThemeManager().getTemaActual();
        uiManager.actualizarContadoresDeTitulos(tema);
    } // --- Fin del metodo: actualizarContadoresDeTitulos ---


    /**
     * Pide confirmación y elimina permanentemente las imágenes seleccionadas en Descartes del proyecto.
     */
    public void solicitarEliminacionPermanente() {
        if (registry == null || projectManager == null || view == null) {
            return;
        }

        JList<String> listaDescartesUI = registry.get("list.proyecto.descartes");
        if (listaDescartesUI == null) {
            return;
        }

        List<Path> rutasAEliminar = obtenerRutasDesdeListaSeleccionada(listaDescartesUI);
        if (rutasAEliminar.isEmpty()) {
            return;
        }

        int cantidad = rutasAEliminar.size();
        String mensaje = cantidad == 1
                ? "¿Seguro que quieres eliminar esta imagen del proyecto?\n(No se borrará el archivo del disco)"
                : "¿Seguro que quieres eliminar estas " + cantidad + " imágenes del proyecto?\n(No se borrarán los archivos del disco)";

        int confirm = JOptionPane.showConfirmDialog(view, mensaje,
                "Confirmar Eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            int indiceAncla = obtenerIndiceAnclaDeLista(listaDescartesUI);
            int eliminados = fileManagementService.eliminarDelProyecto(rutasAEliminar);
            if (eliminados > 0) {
                logger.debug("  [ProjectController] {} imagen(es) eliminada(s) del proyecto.", eliminados);
                refrescarListasDeProyecto();
                reubicarSeleccionTrasOperacionEnLista(registry.get("list.proyecto.descartes"), indiceAncla,
                        "descartes");
            }
        }
    } // --- Fin del metodo: solicitarEliminacionPermanente ---


    /**
     * Delega en ProjectUIManager para aplicar colores de fondo y texto según la lista con foco.
     */
    private void actualizarAparienciaListasPorFoco() {
        if (registry == null || model == null || generalController == null
                || generalController.getVisorController() == null
                || generalController.getVisorController().getThemeManager() == null)
            return;
        String nombreListaActiva = model.getProyectoListContext().getNombreListaActiva();
        Tema tema = generalController.getVisorController().getThemeManager().getTemaActual();
        uiManager.actualizarAparienciaListasPorFoco(nombreListaActiva, tema);
    } // --- Fin del metodo: actualizarAparienciaListasPorFoco ---


    /**
     * Construye y devuelve un JPopupMenu dinámico para el visor principal.
     *
     * @return JPopupMenu contextual
     */
    public JPopupMenu crearMenuContextualVisorManualmente() {
        return uiManager.crearMenuContextualVisorManualmente(currentViewState, model, actionMap);
    } // --- Fin del metodo: crearMenuContextualVisorManualmente ---


    /**
     * Método central para refrescar toda la UI del modo proyecto.
     */
    public void refrescarVistaProyectoCompleta() {
        logger.info("[ProjectController] Iniciando refresco completo de la vista del proyecto...");

        autoRelocalizarImagenesHuerfanas();

        poblarListasSeleccionYDescartes();

        if (isExportPanelVisible()) {
            logger.debug(" -> Panel de exportación visible. Preparando nueva cola...");
            solicitarPreparacionColaExportacion();
        }

        actualizarModeloPrincipalConListaDeProyectoActiva();
        sincronizarSeleccionEnGridProyecto();

        refrescarGridProyecto();

        logger.info("[ProjectController] Refresco completo de la vista del proyecto finalizado.");
    } // --- Fin del metodo: refrescarVistaProyectoCompleta ---


    /**
     * Configura el menú contextual para la tabla de exportación.
     */
    public void configurarContextMenuTablaExportacion() {
        uiManager.configurarContextMenuTablaExportacion(actionMap, model);
    } // --- Fin del metodo: configurarContextMenuTablaExportacion ---


    /**
     * Permite al usuario relocalizar manualmente una imagen no encontrada.
     */
    public void solicitarRelocalizacionImagen() {
        if (registry == null || exportQueueManager == null || view == null)
            return;
        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1)
            return;
        int modelRow = tablaExportacion.convertRowIndexToModel(tablaExportacion.getSelectedRow());
        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
        modelo.proyecto.ExportItem item = modelTabla.getItemAt(modelRow);
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
            Path oldPath = item.getRutaImagen();

            fileManagementService.relocalizarImagenEnCola(item, nuevaRuta, modelRow);
            fileManagementService.migrarClaveEnModelo(projectManager.getCurrentProject(), oldPath, nuevaRuta);
            projectManager.notificarModificacion();

            refrescarVistaProyectoCompleta();
            logger.info("Imagen relocalizada manualmente con éxito de: {} -> {}", oldPath, nuevaRuta);
        }
    } // --- Fin del metodo: solicitarRelocalizacionImagen ---


    /**
     * Orquesta el movimiento de una imagen de la lista de selección a descartes.
     */
    public void solicitarMoverSeleccionadoAdescartes() {
        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) {
            logger.warn("Se intentó mover a descartes sin selección en la tabla de exportación.");
            return;
        }

        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
        int modelRow = tablaExportacion.convertRowIndexToModel(tablaExportacion.getSelectedRow());
        ExportItem selectedItem = modelTabla.getItemAt(modelRow);

        if (selectedItem != null) {
            logger.debug("Solicitud para mover a descartes: {}", selectedItem.getRutaImagen().getFileName());

            projectManager.moverAdescartes(selectedItem.getRutaImagen());

            projectManager.notificarModificacion();

            refrescarVistaProyectoCompleta();
        }
    } // --- Fin del metodo: solicitarMoverSeleccionadoAdescartes ---


    /**
     * Delega en ProjectIntegrityService para relocalizar automáticamente imágenes con rutas rotas.
     */
    public void autoRelocalizarImagenesHuerfanas() {
        if (generalController == null) return;
        DataManager dm = generalController.getDataController() != null
                ? generalController.getDataController().getDataManager() : null;
        if (dm == null) return;
        integrityService.autoRelocalizarImagenesHuerfanas(dm);
    } // --- Fin del metodo: autoRelocalizarImagenesHuerfanas ---


    /**
     * Identifica imágenes huérfanas sin disco, pide confirmación y las elimina del proyecto.
     */
    public void solicitarLimpiarImagenesNoEncontradas() {
        if (projectManager == null || projectManager.getCurrentProject() == null) {
            return;
        }

        ProjectIntegrityService.OrphanReport report = integrityService.identificarHuerfanos();

        if (report.isEmpty())
        {
            JOptionPane.showMessageDialog(view,
                    "No se encontraron imágenes huérfanas en el proyecto.",
                    "Limpieza de Proyecto",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(view,
                "Se han detectado " + report.getTotal()
                        + " imágenes en el proyecto que ya no existen en disco.\n"
                        + "¿Desea eliminarlas permanentemente del proyecto?\n\n"
                        + "Nota: Esta acción no borrará ningún archivo de su disco duro, solo limpiará el proyecto.",
                "Confirmar Limpieza de Huérfanos",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION)
        {
            integrityService.limpiarHuerfanos(report);
            projectManager.notificarModificacion();
            refrescarVistaProyectoCompleta();

            JOptionPane.showMessageDialog(view,
                    "Se han limpiado " + report.getTotal() + " imágenes huérfanas del proyecto con éxito.",
                    "Limpieza Completada",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    } // --- Fin del metodo: solicitarLimpiarImagenesNoEncontradas ---


    /**
     * Navega filas de la tabla de exportación hacia arriba o abajo según el giro de la rueda del ratón.
     *
     * @param e Evento de rueda del ratón
     */
    public void navegarTablaExportacionConRueda(java.awt.event.MouseWheelEvent e) {
        if (registry == null || model == null) {
            logger.warn("WARN [navegarTablaExportacionConRueda]: Registry o Modelo nulos.");
            return;
        }

        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getModel().getRowCount() == 0) {
            logger.debug(
                    "  [navegarTablaExportacionConRueda] Tabla de exportación vacía o no encontrada. No se puede navegar.");
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
    } // --- Fin del metodo: navegarTablaExportacionConRueda ---


    /**
     * Selecciona y muestra en el visor principal la imagen indicada por su ruta absoluta.
     *
     * @param rutaImagen Ruta de la imagen a mostrar
     */
    public void mostrarImagenDeExportacion(Path rutaImagen) {
        logger.debug("[ProjectController] Solicitud para mostrar imagen de exportación: " + rutaImagen);

        if (model == null || projectListCoordinator == null) {
            logger.error("ERROR [mostrarImagenDeExportacion]: Modelo o ProjectListCoordinator nulos.");
            return;
        }
        if (rutaImagen == null) {
            logger.debug("[mostrarImagenDeExportacion]: Ruta de imagen nula. Limpiando visor principal.");
            projectListCoordinator.seleccionarImagenPorIndice(-1);
            return;
        }

        String claveImagen = rutaImagen.toString().replace("\\", "/");

        projectListCoordinator.seleccionarImagenPorClave(claveImagen);

    } // --- Fin del metodo: mostrarImagenDeExportacion ---


    /**
     * Abre el explorador y selecciona el archivo de la imagen actualmente seleccionada en el visor.
     */
    public void solicitarLocalizarArchivoSeleccionado() {
        if (model == null)
            return;
        String claveSeleccionada = model.getSelectedImageKey();
        if (claveSeleccionada == null || claveSeleccionada.isEmpty()) {
            logger.debug("[ProjectController] No hay imagen seleccionada para localizar.");
            return;
        }

        Path rutaAbsoluta = model.getProyectoListContext().getRutaCompleta(claveSeleccionada);
        if (rutaAbsoluta == null)
            return;

        if (Files.exists(rutaAbsoluta)) {
            try {
                DesktopUtils.openAndSelectFile(rutaAbsoluta);
            } catch (Exception e) {
                logger.error("Error al intentar abrir y seleccionar el archivo: " + e.getMessage());
                JOptionPane.showMessageDialog(view, "No se pudo abrir la ubicación del archivo.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            int opcion = JOptionPane.showConfirmDialog(view,
                    "El archivo '" + rutaAbsoluta.getFileName() + "' ya no está disponible.\n" +
                            "¿Deseas relocalizarlo seleccionando el nuevo archivo o nombre?",
                    "Archivo no encontrado",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (opcion == JOptionPane.YES_OPTION) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Relocalizar Imagen: " + rutaAbsoluta.getFileName());

                Path parent = rutaAbsoluta.getParent();
                if (parent != null && Files.exists(parent)) {
                    fileChooser.setCurrentDirectory(parent.toFile());
                }

                javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter(
                        "Imágenes (jpg, png, gif, bmp)", "jpg", "jpeg", "png", "gif", "bmp");
                fileChooser.setFileFilter(filter);

                int result = fileChooser.showOpenDialog(view);
                if (result == JFileChooser.APPROVE_OPTION) {
                    Path nuevaRuta = fileChooser.getSelectedFile().toPath();

                    fileManagementService.relocalizarImagen(rutaAbsoluta, nuevaRuta);

                    prepararDatosProyecto();
                    refrescarVistaProyectoCompleta();

                    String nuevaClave = nuevaRuta.toString().replace("\\", "/");
                    if (projectListCoordinator != null) {
                        projectListCoordinator.seleccionarImagenPorClave(nuevaClave);
                    }
                }
            }
        }
    } // --- Fin del metodo: solicitarLocalizarArchivoSeleccionado ---


    /**
     * Pide confirmación y elimina permanentemente todas las imágenes de la lista de Descartes.
     */
    public void solicitarVaciarDescartes() {
        if (projectManager == null || view == null)
            return;

        int confirm = JOptionPane.showConfirmDialog(
                view,
                "¿Seguro que quieres vaciar TODAS las imágenes de la lista de descartes?\n" +
                        "(Las imágenes se eliminaran del proyecto pero no del disco)",
                "Confirmar Vaciar Descartes",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {

            projectManager.vaciarDescartes();
            projectManager.notificarModificacion();

            refrescarListasDeProyecto();
        }
    } // --- Fin del metodo: solicitarVaciarDescartes ---


    /**
     * Muestra un diálogo para editar la etiqueta de texto de la imagen seleccionada en el grid.
     */
    public void solicitarEtiquetaParaImagenSeleccionada() {
        if (model.getCurrentDisplayMode() != VisorModel.DisplayMode.GRID)
            return;

        String claveSeleccionada = model.getSelectedImageKey();
        if (claveSeleccionada == null || claveSeleccionada.isBlank())
            return;

        Path ruta = model.getRutaCompleta(claveSeleccionada);
        if (ruta == null)
            return;

        String etiquetaActual = projectManager.getEtiqueta(ruta);
        if (etiquetaActual == null)
            etiquetaActual = "";

        Path fn = ruta.getFileName();
        String s = (fn != null) ? fn.toString() : ruta.toString();
        String nuevaEtiqueta = (String) JOptionPane.showInputDialog(
                view,
                "Introduce la etiqueta para:\n" + s,
                "Editar Etiqueta",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                etiquetaActual);

        if (nuevaEtiqueta != null) {
            projectManager.setEtiqueta(ruta, nuevaEtiqueta);

            projectManager.notificarModificacion();

            refrescarGridProyecto();
        }
    } // --- Fin del metodo: solicitarEtiquetaParaImagenSeleccionada ---


    /**
     * Elimina la etiqueta de texto de la imagen seleccionada en el grid.
     */
    public void solicitarBorradoEtiquetaParaImagenSeleccionada() {
        if (model.getCurrentDisplayMode() != VisorModel.DisplayMode.GRID)
            return;

        String claveSeleccionada = model.getSelectedImageKey();
        if (claveSeleccionada == null || claveSeleccionada.isBlank())
            return;

        Path ruta = model.getRutaCompleta(claveSeleccionada);
        if (ruta != null) {
            projectManager.setEtiqueta(ruta, null);

            projectManager.notificarModificacion();

            refrescarGridProyecto();
        }
    } // --- Fin del metodo: solicitarBorradoEtiquetaParaImagenSeleccionada ---


    /**
     * Escala el tamaño de las celdas del grid aplicando un factor multiplicador (límites 50-500 px).
     *
     * @param factor Factor de escala
     */
    public void cambiarTamanoGrid(double factor) {
        if (model.getCurrentDisplayMode() != VisorModel.DisplayMode.GRID)
            return;

        vista.panels.GridDisplayPanel gridPanel = registry.get("panel.display.grid.proyecto");
        if (gridPanel == null)
            return;

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

    } // --- Fin del metodo: cambiarTamanoGrid ---


    /**
     * Fuerza el revalidate y repaint de la JList del grid de proyecto.
     */
    private void refrescarGridProyecto() {
        JList<String> gridList = registry.get("list.grid.proyecto");
        if (gridList != null) {
            gridList.revalidate();
            gridList.repaint();
        }
    } // --- Fin del metodo: refrescarGridProyecto ---


    /**
     * Busca los renderers de las listas de proyecto en el registro y limpia su caché.
     */
    private void limpiarCacheRenderersProyecto() {
        if (registry == null) {
            return;
        }

        JList<String> listaNombres = registry.get("list.proyecto.nombres");
        if (listaNombres != null && listaNombres.getCellRenderer() instanceof vista.renderers.ProjectListCellRenderer) {
            ((vista.renderers.ProjectListCellRenderer) listaNombres.getCellRenderer()).clearCache();
        }

        JList<String> listaDescartes = registry.get("list.proyecto.descartes");
        if (listaDescartes != null
                && listaDescartes.getCellRenderer() instanceof vista.renderers.ProjectListCellRenderer) {
            ((vista.renderers.ProjectListCellRenderer) listaDescartes.getCellRenderer()).clearCache();
        }

        logger.debug("[ProjectController] Caché de los renderers de listas de proyecto limpiado.");
    } // --- Fin del metodo: limpiarCacheRenderersProyecto ---


    /**
     * Obtiene la JTable de exportación desde el ExportPanel registrado en ComponentRegistry.
     *
     * @return JTable de exportación, o null
     */
    public JTable getTablaExportacionDesdeRegistro() {
        return uiManager.getTablaExportacionDesdeRegistro();
    } // --- Fin del metodo: getTablaExportacionDesdeRegistro ---


    /**
     * Comprueba si el panel de herramientas de exportación está visible.
     *
     * @return true si el panel de exportación está visible
     */
    public boolean isExportPanelVisible() {
        if (registry == null)
            return false;
        JPanel toolsPanel = registry.get("panel.proyecto.herramientas.container");
        return toolsPanel != null && toolsPanel.isVisible();
    } // --- Fin del metodo: isExportPanelVisible ---


    /**
     * Establece la visibilidad del panel de herramientas de exportación sin alterar el estado.
     *
     * @param visible true para mostrar, false para ocultar
     */
    public void setExportPanelVisible(boolean visible) {
        if (registry == null)
            return;
        JSplitPane rightSplit = registry.get("splitpane.proyecto.right");
        JPanel toolsPanel = registry.get("panel.proyecto.herramientas.container");
        if (rightSplit == null || toolsPanel == null)
            return;

        if (visible) {
            if (!toolsPanel.isVisible()) {
                if (currentLayout == ProjectLayout.DEFAULT) {
                    toggleProjectLayout();
                }
                toolsPanel.setVisible(true);
                rightSplit.setDividerSize(5);

                ensureExportPanelIsFullyInitialized();

                SwingUtilities.invokeLater(() -> {
                    ajustarPosicionDivisorDerecho();
                });

                logger.debug("[ProjectController] Panel de exportación restaurado a visible.");
            }
        } else {
            if (toolsPanel.isVisible()) {
                lastRightDividerLocation = rightSplit.getDividerLocation();
                toolsPanel.setVisible(false);
                rightSplit.setDividerSize(0);
                if (currentLayout == ProjectLayout.ASSIGNMENT) {
                    toggleProjectLayout();
                }
                logger.debug("[ProjectController] Panel de exportación ocultado programáticamente.");
            }
        }

        // Sincronizar el estado del botón de toggle
        Action toggleAction = actionMap.get(AppActionCommands.CMD_EXPORT_ASSIGN_PANNEL);
        if (toggleAction != null) {
            toggleAction.putValue(Action.SELECTED_KEY, visible);
        }
    } // --- Fin del metodo: setExportPanelVisible ---


    /**
     * Calcula y ajusta la posición del divisor del split pane derecho.
     */
    public void ajustarPosicionDivisorDerecho() {
        uiManager.ajustarPosicionDivisorDerecho();
    } // --- Fin del metodo: ajustarPosicionDivisorDerecho ---


    /**
     * Inyecta el ProjectListCoordinator y establece la referencia bidireccional.
     *
     * @param coordinator Coordinador de listas del proyecto
     */
    public void setProjectListCoordinator(ProjectListCoordinator coordinator) {
        this.projectListCoordinator = coordinator;
        if (this.projectListCoordinator != null) {
            this.projectListCoordinator.setProjectController(this);
        }
    } // --- Fin del metodo: setProjectListCoordinator ---


    /**
     * Inyecta el GeneralController, verificando que no sea null.
     *
     * @param generalController Controlador general
     */
    public void setGeneralController(GeneralController generalController) {
        this.generalController = Objects.requireNonNull(generalController,
                "GeneralController no puede ser null en ProjectController");
    } // --- Fin del metodo: setGeneralController ---


    /**
     * Convierte un ListModel de Swing a una List de Java estándar.
     *
     * @param model ListModel de Swing
     * @return Lista de strings
     */
    private List<String> listModelToList(javax.swing.ListModel<String> model)
    {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++)
        {
            result.add(model.getElementAt(i));
        }
        return result;
    } // --- Fin del metodo: listModelToList ---


    /**
     * Inyecta el IProjectManager y crea los servicios que dependen de él.
     *
     * @param projectManager Gestor de proyectos
     */
    public void setProjectManager(IProjectManager projectManager) {
        this.projectManager = Objects.requireNonNull(projectManager);
        this.fileManagementService = new ProjectFileManagementService(this.projectManager, this.exportQueueManager);
        this.integrityService = new ProjectIntegrityService(this.projectManager);
        this.syncService = new ProjectSyncService(this.projectManager, this.exportQueueManager);
    } // --- Fin del metodo: setProjectManager ---


    /**
     * Inyecta el ComponentRegistry y crea el ProjectUIManager asociado.
     *
     * @param registry Registro de componentes
     */
    public void setRegistry(ComponentRegistry registry) {
        this.registry = Objects.requireNonNull(registry);
        this.uiManager = new ProjectUIManager(this.registry);
    } // --- Fin del metodo: setRegistry ---


    /**
     * Inyecta el DataManager para operaciones de etiquetado desde el visor de proyecto.
     *
     * @param dataManager Gestor de datos
     */
    public void setDataManager(controlador.managers.DataManager dataManager) {
        if (this.uiManager != null) {
            this.uiManager.setDataManager(dataManager);
        }
    } // --- Fin del metodo: setDataManager ---


    /**
     * Inyecta el IZoomManager para operaciones de zoom y paneo.
     *
     * @param zoomManager Gestor de zoom
     */
    public void setZoomManager(IZoomManager zoomManager) {
        this.zoomManager = Objects.requireNonNull(zoomManager);
    } // --- Fin del metodo: setZoomManager ---


    /**
     * Inyecta la referencia a la vista principal del visor.
     *
     * @param view Vista principal
     */
    public void setView(VisorView view) {
        this.view = Objects.requireNonNull(view);
    } // --- Fin del metodo: setView ---


    /**
     * Inyecta el mapa de acciones registradas en el ProjectBuilder.
     *
     * @param actionMap Mapa de acciones
     */
    public void setActionMap(Map<String, Action> actionMap) {
        this.actionMap = Objects.requireNonNull(actionMap);
    } // --- Fin del metodo: setActionMap ---


    /**
     * Inyecta el VisorModel que contiene el estado central de la aplicación.
     *
     * @param model Modelo del visor
     */
    public void setModel(VisorModel model) {
        this.model = Objects.requireNonNull(model);
    } // --- Fin del metodo: setModel ---


    /**
     * Inyecta el DisplayModeManager para controlar los modos de visualización.
     *
     * @param displayModeManager Gestor de modos de visualización
     */
    public void setDisplayModeManager(DisplayModeManager displayModeManager) {
        this.displayModeManager = displayModeManager;
    } // --- Fin del metodo: setDisplayModeManager ---


    /**
     * Devuelve la referencia al ProjectManager inyectado.
     *
     * @return ProjectManager
     */
    public IProjectManager getProjectManager() {
        return this.projectManager;
    } // --- Fin del metodo: getProjectManager ---


    /**
     * Devuelve la referencia al ProjectListCoordinator inyectado.
     *
     * @return ProjectListCoordinator
     */
    public ProjectListCoordinator getProjectListCoordinator() {
        return this.projectListCoordinator;
    } // --- Fin del metodo: getProjectListCoordinator ---


    /**
     * Devuelve la referencia a la VisorView principal.
     *
     * @return VisorView
     */
    public VisorView getView() {
        return this.view;
    } // --- Fin del metodo: getView ---


    /**
     * Devuelve la referencia al ComponentRegistry inyectado.
     *
     * @return ComponentRegistry
     */
    public ComponentRegistry getRegistry() {
        return this.registry;
    } // --- Fin del metodo: getRegistry ---


    /**
     * Devuelve la referencia al GeneralController inyectado.
     *
     * @return GeneralController
     */
    public GeneralController getGeneralController() {
        return this.generalController;
    } // --- Fin del metodo: getGeneralController ---

    public ProjectLayout getCurrentLayout() {
        return this.currentLayout;
    }


    /**
     * Devuelve el mapa de acciones registradas en el ProjectBuilder.
     *
     * @return Mapa de acciones
     */
    public Map<String, Action> getActionMap() {
        return this.actionMap;
    } // --- Fin del metodo: getActionMap ---

} // --- Fin del metodo/clase ProjectController ---
