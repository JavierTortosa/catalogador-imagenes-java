package vista.builders;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.GeneralController;
import controlador.ProjectController;
import controlador.commands.AppActionCommands;
import controlador.managers.ToolbarManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import vista.panels.GridDisplayPanel;
import vista.panels.ImageDisplayPanel;
import vista.panels.export.ExportPanel;
import vista.panels.export.ProjectMetadataPanel;
import vista.renderers.ProjectListCellRenderer;
import vista.theme.Tema;
import vista.theme.ThemeChangeListener;
import vista.theme.ThemeManager;
import vista.util.ThumbnailPreviewer;

public class ProjectBuilder implements ThemeChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(ProjectBuilder.class);

    private final ComponentRegistry registry;
    private final VisorModel model;
    private final ThemeManager themeManager;
    private final GeneralController generalController;
    private final ToolbarManager toolbarManager;
    private final ProjectController projectController;

    public ProjectBuilder(ComponentRegistry registry, VisorModel model, ThemeManager themeManager,
            GeneralController generalController, ToolbarManager toolbarManager, ProjectController projectController) {
    	
    	logger.info("[ProjectBuilder] Iniciando...");
    	
        this.registry = Objects.requireNonNull(registry, "Registry no puede ser null en ProjectBuilder");
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en ProjectBuilder");
        this.themeManager = Objects.requireNonNull(themeManager, "ThemeManager no puede ser null en ProjectBuilder");
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null");
        this.toolbarManager = Objects.requireNonNull(toolbarManager, "ToolbarManager no puede ser null");
        this.projectController = Objects.requireNonNull(projectController, "ProjectController no puede ser null");
        this.themeManager.addThemeChangeListener(this);
    } // --- Fin del método ProjectBuilder (constructor) ---

    
    public JPanel buildProjectViewPanel() {
        logger.info("  [ProjectBuilder] Construyendo el panel del modo proyecto (Dashboard)...");

        JPanel panelProyectoRaiz = new JPanel(new BorderLayout());
        registry.register("view.panel.proyectos", panelProyectoRaiz);

        JSplitPane leftSplitPanel = createLeftPanel();
        JSplitPane rightSplitPanel = createRightPanel();
        
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplitPanel, rightSplitPanel);
        mainSplit.setResizeWeight(0.25);
        mainSplit.setContinuousLayout(true);
        mainSplit.setBorder(null);
        mainSplit.setDividerLocation(0.25);
        registry.register("splitpane.proyecto.main", mainSplit);
        
        panelProyectoRaiz.add(mainSplit, BorderLayout.CENTER);
        
        logger.info("  [ProjectBuilder] Panel del modo proyecto (Dashboard) construido y ensamblado.");
        return panelProyectoRaiz;
    } // --- FIN de metodo buildProjectViewPanel ---
    
    private JSplitPane createLeftPanel() {
        JPanel panelSeleccion = createSelectionPanel();
        JPanel panelDescartes = createDiscardsPanel();
        
        
        // 1. Le damos un tamaño mínimo al panel de descartes.
        //    Esto le dice al SplitPane: "Por muy poco espacio que haya,
        //    nunca me hagas más pequeño que esto". 100 píxeles suele ser suficiente.
        panelDescartes.setMinimumSize(new java.awt.Dimension(100, 100));
        
        // 2. Le damos también un tamaño mínimo al panel de selección, para equilibrio.
        panelSeleccion.setMinimumSize(new java.awt.Dimension(100, 150));
        
        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelSeleccion, panelDescartes);
        leftSplit.setContinuousLayout(true);
        leftSplit.setBorder(null);

        leftSplit.setResizeWeight(0.85);
        leftSplit.setDividerLocation(0.85);
        
        registry.register("splitpane.proyecto.left", leftSplit);
        
        return leftSplit;
    } // --- FIN de metodo createLeftPanel ---

    
    private JSplitPane createRightPanel() { // <-- CAMBIO: Volvemos a devolver JSplitPane
        
        // --- 1. Crear el panel de VISUALIZACIÓN con su propio marco ---
        
        // Obtenemos el CardLayout que contiene el visor de imagen y el grid.
        JPanel displayModesContainer = createDisplayModesContainer();
        
        // ¡LA CLAVE! Envolvemos SÓLO este panel en un contenedor con el borde "Visor".
        JPanel visorConBordePanel = new JPanel(new BorderLayout());
        TitledBorder visorBorder = BorderFactory.createTitledBorder("Visor");
        visorConBordePanel.setBorder(visorBorder);
        visorConBordePanel.add(displayModesContainer, BorderLayout.CENTER);
        
        // Lo registramos para que el ThemeManager pueda actualizar el color del borde.
        registry.register("panel.proyecto.visor.container", visorConBordePanel);

        
        // --- 2. Crear el panel de HERRAMIENTAS (Exportar/Etiquetar), que ya tiene sus propios marcos ---
        JPanel toolsPanel = createRightToolsPanel();
        toolsPanel.setVisible(false); // Lo ocultamos por defecto
        
        
        // --- 3. Ensamblar ambos en el JSplitPane vertical ---
        
        // AHORA, el componente superior del SplitPane es nuestro nuevo panel con borde.
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, visorConBordePanel, toolsPanel);
        
        rightSplit.setResizeWeight(0.7); 
        rightSplit.setContinuousLayout(true);
        rightSplit.setBorder(null);
        rightSplit.setDividerLocation(1.0);
        rightSplit.setDividerSize(0);
        registry.register("splitpane.proyecto.right", rightSplit);

        return rightSplit; // Devolvemos el JSplitPane completo
    } // --- FIN de metodo createRightPanel ---
    

    private JPanel createSelectionPanel() {
        JPanel panelSeleccion = new JPanel(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder("Selección Actual: 0");
        
        panelSeleccion.setBorder(border);
        registry.register("panel.proyecto.seleccion.container", panelSeleccion);
        
        JList<String> projectFileList = new JList<>();
        projectFileList.setName("list.proyecto.nombres");
        projectFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // --- INICIO DE LA MODIFICACIÓN ---
        projectFileList.setCellRenderer(new ProjectListCellRenderer());
        // --- FIN DE LA MODIFICACIÓN ---
        registry.register("list.proyecto.nombres", projectFileList, "WHEEL_NAVIGABLE");	

        Action moveToDiscardsAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_MOVER_A_DESCARTES);
        Action localizarAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_LOCALIZAR_ARCHIVO);
        if (moveToDiscardsAction != null) {
            projectFileList.addMouseListener(createContextMenuListener(projectFileList, 
                moveToDiscardsAction, new JPopupMenu.Separator(), localizarAction));
        }

        JScrollPane scrollPane = new JScrollPane(projectFileList);
        scrollPane.setBorder(null);
        registry.register("scroll.proyecto.nombres", scrollPane);
        panelSeleccion.add(scrollPane, BorderLayout.CENTER);
        
        return panelSeleccion;
    } // --- FIN de metodo createSelectionPanel ---

    private JPanel createDiscardsPanel() {
        JPanel panelDescartes = new JPanel(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder("Descartes: 0");
        panelDescartes.setBorder(border);
        registry.register("panel.proyecto.descartes.container", panelDescartes);

        JList<String> descartesList = new JList<>();
        descartesList.setName("list.proyecto.descartes");
        descartesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // --- INICIO DE LA MODIFICACIÓN ---
        descartesList.setCellRenderer(new ProjectListCellRenderer());
        // --- FIN DE LA MODIFICACIÓN ---
        registry.register("list.proyecto.descartes", descartesList, "WHEEL_NAVIGABLE");

        Action restoreAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_RESTAURAR_DE_DESCARTES);
        Action deleteAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_ELIMINAR_PERMANENTEMENTE);
        Action localizarAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_LOCALIZAR_ARCHIVO);
        Action vaciarAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_VACIAR_DESCARTES);
        
        if (restoreAction != null && deleteAction != null) {
            descartesList.addMouseListener(createContextMenuListener(descartesList, 
                restoreAction, new JPopupMenu.Separator(), localizarAction, 
                new JPopupMenu.Separator(), vaciarAction, new JPopupMenu.Separator(), deleteAction));
        }
        
        JScrollPane scrollPaneDescartes = new JScrollPane(descartesList);
        scrollPaneDescartes.setBorder(null);
        registry.register("scroll.proyecto.descartes", scrollPaneDescartes);
        panelDescartes.add(scrollPaneDescartes, BorderLayout.CENTER);

        return panelDescartes;
    } // --- FIN de metodo createDiscardsPanel ---
    
    
    
    private JPanel createDisplayModesContainer() {
        JPanel displayModesContainer = new JPanel(new CardLayout());
        registry.register("container.displaymodes.proyecto", displayModesContainer); 
        displayModesContainer.setMinimumSize(new java.awt.Dimension(200, 200));

        // --- Visor de Imagen Única ---
        ImageDisplayPanel singleImageViewPanel = new ImageDisplayPanel(this.themeManager, this.model);
        registry.register("panel.proyecto.display", singleImageViewPanel);
        registry.register("label.proyecto.imagen", singleImageViewPanel.getInternalLabel(), "WHEEL_NAVIGABLE");
        singleImageViewPanel.setBorder(BorderFactory.createTitledBorder(""));

        // --- INICIO DE LA CORRECCIÓN DE FOCO PARA MODO PROYECTO ---
        singleImageViewPanel.setFocusable(true);

        java.awt.event.MouseAdapter focusRequester = new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                singleImageViewPanel.requestFocusInWindow();
            }
        };

        singleImageViewPanel.addMouseListener(focusRequester);
        singleImageViewPanel.getInternalLabel().addMouseListener(focusRequester);
        // --- FIN DE LA CORRECCIÓN DE FOCO ---
        
        // Creamos el listener una sola vez para reutilizarlo
        java.awt.event.MouseAdapter sharedContextMenuListener = new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) { if (e.isPopupTrigger()) showProjectContextMenu(e); }
            public void mouseReleased(java.awt.event.MouseEvent e) { if (e.isPopupTrigger()) showProjectContextMenu(e); }

            private void showProjectContextMenu(java.awt.event.MouseEvent e) {
                // Si el clic es en una JList, seleccionamos el item bajo el cursor
                if (e.getComponent() instanceof JList) {
                    JList<?> list = (JList<?>) e.getComponent();
                    int row = list.locationToIndex(e.getPoint());
                    if (row != -1 && list.getSelectedIndex() != row) {
                        list.setSelectedIndex(row);
                    }
                }

                if (projectController != null) {
                    JPopupMenu menu = projectController.crearMenuContextualVisorManualmente();
                    if (menu != null && menu.getComponentCount() > 0) {
                        menu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        };
        
        singleImageViewPanel.getInternalLabel().addMouseListener(sharedContextMenuListener);

        // --- Visor de Grid ---
        ThumbnailPreviewer projectGridPreviewer = new ThumbnailPreviewer(null, this.model, this.themeManager, null, this.registry);
        GridDisplayPanel gridViewPanel = new GridDisplayPanel(this.model, generalController.getVisorController().getServicioMiniaturas(), this.themeManager, generalController.getVisorController().getIconUtils(), projectGridPreviewer, projectController.getProjectManager(), this.projectController, this.registry);
        
        // --- INICIO DE LA MODIFICACIÓN: Componer toolbars para el grid de proyecto ---
        if (this.toolbarManager != null) {
            // 1. Obtenemos las dos toolbars que necesita este grid.
            JToolBar proyectoToolbar = this.toolbarManager.getToolbar("barra_grid_proyecto");
            JToolBar tamanoToolbar = this.toolbarManager.getToolbar("barra_grid_tamano");

            // 2. Las añadimos a una lista.
            java.util.List<JToolBar> toolbarsParaGrid = new java.util.ArrayList<>();
            if (proyectoToolbar != null) toolbarsParaGrid.add(proyectoToolbar);
            if (tamanoToolbar != null) toolbarsParaGrid.add(tamanoToolbar);

            // 3. Pasamos la lista completa al panel del grid usando el nuevo método.
            if (!toolbarsParaGrid.isEmpty()) {
                gridViewPanel.setToolbars(toolbarsParaGrid);
            }
        }
        // --- FIN DE LA MODIFICACIÓN ---
        
        
        registry.register("panel.display.grid.proyecto", gridViewPanel);
        JList<String> gridList = gridViewPanel.getGridList(); // Obtenemos la JList interna
        registry.register("list.grid.proyecto", gridList, "WHEEL_NAVIGABLE");

        // --- LA CLAVE: AÑADIMOS EL MISMO LISTENER AL GRID ---
        gridList.addMouseListener(sharedContextMenuListener);

        // Ensamblaje final
        displayModesContainer.add(singleImageViewPanel, "VISTA_SINGLE_IMAGE");
        displayModesContainer.add(gridViewPanel, "VISTA_GRID");

        return displayModesContainer;
    } // --- FIN de metodo createDisplayModesContainer ---
    
    
    private JPanel createRightToolsPanel() {
        JPanel panelHerramientas = new JPanel(new BorderLayout());
        registry.register("panel.proyecto.herramientas.container", panelHerramientas);
        panelHerramientas.setMinimumSize(new java.awt.Dimension(200, 200));
        
	    java.awt.Color borderColor = javax.swing.UIManager.getColor("Component.borderColor");
	    if (borderColor == null) borderColor = java.awt.Color.GRAY;
	    javax.swing.border.Border lineBorder = javax.swing.BorderFactory.createLineBorder(borderColor);
	    javax.swing.border.Border emptyBorder = javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2);
	    panelHerramientas.setBorder(javax.swing.BorderFactory.createCompoundBorder(emptyBorder, lineBorder));
	
        JTabbedPane herramientasTabbedPane = new JTabbedPane();
        registry.register("tabbedpane.proyecto.herramientas", herramientasTabbedPane);

        // --- Pestaña 1: Propiedades (NUEVA) ---
        ProjectMetadataPanel panelPropiedades = new ProjectMetadataPanel();
        registry.register("panel.proyecto.propiedades", panelPropiedades);

        // --- Pestaña 2: Exportar (EXISTENTE) ---
        ExportPanel panelExportar = new ExportPanel(this.projectController,
                (e) -> {
                	logger.info ("--- PASO 2: Callback en ProjectBuilder EJECUTADO ---");
                	
                    this.projectController.notificarCambioEnProyecto();
                    this.projectController.actualizarEstadoExportacionUI();
                });
        registry.register("panel.proyecto.exportacion.completo", panelExportar);
        registry.register("tabla.exportacion", panelExportar.getTablaExportacion(), "WHEEL_NAVIGABLE");
        
        if (panelExportar.getDetailPanel() != null) {
            Action addAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_EXPORT_ADD_ASSOCIATED_FILE);
            Action removeAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_EXPORT_DEL_ASSOCIATED_FILE);
            Action locateAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_EXPORT_LOCATE_ASSOCIATED_FILE);
            panelExportar.getDetailPanel().setActions(addAction, removeAction, locateAction);
            registry.register("panel.proyecto.exportacion.detalles", panelExportar.getDetailPanel());
        }
        
        herramientasTabbedPane.addTab("Exportar", panelExportar);
        herramientasTabbedPane.addTab("Propiedades", panelPropiedades);

        // --- Pestaña 3: Etiquetar (FUTURA) ---
        JPanel panelEtiquetar = new JPanel();
        panelEtiquetar.add(new JLabel("Funcionalidad de etiquetado en desarrollo."));
        herramientasTabbedPane.addTab("Etiquetar", panelEtiquetar);
        herramientasTabbedPane.setEnabledAt(2, false); // Ahora es el índice 2

        panelHerramientas.add(herramientasTabbedPane, BorderLayout.CENTER);
        return panelHerramientas;
    } // --- FIN de metodo createRightToolsPanel ---
    
    
    @Override
    public void onThemeChanged(Tema nuevoTema) {
        SwingUtilities.invokeLater(() -> {
            actualizarBordeConTema("panel.proyecto.seleccion.container", "Selección Actual", nuevoTema);
            actualizarBordeConTema("panel.proyecto.descartes.container", "Descartes", nuevoTema);
            JList<?> listaNombres = registry.get("list.proyecto.nombres");
            if(listaNombres != null) listaNombres.repaint();
            JList<?> listaDescartes = registry.get("list.proyecto.descartes");
            if(listaDescartes != null) listaDescartes.repaint();
            JTabbedPane rightTabs = registry.get("tabbedpane.proyecto.herramientas");
            if (rightTabs != null) SwingUtilities.updateComponentTreeUI(rightTabs);
        });
    } // --- FIN de metodo onThemeChanged ---
    
    private void actualizarBordeConTema(String panelKey, String tituloBase, Tema tema) {
        JPanel panel = registry.get(panelKey);
        if (panel != null && panel.getBorder() instanceof TitledBorder) {
            TitledBorder border = (TitledBorder) panel.getBorder();
            int count = 0;
            if ("panel.proyecto.seleccion.container".equals(panelKey)) {
                JList<?> list = registry.get("list.proyecto.nombres");
                if (list != null && list.getModel() != null) count = list.getModel().getSize();
            } else if ("panel.proyecto.descartes.container".equals(panelKey)) {
                JList<?> list = registry.get("list.proyecto.descartes");
                if (list != null && list.getModel() != null) count = list.getModel().getSize();
            }
            border.setTitle(tituloBase + ": " + count);
            border.setTitleColor(tema.colorBordeTitulo());
            panel.repaint();
        }
    } // --- FIN de metodo actualizarBordeConTema ---

    private java.awt.event.MouseAdapter createContextMenuListener(JComponent component, Object... menuItems) {
        return new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
            public void mouseReleased(java.awt.event.MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }

            private void showMenu(java.awt.event.MouseEvent e) {
                if (component instanceof JList) {
                    JList<?> list = (JList<?>) component;
                    int row = list.locationToIndex(e.getPoint());
                    if (row != -1) list.setSelectedIndex(row);
                } else if (component instanceof JTable) {
                    JTable table = (JTable) component;
                    int row = table.rowAtPoint(e.getPoint());
                    if (row != -1) table.setRowSelectionInterval(row, row);
                }
                JPopupMenu menu = new JPopupMenu();
                for (Object item : menuItems) {
                    if (item instanceof Action) menu.add((Action) item);
                    else if (item instanceof JPopupMenu.Separator) menu.addSeparator();
                }
                if (menu.getComponentCount() > 0) menu.show(e.getComponent(), e.getX(), e.getY());
            }
        };
    } // --- FIN de metodo createContextMenuListener ---
    
} // --- FIN DE LA CLASE ProjectBuilder ---


