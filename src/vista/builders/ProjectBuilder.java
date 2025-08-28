package vista.builders;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
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
import controlador.managers.DisplayModeManager;
import controlador.managers.ToolbarManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import vista.panels.GridDisplayPanel;
import vista.panels.ImageDisplayPanel;
import vista.renderers.NombreArchivoRenderer;
import vista.theme.Tema;
import vista.theme.ThemeChangeListener;
import vista.theme.ThemeManager;
import vista.util.ThumbnailPreviewer; 

public class ProjectBuilder implements ThemeChangeListener{

	private static final Logger logger = LoggerFactory.getLogger(ProjectBuilder.class);
	
    private final ComponentRegistry registry;
    private final VisorModel model;
    private final ThemeManager themeManager;
    private final GeneralController generalController;
    private final ToolbarManager toolbarManager;
    private final ProjectController projectController;
    
    public ProjectBuilder(ComponentRegistry registry, VisorModel model, ThemeManager themeManager, GeneralController generalController, ToolbarManager toolbarManager, ProjectController projectController) {
        this.registry = Objects.requireNonNull(registry, "Registry no puede ser null en ProjectBuilder");
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en ProjectBuilder");
        this.themeManager = Objects.requireNonNull(themeManager, "ThemeManager no puede ser null en ProjectBuilder");
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null");
        this.toolbarManager = Objects.requireNonNull(toolbarManager, "ToolbarManager no puede ser null");
        this.projectController = Objects.requireNonNull(projectController, "ProjectController no puede ser null");
    } // --- Fin del método ProjectBuilder (constructor) ---

    
    public JPanel buildProjectViewPanel() {
        logger.info("  [ProjectBuilder] Construyendo el panel del modo proyecto (Dashboard)...");

        JPanel panelProyectoRaiz = new JPanel(new BorderLayout());
        registry.register("view.panel.proyectos", panelProyectoRaiz);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setResizeWeight(0.25);
        mainSplit.setContinuousLayout(true);
        mainSplit.setBorder(null);
        registry.register("splitpane.proyecto.main", mainSplit);

        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftSplit.setResizeWeight(0.6);
        leftSplit.setContinuousLayout(true);
        leftSplit.setBorder(null);
        registry.register("splitpane.proyecto.left", leftSplit);

        leftSplit.setMinimumSize(new java.awt.Dimension(0, 0));
        
        JPanel panelListas = createProjectListsPanel();
        JPanel panelHerramientas = createProjectToolsPanel();
        
        JPanel displayModesContainer = new JPanel(new CardLayout());
        registry.register("container.displaymodes.proyecto", displayModesContainer); 

        ImageDisplayPanel singleImageViewPanel = new ImageDisplayPanel(this.themeManager, this.model);
        registry.register("panel.proyecto.display", singleImageViewPanel);
        registry.register("label.proyecto.imagen", singleImageViewPanel.getInternalLabel(), "WHEEL_NAVIGABLE");
        
        TitledBorder border = BorderFactory.createTitledBorder("");
        border.setTitleColor(themeManager.getTemaActual().colorBordeTitulo());
        singleImageViewPanel.setBorder(border);
        
	     singleImageViewPanel.getInternalLabel().addMouseListener(new java.awt.event.MouseAdapter() {
	         public void mousePressed(java.awt.event.MouseEvent e) { if (e.isPopupTrigger()) { showProjectSingleImageMenu(e); } }
	         public void mouseReleased(java.awt.event.MouseEvent e) { if (e.isPopupTrigger()) { showProjectSingleImageMenu(e); } }
	
	         private void showProjectSingleImageMenu(java.awt.event.MouseEvent e) {
	             String currentImageKey = model.getSelectedImageKey();
	             if (currentImageKey == null || currentImageKey.isEmpty()) {
	                 return; 
	             }
	
	             java.nio.file.Path imagePath = model.getRutaCompleta(currentImageKey);
	             
	             controlador.managers.interfaces.IProjectManager projectManager = generalController.getProjectController().getProjectManager();
	
	             if (imagePath == null || projectManager == null) {
	                 return;
	             }
	
	             boolean isEnDescartes = projectManager.estaEnDescartes(imagePath);
	             
	             String listaActiva = model.getProyectoListContext().getNombreListaActiva();
	             
	             JPopupMenu menu = new JPopupMenu();
	
	             Action moveToDiscardsAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_MOVER_A_DESCARTES);
	             Action restoreFromDiscardsAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_RESTAURAR_DE_DESCARTES);
	             Action deleteFromProjectAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_ELIMINAR_PERMANENTEMENTE);
	             Action localizarAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_LOCALIZAR_ARCHIVO);
	             Action vaciarDescartesAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_VACIAR_DESCARTES);
	             
	             if ("seleccion".equals(listaActiva)) {
	                 if (moveToDiscardsAction != null) menu.add(moveToDiscardsAction);
	                 menu.addSeparator(); 
	                 if (localizarAction != null) menu.add(localizarAction);

	             } else if ("descartes".equals(listaActiva)) {
	                 if (restoreFromDiscardsAction != null) menu.add(restoreFromDiscardsAction);
	                 menu.addSeparator(); 
	                 if (localizarAction != null) menu.add(localizarAction); 
	                 menu.addSeparator(); 
	                 if (vaciarDescartesAction != null) menu.add(vaciarDescartesAction);
	                 menu.addSeparator(); 
	                 if (deleteFromProjectAction != null) menu.add(deleteFromProjectAction);
	             }
	             
	             if (menu.getComponentCount() > 0) {
	                 menu.show(e.getComponent(), e.getX(), e.getY());
	             }
	         }
	     });
	
        ThumbnailPreviewer projectGridPreviewer = new ThumbnailPreviewer(
            null, 
            this.model, 
            this.themeManager, 
            null, 
            this.registry
        );

        GridDisplayPanel gridViewPanel = new GridDisplayPanel(
            this.model, 
            generalController.getVisorController().getServicioMiniaturas(), 
            this.themeManager, 
            generalController.getVisorController().getIconUtils(),
            projectGridPreviewer,
            generalController.getProjectController().getProjectManager()
            ,this.projectController
        );
        
        
        if (this.toolbarManager != null) {
            logger.debug("[ProjectBuilder] Obteniendo 'barra_grid' desde ToolbarManager...");
            JToolBar gridToolbar = this.toolbarManager.getToolbar("barra_grid");
            
            if (gridToolbar != null) {
                logger.info(">>> ÉXITO: Toolbar 'barra_grid' obtenida. Inyectando en GridDisplayPanel. <<<");
                gridViewPanel.setToolbar(gridToolbar);
            } else {
                logger.error(">>> FALLO: ToolbarManager devolvió NULL para 'barra_grid'. La toolbar no se añadirá. <<<");
            }
        } else {
            logger.error("[ProjectBuilder] ToolbarManager es NULL. No se puede obtener la 'barra_grid'.");
        }
        
        ToolbarManager toolbarManager = generalController.getToolbarManager();
        if (toolbarManager != null) {
            JToolBar gridToolbar = toolbarManager.getToolbar("barra_grid");
            gridViewPanel.setToolbar(gridToolbar);
            logger.debug(" -> Toolbar 'barra_grid' inyectada en el GridDisplayPanel del proyecto.");
        }
        
        registry.register("panel.display.grid.proyecto", gridViewPanel);
        
        JList<String> projectGridList = gridViewPanel.getGridList();
        registry.register("list.grid.proyecto", gridViewPanel.getGridList(), "WHEEL_NAVIGABLE");

        projectGridList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) { 
                handlePopupAndSelection(e);
                if (e.isPopupTrigger()) { showProjectGridMenu(e); } 
            }
            public void mouseReleased(java.awt.event.MouseEvent e) { 
                handlePopupAndSelection(e);
                if (e.isPopupTrigger()) { showProjectGridMenu(e); } 
            }

            private void handlePopupAndSelection(java.awt.event.MouseEvent e) {
                int index = projectGridList.locationToIndex(e.getPoint());
                if (index != -1) {
                    if (projectGridList.getSelectedIndex() != index) {
                        projectGridList.setSelectedIndex(index);
                    }
                    
                    if (generalController != null && generalController.getProjectController() != null) {
                        controlador.ProjectListCoordinator coordinator = generalController.getProjectController().getProjectListCoordinator();
                        if (coordinator != null) {
                            coordinator.seleccionarImagenPorIndice(index);
                        }
                    }
                }
            }
            
            private void showProjectGridMenu(java.awt.event.MouseEvent e) {
                int index = projectGridList.locationToIndex(e.getPoint());
                if (index != -1) {
                    projectGridList.setSelectedIndex(index);
                }

                String listaActiva = model.getProyectoListContext().getNombreListaActiva();
                
                JPopupMenu menu = new JPopupMenu();

                Action moveToDiscardsAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_MOVER_A_DESCARTES);
                Action restoreFromDiscardsAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_RESTAURAR_DE_DESCARTES);
                Action deleteFromProjectAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_ELIMINAR_PERMANENTEMENTE);

                if ("seleccion".equals(listaActiva)) {
                    if (moveToDiscardsAction != null) {
                        menu.add(moveToDiscardsAction);
                    }
                } else if ("descartes".equals(listaActiva)) {
                    if (restoreFromDiscardsAction != null) {
                        menu.add(restoreFromDiscardsAction);
                    }
                    menu.addSeparator();
                    if (deleteFromProjectAction != null) {
                        menu.add(deleteFromProjectAction);
                    }
                }

                if (menu.getComponentCount() > 0) {
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        
        JPanel polaroidViewPanel = new JPanel();
        polaroidViewPanel.add(new JLabel("Vista POLAROID (Proyecto) en construcción..."));
        registry.register("panel.display.polaroid.proyecto", polaroidViewPanel);
        
        displayModesContainer.add(singleImageViewPanel, "VISTA_SINGLE_IMAGE");
        displayModesContainer.add(gridViewPanel, "VISTA_GRID");
        displayModesContainer.add(polaroidViewPanel, "VISTA_POLAROID");

        leftSplit.setTopComponent(panelListas);
        leftSplit.setBottomComponent(panelHerramientas);

        mainSplit.setLeftComponent(leftSplit);
        mainSplit.setRightComponent(displayModesContainer);

        panelProyectoRaiz.add(mainSplit, BorderLayout.CENTER);
        
        logger.info("  [ProjectBuilder] Panel del modo proyecto (Dashboard) construido y ensamblado.");
        
        return panelProyectoRaiz;
        
    } // --- Fin del método buildProjectViewPanel ---
    
    
    private JPanel createProjectListsPanel() {
        JPanel panelListas = new JPanel(new BorderLayout());
        
        TitledBorder border = BorderFactory.createTitledBorder("Selección Actual");
        border.setTitleColor(themeManager.getTemaActual().colorBordeTitulo());
        panelListas.setBorder(border);
        registry.register("panel.proyecto.listas.container", panelListas);
        
        
        JList<String> projectFileList = new JList<>();
        projectFileList.setName("list.proyecto.nombres");
        
        projectFileList.setBackground(themeManager.getTemaActual().colorFondoSecundario());
        
        projectFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectFileList.setCellRenderer(new NombreArchivoRenderer(themeManager, model));
        registry.register("list.proyecto.nombres", projectFileList, "WHEEL_NAVIGABLE");

        Action moveToDiscardsAction = this.generalController.getVisorController().getActionFactory().getActionMap().get(AppActionCommands.CMD_PROYECTO_MOVER_A_DESCARTES);
        Action localizarAction = this.generalController.getVisorController().getActionFactory().getActionMap().get(AppActionCommands.CMD_PROYECTO_LOCALIZAR_ARCHIVO);
        
        if (moveToDiscardsAction != null) {
            projectFileList.addMouseListener(createContextMenuListener(
            		projectFileList, 
            		moveToDiscardsAction, 
            		new javax.swing.JPopupMenu.Separator(), 
            		localizarAction));
        } else {
             logger.error("WARN [ProjectBuilder]: No se pudo encontrar la acción CMD_PROYECTO_MOVER_A_DESCARTES en el ActionFactory.");
        }

        JScrollPane scrollPane = new JScrollPane(projectFileList);
        scrollPane.setBorder(null);
        registry.register("scroll.proyecto.nombres", scrollPane);
        
        panelListas.add(scrollPane, BorderLayout.CENTER);
        
        return panelListas;
    } // --- Fin del método createProjectListsPanel ---
    
    
    private JPanel createProjectToolsPanel() {
        JPanel panelHerramientas = new JPanel(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder("Herramientas de Proyecto");
        border.setTitleColor(themeManager.getTemaActual().colorBordeTitulo());
        registry.register("panel.proyecto.herramientas.container", panelHerramientas);
        
        javax.swing.JTabbedPane herramientasTabbedPane = new javax.swing.JTabbedPane();
        registry.register("tabbedpane.proyecto.herramientas", herramientasTabbedPane);
        
        JList<String> descartesList = new JList<>();
        descartesList.setName("list.proyecto.descartes");
        
        descartesList.setBackground(themeManager.getTemaActual().colorFondoPrincipal());
        descartesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        descartesList.setCellRenderer(new NombreArchivoRenderer(themeManager, model, true));
        registry.register("list.proyecto.descartes", descartesList, "WHEEL_NAVIGABLE");
        
        Action restoreFromDiscardsAction = this.generalController.getVisorController().getActionFactory().getActionMap().get(AppActionCommands.CMD_PROYECTO_RESTAURAR_DE_DESCARTES);
        Action deleteFromProjectAction = this.generalController.getVisorController().getActionFactory().getActionMap().get(AppActionCommands.CMD_PROYECTO_ELIMINAR_PERMANENTEMENTE);

        Action localizarAction = this.generalController.getVisorController().getActionFactory().getActionMap().get(AppActionCommands.CMD_PROYECTO_LOCALIZAR_ARCHIVO);
        Action vaciarDescartesAction = this.generalController.getVisorController().getActionFactory().getActionMap().get(AppActionCommands.CMD_PROYECTO_VACIAR_DESCARTES);
        
        if (restoreFromDiscardsAction != null && deleteFromProjectAction != null) {
        	
        	descartesList.addMouseListener(createContextMenuListener(descartesList, 
        	        restoreFromDiscardsAction,
        	        new javax.swing.JPopupMenu.Separator(),
        	        localizarAction,
        	        new javax.swing.JPopupMenu.Separator(),
        	        vaciarDescartesAction,
        	        new javax.swing.JPopupMenu.Separator(),
        	        deleteFromProjectAction
            ));
        } else {
            if (restoreFromDiscardsAction == null) {
                logger.warn("WARN [ProjectBuilder]: No se pudo encontrar la acción CMD_PROYECTO_RESTAURAR_DE_DESCARTES.");
            }
            if (deleteFromProjectAction == null) {
                logger.warn("WARN [ProjectBuilder]: No se pudo encontrar la acción CMD_PROYECTO_ELIMINAR_PERMANENTEMENTE.");
            }
        }        
        
        JScrollPane scrollPaneDescartes = new JScrollPane(descartesList);
        scrollPaneDescartes.setBorder(null);
        registry.register("scroll.proyecto.descartes", scrollPaneDescartes);
        herramientasTabbedPane.addTab("Descartes", scrollPaneDescartes);

        vista.panels.export.ExportPanel panelExportar = new vista.panels.export.ExportPanel(
        	    this.generalController.getProjectController(),
        	    (e) -> this.generalController.getProjectController().actualizarEstadoExportacionUI()
        	);
        
        registry.register("panel.proyecto.herramientas.exportar", panelExportar);
        
        registry.register("panel.proyecto.exportacion", panelExportar);
        logger.debug("  -> Panel de exportación registrado con la clave 'panel.proyecto.exportacion' para el refresco del tema.");
        
        JTable tablaExportacion = panelExportar.getTablaExportacion();
        if (tablaExportacion != null) {
            registry.register("tabla.exportacion", tablaExportacion, "WHEEL_NAVIGABLE");
        }
        
        Action assignAction = generalController.getVisorController().getActionFactory().getActionMap().get(AppActionCommands.CMD_EXPORT_ASIGNAR_ARCHIVO);
        Action openLocationAction = generalController.getVisorController().getActionFactory().getActionMap().get(AppActionCommands.CMD_EXPORT_ABRIR_UBICACION);
        Action removeFromQueueAction = generalController.getVisorController().getActionFactory().getActionMap().get(AppActionCommands.CMD_EXPORT_QUITAR_DE_COLA);
        Action toggleIgnoreAction = generalController.getVisorController().getActionFactory().getActionMap().get(AppActionCommands.CMD_EXPORT_IGNORAR_COMPRIMIDO);
        Action relocateAction = generalController.getVisorController().getActionFactory().getActionMap().get(AppActionCommands.CMD_EXPORT_RELOCALIZAR_IMAGEN);
        
        tablaExportacion.addMouseListener(createContextMenuListener(tablaExportacion, 
            assignAction, 
            openLocationAction,
            relocateAction,
            new javax.swing.JPopupMenu.Separator(),
            toggleIgnoreAction,
            new javax.swing.JPopupMenu.Separator(),
            removeFromQueueAction
        ));
        
        herramientasTabbedPane.addTab("Exportar", panelExportar);

        JPanel panelEtiquetarPlaceholder = new JPanel();
        herramientasTabbedPane.addTab("Etiquetar", panelEtiquetarPlaceholder);
        herramientasTabbedPane.setEnabledAt(2, false); 

        panelHerramientas.add(herramientasTabbedPane, BorderLayout.CENTER);

        // =========================================================================
        // === INICIO DE MODIFICACIÓN ===
        // =========================================================================
        herramientasTabbedPane.addChangeListener(e -> {
            if (herramientasTabbedPane.getSelectedIndex() == 1) { // Pestaña "Exportar"
                logger.debug("[ProjectBuilder ChangeListener] Pestaña 'Exportar' seleccionada.");
                
                // 1. Prepara la cola de exportación (esto llena el modelo de la tabla).
                this.projectController.solicitarPreparacionColaExportacion();
                
                // 2. Llama al nuevo método del controlador para que ESTABLEZCA la lista maestra.
                this.projectController.setExportListAsMasterList();

            } else {
                // Al salir de "Exportar", la lógica en desactivarModoVistaExportacion()
                // del ProjectController ya se encarga de restaurar la lista maestra original.
            }
        });
        // =========================================================================
        // === FIN DE MODIFICACIÓN ===
        // =========================================================================
        
        return panelHerramientas;
    } // --- Fin del método createProjectToolsPanel ---
    
    
    @Override
    public void onThemeChanged(Tema nuevoTema) {
        logger.info("--- [ProjectBuilder] Reaccionando al cambio de tema...");
        if (registry == null) return;
        
        SwingUtilities.invokeLater(() -> {
        	actualizarColorPanel("view.panel.proyectos", nuevoTema.colorFondoPrincipal());
            actualizarBordeConTema("panel.proyecto.listas.container", "Selección Actual", nuevoTema);
            actualizarBordeConTema("panel.proyecto.herramientas.container", "Herramientas de Proyecto", nuevoTema);

            ImageDisplayPanel displayPanel = registry.get("panel.proyecto.display");
            if (displayPanel != null) {
                displayPanel.actualizarColorDeFondoPorTema(themeManager);
            }

            JList<?> listaNombres = registry.get("list.proyecto.nombres");
            if(listaNombres != null) listaNombres.setBackground(nuevoTema.colorFondoSecundario());
            JList<?> listaDescartes = registry.get("list.proyecto.descartes");
            if(listaDescartes != null) listaDescartes.setBackground(nuevoTema.colorFondoPrincipal());
            
            javax.swing.JTabbedPane tabbedPane = registry.get("tabbedpane.proyecto.herramientas");
            if (tabbedPane != null) {
                SwingUtilities.updateComponentTreeUI(tabbedPane);
            }
            
            JTable tablaExportacion = registry.get("tabla.exportacion");
            if (tablaExportacion != null) {
                tablaExportacion.setBackground(nuevoTema.colorFondoSecundario());
                tablaExportacion.setForeground(nuevoTema.colorTextoPrimario());
                tablaExportacion.getTableHeader().setBackground(nuevoTema.colorFondoPrincipal());
                tablaExportacion.getTableHeader().setForeground(nuevoTema.colorTextoPrimario());
                
                java.awt.Component parent = tablaExportacion.getParent();
                if(parent instanceof javax.swing.JViewport) {
                    parent.setBackground(nuevoTema.colorFondoSecundario());
                }
            }
            
            vista.panels.export.ExportPanel panelExportar = registry.get("panel.proyecto.exportacion");
            if(panelExportar != null){
                SwingUtilities.updateComponentTreeUI(panelExportar);
                panelExportar.setBackground(nuevoTema.colorFondoPrincipal());
                panelExportar.repaint();
            }

            JToolBar accionesExportacionToolbar = registry.get("toolbar.acciones_exportacion");
            if(accionesExportacionToolbar != null) {
                accionesExportacionToolbar.repaint();
            }
            
        });
        
    } // --- FIN del metodo onThemeChanged ---

    private void actualizarBordeConTema(String panelKey, String titulo, Tema tema) {
        JPanel panel = registry.get(panelKey);
        if (panel != null && panel.getBorder() instanceof TitledBorder) {
            TitledBorder border = (TitledBorder) panel.getBorder();
            border.setTitleColor(tema.colorBordeTitulo());
            panel.repaint();
        }
    } // ---FIN de metodo ---

    private void actualizarColorPanel(String panelKey, java.awt.Color color) {
        JPanel panel = registry.get(panelKey);
        if (panel != null) {
            panel.setBackground(color);
        }
    } // ---FIN de metodo ---

    
    private java.awt.event.MouseAdapter createContextMenuListener(javax.swing.JComponent component, Object... menuItems) {
        return new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) { if (e.isPopupTrigger()) { showMenu(e); } }
            public void mouseReleased(java.awt.event.MouseEvent e) { if (e.isPopupTrigger()) { showMenu(e); } }

            private void showMenu(java.awt.event.MouseEvent e) {
                if (component instanceof JList) {
                    JList<?> list = (JList<?>) component;
                    int row = list.locationToIndex(e.getPoint());
                    if (row != -1) {
                        list.setSelectedIndex(row);
                    }
                } else if (component instanceof JTable) {
                    JTable table = (JTable) component;
                    int row = table.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        table.setRowSelectionInterval(row, row);
                    }
                }
                
                JPopupMenu menu = new JPopupMenu();
                for (Object item : menuItems) {
                    if (item instanceof Action) {
                        Action action = (Action) item;
                        action.setEnabled(true); 
                        menu.add(action);
                    } else if (item instanceof javax.swing.JPopupMenu.Separator) {
                        menu.addSeparator();
                    }
                }
                
                if (menu.getComponentCount() > 0) {
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        };
    } // --- Fin del método createContextMenuListener ---
    

} // --- FIN DE LA CLASE ProjectBuilder ---