package vista.builders;

import java.awt.BorderLayout;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import controlador.GeneralController;
import controlador.commands.AppActionCommands;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import vista.panels.ImageDisplayPanel;
import vista.renderers.NombreArchivoRenderer;
import vista.theme.ThemeManager;

public class ProjectBuilder {

    private final ComponentRegistry registry;
    private final VisorModel model;
    private final ThemeManager themeManager;
    private final GeneralController generalController;

    // <--- MODIFICADO: Constructor ahora necesita ThemeManager ---
    public ProjectBuilder(ComponentRegistry registry, VisorModel model, ThemeManager themeManager, GeneralController generalController) {
        this.registry = Objects.requireNonNull(registry, "Registry no puede ser null en ProjectBuilder");
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en ProjectBuilder");
        this.themeManager = Objects.requireNonNull(themeManager, "ThemeManager no puede ser null en ProjectBuilder");
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null");
    } // --- Fin del método ProjectBuilder (constructor) ---

    
    /**
     * MODIFICADO: Construye el panel principal para la vista de proyecto con una estructura
     * de Dashboard, utilizando JSplitPanes anidados y JTabbedPanes.
     * Esta estructura es flexible y permite añadir nuevas herramientas y listas en el futuro.
     * @return Un JPanel configurado con la nueva estructura de Dashboard.
     */
    public JPanel buildProjectViewPanel() {
        System.out.println("  [ProjectBuilder] Construyendo el panel del modo proyecto (Dashboard)...");

        // --- 1. Panel Raíz de la Vista Proyecto ---
        // Este es el panel que se añadirá al CardLayout principal. Usa BorderLayout.
        JPanel panelProyectoRaiz = new JPanel(new BorderLayout());
        registry.register("view.panel.proyectos", panelProyectoRaiz);

        // --- 2. JSplitPane Principal (HORIZONTAL) ---
        // Divide la vista en una zona izquierda (listas/herramientas) y una derecha (visor).
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setResizeWeight(0.25); // El 25% del espacio va a la izquierda por defecto.
        mainSplit.setContinuousLayout(true);
        mainSplit.setBorder(null); // Sin bordes para una apariencia limpia.
        registry.register("splitpane.proyecto.main", mainSplit);

        // --- 3. JSplitPane Izquierdo (VERTICAL) ---
        // Divide la zona izquierda en una parte superior (listas) y una inferior (herramientas).
        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftSplit.setResizeWeight(0.6); // El 60% del espacio para las listas por defecto.
        leftSplit.setContinuousLayout(true);
        leftSplit.setBorder(null);
        registry.register("splitpane.proyecto.left", leftSplit);

        // --- 4. Creación de los Paneles Contenedores ---
        // Se crean los paneles que irán dentro de los JSplitPanes.

        // 4.1. Panel para las listas (arriba-izquierda)
        JPanel panelListas = createProjectListsPanel();
        
        // 4.2. Panel para las herramientas (abajo-izquierda)
        JPanel panelHerramientas = createProjectToolsPanel();

        // --- INICIO DE LA CORRECCIÓN ---
        // 4.3. Panel para el visor de imagen (derecha)
        // Se crea directamente aquí para no depender de un método que vamos a eliminar.
        JPanel panelVisor = new JPanel(new BorderLayout());
        panelVisor.setBackground(themeManager.getTemaActual().colorFondoSecundario());
        registry.register("panel.proyecto.visor", panelVisor);

        ImageDisplayPanel imageDisplayPanel = new ImageDisplayPanel(this.themeManager, this.model);
        registry.register("panel.proyecto.display", imageDisplayPanel);
        
        TitledBorder border = BorderFactory.createTitledBorder("");
        border.setTitleColor(themeManager.getTemaActual().colorBordeTitulo());
        imageDisplayPanel.setBorder(border);
        
        panelVisor.add(imageDisplayPanel, BorderLayout.CENTER);
        // --- FIN DE LA CORRECCIÓN ---

        // --- 5. Ensamblaje de la Estructura ---
        // Se conectan todos los paneles en el orden correcto.
        leftSplit.setTopComponent(panelListas);
        leftSplit.setBottomComponent(panelHerramientas);

        mainSplit.setLeftComponent(leftSplit);
        mainSplit.setRightComponent(panelVisor);

        // --- 6. Añadir la estructura completa al panel raíz ---
        panelProyectoRaiz.add(mainSplit, BorderLayout.CENTER);
        
        System.out.println("  [ProjectBuilder] Panel del modo proyecto (Dashboard) construido y ensamblado.");
        
        return panelProyectoRaiz;
    } // --- Fin del método buildProjectViewPanel ---
    
    
    /**
     * MODIFICADO: Crea el panel que contiene únicamente la lista de "Selección Actual" del proyecto
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
        projectFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectFileList.setCellRenderer(new NombreArchivoRenderer(themeManager));
        registry.register("list.proyecto.nombres", projectFileList);

        // --- AÑADIR LISTENER DE MENÚ CONTEXTUAL ---
        // Se obtiene la acción correspondiente del ActionFactory a través del GeneralController.
        Action moveToDiscardsAction = this.generalController.getVisorController().getActionFactory().getActionMap().get(AppActionCommands.CMD_PROYECTO_MOVER_A_DESCARTES);
        if (moveToDiscardsAction != null) {
            // Se le asigna el listener a la lista.
            projectFileList.addMouseListener(createContextMenuListener(projectFileList, moveToDiscardsAction));
        } else {
             System.err.println("WARN [ProjectBuilder]: No se pudo encontrar la acción CMD_PROYECTO_MOVER_A_DESCARTES en el ActionFactory.");
        }
        // --- FIN DE LA ADICIÓN DEL LISTENER ---

        JScrollPane scrollPane = new JScrollPane(projectFileList);
        scrollPane.setBorder(null); // El borde lo pone el panel contenedor
        registry.register("scroll.proyecto.nombres", scrollPane);
        
        panelListas.add(scrollPane, BorderLayout.CENTER);
        
        return panelListas;
    } // --- Fin del método createProjectListsPanel ---
    
    
    /**
     * MODIFICADO: Crea el panel de herramientas, que ahora incluye un ExportPanel funcional
     * con todos sus ActionListeners conectados al ProjectController.
     * @return Un JPanel configurado con un JTabbedPane para las herramientas.
     */
    private JPanel createProjectToolsPanel() {
        JPanel panelHerramientas = new JPanel(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder("Herramientas de Proyecto");
        border.setTitleColor(themeManager.getTemaActual().colorBordeTitulo());
        panelHerramientas.setBorder(border);
        registry.register("panel.proyecto.herramientas.container", panelHerramientas);
        
        javax.swing.JTabbedPane herramientasTabbedPane = new javax.swing.JTabbedPane();
        registry.register("tabbedpane.proyecto.herramientas", herramientasTabbedPane);
        
        // Pestaña 1: Lista de Descartes
        JList<String> descartesList = new JList<>();
        descartesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        descartesList.setCellRenderer(new NombreArchivoRenderer(themeManager));
        registry.register("list.proyecto.descartes", descartesList);
        Action restoreFromDiscardsAction = this.generalController.getVisorController().getActionFactory().getActionMap().get(AppActionCommands.CMD_PROYECTO_RESTAURAR_DE_DESCARTES);
        if (restoreFromDiscardsAction != null) {
            descartesList.addMouseListener(createContextMenuListener(descartesList, restoreFromDiscardsAction));
        }
        JScrollPane scrollPaneDescartes = new JScrollPane(descartesList);
        scrollPaneDescartes.setBorder(null);
        registry.register("scroll.proyecto.descartes", scrollPaneDescartes);
        herramientasTabbedPane.addTab("Descartes", scrollPaneDescartes);

        // Pestaña 2: Panel de Exportación
        vista.panels.export.ExportPanel panelExportar = new vista.panels.export.ExportPanel(this.generalController.getProjectController());
        registry.register("panel.proyecto.herramientas.exportar", panelExportar);
        
        // --- CONECTAR BOTONES A LA LÓGICA ---
        panelExportar.getBotonCargarProyecto().addActionListener(e -> {
            if (generalController != null && generalController.getProjectController() != null) {
                generalController.getProjectController().solicitarPreparacionColaExportacion();
            }
        });
        
        panelExportar.getBotonSeleccionarCarpeta().addActionListener(e -> {
            if (generalController != null && generalController.getProjectController() != null) {
                generalController.getProjectController().solicitarSeleccionCarpetaDestino();
            }
        });
        
        // --- INICIO DE LA NUEVA MODIFICACIÓN ---
        panelExportar.getBotonIniciarExportacion().addActionListener(e -> {
            if (generalController != null && generalController.getProjectController() != null) {
                generalController.getProjectController().solicitarInicioExportacion();
            }
        });
        // --- FIN DE LA NUEVA MODIFICACIÓN ---
        
        herramientasTabbedPane.addTab("Exportar", panelExportar);

        // Pestaña 3: Placeholder para el futuro Panel de Etiquetado
        JPanel panelEtiquetarPlaceholder = new JPanel();
        herramientasTabbedPane.addTab("Etiquetar", panelEtiquetarPlaceholder);
        herramientasTabbedPane.setEnabledAt(2, false); 

        panelHerramientas.add(herramientasTabbedPane, BorderLayout.CENTER);
        return panelHerramientas;
    } // --- Fin del método createProjectToolsPanel ---
    
    
    /**
     * Crea un MouseListener que muestra un menú contextual con acciones.
     * @param list La JList a la que se asociará el listener.
     * @param actions Las acciones a añadir al menú contextual.
     * @return Un MouseAdapter configurado.
     */
    private java.awt.event.MouseAdapter createContextMenuListener(JList<String> list, Action... actions) {
        return new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMenu(e);
                }
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMenu(e);
                }
            }

            private void showMenu(java.awt.event.MouseEvent e) {
                int row = list.locationToIndex(e.getPoint());
                if (row != -1) { list.setSelectedIndex(row); }
                JPopupMenu menu = new JPopupMenu();
                for (Action action : actions) {
                    action.setEnabled(true);
                    menu.add(action);
                }
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        };
    } // --- Fin del método createContextMenuListener ---
    

} // --- FIN DE LA CLASE ProjectBuilder ---


