package controlador.services;

import controlador.VisorController;
import controlador.commands.AppActionCommands;
import controlador.managers.ConfigApplicationManager;
import controlador.managers.FilterManager;
import controlador.managers.InfobarStatusManager;
import controlador.managers.filter.FilterCriterion;
import controlador.managers.filter.FilterCriterion.FilterSource;
import controlador.utils.ComponentRegistry;
import modelo.ListContext;
import modelo.VisorModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Servicio encargado de la búsqueda de coincidencias (Tornado),
 * la ordenación de la lista de archivos y la sincronización
 * del botón de ordenación.
 */
public class SearchSortService {
    private static final Logger logger = LoggerFactory.getLogger(SearchSortService.class);

    private final VisorModel model;
    private final FilterManager filterManager;
    private final ComponentRegistry registry;
    private Map<String, Action> actionMap;
    private final ConfigApplicationManager configAppManager;
    private InfobarStatusManager statusBarManager;
    private final ConfigurationManager configuration;
    private VisorController visorController;

    private Border sortButtonActiveBorder;
    private Border sortButtonInactiveBorder;
    private boolean sortBordersInitialized = false;

    public SearchSortService(VisorModel model, FilterManager filterManager, ComponentRegistry registry,
                             ConfigApplicationManager configAppManager, ConfigurationManager configuration) {
        this.model = model;
        this.filterManager = filterManager;
        this.registry = registry;
        this.configAppManager = configAppManager;
        this.configuration = configuration;
    }

    public void setActionMap(Map<String, Action> actionMap) {
        this.actionMap = actionMap;
    }

    public void setStatusBarManager(InfobarStatusManager statusBarManager) {
        this.statusBarManager = statusBarManager;
    }

    public void setVisorController(VisorController visorController) {
        this.visorController = visorController;
    }

    // ==================== BÚSQUEDA (TORNADO) ====================

    public void buscarSiguienteCoincidencia() {
        if (registry == null || model == null || visorController == null || filterManager == null
                || visorController.getListCoordinator() == null) {
            logger.warn("[SearchSortService] No se puede buscar, faltan dependencias críticas.");
            return;
        }

        JTextField searchField = registry.get("textfield.filtro.orden");
        if (searchField == null) {
            logger.error("[SearchSortService] No se encontró 'textfield.filtro.orden' en el registro.");
            return;
        }

        String searchText = searchField.getText();
        if (searchText.isBlank() || searchText.equals("Texto a buscar...")) {
            return;
        }

        ListContext currentContext = model.getCurrentListContext();
        DefaultListModel<String> masterListModel = currentContext.getModeloLista();
        int startIndex = visorController.getListCoordinator().getOfficialSelectedIndex();

        int foundIndex = filterManager.buscarSiguiente(masterListModel, startIndex, searchText);

        if (foundIndex != -1) {
            visorController.getListCoordinator().seleccionarImagenPorIndice(foundIndex);
        } else {
            if (statusBarManager != null) {
                statusBarManager.mostrarMensajeTemporal("No se encontró: \"" + searchText + "\"", 3000);
            }
        }
    }

    // ==================== FILTRO EN VIVO (TORNADO) ====================

    public void onLiveFilterStateChanged(boolean isSelected) {
        filterManager.setLiveFilterActive(isSelected);

        Action liveFilterAction = actionMap.get(AppActionCommands.CMD_FILTRO_TOGGLE_LIVE_FILTER);
        if (configAppManager != null && liveFilterAction != null) {
            configAppManager.actualizarAspectoBotonToggle(liveFilterAction, isSelected);
            sincronizarEstadoControlesTornado();
        }
    }

    public void limpiarEstadoFiltroRapidoSiActivo() {
        JTextField searchField = registry.get("textfield.filtro.orden");

        if (model.isLiveFilterActive()) {
            onLiveFilterStateChanged(false);
        }

        if (searchField != null) {
            SwingUtilities.invokeLater(() -> searchField.setText(""));
        }
    }

    public void solicitarPersistenciaDeFiltroRapido() {
        logger.debug("[SearchSortService] Solicitud para AÑADIR filtro rápido a persistentes...");

        JTextField searchField = registry.get("textfield.filtro.orden");
        if (searchField == null) return;

        if (!model.isLiveFilterActive()) {
            logger.warn("[SearchSortService] No se puede persistir: el filtro rápido no está activo.");
            return;
        }

        String text = searchField.getText();
        if (text == null || text.isBlank() || text.equals("Texto a buscar...")) {
            logger.warn("[SearchSortService] No se puede persistir: el campo de texto está vacío o es placeholder.");
            return;
        }

        filterManager.setLiveFilterActive(false);

        FilterSource source = filterManager.getFiltroActivoSource();
        String[] terms = text.split(",");
        for (String term : terms) {
            String trimmed = term.trim();
            if (!trimmed.isEmpty()) {
                filterManager.addFilter(new FilterCriterion(trimmed, source, FilterCriterion.FilterType.CONTAINS));
            }
        }

        filterManager.gestionarFiltroPersistente();

        SwingUtilities.invokeLater(() -> searchField.setText(""));

        Action persistAction = actionMap.get(AppActionCommands.CMD_FILTRO_ACTIVO);
        if (configAppManager != null && persistAction != null) {
            configAppManager.actualizarAspectoBotonToggle(persistAction, false);
        }

        sincronizarEstadoControlesTornado();
        logger.debug("[SearchSortService] Filtro rápido añadido a persistentes y modo tornado desactivado.");
    }

    public void sincronizarEstadoControlesTornado() {
        Action persistAction = actionMap.get(AppActionCommands.CMD_FILTRO_ACTIVO);
        Action toggleAction = actionMap.get(AppActionCommands.CMD_FILTRO_TOGGLE_LIVE_FILTER);

        if (toggleAction != null) {
            toggleAction.setEnabled(true);
        }

        if (persistAction != null) {
            boolean debeEstarHabilitado = model.isLiveFilterActive();
            if (debeEstarHabilitado) {
                JTextField searchField = registry.get("textfield.filtro.orden");
                String text = (searchField != null) ? searchField.getText() : "";
                debeEstarHabilitado = debeEstarHabilitado && !text.isBlank() && !text.equals("Texto a buscar...");
            }
            persistAction.setEnabled(debeEstarHabilitado);
        }
    }

    public void configurePlaceholderText(JTextField searchField) {
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getText().equals("Texto a buscar...")) {
                    searchField.setText("");
                    searchField.setForeground(UIManager.getColor("TextField.foreground"));
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Texto a buscar...");
                    searchField.setForeground(UIManager.getColor("TextField.inactiveForeground"));
                }
            }
        });
        searchField.setText("Texto a buscar...");
        searchField.setForeground(UIManager.getColor("TextField.inactiveForeground"));
    }

    // ==================== ORDENACIÓN ====================

    public void resortFileListAndSyncButton() {
        VisorModel.SortDirection direction = model.getSortDirection();
        ListContext currentContext = model.getCurrentListContext();
        if (currentContext == null) return;

        DefaultListModel<String> listModel = currentContext.getModeloLista();
        if (listModel.isEmpty()) {
            syncSortButtonUI(direction);
            return;
        }

        if (direction == VisorModel.SortDirection.NONE) {
            visorController.getGeneralController().solicitarCargaDesdeNuevaRaiz(model.getCarpetaRaizActual());
            syncSortButtonUI(direction);
            return;
        }

        List<String> items = new ArrayList<>();
        for (int i = 0; i < listModel.getSize(); i++) {
            items.add(listModel.getElementAt(i));
        }

        String selectedKey = model.getSelectedImageKey();

        items.sort((pathStr1, pathStr2) -> {
            Path p1 = Paths.get(pathStr1);
            Path p2 = Paths.get(pathStr2);
            Path fn1 = p1.getFileName();
            Path fn2 = p2.getFileName();
            String s1 = (fn1 != null) ? fn1.toString() : p1.toString();
            String s2 = (fn2 != null) ? fn2.toString() : p2.toString();
            return s1.compareToIgnoreCase(s2);
        });

        if (direction == VisorModel.SortDirection.DESCENDING) {
            Collections.reverse(items);
        }

        listModel.clear();
        listModel.addAll(items);

        int newIndex = (selectedKey != null) ? listModel.indexOf(selectedKey) : -1;
        if (newIndex == -1 && !listModel.isEmpty()) {
            newIndex = 0;
        }

        if (visorController.getListCoordinator() != null) {
            visorController.getListCoordinator().reiniciarYSeleccionarIndice(newIndex);
        }

        syncSortButtonUI(direction);
    }

    public void sincronizarBotonDeOrdenacion() {
        if (model == null) return;
        syncSortButtonUI(model.getSortDirection());
    }

    private void syncSortButtonUI(VisorModel.SortDirection direction) {
        Action sortAction = actionMap.get(AppActionCommands.CMD_ORDEN_CICLO);
        if (sortAction == null) return;

        String buttonKey = "interfaz.boton.orden_lista.orden_ciclo";
        JButton sortButton = registry.get(buttonKey);
        if (sortButton == null) return;

        if (!sortBordersInitialized) {
            int thickness = 2;
            Color activeColor = UIManager.getColor("Component.accentColor");
            if (activeColor == null) {
                activeColor = UIManager.getColor("Component.focusColor");
                if (activeColor == null) {
                    activeColor = new Color(59, 142, 255);
                }
            }
            this.sortButtonActiveBorder = BorderFactory.createLineBorder(activeColor, thickness);
            this.sortButtonInactiveBorder = BorderFactory.createEmptyBorder(thickness, thickness, thickness, thickness);
            sortBordersInitialized = true;
        }

        String iconKey;
        String tooltip;

        switch (direction) {
            case ASCENDING:
                iconKey = "30004-orden_ascendente.png";
                tooltip = "Orden: Ascendente (clic para Z-A)";
                sortButton.setBorder(this.sortButtonActiveBorder);
                break;
            case DESCENDING:
                iconKey = "30005-orden_descendente.png";
                tooltip = "Orden: Descendente (clic para apagar)";
                sortButton.setBorder(this.sortButtonActiveBorder);
                break;
            case NONE:
            default:
                iconKey = "30006-orden_off.png";
                tooltip = "Orden: Apagado (clic para A-Z)";
                sortButton.setBorder(this.sortButtonInactiveBorder);
                break;
        }

        int iconSize = configuration.getInt(ConfigKeys.ICONOS_ANCHO, 24);
        ImageIcon newIcon = visorController.getIconUtils().getScaledIcon(iconKey, iconSize, iconSize);
        sortAction.putValue(Action.SMALL_ICON, newIcon);
        sortAction.putValue(Action.SHORT_DESCRIPTION, tooltip);

        sortButton.repaint();
    }
}
