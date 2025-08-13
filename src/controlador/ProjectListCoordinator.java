package controlador;

import java.util.List;
import java.util.Objects;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.interfaces.ContextSensitiveAction;
import controlador.managers.interfaces.IListCoordinator;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigKeys;

/**
 * Servicio especializado en coordinar la navegación y selección de las listas
 * del MODO PROYECTO. Maneja la lógica de la lista de "Selección" y "Descartes",
 * así como el cambio de foco entre ellas.
 */
public class ProjectListCoordinator implements IListCoordinator {
	
	private static final Logger logger = LoggerFactory.getLogger(ProjectListCoordinator.class);

    // --- Dependencias ---
    private final VisorModel model;
    private final VisorController controller;
    private final ComponentRegistry registry;
    private List<ContextSensitiveAction> contextSensitiveActions;

    // --- Estado Interno ---
    private boolean isSyncingUI = false;
    private int pageScrollIncrement;

    /**
     * Constructor.
     * @param model El modelo principal de datos.
     * @param controller El controlador principal para acceder a servicios como la carga de imágenes.
     * @param registry El registro para encontrar componentes de la UI.
     */
    public ProjectListCoordinator(VisorModel model, VisorController controller, ComponentRegistry registry) {
        this.model = Objects.requireNonNull(model);
        this.controller = Objects.requireNonNull(controller);
        this.registry = Objects.requireNonNull(registry);
        
        if (this.controller.getConfigurationManager() != null) {
            this.pageScrollIncrement = this.controller.getConfigurationManager().getInt(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE, 10);
        }
    } // --- Fin del constructor ProjectListCoordinator ---

    // =================================================================================
    // === MÉTODOS DE LA INTERFAZ IListCoordinator (A IMPLEMENTAR) ===
    // =================================================================================

    @Override
    public void seleccionarSiguiente() {
        // 1. Obtener la JList activa actualmente desde la UI
        JList<String> listaActiva = obtenerListaActivaUI();
        if (listaActiva == null || listaActiva.getModel().getSize() == 0) {
            return; // No hay nada que seleccionar
        }

        // 2. Calcular el nuevo índice (lógica tomada de tu ProjectController)
        int total = listaActiva.getModel().getSize();
        int currentIndex = listaActiva.getSelectedIndex();
        int nextIndex = (currentIndex == -1) ? 0 : currentIndex + 1; // Si no hay nada seleccionado, vamos al primero

        if (model.isNavegacionCircularActivada() && nextIndex >= total) {
            nextIndex = 0; // Damos la vuelta
        } else {
            nextIndex = Math.min(nextIndex, total - 1); // Nos detenemos en el final
        }

        // 3. ¡Paso clave! Llamamos a nuestro propio método central de selección.
        // Este método se encargará de actualizar el modelo y sincronizar la UI.
        seleccionarImagenEnLista(listaActiva, nextIndex);
        
    } // --- Fin del método seleccionarSiguiente ---

    
    
    @Override
    public void seleccionarAnterior() {
        JList<String> listaActiva = obtenerListaActivaUI();
        if (listaActiva == null || listaActiva.getModel().getSize() == 0) return;

        int total = listaActiva.getModel().getSize();
        int currentIndex = listaActiva.getSelectedIndex();
        int prevIndex;

        if (model.isNavegacionCircularActivada()) {
            prevIndex = (currentIndex <= 0) ? total - 1 : currentIndex - 1;
        } else {
            prevIndex = Math.max(0, currentIndex - 1);
        }
        seleccionarImagenEnLista(listaActiva, prevIndex);
    } // --- Fin del método seleccionarAnterior ---

    @Override
    public void seleccionarPrimero() {
        JList<String> listaActiva = obtenerListaActivaUI();
        if (listaActiva != null && listaActiva.getModel().getSize() > 0) {
            seleccionarImagenEnLista(listaActiva, 0);
        }
    } // --- Fin del método seleccionarPrimero ---

    @Override
    public void seleccionarUltimo() {
        JList<String> listaActiva = obtenerListaActivaUI();
        if (listaActiva != null && listaActiva.getModel().getSize() > 0) {
            seleccionarImagenEnLista(listaActiva, listaActiva.getModel().getSize() - 1);
        }
    } // --- Fin del método seleccionarUltimo ---

    @Override
    public void seleccionarBloqueSiguiente() {
        JList<String> listaActiva = obtenerListaActivaUI();
        if (listaActiva == null || listaActiva.getModel().getSize() == 0) return;
        
        int currentIndex = listaActiva.getSelectedIndex();
        if (currentIndex == -1) currentIndex = 0; // Si no hay nada, empezamos desde el principio
        
        int nextIndex = Math.min(currentIndex + pageScrollIncrement, listaActiva.getModel().getSize() - 1);
        if (nextIndex != currentIndex) {
            seleccionarImagenEnLista(listaActiva, nextIndex);
        }
    } // --- Fin del método seleccionarBloqueSiguiente ---

    @Override
    public void seleccionarBloqueAnterior() {
        JList<String> listaActiva = obtenerListaActivaUI();
        if (listaActiva == null || listaActiva.getModel().getSize() == 0) return;

        int currentIndex = listaActiva.getSelectedIndex();
        if (currentIndex == -1) currentIndex = 0;

        int prevIndex = Math.max(0, currentIndex - pageScrollIncrement);
        if (prevIndex != currentIndex) {
            seleccionarImagenEnLista(listaActiva, prevIndex);
        }
    } // --- Fin del método seleccionarBloqueAnterior ---

    @Override
    public void seleccionarImagenPorIndice(int indiceDeseado) {
        // En el modo proyecto, un índice genérico no tiene mucho sentido sin saber a qué lista se refiere.
        // Podríamos decidir que por defecto actúa sobre la lista activa.
        JList<String> listaActiva = obtenerListaActivaUI();
        if (listaActiva != null) {
            seleccionarImagenEnLista(listaActiva, indiceDeseado);
        }
    } // --- Fin del método seleccionarImagenPorIndice ---

    @Override
    public void reiniciarYSeleccionarIndice(int indiceDeseado) {
        // Para el modo proyecto, un reinicio implica limpiar ambas listas y seleccionar un índice en la activa.
        JList<String> listaSeleccion = registry.get("list.proyecto.nombres");
        if (listaSeleccion != null) listaSeleccion.clearSelection();

        JList<String> listaDescartes = registry.get("list.proyecto.descartes");
        if (listaDescartes != null) listaDescartes.clearSelection();
        
        seleccionarImagenPorIndice(indiceDeseado);
    } // --- Fin del método reiniciarYSeleccionarIndice ---

    @Override
    public void forzarActualizacionEstadoAcciones() {
        if (contextSensitiveActions != null) {
            for (ContextSensitiveAction action : contextSensitiveActions) {
                action.updateEnabledState(model);
            }
        }
    } // --- Fin del método forzarActualizacionEstadoAcciones ---

    @Override
    public void seleccionarSiguienteOAnterior(int wheelRotation) {
        if (wheelRotation < 0) seleccionarAnterior();
        else if (wheelRotation > 0) seleccionarSiguiente();
    } // --- Fin del método seleccionarSiguienteOAnterior ---

    @Override
    public void navegarAIndice(int index) {
        // Alias para seleccionarImagenPorIndice
        seleccionarImagenPorIndice(index);
    } // --- Fin del método navegarAIndice ---
    
    
    @Override
    public void seleccionarAleatorio() {
        // 1. Obtener la JList activa actualmente (Selección o Descartes)
        JList<String> listaActiva = obtenerListaActivaUI();
        if (listaActiva == null || listaActiva.getModel().getSize() == 0) {
            return; // No hay nada que seleccionar
        }
        
        // 2. Calcular un índice aleatorio dentro de esa lista
        int listSize = listaActiva.getModel().getSize();
        int randomIndex = new java.util.Random().nextInt(listSize);
        
        // 3. Seleccionar la imagen en ese índice en la lista activa
        seleccionarImagenEnLista(listaActiva, randomIndex);
    } // --- Fin del método seleccionarAleatorio ---
    
    
    // =================================================================================
    // === MÉTODOS AUXILIARES ===
    // =================================================================================
    
    
    /**
     * Selecciona una imagen basándose en su clave única.
     * Determina en qué lista (Selección o Descartes) se encuentra la clave
     * y la selecciona, actualizando el modelo y la UI.
     * @param clave La clave de la imagen a seleccionar.
     */
    public void seleccionarImagenPorClave(String clave) {
        if (clave == null) {
            // Si la clave es nula, deseleccionamos todo
            seleccionarImagenEnLista(obtenerListaActivaUI(), -1);
            return;
        }

        JList<String> listaSeleccion = registry.get("list.proyecto.nombres");
        JList<String> listaDescartes = registry.get("list.proyecto.descartes");
        
        // Buscar la clave en la lista de SELECCIÓN
        int indiceEnSeleccion = -1;
        if (listaSeleccion != null) {
            DefaultListModel<String> modeloSeleccion = (DefaultListModel<String>) listaSeleccion.getModel();
            indiceEnSeleccion = modeloSeleccion.indexOf(clave);
        }

        if (indiceEnSeleccion != -1) {
            // La encontramos en la lista de Selección. ¡La seleccionamos ahí!
            seleccionarImagenEnLista(listaSeleccion, indiceEnSeleccion);
            return;
        }
        
        // Si no estaba en Selección, la buscamos en DESCARTES
        int indiceEnDescartes = -1;
        if (listaDescartes != null) {
            DefaultListModel<String> modeloDescartes = (DefaultListModel<String>) listaDescartes.getModel();
            indiceEnDescartes = modeloDescartes.indexOf(clave);
        }

        if (indiceEnDescartes != -1) {
            // La encontramos en la lista de Descartes. ¡La seleccionamos ahí!
            seleccionarImagenEnLista(listaDescartes, indiceEnDescartes);
            return;
        }
        
        // Si no la encontramos en ninguna lista visible, no hacemos nada.
        logger.warn("WARN [ProjectListCoordinator]: La clave '" + clave + "' no se encontró en ninguna lista visible del proyecto.");
        
    } // --- Fin del método seleccionarImagenPorClave ---
    
    
    /**
     * Obtiene la JList activa (Selección o Descartes) basándose en el estado del modelo.
     * @return La JList que tiene el foco, o null si no se encuentra.
     */
    private JList<String> obtenerListaActivaUI() {
        String nombreListaActiva = model.getProyectoListContext().getNombreListaActiva();
        if ("descartes".equals(nombreListaActiva)) {
            return registry.get("list.proyecto.descartes");
        }
        // Por defecto, o si es "seleccion", devolvemos la lista de nombres del proyecto.
        return registry.get("list.proyecto.nombres");
    } // --- Fin del método obtenerListaActivaUI ---

    
    /**
     * Método central para actualizar la selección en el modo proyecto.
     * @param lista La JList donde se ha producido la selección (Selección o Descartes).
     * @param indice El nuevo índice a seleccionar en esa JList.
     */
    private synchronized void seleccionarImagenEnLista(JList<String> lista, int indice) {
        if (isSyncingUI || lista == null || indice < 0 || indice >= lista.getModel().getSize()) {
            return;
        }

        String claveSeleccionada = lista.getModel().getElementAt(indice);
        
        // Evitar trabajo innecesario si ya es la imagen seleccionada
        if (Objects.equals(claveSeleccionada, model.getProyectoListContext().getSelectedImageKey())) {
            return;
        }
        
        logger.debug("[ProjectListCoordinator] Nueva selección en Proyecto. Clave: " + claveSeleccionada);
        
        isSyncingUI = true;
        try {
            // 1. Actualizar el Modelo (la fuente de verdad)
            model.getProyectoListContext().setSelectedImageKey(claveSeleccionada);
            // Guardamos también la selección específica de la lista
            if (lista.getName().equals("list.proyecto.nombres")) { // Podríamos usar una mejor forma de identificar la lista
                model.getProyectoListContext().setSeleccionListKey(claveSeleccionada);
            } else {
                model.getProyectoListContext().setDescartesListKey(claveSeleccionada);
            }

            // 2. Cargar la imagen principal
            int indiceEnModeloUnificado = model.getProyectoListContext().getModeloLista().indexOf(claveSeleccionada);
            controller.actualizarImagenPrincipal(indiceEnModeloUnificado);
            
            // 3. Sincronizar la UI (seleccionar el item en la JList)
            lista.setSelectedIndex(indice);
            lista.ensureIndexIsVisible(indice);
            
            // AÑADE ESTA LLAMADA JUSTO AQUÍ, ANTES DE ACTUALIZAR LAS ACCIONES
            sincronizarSeleccionEnGrid(indice);
            
            // 4. Actualizar estado de los botones
            forzarActualizacionEstadoAcciones();
            
        } finally {
            SwingUtilities.invokeLater(() -> isSyncingUI = false);
        }
        
    } // --- Fin del método seleccionarImagenEnLista ---
    

    /**
     * Sincroniza la selección en la UI del grid si este está visible.
     * Es llamado después de que una selección se ha realizado.
     * @param indiceEnListaActiva El índice que debe ser seleccionado en el grid.
     */
    private void sincronizarSeleccionEnGrid(int indiceEnListaActiva) {
        // Solo actuamos si el modo de visualización actual es GRID
        if (model.getCurrentDisplayMode() != VisorModel.DisplayMode.GRID) {
            return;
        }

        JList<String> gridList = registry.get("list.grid");
        if (gridList != null) {
            SwingUtilities.invokeLater(() -> {
                // Comprobamos si el índice es válido para el modelo actual del grid
                if (indiceEnListaActiva >= 0 && indiceEnListaActiva < gridList.getModel().getSize()) {
                    // Si el índice ya está seleccionado, no hacemos nada para evitar bucles.
                    if (gridList.getSelectedIndex() != indiceEnListaActiva) {
                        logger.debug("  [ProjectListCoordinator] Sincronizando selección del GRID al índice: " + indiceEnListaActiva);
                        gridList.setSelectedIndex(indiceEnListaActiva);
                        gridList.ensureIndexIsVisible(indiceEnListaActiva);
                    }
                } else {
                    gridList.clearSelection();
                }
            });
        }
        
    } // --- FIN del metodo sincronizarSeleccionEnGrid ---
    
    
    // =================================================================================
    // === Getters y Setters ===
    // =================================================================================

    @Override
    public boolean isSincronizandoUI() {
        return this.isSyncingUI;
    } // --- Fin del método isSincronizandoUI ---

    @Override
    public void setSincronizandoUI(boolean sincronizando) {
        this.isSyncingUI = sincronizando;
    } // --- Fin del método setSincronizandoUI ---
    
    @Override
    public void setContextSensitiveActions(List<ContextSensitiveAction> actions) {
        this.contextSensitiveActions = actions;
    } // --- Fin del método setContextSensitiveActions ---

} // --- Fin de la clase ProjectListCoordinator ---