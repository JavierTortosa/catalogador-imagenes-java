package vista.builders;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ClientController;
import controlador.GeneralController;
import controlador.managers.ToolbarManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ProjectManager;
import vista.renderers.ClientListCellRenderer;
import vista.renderers.ProjectListCellRenderer;
import vista.theme.ThemeManager;
import vista.panels.ImageDisplayPanel;
import vista.panels.GridDisplayPanel;
import vista.panels.PolaroidDisplayPanel;
import vista.util.ThumbnailPreviewer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ClientBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ClientBuilder.class);

    private final ComponentRegistry registry;
    private final VisorModel model;
    private final ThemeManager themeManager;
    private final GeneralController generalController;
    private final ToolbarManager toolbarManager;
    private final ClientController clientController;
    private final ProjectManager projectManager;

    public ClientBuilder(ComponentRegistry registry, VisorModel model, ThemeManager themeManager,
                         GeneralController generalController, ToolbarManager toolbarManager,
                         ClientController clientController, ProjectManager projectManager) {

        this.registry = Objects.requireNonNull(registry, "Registry no puede ser null en ClientBuilder");
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en ClientBuilder");
        this.themeManager = Objects.requireNonNull(themeManager, "ThemeManager no puede ser null");
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null");
        this.toolbarManager = Objects.requireNonNull(toolbarManager, "ToolbarManager no puede ser null");
        this.clientController = Objects.requireNonNull(clientController, "ClientController no puede ser null");
        this.projectManager = Objects.requireNonNull(projectManager, "ProjectManager no puede ser null");
    } // --- FIN de constructor ClientBuilder ---


    public JPanel buildClientViewPanel() {
        logger.info("[ClientBuilder] Construyendo el panel del Modo Cliente...");

        JPanel panelClienteRaiz = new JPanel(new BorderLayout());
        registry.register("view.panel.cliente", panelClienteRaiz);

        // Panel Izquierdo: Proyecto
        JSplitPane leftSplitPanel = createProjectListsPanel();
        // Panel Derecho: Cliente
        JSplitPane rightSplitPanel = createClientListsPanel();

                // Crear el contenedor de modos de visualización (CardLayout)
        JPanel displayModesContainer = new JPanel(new CardLayout());
        registry.register("container.displaymodes.cliente", displayModesContainer);

        ImageDisplayPanel singleImageViewPanel = new ImageDisplayPanel(themeManager, model);
        registry.register("panel.cliente.display", singleImageViewPanel);
        registry.register("label.cliente.imagen", singleImageViewPanel.getInternalLabel(), "WHEEL_NAVIGABLE");

        MouseAdapter focusRequester = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                singleImageViewPanel.requestFocusInWindow();
            }
        };
        singleImageViewPanel.addMouseListener(focusRequester);
        singleImageViewPanel.getInternalLabel().addMouseListener(focusRequester);

        displayModesContainer.add(singleImageViewPanel, "VISTA_SINGLE_IMAGE");

        // --- Visor de Grid ---
        ThumbnailPreviewer clientGridPreviewer = new ThumbnailPreviewer(null, this.model, this.themeManager, null,
                this.registry);
        GridDisplayPanel gridViewPanel = new GridDisplayPanel(this.model,
                generalController.getVisorController().getServicioMiniaturas(), this.themeManager,
                generalController.getVisorController().getIconUtils(), clientGridPreviewer,
                this.registry);
        registry.register("panel.display.grid.cliente", gridViewPanel);
        JList<String> gridList = gridViewPanel.getGridList();
        registry.register("list.grid.cliente", gridList, "WHEEL_NAVIGABLE");

        // --- Visor Polaroid ---
        PolaroidDisplayPanel polaroidViewPanel = new PolaroidDisplayPanel(this.themeManager, this.model);
        registry.register("panel.cliente.display.polaroid", polaroidViewPanel);
        registry.register("panel.cliente.display.polaroid.image", polaroidViewPanel.getImagePanel());
        registry.register("label.cliente.polaroid.imagen", polaroidViewPanel.getInternalLabel(), "WHEEL_NAVIGABLE");

        displayModesContainer.add(gridViewPanel, "VISTA_GRID");
        displayModesContainer.add(polaroidViewPanel, "VISTA_POLAROID");


        JPanel centerPanel = createCenterPanel(displayModesContainer);

        // Ensamblaje (Modo Comparativo: Izquierda, Centro, Derecha)
        // Split Derecho que contiene (Centro + Derecha)
        JSplitPane centerRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerPanel, rightSplitPanel);
        centerRightSplit.setResizeWeight(0.8);
        centerRightSplit.setContinuousLayout(true);
        centerRightSplit.setBorder(null);

        // Split Principal que contiene (Izquierda + CenterRightSplit)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplitPanel, centerRightSplit);
        mainSplit.setResizeWeight(0.2); // 20% para la izquierda
        mainSplit.setContinuousLayout(true);
        mainSplit.setBorder(null);
        registry.register("splitpane.cliente.main", mainSplit);

        panelClienteRaiz.add(mainSplit, BorderLayout.CENTER);

        return panelClienteRaiz;
    } // --- FIN de metodo buildClientViewPanel ---


    private JSplitPane createProjectListsPanel() {
        JPanel panelSeleccion = createListPanel("Selección Actual (Proyecto)", "list.cliente.proyecto.seleccion", true);
        JPanel panelDescartes = createListPanel("Descartes (Proyecto)", "list.cliente.proyecto.descartes", true);

        panelDescartes.setMinimumSize(new java.awt.Dimension(100, 100));
        panelSeleccion.setMinimumSize(new java.awt.Dimension(100, 150));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelSeleccion, panelDescartes);
        split.setContinuousLayout(true);
        split.setBorder(null);
        split.setResizeWeight(0.7);
        return split;
    } // --- FIN de metodo createProjectListsPanel ---


    private JSplitPane createClientListsPanel() {
        JPanel panelSeleccion = createListPanel("Selección (Cliente)", "list.cliente.cliente.seleccion", false);
        JPanel panelDescartes = createListPanel("Descartes (Cliente)", "list.cliente.cliente.descartes", false);

        panelDescartes.setMinimumSize(new java.awt.Dimension(150, 100));
        panelSeleccion.setMinimumSize(new java.awt.Dimension(150, 150));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelSeleccion, panelDescartes);
        split.setContinuousLayout(true);
        split.setBorder(null);
        split.setResizeWeight(0.7);
        return split;
    } // --- FIN de metodo createClientListsPanel ---


    private JPanel createListPanel(String title, String listName, boolean isProject) {
        JPanel panel = new JPanel(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder(title);
        panel.setBorder(border);
        
        JList<String> list = new JList<>();
        list.setName(listName);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        if (isProject) {
            list.setCellRenderer(new ProjectListCellRenderer());
        } else {
            list.setCellRenderer(new ClientListCellRenderer(projectManager));
        }
        
        registry.register(listName, list, "WHEEL_NAVIGABLE");
        JScrollPane scrollPane = new JScrollPane(list);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    } // --- FIN de metodo createListPanel ---


    private JPanel createCenterPanel(JPanel displayModesContainer) {
        JPanel centerPanel = new JPanel(new BorderLayout());
        TitledBorder visorBorder = BorderFactory.createTitledBorder("Visor (Comparativa)");
        centerPanel.setBorder(visorBorder);

        // Agregamos el contenedor de modos de visualización (pasado desde ViewBuilder)
        if (displayModesContainer != null) {
            centerPanel.add(displayModesContainer, BorderLayout.CENTER);
        }

        // Panel inferior para comentarios (mockup por ahora)
        JPanel commentsPanel = new JPanel(new BorderLayout());
        commentsPanel.setBorder(BorderFactory.createTitledBorder("Comentarios del Cliente"));
        javax.swing.JTextArea txtComments = new javax.swing.JTextArea(3, 20);
        txtComments.setEditable(false);
        txtComments.setLineWrap(true);
        txtComments.setWrapStyleWord(true);
        registry.register("textarea.cliente.comentarios", txtComments);
        commentsPanel.add(new JScrollPane(txtComments), BorderLayout.CENTER);

        centerPanel.add(commentsPanel, BorderLayout.SOUTH);

        return centerPanel;
    } // --- FIN de metodo createCenterPanel ---

} // --- FIN de clase ClientBuilder ---
