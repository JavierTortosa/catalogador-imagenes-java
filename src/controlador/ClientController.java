package controlador;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.interfaces.IModoController;
import controlador.utils.ComponentRegistry;
import modelo.proyecto.ProjectModel;
import modelo.proyecto.SelectionState;
import servicios.ProjectManager;
import servicios.ProyectoIOException;
import servicios.ValidationService;
import servicios.cliente.ClientResponseImporter;
import servicios.cliente.ClientResponseImporter.ImportReport;
import servicios.cliente.ClientSyncService;
import modelo.proyecto.ImageCheckboxOverlay;
import modelo.proyecto.ExportConfig;
import modelo.proyecto.ProjectImage;
import servicios.cliente.WebCatalogExporter;

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import controlador.managers.interfaces.IProjectManager;
import controlador.services.proyecto.ExportPreflightService;
import modelo.VisorModel;
import vista.models.ClienteTableModel;
import vista.models.ProyectoClienteTableModel;

/**
 * Controlador principal para el Modo Cliente.
 * Gestiona la lógica de la UI y la interacción del usuario cuando 
 * la aplicación está en modo de revisión de cliente.
 */
public class ClientController implements IModoController {

    private static final Logger logger = LoggerFactory.getLogger(ClientController.class);

    private GeneralController generalController;
    private ProjectManager projectManager;
    private ValidationService validationService;
    private ClientSyncService clientSyncService;
    private WebCatalogExporter webCatalogExporter;
    private ClientResponseImporter clientResponseImporter;
    private ComponentRegistry registry;
    private VisorController visorController;
    private controlador.ProjectListCoordinator projectListCoordinator;
    private boolean sincronizandoTablas;
    private boolean gridListenerRegistered;
    private String lastClientImageKey;

    public ClientController() {
        // Inicialización vacía, las dependencias se inyectarán vía setters
    } // --- Fin de metodo ClientController (constructor) ---


    public void setVisorController(VisorController visorController) {
        this.visorController = visorController;
    } // --- Fin de metodo setVisorController ---


    public void setComponentRegistry(ComponentRegistry registry) {
        this.registry = registry;
    } // --- Fin de metodo setComponentRegistry ---


    public void setProjectListCoordinator(controlador.ProjectListCoordinator projectListCoordinator) {
        this.projectListCoordinator = projectListCoordinator;
    } // --- Fin de metodo setProjectListCoordinator ---


    public void setGeneralController(GeneralController generalController) {
        this.generalController = generalController;
    } // --- Fin de metodo setGeneralController ---


    public void setProjectManager(ProjectManager projectManager) {
        this.projectManager = projectManager;
    } // --- Fin de metodo setProjectManager ---


    public ProjectManager getProjectManager() {
        return projectManager;
    } // --- Fin de metodo getProjectManager ---


    public void setValidationService(ValidationService validationService) {
        this.validationService = validationService;
    } // --- Fin de metodo setValidationService ---


    public void setClientSyncService(ClientSyncService clientSyncService) {
        this.clientSyncService = clientSyncService;
    } // --- Fin de metodo setClientSyncService ---


    /**
     * Activa y prepara la vista para el modo cliente.
     */
    public void activarVistaCliente() {
        logger.info("Activando vista del Modo Cliente...");
        ProjectModel project = projectManager != null ? projectManager.getCurrentProject() : null;
        if (project == null) {
            logger.warn("[ClientController] No hay proyecto activo.");
            return;
        }
        if (registry != null) {
            asignarModelosTablas(project);
        }
        // Registrar listener para sincronizar tablas cuando cambia la imagen desde el display
        if (visorController != null && visorController.getModel() != null) {
            visorController.getModel().setSelectedImageKeyListener(this::sincronizarSeleccionEnTablas);
        }
        // Registrar listener de selección en el grid del modo cliente
        javax.swing.JList<String> gridList = registry != null ? registry.get("list.grid.cliente") : null;
        if (gridList != null && !gridListenerRegistered) {
            gridList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int idx = gridList.getLeadSelectionIndex();
                    if (idx >= 0 && idx < gridList.getModel().getSize()) {
                        String key = gridList.getModel().getElementAt(idx);
                        if (key != null && visorController != null && visorController.getModel() != null) {
                            Path fullPath = visorController.getModel().getRutaCompleta(key);
                            if (fullPath != null) {
                                visorController.actualizarImagenPrincipalPorPath(fullPath, key);
                            }
                        }
                    }
                }
            });
            gridListenerRegistered = true;
        }

        // Restaurar imagen seleccionada anteriormente, o seleccionar primera fila
        restaurarOIniciarSeleccion();
        logger.info("[ClientController] Tablas del Modo Cliente refrescadas.");
    } // --- Fin de metodo activarVistaCliente ---


    /**
     * Restaura la &uacute;ltima imagen seleccionada en modo cliente,
     * o si es la primera vez, selecciona la primera fila de la tabla de
     * selecci&oacute;n del proyecto para mostrar una imagen en el visor.
     */
    private void restaurarOIniciarSeleccion() {
        JTable t = registry != null ? registry.get("table.cliente.proyecto.seleccion") : null;
        if (t == null || t.getRowCount() == 0) return;

        String targetKey = lastClientImageKey;
        if (targetKey != null) {
            String canonicalTarget = ProjectModel.normalizarClaveImagen(targetKey);
            for (int viewRow = 0; viewRow < t.getRowCount(); viewRow++) {
                int modelRow = t.convertRowIndexToModel(viewRow);
                javax.swing.table.TableModel m = t.getModel();
                String rowKey = null;
                if (m instanceof ProyectoClienteTableModel pctm) {
                    rowKey = pctm.getImageKey(modelRow);
                } else if (m instanceof ClienteTableModel ctm) {
                    rowKey = ctm.getImageKey(modelRow);
                }
                if (canonicalTarget != null && canonicalTarget.equals(rowKey)) {
                    t.setRowSelectionInterval(viewRow, viewRow);
                    t.scrollRectToVisible(t.getCellRect(viewRow, 0, true));
                    return;
                }
            }
        }
        // No hay imagen previa o no se encontró: seleccionar primera fila
        t.setRowSelectionInterval(0, 0);
        t.scrollRectToVisible(t.getCellRect(0, 0, true));
    } // --- Fin de metodo restaurarOIniciarSeleccion ---


    private void asignarModelosTablas(ProjectModel project) {
        JTable t;
        t = registry.get("table.cliente.proyecto.seleccion");
        if (t != null) {
            t.setModel(new vista.models.ProyectoClienteTableModel(project, true));
            t.getColumnModel().getColumn(vista.models.ProyectoClienteTableModel.COL_CODIGO)
                    .setCellRenderer(new vista.renderers.CodeCellRenderer());
            t.getColumnModel().getColumn(vista.models.ProyectoClienteTableModel.COL_CODIGO).setMaxWidth(80);
        }
        t = registry.get("table.cliente.proyecto.descartes");
        if (t != null) {
            t.setModel(new vista.models.ProyectoClienteTableModel(project, false));
            t.getColumnModel().getColumn(vista.models.ProyectoClienteTableModel.COL_CODIGO)
                    .setCellRenderer(new vista.renderers.CodeCellRenderer());
            t.getColumnModel().getColumn(vista.models.ProyectoClienteTableModel.COL_CODIGO).setMaxWidth(80);
        }
        t = registry.get("table.cliente.cliente.seleccion");
        if (t != null) {
            t.setModel(new vista.models.ClienteTableModel(project, true));
            t.getColumnModel().getColumn(vista.models.ClienteTableModel.COL_ESTADO)
                    .setCellRenderer(new vista.renderers.TristateCellRenderer());
            t.getColumnModel().getColumn(vista.models.ClienteTableModel.COL_ESTADO).setMaxWidth(50);
            t.getColumnModel().getColumn(vista.models.ClienteTableModel.COL_CODIGO_IMG).setMaxWidth(80);
            t.getColumnModel().getColumn(vista.models.ClienteTableModel.COL_CODIGO_IMG)
                    .setCellRenderer(new vista.renderers.CodeCellRenderer());
            t.getColumnModel().getColumn(vista.models.ClienteTableModel.COL_CODIGO_CB).setMaxWidth(80);
            t.getColumnModel().getColumn(vista.models.ClienteTableModel.COL_CODIGO_CB)
                    .setCellRenderer(new vista.renderers.CodeCellRenderer());
            t.getColumnModel().getColumn(vista.models.ClienteTableModel.COL_COMENTARIO)
                    .setCellRenderer(new vista.renderers.CommentCellRenderer());
            t.getColumnModel().getColumn(vista.models.ClienteTableModel.COL_COMENTARIO).setMaxWidth(50);
        }
        t = registry.get("table.cliente.cliente.descartes");
        if (t != null) {
            t.setModel(new vista.models.ClienteTableModel(project, false));
            t.getColumnModel().getColumn(vista.models.ClienteTableModel.COL_ESTADO)
                    .setCellRenderer(new vista.renderers.TristateCellRenderer());
            t.getColumnModel().getColumn(vista.models.ClienteTableModel.COL_ESTADO).setMaxWidth(50);
            t.getColumnModel().getColumn(vista.models.ClienteTableModel.COL_CODIGO_IMG).setMaxWidth(80);
            t.getColumnModel().getColumn(vista.models.ClienteTableModel.COL_CODIGO_IMG)
                    .setCellRenderer(new vista.renderers.CodeCellRenderer());
            t.getColumnModel().getColumn(vista.models.ClienteTableModel.COL_CODIGO_CB).setMaxWidth(80);
            t.getColumnModel().getColumn(vista.models.ClienteTableModel.COL_CODIGO_CB)
                    .setCellRenderer(new vista.renderers.CodeCellRenderer());
            t.getColumnModel().getColumn(vista.models.ClienteTableModel.COL_COMENTARIO)
                    .setCellRenderer(new vista.renderers.CommentCellRenderer());
            t.getColumnModel().getColumn(vista.models.ClienteTableModel.COL_COMENTARIO).setMaxWidth(50);
        }
        refrescarTablas();

        // Poblar el grid del modo cliente con las imágenes seleccionadas del proyecto
        // usando las claves del VisorModel para que el GridCellRenderer pueda resolverlas
        javax.swing.JList<String> gridCliente = registry.get("list.grid.cliente");
        if (gridCliente != null && visorController != null && visorController.getModel() != null) {
            DefaultListModel<String> gridModel = new DefaultListModel<>();
            Map<String, Path> rutaCompletaMap = visorController.getModel().getRutaCompletaMap();
            if (project.getMasterImages() != null && rutaCompletaMap != null) {
                for (modelo.proyecto.ProjectImage pi : project.getMasterImages().values()) {
                    if (!pi.isEnSeleccionProyecto()) continue;
                    String canonicalPath = pi.getRutaImagen();
                    // Buscar la clave del VisorModel cuya ruta completa coincida
                    for (Map.Entry<String, Path> entry : rutaCompletaMap.entrySet()) {
                        if (entry.getValue() != null && canonicalPath != null
                                && entry.getValue().toString().replace("\\", "/").equals(canonicalPath)) {
                            gridModel.addElement(entry.getKey());
                            break;
                        }
                    }
                }
            }
            gridCliente.setModel(gridModel);
        }
    } // --- Fin de metodo asignarModelosTablas ---


    /**
     * Deriva el estado de la imagen a partir del estado de sus checkboxes internos.
     * - Al menos un SELECTED → imagen SELECTED
     * - Ningún SELECTED, al menos un UNDEFINED → imagen UNDEFINED
     * - Todos DISCARDED → imagen DISCARDED
     * También sincroniza las entradas compuestas (imgCode_cbCode) con el estado de cada overlay.
     */
    public void derivarEstadoImagen(String imageKey) {
        if (projectManager == null || projectManager.getCurrentProject() == null) return;
        ProjectModel project = projectManager.getCurrentProject();
        String canonicalKey = ProjectModel.normalizarClaveImagen(imageKey);
        ProjectImage pi = project.getMasterImages().get(canonicalKey);
        if (pi == null) return;
        List<ImageCheckboxOverlay> checkboxes = pi.getCheckboxes();
        if (checkboxes == null || checkboxes.isEmpty()) return;

        boolean hasSelected = false;
        boolean hasUndefined = false;
        for (ImageCheckboxOverlay cb : checkboxes) {
            switch (cb.getState()) {
                case SELECTED  -> hasSelected = true;
                case UNDEFINED -> hasUndefined = true;
                default -> {}
            }
        }

        SelectionState derived;
        if (hasSelected) {
            derived = SelectionState.SELECTED;
        } else if (hasUndefined) {
            derived = SelectionState.UNDEFINED;
        } else {
            derived = SelectionState.DISCARDED;
        }
        pi.setEstadoCliente(derived);
        projectManager.notificarModificacion();
        refrescarTablas();
    } // --- Fin de metodo derivarEstadoImagen ---


    public void compartirConCliente() {
        logger.info("[ClientController] Iniciando flujo de compartir con el cliente...");
        ProjectModel project = projectManager != null ? projectManager.getCurrentProject() : null;
        if (project == null) {
            JOptionPane.showMessageDialog(null, "No hay ningún proyecto activo.",
                    "Compartir al Cliente", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (project.isSharedWithClient()) {
            // === ITERACIÓN: proyecto ya compartido ===
            compartirIteracion(project);
            return;
        }

        // === PRIMERA VEZ ===
        Map<String, String> selectedImages = project.getSelectedImages();
        List<String> discardedImages = project.getDiscardedImages();
        if (selectedImages == null || selectedImages.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No hay imágenes seleccionadas en el proyecto.",
                    "Compartir al Cliente", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int totalImages = selectedImages.size() + discardedImages.size();

        // 1. Preparar masterImages localmente (sin tocar el proyecto aún)
        Map<String, ProjectImage> master = new LinkedHashMap<>();
        for (String ruta : selectedImages.keySet()) {
            String canonical = ProjectModel.normalizarClaveImagen(ruta);
            ProjectImage pi = null;
            // Preservar datos existentes si ya hay masterImages
            if (project.getMasterImages() != null && project.getMasterImages().containsKey(canonical)) {
                pi = project.getMasterImages().get(canonical);
                pi.setEstadoCliente(SelectionState.UNDEFINED);
            }
            if (pi == null) {
                pi = new ProjectImage(canonical);
                pi.setEnSeleccionProyecto(true);
                pi.setEstadoCliente(SelectionState.UNDEFINED);
                String etiqueta = selectedImages.get(ruta);
                if (etiqueta != null && !etiqueta.isEmpty()) {
                    pi.setEtiqueta(etiqueta);
                }
            }
            master.put(canonical, pi);
        }
        for (String ruta : discardedImages) {
            String canonical = ProjectModel.normalizarClaveImagen(ruta);
            ProjectImage pi = null;
            if (project.getMasterImages() != null && project.getMasterImages().containsKey(canonical)) {
                pi = project.getMasterImages().get(canonical);
                pi.setEstadoCliente(SelectionState.UNDEFINED);
                pi.setEnSeleccionProyecto(false);
            }
            if (pi == null) {
                pi = new ProjectImage(canonical);
                pi.setEnSeleccionProyecto(false);
                pi.setEstadoCliente(SelectionState.UNDEFINED);
            }
            master.put(canonical, pi);
        }

        // 2. Generar códigos correlativos localmente
        int numDigitos = Math.max(3, String.valueOf(totalImages).length());
        String formato = "C%0" + numDigitos + "d";
        int idx = 0;
        for (ProjectImage pi : master.values()) {
            pi.setCodigoCatalogo(String.format(formato, ++idx));
        }

        // 3. Validar localmente
        List<String> errores = ExportPreflightService.validarAsignaciones(master);
        if (!errores.isEmpty()) {
            StringBuilder msg = new StringBuilder(
                    "No se puede compartir el proyecto. Corrige los siguientes errores:\n\n");
            for (String err : errores) {
                msg.append(" \u2022 ").append(err).append("\n");
            }
            JOptionPane.showMessageDialog(null, msg.toString(), "Error al Compartir", JOptionPane.ERROR_MESSAGE);
            return; // NO se modifica el proyecto, no hace falta rollback
        }

        // 4. Commit: Aplicar cambios al proyecto
        project.setMasterImages(master);
        project.setSharedWithClient(true);
        project.setSharedTimestamp(System.currentTimeMillis());
        project.setSharedIteration(1);
        if (project.getSchemaVersion() < 2) {
            project.setSchemaVersion(2);
        }

        Path prjPath = projectManager.getArchivoProyectoActivo();
        if (prjPath != null) {
            String prjName = prjPath.getFileName().toString();
            Path prjclPath = prjPath.resolveSibling(
                    prjName.replaceAll("(?i)\\.prj$", "") + ".prjcl");
            projectManager.saveAsCopy(prjclPath);
        }
        projectManager.notificarModificacion();

        // Sincronizar códigos de catálogo de ProjectImage → ExportConfig para que el panel de asignaciones los muestre
        Map<String, ExportConfig> configs = project.getExportConfigs();
        for (ProjectImage pi : project.getMasterImages().values()) {
            String code = pi.getCodigoCatalogo();
            if (code != null && !code.trim().isEmpty()) {
                configs.computeIfAbsent(pi.getRutaImagen(), k -> new ExportConfig()).setCodigoCatalogo(code);
            }
        }

        if (visorController != null && visorController.getStatusBarManager() != null) {
            visorController.getStatusBarManager().mostrarMensajeTemporal(
                    "Proyecto compartido con el cliente. Cambiando a modo cliente...", 5000);
        }

        if (generalController != null) {
            generalController.cambiarModoDeTrabajo(VisorModel.WorkMode.CLIENTE);
        }

        if (!selectedImages.isEmpty()) {
            String primeraImagen = selectedImages.keySet().iterator().next();
            if (visorController != null && primeraImagen != null) {
                visorController.actualizarImagenPrincipalPorPath(Paths.get(primeraImagen), primeraImagen);
            }
        }

        logger.info("[ClientController] Proyecto compartido con \u00e9xito. C\u00f3digos generados: {}",
                totalImages);
        JOptionPane.showMessageDialog(null,
                "Proyecto compartido con el cliente.\n\nSe han generado " + totalImages
                        + " c\u00f3digos de cat\u00e1logo.\nModo Cliente activado.",
                "Compartir Completado", JOptionPane.INFORMATION_MESSAGE);
    } // --- Fin de metodo compartirConCliente ---


    private void compartirIteracion(ProjectModel project) {
        Map<String, ProjectImage> masterActual = project.getMasterImages();
        Map<String, String> selectedImages = project.getSelectedImages();
        List<String> discardedImages = project.getDiscardedImages();

        // Crear copia local para preparación atómica
        Map<String, ProjectImage> nuevasMaster = new LinkedHashMap<>(masterActual);

        // Detectar nuevas imgenes en el proyecto no presentes en masterImages
        int newCount = 0;
        for (String ruta : selectedImages.keySet()) {
            String canonical = ProjectModel.normalizarClaveImagen(ruta);
            if (!nuevasMaster.containsKey(canonical)) {
                ProjectImage pi = new ProjectImage(canonical);
                pi.setEnSeleccionProyecto(true);
                pi.setEstadoCliente(SelectionState.UNDEFINED);
                nuevasMaster.put(canonical, pi);
                newCount++;
            }
        }
        for (String ruta : discardedImages) {
            String canonical = ProjectModel.normalizarClaveImagen(ruta);
            if (!nuevasMaster.containsKey(canonical)) {
                ProjectImage pi = new ProjectImage(canonical);
                pi.setEnSeleccionProyecto(false);
                pi.setEstadoCliente(SelectionState.DISCARDED);
                nuevasMaster.put(canonical, pi);
                newCount++;
            }
        }

        // Generar códigos para todas las imágenes que no tengan (nuevas o añadidas desde el visor)
        int maxCode = 0;
        for (ProjectImage pi : nuevasMaster.values()) {
            String code = pi.getCodigoCatalogo();
            if (code != null && code.startsWith("C")) {
                try {
                    int num = Integer.parseInt(code.substring(1));
                    maxCode = Math.max(maxCode, num);
                } catch (NumberFormatException ignored) {}
            }
        }
        int numDigitos = Math.max(3, String.valueOf(nuevasMaster.size()).length());
        String formato = "C%0" + numDigitos + "d";
        int idx = maxCode;
        for (ProjectImage pi : nuevasMaster.values()) {
            if (pi.getCodigoCatalogo() == null || pi.getCodigoCatalogo().isEmpty()) {
                pi.setCodigoCatalogo(String.format(formato, ++idx));
            }
        }

        // Validar localmente
        List<String> errores = ExportPreflightService.validarAsignaciones(nuevasMaster);
        if (!errores.isEmpty()) {
            StringBuilder msg = new StringBuilder(
                    "No se puede compartir la nueva iteración. Corrige los siguientes errores:\n\n");
            for (String err : errores) {
                msg.append(" \u2022 ").append(err).append("\n");
            }
            JOptionPane.showMessageDialog(null, msg.toString(), "Error al Compartir Iteración", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Commit: Aplicar cambios al proyecto
        project.setMasterImages(nuevasMaster);
        project.setSharedIteration(project.getSharedIteration() + 1);

        Path prjPath = projectManager.getArchivoProyectoActivo();
        if (prjPath != null) {
            String prjName = prjPath.getFileName().toString();
            Path prjclPath = prjPath.resolveSibling(
                    prjName.replaceAll("(?i)\\.prj$", "") + ".prjcl");
            projectManager.saveAsCopy(prjclPath);
        }
        projectManager.notificarModificacion();

        // Sincronizar códigos de catálogo de ProjectImage → ExportConfig para que el panel de asignaciones los muestre
        Map<String, ExportConfig> configsIter = project.getExportConfigs();
        for (ProjectImage pi : project.getMasterImages().values()) {
            String code = pi.getCodigoCatalogo();
            if (code != null && !code.trim().isEmpty()) {
                configsIter.computeIfAbsent(pi.getRutaImagen(), k -> new ExportConfig()).setCodigoCatalogo(code);
            }
        }

        if (generalController != null) {
            generalController.cambiarModoDeTrabajo(VisorModel.WorkMode.CLIENTE);
        }

        JOptionPane.showMessageDialog(null,
                "Nueva iteracion compartida con exito.\n" + (newCount > 0 ? "Se han generado codigos para " + newCount + " imagenes nuevas." : "No hubo imagenes nuevas."),
                "Compartir Iteracion", JOptionPane.INFORMATION_MESSAGE);
    } // --- Fin de metodo compartirIteracion ---


    /**
     * Refresca las listas de selección y descartes del cliente en la UI.
     */
    private void refrescarListasCliente() {
        if (registry == null || projectManager == null) return;
        ProjectModel project = projectManager.getCurrentProject();
        if (project == null) return;

        JList<String> cliSel = registry.get("list.cliente.cliente.seleccion");
        JList<String> cliDesc = registry.get("list.cliente.cliente.descartes");
        if (cliSel != null) cliSel.setModel(new DefaultListModel<>());
        if (cliDesc != null) cliDesc.setModel(new DefaultListModel<>());

        DefaultListModel<String> cliSelModel = (cliSel != null) ? (DefaultListModel<String>) cliSel.getModel() : null;
        DefaultListModel<String> cliDescModel = (cliDesc != null) ? (DefaultListModel<String>) cliDesc.getModel() : null;
        for (var pi : project.getMasterImages().values()) {
            String ruta = pi.getRutaImagen();
            if (ruta == null) continue;
            switch (pi.getEstadoCliente()) {
                case SELECTED, UNDEFINED:
                    if (cliSelModel != null) cliSelModel.addElement(ruta);
                    break;
                case DISCARDED:
                    if (cliDescModel != null) cliDescModel.addElement(ruta);
                    break;
            }
        }
    } // --- Fin de metodo refrescarListasCliente ---


    public void refrescarTablas() {
        if (registry == null) return;
        javax.swing.JTable t;
        t = registry.get("table.cliente.proyecto.seleccion");
        if (t != null && t.getModel() instanceof vista.models.ProyectoClienteTableModel pm1) pm1.refrescar();
        t = registry.get("table.cliente.proyecto.descartes");
        if (t != null && t.getModel() instanceof vista.models.ProyectoClienteTableModel pm2) pm2.refrescar();
        t = registry.get("table.cliente.cliente.seleccion");
        if (t != null && t.getModel() instanceof vista.models.ClienteTableModel cm1) cm1.refrescar();
        t = registry.get("table.cliente.cliente.descartes");
        if (t != null && t.getModel() instanceof vista.models.ClienteTableModel cm2) cm2.refrescar();
    } // --- Fin de metodo refrescarTablas ---


    /**
     * Sincroniza la selecci&oacute;n de todas las tablas del modo cliente
     * para que coincidan con la imagen actualmente mostrada en el display.
     * Se ejecuta cuando el usuario navega desde el panel de visualizaci&oacute;n.
     *
     * @param imageKey clave normalizada de la imagen a seleccionar en las tablas.
     */
    public void sincronizarSeleccionEnTablas(String imageKey) {
        if (sincronizandoTablas || registry == null || imageKey == null) return;
        sincronizandoTablas = true;
        try {
            String canonicalKey = ProjectModel.normalizarClaveImagen(imageKey);
            String[] tableNames = {
                    "table.cliente.proyecto.seleccion",
                    "table.cliente.proyecto.descartes",
                    "table.cliente.cliente.seleccion",
                    "table.cliente.cliente.descartes"
            };
            for (String name : tableNames) {
                JTable t = registry.get(name);
                if (t == null) continue;
                javax.swing.table.TableModel m = t.getModel();
                for (int viewRow = 0; viewRow < t.getRowCount(); viewRow++) {
                    int modelRow = t.convertRowIndexToModel(viewRow);
                    String rowKey = null;
                    if (m instanceof ProyectoClienteTableModel pctm) {
                        rowKey = pctm.getImageKey(modelRow);
                    } else if (m instanceof ClienteTableModel ctm) {
                        rowKey = ctm.getImageKey(modelRow);
                    }
                    if (canonicalKey != null && canonicalKey.equals(rowKey)) {
                        t.setRowSelectionInterval(viewRow, viewRow);
                        t.scrollRectToVisible(t.getCellRect(viewRow, 0, true));
                        break;
                    }
                }
            }

            // Sincronizar selección del grid del modo cliente
            JList<String> gridCliente = registry.get("list.grid.cliente");
            if (gridCliente != null) {
                DefaultListModel<String> gridModel = (DefaultListModel<String>) gridCliente.getModel();
                for (int i = 0; i < gridModel.size(); i++) {
                    if (canonicalKey.equals(gridModel.get(i))) {
                        gridCliente.setSelectedIndex(i);
                        gridCliente.ensureIndexIsVisible(i);
                        break;
                    }
                }
            }
        } finally {
            sincronizandoTablas = false;
        }
    } // --- Fin de metodo sincronizarSeleccionEnTablas ---

    public VisorController getVisorController() {
        return visorController;
    } // --- Fin de metodo getVisorController ---


    /**
     * Cierra el modo cliente: sincroniza cambios al proyecto, desactiva shared,
     * vuelve a modo proyecto y guarda.
     */
    /**
     * Guarda el estado actual de la vista cliente (imagen seleccionada)
     * para restaurarlo al reingresar.
     */
    public void guardarEstado() {
        if (visorController != null && visorController.getModel() != null) {
            lastClientImageKey = visorController.getModel().getSelectedImageKey();
        }
    } // --- Fin de metodo guardarEstado ---


    public void cerrarCliente() {
        logger.info("[ClientController] Cerrando modo cliente...");
        guardarEstado();
        ProjectModel project = projectManager != null ? projectManager.getCurrentProject() : null;
        if (project == null) {
            JOptionPane.showMessageDialog(null, "No hay proyecto activo.",
                    "Cerrar Cliente", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(null,
                "\u00bfEst\u00e1s seguro de cerrar el modo cliente?\n\n"
                + "Los datos del cliente se conservar\u00e1n.\n"
                + "Volver\u00e1s al modo proyecto para poder exportar.",
                "Cerrar Modo Cliente",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (clientSyncService != null && project.hasClientSelection()) {
            clientSyncService.closeAndSync(project);
        }

        if (generalController != null) {
            var visorModel = generalController.getModel();
            if (visorModel != null) {
                visorModel.setClienteCheckboxVisible(false);
                visorModel.setClienteEditorPanelVisible(false);
            }
        }

        if (registry != null) {
            java.awt.Container displayWrapper = registry.get("container.displaymodes.cliente.wrapper");
            if (displayWrapper != null && displayWrapper.getLayout() instanceof java.awt.CardLayout) {
                ((java.awt.CardLayout) displayWrapper.getLayout()).show(displayWrapper, "DISPLAY_NORMAL");
            }
            java.awt.Window editorDialog = registry.get("dialog.cliente.editor");
            if (editorDialog != null) {
                editorDialog.setVisible(false);
            }
        }

        if (visorController != null && visorController.getModel() != null) {
            visorController.getModel().setSelectedImageKeyListener(null);
        }

        if (generalController != null) {
            generalController.cambiarModoDeTrabajo(VisorModel.WorkMode.PROYECTO);
        }

        projectManager.guardarAArchivo();
        projectManager.notificarModificacion();

        logger.info("[ClientController] Modo cliente cerrado. Proyecto guardado.");
        JOptionPane.showMessageDialog(null,
                "Modo cliente cerrado correctamente.\n"
                + "Los datos del cliente se han conservado.\n"
                + "Puedes exportar desde el modo proyecto.",
                "Cliente Cerrado", JOptionPane.INFORMATION_MESSAGE);
    } // --- Fin de metodo cerrarCliente ---


    public void solicitarUpdateCliente() {
        logger.info("[ClientController] Solicitando update del cliente...");
        if (projectManager == null || projectManager.getCurrentProject() == null) {
            JOptionPane.showMessageDialog(null, "No hay proyecto activo.", "Update",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        ProjectModel project = projectManager.getCurrentProject();
        if (project.getClientSelection() == null
                || project.getClientSelection().getFechaRespuesta() == null
                || project.getClientSelection().getFechaRespuesta().isBlank()) {
            JOptionPane.showMessageDialog(null,
                    "No hay respuesta del cliente. Importa una respuesta antes de actualizar.",
                    "Update", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Construir filas de conflicto desde masterImages
        List<vista.dialogos.SyncConflictDialog.FilaConflicto> conflictRows = new ArrayList<>();
        for (ProjectImage pi : project.getMasterImages().values()) {
            SelectionState clientState = pi.getEstadoCliente();
            if (clientState == SelectionState.UNDEFINED) continue;
            SelectionState projectState = pi.isEnSeleccionProyecto()
                    ? SelectionState.SELECTED : SelectionState.DISCARDED;
            if (clientState != projectState) {
                String nombre = pi.getRutaImagen() != null
                        ? java.nio.file.Paths.get(pi.getRutaImagen()).getFileName().toString() : "";
                conflictRows.add(new vista.dialogos.SyncConflictDialog.FilaConflicto(
                        pi.getRutaImagen(),
                        pi.getCodigoCatalogo() != null ? pi.getCodigoCatalogo() : "",
                        nombre, projectState, clientState));
            }
        }

        if (conflictRows.isEmpty()) {
            // Sin conflictos: aplicar estado del cliente directamente
            for (ProjectImage pi : project.getMasterImages().values()) {
                if (pi.getEstadoCliente() == SelectionState.SELECTED) {
                    pi.setEnSeleccionProyecto(true);
                } else if (pi.getEstadoCliente() == SelectionState.DISCARDED) {
                    pi.setEnSeleccionProyecto(false);
                }
            }
            projectManager.guardarAArchivo();
            projectManager.notificarModificacion();
            refrescarTablas();
            JOptionPane.showMessageDialog(null, "No hay conflictos. Proyecto actualizado.",
                    "Update", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFrame owner = registry != null ? registry.get("frame.principal") : null;
        vista.dialogos.SyncConflictDialog dialog = new vista.dialogos.SyncConflictDialog(owner, conflictRows);
        dialog.setVisible(true);

        if (dialog.isAccepted()) {
            Map<String, Boolean> resolved = dialog.getResolvedConflicts();
            for (Map.Entry<String, Boolean> entry : resolved.entrySet()) {
                ProjectImage pi = project.getMasterImages().get(entry.getKey());
                if (pi != null) {
                    pi.setEnSeleccionProyecto(entry.getValue());
                }
            }
            projectManager.guardarAArchivo();
            projectManager.notificarModificacion();
            refrescarTablas();
            JOptionPane.showMessageDialog(null,
                    "Conflictos resueltos y aplicados al proyecto.",
                    "Update", JOptionPane.INFORMATION_MESSAGE);
        }
    } // --- Fin de metodo solicitarUpdateCliente ---


    public void closeAndSyncProject() {
        logger.info("Cerrando y sincronizando proyecto de cliente...");
        if (projectManager != null && projectManager.getCurrentProject() != null && clientSyncService != null) {
            ProjectModel project = projectManager.getCurrentProject();
            clientSyncService.closeAndSync(project);
            projectManager.guardarAArchivo();
            projectManager.notificarModificacion();
        }
    } // --- Fin de metodo closeAndSyncProject ---


    /**
     * Carga un archivo de proyecto de cliente (.prjcl) y configura el modelo.
     * Rechaza archivos .prj normales.
     *
     * @param ruta Path al archivo .prjcl.
     * @throws ProyectoIOException si el archivo es .prj en lugar de .prjcl.
     */
    public void cargarPrjcl(Path ruta) throws ProyectoIOException {
        String nameLC = ruta.getFileName().toString().toLowerCase();
        if (nameLC.endsWith(servicios.ProjectManager.EXTENSION_PRJ)) {
            throw new ProyectoIOException(
                    "El archivo '" + ruta.getFileName()
                    + "' es un proyecto normal (.prj).\n"
                    + "Usa el Modo Proyecto (Ctrl+2) para abrir este tipo de archivos.");
        }
        logger.info("[ClientController] Cargando proyecto de cliente: {}", ruta);
        if (projectManager != null) {
            projectManager.abrirProyectoCliente(ruta);
            activarVistaCliente();
        }
    } // --- Fin de metodo cargarPrjcl ---


    /**
     * Crea un nuevo archivo .prjcl (borra la selección de cliente actual).
     */
    public void solicitarNuevoPrjcl() {
        logger.info("[ClientController] Nuevo archivo .prjcl...");
        ProjectModel project = projectManager != null ? projectManager.getCurrentProject() : null;
        if (project == null) {
            logger.warn("[ClientController] No hay proyecto activo.");
            return;
        }
        for (var pi : project.getMasterImages().values()) {
            pi.setEstadoCliente(SelectionState.UNDEFINED);
            pi.setEstadoClienteOriginal(SelectionState.UNDEFINED);
        }
        projectManager.notificarModificacion();
        activarVistaCliente();
    } // --- Fin de metodo solicitarNuevoPrjcl ---


    /**
     * Abre un archivo .prjcl mediante JFileChooser.
     */
    public void manejarAbrirPrjcl() {
        logger.info("[ClientController] Abrir archivo .prjcl...");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Abrir archivo de cliente (.prjcl)");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Archivos de Cliente (*.prjcl)", "prjcl"));
        Path dirInicial = projectManager != null
                ? projectManager.getCarpetaBaseProyectos()
                : Path.of(System.getProperty("user.home"));
        fileChooser.setCurrentDirectory(dirInicial.toFile());

        int result = fileChooser.showOpenDialog(visorController != null ? visorController.getView() : null);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                cargarPrjcl(fileChooser.getSelectedFile().toPath());
            } catch (ProyectoIOException e) {
                logger.error("Error al abrir .prjcl", e);
                JOptionPane.showMessageDialog(visorController != null ? visorController.getView() : null,
                        e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    } // --- Fin de metodo manejarAbrirPrjcl ---


    /**
     * Guarda la selección de cliente en el archivo .prjcl activo (o pide nombre).
     */
    public void solicitarGuardarPrjcl() {
        IProjectManager pm = projectManager;
        if (pm == null) return;
        if (pm.getArchivoProyectoActivo() == null) {
            solicitarGuardarPrjclComo();
        } else {
            pm.guardarAArchivo();
            logger.info("[ClientController] Proyecto de cliente guardado.");
        }
    } // --- Fin de metodo solicitarGuardarPrjcl ---


    /**
     * Guarda la selección de cliente en un nuevo archivo .prjcl.
     */
    public void solicitarGuardarPrjclComo() {
        logger.info("[ClientController] Guardar archivo .prjcl como...");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar archivo de cliente (.prjcl)");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Archivos de Cliente (*.prjcl)", "prjcl"));
        Path dirInicial = projectManager != null
                ? projectManager.getCarpetaBaseProyectos()
                : Path.of(System.getProperty("user.home"));
        fileChooser.setCurrentDirectory(dirInicial.toFile());

        int result = fileChooser.showSaveDialog(visorController != null ? visorController.getView() : null);
        if (result == JFileChooser.APPROVE_OPTION) {
            Path archivo = fileChooser.getSelectedFile().toPath();
            if (!archivo.getFileName().toString().toLowerCase().endsWith(".prjcl")) {
                archivo = archivo.resolveSibling(archivo.getFileName() + ".prjcl");
            }
            if (projectManager != null) {
                projectManager.guardarProyectoComo(archivo);
            }
        }
    } // --- Fin de metodo solicitarGuardarPrjclComo ---


    /**
     * Exporta el catálogo web para enviar al cliente.
     *
     * @param outputDir Carpeta donde se generarán los archivos.
     * @param iteracion Número de iteración actual.
     */
    public void exportarParaCliente(Path outputDir, int iteracion) {
        logger.info("[ClientController] Exportando catálogo para el cliente en: {}", outputDir);
        ProjectModel project = projectManager != null ? projectManager.getCurrentProject() : null;
        if (project == null) {
            logger.error("[ClientController] No hay proyecto activo para exportar.");
            return;
        }
        if (webCatalogExporter == null) {
            webCatalogExporter = new WebCatalogExporter();
        }

        javax.swing.JDialog progressDialog = new javax.swing.JDialog(
                registry != null ? (JFrame) registry.get("frame.principal") : null,
                "Exportando catálogo...", true);
        progressDialog.setDefaultCloseOperation(javax.swing.JDialog.DO_NOTHING_ON_CLOSE);
        javax.swing.JProgressBar progressBar = new javax.swing.JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Generando miniaturas...");
        progressBar.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        progressDialog.add(progressBar);
        progressDialog.setSize(350, 80);
        progressDialog.setLocationRelativeTo(registry != null
                ? (java.awt.Window) registry.get("frame.principal") : null);

        javax.swing.SwingWorker<Void, Integer> worker = new javax.swing.SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                webCatalogExporter.exportar(project, outputDir, iteracion, progress -> {
                    publish(progress);
                });
                return null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                int p = chunks.get(chunks.size() - 1);
                progressBar.setValue(p);
                if (p < 90) {
                    progressBar.setString("Miniaturas: " + p + "%");
                } else if (p < 100) {
                    progressBar.setString("Generando archivos...");
                } else {
                    progressBar.setString("Completado");
                }
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    get();
                    logger.info("[ClientController] Exportación completada.");
                    JOptionPane.showMessageDialog(
                            registry != null ? (java.awt.Component) registry.get("frame.principal") : null,
                            "Catálogo exportado correctamente a:\n" + outputDir.toAbsolutePath(),
                            "Exportación Completada", JOptionPane.INFORMATION_MESSAGE);
                } catch (java.util.concurrent.ExecutionException e) {
                    logger.error("[ClientController] Error durante la exportación: {}",
                            e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
                    JOptionPane.showMessageDialog(
                            registry != null ? (java.awt.Component) registry.get("frame.principal") : null,
                            "Error al exportar: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()),
                            "Error de Exportación", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };

        worker.execute();
        progressDialog.setVisible(true);
    } // --- Fin de metodo exportarParaCliente ---


    public void exportarHtmlCliente() {
        logger.info("[ClientController] Exportando HTML único para el cliente...");
        ProjectModel project = projectManager != null ? projectManager.getCurrentProject() : null;
        if (project == null) {
            logger.error("[ClientController] No hay proyecto activo para exportar.");
            return;
        }
        if (!project.hasClientSelection()) {
            JOptionPane.showMessageDialog(null, "El proyecto no tiene selección de cliente.\n"
                    + "Comparte primero el proyecto con el cliente.",
                    "Exportar HTML", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar catálogo HTML para el cliente");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Archivo HTML (*.html)", "html"));
        String defaultName = (project.getProjectName() != null
                ? project.getProjectName().replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase()
                : "proyecto")
                + "_iteracion" + project.getSharedIteration() + ".html";
        chooser.setSelectedFile(new java.io.File(defaultName));

        if (chooser.showSaveDialog(registry != null
                ? (java.awt.Window) registry.get("frame.principal") : null)
                != JFileChooser.APPROVE_OPTION) return;

        Path outputFile = chooser.getSelectedFile().toPath();
        if (!outputFile.getFileName().toString().toLowerCase().endsWith(".html")) {
            outputFile = outputFile.resolveSibling(outputFile.getFileName() + ".html");
        }

        Path finalOutput = outputFile;
        var progressDialog = new javax.swing.JDialog(
                registry != null ? (java.awt.Window) registry.get("frame.principal") : null,
                "Exportando catálogo HTML...", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        var progressBar = new javax.swing.JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Generando miniaturas...");
        progressBar.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        progressDialog.add(progressBar);
        progressDialog.setSize(350, 70);
        progressDialog.setLocationRelativeTo(
                registry != null ? (java.awt.Window) registry.get("frame.principal") : null);
        progressDialog.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);

        javax.swing.SwingWorker<Void, Void> worker = new javax.swing.SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (webCatalogExporter == null) {
                    webCatalogExporter = new WebCatalogExporter();
                }
                webCatalogExporter.exportarHtmlCliente(project, finalOutput,
                        project.getSharedIteration(), progress -> setProgress(progress));
                return null;
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    get();
                    logger.info("[ClientController] HTML cliente exportado: {}", finalOutput);
                    JOptionPane.showMessageDialog(
                            registry != null ? (java.awt.Component) registry.get("frame.principal") : null,
                            "Catálogo HTML exportado correctamente a:\n" + finalOutput.toAbsolutePath(),
                            "Exportación Completada", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    logger.error("[ClientController] Error exportando HTML: {}",
                            e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
                    JOptionPane.showMessageDialog(
                            registry != null ? (java.awt.Component) registry.get("frame.principal") : null,
                            "Error al exportar: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                int progress = (int) evt.getNewValue();
                progressBar.setValue(progress);
                if (progress < 100) {
                    progressBar.setString("Generando miniaturas... " + progress + "%");
                } else {
                    progressBar.setString("Escribiendo archivo HTML...");
                }
            }
        });
        worker.execute();
        progressDialog.setVisible(true);
    } // --- Fin de metodo exportarHtmlCliente ---


    /**
     * Importa el JSON de respuesta del cliente y actualiza la selección.
     *
     * @param respuestaJson Path al archivo JSON devuelto por el cliente.
     * @return ImportReport con el resumen, o null si hubo error.
     */
    public ImportReport cargarRespuestaCliente(Path respuestaJson) {
        logger.info("[ClientController] Cargando respuesta del cliente: {}", respuestaJson);
        ProjectModel project = projectManager != null ? projectManager.getCurrentProject() : null;
        if (project == null) {
            logger.error("[ClientController] No hay proyecto activo.");
            return null;
        }
        if (clientResponseImporter == null) {
            clientResponseImporter = new ClientResponseImporter();
        }
        try {
            ImportReport report = clientResponseImporter.importar(project, respuestaJson);
            projectManager.notificarModificacion();
            activarVistaCliente();
            logger.info("[ClientController] {}", report.resumen());
            return report;
        } catch (java.io.IOException e) {
            logger.error("[ClientController] Error importando respuesta: {}", e.getMessage(), e);
            return null;
        }
    } // --- Fin de metodo cargarRespuestaCliente ---


    @Override
    public void navegarSiguiente() {
        // Implementación futura
    } // --- Fin de metodo navegarSiguiente ---


    @Override
    public void navegarAnterior() {
        // Implementación futura
    } // --- Fin de metodo navegarAnterior ---


    @Override
    public void navegarPrimero() {
        // Implementación futura
    } // --- Fin de metodo navegarPrimero ---


    @Override
    public void navegarUltimo() {
        // Implementación futura
    } // --- Fin de metodo navegarUltimo ---


    @Override
    public void navegarBloqueAnterior() {
        // Implementación futura
    } // --- Fin de metodo navegarBloqueAnterior ---


    @Override
    public void navegarBloqueSiguiente() {
        // Implementación futura
    } // --- Fin de metodo navegarBloqueSiguiente ---


    @Override
    public void aplicarZoomConRueda(MouseWheelEvent e) {
        // Implementación futura
    } // --- Fin de metodo aplicarZoomConRueda ---


    @Override
    public void aplicarPan(int deltaX, int deltaY) {
        // Implementación futura
    } // --- Fin de metodo aplicarPan ---


    @Override
    public void iniciarPaneo(MouseEvent e) {
        // Implementación futura
    } // --- Fin de metodo iniciarPaneo ---


    @Override
    public void solicitarRefresco() {
        // Implementación futura
    } // --- Fin de metodo solicitarRefresco ---


    @Override
    public void aumentarTamanoMiniaturas() {
        // Implementación futura
    } // --- Fin de metodo aumentarTamanoMiniaturas ---


    @Override
    public void reducirTamanoMiniaturas() {
        // Implementación futura
    } // --- Fin de metodo reducirTamanoMiniaturas ---


} // --- Fin de clase ClientController ---

