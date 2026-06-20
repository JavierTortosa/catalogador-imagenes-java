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
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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
    } // --- FIN del constructor ---

    public void setVisorController(VisorController visorController) {
        this.visorController = visorController;
    } // --- FIN del metodo setVisorController ---

    public void setComponentRegistry(ComponentRegistry registry) {
        this.registry = registry;
    } // --- FIN del metodo setComponentRegistry ---

    public void setProjectListCoordinator(controlador.ProjectListCoordinator projectListCoordinator) {
        this.projectListCoordinator = projectListCoordinator;
    } // --- FIN del metodo setProjectListCoordinator ---

    public void setGeneralController(GeneralController generalController) {
        this.generalController = generalController;
    } // --- FIN del metodo setGeneralController ---

    public void setProjectManager(ProjectManager projectManager) {
        this.projectManager = projectManager;
    } // --- FIN del metodo setProjectManager ---

    public void setValidationService(ValidationService validationService) {
        this.validationService = validationService;
    } // --- FIN del metodo setValidationService ---

    public void setClientSyncService(ClientSyncService clientSyncService) {
        this.clientSyncService = clientSyncService;
    } // --- FIN del metodo setClientSyncService ---

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

        // 1. Obtener JLists del registro
        JList<String> proySel = registry != null ? registry.get("list.cliente.proyecto.seleccion") : null;
        JList<String> proyDesc = registry != null ? registry.get("list.cliente.proyecto.descartes") : null;
        JList<String> cliSel = registry != null ? registry.get("list.cliente.cliente.seleccion") : null;
        JList<String> cliDesc = registry != null ? registry.get("list.cliente.cliente.descartes") : null;

        // 2. Poblar JLists
        if (proySel != null) {
            DefaultListModel<String> model = new DefaultListModel<>();
            for (String key : project.getSelectedImages().keySet()) {
                model.addElement(key);
            }
            proySel.setModel(model);
        }

        if (proyDesc != null) {
            DefaultListModel<String> model = new DefaultListModel<>();
            for (String key : project.getDiscardedImages()) {
                model.addElement(key);
            }
            proyDesc.setModel(model);
        }

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

        // 3. Añadir Listeners
        if (proySel != null) {
            proySel.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return;
                String key = proySel.getSelectedValue();
                if (key != null && visorController != null) {
                    visorController.actualizarImagenPrincipalPorPath(Paths.get(key), key);
                }
            });
        }

        if (proyDesc != null) {
            proyDesc.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return;
                String key = proyDesc.getSelectedValue();
                if (key != null && visorController != null) {
                    visorController.actualizarImagenPrincipalPorPath(Paths.get(key), key);
                }
            });
        }

        if (cliSel != null) {
            cliSel.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return;
                String key = cliSel.getSelectedValue();
                if (key != null && visorController != null) {
                    updateClientSelectionState(key, SelectionState.SELECTED);
                    visorController.actualizarImagenPrincipalPorPath(Paths.get(key), key);
                }
            });
        }

        if (cliDesc != null) {
            cliDesc.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return;
                String key = cliDesc.getSelectedValue();
                if (key != null && visorController != null) {
                    updateClientSelectionState(key, SelectionState.DISCARDED);
                    visorController.actualizarImagenPrincipalPorPath(Paths.get(key), key);
                }
            });
        }

        logger.info("[ClientController] Listas del Modo Cliente pobladas y listeners configurados.");
    } // --- FIN de metodo activarVistaCliente ---

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
                // Refresh client lists
                refrescarListasCliente();
            }
        }
    } // --- FIN de metodo updateClientSelectionState ---

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
    } // --- Fin del metodo refrescarListasCliente ---

    /**
     * Mueve el elemento seleccionado actualmente en la vista de cliente a Descartes.
     */
    public void moverADescartesCliente() {
        // Implementación futura
    } // --- FIN de metodo moverADescartesCliente ---

    /**
     * Restaura el elemento de Descartes del cliente a Selección del cliente.
     */
    public void restaurarDeDescartesCliente() {
        // Implementación futura
    } // --- FIN de metodo restaurarDeDescartesCliente ---

    /**
     * Cierra la revisión actual del cliente, fusionando los cambios en el proyecto.
     * @return SyncReport con el resumen de cambios, o null si no se pudo completar.
     */
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
    } // --- FIN de metodo closeAndSyncProject ---

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
            projectManager.abrirProyecto(ruta);
            activarVistaCliente();
        }
    } // --- FIN de metodo cargarPrjcl ---

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
        try {
            webCatalogExporter.exportar(project, outputDir, iteracion);
            logger.info("[ClientController] Exportación completada.");
        } catch (java.io.IOException e) {
            logger.error("[ClientController] Error durante la exportación: {}", e.getMessage(), e);
        }
    } // --- FIN de metodo exportarParaCliente ---

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
    } // --- FIN de metodo cargarRespuestaCliente ---

    @Override
    public void navegarSiguiente() {
        // Implementación futura
    }

    @Override
    public void navegarAnterior() {
        // Implementación futura
    }

    @Override
    public void navegarPrimero() {
        // Implementación futura
    }

    @Override
    public void navegarUltimo() {
        // Implementación futura
    }

    @Override
    public void navegarBloqueAnterior() {
        // Implementación futura
    }

    @Override
    public void navegarBloqueSiguiente() {
        // Implementación futura
    }

    @Override
    public void aplicarZoomConRueda(MouseWheelEvent e) {
        // Implementación futura
    }

    @Override
    public void aplicarPan(int deltaX, int deltaY) {
        // Implementación futura
    }

    @Override
    public void iniciarPaneo(MouseEvent e) {
        // Implementación futura
    }

    @Override
    public void solicitarRefresco() {
        // Implementación futura
    }

    @Override
    public void aumentarTamanoMiniaturas() {
        // Implementación futura
    }

    @Override
    public void reducirTamanoMiniaturas() {
        // Implementación futura
    }

} // --- FIN de clase ClientController ---
