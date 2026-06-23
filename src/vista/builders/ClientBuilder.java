package vista.builders;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Paths;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ClientController;
import controlador.GeneralController;
import controlador.managers.ToolbarManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import modelo.proyecto.ProjectModel;
import modelo.proyecto.SelectionState;
import servicios.ProjectManager;
import vista.models.ClienteTableModel;
import vista.models.ProyectoClienteTableModel;
import vista.panels.ClientReviewPanel;
import vista.panels.GridDisplayPanel;
import vista.panels.CheckboxEditorPanel;
import vista.panels.ImageDisplayPanel;
import vista.panels.PolaroidDisplayPanel;
import vista.renderers.ActionCellRenderer;
import vista.renderers.CodeCellRenderer;
import vista.renderers.CommentCellRenderer;
import vista.renderers.TristateCellRenderer;
import vista.theme.ThemeManager;

/**
 * Constructor del panel principal del Modo Cliente.
 * Organiza las tablas de proyecto (izquierda), visor con editor (centro)
 * y tablas de cliente (derecha) en un JSplitPane jerárquico,
 * además de registrar todos los componentes en el ComponentRegistry.
 */
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

        JSplitPane leftPanel = createProjectTablesPanel();
        JSplitPane rightPanel = createClientTablesPanel();

        JPanel displayPlaceholder = new JPanel(new CardLayout());
        registry.register("container.displaymodes.cliente", displayPlaceholder);
        registry.register("placeholder.display.cliente", displayPlaceholder);

        ImageDisplayPanel sharedImagePanel = registry.get("panel.display.imagen");
        GridDisplayPanel sharedGridPanel = registry.get("panel.display.grid");
        PolaroidDisplayPanel sharedPolaroidPanel = registry.get("panel.display.polaroid");

        if (sharedImagePanel != null) {
            registry.register("panel.cliente.display", sharedImagePanel);
            registry.register("label.cliente.imagen", sharedImagePanel.getInternalLabel(), "WHEEL_NAVIGABLE");
        }
        if (sharedGridPanel != null) {
            registry.register("panel.display.grid.cliente", sharedGridPanel);
            registry.register("list.grid.cliente", sharedGridPanel.getGridList(), "WHEEL_NAVIGABLE");
        }
        if (sharedPolaroidPanel != null) {
            registry.register("panel.cliente.display.polaroid", sharedPolaroidPanel);
            registry.register("panel.cliente.display.polaroid.image", sharedPolaroidPanel.getImagePanel());
            registry.register("label.cliente.polaroid.imagen", sharedPolaroidPanel.getInternalLabel(), "WHEEL_NAVIGABLE");
        }

        JPanel displayWrapper = new JPanel(new CardLayout());
        registry.register("container.displaymodes.cliente.wrapper", displayWrapper);
        displayWrapper.add(displayPlaceholder, "DISPLAY_NORMAL");

        ClientReviewPanel reviewPanel = new ClientReviewPanel(themeManager, model, projectManager);
        registry.register("panel.cliente.review", reviewPanel);
        displayWrapper.add(reviewPanel, "DISPLAY_REVIEW");

        CheckboxEditorPanel checkboxEditorPanel = new CheckboxEditorPanel(themeManager, model, projectManager, registry);
        registry.register("panel.cliente.editor", checkboxEditorPanel);
        displayWrapper.add(checkboxEditorPanel, "DISPLAY_EDITOR");

        ((CardLayout) displayWrapper.getLayout()).show(displayWrapper, "DISPLAY_NORMAL");

        JPanel centerPanel = createCenterPanel(displayWrapper);

        JSplitPane centerRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerPanel, rightPanel);
        centerRightSplit.setResizeWeight(0.8);
        centerRightSplit.setContinuousLayout(true);
        centerRightSplit.setBorder(null);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, centerRightSplit);
        mainSplit.setResizeWeight(0.25);
        mainSplit.setContinuousLayout(true);
        mainSplit.setBorder(null);
        registry.register("splitpane.cliente.main", mainSplit);

        panelClienteRaiz.add(mainSplit, BorderLayout.CENTER);

        return panelClienteRaiz;
    } // --- FIN de metodo buildClientViewPanel ---


    private JSplitPane createProjectTablesPanel() {
        JPanel panelSeleccion = createProjectTable("Selecci\u00f3n Actual (Proyecto)", "table.cliente.proyecto.seleccion", true);
        JPanel panelDescartes = createProjectTable("Descartes (Proyecto)", "table.cliente.proyecto.descartes", false);

        panelDescartes.setMinimumSize(new java.awt.Dimension(100, 100));
        panelSeleccion.setMinimumSize(new java.awt.Dimension(100, 150));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelSeleccion, panelDescartes);
        split.setContinuousLayout(true);
        split.setBorder(null);
        split.setResizeWeight(0.7);
        return split;
    } // --- FIN de metodo createProjectTablesPanel ---


    private JSplitPane createClientTablesPanel() {
        JPanel panelSeleccion = createClientTable("Selecci\u00f3n (Cliente)", "table.cliente.cliente.seleccion", true);
        JPanel panelDescartes = createClientTable("Descartes (Cliente)", "table.cliente.cliente.descartes", false);

        panelDescartes.setMinimumSize(new java.awt.Dimension(150, 100));
        panelSeleccion.setMinimumSize(new java.awt.Dimension(150, 150));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelSeleccion, panelDescartes);
        split.setContinuousLayout(true);
        split.setBorder(null);
        split.setResizeWeight(0.7);
        return split;
    } // --- FIN de metodo createClientTablesPanel ---


    private JPanel createProjectTable(String title, String tableName, boolean isSeleccion) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));

        ProjectModel project = projectManager.getCurrentProject();
        ProyectoClienteTableModel tableModel = new ProyectoClienteTableModel(project, isSeleccion);
        JTable table = new JTable(tableModel);
        table.setName(tableName);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);

        table.getColumnModel().getColumn(ProyectoClienteTableModel.COL_ESTADO)
                .setCellRenderer(new TristateCellRenderer());
        table.getColumnModel().getColumn(ProyectoClienteTableModel.COL_CODIGO)
                .setCellRenderer(new CodeCellRenderer());
        table.getColumnModel().getColumn(ProyectoClienteTableModel.COL_ACCIONES)
                .setCellRenderer(new ActionCellRenderer(isSeleccion));

        table.getColumnModel().getColumn(ProyectoClienteTableModel.COL_ESTADO).setMaxWidth(50);
        table.getColumnModel().getColumn(ProyectoClienteTableModel.COL_CODIGO).setMaxWidth(80);
        table.getColumnModel().getColumn(ProyectoClienteTableModel.COL_ACCIONES).setMaxWidth(80);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;
                ProyectoClienteTableModel model = (ProyectoClienteTableModel) table.getModel();
                if (col == ProyectoClienteTableModel.COL_ACCIONES) {
                    String key = model.getImageKey(row);
                    Rectangle cellRect = table.getCellRect(row, col, false);
                    boolean clickIzquierdo = e.getX() < cellRect.x + cellRect.width / 2;
                    if (clickIzquierdo) {
                        if (model.isSeleccion()) {
                            clientController.moverADescartesCliente(key);
                        } else {
                            clientController.restaurarDeDescartesCliente(key);
                        }
                    } else {
                        String code = model.getValueAt(row, ProyectoClienteTableModel.COL_CODIGO).toString();
                        String nuevo = JOptionPane.showInputDialog(table,
                                "Editar c\u00f3digo de cat\u00e1logo:", code);
                        if (nuevo != null && !nuevo.trim().isEmpty()) {
                            projectManager.getCurrentProject().getImageCodes().put(key, nuevo.trim());
                            model.refrescar();
                        }
                    }
                } else {
                    String key = model.getImageKey(row);
                    if (key != null && clientController.getVisorController() != null) {
                        clientController.getVisorController()
                                .actualizarImagenPrincipalPorPath(Paths.get(key), key);
                    }
                }
            }
        });

        registry.register(tableName, table, "WHEEL_NAVIGABLE");
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    } // --- FIN de metodo createProjectTable ---


    private JPanel createClientTable(String title, String tableName, boolean isSeleccion) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));

        ProjectModel project = projectManager.getCurrentProject();
        ClienteTableModel tableModel = new ClienteTableModel(project, isSeleccion);
        JTable table = new JTable(tableModel);
        table.setName(tableName);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);

        TableColumn estadoCol = table.getColumnModel().getColumn(ClienteTableModel.COL_ESTADO);
        estadoCol.setCellRenderer(new TristateCellRenderer());
        estadoCol.setMaxWidth(50);

        table.getColumnModel().getColumn(ClienteTableModel.COL_CODIGO_IMG).setMaxWidth(80);
        table.getColumnModel().getColumn(ClienteTableModel.COL_CODIGO_IMG)
                .setCellRenderer(new CodeCellRenderer());

        table.getColumnModel().getColumn(ClienteTableModel.COL_CODIGO_CB).setMaxWidth(80);

        TableColumn commentCol = table.getColumnModel().getColumn(ClienteTableModel.COL_COMENTARIO);
        commentCol.setCellRenderer(new CommentCellRenderer());
        commentCol.setMaxWidth(50);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;
                ClienteTableModel model = (ClienteTableModel) table.getModel();
                String key = model.getImageKey(row);

                if (col == ClienteTableModel.COL_ESTADO) {
                    SelectionState current = model.getEstado(row);
                    SelectionState next = switch (current) {
                        case SELECTED -> SelectionState.DISCARDED;
                        case DISCARDED -> SelectionState.UNDEFINED;
                        case UNDEFINED -> SelectionState.SELECTED;
                    };
                    clientController.updateClientSelectionState(key, next);
                    model.refrescar();
                } else if (col == ClienteTableModel.COL_COMENTARIO && e.getClickCount() >= 2) {
                    ProjectModel currentProject = projectManager.getCurrentProject();
                    String actual = currentProject != null && currentProject.hasClientSelection()
                            ? currentProject.getClientSelection().getComments().getOrDefault(key, "")
                            : "";
                    String nuevo = JOptionPane.showInputDialog(table,
                            "Comentario para esta imagen:", actual);
                    if (nuevo != null && currentProject != null) {
                        currentProject.getClientSelection().getComments().put(key, nuevo);
                        model.refrescar();
                    }
                } else if (col != ClienteTableModel.COL_ESTADO && col != ClienteTableModel.COL_COMENTARIO) {
                    if (key != null && clientController.getVisorController() != null) {
                        clientController.getVisorController()
                                .actualizarImagenPrincipalPorPath(Paths.get(key), key);
                    }
                }
            }
        });

        registry.register(tableName, table, "WHEEL_NAVIGABLE");
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    } // --- FIN de metodo createClientTable ---


    private JPanel createCenterPanel(JPanel displayModesContainer) {
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Visor (Comparativa)"));

        if (displayModesContainer != null) {
            centerPanel.add(displayModesContainer, BorderLayout.CENTER);
        }

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
