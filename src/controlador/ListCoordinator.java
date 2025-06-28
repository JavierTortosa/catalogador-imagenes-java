package controlador; 

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.SwingUtilities;

import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import controlador.managers.interfaces.IListCoordinator;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigKeys;
import vista.VisorView;

public class ListCoordinator implements IListCoordinator {

    // --- Dependencias ---
    private VisorModel model;
    private VisorView view;
    private VisorController controller;
    private ComponentRegistry registry;
    private List<ContextSensitiveAction> contextSensitiveActions = Collections.emptyList();
    
    // --- Estado Interno ---
    private int indiceOficialSeleccionado = -1;
    private boolean sincronizandoUI = false;
    private int pageScrollIncrement;
    
    public ListCoordinator() {
        this.pageScrollIncrement = 10;
    } // --- Fin del método ListCoordinator (constructor) ---

    // --- PUNTO DE ENTRADA ÚNICO Y CENTRALIZADO PARA LA SELECCIÓN ---
    @Override
    public synchronized void seleccionarImagenPorIndice(int indiceDeseado) {
        if (sincronizandoUI) return;
        
        DefaultListModel<String> currentModel = model.getModeloLista();
        if (indiceDeseado < -1 || indiceDeseado >= currentModel.getSize() || indiceDeseado == this.indiceOficialSeleccionado) {
            return;
        }

        this.indiceOficialSeleccionado = indiceDeseado;
        String claveSeleccionada = (indiceDeseado != -1) ? currentModel.getElementAt(indiceDeseado) : null;
        
        model.setSelectedImageKey(claveSeleccionada);
        
        controller.actualizarImagenPrincipal(indiceDeseado);
        
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR) {
            actualizarModeloYVistaMiniaturas(indiceDeseado);
        }
        
        JList<String> listaNombres = (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) 
                                     ? registry.get("list.proyecto.nombres") 
                                     : registry.get("list.nombresArchivo");
        sincronizarListaUI(listaNombres, indiceDeseado);
        
        forzarActualizacionEstadoAcciones();
    } // --- Fin del método seleccionarImagenPorIndice ---
    
    // --- MÉTODOS DE NAVEGACIÓN (DELEGAN AL MÉTODO CENTRAL) ---
    
    @Override
    public synchronized void seleccionarSiguiente() {
        if (model.getModeloLista().isEmpty()) return;
        int total = model.getModeloLista().getSize();
        int nuevoIndice = (this.indiceOficialSeleccionado == -1) ? 0 : this.indiceOficialSeleccionado + 1;
        if (model.isNavegacionCircularActivada() && nuevoIndice >= total) {
            nuevoIndice = 0;
        } else {
            nuevoIndice = Math.min(nuevoIndice, total - 1);
        }
        seleccionarImagenPorIndice(nuevoIndice);
    } // --- Fin del método seleccionarSiguiente ---
    
    @Override
    public synchronized void seleccionarAnterior() {
        if (model.getModeloLista().isEmpty()) return;
        int total = model.getModeloLista().getSize();
        int nuevoIndice;
        if (model.isNavegacionCircularActivada()) {
            nuevoIndice = (this.indiceOficialSeleccionado <= 0) ? total - 1 : this.indiceOficialSeleccionado - 1;
        } else {
            nuevoIndice = Math.max(0, this.indiceOficialSeleccionado - 1);
        }
        seleccionarImagenPorIndice(nuevoIndice);
    } // --- Fin del método seleccionarAnterior ---

    @Override
    public synchronized void seleccionarPrimero() {
        if (!model.getModeloLista().isEmpty()) seleccionarImagenPorIndice(0);
    } // --- Fin del método seleccionarPrimero ---

    @Override
    public synchronized void seleccionarUltimo() {
        if (!model.getModeloLista().isEmpty()) seleccionarImagenPorIndice(model.getModeloLista().getSize() - 1);
    } // --- Fin del método seleccionarUltimo ---

    @Override
    public synchronized void seleccionarBloqueSiguiente() {
        if (model.getModeloLista().isEmpty()) return;
        int total = model.getModeloLista().getSize();
        int nuevoIndice = Math.min(this.indiceOficialSeleccionado + pageScrollIncrement, total - 1);
        seleccionarImagenPorIndice(nuevoIndice);
    } // --- Fin del método seleccionarBloqueSiguiente ---

    @Override
    public synchronized void seleccionarBloqueAnterior() {
        if (model.getModeloLista().isEmpty()) return;
        int nuevoIndice = Math.max(0, this.indiceOficialSeleccionado - pageScrollIncrement);
        seleccionarImagenPorIndice(nuevoIndice);
    } // --- Fin del método seleccionarBloqueAnterior ---

    @Override
    public synchronized void reiniciarYSeleccionarIndice(int indiceDeseado) {
        this.indiceOficialSeleccionado = -1;
        JList<String> listaNombres = registry.get("list.nombresArchivo");
        if (listaNombres != null) listaNombres.clearSelection();
        JList<String> listaProyecto = registry.get("list.proyecto.nombres");
        if (listaProyecto != null) listaProyecto.clearSelection();
        
        seleccionarImagenPorIndice(indiceDeseado);
    } // --- Fin del método reiniciarYSeleccionarIndice ---

    // --- MÉTODOS DE SINCRONIZACIÓN DE UI ---
    
    private void sincronizarListaUI(JList<String> lista, int indice) {
        if (lista == null || lista.getSelectedIndex() == indice) return;
        setSincronizandoUI(true);
        try {
            if (indice == -1) {
                lista.clearSelection();
            } else if (indice < lista.getModel().getSize()) {
                lista.setSelectedIndex(indice);
                lista.ensureIndexIsVisible(indice);
            }
        } finally {
            SwingUtilities.invokeLater(() -> setSincronizandoUI(false));
        }
    } // --- Fin del método sincronizarListaUI ---
    
    
    private void actualizarModeloYVistaMiniaturas(int indiceSeleccionadoPrincipal) {
        // --- 1. Validaciones ---
        if (model == null || controller == null || registry == null || controller.getModeloMiniaturas() == null) {
            System.err.println("WARN [actualizarModeloYVistaMiniaturas]: Dependencias nulas. Abortando.");
            return;
        }

        // LOG *** AÑADIR ESTA LÍNEA DE DEBUG ***
        System.out.println("!!! DEBUG ListCoordinator: Voy a modificar el modelo con hashCode: " + System.identityHashCode(controller.getModeloMiniaturas()));
        
        DefaultListModel<String> modeloMiniaturasReal = controller.getModeloMiniaturas();

        if (indiceSeleccionadoPrincipal < 0) {
            if (!modeloMiniaturasReal.isEmpty()) {
                modeloMiniaturasReal.clear();
            }
            return;
        }
        
        DefaultListModel<String> modeloPrincipal = model.getModeloLista();
        if (indiceSeleccionadoPrincipal >= modeloPrincipal.getSize()) {
            if (!modeloMiniaturasReal.isEmpty()) {
                modeloMiniaturasReal.clear();
            }
            return;
        }
        
        // --- 2. Calcular rango y preparar datos (sin cambios) ---
        VisorController.RangoMiniaturasCalculado rango = controller.calcularNumMiniaturasDinamicas();
        int inicio = Math.max(0, indiceSeleccionadoPrincipal - rango.antes);
        int fin = Math.min(modeloPrincipal.getSize() - 1, indiceSeleccionadoPrincipal + rango.despues);

        List<String> clavesParaMiniaturas = new ArrayList<>();
        List<Path> rutasParaCache = new ArrayList<>();
        for (int i = inicio; i <= fin; i++) {
            String clave = modeloPrincipal.getElementAt(i);
            clavesParaMiniaturas.add(clave);
            rutasParaCache.add(model.getRutaCompleta(clave));
        }
        
        // --- 3. Precalentar caché (sin cambios) ---
        controller.precalentarCacheMiniaturasAsync(rutasParaCache);
        
        // --- 4. ACTUALIZACIÓN ATÓMICA Y EFICIENTE DEL MODELO ---
        // Esta es la corrección principal. En lugar de un bucle, usamos los métodos
        // de DefaultListModel para reemplazar el contenido de forma eficiente.
        // Esto dispara un único evento de cambio en la lista, evitando parpadeos.
        
        modeloMiniaturasReal.clear(); // Limpiamos el modelo existente.
        modeloMiniaturasReal.addAll(clavesParaMiniaturas); // Añadimos todos los nuevos elementos de una vez.
                                                           // Nota: addAll() existe desde Java 11.
                                                           // Si usas una versión anterior, el bucle era necesario,
                                                           // pero la causa del error sigue siendo probable en otro punto.
                                                           // Con este enfoque, eliminamos esa posibilidad.
        
        // --- 5. Sincronizar UI (sin cambios en la lógica) ---
        JList<String> listaMiniaturas = registry.get("list.miniaturas");
        int indiceRelativo = indiceSeleccionadoPrincipal - inicio;
        sincronizarListaUI(listaMiniaturas, indiceRelativo);
        
        if (listaMiniaturas != null) {
            listaMiniaturas.revalidate();
            listaMiniaturas.repaint();
        }

    } // --- Fin del método actualizarModeloYVistaMiniaturas (CORREGIDO) ---
    

//    private void actualizarModeloYVistaMiniaturas(int indiceSeleccionadoPrincipal) {
//        if (model == null || controller == null || registry.get("list.miniaturas") == null) return;
//        if (indiceSeleccionadoPrincipal < 0) {
//            if (controller.getModeloMiniaturas() != null) controller.getModeloMiniaturas().clear();
//            return;
//        }
//
//        DefaultListModel<String> modeloPrincipal = model.getModeloLista();
//        if (indiceSeleccionadoPrincipal >= modeloPrincipal.getSize()) return;
//
//        VisorController.RangoMiniaturasCalculado rango = controller.calcularNumMiniaturasDinamicas();
//        int inicio = Math.max(0, indiceSeleccionadoPrincipal - rango.antes);
//        int fin = Math.min(modeloPrincipal.getSize() - 1, indiceSeleccionadoPrincipal + rango.despues);
//        
//        DefaultListModel<String> nuevoModeloMiniaturas = new DefaultListModel<>();
//        List<Path> rutasEnRango = new ArrayList<>();
//        for (int i = inicio; i <= fin; i++) {
//            String clave = modeloPrincipal.getElementAt(i);
//            nuevoModeloMiniaturas.addElement(clave);
//            rutasEnRango.add(model.getRutaCompleta(clave));
//        }
//        
//        controller.precalentarCacheMiniaturasAsync(rutasEnRango);
//        
//        
//        JList<String> listaMiniaturas = registry.get("list.miniaturas");
//        int indiceRelativo = indiceSeleccionadoPrincipal - inicio;
//        
//        // --- INICIO DE LA CORRECCIÓN ---
//        DefaultListModel<String> modeloDestino = controller.getModeloMiniaturas();
//        if (modeloDestino != null) {
//            modeloDestino.clear();
//            // Iteramos sobre el modelo nuevo y añadimos cada elemento al modelo destino.
//            for (int i = 0; i < nuevoModeloMiniaturas.size(); i++) {
//                modeloDestino.addElement(nuevoModeloMiniaturas.getElementAt(i));
//            }
//        }
//        
//        sincronizarListaUI(listaMiniaturas, indiceRelativo);
//    } // --- Fin del método actualizarModeloYVistaMiniaturas ---
    
    @Override
    public synchronized void forzarActualizacionEstadoAcciones() {
        if (model == null || controller == null || controller.getActionMap() == null) return;

        boolean hayImagenes = !model.getModeloLista().isEmpty();
        int ultimoIndice = hayImagenes ? model.getModeloLista().getSize() - 1 : -1;
        boolean navCircular = model.isNavegacionCircularActivada();
        
        Map<String, Action> actionMap = controller.getActionMap();
        actionMap.get(AppActionCommands.CMD_NAV_ANTERIOR).setEnabled(hayImagenes && (navCircular || indiceOficialSeleccionado > 0));
        actionMap.get(AppActionCommands.CMD_NAV_SIGUIENTE).setEnabled(hayImagenes && (navCircular || indiceOficialSeleccionado < ultimoIndice));
        actionMap.get(AppActionCommands.CMD_NAV_PRIMERA).setEnabled(hayImagenes && indiceOficialSeleccionado > 0);
        actionMap.get(AppActionCommands.CMD_NAV_ULTIMA).setEnabled(hayImagenes && indiceOficialSeleccionado < ultimoIndice);
        
        if (contextSensitiveActions != null) {
            for (ContextSensitiveAction action : contextSensitiveActions) {
                action.updateEnabledState(model);
            }
        }
    } // --- Fin del método forzarActualizacionEstadoAcciones ---

    
    /**
     * Unifica la navegación de la rueda del ratón. Llama a seleccionarSiguiente()
     * o seleccionarAnterior() basándose en la dirección de la rotación de la rueda.
     *
     * @param wheelRotation El valor de getWheelRotation() del MouseWheelEvent.
     */
    public void seleccionarSiguienteOAnterior(int wheelRotation) {
        if (wheelRotation < 0) {
            seleccionarAnterior();
        } else if (wheelRotation > 0) {
            seleccionarSiguiente();
        }
    } // --- Fin del método seleccionarSiguienteOAnterior ---
    
    
    /**
     * Navega directamente a un índice específico en la lista principal.
     * Este método simplemente delega la llamada al método central de selección.
     *
     * @param index El índice del elemento al que se desea navegar.
     */
    public void navegarAIndice(int index) {
        if (model == null || model.getModeloLista() == null) {
            return;
        }

        if (index >= 0 && index < model.getModeloLista().getSize()) {
            seleccionarImagenPorIndice(index);
        }
    } // --- Fin del método navegarAIndice ---
    
    
    
    // --- Getters y Setters de dependencias ---
    public void setModel(VisorModel model) { this.model = model; }
    public void setView(VisorView view) { this.view = view; }
    public void setController(VisorController controller) {
        this.controller = controller;
        if (this.controller != null && this.controller.getConfigurationManager() != null) {
            this.pageScrollIncrement = this.controller.getConfigurationManager().getInt(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE, 10);
        }
    } // --- Fin del método setController ---
    
    public void setRegistry(ComponentRegistry registry) { this.registry = registry; }
    public void setContextSensitiveActions(List<ContextSensitiveAction> actions) { this.contextSensitiveActions = actions; }
    public synchronized boolean isSincronizandoUI() { return sincronizandoUI; }
    public synchronized void setSincronizandoUI(boolean sincronizando) { this.sincronizandoUI = sincronizando; }
    
} // --- Fin de la clase ListCoordinator ---



