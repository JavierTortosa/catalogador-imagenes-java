package controlador;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.nio.file.Path;
import java.nio.file.Paths;

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
import servicios.cliente.ClientSyncService.SyncReport;
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
        logger.info("[ClientController] Tablas del Modo Cliente refrescadas.");
    } // --- Fin de metodo activarVistaCliente ---


    private void asignarModelosTablas(ProjectModel project) {
        JTable t;
        t = registry.get("table.cliente.proyecto.seleccion");
        if (t != null) {
            t.setModel(new vista.models.ProyectoClienteTableModel(project, true));
            t.getColumnModel().getColumn(vista.models.ProyectoClienteTableModel.COL_CODIGO)
                    .setCellRenderer(new vista.renderers.CodeCellRenderer());
            t.getColumnModel().getColumn(vista.models.ProyectoClienteTableModel.COL_ACCIONES)
                    .setCellRenderer(new vista.renderers.ActionCellRenderer(true));
            t.getColumnModel().getColumn(vista.models.ProyectoClienteTableModel.COL_CODIGO).setMaxWidth(80);
            t.getColumnModel().getColumn(vista.models.ProyectoClienteTableModel.COL_ACCIONES).setMaxWidth(80);
        }
        t = registry.get("table.cliente.proyecto.descartes");
        if (t != null) {
            t.setModel(new vista.models.ProyectoClienteTableModel(project, false));
            t.getColumnModel().getColumn(vista.models.ProyectoClienteTableModel.COL_CODIGO)
                    .setCellRenderer(new vista.renderers.CodeCellRenderer());
            t.getColumnModel().getColumn(vista.models.ProyectoClienteTableModel.COL_ACCIONES)
                    .setCellRenderer(new vista.renderers.ActionCellRenderer(false));
            t.getColumnModel().getColumn(vista.models.ProyectoClienteTableModel.COL_CODIGO).setMaxWidth(80);
            t.getColumnModel().getColumn(vista.models.ProyectoClienteTableModel.COL_ACCIONES).setMaxWidth(80);
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
            t.getColumnModel().getColumn(vista.models.ClienteTableModel.COL_COMENTARIO)
                    .setCellRenderer(new vista.renderers.CommentCellRenderer());
            t.getColumnModel().getColumn(vista.models.ClienteTableModel.COL_COMENTARIO).setMaxWidth(50);
        }
        refrescarTablas();
    } // --- Fin de metodo asignarModelosTablas ---


    /**
     * Actualiza el estado de selección de una imagen por parte del cliente.
     * @param imageKey La ruta/clave de la imagen.
     * @param newState El nuevo estado (SELECTED, DISCARDED, UNDEFINED).
     */
    public void updateClientSelectionState(String imageKey, SelectionState newState) {
        logger.info("Actualizando estado de cliente para {} a {}", imageKey, newState);
        if (projectManager != null && projectManager.getCurrentProject() != null) {
            if (projectManager.getCurrentProject().hasClientSelection()) {
                projectManager.getCurrentProject().getClientSelection().getImages().put(imageKey, newState);
                projectManager.notificarModificacion();
                refrescarTablas();
            }
        }
    } // --- Fin de metodo updateClientSelectionState ---


    public void compartirConCliente() {
        logger.info("[ClientController] Iniciando flujo de compartir con el cliente...");
        ProjectModel project = projectManager != null ? projectManager.getCurrentProject() : null;
        if (project == null) {
            JOptionPane.showMessageDialog(null, "No hay ning\u00fan proyecto activo.",
                    "Compartir al Cliente", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.util.Map<String, String> selectedImages = project.getSelectedImages();
        if (selectedImages == null || selectedImages.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No hay im\u00e1genes seleccionadas en el proyecto.",
                    "Compartir al Cliente", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.util.Map<String, String> imageCodes = project.getImageCodes();
        imageCodes.clear();
        int numDigitos = Math.max(3, String.valueOf(selectedImages.size()).length());
        String formato = "C%0" + numDigitos + "d";
        int idx = 0;
        for (String rutaImagen : selectedImages.keySet()) {
            String codigo = String.format(formato, ++idx);
            imageCodes.put(rutaImagen, codigo);
            if (project.getExportConfigs().containsKey(rutaImagen)) {
                project.getExportConfigs().get(rutaImagen).setCodigoCatalogo(codigo);
            }
        }

        java.util.List<String> errores = ExportPreflightService.validarAsignaciones(project);
        if (!errores.isEmpty()) {
            StringBuilder msg = new StringBuilder(
                    "No se puede compartir el proyecto. Corrige los siguientes errores:\n\n");
            for (String err : errores) {
                msg.append(" \u2022 ").append(err).append("\n");
            }
            JOptionPane.showMessageDialog(null, msg.toString(), "Error al Compartir", JOptionPane.ERROR_MESSAGE);
            return;
        }

        project.setSharedWithClient(true);
        project.setSharedTimestamp(System.currentTimeMillis());
        project.setSharedIteration(1);

        Path prjPath = projectManager.getArchivoProyectoActivo();
        if (prjPath != null) {
            projectManager.guardarAArchivo();
        }

        Path prjclPath;
        if (prjPath != null) {
            String prjName = prjPath.getFileName().toString();
            prjclPath = prjPath.resolveSibling(
                    prjName.replaceAll("(?i)\\.prj$", "") + ".prjcl");
        } else {
            prjclPath = projectManager.getCarpetaBaseProyectos()
                    .resolve("compartido_" + System.currentTimeMillis() + ".prjcl");
        }
        projectManager.guardarProyectoComo(prjclPath);

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
                selectedImages.size());
        JOptionPane.showMessageDialog(null,
                "Proyecto compartido con el cliente.\n\nSe han generado " + selectedImages.size()
                        + " c\u00f3digos de cat\u00e1logo.\nModo Cliente activado.",
                "Compartir Completado", JOptionPane.INFORMATION_MESSAGE);
    } // --- Fin de metodo compartirConCliente ---


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

        if (project.hasClientSelection()) {
            java.util.Map<String, SelectionState> clientImages = project.getClientSelection().getImages();
            DefaultListModel<String> cliSelModel = (cliSel != null) ? (DefaultListModel<String>) cliSel.getModel() : null;
            DefaultListModel<String> cliDescModel = (cliDesc != null) ? (DefaultListModel<String>) cliDesc.getModel() : null;
            for (java.util.Map.Entry<String, SelectionState> entry : clientImages.entrySet()) {
                if (entry.getValue() == SelectionState.SELECTED) {
                    if (cliSelModel != null) cliSelModel.addElement(entry.getKey());
                } else if (entry.getValue() == SelectionState.DISCARDED) {
                    if (cliDescModel != null) cliDescModel.addElement(entry.getKey());
                }
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


    public void moverADescartesCliente(String imageKey) {
        logger.info("[ClientController] Moviendo {} a descartes (proyecto).", imageKey);
        if (projectManager != null && projectManager.getCurrentProject() != null) {
            ProjectModel project = projectManager.getCurrentProject();
            if (project.getSelectedImages().containsKey(imageKey)) {
                project.getSelectedImages().remove(imageKey);
                if (!project.getDiscardedImages().contains(imageKey)) {
                    project.getDiscardedImages().add(imageKey);
                }
                projectManager.notificarModificacion();
                refrescarTablas();
            }
        }
    } // --- Fin de metodo moverADescartesCliente ---


    public void restaurarDeDescartesCliente(String imageKey) {
        logger.info("[ClientController] Restaurando {} de descartes a selecci\u00f3n.", imageKey);
        if (projectManager != null && projectManager.getCurrentProject() != null) {
            ProjectModel project = projectManager.getCurrentProject();
            if (project.getDiscardedImages().contains(imageKey)) {
                project.getDiscardedImages().remove(imageKey);
                project.getSelectedImages().putIfAbsent(imageKey, "");
                projectManager.notificarModificacion();
                refrescarTablas();
            }
        }
    } // --- Fin de metodo restaurarDeDescartesCliente ---


    public VisorController getVisorController() {
        return visorController;
    } // --- Fin de metodo getVisorController ---


    /**
     * Cierra el modo cliente: sincroniza cambios al proyecto, desactiva shared,
     * vuelve a modo proyecto y guarda.
     */
    public void cerrarCliente() {
        logger.info("[ClientController] Cerrando modo cliente...");
        ProjectModel project = projectManager != null ? projectManager.getCurrentProject() : null;
        if (project == null) {
            JOptionPane.showMessageDialog(null, "No hay proyecto activo.",
                    "Cerrar Cliente", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(null,
                "\u00bfEst\u00e1s seguro de cerrar el modo cliente?\n\n"
                + "Los cambios del cliente se sincronizar\u00e1n con el proyecto.\n"
                + "Se desactivar\u00e1 el modo compartido y volver\u00e1s al modo proyecto.",
                "Cerrar Modo Cliente",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        // 1. Sync cliente → proyecto
        if (clientSyncService != null && project.hasClientSelection()) {
            clientSyncService.update(project);
        }

        // 2. Desactivar shared
        project.setSharedWithClient(false);
        project.setSharedTimestamp(0);
        project.setSharedIteration(0);

        // 3. Limpiar selecci\u00f3n de cliente
        project.getClientSelection().getImages().clear();
        project.getClientSelection().getComments().clear();
        project.getClientSelection().getCommentOverlays().clear();
        project.getClientSelection().getImageCheckboxesMap().clear();

        // 4. Resetear flags de panel
        if (generalController != null) {
            modelo.VisorModel visorModel = generalController.getModel();
            if (visorModel != null) {
                visorModel.setClienteCheckboxVisible(false);
                visorModel.setClienteEditorPanelVisible(false);
            }
        }

        // 5. Restaurar card DISPLAY_NORMAL en el wrapper
        if (registry != null) {
            java.awt.Container displayWrapper = registry.get("container.displaymodes.cliente.wrapper");
            if (displayWrapper != null && displayWrapper.getLayout() instanceof java.awt.CardLayout) {
                ((java.awt.CardLayout) displayWrapper.getLayout()).show(displayWrapper, "DISPLAY_NORMAL");
            }
        }

        // 6. Cambiar modo a PROYECTO
        if (generalController != null) {
            generalController.cambiarModoDeTrabajo(VisorModel.WorkMode.PROYECTO);
        }

        // 7. Guardar .prj
        projectManager.guardarAArchivo();
        projectManager.notificarModificacion();

        logger.info("[ClientController] Modo cliente cerrado. Proyecto guardado.");
        JOptionPane.showMessageDialog(null,
                "Modo cliente cerrado correctamente.\n"
                + "Los cambios se han sincronizado con el proyecto.\n"
                + "La exportaci\u00f3n ya est\u00e1 disponible.",
                "Cliente Cerrado", JOptionPane.INFORMATION_MESSAGE);
    } // --- Fin de metodo cerrarCliente ---


    /**
     * Cierra la revisión actual del cliente, fusionando los cambios en el proyecto.
     * @return SyncReport con el resumen de cambios, o null si no se pudo completar.
     */
    public void solicitarUpdateCliente() {
        logger.info("[ClientController] Solicitando update del cliente...");
        if (projectManager == null || projectManager.getCurrentProject() == null) {
            JOptionPane.showMessageDialog(null, "No hay proyecto activo.", "Update",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        ProjectModel project = projectManager.getCurrentProject();
        if (!project.hasClientSelection()
                || project.getClientSelection().getFechaRespuesta() == null
                || project.getClientSelection().getFechaRespuesta().isBlank()) {
            JOptionPane.showMessageDialog(null,
                    "No hay respuesta del cliente. Importa una respuesta antes de actualizar.",
                    "Update", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (clientSyncService == null) return;

        java.util.List<servicios.cliente.ClientSyncService.ConflictEntry> conflicts =
                clientSyncService.computeConflicts(project);

        if (conflicts.isEmpty()) {
            clientSyncService.update(project);
            projectManager.guardarAArchivo();
            projectManager.notificarModificacion();
            refrescarTablas();
            JOptionPane.showMessageDialog(null, "No hay conflictos. Proyecto actualizado.",
                    "Update", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFrame owner = registry != null ? registry.get("frame.principal") : null;
        vista.dialogos.SyncConflictDialog dialog = new vista.dialogos.SyncConflictDialog(owner, conflicts);
        dialog.setVisible(true);

        if (dialog.isAccepted()) {
            clientSyncService.applyResolvedConflicts(project, dialog.getResolvedConflicts());
            projectManager.guardarAArchivo();
            projectManager.notificarModificacion();
            refrescarTablas();
            JOptionPane.showMessageDialog(null,
                    "Conflictos resueltos y aplicados al proyecto.",
                    "Update", JOptionPane.INFORMATION_MESSAGE);
        }
    } // --- Fin de metodo solicitarUpdateCliente ---


    public SyncReport closeAndSyncProject() {
        logger.info("Cerrando y sincronizando proyecto de cliente...");
        if (projectManager != null && projectManager.getCurrentProject() != null && clientSyncService != null) {
            ProjectModel project = projectManager.getCurrentProject();
            SyncReport report = clientSyncService.mergeClientResponse(project, null);
            clientSyncService.closeAndSync(project, null);
            projectManager.guardarAArchivo();
            projectManager.notificarModificacion();
            return report;
        }
        return null;
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
        project.getClientSelection().getImages().clear();
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

