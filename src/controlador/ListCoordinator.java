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
    private GridCoordinator gridCoordinator;
    
    private List<ContextSensitiveAction> contextSensitiveActions = Collections.emptyList();
    
    private final List<MasterSelectionChangeListener> selectionListeners = new ArrayList<>();
    
    
    
    // --- Estado Interno ---
    
//    private boolean isSyncingUI = false;
    
    private int pageScrollIncrement;
    private int officialSelectedIndex = -1;

    private boolean thumbnailUpdatesEnabled = true;
    
    public ListCoordinator() {
        // Constructor simple. Las dependencias se inyectan.
    } // --- Fin del método ListCoordinator (constructor) ---

    // =================================================================================
    // === LÓGICA CENTRAL DE SELECCIÓN Y SINCRONIZACIÓN ===
    // =================================================================================

    
    // --- INICIO DE MODIFICACIÓN 2: MÉTODOS PARA GESTIONAR LISTENERS ---
    public void addMasterSelectionChangeListener(MasterSelectionChangeListener listener) {
        if (!selectionListeners.contains(listener)) {
            selectionListeners.add(listener);
        }
    }

    public void removeMasterSelectionChangeListener(MasterSelectionChangeListener listener) {
        selectionListeners.remove(listener);
    }

    private void fireMasterSelectionChanged(int newIndex) {
        for (MasterSelectionChangeListener listener : selectionListeners) {
            listener.onMasterSelectionChanged(newIndex, this);
        }
    }
    // --- FIN DE MODIFICACIÓN 2 ---
    
    
    
    
    
    @Override
    public synchronized void seleccionarImagenPorIndice(int desiredIndex) {
    	 ListContext currentContext = model.getCurrentListContext();
         if (currentContext == null || currentContext.getModeloLista() == null) return;

         DefaultListModel<String> listModel = currentContext.getModeloLista();

         // La comprobación de índice duplicado se hace AHORA en los listeners.
         // Aquí solo validamos los límites.
         if (desiredIndex < -1 || desiredIndex >= listModel.getSize()) {
             return;
         }
         
         // ¡Esta comprobación es redundante si los listeners ya la hacen, pero es una buena
         // segunda línea de defensa para evitar trabajo innecesario!
         if (desiredIndex == this.officialSelectedIndex) {
             return;
         }

         logger.debug("[ListCoordinator] ORDEN RECIBIDA: Sincronizar toda la app al índice maestro: " + desiredIndex);

         // 1. Actualizar el Modelo (la única fuente de verdad)
         this.officialSelectedIndex = desiredIndex;
         String selectedKey = (desiredIndex != -1) ? listModel.getElementAt(desiredIndex) : null;
         currentContext.setSelectedImageKey(selectedKey);
         
         // 2. Cargar la imagen principal
         controller.actualizarImagenPrincipal(desiredIndex);
         
         // 3. Ordenar a TODAS las vistas que se sincronicen
         sincronizarSeleccionJList(getMainJListForCurrentMode(), desiredIndex);
         actualizarTiraDeMiniaturas(desiredIndex);

         // En lugar de llamar directamente al grid, notificamos a todos los que escuchan.
         logger.debug("[ListCoordinator] Notificando a {} listeners del cambio de selección al índice {}.", selectionListeners.size(), desiredIndex);
         fireMasterSelectionChanged(desiredIndex);

         // 4. Actualizar estado de los botones
         forzarActualizacionEstadoAcciones();
     } // --- Fin del método seleccionarImagenPorIndice ---

    /**
     * Orquesta la actualización de la lista de nombres y la tira de miniaturas.
     * @param selectedIndex El índice a seleccionar.
     */
    private void sincronizarVistasConMiniaturas(int selectedIndex) {
        // Actualiza la JList de nombres de archivo (si existe para este modo)
        JList<String> listaNombres = getMainJListForCurrentMode();
        sincronizarSeleccionJList(listaNombres, selectedIndex);

        // Actualiza la JList de la tira de miniaturas
        actualizarTiraDeMiniaturas(selectedIndex);
    } // --- Fin del método sincronizarVistasConMiniaturas ---

    /**
     * Lógica CLAVE para actualizar el modelo de la tira de miniaturas y su selección.
     */
    private void actualizarTiraDeMiniaturas(int selectedIndex) {
    	
    	logger.debug("[ListCoordinator] Iniciando ActualizarTiraDeMiniaturas");
    	
    	// --- INICIO DE LA MODIFICACIÓN ---
        // Si estamos en modo Grid, la barra de miniaturas está oculta y no necesita actualizarse.
        if (model != null && model.getCurrentDisplayMode() == VisorModel.DisplayMode.GRID) {
            return; // No hacer nada.
        }
        // --- FIN DE LA MODIFICACIÓN ---
    	
    	
    	if (!thumbnailUpdatesEnabled || controller.getModeloMiniaturas() == null || model.getCurrentListContext() == null) return;
    	
        if (controller.getModeloMiniaturas() == null || model.getCurrentListContext() == null) return;

        DefaultListModel<String> modeloMiniaturas = controller.getModeloMiniaturas();
        DefaultListModel<String> modeloPrincipal = model.getCurrentListContext().getModeloLista();
        
        if (selectedIndex < 0 || modeloPrincipal.isEmpty()) {
            if (!modeloMiniaturas.isEmpty()) modeloMiniaturas.clear();
            return;
        }

        VisorController.RangoMiniaturasCalculado rango = controller.calcularNumMiniaturasDinamicas();
        int inicio = Math.max(0, selectedIndex - rango.antes);
        int fin = Math.min(modeloPrincipal.getSize() - 1, selectedIndex + rango.despues);

        List<String> clavesParaMiniaturas = new ArrayList<>();
        List<Path> rutasParaCache = new ArrayList<>();
        for (int i = inicio; i <= fin; i++) {
            String clave = modeloPrincipal.getElementAt(i);
            clavesParaMiniaturas.add(clave);
            rutasParaCache.add(model.getCurrentListContext().getRutaCompleta(clave));
        }
        
        controller.precalentarCacheMiniaturasAsync(rutasParaCache);
        
        modeloMiniaturas.clear();
        modeloMiniaturas.addAll(clavesParaMiniaturas);
        
        // --- LÓGICA CORREGIDA ---
        // Determinamos qué JList de miniaturas usar según el modo actual.
        JList<String> listaMiniaturasActiva = (model.getCurrentWorkMode() == WorkMode.CARROUSEL)
                                            ? registry.get("list.miniaturas.carousel")
                                            : registry.get("list.miniaturas");

        int indiceRelativo = selectedIndex - inicio;
        sincronizarSeleccionJList(listaMiniaturasActiva, indiceRelativo);

    } // --- Fin del método actualizarTiraDeMiniaturas ---

    /**
     * Sincroniza de forma segura la selección de una JList.
     */
    private void sincronizarSeleccionJList(JList<String> lista, int index) {
        if (lista == null) return;
        
        // No comprobamos el índice aquí (if lista.getSelectedIndex() == index)
        // porque queremos FORZAR a la vista a obedecer al modelo, incluso si cree que ya está sincronizada.
        
        if (index >= 0 && index < lista.getModel().getSize()) {
            lista.setSelectedIndex(index);
            lista.ensureIndexIsVisible(index);
        } else {
            lista.clearSelection();
        }
    } // --- Fin del método sincronizarSeleccionJList ---

    // =================================================================================
    // === MÉTODOS DE NAVEGACIÓN (Robustos gracias al estado interno) ===
    // =================================================================================
    
    @Override
    public void seleccionarSiguiente() {
        DefaultListModel<String> listModel = model.getCurrentListContext().getModeloLista();
        if (listModel.isEmpty()) return;
        
        int total = listModel.getSize();
        int nextIndex = (this.officialSelectedIndex == -1) ? 0 : this.officialSelectedIndex + 1;
        
        if (model.isNavegacionCircularActivada() && nextIndex >= total) {
            nextIndex = 0;
        } else {
            nextIndex = Math.min(nextIndex, total - 1);
        }
        seleccionarImagenPorIndice(nextIndex);
    } // --- Fin del método seleccionarSiguiente ---

    @Override
    public void seleccionarAnterior() {
        DefaultListModel<String> listModel = model.getCurrentListContext().getModeloLista();
        if (listModel.isEmpty()) return;

        int total = listModel.getSize();
        int prevIndex;
        if (model.isNavegacionCircularActivada()) {
            prevIndex = (this.officialSelectedIndex <= 0) ? total - 1 : this.officialSelectedIndex - 1;
        } else {
            prevIndex = Math.max(0, this.officialSelectedIndex - 1);
        }
        seleccionarImagenPorIndice(prevIndex);
    } // --- Fin del método seleccionarAnterior ---

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
        int next = Math.min(this.officialSelectedIndex + pageScrollIncrement, listModel.getSize() - 1);
        seleccionarImagenPorIndice(next);
    } // --- Fin del método seleccionarBloqueSiguiente ---

    @Override
    public void seleccionarBloqueAnterior() {
        if (model.getCurrentListContext().getModeloLista().isEmpty()) return;
        int prev = Math.max(0, this.officialSelectedIndex - pageScrollIncrement);
        seleccionarImagenPorIndice(prev);
    } // --- Fin del método seleccionarBloqueAnterior ---

    @Override
    public void reiniciarYSeleccionarIndice(int desiredIndex) {
        // MÉTODO REFORZADO: Resetea el estado interno y limpia las vistas
        this.officialSelectedIndex = -1; 
        
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
    public synchronized boolean isSincronizandoUI() { return false;  }
    public synchronized void setSincronizandoUI(boolean sincronizando) {  }
    
    public int getOfficialSelectedIndex() {return this.officialSelectedIndex;}
    
} // --- Fin de la clase ListCoordinator ---

