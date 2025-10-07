package controlador.managers;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.filter.FilterCriterion;
import controlador.managers.filter.FilterCriterion.FilterSource;
import controlador.managers.filter.FilterCriterion.FilterType;
import modelo.VisorModel;

/**
 * Gestiona la lógica de búsqueda y el conjunto de reglas de filtrado activas.
 */
public class FilterManager {

	private static final Logger logger = LoggerFactory.getLogger(FilterManager.class);

	private final List<FilterCriterion> activeFilters = new ArrayList<>();

	// --- Campos originales de FilterManager para "Carpeta Virtual" ---
	private DefaultListModel<String> masterModelSinFiltro = null;
	private Map<String, Path> masterMapSinFiltro = null;

	// --- Dependencias Inyectadas ---
	private VisorModel model;
	private controlador.VisorController visorController;
	private controlador.utils.ComponentRegistry registry;

	// --- Estado para FILTRO PERSISTENTE (Movido desde GeneralController) ---
	private DefaultListModel<String> persistente_listaMaestraOriginal;
	private String persistente_punteroOriginalKey;
	private boolean persistente_activo = false;

	// --- Estado para FILTRO EN VIVO / TORNADO (Movido desde GeneralController) ---
	private DefaultListModel<String> masterModelSinFinito; // Anteriormente 'masterModelSinFiltro' en GeneralController
	private int indiceSeleccionadoAntesDeFiltrar = -1;
	private FilterCriterion tornadoCriterion;
	private FilterSource filtroActivoSource = FilterSource.FILENAME;    
	private javax.swing.SwingWorker<DefaultListModel<String>, Void> liveFilterWorker;
	private InfobarStatusManager statusBarManager;
	private DefaultListModel<String> absoluteMasterList = new DefaultListModel<>();
	
    public FilterManager(VisorModel model) {
        this.model = model;
    } // --- Fin del constructor FilterManager ---

    public void addFilter(FilterCriterion newFilter) {
        if (!activeFilters.contains(newFilter)) {
            activeFilters.add(newFilter);
            logger.debug("Filtro añadido: {}", newFilter);
        }
    } // --- Fin del método addFilter ---

    public void removeFilter(FilterCriterion filterToRemove) {
        if (activeFilters.remove(filterToRemove)) {
            logger.debug("Filtro eliminado: {}", filterToRemove);
        }
    } // --- Fin del método removeFilter ---
    
    public void clearFilters() {
        if (!activeFilters.isEmpty()) {
            activeFilters.clear();
            logger.debug("Todos los filtros han sido eliminados.");
        }
    } // --- Fin del método clearFilters ---

    public List<FilterCriterion> getActiveFilters() {
        return java.util.Collections.unmodifiableList(activeFilters);
    } // --- Fin del método getActiveFilters ---
    
    public boolean isFilterActive() {
        return !activeFilters.isEmpty();
    } // --- Fin del método isFilterActive ---

    public int buscarSiguiente(DefaultListModel<String> masterListModel, int startIndex, String searchText) {
        if (masterListModel == null || masterListModel.isEmpty() || searchText == null || searchText.isBlank()) return -1;
        int totalItems = masterListModel.getSize();
        for (int i = 1; i <= totalItems; i++) {
            int currentIndex = (startIndex + i) % totalItems;
            String currentItem = masterListModel.getElementAt(currentIndex);
            if (currentItem.toLowerCase().contains(searchText.toLowerCase())) {
                return currentIndex;
            }
        }
        return -1;
    } // --- Fin del método buscarSiguiente ---

    
    public DefaultListModel<String> applyFilters(DefaultListModel<String> masterListModel) {
        DefaultListModel<String> filteredModel = new DefaultListModel<>();
        if (masterListModel == null) return filteredModel;

        if (!isFilterActive()) {
            for(int i = 0; i < masterListModel.getSize(); i++){
                filteredModel.addElement(masterListModel.getElementAt(i));
            }
            return filteredModel;
        }

        for (int i = 0; i < masterListModel.getSize(); i++) {
            String itemKey = masterListModel.getElementAt(i);
            if (passesAllFilters(itemKey)) {
                filteredModel.addElement(itemKey);
            }
        }
        
        logger.debug("Filtros aplicados. {} resultados de {} totales.", filteredModel.getSize(), masterListModel.getSize());
        return filteredModel;
    } // --- Fin del método applyFilters ---

    private boolean passesAllFilters(String itemKey) {
        Path filePath = model.getRutaCompleta(itemKey);
        if (filePath == null) { 
            return false;
        }
        
        String fileName = filePath.getFileName().toString().toLowerCase();
        String folderPath = (filePath.getParent() != null) ? filePath.getParent().toString().toLowerCase() : "";

        // Iteramos sobre CADA filtro en la lista de filtros activos.
        for (FilterCriterion filter : activeFilters) {
            
            // Determinamos sobre qué texto vamos a buscar (nombre de archivo, carpeta o tag)
            String targetString;
            switch (filter.getSourceType()) {
                case FOLDER:
                    targetString = folderPath;
                    break;
                case TAG:
                    // TODO: Obtener los tags de la imagen 'itemKey' desde la base de datos
                    // y comprobar si alguno coincide con filter.getValue().
                    // Por ahora, para que no falle, lo tratamos como si no coincidiera.
                    targetString = ""; 
                    break;
                case TEXT:
                default:
                    targetString = fileName;
                    break;
            }
            
            String filterValue = filter.getValue().toLowerCase();
            if (filterValue.isEmpty()) {
                continue; // Ignoramos filtros con valor vacío
            }

            // Verificamos si el texto objetivo contiene el valor del filtro.
            boolean conditionMet = targetString.contains(filterValue);

            // --- INICIO DE LA NUEVA LÓGICA UNIFICADA ---
            
            // CASO 1: Es un filtro de tipo AÑADIR (ADD) y la condición NO se cumple.
            // Si la imagen NO contiene el texto que debería, la descartamos inmediatamente.
            if (filter.getLogic() == FilterCriterion.Logic.ADD && !conditionMet) {
                return false; // No cumple una de las condiciones obligatorias.
            }

            // CASO 2: Es un filtro de tipo EXCLUIR (NOT) y la condición SÍ se cumple.
            // Si la imagen SÍ contiene el texto que NO debería, la descartamos inmediatamente.
            if (filter.getLogic() == FilterCriterion.Logic.NOT && conditionMet) {
                return false; // Contiene algo prohibido.
            }
            // --- FIN DE LA NUEVA LÓGICA UNIFICADA ---
        }

        // Si el bucle termina, significa que la imagen ha pasado todas las reglas de filtro.
        return true;

    } // ---FIN de metodo passesAllFilters---
    
    
    /**
     * Activa el modo "carpeta virtual" y aplica un filtro.
     * @return Un objeto FilterResult que contiene tanto el modelo filtrado como su mapa de rutas.
     */
    public FilterResult activarYAplicarFiltroEnVivo(String textoFiltro) {
        if (this.masterModelSinFiltro == null) {
            this.masterModelSinFiltro = model.getCurrentListContext().getModeloLista();
            this.masterMapSinFiltro = model.getCurrentListContext().getRutaCompletaMap();
        }

        clearFilters();
        if (textoFiltro != null && !textoFiltro.isBlank()) {
            addFilter(new FilterCriterion(textoFiltro, FilterSource.FILENAME, FilterType.CONTAINS));
        }

        DefaultListModel<String> filteredModel = applyFilters(this.masterModelSinFiltro);
        Map<String, Path> filteredMap = new HashMap<>();
        for (String key : Collections.list(filteredModel.elements())) {
            filteredMap.put(key, this.masterMapSinFiltro.get(key));
        }

        return new FilterResult(filteredModel, filteredMap);
        
    } // --- Fin del metodo activarYAplicarFiltroEnVivo --- 
    
    
    /**
     * Desactiva el filtro en vivo y restaura el estado original.
     * @return Un FilterResult con el modelo y mapa originales.
     */
    public FilterResult desactivarFiltroEnVivoYRestaurar() {
        if (this.masterModelSinFiltro == null) {
            return new FilterResult(model.getCurrentListContext().getModeloLista(), model.getCurrentListContext().getRutaCompletaMap());
        }

        FilterResult originalState = new FilterResult(this.masterModelSinFiltro, this.masterMapSinFiltro);

        this.masterModelSinFiltro = null;
        this.masterMapSinFiltro = null;
        clearFilters();

        return originalState;
        
    } // --- Fin del metodo desactivarFiltroEnVivoYRestaurar --- 
    
    
    /**
     * Devuelve el mapa de rutas original guardado. Debe llamarse DESPUÉS de desactivarFiltroEnVivo.
     * @return El mapa de rutas original.
     */
    public Map<String, Path> getMasterMapRestaurado() {
        // Este método es un poco "feo", pero nos permite devolver ambas cosas (modelo y mapa).
        // La alternativa sería crear una clase contenedora.
        return this.masterMapSinFiltro != null ? this.masterMapSinFiltro : new HashMap<>();
        
    } // --- Fin del metodo getMasterMapRestaurado ---

   
    
// ****************************************************************************************************************************************** GETTERS Y SETTERS
    
    public void setVisorController(controlador.VisorController visorController) {
        this.visorController = Objects.requireNonNull(visorController, "VisorController no puede ser null en FilterManager");
    } // ---FIN de metodo setVisorController---

    public void setRegistry(controlador.utils.ComponentRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null en FilterManager");
    } // ---FIN de metodo setRegistry---
    
    public void setStatusBarManager(InfobarStatusManager statusBarManager) {
        this.statusBarManager = Objects.requireNonNull(statusBarManager, "InfobarStatusManager no puede ser null en FilterManager");
    } // ---FIN de metodo setStatusBarManager---
    
    /**
     * Establece el tipo de fuente por defecto para los nuevos filtros que se añadan.
     * @param nuevoSource El nuevo FilterSource a usar.
     */
    public void setFiltroActivoSource(FilterSource nuevoSource) {
        if (this.filtroActivoSource != nuevoSource) {
            this.filtroActivoSource = nuevoSource;
            logger.debug("[FilterManager] Tipo de filtro activo cambiado a: {}", nuevoSource);
        }
    } // ---FIN de metodo setFiltroActivoSource---

    /**
     * Obtiene el tipo de fuente de filtro actualmente activo.
     * @return El FilterSource activo.
     */
    public FilterSource getFiltroActivoSource() {
        return this.filtroActivoSource;
    } // ---FIN de metodo getFiltroActivoSource---
    
    
    /**
     * Establece la lista maestra absoluta y sin filtrar.
     * Este método debe ser llamado CADA VEZ que se carga una nueva carpeta.
     * Almacena un clon para asegurar que la fuente de verdad no sea modificada externamente.
     * @param masterList La nueva lista completa de archivos.
     */
    public void setAbsoluteMasterList(DefaultListModel<String> masterList) {
        this.absoluteMasterList = clonarModelo(masterList);
        logger.info("[FilterManager] Nueva lista maestra absoluta establecida con {} elementos.", this.absoluteMasterList.getSize());
    } // ---FIN de metodo setAbsoluteMasterList---
    
    
    
// **********************************************************************************************************************************************************
    
    
    /**
     * Resetea completamente el estado del filtro persistente.
     * Se debe llamar cuando se carga una nueva carpeta, invalidando cualquier filtro anterior.
     */
    public void resetPersistentFilterState() {
        if (this.persistente_activo) {
            logger.info("[FilterManager] Reseteando estado de filtro persistente debido a carga de nuevo contexto.");
            this.persistente_listaMaestraOriginal = null;
            this.persistente_punteroOriginalKey = null;
            this.persistente_activo = false;
        }
        
        // También limpiamos los criterios de filtro, ya que pertenecen al contexto anterior.
        clearFilters();
        
        // Y nos aseguramos de que la UI se actualice para reflejar que no hay filtros.
        refrescarConFiltrosPersistentes();
        
    } // ---FIN de metodo resetPersistentFilterState---
    
    
    /**
     * Helper para clonar un DefaultListModel.
     */
    private DefaultListModel<String> clonarModelo(DefaultListModel<String> original) {
        if (original == null) return null;
        DefaultListModel<String> copia = new DefaultListModel<>();
        for (int i = 0; i < original.getSize(); i++) {
            copia.addElement(original.getElementAt(i));
        }
        return copia;
    }// --- Fin del método clonarModelo ---

    // --- LÓGICA DE FILTRO PERSISTENTE ---

    public void gestionarFiltroPersistente() {
        refrescarConFiltrosPersistentes();
    } // --- Fin del método gestionarFiltroPersistente ---

    public void refrescarConFiltrosPersistentes() {
        Objects.requireNonNull(registry, "Registry no ha sido inyectado en FilterManager");

        JList<FilterCriterion> filterListUI = registry.get("list.filtrosActivos");
        if (filterListUI != null) {
            DefaultListModel<FilterCriterion> modelUI;
            if (filterListUI.getModel() instanceof DefaultListModel) {
                modelUI = (DefaultListModel<FilterCriterion>) filterListUI.getModel();
            } else {
                modelUI = new DefaultListModel<>();
                filterListUI.setModel(modelUI);
            }
            modelUI.clear();
            modelUI.addAll(getActiveFilters());
        }

        DefaultListModel<String> listaResultado;
        int indiceASeleccionar = 0;

        if (isFilterActive()) {
            if (!persistente_activo) {
                // Guardamos el puntero, pero la lista la leemos de la fuente de la verdad
                this.persistente_punteroOriginalKey = model.getSelectedImageKey();
                this.persistente_activo = true;
            }
            // SIEMPRE filtramos desde la lista maestra absoluta
            listaResultado = applyFilters(this.absoluteMasterList);

        } else { // No hay filtros activos
            // Restauramos la lista maestra absoluta
            listaResultado = this.absoluteMasterList;
            
            if (persistente_activo) {
                int indiceOriginal = (persistente_punteroOriginalKey != null) 
                                   ? listaResultado.indexOf(persistente_punteroOriginalKey) : -1;
                indiceASeleccionar = (indiceOriginal != -1) ? indiceOriginal : 0;
                
                // Limpiamos el estado del filtro persistente
                this.persistente_punteroOriginalKey = null;
                this.persistente_activo = false;
            }
        }
        
        actualizarListaVisibleConResultado(listaResultado, indiceASeleccionar, !isFilterActive());
        
    } // ---FIN de metodo refrescarConFiltrosPersistentes---

    private void actualizarListaVisibleConResultado(DefaultListModel<String> nuevaLista, int indiceASeleccionar, boolean resetearCheckpoint) {
        // Aseguramos que las dependencias estén inyectadas antes de usarlas
        Objects.requireNonNull(visorController, "VisorController no ha sido inyectado en FilterManager");
        
        DefaultListModel<String> modeloEnUso = model.getModeloLista();
        final int finalIndice = indiceASeleccionar;

        javax.swing.SwingUtilities.invokeLater(() -> {
            modeloEnUso.clear();
            modeloEnUso.addAll(java.util.Collections.list(nuevaLista.elements()));
            
            String titulo = isFilterActive() ? "Archivos (Filtro): " : "Archivos: ";
            visorController.getView().setTituloPanelIzquierdo(titulo + modeloEnUso.getSize());
            
            visorController.getListCoordinator().reiniciarYSeleccionarIndice(finalIndice);
            
            if (resetearCheckpoint) {
                logger.info("[FM] Reseteando estado del checkpoint persistente...");
                this.persistente_listaMaestraOriginal = null;
                this.persistente_punteroOriginalKey = null;
                this.persistente_activo = false;
            }
        });
    } // --- Fin del método actualizarListaVisibleConResultado ---

    // --- LÓGICA DE FILTRO EN VIVO (TORNADO) ---

    public void setLiveFilterActive(boolean isSelected) {
        model.setLiveFilterActive(isSelected);
        logger.debug("[FilterManager] Modo Filtro en Vivo cambiado a: {}", isSelected);
        
        if (isSelected) {
            // ¡CORRECCIÓN CLAVE! El backup para el filtro en vivo TAMBIÉN se toma de la lista maestra absoluta.
            this.masterModelSinFinito = clonarModelo(this.absoluteMasterList);
            this.indiceSeleccionadoAntesDeFiltrar = visorController.getListCoordinator().getOfficialSelectedIndex();
            actualizarFiltro();
        } else {
            limpiarFiltro();
        }
    } // --- Fin del método setLiveFilterActive ---

    public void actualizarFiltro() {
        if (!model.isLiveFilterActive() || this.masterModelSinFinito == null) {
            return;
        }
        Objects.requireNonNull(registry, "Registry no ha sido inyectado en FilterManager");

        // 1. Cancelar cualquier filtro anterior que aún se esté ejecutando.
        if (liveFilterWorker != null && !liveFilterWorker.isDone()) {
            liveFilterWorker.cancel(true);
        }

        // --- INICIO MODIFICACIÓN: Mostrar mensaje de "Filtrando..." ---
        if (statusBarManager != null) {
            statusBarManager.mostrarMensaje("Filtrando...");
        }
        // --- FIN MODIFICACIÓN ---
        
        // 2. Crear un nuevo SwingWorker para la tarea de filtrado.
        liveFilterWorker = new javax.swing.SwingWorker<DefaultListModel<String>, Void>() {
            
            @Override
            protected DefaultListModel<String> doInBackground() throws Exception {
                // ESTO SE EJECUTA EN UN HILO DE FONDO (SEGURO PARA TAREAS LARGAS)
                
                javax.swing.JTextField searchField = registry.get("textfield.filtro.orden");
                if (searchField == null) return new DefaultListModel<>(); // Devuelve lista vacía si hay error
                
                String searchText = searchField.getText();
                if (searchText.equals("Texto a buscar...")) {
                    searchText = "";
                }

                // La lógica de negocio para determinar qué filtrar permanece igual
                if (tornadoCriterion != null) {
                    removeFilter(tornadoCriterion);
                    tornadoCriterion = null;
                }

                if (!searchText.isBlank()) {
                    tornadoCriterion = new FilterCriterion(searchText, FilterSource.FILENAME, FilterType.CONTAINS);
                    addFilter(tornadoCriterion);
                }

                // La parte pesada: aplicar los filtros a la lista maestra de ~12,000 elementos
                return applyFilters(masterModelSinFinito);
            } // --- Fin del método doInBackground ---

            @Override
            protected void done() {
                // ESTO SE EJECUTA DE VUELTA EN EL EDT (SEGURO PARA ACTUALIZAR LA UI)
                try {
                    if (isCancelled()) {
                        logger.debug("[FilterManager] Tarea de filtrado en vivo cancelada.");
                        // No limpiamos el mensaje aquí para evitar parpadeos si se lanza un nuevo filtro inmediatamente
                        return;
                    }

                    DefaultListModel<String> filteredContent = get(); // Obtiene el resultado del doInBackground
                    
                    DefaultListModel<String> modeloEnUso = model.getModeloLista();
                    modeloEnUso.clear();
                    modeloEnUso.addAll(java.util.Collections.list(filteredContent.elements()));

                    visorController.getListCoordinator().reiniciarYSeleccionarIndice(modeloEnUso.isEmpty() ? -1 : 0);
                    
                    // --- INICIO MODIFICACIÓN: Mostrar mensaje con el resultado ---
                    if (statusBarManager != null) {
                        javax.swing.JTextField searchField = registry.get("textfield.filtro.orden");
                        String searchText = (searchField != null) ? searchField.getText() : "";
                        
                        if (searchText.isBlank() || searchText.equals("Texto a buscar...")) {
                            statusBarManager.mostrarMensajeTemporal("Filtro limpiado. Mostrando " + masterModelSinFinito.getSize() + " elementos.", 3000);
                        } else {
                            statusBarManager.mostrarMensajeTemporal("Filtro aplicado. " + filteredContent.getSize() + " resultados.", 3000);
                        }
                    }
                    // --- FIN MODIFICACIÓN ---
                    
                } catch (Exception e) {
                    if (!(e instanceof java.util.concurrent.CancellationException)) {
                        logger.error("Error al finalizar la tarea de filtrado en vivo", e);
                        if (statusBarManager != null) {
                            statusBarManager.mostrarMensajeTemporal("Error al aplicar el filtro.", 3000);
                        }
                    }
                }
            } // --- Fin del método done ---
        };

        // 3. Ejecutar el worker.
        liveFilterWorker.execute();
        
    } // --- Fin del método actualizarFiltro ---
    

    public void limpiarFiltro() {
        if (this.masterModelSinFinito == null) return;
        Objects.requireNonNull(visorController, "VisorController no ha sido inyectado en FilterManager");

        if (this.tornadoCriterion != null) {
            removeFilter(this.tornadoCriterion);
            this.tornadoCriterion = null;
        }

        DefaultListModel<String> modeloEnUso = model.getModeloLista();
        modeloEnUso.clear();
        modeloEnUso.addAll(java.util.Collections.list(this.masterModelSinFinito.elements()));
        
        visorController.getListCoordinator().reiniciarYSeleccionarIndice(this.indiceSeleccionadoAntesDeFiltrar);

        this.masterModelSinFinito = null;
        this.indiceSeleccionadoAntesDeFiltrar = -1;
    } // --- Fin del método limpiarFiltro ---
    
    
   
    
// ****************************************************************************************** record
    
    public record FilterResult(DefaultListModel<String> model, Map<String, Path> pathMap) {}
    
} // --- Fin de la clase FilterManager ---