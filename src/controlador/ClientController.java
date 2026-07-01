package controlador;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.commands.AppActionCommands;
import controlador.factory.ActionFactory;
import controlador.interfaces.IModoController;
import controlador.managers.interfaces.IProjectManager;
import controlador.services.proyecto.ExportPreflightService;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import modelo.proyecto.ExportConfig;
import modelo.proyecto.ImageCheckboxOverlay;
import modelo.proyecto.ProjectImage;
import modelo.proyecto.ProjectModel;
import modelo.proyecto.SelectionState;
import servicios.ProjectManager;
import servicios.ProyectoIOException;
import servicios.ValidationService;
import servicios.cliente.ClientResponseImporter;
import servicios.cliente.ClientResponseImporter.ImportReport;
import servicios.cliente.ClientSyncService;
import servicios.cliente.WebCatalogExporter;
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
    private boolean editingActive;
    private String lastClientImageKey;
    private ActionFactory actionFactory;

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

    public void setActionFactory(ActionFactory actionFactory) {
        this.actionFactory = actionFactory;
    } // --- Fin de metodo setActionFactory ---

    public boolean isEditingActive() {
        return editingActive;
    } // --- Fin de metodo isEditingActive ---

    public void setEditingActive(boolean editingActive) {
        this.editingActive = editingActive;
        if (registry == null) return;
        // Sync toggle button
        AbstractButton btn = registry.get("button.cliente.editar");
        if (btn != null) btn.setSelected(editingActive);
        // Sync table models
        javax.swing.JTable t;
        t = registry.get("table.cliente.cliente.seleccion");
        if (t != null && t.getModel() instanceof vista.models.ClienteTableModel cm1) cm1.setEditingActive(editingActive);
        t = registry.get("table.cliente.cliente.descartes");
        if (t != null && t.getModel() instanceof vista.models.ClienteTableModel cm2) cm2.setEditingActive(editingActive);
    } // --- Fin de metodo setEditingActive ---


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
        editingActive = false;
        if (registry != null) {
            asignarModelosTablas(project);
        }
        // Registrar listener para sincronizar tablas cuando cambia la imagen desde el display
        if (visorController != null && visorController.getModel() != null) {
            visorController.getModel().setSelectedImageKeyListener(this::sincronizarSeleccionEnTablas);
        }
        // Registrar listener de selecci�n en el grid del modo cliente
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
                            sincronizarContextoNavegacion(key);
                        }
                    }
                }
            });
            gridListenerRegistered = true;
        }

        // Restaurar imagen seleccionada anteriormente, o seleccionar primera fila
        restaurarOIniciarSeleccion();

        // Ajustar botones y barra seg�n el estado del proyecto
        ajustarBotonesSegunEstado();
        actualizarBarraEstado();

        logger.info("[ClientController] Tablas del Modo Cliente refrescadas.");
    } // --- Fin de metodo activarVistaCliente ---


    /**
     * Sincroniza el contexto de navegaci&oacute;n del visor para que las flechas
     * de navegaci&oacute;n (anterior/siguiente) se habiliten correctamente
     * en modo cliente. Acepta tanto una clave de VisorModel como una ruta de archivo.
     */
    public void sincronizarContextoNavegacion(String key) {
        if (visorController == null || generalController == null) return;
        var visorModel = visorController.getModel();
        if (visorModel == null) return;
        var listContext = visorModel.getCurrentListContext();
        if (listContext == null) return;

        // Si la key no est&aacute; en el modelo de lista, buscar por ruta completa
        String modelKey = key;
        var modeloLista = listContext.getModeloLista();
        if (modeloLista == null || !modeloLista.contains(key)) {
            var rutaMap = listContext.getRutaCompletaMap();
            if (rutaMap != null && key != null) {
                String canonicalSearch = key.replace("\\", "/");
                for (var entry : rutaMap.entrySet()) {
                    if (entry.getValue() != null
                            && entry.getValue().toString().replace("\\", "/").equals(canonicalSearch)) {
                        modelKey = entry.getKey();
                        break;
                    }
                }
            }
        }

        listContext.setSelectedImageKey(modelKey);
        generalController.notificarAccionesSensiblesAlContexto();
    } // --- Fin de metodo sincronizarContextoNavegacion ---


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
                    sincronizarContextoNavegacion(rowKey);
                    return;
                }
            }
        }
        // No hay imagen previa o no se encontró: seleccionar primera fila
        t.setRowSelectionInterval(0, 0);
        t.scrollRectToVisible(t.getCellRect(0, 0, true));
        // Obtener la clave de la primera fila para inicializar el contexto de navegaci&oacute;n
        ProyectoClienteTableModel model = (ProyectoClienteTableModel) t.getModel();
        String firstKey = model.getImageKey(t.convertRowIndexToModel(0));
        if (firstKey != null) {
            sincronizarContextoNavegacion(firstKey);
        }
    } // --- Fin de metodo restaurarOIniciarSeleccion ---


    private void asignarModelosTablas(ProjectModel project) {
        JTable t;
        t = registry.get("table.cliente.proyecto.seleccion");
        if (t != null) {
            t.setModel(new vista.models.ProyectoClienteTableModel(project, true));
            vista.config.ClientTableConfig.configureProjectTable(t);
        }
        t = registry.get("table.cliente.proyecto.descartes");
        if (t != null) {
            t.setModel(new vista.models.ProyectoClienteTableModel(project, false));
            vista.config.ClientTableConfig.configureProjectTable(t);
        }
        t = registry.get("table.cliente.cliente.seleccion");
        if (t != null) {
            var m = new vista.models.ClienteTableModel(project, true);
            m.setEditingActive(editingActive);
            m.setModificationListener(() -> {
                if (projectManager != null) projectManager.notificarModificacion();
            });
            t.setModel(m);
            vista.config.ClientTableConfig.configureClientTable(t);
        }
        t = registry.get("table.cliente.cliente.descartes");
        if (t != null) {
            var m = new vista.models.ClienteTableModel(project, false);
            m.setEditingActive(editingActive);
            m.setModificationListener(() -> {
                if (projectManager != null) projectManager.notificarModificacion();
            });
            t.setModel(m);
            vista.config.ClientTableConfig.configureClientTable(t);
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

            // Sincronizar el modelo de lista del contexto de navegaci&oacute;n
            // para que las flechas del visor se habiliten en modo cliente
            var navCtx = visorController.getModel().getCurrentListContext();
            if (navCtx != null) {
                DefaultListModel<String> navModel = new DefaultListModel<>();
                for (int i = 0; i < gridModel.size(); i++) {
                    navModel.addElement(gridModel.get(i));
                }
                navCtx.setModeloLista(navModel);
            }
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


    public void ajustarBotonesSegunEstado() {
        if (actionFactory == null || actionFactory.getActionMap() == null) return;
        var map = actionFactory.getActionMap();
        ProjectModel project = projectManager != null ? projectManager.getCurrentProject() : null;
        boolean closed = project != null && project.isClientModeClosed();
        boolean shared = project != null && project.isSharedWithClient();

        // Botones que se deshabilitan cuando el modo cliente est� cerrado
        String[] disableWhenClosed = {
            AppActionCommands.CMD_CLIENTE_CARGAR_RESPUESTA,
            AppActionCommands.CMD_CLIENTE_EXPORTAR_WEB,
            AppActionCommands.CMD_CLIENTE_EXPORTAR_HTML,
            AppActionCommands.CMD_CLIENTE_CERRAR_SINCRONIZAR,
            AppActionCommands.CMD_CLIENTE_TOGGLE_EDITOR_PANEL,
            AppActionCommands.CMD_PROYECTO_MOVER_A_DESCARTES,
        };
        for (String cmd : disableWhenClosed) {
            javax.swing.Action a = map.get(cmd);
            if (a != null) a.setEnabled(!closed);
        }
    } // --- Fin de metodo ajustarBotonesSegunEstado ---


    public void actualizarBarraEstado() {
        if (registry == null) return;
        javax.swing.JLabel lbl = registry.get("label.cliente.estado");
        if (lbl == null) return;
        ProjectModel project = projectManager != null ? projectManager.getCurrentProject() : null;
        if (project == null) return;

        String text;
        if (project.isClientModeClosed()) {
            text = "Modo cliente cerrado \u2014 Solo lectura";
        } else if (editingActive) {
            text = "Modo cliente abierto \u2014 Edici\u00f3n activa";
        } else {
            text = "Modo cliente abierto \u2014 Iteraci\u00f3n " + project.getSharedIteration();
        }
        lbl.setText(text);
    } // --- Fin de metodo actualizarBarraEstado ---


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
            String selCbCode = visorController != null ? visorController.getModel().getSelectedCheckboxCode() : null;
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
                int bestViewRow = -1;
                for (int viewRow = 0; viewRow < t.getRowCount(); viewRow++) {
                    int modelRow = t.convertRowIndexToModel(viewRow);
                    String rowKey = null;
                    if (m instanceof ProyectoClienteTableModel pctm) {
                        rowKey = pctm.getImageKey(modelRow);
                    } else if (m instanceof ClienteTableModel ctm) {
                        rowKey = ctm.getImageKey(modelRow);
                        if (canonicalKey != null && canonicalKey.equals(rowKey) && selCbCode != null
                                && ctm.isChildRow(modelRow) && selCbCode.equals(ctm.getCheckboxCode(modelRow))) {
                            bestViewRow = viewRow;
                            break;
                        }
                    }
                    if (canonicalKey != null && canonicalKey.equals(rowKey) && bestViewRow < 0) {
                        bestViewRow = viewRow;
                    }
                }
                if (bestViewRow >= 0) {
                    t.setRowSelectionInterval(bestViewRow, bestViewRow);
                    t.scrollRectToVisible(t.getCellRect(bestViewRow, 0, true));
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
                + "Las im\u00e1genes sin marcar (O) se tratar\u00e1n como descartadas.\n"
                + "Las im\u00e1genes marcadas con (V) se conservar\u00e1n en la selecci\u00f3n del proyecto.",
                "Cerrar Modo Cliente",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        // Paso 1: UNDEFINED → DISCARDED (im�genes sin marcar se tratan como descartadas)
        if (project.getMasterImages() != null) {
            for (ProjectImage pi : project.getMasterImages().values()) {
                if (!pi.isEnSeleccionProyecto()) continue;
                if (pi.getEstadoCliente() == SelectionState.UNDEFINED) {
                    pi.setEstadoCliente(SelectionState.DISCARDED);
                    if (pi.getCheckboxes() != null) {
                        for (ImageCheckboxOverlay cb : pi.getCheckboxes()) {
                            if (cb.getState() == SelectionState.UNDEFINED) {
                                cb.setState(SelectionState.DISCARDED);
                            }
                        }
                    }
                }
            }
        }

        // Paso 2: sincronizar con el proyecto (SELECTED → se queda, DISCARDED → sale)
        if (clientSyncService != null && project.hasClientSelection()) {
            clientSyncService.closeAndSync(project);
        }

        // Paso 3: marcar como cerrado (sharedWithClient NO se toca)
        project.setClientModeClosed(true);
        project.setSharedWithClient(true); // asegurar que se mantiene compartido
        editingActive = false;

        // Paso 4: ocultar overlays y editor
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

        // Paso 5: actualizar estado de botones del toolbar
        ajustarBotonesSegunEstado();

        // Paso 6: refrescar tablas para reflejar los cambios
        refrescarTablas();

        // Paso 7: actualizar barra de estado
        actualizarBarraEstado();

        projectManager.guardarAArchivo();
        projectManager.notificarModificacion();

        logger.info("[ClientController] Modo cliente cerrado. Proyecto guardado.");
        JOptionPane.showMessageDialog(null,
                "Modo cliente cerrado correctamente.\n"
                + "Los datos del cliente se han conservado.\n"
                + "Usa el bot\u00f3n 'Editar' para reabrir si necesitas hacer cambios.",
                "Cliente Cerrado", JOptionPane.INFORMATION_MESSAGE);
    } // --- Fin de metodo cerrarCliente ---


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

        // Diálogo de iteración
        int iteracion = project.getSharedIteration();
        String input = JOptionPane.showInputDialog(
                registry != null ? (java.awt.Component) registry.get("frame.principal") : null,
                "N\u00famero de iteraci\u00f3n:", String.valueOf(iteracion));
        if (input == null) return;
        try {
            iteracion = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            return;
        }
        if (iteracion < 1) iteracion = 1;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar catálogo HTML para el cliente");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Archivo HTML (*.html)", "html"));
        String defaultName = (project.getProjectName() != null
                ? project.getProjectName().replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase()
                : "proyecto")
                + "_iteracion" + iteracion + ".html";
        chooser.setSelectedFile(new java.io.File(defaultName));

        if (chooser.showSaveDialog(registry != null
                ? (java.awt.Window) registry.get("frame.principal") : null)
                != JFileChooser.APPROVE_OPTION) return;

        Path outputFile = chooser.getSelectedFile().toPath();
        if (!outputFile.getFileName().toString().toLowerCase().endsWith(".html")) {
            outputFile = outputFile.resolveSibling(outputFile.getFileName() + ".html");
        }

        // Confirmar sobrescritura
        Path finalOutput = outputFile;
        if (java.nio.file.Files.exists(finalOutput)) {
            int overwrite = JOptionPane.showConfirmDialog(
                    registry != null ? (java.awt.Component) registry.get("frame.principal") : null,
                    "El archivo ya existe.\n\u00bfDeseas sobrescribirlo?",
                    "Confirmar sobrescritura", JOptionPane.YES_NO_OPTION);
            if (overwrite != JOptionPane.YES_OPTION) return;
        }

        final int iteracionFinal = iteracion;
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
                        iteracionFinal, progress -> setProgress(progress));
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

