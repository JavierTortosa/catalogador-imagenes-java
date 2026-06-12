package controlador.ui;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ProjectViewState;
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import controlador.managers.DataManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import vista.panels.GridDisplayPanel;
import vista.panels.export.ExportPanel;
import vista.panels.export.ProjectMetadataPanel;
import vista.theme.Tema;

/**
 * Gestiona la construccion y actualizacion de componentes de UI especificos
 * del modo PROYECTO: menus contextuales, listas de seleccion/descartes,
 * paneles de propiedades y layout del panel derecho.
 */
public class ProjectUIManager {

    private static final Logger logger = LoggerFactory.getLogger(ProjectUIManager.class);

    private ComponentRegistry registry;
    private DataManager dataManager;

    // Constructor: recibe el registro de componentes para acceder a las vistas
    public ProjectUIManager(ComponentRegistry registry) {
        this.registry = registry;
    } // --- Fin del metodo ProjectUIManager (constructor) ---


    // Inyecta el DataManager para permitir operaciones de asignacion de etiquetas
    public void setDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    } // --- Fin del metodo setDataManager ---

    // Calcula y ajusta la posición del divisor del split pane derecho (vertical)
    public void ajustarPosicionDivisorDerecho() {
        if (registry == null)
            return;

        JSplitPane rightSplit = registry.get("splitpane.proyecto.right");
        GridDisplayPanel gridPanel = registry.get("panel.display.grid.proyecto");

        if (rightSplit != null && gridPanel != null && rightSplit.isVisible()) {
            SwingUtilities.invokeLater(() -> {
                int cellHeight = gridPanel.getGridList().getFixedCellHeight();
                if (cellHeight <= 0)
                    cellHeight = 132;

                int desiredHeight = (int) (cellHeight * 1.5) + 15;

                // Asegurarnos de no poner el divisor en una posición inválida
                int maxLocation = rightSplit.getHeight() - rightSplit.getDividerSize() - 50;
                desiredHeight = Math.min(desiredHeight, maxLocation);

                rightSplit.setDividerLocation(desiredHeight);
                logger.debug("Posición del divisor derecho ajustada a {}px.", desiredHeight);
            });
        }
    } // --- Fin del metodo: ajustarPosicionDivisorDerecho ---


    // Actualiza los colores de fondo y texto de las listas de Selección y Descartes
    public void actualizarAparienciaListasPorFoco(String nombreListaActiva, Tema tema) {
        if (registry == null)
            return;
        JList<?> projectList = registry.get("list.proyecto.nombres");
        JList<?> descartesList = registry.get("list.proyecto.descartes");
        if (projectList == null || descartesList == null)
            return;
        Color colorFondoActivo = tema.colorFondoSecundario();
        Color colorFondoInactivo = tema.colorBorde();
        Color colorTextoActivo = tema.colorTextoPrimario();
        Color colorTextoInactivo = tema.colorTextoSecundario().brighter();
        if ("seleccion".equals(nombreListaActiva)) {
            projectList.setBackground(colorFondoActivo);
            projectList.setForeground(colorTextoActivo);
            descartesList.setBackground(colorFondoInactivo);
            descartesList.setForeground(colorTextoInactivo);
        } else {
            projectList.setBackground(colorFondoInactivo);
            projectList.setForeground(colorTextoInactivo);
            descartesList.setBackground(colorFondoActivo);
            descartesList.setForeground(colorTextoActivo);
        }

        projectList.repaint();
        descartesList.repaint();
    } // --- Fin del metodo: actualizarAparienciaListasPorFoco ---


    // Actualiza los títulos de los paneles "Selección Actual" y "Descartes"
    public void actualizarContadoresDeTitulos(Tema tema) {
        if (registry == null)
            return;

        JPanel panelSeleccion = registry.get("panel.proyecto.seleccion.container");
        JPanel panelDescartes = registry.get("panel.proyecto.descartes.container");
        Color titleColor = tema.colorBordeTitulo();

        // Actualizar título para "Selección Actual"
        if (panelSeleccion != null && panelSeleccion.getBorder() instanceof TitledBorder) {
            TitledBorder border = (TitledBorder) panelSeleccion.getBorder();
            JList<?> list = registry.get("list.proyecto.nombres");
            int count = (list != null && list.getModel() != null) ? list.getModel().getSize() : 0;
            border.setTitle("Selección Actual: " + count);
            border.setTitleColor(titleColor);
            panelSeleccion.repaint();
        }

        // Actualizar título para "Descartes"
        if (panelDescartes != null && panelDescartes.getBorder() instanceof TitledBorder) {
            TitledBorder border = (TitledBorder) panelDescartes.getBorder();
            JList<?> list = registry.get("list.proyecto.descartes");
            int count = (list != null && list.getModel() != null) ? list.getModel().getSize() : 0;
            border.setTitle("Descartes: " + count);
            border.setTitleColor(titleColor);
            panelDescartes.repaint();
        }
        logger.debug("[ProjectUIManager] Contadores de títulos de paneles actualizados.");
    } // --- Fin del metodo: actualizarContadoresDeTitulos ---


    // Construye y devuelve un JPopupMenu dinámico para el visor principal (Single o
    public JPopupMenu crearMenuContextualVisorManualmente(ProjectViewState viewState, VisorModel model,
            Map<String, Action> actionMap) {
        logger.debug("[MenuContextualVisor] Creando menú manualmente para el estado de vista: {} y modo display: {}",
                viewState, model.getCurrentDisplayMode());

        JPopupMenu menu = new JPopupMenu();
        boolean isGridMode = (model.getCurrentDisplayMode() == VisorModel.DisplayMode.GRID);
        boolean hasSelection = model.getSelectedImageKey() != null && !model.getSelectedImageKey().isEmpty();

        // Paso 1: Añadir acciones basadas en el contexto de la selección (Mover, Localizar, etc.)
        if (hasSelection) {
            switch (viewState) {
                case VIEW_SELECTION:
                case VIEW_EXPORT:
                    menu.add(actionMap.get(AppActionCommands.CMD_PROYECTO_MOVER_A_DESCARTES));
                    menu.add(actionMap.get(AppActionCommands.CMD_PROYECTO_LOCALIZAR_ARCHIVO));
                    break;
                case VIEW_DISCARDS:
                    menu.add(actionMap.get(AppActionCommands.CMD_PROYECTO_RESTAURAR_DE_DESCARTES));
                    menu.add(actionMap.get(AppActionCommands.CMD_PROYECTO_LOCALIZAR_ARCHIVO));
                    menu.addSeparator();
                    menu.add(actionMap.get(AppActionCommands.CMD_PROYECTO_ELIMINAR_PERMANENTEMENTE));
                    break;
            }
            menu.addSeparator();
        }

        // Paso 2: Añadir acciones de Paneo/Zoom (solo si no es Grid)
        Action toggleZoomAction = actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
        if (toggleZoomAction != null) {
            JCheckBoxMenuItem toggleZoomItem = new JCheckBoxMenuItem(toggleZoomAction);
            toggleZoomItem.setSelected(model.isZoomHabilitado());
            toggleZoomItem.setEnabled(!isGridMode);
            menu.add(toggleZoomItem);
        }

        Action resetZoomAction = actionMap.get(AppActionCommands.CMD_ZOOM_RESET);
        if (resetZoomAction != null) {
            JMenuItem resetZoomItem = new JMenuItem(resetZoomAction);
            resetZoomItem.setEnabled(!isGridMode);
            menu.add(resetZoomItem);
        }

        menu.addSeparator();

        // Paso 3: Asignar Etiqueta (solo si hay DataManager disponible)
        if (dataManager != null) {
            JMenuItem assignTagItem = new JMenuItem("Asignar Etiqueta...");
            assignTagItem.addActionListener(e -> {
                JList<String> gridList = registry.get("list.grid.proyecto");
                if (gridList == null) return;
                List<String> selectedKeys = gridList.getSelectedValuesList();
                List<Path> paths = new ArrayList<>();
                Map<String, Path> rutaMap = model.getCurrentListContext().getRutaCompletaMap();
                if (rutaMap != null) {
                    for (String key : selectedKeys) {
                        Path p = rutaMap.get(key);
                        if (p != null) paths.add(p);
                    }
                }
                if (!paths.isEmpty()) {
                    Frame frame = gridList != null ? (Frame) SwingUtilities.getWindowAncestor(gridList) : null;
                    new vista.dialogos.TagAssignmentDialog(frame, dataManager, paths).setVisible(true);
                }
            });
            menu.add(assignTagItem);
            menu.addSeparator();
        }

        // Paso 4: Acción global: Añadir archivos (SIEMPRE disponible al final)
        menu.add(actionMap.get(AppActionCommands.CMD_PROYECTO_ANADIR_ARCHIVOS));

        return menu;
    } // --- Fin del metodo: crearMenuContextualVisorManualmente ---


    // Configura el menú contextual para la tabla de exportación
    public void configurarContextMenuTablaExportacion(Map<String, Action> actionMap, VisorModel model) {
        logger.debug("[DIAGNÓSTICO] Se ha llamado a configurarContextMenuTablaExportacion().");

        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
        if (tablaExportacion == null || actionMap == null) {
            logger.error("[DIAGNÓSTICO] No se puede configurar menú: tablaExportacion es {} y actionMap es {}.",
                    (tablaExportacion == null ? "NULL" : "OK"),
                    (actionMap == null ? "NULL" : "OK"));
            return;
        }

        // Limpiamos listeners antiguos para evitar duplicados
        for (java.awt.event.MouseListener ml : tablaExportacion.getMouseListeners()) {
            if (ml.getClass().getName().contains("ContextMenuListener")) {
                tablaExportacion.removeMouseListener(ml);
            }
        }

        // Obtenemos las acciones que queremos en el menú
        Action quitarAction = actionMap.get(AppActionCommands.CMD_EXPORT_QUITAR_DE_COLA);
        Action asignarAction = actionMap.get(AppActionCommands.CMD_EXPORT_ASIGNAR_ARCHIVO);
        Action ignorarAction = actionMap.get(AppActionCommands.CMD_EXPORT_IGNORAR_COMPRIMIDO);
        Action relocalizarAction = actionMap.get(AppActionCommands.CMD_EXPORT_RELOCALIZAR_IMAGEN);
        Action limpiarHuerfanosAction = actionMap.get(AppActionCommands.CMD_EXPORT_LIMPIAR_NO_ENCONTRADOS);
        Action abrirUbicacionAction = actionMap.get(AppActionCommands.CMD_EXPORT_ABRIR_UBICACION);

        // Creamos el listener usando el método helper
        MouseAdapter contextMenuListener = createContextMenuListener(tablaExportacion, model,
                asignarAction,
                quitarAction,
                new JPopupMenu.Separator(),
                ignorarAction,
                relocalizarAction,
                limpiarHuerfanosAction,
                new JPopupMenu.Separator(),
                abrirUbicacionAction);

        // Asignamos el listener a la tabla
        tablaExportacion.addMouseListener(contextMenuListener);
        logger.debug("[DIAGNÓSTICO] Menú contextual configurado y listener añadido a la tabla de exportación.");
    } // --- Fin del metodo: configurarContextMenuTablaExportacion ---


    public JTable getTablaExportacionDesdeRegistro() {
        if (registry == null)
            return null;

        // Usamos la clave correcta con la que registramos el panel en ProjectBuilder.
        ExportPanel exportPanel = registry.get("panel.proyecto.exportacion.completo");

        if (exportPanel != null) {
            return exportPanel.getTablaExportacion();
        }

        logger.warn(
                "WARN [ProjectUIManager]: No se pudo encontrar 'ExportPanel' en el registro con la clave 'panel.proyecto.exportacion.completo'.");
        return null;
    } // --- Fin del metodo: getTablaExportacionDesdeRegistro ---


    // Crea un MouseListener que muestra un menú contextual en un JComponent
    private MouseAdapter createContextMenuListener(JComponent component, VisorModel model, Object... menuItems) {
        return new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    showMenu(e);
            }


            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    showMenu(e);
            }


            private void showMenu(MouseEvent e) {
                logger.debug("[ContextMenuListener] Evento de popup detectado en el componente: {}",
                        component.getClass().getSimpleName());

                if (component instanceof JTable) {
                    JTable table = (JTable) component;
                    int row = table.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        if (table.getSelectedRow() != row) {
                            table.setRowSelectionInterval(row, row);
                        }
                    } else {
                        logger.debug("[ContextMenuListener] Clic en área vacía de la tabla. No se mostrará el menú.");
                        return;
                    }
                }

                JPopupMenu menu = new JPopupMenu();
                for (Object item : menuItems) {
                    if (item instanceof Action) {
                        Action action = (Action) item;

                        // Si la acción es sensible al contexto, le pedimos que se actualice ahora
                        if (action instanceof ContextSensitiveAction) {
                            ((ContextSensitiveAction) action).updateEnabledState(model);
                        }
                        menu.add(action);
                    } else if (item instanceof JPopupMenu.Separator) {
                        menu.addSeparator();
                    }
                }

                if (menu.getComponentCount() > 0) {
                    logger.debug("[ContextMenuListener] Mostrando menú con {} componentes.", menu.getComponentCount());
                    menu.show(e.getComponent(), e.getX(), e.getY());
                } else {
                    logger.debug("[ContextMenuListener] El menú no tiene componentes, no se mostrará.");
                }
            }
        };
    } // --- Fin del metodo: createContextMenuListener ---

    // Rellena la JList de Selección con los elementos proporcionados
    public void poblarListaSeleccion(List<String> items) {
        JList<String> list = (registry != null) ? registry.get("list.proyecto.nombres") : null;
        if (list == null)
            return;
        DefaultListModel<String> model = new DefaultListModel<>();
        for (String item : items) {
            model.addElement(item);
        }
        list.setModel(model);
        logger.debug("[ProjectUIManager] Lista de selección poblada con {} elementos.", items.size());
    } // --- Fin del metodo: poblarListaSeleccion ---


    // Rellena la JList de Descartes y actualiza el título de la pestaña correspondiente
    public void poblarListaDescartes(List<String> items) {
        JList<String> list = (registry != null) ? registry.get("list.proyecto.descartes") : null;
        if (list == null)
            return;
        DefaultListModel<String> model = new DefaultListModel<>();
        for (String item : items) {
            model.addElement(item);
        }
        list.setModel(model);
        actualizarTituloTabDescartes(items.size());
        logger.debug("[ProjectUIManager] Lista de descartes poblada con {} elementos.", items.size());
    } // --- Fin del metodo: poblarListaDescartes ---


    // Actualiza el título de la pestaña de Descartes en el JTabbedPane de herramientas
    private void actualizarTituloTabDescartes(int count) {
        if (registry == null)
            return;
        JTabbedPane pane = registry.get("tabbedpane.proyecto.herramientas");
        if (pane != null) {
            for (int i = 0; i < pane.getTabCount(); i++) {
                String t = pane.getTitleAt(i);
                if ("Descartes".equals(t) || t.startsWith("Descartes:")) {
                    pane.setTitleAt(i, "Descartes: " + count);
                    break;
                }
            }
        }
    } // --- Fin del metodo: actualizarTituloTabDescartes ---


    // Limpia los modelos de las JLists de Selección y Descartes y resetea el título de la pestaña
    public void limpiarListasProyecto() {
        if (registry == null)
            return;
        JList<String> listaSeleccion = registry.get("list.proyecto.nombres");
        if (listaSeleccion != null) {
            if (listaSeleccion.getModel() instanceof DefaultListModel) {
                ((DefaultListModel<String>) listaSeleccion.getModel()).clear();
            } else {
                listaSeleccion.setModel(new DefaultListModel<>());
            }
        }
        JList<String> listaDescartes = registry.get("list.proyecto.descartes");
        if (listaDescartes != null) {
            if (listaDescartes.getModel() instanceof DefaultListModel) {
                ((DefaultListModel<String>) listaDescartes.getModel()).clear();
            } else {
                listaDescartes.setModel(new DefaultListModel<>());
            }
        }
        actualizarTituloTabDescartes(0);
        logger.debug("[ProjectUIManager] Listas del proyecto limpiadas.");
    } // --- Fin del metodo: limpiarListasProyecto ---


    // Resetea el layout del panel derecho: oculta herramientas, reposiciona divisor y resetea botón toggle
    public void resetLayoutProyecto(Map<String, Action> actionMap) {
        if (registry == null)
            return;
        JSplitPane rightSplit = registry.get("splitpane.proyecto.right");
        JPanel toolsPanel = registry.get("panel.proyecto.herramientas.container");
        if (rightSplit != null && toolsPanel != null) {
            toolsPanel.setVisible(false);
            rightSplit.setDividerLocation(1.0);
            rightSplit.setDividerSize(0);
        }
        if (actionMap != null) {
            Action toggleAction = actionMap.get(AppActionCommands.CMD_EXPORT_ASSIGN_PANNEL);
            if (toggleAction != null) {
                toggleAction.putValue(Action.SELECTED_KEY, false);
            }
        }
        logger.debug("[ProjectUIManager] Layout del panel derecho reseteado.");
    } // --- Fin del metodo: resetLayoutProyecto ---


    // Selecciona y asegura visibilidad del índice indicado en el grid del proyecto
    public void sincronizarSeleccionGrid(int indiceSeleccionado) {
        if (registry == null)
            return;
        JList<String> gridList = registry.get("list.grid.proyecto");
        if (gridList == null)
            return;
        SwingUtilities.invokeLater(() -> {
            if (indiceSeleccionado >= 0 && indiceSeleccionado < gridList.getModel().getSize()) {
                if (gridList.getSelectedIndex() != indiceSeleccionado) {
                    gridList.setSelectedIndex(indiceSeleccionado);
                }
                gridList.ensureIndexIsVisible(indiceSeleccionado);
            } else {
                gridList.clearSelection();
            }
        });
    } // --- Fin del metodo: sincronizarSeleccionGrid ---


    // Actualiza los campos de nombre y descripción en el panel de propiedades del proyecto
    public void actualizarPanelPropiedades(String name, String description) {
        if (registry == null)
            return;
        ProjectMetadataPanel propsPanel = registry.get("panel.proyecto.propiedades");
        if (propsPanel != null) {
            propsPanel.getProjectNameLabel().setText(name);
            propsPanel.getProjectDescriptionArea().setText(description);
            logger.debug("[ProjectUIManager] Panel de propiedades actualizado.");
        }
    } // --- Fin del metodo: actualizarPanelPropiedades ---


} // --- FIN de la clase ProjectUIManager ---
