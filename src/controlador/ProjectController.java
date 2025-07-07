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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import controlador.interfaces.IModoController; // <-- NUEVO: Importación de la interfaz
import controlador.managers.interfaces.IListCoordinator; // <-- NUEVO: Importación
import controlador.managers.interfaces.IProjectManager;
import controlador.managers.interfaces.IViewManager;
import controlador.managers.interfaces.IZoomManager;
import controlador.utils.ComponentRegistry;
import modelo.ListContext;
import modelo.VisorModel;
import vista.VisorView;
import vista.panels.ImageDisplayPanel;

public class ProjectController implements IModoController { // <-- MODIFICADO: implementa la interfaz

    // --- Dependencias ---
    private IProjectManager projectManager;
    private IViewManager viewManager;
    private ComponentRegistry registry;
    private IZoomManager zoomManager;
    private IListCoordinator listCoordinator; // <-- NUEVO: Referencia al coordinador de listas
    private VisorView view;
    private Map<String, Action> actionMap;
    private VisorModel model;
    private VisorController controllerRef;

    public ProjectController() {
        System.out.println("[ProjectController] Instancia creada.");
    } // --- Fin del método ProjectController (constructor) ---

    // ... (El resto de tus métodos actuales como configurarListeners, prepararDatosProyecto, etc. se mantienen igual)
    // ... (Los pego aquí para que tengas el código completo de la clase)
    
    public VisorController getController() {
        return this.controllerRef;
    } // --- Fin del método getController ---

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
                        // Delega la selección al ListCoordinator para mantener la lógica centralizada
                        if (listCoordinator != null) {
                            int indice = ((DefaultListModel<String>)projectList.getModel()).indexOf(selectedKey);
                            listCoordinator.seleccionarImagenPorIndice(indice);
                        }
                    }
                }
            });
            System.out.println("[ProjectController] Listener añadido a 'list.proyecto.nombres'.");
        } else {
             System.err.println("WARN [ProjectController]: No se encontró 'list.proyecto.nombres' en el registro al configurar listeners.");
        }
    } // --- Fin del método configurarListeners ---

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

        ListContext proyectoContext = model.getProyectoListContext();
        proyectoContext.actualizarContextoCompleto(modeloProyecto, mapaRutasProyecto);
        
        System.out.println("    -> Datos del proyecto preparados en proyectoListContext.");
        return true;
    } // --- Fin del método prepararDatosProyecto ---
    
    public void activarVistaProyecto() {
        System.out.println("  [ProjectController] Activando la UI de la vista de proyecto...");
        if (registry == null || model == null) {
            System.err.println("ERROR [ProjectController.activarVistaProyecto]: Dependencias nulas.");
            return;
        }
        
        DefaultListModel<String> modeloProyecto = model.getModeloLista(); 
        JList<String> projectList = registry.get("list.proyecto.nombres");

        javax.swing.event.ListSelectionListener[] listeners = null;
        if (projectList != null) {
            listeners = projectList.getListSelectionListeners();
            for (javax.swing.event.ListSelectionListener l : listeners) {
                projectList.removeListSelectionListener(l);
            }
        }

        if (projectList != null) {
            projectList.setModel(modeloProyecto);
        }

        JPanel panelProyectoLista = registry.get("panel.proyecto.lista");
        if (panelProyectoLista != null && panelProyectoLista.getBorder() instanceof javax.swing.border.TitledBorder) {
            ((javax.swing.border.TitledBorder) panelProyectoLista.getBorder()).setTitle("Imágenes del Proyecto: " + modeloProyecto.getSize());
            panelProyectoLista.repaint();
        }

        String claveGuardada = model.getSelectedImageKey();
        int indiceARestaurar = -1;
        if (claveGuardada != null && !claveGuardada.isEmpty()) {
            indiceARestaurar = modeloProyecto.indexOf(claveGuardada);
        }

        if (indiceARestaurar == -1 && !modeloProyecto.isEmpty()) {
            indiceARestaurar = 0;
        }

        if (listCoordinator != null) {
            listCoordinator.reiniciarYSeleccionarIndice(indiceARestaurar);
        }
        
        if (projectList != null && listeners != null) {
            for (javax.swing.event.ListSelectionListener l : listeners) {
                projectList.addListSelectionListener(l);
            }
        }
        
        System.out.println("  [ProjectController] UI de la vista de proyecto activada.");
    } // --- Fin del método activarVistaProyecto ---

    private void actualizarImagenVistaProyecto() {
        if (model.getCurrentWorkMode() != VisorModel.WorkMode.PROYECTO) return;

        ImageDisplayPanel projectDisplayPanel = registry.get("panel.proyecto.display");
        if (projectDisplayPanel == null || model == null || controllerRef == null) {
            System.err.println("ERROR CRÍTICO [actualizarImagenVistaProyecto]: Dependencias nulas.");
            return;
        }

        String claveSeleccionada = model.getSelectedImageKey();
        if (claveSeleccionada == null) {
            model.setCurrentImage(null);
            projectDisplayPanel.limpiar();
            return;
        }
        controllerRef.actualizarImagenPrincipal(model.getModeloLista().indexOf(claveSeleccionada));
    } // --- Fin del método actualizarImagenVistaProyecto ---


    // --- Setters de Dependencias ---
    public void setProjectManager(IProjectManager projectManager) {this.projectManager = Objects.requireNonNull(projectManager);}
    public void setViewManager(IViewManager viewManager) { this.viewManager = Objects.requireNonNull(viewManager); }
    public void setRegistry(ComponentRegistry registry) { this.registry = Objects.requireNonNull(registry); }
    public void setZoomManager(IZoomManager zoomManager) { this.zoomManager = Objects.requireNonNull(zoomManager); }
    public void setListCoordinator(IListCoordinator listCoordinator) { this.listCoordinator = Objects.requireNonNull(listCoordinator); } // <-- NUEVO SETTER
    public void setView(VisorView view) { this.view = Objects.requireNonNull(view); }
    public void setActionMap(Map<String, Action> actionMap) { this.actionMap = Objects.requireNonNull(actionMap); }
    public void setModel(VisorModel model) { this.model = Objects.requireNonNull(model); }
    public void setController(VisorController controller) { this.controllerRef = Objects.requireNonNull(controller); }


// ****************************************************************** --- INICIO DE LA IMPLEMENTACIÓN DE IModoController ---

    @Override
    public void navegarSiguiente() {
        if (listCoordinator != null) {
            listCoordinator.seleccionarSiguiente();
        }
    } // --- Fin del método navegarSiguiente ---

    @Override
    public void navegarAnterior() {
        if (listCoordinator != null) {
            listCoordinator.seleccionarAnterior();
        }
    } // --- Fin del método navegarAnterior ---

    @Override
    public void navegarPrimero() {
        if (listCoordinator != null) {
            listCoordinator.seleccionarPrimero();
        }
    } // --- Fin del método navegarPrimero ---

    @Override
    public void navegarUltimo() {
        if (listCoordinator != null) {
            listCoordinator.seleccionarUltimo();
        }
    } // --- Fin del método navegarUltimo ---

    @Override
    public void navegarBloqueAnterior() {
        if (listCoordinator != null) {
            listCoordinator.seleccionarBloqueAnterior();
        }
    } // --- Fin del método navegarBloqueAnterior ---

    @Override
    public void navegarBloqueSiguiente() {
        if (listCoordinator != null) {
            listCoordinator.seleccionarBloqueSiguiente();
        }
    } // --- Fin del método navegarBloqueSiguiente ---

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
    
// ****************************************************************** --- FIN DE LA IMPLEMENTACIÓN DE IModoController ---
    
    
} // --- FIN de la clase ProjectController ---
