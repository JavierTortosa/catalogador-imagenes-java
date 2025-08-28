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
import vista.util.ThumbnailPreviewer; // <<< AÑADIR IMPORT

public class ProjectBuilder implements ThemeChangeListener{

	private static final Logger logger = LoggerFactory.getLogger(ProjectBuilder.class);
	
    private final ComponentRegistry registry;
    private final VisorModel model;
    private final ThemeManager themeManager;
    private final GeneralController generalController;
    private final ToolbarManager toolbarManager;
    
    // Constructor ahora necesita ThemeManager ---
    public ProjectBuilder(ComponentRegistry registry, VisorModel model, ThemeManager themeManager, GeneralController generalController, ToolbarManager toolbarManager) {
        this.registry = Objects.requireNonNull(registry, "Registry no puede ser null en ProjectBuilder");
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en ProjectBuilder");
        this.themeManager = Objects.requireNonNull(themeManager, "ThemeManager no puede ser null en ProjectBuilder");
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null");
        this.toolbarManager = Objects.requireNonNull(toolbarManager, "ToolbarManager no puede ser null");
    } // --- Fin del método ProjectBuilder (constructor) ---

    
    /**
     * Construye el panel principal para la vista de proyecto con una estructura
     * de Dashboard, utilizando JSplitPanes anidados y JTabbedPanes.
     * Esta estructura es flexible y permite añadir nuevas herramientas y listas en el futuro.
     * @return Un JPanel configurado con la nueva estructura de Dashboard.
     */
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
        
        // =========================================================================
        // === INICIO DE LA CORRECCIÓN ESTRUCTURAL ===
        // =========================================================================
        // Replicamos la estructura de CardLayout del modo Visualizador aquí.
        
        // 1. Creamos el contenedor que usará CardLayout.
        JPanel displayModesContainer = new JPanel(new CardLayout());
        // ¡¡IMPORTANTE!! Usamos una clave DIFERENTE en el registro para evitar conflictos.
        registry.register("container.displaymodes.proyecto", displayModesContainer); 

        // 2. Creamos el panel para la vista SINGLE_IMAGE del proyecto.
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
	             // Obtenemos la clave de la imagen que se está mostrando actualmente.
	             String currentImageKey = model.getSelectedImageKey();
	             if (currentImageKey == null || currentImageKey.isEmpty()) {
	                 return; // No hay imagen seleccionada, no mostramos menú.
	             }
	
	             // Determinamos a qué lista pertenece la imagen actual.
	             // La forma más fiable es preguntar al ProjectManager.
	             java.nio.file.Path imagePath = model.getRutaCompleta(currentImageKey);
	             
	             // Necesitamos acceso al ProjectManager, que está en el ProjectController.
	             controlador.managers.interfaces.IProjectManager projectManager = generalController.getProjectController().getProjectManager();
	
	             if (imagePath == null || projectManager == null) {
	                 return;
	             }
	
	             boolean isEnDescartes = projectManager.estaEnDescartes(imagePath);
	             
	             String listaActiva = model.getProyectoListContext().getNombreListaActiva();
	             
	             JPopupMenu menu = new JPopupMenu();
	
	             // Obtenemos las Actions que vamos a necesitar.
	             Action moveToDiscardsAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_MOVER_A_DESCARTES);
	             Action restoreFromDiscardsAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_RESTAURAR_DE_DESCARTES);
	             Action deleteFromProjectAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_ELIMINAR_PERMANENTEMENTE);
	             Action localizarAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_LOCALIZAR_ARCHIVO);
	             Action vaciarDescartesAction = generalController.getVisorController().getActionMap().get(AppActionCommands.CMD_PROYECTO_VACIAR_DESCARTES);
	             
	             // Construir el menú dinámicamente.
	             
	             if ("seleccion".equals(listaActiva)) {
	                 if (moveToDiscardsAction != null) menu.add(moveToDiscardsAction);
	                 menu.addSeparator(); // <-- Separador
	                 if (localizarAction != null) menu.add(localizarAction); // <-- Añadir localizar

	             } else if ("descartes".equals(listaActiva)) {
	                 if (restoreFromDiscardsAction != null) menu.add(restoreFromDiscardsAction);
	                 menu.addSeparator(); // <-- Separador
	                 if (localizarAction != null) menu.add(localizarAction); // <-- Añadir localizar
	                 menu.addSeparator(); // <-- Separador
	                 if (vaciarDescartesAction != null) menu.add(vaciarDescartesAction); // <-- Añadir vaciar
	                 menu.addSeparator(); // <-- Separador
	                 if (deleteFromProjectAction != null) menu.add(deleteFromProjectAction);
	             }
	             
	             // Mostrar el menú solo si tiene items.
	             if (menu.getComponentCount() > 0) {
	                 menu.show(e.getComponent(), e.getX(), e.getY());
	             }
	         }
	     });
	
        // 3. Creamos el previsualizador para el grid del proyecto.
        ThumbnailPreviewer projectGridPreviewer = new ThumbnailPreviewer(
            null, 
            this.model, 
            this.themeManager, 
            null, // viewManager no está disponible y no es necesario para el previsualizador.
            this.registry
        );

        // 4. Creamos el panel para la vista GRID del proyecto, pasándole el previsualizador.
        GridDisplayPanel gridViewPanel = new GridDisplayPanel(
            this.model, 
            generalController.getVisorController().getServicioMiniaturas(), 
            this.themeManager, 
            generalController.getVisorController().getIconUtils(),
            projectGridPreviewer,
            generalController.getProjectController().getProjectManager() // <<< Pasamos la instancia que acabamos de crear
        );
        
        
        // --- Bloque de conexión de la toolbar ---
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
        // --- Fin del bloque ---
        
        
        ToolbarManager toolbarManager = generalController.getToolbarManager();
        if (toolbarManager != null) {
            JToolBar gridToolbar = toolbarManager.getToolbar("barra_grid");
            // Y se la pasamos al GridDisplayPanel para que la muestre
            gridViewPanel.setToolbar(gridToolbar);
            logger.debug(" -> Toolbar 'barra_grid' inyectada en el GridDisplayPanel del proyecto.");
        }
        
        registry.register("panel.display.grid.proyecto", gridViewPanel);
        
        JList<String> projectGridList = gridViewPanel.getGridList();
        registry.register("list.grid.proyecto", gridViewPanel.getGridList(), "WHEEL_NAVIGABLE");

        // 5. Creamos el MouseListener para el menú contextual del grid (esta parte no cambia).
        projectGridList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) { 
                handlePopupAndSelection(e);
                if (e.isPopupTrigger()) { showProjectGridMenu(e); } 
            }
            public void mouseReleased(java.awt.event.MouseEvent e) { 
                handlePopupAndSelection(e);
                if (e.isPopupTrigger()) { showProjectGridMenu(e); } 
            }

            // --- MÉTODO HELPER PARA SINCRONIZAR LA SELECCIÓN ---
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
        
        // 6. Creamos un placeholder para POLAROID en proyecto.
        JPanel polaroidViewPanel = new JPanel();
        polaroidViewPanel.add(new JLabel("Vista POLAROID (Proyecto) en construcción..."));
        registry.register("panel.display.polaroid.proyecto", polaroidViewPanel);
        
        // 7. Añadimos las "tarjetas" al CardLayout.
        displayModesContainer.add(singleImageViewPanel, "VISTA_SINGLE_IMAGE");
        displayModesContainer.add(gridViewPanel, "VISTA_GRID");
        displayModesContainer.add(polaroidViewPanel, "VISTA_POLAROID");

        leftSplit.setTopComponent(panelListas);
        leftSplit.setBottomComponent(panelHerramientas);

        mainSplit.setLeftComponent(leftSplit);
        // Ahora, el lado derecho del split es el contenedor con el CardLayout.
        mainSplit.setRightComponent(displayModesContainer);

        panelProyectoRaiz.add(mainSplit, BorderLayout.CENTER);
        
        logger.info("  [ProjectBuilder] Panel del modo proyecto (Dashboard) construido y ensamblado.");
        
        return panelProyectoRaiz;
        
    } // --- Fin del método buildProjectViewPanel ---
    
    
    /**
     * Crea el panel que contiene únicamente la lista de "Selección Actual" del proyecto
     * y le añade un listener de menú contextual para mover imágenes a descartes.
     * @return Un JPanel configurado con la lista de imágenes seleccionadas.
     */
    private JPanel createProjectListsPanel() {
        // --- Panel contenedor principal para la zona de listas ---
        JPanel panelListas = new JPanel(new BorderLayout());
        
        TitledBorder border = BorderFactory.createTitledBorder("Selección Actual");
        border.setTitleColor(themeManager.getTemaActual().colorBordeTitulo());
        panelListas.setBorder(border);
        registry.register("panel.proyecto.listas.container", panelListas);
        
        
        // --- Crear y añadir la lista de "Selección Actual" ---
        JList<String> projectFileList = new JList<>();
        projectFileList.setName("list.proyecto.nombres");
        
        projectFileList.setBackground(themeManager.getTemaActual().colorFondoSecundario());
        
        projectFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectFileList.setCellRenderer(new NombreArchivoRenderer(themeManager, model)); // Se pasa el modelo
        registry.register("list.proyecto.nombres", projectFileList, "WHEEL_NAVIGABLE");

        // Se obtiene la acción correspondiente del ActionFactory a través del GeneralController.
        Action moveToDiscardsAction = this.generalController.getVisorController().getActionFactory().getActionMap().get(AppActionCommands.CMD_PROYECTO_MOVER_A_DESCARTES);
        Action localizarAction = this.generalController.getVisorController().getActionFactory().getActionMap().get(AppActionCommands.CMD_PROYECTO_LOCALIZAR_ARCHIVO);
        
        if (moveToDiscardsAction != null) {
            // Se le asigna el listener a la lista.
            projectFileList.addMouseListener(createContextMenuListener(
            		projectFileList, 
            		moveToDiscardsAction, 
            		new javax.swing.JPopupMenu.Separator(), 
            		localizarAction));
        } else {
             logger.error("WARN [ProjectBuilder]: No se pudo encontrar la acción CMD_PROYECTO_MOVER_A_DESCARTES en el ActionFactory.");
        }

        JScrollPane scrollPane = new JScrollPane(projectFileList);
        scrollPane.setBorder(null); // El borde lo pone el panel contenedor
        registry.register("scroll.proyecto.nombres", scrollPane);
        
        panelListas.add(scrollPane, BorderLayout.CENTER);
        
        return panelListas;
    } // --- Fin del método createProjectListsPanel ---
    
    
    /**
     * Crea el panel de herramientas, incluyendo un ExportPanel con una tabla
     * que tiene un menú contextual con acciones para gestionar la cola.
     * @return Un JPanel configurado con un JTabbedPane para las herramientas.
     */
    private JPanel createProjectToolsPanel() {
        JPanel panelHerramientas = new JPanel(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder("Herramientas de Proyecto");
        border.setTitleColor(themeManager.getTemaActual().colorBordeTitulo());
        registry.register("panel.proyecto.herramientas.container", panelHerramientas);
        
        javax.swing.JTabbedPane herramientasTabbedPane = new javax.swing.JTabbedPane();
        registry.register("tabbedpane.proyecto.herramientas", herramientasTabbedPane);
        
        // --- Pestaña 1: Lista de Descartes ---
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

        // --- Pestaña 2: Panel de Exportación ---
        vista.panels.export.ExportPanel panelExportar = new vista.panels.export.ExportPanel(
        	    this.generalController.getProjectController(),
        	    (e) -> this.generalController.getProjectController().actualizarEstadoExportacionUI()
        	);
        
        registry.register("panel.proyecto.herramientas.exportar", panelExportar);
        
        registry.register("panel.proyecto.exportacion", panelExportar);
        logger.debug("  -> Panel de exportación registrado con la clave 'panel.proyecto.exportacion' para el refresco del tema.");
        
        JTable tablaExportacion = panelExportar.getTablaExportacion();
        if (tablaExportacion != null) {
            // Registramos la TABLA, no su scroll, con la clave que GeneralController espera.
            // Y le añadimos la etiqueta para que el listener se le aplique.
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
            relocateAction, // <-- Añadimos la nueva acción al menú
            new javax.swing.JPopupMenu.Separator(),
            toggleIgnoreAction,
            new javax.swing.JPopupMenu.Separator(),
            removeFromQueueAction
        ));
        
        herramientasTabbedPane.addTab("Exportar", panelExportar);

        // --- Pestaña 3: Placeholder para Etiquetar ---
        JPanel panelEtiquetarPlaceholder = new JPanel();
        herramientasTabbedPane.addTab("Etiquetar", panelEtiquetarPlaceholder);
        herramientasTabbedPane.setEnabledAt(2, false); 

        panelHerramientas.add(herramientasTabbedPane, BorderLayout.CENTER);

        // --- LÓGICA DE CARGA AUTOMÁTICA ---
        herramientasTabbedPane.addChangeListener(e -> {
        	
        	// --- INICIO DE LA MODIFICACIÓN ---
            DisplayModeManager dmm = generalController.getVisorController().getDisplayModeManager();

            // Comprobamos si la pestaña recién seleccionada es la de "Exportar"
            if (herramientasTabbedPane.getSelectedIndex() == 1) { // El índice 1 corresponde a "Exportar"
                logger.debug("[ChangeListener] Pestaña 'Exportar' seleccionada. Limpiando lista maestra y preparando cola...");
                
                // Vaciamos la lista maestra para que el grid se quede en blanco
                if (dmm != null) {
                    dmm.poblarGridConModelo(new DefaultListModel<>());
                }

                generalController.getProjectController().solicitarPreparacionColaExportacion();
            } else {
                // Si se selecciona CUALQUIER OTRA pestaña (como "Descartes"), restauramos la lista.
                logger.debug("[ChangeListener] Pestaña que no es 'Exportar' seleccionada. Restaurando modelo principal...");
                generalController.getProjectController().actualizarModeloPrincipalConListaDeProyectoActiva();
            }
            // --- FIN DE LA MODIFICACIÓN ---
        });
        // --- FIN DE LÓGICA DE CARGA AUTOMÁTICA ---
        
        return panelHerramientas;
    } // --- Fin del método createProjectToolsPanel ---
    
    
    @Override
    public void onThemeChanged(Tema nuevoTema) {
        logger.info("--- [ProjectBuilder] Reaccionando al cambio de tema...");
        if (registry == null) return;
        
        SwingUtilities.invokeLater(() -> {
            // Actualiza los paneles principales y sus bordes
        	actualizarColorPanel("view.panel.proyectos", nuevoTema.colorFondoPrincipal());
            actualizarBordeConTema("panel.proyecto.listas.container", "Selección Actual", nuevoTema);
            actualizarBordeConTema("panel.proyecto.herramientas.container", "Herramientas de Proyecto", nuevoTema);

            // Actualiza el visor de imagen
            ImageDisplayPanel displayPanel = registry.get("panel.proyecto.display");
            if (displayPanel != null) {
                displayPanel.actualizarColorDeFondoPorTema(themeManager);
            }

            // Actualiza las listas
            JList<?> listaNombres = registry.get("list.proyecto.nombres");
            if(listaNombres != null) listaNombres.setBackground(nuevoTema.colorFondoSecundario());
            JList<?> listaDescartes = registry.get("list.proyecto.descartes");
            if(listaDescartes != null) listaDescartes.setBackground(nuevoTema.colorFondoPrincipal());
            
            // Actualiza el TabbedPane y su contenido
            javax.swing.JTabbedPane tabbedPane = registry.get("tabbedpane.proyecto.herramientas");
            if (tabbedPane != null) {
                SwingUtilities.updateComponentTreeUI(tabbedPane);
            }
            
            // MUY IMPORTANTE: Actualiza la tabla y su viewport
            JTable tablaExportacion = registry.get("tabla.exportacion");
            if (tablaExportacion != null) {
                // La tabla en sí
                tablaExportacion.setBackground(nuevoTema.colorFondoSecundario());
                tablaExportacion.setForeground(nuevoTema.colorTextoPrimario());
                tablaExportacion.getTableHeader().setBackground(nuevoTema.colorFondoPrincipal());
                tablaExportacion.getTableHeader().setForeground(nuevoTema.colorTextoPrimario());
                
                // El panel que la contiene (viewport)
                java.awt.Component parent = tablaExportacion.getParent();
                if(parent instanceof javax.swing.JViewport) {
                    parent.setBackground(nuevoTema.colorFondoSecundario());
                }
            }
            
            // Actualizamos explícitamente el ExportPanel, que es el contenedor de la JToolBar problemática.
            vista.panels.export.ExportPanel panelExportar = registry.get("panel.proyecto.exportacion");
            if(panelExportar != null){
                // 1. Actualizar el panel en sí mismo y sus hijos estándar.
                SwingUtilities.updateComponentTreeUI(panelExportar);
                // 2. Forzar el color de fondo para asegurar que no se herede nada raro.
                panelExportar.setBackground(nuevoTema.colorFondoPrincipal());
                panelExportar.repaint();
            }

            // <<-- Forzar repintado de la propia toolbar -->>
            // Aunque reconstruirPanelesEspecialesTrasTema ya lo hace, un repaint extra no hace daño.
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

    
    /**
     * Crea un MouseListener que muestra un menú contextual.
     * Funciona tanto para JList como para JTable y permite añadir separadores.
     * @param component El componente (JList o JTable) al que se asociará el listener.
     * @param menuItems Los ítems del menú (pueden ser Actions o JSeparators).
     * @return Un MouseAdapter configurado.
     */
    private java.awt.event.MouseAdapter createContextMenuListener(javax.swing.JComponent component, Object... menuItems) {
        return new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) { if (e.isPopupTrigger()) { showMenu(e); } }
            public void mouseReleased(java.awt.event.MouseEvent e) { if (e.isPopupTrigger()) { showMenu(e); } }

            private void showMenu(java.awt.event.MouseEvent e) {
                // Seleccionar la fila bajo el cursor antes de mostrar el menú
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
                        // Actualizar el estado 'enabled' de la acción antes de mostrarla
                        action.setEnabled(true); 
                        menu.add(action);
                    } else if (item instanceof javax.swing.JPopupMenu.Separator) {
                        menu.addSeparator();
                    }
                }
                
                // Mostrar el menú solo si tiene items
                if (menu.getComponentCount() > 0) {
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        };
    } // --- Fin del método createContextMenuListener ---
    

} // --- FIN DE LA CLASE ProjectBuilder ---