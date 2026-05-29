package controlador.services;

import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.FilterManager;
import controlador.managers.filter.FilterCriterion;
import controlador.managers.filter.FilterCriterion.FilterSource;
import controlador.managers.filter.FilterCriterion.FilterType;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;

/**
 * Servicio encargado de toda la lógica de filtrado.
 * El GeneralController ya no necesita saber cómo se añaden o eliminan filtros.
 */
public class FilterService {
    private static final Logger logger = LoggerFactory.getLogger(FilterService.class);
    private final FilterManager filterManager;
    private final ComponentRegistry registry;
    private final VisorModel model;

    public FilterService(FilterManager filterManager, ComponentRegistry registry, VisorModel model) {
        this.filterManager = filterManager;
        this.registry = registry;
        this.model = model;
    }

    public void añadirFiltro(FilterSource source, FilterType type) {
        limpiarFiltroRapidoSiActivo();
        JTextField tf = registry.get("textfield.filtro.texto");
        if (tf == null || tf.getText().isBlank()) return;

        filterManager.addFilter(new FilterCriterion(tf.getText(), source, type));
        tf.setText("");
        filterManager.gestionarFiltroPersistente();
    }

    public void añadirFiltroSilencioso(String texto, FilterSource source, FilterType type) {
        if (texto == null || texto.isBlank()) return;
        filterManager.addFilter(new FilterCriterion(texto, source, type));
        filterManager.gestionarFiltroPersistente();
    }

    public void eliminarFiltroSeleccionado() {
        limpiarFiltroRapidoSiActivo();
        
        // Obtenemos el componente y hacemos el cast seguro a JList<FilterCriterion>
        Object component = registry.get("list.filtrosActivos");
        
        if (component instanceof JList<?>) {
            @SuppressWarnings("unchecked")
            JList<FilterCriterion> filterList = (JList<FilterCriterion>) component;
            
            if (filterList.getSelectedValue() != null) {
                filterManager.removeFilter(filterList.getSelectedValue());
                filterManager.gestionarFiltroPersistente();
            }
        } else {
            logger.error("El componente 'list.filtrosActivos' no es una JList o no existe.");
        }
    }

    public void limpiarTodosLosFiltros() {
        limpiarFiltroRapidoSiActivo();
        filterManager.clearFilters();
        filterManager.gestionarFiltroPersistente();
    }

    public void cambiarTipoFiltro(FilterSource nuevoSource) {
        filterManager.setFiltroActivoSource(nuevoSource);
    }

    private void limpiarFiltroRapidoSiActivo() {
        JTextField searchField = registry.get("textfield.filtro.orden");
        if (model.isLiveFilterActive()) {
            filterManager.setLiveFilterActive(false);
        }
        if (searchField != null) {
            SwingUtilities.invokeLater(() -> searchField.setText(""));
        }
    }
}