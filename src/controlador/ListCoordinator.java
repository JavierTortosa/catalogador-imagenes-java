package controlador;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.SwingUtilities;

import controlador.interfaces.ContextSensitiveAction;
import controlador.managers.interfaces.IListCoordinator;
import controlador.utils.ComponentRegistry;
import modelo.ListContext;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;
import servicios.ConfigKeys;

/**
 * Servicio especializado en coordinar la selección y sincronización entre
 * múltiples JLists (ej. lista de nombres y tira de miniaturas) para un
 * contexto de lista dado.
 * Mantiene un estado interno del índice seleccionado para mayor robustez.
 */
public class ListCoordinator implements IListCoordinator {

    // --- Dependencias ---
    private VisorModel model;
    private VisorController controller;
    private ComponentRegistry registry;
    private List<ContextSensitiveAction> contextSensitiveActions = Collections.emptyList();
    
    // --- Estado Interno ---
    private boolean isSyncingUI = false;
    private int pageScrollIncrement;
    // ¡LA MEMORIA VUELVE! Este es el índice oficial para este coordinador.
    private int officialSelectedIndex = -1;

    public ListCoordinator() {
        // Constructor simple. Las dependencias se inyectan.
    } // --- Fin del método ListCoordinator (constructor) ---

    // =================================================================================
    // === LÓGICA CENTRAL DE SELECCIÓN Y SINCRONIZACIÓN ===
    // =================================================================================

    @Override
    public synchronized void seleccionarImagenPorIndice(int desiredIndex) {
        if (isSyncingUI) {
            return; // Prevenir bucles de eventos
        }
        
        // Obtiene el contexto del modo de trabajo ACTUAL.
        // Esto hace que el coordinador sea reutilizable para Visualizador y Carrusel.
        ListContext currentContext = model.getCurrentListContext();
        if (currentContext == null || currentContext.getModeloLista() == null) {
            return;
        }

        DefaultListModel<String> listModel = currentContext.getModeloLista();

        // Validaciones: No hacer nada si el índice es inválido o es el mismo que ya está seleccionado.
        if (desiredIndex < -1 || desiredIndex >= listModel.getSize() || desiredIndex == this.officialSelectedIndex) {
            return;
        }

        System.out.println("[ListCoordinator] Nueva selección. Modo: " + model.getCurrentWorkMode() + ". Índice: " + desiredIndex);

        // 1. Actualizar el estado interno y el del Modelo (la fuente de verdad)
        this.officialSelectedIndex = desiredIndex;
        String selectedKey = (desiredIndex != -1) ? listModel.getElementAt(desiredIndex) : null;
        currentContext.setSelectedImageKey(selectedKey);
        
        // 2. Cargar la imagen principal en la vista
        // El índice que pasamos es el del modelo principal del contexto, que es el que espera el método.
        controller.actualizarImagenPrincipal(desiredIndex);
        
        // 3. Sincronizar las Vistas (JLists)
        // Solo actualizamos las miniaturas si estamos en modo Visualizador o Carrusel (modos con tira de miniaturas).
        if (model.getCurrentWorkMode() == WorkMode.VISUALIZADOR || model.getCurrentWorkMode() == WorkMode.CARROUSEL) {
            sincronizarVistasConMiniaturas(desiredIndex);
        } else {
            // Para otros modos futuros que solo tengan una lista de nombres.
            JList<String> mainList = getMainJListForCurrentMode();
            sincronizarSeleccionJList(mainList, desiredIndex);
        }

        // 4. Actualizar el estado de los botones (enabled/disabled)
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
        if (lista == null || lista.getSelectedIndex() == index) return;
        
        isSyncingUI = true;
        try {
            if (index >= 0 && index < lista.getModel().getSize()) {
                lista.setSelectedIndex(index);
                lista.ensureIndexIsVisible(index);
            } else {
                lista.clearSelection();
            }
        } finally {
            SwingUtilities.invokeLater(() -> isSyncingUI = false);
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
    public synchronized boolean isSincronizandoUI() { return isSyncingUI; }
    public synchronized void setSincronizandoUI(boolean sincronizando) { this.isSyncingUI = sincronizando; }

    public int getOfficialSelectedIndex() {return this.officialSelectedIndex;}
    
} // --- Fin de la clase ListCoordinator ---

