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

        // Ya no dependemos de model.getCarpetaRaizActual(). El proyecto es agnóstico a la carpeta.
        // Las rutas en el proyecto ya son absolutas. Usaremos la ruta absoluta como la "clave"
        // para el mapa, asegurando unicidad y acceso directo.
        
        for (java.nio.file.Path rutaAbsoluta : imagenesMarcadas) {
            // La clave será la ruta absoluta convertida a String, con separadores normalizados.
            String clave = rutaAbsoluta.toString().replace("\\", "/");
            
            modeloProyecto.addElement(clave);
            mapaRutasProyecto.put(clave, rutaAbsoluta);
        }

        ListContext proyectoContext = model.getProyectoListContext();
        proyectoContext.actualizarContextoCompleto(modeloProyecto, mapaRutasProyecto);
        
        System.out.println("    -> Datos del proyecto preparados en proyectoListContext. Total de imágenes: " + modeloProyecto.getSize());
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
        
        poblarListaDescartes();
        
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
    
// ******************************************************************************************** GESTION DEL MODO PROYECTO
    
    
    
    /**
     * Puebla la JList de descartes con los datos actuales del ProjectManager.
     * Este método debe ser llamado cuando se activa la vista de proyecto.
     */
    public void poblarListaDescartes() {
        if (registry == null || projectManager == null) {
            System.err.println("WARN [poblarListaDescartes]: Registry o ProjectManager nulos.");
            return;
        }

        JList<String> listaDescartesUI = registry.get("list.proyecto.descartes");
        if (listaDescartesUI == null) {
            System.err.println("WARN [poblarListaDescartes]: JList 'list.proyecto.descartes' no encontrada en el registro.");
            return;
        }

        List<Path> imagenesDescartadas = projectManager.getImagenesDescartadas();
        DefaultListModel<String> modeloDescartes = new DefaultListModel<>();

        for (Path rutaAbsoluta : imagenesDescartadas) {
            String clave = rutaAbsoluta.toString().replace("\\", "/");
            modeloDescartes.addElement(clave);
        }

        listaDescartesUI.setModel(modeloDescartes);
        System.out.println("  [ProjectController] Lista de descartes actualizada en la UI. Total: " + modeloDescartes.getSize());
    } // --- Fin del método poblarListaDescartes ---

    /**
     * Mueve la imagen actualmente seleccionada en la lista principal a la lista de descartes.
     */
    public void moverSeleccionActualADescartes() {
        if (model == null || projectManager == null) return;

        String claveSeleccionada = model.getSelectedImageKey();
        if (claveSeleccionada == null || claveSeleccionada.isEmpty()) {
            System.out.println("  [ProjectController] No hay imagen seleccionada para mover a descartes.");
            return;
        }

        Path rutaAbsoluta = model.getRutaCompleta(claveSeleccionada);
        if (rutaAbsoluta != null) {
            projectManager.moverAdescartes(rutaAbsoluta);
            // Después de mover, refrescamos ambas listas en la UI
            refrescarListasDeProyecto();
        }
    } // --- Fin del método moverSeleccionActualADescartes ---

    /**
     * Mueve la imagen actualmente seleccionada en la lista de descartes de vuelta a la selección principal.
     */
    public void restaurarDesdeDescartes() {
        if (registry == null || projectManager == null) return;
        
        JList<String> listaDescartesUI = registry.get("list.proyecto.descartes");
        if (listaDescartesUI == null) return;

        String claveSeleccionada = listaDescartesUI.getSelectedValue();
        if (claveSeleccionada == null || claveSeleccionada.isEmpty()) {
            System.out.println("  [ProjectController] No hay imagen seleccionada en descartes para restaurar.");
            return;
        }

        // Como la clave es la ruta absoluta, podemos convertirla directamente a Path
        Path rutaAbsoluta = java.nio.file.Paths.get(claveSeleccionada);
        projectManager.restaurarDeDescartes(rutaAbsoluta);
        
        // Después de restaurar, refrescamos ambas listas en la UI
        refrescarListasDeProyecto();
    } // --- Fin del método restaurarDesdeDescartes ---

    /**
     * Refresca el contenido de las listas de "Selección Actual" y "Descartes"
     * para que reflejen el estado actual del ProjectManager.
     */
    private void refrescarListasDeProyecto() {
        System.out.println("  [ProjectController] Refrescando ambas listas del proyecto...");
        
        // Refrescar la lista de selección principal
        // El método prepararDatosProyecto ya hace esto, así que lo reutilizamos
        prepararDatosProyecto(); 
        
        // Lo activamos de nuevo para que la JList coja el nuevo modelo
        activarVistaProyecto(); 

        // Refrescar la lista de descartes
        poblarListaDescartes();
    } // --- Fin del método refrescarListasDeProyecto ---
    
// ************************************************************************************ FIN DE  GESTION DEL MODO PROYECTO    
    
    
    /**
     * Orquesta la operación de alternar el estado de marca de la imagen
     * seleccionada DENTRO DEL MODO PROYECTO.
     */
    public void solicitudAlternarMarcaImagen() {
        System.out.println("  [ProjectController] Procesando solicitud para alternar marca...");
        if (model == null || projectManager == null || controllerRef == null) {
            System.err.println("ERROR CRÍTICO [ProjectController.solicitudAlternarMarcaImagen]: Dependencias nulas.");
            return;
        }
        
        String claveActual = model.getSelectedImageKey();
        if (claveActual == null || claveActual.isEmpty()) {
            System.out.println("    -> No hay imagen seleccionada en el proyecto para marcar.");
            return;
        }

        Path rutaAbsoluta = model.getRutaCompleta(claveActual); 
        if (rutaAbsoluta == null) {
            System.err.println("ERROR [ProjectController.solicitudAlternarMarcaImagen]: No se pudo resolver la ruta para la clave: " + claveActual);
            return;
        }

        boolean estaAhoraMarcada = projectManager.alternarMarcaImagen(rutaAbsoluta);
        
        controllerRef.actualizarEstadoVisualBotonMarcarYBarraEstado(estaAhoraMarcada, rutaAbsoluta);
        
        System.out.println("    -> Marca alternada para: " + rutaAbsoluta + ". Nuevo estado: " + (estaAhoraMarcada ? "MARCADA" : "NO MARCADA"));
    } // --- Fin del método solicitudAlternarMarcaImagen ---
    
    
} // --- FIN de la clase ProjectController ---
