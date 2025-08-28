package controlador;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import vista.VisorView;
import vista.dialogos.TaskProgressDialog;
import vista.panels.export.ExportTableModel;
	
public class ProjectController implements IModoController {

	private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
	
    private IProjectManager projectManager;
    private ComponentRegistry registry;
    private IZoomManager zoomManager;
    private VisorView view;
    private VisorModel model;
    private VisorController controllerRef;
    private ExportQueueManager exportQueueManager;
    private ProjectListCoordinator projectListCoordinator;
    private DisplayModeManager displayModeManager;
    
    private IViewManager viewManager;
    private IListCoordinator listCoordinator; 
    private Map<String, Action> actionMap;
    
    private boolean enModoVistaExportacion = false;
    private VisorModel.DisplayMode modoVistaAnterior;
    private String nombreListaActivaAnterior;
    private Map<String, ExportItem> exportItemMap = new HashMap<>();

    public ProjectController() {
        logger.debug("[ProjectController] Instancia creada.");
        this.exportQueueManager = new ExportQueueManager();
    } // --- Fin del método ProjectController (constructor) ---

    
    void configurarListeners() {
        if (registry == null || model == null || projectListCoordinator == null) {
            logger.error("ERROR [ProjectController]: Dependencias nulas (registry, model o projectListCoordinator).");
            return;
        }

        // --- Listeners para las listas de Selección y Descartes ---
        JList<String> projectList = registry.get("list.proyecto.nombres");
        JList<String> descartesList = registry.get("list.proyecto.descartes");

        MouseAdapter listMouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JList<?> sourceList = (JList<?>) e.getSource();
                String nombreListaClicada = (sourceList == projectList) ? "seleccion" : "descartes";
                if (!nombreListaClicada.equals(model.getProyectoListContext().getNombreListaActiva())) {
                    cambiarFocoListaActiva(nombreListaClicada);
                }
            }
        };

        if (projectList != null) {
            projectList.addMouseListener(listMouseAdapter);
            for (javax.swing.event.ListSelectionListener lsl : projectList.getListSelectionListeners()) {
                projectList.removeListSelectionListener(lsl);
            }
            projectList.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting() || projectListCoordinator.isSincronizandoUI()) return;
                
                if ("seleccion".equals(model.getProyectoListContext().getNombreListaActiva())) {
                    projectListCoordinator.seleccionarImagenPorIndice(projectList.getSelectedIndex());
                }
            });
        }

        if (descartesList != null) {
            descartesList.addMouseListener(listMouseAdapter);
            for (javax.swing.event.ListSelectionListener lsl : descartesList.getListSelectionListeners()) {
                descartesList.removeListSelectionListener(lsl);
            }
            descartesList.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting() || projectListCoordinator.isSincronizandoUI()) return;

                if ("descartes".equals(model.getProyectoListContext().getNombreListaActiva())) {
                    projectListCoordinator.seleccionarImagenPorIndice(descartesList.getSelectedIndex());
                }
            });
        }
        
        vista.panels.export.ExportPanel panelExportar = registry.get("panel.proyecto.herramientas.exportar");
        if(panelExportar != null) {
            JButton btnSeleccionarCarpeta = panelExportar.getBotonSeleccionarCarpeta();
            if (btnSeleccionarCarpeta != null) {
                btnSeleccionarCarpeta.addActionListener(e -> solicitarSeleccionCarpetaDestino());
            }
        }
        
        JTabbedPane herramientasTabbedPane = registry.get("tabbedpane.proyecto.herramientas");
        if (herramientasTabbedPane != null) {
            herramientasTabbedPane.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    int selectedIndex = herramientasTabbedPane.getSelectedIndex();
                    String selectedTitle = herramientasTabbedPane.getTitleAt(selectedIndex);

                    if ("Exportar".equals(selectedTitle)) {
                        activarModoVistaExportacion();
                    } else {
                        if (enModoVistaExportacion) {
                            desactivarModoVistaExportacion();
                        }
                    }
                }
            });
        } else {
            logger.error("No se encontró 'tabbedpane.proyecto.herramientas' en el ComponentRegistry. La lógica de vista de exportación no funcionará.");
        }

    } // --- Fin del método configurarListeners ---
    
    // =========================================================================
    // === INICIO DE MODIFICACIÓN ===
    // =========================================================================
    /**
     * Activa la vista especializada para la pestaña "Exportar".
     * Cambia el visor a modo GRID y utiliza la lista de exportación como fuente de datos.
     */
    private void activarModoVistaExportacion() {
        if (enModoVistaExportacion) return; // Ya estamos en este modo
        logger.info("Activando modo de vista de Exportación...");
        
        this.enModoVistaExportacion = true;
        
        // 1. Guardar estado actual
        this.modoVistaAnterior = model.getCurrentDisplayMode();
        this.nombreListaActivaAnterior = model.getProyectoListContext().getNombreListaActiva();
        logger.debug(" -> Estado guardado: DisplayMode={}, ListaActiva={}", modoVistaAnterior, nombreListaActivaAnterior);
        
        // 2. Forzar modo GRID. El otro listener (en ProjectBuilder) se encargará de poblarlo con datos.
        displayModeManager.switchToDisplayMode(VisorModel.DisplayMode.GRID);
        
    } // ---FIN de metodo ---
    // =========================================================================
    // === FIN DE MODIFICACIÓN ===
    // =========================================================================

    /**
     * Desactiva la vista especializada de "Exportar" y restaura el estado anterior.
     * Vuelve al modo de vista (SINGLE/GRID) y a la lista de datos (selección/descartes) previos.
     */
    private void desactivarModoVistaExportacion() {
        if (!enModoVistaExportacion) return; // No estábamos en este modo
        logger.info("Desactivando modo de vista de Exportación...");

        this.enModoVistaExportacion = false;
        
        // 1. Restaurar el foco a la lista que estaba activa
        model.getProyectoListContext().setNombreListaActiva(this.nombreListaActivaAnterior);
        
        // 2. Recargar el modelo principal con los datos de la lista de proyecto activa
        actualizarModeloPrincipalConListaDeProyectoActiva();
        
        // 3. Restaurar el modo de display (SINGLE/GRID) que estaba antes
        if (this.modoVistaAnterior != null) {
            displayModeManager.switchToDisplayMode(this.modoVistaAnterior);
        }
        
        logger.info("Modo de vista de Exportación desactivado. Estado restaurado a: DisplayMode={}, ListaActiva={}", this.modoVistaAnterior, this.nombreListaActivaAnterior);
        
        this.modoVistaAnterior = null;
        this.nombreListaActivaAnterior = null;
        this.exportItemMap.clear();
    } // ---FIN de metodo ---

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
    } // ---FIN de metodo ---
    
    /**
     * Devuelve el estado de un item de exportación basado en su clave de ruta.
     * @param clave La ruta de la imagen como String.
     * @return El ExportStatus, o null si no se encuentra.
     */
    public ExportItem getExportItem(String clave) {
        if (!enModoVistaExportacion || clave == null) {
            return null;
        }
        return exportItemMap.get(clave);
    } // ---FIN de metodo ---
    
    // =========================================================================
    // === INICIO DE MODIFICACIÓN ===
    // =========================================================================
    /**
     * Establece la lista de la cola de exportación como la lista maestra actual,
     * provocando que el grid se actualice con su contenido.
     */
    public void setExportListAsMasterList() {
        logger.debug("[ProjectController] Estableciendo la lista de exportación como lista maestra temporal...");
        
        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || !(tablaExportacion.getModel() instanceof ExportTableModel)) {
            logger.error("Tabla de exportación o su modelo no son válidos.");
            return;
        }
        
        ExportTableModel exportModel = (ExportTableModel) tablaExportacion.getModel();
        List<ExportItem> items = exportModel.getCola();
    
        actualizarMapaDeItemsExportacion(items);
    
        DefaultListModel<String> modeloExportacion = new DefaultListModel<>();
        Map<String, Path> mapaRutasExportacion = new HashMap<>();
        
        for (ExportItem item : items) {
            Path ruta = item.getRutaImagen();
            if (ruta != null) {
                String clave = ruta.toString().replace("\\", "/");
                modeloExportacion.addElement(clave);
                mapaRutasExportacion.put(clave, ruta);
            }
        }
    
        model.setMasterListAndNotify(modeloExportacion, mapaRutasExportacion, this);
        logger.debug(" -> Lista maestra actualizada con {} items de exportación.", modeloExportacion.getSize());
    } // ---FIN de metodo ---
    // =========================================================================
    // === FIN DE MODIFICACIÓN ===
    // =========================================================================

    public void actualizarModeloPrincipalConListaDeProyectoActiva() {
        if (model == null || registry == null || controllerRef == null) {
            logger.warn("WARN [actualizarModeloPrincipalConListaDeProyectoActiva]: Dependencias nulas. No se puede actualizar.");
            return;
        }
        
        if (enModoVistaExportacion) {
            logger.trace("Saltando actualización de modelo principal porque estamos en modo vista de exportación.");
            return;
        }

        String nombreListaActiva = model.getProyectoListContext().getNombreListaActiva();
        JList<String> listaActivaUI = null;
        if ("seleccion".equals(nombreListaActiva)) {
            listaActivaUI = registry.get("list.proyecto.nombres");
        } else if ("descartes".equals(nombreListaActiva)) {
            listaActivaUI = registry.get("list.proyecto.descartes");
        }

        if (listaActivaUI == null || listaActivaUI.getModel() == null) {
            logger.error("ERROR: No se pudo encontrar la JList activa o su modelo para '" + nombreListaActiva + "'");
            return;
        }

        DefaultListModel<String> modeloDeListaActiva = (DefaultListModel<String>) listaActivaUI.getModel();
        Map<String, Path> mapaRutasCompleto = model.getProyectoListContext().getRutaCompletaMap();

        logger.debug("[ProjectController] Estableciendo lista maestra del modelo con el contenido de la lista '{}' ({} elementos).", nombreListaActiva, modeloDeListaActiva.getSize());
        model.setMasterListAndNotify(modeloDeListaActiva, mapaRutasCompleto, this);
        
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
        logger.debug("  [ProjectController] Cambiando foco a lista: " + nuevoFoco);
        model.getProyectoListContext().setNombreListaActiva(nuevoFoco);
        
        actualizarModeloPrincipalConListaDeProyectoActiva();
        
        String claveARestaurar;
        JList<String> listaQueGanaFoco;
        
        if ("seleccion".equals(nuevoFoco)) {
            claveARestaurar = model.getProyectoListContext().getSeleccionListKey();
            listaQueGanaFoco = registry.get("list.proyecto.nombres");
        } else {
            claveARestaurar = model.getProyectoListContext().getDescartesListKey();
            listaQueGanaFoco = registry.get("list.proyecto.descartes");
        }

        if (claveARestaurar == null && listaQueGanaFoco != null && listaQueGanaFoco.getModel().getSize() > 0) {
            claveARestaurar = listaQueGanaFoco.getModel().getElementAt(0);
        }
        
        projectListCoordinator.seleccionarImagenPorClave(claveARestaurar);
        
        actualizarAparienciaListasPorFoco();
        
        DisplayModeManager dmm = controllerRef.getDisplayModeManager();

        if (dmm != null) {
            VisorModel.DisplayMode modoGuardado = model.getCurrentDisplayMode();
            logger.debug("  -> Restaurando DisplayMode para la lista '" + nuevoFoco + "' a: " + modoGuardado);
            dmm.switchToDisplayMode(modoGuardado);
        } else {
            logger.error("ERROR: DisplayModeManager es nulo. No se puede restaurar la vista.");
        }

    } // --- Fin del método cambiarFocoListaActiva ---
    
    
    public boolean prepararDatosProyecto() {
        logger.debug("  [ProjectController] Preparando datos para el modo proyecto...");
        if (projectManager == null || model == null) {
            logger.error("ERROR CRÍTICO [prepararDatosProyecto]: ProjectManager o Model nulos.");
            return false;
        }

        List<java.nio.file.Path> imagenesMarcadas = projectManager.getImagenesMarcadas();
        List<java.nio.file.Path> imagenesDescartadas = projectManager.getImagenesDescartadas();
        
        List<java.nio.file.Path> todasLasImagenes = new java.util.ArrayList<>();
        todasLasImagenes.addAll(imagenesMarcadas);
        todasLasImagenes.addAll(imagenesDescartadas);
        
        todasLasImagenes = todasLasImagenes.stream().distinct().collect(Collectors.toList());
        
        todasLasImagenes.sort((p1, p2) -> p1.toString().compareToIgnoreCase(p2.toString()));

        if (todasLasImagenes.isEmpty()) {
            JOptionPane.showMessageDialog(view, "No hay imágenes marcadas ni descartadas en el proyecto actual.", "Proyecto Vacío", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        DefaultListModel<String> modeloUnificado = new DefaultListModel<>();
        Map<String, Path> mapaRutasProyecto = new HashMap<>();
        
        for (java.nio.file.Path rutaAbsoluta : todasLasImagenes) {
            String clave = rutaAbsoluta.toString().replace("\\", "/");
            modeloUnificado.addElement(clave);
            mapaRutasProyecto.put(clave, rutaAbsoluta);
        }

        ListContext proyectoContext = model.getProyectoListContext();
        proyectoContext.actualizarContextoCompleto(modeloUnificado, mapaRutasProyecto);
        
        logger.debug("    -> Datos del proyecto preparados en proyectoListContext. Total de imágenes (selección + descartes): " + modeloUnificado.getSize());
        return true;
    } // --- Fin del método prepararDatosProyecto ---

    
    public void activarVistaProyecto() {
        logger.debug("  [ProjectController] Activando la UI de la vista de proyecto (Lógica Directa)...");
        if (registry == null || model == null || projectManager == null || projectListCoordinator == null) {
            logger.error("ERROR [ProjectController.activarVistaProyecto]: Dependencias nulas.");
            return;
        }

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

        String claveParaMostrar = null;
        String focoActual = model.getProyectoListContext().getNombreListaActiva();
        JList<String> descartesList = registry.get("list.proyecto.descartes");
        
        if ("descartes".equals(focoActual)) {
            claveParaMostrar = model.getProyectoListContext().getDescartesListKey();
        } else {
            claveParaMostrar = model.getProyectoListContext().getSeleccionListKey();
        }

        if (claveParaMostrar == null || !model.getProyectoListContext().getModeloLista().contains(claveParaMostrar)) {
            if ("descartes".equals(focoActual) && descartesList != null && descartesList.getModel().getSize() > 0) {
                claveParaMostrar = descartesList.getModel().getElementAt(0);
            } else if (projectList != null && projectList.getModel().getSize() > 0) {
                claveParaMostrar = projectList.getModel().getElementAt(0);
                model.getProyectoListContext().setNombreListaActiva("seleccion");
            }
        }
        
        projectListCoordinator.seleccionarImagenPorClave(claveParaMostrar);

        actualizarModeloPrincipalConListaDeProyectoActiva();
        
        SwingUtilities.invokeLater(() -> {
            actualizarAparienciaListasPorFoco();
            final JSplitPane leftSplit = registry.get("splitpane.proyecto.left");
            if (leftSplit != null) leftSplit.setDividerLocation(0.55);
            final JSplitPane mainSplit = registry.get("splitpane.proyecto.main");
            if (mainSplit != null) mainSplit.setDividerLocation(0.25);
            
            logger.debug("  -> Solicitando foco para la lista activa del proyecto...");
            
            String listaActiva = model.getProyectoListContext().getNombreListaActiva();
            JList<String> listaParaEnfocar = null;

            if ("seleccion".equals(listaActiva)) {
                listaParaEnfocar = registry.get("list.proyecto.nombres");
            } else if ("descartes".equals(listaActiva)) {
                listaParaEnfocar = registry.get("list.proyecto.descartes");
            }

            if (listaParaEnfocar != null) {
                boolean focusRequested = listaParaEnfocar.requestFocusInWindow();
                logger.debug("    -> Foco solicitado para '" + listaActiva + "'. ¿Éxito?: " + focusRequested);
            } else {
                logger.warn("    -> No se pudo encontrar la JList activa ('" + listaActiva + "') para darle el foco.");
            }

            logger.debug("  [ProjectController] UI de la vista de proyecto activada y restaurada.");
        });
        
    } // --- Fin del método activarVistaProyecto ---
    
    
    @Override
    public void navegarSiguiente() {
        if (projectListCoordinator != null) {
            projectListCoordinator.seleccionarSiguiente();
        }
    } // ---FIN de metodo ---
    
    @Override
    public void navegarAnterior() {
        if (projectListCoordinator != null) projectListCoordinator.seleccionarAnterior();
    } // ---FIN de metodo ---

    @Override
    public void navegarPrimero() {
        if (projectListCoordinator != null) projectListCoordinator.seleccionarPrimero();
    } // ---FIN de metodo ---

    @Override
    public void navegarUltimo() {
        if (projectListCoordinator != null) projectListCoordinator.seleccionarUltimo();
    } // ---FIN de metodo ---

    @Override
    public void navegarBloqueSiguiente() {
        if (projectListCoordinator != null) projectListCoordinator.seleccionarBloqueSiguiente();
    } // ---FIN de metodo ---

    @Override
    public void navegarBloqueAnterior() {
        if (projectListCoordinator != null) projectListCoordinator.seleccionarBloqueAnterior();
    } // ---FIN de metodo ---

    @Override
    public void aplicarZoomConRueda(java.awt.event.MouseWheelEvent e) {
        if (zoomManager != null) {
            zoomManager.aplicarZoomConRueda(e);
            if (controllerRef != null) {
                controllerRef.sincronizarEstadoVisualBotonesYRadiosZoom();
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
        if (model == null || projectManager == null) return;
        String claveSeleccionada = model.getSelectedImageKey();
        if (claveSeleccionada == null || claveSeleccionada.isEmpty()) {
            logger.debug("  [ProjectController] No hay imagen seleccionada para mover a descartes.");
            return;
        }
        Path rutaAbsoluta = model.getProyectoListContext().getRutaCompleta(claveSeleccionada);
        if (rutaAbsoluta != null) {
            projectManager.moverAdescartes(rutaAbsoluta);
            refrescarListasDeProyecto();
        }
    } // --- Fin del método moverSeleccionActualADescartes ---

    public void restaurarDesdeDescartes() {
        if (registry == null || projectManager == null) return;
        JList<String> listaDescartesUI = registry.get("list.proyecto.descartes");
        if (listaDescartesUI == null) return;
        String claveSeleccionada = listaDescartesUI.getSelectedValue();
        if (claveSeleccionada == null || claveSeleccionada.isEmpty()) {
            logger.debug("  [ProjectController] No hay imagen seleccionada en descartes para restaurar.");
            return;
        }
        Path rutaAbsoluta = java.nio.file.Paths.get(claveSeleccionada);
        projectManager.restaurarDeDescartes(rutaAbsoluta);
        refrescarListasDeProyecto();
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
        exportQueueManager.prepararColaDesdeSeleccion(seleccionActual);
        
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
            JPanel exportPanelPlaceholder = registry.get("panel.proyecto.herramientas.exportar");
            if (exportPanelPlaceholder instanceof vista.panels.export.ExportPanel) {
                vista.panels.export.ExportPanel exportPanel = (vista.panels.export.ExportPanel) exportPanelPlaceholder;
                exportPanel.setRutaDestino(carpetaSeleccionada.toString());
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
        if (registry == null || exportQueueManager == null) return;
        
        actualizarMapaDeItemsExportacion(exportQueueManager.getColaDeExportacion());

        vista.panels.export.ExportPanel exportPanel = (vista.panels.export.ExportPanel) registry.get("panel.proyecto.herramientas.exportar");
        if (exportPanel == null) return;
        java.util.List<modelo.proyecto.ExportItem> colaCompleta = exportQueueManager.getColaDeExportacion();
        List<modelo.proyecto.ExportItem> itemsSeleccionadosParaExportar = colaCompleta.stream().filter(modelo.proyecto.ExportItem::isSeleccionadoParaExportar).collect(Collectors.toList());
        boolean todosLosSeleccionadosEstanListos = itemsSeleccionadosParaExportar.stream().allMatch(item ->
            item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.ENCONTRADO_OK ||
            item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.ASIGNADO_MANUAL ||
            item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO
        );
        String rutaDestino = exportPanel.getRutaDestino();
        boolean carpetaOk = rutaDestino != null && !rutaDestino.isBlank() && !rutaDestino.equals("Seleccione una carpeta de destino...");
        boolean puedeExportar = carpetaOk && !itemsSeleccionadosParaExportar.isEmpty() && todosLosSeleccionadosEstanListos;
        String mensaje = itemsSeleccionadosParaExportar.size() + " de " + colaCompleta.size() + " archivos a exportar.";
        exportPanel.actualizarEstadoControles(puedeExportar, mensaje);
        boolean resaltarDestino = !colaCompleta.isEmpty() && !carpetaOk;
        exportPanel.resaltarRutaDestino(resaltarDestino);
        logger.debug("  [ProjectController] Estado de exportación UI actualizado. Puede exportar: " + puedeExportar);
    } // --- Fin del método actualizarEstadoExportacionUI ---
    
    public void solicitarInicioExportacion() {
        if (exportQueueManager == null || registry == null || view == null) {
            logger.error("ERROR [solicitarInicioExportacion]: Dependencias nulas.");
            return;
        }
        vista.panels.export.ExportPanel exportPanel = (vista.panels.export.ExportPanel) registry.get("panel.proyecto.herramientas.exportar");
        Path carpetaDestino = java.nio.file.Paths.get(exportPanel.getRutaDestino());
        List<modelo.proyecto.ExportItem> colaParaCopiar = exportQueueManager.getColaDeExportacion().stream()
            .filter(modelo.proyecto.ExportItem::isSeleccionadoParaExportar)
            .filter(item -> 
                item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.ENCONTRADO_OK ||
                item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.ASIGNADO_MANUAL ||
                item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO)
            .collect(Collectors.toList());
        if (colaParaCopiar.isEmpty()) {
            JOptionPane.showMessageDialog(view, "No hay archivos válidos seleccionados en la cola para exportar.", "Exportación Vacía", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
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
            actualizarEstadoExportacionUI();
        }
    } // --- Fin del método solicitarAlternarIgnorarComprimido ---
    
    public void solicitarAsignacionManual() {
        if (registry == null) return;
        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) return;
        int filaSeleccionada = tablaExportacion.getSelectedRow();
        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
        modelo.proyecto.ExportItem item = modelTabla.getItemAt(filaSeleccionada);
        if (item == null) return;
        JFileChooser fileChooser = new JFileChooser();
        Path carpetaImagen = item.getRutaImagen().getParent();
        if (carpetaImagen != null && Files.isDirectory(carpetaImagen)) {
            fileChooser.setCurrentDirectory(carpetaImagen.toFile());
        }
        fileChooser.setDialogTitle("Localizar Archivo para " + item.getRutaImagen().getFileName());
        int result = fileChooser.showOpenDialog(view);
        if (result == JFileChooser.APPROVE_OPTION) {
            Path selectedPath = fileChooser.getSelectedFile().toPath();
            item.setRutaArchivoComprimido(selectedPath);
            item.setEstadoArchivoComprimido(modelo.proyecto.ExportStatus.ASIGNADO_MANUAL);
            modelTabla.fireTableRowsUpdated(filaSeleccionada, filaSeleccionada);
            actualizarEstadoExportacionUI();
        }
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
    } // --- Fin del método solicitudAlternarMarcaImagen ---
    
    
    public void solicitarEliminacionPermanente() {
        if (registry == null || projectManager == null || view == null) {
            logger.warn("WARN [solicitarEliminacionPermanente]: Dependencias nulas.");
            return;
        }
        JList<String> listaDescartesUI = registry.get("list.proyecto.descartes");
        if (listaDescartesUI == null) {
            logger.warn("WARN [solicitarEliminacionPermanente]: JList 'list.proyecto.descartes' no encontrada.");
            return;
        }
        String claveSeleccionada = listaDescartesUI.getSelectedValue();
        if (claveSeleccionada == null || claveSeleccionada.isEmpty()) {
            logger.debug("  [ProjectController] No hay imagen seleccionada en descartes para eliminar.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(view, "¿Seguro que quieres eliminar esta imagen del proyecto?\n(No se borrará el archivo del disco)", "Confirmar Eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            Path rutaAbsoluta = java.nio.file.Paths.get(claveSeleccionada);
            projectManager.eliminarDeProyecto(rutaAbsoluta);
            refrescarListasDeProyecto();
        }
    } // --- Fin del método solicitarEliminacionPermanente ---
    
        
    private void actualizarAparienciaListasPorFoco() {
        if (registry == null || model == null || controllerRef.getThemeManager() == null) return;
        JList<String> projectList = registry.get("list.proyecto.nombres");
        JList<String> descartesList = registry.get("list.proyecto.descartes");
        if (projectList == null || descartesList == null) return;
        String focoActivo = model.getProyectoListContext().getNombreListaActiva();
        vista.theme.Tema tema = controllerRef.getThemeManager().getTemaActual();
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
    } // --- Fin del método actualizarAparienciaListasPorFoco ---
    

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
        
        if (model == null || controllerRef == null) {
            logger.error("ERROR [mostrarImagenDeExportacion]: Modelo o VisorController de referencia nulos.");
            return;
        }
        if (rutaImagen == null) {
            logger.warn("WARN [mostrarImagenDeExportacion]: Ruta de imagen nula. Limpiando visor principal.");
            controllerRef.actualizarImagenPrincipal(-1); 
            return;
        }
        
        String claveImagen = rutaImagen.toString().replace("\\", "/");
        
        if (enModoVistaExportacion) {
            int indiceEnMasterList = model.getModeloLista().indexOf(claveImagen);
            if(indiceEnMasterList != -1) {
                controllerRef.actualizarImagenPrincipal(indiceEnMasterList);
            } else {
                 controllerRef.actualizarImagenPrincipalPorPath(rutaImagen, claveImagen);
            }
        } else {
            int indiceEnProyectoContext = model.getProyectoListContext().getModeloLista().indexOf(claveImagen);
            if (indiceEnProyectoContext != -1) {
                model.getProyectoListContext().setSelectedImageKey(claveImagen);
                logger.debug("  -> Imagen encontrada en el contexto del proyecto (índice " + indiceEnProyectoContext + "). Cargando...");
                controllerRef.actualizarImagenPrincipal(indiceEnProyectoContext);
            } else {
                logger.debug("  -> Imagen NO encontrada en el contexto del proyecto. Cargando directamente por Path...");
                controllerRef.actualizarImagenPrincipalPorPath(rutaImagen, claveImagen);
                javax.swing.SwingUtilities.invokeLater(() -> {
                    JList<String> listaSeleccion = registry.get("list.proyecto.nombres");
                    JList<String> listaDescartes = registry.get("list.proyecto.descartes");
                    if (listaSeleccion != null) listaSeleccion.clearSelection();
                    if (listaDescartes != null) listaDescartes.clearSelection();
                });
            }
        }
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
            refrescarGridProyecto();
        }
    } // ---FIN de metodo ---

    public void solicitarBorradoEtiquetaParaImagenSeleccionada() {
        if (model.getCurrentDisplayMode() != VisorModel.DisplayMode.GRID) return;

        String claveSeleccionada = model.getSelectedImageKey();
        if (claveSeleccionada == null || claveSeleccionada.isBlank()) return;

        Path ruta = model.getRutaCompleta(claveSeleccionada);
        if (ruta != null) {
            projectManager.setEtiqueta(ruta, null);
            refrescarGridProyecto();
        }
    } // ---FIN de metodo ---

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
    } // ---FIN de metodo ---

    private void refrescarGridProyecto() {
        JList<String> gridList = registry.get("list.grid.proyecto");
        if (gridList != null) {
            gridList.revalidate();
            gridList.repaint();
        }
    } // ---FIN de metodo ---
    
    
    public JTable getTablaExportacionDesdeRegistro() {
    	if (registry == null) return null;
    	vista.panels.export.ExportPanel exportPanel = registry.get("panel.proyecto.herramientas.exportar");
    	if (exportPanel != null) {
    		return exportPanel.getTablaExportacion(); 
    	}
    	logger.warn("WARN [ProjectController]: No se pudo encontrar 'tablaExportacion' a través del registro.");
    	return null;
    } // --- Fin del método getTablaExportacionDesdeRegistro ---
    
    
    public void setViewManager(IViewManager viewManager) { this.viewManager = Objects.requireNonNull(viewManager); }
    public void setProjectManager(IProjectManager projectManager) {this.projectManager = Objects.requireNonNull(projectManager);}
    public void setRegistry(ComponentRegistry registry) { this.registry = Objects.requireNonNull(registry); }
    public void setZoomManager(IZoomManager zoomManager) { this.zoomManager = Objects.requireNonNull(zoomManager); }
    public void setListCoordinator(IListCoordinator listCoordinator) { this.listCoordinator = Objects.requireNonNull(listCoordinator); }
    public void setView(VisorView view) { this.view = Objects.requireNonNull(view); }
    public void setActionMap(Map<String, Action> actionMap) { this.actionMap = Objects.requireNonNull(actionMap); }
    public void setModel(VisorModel model) { this.model = Objects.requireNonNull(model); }
    public void setController(VisorController controller) { this.controllerRef = Objects.requireNonNull(controller); }
    public VisorController getController() {return this.controllerRef;}
    
    public void setProjectListCoordinator(ProjectListCoordinator coordinator) {this.projectListCoordinator = coordinator;}
    public ProjectListCoordinator getProjectListCoordinator() {return this.projectListCoordinator;}
    public IProjectManager getProjectManager() {return this.projectManager;    }
    public void setDisplayModeManager(DisplayModeManager displayModeManager) {this.displayModeManager = displayModeManager;}

} // --- FIN de la clase ProjectController ---