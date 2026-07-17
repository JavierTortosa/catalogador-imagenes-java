package controlador.services;

import controlador.GeneralController;
import controlador.ProjectController;
import controlador.VisorController;
import controlador.managers.interfaces.IProjectManager;
import javafx.application.Platform;
import modelo.ListContext;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Servicio encargado del ciclo de vida de los proyectos.
 * Centraliza la lógica de Nuevo, Abrir, Guardar, Guardar Como y Eliminar.
 */
public class ProjectLifecycleService {
    private static final Logger logger = LoggerFactory.getLogger(ProjectLifecycleService.class);

    private final ProjectController projectController;
    private final GeneralController generalController;
    private final VisorController visorController;
    private final VisorModel model;
    private final ConfigurationManager configuration;

    public enum UserChoice {
        SAVE, DONT_SAVE, CANCEL
    }

    public ProjectLifecycleService(ProjectController projectController, GeneralController generalController,
                                   VisorController visorController, VisorModel model,
                                   ConfigurationManager configuration) {
        this.projectController = projectController;
        this.generalController = generalController;
        this.visorController = visorController;
        this.model = model;
        this.configuration = configuration;
    }

    // ==================== NUEVO PROYECTO ====================

    public void handleNewProject() {
        if (promptToSaveChangesIfNecessary() == UserChoice.CANCEL) {
            return;
        }
        projectController.solicitarNuevoProyecto();
    }

    // ==================== ABRIR PROYECTO ====================

    public void handleOpenProject() {
        if (promptToSaveChangesIfNecessary() == UserChoice.CANCEL) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Abrir Proyecto");
        IProjectManager pm = projectController.getProjectManager();
        fileChooser.setCurrentDirectory(pm.getCarpetaBaseProyectos().toFile());
        javax.swing.filechooser.FileNameExtensionFilter filter =
                new javax.swing.filechooser.FileNameExtensionFilter("Archivos de Proyecto (*.prj)", "prj");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(visorController.getView());
        if (result == JFileChooser.APPROVE_OPTION) {
            Path selectedFile = fileChooser.getSelectedFile().toPath();
            projectController.solicitarAbrirProyecto(selectedFile);
        }
    }

    // ==================== GUARDAR PROYECTO ====================

    public void handleSaveProject() {
        IProjectManager pm = projectController.getProjectManager();
        if (pm.getArchivoProyectoActivo() == null) {
            handleSaveProjectAs();
        } else {
            projectController.solicitarGuardarProyecto();
        }
    }

    // ==================== GUARDAR PROYECTO COMO ====================

    public void handleSaveProjectAs() {
        projectController.solicitarGuardarProyectoComo();
    }

    // ==================== ELIMINAR PROYECTO ====================

    public void handleDeleteProject() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar Proyecto a Eliminar");

        IProjectManager pm = projectController.getProjectManager();
        Path dirInicial = pm.getCarpetaBaseProyectos();
        if (dirInicial != null) {
            fileChooser.setCurrentDirectory(dirInicial.toFile());
        }

        javax.swing.filechooser.FileNameExtensionFilter filter =
                new javax.swing.filechooser.FileNameExtensionFilter("Archivos de Proyecto (*.prj)", "prj");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showDialog(visorController.getView(), "Eliminar Seleccionado");

        if (result == JFileChooser.APPROVE_OPTION) {
            Path archivoAEliminar = fileChooser.getSelectedFile().toPath();

            int confirm = JOptionPane.showConfirmDialog(
                    visorController.getView(),
                    "¿Estás ABSOLUTAMENTE SEGURO de que quieres eliminar el proyecto '"
                            + archivoAEliminar.getFileName() + "'?\n" +
                            "Esta acción es irreversible y borrará el archivo del disco.",
                    "Confirmar Eliminación Permanente",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    Files.delete(archivoAEliminar);
                    logger.info("Proyecto eliminado exitosamente: {}", archivoAEliminar);

                    JOptionPane.showMessageDialog(visorController.getView(),
                            "El proyecto ha sido eliminado.", "Eliminación Completada",
                            JOptionPane.INFORMATION_MESSAGE);

                    if (Objects.equals(pm.getArchivoProyectoActivo(), archivoAEliminar)) {
                        pm.nuevoProyecto();
                        generalController.cambiarModoDeTrabajo(WorkMode.VISUALIZADOR);
                    }

                } catch (java.io.IOException ex) {
                    logger.error("Error al intentar eliminar el archivo de proyecto: " + archivoAEliminar, ex);
                    JOptionPane.showMessageDialog(visorController.getView(),
                            "No se pudo eliminar el archivo del proyecto.\nError: " + ex.getMessage(),
                            "Error de Eliminación", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    // ==================== PROMPT DE GUARDADO ====================

    public UserChoice promptToSaveChangesIfNecessary() {
        IProjectManager pm = projectController.getProjectManager();
        if (pm == null || !pm.hayCambiosSinGuardar()) {
            return UserChoice.DONT_SAVE;
        }

        projectController.sincronizarModeloConUI();
        projectController.sincronizarArchivosAsociadosConModelo();
        projectController.sincronizarDescripcionDesdeUI();

        String[] options = { "Guardar", "No Guardar", "Cancelar" };
        int result = JOptionPane.showOptionDialog(
                visorController.getView(),
                "El proyecto '" + pm.getNombreProyectoActivo() + "' tiene cambios sin guardar. ¿Qué deseas hacer?",
                "Cambios sin Guardar",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        switch (result) {
            case JOptionPane.YES_OPTION:
                handleSaveProject();
                return pm.hayCambiosSinGuardar() ? UserChoice.CANCEL : UserChoice.SAVE;
            case JOptionPane.NO_OPTION:
                return UserChoice.DONT_SAVE;
            default:
                return UserChoice.CANCEL;
        }
    }

    // ==================== APAGADO DE APLICACIÓN ====================

    public void handleApplicationShutdown() {
        logger.info("--- [ProjectLifecycleService] Gestionando el cierre de la aplicación ---");

        UserChoice choice = promptToSaveChangesIfNecessary();

        if (choice == UserChoice.CANCEL) {
            logger.info("  -> Cierre de la aplicación CANCELADO por el usuario.");
            return;
        }

        IProjectManager pm = projectController.getProjectManager();

        if (choice == UserChoice.DONT_SAVE && pm.hayCambiosSinGuardar()) {
            logger.info("  -> Usuario eligió no guardar. Creando sesión de recuperación...");
            if (pm.getCurrentProject() != null) {
                pm.getCurrentProject().setLastWorkMode(model.getCurrentWorkMode().name());
            }
            Path recoveryPath = pm.guardarSesionDeRecuperacion();
            if (recoveryPath != null) {
                configuration.setString(ConfigKeys.PROYECTO_RECUPERACION_PENDIENTE,
                        recoveryPath.toAbsolutePath().toString());
            }
        } else {
            configuration.setString(ConfigKeys.PROYECTO_RECUPERACION_PENDIENTE, "");
        }

        if (model != null && configuration != null) {
            logger.debug("  -> Guardando estado de la sesión de exploración...");
            ListContext visualizadorContext = model.getVisualizadorListContext();

            if (visualizadorContext != null) {
                Path ultimaCarpeta = visualizadorContext.getCarpetaRaizContexto();
                String ultimaImagenKey = visualizadorContext.getSelectedImageKey();

                String carpetaParaGuardar = (ultimaCarpeta != null) ? ultimaCarpeta.toAbsolutePath().toString() : "";
                configuration.setString(ConfigKeys.INICIO_CARPETA, carpetaParaGuardar);
                logger.debug("    -> {} actualizada a: {}", ConfigKeys.INICIO_CARPETA, carpetaParaGuardar);

                String imagenParaGuardar = (ultimaImagenKey != null) ? ultimaImagenKey : "";
                configuration.setString(ConfigKeys.INICIO_IMAGEN, imagenParaGuardar);
                logger.debug("    -> {} actualizada a: {}", ConfigKeys.INICIO_IMAGEN, imagenParaGuardar);
            } else {
                logger.warn("  -> No se pudo obtener el contexto del visualizador para guardar el estado de la sesión.");
            }
        }

        logger.debug("  -> Guardando estado final de la ventana y configuración...");
        visorController.guardarEstadoVentanaEnConfig();

        try {
            configuration.guardarConfiguracion(configuration.getConfig());
        } catch (java.io.IOException e) {
            logger.error("### ERROR FATAL AL GUARDAR CONFIGURACIÓN DURANTE EL CIERRE: " + e.getMessage());
        }

        logger.info("  -> Finalizando recursos (base de datos y ejecutor de tareas)...");
        visorController.finalizarRecursosAlCerrar();

        logger.info("--- Apagado limpio completado. Saliendo de la JVM. ---\n\n");

        Platform.exit();
        System.exit(0);
    }
}
