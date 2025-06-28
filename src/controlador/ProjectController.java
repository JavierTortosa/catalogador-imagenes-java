package controlador;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import controlador.managers.interfaces.IProjectManager;
import controlador.managers.interfaces.IViewManager;
import controlador.managers.interfaces.IZoomManager;
import controlador.utils.ComponentRegistry;
import modelo.ListContext; 
import modelo.VisorModel;
import vista.VisorView;
import vista.panels.ImageDisplayPanel;

public class ProjectController {

    private IProjectManager projectManager;
    private IViewManager viewManager;
    private ComponentRegistry registry;
    private IZoomManager zoomManager;
    private VisorView view;
    private Map<String, Action> actionMap;
    private VisorModel model;
    private VisorController controllerRef;

    public ProjectController() {
        System.out.println("[ProjectController] Instancia creada.");
    }

    // Getter para que VisorController pueda acceder a sí mismo
    public VisorController getController() {
        return this.controllerRef;
    }

    /**
     * Configura los listeners para los componentes de la vista de proyecto.
     * (Sin cambios, tu versión ya es correcta)
     */
    public void configurarListeners() {
        if (registry == null) {
            System.err.println("ERROR [ProjectController]: No se pueden configurar listeners porque ComponentRegistry es nulo.");
            return;
        }
        
        JList<String> projectList = registry.get("list.proyecto.nombres");
        
        if (projectList != null) {
            projectList.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting() && model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
                        String selectedKey = projectList.getSelectedValue();
                        model.setSelectedImageKey(selectedKey);
                        actualizarImagenVistaProyecto();
                    }
                }
            });
            System.out.println("[ProjectController] Listener añadido a 'list.proyecto.nombres'.");
        } else {
             System.err.println("WARN [ProjectController]: No se encontró 'list.proyecto.nombres' en el registro al configurar listeners.");
        }
    }


    /**
     * Prepara los datos para el modo proyecto en el ListContext correspondiente del modelo.
     * No cambia de vista ni de modo, solo prepara los datos.
     * @return true si el proyecto tiene imágenes y está listo para ser mostrado, false si no.
     */
    public boolean prepararDatosProyecto() {
        System.out.println("  [ProjectController] Preparando datos para el modo proyecto...");
        if (projectManager == null || model == null) {
            System.err.println("ERROR CRÍTICO [prepararDatosProyecto]: ProjectManager o Model nulos.");
            return false;
        }

        List<java.nio.file.Path> imagenesMarcadas = projectManager.getImagenesMarcadas();
        if (imagenesMarcadas.isEmpty()) {
            JOptionPane.showMessageDialog(view, "No hay imágenes marcadas en el proyecto actual.", "Proyecto Vacío", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        DefaultListModel<String> modeloProyecto = new DefaultListModel<>();
        Map<String, Path> mapaRutasProyecto = new HashMap<>();
        Path carpetaRaiz = model.getCarpetaRaizActual();
        if (carpetaRaiz == null) {
            JOptionPane.showMessageDialog(view, "No se puede entrar al modo proyecto sin una carpeta base cargada.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        for (java.nio.file.Path rutaAbsoluta : imagenesMarcadas) {
            try {
                Path rutaRelativa = carpetaRaiz.relativize(rutaAbsoluta);
                String clave = rutaRelativa.toString().replace("\\", "/");
                modeloProyecto.addElement(clave);
                mapaRutasProyecto.put(clave, rutaAbsoluta);
            } catch (IllegalArgumentException e) {
                 System.err.println("WARN [prepararDatosProyecto]: No se pudo relativizar la ruta marcada: " + rutaAbsoluta);
            }
        }

        // Obtenemos el contexto específico del proyecto y lo actualizamos.
        // El modo activo de la aplicación aún no ha cambiado.
        ListContext proyectoContext = model.getProyectoListContext();
        proyectoContext.actualizarContextoCompleto(modeloProyecto, mapaRutasProyecto);
        
        System.out.println("    -> Datos del proyecto preparados en proyectoListContext.");
        return true;
    }
    
    
    /**
     * Actualiza la UI de la vista de proyecto para que refleje el estado
     * actual del proyectoListContext en el modelo.
     * Este método se llama DESPUÉS de que el modo ya ha cambiado a PROYECTO.
     */
    public void activarVistaProyecto() {
        System.out.println("  [ProjectController] Activando la UI de la vista de proyecto...");
        if (registry == null || model == null) {
            System.err.println("ERROR [ProjectController.activarVistaProyecto]: Dependencias nulas.");
            return;
        }
        
        // Obtener el modelo del ListContext del proyecto (que ya fue poblado por prepararDatosProyecto())
        DefaultListModel<String> modeloProyecto = model.getModeloLista(); // Obtiene el del proyecto porque model.currentWorkMode ya es PROYECTO

        JList<String> projectList = registry.get("list.proyecto.nombres");

        // INICIO DEL CAMBIO
        // Deshabilitar temporalmente los ListSelectionListeners de projectList
        javax.swing.event.ListSelectionListener[] listeners = null;
        if (projectList != null) {
            listeners = projectList.getListSelectionListeners();
            for (javax.swing.event.ListSelectionListener l : listeners) {
                projectList.removeListSelectionListener(l);
            }
        }
        // FIN DEL CAMBIO

        if (projectList != null) {
            projectList.setModel(modeloProyecto); // Asignar el modelo a la JList del proyecto
        }

        // Restaurar el título del panel de la lista del proyecto
        JPanel panelProyectoLista = registry.get("panel.proyecto.lista");
        if (panelProyectoLista != null && panelProyectoLista.getBorder() instanceof javax.swing.border.TitledBorder) {
            ((javax.swing.border.TitledBorder) panelProyectoLista.getBorder()).setTitle("Imágenes del Proyecto: " + modeloProyecto.getSize());
            panelProyectoLista.repaint();
        }

        // INICIO DEL CAMBIO: Lógica de restauración de selección
        // Obtener la clave seleccionada que se guardó en el ListContext del proyecto.
        String claveGuardada = model.getSelectedImageKey();
        System.out.println("### DEBUG: Restaurando selección del proyecto. Clave guardada en contexto PROYECTO: '" + claveGuardada + "'");
        
        int indiceARestaurar = -1;
        if (claveGuardada != null && !claveGuardada.isEmpty()) {
            indiceARestaurar = modeloProyecto.indexOf(claveGuardada);
        }

        // Si la clave no se encontró o no había, y la lista no está vacía, seleccionar el índice 0.
        if (indiceARestaurar == -1 && !modeloProyecto.isEmpty()) {
            indiceARestaurar = 0;
        }

        // Aplicar la selección al JList y al modelo si es necesario.
        // También llamar a la actualización de la imagen.
        if (indiceARestaurar != -1) {
            if (projectList != null) {
                projectList.setSelectedIndex(indiceARestaurar);
                projectList.ensureIndexIsVisible(indiceARestaurar);
            }
            model.setSelectedImageKey(modeloProyecto.getElementAt(indiceARestaurar)); // Asegurar que el modelo tenga la clave
            actualizarImagenVistaProyecto(); // Cargar la imagen seleccionada
        } else {
            // Si la lista está vacía o no hay nada que seleccionar, limpiar el panel
            model.setSelectedImageKey(null);
            actualizarImagenVistaProyecto(); // Esto limpiará el panel
        }
        // FIN DEL CAMBIO
        
        // Re-habilitar los listeners después de setModel() y setSelectedIndex()
        if (projectList != null && listeners != null) {
            for (javax.swing.event.ListSelectionListener l : listeners) {
                projectList.addListSelectionListener(l);
            }
        }
        
        // El viewManager.cambiarAVista y sincronizarEstadoBotonesDeModo se hacen en VisorController.entrarModo()
        // No es necesario duplicarlos aquí.
        
        System.out.println("  [ProjectController] UI de la vista de proyecto activada.");
        
    }// FIN del metodo activarVistaProyecto
    



    
    private void actualizarImagenVistaProyecto() {
        if (model.getCurrentWorkMode() != VisorModel.WorkMode.PROYECTO) return;

        ImageDisplayPanel projectDisplayPanel = registry.get("panel.proyecto.display");
        if (projectDisplayPanel == null || zoomManager == null || model == null) {
            System.err.println("ERROR CRÍTICO [actualizarImagenVistaProyecto]: Dependencias nulas.");
            return;
        }

        String claveSeleccionada = model.getSelectedImageKey();
        System.out.println("  [ProjectController] Actualizando imagen de proyecto para la clave: " + claveSeleccionada);

        if (claveSeleccionada == null) {
            projectDisplayPanel.limpiar();
            model.setCurrentImage(null);
            return;
        }

        Path rutaImagen = model.getRutaCompleta(claveSeleccionada);
        if (rutaImagen == null) {
            projectDisplayPanel.mostrarError("Ruta no encontrada para:\n" + claveSeleccionada, null);
            model.setCurrentImage(null);
            return;
        }

        new javax.swing.SwingWorker<java.awt.image.BufferedImage, Void>() {
            @Override
            protected java.awt.image.BufferedImage doInBackground() throws Exception {
                SwingUtilities.invokeLater(() -> projectDisplayPanel.mostrarCargando("Cargando: " + rutaImagen.getFileName()));
                return javax.imageio.ImageIO.read(rutaImagen.toFile());
            }

            @Override
            protected void done() {
                if (model.getCurrentWorkMode() != VisorModel.WorkMode.PROYECTO ||
                    !Objects.equals(claveSeleccionada, model.getSelectedImageKey())) {
                    return;
                }
                
                try {
                    java.awt.image.BufferedImage imagen = get();
                    if (imagen != null) {
                        model.setCurrentImage(imagen);
                        projectDisplayPanel.setImagen(imagen);
                        zoomManager.aplicarModoDeZoom(model.getCurrentZoomMode());
                        
                        if (controllerRef != null) {
                            controllerRef.sincronizarEstadoVisualBotonesYRadiosZoom();
                        }
                    } else {
                        throw new java.io.IOException("No se pudo decodificar la imagen.");
                    }
                } catch (Exception e) {
                    System.err.println("ERROR [ProjectController]: No se pudo cargar la imagen del proyecto: " + rutaImagen);
                    projectDisplayPanel.mostrarError("Error al cargar:\n" + rutaImagen.getFileName().toString(), null);
                    model.setCurrentImage(null);
                }
            }
        }.execute();
    }// --- FIN del metodo actualizarImagenVistaProyecto ---

    // --- Setters (sin cambios) ---
    public void setProjectManager(IProjectManager projectManager) {this.projectManager = Objects.requireNonNull(projectManager);}
    public void setViewManager(IViewManager viewManager) { this.viewManager = Objects.requireNonNull(viewManager); }
    public void setRegistry(ComponentRegistry registry) { this.registry = Objects.requireNonNull(registry); }
    public void setZoomManager(IZoomManager zoomManager) { this.zoomManager = Objects.requireNonNull(zoomManager); }
    public void setView(VisorView view) { this.view = Objects.requireNonNull(view); }
    public void setActionMap(Map<String, Action> actionMap) { this.actionMap = Objects.requireNonNull(actionMap); }
    public void setModel(VisorModel model) { this.model = Objects.requireNonNull(model); }
    public void setController(VisorController controller) { this.controllerRef = Objects.requireNonNull(controller); }

}// --- FIN de la clase ProjectController ---

