package controlador;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.interfaces.ContextSensitiveAction;
import controlador.managers.ImageListManager;
import controlador.utils.ComponentRegistry;
import modelo.ListContext;
import modelo.MasterSelectionChangeListener;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;
import servicios.ConfigKeys;

/**
 * Servicio especializado en coordinar la selección y sincronización entre
 * múltiples JLists (ej. lista de nombres y tira de miniaturas) para un
 * contexto de lista dado.
 * Mantiene un estado interno del índice seleccionado para mayor robustez.
 */
public class ListCoordinator extends AbstractListCoordinator {

	private static final Logger logger = LoggerFactory.getLogger(ListCoordinator.class);
	
    // --- Dependencias ---
    private VisorModel model;
    private VisorController controller;
    private ComponentRegistry registry;
    
    private List<ContextSensitiveAction> contextSensitiveActions = Collections.emptyList();
    
    private final List<MasterSelectionChangeListener> selectionListeners = new ArrayList<>();
    
    
    
    // --- Estado Interno ---
    
    private boolean isSyncingUI = false;
    private volatile Integer pendingSelectionIndex = null;
    
    private int pageScrollIncrement;
    private int officialSelectedIndex = -1;

    private boolean thumbnailUpdatesEnabled = true;

    // Sincronización de multi-selección entre paneles
    private boolean syncingMultiSelection = false;
    private int thumbnailWindowStart = 0;
    
    public ListCoordinator() {
        // Constructor simple. Las dependencias se inyectan.
    } // --- Fin del método ListCoordinator (constructor) ---

    // =================================================================================
    // === LÓGICA CENTRAL DE SELECCIÓN Y SINCRONIZACIÓN ===
    // =================================================================================

    
    /**
     * Añade un listener que será notificado cuando la selección maestra cambie.
     * @param listener El listener a añadir
     */
    public void addMasterSelectionChangeListener(MasterSelectionChangeListener listener) {
        if (!selectionListeners.contains(listener)) {
            selectionListeners.add(listener);
        }
    } // --- Fin del metodo/clase addMasterSelectionChangeListener ---


    /**
     * Elimina un listener de la lista de notificaciones.
     * @param listener El listener a eliminar
     */
    public void removeMasterSelectionChangeListener(MasterSelectionChangeListener listener) {
        selectionListeners.remove(listener);
    } // --- Fin del metodo/clase removeMasterSelectionChangeListener ---


    /**
     * Notifica a todos los listeners registrados que la selección maestra ha cambiado.
     * @param newIndex El nuevo índice seleccionado
     */
    private void fireMasterSelectionChanged(int newIndex) {
        for (MasterSelectionChangeListener listener : selectionListeners) {
            listener.onMasterSelectionChanged(newIndex, this);
        }
    } // --- Fin del metodo/clase fireMasterSelectionChanged ---


    /**
     * Selecciona una imagen por su índice, sincronizando todas las vistas.
     * @param desiredIndex El índice a seleccionar
     */
    @Override
    public synchronized void seleccionarImagenPorIndice(int desiredIndex) {
        // --- INICIO LÓGICA DEBOUNCE ---
        if (isSincronizandoUI()) {
            logger.trace("Sincronización en curso. Petición para índice {} encolada.", desiredIndex);
            this.pendingSelectionIndex = desiredIndex; // Guarda la última petición
            return;
        }
        // --- FIN LÓGICA DEBOUNCE ---

        ListContext currentContext = model.getCurrentListContext();
        if (currentContext == null || currentContext.getModeloLista() == null) return;

        DefaultListModel<String> listModel = currentContext.getModeloLista();
        if (desiredIndex < -1 || desiredIndex >= listModel.getSize()) {
            return;
        }
        
        if (desiredIndex == this.officialSelectedIndex) {
            return;
        }

        try {
            setSincronizandoUI(true); // --- CERROJO ACTIVADO ---

            logger.debug("[ListCoordinator] ORDEN RECIBIDA: Sincronizar toda la app al índice maestro: " + desiredIndex);

            this.officialSelectedIndex = desiredIndex;
            String selectedKey = (desiredIndex != -1) ? listModel.getElementAt(desiredIndex) : null;
            currentContext.setSelectedImageKey(selectedKey);
            
            controller.actualizarImagenPrincipal(desiredIndex);
            
            sincronizarSeleccionJList(getMainJListForCurrentMode(), desiredIndex);
            actualizarTiraDeMiniaturas(desiredIndex);

            logger.debug("[ListCoordinator] Notificando a {} listeners del cambio de selección al índice {}.", selectionListeners.size(), desiredIndex);
            fireMasterSelectionChanged(desiredIndex);

            forzarActualizacionEstadoAcciones();

        } finally {
            setSincronizandoUI(false); // --- CERROJO LIBERADO ---
            
            // --- INICIO LÓGICA DEBOUNCE (PROCESAR PENDIENTES) ---
            Integer pendingIndex = this.pendingSelectionIndex;
            if (pendingIndex != null) {
                this.pendingSelectionIndex = null; // Limpiar antes de la llamada recursiva
                logger.debug("Procesando petición pendiente para el índice: {}", pendingIndex);
                // Llamada recursiva para procesar la última petición guardada
                seleccionarImagenPorIndice(pendingIndex);
            }
            // --- FIN LÓGICA DEBOUNCE ---
        }
    } // --- Fin del metodo/clase seleccionarImagenPorIndice ---


    /**
     * Lógica CLAVE para actualizar el modelo de la tira de miniaturas y su selección.
     * @param selectedIndex El índice seleccionado
     */
    private void actualizarTiraDeMiniaturas(int selectedIndex) {
        
        logger.debug("[ListCoordinator] Iniciando ActualizarTiraDeMiniaturas");
        
        // Si estamos en modo Grid o Polaroid, la barra de miniaturas está oculta y no necesita actualizarse.
        if (model != null && (model.getCurrentDisplayMode() == VisorModel.DisplayMode.GRID
                           || model.getCurrentDisplayMode() == VisorModel.DisplayMode.POLAROID)) {
            return; // No hacer nada.
        }
        
        
        if (!thumbnailUpdatesEnabled || controller.getModeloMiniaturas() == null || model.getCurrentListContext() == null) return;
        
        if (controller.getModeloMiniaturas() == null || model.getCurrentListContext() == null) return;

        DefaultListModel<String> modeloMiniaturas = controller.getModeloMiniaturas();
        DefaultListModel<String> modeloPrincipal = model.getCurrentListContext().getModeloLista();
        
        if (selectedIndex < 0 || modeloPrincipal.isEmpty()) {
            if (!modeloMiniaturas.isEmpty()) modeloMiniaturas.clear();
            return;
        }

        ImageListManager.RangoMiniaturasCalculado rango = controller.calcularNumMiniaturasDinamicas();
        int inicio = Math.max(0, selectedIndex - rango.antes);
        int fin = Math.min(modeloPrincipal.getSize() - 1, selectedIndex + rango.despues);
        this.thumbnailWindowStart = inicio;

        List<String> clavesParaMiniaturas = new ArrayList<>();
        List<Path> rutasParaCache = new ArrayList<>();
        for (int i = inicio; i <= fin; i++) {
            String clave = modeloPrincipal.getElementAt(i);
            clavesParaMiniaturas.add(clave);
            rutasParaCache.add(model.getCurrentListContext().getRutaCompleta(clave));
        }
        
        controller.getImageListManager().precalentarCacheMiniaturasAsync(rutasParaCache);
        
        modeloMiniaturas.clear();
        modeloMiniaturas.addAll(clavesParaMiniaturas);
        
        // --- LÓGICA CORREGIDA ---
        // Determinamos qué JList de miniaturas usar según el modo actual.
        JList<String> listaMiniaturasActiva = (model.getCurrentWorkMode() == WorkMode.CARROUSEL)
                                            ? registry.get("list.miniaturas.carousel")
                                            : registry.get("list.miniaturas");

        int indiceRelativo = selectedIndex - inicio;
        sincronizarSeleccionJList(listaMiniaturasActiva, indiceRelativo);

    } // --- Fin del metodo/clase actualizarTiraDeMiniaturas ---


    /**
     * Sincroniza de forma segura la selección de una JList.
     * @param lista La lista a sincronizar
     * @param index El índice a seleccionar
     */
    private void sincronizarSeleccionJList(JList<String> lista, int index) {
        if (lista == null) return;
        
        // No comprobamos el índice aquí (if lista.getSelectedIndex() == index)
        // porque queremos FORZAR a la vista a obedecer al modelo, incluso si cree que ya está sincronizada.
        
        if (index >= 0 && index < lista.getModel().getSize()) {
            lista.setSelectedIndices(new int[]{index});
            lista.ensureIndexIsVisible(index);
        } else {
            lista.clearSelection();
        }
    } // --- Fin del metodo/clase sincronizarSeleccionJList ---


    /**
     * Propaga la multi-selección desde una lista origen a todas las demás listas
     * del modo VISUALIZADOR, traduciendo índices entre modelos (completo vs. ventana).
     * @param source La lista origen
     * @param indices Los índices seleccionados en la lista origen
     */
    private void propagarMultiSeleccion(JList<String> source, int[] indices) {
        if (syncingMultiSelection || indices == null || indices.length == 0) return;
        syncingMultiSelection = true;
        try {
            // Convertir índices relativos de thumbnails a absolutos si es necesario
            int[] absIndices;
            boolean isThumbSource = (source == registry.get("list.miniaturas")
                                  || source == registry.get("list.miniaturas.carousel"));
            if (isThumbSource) {
                absIndices = new int[indices.length];
                for (int i = 0; i < indices.length; i++) {
                    absIndices[i] = indices[i] + thumbnailWindowStart;
                }
            } else {
                absIndices = indices;
            }

            // Aplicar a lista de nombres (modelo completo)
            JList<String> fileList = registry.get("list.nombresArchivo");
            if (fileList != null && fileList != source) {
                fileList.setSelectedIndices(absIndices);
            }

            // Aplicar a grid del visor (modelo completo)
            JList<String> gridList = registry.get("list.grid");
            if (gridList != null && gridList != source) {
                gridList.setSelectedIndices(absIndices);
            }

            // Aplicar a miniaturas (modelo de ventana, convertir absolutos a relativos)
            JList<String> thumbList = registry.get("list.miniaturas");
            if (thumbList != null && thumbList != source) {
                java.util.List<Integer> relIndices = new java.util.ArrayList<>();
                for (int absIdx : absIndices) {
                    int relIdx = absIdx - thumbnailWindowStart;
                    if (relIdx >= 0 && relIdx < thumbList.getModel().getSize()) {
                        relIndices.add(relIdx);
                    }
                }
                if (!relIndices.isEmpty()) {
                    int[] thumbArr = relIndices.stream().mapToInt(Integer::intValue).toArray();
                    thumbList.setSelectedIndices(thumbArr);
                }
            }
        } finally {
            syncingMultiSelection = false;
        }
    } // --- Fin del metodo/clase propagarMultiSeleccion ---


    /**
     * Configura los listeners de selección en todas las listas del modo VISUALIZADOR
     * para sincronizar la multi-selección entre paneles.
     * Debe llamarse después de que todas las listas estén creadas e inicializadas.
     */
    public void configurarSyncMultiSeleccion() {
        javax.swing.event.ListSelectionListener listener = (javax.swing.event.ListSelectionEvent e) -> {
            if (syncingMultiSelection || isSincronizandoUI()) return;
            if (e.getValueIsAdjusting()) return;
            Object src = e.getSource();
            if (!(src instanceof javax.swing.JList)) return;
            @SuppressWarnings("unchecked")
            javax.swing.JList<String> list = (javax.swing.JList<String>) src;
            propagarMultiSeleccion(list, list.getSelectedIndices());
        };

        JList<String>[] lists = new JList[]{
            registry.get("list.nombresArchivo"),
            registry.get("list.miniaturas"),
            registry.get("list.grid")
        };
        
        
        for (JList<String> list : lists) {
            if (list != null) {
                list.addListSelectionListener(listener);
            }
        }
    } // --- Fin del metodo/clase configurarSyncMultiSeleccion ---


    /**
     * Selecciona la siguiente imagen en la lista.
     */
    @Override
    public void seleccionarSiguiente() {
        DefaultListModel<String> listModel = model.getCurrentListContext().getModeloLista();
        if (listModel.isEmpty()) return;
        
        int total = listModel.getSize();
        // Usa el índice pendiente si existe, si no, el oficial
        int baseIndex = (pendingSelectionIndex != null) ? pendingSelectionIndex : this.officialSelectedIndex;
        int nextIndex = (baseIndex == -1) ? 0 : baseIndex + 1;
        
        if (model.isNavegacionCircularActivada() && nextIndex >= total) {
            nextIndex = 0;
        } else {
            nextIndex = Math.min(nextIndex, total - 1);
        }
        seleccionarImagenPorIndice(nextIndex);
    } // --- Fin del metodo/clase seleccionarSiguiente ---


    /**
     * Selecciona la imagen anterior en la lista.
     */
    @Override
    public void seleccionarAnterior() {
        DefaultListModel<String> listModel = model.getCurrentListContext().getModeloLista();
        if (listModel.isEmpty()) return;

        int total = listModel.getSize();
        int baseIndex = (pendingSelectionIndex != null) ? pendingSelectionIndex : this.officialSelectedIndex;
        int prevIndex;
        
        if (model.isNavegacionCircularActivada()) {
            prevIndex = (baseIndex <= 0) ? total - 1 : baseIndex - 1;
        } else {
            prevIndex = Math.max(0, baseIndex - 1);
        }
        seleccionarImagenPorIndice(prevIndex);
    } // --- Fin del metodo/clase seleccionarAnterior ---

    @Override
    public void seleccionarPrimero() {

        if (!model.getCurrentListContext().getModeloLista().isEmpty()) {
            seleccionarImagenPorIndice(0);
        }
    } // --- Fin del método seleccionarPrimero ---

    @Override
    public void seleccionarUltimo() {

        DefaultListModel<String> listModel = model.getCurrentListContext().getModeloLista();
        if (!listModel.isEmpty()) {
            seleccionarImagenPorIndice(listModel.getSize() - 1);
        }
    } // --- Fin del método seleccionarUltimo ---

    @Override
    public void seleccionarBloqueSiguiente() {
        DefaultListModel<String> listModel = model.getCurrentListContext().getModeloLista();
        if (listModel.isEmpty()) return;
        int baseIndex = (pendingSelectionIndex != null) ? pendingSelectionIndex : this.officialSelectedIndex;
        int next = Math.min(baseIndex + pageScrollIncrement, listModel.getSize() - 1);
        seleccionarImagenPorIndice(next);
    } // --- Fin del método seleccionarBloqueSiguiente ---


    @Override
    public void seleccionarBloqueAnterior() {
        DefaultListModel<String> listModel = model.getCurrentListContext().getModeloLista();
        if (listModel.isEmpty()) return;
        
        // Usa el índice pendiente si existe, si no, el oficial
        int baseIndex = (pendingSelectionIndex != null) ? pendingSelectionIndex : this.officialSelectedIndex;
        
        int prev = Math.max(0, baseIndex - pageScrollIncrement);
        seleccionarImagenPorIndice(prev);
    } // --- Fin del método seleccionarBloqueAnterior ---
    
    @Override
    public void reiniciarYSeleccionarIndice(int desiredIndex) {

        // MÉTODO REFORZADO: Resetea el estado interno y limpia las vistas
        // Usamos -2 para asegurar que el posterior seleccionarImagenPorIndice(-1)
        // sea detectado como un cambio y se ejecute la limpieza de la UI.
        this.officialSelectedIndex = -2; 
        
        JList<String> listaNombres = registry.get("list.nombresArchivo");
        if (listaNombres != null) listaNombres.clearSelection();
        
        JList<String> listaMiniaturas = registry.get("list.miniaturas");
        if (listaMiniaturas != null) listaMiniaturas.clearSelection();
        
        // Ahora sí, selecciona el nuevo índice desde un estado limpio.
        seleccionarImagenPorIndice(desiredIndex);
    } // --- Fin del método reiniciarYSeleccionarIndice ---

    @Override
    public void forzarActualizacionEstadoAcciones() {
        if (contextSensitiveActions != null) {
            for (ContextSensitiveAction action : contextSensitiveActions) {
                action.updateEnabledState(model);
            }
        }
    } // --- Fin del método forzarActualizacionEstadoAcciones ---

    
    /**
     * Selecciona un índice completamente aleatorio de la lista de imágenes
     * del contexto de trabajo actual.
     */
    public void seleccionarAleatorio() {

        DefaultListModel<String> modeloLista = model.getCurrentListContext().getModeloLista();
        if (modeloLista == null || modeloLista.isEmpty()) {
            return; // No hay nada que seleccionar
        }
        
        int listSize = modeloLista.getSize();
        // Genera un número aleatorio entre 0 (incluido) y listSize (excluido)
        int randomIndex = new java.util.Random().nextInt(listSize);
        
        // Llama al método existente para seleccionar el índice
        seleccionarImagenPorIndice(randomIndex); // true para asegurar que la UI se actualice
    } // --- Fin del método seleccionarAleatorio ---
    
    // =================================================================================
    // === MÉTODOS VARIOS (DELEGACIÓN) ===
    // =================================================================================

    @Override
    public void seleccionarSiguienteOAnterior(int wheelRotation) {
        if (wheelRotation < 0) seleccionarAnterior();
        else if (wheelRotation > 0) seleccionarSiguiente();
    } // --- Fin del método seleccionarSiguienteOAnterior ---

    @Override
    public void navegarAIndice(int index) {

    	
        ListContext ctx = model.getCurrentListContext();
        if (ctx != null && ctx.getModeloLista() != null && index >= 0 && index < ctx.getModeloLista().getSize()) {
            seleccionarImagenPorIndice(index);
        }
    } // --- Fin del método navegarAIndice ---

    private JList<String> getMainJListForCurrentMode() {
        if (model.getCurrentWorkMode() == WorkMode.VISUALIZADOR) {
            return registry.get("list.nombresArchivo");
        }
        // Para el carrusel, no hay lista de nombres principal, así que devolvemos null.
        return null; 
    } // --- Fin del método getMainJListForCurrentMode ---

    public void setThumbnailUpdatesEnabled(boolean enabled) {
        this.thumbnailUpdatesEnabled = enabled;
        logger.debug("[ListCoordinator] Actualizaciones de la tira de miniaturas " + (enabled ? "HABILITADAS" : "DESHABILITADAS"));
    } // --- Fin del método setThumbnailUpdatesEnabled ---

    public void forzarActualizacionDeTiraDeMiniaturas() {
        if(this.thumbnailUpdatesEnabled) {
            actualizarTiraDeMiniaturas(this.officialSelectedIndex);
        }
    } // --- Fin del método forzarActualizacionDeTiraDeMiniaturas ---
    
    
    // =================================================================================
    // === SETTERS Y GETTERS ===
    // =================================================================================
    
    public void setModel(VisorModel model) { this.model = model; }
    public void setController(VisorController controller) {
        this.controller = controller;
        if (this.controller != null && this.controller.getConfigurationManager() != null) {
            this.pageScrollIncrement = this.controller.getConfigurationManager().getInt(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE, 10);
        }
    } // --- Fin del método setController ---
    public void setRegistry(ComponentRegistry registry) { this.registry = registry; }
    public void setContextSensitiveActions(List<ContextSensitiveAction> actions) { this.contextSensitiveActions = actions; }
    
    public synchronized boolean isSincronizandoUI() { return this.isSyncingUI;  }
    public synchronized void setSincronizandoUI(boolean sincronizando) { this.isSyncingUI = sincronizando; }
    
    public int getOfficialSelectedIndex() {return this.officialSelectedIndex;}
    
} // --- Fin de la clase ListCoordinator ---

