package controlador.ui;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ProjectViewState;
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import vista.panels.GridDisplayPanel;
import vista.panels.export.ExportPanel;
import vista.theme.Tema;

public class ProjectUIManager {

    private static final Logger logger = LoggerFactory.getLogger(ProjectUIManager.class);

    private ComponentRegistry registry;

    public ProjectUIManager(ComponentRegistry registry) {
        this.registry = registry;
    }

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

        // Paso 3: Acción global: Añadir archivos (SIEMPRE disponible al final)
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


    private JTable getTablaExportacionDesdeRegistro() {
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

} // --- FIN de la clase ProjectUIManager ---
