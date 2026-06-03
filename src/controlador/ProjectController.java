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
import controlador.managers.DataManager;
import controlador.managers.DisplayModeManager;
import controlador.managers.ExportQueueManager;
import controlador.managers.interfaces.IProjectManager;
import controlador.managers.interfaces.IZoomManager;
import controlador.utils.ComponentRegistry;
import controlador.utils.DesktopUtils;
import controlador.services.proyecto.ExportPreflightReport;
import controlador.services.proyecto.ExportStatusReport;
import controlador.services.proyecto.PdfWorkflowService;
import controlador.services.proyecto.ProjectExportService;
import controlador.services.proyecto.ProjectFileManagementService;
import controlador.services.proyecto.ProjectIntegrityService;
import controlador.worker.ExportWorker;
import modelo.ListContext;
import modelo.VisorModel;
import modelo.export.pdf.PDFExportPreflightService;
import modelo.proyecto.ExportItem;
import vista.dialogos.PDFExportPreflightDialog;
import vista.dialogos.PDFPreviewDialog;
import modelo.proyecto.ExportStatus;
import modelo.proyecto.ProjectModel;
import vista.VisorView;
import vista.dialogos.TaskProgressDialog;
import vista.panels.export.ExportDetailPanel;
import vista.panels.export.ExportPanel;
import vista.panels.export.ExportTableModel;

public class ProjectController implements IModoController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    // Define los posibles estados de visualización del panel de proyecto
    private enum ProjectViewState {
        VIEW_SELECTION, // Foco en la lista de Selección, Grid normal
        VIEW_DISCARDS, // Foco en la lista de Descartes, Grid normal
        VIEW_EXPORT // Panel de exportación activo, Grid muestra Selección con bordes de estado
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
    private ProjectExportService exportService;
    private PdfWorkflowService pdfWorkflowService;
    private ProjectFileManagementService fileManagementService;
    private ProjectIntegrityService integrityService;

    private Map<String, Action> actionMap;
    private Map<String, ExportItem> exportItemMap = new HashMap<>();
    private int lastRightDividerLocation = -1;

    public ProjectController() {
        logger.debug("[ProjectController] Instancia creada.");
        this.exportQueueManager = new ExportQueueManager();
        this.exportService = new ProjectExportService();
        this.pdfWorkflowService = new PdfWorkflowService();
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


    // Limpia por completo la interfaz de usuario del modo Proyecto
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

    // Limpia de forma exhaustiva todo el estado relacionado con el proyecto actual
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


    // El método orquestador central DEFINITIVO
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


    // Actualiza el mapa interno que se usa para buscar rápidamente un ExportItem
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


    // Alterna la visibilidad del panel de herramientas inferior derecho
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

    // Se llama DESPUÉS de que las barras de herramientas del modo Proyecto
    public void postToolbarInitialization() {
        logger.debug("[ProjectController] Realizando inicialización post-toolbars...");
        ensureExportPanelIsFullyInitialized();
        configurarContextMenuTablaExportacion();
        configurarListenersMetadatos();
        logger.debug("[ProjectController] Inicialización post-toolbars completada.");

    } // --- Fin del metodo: postToolbarInitialization ---


    // Añade un DocumentListener al área de descripción para que cualquier
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


    // Resetea el layout del panel derecho a su estado por defecto (panel de
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

    // Carga en la `masterList` del `VisorModel` la lista de datos correcta
    public void actualizarModeloPrincipalConListaDeProyectoActiva() {
        if (model == null || projectManager == null) {
            logger.warn(
                    "WARN [actualizarModeloPrincipalConListaDeProyectoActiva]: Modelo o ProjectManager nulos. No se puede actualizar.");
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

    } // --- Fin del nuevo método actualizarModeloPrincipalConListaDeProyectoActiva
      // ---

    public void sincronizarSeleccionEnGridProyecto() {
        if (registry == null || projectListCoordinator == null)
            return;

        if (model.getCurrentDisplayMode() != VisorModel.DisplayMode.GRID) {
            return;
        }

        JList<String> gridList = registry.get("list.grid.proyecto");
        if (gridList == null)
            return;

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
    } // --- Fin del metodo: sincronizarSeleccionEnGridProyecto ---


    private void cambiarFocoListaActiva(String nuevoFoco) {
        if ("descartes".equals(nuevoFoco)) {
            setProjectViewState(ProjectViewState.VIEW_DISCARDS);
        } else {
            setProjectViewState(ProjectViewState.VIEW_SELECTION);
        }
    } // --- Fin del metodo: cambiarFocoListaActiva ---


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


    // Se llama desde GeneralController cuando se entra en el modo Proyecto
    public void activarVistaProyecto() {
        logger.debug("  [ProjectController] Activando la UI de la vista de proyecto...");

        limpiarCacheRenderersProyecto();

        if (registry == null || model == null || projectManager == null || projectListCoordinator == null
                || generalController == null) {
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

    } // --- Fin del metodo: activarVistaProyecto ---


    private void ensureExportPanelIsFullyInitialized() {
        ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");
        if (exportPanel != null) {
            exportPanel.setupHighlightingListener();
        }
    } // --- Fin del metodo: ensureExportPanelIsFullyInitialized ---


    // Rellena las JList de Selección y Descartes con los datos del ProjectManager
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

    } // --- Fin del metodo: poblarListasSeleccionYDescartes ---


    // Determina qué clave de imagen debe estar seleccionada al activar la vista
    private String determinarClaveASeleccionar(ListContext proyectoContext) {
        String focoActual = proyectoContext.getNombreListaActiva();
        String claveParaMostrar = null;

        if ("descartes".equals(focoActual)) {
            claveParaMostrar = proyectoContext.getDescartesListKey();
        } else {
            claveParaMostrar = proyectoContext.getSeleccionListKey();
        }

        // Si después de buscar la clave guardada, sigue siendo nula o no está en la
        // lista actual...
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

    } // --- Fin del metodo: determinarClaveASeleccionar ---


    // Ajusta los componentes visuales de la UI del modo proyecto
    private void ajustarLayoutProyectoUI() {
        SwingUtilities.invokeLater(() -> {
            actualizarAparienciaListasPorFoco();

            // Este método ya no es necesario aquí, el Builder lo maneja.
            // Si se necesita un ajuste dinámico, se puede añadir más lógica después.

            logger.debug("  [ProjectController] UI de la vista de proyecto activada y apariencia actualizada.");
        });
    } // --- Fin del metodo: ajustarLayoutProyectoUI ---


    @Override
    public void navegarSiguiente() {
        if (projectListCoordinator != null) {
            projectListCoordinator.seleccionarSiguiente();
        }
    } // --- Fin del metodo: navegarSiguiente ---


    @Override
    public void navegarAnterior() {
        if (projectListCoordinator != null)
            projectListCoordinator.seleccionarAnterior();
    } // --- Fin del metodo: navegarAnterior ---


    @Override
    public void navegarPrimero() {
        if (projectListCoordinator != null)
            projectListCoordinator.seleccionarPrimero();
    } // --- Fin del metodo: navegarPrimero ---


    @Override
    public void navegarUltimo() {
        if (projectListCoordinator != null)
            projectListCoordinator.seleccionarUltimo();
    } // --- Fin del metodo: navegarUltimo ---


    @Override
    public void navegarBloqueSiguiente() {
        if (projectListCoordinator != null)
            projectListCoordinator.seleccionarBloqueSiguiente();
    } // --- Fin del metodo: navegarBloqueSiguiente ---


    @Override
    public void navegarBloqueAnterior() {
        if (projectListCoordinator != null)
            projectListCoordinator.seleccionarBloqueAnterior();
    } // --- Fin del metodo: navegarBloqueAnterior ---


    @Override
    public void aplicarZoomConRueda(java.awt.event.MouseWheelEvent e) {
        if (zoomManager != null) {
            zoomManager.aplicarZoomConRueda(e);
            if (generalController != null && generalController.getVisorController() != null) {
                zoomManager.sincronizarEstadoVisualBotonesYRadiosZoom();
            }
        }
    } // --- Fin del metodo: aplicarZoomConRueda ---


    @Override
    public void aplicarPan(int deltaX, int deltaY) {
        if (zoomManager != null) {
            zoomManager.aplicarPan(deltaX, deltaY);
        }
    } // --- Fin del metodo: aplicarPan ---


    @Override
    public void iniciarPaneo(java.awt.event.MouseEvent e) {
        if (zoomManager != null && model.isZoomHabilitado()) {
            zoomManager.iniciarPaneo(e);
        }
    } // --- Fin del metodo: iniciarPaneo ---


    @Override
    public void solicitarRefresco() {
        logger.debug("[ProjectController] Solicitud de refresco recibida. Llamando a refrescarListasDeProyecto...");
        refrescarListasDeProyecto();
    } // --- Fin del metodo: solicitarRefresco ---


    @Override
    public void aumentarTamanoMiniaturas() {
        cambiarTamanoGrid(1.2); // Aumenta un 20%
    } // --- Fin del metodo: aumentarTamanoMiniaturas ---


    @Override
    public void reducirTamanoMiniaturas() {
        cambiarTamanoGrid(0.8); // Reduce un 20%
    } // --- Fin del metodo: reducirTamanoMiniaturas ---


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

        logger.debug(
                "  [ProjectController] Lista de descartes actualizada en la UI. Total: " + modeloDescartes.getSize());
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

    } // --- Fin del metodo: poblarListaDescartes ---


    public void moverSeleccionActualADescartes() {
        if (model == null || projectManager == null || registry == null) {
            return;
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

        logger.debug("  [ProjectController] {} imagen(es) movida(s) a descartes.", movidos);
        refrescarVistaProyectoCompleta();
        reubicarSeleccionTrasOperacionEnLista(registry.get("list.proyecto.nombres"), indiceAncla, "seleccion");
    } // --- Fin del metodo: moverSeleccionActualADescartes ---


    public void restaurarDesdeDescartes() {
        if (registry == null || projectManager == null) {
            return;
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

        logger.debug("  [ProjectController] {} imagen(es) restaurada(s) desde descartes.", restaurados);
        refrescarVistaProyectoCompleta();

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


    // Obtiene las rutas absolutas de todos los elementos seleccionados en una JList de proyecto
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


    // Índice de referencia para re-seleccionar tras una operación por lotes (ancla o mínimo seleccionado)
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


    // Tras refrescar una lista, deja seleccionado un único índice coherente con la operación anterior
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


    private void refrescarListasDeProyecto() {
        logger.debug("  [ProjectController] Refrescando ambas listas del proyecto...");
        prepararDatosProyecto();
        activarVistaProyecto();
    } // --- Fin del metodo: refrescarListasDeProyecto ---


    public void solicitarPreparacionColaExportacion() {
        solicitarPreparacionColaExportacion(false);
    } // --- Fin del método solicitarPreparacionColaExportacion (sin params) ---

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
        }
        
        actualizarEstadoExportacionUI();
    } // --- Fin del metodo: solicitarPreparacionColaExportacion ---


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


    public void onExportItemManuallyAssigned(modelo.proyecto.ExportItem itemModificado) {
        logger.debug("  [ProjectController] Archivo asignado manualmente para: "
                + itemModificado.getRutaImagen().getFileName());
        actualizarEstadoExportacionUI();
    } // --- Fin del metodo: onExportItemManuallyAssigned ---


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

        exportPanel.actualizarEstadoControles(puedeExportar, mensajeResumen);

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


    // Método de ayuda para actualizar el tooltip (SHORT_DESCRIPTION) de una acción
    private void actualizarTooltipAccion(String commandKey, String tooltipText) {
        if (actionMap != null) {
            Action action = actionMap.get(commandKey);
            if (action != null) {
                action.putValue(Action.SHORT_DESCRIPTION, tooltipText);
            }
        }
    } // --- Fin del metodo: actualizarTooltipAccion ---


    // Orquesta la adición de uno o más archivos asociados a un ExportItem
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
        int selectedRow = tablaExportacion.getSelectedRow();
        ExportItem selectedItem = tableModel.getItemAt(selectedRow);
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

            tableModel.fireTableRowsUpdated(selectedRow, selectedRow);
            actualizarEstadoExportacionUI();
            notificarCambioEnProyecto();

            ExportPanel exportPanel = getRegistry().get("panel.proyecto.exportacion.completo");
            if (exportPanel != null && exportPanel.getDetailPanel() != null) {
                exportPanel.getDetailPanel().updateDetails(selectedItem);
            }
        }
    } // --- Fin del metodo: solicitarAnadirArchivoAsociado ---


    // Orquesta la eliminación de un archivo asociado de un ExportItem
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
        int selectedRow = tablaExportacion.getSelectedRow();
        ExportItem selectedItem = model.getItemAt(selectedRow);

        if (selectedItem != null) {
            fileManagementService.removeAssociatedFile(selectedItem, archivoSeleccionado);

            if (selectedItem.getRutasArchivosAsociados().isEmpty()) {
                notificarCambioEnProyecto();
            }

            model.fireTableRowsUpdated(selectedRow, selectedRow);
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
    } // --- FIN de metodo solicitarLocalizarArchivoAsociado ---

    // Abre un selector de archivos para permitir al usuario añadir una o varias imágenes
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
    } // --- FIN de metodo solicitarAnadirArchivosAlProyecto ---

    // Sincroniza la selección de la JTable de exportación para que coincida con la
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
            int currentRow = tableModel.findRowIndexByPath(claveSeleccionada);
            if (currentRow != -1 && currentRow < tablaExportacion.getRowCount()) {
                if (tablaExportacion.getSelectedRow() != currentRow) {
                    tablaExportacion.setRowSelectionInterval(currentRow, currentRow);
                    tablaExportacion.scrollRectToVisible(tablaExportacion.getCellRect(currentRow, 0, true));
                }
            } else {
                tablaExportacion.clearSelection();
            }
        });
    } // --- Fin del metodo: sincronizarSeleccionEnTablaExportacion ---


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


    public void generarCatalogoPDF() {
        ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");
        if (exportPanel == null) return;

        ExportTableModel model = (ExportTableModel) exportPanel.getTablaExportacion().getModel();
        List<ExportItem> seleccionados = model.getCola().stream()
                .filter(ExportItem::isSeleccionadoParaExportar)
                .collect(Collectors.toList());

        if (seleccionados.isEmpty())
        {
            JOptionPane.showMessageDialog(null, "Selecciona elementos en la tabla.");
            return;
        }

        pdfWorkflowService.asignarCodigosCatalogo(seleccionados);

        PDFPreviewDialog preview = new PDFPreviewDialog(view, seleccionados);
        preview.setVisible(true);
        if (!preview.isConfirmed()) return;

        pdfWorkflowService.asignarCodigosCatalogo(seleccionados);

        if (seleccionados.isEmpty()) return;

        List<Path> paths = seleccionados.stream()
                .map(ExportItem::getRutaImagen)
                .collect(Collectors.toList());

        PDFExportPreflightService.PreflightResult result = pdfWorkflowService.ejecutarPreflight(seleccionados);

        if (!result.isSuccess)
        {
            PDFExportPreflightDialog dialog = new PDFExportPreflightDialog(view, paths, result.warnings);
            dialog.setVisible(true);
            if (!dialog.isGenerateConfirmed()) return;
        }

        JFileChooser chooser = new JFileChooser();
        javax.swing.filechooser.FileNameExtensionFilter pdfFilter =
                new javax.swing.filechooser.FileNameExtensionFilter("Archivos PDF (*.pdf)", "pdf");
        chooser.setFileFilter(pdfFilter);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
        {
            File destino = chooser.getSelectedFile();
            if (!destino.getName().toLowerCase().endsWith(".pdf"))
            {
                destino = new File(destino.getAbsolutePath() + ".pdf");
            }
            if (destino.exists())
            {
                int resp = JOptionPane.showConfirmDialog(null,
                        "El archivo ya existe. ¿Deseas sobrescribirlo?",
                        "Confirmar sobrescritura",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (resp != JOptionPane.YES_OPTION) return;
            }
            try
            {
                sincronizarDescripcionDesdeUI();
                String notasProyecto = projectManager != null && projectManager.getCurrentProject() != null
                        ? projectManager.getCurrentProject().getProjectDescription()
                        : null;
                pdfWorkflowService.generarPDF(seleccionados, destino.toPath(), notasProyecto);
                ProjectModel modeloActual = projectManager != null ? projectManager.getCurrentProject() : null;
                if (pdfWorkflowService.sincronizarDatosCatalogoConModelo(seleccionados, modeloActual))
                {
                    projectManager.notificarModificacion();
                }
                JOptionPane.showMessageDialog(null, "PDF Creado con éxito.");
            }
            catch (Exception e)
            {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error al generar el PDF:\n" + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    } // --- Fin del metodo: generarCatalogoPDF ---


    public void solicitarAbrirUbicacionImagen() {
        if (exportQueueManager == null || registry == null)
            return;
        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1)
            return;
        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
        modelo.proyecto.ExportItem itemSeleccionado = modelTabla.getItemAt(tablaExportacion.getSelectedRow());
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


    public void solicitarAlternarIgnorarComprimido() {
        if (registry == null)
            return;
        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1)
            return;
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
    } // --- Fin del metodo: solicitarAlternarIgnorarComprimido ---


    public void solicitarAsignacionManual() {
        // Esta acción ahora es idéntica a "Añadir Archivo Asociado".
        // Simplemente delegamos la llamada al método orquestador.
        solicitarAnadirArchivoAsociado();
    } // --- Fin del metodo: solicitarAsignacionManual ---


    public void solicitarQuitarDeLaCola() {
        if (exportQueueManager == null || registry == null)
            return;
        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1)
            return;
        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
        modelo.proyecto.ExportItem itemSeleccionado = modelTabla.getItemAt(tablaExportacion.getSelectedRow());
        if (itemSeleccionado != null) {
            exportQueueManager.getColaDeExportacion().remove(itemSeleccionado);
            modelTabla.setCola(exportQueueManager.getColaDeExportacion());
            actualizarEstadoExportacionUI();
        }
    } // --- Fin del metodo: solicitarQuitarDeLaCola ---


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

    // Orquesta la creación de un nuevo proyecto
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
    } // --- Fin del metodo: solicitarNuevoProyecto ---


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

            // PASO 4: Si estamos en modo proyecto, refrescar la vista para mostrar los
            // nuevos datos.
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
                    JOptionPane.ERROR_MESSAGE);
        }
    } // --- Fin del metodo: solicitarAbrirProyecto ---


    public void solicitarGuardarProyecto() {
        if (projectManager == null)
            return;

        if (projectManager.getArchivoProyectoActivo() == null) {
            // Si el proyecto es temporal, la acción "Guardar" debe comportarse como
            // "Guardar Como".
            Action guardarComoAction = actionMap.get(AppActionCommands.CMD_PROYECTO_GUARDAR_COMO);
            if (guardarComoAction != null) {
                guardarComoAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
            }
        } else {
            // Si ya tiene nombre, simplemente guarda. El archivo ya está actualizado por
            // las acciones.
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

    } // --- Fin del metodo: solicitarGuardarProyecto ---


    // Orquesta el guardado del proyecto actual en una nueva ubicación
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
        sincronizarMetadatosParaGuardado();

        projectManager.guardarProyectoComo(archivoDestino);
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


    // Actualiza los metadatos del ProjectModel (nombre, fecha de modificación)
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


    // Sincroniza el 'ProjectModel' en el ProjectManager con el estado actual
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

        // Preservamos el mapa de etiquetas existente para no perderlo al reconstruir la
        // lista de selección.
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

        logger.info(
                "[ProjectController] Sincronización de UI -> Modelo completada. El modelo está listo para guardarse.");
    } // --- Fin del metodo: sincronizarModeloConUI ---


    // Sincroniza el mapa de 'associatedFiles' en el ProjectModel con el estado
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
            config.setIgnoreCompressed(
                    item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO);
                    
            // Guardamos el estado de asignación (Automático, Manual, etc.)
            config.setStatus(item.getEstadoArchivoComprimido());

            // Guardamos la lista de archivos asociados.
            if (item.getRutasArchivosAsociados() != null && !item.getRutasArchivosAsociados().isEmpty()) {
                List<String> rutasComoString = item.getRutasArchivosAsociados().stream()
                        .map(path -> path.toString().replace("\\", "/"))
                        .collect(Collectors.toList());
                config.setAssociatedFiles(rutasComoString);
            }

            // Guardamos el código de catálogo
            config.setCodigoCatalogo(item.getCodigoCatalogo());
            config.setPiezas(item.getPiezas());
            config.setLvl(item.getLvl());
            config.setPvp(item.getPvp());
            config.setNotas(item.getNotas());

            // 4. Guardar el objeto de configuración completo en el mapa del modelo.
            String claveImagen = item.getRutaImagen().toString().replace("\\", "/");
            exportConfigsMap.put(claveImagen, config);
            contador++;
        }

        logger.info(
                "[ProjectController] Sincronización de configuración de exportación completada. Se persistirán {} entradas.",
                contador);
    } // --- Fin del metodo: sincronizarArchivosAsociadosConModelo ---


    // Carga los metadatos (nombre, descripción) desde el ProjectModel
    private void actualizarPanelDePropiedadesEnUI() {
        if (projectManager == null || registry == null)
            return;

        vista.panels.export.ProjectMetadataPanel propsPanel = registry.get("panel.proyecto.propiedades");
        ProjectModel currentProject = projectManager.getCurrentProject();

        if (propsPanel != null && currentProject != null) {
            String name = projectManager.getNombreProyectoActivo(); // Usamos el método que ya es inteligente
            if (name.toLowerCase().endsWith(".prj")) {
                name = name.substring(0, name.lastIndexOf('.'));
            }

            String description = currentProject.getProjectDescription() != null ? currentProject.getProjectDescription()
                    : "";

            propsPanel.getProjectNameLabel().setText(name);
            propsPanel.getProjectDescriptionArea().setText(description);
            logger.debug("Panel de propiedades en la UI actualizado con los datos del modelo.");
        }
    } // --- Fin del metodo: actualizarPanelDePropiedadesEnUI ---


    // Sincroniza la descripción del proyecto desde el campo de texto de la UI
    public void sincronizarDescripcionDesdeUI() {
        if (projectManager == null || registry == null)
            return;

        vista.panels.export.ProjectMetadataPanel propsPanel = registry.get("panel.proyecto.propiedades");
        ProjectModel currentProject = projectManager.getCurrentProject();

        if (propsPanel != null && currentProject != null) {
            currentProject.setProjectDescription(propsPanel.getProjectDescriptionArea().getText());
            logger.debug("Descripción del ProjectModel sincronizada desde la UI.");
        }
    } // --- Fin del metodo: sincronizarDescripcionDesdeUI ---


    // Actualiza los títulos de los paneles "Selección Actual" y "Descartes"
    public void actualizarContadoresDeTitulos() {
        if (registry == null || generalController == null || generalController.getVisorController() == null
                || generalController.getVisorController().getThemeManager() == null)
            return;

        JPanel panelSeleccion = registry.get("panel.proyecto.seleccion.container");
        JPanel panelDescartes = registry.get("panel.proyecto.descartes.container");
        java.awt.Color titleColor = generalController.getVisorController().getThemeManager().getTemaActual()
                .colorBordeTitulo();

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
    } // --- Fin del metodo: actualizarContadoresDeTitulos ---


    // ********************************************************************************************
    // ********************************************************** METODOS DE LA
    // TOOLBAR DE PROYECTO
    // ********************************************************************************************

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


    private void actualizarAparienciaListasPorFoco() {
        if (registry == null || model == null || generalController == null
                || generalController.getVisorController() == null
                || generalController.getVisorController().getThemeManager() == null)
            return;
        JList<String> projectList = registry.get("list.proyecto.nombres");
        JList<String> descartesList = registry.get("list.proyecto.descartes");
        if (projectList == null || descartesList == null)
            return;
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
    } // --- Fin del metodo: actualizarAparienciaListasPorFoco ---


    // Crea un MouseListener que muestra un menú contextual en un JComponent
    private java.awt.event.MouseAdapter createContextMenuListener(javax.swing.JComponent component,
            Object... menuItems) {
        return new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger())
                    showMenu(e);
            }


            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger())
                    showMenu(e);
            }


            private void showMenu(java.awt.event.MouseEvent e) {
                logger.debug("[ContextMenuListener] Evento de popup detectado en el componente: {}",
                        component.getClass().getSimpleName());

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

                        // Si la acción es sensible al contexto, le pedimos que se actualice ahora
                        // mismo.
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
    } // --- Fin del metodo: createContextMenuListener ---


    // Construye y devuelve un JPopupMenu dinámico para el visor principal (Single o
    public JPopupMenu crearMenuContextualVisorManualmente() {
        logger.debug("[MenuContextualVisor] Creando menú manualmente para el estado de vista: {} y modo display: {}",
                currentViewState, model.getCurrentDisplayMode());

        JPopupMenu menu = new JPopupMenu();
        boolean isGridMode = (model.getCurrentDisplayMode() == VisorModel.DisplayMode.GRID);
        boolean hasSelection = model.getSelectedImageKey() != null && !model.getSelectedImageKey().isEmpty();

        // Paso 1: Añadir acciones basadas en el contexto de la selección (Mover, Localizar, etc.)
        if (hasSelection) {
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
        }

        // Paso 2: Añadir acciones de Paneo/Zoom (solo si no es Grid)
        Action toggleZoomAction = actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
        if (toggleZoomAction != null) {
            JCheckBoxMenuItem toggleZoomItem = new JCheckBoxMenuItem(toggleZoomAction);
            toggleZoomItem.setSelected(model.isZoomHabilitado());
            toggleZoomItem.setEnabled(!isGridMode);
            menu.add(toggleZoomItem);
        }

        Action resetZoomAction = actionMap.get(AppActionCommands.CMD_ZOOM_RESET);
        if (resetZoomAction != null) {
            JMenuItem resetZoomItem = new JMenuItem(resetZoomAction);
            resetZoomItem.setEnabled(!isGridMode);
            menu.add(resetZoomItem);
        }

        menu.addSeparator();

        // Paso 3: Acción global: Añadir archivos (SIEMPRE disponible al final)
        menu.add(actionMap.get(AppActionCommands.CMD_PROYECTO_ANADIR_ARCHIVOS));

        return menu;
    } // --- Fin del metodo: crearMenuContextualVisorManualmente ---


    // El método central para refrescar TODA la UI del modo proyecto
    public void refrescarVistaProyectoCompleta() {
        logger.info("[ProjectController] Iniciando refresco completo de la vista del proyecto...");

        autoRelocalizarImagenesHuerfanas(); // Auto-curar rutas antes de poblar la interfaz

        poblarListasSeleccionYDescartes();

        if (isExportPanelVisible()) {
            logger.debug(" -> Panel de exportación visible. Preparando nueva cola...");
            solicitarPreparacionColaExportacion();
        }

        actualizarModeloPrincipalConListaDeProyectoActiva();
        sincronizarSeleccionEnGridProyecto();

        refrescarGridProyecto(); // Asegura que el repintado ocurra siempre.

        logger.info("[ProjectController] Refresco completo de la vista del proyecto finalizado.");
    } // --- Fin del metodo: refrescarVistaProyectoCompleta ---


    // Configura el menú contextual para la tabla de exportación
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
        for (java.awt.event.MouseListener ml : tablaExportacion.getMouseListeners()) {
            if (ml.getClass().getName().contains("ContextMenuListener")) { // Una forma de identificar nuestro listener
                tablaExportacion.removeMouseListener(ml);
            }
        }

        // Obtenemos las acciones que queremos en el menú
        Action quitarAction = actionMap.get(AppActionCommands.CMD_EXPORT_QUITAR_DE_COLA);
        Action asignarAction = actionMap.get(AppActionCommands.CMD_EXPORT_ASIGNAR_ARCHIVO);
        Action ignorarAction = actionMap.get(AppActionCommands.CMD_EXPORT_IGNORAR_COMPRIMIDO);
        Action relocalizarAction = actionMap.get(AppActionCommands.CMD_EXPORT_RELOCALIZAR_IMAGEN);
        Action limpiarHuerfanosAction = actionMap.get(AppActionCommands.CMD_EXPORT_LIMPIAR_NO_ENCONTRADOS);
        Action abrirUbicacionAction = actionMap.get(AppActionCommands.CMD_EXPORT_ABRIR_UBICACION);

        // Creamos el listener usando el método helper
        java.awt.event.MouseAdapter contextMenuListener = createContextMenuListener(tablaExportacion,
                asignarAction,
                quitarAction,
                new javax.swing.JPopupMenu.Separator(),
                ignorarAction,
                relocalizarAction,
                limpiarHuerfanosAction,
                new javax.swing.JPopupMenu.Separator(),
                abrirUbicacionAction);

        // Asignamos el listener a la tabla
        tablaExportacion.addMouseListener(contextMenuListener);
        logger.debug("[DIAGNÓSTICO] Menú contextual configurado y listener añadido a la tabla de exportación.");
    } // --- Fin del metodo: configurarContextMenuTablaExportacion ---


    public void solicitarRelocalizacionImagen() {
        if (registry == null || exportQueueManager == null || view == null)
            return;
        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1)
            return;
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
            Path oldPath = item.getRutaImagen();

            fileManagementService.relocalizarImagenEnCola(item, nuevaRuta, filaSeleccionada);
            fileManagementService.migrarClaveEnModelo(projectManager.getCurrentProject(), oldPath, nuevaRuta);
            projectManager.notificarModificacion();

            refrescarVistaProyectoCompleta();
            logger.info("Imagen relocalizada manualmente con éxito de: {} -> {}", oldPath, nuevaRuta);
        }
    } // --- Fin del metodo: solicitarRelocalizacionImagen ---


    // Orquesta el movimiento de una imagen de la lista de selección a la de
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
    } // --- Fin del metodo: solicitarMoverSeleccionadoAdescartes ---


    public void autoRelocalizarImagenesHuerfanas() {
        if (generalController == null) return;
        DataManager dm = generalController.getDataController() != null
                ? generalController.getDataController().getDataManager() : null;
        if (dm == null) return;
        integrityService.autoRelocalizarImagenesHuerfanas(dm);
    } // --- Fin del metodo: autoRelocalizarImagenesHuerfanas ---

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
    }// --- Fin del metodo: navegarTablaExportacionConRueda ---

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

    } // --- Fin del metodo: mostrarImagenDeExportacion ---


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


    private void refrescarGridProyecto() {
        JList<String> gridList = registry.get("list.grid.proyecto");
        if (gridList != null) {
            gridList.revalidate();
            gridList.repaint();
        }
    } // --- Fin del metodo: refrescarGridProyecto ---


    // Busca los renderers de las listas de proyecto en el registro y limpia su
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
        if (listaDescartes != null
                && listaDescartes.getCellRenderer() instanceof vista.renderers.ProjectListCellRenderer) {
            ((vista.renderers.ProjectListCellRenderer) listaDescartes.getCellRenderer()).clearCache();
        }

        logger.debug("[ProjectController] Caché de los renderers de listas de proyecto limpiado.");
    } // --- Fin del metodo: limpiarCacheRenderersProyecto ---


    public JTable getTablaExportacionDesdeRegistro() {
        if (registry == null)
            return null;

        // Usamos la clave correcta con la que registramos el panel en ProjectBuilder.
        vista.panels.export.ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");

        if (exportPanel != null) {
            return exportPanel.getTablaExportacion();
        }

        // Este log ahora nos ayudará a depurar si vuelve a fallar.
        logger.warn(
                "WARN [ProjectController]: No se pudo encontrar 'ExportPanel' en el registro con la clave 'panel.proyecto.exportacion.completo'.");
        return null;
    } // --- Fin del metodo: getTablaExportacionDesdeRegistro ---


    // Comprueba si el panel de herramientas de la derecha (que contiene la
    public boolean isExportPanelVisible() {
        if (registry == null)
            return false;
        JPanel toolsPanel = registry.get("panel.proyecto.herramientas.container");
        return toolsPanel != null && toolsPanel.isVisible();
    } // --- Fin del metodo: isExportPanelVisible ---


    // Establece la visibilidad del panel de herramientas de exportación sin alterar
    public void setExportPanelVisible(boolean visible) {
        if (registry == null)
            return;
        JSplitPane rightSplit = registry.get("splitpane.proyecto.right");
        JPanel toolsPanel = registry.get("panel.proyecto.herramientas.container");
        if (rightSplit == null || toolsPanel == null)
            return;

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
    } // --- Fin del metodo: setExportPanelVisible ---


    // Calcula y ajusta la posición del divisor del split pane derecho (vertical)
    public void ajustarPosicionDivisorDerecho() {
        if (registry == null)
            return;

        JSplitPane rightSplit = registry.get("splitpane.proyecto.right");
        vista.panels.GridDisplayPanel gridPanel = registry.get("panel.display.grid.proyecto");

        if (rightSplit != null && gridPanel != null && rightSplit.isVisible()) {
            SwingUtilities.invokeLater(() -> {
                int cellHeight = gridPanel.getGridList().getFixedCellHeight();
                if (cellHeight <= 0)
                    cellHeight = 132;

                int desiredHeight = (int) (cellHeight * 1.5) + 15;

                // Asegurarnos de no poner el divisor en una posición inválida
                int maxLocation = rightSplit.getHeight() - rightSplit.getDividerSize() - 50; // Dejar un mínimo para el
                                                                                             // panel inferior
                desiredHeight = Math.min(desiredHeight, maxLocation);

                rightSplit.setDividerLocation(desiredHeight);
                logger.debug("Posición del divisor derecho ajustada a {}px.", desiredHeight);
            });
        }
    } // --- Fin del metodo: ajustarPosicionDivisorDerecho ---


    public void setProjectListCoordinator(ProjectListCoordinator coordinator) {
        this.projectListCoordinator = coordinator;
        if (this.projectListCoordinator != null) {
            this.projectListCoordinator.setProjectController(this);
        }
    }


    public void setGeneralController(GeneralController generalController) {
        this.generalController = Objects.requireNonNull(generalController,
                "GeneralController no puede ser null en ProjectController");
    }


    public void setProjectManager(IProjectManager projectManager) {
        this.projectManager = Objects.requireNonNull(projectManager);
        this.fileManagementService = new ProjectFileManagementService(this.projectManager, this.exportQueueManager);
        this.integrityService = new ProjectIntegrityService(this.projectManager);
    }


    public void setRegistry(ComponentRegistry registry) {
        this.registry = Objects.requireNonNull(registry);
    }


    public void setZoomManager(IZoomManager zoomManager) {
        this.zoomManager = Objects.requireNonNull(zoomManager);
    }


    public void setView(VisorView view) {
        this.view = Objects.requireNonNull(view);
    }


    public void setActionMap(Map<String, Action> actionMap) {
        this.actionMap = Objects.requireNonNull(actionMap);
    }


    public void setModel(VisorModel model) {
        this.model = Objects.requireNonNull(model);
    }


    public void setDisplayModeManager(DisplayModeManager displayModeManager) {
        this.displayModeManager = displayModeManager;
    }


    public IProjectManager getProjectManager() {
        return this.projectManager;
    }


    public ProjectListCoordinator getProjectListCoordinator() {
        return this.projectListCoordinator;
    }


    public VisorView getView() {
        return this.view;
    }


    public ComponentRegistry getRegistry() {
        return this.registry;
    }


    public GeneralController getGeneralController() {
        return this.generalController;
    }


    public Map<String, Action> getActionMap() {
        return this.actionMap;
    }

} // --- FIN de la clase ProjectController ---
