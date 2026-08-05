package controlador.managers;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.GeneralController;
import controlador.commands.AppActionCommands;
import controlador.factory.ActionFactory;
import controlador.managers.filter.FilterCriterion;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.zoom.ZoomModeEnum;

/**
 * Gestiona la creación, configuración y visualización de todos los menús
 * contextuales (popups) de la aplicación.
 * Centraliza la lógica de menús para que sea fácil encontrar y modificar cualquier popup.
 */
public class MenuPopupManager {
    private static final Logger logger = LoggerFactory.getLogger(MenuPopupManager.class);

    // --- Dependencias ---
    private final Map<String, Action> actionMap;
    private final ComponentRegistry registry;
    private final GeneralController generalController;
    private final ActionFactory actionFactory;
    private final VisorModel model;
    private final DataManager dataManager; // Nuevo

    // --- Menús contextuales ---
    private JPopupMenu popupMenuImagenPrincipal;
    private JPopupMenu popupMenuListaNombres;
    private JPopupMenu popupMenuListaMiniaturas;
    private JPopupMenu popupMenuGrid;

    // --- Listeners ---
    private MouseListener popupListenerImagenPrincipal;
    private MouseListener popupListenerListaNombres;
    private MouseListener popupListenerListaMiniaturas;
    private MouseListener popupListenerGrid;

    public MenuPopupManager(Map<String, Action> actionMap, ComponentRegistry registry,
                            GeneralController generalController, ActionFactory actionFactory,
                            VisorModel model, DataManager dataManager) { // Actualizado
        this.actionMap = actionMap;
        this.registry = registry;
        this.generalController = generalController;
        this.actionFactory = actionFactory;
        this.model = model;
        this.dataManager = dataManager; // Nuevo
    }

    // ==================== CONFIGURACIÓN GENERAL ====================

    /**
     * Configura todos los menús contextuales de la aplicación.
     * Se llama durante la inicialización de la UI.
     */
    public void configurarMenusContextuales() {
        logger.debug("  [MenuPopupManager] Configurando Menús Contextuales...");
        if (actionMap == null || actionMap.isEmpty()) {
            logger.warn("WARN [configurarMenusContextuales]: ActionMap es nulo o vacío.");
            return;
        }

        // ==================== MENÚ VISUALIZADOR ====================
        popupMenuImagenPrincipal = crearMenuContextualStandard(null);
        popupMenuListaNombres = crearMenuContextualStandard(registry.get("list.nombresArchivo"));
        popupMenuListaMiniaturas = crearMenuContextualStandard(registry.get("list.miniaturas"));

        // --- Imagen principal ---
        JLabel labelImagen = registry.get("label.imagenPrincipal");
        if (labelImagen != null) {
            popupListenerImagenPrincipal = new PopupListener(popupMenuImagenPrincipal);
            labelImagen.addMouseListener(popupListenerImagenPrincipal);
            logger.debug("    -> Menú contextual añadido a label.imagenPrincipal.");
        }

        // --- Lista de nombres ---
        JList<String> listaNombres = registry.get("list.nombresArchivo");
        if (listaNombres != null) {
            popupListenerListaNombres = new PopupListener(popupMenuListaNombres);
            listaNombres.addMouseListener(popupListenerListaNombres);
            logger.debug("    -> Menú contextual añadido a list.nombresArchivo.");
        }

        // --- Lista de miniaturas ---
        JList<String> listaMiniaturas = registry.get("list.miniaturas");
        if (listaMiniaturas != null) {
            popupListenerListaMiniaturas = new PopupListener(popupMenuListaMiniaturas);
            listaMiniaturas.addMouseListener(popupListenerListaMiniaturas);
            logger.debug("    -> Menú contextual añadido a list.miniaturas.");
        }

        // ==================== MENÚ GRID ====================
        JList<String> gridList = registry.get("list.grid");
        if (gridList != null) {
            popupMenuGrid = crearMenuContextualStandard(gridList);

            popupListenerGrid = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        int index = gridList.locationToIndex(e.getPoint());
                        if (index != -1 && !gridList.isSelectedIndex(index)) {
                            gridList.setSelectedIndex(index);
                        }
                        popupMenuGrid.show(e.getComponent(), e.getX(), e.getY());
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        int index = gridList.locationToIndex(e.getPoint());
                        if (index != -1 && !gridList.isSelectedIndex(index)) {
                            gridList.setSelectedIndex(index);
                        }
                        popupMenuGrid.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            };

            gridList.addMouseListener(popupListenerGrid);
            logger.debug("    -> Menú contextual añadido a list.grid.");
        } else {
            logger.warn("WARN [configurarMenusContextuales]: 'list.grid' no encontrado.");
        }

        // ==================== MENÚ RUTAS ====================
        configurarMenusContextualesRuta();

        logger.debug("  [MenuPopupManager] Menús Contextuales configurados.");
    }

    // ==================== MENÚ ESTÁNDAR (VISUALIZADOR) ====================

    /**
     * Crea y devuelve un JPopupMenu con el conjunto estándar de acciones
     * para el modo VISUALIZADOR.
     */
    public JPopupMenu crearMenuContextualStandard(JList<String> listaFuente) {
        JPopupMenu menu = new JPopupMenu();
        if (actionMap == null) return menu;

        // 1. Acción de Marcar
        Action marcarAction = actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
        if (marcarAction != null) {
            menu.add(new JCheckBoxMenuItem(marcarAction));
        }

        menu.addSeparator();

        // 2. NUEVA OPCIÓN: Asignar Etiqueta
        JMenuItem assignTagItem = new JMenuItem("Asignar Etiqueta...");
        assignTagItem.addActionListener(e -> {
            if (listaFuente == null) return;
            List<String> selectedKeys = listaFuente.getSelectedValuesList();
            List<Path> paths = new ArrayList<>();
            Map<String, Path> rutaCompletaMap = model.getCurrentListContext().getRutaCompletaMap();
            for (String key : selectedKeys) {
                Path p = rutaCompletaMap.get(key);
                if (p != null) paths.add(p);
            }
            if (!paths.isEmpty()) {
                vista.dialogos.TagAssignmentDialog dialog = new vista.dialogos.TagAssignmentDialog(
                    (Frame) SwingUtilities.getWindowAncestor(listaFuente), dataManager, paths);
                dialog.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(listaFuente, "Selecciona al menos una imagen.");
            }
        });
        menu.add(assignTagItem);
        menu.addSeparator();

        // 3. Acciones de Archivo/Ubicación
        menu.add(new JMenuItem(actionMap.get(AppActionCommands.CMD_IMAGEN_LOCALIZAR)));
        Action copiarImagenAction = actionMap.get(AppActionCommands.CMD_COPIAR_IMAGEN);
        if (copiarImagenAction != null) {
            menu.add(new JMenuItem(copiarImagenAction));
        }

        menu.addSeparator();

        // 4. Acciones de Zoom/Vista
        menu.add(new JCheckBoxMenuItem(actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE)));
        menu.add(new JMenuItem(actionMap.get(AppActionCommands.CMD_ZOOM_RESET)));
        menu.addSeparator();
        menu.add(new JCheckBoxMenuItem(actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES)));
        menu.add(new JCheckBoxMenuItem(actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS)));

        return menu;
    } //--- Fin del metodo: crearMenuContextualStandard

    // ==================== MENÚ RUTAS ====================

    private void configurarMenusContextualesRuta() {
        JTextField tfRutaSup = registry.get("textfield.info.rutaImagen");
        JTextField tfCarpetaInf = registry.get("textfield.estado.carpetaRaiz");

        if (tfRutaSup != null) {
            tfRutaSup.addMouseListener(new PathPopupListener(tfRutaSup, true));
        }
        if (tfCarpetaInf != null) {
            tfCarpetaInf.addMouseListener(new PathPopupListener(tfCarpetaInf, false));
        }

        // Popup para el nombre de la imagen: copiar solo el nombre del archivo
        JLabel nombreLabel = registry.get("label.info.nombreArchivo");
        if (nombreLabel != null) {
            nombreLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) { maybeShow(e); }
                @Override
                public void mouseReleased(MouseEvent e) { maybeShow(e); }
                private void maybeShow(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        String selectedKey = model != null ? model.getSelectedImageKey() : null;
                        if (selectedKey == null) return;
                        Path fullPath = model.getRutaCompleta(selectedKey);
                        if (fullPath == null) {
                            Path raiz = model.getCarpetaRaizActual();
                            fullPath = (raiz != null) ? raiz.resolve(selectedKey) : Path.of(selectedKey);
                        }
                        if (fullPath == null) return;
                        final String soloNombre = fullPath.getFileName().toString();
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem copiarItem = new JMenuItem("Copiar nombre");
                        copiarItem.addActionListener(al -> {
                            java.awt.datatransfer.StringSelection selection =
                                new java.awt.datatransfer.StringSelection(soloNombre);
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                        });
                        menu.add(copiarItem);
                        Action copiarImagenAction = actionMap.get(AppActionCommands.CMD_COPIAR_IMAGEN);
                        if (copiarImagenAction != null) {
                            menu.add(new JMenuItem(copiarImagenAction));
                        }
                        menu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });
        }
    }

    // ==================== MENÚ CARRUSEL (VELOCIDAD) ====================

    /**
     * Muestra el menú de selección de velocidad del carrusel.
     */
    public void showCarouselSpeedMenu(Component invoker) {
        JPopupMenu speedMenu = new JPopupMenu();

        Map<String, Integer> speedOptions = new LinkedHashMap<>();
        speedOptions.put("Muy Rápido (1.0s)", 1000);
        speedOptions.put("Rápido (2.0s)", 2000);
        speedOptions.put("Normal (5.0s)", 5000);
        speedOptions.put("Lento (10.0s)", 10000);
        speedOptions.put("Muy Lento (20.0s)", 20000);

        controlador.managers.CarouselManager carouselManager = actionFactory.getCarouselManager();

        for (Map.Entry<String, Integer> entry : speedOptions.entrySet()) {
            String text = entry.getKey();
            int delayMs = entry.getValue();
            Action setSpeedAction = new controlador.actions.carousel.SetCarouselSpeedAction(
                model, carouselManager, text, delayMs);
            speedMenu.add(new JMenuItem(setSpeedAction));
        }

        speedMenu.addSeparator();

        Action setReverseSpeedAction = new controlador.actions.carousel.SetCarouselSpeedAction(
            model, carouselManager, "Velocidad Inversa (-5.0s)", -5000);
        speedMenu.add(new JMenuItem(setReverseSpeedAction));

        speedMenu.show(invoker, 0, -speedMenu.getPreferredSize().height);
    }

    // ==================== MENÚ ÁRBOL DE CARPETAS ====================

    /**
     * Crea y devuelve el menú contextual para el árbol de carpetas.
     */
    public JPopupMenu crearMenuContextualParaArbol() {
        JPopupMenu menu = new JPopupMenu();

        Action openAction = actionMap.get(AppActionCommands.CMD_TREE_OPEN_FOLDER);
        Action drillDownAction = actionMap.get(AppActionCommands.CMD_TREE_DRILL_DOWN_FOLDER);

        if (openAction != null) {
            menu.add(new JMenuItem(openAction));
        }
        if (drillDownAction != null) {
            menu.add(new JMenuItem(drillDownAction));
        }

        return menu;
    }

    // ==================== MENÚ ZOOM (PORCENTAJES) ====================

    /**
     * Muestra el menú de selección de porcentaje de zoom.
     */
    public void mostrarMenuPorcentajes(Component invoker) {
        JPopupMenu menu = new JPopupMenu();
        int[] porcentajes = { 25, 50, 75, 100, 150, 200 };
        for (int p : porcentajes) {
            JMenuItem item = new JMenuItem(p + "%");
            item.addActionListener(e -> aplicarZoomPersonalizado(p));
            menu.add(item);
        }
        menu.addSeparator();
        JMenuItem otrosItem = new JMenuItem("Otro...");
        otrosItem.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(invoker, "Introduce el porcentaje:", "Zoom Personalizado",
                    JOptionPane.PLAIN_MESSAGE);
            if (input != null && !input.trim().isEmpty()) {
                try {
                    aplicarZoomPersonalizado(Double.parseDouble(input.replace('%', ' ').trim()));
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(invoker, "Porcentaje inválido.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        menu.add(otrosItem);
        menu.show(invoker, 0, -invoker.getHeight());
    }

    // ==================== MENÚ ZOOM (MODOS) ====================

    /**
     * Muestra el menú de selección de modo de zoom.
     */
    public void mostrarMenuModosZoom(Component invoker) {
        JPopupMenu menu = new JPopupMenu();
        ZoomModeEnum[] modosParaMenu = {
                ZoomModeEnum.FIT_TO_SCREEN,
                ZoomModeEnum.FIT_TO_WIDTH,
                ZoomModeEnum.FIT_TO_HEIGHT,
                ZoomModeEnum.DISPLAY_ORIGINAL,
                ZoomModeEnum.FILL,
                ZoomModeEnum.MAINTAIN_CURRENT_ZOOM,
                ZoomModeEnum.USER_SPECIFIED_PERCENTAGE
        };
        for (ZoomModeEnum modo : modosParaMenu) {
            Action accionAsociada = actionMap.get(modo.getAssociatedActionCommand());
            if (accionAsociada != null) {
                JMenuItem item = new JMenuItem(accionAsociada);
                item.setText(modo.getNombreLegible());
                item.setIcon((Icon) accionAsociada.getValue(Action.SMALL_ICON));
                menu.add(item);
            }
        }
        menu.show(invoker, 0, -menu.getPreferredSize().height);
    }

    // ==================== HELPERS ====================

    private void aplicarZoomPersonalizado(double porcentaje) {
        if (generalController != null && generalController.getVisorController() != null) {
            generalController.getVisorController().getZoomManager().solicitarZoomPersonalizado(porcentaje);
        } else {
            logger.error("ERROR: No se puede delegar zoom — dependencias nulas.");
        }
    }

    // ==================== GETTERS ====================

    public JPopupMenu getPopupMenuImagenPrincipal() { return popupMenuImagenPrincipal; }
    public JPopupMenu getPopupMenuListaNombres() { return popupMenuListaNombres; }
    public JPopupMenu getPopupMenuListaMiniaturas() { return popupMenuListaMiniaturas; }
    public JPopupMenu getPopupMenuGrid() { return popupMenuGrid; }

    // ==================== INNER CLASSES ====================

    /**
     * Listener genérico que muestra un JPopupMenu al hacer clic derecho.
     */
    private static class PopupListener extends MouseAdapter {
        private final JPopupMenu popupMenu;

        public PopupListener(JPopupMenu popupMenu) {
            this.popupMenu = popupMenu;
        }

        @Override
        public void mousePressed(MouseEvent e) { maybeShowPopup(e); }

        @Override
        public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    /**
     * Listener para campos de ruta que ofrece copiar y filtrar.
     */
    private class PathPopupListener extends MouseAdapter {
        private final JTextField textField;
        private final boolean allowFilter;

        public PathPopupListener(JTextField textField, boolean allowFilter) {
            this.textField = textField;
            this.allowFilter = allowFilter;
        }

        @Override
        public void mousePressed(MouseEvent e) { maybeShow(e); }
        @Override
        public void mouseReleased(MouseEvent e) { maybeShow(e); }

        private void maybeShow(MouseEvent e) {
            if (e.isPopupTrigger()) {
                JPopupMenu menu = new JPopupMenu();

                String textoRaw = textField.getToolTipText();
                if (textoRaw == null || textoRaw.isEmpty()) {
                    textoRaw = textField.getText();
                }
                final String rutaLimpia = textoRaw.replaceFirst("^(Carpeta Destino: |Carpeta: |Ruta: )", "").trim();

                JMenuItem copiarItem = new JMenuItem("Copiar ruta");
                copiarItem.addActionListener(al -> {
                    java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(rutaLimpia);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                });
                menu.add(copiarItem);

                if (allowFilter) {
                    menu.addSeparator();

                    JMenuItem addPosItem = new JMenuItem("Añadir a filtros (+)");
                    addPosItem.addActionListener(al -> {
                        if (generalController != null) {
                            generalController.solicitarAnadirFiltroSilencioso(rutaLimpia,
                                FilterCriterion.FilterSource.FOLDER_PATH,
                                FilterCriterion.FilterType.CONTAINS);
                        }
                    });
                    menu.add(addPosItem);

                    JMenuItem addNegItem = new JMenuItem("Añadir a filtros (-)");
                    addNegItem.addActionListener(al -> {
                        if (generalController != null) {
                            generalController.solicitarAnadirFiltroSilencioso(rutaLimpia,
                                FilterCriterion.FilterSource.FOLDER_PATH,
                                FilterCriterion.FilterType.DOES_NOT_CONTAIN);
                        }
                    });
                    menu.add(addNegItem);
                }

                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
}
